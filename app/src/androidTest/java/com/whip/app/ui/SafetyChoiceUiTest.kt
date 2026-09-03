package com.whip.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.whip.app.core.WhipResult
import com.whip.app.data.BackupPreview
import com.whip.app.domain.BodyweightLoadPolicy
import com.whip.app.domain.EstimatedOneRepMaxFormula
import com.whip.app.domain.Exercise
import com.whip.app.domain.ExerciseTrackingType
import com.whip.app.domain.WorkoutExercise
import com.whip.app.ui.theme.WhipTheme
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SafetyChoiceUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun workoutCompletionActionsAreDisabledWhileAnotherWorkoutMutationOwnsPersistence() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WorkoutCompletionActions(
                    showGroupAction = true,
                    saving = true,
                    onGroupExercises = {},
                    onFinish = {},
                    onDiscard = {},
                )
            }
        }

        compose.onNodeWithTag("active-workout-group").assertIsNotEnabled()
        compose.onNodeWithTag("active-workout-finish").assertIsNotEnabled()
        compose.onNodeWithTag("active-workout-discard").assertIsNotEnabled()
    }

    @Test
    fun replaceEverythingRequiresFinalConfirmationAndBusyBlocksDuplicates() {
        val replacements = AtomicInteger(0)
        var busy by mutableStateOf(false)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                BackupRestorePreviewDialogs(
                    preview = preview(),
                    busy = busy,
                    onCancel = {},
                    onMerge = {},
                    onReplace = {
                        replacements.incrementAndGet()
                        busy = true
                    },
                )
            }
        }

        compose.onNodeWithTag("request-replace-everything").performClick()
        assertEquals(0, replacements.get())
        compose.onNodeWithText("Replace Everything With This Backup?").assertExists()
        compose.onNodeWithText("private recovery snapshot", substring = true).assertExists()
        compose.onNodeWithText("recoverable only from a backup you exported", substring = true).assertExists()

        compose.onNodeWithTag("confirm-replace-everything").performClick().assertIsNotEnabled()
        assertEquals(1, replacements.get())
        compose.onNodeWithText("Replacing…").assertExists()
    }

    @Test
    fun cancellingFinalReplacementReturnsToPreview() {
        val replacements = AtomicInteger(0)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                BackupRestorePreviewDialogs(
                    preview = preview(),
                    busy = false,
                    onCancel = {},
                    onMerge = {},
                    onReplace = { replacements.incrementAndGet() },
                )
            }
        }

        compose.onNodeWithTag("request-replace-everything").performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Import This Whip Backup?").assertExists()
        assertEquals(0, replacements.get())
    }

    @Test
    fun workoutEditorPristineCancelClosesButDirtyCancelRequiresDiscard() {
        val dismissals = AtomicInteger(0)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WorkoutEditorDialog(
                    session = null,
                    initialDate = LocalDate.of(2026, 8, 29),
                    onDismiss = { dismissals.incrementAndGet() },
                    onStart = { _, _, _, _, _ -> },
                )
            }
        }
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(1, dismissals.get())

        compose.onNodeWithTag("workout-editor-name").performTextInput("Protected draft")
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Discard Unsaved Changes?").assertExists()
        compose.onNodeWithText("Keep Editing").performClick()
        compose.onNodeWithTag("workout-editor-name").assertTextContains("Protected draft")
        assertEquals(1, dismissals.get())
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Discard Changes").performClick()
        assertEquals(2, dismissals.get())
    }

    @Test
    fun workoutGroupDirtyCancelRequiresDiscard() {
        val dismissals = AtomicInteger(0)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WorkoutGroupDialog(
                    exercises = emptyList(),
                    onDismiss = { dismissals.incrementAndGet() },
                    onCreate = { _, _, _ -> true },
                )
            }
        }

        compose.onNodeWithTag("workout-group-name").performTextInput("Intervals")
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.pressBack()
        compose.waitForIdle()
        compose.onAllNodesWithText("Discard Unsaved Changes?").assertCountEquals(0)
        device.pressBack()
        compose.waitForIdle()
        compose.onNodeWithText("Discard Unsaved Changes?").assertExists()
        compose.onNodeWithText("Discard Changes").performClick()
        assertEquals(1, dismissals.get())
    }

    @Test
    fun workoutSaveSubmissionBlocksDuplicateActionsAndDismissal() {
        val starts = AtomicInteger(0)
        val dismissals = AtomicInteger(0)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WorkoutEditorDialog(
                    session = null,
                    initialDate = LocalDate.of(2026, 8, 29),
                    onDismiss = { dismissals.incrementAndGet() },
                    onStart = { _, _, _, _, _ -> starts.incrementAndGet() },
                )
            }
        }

        compose.onNodeWithTag("workout-editor-confirm").performClick().assertIsNotEnabled()
        assertEquals(1, starts.get())
        compose.onNodeWithText("Cancel").assertIsNotEnabled()
        assertEquals(0, dismissals.get())
    }

    @Test
    fun workoutSaveFailureKeepsParentAndDraftOpenForRetry() {
        val starts = AtomicInteger(0)
        val dismissals = AtomicInteger(0)
        val completion = AtomicReference<(WhipResult<Unit>) -> Unit>()
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WorkoutEditorDialog(
                    session = null,
                    initialDate = LocalDate.of(2026, 8, 29),
                    onDismiss = { dismissals.incrementAndGet() },
                    onStart = { _, _, _, _, onFinished ->
                        starts.incrementAndGet()
                        completion.set(onFinished)
                    },
                )
            }
        }

        compose.onNodeWithTag("workout-editor-name").performTextInput("Retry draft")
        compose.onNodeWithTag("workout-editor-confirm").performClick().assertIsNotEnabled()
        compose.runOnIdle { completion.get().invoke(WhipResult.Failure("Database unavailable")) }

        compose.onNodeWithTag("workout-editor-save-error").assertTextContains("Database unavailable")
        compose.onNodeWithTag("workout-editor-name").assertTextContains("Retry draft")
        compose.onNodeWithTag("workout-editor-confirm").assertIsEnabled()
        assertEquals(0, dismissals.get())

        compose.onNodeWithTag("workout-editor-confirm").performClick()
        compose.runOnIdle { completion.get().invoke(WhipResult.Success(Unit)) }
        assertEquals(2, starts.get())
        assertEquals(1, dismissals.get())
    }

    @Test
    fun workoutGroupSaveFailureKeepsParentAndDraftOpenForRetry() {
        val creations = AtomicInteger(0)
        val dismissals = AtomicInteger(0)
        val saving = androidx.compose.runtime.mutableStateOf(false)
        val saveError = androidx.compose.runtime.mutableStateOf<String?>(null)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WorkoutGroupDialog(
                    exercises = listOf(workoutExerciseUi(1), workoutExerciseUi(2)),
                    saving = saving.value,
                    saveError = saveError.value,
                    onDismiss = { dismissals.incrementAndGet() },
                    onCreate = { _, _, _ ->
                        creations.incrementAndGet()
                        saving.value = true
                        true
                    },
                )
            }
        }

        compose.onNodeWithTag("workout-group-name").performTextInput("Retry group")
        compose.onNodeWithTag("workout-group-confirm").performClick()
        compose.onNodeWithTag("workout-group-confirm", useUnmergedTree = true).assertIsNotEnabled()
        compose.onNodeWithTag("workout-group-name", useUnmergedTree = true).assertIsNotEnabled()
        compose.runOnIdle {
            saving.value = false
            saveError.value = "Group write failed"
        }

        compose.onNodeWithTag("workout-group-save-error").assertTextContains("Group write failed")
        compose.onNodeWithTag("workout-group-name").assertTextContains("Retry group")
        compose.onNodeWithTag("workout-group-confirm").assertIsEnabled()
        assertEquals(0, dismissals.get())

        compose.onNodeWithTag("workout-group-confirm").performClick()
        compose.runOnIdle { saving.value = false }
        assertEquals(2, creations.get())
        assertEquals(0, dismissals.get())
    }

    @Test
    fun choiceRowsOwnTheOnlyRoleAndStateSemanticsNodes() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Column {
                    WhipSingleChoiceRow(
                        label = "Personal",
                        selected = true,
                        onSelect = {},
                        accessibilityLabel = "Move to Personal",
                    )
                    WhipMultiChoiceRow(
                        label = "Squat",
                        checked = true,
                        onCheckedChange = {},
                        accessibilityLabel = "Include Squat in group",
                    )
                }
            }
        }

        compose.onNodeWithContentDescription("Move to Personal").assertIsSelected()
        compose.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton),
            useUnmergedTree = true,
        ).assertCountEquals(1)
        compose.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox),
            useUnmergedTree = true,
        ).assertCountEquals(1)
        val checkbox = compose.onNodeWithContentDescription("Include Squat in group").fetchSemanticsNode()
        assertEquals(ToggleableState.On, checkbox.config[SemanticsProperties.ToggleableState])
    }

    private fun preview() = BackupPreview(
        envelopeVersion = 3,
        dataModelEpoch = 2,
        databaseVersion = 19,
        exportedAt = Instant.parse("2026-08-29T12:00:00Z"),
        tableCounts = mapOf("tasks" to 3),
        totalRecords = 3,
        duplicateStableIds = 0,
        checksumValid = true,
        settingsIncluded = true,
        restoreCompatible = true,
    )

    private fun workoutExerciseUi(id: Long): WorkoutExerciseUi {
        val exercise = Exercise(
            id = id,
            uuid = "exercise-$id",
            name = "Exercise $id",
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
            position = id.toInt(),
            archived = false,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        val placement = WorkoutExercise(
            id = id,
            uuid = "placement-$id",
            sessionId = 1,
            exerciseId = id,
            position = id.toInt(),
            notes = "",
            groupId = null,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        return WorkoutExerciseUi(placement, exercise, emptyList(), emptyList(), 0, null, null)
    }
}
