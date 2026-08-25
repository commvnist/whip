package com.whip.app

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.whip.app.data.TaskDeletionBatchImpact
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.WhipTask
import com.whip.app.ui.TaskUiState
import com.whip.app.ui.WhipScreen
import com.whip.app.ui.theme.WhipTheme
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TaskBulkSelectionUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun activeSelectionKeepsEveryBulkActionVisibleAndDeleteRequiresImpactConfirmation() {
        val today = LocalDate.of(2026, 8, 25)
        val item = scheduledTask(1, "Visible bulk actions", today)
        val archived = AtomicReference<List<ScheduledTask>>(emptyList())
        val deletionImpact = mutableStateOf<TaskDeletionBatchImpact?>(null)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                    WhipScreen(
                        state = TaskUiState(today = listOf(item), currentDate = today, loading = false),
                        onSaveTask = { _, _, _ -> },
                        onComplete = {},
                        onSkip = {},
                        onReschedule = { _, _ -> },
                        onArchive = {},
                        onReopen = {},
                        onBulkArchiveTasks = archived::set,
                        taskDeletionBatchImpact = deletionImpact.value,
                        onPreviewBulkTaskDeletion = { ids ->
                            deletionImpact.value = TaskDeletionBatchImpact(
                                requestedTaskIds = ids,
                                taskIds = ids,
                                titles = listOf(item.task.title),
                                revisionTokens = ids.associateWith { 1L },
                            )
                        },
                    )
                }
            }
        }

        openSelectionFor(item.task.title)
        listOf("complete", "archive", "edit", "more", "delete").forEach { action ->
            compose.onNodeWithTag("task-selection-$action").assertIsDisplayed().assertIsEnabled()
        }

        compose.onNodeWithTag("task-selection-delete").performClick()
        compose.onNodeWithText("Delete 1 Task Permanently?").assertIsDisplayed()
        compose.onNodeWithText("This cannot be undone", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("confirm-task-selection-delete").assertIsEnabled()
        compose.onNodeWithText("Cancel").performClick()

        compose.onNodeWithTag("task-selection-archive").performClick()
        compose.onNodeWithText("Archive 1 Task?").assertIsDisplayed()
        compose.onNodeWithText("Archive 1").performClick()
        compose.runOnIdle { assertEquals(listOf(item), archived.get()) }
    }

    @Test
    fun historySelectionOffersRelevantActionsForCompletedAndArchivedTasks() {
        val today = LocalDate.of(2026, 8, 25)
        val completed = scheduledTask(2, "Completed selection", today, completed = true)
        val archived = scheduledTask(3, "Archived selection", today, archived = true)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(
                        completed = listOf(completed),
                        archived = listOf(archived),
                        currentDate = today,
                        loading = false,
                    ),
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Tasks tab").performClick()
        compose.onNodeWithTag("task-destination-History").performClick()
        openCurrentDestinationSelection(completed.task.title)
        listOf("reopen", "archive", "edit", "delete").forEach { action ->
            compose.onNodeWithTag("task-selection-$action").assertIsDisplayed().assertIsEnabled()
        }
        compose.onNodeWithText("Done").performClick()

        compose.onNodeWithText("Archived").performClick()
        openCurrentDestinationSelection(archived.task.title)
        listOf("restore", "edit", "delete").forEach { action ->
            compose.onNodeWithTag("task-selection-$action").assertIsDisplayed().assertIsEnabled()
        }
    }

    private fun openSelectionFor(title: String) {
        compose.onNodeWithContentDescription("Tasks tab").performClick()
        openCurrentDestinationSelection(title)
    }

    private fun openCurrentDestinationSelection(title: String) {
        compose.onNodeWithContentDescription("More task list actions").performClick()
        compose.onNodeWithText("Select Tasks").performClick()
        compose.onNodeWithContentDescription("Select task $title").performClick()
        compose.onNodeWithText("1 selected").assertIsDisplayed()
    }

    private fun scheduledTask(
        id: Long,
        title: String,
        date: LocalDate,
        completed: Boolean = false,
        archived: Boolean = false,
    ): ScheduledTask {
        val completedAt = 1_777_000_000_000L.takeIf { completed }
        val task = WhipTask(
            id = id,
            title = title,
            notes = "",
            scheduleKind = ScheduleKind.Once,
            date = date,
            recurrence = null,
            timeMinutes = null,
            reminderEnabled = false,
            archived = archived,
            completedAtMillis = completedAt,
            createdAtMillis = 1L,
            updatedAtMillis = 1L,
        )
        return ScheduledTask(
            task = task,
            originalDate = date,
            scheduledDate = date,
            completedAtMillis = completedAt,
        )
    }
}
