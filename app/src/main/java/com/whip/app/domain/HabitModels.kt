package com.whip.app.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class HabitTrackingMode { CheckOff, Count, Decimal, Duration, Checklist, Rating, LogOnly }
enum class TargetComparison { AtLeast, AtMost, Exactly, WithinRange, None }
enum class TargetPeriod { Occurrence, Day, Week, Month, RollingDays }
enum class HabitScheduleType { Daily, EveryNDays, SelectedWeekdays, FlexibleTimesPerWeek, FlexibleTimesPerMonth }
enum class HabitEndType { Never, OnDate, AfterStreak, AfterCompletions, AfterTotal }
enum class HabitLogStatus { Recorded, Success, Failed }
enum class HabitDayState { Pending, Completed, BelowTarget, Missed, Skipped, Paused, NotScheduled }
enum class HabitTimerSessionState { Running, ReviewRequired, Completed, Discarded }

data class HabitTimerBoundary(
    val habitId: Long,
    val habitUuid: String,
    val sessionId: String,
)

data class HabitTimerStartRequest(
    val habitId: Long,
    val habitUuid: String,
    val requestId: String,
)

sealed interface HabitTimerStartOutcome {
    data class Started(val boundary: HabitTimerBoundary, val needsReview: Boolean) : HabitTimerStartOutcome
    data class AlreadyRunning(val boundary: HabitTimerBoundary, val needsReview: Boolean) : HabitTimerStartOutcome
    data object AlreadyResolved : HabitTimerStartOutcome
}

sealed interface HabitTimerStopOutcome {
    data class Stopped(
        val boundary: HabitTimerBoundary,
        val canonicalSeconds: Double,
        val logId: Long?,
    ) : HabitTimerStopOutcome
    data class AlreadyCompleted(val historyPresent: Boolean) : HabitTimerStopOutcome
    data object AlreadyDiscarded : HabitTimerStopOutcome
    data class ReviewRequired(
        val boundary: HabitTimerBoundary,
        val estimatedCanonicalSeconds: Double,
    ) : HabitTimerStopOutcome
    data class Continued(val boundary: HabitTimerBoundary, val canonicalSeconds: Double) : HabitTimerStopOutcome
    data object Discarded : HabitTimerStopOutcome
}

sealed interface HabitTimerReviewResolution {
    data class StopAndLog(val canonicalSeconds: Double, val date: LocalDate? = null) : HabitTimerReviewResolution
    data class Continue(val canonicalSeconds: Double) : HabitTimerReviewResolution
    data object Discard : HabitTimerReviewResolution
}

data class HabitDraft(
    val name: String,
    val notes: String = "",
    val areaId: String? = null,
    val area: String = "",
    val tags: List<String> = emptyList(),
    val icon: String = DEFAULT_HABIT_EMOJI,
    val trackingMode: HabitTrackingMode = HabitTrackingMode.CheckOff,
    val dimension: UnitDimension = UnitDimension.Count,
    val unitId: String = "count",
    val precision: Int = 0,
    val comparison: TargetComparison = TargetComparison.AtLeast,
    val targetMin: Double? = 1.0,
    val targetMax: Double? = null,
    val targetPeriod: TargetPeriod = TargetPeriod.Day,
    val rollingDays: Int? = null,
    val scheduleType: HabitScheduleType = HabitScheduleType.Daily,
    val scheduleInterval: Int = 1,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val flexibleTimesPerWeek: Int? = null,
    val startDate: LocalDate,
    val endType: HabitEndType = HabitEndType.Never,
    val endDate: LocalDate? = null,
    val endValue: Double? = null,
    val quickIncrement: Double = 1.0,
    val quickActions: List<Double> = emptyList(),
    val reminderMinutes: List<Int> = emptyList(),
    val weekdayReminderMinutes: Map<DayOfWeek, List<Int>> = emptyMap(),
    val weekStart: DayOfWeek = DayOfWeek.MONDAY,
    val checklistItems: List<HabitChecklistItemDraft> = emptyList(),
    val autoCompleteFromItems: Boolean = true,
    /** Optional external metric mirrored into this habit (for example Health Connect steps). */
    val sourceMetricId: String? = null,
) : java.io.Serializable

