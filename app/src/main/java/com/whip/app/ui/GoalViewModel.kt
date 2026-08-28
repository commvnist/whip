package com.whip.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.whip.app.WhipApplication
import com.whip.app.core.OperationFeedbackPresentation
import com.whip.app.core.OperationStatus
import com.whip.app.core.currentDateFlow
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
import com.whip.app.domain.TrackProjection
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.WhipTask
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
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
    val sourceTracks: List<TrackProjection> = emptyList(),
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
        app.settingsRepository.currentDateFlow(clock),
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

    private val linkAndTracks = combine(linkData, app.trackRepository.projections) { links, tracks -> links to tracks }

    val uiState = reloadKey.flatMapLatest {
        combine(goalCore, linkAndTracks, app.taskRepository.steps, _backfillPreview, measurementMetadata) { core, linkAndTrackData, steps, preview, metadata ->
            val (links, tracks) = linkAndTrackData
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
                sourceTracks = tracks.filterNot { it.track.archived },
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
    fun setPinned(id: Long, pinned: Boolean) = runOperation("Updating goal…", "Goal updated") { repository.setPinned(id, pinned) }
    fun reorder(ids: List<Long>) = runSilentReorder { repository.reorder(ids) }
    fun record(id: Long, value: Double, date: LocalDate?, note: String) = runOperation("Saving measurement…", "Measurement saved") { repository.recordMeasurement(id, value, date = date, note = note) }
    fun updateMeasurement(id: Long, entryId: String, value: Double, date: LocalDate, note: String) =
        runOperation("Updating measurement…", "Measurement updated") { repository.updateMeasurement(id, entryId, value, date, note) }
    fun deleteMeasurement(id: Long, entryId: String) =
        runOperation(
            "Removing measurement…",
            "Measurement removed",
        ) { repository.deleteMeasurement(id, entryId) }
    fun toggleMilestone(id: Long, completed: Boolean) = runOperation(
        "Updating milestone…",
        "Milestone updated",
        successFeedbackPresentation = OperationFeedbackPresentation.Inline,
    ) { repository.toggleMilestone(id, completed) }
    fun resetElapsedStart(id: Long, start: Instant) = runOperation("Resetting timer…", "Timer reset") {
        repository.resetElapsedStart(id, start)
    }
    fun previewLink(draft: LinkRuleDraft) = runOperation("Previewing history…", "Backfill preview ready") {
        _backfillPreview.value = linkRepository.previewBackfill(draft)
    }
    fun clearLinkPreview() { _backfillPreview.value = null }
    fun createLink(draft: LinkRuleDraft, includeHistory: Boolean, onFinished: (Boolean) -> Unit = {}) = runOperation("Creating Goal Automation…", "Goal Automation created", onFinished) {
        alignGoalCalculation(draft)
        linkRepository.createRule(draft, includeHistory)
        _backfillPreview.value = null
    }
    fun updateLink(id: Long, draft: LinkRuleDraft, onFinished: (Boolean) -> Unit = {}) = runOperation("Saving Goal Automation…", "Goal Automation saved", onFinished) {
        alignGoalCalculation(draft)
        linkRepository.updateRule(id, draft)
        _backfillPreview.value = null
    }
    fun setLinkEnabled(id: Long, enabled: Boolean) = runOperation("Updating Goal Automation…", "Goal Automation updated") { linkRepository.setRuleEnabled(id, enabled) }
    fun deleteLink(id: Long) = runOperation("Removing Goal Automation…", "Goal Automation removed") { linkRepository.deleteRule(id) }
    fun setContributionExcluded(id: Long, excluded: Boolean) = runOperation("Updating contribution…", "Contribution updated") {
        linkRepository.setContributionExcluded(id, excluded)
    }
    fun setContributionOverride(id: Long, canonicalValue: Double?) = runOperation("Updating contribution…", "Contribution updated") {
        linkRepository.setContributionOverride(id, canonicalValue)
    }

    private suspend fun alignGoalCalculation(draft: LinkRuleDraft) {
        if (draft.sourceType != LinkSourceType.Track || draft.targetMilestoneId != null) return
        val measure = draft.trackAggregation ?: return
        val goal = repository.goals.first().firstOrNull { it.id == draft.targetGoalId }
            ?: error("Goal no longer exists")
        val required = goal.requiredAggregationForTrack(measure)
        val aligned = goal.toTrackAutomationDraft(required, draft.retroactiveFrom)
        if (goal.aggregation != aligned.aggregation || goal.startDate != aligned.startDate) {
            repository.update(goal.id, aligned)
        }
    }

    private val reorderMutex = Mutex()

    private fun runSilentReorder(block: suspend () -> Unit) {
        viewModelScope.launch {
            reorderMutex.withLock {
                try {
                    block()
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
                block()
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
