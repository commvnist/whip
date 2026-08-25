package com.whip.app.domain

import java.io.Serializable
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs

enum class GoalType { ReachValue, ReduceValue, AccumulateTotal, MaintainRange, MeetAverage, Consistency, WeightedMilestones, OpenEndedTrend, ElapsedSince }
enum class GoalAggregation { Latest, Sum, Average, Minimum, Maximum, CompletionCount, TimeInRange }
enum class GoalAggregationPeriod { All, Day, Week, Month, RollingDays }
enum class GoalConsistencyPeriod { Day, Week, Month }
enum class GoalPaceType { Linear, None }
enum class GoalDirection { Increase, Decrease, Neutral }
enum class GoalStatus { Active, Paused, Completed, Abandoned, Archived }
enum class ElapsedDisplayUnit { Auto, Minutes, Hours, Days, Weeks, Years }

/** The goal type is the user-facing promise; storage and calculation choices
 * must not be allowed to contradict it. */
fun GoalType.defaultAggregation(): GoalAggregation = when (this) {
    GoalType.ReachValue, GoalType.ReduceValue, GoalType.MaintainRange, GoalType.OpenEndedTrend, GoalType.ElapsedSince -> GoalAggregation.Latest
    GoalType.AccumulateTotal -> GoalAggregation.Sum
    GoalType.MeetAverage -> GoalAggregation.Average
    GoalType.Consistency, GoalType.WeightedMilestones -> GoalAggregation.CompletionCount
}

fun GoalType.compatibleAggregations(): List<GoalAggregation> = when (this) {
    // A Reach goal may represent a cumulative target (for example, read 50
    // books). The source editor makes Latest versus Sum explicit.
    GoalType.ReachValue -> listOf(GoalAggregation.Latest, GoalAggregation.Sum)
    GoalType.MaintainRange -> listOf(GoalAggregation.Latest, GoalAggregation.TimeInRange)
    GoalType.OpenEndedTrend -> listOf(
        GoalAggregation.Latest,
        GoalAggregation.Average,
        GoalAggregation.Minimum,
        GoalAggregation.Maximum,
    )
    else -> listOf(defaultAggregation())
}

fun GoalType.defaultDirection(): GoalDirection = when (this) {
    GoalType.ReduceValue -> GoalDirection.Decrease
    GoalType.MaintainRange, GoalType.OpenEndedTrend, GoalType.ElapsedSince -> GoalDirection.Neutral
    else -> GoalDirection.Increase
}

data class GoalDraft(
    val name: String,
    val description: String = "",
    val areaId: String? = null,
    val area: String = "",
    val tags: List<String> = emptyList(),
    val icon: String = DEFAULT_GOAL_EMOJI,
    val type: GoalType,
    val dimension: UnitDimension = UnitDimension.Unitless,
    val unitId: String = "unitless",
    val precision: Int = 1,
    val baseline: Double? = null,
    val targetMin: Double? = null,
    val targetMax: Double? = null,
    val direction: GoalDirection = GoalDirection.Increase,
    val startDate: LocalDate,
    val deadline: LocalDate? = null,
    val aggregation: GoalAggregation = GoalAggregation.Latest,
    val paceType: GoalPaceType = GoalPaceType.Linear,
    val reminderMinutes: Int? = null,
    val milestones: List<GoalMilestoneDraft> = emptyList(),
    val aggregationPeriod: GoalAggregationPeriod = GoalAggregationPeriod.All,
    val rollingDays: Int? = null,
    val consistencyPeriod: GoalConsistencyPeriod = GoalConsistencyPeriod.Week,
    val consistencyRequiredPeriods: Int? = null,
    val elapsedStartMillis: Long? = null,
    val elapsedDisplayUnit: ElapsedDisplayUnit = ElapsedDisplayUnit.Auto,
) : Serializable

