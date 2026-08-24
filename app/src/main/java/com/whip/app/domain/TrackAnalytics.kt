package com.whip.app.domain

import java.time.LocalDate

const val TRACK_ENTRY_DATE_CONDITION_UUID = "__whip_entry_date__"

enum class TrackConditionMode { MatchAll, MatchAny }

enum class TrackConditionOperator {
    Is,
    IsNot,
    Contains,
    DoesNotContain,
    IsBlank,
    IsNotBlank,
    Equals,
    NotEqual,
    GreaterThan,
    AtLeast,
    LessThan,
    AtMost,
    Between,
    IsOneOf,
    On,
    Before,
    OnOrBefore,
    After,
    OnOrAfter,
    IsYes,
    IsNo,
    IsUnanswered,
    IsAnswered,
}

data class TrackCondition(
    val fieldUuid: String,
    val operator: TrackConditionOperator,
    val textValue: String? = null,
    val numberValue: Double? = null,
    val secondNumberValue: Double? = null,
    val choiceOptionUuids: Set<String> = emptySet(),
    val dateValue: LocalDate? = null,
    val secondDateValue: LocalDate? = null,
)

enum class TrackAggregation {
    CountEntries,
    CountMatchingEntries,
    Sum,
    Average,
    Latest,
    Minimum,
    Maximum,
    FixedAmount,
}

data class TrackAggregationResult(
    val value: Double?,
    val eligibleEntryCount: Int,
    val skippedEntryCount: Int,
    val firstDate: LocalDate?,
    val lastDate: LocalDate?,
)

fun TrackProjection.matchingEntries(
    conditions: List<TrackCondition>,
    mode: TrackConditionMode = TrackConditionMode.MatchAll,
): List<TrackEntryProjection> {
    if (conditions.isEmpty()) return entries
    return entries.filter { entry ->
        val results = conditions.map { condition -> matches(entry, condition) }
        when (mode) {
            TrackConditionMode.MatchAll -> results.all(Boolean::identity)
            TrackConditionMode.MatchAny -> results.any(Boolean::identity)
        }
    }
}

fun TrackProjection.aggregate(
    aggregation: TrackAggregation,
    fieldUuid: String? = null,
    conditions: List<TrackCondition> = emptyList(),
    conditionMode: TrackConditionMode = TrackConditionMode.MatchAll,
    fixedCanonicalValue: Double? = null,
): TrackAggregationResult {
    val matching = matchingEntries(conditions, conditionMode)
    val field = fieldUuid?.let { uuid -> fields.firstOrNull { it.uuid == uuid } }
    val numeric = field?.let { selected -> matching.mapNotNull { it.numericValue(selected) } }.orEmpty()
    val usesNumericField = aggregation in setOf(
        TrackAggregation.Sum,
        TrackAggregation.Average,
        TrackAggregation.Latest,
        TrackAggregation.Minimum,
        TrackAggregation.Maximum,
    )
    val eligibleCount = if (usesNumericField) numeric.size else matching.size
    val value = when (aggregation) {
        TrackAggregation.CountEntries, TrackAggregation.CountMatchingEntries -> matching.size.toDouble()
        TrackAggregation.Sum -> numeric.takeIf { it.isNotEmpty() }?.sum()
        TrackAggregation.Average -> numeric.takeIf { it.isNotEmpty() }?.average()
        TrackAggregation.Latest -> field?.let { selected ->
            matching.sortedWith(compareByDescending<TrackEntryProjection> { it.entry.entryDate }.thenByDescending { it.entry.createdAtMillis })
                .firstNotNullOfOrNull { it.numericValue(selected) }
        }
        TrackAggregation.Minimum -> numeric.minOrNull()
        TrackAggregation.Maximum -> numeric.maxOrNull()
        TrackAggregation.FixedAmount -> fixedCanonicalValue?.times(matching.size)
    }
    return TrackAggregationResult(
        value = value,
        eligibleEntryCount = eligibleCount,
        skippedEntryCount = entries.size - eligibleCount,
        firstDate = matching.minOfOrNull { it.entry.entryDate },
        lastDate = matching.maxOfOrNull { it.entry.entryDate },
    )
}

