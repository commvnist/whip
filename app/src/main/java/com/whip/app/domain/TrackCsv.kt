package com.whip.app.domain

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.Serializable
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

const val TRACK_CSV_IMPORT_FINGERPRINT_VERSION = 1
const val TRACK_CSV_ENTRY_IDENTITY_VERSION = 1
const val TRACK_CSV_MAX_IMPORT_ROWS = 5_000
const val TRACK_CSV_MAX_IMPORT_CELLS = 100_000

data class TrackCsvMapping(
    val entryDateColumn: String? = null,
    /** Stable Field UUID to CSV header. */
    val fieldColumns: Map<String, String> = emptyMap(),
    /** Optional per-row entered-unit column for Number Fields. */
    val numberUnitColumns: Map<String, String> = emptyMap(),
) : Serializable

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

/**
 * Process-saveable, exact identity of one previewed CSV batch. The payload and
 * human-readable mapping stay in presentation state; only their digest is
 * persisted in the private receipt table.
 */
data class TrackCsvImportRequest(
    val batchUuid: String,
    val openingFormBoundary: TrackEntryFormBoundary,
    val payloadFingerprint: String,
    val mapping: TrackCsvMapping,
    val defaultEntryDate: LocalDate,
    val requestFingerprint: String,
    val entryUuids: List<String>,
    val entryIdentityDigest: String,
    val rowCount: Int,
    val fingerprintVersion: Int = TRACK_CSV_IMPORT_FINGERPRINT_VERSION,
    val identityVersion: Int = TRACK_CSV_ENTRY_IDENTITY_VERSION,
) : Serializable

data class TrackCsvImportPreparation(
    val request: TrackCsvImportRequest,
    val form: TrackEntryFormSnapshot,
) : Serializable

data class TrackCsvImportReceipt(
    val batchUuid: String,
    val trackId: Long,
    val trackUuid: String,
    val trackCreatedAtMillis: Long,
    val requestFingerprint: String,
    val entryIdentityDigest: String,
    val rowCount: Int,
    val fingerprintVersion: Int,
    val identityVersion: Int,
    val committedAtMillis: Long,
    val changed: Boolean,
    val alreadyApplied: Boolean,
) : Serializable

/** Compact exact expectation saved by presentation for unknown-outcome recovery. */
data class TrackCsvImportReceiptEnvelope(
    val batchUuid: String,
    val trackId: Long,
    val trackUuid: String,
    val trackCreatedAtMillis: Long,
    val requestFingerprint: String,
    val entryIdentityDigest: String,
    val rowCount: Int,
    val fingerprintVersion: Int,
    val identityVersion: Int,
) : Serializable

sealed interface TrackCsvImportReceiptVerification : Serializable {
    data class Missing(val expected: TrackCsvImportReceiptEnvelope) : TrackCsvImportReceiptVerification
    data class Exact(val receipt: TrackCsvImportReceipt) : TrackCsvImportReceiptVerification
    data class Collision(
        val expected: TrackCsvImportReceiptEnvelope,
        val committed: TrackCsvImportReceiptEnvelope,
    ) : TrackCsvImportReceiptVerification
}

fun TrackCsvImportRequest.receiptEnvelope() = TrackCsvImportReceiptEnvelope(
    batchUuid = batchUuid,
    trackId = openingFormBoundary.trackId,
    trackUuid = openingFormBoundary.trackUuid,
    trackCreatedAtMillis = openingFormBoundary.trackCreatedAtMillis,
    requestFingerprint = requestFingerprint,
    entryIdentityDigest = entryIdentityDigest,
    rowCount = rowCount,
    fingerprintVersion = fingerprintVersion,
    identityVersion = identityVersion,
)

fun TrackCsvImportReceipt.receiptEnvelope() = TrackCsvImportReceiptEnvelope(
    batchUuid = batchUuid,
    trackId = trackId,
    trackUuid = trackUuid,
    trackCreatedAtMillis = trackCreatedAtMillis,
    requestFingerprint = requestFingerprint,
    entryIdentityDigest = entryIdentityDigest,
    rowCount = rowCount,
    fingerprintVersion = fingerprintVersion,
    identityVersion = identityVersion,
)

fun TrackCsvImportReceipt.matches(expected: TrackCsvImportReceiptEnvelope): Boolean =
    receiptEnvelope() == expected

