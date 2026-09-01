package com.whip.app.data

import androidx.room.withTransaction
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.domain.Area
import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.MetricDefinition
import com.whip.app.domain.MetricEntry
import com.whip.app.domain.MetricEntryStatus
import com.whip.app.domain.MetricSourceType
import com.whip.app.domain.MetricValueKind
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.WhipTag
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.math.sign

interface MeasurementRepository {
    val customUnits: Flow<List<UnitDefinition>>
    val metrics: Flow<List<MetricDefinition>>
    val entries: Flow<List<MetricEntry>>
    val areas: Flow<List<Area>>
    val tags: Flow<List<WhipTag>>

    suspend fun createMetric(
        name: String,
        valueKind: MetricValueKind,
        dimension: UnitDimension,
        defaultUnitId: String,
        precision: Int = 1,
    ): String

    suspend fun ensureMetric(
        id: String,
        name: String,
        valueKind: MetricValueKind,
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

    suspend fun ensureArea(name: String): String
    suspend fun ensureTag(name: String): String
    suspend fun renameArea(id: String, name: String)
    suspend fun renameTag(id: String, name: String)
    suspend fun setAreaArchived(id: String, archived: Boolean)
    suspend fun setTagArchived(id: String, archived: Boolean)
    suspend fun moveArea(id: String, direction: Int)

    suspend fun record(
        metricId: String,
        value: Double?,
        unitId: String?,
        status: MetricEntryStatus = MetricEntryStatus.Recorded,
        timestamp: Instant? = null,
        localDate: LocalDate? = null,
        zoneId: ZoneId? = null,
        sourceType: MetricSourceType = MetricSourceType.Manual,
        sourceId: String? = null,
        note: String = "",
        existingEntryId: String? = null,
        createIfMissingForHealthReconciliation: Boolean = false,
    ): String

    suspend fun deleteEntry(entryId: String)
    suspend fun deleteSourceEntriesExcept(
        sourceType: MetricSourceType,
        sourcePrefix: String,
        retainedEntryIds: Set<String>,
    )
}

class RoomMeasurementRepository(
    private val database: WhipDatabase,
    private val clock: WhipClock,
    private val ids: WhipIdGenerator,
) : MeasurementRepository {
    private val dao = database.measurementDao()
    private val areaRepository = RoomAreaRepository(database, clock, ids)

    override val customUnits = dao.observeCustomUnits().map { list -> list.map { it.toDomain() } }
    override val metrics = dao.observeMetrics().map { list -> list.map { it.toDomain() } }
    override val entries = dao.observeEntries().map { list -> list.map { it.toDomain() } }
    override val areas = areaRepository.areas
    override val tags = dao.observeTags().map { list -> list.map { it.toDomain() } }

    override suspend fun createMetric(
        name: String,
        valueKind: MetricValueKind,
        dimension: UnitDimension,
        defaultUnitId: String,
        precision: Int,
    ): String = database.withTransaction {
        require(name.isNotBlank()) { "Metric name is required" }
        val unit = findUnit(defaultUnitId) ?: error("Unknown unit")
        require(unit.dimension == dimension) { "The selected unit has an incompatible dimension" }
        val now = clock.now().toEpochMilli()
        val id = ids.nextId()
        dao.upsertMetric(
            MetricDefinitionEntity(
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

    override suspend fun ensureMetric(
        id: String,
        name: String,
        valueKind: MetricValueKind,
        dimension: UnitDimension,
        defaultUnitId: String,
        precision: Int,
    ): String = database.withTransaction {
        require(id.isNotBlank()) { "Metric ID is required" }
        val unit = findUnit(defaultUnitId) ?: error("Unknown unit")
        require(unit.dimension == dimension) { "The selected unit has an incompatible dimension" }
        val existing = dao.getMetric(id)
        if (existing != null) {
            require(existing.dimension == dimension.name) { "The metric already uses another dimension" }
            return@withTransaction id
        }
        val now = clock.now().toEpochMilli()
        dao.upsertMetric(
            MetricDefinitionEntity(
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
    ): String {
        require(name.isNotBlank()) { "Unit name is required" }
        require(toCanonicalFactor.isFinite() && toCanonicalFactor > 0.0) { "Conversion factor must be a finite positive number" }
        val now = clock.now().toEpochMilli()
        val id = ids.nextId()
        dao.upsertUnit(
            UnitDefinitionEntity(
                id = id,
                name = name.trim(),
                symbol = symbol.trim(),
                dimension = dimension.name,
                toCanonicalFactor = toCanonicalFactor,
                toCanonicalOffset = 0.0,
                custom = true,
                archived = false,
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
        )
        return id
    }

    override suspend fun ensureArea(name: String): String = areaRepository.create(name)

    override suspend fun ensureTag(name: String): String = database.withTransaction {
        val normalized = name.trim()
        require(normalized.isNotBlank()) { "Tag name is required" }
        val existing = dao.observeTags().first().firstOrNull { it.name.equals(normalized, true) }
        if (existing != null) return@withTransaction existing.id
        val now = clock.now().toEpochMilli()
        val id = ids.nextId()
        dao.upsertTag(TagEntity(id, normalized, false, now, now))
        id
    }

    override suspend fun renameArea(id: String, name: String) = areaRepository.rename(id, name)

    override suspend fun renameTag(id: String, name: String) = database.withTransaction {
        val existing = requireNotNull(dao.getTag(id)) { "Tag no longer exists" }
        val normalized = name.trim()
        require(normalized.isNotBlank()) { "Tag name is required" }
        val target = dao.observeTags().first().firstOrNull { it.id != id && it.name.equals(normalized, true) }
        val resolvedName = target?.name ?: normalized
        replaceTagName(existing.name, resolvedName)
        if (target == null) dao.upsertTag(existing.copy(name = normalized, updatedAtMillis = clock.now().toEpochMilli()))
        else dao.deleteTag(id)
        Unit
    }

    override suspend fun setAreaArchived(id: String, archived: Boolean) = areaRepository.setArchived(id, archived)

    override suspend fun setTagArchived(id: String, archived: Boolean) {
        val existing = requireNotNull(dao.getTag(id)) { "Tag no longer exists" }
        dao.upsertTag(existing.copy(archived = archived, updatedAtMillis = clock.now().toEpochMilli()))
    }

    override suspend fun moveArea(id: String, direction: Int) = areaRepository.move(id, direction)

    private fun replaceTagName(old: String, new: String) {
        val db = database.openHelper.writableDatabase
        listOf("tasks", "habits", "goals").forEach { table ->
            db.query("SELECT id, tagsCsv FROM $table").use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow("id")
                val tagsIndex = cursor.getColumnIndexOrThrow("tagsCsv")
                while (cursor.moveToNext()) {
                    val tags = cursor.getString(tagsIndex).split(',').map(String::trim).filter(String::isNotBlank)
                    if (tags.none { it.equals(old, true) }) continue
                    val replaced = tags.map { if (it.equals(old, true)) new else it }
                        .distinctBy(String::lowercase).joinToString(",")
                    db.execSQL("UPDATE $table SET tagsCsv = ? WHERE id = ?", arrayOf<Any>(replaced, cursor.getLong(idIndex)))
                }
            }
        }
    }

    override suspend fun ensureCustomUnit(
        id: String,
        name: String,
        symbol: String,
        dimension: UnitDimension,
        toCanonicalFactor: Double,
    ): String {
        require(id.isNotBlank()) { "Unit ID is required" }
        val existing = findUnit(id)
        if (existing != null) {
            require(existing.dimension == dimension) { "The unit already belongs to ${existing.dimension}" }
            return existing.id
        }
        require(toCanonicalFactor.isFinite() && toCanonicalFactor > 0.0) { "Conversion factor must be a finite positive number" }
        val now = clock.now().toEpochMilli()
        dao.upsertUnit(
            UnitDefinitionEntity(
                id = id.trim(), name = name.trim().ifBlank { id.trim() }, symbol = symbol.trim(),
                dimension = dimension.name, toCanonicalFactor = toCanonicalFactor,
                toCanonicalOffset = 0.0, custom = true, archived = false, createdAtMillis = now, updatedAtMillis = now,
            ),
        )
        return id.trim()
    }

    override suspend fun renameCustomUnit(id: String, name: String, symbol: String) {
        require(name.isNotBlank()) { "Unit name is required" }
        val existing = requireNotNull(dao.getUnit(id)) { "Custom unit no longer exists" }
        require(existing.custom) { "Built-in units cannot be edited" }
        dao.upsertUnit(existing.copy(name = name.trim(), symbol = symbol.trim(), updatedAtMillis = clock.now().toEpochMilli()))
    }

    override suspend fun setCustomUnitArchived(id: String, archived: Boolean) {
        val existing = requireNotNull(dao.getUnit(id)) { "Custom unit no longer exists" }
        require(existing.custom) { "Built-in units cannot be archived" }
        dao.upsertUnit(existing.copy(archived = archived, updatedAtMillis = clock.now().toEpochMilli()))
    }

    override suspend fun createCustomUnitVersion(
        sourceId: String,
        name: String,
        symbol: String,
        toCanonicalFactor: Double,
    ): String = database.withTransaction {
        val existing = requireNotNull(dao.getUnit(sourceId)) { "Custom unit no longer exists" }
        require(existing.custom) { "Built-in units cannot be versioned" }
        require(name.isNotBlank()) { "Unit name is required" }
        require(toCanonicalFactor.isFinite() && toCanonicalFactor > 0.0) { "Conversion factor must be positive" }
        val now = clock.now().toEpochMilli()
        dao.upsertUnit(existing.copy(archived = true, updatedAtMillis = now))
        val id = ids.nextId()
        dao.upsertUnit(
            existing.copy(
                id = id,
                name = name.trim(),
                symbol = symbol.trim(),
                toCanonicalFactor = toCanonicalFactor,
                archived = false,
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
        )
        id
    }

    override suspend fun record(
        metricId: String,
        value: Double?,
        unitId: String?,
        status: MetricEntryStatus,
        timestamp: Instant?,
        localDate: LocalDate?,
        zoneId: ZoneId?,
        sourceType: MetricSourceType,
        sourceId: String?,
        note: String,
        existingEntryId: String?,
        createIfMissingForHealthReconciliation: Boolean,
    ): String = database.withTransaction {
        require(!createIfMissingForHealthReconciliation || existingEntryId != null) {
            "Reconciliation requires a stable measurement ID"
        }
        val metric = dao.getMetric(metricId)?.toDomain() ?: error("Metric no longer exists")
        val unit = unitId?.let { findUnit(it) }
        require(value == null || value.isFinite()) { "Measurement value must be a finite number" }
        if (status == MetricEntryStatus.Recorded) {
            requireNotNull(value) { "A value is required" }
            requireNotNull(unit) { "A unit is required" }
            require(unit.dimension == metric.dimension) { "Incompatible unit" }
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
                require(sourceType == MetricSourceType.HealthConnect && !sourceId.isNullOrBlank()) {
                    "Only identified Health Connect records can be reconciled"
                }
                require(existingId == "entry-$sourceId") {
                    "Health reconciliation requires the source's stable measurement ID"
                }
            } else {
                require(stored.metricId == metricId) { "Measurement belongs to another metric" }
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
        val isFirstEntry = existing == null && dao.entryCount(metricId) == 0
        dao.upsertEntry(
            MetricEntryEntity(
                id = entryId,
                metricId = metricId,
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
        if (isFirstEntry && !metric.dimensionLocked) {
            dao.upsertMetric(
                metric.toEntity().copy(dimensionLocked = true, updatedAtMillis = now),
            )
        }
        entryId
    }

    override suspend fun deleteEntry(entryId: String) = dao.deleteEntry(entryId)

    override suspend fun deleteSourceEntriesExcept(
        sourceType: MetricSourceType,
        sourcePrefix: String,
        retainedEntryIds: Set<String>,
    ) = database.withTransaction {
        dao.getEntriesBySourcePrefix(sourceType.name, sourcePrefix)
            .filterNot { it.id in retainedEntryIds }
            .forEach { dao.deleteEntry(it.id) }
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

private fun MetricDefinitionEntity.toDomain() = MetricDefinition(
    id = id,
    name = name,
    valueKind = MetricValueKind.valueOf(valueKind),
    dimension = UnitDimension.valueOf(dimension),
    defaultUnitId = defaultUnitId,
    precision = precision,
    dimensionLocked = dimensionLocked,
    archived = archived,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

private fun MetricDefinition.toEntity() = MetricDefinitionEntity(
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

internal fun MetricEntryEntity.toDomain() = MetricEntry(
    id = id,
    metricId = metricId,
    canonicalValue = canonicalValue,
    enteredValue = enteredValue,
    enteredUnitId = enteredUnitId,
    status = MetricEntryStatus.valueOf(status),
    timestamp = Instant.ofEpochMilli(timestampMillis),
    localDate = LocalDate.ofEpochDay(localEpochDay),
    zoneId = zoneId,
    offsetSeconds = offsetSeconds,
    sourceType = MetricSourceType.valueOf(sourceType),
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
