package com.whip.app.ui

import com.whip.app.domain.Goal
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.GoalType
import com.whip.app.domain.Habit
import com.whip.app.domain.LinkRule
import com.whip.app.domain.LinkSourceMetric
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.MetricDefinition
import com.whip.app.domain.TrackAggregation
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackProjection
import com.whip.app.domain.UnitDimension
import java.time.LocalDate

/** User concepts for Goal progress. Storage keeps Metric for compatibility, but the UI never exposes it. */
internal enum class GoalProgressSourceKind(
    val label: String,
    val sourceType: LinkSourceType,
) {
    Habit("Habit Check-In", LinkSourceType.Habit),
    Task("Task Completion", LinkSourceType.Task),
    Subtask("Subtask Completion", LinkSourceType.Subtask),
    Track("Track Entry", LinkSourceType.Track),
    Workout("Completed Workout", LinkSourceType.Workout),
    Exercise("Exercise Result", LinkSourceType.Exercise),
    Goal("Another Goal", LinkSourceType.Metric),
    HealthData("Health Connect Data", LinkSourceType.Metric),
}

internal fun inferGoalProgressSourceKind(
    rule: LinkRule?,
    goals: List<Goal>,
    habits: List<Habit>,
): GoalProgressSourceKind = when (rule?.sourceType) {
    null, LinkSourceType.Habit -> GoalProgressSourceKind.Habit
    LinkSourceType.Task -> GoalProgressSourceKind.Task
    LinkSourceType.Subtask -> GoalProgressSourceKind.Subtask
    LinkSourceType.Track -> GoalProgressSourceKind.Track
    LinkSourceType.Workout -> GoalProgressSourceKind.Workout
    LinkSourceType.Exercise -> GoalProgressSourceKind.Exercise
    LinkSourceType.Metric -> when (rule.sourceMetricId) {
        in goals.map(Goal::metricId) -> GoalProgressSourceKind.Goal
        in habits.map(Habit::metricId) -> GoalProgressSourceKind.Habit
        else -> GoalProgressSourceKind.HealthData
    }
}

internal fun List<MetricDefinition>.healthDataSources(currentMetricId: String? = null): List<MetricDefinition> =
    filter { !it.archived && (it.id.startsWith("health-connect-") || it.id == currentMetricId) }

internal fun List<Goal>.connectableAutomationGoals(): List<Goal> = filter {
    it.status in setOf(GoalStatus.Active, GoalStatus.Paused) &&
        it.type !in setOf(GoalType.ElapsedSince, GoalType.WeightedMilestones)
}

internal fun TrackProjection.compatibleAutomationGoals(goals: List<Goal>): List<Goal> =
    goals.connectableAutomationGoals().filter { goal ->
        val hasCompatibleNumberField = fields.any { field ->
            field.type == TrackFieldType.Scale && goal.dimension == UnitDimension.Unitless ||
                field.type == TrackFieldType.Number && field.dimension == goal.dimension
        }
        goal.compatibleTrackAutomationMeasures().any { !it.needsTrackNumberField() || hasCompatibleNumberField }
    }

internal val userSelectableTrackAutomationMeasures = listOf(
    TrackAggregation.CountEntries,
    TrackAggregation.Sum,
    TrackAggregation.Average,
    TrackAggregation.Latest,
    TrackAggregation.Minimum,
    TrackAggregation.Maximum,
    TrackAggregation.FixedAmount,
)

internal fun TrackAggregation.normalizedAutomationMeasure(): TrackAggregation = when (this) {
    TrackAggregation.CountMatchingEntries -> TrackAggregation.CountEntries
    else -> this
}

internal fun TrackAggregation.needsTrackNumberField(): Boolean = this in setOf(
    TrackAggregation.Sum,
    TrackAggregation.Average,
    TrackAggregation.Latest,
    TrackAggregation.Minimum,
    TrackAggregation.Maximum,
)

internal fun TrackAggregation.progressSourceMetric(): LinkSourceMetric =
    if (needsTrackNumberField()) LinkSourceMetric.FieldValue else LinkSourceMetric.EntryCount

internal fun TrackAggregation.automationLabel(): String = when (this) {
    TrackAggregation.CountEntries, TrackAggregation.CountMatchingEntries -> "Count Entries"
    TrackAggregation.Sum -> "Add Values From a Number or Scale Field"
    TrackAggregation.Average -> "Average a Number or Scale Field"
    TrackAggregation.Latest -> "Use the Latest Number or Scale Value"
    TrackAggregation.Minimum -> "Use the Lowest Field Value"
    TrackAggregation.Maximum -> "Use the Highest Field Value"
    TrackAggregation.FixedAmount -> "Add a Fixed Amount per Entry"
}

internal fun TrackAggregation.automationExplanation(): String = when (this) {
    TrackAggregation.CountEntries, TrackAggregation.CountMatchingEntries ->
        "Each eligible Entry adds 1 to Goal progress."
    TrackAggregation.Sum -> "Each eligible Entry contributes its selected Field value; the Goal adds them together."
    TrackAggregation.Average -> "The Goal uses the average selected Field value across eligible Entries."
    TrackAggregation.Latest -> "The Goal uses the selected Field value from the latest eligible Entry."
    TrackAggregation.Minimum -> "The Goal uses the lowest selected Field value across eligible Entries."
    TrackAggregation.Maximum -> "The Goal uses the highest selected Field value across eligible Entries."
    TrackAggregation.FixedAmount -> "Each eligible Entry adds the fixed amount you choose."
}

