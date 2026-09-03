package com.whip.app

import android.content.Intent
import android.os.Build
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.semantics.SemanticsProperties
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
    private lateinit var currentScenario: ActivityScenario<MainActivity>

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
        waitForTag("task-editor-title")
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            requireNotNull(device.wait(Until.findObject(By.desc("Cancel Task editing")), 5_000)).click()
        } else {
            compose.onNodeWithContentDescription("Cancel Task editing").performClick()
        }

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

        openExerciseEditorFromGym()
        compose.onNodeWithTag("exercise-editor-name").performTextInput("Protected exercise")
        compose.waitForIdle()
        pressSystemBack()
        compose.onNodeWithText("Discard Unsaved Changes?").assertIsDisplayed()
        compose.onNodeWithText("Discard Changes").performClick()

        openGymDestination("Machines")
        compose.onAllNodesWithText("Create Machine")[0].performScrollTo().performClick()
        compose.onNodeWithTag("machine-editor-name").performTextInput("Protected machine")
        compose.waitForIdle()
        pressSystemBack()
        compose.onNodeWithText("Discard Unsaved Changes?").assertIsDisplayed()
        compose.onNodeWithText("Discard Changes").performClick()

        openGymDestination("Routines")
        compose.onAllNodesWithText("Create Routine")[0].performScrollTo().performClick()
        compose.onNodeWithTag("routine-editor-name").performTextInput("Protected routine")
        compose.waitForIdle()
        pressSystemBack()
        compose.onNodeWithText("Discard Unsaved Changes?").assertIsDisplayed()
    }

    @Test fun dirtyHabitEditorSurvivesActivityRecreation() = withActivity {
        openGlobal("Habit")
        compose.onNodeWithTag("habit-editor-name").performTextInput("Keep habit draft")
        it.recreate()
        waitForTag("habit-editor-name")
        compose.onNodeWithTag("habit-editor-name").assertTextContains("Keep habit draft")
    }

    @Test fun dirtyGoalEditorSurvivesActivityRecreation() = withActivity {
        openGlobal("Goal")
        compose.onNodeWithTag("goal-editor-name").performTextInput("Keep goal draft")
        it.recreate()
        waitForTag("goal-editor-name")
        compose.onNodeWithTag("goal-editor-name").assertTextContains("Keep goal draft")
    }

    @Test fun dirtyTrackEditorIsRootOwnedAndSurvivesActivityRecreation() = withActivity {
        openGlobal("Track")
        compose.onNodeWithTag("track-editor-name").performTextInput("Keep Track draft")
        check(
            compose.onNodeWithTag("app-background-shell").fetchSemanticsNode().config
                .contains(SemanticsProperties.HideFromAccessibility),
        ) { "The background shell must be hidden from accessibility while the Track editor owns the foreground" }
        it.recreate()
        waitForTag("track-editor-name")
        compose.onNodeWithTag("track-editor-name").assertTextContains("Keep Track draft")
        check(
            compose.onNodeWithTag("app-background-shell").fetchSemanticsNode().config
                .contains(SemanticsProperties.HideFromAccessibility),
        )
    }

    @Test fun dirtyExerciseEditorSurvivesActivityRecreation() = withActivity {
        openExerciseEditorFromGym()
        compose.onNodeWithTag("exercise-editor-name").performTextInput("Keep exercise draft")
        it.recreate()
        waitForTag("exercise-editor-name")
        compose.onNodeWithTag("exercise-editor-name").assertTextContains("Keep exercise draft")
    }

    @Test fun dirtyExerciseEditorRequiresExplicitDiscardOnSystemBack() = withActivity {
        openExerciseEditorFromGym()
        compose.onNodeWithTag("exercise-editor-name").performTextInput("Protected exercise")
        compose.waitForIdle()
        pressSystemBack()
        compose.onNodeWithText("Discard Unsaved Changes?").assertIsDisplayed()
        compose.onNodeWithText("Discard Changes").performClick()
    }

    @Test fun dirtyMachineEditorSurvivesActivityRecreation() = withActivity {
        openGymDestination("Machines")
        compose.onAllNodesWithText("Create Machine")[0].performScrollTo().performClick()
        compose.onNodeWithTag("machine-editor-name").performTextInput("Keep machine draft")
        it.recreate()
        waitForTag("machine-editor-name")
        compose.onNodeWithTag("machine-editor-name").assertTextContains("Keep machine draft")
    }

    @Test fun dirtyRoutineEditorSurvivesActivityRecreation() = withActivity {
        openGymDestination("Routines")
        compose.onAllNodesWithText("Create Routine")[0].performScrollTo().performClick()
        compose.onNodeWithTag("routine-editor-name").performTextInput("Keep routine draft")
        it.recreate()
        waitForTag("routine-editor-name")
        compose.onNodeWithTag("routine-editor-name").assertTextContains("Keep routine draft")
    }

    @Test fun categoryEditorRetainsItsDraftAcrossRecreationAndClosesOnlyAfterSave() = withActivity {
        openGymDestination("Categories")
        compose.onNodeWithTag("workspace-add-action").performClick()
        waitForTag("gym-category-name")
        compose.onNodeWithTag("gym-category-name").performTextInput("Posterior chain")
        compose.onNodeWithTag("gym-category-type").performTextClearance()
        compose.onNodeWithTag("gym-category-type").performTextInput("Movement family")

        it.recreate()

        waitForTag("gym-category-name")
        compose.onNodeWithTag("gym-category-name").assertTextContains("Posterior chain")
        compose.onNodeWithTag("gym-category-type").assertTextContains("Movement family")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("gym-category-editor").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithText("Posterior chain").assertIsDisplayed()
    }

    @Test fun dirtyInlineSetSurvivesActivityRecreation() = withActivity {
        compose.onNodeWithContentDescription("Gym tab").performClick()
        compose.onNodeWithTag("active-workout-list").performScrollToNode(
            hasTestTag("quick-set-load-$workoutSetId"),
        )
        compose.onNodeWithTag("quick-set-load-$workoutSetId").performTextClearance()
        compose.onNodeWithTag("quick-set-load-$workoutSetId").performTextInput("72.5")
        it.recreate()
        waitForTag("quick-set-load-$workoutSetId")
        compose.onNodeWithTag("quick-set-load-$workoutSetId").assertTextContains("72.5")
    }

    private fun waitForTag(tag: String) {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openGlobal(label: String) {
        compose.waitForIdle()
        if (compose.onAllNodesWithContentDescription("Go to Home").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithContentDescription("Go to Home").performClick()
        } else {
            compose.onNodeWithContentDescription("Home").performClick()
        }
        compose.onNodeWithContentDescription("Add task, habit, goal, track, or workout").performClick()
        compose.onNodeWithText("New $label").performClick()
    }

    private fun pressSystemBack() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            currentScenario.onActivity { activity -> activity.onBackPressedDispatcher.onBackPressed() }
            compose.waitForIdle()
            return
        }
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // Android may consume Back while the IME is hiding, especially for a platform
        // AlertDialog. Retry the physical action, but never treat an IME-only Back
        // as evidence that the editor accepted a destructive dismissal.
        repeat(3) {
            device.pressBack()
            device.waitForIdle(2_000)
            compose.waitForIdle()
            if (compose.onAllNodesWithText("Discard Unsaved Changes?").fetchSemanticsNodes().isNotEmpty()) return
        }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Discard Unsaved Changes?").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openGymDestination(label: String) {
        compose.onNodeWithContentDescription("Gym tab").performClick()
        if (label in setOf("Routines", "Exercises", "Machines", "Categories", "Tools")) {
            compose.onNodeWithTag("gym-destination-Library").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("gym-library-$label").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("gym-library-$label").performClick()
            compose.onNodeWithTag("gym-destination-Library").assertIsSelected()
        } else {
            compose.onNodeWithTag("gym-destination-$label").performClick().assertIsSelected()
        }
        compose.waitForIdle()
        val readyText = when (label) {
            "Machines" -> "Create Machine"
            "Routines" -> "Create Routine"
            else -> null
        }
        readyText?.let { expected ->
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText(expected).fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    private fun openExerciseEditorFromGym() {
        openGymDestination("Exercises")
        compose.onNodeWithTag("workspace-add-action").performClick()
        waitForTag("exercise-editor-name")
    }

    private fun withActivity(block: (ActivityScenario<MainActivity>) -> Unit) {
        ActivityScenario.launch<MainActivity>(
            Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
                .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true),
        ).use { scenario ->
            currentScenario = scenario
            block(scenario)
        }
    }
}
