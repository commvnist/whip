package com.whip.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.ScheduledSubtask
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.TaskStep
import com.whip.app.domain.WhipTask
import com.whip.app.ui.TaskRow
import com.whip.app.ui.TaskFocusDialog
import com.whip.app.ui.theme.WhipTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskInlineExecutionUiTest {
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)

    @Test
    @AndroidFontScale
    fun collapsedTaskExecutesSubtasksFocusCompletionAndReopenWithoutOpeningDetails() {
        val date = LocalDate.of(2026, 9, 29)
        val task = WhipTask(901, "Read audit", "", ScheduleKind.Once, date, null, null,
            false, false, null, 1, 1)
        val step = TaskStep(902, task.id, "Read named step", 0, "", false, 1, 1)
        val stepCompleted = mutableStateOf(false)
        val completed = mutableStateOf(false)
        val focusBusy = mutableStateOf(false)
        val selectionMode = mutableStateOf(false)
        var details = 0
        var focusStarts = 0
        var completionChanges = 0
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                    TaskRow(
                        item = ScheduledTask(task, date, date,
                            subtasks = listOf(ScheduledSubtask(step, stepCompleted.value, null, step.title))),
                        completed = completed.value,
                        onComplete = { completed.value = !completed.value; completionChanges++ },
                        onOpenActions = { details++ },
                        selectionMode = selectionMode.value,
                        onToggleSubtask = { id, checked ->
                            assertEquals(step.id, id)
                            stepCompleted.value = checked
                        },
                        onStartFocus = { focusStarts++ },
                        focusBusy = focusBusy.value,
                        executionLabel = "Today · Sep 29",
                    )
                }
            }
        }
        compose.onNodeWithText("Today · Sep 29").assertIsDisplayed()
        compose.onNodeWithContentDescription("Complete Subtask Read named step").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Mark Subtask Read named step incomplete").performClick()
        compose.runOnIdle { assertEquals(false, stepCompleted.value) }
        compose.onNodeWithTag("task-inline-focus-901").performScrollTo().performClick()
        compose.runOnIdle { focusBusy.value = true }
        compose.onNodeWithTag("task-inline-focus-901").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Complete task Read audit").performScrollTo().performClick()
        compose.onNodeWithTag("task-inline-focus-901").assertDoesNotExist()
        compose.onNodeWithTag("task-inline-subtask-901-902", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("Reopen task Read audit").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("task-inline-subtask-901-902", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.runOnIdle { selectionMode.value = true }
        compose.onNodeWithTag("task-inline-focus-901").assertDoesNotExist()
        compose.onNodeWithTag("task-inline-subtask-901-902", useUnmergedTree = true).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(0, details)
            assertEquals(1, focusStarts)
            assertEquals(2, completionChanges)
        }
    }

    @Test
    fun inlineFocusPreservesCustomValidationAndRequiresActiveTimerReplacement() {
        val date = LocalDate.of(2026, 9, 29)
        val task = WhipTask(901, "Read audit", "", ScheduleKind.Once, date, null, null,
            false, false, null, 1, 1)
        val starts = mutableListOf<Int>()
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TaskFocusDialog(
                    item = ScheduledTask(task, date, date),
                    onDismiss = {},
                    onStartFocus = { starts += it },
                    activeFocusTaskTitle = "Existing session",
                    activeFocusDeadlineMillis = System.currentTimeMillis() + 600_000,
                )
            }
        }
        compose.onNodeWithTag("focus-preset-15").performClick()
        compose.onNodeWithText("Replace Focus Timer?").assertIsDisplayed()
        compose.runOnIdle { assertEquals(emptyList<Int>(), starts) }
        compose.onNodeWithText("Keep Timer").performClick()
        compose.onNodeWithTag("focus-custom-time").performClick()
        compose.onNodeWithTag("focus-custom-minutes").performTextReplacement("241")
        compose.onNodeWithTag("focus-custom-start").assertIsNotEnabled()
        compose.onNodeWithTag("focus-custom-minutes").performTextReplacement("37")
        closeSoftKeyboard()
        compose.onNodeWithTag("focus-custom-start").performClick()
        compose.onNodeWithTag("focus-replace").performClick()
        compose.runOnIdle { assertEquals(listOf(37), starts) }
    }
}
