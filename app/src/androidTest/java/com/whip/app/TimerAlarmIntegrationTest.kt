package com.whip.app

import android.Manifest
import android.app.NotificationManager
import android.app.Notification
import android.content.ComponentName
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.work.WorkInfo
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.WorkManager
import androidx.work.await
import com.whip.app.domain.TaskDraft
import com.whip.app.reminders.ALL_WHIP_WORK_TAG
import com.whip.app.reminders.FocusTimerNotifications
import com.whip.app.reminders.FocusTimerStopReceiver
import com.whip.app.reminders.FocusTimerWorker
import com.whip.app.reminders.FocusTimerCompletionReceipt
import com.whip.app.startup.USER_DATA_GENERATION_KEY
import com.whip.app.reminders.RestTimerNotifications
import com.whip.app.reminders.TimerAlarmKind
import com.whip.app.reminders.TimerAlarmPayload
import com.whip.app.reminders.TimerAlarmReceiver
import com.whip.app.reminders.ACTION_EXACT_ALARM_ACCESS_CHANGED
import com.whip.app.reminders.canScheduleExactReminderAlarms
import com.whip.app.startup.StartupRecoveryState
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.regex.Pattern
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
        val complete = app.getSystemService(NotificationManager::class.java).activeNotifications
            .single { it.id == FocusTimerNotifications.notificationId }.notification
        assertEquals("Focus session complete", complete.extras.getString(Notification.EXTRA_TITLE))
        assertFalse(complete.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertEquals(0, complete.flags and Notification.FLAG_ONGOING_EVENT)
        assertEquals(0, complete.flags and Notification.FLAG_ONLY_ALERT_ONCE)
        assertEquals(Notification.GROUP_ALERT_ALL, complete.groupAlertBehavior)
        assertNull(complete.group)
        assertTrue(complete.actions.isNullOrEmpty())
        assertNull(payload.pendingIntent(app, create = false))
    }

    @Test fun confirmedFocusStartPublishesNativeCountdownAndStopActionClearsIt() = runBlocking {
        val taskId = app.taskRepository.create(TaskDraft(title = "Native focus countdown"))
        val receipt = app.focusTimerScheduler.start(taskId, 30)
        assertEquals(30, receipt.minutes)
        assertEquals("Native focus countdown", receipt.taskTitle)
        assertEquals(receipt.deadlineMillis, app.settingsRepository.current().focusTimerDeadlineMillis)
        val manager = app.getSystemService(NotificationManager::class.java)
        val running = manager.activeNotifications.single { it.id == FocusTimerNotifications.notificationId }.notification
        assertEquals(receipt.deadlineMillis, running.`when`)
        assertTrue(running.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(running.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
        assertTrue(running.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertTrue(running.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0)
        assertTrue(running.timeoutAfter in 1L..30 * 60_000L)
        assertEquals(listOf("Open", "Stop"), running.actions.map { it.title.toString() })
        val receiver = app.packageManager.getReceiverInfo(ComponentName(app, FocusTimerStopReceiver::class.java), 0)
        assertFalse(receiver.exported)
        running.actions.single { it.title == "Stop" }.actionIntent.send()
        withTimeout(5_000L) {
            while (app.settingsRepository.current().focusTimerTaskId != null ||
                manager.activeNotifications.any { it.id == FocusTimerNotifications.notificationId }
            ) delay(20L)
        }
    }

    @Test fun focusReplacementRequiresConsentAndOldStopCannotStopReplacement() = runBlocking {
        val taskId = app.taskRepository.create(TaskDraft(title = "Original focus"))
        val otherId = app.taskRepository.create(TaskDraft(title = "Replacement focus"))
        val original = app.focusTimerScheduler.start(taskId, 15)
        val originalNotification = app.getSystemService(NotificationManager::class.java).activeNotifications
            .single { it.id == FocusTimerNotifications.notificationId }.notification
        assertTrue(runCatching { app.focusTimerScheduler.start(otherId, 45) }.isFailure)
        assertEquals(original.deadlineMillis, app.settingsRepository.current().focusTimerDeadlineMillis)
        val replacement = app.focusTimerScheduler.start(otherId, 45, taskId, original.deadlineMillis)
        originalNotification.actions.single { it.title == "Stop" }.actionIntent.send()
        // Await the same operation boundary after dispatch; also directly exercise the durable stale guard.
        assertFalse(app.focusTimerScheduler.stopIfCurrent(taskId, original.deadlineMillis, app.currentUserDataGeneration()))
        app.focusTimerScheduler.cancel()
        app.focusTimerScheduler.refreshNotification()
        assertEquals(replacement.deadlineMillis, app.settingsRepository.current().focusTimerDeadlineMillis)
        assertTrue(app.getSystemService(NotificationManager::class.java).activeNotifications
            .any { it.notification.extras.getString(Notification.EXTRA_TITLE) == "Focusing on Replacement focus" })
        assertFalse(app.focusTimerScheduler.stopIfCurrent(otherId, replacement.deadlineMillis,
            app.currentUserDataGeneration() + 1L))
        assertTrue(app.focusTimerScheduler.stopIfCurrent(otherId, replacement.deadlineMillis))
    }

    @Test fun focusStartRejectsInvalidDurationMissingAndArchivedTask() = runBlocking {
        val taskId = app.taskRepository.create(TaskDraft(title = "Focus validation"))
        for (minutes in listOf(0, 241, Int.MAX_VALUE)) {
            assertTrue(runCatching { app.focusTimerScheduler.start(taskId, minutes) }.isFailure)
        }
        assertTrue(runCatching { app.focusTimerScheduler.start(Long.MAX_VALUE, 15) }.isFailure)
        app.taskRepository.archive(taskId)
        assertTrue(runCatching { app.focusTimerScheduler.start(taskId, 15) }.isFailure)
        assertNull(app.settingsRepository.current().focusTimerTaskId)
    }

    @Test fun focusNotificationRefreshRestoresPresentationWithoutReplacingWork() = runBlocking {
        val taskId = app.taskRepository.create(TaskDraft(title = "Restored countdown"))
        val started = app.focusTimerScheduler.start(taskId, 60)
        val workManager = WorkManager.getInstance(app)
        val work = workManager.getWorkInfosForUniqueWork("whip-focus-timer").get()
            .single { it.state == WorkInfo.State.ENQUEUED }.id
        val manager = app.getSystemService(NotificationManager::class.java)
        manager.cancel(FocusTimerNotifications.notificationId)
        app.focusTimerScheduler.refreshNotification()
        assertEquals(work, workManager.getWorkInfosForUniqueWork("whip-focus-timer").get()
            .single { it.state == WorkInfo.State.ENQUEUED }.id)
        assertEquals(started.deadlineMillis, manager.activeNotifications
            .single { it.id == FocusTimerNotifications.notificationId }.notification.`when`)
        app.rebuildBackgroundState()
        assertTrue(manager.activeNotifications.single { it.id == FocusTimerNotifications.notificationId }
            .notification.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
    }

    @Test fun blockedFocusChannelKeepsTheConfirmedTimerAndRefreshesAfterAndroidAllowsIt() = runBlocking {
        val manager = app.getSystemService(NotificationManager::class.java)
        val taskId = app.taskRepository.create(TaskDraft(title = "Focus without drawer access"))
        val originallyEnabled = manager.getNotificationChannel(FocusTimerNotifications.channelId).importance != NotificationManager.IMPORTANCE_NONE
        try {
            setFocusChannelEnabled(false)
            assertTrue(manager.areNotificationsEnabled())
            val receipt = app.focusTimerScheduler.start(taskId, 15)
            assertEquals(receipt.deadlineMillis, app.settingsRepository.current().focusTimerDeadlineMillis)
            assertTrue(receipt.warnings.any { it.contains("notifications") })
            assertTrue(manager.activeNotifications.none { it.id == FocusTimerNotifications.notificationId })
            val workManager = WorkManager.getInstance(app)
            val work = workManager.getWorkInfosForUniqueWork("whip-focus-timer").get()
                .single { it.state == WorkInfo.State.ENQUEUED }.id
            setFocusChannelEnabled(true)
            app.focusTimerScheduler.refreshNotification()
            assertEquals(work, workManager.getWorkInfosForUniqueWork("whip-focus-timer").get()
                .single { it.state == WorkInfo.State.ENQUEUED }.id)
            assertTrue(manager.activeNotifications.any { it.id == FocusTimerNotifications.notificationId })
        } finally {
            setFocusChannelEnabled(originallyEnabled)
        }
    }

    @Test fun focusCompletionEventRequiresMatchingDurableClearEvenWhenAndroidBlocksTheFocusChannel() = runBlocking {
        val taskId = app.taskRepository.create(TaskDraft(title = "Foreground completion"))
        val deadline = System.currentTimeMillis() - 1_000L
        val generation = app.currentUserDataGeneration()
        val events = mutableListOf<FocusTimerCompletionReceipt>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            app.focusTimerScheduler.completions.collect { events += it }
        }
        suspend fun deliver(expectedDeadline: Long, presentedGeneration: Long = generation) {
            val input = Data.Builder().putLong(USER_DATA_GENERATION_KEY, presentedGeneration)
                .putLong(FocusTimerWorker.taskIdKey, taskId)
                .putLong(FocusTimerWorker.deadlineKey, expectedDeadline).build()
            assertEquals(ListenableWorker.Result.success(),
                TestListenableWorkerBuilder<FocusTimerWorker>(app).setInputData(input).build().doWork())
            yield()
        }
        val manager = app.getSystemService(NotificationManager::class.java)
        val originallyEnabled = manager.getNotificationChannel(FocusTimerNotifications.channelId).importance != NotificationManager.IMPORTANCE_NONE
        try {
            setFocusChannelEnabled(false)
            assertTrue(manager.areNotificationsEnabled())
            app.settingsRepository.updateAndConfirm {
                it.copy(focusTimerTaskId = taskId, focusTimerDeadlineMillis = deadline)
            }
            deliver(deadline - 1L)
            deliver(deadline, generation + 1L)
            assertTrue(events.isEmpty())
            deliver(deadline)
            assertEquals(listOf(FocusTimerCompletionReceipt(taskId, "Foreground completion", deadline)), events)
            assertNull(app.settingsRepository.current().focusTimerDeadlineMillis)
            deliver(deadline)
            assertEquals(1, events.size)
            assertTrue(app.focusTimerScheduler.completions.replayCache.isEmpty())
            assertTrue(app.getSystemService(NotificationManager::class.java).activeNotifications
                .none { it.id == FocusTimerNotifications.notificationId })
        } finally {
            collector.cancel()
            setFocusChannelEnabled(originallyEnabled)
        }
    }

    private suspend fun setFocusChannelEnabled(enabled: Boolean) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        app.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, app.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, FocusTimerNotifications.channelId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        assertTrue(device.wait(Until.hasObject(By.pkg("com.android.settings")), 5_000L))
        val switch = checkNotNull(device.wait(Until.findObject(By.clazz("android.widget.Switch")), 5_000L)) {
            "Android Focus settings did not expose its notification switch"
        }
        if (switch.isChecked != enabled) switch.click()
        val manager = app.getSystemService(NotificationManager::class.java)
        withTimeout(5_000L) {
            while ((manager.getNotificationChannel(FocusTimerNotifications.channelId).importance != NotificationManager.IMPORTANCE_NONE) != enabled) {
                delay(20L)
            }
        }
        device.pressBack()
    }

    @Test fun focusCountdownTicksInAndroidDrawerAndVisibleStopClearsIt() = runBlocking {
        val taskId = app.taskRepository.create(TaskDraft(title = "Drawer countdown evidence"))
        app.focusTimerScheduler.start(taskId, 2)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        try {
            device.openNotification()
            assertNotNull(device.wait(Until.findObject(By.text("Focusing on Drawer countdown evidence")), 5_000L))
            val chronometerSelector = By.res("android", "chronometer")
            val clock = requireNotNull(device.wait(Until.findObject(chronometerSelector), 5_000L)) {
                "Android drawer did not show the native focus chronometer"
            }
            val initial = clock.text
            assertFalse(initial.isNullOrBlank())
            withTimeout(5_000L) {
                while (device.findObject(chronometerSelector)?.text == initial) delay(100L)
            }
            assertNotNull(device.findObject(chronometerSelector))
            val open = device.wait(Until.findObject(By.text(Pattern.compile("Open", Pattern.CASE_INSENSITIVE))), 5_000L)
            val stop = device.wait(Until.findObject(By.text(Pattern.compile("Stop", Pattern.CASE_INSENSITIVE))), 5_000L)
            assertNotNull(open)
            assertNotNull(stop)
            val evidence = File(requireNotNull(app.getExternalFilesDir(null)), "focus-timer-evidence")
            check(evidence.isDirectory || evidence.mkdirs())
            val screenshot = File(evidence, "focus-countdown-drawer.png")
            val hierarchy = File(evidence, "focus-countdown-drawer.xml")
            assertTrue(device.takeScreenshot(screenshot))
            device.dumpWindowHierarchy(hierarchy)
            publishDrawerEvidence(screenshot, "image/png")
            publishDrawerEvidence(hierarchy, "text/xml")
            requireNotNull(stop).click()
            withTimeout(5_000L) {
                while (app.settingsRepository.current().focusTimerTaskId != null ||
                    app.getSystemService(NotificationManager::class.java).activeNotifications
                        .any { it.id == FocusTimerNotifications.notificationId }
                ) delay(20L)
            }
        } finally {
            device.pressBack()
        }
    }

    private fun publishDrawerEvidence(file: File, mimeType: String) {
        // Older supported Android keeps the original external-files capture; the acceptance
        // emulator's scoped Downloads copy survives instrumentation uninstall cleanup.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val resolver = app.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/whip-ui-catalog/")
        }
        val uri = checkNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        checkNotNull(resolver.openOutputStream(uri, "w")).use { output ->
            file.inputStream().use { input -> input.copyTo(output) }
        }
    }

    @Test fun cancellationQueuedForClearedTimerCannotCancelNewerPairOrWork() = runBlocking {
        val taskId = app.taskRepository.create(TaskDraft(title = "Queued cancellation"))
        val original = app.focusTimerScheduler.start(taskId, 15)
        val newerDeadline = original.deadlineMillis + 60_000L
        app.focusTimerScheduler.withDeliveryBoundary {
            app.settingsRepository.updateAndConfirm { it.copy(focusTimerTaskId = null, focusTimerDeadlineMillis = null) }
            app.focusTimerScheduler.cancel()
            app.settingsRepository.updateAndConfirm {
                it.copy(focusTimerTaskId = taskId, focusTimerDeadlineMillis = newerDeadline)
            }
        }
        app.focusTimerScheduler.schedule(taskId, newerDeadline)
        assertEquals(newerDeadline, app.settingsRepository.current().focusTimerDeadlineMillis)
        assertTrue(WorkManager.getInstance(app).getWorkInfosForUniqueWork("whip-focus-timer").get()
            .any { it.state == WorkInfo.State.ENQUEUED })
        assertEquals(newerDeadline, app.getSystemService(NotificationManager::class.java).activeNotifications
            .single { it.id == FocusTimerNotifications.notificationId }.notification.`when`)
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
