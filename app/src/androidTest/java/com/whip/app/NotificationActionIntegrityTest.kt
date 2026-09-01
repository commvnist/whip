package com.whip.app

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.whip.app.core.adjustForQuietHours
import com.whip.app.core.zoneId
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.HabitLogStatus
import com.whip.app.domain.HabitScheduleType
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.MetricSourceType
import com.whip.app.domain.MetricValueKind
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalType
import com.whip.app.domain.RecurrenceRule
import com.whip.app.domain.RecurrenceUnit
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.ScheduledSubtask
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskStepDraft
import com.whip.app.domain.UnitDimension
import com.whip.app.reminders.ALL_WHIP_WORK_TAG
import com.whip.app.reminders.HabitNotificationAction
import com.whip.app.reminders.HabitReminderActionReceiver
import com.whip.app.reminders.GoalReminderActionReceiver
import com.whip.app.reminders.ReminderDeliveryClaim
import com.whip.app.reminders.ReminderDeliveryKind
import com.whip.app.reminders.ReminderActionReceiver
import com.whip.app.reminders.TaskNotificationAction
import com.whip.app.reminders.currentHabitReminderDeliveryClaim
import com.whip.app.reminders.currentTaskNotificationTarget
import com.whip.app.reminders.goalReminderSemanticFingerprint
import com.whip.app.reminders.isCurrentHabitNotificationAction
import com.whip.app.startup.USER_DATA_GENERATION_KEY
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
class NotificationActionIntegrityTest {
    private lateinit var app: WhipApplication
    private lateinit var today: LocalDate

    @Before
    fun setUp() = runBlocking {
        app = ApplicationProvider.getApplicationContext()
        WorkManager.getInstance(app).cancelAllWorkByTag(ALL_WHIP_WORK_TAG).result.get()
        app.backupRepository.deleteAllData()
        today = app.clock.today()
    }

    @After
    fun tearDown() = runBlocking {
        WorkManager.getInstance(app).cancelAllWorkByTag(ALL_WHIP_WORK_TAG).result.get()
        app.backupRepository.deleteAllData()
    }

    @Test
    fun taskActionValidationRejectsMissingArchivedWrongClosedAndNewlyBlockedTargets() = runBlocking {
        assertNull(app.currentTaskNotificationTarget(Long.MAX_VALUE, today, TaskNotificationAction.Complete))

        val archivedId = app.taskRepository.create(reminderTask("Archived"))
        app.taskRepository.archive(archivedId)
        assertNull(app.currentTaskNotificationTarget(archivedId, today, TaskNotificationAction.Complete))

        val remindersDisabledId = app.taskRepository.create(
            reminderTask("Reminder disabled").copy(reminderEnabled = false),
        )
        assertNull(
            app.currentTaskNotificationTarget(
                remindersDisabledId,
                today,
                TaskNotificationAction.Snooze,
            ),
        )

        val recurringId = app.taskRepository.create(
            reminderTask("Recurring").copy(
                scheduleKind = ScheduleKind.Recurring,
                recurrence = RecurrenceRule(RecurrenceUnit.Days, startDate = today),
            ),
        )
        assertNull(
            app.currentTaskNotificationTarget(
                recurringId,
                today.minusDays(1),
                TaskNotificationAction.Complete,
            ),
        )

        val editedId = app.taskRepository.create(reminderTask("Edited after posting"))
        app.taskRepository.update(
            editedId,
            reminderTask("Edited after posting").copy(
                steps = listOf(TaskStepDraft(title = "New unfinished work", position = 0)),
            ),
        )
        assertNull(app.currentTaskNotificationTarget(editedId, today, TaskNotificationAction.Complete))
        assertNotNull(app.currentTaskNotificationTarget(editedId, today, TaskNotificationAction.Snooze))

        val completedId = app.taskRepository.create(reminderTask("Already complete"))
        app.taskRepository.completeOccurrence(completedId, today)
        assertNull(app.currentTaskNotificationTarget(completedId, today, TaskNotificationAction.Complete))
        assertNotNull(app.currentTaskNotificationTarget(completedId, today, TaskNotificationAction.Undo))

        val skippedTask = requireNotNull(app.taskRepository.getTask(recurringId))
        app.taskRepository.skip(ScheduledTask(skippedTask, today, today))
        assertNull(app.currentTaskNotificationTarget(recurringId, today, TaskNotificationAction.Complete))
        assertNull(app.currentTaskNotificationTarget(recurringId, today, TaskNotificationAction.Undo))
    }

