package com.whip.app

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.HabitScheduleType
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.RecurrenceRule
import com.whip.app.domain.RecurrenceUnit
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.ScheduledSubtask
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskStepDraft
import com.whip.app.reminders.HabitNotificationAction
import com.whip.app.reminders.HabitReminderActionReceiver
import com.whip.app.reminders.ReminderActionReceiver
import com.whip.app.reminders.TaskNotificationAction
import com.whip.app.reminders.currentTaskNotificationTarget
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
        app.backupRepository.deleteAllData()
        today = app.clock.today()
    }

    @After
    fun tearDown() = runBlocking {
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
    fun staleReceiverActionsCancelWithoutCreatingTaskOrHabitHistory() = runBlocking {
        val taskDraft = reminderTask("Now requires review").copy(
            steps = listOf(TaskStepDraft(title = "Added after notification", position = 0)),
        )
        val taskId = app.taskRepository.create(taskDraft)
        app.sendBroadcast(taskIntent(ReminderActionReceiver.ACTION_COMPLETE, taskId, today))

        val habitId = app.habitRepository.create(reminderHabit("No longer active"))
        app.habitRepository.setArchived(habitId, true)
        app.sendBroadcast(habitIntent(HabitReminderActionReceiver.ACTION_COMPLETE, habitId, today))

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

    private fun taskIntent(action: String, taskId: Long, date: LocalDate) =
        Intent(app, ReminderActionReceiver::class.java)
            .putExtra(USER_DATA_GENERATION_KEY, app.currentUserDataGeneration())
            .setAction(action)
            .putExtra(ReminderActionReceiver.EXTRA_TASK_ID, taskId)
            .putExtra(ReminderActionReceiver.EXTRA_ORIGINAL_EPOCH_DAY, date.toEpochDay())
            .putExtra(ReminderActionReceiver.EXTRA_ACTION_TOKEN, System.nanoTime())

    private fun habitIntent(action: String, habitId: Long, date: LocalDate) =
        Intent(app, HabitReminderActionReceiver::class.java)
            .putExtra(USER_DATA_GENERATION_KEY, app.currentUserDataGeneration())
            .setAction(action)
            .putExtra(HabitReminderActionReceiver.EXTRA_HABIT_ID, habitId)
            .putExtra(HabitReminderActionReceiver.EXTRA_LOGICAL_EPOCH_DAY, date.toEpochDay())
            .putExtra(HabitReminderActionReceiver.EXTRA_ACTION_TOKEN, System.nanoTime())

    private suspend fun eventually(predicate: suspend () -> Boolean) {
        withTimeout(5_000) {
            while (!predicate()) delay(25)
        }
    }
}
