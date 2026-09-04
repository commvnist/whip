package com.whip.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.whip.app.WhipApplication
import com.whip.app.core.HomeSection
import com.whip.app.core.CommittedEntitySaveCancellation
import com.whip.app.core.OperationFeedbackPresentation
import com.whip.app.core.OperationStatus
import com.whip.app.core.EntitySaveReceipt
import com.whip.app.core.PersistenceRequestState
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipResult
import com.whip.app.core.completeCommittedEntitySave
import com.whip.app.core.completeCommittedPersistence
import com.whip.app.core.revealHomeSection
import com.whip.app.core.saveFollowUpWarning
import com.whip.app.core.tryStartPersistenceRequest
import com.whip.app.core.zoneId
import com.whip.app.data.TaskRepository
import com.whip.app.data.TaskDeletionBatchImpact
import com.whip.app.data.TaskDeletionImpact
import com.whip.app.data.TaskBulkEdit
import com.whip.app.data.CommittedTaskDeletionBatchCancellation
import com.whip.app.data.CommittedTaskDeletionCancellation
import com.whip.app.domain.OccurrenceState
import com.whip.app.domain.MissedOccurrencePolicy
import com.whip.app.domain.RecurrenceEngine
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.ScheduledSubtask
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskEditBoundary
import com.whip.app.domain.TaskOccurrence
import com.whip.app.domain.TaskStep
import com.whip.app.domain.TaskStepState
import com.whip.app.domain.TaskStepSnapshot
import com.whip.app.domain.WhipTask
import com.whip.app.domain.RepeatStepPolicy
import com.whip.app.domain.RecurrenceAnchor
import com.whip.app.domain.visibleTaskStepsForOccurrence
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class TaskDestination {
    Inbox,
    Today,
    Upcoming,
    Completed,
    Archived,
}

data class TaskUiState(
    /** Repository entities, independent of the finite schedule projection below. */
    val taskEntities: List<WhipTask> = emptyList(),
    val inbox: List<ScheduledTask> = emptyList(),
    val today: List<ScheduledTask> = emptyList(),
    val upcoming: List<ScheduledTask> = emptyList(),
    val completed: List<ScheduledTask> = emptyList(),
    val archived: List<ScheduledTask> = emptyList(),
    val planning: List<ScheduledTask> = emptyList(),
    val occurrences: List<TaskOccurrence> = emptyList(),
    val currentDate: LocalDate = LocalDate.now(),
    val loading: Boolean = true,
    val errorMessage: String? = null,
)

data class TaskOperationFeedback(
    val status: OperationStatus = OperationStatus.Idle,
    val undoMessage: String? = null,
    val undoToken: Long? = null,
    val quickAddedTaskId: Long? = null,
)

enum class TaskMutationKind {
    Created,
    Rescheduled,
    BulkRescheduled,
    BulkMetadataUpdated,
    BulkArchived,
    PermanentlyDeleted,
    BulkPermanentlyDeleted,
}

data class TaskDeletionReceipt(
    val tasksDeleted: Int,
)

data class TaskMutationReceipt(
    val kind: TaskMutationKind,
    val taskIds: Set<Long>,
    val occurrenceKeys: Set<String> = emptySet(),
    val effectiveDate: LocalDate? = null,
    val deletion: TaskDeletionReceipt? = null,
    val warnings: List<String> = emptyList(),
)

internal class CommittedTaskMutationCancellation(
    val receipt: TaskMutationReceipt,
    cause: CancellationException,
) : CancellationException(cause.message) {
    init { initCause(cause) }
}