    @Test
    fun completedSubtasksDoNotMakeAnOtherwiseCurrentTaskActionStale() = runBlocking {
        val draft = reminderTask("Prepared").copy(
            steps = listOf(TaskStepDraft(title = "Prepared step", position = 0)),
            autoCompleteFromSteps = false,
        )
        val id = app.taskRepository.create(draft)
        val task = requireNotNull(app.taskRepository.getTask(id))
        val item = ScheduledTask(
            task,
            today,
            today,
            subtasks = task.steps.map { step -> ScheduledSubtask(step, false, null, step.title) },
        )
        app.taskRepository.setStepCompleted(item, task.steps.single().id, true)

        assertNotNull(app.currentTaskNotificationTarget(id, today, TaskNotificationAction.Complete))
    }

    @Test
    fun habitActionValidationRejectsStaleLifecycleScheduleModeStateAndPayload() = runBlocking {
        assertFalse(
            app.isCurrentHabitNotificationAction(
                Long.MAX_VALUE,
                today,
                HabitNotificationAction.Complete,
                1.0,
            ),
        )

        val archived = app.habitRepository.create(reminderHabit("Archived"))
        app.habitRepository.setArchived(archived, true)
        assertFalse(app.isCurrentHabitNotificationAction(archived, today, HabitNotificationAction.Complete, 1.0))

        val paused = app.habitRepository.create(reminderHabit("Paused"))
        app.habitRepository.setPaused(paused, true)
        assertFalse(app.isCurrentHabitNotificationAction(paused, today, HabitNotificationAction.Complete, 1.0))

        val datedPause = app.habitRepository.create(reminderHabit("Date pause"))
        app.habitRepository.addPause(datedPause, today, today)
        assertFalse(app.isCurrentHabitNotificationAction(datedPause, today, HabitNotificationAction.Complete, 1.0))

        val otherDay = DayOfWeek.entries.first { it != today.dayOfWeek }
        val unscheduled = app.habitRepository.create(
            reminderHabit("Not today").copy(
                scheduleType = HabitScheduleType.SelectedWeekdays,
                weekdays = setOf(otherDay),
            ),
        )
        assertFalse(app.isCurrentHabitNotificationAction(unscheduled, today, HabitNotificationAction.Complete, 1.0))

        val count = app.habitRepository.create(
            reminderHabit("Count", HabitTrackingMode.Count).copy(targetMin = 5.0, quickIncrement = 2.0),
        )
        assertFalse(app.isCurrentHabitNotificationAction(count, today, HabitNotificationAction.Complete, 1.0))
        assertFalse(app.isCurrentHabitNotificationAction(count, today, HabitNotificationAction.Increment, 1.0))
        assertNotNull(app.habitRepository.get(count))
        assertFalse(app.habitRepository.logs.first().any { it.habitId == count })

        val completed = app.habitRepository.create(reminderHabit("Completed"))
        app.habitRepository.setCheckOff(completed, today, true)
        assertFalse(app.isCurrentHabitNotificationAction(completed, today, HabitNotificationAction.Complete, 1.0))

        val current = app.habitRepository.create(reminderHabit("Current"))
        assertEquals(true, app.isCurrentHabitNotificationAction(current, today, HabitNotificationAction.Complete, 1.0))
        assertEquals(true, app.isCurrentHabitNotificationAction(current, today, HabitNotificationAction.Snooze, 1.0))
        assertEquals(true, app.isCurrentHabitNotificationAction(count, today, HabitNotificationAction.Increment, 2.0))

        val sourceLinked = app.habitRepository.create(
            reminderHabit("Health-linked", HabitTrackingMode.Count).copy(
                sourceMetricId = "health:steps",
                targetMin = 5_000.0,
                quickIncrement = 1_000.0,
            ),
        )
        assertFalse(
            app.isCurrentHabitNotificationAction(
                sourceLinked,
                today,
                HabitNotificationAction.Increment,
                1_000.0,
            ),
        )
        assertFalse(
            app.isCurrentHabitNotificationAction(
                sourceLinked,
                today,
                HabitNotificationAction.Complete,
                1.0,
            ),
        )
    }

