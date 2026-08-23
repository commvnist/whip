package com.whip.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.whip.app.WhipApplication
import com.whip.app.core.OperationStatus
import com.whip.app.data.GoalRepository
import com.whip.app.data.LinkRepository
import com.whip.app.domain.Goal
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalMilestone
import com.whip.app.domain.GoalProjection
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.Contribution
import com.whip.app.domain.Exercise
import com.whip.app.domain.Habit
import com.whip.app.domain.LinkBackfillPreview
import com.whip.app.domain.LinkRule
import com.whip.app.domain.LinkRuleDraft
import com.whip.app.domain.MetricEntry
import com.whip.app.domain.MetricDefinition
import com.whip.app.domain.TaskStep
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.WhipTask
import com.whip.app.domain.projectGoal
import java.time.LocalDate
import java.util.concurrent.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val linkRules: List<LinkRule> = emptyList(),
    val contributions: List<Contribution> = emptyList(),
    val sourceHabits: List<Habit> = emptyList(),
    val sourceTasks: List<WhipTask> = emptyList(),
    val sourceTaskSteps: List<TaskStep> = emptyList(),
    val sourceExercises: List<Exercise> = emptyList(),
    val backfillPreview: LinkBackfillPreview? = null,
    val customUnits: List<UnitDefinition> = emptyList(),
    val sourceMetrics: List<MetricDefinition> = emptyList(),
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GoalViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as WhipApplication
    private val repository: GoalRepository = app.goalRepository
    private val linkRepository: LinkRepository = app.linkRepository
    private val clock = app.clock
    private val reminders = app.goalReminderScheduler
    private val _operationStatus = MutableStateFlow<OperationStatus>(OperationStatus.Idle)
    val operationStatus: StateFlow<OperationStatus> = _operationStatus.asStateFlow()
    private val reloadKey = MutableStateFlow(0)

    fun defaultSettings() = app.settingsRepository.current()
    private val _backfillPreview = MutableStateFlow<LinkBackfillPreview?>(null)

    init { viewModelScope.launch { runCatching { reminders.syncAll() } } }

    private val goalCore = combine(
        repository.goals,
        repository.milestones,
        repository.metricEntries,
        currentDateFlow(),
    ) { goals, milestones, entries, today -> buildState(goals, milestones, entries, today) }

    private val linkData = combine(
        linkRepository.rules,
        linkRepository.contributions,
        app.habitRepository.habits,
        app.taskRepository.tasks,
        app.gymRepository.exercises,
    ) { rules, contributions, habits, tasks, exercises ->
        GoalLinkData(rules, contributions, habits, tasks, exercises)
    }

    private val measurementMetadata = combine(
        app.measurementRepository.customUnits,
        app.measurementRepository.metrics,
    ) { units, metrics -> units to metrics }

    val uiState = reloadKey.flatMapLatest {
        combine(goalCore, linkData, app.taskRepository.steps, _backfillPreview, measurementMetadata) { core, links, steps, preview, metadata ->
            val (units, metrics) = metadata
            core.copy(
                linkRules = links.rules,
                contributions = links.contributions,
                sourceHabits = links.habits.filterNot(Habit::archived),
                sourceTasks = links.tasks.filterNot(WhipTask::archived),
                sourceTaskSteps = steps.filterNot(TaskStep::archived),
                sourceExercises = links.exercises.filterNot(Exercise::archived),
                backfillPreview = preview,
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
    fun saveGoal(id: Long?, draft: GoalDraft) = runOperation(
        if (id == null) "Creating goal…" else "Saving goal…",
        if (id == null) "Goal created" else "Goal saved",
    ) {
        val savedId = if (id == null) repository.create(draft) else { repository.update(id, draft); id }
        reminders.syncGoal(savedId)
        draft.tags.forEach { app.measurementRepository.ensureTag(it) }
    }
    fun duplicate(id: Long) = runOperation("Duplicating goal…", "Goal duplicated") { repository.duplicate(id) }
    fun setStatus(id: Long, status: GoalStatus) = runOperation("Updating goal…", "Goal ${status.name.lowercase()}") { repository.setStatus(id, status); reminders.syncGoal(id) }
    fun deletePermanently(id: Long) = runOperation("Deleting goal…", "Goal permanently deleted") {
        app.domainDeletionCoordinator.deleteGoal(id)
        reminders.syncGoal(id)
    }
    fun setPinned(id: Long, pinned: Boolean) = runOperation("Updating goal…", "Goal updated") { repository.setPinned(id, pinned) }
    fun reorder(ids: List<Long>) = runOperation("Reordering goals…", "Goal order saved") { repository.reorder(ids) }
    fun record(id: Long, value: Double, date: LocalDate?, note: String) = runOperation("Saving measurement…", "Measurement saved") { repository.recordMeasurement(id, value, date = date, note = note) }
    fun updateMeasurement(id: Long, entryId: String, value: Double, date: LocalDate, note: String) =
        runOperation("Updating measurement…", "Measurement updated") { repository.updateMeasurement(id, entryId, value, date, note) }
    fun deleteMeasurement(id: Long, entryId: String) =
        runOperation("Removing measurement…", "Measurement removed") { repository.deleteMeasurement(id, entryId) }
    fun toggleMilestone(id: Long, completed: Boolean) = runOperation("Updating milestone…", "Milestone updated") { repository.toggleMilestone(id, completed) }
    fun previewLink(draft: LinkRuleDraft) = runOperation("Previewing history…", "Backfill preview ready") {
        _backfillPreview.value = linkRepository.previewBackfill(draft)
    }
    fun clearLinkPreview() { _backfillPreview.value = null }
    fun createLink(draft: LinkRuleDraft, includeHistory: Boolean) = runOperation("Creating link…", "Link created") {
        linkRepository.createRule(draft, includeHistory)
        _backfillPreview.value = null
    }
    fun updateLink(id: Long, draft: LinkRuleDraft) = runOperation("Saving link…", "Link saved") {
        linkRepository.updateRule(id, draft)
        _backfillPreview.value = null
    }
    fun setLinkEnabled(id: Long, enabled: Boolean) = runOperation("Updating link…", "Link updated") { linkRepository.setRuleEnabled(id, enabled) }
    fun deleteLink(id: Long) = runOperation("Removing link…", "Link removed") { linkRepository.deleteRule(id) }
    fun setContributionExcluded(id: Long, excluded: Boolean) = runOperation("Updating contribution…", "Contribution updated") {
        linkRepository.setContributionExcluded(id, excluded)
    }
    fun setContributionOverride(id: Long, canonicalValue: Double?) = runOperation("Updating contribution…", "Contribution updated") {
        linkRepository.setContributionOverride(id, canonicalValue)
    }

    private fun runOperation(running: String, success: String, block: suspend () -> Unit) {
        _operationStatus.value = OperationStatus.Running(running)
        viewModelScope.launch {
            try {
                block()
                _operationStatus.value = OperationStatus.Succeeded(success)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _operationStatus.value = OperationStatus.Failed(error.message ?: "Something went wrong", error)
            }
        }
    }

    private fun currentDateFlow(): Flow<LocalDate> = flow {
        while (currentCoroutineContext().isActive) {
            emit(clock.today())
            delay(60_000)
        }
    }
}

private data class GoalLinkData(
    val rules: List<LinkRule>,
    val contributions: List<Contribution>,
    val habits: List<Habit>,
    val tasks: List<WhipTask>,
    val exercises: List<Exercise>,
)

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
