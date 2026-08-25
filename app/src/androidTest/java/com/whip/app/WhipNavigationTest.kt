package com.whip.app

import android.app.ActivityOptions
import android.content.Intent
import android.view.Display
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalType
import com.whip.app.core.ReviewPeriod

@RunWith(AndroidJUnit4::class)
class WhipNavigationTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun primaryAreasAndAccessibleTopActionsAreReachable() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<WhipApplication>()
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { it.copy(setupCompleted = true) }
        }
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java,
        ).putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        val options = ActivityOptions.makeBasic()
            .setLaunchDisplayId(Display.DEFAULT_DISPLAY)
            .toBundle()

        // The explicit display is required on foldable devices that expose more than one display.
        ActivityScenario.launch<MainActivity>(intent, options).use {
            compose.waitUntil(TIMEOUT_MS) {
                compose.onAllNodesWithText("Review & Trends").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("Home").assertIsDisplayed()
            compose.onNodeWithText("Review & Trends").assertIsDisplayed()

            compose.onNodeWithContentDescription("Search All Whip Data").performClick()
            compose.onNodeWithTag("unified-search-query").assertIsDisplayed()
            compose.onNodeWithText("Close").performClick()
            compose.onNodeWithContentDescription("Tasks tab").performClick()
            compose.onNodeWithTag("task-destination-Upcoming").performClick()
            compose.onNodeWithText("The next 30 days", substring = true).assertIsDisplayed()

            compose.onNodeWithContentDescription("Habits tab").performClick()
            compose.onNodeWithText("Check in, log a value, or continue a timer for habits due today.").assertIsDisplayed()

            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithTag("gym-destination-Workout").assertIsDisplayed()

            compose.onNodeWithContentDescription("Goals tab").performClick()
            compose.onNodeWithText("Long-term measurements, consistency, ranges, totals, and project milestones.").assertIsDisplayed()

            compose.onNodeWithContentDescription("Tracks tab").performClick()
            compose.onNodeWithTag("track-workspace-destination-Tracks").assertIsSelected()
            compose.onNodeWithTag("track-workspace-destination-Activity").performClick().assertIsSelected()
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithContentDescription("Tracks tab").performClick()
            compose.onNodeWithTag("track-workspace-destination-Tracks").assertIsSelected()
            compose.onNodeWithText("Define the Evidence That Matters").assertIsDisplayed()
            compose.onNodeWithText("Create Track").assertIsDisplayed()
        }
    }

    @Test
    fun switchingPrimaryAreasResetsEachAreaToItsFirstHeading() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<WhipApplication>()
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { it.copy(setupCompleted = true) }
        }
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java,
        ).putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        val options = ActivityOptions.makeBasic()
            .setLaunchDisplayId(Display.DEFAULT_DISPLAY)
            .toBundle()

        ActivityScenario.launch<MainActivity>(intent, options).use {
            compose.waitUntil(TIMEOUT_MS) {
                compose.onAllNodesWithText("Review & Trends").fetchSemanticsNodes().isNotEmpty()
            }

            compose.onNodeWithContentDescription("Tasks tab").performClick()
            compose.onNodeWithTag("task-destination-Today").assertIsSelected()
            compose.onNodeWithTag("task-destination-Inbox").performClick().assertIsSelected()

            compose.onNodeWithContentDescription("Habits tab").performClick()
            compose.onNodeWithTag("habit-destination-Today").assertIsSelected()
            compose.onNodeWithTag("habit-destination-All").performClick().assertIsSelected()

            compose.onNodeWithContentDescription("Tasks tab").performClick()
            compose.onNodeWithTag("task-destination-Today").assertIsSelected()

            compose.onNodeWithContentDescription("Habits tab").performClick()
            compose.onNodeWithTag("habit-destination-Today").assertIsSelected()
        }
    }

    @Test
    fun everyFirstClassDestinationIsReachableThroughVisibleNavigation() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<WhipApplication>()
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { it.copy(setupCompleted = true) }
            app.trackRepository.create(
                TrackDraft(
                    name = "Navigation Track",
                    fields = listOf(TrackFieldDraft("Name", TrackFieldType.ShortText, required = true, primary = true)),
                ),
            )
            val goalId = app.goalRepository.create(
                GoalDraft(
                    name = "Navigation Goal",
                    type = GoalType.ReachValue,
                    targetMin = 10.0,
                    startDate = app.clock.today(),
                ),
            )
            app.goalRepository.recordMeasurement(goalId, 5.0, app.clock.today())
        }
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java,
        ).putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(Display.DEFAULT_DISPLAY).toBundle()

        ActivityScenario.launch<MainActivity>(intent, options).use {
            compose.waitUntil(TIMEOUT_MS) {
                compose.onAllNodesWithText("Review & Trends").fetchSemanticsNodes().isNotEmpty()
            }

            compose.onNodeWithText("Review & Trends").performClick()
            compose.onNodeWithText("Included Sections").assertIsDisplayed()
            compose.onNodeWithText("Monthly").performClick()
            compose.waitUntil(TIMEOUT_MS) {
                ApplicationProvider.getApplicationContext<WhipApplication>()
                    .settingsRepository.current().reviewPeriod == ReviewPeriod.Monthly
            }
            compose.onNodeWithText("Save Review Filter").performClick()
            compose.onNodeWithTag("review-filter-name").performTextInput("Monthly Outcomes")
            compose.onNodeWithText("Save").performClick()
            compose.waitUntil(TIMEOUT_MS) {
                ApplicationProvider.getApplicationContext<WhipApplication>()
                    .settingsRepository.current().savedReviewFilters.any { it.name == "Monthly Outcomes" }
            }
            compose.onNodeWithContentDescription("Close Review & Trends").performClick()

            compose.onNodeWithContentDescription("Tasks tab").performClick()
            listOf("Today", "Inbox", "Upcoming", "Anytime").forEach { destination ->
                selectDestination("task-destination-$destination", destination)
            }
            compose.onNodeWithContentDescription("More task list actions").performClick()
            compose.onNodeWithText("Task History").performClick()
            compose.onNodeWithText("Your latest completed tasks", substring = true).assertIsDisplayed()

            compose.onNodeWithContentDescription("Habits tab").performClick()
            listOf("Today", "All", "Insights").forEach { destination ->
                selectDestination("habit-destination-$destination", destination)
            }
            compose.onNodeWithContentDescription("Open Pages").performClick()
            compose.onNodeWithText("Automations").performClick()
            compose.onNodeWithText("No Next-Action Automations Yet").assertIsDisplayed()
            compose.onNodeWithContentDescription("Open Pages").performClick()
            compose.onNodeWithText("Archived").performClick()
            compose.onNodeWithText("Archived Habits").assertIsDisplayed()

            compose.onNodeWithContentDescription("Goals tab").performClick()
            listOf("Active", "Completed", "Insights", "Archived").forEach { destination ->
                selectDestination("goal-destination-$destination", destination)
            }
            compose.onNodeWithText("Archived Goals").assertIsDisplayed()

            compose.onNodeWithContentDescription("Gym tab").performClick()
            listOf("Workout", "History", "Progress", "Library").forEach { destination ->
                selectDestination("gym-destination-$destination", destination)
            }
            listOf("Routines", "Exercises", "Machines", "Categories", "Tools").forEach { destination ->
                compose.onNodeWithTag("gym-library-$destination").performClick()
                compose.onNodeWithTag("gym-library-child-$destination").assertIsDisplayed().performClick()
            }

            compose.onNodeWithContentDescription("Tracks tab").performClick()
            listOf("Tracks", "Activity", "Insights").forEach { destination ->
                selectDestination("track-workspace-destination-$destination", destination)
            }
            compose.onNodeWithTag("track-workspace-destination-Tracks").performClick()
            compose.onNodeWithContentDescription("Navigation Track, 0 Entries. Open Track").performClick()
            compose.onNodeWithTag("track-workspace-navigation").assertIsDisplayed()
            compose.onNodeWithTag("track-detail-navigation").assertIsDisplayed()
            listOf("Entries", "Rules", "Insights", "Options").forEach { destination ->
                selectDestination("track-destination-$destination", destination)
            }
            compose.onNodeWithText("Track Options").assertIsDisplayed()
        }
    }

    private fun selectDestination(testTag: String, fullLabel: String) {
        if (compose.onAllNodesWithTag(testTag).fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag(testTag).performClick().assertIsSelected()
        } else {
            compose.onNodeWithContentDescription("Open Pages").performClick()
            compose.onNodeWithText(fullLabel).performClick()
        }
        compose.waitForIdle()
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