    @Test
    fun habitClaimRejectsReminderMutationAndLegacyPayloadsFailClosed() = runBlocking {
        val original = reminderHabit("Claimed habit")
        val habitId = app.habitRepository.create(original)
        val claim = requireNotNull(app.currentHabitReminderDeliveryClaim(habitId, today))
        assertTrue(
            app.isCurrentHabitNotificationAction(
                habitId,
                today,
                HabitNotificationAction.Complete,
                1.0,
                claim,
            ),
        )
        assertNull(
            app.currentHabitReminderDeliveryClaim(
                habitId,
                today,
                expectedTriggerAtMillis = app.clock.now().plusSeconds(60).toEpochMilli(),
            ),
        )

        app.habitRepository.update(habitId, original.copy(reminderMinutes = listOf(10 * 60)))
        assertFalse(
            app.isCurrentHabitNotificationAction(
                habitId,
                today,
                HabitNotificationAction.Complete,
                1.0,
                claim,
            ),
        )

        val legacyId = app.habitRepository.create(reminderHabit("Legacy action"))
        app.sendBroadcast(
            Intent(app, HabitReminderActionReceiver::class.java)
                .putExtra(USER_DATA_GENERATION_KEY, app.currentUserDataGeneration())
                .setAction(HabitReminderActionReceiver.ACTION_COMPLETE)
                .putExtra(HabitReminderActionReceiver.EXTRA_HABIT_ID, legacyId)
                .putExtra(HabitReminderActionReceiver.EXTRA_LOGICAL_EPOCH_DAY, today.toEpochDay())
                .putExtra(HabitReminderActionReceiver.EXTRA_ACTION_TOKEN, System.nanoTime()),
        )
        delay(750)
        assertFalse(app.habitRepository.logs.first().any { it.habitId == legacyId })

        val malformedId = app.habitRepository.create(reminderHabit("Malformed persistence"))
        val malformed = requireNotNull(app.database.habitDao().getHabit(malformedId))
        app.database.habitDao().updateHabit(malformed.copy(reminderMinutesCsv = "540,not-a-minute"))
        assertNull(app.currentHabitReminderDeliveryClaim(malformedId, today))
    }

