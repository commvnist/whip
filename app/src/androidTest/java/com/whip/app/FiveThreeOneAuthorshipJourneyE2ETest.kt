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
import com.whip.app.domain.RoutineWorkSection
import com.whip.app.domain.RoutineSupplementalScheme
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

    @Test fun editingSavedSupplementalWorkUpdatesEveryTrainingWeekAndNextWorkout() {
        runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update {
                AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark, dynamicColor = false, restTimerAutoStart = false)
            }
            listOf("Flat Barbell Bench Press", "Zercher Deadlift").forEach {
                app.gymRepository.createExercise(ExerciseDraft(name = it))
            }
        }
        val benchId = runBlocking { app.gymRepository.exercises.first() }.single { it.name == "Flat Barbell Bench Press" }.id
        fun openRoutines() {
            compose.onNodeWithTag("gym-destination-Library").performClick()
            compose.onNodeWithTag("gym-library-Routines").performClick()
        }
        fun savedSets(exerciseId: Long) = runBlocking {
            val placement = app.routineRepository.exercises.first().single { it.exerciseId == exerciseId }
            app.routineRepository.sets.first().filter { it.routineExerciseId == placement.id }.map { it.draft }
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Gym tab").performClick()
            openRoutines()
            compose.onNodeWithTag("workspace-add-action").performClick()
            compose.onNode(hasText("Set Up 5/3/1") and hasClickAction() and
                hasAnyAncestor(hasTestTag("routine-five-three-one-program-entry"))).performClick()
            compose.onNodeWithTag("five-three-one-layout-Custom").performScrollTo().performClick()
            listOf("100", "200").forEachIndexed { index, tm ->
                compose.onNodeWithTag("five-three-one-training-max-Custom-$index").performScrollTo().performTextReplacement(tm)
                closeSoftKeyboard()
            }
            compose.onNodeWithTag("five-three-one-program-create").performClick()
            compose.onNodeWithTag("routine-builder-save").performClick()
            compose.waitUntil(10_000) { runBlocking { app.routineRepository.routines.first() }.size == 1 }
            val routine = runBlocking { app.routineRepository.routines.first() }.single()
            val otherId = runBlocking { app.gymRepository.exercises.first() }.single { it.id != benchId }.id
            val otherBefore = savedSets(otherId)
            val before = savedSets(benchId)
            // Edit during week two, after both original week-one workouts have become history.
            repeat(2) {
                compose.onNodeWithTag("routine-start-next-${routine.id}").performScrollTo().performClick()
                compose.onNodeWithTag("routine-active-workout-action").performScrollTo().performClick()
                val session = runBlocking { app.gymRepository.sessions.first() }.single { it.state == WorkoutSessionState.Active }
                val placement = runBlocking { app.gymRepository.workoutExercises.first() }.single { it.sessionId == session.id }
                val sets = runBlocking { app.gymRepository.sets.first() }.filter { it.workoutExerciseId == placement.id }.sortedBy { it.position }
                for (set in sets) {
                    compose.onNodeWithTag("quick-set-save-next-${set.id}").performScrollTo().performClick()
                    compose.waitUntil(10_000) { runBlocking { app.gymRepository.sets.first() }.single { it.id == set.id }.completed }
                }
                compose.onNodeWithTag("active-workout-list").performScrollToNode(hasTestTag("active-workout-finish"))
                compose.onNodeWithTag("active-workout-finish").performClick()
                compose.waitUntil(10_000) {
                    runBlocking { app.gymRepository.sessions.first() }.single { it.id == session.id }.state == WorkoutSessionState.Finished ||
                        compose.onAllNodesWithTag("finish-workout-confirm").fetchSemanticsNodes().isNotEmpty()
                }
                if (compose.onAllNodesWithTag("finish-workout-confirm").fetchSemanticsNodes().isNotEmpty()) {
                    compose.onNodeWithTag("finish-workout-confirm").performClick()
                }
                compose.waitUntil(10_000) { runBlocking { app.gymRepository.sessions.first() }.single { it.id == session.id }.state == WorkoutSessionState.Finished }
                openRoutines()
            }
            val performed = runBlocking { app.gymRepository.sets.first() }
            val position = runBlocking { app.routineRepository.routines.first() }.single()
            assertEquals(1, position.currentProgramPhaseIndex)
            compose.onNodeWithContentDescription("Edit routine ${routine.name}").performScrollTo().performClick()
            compose.onNodeWithTag("routine-open-program-structure").performScrollTo().performClick()
            compose.onNodeWithTag("routine-program-structure-page").performScrollToNode(hasTestTag("routine-program-policy-scope-$benchId"))
            compose.onNodeWithTag("routine-program-policy-scope-$benchId").performClick()
            compose.onNodeWithTag("routine-program-structure-page").performScrollToNode(hasTestTag("routine-program-phase-supplemental-1"))
            compose.onNodeWithTag("routine-program-phase-supplemental-1").performClick()
            compose.onNodeWithContentDescription("Supplemental work option: BBB · 5 × 10").performClick()
            scenario.recreate()
            compose.onNodeWithTag("routine-program-structure-page").performScrollToNode(hasTestTag("routine-program-supplemental-scope-training"))
            compose.onNodeWithTag("routine-program-supplemental-scope-training").assertIsSelected()
            compose.onNodeWithTag("routine-program-supplemental-scope-phase").performClick()
            scenario.recreate()
            compose.onNodeWithTag("routine-program-structure-page").performScrollToNode(hasTestTag("routine-program-supplemental-scope-phase"))
            compose.onNodeWithTag("routine-program-supplemental-scope-phase").assertIsSelected()
            captureVisualCatalogSurface("gym.531-edit.supplemental-scope")
            compose.onNodeWithTag("routine-program-supplemental-scope-training").performClick()
            compose.onNodeWithContentDescription("Back to routine outline").performClick()
            compose.onNodeWithTag("routine-builder-save").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("routine-saved-in-place").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("routine-saved-in-place").assertExists()
            compose.onNodeWithContentDescription("Close routine editor").performClick()
            for (phase in 0..2) {
                val supplemental = savedSets(benchId).filter { it.routinePhaseIndex == phase && it.workSection == RoutineWorkSection.Supplemental }
                assertEquals(5, supplemental.size)
                assertTrue("Week ${phase + 1} must use the saved BBB choice", supplemental.all {
                    it.reps == 10 && it.loadPercentage == 50.0 && it.supplementalScheme == RoutineSupplementalScheme.BoringButBig
                })
            }
            assertEquals(otherBefore, savedSets(otherId))
            assertEquals(before.filter { it.workSection != RoutineWorkSection.Supplemental },
                savedSets(benchId).filter { it.workSection != RoutineWorkSection.Supplemental })
            val savedRoutine = runBlocking { app.routineRepository.routines.first() }.single()
            assertEquals(position.currentProgramPhaseIndex, savedRoutine.currentProgramPhaseIndex)
            assertEquals(position.nextProgramDayPosition, savedRoutine.nextProgramDayPosition)
            assertEquals(position.currentProgramCycle, savedRoutine.currentProgramCycle)
            assertEquals(performed, runBlocking { app.gymRepository.sets.first() })
            compose.onNodeWithContentDescription("Edit routine ${routine.name}").performScrollTo().performClick()
            scenario.recreate()
            compose.onNodeWithTag("routine-open-program-structure").performScrollTo().performClick()
            compose.onNodeWithTag("routine-program-structure-page").performScrollToNode(hasTestTag("routine-program-policy-scope-$benchId"))
            compose.onNodeWithTag("routine-program-policy-scope-$benchId").performClick()
            compose.onNodeWithTag("routine-program-structure-page").performScrollToNode(hasTestTag("routine-program-phase-supplemental-1"))
            compose.onNodeWithContentDescription("Supplemental work: BBB · 5 × 10").assertIsDisplayed()
            captureVisualCatalogSurface("gym.531-edit.saved-supplemental")
            compose.onNodeWithContentDescription("Back to routine outline").performClick()
            compose.onNodeWithContentDescription("Close routine editor").performClick()
            compose.onNodeWithTag("routine-start-next-${routine.id}").performScrollTo().performClick()
            compose.onNodeWithTag("routine-active-workout-action").performScrollTo().performClick()
            val session = runBlocking { app.gymRepository.sessions.first() }.single { it.state == WorkoutSessionState.Active }
            val placement = runBlocking { app.gymRepository.workoutExercises.first() }.single { it.sessionId == session.id }
            val active = runBlocking { app.gymRepository.sets.first() }.filter { it.workoutExerciseId == placement.id }
            val supplemental = active.filter { it.workSectionSnapshot == RoutineWorkSection.Supplemental }
            assertEquals(5, supplemental.size)
            assertTrue(supplemental.all { it.enteredWeight == 50.0 && it.repetitions == 10 })
            compose.onNodeWithTag("next-set-focus").assertTextContains("Flat Barbell Bench Press", substring = true)
            compose.onNodeWithTag("active-workout-list").performScrollToNode(hasTestTag("workout-set-card-${supplemental.first().id}"))
            compose.onNodeWithTag("workout-set-card-${supplemental.first().id}").assertTextContains("50 kg", substring = true)
            captureVisualCatalogSurface("gym.531-edit.updated-workout")
            assertEquals(performed, runBlocking { app.gymRepository.sets.first() }.filter { it.workoutExerciseId != placement.id })
        }
    }

    @Test fun customDaysKeepIndependentSupplementalWorkThroughSaveAndBothWorkouts() {
        val names = listOf("Flat Barbell Bench Press", "Zercher Deadlift")
        runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update {
                AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark, dynamicColor = false, restTimerAutoStart = false)
            }
            names.forEach { app.gymRepository.createExercise(ExerciseDraft(name = it)) }
        }
        val exercises = runBlocking { app.gymRepository.exercises.first() }.associateBy { it.name }
        val benchId = exercises.getValue(names[0]).id
        val zercherId = exercises.getValue(names[1]).id
        fun enter(tag: String, value: String) {
            compose.onNodeWithTag(tag).performScrollTo().performTextReplacement(value)
            closeSoftKeyboard()
        }
        fun assertSupplement(exerciseId: Long, label: String) {
            compose.onNode(hasContentDescription("Supplemental Work: $label") and
                hasAnyAncestor(hasTestTag("five-three-one-supplement-$exerciseId")))
                .performScrollTo().assertIsDisplayed()
        }
        fun openRoutines() {
            compose.onNodeWithTag("gym-destination-Library").performClick()
            compose.onNodeWithTag("gym-library-Routines").performClick()
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Gym tab").performClick()
            openRoutines()
            compose.onNodeWithTag("workspace-add-action").performClick()
            compose.onNode(hasText("Set Up 5/3/1") and hasClickAction() and
                hasAnyAncestor(hasTestTag("routine-five-three-one-program-entry"))).performClick()
            compose.onNodeWithTag("five-three-one-layout-Custom").performScrollTo().performClick()
            enter("five-three-one-training-max-Custom-0", "100")
            enter("five-three-one-training-max-Custom-1", "200")
            compose.onNodeWithTag("five-three-one-supplement-$benchId").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Supplemental Work option: BBB · 5 × 10").performClick()
            enter("five-three-one-bbb-percent-$benchId", "60")
            assertSupplement(benchId, "BBB · 5 × 10")
            captureVisualCatalogSurface("gym.531-supplemental.bench-setup")
            assertSupplement(zercherId, "FSL · 5 × 5")
            // Authored supplemental choices and percentage move with identity, then restore the requested day order.
            compose.onNodeWithContentDescription("Move ${names[0]} later").performScrollTo().performClick()
            scenario.recreate()
            assertSupplement(benchId, "BBB · 5 × 10")
            compose.onNodeWithTag("five-three-one-bbb-percent-$benchId").assertTextContains("60")
            compose.onNodeWithContentDescription("Move ${names[0]} earlier").performScrollTo().performClick()
            compose.onNodeWithTag("five-three-one-program-preview-supplement-$benchId").performScrollTo()
                .assertTextEquals("${names[0]} · BBB · 5 × 10 · ${names[0]} · 60 kg")
            compose.onNodeWithTag("five-three-one-program-preview-supplement-$zercherId").performScrollTo()
                .assertTextEquals("${names[1]} · FSL · 5 × 5 · ${names[1]} · 130 kg")
            captureVisualCatalogSurface("gym.531-supplemental.mixed-preview")
            compose.onNodeWithTag("five-three-one-program-create").performClick()
            compose.onNodeWithTag("routine-builder-save").performClick()
            compose.waitUntil(10_000) { runBlocking { app.routineRepository.routines.first() }.size == 1 }
            val routine = runBlocking { app.routineRepository.routines.first() }.single()
            val days = runBlocking { app.routineRepository.days.first() }.sortedBy { it.position }
            assertEquals(names, days.map { it.name })
            val placements = runBlocking { app.routineRepository.exercises.first() }.associateBy { it.exerciseId }
            val savedSets = runBlocking { app.routineRepository.sets.first() }
            val benchSets = savedSets.filter { it.routineExerciseId == placements.getValue(benchId).id }.map { it.draft }
                .filter { it.routinePhaseIndex == 0 && it.workSection == RoutineWorkSection.Supplemental }
            val zercherSets = savedSets.filter { it.routineExerciseId == placements.getValue(zercherId).id }.map { it.draft }
                .filter { it.routinePhaseIndex == 0 && it.workSection == RoutineWorkSection.Supplemental }
            assertEquals(5, benchSets.size)
            assertTrue(benchSets.all { it.reps == 10 && it.loadPercentage == 60.0 && it.supplementalScheme == RoutineSupplementalScheme.BoringButBig })
            assertEquals(5, zercherSets.size)
            assertTrue(zercherSets.all { it.reps == 5 && it.loadPercentage == 65.0 && it.supplementalScheme == RoutineSupplementalScheme.FirstSetLast })
            compose.onNodeWithContentDescription("Edit routine ${routine.name}").performScrollTo().performClick()
            scenario.recreate()
            compose.onNodeWithTag("routine-program-structure").assertExists()
            compose.onNodeWithContentDescription("Close routine editor").performClick()
            assertEquals(savedSets, runBlocking { app.routineRepository.sets.first() })
            for ((dayIndex, expectedLoad) in listOf(60.0, 130.0).withIndex()) {
                compose.onNodeWithTag("routine-start-next-${routine.id}").performScrollTo().performClick()
                compose.onNodeWithTag("routine-active-workout-action").performScrollTo().performClick()
                compose.onNodeWithTag("next-set-focus").assertTextContains(names[dayIndex], substring = true)
                val session = runBlocking { app.gymRepository.sessions.first() }.single { it.state == WorkoutSessionState.Active }
                val placementId = runBlocking { app.gymRepository.workoutExercises.first() }.single { it.sessionId == session.id }.id
                val sets = runBlocking { app.gymRepository.sets.first() }.filter { it.workoutExerciseId == placementId }.sortedBy { it.position }
                val supplemental = sets.filter { it.workSectionSnapshot == RoutineWorkSection.Supplemental }
                assertEquals(5, supplemental.size)
                assertTrue(supplemental.all { it.enteredWeight == expectedLoad && it.repetitions == if (dayIndex == 0) 10 else 5 })
                captureVisualCatalogSurface(if (dayIndex == 0) "gym.531-supplemental.bench-workout" else "gym.531-supplemental.zercher-workout")
                for (set in sets) {
                    compose.onNodeWithTag("quick-set-save-next-${set.id}").performScrollTo().performClick()
                    compose.waitUntil(10_000) { runBlocking { app.gymRepository.sets.first() }.single { it.id == set.id }.completed }
                }
                val performed = runBlocking { app.gymRepository.sets.first() }.associateBy { it.id }
                compose.onNodeWithTag("active-workout-list").performScrollToNode(hasTestTag("active-workout-finish"))
                compose.onNodeWithTag("active-workout-finish").performClick()
                compose.waitUntil(10_000) {
                    runBlocking { app.gymRepository.sessions.first() }.single { it.id == session.id }.state == WorkoutSessionState.Finished ||
                        compose.onAllNodesWithTag("finish-workout-confirm").fetchSemanticsNodes().isNotEmpty()
                }
                if (compose.onAllNodesWithTag("finish-workout-confirm").fetchSemanticsNodes().isNotEmpty()) {
                    compose.onNodeWithTag("finish-workout-confirm").performClick()
                }
                compose.waitUntil(10_000) { runBlocking { app.gymRepository.sessions.first() }.single { it.id == session.id }.state == WorkoutSessionState.Finished }
                assertEquals(performed, runBlocking { app.gymRepository.sets.first() }.associateBy { it.id })
                openRoutines()
            }
        }
    }

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
