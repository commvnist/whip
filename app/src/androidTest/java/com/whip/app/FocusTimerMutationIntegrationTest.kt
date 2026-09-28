package com.whip.app

import android.app.NotificationManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import androidx.work.await
import com.whip.app.core.PersistenceRequestState
import com.whip.app.core.WhipResult
import com.whip.app.domain.TaskDraft
import com.whip.app.reminders.ALL_WHIP_WORK_TAG
import com.whip.app.startup.StartupRecoveryState
import com.whip.app.ui.FocusTimerMutationReceipt
import com.whip.app.ui.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FocusTimerMutationIntegrationTest {
    private lateinit var app: WhipApplication
    private lateinit var viewModel: SettingsViewModel
    private val store = ViewModelStore()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Before fun prepare() = runBlocking {
        app = ApplicationProvider.getApplicationContext()
        withTimeout(10_000L) {
            while (app.startupRecoveryState.value != StartupRecoveryState.Ready) delay(20L)
        }
        WorkManager.getInstance(app).cancelAllWorkByTag(ALL_WHIP_WORK_TAG).await()
        app.timerAlarmScheduler.cancelAll()
        app.backupRepository.deleteAllData()
        app.settingsRepository.updateAndConfirm {
            it.copy(focusTimerTaskId = null, focusTimerDeadlineMillis = null)
        }
        instrumentation.runOnMainSync {
            viewModel = ViewModelProvider(store, ViewModelProvider.AndroidViewModelFactory.getInstance(app))[
                SettingsViewModel::class.java,
            ]
        }
    }

    @After fun cleanUp() = runBlocking {
        instrumentation.runOnMainSync { store.clear() }
        WorkManager.getInstance(app).cancelAllWorkByTag(ALL_WHIP_WORK_TAG).await()
        app.timerAlarmScheduler.cancelAll()
        app.backupRepository.deleteAllData()
        app.settingsRepository.updateAndConfirm {
            it.copy(focusTimerTaskId = null, focusTimerDeadlineMillis = null)
        }
        app.getSystemService(NotificationManager::class.java).cancelAll()
    }

    @Test fun confirmedStartUsesThirtyMinutesAndOnlyItsOwnerCanConsumeTheResult() = runBlocking {
        val taskId = app.taskRepository.create(TaskDraft(title = "A confirmed Focus session"))
        val before = System.currentTimeMillis()
        instrumentation.runOnMainSync {
            assertTrue(viewModel.startFocusTimer("start", taskId))
            assertFalse(viewModel.startFocusTimer("duplicate", taskId, 60))
        }
        val result = finished("start")
        val receipt = (result.result as WhipResult.Success).value.started!!
        assertEquals(30, receipt.minutes)
        assertEquals(taskId, receipt.taskId)
        assertEquals("A confirmed Focus session", receipt.taskTitle)
        assertTrue(receipt.deadlineMillis in (before + 30 * 60_000L)..(System.currentTimeMillis() + 30 * 60_000L))
        assertEquals(receipt.deadlineMillis, app.settingsRepository.current().focusTimerDeadlineMillis)
        instrumentation.runOnMainSync {
            viewModel.consumeFocusTimerMutation("someone-else")
            assertEquals(result, viewModel.focusTimerMutationState.value)
            assertFalse(viewModel.startFocusTimer("unconsumed", taskId, 45))
            viewModel.consumeFocusTimerMutation("start")
            assertEquals(PersistenceRequestState.Idle, viewModel.focusTimerMutationState.value)
        }
    }

    @Test fun failedCustomDurationRetainsTheExistingTimerAndAllowsAnExplicitCorrection() = runBlocking {
        val taskId = app.taskRepository.create(TaskDraft(title = "Custom duration"))
        val original = app.focusTimerScheduler.start(taskId, 15)
        instrumentation.runOnMainSync {
            assertTrue(viewModel.startFocusTimer("invalid", taskId, 241, taskId, original.deadlineMillis))
        }
        assertTrue(finished("invalid").result is WhipResult.Failure)
        assertEquals(original.deadlineMillis, app.settingsRepository.current().focusTimerDeadlineMillis)
        instrumentation.runOnMainSync {
            viewModel.consumeFocusTimerMutation("invalid")
            assertTrue(viewModel.startFocusTimer("corrected", taskId, 37, taskId, original.deadlineMillis))
        }
        val corrected = (finished("corrected").result as WhipResult.Success).value.started!!
        assertEquals(37, corrected.minutes)
        assertEquals(corrected.deadlineMillis, app.settingsRepository.current().focusTimerDeadlineMillis)
    }

    @Test fun staleStopAcknowledgesNoChangeAndCurrentStopClearsTheDurablePair() = runBlocking {
        val taskId = app.taskRepository.create(TaskDraft(title = "Stop the current session"))
        val original = app.focusTimerScheduler.start(taskId, 15)
        val replacement = app.focusTimerScheduler.start(taskId, 45, taskId, original.deadlineMillis)
        instrumentation.runOnMainSync {
            assertTrue(viewModel.stopFocusTimer("stale-stop", taskId, original.deadlineMillis))
        }
        assertFalse((finished("stale-stop").result as WhipResult.Success).value.stopped)
        assertEquals(replacement.deadlineMillis, app.settingsRepository.current().focusTimerDeadlineMillis)
        instrumentation.runOnMainSync {
            viewModel.consumeFocusTimerMutation("stale-stop")
            assertTrue(viewModel.stopFocusTimer("current-stop", taskId, replacement.deadlineMillis))
        }
        assertTrue((finished("current-stop").result as WhipResult.Success).value.stopped)
        assertNull(app.settingsRepository.current().focusTimerTaskId)
        assertNull(app.settingsRepository.current().focusTimerDeadlineMillis)
    }

    private suspend fun finished(requestId: String): PersistenceRequestState.Finished<FocusTimerMutationReceipt> =
        withTimeout(10_000L) {
            viewModel.focusTimerMutationState.first {
                it is PersistenceRequestState.Finished && it.requestId == requestId
            } as PersistenceRequestState.Finished<FocusTimerMutationReceipt>
        }
}