    @Test
    fun habitClaimKeepsPartialSourceProgressCurrentButRejectsLiveClosureAndDefinitionChanges() = runBlocking {
        val sourceMetricId = app.measurementRepository.ensureMetric(
            id = "test-source-progress",
            name = "Source progress",
            valueKind = MetricValueKind.Decimal,
            dimension = UnitDimension.Count,
            defaultUnitId = "count",
            precision = 1,
        )
        val halfScaleUnitId = app.measurementRepository.createCustomUnit(
            name = "Half-scale count",
            symbol = "half",
            dimension = UnitDimension.Count,
            toCanonicalFactor = 2.0,
        )
        val sourceDraft = reminderHabit("Converted source", HabitTrackingMode.Count).copy(
            unitId = halfScaleUnitId,
            dimension = UnitDimension.Count,
            precision = 1,
            targetMin = 5.0,
            quickIncrement = 1.0,
            sourceMetricId = sourceMetricId,
        )
        val sourceHabitId = app.habitRepository.create(sourceDraft)
        val sourceClaim = requireNotNull(app.currentHabitReminderDeliveryClaim(sourceHabitId, today))

        val entryId = app.measurementRepository.record(
            metricId = sourceMetricId,
            value = 8.0,
            unitId = "count",
            localDate = today,
            sourceType = MetricSourceType.HealthConnect,
            sourceId = "partial",
        )
        assertTrue(
            app.isCurrentHabitNotificationAction(
                sourceHabitId,
                today,
                HabitNotificationAction.Snooze,
                1.0,
                sourceClaim,
            ),
        )

        app.measurementRepository.record(
            metricId = sourceMetricId,
            value = 10.0,
            unitId = "count",
            localDate = today,
            sourceType = MetricSourceType.HealthConnect,
            sourceId = "complete",
            existingEntryId = entryId,
        )
        assertFalse(
            app.isCurrentHabitNotificationAction(
                sourceHabitId,
                today,
                HabitNotificationAction.Snooze,
                1.0,
                sourceClaim,
            ),
        )

        app.measurementRepository.deleteEntry(entryId)
        assertTrue(
            app.isCurrentHabitNotificationAction(
                sourceHabitId,
                today,
                HabitNotificationAction.Snooze,
                1.0,
                sourceClaim,
            ),
        )
        app.habitRepository.update(sourceHabitId, sourceDraft.copy(sourceMetricId = "another-source"))
        assertFalse(
            app.isCurrentHabitNotificationAction(
                sourceHabitId,
                today,
                HabitNotificationAction.Snooze,
                1.0,
                sourceClaim,
            ),
        )

        val pausedId = app.habitRepository.create(reminderHabit("Claim then pause"))
        val pausedClaim = requireNotNull(app.currentHabitReminderDeliveryClaim(pausedId, today))
        app.habitRepository.setPaused(pausedId, true)
        assertFalse(
            app.isCurrentHabitNotificationAction(
                pausedId, today, HabitNotificationAction.Snooze, 1.0, pausedClaim,
            ),
        )

        val skippedId = app.habitRepository.create(reminderHabit("Claim then skip"))
        val skippedClaim = requireNotNull(app.currentHabitReminderDeliveryClaim(skippedId, today))
        app.habitRepository.skipDay(skippedId, today)
        assertFalse(
            app.isCurrentHabitNotificationAction(
                skippedId, today, HabitNotificationAction.Snooze, 1.0, skippedClaim,
            ),
        )

        val archivedId = app.habitRepository.create(reminderHabit("Claim then archive"))
        val archivedClaim = requireNotNull(app.currentHabitReminderDeliveryClaim(archivedId, today))
        app.habitRepository.setArchived(archivedId, true)
        assertFalse(
            app.isCurrentHabitNotificationAction(
                archivedId, today, HabitNotificationAction.Snooze, 1.0, archivedClaim,
            ),
        )
    }

