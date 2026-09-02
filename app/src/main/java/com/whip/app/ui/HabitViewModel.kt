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
import com.whip.app.core.revealHomeSection
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipResult
import com.whip.app.core.completeCommittedEntitySave
import com.whip.app.core.completeCommittedPersistence
import com.whip.app.core.saveFollowUpWarning
import com.whip.app.core.tryStartPersistenceRequest
import com.whip.app.data.HabitRepository
import com.whip.app.reminders.HabitReminderScheduler
import com.whip.app.domain.Habit
import com.whip.app.domain.HabitChecklistItem
import com.whip.app.domain.HabitChecklistState
import com.whip.app.domain.HabitDayProgress
import com.whip.app.domain.HabitDayState
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.HabitLog
import com.whip.app.domain.HabitLogStatus
import com.whip.app.domain.HabitPause
import com.whip.app.domain.HabitSkip
import com.whip.app.domain.HabitScheduleType
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.HabitTimerBoundary
import com.whip.app.domain.HabitTimerReviewResolution
import com.whip.app.domain.HabitTimerStartOutcome
import com.whip.app.domain.HabitTimerStartRequest
import com.whip.app.domain.HabitTimerStopOutcome
import com.whip.app.domain.TargetPeriod
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.MetricDefinition
import com.whip.app.domain.MetricEntry
import com.whip.app.domain.MetricEntryStatus
import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.habitStreak
import com.whip.app.domain.hasEnded
import com.whip.app.domain.completionRateOverRecentPeriods
import com.whip.app.domain.flexiblePeriodStreak
import com.whip.app.domain.isScheduledOn
import com.whip.app.domain.isNeutralDate
import com.whip.app.domain.outcomeForPeriod
import com.whip.app.domain.periodBounds
import com.whip.app.domain.flexibleProgress
import com.whip.app.domain.targetSatisfied
import com.whip.app.domain.valueForPeriod
import com.whip.app.domain.dayStateOn
import java.time.LocalDate
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
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class HabitUiState(
    val today: List<HabitDayProgress> = emptyList(),
    val all: List<HabitDayProgress> = emptyList(),
    val archived: List<Habit> = emptyList(),
    val archivedProgress: List<HabitDayProgress> = emptyList(),
    val logs: List<HabitLog> = emptyList(),
    val pauses: List<HabitPause> = emptyList(),
    val skips: List<HabitSkip> = emptyList(),
    val currentDate: LocalDate = LocalDate.now(),
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val customUnits: List<UnitDefinition> = emptyList(),
    val sourceMetrics: List<MetricDefinition> = emptyList(),
)

data class HabitTimerReviewPrompt(
    val boundary: HabitTimerBoundary,
    val habitName: String,
    val estimatedCanonicalSeconds: Double,
)

internal enum class HabitMutationKind {
    LogCreated,
    LogUpdated,
    LogDeleted,
    PauseCreated,
    PauseUpdated,
    PauseDeleted,
    SkipDeleted,
    ValueUnchanged,
}

internal data class HabitMutationReceipt(
    val kind: HabitMutationKind,
    val habitId: Long,
    val logId: Long? = null,
    val pauseId: Long? = null,
    val effectiveDate: LocalDate? = null,
    val warnings: List<String> = emptyList(),
)

internal class CommittedHabitMutationCancellation(
    val receipt: HabitMutationReceipt,
    cause: CancellationException,
) : CancellationException(cause.message) {
    init { initCause(cause) }
}

