package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.WorkoutSessionState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FiveThreeOneAuthorshipJourneyE2ETest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun libraryCreationKeepsCustomOrderAndExerciseOwnedTrainingValues() {
        runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update {
                AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark, dynamicColor = false, restTimerAutoStart = false)
            }
            listOf("Bench Press", "Squat").forEach { app.gymRepository.createExercise(ExerciseDraft(name = it)) }
        }
        fun tm(index: Int) = compose.onNodeWithTag("five-three-one-training-max-Custom-$index")
        fun increase(index: Int) = compose.onNodeWithTag("five-three-one-cycle-increase-Custom-$index")
        fun enter(field: SemanticsNodeInteraction, value: String) {
            field.performScrollTo().performTextReplacement(value)
            closeSoftKeyboard()
            field.assertTextContains(value)
        }
        fun picker(name: String) = compose.onNode(
            hasText(name) and hasAnyAncestor(hasTestTag("workout-exercise-picker-list")), useUnmergedTree = true,
        )
        fun rows() = runBlocking { app.routineRepository.exercises.first() }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithTag("gym-destination-Library").performClick()
            compose.onNodeWithTag("gym-library-Routines").performClick()
            compose.onNodeWithTag("workspace-add-action").performClick()
            compose.onNode(hasText("Set Up 5/3/1") and hasClickAction() and
                hasAnyAncestor(hasTestTag("routine-five-three-one-program-entry"))).performClick()
            compose.onNodeWithTag("five-three-one-layout-Custom").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Exercise 1: Bench Press").performScrollTo().assertIsDisplayed()
            enter(tm(0), "100")
            enter(increase(0), "4")
            compose.onNodeWithContentDescription("Exercise 2: Squat").performScrollTo().assertIsDisplayed()
            enter(tm(1), "200")
            enter(increase(1), "7")
            compose.onNodeWithContentDescription("Move Bench Press later").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Exercise 1: Squat").performScrollTo().assertIsDisplayed()
            tm(0).performScrollTo().assertTextContains("200")
            increase(0).assertTextContains("7")
            capture("reordered")
            compose.onNodeWithTag("five-three-one-add-custom-exercise").performScrollTo().performClick()
            compose.onNodeWithTag("exercise-picker-search").performTextReplacement("Paused Press")
            compose.onNodeWithTag("exercise-picker-create-empty").performClick()
            compose.onNodeWithTag("exercise-editor-name").assertTextContains("Paused Press")
            compose.onNode(hasText("Save") and hasAnyAncestor(hasTestTag("exercise-editor-surface"))).performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasText("Paused Press") and hasAnyAncestor(hasTestTag("workout-exercise-picker-list")),
                    useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }
            picker("Paused Press").performClick()
            tm(0).performScrollTo().assertIsDisplayed()
            capture("library-return")
            compose.onNodeWithContentDescription("Exercise 1: Squat").performScrollTo().assertIsDisplayed()
            tm(0).assertTextContains("200")
            increase(0).assertTextContains("7")
            compose.onNodeWithContentDescription("Exercise 2: Bench Press").performScrollTo().assertIsDisplayed()
            tm(1).assertTextContains("100")
            increase(1).assertTextContains("4")
            // Reconfirming the same Exercise must retain its authored cycle increase too.
            compose.onNodeWithContentDescription("Exercise 1: Squat").performScrollTo().performClick()
            picker("Squat · Current selection").performScrollTo().performClick()
            increase(0).performScrollTo().assertTextContains("7")
            enter(tm(2), "80")
            scenario.recreate()
            tm(0).performScrollTo().assertTextContains("200")
            increase(0).assertTextContains("7")
            compose.onNodeWithTag("five-three-one-program-create").performClick()
            compose.onNodeWithTag("routine-program-structure").assertIsDisplayed()
            compose.onNodeWithTag("routine-builder-save").performClick()
            compose.waitUntil(10_000) { rows().size == 3 }
            val names = runBlocking { app.gymRepository.exercises.first() }.associate { it.id to it.name }
            val days = runBlocking { app.routineRepository.days.first() }.sortedBy { it.position }
            assertEquals(listOf("Squat", "Bench Press", "Paused Press"), days.map { it.name })
            val byName = rows().associateBy { names.getValue(it.exerciseId) }
            assertEquals(200.0, byName.getValue("Squat").trainingMaxValue!!, 0.0)
            assertEquals(7.0, byName.getValue("Squat").cycleIncrementValue!!, 0.0)
            assertEquals(100.0, byName.getValue("Bench Press").trainingMaxValue!!, 0.0)
            assertEquals(4.0, byName.getValue("Bench Press").cycleIncrementValue!!, 0.0)
            assertEquals(80.0, byName.getValue("Paused Press").trainingMaxValue!!, 0.0)
            val routine = runBlocking { app.routineRepository.routines.first() }.single()
            compose.onNodeWithTag("routine-start-next-${routine.id}").performScrollTo().performClick()
            compose.onNodeWithTag("routine-active-workout-action").performScrollTo().performClick()
            compose.onNodeWithTag("next-set-focus").assertTextContains("Squat", substring = true)
            val session = runBlocking { app.gymRepository.sessions.first() }.single()
            val placement = runBlocking { app.gymRepository.workoutExercises.first() }.single()
            val sets = runBlocking { app.gymRepository.sets.first() }.sortedBy { it.position }
            assertEquals(200.0, placement.trainingMaxValueSnapshot!!, 0.0)
            assertEquals(130.0, sets.first().enteredWeight!!, 0.0)
            capture("workout")
            for (set in sets) {
                compose.onNodeWithTag("quick-set-save-next-${set.id}").performScrollTo().performClick()
                compose.waitUntil(10_000) { runBlocking { app.gymRepository.sets.first() }.single { it.id == set.id }.completed }
            }
            val performed = runBlocking { app.gymRepository.sets.first() }.associateBy { it.id }
            compose.onNodeWithTag("active-workout-list").performScrollToNode(hasTestTag("active-workout-finish"))
            compose.onNodeWithTag("active-workout-finish").performClick()
            compose.waitUntil(10_000) {
                runBlocking { app.gymRepository.sessions.first() }.single().state == WorkoutSessionState.Finished ||
                    compose.onAllNodesWithTag("finish-workout-confirm").fetchSemanticsNodes().isNotEmpty()
            }
            if (compose.onAllNodesWithTag("finish-workout-confirm").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithTag("finish-workout-confirm").performClick()
            }
            compose.waitUntil(10_000) { runBlocking { app.gymRepository.sessions.first() }.single().state == WorkoutSessionState.Finished }
            assertEquals(1, runBlocking { app.routineRepository.routines.first() }.single().nextProgramDayPosition)
            compose.onNodeWithTag("gym-destination-History").performClick()
            compose.onNodeWithTag("history-workout-toggle-${session.id}").performScrollTo().performClick()
            compose.onNodeWithTag("history-set-card-${sets.first().id}").performScrollTo()
            compose.onNodeWithTag("history-set-performed-${sets.first().id}").assertTextContains("130 kg × 5 reps")
            capture("history")
            assertEquals(performed, runBlocking { app.gymRepository.sets.first() }.associateBy { it.id })
        }
    }

    private fun capture(state: String) {
        compose.waitForIdle()
        captureVisualCatalogSurface("gym.531-authorship.$state")
    }
}