    @Test
    fun sourceMetricResyncOnlyTouchesLinkedReminderEnabledHabits() = runBlocking {
        val sourceMetricId = "bounded-source"
        val linkedId = app.habitRepository.create(
            reminderHabit("Linked reminder").copy(sourceMetricId = sourceMetricId),
        )
        val noReminderId = app.habitRepository.create(
            reminderHabit("Linked without reminder").copy(
                sourceMetricId = sourceMetricId,
                reminderMinutes = emptyList(),
            ),
        )
        val unrelatedId = app.habitRepository.create(
            reminderHabit("Unrelated reminder").copy(sourceMetricId = "different-source"),
        )
        val archivedId = app.habitRepository.create(
            reminderHabit("Archived linked reminder").copy(sourceMetricId = sourceMetricId),
        )
        app.habitRepository.setArchived(archivedId, true)

        app.habitReminderScheduler.syncSourceMetric(sourceMetricId)

        fun hasCurrentWork(habitId: Long): Boolean = WorkManager.getInstance(app)
            .getWorkInfosByTag("whip-habit-reminder-$habitId")
            .get()
            .any { it.state != WorkInfo.State.CANCELLED }
        assertTrue(hasCurrentWork(linkedId))
        assertFalse(hasCurrentWork(noReminderId))
        assertFalse(hasCurrentWork(unrelatedId))
        assertFalse(hasCurrentWork(archivedId))

        val historicalUnitId = app.measurementRepository.createCustomUnit(
            name = "Historical conversion",
            symbol = "hc",
            dimension = UnitDimension.Count,
            toCanonicalFactor = 2.0,
        )
        val historicalUnitHabitId = app.habitRepository.create(
            reminderHabit("Historical unit reminder", HabitTrackingMode.Count).copy(targetMin = 10.0),
        )
        val historicalLogId = app.habitRepository.log(historicalUnitHabitId, 2.0, date = today)
        app.habitRepository.updateLog(
            historicalLogId,
            value = 2.0,
            status = HabitLogStatus.Recorded,
            date = today,
            enteredUnitId = historicalUnitId,
        )

        app.habitReminderScheduler.syncUnit(historicalUnitId)

        assertTrue(hasCurrentWork(historicalUnitHabitId))
    }

    @Test
    fun applicationObserverReconcilesSourceHabitAfterPartialAndCompletingMeasurements() = runBlocking {
        val sourceMetricId = app.measurementRepository.ensureMetric(
            id = "observer-source-progress",
            name = "Observer source progress",
            valueKind = MetricValueKind.Decimal,
            dimension = UnitDimension.Count,
            defaultUnitId = "count",
            precision = 1,
        )
        val reminderMinute = app.clock.now().atZone(app.settingsRepository.current().zoneId())
            .plusMinutes(2)
        val logicalDate = reminderMinute.toLocalDate()
        val habitId = app.habitRepository.create(
            reminderHabit("Observed source", HabitTrackingMode.Count).copy(
                startDate = logicalDate,
                targetMin = 10.0,
                sourceMetricId = sourceMetricId,
                reminderMinutes = listOf(reminderMinute.hour * 60 + reminderMinute.minute),
            ),
        )
        app.habitReminderScheduler.syncHabit(habitId)

        val settings = app.settingsRepository.current()
        val dueTrigger = adjustForQuietHours(
            logicalDate.atTime(reminderMinute.hour, reminderMinute.minute)
                .atZone(settings.zoneId())
                .toInstant(),
            settings.zoneId(),
            settings.quietStartMinutes,
            settings.quietEndMinutes,
        ).toEpochMilli()

        fun activeDueWorkIds() = WorkManager.getInstance(app)
            .getWorkInfosForUniqueWork("whip-habit-reminder-$habitId-$dueTrigger")
            .get()
            .filter { it.state != WorkInfo.State.CANCELLED }
            .map { it.id }
            .toSet()

        val initialIds = activeDueWorkIds()
        assertTrue(initialIds.isNotEmpty())
        app.measurementRepository.record(
            metricId = sourceMetricId,
            value = 5.0,
            unitId = "count",
            localDate = logicalDate,
            sourceType = MetricSourceType.HealthConnect,
            sourceId = "observer-partial",
        )
        eventually { activeDueWorkIds().let { it.isNotEmpty() && it != initialIds } }

        app.measurementRepository.record(
            metricId = sourceMetricId,
            value = 5.0,
            unitId = "count",
            localDate = logicalDate,
            sourceType = MetricSourceType.Import,
            sourceId = "observer-complete",
        )
        eventually { activeDueWorkIds().isEmpty() }
    }

    @Test
    fun habitSnoozeActionQueuesSnoozedWorkWithoutWritingHistory() = runBlocking {
        val habitId = app.habitRepository.create(reminderHabit("Snooze claim"))
        app.sendBroadcast(habitIntent(HabitReminderActionReceiver.ACTION_SNOOZE, habitId, today))

        eventually {
            WorkManager.getInstance(app)
                .getWorkInfosForUniqueWork("whip-habit-reminder-$habitId-snooze")
                .get()
                .any { it.state != WorkInfo.State.CANCELLED }
        }
        assertFalse(app.habitRepository.logs.first().any { it.habitId == habitId })
    }

