package com.whip.app.ui

import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.Goal
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalType
import com.whip.app.domain.GoalProjection
import com.whip.app.domain.editableNumericValue
import com.whip.app.domain.UnitDefinition
import java.text.NumberFormat
import java.util.Locale
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal fun GoalProjection.typedOutcomeReading(customUnits: List<UnitDefinition> = emptyList()): String? = when (goal.type) {
    GoalType.MaintainRange -> {
        val bounds = "${formatGoalCanonicalValue(goal.targetMin, goal.unitId, goal.precision, customUnits)}–${formatGoalCanonicalValue(goal.targetMax, goal.unitId, goal.precision, customUnits)}"
        when {
            terminalSnapshot != null -> when {
                currentValue == null -> "No observed value stored at closure"
                goal.aggregation == GoalAggregation.TimeInRange ->
                    "${formatGoalCanonicalValue(currentValue, "percent", goal.precision)} of observations in range at closure"
                else -> "Observed value at closure · " +
                    formatGoalCanonicalValue(currentValue, goal.unitId, goal.precision, customUnits)
            }
            currentValue == null -> "No observations yet · target range $bounds"
            goal.aggregation == GoalAggregation.TimeInRange ->
                "${formatGoalCanonicalValue(currentValue, "percent", goal.precision)} of observations in range · $bounds"
            else -> "${if (goal.targetMin != null && goal.targetMax != null && currentValue in goal.targetMin..goal.targetMax) "In range" else "Outside range"} · " +
                "${formatGoalCanonicalValue(currentValue, goal.unitId, goal.precision, customUnits)} · target $bounds"
        }
    }
    GoalType.Consistency -> {
        val count = consistency?.successfulPeriods?.toDouble() ?: currentValue
        count?.let { if (terminalSnapshot != null) "${editableNumericValue(it)} successful periods at closure"
            else "${editableNumericValue(it)} of ${consistency?.requiredPeriods ?: goal.consistencyRequiredPeriods ?: 1} successful periods" }
            ?: "No successful periods yet"
    }
    else -> null
}

internal fun GoalProjection.consistencyPeriodReading(): String? = consistency?.let {
    val period = if (it.trackingWindowEnded) {
        "Last tracked ${it.period.periodLabel}, ${it.trackedPeriodStart?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)).orEmpty()}"
    } else "This ${it.period.periodLabel}"
    "$period · ${editableNumericValue(it.currentPeriodValue)}/${editableNumericValue(it.targetPerPeriod)} successes" +
        if (it.trackingWindowEnded) ". This tracking window has ended." else ""
}

// Timeline aggregates can represent counts or percentages instead of measurements.
internal val Goal.trendUnitId: String
    get() = when {
        type == GoalType.Consistency || aggregation == GoalAggregation.CompletionCount -> "count"
        aggregation == GoalAggregation.TimeInRange -> "percent"
        else -> unitId
    }

internal fun formatGoalCanonicalValue(
    canonical: Double?,
    unitId: String,
    precision: Int,
    customUnits: List<UnitDefinition> = emptyList(),
    difference: Boolean = false,
    locale: Locale = Locale.getDefault(),
): String {
    if (canonical == null || !canonical.isFinite()) return "—"
    val unit = BuiltInUnits.get(unitId) ?: customUnits.firstOrNull { it.id == unitId }
    val value = if (difference) canonical / (unit?.toCanonicalFactor ?: 1.0)
        else unit?.fromCanonical(canonical) ?: canonical
    val symbol = if (unitId in setOf("unitless", "count")) ""
        else unit?.symbol?.ifBlank { unit.name } ?: unitId
    return (String.format(locale, "%.${precision.coerceIn(0, 6)}f", value) + " $symbol").trim()
}

/** Formats the domain's progress ratio without hiding a beginning or inventing a completed target. */
internal fun formatGoalProgressPercent(progress: Double, locale: Locale = Locale.getDefault()): String {
    val percent = NumberFormat.getPercentInstance(locale).apply {
        maximumFractionDigits = 1
        isGroupingUsed = false
    }
    val formatted = percent.format(progress)
    return when {
        progress == 0.0 -> percent.format(0.0)
        progress > 0.0 && progress < 0.001 -> "<${percent.format(0.001)}"
        progress > 0.999 && progress < 1.0 -> ">${percent.format(0.999)}"
        progress > 1.0 && formatted == percent.format(1.0) -> ">$formatted"
        else -> formatted
    }
}
