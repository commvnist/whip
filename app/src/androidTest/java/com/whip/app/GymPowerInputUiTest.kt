package com.whip.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.BodyweightLoadPolicy
import com.whip.app.domain.EstimatedOneRepMaxFormula
import com.whip.app.domain.Exercise
import com.whip.app.domain.ExerciseTrackingType
import com.whip.app.domain.LoadInterpretation
import com.whip.app.domain.GymMachineDraft
import com.whip.app.domain.MachineLevelDirection
import com.whip.app.domain.WorkoutExercise
import com.whip.app.domain.WorkoutSession
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.domain.WorkoutSet
import com.whip.app.domain.WorkoutSetClassification
import com.whip.app.core.DEFAULT_REST_TIMER_PRESET_SECONDS
import com.whip.app.ui.ExerciseEditorDialog
import com.whip.app.ui.MachineEditorDialog
import com.whip.app.ui.MachinePermanentDeleteDialog
import com.whip.app.ui.QuickSetEntry
import com.whip.app.ui.RestTimerCard
import com.whip.app.ui.WorkoutExerciseCard
import com.whip.app.ui.WorkoutExerciseUi
import com.whip.app.ui.WorkoutHistoryCard
import com.whip.app.data.MachineDeletionImpact
import com.whip.app.ui.theme.WhipTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import java.time.LocalDate
import java.time.Instant

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
        compose.onNodeWithText("Save Presets").performClick()
        compose.onNodeWithText("0:45").assertIsDisplayed()
        compose.runOnIdle { check(savedPresets?.contains(45) == true) }
        compose.onNodeWithContentDescription("Increase workout rest time by 15").performClick()
        compose.onNodeWithText("Use for This Workout").performClick()
        compose.onNodeWithText("Rest · 2:15").assertIsDisplayed()
        compose.onNodeWithText("Start").performClick()
        compose.runOnIdle { assertEquals(135, startedWith) }
    }

    @Test
    fun activeWorkoutUsesOneFocusedComposerWithoutExecutionTimeSetHandles() {
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

        compose.onAllNodesWithContentDescription("Reorder Bench press").assertCountEquals(0)
        compose.onAllNodes(hasTestTag("active-set-composer")).assertCountEquals(1)
        compose.onAllNodesWithContentDescription("Reorder set 1").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Reorder set 2").assertCountEquals(0)
        compose.onNodeWithText("Set 2", substring = true).assertIsDisplayed()
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
        compose.onNodeWithText("Choosing a replacement removes 1 saved set from this workout. This cannot be undone.")
            .assertIsDisplayed()
        compose.runOnIdle { assertEquals(false, substitutionRequested) }
        compose.onNodeWithText("Choose Replacement").performClick()
        compose.runOnIdle { assertEquals(true, substitutionRequested) }
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
                    onSave = { _, _ -> },
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
                    onSave = { _, _ -> },
                )
            }
        }

        compose.onNodeWithTag("quick-set-use-last-${set.id}").performClick()
        compose.onNodeWithTag("quick-set-load-${set.id}").assertTextContains("55")
        compose.onNodeWithTag("quick-set-reps-${set.id}").assertTextContains("8")
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
                    onSave = { _, _ -> },
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
            testWorkoutExercise(exercise).copy(id = (index + 10).toLong(), exerciseId = exercise.id, position = index)
        }
        val sets = placements.flatMapIndexed { index, placement ->
            List(index + 1) { setIndex ->
                testWorkoutSet((index * 10 + setIndex + 1).toLong(), placement.id).copy(completed = true)
            }
        }
        val session = testHistorySession()
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
        compose.onAllNodesWithText("Use Again").assertCountEquals(3)
        compose.onNodeWithText("Repeat Workout").assertIsDisplayed()
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
}
