package com.whip.app.data

import androidx.room.withTransaction
import com.whip.app.core.WhipClock
import com.whip.app.core.UuidWhipIdGenerator
import com.whip.app.domain.OccurrenceState
import com.whip.app.domain.RecurrenceEnd
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.ScheduledSubtask
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskOccurrence
import com.whip.app.domain.TaskStep
import com.whip.app.domain.TaskStepState
import com.whip.app.domain.TaskStepSnapshot
import com.whip.app.domain.TaskPriority
import com.whip.app.domain.TaskEffort
import com.whip.app.domain.WhipTask
import com.whip.app.domain.toDraft
import java.time.LocalDate
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
    suspend fun reopenOccurrence(item: ScheduledTask)
    suspend fun resetOccurrence(taskId: Long, originalDate: LocalDate): Boolean
    suspend fun setPinned(taskId: Long, pinned: Boolean)
    suspend fun setInbox(taskId: Long, inbox: Boolean)
    suspend fun duplicate(taskId: Long): Long
    suspend fun completeAll(items: List<ScheduledTask>)
    suspend fun rescheduleAll(items: List<ScheduledTask>, newDate: LocalDate)
    suspend fun planAll(items: List<ScheduledTask>, newDate: LocalDate)
    suspend fun restoreSchedules(items: List<ScheduledTask>)
    suspend fun restorePlan(items: List<ScheduledTask>, originalInboxTaskIds: Set<Long>)
    suspend fun archiveAll(taskIds: List<Long>)
    suspend fun restoreAll(taskIds: List<Long>)
    suspend fun setPinnedAll(taskIds: List<Long>, pinned: Boolean)
    suspend fun setInboxAll(taskIds: List<Long>, inbox: Boolean)
    suspend fun updateMetadataAll(taskIds: List<Long>, edit: TaskBulkEdit)
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
    val inbox: Boolean? = null,
)

