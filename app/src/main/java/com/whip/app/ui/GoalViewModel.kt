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
import com.whip.app.core.completeCommittedPersistence
import com.whip.app.core.revealHomeSection
import com.whip.app.core.saveFollowUpWarning
import com.whip.app.core.tryStartPersistenceRequest
import com.whip.app.data.GoalRepository
import com.whip.app.data.GoalDeletionImpact
import com.whip.app.data.GoalDeletionSummary
import com.whip.app.data.CommittedGoalDeletionCancellation
import com.whip.app.domain.Goal
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalClosureSnapshot
import com.whip.app.domain.GoalElapsedResetEvent
import com.whip.app.domain.GoalMeasurementBoundary
import com.whip.app.domain.GoalMilestoneBoundary
import com.whip.app.domain.GoalMutationBoundary
import com.whip.app.domain.GoalProgressBoundary
import com.whip.app.domain.GoalMilestone
import com.whip.app.domain.GoalProjection
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.MetricEntry
import com.whip.app.domain.MetricDefinition
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.projectGoal
import com.whip.app.reminders.GoalReminderScheduler
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
import kotlinx.coroutines.flow.drop
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

internal enum class GoalMutationKind {
    Duplicated,
    ProgressRecorded,
    ProgressUpdated,
    ProgressDeleted,
    ElapsedOriginReset,
    LifecycleChanged,
    ArchiveChanged,
    PermanentlyDeleted,
}

internal data class GoalMutationReceipt(
    val kind: GoalMutationKind,
    val goalId: Long,
    val createdGoalId: Long? = null,
    val measurementEntryId: String? = null,
    val deletion: GoalDeletionSummary? = null,
    val warnings: List<String> = emptyList(),
)

internal class CommittedGoalMutationCancellation(
    val receipt: GoalMutationReceipt,
    cause: CancellationException,
) : CancellationException(cause.message) {
    init { initCause(cause) }
}

