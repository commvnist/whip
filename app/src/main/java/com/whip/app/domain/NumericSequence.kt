package com.whip.app.domain

import kotlin.math.abs
import kotlin.math.round
import java.text.DecimalFormatSymbols
import java.util.Locale

data class NumericSequenceParseResult(
    val values: List<Double> = emptyList(),
    val isRange: Boolean = false,
    val error: String? = null,
)

data class CompactNumericSequence(
    val specification: String,
    val increment: Double?,
    val isRange: Boolean,
)

data class WeightEquipmentSetup(
    val increment: Double,
    val barWeight: Double,
    val plates: List<Double>,
)

data class MachineSequenceSetup(
    val specification: String,
    val increment: Double,
)

private val numericRangePattern = Regex(
    """^\s*(\d+(?:[.,]\d+)?)\s*(?:-|–|—|\.\.)\s*(\d+(?:[.,]\d+)?)\s*$""",
)

fun parseNumericSequence(
    specification: String,
    rangeIncrement: Double?,
    maximumValues: Int = 200,
    locale: Locale = Locale.getDefault(),
): NumericSequenceParseResult {
    val source = specification.trim()
    if (source.isBlank()) return NumericSequenceParseResult()

    numericRangePattern.matchEntire(source)?.let { match ->
        val minimum = match.groupValues[1].toWhipDoubleOrNull(locale)
            ?: return NumericSequenceParseResult(error = "Enter a valid range minimum")
        val maximum = match.groupValues[2].toWhipDoubleOrNull(locale)
            ?: return NumericSequenceParseResult(error = "Enter a valid range maximum")
        val increment = rangeIncrement
        if (increment == null || !increment.isFinite() || increment <= 0.0) {
            return NumericSequenceParseResult(error = "Enter a positive increment for this range")
        }
        if (maximum < minimum) {
            return NumericSequenceParseResult(error = "Range maximum must be at least the minimum")
        }
        val rawSteps = (maximum - minimum) / increment
        val stepCount = round(rawSteps).toInt()
        if (abs(rawSteps - stepCount) > 1e-7) {
            return NumericSequenceParseResult(
                error = "The increment must land exactly on the range maximum",
            )
        }
        if (stepCount + 1 > maximumValues) {
            return NumericSequenceParseResult(error = "Range creates more than $maximumValues values")
        }
        return NumericSequenceParseResult(
            values = (0..stepCount).map { index -> normalizeNumericValue(minimum + index * increment) },
            isRange = true,
        )
    }

    val decimalSeparator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
    val tokens = when {
        ';' in source -> source.split(';')
        decimalSeparator == ',' && ',' in source -> {
            if (source.count { it == ',' } == 1) listOf(source)
            else return NumericSequenceParseResult(error = "Use semicolons between values when decimals use commas")
        }
        else -> source.split(',')
    }.map(String::trim).filter(String::isNotBlank)
    if (tokens.isEmpty()) return NumericSequenceParseResult()
    val values = tokens.map { token -> token.toWhipDoubleOrNull(locale) }
    if (values.any { it == null || !it.isFinite() || it < 0.0 }) {
        return NumericSequenceParseResult(
            error = "Use non-negative numbers separated by commas, or a range such as 1-10",
        )
    }
    val distinct = values.filterNotNull().map(::normalizeNumericValue).distinct().sorted()
    if (distinct.size > maximumValues) {
        return NumericSequenceParseResult(error = "List contains more than $maximumValues values")
    }
    return NumericSequenceParseResult(values = distinct)
}

fun compactNumericSequence(values: List<Double>): CompactNumericSequence {
    val sorted = values.filter { it.isFinite() && it >= 0.0 }
        .map(::normalizeNumericValue)
        .distinct()
        .sorted()
    if (sorted.isEmpty()) return CompactNumericSequence("", null, false)
    if (sorted.size >= 2) {
        val increment = normalizeNumericValue(sorted[1] - sorted[0])
        val evenlySpaced = increment > 0.0 && sorted.zipWithNext().all { (left, right) ->
            abs((right - left) - increment) <= 1e-7
        }
        if (evenlySpaced) {
            return CompactNumericSequence(
                specification = "${editableNumericValue(sorted.first())}-${editableNumericValue(sorted.last())}",
                increment = increment,
                isRange = true,
            )
        }
    }
    return CompactNumericSequence(
        specification = sorted.joinToString(",", transform = ::editableNumericValue),
        increment = null,
        isRange = false,
    )
}