fun GoalDraft.withTypeSemantics(): GoalDraft = copy(
    aggregation = aggregation.takeIf { it in type.compatibleAggregations() } ?: type.defaultAggregation(),
    direction = type.defaultDirection(),
    paceType = paceType.takeIf { deadline != null && type !in setOf(GoalType.OpenEndedTrend, GoalType.ElapsedSince) } ?: GoalPaceType.None,
)

/**
 * The authoritative save rules for a Goal draft. Editors use the complete list
 * to explain every blocked save, while repositories enforce the same contract
 * before writing anything.
 */
fun GoalDraft.validationErrors(nowMillis: Long): List<String> = buildList {
    if (name.isBlank()) add("Goal name is required")
    if (deadline != null && deadline.isBefore(startDate)) add("Deadline cannot precede the start date")
    if (precision !in 0..6) add("Decimal places must be between 0 and 6")
    if (listOfNotNull(baseline, targetMin, targetMax).any { !it.isFinite() }) add("Goal values must be finite numbers")
    if (aggregation !in type.compatibleAggregations()) add("Goal calculation does not match its type")
    if (direction != type.defaultDirection()) add("Goal direction does not match its type")
    if (paceType != GoalPaceType.None && deadline == null) add("Pace guidance requires a deadline")
    when (type) {
        GoalType.ReachValue, GoalType.ReduceValue, GoalType.AccumulateTotal, GoalType.MeetAverage ->
            if (targetMin?.isFinite() != true) add("Enter a target")
        GoalType.MaintainRange -> when {
            targetMin?.isFinite() != true || targetMax?.isFinite() != true -> add("Enter both range limits")
            targetMin > targetMax -> add("Range minimum cannot exceed range maximum")
        }
        GoalType.WeightedMilestones -> {
            if (milestones.none { it.name.isNotBlank() }) add("Add at least one milestone")
            if (milestones.any { !it.weight.isFinite() || it.weight < 0.0 }) add("Milestone weights must be non-negative numbers")
            if (milestones.none { it.name.isNotBlank() && it.weight > 0.0 }) add("At least one named milestone must have a positive weight")
        }
        GoalType.ElapsedSince -> {
            if (elapsedStartMillis == null) add("Choose when the timer started")
            if (elapsedStartMillis != null && elapsedStartMillis > nowMillis) add("Start time cannot be in the future")
            if (deadline != null) add("Elapsed-time Goals do not use a deadline")
        }
        GoalType.Consistency, GoalType.OpenEndedTrend -> Unit
    }
    if (aggregationPeriod == GoalAggregationPeriod.RollingDays && (rollingDays ?: 0) <= 0) {
        add("Enter a positive rolling window")
    }
    if (type == GoalType.Consistency) {
        if ((targetMin ?: 0.0) <= 0.0) add("Enter a per-period consistency target")
        if ((consistencyRequiredPeriods ?: 0) <= 0) add("Enter how many periods the goal should cover")
    }
}.distinct()

data class Goal(
    val id: Long,
    val uuid: String,
    val metricId: String,
    val name: String,
    val description: String,
    val areaId: String? = null,
    val area: String,
    val tags: List<String>,
    val icon: String,
    val type: GoalType,
    val dimension: UnitDimension,
    val unitId: String,
    val precision: Int,
    val baseline: Double?,
    val targetMin: Double?,
    val targetMax: Double?,
    val direction: GoalDirection,
    val startDate: LocalDate,
    val deadline: LocalDate?,
    val aggregation: GoalAggregation,
    val paceType: GoalPaceType,
    val reminderMinutes: Int?,
    val status: GoalStatus,
    val pinned: Boolean,
    val position: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val aggregationPeriod: GoalAggregationPeriod = GoalAggregationPeriod.All,
    val rollingDays: Int? = null,
    val consistencyPeriod: GoalConsistencyPeriod = GoalConsistencyPeriod.Week,
    val consistencyRequiredPeriods: Int? = null,
    val elapsedStartMillis: Long? = null,
    val elapsedDisplayUnit: ElapsedDisplayUnit = ElapsedDisplayUnit.Auto,
)