enum class TrackCsvImportConflictKind {
    TargetMissing,
    IdentityChanged,
    FormChanged,
    BatchIdentityCollision,
    RequestMalformed,
    EntryIdentityCollision,
}

class TrackCsvImportConflictException(
    val kind: TrackCsvImportConflictKind,
    message: String,
) : IllegalStateException(message)

/** Exact-byte payload digest. URI, filename, and provider metadata are deliberately excluded. */
fun trackCsvPayloadFingerprint(csv: String): String = sha256Hex(csv.toByteArray(StandardCharsets.UTF_8))

/**
 * Builds the only request shape accepted by persistence. Canonicalization is
 * independent of Map iteration order and preserves raw floating-point bits and
 * null versus empty distinctions.
 */
fun prepareTrackCsvImportRequest(
    batchUuid: String,
    openingFormBoundary: TrackEntryFormBoundary,
    payloadFingerprint: String,
    mapping: TrackCsvMapping,
    defaultEntryDate: LocalDate,
    drafts: List<TrackEntryDraft>,
): TrackCsvImportRequest {
    requireCanonicalUuid(batchUuid, "CSV batch")
    require(payloadFingerprint.isSha256Hex()) { "CSV payload fingerprint is malformed" }
    require(drafts.isNotEmpty()) { "There are no valid CSV rows to import" }
    require(drafts.size <= TRACK_CSV_MAX_IMPORT_ROWS) {
        "A CSV import can contain at most $TRACK_CSV_MAX_IMPORT_ROWS valid rows"
    }
    require(drafts.sumOf { it.values.size.toLong() } <= TRACK_CSV_MAX_IMPORT_CELLS) {
        "A CSV import can contain at most $TRACK_CSV_MAX_IMPORT_CELLS mapped values"
    }
    require(openingFormBoundary.trackUuid.isNotBlank()) { "Track identity is required" }
    require(openingFormBoundary.trackCreatedAtMillis >= 0L) { "Track identity is malformed" }
    val fieldsByUuid = openingFormBoundary.fieldContracts.associateBy(TrackEntryFieldContract::uuid)
    require(fieldsByUuid.size == openingFormBoundary.fieldContracts.size) { "CSV form contains duplicate Field identities" }
    require(openingFormBoundary.fieldContracts.map(TrackEntryFieldContract::id).distinct().size == fieldsByUuid.size) {
        "CSV form contains duplicate Field IDs"
    }
    require(openingFormBoundary.choiceContracts.map(TrackEntryChoiceContract::uuid).distinct().size == openingFormBoundary.choiceContracts.size) {
        "CSV form contains duplicate Choice identities"
    }
    require(openingFormBoundary.choiceContracts.map(TrackEntryChoiceContract::id).distinct().size == openingFormBoundary.choiceContracts.size) {
        "CSV form contains duplicate Choice IDs"
    }
    require(openingFormBoundary.unitContracts.map(TrackEntryUnitContract::id).distinct().size == openingFormBoundary.unitContracts.size) {
        "CSV form contains duplicate unit identities"
    }
    require(mapping.entryDateColumn == null || mapping.entryDateColumn.isNotBlank()) {
        "The mapped Entry Date column is blank"
    }
    require(mapping.fieldColumns.keys.all(fieldsByUuid::containsKey)) { "CSV mapping contains an unknown Field" }
    require(mapping.fieldColumns.values.none(String::isBlank)) { "CSV mapping contains a blank column" }
    require(openingFormBoundary.fieldContracts.filter(TrackEntryFieldContract::primary).all { it.uuid in mapping.fieldColumns }) {
        "Map every Entry Identity Field before importing"
    }
    require(mapping.numberUnitColumns.all { (fieldUuid, header) ->
        header.isNotBlank() && fieldsByUuid[fieldUuid]?.type == TrackFieldType.Number && fieldUuid in mapping.fieldColumns
    }) { "CSV unit mapping must target a mapped Number Field and a nonblank column" }
    val normalizedDrafts = normalizeTrackCsvDrafts(openingFormBoundary, drafts)
    val requestFingerprint = CanonicalCsvDigest().apply {
        integer("fingerprintVersion", TRACK_CSV_IMPORT_FINGERPRINT_VERSION)
        form(openingFormBoundary)
        string("payloadFingerprint", payloadFingerprint)
        nullableString("entryDateColumn", mapping.entryDateColumn)
        mapping.fieldColumns.toSortedMap().forEach { (fieldUuid, header) ->
            string("fieldColumn.fieldUuid", fieldUuid)
            string("fieldColumn.header", header)
        }
        mapping.numberUnitColumns.toSortedMap().forEach { (fieldUuid, header) ->
            string("unitColumn.fieldUuid", fieldUuid)
            string("unitColumn.header", header)
        }
        long("defaultEntryDate", defaultEntryDate.toEpochDay())
        normalizedDrafts.forEachIndexed { index, draft -> draft(index, draft) }
    }.digest()
    val entryUuids = normalizedDrafts.indices.map { ordinal ->
        deterministicTrackCsvEntryUuid(batchUuid, requestFingerprint, ordinal)
    }
    val entryIdentityDigest = trackCsvEntryIdentityDigest(entryUuids)
    return TrackCsvImportRequest(
        batchUuid = batchUuid,
        openingFormBoundary = openingFormBoundary,
        payloadFingerprint = payloadFingerprint,
        mapping = mapping,
        defaultEntryDate = defaultEntryDate,
        requestFingerprint = requestFingerprint,
        entryUuids = entryUuids,
        entryIdentityDigest = entryIdentityDigest,
        rowCount = normalizedDrafts.size,
    )
}