class RoomTaskRepository(
    private val database: WhipDatabase,
    private val clock: WhipClock,
) : TaskRepository {
    private val dao = database.taskDao()
    private val areaRepository = RoomAreaRepository(database, clock, UuidWhipIdGenerator)

    override val tasks: Flow<List<WhipTask>> = dao.observeAllTasks().map { tasks ->
        tasks.map(TaskEntity::toDomain)
    }

    override val occurrences: Flow<List<TaskOccurrence>> =
        dao.observeOccurrences().map { occurrences ->
            occurrences.map(TaskOccurrenceEntity::toDomain)
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
            val adjustedDraft = if (
                fromOccurrence != null && draft.scheduleKind == ScheduleKind.Recurring
            ) {
                draft.copy(
                    date = fromOccurrence,
                    recurrence = requireNotNull(draft.recurrence).copy(startDate = fromOccurrence),
                )
            } else {
                draft
            }
            val selectedArea = areaRepository.resolve(adjustedDraft.areaId, adjustedDraft.area)
            val resolvedDraft = adjustedDraft.copy(areaId = selectedArea.id, area = selectedArea.name)
            val now = clock.now().toEpochMilli()
            val existingTask = existing.toDomain().copy(
                steps = dao.getSteps(taskId).map(TaskStepEntity::toDomain),
            )
            val shouldSplitFuture = fromOccurrence != null &&
                existingTask.scheduleKind == ScheduleKind.Recurring &&
                resolvedDraft.scheduleKind == ScheduleKind.Recurring &&
                fromOccurrence.isAfter(requireNotNull(existingTask.recurrence).startDate)
            if (shouldSplitFuture) {
                val boundary = requireNotNull(fromOccurrence)
                val oldRule = requireNotNull(existingTask.recurrence)
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
                    resolvedDraft.toEntity(createdAtMillis = now, manualPosition = dao.nextManualPosition())
                        .copy(pinned = existing.pinned),
                )
                syncSteps(futureId, resolvedDraft, now)
                copyFutureSeriesIntegrations(taskId, futureId, boundary, now)
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
        val item = ScheduledTask(
            task = task,
            originalDate = original,
            scheduledDate = scheduled,
            subtasks = task.steps.filterNot(TaskStep::archived).map { step ->
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

    override suspend fun reschedule(item: ScheduledTask, newDate: LocalDate) {
        database.withTransaction {
            if (item.task.scheduleKind == ScheduleKind.Recurring) {
                dao.upsertOccurrence(
                    TaskOccurrence(
                        taskId = item.task.id,
                        originalDate = requireNotNull(item.originalDate),
                        scheduledDate = newDate,
                        state = OccurrenceState.Open,
                        completedAtMillis = null,
                    ).toEntity(),
                )
            } else {
                val existing = requireNotNull(dao.getTask(item.task.id)) { "Task no longer exists" }
                dao.updateTask(
                    existing.copy(
                        scheduleKind = ScheduleKind.Once.name,
                        dateEpochDay = newDate.toEpochDay(),
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

    override suspend fun reopenOccurrence(item: ScheduledTask) {
        require(item.task.scheduleKind == ScheduleKind.Recurring) {
            "Only a recurring occurrence can be reopened this way"
        }
        database.withTransaction {
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

    override suspend fun setPinned(taskId: Long, pinned: Boolean) {
        val existing = dao.getTask(taskId) ?: return
        dao.updateTask(existing.copy(pinned = pinned, updatedAtMillis = clock.now().toEpochMilli()))
    }

    override suspend fun setInbox(taskId: Long, inbox: Boolean) {
        val existing = dao.getTask(taskId) ?: return
        dao.updateTask(
            existing.copy(
                inbox = inbox && existing.scheduleKind == ScheduleKind.Anytime.name,
                updatedAtMillis = clock.now().toEpochMilli(),
            ),
        )
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
        unique.map { it.task.id }.distinct().forEach { setInbox(it, false) }
    }

    override suspend fun restoreSchedules(items: List<ScheduledTask>) = database.withTransaction {
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
                        completedAtMillis = item.task.completedAtMillis,
                        updatedAtMillis = now,
                    ),
                )
            }
        }
    }

    override suspend fun restorePlan(
        items: List<ScheduledTask>,
        originalInboxTaskIds: Set<Long>,
    ) = database.withTransaction {
        restoreSchedules(items)
        val ids = items.map { it.task.id }.distinct()
        ids.forEach { setInbox(it, false) }
        originalInboxTaskIds.forEach { setInbox(it, true) }
    }

    override suspend fun archiveAll(taskIds: List<Long>) = database.withTransaction {
        taskIds.distinct().forEach { archive(it) }
    }

    override suspend fun restoreAll(taskIds: List<Long>) = database.withTransaction {
        taskIds.distinct().forEach { restore(it) }
    }

    override suspend fun setPinnedAll(taskIds: List<Long>, pinned: Boolean) = database.withTransaction {
        taskIds.distinct().forEach { setPinned(it, pinned) }
    }

    override suspend fun setInboxAll(taskIds: List<Long>, inbox: Boolean) = database.withTransaction {
        taskIds.distinct().forEach { setInbox(it, inbox) }
    }

    override suspend fun updateMetadataAll(taskIds: List<Long>, edit: TaskBulkEdit) = database.withTransaction {
        val now = clock.now().toEpochMilli()
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
                    inbox = edit.inbox?.let { requested ->
                        requested && existing.scheduleKind == ScheduleKind.Anytime.name
                    } ?: existing.inbox,
                    updatedAtMillis = now,
                ),
            )
        }
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

    private suspend fun copyFutureSeriesIntegrations(
        oldTaskId: Long,
        newTaskId: Long,
        boundary: LocalDate,
        now: Long,
    ) {
        val linkDao = database.linkDao()
        val oldSteps = dao.getSteps(oldTaskId).filterNot(TaskStepEntity::archived)
        val newSteps = dao.getSteps(newTaskId).filterNot(TaskStepEntity::archived)
        val newStepByOldId = oldSteps.mapNotNull { old ->
            newSteps.firstOrNull { it.position == old.position }?.id?.let { old.id to it }
        }.toMap()
        linkDao.getRules()
            .filter { it.sourceEntityId == oldTaskId && it.sourceType in setOf("Task", "Subtask") }
            .forEach { rule ->
                val newSourceItemId = if (rule.sourceType == "Subtask") {
                    rule.sourceItemId?.let(newStepByOldId::get)
                } else null
                if (rule.sourceType != "Subtask" || rule.sourceItemId == null || newSourceItemId != null) {
                    linkDao.insertRule(
                        rule.copy(
                            id = 0,
                            uuid = UUID.randomUUID().toString(),
                            name = "${rule.name} (future series)",
                            sourceEntityId = newTaskId,
                            sourceItemId = newSourceItemId,
                            retroactiveFromEpochDay = boundary.toEpochDay(),
                            createdAtMillis = now,
                            updatedAtMillis = now,
                        ),
                    )
                }
            }
        linkDao.getTriggerRules()
            .filter {
                (it.sourceType == "Task" && it.sourceEntityId == oldTaskId) ||
                    (it.targetType == "Task" && it.targetEntityId == oldTaskId)
            }
            .forEach { rule ->
                linkDao.insertTriggerRule(
                    rule.copy(
                        id = 0,
                        uuid = UUID.randomUUID().toString(),
                        name = "${rule.name} (future series)",
                        sourceEntityId = if (rule.sourceType == "Task" && rule.sourceEntityId == oldTaskId) newTaskId else rule.sourceEntityId,
                        targetEntityId = if (rule.targetType == "Task" && rule.targetEntityId == oldTaskId) newTaskId else rule.targetEntityId,
                        createdAtMillis = now,
                        updatedAtMillis = now,
                    ),
                )
            }
    }

    private suspend fun completeWithinTransaction(item: ScheduledTask, now: Long) {
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
