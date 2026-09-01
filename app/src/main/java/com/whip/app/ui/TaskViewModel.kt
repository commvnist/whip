package com.whip.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.whip.app.WhipApplication
import com.whip.app.core.HomeSection
import com.whip.app.core.OperationFeedbackPresentation
import com.whip.app.core.OperationStatus
import com.whip.app.core.WhipClock
import com.whip.app.core.revealHomeSection
import com.whip.app.core.zoneId
import com.whip.app.core.currentDateFlow
import com.whip.app.data.TaskRepository
import com.whip.app.data.TaskDeletionBatchImpact
import com.whip.app.data.TaskDeletionImpact
import com.whip.app.data.TaskBulkEdit
import com.whip.app.domain.OccurrenceState
import com.whip.app.domain.MissedOccurrencePolicy
import com.whip.app.domain.RecurrenceEngine
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.ScheduledSubtask
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskOccurrence
import com.whip.app.domain.TaskStep
import com.whip.app.domain.TaskStepState
import com.whip.app.domain.TaskStepSnapshot
import com.whip.app.domain.WhipTask
import com.whip.app.domain.RepeatStepPolicy
import com.whip.app.domain.RecurrenceAnchor
import com.whip.app.domain.visibleTaskStepsForOccurrence
import com.whip.app.reminders.reminderDefinitionChanged
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
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

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as WhipApplication
    private val repository: TaskRepository = app.taskRepository
    private val reminders = app.reminderScheduler
    private val clock = app.clock

    private val _operationFeedback = MutableStateFlow(TaskOperationFeedback())
    val operationFeedback: StateFlow<TaskOperationFeedback> = _operationFeedback.asStateFlow()
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
            app.settingsRepository.currentDateFlow(clock),
            app.settingsRepository.settings,
        ) { data, today, settings ->
            buildUiState(
                tasks = data.tasks,
                occurrences = data.occurrences,
                steps = data.steps,
                stepStates = data.stepStates,
                stepSnapshots = data.stepSnapshots,
                today = today,
                showAllUpcomingRecurringOccurrences = settings.showAllUpcomingTaskOccurrences,
                zoneId = settings.zoneId(),
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
            app.settingsRepository.currentDateFlow(clock).collect { reminders.syncAll() }
        }
        viewModelScope.launch {
            app.userDataGeneration.drop(1).collect {
                clearPendingUndo()
                _taskDeletionImpact.value = null
                _taskDeletionBatchImpact.value = null
                _operationFeedback.value = TaskOperationFeedback()
            }
        }
    }

    fun consumeOperationStatus() {
        _operationFeedback.value = _operationFeedback.value.copy(status = OperationStatus.Idle)
    }

    fun retryLoading() { reloadKey.value++ }

    fun saveTask(
        taskId: Long?,
        draft: TaskDraft,
        fromOccurrence: LocalDate? = null,
        onFinished: (Boolean) -> Unit = {},
    ) {
        runOperation(
            runningMessage = if (taskId == null) "Creating task…" else "Saving task…",
            successMessage = when {
                taskId == null -> "Task created"
                fromOccurrence != null -> "Future series created · earlier history preserved"
                else -> "Task saved"
            },
            onFinished = onFinished,
        ) {
            val existing = taskId?.let { repository.getTask(it) }
            val savedId = if (taskId == null) {
                repository.create(draft)
            } else {
                repository.update(taskId, draft, fromOccurrence)
            }
            if (existing == null || existing.reminderDefinitionChanged(draft) || savedId != taskId) {
                reminders.syncTask(savedId)
            }
            if (taskId != null && savedId != taskId) {
                reminders.syncTask(taskId)
            }
            draft.tags.filter { tag -> existing?.tags?.none { it.equals(tag, ignoreCase = true) } != false }
                .forEach { app.measurementRepository.ensureTag(it) }
        }
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
            reminders.syncTask(taskId)
            offerUndo("Quick Add can be undone", TaskUndoAction.DeleteCreated(taskId))
        }
    }

    fun complete(item: ScheduledTask) {
        runOperation(
            "Completing task…",
            "Task completed",
            successFeedbackPresentation = OperationFeedbackPresentation.Inline,
        ) {
            repository.complete(item)
            reminders.syncTask(item.task.id)
            offerUndo("Completion can be undone", TaskUndoAction.Complete(listOf(item)))
        }
    }

    fun skip(item: ScheduledTask) {
        runOperation(
            "Skipping occurrence…",
            "Occurrence skipped",
            successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
        ) {
            repository.skip(item)
            reminders.syncTask(item.task.id)
            offerUndo("Skip can be undone", TaskUndoAction.ResetOccurrences(listOf(item)))
        }
    }

    fun reschedule(item: ScheduledTask, newDate: LocalDate) {
        runOperation(
            "Moving task…",
            "Task moved",
            successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
        ) {
            repository.reschedule(item, newDate)
            reminders.syncTask(item.task.id)
            offerUndo("Move can be undone", TaskUndoAction.Reschedule(item))
        }
    }

    fun setStepCompleted(item: ScheduledTask, stepId: Long, completed: Boolean) {
        runOperation(
            runningMessage = "Updating subtask…",
            successMessage = if (completed) "Subtask completed" else "Subtask reopened",
            successFeedbackPresentation = OperationFeedbackPresentation.Inline,
        ) {
            repository.setStepCompleted(item, stepId, completed)
            reminders.syncTask(item.task.id)
        }
    }

    fun promoteStep(item: ScheduledTask, stepId: Long) {
        runOperation(
            "Moving subtask…",
            "Subtask moved to a new Inbox task",
            successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
        ) {
            val promotedTaskId = repository.promoteStep(item, stepId)
            offerUndo(
                "Move to a new Task can be undone",
                TaskUndoAction.Promote(promotedTaskId, item.task.id, stepId),
            )
        }
    }

    fun archive(taskId: Long) {
        runOperation(
            "Archiving task…",
            "Task archived",
            successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
        ) {
            repository.archive(taskId)
            reminders.syncTask(taskId)
            offerUndo("Archive can be undone", TaskUndoAction.Restore(listOf(taskId)))
        }
    }

    fun restore(taskId: Long) {
        runOperation("Restoring task…", "Task restored") {
            repository.restore(taskId)
            reminders.syncTask(taskId)
        }
    }

    fun deletePermanently(taskId: Long) {
        runOperation(
            "Deleting task permanently…",
            "Task permanently deleted",
            successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
        ) {
            val revision = _taskDeletionImpact.value?.takeIf { it.taskId == taskId }?.revisionToken
            app.taskDeletionCoordinator.delete(taskId, revision)
            _taskDeletionImpact.value = null
            // syncTask cancels every scheduled reminder carrying this task's tag before it
            // observes that the task no longer exists.
            reminders.syncTask(taskId)
        }
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
        val uniqueIds = taskIds.filterTo(linkedSetOf()) { it > 0 }
        if (uniqueIds.isEmpty()) return
        runOperation(
            "Deleting ${uniqueIds.size} tasks permanently…",
            "${uniqueIds.size} tasks permanently deleted",
            successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
        ) {
            val preview = checkNotNull(
                _taskDeletionBatchImpact.value?.takeIf {
                    it.requestedTaskIds == uniqueIds && it.taskIds == uniqueIds
                },
            ) { "Review the exact deletion impact before deleting these tasks" }
            app.taskDeletionCoordinator.delete(uniqueIds, preview.revisionTokens)
            _taskDeletionBatchImpact.value = null
            uniqueIds.forEach { reminders.syncTask(it) }
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

    fun reopen(taskId: Long) {
        runOperation(
            "Reopening task…",
            "Task reopened",
            successFeedbackPresentation = OperationFeedbackPresentation.Inline,
        ) {
            repository.reopen(taskId)
            reminders.syncTask(taskId)
        }
    }

    fun reopenOccurrence(item: ScheduledTask) {
        runOperation(
            "Reopening occurrence…",
            "Occurrence reopened",
            successFeedbackPresentation = OperationFeedbackPresentation.Inline,
        ) {
            repository.reopenOccurrence(item)
            reminders.syncTask(item.task.id)
        }
    }

    fun resetOccurrence(taskId: Long, originalDate: LocalDate) {
        runOperation("Restoring occurrence schedule…", "Occurrence restored") {
            repository.resetOccurrence(taskId, originalDate)
            reminders.syncTask(taskId)
        }
    }

    fun setPinned(taskId: Long, pinned: Boolean) {
        runOperation(
            "Updating Home priority…",
            if (pinned) "Task pinned · first on Whip Home when due" else "Task unpinned from Whip Home",
            successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
        ) {
            repository.setPinned(taskId, pinned)
            if (pinned) app.settingsRepository.revealHomeSection(HomeSection.Tasks)
        }
    }

    fun duplicate(taskId: Long) {
        runOperation("Duplicating task…", "Copy added to Inbox") { repository.duplicate(taskId) }
    }

    fun postponeAll(items: List<ScheduledTask>, newDate: LocalDate) {
        runOperation(
            "Postponing ${items.size} tasks…",
            "${items.size} tasks moved",
            successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
        ) {
            val unique = items.distinctBy(ScheduledTask::stableKey)
            repository.rescheduleAll(unique, newDate)
            unique.map { it.task.id }.distinct().forEach { reminders.syncTask(it) }
            offerUndo("Bulk move can be undone", TaskUndoAction.RescheduleMany(unique))
        }
    }

    fun planMyDay(candidates: List<ScheduledTask>, capacityMinutes: Int) {
        val selected = candidates.distinctBy(ScheduledTask::stableKey)
        val selectedMinutes = selected.sumOf(ScheduledTask::estimatedDurationMinutes)
        val assumedCount = selected.count { it.task.durationMinutes == null }
        val assumption = if (assumedCount == 0) "" else " · $assumedCount without estimates counted as 30 min"
        runOperation(
            "Planning today…",
            "${selected.size} tasks added to Today · $selectedMinutes min of $capacityMinutes daily capacity$assumption",
            successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
        ) {
            repository.planAll(selected, clock.today())
            selected.map { it.task.id }.distinct().forEach { reminders.syncTask(it) }
            offerUndo(
                "Plan My Day can be undone",
                TaskUndoAction.PlanMyDay(selected),
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
            items.map { it.task.id }.distinct().forEach { taskId ->
                reminders.syncTask(taskId)
            }
            offerUndo("Bulk completion can be undone", TaskUndoAction.Complete(items.distinctBy(ScheduledTask::stableKey)))
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
            ids.forEach { taskId ->
                reminders.syncTask(taskId)
            }
            offerUndo("Bulk archive can be undone", TaskUndoAction.Restore(items.map { it.task.id }.distinct()))
        }
    }

    fun restoreAll(items: List<ScheduledTask>) {
        runOperation("Restoring ${items.size} tasks…", "${items.size} tasks restored") {
            val ids = items.map { it.task.id }.distinct()
            repository.restoreAll(ids)
            ids.forEach { taskId ->
                reminders.syncTask(taskId)
            }
        }
    }

    fun reopenAll(items: List<ScheduledTask>) {
        val uniqueItems = items.distinctBy(ScheduledTask::stableKey)
        runOperation("Reopening ${uniqueItems.size} tasks…", "${uniqueItems.size} tasks reopened") {
            uniqueItems.forEach { item ->
                if (item.task.scheduleKind == ScheduleKind.Recurring) repository.reopenOccurrence(item)
                else repository.reopen(item.task.id)
            }
            uniqueItems.map { it.task.id }.distinct().forEach { reminders.syncTask(it) }
        }
    }

    fun pinAll(items: List<ScheduledTask>, pinned: Boolean) {
        runOperation(
            "Updating ${items.size} tasks…",
            "${items.size} tasks ${if (pinned) "pinned to" else "unpinned from"} Whip Home",
            successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
        ) {
            repository.setPinnedAll(items.map { it.task.id }, pinned)
            if (pinned) app.settingsRepository.revealHomeSection(HomeSection.Tasks)
        }
    }

    fun editAll(items: List<ScheduledTask>, edit: TaskBulkEdit) {
        val ids = items.map { it.task.id }.distinct()
        runOperation("Updating ${ids.size} tasks…", "${ids.size} tasks updated") {
            repository.updateMetadataAll(ids, edit)
            ids.forEach { taskId ->
                reminders.syncTask(taskId)
            }
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
                is TaskUndoAction.Complete -> action.items.forEach { item ->
                    if (item.task.scheduleKind == ScheduleKind.Recurring) repository.reopenOccurrence(item)
                    else repository.reopen(item.task.id)
                }
                is TaskUndoAction.ResetOccurrences -> action.items.forEach { item ->
                    repository.resetOccurrence(item.task.id, requireNotNull(item.originalDate))
                }
                is TaskUndoAction.Reschedule -> repository.restoreSchedules(listOf(action.item))
                is TaskUndoAction.RescheduleMany -> repository.restoreSchedules(action.items)
                is TaskUndoAction.Restore -> action.taskIds.forEach { repository.restore(it) }
                is TaskUndoAction.DeleteCreated -> repository.deletePermanently(action.taskId)
                is TaskUndoAction.PlanMyDay -> {
                    repository.restorePlan(action.items)
                }
                is TaskUndoAction.Promote -> repository.undoPromoteStep(
                    action.promotedTaskId,
                    action.sourceTaskId,
                    action.sourceStepId,
                )
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
            taskIds.forEach { id -> reminders.syncTask(id) }
        }
    }

    private fun offerUndo(message: String, action: TaskUndoAction) {
        pendingUndoAction = action
        pendingUndoMessage = message
        pendingUndoToken = ++nextUndoToken
        pendingQuickAddTaskId = (action as? TaskUndoAction.DeleteCreated)?.taskId
    }

    private val reorderMutex = Mutex()

    private fun runOperation(
        runningMessage: String,
        successMessage: String,
        onFinished: (Boolean) -> Unit = {},
        successFeedbackPresentation: OperationFeedbackPresentation = OperationFeedbackPresentation.Inline,
        block: suspend () -> Unit,
    ) {
        pendingUndoAction = null
        pendingUndoMessage = null
        pendingUndoToken = null
        pendingQuickAddTaskId = null
        _operationFeedback.value = TaskOperationFeedback(
            status = OperationStatus.Running(runningMessage),
        )
        viewModelScope.launch {
            try {
                checkNotNull(app.withUserDataAccess {
                    block()
                    Unit
                }) { "Whip data is unavailable while recovery is in progress" }
                _operationFeedback.value = TaskOperationFeedback(
                    status = OperationStatus.Succeeded(successMessage, successFeedbackPresentation),
                    undoMessage = pendingUndoMessage,
                    undoToken = pendingUndoToken,
                    quickAddedTaskId = pendingQuickAddTaskId,
                )
                runCatching { onFinished(true) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                pendingUndoAction = null
                pendingUndoMessage = null
                pendingQuickAddTaskId = null
                _operationFeedback.value = TaskOperationFeedback(
                    status = OperationStatus.Failed(
                        message = error.message ?: "Something went wrong",
                        cause = error,
                    ),
                )
                runCatching { onFinished(false) }
            }
        }
    }
}

private sealed interface TaskUndoAction {
    data class Complete(val items: List<ScheduledTask>) : TaskUndoAction
    data class ResetOccurrences(val items: List<ScheduledTask>) : TaskUndoAction
    data class Reschedule(val item: ScheduledTask) : TaskUndoAction
    data class RescheduleMany(val items: List<ScheduledTask>) : TaskUndoAction
    data class Restore(val taskIds: List<Long>) : TaskUndoAction
    data class DeleteCreated(val taskId: Long) : TaskUndoAction
    data class PlanMyDay(
        val items: List<ScheduledTask>,
    ) : TaskUndoAction
    data class Promote(
        val promotedTaskId: Long,
        val sourceTaskId: Long,
        val sourceStepId: Long,
    ) : TaskUndoAction
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
            ).withStepProgress()
        }

        if (task.completedAtMillis != null) return@forEach

        when (task.scheduleKind) {
            ScheduleKind.Anytime -> {
                // `ScheduleKind.Anytime` remains the persisted, backward-compatible
                // representation of "no date". Inbox is now the single product surface
                // for every undated Task, including records created before consolidation.
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
                    if (original != null) {
                        val record = records[original]
                        if (record?.state !in setOf(OccurrenceState.Completed, OccurrenceState.Skipped)) {
                            val scheduled = record?.scheduledDate ?: original
                            val item = ScheduledTask(
                                task = task,
                                originalDate = original,
                                scheduledDate = scheduled,
                                isPastScheduledDate = scheduled.isBefore(today),
                            ).withStepProgress()
                            if (scheduled.isAfter(today)) {
                                if (!scheduled.isAfter(planningThrough)) planningItems += item
                                if (!scheduled.isAfter(upcomingThrough)) upcomingItems += item
                            } else {
                                todayItems += item
                                planningItems += item
                            }
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
