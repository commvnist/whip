package com.whip.app

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.input.key.Key
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.HomeSection
import com.whip.app.core.EntitySaveReceipt
import com.whip.app.core.PersistenceRequestState
import com.whip.app.core.WhipResult
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TargetPeriod
import com.whip.app.domain.TaskDraft
import com.whip.app.ui.FirstRunSetupDialog
import com.whip.app.ui.GoalUiState
import com.whip.app.ui.GoalViewModel
import com.whip.app.ui.HabitUiState
import com.whip.app.ui.HabitViewModel
import com.whip.app.ui.TaskUiState
import com.whip.app.ui.WhipScreen
import com.whip.app.ui.theme.WhipTheme
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductivityDefaultsUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val app: WhipApplication
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun reset() = runBlocking { app.backupRepository.deleteAllData() }

    @After
    fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test
    fun taskCreatedFromHomeDefaultsToTodayInsteadOfInbox() {
        val today = LocalDate.of(2026, 8, 19)
        val saved = AtomicReference<TaskDraft?>()
        val permissionRequests = AtomicInteger()
        var saveState by mutableStateOf<PersistenceRequestState<EntitySaveReceipt>>(PersistenceRequestState.Idle)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(currentDate = today, loading = false),
                    onRequestNotificationPermission = { permissionRequests.incrementAndGet() },
                    taskEditorSaveState = saveState,
                    onTaskEditorSaveResultConsumed = { saveState = PersistenceRequestState.Idle },
                    onSaveTask = { _, _, _ -> },
                    onSaveTaskRequest = { _, draft, _, requestId ->
                        saved.set(draft)
                        saveState = PersistenceRequestState.Finished(
                            requestId,
                            WhipResult.Success(EntitySaveReceipt(null, draft.areaId)),
                        )
                        true
                    },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Add task, habit, goal, track, or workout").performClick()
        compose.onNodeWithText("New Task").performClick()
        compose.onNodeWithTag("task-editor-title").performTextInput("Visible today")
        compose.onNodeWithText("Save").performClick()

        compose.runOnIdle {
            val draft = requireNotNull(saved.get())
            assertEquals(ScheduleKind.Once, draft.scheduleKind)
            assertEquals(today, draft.date)
            assertFalse(draft.inbox)
            assertEquals(0, permissionRequests.get())
        }
    }

    @Test
    fun todayAndInboxQuickCaptureUseMatchingDestinationCopyAndDefaults() {
        val today = LocalDate.of(2026, 8, 25)
        val quickCaptures = mutableListOf<Pair<String, LocalDate?>>()
        val detailedDraft = AtomicReference<TaskDraft?>()
        var saveState by mutableStateOf<PersistenceRequestState<EntitySaveReceipt>>(PersistenceRequestState.Idle)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(currentDate = today, loading = false),
                    taskEditorSaveState = saveState,
                    onTaskEditorSaveResultConsumed = { saveState = PersistenceRequestState.Idle },
                    onSaveTask = { _, _, _ -> },
                    onSaveTaskRequest = { _, draft, _, requestId ->
                        detailedDraft.set(draft)
                        saveState = PersistenceRequestState.Finished(
                            requestId,
                            WhipResult.Success(EntitySaveReceipt(null, draft.areaId)),
                        )
                        true
                    },
                    onQuickAddTaskWithResult = { capture, date, _, onFinished ->
                        quickCaptures += capture to date
                        onFinished(true)
                    },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Tasks tab").performClick()
        compose.onNodeWithText("Quick Capture to Today").assertIsDisplayed()
        compose.onNodeWithTag("task-quick-capture").performTextInput("Captured from Today")
        compose.onNodeWithContentDescription("Add task now").performClick()

        compose.onNodeWithTag("task-destination-Inbox").performClick()
        compose.onNodeWithText("Quick Capture to Inbox").assertIsDisplayed()
        compose.onNodeWithTag("task-quick-capture").performTextInput("Captured from Inbox")
        compose.onNodeWithContentDescription("Add task now").performClick()

        compose.onNodeWithTag("task-destination-Today").performClick()
        compose.onNodeWithTag("task-quick-capture").performTextInput("Detailed from Today")
        compose.onNodeWithText("Add Details").performClick()
        compose.onNodeWithText("Save").performClick()

        compose.runOnIdle {
            assertEquals(
                listOf(
                    "Captured from Today" to today,
                    "Captured from Inbox" to null,
                ),
                quickCaptures,
            )
            val draft = requireNotNull(detailedDraft.get())
            assertEquals("Detailed from Today", draft.title)
            assertEquals(ScheduleKind.Once, draft.scheduleKind)
            assertEquals(today, draft.date)
            assertFalse(draft.inbox)
        }
    }

    @Test
    fun savingGoalWithoutAReminderDoesNotRequestNotificationPermission() {
        val permissionRequests = AtomicInteger()
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                val goalViewModel: GoalViewModel = viewModel()
                WhipScreen(
                    state = TaskUiState(loading = false),
                    goalState = GoalUiState(loading = false),
                    goalViewModel = goalViewModel,
                    onRequestNotificationPermission = { permissionRequests.incrementAndGet() },
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Goals tab").performClick()
        compose.onNodeWithContentDescription("Add goal").performClick()
        compose.onNodeWithTag("goal-editor-name").performTextInput("Quiet goal")
        compose.onNodeWithTag("goal-editor-fields").performScrollToNode(hasTestTag("goal-editor-target"))
        compose.onNodeWithTag("goal-editor-target").performTextInput("10")
        compose.onNodeWithText("Save").performClick()

        runBlocking {
            withTimeout(5_000) {
                app.goalRepository.goals.first { goals -> goals.any { it.name == "Quiet goal" } }
            }
        }
        assertEquals(0, permissionRequests.get())
    }

    @Test
    fun goalEditorMarksRequiredTargetAndExplainsRejectedSave() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                val goalViewModel: GoalViewModel = viewModel()
                WhipScreen(
                    state = TaskUiState(loading = false),
                    goalState = GoalUiState(loading = false),
                    goalViewModel = goalViewModel,
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Goals tab").performClick()
        compose.onNodeWithContentDescription("Add goal").performClick()
        compose.onNodeWithTag("goal-editor-name").performTextInput("Needs a target")
        compose.onNodeWithText("Save").performClick()

        compose.onNodeWithTag("goal-save-problem")
            .assertIsDisplayed()
            .assertContentDescriptionContains("Enter a target", substring = true)
        compose.onNodeWithText("Target *").assertIsDisplayed()
        assertTrue(runBlocking { app.goalRepository.goals.first().none { it.name == "Needs a target" } })

        compose.onNodeWithTag("goal-editor-target").performTextInput("10")
        compose.onNodeWithText("Save").performClick()
        runBlocking {
            withTimeout(5_000) {
                app.goalRepository.goals.first { goals -> goals.any { it.name == "Needs a target" } }
            }
        }
    }

    @Test
    fun untouchedDoneHabitUsesDiscreteOccurrenceDefaultsWithoutRequestingNotifications() {
        val permissionRequests = AtomicInteger()
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                val habitViewModel: HabitViewModel = viewModel()
                WhipScreen(
                    state = TaskUiState(loading = false),
                    habitState = HabitUiState(loading = false),
                    habitViewModel = habitViewModel,
                    onRequestNotificationPermission = { permissionRequests.incrementAndGet() },
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Add task, habit, goal, track, or workout").performClick()
        compose.onNodeWithText("New Habit").performClick()
        compose.onNodeWithTag("habit-editor-name").performTextInput("Discrete default")
        compose.onNodeWithText("Save").performClick()

        val habit = runBlocking {
            withTimeout(5_000) {
                app.habitRepository.habits.first { habits -> habits.any { it.name == "Discrete default" } }
                    .first { it.name == "Discrete default" }
            }
        }
        assertEquals(HabitTrackingMode.CheckOff, habit.trackingMode)
        assertEquals(0, habit.precision)
        assertEquals(TargetPeriod.Occurrence, habit.targetPeriod)
        assertEquals(0, permissionRequests.get())
    }

    @Test
    fun onboardingStartsWithValueAndKeepsPreferencesOptional() {
        val completedNotifications = AtomicReference<Boolean?>()
        val defaultsUsed = AtomicInteger()
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                FirstRunSetupDialog(
                    onComplete = { _: Set<HomeSection>, _, _, _, notifications -> completedNotifications.set(notifications) },
                    onUseDefaults = { defaultsUsed.incrementAndGet() },
                )
            }
        }

        compose.onNodeWithText("Welcome to Whip").assertIsDisplayed()
        compose.onNodeWithText("Use Recommended").assertIsDisplayed()
        compose.onNodeWithText("Customize").assertIsDisplayed()
        compose.onAllNodesWithText("Decide Later").assertCountEquals(0)
        compose.onAllNodesWithText("Create Encrypted Backup Now").assertCountEquals(0)
        compose.onNodeWithText("Your data stays on this device", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Use Recommended").performClick()
        compose.runOnIdle { assertEquals(1, defaultsUsed.get()) }
        compose.onNodeWithText("Customize").performClick()
        compose.onNodeWithText("Customize Whip").assertIsDisplayed()
        compose.onNodeWithText("Optional Preferences").performClick()
        compose.onAllNodesWithText("Portable Backup Privacy").assertCountEquals(0)
        compose.onNodeWithContentDescription("Ask for reminder notifications").assertHasClickAction().performClick()
        compose.onNodeWithText("Save and Start").performClick()
        compose.runOnIdle { assertEquals(true, completedNotifications.get()) }
    }

    @Test
    fun onboardingKeepsItsActionsReachableAtLargeText() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(compose.density.density, fontScale = 2f)) {
                WhipTheme(dynamicColor = false) {
                    FirstRunSetupDialog(onComplete = { _, _, _, _, _ -> }, onUseDefaults = {})
                }
            }
        }

        compose.onNodeWithText("Welcome to Whip").assertIsDisplayed()
        compose.onNodeWithText("Customize").assertIsDisplayed()
        compose.onNodeWithText("Use Recommended").assertIsDisplayed()
        compose.onNodeWithText("Your data stays on this device", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun controlNFromSettingsLeavesSettingsInPlaceAndDoesNotOpenGlobalAdd() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(loading = false),
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        compose.onNodeWithTag("workspace-settings-action").performClick()
        compose.onRoot().performKeyInput {
            keyDown(Key.CtrlLeft)
            keyDown(Key.N)
            keyUp(Key.N)
            keyUp(Key.CtrlLeft)
        }
        compose.onNodeWithText("Settings").assertIsDisplayed()
        compose.onAllNodesWithTag("workspace-add-action").assertCountEquals(0)
    }
}
