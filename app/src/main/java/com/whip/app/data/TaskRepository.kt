package com.whip.app.data

import androidx.room.withTransaction
import com.whip.app.core.WhipClock
import com.whip.app.core.UuidWhipIdGenerator
import com.whip.app.domain.OccurrenceState
import com.whip.app.domain.RecurrenceEnd
import com.whip.app.domain.RecurrenceAnchor
import com.whip.app.domain.RecurrenceEngine
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.ScheduledSubtask
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskEditBoundary
import com.whip.app.domain.TaskEditSubtaskBoundary
import com.whip.app.domain.TaskOccurrence
import com.whip.app.domain.TaskStep
import com.whip.app.domain.TaskStepState
import com.whip.app.domain.TaskStepSnapshot
import com.whip.app.domain.TaskPriority
import com.whip.app.domain.TaskEffort
import com.whip.app.domain.WhipTask
import com.whip.app.domain.toDraft
import com.whip.app.domain.semanticRevisionToken
import com.whip.app.domain.visibleTaskStepsForOccurrence
import java.time.LocalDate
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface TaskRepository {
    val tasks: Flow<List<WhipTask>>
    val occurrences: Flow<List<TaskOccurrence>>
    val steps: Flow<List<TaskStep>>
    val stepStates: Flow<List<TaskStepState>>
    val stepSnapshots: Flow<List<TaskStepSnapshot>>

    suspend fun create(draft: TaskDraft): Long
    suspend fun update(taskId: Long, draft: TaskDraft, fromOccurrence: LocalDate? = null): Long
    suspend fun updateIfCurrent(expected: TaskEditBoundary, draft: TaskDraft, fromOccurrence: LocalDate? = null): Long
    suspend fun complete(item: ScheduledTask)
    suspend fun completeOccurrence(taskId: Long, originalDate: LocalDate?)
    suspend fun skip(item: ScheduledTask)
    suspend fun reschedule(item: ScheduledTask, newDate: LocalDate)
    suspend fun setStepCompleted(item: ScheduledTask, stepId: Long, completed: Boolean): Boolean
    suspend fun promoteStep(item: ScheduledTask, stepId: Long): Long
    suspend fun archive(taskId: Long)
    suspend fun restore(taskId: Long)
    suspend fun deletePermanently(taskId: Long): Boolean
    suspend fun reopen(taskId: Long)
    suspend fun reopenIfCurrent(item: ScheduledTask)
    suspend fun reopenOccurrence(item: ScheduledTask)
    suspend fun reopenAllIfCurrent(items: List<ScheduledTask>)
    suspend fun resetOccurrence(taskId: Long, originalDate: LocalDate): Boolean
    suspend fun resetOccurrenceIfCurrent(item: ScheduledTask): Boolean
    suspend fun setPinned(taskId: Long, pinned: Boolean)
    suspend fun duplicate(taskId: Long): Long
    suspend fun completeAll(items: List<ScheduledTask>)
    suspend fun rescheduleAll(items: List<ScheduledTask>, newDate: LocalDate)
    suspend fun planAll(items: List<ScheduledTask>, newDate: LocalDate)
    suspend fun restoreSchedulesIfCurrent(items: List<ScheduledTask>, expectedDate: LocalDate)
    suspend fun archiveAll(taskIds: List<Long>)
    suspend fun archiveAllIfCurrent(items: List<ScheduledTask>)
    suspend fun restoreAll(taskIds: List<Long>)
    suspend fun setPinnedAll(taskIds: List<Long>, pinned: Boolean)
    suspend fun updateMetadataAll(taskIds: List<Long>, edit: TaskBulkEdit)
    suspend fun updateMetadataAllIfCurrent(items: List<ScheduledTask>, edit: TaskBulkEdit)
    suspend fun reorderAll(taskIdsInOrder: List<Long>)
    suspend fun getTask(taskId: Long): WhipTask?
    suspend fun getOccurrences(taskId: Long): List<TaskOccurrence>
    suspend fun undoPromoteStep(promotedTaskId: Long, sourceTaskId: Long, sourceStepId: Long)
}

/** Fields are left unchanged unless their corresponding update flag/value is supplied. */
data class TaskBulkEdit(
    val updateArea: Boolean = false,
    val areaId: String? = null,
    val areaName: String = "",
    val tags: Set<String>? = null,
    val priority: TaskPriority? = null,
    val effort: TaskEffort? = null,
)