    @Test
    fun goalSnoozeRequiresTheCompleteCurrentClaimAndLegacyActionsFailClosed() = runBlocking {
        val now = app.clock.now()
        val logicalDate = now.atZone(app.settingsRepository.current().zoneId()).toLocalDate()
        val original = GoalDraft(
            name = "Exact Goal snooze",
            type = GoalType.ReachValue,
            startDate = logicalDate,
            targetMin = 10.0,
            reminderMinutes = 9 * 60,
        )
        val goalId = app.goalRepository.create(original)
        val stored = requireNotNull(app.database.goalDao().getGoal(goalId))
        val settings = app.settingsRepository.current()
        val claim = ReminderDeliveryClaim(
            kind = ReminderDeliveryKind.Snoozed,
            stableEntityId = stored.uuid,
            logicalEpochDay = logicalDate.toEpochDay(),
            expectedTriggerAtMillis = now.toEpochMilli(),
            definitionFingerprint = goalReminderSemanticFingerprint(
                stored,
                settings.zoneId(),
                settings.quietStartMinutes,
                settings.quietEndMinutes,
            ),
        )
        val validIntent = goalIntent(goalId, claim)
        app.sendBroadcast(validIntent)
        eventually {
            WorkManager.getInstance(app)
                .getWorkInfosForUniqueWork("whip-goal-reminder-$goalId-snooze")
                .get()
                .any { it.state != WorkInfo.State.CANCELLED }
        }

        app.goalRepository.update(goalId, original.copy(reminderMinutes = 10 * 60))
        app.sendBroadcast(goalIntent(goalId, claim))
        eventually {
            WorkManager.getInstance(app)
                .getWorkInfosForUniqueWork("whip-goal-reminder-$goalId-snooze")
                .get()
                .none { it.state != WorkInfo.State.CANCELLED }
        }

        val legacyId = app.goalRepository.create(original.copy(name = "Legacy Goal action"))
        app.sendBroadcast(
            Intent(app, GoalReminderActionReceiver::class.java)
                .setAction(GoalReminderActionReceiver.ACTION_SNOOZE)
                .putExtra(USER_DATA_GENERATION_KEY, app.currentUserDataGeneration())
                .putExtra(GoalReminderActionReceiver.EXTRA_GOAL_ID, legacyId)
                .putExtra(GoalReminderActionReceiver.EXTRA_ACTION_TOKEN, System.nanoTime()),
        )
        delay(750)
        assertTrue(
            WorkManager.getInstance(app)
                .getWorkInfosForUniqueWork("whip-goal-reminder-$legacyId-snooze")
                .get()
                .none { it.state != WorkInfo.State.CANCELLED },
        )
    }

    @Test
    fun validTaskAndHabitReceiverActionsStillCompleteIncrementAndUndo() = runBlocking {
        val taskId = app.taskRepository.create(reminderTask("Receiver task"))
        app.sendBroadcast(taskIntent(ReminderActionReceiver.ACTION_COMPLETE, taskId, today))
        eventually { app.taskRepository.getTask(taskId)?.completedAtMillis != null }

        app.sendBroadcast(taskIntent(ReminderActionReceiver.ACTION_UNDO, taskId, today))
        eventually { app.taskRepository.getTask(taskId)?.completedAtMillis == null }

        val checkOffId = app.habitRepository.create(reminderHabit("Receiver check-off"))
        app.sendBroadcast(habitIntent(HabitReminderActionReceiver.ACTION_COMPLETE, checkOffId, today))
        eventually {
            app.habitRepository.logs.first().any { it.habitId == checkOffId && (it.value ?: 0.0) > 0.0 }
        }

        val countId = app.habitRepository.create(
            reminderHabit("Receiver count", HabitTrackingMode.Count).copy(
                targetMin = 5.0,
                quickIncrement = 2.5,
                precision = 1,
            ),
        )
        app.sendBroadcast(
            habitIntent(HabitReminderActionReceiver.ACTION_INCREMENT, countId, today)
                .putExtra(HabitReminderActionReceiver.EXTRA_INCREMENT, 2.5),
        )
        eventually {
            app.habitRepository.logs.first().any { it.habitId == countId && it.value == 2.5 }
        }
    }

