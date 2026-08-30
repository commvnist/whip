package com.whip.app

import android.content.Intent
import android.os.SystemClock
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.whip.app.core.AppSettings
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImeNavigationRailE2ETest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private var originalSizeOverride: String? = null
    private var originalDensityOverride: String? = null

    @Before
    fun prepareWideDisposableDisplay() = runBlocking {
        val sizeState = device.executeShellCommand("wm size")
        val densityState = device.executeShellCommand("wm density")
        originalSizeOverride = Regex("Override size: ([0-9]+x[0-9]+)")
            .find(sizeState)?.groupValues?.get(1)
        originalDensityOverride = Regex("Override density: ([0-9]+)")
            .find(densityState)?.groupValues?.get(1)

        device.executeShellCommand("wm size 1800x1200")
        device.executeShellCommand("wm density 240")
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")

        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { AppSettings(setupCompleted = true) }
    }

    @After
    fun restoreDisposableDisplay() {
        device.executeShellCommand(
            originalSizeOverride?.let { "wm size $it" } ?: "wm size reset",
        )
        device.executeShellCommand(
            originalDensityOverride?.let { "wm density $it" } ?: "wm density reset",
        )
    }

    @Test
    fun openingTheImeDoesNotMovePersistentNavigationOnAWideLayout() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        ActivityScenario.launch<MainActivity>(intent).use {
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("adaptive-navigation-rail-destinations")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            val before = compose.onNodeWithTag("adaptive-navigation-rail-destinations")
                .fetchSemanticsNode().boundsInRoot

            compose.onNodeWithContentDescription("Add task, habit, goal, track, or workout")
                .performClick()
            compose.onNodeWithText("New Task").performClick()
            compose.onNodeWithTag("task-editor-title").performClick().performTextInput("Stable rail")

            repeat(20) {
                if (device.executeShellCommand("dumpsys input_method").contains("mInputShown=true")) {
                    return@repeat
                }
                SystemClock.sleep(100)
            }
            assertTrue(
                "The keyboard never became visibly active, so this cannot prove the IME regression",
                device.executeShellCommand("dumpsys input_method").contains("mInputShown=true"),
            )

            val after = compose.onNodeWithTag("adaptive-navigation-rail-destinations")
                .fetchSemanticsNode().boundsInRoot
            assertSameBounds(before, after)
        }
    }

    private fun assertSameBounds(before: Rect, after: Rect) {
        val tolerancePx = 1f
        assertTrue("Navigation moved horizontally when the IME opened: $before -> $after", abs(before.left - after.left) <= tolerancePx)
        assertTrue("Navigation moved vertically when the IME opened: $before -> $after", abs(before.top - after.top) <= tolerancePx)
        assertTrue("Navigation width changed when the IME opened: $before -> $after", abs(before.right - after.right) <= tolerancePx)
        assertTrue("Navigation height changed when the IME opened: $before -> $after", abs(before.bottom - after.bottom) <= tolerancePx)
    }
}
