package com.whip.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.WhipClock
import com.whip.app.core.UuidWhipIdGenerator
import com.whip.app.data.RoomTrackRepository
import com.whip.app.data.RoomGoalRepository
import com.whip.app.data.RoomLinkRepository
import com.whip.app.data.RoomMeasurementRepository
import com.whip.app.data.RoomTaskRepository
import com.whip.app.data.LinkRuleConditionEntity
import com.whip.app.data.TriggerFieldMappingEntity
import com.whip.app.data.TriggerOccurrenceEntity
import com.whip.app.data.TriggerRuleConditionEntity
import com.whip.app.data.WhipDatabase
import com.whip.app.data.TriggerRuleEntity
import com.whip.app.data.TaskBulkEdit
import com.whip.app.domain.OccurrenceState
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalType
import com.whip.app.domain.LinkRuleDraft
import com.whip.app.domain.LinkSourceMetric
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.RecurrenceRule
import com.whip.app.domain.RecurrenceEnd
import com.whip.app.domain.RecurrenceEngine
import com.whip.app.domain.RecurrenceUnit
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.ScheduledSubtask
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskStepDraft
import com.whip.app.domain.TaskPriority
import com.whip.app.domain.TaskEffort
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.toDraft
import com.whip.app.domain.toEditBoundary
import com.whip.app.domain.visibleTaskStepsForOccurrence
import com.whip.app.domain.MissedOccurrencePolicy
import com.whip.app.domain.RecurrenceAnchor
import com.whip.app.domain.RepeatStepPolicy
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    fun invalidTaskDraftsAreRejectedWithoutPartialRows() = runBlocking {
        val invalidDrafts = listOf(
            TaskDraft(title = " "),
            TaskDraft(title = "x".repeat(201)),
            TaskDraft(title = "Bad time", scheduleKind = ScheduleKind.Once, date = monday, timeMinutes = 1_440),
            TaskDraft(title = "Bad duration", durationMinutes = 0),
            TaskDraft(title = "Bad reminder", reminderOffsetsMinutes = listOf(-1)),
            TaskDraft(title = "Missing date", scheduleKind = ScheduleKind.Once),
            TaskDraft(title = "Missing recurrence", scheduleKind = ScheduleKind.Recurring),
            TaskDraft(title = "Ambiguous tag", tags = setOf("one,two")),
        )

        invalidDrafts.forEach { draft -> assertTrue(runCatching { repository.create(draft) }.isFailure) }

        assertTrue(repository.tasks.first().isEmpty())
        assertTrue(repository.steps.first().isEmpty())
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
    fun notificationStyleCompletionSnapshotsOnlyCarryUnfinishedStepsVisibleThisOccurrence() = runBlocking {
        val taskId = repository.create(
            recurringDraft(autoComplete = false).copy(repeatStepPolicy = RepeatStepPolicy.CarryUnfinished),
        )
        val task = requireNotNull(repository.getTask(taskId))
        val first = item(task, monday)
        repository.setStepCompleted(first, task.steps.first().id, true)
        repository.complete(first)
        val secondDate = monday.plusDays(1)
        val secondStep = task.steps[1]
        val second = item(task, secondDate).copy(
            subtasks = listOf(ScheduledSubtask(secondStep, false, null, secondStep.title)),
        )
        repository.setStepCompleted(second, secondStep.id, true)

        repository.completeOccurrence(taskId, secondDate)

        val secondSnapshots = repository.stepSnapshots.first()
            .filter { it.occurrenceKey == secondDate.toEpochDay() }
        assertEquals(listOf("Second"), secondSnapshots.map { it.title })
        assertEquals(listOf(true), secondSnapshots.map { it.completed })
    }

    @Test
    fun carryUnfinishedUsesCumulativePerStepHistoryAcrossThreeOccurrences() = runBlocking {
        val taskId = repository.create(
            recurringDraft(autoComplete = false).copy(repeatStepPolicy = RepeatStepPolicy.CarryUnfinished),
        )
        val task = requireNotNull(repository.getTask(taskId))
        val first = item(task, monday)
        repository.setStepCompleted(first, task.steps.first().id, true)
        repository.complete(first)
        val secondDate = monday.plusDays(1)
        val secondStep = task.steps[1]
        val second = item(task, secondDate).copy(
            subtasks = listOf(ScheduledSubtask(secondStep, false, null, secondStep.title)),
        )
        repository.setStepCompleted(second, secondStep.id, true)
        repository.complete(second.copy(subtasks = listOf(second.subtasks.single().copy(completed = true))))

        val thirdVisible = visibleTaskStepsForOccurrence(
            task.steps,
            repository.stepSnapshots.first().filter { it.taskId == taskId },
            monday.plusDays(2).toEpochDay(),
            RepeatStepPolicy.CarryUnfinished,
        )

        assertTrue(thirdVisible.isEmpty())
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

        repository.reopenOccurrence(
            scheduled.copy(
                completedAtMillis = FixedClock.now().toEpochMilli(),
                occurrenceState = OccurrenceState.Completed,
            ),
        )

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
    fun editThisAndFutureRejectsOccurrenceCompletedWhileEditorWasOpen() = runBlocking {
        val oldTaskId = repository.create(recurringDraft(autoComplete = false))
        val oldTask = requireNotNull(repository.getTask(oldTaskId))
        val boundary = monday.plusDays(2)
        val editorSnapshot = item(oldTask, boundary)

        repository.complete(editorSnapshot)
        val result = runCatching {
            repository.updateIfCurrent(
                editorSnapshot.toEditBoundary(),
                oldTask.toDraft().copy(title = "Should not split"),
                boundary,
            )
        }

        assertTrue(result.isFailure)
        assertEquals(listOf(oldTaskId), repository.tasks.first().map { it.id })
        assertEquals(
            OccurrenceState.Completed,
            repository.getOccurrences(oldTaskId).single().state,
        )
        assertEquals("Recurring", requireNotNull(repository.getTask(oldTaskId)).title)
    }

    @Test
    fun editingCompletedRecurringHistoryUpdatesSeriesWithoutCreatingDuplicateBoundary() = runBlocking {
        val taskId = repository.create(recurringDraft(autoComplete = false))
        val task = requireNotNull(repository.getTask(taskId))
        val completedSnapshot = item(task, monday)
        repository.complete(completedSnapshot)

        val savedId = repository.updateIfCurrent(
            completedSnapshot.copy(
                completedAtMillis = FixedClock.now().toEpochMilli(),
                occurrenceState = OccurrenceState.Completed,
            ).toEditBoundary(),
            task.toDraft().copy(title = "Edited series"),
            fromOccurrence = null,
        )

        assertEquals(taskId, savedId)
        assertEquals(listOf(taskId), repository.tasks.first().map { it.id })
        assertEquals("Edited series", requireNotNull(repository.getTask(taskId)).title)
        assertEquals(OccurrenceState.Completed, repository.getOccurrences(taskId).single().state)
    }

    @Test
    fun editThisAndFutureCanEndRecurrenceWithoutRewritingRecordedHistory() = runBlocking {
        val oldTaskId = repository.create(recurringDraft(autoComplete = false))
        val oldTask = requireNotNull(repository.getTask(oldTaskId))
        repository.complete(item(oldTask, monday))
        val boundary = monday.plusDays(2)
        val openBoundary = item(oldTask, boundary)

        val futureTaskId = repository.updateIfCurrent(
            openBoundary.toEditBoundary(),
            oldTask.toDraft().copy(
                title = "One final occurrence",
                scheduleKind = ScheduleKind.Once,
                date = boundary,
                recurrence = null,
            ),
            boundary,
        )

        assertTrue(futureTaskId != oldTaskId)
        val historical = requireNotNull(repository.getTask(oldTaskId))
        val future = requireNotNull(repository.getTask(futureTaskId))
        assertEquals(monday.plusDays(1), historical.recurrence?.endDate)
        assertEquals(OccurrenceState.Completed, repository.getOccurrences(oldTaskId).single().state)
        assertEquals(ScheduleKind.Once, future.scheduleKind)
        assertEquals(boundary, future.date)
        assertNull(future.recurrence)
    }

    @Test
    fun editThisAndFutureCanMoveFutureWorkToInboxWithoutMutatingTheOldSeries() = runBlocking {
        val oldTaskId = repository.create(recurringDraft(autoComplete = false))
        val oldTask = requireNotNull(repository.getTask(oldTaskId))
        val boundary = monday.plusDays(2)

        val futureTaskId = repository.updateIfCurrent(
            item(oldTask, boundary).toEditBoundary(),
            oldTask.toDraft().copy(
                title = "Future inbox task",
                scheduleKind = ScheduleKind.Anytime,
                date = null,
                recurrence = null,
                inbox = true,
            ),
            boundary,
        )

        val historical = requireNotNull(repository.getTask(oldTaskId))
        val future = requireNotNull(repository.getTask(futureTaskId))
        assertEquals(monday.plusDays(1), historical.recurrence?.endDate)
        assertEquals(ScheduleKind.Recurring, historical.scheduleKind)
        assertEquals(ScheduleKind.Anytime, future.scheduleKind)
        assertNull(future.date)
        assertTrue(future.inbox)
    }

    @Test
    fun futureSeriesRetainsSubtaskTriggerIdentityAndItsConditions() = runBlocking {
        val oldTaskId = repository.create(recurringDraft(autoComplete = false))
        val oldTask = requireNotNull(repository.getTask(oldTaskId))
        val sourceStep = oldTask.steps.first()
        val linkDao = database.linkDao()
        val triggerId = linkDao.insertTriggerRule(
            TriggerRuleEntity(
                uuid = "subtask-trigger",
                name = "Continue after first step",
                sourceType = "Subtask",
                sourceEntityId = oldTaskId,
                sourceItemId = sourceStep.id,
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
        linkDao.insertTriggerCondition(
            TriggerRuleConditionEntity(
                triggerRuleId = triggerId,
                fieldId = null,
                entryDate = true,
                operator = "OnOrAfter",
                position = 0,
                textValue = null,
                numberValue = null,
                secondNumberValue = null,
                dateEpochDay = monday.toEpochDay(),
                secondDateEpochDay = null,
            ),
        )
        val measurements = RoomMeasurementRepository(database, FixedClock, UuidWhipIdGenerator)
        val goals = RoomGoalRepository(database, measurements, FixedClock, UuidWhipIdGenerator)
        val links = RoomLinkRepository(database, measurements, FixedClock, UuidWhipIdGenerator)
        val boundary = monday.plusDays(2)
        val goalId = goals.create(
            GoalDraft(
                name = "Retained Subtask goal",
                type = GoalType.AccumulateTotal,
                dimension = UnitDimension.Count,
                unitId = "count",
                targetMin = 1.0,
                startDate = monday,
                aggregation = GoalAggregation.Sum,
            ),
        )
        val sourceLinkId = links.createRule(
            LinkRuleDraft(
                name = "First step contribution",
                sourceType = LinkSourceType.Subtask,
                sourceEntityId = oldTaskId,
                sourceItemId = sourceStep.id,
                sourceMetric = LinkSourceMetric.Completion,
                targetGoalId = goalId,
                retroactiveFrom = boundary.plusDays(4),
            ),
            commitBackfill = true,
        )
        linkDao.insertRuleCondition(
            LinkRuleConditionEntity(
                linkRuleId = sourceLinkId,
                fieldId = null,
                entryDate = true,
                operator = "OnOrAfter",
                position = 0,
                textValue = null,
                numberValue = null,
                secondNumberValue = null,
                dateEpochDay = monday.toEpochDay(),
                secondDateEpochDay = null,
            ),
        )
        val futureDraft = oldTask.toDraft().copy(
            recurrence = requireNotNull(oldTask.recurrence).copy(startDate = boundary),
            steps = listOf(
                TaskStepDraft(title = "Inserted first", position = 0),
                TaskStepDraft(
                    id = oldTask.steps[1].id,
                    title = oldTask.steps[1].title,
                    position = 1,
                ),
                TaskStepDraft(
                    id = oldTask.steps[0].id,
                    title = oldTask.steps[0].title,
                    position = 2,
                ),
            ),
        )

        val futureTaskId = repository.update(oldTaskId, futureDraft, boundary)

        val futureTask = requireNotNull(repository.getTask(futureTaskId))
        val copied = linkDao.getTriggerRules().single { it.sourceEntityId == futureTaskId }
        assertEquals(futureTask.steps.single { it.title == sourceStep.title }.id, copied.sourceItemId)
        assertTrue(copied.sourceItemId != sourceStep.id)
        assertEquals(
            linkDao.getTriggerConditions(triggerId).map { it.copy(id = 0, triggerRuleId = 0) },
            linkDao.getTriggerConditions(copied.id).map { it.copy(id = 0, triggerRuleId = 0) },
        )
        val copiedLink = linkDao.getRules().single { it.sourceEntityId == futureTaskId }
        assertEquals(futureTask.steps.single { it.title == sourceStep.title }.id, copiedLink.sourceItemId)
        assertEquals(boundary.plusDays(4).toEpochDay(), copiedLink.retroactiveFromEpochDay)
        assertEquals(
            linkDao.getRuleConditions(sourceLinkId).map { it.copy(id = 0, linkRuleId = 0) },
            linkDao.getRuleConditions(copiedLink.id).map { it.copy(id = 0, linkRuleId = 0) },
        )
    }

    @Test
    fun futureSplitMigratesRescheduledBoundaryAndItsSubtaskStateExactlyOnce() = runBlocking {
        val oldTaskId = repository.create(recurringDraft(autoComplete = false))
        val oldTask = requireNotNull(repository.getTask(oldTaskId))
        val boundary = monday.plusDays(2)
        val movedDate = boundary.plusDays(4)
        val original = item(oldTask, boundary)
        repository.reschedule(original, movedDate)
        val moved = original.copy(
            scheduledDate = movedDate,
            occurrenceState = OccurrenceState.Open,
        )
        repository.setStepCompleted(moved, oldTask.steps.first().id, true)
        val editorSnapshot = moved.copy(
            subtasks = moved.subtasks.mapIndexed { index, subtask ->
                subtask.copy(
                    completed = index == 0,
                    completedAtMillis = FixedClock.now().toEpochMilli().takeIf { index == 0 },
                )
            },
        )

        val futureTaskId = repository.updateIfCurrent(
            editorSnapshot.toEditBoundary(),
            oldTask.toDraft().copy(title = "Future definition"),
            boundary,
        )

        assertTrue(repository.getOccurrences(oldTaskId).none { it.originalDate >= boundary })
        val migrated = repository.getOccurrences(futureTaskId).single()
        assertEquals(boundary, migrated.originalDate)
        assertEquals(movedDate, migrated.scheduledDate)
        assertEquals(OccurrenceState.Open, migrated.state)
        val futureTask = requireNotNull(repository.getTask(futureTaskId))
        val futureFirst = futureTask.steps.single { it.title == oldTask.steps.first().title }
        val migratedStates = repository.stepStates.first().filter { it.taskId == futureTaskId }
        assertTrue(migratedStates.single { it.stepId == futureFirst.id }.completed)
        assertTrue(repository.stepStates.first().none { it.taskId == oldTaskId && it.occurrenceKey >= boundary.toEpochDay() })
    }

    @Test
    fun futureSplitKeepsLaterStateOnlyOccurrenceReachableWhenCadenceChanges() = runBlocking {
        val oldTaskId = repository.create(recurringDraft(autoComplete = false))
        val oldTask = requireNotNull(repository.getTask(oldTaskId))
        val boundary = monday.plusDays(1)
        val laterStateDate = monday.plusDays(2)
        repository.setStepCompleted(
            item(oldTask, laterStateDate),
            oldTask.steps.first().id,
            true,
        )

        val futureTaskId = repository.updateIfCurrent(
            item(oldTask, boundary).toEditBoundary(),
            oldTask.toDraft().copy(
                recurrence = requireNotNull(oldTask.recurrence).copy(interval = 3),
            ),
            boundary,
        )

        val migratedOccurrence = repository.getOccurrences(futureTaskId).single {
            it.originalDate == laterStateDate
        }
        assertEquals(OccurrenceState.Open, migratedOccurrence.state)
        assertEquals(laterStateDate, migratedOccurrence.scheduledDate)
        val futureTask = requireNotNull(repository.getTask(futureTaskId))
        val futureStep = futureTask.steps.single { it.title == oldTask.steps.first().title }
        val migratedState = repository.stepStates.first().single {
            it.taskId == futureTaskId &&
                it.stepId == futureStep.id &&
                it.occurrenceKey == laterStateDate.toEpochDay()
        }
        assertTrue(migratedState.completed)
        assertTrue(
            repository.stepStates.first().none {
                it.taskId == oldTaskId && it.occurrenceKey == laterStateDate.toEpochDay()
            },
        )
    }

    @Test
    fun futureSplitPreservesCarryUnfinishedBaselineForRetainedSubtasks() = runBlocking {
        val oldTaskId = repository.create(
            recurringDraft(autoComplete = false).copy(repeatStepPolicy = RepeatStepPolicy.CarryUnfinished),
        )
        val oldTask = requireNotNull(repository.getTask(oldTaskId))
        val firstOccurrence = item(oldTask, monday)
        repository.setStepCompleted(firstOccurrence, oldTask.steps.first().id, true)
        repository.complete(firstOccurrence)
        val boundary = monday.plusDays(1)
        val visibleBoundaryStep = oldTask.steps[1]
        val editorSnapshot = item(oldTask, boundary).copy(
            subtasks = listOf(
                ScheduledSubtask(
                    step = visibleBoundaryStep,
                    completed = false,
                    completedAtMillis = null,
                    title = visibleBoundaryStep.title,
                ),
            ),
        )

        val futureTaskId = repository.updateIfCurrent(
            editorSnapshot.toEditBoundary(),
            oldTask.toDraft().copy(title = "Carry forward"),
            boundary,
        )

        val futureTask = requireNotNull(repository.getTask(futureTaskId))
        val visibleFuture = visibleTaskStepsForOccurrence(
            futureTask.steps,
            repository.stepSnapshots.first().filter { it.taskId == futureTaskId },
            boundary.toEpochDay(),
            futureTask.repeatStepPolicy,
        )
        assertEquals(listOf("Second"), visibleFuture.map { it.title })
    }

    @Test
    fun unchangedFiniteOccurrenceCountPreservesOriginalSeriesTotalAcrossSplit() = runBlocking {
        val draft = recurringDraft(autoComplete = false).copy(
            recurrence = requireNotNull(recurringDraft(false).recurrence).copy(
                end = RecurrenceEnd.AfterCount,
                occurrenceCount = 5,
            ),
        )
        val oldTaskId = repository.create(draft)
        val oldTask = requireNotNull(repository.getTask(oldTaskId))
        val boundary = monday.plusDays(2)

        val futureTaskId = repository.updateIfCurrent(
            item(oldTask, boundary).toEditBoundary(),
            oldTask.toDraft().copy(title = "Renamed future"),
            boundary,
        )

        assertEquals(3, requireNotNull(repository.getTask(futureTaskId)).recurrence?.occurrenceCount)
        val historicalSlots = RecurrenceEngine.occurrencesBetween(
            requireNotNull(repository.getTask(oldTaskId)).recurrence!!,
            monday,
            boundary.minusDays(1),
        ).size
        assertEquals(5, historicalSlots + requireNotNull(repository.getTask(futureTaskId)).recurrence!!.occurrenceCount!!)
    }

    @Test
    fun futureSeriesCopiesTrackMappingsAndRetargetsInboundAutomationWithoutLosingHistory() = runBlocking {
        val oldTaskId = repository.create(recurringDraft(autoComplete = false))
        val oldTask = requireNotNull(repository.getTask(oldTaskId))
        val trackRepository = RoomTrackRepository(database, FixedClock, UuidWhipIdGenerator)
        val trackId = trackRepository.create(
            TrackDraft(
                name = "Automation target",
                fields = listOf(
                    TrackFieldDraft(
                        name = "Task",
                        type = TrackFieldType.ShortText,
                        required = true,
                        primary = true,
                    ),
                ),
            ),
        )
        val targetFieldId = requireNotNull(trackRepository.projection(trackId)).fields.single().id
        val linkDao = database.linkDao()
        val outboundId = linkDao.insertTriggerRule(
            TriggerRuleEntity(
                uuid = "task-track-trigger",
                name = "Record completed task",
                sourceType = "Task",
                sourceEntityId = oldTaskId,
                outcome = "Completed",
                targetType = "Track",
                targetEntityId = trackId,
                delayMinutes = 0,
                quietStartMinutes = null,
                quietEndMinutes = null,
                action = "PromptTrackEntry",
                notificationEnabled = false,
                conditionMode = "MatchAll",
                enabled = true,
                createdAtMillis = 1,
                updatedAtMillis = 1,
            ),
        )
        linkDao.insertTriggerMapping(
            TriggerFieldMappingEntity(
                triggerRuleId = outboundId,
                targetFieldId = targetFieldId,
                sourceProperty = "Title",
                constantText = null,
                constantNumber = null,
                constantUnitId = null,
                constantDateEpochDay = null,
                constantBoolean = null,
                constantChoiceOptionId = null,
                constantScale = null,
            ),
        )
        val inboundId = linkDao.insertTriggerRule(
            TriggerRuleEntity(
                uuid = "track-task-trigger",
                name = "Prompt recurring task",
                sourceType = "Track",
                sourceEntityId = trackId,
                outcome = "Recorded",
                targetType = "Task",
                targetEntityId = oldTaskId,
                delayMinutes = 0,
                quietStartMinutes = null,
                quietEndMinutes = null,
                action = "PromptTask",
                notificationEnabled = true,
                conditionMode = "MatchAll",
                enabled = true,
                createdAtMillis = 1,
                updatedAtMillis = 1,
            ),
        )
        linkDao.insertTriggerCondition(
            TriggerRuleConditionEntity(
                triggerRuleId = inboundId,
                fieldId = targetFieldId,
                entryDate = false,
                operator = "Contains",
                position = 0,
                textValue = "important",
                numberValue = null,
                secondNumberValue = null,
                dateEpochDay = null,
                secondDateEpochDay = null,
            ),
        )
        val occurrenceId = linkDao.upsertTriggerOccurrence(
            TriggerOccurrenceEntity(
                triggerRuleId = inboundId,
                sourceEventId = "track:$trackId:entry:1",
                availableAtMillis = 10,
                deliveredAtMillis = 11,
                dismissedAtMillis = null,
                remindAtMillis = null,
                fulfilledEntryId = null,
                sourceSnapshot = "{}",
            ),
        )
        val boundary = monday.plusDays(2)

        val futureTaskId = repository.update(
            oldTaskId,
            oldTask.toDraft().copy(
                recurrence = requireNotNull(oldTask.recurrence).copy(startDate = boundary),
            ),
            boundary,
        )

        val triggers = linkDao.getTriggerRules()
        val copiedOutbound = triggers.single {
            it.sourceEntityId == futureTaskId && it.targetEntityId == trackId
        }
        assertEquals(
            linkDao.getTriggerMappings(outboundId).map { it.copy(id = 0, triggerRuleId = 0) },
            linkDao.getTriggerMappings(copiedOutbound.id).map { it.copy(id = 0, triggerRuleId = 0) },
        )
        val retargetedInbound = requireNotNull(linkDao.getTriggerRule(inboundId))
        assertEquals(futureTaskId, retargetedInbound.targetEntityId)
        assertEquals(1, triggers.count { it.uuid == "track-task-trigger" })
        assertEquals(1, linkDao.getTriggerConditions(inboundId).size)
        assertEquals(occurrenceId, linkDao.getTriggerOccurrences(inboundId).single().id)
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
    fun staleSkipCannotReplaceCompletedOccurrenceHistory() = runBlocking {
        val taskId = repository.create(recurringDraft(autoComplete = false))
        val task = requireNotNull(repository.getTask(taskId))
        val stale = item(task, monday)
        repository.complete(stale)
        val committed = repository.getOccurrences(taskId).single()

        val result = runCatching { repository.skip(stale) }

        assertTrue(result.isFailure)
        assertEquals(committed, repository.getOccurrences(taskId).single())
        assertEquals(OccurrenceState.Completed, committed.state)
    }

    @Test
    fun staleSubtaskToggleCannotRewriteAClosedOccurrence() = runBlocking {
        val taskId = repository.create(recurringDraft(autoComplete = false))
        val task = requireNotNull(repository.getTask(taskId))
        val stale = item(task, monday)
        repository.complete(stale)

        val result = runCatching {
            repository.setStepCompleted(stale, task.steps.first().id, true)
        }

        assertTrue(result.isFailure)
        assertTrue(repository.stepStates.first().none { it.taskId == taskId && it.completed })
        assertEquals(OccurrenceState.Completed, repository.getOccurrences(taskId).single().state)
    }

    @Test
    fun repeatedHistoricalReopenCannotOverwriteAnAlreadyOpenOccurrence() = runBlocking {
        val taskId = repository.create(recurringDraft(autoComplete = false))
        val task = requireNotNull(repository.getTask(taskId))
        repository.complete(item(task, monday))
        val completed = repository.getOccurrences(taskId).single()
        val historical = item(task, monday).copy(
            scheduledDate = completed.scheduledDate,
            completedAtMillis = completed.completedAtMillis,
            occurrenceState = completed.state,
        )
        repository.reopenOccurrence(historical)

        val repeated = runCatching { repository.reopenOccurrence(historical) }

        assertTrue(repeated.isFailure)
        assertEquals(OccurrenceState.Open, repository.getOccurrences(taskId).single().state)
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
    fun staleRecurringRescheduleCannotOverwriteCompletedHistory() = runBlocking {
        val taskId = repository.create(recurringDraft(autoComplete = false))
        val task = requireNotNull(repository.getTask(taskId))
        val stale = item(task, monday)
        repository.complete(stale)
        val committed = repository.getOccurrences(taskId).single()

        val result = runCatching { repository.reschedule(stale, monday.plusDays(4)) }

        assertTrue(result.isFailure)
        assertEquals(committed, repository.getOccurrences(taskId).single())
        assertEquals(OccurrenceState.Completed, committed.state)
    }

    @Test
    fun staleOneShotProjectionCannotReplaceANewerRecurringDefinition() = runBlocking {
        val taskId = repository.create(
            TaskDraft(title = "Changing schedule", scheduleKind = ScheduleKind.Once, date = monday),
        )
        val staleTask = requireNotNull(repository.getTask(taskId))
        val stale = ScheduledTask(staleTask, originalDate = null, scheduledDate = monday)
        repository.update(taskId, recurringDraft(autoComplete = false).copy(title = "Changing schedule"))

        val result = runCatching { repository.reschedule(stale, monday.plusDays(2)) }

        assertTrue(result.isFailure)
        val current = requireNotNull(repository.getTask(taskId))
        assertEquals(ScheduleKind.Recurring, current.scheduleKind)
        assertEquals(monday, current.recurrence?.startDate)
        assertTrue(repository.getOccurrences(taskId).isEmpty())
    }

    @Test
    fun moveUndoRejectsANewerScheduleInsteadOfOverwritingIt() = runBlocking {
        val taskId = repository.create(
            TaskDraft(title = "Move safely", scheduleKind = ScheduleKind.Once, date = monday),
        )
        val originalTask = requireNotNull(repository.getTask(taskId))
        val original = ScheduledTask(originalTask, originalDate = null, scheduledDate = monday)
        val firstMove = monday.plusDays(2)
        val newerMove = monday.plusDays(5)
        repository.reschedule(original, firstMove)
        val movedTask = requireNotNull(repository.getTask(taskId))
        repository.reschedule(
            ScheduledTask(movedTask, originalDate = null, scheduledDate = firstMove),
            newerMove,
        )

        val undo = runCatching { repository.restoreSchedulesIfCurrent(listOf(original), firstMove) }

        assertTrue(undo.isFailure)
        assertEquals(newerMove, requireNotNull(repository.getTask(taskId)).date)
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
        repository.restoreSchedulesIfCurrent(originals, monday.plusDays(4))

        assertEquals(ScheduleKind.Anytime, requireNotNull(repository.getTask(inboxId)).scheduleKind)
        assertTrue(requireNotNull(repository.getTask(inboxId)).inbox)
        assertNull(requireNotNull(repository.getTask(inboxId)).date)
        assertEquals(monday, requireNotNull(repository.getTask(onceId)).date)
        assertTrue(repository.getOccurrences(recurringId).isEmpty())
    }

    @Test
    fun moveUndoRejectsACompletionThatHappenedAfterTheMove() = runBlocking {
        val taskId = repository.create(
            TaskDraft(title = "Move then finish", scheduleKind = ScheduleKind.Once, date = monday),
        )
        val originalTask = requireNotNull(repository.getTask(taskId))
        val original = ScheduledTask(originalTask, originalDate = null, scheduledDate = monday)
        val movedDate = monday.plusDays(2)
        repository.reschedule(original, movedDate)
        val moved = requireNotNull(repository.getTask(taskId))
        repository.complete(ScheduledTask(moved, originalDate = null, scheduledDate = movedDate))

        val undo = runCatching { repository.restoreSchedulesIfCurrent(listOf(original), movedDate) }

        assertTrue(undo.isFailure)
        val completed = requireNotNull(repository.getTask(taskId))
        assertEquals(movedDate, completed.date)
        assertNotNull(completed.completedAtMillis)
    }

    @Test
    fun sameMillisecondStaleOneShotMoveCannotOverwriteTheAuthoritativeDate() = runBlocking {
        val taskId = repository.create(
            TaskDraft(title = "Fixed clock move", scheduleKind = ScheduleKind.Once, date = monday),
        )
        val originalTask = requireNotNull(repository.getTask(taskId))
        val original = ScheduledTask(originalTask, originalDate = null, scheduledDate = monday)
        val newerDate = monday.plusDays(5)
        repository.reschedule(original, newerDate)

        val staleMove = runCatching { repository.reschedule(original, monday.plusDays(2)) }

        assertTrue(staleMove.isFailure)
        assertEquals(newerDate, requireNotNull(repository.getTask(taskId)).date)
    }

    @Test
    fun bulkReopenValidatesEverySnapshotBeforeChangingAnyTask() = runBlocking {
        val firstId = repository.create(
            TaskDraft(title = "First closed", scheduleKind = ScheduleKind.Once, date = monday),
        )
        val secondId = repository.create(
            TaskDraft(title = "Second closed", scheduleKind = ScheduleKind.Once, date = monday),
        )
        repository.completeOccurrence(firstId, monday)
        repository.completeOccurrence(secondId, monday)
        val firstClosed = requireNotNull(repository.getTask(firstId)).let {
            ScheduledTask(it, originalDate = monday, scheduledDate = monday, completedAtMillis = it.completedAtMillis)
        }
        val secondClosed = requireNotNull(repository.getTask(secondId)).let {
            ScheduledTask(it, originalDate = monday, scheduledDate = monday, completedAtMillis = it.completedAtMillis)
        }
        repository.reopenIfCurrent(secondClosed)

        val staleBulkReopen = runCatching {
            repository.reopenAllIfCurrent(listOf(firstClosed, secondClosed))
        }

        assertTrue(staleBulkReopen.isFailure)
        assertNotNull(requireNotNull(repository.getTask(firstId)).completedAtMillis)
        assertNull(requireNotNull(repository.getTask(secondId)).completedAtMillis)
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

        repository.restoreSchedulesIfCurrent(originals, monday)

        assertEquals(ScheduleKind.Anytime, requireNotNull(repository.getTask(inboxId)).scheduleKind)
        assertEquals(true, requireNotNull(repository.getTask(inboxId)).inbox)
        assertEquals(ScheduleKind.Anytime, requireNotNull(repository.getTask(legacyUndatedId)).scheduleKind)
        assertTrue(requireNotNull(repository.getTask(legacyUndatedId)).inbox)
    }

    @Test
    fun planMyDayUndoCannotEraseACompletionThatHappenedAfterPlanning() = runBlocking {
        val id = repository.create(TaskDraft(title = "Plan then complete"))
        val original = requireNotNull(repository.getTask(id)).let {
            ScheduledTask(it, originalDate = null, scheduledDate = null)
        }
        repository.planAll(listOf(original), monday)
        val planned = requireNotNull(repository.getTask(id)).let {
            ScheduledTask(it, originalDate = null, scheduledDate = monday)
        }
        repository.complete(planned)

        val staleUndo = runCatching {
            repository.restoreSchedulesIfCurrent(listOf(original), monday)
        }

        assertTrue(staleUndo.isFailure)
        val current = requireNotNull(repository.getTask(id))
        assertEquals(monday, current.date)
        assertNotNull(current.completedAtMillis)
        assertFalse(current.inbox)
    }

    @Test
    fun bulkMoveUndoRestoresAnAlreadyMovedRecurringOccurrence() = runBlocking {
        val recurringId = repository.create(recurringDraft(autoComplete = false))
        val recurring = requireNotNull(repository.getTask(recurringId))
        repository.reschedule(item(recurring, monday), monday.plusDays(2))
        val previouslyMoved = item(recurring, monday).copy(scheduledDate = monday.plusDays(2))

        repository.rescheduleAll(listOf(previouslyMoved), monday.plusDays(6))
        repository.restoreSchedulesIfCurrent(listOf(previouslyMoved), monday.plusDays(6))

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
    fun bulkMetadataRejectsAnEnabledButUnselectedArea() = runBlocking {
        val id = repository.create(TaskDraft(title = "Keep Area", area = "Original"))

        val result = runCatching {
            repository.updateMetadataAll(
                listOf(id),
                TaskBulkEdit(updateArea = true, areaId = null, areaName = ""),
            )
        }

        assertTrue(result.isFailure)
        assertEquals("Original", requireNotNull(repository.getTask(id)).area)
    }

    @Test
    fun sameMillisecondBulkMetadataCannotOverwriteNewerTaskContent() = runBlocking {
        val id = repository.create(TaskDraft(title = "Semantic revision", tags = setOf("original")))
        val reviewed = requireNotNull(repository.getTask(id))
        val reviewedItem = ScheduledTask(reviewed, originalDate = null, scheduledDate = null)
        repository.updateMetadataAll(
            listOf(id),
            TaskBulkEdit(tags = setOf("newer")),
        )

        val stale = runCatching {
            repository.updateMetadataAllIfCurrent(
                listOf(reviewedItem),
                TaskBulkEdit(tags = setOf("stale")),
            )
        }

        assertTrue(stale.isFailure)
        assertEquals(setOf("newer"), requireNotNull(repository.getTask(id)).tags)
    }

    @Test
    fun sameMillisecondBulkArchiveCannotHideAChangedTask() = runBlocking {
        val id = repository.create(TaskDraft(title = "Archive revision", tags = setOf("original")))
        val reviewed = requireNotNull(repository.getTask(id))
        val reviewedItem = ScheduledTask(reviewed, originalDate = null, scheduledDate = null)
        repository.updateMetadataAll(
            listOf(id),
            TaskBulkEdit(tags = setOf("newer")),
        )

        val stale = runCatching { repository.archiveAllIfCurrent(listOf(reviewedItem)) }

        assertTrue(stale.isFailure)
        assertFalse(requireNotNull(repository.getTask(id)).archived)
        assertEquals(setOf("newer"), requireNotNull(repository.getTask(id)).tags)
    }

    @Test
    fun sameMillisecondStepDefinitionChangeInvalidatesAStaleCompletion() = runBlocking {
        val id = repository.create(recurringDraft(autoComplete = false))
        val reviewed = requireNotNull(repository.getTask(id))
        val reviewedItem = item(reviewed, monday)
        repository.update(
            id,
            recurringDraft(autoComplete = false).copy(
                steps = listOf(TaskStepDraft(reviewed.steps.first().id, "Renamed", 0)),
            ),
        )

        val stale = runCatching { repository.complete(reviewedItem) }

        assertTrue(stale.isFailure)
        assertTrue(repository.getOccurrences(id).isEmpty())
    }

    @Test
    fun currentWeeklyRuleRejectsAnInvalidVirtualTuesdayAcrossMutations() = runBlocking {
        val id = repository.create(recurringDraft(autoComplete = false))
        repository.update(
            id,
            recurringDraft(autoComplete = false).copy(
                recurrence = RecurrenceRule(
                    unit = RecurrenceUnit.Weeks,
                    weekdays = setOf(DayOfWeek.MONDAY),
                    startDate = monday,
                ),
            ),
        )
        val current = requireNotNull(repository.getTask(id))
        val invalidTuesday = item(current, monday.plusDays(1))

        assertTrue(runCatching { repository.completeOccurrence(id, monday.plusDays(1)) }.isFailure)
        assertTrue(runCatching { repository.skip(invalidTuesday) }.isFailure)
        assertTrue(runCatching { repository.reschedule(invalidTuesday, monday.plusDays(3)) }.isFailure)
        assertTrue(repository.getOccurrences(id).isEmpty())
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
