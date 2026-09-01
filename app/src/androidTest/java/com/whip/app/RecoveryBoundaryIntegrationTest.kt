package com.whip.app

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import com.whip.app.data.PortableBackupWorker
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskDraft
import com.whip.app.reminders.ReminderActionReceiver
import com.whip.app.reminders.FocusTimerScheduler
import com.whip.app.startup.StartupRecoveryState
import com.whip.app.startup.USER_DATA_GENERATION_KEY
import com.whip.app.widget.WhipWidgetProvider
import com.whip.app.widget.WhipWidgetPreferences
import com.whip.app.widget.WidgetPreferences
import com.whip.app.widget.persistWidgetConfiguration
import com.whip.app.health.HealthPermissionsRationaleActivity
import com.whip.app.core.OperationStatus
import com.whip.app.ui.TaskViewModel
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecoveryBoundaryIntegrationTest {
    @Test
    fun blockedRecoveryPreventsWorkerNotificationAndWidgetDataAccess() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        app.backupRepository.deleteAllData()
        val today = app.clock.today()
        val taskId = app.taskRepository.create(
            TaskDraft(
                title = "Must remain unchanged while recovery is blocked",
                scheduleKind = ScheduleKind.Once,
                date = today,
                inbox = false,
                reminderEnabled = true,
                timeMinutes = 9 * 60,
            ),
        )
        val marker = File(app.noBackupFilesDir, "restore-recovery.whip.json")
        check(!marker.exists()) { "A real pending recovery must never be overwritten by a test" }
        val previousPortableError = app.portableBackupManager.state.value.lastError

        try {
            marker.writeText("intentionally corrupt recovery snapshot")
            app.blockForPendingRecovery()
            assertEquals(
                com.whip.app.startup.StartupRecoveryState.Blocked(
                    com.whip.app.startup.StartupBlockReason.Recovery,
                ),
                app.startupRecoveryState.value,
            )

            val worker = TestListenableWorkerBuilder<PortableBackupWorker>(app).build()
            assertEquals(ListenableWorker.Result.retry(), worker.doWork())
            assertEquals(previousPortableError, app.portableBackupManager.state.value.lastError)

            app.focusTimerScheduler.schedule(taskId, System.currentTimeMillis() + 60_000L)
            assertTrue(
                WorkManager.getInstance(app)
                    .getWorkInfosForUniqueWork(FocusTimerScheduler.uniqueName)
                    .get()
                    .none { it.state != WorkInfo.State.CANCELLED },
            )

            val generation = app.currentUserDataGeneration()
            app.sendBroadcast(
                Intent(app, ReminderActionReceiver::class.java)
                    .setAction(ReminderActionReceiver.ACTION_COMPLETE)
                    .putExtra(ReminderActionReceiver.EXTRA_TASK_ID, taskId)
                    .putExtra(ReminderActionReceiver.EXTRA_ORIGINAL_EPOCH_DAY, today.toEpochDay())
                    .putExtra(ReminderActionReceiver.EXTRA_ACTION_TOKEN, System.nanoTime())
                    .putExtra(USER_DATA_GENERATION_KEY, generation),
            )
            WhipWidgetProvider().onReceive(
                app,
                Intent(app, WhipWidgetProvider::class.java)
                    .setAction(WhipWidgetProvider.ACTION_COMPLETE_TASK)
                    .putExtra(WhipWidgetProvider.EXTRA_TASK_ID, taskId)
                    .putExtra(WhipWidgetProvider.EXTRA_OCCURRENCE_EPOCH_DAY, today.toEpochDay())
                    .putExtra(USER_DATA_GENERATION_KEY, generation),
            )

            delay(750)
            assertNull(app.taskRepository.getTask(taskId)?.completedAtMillis)
            assertEquals(previousPortableError, app.portableBackupManager.state.value.lastError)
        } finally {
            marker.delete()
            app.retryStartupRecovery()
            withTimeout(10_000) {
                while (app.startupRecoveryState.value != StartupRecoveryState.Ready) delay(20)
            }
            app.backupRepository.deleteAllData()
        }
    }

    @Test
    fun widgetConfigurationSaveCannotCrossAClosedRecoveryBoundary() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        val widgetId = 73_998
        val marker = File(app.noBackupFilesDir, "restore-recovery.whip.json")
        check(!marker.exists()) { "A real pending recovery must never be overwritten by a test" }
        WhipWidgetPreferences.remove(app, intArrayOf(widgetId))
        val generation = app.currentUserDataGeneration()
        WhipWidgetPreferences.save(
            context = app,
            appWidgetId = widgetId,
            value = WidgetPreferences(transparencyPercent = 20),
            dataGeneration = generation,
        )

        try {
            marker.writeText("intentionally corrupt recovery snapshot")
            app.blockForPendingRecovery()

            val saved = persistWidgetConfiguration(
                app = app,
                appWidgetId = widgetId,
                preferences = WidgetPreferences(transparencyPercent = 80),
                expectedDataGeneration = generation,
                updateWidget = { _, _ -> error("A blocked save must not update the widget") },
            )

            assertEquals(false, saved)
            assertEquals(
                20,
                WhipWidgetPreferences.load(app, widgetId, dataGeneration = generation)
                    .transparencyPercent,
            )
        } finally {
            marker.delete()
            app.retryStartupRecovery()
            withTimeout(10_000) {
                while (app.startupRecoveryState.value != StartupRecoveryState.Ready) delay(20)
            }
            WhipWidgetPreferences.remove(app, intArrayOf(widgetId))
        }
    }

    @Test
    fun secondaryActivityShowsRecoveryGateInsteadOfReadingWhipSettings() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        val marker = File(app.noBackupFilesDir, "restore-recovery.whip.json")
        check(!marker.exists()) { "A real pending recovery must never be overwritten by a test" }
        try {
            marker.writeText("intentionally corrupt recovery snapshot")
            app.blockForPendingRecovery()
            assertEquals(
                StartupRecoveryState.Blocked(com.whip.app.startup.StartupBlockReason.Recovery),
                app.startupRecoveryState.value,
            )
            ActivityScenario.launch(HealthPermissionsRationaleActivity::class.java).use {
                val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
                check(
                    device.wait(
                        Until.hasObject(By.text("Whip Couldn't Safely Open Your Data")),
                        5_000,
                    ),
                ) { "The secondary activity did not surface the recovery block" }
                check(device.hasObject(By.text("Retry Recovery"))) {
                    "The secondary recovery screen did not expose its retry action"
                }
            }
        } finally {
            marker.delete()
            app.retryStartupRecovery()
            withTimeout(10_000) {
                while (app.startupRecoveryState.value != StartupRecoveryState.Ready) delay(20)
            }
        }
        Unit
    }

    @Test
    fun failedLiveRestoreRebuildsCurrentReminderStateAndReturnsReady() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        app.backupRepository.deleteAllData()
        val taskId = app.taskRepository.create(
            TaskDraft(
                title = "Reminder survives a rejected restore",
                scheduleKind = ScheduleKind.Once,
                date = app.clock.today().plusDays(1),
                inbox = false,
                reminderEnabled = true,
                timeMinutes = 9 * 60,
            ),
        )
        app.reminderScheduler.syncTask(taskId)
        val initialGeneration = app.currentUserDataGeneration()

        try {
            val result = runCatching { app.restoreBackup("{not-a-valid-whip-backup") }

            assertTrue(result.isFailure)
            assertEquals(StartupRecoveryState.Ready, app.startupRecoveryState.value)
            assertEquals(initialGeneration + 1L, app.currentUserDataGeneration())
            assertEquals(
                "Reminder survives a rejected restore",
                app.taskRepository.getTask(taskId)?.title,
            )
            assertTrue(
                WorkManager.getInstance(app)
                    .getWorkInfosByTag("whip-reminder-$taskId")
                    .get()
                    .any { it.state != WorkInfo.State.CANCELLED },
            )
        } finally {
            app.backupRepository.deleteAllData()
            app.rebuildBackgroundState()
        }
    }

    @Test
    fun replaceRestoreInvalidatesActivityViewModelUndoActionsFromTheOldGeneration() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        app.backupRepository.deleteAllData()
        val taskId = app.taskRepository.create(TaskDraft(title = "Old-generation undo target"))
        val viewModel = TaskViewModel(app)

        try {
            viewModel.archive(taskId)
            withTimeout(10_000) {
                while (viewModel.operationFeedback.value.undoToken == null) delay(20)
            }
            assertTrue(viewModel.operationFeedback.value.status is OperationStatus.Succeeded)

            assertTrue(runCatching { app.restoreBackup("{invalid-backup") }.isFailure)
            withTimeout(10_000) {
                while (
                    viewModel.operationFeedback.value.undoToken != null ||
                    viewModel.operationFeedback.value.status != OperationStatus.Idle
                ) delay(20)
            }

            assertNull(viewModel.operationFeedback.value.undoToken)
            assertEquals(OperationStatus.Idle, viewModel.operationFeedback.value.status)
        } finally {
            app.backupRepository.deleteAllData()
            app.rebuildBackgroundState()
        }
    }
}
