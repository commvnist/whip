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

/** Ordinary app-shell authoring, draft recovery, editing and day-specific workout launch. */
class RoutineAuthoringJourneyE2ETest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun twoDayRoutinePreservesContextAndStartsTheChosenDay() {
        val ids = runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update {
                AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark, dynamicColor = false)
            }
            listOf("Bench Press", "Supported cable row", "Goblet Squat").map {
                app.gymRepository.createExercise(ExerciseDraft(name = it))
            }
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithTag("gym-destination-Library").performClick()
            compose.onNodeWithTag("gym-library-Routines").performClick()
            compose.onNodeWithTag("workspace-add-action").performClick()
            compose.onNodeWithTag("routine-add-exercises").assertIsDisplayed()
            compose.onNodeWithTag("routine-five-three-one-program-entry").assertIsDisplayed()
            capture("new")
            compose.onNodeWithTag("routine-editor-name").performTextReplacement("Two day strength")
            closeSoftKeyboard()
            outline(hasText("Upper / Lower")).performClick()
            add("Bench Press", "Supported cable row")
            backToOutline()
            capture("upper")
            outline(hasText("Lower · 0")).performClick()
            add("Goblet Squat")
            compose.onNodeWithText("Exercise notes").performTextReplacement("Slow descent; keep the prescribed day.")
            closeSoftKeyboard()
            scenario.recreate()
            compose.onNodeWithTag("routine-placement-editor").assertIsDisplayed()
            compose.onNodeWithText("Exercise notes").assertTextContains("Slow descent; keep the prescribed day.")
            capture("recovered-placement")
            backToOutline()
            outline(hasText("Lower · 1")).assertIsSelected()
            capture("lower")
            outline(hasTestTag("routine-notes-disclosure")).performClick()
            outline(hasTestTag("routine-editor-notes")).performTextReplacement("Alternate these days with a rest day between sessions.")
            closeSoftKeyboard()
            scenario.recreate()
            outline(hasTestTag("routine-editor-notes")).assertTextContains("Alternate these days with a rest day between sessions.")
            capture("routine-notes")
            compose.onNodeWithTag("routine-builder-save").performClick()
            compose.waitUntil(10_000) { routines().any { it.name == "Two day strength" } }
            val routine = routines().single()
            assertEquals("Alternate these days with a rest day between sessions.", routine.notes)
            val days = runBlocking { app.routineRepository.days.first() }.sortedBy { it.position }
            val placements = runBlocking { app.routineRepository.exercises.first() }
            assertEquals(listOf("Upper", "Lower"), days.map { it.name })
            assertEquals(ids.take(2), placements.filter { it.routineDayId == days[0].id }.sortedBy { it.position }.map { it.exerciseId })
            val lower = placements.single { it.routineDayId == days[1].id }
            assertEquals(ids[2], lower.exerciseId)
            assertEquals("Slow descent; keep the prescribed day.", lower.notes)
            val originalSets = runBlocking { app.routineRepository.sets.first() }
            compose.onNodeWithContentDescription("Edit routine Two day strength").performScrollTo().performClick()
            compose.onNodeWithTag("routine-editor-name").assertTextContains("Two day strength")
            outline(hasTestTag("routine-editor-notes")).assertTextContains(routine.notes)
            outline(hasText("Lower · 1")).performClick()
            compose.onNodeWithContentDescription("Edit routine exercise Goblet Squat").performScrollTo().performClick()
            compose.onNodeWithText("Exercise notes").assertTextContains(lower.notes)
            compose.onNodeWithText("Exercise notes").performTextReplacement("Updated lower-day cue")
            closeSoftKeyboard()
            compose.onNodeWithTag("routine-builder-save").performClick()
            compose.onNodeWithText("Routine saved. Continue editing Lower.").assertIsDisplayed()
            compose.onNodeWithTag("routine-placement-editor").assertIsDisplayed()
            capture("saved-in-place")
            backToOutline()
            compose.onNodeWithContentDescription("Close routine editor").performClick()
            val updated = runBlocking { app.routineRepository.exercises.first() }
            fun normalized(row: com.whip.app.domain.RoutineExercise) = row.copy(
                id = 0, uuid = "", routineDayId = 0, createdAtMillis = 0, updatedAtMillis = 0,
            )
            assertEquals(placements.map {
                normalized(if (it.id == lower.id) it.copy(notes = "Updated lower-day cue") else it)
            }, updated.map(::normalized))
            assertEquals(originalSets.map { it.position to it.draft }, runBlocking {
                app.routineRepository.sets.first().map { it.position to it.draft }
            })
            val updatedLowerDay = runBlocking { app.routineRepository.days.first() }.single { it.name == "Lower" }
            compose.onNodeWithText("Start Lower · 1 exercise").performScrollTo().performClick()
            compose.waitUntil(10_000) {
                runBlocking { app.gymRepository.sessions.first() }.any { it.state == WorkoutSessionState.Active }
            }
            val session = runBlocking { app.gymRepository.sessions.first() }.single()
            assertEquals(routine.id, session.sourceRoutineId)
            assertEquals(updatedLowerDay.id, session.sourceRoutineDayId)
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("routine-active-workout-action").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("routine-active-workout-action").performScrollTo().performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("next-set-focus").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("next-set-focus").assertTextContains("Goblet Squat", substring = true)
            capture("workout")
        }
    }

    private fun routines() = runBlocking { app.routineRepository.routines.first() }
    private fun outline(matcher: SemanticsMatcher): SemanticsNodeInteraction {
        compose.onNodeWithTag("routine-selected-exercises").performScrollToNode(matcher)
        return compose.onNode(matcher and hasAnyAncestor(hasTestTag("routine-selected-exercises")))
    }
    private fun add(vararg names: String) {
        outline(hasTestTag("routine-add-exercises")).performClick()
        names.forEach { name ->
            compose.onNodeWithTag("routine-exercise-search").performTextReplacement(name)
            closeSoftKeyboard()
            compose.onNode(hasText(name) and hasAnyAncestor(hasTestTag("routine-exercise-picker-list"))).performClick()
        }
        compose.onNodeWithTag("routine-add-selected").performClick()
    }
    private fun backToOutline() = compose.onNodeWithContentDescription("Back to routine outline").performClick()
    private fun capture(state: String) {
        compose.waitForIdle()
        captureVisualCatalogSurface("gym.routine-authoring.$state")
    }
}
