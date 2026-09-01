package com.whip.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.whip.app.WhipApplication
import com.whip.app.core.CommittedEntitySaveCancellation
import com.whip.app.core.HomeSection
import com.whip.app.core.EntitySaveReceipt
import com.whip.app.core.OperationFeedbackPresentation
import com.whip.app.core.OperationStatus
import com.whip.app.core.PersistenceRequestState
import com.whip.app.core.WhipResult
import com.whip.app.core.completeCommittedEntitySave
import com.whip.app.core.revealHomeSection
import com.whip.app.core.saveFollowUpWarning
import com.whip.app.core.tryStartPersistenceRequest
import com.whip.app.data.GoalRepository
import com.whip.app.domain.Goal
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalMilestone
import com.whip.app.domain.GoalProjection
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.MetricEntry
import com.whip.app.domain.MetricDefinition
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.projectGoal
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class GoalUiState(
    val active: List<GoalProjection> = emptyList(),
    val completed: List<GoalProjection> = emptyList(),
    val archived: List<GoalProjection> = emptyList(),
    val currentDate: LocalDate = LocalDate.now(),
    val activeZoneId: ZoneId = ZoneId.systemDefault(),
    val nowMillis: Long = System.currentTimeMillis(),
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val customUnits: List<UnitDefinition> = emptyList(),
    val sourceMetrics: List<MetricDefinition> = emptyList(),
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GoalViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as WhipApplication
    private val repository: GoalRepository = app.goalRepository
    private val clock = app.clock
    private val reminders = app.goalReminderScheduler
    private val _operationStatus = MutableStateFlow<OperationStatus>(OperationStatus.Idle)
    val operationStatus: StateFlow<OperationStatus> = _operationStatus.asStateFlow()
    private val _editorSaveState = MutableStateFlow<PersistenceRequestState<EntitySaveReceipt>>(
        PersistenceRequestState.Idle,
    )
    val editorSaveState: StateFlow<PersistenceRequestState<EntitySaveReceipt>> = _editorSaveState.asStateFlow()
    private val reloadKey = MutableStateFlow(0)

    fun defaultSettings() = app.settingsRepository.current()

    init { viewModelScope.launch { runCatching { reminders.syncAll() } } }

    private val goalCore = combine(
        repository.goals,
        repository.milestones,
        repository.metricEntries,
        app.calendarContext,
    ) { goals, milestones, entries, calendar ->
        buildState(goals, milestones, entries, calendar.logicalDate).copy(activeZoneId = calendar.zoneId)
    }

    private val measurementMetadata = combine(
        app.measurementRepository.customUnits,
        app.measurementRepository.metrics,
    ) { units, metrics -> units to metrics }

    val uiState = reloadKey.flatMapLatest {
        combine(goalCore, measurementMetadata, elapsedClockFlow()) { core, metadata, nowMillis ->
            val (units, metrics) = metadata
            core.copy(
                customUnits = units,
                sourceMetrics = metrics.filterNot { it.archived },
                nowMillis = nowMillis,
            )
        }.catch { error ->
            val calendar = app.calendarContext.value
            emit(
                GoalUiState(
                    currentDate = calendar.logicalDate,
                    activeZoneId = calendar.zoneId,
                    nowMillis = clock.now().toEpochMilli(),
                    loading = false,
                    errorMessage = error.message ?: "Could not load goals",
                ),
            )
        }
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            GoalUiState(
                currentDate = app.calendarContext.value.logicalDate,
                activeZoneId = app.calendarContext.value.zoneId,
                nowMillis = clock.now().toEpochMilli(),
            ),
        )

    fun consumeOperationStatus() { _operationStatus.value = OperationStatus.Idle }
    fun consumeEditorSaveResult(requestId: String) {
        if ((_editorSaveState.value as? PersistenceRequestState.Finished)?.requestId == requestId) {
            _editorSaveState.value = PersistenceRequestState.Idle
        }
    }
    fun retryLoading() { reloadKey.value++ }
    fun saveGoal(
        id: Long?,
        draft: GoalDraft,
        requestId: String? = null,
        onFinished: (Boolean) -> Unit = {},
    ): Boolean {
        if (requestId != null && !_editorSaveState.tryStartPersistenceRequest(requestId)) {
            onFinished(false)
            return false
        }
        runEntitySaveOperation(
            if (id == null) "Creating goal…" else "Saving goal…",
            if (id == null) "Goal created" else "Goal saved",
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
                    val savedId = if (id == null) repository.create(draft) else {
                        repository.update(id, draft)
                        id
                    }
                    EntitySaveReceipt(savedId, draft.areaId, areaVerified = false)
                },
                followUp = { committed ->
                    val savedId = requireNotNull(committed.entityId)
                    var resolvedAreaId = committed.areaId
                    var areaVerified = false
                    val warnings = listOfNotNull(
                        saveFollowUpWarning("Saved Area could not be verified; showing All Areas.") {
                            val saved = requireNotNull(repository.get(savedId)) { "Saved Goal could not be reread" }
                            resolvedAreaId = saved.areaId
                            areaVerified = true
                        },
                        saveFollowUpWarning("Some tag suggestions did not refresh. Saving again will retry them.") {
                            draft.tags.forEach { app.measurementRepository.ensureTag(it) }
                        },
                        saveFollowUpWarning("Reminder refresh did not finish. Reopen and save the Goal to retry.") {
                            reminders.syncGoal(savedId)
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
    fun duplicate(id: Long) = runOperation("Duplicating goal…", "Goal duplicated") {
        reminders.syncGoal(repository.duplicate(id))
    }
    fun setStatus(id: Long, status: GoalStatus) = runOperation(
        "Updating goal…",
        "Goal ${status.name.lowercase()}",
    ) { repository.setStatus(id, status); reminders.syncGoal(id) }
    fun deletePermanently(id: Long) = runOperation(
        "Deleting goal…",
        "Goal permanently deleted",
        successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
    ) {
        app.domainDeletionCoordinator.deleteGoal(id)
        reminders.syncGoal(id)
    }
    fun setPinned(id: Long, pinned: Boolean) = runOperation(
        "Updating Home summary…",
        if (pinned) "Goal pinned to Whip Home" else "Goal unpinned from Whip Home",
        successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
    ) {
        repository.setPinned(id, pinned)
        if (pinned) app.settingsRepository.revealHomeSection(HomeSection.Goals)
    }
    fun reorder(ids: List<Long>) = runSilentReorder { repository.reorder(ids) }
    fun record(id: Long, value: Double, date: LocalDate?, note: String) = runOperation("Saving progress…", "Progress saved") { repository.recordMeasurement(id, value, date = date, note = note) }
    fun updateMeasurement(id: Long, entryId: String, value: Double, date: LocalDate, note: String) =
        runOperation("Updating progress…", "Progress updated") { repository.updateMeasurement(id, entryId, value, date, note) }
    fun deleteMeasurement(id: Long, entryId: String) =
        runOperation(
            "Removing progress update…",
            "Progress update removed",
        ) { repository.deleteMeasurement(id, entryId) }
    fun toggleMilestone(id: Long, completed: Boolean) = runOperation(
        "Updating milestone…",
        "Milestone updated",
        successFeedbackPresentation = OperationFeedbackPresentation.Inline,
    ) { repository.toggleMilestone(id, completed) }
    fun resetElapsedStart(id: Long, start: Instant) = runOperation("Resetting timer…", "Timer reset") {
        repository.resetElapsedStart(id, start)
        reminders.syncGoal(id)
    }
    fun resetElapsedStartToNow(id: Long) = resetElapsedStart(id, clock.now())

    private fun elapsedClockFlow(): Flow<Long> = flow {
        while (currentCoroutineContext().isActive) {
            emit(clock.now().toEpochMilli())
            delay(30_000L)
        }
    }
    private val reorderMutex = Mutex()

    private fun runSilentReorder(block: suspend () -> Unit) {
        viewModelScope.launch {
            reorderMutex.withLock {
                try {
                    checkNotNull(app.withUserDataAccess {
                        block()
                        Unit
                    }) { "Whip data is unavailable while recovery is in progress" }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    _operationStatus.value = OperationStatus.Failed(error.message ?: "Could not save the new order", error)
                }
            }
        }
    }

    private fun runOperation(
        running: String,
        success: String,
        onFinished: (Boolean) -> Unit = {},
        successFeedbackPresentation: OperationFeedbackPresentation = OperationFeedbackPresentation.Inline,
        block: suspend () -> Unit,
    ) {
        _operationStatus.value = OperationStatus.Running(running)
        viewModelScope.launch {
            try {
                checkNotNull(app.withUserDataAccess {
                    block()
                    Unit
                }) { "Whip data is unavailable while recovery is in progress" }
                _operationStatus.value = OperationStatus.Succeeded(success, successFeedbackPresentation)
                runCatching { onFinished(true) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _operationStatus.value = OperationStatus.Failed(error.message ?: "Something went wrong", error)
                runCatching { onFinished(false) }
            }
        }
    }

    private fun runEntitySaveOperation(
        running: String,
        success: String,
        requestId: String?,
        onFinished: (WhipResult<EntitySaveReceipt>) -> Unit,
        block: suspend () -> EntitySaveReceipt,
    ) {
        _operationStatus.value = OperationStatus.Running(running)
        viewModelScope.launch {
            fun successResult(receipt: EntitySaveReceipt): WhipResult.Success<EntitySaveReceipt> {
                val message = if (receipt.warnings.isEmpty()) success else "$success · ${receipt.warnings.joinToString(" ")}"
                _operationStatus.value = OperationStatus.Succeeded(message, OperationFeedbackPresentation.Snackbar)
                return WhipResult.Success(receipt)
            }
            val result = try {
                val receipt = checkNotNull(app.withUserDataAccess { block() }) {
                    "Whip data is unavailable while recovery is in progress"
                }
                successResult(receipt)
            } catch (cancelled: CommittedEntitySaveCancellation) {
                if (requestId != null && currentCoroutineContext().isActive) {
                    successResult(
                        cancelled.receipt.copy(
                            warnings = cancelled.receipt.warnings +
                                "Some post-save updates were interrupted; the Goal itself was saved.",
                        ),
                    )
                } else {
                    if ((_editorSaveState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                        _editorSaveState.value = PersistenceRequestState.Idle
                    }
                    _operationStatus.value = OperationStatus.Idle
                    throw cancelled
                }
            } catch (cancelled: CancellationException) {
                if (requestId != null && currentCoroutineContext().isActive) {
                    _operationStatus.value = OperationStatus.Idle
                    WhipResult.Failure("The Goal save was interrupted. Your changes are still here.", cancelled)
                } else {
                    if ((_editorSaveState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                        _editorSaveState.value = PersistenceRequestState.Idle
                    }
                    _operationStatus.value = OperationStatus.Idle
                    throw cancelled
                }
            } catch (error: Exception) {
                _operationStatus.value = if (requestId == null) {
                    OperationStatus.Failed(error.message ?: "Something went wrong", error)
                } else OperationStatus.Idle
                WhipResult.Failure(error.message ?: "The goal could not be saved.", error)
            }
            runCatching { onFinished(result) }
        }
    }

}

private fun buildState(
    goals: List<Goal>,
    milestones: List<GoalMilestone>,
    entries: List<MetricEntry>,
    today: LocalDate,
): GoalUiState {
    val projections = goals.map { goal ->
        projectGoal(
            goal,
            entries.filter { it.metricId == goal.metricId },
            milestones.filter { it.goalId == goal.id },
            today,
        )
    }
    return GoalUiState(
        active = projections.filter { it.goal.status in setOf(GoalStatus.Active, GoalStatus.Paused) },
        completed = projections.filter { it.goal.status in setOf(GoalStatus.Completed, GoalStatus.Abandoned) },
        archived = projections.filter { it.goal.status == GoalStatus.Archived },
        currentDate = today,
        loading = false,
    )
}
