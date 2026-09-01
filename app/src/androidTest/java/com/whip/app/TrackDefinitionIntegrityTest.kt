package com.whip.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.data.ContributionEntity
import com.whip.app.data.GoalEntity
import com.whip.app.data.LinkConditionChoiceEntity
import com.whip.app.data.LinkRuleConditionEntity
import com.whip.app.data.LinkRuleEntity
import com.whip.app.data.RoomAreaRepository
import com.whip.app.data.RoomTrackRepository
import com.whip.app.data.TriggerConditionChoiceEntity
import com.whip.app.data.TriggerFieldMappingEntity
import com.whip.app.data.TriggerOccurrenceEntity
import com.whip.app.data.TriggerRuleConditionEntity
import com.whip.app.data.TriggerRuleEntity
import com.whip.app.data.UnitDefinitionEntity
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.TrackChoiceOptionDraft
import com.whip.app.domain.TrackCsvMapping
import com.whip.app.domain.TrackDefinitionConflictException
import com.whip.app.domain.TrackDefinitionConflictKind
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackProjection
import com.whip.app.domain.TrackValueDraft
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.trackCsvPayloadFingerprint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackDefinitionIntegrityTest {
    private lateinit var database: WhipDatabase
    private lateinit var tracks: RoomTrackRepository
    private lateinit var areas: RoomAreaRepository

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WhipDatabase::class.java,
        ).addCallback(WhipDatabase.integrityGuardCallback).build()
        val ids = TestIds()
        areas = RoomAreaRepository(database, TestClock, ids)
        areas.ensureDefaultArea()
        tracks = RoomTrackRepository(database, TestClock, ids)
    }

    @After fun tearDown() = database.close()

    @Test
    fun reviewedChoiceDeletionRejectsValueAddedAfterReviewAndPreservesEverything() = runBlocking {
        val trackId = tracks.create(choiceDraft())
        var projection = requireNotNull(tracks.projection(trackId))
        val genre = projection.fields.single { it.name == "Genre" }
        val history = projection.optionsFor(genre.id).single { it.label == "History" }
        addChoiceEntry(trackId, projection, "First", genre.uuid, history.uuid)
        projection = requireNotNull(tracks.projection(trackId))
        val removalDraft = projection.toDraft().withoutOption(history.id).copy(name = "Should not commit")
        val boundary = requireNotNull(tracks.definitionBoundary(trackId))
        val review = tracks.reviewDefinitionUpdate(trackId, removalDraft, boundary)
        assertEquals(1, review.removedChoices.single { it.optionId == history.id }.savedValueCount)

        addChoiceEntry(trackId, projection, "Concurrent", genre.uuid, history.uuid)

        val failure = runCatching { tracks.update(trackId, removalDraft, boundary, review) }.exceptionOrNull()
        assertTrue(failure is TrackDefinitionConflictException)
        assertEquals(TrackDefinitionConflictKind.RemovalImpactChanged, (failure as TrackDefinitionConflictException).kind)
        val current = requireNotNull(tracks.projection(trackId))
        assertEquals("Choice Track", current.track.name)
        assertNotNull(current.options.firstOrNull { it.id == history.id })
        assertEquals(2, current.entries.count { it.value(genre.id)?.choiceOptionId == history.id })
    }

    @Test
    fun sameCountValueEditInvalidatesFieldRemovalReviewAndRollsBackMetadata() = runBlocking {
        val trackId = tracks.create(textFieldDraft())
        var projection = requireNotNull(tracks.projection(trackId))
        val notes = projection.fields.single { it.name == "Notes" }
        val entryId = tracks.addEntry(
            trackId,
            TrackEntryDraft(
                TestClock.today(),
                mapOf(
                    projection.primaryField.uuid to TrackValueDraft(textValue = "Rollback sentinel"),
                    notes.uuid to TrackValueDraft(textValue = "Before"),
                ),
            ),
        )
        projection = requireNotNull(tracks.projection(trackId))
        val removalDraft = projection.toDraft().copy(
            name = "Stale rename",
            fields = projection.toDraft().fields.filterNot { it.id == notes.id },
        )
        val boundary = requireNotNull(tracks.definitionBoundary(trackId))
        val review = tracks.reviewDefinitionUpdate(trackId, removalDraft, boundary)

        tracks.updateEntry(
            entryId,
            TrackEntryDraft(
                TestClock.today(),
                mapOf(
                    projection.primaryField.uuid to TrackValueDraft(textValue = "Rollback sentinel"),
                    notes.uuid to TrackValueDraft(textValue = "After"),
                ),
            ),
        )

        val failure = runCatching { tracks.update(trackId, removalDraft, boundary, review) }.exceptionOrNull()
        assertTrue(failure is TrackDefinitionConflictException)
        val current = requireNotNull(tracks.projection(trackId))
        assertEquals("Text Track", current.track.name)
        assertEquals("After", current.entries.single().value(notes.id)?.textValue)
        assertEquals(setOf(entryId), tracks.searchEntryIds(trackId, "Rollback sentinel"))
    }

    @Test
    fun staleDefinitionCannotDeleteAFieldAddedByAnotherSave() = runBlocking {
        val trackId = tracks.create(textFieldDraft())
        val opening = requireNotNull(tracks.projection(trackId))
        val staleBoundary = requireNotNull(tracks.definitionBoundary(trackId))
        val newerDraft = opening.toDraft().copy(
            fields = opening.toDraft().fields + TrackFieldDraft("Concurrent Field", TrackFieldType.YesNo),
        )
        tracks.update(trackId, newerDraft, staleBoundary)

        val staleDraft = opening.toDraft().copy(name = "Stale overwrite")
        val failure = runCatching { tracks.update(trackId, staleDraft, staleBoundary) }.exceptionOrNull()

        assertTrue(failure is TrackDefinitionConflictException)
        assertEquals(TrackDefinitionConflictKind.DefinitionChanged, (failure as TrackDefinitionConflictException).kind)
        val current = requireNotNull(tracks.projection(trackId))
        assertEquals("Text Track", current.track.name)
        assertNotNull(current.fields.firstOrNull { it.name == "Concurrent Field" })
    }

    @Test
    fun nondestructiveSaveDoesNotConflictWithConcurrentEntryImport() = runBlocking {
        val trackId = tracks.create(textFieldDraft())
        val opening = requireNotNull(tracks.projection(trackId))
        val boundary = requireNotNull(tracks.definitionBoundary(trackId))
        val importedDrafts = listOf(
            TrackEntryDraft(
                TestClock.today(),
                mapOf(opening.primaryField.uuid to TrackValueDraft(textValue = "Imported safely")),
            ),
        )
        val preparation = requireNotNull(
            tracks.prepareCsvImport(
                openingForm = requireNotNull(tracks.csvImportForm(trackId)),
                batchUuid = "8060d0d9-9a55-4ebd-805e-ee88ee04c16e",
                payloadFingerprint = trackCsvPayloadFingerprint("Title\nImported safely\n"),
                mapping = TrackCsvMapping(
                    fieldColumns = mapOf(opening.primaryField.uuid to "Title"),
                ),
                defaultEntryDate = TestClock.today(),
                drafts = importedDrafts,
            ),
        )
        val importReceipt = tracks.importEntries(preparation.request, importedDrafts)
        val entryId = requireNotNull(database.trackDao().getEntryByUuid(preparation.request.entryUuids.single())).id
        assertEquals(1, importReceipt.rowCount)

        val receipt = tracks.update(trackId, opening.toDraft().copy(description = "Metadata only"), boundary)

        assertFalse(receipt.schemaChanged)
        val current = requireNotNull(tracks.projection(trackId))
        assertEquals("Metadata only", current.track.description)
        assertEquals(entryId, current.entries.single().entry.id)
    }

    @Test
    fun choiceReplacementRetargetsExactValuesAndDormantLinkTriggerRows() = runBlocking {
        val trackId = tracks.create(choiceDraft())
        var projection = requireNotNull(tracks.projection(trackId))
        val genre = projection.fields.single { it.name == "Genre" }
        val history = projection.optionsFor(genre.id).single { it.label == "History" }
        val fiction = projection.optionsFor(genre.id).single { it.label == "Fiction" }
        addChoiceEntry(trackId, projection, "Retarget me", genre.uuid, history.uuid)
        val legacy = insertDormantDefinitions(trackId, genre.id, history.id, fiction.id)
        projection = requireNotNull(tracks.projection(trackId))
        val draft = projection.toDraft().withoutOption(history.id)
        val boundary = requireNotNull(tracks.definitionBoundary(trackId))
        val review = tracks.reviewDefinitionUpdate(
            trackId,
            draft,
            boundary,
            choiceReplacementIds = mapOf(history.id to fiction.id),
        )
        val impact = review.removedChoices.single { it.optionId == history.id }
        assertEquals(1, impact.savedValueCount)
        assertEquals(1, impact.legacyLinkConditionCount)
        assertEquals(1, impact.legacyTriggerConditionCount)
        assertEquals(1, impact.legacyTriggerMappingCount)

        val receipt = tracks.update(trackId, draft, boundary, review)

        assertEquals(1, receipt.replacedValueCount)
        val current = requireNotNull(tracks.projection(trackId))
        assertEquals(fiction.id, current.entries.single().value(genre.id)?.choiceOptionId)
        val linkChoices = database.linkDao().getLinkConditionChoices(listOf(legacy.linkConditionId))
        assertEquals(listOf(fiction.id), linkChoices.map { it.optionId })
        val triggerChoices = database.linkDao().getTriggerConditionChoices(listOf(legacy.triggerConditionId))
        assertEquals(listOf(fiction.id), triggerChoices.map { it.optionId })
        assertEquals(fiction.id, database.linkDao().getTriggerMappings(legacy.triggerRuleId).single().constantChoiceOptionId)
        assertEquals(1, database.linkDao().getContributions(legacy.linkRuleId).size)
    }

    @Test
    fun choiceDeletionClearsOnlyReviewedValuesAndDormantReferences() = runBlocking {
        val trackId = tracks.create(choiceDraft())
        var projection = requireNotNull(tracks.projection(trackId))
        val genre = projection.fields.single { it.name == "Genre" }
        val history = projection.optionsFor(genre.id).single { it.label == "History" }
        val fiction = projection.optionsFor(genre.id).single { it.label == "Fiction" }
        addChoiceEntry(trackId, projection, "Keep this Entry", genre.uuid, history.uuid)
        val legacy = insertDormantDefinitions(trackId, genre.id, history.id, fiction.id)
        projection = requireNotNull(tracks.projection(trackId))
        val draft = projection.toDraft().withoutOption(history.id)
        val boundary = requireNotNull(tracks.definitionBoundary(trackId))
        val review = tracks.reviewDefinitionUpdate(trackId, draft, boundary)
        val impact = review.removedChoices.single { it.optionId == history.id }
        assertEquals(1, impact.savedValueCount)
        assertEquals(1, impact.legacyLinkConditionCount)
        assertEquals(1, impact.legacyTriggerConditionCount)
        assertEquals(1, impact.legacyTriggerMappingCount)

        val receipt = tracks.update(trackId, draft, boundary, review)

        assertEquals(1, receipt.deletedValueCount)
        assertEquals(0, receipt.replacedValueCount)
        val current = requireNotNull(tracks.projection(trackId))
        assertNull(current.options.firstOrNull { it.id == history.id })
        assertNotNull(current.options.firstOrNull { it.id == fiction.id })
        assertEquals("Keep this Entry", current.primaryText(current.entries.single()))
        assertNull(current.entries.single().value(genre.id))
        assertEquals(listOf(fiction.id), database.linkDao().getLinkConditionChoices(listOf(legacy.linkConditionId)).map { it.optionId })
        assertEquals(
            listOf(fiction.id),
            database.linkDao().getTriggerConditionChoices(listOf(legacy.triggerConditionId)).map { it.optionId },
        )
        assertNull(database.linkDao().getTriggerMappings(legacy.triggerRuleId).single().constantChoiceOptionId)
        assertNotNull(database.linkDao().getRule(legacy.linkRuleId))
        assertNotNull(database.linkDao().getTriggerRule(legacy.triggerRuleId))
        assertEquals(legacy.contributionId, database.linkDao().getContributions(legacy.linkRuleId).single().id)
        assertEquals(legacy.occurrenceId, database.linkDao().getTriggerOccurrences(legacy.triggerRuleId).single().id)
    }

    @Test
    fun dependencyAddedAfterFieldReviewRejectsWithoutCascadingIt() = runBlocking {
        val trackId = tracks.create(textFieldDraft())
        val projection = requireNotNull(tracks.projection(trackId))
        val notes = projection.fields.single { it.name == "Notes" }
        val draft = projection.toDraft().copy(fields = projection.toDraft().fields.filterNot { it.id == notes.id })
        val boundary = requireNotNull(tracks.definitionBoundary(trackId))
        val review = tracks.reviewDefinitionUpdate(trackId, draft, boundary)

        val ruleId = insertTriggerRule(trackId)
        val mappingId = database.linkDao().insertTriggerMapping(
            TriggerFieldMappingEntity(
                triggerRuleId = ruleId,
                targetFieldId = notes.id,
                sourceProperty = "Constant",
                constantText = "late dependency",
                constantNumber = null,
                constantUnitId = null,
                constantDateEpochDay = null,
                constantBoolean = null,
                constantChoiceOptionId = null,
                constantScale = null,
            ),
        )

        val failure = runCatching { tracks.update(trackId, draft, boundary, review) }.exceptionOrNull()
        assertTrue(failure is TrackDefinitionConflictException)
        assertNotNull(database.trackDao().getField(notes.id))
        assertEquals(mappingId, database.linkDao().getTriggerMappings(ruleId).single().id)
    }

    @Test
    fun reviewedFieldDeletionReconcilesDormantDefinitionsButPreservesTheirHistory() = runBlocking {
        val trackId = tracks.create(textFieldDraft())
        var projection = requireNotNull(tracks.projection(trackId))
        val notes = projection.fields.single { it.name == "Notes" }
        val entryId = tracks.addEntry(
            trackId,
            TrackEntryDraft(
                TestClock.today(),
                mapOf(
                    projection.primaryField.uuid to TrackValueDraft(textValue = "Field deletion history"),
                    notes.uuid to TrackValueDraft(textValue = "Reviewed value"),
                ),
            ),
        )
        val legacy = insertDormantFieldDefinitions(trackId, notes.id, entryId)
        projection = requireNotNull(tracks.projection(trackId))
        val draft = projection.toDraft().copy(
            description = "Field removed exactly",
            fields = projection.toDraft().fields.filterNot { it.id == notes.id },
        )
        val boundary = requireNotNull(tracks.definitionBoundary(trackId))
        val review = tracks.reviewDefinitionUpdate(trackId, draft, boundary)
        val impact = review.removedFields.single { it.fieldId == notes.id }
        assertEquals(1, impact.savedValueCount)
        assertEquals(1, impact.legacyLinkSourceCount)
        assertEquals(1, impact.legacyLinkConditionCount)
        assertEquals(1, impact.legacyTriggerConditionCount)
        assertEquals(1, impact.legacyTriggerMappingCount)

        val receipt = tracks.update(trackId, draft, boundary, review)

        assertEquals(1, receipt.removedFieldCount)
        assertEquals(1, receipt.deletedValueCount)
        val current = requireNotNull(tracks.projection(trackId))
        assertEquals("Field removed exactly", current.track.description)
        assertNull(current.fields.firstOrNull { it.id == notes.id })
        assertEquals(entryId, current.entries.single().entry.id)
        assertEquals("Field deletion history", current.primaryText(current.entries.single()))
        assertEquals(setOf(entryId), tracks.searchEntryIds(trackId, "Field deletion history"))

        assertNull(requireNotNull(database.linkDao().getRule(legacy.linkRuleId)).sourceFieldId)
        assertNull(database.linkDao().getRuleConditions(legacy.linkRuleId).single().fieldId)
        assertEquals(legacy.contributionId, database.linkDao().getContributions(legacy.linkRuleId).single().id)
        assertNotNull(database.linkDao().getTriggerRule(legacy.triggerRuleId))
        assertNull(database.linkDao().getTriggerConditions(legacy.triggerRuleId).single().fieldId)
        assertTrue(database.linkDao().getTriggerMappings(legacy.triggerRuleId).isEmpty())
        val occurrence = database.linkDao().getTriggerOccurrences(legacy.triggerRuleId).single()
        assertEquals(legacy.occurrenceId, occurrence.id)
        assertEquals(entryId, occurrence.fulfilledEntryId)
    }

    @Test
    fun numberDimensionChangeWithHistoryRollsBackDefinitionAndSearch() = runBlocking {
        val trackId = tracks.create(numberDraft())
        var projection = requireNotNull(tracks.projection(trackId))
        val score = projection.fields.single { it.name == "Score" }
        val entryId = tracks.addEntry(
            trackId,
            TrackEntryDraft(
                TestClock.today(),
                mapOf(
                    projection.primaryField.uuid to TrackValueDraft(textValue = "Dimension rollback"),
                    score.uuid to TrackValueDraft(enteredNumber = 4.0, enteredUnitId = "unitless"),
                ),
            ),
        )
        projection = requireNotNull(tracks.projection(trackId))
        val changed = projection.toDraft().copy(
            name = "Should roll back",
            fields = projection.toDraft().fields.map { field ->
                if (field.id == score.id) field.copy(dimension = UnitDimension.Count, unitId = "count") else field
            },
        )
        val boundary = requireNotNull(tracks.definitionBoundary(trackId))

        val failure = runCatching { tracks.update(trackId, changed, boundary) }.exceptionOrNull()

        assertTrue(failure?.message.orEmpty().contains("measurement type"))
        val current = requireNotNull(tracks.projection(trackId))
        assertEquals("Number Track", current.track.name)
        assertEquals(UnitDimension.Unitless, current.fields.single { it.id == score.id }.dimension)
        assertEquals(setOf(entryId), tracks.searchEntryIds(trackId, "Dimension rollback"))
    }

    @Test
    fun scaleChangeRejectsAnIncompatibleDormantConstant() = runBlocking {
        val trackId = tracks.create(scaleDraft())
        val projection = requireNotNull(tracks.projection(trackId))
        val rating = projection.fields.single { it.name == "Rating" }
        val ruleId = insertTriggerRule(trackId)
        database.linkDao().insertTriggerMapping(
            TriggerFieldMappingEntity(
                triggerRuleId = ruleId,
                targetFieldId = rating.id,
                sourceProperty = "Constant",
                constantText = null,
                constantNumber = null,
                constantUnitId = null,
                constantDateEpochDay = null,
                constantBoolean = null,
                constantChoiceOptionId = null,
                constantScale = 3.5,
            ),
        )
        val draft = projection.toDraft().copy(
            fields = projection.toDraft().fields.map { field ->
                if (field.id == rating.id) field.copy(scaleStep = 1.0) else field
            },
        )
        val boundary = requireNotNull(tracks.definitionBoundary(trackId))

        val failure = runCatching { tracks.update(trackId, draft, boundary) }.exceptionOrNull()

        assertTrue(failure?.message.orEmpty().contains("legacy automation value"))
        assertEquals(0.5, requireNotNull(tracks.projection(trackId)).fields.single { it.id == rating.id }.scaleStep, 0.0)
    }

    @Test
    fun typeChangeWithNoHistoryButDormantDependencyIsRejected() = runBlocking {
        val trackId = tracks.create(textFieldDraft())
        val projection = requireNotNull(tracks.projection(trackId))
        val notes = projection.fields.single { it.name == "Notes" }
        val ruleId = insertTriggerRule(trackId)
        database.linkDao().insertTriggerMapping(
            TriggerFieldMappingEntity(
                triggerRuleId = ruleId,
                targetFieldId = notes.id,
                sourceProperty = "Constant",
                constantText = "dormant value",
                constantNumber = null,
                constantUnitId = null,
                constantDateEpochDay = null,
                constantBoolean = null,
                constantChoiceOptionId = null,
                constantScale = null,
            ),
        )
        val changed = projection.toDraft().copy(
            fields = projection.toDraft().fields.map { field ->
                if (field.id == notes.id) field.copy(type = TrackFieldType.YesNo) else field
            },
        )
        val boundary = requireNotNull(tracks.definitionBoundary(trackId))

        val failure = runCatching { tracks.update(trackId, changed, boundary) }.exceptionOrNull()

        assertTrue(failure?.message.orEmpty().contains("legacy automation definitions"))
        assertEquals(
            TrackFieldType.LongText,
            requireNotNull(tracks.projection(trackId)).fields.single { it.id == notes.id }.type,
        )
        assertEquals(notes.id, database.linkDao().getTriggerMappings(ruleId).single().targetFieldId)
    }

    @Test
    fun sameDimensionDefaultUnitCanChangeWithoutRewritingHistory() = runBlocking {
        val replacementUnit = UnitDefinitionEntity(
            id = "alternate-score",
            name = "Alternate score",
            symbol = "as",
            dimension = UnitDimension.Unitless.name,
            toCanonicalFactor = 2.0,
            toCanonicalOffset = 0.0,
            custom = true,
            archived = false,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        database.measurementDao().upsertUnit(replacementUnit)
        val trackId = tracks.create(numberDraft())
        var projection = requireNotNull(tracks.projection(trackId))
        val score = projection.fields.single { it.name == "Score" }
        tracks.addEntry(
            trackId,
            TrackEntryDraft(
                TestClock.today(),
                mapOf(
                    projection.primaryField.uuid to TrackValueDraft(textValue = "Historical unit"),
                    score.uuid to TrackValueDraft(enteredNumber = 4.0, enteredUnitId = "unitless"),
                ),
            ),
        )
        projection = requireNotNull(tracks.projection(trackId))
        val changed = projection.toDraft().copy(
            fields = projection.toDraft().fields.map { field ->
                if (field.id == score.id) field.copy(unitId = replacementUnit.id) else field
            },
        )
        val boundary = requireNotNull(tracks.definitionBoundary(trackId))

        tracks.update(trackId, changed, boundary)

        val current = requireNotNull(tracks.projection(trackId))
        assertEquals(replacementUnit.id, current.fields.single { it.id == score.id }.unitId)
        val historical = requireNotNull(current.entries.single().value(score.id))
        assertEquals("unitless", historical.enteredUnitId)
        assertEquals(4.0, historical.enteredNumber ?: Double.NaN, 0.0)
        assertEquals(4.0, historical.canonicalNumber ?: Double.NaN, 0.0)
    }

    @Test
    fun archivedUnitCanBeRetainedButCannotBeNewlySelected() = runBlocking {
        val unit = UnitDefinitionEntity(
            id = "custom-score",
            name = "Custom score",
            symbol = "cs",
            dimension = UnitDimension.Unitless.name,
            toCanonicalFactor = 1.0,
            toCanonicalOffset = 0.0,
            custom = true,
            archived = false,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        database.measurementDao().upsertUnit(unit)
        val trackId = tracks.create(numberDraft(unit.id))
        database.measurementDao().upsertUnit(unit.copy(archived = true, updatedAtMillis = 2))
        var projection = requireNotNull(tracks.projection(trackId))
        var boundary = requireNotNull(tracks.definitionBoundary(trackId))

        tracks.update(trackId, projection.toDraft().copy(description = "Retained archived unit"), boundary)
        assertEquals("Retained archived unit", requireNotNull(tracks.projection(trackId)).track.description)
        assertTrue(runCatching { tracks.create(numberDraft(unit.id).copy(name = "New archived unit")) }.isFailure)

        projection = requireNotNull(tracks.projection(trackId))
        boundary = requireNotNull(tracks.definitionBoundary(trackId))
        val addField = projection.toDraft().copy(
            fields = projection.toDraft().fields + TrackFieldDraft(
                "New archived selection",
                TrackFieldType.Number,
                dimension = UnitDimension.Unitless,
                unitId = unit.id,
            ),
        )
        assertTrue(runCatching { tracks.update(trackId, addField, boundary) }.isFailure)
        assertNull(requireNotNull(tracks.projection(trackId)).fields.firstOrNull { it.name == "New archived selection" })
    }

    @Test
    fun archivedAreaCanBeRetainedButCannotReceiveANewTrack() = runBlocking {
        val archivedAreaId = areas.create("Archived destination")
        val retainedTrackId = tracks.create(textFieldDraft().copy(name = "Retained", areaId = archivedAreaId))
        areas.setArchived(archivedAreaId, true)
        val retained = requireNotNull(tracks.projection(retainedTrackId))
        val retainedBoundary = requireNotNull(tracks.definitionBoundary(retainedTrackId))
        tracks.update(
            retainedTrackId,
            retained.toDraft().copy(
                description = "Still editable",
                // Legacy drafts may retain only the derived display name.
                areaId = null,
            ),
            retainedBoundary,
        )

        assertTrue(
            runCatching {
                tracks.create(textFieldDraft().copy(name = "Rejected", areaId = archivedAreaId))
            }.isFailure,
        )
        val otherId = tracks.create(textFieldDraft().copy(name = "Other"))
        val other = requireNotNull(tracks.projection(otherId))
        val otherBoundary = requireNotNull(tracks.definitionBoundary(otherId))
        assertTrue(
            runCatching {
                tracks.update(otherId, other.toDraft().copy(areaId = archivedAreaId), otherBoundary)
            }.isFailure,
        )
        assertEquals("Still editable", requireNotNull(tracks.projection(retainedTrackId)).track.description)
        assertTrue(requireNotNull(database.measurementDao().getArea(archivedAreaId)).archived)
    }

    @Test
    fun derivedAreaRenameDoesNotFalseConflictButAreaMoveDoes() = runBlocking {
        val sourceAreaId = areas.create("Training")
        val trackId = tracks.create(textFieldDraft().copy(areaId = sourceAreaId))
        val beforeRename = requireNotNull(tracks.projection(trackId))
        val renameBoundary = requireNotNull(tracks.definitionBoundary(trackId))

        areas.rename(sourceAreaId, "Strength")
        tracks.update(
            trackId,
            beforeRename.toDraft().copy(description = "Saved across a derived-name refresh"),
            renameBoundary,
        )

        val afterRename = requireNotNull(tracks.projection(trackId))
        assertEquals(sourceAreaId, afterRename.track.areaId)
        assertEquals("Strength", afterRename.track.area)
        assertEquals("Saved across a derived-name refresh", afterRename.track.description)

        val moveBoundary = requireNotNull(tracks.definitionBoundary(trackId))
        val staleMoveDraft = afterRename.toDraft().copy(description = "Must not overwrite a move")
        val destinationAreaId = areas.create("Performance")
        areas.moveAssignments(sourceAreaId, destinationAreaId)

        val failure = runCatching { tracks.update(trackId, staleMoveDraft, moveBoundary) }.exceptionOrNull()

        assertTrue(failure is TrackDefinitionConflictException)
        assertEquals(TrackDefinitionConflictKind.DefinitionChanged, (failure as TrackDefinitionConflictException).kind)
        val afterMove = requireNotNull(tracks.projection(trackId))
        assertEquals(destinationAreaId, afterMove.track.areaId)
        assertEquals("Performance", afterMove.track.area)
        assertEquals("Saved across a derived-name refresh", afterMove.track.description)
    }

    private suspend fun addChoiceEntry(
        trackId: Long,
        projection: TrackProjection,
        title: String,
        fieldUuid: String,
        optionUuid: String,
    ) = tracks.addEntry(
        trackId,
        TrackEntryDraft(
            TestClock.today(),
            mapOf(
                projection.primaryField.uuid to TrackValueDraft(textValue = title),
                fieldUuid to TrackValueDraft(choiceOptionUuid = optionUuid),
            ),
        ),
    )

    private suspend fun insertDormantDefinitions(
        trackId: Long,
        fieldId: Long,
        removedOptionId: Long,
        retainedOptionId: Long,
    ): LegacyRows {
        val goalId = database.goalDao().insertGoal(goalEntity())
        val linkRuleId = database.linkDao().insertRule(
            LinkRuleEntity(
                uuid = "legacy-link",
                name = "Dormant Link",
                kind = "Contribution",
                sourceType = "Track",
                sourceEntityId = trackId,
                sourceMetricId = null,
                sourceItemId = null,
                sourceMetric = "FieldValue",
                targetGoalId = goalId,
                targetMilestoneId = null,
                valueMode = "SourceValue",
                fixedValue = null,
                multiplier = 1.0,
                offset = 0.0,
                retroactiveFromEpochDay = null,
                enabled = false,
                createdAtMillis = 1,
                updatedAtMillis = 1,
                trackAggregation = "Latest",
                sourceFieldId = fieldId,
                conditionMode = "MatchAll",
            ),
        )
        val linkConditionId = database.linkDao().insertRuleCondition(
            LinkRuleConditionEntity(
                linkRuleId = linkRuleId,
                fieldId = fieldId,
                entryDate = false,
                operator = "IsOneOf",
                position = 0,
                textValue = null,
                numberValue = null,
                secondNumberValue = null,
                dateEpochDay = null,
                secondDateEpochDay = null,
            ),
        )
        database.linkDao().insertLinkConditionChoice(LinkConditionChoiceEntity(linkConditionId, removedOptionId))
        database.linkDao().insertLinkConditionChoice(LinkConditionChoiceEntity(linkConditionId, retainedOptionId))
        val contributionId = database.linkDao().upsertContribution(
            ContributionEntity(
                uuid = "legacy-contribution",
                linkRuleId = linkRuleId,
                sourceEventId = "historical-event",
                sourceType = "Track",
                sourceEntityId = trackId,
                targetGoalId = goalId,
                metricEntryId = null,
                canonicalValue = 1.0,
                localEpochDay = TestClock.today().toEpochDay(),
                timestampMillis = 1,
                excluded = false,
                overrideValue = null,
                explanation = "Preserved history",
                createdAtMillis = 1,
                updatedAtMillis = 1,
            ),
        )
        val triggerRuleId = insertTriggerRule(trackId)
        val triggerConditionId = database.linkDao().insertTriggerCondition(
            TriggerRuleConditionEntity(
                triggerRuleId = triggerRuleId,
                fieldId = fieldId,
                entryDate = false,
                operator = "IsOneOf",
                position = 0,
                textValue = null,
                numberValue = null,
                secondNumberValue = null,
                dateEpochDay = null,
                secondDateEpochDay = null,
            ),
        )
        database.linkDao().insertTriggerConditionChoice(TriggerConditionChoiceEntity(triggerConditionId, removedOptionId))
        database.linkDao().insertTriggerConditionChoice(TriggerConditionChoiceEntity(triggerConditionId, retainedOptionId))
        database.linkDao().insertTriggerMapping(
            TriggerFieldMappingEntity(
                triggerRuleId = triggerRuleId,
                targetFieldId = fieldId,
                sourceProperty = "Constant",
                constantText = null,
                constantNumber = null,
                constantUnitId = null,
                constantDateEpochDay = null,
                constantBoolean = null,
                constantChoiceOptionId = removedOptionId,
                constantScale = null,
            ),
        )
        val occurrenceId = database.linkDao().upsertTriggerOccurrence(
            TriggerOccurrenceEntity(
                triggerRuleId = triggerRuleId,
                sourceEventId = "legacy-choice-event",
                availableAtMillis = 1,
                deliveredAtMillis = 2,
                dismissedAtMillis = null,
                remindAtMillis = null,
                fulfilledEntryId = null,
                sourceSnapshot = "{\"kind\":\"choice\"}",
            ),
        )
        return LegacyRows(
            linkRuleId,
            linkConditionId,
            triggerRuleId,
            triggerConditionId,
            contributionId,
            occurrenceId,
        )
    }

    private suspend fun insertDormantFieldDefinitions(
        trackId: Long,
        fieldId: Long,
        fulfilledEntryId: Long,
    ): FieldLegacyRows {
        val goalId = database.goalDao().insertGoal(goalEntity())
        val linkRuleId = database.linkDao().insertRule(
            LinkRuleEntity(
                uuid = "field-link-${nextFixtureId.incrementAndGet()}",
                name = "Dormant Field Link",
                kind = "Contribution",
                sourceType = "Track",
                sourceEntityId = trackId,
                sourceMetricId = null,
                sourceItemId = null,
                sourceMetric = "FieldValue",
                targetGoalId = goalId,
                targetMilestoneId = null,
                valueMode = "SourceValue",
                fixedValue = null,
                multiplier = 1.0,
                offset = 0.0,
                retroactiveFromEpochDay = null,
                enabled = false,
                createdAtMillis = 1,
                updatedAtMillis = 1,
                trackAggregation = "Latest",
                sourceFieldId = fieldId,
                conditionMode = "MatchAll",
            ),
        )
        database.linkDao().insertRuleCondition(
            LinkRuleConditionEntity(
                linkRuleId = linkRuleId,
                fieldId = fieldId,
                entryDate = false,
                operator = "HasValue",
                position = 0,
                textValue = null,
                numberValue = null,
                secondNumberValue = null,
                dateEpochDay = null,
                secondDateEpochDay = null,
            ),
        )
        val contributionId = database.linkDao().upsertContribution(
            ContributionEntity(
                uuid = "field-contribution-${nextFixtureId.incrementAndGet()}",
                linkRuleId = linkRuleId,
                sourceEventId = "field-history-event",
                sourceType = "Track",
                sourceEntityId = trackId,
                targetGoalId = goalId,
                metricEntryId = null,
                canonicalValue = 1.0,
                localEpochDay = TestClock.today().toEpochDay(),
                timestampMillis = 1,
                excluded = false,
                overrideValue = null,
                explanation = "Preserved Field history",
                createdAtMillis = 1,
                updatedAtMillis = 1,
            ),
        )
        val triggerRuleId = insertTriggerRule(trackId)
        database.linkDao().insertTriggerCondition(
            TriggerRuleConditionEntity(
                triggerRuleId = triggerRuleId,
                fieldId = fieldId,
                entryDate = false,
                operator = "HasValue",
                position = 0,
                textValue = null,
                numberValue = null,
                secondNumberValue = null,
                dateEpochDay = null,
                secondDateEpochDay = null,
            ),
        )
        database.linkDao().insertTriggerMapping(
            TriggerFieldMappingEntity(
                triggerRuleId = triggerRuleId,
                targetFieldId = fieldId,
                sourceProperty = "Constant",
                constantText = "dormant mapping",
                constantNumber = null,
                constantUnitId = null,
                constantDateEpochDay = null,
                constantBoolean = null,
                constantChoiceOptionId = null,
                constantScale = null,
            ),
        )
        val occurrenceId = database.linkDao().upsertTriggerOccurrence(
            TriggerOccurrenceEntity(
                triggerRuleId = triggerRuleId,
                sourceEventId = "field-trigger-history",
                availableAtMillis = 1,
                deliveredAtMillis = 2,
                dismissedAtMillis = null,
                remindAtMillis = null,
                fulfilledEntryId = fulfilledEntryId,
                sourceSnapshot = "{\"kind\":\"field\"}",
            ),
        )
        return FieldLegacyRows(linkRuleId, triggerRuleId, contributionId, occurrenceId)
    }

    private suspend fun insertTriggerRule(trackId: Long): Long = database.linkDao().insertTriggerRule(
        TriggerRuleEntity(
            uuid = "trigger-${nextFixtureId.incrementAndGet()}",
            name = "Dormant Trigger",
            sourceType = "Track",
            sourceEntityId = trackId,
            sourceItemId = null,
            outcome = "Completed",
            targetType = "Track",
            targetEntityId = trackId,
            delayMinutes = 0,
            quietStartMinutes = null,
            quietEndMinutes = null,
            action = "PromptTrackEntry",
            notificationEnabled = false,
            conditionMode = "MatchAll",
            enabled = false,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        ),
    )

    private fun goalEntity() = GoalEntity(
        uuid = "goal-${nextFixtureId.incrementAndGet()}",
        metricId = "metric-${nextFixtureId.incrementAndGet()}",
        name = "History target",
        description = "",
        areaId = "whip-default-main",
        area = "Main",
        tagsCsv = "",
        icon = "🎯",
        type = "ReachValue",
        dimension = "Count",
        unitId = "count",
        precision = 0,
        baseline = 0.0,
        targetMin = 10.0,
        targetMax = null,
        direction = "Increase",
        startEpochDay = TestClock.today().toEpochDay(),
        deadlineEpochDay = null,
        aggregation = "Sum",
        aggregationPeriod = "All",
        rollingDays = null,
        paceType = "None",
        consistencyPeriod = "Week",
        consistencyRequiredPeriods = null,
        elapsedStartMillis = null,
        elapsedDisplayUnit = "Auto",
        reminderMinutes = null,
        status = "Active",
        pinned = false,
        position = 0,
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )

    private fun choiceDraft() = TrackDraft(
        name = "Choice Track",
        fields = listOf(
            TrackFieldDraft("Title", TrackFieldType.ShortText, required = true, primary = true),
            TrackFieldDraft(
                "Genre",
                TrackFieldType.SingleChoice,
                options = listOf(TrackChoiceOptionDraft("History"), TrackChoiceOptionDraft("Fiction")),
            ),
        ),
    )

    private fun textFieldDraft() = TrackDraft(
        name = "Text Track",
        fields = listOf(
            TrackFieldDraft("Title", TrackFieldType.ShortText, required = true, primary = true),
            TrackFieldDraft("Notes", TrackFieldType.LongText),
        ),
    )

    private fun numberDraft(unitId: String = "unitless") = TrackDraft(
        name = "Number Track",
        fields = listOf(
            TrackFieldDraft("Title", TrackFieldType.ShortText, required = true, primary = true),
            TrackFieldDraft("Score", TrackFieldType.Number, dimension = UnitDimension.Unitless, unitId = unitId),
        ),
    )

    private fun scaleDraft() = TrackDraft(
        name = "Scale Track",
        fields = listOf(
            TrackFieldDraft("Title", TrackFieldType.ShortText, required = true, primary = true),
            TrackFieldDraft("Rating", TrackFieldType.Scale, scaleMin = 1, scaleMax = 5, scaleStep = 0.5),
        ),
    )

    private fun TrackProjection.toDraft() = TrackDraft(
        name = track.name,
        description = track.description,
        icon = track.icon,
        areaId = track.areaId,
        area = track.area,
        tags = track.tags,
        fields = fields.map { field ->
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
                options = optionsFor(field.id).map { TrackChoiceOptionDraft(it.label, it.uuid, it.id) },
                uuid = field.uuid,
                id = field.id,
            )
        },
    )

    private fun TrackDraft.withoutOption(optionId: Long) = copy(
        fields = fields.map { field -> field.copy(options = field.options.filterNot { it.id == optionId }) },
    )

    private data class LegacyRows(
        val linkRuleId: Long,
        val linkConditionId: Long,
        val triggerRuleId: Long,
        val triggerConditionId: Long,
        val contributionId: Long,
        val occurrenceId: Long,
    )

    private data class FieldLegacyRows(
        val linkRuleId: Long,
        val triggerRuleId: Long,
        val contributionId: Long,
        val occurrenceId: Long,
    )

    private object TestClock : WhipClock {
        override fun now(): Instant = Instant.parse("2026-09-01T12:00:00Z")
        override fun zoneId(): ZoneId = ZoneId.of("UTC")
        override fun today(zoneId: ZoneId): LocalDate = LocalDate.of(2026, 9, 1)
    }

    private class TestIds : WhipIdGenerator {
        private val next = AtomicInteger()
        override fun nextId(): String = "track-definition-${next.incrementAndGet()}"
    }

    private companion object {
        val nextFixtureId = AtomicInteger()
    }
}