fun steppedNumericValue(
    currentText: String,
    direction: Int,
    increment: Double,
    allowedValues: List<Double> = emptyList(),
): Double {
    require(direction == -1 || direction == 1) { "Direction must be -1 or 1" }
    val current = currentText.toWhipDoubleOrNull()
    val allowed = allowedValues.filter { it.isFinite() && it >= 0.0 }.distinct().sorted()
    if (allowed.isNotEmpty()) {
        if (current == null) return if (direction > 0) allowed.first() else allowed.last()
        return if (direction > 0) {
            allowed.firstOrNull { it > current + 1e-7 } ?: allowed.last()
        } else {
            allowed.lastOrNull { it < current - 1e-7 } ?: allowed.first()
        }
    }
    val validIncrement = increment.takeIf { it.isFinite() && it > 0.0 } ?: 1.0
    return normalizeNumericValue(((current ?: 0.0) + direction * validIncrement).coerceAtLeast(0.0))
}

fun standardWeightEquipment(unitId: String): WeightEquipmentSetup = if (unitId == "pound") {
    WeightEquipmentSetup(
        increment = 5.0,
        barWeight = 45.0,
        plates = listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5),
    )
} else {
    WeightEquipmentSetup(
        increment = 2.5,
        barWeight = 20.0,
        plates = listOf(20.0, 15.0, 10.0, 5.0, 2.5, 1.25),
    )
}

/**
 * Converts future-entry equipment defaults without leaking unit-conversion
 * noise into fields a lifter must type against. Conventional metric equipment
 * maps to its conventional imperial counterpart (for example 20 kg -> 45 lb
 * and 2.5 kg -> 5 lb). User-defined values remain user-defined, but are rounded
 * to the nearest quarter unit so a custom 7.5 kg implement becomes 16.5 lb
 * instead of 16.534669 lb.
 */
fun convertWeightEquipmentSetup(
    setup: WeightEquipmentSetup,
    fromUnitId: String,
    toUnitId: String,
): WeightEquipmentSetup {
    if (fromUnitId == toUnitId) return setup
    val sourceStandard = standardWeightEquipment(fromUnitId)
    val targetStandard = standardWeightEquipment(toUnitId)
    val conventionalPairs = buildList {
        add(sourceStandard.barWeight to targetStandard.barWeight)
        sourceStandard.plates.zip(targetStandard.plates).forEach { add(it) }
    }
    fun convert(value: Double): Double {
        conventionalPairs.firstOrNull { (source, _) -> abs(value - source) <= 1e-7 }
            ?.let { return it.second }
        val converted = massFromKilograms(massToKilograms(value, fromUnitId), toUnitId)
        return normalizeNumericValue(round(converted * 4.0) / 4.0)
    }
    return WeightEquipmentSetup(
        increment = convert(setup.increment),
        barWeight = convert(setup.barWeight),
        plates = setup.plates.map(::convert),
    )
}

/** Converts a mass default to a value that is practical to type and display. */
fun convertPracticalMassValue(value: Double, fromUnitId: String, toUnitId: String): Double {
    if (fromUnitId == toUnitId) return normalizeNumericValue(value)
    val sourceStandard = standardWeightEquipment(fromUnitId)
    val targetStandard = standardWeightEquipment(toUnitId)
    val conventionalPairs = buildList {
        add(sourceStandard.barWeight to targetStandard.barWeight)
        sourceStandard.plates.zip(targetStandard.plates).forEach { add(it) }
    }
    conventionalPairs.firstOrNull { (source, _) -> abs(value - source) <= 1e-7 }
        ?.let { return it.second }
    val converted = massFromKilograms(massToKilograms(value, fromUnitId), toUnitId)
    return normalizeNumericValue(round(converted * 4.0) / 4.0)
}

fun standardMachineSequence(loadType: MachineLoadType, unitId: String): MachineSequenceSetup = when {
    loadType == MachineLoadType.Level -> MachineSequenceSetup("1-10", 1.0)
    unitId == "pound" -> MachineSequenceSetup("10-200", 10.0)
    else -> MachineSequenceSetup("5-100", 5.0)
}

fun editableNumericValue(value: Double): String = if (value % 1.0 == 0.0) {
    value.toLong().toString()
} else {
    String.format(java.util.Locale.ROOT, "%.6f", value).trimEnd('0').trimEnd('.')
}

private fun normalizeNumericValue(value: Double): Double = round(value * 1_000_000.0) / 1_000_000.0
