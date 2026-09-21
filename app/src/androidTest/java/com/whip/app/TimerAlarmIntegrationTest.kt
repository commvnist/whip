package com.whip.app

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import com.whip.app.domain.TaskDraft
import com.whip.app.reminders.ALL_WHIP_WORK_TAG
import com.whip.app.reminders.FocusTimerNotifications
import com.whip.app.reminders.RestTimerNotifications
import com.whip.app.reminders.TimerAlarmKind
import com.whip.app.reminders.TimerAlarmPayload
import com.whip.app.reminders.TimerAlarmReceiver
import com.whip.app.reminders.ACTION_EXACT_ALARM_ACCESS_CHANGED
import com.whip.app.reminders.canScheduleExactReminderAlarms
import com.whip.app.startup.StartupRecoveryState
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
class TimerAlarmIntegrationTest {
    private lateinit var app: WhipApplication

    @Before fun prepare() = runBlocking {
        app = ApplicationProvider.getApplicationContext()
        withTimeout(10_000L) {
            while (app.startupRecoveryState.value != StartupRecoveryState.Ready) delay(20L)
        }
        app.timerAlarmScheduler.cancelAll()
        WorkManager.getInstance(app).cancelAllWorkByTag(ALL_WHIP_WORK_TAG).await()
        app.getSystemService(NotificationManager::class.java).cancelAll()
        app.backupRepository.deleteAllData()
        app.settingsRepository.updateAndConfirm {
            it.copy(focusTimerTaskId = null, focusTimerDeadlineMillis = null)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
                app.packageName,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "cmd appops set ${app.packageName} SCHEDULE_EXACT_ALARM allow",
            ).close()
            withTimeout(5_000L) {
                while (!canScheduleExactReminderAlarms(app)) delay(20L)
            }
        }
    }

    @After fun cleanUp() = runBlocking {
        app.timerAlarmScheduler.cancelAll()
        WorkManager.getInstance(app).cancelAllWorkByTag(ALL_WHIP_WORK_TAG).await()
        app.backupRepository.deleteAllData()
        app.settingsRepository.updateAndConfirm {
            it.copy(focusTimerTaskId = null, focusTimerDeadlineMillis = null)
        }
        app.getSystemService(NotificationManager::class.java).cancelAll()
    }

    @Test fun focusTimerWakesAndPostsWithoutOpeningAnActivity() = runBlocking {
        val receiver = app.packageManager.getReceiverInfo(
            ComponentName(app, TimerAlarmReceiver::class.java),
            0,
        )
        assertFalse(receiver.exported)
        val taskId = app.taskRepository.create(TaskDraft(title = "Exact focus alert"))
        val deadline = System.currentTimeMillis() + 3_000L
        assertTrue(app.settingsRepository.updateAndConfirm {
            it.copy(focusTimerTaskId = taskId, focusTimerDeadlineMillis = deadline)
        })
        val payload = TimerAlarmPayload(TimerAlarmKind.Focus, taskId, deadline, app.currentUserDataGeneration())

        app.focusTimerScheduler.schedule(taskId, deadline)
        if (!canScheduleExactReminderAlarms(app)) {
            assertNull(payload.pendingIntent(app, create = false))
            assertTrue(WorkManager.getInstance(app).getWorkInfosForUniqueWork(payload.workName).get()
                .any { it.state == WorkInfo.State.ENQUEUED })
            return@runBlocking
        }
        assertNotNull(payload.pendingIntent(app, create = false))
        try {
            withTimeout(20_000L) {
                while (app.settingsRepository.current().focusTimerDeadlineMillis != null ||
                    app.getSystemService(NotificationManager::class.java).activeNotifications
                        .none { it.id == FocusTimerNotifications.notificationId }
                ) delay(30L)
            }
        } catch (error: Exception) {
            val manager = app.getSystemService(NotificationManager::class.java)
            throw AssertionError(
                "Focus deadline=${app.settingsRepository.current().focusTimerDeadlineMillis}, " +
                    "active=${manager.activeNotifications.map { it.id to it.notification.channelId }}, " +
                    "channel=${manager.getNotificationChannel(FocusTimerNotifications.channelId)?.importance}, " +
                    "enabled=${manager.areNotificationsEnabled()}, " +
                    "permission=${ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED}, " +
                    "work=${WorkManager.getInstance(app).getWorkInfosForUniqueWork(payload.workName).get().map { it.state }}",
                error,
            )
        }
        assertTrue(app.getSystemService(NotificationManager::class.java).activeNotifications
            .any { it.id == FocusTimerNotifications.notificationId })
        assertNull(payload.pendingIntent(app, create = false))
    }

    @Test fun restTimerWakesAndPostsWithoutOpeningAnActivity() = runBlocking {
        val sessionId = app.gymRepository.startWorkout()
        app.gymRepository.startRestTimer(sessionId, 3)
        val session = app.gymRepository.sessions.first().single { it.id == sessionId }
        val deadline = requireNotNull(session.restTimerDeadlineMillis)
        val payload = TimerAlarmPayload(
            TimerAlarmKind.Rest,
            sessionId,
            deadline,
            app.currentUserDataGeneration(),
            timerRevision = session.restTimerRevision,
        )

        app.restTimerScheduler.schedule(
            sessionId,
            null,
            session.restTimerRevision,
            deadline,
        )
        if (!canScheduleExactReminderAlarms(app)) {
            assertNull(payload.pendingIntent(app, create = false))
            assertTrue(WorkManager.getInstance(app).getWorkInfosForUniqueWork(payload.workName).get()
                .any { it.state == WorkInfo.State.ENQUEUED })
            return@runBlocking
        }
        assertNotNull(payload.pendingIntent(app, create = false))
        withTimeout(20_000L) {
            while (app.gymRepository.sessions.first().single { it.id == sessionId }.restTimerDeadlineMillis != null) {
                delay(30L)
            }
        }
        assertTrue(app.getSystemService(NotificationManager::class.java).activeNotifications
            .any { it.id == RestTimerNotifications.notificationId(sessionId) })
        assertNull(payload.pendingIntent(app, create = false))
    }

    @Test fun staleFocusWakeupCannotReplaceANewerTimer() = runBlocking {
        val taskId = app.taskRepository.create(TaskDraft(title = "Rescheduled focus"))
        val firstDeadline = System.currentTimeMillis() + 60_000L
        val secondDeadline = firstDeadline + 60_000L
        app.settingsRepository.updateAndConfirm {
            it.copy(focusTimerTaskId = taskId, focusTimerDeadlineMillis = firstDeadline)
        }
        app.focusTimerScheduler.schedule(taskId, firstDeadline)
        val stale = TimerAlarmPayload(TimerAlarmKind.Focus, taskId, firstDeadline, app.currentUserDataGeneration())
        app.settingsRepository.updateAndConfirm {
            it.copy(focusTimerTaskId = taskId, focusTimerDeadlineMillis = secondDeadline)
        }
        app.focusTimerScheduler.schedule(taskId, secondDeadline)
        app.focusTimerScheduler.promoteIfCurrent(stale)

        assertEquals(secondDeadline, app.settingsRepository.current().focusTimerDeadlineMillis)
        if (canScheduleExactReminderAlarms(app)) {
            assertNotNull(stale.pendingIntent(app, create = false))
        }
        assertTrue(WorkManager.getInstance(app).getWorkInfosForUniqueWork(stale.workName).get()
            .any { it.state == WorkInfo.State.ENQUEUED })
    }

    @Test fun staleRestWakeupCannotReplaceANewerTimer() = runBlocking {
        val sessionId = app.gymRepository.startWorkout()
        app.gymRepository.startRestTimer(sessionId, 90)
        val first = app.gymRepository.sessions.first().single { it.id == sessionId }
        val stale = TimerAlarmPayload(
            TimerAlarmKind.Rest,
            sessionId,
            requireNotNull(first.restTimerDeadlineMillis),
            app.currentUserDataGeneration(),
            timerRevision = first.restTimerRevision,
        )
        app.restTimerScheduler.schedule(
            sessionId, null, first.restTimerRevision, requireNotNull(first.restTimerDeadlineMillis),
        )
        app.gymRepository.adjustRestTimer(sessionId, 90)
        val second = app.gymRepository.sessions.first().single { it.id == sessionId }
        app.restTimerScheduler.schedule(
            sessionId, null, second.restTimerRevision, requireNotNull(second.restTimerDeadlineMillis),
        )

        app.restTimerScheduler.promoteIfCurrent(stale)

        assertEquals(second.restTimerDeadlineMillis,
            app.gymRepository.sessions.first().single { it.id == sessionId }.restTimerDeadlineMillis)
        assertNotNull(stale.pendingIntent(app, create = false))
        assertTrue(WorkManager.getInstance(app).getWorkInfosForUniqueWork(stale.workName).get()
            .any { it.state == WorkInfo.State.ENQUEUED })
    }

    @Test fun lateOldFocusScheduleCannotReplaceTheCurrentTimerWork() = runBlocking {
        val taskId = app.taskRepository.create(TaskDraft(title = "Focus schedule order"))
        val firstDeadline = System.currentTimeMillis() + 90_000L
        val secondDeadline = firstDeadline + 90_000L
        app.settingsRepository.updateAndConfirm {
            it.copy(focusTimerTaskId = taskId, focusTimerDeadlineMillis = firstDeadline)
        }
        app.focusTimerScheduler.schedule(taskId, firstDeadline)
        app.settingsRepository.updateAndConfirm {
            it.copy(focusTimerTaskId = taskId, focusTimerDeadlineMillis = secondDeadline)
        }
        app.focusTimerScheduler.schedule(taskId, secondDeadline)
        val current = WorkManager.getInstance(app).getWorkInfosForUniqueWork("whip-focus-timer").get()
            .single { it.state == WorkInfo.State.ENQUEUED }.id

        app.focusTimerScheduler.schedule(taskId, firstDeadline)

        assertEquals(current, WorkManager.getInstance(app).getWorkInfosForUniqueWork("whip-focus-timer").get()
            .single { it.state == WorkInfo.State.ENQUEUED }.id)
        assertEquals(secondDeadline, app.settingsRepository.current().focusTimerDeadlineMillis)
    }

    @Test fun lateOldRestScheduleCannotReplaceTheCurrentTimerWork() = runBlocking {
        val sessionId = app.gymRepository.startWorkout()
        app.gymRepository.startRestTimer(sessionId, 90)
        val first = app.gymRepository.sessions.first().single { it.id == sessionId }
        app.restTimerScheduler.schedule(
            sessionId, null, first.restTimerRevision, requireNotNull(first.restTimerDeadlineMillis),
        )
        app.gymRepository.adjustRestTimer(sessionId, 90)
        val second = app.gymRepository.sessions.first().single { it.id == sessionId }
        app.restTimerScheduler.schedule(
            sessionId, null, second.restTimerRevision, requireNotNull(second.restTimerDeadlineMillis),
        )
        val workName = "whip-rest-$sessionId"
        val current = WorkManager.getInstance(app).getWorkInfosForUniqueWork(workName).get()
            .single { it.state == WorkInfo.State.ENQUEUED }.id

        app.restTimerScheduler.schedule(
            sessionId, null, first.restTimerRevision, requireNotNull(first.restTimerDeadlineMillis),
        )

        assertEquals(current, WorkManager.getInstance(app).getWorkInfosForUniqueWork(workName).get()
            .single { it.state == WorkInfo.State.ENQUEUED }.id)
        assertEquals(second.restTimerDeadlineMillis,
            app.gymRepository.sessions.first().single { it.id == sessionId }.restTimerDeadlineMillis)
    }

    @Test fun exactAccessGrantRebuildsBothPersistedTimerAlarms() = runBlocking {
        val taskId = app.taskRepository.create(TaskDraft(title = "Rebuilt focus timer"))
        val focusDeadline = System.currentTimeMillis() + 90_000L
        app.settingsRepository.updateAndConfirm {
            it.copy(focusTimerTaskId = taskId, focusTimerDeadlineMillis = focusDeadline)
        }
        app.focusTimerScheduler.schedule(taskId, focusDeadline)
        val focus = TimerAlarmPayload(
            TimerAlarmKind.Focus, taskId, focusDeadline, app.currentUserDataGeneration(),
        )
        val sessionId = app.gymRepository.startWorkout()
        app.gymRepository.startRestTimer(sessionId, 90)
        val session = app.gymRepository.sessions.first().single { it.id == sessionId }
        val rest = TimerAlarmPayload(
            TimerAlarmKind.Rest, sessionId, requireNotNull(session.restTimerDeadlineMillis),
            app.currentUserDataGeneration(), timerRevision = session.restTimerRevision,
        )
        app.restTimerScheduler.schedule(
            sessionId, null, session.restTimerRevision, requireNotNull(session.restTimerDeadlineMillis),
        )
        app.timerAlarmScheduler.cancelAll()
        assertNull(focus.pendingIntent(app, create = false))
        assertNull(rest.pendingIntent(app, create = false))

        app.reconcileReminderTimeInvalidation(ACTION_EXACT_ALARM_ACCESS_CHANGED)

        assertNotNull(focus.pendingIntent(app, create = false))
        assertNotNull(rest.pendingIntent(app, create = false))
    }

    @Test fun longExpiredFocusTimerIsClearedWithoutResurrectingAnAlert() = runBlocking {
        val taskId = app.taskRepository.create(TaskDraft(title = "Old focus timer"))
        val deadline = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1_000L
        app.settingsRepository.updateAndConfirm {
            it.copy(focusTimerTaskId = taskId, focusTimerDeadlineMillis = deadline)
        }
        val payload = TimerAlarmPayload(TimerAlarmKind.Focus, taskId, deadline, app.currentUserDataGeneration())

        app.reconcileReminderTimeInvalidation(ACTION_EXACT_ALARM_ACCESS_CHANGED)

        assertNull(app.settingsRepository.current().focusTimerDeadlineMillis)
        assertNull(payload.pendingIntent(app, create = false))
        assertTrue(app.getSystemService(NotificationManager::class.java).activeNotifications
            .none { it.id == FocusTimerNotifications.notificationId })
    }
}