class RoomTaskRepository(
    private val database: WhipDatabase,
    private val clock: WhipClock,
) : TaskRepository {
    private val dao = database.taskDao()
    private val areaRepository = RoomAreaRepository(database, clock, UuidWhipIdGenerator)

    override val tasks: Flow<List<WhipTask>> = dao.observeAllTasks().map { tasks ->
        tasks.mapNotNull { task -> runCatching(task::toDomain).getOrNull() }
    }

    override val occurrences: Flow<List<TaskOccurrence>> =
        dao.observeOccurrences().map { occurrences ->
            occurrences.mapNotNull { occurrence -> runCatching(occurrence::toDomain).getOrNull() }
        }

    override val steps: Flow<List<TaskStep>> = dao.observeSteps().map { steps ->
        steps.map(TaskStepEntity::toDomain)
    }

    override val stepStates: Flow<List<TaskStepState>> =
        dao.observeStepStates().map { states ->
            states.map(TaskStepStateEntity::toDomain)
        }

    override val stepSnapshots: Flow<List<TaskStepSnapshot>> =
        dao.observeStepSnapshots().map { snapshots ->
            snapshots.map(TaskStepSnapshotEntity::toDomain)
        }

    override suspend fun create(draft: TaskDraft): Long = database.withTransaction {
        val now = clock.now().toEpochMilli()
        val area = areaRepository.resolve(draft.areaId, draft.area)
        val resolvedDraft = draft.copy(areaId = area.id, area = area.name)
        val taskId = dao.insertTask(resolvedDraft.toEntity(createdAtMillis = now, manualPosition = dao.nextManualPosition()))
        syncSteps(taskId, resolvedDraft, now)
        taskId
    }

    override suspend fun update(
        taskId: Long,
        draft: TaskDraft,
        fromOccurrence: LocalDate?,
    ): Long = database.withTransaction {
            val existing = requireNotNull(dao.getTask(taskId)) { "Task no longer exists" }
            val adjustedDraft = when {
                fromOccurrence != null && draft.scheduleKind == ScheduleKind.Recurring -> draft.copy(
                    date = fromOccurrence,
                    recurrence = requireNotNull(draft.recurrence).copy(startDate = fromOccurrence),
                )
                fromOccurrence != null && draft.scheduleKind == ScheduleKind.Once -> draft.copy(
                    date = draft.date ?: fromOccurrence,
                )
                else -> draft
            }
            val selectedArea = areaRepository.resolve(adjustedDraft.areaId, adjustedDraft.area)
            val resolvedDraft = adjustedDraft.copy(areaId = selectedArea.id, area = selectedArea.name)
            val now = clock.now().toEpochMilli()
            val existingTask = existing.toDomain().copy(
                steps = dao.getSteps(taskId).map(TaskStepEntity::toDomain),
            )
            val shouldSplitFuture = fromOccurrence != null &&
                existingTask.scheduleKind == ScheduleKind.Recurring &&
                fromOccurrence.isAfter(requireNotNull(existingTask.recurrence).startDate)
            if (shouldSplitFuture) {
                val boundary = requireNotNull(fromOccurrence)
                val oldRule = requireNotNull(existingTask.recurrence)
                val futureDraft = resolvedDraft.copy(
                    recurrence = resolvedDraft.recurrence?.let { editedRule ->
                        if (
                            oldRule.end == RecurrenceEnd.AfterCount &&
                            editedRule.end == RecurrenceEnd.AfterCount &&
                            editedRule.occurrenceCount == oldRule.occurrenceCount
                        ) {
                            val consumed = if (oldRule.anchor == RecurrenceAnchor.Schedule) {
                                RecurrenceEngine.occurrencesBetween(
                                    oldRule,
                                    oldRule.startDate,
                                    boundary.minusDays(1),
                                ).size
                            } else {
                                dao.getOccurrences(taskId).count { occurrence ->
                                    occurrence.originalEpochDay < boundary.toEpochDay() &&
                                        occurrence.state in setOf(
                                            OccurrenceState.Completed.name,
                                            OccurrenceState.Skipped.name,
                                        )
                                }
                            }
                            val remaining = requireNotNull(oldRule.occurrenceCount) - consumed
                            require(remaining > 0) { "This recurring series has no future occurrences to edit" }
                            editedRule.copy(occurrenceCount = remaining)
                        } else {
                            editedRule
                        }
                    },
                )
                val historicalDraft = existingTask.toDraft().copy(
                    recurrence = oldRule.copy(
                        end = RecurrenceEnd.OnDate,
                        endDate = boundary.minusDays(1),
                        occurrenceCount = null,
                    ),
                )
                dao.updateTask(
                    historicalDraft.toEntity(
                        id = existing.id,
                        uuid = existing.uuid,
                        createdAtMillis = existing.createdAtMillis,
                        updatedAtMillis = now,
                        completedAtMillis = existing.completedAtMillis,
                        manualPosition = existing.manualPosition,
                    ).copy(archived = existing.archived, pinned = existing.pinned),
                )
                val futureId = dao.insertTask(
                    futureDraft.toEntity(createdAtMillis = now, manualPosition = dao.nextManualPosition())
                        .copy(pinned = existing.pinned),
                )
                syncSteps(futureId, futureDraft, now)
                val newStepByOldId = futureStepIdMap(taskId, futureId, futureDraft)
                migrateOpenFutureState(
                    oldTaskId = taskId,
                    newTaskId = futureId,
                    boundary = boundary,
                    futureDraft = futureDraft,
                    newStepByOldId = newStepByOldId,
                )
                copyFutureSeriesIntegrations(taskId, futureId, boundary, now, newStepByOldId)
                return@withTransaction futureId
            }
            dao.updateTask(
                resolvedDraft.toEntity(
                    id = existing.id,
                    uuid = existing.uuid,
                    createdAtMillis = existing.createdAtMillis,
                    updatedAtMillis = now,
                    completedAtMillis = existing.completedAtMillis,
                    manualPosition = existing.manualPosition,
                ).copy(archived = existing.archived, pinned = existing.pinned),
            )
            val existingSteps = dao.getSteps(taskId)
            if (!existingSteps.matches(resolvedDraft.steps)) {
                syncSteps(taskId, resolvedDraft, now)
            }
            taskId
    }

    override suspend fun updateIfCurrent(
        expected: TaskEditBoundary,
        draft: TaskDraft,
        fromOccurrence: LocalDate?,
    ): Long = database.withTransaction {
        requireMatchingTaskRevision(expected)
        if (fromOccurrence != null) {
            require(expected.originalEpochDay == fromOccurrence.toEpochDay()) {
                "The edited occurrence changed while this editor was open"
            }
            requireCurrentOpenSchedule(
                expected,
                "Task occurrence changed while this editor was open. Close it, review the latest version, and try again.",
            )
        }
        update(expected.taskId, draft, fromOccurrence)
    }

    override suspend fun complete(item: ScheduledTask) {
        database.withTransaction {
            completeWithinTransaction(item, clock.now().toEpochMilli())
        }
    }

    override suspend fun completeOccurrence(taskId: Long, originalDate: LocalDate?) = database.withTransaction {
        val entity = dao.getTask(taskId) ?: return@withTransaction
        if (entity.completedAtMillis != null) return@withTransaction
        val task = entity.toDomain().copy(steps = dao.getSteps(taskId).map(TaskStepEntity::toDomain))
        val original = when (task.scheduleKind) {
            ScheduleKind.Recurring -> requireNotNull(originalDate) { "Occurrence date is required" }
            ScheduleKind.Once -> task.date
            ScheduleKind.Anytime -> null
        }
        val occurrence = original?.let { date ->
            dao.getOccurrences(taskId).firstOrNull { it.originalEpochDay == date.toEpochDay() }
        }
        if (occurrence?.state == OccurrenceState.Completed.name) return@withTransaction
        val scheduled = occurrence?.scheduledEpochDay?.let(LocalDate::ofEpochDay) ?: original
        val key = original?.toEpochDay() ?: task.date?.toEpochDay()
            ?: com.whip.app.domain.ANYTIME_TASK_OCCURRENCE_KEY
        val states = dao.getStepStates(taskId, key).associateBy(TaskStepStateEntity::stepId)
        val visibleSteps = visibleTaskStepsForOccurrence(
            steps = task.steps,
            snapshots = dao.getStepSnapshotsForTask(taskId).map(TaskStepSnapshotEntity::toDomain),
            occurrenceKey = key,
            policy = task.repeatStepPolicy,
        )
        val item = ScheduledTask(
            task = task,
            originalDate = original,
            scheduledDate = scheduled,
            subtasks = visibleSteps.map { step ->
                val state = states[step.id]
                ScheduledSubtask(
                    step = step,
                    completed = state?.completed == true,
                    completedAtMillis = state?.completedAtMillis,
                    title = state?.titleSnapshot ?: step.title,
                    notes = step.notes,
                )
            },
        )
        completeWithinTransaction(item, clock.now().toEpochMilli())
    }

    override suspend fun skip(item: ScheduledTask) {
        if (item.task.scheduleKind != ScheduleKind.Recurring) return
        database.withTransaction {
            requireCurrentOpenSchedule(item, "Task occurrence changed before it could be skipped")
            val now = clock.now().toEpochMilli()
            dao.upsertOccurrence(
                TaskOccurrence(
                    taskId = item.task.id,
                    originalDate = requireNotNull(item.originalDate),
                    scheduledDate = requireNotNull(item.scheduledDate),
                    state = OccurrenceState.Skipped,
                    // A deliberate skip closes an occurrence at a real point in time. This is
                    // also the anchor used by completion-relative repeats.
                    completedAtMillis = now,
                ).toEntity(),
            )
        }
    }

    override suspend fun reschedule(item: ScheduledTask, newDate: LocalDate) {
        database.withTransaction {
            val existing = requireCurrentOpenSchedule(
                item,
                "Task schedule changed while the date picker was open",
            )
            if (item.task.scheduleKind == ScheduleKind.Recurring) {
                val originalDate = requireNotNull(item.originalDate) { "Recurring occurrence identity is missing" }
                dao.upsertOccurrence(
                    TaskOccurrence(
                        taskId = item.task.id,
                        originalDate = originalDate,
                        scheduledDate = newDate,
                        state = OccurrenceState.Open,
                        completedAtMillis = null,
                    ).toEntity(),
                )
            } else {
                dao.updateTask(
                    existing.copy(
                        scheduleKind = ScheduleKind.Once.name,
                        dateEpochDay = newDate.toEpochDay(),
                        inbox = false,
                        completedAtMillis = null,
                        updatedAtMillis = clock.now().toEpochMilli(),
                    ),
                )
            }
        }
    }

    override suspend fun setStepCompleted(
        item: ScheduledTask,
        stepId: Long,
        completed: Boolean,
    ): Boolean = database.withTransaction {
        requireCurrentOpenSchedule(item, "Task changed before its Subtask could be updated")
        val step = dao.getSteps(item.task.id).firstOrNull { it.id == stepId && !it.archived }
            ?: error("Subtask no longer exists")
        val now = clock.now().toEpochMilli()
        dao.upsertStepState(
            TaskStepStateEntity(
                stepId = step.id,
                taskId = item.task.id,
                occurrenceKey = item.occurrenceKey,
                completed = completed,
                completedAtMillis = now.takeIf { completed },
                titleSnapshot = step.title,
            ),
        )

        val visibleStepIds = item.subtasks.mapTo(mutableSetOf()) { it.step.id }
        val activeSteps = dao.getSteps(item.task.id).filter { it.id in visibleStepIds }
        val states = dao.getStepStates(item.task.id, item.occurrenceKey).associateBy { it.stepId }
        val shouldCompleteParent = item.task.autoCompleteFromSteps &&
            activeSteps.isNotEmpty() &&
            activeSteps.all { states[it.id]?.completed == true }
        if (shouldCompleteParent) completeWithinTransaction(item, now)
        shouldCompleteParent
    }

    override suspend fun promoteStep(item: ScheduledTask, stepId: Long): Long =
        database.withTransaction {
            requireCurrentOpenSchedule(item, "Task changed before its Subtask could be moved")
            val step = dao.getSteps(item.task.id).firstOrNull { it.id == stepId && !it.archived }
                ?: error("Subtask no longer exists")
            val now = clock.now().toEpochMilli()
            val promotedId = dao.insertTask(
                TaskDraft(
                    title = step.title,
                    icon = item.task.icon,
                    notes = buildString {
                        append("From task: ${item.task.title}")
                        if (step.notes.isNotBlank()) append("\n\n${step.notes}")
                    },
                    priority = item.task.priority,
                    areaId = item.task.areaId,
                    area = item.task.area,
                    tags = item.task.tags,
                    effort = item.task.effort,
                    inbox = true,
                ).toEntity(createdAtMillis = now, manualPosition = dao.nextManualPosition()),
            )
            dao.updateStep(step.copy(archived = true, updatedAtMillis = now))
            promotedId
        }

    override suspend fun undoPromoteStep(
        promotedTaskId: Long,
        sourceTaskId: Long,
        sourceStepId: Long,
    ) = database.withTransaction {
        dao.deleteTask(promotedTaskId)
        val source = dao.getSteps(sourceTaskId).firstOrNull { it.id == sourceStepId } ?: return@withTransaction
        dao.updateStep(source.copy(archived = false, updatedAtMillis = clock.now().toEpochMilli()))
    }

    override suspend fun archive(taskId: Long) {
        val existing = dao.getTask(taskId) ?: return
        dao.updateTask(
            existing.copy(
                archived = true,
                updatedAtMillis = clock.now().toEpochMilli(),
            ),
        )
    }

    override suspend fun restore(taskId: Long) {
        val existing = dao.getTask(taskId) ?: return
        dao.updateTask(
            existing.copy(
                archived = false,
                updatedAtMillis = clock.now().toEpochMilli(),
            ),
        )
    }

    override suspend fun deletePermanently(taskId: Long): Boolean =
        dao.deleteTask(taskId) > 0

    override suspend fun reopen(taskId: Long) {
        val existing = dao.getTask(taskId) ?: return
        dao.updateTask(
            existing.copy(
                completedAtMillis = null,
                archived = false,
                updatedAtMillis = clock.now().toEpochMilli(),
            ),
        )
    }

    override suspend fun reopenIfCurrent(item: ScheduledTask) = database.withTransaction {
        val existing = requireReopenableOneShot(item)
        dao.updateTask(existing.copy(completedAtMillis = null, updatedAtMillis = clock.now().toEpochMilli()))
    }

    override suspend fun reopenOccurrence(item: ScheduledTask) {
        require(item.task.scheduleKind == ScheduleKind.Recurring) {
            "Only a recurring occurrence can be reopened this way"
        }
        database.withTransaction {
            val originalDate = requireNotNull(item.originalDate)
            requireReopenableOccurrence(item)
            dao.upsertOccurrence(
                TaskOccurrence(
                    taskId = item.task.id,
                    originalDate = originalDate,
                    scheduledDate = requireNotNull(item.scheduledDate),
                    state = OccurrenceState.Open,
                    completedAtMillis = null,
                ).toEntity(),
            )
            dao.deleteStepSnapshotsForOccurrence(item.task.id, originalDate.toEpochDay())
        }
    }

    override suspend fun reopenAllIfCurrent(items: List<ScheduledTask>) = database.withTransaction {
        val unique = items.distinctBy(ScheduledTask::stableKey)
        val oneShots = unique.filter { it.task.scheduleKind != ScheduleKind.Recurring }
            .associateWith { requireReopenableOneShot(it) }
        val recurring = unique.filter { it.task.scheduleKind == ScheduleKind.Recurring }
            .onEach { requireReopenableOccurrence(it) }
        val now = clock.now().toEpochMilli()
        oneShots.forEach { (_, existing) ->
            dao.updateTask(existing.copy(completedAtMillis = null, updatedAtMillis = now))
        }
        recurring.forEach { item ->
            val originalDate = requireNotNull(item.originalDate)
            dao.upsertOccurrence(
                TaskOccurrence(
                    taskId = item.task.id,
                    originalDate = originalDate,
                    scheduledDate = requireNotNull(item.scheduledDate),
                    state = OccurrenceState.Open,
                    completedAtMillis = null,
                ).toEntity(),
            )
            dao.deleteStepSnapshotsForOccurrence(item.task.id, originalDate.toEpochDay())
        }
    }

    override suspend fun resetOccurrence(taskId: Long, originalDate: LocalDate): Boolean =
        dao.deleteOccurrence(taskId, originalDate.toEpochDay()) > 0

    override suspend fun resetOccurrenceIfCurrent(item: ScheduledTask): Boolean = database.withTransaction {
        require(item.task.scheduleKind == ScheduleKind.Recurring) { "Only repeating occurrences can be reset" }
        requireMatchingTaskRevision(item)
        val originalDate = requireNotNull(item.originalDate)
        val current = dao.getOccurrences(item.task.id)
            .firstOrNull { it.originalEpochDay == originalDate.toEpochDay() }
            ?: return@withTransaction false
        val expectedState = requireNotNull(item.occurrenceState) {
            "Task occurrence state is missing; reopen the history view and try again"
        }
        require(
            current.scheduledEpochDay == requireNotNull(item.scheduledDate).toEpochDay() &&
                current.state == expectedState.name &&
                current.completedAtMillis == item.completedAtMillis,
        ) { "Task occurrence changed before it could be restored" }
        dao.deleteOccurrence(item.task.id, originalDate.toEpochDay()) > 0
    }

    override suspend fun setPinned(taskId: Long, pinned: Boolean) {
        val existing = dao.getTask(taskId) ?: return
        dao.updateTask(existing.copy(pinned = pinned, updatedAtMillis = clock.now().toEpochMilli()))
    }

    override suspend fun duplicate(taskId: Long): Long = database.withTransaction {
        val existing = requireNotNull(dao.getTask(taskId)) { "Task no longer exists" }
        val now = clock.now().toEpochMilli()
        val domain = existing.toDomain().copy(steps = dao.getSteps(taskId).map(TaskStepEntity::toDomain))
        val duplicateDraft = domain.toDraft().copy(
            title = "${domain.title} copy",
            inbox = true,
            scheduleKind = ScheduleKind.Anytime,
            date = null,
            recurrence = null,
            reminderEnabled = false,
            reminderOffsetsMinutes = emptyList(),
        )
        val newId = dao.insertTask(duplicateDraft.toEntity(createdAtMillis = now, manualPosition = dao.nextManualPosition()))
        syncSteps(newId, duplicateDraft, now)
        newId
    }

    override suspend fun completeAll(items: List<ScheduledTask>) = database.withTransaction {
        val now = clock.now().toEpochMilli()
        items.distinctBy(ScheduledTask::stableKey).forEach { completeWithinTransaction(it, now) }
    }

    override suspend fun rescheduleAll(items: List<ScheduledTask>, newDate: LocalDate) = database.withTransaction {
        items.distinctBy(ScheduledTask::stableKey).forEach { reschedule(it, newDate) }
    }

    override suspend fun planAll(items: List<ScheduledTask>, newDate: LocalDate) = database.withTransaction {
        val unique = items.distinctBy(ScheduledTask::stableKey)
        unique.forEach { reschedule(it, newDate) }
    }

    private suspend fun restoreSchedulesWithinTransaction(items: List<ScheduledTask>) {
        val now = clock.now().toEpochMilli()
        items.distinctBy(ScheduledTask::stableKey).forEach { item ->
            if (item.task.scheduleKind == ScheduleKind.Recurring) {
                val originalDate = requireNotNull(item.originalDate)
                if (item.scheduledDate == originalDate) {
                    dao.deleteOccurrence(item.task.id, originalDate.toEpochDay())
                } else {
                    dao.upsertOccurrence(
                        TaskOccurrence(
                            taskId = item.task.id,
                            originalDate = originalDate,
                            scheduledDate = requireNotNull(item.scheduledDate),
                            state = OccurrenceState.Open,
                            completedAtMillis = null,
                        ).toEntity(),
                    )
                }
            } else {
                val existing = requireNotNull(dao.getTask(item.task.id)) { "Task no longer exists" }
                dao.updateTask(
                    existing.copy(
                        scheduleKind = item.task.scheduleKind.name,
                        dateEpochDay = item.task.date?.toEpochDay(),
                        inbox = item.task.scheduleKind == ScheduleKind.Anytime,
                        completedAtMillis = item.task.completedAtMillis,
                        updatedAtMillis = now,
                    ),
                )
            }
        }
    }

    override suspend fun restoreSchedulesIfCurrent(
        items: List<ScheduledTask>,
        expectedDate: LocalDate,
    ) = database.withTransaction {
        val unique = items.distinctBy(ScheduledTask::stableKey)
        unique.forEach { item ->
            val existing = requireTaskIdentity(item)
            if (item.task.scheduleKind == ScheduleKind.Recurring) {
                require(existing.scheduleKind == ScheduleKind.Recurring.name) {
                    "Task schedule changed since this move"
                }
                val originalDate = requireNotNull(item.originalDate)
                val current = dao.getOccurrences(item.task.id)
                    .firstOrNull { it.originalEpochDay == originalDate.toEpochDay() }
                require(current?.state == OccurrenceState.Open.name &&
                    current.scheduledEpochDay == expectedDate.toEpochDay()
                ) { "Task changed since this move" }
            } else {
                require(existing.scheduleKind == ScheduleKind.Once.name &&
                    existing.dateEpochDay == expectedDate.toEpochDay() &&
                    !existing.inbox &&
                    existing.completedAtMillis == null &&
                    existing.archived == item.task.archived
                ) { "Task changed since this move" }
            }
        }
        restoreSchedulesWithinTransaction(unique)
    }

    override suspend fun archiveAll(taskIds: List<Long>) = database.withTransaction {
        taskIds.distinct().forEach { archive(it) }
    }

    override suspend fun archiveAllIfCurrent(items: List<ScheduledTask>) = database.withTransaction {
        val unique = items.distinctBy { it.task.id }
        unique.forEach { requireMatchingTaskRevision(it) }
        unique.forEach { archive(it.task.id) }
    }

    override suspend fun restoreAll(taskIds: List<Long>) = database.withTransaction {
        taskIds.distinct().forEach { restore(it) }
    }

    override suspend fun setPinnedAll(taskIds: List<Long>, pinned: Boolean) = database.withTransaction {
        taskIds.distinct().forEach { setPinned(it, pinned) }
    }

    override suspend fun updateMetadataAll(taskIds: List<Long>, edit: TaskBulkEdit) = database.withTransaction {
        val now = clock.now().toEpochMilli()
        require(!edit.updateArea || !edit.areaId.isNullOrBlank() || edit.areaName.isNotBlank()) {
            "Choose an Area before applying this change"
        }
        val selectedArea = if (edit.updateArea) areaRepository.resolve(edit.areaId, edit.areaName) else null
        taskIds.distinct().forEach { taskId ->
            val existing = requireNotNull(dao.getTask(taskId)) { "Task no longer exists" }
            dao.updateTask(
                existing.copy(
                    areaId = selectedArea?.id ?: if (edit.updateArea) null else existing.areaId,
                    area = selectedArea?.name ?: if (edit.updateArea) "" else existing.area,
                    tagsCsv = edit.tags?.map(String::trim)?.filter(String::isNotBlank)
                        ?.distinctBy(String::lowercase)?.joinToString(",") ?: existing.tagsCsv,
                    priority = edit.priority?.name ?: existing.priority,
                    effort = edit.effort?.name ?: existing.effort,
                    updatedAtMillis = now,
                ),
            )
        }
    }

    override suspend fun updateMetadataAllIfCurrent(
        items: List<ScheduledTask>,
        edit: TaskBulkEdit,
    ) = database.withTransaction {
        val unique = items.distinctBy { it.task.id }
        unique.forEach { requireMatchingTaskRevision(it) }
        updateMetadataAll(unique.map { it.task.id }, edit)
    }

    override suspend fun reorderAll(taskIdsInOrder: List<Long>) = database.withTransaction {
        val requested = taskIdsInOrder.distinct()
        val all = dao.getAllTasks()
        require(requested.all { requestedId -> all.any { it.id == requestedId } }) { "Task no longer exists" }
        val byId = all.associateBy(TaskEntity::id)
        val order = requested + all.filterNot { it.id in requested }.sortedBy(TaskEntity::manualPosition).map(TaskEntity::id)
        val now = clock.now().toEpochMilli()
        order.forEachIndexed { index, id ->
            val current = requireNotNull(byId[id])
            if (current.manualPosition != index) dao.updateTask(current.copy(manualPosition = index, updatedAtMillis = now))
        }
    }

    override suspend fun getTask(taskId: Long): WhipTask? {
        val task = dao.getTask(taskId)?.toDomain() ?: return null
        return task.copy(steps = dao.getSteps(taskId).map(TaskStepEntity::toDomain))
    }

    override suspend fun getOccurrences(taskId: Long): List<TaskOccurrence> =
        dao.getOccurrences(taskId).map(TaskOccurrenceEntity::toDomain)

    private suspend fun requireTaskIdentity(item: ScheduledTask): TaskEntity {
        val existing = requireNotNull(dao.getTask(item.task.id)) { "Task no longer exists" }
        require(existing.createdAtMillis == item.task.createdAtMillis &&
            (item.task.uuid.isBlank() || existing.uuid == item.task.uuid)
        ) { "Task identity changed while this action was open" }
        return existing
    }

    private suspend fun requireMatchingTaskRevision(item: ScheduledTask): TaskEntity {
        return requireMatchingTaskRevision(item.task)
    }

    private suspend fun requireMatchingTaskRevision(expected: WhipTask): TaskEntity {
        val existing = requireNotNull(dao.getTask(expected.id)) { "Task no longer exists" }
        require(
            existing.createdAtMillis == expected.createdAtMillis &&
                (expected.uuid.isBlank() || existing.uuid == expected.uuid),
        ) { "Task identity changed while this action was open" }
        val current = existing.toDomain().copy(
            steps = dao.getSteps(expected.id).map(TaskStepEntity::toDomain),
        )
        require(current == expected) {
            "Task changed while this action was open"
        }
        return existing
    }

    private suspend fun requireMatchingTaskRevision(expected: TaskEditBoundary): TaskEntity {
        val existing = requireNotNull(dao.getTask(expected.taskId)) { "Task no longer exists" }
        require(
            existing.createdAtMillis == expected.taskCreatedAtMillis &&
                (expected.taskUuid.isBlank() || existing.uuid == expected.taskUuid),
        ) { "Task identity changed while this editor was open" }
        val current = existing.toDomain().copy(
            steps = dao.getSteps(expected.taskId).map(TaskStepEntity::toDomain),
        )
        require(current.semanticRevisionToken() == expected.taskRevision) {
            "Task changed while this editor was open"
        }
        return existing
    }

    private suspend fun requireReopenableOneShot(item: ScheduledTask): TaskEntity {
        require(item.task.scheduleKind != ScheduleKind.Recurring) { "Use occurrence reopen for repeating Tasks" }
        val existing = requireMatchingTaskRevision(item)
        require(
            existing.dateEpochDay == item.scheduledDate?.toEpochDay() &&
                existing.completedAtMillis != null &&
                existing.completedAtMillis == item.completedAtMillis &&
                !existing.archived,
        ) { "Task changed before it could be reopened" }
        return existing
    }

    private suspend fun requireReopenableOccurrence(item: ScheduledTask): TaskOccurrenceEntity {
        require(item.task.scheduleKind == ScheduleKind.Recurring) {
            "Only a recurring occurrence can be reopened this way"
        }
        requireMatchingTaskRevision(item)
        val originalDate = requireNotNull(item.originalDate)
        val current = dao.getOccurrences(item.task.id)
            .firstOrNull { it.originalEpochDay == originalDate.toEpochDay() }
            ?: error("Task occurrence no longer exists")
        val expectedState = requireNotNull(item.occurrenceState) {
            "Task occurrence state is missing; reopen the history view and try again"
        }
        require(
            current.state != OccurrenceState.Open.name &&
                current.state == expectedState.name &&
                current.scheduledEpochDay == requireNotNull(item.scheduledDate).toEpochDay() &&
                current.completedAtMillis == item.completedAtMillis,
        ) { "Task occurrence changed before it could be reopened" }
        return current
    }

    private suspend fun requireCurrentOpenSchedule(
        item: ScheduledTask,
        failureMessage: String,
    ): TaskEntity {
        val existing = requireMatchingTaskRevision(item)
        require(!existing.archived && existing.completedAtMillis == null) { failureMessage }
        if (item.task.scheduleKind == ScheduleKind.Recurring) {
            val originalDate = requireNotNull(item.originalDate) { "Recurring occurrence identity is missing" }
            val expectedScheduledDate = requireNotNull(item.scheduledDate) { "Recurring schedule is missing" }
            val records = dao.getOccurrences(item.task.id)
            val current = records.firstOrNull { it.originalEpochDay == originalDate.toEpochDay() }
            if (current == null) {
                require(expectedScheduledDate == originalDate) { failureMessage }
                val task = existing.toDomain()
                val rule = requireNotNull(task.recurrence)
                if (rule.anchor == RecurrenceAnchor.Schedule) {
                    require(
                        originalDate in RecurrenceEngine.occurrencesBetween(rule, originalDate, originalDate),
                    ) { failureMessage }
                } else {
                    val closed = records.filter {
                        it.state == OccurrenceState.Completed.name || it.state == OccurrenceState.Skipped.name
                    }
                    val latest = closed.maxByOrNull {
                        it.completedAtMillis ?: it.scheduledEpochDay * 86_400_000L
                    }
                    val closedOn = latest?.completedAtMillis?.let { completedAt ->
                        Instant.ofEpochMilli(completedAt).atZone(clock.zoneId()).toLocalDate()
                    } ?: latest?.scheduledEpochDay?.let(LocalDate::ofEpochDay)
                    require(
                        RecurrenceEngine.nextCompletionRelative(rule, closedOn, closed.size) == originalDate,
                    ) { failureMessage }
                }
            } else {
                require(
                    current.state == OccurrenceState.Open.name &&
                        (item.occurrenceState == null || item.occurrenceState == OccurrenceState.Open) &&
                        current.scheduledEpochDay == expectedScheduledDate.toEpochDay(),
                ) { failureMessage }
            }
        } else {
            require(
                existing.dateEpochDay == item.scheduledDate?.toEpochDay() &&
                    item.task.completedAtMillis == null && !item.task.archived,
            ) { failureMessage }
        }
        return existing
    }

    private suspend fun requireCurrentOpenSchedule(
        expected: TaskEditBoundary,
        failureMessage: String,
    ): TaskEntity {
        val existing = requireMatchingTaskRevision(expected)
        require(
            existing.scheduleKind == ScheduleKind.Recurring.name &&
                !existing.archived &&
                existing.completedAtMillis == null,
        ) { failureMessage }
        val originalDate = expected.originalEpochDay?.let(LocalDate::ofEpochDay)
            ?: error("Recurring occurrence identity is missing")
        val scheduledDate = expected.scheduledEpochDay?.let(LocalDate::ofEpochDay)
            ?: error("Recurring schedule is missing")
        val records = dao.getOccurrences(expected.taskId)
        val currentOccurrence = records.firstOrNull { it.originalEpochDay == expected.originalEpochDay }
        if (currentOccurrence == null) {
            require(expected.occurrenceState == null && scheduledDate == originalDate) { failureMessage }
            val rule = requireNotNull(existing.toDomain().recurrence)
            if (rule.anchor == RecurrenceAnchor.Schedule) {
                require(
                    originalDate in RecurrenceEngine.occurrencesBetween(rule, originalDate, originalDate),
                ) { failureMessage }
            } else {
                val closed = records.filter {
                    it.state == OccurrenceState.Completed.name || it.state == OccurrenceState.Skipped.name
                }
                val latest = closed.maxByOrNull {
                    it.completedAtMillis ?: it.scheduledEpochDay * 86_400_000L
                }
                val closedOn = latest?.completedAtMillis?.let { completedAt ->
                    Instant.ofEpochMilli(completedAt).atZone(clock.zoneId()).toLocalDate()
                } ?: latest?.scheduledEpochDay?.let(LocalDate::ofEpochDay)
                require(
                    RecurrenceEngine.nextCompletionRelative(rule, closedOn, closed.size) == originalDate,
                ) { failureMessage }
            }
        } else {
            require(
                expected.occurrenceState == OccurrenceState.Open &&
                    currentOccurrence.state == OccurrenceState.Open.name &&
                    currentOccurrence.scheduledEpochDay == expected.scheduledEpochDay &&
                    currentOccurrence.completedAtMillis == expected.completedAtMillis,
            ) { failureMessage }
        }
        val task = existing.toDomain().copy(
            steps = dao.getSteps(expected.taskId).map(TaskStepEntity::toDomain),
        )
        val currentStates = dao.getStepStates(expected.taskId, expected.originalEpochDay)
            .associateBy(TaskStepStateEntity::stepId)
        val visibleSteps = visibleTaskStepsForOccurrence(
            steps = task.steps,
            snapshots = dao.getStepSnapshotsForTask(expected.taskId).map(TaskStepSnapshotEntity::toDomain),
            occurrenceKey = expected.originalEpochDay,
            policy = task.repeatStepPolicy,
        )
        val currentSubtasks = visibleSteps.map { step ->
            val state = currentStates[step.id]
            TaskEditSubtaskBoundary(
                stepId = step.id,
                completed = state?.completed == true,
                completedAtMillis = state?.completedAtMillis,
                title = state?.titleSnapshot ?: step.title,
            )
        }
        require(currentSubtasks == expected.subtasks) { failureMessage }
        return existing
    }

    private suspend fun syncSteps(taskId: Long, draft: TaskDraft, now: Long) {
        val existing = dao.getSteps(taskId).associateBy(TaskStepEntity::id)
        val retainedIds = mutableSetOf<Long>()

        draft.steps
            .filter { it.title.isNotBlank() }
            .sortedBy { it.position }
            .forEachIndexed { index, step ->
                val current = step.id?.let(existing::get)
                if (current == null) {
                    dao.insertStep(
                        TaskStepEntity(
                            uuid = UUID.randomUUID().toString(),
                            taskId = taskId,
                            title = step.title.trim(),
                            position = index,
                            notes = step.notes.trim(),
                            archived = false,
                            createdAtMillis = now,
                            updatedAtMillis = now,
                        ),
                    )
                } else {
                    retainedIds += current.id
                    dao.updateStep(
                        current.copy(
                            title = step.title.trim(),
                            position = index,
                            notes = step.notes.trim(),
                            archived = false,
                            updatedAtMillis = now,
                        ),
                    )
                }
            }

        existing.values
            .filter { it.id !in retainedIds && !it.archived }
            .forEach { removed ->
                dao.updateStep(removed.copy(archived = true, updatedAtMillis = now))
            }
    }

    private suspend fun futureStepIdMap(
        oldTaskId: Long,
        newTaskId: Long,
        futureDraft: TaskDraft,
    ): Map<Long, Long> {
        val oldStepIds = dao.getSteps(oldTaskId)
            .filterNot(TaskStepEntity::archived)
            .mapTo(mutableSetOf(), TaskStepEntity::id)
        val futureDraftPositionByOldId = futureDraft.steps
            .filter { it.title.isNotBlank() }
            .sortedBy { it.position }
            .mapIndexedNotNull { normalizedPosition, step ->
                step.id?.takeIf(oldStepIds::contains)?.let { it to normalizedPosition }
            }
            .toMap()
        val newStepByPosition = dao.getSteps(newTaskId)
            .filterNot(TaskStepEntity::archived)
            .associateBy(TaskStepEntity::position)
        return futureDraftPositionByOldId.mapNotNull { (oldStepId, newPosition) ->
            newStepByPosition[newPosition]?.id?.let { oldStepId to it }
        }.toMap()
    }

    private suspend fun migrateOpenFutureState(
        oldTaskId: Long,
        newTaskId: Long,
        boundary: LocalDate,
        futureDraft: TaskDraft,
        newStepByOldId: Map<Long, Long>,
    ) {
        val boundaryKey = boundary.toEpochDay()
        val futureOccurrences = dao.getOccurrences(oldTaskId)
            .filter { it.originalEpochDay >= boundaryKey }
        require(futureOccurrences.all { it.state == OccurrenceState.Open.name }) {
            "A later occurrence already has recorded history. Edit the series after that history instead."
        }
        val futureStates = dao.getAllStepStates().filter {
            it.taskId == oldTaskId && it.occurrenceKey >= boundaryKey
        }
        val futureSnapshots = dao.getAllStepSnapshots().filter {
            it.taskId == oldTaskId && it.occurrenceKey >= boundaryKey
        }
        require(futureSnapshots.isEmpty()) {
            "A later occurrence already has recorded Subtask history. Edit the series after that history instead."
        }
        if (futureDraft.scheduleKind != ScheduleKind.Recurring) {
            require(
                futureOccurrences.all { it.originalEpochDay == boundaryKey } &&
                    futureStates.all { it.occurrenceKey == boundaryKey },
            ) {
                "Future occurrence changes must be reset before ending this recurring series."
            }
        }

        if (futureDraft.scheduleKind == ScheduleKind.Recurring) {
            futureOccurrences.forEach { occurrence ->
                dao.upsertOccurrence(occurrence.copy(taskId = newTaskId))
            }
            val migratedOccurrenceKeys = futureOccurrences
                .mapTo(mutableSetOf(), TaskOccurrenceEntity::originalEpochDay)
            futureStates
                .asSequence()
                .filter { it.stepId in newStepByOldId }
                .map(TaskStepStateEntity::occurrenceKey)
                .distinct()
                .filterNot(migratedOccurrenceKeys::contains)
                .forEach { occurrenceKey ->
                    // A Subtask toggle intentionally does not create an occurrence row. Once a
                    // series is split onto a different cadence, however, that virtual occurrence
                    // might no longer be generated. Persist an Open override so the authored
                    // state remains reachable in Today/Upcoming instead of becoming orphaned.
                    dao.upsertOccurrence(
                        TaskOccurrenceEntity(
                            taskId = newTaskId,
                            originalEpochDay = occurrenceKey,
                            scheduledEpochDay = occurrenceKey,
                            state = OccurrenceState.Open.name,
                            completedAtMillis = null,
                        ),
                    )
                }
        }
        val newOccurrenceKey = when (futureDraft.scheduleKind) {
            ScheduleKind.Recurring -> null
            ScheduleKind.Once -> requireNotNull(futureDraft.date).toEpochDay()
            ScheduleKind.Anytime -> com.whip.app.domain.ANYTIME_TASK_OCCURRENCE_KEY
        }
        futureStates.forEach { state ->
            val newStepId = newStepByOldId[state.stepId] ?: return@forEach
            dao.upsertStepState(
                state.copy(
                    stepId = newStepId,
                    taskId = newTaskId,
                    occurrenceKey = newOccurrenceKey ?: state.occurrenceKey,
                ),
            )
        }
        dao.deleteOccurrencesFrom(oldTaskId, boundaryKey)
        dao.deleteStepStatesFrom(oldTaskId, boundaryKey)

        if (
            futureDraft.scheduleKind == ScheduleKind.Recurring &&
            futureDraft.repeatStepPolicy == com.whip.app.domain.RepeatStepPolicy.CarryUnfinished
        ) {
            val priorSnapshotsByStep = dao.getStepSnapshotsForTask(oldTaskId)
                .filter { it.occurrenceKey < boundaryKey }
                .groupBy(TaskStepSnapshotEntity::stepId)
                .mapValues { (_, snapshots) -> snapshots.maxBy(TaskStepSnapshotEntity::occurrenceKey) }
                .values
            val newSteps = dao.getSteps(newTaskId).associateBy(TaskStepEntity::id)
            priorSnapshotsByStep.forEach { snapshot ->
                val newStepId = newStepByOldId[snapshot.stepId] ?: return@forEach
                val newStep = newSteps[newStepId] ?: return@forEach
                dao.upsertStepSnapshot(
                    snapshot.copy(
                        taskId = newTaskId,
                        stepId = newStepId,
                        position = newStep.position,
                    ),
                )
            }
        }
    }

    private suspend fun copyFutureSeriesIntegrations(
        oldTaskId: Long,
        newTaskId: Long,
        boundary: LocalDate,
        now: Long,
        newStepByOldId: Map<Long, Long>,
    ) {
        val linkDao = database.linkDao()

        suspend fun copyLinkRuleDetails(oldRuleId: Long, newRuleId: Long) {
            val conditions = linkDao.getRuleConditions(oldRuleId)
            val choices = conditions.takeIf { it.isNotEmpty() }
                ?.let { linkDao.getLinkConditionChoices(it.map { condition -> condition.id }) }
                .orEmpty()
            conditions.forEach { condition ->
                val newConditionId = linkDao.insertRuleCondition(
                    condition.copy(id = 0, linkRuleId = newRuleId),
                )
                choices.filter { it.conditionId == condition.id }.forEach { choice ->
                    linkDao.insertLinkConditionChoice(choice.copy(conditionId = newConditionId))
                }
            }
        }

        suspend fun copyTriggerDetails(oldRuleId: Long, newRuleId: Long) {
            val conditions = linkDao.getTriggerConditions(oldRuleId)
            val choices = conditions.takeIf { it.isNotEmpty() }
                ?.let { linkDao.getTriggerConditionChoices(it.map { condition -> condition.id }) }
                .orEmpty()
            conditions.forEach { condition ->
                val newConditionId = linkDao.insertTriggerCondition(
                    condition.copy(id = 0, triggerRuleId = newRuleId),
                )
                choices.filter { it.conditionId == condition.id }.forEach { choice ->
                    linkDao.insertTriggerConditionChoice(choice.copy(conditionId = newConditionId))
                }
            }
            linkDao.getTriggerMappings(oldRuleId).forEach { mapping ->
                linkDao.insertTriggerMapping(mapping.copy(id = 0, triggerRuleId = newRuleId))
            }
        }

        linkDao.getRules()
            .filter { it.sourceEntityId == oldTaskId && it.sourceType in setOf("Task", "Subtask") }
            .forEach { rule ->
                val newSourceItemId = if (rule.sourceType == "Subtask") {
                    rule.sourceItemId?.let(newStepByOldId::get)
                } else null
                if (rule.sourceType != "Subtask" || rule.sourceItemId == null || newSourceItemId != null) {
                    val newRuleId = linkDao.insertRule(
                        rule.copy(
                            id = 0,
                            uuid = UUID.randomUUID().toString(),
                            name = "${rule.name} (future series)",
                            sourceEntityId = newTaskId,
                            sourceItemId = newSourceItemId,
                            retroactiveFromEpochDay = maxOf(
                                boundary.toEpochDay(),
                                rule.retroactiveFromEpochDay ?: boundary.toEpochDay(),
                            ),
                            createdAtMillis = now,
                            updatedAtMillis = now,
                        ),
                    )
                    copyLinkRuleDetails(rule.id, newRuleId)
                }
            }
        val triggerRules = linkDao.getTriggerRules()
        triggerRules
            .filter { rule ->
                rule.sourceEntityId == oldTaskId && rule.sourceType in setOf("Task", "Subtask")
            }
            .forEach { rule ->
                val newSourceItemId = if (rule.sourceType == "Subtask") {
                    rule.sourceItemId?.let(newStepByOldId::get)
                } else null
                if (rule.sourceType == "Subtask" && (rule.sourceItemId == null || newSourceItemId == null)) {
                    return@forEach
                }
                val newRuleId = linkDao.insertTriggerRule(
                    rule.copy(
                        id = 0,
                        uuid = UUID.randomUUID().toString(),
                        name = "${rule.name} (future series)",
                        sourceEntityId = newTaskId,
                        sourceItemId = newSourceItemId,
                        targetEntityId = if (rule.targetType == "Task" && rule.targetEntityId == oldTaskId) newTaskId else rule.targetEntityId,
                        createdAtMillis = now,
                        updatedAtMillis = now,
                    ),
                )
                copyTriggerDetails(rule.id, newRuleId)
            }
        triggerRules
            .filter { rule ->
                rule.targetType == "Task" &&
                    rule.targetEntityId == oldTaskId &&
                    !(rule.sourceEntityId == oldTaskId && rule.sourceType in setOf("Task", "Subtask"))
            }
            .forEach { rule ->
                linkDao.updateTriggerRule(
                    rule.copy(targetEntityId = newTaskId, updatedAtMillis = now),
                )
            }
    }

    private suspend fun completeWithinTransaction(item: ScheduledTask, now: Long) {
        requireCurrentOpenSchedule(item, "Task changed before it could be completed")
        snapshotSteps(item)
        if (item.task.scheduleKind == ScheduleKind.Recurring) {
            dao.upsertOccurrence(
                TaskOccurrence(
                    taskId = item.task.id,
                    originalDate = requireNotNull(item.originalDate),
                    scheduledDate = requireNotNull(item.scheduledDate),
                    state = OccurrenceState.Completed,
                    completedAtMillis = now,
                ).toEntity(),
            )
        } else {
            val existing = dao.getTask(item.task.id) ?: return
            dao.updateTask(existing.copy(completedAtMillis = now, updatedAtMillis = now))
        }
    }

    private suspend fun snapshotSteps(item: ScheduledTask) {
        val states = dao.getStepStates(item.task.id, item.occurrenceKey).associateBy { it.stepId }
        val visibleStepIds = item.subtasks.mapTo(mutableSetOf()) { it.step.id }
        val steps = dao.getSteps(item.task.id).filter { it.id in visibleStepIds }
        steps.forEach { step ->
            val state = states[step.id]
            dao.upsertStepSnapshot(
                TaskStepSnapshotEntity(
                    taskId = item.task.id,
                    occurrenceKey = item.occurrenceKey,
                    stepId = step.id,
                    title = state?.titleSnapshot ?: step.title,
                    position = step.position,
                    notes = step.notes,
                    completed = state?.completed == true,
                    completedAtMillis = state?.completedAtMillis,
                ),
            )
        }
    }
}

private fun List<TaskStepEntity>.matches(drafts: List<com.whip.app.domain.TaskStepDraft>): Boolean {
    val active = filterNot(TaskStepEntity::archived).sortedBy(TaskStepEntity::position)
    val normalized = drafts.filter { it.title.isNotBlank() }.sortedBy { it.position }
    if (active.size != normalized.size) return false
    return active.zip(normalized).withIndex().all { (index, pair) ->
        val (stored, draft) = pair
        stored.title == draft.title.trim() &&
            stored.notes == draft.notes.trim() &&
            stored.position == index
    }
}
