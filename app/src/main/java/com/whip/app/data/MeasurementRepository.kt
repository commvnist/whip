package com.whip.app.data

import androidx.room.withTransaction
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.domain.Area
import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.CustomUnitBoundary
import com.whip.app.domain.customUnitBoundary
import com.whip.app.domain.MeasurementDefinition
import com.whip.app.domain.MeasurementEntry
import com.whip.app.domain.MeasurementEntryStatus
import com.whip.app.domain.MeasurementSourceType
import com.whip.app.domain.MeasurementValueKind
import com.whip.app.domain.HealthSourceRecord
import com.whip.app.domain.HealthSourceWindow
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.WhipTag
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.math.sign

interface MeasurementRepository {
    val customUnits: Flow<List<UnitDefinition>>
    val measurements: Flow<List<MeasurementDefinition>>
    val entries: Flow<List<MeasurementEntry>>
    val areas: Flow<List<Area>>
    val tags: Flow<List<WhipTag>>

    suspend fun createMeasurement(
        name: String,
        valueKind: MeasurementValueKind,
        dimension: UnitDimension,
        defaultUnitId: String,
        precision: Int = 1,
    ): String

    suspend fun ensureMeasurement(
        id: String,
        name: String,
        valueKind: MeasurementValueKind,
        dimension: UnitDimension,
        defaultUnitId: String,
        precision: Int = 1,
    ): String

    suspend fun createCustomUnit(
        name: String,
        symbol: String,
        dimension: UnitDimension,
        toCanonicalFactor: Double,
    ): String

    suspend fun ensureCustomUnit(
        id: String,
        name: String,
        symbol: String,
        dimension: UnitDimension,
        toCanonicalFactor: Double = 1.0,
    ): String

    suspend fun renameCustomUnit(id: String, name: String, symbol: String)
    suspend fun setCustomUnitArchived(id: String, archived: Boolean)
    suspend fun createCustomUnitVersion(
        sourceId: String,
        name: String,
        symbol: String,
        toCanonicalFactor: Double,
    ): String

    suspend fun createCustomUnitExact(
        requestedId: String,
        name: String,
        symbol: String,
        dimension: UnitDimension,
        toCanonicalFactor: Double,
    ): String

    suspend fun renameCustomUnitExact(boundary: CustomUnitBoundary, name: String, symbol: String)
    suspend fun setCustomUnitArchivedExact(boundary: CustomUnitBoundary, archived: Boolean)
    suspend fun createCustomUnitVersionExact(
        boundary: CustomUnitBoundary,
        requestedId: String,
        name: String,
        symbol: String,
        toCanonicalFactor: Double,
    ): String

    suspend fun ensureArea(name: String): String
    suspend fun ensureTag(name: String): String
    suspend fun createOrRestoreTag(name: String): String
    suspend fun renameArea(id: String, name: String)
    suspend fun renameTag(id: String, name: String)
    suspend fun mergeTags(sourceId: String, targetId: String)
    suspend fun setAreaArchived(id: String, archived: Boolean)
    suspend fun setTagArchived(id: String, archived: Boolean)
    suspend fun moveArea(id: String, direction: Int)

    suspend fun record(
        measurementId: String,
        value: Double?,
        unitId: String?,
        status: MeasurementEntryStatus = MeasurementEntryStatus.Recorded,
        timestamp: Instant? = null,
        localDate: LocalDate? = null,
        zoneId: ZoneId? = null,
        sourceType: MeasurementSourceType = MeasurementSourceType.Manual,
        sourceId: String? = null,
        note: String = "",
        existingEntryId: String? = null,
        createIfMissingForHealthReconciliation: Boolean = false,
    ): String

    suspend fun deleteEntry(entryId: String)
    suspend fun deleteSourceEntriesExcept(
        sourceType: MeasurementSourceType,
        sourcePrefix: String,
        retainedEntryIds: Set<String>,
    )

    /** Atomically applies every selected authoritative Health Connect source window. */
    suspend fun reconcileHealthSourceWindows(windows: List<HealthSourceWindow>): Int

