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
import com.whip.app.core.WhipClock
import com.whip.app.data.HabitRepository
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
import com.whip.app.reminders.reminderDefinitionChanged
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
import kotlinx.coroutines.flow.stateIn
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

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HabitViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as WhipApplication
    private val repository: HabitRepository = app.habitRepository
    private val clock = app.clock
    private val reminders = app.habitReminderScheduler
    private val _operationStatus = MutableStateFlow<OperationStatus>(OperationStatus.Idle)
    val operationStatus: StateFlow<OperationStatus> = _operationStatus.asStateFlow()
    private val reloadKey = MutableStateFlow(0)

    init { viewModelScope.launch { runCatching { reminders.syncAll() } } }

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
        app.settingsRepository.currentDateFlow(clock),
        app.measurementRepository.metrics,
        app.measurementRepository.entries,
        app.measurementRepository.customUnits,
    ) { data, today, metrics, metricEntries, customUnits ->
        val mirrored = mirrorMetricEntriesAsHabitLogs(data.habits, metricEntries, customUnits)
        buildHabitUiState(data.copy(logs = data.logs + mirrored), today, customUnits).copy(
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
    fun retryLoading() { reloadKey.value++ }
    fun defaultSettings() = app.settingsRepository.current()
    fun saveHabit(id: Long?, draft: HabitDraft, onFinished: (Boolean) -> Unit = {}) = runOperation(
        if (id == null) "Creating habit…" else "Saving habit…",
        if (id == null) "Habit created" else "Habit saved",
        onFinished,
    ) {
        val existing = id?.let { repository.get(it) }
        val savedId = if (id == null) repository.create(draft) else { repository.update(id, draft); id }
        if (existing == null || existing.reminderDefinitionChanged(draft)) reminders.syncHabit(savedId)
        draft.tags.filter { tag -> existing?.tags?.none { it.equals(tag, ignoreCase = true) } != false }
            .forEach { app.measurementRepository.ensureTag(it) }
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
    fun addPause(id: Long, start: LocalDate, end: LocalDate?, note: String) = runOperation("Scheduling pause…", "Pause scheduled") {
        repository.addPause(id, start, end, note)
        reminders.syncHabit(id)
    }
    fun skipDay(habitId: Long, date: LocalDate) = runOperation(
        "Skipping today…",
        "Today skipped · streak protected",
        successFeedbackPresentation = OperationFeedbackPresentation.Inline,
    ) {
        repository.skipDay(habitId, date)
        reminders.syncHabit(habitId)
    }
    fun undoSkip(habitId: Long, date: LocalDate) = runOperation(
        "Restoring today…",
        "Skip undone",
        successFeedbackPresentation = OperationFeedbackPresentation.Inline,
    ) {
        repository.undoSkip(habitId, date)
        reminders.syncHabit(habitId)
    }
    fun log(habitId: Long, value: Double?, status: HabitLogStatus = HabitLogStatus.Recorded, date: LocalDate? = null, note: String = "") =
        runOperation(
            "Saving check-in…",
            "Habit logged",
            successFeedbackPresentation = OperationFeedbackPresentation.Inline,
        ) {
            repository.log(habitId, value, status, date, note = note)
            reminders.syncHabit(habitId)
        }
    fun addValue(item: HabitDayProgress, amount: Double) {
        if (!amount.isFinite() || amount == 0.0) return
        log(item.habit.id, amount, date = item.date)
    }
    fun decrementValue(item: HabitDayProgress) {
        val amount = minOf(item.habit.quickIncrement, item.value.coerceAtLeast(0.0))
        if (amount > 0.0) log(item.habit.id, -amount, date = item.date)
    }
    fun setPeriodValue(item: HabitDayProgress, value: Double, note: String = "") {
        if (!value.isFinite()) return
        val loggedValue = when (item.habit.trackingMode) {
            HabitTrackingMode.Count, HabitTrackingMode.Decimal, HabitTrackingMode.Duration -> value - item.value
            else -> value
        }
        if (loggedValue != 0.0 || note.isNotBlank()) log(item.habit.id, loggedValue, date = item.date, note = note)
    }
    fun undoLog(logId: Long, habitId: Long? = null) = runOperation(
        "Undoing check-in…",
        "Check-in removed",
        successFeedbackPresentation = OperationFeedbackPresentation.Inline,
    ) {
        repository.undoLog(logId)
        habitId?.let { reminders.syncHabit(it) }
    }
    fun updateLog(logId: Long, value: Double?, status: HabitLogStatus, date: LocalDate, note: String) =
        runOperation(
            "Updating check-in…",
            "Check-in updated",
            successFeedbackPresentation = OperationFeedbackPresentation.Inline,
        ) {
            repository.updateLog(logId, value, status, date, note)
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
    fun startTimer(habitId: Long) = runOperation("Starting timer…", "Habit timer started") { repository.startTimer(habitId) }
    fun stopTimer(habitId: Long) = runOperation("Stopping timer…", "Duration logged") { repository.stopTimer(habitId) }
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
            note = entry.note.ifBlank { "Mirrored from ${entry.sourceType.name}" },
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
    val active = data.habits.filterNot(Habit::archived)
    val progress = active.map { habit -> buildProgress(habit, data, today, customUnits) }
    val archived = data.habits.filter(Habit::archived)
    return HabitUiState(
        today = progress.filter { it.scheduled },
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
