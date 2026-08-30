package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
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
        launchMainActivity(intent).use { scenario ->
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText("E2E task").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("home-tasks-today-record").assertIsDisplayed().performClick()
            compose.onNodeWithTag("task-destination-Today").assertIsSelected()
            compose.onNodeWithContentDescription("Go to Home").performClick()
            compose.onNodeWithText("E2E task").assertIsDisplayed()
            compose.onNodeWithTag("home-list").performScrollToNode(hasText("E2E habit"))
            compose.onNodeWithText("E2E habit").assertIsDisplayed()

            compose.onNodeWithContentDescription("Goals tab").performClick()
            compose.onNodeWithText("E2E goal").assertIsDisplayed()

            compose.onNodeWithContentDescription("Gym tab").performClick()
            // Expanded Fold layouts intentionally show the selected record in both
            // master and contextual detail panes.
            compose.onAllNodesWithText("E2E workout")[0].assertIsDisplayed()
            compose.onAllNodesWithText("E2E exercise")[0].assertIsDisplayed()

            compose.onNodeWithTag("gym-destination-Progress").performClick()
            compose.onAllNodesWithContentDescription("More Gym destinations").assertCountEquals(0)
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithTag("gym-progress-title").fetchSemanticsNodes().isNotEmpty()
            }
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithTag("gym-chart-summary").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("gym-chart-summary").assertIsDisplayed()
            compose.onNodeWithTag("gym-progress-list").performScrollToNode(
                hasContentDescription("E2E exercise Max weight chart", substring = true),
            )
            compose.onAllNodesWithContentDescription(
                "E2E exercise Max weight chart",
                substring = true,
            ).assertCountEquals(1)

            scenario.recreate()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithTag("gym-progress-list").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("gym-progress-list").performScrollToNode(
                hasTestTag("gym-chart-summary"),
            )
            compose.onNodeWithTag("gym-chart-summary").assertIsDisplayed()
        }
    }

    @Test
    fun dirtyTaskEditorSurvivesActivityRecreation() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        launchMainActivity(intent).use { scenario ->
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithContentDescription("Edit task", substring = true).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onAllNodesWithContentDescription("Edit task", substring = true)[0].performClick()
            compose.onNodeWithTag("task-editor-title").performTextClearance()
            compose.onNodeWithTag("task-editor-title").performTextInput("Unsaved fold draft")

            scenario.recreate()

            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText("Unsaved fold draft").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("task-editor-title").assertIsDisplayed()
            compose.onNodeWithText("Edit Task").assertIsDisplayed()
        }
    }
}