data class ElapsedCounter(val value: Long, val unit: ElapsedDisplayUnit) {
    fun label(): String {
        val noun = unit.name.lowercase().removeSuffix("s")
        return "$value $noun${if (value == 1L) "" else "s"}"
    }
}

/** Formats an elapsed-time goal from instants, independent of calendar/time-zone presentation. */
fun elapsedCounter(startMillis: Long, nowMillis: Long, requested: ElapsedDisplayUnit): ElapsedCounter {
    val elapsedMillis = (nowMillis - startMillis).coerceAtLeast(0L)
    val minute = 60_000L
    val hour = 60L * minute
    val day = 24L * hour
    val selected = if (requested != ElapsedDisplayUnit.Auto) requested else when {
        elapsedMillis >= 365L * day -> ElapsedDisplayUnit.Years
        elapsedMillis >= 14L * day -> ElapsedDisplayUnit.Weeks
        elapsedMillis >= 2L * day -> ElapsedDisplayUnit.Days
        elapsedMillis >= 2L * hour -> ElapsedDisplayUnit.Hours
        else -> ElapsedDisplayUnit.Minutes
    }
    val divisor = when (selected) {
        ElapsedDisplayUnit.Auto, ElapsedDisplayUnit.Minutes -> minute
        ElapsedDisplayUnit.Hours -> hour
        ElapsedDisplayUnit.Days -> day
        ElapsedDisplayUnit.Weeks -> 7L * day
        ElapsedDisplayUnit.Years -> 365L * day
    }
    return ElapsedCounter(elapsedMillis / divisor, selected)
}

data class GoalMilestoneDraft(
    val name: String,
    val weight: Double = 1.0,
    val reward: String = "",
    /** Existing identity is carried through editors so reorder/insert never
     * reassigns completion state by list position. */
    val id: Long? = null,
    val uuid: String? = null,
) : Serializable