fun trackCsvEntryIdentityDigest(
    entryUuids: List<String>,
    identityVersion: Int = TRACK_CSV_ENTRY_IDENTITY_VERSION,
): String {
    require(identityVersion == TRACK_CSV_ENTRY_IDENTITY_VERSION) { "Unsupported CSV Entry identity version" }
    entryUuids.forEach { requireCanonicalUuid(it, "CSV Entry") }
    return CanonicalCsvDigest().apply {
        integer("identityVersion", identityVersion)
        entryUuids.forEachIndexed { ordinal, uuid ->
            integer("ordinal", ordinal)
            string("entryUuid", uuid)
        }
    }.digest()
}

private fun normalizeTrackCsvDrafts(
    boundary: TrackEntryFormBoundary,
    drafts: List<TrackEntryDraft>,
): List<TrackEntryDraft> {
    val fields = boundary.fieldContracts.map { field ->
        TrackField(
            id = field.id,
            uuid = field.uuid,
            trackId = field.trackId,
            name = field.name,
            type = field.type,
            position = 0,
            required = field.required,
            primary = field.primary,
            showInList = false,
            dimension = field.dimension,
            unitId = field.unitId,
            precision = field.precision,
            scaleMin = field.scaleMin,
            scaleMax = field.scaleMax,
            scaleLowLabel = field.scaleLowLabel,
            scaleHighLabel = field.scaleHighLabel,
            createdAtMillis = 0,
            updatedAtMillis = 0,
            scaleStep = field.scaleStep,
        )
    }
    val options = boundary.choiceContracts.map { choice ->
        TrackChoiceOption(choice.id, choice.uuid, choice.fieldId, choice.label, 0, 0, 0)
    }
    val fieldsByUuid = fields.associateBy(TrackField::uuid)
    val unitsById = boundary.unitContracts.associateBy(TrackEntryUnitContract::id)
    return drafts.map { draft ->
        validateTrackEntryDraft(fields, options, draft)
        TrackEntryDraft(
            entryDate = draft.entryDate,
            values = draft.values.toSortedMap().mapNotNull { (fieldUuid, value) ->
                val field = requireNotNull(fieldsByUuid[fieldUuid]) { "Entry contains a Field that no longer exists" }
                if (value.isBlankFor(field.type)) return@mapNotNull null
                val normalized = when (field.type) {
                    TrackFieldType.ShortText, TrackFieldType.LongText -> TrackValueDraft(textValue = value.textValue?.trim())
                    TrackFieldType.Number -> {
                        val unitId = value.enteredUnitId ?: field.unitId
                        val unit = unitId?.let(unitsById::get)
                            ?: error("${field.name}'s unit was not available when this Entry form opened")
                        require(unit.dimension == field.dimension) { "${field.name}'s entry unit is incompatible" }
                        require(!unit.archived || unit.id == field.unitId) {
                            "${unit.name} is archived; restore it before using it for ${field.name}"
                        }
                        TrackValueDraft(enteredNumber = value.enteredNumber, enteredUnitId = unit.id)
                    }
                    TrackFieldType.SingleChoice -> TrackValueDraft(choiceOptionUuid = value.choiceOptionUuid)
                    TrackFieldType.Scale -> TrackValueDraft(
                        scaleValue = normalizeTrackScaleValue(
                            requireNotNull(value.scaleValue),
                            requireNotNull(field.scaleMin),
                            requireNotNull(field.scaleMax),
                            field.scaleStep,
                        ),
                    )
                    TrackFieldType.Date -> TrackValueDraft(dateValue = value.dateValue)
                    TrackFieldType.YesNo -> TrackValueDraft(booleanValue = value.booleanValue)
                }
                fieldUuid to normalized
            }.toMap(linkedMapOf()),
        )
    }
}

