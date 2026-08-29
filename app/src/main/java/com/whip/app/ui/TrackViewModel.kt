package com.whip.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.whip.app.WhipApplication
import com.whip.app.core.HomeSection
import com.whip.app.core.OperationFeedbackPresentation
import com.whip.app.core.OperationStatus
import com.whip.app.core.revealHomeSection
import com.whip.app.data.TrackRepository
import com.whip.app.domain.DeletedTrackEntry
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalConsistencyPeriod
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalType
import com.whip.app.domain.Habit
import com.whip.app.domain.LinkRuleDraft
import com.whip.app.domain.LinkRule
import com.whip.app.domain.LinkSourceMetric
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.LinkValueMode
import com.whip.app.domain.Track
import com.whip.app.domain.TrackChoiceOption
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackEntryPage
import com.whip.app.domain.TrackField
import com.whip.app.domain.TrackProjection
import com.whip.app.domain.TrackAggregation
import com.whip.app.domain.TrackCondition
import com.whip.app.domain.TrackConditionMode
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TaskStep
import com.whip.app.domain.TriggerOccurrence
import com.whip.app.domain.TriggerRule
import com.whip.app.domain.TriggerRuleDraft
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.WhipTask
import com.whip.app.domain.Goal
import com.whip.app.domain.compatibleAggregations
import com.whip.app.domain.Contribution
import java.time.LocalDate
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class TrackUiState(
    val projections: List<TrackProjection> = emptyList(),
    val linkRules: List<LinkRule> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val triggerRules: List<TriggerRule> = emptyList(),
    val triggerOccurrences: List<TriggerOccurrence> = emptyList(),
    val contributions: List<Contribution> = emptyList(),
    val sourceTasks: List<WhipTask> = emptyList(),
    val sourceTaskSteps: List<TaskStep> = emptyList(),
    val sourceHabits: List<Habit> = emptyList(),
    val currentDate: LocalDate = LocalDate.now(),
    val loading: Boolean = true,
    val errorMessage: String? = null,
) {
    val active: List<TrackProjection> get() = projections.filterNot { it.track.archived }
    val archived: List<TrackProjection> get() = projections.filter { it.track.archived }
    val pinned: List<TrackProjection> get() = active.filter { it.track.pinned }
    fun track(id: Long): TrackProjection? = projections.firstOrNull { it.track.id == id }
    fun forArea(areaId: String?): TrackUiState = if (areaId == null) this else copy(
        projections = projections.filter { it.track.areaId == areaId },
    )
}

data class PendingTrackEntryUndo(
    val token: Long,
    val deletedEntry: DeletedTrackEntry,
)

private data class TrackAutomationState(
    val linkRules: List<LinkRule>,
    val triggerRules: List<TriggerRule>,
    val occurrences: List<TriggerOccurrence>,
    val contributions: List<Contribution>,
)

private data class TrackSourceState(
    val tasks: List<WhipTask>,
    val taskSteps: List<TaskStep>,
    val habits: List<Habit>,
    val goals: List<Goal>,
)

data class TrackGoalAutomationDraft(
    val goalName: String,
    val goalType: GoalType,
    val aggregation: TrackAggregation,
    val sourceFieldId: Long? = null,
    val conditionMode: TrackConditionMode = TrackConditionMode.MatchAll,
    val conditions: List<TrackCondition> = emptyList(),
    val target: Double? = null,
    val targetMax: Double? = null,
    val fixedAmount: Double? = null,
    val fixedDimension: UnitDimension = UnitDimension.Count,
    val fixedUnitId: String = "count",
    val retroactiveFrom: LocalDate? = null,
    val deadline: LocalDate? = null,
    val consistencyPeriod: GoalConsistencyPeriod = GoalConsistencyPeriod.Week,
    val consistencyRequiredPeriods: Int? = null,
)

class TrackViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as WhipApplication
    private val repository: TrackRepository = app.trackRepository
    private val clock = app.clock
    private val _operationStatus = MutableStateFlow<OperationStatus>(OperationStatus.Idle)
    val operationStatus: StateFlow<OperationStatus> = _operationStatus.asStateFlow()
    private val operationMutex = Mutex()
    private var recoveryAcknowledgement: CompletableDeferred<Unit>? = null
    private val _lastDeletedEntry = MutableStateFlow<PendingTrackEntryUndo?>(null)
    val lastDeletedEntry: StateFlow<PendingTrackEntryUndo?> = _lastDeletedEntry.asStateFlow()
    private var nextEntryUndoToken = 0L

    private val automationState = combine(
        app.linkRepository.rules,
        app.linkRepository.triggerRules,
        app.linkRepository.triggerOccurrences,
        app.linkRepository.contributions,
    ) { linkRules, triggerRules, occurrences, contributions ->
        TrackAutomationState(linkRules, triggerRules, occurrences, contributions)
    }
    private val sourceState = combine(
        app.taskRepository.tasks,
        app.taskRepository.steps,
        app.habitRepository.habits,
        app.goalRepository.goals,
    ) { tasks, taskSteps, habits, goals -> TrackSourceState(tasks, taskSteps, habits, goals) }

    val uiState: StateFlow<TrackUiState> = combine(
        repository.projections,
        automationState,
        sourceState,
    ) { projections, automations, sources ->
        val (linkRules, triggerRules, occurrences, contributions) = automations
        TrackUiState(
            projections = projections,
            linkRules = linkRules,
            goals = sources.goals,
            triggerRules = triggerRules,
            triggerOccurrences = occurrences,
            contributions = contributions,
            sourceTasks = sources.tasks.filterNot(WhipTask::archived),
            sourceTaskSteps = sources.taskSteps.filterNot(TaskStep::archived),
            sourceHabits = sources.habits.filterNot(Habit::archived),
            currentDate = clock.today(),
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackUiState())

    fun consumeOperationStatus() {
        _operationStatus.value = OperationStatus.Idle
        recoveryAcknowledgement?.complete(Unit)
        recoveryAcknowledgement = null
    }

    fun saveTrack(
        id: Long?,
        draft: TrackDraft,
        confirmedFieldValueDeletionIds: Set<Long> = emptySet(),
        confirmedOptionValueDeletionIds: Set<Long> = emptySet(),
        optionReplacementIds: Map<Long, Long> = emptyMap(),
        onSaved: (Long) -> Unit = {},
    ) = runOperation(if (id == null) "Creating Track…" else "Saving Track…", if (id == null) "Track created" else "Track saved") {
        val existingTags = id?.let { uiState.value.track(it)?.track?.tags }
        var automationInputsChanged = false
        val savedId = if (id == null) repository.create(draft) else {
            automationInputsChanged = repository.update(
                id,
                draft,
                confirmedFieldValueDeletionIds,
                confirmedOptionValueDeletionIds,
                optionReplacementIds,
            ).automationInputsChanged
            id
        }
        draft.tags.filter { tag -> existingTags?.none { it.equals(tag, ignoreCase = true) } != false }
            .forEach { app.measurementRepository.ensureTag(it) }
        if (automationInputsChanged) {
            app.linkRepository.rebuildAll()
            app.automationPromptScheduler.syncAll()
        }
        onSaved(savedId)
    }

    fun duplicate(id: Long) = runOperation("Duplicating Track…", "Track structure duplicated") { repository.duplicate(id) }
    fun setPinned(id: Long, pinned: Boolean) = runOperation(
        "Updating Home Quick Log…",
        if (pinned) "Track added to Home Quick Log" else "Track removed from Home Quick Log",
        successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
    ) {
        repository.setPinned(id, pinned)
        if (pinned) app.settingsRepository.revealHomeSection(HomeSection.Tracks)
    }
    fun setPinned(ids: Collection<Long>, pinned: Boolean) = runOperation(
        "Updating Home Quick Log…",
        "${ids.size} Tracks ${if (pinned) "added to" else "removed from"} Home Quick Log",
        successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
    ) {
        app.database.withTransaction { ids.distinct().forEach { repository.setPinned(it, pinned) } }
        if (pinned) app.settingsRepository.revealHomeSection(HomeSection.Tracks)
    }
    fun setArchived(id: Long, archived: Boolean) = runOperation(
        if (archived) "Archiving Track…" else "Restoring Track…",
        if (archived) "Track archived" else "Track restored",
    ) {
        repository.setArchived(id, archived)
        app.linkRepository.rebuildAll()
        app.automationPromptScheduler.syncAll()
    }
    fun setArchived(ids: Collection<Long>, archived: Boolean) = runOperation(
        if (archived) "Archiving Tracks…" else "Restoring Tracks…",
        "${ids.size} Tracks ${if (archived) "archived" else "restored"}",
    ) {
        app.database.withTransaction { ids.distinct().forEach { repository.setArchived(it, archived) } }
        app.linkRepository.rebuildAll()
        app.automationPromptScheduler.syncAll()
    }
    fun reorder(ids: List<Long>) = runSilentReorder { repository.reorder(ids) }
    fun deleteTrack(id: Long) = runOperation(
        "Deleting Track…",
        "Track permanently deleted",
        successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
    ) {
        app.domainDeletionCoordinator.deleteTrack(id)
    }

    fun saveEntry(trackId: Long, entryId: Long?, draft: TrackEntryDraft, onSaved: (Long) -> Unit = {}) = runOperation(
        if (entryId == null) "Adding Entry…" else "Saving Entry…",
        if (entryId == null) "Entry added" else "Entry saved",
    ) {
        val savedId = if (entryId == null) repository.addEntry(trackId, draft) else {
            repository.updateEntry(entryId, draft)
            entryId
        }
        reconcileTrackAutomations()
        onSaved(savedId)
    }

    fun importEntries(trackId: Long, drafts: List<TrackEntryDraft>, onSaved: (Int) -> Unit = {}) = runOperation(
        "Importing Entries…",
        "${drafts.size} Entries imported",
        successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
    ) {
        val importedCount = repository.importEntries(trackId, drafts).size
        reconcileTrackAutomations()
        onSaved(importedCount)
    }

    fun deleteEntry(entryId: Long): Unit {
        val undoToken = ++nextEntryUndoToken
        runOperation(
            "Deleting Entry…",
            "Entry deleted",
            successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
        recoveryToken = undoToken,
        ) {
            val deletedEntry = repository.deleteEntry(entryId) ?: error("Entry no longer exists")
            _lastDeletedEntry.value = PendingTrackEntryUndo(undoToken, deletedEntry)
            reconcileTrackAutomations()
        }
    }

    fun undoEntryDeletion(expectedToken: Long) {
        if (_lastDeletedEntry.value?.token != expectedToken) return
        runOperation("Restoring Entry…", "Entry restored") {
            _lastDeletedEntry.value
                ?.takeIf { it.token == expectedToken }
                ?.let { repository.restoreEntry(it.deletedEntry) }
            reconcileTrackAutomations()
            if (_lastDeletedEntry.value?.token == expectedToken) _lastDeletedEntry.value = null
        }
    }

    fun clearEntryUndo(expectedToken: Long) {
        if (_lastDeletedEntry.value?.token == expectedToken) _lastDeletedEntry.value = null
    }
    suspend fun exportCsv(trackId: Long): String = repository.exportCsv(trackId)
    suspend fun searchEntryIds(trackId: Long, query: String): Set<Long> = repository.searchEntryIds(trackId, query)
    suspend fun entryPage(trackId: Long, offset: Int, limit: Int = 100): TrackEntryPage =
        repository.entryPage(trackId, offset, limit)

    fun createTrigger(draft: TriggerRuleDraft, onSaved: () -> Unit = {}) = runOperation("Creating Next-Action Automation…", "Next-Action Automation created") {
        app.linkRepository.createTrigger(draft)
        app.automationPromptScheduler.syncAll()
        onSaved()
    }

    fun updateTrigger(id: Long, draft: TriggerRuleDraft, onSaved: () -> Unit = {}) = runOperation("Saving Next-Action Automation…", "Next-Action Automation saved") {
        app.linkRepository.updateTrigger(id, draft)
        app.automationPromptScheduler.syncAll()
        onSaved()
    }

    fun deleteTrigger(id: Long) = runOperation("Removing Next-Action Automation…", "Next-Action Automation removed") {
        app.linkRepository.deleteTrigger(id)
        app.automationPromptScheduler.syncAll()
    }

    fun updateProgressAutomation(id: Long, draft: LinkRuleDraft, onSaved: () -> Unit = {}) = runOperation("Saving Goal Automation…", "Goal Automation saved") {
        alignGoalCalculation(draft)
        app.linkRepository.updateRule(id, draft)
        onSaved()
    }

    fun createProgressAutomation(
        draft: LinkRuleDraft,
        includeHistory: Boolean,
        onSaved: () -> Unit = {},
    ) = runOperation("Connecting Track to Goal…", "Track connected to Goal") {
        alignGoalCalculation(draft)
        app.linkRepository.createRule(draft, commitBackfill = includeHistory)
        onSaved()
    }

    fun setProgressAutomationEnabled(id: Long, enabled: Boolean) = runOperation("Updating Goal Automation…", "Goal Automation updated") {
        app.linkRepository.setRuleEnabled(id, enabled)
    }

    fun deleteProgressAutomation(id: Long) = runOperation("Removing Goal Automation…", "Goal Automation removed") {
        app.linkRepository.deleteRule(id)
    }

    fun dismissPrompt(id: Long) = runOperation("Dismissing prompt…", "Prompt dismissed") {
        app.linkRepository.dismissTriggerOccurrence(id)
        app.automationPromptScheduler.cancel(id)
    }

    fun remindPrompt(id: Long, at: java.time.Instant) = runOperation("Setting reminder…", "Reminder set") {
        app.linkRepository.remindTriggerOccurrence(id, at)
        app.automationPromptScheduler.syncAll()
    }

    fun loadPromptDraft(id: Long, onLoaded: (TrackEntryDraft) -> Unit) {
        viewModelScope.launch {
            try {
                onLoaded(app.linkRepository.trackPromptDraft(id))
            } catch (error: Throwable) {
                _operationStatus.value = OperationStatus.Failed(error.message ?: "Could not open this prompt", error)
            }
        }
    }

    fun fulfillPrompt(id: Long, draft: TrackEntryDraft, onSaved: (Long) -> Unit = {}) = runOperation(
        "Adding prompted Entry…",
        "Entry added",
    ) {
        val entryId = app.linkRepository.fulfillTrackPrompt(id, draft)
        reconcileTrackAutomations()
        app.automationPromptScheduler.cancel(id)
        onSaved(entryId)
    }

    fun createGoalFromTrack(trackId: Long, draft: TrackGoalAutomationDraft, onSaved: (Long) -> Unit = {}) = runOperation(
        "Creating Goal From Track…",
        "Goal and Automation created",
    ) {
        val projection = repository.projection(trackId) ?: error("Track no longer exists")
        val needsField = draft.aggregation in setOf(
            TrackAggregation.Sum,
            TrackAggregation.Average,
            TrackAggregation.Latest,
            TrackAggregation.Minimum,
            TrackAggregation.Maximum,
        )
        val field = draft.sourceFieldId?.let { id -> projection.fields.firstOrNull { it.id == id } }
        if (needsField) require(field?.type in setOf(TrackFieldType.Number, TrackFieldType.Scale)) {
            "Choose a Number or Scale Field"
        }
        val goalAggregation = when {
            draft.goalType == GoalType.Consistency -> GoalAggregation.CompletionCount
            draft.aggregation in setOf(TrackAggregation.Sum, TrackAggregation.CountEntries, TrackAggregation.CountMatchingEntries, TrackAggregation.FixedAmount) -> GoalAggregation.Sum
            draft.aggregation == TrackAggregation.Average -> GoalAggregation.Average
            draft.aggregation == TrackAggregation.Latest -> GoalAggregation.Latest
            draft.aggregation == TrackAggregation.Minimum -> GoalAggregation.Minimum
            draft.aggregation == TrackAggregation.Maximum -> GoalAggregation.Maximum
            else -> error("Choose a compatible Track measure")
        }
        require(draft.goalType != GoalType.Consistency || draft.aggregation in setOf(TrackAggregation.CountEntries, TrackAggregation.CountMatchingEntries)) {
            "Consistency Goals require Count Entries or Count Matching Entries"
        }
        require(goalAggregation in draft.goalType.compatibleAggregations()) {
            "${draft.aggregation.name} does not match ${draft.goalType.name}"
        }
        val dimension = when {
            draft.aggregation in setOf(TrackAggregation.CountEntries, TrackAggregation.CountMatchingEntries) -> UnitDimension.Count
            draft.aggregation == TrackAggregation.FixedAmount -> draft.fixedDimension
            field?.type == TrackFieldType.Scale -> UnitDimension.Unitless
            else -> field?.dimension ?: UnitDimension.Unitless
        }
        val unitId = when {
            draft.aggregation == TrackAggregation.FixedAmount -> draft.fixedUnitId
            dimension == UnitDimension.Count -> "count"
            field?.type == TrackFieldType.Scale -> "unitless"
            else -> field?.unitId ?: "unitless"
        }
        val goalDraft = GoalDraft(
            name = draft.goalName,
            description = "Progress from ${projection.track.name} Entries.",
            areaId = projection.track.areaId,
            area = projection.track.area,
            tags = projection.track.tags,
            icon = projection.track.icon,
            type = draft.goalType,
            dimension = dimension,
            unitId = unitId,
            precision = if (dimension == UnitDimension.Count) 0 else field?.precision ?: 1,
            targetMin = draft.target,
            targetMax = draft.targetMax,
            startDate = draft.retroactiveFrom ?: clock.today(),
            deadline = draft.deadline,
            aggregation = goalAggregation,
            consistencyPeriod = draft.consistencyPeriod,
            consistencyRequiredPeriods = draft.consistencyRequiredPeriods,
        )
        var goalId = 0L
        app.database.withTransaction {
            goalId = app.goalRepository.create(goalDraft)
            app.linkRepository.createRule(
                LinkRuleDraft(
                    name = "${projection.track.name} → ${draft.goalName}",
                    sourceType = LinkSourceType.Track,
                    sourceEntityId = trackId,
                    sourceMetric = if (needsField) LinkSourceMetric.FieldValue else LinkSourceMetric.EntryCount,
                    targetGoalId = goalId,
                    valueMode = if (draft.aggregation == TrackAggregation.FixedAmount) LinkValueMode.FixedValue else LinkValueMode.SourceValue,
                    fixedValue = draft.fixedAmount,
                    retroactiveFrom = draft.retroactiveFrom,
                    trackAggregation = draft.aggregation,
                    sourceFieldId = field?.id.takeIf { needsField },
                    conditionMode = draft.conditionMode,
                    conditions = draft.conditions,
                ),
                commitBackfill = draft.retroactiveFrom != null,
            )
        }
        app.goalReminderScheduler.syncGoal(goalId)
        onSaved(goalId)
    }

    private suspend fun reconcileTrackAutomations() {
        app.linkRepository.rebuildSources(setOf(LinkSourceType.Track))
        app.automationPromptScheduler.syncAll()
    }

    private suspend fun alignGoalCalculation(draft: LinkRuleDraft) {
        if (draft.sourceType != LinkSourceType.Track || draft.targetMilestoneId != null) return
        val measure = draft.trackAggregation ?: return
        val goal = app.goalRepository.goals.first().firstOrNull { it.id == draft.targetGoalId }
            ?: error("Goal no longer exists")
        val required = goal.requiredAggregationForTrack(measure)
        val aligned = goal.toTrackAutomationDraft(required, draft.retroactiveFrom)
        if (goal.aggregation != aligned.aggregation || goal.startDate != aligned.startDate) {
            app.goalRepository.update(goal.id, aligned)
        }
    }

    private fun runSilentReorder(block: suspend () -> Unit) {
        viewModelScope.launch {
            operationMutex.withLock {
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
        successFeedbackPresentation: OperationFeedbackPresentation = OperationFeedbackPresentation.Inline,
        recoveryToken: Long? = null,
        block: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            operationMutex.withLock {
                _operationStatus.value = OperationStatus.Running(running)
                try {
                    block()
                    val acknowledgement = recoveryToken?.let { CompletableDeferred<Unit>() }
                    recoveryAcknowledgement = acknowledgement
                    _operationStatus.value = OperationStatus.Succeeded(
                        success,
                        successFeedbackPresentation,
                        recoveryToken,
                    )
                    acknowledgement?.await()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    _operationStatus.value = OperationStatus.Failed(error.message ?: "Something went wrong", error)
                }
            }
        }
    }
}

internal fun Track.toDraft(fields: List<TrackField>, options: List<TrackChoiceOption>) = TrackDraft(
    name = name,
    description = description,
    icon = icon,
    areaId = areaId,
    area = area,
    tags = tags,
    fields = fields.sortedBy(TrackField::position).map { field ->
        com.whip.app.domain.TrackFieldDraft(
            name = field.name,
            type = field.type,
            required = field.required,
            primary = field.primary,
            showInList = field.showInList,
            dimension = field.dimension,
            unitId = field.unitId,
            precision = field.precision,
            scaleMin = field.scaleMin,
            scaleMax = field.scaleMax,
            scaleLowLabel = field.scaleLowLabel,
            scaleHighLabel = field.scaleHighLabel,
            scaleStep = field.scaleStep,
            options = options.filter { it.fieldId == field.id }.sortedBy(TrackChoiceOption::position).map {
                com.whip.app.domain.TrackChoiceOptionDraft(it.label, it.uuid, it.id)
            },
            uuid = field.uuid,
            id = field.id,
        )
    },
)
