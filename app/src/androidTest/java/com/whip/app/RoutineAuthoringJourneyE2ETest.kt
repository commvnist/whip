package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.*
import com.whip.app.core.RepPrescriptionScheme
import java.time.Instant
import java.time.ZoneOffset
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
            compose.onNodeWithTag("routine-five-three-one-program-entry").assertDoesNotExist()
            capture("new")
            compose.onNodeWithTag("routine-editor-name").performTextReplacement("Two day strength")
            closeSoftKeyboard()
            outline(hasText("Upper / Lower")).performClick()
            add("Bench Press", "Supported cable row")
            backToOutline()
            capture("upper")
            outline(hasText("Lower · 0")).performClick()
            add("Goblet Squat")
            placement(hasText("Exercise notes")).performTextReplacement("Slow descent; keep the prescribed day.")
            closeSoftKeyboard()
            scenario.recreate()
            compose.onNodeWithTag("routine-placement-editor").assertIsDisplayed()
            placement(hasText("Exercise notes")).assertTextContains("Slow descent; keep the prescribed day.")
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
            placement(hasText("Exercise notes")).assertTextContains(lower.notes)
            placement(hasText("Exercise notes")).performTextReplacement("Updated lower-day cue")
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
            compose.onNodeWithTag("routine-details-${routine.id}").performScrollTo().assertIsDisplayed()
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

    @Test fun authoredPrescriptionSurvivesRecoveryExecutionAndLaterRoutineEdits() {
        runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update {
                AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark, dynamicColor = false)
            }
            app.gymRepository.createExercise(ExerciseDraft(name = "Prescription Bench"))
        }
        fun field(label: String) = placement(hasText(label))
        fun enter(label: String, value: String) {
            field(label).performTextReplacement(value)
            closeSoftKeyboard()
        }
        fun planned() = runBlocking { app.routineRepository.sets.first() }.single().draft
        fun workoutSet() = runBlocking { app.gymRepository.sets.first() }.single()
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithTag("gym-destination-Library").performClick()
            compose.onNodeWithTag("gym-library-Routines").performClick()
            compose.onNodeWithTag("workspace-add-action").performClick()
            compose.onNodeWithTag("routine-editor-name").performTextReplacement("Prescription continuity")
            closeSoftKeyboard()
            add("Prescription Bench")
            compose.onNodeWithTag("routine-placement-editor").assertIsDisplayed()
            compose.onNodeWithText("Load (kg)").assertIsDisplayed()
            compose.onNodeWithText("Reps min").assertIsDisplayed()
            prescriptionCapture("initial")
            placement(hasTestTag("routine-generate-warmups")).assertIsNotEnabled()
            enter("Load (kg)", "40")
            enter("Reps min", "6")
            enter("Reps max", "8")
            placement(hasTestTag("routine-show-advanced")).performClick()
            enter("RPE", "8")
            enter("RIR", "2")
            enter("Rest seconds", "90")
            enter("Tempo", "3010")
            enter("Set note", "Pause before pressing.")
            enter("Exercise notes", "Keep the shoulder blades set.")
            scenario.recreate()
            field("Load (kg)").assertTextContains("40")
            field("Reps min").assertTextContains("6")
            field("Reps max").assertTextContains("8")
            field("Set note").assertTextContains("Pause before pressing.")
            prescriptionCapture("advanced")
            placement(hasTestTag("routine-show-advanced")).performClick()
            placement(hasContentDescription("Duplicate set 1")).performClick()
            placement(hasContentDescription("Delete set 2")).performClick()
            placement(hasTestTag("routine-generate-warmups")).assertIsEnabled()
            prescriptionCapture("tools")
            placement(hasTestTag("routine-generate-warmups")).performClick()
            placement(hasContentDescription("Delete set 4")).assertIsDisplayed()
            placement(hasText("Set 1") and hasClickAction()).performClick()
            repeat(3) {
                placement(hasContentDescription("Delete set 1")).performClick()
            }
            compose.onNodeWithTag("routine-builder-save").performClick()
            compose.waitUntil(10_000) { routines().any { it.name == "Prescription continuity" } }
            val routine = routines().single()
            val authored = planned()
            assertEquals(WorkoutSetDraft(
                weight = 40.0, reps = 6, repsMax = 8, planned = true,
                rpe = 8.0, rir = 2.0, restSeconds = 90, tempo = "3010", note = "Pause before pressing.",
            ), authored)
            compose.onNodeWithContentDescription("Edit routine Prescription continuity").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Edit routine exercise Prescription Bench").performScrollTo().performClick()
            field("Load (kg)").assertTextContains("40")
            field("Reps max").assertTextContains("8")
            prescriptionCapture("reopened")
            compose.onNodeWithTag("routine-builder-save").assertIsNotEnabled()
            enter("Exercise notes", "Updated cue; preserve every Set value.")
            compose.onNodeWithTag("routine-builder-save").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Routine saved. Continue editing Day A.").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Routine saved. Continue editing Day A.").assertIsDisplayed()
            assertEquals(authored, planned())
            backToOutline()
            compose.onNodeWithContentDescription("Close routine editor").performClick()
            compose.onNodeWithText("Start Day A · 1 exercise").performScrollTo().performClick()
            compose.waitUntil(10_000) {
                runBlocking { app.gymRepository.sessions.first() }.any { it.state == WorkoutSessionState.Active }
            }
            val session = runBlocking { app.gymRepository.sessions.first() }.single()
            assertEquals(routine.id, session.sourceRoutineId)
            compose.onNodeWithTag("routine-active-workout-action").performScrollTo().performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("next-set-focus").fetchSemanticsNodes().isNotEmpty() }
            val instantiated = workoutSet()
            assertEquals(40.0, instantiated.prescribedEnteredWeight!!, 0.0)
            assertEquals(6, instantiated.prescribedRepetitions)
            assertEquals(8, instantiated.prescribedRepetitionsMax)
            assertEquals(8.0, instantiated.prescribedRpe!!, 0.0)
            assertEquals(2.0, instantiated.prescribedRir!!, 0.0)
            assertEquals(90, instantiated.restSeconds)
            assertEquals("3010", instantiated.tempo)
            assertEquals("Pause before pressing.", instantiated.note)
            val setId = instantiated.id
            prescriptionCapture("workout")
            compose.onNodeWithTag("quick-set-reps-$setId").performScrollTo().performTextReplacement("7")
            closeSoftKeyboard()
            compose.onNodeWithTag("quick-set-save-next-$setId").performScrollTo().performClick()
            compose.waitUntil(10_000) { workoutSet().completed }
            val performed = workoutSet()
            assertEquals(40.0, performed.enteredWeight!!, 0.0)
            assertEquals(7, performed.repetitions)
            assertEquals(instantiated.prescribedRepetitionsMax, performed.prescribedRepetitionsMax)
            compose.onNodeWithTag("active-workout-list").performScrollToNode(hasTestTag("active-workout-finish"))
            compose.onNodeWithTag("active-workout-finish").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("finish-workout-confirm").fetchSemanticsNodes().isNotEmpty() ||
                    runBlocking { app.gymRepository.sessions.first().single().state == WorkoutSessionState.Finished }
            }
            if (compose.onAllNodesWithTag("finish-workout-confirm").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithTag("finish-workout-confirm").performClick()
            }
            compose.waitUntil(10_000) {
                runBlocking { app.gymRepository.sessions.first().single().state == WorkoutSessionState.Finished }
            }
            compose.onNodeWithTag("gym-destination-Library").performClick()
            compose.onNodeWithTag("gym-library-Routines").performClick()
            compose.onNodeWithContentDescription("Edit routine Prescription continuity").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Edit routine exercise Prescription Bench").performScrollTo().performClick()
            enter("Load (kg)", "45")
            compose.onNodeWithTag("routine-builder-save").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Routine saved. Continue editing Day A.").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Routine saved. Continue editing Day A.").assertIsDisplayed()
            assertEquals(authored.copy(weight = 45.0), planned())
            backToOutline()
            compose.onNodeWithContentDescription("Close routine editor").performClick()
            compose.onNodeWithTag("gym-destination-History").performClick()
            val historyToggle = compose.onNodeWithTag("history-workout-toggle-${session.id}").performScrollTo()
            val expanded = historyToggle.fetchSemanticsNode().config[
                androidx.compose.ui.semantics.SemanticsProperties.StateDescription] == "Expanded"
            if (!expanded) historyToggle.performClick()
            compose.onNodeWithTag("history-set-card-$setId").performScrollTo()
            compose.onNodeWithTag("history-set-performed-$setId").assertTextContains("40 kg × 7 reps")
            prescriptionCapture("history")
            assertEquals(performed, workoutSet())
        }
    }


    @Test fun transformedMiddlePhasePersistsReopensAndInstantiatesWithoutChangingOtherPhases() {
        val common = WorkoutSetDraft(weight = 20.0, reps = 10, planned = true, note = "Every phase")
        val first = WorkoutSetDraft(weight = 100.0, reps = 3, planned = true, routinePhaseIndex = 0, note = "Keep first")
        val middle = WorkoutSetDraft(weight = 80.0, reps = 5, planned = true, routinePhaseIndex = 1)
        val last = WorkoutSetDraft(weight = 120.0, reps = 1, planned = true, routinePhaseIndex = 2, note = "Keep last")
        val routineId = runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { AppSettings(setupCompleted = true, dynamicColor = false,
                repPrescriptionSchemes = listOf(RepPrescriptionScheme("middle-volume", "Middle Volume", 2, 8))) }
            val exercise = app.gymRepository.createExercise(ExerciseDraft("Phase isolation bench"))
            app.routineRepository.createRoutine(RoutineDraft("Middle phase continuity",
                program = RoutineProgramDraft(kind = RoutineProgramKind.Custom, phaseCount = 3,
                    phaseLabels = listOf("First", "Middle", "Last")),
                days = listOf(RoutineDayDraft("Upper", listOf(RoutineExerciseDraft(exercise,
                    plannedSets = listOf(common, first, middle, last))))))).also {
                app.routineRepository.setRoutineProgramPosition(it, phaseIndex = 1, dayPosition = 0, cycle = 1)
            }
        }
        fun stored() = runBlocking { app.routineRepository.sets.first().sortedBy { it.position }.map { it.draft } }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithTag("gym-destination-Library").performClick()
            compose.onNodeWithTag("gym-library-Routines").performClick()
            compose.onNodeWithContentDescription("Edit routine Middle phase continuity").performScrollTo().performClick()
            outline(hasContentDescription("Edit routine exercise Phase isolation bench")).performClick()
            placement(hasTestTag("routine-program-phase-1")).performClick()
            placement(hasText("Middle Volume · 2 × 8")).performClick()
            placement(hasTestTag("routine-generate-warmups")).performClick()
            captureVisualCatalogSurface("overhaul.gym.phase-room.transformed")
            compose.onNodeWithTag("routine-builder-save").performClick()
            compose.waitUntil(10_000) { stored().size == 8 &&
                compose.onAllNodesWithTag("routine-saved-in-place").fetchSemanticsNodes().isNotEmpty() }
            val saved = stored()
            assertEquals(listOf(common, first, last), saved.filter { it.routinePhaseIndex != 1 })
            val transformed = saved.filter { it.routinePhaseIndex == 1 }
            assertEquals(listOf(30.0, 47.5, 62.5, 80.0, null), transformed.map { it.weight })
            assertEquals(listOf(8, 5, 3, 8, 8), transformed.map { it.reps })
            assertEquals(List(3) { WorkoutSetClassification.WarmUp } + List(2) { WorkoutSetClassification.Working },
                transformed.map { it.classification })
            backToOutline()
            compose.onNodeWithContentDescription("Close routine editor").performClick()
            scenario.recreate()
            compose.onNodeWithContentDescription("Edit routine Middle phase continuity").performScrollTo().performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("routine-selected-exercises").fetchSemanticsNodes().isNotEmpty() ||
                    compose.onAllNodesWithTag("routine-placement-editor").fetchSemanticsNodes().isNotEmpty()
            }
            captureVisualCatalogSurface("overhaul.gym.phase-room.reopen-diagnostic")
            if (compose.onAllNodesWithTag("routine-placement-editor").fetchSemanticsNodes().isEmpty()) {
                outline(hasContentDescription("Edit routine exercise Phase isolation bench")).performClick()
            }
            placement(hasText("Phase isolation bench")).assertIsDisplayed()
            placement(hasTestTag("routine-program-phase-1")).performClick()
            assertEquals(saved, stored())
            compose.onNodeWithTag("routine-builder-save").assertIsNotEnabled()
            captureVisualCatalogSurface("overhaul.gym.phase-room.reopened")
            backToOutline()
            compose.onNodeWithContentDescription("Close routine editor").performClick()
            compose.onNodeWithTag("routine-start-next-$routineId").performScrollTo().performClick()
            compose.waitUntil(10_000) {
                runBlocking { app.gymRepository.sessions.first().any { it.state == WorkoutSessionState.Active } }
            }
            val session = runBlocking { app.gymRepository.sessions.first() }.single()
            assertEquals(routineId, session.sourceRoutineId)
            assertEquals(1, session.sourceRoutinePhaseIndex)
            val instantiated = runBlocking { app.gymRepository.sets.first() }.sortedBy { it.position }
            val expected = saved.filter { it.routinePhaseIndex == null || it.routinePhaseIndex == 1 }
            assertEquals(expected.map { it.weight }, instantiated.map { it.prescribedEnteredWeight })
            assertEquals(expected.map { it.reps }, instantiated.map { it.prescribedRepetitions })
            assertEquals(expected.map { it.classification }, instantiated.map { it.classification })
            assertEquals(expected.map { it.note }, instantiated.map { it.note })
            assertEquals(saved, stored())
            compose.onNodeWithTag("routine-active-workout-action").performScrollTo().performClick()
            compose.onNodeWithTag("active-workout-program-context").assertTextEquals("Phased Routine · Cycle 1 · Middle · Day 1")
            scenario.recreate()
            assertEquals(session.id, runBlocking { app.gymRepository.sessions.first() }.single().id)
            assertEquals(instantiated, runBlocking { app.gymRepository.sets.first() }.sortedBy { it.position })
            captureVisualCatalogSurface("overhaul.gym.phase-room.instantiated")
        }
    }

    @Test fun olderThanFiftyWorkoutImportsIntoSelectedDayAndPersistsAcrossReopen() {
        val original = WorkoutSetDraft(weight = 73.0, reps = 9, rpe = 8.0, restSeconds = 75,
            tempo = "3010", note = "Older source prescription", completed = true)
        runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { AppSettings(setupCompleted = true, dynamicColor = false) }
            val exercise = app.gymRepository.createExercise(ExerciseDraft("Older source row"))
            repeat(56) { index ->
                val started = Instant.now().minusSeconds((56L - index) * 86_400)
                val session = app.gymRepository.startWorkout(
                    if (index == 0) "Oldest selected source" else "Recent source $index",
                    startedAt = started, localDate = started.atZone(ZoneOffset.UTC).toLocalDate())
                if (index == 0) {
                    val placement = app.gymRepository.addExerciseToWorkout(session, exercise)
                    app.gymRepository.addSet(placement, original)
                }
                app.gymRepository.finishWorkout(session)
                // Pagination starts from real settled history, not an unrelated 56-timer startup burst.
                val finished = app.gymRepository.sessions.first().single { it.id == session }
                app.restTimerScheduler.cancel(session)
                app.gymRepository.acknowledgeRestTimerCleanup(session, finished.restTimerRevision)
                assertTrue(!app.gymRepository.sessions.first().single { it.id == session }.restTimerCleanupPending)
            }
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Gym tab").performClick()
            // App startup acknowledges completed timers; import must preserve the settled history exactly.
            val cleanupStartedAt = android.os.SystemClock.elapsedRealtime()
            var cleanupLastLoggedAt = -1_000L
            compose.waitUntil(10_000) {
                val sessions = runBlocking { app.gymRepository.sessions.first() }
                val pending = sessions.filter { it.restTimerCleanupPending }
                val elapsed = android.os.SystemClock.elapsedRealtime() - cleanupStartedAt
                if (elapsed - cleanupLastLoggedAt >= 1_000L || pending.isEmpty()) {
                    android.util.Log.i(
                        "WhipRoutineCleanupQA",
                        "elapsedMs=$elapsed sessions=${sessions.size} pending=${pending.size} " +
                            "id:revision=${pending.joinToString { "${it.id}:${it.restTimerRevision}" }}",
                    )
                    cleanupLastLoggedAt = elapsed
                }
                pending.isEmpty()
            }
            val historyBefore = runBlocking { app.gymRepository.sessions.first() }
            compose.onNodeWithTag("gym-destination-Library").performClick()
            compose.onNodeWithTag("gym-library-Routines").performClick()
            compose.onNodeWithTag("workspace-add-action").performClick()
            compose.onNodeWithTag("routine-editor-name").performTextReplacement("Older selected day import")
            closeSoftKeyboard()
            outline(hasText("Upper / Lower")).performClick()
            outline(hasText("Lower · 0")).performClick()
            outline(hasText("Add from a Previous Workout")).performClick()
            compose.onNodeWithText("Oldest selected source").assertDoesNotExist()
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("routine-workout-picker-more"))
            compose.onNodeWithTag("routine-workout-picker-more").assertIsDisplayed()
            captureVisualCatalogSurface("overhaul.gym.older-picker.first-fifty")
            compose.onNodeWithTag("routine-workout-picker-more").performClick()
            scenario.recreate()
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Oldest selected source"))
            compose.onNodeWithText("Oldest selected source").performClick()
            backToOutline()
            outline(hasText("Lower · 1")).assertIsSelected()
            captureVisualCatalogSurface("overhaul.gym.older-picker.selected-day")
            compose.onNodeWithTag("routine-builder-save").performClick()
            compose.waitUntil(10_000) { routines().any { it.name == "Older selected day import" } }
            val routine = routines().single()
            val days = runBlocking { app.routineRepository.days.first() }.sortedBy { it.position }
            val placements = runBlocking { app.routineRepository.exercises.first() }
            assertEquals(listOf("Upper", "Lower"), days.map { it.name })
            assertTrue(placements.none { it.routineDayId == days[0].id })
            assertEquals(days[1].id, placements.single().routineDayId)
            val prescription = runBlocking { app.routineRepository.sets.first() }.single().draft
            assertEquals(original.copy(completed = false, planned = true, repsMax = 9), prescription)
            scenario.recreate()
            compose.onNodeWithContentDescription("Edit routine Older selected day import").performScrollTo().performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("routine-selected-exercises").fetchSemanticsNodes().isNotEmpty() ||
                    compose.onAllNodesWithTag("routine-placement-editor").fetchSemanticsNodes().isNotEmpty()
            }
            captureVisualCatalogSurface("overhaul.gym.older-picker.reopen-diagnostic")
            outline(hasText("Lower · 1")).performClick()
            outline(hasContentDescription("Edit routine exercise Older source row")).performClick()
            placement(hasText("Load (kg)")).assertTextContains("73")
            placement(hasText("Reps min")).assertTextContains("9")
            assertEquals(prescription, runBlocking { app.routineRepository.sets.first() }.single().draft)
            assertEquals(historyBefore, runBlocking { app.gymRepository.sessions.first() })
            captureVisualCatalogSurface("overhaul.gym.older-picker.reopened")
            assertEquals(routine.id, runBlocking { app.routineRepository.routines.first() }.single().id)
        }
    }

    private fun placement(matcher: SemanticsMatcher): SemanticsNodeInteraction {
        compose.onNodeWithTag("routine-placement-editor").performScrollToNode(matcher)
        return compose.onNode(matcher and hasAnyAncestor(hasTestTag("routine-placement-editor")))
    }

    private fun prescriptionCapture(state: String) {
        compose.waitForIdle()
        captureVisualCatalogSurface("gym.routine-prescription.$state")
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
