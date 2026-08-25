package com.whip.app

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.whip.app.reminders.ReminderNotifications
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ZPlatformNotificationSurfaceE2ETest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var app: WhipApplication
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Before
    fun prepare() = runBlocking {
        app = ApplicationProvider.getApplicationContext()
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { it.copy(setupCompleted = true) }
        app.getSystemService(NotificationManager::class.java).apply {
            cancelAll()
            deleteNotificationChannel(ReminderNotifications.CHANNEL_ID)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            instrumentation.uiAutomation.grantRuntimePermission(app.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @Test
    fun settingsTestNotificationIsPostedAndVisibleInAndroidsNotificationShade() {
        ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java)).use {
            if (compose.onAllNodesWithContentDescription("Open Settings").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithContentDescription("Open Settings").performClick()
            } else {
                compose.onNodeWithContentDescription("App actions").performClick()
                compose.onNodeWithText("Open Settings").performClick()
            }
            compose.onNodeWithTag("settings-section-Reminders").performClick()
            compose.onNodeWithTag("settings-list").performScrollToNode(hasTestTag("send-test-notification"))
            compose.onNodeWithTag("send-test-notification").performClick()
            compose.onNodeWithText("Test sent. Check the notification shade.").assertIsDisplayed()

            val manager = app.getSystemService(NotificationManager::class.java)
            compose.waitUntil(5_000) {
                manager.activeNotifications.any { notification ->
                    notification.notification.extras.getCharSequence("android.title") == "Whip notifications are working"
                }
            }

            val device = UiDevice.getInstance(instrumentation)
            device.openNotification()
            assertTrue(
                "Android's notification shade did not present Whip's test reminder",
                device.wait(Until.hasObject(By.text("Whip notifications are working")), 5_000),
            )
            device.pressBack()
            manager.cancelAll()
        }
    }
}