internal suspend fun completeCommittedTaskMutation(
    commit: suspend () -> TaskMutationReceipt,
    followUp: suspend (TaskMutationReceipt) -> TaskMutationReceipt,
): TaskMutationReceipt = completeCommittedPersistence(
    commit = commit,
    followUp = followUp,
    onCancellation = { committed, cancelled -> CommittedTaskMutationCancellation(committed, cancelled) },
    onOrdinaryFailure = { committed ->
        committed.copy(
            warnings = committed.warnings +
                "Some post-save updates did not finish; the Task change itself was saved.",
        )
    },
)

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as WhipApplication
    private val repository: TaskRepository = app.taskRepository
    private val reminders = app.reminderScheduler
    private val clock = app.clock

    private val _operationFeedback = MutableStateFlow(TaskOperationFeedback())
    val operationFeedback: StateFlow<TaskOperationFeedback> = _operationFeedback.asStateFlow()
    private val _editorSaveState = MutableStateFlow<PersistenceRequestState<EntitySaveReceipt>>(
        PersistenceRequestState.Idle,
    )
    val editorSaveState: StateFlow<PersistenceRequestState<EntitySaveReceipt>> = _editorSaveState.asStateFlow()
    private val _authoredMutationState = MutableStateFlow<PersistenceRequestState<TaskMutationReceipt>>(
        PersistenceRequestState.Idle,
    )
    val authoredMutationState: StateFlow<PersistenceRequestState<TaskMutationReceipt>> =
        _authoredMutationState.asStateFlow()
    private var pendingUndoAction: TaskUndoAction? = null
    private var pendingUndoMessage: String? = null
    private var pendingUndoToken: Long? = null
    private var nextUndoToken = 0L
    private var pendingQuickAddTaskId: Long? = null
    private val _taskDeletionImpact = MutableStateFlow<TaskDeletionImpact?>(null)
    val taskDeletionImpact: StateFlow<TaskDeletionImpact?> = _taskDeletionImpact.asStateFlow()
    private val _taskDeletionBatchImpact = MutableStateFlow<TaskDeletionBatchImpact?>(null)
    val taskDeletionBatchImpact: StateFlow<TaskDeletionBatchImpact?> = _taskDeletionBatchImpact.asStateFlow()
    private val reloadKey = MutableStateFlow(0)

    private val taskData = combine(
        repository.tasks,
        repository.occurrences,
        repository.steps,
        repository.stepStates,
        repository.stepSnapshots,
        ::TaskData,
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState = reloadKey.flatMapLatest {
        combine(
            taskData,
            app.calendarContext,
            app.settingsRepository.settings,
        ) { data, calendar, settings ->
            buildUiState(
                tasks = data.tasks,
                occurrences = data.occurrences,
                steps = data.steps,
                stepStates = data.stepStates,
                stepSnapshots = data.stepSnapshots,
                today = calendar.logicalDate,
                showAllUpcomingRecurringOccurrences = settings.showAllUpcomingTaskOccurrences,
                zoneId = calendar.zoneId,
            )
        }.catch { error ->
            emit(TaskUiState(currentDate = clock.today(), loading = false, errorMessage = error.message ?: "Could not load tasks"))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TaskUiState(currentDate = clock.today()),
    )

    init {
        viewModelScope.launch {
            app.calendarContext.map { it.logicalDate }.distinctUntilChanged().collect {
                try {
                    reminders.syncAll()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    _operationFeedback.value = TaskOperationFeedback(
                        status = OperationStatus.Failed(
                            error.message ?: "Some Task reminders could not be refreshed",
                            error,
                        ),
                    )
                }
            }
        }
        viewModelScope.launch {
            app.userDataGeneration.drop(1).collect {
                clearPendingUndo()
                _taskDeletionImpact.value = null
                _taskDeletionBatchImpact.value = null
                _editorSaveState.value = PersistenceRequestState.Idle
                _authoredMutationState.value = PersistenceRequestState.Idle
                _operationFeedback.value = TaskOperationFeedback()
            }
        }
    }

    fun consumeOperationStatus() {
        _operationFeedback.value = _operationFeedback.value.copy(status = OperationStatus.Idle)
    }

    fun consumeEditorSaveResult(requestId: String) {
        if ((_editorSaveState.value as? PersistenceRequestState.Finished)?.requestId == requestId) {
            _editorSaveState.value = PersistenceRequestState.Idle
        }
    }

    fun consumeAuthoredMutationResult(requestId: String) {
        val finished = (_authoredMutationState.value as? PersistenceRequestState.Finished)
            ?.takeIf { it.requestId == requestId }
        if (finished != null) {
            val receipt = (finished.result as? WhipResult.Success)?.value
            when (receipt?.kind) {
                TaskMutationKind.PermanentlyDeleted -> _taskDeletionImpact.value = null
                TaskMutationKind.BulkPermanentlyDeleted -> _taskDeletionBatchImpact.value = null
                else -> Unit
            }
            _authoredMutationState.value = PersistenceRequestState.Idle
        }
    }

    fun retryLoading() { reloadKey.value++ }

    fun saveTask(
        taskId: Long?,
        draft: TaskDraft,
        fromOccurrence: LocalDate? = null,
        requestId: String? = null,
        expectedBoundary: TaskEditBoundary? = null,
        onFinished: (Boolean) -> Unit = {},
    ): Boolean {
        if (requestId != null && !_editorSaveState.tryStartPersistenceRequest(requestId)) {
            onFinished(false)
            return false
        }
        runEntitySaveOperation(
            runningMessage = if (taskId == null) "Creating task…" else "Saving task…",
            successMessage = when {
                taskId == null -> "Task created"
                fromOccurrence != null -> "Future series created · earlier history preserved"
                else -> "Task saved"
            },
            requestId = requestId,
            onFinished = { result ->
                if (requestId != null &&
                    (_editorSaveState.value as? PersistenceRequestState.Running)?.requestId == requestId
                ) {
                    _editorSaveState.value = PersistenceRequestState.Finished(requestId, result)
                }
                onFinished(result is WhipResult.Success)
            },
        ) {
            completeCommittedEntitySave(
                commit = {
                    val savedId = if (taskId == null) {
                        repository.create(draft)
                    } else if (expectedBoundary != null) {
                        require(expectedBoundary.taskId == taskId) { "Task editor identity changed" }
                        repository.updateIfCurrent(expectedBoundary, draft, fromOccurrence)
                    } else {
                        require(requestId == null) {
                            "The Task changed while this editor was open. Close it, review the latest version, and try again."
                        }
                        repository.update(taskId, draft, fromOccurrence)
                    }
                    EntitySaveReceipt(savedId, draft.areaId, areaVerified = false)
                },
                followUp = { committed ->
                    val savedId = requireNotNull(committed.entityId)
                    var resolvedAreaId = committed.areaId
                    var areaVerified = false
                    val warnings = listOfNotNull(
                        saveFollowUpWarning("Saved Area could not be verified; showing All Areas.") {
                            val saved = requireNotNull(repository.getTask(savedId)) { "Saved Task could not be reread" }
                            resolvedAreaId = saved.areaId
                            areaVerified = true
                        },
                        saveFollowUpWarning("Some tag suggestions did not refresh. Saving again will retry them.") {
                            draft.tags.forEach { app.measurementRepository.ensureTag(it) }
                        },
                        saveFollowUpWarning("Reminder refresh did not finish. Reopen and save the Task to retry.") {
                            reminders.syncTask(savedId)
                            if (taskId != null && savedId != taskId) reminders.syncTask(taskId)
                        },
                    )
                    committed.copy(
                        areaId = resolvedAreaId,
                        warnings = warnings,
                        areaVerified = areaVerified,
                    )
                },
            )
        }
        return true
    }

    fun quickAddTask(
        capture: String,
        defaultDate: LocalDate?,
        areaId: String?,
        onFinished: (Boolean) -> Unit = {},
    ) {
        val settings = app.settingsRepository.current()
        val draft = buildQuickAddTaskDraft(
            capture = capture,
            defaultDate = defaultDate,
            areaId = areaId,
            smartCaptureToday = clock.today(settings.zoneId())
                .takeIf { settings.naturalLanguageTaskCapture },
        )
        if (draft == null) {
            onFinished(false)
            return
        }
        runOperation(
            runningMessage = "Adding task…",
            successMessage = "Task added",
            onFinished = onFinished,
            successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
        ) {
            val taskId = repository.create(draft)
            try {
                val revision = app.taskDeletionCoordinator.preview(taskId).revisionToken
                offerUndo("Quick Add can be undone", TaskUndoAction.DeleteCreated(taskId, revision))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                warn("Undo could not be prepared; the Task was still added.")
            }
            try {
                reminders.syncTask(taskId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                warn("Reminder refresh did not finish; the Task was still added.")
            }
        }
    }

    fun quickAddTaskRequest(
        capture: String,
        defaultDate: LocalDate?,
        areaId: String?,
        requestId: String,
    ): Boolean {
        val settings = app.settingsRepository.current()
        val draft = buildQuickAddTaskDraft(
            capture = capture,
            defaultDate = defaultDate,
            areaId = areaId,
            smartCaptureToday = clock.today(settings.zoneId())
                .takeIf { settings.naturalLanguageTaskCapture },
        ) ?: return false
        return runAuthoredTaskMutation(
            running = "Adding task…",
            success = "Task added",
            requestId = requestId,
            savedDescription = "Task",
        ) {
            completeCommittedTaskMutation(
                commit = {
                    val taskId = repository.create(draft)
                    TaskMutationReceipt(TaskMutationKind.Created, setOf(taskId))
                },
                followUp = { committed ->
                    val taskId = committed.taskIds.single()
                    val withUndo = try {
                        val revision = app.taskDeletionCoordinator.preview(taskId).revisionToken
                        installUndo("Quick Add can be undone", TaskUndoAction.DeleteCreated(taskId, revision))
                        committed
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        committed.copy(
                            warnings = committed.warnings +
                                "Undo could not be prepared; the Task was still added.",
                        )
                    }
                    withUndo.withReminderRefresh(reminders)
                },
            )
        }
    }

    fun complete(item: ScheduledTask) {
        runOperation(
            "Completing task…",
            "Task completed",
            successFeedbackPresentation = OperationFeedbackPresentation.Inline,
        ) {
            repository.complete(item)
            val completed = try {
                currentClosedSnapshot(item)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                warn("Undo could not be prepared; the Task was still completed.")
                null
            }
            refreshReminders(listOf(item.task.id))
            completed?.let { offerUndo("Completion can be undone", TaskUndoAction.Complete(listOf(it))) }
        }
    }

    fun skip(item: ScheduledTask) {
        runOperation(
            "Skipping occurrence…",
            "Occurrence skipped",
            successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
        ) {
            repository.skip(item)
            val skipped = try {
                currentClosedSnapshot(item)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                warn("Undo could not be prepared; the occurrence was still skipped.")
                null
            }
            refreshReminders(listOf(item.task.id))
            skipped?.let { offerUndo("Skip can be undone", TaskUndoAction.ResetOccurrences(listOf(it))) }
        }
    }

    fun reschedule(item: ScheduledTask, newDate: LocalDate) {
        rescheduleMutation(item, newDate, requestId = null)
    }

    fun rescheduleRequest(item: ScheduledTask, newDate: LocalDate, requestId: String): Boolean =
        rescheduleMutation(item, newDate, requestId)

    private fun rescheduleMutation(
        item: ScheduledTask,
        newDate: LocalDate,
        requestId: String?,
    ): Boolean = runAuthoredTaskMutation(
        running = "Moving task…",
        success = "Task moved",
        requestId = requestId,
        savedDescription = "Task schedule",
    ) {
        completeCommittedTaskMutation(
            commit = {
                repository.reschedule(item, newDate)
                installUndo("Move can be undone", TaskUndoAction.Reschedule(item, newDate))
                TaskMutationReceipt(
                    kind = TaskMutationKind.Rescheduled,
                    taskIds = setOf(item.task.id),
                    occurrenceKeys = setOf(item.stableKey),
                    effectiveDate = newDate,
                )
            },
            followUp = { committed -> committed.withReminderRefresh(reminders) },
        )
    }

    fun setStepCompleted(item: ScheduledTask, stepId: Long, completed: Boolean) {
        runOperation(
            runningMessage = "Updating subtask…",
            successMessage = if (completed) "Subtask completed" else "Subtask reopened",
            successFeedbackPresentation = OperationFeedbackPresentation.Inline,
        ) {
            repository.setStepCompleted(item, stepId, completed)
            refreshReminders(listOf(item.task.id))
        }
    }

    fun promoteStep(item: ScheduledTask, stepId: Long) {
        runOperation(
            "Moving subtask…",
            "Subtask moved to a new Inbox task",
            successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
        ) {
            val promotedTaskId = repository.promoteStep(item, stepId)
            try {
                val sourceStep = requireNotNull(
                    repository.getTask(item.task.id)?.steps?.firstOrNull { it.id == stepId },
                ) { "The source Subtask no longer exists" }
                val promotedRevision = app.taskDeletionCoordinator.preview(promotedTaskId).revisionToken
                offerUndo(
                    "Move to a new Task can be undone",
                    TaskUndoAction.Promote(
                        promotedTaskId = promotedTaskId,
                        expectedRevisionToken = promotedRevision,
                        sourceTaskId = item.task.id,
                        sourceStepId = stepId,
                        expectedSourceStepUpdatedAtMillis = sourceStep.updatedAtMillis,
                    ),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                warn("Undo could not be prepared; the Subtask was still moved.")
            }
            listOf(item.task.id, promotedTaskId).forEach { taskId ->
                try {
                    reminders.syncTask(taskId)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    warn("Reminder refresh did not finish for Task $taskId; the Subtask was still moved.")
                }
            }
        }
    }

    fun archive(taskId: Long) {
        runOperation(
            "Archiving task…",
            "Task archived",
            successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
        ) {
            repository.archive(taskId)
            refreshReminders(listOf(taskId))
            offerUndo("Archive can be undone", TaskUndoAction.Restore(listOf(taskId)))
        }
    }

    fun restore(taskId: Long) {
        runOperation("Restoring task…", "Task restored") {
            repository.restore(taskId)
            refreshReminders(listOf(taskId))
        }
    }

    fun deletePermanently(taskId: Long) {
        val revision = _taskDeletionImpact.value
            ?.takeIf { it.taskId == taskId && it.exists }
            ?.revisionToken
        deletePermanentlyMutation(taskId, revision, requestId = null)
    }

    fun deletePermanentlyRequest(
        taskId: Long,
        expectedRevisionToken: String,
        requestId: String,
    ): Boolean = deletePermanentlyMutation(taskId, expectedRevisionToken, requestId)

    private fun deletePermanentlyMutation(
        taskId: Long,
        expectedRevisionToken: String?,
        requestId: String?,
    ): Boolean = runAuthoredTaskMutation(
        running = "Deleting task permanently…",
        success = "Task permanently deleted",
        requestId = requestId,
        savedDescription = "permanent Task deletion",
    ) {
        completeCommittedTaskMutation(
            commit = {
                val revision = requireNotNull(expectedRevisionToken) {
                    "Review the exact deletion impact before deleting this Task"
                }
                val summary = try {
                    app.taskDeletionCoordinator.delete(taskId, revision)
                } catch (cancelled: CommittedTaskDeletionCancellation) {
                    clearUndoForCommittedOperation()
                    throw CommittedTaskMutationCancellation(
                        TaskMutationReceipt(
                            TaskMutationKind.PermanentlyDeleted,
                            setOf(taskId),
                            deletion = TaskDeletionReceipt(
                                tasksDeleted = 1,
                            ),
                            warnings = cancelled.summary.warnings,
                        ),
                        cancelled,
                    )
                }
                check(summary.taskDeleted) { "Task no longer exists; review the deletion impact again" }
                clearUndoForCommittedOperation()
                TaskMutationReceipt(
                    TaskMutationKind.PermanentlyDeleted,
                    setOf(taskId),
                    deletion = TaskDeletionReceipt(
                        tasksDeleted = 1,
                    ),
                    warnings = summary.warnings,
                )
            },
            // syncTask cancels every scheduled reminder carrying this task's tag after
            // the authoritative deletion. A cleanup failure is a warning, not a retry.
            followUp = { committed -> committed.withDeletedTaskReminderCleanup(reminders) },
        )
    }

    fun previewPermanentDeletion(taskId: Long) {
        _taskDeletionImpact.value = null
        viewModelScope.launch {
            runCatching {
                checkNotNull(app.withUserDataAccess { app.taskDeletionCoordinator.preview(taskId) }) {
                    "Whip data is unavailable while recovery is in progress"
                }
            }
                .onSuccess { _taskDeletionImpact.value = it }
                .onFailure { error ->
                    _operationFeedback.value = TaskOperationFeedback(
                        status = OperationStatus.Failed(
                            error.message ?: "Could not calculate deletion impact",
                            error,
                        ),
                    )
                }
        }
    }

    fun clearPermanentDeletionPreview() {
        _taskDeletionImpact.value = null
    }

    fun deleteAllPermanently(taskIds: Set<Long>) {
        val preview = _taskDeletionBatchImpact.value?.takeIf {
            it.requestedTaskIds == taskIds && it.taskIds == taskIds
        }
        deleteAllPermanentlyMutation(taskIds, preview?.revisionTokens, requestId = null)
    }

    fun deleteAllPermanentlyRequest(
        taskIds: Set<Long>,
        expectedRevisionTokens: Map<Long, String>,
        requestId: String,
    ): Boolean = deleteAllPermanentlyMutation(taskIds, expectedRevisionTokens, requestId)

    private fun deleteAllPermanentlyMutation(
        taskIds: Set<Long>,
        expectedRevisionTokens: Map<Long, String>?,
        requestId: String?,
    ): Boolean {
        val uniqueIds = taskIds.filterTo(linkedSetOf()) { it > 0 }
        if (uniqueIds.isEmpty()) return false
        return runAuthoredTaskMutation(
            running = "Deleting ${uniqueIds.size} tasks permanently…",
            success = "${uniqueIds.size} tasks permanently deleted",
            requestId = requestId,
            savedDescription = "permanent Task deletion",
        ) {
            completeCommittedTaskMutation(
                commit = {
                    val revisions = requireNotNull(expectedRevisionTokens)
                    require(revisions.keys == uniqueIds) {
                        "Review the exact deletion impact before deleting these tasks"
                    }
                    val summary = try {
                        app.taskDeletionCoordinator.delete(uniqueIds, revisions)
                    } catch (cancelled: CommittedTaskDeletionBatchCancellation) {
                        clearUndoForCommittedOperation()
                        throw CommittedTaskMutationCancellation(
                            TaskMutationReceipt(
                                TaskMutationKind.BulkPermanentlyDeleted,
                                uniqueIds,
                                deletion = TaskDeletionReceipt(
                                    tasksDeleted = cancelled.summary.tasksDeleted,
                                ),
                                warnings = cancelled.summary.warnings,
                            ),
                            cancelled,
                        )
                    }
                    check(summary.tasksDeleted == uniqueIds.size) {
                        "One or more Tasks no longer exist; review the deletion impact again"
                    }
                    clearUndoForCommittedOperation()
                    TaskMutationReceipt(
                        TaskMutationKind.BulkPermanentlyDeleted,
                        uniqueIds,
                        deletion = TaskDeletionReceipt(
                            tasksDeleted = summary.tasksDeleted,
                        ),
                        warnings = summary.warnings,
                    )
                },
                followUp = { committed -> committed.withDeletedTaskReminderCleanup(reminders) },
            )
        }
    }

    fun previewPermanentDeletions(taskIds: Set<Long>) {
        val uniqueIds = taskIds.filterTo(linkedSetOf()) { it > 0 }
        _taskDeletionBatchImpact.value = null
        if (uniqueIds.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                checkNotNull(app.withUserDataAccess { app.taskDeletionCoordinator.preview(uniqueIds) }) {
                    "Whip data is unavailable while recovery is in progress"
                }
            }
                .onSuccess { _taskDeletionBatchImpact.value = it }
                .onFailure { error ->
                    _operationFeedback.value = TaskOperationFeedback(
                        status = OperationStatus.Failed(
                            error.message ?: "Could not calculate deletion impact",
                            error,
                        ),
                    )
                }
        }
    }

    fun clearPermanentDeletionBatchPreview() {
        _taskDeletionBatchImpact.value = null
    }

    fun reopen(item: ScheduledTask) {
        runOperation(
            "Reopening task…",
            "Task reopened",
            successFeedbackPresentation = OperationFeedbackPresentation.Inline,
        ) {
            repository.reopenIfCurrent(item)
            refreshReminders(listOf(item.task.id))
        }
    }

    fun reopenOccurrence(item: ScheduledTask) {
        runOperation(
            "Reopening occurrence…",
            "Occurrence reopened",
            successFeedbackPresentation = OperationFeedbackPresentation.Inline,
        ) {
            repository.reopenOccurrence(item)
            refreshReminders(listOf(item.task.id))
        }
    }

    fun resetOccurrence(item: ScheduledTask) {
        runOperation("Restoring occurrence schedule…", "Occurrence restored") {
            check(repository.resetOccurrenceIfCurrent(item)) {
                "Task occurrence changed or no longer exists"
            }
            refreshReminders(listOf(item.task.id))
        }
    }

    fun setPinned(taskId: Long, pinned: Boolean) {
        runOperation(
            "Updating Home priority…",
            if (pinned) "Task pinned · first on Whip Home when due" else "Task unpinned from Whip Home",
            successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
        ) {
            repository.setPinned(taskId, pinned)
            if (pinned) {
                try {
                    app.settingsRepository.revealHomeSection(HomeSection.Tasks)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    warn("The Task was pinned, but the Tasks section could not be revealed on Home.")
                }
            }
        }
    }

    fun duplicate(taskId: Long) {
        runOperation("Duplicating task…", "Copy added to Inbox") {
            val duplicateId = repository.duplicate(taskId)
            refreshReminders(listOf(duplicateId))
        }
    }

    fun postponeAll(items: List<ScheduledTask>, newDate: LocalDate) {
        postponeAllMutation(items, newDate, requestId = null)
    }

    fun postponeAllRequest(
        items: List<ScheduledTask>,
        newDate: LocalDate,
        requestId: String,
    ): Boolean = postponeAllMutation(items, newDate, requestId)

    private fun postponeAllMutation(
        items: List<ScheduledTask>,
        newDate: LocalDate,
        requestId: String?,
    ): Boolean {
        val unique = items.distinctBy(ScheduledTask::stableKey)
        if (unique.isEmpty()) return false
        return runAuthoredTaskMutation(
            running = "Postponing ${unique.size} tasks…",
            success = "${unique.size} tasks moved",
            requestId = requestId,
            savedDescription = "Task schedules",
        ) {
            completeCommittedTaskMutation(
                commit = {
                    repository.rescheduleAll(unique, newDate)
                    installUndo(
                        "Bulk move can be undone",
                        TaskUndoAction.RescheduleMany(unique, newDate),
                    )
                    TaskMutationReceipt(
                        kind = TaskMutationKind.BulkRescheduled,
                        taskIds = unique.mapTo(linkedSetOf()) { it.task.id },
                        occurrenceKeys = unique.mapTo(linkedSetOf(), ScheduledTask::stableKey),
                        effectiveDate = newDate,
                    )
                },
                followUp = { committed -> committed.withReminderRefresh(reminders) },
            )
        }
    }

    fun planMyDay(candidates: List<ScheduledTask>, capacityMinutes: Int) {
        val selected = candidates.distinctBy(ScheduledTask::stableKey)
        val plannedDate = clock.today()
        val selectedMinutes = selected.sumOf(ScheduledTask::estimatedDurationMinutes)
        val assumedCount = selected.count { it.task.durationMinutes == null }
        val assumption = if (assumedCount == 0) "" else " · $assumedCount without estimates counted as 30 min"
        runOperation(
            "Planning today…",
            "${selected.size} tasks added to Today · $selectedMinutes min of $capacityMinutes daily capacity$assumption",
            successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
        ) {
            repository.planAll(selected, plannedDate)
            refreshReminders(selected.map { it.task.id })
            offerUndo(
                "Plan My Day can be undone",
                TaskUndoAction.PlanMyDay(selected, plannedDate),
            )
        }
    }

    fun completeAll(items: List<ScheduledTask>) {
        runOperation(
            "Completing ${items.size} tasks…",
            "${items.size} tasks completed",
            successFeedbackPresentation = OperationFeedbackPresentation.Inline,
        ) {
            repository.completeAll(items)
            val completedItems = try {
                items.distinctBy(ScheduledTask::stableKey).map { currentClosedSnapshot(it) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                warn("Undo could not be prepared; the selected Tasks were still completed.")
                emptyList()
            }
            refreshReminders(items.map { it.task.id })
            if (completedItems.isNotEmpty()) {
                offerUndo("Bulk completion can be undone", TaskUndoAction.Complete(completedItems))
            }
        }
    }

    fun archiveAll(items: List<ScheduledTask>) {
        runOperation(
            "Archiving ${items.size} tasks…",
            "${items.size} tasks archived",
            successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
        ) {
            val ids = items.map { it.task.id }.distinct()
            repository.archiveAll(ids)
            refreshReminders(ids)
            offerUndo("Bulk archive can be undone", TaskUndoAction.Restore(items.map { it.task.id }.distinct()))
        }
    }

    fun restoreAll(items: List<ScheduledTask>) {
        runOperation("Restoring ${items.size} tasks…", "${items.size} tasks restored") {
            val ids = items.map { it.task.id }.distinct()
            repository.restoreAll(ids)
            refreshReminders(ids)
        }
    }

    fun reopenAll(items: List<ScheduledTask>) {
        val uniqueItems = items.distinctBy(ScheduledTask::stableKey)
        runOperation("Reopening ${uniqueItems.size} tasks…", "${uniqueItems.size} tasks reopened") {
            repository.reopenAllIfCurrent(uniqueItems)
            refreshReminders(uniqueItems.map { it.task.id })
        }
    }

    fun pinAll(items: List<ScheduledTask>, pinned: Boolean) {
        runOperation(
            "Updating ${items.size} tasks…",
            "${items.size} tasks ${if (pinned) "pinned to" else "unpinned from"} Whip Home",
            successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
        ) {
            repository.setPinnedAll(items.map { it.task.id }, pinned)
            if (pinned) {
                try {
                    app.settingsRepository.revealHomeSection(HomeSection.Tasks)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    warn("The Tasks were pinned, but the Tasks section could not be revealed on Home.")
                }
            }
        }
    }

    fun editAll(items: List<ScheduledTask>, edit: TaskBulkEdit) {
        editAllMutation(items, edit, requestId = null)
    }

    fun editAllRequest(
        items: List<ScheduledTask>,
        edit: TaskBulkEdit,
        requestId: String,
    ): Boolean = editAllMutation(items, edit, requestId)

    private fun editAllMutation(
        items: List<ScheduledTask>,
        edit: TaskBulkEdit,
        requestId: String?,
    ): Boolean {
        val ids = items.map { it.task.id }.distinct()
        if (ids.isEmpty()) return false
        return runAuthoredTaskMutation(
            running = "Updating ${ids.size} tasks…",
            success = "${ids.size} tasks updated",
            requestId = requestId,
            savedDescription = "Task metadata",
        ) {
            completeCommittedTaskMutation(
                commit = {
                    repository.updateMetadataAllIfCurrent(items, edit)
                    clearUndoForCommittedOperation()
                    TaskMutationReceipt(TaskMutationKind.BulkMetadataUpdated, ids.toSet())
                },
                followUp = { committed -> committed.withReminderRefresh(reminders) },
            )
        }
    }

    fun archiveAllRequest(items: List<ScheduledTask>, requestId: String): Boolean {
        val unique = items.distinctBy { it.task.id }
        if (unique.isEmpty()) return false
        return runAuthoredTaskMutation(
            running = "Archiving ${unique.size} tasks…",
            success = "${unique.size} tasks archived",
            requestId = requestId,
            savedDescription = "Task archive",
        ) {
            completeCommittedTaskMutation(
                commit = {
                    repository.archiveAllIfCurrent(unique)
                    installUndo(
                        "Bulk archive can be undone",
                        TaskUndoAction.Restore(unique.map { it.task.id }),
                    )
                    TaskMutationReceipt(TaskMutationKind.BulkArchived, unique.mapTo(linkedSetOf()) { it.task.id })
                },
                followUp = { committed -> committed.withReminderRefresh(reminders) },
            )
        }
    }

    fun reorder(tasks: List<ScheduledTask>) {
        viewModelScope.launch {
            reorderMutex.withLock {
                try {
                    checkNotNull(app.withUserDataAccess {
                        repository.reorderAll(tasks.map { it.task.id })
                        Unit
                    }) { "Whip data is unavailable while recovery is in progress" }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    _operationFeedback.value = TaskOperationFeedback(
                        status = OperationStatus.Failed(error.message ?: "Could not save the new order", error),
                    )
                }
            }
        }
    }

    fun clearPendingUndo(token: Long? = null) {
        if (token != null && token != pendingUndoToken) return
        pendingUndoAction = null
        pendingUndoMessage = null
        pendingUndoToken = null
        pendingQuickAddTaskId = null
        val feedback = _operationFeedback.value
        if (token == null || feedback.undoToken == token) {
            _operationFeedback.value = feedback.copy(
                undoMessage = null,
                undoToken = null,
                quickAddedTaskId = null,
            )
        }
    }

    fun undoLastTaskAction(token: Long) {
        if (token != pendingUndoToken) return
        val action = pendingUndoAction ?: return
        clearPendingUndo(token)
        runOperation("Undoing task action…", "Task action undone") {
            when (action) {
                is TaskUndoAction.Complete -> repository.reopenAllIfCurrent(action.items)
                is TaskUndoAction.ResetOccurrences -> action.items.forEach { item ->
                    check(repository.resetOccurrenceIfCurrent(item)) {
                        "Task occurrence changed and cannot be safely restored"
                    }
                }
                is TaskUndoAction.Reschedule -> repository.restoreSchedulesIfCurrent(
                    listOf(action.item),
                    action.expectedDate,
                )
                is TaskUndoAction.RescheduleMany -> repository.restoreSchedulesIfCurrent(
                    action.items,
                    action.expectedDate,
                )
                is TaskUndoAction.Restore -> action.taskIds.forEach { repository.restore(it) }
                is TaskUndoAction.DeleteCreated -> {
                    val deleted = try {
                        app.taskDeletionCoordinator.delete(action.taskId, action.expectedRevisionToken)
                    } catch (cancelled: CommittedTaskDeletionCancellation) {
                        cancelled.summary
                    }
                    check(deleted.taskDeleted) { "The quick-added Task changed and cannot be safely removed" }
                }
                is TaskUndoAction.PlanMyDay -> {
                    repository.restoreSchedulesIfCurrent(action.items, action.expectedDate)
                }
                is TaskUndoAction.Promote -> try {
                    app.taskDeletionCoordinator.undoPromotion(
                        promotedTaskId = action.promotedTaskId,
                        expectedRevisionToken = action.expectedRevisionToken,
                        sourceTaskId = action.sourceTaskId,
                        sourceStepId = action.sourceStepId,
                        expectedSourceStepUpdatedAtMillis = action.expectedSourceStepUpdatedAtMillis,
                    )
                } catch (cancelled: CommittedTaskDeletionCancellation) {
                    cancelled.summary
                }
            }
            val taskIds = when (action) {
                is TaskUndoAction.Complete -> action.items.map { it.task.id }
                is TaskUndoAction.ResetOccurrences -> action.items.map { it.task.id }
                is TaskUndoAction.Reschedule -> listOf(action.item.task.id)
                is TaskUndoAction.RescheduleMany -> action.items.map { it.task.id }
                is TaskUndoAction.Restore -> action.taskIds
                is TaskUndoAction.DeleteCreated -> listOf(action.taskId)
                is TaskUndoAction.PlanMyDay -> action.items.map { it.task.id }
                is TaskUndoAction.Promote -> listOf(action.promotedTaskId, action.sourceTaskId)
            }.distinct()
            refreshReminders(taskIds)
        }
    }

    private fun installUndo(message: String, action: TaskUndoAction) {
        pendingUndoAction = action
        pendingUndoMessage = message
        pendingUndoToken = ++nextUndoToken
        pendingQuickAddTaskId = (action as? TaskUndoAction.DeleteCreated)?.taskId
    }

    private suspend fun currentClosedSnapshot(item: ScheduledTask): ScheduledTask {
        return if (item.task.scheduleKind == ScheduleKind.Recurring) {
            val originalDate = requireNotNull(item.originalDate)
            val occurrence = repository.getOccurrences(item.task.id)
                .firstOrNull { it.originalDate == originalDate }
                ?: error("The saved Task occurrence could not be reread")
            item.copy(
                scheduledDate = occurrence.scheduledDate,
                completedAtMillis = occurrence.completedAtMillis,
                occurrenceState = occurrence.state,
            )
        } else {
            val current = requireNotNull(repository.getTask(item.task.id)) {
                "The saved Task could not be reread"
            }
            item.copy(task = current, scheduledDate = current.date, completedAtMillis = current.completedAtMillis)
        }
    }

    private fun clearUndoForCommittedOperation() {
        pendingUndoAction = null
        pendingUndoMessage = null
        pendingUndoToken = null
        pendingQuickAddTaskId = null
    }

    private inner class TaskOperationScope {
        var undoMessage: String? = null
            private set
        var undoAction: TaskUndoAction? = null
            private set
        private val warnings = mutableListOf<String>()

        fun offerUndo(message: String, action: TaskUndoAction) {
            undoMessage = message
            undoAction = action
        }

        fun warn(message: String) {
            warnings += message
        }

        fun successMessage(base: String): String =
            if (warnings.isEmpty()) base else "$base · ${warnings.joinToString(" ")}"

        suspend fun refreshReminders(taskIds: Iterable<Long>) {
            taskIds.distinct().sorted().forEach { taskId ->
                try {
                    reminders.syncTask(taskId)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    warn(
                        "Reminder refresh failed for Task $taskId. The Task change is intact; " +
                            "open and save it to retry reminders.",
                    )
                }
            }
        }
    }

    private val reorderMutex = Mutex()
    private val taskMutationMutex = Mutex()

    private fun runAuthoredTaskMutation(
        running: String,
        success: String,
        requestId: String?,
        savedDescription: String,
        block: suspend () -> TaskMutationReceipt,
    ): Boolean {
        if (requestId != null && !_authoredMutationState.tryStartPersistenceRequest(requestId)) return false
        _operationFeedback.value = TaskOperationFeedback(status = OperationStatus.Running(running))
        viewModelScope.launch {
            fun successResult(receipt: TaskMutationReceipt): WhipResult.Success<TaskMutationReceipt> {
                val message = if (receipt.warnings.isEmpty()) success else {
                    "$success · ${receipt.warnings.joinToString(" ")}"
                }
                _operationFeedback.value = TaskOperationFeedback(
                    status = OperationStatus.Succeeded(message, OperationFeedbackPresentation.Snackbar),
                    undoMessage = pendingUndoMessage,
                    undoToken = pendingUndoToken,
                    quickAddedTaskId = pendingQuickAddTaskId,
                )
                return WhipResult.Success(receipt)
            }
            val result = try {
                val receipt = checkNotNull(app.withUserDataAccess { taskMutationMutex.withLock { block() } }) {
                    "Whip data is unavailable while recovery is in progress"
                }
                successResult(receipt)
            } catch (cancelled: CommittedTaskMutationCancellation) {
                if (requestId != null && currentCoroutineContext().isActive) {
                    successResult(
                        cancelled.receipt.copy(
                            warnings = cancelled.receipt.warnings +
                                "Some post-save updates were interrupted; the $savedDescription was saved.",
                        ),
                    )
                } else {
                    if ((_authoredMutationState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                        _authoredMutationState.value = PersistenceRequestState.Idle
                    }
                    _operationFeedback.value = TaskOperationFeedback(status = OperationStatus.Idle)
                    throw cancelled
                }
            } catch (cancelled: CancellationException) {
                if (requestId != null && currentCoroutineContext().isActive) {
                    _operationFeedback.value = TaskOperationFeedback(status = OperationStatus.Idle)
                    WhipResult.Failure(
                        "The $savedDescription save was interrupted. Your changes are still here.",
                        cancelled,
                    )
                } else {
                    if ((_authoredMutationState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                        _authoredMutationState.value = PersistenceRequestState.Idle
                    }
                    _operationFeedback.value = TaskOperationFeedback(status = OperationStatus.Idle)
                    throw cancelled
                }
            } catch (error: Exception) {
                _operationFeedback.value = if (requestId == null) {
                    TaskOperationFeedback(status = OperationStatus.Failed(error.message ?: "Something went wrong", error))
                } else TaskOperationFeedback(status = OperationStatus.Idle)
                WhipResult.Failure(error.message ?: "The $savedDescription could not be saved.", error)
            }
            if (requestId != null &&
                (_authoredMutationState.value as? PersistenceRequestState.Running)?.requestId == requestId
            ) {
                _authoredMutationState.value = PersistenceRequestState.Finished(requestId, result)
            }
        }
        return true
    }

    private fun runEntitySaveOperation(
        runningMessage: String,
        successMessage: String,
        requestId: String?,
        onFinished: (WhipResult<EntitySaveReceipt>) -> Unit,
        block: suspend () -> EntitySaveReceipt,
    ) {
        _operationFeedback.value = TaskOperationFeedback(status = OperationStatus.Running(runningMessage))
        viewModelScope.launch {
            fun successResult(receipt: EntitySaveReceipt): WhipResult.Success<EntitySaveReceipt> {
                val message = if (receipt.warnings.isEmpty()) successMessage else {
                    "$successMessage · ${receipt.warnings.joinToString(" ")}"
                }
                _operationFeedback.value = TaskOperationFeedback(
                    status = OperationStatus.Succeeded(message, OperationFeedbackPresentation.Snackbar),
                )
                return WhipResult.Success(receipt)
            }
            val result = try {
                val receipt = checkNotNull(app.withUserDataAccess { taskMutationMutex.withLock { block() } }) {
                    "Whip data is unavailable while recovery is in progress"
                }
                successResult(receipt)
            } catch (cancelled: CommittedEntitySaveCancellation) {
                if (requestId != null && currentCoroutineContext().isActive) {
                    successResult(
                        cancelled.receipt.copy(
                            warnings = cancelled.receipt.warnings +
                                "Some post-save updates were interrupted; the Task itself was saved.",
                        ),
                    )
                } else {
                    if ((_editorSaveState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                        _editorSaveState.value = PersistenceRequestState.Idle
                    }
                    _operationFeedback.value = TaskOperationFeedback(status = OperationStatus.Idle)
                    throw cancelled
                }
            } catch (cancelled: CancellationException) {
                if (requestId != null && currentCoroutineContext().isActive) {
                    _operationFeedback.value = TaskOperationFeedback(status = OperationStatus.Idle)
                    WhipResult.Failure("The Task save was interrupted. Your changes are still here.", cancelled)
                } else {
                    if ((_editorSaveState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                        _editorSaveState.value = PersistenceRequestState.Idle
                    }
                    _operationFeedback.value = TaskOperationFeedback(status = OperationStatus.Idle)
                    throw cancelled
                }
            } catch (error: Exception) {
                _operationFeedback.value = if (requestId == null) {
                    TaskOperationFeedback(status = OperationStatus.Failed(error.message ?: "Something went wrong", error))
                } else TaskOperationFeedback(status = OperationStatus.Idle)
                WhipResult.Failure(error.message ?: "The task could not be saved.", error)
            }
            runCatching { onFinished(result) }
        }
    }

    private fun runOperation(
        runningMessage: String,
        successMessage: String,
        onFinished: (Boolean) -> Unit = {},
        successFeedbackPresentation: OperationFeedbackPresentation = OperationFeedbackPresentation.Inline,
        block: suspend TaskOperationScope.() -> Unit,
    ) {
        _operationFeedback.value = TaskOperationFeedback(
            status = OperationStatus.Running(runningMessage),
            undoMessage = pendingUndoMessage,
            undoToken = pendingUndoToken,
            quickAddedTaskId = pendingQuickAddTaskId,
        )
        viewModelScope.launch {
            val operation = TaskOperationScope()
            try {
                checkNotNull(app.withUserDataAccess {
                    taskMutationMutex.withLock { operation.block() }
                    Unit
                }) { "Whip data is unavailable while recovery is in progress" }
                val committedUndo = operation.undoAction
                if (committedUndo == null) clearUndoForCommittedOperation()
                else installUndo(requireNotNull(operation.undoMessage), committedUndo)
                _operationFeedback.value = TaskOperationFeedback(
                    status = OperationStatus.Succeeded(
                        operation.successMessage(successMessage),
                        successFeedbackPresentation,
                    ),
                    undoMessage = pendingUndoMessage,
                    undoToken = pendingUndoToken,
                    quickAddedTaskId = pendingQuickAddTaskId,
                )
                runCatching { onFinished(true) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _operationFeedback.value = TaskOperationFeedback(
                    status = OperationStatus.Failed(
                        message = error.message ?: "Something went wrong",
                        cause = error,
                    ),
                    undoMessage = pendingUndoMessage,
                    undoToken = pendingUndoToken,
                    quickAddedTaskId = pendingQuickAddTaskId,
                )
                runCatching { onFinished(false) }
            }
        }
    }
}

private sealed interface TaskUndoAction {
    data class Complete(val items: List<ScheduledTask>) : TaskUndoAction
    data class ResetOccurrences(val items: List<ScheduledTask>) : TaskUndoAction
    data class Reschedule(val item: ScheduledTask, val expectedDate: LocalDate) : TaskUndoAction
    data class RescheduleMany(
        val items: List<ScheduledTask>,
        val expectedDate: LocalDate,
    ) : TaskUndoAction
    data class Restore(val taskIds: List<Long>) : TaskUndoAction
    data class DeleteCreated(val taskId: Long, val expectedRevisionToken: String) : TaskUndoAction
    data class PlanMyDay(
        val items: List<ScheduledTask>,
        val expectedDate: LocalDate,
    ) : TaskUndoAction
    data class Promote(
        val promotedTaskId: Long,
        val expectedRevisionToken: String,
        val sourceTaskId: Long,
        val sourceStepId: Long,
        val expectedSourceStepUpdatedAtMillis: Long,
    ) : TaskUndoAction
}

private suspend fun TaskMutationReceipt.withReminderRefresh(
    scheduler: com.whip.app.reminders.ReminderScheduler,
): TaskMutationReceipt {
    val failures = mutableListOf<Long>()
    taskIds.sorted().forEach { taskId ->
        try {
            scheduler.syncTask(taskId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            failures += taskId
        }
    }
    return if (failures.isEmpty()) this else copy(
        warnings = warnings +
            "Reminder refresh failed for Task ${failures.joinToString()}. The saved change is intact; open and save each listed Task to retry its reminders.",
    )
}

private suspend fun TaskMutationReceipt.withDeletedTaskReminderCleanup(
    scheduler: com.whip.app.reminders.ReminderScheduler,
): TaskMutationReceipt {
    val failures = mutableListOf<Long>()
    taskIds.sorted().forEach { taskId ->
        try {
            scheduler.syncTask(taskId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            failures += taskId
        }
    }
    return if (failures.isEmpty()) this else copy(
        warnings = warnings +
            "Reminder cleanup failed for deleted Task ${failures.joinToString()}. The deletion is intact; Whip will retry during reminder reconciliation.",
    )
}

private data class TaskData(
    val tasks: List<WhipTask>,
    val occurrences: List<TaskOccurrence>,
    val steps: List<TaskStep>,
    val stepStates: List<TaskStepState>,
    val stepSnapshots: List<TaskStepSnapshot>,
)

internal fun buildUiState(
    tasks: List<WhipTask>,
    occurrences: List<TaskOccurrence>,
    steps: List<TaskStep>,
    stepStates: List<TaskStepState>,
    stepSnapshots: List<TaskStepSnapshot>,
    today: LocalDate,
    showAllUpcomingRecurringOccurrences: Boolean,
    zoneId: ZoneId = ZoneId.systemDefault(),
): TaskUiState {
    val stepsByTask = steps.groupBy(TaskStep::taskId)
    val tasksWithSteps = tasks.map { task ->
        task.copy(steps = stepsByTask[task.id].orEmpty().sortedBy(TaskStep::position))
    }
    val recordsByTask = occurrences.groupBy(TaskOccurrence::taskId)
    val statesByTask = stepStates.groupBy(TaskStepState::taskId)
    val snapshotsByTask = stepSnapshots.groupBy(TaskStepSnapshot::taskId)
    val inboxItems = mutableListOf<ScheduledTask>()
    val todayItems = mutableListOf<ScheduledTask>()
    val upcomingItems = mutableListOf<ScheduledTask>()
    val completedItems = mutableListOf<ScheduledTask>()
    val archivedItems = mutableListOf<ScheduledTask>()
    val planningItems = mutableListOf<ScheduledTask>()
    val upcomingThrough = today.plusDays(30)
    val planningThrough = today.plusYears(1)

    fun ScheduledTask.withStepProgress(): ScheduledTask {
        val states = statesByTask[task.id]
            .orEmpty()
            .filter { it.occurrenceKey == occurrenceKey }
            .associateBy(TaskStepState::stepId)
        val occurrenceSnapshots = snapshotsByTask[task.id]
            .orEmpty()
            .filter { it.occurrenceKey == occurrenceKey }
            .sortedBy(TaskStepSnapshot::position)
        if (completedAtMillis != null && occurrenceSnapshots.isNotEmpty()) {
            return copy(
                subtasks = occurrenceSnapshots.map { snapshot ->
                    val definition = task.steps.firstOrNull { it.id == snapshot.stepId }
                        ?: TaskStep(
                            id = snapshot.stepId,
                            taskId = snapshot.taskId,
                            title = snapshot.title,
                            position = snapshot.position,
                            notes = snapshot.notes,
                            archived = true,
                            createdAtMillis = completedAtMillis,
                            updatedAtMillis = completedAtMillis,
                        )
                    ScheduledSubtask(
                        step = definition,
                        completed = snapshot.completed,
                        completedAtMillis = snapshot.completedAtMillis,
                        title = snapshot.title,
                        notes = snapshot.notes,
                    )
                },
            )
        }

        val visibleSteps = if (
            completedAtMillis == null &&
            task.scheduleKind == ScheduleKind.Recurring &&
            task.repeatStepPolicy == RepeatStepPolicy.CarryUnfinished
        ) {
            visibleTaskStepsForOccurrence(
                steps = task.steps,
                snapshots = snapshotsByTask[task.id].orEmpty(),
                occurrenceKey = occurrenceKey,
                policy = task.repeatStepPolicy,
            )
        } else {
            task.steps.filterNot(TaskStep::archived)
        }
        return copy(
            subtasks = visibleSteps.map { step ->
                val state = states[step.id]
                ScheduledSubtask(
                    step = step,
                    completed = state?.completed == true,
                    completedAtMillis = state?.completedAtMillis,
                    title = state?.titleSnapshot ?: step.title,
                    notes = step.notes,
                )
            },
        )
    }

    tasksWithSteps.forEach { task ->
        if (task.archived) {
            archivedItems += ScheduledTask(
                task = task,
                originalDate = task.date,
                scheduledDate = task.date,
                completedAtMillis = task.completedAtMillis,
            ).withStepProgress()
            return@forEach
        }
        if (task.completedAtMillis != null) {
            completedItems += ScheduledTask(
                task = task,
                originalDate = task.date,
                scheduledDate = task.date,
                completedAtMillis = task.completedAtMillis,
            ).withStepProgress()
        }

        val taskRecords = recordsByTask[task.id].orEmpty()
        taskRecords.filter { it.state == OccurrenceState.Completed }.forEach { occurrence ->
            completedItems += ScheduledTask(
                task = task,
                originalDate = occurrence.originalDate,
                scheduledDate = occurrence.scheduledDate,
                completedAtMillis = occurrence.completedAtMillis,
                occurrenceState = occurrence.state,
            ).withStepProgress()
        }

        if (task.completedAtMillis != null) return@forEach

        when (task.scheduleKind) {
            ScheduleKind.Anytime -> {
                // `ScheduleKind.Anytime` is the persisted representation of "no date".
                // Inbox is the single product surface for every undated Task.
                val item = ScheduledTask(task.copy(inbox = true), null, null).withStepProgress()
                inboxItems += item
            }
            ScheduleKind.Once -> {
                val date = requireNotNull(task.date)
                val item = ScheduledTask(
                    task = task,
                    originalDate = date,
                    scheduledDate = date,
                    isPastScheduledDate = date.isBefore(today),
                    isDeadlineOverdue = task.deadline?.isBefore(today) == true,
                ).withStepProgress()
                planningItems += item
                if (date.isAfter(today)) upcomingItems += item else todayItems += item
            }
            ScheduleKind.Recurring -> {
                val rule = requireNotNull(task.recurrence)
                val records = taskRecords.associateBy(TaskOccurrence::originalDate)
                if (rule.anchor == RecurrenceAnchor.Completion) {
                    val closedRecords = taskRecords.filter { it.state in setOf(OccurrenceState.Completed, OccurrenceState.Skipped) }
                    val latest = closedRecords.maxByOrNull { it.completedAtMillis ?: it.scheduledDate.toEpochDay() * 86_400_000L }
                    val closedOn = latest?.completedAtMillis?.let {
                        Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate()
                    } ?: latest?.scheduledDate
                    val original = RecurrenceEngine.nextCompletionRelative(
                        rule = rule,
                        lastCompletedOn = closedOn,
                        completedCount = closedRecords.size,
                    )
                    val openByOriginal = linkedMapOf<LocalDate, ScheduledTask>()
                    if (original != null) {
                        val record = records[original]
                        if (record?.state !in setOf(OccurrenceState.Completed, OccurrenceState.Skipped)) {
                            val scheduled = record?.scheduledDate ?: original
                            openByOriginal[original] = ScheduledTask(
                                task = task,
                                originalDate = original,
                                scheduledDate = scheduled,
                                occurrenceState = record?.state,
                                isPastScheduledDate = scheduled.isBefore(today),
                            ).withStepProgress()
                        }
                    }
                    // Explicit Open rows are authored overrides, including state-only dates
                    // materialized during a cadence/anchor-changing series split. They remain
                    // visible even when the new completion-relative rule would not generate them.
                    taskRecords.filter { it.state == OccurrenceState.Open }.forEach { occurrence ->
                        openByOriginal[occurrence.originalDate] = ScheduledTask(
                            task = task,
                            originalDate = occurrence.originalDate,
                            scheduledDate = occurrence.scheduledDate,
                            occurrenceState = occurrence.state,
                            isPastScheduledDate = occurrence.scheduledDate.isBefore(today),
                        ).withStepProgress()
                    }
                    openByOriginal.values.forEach { item ->
                        val scheduled = requireNotNull(item.scheduledDate)
                        if (scheduled.isAfter(today)) {
                            if (!scheduled.isAfter(planningThrough)) planningItems += item
                            if (!scheduled.isAfter(upcomingThrough)) upcomingItems += item
                        } else {
                            todayItems += item
                            planningItems += item
                        }
                    }
                    return@forEach
                }
                fun dueItem(original: LocalDate): ScheduledTask? {
                    val record = records[original]
                    if (record?.state in setOf(OccurrenceState.Completed, OccurrenceState.Skipped)) return null
                    val scheduled = record?.scheduledDate ?: original
                    if (scheduled.isAfter(today)) return null
                    return ScheduledTask(
                        task = task,
                        originalDate = original,
                        scheduledDate = scheduled,
                        occurrenceState = record?.state,
                        isPastScheduledDate = scheduled.isBefore(today),
                    )
                }
                fun oldestGeneratedDue(): ScheduledTask? {
                    var candidate = RecurrenceEngine.nextOccurrence(rule, rule.startDate, 366_000)
                    while (candidate != null && !candidate.isAfter(today)) {
                        dueItem(candidate)?.let { return it }
                        candidate = RecurrenceEngine.nextOccurrence(rule, candidate.plusDays(1), 366_000)
                    }
                    return null
                }
                fun latestGeneratedDue(): ScheduledTask? {
                    var candidate = RecurrenceEngine.previousOccurrence(rule, today)
                    while (candidate != null) {
                        dueItem(candidate)?.let { return it }
                        candidate = RecurrenceEngine.previousOccurrence(rule, candidate.minusDays(1))
                    }
                    return null
                }
                val recordedDue = taskRecords.asSequence()
                    .filter { it.state == OccurrenceState.Open && !it.scheduledDate.isAfter(today) }
                    .map { occurrence ->
                        ScheduledTask(
                            task = task,
                            originalDate = occurrence.originalDate,
                            scheduledDate = occurrence.scheduledDate,
                            occurrenceState = occurrence.state,
                            isPastScheduledDate = occurrence.scheduledDate.isBefore(today),
                        )
                    }
                    .toList()
                val generatedDue = when (task.missedOccurrencePolicy) {
                    MissedOccurrencePolicy.KeepOldest -> oldestGeneratedDue()
                    MissedOccurrencePolicy.KeepLatest -> latestGeneratedDue()
                    MissedOccurrencePolicy.CurrentOnly -> RecurrenceEngine.nextOccurrence(rule, today, 0)?.let(::dueItem)
                }
                val dueCandidate = (recordedDue + listOfNotNull(generatedDue))
                    .distinctBy(ScheduledTask::stableKey)
                    .let { candidates ->
                        when (task.missedOccurrencePolicy) {
                            MissedOccurrencePolicy.KeepOldest -> candidates.minByOrNull { requireNotNull(it.scheduledDate) }
                            MissedOccurrencePolicy.KeepLatest -> candidates.maxByOrNull { requireNotNull(it.scheduledDate) }
                            MissedOccurrencePolicy.CurrentOnly -> candidates.filter { it.scheduledDate == today }.maxByOrNull { requireNotNull(it.originalDate) }
                        }
                    }
                    ?.withStepProgress()
                if (dueCandidate != null) todayItems += dueCandidate

                val futureByOriginal = linkedMapOf<LocalDate, ScheduledTask>()
                val futureOriginals = if (showAllUpcomingRecurringOccurrences) {
                    RecurrenceEngine.occurrencesBetween(
                        rule = rule,
                        from = maxOf(rule.startDate, today.plusDays(1)),
                        through = planningThrough,
                    )
                } else {
                    var candidate = RecurrenceEngine.nextOccurrence(rule, today.plusDays(1), 366)
                    while (candidate != null && records[candidate]?.state in setOf(OccurrenceState.Completed, OccurrenceState.Skipped)) {
                        candidate = RecurrenceEngine.nextOccurrence(rule, candidate.plusDays(1), 366)
                    }
                    listOfNotNull(candidate)
                }
                futureOriginals.forEach { original ->
                    val record = records[original]
                    if (record?.state !in setOf(OccurrenceState.Completed, OccurrenceState.Skipped)) {
                        val scheduled = record?.scheduledDate ?: original
                        if (scheduled.isAfter(today) && !scheduled.isAfter(planningThrough)) {
                            futureByOriginal[original] = ScheduledTask(
                                task = task,
                                originalDate = original,
                                scheduledDate = scheduled,
                                occurrenceState = record?.state,
                            ).withStepProgress()
                        }
                    }
                }
                taskRecords
                    .filter { occurrence ->
                        occurrence.state == OccurrenceState.Open &&
                            occurrence.scheduledDate.isAfter(today) &&
                            !occurrence.scheduledDate.isAfter(planningThrough)
                    }
                    .forEach { occurrence ->
                        futureByOriginal[occurrence.originalDate] = ScheduledTask(
                            task = task,
                            originalDate = occurrence.originalDate,
                            scheduledDate = occurrence.scheduledDate,
                            occurrenceState = occurrence.state,
                        ).withStepProgress()
                    }
                planningItems += futureByOriginal.values
                upcomingItems += futureByOriginal.values.filter { !requireNotNull(it.scheduledDate).isAfter(upcomingThrough) }
            }
        }
    }

    val sortedUpcoming = upcomingItems.sortedWith(
        compareByDescending<ScheduledTask> { it.task.pinned }
            .thenByDescending { it.task.priority.ordinal }
            .thenBy { it.scheduledDate }
            .thenBy { it.task.title.lowercase() },
    ).withRecurringOccurrenceVisibility(showAllUpcomingRecurringOccurrences)

    return TaskUiState(
        taskEntities = tasksWithSteps,
        inbox = inboxItems.sortedBy { it.task.createdAtMillis },
        today = todayItems.sortedWith(
            compareByDescending<ScheduledTask> { it.task.pinned }
                .thenByDescending { it.task.priority.ordinal }
                .thenByDescending(ScheduledTask::isDeadlineOverdue)
                .thenByDescending(ScheduledTask::isPastScheduledDate)
                .thenBy { it.scheduledDate }
                .thenBy { it.task.createdAtMillis },
        ),
        upcoming = sortedUpcoming,
        completed = completedItems
            .sortedByDescending(ScheduledTask::completedAtMillis),
        archived = archivedItems.sortedByDescending { it.task.updatedAtMillis },
        planning = planningItems.distinctBy(ScheduledTask::stableKey).sortedWith(
            compareBy<ScheduledTask> { it.scheduledDate ?: LocalDate.MAX }
                .thenByDescending { it.task.priority.ordinal }
                .thenBy { it.task.title.lowercase() },
        ),
        occurrences = occurrences.sortedWith(
            compareByDescending<TaskOccurrence> { it.originalDate }
                .thenByDescending { it.scheduledDate },
        ),
        currentDate = today,
        loading = false,
    )
}

internal fun selectTasksForCapacity(
    candidates: List<ScheduledTask>,
    capacityMinutes: Int,
): List<ScheduledTask> {
    var remaining = capacityMinutes.coerceAtLeast(0)
    return candidates
        .filter { it.task.completedAtMillis == null && !it.task.archived }
        .sortedWith(
            compareByDescending<ScheduledTask> { it.task.priority.ordinal }
                .thenBy { it.task.deadline ?: LocalDate.MAX }
                .thenBy { it.estimatedDurationMinutes() }
                .thenBy { it.task.createdAtMillis },
        )
        .filter { item ->
            val duration = item.estimatedDurationMinutes()
            if (duration > remaining) false else {
                remaining -= duration
                true
            }
        }
}

internal fun ScheduledTask.estimatedDurationMinutes(): Int =
    (task.durationMinutes ?: 30).coerceAtLeast(1)

internal fun List<ScheduledTask>.withRecurringOccurrenceVisibility(
    showAllRecurringOccurrences: Boolean,
): List<ScheduledTask> {
    if (showAllRecurringOccurrences) return this
    val nextOccurrenceByTask = asSequence()
        .filter { it.task.scheduleKind == ScheduleKind.Recurring }
        .groupBy { it.task.id }
        .mapValues { (_, occurrences) ->
            occurrences.minWith(
                compareBy<ScheduledTask> { it.scheduledDate ?: LocalDate.MAX }
                    .thenBy { it.originalDate ?: LocalDate.MAX },
            )
        }
    return filter { item ->
        item.task.scheduleKind != ScheduleKind.Recurring ||
            nextOccurrenceByTask[item.task.id]?.stableKey == item.stableKey
    }
}
