package com.whip.app.domain

import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class TrackFieldType {
    ShortText,
    LongText,
    Number,
    SingleChoice,
    Scale,
    Date,
    YesNo,
}

data class TrackDraft(
    val name: String,
    val description: String = "",
    val icon: String = DEFAULT_TRACK_EMOJI,
    val areaId: String? = null,
    val area: String = "",
    val tags: List<String> = emptyList(),
    val fields: List<TrackFieldDraft>,
) : Serializable

data class TrackFieldDraft(
    val name: String,
    val type: TrackFieldType,
    val required: Boolean = false,
    val primary: Boolean = false,
    val showInList: Boolean = false,
    val dimension: UnitDimension? = null,
    val unitId: String? = null,
    val precision: Int = 1,
    val scaleMin: Int? = null,
    val scaleMax: Int? = null,
    val scaleLowLabel: String = "",
    val scaleHighLabel: String = "",
    val scaleStep: Double = 1.0,
    val options: List<TrackChoiceOptionDraft> = emptyList(),
    val uuid: String? = null,
    val id: Long? = null,
) : Serializable

data class TrackChoiceOptionDraft(
    val label: String,
    val uuid: String? = null,
    val id: Long? = null,
) : Serializable

data class Track(
    val id: Long,
    val uuid: String,
    val name: String,
    val description: String,
    val icon: String,
    val areaId: String,
    val area: String,
    val tags: List<String>,
    val pinned: Boolean,
    val archived: Boolean,
    val position: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

data class TrackField(
    val id: Long,
    val uuid: String,
    val trackId: Long,
    val name: String,
    val type: TrackFieldType,
    val position: Int,
    val required: Boolean,
    val primary: Boolean,
    val showInList: Boolean,
    val dimension: UnitDimension?,
    val unitId: String?,
    val precision: Int,
    val scaleMin: Int?,
    val scaleMax: Int?,
    val scaleLowLabel: String,
    val scaleHighLabel: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val scaleStep: Double = 1.0,
)

data class TrackChoiceOption(
    val id: Long,
    val uuid: String,
    val fieldId: Long,
    val label: String,
    val position: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

data class TrackEntry(
    val id: Long,
    val uuid: String,
    val trackId: Long,
    val entryDate: LocalDate,
    val sourceOccurrenceId: Long? = null,
    val sourceExplanation: String = "",
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

/**
 * One typed value row. Repository validation guarantees that exactly the
 * column matching [TrackField.type] is populated.
 */
data class TrackFieldValue(
    val id: Long,
    val uuid: String,
    val entryId: Long,
    val fieldId: Long,
    val textValue: String? = null,
    val enteredNumber: Double? = null,
    val canonicalNumber: Double? = null,
    val enteredUnitId: String? = null,
    val dateValue: LocalDate? = null,
    val booleanValue: Boolean? = null,
    val choiceOptionId: Long? = null,
    val scaleValue: Double? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

data class TrackValueDraft(
    val textValue: String? = null,
    val enteredNumber: Double? = null,
    val enteredUnitId: String? = null,
    val dateValue: LocalDate? = null,
    val booleanValue: Boolean? = null,
    val choiceOptionUuid: String? = null,
    val scaleValue: Double? = null,
) : Serializable {
    fun isBlankFor(type: TrackFieldType): Boolean = when (type) {
        TrackFieldType.ShortText, TrackFieldType.LongText -> textValue.isNullOrBlank()
        TrackFieldType.Number -> enteredNumber == null
        TrackFieldType.SingleChoice -> choiceOptionUuid == null
        TrackFieldType.Scale -> scaleValue == null
        TrackFieldType.Date -> dateValue == null
        TrackFieldType.YesNo -> booleanValue == null
    }
}

data class TrackEntryDraft(
    val entryDate: LocalDate,
    /** Values are keyed by stable Field UUID, never Field position or label. */
    val values: Map<String, TrackValueDraft>,
    val sourceOccurrenceId: Long? = null,
    val sourceExplanation: String = "",
) : Serializable

data class TrackEntryProjection(
    val entry: TrackEntry,
    val values: Map<Long, TrackFieldValue>,
) {
    fun value(fieldId: Long): TrackFieldValue? = values[fieldId]
}

/** A stable database-backed window for rendering very large Track histories. */
data class TrackEntryPage(
    val entries: List<TrackEntryProjection>,
    val offset: Int,
    val totalCount: Int,
) {
    val hasMore: Boolean get() = offset + entries.size < totalCount
    val nextOffset: Int get() = offset + entries.size
}

data class DeletedTrackEntry(
    val entry: TrackEntry,
    val values: List<TrackFieldValue>,
)

data class TrackProjection(
    val track: Track,
    val fields: List<TrackField>,
    val options: List<TrackChoiceOption>,
    val entries: List<TrackEntryProjection>,
) {
    val primaryFields: List<TrackField>
        get() = fields.filter(TrackField::primary).sortedBy(TrackField::position)

    /** The first identity Field is the concise noun used by add/edit actions. */
    val primaryField: TrackField
        get() = primaryFields.first()

    fun optionsFor(fieldId: Long): List<TrackChoiceOption> =
        options.filter { it.fieldId == fieldId }.sortedBy(TrackChoiceOption::position)

    fun primaryText(entry: TrackEntryProjection): String = primaryFields
        .mapNotNull { field -> identityValue(entry, field).takeIf(String::isNotBlank) }
        .joinToString(" · ")
        .ifBlank { "Untitled Entry" }

    /** Case-insensitive composite identity used only to flag possible duplicates. */
    fun identityKey(entry: TrackEntryProjection): String = primaryFields
        .joinToString("\u001f") { field -> identityValue(entry, field).trim().lowercase(Locale.ROOT) }

    private fun identityValue(entry: TrackEntryProjection, field: TrackField): String {
        val value = entry.value(field.id) ?: return ""
        return when (field.type) {
            TrackFieldType.ShortText, TrackFieldType.LongText -> value.textValue.orEmpty()
            TrackFieldType.Number -> value.enteredNumber?.let(::plainTrackNumber)
                ?.plus(value.enteredUnitId?.takeIf(String::isNotBlank)?.let { " $it" }.orEmpty()).orEmpty()
            TrackFieldType.SingleChoice -> options.firstOrNull { it.id == value.choiceOptionId }?.label.orEmpty()
            TrackFieldType.Scale -> value.scaleValue?.let(::formatTrackScaleValue).orEmpty()
            TrackFieldType.Date -> value.dateValue?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)).orEmpty()
            TrackFieldType.YesNo -> value.booleanValue?.let { if (it) "Yes" else "No" }.orEmpty()
        }
    }
}

private fun plainTrackNumber(value: Double): String =
    BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

fun formatTrackScaleValue(value: Double): String = plainTrackNumber(value)

/** Returns every selectable value for a bounded, evenly divisible discrete Scale. */
fun trackScaleValues(minimum: Int, maximum: Int, increment: Double): List<Double> {
    require(minimum < maximum) { "Scale maximum must be greater than its minimum" }
    require(increment.isFinite() && increment > 0.0) { "Scale increment must be greater than zero" }
    val step = BigDecimal.valueOf(increment).stripTrailingZeros()
    require(step.scale().coerceAtLeast(0) <= 6) { "Scale increment can use at most 6 decimal places" }
    val span = BigDecimal.valueOf(maximum.toLong() - minimum.toLong())
    val (intervalsDecimal, remainder) = span.divideAndRemainder(step)
    require(remainder.compareTo(BigDecimal.ZERO) == 0) { "Scale increment must land exactly on the maximum" }
    val intervals = runCatching { intervalsDecimal.intValueExact() }
        .getOrElse { throw IllegalArgumentException("Scale has too many selectable values") }
    require(intervals in 1..1_000) { "Scale can contain at most 1,001 selectable values" }
    val start = BigDecimal.valueOf(minimum.toLong())
    return (0..intervals).map { index ->
        start.add(step.multiply(BigDecimal.valueOf(index.toLong()))).toDouble()
    }
}

fun normalizeTrackScaleValue(
    value: Double,
    minimum: Int,
    maximum: Int,
    increment: Double,
): Double? {
    if (!value.isFinite()) return null
    val choices = trackScaleValues(minimum, maximum, increment)
    val position = ((value - minimum) / increment).roundToInt().coerceIn(0, choices.lastIndex)
    val normalized = choices[position]
    val tolerance = 1e-7 * maxOf(1.0, abs(value), abs(normalized))
    return normalized.takeIf { abs(value - normalized) <= tolerance }
}

fun snapTrackScaleValue(
    value: Double,
    minimum: Int,
    maximum: Int,
    increment: Double,
): Double {
    val choices = trackScaleValues(minimum, maximum, increment)
    return choices.minBy { choice -> abs(choice - value) }
}

fun TrackDraft.validated(): TrackDraft {
    val normalizedName = name.trim().replace(Regex("\\s+"), " ")
    require(normalizedName.isNotBlank()) { "Track name is required" }
    require(normalizedName.length <= 100) { "Track names can be at most 100 characters" }
    require(fields.isNotEmpty()) { "Add at least one Field" }
    require(fields.any(TrackFieldDraft::primary)) { "Choose at least one Entry Identity Field" }
    val normalizedFields = fields.map(TrackFieldDraft::validated)
    require(normalizedFields.filter(TrackFieldDraft::primary).all(TrackFieldDraft::required)) {
        "Every Entry Identity Field must be required"
    }
    val normalizedNames = normalizedFields.map { it.name.lowercase(Locale.ROOT) }
    require(normalizedNames.distinct().size == normalizedNames.size) { "Field names must be unique within a Track" }
    val stableIds = normalizedFields.mapNotNull(TrackFieldDraft::uuid)
    require(stableIds.distinct().size == stableIds.size) { "Field identities must be unique within a Track" }
    return copy(
        name = normalizedName,
        description = description.trim(),
        icon = icon.normalizedIdentityEmoji(DEFAULT_TRACK_EMOJI),
        area = area.trim(),
        tags = tags.map(String::trim).filter(String::isNotBlank).distinctBy { it.lowercase(Locale.ROOT) },
        fields = normalizedFields,
    )
}

private fun TrackFieldDraft.validated(): TrackFieldDraft {
    val normalizedName = name.trim().replace(Regex("\\s+"), " ")
    require(normalizedName.isNotBlank()) { "Field name is required" }
    require(normalizedName.length <= 80) { "Field names can be at most 80 characters" }
    require(precision in 0..6) { "Number precision must be between 0 and 6" }
    val normalizedOptions = options.map { option ->
        option.copy(label = option.label.trim().replace(Regex("\\s+"), " ")).also {
            require(it.label.isNotBlank()) { "Choice labels cannot be blank" }
            require(it.label.length <= 80) { "Choice labels can be at most 80 characters" }
        }
    }
    require(normalizedOptions.map { it.label.lowercase(Locale.ROOT) }.distinct().size == normalizedOptions.size) {
        "Choice labels must be unique within a Field"
    }
    when (type) {
        TrackFieldType.Number -> {
            require(dimension != null && !unitId.isNullOrBlank()) { "Choose a measurement type and unit" }
            require(options.isEmpty()) { "Number Fields cannot contain Choice options" }
        }
        TrackFieldType.SingleChoice -> require(normalizedOptions.isNotEmpty()) { "Add at least one Choice option" }
        TrackFieldType.Scale -> {
            val minimum = requireNotNull(scaleMin) { "Enter a Scale minimum" }
            val maximum = requireNotNull(scaleMax) { "Enter a Scale maximum" }
            trackScaleValues(minimum, maximum, scaleStep)
            require(options.isEmpty()) { "Scale Fields cannot contain Choice options" }
        }
        else -> require(options.isEmpty()) { "Only Single Choice Fields can contain options" }
    }
    return copy(
        name = normalizedName,
        required = required || primary,
        showInList = showInList,
        dimension = dimension.takeIf { type == TrackFieldType.Number },
        unitId = unitId?.takeIf { type == TrackFieldType.Number },
        precision = when (type) {
            TrackFieldType.Number -> precision
            TrackFieldType.Scale -> BigDecimal.valueOf(scaleStep).stripTrailingZeros().scale().coerceIn(0, 6)
            else -> 0
        },
        scaleMin = scaleMin.takeIf { type == TrackFieldType.Scale },
        scaleMax = scaleMax.takeIf { type == TrackFieldType.Scale },
        scaleLowLabel = scaleLowLabel.trim().takeIf { type == TrackFieldType.Scale }.orEmpty(),
        scaleHighLabel = scaleHighLabel.trim().takeIf { type == TrackFieldType.Scale }.orEmpty(),
        scaleStep = scaleStep.takeIf { type == TrackFieldType.Scale } ?: 1.0,
        options = normalizedOptions.takeIf { type == TrackFieldType.SingleChoice }.orEmpty(),
    )
}

fun validateTrackEntryDraft(
    fields: List<TrackField>,
    options: List<TrackChoiceOption>,
    draft: TrackEntryDraft,
) {
    val fieldByUuid = fields.associateBy(TrackField::uuid)
    require(draft.values.keys.all(fieldByUuid::containsKey)) { "Entry contains a Field that no longer exists" }
    fields.forEach { field ->
        val value = draft.values[field.uuid]
        require(!field.required || value?.isBlankFor(field.type) == false) { "${field.name} is required" }
        if (value == null || value.isBlankFor(field.type)) return@forEach
        validateTrackValue(field, options.filter { it.fieldId == field.id }, value)
    }
}

private fun validateTrackValue(
    field: TrackField,
    options: List<TrackChoiceOption>,
    value: TrackValueDraft,
) {
    when (field.type) {
        TrackFieldType.ShortText, TrackFieldType.LongText -> require(value.textValue != null) { "Enter ${field.name}" }
        TrackFieldType.Number -> require(value.enteredNumber?.isFinite() == true) { "Enter a valid ${field.name}" }
        TrackFieldType.SingleChoice -> require(options.any { it.uuid == value.choiceOptionUuid }) {
            "Choose an available ${field.name} option"
        }
        TrackFieldType.Scale -> require(
            normalizeTrackScaleValue(
                requireNotNull(value.scaleValue),
                requireNotNull(field.scaleMin),
                requireNotNull(field.scaleMax),
                field.scaleStep,
            ) != null,
        ) { "Choose ${field.name} using ${formatTrackScaleValue(field.scaleStep)} increments" }
        TrackFieldType.Date -> require(value.dateValue != null) { "Choose ${field.name}" }
        TrackFieldType.YesNo -> require(value.booleanValue != null) { "Choose Yes or No for ${field.name}" }
    }
}
