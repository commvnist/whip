package com.whip.app

import android.app.ActivityOptions
import android.content.Intent
import android.view.Display
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalType
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.WorkoutSetDraft
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoreFeatureJourneyE2ETest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app: WhipApplication
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun seedUserJourney() = runBlocking {
        app.backupRepository.deleteAllData()
        val today = app.clock.today()
        app.taskRepository.create(TaskDraft("E2E task", scheduleKind = ScheduleKind.Once, date = today))
        app.habitRepository.create(HabitDraft(name = "E2E habit", startDate = today))
        app.goalRepository.create(
            GoalDraft(
                name = "E2E goal",
                type = GoalType.ReachValue,
                targetMin = 10.0,
                startDate = today,
            ),
        )
        val exerciseId = app.gymRepository.createExercise(
            ExerciseDraft(name = "E2E exercise", defaultGraphMetric = "MaxWeight"),
        )
        val historyId = app.gymRepository.startWorkout("E2E history workout")
        val historyExerciseId = app.gymRepository.addExerciseToWorkout(historyId, exerciseId)
        app.gymRepository.addSet(historyExerciseId, WorkoutSetDraft(weight = 50.0, reps = 5, completed = true))
        app.gymRepository.finishWorkout(historyId)
        val sessionId = app.gymRepository.startWorkout("E2E workout")
        val workoutExerciseId = app.gymRepository.addExerciseToWorkout(sessionId, exerciseId)
        app.gymRepository.addSet(workoutExerciseId, WorkoutSetDraft(weight = 52.5, reps = 5))
        Unit
    }

    @After
    fun clearJourney() = runBlocking {
        app.backupRepository.deleteAllData()
    }

    @Test
    fun seededFeaturesRenderThroughRealRepositoriesAndSurviveActivityRecreation() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(Display.DEFAULT_DISPLAY).toBundle()

        ActivityScenario.launch<MainActivity>(intent, options).use { scenario ->
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText("E2E task").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("E2E task").assertIsDisplayed()
            compose.onNodeWithText("E2E habit").assertIsDisplayed()

            compose.onNodeWithContentDescription("Goals tab").performClick()
            compose.onNodeWithText("E2E goal").assertIsDisplayed()

            compose.onNodeWithContentDescription("Gym tab").performClick()
            // Expanded Fold layouts intentionally show the selected record in both
            // master and contextual detail panes.
            compose.onAllNodesWithText("E2E workout")[0].assertIsDisplayed()
            compose.onAllNodesWithText("E2E exercise")[0].assertIsDisplayed()

            compose.onNodeWithTag("gym-destination-Progress").assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithTag("gym-progress-title").fetchSemanticsNodes().isNotEmpty()
            }
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithTag("gym-chart-summary").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("gym-chart-summary").assertIsDisplayed()
            compose.onAllNodesWithContentDescription(
                "E2E exercise Max weight chart",
                substring = true,
            ).assertCountEquals(2)

            scenario.recreate()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithTag("gym-progress-title").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("gym-progress-title").assertIsDisplayed()
            compose.onNodeWithTag("gym-chart-summary").assertIsDisplayed()
        }
    }

    @Test
    fun dirtyTaskEditorSurvivesActivityRecreation() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(Display.DEFAULT_DISPLAY).toBundle()

        ActivityScenario.launch<MainActivity>(intent, options).use { scenario ->
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithContentDescription("Task actions").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("Task actions").performClick()
            compose.onNodeWithText("Edit task").performClick()
            compose.onNodeWithTag("task-editor-title").performTextClearance()
            compose.onNodeWithTag("task-editor-title").performTextInput("Unsaved fold draft")

            scenario.recreate()

            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText("Unsaved fold draft").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("task-editor-title").assertIsDisplayed()
            compose.onNodeWithText("Edit task").assertIsDisplayed()
        }
    }
}
