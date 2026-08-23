package com.whip.app

import android.app.ActivityOptions
import android.content.Intent
import android.view.Display
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class WhipNavigationTest {
    @Test
    fun primaryAreasAndAccessibleTopActionsAreReachable() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<WhipApplication>()
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { it.copy(setupCompleted = true) }
        }
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java,
        ).putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        val options = ActivityOptions.makeBasic()
            .setLaunchDisplayId(Display.DEFAULT_DISPLAY)
            .toBundle()

        // The explicit display is required on foldable devices that expose more than one display.
        ActivityScenario.launch<MainActivity>(intent, options).use {
            device.waitForIdle()
            assertVisible(device, By.text("Home"))
            assertVisible(device, By.text("Review & Trends"))

            assertVisible(device, By.desc("Search All Whip Data"))
            device.wait(Until.findObject(By.desc("Search All Whip Data")), TIMEOUT_MS).click()
            assertVisible(device, By.text("Search Whip"))
            device.wait(Until.findObject(By.text("Close")), TIMEOUT_MS).click()
            device.wait(Until.findObject(By.desc("Tasks tab")), TIMEOUT_MS).click()
            device.wait(Until.findObject(By.text("Upcoming")), TIMEOUT_MS).click()
            assertVisible(device, By.textContains("The next 30 days"))

            device.wait(Until.findObject(By.desc("Habits tab")), TIMEOUT_MS).click()
            assertVisible(device, By.text("Check in, log a value, or continue a timer for habits due today."))

            device.wait(Until.findObject(By.desc("Gym tab")), TIMEOUT_MS).click()
            assertVisible(device, By.text("Workout"))

            device.wait(Until.findObject(By.desc("Goals tab")), TIMEOUT_MS).click()
            assertVisible(
                device,
                By.text("Long-term measurements, consistency, ranges, totals, and project milestones."),
            )
        }
    }

    private fun assertVisible(device: UiDevice, selector: androidx.test.uiautomator.BySelector) {
        assertNotNull(device.wait(Until.findObject(selector), TIMEOUT_MS))
    }

    private companion object {
        const val SHORT_TIMEOUT_MS = 1_000L
        const val TIMEOUT_MS = 5_000L
    }
}
