package com.whip.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.whip.app.WhipApplication
import com.whip.app.core.HomeSection
import com.whip.app.core.OperationFeedbackPresentation
import com.whip.app.core.OperationStatus
import com.whip.app.core.currentDateFlow
import com.whip.app.core.revealHomeSection
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
import com.whip.app.reminders.reminderDefinitionChanged
import java.time.LocalDate
import java.time.Instant
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GoalUiState(
    val active: List<GoalProjection> = emptyList(),
    val completed: List<GoalProjection> = emptyList(),
    val archived: List<GoalProjection> = emptyList(),
    val currentDate: LocalDate = LocalDate.now(),
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
    private val reloadKey = MutableStateFlow(0)

    fun defaultSettings() = app.settingsRepository.current()

    init { viewModelScope.launch { runCatching { reminders.syncAll() } } }

    private val goalCore = combine(
        repository.goals,
        repository.milestones,
        repository.metricEntries,
        app.settingsRepository.currentDateFlow(clock),
    ) { goals, milestones, entries, today -> buildState(goals, milestones, entries, today) }

    private val measurementMetadata = combine(
        app.measurementRepository.customUnits,
        app.measurementRepository.metrics,
    ) { units, metrics -> units to metrics }

    val uiState = reloadKey.flatMapLatest {
        combine(goalCore, measurementMetadata) { core, metadata ->
            val (units, metrics) = metadata
            core.copy(
                customUnits = units,
                sourceMetrics = metrics.filterNot { it.archived },
            )
        }.catch { error ->
            emit(GoalUiState(currentDate = clock.today(), loading = false, errorMessage = error.message ?: "Could not load goals"))
        }
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            GoalUiState(currentDate = clock.today()),
        )

    fun consumeOperationStatus() { _operationStatus.value = OperationStatus.Idle }
    fun retryLoading() { reloadKey.value++ }
    fun saveGoal(id: Long?, draft: GoalDraft, onFinished: (Boolean) -> Unit = {}) = runOperation(
        if (id == null) "Creating goal…" else "Saving goal…",
        if (id == null) "Goal created" else "Goal saved",
        onFinished,
    ) {
        val existing = id?.let { repository.get(it) }
        val savedId = if (id == null) repository.create(draft) else { repository.update(id, draft); id }
        if (existing == null || existing.reminderDefinitionChanged(draft)) reminders.syncGoal(savedId)
        draft.tags.filter { tag -> existing?.tags?.none { it.equals(tag, ignoreCase = true) } != false }
            .forEach { app.measurementRepository.ensureTag(it) }
    }
    fun duplicate(id: Long) = runOperation("Duplicating goal…", "Goal duplicated") { repository.duplicate(id) }
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
