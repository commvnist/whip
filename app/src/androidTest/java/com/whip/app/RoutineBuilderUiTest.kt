package com.whip.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.BodyweightLoadPolicy
import com.whip.app.core.AppSettings
import com.whip.app.core.RepPrescriptionScheme
import com.whip.app.domain.EstimatedOneRepMaxFormula
import com.whip.app.domain.Exercise
import com.whip.app.domain.ExerciseTrackingType
import com.whip.app.domain.PersonalRecord
import com.whip.app.domain.PersonalRecordType
import com.whip.app.domain.RoutineDayDraft
import com.whip.app.domain.RoutineDraft
import com.whip.app.domain.RoutineExerciseDraft
import com.whip.app.domain.RoutineAssistanceRole
import com.whip.app.domain.RoutinePlacementKind
import com.whip.app.domain.RoutineLoadPrescriptionType
import com.whip.app.domain.RoutineMainWorkScheme
import com.whip.app.domain.RoutineProgramKind
import com.whip.app.domain.RoutineProgramDraft
import com.whip.app.domain.RoutineSupplementalScheme
import com.whip.app.domain.RoutineTrainingMaxSource
import com.whip.app.domain.RoutineWorkSection
import com.whip.app.domain.TrainingMaxBasisKind
import com.whip.app.domain.WorkoutSetClassification
import com.whip.app.domain.WorkoutSetDraft
import com.whip.app.domain.massToKilograms
import com.whip.app.ui.GymUiState
import com.whip.app.ui.RoutineBuilderScreen
import com.whip.app.ui.theme.WhipTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoutineBuilderUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun blankRoutineSurfacesTopLevelFiveThreeOneEntry() {
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutineBuilderScreen(
                    routineId = null,
                    gymState = GymUiState(loading = false),
                    initial = null,
                    onDismiss = {},
                    onSave = { _, complete -> complete(true) },
                    onCreateExercise = { _, _ -> },
                    onCreateMachine = { _, _ -> },
                )
            }
        }

        compose.onNodeWithTag("routine-five-three-one-program-entry").assertIsDisplayed()
        compose.onNode(
            hasText("Set Up 5/3/1") and hasClickAction() and
                hasAnyAncestor(hasTestTag("routine-five-three-one-program-entry")),
        ).performClick()

        compose.onNodeWithText("Choose a layout and enter an explicit Training Max for each main lift. A Training Max is not your actual or estimated 1RM; start around 80–85% of a strong recent max and err lighter.")
            .assertIsDisplayed()
        compose.onNode(hasText("Choose Your Lifts") and hasClickAction()).assertIsDisplayed()
        compose.onNodeWithTag("five-three-one-create-standard-lifts").assertIsDisplayed()
    }

    @Test
    fun emptyLibraryCreatesUniqueStandardLiftsAndBuildsExactFourDayProgram() {
        val exercises = mutableStateOf<List<Exercise>>(emptyList())
        val createdNames = mutableListOf<String>()
        var nextExerciseId = 101L
        var savedDraft: RoutineDraft? = null
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutineBuilderScreen(
                    routineId = null,
                    gymState = GymUiState(exercises = exercises.value, loading = false),
                    initial = null,
                    onDismiss = {},
                    onSave = { draft, complete -> savedDraft = draft; complete(true) },
                    onCreateExercise = { draft, complete ->
                        val id = nextExerciseId++
                        createdNames += draft.name
                        exercises.value = exercises.value + exercise(id, draft.name)
                        complete(id)
                    },
                    onCreateMachine = { _, _ -> },
                )
            }
        }

        compose.onNode(
            hasText("Set Up 5/3/1") and hasClickAction() and
                hasAnyAncestor(hasTestTag("routine-five-three-one-program-entry")),
        ).performClick()
        compose.onNodeWithTag("five-three-one-create-standard-lifts").performClick()

        compose.waitUntil(5_000) {
            createdNames.size == 4 &&
                listOf("Squat", "Bench", "Deadlift", "Press").all { role ->
                    compose.onAllNodes(hasTestTag("five-three-one-training-max-$role"))
                        .fetchSemanticsNodes().isNotEmpty()
                }
        }
        assertEquals(listOf("Squat", "Bench Press", "Deadlift", "Overhead Press"), createdNames)
        listOf("Squat", "Bench", "Deadlift", "Press")
            .zip(listOf("100", "110", "120", "130"))
            .forEach { (role, trainingMax) ->
                compose.onNodeWithTag("five-three-one-training-max-$role")
                    .performScrollTo()
                    .performTextReplacement(trainingMax)
            }
        compose.onNodeWithTag("five-three-one-program-create").assertIsEnabled().performClick()

        listOf("Squat · 1", "Bench · 1", "Deadlift · 1", "Press · 1").forEach { day ->
            compose.onNodeWithText(day).performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithText(
            "4 phases · 3–8 active sets/phase · Main + FSL",
            substring = true,
        ).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("routine-program-structure").assertIsDisplayed()
        compose.onNodeWithTag("routine-open-program-structure").performClick()
        compose.onNodeWithTag("routine-program-structure-page").assertIsDisplayed()
        compose.onNodeWithTag("routine-program-structure-page")
            .performScrollToNode(hasText("Leader and Anchor describe block membership", substring = true))
        compose.onNodeWithText("Leader and Anchor describe block membership", substring = true)
            .assertIsDisplayed()
        compose.onNodeWithTag("routine-program-structure-page")
            .performScrollToNode(hasTestTag("routine-program-phase-role-0"))
        compose.onNodeWithTag("routine-program-phase-role-0").assertIsDisplayed()
        compose.onNodeWithTag("routine-program-structure-page")
            .performScrollToNode(hasTestTag("routine-program-phase-joker-0"))
        compose.onNodeWithTag("routine-program-phase-joker-0")
            .performClick()
            .assertIsOn()
        compose.onNodeWithTag("routine-program-structure-page")
            .performScrollToNode(hasTestTag("routine-program-phase-role-0"))
        compose.onNodeWithContentDescription("Phase role: Standard").performClick()
        compose.onNodeWithText("Deload").performClick()
        compose.onNodeWithTag("routine-program-structure-page")
            .performScrollToNode(hasTestTag("routine-program-phase-joker-0"))
        compose.onNodeWithTag("routine-program-phase-joker-0")
            .assertIsOff()
            .assertIsNotEnabled()
        compose.onNodeWithTag("routine-program-structure-page")
            .performScrollToNode(hasTestTag("routine-program-phase-tm-boundary-0"))
        compose.onNodeWithTag("routine-program-phase-tm-boundary-0").assertIsDisplayed()
        compose.onNodeWithTag("routine-program-structure-page")
            .performScrollToNode(hasTestTag("routine-program-add-phase"))
        compose.onNodeWithTag("routine-program-add-phase").performClick()
        compose.onNodeWithTag("routine-program-structure-page")
            .performScrollToNode(hasTestTag("routine-program-phase-select-4"))
        compose.onNodeWithTag("routine-program-phase-select-4").assertIsDisplayed()
        compose.onNodeWithTag("routine-program-structure-page")
            .performScrollToNode(hasTestTag("routine-program-remove-phase"))
        compose.onNodeWithTag("routine-program-remove-phase").performClick()
        compose.onNodeWithTag("routine-program-confirm-remove-phase").performClick()
        compose.onAllNodes(hasTestTag("routine-program-phase-select-4")).assertCountEquals(0)
        compose.onNodeWithContentDescription("Back in routine builder").performClick()
        compose.onNodeWithTag("routine-selected-exercises")
            .performScrollToNode(hasTestTag("routine-add-assistance-Push"))
        compose.onNodeWithTag("routine-add-assistance-Push").assertIsDisplayed()
        compose.onNodeWithTag("routine-add-assistance-Pull").assertExists()
        compose.onNodeWithTag("routine-add-assistance-SingleLegCore").assertExists()
        compose.onNodeWithTag("routine-add-assistance-Pull").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Choose Pull assistance").assertIsDisplayed()
        compose.onNodeWithText("For Squat · every exercise selected here will be assigned as Pull in this routine.")
            .assertIsDisplayed()
        compose.onNodeWithTag("routine-add-selected").assertTextEquals("Add 0 as Pull to Squat")
        compose.onNodeWithContentDescription("Back in routine builder").performClick()
        compose.onNodeWithTag("routine-builder-save").performClick()

        compose.runOnIdle {
            val draft = requireNotNull(savedDraft)
            assertEquals("4-Day 5/3/1", draft.name)
            assertEquals(listOf("Squat", "Bench", "Deadlift", "Press"), draft.days.map(RoutineDayDraft::name))
            assertEquals(listOf(101L, 102L, 103L, 104L), draft.days.map { it.exercises.single().exerciseId })
            assertEquals(listOf(100.0, 110.0, 120.0, 130.0), draft.days.map { it.exercises.single().trainingMaxValue })
            assertEquals(4, draft.days.flatMap(RoutineDayDraft::exercises).map(RoutineExerciseDraft::exerciseId).distinct().size)
            assertEquals(RoutineProgramKind.FiveThreeOne, draft.program?.kind)
            assertEquals(4, draft.program?.phaseCount)
        }
    }

    @Test
    fun assistanceIntentNamesItsSourceAndPersistsOnTheRoutinePlacement() {
        val bench = exercise(31, "Bench Press")
        val row = exercise(32, "Chest-supported row")
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutineBuilderScreen(
                    routineId = 7,
                    gymState = GymUiState(exercises = listOf(bench, row), loading = false),
                    initial = RoutineDraft(
                        name = "Assistance provenance",
                        program = RoutineProgramDraft(RoutineProgramKind.FiveThreeOne, phaseCount = 1),
                        days = listOf(
                            RoutineDayDraft(
                                "Monday",
                                listOf(
                                    RoutineExerciseDraft(
                                        exerciseId = bench.id,
                                        trainingMaxValue = 200.0,
                                        trainingMaxUnitId = "pound",
                                        cycleIncrementValue = 5.0,
                                        trainingMaxSource = RoutineTrainingMaxSource.Explicit,
                                        mainWorkScheme = RoutineMainWorkScheme.FivesPro,
                                        assistanceRole = RoutineAssistanceRole.MainLift,
                                        placementKind = RoutinePlacementKind.MainLift,
                                        plannedSets = listOf(
                                            WorkoutSetDraft(
                                                weightUnitId = "pound",
                                                reps = 5,
                                                loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                                                loadPercentage = 65.0,
                                                workSection = RoutineWorkSection.Main,
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                    onDismiss = {},
                    onSave = { _, complete -> complete(true) },
                    onCreateExercise = { _, _ -> },
                    onCreateMachine = { _, _ -> },
                )
            }
        }

        compose.onNodeWithTag("routine-selected-exercises")
            .performScrollToNode(hasTestTag("routine-add-assistance-Pull"))
        compose.onNodeWithTag("routine-add-assistance-Pull").performClick()
        compose.onNodeWithText("Choose Pull assistance").assertIsDisplayed()
        compose.onNodeWithText(
            "You make this assignment. Whip does not infer it from muscles, equipment, or Exercise Library categories.",
        ).assertIsDisplayed()
        compose.onNode(
            hasText("Chest-supported row") and hasAnyAncestor(hasTestTag("routine-exercise-picker-list")),
            useUnmergedTree = true,
        ).performClick()
        compose.onNodeWithTag("routine-add-selected").assertTextEquals("Add 1 as Pull to Monday").performClick()

        compose.onNodeWithTag("routine-selected-exercises")
            .performScrollToNode(hasText("Assistance · Pull · rep target needs review"))
        compose.onNodeWithText("Assistance · Pull · rep target needs review").assertIsDisplayed()
        compose.onNode(
            hasText("Chest-supported row") and hasAnyAncestor(hasTestTag("routine-selected-exercises")),
            useUnmergedTree = true,
        ).performClick()
        compose.onNodeWithText("Role in this routine").assertIsDisplayed()
        compose.onNodeWithTag("routine-assistance-category-Pull").assertIsSelected()
    }

    @Test
    fun customFiveThreeOneBuildsBenchDeadliftAndZercherWithoutRequiringFourStandardLifts() {
        val exercises = listOf(
            exercise(21, "Bench Press"),
            exercise(22, "Deadlift"),
            exercise(23, "Zercher Squat"),
            exercise(24, "Overhead Press"),
        )
        var savedDraft: RoutineDraft? = null
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutineBuilderScreen(
                    routineId = null,
                    gymState = GymUiState(exercises = exercises, loading = false),
                    initial = null,
                    onDismiss = {},
                    onSave = { draft, complete -> savedDraft = draft; complete(true) },
                    onCreateExercise = { _, _ -> },
                    onCreateMachine = { _, _ -> },
                )
            }
        }

        compose.onNode(
            hasText("Set Up 5/3/1") and hasClickAction() and
                hasAnyAncestor(hasTestTag("routine-five-three-one-program-entry")),
        ).performClick()
        compose.onNode(hasText("Choose Your Lifts") and hasClickAction()).performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasTestTag("five-three-one-training-max-Custom-3"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithContentDescription("Lift 1 exercise: Bench Press").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Lift 2 exercise: Deadlift").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Lift 3 exercise: Zercher Squat").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("five-three-one-remove-Custom-3").performScrollTo().performClick()
        compose.onNodeWithTag("five-three-one-add-custom-lift").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Move Bench Press later").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Lift 1 exercise: Deadlift").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Move Bench Press earlier").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Lift 1 exercise: Bench Press").performScrollTo().assertIsDisplayed()

        listOf("200", "300", "250").forEachIndexed { index, trainingMax ->
            compose.onNodeWithTag("five-three-one-training-max-Custom-$index")
                .performScrollTo()
                .performTextReplacement(trainingMax)
        }
        compose.onNodeWithTag("five-three-one-program-create").assertIsEnabled().performClick()

        listOf("Bench Press · 1", "Deadlift · 1", "Zercher Squat · 1").forEach { day ->
            compose.onNodeWithText(day).performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithTag("routine-builder-save").performClick()

        compose.runOnIdle {
            val draft = requireNotNull(savedDraft)
            assertEquals("Custom 5/3/1", draft.name)
            assertEquals(listOf("Bench Press", "Deadlift", "Zercher Squat"), draft.days.map(RoutineDayDraft::name))
            assertEquals(listOf(21L, 22L, 23L), draft.days.map { it.exercises.single().exerciseId })
            assertEquals(listOf(200.0, 300.0, 250.0), draft.days.map { it.exercises.single().trainingMaxValue })
            assertEquals(RoutineProgramKind.FiveThreeOne, draft.program?.kind)
        }
    }

    @Test
    fun trainingMaxSuggestionCopiesRoundedValueWithoutStayingLinked() {
        val exercises = listOf(
            exercise(1, "Squat"),
            exercise(2, "Bench Press"),
            exercise(3, "Deadlift"),
            exercise(4, "Overhead Press"),
        )
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutineBuilderScreen(
                    routineId = null,
                    gymState = GymUiState(exercises = exercises, loading = false),
                    initial = null,
                    onDismiss = {},
                    onSave = { _, complete -> complete(true) },
                    onCreateExercise = { _, _ -> },
                    onCreateMachine = { _, _ -> },
                )
            }
        }

        compose.onNode(
            hasText("Set Up 5/3/1") and hasClickAction() and
                hasAnyAncestor(hasTestTag("routine-five-three-one-program-entry")),
        ).performClick()
        compose.onAllNodes(hasText("Calculate from max / e1RM") and hasClickAction())[0].performClick()
        compose.onNodeWithTag("five-three-one-recent-max-Squat")
            .performScrollTo()
            .performTextReplacement("200")
        compose.onNodeWithTag("five-three-one-use-tm-suggestion-Squat")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasText("Current explicit TM · 170 lb"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Current explicit TM · 170 lb").performScrollTo().assertIsDisplayed()

        compose.onNodeWithTag("five-three-one-recent-max-Squat")
            .performTextReplacement("300")
        compose.onNodeWithText("Current explicit TM · 170 lb").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("five-three-one-use-tm-suggestion-Squat")
            .assertTextContains("Use 255 lb as Training Max")
    }

    @Test
    fun setupWizardRequiresReapplyBeforePersistingChangedTrainingMaxProvenance() {
        val squat = exercise(1, "Squat")
        var savedDraft: RoutineDraft? = null
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutineBuilderScreen(
                    routineId = null,
                    gymState = GymUiState(exercises = listOf(squat), loading = false),
                    initial = null,
                    onDismiss = {},
                    onSave = { draft, complete -> savedDraft = draft; complete(true) },
                    onCreateExercise = { _, _ -> },
                    onCreateMachine = { _, _ -> },
                )
            }
        }

        compose.onNode(
            hasText("Set Up 5/3/1") and hasClickAction() and
                hasAnyAncestor(hasTestTag("routine-five-three-one-program-entry")),
        ).performClick()
        compose.onNode(hasText("Choose Your Lifts") and hasClickAction()).performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasTestTag("five-three-one-training-max-Custom-0"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasText("Calculate from max / e1RM") and hasClickAction()).performClick()
        compose.onNodeWithTag("five-three-one-recent-max-Custom-0")
            .performScrollTo()
            .performTextReplacement("200")
        compose.onNodeWithTag("five-three-one-use-tm-suggestion-Custom-0")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        compose.onAllNodes(hasTestTag("five-three-one-program-build-blocker")).assertCountEquals(0)
        compose.onNodeWithTag("five-three-one-program-create").assertIsEnabled()

        compose.onNodeWithTag("five-three-one-recent-max-Custom-0").performTextReplacement("300")
        compose.onNodeWithTag("five-three-one-use-tm-suggestion-Custom-0")
            .assertTextContains("Use 255 lb as Training Max")
        compose.onNodeWithTag("five-three-one-tm-unapplied-Custom-0").assertExists()
        compose.onNodeWithTag("five-three-one-program-create").assertIsNotEnabled().performClick()
        compose.runOnIdle { assertNull(savedDraft) }

        // A different source that rounds to the already-applied 170 lb is still a provenance edit.
        compose.onNodeWithTag("five-three-one-recent-max-Custom-0").performTextReplacement("201")
        compose.onNodeWithTag("five-three-one-use-tm-suggestion-Custom-0")
            .assertTextContains("Use 170 lb as Training Max")
        compose.onNodeWithTag("five-three-one-program-create").assertIsNotEnabled()
        compose.onNodeWithTag("five-three-one-use-tm-suggestion-Custom-0")
            .performScrollTo()
            .performClick()
        compose.onNodeWithTag("five-three-one-program-create").assertIsEnabled().performClick()
        compose.onNodeWithTag("routine-builder-save").performClick()

        compose.runOnIdle {
            val placement = requireNotNull(savedDraft).days.single().exercises.single()
            assertEquals(170.0, placement.trainingMaxValue)
            assertEquals(201.0, placement.trainingMaxBasisValue)
            assertEquals(85.0, placement.trainingMaxPercent, 0.0)
            assertEquals(TrainingMaxBasisKind.ActualOneRepMax, placement.trainingMaxBasisKind)
        }
    }

    @Test
    fun trainingMaxSourceChipsLoadMatchingActualAndEstimatedRecordsAndEditablePercentage() {
        val exercises = listOf(
            exercise(1, "Squat"),
            exercise(2, "Bench Press"),
            exercise(3, "Deadlift"),
            exercise(4, "Overhead Press"),
        )
        val records = listOf(
            PersonalRecord(
                uuid = "actual",
                exerciseId = 1,
                type = PersonalRecordType.BestWeightForRepCount,
                value = massToKilograms(200.0, "pound"),
                secondaryValue = 1.0,
                unitId = "pound",
                sourceSetId = 1,
                sourceSessionId = 1,
                achievedAtMillis = 1,
                current = true,
                imported = false,
                createdAtMillis = 1,
                updatedAtMillis = 1,
            ),
            PersonalRecord(
                uuid = "estimated",
                exerciseId = 1,
                type = PersonalRecordType.EstimatedOneRepMax,
                value = massToKilograms(250.0, "pound"),
                secondaryValue = null,
                unitId = "pound",
                sourceSetId = 2,
                sourceSessionId = 2,
                achievedAtMillis = 2,
                current = true,
                imported = false,
                createdAtMillis = 2,
                updatedAtMillis = 2,
            ),
        )
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutineBuilderScreen(
                    routineId = null,
                    gymState = GymUiState(exercises = exercises, personalRecords = records, loading = false),
                    initial = null,
                    onDismiss = {},
                    onSave = { _, complete -> complete(true) },
                    onCreateExercise = { _, _ -> },
                    onCreateMachine = { _, _ -> },
                )
            }
        }

        compose.onNode(
            hasText("Set Up 5/3/1") and hasClickAction() and
                hasAnyAncestor(hasTestTag("routine-five-three-one-program-entry")),
        ).performClick()
        compose.onAllNodes(hasText("Calculate from max / e1RM") and hasClickAction())[0].performClick()
        compose.onNodeWithTag("five-three-one-recent-max-Squat").performScrollTo().assertTextContains("200")

        compose.onNode(hasText("Estimated 1RM") and hasClickAction()).performClick()
        compose.onNodeWithTag("five-three-one-recent-max-Squat").assertTextContains("250")
        compose.onNode(hasText("Actual 1RM") and hasClickAction()).performClick()
        compose.onNodeWithTag("five-three-one-recent-max-Squat").assertTextContains("200")
        compose.onNodeWithTag("five-three-one-tm-percent-Squat").performTextReplacement("80")
        compose.onNodeWithTag("five-three-one-use-tm-suggestion-Squat")
            .assertTextContains("Use 160 lb as Training Max")
    }

    @Test
    fun programTrainingMaxBasisEditsMustBeAppliedBeforeSaveEvenWhenTheyRoundToSameValue() {
        val squat = exercise(1, "Squat")
        var savedDraft: RoutineDraft? = null
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutineBuilderScreen(
                    routineId = 9,
                    gymState = GymUiState(exercises = listOf(squat), loading = false),
                    initial = RoutineDraft(
                        name = "Applied provenance",
                        program = RoutineProgramDraft(RoutineProgramKind.FiveThreeOne, phaseCount = 1),
                        days = listOf(
                            RoutineDayDraft(
                                "Squat",
                                listOf(
                                    RoutineExerciseDraft(
                                        exerciseId = squat.id,
                                        trainingMaxValue = 170.0,
                                        trainingMaxUnitId = "pound",
                                        cycleIncrementValue = 5.0,
                                        trainingMaxSource = RoutineTrainingMaxSource.Explicit,
                                        trainingMaxPercent = 85.0,
                                        trainingMaxBasisKind = TrainingMaxBasisKind.ActualOneRepMax,
                                        trainingMaxBasisValue = 200.0,
                                        trainingMaxBasisUnitId = "pound",
                                        assistanceRole = RoutineAssistanceRole.MainLift,
                                        placementKind = RoutinePlacementKind.MainLift,
                                        plannedSets = listOf(
                                            WorkoutSetDraft(
                                                reps = 5,
                                                loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                                                loadPercentage = 65.0,
                                                workSection = RoutineWorkSection.Main,
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                    onDismiss = {},
                    onSave = { draft, complete -> savedDraft = draft; complete(true) },
                    onCreateExercise = { _, _ -> },
                    onCreateMachine = { _, _ -> },
                )
            }
        }

        compose.onNodeWithTag("routine-open-program-structure").performClick()
        compose.onNodeWithTag("routine-program-structure-page")
            .performScrollToNode(hasTestTag("routine-program-tm-basis-1"))
        compose.onNodeWithTag("routine-program-tm-basis-1").performClick()
        compose.onNodeWithTag("routine-program-tm-source-1")
            .performScrollTo()
            .performTextReplacement("300")
        compose.onNodeWithTag("routine-program-apply-tm-1").assertTextContains("Apply 255 lb Training Max")
        compose.onNodeWithTag("routine-program-tm-unapplied-1").assertExists()
        compose.runOnIdle { assertNull(savedDraft) }

        // 201 × 85% still rounds to 170 lb, but it is different provenance and remains pending.
        compose.onNodeWithTag("routine-program-tm-source-1").performTextReplacement("201")
        compose.onNodeWithTag("routine-program-apply-tm-1").assertTextContains("Apply 170 lb Training Max")
        compose.onNodeWithContentDescription("Back in routine builder").performClick()
        compose.onNodeWithTag("routine-builder-save").assertIsNotEnabled().performClick()
        compose.runOnIdle { assertNull(savedDraft) }

        compose.onNodeWithTag("routine-open-program-structure").performClick()
        compose.onNodeWithTag("routine-program-structure-page")
            .performScrollToNode(hasTestTag("routine-program-tm-basis-1"))
        compose.onNodeWithTag("routine-program-tm-basis-1").performClick()
        compose.onNodeWithTag("routine-program-structure-page")
            .performScrollToNode(hasTestTag("routine-program-apply-tm-1"))
        compose.onNodeWithTag("routine-program-apply-tm-1").performClick()
        compose.onNodeWithContentDescription("Back in routine builder").performClick()
        compose.onNodeWithTag("routine-builder-save").assertIsEnabled().performClick()

        compose.runOnIdle {
            val placement = requireNotNull(savedDraft).days.single().exercises.single()
            assertEquals(170.0, placement.trainingMaxValue)
            assertEquals(201.0, placement.trainingMaxBasisValue)
            assertEquals(85.0, placement.trainingMaxPercent, 0.0)
            assertEquals(TrainingMaxBasisKind.ActualOneRepMax, placement.trainingMaxBasisKind)
        }
    }

    @Test
    fun ordinaryRoutineCanEnterAnExplicitTrainingMaxForPercentageSets() {
        val bench = exercise(1, "Bench Press")
        var savedDraft: RoutineDraft? = null
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutineBuilderScreen(
                    routineId = 7,
                    gymState = GymUiState(exercises = listOf(bench), loading = false),
                    initial = RoutineDraft(
                        name = "Bench day",
                        days = listOf(
                            RoutineDayDraft(
                                "Day A",
                                listOf(
                                    RoutineExerciseDraft(
                                        exerciseId = bench.id,
                                        plannedSets = listOf(
                                            WorkoutSetDraft(
                                                reps = 5,
                                                loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                                                loadPercentage = 80.0,
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                    onDismiss = {},
                    onSave = { draft, complete -> savedDraft = draft; complete(true) },
                    onCreateExercise = { _, _ -> },
                    onCreateMachine = { _, _ -> },
                )
            }
        }

        compose.onNodeWithText("Bench Press", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("routine-placement-editor")
            .performScrollToNode(hasTestTag("routine-training-max-section"))
        compose.onNodeWithTag("routine-training-max-missing-estimate").assertIsDisplayed()
        compose.onNodeWithTag("routine-builder-save").assertIsNotEnabled()

        compose.onNodeWithTag("routine-training-max-source-explicit").performClick()
        compose.onNodeWithTag("routine-training-max-value")
            .performScrollTo()
            .performTextInput("225")
        compose.onNodeWithTag("routine-builder-save").assertIsEnabled().performClick()

        compose.runOnIdle {
            val saved = requireNotNull(savedDraft)
            val placement = saved.days.single().exercises.single()
            assertEquals(225.0, placement.trainingMaxValue)
            assertEquals("pound", placement.trainingMaxUnitId)
            assertEquals(RoutineTrainingMaxSource.Explicit, placement.trainingMaxSource)
            assertEquals(RoutineProgramKind.Static, saved.program?.kind ?: RoutineProgramKind.Static)
        }
    }

    @Test
    fun staticRoutineExposesTrainingMaxBeforeAnySetUsesIt() {
        val bench = exercise(1, "Bench Press").copy(weightUnitId = "pound")
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutineBuilderScreen(
                    routineId = 8,
                    gymState = GymUiState(exercises = listOf(bench), loading = false),
                    initial = RoutineDraft(
                        name = "Custom strength day",
                        days = listOf(
                            RoutineDayDraft(
                                "Day A",
                                listOf(
                                    RoutineExerciseDraft(
                                        exerciseId = bench.id,
                                        plannedSets = listOf(WorkoutSetDraft(weight = 185.0, reps = 5)),
                                    ),
                                ),
                            ),
                        ),
                    ),
                    onDismiss = {},
                    onSave = { _, complete -> complete(true) },
                    onCreateExercise = { _, _ -> },
                    onCreateMachine = { _, _ -> },
                )
            }
        }

        compose.onNodeWithText("Bench Press", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("routine-placement-editor")
            .performScrollToNode(hasTestTag("routine-training-max-disclosure"))
        compose.onNodeWithTag("routine-training-max-disclosure")
            .assertIsDisplayed()
            .performClick()
        compose.onNodeWithTag("routine-training-max-section").assertIsDisplayed()
        compose.onNodeWithTag("routine-training-max-source-explicit").performClick()
        compose.onNodeWithTag("routine-training-max-value").performTextInput("225")
        compose.onNodeWithTag("routine-builder-save").assertIsEnabled()
    }

    @Test
    fun routineEditRoundTripsSetUnitsBodyweightAndDayProgression() {
        val bench = exercise(1, "Bench Press").copy(weightUnitId = "kilogram")
        var savedDraft: RoutineDraft? = null
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutineBuilderScreen(
                    routineId = null,
                    gymState = GymUiState(exercises = listOf(bench), loading = false),
                    initial = RoutineDraft(
                        name = "Preserve meaning",
                        days = listOf(
                            RoutineDayDraft(
                                name = "Day A",
                                progressionIndex = 3,
                                exercises = listOf(
                                    RoutineExerciseDraft(
                                        exerciseId = bench.id,
                                        plannedSets = listOf(
                                            WorkoutSetDraft(
                                                weight = 100.0,
                                                weightUnitId = "pound",
                                                reps = 5,
                                                distance = 1.0,
                                                distanceUnitId = "mile",
                                                bodyweightKg = 90.0,
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                    onDismiss = {},
                    onSave = { draft, complete -> savedDraft = draft; complete(true) },
                    onCreateExercise = { _, _ -> },
                    onCreateMachine = { _, _ -> },
                )
            }
        }

        compose.onNodeWithTag("routine-builder-save").assertIsEnabled().performClick()

        compose.runOnIdle {
            val savedDay = requireNotNull(savedDraft).days.single()
            val set = savedDay.exercises.single().plannedSets.single()
            assertEquals(3, savedDay.progressionIndex)
            assertEquals(100.0, set.weight)
            assertEquals("pound", set.weightUnitId)
            assertEquals(1.0, set.distance)
            assertEquals("mile", set.distanceUnitId)
            assertEquals(90.0, set.bodyweightKg)
        }
    }

    @Test
    fun splitPresetsUseConsistentTitleCapitalization() {
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutineBuilderScreen(
                    routineId = null,
                    gymState = GymUiState(loading = false),
                    initial = null,
                    onDismiss = {},
                    onSave = { _, complete -> complete(true) },
                    onCreateExercise = { _, _ -> },
                    onCreateMachine = { _, _ -> },
                )
            }
        }

        compose.onNodeWithText("Full Body").assertIsDisplayed()
        compose.onNodeWithText("Upper / Lower").assertIsDisplayed()
        compose.onNodeWithText("Push / Pull / Legs").assertIsDisplayed()
    }

    @Test
    fun exerciseListUsesTheRoutinePaneInsteadOfAOneRowNestedViewport() {
        val exercises = (1L..5L).map { exercise(it, "Lift $it") }
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                Box(Modifier.width(320.dp).height(600.dp)) {
                    RoutineBuilderScreen(
                        routineId = 7,
                        gymState = GymUiState(exercises = exercises, loading = false),
                        initial = RoutineDraft(
                            name = "Compact 5/3/1",
                            notes = "Main, supplemental, and assistance work",
                            days = listOf(
                                RoutineDayDraft(
                                    "Bench day",
                                    exercises.map { RoutineExerciseDraft(it.id) },
                                ),
                            ),
                            program = RoutineProgramDraft(
                                kind = RoutineProgramKind.FiveThreeOne,
                                phaseCount = 4,
                                phaseLabels = listOf("5s Week", "3s Week", "5/3/1 Week", "Deload"),
                            ),
                        ),
                        onDismiss = {},
                        onSave = { _, complete -> complete(true) },
                        onCreateExercise = { _, _ -> },
                        onCreateMachine = { _, _ -> },
                    )
                }
            }
        }

        val builder = compose.onNodeWithTag("routine-builder").fetchSemanticsNode().boundsInRoot
        val outline = compose.onNodeWithTag("routine-selected-exercises").fetchSemanticsNode().boundsInRoot
        check(outline.height >= builder.height * 0.65f) {
            "Routine outline must use the pane instead of reserving a tiny nested exercise viewport: outline=$outline builder=$builder"
        }

        compose.onNodeWithTag("routine-selected-exercises").performScrollToNode(hasText("Lift 5"))
        compose.onNodeWithText("Lift 3", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Lift 4", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Lift 5", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun guidedFiveThreeOneBuilderPersistsCompleteExplicitBbbCycle() {
        val bench = exercise(1, "Bench")
        var savedDraft: RoutineDraft? = null
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutineBuilderScreen(
                    routineId = 7,
                    gymState = GymUiState(exercises = listOf(bench), loading = false),
                    initial = RoutineDraft(
                        name = "5/3/1",
                        days = listOf(RoutineDayDraft("Press", listOf(RoutineExerciseDraft(bench.id)))),
                    ),
                    onDismiss = {},
                    onSave = { draft, complete -> savedDraft = draft; complete(true) },
                    onCreateExercise = { _, _ -> },
                    onCreateMachine = { _, _ -> },
                )
            }
        }

        compose.onNodeWithText("Bench", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("routine-five-three-one-toggle").performClick()
        compose.onNodeWithTag("routine-placement-editor")
            .performScrollToNode(hasTestTag("five-three-one-training-max"))
        compose.onNodeWithTag("five-three-one-training-max").performTextReplacement("200")
        compose.onNodeWithTag("routine-placement-editor")
            .performScrollToNode(hasTestTag("five-three-one-main-FivesPro"))
        compose.onNodeWithTag("five-three-one-main-FivesPro").performClick()
        compose.onNodeWithTag("routine-placement-editor")
            .performScrollToNode(hasTestTag("five-three-one-supplement-BoringButBig"))
        compose.onNodeWithTag("five-three-one-supplement-BoringButBig").performClick()
        compose.onNodeWithTag("routine-placement-editor")
            .performScrollToNode(hasTestTag("five-three-one-apply"))
        compose.onNodeWithTag("five-three-one-apply").performClick()
        compose.onNodeWithTag("routine-builder-save").performClick()

        compose.runOnIdle {
            val draft = requireNotNull(savedDraft)
            assertEquals(RoutineProgramKind.FiveThreeOne, draft.program?.kind)
            assertEquals(4, draft.program?.phaseCount)
            assertEquals(listOf("5s Week", "3s Week", "5/3/1 Week", "Deload"), draft.program?.phaseLabels)
            val placement = draft.days.single().exercises.single()
            assertEquals(200.0, placement.trainingMaxValue)
            assertEquals("pound", placement.trainingMaxUnitId)
            assertEquals(5.0, placement.cycleIncrementValue)
            assertEquals(RoutineTrainingMaxSource.Explicit, placement.trainingMaxSource)
            assertEquals(RoutineMainWorkScheme.FivesPro, placement.mainWorkScheme)
            assertEquals(RoutineSupplementalScheme.BoringButBig, placement.supplementalScheme)
            assertEquals(17, placement.plannedSets.size)
            assertEquals(12, placement.plannedSets.count { it.routinePhaseIndex != null })
            assertEquals(5, placement.plannedSets.count { it.routinePhaseIndex == null && it.reps == 10 })
        }
    }

    @Test
    fun multiDayRoutineRoutesFiveThreeOneConversionThroughWholeProgramReview() {
        val bench = exercise(1, "Bench")
        val deadlift = exercise(2, "Deadlift")
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutineBuilderScreen(
                    routineId = 7,
                    gymState = GymUiState(exercises = listOf(bench, deadlift), loading = false),
                    initial = RoutineDraft(
                        name = "Two day draft",
                        days = listOf(
                            RoutineDayDraft("Bench day", listOf(RoutineExerciseDraft(bench.id))),
                            RoutineDayDraft("Deadlift day", listOf(RoutineExerciseDraft(deadlift.id))),
                        ),
                    ),
                    onDismiss = {},
                    onSave = { _, complete -> complete(true) },
                    onCreateExercise = { _, _ -> },
                    onCreateMachine = { _, _ -> },
                )
            }
        }

        compose.onNodeWithText("Bench", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("routine-five-three-one-whole-program-required").assertIsDisplayed()
        compose.onAllNodes(hasTestTag("routine-five-three-one-toggle")).assertCountEquals(0)
        compose.onNodeWithTag("routine-five-three-one-replace-with-program").performClick()
        compose.onNodeWithTag("five-three-one-program-replacement-warning").assertIsDisplayed()
        compose.onNodeWithTag("five-three-one-program-create").assertTextContains("Replace Draft with Program")
    }

    @Test
    fun singleDayRoutineHidesActionsThatCannotApply() {
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutineBuilderScreen(
                    routineId = null,
                    gymState = GymUiState(loading = false),
                    initial = null,
                    onDismiss = {},
                    onSave = { _, complete -> complete(true) },
                    onCreateExercise = { _, _ -> },
                    onCreateMachine = { _, _ -> },
                )
            }
        }

        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription("Duplicate Day A").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithContentDescription("Move Day A earlier").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Move Day A later").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Delete Day A").assertCountEquals(0)
        compose.onNodeWithContentDescription("Duplicate Day A").assertIsDisplayed()
    }

    @Test
    fun twoHundredExerciseLibraryIsSearchableAndOnlySelectionsEnterOutline() {
        val exercises = (1L..205L).map { exercise(it, "Exercise ${it.toString().padStart(3, '0')}") }
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutineBuilderScreen(
                    routineId = null,
                    gymState = GymUiState(
                        exercises = exercises,
                        appSettings = AppSettings(
                            repPrescriptionSchemes = listOf(
                                RepPrescriptionScheme(
                                    "hypertrophy",
                                    "Hypertrophy",
                                    setCount = 3,
                                    repetitionsMin = 8,
                                    repetitionsMax = 10,
                                ),
                            ),
                        ),
                        loading = false,
                    ),
                    initial = null,
                    onDismiss = {},
                    onSave = { _, complete -> complete(true) },
                    onCreateExercise = { _, _ -> },
                    onCreateMachine = { _, _ -> },
                )
            }
        }

        compose.onNodeWithTag("routine-add-exercises").performClick()
        compose.onNodeWithText("205 exercises").assertIsDisplayed()
        compose.onNodeWithTag("routine-exercise-search").performTextInput("Exercise 200")
        compose.onNode(
            hasText("Exercise 200") and hasAnyAncestor(hasTestTag("routine-exercise-picker-list")),
            useUnmergedTree = true,
        ).performClick()
        compose.onNodeWithTag("routine-add-selected").assertTextEquals("Add 1 Exercise to Day A").performClick()

        compose.onNodeWithTag("routine-placement-editor").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back in routine builder").performClick()
        compose.onNode(
            hasText("Exercise 200") and hasAnyAncestor(hasTestTag("routine-selected-exercises")),
            useUnmergedTree = true,
        ).assertIsDisplayed()
        compose.onNodeWithContentDescription("Manage Exercise 200").assertIsDisplayed()
        compose.onNodeWithContentDescription("Edit routine exercise Exercise 200").assertIsDisplayed().performClick()
        compose.onNodeWithText("Hypertrophy · 3 × 8–10").performClick()
        compose.onNodeWithTag("routine-placement-editor").performScrollToNode(hasText("Reps min"))
        compose.onNodeWithTag("routine-reps-min-3").assertTextContains("8")
        compose.onNodeWithTag("routine-reps-max-3").assertTextContains("10")
    }

    @Test
    fun exerciseCanBeCreatedWithoutDroppingRoutineDraft() {
        var createdName = ""
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutineBuilderScreen(
                    routineId = null,
                    gymState = GymUiState(loading = false),
                    initial = null,
                    onDismiss = {},
                    onSave = { _, complete -> complete(true) },
                    onCreateExercise = { draft, complete -> createdName = draft.name; complete(77) },
                    onCreateMachine = { _, _ -> },
                )
            }
        }

        compose.onNodeWithTag("routine-editor-name").performTextInput("My routine")
        compose.onNodeWithTag("routine-add-exercises").performClick()
        compose.onNodeWithTag("routine-exercise-search").performTextInput("Incline press")
        compose.onNodeWithText("Create “Incline press”").performClick()
        compose.onNodeWithTag("exercise-editor-name").assertTextContains("Incline press")
        compose.onNodeWithText("Save").performClick()

        compose.waitForIdle()
        assertEquals("Incline press", createdName)
        compose.onNodeWithTag("routine-placement-editor").assertIsDisplayed()
        compose.onNodeWithText("Incline press").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back in routine builder").performClick()
        compose.onNode(
            hasText("Incline press") and hasAnyAncestor(hasTestTag("routine-selected-exercises")),
            useUnmergedTree = true,
        ).assertIsDisplayed()
        compose.onNodeWithTag("routine-editor-name").assertTextContains("My routine")
        compose.onNodeWithText("1 new library item was saved independently and will remain if this routine is canceled.").assertIsDisplayed()
    }

    @Test
    fun machineCanBeQuickCreatedForAPlacementWithoutLeavingBuilder() {
        val bench = exercise(1, "Machine bench")
        var createdMachineName = ""
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutineBuilderScreen(
                    routineId = 42,
                    gymState = GymUiState(exercises = listOf(bench), loading = false),
                    initial = RoutineDraft(
                        name = "Machine push",
                        days = listOf(RoutineDayDraft("A", listOf(RoutineExerciseDraft(bench.id)))),
                    ),
                    onDismiss = {},
                    onSave = { _, complete -> complete(true) },
                    onCreateExercise = { _, _ -> },
                    onCreateMachine = { draft, complete -> createdMachineName = draft.name; complete(88) },
                )
            }
        }

        compose.onNodeWithText("Machine bench", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("routine-equipment-picker").performClick()
        compose.onNodeWithText("Quick-Create Machine for This Exercise").assertIsDisplayed().performClick()
        compose.onNodeWithTag("routine-quick-machine-name").performTextInput("Home stack")
        compose.onNodeWithTag("routine-quick-machine-create").performClick()

        compose.waitForIdle()
        assertEquals("Home stack", createdMachineName)
        compose.onNodeWithText("1 new library item was saved independently and will remain if this routine is canceled.").assertIsDisplayed()
        compose.onNodeWithText("Equipment · Home stack").assertIsDisplayed()
    }

    @Test
    fun failedRoutineSaveKeepsTheCompleteDraftOpenForRetry() {
        var attempts = 0
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutineBuilderScreen(
                    routineId = null,
                    gymState = GymUiState(loading = false),
                    initial = null,
                    onDismiss = {},
                    onSave = { _, complete -> attempts++; complete(false) },
                    onCreateExercise = { _, _ -> },
                    onCreateMachine = { _, _ -> },
                )
            }
        }

        compose.onNodeWithTag("routine-editor-name").performTextInput("Do not lose me")
        compose.onNodeWithTag("routine-builder-save").performClick()

        compose.waitForIdle()
        assertEquals(1, attempts)
        compose.onNodeWithTag("routine-builder").assertIsDisplayed()
        compose.onNodeWithTag("routine-editor-name").assertTextContains("Do not lose me")
    }

    @Test
    fun addingExerciseToExistingRoutineSavesWithoutLeavingTheEditedDay() {
        val bench = exercise(1, "Bench Press")
        val zercher = exercise(2, "Zercher Squat")
        var savedDraft: RoutineDraft? = null
        var dismissals = 0
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutineBuilderScreen(
                    routineId = 42,
                    gymState = GymUiState(exercises = listOf(bench, zercher), loading = false),
                    initial = RoutineDraft(
                        name = "Strength",
                        days = listOf(
                            RoutineDayDraft("Bench", listOf(RoutineExerciseDraft(bench.id))),
                            RoutineDayDraft("Zercher", emptyList()),
                        ),
                    ),
                    onDismiss = { dismissals++ },
                    onSave = { draft, complete -> savedDraft = draft; complete(true) },
                    onCreateExercise = { _, _ -> },
                    onCreateMachine = { _, _ -> },
                )
            }
        }

        compose.onNodeWithText("Zercher · 0").performClick()
        compose.onNodeWithTag("routine-add-exercises").performClick()
        compose.onNode(
            hasText("Zercher Squat") and hasAnyAncestor(hasTestTag("routine-exercise-picker-list")),
            useUnmergedTree = true,
        ).performClick()
        compose.onNodeWithTag("routine-add-selected").assertTextEquals("Add 1 Exercise to Zercher").performClick()
        compose.onNodeWithTag("routine-builder-save").performClick()

        compose.runOnIdle {
            assertEquals(0, dismissals)
            assertEquals(listOf(zercher.id), savedDraft?.days?.get(1)?.exercises?.map { it.exerciseId })
        }
        compose.onNodeWithTag("routine-builder").assertIsDisplayed()
        compose.onNodeWithTag("routine-placement-editor").assertIsDisplayed()
        compose.onNodeWithText("Zercher Squat", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("routine-saved-in-place").assertIsDisplayed()
        compose.onNodeWithText("Routine saved. Continue editing Zercher.").assertIsDisplayed()
        compose.onNodeWithText("Close").assertIsDisplayed()
        compose.onNodeWithTag("routine-builder-save").assertIsNotEnabled()
        compose.onNodeWithText("Close").performClick()
        compose.runOnIdle { assertEquals(1, dismissals) }
    }

    @Test
    fun repSchemeLibraryStartsBlankAndPlusCreatesAReusableScheme() {
        val bench = exercise(1, "Bench")
        var saved: RepPrescriptionScheme? = null
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutineBuilderScreen(
                    routineId = null,
                    gymState = GymUiState(exercises = listOf(bench), loading = false),
                    initial = RoutineDraft(
                        name = "Push",
                        days = listOf(RoutineDayDraft("A", listOf(RoutineExerciseDraft(bench.id)))),
                    ),
                    onDismiss = {},
                    onSave = { _, complete -> complete(true) },
                    onCreateExercise = { _, _ -> },
                    onCreateMachine = { _, _ -> },
                    onSavePrescriptionScheme = { saved = it },
                )
            }
        }

        compose.onNodeWithText("Bench", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("routine-rep-schemes-empty").assertIsDisplayed()
        compose.onNodeWithTag("routine-add-rep-scheme").performClick()
        compose.onNodeWithTag("rep-scheme-name").performTextInput("Strength")
        compose.onNodeWithTag("rep-scheme-set-count").performTextInput("5")
        compose.onNodeWithTag("rep-scheme-reps-min").performTextInput("3")
        compose.onNodeWithTag("rep-scheme-reps-max").performTextInput("5")
        compose.onNodeWithTag("rep-scheme-rest").performTextInput("180")
        compose.onNodeWithTag("rep-scheme-save").performClick()

        compose.runOnIdle {
            assertEquals("Strength", saved?.name)
            assertEquals(5, saved?.setCount)
            assertEquals(3, saved?.repetitionsMin)
            assertEquals(5, saved?.repetitionsMax)
            assertEquals(180, saved?.restSeconds)
            assertEquals(WorkoutSetClassification.Working, saved?.classification)
        }
    }

    @Test
    fun savedRepSchemeCanBeAppliedEditedAndDeleted() {
        val bench = exercise(1, "Bench")
        val scheme = RepPrescriptionScheme(
            id = "custom",
            name = "Volume",
            setCount = 3,
            repetitionsMin = 8,
            repetitionsMax = 12,
            classification = WorkoutSetClassification.BackOff,
            restSeconds = 90,
        )
        var edited: RepPrescriptionScheme? = null
        var deletedId: String? = null
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutineBuilderScreen(
                    routineId = 7,
                    gymState = GymUiState(
                        exercises = listOf(bench),
                        appSettings = AppSettings(repPrescriptionSchemes = listOf(scheme)),
                        loading = false,
                    ),
                    initial = RoutineDraft(
                        name = "Push",
                        days = listOf(RoutineDayDraft("A", listOf(RoutineExerciseDraft(bench.id)))),
                    ),
                    onDismiss = {},
                    onSave = { _, complete -> complete(true) },
                    onCreateExercise = { _, _ -> },
                    onCreateMachine = { _, _ -> },
                    onSavePrescriptionScheme = { edited = it },
                    onDeletePrescriptionScheme = { deletedId = it },
                )
            }
        }

        compose.onNodeWithText("Bench", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Volume · 3 × 8–12").performClick()
        compose.onNodeWithTag("routine-placement-editor").performScrollToNode(hasText("Reps min"))
        compose.onNodeWithTag("routine-reps-min-1").assertTextContains("8")
        compose.onNodeWithTag("routine-reps-max-1").assertTextContains("12")

        compose.onNodeWithTag("routine-placement-editor").performScrollToNode(hasText("Saved Schemes · App-wide"))
        compose.onNodeWithContentDescription("Edit Volume · 3 × 8–12").performClick()
        compose.onNodeWithTag("rep-scheme-name").performTextReplacement("Backoff")
        compose.onNodeWithTag("rep-scheme-reps-max").performTextReplacement("10")
        compose.onNodeWithTag("rep-scheme-save").performClick()
        compose.runOnIdle {
            assertEquals("custom", edited?.id)
            assertEquals("Backoff", edited?.name)
            assertEquals(10, edited?.repetitionsMax)
        }

        compose.onNodeWithContentDescription("Delete Volume · 3 × 8–12").performClick()
        compose.onNodeWithText("Delete Scheme").performClick()
        compose.runOnIdle { assertEquals("custom", deletedId) }
    }

    private fun exercise(id: Long, name: String) = Exercise(
        id = id,
        uuid = "exercise-$id",
        name = name,
        trackingType = ExerciseTrackingType.WeightReps,
        notes = "",
        equipment = if (id % 2L == 0L) "Barbell" else "Dumbbell",
        primaryMuscles = if (id % 2L == 0L) "Chest" else "Back",
        secondaryMuscles = "",
        weightUnitId = "pound",
        weightIncrement = 5.0,
        repetitionIncrement = 1,
        defaultRestSeconds = 120,
        defaultGraphMetric = "EstimatedOneRepMax",
        oneRepMaxFormula = EstimatedOneRepMaxFormula.Epley,
        barWeightKg = null,
        availablePlatesKg = emptyList(),
        includeInVolume = true,
        includeInPersonalRecords = true,
        bodyweightLoadPolicy = BodyweightLoadPolicy.ExternalWeightOnly,
        effectiveBodyweightPercent = 100.0,
        showRpe = null,
        showRir = null,
        showTempo = null,
        favorite = id % 10L == 0L,
        position = id.toInt(),
        archived = false,
        createdAtMillis = id,
        updatedAtMillis = id,
    )
}
