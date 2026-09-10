package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ExecutionItemBuilderJourneyE2ETest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun skippedSetKeepsItsMeaningThenRestoresLogsAndReopensInHistory() {
        val setup = runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark, dynamicColor = false) }
            val exercise = app.gymRepository.createExercise(ExerciseDraft(name = "Builder Bench Press"))
            val session = app.gymRepository.startWorkout("Set continuity")
            val placement = app.gymRepository.addExerciseToWorkout(session, exercise)
            val skipped = app.gymRepository.addSet(placement, WorkoutSetDraft(weight = 40.0, reps = 5, workSection = RoutineWorkSection.Optional))
            app.gymRepository.addSet(placement, WorkoutSetDraft(weight = 42.5, reps = 5, completed = true))
            app.gymRepository.deleteSet(skipped, WorkoutSetRemovalReason.Skipped)
            Triple(session, skipped, app.gymRepository.sets.first().associateBy { it.id })
        }
        val (session, skipped, original) = setup
        fun saved() = runBlocking { app.gymRepository.sets.first().associateBy { it.id } }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithTag("workout-set-card-$skipped").performScrollTo()
            captureVisualCatalogSurface("gym.execution-builder.skipped")
            compose.onNode(hasText("Skipped", substring = true) and hasAnyAncestor(hasTestTag("workout-set-card-$skipped")))
                .assertIsDisplayed()
            scenario.recreate()
            compose.onNode(hasText("Undo") and hasAnyAncestor(hasTestTag("workout-set-card-$skipped")))
                .performScrollTo().performClick()
            compose.waitUntil(10_000) { saved().getValue(skipped).deletedAtMillis == null }
            assertEquals(original.getValue(skipped).uuid, saved().getValue(skipped).uuid)
            compose.onNodeWithTag("quick-set-load-$skipped").performScrollTo().performTextReplacement("45")
            compose.onNodeWithTag("quick-set-reps-$skipped").performScrollTo().performTextReplacement("6")
            compose.onNodeWithTag("quick-set-reps-$skipped").assertTextContains("6")
            scenario.recreate()
            compose.onNodeWithTag("quick-set-reps-$skipped").assertTextContains("6")
            closeSoftKeyboard()
            compose.onNodeWithTag("quick-set-save-next-$skipped").performScrollTo()
            // Reproduce a normal scroll position with only the toolbar's bottom edge exposed.
            val target = with(compose.density) { androidx.compose.ui.unit.Dp(4f).toPx() }
            for (attempt in 0 until 30) {
                val action = compose.onNodeWithTag("add-exercise-to-active-workout").fetchSemanticsNode().boundsInRoot
                val lane = compose.onNodeWithTag("workout-execution-lane").fetchSemanticsNode().boundsInRoot
                val delta = action.bottom - lane.bottom - target
                if (kotlin.math.abs(delta) <= 1f) break
                compose.onNodeWithTag("active-workout-list").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy) {
                    it(0f, delta.coerceIn(-target * 6, target * 6))
                }
                compose.waitForIdle()
            }
            val exposed = compose.onNodeWithTag("add-exercise-to-active-workout").fetchSemanticsNode().boundsInRoot.bottom -
                compose.onNodeWithTag("workout-execution-lane").fetchSemanticsNode().boundsInRoot.bottom
            assertTrue("Exercise toolbar must be partially exposed under the sticky lane: $exposed px", kotlin.math.abs(exposed - target) <= 1f)
            captureVisualCatalogSurface("gym.execution-builder.restored")
            compose.onNodeWithTag("quick-set-save-next-$skipped").performClick()
            compose.waitUntil(10_000) { saved().getValue(skipped).completed }
            val committed = saved().getValue(skipped)
            assertEquals(45.0, committed.enteredWeight!!, 0.0)
            assertEquals(6, committed.repetitions)
            assertNull(committed.removalReason)
            original.filterKeys { it != skipped }.forEach { (id, set) -> assertEquals(set, saved().getValue(id)) }
            compose.onNodeWithTag("active-workout-list").performScrollToNode(hasTestTag("active-workout-finish"))
            compose.onNodeWithTag("active-workout-finish").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("finish-workout-confirm").fetchSemanticsNodes().isNotEmpty() ||
                    runBlocking { app.gymRepository.sessions.first().single { it.id == session }.state == WorkoutSessionState.Finished }
            }
            if (compose.onAllNodesWithTag("finish-workout-confirm").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithTag("finish-workout-confirm").performClick()
            }
            compose.waitUntil(10_000) {
                runBlocking { app.gymRepository.sessions.first().single { it.id == session }.state == WorkoutSessionState.Finished }
            }
            compose.onNodeWithTag("gym-destination-History").performClick()
            compose.onNodeWithTag("history-workout-toggle-$session").performScrollTo().performClick()
            compose.onNodeWithTag("history-set-card-$skipped").performScrollTo()
            compose.onNodeWithTag("history-set-performed-$skipped").assertTextContains("45 kg × 6 reps")
            compose.onNodeWithTag("history-set-status-$skipped").assertTextContains("Working · Performed")
            captureVisualCatalogSurface("gym.execution-builder.history")
            assertEquals(committed, saved().getValue(skipped))
        }
    }

}
