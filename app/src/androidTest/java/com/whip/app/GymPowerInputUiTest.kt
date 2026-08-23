package com.whip.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.BodyweightLoadPolicy
import com.whip.app.domain.EstimatedOneRepMaxFormula
import com.whip.app.domain.Exercise
import com.whip.app.domain.ExerciseTrackingType
import com.whip.app.domain.LoadInterpretation
import com.whip.app.domain.WorkoutExercise
import com.whip.app.domain.WorkoutSet
import com.whip.app.domain.WorkoutSetClassification
import com.whip.app.ui.ExerciseEditorDialog
import com.whip.app.ui.MachineEditorDialog
import com.whip.app.ui.MachinePermanentDeleteDialog
import com.whip.app.ui.QuickSetEntry
import com.whip.app.ui.WorkoutExerciseCard
import com.whip.app.ui.WorkoutExerciseUi
import com.whip.app.data.MachineDeletionImpact
import com.whip.app.ui.theme.WhipTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class GymPowerInputUiTest {
    @get:Rule
    val compose = createComposeRule()

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

        compose.onNodeWithText("Show advanced").performClick()
        compose.onNodeWithText("lb").performScrollTo().performClick()
        compose.onNodeWithText("Convert default values").performClick()
        compose.onNodeWithTag("exercise-editor-list").performScrollToNode(hasTestTag("exercise-weight-increment"))
        compose.onNodeWithTag("exercise-weight-increment").assertTextContains("5")
        compose.onNodeWithTag("exercise-editor-list").performScrollToNode(hasTestTag("exercise-bar-weight"))
        compose.onNodeWithTag("exercise-bar-weight").assertTextContains("45")
        compose.onNodeWithTag("exercise-plates").assertTextContains("45,35,25,10,5,2.5")
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

        compose.onNodeWithTag("machine-editor-list").performScrollToNode(hasText("Numbered stack / level"))
        compose.onNodeWithText("Numbered stack / level").performClick()
        compose.onNodeWithTag("machine-editor-list").performScrollToNode(hasTestTag("machine-load-spec"))
        compose.onNodeWithTag("machine-load-spec").assertTextContains("1-10")
        compose.onNodeWithTag("machine-editor-list").performScrollToNode(hasTestTag("machine-load-increment"))
        compose.onNodeWithTag("machine-load-increment").assertTextContains("1")
        compose.onNodeWithTag("machine-editor-list").performScrollToNode(hasTestTag("machine-load-preview"))
        compose.onNodeWithTag("machine-load-preview").assertIsDisplayed().assertTextContains("Preview · 10 values · 1-10 by 1")
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

        compose.onNodeWithText("Delete “Downtown cable stack” v2 permanently?").assertIsDisplayed()
        compose.onNodeWithText("Kept").assertIsDisplayed()
        compose.onNodeWithText("Needs attention").assertIsDisplayed()
        compose.onNodeWithText("Active workout").assertIsDisplayed()
        compose.onNodeWithTag("machine-delete-confirm").assertIsNotEnabled()
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
                    onSave = { _, _ -> },
                )
            }
        }

        val load = compose.onNodeWithTag("quick-set-load-${set.id}").fetchSemanticsNode().boundsInRoot
        val reps = compose.onNodeWithTag("quick-set-reps-${set.id}").fetchSemanticsNode().boundsInRoot
        val saveNext = compose.onNodeWithTag("quick-set-save-next-${set.id}").fetchSemanticsNode().boundsInRoot
        check(load.bottom <= saveNext.top && reps.bottom <= saveNext.top) {
            "Quick-set actions must follow data entry: load=$load reps=$reps saveNext=$saveNext"
        }
    }

    @Test
    fun activeWorkoutUsesOneCompletionPathAndDirectAccessibleReordering() {
        val exercise = testExercise().copy(name = "Bench press")
        val workoutExercise = testWorkoutExercise(exercise)
        val first = testWorkoutSet(4, workoutExercise.id)
        val second = testWorkoutSet(5, workoutExercise.id).copy(position = 1)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WorkoutExerciseCard(
                    item = WorkoutExerciseUi(workoutExercise, exercise, listOf(first, second), emptyList(), 0, null, null),
                    preferredWeightUnitId = "kilogram",
                    preferredDistanceUnitId = "kilometre",
                    numberPrecision = 1,
                    compactRows = false,
                    showRpe = false,
                    showRir = false,
                    nextSetId = first.id,
                    nextInGroup = false,
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

        val exerciseActions: List<CustomAccessibilityAction> = compose.onNodeWithContentDescription("Reorder Bench press")
            .fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertTrue(exerciseActions.any { it.label == "Move Bench press down" })
        val setActions: List<CustomAccessibilityAction> = compose.onNodeWithContentDescription("Reorder set 1")
            .fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertTrue(setActions.any { it.label == "Move set 1 down" })
        val incomplete = compose.onNodeWithContentDescription("Incomplete set 1; enter values below to save")
            .fetchSemanticsNode().config
        assertFalse(incomplete.contains(SemanticsProperties.ToggleableState))
    }

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
}
