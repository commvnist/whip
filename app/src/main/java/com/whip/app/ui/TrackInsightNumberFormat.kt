package com.whip.app.ui

import com.whip.app.domain.TrackField
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.UnitDimension
import java.text.NumberFormat
import java.util.Locale

/** Presentation of canonical readings; stored values and aggregation contracts stay unchanged. */
internal class TrackInsightNumberFormat(
    private val field: TrackField,
    units: List<UnitDefinition>,
    locale: Locale = Locale.getDefault(),
) {
    private val unit = units.firstOrNull { it.id == field.unitId && it.dimension == field.dimension }
    private val numberFormat = NumberFormat.getNumberInstance(locale).apply {
        isGroupingUsed = false
        minimumFractionDigits = if (field.type == TrackFieldType.Number) field.precision.coerceIn(0, 6) else 0
        maximumFractionDigits = if (field.type == TrackFieldType.Number) minimumFractionDigits
        else maxOf(2, field.precision.coerceIn(0, 6))
    }

    // Temperature and rating scales are readings, not additive quantities.
    val showTotal: Boolean = field.type == TrackFieldType.Number &&
        field.dimension != UnitDimension.Temperature && unit?.toCanonicalOffset == 0.0

    fun format(canonical: Double?): String = formatReading(canonical, difference = false)

    fun formatDifference(canonical: Double?): String = formatReading(canonical, difference = true)

    private fun formatReading(canonical: Double?, difference: Boolean): String {
        if (canonical == null || !canonical.isFinite()) return "—"
        val value = when (field.type) {
            TrackFieldType.Number -> {
                val selected = unit ?: return "—"
                if (difference) canonical / selected.toCanonicalFactor else selected.fromCanonical(canonical)
            }
            TrackFieldType.Scale -> canonical
            else -> return "—"
        }
        if (!value.isFinite()) return "—"
        val suffix = if (field.type == TrackFieldType.Number) unit?.symbol.orEmpty() else ""
        return numberFormat.format(value) + suffix.takeIf(String::isNotBlank)?.let { " $it" }.orEmpty()
    }
}
