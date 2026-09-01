package com.whip.app

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.test.espresso.Espresso.pressBack
import com.whip.app.data.TaskDeletionBatchImpact
import com.whip.app.core.PersistenceRequestState
import com.whip.app.core.WhipResult
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.OccurrenceState
import com.whip.app.domain.RecurrenceRule
import com.whip.app.domain.RecurrenceUnit
import com.whip.app.domain.ScheduledSubtask
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.TaskStep
import com.whip.app.domain.TaskEditBoundary
import com.whip.app.domain.toEditBoundary
import com.whip.app.domain.WhipTask
import com.whip.app.ui.TaskUiState
import com.whip.app.ui.TaskMutationKind
import com.whip.app.ui.TaskMutationReceipt
import com.whip.app.ui.WhipScreen
import com.whip.app.ui.theme.WhipTheme
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TaskBulkSelectionUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun activeSelectionKeepsPrimaryActionsVisibleAndSecondaryActionsInOverflow() {
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
                        onBulkArchiveTasks = { items, _ -> archived.set(items); true },
                        taskDeletionBatchImpact = deletionImpact.value,
                        onPreviewBulkTaskDeletion = { ids ->
                            deletionImpact.value = TaskDeletionBatchImpact(
                                requestedTaskIds = ids,
                                taskIds = ids,
                                titles = listOf(item.task.title),
                                revisionTokens = ids.associateWith { "reviewed-revision" },
                            )
                        },
                    )
                }
            }
        }

        openSelectionFor(item.task.title)
        listOf("complete", "edit", "more").forEach { action ->
            compose.onNodeWithTag("task-selection-$action").assertIsDisplayed().assertIsEnabled()
        }
        compose.onNodeWithTag("task-selection-more").performClick()
        compose.onNodeWithTag("task-selection-archive").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithTag("task-selection-delete").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithText("Delete Permanently").assertIsDisplayed()

        compose.onNodeWithTag("task-selection-delete").performClick()
        compose.onNodeWithText("Delete 1 Task Permanently?").assertIsDisplayed()
        compose.onNodeWithText("This cannot be undone", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("confirm-task-selection-delete").assertIsEnabled()
        compose.onNodeWithText("Cancel").performClick()

        compose.onNodeWithTag("task-selection-more").performClick()
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
        listOf("reopen", "edit", "more").forEach { action ->
            compose.onNodeWithTag("task-selection-$action").assertIsDisplayed().assertIsEnabled()
        }
        compose.onNodeWithText("Done").performClick()

        compose.onNodeWithText("Archived").performClick()
        openCurrentDestinationSelection(archived.task.title)
        listOf("restore", "edit", "more").forEach { action ->
            compose.onNodeWithTag("task-selection-$action").assertIsDisplayed().assertIsEnabled()
        }
    }

    @Test
    fun successfulReopenClosesCompletedInspectorWhenStableKeyBecomesOpen() {
        val today = LocalDate.of(2026, 8, 25)
        val completed = scheduledTask(92, "Close completed inspector", today, completed = true)
        val screenState = mutableStateOf(
            TaskUiState(completed = listOf(completed), currentDate = today, loading = false),
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = screenState.value,
                    unscopedTaskState = screenState.value,
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = { reviewed ->
                        val openTask = reviewed.task.copy(completedAtMillis = null, updatedAtMillis = 2L)
                        val openItem = reviewed.copy(task = openTask, completedAtMillis = null)
                        screenState.value = TaskUiState(
                            today = listOf(openItem),
                            currentDate = today,
                            loading = false,
                        )
                    },
                )
            }
        }

        compose.onNodeWithContentDescription("Tasks tab").performClick()
        compose.onNodeWithTag("task-destination-History").performClick()
        compose.onNodeWithText(completed.task.title).performClick()
        compose.onNodeWithTag("completed-task-surface").assertIsDisplayed()
        compose.onNodeWithText("Reopen Task").performClick()

        compose.onAllNodesWithTag("completed-task-surface").assertCountEquals(0)
        compose.onAllNodesWithText("This task is complete", substring = true).assertCountEquals(0)
    }

    @Test
    fun completedRecurringHistoryOffersSeriesEditInsteadOfDuplicateFutureBoundary() {
        val today = LocalDate.of(2026, 8, 25)
        val completed = scheduledTask(
            id = 93,
            title = "Closed recurring occurrence",
            date = today,
            completed = true,
            recurring = true,
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(completed = listOf(completed), currentDate = today, loading = false),
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
        compose.onNodeWithText(completed.task.title).performClick()

        compose.onNodeWithContentDescription("Edit Series").assertIsDisplayed()
        compose.onAllNodesWithText("Edit This and Future").assertCountEquals(0)
    }

    @Test
    fun archivedRecurringTaskEditsTheSeriesDefinitionInsteadOfAnOpenOccurrence() {
        val today = LocalDate.of(2026, 8, 25)
        val archived = scheduledTask(
            id = 95,
            title = "Archived recurring definition",
            date = today,
            archived = true,
            recurring = true,
        )
        val submittedFrom = AtomicReference<LocalDate?>(today)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(archived = listOf(archived), currentDate = today, loading = false),
                    unscopedTaskState = TaskUiState(archived = listOf(archived), currentDate = today, loading = false),
                    onSaveTask = { _, _, _ -> },
                    onSaveTaskRequest = { _, _, _, from, _ ->
                        submittedFrom.set(from)
                        false
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
        compose.onNodeWithTag("task-destination-History").performClick()
        compose.onNodeWithText("Archived").performClick()
        compose.onNodeWithText(archived.task.title).performClick()
        compose.onNodeWithContentDescription("Edit Series").performClick()
        compose.onNodeWithText("Save").performClick()

        compose.runOnIdle { assertEquals(null, submittedFrom.get()) }
    }

    @Test
    fun bulkDateFailureRetainsTheFrozenSelectionAndOnlyMatchingSuccessCloses() {
        val today = LocalDate.of(2026, 8, 25)
        val item = scheduledTask(12, "Retained bulk move", today)
        val mutationState = mutableStateOf<PersistenceRequestState<TaskMutationReceipt>>(
            PersistenceRequestState.Idle,
        )
        val requestId = AtomicReference<String?>(null)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(today = listOf(item), currentDate = today, loading = false),
                    taskAuthoredMutationState = mutationState.value,
                    onTaskAuthoredMutationResultConsumed = { consumed ->
                        val finished = mutationState.value as? PersistenceRequestState.Finished
                        if (finished?.requestId == consumed) mutationState.value = PersistenceRequestState.Idle
                    },
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                    onBulkPostponeTasksRequest = { _, _, request ->
                        requestId.set(request)
                        mutationState.value = PersistenceRequestState.Running(request)
                        true
                    },
                )
            }
        }

        openSelectionFor(item.task.title)
        compose.onNodeWithTag("task-selection-more").performClick()
        compose.onNodeWithText("Choose Date").performClick()
        compose.onNodeWithTag("date-picker-dialog").assertIsDisplayed()
        compose.onNodeWithText("Set").performClick()
        compose.onNodeWithTag("persistence-saving-overlay").assertIsDisplayed()
        compose.runOnIdle {
            val owned = requireNotNull(requestId.get())
            mutationState.value = PersistenceRequestState.Finished(
                owned,
                WhipResult.Failure("Disk unavailable"),
            )
        }

        compose.onNodeWithTag("date-picker-dialog").assertIsDisplayed()
        compose.onNodeWithTag("date-picker-save-problem")
            .assertContentDescriptionContains("Disk unavailable", substring = true)
        compose.onNodeWithText("1 selected").assertIsDisplayed()
        compose.onNodeWithText("Set").performClick()
        compose.runOnIdle {
            val owned = requireNotNull(requestId.get())
            mutationState.value = PersistenceRequestState.Finished(
                owned,
                WhipResult.Success(
                    TaskMutationReceipt(
                        TaskMutationKind.BulkRescheduled,
                        setOf(item.task.id),
                        occurrenceKeys = setOf(item.stableKey),
                        effectiveDate = today,
                    ),
                ),
            )
        }

        compose.onAllNodesWithTag("date-picker-dialog").assertCountEquals(0)
        compose.onAllNodesWithText("1 selected").assertCountEquals(0)
    }

    @Test
    fun bulkAreaChangeCannotApplyUntilAnAreaIsExplicitlyChosen() {
        val today = LocalDate.of(2026, 8, 25)
        val item = scheduledTask(13, "Explicit Area", today)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(today = listOf(item), currentDate = today, loading = false),
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        openSelectionFor(item.task.title)
        compose.onNodeWithTag("task-selection-edit").performClick()
        compose.onNodeWithTag("task-bulk-edit-area-toggle").performClick()

        compose.onNodeWithText("Choose an Area before applying this change.").assertIsDisplayed()
        compose.onNodeWithText("Apply Changes").assertIsNotEnabled()
    }

    @Test
    fun quickCaptureReconnectsAfterRecreationAndClearsTheTrimmedCommittedDraftOnce() {
        val today = LocalDate.of(2026, 8, 25)
        val mutationState = mutableStateOf<PersistenceRequestState<TaskMutationReceipt>>(
            PersistenceRequestState.Idle,
        )
        val requestId = AtomicReference<String?>(null)
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(currentDate = today, loading = false),
                    taskAuthoredMutationState = mutationState.value,
                    onTaskAuthoredMutationResultConsumed = { consumed ->
                        val finished = mutationState.value as? PersistenceRequestState.Finished
                        if (finished?.requestId == consumed) mutationState.value = PersistenceRequestState.Idle
                    },
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                    onQuickAddTaskRequest = { _, _, _, request ->
                        requestId.set(request)
                        mutationState.value = PersistenceRequestState.Running(request)
                        true
                    },
                )
            }
        }

        compose.onNodeWithContentDescription("Tasks tab").performClick()
        compose.onNodeWithTag("task-quick-capture").performTextInput("  Ship report  ")
        compose.onNodeWithContentDescription("Add task now").performClick()
        compose.onNodeWithText("Saving…").assertIsDisplayed()
        compose.onNodeWithTag("task-quick-capture").assertIsNotEnabled()

        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Saving…").assertIsDisplayed()
        compose.runOnIdle {
            val owned = requireNotNull(requestId.get())
            mutationState.value = PersistenceRequestState.Finished(
                owned,
                WhipResult.Success(TaskMutationReceipt(TaskMutationKind.Created, setOf(91L))),
            )
        }

        compose.onNodeWithTag("task-quick-capture").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")),
        )
        compose.onAllNodesWithText("Saving…").assertCountEquals(0)
    }

    @Test
    fun restoredExistingTaskEditorPreservesItsExactIdentityAndRevisionBoundary() {
        val today = LocalDate.of(2026, 8, 25)
        val item = scheduledTask(94, "Restore exact editor", today)
        val submittedTaskId = AtomicReference<Long?>(null)
        val submittedExpected = AtomicReference<TaskEditBoundary?>(null)
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(
                        taskEntities = listOf(item.task),
                        today = listOf(item),
                        currentDate = today,
                        loading = false,
                    ),
                    unscopedTaskState = TaskUiState(
                        taskEntities = listOf(item.task),
                        today = listOf(item),
                        currentDate = today,
                        loading = false,
                    ),
                    onSaveTask = { _, _, _ -> },
                    onSaveTaskRequest = { taskId, expected, _, _, _ ->
                        submittedTaskId.set(taskId)
                        submittedExpected.set(expected)
                        false
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
        compose.onNodeWithText(item.task.title).performClick()
        compose.onNodeWithContentDescription("Edit Task").performClick()
        compose.onNodeWithTag("task-editor-title").performTextInput(" revised")

        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("task-editor-title").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.EditableText,
                AnnotatedString(" revised${item.task.title}"),
            ),
        )
        compose.onNodeWithText("Save").performClick()

        compose.runOnIdle {
            assertEquals(item.task.id, submittedTaskId.get())
            assertEquals(item.toEditBoundary(), submittedExpected.get())
        }
    }

    @Test
    fun bulkEditDraftAndSelectionSurviveSavedStateRestoration() {
        val today = LocalDate.of(2026, 8, 25)
        val item = scheduledTask(14, "Restore bulk draft", today)
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(today = listOf(item), currentDate = today, loading = false),
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        openSelectionFor(item.task.title)
        compose.onNodeWithTag("task-selection-edit").performClick()
        compose.onNodeWithTag("task-bulk-edit-tags-toggle").performClick()
        compose.onNodeWithTag("task-bulk-edit-tags").performTextInput("work, urgent")

        restoration.emulateSavedInstanceStateRestore()

        compose.onNodeWithText("Edit 1 Task").assertIsDisplayed()
        compose.onNodeWithTag("task-bulk-edit-tags").assertTextContains("work, urgent")
        compose.onNodeWithText("1 selected").assertIsDisplayed()
    }

    @Test
    fun bulkDeleteReissuesItsExactPreviewAfterStateRestoration() {
        val today = LocalDate.of(2026, 8, 25)
        val item = scheduledTask(15, "Restore deletion preview", today)
        val impact = mutableStateOf<TaskDeletionBatchImpact?>(null)
        val previewCalls = AtomicInteger()
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(today = listOf(item), currentDate = today, loading = false),
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                    taskDeletionBatchImpact = impact.value,
                    onClearBulkTaskDeletionPreview = { impact.value = null },
                    onPreviewBulkTaskDeletion = { ids ->
                        previewCalls.incrementAndGet()
                        impact.value = TaskDeletionBatchImpact(
                            requestedTaskIds = ids,
                            taskIds = ids,
                            titles = listOf(item.task.title),
                            revisionTokens = ids.associateWith { "restored-review" },
                        )
                    },
                )
            }
        }

        openSelectionFor(item.task.title)
        compose.onNodeWithTag("task-selection-more").performClick()
        compose.onNodeWithTag("task-selection-delete").performClick()
        compose.onNodeWithTag("confirm-task-selection-delete").assertIsEnabled()
        compose.runOnIdle { impact.value = null }

        restoration.emulateSavedInstanceStateRestore()

        compose.onNodeWithText("Delete 1 Task Permanently?").assertIsDisplayed()
        compose.onNodeWithTag("confirm-task-selection-delete").assertIsEnabled()
        compose.runOnIdle { assertEquals(2, previewCalls.get()) }
    }

    @Test
    fun fixedDateBulkMoveFailureKeepsSelectionAndShowsRetryContext() {
        val today = LocalDate.of(2026, 8, 25)
        val item = scheduledTask(16, "Keep failed move", today)
        val mutationState = mutableStateOf<PersistenceRequestState<TaskMutationReceipt>>(
            PersistenceRequestState.Idle,
        )
        val requestId = AtomicReference<String?>(null)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(today = listOf(item), currentDate = today, loading = false),
                    taskAuthoredMutationState = mutationState.value,
                    onTaskAuthoredMutationResultConsumed = { mutationState.value = PersistenceRequestState.Idle },
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                    onBulkPostponeTasksRequest = { _, _, request ->
                        requestId.set(request)
                        mutationState.value = PersistenceRequestState.Running(request)
                        true
                    },
                )
            }
        }

        openSelectionFor(item.task.title)
        compose.onNodeWithTag("task-selection-more").performClick()
        compose.onNodeWithText("Move to Tomorrow").performClick()
        compose.onNodeWithTag("persistence-saving-overlay")
            .assertIsDisplayed()
            .assertContentDescriptionContains("Editing is temporarily unavailable", substring = true)
        compose.onAllNodesWithText("Done").assertCountEquals(0)
        compose.onAllNodesWithTag("task-selection-complete").assertCountEquals(0)
        compose.onAllNodesWithTag("task-workspace-navigation").assertCountEquals(0)
        compose.runOnIdle {
            mutationState.value = PersistenceRequestState.Finished(
                requireNotNull(requestId.get()),
                WhipResult.Failure("Task schedule changed while this action was open"),
            )
        }

        compose.onNodeWithTag("task-bulk-quick-date-problem")
            .assertIsDisplayed()
            .assertContentDescriptionContains("Task schedule changed while this action was open", substring = true)
        compose.onNodeWithText("1 selected").assertIsDisplayed()
    }

    @Test
    fun bulkArchiveImpactRemainsScrollableAtTwoHundredPercentText() {
        val today = LocalDate.of(2026, 8, 25)
        val items = (1L..8L).map { id ->
            scheduledTask(id + 100, "A deliberately long archived Task title number $id", today)
        }
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                    WhipScreen(
                        state = TaskUiState(today = items, currentDate = today, loading = false),
                        onSaveTask = { _, _, _ -> },
                        onComplete = {},
                        onSkip = {},
                        onReschedule = { _, _ -> },
                        onArchive = {},
                        onReopen = {},
                    )
                }
            }
        }

        compose.onNodeWithContentDescription("Tasks tab").performClick()
        compose.onNodeWithContentDescription("More task list actions").performClick()
        compose.onNodeWithText("Select Tasks").performClick()
        compose.onNodeWithText("Select All").performClick()
        compose.onNodeWithTag("task-selection-more").performClick()
        compose.onNodeWithTag("task-selection-archive").performClick()

        compose.onNodeWithText("Archive 8 Tasks?").assertIsDisplayed()
        compose.onNodeWithTag("task-bulk-archive-impact").performTouchInput { swipeUp() }
        compose.onNodeWithText("Completed history is kept", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Archive 8").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun bulkArchiveFailureRetainsFrozenSelectionAndMatchingSuccessAloneCloses() {
        val today = LocalDate.of(2026, 8, 25)
        val item = scheduledTask(196, "Request-owned archive", today)
        val mutationState = mutableStateOf<PersistenceRequestState<TaskMutationReceipt>>(
            PersistenceRequestState.Idle,
        )
        val requestId = AtomicReference<String?>(null)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(today = listOf(item), currentDate = today, loading = false),
                    taskAuthoredMutationState = mutationState.value,
                    onTaskAuthoredMutationResultConsumed = { consumed ->
                        val finished = mutationState.value as? PersistenceRequestState.Finished
                        if (finished?.requestId == consumed) mutationState.value = PersistenceRequestState.Idle
                    },
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                    onBulkArchiveTasks = { _, request ->
                        requestId.set(request)
                        mutationState.value = PersistenceRequestState.Running(request)
                        true
                    },
                )
            }
        }

        openSelectionFor(item.task.title)
        compose.onNodeWithTag("task-selection-more").performClick()
        compose.onNodeWithTag("task-selection-archive").performClick()
        compose.onNodeWithText("Archive 1").performClick()
        compose.onNodeWithText("Archiving…").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithText("Cancel").assertIsNotEnabled()
        pressBack()
        compose.onNodeWithText("Archive 1 Task?").assertIsDisplayed()

        compose.runOnIdle {
            mutationState.value = PersistenceRequestState.Finished(
                requireNotNull(requestId.get()),
                WhipResult.Failure("Archive storage unavailable"),
            )
        }
        compose.onNodeWithTag("task-bulk-archive-problem")
            .assertContentDescriptionContains("Archive storage unavailable", substring = true)
        compose.onNodeWithText("Archive 1 Task?").assertIsDisplayed()
        compose.onNodeWithText("1 selected").assertIsDisplayed()

        compose.onNodeWithText("Archive 1").performClick()
        compose.runOnIdle {
            val owned = requireNotNull(requestId.get())
            mutationState.value = PersistenceRequestState.Finished(
                "not-$owned",
                WhipResult.Success(TaskMutationReceipt(TaskMutationKind.BulkArchived, setOf(item.task.id))),
            )
        }
        compose.onNodeWithText("Archive 1 Task?").assertIsDisplayed()
        compose.runOnIdle {
            mutationState.value = PersistenceRequestState.Finished(
                requireNotNull(requestId.get()),
                WhipResult.Success(TaskMutationReceipt(TaskMutationKind.BulkArchived, setOf(item.task.id))),
            )
        }
        compose.onAllNodesWithText("Archive 1 Task?").assertCountEquals(0)
        compose.onAllNodesWithText("1 selected").assertCountEquals(0)
    }

    @Test
    fun bulkCompletionReviewsUnfinishedSubtasksBeforeCommitAndCancelKeepsSelection() {
        val today = LocalDate.of(2026, 8, 25)
        val item = scheduledTask(4, "Bulk subtask review", today, unfinishedSubtasks = 1)
        val completed = AtomicReference<List<ScheduledTask>>(emptyList())
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(today = listOf(item), currentDate = today, loading = false),
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                    onBulkCompleteTasks = completed::set,
                )
            }
        }

        openSelectionFor(item.task.title)
        compose.onNodeWithTag("task-selection-complete").performClick()
        compose.onNodeWithTag("task-bulk-completion-review").assertIsDisplayed()
        compose.onNodeWithText("1 unfinished subtask remains across 1 selected task", substring = true).assertIsDisplayed()
        compose.runOnIdle { assertEquals(emptyList<ScheduledTask>(), completed.get()) }

        compose.onNodeWithTag("cancel-task-bulk-completion").performClick()
        compose.onNodeWithText("1 selected").assertIsDisplayed()
        compose.onNodeWithTag("task-selection-complete").performClick()
        compose.onNodeWithTag("confirm-task-bulk-completion").performClick()
        compose.runOnIdle { assertEquals(listOf(item), completed.get()) }
    }

    private fun openSelectionFor(title: String) {
        compose.onNodeWithContentDescription("Tasks tab").performClick()
        openCurrentDestinationSelection(title)
    }

    private fun openCurrentDestinationSelection(title: String) {
        compose.onNodeWithText("Select").performClick()
        compose.onNodeWithContentDescription("Select task $title").performClick()
        compose.onNodeWithText("1 selected").assertIsDisplayed()
    }

    private fun scheduledTask(
        id: Long,
        title: String,
        date: LocalDate,
        completed: Boolean = false,
        archived: Boolean = false,
        unfinishedSubtasks: Int = 0,
        recurring: Boolean = false,
    ): ScheduledTask {
        val completedAt = 1_777_000_000_000L.takeIf { completed }
        val task = WhipTask(
            id = id,
            title = title,
            notes = "",
            scheduleKind = if (recurring) ScheduleKind.Recurring else ScheduleKind.Once,
            date = date,
            recurrence = RecurrenceRule(
                unit = RecurrenceUnit.Days,
                interval = 1,
                startDate = date,
            ).takeIf { recurring },
            timeMinutes = null,
            reminderEnabled = false,
            archived = archived,
            completedAtMillis = completedAt.takeUnless { recurring },
            createdAtMillis = 1L,
            updatedAtMillis = 1L,
        )
        return ScheduledTask(
            task = task,
            originalDate = date,
            scheduledDate = date,
            completedAtMillis = completedAt,
            occurrenceState = OccurrenceState.Completed.takeIf { recurring && completed },
            subtasks = List(unfinishedSubtasks) { index ->
                val stepId = id * 100 + index
                ScheduledSubtask(
                    step = TaskStep(
                        id = stepId,
                        taskId = id,
                        title = "Subtask ${index + 1}",
                        position = index,
                        createdAtMillis = 1,
                        updatedAtMillis = 1,
                    ),
                    completed = false,
                    completedAtMillis = null,
                    title = "Subtask ${index + 1}",
                )
            },
        )
    }
}
