package com.whip.app

import android.app.ActivityOptions
import android.content.Intent
import android.view.Display
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.whip.app.domain.HabitDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitSkipJourneyE2ETest {
    @get:Rule val compose = createEmptyComposeRule()

    private val app: WhipApplication
        get() = ApplicationProvider.getApplicationContext()

    private var habitId = -1L

    @Before
    fun setUp() = runBlocking {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update {
            it.copy(
                setupCompleted = true,
                powerMode = false,
                hiddenHomeSections = emptySet(),
                collapsedHomeSections = emptySet(),
            )
        }
        habitId = app.habitRepository.create(HabitDraft(name = "Read Today", startDate = app.clock.today()))
    }

    @After
    fun cleanUp() = runBlocking { app.backupRepository.deleteAllData() }

    @Test
    fun skipIsOneVisibleNeutralStateWithHistoryInsightsAndUndo() {
        val intent = Intent(app, MainActivity::class.java).putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(Display.DEFAULT_DISPLAY).toBundle()
        ActivityScenario.launch<MainActivity>(intent, options).use {
            compose.onNodeWithContentDescription("Habits tab").performClick()
            compose.onNodeWithTag("habit-card-$habitId").assertIsDisplayed()
            compose.onNodeWithContentDescription("Open habit details for Read Today")
                .performSemanticsAction(SemanticsActions.OnClick)

            compose.onAllNodesWithText("Excuse Today").assertCountEquals(0)
            compose.onAllNodesWithText("Mark Today Missing").assertCountEquals(0)
            compose.onNodeWithText("Skip Today").performClick()
            compose.onNodeWithText("Skip Today?").assertIsDisplayed()
            compose.onNodeWithText("Skip Today").performClick()

            runBlocking {
                withTimeout(5_000) { app.habitRepository.skips.first { rows -> rows.any { it.habitId == habitId } } }
                check(app.habitRepository.logs.first().none { it.habitId == habitId })
                check(app.measurementRepository.entries.first().none { it.sourceId?.contains("$habitId") == true })
            }
            compose.onNodeWithText("Skipped Today · Streak Protected").assertIsDisplayed()

            compose.onNodeWithContentDescription("Open habit details for Read Today")
                .performSemanticsAction(SemanticsActions.OnClick)
            compose.onNodeWithTag("habit-detail-section-History").performClick()
            compose.onNodeWithText("${app.clock.today()}: Skipped").assertIsDisplayed()
            compose.onNodeWithText("Close").performClick()

            compose.onNodeWithTag("habit-destination-Insights").performClick()
            compose.onNodeWithText("Last 30 Days: 0 Completed · 1 Skipped · 0 Missed/Below Target").assertIsDisplayed()
            compose.onNodeWithTag("habit-destination-Today").performClick()
            compose.onNodeWithText("Undo Skip").performClick()

            runBlocking { withTimeout(5_000) { app.habitRepository.skips.first { it.isEmpty() } } }
            compose.onAllNodesWithText("Skipped Today · Streak Protected").assertCountEquals(0)
        }
    }

    @Test
    fun completedHabitMovesBelowRemainingHabitsAndStaysAvailableForUndo() {
        val today = app.clock.today()
        val pendingHabitId = runBlocking {
            app.habitRepository.setCheckOff(habitId, today, true)
            app.habitRepository.create(HabitDraft(name = "Drink Water", startDate = today))
        }
        val intent = Intent(app, MainActivity::class.java).putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(Display.DEFAULT_DISPLAY).toBundle()

        ActivityScenario.launch<MainActivity>(intent, options).use {
            compose.onNodeWithContentDescription("Habits tab").performClick()
            compose.onNodeWithTag("habit-card-$pendingHabitId").assertIsDisplayed()
            compose.onNodeWithTag("habit-done-disclosure").assertIsDisplayed()
            compose.onAllNodesWithTag("habit-card-$habitId").assertCountEquals(0)

            compose.onNodeWithTag("habit-done-disclosure").performSemanticsAction(SemanticsActions.OnClick)
            compose.onNodeWithTag("habit-card-$habitId").assertIsDisplayed()
            val pendingBounds = compose.onNodeWithTag("habit-card-$pendingHabitId").getUnclippedBoundsInRoot()
            val doneBounds = compose.onNodeWithTag("habit-card-$habitId").getUnclippedBoundsInRoot()
            assertTrue("Done habits must follow habits that still need attention", pendingBounds.top < doneBounds.top)

            compose.onNodeWithContentDescription("Mark habit Read Today incomplete").performClick()
            runBlocking {
                withTimeout(5_000) {
                    app.habitRepository.logs.first { logs -> logs.none { log -> log.habitId == habitId } }
                }
            }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag("habit-done-disclosure").fetchSemanticsNodes().isEmpty()
            }
            compose.onNodeWithTag("habit-card-$habitId").assertIsDisplayed()

            compose.onNodeWithContentDescription("Check off habit Read Today").performClick()
            runBlocking {
                withTimeout(5_000) {
                    app.habitRepository.logs.first { logs -> logs.any { log -> log.habitId == habitId } }
                }
            }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag("habit-card-$habitId").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("habit-done-disclosure").assertIsDisplayed()
            compose.onNodeWithTag("habit-card-$habitId").assertIsDisplayed()

            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
            compose.onNodeWithTag("home-list").performScrollToNode(hasTestTag("habit-done-disclosure"))
            compose.onNodeWithTag("habit-done-disclosure").assertIsDisplayed()
            assertEquals(
                "Collapsed",
                compose.onNodeWithTag("habit-done-disclosure").fetchSemanticsNode()
                    .config[SemanticsProperties.StateDescription],
            )
            compose.onNodeWithTag("habit-done-disclosure").performSemanticsAction(SemanticsActions.OnClick)
            compose.waitForIdle()
            assertEquals(
                "Expanded",
                compose.onNodeWithTag("habit-done-disclosure").fetchSemanticsNode()
                    .config[SemanticsProperties.StateDescription],
            )
            compose.onNodeWithTag("home-list").performScrollToNode(hasTestTag("habit-card-$habitId"))
            compose.onNodeWithTag("habit-card-$habitId").assertIsDisplayed()
        }
    }
}