internal suspend fun completeCommittedHabitMutation(
    commit: suspend () -> HabitMutationReceipt,
    followUp: suspend (HabitMutationReceipt) -> HabitMutationReceipt,
): HabitMutationReceipt = completeCommittedPersistence(
    commit = commit,
    followUp = followUp,
    onCancellation = { committed, cancelled -> CommittedHabitMutationCancellation(committed, cancelled) },
    onOrdinaryFailure = { committed ->
        val subject = if (committed.kind in setOf(
                HabitMutationKind.PauseCreated,
                HabitMutationKind.PauseUpdated,
                HabitMutationKind.PauseDeleted,
            )
        ) "schedule change" else "history change"
        committed.copy(
            warnings = committed.warnings +
                "Some post-save updates did not finish; the Habit $subject itself was saved.",
        )
    },
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HabitViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as WhipApplication
    private val repository: HabitRepository = app.habitRepository
    private val clock = app.clock
    private val reminders = app.habitReminderScheduler
    private val _operationStatus = MutableStateFlow<OperationStatus>(OperationStatus.Idle)
    val operationStatus: StateFlow<OperationStatus> = _operationStatus.asStateFlow()
    private val _timerReviewPrompt = MutableStateFlow<HabitTimerReviewPrompt?>(null)
    val timerReviewPrompt: StateFlow<HabitTimerReviewPrompt?> = _timerReviewPrompt.asStateFlow()
    private val _editorSaveState = MutableStateFlow<PersistenceRequestState<EntitySaveReceipt>>(
        PersistenceRequestState.Idle,
    )
    val editorSaveState: StateFlow<PersistenceRequestState<EntitySaveReceipt>> = _editorSaveState.asStateFlow()
    private val _authoredMutationState = MutableStateFlow<PersistenceRequestState<HabitMutationReceipt>>(
        PersistenceRequestState.Idle,
    )
    internal val authoredMutationState: StateFlow<PersistenceRequestState<HabitMutationReceipt>> =
        _authoredMutationState.asStateFlow()
    private val reloadKey = MutableStateFlow(0)

    init {
        viewModelScope.launch { runCatching { reminders.syncAll() } }
        viewModelScope.launch {
            app.userDataGeneration.drop(1).collect {
                _editorSaveState.value = PersistenceRequestState.Idle
                _authoredMutationState.value = PersistenceRequestState.Idle
                _operationStatus.value = OperationStatus.Idle
                _timerReviewPrompt.value = null
            }
        }
    }

    private val pausesAndSkips = combine(repository.pauses, repository.skips, ::Pair)

    private val habitData = combine(
        repository.habits,
        repository.checklistItems,
        repository.logs,
        repository.checklistStates,
        pausesAndSkips,
    ) { habits, items, logs, states, neutral -> HabitData(habits, items, logs, states, neutral.first, neutral.second) }

    private val habitUiState = combine(
        habitData,
        app.calendarContext,
        app.measurementRepository.metrics,
        app.measurementRepository.entries,
        app.measurementRepository.customUnits,
    ) { data, calendar, metrics, metricEntries, customUnits ->
        val mirrored = mirrorMetricEntriesAsHabitLogs(data.habits, metricEntries, customUnits)
        buildHabitUiState(data.copy(logs = data.logs + mirrored), calendar.logicalDate, customUnits).copy(
            sourceMetrics = metrics.filter { it.id.startsWith("health-connect-") && !it.archived },
            customUnits = customUnits,
        )
    }

    val uiState = reloadKey.flatMapLatest {
        habitUiState.catch { error ->
            emit(HabitUiState(currentDate = clock.today(), loading = false, errorMessage = error.message ?: "Could not load habits"))
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HabitUiState(currentDate = clock.today()),
    )

    fun consumeOperationStatus() { _operationStatus.value = OperationStatus.Idle }
    fun consumeEditorSaveResult(requestId: String) {
        if ((_editorSaveState.value as? PersistenceRequestState.Finished)?.requestId == requestId) {
            _editorSaveState.value = PersistenceRequestState.Idle
        }
    }
    fun consumeAuthoredMutationResult(requestId: String) {
        if ((_authoredMutationState.value as? PersistenceRequestState.Finished)?.requestId == requestId) {
            _authoredMutationState.value = PersistenceRequestState.Idle
        }
    }
    fun retryLoading() { reloadKey.value++ }
    fun defaultSettings() = app.settingsRepository.current()
    fun saveHabit(
        id: Long?,
        draft: HabitDraft,
        requestId: String? = null,
        onFinished: (Boolean) -> Unit = {},
    ): Boolean {
        if (requestId != null && !_editorSaveState.tryStartPersistenceRequest(requestId)) {
            onFinished(false)
            return false
        }
        runEntitySaveOperation(
            if (id == null) "Creating habit…" else "Saving habit…",
            if (id == null) "Habit created" else "Habit saved",
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
                            val saved = requireNotNull(repository.get(savedId)) { "Saved Habit could not be reread" }
                            resolvedAreaId = saved.areaId
                            areaVerified = true
                        },
                        saveFollowUpWarning("Some tag suggestions did not refresh. Saving again will retry them.") {
                            draft.tags.forEach { app.measurementRepository.ensureTag(it) }
                        },
                        saveFollowUpWarning("Reminder refresh did not finish. Reopen and save the Habit to retry.") {
                            reminders.syncHabit(savedId)
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
    fun duplicate(id: Long) = runOperation("Duplicating habit…", "Habit duplicated") { reminders.syncHabit(repository.duplicate(id)) }
    fun setArchived(id: Long, archived: Boolean) = runOperation(
        if (archived) "Archiving habit…" else "Restoring habit…",
        if (archived) "Habit archived" else "Habit restored",
    ) { repository.setArchived(id, archived); reminders.syncHabit(id) }
    fun deletePermanently(id: Long) = runOperation(
        "Deleting habit…",
        "Habit permanently deleted",
        successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
    ) {
        app.domainDeletionCoordinator.deleteHabit(id)
        reminders.syncHabit(id)
    }
    fun setPinned(id: Long, pinned: Boolean) = runOperation(
        "Updating Home priority…",
        if (pinned) "Habit pinned · first on Whip Home when due" else "Habit unpinned from Whip Home",
        successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
    ) {
        repository.setPinned(id, pinned)
        if (pinned) app.settingsRepository.revealHomeSection(HomeSection.Habits)
    }
    fun reorder(ids: List<Long>) = runSilentReorder { repository.reorder(ids) }
    fun setPaused(id: Long, paused: Boolean) = runOperation("Updating habit…", if (paused) "Habit paused" else "Habit resumed") { repository.setPaused(id, paused); reminders.syncHabit(id) }
    fun addPause(
        id: Long,
        start: LocalDate,
        end: LocalDate?,
        note: String,
        requestId: String? = null,
    ): Boolean = runAuthoredMutation(
        running = "Scheduling pause…",
        success = "Pause scheduled",
        requestId = requestId,
        savedDescription = "scheduled pause",
    ) {
        completeCommittedHabitMutation(
            commit = {
                val pauseId = repository.addPause(id, start, end, note)
                HabitMutationReceipt(HabitMutationKind.PauseCreated, id, pauseId = pauseId, effectiveDate = start)
            },
            followUp = { committed -> committed.withReminderRefresh(reminders, id) },
        )
    }
    fun updatePause(
        pauseId: Long,
        habitId: Long,
        start: LocalDate,
        end: LocalDate?,
        note: String,
        requestId: String? = null,
    ): Boolean = runAuthoredMutation(
        running = "Saving scheduled pause…",
        success = "Scheduled pause updated",
        requestId = requestId,
        savedDescription = "scheduled pause",
    ) {
        completeCommittedHabitMutation(
            commit = {
                val savedHabitId = repository.updatePause(
                    pauseId,
                    start,
                    end,
                    note,
                    expectedHabitId = habitId,
                )
                HabitMutationReceipt(
                    HabitMutationKind.PauseUpdated,
                    savedHabitId,
                    pauseId = pauseId,
                    effectiveDate = start,
                )
            },
            followUp = { committed -> committed.withReminderRefresh(reminders, committed.habitId) },
        )
    }
    fun deletePause(
        pauseId: Long,
        habitId: Long,
        requestId: String? = null,
    ): Boolean = runAuthoredMutation(
        running = "Deleting scheduled pause…",
        success = "Scheduled pause deleted",
        requestId = requestId,
        savedDescription = "scheduled pause deletion",
    ) {
        completeCommittedHabitMutation(
            commit = {
                val savedHabitId = repository.deletePause(pauseId, expectedHabitId = habitId)
                HabitMutationReceipt(HabitMutationKind.PauseDeleted, savedHabitId, pauseId = pauseId)
            },
            followUp = { committed -> committed.withReminderRefresh(reminders, committed.habitId) },
        )
    }
    fun skipDay(habitId: Long, date: LocalDate) = runOperation(
        "Skipping today…",
        "Today skipped · streak protected",
        successFeedbackPresentation = OperationFeedbackPresentation.Inline,
    ) {
        repository.skipDay(habitId, date)
        reminders.syncHabit(habitId)
    }
    fun undoSkip(
        habitId: Long,
        date: LocalDate,
        requestId: String? = null,
    ): Boolean = runAuthoredMutation(
        running = "Restoring scheduled day…",
        success = "Skip undone",
        requestId = requestId,
        savedDescription = "skipped-day change",
    ) {
        completeCommittedHabitMutation(
            commit = {
                repository.undoSkip(habitId, date)
                HabitMutationReceipt(HabitMutationKind.SkipDeleted, habitId, effectiveDate = date)
            },
            followUp = { committed -> committed.withReminderRefresh(reminders, habitId) },
        )
    }
    fun log(
        habitId: Long,
        value: Double?,
        status: HabitLogStatus = HabitLogStatus.Recorded,
        date: LocalDate? = null,
        note: String = "",
        requestId: String? = null,
    ): Boolean = runAuthoredMutation(
        running = "Saving check-in…",
        success = "Habit logged",
        requestId = requestId,
        savedDescription = "check-in",
    ) {
        completeCommittedHabitMutation(
            commit = {
                val logId = repository.log(habitId, value, status, date, note = note)
                HabitMutationReceipt(
                    HabitMutationKind.LogCreated,
                    habitId,
                    logId = logId,
                    effectiveDate = date ?: clock.today(),
                )
            },
            followUp = { committed -> committed.withReminderRefresh(reminders, habitId) },
        )
    }
    fun addValue(item: HabitDayProgress, amount: Double) {
        if (!amount.isFinite() || amount == 0.0) return
        log(item.habit.id, amount, date = item.date)
    }
    fun decrementValue(item: HabitDayProgress) {
        val amount = minOf(item.habit.quickIncrement, item.value.coerceAtLeast(0.0))
        if (amount > 0.0) log(item.habit.id, -amount, date = item.date)
    }
    fun setPeriodValue(
        item: HabitDayProgress,
        value: Double,
        note: String = "",
        requestId: String? = null,
    ): Boolean {
        if (!value.isFinite()) return false
        if (item.habit.trackingMode !in setOf(
                HabitTrackingMode.Count,
                HabitTrackingMode.Decimal,
                HabitTrackingMode.Duration,
            )
        ) return log(item.habit.id, value, date = item.date, note = note, requestId = requestId)
        return runAuthoredMutation(
            running = "Saving check-in…",
            success = "Habit value saved",
            requestId = requestId,
            savedDescription = "check-in",
        ) {
            completeCommittedHabitMutation(
                commit = {
                    val logId = repository.setPeriodValue(item.habit.id, item.date, value, note)
                    HabitMutationReceipt(
                        kind = if (logId == null) HabitMutationKind.ValueUnchanged else HabitMutationKind.LogCreated,
                        habitId = item.habit.id,
                        logId = logId,
                        effectiveDate = item.date,
                    )
                },
                followUp = { committed ->
                    if (committed.logId == null) committed else committed.withReminderRefresh(reminders, item.habit.id)
                },
            )
        }
    }
    fun undoLog(logId: Long, habitId: Long? = null, requestId: String? = null): Boolean = runAuthoredMutation(
        running = "Undoing check-in…",
        success = "Check-in removed",
        requestId = requestId,
        savedDescription = "check-in deletion",
    ) {
        completeCommittedHabitMutation(
            commit = {
                val resolvedHabitId = repository.undoLog(logId, expectedHabitId = habitId)
                HabitMutationReceipt(HabitMutationKind.LogDeleted, resolvedHabitId, logId = logId)
            },
            followUp = { committed -> committed.withReminderRefresh(reminders, committed.habitId) },
        )
    }
    fun updateLog(
        logId: Long,
        value: Double?,
        status: HabitLogStatus,
        date: LocalDate,
        note: String,
        expectedHabitId: Long? = null,
        requestId: String? = null,
    ): Boolean = runAuthoredMutation(
        running = "Updating check-in…",
        success = "Check-in updated",
        requestId = requestId,
        savedDescription = "check-in",
    ) {
        completeCommittedHabitMutation(
            commit = {
                val habitId = repository.updateLog(
                    logId,
                    value,
                    status,
                    date,
                    note,
                    expectedHabitId = expectedHabitId,
                )
                HabitMutationReceipt(HabitMutationKind.LogUpdated, habitId, logId = logId, effectiveDate = date)
            },
            followUp = { committed -> committed.withReminderRefresh(reminders, committed.habitId) },
        )
    }
    fun setCheckOff(habitId: Long, date: LocalDate, completed: Boolean) =
        runOperation(
            "Updating habit…",
            if (completed) "Habit completed" else "Completion removed",
            successFeedbackPresentation = OperationFeedbackPresentation.Inline,
        ) {
            repository.setCheckOff(habitId, date, completed)
            reminders.syncHabit(habitId)
        }
    fun toggleChecklist(habitId: Long, itemId: Long, date: LocalDate, completed: Boolean) =
        runOperation(
            "Updating checklist…",
            "Checklist updated",
            successFeedbackPresentation = OperationFeedbackPresentation.Inline,
        ) {
            repository.toggleChecklistItem(habitId, itemId, date, completed)
            reminders.syncHabit(habitId)
        }
    fun startTimer(habitId: Long) {
        _operationStatus.value = OperationStatus.Running("Starting timer…")
        viewModelScope.launch {
            try {
                val outcome = checkNotNull(app.withUserDataAccess {
                    val habit = repository.get(habitId) ?: error("Habit no longer exists")
                    repository.startTimer(
                        HabitTimerStartRequest(habit.id, habit.uuid, app.idGenerator.nextId()),
                    )
                }) { "Whip data is unavailable while recovery is in progress" }
                _operationStatus.value = when (outcome) {
                    is HabitTimerStartOutcome.Started -> OperationStatus.Succeeded(
                        if (outcome.needsReview) "Timer needs review before it can be logged" else "Habit timer started",
                        OperationFeedbackPresentation.Inline,
                    )
                    is HabitTimerStartOutcome.AlreadyRunning -> OperationStatus.Succeeded(
                        "Habit timer is already running",
                        OperationFeedbackPresentation.Inline,
                    )
                    HabitTimerStartOutcome.AlreadyResolved -> OperationStatus.Failed(
                        "This timer action was already used. Start a new timer.",
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _operationStatus.value = OperationStatus.Failed(error.message ?: "Could not start the timer", error)
            }
        }
    }

    fun stopTimer(habitId: Long) {
        _operationStatus.value = OperationStatus.Running("Stopping timer…")
        viewModelScope.launch {
            try {
                val habit = repository.get(habitId) ?: error("Habit no longer exists")
                val sessionId = habit.timerSessionId ?: error("Timer is not running")
                val outcome = checkNotNull(app.withUserDataAccess {
                    repository.stopTimer(HabitTimerBoundary(habit.id, habit.uuid, sessionId))
                }) { "Whip data is unavailable while recovery is in progress" }
                handleTimerStopOutcome(habit, outcome)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _operationStatus.value = OperationStatus.Failed(
                    error.message ?: "Couldn't stop the timer. It is still running.",
                    error,
                )
            }
        }
    }

    fun resolveTimerReview(canonicalSeconds: Double, continueTimer: Boolean) {
        val prompt = _timerReviewPrompt.value ?: return
        _operationStatus.value = OperationStatus.Running(if (continueTimer) "Continuing timer…" else "Logging duration…")
        viewModelScope.launch {
            try {
                val habit = repository.get(prompt.boundary.habitId) ?: error("Habit no longer exists")
                val outcome = checkNotNull(app.withUserDataAccess {
                    repository.resolveTimerReview(
                        prompt.boundary,
                        if (continueTimer) HabitTimerReviewResolution.Continue(canonicalSeconds)
                        else HabitTimerReviewResolution.StopAndLog(canonicalSeconds),
                    )
                }) { "Whip data is unavailable while recovery is in progress" }
                _timerReviewPrompt.value = null
                handleTimerStopOutcome(habit, outcome)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _operationStatus.value = OperationStatus.Failed(error.message ?: "Could not resolve the timer", error)
            }
        }
    }

    fun discardTimer() {
        val prompt = _timerReviewPrompt.value ?: return
        _operationStatus.value = OperationStatus.Running("Discarding timer…")
        viewModelScope.launch {
            try {
                checkNotNull(app.withUserDataAccess {
                    repository.resolveTimerReview(prompt.boundary, HabitTimerReviewResolution.Discard)
                }) { "Whip data is unavailable while recovery is in progress" }
                _timerReviewPrompt.value = null
                _operationStatus.value = OperationStatus.Succeeded(
                    "Timer discarded; no duration was logged",
                    OperationFeedbackPresentation.Snackbar,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _operationStatus.value = OperationStatus.Failed(error.message ?: "Could not discard the timer", error)
            }
        }
    }

    fun dismissTimerReview() {
        _timerReviewPrompt.value = null
        _operationStatus.value = OperationStatus.Idle
    }

    private suspend fun handleTimerStopOutcome(habit: Habit, outcome: HabitTimerStopOutcome) {
        when (outcome) {
            is HabitTimerStopOutcome.ReviewRequired -> {
                _timerReviewPrompt.value = HabitTimerReviewPrompt(
                    outcome.boundary,
                    habit.name,
                    outcome.estimatedCanonicalSeconds,
                )
                _operationStatus.value = OperationStatus.Succeeded("Review the timer duration", OperationFeedbackPresentation.Inline)
            }
            is HabitTimerStopOutcome.Stopped -> {
                val warning = saveFollowUpWarning(
                    "Duration was logged, but reminders did not refresh.",
                ) { reminders.syncHabit(habit.id) }
                val duration = formatElapsedDurationSpoken(outcome.canonicalSeconds)
                _operationStatus.value = OperationStatus.Succeeded(
                    if (outcome.logId == null) "Timer stopped; no duration was logged"
                    else "Logged $duration for ${habit.name}.${warning?.let { " $it" }.orEmpty()}",
                    OperationFeedbackPresentation.Snackbar,
                )
            }
            is HabitTimerStopOutcome.AlreadyCompleted -> _operationStatus.value = OperationStatus.Succeeded(
                if (outcome.historyPresent) "Duration was already logged" else "Timer was already resolved",
                OperationFeedbackPresentation.Inline,
            )
            HabitTimerStopOutcome.AlreadyDiscarded,
            HabitTimerStopOutcome.Discarded,
            -> _operationStatus.value = OperationStatus.Succeeded("Timer was discarded", OperationFeedbackPresentation.Inline)
            is HabitTimerStopOutcome.Continued -> _operationStatus.value = OperationStatus.Succeeded(
                "Timer continued",
                OperationFeedbackPresentation.Inline,
            )
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

    private fun runAuthoredMutation(
        running: String,
        success: String,
        requestId: String?,
        savedDescription: String,
        block: suspend () -> HabitMutationReceipt,
    ): Boolean {
        if (requestId != null && !_authoredMutationState.tryStartPersistenceRequest(requestId)) return false
        _operationStatus.value = OperationStatus.Running(running)
        viewModelScope.launch {
            fun successResult(receipt: HabitMutationReceipt): WhipResult.Success<HabitMutationReceipt> {
                val message = if (receipt.warnings.isEmpty()) success else {
                    "$success · ${receipt.warnings.joinToString(" ")}"
                }
                _operationStatus.value = OperationStatus.Succeeded(
                    message,
                    if (requestId == null) OperationFeedbackPresentation.Inline
                    else OperationFeedbackPresentation.Snackbar,
                )
                return WhipResult.Success(receipt)
            }
            val result = try {
                val receipt = checkNotNull(app.withUserDataAccess { block() }) {
                    "Whip data is unavailable while recovery is in progress"
                }
                successResult(receipt)
            } catch (cancelled: CommittedHabitMutationCancellation) {
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
                        "The $savedDescription save was interrupted. Your changes are still here.",
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
                                "Some post-save updates were interrupted; the Habit itself was saved.",
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
                    WhipResult.Failure("The Habit save was interrupted. Your changes are still here.", cancelled)
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
                WhipResult.Failure(error.message ?: "The habit could not be saved.", error)
            }
            runCatching { onFinished(result) }
        }
    }
}

private suspend fun HabitMutationReceipt.withReminderRefresh(
    scheduler: HabitReminderScheduler,
    habitId: Long,
): HabitMutationReceipt {
    val subject = if (kind in setOf(
            HabitMutationKind.PauseCreated,
            HabitMutationKind.PauseUpdated,
            HabitMutationKind.PauseDeleted,
        )
    ) "schedule change" else "history change"
    val warning = saveFollowUpWarning(
        "Reminder refresh did not finish. The $subject was saved. Open Edit Habit and save once to refresh reminders.",
    ) {
        scheduler.syncHabit(habitId)
    }
    return if (warning == null) this else copy(warnings = warnings + warning)
}

internal fun mirrorMetricEntriesAsHabitLogs(
    habits: List<Habit>,
    entries: List<MetricEntry>,
    customUnits: List<UnitDefinition> = emptyList(),
): List<HabitLog> = habits.filter { it.sourceMetricId != null }.flatMap { habit ->
    entries.asSequence().filter { it.metricId == habit.sourceMetricId }.mapNotNull { entry ->
        val value = when {
            entry.enteredValue != null && entry.enteredUnitId == habit.unitId -> entry.enteredValue
            entry.canonicalValue != null -> (BuiltInUnits.get(habit.unitId) ?: customUnits.firstOrNull { it.id == habit.unitId })
                ?.fromCanonical(entry.canonicalValue) ?: entry.canonicalValue
            else -> null
        }
        val stable = ("${habit.id}:${entry.id}".hashCode().toLong() and 0x7fff_ffffL).let { if (it == 0L) -1L else -it }
        val status = when (entry.status) {
            MetricEntryStatus.Recorded -> HabitLogStatus.Recorded
            MetricEntryStatus.Failed -> HabitLogStatus.Failed
            MetricEntryStatus.Missing, MetricEntryStatus.Skipped, MetricEntryStatus.Excused -> return@mapNotNull null
        }
        HabitLog(
            id = stable,
            uuid = "metric:${habit.id}:${entry.id}",
            habitId = habit.id,
            value = value,
            canonicalValue = entry.canonicalValue,
            enteredUnitId = habit.unitId,
            status = status,
            timestamp = entry.timestamp,
            localDate = entry.localDate,
            zoneId = entry.zoneId,
            offsetSeconds = entry.offsetSeconds,
            note = entry.note,
            sourceType = entry.sourceType,
            sourceId = entry.sourceId,
            metricEntryId = entry.id,
            createdAtMillis = entry.createdAtMillis,
            updatedAtMillis = entry.updatedAtMillis,
        )
    }.toList()
}

private data class HabitData(
    val habits: List<Habit>,
    val items: List<HabitChecklistItem>,
    val logs: List<HabitLog>,
    val states: List<HabitChecklistState>,
    val pauses: List<HabitPause>,
    val skips: List<HabitSkip>,
)

private fun buildHabitUiState(data: HabitData, today: LocalDate, customUnits: List<UnitDefinition>): HabitUiState {
    // An unresolved timer stays reachable even when legacy/restored state says archived,
    // paused, ended, or not scheduled today. New archive/pause operations are blocked first.
    val active = data.habits.filter { !it.archived || it.timerSessionId != null }
    val progress = active.map { habit -> buildProgress(habit, data, today, customUnits) }
    val archived = data.habits.filter(Habit::archived)
    return HabitUiState(
        today = progress.filter { it.scheduled || it.habit.timerSessionId != null },
        all = progress,
        archived = archived,
        archivedProgress = archived.map { habit -> buildProgress(habit, data, today, customUnits) },
        logs = data.logs,
        pauses = data.pauses,
        skips = data.skips,
        currentDate = today,
        loading = false,
    )
}

private fun buildProgress(
    habit: Habit,
    data: HabitData,
    date: LocalDate,
    customUnits: List<UnitDefinition>,
): HabitDayProgress {
    val habitLogs = data.logs.filter { it.habitId == habit.id }
    val habitPauses = data.pauses.filter { it.habitId == habit.id }
    val habitSkips = data.skips.filter { it.habitId == habit.id }
    val flexibleProgress = habit.flexibleProgress(habitLogs, date, habitPauses, habitSkips)
    val weekCompletions = flexibleProgress?.completed.takeIf { habit.scheduleType == HabitScheduleType.FlexibleTimesPerWeek } ?: 0
    val monthCompletions = flexibleProgress?.completed.takeIf { habit.scheduleType == HabitScheduleType.FlexibleTimesPerMonth } ?: 0
    val status = habitLogs.filter { it.localDate == date }.maxByOrNull(HabitLog::timestamp)?.status
    val successByDate = (0L..365L).associate { offset ->
        val day = date.minusDays(offset)
        day to habit.outcomeForPeriod(habitLogs, day, customUnits)
    }
    val completionRate = habit.completionRateOverRecentPeriods(
        habitLogs,
        date,
        pauses = habitPauses,
        customUnits = customUnits,
        skips = habitSkips,
    )
    val streak = if (habit.scheduleType in setOf(HabitScheduleType.FlexibleTimesPerWeek, HabitScheduleType.FlexibleTimesPerMonth)) {
        habit.flexiblePeriodStreak(habitLogs, date, habitPauses, habitSkips)
    } else {
        val neutralDates = (0L..365L).mapNotNullTo(mutableSetOf()) { offset ->
            date.minusDays(offset).takeIf { habit.isNeutralDate(it, habitPauses, habitSkips) }
        }
        habitStreak(habit, date, successByDate, neutralDates)
    }
    val ended = habit.hasEnded(habitLogs, date, habitPauses, customUnits, habitSkips)
    val explicitlyPaused = data.pauses.any { it.habitId == habit.id && !date.isBefore(it.startDate) && (it.endDate == null || !date.isAfter(it.endDate)) }
    val items = data.items.filter { it.habitId == habit.id && !it.archived }.map { item ->
        val completed = data.states.firstOrNull { it.habitId == habit.id && it.itemId == item.id && it.localDate == date }?.completed == true
        item to completed
    }
    return HabitDayProgress(
        habit = habit,
        date = date,
        scheduled = !ended && !explicitlyPaused && habit.isScheduledOn(date, weekCompletions, monthCompletions),
        value = habit.valueForPeriod(habitLogs, date, customUnits),
        status = status,
        successful = habit.outcomeForPeriod(habitLogs, date, customUnits),
        checklistItems = items,
        streak = streak,
        completionRate = completionRate,
        flexibleScheduleProgress = flexibleProgress?.completed,
        flexibleScheduleTarget = flexibleProgress?.target,
        dayState = habit.dayStateOn(date, date, habitLogs, habitPauses, habitSkips, customUnits),
    )
}