fun deterministicTrackCsvEntryUuid(
    batchUuid: String,
    requestFingerprint: String,
    rowOrdinal: Int,
    identityVersion: Int = TRACK_CSV_ENTRY_IDENTITY_VERSION,
): String {
    requireCanonicalUuid(batchUuid, "CSV batch")
    require(requestFingerprint.isSha256Hex()) { "CSV request fingerprint is malformed" }
    require(rowOrdinal >= 0) { "CSV row ordinal cannot be negative" }
    require(identityVersion == TRACK_CSV_ENTRY_IDENTITY_VERSION) { "Unsupported CSV Entry identity version" }
    val bytes = MessageDigest.getInstance("SHA-256").digest(CanonicalCsvBytes().apply {
        integer(identityVersion)
        string(batchUuid)
        string(requestFingerprint)
        integer(rowOrdinal)
    }.bytes()).copyOfRange(0, 16)
    // RFC 9562 UUIDv8 identifies an application-defined deterministic layout.
    bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x80).toByte()
    bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()
    val buffer = java.nio.ByteBuffer.wrap(bytes)
    return UUID(buffer.long, buffer.long).toString()
}

fun trackCsvHeaders(csv: String): List<String> = normalizeTrackCsvHeaders(parseCsv(csv).firstOrNull().orEmpty())

/**
 * Preview against one repository-originated atomic form snapshot. No global
 * projection or later unit cache may silently replace the form the user saw.
 */
fun previewTrackCsvImport(
    form: TrackEntryFormSnapshot,
    csv: String,
    mapping: TrackCsvMapping,
    today: LocalDate,
): TrackCsvImportPreview {
    require(form.hasExactCsvBoundary()) { "The CSV form snapshot is inconsistent" }
    return previewTrackCsvImport(
        projection = TrackProjection(form.track, form.fields, form.options, emptyList()),
        csv = csv,
        mapping = mapping,
        today = today,
        availableUnits = form.units.map { unit ->
            UnitDefinition(
                id = unit.id,
                name = unit.name,
                symbol = unit.symbol,
                dimension = unit.dimension,
                toCanonicalFactor = unit.toCanonicalFactor,
                toCanonicalOffset = unit.toCanonicalOffset,
                archived = unit.archived,
            )
        },
    )
}

