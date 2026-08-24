package com.whip.app.domain

import java.time.LocalDate

data class TrackCsvMapping(
    val entryDateColumn: String? = null,
    /** Stable Field UUID to CSV header. */
    val fieldColumns: Map<String, String> = emptyMap(),
    /** Optional per-row entered-unit column for Number Fields. */
    val numberUnitColumns: Map<String, String> = emptyMap(),
)

data class TrackCsvImportIssue(val rowNumber: Int, val message: String)

data class TrackCsvImportPreview(
    val headers: List<String>,
    val totalRows: Int,
    val validDrafts: List<TrackEntryDraft>,
    val issues: List<TrackCsvImportIssue>,
) {
    val validRows: Int get() = validDrafts.size
    val invalidRows: Int get() = issues.map(TrackCsvImportIssue::rowNumber).distinct().size
}

fun trackCsvHeaders(csv: String): List<String> = parseCsv(csv).firstOrNull().orEmpty()

fun previewTrackCsvImport(
    projection: TrackProjection,
    csv: String,
    mapping: TrackCsvMapping,
    today: LocalDate,
    availableUnits: List<UnitDefinition> = BuiltInUnits.all,
): TrackCsvImportPreview {
    val rows = parseCsv(csv)
    val headers = rows.firstOrNull().orEmpty().map(String::trim)
    require(headers.isNotEmpty()) { "The CSV file has no header row" }
    require(headers.none(String::isBlank)) { "Every CSV column needs a header" }
    require(headers.distinct().size == headers.size) { "CSV headers must be unique" }
    val indexByHeader = headers.withIndex().associate { it.value to it.index }
    val missingIdentity = projection.primaryFields.filter { mapping.fieldColumns[it.uuid] !in indexByHeader }
    require(missingIdentity.isEmpty()) {
        "Map every Entry Identity Field before importing: ${missingIdentity.joinToString { it.name }}"
    }
    require(mapping.fieldColumns.values.all { it in indexByHeader }) { "One or more mapped columns no longer exist" }
    require(mapping.numberUnitColumns.values.all { it in indexByHeader }) { "One or more mapped unit columns no longer exist" }
    require(mapping.entryDateColumn == null || mapping.entryDateColumn in indexByHeader) { "The Entry Date column no longer exists" }

    val valid = mutableListOf<TrackEntryDraft>()
    val issues = mutableListOf<TrackCsvImportIssue>()
    rows.drop(1).forEachIndexed { rowIndex, rawRow ->
        val rowNumber = rowIndex + 2
        val row = rawRow + List((headers.size - rawRow.size).coerceAtLeast(0)) { "" }
        if (row.take(headers.size).all(String::isBlank)) return@forEachIndexed
        val values = mutableMapOf<String, TrackValueDraft>()
        projection.fields.forEach { field ->
            val header = mapping.fieldColumns[field.uuid] ?: return@forEach
            val raw = row[indexByHeader.getValue(header)].trim()
            if (raw.isBlank()) return@forEach
            val enteredUnit = mapping.numberUnitColumns[field.uuid]
                ?.let(indexByHeader::get)
                ?.let(row::get)
                ?.trim()
                ?.takeIf(String::isNotBlank)
            runCatching { parseTrackCsvValue(projection, field, raw, enteredUnit, availableUnits) }
                .onSuccess { values[field.uuid] = it }
                .onFailure { issues += TrackCsvImportIssue(rowNumber, "${field.name}: ${it.message ?: "invalid value"}") }
        }
        projection.fields.filter { it.required }.forEach { field ->
            if (values[field.uuid]?.isBlankFor(field.type) != false) {
                issues += TrackCsvImportIssue(rowNumber, "${field.name} is required")
            }
        }
        val entryDate = mapping.entryDateColumn?.let(indexByHeader::get)?.let(row::get)?.trim()?.takeIf(String::isNotBlank)
            ?.let { value -> runCatching { LocalDate.parse(value) }.getOrElse {
                issues += TrackCsvImportIssue(rowNumber, "Entry Date must use YYYY-MM-DD")
                null
            } }
            ?: today
        if (issues.none { it.rowNumber == rowNumber }) valid += TrackEntryDraft(entryDate, values)
    }
    return TrackCsvImportPreview(headers, rows.drop(1).count { row -> row.any(String::isNotBlank) }, valid, issues)
}

