package com.whip.app.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.whip.app.captureVisualCatalogSurface
import com.whip.app.core.AppSettings
import com.whip.app.data.TaskDeletionImpact
import com.whip.app.domain.Area
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.WhipTask
import com.whip.app.ui.theme.WhipTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VisualCatalogTaskComponentsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun captureTaskComponentCatalog() {
        val fixture = mutableStateOf(TaskFixture.CreateEditor)
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                Surface(Modifier.fillMaxSize()) {
                    key(fixture.value) { TaskCatalogFixture(fixture.value) }
                }
            }
        }

        capture("tasks.editor.create")
        compose.onNodeWithText("Use a Template").performScrollTo().performClick()
        capture("tasks.recipe")

        show(fixture, TaskFixture.RepeatEditor)
        compose.onNodeWithTag("task-repeat-toggle").performScrollTo().performClick()
        capture("tasks.repeat-change")

        show(fixture, TaskFixture.EditEditor)
        capture("tasks.editor.edit")
        show(fixture, TaskFixture.Actions)
        capture("tasks.actions")
        show(fixture, TaskFixture.Completed)
        capture("tasks.completed-detail")
        show(fixture, TaskFixture.Delete)
        capture("tasks.permanent-delete")
        show(fixture, TaskFixture.PendingLaunch)
        capture("tasks.pending-editor-launch")
    }

    @Test
    fun captureTaskWorkspaceControlCatalog() {
        val first = catalogScheduledTask(completed = false)
        val second = first.copy(task = first.task.copy(id = 102L, title = "Confirm launch checklist"))
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(
                        today = listOf(first, second),
                        currentDate = LocalDate.of(2026, 9, 6),
                        loading = false,
                    ),
                    settingsState = SettingsUiState(
                        settings = AppSettings(setupCompleted = true),
                        areas = listOf(
                            Area("main", "Main", 0xFF5B8DEF, 0, false, 1L, 1L),
                            Area("work", "Work", 0xFFB875F0, 1, false, 1L, 1L),
                        ),
                        taxonomyLoaded = true,
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
        compose.onNodeWithContentDescription("Area scope: All Areas").performClick()
        capture("tasks.area-scope.menu")
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        compose.waitForIdle()

        compose.onNodeWithContentDescription("More task list actions").performClick()
        capture("tasks.row.menu")
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Open task details for ${first.task.title}").performClick()
        compose.onNodeWithTag("task-detail-section-Schedule").performClick()
        compose.onNodeWithText("Change Scheduled Date").performClick()
        capture("tasks.reschedule")
    }

    private fun show(state: androidx.compose.runtime.MutableState<TaskFixture>, value: TaskFixture) {
        compose.runOnIdle { state.value = value }
        compose.waitForIdle()
    }

    private fun capture(surfaceId: String) {
        compose.waitForIdle()
        captureVisualCatalogSurface(surfaceId)
    }
}

private enum class TaskFixture { CreateEditor, RepeatEditor, EditEditor, Actions, Completed, Delete, PendingLaunch }

@androidx.compose.runtime.Composable
private fun TaskCatalogFixture(fixture: TaskFixture) {
    val item = catalogScheduledTask(completed = fixture == TaskFixture.Completed)
    when (fixture) {
        TaskFixture.CreateEditor,
        TaskFixture.RepeatEditor,
        -> TaskEditorDialog(
            request = TaskEditorRequest(
                initialCapture = if (fixture == TaskFixture.CreateEditor) "Prepare quarterly review" else "Plan weekly review",
                initialPlacement = TaskPlacement.Inbox,
                sessionId = fixture.ordinal.toLong() + 10L,
            ),
            onDismiss = {},
            onSave = { _, _, _ -> },
            onRequestNotificationPermission = {},
            today = LocalDate.of(2026, 9, 6),
            naturalLanguageCapture = false,
        )
        TaskFixture.EditEditor -> TaskEditorDialog(
            request = TaskEditorRequest(task = item.task, sessionId = 20L),
            onDismiss = {},
            onSave = { _, _, _ -> },
            onRequestNotificationPermission = {},
            today = LocalDate.of(2026, 9, 6),
            naturalLanguageCapture = false,
        )
        TaskFixture.Actions -> TaskActionsDialog(
            item = item,
            onDismiss = {},
            onComplete = {},
            onEdit = {},
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
        TaskFixture.Completed -> CompletedTaskDialog(
            item = item,
            onDismiss = {},
            onEdit = {},
            onReopen = {},
            onDeletePermanently = {},
            onReopenOccurrence = {},
            onResetOccurrence = {},
        )
        TaskFixture.Delete -> PermanentTaskDeleteDialog(
            item = item,
            impact = TaskDeletionImpact(
                taskId = item.task.id,
                exists = true,
                title = item.task.title,
                recordedOccurrenceCount = 14,
                completedOccurrenceCount = 11,
                skippedOccurrenceCount = 2,
                openOccurrenceCount = 1,
                stepCount = 3,
            ),
            onDismiss = {},
            onConfirm = {},
        )
        TaskFixture.PendingLaunch -> PendingTaskEditorLaunchDialog(
            visible = true,
            saving = false,
            modifier = Modifier,
            onReplace = {},
            onKeepEditing = {},
        )
    }
}

private fun catalogScheduledTask(completed: Boolean): ScheduledTask {
    val date = LocalDate.of(2026, 9, 6)
    val completedAt = 1_788_715_200_000L.takeIf { completed }
    return ScheduledTask(
        task = WhipTask(
            id = 101L,
            title = "Prepare quarterly review",
            notes = "Gather progress, decisions, and the next commitments.",
            scheduleKind = ScheduleKind.Once,
            date = date,
            recurrence = null,
            timeMinutes = 9 * 60,
            reminderEnabled = true,
            archived = false,
            completedAtMillis = completedAt,
            createdAtMillis = 1L,
            updatedAtMillis = 2L,
            area = "Work",
            icon = "📝",
        ),
        originalDate = date,
        scheduledDate = date,
        completedAtMillis = completedAt,
    )
}
