package com.whip.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.data.RoomAreaRepository
import com.whip.app.data.RoomTrackRepository
import com.whip.app.data.WhipDatabase
import com.whip.app.data.buildTrackCsv
import com.whip.app.data.requireTrackCsvExportWithinLimit
import com.whip.app.domain.TrackAggregation
import com.whip.app.domain.TrackChoiceOptionDraft
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackValueDraft
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.aggregate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackRepositoryTest {
    private lateinit var database: WhipDatabase
    private lateinit var tracks: RoomTrackRepository

    @Before fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), WhipDatabase::class.java)
            .addCallback(WhipDatabase.integrityGuardCallback)
            .build()
        val ids = TestIds()
        RoomAreaRepository(database, TestClock, ids).ensureDefaultArea()
        tracks = RoomTrackRepository(database, TestClock, ids)
    }

    @After fun tearDown() = database.close()

    @Test fun projectionsNeverExposeATrackBeforeItsRequiredFields() = runBlocking {
        var partialProjectionObserved = false
        val collector = launch {
            tracks.projections.collect { rows ->
                if (rows.any { it.fields.isEmpty() }) partialProjectionObserved = true
            }
        }
        yield()

        val id = tracks.create(booksDraft())
        tracks.projections.first { rows -> rows.any { it.track.id == id } }
        collector.cancelAndJoin()

        assertFalse(partialProjectionObserved)
    }

    @Test fun reorderNormalizesOmittedArchivedTracksWithoutDuplicatePositions() = runBlocking {
        val area = database.measurementDao().observeAreasSnapshot().first().id
        fun draft(name: String) = booksDraft().copy(name = name, areaId = area)
        val first = tracks.create(draft("First"))
        val second = tracks.create(draft("Second"))
        val archived = tracks.create(draft("Archived"))
        tracks.setArchived(archived, true)

        tracks.reorder(listOf(second, first))

        val stored = database.trackDao().getAllTracks()
        assertEquals(stored.size, stored.map { it.position }.distinct().size)
        assertEquals(listOf(second, first), stored.sortedBy { it.position }.take(2).map { it.id })
    }

    @Test fun identityOnlyEditSkipsSchemaInvalidation() = runBlocking {
        val id = tracks.create(booksDraft())
        val before = requireNotNull(tracks.projection(id))
        val entryId = tracks.addEntry(
            id,
            TrackEntryDraft(
                TestClock.today(),
                mapOf(before.primaryField.uuid to TrackValueDraft(textValue = "Fast save")),
            ),
        )

        val boundary = requireNotNull(tracks.definitionBoundary(id))
        val impact = tracks.update(id, before.toDraft().copy(icon = "⚡"), boundary)

        assertFalse(impact.schemaChanged)
        assertEquals("⚡", requireNotNull(tracks.projection(id)).track.icon)
        assertEquals(setOf(entryId), tracks.searchEntryIds(id, "Fast save"))
    }

    @Test fun fractionalScaleRoundTripsThroughStorageCsvAnalyticsAndSchemaEdits() = runBlocking {
        val trackId = tracks.create(
            TrackDraft(
                name = "Films",
                fields = listOf(
                    TrackFieldDraft("Title", TrackFieldType.ShortText, primary = true),
                    TrackFieldDraft(
                        "Rating",
                        TrackFieldType.Scale,
                        showInList = true,
                        scaleMin = 1,
                        scaleMax = 5,
                        scaleStep = 0.5,
                        scaleLowLabel = "Poor",
                        scaleHighLabel = "Excellent",
                    ),
                ),
            ),
        )
        var projection = requireNotNull(tracks.projection(trackId))
        val rating = projection.fields.single { it.name == "Rating" }
        tracks.addEntry(
            trackId,
            TrackEntryDraft(
                entryDate = TestClock.today(),
                values = mapOf(
                    projection.primaryField.uuid to TrackValueDraft(textValue = "Arrival"),
                    rating.uuid to TrackValueDraft(scaleValue = 3.5),
                ),
            ),
        )

        projection = requireNotNull(tracks.projection(trackId))
        val storedRating = projection.fields.single { it.name == "Rating" }
        assertEquals(0.5, storedRating.scaleStep, 0.0)
        assertEquals(3.5, projection.entries.single().value(storedRating.id)?.scaleValue ?: 0.0, 0.0)
        assertEquals(3.5, projection.aggregate(TrackAggregation.Average, storedRating.uuid).value ?: 0.0, 0.0)
        assertTrue(tracks.exportCsv(trackId).contains("\"3.5\""))

        val integerOnly = projection.toDraft().copy(
            fields = projection.toDraft().fields.map { field ->
                if (field.id == storedRating.id) field.copy(scaleStep = 1.0) else field
            },
        )
        var boundary = requireNotNull(tracks.definitionBoundary(trackId))
        val rejected = runCatching { tracks.update(trackId, integerOnly, boundary) }
        assertTrue(rejected.isFailure)
        assertTrue(rejected.exceptionOrNull()?.message.orEmpty().contains("existing Scale value"))

        val quarterPoint = integerOnly.copy(
            fields = integerOnly.fields.map { field ->
                if (field.id == storedRating.id) field.copy(scaleStep = 0.25) else field
            },
        )
        boundary = requireNotNull(tracks.definitionBoundary(trackId))
        tracks.update(trackId, quarterPoint, boundary)
        assertEquals(0.25, requireNotNull(tracks.projection(trackId)).fields.single { it.id == storedRating.id }.scaleStep, 0.0)
    }

    @Test fun crudSearchCsvAndDestructiveChoiceConfirmationPreserveIdentity() = runBlocking {
        val id = tracks.create(booksDraft())
        var projection = requireNotNull(tracks.projection(id))
        val title = projection.primaryField
        val genre = projection.fields.first { it.name == "Genre" }
        val rating = projection.fields.first { it.name == "Rating" }
        val history = projection.optionsFor(genre.id).first { it.label == "History" }
        val entryId = tracks.addEntry(
            id,
            TrackEntryDraft(
                TestClock.today(),
                mapOf(
                    title.uuid to TrackValueDraft(textValue = "The Dispossessed, annotated"),
                    genre.uuid to TrackValueDraft(choiceOptionUuid = history.uuid),
                    rating.uuid to TrackValueDraft(enteredNumber = 4.5, enteredUnitId = "unitless"),
                ),
            ),
        )
        assertEquals(setOf(entryId), tracks.searchEntryIds(id, "Dispossessed annotated"))
        assertTrue(tracks.exportCsv(id).contains("\"The Dispossessed, annotated\""))

        projection = requireNotNull(tracks.projection(id))
        val existingDraft = projection.toDraft()
        val draftWithoutHistory = existingDraft.copy(
            fields = existingDraft.fields.map { field ->
                if (field.id == genre.id) field.copy(options = field.options.filterNot { it.id == history.id }) else field
            },
        )
        val boundary = requireNotNull(tracks.definitionBoundary(id))
        val rejected = runCatching { tracks.update(id, draftWithoutHistory, boundary) }
        assertTrue(rejected.isFailure)
        assertTrue(rejected.exceptionOrNull()?.message.orEmpty().contains("Review"))
        val review = tracks.reviewDefinitionUpdate(id, draftWithoutHistory, boundary)
        assertEquals(1, review.removedChoices.single().savedValueCount)
        tracks.update(id, draftWithoutHistory, boundary, review)
        projection = requireNotNull(tracks.projection(id))
        assertEquals("The Dispossessed, annotated", projection.primaryText(projection.entries.single()))
        assertEquals(null, projection.entries.single().value(genre.id))

        val deleted = requireNotNull(tracks.deleteEntry(entryId))
        assertTrue(requireNotNull(tracks.projection(id)).entries.isEmpty())
        val restored = tracks.restoreEntryId(deleted)
        assertNotNull(tracks.projection(id)?.entries?.firstOrNull { it.entry.id == restored })
    }

    @Test fun compositeEntryIdentityAndInlineLabelRoundTripWithoutSilentClearing() = runBlocking {
        val trackId = tracks.create(
            TrackDraft(
                name = "Films",
                fields = listOf(
                    TrackFieldDraft("Title", TrackFieldType.ShortText, primary = true),
                    TrackFieldDraft("Director", TrackFieldType.ShortText, primary = true, showInList = true),
                    TrackFieldDraft("Year", TrackFieldType.Number, primary = true, showInList = true, dimension = UnitDimension.Count, unitId = "count", precision = 0),
                ),
            ),
        )
        var projection = requireNotNull(tracks.projection(trackId))
        val title = projection.fields.first { it.name == "Title" }
        val director = projection.fields.first { it.name == "Director" }
        val year = projection.fields.first { it.name == "Year" }
        val entryId = tracks.addEntry(
            trackId,
            TrackEntryDraft(
                TestClock.today(),
                mapOf(
                    title.uuid to TrackValueDraft(textValue = "Dune"),
                    director.uuid to TrackValueDraft(textValue = "Denis Villeneuve"),
                    year.uuid to TrackValueDraft(enteredNumber = 2021.0, enteredUnitId = "count"),
                ),
            ),
        )
        projection = requireNotNull(tracks.projection(trackId))
        assertEquals(3, projection.primaryFields.size)
        assertEquals(2, projection.fields.count { it.showInList })
        assertEquals("Dune · Denis Villeneuve · 2021 count", projection.primaryText(projection.entries.single { it.entry.id == entryId }))

        val boundary = requireNotNull(tracks.definitionBoundary(trackId))
        tracks.update(trackId, projection.toDraft(), boundary)
        projection = requireNotNull(tracks.projection(trackId))
        assertEquals(3, projection.primaryFields.size)
        assertTrue(projection.fields.first { it.name == "Director" }.showInList)
        assertTrue(projection.fields.first { it.name == "Year" }.showInList)
    }

    @Test fun replacingAChoiceRetargetsEntriesWithoutChangingTheirIdentity() = runBlocking {
        val trackId = tracks.create(booksDraft())
        val projection = requireNotNull(tracks.projection(trackId))
        val genre = projection.fields.first { it.name == "Genre" }
        val history = projection.optionsFor(genre.id).first { it.label == "History" }
        val fiction = projection.optionsFor(genre.id).first { it.label == "Fiction" }
        tracks.addEntry(
            trackId,
            TrackEntryDraft(
                TestClock.today(),
                mapOf(
                    projection.primaryField.uuid to TrackValueDraft(textValue = "A Choice in Motion"),
                    genre.uuid to TrackValueDraft(choiceOptionUuid = history.uuid),
                ),
            ),
        )
        val withoutHistory = projection.toDraft().copy(
            fields = projection.toDraft().fields.map { field ->
                if (field.id == genre.id) field.copy(options = field.options.filterNot { it.id == history.id }) else field
            },
        )

        val boundary = requireNotNull(tracks.definitionBoundary(trackId))
        val review = tracks.reviewDefinitionUpdate(
            trackId,
            withoutHistory,
            boundary,
            choiceReplacementIds = mapOf(history.id to fiction.id),
        )
        tracks.update(trackId, withoutHistory, boundary, review)

        val updated = requireNotNull(tracks.projection(trackId))
        assertEquals(fiction.id, updated.entries.single().value(genre.id)?.choiceOptionId)
    }

    @Test fun largeTrackHistoryIsReadThroughStableBoundedRepositoryPages() = runBlocking {
        val trackId = tracks.create(booksDraft())
        val projection = requireNotNull(tracks.projection(trackId))
        repeat(205) { index ->
            tracks.addEntry(
                trackId,
                TrackEntryDraft(
                    entryDate = TestClock.today().minusDays((index % 31).toLong()),
                    values = mapOf(projection.primaryField.uuid to TrackValueDraft(textValue = "Book $index")),
                ),
            )
        }

        val first = tracks.entryPage(trackId, offset = 0, limit = 100)
        val second = tracks.entryPage(trackId, offset = first.nextOffset, limit = 100)
        val third = tracks.entryPage(trackId, offset = second.nextOffset, limit = 100)

        assertEquals(205, first.totalCount)
        assertEquals(100, first.entries.size)
        assertEquals(100, second.entries.size)
        assertEquals(5, third.entries.size)
        assertTrue(first.hasMore)
        assertTrue(second.hasMore)
        assertFalse(third.hasMore)
        val allIds = (first.entries + second.entries + third.entries).map { it.entry.id }
        assertEquals(205, allIds.distinct().size)
    }

    @Test fun duplicatePreservesCompleteTypedDefinitionWithFreshIdentitiesAndNoHistory() = runBlocking {
        val sourceId = tracks.create(allTypesDraft())
        val source = requireNotNull(tracks.projection(sourceId))
        val byName = source.fields.associateBy { it.name }
        val selected = source.optionsFor(requireNotNull(byName["Status"]).id).single { it.label.startsWith("Ready") }
        tracks.addEntry(
            sourceId,
            TrackEntryDraft(
                entryDate = TestClock.today(),
                values = mapOf(
                    requireNotNull(byName["Title"]).uuid to TrackValueDraft(textValue = "Do not copy history"),
                    requireNotNull(byName["Notes"]).uuid to TrackValueDraft(textValue = "Private notes"),
                    requireNotNull(byName["Amount"]).uuid to TrackValueDraft(enteredNumber = 2.5),
                    requireNotNull(byName["Status"]).uuid to TrackValueDraft(choiceOptionUuid = selected.uuid),
                    requireNotNull(byName["Rating"]).uuid to TrackValueDraft(scaleValue = 3.5),
                    requireNotNull(byName["When"]).uuid to TrackValueDraft(dateValue = LocalDate.of(2026, 8, 22)),
                    requireNotNull(byName["Confirmed"]).uuid to TrackValueDraft(booleanValue = true),
                ),
            ),
        )

        val duplicateId = tracks.duplicate(sourceId)
        val duplicate = requireNotNull(tracks.projection(duplicateId))

        assertEquals("${source.track.name} Copy", duplicate.track.name)
        assertEquals(source.track.description, duplicate.track.description)
        assertEquals(source.track.icon, duplicate.track.icon)
        assertEquals(source.track.areaId, duplicate.track.areaId)
        assertEquals(source.track.tags, duplicate.track.tags)
        assertTrue(duplicate.entries.isEmpty())
        assertNotEquals(source.track.uuid, duplicate.track.uuid)
        assertEquals(source.fields.map { it.name }, duplicate.fields.map { it.name })
        source.fields.zip(duplicate.fields).forEach { (before, after) ->
            assertNotEquals(before.id, after.id)
            assertNotEquals(before.uuid, after.uuid)
            assertEquals(before.type, after.type)
            assertEquals(before.required, after.required)
            assertEquals(before.primary, after.primary)
            assertEquals(before.showInList, after.showInList)
            assertEquals(before.dimension, after.dimension)
            assertEquals(before.unitId, after.unitId)
            assertEquals(before.precision, after.precision)
            assertEquals(before.scaleMin, after.scaleMin)
            assertEquals(before.scaleMax, after.scaleMax)
            assertEquals(before.scaleStep, after.scaleStep, 0.0)
            assertEquals(before.scaleLowLabel, after.scaleLowLabel)
            assertEquals(before.scaleHighLabel, after.scaleHighLabel)
            val beforeOptions = source.optionsFor(before.id)
            val afterOptions = duplicate.optionsFor(after.id)
            assertEquals(beforeOptions.map { it.label }, afterOptions.map { it.label })
            beforeOptions.zip(afterOptions).forEach { (beforeOption, afterOption) ->
                assertNotEquals(beforeOption.id, afterOption.id)
                assertNotEquals(beforeOption.uuid, afterOption.uuid)
                assertEquals(beforeOption.position, afterOption.position)
            }
        }
    }

    @Test fun csvExportEscapesAndFormatsEveryValueTypeAtItsExactUtf8Boundary() = runBlocking {
        val trackId = tracks.create(allTypesDraft().copy(name = "CSV all types"))
        val projection = requireNotNull(tracks.projection(trackId))
        val fields = projection.fields.associateBy { it.name }
        val selected = projection.optionsFor(requireNotNull(fields["Status"]).id).single { it.label == "Ready, \"now\"" }
        tracks.addEntry(
            trackId,
            TrackEntryDraft(
                entryDate = TestClock.today(),
                values = mapOf(
                    requireNotNull(fields["Title"]).uuid to TrackValueDraft(textValue = "Comma, quote \" and emoji 😀"),
                    requireNotNull(fields["Notes"]).uuid to TrackValueDraft(textValue = "café · 中"),
                    requireNotNull(fields["Amount"]).uuid to TrackValueDraft(enteredNumber = 2.5),
                    requireNotNull(fields["Status"]).uuid to TrackValueDraft(choiceOptionUuid = selected.uuid),
                    requireNotNull(fields["Rating"]).uuid to TrackValueDraft(scaleValue = 4.5),
                    requireNotNull(fields["When"]).uuid to TrackValueDraft(dateValue = LocalDate.of(2026, 8, 20)),
                    requireNotNull(fields["Confirmed"]).uuid to TrackValueDraft(booleanValue = false),
                ),
            ),
        )
        val stored = requireNotNull(tracks.projection(trackId))
        val csv = buildTrackCsv(stored)

        assertTrue(csv.contains("\"Comma, quote \"\" and emoji 😀\""))
        assertTrue(csv.contains("\"café · 中\""))
        assertTrue(csv.contains("\"Ready, \"\"now\"\"\""))
        assertTrue(csv.contains("\"2.5\",\"count\",\"2.5\""))
        assertTrue(csv.contains("\"4.5\""))
        assertTrue(csv.contains("\"2026-08-20\""))
        assertTrue(csv.contains("\"No\""))
        val exactBytes = csv.encodeToByteArray().size
        assertEquals(csv, buildTrackCsv(stored, maxBytes = exactBytes))
        assertTrue(runCatching { buildTrackCsv(stored, maxBytes = exactBytes - 1) }.isFailure)
        assertEquals(csv, tracks.exportCsv(trackId))
    }

    @Test fun csvSizeGuardCountsAsciiTwoByteThreeByteAndSurrogateUtf8Exactly() = runBlocking {
        listOf(
            "A" to 1,
            "é" to 2,
            "中" to 3,
            "😀" to 4,
            "Aé中😀" to 10,
        ).forEach { (value, expectedBytes) ->
            requireTrackCsvExportWithinLimit(value, maxBytes = expectedBytes)
            assertTrue(
                "Expected $value to exceed ${expectedBytes - 1} bytes",
                runCatching {
                    requireTrackCsvExportWithinLimit(value, maxBytes = expectedBytes - 1)
                }.exceptionOrNull() is IllegalArgumentException,
            )
        }
    }

    private fun booksDraft() = TrackDraft(
        name = "Books Read",
        fields = listOf(
            TrackFieldDraft("Title", TrackFieldType.ShortText, required = true, primary = true),
            TrackFieldDraft("Genre", TrackFieldType.SingleChoice, options = listOf(TrackChoiceOptionDraft("History"), TrackChoiceOptionDraft("Fiction"))),
            TrackFieldDraft("Rating", TrackFieldType.Number, dimension = UnitDimension.Unitless, unitId = "unitless"),
        ),
    )

    private fun allTypesDraft() = TrackDraft(
        name = "Complete Track",
        description = "Every first-class Field type",
        icon = "🧪",
        tags = listOf("complete", "typed"),
        fields = listOf(
            TrackFieldDraft("Title", TrackFieldType.ShortText, required = true, primary = true),
            TrackFieldDraft("Notes", TrackFieldType.LongText, showInList = true),
            TrackFieldDraft(
                "Amount",
                TrackFieldType.Number,
                showInList = true,
                dimension = UnitDimension.Count,
                unitId = "count",
                precision = 1,
            ),
            TrackFieldDraft(
                "Status",
                TrackFieldType.SingleChoice,
                options = listOf(TrackChoiceOptionDraft("Ready, \"now\""), TrackChoiceOptionDraft("Waiting")),
            ),
            TrackFieldDraft(
                "Rating",
                TrackFieldType.Scale,
                scaleMin = 1,
                scaleMax = 5,
                scaleStep = 0.5,
                scaleLowLabel = "Low",
                scaleHighLabel = "High",
            ),
            TrackFieldDraft("When", TrackFieldType.Date),
            TrackFieldDraft("Confirmed", TrackFieldType.YesNo),
        ),
    )

    private fun com.whip.app.domain.TrackProjection.toDraft() = TrackDraft(
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

    private object TestClock : WhipClock {
        override fun now(): Instant = Instant.parse("2026-08-23T12:00:00Z")
        override fun zoneId(): ZoneId = ZoneId.of("UTC")
        override fun today(zoneId: ZoneId): LocalDate = LocalDate.of(2026, 8, 23)
    }

    private class TestIds : WhipIdGenerator {
        private val next = AtomicInteger()
        override fun nextId(): String = "track-test-${next.incrementAndGet()}"
    }
}
