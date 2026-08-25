package com.whip.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.WhipClock
import com.whip.app.data.RoomTaskRepository
import com.whip.app.data.WhipDatabase
import com.whip.app.data.TriggerRuleEntity
import com.whip.app.data.TaskBulkEdit
import com.whip.app.domain.OccurrenceState
import com.whip.app.domain.RecurrenceRule
import com.whip.app.domain.RecurrenceUnit
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.ScheduledSubtask
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskStepDraft
import com.whip.app.domain.TaskPriority
import com.whip.app.domain.TaskEffort
import com.whip.app.domain.MissedOccurrencePolicy
import com.whip.app.domain.RecurrenceAnchor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskRepositoryTest {
    private lateinit var database: WhipDatabase
    private lateinit var repository: RoomTaskRepository
    private val monday = LocalDate.of(2026, 8, 17)

    @Before
    fun setUp() {
        FixedClock.current = Instant.parse("2026-08-17T16:00:00Z")
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WhipDatabase::class.java,
        ).build()
        repository = RoomTaskRepository(database, FixedClock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun stepStateIsPerOccurrenceAndAllStepsCanAutoCompleteParent() = runBlocking {
        val taskId = repository.create(recurringDraft(autoComplete = true))
        val task = requireNotNull(repository.getTask(taskId))
        val mondayItem = item(task, monday)
        val tuesdayItem = item(task, monday.plusDays(1))

        assertFalse(repository.setStepCompleted(mondayItem, task.steps[0].id, true))
        assertTrue(
            repository.stepStates.first().none {
                it.occurrenceKey == tuesdayItem.occurrenceKey && it.completed
            },
        )
        assertTrue(repository.setStepCompleted(mondayItem, task.steps[1].id, true))

        val completedOccurrence = repository.occurrences.first().single()
        assertEquals(OccurrenceState.Completed, completedOccurrence.state)
        assertEquals(monday, completedOccurrence.originalDate)
    }

    @Test
    fun manualCompletionSnapshotsNamesAndUnfinishedProgress() = runBlocking {
        val taskId = repository.create(recurringDraft(autoComplete = false))
        val task = requireNotNull(repository.getTask(taskId))
        val item = item(task, monday)

        repository.setStepCompleted(item, task.steps[0].id, true)
        repository.complete(item)
        repository.update(
            taskId,
            recurringDraft(autoComplete = false).copy(
                steps = listOf(
                    TaskStepDraft(task.steps[0].id, "Renamed", 0),
                ),
            ),
        )

        val snapshots = repository.stepSnapshots.first()
        assertEquals(listOf("First", "Second"), snapshots.map { it.title })
        assertEquals(listOf(true, false), snapshots.map { it.completed })
    }

    @Test
    fun reminderQueryExcludesOrdinaryAndArchivedTasks() = runBlocking {
        val enabledId = repository.create(
            TaskDraft(
                title = "Reminder",
                scheduleKind = ScheduleKind.Once,
                date = monday,
                timeMinutes = 9 * 60,
                reminderEnabled = true,
            ),
        )
        repository.create(
            TaskDraft(
                title = "No reminder",
                scheduleKind = ScheduleKind.Once,
                date = monday,
                timeMinutes = 10 * 60,
                reminderEnabled = false,
            ),
        )
        val archivedId = repository.create(
            TaskDraft(
                title = "Archived reminder",
                scheduleKind = ScheduleKind.Once,
                date = monday,
                timeMinutes = 11 * 60,
                reminderEnabled = true,
            ),
        )
        repository.archive(archivedId)

        assertEquals(listOf(enabledId), database.taskDao().getReminderTaskIds())

    }

    @Test
    fun promoteStepCreatesInboxTaskAndArchivesDefinition() = runBlocking {
        val parentId = repository.create(recurringDraft(autoComplete = false))
        val parent = requireNotNull(repository.getTask(parentId))

        val promotedId = repository.promoteStep(item(parent, monday), parent.steps[0].id)
        val promoted = requireNotNull(repository.getTask(promotedId))
        val refreshedParent = requireNotNull(repository.getTask(parentId))

        assertEquals("First", promoted.title)
        assertEquals(parent.icon, promoted.icon)
        assertEquals("From task: Recurring\n\nUseful context", promoted.notes)
        assertEquals(ScheduleKind.Anytime, promoted.scheduleKind)
        assertTrue(refreshedParent.steps.first { it.id == parent.steps[0].id }.archived)
    }

    @Test
    fun archivedTaskCanBeRestoredWithItsSteps() = runBlocking {
        val taskId = repository.create(recurringDraft(autoComplete = false))
        repository.archive(taskId)
        assertTrue(requireNotNull(repository.getTask(taskId)).archived)
        assertEquals(2, requireNotNull(repository.getTask(taskId)).steps.size)

        repository.restore(taskId)
        assertFalse(requireNotNull(repository.getTask(taskId)).archived)
        assertEquals(listOf("First", "Second"), requireNotNull(repository.getTask(taskId)).steps.map { it.title })
    }

    @Test
    fun permanentDeleteCascadesSubtasksOccurrenceStateAndHistory() = runBlocking {
        val taskId = repository.create(recurringDraft(autoComplete = false))
        val task = requireNotNull(repository.getTask(taskId))
        val scheduled = item(task, monday)
        repository.setStepCompleted(scheduled, task.steps.first().id, true)
        repository.complete(scheduled)

        assertTrue(repository.occurrences.first().isNotEmpty())
        assertTrue(repository.stepStates.first().isNotEmpty())
        assertTrue(repository.stepSnapshots.first().isNotEmpty())
        assertTrue(repository.deletePermanently(taskId))

        assertNull(repository.getTask(taskId))
        assertTrue(repository.occurrences.first().isEmpty())
        assertTrue(repository.steps.first().isEmpty())
        assertTrue(repository.stepStates.first().isEmpty())
        assertTrue(repository.stepSnapshots.first().isEmpty())
        assertFalse(repository.deletePermanently(taskId))
    }

    @Test
    fun completedRecurringOccurrenceCanBeReopenedWithoutLosingItsStepState() = runBlocking {
        val taskId = repository.create(recurringDraft(autoComplete = false))
        val task = requireNotNull(repository.getTask(taskId))
        val scheduled = item(task, monday)
        repository.setStepCompleted(scheduled, task.steps.first().id, true)
        repository.complete(scheduled)

        repository.reopenOccurrence(scheduled.copy(completedAtMillis = FixedClock.now().toEpochMilli()))

        assertEquals(OccurrenceState.Open, repository.occurrences.first().single().state)
        assertTrue(repository.stepSnapshots.first().isEmpty())
        assertTrue(repository.stepStates.first().single { it.stepId == task.steps.first().id }.completed)
    }

    @Test
    fun skippedAndMovedOccurrencesCanReturnToTheSeriesSchedule() = runBlocking {
        val taskId = repository.create(recurringDraft(autoComplete = false))
        val task = requireNotNull(repository.getTask(taskId))
        val mondayItem = item(task, monday)

        repository.skip(mondayItem)
        assertEquals(OccurrenceState.Skipped, repository.occurrences.first().single().state)
        assertTrue(repository.resetOccurrence(taskId, monday))
        assertTrue(repository.occurrences.first().isEmpty())

        repository.reschedule(mondayItem, monday.plusDays(3))
        assertEquals(monday.plusDays(3), repository.occurrences.first().single().scheduledDate)
        assertTrue(repository.resetOccurrence(taskId, monday))
        assertTrue(repository.occurrences.first().isEmpty())
        assertFalse(repository.resetOccurrence(taskId, monday))
    }

    @Test
    fun editThisAndFutureSplitsSeriesWithoutRewritingRecordedHistory() = runBlocking {
        val oldTaskId = repository.create(recurringDraft(autoComplete = false))
        val oldTask = requireNotNull(repository.getTask(oldTaskId))
        repository.complete(item(oldTask, monday))
        repository.skip(item(oldTask, monday.plusDays(1)))
        database.linkDao().insertTriggerRule(
            TriggerRuleEntity(
                uuid = "task-trigger",
                name = "Continue after recurring task",
                sourceType = "Task",
                sourceEntityId = oldTaskId,
                outcome = "Completed",
                targetType = "Habit",
                targetEntityId = 99,
                delayMinutes = 0,
                quietStartMinutes = null,
                quietEndMinutes = null,
                action = "PromptHabit",
                notificationEnabled = false,
                conditionMode = "MatchAll",
                enabled = true,
                createdAtMillis = 1,
                updatedAtMillis = 1,
            ),
        )
        val boundary = monday.plusDays(2)
        val futureDraft = recurringDraft(autoComplete = false).copy(
            title = "Changed future",
            recurrence = requireNotNull(recurringDraft(false).recurrence).copy(
                interval = 2,
                startDate = boundary,
            ),
            steps = listOf(TaskStepDraft(title = "New future step", position = 0)),
        )

        val futureTaskId = repository.update(oldTaskId, futureDraft, boundary)

        assertTrue(futureTaskId != oldTaskId)
        val historical = requireNotNull(repository.getTask(oldTaskId))
        val future = requireNotNull(repository.getTask(futureTaskId))
        assertEquals(monday.plusDays(1), historical.recurrence?.endDate)
        assertEquals("Recurring", historical.title)
        assertEquals(listOf(OccurrenceState.Completed, OccurrenceState.Skipped), repository.getOccurrences(oldTaskId).map { it.state })
        assertEquals("Changed future", future.title)
        assertEquals(boundary, future.recurrence?.startDate)
        assertEquals(listOf("New future step"), future.steps.map { it.title })
        val triggers = database.linkDao().getTriggerRules()
        assertEquals(setOf(oldTaskId, futureTaskId), triggers.map { it.sourceEntityId }.toSet())
        assertTrue(triggers.any { it.sourceEntityId == futureTaskId && "future series" in it.name })
    }

    @Test
    fun powerTaskFieldsPersistAndUpdateTogether() = runBlocking {
        val draft = TaskDraft(
            title = "Plan launch",
            icon = "🚀",
            scheduleKind = ScheduleKind.Recurring,
            recurrence = RecurrenceRule(
                unit = RecurrenceUnit.Months,
                interval = 2,
                startDate = monday,
                anchor = RecurrenceAnchor.Completion,
            ),
            timeMinutes = 9 * 60,
            reminderEnabled = true,
            reminderOffsetsMinutes = listOf(1_440, 30, 0),
            priority = TaskPriority.Urgent,
            area = "Work",
            tags = setOf("launch", "client"),
            deadline = monday.plusDays(10),
        )

        val task = requireNotNull(repository.getTask(repository.create(draft)))
        assertEquals("🚀", task.icon)
        assertEquals(TaskPriority.Urgent, task.priority)
        assertEquals("Work", task.area)
        assertEquals(setOf("launch", "client"), task.tags)
        assertEquals(listOf(1_440, 30, 0), task.reminderOffsetsMinutes)
        assertEquals(RecurrenceAnchor.Completion, task.recurrence?.anchor)
        assertEquals(monday.plusDays(10), task.deadline)
    }

    @Test
    fun currentOnlyPolicyDoesNotManufactureOccurrenceHistory() = runBlocking {
        val taskId = repository.create(
            recurringDraft(autoComplete = false).copy(
                missedOccurrencePolicy = MissedOccurrencePolicy.CurrentOnly,
            ),
        )
        assertTrue(repository.getOccurrences(taskId).isEmpty())
    }

    @Test
    fun explicitSkipRecordsTheRealClosureTime() = runBlocking {
        val taskId = repository.create(recurringDraft(autoComplete = false))
        val task = requireNotNull(repository.getTask(taskId))
        val expected = FixedClock.now().toEpochMilli()

        repository.skip(item(task, monday))

        val skipped = repository.getOccurrences(taskId).single()
        assertEquals(OccurrenceState.Skipped, skipped.state)
        assertEquals(expected, skipped.completedAtMillis)
    }

    @Test
    fun repeatedNotificationCompletionDoesNotRewriteCompletionTime() = runBlocking {
        val taskId = repository.create(recurringDraft(autoComplete = false).copy(steps = emptyList()))
        repository.completeOccurrence(taskId, monday)
        val firstTimestamp = repository.getOccurrences(taskId).single().completedAtMillis
        FixedClock.current = FixedClock.current.plusSeconds(600)

        repository.completeOccurrence(taskId, monday)

        assertEquals(firstTimestamp, repository.getOccurrences(taskId).single().completedAtMillis)
    }

    @Test
    fun bulkRescheduleIsAtomicAndKeepsRecurringScopeAtOccurrenceLevel() = runBlocking {
        val onceId = repository.create(
            TaskDraft(title = "One shot", scheduleKind = ScheduleKind.Once, date = monday),
        )
        val recurringId = repository.create(recurringDraft(autoComplete = false))
        val once = requireNotNull(repository.getTask(onceId))
        val recurring = requireNotNull(repository.getTask(recurringId))

        val failed = runCatching {
            repository.rescheduleAll(
                listOf(
                    ScheduledTask(once, originalDate = monday, scheduledDate = monday),
                    ScheduledTask(recurring, originalDate = null, scheduledDate = monday),
                ),
                monday.plusDays(3),
            )
        }

        assertTrue(failed.isFailure)
        assertEquals(monday, requireNotNull(repository.getTask(onceId)).date)
        assertTrue(repository.getOccurrences(recurringId).isEmpty())

        repository.rescheduleAll(
            listOf(
                ScheduledTask(once, originalDate = monday, scheduledDate = monday),
                item(recurring, monday),
            ),
            monday.plusDays(4),
        )

        assertEquals(monday.plusDays(4), requireNotNull(repository.getTask(onceId)).date)
        assertEquals(monday.plusDays(4), repository.getOccurrences(recurringId).single().scheduledDate)
        assertEquals(monday, requireNotNull(repository.getTask(recurringId)).recurrence?.startDate)
    }

    @Test
    fun bulkMoveUndoRestoresInboxOnceAndRecurringSchedulesAtomically() = runBlocking {
        val inboxId = repository.create(TaskDraft(title = "Inbox"))
        val onceId = repository.create(TaskDraft(title = "Once", scheduleKind = ScheduleKind.Once, date = monday))
        val recurringId = repository.create(recurringDraft(autoComplete = false))
        val inbox = requireNotNull(repository.getTask(inboxId))
        val once = requireNotNull(repository.getTask(onceId))
        val recurring = requireNotNull(repository.getTask(recurringId))
        val originals = listOf(
            ScheduledTask(inbox, originalDate = null, scheduledDate = null),
            ScheduledTask(once, originalDate = monday, scheduledDate = monday),
            item(recurring, monday),
        )

        repository.rescheduleAll(originals, monday.plusDays(4))
        repository.restoreSchedules(originals)

        assertEquals(ScheduleKind.Anytime, requireNotNull(repository.getTask(inboxId)).scheduleKind)
        assertTrue(requireNotNull(repository.getTask(inboxId)).inbox)
        assertNull(requireNotNull(repository.getTask(inboxId)).date)
        assertEquals(monday, requireNotNull(repository.getTask(onceId)).date)
        assertTrue(repository.getOccurrences(recurringId).isEmpty())
    }

    @Test
    fun planMyDayAndItsUndoRestoreEveryUndatedTaskToInbox() = runBlocking {
        val inboxId = repository.create(TaskDraft(title = "Inbox", inbox = true))
        val legacyUndatedId = repository.create(TaskDraft(title = "Legacy undated", inbox = false))
        val storedLegacyUndated = requireNotNull(database.taskDao().getTask(legacyUndatedId))
        database.taskDao().updateTask(storedLegacyUndated.copy(inbox = false))
        assertFalse(requireNotNull(database.taskDao().getTask(legacyUndatedId)).inbox)
        val inbox = requireNotNull(repository.getTask(inboxId))
        val legacyUndated = requireNotNull(repository.getTask(legacyUndatedId))
        assertTrue(legacyUndated.inbox)
        val originals = listOf(
            ScheduledTask(inbox, originalDate = null, scheduledDate = null),
            ScheduledTask(legacyUndated, originalDate = null, scheduledDate = null),
        )

        repository.planAll(originals, monday)

        assertEquals(ScheduleKind.Once, requireNotNull(repository.getTask(inboxId)).scheduleKind)
        assertEquals(false, requireNotNull(repository.getTask(inboxId)).inbox)
        assertEquals(ScheduleKind.Once, requireNotNull(repository.getTask(legacyUndatedId)).scheduleKind)

        repository.restorePlan(originals)

        assertEquals(ScheduleKind.Anytime, requireNotNull(repository.getTask(inboxId)).scheduleKind)
        assertEquals(true, requireNotNull(repository.getTask(inboxId)).inbox)
        assertEquals(ScheduleKind.Anytime, requireNotNull(repository.getTask(legacyUndatedId)).scheduleKind)
        assertTrue(requireNotNull(repository.getTask(legacyUndatedId)).inbox)
    }

    @Test
    fun bulkMoveUndoRestoresAnAlreadyMovedRecurringOccurrence() = runBlocking {
        val recurringId = repository.create(recurringDraft(autoComplete = false))
        val recurring = requireNotNull(repository.getTask(recurringId))
        repository.reschedule(item(recurring, monday), monday.plusDays(2))
        val previouslyMoved = item(recurring, monday).copy(scheduledDate = monday.plusDays(2))

        repository.rescheduleAll(listOf(previouslyMoved), monday.plusDays(6))
        repository.restoreSchedules(listOf(previouslyMoved))

        assertEquals(monday.plusDays(2), repository.getOccurrences(recurringId).single().scheduledDate)
    }

    @Test
    fun bulkMetadataReplacesOnlySelectedFieldsAndKeepsPlacementSemantics() = runBlocking {
        val inboxId = repository.create(TaskDraft(title = "Inbox", area = "Old", tags = setOf("old")))
        val datedId = repository.create(TaskDraft(title = "Dated", scheduleKind = ScheduleKind.Once, date = monday, inbox = false))

        repository.updateMetadataAll(
            listOf(inboxId, datedId),
            TaskBulkEdit(
                updateArea = true,
                areaName = "Work",
                tags = setOf("Deep", "deep", "Today"),
                priority = TaskPriority.High,
                effort = TaskEffort.High,
            ),
        )

        val inbox = requireNotNull(repository.getTask(inboxId))
        val dated = requireNotNull(repository.getTask(datedId))
        assertEquals("Work", inbox.area)
        assertEquals(setOf("Deep", "Today"), inbox.tags)
        assertEquals(TaskPriority.High, inbox.priority)
        assertEquals(TaskEffort.High, inbox.effort)
        assertTrue(inbox.inbox)
        assertFalse(dated.inbox)
    }

    @Test
    fun bulkMetadataRollsBackEveryTaskWhenAnyTargetIsMissing() = runBlocking {
        val id = repository.create(TaskDraft(title = "Keep", area = "Original"))

        runCatching {
            repository.updateMetadataAll(listOf(id, Long.MAX_VALUE), TaskBulkEdit(updateArea = true, areaName = "Changed"))
        }

        assertEquals("Original", requireNotNull(repository.getTask(id)).area)
    }

    @Test
    fun manualOrderIsStableAndRejectsMissingTargetsAtomically() = runBlocking {
        val first = repository.create(TaskDraft(title = "First"))
        val second = repository.create(TaskDraft(title = "Second"))
        val third = repository.create(TaskDraft(title = "Third"))

        repository.reorderAll(listOf(third, first, second))
        assertEquals(
            listOf("Third", "First", "Second"),
            repository.tasks.first().sortedBy { it.manualPosition }.map { it.title },
        )
        runCatching { repository.reorderAll(listOf(second, Long.MAX_VALUE, third)) }
        assertEquals(
            listOf("Third", "First", "Second"),
            repository.tasks.first().sortedBy { it.manualPosition }.map { it.title },
        )
    }

    private fun recurringDraft(autoComplete: Boolean) = TaskDraft(
        title = "Recurring",
        scheduleKind = ScheduleKind.Recurring,
        date = monday,
        recurrence = RecurrenceRule(
            unit = RecurrenceUnit.Days,
            startDate = monday,
        ),
        steps = listOf(
            TaskStepDraft(title = "First", position = 0, notes = "Useful context"),
            TaskStepDraft(title = "Second", position = 1),
        ),
        autoCompleteFromSteps = autoComplete,
    )

    private fun item(task: com.whip.app.domain.WhipTask, date: LocalDate) = ScheduledTask(
        task = task,
        originalDate = date,
        scheduledDate = date,
        subtasks = task.steps.filterNot { it.archived }.map { step ->
            ScheduledSubtask(
                step = step,
                completed = false,
                completedAtMillis = null,
                title = step.title,
            )
        },
    )

    private object FixedClock : WhipClock {
        var current: Instant = Instant.parse("2026-08-17T16:00:00Z")
        override fun now(): Instant = current
        override fun today(zoneId: ZoneId): LocalDate = LocalDate.of(2026, 8, 17)
    }
}
