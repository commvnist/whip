package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.WorkoutSetDraft
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditorStateRecreationTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private var workoutSetId: Long = 0

    @Before fun prepareApp() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { it.copy(setupCompleted = true) }
        val exerciseId = app.gymRepository.createExercise(ExerciseDraft(name = "Continuity press"))
        val sessionId = app.gymRepository.startWorkout("Continuity workout")
        val placementId = app.gymRepository.addExerciseToWorkout(sessionId, exerciseId)
        workoutSetId = app.gymRepository.addSet(
            placementId,
            WorkoutSetDraft(weight = 60.0, reps = 5, completed = false),
        )
    }

    @Test fun dirtyTaskEditorSurvivesActivityRecreation() = withActivity {
        openGlobal("Task")
        compose.onNodeWithTag("task-editor-title").performTextInput("Keep task draft")
        it.recreate()
        compose.waitForIdle()
        compose.onNodeWithTag("task-editor-title").assertTextContains("Keep task draft")
    }

    @Test fun dirtyTaskEditorRequiresExplicitDiscardOnSystemBack() = withActivity { scenario ->
        openGlobal("Task")
        compose.onNodeWithTag("task-editor-title").performTextInput("Do not lose this")
        compose.waitForIdle()

        pressSystemBack()

        compose.onNodeWithText("Discard Unsaved Changes?").assertIsDisplayed()
        compose.onNodeWithText("Keep Editing").performClick()
        compose.onNodeWithTag("task-editor-title").assertTextContains("Do not lose this")
    }

    @Test fun taskEditorFooterAcceptsAPhysicalTapWhileTheImeIsOpen() = withActivity {
        openGlobal("Task")
        compose.onNodeWithTag("task-editor-title").performTextInput("Keyboard-safe task")
        compose.waitForIdle()

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // UiDevice coordinates are physical-screen coordinates. Compose boundsInRoot
        // are local to the dialog window and diverge on edge-to-edge/fold layouts.
        device.wait(Until.findObject(By.text("Cancel")), 3_000).click()

        compose.waitUntil(3_000) {
            compose.onAllNodesWithText("Discard Unsaved Changes?").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Discard Unsaved Changes?").assertIsDisplayed()
        compose.onNodeWithText("Discard Changes").performClick()
    }

    @Test fun primaryEntityEditorsRequireExplicitDiscardOnBack() = withActivity {
        fun verify(globalLabel: String, tag: String, value: String) {
            openGlobal(globalLabel)
            compose.onNodeWithTag(tag).performTextInput(value)
            compose.waitForIdle()
            pressSystemBack()
            compose.onNodeWithText("Discard Unsaved Changes?").assertIsDisplayed()
            compose.onNodeWithText("Discard Changes").performClick()
        }

        verify("Habit", "habit-editor-name", "Protected habit")
        verify("Goal", "goal-editor-name", "Protected goal")
        verify("Exercise", "exercise-editor-name", "Protected exercise")

        openGymDestination("Machines")
        compose.onNodeWithText("Create Machine Profile").performScrollTo().performClick()
        compose.onNodeWithTag("machine-editor-name").performTextInput("Protected machine")
        compose.waitForIdle()
        pressSystemBack()
        compose.onNodeWithText("Discard Unsaved Changes?").assertIsDisplayed()
        compose.onNodeWithText("Discard Changes").performClick()

        openGymDestination("Routines")
        compose.onNodeWithText("Create Routine").performScrollTo().performClick()
        compose.onNodeWithTag("routine-editor-name").performTextInput("Protected routine")
        compose.waitForIdle()
        pressSystemBack()
        compose.onNodeWithText("Discard Unsaved Changes?").assertIsDisplayed()
    }

    @Test fun dirtyHabitEditorSurvivesActivityRecreation() = withActivity {
        openGlobal("Habit")
        compose.onNodeWithTag("habit-editor-name").performTextInput("Keep habit draft")
        it.recreate()
        compose.waitForIdle()
        compose.onNodeWithTag("habit-editor-name").assertTextContains("Keep habit draft")
    }

    @Test fun dirtyGoalEditorSurvivesActivityRecreation() = withActivity {
        openGlobal("Goal")
        compose.onNodeWithTag("goal-editor-name").performTextInput("Keep goal draft")
        it.recreate()
        compose.waitForIdle()
        compose.onNodeWithTag("goal-editor-name").assertTextContains("Keep goal draft")
    }

    @Test fun dirtyExerciseEditorSurvivesActivityRecreation() = withActivity {
        openGlobal("Exercise")
        compose.onNodeWithTag("exercise-editor-name").performTextInput("Keep exercise draft")
        it.recreate()
        compose.waitForIdle()
        compose.onNodeWithTag("exercise-editor-name").assertTextContains("Keep exercise draft")
    }

    @Test fun dirtyExerciseEditorRequiresExplicitDiscardOnSystemBack() = withActivity {
        openGlobal("Exercise")
        compose.onNodeWithTag("exercise-editor-name").performTextInput("Protected exercise")
        compose.waitForIdle()
        pressSystemBack()
        compose.onNodeWithText("Discard Unsaved Changes?").assertIsDisplayed()
        compose.onNodeWithText("Discard Changes").performClick()
    }

    @Test fun dirtyMachineEditorSurvivesActivityRecreation() = withActivity {
        openGymDestination("Machines")
        compose.onNodeWithText("Create Machine Profile").performScrollTo().performClick()
        compose.onNodeWithTag("machine-editor-name").performTextInput("Keep machine draft")
        it.recreate()
        compose.waitForIdle()
        compose.onNodeWithTag("machine-editor-name").assertTextContains("Keep machine draft")
    }

    @Test fun dirtyRoutineEditorSurvivesActivityRecreation() = withActivity {
        openGymDestination("Routines")
        compose.onNodeWithText("Create Routine").performScrollTo().performClick()
        compose.onNodeWithTag("routine-editor-name").performTextInput("Keep routine draft")
        it.recreate()
        compose.waitForIdle()
        compose.onNodeWithTag("routine-editor-name").assertTextContains("Keep routine draft")
    }

    @Test fun dirtyInlineSetSurvivesActivityRecreation() = withActivity {
        compose.onNodeWithContentDescription("Gym tab").performClick()
        compose.onNodeWithTag("quick-set-load-$workoutSetId").performTextClearance()
        compose.onNodeWithTag("quick-set-load-$workoutSetId").performTextInput("72.5")
        it.recreate()
        compose.waitForIdle()
        compose.onNodeWithTag("quick-set-load-$workoutSetId").assertTextContains("72.5")
    }

    private fun openGlobal(label: String) {
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Home tab").performClick()
        compose.onNodeWithContentDescription("Add task, habit, goal, exercise, workout, or measurement").performClick()
        compose.onNodeWithText(label).performClick()
    }

    private fun pressSystemBack() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // Android may consume Back while the IME is hiding, especially for a platform
        // AlertDialog. Wait for that asynchronous window transition before one retry.
        device.pressBack()
        device.waitForIdle(2_000)
        compose.waitForIdle()
        if (compose.onAllNodesWithText("Discard Unsaved Changes?").fetchSemanticsNodes().isEmpty()) {
            device.pressBack()
            device.waitForIdle(2_000)
            compose.waitUntil(3_000) {
                compose.onAllNodesWithText("Discard Unsaved Changes?").fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    private fun openGymDestination(label: String) {
        compose.onNodeWithContentDescription("Gym tab").performClick()
        if (label in setOf("Routines", "Exercises", "Machines", "Categories", "Tools")) {
            compose.onNodeWithTag("gym-destination-Library").performClick()
            compose.onNodeWithTag("gym-library-$label").performClick()
            compose.onNodeWithTag("gym-destination-Library").assertIsSelected()
        } else {
            compose.onNodeWithTag("gym-destination-$label").performClick().assertIsSelected()
        }
        compose.waitForIdle()
        val readyText = when (label) {
            "Machines" -> "Create Machine Profile"
            "Routines" -> "Create Routine"
            else -> null
        }
        readyText?.let { expected ->
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText(expected).fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    private fun withActivity(block: (ActivityScenario<MainActivity>) -> Unit) {
        ActivityScenario.launch<MainActivity>(
            Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
                .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true),
        ).use(block)
    }
}
