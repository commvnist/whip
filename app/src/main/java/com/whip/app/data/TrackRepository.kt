package com.whip.app.data

import androidx.room.withTransaction
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.DeletedTrackEntry
import com.whip.app.domain.Track
import com.whip.app.domain.TrackChoiceOption
import com.whip.app.domain.TrackChoiceOptionDraft
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackEntry
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackEntryPage
import com.whip.app.domain.TrackEntryProjection
import com.whip.app.domain.TrackField
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackFieldValue
import com.whip.app.domain.TrackProjection
import com.whip.app.domain.TrackValueDraft
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.formatTrackScaleValue
import com.whip.app.domain.normalizeTrackScaleValue
import com.whip.app.domain.validateTrackEntryDraft
import com.whip.app.domain.validated
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal const val TRACK_CSV_MAX_EXPORT_BYTES = 25 * 1024 * 1024

internal suspend fun requireTrackCsvExportWithinLimit(
    csv: String,
    maxBytes: Int = TRACK_CSV_MAX_EXPORT_BYTES,
) {
    var bytes = 0
    var index = 0
    while (index < csv.length) {
        if (index % 4_096 == 0) currentCoroutineContext().ensureActive()
        val char = csv[index]
        bytes += when {
            char.code <= 0x7f -> 1
            char.code <= 0x7ff -> 2
            char.isHighSurrogate() && index + 1 < csv.length && csv[index + 1].isLowSurrogate() -> {
                index++
                4
            }
            else -> 3
        }
        require(bytes <= maxBytes) {
            "This Track export is larger than 25 MB. Remove unneeded long-text values or older Entries, then try again."
        }
        index++
    }
}

interface TrackRepository {
    val tracks: Flow<List<Track>>
    val fields: Flow<List<TrackField>>
    val options: Flow<List<TrackChoiceOption>>
    val entries: Flow<List<TrackEntry>>
    val values: Flow<List<TrackFieldValue>>
    /** Complete read models shared by Track screens and exports. */
    val projections: Flow<List<TrackProjection>>

    suspend fun create(draft: TrackDraft): Long
    suspend fun update(
        id: Long,
        draft: TrackDraft,
        confirmedFieldValueDeletionIds: Set<Long> = emptySet(),
        confirmedOptionValueDeletionIds: Set<Long> = emptySet(),
        optionReplacementIds: Map<Long, Long> = emptyMap(),
    ): TrackUpdateImpact
    suspend fun duplicate(id: Long): Long
    suspend fun setPinned(id: Long, pinned: Boolean)
    suspend fun setArchived(id: Long, archived: Boolean)
    suspend fun reorder(ids: List<Long>)
    suspend fun addEntry(trackId: Long, draft: TrackEntryDraft): Long
    suspend fun importEntries(trackId: Long, drafts: List<TrackEntryDraft>): List<Long>
    suspend fun updateEntry(entryId: Long, draft: TrackEntryDraft)
    suspend fun deleteEntry(entryId: Long): DeletedTrackEntry?
    suspend fun restoreEntry(deleted: DeletedTrackEntry): Long
    suspend fun projection(trackId: Long): TrackProjection?
    suspend fun entryPage(trackId: Long, offset: Int, limit: Int): TrackEntryPage
    suspend fun searchEntryIds(trackId: Long, query: String): Set<Long>
    suspend fun exportCsv(trackId: Long): String
    suspend fun rebuildSearchIndex(trackId: Long? = null)
}

data class TrackUpdateImpact(
    val schemaChanged: Boolean,
)

