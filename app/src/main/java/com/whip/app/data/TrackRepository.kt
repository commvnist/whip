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
import com.whip.app.domain.TrackEntryBoundary
import com.whip.app.domain.TrackEntryConflictException
import com.whip.app.domain.TrackEntryConflictKind
import com.whip.app.domain.TrackEntryChoiceContract
import com.whip.app.domain.TrackEntryCreatePreparation
import com.whip.app.domain.TrackEntryCreateRequest
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackEntryEditSnapshot
import com.whip.app.domain.TrackEntryFormBoundary
import com.whip.app.domain.TrackEntryFormSnapshot
import com.whip.app.domain.TrackEntryFieldContract
import com.whip.app.domain.TrackEntryFulfillmentSnapshot
import com.whip.app.domain.TrackEntryMutationKind
import com.whip.app.domain.TrackEntryMutationReceipt
import com.whip.app.domain.TrackEntryUnitContract
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
    suspend fun prepareEntryCreate(trackId: Long): TrackEntryCreatePreparation?
    suspend fun prepareEntryEdit(entryId: Long): TrackEntryEditSnapshot?
    suspend fun addEntry(request: TrackEntryCreateRequest, draft: TrackEntryDraft): TrackEntryMutationReceipt
    suspend fun importEntries(trackId: Long, drafts: List<TrackEntryDraft>): List<Long>
    suspend fun updateEntry(expectedBoundary: TrackEntryBoundary, draft: TrackEntryDraft): TrackEntryMutationReceipt
    suspend fun deleteEntry(expectedBoundary: TrackEntryBoundary): TrackEntryMutationReceipt
    suspend fun restoreEntry(deleted: DeletedTrackEntry): TrackEntryMutationReceipt
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

    private suspend fun loadEntryFormSnapshot(trackId: Long): TrackEntryFormRows? {
        val track = dao.getTrack(trackId) ?: return null
        val fields = dao.getFields(trackId)
        val options = fields.takeIf { it.isNotEmpty() }
            ?.let { dao.getOptionsForFields(it.map(TrackFieldEntity::id)) }
            .orEmpty()
        val dimensions = fields.mapNotNull { field ->
            field.dimension?.let(UnitDimension::valueOf)
        }.toSet()
        val units = (BuiltInUnits.all + database.measurementDao().getAllUnits().map(UnitDefinitionEntity::toDomain))
            .distinctBy(UnitDefinition::id)
            .filter { it.dimension in dimensions }
        return TrackEntryFormRows(track, fields, options, units)
    }

    private suspend fun loadEntrySnapshot(entryId: Long): TrackEntrySnapshot? {
        val entry = dao.getEntry(entryId) ?: return null
        val form = loadEntryFormSnapshot(entry.trackId) ?: return null
        return TrackEntrySnapshot(form, entry, dao.getValues(entryId))
    }

    private fun requireMatchingEntryFormBoundary(
        snapshot: TrackEntryFormRows?,
        expected: TrackEntryFormBoundary,
    ): TrackEntryFormRows {
        val current = snapshot ?: throw TrackEntryConflictException(
            TrackEntryConflictKind.ParentMissing,
            "This Track no longer exists. Your Entry has not been saved.",
        )
        val boundary = current.boundary()
        if (
            boundary.trackId != expected.trackId ||
            boundary.trackUuid != expected.trackUuid ||
            boundary.trackCreatedAtMillis != expected.trackCreatedAtMillis
        ) {
            throw TrackEntryConflictException(
                TrackEntryConflictKind.IdentityChanged,
                "The Track identity changed while this Entry form was open.",
            )
        }
        if (!boundary.sameContractAs(expected)) {
            throw TrackEntryConflictException(
                TrackEntryConflictKind.FormChanged,
                "This Entry form changed while it was open. Review the latest Fields and try again.",
            )
        }
        return current
    }

    private fun requireWritableEntryForm(form: TrackEntryFormRows) {
        if (form.track.archived) {
            throw TrackEntryConflictException(
                TrackEntryConflictKind.FormChanged,
                "Restore this Track before adding or editing Entries.",
            )
        }
    }

    private suspend fun requireEntryIdentity(
        expected: TrackEntryBoundary,
        missingKind: TrackEntryConflictKind = TrackEntryConflictKind.TargetMissing,
    ): TrackEntrySnapshot {
        val row = dao.getEntry(expected.entryId)
        if (row == null) {
            dao.getEntryByUuid(expected.entryUuid)?.let {
                throw TrackEntryConflictException(
                    TrackEntryConflictKind.IdentityChanged,
                    "The Entry identity changed while this request was open.",
                )
            }
            throw TrackEntryConflictException(
                missingKind,
                if (missingKind == TrackEntryConflictKind.OutcomeUnknown) {
                    "This Entry is no longer present, so this deletion cannot be safely attributed to the current request."
                } else {
                    "This Entry no longer exists. Your draft has not been discarded."
                },
            )
        }
        val current = loadEntrySnapshot(row.id) ?: throw TrackEntryConflictException(
            TrackEntryConflictKind.ParentMissing,
            "The Track for this Entry no longer exists.",
        )
        if (
            row.id != expected.entryId || row.uuid != expected.entryUuid ||
            row.createdAtMillis != expected.entryCreatedAtMillis ||
            current.form.track.id != expected.formBoundary.trackId ||
            current.form.track.uuid != expected.formBoundary.trackUuid ||
            current.form.track.createdAtMillis != expected.formBoundary.trackCreatedAtMillis
        ) {
            throw TrackEntryConflictException(
                TrackEntryConflictKind.IdentityChanged,
                "The Entry or its Track identity changed while this request was open.",
            )
        }
        return current
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

    override suspend fun prepareEntryCreate(trackId: Long): TrackEntryCreatePreparation? = database.withTransaction {
        val form = loadEntryFormSnapshot(trackId) ?: return@withTransaction null
        TrackEntryCreatePreparation(
            request = TrackEntryCreateRequest(
                entryUuid = ids.nextId(),
                openingFormBoundary = form.boundary(),
            ),
            form = form.toDomainSnapshot(),
        )
    }

    override suspend fun prepareEntryEdit(entryId: Long): TrackEntryEditSnapshot? = database.withTransaction {
        loadEntrySnapshot(entryId)?.toEditSnapshot()
    }

    override suspend fun addEntry(
        request: TrackEntryCreateRequest,
        draft: TrackEntryDraft,
    ): TrackEntryMutationReceipt = database.withTransaction {
        requireGenericEntryProvenance(draft)
        require(request.entryUuid.isNotBlank()) { "Entry identity is required" }

        // Stable identity is the create idempotency key. Compare against the
        // opening contract before consulting the current form so a committed
        // retry still resolves after, for example, a new required Field appears.
        dao.getEntryByUuid(request.entryUuid)?.let { existing ->
            val existingSnapshot = loadEntrySnapshot(existing.id) ?: throw TrackEntryConflictException(
                TrackEntryConflictKind.IdentityCollision,
                "An Entry with this identity already exists but could not be verified.",
            )
            val intended = normalizeEntryDraft(request.openingFormBoundary, draft, verifyLiveUnits = false)
            if (
                existing.trackId == request.openingFormBoundary.trackId &&
                existingSnapshot.form.track.uuid == request.openingFormBoundary.trackUuid &&
                existingSnapshot.form.track.createdAtMillis == request.openingFormBoundary.trackCreatedAtMillis &&
                existingSnapshot.matches(intended, sourceOccurrenceId = null, sourceExplanation = "")
            ) {
                return@withTransaction existingSnapshot.receipt(
                    kind = TrackEntryMutationKind.Create,
                    changed = false,
                    alreadyApplied = true,
                )
            }
            throw TrackEntryConflictException(
                TrackEntryConflictKind.IdentityCollision,
                "An Entry with this stable identity already exists with different content.",
            )
        }

        val form = requireMatchingEntryFormBoundary(
            loadEntryFormSnapshot(request.openingFormBoundary.trackId),
            request.openingFormBoundary,
        )
        requireWritableEntryForm(form)
        val normalized = normalizeEntryDraft(request.openingFormBoundary, draft, verifyLiveUnits = true)
        insertEntryLocked(
            form = form,
            entryUuid = request.entryUuid,
            normalized = normalized,
            sourceOccurrenceId = null,
            sourceExplanation = "",
        ).receipt(kind = TrackEntryMutationKind.Create, changed = true, alreadyApplied = false)
    }

    /**
     * Narrow trusted caller for LinkRepository's enclosing prompt transaction.
     * Generic Entry APIs intentionally cannot author these provenance columns.
     */
    internal suspend fun addPromptEntry(trackId: Long, draft: TrackEntryDraft): Long = database.withTransaction {
        val occurrenceId = draft.sourceOccurrenceId ?: throw TrackEntryConflictException(
            TrackEntryConflictKind.ProvenanceChanged,
            "A prompt Entry requires its source occurrence.",
        )
        val occurrence = linkDao.getTriggerOccurrence(occurrenceId) ?: throw TrackEntryConflictException(
            TrackEntryConflictKind.ProvenanceChanged,
            "The source prompt no longer exists.",
        )
        val rule = linkDao.getTriggerRule(occurrence.triggerRuleId) ?: throw TrackEntryConflictException(
            TrackEntryConflictKind.ProvenanceChanged,
            "The source prompt definition no longer exists.",
        )
        if (
            occurrence.fulfilledEntryId != null || rule.targetEntityId != trackId ||
            rule.action != "PromptTrackEntry" || rule.targetType != "Track"
        ) {
            throw TrackEntryConflictException(
                TrackEntryConflictKind.ProvenanceChanged,
                "This prompt can no longer create an Entry for this Track.",
            )
        }
        val form = loadEntryFormSnapshot(trackId) ?: throw TrackEntryConflictException(
            TrackEntryConflictKind.ParentMissing,
            "The prompt's target Track no longer exists.",
        )
        requireWritableEntryForm(form)
        val normalized = normalizeEntryDraft(form.boundary(), draft, verifyLiveUnits = true)
        insertEntryLocked(
            form = form,
            entryUuid = ids.nextId(),
            normalized = normalized,
            sourceOccurrenceId = occurrence.id,
            sourceExplanation = draft.sourceExplanation,
        ).entry.id
    }

    override suspend fun importEntries(trackId: Long, drafts: List<TrackEntryDraft>): List<Long> = database.withTransaction {
        require(drafts.isNotEmpty()) { "There are no valid CSV rows to import" }
        val form = loadEntryFormSnapshot(trackId) ?: throw TrackEntryConflictException(
            TrackEntryConflictKind.ParentMissing,
            "Track no longer exists",
        )
        requireWritableEntryForm(form)
        drafts.forEach(::requireGenericEntryProvenance)
        // Normalize the complete batch before the first insert. A bad row
        // cannot leave a partial import, even before Room rolls back.
        val normalized = drafts.map { normalizeEntryDraft(form.boundary(), it, verifyLiveUnits = true) }
        normalized.map { entry ->
            insertEntryLocked(form, ids.nextId(), entry, null, "").entry.id
        }
    }

    override suspend fun updateEntry(
        expectedBoundary: TrackEntryBoundary,
        draft: TrackEntryDraft,
    ): TrackEntryMutationReceipt = database.withTransaction {
        requireGenericEntryProvenance(draft)
        val current = requireEntryIdentity(expectedBoundary)
        val intendedFromOpeningForm = normalizeEntryDraft(
            expectedBoundary.formBoundary,
            draft,
            verifyLiveUnits = false,
        )
        if (current.matches(intendedFromOpeningForm, current.entry.sourceOccurrenceId, current.entry.sourceExplanation)) {
            return@withTransaction current.receipt(
                kind = TrackEntryMutationKind.Update,
                changed = false,
                alreadyApplied = true,
            )
        }
        val currentFormBoundary = current.form.boundary()
        if (!currentFormBoundary.sameContractAs(expectedBoundary.formBoundary)) {
            throw TrackEntryConflictException(
                TrackEntryConflictKind.FormChanged,
                "This Entry form changed while the editor was open. Review the latest Fields and try again.",
            )
        }
        requireWritableEntryForm(current.form)
        val normalized = normalizeEntryDraft(expectedBoundary.formBoundary, draft, verifyLiveUnits = true)
        if (current.boundary().semanticRevisionToken != expectedBoundary.semanticRevisionToken) {
            throw TrackEntryConflictException(
                TrackEntryConflictKind.EntryChanged,
                "This Entry changed while the editor was open. Review the latest values and try again.",
            )
        }

        val now = clock.now().toEpochMilli()
        check(
            dao.updateEntry(
                current.entry.copy(
                    entryEpochDay = normalized.entryDate.toEpochDay(),
                    // Automation provenance is immutable outside the trusted
                    // prompt transaction.
                    sourceOccurrenceId = current.entry.sourceOccurrenceId,
                    sourceExplanation = current.entry.sourceExplanation,
                    updatedAtMillis = now,
                ),
            ) == 1,
        ) { "Entry no longer exists" }
        upsertNormalizedValues(current.entry.id, normalized.values, current.values.associateBy(TrackValueEntity::fieldId), now)
        val retainedFieldIds = normalized.values.keys.toList()
        if (retainedFieldIds.isEmpty()) dao.deleteValues(current.entry.id)
        else dao.deleteValuesOutsideFields(current.entry.id, retainedFieldIds)
        rebuildSearchEntry(current.entry.trackId, current.entry.id)
        requireNotNull(loadEntrySnapshot(current.entry.id)).receipt(
            kind = TrackEntryMutationKind.Update,
            changed = true,
            alreadyApplied = false,
        )
    }

    override suspend fun deleteEntry(expectedBoundary: TrackEntryBoundary): TrackEntryMutationReceipt = database.withTransaction {
        val current = requireEntryIdentity(expectedBoundary, missingKind = TrackEntryConflictKind.OutcomeUnknown)
        if (!current.form.boundary().sameContractAs(expectedBoundary.formBoundary)) {
            throw TrackEntryConflictException(
                TrackEntryConflictKind.FormChanged,
                "This Entry form changed before deletion. Review the latest Entry before deleting it.",
            )
        }
        if (current.boundary().semanticRevisionToken != expectedBoundary.semanticRevisionToken) {
            throw TrackEntryConflictException(
                TrackEntryConflictKind.EntryChanged,
                "This Entry changed before deletion. Review the latest values before deleting it.",
            )
        }
        val fulfilled = linkDao.getTriggerOccurrencesForFulfilledEntry(current.entry.id)
        val sourceOccurrence = current.entry.sourceOccurrenceId?.let { sourceOccurrenceId ->
            linkDao.getTriggerOccurrence(sourceOccurrenceId) ?: throw TrackEntryConflictException(
                TrackEntryConflictKind.ProvenanceChanged,
                "This Entry's source occurrence is missing, so an exact Undo cannot be guaranteed.",
            )
        }
        val deleted = DeletedTrackEntry(
            entry = current.entry.toDomain(),
            values = current.values.map(TrackValueEntity::toDomain),
            openingFormBoundary = current.form.boundary(),
            sourceOccurrence = sourceOccurrence?.toTrackEntrySnapshot(),
            fulfilledOccurrences = fulfilled.map(TriggerOccurrenceEntity::toTrackEntrySnapshot),
        )
        dao.deleteSearch(current.entry.id)
        check(dao.deleteEntry(current.entry.id) == 1) { "Entry no longer exists" }
        fulfilled.forEach { before ->
            val after = requireNotNull(linkDao.getTriggerOccurrence(before.id)) {
                "Trigger occurrence changed during Entry deletion"
            }
            check(after == before.copy(fulfilledEntryId = null)) {
                "Trigger occurrence changed during Entry deletion"
            }
        }
        TrackEntryMutationReceipt(
            kind = TrackEntryMutationKind.Delete,
            trackId = current.form.track.id,
            trackUuid = current.form.track.uuid,
            entryId = current.entry.id,
            entryUuid = current.entry.uuid,
            changed = true,
            alreadyApplied = false,
            affectedValueCount = current.values.size,
            postBoundary = null,
            deletedEntry = deleted,
        )
    }

    override suspend fun restoreEntry(deleted: DeletedTrackEntry): TrackEntryMutationReceipt = database.withTransaction {
        dao.getEntryByUuid(deleted.entry.uuid)?.let { existing ->
            val current = loadEntrySnapshot(existing.id) ?: throw TrackEntryConflictException(
                TrackEntryConflictKind.IdentityCollision,
                "The restored Entry identity could not be verified.",
            )
            if (
                current.form.track.id != deleted.openingFormBoundary.trackId ||
                current.form.track.uuid != deleted.openingFormBoundary.trackUuid ||
                current.form.track.createdAtMillis != deleted.openingFormBoundary.trackCreatedAtMillis
            ) {
                throw TrackEntryConflictException(
                    TrackEntryConflictKind.IdentityCollision,
                    "An Entry with this stable identity exists under a different Track identity.",
                )
            }
            if (
                current.matchesDeleted(deleted) &&
                restoredOccurrencesMatch(deleted.fulfilledOccurrences, existing.id) &&
                sourceOccurrenceMatches(deleted, existing.id)
            ) {
                return@withTransaction current.receipt(
                    kind = TrackEntryMutationKind.Restore,
                    changed = false,
                    alreadyApplied = true,
                )
            }
            throw TrackEntryConflictException(
                TrackEntryConflictKind.IdentityCollision,
                "An Entry with this stable identity already exists with different history.",
            )
        }
        val form = loadEntryFormSnapshot(deleted.entry.trackId) ?: throw TrackEntryConflictException(
            TrackEntryConflictKind.ParentMissing,
            "The Track for this deleted Entry no longer exists.",
        )
        validateRestoreCompatibility(deleted, form)
        validateOccurrencesBeforeRestore(deleted)
        val restoredId = dao.insertEntry(
            TrackEntryEntity(
                uuid = deleted.entry.uuid,
                trackId = deleted.entry.trackId,
                entryEpochDay = deleted.entry.entryDate.toEpochDay(),
                sourceOccurrenceId = deleted.entry.sourceOccurrenceId,
                sourceExplanation = deleted.entry.sourceExplanation,
                createdAtMillis = deleted.entry.createdAtMillis,
                updatedAtMillis = deleted.entry.updatedAtMillis,
            ),
        )
        deleted.values.forEach { value ->
            dao.upsertValue(
                value.toEntity(entryId = restoredId, id = 0, updatedAtMillis = value.updatedAtMillis),
            )
        }
        deleted.fulfilledOccurrences.forEach { occurrence ->
            check(linkDao.restoreTriggerOccurrenceFulfillment(occurrence.id, restoredId) == 1) {
                "Trigger occurrence changed before Entry restoration"
            }
        }
        rebuildSearchEntry(deleted.entry.trackId, restoredId)
        val restored = requireNotNull(loadEntrySnapshot(restoredId))
        check(
            restored.matchesDeleted(deleted) &&
                restoredOccurrencesMatch(deleted.fulfilledOccurrences, restoredId) &&
                sourceOccurrenceMatches(deleted, restoredId),
        ) {
            "Entry restoration did not preserve its exact history"
        }
        restored.receipt(kind = TrackEntryMutationKind.Restore, changed = true, alreadyApplied = false)
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

    private fun requireGenericEntryProvenance(draft: TrackEntryDraft) {
        if (draft.sourceOccurrenceId != null || draft.sourceExplanation.isNotBlank()) {
            throw TrackEntryConflictException(
                TrackEntryConflictKind.ProvenanceChanged,
                "Automation provenance can only be authored by the trusted prompt workflow.",
            )
        }
    }

    private suspend fun normalizeEntryDraft(
        boundary: TrackEntryFormBoundary,
        draft: TrackEntryDraft,
        verifyLiveUnits: Boolean,
    ): NormalizedTrackEntryDraft {
        val fields = boundary.fieldContracts.map(TrackEntryFieldContract::toDomain)
        val options = boundary.choiceContracts.map(TrackEntryChoiceContract::toDomain)
        validateTrackEntryDraft(fields, options, draft)
        val fieldByUuid = fields.associateBy(TrackField::uuid)
        val expectedUnitById = boundary.unitContracts.associateBy(TrackEntryUnitContract::id)
        val normalized = linkedMapOf<Long, NormalizedTrackValue>()
        draft.values.forEach { (fieldUuid, value) ->
            val field = requireNotNull(fieldByUuid[fieldUuid]) { "Entry contains a Field that no longer exists" }
            val selectedUnit = if (field.type == TrackFieldType.Number) {
                val unitId = value.enteredUnitId ?: field.unitId
                val expected = unitId?.let(expectedUnitById::get) ?: throw TrackEntryConflictException(
                    TrackEntryConflictKind.FormChanged,
                    "${field.name}'s unit was not available when this Entry form opened.",
                )
                require(expected.dimension == field.dimension) { "${field.name}'s entry unit is incompatible" }
                if (verifyLiveUnits) {
                    val live = resolveUnit(expected.id)?.toEntryContract()
                    if (live != expected) {
                        throw TrackEntryConflictException(
                            TrackEntryConflictKind.FormChanged,
                            "${field.name}'s selected unit changed while this Entry form was open.",
                        )
                    }
                }
                expected
            } else {
                null
            }
            if (value.isBlankFor(field.type)) return@forEach
            normalized[field.id] = when (field.type) {
                TrackFieldType.ShortText, TrackFieldType.LongText -> NormalizedTrackValue(
                    fieldId = field.id,
                    textValue = value.textValue?.trim(),
                )
                TrackFieldType.Number -> NormalizedTrackValue(
                    fieldId = field.id,
                    enteredNumber = value.enteredNumber,
                    canonicalNumber = value.enteredNumber?.let { entered ->
                        val unit = requireNotNull(selectedUnit)
                        ((entered + unit.toCanonicalOffset) * unit.toCanonicalFactor).also { canonical ->
                            require(canonical.isFinite()) { "${field.name}'s converted value is too large" }
                        }
                    },
                    enteredUnitId = requireNotNull(selectedUnit).id,
                )
                TrackFieldType.SingleChoice -> NormalizedTrackValue(
                    fieldId = field.id,
                    choiceOptionId = options.first { option ->
                        option.fieldId == field.id && option.uuid == value.choiceOptionUuid
                    }.id,
                )
                TrackFieldType.Scale -> NormalizedTrackValue(
                    fieldId = field.id,
                    scaleValue = normalizeTrackScaleValue(
                        requireNotNull(value.scaleValue),
                        requireNotNull(field.scaleMin),
                        requireNotNull(field.scaleMax),
                        field.scaleStep,
                    ),
                )
                TrackFieldType.Date -> NormalizedTrackValue(
                    fieldId = field.id,
                    dateEpochDay = value.dateValue?.toEpochDay(),
                )
                TrackFieldType.YesNo -> NormalizedTrackValue(
                    fieldId = field.id,
                    booleanValue = value.booleanValue,
                )
            }
        }
        return NormalizedTrackEntryDraft(draft.entryDate, normalized)
    }

    private suspend fun insertEntryLocked(
        form: TrackEntryFormRows,
        entryUuid: String,
        normalized: NormalizedTrackEntryDraft,
        sourceOccurrenceId: Long?,
        sourceExplanation: String,
    ): TrackEntrySnapshot {
        val now = clock.now().toEpochMilli()
        val entryId = dao.insertEntry(
            TrackEntryEntity(
                uuid = entryUuid,
                trackId = form.track.id,
                entryEpochDay = normalized.entryDate.toEpochDay(),
                sourceOccurrenceId = sourceOccurrenceId,
                sourceExplanation = sourceExplanation.trim(),
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
        )
        upsertNormalizedValues(entryId, normalized.values, emptyMap(), now)
        rebuildSearchEntry(form.track.id, entryId)
        return requireNotNull(loadEntrySnapshot(entryId))
    }

    private suspend fun upsertNormalizedValues(
        entryId: Long,
        values: Map<Long, NormalizedTrackValue>,
        existingValues: Map<Long, TrackValueEntity>,
        now: Long,
    ) {
        values.forEach { (fieldId, value) ->
            val current = existingValues[fieldId]
            dao.upsertValue(
                TrackValueEntity(
                    id = current?.id ?: 0,
                    uuid = current?.uuid ?: ids.nextId(),
                    entryId = entryId,
                    fieldId = fieldId,
                    textValue = value.textValue,
                    enteredNumber = value.enteredNumber,
                    canonicalNumber = value.canonicalNumber,
                    enteredUnitId = value.enteredUnitId,
                    dateEpochDay = value.dateEpochDay,
                    booleanValue = value.booleanValue,
                    choiceOptionId = value.choiceOptionId,
                    scaleValue = value.scaleValue,
                    createdAtMillis = current?.createdAtMillis ?: now,
                    updatedAtMillis = now,
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

    private suspend fun validateRestoreCompatibility(
        deleted: DeletedTrackEntry,
        currentForm: TrackEntryFormRows,
    ) {
        val opening = deleted.openingFormBoundary
        if (
            currentForm.track.id != opening.trackId || currentForm.track.uuid != opening.trackUuid ||
            currentForm.track.createdAtMillis != opening.trackCreatedAtMillis
        ) {
            throw TrackEntryConflictException(
                TrackEntryConflictKind.IdentityChanged,
                "The deleted Entry's Track identity is no longer available.",
            )
        }
        require(deleted.values.map(TrackFieldValue::uuid).distinct().size == deleted.values.size) {
            "Deleted Entry contains duplicate value identities"
        }
        require(deleted.values.map(TrackFieldValue::fieldId).distinct().size == deleted.values.size) {
            "Deleted Entry contains duplicate Field values"
        }
        val openingFields = opening.fieldContracts.associateBy(TrackEntryFieldContract::id)
        val currentFields = currentForm.fields.associateBy(TrackFieldEntity::id)
        val openingChoices = opening.choiceContracts.associateBy(TrackEntryChoiceContract::id)
        val currentChoices = currentForm.options.associateBy(TrackChoiceOptionEntity::id)
        val openingUnits = opening.unitContracts.associateBy(TrackEntryUnitContract::id)
        deleted.values.forEach { value ->
            dao.getValueByUuid(value.uuid)?.let {
                throw TrackEntryConflictException(
                    TrackEntryConflictKind.IdentityCollision,
                    "A saved value already uses the deleted value's stable identity.",
                )
            }
            val beforeField = openingFields[value.fieldId] ?: restoreConflict("A deleted value's Field contract is missing.")
            val field = currentFields[value.fieldId] ?: restoreConflict("A deleted value's Field no longer exists.")
            if (
                field.uuid != beforeField.uuid || field.trackId != currentForm.track.id ||
                field.type != beforeField.type.name
            ) {
                restoreConflict("A deleted value's Field identity or type changed.")
            }
            validateStoredValueShape(value, beforeField.type)
            when (beforeField.type) {
                TrackFieldType.Number -> {
                    if (field.dimension != beforeField.dimension?.name) {
                        restoreConflict("A deleted Number value's measurement type changed.")
                    }
                    val unitId = value.enteredUnitId ?: restoreConflict("A deleted Number value has no unit.")
                    val beforeUnit = openingUnits[unitId] ?: restoreConflict("A deleted Number value's unit contract is missing.")
                    val currentUnit = resolveUnit(unitId)?.toEntryContract()
                        ?: restoreConflict("A deleted Number value's unit no longer exists.")
                    if (!currentUnit.sameStoredNumberSemanticsAs(beforeUnit) || currentUnit.dimension.name != field.dimension) {
                        restoreConflict("A deleted Number value's unit semantics changed.")
                    }
                }
                TrackFieldType.SingleChoice -> {
                    val choiceId = value.choiceOptionId ?: restoreConflict("A deleted Choice value is incomplete.")
                    val beforeChoice = openingChoices[choiceId] ?: restoreConflict("A deleted Choice contract is missing.")
                    val choice = currentChoices[choiceId] ?: restoreConflict("A deleted Choice no longer exists.")
                    if (choice.uuid != beforeChoice.uuid || choice.fieldId != field.id || beforeChoice.fieldId != field.id) {
                        restoreConflict("A deleted Choice identity or ownership changed.")
                    }
                }
                TrackFieldType.Scale -> {
                    val scale = value.scaleValue ?: restoreConflict("A deleted Scale value is incomplete.")
                    if (
                        normalizeTrackScaleValue(
                            scale,
                            field.scaleMin ?: restoreConflict("The Scale minimum is missing."),
                            field.scaleMax ?: restoreConflict("The Scale maximum is missing."),
                            field.scaleStep,
                        ) == null
                    ) {
                        restoreConflict("A deleted Scale value no longer fits the current Scale.")
                    }
                }
                else -> Unit
            }
        }
        deleted.entry.sourceOccurrenceId?.let { sourceId ->
            if (deleted.sourceOccurrence?.id != sourceId) {
                throw TrackEntryConflictException(
                    TrackEntryConflictKind.ProvenanceChanged,
                    "The deleted Entry's exact source occurrence was not captured.",
                )
            }
        }
    }

    private suspend fun validateOccurrencesBeforeRestore(deleted: DeletedTrackEntry) {
        require(deleted.fulfilledOccurrences.map(TrackEntryFulfillmentSnapshot::id).distinct().size == deleted.fulfilledOccurrences.size) {
            "Deleted Entry contains duplicate occurrence identities"
        }
        val exactOccurrences = (deleted.fulfilledOccurrences + listOfNotNull(deleted.sourceOccurrence))
            .distinctBy(TrackEntryFulfillmentSnapshot::id)
        exactOccurrences.forEach { expected ->
            val fulfilledThisEntry = expected.fulfilledEntryId == deleted.entry.id
            if (expected.fulfilledEntryId != deleted.entry.id) {
                if (deleted.fulfilledOccurrences.any { it.id == expected.id }) {
                    throw TrackEntryConflictException(
                        TrackEntryConflictKind.ProvenanceChanged,
                        "A saved fulfillment occurrence does not belong to this deleted Entry.",
                    )
                }
            }
            val current = linkDao.getTriggerOccurrence(expected.id)
            val expectedAfterDelete = expected.copy(
                fulfilledEntryId = if (fulfilledThisEntry) null else expected.fulfilledEntryId,
            )
            if (current?.toTrackEntrySnapshot() != expectedAfterDelete) {
                throw TrackEntryConflictException(
                    TrackEntryConflictKind.ProvenanceChanged,
                    "A source occurrence changed before this Entry could be restored.",
                )
            }
        }
    }

    private suspend fun restoredOccurrencesMatch(
        expected: List<TrackEntryFulfillmentSnapshot>,
        restoredEntryId: Long,
    ): Boolean = expected.all { occurrence ->
        linkDao.getTriggerOccurrence(occurrence.id)?.toTrackEntrySnapshot() ==
            occurrence.copy(fulfilledEntryId = restoredEntryId)
    }

    private suspend fun sourceOccurrenceMatches(deleted: DeletedTrackEntry, restoredEntryId: Long): Boolean {
        val expected = deleted.sourceOccurrence ?: return deleted.entry.sourceOccurrenceId == null
        val fulfilledId = if (expected.fulfilledEntryId == deleted.entry.id) restoredEntryId else expected.fulfilledEntryId
        return linkDao.getTriggerOccurrence(expected.id)?.toTrackEntrySnapshot() ==
            expected.copy(fulfilledEntryId = fulfilledId)
    }

    private fun restoreConflict(message: String): Nothing = throw TrackEntryConflictException(
        TrackEntryConflictKind.RestoreIncompatible,
        message,
    )

}

private data class TrackEntryFormRows(
    val track: TrackEntity,
    val fields: List<TrackFieldEntity>,
    val options: List<TrackChoiceOptionEntity>,
    val units: List<UnitDefinition>,
) {
    fun boundary(): TrackEntryFormBoundary {
        val fieldContracts = fields.map(TrackFieldEntity::toEntryContract)
        val choiceContracts = options.map(TrackChoiceOptionEntity::toEntryContract)
        val unitContracts = units.map(UnitDefinition::toEntryContract).sortedBy(TrackEntryUnitContract::id)
        val unitById = unitContracts.associateBy(TrackEntryUnitContract::id)
        return TrackEntryFormBoundary(
            trackId = track.id,
            trackUuid = track.uuid,
            trackCreatedAtMillis = track.createdAtMillis,
            writable = !track.archived,
            semanticRevisionToken = canonicalTrackRevision {
                value("track.id", track.id)
                value("track.uuid", track.uuid)
                value("track.created", track.createdAtMillis)
                value("track.writable", !track.archived)
                fieldContracts.sortedBy(TrackEntryFieldContract::id).forEach(::row)
                choiceContracts.sortedBy(TrackEntryChoiceContract::id).forEach(::row)
                // Non-default units are carried for selected-unit validation but
                // do not invalidate a form unless the user actually chose them.
                fieldContracts.filter { it.type == TrackFieldType.Number }
                    .mapNotNull(TrackEntryFieldContract::unitId)
                    .distinct()
                    .sorted()
                    .forEach { unitId ->
                        value("defaultUnit.id", unitId)
                        unitById[unitId]?.let(::row)
                    }
            },
            fieldContracts = fieldContracts,
            choiceContracts = choiceContracts,
            unitContracts = unitContracts,
        )
    }

    fun toDomainSnapshot(): TrackEntryFormSnapshot = TrackEntryFormSnapshot(
        boundary = boundary(),
        track = track.toDomain(),
        fields = fields.map(TrackFieldEntity::toDomain),
        options = options.map(TrackChoiceOptionEntity::toDomain),
        units = units.map(UnitDefinition::toEntryContract),
    )
}

private data class TrackEntrySnapshot(
    val form: TrackEntryFormRows,
    val entry: TrackEntryEntity,
    val values: List<TrackValueEntity>,
) {
    fun boundary(): TrackEntryBoundary {
        val enteredUnits = values.mapNotNull(TrackValueEntity::enteredUnitId)
            .distinct()
            .mapNotNull { id -> form.units.firstOrNull { it.id == id } }
            .map(UnitDefinition::toEntryContract)
            .sortedBy(TrackEntryUnitContract::id)
        return TrackEntryBoundary(
            formBoundary = form.boundary(),
            entryId = entry.id,
            entryUuid = entry.uuid,
            entryCreatedAtMillis = entry.createdAtMillis,
            semanticRevisionToken = canonicalTrackRevision {
                value("entry.id", entry.id)
                value("entry.uuid", entry.uuid)
                value("entry.trackId", entry.trackId)
                value("entry.date", entry.entryEpochDay)
                value("entry.sourceOccurrenceId", entry.sourceOccurrenceId)
                value("entry.sourceExplanation", entry.sourceExplanation)
                values.sortedBy(TrackValueEntity::id).forEach(::entryRow)
                enteredUnits.forEach(::row)
            },
            enteredUnitContracts = enteredUnits,
        )
    }

    fun toEditSnapshot(): TrackEntryEditSnapshot {
        val domainFields = form.fields.map(TrackFieldEntity::toDomain)
        val domainOptions = form.options.map(TrackChoiceOptionEntity::toDomain)
        val valueByField = values.associateBy(TrackValueEntity::fieldId)
        val draft = TrackEntryDraft(
            entryDate = LocalDate.ofEpochDay(entry.entryEpochDay),
            values = domainFields.associate { field ->
                val value = valueByField[field.id]
                field.uuid to TrackValueDraft(
                    textValue = value?.textValue,
                    enteredNumber = value?.enteredNumber,
                    enteredUnitId = value?.enteredUnitId ?: field.unitId,
                    dateValue = value?.dateEpochDay?.let(LocalDate::ofEpochDay),
                    booleanValue = value?.booleanValue,
                    choiceOptionUuid = domainOptions.firstOrNull { it.id == value?.choiceOptionId }?.uuid,
                    scaleValue = value?.scaleValue,
                )
            },
        )
        val domainEntry = entry.toDomain()
        val domainValues = values.map(TrackValueEntity::toDomain).associateBy(TrackFieldValue::fieldId)
        val projection = TrackProjection(
            track = form.track.toDomain(),
            fields = domainFields,
            options = domainOptions,
            entries = listOf(TrackEntryProjection(domainEntry, domainValues)),
        )
        return TrackEntryEditSnapshot(
            boundary = boundary(),
            form = form.toDomainSnapshot(),
            draft = draft,
            displayName = projection.primaryText(projection.entries.single()),
            populatedValueCount = values.size,
        )
    }

    fun matches(
        normalized: NormalizedTrackEntryDraft,
        sourceOccurrenceId: Long?,
        sourceExplanation: String,
    ): Boolean =
        entry.entryEpochDay == normalized.entryDate.toEpochDay() &&
            entry.sourceOccurrenceId == sourceOccurrenceId &&
            entry.sourceExplanation == sourceExplanation &&
            values.associateBy(TrackValueEntity::fieldId).let { current ->
                current.keys == normalized.values.keys && normalized.values.all { (fieldId, expected) ->
                    current[fieldId]?.matches(expected) == true
                }
            }

    fun matchesDeleted(deleted: DeletedTrackEntry): Boolean =
        entry.uuid == deleted.entry.uuid && entry.trackId == deleted.entry.trackId &&
            entry.entryEpochDay == deleted.entry.entryDate.toEpochDay() &&
            entry.sourceOccurrenceId == deleted.entry.sourceOccurrenceId &&
            entry.sourceExplanation == deleted.entry.sourceExplanation &&
            entry.createdAtMillis == deleted.entry.createdAtMillis &&
            entry.updatedAtMillis == deleted.entry.updatedAtMillis &&
            values.matchDeleted(deleted.values)

    fun receipt(
        kind: TrackEntryMutationKind,
        changed: Boolean,
        alreadyApplied: Boolean,
    ): TrackEntryMutationReceipt = TrackEntryMutationReceipt(
        kind = kind,
        trackId = form.track.id,
        trackUuid = form.track.uuid,
        entryId = entry.id,
        entryUuid = entry.uuid,
        changed = changed,
        alreadyApplied = alreadyApplied,
        affectedValueCount = values.size,
        postBoundary = boundary(),
    )
}

private data class NormalizedTrackEntryDraft(
    val entryDate: LocalDate,
    val values: Map<Long, NormalizedTrackValue>,
)

private data class NormalizedTrackValue(
    val fieldId: Long,
    val textValue: String? = null,
    val enteredNumber: Double? = null,
    val canonicalNumber: Double? = null,
    val enteredUnitId: String? = null,
    val dateEpochDay: Long? = null,
    val booleanValue: Boolean? = null,
    val choiceOptionId: Long? = null,
    val scaleValue: Double? = null,
)

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

private fun TrackEntryFormBoundary.sameContractAs(other: TrackEntryFormBoundary): Boolean =
    trackId == other.trackId && trackUuid == other.trackUuid &&
        trackCreatedAtMillis == other.trackCreatedAtMillis && writable == other.writable &&
        semanticRevisionToken == other.semanticRevisionToken

private fun TrackFieldEntity.toEntryContract() = TrackEntryFieldContract(
    id = id,
    uuid = uuid,
    trackId = trackId,
    name = name,
    type = TrackFieldType.valueOf(type),
    required = required,
    primary = primaryField,
    dimension = dimension?.let(UnitDimension::valueOf),
    unitId = unitId,
    precision = precision,
    scaleMin = scaleMin,
    scaleMax = scaleMax,
    scaleLowLabel = scaleLowLabel,
    scaleHighLabel = scaleHighLabel,
    scaleStep = scaleStep,
)

private fun TrackChoiceOptionEntity.toEntryContract() = TrackEntryChoiceContract(
    id = id,
    uuid = uuid,
    fieldId = fieldId,
    label = label,
)

private fun UnitDefinition.toEntryContract() = TrackEntryUnitContract(
    id = id,
    name = name,
    symbol = symbol,
    dimension = dimension,
    toCanonicalFactor = toCanonicalFactor,
    toCanonicalOffset = toCanonicalOffset,
    archived = archived,
)

private fun TrackEntryFieldContract.toDomain() = TrackField(
    id = id,
    uuid = uuid,
    trackId = trackId,
    name = name,
    type = type,
    position = 0,
    required = required,
    primary = primary,
    showInList = false,
    dimension = dimension,
    unitId = unitId,
    precision = precision,
    scaleMin = scaleMin,
    scaleMax = scaleMax,
    scaleLowLabel = scaleLowLabel,
    scaleHighLabel = scaleHighLabel,
    createdAtMillis = 0,
    updatedAtMillis = 0,
    scaleStep = scaleStep,
)

private fun TrackEntryChoiceContract.toDomain() = TrackChoiceOption(
    id = id,
    uuid = uuid,
    fieldId = fieldId,
    label = label,
    position = 0,
    createdAtMillis = 0,
    updatedAtMillis = 0,
)

private fun CanonicalTrackRevisionBuilder.row(contract: TrackEntryFieldContract) {
    value("form.field.id", contract.id); value("form.field.uuid", contract.uuid)
    value("form.field.trackId", contract.trackId); value("form.field.name", contract.name)
    value("form.field.type", contract.type.name); value("form.field.required", contract.required)
    value("form.field.primary", contract.primary); value("form.field.dimension", contract.dimension?.name)
    value("form.field.unitId", contract.unitId); value("form.field.precision", contract.precision)
    value("form.field.scaleMin", contract.scaleMin); value("form.field.scaleMax", contract.scaleMax)
    value("form.field.scaleLowLabel", contract.scaleLowLabel); value("form.field.scaleHighLabel", contract.scaleHighLabel)
    value("form.field.scaleStep", contract.scaleStep)
}

private fun CanonicalTrackRevisionBuilder.row(contract: TrackEntryChoiceContract) {
    value("form.choice.id", contract.id); value("form.choice.uuid", contract.uuid)
    value("form.choice.fieldId", contract.fieldId); value("form.choice.label", contract.label)
}

private fun CanonicalTrackRevisionBuilder.row(contract: TrackEntryUnitContract) {
    value("form.unit.id", contract.id); value("form.unit.name", contract.name)
    value("form.unit.symbol", contract.symbol); value("form.unit.dimension", contract.dimension.name)
    value("form.unit.factor", contract.toCanonicalFactor); value("form.unit.offset", contract.toCanonicalOffset)
    value("form.unit.archived", contract.archived)
}

private fun CanonicalTrackRevisionBuilder.entryRow(row: TrackValueEntity) {
    value("entry.value.id", row.id); value("entry.value.uuid", row.uuid)
    value("entry.value.entryId", row.entryId); value("entry.value.fieldId", row.fieldId)
    value("entry.value.text", row.textValue); value("entry.value.enteredNumber", row.enteredNumber)
    value("entry.value.canonicalNumber", row.canonicalNumber); value("entry.value.unit", row.enteredUnitId)
    value("entry.value.date", row.dateEpochDay); value("entry.value.boolean", row.booleanValue)
    value("entry.value.choice", row.choiceOptionId); value("entry.value.scale", row.scaleValue)
}

private fun TrackValueEntity.matches(expected: NormalizedTrackValue): Boolean =
    fieldId == expected.fieldId && textValue == expected.textValue &&
        enteredNumber.rawEquals(expected.enteredNumber) && canonicalNumber.rawEquals(expected.canonicalNumber) &&
        enteredUnitId == expected.enteredUnitId && dateEpochDay == expected.dateEpochDay &&
        booleanValue == expected.booleanValue && choiceOptionId == expected.choiceOptionId &&
        scaleValue.rawEquals(expected.scaleValue)

private fun List<TrackValueEntity>.matchDeleted(expected: List<TrackFieldValue>): Boolean {
    if (size != expected.size) return false
    val expectedByUuid = expected.associateBy(TrackFieldValue::uuid)
    return all { current ->
        val old = expectedByUuid[current.uuid] ?: return@all false
        current.fieldId == old.fieldId && current.textValue == old.textValue &&
            current.enteredNumber.rawEquals(old.enteredNumber) && current.canonicalNumber.rawEquals(old.canonicalNumber) &&
            current.enteredUnitId == old.enteredUnitId && current.dateEpochDay == old.dateValue?.toEpochDay() &&
            current.booleanValue == old.booleanValue && current.choiceOptionId == old.choiceOptionId &&
            current.scaleValue.rawEquals(old.scaleValue) && current.createdAtMillis == old.createdAtMillis &&
            current.updatedAtMillis == old.updatedAtMillis
    }
}

private fun Double?.rawEquals(other: Double?): Boolean = when {
    this == null || other == null -> this == null && other == null
    else -> java.lang.Double.doubleToRawLongBits(this) == java.lang.Double.doubleToRawLongBits(other)
}

private fun TrackEntryUnitContract.sameStoredNumberSemanticsAs(other: TrackEntryUnitContract): Boolean =
    id == other.id && dimension == other.dimension &&
        toCanonicalFactor.rawEquals(other.toCanonicalFactor) &&
        toCanonicalOffset.rawEquals(other.toCanonicalOffset)

private fun TriggerOccurrenceEntity.toTrackEntrySnapshot() = TrackEntryFulfillmentSnapshot(
    id = id,
    triggerRuleId = triggerRuleId,
    sourceEventId = sourceEventId,
    availableAtMillis = availableAtMillis,
    deliveredAtMillis = deliveredAtMillis,
    dismissedAtMillis = dismissedAtMillis,
    remindAtMillis = remindAtMillis,
    fulfilledEntryId = fulfilledEntryId,
    sourceSnapshot = sourceSnapshot,
)

private fun validateStoredValueShape(value: TrackFieldValue, type: TrackFieldType) {
    val valid = when (type) {
        TrackFieldType.ShortText, TrackFieldType.LongText ->
            value.textValue != null && value.enteredNumber == null && value.canonicalNumber == null &&
                value.enteredUnitId == null && value.dateValue == null && value.booleanValue == null &&
                value.choiceOptionId == null && value.scaleValue == null
        TrackFieldType.Number ->
            value.textValue == null && value.enteredNumber?.isFinite() == true &&
                value.canonicalNumber?.isFinite() == true && value.enteredUnitId != null &&
                value.dateValue == null && value.booleanValue == null && value.choiceOptionId == null &&
                value.scaleValue == null
        TrackFieldType.SingleChoice ->
            value.textValue == null && value.enteredNumber == null && value.canonicalNumber == null &&
                value.enteredUnitId == null && value.dateValue == null && value.booleanValue == null &&
                value.choiceOptionId != null && value.scaleValue == null
        TrackFieldType.Scale ->
            value.textValue == null && value.enteredNumber == null && value.canonicalNumber == null &&
                value.enteredUnitId == null && value.dateValue == null && value.booleanValue == null &&
                value.choiceOptionId == null && value.scaleValue?.isFinite() == true
        TrackFieldType.Date ->
            value.textValue == null && value.enteredNumber == null && value.canonicalNumber == null &&
                value.enteredUnitId == null && value.dateValue != null && value.booleanValue == null &&
                value.choiceOptionId == null && value.scaleValue == null
        TrackFieldType.YesNo ->
            value.textValue == null && value.enteredNumber == null && value.canonicalNumber == null &&
                value.enteredUnitId == null && value.dateValue == null && value.booleanValue != null &&
                value.choiceOptionId == null && value.scaleValue == null
    }
    if (!valid) {
        throw TrackEntryConflictException(
            TrackEntryConflictKind.RestoreIncompatible,
            "A deleted value no longer has a valid ${type.name} shape.",
        )
    }
}

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
