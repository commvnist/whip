package com.whip.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.data.RoomAreaRepository
import com.whip.app.data.RoomTrackRepository
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
                // Connected drafts may retain only the derived display name.
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

    private object TestClock : WhipClock {
        override fun now(): Instant = Instant.parse("2026-09-01T12:00:00Z")
        override fun zoneId(): ZoneId = ZoneId.of("UTC")
        override fun today(zoneId: ZoneId): LocalDate = LocalDate.of(2026, 9, 1)
    }

    private class TestIds : WhipIdGenerator {
        private val next = AtomicInteger()
        override fun nextId(): String = "track-definition-${next.incrementAndGet()}"
    }
}