class RoomTrackRepository(
    private val database: WhipDatabase,
    private val clock: WhipClock,
    private val ids: WhipIdGenerator,
) : TrackRepository {
    private val dao = database.trackDao()
    private val areaRepository = RoomAreaRepository(database, clock, ids)

    override val tracks: Flow<List<Track>> = dao.observeTracks().map { rows -> rows.map(TrackEntity::toDomain) }
    override val fields: Flow<List<TrackField>> = dao.observeFields().map { rows -> rows.map(TrackFieldEntity::toDomain) }
    override val options: Flow<List<TrackChoiceOption>> = dao.observeOptions().map { rows -> rows.map(TrackChoiceOptionEntity::toDomain) }
    override val entries: Flow<List<TrackEntry>> = dao.observeEntries().map { rows -> rows.map(TrackEntryEntity::toDomain) }
    override val values: Flow<List<TrackFieldValue>> = dao.observeValues().map { rows -> rows.map(TrackValueEntity::toDomain) }
    override val projections: Flow<List<TrackProjection>> = combine(
        tracks,
        fields,
        options,
        entries,
        values,
    ) { allTracks, allFields, allOptions, allEntries, allValues ->
        val fieldsByTrack = allFields.groupBy(TrackField::trackId)
        val fieldIdsByTrack = fieldsByTrack.mapValues { (_, rows) -> rows.mapTo(mutableSetOf(), TrackField::id) }
        val entriesByTrack = allEntries.groupBy(TrackEntry::trackId)
        val valuesByEntry = allValues.groupBy(TrackFieldValue::entryId)
        // Each Track is committed with at least one Field, but Room invalidates
        // the joined tables independently. Never publish the short-lived
        // half-projection between those emissions: every consumer assumes a
        // usable primary Field and a live UI could otherwise crash immediately
        // after creating or changing a Track.
        allTracks.mapNotNull { track ->
            val trackFields = fieldsByTrack[track.id].orEmpty()
            if (trackFields.isEmpty()) return@mapNotNull null
            TrackProjection(
                track = track,
                fields = trackFields.sortedBy(TrackField::position),
                options = allOptions.filter { it.fieldId in fieldIdsByTrack[track.id].orEmpty() }
                    .sortedBy(TrackChoiceOption::position),
                entries = entriesByTrack[track.id].orEmpty().map { entry ->
                    TrackEntryProjection(
                        entry = entry,
                        values = valuesByEntry[entry.id].orEmpty().associateBy(TrackFieldValue::fieldId),
                    )
                },
            )
        }
    }

    override suspend fun create(draft: TrackDraft): Long = database.withTransaction {
        val valid = draft.validated()
        validateFieldUnits(valid.fields)
        val area = areaRepository.resolve(valid.areaId, valid.area)
        val now = clock.now().toEpochMilli()
        val trackId = dao.insertTrack(
            TrackEntity(
                uuid = ids.nextId(),
                name = valid.name,
                description = valid.description,
                icon = valid.icon,
                areaId = requireNotNull(area.id),
                area = area.name,
                tagsCsv = valid.tags.joinToString(","),
                pinned = false,
                archived = false,
                position = dao.nextTrackPosition(),
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
        )
        insertFields(trackId, valid.fields, now)
        trackId
    }

    override suspend fun update(
        id: Long,
        draft: TrackDraft,
        confirmedFieldValueDeletionIds: Set<Long>,
        confirmedOptionValueDeletionIds: Set<Long>,
        optionReplacementIds: Map<Long, Long>,
    ) = database.withTransaction {
        val valid = draft.validated()
        validateFieldUnits(valid.fields)
        val existing = dao.getTrack(id) ?: error("Track no longer exists")
        val area = areaRepository.resolve(valid.areaId, valid.area)
        val existingFields = dao.getFields(id)
        val existingOptions = existingFields.takeIf { it.isNotEmpty() }
            ?.let { dao.getOptionsForFields(it.map(TrackFieldEntity::id)) }
            .orEmpty()
        val fieldsChanged = !existingFields.matches(valid.fields, existingOptions)
        val searchMetadataChanged = existing.name != valid.name ||
            existing.areaId != area.id ||
            existing.area != area.name ||
            existing.tagsCsv != valid.tags.joinToString(",")
        dao.updateTrack(
            existing.copy(
                name = valid.name,
                description = valid.description,
                icon = valid.icon,
                areaId = requireNotNull(area.id),
                area = area.name,
                tagsCsv = valid.tags.joinToString(","),
                updatedAtMillis = clock.now().toEpochMilli(),
            ),
        )
        if (fieldsChanged) {
            syncFields(
                trackId = id,
                drafts = valid.fields,
                confirmedFieldValueDeletionIds = confirmedFieldValueDeletionIds,
                confirmedOptionValueDeletionIds = confirmedOptionValueDeletionIds,
                optionReplacementIds = optionReplacementIds,
            )
        }
        if (fieldsChanged || searchMetadataChanged) rebuildSearchIndex(id)
        TrackUpdateImpact(schemaChanged = fieldsChanged)
    }

    override suspend fun duplicate(id: Long): Long {
        val source = projection(id) ?: error("Track no longer exists")
        return create(
            TrackDraft(
                name = "${source.track.name} Copy",
                description = source.track.description,
                icon = source.track.icon,
                areaId = source.track.areaId,
                area = source.track.area,
                tags = source.track.tags,
                fields = source.fields.map { field ->
                    TrackFieldDraft(
                        name = field.name,
                        type = field.type,
                        required = field.required,
                        primary = field.primary,
                        showInList = field.showInList,
                        dimension = field.dimension,
                        unitId = field.unitId,
                        precision = field.precision,
                        scaleMin = field.scaleMin,
                        scaleMax = field.scaleMax,
                        scaleLowLabel = field.scaleLowLabel,
                        scaleHighLabel = field.scaleHighLabel,
                        scaleStep = field.scaleStep,
                        options = source.optionsFor(field.id).map { TrackChoiceOptionDraft(it.label) },
                    )
                },
            ),
        )
    }

    override suspend fun setPinned(id: Long, pinned: Boolean) {
        val current = dao.getTrack(id) ?: error("Track no longer exists")
        dao.updateTrack(current.copy(pinned = pinned, updatedAtMillis = clock.now().toEpochMilli()))
    }

    override suspend fun setArchived(id: Long, archived: Boolean) {
        val current = dao.getTrack(id) ?: error("Track no longer exists")
        dao.updateTrack(current.copy(archived = archived, updatedAtMillis = clock.now().toEpochMilli()))
    }

    override suspend fun reorder(ids: List<Long>) = database.withTransaction {
        val requested = ids.distinct()
        val all = dao.getAllTracks()
        require(requested.all { id -> all.any { it.id == id } }) { "Track no longer exists" }
        val byId = all.associateBy(TrackEntity::id)
        val order = requested + all.filterNot { it.id in requested }.sortedBy(TrackEntity::position).map(TrackEntity::id)
        val now = clock.now().toEpochMilli()
        order.forEachIndexed { position, id ->
            val track = requireNotNull(byId[id])
            if (track.position != position) dao.updateTrack(track.copy(position = position, updatedAtMillis = now))
        }
    }

    override suspend fun addEntry(trackId: Long, draft: TrackEntryDraft): Long = database.withTransaction {
        val track = dao.getTrack(trackId) ?: error("Track no longer exists")
        require(!track.archived) { "Restore this Track before adding Entries" }
        val fields = dao.getFields(trackId).map(TrackFieldEntity::toDomain)
        val options = fields.takeIf { it.isNotEmpty() }
            ?.let { dao.getOptionsForFields(it.map(TrackField::id)) }
            .orEmpty()
            .map(TrackChoiceOptionEntity::toDomain)
        validateTrackEntryDraft(fields, options, draft)
        val now = clock.now().toEpochMilli()
        val entryId = dao.insertEntry(
            TrackEntryEntity(
                uuid = ids.nextId(),
                trackId = trackId,
                entryEpochDay = draft.entryDate.toEpochDay(),
                sourceOccurrenceId = draft.sourceOccurrenceId,
                sourceExplanation = draft.sourceExplanation.trim(),
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
        )
        upsertDraftValues(entryId, fields, options, draft.values, emptyMap(), now)
        rebuildSearchEntry(trackId, entryId)
        entryId
    }

    override suspend fun importEntries(trackId: Long, drafts: List<TrackEntryDraft>): List<Long> = database.withTransaction {
        require(drafts.isNotEmpty()) { "There are no valid CSV rows to import" }
        val track = dao.getTrack(trackId) ?: error("Track no longer exists")
        require(!track.archived) { "Restore this Track before importing Entries" }
        val fields = dao.getFields(trackId).map(TrackFieldEntity::toDomain)
        val options = fields.takeIf { it.isNotEmpty() }
            ?.let { dao.getOptionsForFields(it.map(TrackField::id)) }
            .orEmpty()
            .map(TrackChoiceOptionEntity::toDomain)
        // Validate the complete batch before the first insert. A bad row cannot
        // leave a partially imported Track even before Room rolls back.
        drafts.forEach { validateTrackEntryDraft(fields, options, it) }
        val trackDomain = track.toDomain()
        drafts.map { draft ->
            val now = clock.now().toEpochMilli()
            val entryId = dao.insertEntry(
                TrackEntryEntity(
                    uuid = ids.nextId(),
                    trackId = trackId,
                    entryEpochDay = draft.entryDate.toEpochDay(),
                    sourceOccurrenceId = draft.sourceOccurrenceId,
                    sourceExplanation = draft.sourceExplanation.trim(),
                    createdAtMillis = now,
                    updatedAtMillis = now,
                ),
            )
            upsertDraftValues(entryId, fields, options, draft.values, emptyMap(), now)
            val entry = requireNotNull(dao.getEntry(entryId)).toDomain()
            val values = dao.getValues(entryId).map(TrackValueEntity::toDomain)
            upsertSearchProjection(trackDomain, fields, options, entry, values)
            entryId
        }
    }

    override suspend fun updateEntry(entryId: Long, draft: TrackEntryDraft) = database.withTransaction {
        val existing = dao.getEntry(entryId) ?: error("Entry no longer exists")
        val track = dao.getTrack(existing.trackId) ?: error("Track no longer exists")
        require(!track.archived) { "Restore this Track before editing Entries" }
        val fields = dao.getFields(existing.trackId).map(TrackFieldEntity::toDomain)
        val options = fields.takeIf { it.isNotEmpty() }
            ?.let { dao.getOptionsForFields(it.map(TrackField::id)) }
            .orEmpty()
            .map(TrackChoiceOptionEntity::toDomain)
        validateTrackEntryDraft(fields, options, draft)
        val now = clock.now().toEpochMilli()
        dao.updateEntry(
            existing.copy(
                entryEpochDay = draft.entryDate.toEpochDay(),
                sourceExplanation = draft.sourceExplanation.trim().ifBlank { existing.sourceExplanation },
                updatedAtMillis = now,
            ),
        )
        val existingValues = dao.getValues(entryId).associateBy(TrackValueEntity::fieldId)
        upsertDraftValues(entryId, fields, options, draft.values, existingValues, now)
        val retainedFields = draft.values.mapNotNull { (fieldUuid, value) ->
            fields.firstOrNull { it.uuid == fieldUuid }?.takeUnless { value.isBlankFor(it.type) }?.id
        }
        if (retainedFields.isEmpty()) dao.deleteValues(entryId)
        else dao.deleteValuesOutsideFields(entryId, retainedFields)
        rebuildSearchEntry(existing.trackId, entryId)
        Unit
    }

    override suspend fun deleteEntry(entryId: Long): DeletedTrackEntry? = database.withTransaction {
        val entry = dao.getEntry(entryId) ?: return@withTransaction null
        val values = dao.getValues(entryId)
        dao.deleteSearch(entryId)
        check(dao.deleteEntry(entryId) == 1) { "Entry no longer exists" }
        DeletedTrackEntry(entry.toDomain(), values.map(TrackValueEntity::toDomain))
    }

    override suspend fun restoreEntry(deleted: DeletedTrackEntry): Long = database.withTransaction {
        require(dao.getEntryByUuid(deleted.entry.uuid) == null) { "Entry has already been restored" }
        requireNotNull(dao.getTrack(deleted.entry.trackId)) { "Track no longer exists" }
        val restoredId = dao.insertEntry(
            TrackEntryEntity(
                uuid = deleted.entry.uuid,
                trackId = deleted.entry.trackId,
                entryEpochDay = deleted.entry.entryDate.toEpochDay(),
                sourceOccurrenceId = deleted.entry.sourceOccurrenceId,
                sourceExplanation = deleted.entry.sourceExplanation,
                createdAtMillis = deleted.entry.createdAtMillis,
                updatedAtMillis = clock.now().toEpochMilli(),
            ),
        )
        deleted.values.forEach { value ->
            dao.upsertValue(
                value.toEntity(entryId = restoredId, id = 0, updatedAtMillis = clock.now().toEpochMilli()),
            )
        }
        rebuildSearchEntry(deleted.entry.trackId, restoredId)
        restoredId
    }

    override suspend fun projection(trackId: Long): TrackProjection? = database.withTransaction {
        val track = dao.getTrack(trackId)?.toDomain() ?: return@withTransaction null
        val fields = dao.getFields(trackId).map(TrackFieldEntity::toDomain)
        val options = fields.takeIf { it.isNotEmpty() }
            ?.let { dao.getOptionsForFields(it.map(TrackField::id)) }
            .orEmpty()
            .map(TrackChoiceOptionEntity::toDomain)
        val entries = dao.getEntries(trackId)
        val values = entries.takeIf { it.isNotEmpty() }
            ?.let { dao.getValuesForEntries(it.map(TrackEntryEntity::id)) }
            .orEmpty()
            .groupBy(TrackValueEntity::entryId)
        TrackProjection(
            track = track,
            fields = fields,
            options = options,
            entries = entries.map { entry ->
                TrackEntryProjection(
                    entry = entry.toDomain(),
                    values = values[entry.id].orEmpty().associate { it.fieldId to it.toDomain() },
                )
            },
        )
    }

    override suspend fun entryPage(trackId: Long, offset: Int, limit: Int): TrackEntryPage = database.withTransaction {
        require(offset >= 0) { "Entry page offset cannot be negative" }
        require(limit in 1..250) { "Entry page size must be between 1 and 250" }
        requireNotNull(dao.getTrack(trackId)) { "Track no longer exists" }
        val entries = dao.getEntryPage(trackId, offset, limit)
        val valuesByEntry = entries.takeIf { it.isNotEmpty() }
            ?.let { dao.getValuesForEntries(it.map(TrackEntryEntity::id)) }
            .orEmpty()
            .groupBy(TrackValueEntity::entryId)
        TrackEntryPage(
            entries = entries.map { entry ->
                TrackEntryProjection(
                    entry = entry.toDomain(),
                    values = valuesByEntry[entry.id].orEmpty().associate { it.fieldId to it.toDomain() },
                )
            },
            offset = offset,
            totalCount = dao.countEntries(trackId),
        )
    }

    override suspend fun searchEntryIds(trackId: Long, query: String): Set<Long> {
        val tokens = query.trim().split(Regex("\\s+")).map { token -> token.filter { it.isLetterOrDigit() } }.filter(String::isNotBlank)
        if (tokens.isEmpty()) return emptySet()
        // FTS4 does not apply a prefix wildcard placed after a quoted term
        // (`"term"*`). Tokens are already reduced to letters and digits, so
        // an unquoted prefix expression is safe and preserves type-ahead search.
        val ftsQuery = tokens.joinToString(" AND ") { token -> "$token*" }
        val indexedMatches = dao.searchEntryIds(trackId, ftsQuery).toSet()
        if (indexedMatches.isNotEmpty()) return indexedMatches
        // Some platform FTS4 builds reject otherwise valid prefix expressions
        // without raising an error. Keep search truthful with a bounded fallback
        // over this Track's rebuildable projection instead of returning no result.
        return tokens
            .map { token -> dao.searchEntryIdsContaining(trackId, token).toSet() }
            .reduce(Set<Long>::intersect)
    }

    override suspend fun exportCsv(trackId: Long): String {
        val projection = projection(trackId) ?: error("Track no longer exists")
        return buildTrackCsv(projection)
    }

    override suspend fun rebuildSearchIndex(trackId: Long?) = database.withTransaction {
        val ids = trackId?.let(::listOf) ?: dao.getAllTracks().map(TrackEntity::id)
        ids.forEach { id ->
            dao.deleteSearchForTrack(id)
            val track = dao.getTrack(id)?.toDomain() ?: return@forEach
            val fields = dao.getFields(id).map(TrackFieldEntity::toDomain)
            val options = fields.takeIf { it.isNotEmpty() }
                ?.let { dao.getOptionsForFields(it.map(TrackField::id)) }
                .orEmpty()
                .map(TrackChoiceOptionEntity::toDomain)
            val entries = dao.getEntries(id)
            val valuesByEntry = entries.takeIf { it.isNotEmpty() }
                ?.let { dao.getValuesForEntries(it.map(TrackEntryEntity::id)) }
                .orEmpty()
                .groupBy(TrackValueEntity::entryId)
            entries.forEach { entry ->
                upsertSearchProjection(
                    track,
                    fields,
                    options,
                    entry.toDomain(),
                    valuesByEntry[entry.id].orEmpty().map(TrackValueEntity::toDomain),
                )
            }
        }
    }

    private suspend fun rebuildSearchEntry(trackId: Long, entryId: Long) {
        val track = dao.getTrack(trackId)?.toDomain() ?: return
        val entry = dao.getEntry(entryId)?.takeIf { it.trackId == trackId }?.toDomain() ?: return
        val fields = dao.getFields(trackId).map(TrackFieldEntity::toDomain)
        val options = fields.takeIf { it.isNotEmpty() }
            ?.let { dao.getOptionsForFields(it.map(TrackField::id)) }
            .orEmpty()
            .map(TrackChoiceOptionEntity::toDomain)
        val values = dao.getValues(entryId).map(TrackValueEntity::toDomain)
        upsertSearchProjection(track, fields, options, entry, values)
    }

    private suspend fun upsertSearchProjection(
        track: Track,
        fields: List<TrackField>,
        options: List<TrackChoiceOption>,
        entry: TrackEntry,
        valueRows: List<TrackFieldValue>,
    ) {
        val values = valueRows.associateBy(TrackFieldValue::fieldId)
        val content = buildList {
            add(track.name)
            add(track.area)
            add(track.tags.joinToString(" "))
            fields.forEach { field ->
                add(field.name)
                val value = values[field.id]
                add(
                    when (field.type) {
                        TrackFieldType.ShortText, TrackFieldType.LongText -> value?.textValue.orEmpty()
                        TrackFieldType.Number -> listOfNotNull(value?.enteredNumber, value?.enteredUnitId, value?.canonicalNumber).joinToString(" ")
                        TrackFieldType.SingleChoice -> options.firstOrNull { it.id == value?.choiceOptionId }?.label.orEmpty()
                        TrackFieldType.Scale -> value?.scaleValue?.let(::formatTrackScaleValue).orEmpty()
                        TrackFieldType.Date -> value?.dateValue?.toString().orEmpty()
                        TrackFieldType.YesNo -> value?.booleanValue?.let { if (it) "Yes" else "No" }.orEmpty()
                    },
                )
            }
        }.filter(String::isNotBlank).joinToString(" ")
        dao.upsertSearch(TrackEntrySearchEntity(entry.id, track.id, content))
    }

    private suspend fun insertFields(trackId: Long, drafts: List<TrackFieldDraft>, now: Long) {
        drafts.forEachIndexed { position, draft ->
            val fieldId = dao.insertField(draft.toEntity(trackId, position, ids.nextId(), now))
            draft.options.forEachIndexed { optionPosition, option ->
                dao.insertOption(option.toEntity(fieldId, optionPosition, ids.nextId(), now))
            }
        }
    }

    private suspend fun syncFields(
        trackId: Long,
        drafts: List<TrackFieldDraft>,
        confirmedFieldValueDeletionIds: Set<Long>,
        confirmedOptionValueDeletionIds: Set<Long>,
        optionReplacementIds: Map<Long, Long> = emptyMap(),
    ) {
        val now = clock.now().toEpochMilli()
        val existingFields = dao.getFields(trackId)
        val byId = existingFields.associateBy(TrackFieldEntity::id)
        val byUuid = existingFields.associateBy(TrackFieldEntity::uuid)
        val retainedFieldIds = mutableSetOf<Long>()
        drafts.forEachIndexed { position, draft ->
            val current = draft.id?.let(byId::get) ?: draft.uuid?.let(byUuid::get)
            val fieldId = if (current == null) {
                dao.insertField(draft.toEntity(trackId, position, ids.nextId(), now))
            } else {
                require(current.trackId == trackId) { "Field does not belong to this Track" }
                if (current.type != draft.type.name && dao.countValuesForField(current.id) > 0) {
                    error("${current.name} has saved values. Delete it and add a new Field instead of changing its type.")
                }
                if (current.type == TrackFieldType.Scale.name && draft.type == TrackFieldType.Scale) {
                    validateExistingScaleConfiguration(current.id, draft)
                }
                dao.updateField(
                    draft.toEntity(trackId, position, current.uuid, current.createdAtMillis).copy(
                        id = current.id,
                        updatedAtMillis = now,
                    ),
                )
                current.id
            }
            retainedFieldIds += fieldId
            syncOptions(fieldId, draft.options, confirmedOptionValueDeletionIds, optionReplacementIds, now)
        }
        existingFields.filterNot { it.id in retainedFieldIds }.forEach { field ->
            val valueCount = dao.countValuesForField(field.id)
            require(valueCount == 0 || field.id in confirmedFieldValueDeletionIds) {
                "Deleting ${field.name} will permanently remove $valueCount saved values. Confirm this deletion first."
            }
            check(dao.deleteField(field.id) == 1) { "Field no longer exists" }
        }
    }

    private suspend fun syncOptions(
        fieldId: Long,
        drafts: List<TrackChoiceOptionDraft>,
        confirmedOptionValueDeletionIds: Set<Long>,
        optionReplacementIds: Map<Long, Long>,
        now: Long,
    ) {
        val existing = dao.getOptions(fieldId)
        val byId = existing.associateBy(TrackChoiceOptionEntity::id)
        val byUuid = existing.associateBy(TrackChoiceOptionEntity::uuid)
        val retainedIds = mutableSetOf<Long>()
        drafts.forEachIndexed { position, draft ->
            val current = draft.id?.let(byId::get) ?: draft.uuid?.let(byUuid::get)
            val optionId = if (current == null) {
                dao.insertOption(draft.toEntity(fieldId, position, ids.nextId(), now))
            } else {
                require(current.fieldId == fieldId) { "Choice option does not belong to this Field" }
                dao.updateOption(
                    current.copy(label = draft.label, position = position, updatedAtMillis = now),
                )
                current.id
            }
            retainedIds += optionId
        }
        existing.filterNot { it.id in retainedIds }.forEach { option ->
            val replacementId = optionReplacementIds[option.id]
            if (replacementId != null) {
                val replacement = dao.getOption(replacementId) ?: error("Replacement Choice no longer exists")
                require(replacement.fieldId == fieldId && replacement.id in retainedIds) {
                    "Choose a retained Choice from ${replacement.label}'s Field"
                }
                replaceChoiceOption(option.id, replacement.id, now)
                check(dao.deleteOption(option.id) == 1) { "Choice option no longer exists" }
                return@forEach
            }
            val valueCount = dao.countValuesForOption(option.id)
            require(valueCount == 0 || option.id in confirmedOptionValueDeletionIds) {
                "Deleting ${option.label} will permanently remove it from $valueCount Entries. Confirm this deletion first."
            }
            if (valueCount > 0) {
                database.openHelper.writableDatabase.execSQL(
                    "DELETE FROM track_values WHERE choiceOptionId = ?",
                    arrayOf(option.id),
                )
            }
            check(dao.deleteOption(option.id) == 1) { "Choice option no longer exists" }
        }
    }

    private fun replaceChoiceOption(removedId: Long, replacementId: Long, now: Long) {
        val db = database.openHelper.writableDatabase
        db.execSQL(
            "UPDATE track_values SET choiceOptionId = ?, updatedAtMillis = ? WHERE choiceOptionId = ?",
            arrayOf(replacementId, now, removedId),
        )
        db.execSQL(
            "INSERT OR IGNORE INTO link_condition_choices(conditionId, optionId) SELECT conditionId, ? FROM link_condition_choices WHERE optionId = ?",
            arrayOf(replacementId, removedId),
        )
        db.execSQL("DELETE FROM link_condition_choices WHERE optionId = ?", arrayOf(removedId))
        db.execSQL(
            "INSERT OR IGNORE INTO trigger_condition_choices(conditionId, optionId) SELECT conditionId, ? FROM trigger_condition_choices WHERE optionId = ?",
            arrayOf(replacementId, removedId),
        )
        db.execSQL("DELETE FROM trigger_condition_choices WHERE optionId = ?", arrayOf(removedId))
        db.execSQL(
            "UPDATE trigger_field_mappings SET constantChoiceOptionId = ? WHERE constantChoiceOptionId = ?",
            arrayOf(replacementId, removedId),
        )
    }

    private suspend fun upsertDraftValues(
        entryId: Long,
        fields: List<TrackField>,
        options: List<TrackChoiceOption>,
        values: Map<String, TrackValueDraft>,
        existingValues: Map<Long, TrackValueEntity>,
        now: Long,
    ) {
        fields.forEach { field ->
            val value = values[field.uuid] ?: return@forEach
            if (value.isBlankFor(field.type)) return@forEach
            val current = existingValues[field.id]
            dao.upsertValue(
                value.toEntity(
                    entryId = entryId,
                    field = field,
                    options = options.filter { it.fieldId == field.id },
                    current = current,
                    now = now,
                ),
            )
        }
    }

    private suspend fun validateFieldUnits(fields: List<TrackFieldDraft>) {
        fields.filter { it.type == TrackFieldType.Number }.forEach { field ->
            val unit = resolveUnit(requireNotNull(field.unitId)) ?: error("${field.name} uses an unknown unit")
            require(unit.dimension == field.dimension) { "${field.name}'s unit does not match its measurement type" }
        }
    }

    private suspend fun validateExistingScaleConfiguration(fieldId: Long, draft: TrackFieldDraft) {
        val minimum = requireNotNull(draft.scaleMin)
        val maximum = requireNotNull(draft.scaleMax)
        val invalidSavedValue = dao.getValuesForField(fieldId)
            .mapNotNull(TrackValueEntity::scaleValue)
            .firstOrNull { value -> normalizeTrackScaleValue(value, minimum, maximum, draft.scaleStep) == null }
        require(invalidSavedValue == null) {
            "An existing Scale value does not fit the new range and increment. Keep it selectable or edit that Entry first."
        }
    }

    private suspend fun resolveUnit(id: String): UnitDefinition? = BuiltInUnits.get(id)
        ?: database.measurementDao().getUnit(id)?.let { unit ->
            UnitDefinition(
                id = unit.id,
                name = unit.name,
                symbol = unit.symbol,
                dimension = UnitDimension.valueOf(unit.dimension),
                toCanonicalFactor = unit.toCanonicalFactor,
                toCanonicalOffset = unit.toCanonicalOffset,
                custom = unit.custom,
                archived = unit.archived,
                createdAtMillis = unit.createdAtMillis,
                updatedAtMillis = unit.updatedAtMillis,
            )
        }

    private suspend fun TrackValueDraft.toEntity(
        entryId: Long,
        field: TrackField,
        options: List<TrackChoiceOption>,
        current: TrackValueEntity?,
        now: Long,
    ): TrackValueEntity {
        val numberUnit = enteredUnitId?.let { resolveUnit(it) }
            ?: field.unitId?.let { resolveUnit(it) }
        if (field.type == TrackFieldType.Number) {
            require(numberUnit?.dimension == field.dimension) { "${field.name}'s entry unit is incompatible" }
        }
        return TrackValueEntity(
            id = current?.id ?: 0,
            uuid = current?.uuid ?: ids.nextId(),
            entryId = entryId,
            fieldId = field.id,
            textValue = textValue?.trim().takeIf { field.type in setOf(TrackFieldType.ShortText, TrackFieldType.LongText) },
            enteredNumber = enteredNumber.takeIf { field.type == TrackFieldType.Number },
            canonicalNumber = if (field.type == TrackFieldType.Number) {
                enteredNumber?.let { requireNotNull(numberUnit).toCanonical(it) }
            } else {
                null
            },
            enteredUnitId = numberUnit?.id.takeIf { field.type == TrackFieldType.Number },
            dateEpochDay = dateValue?.toEpochDay().takeIf { field.type == TrackFieldType.Date },
            booleanValue = booleanValue.takeIf { field.type == TrackFieldType.YesNo },
            choiceOptionId = choiceOptionUuid?.let { uuid -> options.first { it.uuid == uuid }.id }
                .takeIf { field.type == TrackFieldType.SingleChoice },
            scaleValue = scaleValue.takeIf { field.type == TrackFieldType.Scale },
            createdAtMillis = current?.createdAtMillis ?: now,
            updatedAtMillis = now,
        )
    }
}

private fun List<TrackFieldEntity>.matches(
    drafts: List<TrackFieldDraft>,
    options: List<TrackChoiceOptionEntity>,
): Boolean {
    val ordered = sortedBy(TrackFieldEntity::position)
    if (ordered.size != drafts.size) return false
    return ordered.zip(drafts).withIndex().all { (index, pair) ->
        val (stored, draft) = pair
        val identityMatches = when {
            draft.id != null -> draft.id == stored.id
            draft.uuid != null -> draft.uuid == stored.uuid
            else -> false
        }
        identityMatches &&
            stored.name == draft.name &&
            stored.type == draft.type.name &&
            stored.position == index &&
            stored.required == draft.required &&
            stored.primaryField == draft.primary &&
            stored.showInList == draft.showInList &&
            stored.dimension == draft.dimension?.name &&
            stored.unitId == draft.unitId &&
            stored.precision == draft.precision &&
            stored.scaleMin == draft.scaleMin &&
            stored.scaleMax == draft.scaleMax &&
            stored.scaleLowLabel == draft.scaleLowLabel &&
            stored.scaleHighLabel == draft.scaleHighLabel &&
            stored.scaleStep == draft.scaleStep &&
            options.filter { it.fieldId == stored.id }.sortedBy(TrackChoiceOptionEntity::position)
                .matches(draft.options)
    }
}

private fun List<TrackChoiceOptionEntity>.matches(drafts: List<TrackChoiceOptionDraft>): Boolean {
    if (size != drafts.size) return false
    return zip(drafts).withIndex().all { (index, pair) ->
        val (stored, draft) = pair
        val identityMatches = when {
            draft.id != null -> draft.id == stored.id
            draft.uuid != null -> draft.uuid == stored.uuid
            else -> false
        }
        identityMatches && stored.label == draft.label && stored.position == index
    }
}

private fun TrackFieldDraft.toEntity(
    trackId: Long,
    position: Int,
    stableUuid: String,
    createdAtMillis: Long,
) = TrackFieldEntity(
    uuid = uuid ?: stableUuid,
    trackId = trackId,
    name = name,
    type = type.name,
    position = position,
    required = required,
    primaryField = primary,
    showInList = showInList,
    dimension = dimension?.name,
    unitId = unitId,
    precision = precision,
    scaleMin = scaleMin,
    scaleMax = scaleMax,
    scaleLowLabel = scaleLowLabel,
    scaleHighLabel = scaleHighLabel,
    scaleStep = scaleStep,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = createdAtMillis,
)

private fun TrackChoiceOptionDraft.toEntity(
    fieldId: Long,
    position: Int,
    stableUuid: String,
    createdAtMillis: Long,
) = TrackChoiceOptionEntity(
    uuid = uuid ?: stableUuid,
    fieldId = fieldId,
    label = label,
    position = position,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = createdAtMillis,
)

internal fun TrackEntity.toDomain() = Track(
    id = id,
    uuid = uuid,
    name = name,
    description = description,
    icon = icon,
    areaId = areaId,
    area = area,
    tags = tagsCsv.split(',').map(String::trim).filter(String::isNotBlank),
    pinned = pinned,
    archived = archived,
    position = position,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

internal fun TrackFieldEntity.toDomain() = TrackField(
    id = id,
    uuid = uuid,
    trackId = trackId,
    name = name,
    type = TrackFieldType.valueOf(type),
    position = position,
    required = required,
    primary = primaryField,
    showInList = showInList,
    dimension = dimension?.let(UnitDimension::valueOf),
    unitId = unitId,
    precision = precision,
    scaleMin = scaleMin,
    scaleMax = scaleMax,
    scaleLowLabel = scaleLowLabel,
    scaleHighLabel = scaleHighLabel,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    scaleStep = scaleStep,
)

internal fun TrackChoiceOptionEntity.toDomain() = TrackChoiceOption(
    id = id,
    uuid = uuid,
    fieldId = fieldId,
    label = label,
    position = position,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

internal fun TrackEntryEntity.toDomain() = TrackEntry(
    id = id,
    uuid = uuid,
    trackId = trackId,
    entryDate = LocalDate.ofEpochDay(entryEpochDay),
    sourceOccurrenceId = sourceOccurrenceId,
    sourceExplanation = sourceExplanation,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

internal fun TrackValueEntity.toDomain() = TrackFieldValue(
    id = id,
    uuid = uuid,
    entryId = entryId,
    fieldId = fieldId,
    textValue = textValue,
    enteredNumber = enteredNumber,
    canonicalNumber = canonicalNumber,
    enteredUnitId = enteredUnitId,
    dateValue = dateEpochDay?.let(LocalDate::ofEpochDay),
    booleanValue = booleanValue,
    choiceOptionId = choiceOptionId,
    scaleValue = scaleValue,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

private fun TrackFieldValue.toEntity(entryId: Long, id: Long, updatedAtMillis: Long) = TrackValueEntity(
    id = id,
    uuid = uuid,
    entryId = entryId,
    fieldId = fieldId,
    textValue = textValue,
    enteredNumber = enteredNumber,
    canonicalNumber = canonicalNumber,
    enteredUnitId = enteredUnitId,
    dateEpochDay = dateValue?.toEpochDay(),
    booleanValue = booleanValue,
    choiceOptionId = choiceOptionId,
    scaleValue = scaleValue,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

internal suspend fun buildTrackCsv(
    projection: TrackProjection,
    maxBytes: Int = TRACK_CSV_MAX_EXPORT_BYTES,
    cancellationCheckpoint: suspend () -> Unit = { currentCoroutineContext().ensureActive() },
): String {
    require(maxBytes >= 0)
    val result = StringBuilder(minOf(maxBytes, 64 * 1024))
    var bytes = 0

    fun appendChecked(value: String, startIndex: Int, endIndex: Int, byteCount: Int) {
        require(bytes + byteCount <= maxBytes) {
            "This Track export is larger than 25 MB. Remove unneeded long-text values or older Entries, then try again."
        }
        result.append(value, startIndex, endIndex)
        bytes += byteCount
    }

    fun appendAscii(char: Char) {
        require(bytes < maxBytes) {
            "This Track export is larger than 25 MB. Remove unneeded long-text values or older Entries, then try again."
        }
        result.append(char)
        bytes++
    }

    suspend fun appendCsvCell(value: String) {
        cancellationCheckpoint()
        appendAscii('"')
        var index = 0
        while (index < value.length) {
            if (index % 4_096 == 0) cancellationCheckpoint()
            val char = value[index]
            if (char == '"') {
                appendAscii('"')
                appendAscii('"')
            } else {
                val (characterCount, byteCount) = when {
                    char.code <= 0x7f -> 1 to 1
                    char.code <= 0x7ff -> 1 to 2
                    char.isHighSurrogate() && index + 1 < value.length && value[index + 1].isLowSurrogate() -> 2 to 4
                    else -> 1 to 3
                }
                appendChecked(value, index, index + characterCount, byteCount)
                index += characterCount - 1
            }
            index++
        }
        appendAscii('"')
    }

    suspend fun appendRow(cells: List<String>) {
        cancellationCheckpoint()
        cells.forEachIndexed { index, value ->
            if (index > 0) appendAscii(',')
            appendCsvCell(value)
        }
        appendAscii('\n')
    }

    val columns = listOf("Entry UUID", "Entry Date") + projection.fields.flatMap { field ->
        if (field.type == TrackFieldType.Number) {
            listOf("${field.name} (Entered)", "${field.name} (Unit)", "${field.name} (Canonical)")
        } else {
            listOf(field.name)
        }
    }
    appendRow(columns)
    projection.entries.forEach { item ->
        val cells = buildList {
            add(item.entry.uuid)
            add(item.entry.entryDate.toString())
            projection.fields.forEach { field ->
                val value = item.value(field.id)
                if (field.type == TrackFieldType.Number) {
                    add(value?.enteredNumber?.toString().orEmpty())
                    add(value?.enteredUnitId.orEmpty())
                    add(value?.canonicalNumber?.toString().orEmpty())
                } else {
                    add(formatCsvValue(value, field, projection.optionsFor(field.id)))
                }
            }
        }
        appendRow(cells)
    }
    return result.toString()
}

private fun formatCsvValue(
    value: TrackFieldValue?,
    field: TrackField,
    options: List<TrackChoiceOption>,
): String = when (field.type) {
    TrackFieldType.ShortText, TrackFieldType.LongText -> value?.textValue.orEmpty()
    TrackFieldType.Number -> value?.enteredNumber?.toString().orEmpty() +
        value?.enteredUnitId?.let { " $it" }.orEmpty()
    TrackFieldType.SingleChoice -> options.firstOrNull { it.id == value?.choiceOptionId }?.label.orEmpty()
    TrackFieldType.Scale -> value?.scaleValue?.let(::formatTrackScaleValue).orEmpty()
    TrackFieldType.Date -> value?.dateValue?.toString().orEmpty()
    TrackFieldType.YesNo -> value?.booleanValue?.let { if (it) "Yes" else "No" }.orEmpty()
}
