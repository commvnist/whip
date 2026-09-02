package com.whip.app.domain

import java.io.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
    Temperature,
    Speed,
    Pace,
    Frequency,
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
    Track,
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

data class CustomUnitBoundary(
    val id: String,
    val name: String,
    val symbol: String,
    val dimension: UnitDimension,
    val toCanonicalFactor: Double,
    val toCanonicalOffset: Double,
    val archived: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
) : Serializable

fun UnitDefinition.customUnitBoundary() = CustomUnitBoundary(
    id = id,
    name = name,
    symbol = symbol,
    dimension = dimension,
    toCanonicalFactor = toCanonicalFactor,
    toCanonicalOffset = toCanonicalOffset,
    archived = archived,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

fun CustomUnitBoundary.toUnitDefinition() = UnitDefinition(
    id = id,
    name = name,
    symbol = symbol,
    dimension = dimension,
    toCanonicalFactor = toCanonicalFactor,
    toCanonicalOffset = toCanonicalOffset,
    custom = true,
    archived = archived,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

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
) : Serializable

/** One provider-owned fact prepared for an exact Health Connect source-window commit. */
data class HealthSourceRecord(
    val providerRecordId: String,
    val value: Double,
    val unitId: String,
    val timestamp: Instant,
    /** Provider-authored offset at [timestamp]; null only when the provider omitted it. */
    val zoneOffsetSeconds: Int? = null,
    val localDate: LocalDate? = null,
    val note: String = "Imported from Health Connect",
)

data class HealthMetricContract(
    val id: String,
    val name: String,
    val valueKind: MetricValueKind,
    val dimension: UnitDimension,
    val defaultUnitId: String,
    val precision: Int,
)

data class HealthSourceWindow(
    val metric: HealthMetricContract,
    val sourcePrefix: String,
    val startInclusive: Instant,
    val endExclusive: Instant,
    val zoneId: ZoneId,
    val records: List<HealthSourceRecord>,
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
        UnitDefinition("day", "days", "day", UnitDimension.Duration, 86_400.0),
        UnitDefinition("week", "weeks", "wk", UnitDimension.Duration, 604_800.0),
        UnitDefinition("distance_m", "metres", "m", UnitDimension.Distance, 1.0),
        UnitDefinition("kilometre", "kilometres", "km", UnitDimension.Distance, 1_000.0),
        UnitDefinition("mile", "miles", "mi", UnitDimension.Distance, 1_609.344),
        UnitDefinition("millilitre", "millilitres", "mL", UnitDimension.Volume, 1.0),
        UnitDefinition("litre", "litres", "L", UnitDimension.Volume, 1_000.0),
        UnitDefinition("cup", "cups", "cup", UnitDimension.Volume, 236.5882365),
        UnitDefinition("fluid_ounce", "fluid ounces", "fl oz", UnitDimension.Volume, 29.5735295625),
        UnitDefinition("kilogram", "kilograms", "kg", UnitDimension.Mass, 1.0),
        UnitDefinition("gram", "grams", "g", UnitDimension.Mass, 0.001),
        UnitDefinition("pound", "pounds", "lb", UnitDimension.Mass, 0.45359237),
        UnitDefinition("ounce", "ounces", "oz", UnitDimension.Mass, 0.028349523125),
        UnitDefinition("stone", "stone", "st", UnitDimension.Mass, 6.35029318),
        UnitDefinition("length_m", "metres", "m", UnitDimension.Length, 1.0),
        UnitDefinition("millimetre", "millimetres", "mm", UnitDimension.Length, 0.001),
        UnitDefinition("centimetre", "centimetres", "cm", UnitDimension.Length, 0.01),
        UnitDefinition("inch", "inches", "in", UnitDimension.Length, 0.0254),
        UnitDefinition("foot", "feet", "ft", UnitDimension.Length, 0.3048),
        UnitDefinition("yard", "yards", "yd", UnitDimension.Length, 0.9144),
        UnitDefinition("currency", "currency", "$", UnitDimension.Money, 1.0),
        UnitDefinition("kilojoule", "kilojoules", "kJ", UnitDimension.Energy, 1.0),
        UnitDefinition("kilocalorie", "kilocalories", "kcal", UnitDimension.Energy, 4.184),
        UnitDefinition("celsius", "degrees Celsius", "°C", UnitDimension.Temperature, 1.0),
        UnitDefinition("fahrenheit", "degrees Fahrenheit", "°F", UnitDimension.Temperature, 5.0 / 9.0, -32.0),
        UnitDefinition("kelvin", "kelvin", "K", UnitDimension.Temperature, 1.0, -273.15),
        UnitDefinition("metre_per_second", "metres per second", "m/s", UnitDimension.Speed, 1.0),
        UnitDefinition("kilometre_per_hour", "kilometres per hour", "km/h", UnitDimension.Speed, 1.0 / 3.6),
        UnitDefinition("mile_per_hour", "miles per hour", "mph", UnitDimension.Speed, 0.44704),
        UnitDefinition("minute_per_kilometre", "minutes per kilometre", "min/km", UnitDimension.Pace, 0.06),
        UnitDefinition("minute_per_mile", "minutes per mile", "min/mi", UnitDimension.Pace, 60.0 / 1_609.344),
        UnitDefinition("hertz", "hertz", "Hz", UnitDimension.Frequency, 1.0),
        UnitDefinition("per_minute", "per minute", "/min", UnitDimension.Frequency, 1.0 / 60.0),
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
