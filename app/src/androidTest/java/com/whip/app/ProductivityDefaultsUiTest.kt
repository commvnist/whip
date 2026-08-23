package com.whip.app

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.input.key.Key
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.HomeSection
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
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(currentDate = today, loading = false),
                    onRequestNotificationPermission = { permissionRequests.incrementAndGet() },
                    onSaveTask = { _, draft, _ -> saved.set(draft) },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Add task, habit, goal, exercise, workout, or measurement").performClick()
        compose.onNodeWithText("Task").performClick()
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

        compose.onNodeWithContentDescription("Add task, habit, goal, exercise, workout, or measurement").performClick()
        compose.onNodeWithText("Habit").performClick()
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
    fun optionalOnboardingChoicesAreProgressiveAndSwitchesHaveAccessibleLabels() {
        val completedNotifications = AtomicReference<Boolean?>()
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                FirstRunSetupDialog(
                    onComplete = { _: Set<HomeSection>, _, _, _, _, notifications -> completedNotifications.set(notifications) },
                    onSkip = {},
                )
            }
        }

        compose.onAllNodesWithText("Portable backup privacy").assertCountEquals(0)
        compose.onNodeWithText("More preferences").performClick()
        compose.onNodeWithText("Portable backup privacy").assertIsDisplayed()
        compose.onNodeWithContentDescription("I want reminder notifications").assertHasClickAction().performClick()
        compose.onNodeWithText("Finish setup").performClick()
        compose.runOnIdle { assertEquals(true, completedNotifications.get()) }
    }

    @Test
    fun controlNFromSettingsOpensTheGlobalAddSurfaceInsteadOfDoingNothing() {
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

        compose.onNodeWithContentDescription("Open settings").performClick()
        compose.onRoot().performKeyInput {
            keyDown(Key.CtrlLeft)
            keyDown(Key.N)
            keyUp(Key.N)
            keyUp(Key.CtrlLeft)
        }
        compose.onNodeWithText("Task").assertIsDisplayed()
        compose.onNodeWithText("Habit").assertIsDisplayed()
        compose.onAllNodesWithText("Measurement").assertCountEquals(1)
    }
}
