package com.whip.app

import android.app.ActivityOptions
import android.content.Intent
import android.view.Display
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.WorkoutSetDraft
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WhipComposeSemanticsTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun resetProductState() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { it.copy(setupCompleted = true, powerMode = false) }
    }

    @Test
    fun globalAddAndHabitEditorAreReachableThroughComposeSemantics() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java,
        ).putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(Display.DEFAULT_DISPLAY).toBundle()

        ActivityScenario.launch<MainActivity>(intent, options).use {
            compose.waitForIdle()
            compose.onNodeWithContentDescription("Home").assertIsDisplayed()
            compose.onNodeWithContentDescription("Add task, habit, goal, track, exercise, or workout").performClick()
            compose.onNodeWithText("Habit").assertIsDisplayed().performClick()
            compose.onNodeWithText("Name *").assertIsDisplayed()
            compose.onNodeWithContentDescription("Cancel Habit editing").performClick()
            compose.onNodeWithContentDescription("Tasks tab").performClick()
            compose.onNodeWithTag("task-destination-Upcoming").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            compose.onNodeWithText("The next 30 days", substring = true).assertIsDisplayed()
            compose.onNodeWithText("Agenda").assertIsDisplayed().performClick()
            compose.onNodeWithText("Calendar").assertIsDisplayed().performClick()
            compose.onNodeWithTag("task-calendar").performScrollTo()
            compose.onNodeWithText("Previous").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            compose.waitForIdle()
            compose.onNodeWithText("July 2026").fetchSemanticsNode()
            openSettings()
            compose.onNodeWithTag("settings-section-Appearance & Home").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Home Overview"))
            compose.onNodeWithText("Home Overview").assertIsDisplayed()
            compose.onNodeWithText("All Settings").performClick()
            compose.onNodeWithTag("settings-section-Planning & Units").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Create reusable units", substring = true))
            compose.onNodeWithText("Create reusable units", substring = true).assertIsDisplayed()
            compose.onNodeWithText("All Settings").performClick()
            compose.onNodeWithTag("settings-section-Organization").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Manage Areas"))
            compose.onNodeWithText("Manage Areas").assertIsDisplayed()
            compose.onNodeWithText("All Settings").performClick()
            compose.onNodeWithTag("settings-section-Reminders").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Delivery Details"))
            compose.onNodeWithTag("notification-diagnostics").assertIsDisplayed()
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Send Test Notification"))
            compose.onNodeWithText("Send Test Notification").assertIsDisplayed()
            compose.onNodeWithText("All Settings").performClick()
            compose.onNodeWithTag("settings-section-Data & Privacy").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Portable Backup Folder"))
            compose.onNodeWithText("Portable Backup Folder").assertIsDisplayed()
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Choose Backup Folder"))
            compose.onNodeWithText("Choose Backup Folder").assertIsDisplayed()
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Reset Whip and Delete All Data"))
            compose.onAllNodesWithText("FitNotes", substring = true).assertCountEquals(0)
            compose.onNodeWithText("All Settings").performClick()
            compose.onNodeWithTag("settings-section-About Whip").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("About Whip"))
            compose.onNodeWithTag("about-build-identity").assertIsDisplayed()
        }
    }

    private fun openSettings() {
        if (compose.onAllNodesWithContentDescription("Open Settings").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithContentDescription("Open Settings").performClick()
        } else {
            compose.onNodeWithContentDescription("App actions").performClick()
            compose.onNodeWithText("Open Settings").performClick()
        }
    }

    @Test
    fun machineLibraryExplainsMachineScopedTracking() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(Display.DEFAULT_DISPLAY).toBundle()

        ActivityScenario.launch<MainActivity>(intent, options).use {
            compose.waitForIdle()
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithTag("gym-destination-Library").assertIsDisplayed().performClick()
            compose.onNodeWithTag("gym-library-Machines").performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("gym-destination-Library").assertIsSelected()
            compose.onNodeWithTag("gym-machine-list").assertIsDisplayed()
            compose.onNodeWithText("keeps its history", substring = true).assertIsDisplayed()
            device.pressBack()
            compose.waitForIdle()
            compose.onNodeWithTag("gym-library-Machines").assertIsDisplayed()
        }
    }

    @Test
    fun taskToolbarKeepsTemporaryControlsAttachedAndFocused() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<WhipApplication>()
            app.taskRepository.create(
                TaskDraft(
                    title = "Toolbar test task",
                    scheduleKind = ScheduleKind.Once,
                    date = app.clock.today(),
                ),
            )
        }
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(Display.DEFAULT_DISPLAY).toBundle()

        ActivityScenario.launch<MainActivity>(intent, options).use {
            compose.waitForIdle()
            compose.onNodeWithContentDescription("Tasks tab").performClick()
            compose.onNodeWithContentDescription("Filter & Sort Tasks").performClick()
            compose.onNodeWithText("Sort, Group & Filter Tasks").assertIsDisplayed()
            compose.onNodeWithText("Done").performClick()
            compose.onNodeWithContentDescription("Search Tasks").performClick()
            compose.onNodeWithTag("unified-search-query").assertIsDisplayed()
            compose.onNodeWithText("Close").performClick()
            compose.onNodeWithContentDescription("More task list actions").performClick()
            compose.onNodeWithText("Select Tasks").performClick()
            compose.onNodeWithText("0 selected").assertIsDisplayed()
            compose.onNodeWithTag("task-destination-Inbox").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            compose.onNodeWithTag("task-quick-capture").assertIsDisplayed()
        }
    }

    @Test
    fun gymConfigurationControlsUseProgressiveDisclosure() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<WhipApplication>()
            val exerciseId = app.gymRepository.createExercise(ExerciseDraft("Graph controls test"))
            val workoutId = app.gymRepository.startWorkout("Graph controls history")
            val placementId = app.gymRepository.addExerciseToWorkout(workoutId, exerciseId)
            app.gymRepository.addSet(
                placementId,
                WorkoutSetDraft(weight = 50.0, reps = 5, completed = true),
            )
            app.gymRepository.finishWorkout(workoutId)
        }
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(Display.DEFAULT_DISPLAY).toBundle()

        ActivityScenario.launch<MainActivity>(intent, options).use {
            compose.waitForIdle()
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithTag("gym-destination-History")
                .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            compose.onNodeWithText("History Options").assertIsDisplayed()
            compose.onAllNodesWithText("Calendar View").assertCountEquals(0)
            compose.onNodeWithText("History Options").performClick()
            compose.onNodeWithText("Calendar View").assertIsDisplayed()

            compose.onNodeWithTag("gym-destination-Progress")
                .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            compose.onNodeWithText("Graph Options").performScrollTo().assertIsDisplayed()
            compose.onAllNodesWithText("Range").assertCountEquals(0)
            compose.onNodeWithText("Graph Options").performClick()
            compose.onNodeWithText("Range").assertIsDisplayed()
            compose.onNodeWithTag("gym-progress-list").performScrollToNode(hasText("Graph Presets"))
            compose.onNodeWithText("Graph Presets").assertIsDisplayed()
            compose.onAllNodesWithText("Preset name").assertCountEquals(0)
            compose.onNodeWithText("Graph Presets").performClick()
            compose.onNodeWithText("Preset name").assertIsDisplayed()
        }
    }

    @Test
    fun gymDestinationSurvivesExpandingAndRestoringTheFoldPane() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(Display.DEFAULT_DISPLAY).toBundle()

        ActivityScenario.launch<MainActivity>(intent, options).use {
            compose.waitForIdle()
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithTag("gym-destination-Library").performClick()
            compose.onNodeWithTag("gym-library-Routines").performClick()
            compose.onNodeWithTag("gym-destination-Library").assertIsSelected()
            compose.onNodeWithTag("gym-library-child-Routines").assertIsDisplayed()
            if (compose.onAllNodesWithContentDescription("App actions").fetchSemanticsNodes().isEmpty()) {
                return@use
            }
            compose.onNodeWithContentDescription("App actions").performClick()
            compose.onNodeWithTag("expand-content-pane-action").performClick()
            compose.onNodeWithTag("gym-destination-Library").assertIsSelected()
            compose.onNodeWithTag("gym-library-child-Routines").assertIsDisplayed()
            compose.onNodeWithContentDescription("Restore split view").performClick()
            compose.onNodeWithTag("gym-destination-Library").assertIsSelected()
            compose.onNodeWithTag("gym-library-child-Routines").assertIsDisplayed()
        }
    }

    @Test
    fun foldSupportPaneKeepsGymContextSeparateFromPrimaryNavigation() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(Display.DEFAULT_DISPLAY).toBundle()

        ActivityScenario.launch<MainActivity>(intent, options).use {
            compose.waitForIdle()
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onAllNodesWithTag("fold-gym-destination-library").assertCountEquals(0)
            compose.onAllNodesWithTag("fold-gym-destination-Routines").assertCountEquals(0)
            compose.onNodeWithTag("gym-destination-Library").assertIsDisplayed()
        }
    }

    @Test
    fun taskHabitAndGoalEditorsStartInBasicModeAndRevealAdvancedFields() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(Display.DEFAULT_DISPLAY).toBundle()

        ActivityScenario.launch<MainActivity>(intent, options).use {
            compose.waitForIdle()

            compose.onNodeWithContentDescription("Add task, habit, goal, track, exercise, or workout").performClick()
            compose.onNodeWithText("Task").performClick()
            compose.onAllNodesWithText("Notes (Optional)").assertCountEquals(0)
            compose.onNodeWithTag("task-editor-more-details").performScrollTo().performClick()
            compose.onNodeWithText("Planning").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Priority").performScrollTo().assertIsDisplayed()
            compose.onAllNodesWithText("Location cue").assertCountEquals(0)
            compose.onNodeWithContentDescription("Cancel Task editing").performClick()

            compose.onNodeWithContentDescription("Home").performClick()
            compose.onNodeWithContentDescription("Add task, habit, goal, track, exercise, or workout").performClick()
            compose.onNodeWithText("Habit").performClick()
            compose.onAllNodesWithText("Notes").assertCountEquals(0)
            compose.onNodeWithText("How do you want to track it?").assertIsDisplayed()
            compose.onAllNodesWithText("Intent").assertCountEquals(0)
            compose.onAllNodesWithText("Target rule").assertCountEquals(0)
            compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("Advanced Options"))
            compose.onNodeWithText("Advanced Options").assertIsDisplayed().performClick()
            compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("Notes"))
            compose.onNodeWithText("Notes").assertIsDisplayed()
            compose.onAllNodesWithText("Intent").assertCountEquals(0)
            compose.onNodeWithTag("habit-editor-fields").performScrollToNode(
                hasText("One check completes each scheduled occurrence. No numeric value is required."),
            )
            compose.onNodeWithText("One check completes each scheduled occurrence. No numeric value is required.").assertIsDisplayed()
            compose.onAllNodesWithText("Target rule").assertCountEquals(0)
            compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("Count"))
            compose.onNodeWithText("Count").performClick()
            compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("Target rule"))
            compose.onNodeWithText("Target rule").assertIsDisplayed()
            compose.onNodeWithContentDescription("Cancel Habit editing").performClick()
            compose.onNodeWithText("Discard Changes")
                .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag("habit-editor-surface").fetchSemanticsNodes().isEmpty()
            }

            compose.onNodeWithContentDescription("Go to Home").performClick()
            compose.onNodeWithContentDescription("Add task, habit, goal, track, exercise, or workout").performClick()
            compose.onNodeWithText("Goal").performClick()
            compose.onAllNodesWithText("Description").assertCountEquals(0)
            compose.onNodeWithTag("goal-editor-fields").performScrollToNode(hasText("Starting Value (Optional)"))
            compose.onNodeWithText("Starting Value (Optional)").assertIsDisplayed()
            compose.onNodeWithText("Move from a starting value", substring = true).assertIsDisplayed()
            compose.onNodeWithTag("goal-editor-fields").performScrollToNode(hasText("Advanced Options"))
            compose.onNodeWithText("Advanced Options").assertIsDisplayed().performClick()
            compose.onNodeWithTag("goal-editor-fields").performScrollToNode(hasText("Description"))
            compose.onNodeWithText("Description").assertIsDisplayed()
            compose.onNodeWithContentDescription("Cancel Goal editing").performClick()

            compose.onNodeWithContentDescription("Goals tab").performClick()
            compose.onNodeWithContentDescription("More Goal Actions").performClick()
            compose.onNodeWithTag("goal-browse-templates-menu-action").performClick()
            compose.onNodeWithTag("goal-template-list").performScrollToNode(hasText("Build Savings", substring = true))
            compose.onNodeWithText("Build Savings", substring = true).assertIsDisplayed().performClick()
            compose.onNodeWithText("Savings").assertIsDisplayed()
        }
    }
}