    @Test
    fun taskReceiverFingerprintRejectsScheduleMutationButAllowsPresentationEdit() = runBlocking {
        val staleId = app.taskRepository.create(reminderTask("Original schedule"))
        val staleIntent = taskIntent(ReminderActionReceiver.ACTION_COMPLETE, staleId, today)
        app.taskRepository.update(
            staleId,
            reminderTask("Original schedule").copy(timeMinutes = 10 * 60),
        )
        app.sendBroadcast(staleIntent)
        delay(750)
        assertNull(app.taskRepository.getTask(staleId)?.completedAtMillis)

        val presentationId = app.taskRepository.create(reminderTask("Before rename"))
        val stillCurrentIntent = taskIntent(ReminderActionReceiver.ACTION_COMPLETE, presentationId, today)
        app.taskRepository.update(presentationId, reminderTask("After rename").copy(notes = "More context"))
        app.sendBroadcast(stillCurrentIntent)
        eventually { app.taskRepository.getTask(presentationId)?.completedAtMillis != null }
    }

    @Test
    fun staleReceiverActionsCancelWithoutCreatingTaskOrHabitHistory() = runBlocking {
        val originalTask = reminderTask("Now requires review")
        val taskId = app.taskRepository.create(originalTask)
        val staleTaskIntent = taskIntent(ReminderActionReceiver.ACTION_COMPLETE, taskId, today)
        app.taskRepository.update(
            taskId,
            originalTask.copy(
                steps = listOf(TaskStepDraft(title = "Added after notification", position = 0)),
            ),
        )
        app.sendBroadcast(staleTaskIntent)

        val habitId = app.habitRepository.create(reminderHabit("No longer active"))
        val staleHabitIntent = habitIntent(HabitReminderActionReceiver.ACTION_COMPLETE, habitId, today)
        app.habitRepository.setArchived(habitId, true)
        app.sendBroadcast(staleHabitIntent)

        // Stale paths intentionally have no success state to await. Give both async receivers
        // enough time to validate and refresh, then prove neither domain gained history.
        delay(750)
        assertNull(app.taskRepository.getTask(taskId)?.completedAtMillis)
        assertTrue(app.taskRepository.getOccurrences(taskId).isEmpty())
        assertFalse(app.habitRepository.logs.first().any { it.habitId == habitId })
    }

    private fun reminderTask(name: String) = TaskDraft(
        title = name,
        scheduleKind = ScheduleKind.Once,
        date = today,
        timeMinutes = 9 * 60,
        reminderEnabled = true,
    )

    private fun reminderHabit(
        name: String,
        mode: HabitTrackingMode = HabitTrackingMode.CheckOff,
    ) = HabitDraft(
        name = name,
        trackingMode = mode,
        startDate = today,
        reminderMinutes = listOf(9 * 60),
    )