private fun parseTrackCsvValue(
    projection: TrackProjection,
    field: TrackField,
    raw: String,
    enteredUnit: String?,
    availableUnits: List<UnitDefinition>,
): TrackValueDraft = when (field.type) {
    TrackFieldType.ShortText, TrackFieldType.LongText -> TrackValueDraft(textValue = raw)
    TrackFieldType.Number -> {
        val unit = enteredUnit?.let { label ->
            availableUnits.firstOrNull { unit ->
                unit.id.equals(label, true) || unit.symbol.equals(label, true) || unit.name.equals(label, true)
            } ?: error("unit '$label' is not available")
        } ?: availableUnits.firstOrNull { it.id == field.unitId }
        require(unit?.dimension == field.dimension) { "unit '${enteredUnit ?: field.unitId}' does not match ${field.dimension?.name}" }
        TrackValueDraft(
            enteredNumber = raw.toWhipDoubleOrNull() ?: error("enter a number"),
            enteredUnitId = requireNotNull(unit).id,
        )
    }
    TrackFieldType.SingleChoice -> projection.optionsFor(field.id).firstOrNull { it.label.equals(raw, ignoreCase = true) }
        ?.let { TrackValueDraft(choiceOptionUuid = it.uuid) }
        ?: error("choose one of ${projection.optionsFor(field.id).joinToString { it.label }}")
    TrackFieldType.Scale -> raw.toWhipDoubleOrNull()?.let { value ->
        normalizeTrackScaleValue(
            value,
            requireNotNull(field.scaleMin),
            requireNotNull(field.scaleMax),
            field.scaleStep,
        )
    }?.let { TrackValueDraft(scaleValue = it) }
        ?: error(
            "enter a value from ${field.scaleMin} to ${field.scaleMax} " +
                "in ${formatTrackScaleValue(field.scaleStep)} increments",
        )
    TrackFieldType.Date -> runCatching { LocalDate.parse(raw) }.getOrNull()?.let { TrackValueDraft(dateValue = it) }
        ?: error("use YYYY-MM-DD")
    TrackFieldType.YesNo -> when (raw.lowercase()) {
        "yes", "true", "1", "y" -> TrackValueDraft(booleanValue = true)
        "no", "false", "0", "n" -> TrackValueDraft(booleanValue = false)
        else -> error("use Yes or No")
    }
}

/** RFC 4180-style parser with quoted commas, escaped quotes, and embedded newlines. */
internal fun parseCsv(text: String): List<List<String>> {
    val rows = mutableListOf<MutableList<String>>()
    var row = mutableListOf<String>()
    val cell = StringBuilder()
    var quoted = false
    var index = 0
    while (index < text.length) {
        val char = text[index]
        when {
            quoted && char == '"' && index + 1 < text.length && text[index + 1] == '"' -> {
                cell.append('"')
                index++
            }
            char == '"' -> quoted = !quoted
            !quoted && char == ',' -> { row += cell.toString(); cell.clear() }
            !quoted && (char == '\n' || char == '\r') -> {
                if (char == '\r' && index + 1 < text.length && text[index + 1] == '\n') index++
                row += cell.toString(); cell.clear(); rows += row; row = mutableListOf()
            }
            else -> cell.append(char)
        }
        index++
    }
    require(!quoted) { "The CSV contains an unclosed quoted value" }
    if (cell.isNotEmpty() || row.isNotEmpty()) { row += cell.toString(); rows += row }
    return rows.dropLastWhile { it.size == 1 && it.single().isBlank() }
}
