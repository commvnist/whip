package com.whip.app

import android.content.Intent
import android.Manifest
import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.platform.app.InstrumentationRegistry
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.domain.WorkoutSetDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Real execution lane: authored rest, manual/automatic starts, override recovery and history. */
class WorkoutRestJourneyE2ETest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun restFollowsTheNextSetAndOverridesCanReturnToPrescriptions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(app.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        val ids = runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update {
                AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark, dynamicColor = false,
                    defaultRestSeconds = 120, restTimerAutoStart = true, notificationPermissionRequested = true)
            }
            val exercise = app.gymRepository.createExercise(ExerciseDraft(name = "Rest continuity bench", defaultRestSeconds = 75))
            val session = app.gymRepository.startWorkout("Prescription rest")
            val placement = app.gymRepository.addExerciseToWorkout(session, exercise)
            listOf(90, 45, 0, null).map { rest ->
                app.gymRepository.addSet(placement, WorkoutSetDraft(weight = 40.0, reps = 6, planned = true, restSeconds = rest))
            }
        }
        fun session() = runBlocking { app.gymRepository.sessions.first() }.single()
        fun sets() = runBlocking { app.gymRepository.sets.first() }.associateBy { it.id }
        val original = sets()
        fun action(label: String) = compose.onNodeWithContentDescription(label)
        fun ready(text: String, source: String) {
            compose.onNodeWithText("Rest · $text").assertIsDisplayed()
            compose.onNodeWithText(source).assertIsDisplayed()
        }
        fun stop() {
            action("Stop rest timer").performClick()
            compose.waitUntil(10_000) { session().restTimerDeadlineMillis == null }
            action("Start rest timer").assertIsDisplayed()
        }
        fun start(seconds: Int) {
            val revision = session().restTimerRevision
            action("Start rest timer").performClick()
            compose.waitUntil(10_000) { session().restTimerRevision > revision && session().restTimerDurationSeconds == seconds }
            action("Stop rest timer").assertIsDisplayed()
        }
        fun complete(id: Long) {
            compose.onNodeWithTag("quick-set-reps-$id").performScrollTo().performTextReplacement("7")
            closeSoftKeyboard()
            compose.onNodeWithTag("quick-set-save-next-$id").performScrollTo().performClick()
            compose.waitUntil(10_000) { sets().getValue(id).completed }
            compose.waitForIdle()
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Gym tab").performClick()
            ready("1:30", "Next Set rest")
            capture("ready")
            start(90)
            val running = session()
            scenario.recreate()
            action("Stop rest timer").assertIsDisplayed()
            assertEquals(running.restTimerDeadlineMillis, session().restTimerDeadlineMillis)
            assertEquals(running.restTimerRevision, session().restTimerRevision)
            stop()
            ready("1:30", "Next Set rest")
            complete(ids[0])
            assertEquals(90, session().restTimerDurationSeconds)
            capture("automatic")
            stop()
            ready("0:45", "Next Set rest")
            action("Adjust rest time for this workout").performClick()
            compose.onNodeWithText("1:00").performClick()
            compose.onNodeWithText("Use for This Workout").performClick()
            ready("1:00", "Workout override")
            scenario.recreate()
            ready("1:00", "Workout override")
            start(60)
            stop()
            action("Adjust rest time for this workout").performClick()
            compose.onNodeWithText("Follow Set rest").assertIsDisplayed()
            capture("override")
            compose.onNodeWithText("Cancel").performClick()
            complete(ids[1])
            assertEquals(60, session().restTimerDurationSeconds)
            stop()
            action("Adjust rest time for this workout").performClick()
            compose.onNodeWithText("Follow Set rest").performClick()
            ready("0:00", "No rest for next Set")
            action("Start rest timer").assertIsNotEnabled()
            scenario.recreate()
            ready("0:00", "No rest for next Set")
            capture("no-rest")
            complete(ids[2])
            assertNull(session().restTimerDeadlineMillis)
            ready("1:15", "Exercise default")
            start(75)
            stop()
            complete(ids[3])
            assertEquals(75, session().restTimerDurationSeconds)
            stop()
            ready("2:00", "App default")
            start(120)
            stop()
            ids.forEach { id ->
                assertEquals(original.getValue(id).restSeconds, sets().getValue(id).restSeconds)
                assertEquals(7, sets().getValue(id).repetitions)
            }
            assertEquals(120, runBlocking { app.settingsRepository.current() }.defaultRestSeconds)
            assertEquals(75, runBlocking { app.gymRepository.exercises.first() }.single().defaultRestSeconds)
            val performed = sets()
            compose.onNodeWithTag("active-workout-list").performScrollToNode(hasTestTag("active-workout-finish"))
            compose.onNodeWithTag("active-workout-finish").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("finish-workout-confirm").fetchSemanticsNodes().isNotEmpty() || session().state == WorkoutSessionState.Finished
            }
            if (compose.onAllNodesWithTag("finish-workout-confirm").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithTag("finish-workout-confirm").performClick()
            }
            compose.waitUntil(10_000) { session().state == WorkoutSessionState.Finished }
            assertNull(session().restTimerDeadlineMillis)
            compose.onNodeWithTag("gym-destination-History").performClick()
            compose.onNodeWithTag("history-workout-toggle-${session().id}").performScrollTo().performClick()
            compose.onNodeWithTag("history-set-card-${ids[0]}").performScrollTo()
            compose.onNodeWithTag("history-set-performed-${ids[0]}").assertTextContains("40 kg × 7 reps")
            capture("history")
            assertEquals(performed, sets())
        }
    }

    private fun capture(state: String) {
        compose.waitForIdle()
        captureVisualCatalogSurface("gym.rest-journey.$state")
    }
}
