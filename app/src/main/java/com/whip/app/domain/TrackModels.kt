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
) : Serializable

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
) : Serializable

data class TrackChoiceOption(
    val id: Long,
    val uuid: String,
    val fieldId: Long,
    val label: String,
    val position: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
) : Serializable

data class TrackEntry(
    val id: Long,
    val uuid: String,
    val trackId: Long,
    val entryDate: LocalDate,
    val sourceOccurrenceId: Long? = null,
    val sourceExplanation: String = "",
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
) : Serializable

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
) : Serializable

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
    val openingFormBoundary: TrackEntryFormBoundary,
    val sourceOccurrence: TrackEntryFulfillmentSnapshot? = null,
    val fulfilledOccurrences: List<TrackEntryFulfillmentSnapshot> = emptyList(),
) : Serializable

/**
 * Compact, process-saveable contract for the Entry form a user actually saw.
 * Presentation-only Track/Field ordering is deliberately excluded; every
 * semantic label, type, unit, range, and Choice identity is included.
 */
data class TrackEntryFormBoundary(
    val trackId: Long,
    val trackUuid: String,
    val trackCreatedAtMillis: Long,
    val writable: Boolean,
    val semanticRevisionToken: String,
    val fieldContracts: List<TrackEntryFieldContract> = emptyList(),
    val choiceContracts: List<TrackEntryChoiceContract> = emptyList(),
    /** Units selectable for this form; only a draft's selected/default unit is checked at commit. */
    val unitContracts: List<TrackEntryUnitContract> = emptyList(),
) : Serializable

/** Exact optimistic-concurrency boundary for one historical Entry. */
data class TrackEntryBoundary(
    val formBoundary: TrackEntryFormBoundary,
    val entryId: Long,
    val entryUuid: String,
    val entryCreatedAtMillis: Long,
    val semanticRevisionToken: String,
    val enteredUnitContracts: List<TrackEntryUnitContract> = emptyList(),
) : Serializable

data class TrackEntryFieldContract(
    val id: Long,
    val uuid: String,
    val trackId: Long,
    val name: String,
    val type: TrackFieldType,
    val required: Boolean,
    val primary: Boolean,
    val dimension: UnitDimension?,
    val unitId: String?,
    val precision: Int,
    val scaleMin: Int?,
    val scaleMax: Int?,
    val scaleLowLabel: String,
    val scaleHighLabel: String,
    val scaleStep: Double,
) : Serializable

data class TrackEntryChoiceContract(
    val id: Long,
    val uuid: String,
    val fieldId: Long,
    val label: String,
) : Serializable

data class TrackEntryUnitContract(
    val id: String,
    val name: String,
    val symbol: String,
    val dimension: UnitDimension,
    val toCanonicalFactor: Double,
    val toCanonicalOffset: Double,
    val archived: Boolean,
) : Serializable

/** Stable identity is allocated before persistence so Create can be retried safely. */
data class TrackEntryCreateRequest(
    val entryUuid: String,
    val openingFormBoundary: TrackEntryFormBoundary,
) : Serializable

/** Exact form rows rendered by an Entry editor, read with its boundary. */
data class TrackEntryFormSnapshot(
    val boundary: TrackEntryFormBoundary,
    val track: Track,
    val fields: List<TrackField>,
    val options: List<TrackChoiceOption>,
    val units: List<TrackEntryUnitContract>,
) : Serializable

data class TrackEntryCreatePreparation(
    val request: TrackEntryCreateRequest,
    val form: TrackEntryFormSnapshot,
) : Serializable

/** Atomic edit/delete opening state; never pair a fresh boundary with a stale Flow row. */
data class TrackEntryEditSnapshot(
    val boundary: TrackEntryBoundary,
    val form: TrackEntryFormSnapshot,
    val draft: TrackEntryDraft,
    val displayName: String,
    val populatedValueCount: Int,
) : Serializable

enum class TrackEntryMutationKind {
    Create,
    Update,
    Delete,
    Restore,
}

enum class TrackEntryConflictKind {
    TargetMissing,
    ParentMissing,
    IdentityChanged,
    FormChanged,
    EntryChanged,
    IdentityCollision,
    ProvenanceChanged,
    RestoreIncompatible,
    OutcomeUnknown,
}

class TrackEntryConflictException(
    val kind: TrackEntryConflictKind,
    message: String,
) : IllegalStateException(message)

/** Exact historical Trigger occurrence captured before its Entry link is cleared. */
data class TrackEntryFulfillmentSnapshot(
    val id: Long,
    val triggerRuleId: Long,
    val sourceEventId: String,
    val availableAtMillis: Long,
    val deliveredAtMillis: Long?,
    val dismissedAtMillis: Long?,
    val remindAtMillis: Long?,
    val fulfilledEntryId: Long?,
    val sourceSnapshot: String,
) : Serializable

/** Authoritative result built inside the transaction that committed the mutation. */
data class TrackEntryMutationReceipt(
    val kind: TrackEntryMutationKind,
    val trackId: Long,
    val trackUuid: String,
    val entryId: Long,
    val entryUuid: String,
    val changed: Boolean,
    val alreadyApplied: Boolean,
    val affectedValueCount: Int,
    val postBoundary: TrackEntryBoundary? = null,
    val deletedEntry: DeletedTrackEntry? = null,
    val warnings: List<String> = emptyList(),
) : Serializable

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

/**
 * Compact, process-saveable optimistic-concurrency boundary for an authored
 * Track definition. Performance history is deliberately excluded so ordinary
 * Entry logging does not invalidate a non-destructive definition edit.
 */
data class TrackDefinitionBoundary(
    val trackId: Long,
    val trackUuid: String,
    val trackCreatedAtMillis: Long,
    val semanticRevisionToken: String,
) : Serializable

