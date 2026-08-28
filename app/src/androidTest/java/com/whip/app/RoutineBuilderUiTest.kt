package com.whip.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.BodyweightLoadPolicy
import com.whip.app.core.AppSettings
import com.whip.app.core.RepPrescriptionScheme
import com.whip.app.domain.EstimatedOneRepMaxFormula
import com.whip.app.domain.Exercise
import com.whip.app.domain.ExerciseTrackingType
import com.whip.app.domain.RoutineDayDraft
import com.whip.app.domain.RoutineDraft
import com.whip.app.domain.RoutineExerciseDraft
import com.whip.app.domain.WorkoutSetClassification
import com.whip.app.ui.GymUiState
import com.whip.app.ui.RoutineBuilderScreen
import com.whip.app.ui.theme.WhipTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoutineBuilderUiTest {
    @get:Rule
    val compose = createComposeRule()

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
        compose.onNodeWithTag("routine-add-selected").assertTextContains("Add 1 Exercise").performClick()

        compose.onNodeWithTag("routine-placement-editor").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back in routine builder").performClick()
        compose.onNode(
            hasText("Exercise 200") and hasAnyAncestor(hasTestTag("routine-selected-exercises")),
            useUnmergedTree = true,
        ).assertIsDisplayed()
        compose.onNodeWithContentDescription("More options for Exercise 200").assertIsDisplayed()
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
