package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.AppSettings
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.domain.WorkoutSetDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * True UI-to-database-to-UI acceptance paths for first-class areas that used
 * to have only component or repository tests.
 */
@RunWith(AndroidJUnit4::class)
class FirstClassWorkflowE2ETest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app: WhipApplication
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun reset() = runBlocking {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { AppSettings(setupCompleted = true) }
    }

    @After
    fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test
    fun trackCanBeDefinedPopulatedAndReopenedThroughTheRealUi() {
        launch().use { scenario ->
            compose.waitForIdle()
            compose.onNodeWithContentDescription("Tracks tab").performClick()
            compose.onNodeWithTag("track-list").performScrollToNode(hasText("Create First Track"))
            compose.onNodeWithText("Create First Track").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("track-editor-name").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("track-editor-name").performTextInput("Reading Log")
            compose.onNodeWithText("Save").performClick()

            val projection = runBlocking {
                withTimeout(5_000) {
                    app.trackRepository.projections.first { rows ->
                        rows.any { it.track.name == "Reading Log" && it.fields.isNotEmpty() }
                    }
                        .single { it.track.name == "Reading Log" }
                }
            }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText("Reading Log").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("workspace-add-action").performClick()
            compose.onNodeWithTag("track-entry-short-text-${projection.primaryField.uuid}")
                .performTextInput("The Left Hand of Darkness")
            compose.onNodeWithText("Add").performClick()

            runBlocking {
                withTimeout(5_000) {
                    app.trackRepository.projections.first { rows ->
                        rows.singleOrNull { it.track.id == projection.track.id }?.entries?.size == 1
                    }
                }
            }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText("The Left Hand of Darkness").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("The Left Hand of Darkness").assertIsDisplayed()

            scenario.recreate()
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText("The Left Hand of Darkness").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("The Left Hand of Darkness").assertIsDisplayed()
        }
    }

    @Test
    fun workoutCanBeCreatedLoggedFinishedAndFoundInHistoryThroughTheRealUi() {
        launch().use {
            compose.waitForIdle()
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithText("Create First Exercise").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("exercise-editor-name").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("exercise-editor-name").performTextInput("Goblet Squat")
            compose.onNodeWithText("Save").performClick()

            val exerciseCreated = runBlocking {
                withTimeoutOrNull(5_000) {
                    app.gymRepository.exercises.first { rows -> rows.any { it.name == "Goblet Squat" } }
                }
            }
            checkNotNull(exerciseCreated) { "Exercise creation did not reach the repository" }
            val workoutOnlyExerciseId = runBlocking {
                app.gymRepository.createExercise(ExerciseDraft(name = "Band Pull-Apart"))
            }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText("Start Workout").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Start Workout").performClick()
            compose.onNodeWithTag("workout-editor-name").performTextInput("Coverage Workout")
            closeSoftKeyboard()
            compose.waitForIdle()
            compose.onNodeWithTag("workout-editor-confirm").assertIsDisplayed().performClick()

            val session = runBlocking {
                withTimeoutOrNull(5_000) {
                    app.gymRepository.sessions.first { rows ->
                        rows.any { it.state == WorkoutSessionState.Active }
                    }.single { it.state == WorkoutSessionState.Active }
                }
            }
            checkNotNull(session) { "Workout start did not reach the repository" }
            compose.onNode(
                hasText("Add Exercise to This Workout") and
                    hasAnyAncestor(hasTestTag("active-workout-empty-state")),
            ).performScrollTo().performClick()
            compose.onNodeWithTag("workout-exercise-picker-list").performScrollToNode(hasText("Goblet Squat"))
            compose.onNodeWithText("Goblet Squat").performClick()
            val exerciseAdded = runBlocking {
                withTimeoutOrNull(5_000) {
                    app.gymRepository.workoutExercises.first { rows -> rows.any { it.sessionId == session.id } }
                        .single { it.sessionId == session.id }
                }
            }
            checkNotNull(exerciseAdded) { "Exercise selection did not reach the active workout" }
            val set = runBlocking {
                withTimeoutOrNull(5_000) {
                    app.gymRepository.sets.first { rows ->
                        rows.any { it.workoutExerciseId == exerciseAdded.id }
                    }.single { it.workoutExerciseId == exerciseAdded.id }
                }
            }
            checkNotNull(set) { "Exercise selection did not create its editable initial set" }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag("workout-exercise-picker-scope").fetchSemanticsNodes().isEmpty()
            }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag("quick-set-load-${set.id}").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("quick-set-load-${set.id}").performTextReplacement("24")
            compose.onNodeWithTag("quick-set-reps-${set.id}").performTextReplacement("10")
            closeSoftKeyboard()
            compose.onNodeWithTag("quick-set-save-next-${set.id}").performScrollTo().performClick()
            val setCompleted = runBlocking {
                withTimeoutOrNull(5_000) {
                    app.gymRepository.sets.first { rows -> rows.singleOrNull()?.completed == true }
                }
            }
            checkNotNull(setCompleted) { "Set completion did not reach the repository" }

            compose.onNodeWithTag("add-exercise-to-active-workout").performScrollTo().performClick()
            compose.onNodeWithTag("workout-exercise-picker-scope").assertIsDisplayed()
            compose.onNodeWithTag("workout-exercise-picker-list").performScrollToNode(hasText("Band Pull-Apart"))
            compose.onNodeWithText("Band Pull-Apart").performClick()
            val workoutOnlyExerciseAdded = runBlocking {
                withTimeoutOrNull(5_000) {
                    app.gymRepository.workoutExercises.first { rows ->
                        rows.any { it.sessionId == session.id && it.exerciseId == workoutOnlyExerciseId }
                    }.single { it.sessionId == session.id && it.exerciseId == workoutOnlyExerciseId }
                }
            }
            checkNotNull(workoutOnlyExerciseAdded) { "Workout-only exercise selection did not reach the active session" }
            val workoutOnlyInitialSet = runBlocking {
                withTimeoutOrNull(5_000) {
                    app.gymRepository.sets.first { rows ->
                        rows.any { it.workoutExerciseId == workoutOnlyExerciseAdded.id }
                    }.single { it.workoutExerciseId == workoutOnlyExerciseAdded.id }
                }
            }
            checkNotNull(workoutOnlyInitialSet) { "Workout-only exercise did not receive its editable initial set" }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag("workout-exercise-picker-scope").fetchSemanticsNodes().isEmpty()
            }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag("quick-set-load-${workoutOnlyInitialSet.id}")
                    .fetchSemanticsNodes().isNotEmpty()
            }

            compose.onNodeWithTag("active-workout-list")
                .performScrollToNode(hasTestTag("active-workout-finish"))
            compose.onNodeWithTag("active-workout-finish").performClick()
            compose.onNodeWithTag("finish-workout-confirmation").assertIsDisplayed()
            compose.onNodeWithTag("finish-workout-confirm").performClick()
            val workoutFinished = runBlocking {
                withTimeoutOrNull(5_000) {
                    app.gymRepository.sessions.first { rows ->
                        rows.singleOrNull { it.id == session.id }?.state == WorkoutSessionState.Finished
                    }
                }
            }
            checkNotNull(workoutFinished) { "Workout finish did not reach the repository" }
            compose.onNodeWithText("History").performClick()
            compose.onNodeWithTag("history-workout-card-${session.id}")
                .performScrollTo()
                .assertIsDisplayed()
            compose.onNode(
                hasText("Coverage Workout") and
                    hasAnyAncestor(hasTestTag("history-workout-card-${session.id}")),
                useUnmergedTree = true,
            ).assertIsDisplayed()
            check(
                runBlocking {
                    app.gymRepository.workoutExercises.first()
                        .any { it.sessionId == session.id && it.exerciseId == workoutOnlyExerciseId }
                },
            ) { "Workout-only exercise was not preserved with the finished workout" }
        }
    }

    @Test
    fun editingAnEarlierSetDoesNotInvalidateTheActiveSetComposer() {
        val setup = runBlocking {
            val exerciseId = app.gymRepository.createExercise(ExerciseDraft(name = "Bench Press"))
            val sessionId = app.gymRepository.startWorkout("Sequential set edits")
            val placementId = app.gymRepository.addExerciseToWorkout(sessionId, exerciseId)
            val earlierSetId = app.gymRepository.addSet(
                placementId,
                WorkoutSetDraft(weight = 80.0, reps = 5, completed = true),
            )
            val activeSetId = app.gymRepository.addSet(
                placementId,
                WorkoutSetDraft(weight = 82.5, reps = 5),
            )
            Triple(earlierSetId, activeSetId, sessionId)
        }

        launch().use {
            compose.waitForIdle()
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag("quick-set-load-${setup.second}").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("1 Completed Set").performScrollTo().performClick()
            compose.onNode(
                hasText("Set 1", substring = true) and hasClickAction(),
            ).performScrollTo().performClick()
            compose.onNodeWithTag("workout-set-editor-load").performTextReplacement("85")
            compose.onNodeWithTag("workout-set-editor-reps").performTextReplacement("6")
            closeSoftKeyboard()
            compose.onNodeWithText("Save").performClick()

            runBlocking {
                withTimeout(5_000) {
                    app.gymRepository.sets.first { sets ->
                        sets.single { set -> set.id == setup.first }.enteredWeight == 85.0
                    }
                }
            }
            compose.onNodeWithTag("quick-set-load-${setup.second}").performTextReplacement("87.5")
            compose.onNodeWithTag("quick-set-reps-${setup.second}").performTextReplacement("7")
            closeSoftKeyboard()
            compose.onNodeWithTag("quick-set-save-next-${setup.second}").performScrollTo().performClick()

            val completed = runBlocking {
                withTimeoutOrNull(5_000) {
                    app.gymRepository.sets.first { sets ->
                        sets.single { set -> set.id == setup.second }.completed
                    }.single { set -> set.id == setup.second }
                }
            }
            checkNotNull(completed) { "Active Set save was rejected after an unrelated earlier Set edit" }
            compose.onAllNodesWithText("changed before this quick save", substring = true).assertCountEquals(0)
            check(
                runBlocking {
                    app.gymRepository.sessions.first().single { session -> session.id == setup.third }.state ==
                        WorkoutSessionState.Active
                },
            )
        }
    }

    private fun launch(): ActivityScenario<MainActivity> {
        val intent = Intent(app, MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        return launchMainActivity(intent)
    }
}