fun previewTrackCsvImport(
    projection: TrackProjection,
    csv: String,
    mapping: TrackCsvMapping,
    today: LocalDate,
    availableUnits: List<UnitDefinition> = BuiltInUnits.all,
): TrackCsvImportPreview {
    val rows = parseCsv(csv)
    val headers = normalizeTrackCsvHeaders(rows.firstOrNull().orEmpty())
    require(headers.isNotEmpty()) { "The CSV file has no header row" }
    require(headers.none(String::isBlank)) { "Every CSV column needs a header" }
    require(headers.map { it.lowercase(Locale.ROOT) }.distinct().size == headers.size) {
        "CSV headers must be unique after trimming and ignoring capitalization"
    }
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
        require(rawRow.size <= headers.size) { "CSV row $rowNumber has more values than the header row" }
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
            val matches = availableUnits.filter { unit ->
                unit.id.equals(label, true) || unit.symbol.equals(label, true) || unit.name.equals(label, true)
            }.distinctBy(UnitDefinition::id)
            require(matches.size <= 1) { "unit '$label' is ambiguous; use its exact unit ID" }
            matches.singleOrNull() ?: error("unit '$label' is not available")
        } ?: availableUnits.firstOrNull { it.id == field.unitId }
        require(unit?.dimension == field.dimension) { "unit '${enteredUnit ?: field.unitId}' does not match ${field.dimension?.name}" }
        val selected = requireNotNull(unit)
        require(!selected.archived || selected.id == field.unitId) {
            "${selected.name} is archived; restore it before using it for ${field.name}"
        }
        TrackValueDraft(
            enteredNumber = raw.toWhipDoubleOrNull() ?: error("enter a number"),
            enteredUnitId = selected.id,
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
    var state = CsvCellState.Start
    var index = 0
    while (index < text.length) {
        val char = text[index]
        when (state) {
            CsvCellState.Start -> when (char) {
                '"' -> state = CsvCellState.Quoted
                ',' -> row += ""
                '\n', '\r' -> {
                    if (char == '\r' && index + 1 < text.length && text[index + 1] == '\n') index++
                    row += ""; rows += row; row = mutableListOf()
                }
                else -> { cell.append(char); state = CsvCellState.Unquoted }
            }
            CsvCellState.Unquoted -> when (char) {
                '"' -> error("CSV quotes must begin at the start of a value")
                ',' -> { row += cell.toString(); cell.clear(); state = CsvCellState.Start }
                '\n', '\r' -> {
                    if (char == '\r' && index + 1 < text.length && text[index + 1] == '\n') index++
                    row += cell.toString(); cell.clear(); rows += row; row = mutableListOf(); state = CsvCellState.Start
                }
                else -> cell.append(char)
            }
            CsvCellState.Quoted -> when {
                char != '"' -> cell.append(char)
                index + 1 < text.length && text[index + 1] == '"' -> { cell.append('"'); index++ }
                else -> state = CsvCellState.AfterQuote
            }
            CsvCellState.AfterQuote -> when (char) {
                ',' -> { row += cell.toString(); cell.clear(); state = CsvCellState.Start }
                '\n', '\r' -> {
                    if (char == '\r' && index + 1 < text.length && text[index + 1] == '\n') index++
                    row += cell.toString(); cell.clear(); rows += row; row = mutableListOf(); state = CsvCellState.Start
                }
                else -> error("CSV values cannot contain characters after a closing quote")
            }
        }
        index++
    }
    require(state != CsvCellState.Quoted) { "The CSV contains an unclosed quoted value" }
    if (state != CsvCellState.Start || cell.isNotEmpty() || row.isNotEmpty()) { row += cell.toString(); rows += row }
    return rows.dropLastWhile { it.size == 1 && it.single().isBlank() }
}

private enum class CsvCellState { Start, Unquoted, Quoted, AfterQuote }

fun TrackEntryFormSnapshot.hasExactCsvBoundary(): Boolean {
    val expectedFields = fields.map { field ->
        TrackEntryFieldContract(
            id = field.id,
            uuid = field.uuid,
            trackId = field.trackId,
            name = field.name,
            type = field.type,
            required = field.required,
            primary = field.primary,
            dimension = field.dimension,
            unitId = field.unitId,
            precision = field.precision,
            scaleMin = field.scaleMin,
            scaleMax = field.scaleMax,
            scaleLowLabel = field.scaleLowLabel,
            scaleHighLabel = field.scaleHighLabel,
            scaleStep = field.scaleStep,
        )
    }.sortedBy(TrackEntryFieldContract::id)
    val expectedChoices = options.map { choice ->
        TrackEntryChoiceContract(choice.id, choice.uuid, choice.fieldId, choice.label)
    }.sortedBy(TrackEntryChoiceContract::id)
    return track.id == boundary.trackId && track.uuid == boundary.trackUuid &&
        track.createdAtMillis == boundary.trackCreatedAtMillis && track.archived == !boundary.writable &&
        expectedFields == boundary.fieldContracts.sortedBy(TrackEntryFieldContract::id) &&
        expectedChoices == boundary.choiceContracts.sortedBy(TrackEntryChoiceContract::id) &&
        units.sortedBy(TrackEntryUnitContract::id) == boundary.unitContracts.sortedBy(TrackEntryUnitContract::id)
}

private fun normalizeTrackCsvHeaders(headers: List<String>): List<String> = headers.mapIndexed { index, header ->
    header.trim().let { if (index == 0) it.removePrefix("\uFEFF") else it }
}

private class CanonicalCsvDigest {
    private val bytes = CanonicalCsvBytes()

