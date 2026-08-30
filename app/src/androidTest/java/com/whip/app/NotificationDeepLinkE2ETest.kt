package com.whip.app

import android.content.Intent
import android.os.SystemClock
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.whip.app.core.WhipLaunchActions
import com.whip.app.domain.HabitDraft
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationDeepLinkE2ETest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var app: WhipApplication
    private var habitId: Long = 0

    @Before
    fun prepare() = runBlocking {
        app = ApplicationProvider.getApplicationContext()
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { it.copy(setupCompleted = true) }
        habitId = app.habitRepository.create(HabitDraft(name = "Deep link habit", startDate = app.clock.today()))
    }

    @Test
    fun identicalNewIntentRoutesAgainWithoutRecompositionReplay() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        val intent = openHabitIntent()
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        launchMainActivity(intent).use {
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("habit-detail-surface").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("Close Habit details").performClick()
            compose.onNodeWithContentDescription("Go to Home").performClick()

            ApplicationProvider.getApplicationContext<WhipApplication>().startActivity(openHabitIntent())

            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("habit-detail-surface").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("habit-detail-surface").assertIsDisplayed()
            compose.onNodeWithContentDescription("More Habit options").performClick()
            compose.onNodeWithText("Options").performClick()
            compose.onNodeWithTag("entity-inspector-content-options").assertIsDisplayed()
        }
    }

    @Test
    fun consumedSecondDeliveryDoesNotReplayAfterActivityRecreation() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        val intent = openHabitIntent()
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        launchMainActivity(intent).use { scenario ->
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("habit-detail-surface").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("Close Habit details").performClick()
            compose.onNodeWithContentDescription("Go to Home").performClick()

            ApplicationProvider.getApplicationContext<WhipApplication>().startActivity(openHabitIntent())
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("habit-detail-surface").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("Close Habit details").performClick()
            compose.onNodeWithContentDescription("Go to Home").performClick()

            scenario.recreate()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("home-list").fetchSemanticsNodes().isNotEmpty()
            }
            val replayWindowEnd = SystemClock.uptimeMillis() + 1_000L
            compose.waitUntil(5_000) { SystemClock.uptimeMillis() >= replayWindowEnd }
            compose.onAllNodesWithTag("habit-detail-surface").assertCountEquals(0)
            compose.onNodeWithTag("home-list").assertIsDisplayed()
        }
    }

    private fun openHabitIntent() = Intent(app, MainActivity::class.java)
        .setAction(WhipLaunchActions.ACTION_OPEN_HABIT)
        .putExtra(WhipLaunchActions.EXTRA_ENTITY_ID, habitId)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
}
