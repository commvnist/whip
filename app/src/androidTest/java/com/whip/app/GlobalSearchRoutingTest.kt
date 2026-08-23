package com.whip.app

import android.app.ActivityOptions
import android.content.Intent
import android.view.Display
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
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
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(Display.DEFAULT_DISPLAY).toBundle()

        ActivityScenario.launch<MainActivity>(intent, options).use {
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Home").fetchSemanticsNodes().isNotEmpty() }

            searchFor("Searchable archived habit")
            compose.onNodeWithText("Archived habits").assertIsDisplayed()
            compose.onNodeWithText("Restore").assertIsDisplayed()

            searchFor("Searchable archived goal")
            compose.onNodeWithText("Connections").assertIsDisplayed()
            compose.onNodeWithText("Close").performClick()

            searchFor("Searchable archived exercise")
            compose.onNodeWithText("Duplicate").assertIsDisplayed()
            compose.onNodeWithText("Close").performClick()

            searchFor("Searchable discarded workout")
            compose.onNodeWithText("Workout history").assertIsDisplayed()
            compose.onNodeWithText("Restore to history").assertIsDisplayed()

            searchFor("Searchable archived routine")
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Searchable archived routine").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Searchable archived routine").assertIsDisplayed()
        }
        }
    }

    private fun searchFor(title: String) {
        compose.onNodeWithContentDescription("Search all Whip data").performClick()
        compose.onNodeWithText("Tasks, habits, goals, exercises…").performTextInput(title.removePrefix("Searchable "))
        compose.onNodeWithText(title).assertIsDisplayed().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Tasks, habits, goals, exercises…").fetchSemanticsNodes().isEmpty()
        }
        compose.waitForIdle()
    }
}