internal fun Goal.compatibleTrackAutomationMeasures(): List<TrackAggregation> = when (type) {
    GoalType.ReachValue -> if (dimension == UnitDimension.Count) {
        listOf(TrackAggregation.CountEntries, TrackAggregation.Sum, TrackAggregation.FixedAmount)
    } else {
        listOf(TrackAggregation.Latest, TrackAggregation.Sum, TrackAggregation.FixedAmount)
    }
    GoalType.AccumulateTotal -> buildList {
        if (dimension == UnitDimension.Count) add(TrackAggregation.CountEntries)
        add(TrackAggregation.Sum)
        add(TrackAggregation.FixedAmount)
    }
    GoalType.ReduceValue, GoalType.MaintainRange -> listOf(TrackAggregation.Latest)
    GoalType.MeetAverage -> listOf(TrackAggregation.Average)
    GoalType.Consistency -> listOf(TrackAggregation.CountEntries)
    GoalType.OpenEndedTrend -> listOf(
        TrackAggregation.Latest,
        TrackAggregation.Average,
        TrackAggregation.Minimum,
        TrackAggregation.Maximum,
    )
    GoalType.WeightedMilestones -> listOf(TrackAggregation.CountEntries)
    GoalType.ElapsedSince -> emptyList()
}

internal fun Goal.requiredAggregationForTrack(measure: TrackAggregation): GoalAggregation = when (measure) {
    TrackAggregation.CountEntries, TrackAggregation.CountMatchingEntries ->
        if (type == GoalType.Consistency) GoalAggregation.CompletionCount else GoalAggregation.Sum
    TrackAggregation.Sum, TrackAggregation.FixedAmount -> GoalAggregation.Sum
    TrackAggregation.Average -> GoalAggregation.Average
    TrackAggregation.Latest -> if (type == GoalType.MaintainRange && aggregation == GoalAggregation.TimeInRange) {
        GoalAggregation.TimeInRange
    } else {
        GoalAggregation.Latest
    }
    TrackAggregation.Minimum -> GoalAggregation.Minimum
    TrackAggregation.Maximum -> GoalAggregation.Maximum
}

internal fun GoalAggregation.automationCalculationLabel(): String = when (this) {
    GoalAggregation.Latest -> "Latest Value"
    GoalAggregation.Sum -> "Total"
    GoalAggregation.Average -> "Average"
    GoalAggregation.Minimum -> "Lowest Value"
    GoalAggregation.Maximum -> "Highest Value"
    GoalAggregation.CompletionCount -> "Completion Count"
    GoalAggregation.TimeInRange -> "Time in Range"
}

/**
 * Produces the Goal state required by a Track Automation. An explicitly
 * backfilled Entry must be inside the Goal's active window; otherwise the
 * Automation preview and contribution count can disagree with Goal progress.
 * The start only moves earlier—editing or removing an Automation must never
 * discard another source's established Goal history.
 */
internal fun Goal.toTrackAutomationDraft(
    aggregation: GoalAggregation,
    retroactiveFrom: LocalDate?,
): GoalDraft = GoalDraft(
    name = name,
    description = description,
    areaId = areaId,
    area = area,
    tags = tags,
    icon = icon,
    type = type,
    dimension = dimension,
    unitId = unitId,
    precision = precision,
    baseline = baseline,
    targetMin = targetMin,
    targetMax = targetMax,
    direction = direction,
    startDate = retroactiveFrom?.let { minOf(startDate, it) } ?: startDate,
    deadline = deadline,
    aggregation = aggregation,
    paceType = paceType,
    reminderMinutes = reminderMinutes,
    aggregationPeriod = aggregationPeriod,
    rollingDays = rollingDays,
    consistencyPeriod = consistencyPeriod,
    consistencyRequiredPeriods = consistencyRequiredPeriods,
    elapsedStartMillis = elapsedStartMillis,
    elapsedDisplayUnit = elapsedDisplayUnit,
)

internal fun LinkRule.progressSourceSummary(
    goals: List<Goal>,
    habits: List<Habit>,
    metrics: List<MetricDefinition>,
): String = when (sourceType) {
    LinkSourceType.Habit -> "Habit · ${habits.firstOrNull { it.id == sourceEntityId }?.name ?: "Unavailable Habit"} · ${sourceMetric.uiLabel()}"
    LinkSourceType.Task -> "Task · Completion"
    LinkSourceType.Subtask -> "Subtask · Completion"
    LinkSourceType.Workout -> "Workout · ${sourceMetric.uiLabel()}"
    LinkSourceType.Exercise -> "Exercise · ${sourceMetric.uiLabel()}"
    LinkSourceType.Track -> "Track · ${trackAggregation?.normalizedAutomationMeasure()?.automationLabel() ?: "Count Entries"}"
    LinkSourceType.Metric -> {
        val sourceGoal = goals.firstOrNull { it.metricId == sourceMetricId }
        val sourceHabit = habits.firstOrNull { it.metricId == sourceMetricId }
        val metric = metrics.firstOrNull { it.id == sourceMetricId }
        when {
            sourceGoal != null -> "Goal · ${sourceGoal.name}"
            sourceHabit != null -> "Habit · ${sourceHabit.name}"
            metric?.id?.startsWith("health-connect-") == true -> "Health Connect · ${metric.name}"
            else -> "Measurement · ${metric?.name ?: "Unavailable Source"}"
        }
    }
}
