package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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
import com.whip.app.domain.RoutineProgramPhaseRole
import com.whip.app.domain.RoutineProgramTemplateKey
import com.whip.app.domain.RoutineProgressionMode
import com.whip.app.domain.RoutineWorkSection
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.domain.WorkoutSetClassification
import com.whip.app.domain.WorkoutSetDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Existing legacy sessions can finish truthfully after the branded authoring flow is retired. */
@RunWith(AndroidJUnit4::class)
class GymLegacyRetirementJourneyE2ETest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()

    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun interruptedLegacyWorkoutRetainsReviewThenConvertsOnlyFutureTemplate() {
        val ids = runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { AppSettings(setupCompleted = true) }
            val exerciseId = app.gymRepository.createExercise(ExerciseDraft("Legacy Bench Press"))
            val routineId = app.routineRepository.createRoutine(RoutineDraft(
                name = "Existing strength plan",
                program = RoutineProgramDraft(
                    kind = RoutineProgramKind.FiveThreeOne,
                    phaseCount = 1,
                    phaseLabels = listOf("Test"),
                    phaseRoles = listOf(RoutineProgramPhaseRole.TrainingMaxTest),
                    trainingMaxAdvanceAfterPhaseIndices = setOf(0),
                    templateKey = RoutineProgramTemplateKey.FiveThreeOneCustom,
                    templateRevision = 2,
                    progressionMode = RoutineProgressionMode.PerformanceInformed,
                ),
                days = listOf(RoutineDayDraft("Bench", listOf(RoutineExerciseDraft(
                    exerciseId = exerciseId,
                    placementKind = RoutinePlacementKind.MainExercise,
                    trainingMaxValue = 100.0,
                    trainingMaxUnitId = "kilogram",
                    cycleIncrementValue = 2.5,
                    plannedSets = listOf(WorkoutSetDraft(
                        reps = 3,
                        classification = WorkoutSetClassification.TrainingMaxTest,
                        loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                        loadPercentage = 100.0,
                        routinePhaseIndex = 0,
                        workSection = RoutineWorkSection.Main,
                    )),
                )))),
            ))
            val sessionId = app.routineRepository.startRoutine(routineId)
            app.gymRepository.setSetCompleted(
                app.gymRepository.sets.first().single().id,
                completed = true,
                autoStartRest = false,
            )
            routineId to sessionId
        }
        val (routineId, sessionId) = ids

        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithTag("gym-destination-Workout").performClick()
            compose.onNodeWithTag("active-workout-list")
                .performScrollToNode(hasTestTag("active-workout-finish"))
            compose.onNodeWithTag("active-workout-finish").performClick()
            compose.onNodeWithTag("apply-training-max-decisions").assertIsDisplayed()

            scenario.recreate()

            compose.onNodeWithTag("apply-training-max-decisions").assertIsDisplayed()
            assertEquals(RoutineProgramKind.FiveThreeOne, runBlocking {
                app.routineRepository.routines.first().single { it.id == routineId }.programKind
            })
            compose.onNodeWithTag("apply-training-max-decisions").performClick()
            compose.waitUntil(10_000L) {
                runBlocking { app.gymRepository.sessions.first() }
                    .any { it.id == sessionId && it.state == WorkoutSessionState.Finished }
            }
        }

        compose.waitUntil(10_000L) {
            runBlocking { app.routineRepository.routines.first() }
                .any { it.id == routineId && it.programKind == RoutineProgramKind.Custom }
        }
        val finished = runBlocking { app.gymRepository.sessions.first().single { it.id == sessionId } }
        assertEquals(RoutineProgramKind.FiveThreeOne, finished.sourceRoutineProgramKind)
        assertEquals(WorkoutSetClassification.TrainingMaxTest, runBlocking {
            app.gymRepository.sets.first().single().classification
        })
        assertTrue(finished.programProgressAdvanced)
        val nextSessionId = runBlocking { app.routineRepository.startRoutine(routineId) }
        assertEquals(RoutineProgramKind.Custom, runBlocking {
            app.gymRepository.sessions.first().single { it.id == nextSessionId }.sourceRoutineProgramKind
        })
    }
}
