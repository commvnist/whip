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
            compose.onNodeWithText("More").performClick()
            compose.onNodeWithText("Anytime").assertIsDisplayed().performClick()
            compose.onNodeWithText("Unscheduled tasks", substring = true).assertIsDisplayed()
            compose.onNodeWithText("Workspace tools").performClick()
            compose.onNodeWithText("Agenda").assertIsDisplayed().performClick()
            compose.onNodeWithText("Calendar").assertIsDisplayed().performClick()
            compose.onNodeWithTag("task-calendar").assertIsDisplayed()
            compose.onNodeWithContentDescription("Open settings").performClick()
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Custom units are not limited", substring = true))
            compose.onNodeWithText("Custom units are not limited", substring = true).assertIsDisplayed()
            compose.onNodeWithText("Reminders").performClick()
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Delivery diagnostics"))
            compose.onNodeWithTag("notification-diagnostics").assertIsDisplayed()
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Send test notification"))
            compose.onNodeWithText("Send test notification").assertIsDisplayed()
            compose.onNodeWithText("Data & backup").performClick()
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Portable backup folder"))
            compose.onNodeWithText("Portable backup folder").assertIsDisplayed()
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Choose backup folder"))
            compose.onNodeWithText("Choose backup folder").assertIsDisplayed()
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Delete all local data"))
            compose.onAllNodesWithText("FitNotes", substring = true).assertCountEquals(0)
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
            compose.onAllNodesWithTag("gym-destination-Machines").assertCountEquals(0)
            compose.onNodeWithTag("gym-destination-library").performClick()
            compose.onNodeWithTag("gym-destination-Machines").performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("gym-machine-list").assertIsDisplayed()
            compose.onNodeWithText("keeps its history", substring = true).assertIsDisplayed()
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
            compose.onNodeWithTag("gym-destination-library").performClick()
            compose.onNodeWithTag("gym-destination-Routines").performClick().assertIsSelected()
            compose.onNodeWithContentDescription("Expand content pane").performClick()
            compose.onNodeWithTag("gym-destination-Routines").assertIsSelected()
            compose.onNodeWithContentDescription("Restore split view").performClick()
            compose.onNodeWithTag("gym-destination-Routines").assertIsSelected()
        }
    }

    @Test
    fun foldSupportPaneProvidesUsefulGymNavigation() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(Display.DEFAULT_DISPLAY).toBundle()

        ActivityScenario.launch<MainActivity>(intent, options).use {
            compose.waitForIdle()
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithTag("fold-gym-destination-library").performClick()
            compose.onNodeWithTag("fold-gym-destination-Routines").performClick()
            compose.onNodeWithTag("gym-destination-Routines").assertIsSelected()
            compose.onNodeWithTag("fold-gym-destination-Routines").assertIsSelected()
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
            compose.onNodeWithText("Show advanced options").performClick()
            compose.onNodeWithText("Notes (optional)").assertIsDisplayed()
            compose.onNodeWithText("Priority").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Location cue").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Cancel").performClick()

            compose.onNodeWithContentDescription("Home tab").performClick()
            compose.onNodeWithContentDescription("Add task, habit, goal, exercise, workout, or measurement").performClick()
            compose.onNodeWithText("Habit").performClick()
            compose.onAllNodesWithText("Notes").assertCountEquals(0)
            compose.onNodeWithText("How do you want to track it?").assertIsDisplayed()
            compose.onAllNodesWithText("Intent").assertCountEquals(0)
            compose.onAllNodesWithText("Target rule").assertCountEquals(0)
            compose.onNodeWithTag("habit-editor-fields")
                .performScrollToNode(hasText("Show advanced options"))
            compose.onNodeWithText("Show advanced options").performClick()
            compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("Notes"))
            compose.onNodeWithText("Notes").assertIsDisplayed()
            compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("Intent"))
            compose.onNodeWithText("Intent").assertIsDisplayed()
            compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("Target rule"))
            compose.onNodeWithText("Target rule").assertIsDisplayed()
            compose.onNodeWithText("Cancel").performClick()

            compose.onNodeWithContentDescription("Home tab").performClick()
            compose.onNodeWithContentDescription("Add task, habit, goal, exercise, workout, or measurement").performClick()
            compose.onNodeWithText("Goal").performClick()
            compose.onAllNodesWithText("Description").assertCountEquals(0)
            compose.onNodeWithText("Starting value (optional)").assertIsDisplayed()
            compose.onNodeWithText("Move from a starting value", substring = true).assertIsDisplayed()
            compose.onNodeWithTag("goal-editor-fields")
                .performScrollToNode(hasText("Show advanced options"))
            compose.onNodeWithText("Show advanced options").performClick()
            compose.onNodeWithTag("goal-editor-fields").performScrollToNode(hasText("Description"))
            compose.onNodeWithText("Description").assertIsDisplayed()
            compose.onNodeWithText("Cancel").performClick()

            compose.onNodeWithContentDescription("Goals tab").performClick()
            compose.onNodeWithText("Templates").performScrollTo().performClick()
            compose.onNodeWithTag("goal-template-list").performScrollToNode(hasText("Build savings", substring = true))
            compose.onNodeWithText("Build savings", substring = true).assertIsDisplayed().performClick()
            compose.onNodeWithText("Savings").assertIsDisplayed()
        }
    }
}