/** Numeric quick-add amounts only have meaning for manually accumulated values. */
fun HabitTrackingMode.supportsQuickAddAmounts(): Boolean =
    this == HabitTrackingMode.Count || this == HabitTrackingMode.Decimal

/**
 * Removes values owned by controls that are not active for the selected Habit configuration.
 * This keeps a previously edited, now-hidden field from changing behavior or blocking a save.
 */
fun HabitDraft.withConfigurationSemantics(): HabitDraft {
    val checkBased = trackingMode in setOf(HabitTrackingMode.CheckOff, HabitTrackingMode.Checklist)
    val semanticComparison = if (checkBased) TargetComparison.AtLeast else comparison
    val semanticTargetMin = if (checkBased) 1.0 else when (semanticComparison) {
        TargetComparison.AtLeast, TargetComparison.Exactly -> targetMin ?: targetMax
        TargetComparison.WithinRange -> targetMin
        TargetComparison.AtMost, TargetComparison.None -> null
    }
    val semanticTargetMax = if (checkBased) null else when (semanticComparison) {
        TargetComparison.AtMost -> targetMax ?: targetMin
        TargetComparison.WithinRange -> targetMax
        TargetComparison.AtLeast, TargetComparison.Exactly, TargetComparison.None -> null
    }
    val flexibleSchedule = scheduleType in setOf(
        HabitScheduleType.FlexibleTimesPerWeek,
        HabitScheduleType.FlexibleTimesPerMonth,
    )
    val thresholdEnding = endType in setOf(
        HabitEndType.AfterStreak,
        HabitEndType.AfterCompletions,
        HabitEndType.AfterTotal,
    )
    return copy(
        precision = precision.takeUnless {
            sourceMetricId == null &&
                trackingMode in setOf(HabitTrackingMode.CheckOff, HabitTrackingMode.Checklist, HabitTrackingMode.Rating)
        } ?: 0,
        comparison = semanticComparison,
        targetMin = semanticTargetMin,
        targetMax = semanticTargetMax,
        targetPeriod = targetPeriod.takeUnless { checkBased } ?: TargetPeriod.Occurrence,
        rollingDays = rollingDays.takeIf {
            !checkBased && semanticComparison != TargetComparison.None && targetPeriod == TargetPeriod.RollingDays
        },
        scheduleInterval = scheduleInterval.takeIf { scheduleType == HabitScheduleType.EveryNDays } ?: 1,
        weekdays = weekdays.takeIf { scheduleType == HabitScheduleType.SelectedWeekdays }.orEmpty(),
        flexibleTimesPerWeek = flexibleTimesPerWeek.takeIf { flexibleSchedule },
        endDate = endDate.takeIf { endType == HabitEndType.OnDate },
        endValue = endValue.takeIf { thresholdEnding },
        checklistItems = checklistItems.takeIf { trackingMode == HabitTrackingMode.Checklist }.orEmpty(),
        autoCompleteFromItems = autoCompleteFromItems.takeIf {
            trackingMode == HabitTrackingMode.Checklist
        } ?: true,
    )
}

