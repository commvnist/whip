package com.whip.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.RecurrenceRule
import com.whip.app.domain.RecurrenceUnit
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.WhipTask
import com.whip.app.domain.TaskOccurrence
import com.whip.app.domain.OccurrenceState
import com.whip.app.ui.PermanentTaskDeleteDialog
import com.whip.app.ui.PermanentDeleteDialog
import com.whip.app.ui.CompletedTaskDialog
import com.whip.app.ui.TaskActionsDialog
import com.whip.app.ui.TaskRow
import com.whip.app.data.TaskDeletionImpact
import com.whip.app.ui.theme.WhipTheme
import java.util.concurrent.atomic.AtomicInteger
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskDeletionUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun taskCardSeparatesDetailsFromDirectEditing() {
        val details = AtomicInteger()
        val edits = AtomicInteger()
        val item = ScheduledTask(
            task = WhipTask(
                id = 1,
                title = "Editable task",
                notes = "",
                scheduleKind = ScheduleKind.Anytime,
                date = null,
                recurrence = null,
                timeMinutes = null,
                reminderEnabled = false,
                archived = false,
                completedAtMillis = null,
                createdAtMillis = 1,
                updatedAtMillis = 1,
            ),
            originalDate = null,
            scheduledDate = null,
        )
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                TaskRow(
                    item = item,
                    completed = false,
                    onComplete = {},
                    onOpenActions = { details.incrementAndGet() },
                    onEdit = { edits.incrementAndGet() },
                )
            }
        }

        compose.onNodeWithContentDescription("Edit task Editable task").performClick()
        compose.runOnIdle {
            assertEquals(0, details.get())
            assertEquals(1, edits.get())
        }
        compose.onNodeWithContentDescription("Open task details for Editable task").performClick()
        compose.runOnIdle {
            assertEquals(1, details.get())
            assertEquals(1, edits.get())
        }
    }

    @Test
    fun inboxTaskScheduleSectionCanAssignItsFirstDateWithoutCrashing() {
        val reschedules = AtomicInteger()
        val item = ScheduledTask(
            task = WhipTask(
                id = 2,
                title = "Unscheduled errand",
                notes = "",
                scheduleKind = ScheduleKind.Anytime,
                date = null,
                recurrence = null,
                timeMinutes = null,
                reminderEnabled = false,
                archived = false,
                completedAtMillis = null,
                createdAtMillis = 1,
                updatedAtMillis = 1,
            ),
            originalDate = null,
            scheduledDate = null,
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TaskActionsDialog(
                    item = item,
                    onDismiss = {},
                    onComplete = {},
                    onEdit = {},
                    onReschedule = { reschedules.incrementAndGet() },
                    onSkip = {},
                    onArchive = {},
                    onDeletePermanently = {},
                    onPin = {},
                    onDuplicate = {},
                    onStartFocus = {},
                    onToggleSubtask = { _, _ -> },
                    onPromoteSubtask = {},
                    onReopenOccurrence = {},
                    onResetOccurrence = {},
                )
            }
        }

        compose.onNodeWithText("Activity").performClick()
        compose.onNodeWithText("This task is in Inbox without a scheduled date.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Choose a Date").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, reschedules.get()) }
    }

    @Test
    fun permanentDeleteRequiresExplicitSecondConfirmation() {
        val confirmations = AtomicInteger()
        val item = ScheduledTask(
            task = WhipTask(
                id = 42,
                title = "Private task",
                notes = "",
                scheduleKind = ScheduleKind.Anytime,
                date = null,
                recurrence = null,
                timeMinutes = null,
                reminderEnabled = false,
                archived = false,
                completedAtMillis = null,
                createdAtMillis = 1,
                updatedAtMillis = 1,
            ),
            originalDate = null,
            scheduledDate = null,
        )
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                var confirming by remember { mutableStateOf(false) }
                if (confirming) {
                    PermanentTaskDeleteDialog(
                        item = item,
                        impact = TaskDeletionImpact(
                            taskId = item.task.id,
                            exists = true,
                            title = item.task.title,
                            recordedOccurrenceCount = 0,
                            stepCount = 0,
                            linkRuleCount = 0,
                            automationRuleCount = 0,
                        ),
                        onDismiss = { confirming = false },
                        onConfirm = { confirmations.incrementAndGet() },
                    )
                } else {
                    TaskActionsDialog(
                        item = item,
                        onDismiss = {},
                        onComplete = {},
                        onEdit = {},
                        onReschedule = {},
                        onSkip = {},
                        onArchive = {},
                        onDeletePermanently = { confirming = true },
                        onPin = {},
                        onDuplicate = {},
                        onStartFocus = {},
                        onToggleSubtask = { _, _ -> },
                        onPromoteSubtask = {},
                        onReopenOccurrence = {},
                        onResetOccurrence = {},
                    )
                }
            }
        }

        compose.onNodeWithContentDescription("More Task options").performClick()
        compose.onNodeWithText("Options").performClick()
        compose.onNodeWithText("Delete Permanently").assertIsDisplayed().performClick()
        compose.onNodeWithText("Delete “Private task” Permanently?").assertIsDisplayed()
        compose.onNodeWithText("0 recorded occurrences", substring = true).assertIsDisplayed()
        compose.onNodeWithText("0 subtasks").assertIsDisplayed()
        compose.onNodeWithText("0 goal progress sources").assertIsDisplayed()
        compose.onNodeWithText("0 automations").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, confirmations.get()) }
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Private task").assertIsDisplayed()

        compose.onNodeWithContentDescription("More Task options").performClick()
        compose.onNodeWithText("Options").performClick()
        compose.onNodeWithText("Delete Permanently").performClick()
        compose.onNodeWithText("Delete Permanently").performClick()
        compose.runOnIdle { assertEquals(1, confirmations.get()) }
    }

    @Test
    fun completedRecurringTaskOffersOccurrenceLevelRecovery() {
        val reopens = AtomicInteger()
        val edits = AtomicInteger()
        val date = LocalDate.of(2026, 8, 18)
        val item = ScheduledTask(
            task = WhipTask(
                id = 7,
                title = "Water plants",
                notes = "",
                scheduleKind = ScheduleKind.Recurring,
                date = date,
                recurrence = RecurrenceRule(
                    unit = RecurrenceUnit.Days,
                    startDate = date,
                ),
                timeMinutes = null,
                reminderEnabled = false,
                archived = false,
                completedAtMillis = null,
                createdAtMillis = 1,
                updatedAtMillis = 1,
            ),
            originalDate = date,
            scheduledDate = date,
            completedAtMillis = 1,
        )
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                CompletedTaskDialog(
                    item = item,
                    onDismiss = {},
                    onEdit = { edits.incrementAndGet() },
                    onReopen = { reopens.incrementAndGet() },
                    onDeletePermanently = {},
                    onReopenOccurrence = {},
                    onResetOccurrence = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Edit This and Future").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, edits.get()) }
        compose.onNodeWithText("Reopen Occurrence").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, reopens.get()) }
    }

    @Test
    fun editRemainsVisibleInEveryTaskDetailsSectionIncludingArchivedTasks() {
        val edits = AtomicInteger()
        val date = LocalDate.of(2026, 8, 18)
        val item = ScheduledTask(
            task = WhipTask(
                id = 17,
                title = "Archived recurring task",
                notes = "",
                scheduleKind = ScheduleKind.Recurring,
                date = date,
                recurrence = RecurrenceRule(RecurrenceUnit.Days, startDate = date),
                timeMinutes = null,
                reminderEnabled = false,
                archived = true,
                completedAtMillis = null,
                createdAtMillis = 1,
                updatedAtMillis = 1,
            ),
            originalDate = date,
            scheduledDate = date,
        )
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                TaskActionsDialog(
                    item = item,
                    onDismiss = {},
                    onComplete = {},
                    onEdit = { edits.incrementAndGet() },
                    onReschedule = {},
                    onSkip = {},
                    onArchive = {},
                    onDeletePermanently = {},
                    onPin = {},
                    onDuplicate = {},
                    onStartFocus = {},
                    onToggleSubtask = { _, _ -> },
                    onPromoteSubtask = {},
                    onReopenOccurrence = {},
                    onResetOccurrence = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Edit This and Future").assertIsDisplayed()
        compose.onNodeWithText("Activity").performClick()
        compose.onNodeWithContentDescription("Edit This and Future").assertIsDisplayed()
        compose.onNodeWithContentDescription("More Task options").performClick()
        compose.onNodeWithText("Options").performClick()
        compose.onNodeWithContentDescription("Edit This and Future").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, edits.get()) }
    }

    @Test
    fun seriesHistoryShowsMovedAndSkippedOccurrencesAndAllowsUndoSkip() {
        val resets = AtomicInteger()
        val date = LocalDate.of(2026, 8, 18)
        val item = ScheduledTask(
            task = WhipTask(
                id = 8, title = "Practice", notes = "", scheduleKind = ScheduleKind.Recurring,
                date = date, recurrence = RecurrenceRule(RecurrenceUnit.Days, 2, startDate = date),
                timeMinutes = null, reminderEnabled = false, archived = false,
                completedAtMillis = null, createdAtMillis = 1, updatedAtMillis = 1,
            ),
            originalDate = date,
            scheduledDate = date,
        )
        val skipped = TaskOccurrence(8, date, date, OccurrenceState.Skipped, null)
        val moved = TaskOccurrence(8, date.plusDays(2), date.plusDays(3), OccurrenceState.Open, null)
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                TaskActionsDialog(
                    item = item, onDismiss = {}, onComplete = {}, onEdit = {}, onReschedule = {},
                    onSkip = {}, onArchive = {}, onDeletePermanently = {}, onPin = {},
                    onDuplicate = {}, onStartFocus = {},
                    onToggleSubtask = { _, _ -> }, onPromoteSubtask = {},
                    occurrenceHistory = listOf(moved, skipped),
                    onReopenOccurrence = {},
                    onResetOccurrence = { resets.incrementAndGet() },
                )
            }
        }

        compose.onNodeWithText("Activity").performClick()
        compose.onNodeWithText("Series History").assertIsDisplayed()
        compose.onNodeWithText("Moved to", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Skipped", substring = true).assertIsDisplayed()
        compose.onNodeWithText("The next date is", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Undo Skip").performClick()
        compose.runOnIdle { assertEquals(1, resets.get()) }
    }

    @Test
    fun genericPermanentDeleteDialogDoesNothingWhenCancelled() {
        val confirmations = AtomicInteger()
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                PermanentDeleteDialog(
                    title = "Delete habit permanently?",
                    impacts = listOf("12 check-ins will be removed", "2 links will be recalculated"),
                    onDismiss = {},
                    onConfirm = { confirmations.incrementAndGet() },
                )
            }
        }

        compose.onNodeWithText("12 check-ins will be removed", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(0, confirmations.get()) }
    }
}
