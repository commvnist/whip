package com.whip.app.ui

import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.Goal
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalType
import com.whip.app.domain.UnitDefinition
import java.text.NumberFormat
import java.util.Locale

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
