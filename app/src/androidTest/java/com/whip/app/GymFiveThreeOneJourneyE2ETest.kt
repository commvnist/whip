package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.AppSettings
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.RoutineProgramKind
import com.whip.app.domain.WorkoutSessionState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real app-shell proof across 5/3/1 authoring, persistence, program launch, and execution. */
@RunWith(AndroidJUnit4::class)
class GymFiveThreeOneJourneyE2ETest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app: WhipApplication
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun seedExerciseLibrary() = runBlocking {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { AppSettings(setupCompleted = true) }
        listOf("Squat", "Bench Press", "Deadlift", "Overhead Press").forEach { name ->
            app.gymRepository.createExercise(ExerciseDraft(name = name, equipment = "Barbell"))
        }
    }

    @After
    fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test
    fun generatedProgramPersistsStartsNextDayAndRendersProgrammedWorkout() {
        val intent = Intent(app, MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        launchMainActivity(intent).use {
            compose.waitForIdle()
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithTag("gym-destination-Library").performClick()
            compose.onNodeWithTag("gym-library-Routines").performClick()
            compose.onNodeWithText("Create Routine").performClick()

            compose.onNode(
                hasText("Set Up 5/3/1") and hasClickAction() and
                    hasAnyAncestor(hasTestTag("routine-five-three-one-program-entry")),
            ).performClick()
            listOf("Squat", "Bench", "Deadlift", "Press")
                .zip(listOf("200", "150", "300", "100"))
                .forEach { (role, trainingMax) ->
                    compose.onNodeWithTag("five-three-one-training-max-$role")
                        .performScrollTo()
                        .performTextReplacement(trainingMax)
                }
            compose.onNodeWithTag("five-three-one-program-create").performClick()
            compose.onNodeWithTag("routine-program-structure").assertIsDisplayed()
            compose.onNodeWithTag("routine-builder-save").performClick()

            val routine = runBlocking {
                withTimeout(5_000) {
                    app.routineRepository.routines.first { rows ->
                        rows.any { row -> row.name == "4-Day 5/3/1" && row.programKind == RoutineProgramKind.FiveThreeOne }
                    }.single { row -> row.name == "4-Day 5/3/1" }
                }
            }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag("routine-start-next-${routine.id}").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("routine-start-next-${routine.id}").performScrollTo().performClick()

            runBlocking {
                withTimeout(5_000) {
                    app.gymRepository.sessions.first { sessions ->
                        sessions.any { session ->
                            session.state == WorkoutSessionState.Active && session.sourceRoutineId == routine.id
                        }
                    }
                }
            }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag("routine-start-next-${routine.id}").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("routine-start-next-${routine.id}").performScrollTo().performClick()
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag("active-workout-program-context").fetchSemanticsNodes().isNotEmpty()
            }

            compose.onNodeWithTag("active-workout-program-context")
                .assertIsDisplayed()
                .assertTextContains("5/3/1 · Cycle 1 · 5s Week · Day 1")
            compose.onNodeWithTag("next-set-focus").assertTextContains("Squat", substring = true)
            captureVisualCatalogSurface("gym.531.workout.active")
        }
    }
}
