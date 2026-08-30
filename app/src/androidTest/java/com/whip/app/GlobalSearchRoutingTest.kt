package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.GoalType
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.RoutineDayDraft
import com.whip.app.domain.RoutineDraft
import com.whip.app.domain.RoutineExerciseDraft
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GlobalSearchRoutingTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun everyNonTaskSearchDomainLandsOnTheExactActiveOrArchivedRecord() {
        runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        app.backupRepository.deleteAllData()
        val habitId = app.habitRepository.create(HabitDraft(name = "Searchable archived habit", startDate = app.clock.today()))
        app.habitRepository.setArchived(habitId, true)
        val goalId = app.goalRepository.create(GoalDraft(name = "Searchable archived goal", type = GoalType.ReachValue, targetMin = 10.0, startDate = app.clock.today()))
        app.goalRepository.setStatus(goalId, GoalStatus.Archived)
        val exerciseId = app.gymRepository.createExercise(ExerciseDraft("Searchable archived exercise"))
        val workoutId = app.gymRepository.startWorkout("Searchable discarded workout")
        app.gymRepository.addExerciseToWorkout(workoutId, exerciseId)
        app.gymRepository.discardWorkout(workoutId)
        val routineId = app.routineRepository.createRoutine(
            RoutineDraft("Searchable archived routine", days = listOf(RoutineDayDraft("Day", listOf(RoutineExerciseDraft(exerciseId))))),
        )
        app.routineRepository.setRoutineArchived(routineId, true)
        app.gymRepository.setExerciseArchived(exerciseId, true)

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        val intent = Intent(app, MainActivity::class.java).putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        launchMainActivity(intent).use {
            // Compact navigation labels the destination "Home"; the expanded
            // Fold rail does not. Wait for the action this journey actually
            // needs so launch readiness is layout-independent.
            compose.waitUntil(20_000) {
                SEARCH_DESCRIPTIONS.any { description ->
                    compose.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
                } || compose.onAllNodesWithContentDescription("App actions").fetchSemanticsNodes().isNotEmpty()
            }

            searchFor("Searchable archived habit")
            compose.onNodeWithText("Archived Habits").assertIsDisplayed()
            compose.onNodeWithText("Restore").assertIsDisplayed()

            searchFor("Searchable archived goal")
            if (compose.onAllNodesWithText("Automations").fetchSemanticsNodes().isEmpty()) {
                compose.onNode(
                    hasContentDescription("More Goal options") and
                        hasAnyAncestor(hasTestTag("goal-detail-surface")),
                ).performClick()
            }
            compose.onNodeWithText("Automations").performClick()
            compose.onNodeWithText("Goal Automations").assertIsDisplayed()
            compose.onNodeWithContentDescription("Close Goal details").performClick()

            searchFor("Searchable archived exercise")
            compose.onNodeWithText("Duplicate").assertIsDisplayed()
            compose.onNodeWithContentDescription("Close Exercise details").performClick()

            searchFor("Searchable discarded workout")
            compose.onNodeWithText("Workout History").assertIsDisplayed()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Restore to History").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Restore to History").performScrollTo().assertIsDisplayed()

            searchFor("Searchable archived routine")
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Searchable archived routine").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Searchable archived routine").assertIsDisplayed()
        }
        }
    }

    private fun searchFor(title: String) {
        // A detail can remain in the expanded support pane after a result is
        // opened. Return to the stable app shell before starting the next
        // cross-domain search instead of inheriting that detail's local scope.
        if (compose.onAllNodesWithContentDescription("Go to Home").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithContentDescription("Go to Home").performClick()
            compose.waitForIdle()
        }
        val searchDescription = SEARCH_DESCRIPTIONS.firstOrNull { description ->
            compose.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
        }
        if (searchDescription != null) {
            compose.onNodeWithContentDescription(searchDescription).performClick()
        } else {
            compose.onNodeWithContentDescription("App actions").performClick()
            compose.onNodeWithTag("workspace-search-menu-action").performClick()
        }
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("unified-search-query").fetchSemanticsNodes().isNotEmpty()
        }
        if (compose.onAllNodesWithText("Search All Whip").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("Search All Whip").performClick()
        }
        // Search state is intentionally retained while navigating an expanded
        // pane. Replace the prior query instead of appending to it.
        compose.onNodeWithTag("unified-search-query").performTextReplacement(title.removePrefix("Searchable "))
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(title).assertIsDisplayed().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Search").fetchSemanticsNodes().isEmpty()
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        val SEARCH_DESCRIPTIONS = listOf(
            "Search All Whip Data",
            "Search Habits",
            "Search Goals",
            "Search Tracks",
            "Search Gym",
            "Search Tasks",
        )
    }
}