data class GoalMilestone(
    val id: Long,
    val uuid: String,
    val goalId: Long,
    val name: String,
    val position: Int,
    val weight: Double,
    val completed: Boolean,
    val completedAtMillis: Long?,
    val reward: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

data class GoalProjection(
    val goal: Goal,
    val currentValue: Double?,
    val progress: Double?,
    val deltaFromBaseline: Double?,
    val expectedProgress: Double?,
    val paceDelta: Double?,
    val forecastDate: LocalDate?,
    val onPace: Boolean?,
    val milestones: List<GoalMilestone>,
    val entries: List<MetricEntry>,
    val consistency: GoalConsistencyProgress? = null,
)

data class GoalConsistencyProgress(
    val targetPerPeriod: Double,
    val period: GoalConsistencyPeriod,
    val requiredPeriods: Int,
    val successfulPeriods: Int,
    val observedPeriods: Int,
    val currentPeriodValue: Double,
    val currentPeriodSuccessful: Boolean,
)

data class GoalHistoryPoint(
    val date: LocalDate,
    val canonicalValue: Double?,
    val progress: Double?,
    val recordedEntries: Int,
)

data class GoalInsightSummary(
    val points: List<GoalHistoryPoint>,
    val ratePerDay: Double?,
    val forecastDate: LocalDate?,
    val confidence: String,
    val dataQualityExplanation: String,
    val targetMin: Double?,
    val targetMax: Double?,
)

/** Builds an inspectable timeline without repeatedly rescanning the entire
 * history. Missing/failed/skipped values remain visible in the quality count
 * but never become numeric progress. */
fun buildGoalInsights(
    goal: Goal,
    entries: List<MetricEntry>,
    milestones: List<GoalMilestone> = emptyList(),
): GoalInsightSummary {
    val relevant = entries.filter { it.metricId == goal.metricId }.sortedBy(MetricEntry::timestamp)
    val recordedByDate = relevant.filter { it.status == MetricEntryStatus.Recorded && it.canonicalValue?.isFinite() == true }
        .groupBy(MetricEntry::localDate).toSortedMap()
    var runningSum = 0.0
    var runningCount = 0
    var runningPositive = 0
    var runningInRange = 0
    var runningMin: Double? = null
    var runningMax: Double? = null
    var latest: MetricEntry? = null
    val accumulated = mutableListOf<MetricEntry>()
    val points = recordedByDate.map { (date, dayEntries) ->
        dayEntries.forEach { entry ->
            val value = requireNotNull(entry.canonicalValue)
            runningSum += value
            runningCount++
            if (value > 0.0) runningPositive++
            if (goal.targetMin != null && goal.targetMax != null && value in goal.targetMin..goal.targetMax) runningInRange++
            runningMin = runningMin?.let { minOf(it, value) } ?: value
            runningMax = runningMax?.let { maxOf(it, value) } ?: value
            if (latest == null || entry.timestamp > requireNotNull(latest).timestamp) latest = entry
            accumulated += entry
        }
        val current = if (goal.type == GoalType.Consistency) {
            calculateConsistencyProgress(goal, accumulated, date).successfulPeriods.toDouble()
        } else when (goal.aggregation) {
            GoalAggregation.Latest -> latest?.canonicalValue
            GoalAggregation.Sum -> runningSum
            GoalAggregation.Average -> runningSum / runningCount.coerceAtLeast(1)
            GoalAggregation.Minimum -> runningMin
            GoalAggregation.Maximum -> runningMax
            GoalAggregation.CompletionCount -> runningPositive.toDouble()
            GoalAggregation.TimeInRange -> runningInRange.toDouble() / runningCount.coerceAtLeast(1) * 100.0
        }
        val progress = if (goal.type == GoalType.Consistency) {
            current?.div((goal.consistencyRequiredPeriods ?: 1).coerceAtLeast(1))?.coerceIn(0.0, 1.0)
        } else calculateGoalProgress(goal, current, milestones)
        GoalHistoryPoint(date, current, progress, dayEntries.size)
    }
    val first = points.firstOrNull()
    val last = points.lastOrNull()
    val elapsed = if (first == null || last == null) 0L else java.time.temporal.ChronoUnit.DAYS.between(first.date, last.date)
    val rate = if (elapsed > 0 && first?.canonicalValue != null && last?.canonicalValue != null) {
        (last.canonicalValue - first.canonicalValue) / elapsed
    } else null
    val forecast = forecastGoalDate(goal, last, rate)
    val sourceKinds = relevant.map(MetricEntry::sourceType).distinct()
    val invalid = relevant.count { it.status != MetricEntryStatus.Recorded || it.canonicalValue?.isFinite() != true }
    val confidence = when {
        points.size >= 12 && elapsed >= 28 -> "higher"
        points.size >= 4 && elapsed >= 7 -> "limited"
        else -> "insufficient"
    }
    val quality = buildString {
        append("${points.size} observed days from ${sourceKinds.size.coerceAtLeast(1)} source type")
        if (sourceKinds.size != 1) append('s')
        append("; $invalid missing, skipped, failed, or invalid entries excluded")
        if (confidence != "higher") append(". Forecast confidence is $confidence because history is short or sparse")
    }
    return GoalInsightSummary(points, rate, forecast, confidence, quality, goal.targetMin, goal.targetMax)
}

private fun forecastGoalDate(goal: Goal, last: GoalHistoryPoint?, rate: Double?): LocalDate? {
    val value = last?.canonicalValue ?: return null
    val daily = rate?.takeIf { it.isFinite() && kotlin.math.abs(it) > 1e-9 } ?: return null
    val target = when (goal.type) {
        GoalType.ReduceValue -> goal.targetMin
        GoalType.ReachValue, GoalType.AccumulateTotal -> goal.targetMin
        else -> null
    } ?: return null
    val days = (target - value) / daily
    if (!days.isFinite() || days <= 0.0 || days > 36_500.0) return null
    return last.date.plusDays(kotlin.math.ceil(days).toLong())
}

fun aggregateGoalValue(
    goal: Goal,
    entries: List<MetricEntry>,
    through: LocalDate? = null,
): Double? {
    val relevantEntries = entries.filterForGoalWindow(goal, through)
    val values = relevantEntries.filter { it.status == MetricEntryStatus.Recorded }.mapNotNull(MetricEntry::canonicalValue)
    if (values.isEmpty()) return null
    return when (goal.aggregation) {
        GoalAggregation.Latest -> relevantEntries.filter { it.status == MetricEntryStatus.Recorded && it.canonicalValue != null }
            .maxByOrNull(MetricEntry::timestamp)?.canonicalValue
        GoalAggregation.Sum -> values.sum()
        GoalAggregation.Average -> values.average()
        GoalAggregation.Minimum -> values.min()
        GoalAggregation.Maximum -> values.max()
        GoalAggregation.CompletionCount -> values.count { it > 0.0 }.toDouble()
        GoalAggregation.TimeInRange -> {
            val min = goal.targetMin ?: return null
            val max = goal.targetMax ?: return null
            values.count { it in min..max }.toDouble() / values.size * 100.0
        }
    }
}

fun calculateGoalProgress(
    goal: Goal,
    current: Double?,
    milestones: List<GoalMilestone> = emptyList(),
): Double? {
    if (goal.type in setOf(GoalType.OpenEndedTrend, GoalType.ElapsedSince)) return null
    if (goal.type == GoalType.WeightedMilestones) {
        val total = milestones.sumOf { it.weight.coerceAtLeast(0.0) }
        if (total <= 0.0) return 0.0
        return milestones.filter(GoalMilestone::completed).sumOf { it.weight.coerceAtLeast(0.0) } / total
    }
    val value = current ?: return 0.0
    if (goal.type == GoalType.Consistency) {
        val required = goal.consistencyRequiredPeriods ?: return null
        return (value / required.coerceAtLeast(1)).coerceIn(0.0, 1.0)
    }
    if (goal.type == GoalType.MaintainRange && goal.aggregation == GoalAggregation.TimeInRange) {
        return (value / 100.0).coerceIn(0.0, 1.0)
    }
    if (goal.type == GoalType.MaintainRange) {
        val min = goal.targetMin ?: return null
        val max = goal.targetMax ?: return null
        return if (value in min..max) 1.0 else 0.0
    }
    val baseline = goal.baseline ?: 0.0
    val target = goal.targetMin ?: goal.targetMax ?: return null
    if (baseline == target) {
        return when (goal.direction) {
            GoalDirection.Decrease -> if (value <= target) 1.0 else 0.0
            else -> if (value >= target) 1.0 else 0.0
        }
    }
    val raw = when (goal.direction) {
        GoalDirection.Increase, GoalDirection.Neutral -> (value - baseline) / (target - baseline)
        GoalDirection.Decrease -> (baseline - value) / (baseline - target)
    }
    return raw.coerceIn(0.0, 1.0)
}

fun projectGoal(
    goal: Goal,
    entries: List<MetricEntry>,
    milestones: List<GoalMilestone>,
    today: LocalDate,
): GoalProjection {
    val consistency = if (goal.type == GoalType.Consistency) {
        calculateConsistencyProgress(goal, entries, today)
    } else {
        null
    }
    val current = if (goal.type == GoalType.ElapsedSince) null else consistency?.successfulPeriods?.toDouble() ?: aggregateGoalValue(goal, entries, today)
    val progress = consistency?.let {
        it.successfulPeriods.toDouble().div(it.requiredPeriods).coerceIn(0.0, 1.0)
    } ?: calculateGoalProgress(goal, current, milestones)
    val elapsed = ChronoUnit.DAYS.between(goal.startDate, today).coerceAtLeast(0)
    val total = goal.deadline?.let { ChronoUnit.DAYS.between(goal.startDate, it).coerceAtLeast(1) }
    val expected = if (goal.paceType == GoalPaceType.None || total == null) null
    else (elapsed.toDouble() / total).coerceIn(0.0, 1.0)
    val paceDelta = if (progress != null && expected != null) progress - expected else null
    val forecast = if (goal.type != GoalType.Consistency && progress != null && progress > 0.0 && elapsed > 0 && progress < 1.0) {
        goal.startDate.plusDays((elapsed / progress).toLong())
    } else null
    return GoalProjection(
        goal = goal,
        currentValue = current,
        progress = progress,
        deltaFromBaseline = if (current != null && goal.baseline != null) current - goal.baseline else null,
        expectedProgress = expected,
        paceDelta = paceDelta,
        forecastDate = forecast,
        onPace = paceDelta?.let { it >= -0.02 },
        milestones = milestones.sortedBy(GoalMilestone::position),
        entries = entries.sortedByDescending(MetricEntry::timestamp),
        consistency = consistency,
    )
}

/** A normalized, meaningful outcome for Review: positive progress gained on a
 * day, one successful range/consistency period, or one observed open trend day.
 * Multiple raw entries cannot inflate the outcome merely by being numerous. */
fun goalOutcomeScoreOnDate(
    goal: Goal,
    entries: List<MetricEntry>,
    milestones: List<GoalMilestone>,
    date: LocalDate,
): Double {
    val relevant = entries.filter { it.metricId == goal.metricId && it.status == MetricEntryStatus.Recorded }
    if (relevant.none { it.localDate == date }) return 0.0
    if (goal.type == GoalType.ElapsedSince) return 0.0
    if (goal.type == GoalType.OpenEndedTrend) return 1.0
    if (goal.type == GoalType.MaintainRange) {
        val value = aggregateGoalValue(goal, relevant, date) ?: return 0.0
        val min = goal.targetMin ?: return 0.0
        val max = goal.targetMax ?: min
        return if (value in min..max) 1.0 else 0.0
    }
    val current = projectGoal(goal, relevant, milestones, date).progress ?: return 0.0
    val previous = if (date.isAfter(goal.startDate)) {
        projectGoal(goal, relevant, milestones, date.minusDays(1)).progress ?: 0.0
    } else 0.0
    return (current - previous).coerceIn(0.0, 1.0)
}

fun calculateConsistencyProgress(
    goal: Goal,
    entries: List<MetricEntry>,
    through: LocalDate,
): GoalConsistencyProgress {
    require(goal.type == GoalType.Consistency) { "Goal must use consistency mode" }
    val target = (goal.targetMin ?: 1.0).coerceAtLeast(0.0)
    val required = (goal.consistencyRequiredPeriods ?: inferredRequiredPeriods(goal)).coerceAtLeast(1)
    val effectiveThrough = minOf(through, goal.deadline ?: through)
    val firstPeriod = consistencyPeriodStart(goal.startDate, goal.consistencyPeriod)
    val currentPeriod = consistencyPeriodStart(effectiveThrough, goal.consistencyPeriod)
    val starts = generateSequence(firstPeriod) { nextConsistencyPeriod(it, goal.consistencyPeriod) }
        .takeWhile { !it.isAfter(currentPeriod) }
        .take(required)
        .toList()
    val eligibleEntries = entries.filter {
        it.status == MetricEntryStatus.Recorded &&
            it.canonicalValue != null &&
            !it.localDate.isBefore(goal.startDate) &&
            !it.localDate.isAfter(effectiveThrough)
    }
    fun periodValue(start: LocalDate): Double {
        val end = nextConsistencyPeriod(start, goal.consistencyPeriod).minusDays(1)
        val values = eligibleEntries.filter { it.localDate in start..end }.mapNotNull(MetricEntry::canonicalValue)
        if (values.isEmpty()) return 0.0
        return when (goal.aggregation) {
            GoalAggregation.Latest -> eligibleEntries.filter { it.localDate in start..end }
                .maxByOrNull(MetricEntry::timestamp)?.canonicalValue ?: 0.0
            GoalAggregation.Sum -> values.sum()
            GoalAggregation.Average -> values.average()
            GoalAggregation.Minimum -> values.min()
            GoalAggregation.Maximum -> values.max()
            GoalAggregation.CompletionCount -> values.count { it > 0.0 }.toDouble()
            GoalAggregation.TimeInRange -> {
                val max = goal.targetMax ?: goal.targetMin ?: return 0.0
                values.count { it in target..max }.toDouble()
            }
        }
    }
    val periodValues = starts.associateWith(::periodValue)
    val successful = periodValues.count { (_, value) -> value >= target }
    val activeStart = starts.lastOrNull() ?: firstPeriod
    val currentValue = periodValues[activeStart] ?: 0.0
    return GoalConsistencyProgress(
        targetPerPeriod = target,
        period = goal.consistencyPeriod,
        requiredPeriods = required,
        successfulPeriods = successful,
        observedPeriods = starts.size,
        currentPeriodValue = currentValue,
        currentPeriodSuccessful = currentValue >= target,
    )
}

private fun List<MetricEntry>.filterForGoalWindow(goal: Goal, through: LocalDate?): List<MetricEntry> {
    val requestedEnd = through ?: maxOfOrNull(MetricEntry::localDate) ?: return emptyList()
    val end = minOf(requestedEnd, goal.deadline ?: requestedEnd)
    val windowStart = when (goal.aggregationPeriod) {
        GoalAggregationPeriod.All -> goal.startDate
        GoalAggregationPeriod.Day -> end
        GoalAggregationPeriod.Week -> end.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        GoalAggregationPeriod.Month -> end.withDayOfMonth(1)
        GoalAggregationPeriod.RollingDays -> end.minusDays((goal.rollingDays ?: 1).coerceAtLeast(1).toLong() - 1)
    }.coerceAtLeast(goal.startDate)
    return filter { it.localDate in windowStart..end }
}

private fun consistencyPeriodStart(date: LocalDate, period: GoalConsistencyPeriod): LocalDate = when (period) {
    GoalConsistencyPeriod.Day -> date
    GoalConsistencyPeriod.Week -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    GoalConsistencyPeriod.Month -> date.withDayOfMonth(1)
}

private fun nextConsistencyPeriod(date: LocalDate, period: GoalConsistencyPeriod): LocalDate = when (period) {
    GoalConsistencyPeriod.Day -> date.plusDays(1)
    GoalConsistencyPeriod.Week -> date.plusWeeks(1)
    GoalConsistencyPeriod.Month -> date.plusMonths(1)
}

private fun inferredRequiredPeriods(goal: Goal): Int {
    val deadline = goal.deadline ?: return 1
    val first = consistencyPeriodStart(goal.startDate, goal.consistencyPeriod)
    val last = consistencyPeriodStart(deadline, goal.consistencyPeriod)
    return generateSequence(first) { nextConsistencyPeriod(it, goal.consistencyPeriod) }
        .takeWhile { !it.isAfter(last) }
        .count()
        .coerceAtLeast(1)
}

fun Goal.displayValue(canonicalValue: Double?, customUnits: List<UnitDefinition> = emptyList()): Double? = canonicalValue?.let { value ->
    (BuiltInUnits.get(unitId) ?: customUnits.firstOrNull { it.id == unitId })?.fromCanonical(value) ?: value
}
