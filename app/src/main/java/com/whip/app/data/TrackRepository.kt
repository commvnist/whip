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
import com.whip.app.domain.TrackDefinitionBoundary
import com.whip.app.domain.TrackDefinitionConflictException
import com.whip.app.domain.TrackDefinitionConflictKind
import com.whip.app.domain.TrackDefinitionRemovalReview
import com.whip.app.domain.TrackDefinitionSaveReceipt
import com.whip.app.domain.TrackChoiceRemovalImpact
import com.whip.app.domain.TrackField
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldRemovalImpact
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
import java.security.MessageDigest
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
    suspend fun definitionBoundary(
        id: Long,
        expectedDraft: TrackDraft? = null,
    ): TrackDefinitionBoundary?

    suspend fun reviewDefinitionUpdate(
        id: Long,
        draft: TrackDraft,
        expectedBoundary: TrackDefinitionBoundary,
        choiceReplacementIds: Map<Long, Long> = emptyMap(),
    ): TrackDefinitionRemovalReview

    suspend fun update(
        id: Long,
        draft: TrackDraft,
        expectedBoundary: TrackDefinitionBoundary,
        reviewedRemoval: TrackDefinitionRemovalReview? = null,
    ): TrackDefinitionSaveReceipt
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

class RoomTrackRepository(
    private val database: WhipDatabase,
    private val clock: WhipClock,
    private val ids: WhipIdGenerator,
) : TrackRepository {
    private val dao = database.trackDao()
    private val linkDao = database.linkDao()
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

    override suspend fun definitionBoundary(
        id: Long,
        expectedDraft: TrackDraft?,
    ): TrackDefinitionBoundary? = database.withTransaction {
        val snapshot = loadDefinitionSnapshot(id) ?: return@withTransaction null
        if (expectedDraft != null) {
            val validExpected = expectedDraft.validated()
            if (!snapshot.matches(validExpected)) {
                throw TrackDefinitionConflictException(
                    TrackDefinitionConflictKind.DefinitionChanged,
                    "This Track changed while the editor was opening. Review the latest definition and try again.",
                )
            }
        }
        snapshot.boundary()
    }

    private suspend fun loadDefinitionSnapshot(id: Long): TrackDefinitionSnapshot? {
        val track = dao.getTrack(id) ?: return null
        val fields = dao.getFields(id)
        val options = fields.takeIf { it.isNotEmpty() }
            ?.let { dao.getOptionsForFields(it.map(TrackFieldEntity::id)) }
            .orEmpty()
        return TrackDefinitionSnapshot(track, fields, options)
    }

    private fun requireMatchingDefinitionBoundary(
        snapshot: TrackDefinitionSnapshot?,
        expected: TrackDefinitionBoundary,
    ): TrackDefinitionSnapshot {
        val current = snapshot ?: throw TrackDefinitionConflictException(
            TrackDefinitionConflictKind.TargetMissing,
            "This Track no longer exists. Your draft has not been discarded.",
        )
        if (
            current.track.id != expected.trackId ||
            current.track.uuid != expected.trackUuid ||
            current.track.createdAtMillis != expected.trackCreatedAtMillis
        ) {
            throw TrackDefinitionConflictException(
                TrackDefinitionConflictKind.IdentityChanged,
                "The Track identity changed while this editor was open. Review the latest definition and try again.",
            )
        }
        if (current.boundary().semanticRevisionToken != expected.semanticRevisionToken) {
            throw TrackDefinitionConflictException(
                TrackDefinitionConflictKind.DefinitionChanged,
                "This Track definition changed while this editor was open. Review the latest definition and try again.",
            )
        }
        return current
    }

    override suspend fun reviewDefinitionUpdate(
        id: Long,
        draft: TrackDraft,
        expectedBoundary: TrackDefinitionBoundary,
        choiceReplacementIds: Map<Long, Long>,
    ): TrackDefinitionRemovalReview = database.withTransaction {
        require(id == expectedBoundary.trackId) { "Track boundary does not belong to this Track" }
        val valid = draft.validated()
        val snapshot = requireMatchingDefinitionBoundary(loadDefinitionSnapshot(id), expectedBoundary)
        buildRemovalSnapshot(snapshot, valid, choiceReplacementIds).review
    }

    private suspend fun buildRemovalSnapshot(
        definition: TrackDefinitionSnapshot,
        draft: TrackDraft,
        choiceReplacementIds: Map<Long, Long>,
    ): TrackRemovalSnapshot {
        val retainedFields = definition.resolveRetainedFields(draft)
        val removedFields = definition.fields.filterNot { it.id in retainedFields }
        val removedFieldIds = removedFields.mapTo(linkedSetOf(), TrackFieldEntity::id)
        val retainedOptionIds = definition.resolveRetainedOptions(draft, retainedFields)
        val removedOptions = definition.options.filter { option ->
            option.fieldId in removedFieldIds || option.id !in retainedOptionIds
        }
        val removedOptionIds = removedOptions.mapTo(linkedSetOf(), TrackChoiceOptionEntity::id)
        val independentlyRemovedOptionIds = removedOptions
            .filterNot { it.fieldId in removedFieldIds }
            .mapTo(linkedSetOf(), TrackChoiceOptionEntity::id)

        require(choiceReplacementIds.keys.all { it in independentlyRemovedOptionIds }) {
            "A Choice replacement no longer matches a removed Choice. Review the latest definition."
        }
        val optionById = definition.options.associateBy(TrackChoiceOptionEntity::id)
        choiceReplacementIds.forEach { (removedId, replacementId) ->
            val removed = optionById[removedId] ?: throw TrackDefinitionConflictException(
                TrackDefinitionConflictKind.ReplacementUnavailable,
                "A removed Choice no longer exists. Review the latest definition.",
            )
            val replacement = optionById[replacementId] ?: throw TrackDefinitionConflictException(
                TrackDefinitionConflictKind.ReplacementUnavailable,
                "A replacement Choice no longer exists. Choose another replacement.",
            )
            if (replacement.fieldId != removed.fieldId || replacement.id !in retainedOptionIds) {
                throw TrackDefinitionConflictException(
                    TrackDefinitionConflictKind.ReplacementUnavailable,
                    "Choose a retained replacement from the same Field.",
                )
            }
        }

        val fieldValues = removedFields.flatMap { dao.getValuesForField(it.id) }
        val optionValues = independentlyRemovedOptionIds.takeIf { it.isNotEmpty() }
            ?.let { dao.getValuesForOptions(it.toList()) }
            .orEmpty()
        val affectedValues = (fieldValues + optionValues).distinctBy(TrackValueEntity::id).sortedBy(TrackValueEntity::id)

        val linkRules = linkDao.getRules()
        val linkConditions = linkDao.getAllRuleConditions()
        val linkChoices = linkConditions.map(LinkRuleConditionEntity::id).takeIf { it.isNotEmpty() }
            ?.let { linkDao.getLinkConditionChoices(it) }
            .orEmpty()
        val touchedLinkConditionIds = buildSet {
            linkConditions.filterTo(mutableListOf()) { it.fieldId in removedFieldIds }
                .mapTo(this, LinkRuleConditionEntity::id)
            linkChoices.filter { it.optionId in removedOptionIds }
                .mapTo(this, LinkConditionChoiceEntity::conditionId)
        }
        val affectedLinkConditions = linkConditions.filter { it.id in touchedLinkConditionIds }
        val affectedLinkChoices = linkChoices.filter {
            it.conditionId in touchedLinkConditionIds || it.optionId in removedOptionIds
        }
        val touchedLinkRuleIds = buildSet {
            linkRules.filter { it.sourceFieldId in removedFieldIds }.mapTo(this, LinkRuleEntity::id)
            affectedLinkConditions.mapTo(this, LinkRuleConditionEntity::linkRuleId)
        }
        val affectedLinkRules = linkRules.filter { it.id in touchedLinkRuleIds }

        val triggerRules = linkDao.getTriggerRules()
        val triggerConditions = linkDao.getAllTriggerConditions()
        val triggerChoices = triggerConditions.map(TriggerRuleConditionEntity::id).takeIf { it.isNotEmpty() }
            ?.let { linkDao.getTriggerConditionChoices(it) }
            .orEmpty()
        val triggerMappings = linkDao.getAllTriggerMappings()
        val touchedTriggerConditionIds = buildSet {
            triggerConditions.filter { it.fieldId in removedFieldIds }
                .mapTo(this, TriggerRuleConditionEntity::id)
            triggerChoices.filter { it.optionId in removedOptionIds }
                .mapTo(this, TriggerConditionChoiceEntity::conditionId)
        }
        val affectedTriggerConditions = triggerConditions.filter { it.id in touchedTriggerConditionIds }
        val affectedTriggerChoices = triggerChoices.filter {
            it.conditionId in touchedTriggerConditionIds || it.optionId in removedOptionIds
        }
        val affectedTriggerMappings = triggerMappings.filter {
            it.targetFieldId in removedFieldIds || it.constantChoiceOptionId in removedOptionIds
        }
        val touchedTriggerRuleIds = buildSet {
            affectedTriggerConditions.mapTo(this, TriggerRuleConditionEntity::triggerRuleId)
            affectedTriggerMappings.mapTo(this, TriggerFieldMappingEntity::triggerRuleId)
        }
        val affectedTriggerRules = triggerRules.filter { it.id in touchedTriggerRuleIds }

        val fieldById = definition.fields.associateBy(TrackFieldEntity::id)
        val fieldImpacts = removedFields.sortedBy(TrackFieldEntity::position).map { field ->
            TrackFieldRemovalImpact(
                fieldId = field.id,
                fieldUuid = field.uuid,
                fieldName = field.name,
                savedValueCount = affectedValues.count { it.fieldId == field.id },
                childChoiceCount = removedOptions.count { it.fieldId == field.id },
                legacyLinkSourceCount = affectedLinkRules.count { it.sourceFieldId == field.id },
                legacyLinkConditionCount = affectedLinkConditions.count { it.fieldId == field.id },
                legacyTriggerConditionCount = affectedTriggerConditions.count { it.fieldId == field.id },
                legacyTriggerMappingCount = affectedTriggerMappings.count { it.targetFieldId == field.id },
            )
        }
        val choiceImpacts = removedOptions.sortedWith(
            compareBy<TrackChoiceOptionEntity>({ fieldById[it.fieldId]?.position ?: Int.MAX_VALUE }, TrackChoiceOptionEntity::position),
        ).map { option ->
            val replacement = choiceReplacementIds[option.id]?.let(optionById::get)
            TrackChoiceRemovalImpact(
                optionId = option.id,
                optionUuid = option.uuid,
                fieldId = option.fieldId,
                fieldName = fieldById[option.fieldId]?.name.orEmpty(),
                optionLabel = option.label,
                savedValueCount = affectedValues.count { it.choiceOptionId == option.id },
                replacementOptionId = replacement?.id,
                replacementOptionLabel = replacement?.label,
                legacyLinkConditionCount = affectedLinkChoices.count { it.optionId == option.id },
                legacyTriggerConditionCount = affectedTriggerChoices.count { it.optionId == option.id },
                legacyTriggerMappingCount = affectedTriggerMappings.count { it.constantChoiceOptionId == option.id },
                removedWithField = option.fieldId in removedFieldIds,
            )
        }
        val boundaryToken = definition.boundary().semanticRevisionToken
        val removalToken = trackRemovalRevision(
            definitionRevisionToken = boundaryToken,
            removedFields = removedFields,
            removedOptions = removedOptions,
            choiceReplacementIds = choiceReplacementIds,
            affectedValues = affectedValues,
            affectedLinkRules = affectedLinkRules,
            affectedLinkConditions = affectedLinkConditions,
            affectedLinkChoices = affectedLinkChoices,
            affectedTriggerRules = affectedTriggerRules,
            affectedTriggerConditions = affectedTriggerConditions,
            affectedTriggerChoices = affectedTriggerChoices,
            affectedTriggerMappings = affectedTriggerMappings,
        )
        val review = TrackDefinitionRemovalReview(
            trackId = definition.track.id,
            definitionRevisionToken = boundaryToken,
            removalRevisionToken = removalToken,
            removedFields = fieldImpacts,
            removedChoices = choiceImpacts,
            choiceReplacementIds = choiceReplacementIds.toSortedMap(),
        )
        return TrackRemovalSnapshot(
            review = review,
            removedFieldIds = removedFieldIds,
            removedOptionIds = removedOptionIds,
            affectedValues = affectedValues,
            affectedLinkRules = affectedLinkRules,
            affectedLinkConditions = affectedLinkConditions,
            affectedLinkChoices = affectedLinkChoices,
            affectedTriggerRules = affectedTriggerRules,
            affectedTriggerConditions = affectedTriggerConditions,
            affectedTriggerChoices = affectedTriggerChoices,
            affectedTriggerMappings = affectedTriggerMappings,
        )
    }

    override suspend fun create(draft: TrackDraft): Long = database.withTransaction {
        val valid = draft.validated()
        validateFieldUnits(valid.fields, emptyList())
        val area = resolveAreaForDefinition(valid, existingAreaId = null)
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
        expectedBoundary: TrackDefinitionBoundary,
        reviewedRemoval: TrackDefinitionRemovalReview?,
    ): TrackDefinitionSaveReceipt = database.withTransaction {
        val valid = draft.validated()
        require(id == expectedBoundary.trackId) { "Track boundary does not belong to this Track" }
        val definition = requireMatchingDefinitionBoundary(loadDefinitionSnapshot(id), expectedBoundary)
        val currentRemoval = buildRemovalSnapshot(
            definition,
            valid,
            reviewedRemoval?.choiceReplacementIds.orEmpty(),
        )
        if (reviewedRemoval == null && currentRemoval.review.hasRemovals) {
            throw TrackDefinitionConflictException(
                TrackDefinitionConflictKind.RemovalImpactChanged,
                "Review the exact Field, Choice, history, and legacy automation impact before saving.",
            )
        }
        if (reviewedRemoval != null && reviewedRemoval != currentRemoval.review) {
            throw TrackDefinitionConflictException(
                TrackDefinitionConflictKind.RemovalImpactChanged,
                "The removal impact changed after it was reviewed. Review the updated impact before saving.",
            )
        }
        validateFieldUnits(valid.fields, definition.fields)
        validateRetainedFieldChanges(definition, valid)
        val existing = definition.track
        val area = resolveAreaForDefinition(valid, existing.areaId)
        val existingFields = definition.fields
        val existingOptions = definition.options
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
                removal = currentRemoval,
            )
        }
        if (fieldsChanged || searchMetadataChanged) rebuildSearchIndex(id)
        val replacedOptionIds = currentRemoval.review.choiceReplacementIds.keys
        val replacedValues = currentRemoval.affectedValues.count { value ->
            value.choiceOptionId?.let(replacedOptionIds::contains) == true
        }
        val deletedValues = currentRemoval.affectedValues.size - replacedValues
        TrackDefinitionSaveReceipt(
            trackId = id,
            schemaChanged = fieldsChanged,
            removedFieldCount = currentRemoval.review.removedFields.size,
            removedChoiceCount = currentRemoval.review.removedChoices.size,
            deletedValueCount = deletedValues,
            replacedValueCount = replacedValues,
            legacyLinkReferenceCount = currentRemoval.affectedLinkRules.size +
                currentRemoval.affectedLinkConditions.size + currentRemoval.affectedLinkChoices.size,
            legacyTriggerReferenceCount = currentRemoval.affectedTriggerRules.size +
                currentRemoval.affectedTriggerConditions.size + currentRemoval.affectedTriggerChoices.size +
                currentRemoval.affectedTriggerMappings.size,
        )
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
        removal: TrackRemovalSnapshot,
    ) {
        val now = clock.now().toEpochMilli()
        val existingFields = dao.getFields(trackId)
        val byId = existingFields.associateBy(TrackFieldEntity::id)
        val byUuid = existingFields.associateBy(TrackFieldEntity::uuid)
        val retainedFieldIds = mutableSetOf<Long>()
        drafts.forEachIndexed { position, draft ->
            val current = resolveExistingField(draft, byId, byUuid)
            val fieldId = if (current == null) {
                dao.insertField(draft.toEntity(trackId, position, ids.nextId(), now))
            } else {
                require(current.trackId == trackId) { "Field does not belong to this Track" }
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
            syncOptions(fieldId, draft.options, removal, now)
        }
        val fieldsToRemove = existingFields.filterNot { it.id in retainedFieldIds }
        check(fieldsToRemove.mapTo(linkedSetOf(), TrackFieldEntity::id) == removal.removedFieldIds) {
            "The Field removal impact changed before it could be committed"
        }
        fieldsToRemove.forEach { field ->
            check(dao.deleteField(field.id) == 1) { "Field no longer exists" }
            check(dao.getField(field.id) == null && dao.getValuesForField(field.id).isEmpty()) {
                "Field history was not removed exactly"
            }
            check(
                linkDao.countLinkRulesForSourceField(field.id) == 0 &&
                    linkDao.countLinkConditionsForField(field.id) == 0 &&
                    linkDao.countTriggerConditionsForField(field.id) == 0 &&
                    linkDao.countTriggerMappingsForField(field.id) == 0,
            ) { "Legacy Field references were not reconciled exactly" }
        }
    }

    private suspend fun syncOptions(
        fieldId: Long,
        drafts: List<TrackChoiceOptionDraft>,
        removal: TrackRemovalSnapshot,
        now: Long,
    ) {
        val existing = dao.getOptions(fieldId)
        val byId = existing.associateBy(TrackChoiceOptionEntity::id)
        val byUuid = existing.associateBy(TrackChoiceOptionEntity::uuid)
        val retainedIds = mutableSetOf<Long>()
        drafts.forEachIndexed { position, draft ->
            val current = resolveExistingOption(draft, byId, byUuid)
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
        val optionsToRemove = existing.filterNot { it.id in retainedIds }
        val expectedRemovedIds = existing
            .filter { it.id in removal.removedOptionIds }
            .mapTo(linkedSetOf(), TrackChoiceOptionEntity::id)
        check(optionsToRemove.mapTo(linkedSetOf(), TrackChoiceOptionEntity::id) == expectedRemovedIds) {
            "The Choice removal impact changed before it could be committed"
        }
        optionsToRemove.forEach { option ->
            val replacementId = removal.review.choiceReplacementIds[option.id]
            val expectedValueCount = removal.affectedValues.count { it.choiceOptionId == option.id }
            if (replacementId != null) {
                val replacement = dao.getOption(replacementId) ?: error("Replacement Choice no longer exists")
                require(replacement.fieldId == fieldId && replacement.id in retainedIds) {
                    "Choose a retained Choice from ${replacement.label}'s Field"
                }
                replaceChoiceOption(option.id, replacement.id, expectedValueCount, removal, now)
                check(dao.deleteOption(option.id) == 1) { "Choice option no longer exists" }
                return@forEach
            }
            check(dao.deleteValuesForOption(option.id) == expectedValueCount) {
                "Choice history changed before it could be deleted"
            }
            check(dao.deleteOption(option.id) == 1) { "Choice option no longer exists" }
            check(
                dao.countValuesForOption(option.id) == 0 &&
                    linkDao.countLinkConditionsForOption(option.id) == 0 &&
                    linkDao.countTriggerConditionsForOption(option.id) == 0 &&
                    linkDao.countTriggerMappingsForOption(option.id) == 0,
            ) { "Legacy Choice references were not reconciled exactly" }
        }
    }

    private suspend fun replaceChoiceOption(
        removedId: Long,
        replacementId: Long,
        expectedValueCount: Int,
        removal: TrackRemovalSnapshot,
        now: Long,
    ) {
        val db = database.openHelper.writableDatabase
        check(dao.replaceChoiceOptionValues(removedId, replacementId, now) == expectedValueCount) {
            "Choice history changed before it could be replaced"
        }
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
        check(
            dao.countValuesForOption(removedId) == 0 &&
                linkDao.countLinkConditionsForOption(removedId) == 0 &&
                linkDao.countTriggerConditionsForOption(removedId) == 0 &&
                linkDao.countTriggerMappingsForOption(removedId) == 0,
        ) { "Legacy Choice references were not replaced exactly" }
        val expectedLinkConditions = removal.affectedLinkChoices
            .filter { it.optionId == removedId }
            .mapTo(linkedSetOf(), LinkConditionChoiceEntity::conditionId)
        val currentLinkChoices = expectedLinkConditions.takeIf { it.isNotEmpty() }
            ?.let { linkDao.getLinkConditionChoices(it.toList()) }
            .orEmpty()
        check(expectedLinkConditions.all { conditionId ->
            currentLinkChoices.any { it.conditionId == conditionId && it.optionId == replacementId }
        }) { "A legacy Link Choice replacement was lost" }
        val expectedTriggerConditions = removal.affectedTriggerChoices
            .filter { it.optionId == removedId }
            .mapTo(linkedSetOf(), TriggerConditionChoiceEntity::conditionId)
        val currentTriggerChoices = expectedTriggerConditions.takeIf { it.isNotEmpty() }
            ?.let { linkDao.getTriggerConditionChoices(it.toList()) }
            .orEmpty()
        check(expectedTriggerConditions.all { conditionId ->
            currentTriggerChoices.any { it.conditionId == conditionId && it.optionId == replacementId }
        }) { "A legacy Trigger Choice replacement was lost" }
        val expectedMappingIds = removal.affectedTriggerMappings
            .filter { it.constantChoiceOptionId == removedId }
            .mapTo(linkedSetOf(), TriggerFieldMappingEntity::id)
        val currentMappings = linkDao.getAllTriggerMappings().filter { it.id in expectedMappingIds }
        check(
            currentMappings.mapTo(linkedSetOf(), TriggerFieldMappingEntity::id) == expectedMappingIds &&
                currentMappings.all { it.constantChoiceOptionId == replacementId },
        ) {
            "A legacy Trigger mapping replacement was lost"
        }
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

    private suspend fun validateFieldUnits(
        fields: List<TrackFieldDraft>,
        existingFields: List<TrackFieldEntity>,
    ) {
        val byId = existingFields.associateBy(TrackFieldEntity::id)
        val byUuid = existingFields.associateBy(TrackFieldEntity::uuid)
        fields.filter { it.type == TrackFieldType.Number }.forEach { field ->
            val unit = resolveUnit(requireNotNull(field.unitId)) ?: error("${field.name} uses an unknown unit")
            require(unit.dimension == field.dimension) { "${field.name}'s unit does not match its measurement type" }
            val current = resolveExistingField(field, byId, byUuid)
            require(!unit.archived || current?.unitId == unit.id) {
                "Restore ${unit.name} before selecting it for ${field.name}."
            }
        }
    }

    private suspend fun resolveAreaForDefinition(
        draft: TrackDraft,
        existingAreaId: String?,
    ): AreaSelection {
        val measurementDao = database.measurementDao()
        val selectedBeforeResolve = when {
            !draft.areaId.isNullOrBlank() -> measurementDao.getArea(requireNotNull(draft.areaId))
            draft.area.isNotBlank() -> measurementDao.getAreaByNameKey(areaNameKey(draft.area))
            else -> null
        }
        require(selectedBeforeResolve?.archived != true || selectedBeforeResolve.id == existingAreaId) {
            "Restore ${selectedBeforeResolve?.name ?: "this Area"} before moving this Track into it."
        }
        if (selectedBeforeResolve?.archived == true) {
            // Retaining an archived Area by its legacy display name must not call
            // AreaRepository.create(), which intentionally restores archived
            // Areas when users explicitly create/select them elsewhere.
            return AreaSelection(selectedBeforeResolve.id, selectedBeforeResolve.name)
        }
        val resolved = areaRepository.resolve(draft.areaId, draft.area)
        val selected = requireNotNull(resolved.id?.let { measurementDao.getArea(it) }) {
            "Selected Area no longer exists"
        }
        require(!selected.archived || selected.id == existingAreaId) {
            "Restore ${selected.name} before moving this Track into it."
        }
        return resolved
    }

    private suspend fun validateRetainedFieldChanges(
        definition: TrackDefinitionSnapshot,
        draft: TrackDraft,
    ) {
        val byId = definition.fields.associateBy(TrackFieldEntity::id)
        val byUuid = definition.fields.associateBy(TrackFieldEntity::uuid)
        draft.fields.forEach { fieldDraft ->
            val current = resolveExistingField(fieldDraft, byId, byUuid) ?: return@forEach
            val hasSavedValues = dao.countValuesForField(current.id) > 0
            val hasLegacyDefinitionReferences =
                linkDao.countLinkRulesForSourceField(current.id) > 0 ||
                    linkDao.countLinkConditionsForField(current.id) > 0 ||
                    linkDao.countTriggerConditionsForField(current.id) > 0 ||
                    linkDao.countTriggerMappingsForField(current.id) > 0
            if (current.type != fieldDraft.type.name) {
                require(!hasSavedValues && !hasLegacyDefinitionReferences) {
                    "${current.name} has saved values or legacy automation definitions. Delete it and add a new Field instead of changing its type."
                }
            }
            if (
                current.type == TrackFieldType.Number.name &&
                fieldDraft.type == TrackFieldType.Number &&
                current.dimension != fieldDraft.dimension?.name
            ) {
                require(!hasSavedValues && !hasLegacyDefinitionReferences) {
                    "${current.name}'s measurement type cannot change while it has saved values or legacy automation definitions."
                }
            }
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
        val invalidLegacyMapping = linkDao.getTriggerMappingsForField(fieldId)
            .mapNotNull(TriggerFieldMappingEntity::constantScale)
            .firstOrNull { value -> normalizeTrackScaleValue(value, minimum, maximum, draft.scaleStep) == null }
        require(invalidLegacyMapping == null) {
            "A legacy automation value does not fit the new Scale range and increment. Keep it selectable or replace the Field."
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

private data class TrackDefinitionSnapshot(
    val track: TrackEntity,
    val fields: List<TrackFieldEntity>,
    val options: List<TrackChoiceOptionEntity>,
) {
    fun boundary(): TrackDefinitionBoundary = TrackDefinitionBoundary(
        trackId = track.id,
        trackUuid = track.uuid,
        trackCreatedAtMillis = track.createdAtMillis,
        semanticRevisionToken = canonicalTrackRevision {
            value("track.id", track.id)
            value("track.uuid", track.uuid)
            value("track.name", track.name)
            value("track.description", track.description)
            value("track.icon", track.icon)
            value("track.areaId", track.areaId)
            value("track.tags", track.tagsCsv)
            fields.sortedBy(TrackFieldEntity::id).forEach { field ->
                value("field.id", field.id)
                value("field.uuid", field.uuid)
                value("field.trackId", field.trackId)
                value("field.name", field.name)
                value("field.type", field.type)
                value("field.position", field.position)
                value("field.required", field.required)
                value("field.primary", field.primaryField)
                value("field.showInList", field.showInList)
                value("field.dimension", field.dimension)
                value("field.unitId", field.unitId)
                value("field.precision", field.precision)
                value("field.scaleMin", field.scaleMin)
                value("field.scaleMax", field.scaleMax)
                value("field.scaleLowLabel", field.scaleLowLabel)
                value("field.scaleHighLabel", field.scaleHighLabel)
                value("field.scaleStep", field.scaleStep)
            }
            options.sortedBy(TrackChoiceOptionEntity::id).forEach { option ->
                value("option.id", option.id)
                value("option.uuid", option.uuid)
                value("option.fieldId", option.fieldId)
                value("option.label", option.label)
                value("option.position", option.position)
            }
        },
    )

    fun matches(draft: TrackDraft): Boolean =
        track.name == draft.name &&
            track.description == draft.description &&
            track.icon == draft.icon &&
            track.areaId == draft.areaId &&
            track.tagsCsv == draft.tags.joinToString(",") &&
            fields.matches(draft.fields, options)

    fun resolveRetainedFields(draft: TrackDraft): Set<Long> {
        val byId = fields.associateBy(TrackFieldEntity::id)
        val byUuid = fields.associateBy(TrackFieldEntity::uuid)
        return draft.fields.mapNotNullTo(linkedSetOf()) { field ->
            resolveExistingField(field, byId, byUuid)?.also { current ->
                require(current.trackId == track.id) { "Field does not belong to this Track" }
            }?.id
        }
    }

    fun resolveRetainedOptions(draft: TrackDraft, retainedFieldIds: Set<Long>): Set<Long> {
        val fieldById = fields.associateBy(TrackFieldEntity::id)
        val fieldByUuid = fields.associateBy(TrackFieldEntity::uuid)
        return buildSet {
            draft.fields.forEach { fieldDraft ->
                val currentField = resolveExistingField(fieldDraft, fieldById, fieldByUuid)
                if (currentField == null) {
                    require(fieldDraft.options.all { it.id == null && it.uuid == null }) {
                        "A new Field cannot reuse an existing Choice identity"
                    }
                    return@forEach
                }
                require(currentField.id in retainedFieldIds)
                val currentOptions = options.filter { it.fieldId == currentField.id }
                val optionById = currentOptions.associateBy(TrackChoiceOptionEntity::id)
                val optionByUuid = currentOptions.associateBy(TrackChoiceOptionEntity::uuid)
                fieldDraft.options.mapNotNullTo(this) { option ->
                    resolveExistingOption(option, optionById, optionByUuid)?.id
                }
            }
        }
    }
}

private data class TrackRemovalSnapshot(
    val review: TrackDefinitionRemovalReview,
    val removedFieldIds: Set<Long>,
    val removedOptionIds: Set<Long>,
    val affectedValues: List<TrackValueEntity>,
    val affectedLinkRules: List<LinkRuleEntity>,
    val affectedLinkConditions: List<LinkRuleConditionEntity>,
    val affectedLinkChoices: List<LinkConditionChoiceEntity>,
    val affectedTriggerRules: List<TriggerRuleEntity>,
    val affectedTriggerConditions: List<TriggerRuleConditionEntity>,
    val affectedTriggerChoices: List<TriggerConditionChoiceEntity>,
    val affectedTriggerMappings: List<TriggerFieldMappingEntity>,
)

private fun resolveExistingField(
    draft: TrackFieldDraft,
    byId: Map<Long, TrackFieldEntity>,
    byUuid: Map<String, TrackFieldEntity>,
): TrackFieldEntity? {
    val byDraftId = draft.id?.let { id ->
        byId[id] ?: throw TrackDefinitionConflictException(
            TrackDefinitionConflictKind.DefinitionChanged,
            "A Field changed or no longer exists. Review the latest Track definition.",
        )
    }
    val byDraftUuid = draft.uuid?.let { uuid ->
        byUuid[uuid] ?: throw TrackDefinitionConflictException(
            TrackDefinitionConflictKind.DefinitionChanged,
            "A Field identity changed. Review the latest Track definition.",
        )
    }
    require(byDraftId == null || byDraftUuid == null || byDraftId.id == byDraftUuid.id) {
        "Field identity does not match its stable ID"
    }
    return byDraftId ?: byDraftUuid
}

private fun resolveExistingOption(
    draft: TrackChoiceOptionDraft,
    byId: Map<Long, TrackChoiceOptionEntity>,
    byUuid: Map<String, TrackChoiceOptionEntity>,
): TrackChoiceOptionEntity? {
    val byDraftId = draft.id?.let { id ->
        byId[id] ?: throw TrackDefinitionConflictException(
            TrackDefinitionConflictKind.DefinitionChanged,
            "A Choice changed or no longer exists. Review the latest Track definition.",
        )
    }
    val byDraftUuid = draft.uuid?.let { uuid ->
        byUuid[uuid] ?: throw TrackDefinitionConflictException(
            TrackDefinitionConflictKind.DefinitionChanged,
            "A Choice identity changed. Review the latest Track definition.",
        )
    }
    require(byDraftId == null || byDraftUuid == null || byDraftId.id == byDraftUuid.id) {
        "Choice identity does not match its stable ID"
    }
    return byDraftId ?: byDraftUuid
}

private class CanonicalTrackRevisionBuilder {
    private val canonical = StringBuilder()

    fun value(label: String, value: Any?) {
        val encoded = when (value) {
            null -> "null"
            is Boolean -> if (value) "boolean:1" else "boolean:0"
            is Double -> "double:${java.lang.Double.doubleToRawLongBits(value)}"
            is Float -> "float:${java.lang.Float.floatToRawIntBits(value)}"
            is Number -> "integer:${value.toLong()}"
            else -> "string:$value"
        }
        canonical.append(label.length).append(':').append(label)
            .append(encoded.length).append(':').append(encoded).append('\n')
    }

    fun digest(): String = MessageDigest.getInstance("SHA-256")
        .digest(canonical.toString().toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
}

private inline fun canonicalTrackRevision(block: CanonicalTrackRevisionBuilder.() -> Unit): String =
    CanonicalTrackRevisionBuilder().apply(block).digest()

private fun trackRemovalRevision(
    definitionRevisionToken: String,
    removedFields: List<TrackFieldEntity>,
    removedOptions: List<TrackChoiceOptionEntity>,
    choiceReplacementIds: Map<Long, Long>,
    affectedValues: List<TrackValueEntity>,
    affectedLinkRules: List<LinkRuleEntity>,
    affectedLinkConditions: List<LinkRuleConditionEntity>,
    affectedLinkChoices: List<LinkConditionChoiceEntity>,
    affectedTriggerRules: List<TriggerRuleEntity>,
    affectedTriggerConditions: List<TriggerRuleConditionEntity>,
    affectedTriggerChoices: List<TriggerConditionChoiceEntity>,
    affectedTriggerMappings: List<TriggerFieldMappingEntity>,
): String = canonicalTrackRevision {
    value("definition", definitionRevisionToken)
    removedFields.sortedBy(TrackFieldEntity::id).forEach { value("remove.field", it.id) }
    removedOptions.sortedBy(TrackChoiceOptionEntity::id).forEach { value("remove.option", it.id) }
    choiceReplacementIds.toSortedMap().forEach { (removed, replacement) ->
        value("replace.removed", removed)
        value("replace.target", replacement)
    }
    affectedValues.sortedBy(TrackValueEntity::id).forEach(::row)
    affectedLinkRules.sortedBy(LinkRuleEntity::id).forEach(::row)
    affectedLinkConditions.sortedBy(LinkRuleConditionEntity::id).forEach(::row)
    affectedLinkChoices.sortedWith(compareBy(LinkConditionChoiceEntity::conditionId, LinkConditionChoiceEntity::optionId)).forEach(::row)
    affectedTriggerRules.sortedBy(TriggerRuleEntity::id).forEach(::row)
    affectedTriggerConditions.sortedBy(TriggerRuleConditionEntity::id).forEach(::row)
    affectedTriggerChoices.sortedWith(compareBy(TriggerConditionChoiceEntity::conditionId, TriggerConditionChoiceEntity::optionId)).forEach(::row)
    affectedTriggerMappings.sortedBy(TriggerFieldMappingEntity::id).forEach(::row)
}

private fun CanonicalTrackRevisionBuilder.row(row: TrackValueEntity) {
    value("row", "track_value")
    value("id", row.id); value("uuid", row.uuid); value("entryId", row.entryId); value("fieldId", row.fieldId)
    value("text", row.textValue); value("enteredNumber", row.enteredNumber); value("canonicalNumber", row.canonicalNumber)
    value("enteredUnitId", row.enteredUnitId); value("date", row.dateEpochDay); value("boolean", row.booleanValue)
    value("choice", row.choiceOptionId); value("scale", row.scaleValue)
    value("created", row.createdAtMillis); value("updated", row.updatedAtMillis)
}

private fun CanonicalTrackRevisionBuilder.row(row: LinkRuleEntity) {
    value("row", "link_rule")
    value("id", row.id); value("uuid", row.uuid); value("name", row.name); value("kind", row.kind)
    value("sourceType", row.sourceType); value("sourceEntityId", row.sourceEntityId); value("sourceMetricId", row.sourceMetricId)
    value("sourceItemId", row.sourceItemId); value("sourceMetric", row.sourceMetric); value("targetGoalId", row.targetGoalId)
    value("targetMilestoneId", row.targetMilestoneId); value("valueMode", row.valueMode); value("fixedValue", row.fixedValue)
    value("multiplier", row.multiplier); value("offset", row.offset); value("retroactive", row.retroactiveFromEpochDay)
    value("enabled", row.enabled); value("created", row.createdAtMillis); value("updated", row.updatedAtMillis)
    value("aggregation", row.trackAggregation); value("sourceFieldId", row.sourceFieldId); value("conditionMode", row.conditionMode)
}

private fun CanonicalTrackRevisionBuilder.row(row: LinkRuleConditionEntity) {
    value("row", "link_condition")
    value("id", row.id); value("ruleId", row.linkRuleId); value("fieldId", row.fieldId); value("entryDate", row.entryDate)
    value("operator", row.operator); value("position", row.position); value("text", row.textValue)
    value("number", row.numberValue); value("secondNumber", row.secondNumberValue)
    value("date", row.dateEpochDay); value("secondDate", row.secondDateEpochDay)
}

private fun CanonicalTrackRevisionBuilder.row(row: LinkConditionChoiceEntity) {
    value("row", "link_choice"); value("conditionId", row.conditionId); value("optionId", row.optionId)
}

private fun CanonicalTrackRevisionBuilder.row(row: TriggerRuleEntity) {
    value("row", "trigger_rule")
    value("id", row.id); value("uuid", row.uuid); value("name", row.name); value("sourceType", row.sourceType)
    value("sourceEntityId", row.sourceEntityId); value("sourceItemId", row.sourceItemId); value("outcome", row.outcome)
    value("targetType", row.targetType); value("targetEntityId", row.targetEntityId); value("delay", row.delayMinutes)
    value("quietStart", row.quietStartMinutes); value("quietEnd", row.quietEndMinutes); value("action", row.action)
    value("notifications", row.notificationEnabled); value("conditionMode", row.conditionMode); value("enabled", row.enabled)
    value("created", row.createdAtMillis); value("updated", row.updatedAtMillis)
}

private fun CanonicalTrackRevisionBuilder.row(row: TriggerRuleConditionEntity) {
    value("row", "trigger_condition")
    value("id", row.id); value("ruleId", row.triggerRuleId); value("fieldId", row.fieldId); value("entryDate", row.entryDate)
    value("operator", row.operator); value("position", row.position); value("text", row.textValue)
    value("number", row.numberValue); value("secondNumber", row.secondNumberValue)
    value("date", row.dateEpochDay); value("secondDate", row.secondDateEpochDay)
}

private fun CanonicalTrackRevisionBuilder.row(row: TriggerConditionChoiceEntity) {
    value("row", "trigger_choice"); value("conditionId", row.conditionId); value("optionId", row.optionId)
}

private fun CanonicalTrackRevisionBuilder.row(row: TriggerFieldMappingEntity) {
    value("row", "trigger_mapping")
    value("id", row.id); value("ruleId", row.triggerRuleId); value("fieldId", row.targetFieldId)
    value("sourceProperty", row.sourceProperty); value("text", row.constantText); value("number", row.constantNumber)
    value("unit", row.constantUnitId); value("date", row.constantDateEpochDay); value("boolean", row.constantBoolean)
    value("choice", row.constantChoiceOptionId); value("scale", row.constantScale)
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
