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
            compose.onNodeWithContentDescription("Home tab").assertIsDisplayed()
            compose.onNodeWithContentDescription("Add task, habit, goal, exercise, workout, or measurement").performClick()
            compose.onNodeWithText("Habit").assertIsDisplayed().performClick()
            compose.onNodeWithText("Name *").assertIsDisplayed()
            compose.onNodeWithText("Cancel").performClick()
            compose.onNodeWithContentDescription("Tasks tab").performClick()
            compose.onNodeWithTag("task-destination-Upcoming").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            compose.onNodeWithText("The next 30 days", substring = true).assertIsDisplayed()
            compose.onNodeWithText("Agenda").assertIsDisplayed().performClick()
            compose.onNodeWithText("Calendar").assertIsDisplayed().performClick()
            compose.onNodeWithTag("task-calendar").performScrollTo()
            compose.onNodeWithText("Previous").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            compose.waitForIdle()
            compose.onNodeWithText("July 2026").fetchSemanticsNode()
            compose.onNodeWithContentDescription("Open Settings").performClick()
            compose.onNodeWithTag("settings-section-Planning & Units").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Create reusable units", substring = true))
            compose.onNodeWithText("Create reusable units", substring = true).assertIsDisplayed()
            compose.onNodeWithTag("settings-section-Reminders & Integrations").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Delivery Diagnostics"))
            compose.onNodeWithTag("notification-diagnostics").assertIsDisplayed()
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Send Test Notification"))
            compose.onNodeWithText("Send Test Notification").assertIsDisplayed()
            compose.onNodeWithTag("settings-section-Data & Privacy").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Portable Backup Folder"))
            compose.onNodeWithText("Portable Backup Folder").assertIsDisplayed()
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Choose Backup Folder"))
            compose.onNodeWithText("Choose Backup Folder").assertIsDisplayed()
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Delete All Local Data"))
            compose.onAllNodesWithText("FitNotes", substring = true).assertCountEquals(0)
            compose.onNodeWithTag("settings-section-Advanced").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("About Whip"))
            compose.onNodeWithTag("about-build-identity").assertIsDisplayed()
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
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(Display.DEFAULT_DISPLAY).toBundle()

        ActivityScenario.launch<MainActivity>(intent, options).use {
            compose.waitForIdle()
            compose.onNodeWithContentDescription("Tasks tab").performClick()
            compose.onNodeWithText("Filter & Sort").performClick()
            compose.onNodeWithText("Filter & Sort Tasks").assertIsDisplayed()
            compose.onNodeWithText("Done").performClick()
            compose.onNodeWithText("Search Tasks").performClick()
            compose.onNodeWithText("Search Tasks and Steps").assertIsDisplayed()
            compose.onNodeWithContentDescription("Close Search Tasks").performClick()
            compose.onNodeWithContentDescription("More task list actions").performClick()
            compose.onNodeWithText("Select Tasks").performClick()
            compose.onNodeWithText("0 selected").assertIsDisplayed()
            compose.onNodeWithTag("task-destination-Inbox").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            compose.onNodeWithText("Opens the full task editor before anything is saved.").assertIsDisplayed()
        }
    }

    @Test
    fun gymConfigurationControlsUseProgressiveDisclosure() {
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
            compose.onNodeWithText("Graph Options").assertIsDisplayed()
            compose.onAllNodesWithText("Range").assertCountEquals(0)
            compose.onNodeWithText("Graph Options").performClick()
            compose.onNodeWithText("Range").assertIsDisplayed()
            compose.onNodeWithText("Graph Presets").performScrollTo().assertIsDisplayed()
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
            compose.onNodeWithContentDescription("Expand content pane").performClick()
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

            compose.onNodeWithContentDescription("Add task, habit, goal, exercise, workout, or measurement").performClick()
            compose.onNodeWithText("Task").performClick()
            compose.onAllNodesWithText("Notes (optional)").assertCountEquals(0)
            compose.onNodeWithText("Advanced Options").performClick()
            compose.onNodeWithText("Notes (optional)").assertIsDisplayed()
            compose.onNodeWithText("Priority").performScrollTo().assertIsDisplayed()
            compose.onAllNodesWithText("Location cue").assertCountEquals(0)
            compose.onNodeWithText("Cancel").performClick()

            compose.onNodeWithContentDescription("Home tab").performClick()
            compose.onNodeWithContentDescription("Add task, habit, goal, exercise, workout, or measurement").performClick()
            compose.onNodeWithText("Habit").performClick()
            compose.onAllNodesWithText("Notes").assertCountEquals(0)
            compose.onNodeWithText("How do you want to track it?").assertIsDisplayed()
            compose.onAllNodesWithText("Intent").assertCountEquals(0)
            compose.onAllNodesWithText("Target rule").assertCountEquals(0)
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
            compose.onNodeWithText("Cancel").performClick()

            compose.onNodeWithContentDescription("Home tab").performClick()
            compose.onNodeWithContentDescription("Add task, habit, goal, exercise, workout, or measurement").performClick()
            compose.onNodeWithText("Goal").performClick()
            compose.onAllNodesWithText("Description").assertCountEquals(0)
            compose.onNodeWithText("Starting value (optional)").assertIsDisplayed()
            compose.onNodeWithText("Move from a starting value", substring = true).assertIsDisplayed()
            compose.onNodeWithText("Advanced Options").assertIsDisplayed().performClick()
            compose.onNodeWithTag("goal-editor-fields").performScrollToNode(hasText("Description"))
            compose.onNodeWithText("Description").assertIsDisplayed()
            compose.onNodeWithText("Cancel").performClick()

            compose.onNodeWithContentDescription("Goals tab").performClick()
            compose.onNodeWithText("Templates").performScrollTo().performClick()
            compose.onNodeWithTag("goal-template-list").performScrollToNode(hasText("Build Savings", substring = true))
            compose.onNodeWithText("Build Savings", substring = true).assertIsDisplayed().performClick()
            compose.onNodeWithText("Savings").assertIsDisplayed()
        }
    }
}
