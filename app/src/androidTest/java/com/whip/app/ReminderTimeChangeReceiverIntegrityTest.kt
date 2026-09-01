package com.whip.app

import android.content.ComponentName
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.reminders.ACTION_DEVICE_TIME_CHANGED
import com.whip.app.reminders.ACTION_DEVICE_TIME_ZONE_CHANGED
import com.whip.app.reminders.ReminderTimeChangeReceiver
import com.whip.app.reminders.ReminderTimeInvalidationPlan
import com.whip.app.reminders.routeReminderTimeInvalidation
import com.whip.app.startup.StartupRecoveryState
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderTimeChangeReceiverIntegrityTest {
    @Test
    fun manifestReceiverRoutesFixedAndFollowDeviceTimeZonesWithoutChangingUserData() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        awaitReady(app)
        val receiverInfo = app.packageManager.getReceiverInfo(
            ComponentName(app, ReminderTimeChangeReceiver::class.java),
            0,
        )
        assertFalse(receiverInfo.exported)
        assertNotNull(receiverInfo)

        val originalSettings = app.settingsRepository.current()
        val before = userDataPayload(app.backupRepository.exportBackup())
        try {
            app.settingsRepository.update { it.copy(timeZoneId = "UTC") }
            assertEquals(
                ReminderTimeInvalidationPlan(false, false),
                routeReminderTimeInvalidation(app, ACTION_DEVICE_TIME_ZONE_CHANGED),
            )
            assertEquals(
                ReminderTimeInvalidationPlan(true, true),
                routeReminderTimeInvalidation(app, ACTION_DEVICE_TIME_CHANGED),
            )

            app.settingsRepository.update { it.copy(timeZoneId = null) }
            assertEquals(
                ReminderTimeInvalidationPlan(true, true),
                routeReminderTimeInvalidation(app, ACTION_DEVICE_TIME_ZONE_CHANGED),
            )
        } finally {
            app.settingsRepository.update { originalSettings }
            // Restore authoritative queue timing for whichever zone the test
            // process used before this test.
            routeReminderTimeInvalidation(app, ACTION_DEVICE_TIME_CHANGED)
        }

        assertEquals(originalSettings, app.settingsRepository.current())
        assertEquals(before, userDataPayload(app.backupRepository.exportBackup()))
    }

    @Test
    fun receiverRoutingFailsClosedWhileRestoreRecoveryIsBlocked() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        awaitReady(app)
        val marker = File(app.noBackupFilesDir, "restore-recovery.whip.json")
        check(!marker.exists()) { "A real pending recovery must never be overwritten by a test" }

        try {
            marker.writeText("intentionally corrupt recovery snapshot")
            app.blockForPendingRecovery()

            assertNull(routeReminderTimeInvalidation(app, ACTION_DEVICE_TIME_CHANGED))
        } finally {
            marker.delete()
            app.retryStartupRecovery()
            awaitReady(app)
        }
    }

    private suspend fun awaitReady(app: WhipApplication) = withTimeout(10_000) {
        while (app.startupRecoveryState.value != StartupRecoveryState.Ready) delay(20)
    }

    private fun userDataPayload(json: String): String = JSONObject(json).apply {
        remove("exportedAt")
        remove("checksumSha256")
        // Settings set serialization order is not stable after a round-trip;
        // settings equality is asserted separately above.
        remove("settings")
    }.toString()
}
