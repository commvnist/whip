package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
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
import com.whip.app.domain.HabitDraft
import com.whip.app.core.ReviewPeriod

@RunWith(AndroidJUnit4::class)
class WhipNavigationTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun homeEntityInspectorsPreserveTheHomeDestination() {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update {
                it.copy(
                    setupCompleted = true,
                    hiddenHomeSections = emptySet(),
                    collapsedHomeSections = emptySet(),
                )
            }
            app.habitRepository.create(HabitDraft(name = "Home overlay habit", startDate = app.clock.today()))
            app.goalRepository.create(
                GoalDraft(
                    name = "Home overlay goal",
                    type = GoalType.ReachValue,
                    targetMin = 10.0,
                    startDate = app.clock.today(),
                ),
            )
        }
        val intent = Intent(app, MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)

        launchMainActivity(intent).use {
            compose.waitUntil(TIMEOUT_MS) {
                compose.onAllNodesWithContentDescription("Open habit details for Home overlay habit")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("Open habit details for Home overlay habit")
                .performScrollTo()
                .performClick()
            compose.onNodeWithTag("entity-inspector").assertIsDisplayed()
            compose.onNodeWithContentDescription("Home").assertIsSelected()
            compose.onNodeWithTag("home-list").assertIsDisplayed()
            compose.onAllNodesWithTag("habit-workspace-navigation").assertCountEquals(0)
            compose.onNodeWithContentDescription("Close Habit details").performClick()

            compose.onNodeWithContentDescription("Open goal details for Home overlay goal")
                .performScrollTo()
                .performClick()
            compose.onNodeWithTag("entity-inspector").assertIsDisplayed()
            compose.onNodeWithContentDescription("Home").assertIsSelected()
            compose.onNodeWithTag("home-list").assertIsDisplayed()
            compose.onAllNodesWithTag("goal-workspace-navigation").assertCountEquals(0)
            compose.onNodeWithContentDescription("Close Goal details").performClick()
        }
    }

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
        // API 28+ is pinned to the default display for foldables; API 26–27
        // launches without ActivityOptions because AndroidX forbids them there.
        launchMainActivity(intent).use {
            compose.waitUntil(TIMEOUT_MS) {
                compose.onAllNodesWithText("Build Your Day").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("Home").assertIsDisplayed()
            compose.onNodeWithText("Build Your Day").assertIsDisplayed()
            compose.onNodeWithText("Start here").assertIsDisplayed()
            compose.onNodeWithTag("home-destination-tasks").assertIsDisplayed()
            compose.onAllNodesWithText("Review & Trends").assertCountEquals(1)

            compose.onNodeWithTag("workspace-search-action").performClick()
            compose.onNodeWithTag("unified-search-query").assertIsDisplayed()
            compose.onNodeWithContentDescription("Close Search").performClick()
            compose.onNodeWithContentDescription("Tasks tab").performClick()
            compose.onAllNodesWithText("0 tasks", substring = true).assertCountEquals(0)
            selectDestination("task-destination-Upcoming")
            compose.onNodeWithText("The next 30 days", substring = true).assertIsDisplayed()
            compose.onAllNodesWithText("0 tasks", substring = true).assertCountEquals(0)

            compose.onNodeWithContentDescription("Habits tab").performClick()
            compose.onNodeWithText(
                "Check in, log a value, or continue a timer.",
            ).assertIsDisplayed()

            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithTag("gym-destination-Workout").assertIsDisplayed()

            compose.onNodeWithContentDescription("Goals tab").performClick()
            compose.onNodeWithText("Long-term progress, consistency, ranges, totals, and project milestones.").assertIsDisplayed()

            compose.onNodeWithContentDescription("Tracks tab").performClick()
            compose.onNodeWithTag("track-workspace-destination-Tracks").assertIsSelected()
            compose.onNodeWithTag("track-workspace-destination-Tracks").performClick().assertIsSelected()
            compose.onNodeWithTag("track-workspace-destination-Activity").performClick().assertIsSelected()
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithContentDescription("Tracks tab").performClick()
            compose.onNodeWithTag("track-workspace-destination-Activity").assertIsSelected()
            compose.onNodeWithTag("track-workspace-destination-Tracks").performClick().assertIsSelected()
            compose.onNodeWithTag("track-list").performScrollToNode(hasText("Create First Track"))
            compose.onAllNodesWithText("0 Tracks").assertCountEquals(0)
            compose.onNodeWithText("Track What Matters").assertIsDisplayed()
            compose.onNodeWithText("Create First Track").assertIsDisplayed()
        }
    }

    @Test
    fun switchingPrimaryAreasPreservesEachAreasLastHeading() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<WhipApplication>()
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { it.copy(setupCompleted = true) }
        }
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java,
        ).putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        launchMainActivity(intent).use {
            compose.waitUntil(TIMEOUT_MS) {
                compose.onAllNodesWithText("Build Your Day").fetchSemanticsNodes().isNotEmpty()
            }

            compose.onNodeWithContentDescription("Tasks tab").performClick()
            compose.onNodeWithTag("task-destination-Today").assertIsSelected()
            compose.onNodeWithTag("task-destination-Inbox").performClick().assertIsSelected()

            compose.onNodeWithContentDescription("Habits tab").performClick()
            compose.onNodeWithTag("habit-destination-Today").assertIsSelected()
            compose.onNodeWithTag("habit-destination-All").performClick().assertIsSelected()

            compose.onNodeWithContentDescription("Tasks tab").performClick()
            compose.onNodeWithTag("task-destination-Inbox").assertIsSelected()

            compose.onNodeWithContentDescription("Habits tab").performClick()
            compose.onNodeWithTag("habit-destination-All").assertIsSelected()
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
        launchMainActivity(intent).use {
            compose.waitUntil(TIMEOUT_MS) {
                compose.onAllNodesWithText("Review & Trends").fetchSemanticsNodes().isNotEmpty()
            }

            compose.onNodeWithText("Review & Trends").performClick()
            if (compose.onAllNodesWithText("Review Options").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithText("Review Options").performClick()
            }
            compose.onNodeWithText("Included Sections").assertIsDisplayed()
            compose.onNodeWithText("Monthly").performClick()
            compose.waitUntil(TIMEOUT_MS) {
                ApplicationProvider.getApplicationContext<WhipApplication>()
                    .settingsRepository.current().reviewPeriod == ReviewPeriod.Monthly
            }
            compose.onAllNodesWithText("Saved Views").assertCountEquals(0)
            compose.onAllNodesWithText("Save Review Filter").assertCountEquals(0)
            compose.onNodeWithContentDescription("Close Review & Trends").performClick()

            compose.onNodeWithContentDescription("Tasks tab").performClick()
            listOf("Today", "Inbox", "Upcoming", "History").forEach { destination ->
                selectDestination("task-destination-$destination")
            }
            compose.onNodeWithText("Your latest completed tasks", substring = true).assertIsDisplayed()
            compose.onAllNodesWithText("Task History").assertCountEquals(0)
            compose.onAllNodesWithContentDescription("Back to Today").assertCountEquals(0)

            compose.onNodeWithContentDescription("Habits tab").performClick()
            listOf("Today", "All", "Insights", "Archived").forEach { destination ->
                selectDestination("habit-destination-$destination")
            }
            compose.onNodeWithText("Archived Habits").assertIsDisplayed()

            compose.onNodeWithContentDescription("Goals tab").performClick()
            listOf("Active", "History", "Insights", "Archived").forEach { destination ->
                selectDestination("goal-destination-$destination")
            }
            compose.onNodeWithText("Archived Goals").assertIsDisplayed()

            compose.onNodeWithContentDescription("Gym tab").performClick()
            listOf("Workout", "History", "Progress", "Library").forEach { destination ->
                selectDestination("gym-destination-$destination")
            }
            listOf("Routines", "Exercises", "Machines", "Categories", "Tools").forEach { destination ->
                compose.onNodeWithTag("gym-library-list").performScrollToNode(hasText(destination))
                compose.onNodeWithTag("gym-library-$destination").performClick()
                compose.onNodeWithTag("gym-library-child-$destination").assertIsDisplayed().performClick()
            }

            compose.onNodeWithContentDescription("Tracks tab").performClick()
            listOf("Tracks", "Activity", "Archived", "Insights").forEach { destination ->
                selectDestination("track-workspace-destination-$destination")
            }
            compose.onNodeWithText("Patterns across visible Tracks.").assertIsDisplayed()
            compose.onNodeWithTag("track-workspace-destination-Tracks").performClick()
            compose.onNodeWithContentDescription("Navigation Track, 0 Entries. Open Track").performClick()
            compose.onNodeWithTag("track-workspace-navigation").assertIsDisplayed()
            compose.onNodeWithTag("track-detail-navigation").assertIsDisplayed()
            listOf("Entries", "Insights", "Options").forEach { destination ->
                selectDestination("track-destination-$destination")
            }
            compose.onNodeWithText("Track Options").assertIsDisplayed()
        }
    }

    private fun selectDestination(testTag: String) {
        compose.onNodeWithTag(testTag).performClick().assertIsSelected()
        compose.waitForIdle()
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