/** One validation contract shared by the editor and persistence layer. */
fun HabitDraft.validationErrors(): List<String> = buildList {
    if (name.isBlank()) add("Habit name is required")
    if (name.length > 100) add("Habit names can be at most 100 characters")
    if (tags.any { ',' in it }) add("Use separate Tags instead of commas")
    if (scheduleType == HabitScheduleType.EveryNDays && scheduleInterval <= 0) {
        add("Schedule interval must be a positive whole number")
    }
    if (sourceMetricId == null && trackingMode.supportsQuickAddAmounts()) {
        if (!quickIncrement.isFinite() || quickIncrement <= 0.0) add("Quick increment must be a positive number")
        if (quickActions.any { !it.isFinite() || it < 0.0 }) add("Quick actions must be non-negative numbers")
    }
    if (
        sourceMetricId == null &&
        trackingMode !in setOf(HabitTrackingMode.CheckOff, HabitTrackingMode.Checklist, HabitTrackingMode.Rating) &&
        precision !in 0..6
    ) add("Decimal places must be between 0 and 6")
    if (reminderMinutes.any { it !in 0..1439 } || weekdayReminderMinutes.values.flatten().any { it !in 0..1439 }) {
        add("Reminder times must be valid times of day")
    }
    if (trackingMode == HabitTrackingMode.Checklist) {
        val checklistIds = checklistItems.mapNotNull(HabitChecklistItemDraft::id)
        val checklistUuids = checklistItems.mapNotNull(HabitChecklistItemDraft::uuid)
        if (checklistIds.distinct().size != checklistIds.size || checklistUuids.distinct().size != checklistUuids.size) {
            add("Checklist item identities must be unique")
        }
    }
    if (trackingMode == HabitTrackingMode.Duration && dimension != UnitDimension.Duration) {
        add("Duration tracking requires a Duration unit")
    }
    if (trackingMode == HabitTrackingMode.Checklist && checklistItems.none { it.name.isNotBlank() }) {
        add("Add at least one checklist item")
    }
    if (scheduleType == HabitScheduleType.SelectedWeekdays && weekdays.isEmpty()) add("Pick at least one weekday")
    if (
        scheduleType in setOf(HabitScheduleType.FlexibleTimesPerWeek, HabitScheduleType.FlexibleTimesPerMonth) &&
        (flexibleTimesPerWeek ?: 0) <= 0
    ) add("Flexible schedule count must be a positive whole number")
    val activeTargets = when (comparison) {
        TargetComparison.AtLeast, TargetComparison.Exactly -> listOfNotNull(targetMin)
        TargetComparison.AtMost -> listOfNotNull(targetMax)
        TargetComparison.WithinRange -> listOfNotNull(targetMin, targetMax)
        TargetComparison.None -> emptyList()
    }
    if (activeTargets.any { !it.isFinite() }) add("Habit targets must be valid numbers")
    when (comparison) {
        TargetComparison.AtLeast, TargetComparison.Exactly -> if (targetMin == null) add("Enter a target")
        TargetComparison.AtMost -> if (targetMax == null) add("Enter a maximum")
        TargetComparison.WithinRange -> if (targetMin == null || targetMax == null || targetMin > targetMax) {
            add("Enter a valid target range")
        }
        TargetComparison.None -> Unit
    }
    if (
        comparison != TargetComparison.None && targetPeriod == TargetPeriod.RollingDays &&
        (rollingDays ?: 0) <= 0
    ) add("Enter a positive rolling window")
    when (endType) {
        HabitEndType.Never -> Unit
        HabitEndType.OnDate -> if (endDate == null || endDate.isBefore(startDate)) {
            add("Choose an end date on or after the start date")
        }
        HabitEndType.AfterStreak, HabitEndType.AfterCompletions -> if (
            endValue?.let { it.isFinite() && it > 0.0 && it % 1.0 == 0.0 } != true
        ) add("Enter a positive whole-number ending threshold")
        HabitEndType.AfterTotal -> if (endValue?.let { it.isFinite() && it > 0.0 } != true) {
            add("Enter a positive ending total")
        }
    }
}

data class Habit(
    val id: Long,
    val uuid: String,
    val metricId: String,
    val name: String,
    val notes: String,
    val areaId: String? = null,
    val area: String,
    val tags: List<String>,
    val icon: String,
    val trackingMode: HabitTrackingMode,
    val dimension: UnitDimension,
    val unitId: String,
    val precision: Int,
    val comparison: TargetComparison,
    val targetMin: Double?,
    val targetMax: Double?,
    val targetPeriod: TargetPeriod,
    val rollingDays: Int?,
    val scheduleType: HabitScheduleType,
    val scheduleInterval: Int,
    val weekdays: Set<DayOfWeek>,
    val flexibleTimesPerWeek: Int?,
    val startDate: LocalDate,
    val endType: HabitEndType,
    val endDate: LocalDate?,
    val endValue: Double?,
    val quickIncrement: Double,
    val quickActions: List<Double>,
    val reminderMinutes: List<Int>,
    val weekdayReminderMinutes: Map<DayOfWeek, List<Int>>,
    val weekStart: DayOfWeek,
    val timerStartedAtMillis: Long?,
    val pinned: Boolean,
    val position: Int,
    val archived: Boolean,
    val paused: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val sourceMetricId: String? = null,
    val autoCompleteFromItems: Boolean = true,
    val timerSessionId: String? = null,
    val timerNeedsReview: Boolean = false,
    val timerAccumulatedSeconds: Double = 0.0,
    val timerAnchorElapsedRealtimeMillis: Long? = null,
)

