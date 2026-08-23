package com.whip.app.domain

import java.time.Instant
import java.time.LocalDate

enum class MetricValueKind {
    Boolean,
    Integer,
    Decimal,
    Duration,
    Percentage,
    Rating,
    TimeOfDay,
    Checklist,
}

enum class UnitDimension {
    Count,
    Duration,
    Distance,
    Volume,
    Mass,
    Length,
    Money,
    Energy,
    Percentage,
    Unitless,
    Custom,
}

enum class MetricEntryStatus {
    Recorded,
    Missing,
    Failed,
    Skipped,
    Excused,
}

enum class MetricSourceType {
    Manual,
    Habit,
    Goal,
    Task,
    Workout,
    Exercise,
    Import,
    HealthConnect,
}

data class UnitDefinition(
    val id: String,
    val name: String,
    val symbol: String,
    val dimension: UnitDimension,
    val toCanonicalFactor: Double,
    val toCanonicalOffset: Double = 0.0,
    val custom: Boolean = false,
    val archived: Boolean = false,
    val createdAtMillis: Long = 0,
    val updatedAtMillis: Long = 0,
) {
    fun toCanonical(value: Double): Double = (value + toCanonicalOffset) * toCanonicalFactor
    fun fromCanonical(value: Double): Double = value / toCanonicalFactor - toCanonicalOffset
}

data class MetricDefinition(
    val id: String,
    val name: String,
    val valueKind: MetricValueKind,
    val dimension: UnitDimension,
    val defaultUnitId: String,
    val precision: Int = 1,
    val dimensionLocked: Boolean = false,
    val archived: Boolean = false,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

data class MetricEntry(
    val id: String,
    val metricId: String,
    val canonicalValue: Double?,
    val enteredValue: Double?,
    val enteredUnitId: String?,
    val status: MetricEntryStatus,
    val timestamp: Instant,
    val localDate: LocalDate,
    val zoneId: String,
    val offsetSeconds: Int,
    val sourceType: MetricSourceType,
    val sourceId: String?,
    val note: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

data class Area(
    val id: String,
    val name: String,
    val colorArgb: Long?,
    val position: Int,
    val archived: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

data class WhipTag(
    val id: String,
    val name: String,
    val archived: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

object BuiltInUnits {
    val all = listOf(
        UnitDefinition("count", "count", "", UnitDimension.Count, 1.0),
        UnitDefinition("second", "seconds", "s", UnitDimension.Duration, 1.0),
        UnitDefinition("minute", "minutes", "min", UnitDimension.Duration, 60.0),
        UnitDefinition("hour", "hours", "h", UnitDimension.Duration, 3_600.0),
        UnitDefinition("distance_m", "metres", "m", UnitDimension.Distance, 1.0),
        UnitDefinition("kilometre", "kilometres", "km", UnitDimension.Distance, 1_000.0),
        UnitDefinition("mile", "miles", "mi", UnitDimension.Distance, 1_609.344),
        UnitDefinition("millilitre", "millilitres", "mL", UnitDimension.Volume, 1.0),
        UnitDefinition("litre", "litres", "L", UnitDimension.Volume, 1_000.0),
        UnitDefinition("cup", "cups", "cup", UnitDimension.Volume, 236.5882365),
        UnitDefinition("fluid_ounce", "fluid ounces", "fl oz", UnitDimension.Volume, 29.5735295625),
        UnitDefinition("kilogram", "kilograms", "kg", UnitDimension.Mass, 1.0),
        UnitDefinition("kilogram_rep", "kilogram-repetitions", "kg·rep", UnitDimension.Custom, 1.0),
        UnitDefinition("gram", "grams", "g", UnitDimension.Mass, 0.001),
        UnitDefinition("pound", "pounds", "lb", UnitDimension.Mass, 0.45359237),
        UnitDefinition("length_m", "metres", "m", UnitDimension.Length, 1.0),
        UnitDefinition("centimetre", "centimetres", "cm", UnitDimension.Length, 0.01),
        UnitDefinition("inch", "inches", "in", UnitDimension.Length, 0.0254),
        UnitDefinition("currency", "currency", "$", UnitDimension.Money, 1.0),
        UnitDefinition("kilojoule", "kilojoules", "kJ", UnitDimension.Energy, 1.0),
        UnitDefinition("kilocalorie", "kilocalories", "kcal", UnitDimension.Energy, 4.184),
        UnitDefinition("percent", "percent", "%", UnitDimension.Percentage, 1.0),
        UnitDefinition("unitless", "number", "", UnitDimension.Unitless, 1.0),
    )

    fun get(id: String): UnitDefinition? = all.firstOrNull { it.id == id }
}

fun convertMeasurement(
    value: Double,
    from: UnitDefinition,
    to: UnitDefinition,
): Double {
    require(from.dimension == to.dimension) {
        "Cannot convert ${from.dimension} to ${to.dimension}"
    }
    return to.fromCanonical(from.toCanonical(value))
}
