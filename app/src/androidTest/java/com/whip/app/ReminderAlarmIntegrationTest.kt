package com.whip.app

import android.content.ComponentName
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import com.whip.app.reminders.ReminderAlarmPayload
import com.whip.app.reminders.ReminderAlarmReceiver
import com.whip.app.reminders.ReminderDeliveryClaim
import com.whip.app.reminders.ReminderDeliveryKind
import com.whip.app.reminders.ReminderDomain
import com.whip.app.reminders.canScheduleExactReminderAlarms
import com.whip.app.startup.StartupRecoveryState
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderAlarmIntegrationTest {
    @Test
    fun scheduledTransportWakesValidatedWorkWithoutAnActivityLaunch() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        awaitReady(app)
        val exactAccessAllowed = canScheduleExactReminderAlarms(app)
        val entityId = 9_151_002L
        val trigger = System.currentTimeMillis() + 2_000L
        val workName = "whip-reminder-$entityId-automatic-alarm-integration"
        val payload = testTaskPayload(app, entityId, trigger, workName)
        val workManager = WorkManager.getInstance(app)

        try {
            app.reminderAlarmScheduler.enqueue(payload)
            if (!exactAccessAllowed) {
                assertNull(payload.pendingIntent(app, create = false))
                return@runBlocking
            }
            assertNotNull(payload.pendingIntent(app, create = false))

            withTimeout(15_000L) {
                while (
                    workManager.getWorkInfosForUniqueWork(workName).get()
                        .none { it.state == WorkInfo.State.SUCCEEDED }
                ) {
                    delay(25L)
                }
            }

            // With exact access the WorkManager fallback is two minutes away,
            // so completion inside this bound proves AlarmManager woke the
            // private receiver without MainActivity being opened.
            assertNull(payload.pendingIntent(app, create = false))
        } finally {
            app.reminderAlarmScheduler.cancelEntity(ReminderDomain.Task, entityId)
            workManager.cancelUniqueWork(workName).await()
        }
    }

    @Test
    fun exactAlarmRegistersAndPromotesTheSameValidatedWork() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        awaitReady(app)
        val exactAccessAllowed = canScheduleExactReminderAlarms(app)

        val receiverInfo = app.packageManager.getReceiverInfo(
            ComponentName(app, ReminderAlarmReceiver::class.java),
            0,
        )
        assertFalse(receiverInfo.exported)

        val entityId = 9_151_001L
        val trigger = System.currentTimeMillis() + 5 * 60_000L
        val workName = "whip-reminder-$entityId-alarm-integration"
        val payload = testTaskPayload(app, entityId, trigger, workName)
        val workManager = WorkManager.getInstance(app)

        try {
            app.reminderAlarmScheduler.enqueue(payload)
            val exactPendingIntent = payload.pendingIntent(app, create = false)
            if (exactAccessAllowed) assertNotNull(exactPendingIntent) else assertNull(exactPendingIntent)
            assertTrue(
                workManager.getWorkInfosForUniqueWork(workName).get()
                    .any { it.state == WorkInfo.State.ENQUEUED },
            )

            if (exactPendingIntent != null) {
                exactPendingIntent.send()
            } else {
                app.reminderAlarmScheduler.promote(payload)
            }
            withTimeout(10_000L) {
                while (
                    workManager.getWorkInfosForUniqueWork(workName).get()
                        .none { it.state == WorkInfo.State.SUCCEEDED }
                ) {
                    delay(25L)
                }
            }
            assertNull(payload.pendingIntent(app, create = false))
        } finally {
            app.reminderAlarmScheduler.cancelEntity(ReminderDomain.Task, entityId)
            workManager.cancelUniqueWork(workName).await()
        }
    }

    private suspend fun awaitReady(app: WhipApplication) = withTimeout(10_000L) {
        while (app.startupRecoveryState.value != StartupRecoveryState.Ready) delay(20L)
    }

    private fun testTaskPayload(
        app: WhipApplication,
        entityId: Long,
        trigger: Long,
        workName: String,
    ) = ReminderAlarmPayload(
        domain = ReminderDomain.Task,
        entityId = entityId,
        workName = workName,
        claim = ReminderDeliveryClaim(
            kind = ReminderDeliveryKind.Scheduled,
            stableEntityId = "alarm-integration-stable-id-$entityId",
            logicalEpochDay = LocalDate.now().toEpochDay(),
            expectedTriggerAtMillis = trigger,
            definitionFingerprint = "alarm-integration-fingerprint",
        ),
        userDataGeneration = app.currentUserDataGeneration(),
        offsetMinutes = 0,
    )
}