internal suspend fun completeCommittedGoalMutation(
    commit: suspend () -> GoalMutationReceipt,
    followUp: suspend (GoalMutationReceipt) -> GoalMutationReceipt,
): GoalMutationReceipt = completeCommittedPersistence(
    commit = commit,
    followUp = followUp,
    onCancellation = { committed, cancelled -> CommittedGoalMutationCancellation(committed, cancelled) },
    onOrdinaryFailure = { committed ->
        committed.copy(
            warnings = committed.warnings +
                "Some post-save updates did not finish; the Goal change itself was saved.",
        )
    },
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
    private val _authoredMutationState = MutableStateFlow<PersistenceRequestState<GoalMutationReceipt>>(
        PersistenceRequestState.Idle,
    )
    internal val authoredMutationState: StateFlow<PersistenceRequestState<GoalMutationReceipt>> =
        _authoredMutationState.asStateFlow()
    private val _goalDeletionImpact = MutableStateFlow<GoalDeletionImpact?>(null)
    val goalDeletionImpact: StateFlow<GoalDeletionImpact?> = _goalDeletionImpact.asStateFlow()
    private var deletionPreviewGeneration = 0L
    private val reloadKey = MutableStateFlow(0)

    fun defaultSettings() = app.settingsRepository.current()

    init {
        viewModelScope.launch { runCatching { reminders.syncAll() } }
        viewModelScope.launch {
            app.userDataGeneration.drop(1).collect {
                _editorSaveState.value = PersistenceRequestState.Idle
                _authoredMutationState.value = PersistenceRequestState.Idle
                _goalDeletionImpact.value = null
                _operationStatus.value = OperationStatus.Idle
            }
        }
    }

    private val goalHistory = combine(
        repository.closureSnapshots,
        repository.elapsedResetEvents,
        ::Pair,
    )

    private val goalCore = combine(
        repository.goals,
        repository.milestones,
        repository.metricEntries,
        app.calendarContext,
        goalHistory,
    ) { goals, milestones, entries, calendar, history ->
        buildState(
            goals,
            milestones,
            entries,
            history.first,
            history.second,
            calendar.logicalDate,
        ).copy(activeZoneId = calendar.zoneId)
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
    fun reportUnavailable(message: String) {
        _operationStatus.value = OperationStatus.Failed(message)
    }
    fun consumeEditorSaveResult(requestId: String) {
        if ((_editorSaveState.value as? PersistenceRequestState.Finished)?.requestId == requestId) {
            _editorSaveState.value = PersistenceRequestState.Idle
        }
    }
    fun consumeAuthoredMutationResult(requestId: String) {
        val finished = _authoredMutationState.value as? PersistenceRequestState.Finished
        if (finished?.requestId == requestId) {
            val receipt = (finished.result as? WhipResult.Success)?.value
            if (receipt?.kind == GoalMutationKind.PermanentlyDeleted) _goalDeletionImpact.value = null
            _authoredMutationState.value = PersistenceRequestState.Idle
        }
    }
    fun retryLoading() { reloadKey.value++ }
    fun preparePermanentDeletion(id: Long) {
        val generation = ++deletionPreviewGeneration
        _goalDeletionImpact.value = null
        _operationStatus.value = OperationStatus.Running("Reviewing deletion impact…")
        viewModelScope.launch {
            try {
                val impact = checkNotNull(app.withUserDataAccess {
                    app.domainDeletionCoordinator.previewGoalDeletion(id)
                }) { "Whip data is unavailable while recovery is in progress" }
                require(impact.exists) { "Goal no longer exists" }
                if (deletionPreviewGeneration == generation) {
                    _goalDeletionImpact.value = impact
                    _operationStatus.value = OperationStatus.Idle
                }
            } catch (cancelled: CancellationException) {
                if (deletionPreviewGeneration == generation) _operationStatus.value = OperationStatus.Idle
                throw cancelled
            } catch (error: Exception) {
                if (deletionPreviewGeneration == generation) {
                    _operationStatus.value = OperationStatus.Failed(
                        error.message ?: "Could not review the deletion impact",
                        error,
                    )
                }
            }
        }
    }
    fun clearPermanentDeletionPreview() {
        deletionPreviewGeneration++
        _goalDeletionImpact.value = null
    }
    fun saveGoal(
        id: Long?,
        draft: GoalDraft,
        expectedBoundary: GoalMutationBoundary? = null,
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
                        repository.update(
                            requireNotNull(expectedBoundary) {
                                "Reopen the Goal before saving changes"
                            },
                            draft,
                        )
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
    fun duplicate(
        boundary: GoalMutationBoundary,
        requestId: String? = null,
    ): Boolean = runAuthoredGoalMutation(
        running = "Duplicating goal…",
        success = "Goal duplicated",
        requestId = requestId,
        savedDescription = "duplicate",
    ) {
        completeCommittedGoalMutation(
            commit = {
                val createdId = repository.duplicate(boundary)
                GoalMutationReceipt(GoalMutationKind.Duplicated, boundary.goalId, createdGoalId = createdId)
            },
            followUp = { committed -> committed.withReminderRefresh(reminders, requireNotNull(committed.createdGoalId)) },
        )
    }

    fun setStatus(
        boundary: GoalMutationBoundary,
        status: GoalStatus,
        requestId: String? = null,
    ): Boolean = runAuthoredGoalMutation(
        running = "Updating goal…",
        success = "Goal ${status.name.lowercase()}",
        requestId = requestId,
        savedDescription = "lifecycle change",
    ) {
        completeCommittedGoalMutation(
            commit = {
                repository.setStatus(boundary, status)
                GoalMutationReceipt(GoalMutationKind.LifecycleChanged, boundary.goalId)
            },
            followUp = { committed -> committed.withReminderRefresh(reminders, boundary.goalId) },
        )
    }

    fun setArchived(
        boundary: GoalMutationBoundary,
        archived: Boolean,
        requestId: String? = null,
    ): Boolean = runAuthoredGoalMutation(
        running = if (archived) "Archiving goal…" else "Restoring goal…",
        success = if (archived) "Goal archived" else "Goal restored",
        requestId = requestId,
        savedDescription = if (archived) "archive change" else "restore change",
    ) {
        completeCommittedGoalMutation(
            commit = {
                repository.setArchived(boundary, archived)
                GoalMutationReceipt(GoalMutationKind.ArchiveChanged, boundary.goalId)
            },
            followUp = { committed -> committed.withReminderRefresh(reminders, boundary.goalId) },
        )
    }

    /** Legacy dispatchers remain fail-closed until their caller supplies a reviewed boundary. */
    fun duplicate(id: Long) = runOperation("Could not duplicate goal", "") {
        error("Reopen the Goal before duplicating it")
    }
    fun setStatus(id: Long, status: GoalStatus) = runOperation("Could not update goal", "") {
        error("Reopen the Goal before changing its status")
    }
    fun deletePermanently(
        id: Long,
        expectedRevisionToken: String,
        requestId: String? = null,
    ): Boolean = runAuthoredGoalMutation(
        running = "Deleting goal…",
        success = "Goal permanently deleted",
        requestId = requestId,
        savedDescription = "permanent deletion",
    ) {
        try {
            val summary = app.domainDeletionCoordinator.deleteGoal(id, expectedRevisionToken)
            require(summary.goalDeleted) { "Goal no longer exists" }
            GoalMutationReceipt(
                kind = GoalMutationKind.PermanentlyDeleted,
                goalId = id,
                deletion = summary,
                warnings = summary.warnings,
            )
        } catch (cancelled: CommittedGoalDeletionCancellation) {
            throw CommittedGoalMutationCancellation(
                GoalMutationReceipt(
                    kind = GoalMutationKind.PermanentlyDeleted,
                    goalId = id,
                    deletion = cancelled.summary,
                    warnings = cancelled.summary.warnings,
                ),
                cancelled,
            )
        }
    }

    /** Compatibility entry point for callers that cannot own a reviewed delete request. */
    fun deletePermanently(id: Long) = preparePermanentDeletion(id)
    fun setPinned(
        boundary: GoalMutationBoundary,
        pinned: Boolean,
        requestId: String? = null,
    ) = runAuthoredGoalMutation(
        "Updating Home summary…",
        if (pinned) "Goal pinned to Whip Home" else "Goal unpinned from Whip Home",
        requestId = requestId,
        savedDescription = "Home pin change",
    ) {
        completeCommittedGoalMutation(
            commit = {
                repository.setPinned(boundary, pinned)
                GoalMutationReceipt(GoalMutationKind.LifecycleChanged, boundary.goalId)
            },
            followUp = { committed ->
                if (pinned) app.settingsRepository.revealHomeSection(HomeSection.Goals)
                committed
            },
        )
    }
    fun reorder(ids: List<Long>) = runSilentReorder { repository.reorder(ids) }
    fun record(
        boundary: GoalProgressBoundary,
        value: Double,
        date: LocalDate?,
        note: String,
        requestId: String? = null,
    ): Boolean = runAuthoredGoalMutation("Saving progress…", "Progress saved", requestId, "progress update") {
        val entryId = repository.recordMeasurement(boundary, value, date = date, note = note)
        GoalMutationReceipt(GoalMutationKind.ProgressRecorded, boundary.goalId, measurementEntryId = entryId)
    }
    fun updateMeasurement(
        boundary: GoalMeasurementBoundary,
        value: Double,
        date: LocalDate,
        note: String,
        requestId: String? = null,
    ): Boolean = runAuthoredGoalMutation("Updating progress…", "Progress updated", requestId, "progress update") {
        repository.updateMeasurement(boundary, value, date, note)
        GoalMutationReceipt(GoalMutationKind.ProgressUpdated, boundary.goal.goalId, measurementEntryId = boundary.entryId)
    }
    fun deleteMeasurement(
        boundary: GoalMeasurementBoundary,
        requestId: String? = null,
    ): Boolean = runAuthoredGoalMutation(
        "Removing progress update…",
        "Progress update removed",
        requestId,
        "progress deletion",
    ) {
        repository.deleteMeasurement(boundary)
        GoalMutationReceipt(GoalMutationKind.ProgressDeleted, boundary.goal.goalId, measurementEntryId = boundary.entryId)
    }
    fun toggleMilestone(boundary: GoalMilestoneBoundary, completed: Boolean) = runAuthoredGoalMutation(
        "Updating milestone…",
        "Milestone updated",
        requestId = null,
        savedDescription = "milestone change",
    ) {
        repository.toggleMilestone(boundary, completed)
        GoalMutationReceipt(GoalMutationKind.LifecycleChanged, boundary.goal.goalId)
    }
    fun resetElapsedStart(
        boundary: GoalMutationBoundary,
        start: Instant,
        requestId: String? = null,
    ): Boolean = runAuthoredGoalMutation("Resetting timer…", "Timer reset", requestId, "timer reset") {
        completeCommittedGoalMutation(
            commit = {
                repository.resetElapsedStart(boundary, start)
                GoalMutationReceipt(GoalMutationKind.ElapsedOriginReset, boundary.goalId)
            },
            followUp = { committed -> committed.withReminderRefresh(reminders, boundary.goalId) },
        )
    }
    fun resetElapsedStartToNow(boundary: GoalMutationBoundary, requestId: String? = null) =
        resetElapsedStart(boundary, clock.now(), requestId)

    private fun elapsedClockFlow(): Flow<Long> = flow {
        while (currentCoroutineContext().isActive) {
            emit(clock.now().toEpochMilli())
            delay(30_000L)
        }
    }
    private val reorderMutex = Mutex()
    private val goalMutationMutex = Mutex()

    private fun runAuthoredGoalMutation(
        running: String,
        success: String,
        requestId: String?,
        savedDescription: String,
        block: suspend () -> GoalMutationReceipt,
    ): Boolean {
        if (requestId != null && !_authoredMutationState.tryStartPersistenceRequest(requestId)) return false
        _operationStatus.value = OperationStatus.Running(running)
        viewModelScope.launch {
            fun successResult(receipt: GoalMutationReceipt): WhipResult.Success<GoalMutationReceipt> {
                val message = if (receipt.warnings.isEmpty()) success else {
                    "$success · ${receipt.warnings.joinToString(" ")}"
                }
                _operationStatus.value = OperationStatus.Succeeded(
                    message,
                    OperationFeedbackPresentation.Snackbar,
                )
                return WhipResult.Success(receipt)
            }
            val result = try {
                val receipt = checkNotNull(app.withUserDataAccess {
                    goalMutationMutex.withLock { block() }
                }) { "Whip data is unavailable while recovery is in progress" }
                successResult(receipt)
            } catch (cancelled: CommittedGoalMutationCancellation) {
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
                    _operationStatus.value = OperationStatus.Idle
                    throw cancelled
                }
            } catch (cancelled: CancellationException) {
                if (requestId != null && currentCoroutineContext().isActive) {
                    _operationStatus.value = OperationStatus.Idle
                    WhipResult.Failure(
                        "The $savedDescription was interrupted. Your changes are still here.",
                        cancelled,
                    )
                } else {
                    if ((_authoredMutationState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                        _authoredMutationState.value = PersistenceRequestState.Idle
                    }
                    _operationStatus.value = OperationStatus.Idle
                    throw cancelled
                }
            } catch (error: Exception) {
                _operationStatus.value = if (requestId == null) {
                    OperationStatus.Failed(error.message ?: "Something went wrong", error)
                } else OperationStatus.Idle
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
                } catch (error: Exception) {
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
            } catch (error: Exception) {
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
    closureSnapshots: List<GoalClosureSnapshot>,
    elapsedResetEvents: List<GoalElapsedResetEvent>,
    today: LocalDate,
): GoalUiState {
    val projections = goals.map { goal ->
        val closures = closureSnapshots.filter { it.goalId == goal.id }
        val resetEvents = elapsedResetEvents.filter { it.goalId == goal.id }
        val live = projectGoal(
            goal,
            entries.filter { it.metricId == goal.metricId },
            milestones.filter { it.goalId == goal.id },
            today,
        )
        val frozen = closures.lastOrNull { it.status == goal.status }
            ?.takeIf { goal.status in setOf(GoalStatus.Completed, GoalStatus.Abandoned) }
        if (frozen == null) {
            live.copy(closureSnapshots = closures, elapsedResetEvents = resetEvents)
        } else {
            live.copy(
                currentValue = frozen.value,
                progress = frozen.progress,
                // The closure table predates a stored baseline delta. Do not
                // recompute it from a later edited definition and rewrite history.
                deltaFromBaseline = null,
                expectedProgress = null,
                paceDelta = null,
                forecastDate = null,
                onPace = null,
                consistency = null,
                closureSnapshots = closures,
                elapsedResetEvents = resetEvents,
                terminalSnapshot = frozen,
            )
        }
    }
    return GoalUiState(
        active = projections.filter { !it.goal.archived && it.goal.status in setOf(GoalStatus.Active, GoalStatus.Paused) },
        completed = projections.filter { !it.goal.archived && it.goal.status in setOf(GoalStatus.Completed, GoalStatus.Abandoned) },
        archived = projections.filter { it.goal.archived },
        currentDate = today,
        loading = false,
    )
}

private suspend fun GoalMutationReceipt.withReminderRefresh(
    reminders: GoalReminderScheduler,
    goalId: Long,
): GoalMutationReceipt {
    reminders.syncGoal(goalId)
    return this
}
