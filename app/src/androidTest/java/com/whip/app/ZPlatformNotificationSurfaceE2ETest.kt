package com.whip.app

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
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
import org.junit.After
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

    @After
    fun clearPostedNotifications() {
        app.getSystemService(NotificationManager::class.java).cancelAll()
        // Revoking a runtime permission kills the package process. Instrumentation
        // runs inside that process, so attempting to restore the permission here
        // destroys the runner before it can report the result. This suite is gated
        // to a disposable emulator and the install lifecycle resets app permissions.
    }

    @Test
    fun settingsTestNotificationIsPostedAndVisibleInAndroidsNotificationShade() {
        ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java)).use {
            compose.onNodeWithTag("workspace-settings-action").performClick()
            if (compose.onAllNodesWithTag("settings-support-list").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithTag("settings-support-list")
                    .performScrollToNode(hasTestTag("settings-support-section-Reminders"))
                compose.onNodeWithTag("settings-support-section-Reminders").performClick()
            } else {
                if (compose.onAllNodesWithTag("settings-category-list").fetchSemanticsNodes().isNotEmpty()) {
                    compose.onNodeWithTag("settings-category-list")
                        .performScrollToNode(hasTestTag("settings-section-Reminders"))
                }
                compose.onNodeWithTag("settings-section-Reminders").performClick()
            }
            compose.onNodeWithTag("settings-list").performScrollToNode(hasTestTag("send-test-notification"))
            compose.onNodeWithTag("send-test-notification").performClick()
            compose.onNodeWithText("Test sent. Check the notification shade.").assertIsDisplayed()

            val manager = app.getSystemService(NotificationManager::class.java)
            compose.waitUntil(5_000) {
                manager.activeNotifications.any { notification ->
                    notification.notification.extras.getCharSequence("android.title") == "Whip notifications are working"
                }
            }

            // The API 26 Google APIs image's SystemUI process crashes when its
            // notification shade is expanded under headless SwiftShader. The
            // posted platform notification is still asserted above; exercise
            // presentation in the stable API 28+ SystemUI implementation.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val device = UiDevice.getInstance(instrumentation)
                device.openNotification()
                assertTrue(
                    "Android's notification shade did not present Whip's test reminder",
                    device.wait(Until.hasObject(By.text("Whip notifications are working")), 5_000),
                )
                device.pressBack()
            }
            manager.cancelAll()
        }
    }
}