    /** Deletes Whip's local Health Connect mirror without touching provider records. */
    suspend fun deleteHealthConnectEntries(): Int
}

class RoomMeasurementRepository(
    private val database: WhipDatabase,
    private val clock: WhipClock,
    private val ids: WhipIdGenerator,
) : MeasurementRepository {
    private val dao = database.measurementDao()
    private val areaRepository = RoomAreaRepository(database, clock, ids)

    override val customUnits = dao.observeCustomUnits().map { list -> list.map { it.toDomain() } }
    override val measurements = dao.observeMeasurements().map { list -> list.map { it.toDomain() } }
    override val entries = dao.observeEntries().map { list -> list.map { it.toDomain() } }
    override val areas = areaRepository.areas
    override val tags = dao.observeTags().map { list -> list.map { it.toDomain() } }

    override suspend fun createMeasurement(
        name: String,
        valueKind: MeasurementValueKind,
        dimension: UnitDimension,
        defaultUnitId: String,
        precision: Int,
    ): String = database.withTransaction {
        require(name.isNotBlank()) { "Measurement name is required" }
        val unit = findUnit(defaultUnitId) ?: error("Unknown unit")
        require(unit.dimension == dimension) { "The selected unit has an incompatible dimension" }
        val now = clock.now().toEpochMilli()
        val id = ids.nextId()
        dao.upsertMeasurement(
            MeasurementDefinitionEntity(
                id = id,
                name = name.trim(),
                valueKind = valueKind.name,
                dimension = dimension.name,
                defaultUnitId = defaultUnitId,
                precision = precision.coerceIn(0, 6),
                dimensionLocked = false,
                archived = false,
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
        )
        id
    }

    override suspend fun ensureMeasurement(
        id: String,
        name: String,
        valueKind: MeasurementValueKind,
        dimension: UnitDimension,
        defaultUnitId: String,
        precision: Int,
    ): String = database.withTransaction {
        require(id.isNotBlank()) { "Measurement ID is required" }
        val unit = findUnit(defaultUnitId) ?: error("Unknown unit")
        require(unit.dimension == dimension) { "The selected unit has an incompatible dimension" }
        val existing = dao.getMeasurement(id)
        if (existing != null) {
            require(existing.dimension == dimension.name) { "The measurement already uses another dimension" }
            require(existing.valueKind == valueKind.name) { "The measurement already uses another value type" }
            require(findUnit(existing.defaultUnitId)?.dimension == dimension) {
                "The measurement's existing default unit is incompatible"
            }
            return@withTransaction id
        }
        val now = clock.now().toEpochMilli()
        dao.upsertMeasurement(
            MeasurementDefinitionEntity(
                id = id, name = name.trim(), valueKind = valueKind.name, dimension = dimension.name,
                defaultUnitId = defaultUnitId, precision = precision.coerceIn(0, 6),
                dimensionLocked = false, archived = false, createdAtMillis = now, updatedAtMillis = now,
            ),
        )
        id
    }

    override suspend fun createCustomUnit(
        name: String,
        symbol: String,
        dimension: UnitDimension,
        toCanonicalFactor: Double,
    ): String = createCustomUnitExact(ids.nextId(), name, symbol, dimension, toCanonicalFactor)

    override suspend fun ensureArea(name: String): String = areaRepository.create(name)

    override suspend fun ensureTag(name: String): String = ensureTag(name, restoreArchived = false)

    override suspend fun createOrRestoreTag(name: String): String = ensureTag(name, restoreArchived = true)

    private suspend fun ensureTag(name: String, restoreArchived: Boolean): String = database.withTransaction {
        val normalized = validateTagName(name)
        val existing = dao.getTagsSnapshot().firstOrNull { it.name.equals(normalized, true) }
        if (existing != null) {
            if (restoreArchived && existing.archived) {
                dao.upsertTag(existing.copy(archived = false, updatedAtMillis = clock.now().toEpochMilli()))
            }
            return@withTransaction existing.id
        }
        val now = clock.now().toEpochMilli()
        val id = ids.nextId()
        dao.upsertTag(TagEntity(id, normalized, false, now, now))
        id
    }

    override suspend fun renameArea(id: String, name: String) = areaRepository.rename(id, name)

    override suspend fun renameTag(id: String, name: String) = database.withTransaction {
        val existing = requireNotNull(dao.getTag(id)) { "Tag no longer exists" }
        val normalized = validateTagName(name)
        val target = dao.getTagsSnapshot().firstOrNull { it.id != id && it.name.equals(normalized, true) }
        target?.let { existingTarget ->
            error(
                if (existingTarget.archived) {
                    "An archived Tag named #${existingTarget.name} already exists. Restore it before merging, or choose another name."
                } else {
                    "A Tag named #${existingTarget.name} already exists. Use Merge instead."
                },
            )
        }
        val now = clock.now().toEpochMilli()
        val trackSearchChanged = replaceTagName(existing.name, normalized, now)
        dao.upsertTag(existing.copy(name = normalized, updatedAtMillis = now))
        if (trackSearchChanged) RoomTrackRepository(database, clock, ids).rebuildSearchIndex()
    }

    override suspend fun mergeTags(sourceId: String, targetId: String) = database.withTransaction {
        require(sourceId != targetId) { "Choose two different Tags" }
        val source = requireNotNull(dao.getTag(sourceId)) { "Source Tag no longer exists" }
        val target = requireNotNull(dao.getTag(targetId)) { "Destination Tag no longer exists" }
        require(!target.archived) { "Choose an active destination Tag" }
        val trackSearchChanged = replaceTagName(source.name, target.name, clock.now().toEpochMilli())
        if (trackSearchChanged) RoomTrackRepository(database, clock, ids).rebuildSearchIndex()
        check(dao.deleteTag(sourceId) == 1) { "Source Tag could not be removed" }
    }

    override suspend fun setAreaArchived(id: String, archived: Boolean) = areaRepository.setArchived(id, archived)

    override suspend fun setTagArchived(id: String, archived: Boolean) = database.withTransaction {
        val existing = requireNotNull(dao.getTag(id)) { "Tag no longer exists" }
        dao.upsertTag(existing.copy(archived = archived, updatedAtMillis = clock.now().toEpochMilli()))
    }

    override suspend fun moveArea(id: String, direction: Int) = areaRepository.move(id, direction)

    private fun replaceTagName(old: String, new: String, now: Long): Boolean {
        val db = database.openHelper.writableDatabase
        var trackSearchChanged = false
        listOf("tasks", "habits", "goals", "tracks").forEach { table ->
            db.query("SELECT id, tagsCsv FROM $table").use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow("id")
                val tagsIndex = cursor.getColumnIndexOrThrow("tagsCsv")
                while (cursor.moveToNext()) {
                    val tags = cursor.getString(tagsIndex).split(',').map(String::trim).filter(String::isNotBlank)
                    if (tags.none { it.equals(old, true) }) continue
                    val replaced = tags.map { if (it.equals(old, true)) new else it }
                        .distinctBy { it.lowercase(Locale.ROOT) }.joinToString(",")
                    db.execSQL(
                        "UPDATE $table SET tagsCsv = ?, updatedAtMillis = ? WHERE id = ?",
                        arrayOf<Any>(replaced, now, cursor.getLong(idIndex)),
                    )
                    if (table == "tracks") trackSearchChanged = true
                }
            }
        }
        return trackSearchChanged
    }

    private fun validateTagName(name: String): String = name.trim().also { normalized ->
        require(normalized.isNotBlank()) { "Tag name is required" }
        require(',' !in normalized) { "Use separate Tags instead of commas" }
        require(normalized.length <= 100) { "Tag names can be at most 100 characters" }
    }

    override suspend fun ensureCustomUnit(
        id: String,
        name: String,
        symbol: String,
        dimension: UnitDimension,
        toCanonicalFactor: Double,
    ): String = database.withTransaction {
        val normalizedId = id.trim()
        require(normalizedId.isNotBlank()) { "Unit ID is required" }
        val existing = findUnit(normalizedId)
        if (existing != null) {
            require(existing.dimension == dimension) { "The unit already belongs to ${existing.dimension}" }
            return@withTransaction existing.id
        }
        require(toCanonicalFactor.isFinite() && toCanonicalFactor > 0.0) { "Conversion factor must be a finite positive number" }
        val now = clock.now().toEpochMilli()
        dao.upsertUnit(
            UnitDefinitionEntity(
                id = normalizedId, name = name.trim().ifBlank { normalizedId }, symbol = symbol.trim(),
                dimension = dimension.name, toCanonicalFactor = toCanonicalFactor,
                toCanonicalOffset = 0.0, custom = true, archived = false, createdAtMillis = now, updatedAtMillis = now,
            ),
        )
        normalizedId
    }

    override suspend fun renameCustomUnit(id: String, name: String, symbol: String) {
        val existing = requireNotNull(dao.getUnit(id)) { "Custom unit no longer exists" }.toDomain()
        renameCustomUnitExact(existing.customUnitBoundary(), name, symbol)
    }

    override suspend fun setCustomUnitArchived(id: String, archived: Boolean) {
        val existing = requireNotNull(dao.getUnit(id)) { "Custom unit no longer exists" }.toDomain()
        setCustomUnitArchivedExact(existing.customUnitBoundary(), archived)
    }

    override suspend fun createCustomUnitVersion(
        sourceId: String,
        name: String,
        symbol: String,
        toCanonicalFactor: Double,
    ): String {
        val existing = requireNotNull(dao.getUnit(sourceId)) { "Custom unit no longer exists" }.toDomain()
        return createCustomUnitVersionExact(
            existing.customUnitBoundary(),
            ids.nextId(),
            name,
            symbol,
            toCanonicalFactor,
        )
    }

    override suspend fun createCustomUnitExact(
        requestedId: String,
        name: String,
        symbol: String,
        dimension: UnitDimension,
        toCanonicalFactor: Double,
    ): String = database.withTransaction {
        val normalizedName = validateCustomUnitName(name)
        val normalizedSymbol = validateCustomUnitSymbol(symbol)
        require(requestedId.isNotBlank()) { "Custom unit request identity is required" }
        require(BuiltInUnits.get(requestedId) == null) { "Custom unit identity conflicts with a built-in unit" }
        require(toCanonicalFactor.isFinite() && toCanonicalFactor > 0.0) {
            "Conversion factor must be a finite positive number"
        }
        val existing = dao.getUnit(requestedId)
        if (existing != null) {
            require(
                existing.custom &&
                    existing.name == normalizedName &&
                    existing.symbol == normalizedSymbol &&
                    existing.dimension == dimension.name &&
                    existing.toCanonicalFactor == toCanonicalFactor &&
                    existing.toCanonicalOffset == 0.0 &&
                    !existing.archived,
            ) { "Custom unit request identity already belongs to different data" }
            return@withTransaction requestedId
        }
        val now = clock.now().toEpochMilli()
        dao.upsertUnit(
            UnitDefinitionEntity(
                id = requestedId,
                name = normalizedName,
                symbol = normalizedSymbol,
                dimension = dimension.name,
                toCanonicalFactor = toCanonicalFactor,
                toCanonicalOffset = 0.0,
                custom = true,
                archived = false,
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
        )
        requestedId
    }

    override suspend fun renameCustomUnitExact(boundary: CustomUnitBoundary, name: String, symbol: String) =
        database.withTransaction {
            val normalizedName = validateCustomUnitName(name)
            val normalizedSymbol = validateCustomUnitSymbol(symbol)
            val existing = requireNotNull(dao.getUnit(boundary.id)) { "Custom unit no longer exists" }
            require(existing.custom) { "Built-in units cannot be edited" }
            if (existing.name == normalizedName && existing.symbol == normalizedSymbol) return@withTransaction
            require(existing.toDomain().customUnitBoundary() == boundary) {
                "Custom unit changed after this editor opened; review it and try again"
            }
            dao.upsertUnit(
                existing.copy(
                    name = normalizedName,
                    symbol = normalizedSymbol,
                    updatedAtMillis = clock.now().toEpochMilli(),
                ),
            )
        }

    override suspend fun setCustomUnitArchivedExact(boundary: CustomUnitBoundary, archived: Boolean) =
        database.withTransaction {
            val existing = requireNotNull(dao.getUnit(boundary.id)) { "Custom unit no longer exists" }
            require(existing.custom) { "Built-in units cannot be archived" }
            if (existing.archived == archived) return@withTransaction
            require(existing.toDomain().customUnitBoundary() == boundary) {
                "Custom unit changed before this action was saved; review it and try again"
            }
            dao.upsertUnit(existing.copy(archived = archived, updatedAtMillis = clock.now().toEpochMilli()))
        }

    override suspend fun createCustomUnitVersionExact(
        boundary: CustomUnitBoundary,
        requestedId: String,
        name: String,
        symbol: String,
        toCanonicalFactor: Double,
    ): String = database.withTransaction {
        val normalizedName = validateCustomUnitName(name)
        val normalizedSymbol = validateCustomUnitSymbol(symbol)
        require(requestedId.isNotBlank()) { "Custom unit version request identity is required" }
        require(BuiltInUnits.get(requestedId) == null) { "Custom unit version identity conflicts with a built-in unit" }
        require(toCanonicalFactor.isFinite() && toCanonicalFactor > 0.0) {
            "Conversion factor must be a finite positive number"
        }
        dao.getUnit(requestedId)?.let { achieved ->
            require(
                achieved.custom &&
                    achieved.name == normalizedName &&
                    achieved.symbol == normalizedSymbol &&
                    achieved.dimension == boundary.dimension.name &&
                    achieved.toCanonicalFactor == toCanonicalFactor &&
                    achieved.toCanonicalOffset == boundary.toCanonicalOffset &&
                    !achieved.archived,
            ) { "Custom unit version request identity already belongs to different data" }
            return@withTransaction requestedId
        }
        val source = requireNotNull(dao.getUnit(boundary.id)) { "Custom unit no longer exists" }
        require(source.custom) { "Built-in units cannot be versioned" }
        require(source.toDomain().customUnitBoundary() == boundary) {
            "Custom unit changed after this editor opened; review it and try again"
        }
        require(!source.archived) { "Restore this custom unit before creating a new conversion version" }
        val now = clock.now().toEpochMilli()
        dao.upsertUnit(source.copy(archived = true, updatedAtMillis = now))
        dao.upsertUnit(
            source.copy(
                id = requestedId,
                name = normalizedName,
                symbol = normalizedSymbol,
                toCanonicalFactor = toCanonicalFactor,
                archived = false,
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
        )
        requestedId
    }

    override suspend fun record(
        measurementId: String,
        value: Double?,
        unitId: String?,
        status: MeasurementEntryStatus,
        timestamp: Instant?,
        localDate: LocalDate?,
        zoneId: ZoneId?,
        sourceType: MeasurementSourceType,
        sourceId: String?,
        note: String,
        existingEntryId: String?,
        createIfMissingForHealthReconciliation: Boolean,
    ): String = database.withTransaction {
        require(!createIfMissingForHealthReconciliation || existingEntryId != null) {
            "Reconciliation requires a stable measurement ID"
        }
        val measurement = dao.getMeasurement(measurementId)?.toDomain() ?: error("Measurement no longer exists")
        require((value == null) == (unitId == null)) {
            "Measurement value and unit must be provided together"
        }
        val unit = unitId?.let { findUnit(it) }
        require(value == null || value.isFinite()) { "Measurement value must be a finite number" }
        if (status == MeasurementEntryStatus.Recorded) {
            requireNotNull(value) { "A value is required" }
            requireNotNull(unit) { "A unit is required" }
            require(unit.dimension == measurement.dimension) { "Incompatible unit" }
        }
        val effectiveTimestamp = timestamp ?: clock.now()
        val effectiveZone = zoneId ?: clock.zoneId()
        val effectiveDate = localDate
            ?: timestamp?.atZone(effectiveZone)?.toLocalDate()
            ?: clock.today(effectiveZone)
        val now = clock.now().toEpochMilli()
        val entryId = existingEntryId ?: ids.nextId()
        val existing = existingEntryId?.let { existingId ->
            val stored = dao.getEntry(existingId)
            if (stored == null) {
                require(createIfMissingForHealthReconciliation) { "Measurement no longer exists" }
                require(sourceType == MeasurementSourceType.HealthConnect && !sourceId.isNullOrBlank()) {
                    "Only identified Health Connect records can be reconciled"
                }
                require(existingId == "entry-$sourceId") {
                    "Health reconciliation requires the source's stable measurement ID"
                }
            } else {
                require(stored.measurementId == measurementId) { "Measurement belongs to another measurement" }
                require(stored.sourceType == sourceType.name && stored.sourceId == sourceId) {
                    "Measurement provenance cannot be changed"
                }
            }
            stored
        }
        val canonicalValue = value?.let { requireNotNull(unit).toCanonical(it) }
        require(canonicalValue == null || canonicalValue.isFinite()) {
            "Converted measurement value must be finite"
        }
        val isFirstEntry = existing == null && dao.entryCount(measurementId) == 0
        dao.upsertEntry(
            MeasurementEntryEntity(
                id = entryId,
                measurementId = measurementId,
                canonicalValue = canonicalValue,
                enteredValue = value,
                enteredUnitId = unitId,
                status = status.name,
                timestampMillis = effectiveTimestamp.toEpochMilli(),
                localEpochDay = effectiveDate.toEpochDay(),
                zoneId = effectiveZone.id,
                offsetSeconds = effectiveZone.rules.getOffset(effectiveTimestamp).totalSeconds,
                sourceType = sourceType.name,
                sourceId = sourceId,
                note = note.trim(),
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now,
            ),
        )
        if (isFirstEntry && !measurement.dimensionLocked) {
            dao.upsertMeasurement(
                measurement.toEntity().copy(dimensionLocked = true, updatedAtMillis = now),
            )
        }
        entryId
    }

    override suspend fun deleteEntry(entryId: String) = dao.deleteEntry(entryId)

    override suspend fun deleteSourceEntriesExcept(
        sourceType: MeasurementSourceType,
        sourcePrefix: String,
        retainedEntryIds: Set<String>,
    ) = database.withTransaction {
        dao.getEntriesBySourcePrefix(sourceType.name, sourcePrefix)
            .filterNot { it.id in retainedEntryIds }
            .forEach { dao.deleteEntry(it.id) }
    }

    override suspend fun reconcileHealthSourceWindows(windows: List<HealthSourceWindow>): Int =
        database.withTransaction {
            require(windows.isNotEmpty()) { "Choose at least one Health Connect data type" }
            require(windows.map { it.measurement.id }.distinct().size == windows.size) {
                "Health sync contains a duplicate measurement window"
            }
            require(windows.map { it.sourcePrefix }.distinct().size == windows.size) {
                "Health sync contains a duplicate source window"
            }
            val now = clock.now().toEpochMilli()
            var imported = 0

            windows.forEach { window ->
                require(HEALTH_SOURCE_PREFIX.matches(window.sourcePrefix)) {
                    "Health source prefix is invalid"
                }
                require(window.startInclusive < window.endExclusive) { "Health source window is invalid" }
                val defaultUnit = findUnit(window.measurement.defaultUnitId) ?: error("Unknown Health measurement unit")
                require(defaultUnit.dimension == window.measurement.dimension) { "Health measurement unit is incompatible" }
                require(window.measurement.precision in 0..6) { "Health measurement precision is invalid" }
                val storedMeasurement = dao.getMeasurement(window.measurement.id)
                if (storedMeasurement == null) {
                    dao.upsertMeasurement(
                        MeasurementDefinitionEntity(
                            id = window.measurement.id,
                            name = window.measurement.name,
                            valueKind = window.measurement.valueKind.name,
                            dimension = window.measurement.dimension.name,
                            defaultUnitId = window.measurement.defaultUnitId,
                            precision = window.measurement.precision,
                            dimensionLocked = false,
                            archived = false,
                            createdAtMillis = now,
                            updatedAtMillis = now,
                        ),
                    )
                } else {
                    require(storedMeasurement.name == window.measurement.name) { "Health measurement name does not match its reserved contract" }
                    require(storedMeasurement.valueKind == window.measurement.valueKind.name) { "Health measurement value type does not match" }
                    require(storedMeasurement.dimension == window.measurement.dimension.name) { "Health measurement dimension does not match" }
                    require(storedMeasurement.defaultUnitId == window.measurement.defaultUnitId) { "Health measurement unit does not match" }
                    require(storedMeasurement.precision == window.measurement.precision) { "Health measurement precision does not match" }
                    require(!storedMeasurement.archived) { "Health measurement is archived" }
                }
                val measurement = requireNotNull(dao.getMeasurement(window.measurement.id)).toDomain()
                val hadEntries = dao.entryCount(measurement.id) > 0
                val requestedIds = linkedSetOf<String>()

                window.records.forEach { record ->
                    require(record.providerRecordId.isNotBlank()) { "Health provider record ID is required" }
                    require(record.providerRecordId.length <= 512) { "Health provider record ID is too long" }
                    require(record.value.isFinite()) { "Health measurement value must be finite" }
                    require(record.value >= 0.0) { "Health measurement value cannot be negative" }
                    require(record.timestamp >= window.startInclusive && record.timestamp < window.endExclusive) {
                        "Health provider record is outside the reviewed source window"
                    }
                    val sourceId = "${window.sourcePrefix}${record.providerRecordId}"
                    val entryId = "entry-$sourceId"
                    require(requestedIds.add(entryId)) { "Health provider returned a duplicate record ID" }
                    val unit = findUnit(record.unitId) ?: error("Unknown Health measurement unit")
                    require(unit.dimension == measurement.dimension) { "Health measurement unit is incompatible" }
                    val canonicalValue = unit.toCanonical(record.value)
                    require(canonicalValue.isFinite()) { "Converted Health measurement value must be finite" }
                    val existing = dao.getEntry(entryId)
                    if (existing != null) {
                        require(existing.measurementId == measurement.id) { "Health measurement identity belongs to another measurement" }
                        require(
                            existing.sourceType == MeasurementSourceType.HealthConnect.name && existing.sourceId == sourceId,
                        ) { "Health measurement identity has different provenance" }
                    }
                    val providerOffset = record.zoneOffsetSeconds?.let { offsetSeconds ->
                        runCatching { java.time.ZoneOffset.ofTotalSeconds(offsetSeconds) }
                            .getOrElse { error("Health provider record has an invalid zone offset") }
                    }
                    // Some Health providers omit an offset. Once we have
                    // assigned a stable provider record to a civil day, keep
                    // that provenance on an unchanged timestamp so travel or
                    // a later Settings time-zone change cannot move history.
                    val existingProvenance = existing?.takeIf {
                        providerOffset == null && it.timestampMillis == record.timestamp.toEpochMilli()
                    }
                    val existingOffset = existingProvenance?.let {
                        runCatching { java.time.ZoneOffset.ofTotalSeconds(it.offsetSeconds) }
                            .getOrElse { error("Stored Health record has an invalid zone offset") }
                    }
                    val effectiveOffset = providerOffset
                        ?: existingOffset
                        ?: window.zoneId.rules.getOffset(record.timestamp)
                    val timestampDate = record.timestamp.atOffset(effectiveOffset).toLocalDate()
                    require(record.localDate == null || record.localDate == timestampDate) {
                        "Health provider record date does not match its timestamp"
                    }
                    val effectiveDate = record.localDate ?: timestampDate
                    dao.upsertEntry(
                        MeasurementEntryEntity(
                            id = entryId,
                            measurementId = measurement.id,
                            canonicalValue = canonicalValue,
                            enteredValue = record.value,
                            enteredUnitId = record.unitId,
                            status = MeasurementEntryStatus.Recorded.name,
                            timestampMillis = record.timestamp.toEpochMilli(),
                            localEpochDay = effectiveDate.toEpochDay(),
                            zoneId = providerOffset?.id ?: existingProvenance?.zoneId ?: window.zoneId.id,
                            offsetSeconds = effectiveOffset.totalSeconds,
                            sourceType = MeasurementSourceType.HealthConnect.name,
                            sourceId = sourceId,
                            note = record.note.trim().take(1_000),
                            createdAtMillis = existing?.createdAtMillis ?: now,
                            updatedAtMillis = now,
                        ),
                    )
                }

                val existingWindowEntries = dao.getEntriesBySourceWindow(
                    MeasurementSourceType.HealthConnect.name,
                    window.sourcePrefix,
                    window.startInclusive.toEpochMilli(),
                    window.endExclusive.toEpochMilli(),
                )
                require(existingWindowEntries.all { it.measurementId == measurement.id }) {
                    "Health source window contains data assigned to another measurement"
                }
                existingWindowEntries.filterNot { it.id in requestedIds }.forEach { dao.deleteEntry(it.id) }

                if (!hadEntries && window.records.isNotEmpty() && !measurement.dimensionLocked) {
                    dao.upsertMeasurement(measurement.toEntity().copy(dimensionLocked = true, updatedAtMillis = now))
                }
                imported += requestedIds.size
            }
            imported
        }

    override suspend fun deleteHealthConnectEntries(): Int = database.withTransaction {
        dao.deleteEntriesBySourceType(MeasurementSourceType.HealthConnect.name)
    }

    private suspend fun findUnit(id: String): UnitDefinition? =
        BuiltInUnits.get(id) ?: dao.getUnit(id)?.toDomain()
}

internal fun UnitDefinitionEntity.toDomain() = UnitDefinition(
    id = id,
    name = name,
    symbol = symbol,
    dimension = UnitDimension.valueOf(dimension),
    toCanonicalFactor = toCanonicalFactor,
    toCanonicalOffset = toCanonicalOffset,
    custom = custom,
    archived = archived,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

private fun validateCustomUnitName(value: String): String = value.trim().also { normalized ->
    require(normalized.isNotBlank()) { "Unit name is required" }
    require(normalized.length <= 100) { "Unit name must be 100 characters or fewer" }
}

private fun validateCustomUnitSymbol(value: String): String = value.trim().also { normalized ->
    require(normalized.length <= 20) { "Unit symbol must be 20 characters or fewer" }
}

private val HEALTH_SOURCE_PREFIX = Regex("health:[a-z0-9-]+:")

private fun MeasurementDefinitionEntity.toDomain() = MeasurementDefinition(
    id = id,
    name = name,
    valueKind = MeasurementValueKind.valueOf(valueKind),
    dimension = UnitDimension.valueOf(dimension),
    defaultUnitId = defaultUnitId,
    precision = precision,
    dimensionLocked = dimensionLocked,
    archived = archived,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

private fun MeasurementDefinition.toEntity() = MeasurementDefinitionEntity(
    id = id,
    name = name,
    valueKind = valueKind.name,
    dimension = dimension.name,
    defaultUnitId = defaultUnitId,
    precision = precision,
    dimensionLocked = dimensionLocked,
    archived = archived,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

internal fun MeasurementEntryEntity.toDomain() = MeasurementEntry(
    id = id,
    measurementId = measurementId,
    canonicalValue = canonicalValue,
    enteredValue = enteredValue,
    enteredUnitId = enteredUnitId,
    status = MeasurementEntryStatus.valueOf(status),
    timestamp = Instant.ofEpochMilli(timestampMillis),
    localDate = LocalDate.ofEpochDay(localEpochDay),
    zoneId = zoneId,
    offsetSeconds = offsetSeconds,
    sourceType = MeasurementSourceType.valueOf(sourceType),
    sourceId = sourceId,
    note = note,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

private fun AreaEntity.toDomain() = Area(
    id, name, colorArgb, position, archived, createdAtMillis, updatedAtMillis,
)

private fun TagEntity.toDomain() = WhipTag(
    id, name, archived, createdAtMillis, updatedAtMillis,
)