    fun string(label: String, value: String) { bytes.string(label); bytes.marker(1); bytes.string(value) }
    fun nullableString(label: String, value: String?) {
        bytes.string(label); bytes.marker(if (value == null) 0 else 1); value?.let(bytes::string)
    }
    fun integer(label: String, value: Int) { bytes.string(label); bytes.marker(2); bytes.integer(value) }
    fun long(label: String, value: Long) { bytes.string(label); bytes.marker(3); bytes.long(value) }
    fun boolean(label: String, value: Boolean) { bytes.string(label); bytes.marker(4); bytes.marker(if (value) 1 else 0) }
    fun nullableLong(label: String, value: Long?) {
        bytes.string(label); bytes.marker(if (value == null) 0 else 3); value?.let(bytes::long)
    }
    fun nullableBoolean(label: String, value: Boolean?) {
        bytes.string(label); bytes.marker(if (value == null) 0 else 4); value?.let { bytes.marker(if (it) 1 else 0) }
    }
    fun nullableDouble(label: String, value: Double?) = nullableLong(label, value?.let(java.lang.Double::doubleToRawLongBits))

    fun form(form: TrackEntryFormBoundary) {
        long("track.id", form.trackId)
        string("track.uuid", form.trackUuid)
        long("track.created", form.trackCreatedAtMillis)
        boolean("track.writable", form.writable)
        string("track.semanticRevision", form.semanticRevisionToken)
        form.fieldContracts.sortedWith(compareBy(TrackEntryFieldContract::uuid, TrackEntryFieldContract::id)).forEach { field ->
            long("field.id", field.id); string("field.uuid", field.uuid); long("field.trackId", field.trackId)
            string("field.name", field.name); string("field.type", field.type.name)
            boolean("field.required", field.required); boolean("field.primary", field.primary)
            nullableString("field.dimension", field.dimension?.name); nullableString("field.unitId", field.unitId)
            integer("field.precision", field.precision); nullableLong("field.scaleMin", field.scaleMin?.toLong())
            nullableLong("field.scaleMax", field.scaleMax?.toLong()); string("field.scaleLowLabel", field.scaleLowLabel)
            string("field.scaleHighLabel", field.scaleHighLabel); nullableDouble("field.scaleStep", field.scaleStep)
        }
        form.choiceContracts.sortedWith(compareBy(TrackEntryChoiceContract::uuid, TrackEntryChoiceContract::id)).forEach { choice ->
            long("choice.id", choice.id); string("choice.uuid", choice.uuid); long("choice.fieldId", choice.fieldId)
            string("choice.label", choice.label)
        }
        form.unitContracts.sortedBy(TrackEntryUnitContract::id).forEach { unit ->
            string("unit.id", unit.id); string("unit.name", unit.name); string("unit.symbol", unit.symbol)
            string("unit.dimension", unit.dimension.name); nullableDouble("unit.factor", unit.toCanonicalFactor)
            nullableDouble("unit.offset", unit.toCanonicalOffset); boolean("unit.archived", unit.archived)
        }
    }

    fun draft(ordinal: Int, draft: TrackEntryDraft) {
        integer("draft.ordinal", ordinal); long("draft.entryDate", draft.entryDate.toEpochDay())
        draft.values.toSortedMap().forEach { (fieldUuid, value) ->
            string("value.fieldUuid", fieldUuid); nullableString("value.text", value.textValue)
            nullableDouble("value.enteredNumber", value.enteredNumber); nullableString("value.enteredUnitId", value.enteredUnitId)
            nullableLong("value.date", value.dateValue?.toEpochDay()); nullableBoolean("value.boolean", value.booleanValue)
            nullableString("value.choiceUuid", value.choiceOptionUuid); nullableDouble("value.scale", value.scaleValue)
        }
    }

    fun digest(): String = sha256Hex(bytes.bytes())
}

private class CanonicalCsvBytes {
    private val output = ByteArrayOutputStream()
    private val data = DataOutputStream(output)

    fun marker(value: Int) { data.writeByte(value) }
    fun integer(value: Int) { data.writeInt(value) }
    fun long(value: Long) { data.writeLong(value) }
    fun string(value: String) {
        val encoded = value.toByteArray(StandardCharsets.UTF_8)
        data.writeInt(encoded.size)
        data.write(encoded)
    }
    fun bytes(): ByteArray = output.toByteArray()
}

private fun sha256Hex(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
    .digest(bytes)
    .joinToString("") { byte -> "%02x".format(Locale.ROOT, byte.toInt() and 0xff) }

private fun String.isSha256Hex(): Boolean = length == 64 && all { it in '0'..'9' || it in 'a'..'f' }

private fun requireCanonicalUuid(value: String, label: String) {
    require(runCatching { UUID.fromString(value).toString() == value }.getOrDefault(false)) {
        "$label identity must be a canonical UUID"
    }
}