    private suspend fun taskIntent(action: String, taskId: Long, date: LocalDate): Intent {
        val taskAction = when (action) {
            ReminderActionReceiver.ACTION_COMPLETE -> TaskNotificationAction.Complete
            ReminderActionReceiver.ACTION_SNOOZE -> TaskNotificationAction.Snooze
            ReminderActionReceiver.ACTION_UNDO -> TaskNotificationAction.Undo
            else -> error("Unsupported Task action")
        }
        val target = requireNotNull(
            app.currentTaskNotificationTarget(taskId, date, taskAction, offsetMinutes = 0),
        )
        return Intent(app, ReminderActionReceiver::class.java)
            .putExtra(USER_DATA_GENERATION_KEY, app.currentUserDataGeneration())
            .setAction(action)
            .putExtra(ReminderActionReceiver.EXTRA_TASK_ID, taskId)
            .putExtra(ReminderActionReceiver.EXTRA_ORIGINAL_EPOCH_DAY, date.toEpochDay())
            .putExtra(ReminderActionReceiver.EXTRA_OFFSET_MINUTES, 0)
            .putExtra(ReminderActionReceiver.EXTRA_STABLE_ENTITY_ID, target.stableEntityId)
            .putExtra(ReminderActionReceiver.EXTRA_DEFINITION_FINGERPRINT, target.definitionFingerprint)
            .putExtra(ReminderActionReceiver.EXTRA_ACTION_TOKEN, System.nanoTime())
    }

    private suspend fun habitIntent(action: String, habitId: Long, date: LocalDate): Intent {
        val claim = requireNotNull(app.currentHabitReminderDeliveryClaim(habitId, date))
        return Intent(app, HabitReminderActionReceiver::class.java)
            .putExtra(USER_DATA_GENERATION_KEY, app.currentUserDataGeneration())
            .setAction(action)
            .putExtra(HabitReminderActionReceiver.EXTRA_HABIT_ID, habitId)
            .putExtra(HabitReminderActionReceiver.EXTRA_LOGICAL_EPOCH_DAY, date.toEpochDay())
            .putHabitClaim(claim)
            .putExtra(HabitReminderActionReceiver.EXTRA_ACTION_TOKEN, System.nanoTime())
    }

    private fun Intent.putHabitClaim(claim: ReminderDeliveryClaim): Intent =
        putExtra(HabitReminderActionReceiver.EXTRA_STABLE_ENTITY_ID, claim.stableEntityId)
            .putExtra(HabitReminderActionReceiver.EXTRA_DEFINITION_FINGERPRINT, claim.definitionFingerprint)
            .putExtra(HabitReminderActionReceiver.EXTRA_EXPECTED_TRIGGER_AT_MILLIS, claim.expectedTriggerAtMillis)
            .putExtra(HabitReminderActionReceiver.EXTRA_DELIVERY_KIND, claim.kind.name)
            .putExtra(HabitReminderActionReceiver.EXTRA_CLAIM_VERSION, claim.version)

    private fun goalIntent(goalId: Long, claim: ReminderDeliveryClaim): Intent =
        Intent(app, GoalReminderActionReceiver::class.java)
            .setAction(GoalReminderActionReceiver.ACTION_SNOOZE)
            .putExtra(USER_DATA_GENERATION_KEY, app.currentUserDataGeneration())
            .putExtra(GoalReminderActionReceiver.EXTRA_GOAL_ID, goalId)
            .putExtra(GoalReminderActionReceiver.EXTRA_LOGICAL_EPOCH_DAY, claim.logicalEpochDay)
            .putExtra(GoalReminderActionReceiver.EXTRA_STABLE_ENTITY_ID, claim.stableEntityId)
            .putExtra(GoalReminderActionReceiver.EXTRA_DEFINITION_FINGERPRINT, claim.definitionFingerprint)
            .putExtra(GoalReminderActionReceiver.EXTRA_EXPECTED_TRIGGER_AT_MILLIS, claim.expectedTriggerAtMillis)
            .putExtra(GoalReminderActionReceiver.EXTRA_DELIVERY_KIND, claim.kind.name)
            .putExtra(GoalReminderActionReceiver.EXTRA_CLAIM_VERSION, claim.version)
            .putExtra(GoalReminderActionReceiver.EXTRA_ACTION_TOKEN, System.nanoTime())

    private suspend fun eventually(predicate: suspend () -> Boolean) {
        withTimeout(5_000) {
            while (!predicate()) delay(25)
        }
    }
}
