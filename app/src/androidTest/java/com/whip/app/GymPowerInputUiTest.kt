package com.whip.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.BodyweightLoadPolicy
import com.whip.app.domain.EstimatedOneRepMaxFormula
import com.whip.app.domain.Exercise
import com.whip.app.domain.ExerciseTrackingType
import com.whip.app.domain.LoadInterpretation
import com.whip.app.domain.GymMachineDraft
import com.whip.app.domain.MachineLevelDirection
import com.whip.app.domain.GymRoutine
import com.whip.app.domain.RoutineDay
import com.whip.app.domain.RoutineExercise
import com.whip.app.domain.RoutineAssistanceRole
import com.whip.app.domain.RoutineLoadPrescriptionType
import com.whip.app.domain.RoutineMainWorkScheme
import com.whip.app.domain.RoutineProgramKind
import com.whip.app.domain.RoutineProgramPhaseRole
import com.whip.app.domain.RoutineSet
import com.whip.app.domain.RoutineSupplementalScheme
import com.whip.app.domain.RoutineTrainingMaxSource
import com.whip.app.domain.RoutineWorkSection
import com.whip.app.domain.WorkoutExercise
import com.whip.app.domain.WorkoutExerciseOutcome
import com.whip.app.domain.WorkoutGroup
import com.whip.app.domain.WorkoutGroupType
import com.whip.app.domain.WorkoutSession
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.domain.WorkoutSet
import com.whip.app.domain.WorkoutSetClassification
import com.whip.app.domain.WorkoutSetDraft
import com.whip.app.domain.WorkoutSetRemovalReason
import com.whip.app.domain.WorkoutSetMutationBoundary
import com.whip.app.core.DEFAULT_REST_TIMER_PRESET_SECONDS
import com.whip.app.ui.ExerciseEditorDialog
import com.whip.app.ui.MachineEditorDialog
import com.whip.app.ui.MachinePermanentDeleteDialog
import com.whip.app.ui.ExercisePermanentDeleteDialog
import com.whip.app.ui.RoutinePermanentDeleteDialog
import com.whip.app.ui.LocalWhipDialogPlacement
import com.whip.app.ui.WhipDialogPlacement
import com.whip.app.ui.QuickSetEntry
import com.whip.app.ui.QuickSetAuthorshipBoundary
import com.whip.app.ui.RestTimerCard
import com.whip.app.ui.WorkoutExerciseCard
import com.whip.app.ui.WorkoutExerciseGroupSurface
import com.whip.app.ui.WorkoutExerciseUi
import com.whip.app.ui.WorkoutHistoryCard
import com.whip.app.ui.GymUiState
import com.whip.app.ui.GymProgressContent
import com.whip.app.ui.buildWorkoutExerciseBlocks
import com.whip.app.ui.customDisplayName
import com.whip.app.ui.reorderWorkoutBlock
import com.whip.app.ui.reorderWorkoutGroupMember
import com.whip.app.ui.routineDraftForEditing
import com.whip.app.ui.routineProgramStatusLabel
import com.whip.app.ui.workoutProgramSnapshotLabel
import com.whip.app.data.MachineDeletionImpact
import com.whip.app.data.ExerciseDeletionImpact
import com.whip.app.data.RoutineDeletionImpact
import com.whip.app.ui.theme.WhipTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.time.LocalDate
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class GymPowerInputUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun routineEditReconstructionPreservesAdvancedProgrammingFields() {
        val routine = GymRoutine(
            id = 1,
            uuid = "routine",
            name = "Strength cycle",
            notes = "Keep the plan",
            position = 0,
            archived = false,
            pinned = false,
            createdAtMillis = 1,
            updatedAtMillis = 2,
            programKind = RoutineProgramKind.FiveThreeOneClassic,
            programPhaseCount = 4,
            programPhaseLabels = listOf("5s", "3s", "5/3/1", "Deload"),
            currentProgramPhaseIndex = 2,
            currentProgramCycle = 3,
            nextProgramDayPosition = 1,
            programPhaseRoles = listOf(
                RoutineProgramPhaseRole.Leader,
                RoutineProgramPhaseRole.Leader,
                RoutineProgramPhaseRole.Anchor,
                RoutineProgramPhaseRole.Deload,
            ),
            trainingMaxAdvanceAfterPhaseIndices = setOf(3),
        )
        val day = RoutineDay(2, "day", routine.id, "Upper", 0, 1, 2, progressionIndex = 4)
        val placement = RoutineExercise(
            id = 3,
            uuid = "placement",
            routineDayId = day.id,
            exerciseId = 4,
            position = 0,
            notes = "Bar path",
            groupKey = null,
            copyPreviousWorkout = false,
            createdAtMillis = 1,
            updatedAtMillis = 2,
            trainingMaxPercent = 87.5,
            progressionPercentages = listOf(100.0, 102.5, 90.0),
            alternativeExerciseIds = listOf(7L, 9L),
            trainingMaxValue = 225.0,
            trainingMaxUnitId = "pound",
            cycleIncrementValue = 5.0,
            trainingMaxSource = RoutineTrainingMaxSource.Explicit,
            mainWorkScheme = RoutineMainWorkScheme.ClassicPrSet,
            supplementalScheme = RoutineSupplementalScheme.FirstSetLast,
            assistanceRole = RoutineAssistanceRole.MainLift,
            jokerSetsEnabled = true,
        )
        val plannedSet = RoutineSet(
            id = 5,
            uuid = "planned-set",
            routineExerciseId = placement.id,
            position = 0,
            draft = WorkoutSetDraft(
                reps = 5,
                repsMax = 8,
                planned = true,
                classification = WorkoutSetClassification.Amrap,
                loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                loadPercentage = 85.0,
                routinePhaseIndex = 2,
            ),
            createdAtMillis = 1,
            updatedAtMillis = 2,
        )

        val reconstructed = routineDraftForEditing(
            GymUiState(
                routines = listOf(routine),
                routineDays = listOf(day),
                routineExercises = listOf(placement),
                routineSets = listOf(plannedSet),
            ),
            routine,
        )
        val reconstructedPlacement = reconstructed.days.single().exercises.single()

        assertEquals(87.5, reconstructedPlacement.trainingMaxPercent, 0.0)
        assertEquals(listOf(100.0, 102.5, 90.0), reconstructedPlacement.progressionPercentages)
        assertEquals(listOf(7L, 9L), reconstructedPlacement.alternativeExerciseIds)
        assertEquals(225.0, reconstructedPlacement.trainingMaxValue!!, 0.0)
        assertEquals("pound", reconstructedPlacement.trainingMaxUnitId)
        assertEquals(5.0, reconstructedPlacement.cycleIncrementValue!!, 0.0)
        assertEquals(RoutineTrainingMaxSource.Explicit, reconstructedPlacement.trainingMaxSource)
        assertEquals(RoutineMainWorkScheme.ClassicPrSet, reconstructedPlacement.mainWorkScheme)
        assertEquals(RoutineSupplementalScheme.FirstSetLast, reconstructedPlacement.supplementalScheme)
        assertEquals(RoutineAssistanceRole.MainLift, reconstructedPlacement.assistanceRole)
        assertTrue(reconstructedPlacement.jokerSetsEnabled)
        assertEquals(4, reconstructed.days.single().progressionIndex)
        assertEquals(RoutineProgramKind.FiveThreeOneClassic, reconstructed.program?.kind)
        assertEquals(listOf("5s", "3s", "5/3/1", "Deload"), reconstructed.program?.phaseLabels)
        assertEquals(
            listOf(
                RoutineProgramPhaseRole.Leader,
                RoutineProgramPhaseRole.Leader,
                RoutineProgramPhaseRole.Anchor,
                RoutineProgramPhaseRole.Deload,
            ),
            reconstructed.program?.phaseRoles,
        )
        assertEquals(setOf(3), reconstructed.program?.trainingMaxAdvanceAfterPhaseIndices)
        assertEquals(plannedSet.draft, reconstructedPlacement.plannedSets.single())
        assertEquals(
            "Classic 5/3/1 · Cycle 3 · 5/3/1 · Next · Upper",
            routineProgramStatusLabel(routine, "Upper"),
        )
    }

    @Test
    fun convertingExerciseToPoundsUsesPoundHardwareRatherThanConvertedDecimals() {
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                ExerciseEditorDialog(
                    exercise = null,
                    categories = emptyList(),
                    selectedCategoryIds = emptySet(),
                    defaultWeightUnit = "kilogram",
                    defaultRestSeconds = 120,
                    defaultFormula = EstimatedOneRepMaxFormula.Epley,
                    platePresets = emptyList(),
                    onDismiss = {},
                    onSave = {},
                )
            }
        }

        compose.onNodeWithText("Advanced Options").performClick()
        compose.onNodeWithText("lb").performScrollTo().performClick()
        compose.onNodeWithText("Convert Default Values").performClick()
        compose.onNodeWithTag("exercise-editor-list").performScrollToNode(hasTestTag("exercise-weight-increment"))
        compose.onNodeWithTag("exercise-weight-increment").assertTextContains("5")
        compose.onNodeWithTag("exercise-editor-list").performScrollToNode(hasTestTag("exercise-bar-weight"))
        compose.onNodeWithTag("exercise-bar-weight").assertTextContains("45")
        compose.onNodeWithTag("exercise-plates").assertTextContains("45,35,25,10,5,2.5")
        compose.onNodeWithTag("exercise-editor-list").performScrollToNode(hasText("Effort field"))
        compose.onNodeWithText("Effort field").assertIsDisplayed()
        compose.onAllNodes(hasText("RPE field")).assertCountEquals(0)
        compose.onAllNodes(hasText("RIR field")).assertCountEquals(0)
    }

    @Test
    fun exerciseEditorExplainsInvalidDefaultsAndDoesNotSubmitThem() {
        var submitted = false
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                ExerciseEditorDialog(
                    exercise = null,
                    categories = emptyList(),
                    selectedCategoryIds = emptySet(),
                    defaultWeightUnit = "kilogram",
                    defaultRestSeconds = 120,
                    defaultFormula = EstimatedOneRepMaxFormula.Epley,
                    platePresets = emptyList(),
                    onDismiss = {},
                    onSave = { submitted = true },
                )
            }
        }

        compose.onNodeWithTag("exercise-editor-name").performTextInput("Bench press")
        compose.onNodeWithText("Advanced Options").performClick()
        compose.onNodeWithTag("exercise-editor-list").performScrollToNode(hasTestTag("exercise-weight-increment"))
        compose.onNodeWithTag("exercise-weight-increment").performTextReplacement("0")
        compose.onNodeWithText("Save").performClick()

        compose.onNodeWithTag("exercise-editor-list").performScrollToNode(hasTestTag("exercise-save-problem"))
        compose.onNodeWithTag("exercise-save-problem")
            .assertIsDisplayed()
        compose.onNodeWithContentDescription("Save blocked. Weight increment must be above 0")
            .assertIsDisplayed()
        compose.runOnIdle { assertEquals(false, submitted) }
    }

    @Test
    fun progressWithoutCompletedWorkoutsDoesNotShowInventedTrendControls() {
        var workoutRequested = false
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                GymProgressContent(
                    state = GymUiState(exercises = listOf(testExercise()), loading = false),
                    onOpenExercises = {},
                    onOpenWorkout = { workoutRequested = true },
                    onOpenWorkoutHistory = {},
                    onManageTrackedRecords = {},
                )
            }
        }

        compose.onNodeWithText("No Progress Data Yet").assertIsDisplayed()
        compose.onAllNodes(hasTestTag("gym-progress-exercise-selector")).assertCountEquals(0)
        compose.onNode(hasText("Start a Workout") and hasClickAction()).performClick()
        compose.runOnIdle { assertTrue(workoutRequested) }
    }

    @Test
    fun restTimerStacksActionsAtLargeTextAndNamesEveryAction() {
        val session = WorkoutSession(
            id = 91,
            uuid = "large-text-timer",
            name = "Workout",
            notes = "",
            startedAt = Instant.parse("2026-08-31T16:00:00Z"),
            endedAt = null,
            localDate = LocalDate.of(2026, 8, 31),
            zoneId = "UTC",
            state = WorkoutSessionState.Active,
            keepScreenAwake = false,
            restTimerDeadlineMillis = null,
            restTimerDurationSeconds = null,
            archived = false,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        compose.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(currentDensity.density, fontScale = 2f)) {
                WhipTheme(darkTheme = true, dynamicColor = false) {
                    RestTimerCard(
                        session = session,
                        remaining = 45,
                        selectedSeconds = 120,
                        presetSeconds = DEFAULT_REST_TIMER_PRESET_SECONDS,
                        notificationPermissionRequested = false,
                        onSelectedSecondsChange = {},
                        onPresetSecondsChange = {},
                        onStart = { _, _ -> },
                        onAdjust = { _, _ -> },
                        onStop = {},
                    )
                }
            }
        }

        val timer = compose.onNodeWithText("Rest · 0:45").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val subtract = compose.onNodeWithContentDescription("Subtract 15 seconds from rest timer").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val add = compose.onNodeWithContentDescription("Add 15 seconds to rest timer").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val stop = compose.onNodeWithContentDescription("Stop rest timer").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        check(timer.bottom <= subtract.top) { "Large-text timer actions must move below the timer label" }
        check(subtract.right <= add.left && add.right <= stop.left) { "Timer actions must not overlap" }
    }

    @Test
    fun numberedMachineDefaultsToCompactOneThroughTenRangeAndIncrement() {
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                MachineEditorDialog(
                    machine = null,
                    exercises = listOf(testExercise()),
                    definitionLocked = false,
                    onDismiss = {},
                    onSave = {},
                )
            }
        }

        compose.onNodeWithTag("machine-editor-list").performScrollToNode(hasText("Numbered Stack / Level"))
        compose.onNodeWithText("Numbered Stack / Level").performClick()
        compose.onNodeWithTag("machine-editor-list").performScrollToNode(hasTestTag("machine-load-spec"))
        compose.onNodeWithTag("machine-load-spec").assertTextContains("1-10")
        compose.onNodeWithTag("machine-editor-list").performScrollToNode(hasTestTag("machine-load-increment"))
        compose.onNodeWithTag("machine-load-increment").assertTextContains("1")
        compose.onNodeWithTag("machine-editor-list").performScrollToNode(hasTestTag("machine-load-preview"))
        compose.onNodeWithTag("machine-load-preview").assertIsDisplayed().assertTextContains("Preview · 10 values · 1-10 by 1")
    }

    @Test
    fun machineCanBeSavedWithoutExercises() {
        var saved: GymMachineDraft? = null
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                MachineEditorDialog(
                    machine = null,
                    exercises = emptyList(),
                    definitionLocked = false,
                    onDismiss = {},
                    onSave = { saved = it },
                )
            }
        }

        compose.onNodeWithTag("machine-editor-name").performTextInput("Standalone cable")
        compose.onNodeWithText("Save").assertIsEnabled().performClick()

        compose.runOnIdle { assertEquals(emptySet<Long>(), saved?.exerciseIds) }
    }

    @Test
    fun machineDraftSurvivesCreatingAndLinkingAnExerciseAndSavesReverseLevels() {
        val existing = testExercise().copy(id = 1, name = "Cable row")
        val created = testExercise().copy(id = 2, name = "Cable press")
        var createdRequest by mutableStateOf<Long?>(null)
        var saved: GymMachineDraft? = null
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                MachineEditorDialog(
                    machine = null,
                    exercises = listOf(existing, created),
                    definitionLocked = false,
                    createdExerciseIdRequest = createdRequest,
                    onCreatedExerciseRequestConsumed = { createdRequest = null },
                    onCreateExercise = { createdRequest = created.id },
                    onDismiss = {},
                    onSave = { saved = it },
                )
            }
        }

        compose.onNodeWithTag("machine-editor-name").performTextInput("Shared cable")
        compose.onNodeWithTag("machine-editor-list").performScrollToNode(hasTestTag("machine-choose-exercises"))
        compose.onNodeWithTag("machine-choose-exercises").performClick()
        compose.onNodeWithTag("machine-create-exercise").performClick()
        compose.onNodeWithTag("machine-editor-name").assertTextContains("Shared cable")
        compose.onNodeWithTag("machine-editor-list").performScrollToNode(hasTestTag("machine-choose-exercises"))
        compose.onNodeWithTag("machine-choose-exercises").performClick()
        compose.onNodeWithTag("machine-exercise-picker-list").performScrollToNode(hasText("Cable row"))
        compose.onNodeWithText("Cable row").performClick()
        compose.onNodeWithText("Done").performClick()

        compose.onNodeWithTag("machine-editor-list").performScrollToNode(hasText("Numbered Stack / Level"))
        compose.onNodeWithText("Numbered Stack / Level").performClick()
        compose.onNodeWithTag("machine-editor-list").performScrollToNode(hasText("Higher number = less resistance"))
        compose.onNodeWithText("Higher number = less resistance").performClick()
        compose.onNodeWithText("Save").performClick()

        compose.runOnIdle {
            assertEquals(setOf(1L, 2L), saved?.exerciseIds)
            assertEquals(MachineLevelDirection.HigherNumberLessResistance, saved?.levelDirection)
        }
    }

    @Test
    fun machineDeleteDialogExplainsPreservedHistoryAndBlocksActiveUse() {
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                MachinePermanentDeleteDialog(
                    impact = MachineDeletionImpact(
                        machineId = 1,
                        machineUuid = "machine-1",
                        displayName = "Downtown cable stack",
                        configurationVersion = 2,
                        historicalPlacements = 9,
                        completedSessions = 9,
                        setCount = 46,
                        firstWorkoutDate = LocalDate.of(2026, 1, 1),
                        lastWorkoutDate = LocalDate.of(2026, 8, 1),
                        activePlacements = 1,
                        routineReferences = 2,
                        routineNames = listOf("Push A", "Upper"),
                        currentPersonalRecords = 3,
                        siblingVersions = 2,
                        revisionToken = "revision",
                    ),
                    onDismiss = {},
                    onConfirm = {},
                    onReviewRoutines = {},
                    onOpenActiveWorkout = {},
                    onBackUpFirst = {},
                    deleting = false,
                )
            }
        }

        compose.onNodeWithText("Delete “Downtown cable stack” v2 Permanently?").assertIsDisplayed()
        compose.onNodeWithText("Kept").assertIsDisplayed()
        compose.onNodeWithText("Needs Attention").assertIsDisplayed()
        compose.onNodeWithText("Active Workout").assertIsDisplayed()
        compose.onNodeWithTag("machine-delete-confirm").assertIsNotEnabled()
    }

    @Test
    fun exerciseDeleteDialogBlocksActiveUseAndPreservesTrainingMaxAuditHistory() {
        var openedWorkout = false
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(compose.density.density, fontScale = 2f),
                LocalWhipDialogPlacement provides WhipDialogPlacement(maxWidth = 320.dp),
            ) {
                WhipTheme(darkTheme = true, dynamicColor = false) {
                    ExercisePermanentDeleteDialog(
                        modifier = Modifier.width(320.dp),
                        exerciseName = "Zercher squat",
                        impact = exerciseDeletionImpact(activePlacements = 1, trainingMaxDecisionCount = 3),
                        targetMissing = false,
                        preparing = false,
                        deleting = false,
                        errorMessage = null,
                        onDismiss = {},
                        onReviewUpdatedImpact = {},
                        onOpenActiveWorkout = { openedWorkout = true },
                        onConfirm = {},
                    )
                }
            }
        }

        val dialog = compose.onNodeWithTag("exercise-delete-dialog").getUnclippedBoundsInRoot()
        assertTrue(dialog.right - dialog.left <= 321.dp)
        compose.onNodeWithText("Active Workout").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("exercise-delete-impact-list").performScrollToNode(hasText("Preserved"))
        compose.onNodeWithText("Preserved").assertIsDisplayed()
        compose.onNodeWithTag("exercise-delete-impact-list").performScrollToNode(hasText("References changed"))
        compose.onNodeWithText("References changed").assertIsDisplayed()
        compose.onNodeWithTag("exercise-delete-impact-list").performScrollToNode(hasText("Track history kept"))
        compose.onNodeWithText("Track history kept").assertIsDisplayed()
        compose.onNodeWithTag("exercise-delete-impact-list").performScrollToNode(hasText("Open Active Workout"))
        compose.onNodeWithText("Open Active Workout").assertIsDisplayed().performClick()
        compose.onNodeWithTag("exercise-delete-confirm").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithText("Cancel").assertIsDisplayed()
        compose.runOnIdle { assertTrue(openedWorkout) }
    }

    @Test
    fun routineDeleteDialogBlocksActiveSourceAndKeepsWorkoutAndTrainingMaxHistory() {
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutinePermanentDeleteDialog(
                    routineName = "Custom 5/3/1",
                    impact = RoutineDeletionImpact(
                        routineId = 8,
                        displayName = "Custom 5/3/1",
                        activeSession = true,
                        dayCount = 3,
                        routinePlacementCount = 3,
                        routineSetCount = 36,
                        preservedWorkoutHistoryCount = 11,
                        trainingMaxDecisionCount = 6,
                        revisionToken = "routine-revision",
                    ),
                    targetMissing = false,
                    preparing = false,
                    deleting = false,
                    errorMessage = null,
                    onDismiss = {},
                    onReviewUpdatedImpact = {},
                    onOpenActiveWorkout = {},
                    onConfirm = {},
                )
            }
        }

        compose.onNodeWithText("Active Workout").assertIsDisplayed()
        compose.onNodeWithText("Kept").assertIsDisplayed()
        compose.onNodeWithTag("routine-delete-confirm").assertIsNotEnabled()
    }

    @Test
    fun exerciseDeleteDialogKeepsStaleImpactFailureInlineAndRetryable() {
        var reviews = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                WhipTheme(darkTheme = true, dynamicColor = false) {
                    ExercisePermanentDeleteDialog(
                        exerciseName = "Bench press",
                        impact = exerciseDeletionImpact(),
                        targetMissing = false,
                        preparing = false,
                        deleting = false,
                        errorMessage = "The Exercise or its deletion impact changed while the confirmation was open.",
                        onDismiss = {},
                        onReviewUpdatedImpact = { reviews++ },
                        onOpenActiveWorkout = {},
                        onConfirm = {},
                    )
                }
            }
        }

        compose.onNodeWithTag("exercise-delete-error").assertIsDisplayed()
        compose.onNodeWithText("Review Updated Impact").performClick()
        compose.onNodeWithTag("exercise-delete-confirm").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(1, reviews) }
    }

    @Test
    fun missingExerciseDeletionTargetOffersCloseInsteadOfDestructiveRetry() {
        var closes = 0
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                ExercisePermanentDeleteDialog(
                    exerciseName = "Bench press",
                    impact = null,
                    targetMissing = true,
                    preparing = false,
                    deleting = false,
                    errorMessage = "Exercise no longer exists. It may already have been deleted.",
                    onDismiss = { closes++ },
                    onReviewUpdatedImpact = {},
                    onOpenActiveWorkout = {},
                    onConfirm = {},
                )
            }
        }

        compose.onNodeWithText("Exercise unavailable").assertIsDisplayed()
        val liveRegion = compose.onNodeWithTag("exercise-delete-error")
            .fetchSemanticsNode().config[SemanticsProperties.LiveRegion]
        assertEquals(LiveRegionMode.Polite, liveRegion)
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithTag("exercise-delete-confirm").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(1, closes) }
    }

    @Test
    fun missingRoutineDeletionTargetIsAnnouncedAndCannotBeDeleted() {
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutinePermanentDeleteDialog(
                    routineName = "Custom 5/3/1",
                    impact = null,
                    targetMissing = true,
                    preparing = false,
                    deleting = false,
                    errorMessage = "Routine no longer exists. It may already have been deleted.",
                    onDismiss = {},
                    onReviewUpdatedImpact = {},
                    onOpenActiveWorkout = {},
                    onConfirm = {},
                )
            }
        }

        compose.onNodeWithText("Routine unavailable").assertIsDisplayed()
        val liveRegion = compose.onNodeWithTag("routine-delete-error")
            .fetchSemanticsNode().config[SemanticsProperties.LiveRegion]
        assertEquals(LiveRegionMode.Polite, liveRegion)
        compose.onNodeWithTag("routine-delete-confirm").assertIsNotEnabled()
    }

    @Test
    fun restoredUnverifiedMissingExerciseKeepsRetryVerificationInsteadOfAbandoningRecovery() {
        val restoration = StateRestorationTester(compose)
        var reviews = 0
        restoration.setContent {
            val verificationPending = androidx.compose.runtime.saveable.rememberSaveable {
                mutableStateOf(true)
            }
            WhipTheme(darkTheme = true, dynamicColor = false) {
                ExercisePermanentDeleteDialog(
                    exerciseName = "Bench press",
                    impact = null,
                    targetMissing = true,
                    preparing = false,
                    deleting = false,
                    errorMessage = "Exercise no longer exists. It may already have been deleted.",
                    onDismiss = {},
                    onReviewUpdatedImpact = { reviews++ },
                    onOpenActiveWorkout = {},
                    onConfirm = {},
                    outcomeVerificationPending = verificationPending.value,
                )
            }
        }

        restoration.emulateSavedInstanceStateRestore()

        compose.onNodeWithText("Outcome not verified").assertIsDisplayed()
        compose.onNodeWithText("Retry Verification").performClick()
        compose.onAllNodesWithText("Close").assertCountEquals(0)
        compose.runOnIdle { assertEquals(1, reviews) }
    }

    @Test
    fun quickSetEntryPresentsInputsBeforeItsPrimarySaveAction() {
        val exercise = testExercise()
        val workoutExercise = WorkoutExercise(
            id = 2,
            uuid = "workout-exercise",
            sessionId = 3,
            exerciseId = exercise.id,
            position = 0,
            notes = "",
            groupId = null,
            createdAtMillis = 1,
            updatedAtMillis = 1,
            loadInterpretationSnapshot = LoadInterpretation.Total,
            trackingTypeSnapshot = ExerciseTrackingType.WeightReps,
            exerciseWeightUnitSnapshot = "kilogram",
        )
        val set = WorkoutSet(
            id = 4,
            uuid = "set",
            workoutExerciseId = workoutExercise.id,
            position = 0,
            classification = WorkoutSetClassification.Working,
            planned = false,
            completed = false,
            canonicalWeightKg = 50.0,
            enteredWeight = 50.0,
            enteredWeightUnitId = "kilogram",
            repetitions = 5,
            canonicalDistanceMetres = null,
            enteredDistance = null,
            enteredDistanceUnitId = null,
            durationSeconds = null,
            bodyweightKg = null,
            note = "",
            rpe = null,
            rir = null,
            tempo = "",
            restSeconds = 120,
            completedAtMillis = null,
            deletedAtMillis = null,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                QuickSetEntry(
                    set = set,
                    exercise = exercise,
                    workoutExercise = workoutExercise,
                    machine = null,
                    preferredWeightUnitId = "kilogram",
                    preferredDistanceUnitId = "kilometre",
                    showRpe = false,
                    showRir = false,
                    onMoreDetails = {},
                    onSave = { _, _, _ -> },
                )
            }
        }

        val load = compose.onNodeWithTag("quick-set-load-${set.id}").fetchSemanticsNode().boundsInRoot
        val reps = compose.onNodeWithTag("quick-set-reps-${set.id}").fetchSemanticsNode().boundsInRoot
        val saveNext = compose.onNodeWithTag("quick-set-save-next-${set.id}").fetchSemanticsNode().boundsInRoot
        check(load.bottom <= saveNext.top && reps.bottom <= saveNext.top) {
            "Quick-set actions must follow data entry: load=$load reps=$reps saveNext=$saveNext"
        }
        compose.onNodeWithContentDescription("Decrease Cable row Weight (kg) by 2.5").assertIsDisplayed()
        compose.onNodeWithContentDescription("Increase Cable row Weight (kg) by 2.5").assertIsDisplayed()
        val decrement = compose.onNodeWithContentDescription("Decrease Cable row Weight (kg) by 2.5").fetchSemanticsNode().boundsInRoot
        val increment = compose.onNodeWithContentDescription("Increase Cable row Weight (kg) by 2.5").fetchSemanticsNode().boundsInRoot
        check(decrement.left >= load.left && decrement.right <= load.right)
        check(increment.left >= load.left && increment.right <= load.right)
    }

    @Test
    fun readyRestTimerMakesWorkoutScopedDurationDiscoverableAndUsesIt() {
        val session = WorkoutSession(
            id = 9,
            uuid = "session",
            name = "Workout",
            notes = "",
            startedAt = Instant.parse("2026-08-22T16:00:00Z"),
            endedAt = null,
            localDate = LocalDate.of(2026, 8, 22),
            zoneId = "UTC",
            state = WorkoutSessionState.Active,
            keepScreenAwake = false,
            restTimerDeadlineMillis = null,
            restTimerDurationSeconds = null,
            archived = false,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        var startedWith: Int? = null
        var savedPresets: List<Int>? = null
        compose.setContent {
            var selectedSeconds by remember { mutableStateOf(120) }
            var presets by remember { mutableStateOf(DEFAULT_REST_TIMER_PRESET_SECONDS) }
            WhipTheme(dynamicColor = false) {
                RestTimerCard(
                    session = session,
                    remaining = null,
                    selectedSeconds = selectedSeconds,
                    presetSeconds = presets,
                    notificationPermissionRequested = true,
                    onSelectedSecondsChange = { selectedSeconds = it },
                    onPresetSecondsChange = { changed -> presets = changed; savedPresets = changed },
                    onStart = { _, seconds -> startedWith = seconds },
                    onAdjust = { _, _ -> },
                    onStop = {},
                )
            }
        }

        compose.onNodeWithText("Rest · 2:00").assertIsDisplayed()
        compose.onNodeWithContentDescription("Adjust rest time for this workout").performClick()
        compose.onNodeWithText("Rest Time for This Workout").assertIsDisplayed()
        listOf("1:00", "1:30", "2:00", "2:30", "3:00", "5:00").forEach { preset ->
            compose.onAllNodes(hasText(preset)).fetchSemanticsNodes().also { nodes ->
                check(nodes.isNotEmpty()) { "Missing default rest preset $preset" }
            }
        }
        compose.onNodeWithText("Manage Presets").performClick()
        compose.onNodeWithTag("rest-preset-seconds").performTextReplacement("45")
        compose.onNodeWithText("Add Preset").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasText("0:45 ×")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Save Presets").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasText("Rest Time for This Workout")).fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodes(hasText("0:45")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("0:45").assertIsDisplayed()
        compose.runOnIdle { check(savedPresets?.contains(45) == true) }
        compose.onNodeWithContentDescription("Increase workout rest time by 15").performClick()
        compose.onNodeWithText("Use for This Workout").performClick()
        compose.onNodeWithText("Rest · 2:15").assertIsDisplayed()
        compose.onNodeWithText("Start").performClick()
        compose.runOnIdle { assertEquals(135, startedWith) }
    }

    @Test
    fun activeWorkoutUsesOneFocusedComposerWithExerciseAndSetReordering() {
        val exercise = testExercise().copy(name = "Bench press")
        val workoutExercise = testWorkoutExercise(exercise)
        val first = testWorkoutSet(4, workoutExercise.id)
        val second = testWorkoutSet(5, workoutExercise.id).copy(position = 1)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(compose.density.density, fontScale = 2f)) {
                WhipTheme(dynamicColor = false) {
                    androidx.compose.foundation.layout.Box(Modifier.width(320.dp)) {
                        WorkoutExerciseCard(
                    item = WorkoutExerciseUi(workoutExercise, exercise, listOf(first, second), emptyList(), 0, null, null),
                    preferredWeightUnitId = "kilogram",
                    preferredDistanceUnitId = "kilometre",
                    numberPrecision = 1,
                    compactRows = false,
                    showRpe = false,
                    showRir = false,
                    nextSetId = second.id,
                    nextInGroup = false,
                    arranging = true,
                    canMoveUp = false,
                    canMoveDown = true,
                    onMoveUp = {},
                    onMoveDown = {},
                    onRemoveExercise = {},
                    onSubstituteExercise = {},
                    onAddSet = {},
                    onEditSet = {},
                    onEditNotes = {},
                    onCompleteSet = { _, _ -> },
                    onSaveQuickSet = { _, _, _ -> },
                    onDuplicateSet = {},
                    onDeleteSet = {},
                    onUndoDeleteSet = {},
                    onReorderSets = {},
                        )
                    }
                }
            }
        }

        compose.onAllNodesWithContentDescription("Reorder Bench press").assertCountEquals(1)
        compose.onAllNodes(hasTestTag("active-set-composer")).assertCountEquals(1)
        compose.onAllNodesWithContentDescription("Reorder set 1").assertCountEquals(1)
        compose.onAllNodesWithContentDescription("Reorder set 2").assertCountEquals(1)
        compose.onNodeWithText("Set 2", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("quick-set-load-${second.id}").assertIsDisplayed()
        compose.onNodeWithTag("quick-set-reps-${second.id}").assertIsDisplayed()
        compose.onNodeWithTag("quick-set-save-next-${second.id}").assertIsDisplayed()
        val firstBounds = compose.onNodeWithContentDescription("Reorder set 1").fetchSemanticsNode().boundsInRoot
        val focusedBounds = compose.onNodeWithTag("active-set-composer").fetchSemanticsNode().boundsInRoot
        check(firstBounds.bottom <= focusedBounds.top) {
            "Up Next composer must remain in set order instead of being extracted above set 1: first=$firstBounds focused=$focusedBounds"
        }
    }

    @Test
    fun substitutingAnExerciseWithSavedSetsDisclosesReplacementBeforeOpeningPicker() {
        val exercise = testExercise().copy(name = "Bench press")
        val workoutExercise = testWorkoutExercise(exercise)
        val savedSet = testWorkoutSet(4, workoutExercise.id).copy(completed = true, completedAtMillis = 2)
        var substitutionRequested = false
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WorkoutExerciseCard(
                    item = WorkoutExerciseUi(workoutExercise, exercise, listOf(savedSet), emptyList(), 0, null, null),
                    preferredWeightUnitId = "kilogram",
                    preferredDistanceUnitId = "kilometre",
                    numberPrecision = 1,
                    compactRows = false,
                    showRpe = false,
                    showRir = false,
                    nextSetId = null,
                    nextInGroup = false,
                    canMoveUp = false,
                    canMoveDown = false,
                    onMoveUp = {},
                    onMoveDown = {},
                    onRemoveExercise = {},
                    onSubstituteExercise = { substitutionRequested = true },
                    onAddSet = {},
                    onEditSet = {},
                    onEditNotes = {},
                    onCompleteSet = { _, _ -> },
                    onSaveQuickSet = { _, _, _ -> },
                    onDuplicateSet = {},
                    onDeleteSet = {},
                    onUndoDeleteSet = {},
                    onReorderSets = {},
                )
            }
        }

        compose.onNodeWithContentDescription("More options for Bench press").performClick()
        compose.onNodeWithText("Substitute Exercise").performClick()
        compose.onNodeWithText("Replace Bench press?").assertIsDisplayed()
        compose.onNodeWithText(
            "Completed sets stay attached to Bench press in History. Unperformed sets are marked as replaced; the new exercise is logged separately.",
        )
            .assertIsDisplayed()
        compose.runOnIdle { assertEquals(false, substitutionRequested) }
        compose.onNodeWithText("Choose Replacement").performClick()
        compose.runOnIdle { assertEquals(true, substitutionRequested) }
    }

    @Test
    fun requiredMainSetUsesExplicitNotPerformedWarningInsteadOfGenericRemoval() {
        val exercise = testExercise().copy(name = "Deadlift")
        val workoutExercise = testWorkoutExercise(exercise)
        val mainSet = testWorkoutSet(44, workoutExercise.id).copy(
            workSectionSnapshot = RoutineWorkSection.Main,
            requiredForProgressionSnapshot = true,
        )
        val reviewedBoundary = WorkoutSetMutationBoundary(
            sessionId = workoutExercise.sessionId,
            sessionUuid = "session-reviewed",
            workoutRevision = 3,
            workoutExerciseId = workoutExercise.id,
            workoutExerciseUuid = workoutExercise.uuid,
            setId = mainSet.id,
            setUuid = mainSet.uuid,
            setUpdatedAtMillis = mainSet.updatedAtMillis,
            expectedDeletedAtMillis = null,
            expectedRemovalReason = null,
        )
        val latestBoundary = mutableStateOf(reviewedBoundary)
        var submittedBoundary: WorkoutSetMutationBoundary? = null
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                val currentBoundary = latestBoundary.value
                WorkoutExerciseCard(
                    item = WorkoutExerciseUi(workoutExercise, exercise, listOf(mainSet), emptyList(), 0, null, null),
                    preferredWeightUnitId = "kilogram",
                    preferredDistanceUnitId = "kilometre",
                    numberPrecision = 1,
                    compactRows = false,
                    showRpe = false,
                    showRir = false,
                    nextSetId = null,
                    nextInGroup = false,
                    canMoveUp = false,
                    canMoveDown = false,
                    onMoveUp = {},
                    onMoveDown = {},
                    onRemoveExercise = {},
                    onSubstituteExercise = {},
                    onAddSet = {},
                    onEditSet = {},
                    onEditNotes = {},
                    onCompleteSet = { _, _ -> },
                    onSaveQuickSet = { _, _, _ -> },
                    onDuplicateSet = {},
                    captureSetBoundary = { currentBoundary },
                    onDeleteSet = { submittedBoundary = it },
                    onUndoDeleteSet = {},
                    onReorderSets = {},
                )
            }
        }

        compose.onAllNodesWithContentDescription("Reorder Deadlift").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Reorder set 1").assertCountEquals(0)
        compose.onNodeWithContentDescription("Manage set 1").performClick()
        compose.onNodeWithText("Mark Main Set Not Performed").performClick()
        compose.onNodeWithText("Mark Main Set not performed?").assertIsDisplayed()
        compose.onNodeWithText("It can hold this lift's Training Max progression", substring = true).assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(null, submittedBoundary)
            latestBoundary.value = reviewedBoundary.copy(
                workoutRevision = 4,
                setUpdatedAtMillis = reviewedBoundary.setUpdatedAtMillis + 1,
            )
        }
        compose.onNodeWithText("Mark Not Performed").performClick()
        compose.runOnIdle { assertEquals(reviewedBoundary, submittedBoundary) }
    }

    @Test
    fun destructiveFailureStaysInReviewAndCancelClearsItsOwnedError() {
        val exercise = testExercise().copy(name = "Paused squat")
        val workoutExercise = testWorkoutExercise(exercise)
        val mainSet = testWorkoutSet(45, workoutExercise.id).copy(
            workSectionSnapshot = RoutineWorkSection.Main,
            requiredForProgressionSnapshot = true,
        )
        val error = mutableStateOf<String?>(null)
        var clearCount = 0
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WorkoutExerciseCard(
                    item = WorkoutExerciseUi(workoutExercise, exercise, listOf(mainSet), emptyList(), 0, null, null),
                    preferredWeightUnitId = "kilogram",
                    preferredDistanceUnitId = "kilometre",
                    numberPrecision = 1,
                    compactRows = false,
                    showRpe = false,
                    showRir = false,
                    nextSetId = null,
                    nextInGroup = false,
                    canMoveUp = false,
                    canMoveDown = false,
                    onMoveUp = {},
                    onMoveDown = {},
                    sessionMutationError = error.value,
                    onClearSessionMutationError = { error.value = null; clearCount += 1 },
                    onRemoveExercise = {},
                    onSubstituteExercise = {},
                    onAddSet = {},
                    onEditSet = {},
                    onEditNotes = {},
                    onCompleteSet = { _, _ -> },
                    onSaveQuickSet = { _, _, _ -> },
                    onDuplicateSet = {},
                    onDeleteSet = {},
                    onUndoDeleteSet = {},
                    onReorderSets = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Manage set 1").performClick()
        compose.onNodeWithText("Mark Main Set Not Performed").performClick()
        compose.runOnIdle { error.value = "Set changed after review" }
        compose.onNodeWithText("Set changed after review").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle {
            assertEquals(2, clearCount)
            assertEquals(null, error.value)
        }
        compose.onNodeWithContentDescription("Manage set 1").performClick()
        compose.onNodeWithText("Mark Main Set Not Performed").performClick()
        compose.onAllNodesWithText("Set changed after review").assertCountEquals(0)
    }

    @Test
    fun groupedExerciseUsesOneClearHeaderAndCanRemoveItsDesignation() {
        val exercise = testExercise().copy(name = "Bench press")
        val group = WorkoutGroup(
            id = 20,
            uuid = "upper-superset",
            sessionId = 3,
            name = "Upper A",
            type = WorkoutGroupType.Superset,
            position = 0,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        val workoutExercise = testWorkoutExercise(exercise).copy(groupId = group.id)
        var removalRequested = false
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WorkoutExerciseGroupSurface(
                    group = group,
                    exerciseCount = 2,
                    canMoveUp = false,
                    canMoveDown = true,
                    onMoveUp = {},
                    onMoveDown = {},
                ) {
                    WorkoutExerciseCard(
                        item = WorkoutExerciseUi(workoutExercise, exercise, emptyList(), emptyList(), 0, group, null),
                        preferredWeightUnitId = "kilogram",
                        preferredDistanceUnitId = "kilometre",
                        numberPrecision = 1,
                        compactRows = false,
                        showRpe = false,
                        showRir = false,
                        nextSetId = null,
                        nextInGroup = false,
                        canMoveUp = false,
                        canMoveDown = false,
                        onMoveUp = {},
                        onMoveDown = {},
                        onRemoveExercise = {},
                        onRemoveFromGroup = { removalRequested = true },
                        onSubstituteExercise = {},
                        onAddSet = {},
                        onEditSet = {},
                        onEditNotes = {},
                        onCompleteSet = { _, _ -> },
                        onSaveQuickSet = { _, _, _ -> },
                        onDuplicateSet = {},
                        onDeleteSet = {},
                        onUndoDeleteSet = {},
                        onReorderSets = {},
                    )
                }
            }
        }

        compose.onNodeWithText("Superset").assertIsDisplayed()
        compose.onNodeWithText("Upper A · 2 exercises").assertIsDisplayed()
        compose.onNodeWithContentDescription("Superset group, 2 exercises").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Reorder Superset group").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Reorder Bench press").assertCountEquals(0)
        compose.onNodeWithContentDescription("More options for Bench press").performClick()
        compose.onNodeWithText("Remove from Superset").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(removalRequested) }
    }

    @Test
    fun genericGroupNamesNeverRepeatOrConflictWithTheSelectedType() {
        val group = WorkoutGroup(
            id = 20,
            uuid = "circuit",
            sessionId = 3,
            name = "Superset",
            type = WorkoutGroupType.Circuit,
            position = 0,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        assertEquals(null, group.customDisplayName())

        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WorkoutExerciseGroupSurface(
                    group = group,
                    exerciseCount = 3,
                    canMoveUp = false,
                    canMoveDown = false,
                    onMoveUp = {},
                    onMoveDown = {},
                ) {}
            }
        }

        compose.onNodeWithText("Circuit").assertIsDisplayed()
        compose.onNodeWithText("3 exercises").assertIsDisplayed()
        compose.onAllNodesWithText("Superset").assertCountEquals(0)
    }

    @Test
    fun groupedWorkoutExercisesMoveAsABlockAndReorderWithinTheirGroup() {
        val group = WorkoutGroup(
            id = 20,
            uuid = "circuit",
            sessionId = 3,
            name = "Circuit",
            type = WorkoutGroupType.Circuit,
            position = 0,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        fun item(id: Long, position: Int, grouped: Boolean): WorkoutExerciseUi {
            val exercise = testExercise().copy(id = id, uuid = "exercise-$id", name = "Exercise $id")
            val placement = testWorkoutExercise(exercise).copy(
                id = id,
                uuid = "placement-$id",
                position = position,
                groupId = group.id.takeIf { grouped },
            )
            return WorkoutExerciseUi(
                placement,
                exercise,
                emptyList(),
                emptyList(),
                0,
                group.takeIf { grouped },
                null,
            )
        }
        val firstMember = item(10, 0, grouped = true)
        val independent = item(11, 1, grouped = false)
        val secondMember = item(12, 2, grouped = true)

        val blocks = buildWorkoutExerciseBlocks(listOf(firstMember, independent, secondMember))

        assertEquals(2, blocks.size)
        assertEquals(listOf(10L, 12L), blocks.first().exercises.map { it.workoutExercise.id })
        assertEquals(listOf(11L, 10L, 12L), reorderWorkoutBlock(blocks, 0, 1))
        assertEquals(listOf(12L, 10L, 11L), reorderWorkoutGroupMember(blocks, 0, 0, 1))
    }

    @Test
    fun completedSetsStayQuietUntilTheUserExpandsThem() {
        val exercise = testExercise().copy(name = "Bench press")
        val workoutExercise = testWorkoutExercise(exercise)
        val completed = testWorkoutSet(4, workoutExercise.id).copy(completed = true, completedAtMillis = 2)
        val next = testWorkoutSet(5, workoutExercise.id).copy(position = 1)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WorkoutExerciseCard(
                    item = WorkoutExerciseUi(workoutExercise, exercise, listOf(completed, next), emptyList(), 0, null, null),
                    preferredWeightUnitId = "kilogram",
                    preferredDistanceUnitId = "kilometre",
                    numberPrecision = 1,
                    compactRows = false,
                    showRpe = false,
                    showRir = false,
                    nextSetId = next.id,
                    nextInGroup = false,
                    canMoveUp = false,
                    canMoveDown = false,
                    onMoveUp = {},
                    onMoveDown = {},
                    onRemoveExercise = {},
                    onSubstituteExercise = {},
                    onAddSet = {},
                    onEditSet = {},
                    onEditNotes = {},
                    onCompleteSet = { _, _ -> },
                    onSaveQuickSet = { _, _, _ -> },
                    onDuplicateSet = {},
                    onDeleteSet = {},
                    onUndoDeleteSet = {},
                    onReorderSets = {},
                )
            }
        }

        compose.onNodeWithText("1 Completed Set").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Reorder set 1").assertCountEquals(0)
        compose.onNodeWithTag("active-set-composer").assertIsDisplayed()
        compose.onNodeWithText("1 Completed Set").performClick()
        compose.onNodeWithText("Set 1", substring = true).assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Reorder set 1").assertCountEquals(0)
        compose.onNodeWithContentDescription("Manage set 1").assertIsDisplayed()
    }

    @Test
    fun pristineQuickSetDefersRequiredErrorsUntilCompletionAttempt() {
        val exercise = testExercise()
        val workoutExercise = testWorkoutExercise(exercise)
        val set = testWorkoutSet(4, workoutExercise.id).copy(
            canonicalWeightKg = null,
            enteredWeight = null,
            repetitions = null,
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                QuickSetEntry(
                    set = set,
                    exercise = exercise,
                    workoutExercise = workoutExercise,
                    machine = null,
                    preferredWeightUnitId = "kilogram",
                    preferredDistanceUnitId = "kilometre",
                    showRpe = false,
                    showRir = false,
                    onMoreDetails = {},
                    onSave = { _, _, _ -> },
                )
            }
        }

        compose.onAllNodes(hasText("Required")).assertCountEquals(0)
        compose.onAllNodes(hasText("Enter at least 1")).assertCountEquals(0)
        compose.onNodeWithTag("quick-set-save-next-${set.id}").performClick()
        compose.onNodeWithText("Required").assertIsDisplayed()
        compose.onNodeWithText("Enter at least 1").assertIsDisplayed()
    }

    @Test
    fun previousSetSuggestionPopulatesPrimaryInputs() {
        val exercise = testExercise()
        val workoutExercise = testWorkoutExercise(exercise)
        val set = testWorkoutSet(4, workoutExercise.id).copy(
            canonicalWeightKg = null,
            enteredWeight = null,
            repetitions = null,
        )
        val previous = testWorkoutSet(3, workoutExercise.id).copy(
            canonicalWeightKg = 55.0,
            enteredWeight = 55.0,
            repetitions = 8,
            completed = true,
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                QuickSetEntry(
                    set = set,
                    exercise = exercise,
                    workoutExercise = workoutExercise,
                    machine = null,
                    preferredWeightUnitId = "kilogram",
                    preferredDistanceUnitId = "kilometre",
                    showRpe = false,
                    showRir = false,
                    suggestedSet = previous,
                    onMoreDetails = {},
                    onSave = { _, _, _ -> },
                )
            }
        }

        compose.onNodeWithTag("quick-set-use-last-${set.id}").performClick()
        compose.onNodeWithTag("quick-set-load-${set.id}").assertTextContains("55")
        compose.onNodeWithTag("quick-set-reps-${set.id}").assertTextContains("8")
    }

    @Test
    fun quickSetKeepsItsDraftAndBoundaryThroughStateRestoration() {
        val restoration = StateRestorationTester(compose)
        val exercise = testExercise()
        val workoutExercise = testWorkoutExercise(exercise)
        val opened = testWorkoutSet(4, workoutExercise.id).copy(
            uuid = "set-opened",
            updatedAtMillis = 40,
        )
        var displayedSet by mutableStateOf(opened)
        var committedBoundary: QuickSetAuthorshipBoundary? = null
        var committedDraft: WorkoutSetDraft? = null
        restoration.setContent {
            WhipTheme(dynamicColor = false) {
                QuickSetEntry(
                    set = displayedSet,
                    exercise = exercise,
                    workoutExercise = workoutExercise,
                    machine = null,
                    preferredWeightUnitId = "kilogram",
                    preferredDistanceUnitId = "kilometre",
                    showRpe = false,
                    showRir = false,
                    workoutRevision = 9,
                    onMoreDetails = {},
                    onSave = { boundary, draft, _ ->
                        committedBoundary = boundary
                        committedDraft = draft
                    },
                )
            }
        }

        compose.onNodeWithTag("quick-set-load-${opened.id}").performTextReplacement("62.5")
        compose.onNodeWithTag("quick-set-reps-${opened.id}").performTextReplacement("8")
        compose.runOnIdle {
            displayedSet = opened.copy(
                uuid = "set-newer-details-save",
                updatedAtMillis = 41,
                canonicalWeightKg = 100.0,
                enteredWeight = 100.0,
                repetitions = 1,
            )
        }
        restoration.emulateSavedInstanceStateRestore()

        compose.onNodeWithTag("quick-set-load-${opened.id}").assertTextContains("62.5")
        compose.onNodeWithTag("quick-set-reps-${opened.id}").assertTextContains("8")
        compose.onNodeWithTag("quick-set-save-next-${opened.id}").performClick()
        compose.runOnIdle {
            val boundary = requireNotNull(committedBoundary)
            assertEquals("set-opened", boundary.setUuid)
            assertEquals(40, boundary.setUpdatedAtMillis)
            assertEquals(9, boundary.workoutRevision)
            val draft = requireNotNull(committedDraft)
            assertEquals(62.5, draft.weight ?: error("Missing restored weight"), 0.0)
            assertEquals(8, draft.reps)
        }
    }

    @Test
    fun quickSetShowsOnlyOneEffortScaleWhenLegacyPreferencesEnableBoth() {
        val exercise = testExercise()
        val workoutExercise = testWorkoutExercise(exercise)
        val set = testWorkoutSet(4, workoutExercise.id)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                QuickSetEntry(
                    set = set,
                    exercise = exercise,
                    workoutExercise = workoutExercise,
                    machine = null,
                    preferredWeightUnitId = "kilogram",
                    preferredDistanceUnitId = "kilometre",
                    showRpe = true,
                    showRir = true,
                    onMoreDetails = {},
                    onSave = { _, _, _ -> },
                )
            }
        }

        compose.onNodeWithText("RPE (1–10)").assertIsDisplayed()
        compose.onAllNodes(hasText("RIR (0–10)")).assertCountEquals(0)
    }

    @Test
    fun workoutHistoryCardUsesOneStableActionPerExerciseAndMovesSecondaryActionsToMenu() {
        val exercises = listOf(
            testExercise().copy(id = 1, name = "Flat Barbell Bench Press"),
            testExercise().copy(id = 2, name = "Lat Pulldown"),
            testExercise().copy(id = 3, name = "Dumbbell Bicep Curl"),
        )
        val placements = exercises.mapIndexed { index, exercise ->
            testWorkoutExercise(exercise).copy(
                id = (index + 10).toLong(),
                exerciseId = exercise.id,
                position = index,
                machineNameSnapshot = "Rack A".takeIf { index == 0 }.orEmpty(),
                machineConfigurationSnapshot = "Safety 8 · Bench 3".takeIf { index == 0 }.orEmpty(),
            )
        }
        val sets = placements.flatMapIndexed { index, placement ->
            List(index + 1) { setIndex ->
                testWorkoutSet((index * 10 + setIndex + 1).toLong(), placement.id).copy(
                    completed = true,
                    classification = if (index == 0) WorkoutSetClassification.Amrap else WorkoutSetClassification.Working,
                    enteredWeight = if (index == 0) 85.0 else 50.0,
                    canonicalWeightKg = if (index == 0) 85.0 else 50.0,
                    repetitions = if (index == 0) 7 else 5,
                    rpe = 9.0.takeIf { index == 0 },
                    rir = 1.0.takeIf { index == 0 },
                    tempo = "3-1-1".takeIf { index == 0 }.orEmpty(),
                    restSeconds = if (index == 0) 180 else 120,
                    note = "Strong set".takeIf { index == 0 }.orEmpty(),
                    prescribedEnteredWeight = 82.5.takeIf { index == 0 },
                    prescribedWeightUnitId = "kilogram".takeIf { index == 0 },
                    prescribedRepetitions = 5.takeIf { index == 0 },
                    prescribedRepetitionsMax = 5.takeIf { index == 0 },
                    prescriptionSourceLabel = "85% TM".takeIf { index == 0 }.orEmpty(),
                )
            }
        }
        val session = testHistorySession().copy(
            sourceRoutineProgramKind = RoutineProgramKind.FiveThreeOneClassic,
            sourceRoutinePhaseIndex = 2,
            sourceRoutinePhaseLabel = "Anchor 1",
            sourceRoutinePhaseRole = RoutineProgramPhaseRole.Anchor,
            sourceRoutineCycle = 3,
            sourceRoutineDayPosition = 1,
            sourceRoutineDayProgressionIndex = 4,
            programProgressAdvanced = false,
        )
        compose.setContent {
            var expanded by remember { mutableStateOf(false) }
            var menuExpanded by remember { mutableStateOf(false) }
            WhipTheme(darkTheme = true, dynamicColor = false) {
                WorkoutHistoryCard(
                    session = session,
                    workoutExercises = placements,
                    sets = sets,
                    exerciseById = exercises.associateBy(Exercise::id),
                    expanded = expanded,
                    archivedView = false,
                    hasActiveWorkout = false,
                    menuExpanded = menuExpanded,
                    onToggleExpanded = { expanded = !expanded },
                    onMenuExpandedChange = { menuExpanded = it },
                    onRepeatWorkout = {},
                    onOpenActiveWorkout = {},
                    onEditDetails = {},
                    onResume = {},
                    onSaveAsRoutine = {},
                    onShare = {},
                    onRestore = {},
                    onDelete = {},
                    onReuseExercise = {},
                )
            }
        }

        compose.onNodeWithTag("history-workout-preview-${session.id}").assertIsDisplayed()
        compose.onNodeWithText("Flat Barbell Bench Press · Lat Pulldown · Dumbbell Bicep Curl").assertIsDisplayed()
        compose.onAllNodesWithText("View Details").assertCountEquals(0)
        compose.onAllNodesWithText("Use Again").assertCountEquals(0)

        compose.onNodeWithTag("history-workout-toggle-${session.id}").performClick()
        compose.onNodeWithTag("history-program-snapshot-${session.id}")
            .assertTextContains(
                "Program snapshot · Classic 5/3/1 · Cycle 3 · Anchor 1 · Anchor · Day 2 · Day progression 5 · Did not advance program progress",
            )
        compose.onAllNodesWithText("Use Again").assertCountEquals(3)
        compose.onNodeWithText("Repeat Workout").assertExists()
        compose.onNodeWithText("Equipment: Rack A", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Setup: Safety 8 · Bench 3").assertIsDisplayed()
        compose.onNodeWithTag("history-set-performed-${sets.first().id}")
            .assertTextContains("Performed · 85 kg × 7 reps")
        compose.onNodeWithTag("history-set-target-${sets.first().id}")
            .assertTextContains("85% TM", substring = true)
            .assertTextContains("82.5 kg entered", substring = true)
            .assertTextContains("5+ reps", substring = true)
        compose.onNodeWithText("RPE 9 · RIR 1 · 3:00 rest · Tempo 3-1-1").assertIsDisplayed()
        compose.onNodeWithText("Note · Strong set").assertIsDisplayed()
        compose.onAllNodesWithText("Copy Flat Barbell Bench Press Sets").assertCountEquals(0)
        compose.onAllNodesWithText("Edit Details").assertCountEquals(0)
        compose.onAllNodesWithText("Resume Workout").assertCountEquals(0)
        placements.forEach { placement ->
            val row = compose.onNodeWithTag("history-exercise-row-${placement.id}").fetchSemanticsNode().boundsInRoot
            val action = compose.onNodeWithTag("history-exercise-reuse-${placement.id}").fetchSemanticsNode().boundsInRoot
            check(action.left >= row.left && action.right <= row.right && action.top >= row.top && action.bottom <= row.bottom) {
                "Exercise reuse action must remain inside its row: row=$row action=$action"
            }
        }

        compose.onNodeWithTag("history-workout-menu-${session.id}").performClick()
        compose.onNodeWithText("Edit Details").assertIsDisplayed()
        compose.onNodeWithText("Resume Original Workout").assertIsDisplayed()
        compose.onNodeWithText("Save as Routine").assertIsDisplayed()
        assertEquals(
            "Program snapshot · Classic 5/3/1 · Cycle 3 · Anchor 1 · Anchor · Day 2 · Day progression 5 · Did not advance program progress",
            workoutProgramSnapshotLabel(session),
        )
    }

    @Test
    fun workoutHistoryExplainsReplacementAndUnperformedSetOutcome() {
        val bench = testExercise().copy(id = 1, name = "Back Squat")
        val zercher = testExercise().copy(id = 2, name = "Zercher Squat")
        val replacement = testWorkoutExercise(zercher).copy(id = 12, uuid = "replacement", exerciseId = 2)
        val original = testWorkoutExercise(bench).copy(
            id = 11,
            uuid = "original",
            exerciseId = 1,
            outcome = WorkoutExerciseOutcome.Substituted,
            replacementWorkoutExerciseUuid = replacement.uuid,
        )
        val completed = testWorkoutSet(21, original.id).copy(completed = true, repetitions = 5)
        val unperformed = testWorkoutSet(22, original.id).copy(
            deletedAtMillis = 10,
            removalReason = WorkoutSetRemovalReason.ExerciseSubstituted,
        )
        val session = testHistorySession()
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                WorkoutHistoryCard(
                    session = session,
                    workoutExercises = listOf(original, replacement),
                    sets = listOf(completed, unperformed),
                    exerciseById = listOf(bench, zercher).associateBy(Exercise::id),
                    expanded = true,
                    archivedView = false,
                    hasActiveWorkout = false,
                    menuExpanded = false,
                    onToggleExpanded = {},
                    onMenuExpandedChange = {},
                    onRepeatWorkout = {},
                    onOpenActiveWorkout = {},
                    onEditDetails = {},
                    onResume = {},
                    onSaveAsRoutine = {},
                    onShare = {},
                    onRestore = {},
                    onDelete = {},
                    onReuseExercise = {},
                )
            }
        }

        compose.onNodeWithText("Replaced by Zercher Squat during this workout").assertIsDisplayed()
        compose.onNodeWithText("Not performed · exercise replaced", substring = true).assertIsDisplayed()
    }

    private fun testHistorySession() = WorkoutSession(
        id = 50,
        uuid = "history-session",
        name = "Upper Body",
        notes = "Controlled reps",
        startedAt = Instant.parse("2026-08-26T22:00:00Z"),
        endedAt = Instant.parse("2026-08-26T22:47:00Z"),
        localDate = LocalDate.of(2026, 8, 26),
        zoneId = "UTC",
        state = WorkoutSessionState.Finished,
        keepScreenAwake = false,
        restTimerDeadlineMillis = null,
        restTimerDurationSeconds = null,
        archived = false,
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )

    private fun testWorkoutExercise(exercise: Exercise) = WorkoutExercise(
        id = 2,
        uuid = "workout-exercise",
        sessionId = 3,
        exerciseId = exercise.id,
        position = 0,
        notes = "",
        groupId = null,
        createdAtMillis = 1,
        updatedAtMillis = 1,
        loadInterpretationSnapshot = LoadInterpretation.Total,
        trackingTypeSnapshot = ExerciseTrackingType.WeightReps,
        exerciseWeightUnitSnapshot = "kilogram",
    )

    private fun testWorkoutSet(id: Long, workoutExerciseId: Long) = WorkoutSet(
        id = id,
        uuid = "set-$id",
        workoutExerciseId = workoutExerciseId,
        position = 0,
        classification = WorkoutSetClassification.Working,
        planned = false,
        completed = false,
        canonicalWeightKg = 50.0,
        enteredWeight = 50.0,
        enteredWeightUnitId = "kilogram",
        repetitions = 5,
        canonicalDistanceMetres = null,
        enteredDistance = null,
        enteredDistanceUnitId = null,
        durationSeconds = null,
        bodyweightKg = null,
        note = "",
        rpe = null,
        rir = null,
        tempo = "",
        restSeconds = 120,
        completedAtMillis = null,
        deletedAtMillis = null,
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )

    private fun testExercise() = Exercise(
        id = 1,
        uuid = "exercise",
        name = "Cable row",
        trackingType = ExerciseTrackingType.WeightReps,
        notes = "",
        equipment = "",
        primaryMuscles = "",
        secondaryMuscles = "",
        weightUnitId = "kilogram",
        weightIncrement = 2.5,
        repetitionIncrement = 1,
        defaultRestSeconds = 120,
        defaultGraphMetric = "EstimatedOneRepMax",
        oneRepMaxFormula = EstimatedOneRepMaxFormula.Epley,
        barWeightKg = 20.0,
        availablePlatesKg = emptyList(),
        includeInVolume = true,
        includeInPersonalRecords = true,
        bodyweightLoadPolicy = BodyweightLoadPolicy.ExternalWeightOnly,
        effectiveBodyweightPercent = 100.0,
        showRpe = null,
        showRir = null,
        showTempo = null,
        favorite = false,
        position = 0,
        archived = false,
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )

    private fun exerciseDeletionImpact(
        activePlacements: Int = 0,
        trainingMaxDecisionCount: Int = 0,
    ) = ExerciseDeletionImpact(
        exerciseId = 7,
        displayName = "Zercher squat",
        activePlacements = activePlacements,
        routinePlacementCount = 2,
        routineSetCount = 16,
        routineAlternativeReferenceCount = 1,
        workoutPlacementCount = 9,
        workoutSetCount = 43,
        linkRuleCount = 1,
        linkConditionCount = 1,
        linkConditionChoiceCount = 0,
        contributionCount = 2,
        automationRuleCount = 1,
        automationConditionCount = 1,
        automationConditionChoiceCount = 0,
        automationMappingCount = 1,
        automationOccurrenceCount = 4,
        linkedTrackEntryCount = 3,
        graphPresetUpdateCount = 1,
        graphPresetDeleteCount = 0,
        personalRecordCount = 2,
        trainingMaxDecisionCount = trainingMaxDecisionCount,
        machineReferenceCount = 1,
        categoryReferenceCount = 2,
        revisionToken = "exercise-revision",
    )
}
