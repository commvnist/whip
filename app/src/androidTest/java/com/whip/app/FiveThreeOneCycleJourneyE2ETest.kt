package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.TrainingMaxDecisionAction
import com.whip.app.domain.WorkoutSessionState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Native complete-cycle proof, including an unconfirmed decision through recreation. */
class FiveThreeOneCycleJourneyE2ETest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun fullCycleReviewsExplicitlyAndNextCycleKeepsOriginalPerformedHistory() {
        runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update {
                AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark, dynamicColor = false, restTimerAutoStart = false)
            }
            app.gymRepository.createExercise(ExerciseDraft(name = "Bench Press"))
        }
        fun sessions() = runBlocking { app.gymRepository.sessions.first() }
        fun sets() = runBlocking { app.gymRepository.sets.first() }
        fun sessionSets(sessionId: Long) = runBlocking {
            val placements = app.gymRepository.workoutExercises.first().filter { it.sessionId == sessionId }.map { it.id }.toSet()
            app.gymRepository.sets.first().filter { it.workoutExerciseId in placements }.sortedBy { it.position }
        }
        fun routine() = runBlocking { app.routineRepository.routines.first() }.single()
        fun decisions() = runBlocking { app.routineRepository.trainingMaxDecisions.first() }
        fun openNext() {
            compose.onNodeWithTag("gym-destination-Library").performClick()
            if (compose.onAllNodesWithTag("gym-library-Routines").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithTag("gym-library-Routines").performClick()
            }
            compose.onNodeWithTag("routine-start-next-${routine().id}").performScrollTo().performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("routine-active-workout-action").fetchSemanticsNodes().size == 1
            }
            compose.onNodeWithTag("routine-active-workout-action").performScrollTo().performClick()
            compose.onNodeWithTag("next-set-focus").assertTextContains("Bench Press", substring = true)
        }
        fun finish() {
            compose.onNodeWithTag("active-workout-list").performScrollToNode(hasTestTag("active-workout-finish"))
            compose.onNodeWithTag("active-workout-finish").performClick()
            compose.waitForIdle()
            if (compose.onAllNodesWithTag("finish-workout-confirm").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithTag("finish-workout-confirm").performClick()
            }
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithTag("gym-destination-Library").performClick()
            compose.onNodeWithTag("gym-library-Routines").performClick()
            compose.onNodeWithTag("workspace-add-action").performClick()
            compose.onNode(hasText("Set Up 5/3/1") and hasClickAction() and
                hasAnyAncestor(hasTestTag("routine-five-three-one-program-entry"))).performClick()
            compose.onNodeWithTag("five-three-one-layout-Custom").performScrollTo().performClick()
            compose.onNodeWithTag("five-three-one-progression-adaptive").performScrollTo().performClick()
            compose.onNodeWithTag("five-three-one-training-max-Custom-0").performScrollTo().performTextReplacement("100")
            closeSoftKeyboard()
            compose.onNodeWithTag("five-three-one-cycle-increase-Custom-0").assertTextContains("2.5")
            capture("setup")
            compose.onNodeWithTag("five-three-one-program-create").performClick()
            compose.onNodeWithTag("routine-program-structure").assertIsDisplayed()
            compose.onNodeWithTag("routine-builder-save").performClick()
            compose.waitUntil(10_000) { runBlocking { app.routineRepository.routines.first() }.size == 1 }
            assertEquals("", routine().notes)
            val exerciseId = runBlocking { app.gymRepository.exercises.first() }.single().id
            for (phase in 0..3) {
                openNext()
                val session = sessions().single { it.state == WorkoutSessionState.Active }
                assertEquals(phase, session.sourceRoutinePhaseIndex)
                assertEquals(1, session.sourceRoutineCycle)
                assertEquals("", session.notes)
                val phaseSets = sessionSets(session.id)
                assertTrue(phaseSets.isNotEmpty())
                if (phase == 0) {
                    compose.onNodeWithTag("next-set-focus").assertTextContains("65 kg entered × 5 reps")
                    capture("workout")
                    compose.onNodeWithTag("next-set-focus").performClick()
                    compose.onNodeWithTag("quick-set-save-next-${phaseSets.first().id}").assertIsDisplayed()
                    val menu = compose.onNodeWithContentDescription("Manage set 1").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                    val lane = compose.onNodeWithTag("workout-execution-lane").fetchSemanticsNode().boundsInRoot
                    assertTrue("Next Set must reveal its controls below the pinned execution lane", menu.top >= lane.bottom)
                    capture("focused-set")
                }
                for (set in phaseSets) {
                    compose.onNodeWithTag("quick-set-save-next-${set.id}").performScrollTo().performClick()
                    compose.waitUntil(10_000) { sets().single { it.id == set.id }.completed }
                }
                if (phase == 3) capture("cycle-complete")
                finish()
                if (phase == 3) {
                    compose.onNodeWithTag("training-max-choice-standard-$exerciseId").performScrollTo().assertIsSelected()
                    assertTrue(decisions().isEmpty())
                    capture("review")
                    compose.onNodeWithTag("training-max-choice-hold-$exerciseId").performScrollTo().performClick()
                    scenario.recreate()
                    compose.onNodeWithTag("training-max-choice-hold-$exerciseId").performScrollTo().assertIsSelected()
                    assertEquals(WorkoutSessionState.Active, sessions().single { it.id == session.id }.state)
                    assertTrue(decisions().isEmpty())
                    compose.onNodeWithTag("training-max-choice-standard-$exerciseId").performScrollTo().performClick()
                    compose.onNodeWithTag("training-max-decision-summary-$exerciseId").assertTextContains("102.5", substring = true)
                    capture("decision")
                    compose.onNodeWithTag("apply-training-max-decisions").performClick()
                }
                compose.waitUntil(10_000) {
                    sessions().single { it.id == session.id }.let { it.state == WorkoutSessionState.Finished && !it.restTimerCleanupPending }
                }
                assertTrue(sessions().single { it.id == session.id }.programProgressAdvanced)
            }
            val decision = decisions().single()
            assertEquals(TrainingMaxDecisionAction.UseStandard, decision.action)
            assertEquals(100.0, decision.previousTrainingMax, 0.0)
            assertEquals(2.5, decision.appliedDelta, 0.0)
            assertEquals(102.5, decision.resultingTrainingMax, 0.0)
            val performed = sets().associateBy { it.id }
            val finished = sessions().associateBy { it.id }
            openNext()
            val next = sessions().single { it.state == WorkoutSessionState.Active }
            assertEquals(2, next.sourceRoutineCycle)
            assertEquals(0, next.sourceRoutinePhaseIndex)
            val placement = runBlocking { app.gymRepository.workoutExercises.first() }.single { it.sessionId == next.id }
            assertEquals(102.5, placement.trainingMaxValueSnapshot!!, 0.0)
            capture("next-cycle")
            val first = finished.values.minBy { it.sourceRoutinePhaseIndex!! }
            val firstSet = sessionSets(first.id).first()
            compose.onNodeWithTag("gym-destination-History").performClick()
            compose.onNodeWithTag("history-workout-toggle-${first.id}").performScrollTo().performClick()
            compose.onNodeWithTag("history-set-card-${firstSet.id}").performScrollTo()
            compose.onNodeWithTag("history-set-performed-${firstSet.id}").assertTextContains("65 kg × 5 reps")
            capture("history")
            assertEquals(performed, sets().filter { it.id in performed }.associateBy { it.id })
            assertEquals(finished, sessions().filter { it.id in finished }.associateBy { it.id })
            assertEquals(listOf(decision), decisions())
        }
    }

    private fun capture(state: String) {
        compose.waitForIdle()
        captureVisualCatalogSurface("gym.531-cycle.$state")
    }
}
