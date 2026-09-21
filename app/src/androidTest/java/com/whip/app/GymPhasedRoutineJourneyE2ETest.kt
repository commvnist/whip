package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.AppSettings
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.RoutineDayDraft
import com.whip.app.domain.RoutineDraft
import com.whip.app.domain.RoutineExerciseDraft
import com.whip.app.domain.RoutineLoadPrescriptionType
import com.whip.app.domain.RoutinePlacementKind
import com.whip.app.domain.RoutineProgramDraft
import com.whip.app.domain.RoutineProgramKind
import com.whip.app.domain.RoutineWorkSection
import com.whip.app.domain.WorkoutSetDraft
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real app-shell path from an ordinary phased Routine to its scheduled workout. */
@RunWith(AndroidJUnit4::class)
class GymPhasedRoutineJourneyE2ETest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    private var routineId = 0L

    @Before fun prepare() = runBlocking {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { AppSettings(setupCompleted = true) }
        val benchId = app.gymRepository.createExercise(ExerciseDraft("Bench Press"))
        routineId = app.routineRepository.createRoutine(RoutineDraft(
            name = "Strength blocks",
            program = RoutineProgramDraft(
                kind = RoutineProgramKind.Custom,
                phaseCount = 2,
                phaseLabels = listOf("Volume", "Heavy"),
                trainingMaxAdvanceAfterPhaseIndices = setOf(1),
            ),
            days = listOf(RoutineDayDraft("Upper", listOf(RoutineExerciseDraft(
                exerciseId = benchId,
                placementKind = RoutinePlacementKind.MainExercise,
                trainingMaxValue = 100.0,
                cycleIncrementValue = 2.5,
                plannedSets = listOf(
                    WorkoutSetDraft(reps = 5, routinePhaseIndex = 0, workSection = RoutineWorkSection.Main,
                        loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax, loadPercentage = 80.0),
                    WorkoutSetDraft(reps = 3, routinePhaseIndex = 1, workSection = RoutineWorkSection.Main,
                        loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax, loadPercentage = 90.0),
                ),
            )))),
        ))
    }

    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun routineStartsFromLibraryAndShowsItsActualPhaseInWorkout() {
        launchMainActivity(Intent(app, MainActivity::class.java)).use {
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithTag("gym-destination-Library").performClick()
            compose.onNodeWithTag("gym-library-Routines").performClick()
            compose.onNodeWithTag("routine-start-next-$routineId").performScrollTo().performClick()
            compose.onNodeWithTag("routine-active-workout-action").performScrollTo().assertIsDisplayed()
            captureVisualCatalogSurface("gym.routine.active-blocked")
            compose.onNodeWithTag("routine-active-workout-action").performClick()
            compose.onNodeWithTag("active-workout-program-context")
                .assertIsDisplayed()
                .assertTextContains("Phased Routine · Cycle 1 · Volume · Day 1")
            compose.onNodeWithTag("next-set-focus").assertTextContains("Bench Press", substring = true)
            captureVisualCatalogSurface("gym.routine-phased.workout")
        }
    }
}