private fun TrackProjection.matches(entry: TrackEntryProjection, condition: TrackCondition): Boolean {
    if (condition.fieldUuid == TRACK_ENTRY_DATE_CONDITION_UUID) {
        return when (condition.operator) {
            TrackConditionOperator.On -> entry.entry.entryDate == condition.dateValue
            TrackConditionOperator.Before -> compareDate(entry.entry.entryDate, condition.dateValue) { a, b -> a < b }
            TrackConditionOperator.OnOrBefore -> compareDate(entry.entry.entryDate, condition.dateValue) { a, b -> a <= b }
            TrackConditionOperator.After -> compareDate(entry.entry.entryDate, condition.dateValue) { a, b -> a > b }
            TrackConditionOperator.OnOrAfter -> compareDate(entry.entry.entryDate, condition.dateValue) { a, b -> a >= b }
            TrackConditionOperator.Between -> condition.dateValue?.let { first -> condition.secondDateValue?.let { second ->
                entry.entry.entryDate in minOf(first, second)..maxOf(first, second)
            } } == true
            else -> false
        }
    }
    val field = fields.firstOrNull { it.uuid == condition.fieldUuid } ?: return false
    val value = entry.value(field.id)
    val blank = value == null
    return when (condition.operator) {
        TrackConditionOperator.IsBlank -> blank
        TrackConditionOperator.IsNotBlank -> !blank
        TrackConditionOperator.IsUnanswered -> value?.booleanValue == null
        TrackConditionOperator.IsAnswered -> value?.booleanValue != null
        TrackConditionOperator.IsYes -> value?.booleanValue == true
        TrackConditionOperator.IsNo -> value?.booleanValue == false
        TrackConditionOperator.Contains -> value?.textValue?.contains(condition.textValue.orEmpty(), ignoreCase = true) == true
        TrackConditionOperator.DoesNotContain -> value?.textValue?.contains(condition.textValue.orEmpty(), ignoreCase = true) == false
        TrackConditionOperator.Is -> when (field.type) {
            TrackFieldType.ShortText, TrackFieldType.LongText -> value?.textValue.equals(condition.textValue, ignoreCase = true)
            TrackFieldType.SingleChoice -> optionUuid(value?.choiceOptionId) in condition.choiceOptionUuids
            else -> false
        }
        TrackConditionOperator.IsNot -> when (field.type) {
            TrackFieldType.ShortText, TrackFieldType.LongText -> !value?.textValue.equals(condition.textValue, ignoreCase = true)
            TrackFieldType.SingleChoice -> optionUuid(value?.choiceOptionId) !in condition.choiceOptionUuids
            else -> false
        }
        TrackConditionOperator.IsOneOf -> optionUuid(value?.choiceOptionId) in condition.choiceOptionUuids
        TrackConditionOperator.Equals -> entry.numericValue(field) == condition.numberValue
        TrackConditionOperator.NotEqual -> entry.numericValue(field)?.let { it != condition.numberValue } == true
        TrackConditionOperator.GreaterThan -> compareNumber(entry, field, condition.numberValue) { a, b -> a > b }
        TrackConditionOperator.AtLeast -> compareNumber(entry, field, condition.numberValue) { a, b -> a >= b }
        TrackConditionOperator.LessThan -> compareNumber(entry, field, condition.numberValue) { a, b -> a < b }
        TrackConditionOperator.AtMost -> compareNumber(entry, field, condition.numberValue) { a, b -> a <= b }
        TrackConditionOperator.Between -> entry.numericValue(field)?.let { number ->
            condition.numberValue?.let { first -> condition.secondNumberValue?.let { second -> number in minOf(first, second)..maxOf(first, second) } }
        } == true
        TrackConditionOperator.On -> value?.dateValue == condition.dateValue
        TrackConditionOperator.Before -> compareDate(value?.dateValue, condition.dateValue) { a, b -> a < b }
        TrackConditionOperator.OnOrBefore -> compareDate(value?.dateValue, condition.dateValue) { a, b -> a <= b }
        TrackConditionOperator.After -> compareDate(value?.dateValue, condition.dateValue) { a, b -> a > b }
        TrackConditionOperator.OnOrAfter -> compareDate(value?.dateValue, condition.dateValue) { a, b -> a >= b }
    }
}

private fun TrackProjection.optionUuid(optionId: Long?): String? = options.firstOrNull { it.id == optionId }?.uuid

private fun TrackEntryProjection.numericValue(field: TrackField): Double? = when (field.type) {
    TrackFieldType.Number -> value(field.id)?.canonicalNumber
    TrackFieldType.Scale -> value(field.id)?.scaleValue
    else -> null
}

private inline fun compareNumber(
    entry: TrackEntryProjection,
    field: TrackField,
    expected: Double?,
    comparison: (Double, Double) -> Boolean,
): Boolean = entry.numericValue(field)?.let { actual -> expected?.let { comparison(actual, it) } } == true

private inline fun compareDate(
    actual: LocalDate?,
    expected: LocalDate?,
    comparison: (LocalDate, LocalDate) -> Boolean,
): Boolean = actual?.let { first -> expected?.let { second -> comparison(first, second) } } == true

private fun Boolean.Companion.identity(value: Boolean): Boolean = value