/** User-facing impact of removing one persisted Field from a Track definition. */
data class TrackFieldRemovalImpact(
    val fieldId: Long,
    val fieldUuid: String,
    val fieldName: String,
    val savedValueCount: Int,
    val childChoiceCount: Int,
    val legacyLinkSourceCount: Int,
    val legacyLinkConditionCount: Int,
    val legacyTriggerConditionCount: Int,
    val legacyTriggerMappingCount: Int,
) : Serializable {
    val legacyLinkReferenceCount: Int
        get() = legacyLinkSourceCount + legacyLinkConditionCount

    val legacyTriggerReferenceCount: Int
        get() = legacyTriggerConditionCount + legacyTriggerMappingCount
}

/**
 * User-facing impact of deleting or replacing one persisted Choice. References
 * are retained only as dormant compatibility metadata since automation retired.
 */
data class TrackChoiceRemovalImpact(
    val optionId: Long,
    val optionUuid: String,
    val fieldId: Long,
    val fieldName: String,
    val optionLabel: String,
    val savedValueCount: Int,
    val replacementOptionId: Long? = null,
    val replacementOptionLabel: String? = null,
    val legacyLinkConditionCount: Int,
    val legacyTriggerConditionCount: Int,
    val legacyTriggerMappingCount: Int,
    val removedWithField: Boolean,
) : Serializable {
    val replacesSavedValues: Boolean get() = replacementOptionId != null
    val legacyLinkReferenceCount: Int get() = legacyLinkConditionCount
    val legacyTriggerReferenceCount: Int
        get() = legacyTriggerConditionCount + legacyTriggerMappingCount
}

/**
 * Exact reviewed destructive subset of a Track-definition save. The removal
 * token covers the plan, affected values, and dormant Link/Trigger rows.
 */
data class TrackDefinitionRemovalReview(
    val trackId: Long,
    val definitionRevisionToken: String,
    val removalRevisionToken: String,
    val removedFields: List<TrackFieldRemovalImpact>,
    val removedChoices: List<TrackChoiceRemovalImpact>,
    val choiceReplacementIds: Map<Long, Long> = emptyMap(),
) : Serializable {
    val hasRemovals: Boolean get() = removedFields.isNotEmpty() || removedChoices.isNotEmpty()
}

/** Authoritative result of one committed Track-definition mutation. */
data class TrackDefinitionSaveReceipt(
    val trackId: Long,
    val schemaChanged: Boolean,
    val removedFieldCount: Int = 0,
    val removedChoiceCount: Int = 0,
    val deletedValueCount: Int = 0,
    val replacedValueCount: Int = 0,
    val legacyLinkReferenceCount: Int = 0,
    val legacyTriggerReferenceCount: Int = 0,
    val warnings: List<String> = emptyList(),
) : Serializable

enum class TrackDefinitionConflictKind {
    TargetMissing,
    IdentityChanged,
    DefinitionChanged,
    RemovalImpactChanged,
    ReplacementUnavailable,
}

class TrackDefinitionConflictException(
    val kind: TrackDefinitionConflictKind,
    message: String,
) : IllegalStateException(message)

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
    require(tags.none { ',' in it }) { "Use separate Tags instead of commas" }
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
    val databaseIds = normalizedFields.mapNotNull(TrackFieldDraft::id)
    require(databaseIds.distinct().size == databaseIds.size) { "Field identities must be unique within a Track" }
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
    val optionUuids = normalizedOptions.mapNotNull(TrackChoiceOptionDraft::uuid)
    require(optionUuids.distinct().size == optionUuids.size) { "Choice identities must be unique within a Field" }
    val optionIds = normalizedOptions.mapNotNull(TrackChoiceOptionDraft::id)
    require(optionIds.distinct().size == optionIds.size) { "Choice identities must be unique within a Field" }
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
        if (value != null) validateTrackValueShape(field, value)
        require(!field.required || value?.isBlankFor(field.type) == false) { "${field.name} is required" }
        if (value == null || value.isBlankFor(field.type)) return@forEach
        validateTrackValue(field, options.filter { it.fieldId == field.id }, value)
    }
}

/**
 * Reject payload left behind by a stale editor after a Field type change.
 * Merely looking at the column for the current type would make a valid value
 * for the old type appear blank and could silently discard authored data.
 */
private fun validateTrackValueShape(field: TrackField, value: TrackValueDraft) {
    val unexpected = when (field.type) {
        TrackFieldType.ShortText, TrackFieldType.LongText ->
            value.enteredNumber != null || value.enteredUnitId != null || value.dateValue != null ||
                value.booleanValue != null || value.choiceOptionUuid != null || value.scaleValue != null
        TrackFieldType.Number ->
            value.textValue != null || value.dateValue != null || value.booleanValue != null ||
                value.choiceOptionUuid != null || value.scaleValue != null
        TrackFieldType.SingleChoice ->
            value.textValue != null || value.enteredNumber != null || value.enteredUnitId != null ||
                value.dateValue != null || value.booleanValue != null || value.scaleValue != null
        TrackFieldType.Scale ->
            value.textValue != null || value.enteredNumber != null || value.enteredUnitId != null ||
                value.dateValue != null || value.booleanValue != null || value.choiceOptionUuid != null
        TrackFieldType.Date ->
            value.textValue != null || value.enteredNumber != null || value.enteredUnitId != null ||
                value.booleanValue != null || value.choiceOptionUuid != null || value.scaleValue != null
        TrackFieldType.YesNo ->
            value.textValue != null || value.enteredNumber != null || value.enteredUnitId != null ||
                value.dateValue != null || value.choiceOptionUuid != null || value.scaleValue != null
    }
    require(!unexpected) {
        "${field.name} contains a value for a different Field type. Review the latest Entry form."
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