data class HabitChecklistItemDraft(
    val name: String,
    val position: Int,
    /** Existing identity is retained while editing so checked history follows
     * the logical item rather than whichever label occupies the same row. */
    val id: Long? = null,
    val uuid: String? = null,
) : java.io.Serializable
data class HabitChecklistItem(
    val id: Long,
    val uuid: String,
    val habitId: Long,
    val name: String,
    val position: Int,
    val archived: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

data class HabitLog(
    val id: Long,
    val uuid: String,
    val habitId: Long,
    val value: Double?,
    val canonicalValue: Double?,
    val enteredUnitId: String?,
    val status: HabitLogStatus,
    val timestamp: Instant,
    val localDate: LocalDate,
    val zoneId: String,
    val offsetSeconds: Int,
    val note: String,
    val sourceType: MetricSourceType,
    val sourceId: String?,
    val metricEntryId: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
) : java.io.Serializable

data class HabitChecklistState(
    val habitId: Long,
    val itemId: Long,
    val localDate: LocalDate,
    val completed: Boolean,
    val completedAtMillis: Long?,
    val nameSnapshot: String,
)

data class HabitPause(
    val id: Long,
    val habitId: Long,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val note: String,
) : java.io.Serializable

data class HabitSkip(
    val uuid: String,
    val habitId: Long,
    val localDate: LocalDate,
    val skippedAtMillis: Long,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

data class HabitDayProgress(
    val habit: Habit,
    val date: LocalDate,
    val scheduled: Boolean,
    val value: Double,
    val status: HabitLogStatus?,
    val successful: Boolean?,
    val checklistItems: List<Pair<HabitChecklistItem, Boolean>>,
    val streak: Int,
    val completionRate: Double,
    val flexibleScheduleProgress: Int? = null,
    val flexibleScheduleTarget: Int? = null,
    val dayState: HabitDayState = HabitDayState.Pending,
)

data class FlexibleHabitProgress(
    val completed: Int,
    val target: Int,
)

fun HabitLog.valueInUnit(
    unitId: String,
    customUnits: List<UnitDefinition> = emptyList(),
): Double? {
    val targetUnit = BuiltInUnits.get(unitId) ?: customUnits.firstOrNull { it.id == unitId }
    if (canonicalValue != null && targetUnit != null) return targetUnit.fromCanonical(canonicalValue)
    if (enteredUnitId == unitId || enteredUnitId == null) return value
    val enteredUnit = BuiltInUnits.get(enteredUnitId)
        ?: customUnits.firstOrNull { it.id == enteredUnitId }
    return if (value != null && enteredUnit != null && targetUnit != null) {
        targetUnit.fromCanonical(enteredUnit.toCanonical(value))
    } else null
}

fun Habit.flexibleProgress(
    logs: List<HabitLog>,
    date: LocalDate,
    pauses: List<HabitPause> = emptyList(),
    skips: List<HabitSkip> = emptyList(),
): FlexibleHabitProgress? {
    val configuredTarget = when (scheduleType) {
        HabitScheduleType.FlexibleTimesPerWeek,
        HabitScheduleType.FlexibleTimesPerMonth,
        -> flexibleTimesPerWeek?.coerceAtLeast(1)
        else -> null
    } ?: return null
    val bounds = when (scheduleType) {
        HabitScheduleType.FlexibleTimesPerWeek -> {
            val start = date.with(TemporalAdjusters.previousOrSame(weekStart))
            start..start.plusDays(6)
        }
        HabitScheduleType.FlexibleTimesPerMonth ->
            date.withDayOfMonth(1)..date.withDayOfMonth(date.lengthOfMonth())
        else -> return null
    }
    val completed = logs.count { log ->
        log.habitId == id &&
            log.localDate in bounds &&
            log.status in setOf(HabitLogStatus.Recorded, HabitLogStatus.Success) &&
            (log.value ?: 0.0) > 0.0
    }
    // Flexible targets are period obligations, not seven (or thirty) separate
    // daily obligations. A user-excused/skipped day only lowers the target when
    // there are genuinely too few eligible days left in that period; a fully
    // paused period is neutral rather than a manufactured failure.
    val eligibleDays = generateSequence(bounds.start) { it.plusDays(1) }
        .takeWhile { it <= bounds.endInclusive }
        .count { day ->
            !day.isBefore(startDate) && endDate?.let(day::isAfter) != true &&
                !isNeutralDate(day, pauses, skips)
        }
    return FlexibleHabitProgress(completed, configuredTarget.coerceAtMost(eligibleDays))
}

fun Habit.isNeutralDate(
    date: LocalDate,
    pauses: List<HabitPause> = emptyList(),
    skips: List<HabitSkip> = emptyList(),
): Boolean {
    if (pauses.any { it.habitId == id && !date.isBefore(it.startDate) && (it.endDate == null || !date.isAfter(it.endDate)) }) {
        return true
    }
    return skips.any { it.habitId == id && it.localDate == date }
}

fun Habit.valueForPeriod(
    logs: List<HabitLog>,
    date: LocalDate,
    customUnits: List<UnitDefinition> = emptyList(),
): Double {
    val relevant = logs.filter {
        it.habitId == id &&
            it.localDate in periodBounds(date) &&
            it.status in setOf(HabitLogStatus.Recorded, HabitLogStatus.Success, HabitLogStatus.Failed)
    }
    return when (trackingMode) {
        HabitTrackingMode.Checklist, HabitTrackingMode.Rating ->
            relevant.maxByOrNull(HabitLog::timestamp)?.valueInUnit(unitId, customUnits) ?: 0.0
        HabitTrackingMode.CheckOff -> if (relevant.any { (it.valueInUnit(unitId, customUnits) ?: 0.0) > 0.0 }) 1.0 else 0.0
        else -> relevant.sumOf { it.valueInUnit(unitId, customUnits) ?: 0.0 }
    }
}

/** The outcome for one target period, rather than whether any logging occurred. */
fun Habit.outcomeForPeriod(
    logs: List<HabitLog>,
    date: LocalDate,
    customUnits: List<UnitDefinition> = emptyList(),
): Boolean? {
    val hasData = logs.any {
        it.habitId == id && it.localDate in periodBounds(date) &&
            it.status in setOf(HabitLogStatus.Recorded, HabitLogStatus.Success, HabitLogStatus.Failed) &&
            it.value != null
    }
    if (!hasData) return null
    return targetSatisfied(valueForPeriod(logs, date, customUnits))
}

fun Habit.flexiblePeriodStreak(
    logs: List<HabitLog>,
    through: LocalDate,
    pauses: List<HabitPause> = emptyList(),
    skips: List<HabitSkip> = emptyList(),
): Int {
    if (scheduleType !in setOf(HabitScheduleType.FlexibleTimesPerWeek, HabitScheduleType.FlexibleTimesPerMonth)) {
        return 0
    }
    fun previous(date: LocalDate): LocalDate = when (scheduleType) {
        HabitScheduleType.FlexibleTimesPerWeek -> date.minusWeeks(1)
        HabitScheduleType.FlexibleTimesPerMonth -> date.minusMonths(1)
        else -> date
    }
    var cursor = when (scheduleType) {
        HabitScheduleType.FlexibleTimesPerWeek -> through.with(TemporalAdjusters.previousOrSame(weekStart))
        HabitScheduleType.FlexibleTimesPerMonth -> through.withDayOfMonth(1)
        else -> through
    }
    // An unfinished current period is not a failure. Carry the previous closed
    // streak until the current period either reaches its target or closes.
    if ((flexibleProgress(logs, cursor, pauses, skips)?.completed ?: 0) < (flexibleProgress(logs, cursor, pauses, skips)?.target ?: 1)) {
        cursor = previous(cursor)
    }
    var streak = 0
    while (!cursor.isBefore(startDate.withDayOfMonth(1).takeIf {
            scheduleType == HabitScheduleType.FlexibleTimesPerMonth
        } ?: startDate.with(TemporalAdjusters.previousOrSame(weekStart)))) {
        val progress = flexibleProgress(logs, cursor, pauses, skips) ?: break
        if (progress.target == 0) {
            cursor = previous(cursor)
            continue
        }
        if (progress.completed < progress.target) break
        streak++
        cursor = previous(cursor)
    }
    return streak
}

fun Habit.completionRateOverRecentPeriods(
    logs: List<HabitLog>,
    through: LocalDate,
    lookbackDays: Long = 30,
    pauses: List<HabitPause> = emptyList(),
    customUnits: List<UnitDefinition> = emptyList(),
    skips: List<HabitSkip> = emptyList(),
): Double {
    val since = through.minusDays((lookbackDays - 1).coerceAtLeast(0))
    if (scheduleType !in setOf(HabitScheduleType.FlexibleTimesPerWeek, HabitScheduleType.FlexibleTimesPerMonth)) {
        val scheduled = generateSequence(through) { it.minusDays(1) }
            .takeWhile { !it.isBefore(since) && !it.isBefore(startDate) }
            .filter(::isScheduledOn)
            .toList()
        val outcomes = scheduled.mapNotNull { day ->
            if (isNeutralDate(day, pauses, skips)) return@mapNotNull null
            outcomeForPeriod(logs, day, customUnits)
                ?: false.takeIf { day.isBefore(through) }
        }
        return if (outcomes.isEmpty()) 0.0 else outcomes.count { it }.toDouble() / outcomes.size
    }
    val starts = buildList {
        var cursor = when (scheduleType) {
            HabitScheduleType.FlexibleTimesPerWeek -> through.with(TemporalAdjusters.previousOrSame(weekStart))
            HabitScheduleType.FlexibleTimesPerMonth -> through.withDayOfMonth(1)
            else -> through
        }
        while (!cursor.isBefore(startDate) && !cursor.isBefore(since)) {
            add(cursor)
            cursor = if (scheduleType == HabitScheduleType.FlexibleTimesPerWeek) cursor.minusWeeks(1) else cursor.minusMonths(1)
        }
    }
    val outcomes = starts.mapNotNull { start ->
        val progress = flexibleProgress(logs, start, pauses, skips) ?: return@mapNotNull null
        if (progress.target == 0) return@mapNotNull null
        val complete = progress.completed >= progress.target
        val periodClosed = when (scheduleType) {
            HabitScheduleType.FlexibleTimesPerWeek -> start.plusDays(6).isBefore(through)
            HabitScheduleType.FlexibleTimesPerMonth -> start.withDayOfMonth(start.lengthOfMonth()).isBefore(through)
            else -> true
        }
        complete.takeIf { complete || periodClosed }
    }
    return if (outcomes.isEmpty()) 0.0 else outcomes.count(Boolean::not).let { failures ->
        (outcomes.size - failures).toDouble() / outcomes.size
    }
}

fun Habit.isScheduledOn(date: LocalDate, weekSuccesses: Int = 0, monthSuccesses: Int = 0): Boolean {
    if (
        date.isBefore(startDate) ||
        (endType == HabitEndType.OnDate && endDate?.let(date::isAfter) == true) ||
        paused
    ) return false
    return when (scheduleType) {
        HabitScheduleType.Daily -> true
        HabitScheduleType.EveryNDays -> {
            val delta = date.toEpochDay() - startDate.toEpochDay()
            delta >= 0 && delta % scheduleInterval.coerceAtLeast(1) == 0L
        }
        HabitScheduleType.SelectedWeekdays -> date.dayOfWeek in weekdays
        HabitScheduleType.FlexibleTimesPerWeek -> weekSuccesses < (flexibleTimesPerWeek ?: 1)
        HabitScheduleType.FlexibleTimesPerMonth -> monthSuccesses < (flexibleTimesPerWeek ?: 1)
    }
}

fun Habit.reminderNeededOn(
    logs: List<HabitLog>,
    date: LocalDate,
    customUnits: List<UnitDefinition> = emptyList(),
    skips: List<HabitSkip> = emptyList(),
    pauses: List<HabitPause> = emptyList(),
): Boolean {
    if (hasEnded(logs, date, pauses, customUnits, skips)) return false
    if (isNeutralDate(date, pauses, skips)) return false
    val flexible = flexibleProgress(logs, date, pauses, skips)
    val weekSuccesses = flexible?.completed.takeIf { scheduleType == HabitScheduleType.FlexibleTimesPerWeek } ?: 0
    val monthSuccesses = flexible?.completed.takeIf { scheduleType == HabitScheduleType.FlexibleTimesPerMonth } ?: 0
    if (!isScheduledOn(date, weekSuccesses, monthSuccesses)) return false
    return when (scheduleType) {
        HabitScheduleType.FlexibleTimesPerWeek,
        HabitScheduleType.FlexibleTimesPerMonth,
        -> flexible == null || flexible.completed < flexible.target
        else -> outcomeForPeriod(logs, date, customUnits) != true
    }
}

/** One canonical end-condition evaluator shared by Today, reminders, and
 * widgets. Threshold conditions stay ended once the threshold was reached on
 * any prior date; they cannot reappear after a later missing day. */
fun Habit.hasEnded(
    logs: List<HabitLog>,
    date: LocalDate,
    pauses: List<HabitPause> = emptyList(),
    customUnits: List<UnitDefinition> = emptyList(),
    skips: List<HabitSkip> = emptyList(),
): Boolean = when (endType) {
    HabitEndType.Never -> false
    HabitEndType.OnDate -> endDate?.let(date::isAfter) == true
    HabitEndType.AfterCompletions -> {
        endValue?.toInt()?.let { target ->
            successfulPeriodOutcomeDates(logs, startDate, date, pauses, customUnits, skips).size >= target
        } ?: false
    }
    HabitEndType.AfterTotal -> {
        endValue?.let { target ->
            logs.asSequence()
                .filter {
                    it.habitId == id && !it.localDate.isAfter(date) &&
                        it.status in setOf(HabitLogStatus.Recorded, HabitLogStatus.Success)
                }
                .sumOf { it.valueInUnit(unitId, customUnits) ?: 0.0 } >= target
        } ?: false
    }
    HabitEndType.AfterStreak -> {
        endValue?.toInt()?.let { target ->
            val outcomes = generateSequence(startDate) { it.plusDays(1) }
                .takeWhile { !it.isAfter(date) }
                .associateWith { day -> outcomeForPeriod(logs, day, customUnits) }
            val neutral = outcomes.keys.filterTo(mutableSetOf()) { isNeutralDate(it, pauses, skips) }
            outcomes.keys.asSequence()
                .filter { outcomes[it] == true }
                .any { through -> habitStreak(this, through, outcomes, neutral) >= target }
        } ?: false
    }
}

/**
 * Dates on which a complete habit outcome was achieved. Flexible weekly/monthly
 * habits emit one outcome for the whole target period (on the target-reaching
 * check-in), rather than one outcome per raw increment.
 */
fun Habit.successfulPeriodOutcomeDates(
    logs: List<HabitLog>,
    from: LocalDate,
    through: LocalDate,
    pauses: List<HabitPause> = emptyList(),
    customUnits: List<UnitDefinition> = emptyList(),
    skips: List<HabitSkip> = emptyList(),
): Set<LocalDate> {
    if (through.isBefore(from)) return emptySet()
    val habitLogs = logs.filter { it.habitId == id }
    if (scheduleType !in setOf(HabitScheduleType.FlexibleTimesPerWeek, HabitScheduleType.FlexibleTimesPerMonth)) {
        return generateSequence(from) { it.plusDays(1) }
            .takeWhile { !it.isAfter(through) }
            .filter { isScheduledOn(it) && !isNeutralDate(it, pauses, skips) && outcomeForPeriod(habitLogs, it, customUnits) == true }
            .toSet()
    }
    val firstStart = when (scheduleType) {
        HabitScheduleType.FlexibleTimesPerWeek -> from.with(TemporalAdjusters.previousOrSame(weekStart))
        HabitScheduleType.FlexibleTimesPerMonth -> from.withDayOfMonth(1)
        else -> from
    }
    return buildSet {
        var periodStart = firstStart
        while (!periodStart.isAfter(through)) {
            val bounds = when (scheduleType) {
                HabitScheduleType.FlexibleTimesPerWeek -> periodStart..periodStart.plusDays(6)
                HabitScheduleType.FlexibleTimesPerMonth -> periodStart..periodStart.withDayOfMonth(periodStart.lengthOfMonth())
                else -> periodStart..periodStart
            }
            val progress = flexibleProgress(habitLogs, periodStart, pauses, skips)
            val target = progress?.target ?: 0
            if (target > 0 && (progress?.completed ?: 0) >= target) {
                val achievingDate = habitLogs.asSequence()
                    .filter {
                        it.localDate in bounds &&
                            it.status in setOf(HabitLogStatus.Recorded, HabitLogStatus.Success) &&
                            (it.value ?: 0.0) > 0.0
                    }
                    .sortedWith(compareBy<HabitLog> { it.timestamp }.thenBy { it.id })
                    .drop(target - 1)
                    .firstOrNull()
                    ?.localDate
                if (achievingDate != null && achievingDate in from..through) add(achievingDate)
            }
            periodStart = if (scheduleType == HabitScheduleType.FlexibleTimesPerWeek) periodStart.plusWeeks(1) else periodStart.plusMonths(1)
        }
    }
}

fun Habit.targetSatisfied(value: Double): Boolean? = when (comparison) {
    TargetComparison.AtLeast -> targetMin?.let { value >= it }
    TargetComparison.AtMost -> targetMax?.let { value <= it }
        ?: targetMin?.let { value <= it }
    TargetComparison.Exactly -> targetMin?.let { value == it }
    TargetComparison.WithinRange -> if (targetMin != null && targetMax != null) {
        value in targetMin..targetMax
    } else null
    TargetComparison.None -> null
}

fun Habit.periodBounds(date: LocalDate): ClosedRange<LocalDate> = when (targetPeriod) {
    TargetPeriod.Occurrence, TargetPeriod.Day -> date..date
    TargetPeriod.Week -> {
        val start = date.with(TemporalAdjusters.previousOrSame(weekStart))
        start..start.plusDays(6)
    }
    TargetPeriod.Month -> date.withDayOfMonth(1)..date.withDayOfMonth(date.lengthOfMonth())
    TargetPeriod.RollingDays -> date.minusDays((rollingDays ?: 1).coerceAtLeast(1).toLong() - 1)..date
}

fun habitStreak(
    habit: Habit,
    through: LocalDate,
    successByDate: Map<LocalDate, Boolean?>,
    neutralDates: Set<LocalDate> = emptySet(),
): Int {
    var date = through
    var streak = 0
    while (!date.isBefore(habit.startDate)) {
        if (!habit.isScheduledOn(date)) {
            date = date.minusDays(1)
            continue
        }
        if (date in neutralDates) {
            date = date.minusDays(1)
            continue
        }
        when (successByDate[date]) {
            true -> streak++
            false -> return streak
            null -> if (date == through) {
                // An unfinished current occurrence does not erase the streak
                // earned through the previous scheduled day.
            } else return streak
        }
        date = date.minusDays(1)
    }
    return streak
}

fun Habit.dayStateOn(
    date: LocalDate,
    today: LocalDate,
    logs: List<HabitLog>,
    pauses: List<HabitPause> = emptyList(),
    skips: List<HabitSkip> = emptyList(),
    customUnits: List<UnitDefinition> = emptyList(),
): HabitDayState {
    if (paused || pauses.any { it.habitId == id && !date.isBefore(it.startDate) && (it.endDate == null || !date.isAfter(it.endDate)) }) {
        return HabitDayState.Paused
    }
    if (skips.any { it.habitId == id && it.localDate == date }) return HabitDayState.Skipped
    if (!isScheduledOn(date)) return HabitDayState.NotScheduled
    return when (outcomeForPeriod(logs, date, customUnits)) {
        true -> HabitDayState.Completed
        false -> HabitDayState.BelowTarget
        null -> if (date.isBefore(today)) HabitDayState.Missed else HabitDayState.Pending
    }
}
