package com.whip.app

import androidx.compose.ui.test.hasContentDescription

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
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
import androidx.compose.ui.test.getUnclippedBoundsInRoot
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
import androidx.compose.ui.unit.Density
import androidx.test.espresso.Espresso.closeSoftKeyboard
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
import com.whip.app.domain.RoutinePlacementKind
import com.whip.app.domain.RoutineLoadPrescriptionType
import com.whip.app.domain.RoutineMainWorkScheme
import com.whip.app.domain.RoutineProgramKind
import com.whip.app.domain.RoutineProgramDraft
import com.whip.app.domain.RoutineProgramPhaseRole
import com.whip.app.domain.RoutineAssistanceCategory
import com.whip.app.domain.RoutineOptionalWorkKind
import com.whip.app.domain.RoutineSupplementalScheme
import com.whip.app.domain.RoutineTrainingMaxSource
import com.whip.app.domain.RoutineWorkSection
import com.whip.app.domain.TrainingMaxBasisKind
import com.whip.app.domain.WorkoutSetClassification
import com.whip.app.domain.WorkoutSetDraft
import com.whip.app.domain.massToKilograms
import com.whip.app.ui.GymUiState
import com.whip.app.ui.ExercisePickerDialog
import com.whip.app.ui.FiveThreeOneProgramLayout
import com.whip.app.ui.LocalWhipDialogPlacement
import com.whip.app.ui.RoutineBuilderScreen
import com.whip.app.ui.WhipDialogPlacement
import com.whip.app.ui.theme.WhipTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoutineBuilderUiTest {
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)

    @Test
    fun sharedExercisePickerAlwaysOffersSearchAndSeededCreation() {
        var createdSeed: String? = null
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                ExercisePickerDialog(
                    exercises = listOf(exercise(1, "Bench Press")),
                    title = "Add Exercise",
                    onDismiss = {},
                    onPick = {},
                    onCreate = { createdSeed = it },
                )
            }
        }

        compose.onNodeWithTag("exercise-picker-dialog").assertIsDisplayed()
        compose.onNodeWithTag("exercise-picker-create").assertIsDisplayed()
        captureVisualCatalogSurface("gym.exercise.picker")
        compose.onNodeWithTag("exercise-picker-search").performTextInput("Zercher Squat")
        compose.onNodeWithTag("exercise-picker-empty").assertIsDisplayed()
        compose.onNodeWithText("Nothing matches “Zercher Squat”. Create it as a new exercise without leaving this screen.")
            .assertIsDisplayed()
        compose.onNodeWithTag("exercise-picker-create-empty").assertTextEquals("Create “Zercher Squat”").performClick()

        compose.runOnIdle { assertEquals("Zercher Squat", createdSeed) }
    }

    @Test
    fun sharedExercisePickerOnlyUsesSubstituteLanguageWhenContextProvidesIt() {
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                ExercisePickerDialog(
                    exercises = listOf(
                        exercise(1, "Bench Press"),
                        exercise(2, "Dumbbell Bench Press"),
                    ),
                    priorityIds = listOf(2),
                    priorityGroupLabel = "Preferred workout substitutes are shown first",
                    priorityItemLabel = "Preferred substitute",
                    title = "Substitute Exercise",
                    onDismiss = {},
                    onPick = {},
                    onCreate = {},
                )
            }
        }

        compose.onNodeWithText("Preferred workout substitutes are shown first").assertIsDisplayed()
        compose.onNodeWithText("Dumbbell Bench Press · Preferred substitute").assertIsDisplayed()
        compose.onAllNodesWithText("Planned alternative", substring = true).assertCountEquals(0)
    }

    @Test
    fun ordinaryRoutineCanAddIndependentPhasesWithoutFiveThreeOneSetup() {
        var saved: RoutineDraft? = null
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutineBuilderScreen(
                    routineId = null,
                    gymState = GymUiState(exercises = listOf(exercise(1, "Bench Press")), loading = false),
                    initial = RoutineDraft(
                        name = "Strength",
                        days = listOf(RoutineDayDraft("Day A", listOf(
                            RoutineExerciseDraft(1, plannedSets = listOf(WorkoutSetDraft(weight = 80.0, reps = 5))),
                        ))),
                    ),
                    onDismiss = {},
                    onSave = { draft, complete -> saved = draft; complete(true) },
                    onCreateExercise = { _, _ -> },
                    onCreateMachine = { _, _ -> },
                )
            }
        }

        compose.onAllNodesWithText("Set Up 5/3/1").assertCountEquals(0)
        captureVisualCatalogSurface("gym.routine-builder.outline")
        compose.onNodeWithTag("routine-add-phases").performScrollTo().performClick()
        compose.onNodeWithTag("routine-program-structure").assertExists()
        compose.onNodeWithTag("routine-open-program-structure").performClick()
        compose.onNodeWithTag("routine-program-phase-select-1").assertExists()
        compose.onAllNodesWithText("Apply 7th Week preset").assertCountEquals(0)
        captureVisualCatalogSurface("gym.routine-builder.phases")
        compose.onNodeWithContentDescription("Back to routine outline").performClick()
        compose.onNodeWithTag("routine-builder-save").performClick()
        compose.runOnIdle {
            val draft = requireNotNull(saved)
            assertEquals(RoutineProgramKind.Custom, draft.program?.kind)
            assertEquals(2, draft.program?.phaseCount)
            assertEquals(listOf(0, 1), draft.days.single().exercises.single().plannedSets.map { it.routinePhaseIndex })
            assertEquals(listOf(80.0, 80.0), draft.days.single().exercises.single().plannedSets.map { it.weight })
        }
    }

    @Test
    fun phasedRoutineCanConfigurePrimaryTrainingMaxAndBoundary() {
        var saved: RoutineDraft? = null
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutineBuilderScreen(
                    routineId = null,
                    gymState = GymUiState(exercises = listOf(exercise(1, "Bench Press")), loading = false),
                    initial = RoutineDraft("Strength", days = listOf(RoutineDayDraft("Upper", listOf(
                        RoutineExerciseDraft(1, plannedSets = listOf(
                            WorkoutSetDraft(weight = 40.0, reps = 5, classification = WorkoutSetClassification.WarmUp),
                            WorkoutSetDraft(weight = 80.0, reps = 5),
                        )),
                    )))),
                    onDismiss = {},
                    onSave = { draft, complete -> saved = draft; complete(true) },
                    onCreateExercise = { _, _ -> },
                    onCreateMachine = { _, _ -> },
                )
            }
        }
        compose.onNodeWithTag("routine-add-phases").performScrollTo().performClick()
        compose.onNodeWithTag("routine-open-program-structure").performClick()
        compose.onNodeWithTag("routine-program-phase-select-1").performClick()
        compose.onNodeWithTag("routine-program-structure-page")
            .performScrollToNode(hasTestTag("routine-program-phase-tm-boundary-1"))
        compose.onNodeWithTag("routine-program-phase-tm-boundary-1").performClick()
        compose.onNodeWithContentDescription("Back to routine outline").performClick()
        compose.onNodeWithText("Bench Press", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("routine-primary-lift-progression").performClick()
        compose.onNodeWithTag("routine-training-max-disclosure").performClick()
        compose.onNodeWithTag("routine-training-max-source-explicit").performClick()
        compose.onNodeWithTag("routine-training-max-value").performScrollTo().performTextInput("100")
        compose.onNodeWithTag("routine-cycle-increase").performScrollTo().performTextInput("2.5")
        compose.onNodeWithTag("routine-cycle-increase").assertTextContains("2.5")
        closeSoftKeyboard()
        captureVisualCatalogSurface("gym.routine-builder.primary-progression")
        compose.onNodeWithTag("routine-builder-save").performClick()
        compose.runOnIdle {
            val draft = requireNotNull(saved)
            assertEquals(RoutineProgramKind.Custom, draft.program?.kind)
            assertEquals(setOf(1), draft.program?.trainingMaxAdvanceAfterPhaseIndices)
            val placement = draft.days.single().exercises.single()
            assertEquals(RoutinePlacementKind.MainExercise, placement.placementKind)
            assertEquals(100.0, placement.trainingMaxValue)
            assertEquals(2.5, placement.cycleIncrementValue)
            assertTrue(placement.plannedSets.filter { it.classification == WorkoutSetClassification.WarmUp }
                .all { it.workSection == RoutineWorkSection.Optional })
            assertTrue(placement.plannedSets.filter { it.classification != WorkoutSetClassification.WarmUp }
                .all { it.workSection == RoutineWorkSection.Main })
        }
    }

    @Test
    fun emptyWorkoutPickerUsesTheSharedEmptyStateAndKeepsBackReachable() {
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

        compose.onNodeWithTag("routine-selected-exercises")
            .performScrollToNode(hasText("Add from a Previous Workout"))
        compose.onNodeWithText("Add from a Previous Workout").performClick()

        compose.onNodeWithText("No Completed Workouts").assertIsDisplayed()
        compose.onNodeWithText("Complete a workout first, or go back and add exercises from your library.")
            .assertIsDisplayed()
        compose.onNodeWithContentDescription("Back to routine outline").assertIsDisplayed()
        captureVisualCatalogSurface("gym.routine-builder.workout-picker")
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
                                        placementKind = RoutinePlacementKind.MainExercise,
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
    fun structuredMainExerciseUsesProgramStructureAndHidesGenericRewriteControls() {
        val bench = exercise(1, "Bench Press")
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutineBuilderScreen(
                    routineId = 7,
                    gymState = GymUiState(exercises = listOf(bench), loading = false),
                    initial = RoutineDraft(
                        name = "Bench 5/3/1",
                        program = RoutineProgramDraft(
                            kind = RoutineProgramKind.FiveThreeOne,
                            phaseCount = 4,
                            phaseLabels = listOf("5s Week", "3s Week", "5/3/1 Week", "Deload"),
                            phaseRoles = listOf(
                                RoutineProgramPhaseRole.Standard,
                                RoutineProgramPhaseRole.Standard,
                                RoutineProgramPhaseRole.Standard,
                                RoutineProgramPhaseRole.Deload,
                            ),
                            trainingMaxAdvanceAfterPhaseIndices = setOf(3),
                        ),
                        days = listOf(
                            RoutineDayDraft(
                                "Bench",
                                listOf(
                                    RoutineExerciseDraft(
                                        exerciseId = bench.id,
                                        trainingMaxValue = 200.0,
                                        trainingMaxUnitId = "pound",
                                        cycleIncrementValue = 5.0,
                                        placementKind = RoutinePlacementKind.MainExercise,
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
                    onSave = { _, complete -> complete(true) },
                    onCreateExercise = { _, _ -> },
                    onCreateMachine = { _, _ -> },
                )
            }
        }

        compose.onNodeWithText("Bench Press", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("routine-placement-open-program-structure").assertIsDisplayed()
        compose.onAllNodesWithText("Saved Schemes · App-wide").assertCountEquals(0)
        compose.onAllNodesWithText("Generate Equipment-Aware Warm-Ups").assertCountEquals(0)
        compose.onAllNodes(hasTestTag("routine-copy-previous")).assertCountEquals(0)

        compose.onNodeWithTag("routine-placement-open-program-structure").performClick()
        compose.onNodeWithTag("routine-program-structure-page").assertIsDisplayed()
        compose.onNodeWithTag("routine-program-training-maxes-disclosure").assertIsDisplayed()
        compose.onNodeWithTag("routine-program-phase-select-0").performScrollTo().assertIsDisplayed()
        captureVisualCatalogSurface("gym.routine-builder.program")
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
                                        placementKind = RoutinePlacementKind.MainExercise,
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
        compose.onNodeWithTag("routine-program-training-maxes-disclosure").performClick()
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
        compose.onNodeWithContentDescription("Back to routine outline").performClick()
        compose.onNodeWithTag("routine-builder-save").assertIsNotEnabled().performClick()
        compose.runOnIdle { assertNull(savedDraft) }

        compose.onNodeWithTag("routine-open-program-structure").performClick()
        compose.onNodeWithTag("routine-program-structure-page")
            .performScrollToNode(hasTestTag("routine-program-tm-basis-1"))
        compose.onNodeWithTag("routine-program-tm-basis-1").performClick()
        compose.onNodeWithTag("routine-program-structure-page")
            .performScrollToNode(hasTestTag("routine-program-apply-tm-1"))
        compose.onNodeWithTag("routine-program-apply-tm-1").performClick()
        compose.onNodeWithContentDescription("Back to routine outline").performClick()
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
        val exercises = (1L..5L).map { exercise(it, "Exercise $it") }
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

        compose.onNodeWithTag("routine-selected-exercises").performScrollToNode(hasText("Exercise 5"))
        compose.onNodeWithText("Exercise 3", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Exercise 4", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Exercise 5", useUnmergedTree = true).assertIsDisplayed()
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
        captureVisualCatalogSurface("gym.routine-builder.exercise-picker")
        compose.onNodeWithTag("routine-exercise-search").performTextInput("Exercise 200")
        compose.onNode(
            hasText("Exercise 200") and hasAnyAncestor(hasTestTag("routine-exercise-picker-list")),
            useUnmergedTree = true,
        ).performClick()
        compose.onNodeWithTag("routine-add-selected").assertTextEquals("Add 1 Exercise to Day A").performClick()

        compose.onNodeWithTag("routine-placement-editor").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back to routine outline").performClick()
        compose.onNode(
            hasText("Exercise 200") and hasAnyAncestor(hasTestTag("routine-selected-exercises")),
            useUnmergedTree = true,
        ).assertIsDisplayed()
        compose.onNodeWithContentDescription("Manage Exercise 200").assertIsDisplayed()
        compose.onNodeWithContentDescription("Edit routine exercise Exercise 200").assertIsDisplayed().performClick()
        compose.onNodeWithTag("routine-placement-editor").performScrollToNode(hasText("Hypertrophy · 3 × 8–10"))
        compose.onNodeWithText("Hypertrophy · 3 × 8–10").performClick()
        compose.onNodeWithTag("routine-placement-editor").performScrollToNode(hasText("Reps min"))
        compose.onNodeWithTag("routine-reps-min-3").assertTextContains("8")
        compose.onNodeWithTag("routine-reps-max-3").assertTextContains("10")
        compose.onNodeWithContentDescription("Back to routine outline").performClick()
        compose.onNodeWithContentDescription("Manage Exercise 200").performClick()
        compose.onNodeWithText("Remove").assertIsDisplayed()
        compose.waitForIdle()
        captureVisualCatalogSurface("gym.routine-placement.menu")
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
        compose.onNode(
            hasText("Incline press") and hasAnyAncestor(hasTestTag("routine-placement-editor")),
            useUnmergedTree = true,
        ).assertIsDisplayed()
        compose.onNodeWithContentDescription("Back to routine outline").performClick()
        compose.onNode(
            hasText("Incline press") and hasAnyAncestor(hasTestTag("routine-selected-exercises")),
            useUnmergedTree = true,
        ).assertIsDisplayed()
        compose.onNodeWithTag("routine-editor-name").assertTextContains("My routine")
        compose.onNodeWithText("1 new library item was saved independently and will remain if this routine is canceled.").assertIsDisplayed()
    }

    @Test
    fun advancedMachineCreationReturnsFromSeededExerciseCreationWithBothDraftsPreserved() {
        val bench = exercise(1, "Machine bench")
        val createdExercise = exercise(2, "Cable extension")
        val gymState = mutableStateOf(GymUiState(exercises = listOf(bench), loading = false))
        var createdName: String? = null
        var savedExerciseIds: Set<Long>? = null
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutineBuilderScreen(
                    routineId = 42,
                    gymState = gymState.value,
                    initial = RoutineDraft(
                        name = "Machine push",
                        days = listOf(RoutineDayDraft("A", listOf(RoutineExerciseDraft(bench.id)))),
                    ),
                    onDismiss = {},
                    onSave = { _, complete -> complete(true) },
                    onCreateExercise = { draft, complete ->
                        createdName = draft.name
                        gymState.value = gymState.value.copy(exercises = listOf(bench, createdExercise))
                        complete(createdExercise.id)
                    },
                    onCreateMachine = { draft, complete ->
                        savedExerciseIds = draft.exerciseIds
                        complete(88)
                    },
                )
            }
        }

        compose.onNodeWithText("Machine bench", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("routine-equipment-picker").performClick()
        compose.onNodeWithText("Create Advanced Machine Profile").performClick()
        compose.onNodeWithTag("machine-editor-name").performTextInput("Shared cable")
        compose.onNodeWithTag("machine-editor-list").performScrollToNode(hasTestTag("machine-choose-exercises"))
        compose.onNodeWithTag("machine-choose-exercises").performClick()
        compose.onNodeWithTag("machine-exercise-search").performTextInput("  Cable extension  ")
        compose.onNodeWithTag("machine-create-exercise-empty").performClick()
        compose.onNodeWithTag("exercise-editor-name").assertTextContains("Cable extension")
        compose.onNode(
            hasText("Save") and hasAnyAncestor(hasTestTag("exercise-editor-surface")),
        ).performClick()

        compose.onNodeWithTag("machine-editor-name").assertTextContains("Shared cable")
        compose.onNode(
            hasText("Save") and hasAnyAncestor(hasTestTag("machine-editor-surface")),
        ).performClick()
        compose.runOnIdle {
            assertEquals("Cable extension", createdName)
            assertEquals(setOf(bench.id, createdExercise.id), savedExerciseIds)
        }
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
        compose.onNodeWithText("Choose Equipment").assertIsDisplayed()
        compose.onNodeWithText("Quick-Create Machine for This Exercise").assertIsDisplayed()
        captureVisualCatalogSurface("gym.machine.choice")
        compose.onNodeWithText("Quick-Create Machine for This Exercise").performClick()
        compose.onNodeWithTag("routine-quick-machine-name").assertIsDisplayed()
        compose.onNodeWithTag("routine-quick-machine-create").assertIsDisplayed()
        captureVisualCatalogSurface(
            "gym.quick-machine",
            visuallyDistinctFrom = "gym.machine.choice",
        )
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
        compose.onNode(
            hasText("Zercher Squat") and hasAnyAncestor(hasTestTag("routine-placement-editor")),
            useUnmergedTree = true,
        ).assertIsDisplayed()
        compose.onNodeWithTag("routine-saved-in-place").assertIsDisplayed()
        compose.onNodeWithText("Routine saved. Continue editing Zercher.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back to routine outline").assertIsDisplayed()
        compose.onNodeWithTag("routine-builder-save").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Back to routine outline").performClick()
        compose.onNodeWithContentDescription("Close routine editor").performClick()
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
        compose.onNodeWithTag("routine-placement-editor").performScrollToNode(hasTestTag("routine-rep-schemes-empty"))
        compose.onNodeWithTag("routine-rep-schemes-empty").assertIsDisplayed()
        compose.onNodeWithTag("routine-placement-editor").performScrollToNode(hasTestTag("routine-add-rep-scheme"))
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
        compose.onNodeWithTag("routine-placement-editor").performScrollToNode(hasText("Volume · 3 × 8–12"))
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

    @Test
    fun captureRepSchemeCatalog() {
        val bench = exercise(1, "Bench")
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
                )
            }
        }

        compose.onNodeWithText("Bench", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("routine-placement-editor").performScrollToNode(hasTestTag("routine-add-rep-scheme"))
        compose.onNodeWithTag("routine-add-rep-scheme").performClick()
        compose.onNodeWithText("Add Rep Prescription Scheme").assertIsDisplayed()
        compose.onNodeWithTag("rep-scheme-classification").assertIsDisplayed()
        captureVisualCatalogSurface("gym.rep-scheme")
        compose.onNodeWithTag("rep-scheme-classification").performClick()
        compose.onNodeWithText("Warm-up").assertIsDisplayed()
        compose.onNodeWithText("AMRAP").assertIsDisplayed()
        captureVisualCatalogSurface(
            "gym.classification.menu",
            visuallyDistinctFrom = "gym.rep-scheme",
        )
    }

    private fun exercise(
        id: Long,
        name: String,
        primaryMuscles: String = if (id % 2L == 0L) "Chest" else "Back",
    ) = Exercise(
        id = id,
        uuid = "exercise-$id",
        name = name,
        trackingType = ExerciseTrackingType.WeightReps,
        notes = "",
        equipment = if (id % 2L == 0L) "Barbell" else "Dumbbell",
        primaryMuscles = primaryMuscles,
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
