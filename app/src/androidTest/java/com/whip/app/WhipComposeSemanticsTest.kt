package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.ElapsedDisplayUnit
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalType
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.WorkoutSetDraft
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
        launchMainActivity(intent).use {
            compose.waitForIdle()
            compose.onNodeWithContentDescription("Home").assertIsDisplayed()
            compose.onNodeWithContentDescription("Add task, habit, goal, track, exercise, or workout").performClick()
            compose.onNodeWithText("New Habit").assertIsDisplayed().performClick()
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
            compose.onNodeWithContentDescription("Back to Settings").performClick()
            compose.onNodeWithTag("settings-section-Planning & Units").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Create reusable units", substring = true))
            compose.onNodeWithText("Create reusable units", substring = true).assertIsDisplayed()
            compose.onNodeWithContentDescription("Back to Settings").performClick()
            compose.onNodeWithTag("settings-section-Organization").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Manage Areas"))
            compose.onNodeWithText("Manage Areas").assertIsDisplayed()
            compose.onNodeWithContentDescription("Back to Settings").performClick()
            compose.onNodeWithTag("settings-section-Reminders").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Delivery Details"))
            compose.onNodeWithTag("notification-diagnostics").assertIsDisplayed()
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Send Test Notification"))
            compose.onNodeWithText("Send Test Notification").assertIsDisplayed()
            compose.onNodeWithContentDescription("Back to Settings").performClick()
            compose.onNodeWithTag("settings-section-Data & Privacy").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Portable Backup Folder"))
            compose.onNodeWithText("Portable Backup Folder").assertIsDisplayed()
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Choose Backup Folder"))
            compose.onNodeWithText("Choose Backup Folder").assertIsDisplayed()
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Reset Whip and Delete All Data"))
            compose.onAllNodesWithText("FitNotes", substring = true).assertCountEquals(0)
            compose.onNodeWithContentDescription("Back to Settings").performClick()
            compose.onNodeWithTag("settings-section-About Whip").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("About Whip"))
            compose.onNodeWithTag("about-build-identity").assertIsDisplayed()
        }
    }

    @Test
    fun medicationTemplateStartsAsAThreeItemChecklist() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)

        launchMainActivity(intent).use {
            compose.waitForIdle()
            compose.onNodeWithContentDescription("Habits tab").performClick()
            compose.onNodeWithText("Browse Templates").performClick()
            compose.onNodeWithText("Medication").performClick()

            compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("Medication 3"))
            compose.onNodeWithText("Medication 1").assertIsDisplayed()
            compose.onNodeWithText("Medication 2").assertIsDisplayed()
            compose.onNodeWithText("Medication 3").assertIsDisplayed()
            compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("Complete Habit With Final Item"))
            compose.onNodeWithTag("habit-auto-complete-from-items").assertIsDisplayed()
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
    fun elapsedGoalResetWorksFromHomeOnTheFirstTap() {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        val originalStart = Instant.now().minusSeconds(12 * 60 * 60)
        val goalId = runBlocking {
            app.goalRepository.create(
                GoalDraft(
                    name = "Recovery Counter",
                    type = GoalType.ElapsedSince,
                    startDate = app.clock.today(),
                    elapsedStartMillis = originalStart.toEpochMilli(),
                    elapsedDisplayUnit = ElapsedDisplayUnit.Hours,
                ),
            )
        }
        val intent = Intent(app, MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        launchMainActivity(intent).use {
            compose.onNodeWithText("Reset").performScrollTo().performClick()
            compose.onNodeWithText("Reset Recovery Counter?").assertIsDisplayed()
            compose.onNodeWithText("Reset to Now").performClick()

            runBlocking {
                withTimeout(5_000) {
                    app.goalRepository.goals.first { goals ->
                        goals.firstOrNull { it.id == goalId }
                            ?.elapsedStartMillis
                            ?.let { it > originalStart.toEpochMilli() }
                            ?: false
                    }
                }
            }
        }
    }

    @Test
    fun homeCardOpenAndPrimaryCallbacksWorkOnFirstInteractionAfterLaunch() {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        val today = app.clock.today()
        val (taskId, habitId, goalId, trackId) = runBlocking {
            val taskId = app.taskRepository.create(
                TaskDraft(
                    title = "Home Callback Task",
                    scheduleKind = ScheduleKind.Once,
                    date = today,
                    inbox = false,
                ),
            )
            val habitId = app.habitRepository.create(HabitDraft(name = "Home Callback Habit", startDate = today))
            val goalId = app.goalRepository.create(
                GoalDraft(
                    name = "Home Callback Goal",
                    type = GoalType.ReachValue,
                    targetMin = 10.0,
                    startDate = today,
                ),
            )
            val trackId = app.trackRepository.create(
                TrackDraft(
                    name = "Home Callback Track",
                    fields = listOf(
                        TrackFieldDraft("Title", TrackFieldType.ShortText, required = true, primary = true),
                    ),
                ),
            )
            app.trackRepository.setPinned(trackId, true)
            listOf(taskId, habitId, goalId, trackId)
        }
        val intent = Intent(app, MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        fun launchHome(interaction: () -> Unit) {
            launchMainActivity(intent).use {
                compose.waitUntil(5_000) {
                    compose.onAllNodesWithTag("home-list").fetchSemanticsNodes().isNotEmpty()
                }
                interaction()
            }
        }

        launchHome {
            compose.onNodeWithTag("home-list").performScrollToNode(hasContentDescription("Open task details for Home Callback Task"))
            compose.onNodeWithContentDescription("Open task details for Home Callback Task").performClick()
            compose.onNodeWithTag("task-actions-surface").assertIsDisplayed()
        }
        launchHome {
            compose.onNodeWithTag("home-list").performScrollToNode(hasContentDescription("Complete task Home Callback Task"))
            compose.onNodeWithContentDescription("Complete task Home Callback Task").performClick()
            runBlocking {
                withTimeout(5_000) {
                    app.taskRepository.tasks.first { tasks -> tasks.firstOrNull { it.id == taskId }?.completedAtMillis != null }
                }
            }
        }

        launchHome {
            compose.onNodeWithTag("home-list").performScrollToNode(hasTestTag("habit-card-$habitId"))
            compose.onNodeWithTag("habit-card-$habitId").performClick()
            compose.onNodeWithTag("habit-detail-surface").assertIsDisplayed()
        }
        launchHome {
            compose.onNodeWithTag("home-list").performScrollToNode(hasContentDescription("Check off habit Home Callback Habit"))
            compose.onNodeWithContentDescription("Check off habit Home Callback Habit").performClick()
            runBlocking {
                withTimeout(5_000) {
                    app.habitRepository.logs.first { logs -> logs.any { it.habitId == habitId } }
                }
            }
        }

        launchHome {
            compose.onNodeWithTag("home-list").performScrollToNode(hasTestTag("goal-card-$goalId"))
            compose.onNodeWithTag("goal-card-$goalId").performClick()
            compose.onNodeWithTag("goal-detail-surface").assertIsDisplayed()
        }
        launchHome {
            compose.onNodeWithTag("home-list").performScrollToNode(hasTestTag("goal-card-$goalId"))
            compose.onNode(
                hasText("Log") and hasAnyAncestor(hasTestTag("goal-primary-action-$goalId")),
                useUnmergedTree = true,
            ).performClick()
            compose.onNodeWithText("Record Home Callback Goal").assertIsDisplayed()
        }

        launchHome {
            compose.onNodeWithTag("home-list").performScrollToNode(hasTestTag("track-card-$trackId"))
            compose.onNodeWithTag("track-card-$trackId").performClick()
            compose.onNodeWithTag("track-destination-Entries").assertIsDisplayed()
        }
        launchHome {
            compose.onNodeWithTag("home-list").performScrollToNode(
                hasText("Add Title") and hasAnyAncestor(hasTestTag("track-card-$trackId")),
            )
            compose.onNode(
                hasText("Add Title") and hasAnyAncestor(hasTestTag("track-card-$trackId")),
                useUnmergedTree = true,
            ).performClick()
            compose.onNodeWithContentDescription("Close Entry Editor").assertIsDisplayed()
        }
    }

    @Test
    fun directTaskAndHabitCompletionUseInlineFeedbackWithoutSnackbars() {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        val today = app.clock.today()
        val (taskId, habitId) = runBlocking {
            val taskId = app.taskRepository.create(
                TaskDraft(
                    title = "Quiet completion task",
                    scheduleKind = ScheduleKind.Once,
                    date = today,
                    inbox = false,
                ),
            )
            val habitId = app.habitRepository.create(
                HabitDraft(name = "Quiet completion habit", startDate = today),
            )
            taskId to habitId
        }
        val intent = Intent(app, MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)

        launchMainActivity(intent).use {
            compose.onNodeWithContentDescription("Tasks tab").performClick()
            compose.onNodeWithContentDescription("Complete task Quiet completion task").performClick()
            runBlocking {
                withTimeout(5_000) {
                    app.taskRepository.tasks.first { tasks ->
                        tasks.firstOrNull { it.id == taskId }?.completedAtMillis != null
                    }
                }
            }
            compose.onAllNodesWithText("Task completed").assertCountEquals(0)

            compose.onNodeWithContentDescription("Habits tab").performClick()
            compose.onNodeWithContentDescription("Check off habit Quiet completion habit").performClick()
            runBlocking {
                withTimeout(5_000) {
                    app.habitRepository.logs.first { logs -> logs.any { it.habitId == habitId } }
                }
            }
            compose.onAllNodesWithText("Habit completed").assertCountEquals(0)
            compose.onNodeWithTag("habit-done-disclosure").assertIsDisplayed()
        }
    }

    @Test
    fun machineLibraryExplainsMachineScopedTracking() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        launchMainActivity(intent).use {
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
        val taskId = runBlocking {
            val app = ApplicationProvider.getApplicationContext<WhipApplication>()
            app.taskRepository.create(
                TaskDraft(
                    title = "Toolbar test task",
                    icon = "🧹",
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
        launchMainActivity(intent).use {
            compose.waitForIdle()
            compose.onNodeWithContentDescription("Tasks tab").performClick()
            compose.onNodeWithTag("task-icon-$taskId", useUnmergedTree = true)
                .performScrollTo()
                .assertIsDisplayed()
            compose.onNodeWithContentDescription("Filter & Sort Tasks").performClick()
            compose.onNodeWithText("Sort, Group & Filter Tasks").assertIsDisplayed()
            compose.onNodeWithText("Done").performClick()
            if (compose.onAllNodesWithContentDescription("Search Tasks").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithContentDescription("Search Tasks").performClick()
            } else {
                compose.onNodeWithContentDescription("App actions").performClick()
                compose.onNodeWithTag("workspace-search-menu-action").performClick()
            }
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
            app.gymRepository.addSet(placementId, WorkoutSetDraft(weight = 42.5, reps = 9, completed = true))
            app.gymRepository.addSet(placementId, WorkoutSetDraft(weight = 30.0, reps = 20, completed = true))
            app.gymRepository.addSet(placementId, WorkoutSetDraft(weight = 40.0, reps = 10, completed = true))
            app.gymRepository.finishWorkout(workoutId)
            app.routineRepository.rebuildPersonalRecords(exerciseId)
        }
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        launchMainActivity(intent).use {
            compose.waitForIdle()
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithTag("gym-destination-History")
                .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            compose.onNodeWithText("History Options").assertIsDisplayed()
            compose.onAllNodesWithText("Calendar View").assertCountEquals(0)
            compose.onNodeWithText("History Options").performClick()
            compose.onNodeWithText("Calendar View").assertIsDisplayed()
            compose.onNodeWithTag("history-exercise-filter").assertIsDisplayed()
            compose.onAllNodesWithText("Search Exercise Filters").assertCountEquals(0)

            compose.onNodeWithTag("gym-destination-Progress")
                .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("gym-e1rm-formula").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("gym-e1rm-formula").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Formula: Epley", substring = true).assertIsDisplayed()
            compose.onNodeWithTag("gym-progress-list").performScrollToNode(hasText("Graph Options"))
            compose.onNodeWithText("Graph Options").assertIsDisplayed()
            compose.onAllNodesWithText("Range").assertCountEquals(0)
            compose.onNodeWithText("Graph Options").performClick()
            compose.onNodeWithText("Range").assertIsDisplayed()
            compose.onNodeWithTag("gym-progress-list").performScrollToNode(hasText("Data Points"))
            compose.onNodeWithText("Data Points").assertIsDisplayed()
            compose.onAllNodesWithText("Data Table", substring = true).assertCountEquals(0)
            compose.onAllNodesWithText("Graph Presets").assertCountEquals(0)
            compose.onNodeWithTag("gym-progress-list").performScrollToNode(hasText("Tracked Records"))
            compose.onNodeWithTag("gym-tracked-records").assertIsDisplayed()
            compose.onNodeWithText("No Tracked Records Yet").assertIsDisplayed()
            compose.onNodeWithTag("gym-manage-tracked-records").performClick()
            compose.onNodeWithText("Choose Exercise").performClick()
            compose.onAllNodesWithText("Graph controls test")[1].performClick()
            compose.onNodeWithText("2 records").assertIsDisplayed()
            compose.onAllNodesWithText("Specific targets").assertCountEquals(0)
            compose.onAllNodesWithText("Target repetitions").assertCountEquals(0)
            compose.onAllNodesWithText("Minimum weight", substring = true).assertCountEquals(0)
            compose.onNodeWithTag("tracked-records-save").performClick()
            compose.onAllNodesWithText("Estimated 1RM").assertCountEquals(2)
            compose.onNodeWithText("Epley formula", substring = true).assertIsDisplayed()
            compose.onNodeWithText("Heaviest Weight").performScrollTo().assertIsDisplayed()
            compose.onAllNodesWithText("Heaviest Weight for a Rep Count").assertCountEquals(0)
            compose.onAllNodesWithText("9-Rep Best").assertCountEquals(0)
            compose.onAllNodesWithText("10-Rep Best").assertCountEquals(0)
            compose.onAllNodesWithText("20-Rep Best").assertCountEquals(0)
            compose.onAllNodesWithText("EstimatedOneRepMax").assertCountEquals(0)
            compose.onNodeWithTag("gym-progress-list").performScrollToNode(hasText("Chart Points"))
            compose.onAllNodesWithTag("gym-chart-point")[0].performClick()
            compose.onNodeWithTag("gym-chart-point-open-workout").performClick()
            compose.onNodeWithText("Workout History").assertIsDisplayed()
            compose.onNodeWithText("Showing the workout opened from search.").assertIsDisplayed()
        }
    }

    @Test
    fun gymDestinationSurvivesExpandingAndRestoringTheFoldPane() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        launchMainActivity(intent).use {
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
            if (compose.onAllNodesWithTag("expand-content-pane-action").fetchSemanticsNodes().isEmpty()) {
                return@use
            }
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
        launchMainActivity(intent).use {
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
        launchMainActivity(intent).use {
            compose.waitForIdle()

            compose.onNodeWithContentDescription("Add task, habit, goal, track, exercise, or workout").performClick()
            compose.onNodeWithText("New Task").performClick()
            compose.onAllNodesWithText("Notes (Optional)").assertCountEquals(0)
            compose.onNodeWithTag("task-editor-more-details").performScrollTo().performClick()
            compose.onNodeWithText("Planning").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Priority").performScrollTo().assertIsDisplayed()
            compose.onAllNodesWithText("Location cue").assertCountEquals(0)
            compose.onNodeWithContentDescription("Cancel Task editing").performClick()

            compose.onNodeWithContentDescription("Home").performClick()
            compose.onNodeWithContentDescription("Add task, habit, goal, track, exercise, or workout").performClick()
            compose.onNodeWithText("New Habit").performClick()
            compose.onAllNodesWithText("Notes").assertCountEquals(0)
            compose.onNodeWithText("How do you want to track it?").assertIsDisplayed()
            compose.onAllNodesWithText("Intent").assertCountEquals(0)
            compose.onAllNodesWithText("Target rule").assertCountEquals(0)
            compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("More Details"))
            compose.onNodeWithText("More Details").assertIsDisplayed().performClick()
            compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("Notes"))
            compose.onNodeWithText("Notes").assertIsDisplayed()
            compose.onAllNodesWithText("Intent").assertCountEquals(0)
            compose.onNodeWithTag("habit-editor-fields").performScrollToNode(
                hasText("One check completes each scheduled occurrence. No numeric value is required."),
            )
            compose.onNodeWithText("One check completes each scheduled occurrence. No numeric value is required.").assertIsDisplayed()
            compose.onAllNodesWithText("Target rule").assertCountEquals(0)
            compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("Checklist"))
            compose.onNodeWithText("Checklist").performClick()
            compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("Complete Habit With Final Item"))
            compose.onNodeWithText("Complete Habit With Final Item").assertIsDisplayed()
            compose.onNodeWithTag("habit-auto-complete-from-items").assertIsDisplayed().performClick()
            compose.onNodeWithTag("habit-editor-fields").performScrollToNode(
                hasText("Checklist items stay independent.", substring = true),
            )
            compose.onNodeWithText("Checklist items stay independent.", substring = true).assertIsDisplayed()
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
            compose.onNodeWithText("New Goal").performClick()
            compose.onAllNodesWithText("Description").assertCountEquals(0)
            compose.onNodeWithTag("goal-editor-fields").performScrollToNode(hasText("Starting Value (Optional)"))
            compose.onNodeWithText("Starting Value (Optional)").assertIsDisplayed()
            compose.onNodeWithText("Move from a starting value", substring = true).assertIsDisplayed()
            compose.onNodeWithTag("goal-editor-fields").performScrollToNode(hasText("More Details"))
            compose.onNodeWithText("More Details").assertIsDisplayed().performClick()
            compose.onNodeWithTag("goal-editor-fields").performScrollToNode(hasText("Description"))
            compose.onNodeWithText("Description").assertIsDisplayed()
            compose.onNodeWithContentDescription("Cancel Goal editing").performClick()

            compose.onNodeWithContentDescription("Goals tab").performClick()
            compose.onAllNodesWithText("Log Goal Value").assertCountEquals(0)
            compose.onNodeWithText("Browse Templates").performClick()
            compose.onNodeWithTag("goal-template-list").performScrollToNode(hasText("Build Savings", substring = true))
            compose.onNodeWithText("Build Savings", substring = true).assertIsDisplayed().performClick()
            compose.onNodeWithText("Savings").assertIsDisplayed()
        }
    }
}
