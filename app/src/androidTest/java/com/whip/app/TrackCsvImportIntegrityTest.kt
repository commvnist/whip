package com.whip.app

import androidx.room.Room
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.data.RoomAreaRepository
import com.whip.app.data.RoomTrackRepository
import com.whip.app.data.TrackEntryEntity
import com.whip.app.data.UnitDefinitionEntity
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.TRACK_CSV_MAX_IMPORT_ROWS
import com.whip.app.domain.TrackChoiceOptionDraft
import com.whip.app.domain.TrackCsvImportConflictException
import com.whip.app.domain.TrackCsvImportConflictKind
import com.whip.app.domain.TrackCsvImportPreparation
import com.whip.app.domain.TrackCsvImportReceiptVerification
import com.whip.app.domain.TrackCsvMapping
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackProjection
import com.whip.app.domain.TrackValueDraft
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.trackCsvPayloadFingerprint
import com.whip.app.domain.receiptEnvelope
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackCsvImportIntegrityTest {
    private lateinit var database: WhipDatabase
    private lateinit var tracks: RoomTrackRepository

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WhipDatabase::class.java,
        ).addCallback(WhipDatabase.integrityGuardCallback).build()
        val ids = CsvIds()
        RoomAreaRepository(database, CsvClock, ids).ensureDefaultArea()
        tracks = RoomTrackRepository(database, CsvClock, ids)
    }

    @After fun tearDown() = database.close()

    @Test
    fun firstCommitAndExactRetryReturnOneDurableReceiptAndOneBatchOfRows() = runBlocking {
        val trackId = tracks.create(textTrack())
        val batch = prepareTextBatch(
            trackId,
            "145f61cd-abbe-4817-bc58-32ae84c31f34",
            listOf("  Exact first  ", "Exact second"),
        )

        val first = tracks.importEntries(batch.preparation.request, batch.drafts)
        val retryDrafts = batch.drafts.map { draft ->
            draft.copy(values = draft.values.mapValues { (_, value) -> value.copy(textValue = value.textValue?.trim()) })
        }
        val retry = tracks.importEntries(batch.preparation.request, retryDrafts)

        assertTrue(first.changed)
        assertFalse(first.alreadyApplied)
        assertFalse(retry.changed)
        assertTrue(retry.alreadyApplied)
        assertEquals(first.batchUuid, retry.batchUuid)
        assertEquals(first.entryIdentityDigest, retry.entryIdentityDigest)
        assertEquals(2, count("SELECT COUNT(*) FROM track_entries WHERE trackId = $trackId"))
        assertEquals(1, receiptCount(first.batchUuid))
        val verification = tracks.verifyCsvImportReceipt(first.receiptEnvelope())
        assertTrue(verification is TrackCsvImportReceiptVerification.Exact)
        assertTrue((verification as TrackCsvImportReceiptVerification.Exact).receipt.alreadyApplied)
    }

    @Test
    fun concurrentExactRetrySerializesToOneCommitWithoutDuplicateRows() = runBlocking {
        val trackId = tracks.create(textTrack())
        val batch = prepareTextBatch(
            trackId,
            "804618ab-d3ee-4b90-99d3-cbe3eed5f3b1",
            listOf("Concurrent A", "Concurrent B", "Concurrent C"),
        )

        val secondRepository = RoomTrackRepository(database, CsvClock, CsvIds())
        val receipts = listOf(tracks, secondRepository).map { repository ->
            async(Dispatchers.Default) { repository.importEntries(batch.preparation.request, batch.drafts) }
        }.awaitAll()

        assertEquals(1, receipts.count { it.changed })
        assertEquals(1, receipts.count { it.alreadyApplied })
        assertEquals(setOf(batch.preparation.request.entryIdentityDigest), receipts.mapTo(mutableSetOf()) { it.entryIdentityDigest })
        assertEquals(3, count("SELECT COUNT(*) FROM track_entries WHERE trackId = $trackId"))
        assertEquals(1, receiptCount(batch.preparation.request.batchUuid))
    }

    @Test
    fun reusedBatchWithChangedPayloadOrRowsRejectsButFreshBatchMayDuplicateContent() = runBlocking {
        val trackId = tracks.create(textTrack())
        val first = prepareTextBatch(
            trackId,
            "aee8b5e2-21a6-41dd-9666-eac5dd03317f",
            listOf("Same visible fact"),
        )
        tracks.importEntries(first.preparation.request, first.drafts)
        val changed = prepareTextBatch(
            trackId,
            first.preparation.request.batchUuid,
            listOf("Changed fact"),
        )

        assertCsvConflict(
            TrackCsvImportConflictKind.BatchIdentityCollision,
            runCatching { tracks.importEntries(changed.preparation.request, changed.drafts) }.exceptionOrNull(),
        )
        assertEquals(1, count("SELECT COUNT(*) FROM track_entries WHERE trackId = $trackId"))

        val intentionalDuplicate = prepareTextBatch(
            trackId,
            "b343cf90-a328-49d0-aea6-daf62c0d8854",
            listOf("Same visible fact"),
        )
        tracks.importEntries(intentionalDuplicate.preparation.request, intentionalDuplicate.drafts)

        assertNotEquals(first.preparation.request.entryUuids.single(), intentionalDuplicate.preparation.request.entryUuids.single())
        assertEquals(2, requireNotNull(tracks.projection(trackId)).entries.size)
        assertEquals(2, count("SELECT COUNT(*) FROM track_csv_import_receipts WHERE trackId = $trackId"))
    }

    @Test
    fun identicalRowsInOneBatchKeepDistinctDeterministicIdentities() = runBlocking {
        val trackId = tracks.create(textTrack())
        val batch = prepareTextBatch(
            trackId,
            "13116120-3199-4274-9e36-3e83e91bd3cd",
            listOf("Intentional repeat", "Intentional repeat"),
        )

        assertEquals(2, batch.preparation.request.entryUuids.distinct().size)
        tracks.importEntries(batch.preparation.request, batch.drafts)

        val rows = database.trackDao().getEntriesByUuids(batch.preparation.request.entryUuids)
        assertEquals(2, rows.size)
        assertEquals(2, rows.map(TrackEntryEntity::id).distinct().size)
        assertEquals(2, requireNotNull(tracks.projection(trackId)).entries.size)
    }

    @Test
    fun retryAfterEditDeleteAndAllDeleteNeverRewritesOrResurrectsCommittedHistory() = runBlocking {
        val trackId = tracks.create(textTrack())
        val batch = prepareTextBatch(
            trackId,
            "9141edfe-b813-42d5-9f00-8708ac9f08fd",
            listOf("Edit after import", "Delete after import"),
        )
        tracks.importEntries(batch.preparation.request, batch.drafts)
        val imported = batch.preparation.request.entryUuids.map { requireNotNull(database.trackDao().getEntryByUuid(it)) }
        val firstEdit = requireNotNull(tracks.prepareEntryEdit(imported[0].id))
        val titleUuid = firstEdit.form.fields.single { it.primary }.uuid
        tracks.updateEntry(
            firstEdit.boundary,
            firstEdit.draft.copy(values = mapOf(titleUuid to TrackValueDraft(textValue = "User edit wins"))),
        )
        tracks.deleteEntry(requireNotNull(tracks.prepareEntryEdit(imported[1].id)).boundary)

        val firstRetry = tracks.importEntries(batch.preparation.request, batch.drafts)
        assertTrue(firstRetry.alreadyApplied)
        assertEquals("User edit wins", requireNotNull(tracks.prepareEntryEdit(imported[0].id)).draft.values.getValue(titleUuid).textValue)
        assertEquals(null, database.trackDao().getEntry(imported[1].id))
        assertEquals(1, count("SELECT COUNT(*) FROM track_entries WHERE trackId = $trackId"))

        tracks.deleteEntry(requireNotNull(tracks.prepareEntryEdit(imported[0].id)).boundary)
        val allDeletedRetry = tracks.importEntries(batch.preparation.request, batch.drafts)
        assertTrue(allDeletedRetry.alreadyApplied)
        assertEquals(0, count("SELECT COUNT(*) FROM track_entries WHERE trackId = $trackId"))
        assertEquals(1, receiptCount(batch.preparation.request.batchUuid))
    }

    @Test
    fun deterministicEntryCollisionWithoutReceiptFailsClosedAndPreservesTheCollidingFact() = runBlocking {
        val trackId = tracks.create(textTrack())
        val batch = prepareTextBatch(
            trackId,
            "444dc6b5-d263-49a1-ab1d-d2461f1e10a6",
            listOf("Must not overwrite"),
        )
        val collisionUuid = batch.preparation.request.entryUuids.single()
        val collidingId = database.trackDao().insertEntry(
            TrackEntryEntity(
                uuid = collisionUuid,
                trackId = trackId,
                entryEpochDay = CsvClock.today().minusDays(1).toEpochDay(),
                sourceOccurrenceId = null,
                sourceExplanation = "pre-existing without receipt",
                createdAtMillis = 1,
                updatedAtMillis = 1,
            ),
        )

        assertCsvConflict(
            TrackCsvImportConflictKind.EntryIdentityCollision,
            runCatching { tracks.importEntries(batch.preparation.request, batch.drafts) }.exceptionOrNull(),
        )
        assertEquals(collidingId, requireNotNull(database.trackDao().getEntryByUuid(collisionUuid)).id)
        assertEquals(1, count("SELECT COUNT(*) FROM track_entries WHERE trackId = $trackId"))
        assertEquals(0, receiptCount(batch.preparation.request.batchUuid))
        assertEquals(0, count("SELECT COUNT(*) FROM track_values"))
    }

    @Test
    fun malformedOrForgedSecondRowRollsBackTheWholeBatch() = runBlocking {
        val trackId = tracks.create(textTrack())
        val batch = prepareTextBatch(
            trackId,
            "e914762f-d2bd-471c-9511-752a89699b36",
            listOf("Would otherwise write", "Forged row"),
        )
        val forged = batch.drafts.toMutableList().also { drafts ->
            drafts[1] = drafts[1].copy(sourceOccurrenceId = 77, sourceExplanation = "forged automation")
        }

        assertCsvConflict(
            TrackCsvImportConflictKind.RequestMalformed,
            runCatching { tracks.importEntries(batch.preparation.request, forged) }.exceptionOrNull(),
        )
        assertEquals(0, count("SELECT COUNT(*) FROM track_entries WHERE trackId = $trackId"))
        assertEquals(0, count("SELECT COUNT(*) FROM track_values"))
        assertEquals(0, receiptCount(batch.preparation.request.batchUuid))

        val titleUuid = batch.preparation.form.fields.single { it.primary }.uuid
        val malformed = batch.drafts.toMutableList().also { drafts ->
            drafts[1] = drafts[1].copy(
                values = mapOf(titleUuid to TrackValueDraft(textValue = "mixed payload", booleanValue = true)),
            )
        }
        assertCsvConflict(
            TrackCsvImportConflictKind.RequestMalformed,
            runCatching { tracks.importEntries(batch.preparation.request, malformed) }.exceptionOrNull(),
        )
        assertEquals(0, count("SELECT COUNT(*) FROM track_entries WHERE trackId = $trackId"))
        assertEquals(0, receiptCount(batch.preparation.request.batchUuid))
    }

    @Test
    fun failureAfterTheFirstRowWritesRollsBackEntriesValuesSearchAndReceipt() = runBlocking {
        val trackId = tracks.create(textTrack())
        val batch = prepareTextBatch(
            trackId,
            "f184ef4a-0348-48f3-8450-8d91e53012b8",
            listOf("Written before failure", "Failure during second row"),
        )
        val completedRows = AtomicInteger()
        val failingRepository = RoomTrackRepository(
            database = database,
            clock = CsvClock,
            ids = CsvIds(),
            csvImportCheckpoint = { insertedRows ->
                completedRows.set(insertedRows)
                if (insertedRows == 1) throw InjectedCsvWriteFailure()
            },
        )

        val failure = runCatching {
            failingRepository.importEntries(batch.preparation.request, batch.drafts)
        }.exceptionOrNull()

        assertTrue("Expected an injected mid-write failure, but was $failure", failure is InjectedCsvWriteFailure)
        assertEquals(1, completedRows.get())
        assertEquals(0, count("SELECT COUNT(*) FROM track_entries WHERE trackId = $trackId"))
        assertEquals(0, count("SELECT COUNT(*) FROM track_values"))
        assertEquals(0, count("SELECT COUNT(*) FROM track_entry_search WHERE trackId = $trackId"))
        assertEquals(0, receiptCount(batch.preparation.request.batchUuid))
    }

    @Test
    fun requestAndCommittedReceiptIdentityTamperingFailClosedAcrossTheFullEnvelope() = runBlocking {
        val trackId = tracks.create(textTrack())
        val clean = prepareTextBatch(
            trackId,
            "a2354327-323d-4d4f-b25b-91216513a65d",
            listOf("Identity envelope"),
        )
        val request = clean.preparation.request
        val changedEntryUuid = UUID.fromString(request.entryUuids.single())
            .let { UUID(it.mostSignificantBits, it.leastSignificantBits xor 1L).toString() }
        val tamperedRequests = linkedMapOf(
            "stable Track UUID" to request.copy(
                openingFormBoundary = request.openingFormBoundary.copy(trackUuid = UUID.randomUUID().toString()),
            ),
            "stable Track creation time" to request.copy(
                openingFormBoundary = request.openingFormBoundary.copy(
                    trackCreatedAtMillis = request.openingFormBoundary.trackCreatedAtMillis + 1L,
                ),
            ),
            "request fingerprint" to request.copy(requestFingerprint = "0".repeat(64)),
            "deterministic Entry UUID" to request.copy(entryUuids = listOf(changedEntryUuid)),
            "Entry identity digest" to request.copy(entryIdentityDigest = "0".repeat(64)),
            "row count" to request.copy(rowCount = request.rowCount + 1),
            "fingerprint protocol version" to request.copy(fingerprintVersion = request.fingerprintVersion + 1),
            "identity protocol version" to request.copy(identityVersion = request.identityVersion + 1),
        )

        tamperedRequests.forEach { (field, tampered) ->
            assertCsvConflict(
                TrackCsvImportConflictKind.RequestMalformed,
                runCatching { tracks.importEntries(tampered, clean.drafts) }.exceptionOrNull(),
                field,
            )
            assertEquals("Precommit tamper wrote Entries for $field", 0, count("SELECT COUNT(*) FROM track_entries"))
            assertEquals("Precommit tamper wrote a receipt for $field", 0, receiptCount(request.batchUuid))
        }

        tracks.importEntries(request, clean.drafts)
        tamperedRequests.forEach { (field, tampered) ->
            assertCsvConflict(
                TrackCsvImportConflictKind.BatchIdentityCollision,
                runCatching { tracks.importEntries(tampered, clean.drafts) }.exceptionOrNull(),
                field,
            )
        }
        assertEquals(1, count("SELECT COUNT(*) FROM track_entries WHERE trackId = $trackId"))
        assertEquals(1, receiptCount(request.batchUuid))
    }

    @Test
    fun missingTargetAndReusedNumericIdWithDifferentStableIdentityAreDistinguished() = runBlocking {
        val missingTrackId = tracks.create(textTrack().copy(name = "Missing target"))
        val missingBatch = prepareTextBatch(
            missingTrackId,
            "2235de5c-e0f0-4cc7-8373-4d02cd58c643",
            listOf("Blocked"),
        )
        assertEquals(1, database.trackDao().deleteTrack(missingTrackId))
        assertCsvConflict(
            TrackCsvImportConflictKind.TargetMissing,
            runCatching { tracks.importEntries(missingBatch.preparation.request, missingBatch.drafts) }.exceptionOrNull(),
        )

        val reusedTrackId = tracks.create(textTrack().copy(name = "Reused numeric ID"))
        val reusedBatch = prepareTextBatch(
            reusedTrackId,
            "90b98cef-8430-4204-a8f2-40e87f021a10",
            listOf("Blocked"),
        )
        val originalTrack = requireNotNull(database.trackDao().getTrack(reusedTrackId))
        val originalFields = database.trackDao().getFields(reusedTrackId)
        assertEquals(1, database.trackDao().deleteTrack(reusedTrackId))
        assertEquals(
            reusedTrackId,
            database.trackDao().insertTrack(originalTrack.copy(uuid = UUID.randomUUID().toString())),
        )
        originalFields.forEach { field -> assertEquals(field.id, database.trackDao().insertField(field)) }

        assertCsvConflict(
            TrackCsvImportConflictKind.IdentityChanged,
            runCatching { tracks.importEntries(reusedBatch.preparation.request, reusedBatch.drafts) }.exceptionOrNull(),
        )
        assertEquals(0, count("SELECT COUNT(*) FROM track_entries"))
        assertEquals(0, count("SELECT COUNT(*) FROM track_csv_import_receipts"))
    }

    @Test
    fun exactMaximumCellCustomUnitBatchRemainsBoundedSearchableAndIdempotent() = runBlocking {
        val unit = UnitDefinitionEntity(
            id = "csvbulkunit",
            name = "bulk count",
            symbol = "bulk",
            dimension = UnitDimension.Count.name,
            toCanonicalFactor = 2.0,
            toCanonicalOffset = 1.0,
            custom = true,
            archived = false,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        database.measurementDao().upsertUnit(unit)
        val fieldCount = 20
        val trackId = tracks.create(
            TrackDraft(
                name = "Maximum-cell custom-unit CSV batch",
                fields = List(fieldCount) { fieldIndex ->
                    TrackFieldDraft(
                        name = "Measurement ${fieldIndex + 1}",
                        type = TrackFieldType.Number,
                        required = true,
                        primary = fieldIndex == 0,
                        dimension = UnitDimension.Count,
                        unitId = unit.id,
                    )
                },
            ),
        )
        val openingForm = requireNotNull(tracks.csvImportForm(trackId))
        val fields = openingForm.fields.sortedBy { it.name.removePrefix("Measurement ").toInt() }
        val drafts = List(TRACK_CSV_MAX_IMPORT_ROWS) { rowIndex ->
            TrackEntryDraft(
                entryDate = CsvClock.today().minusDays((rowIndex % 365).toLong()),
                values = fields.mapIndexed { fieldIndex, field ->
                    field.uuid to TrackValueDraft(
                        enteredNumber = (rowIndex * fieldCount + fieldIndex + 1).toDouble(),
                        enteredUnitId = unit.id,
                    )
                }.toMap(linkedMapOf()),
            )
        }
        assertEquals(100_000, drafts.sumOf { it.values.size })

        val prepareStarted = SystemClock.elapsedRealtime()
        val preparation = tracks.prepareCsvImport(
            openingForm = openingForm,
            batchUuid = "be7bc55a-9410-4e24-9f19-38fcb6ec11dd",
            payloadFingerprint = trackCsvPayloadFingerprint("exact 5,000 by 20 custom-unit cells"),
            mapping = TrackCsvMapping(fieldColumns = fields.associate { it.uuid to it.name }),
            defaultEntryDate = CsvClock.today(),
            drafts = drafts,
        )
        val prepareMillis = SystemClock.elapsedRealtime() - prepareStarted
        val commitStarted = SystemClock.elapsedRealtime()
        val first = tracks.importEntries(preparation.request, drafts)
        val commitMillis = SystemClock.elapsedRealtime() - commitStarted
        val retryStarted = SystemClock.elapsedRealtime()
        val retry = tracks.importEntries(preparation.request, drafts)
        val retryMillis = SystemClock.elapsedRealtime() - retryStarted
        val totalMillis = prepareMillis + commitMillis + retryMillis
        val timing = "prepare=${prepareMillis}ms commit=${commitMillis}ms retry=${retryMillis}ms total=${totalMillis}ms"
        Log.i("TrackCsvImportIntegrity", "Exact 5,000-row/100,000-cell custom-unit timing: $timing")

        assertTrue(first.changed)
        assertFalse(first.alreadyApplied)
        assertFalse(retry.changed)
        assertTrue(retry.alreadyApplied)
        assertEquals(TRACK_CSV_MAX_IMPORT_ROWS, first.rowCount)
        assertEquals(TRACK_CSV_MAX_IMPORT_ROWS, count("SELECT COUNT(*) FROM track_entries WHERE trackId = $trackId"))
        assertEquals(100_000, count("SELECT COUNT(*) FROM track_values"))
        assertEquals(TRACK_CSV_MAX_IMPORT_ROWS, count("SELECT COUNT(*) FROM track_entry_search WHERE trackId = $trackId"))
        assertEquals(TRACK_CSV_MAX_IMPORT_ROWS, tracks.searchEntryIds(trackId, unit.id).size)
        assertEquals(1, receiptCount(preparation.request.batchUuid))
        assertTrue(
            tracks.verifyCsvImportReceipt(first.receiptEnvelope()) is TrackCsvImportReceiptVerification.Exact,
        )
        assertTrue(
            "Exact 5,000-row/100,000-cell custom-unit import exceeded the 120-second practical harness bound: $timing",
            totalMillis < 120_000L,
        )
    }

    @Test
    fun archiveFieldChoiceAndUnitRacesRejectWithoutPartialImports() = runBlocking {
        val archivedTrack = tracks.create(textTrack().copy(name = "Archive race"))
        val archiveBatch = prepareTextBatch(
            archivedTrack,
            "753365f4-6d06-4e02-87f9-641928633904",
            listOf("Blocked"),
        )
        tracks.setArchived(archivedTrack, true)
        assertCsvConflict(
            TrackCsvImportConflictKind.FormChanged,
            runCatching { tracks.importEntries(archiveBatch.preparation.request, archiveBatch.drafts) }.exceptionOrNull(),
        )

        val fieldTrack = tracks.create(textTrack().copy(name = "Field race"))
        val fieldBatch = prepareTextBatch(
            fieldTrack,
            "4c616886-1899-4f48-96c5-e075618ae3c7",
            listOf("Blocked"),
        )
        val fieldProjection = requireNotNull(tracks.projection(fieldTrack))
        tracks.update(
            fieldTrack,
            fieldProjection.toDraft().copy(
                fields = fieldProjection.toDraft().fields.map { field ->
                    if (field.primary) field.copy(name = "Renamed identity") else field
                },
            ),
            requireNotNull(tracks.definitionBoundary(fieldTrack)),
        )
        assertCsvConflict(
            TrackCsvImportConflictKind.FormChanged,
            runCatching { tracks.importEntries(fieldBatch.preparation.request, fieldBatch.drafts) }.exceptionOrNull(),
        )

        val choiceTrack = tracks.create(choiceTrack())
        val choiceProjection = requireNotNull(tracks.projection(choiceTrack))
        val title = choiceProjection.primaryField
        val genre = choiceProjection.fields.single { it.name == "Genre" }
        val history = choiceProjection.optionsFor(genre.id).single { it.label == "History" }
        val choiceDrafts = listOf(
            TrackEntryDraft(
                CsvClock.today(),
                mapOf(
                    title.uuid to TrackValueDraft(textValue = "Choice race"),
                    genre.uuid to TrackValueDraft(choiceOptionUuid = history.uuid),
                ),
            ),
        )
        val choiceBatch = requireNotNull(
            tracks.prepareCsvImport(
                requireNotNull(tracks.csvImportForm(choiceTrack)),
                "6f643fb5-cd5c-45c3-8dfe-2e59659a2288",
                trackCsvPayloadFingerprint("Title,Genre\nChoice race,History\n"),
                TrackCsvMapping(fieldColumns = mapOf(title.uuid to "Title", genre.uuid to "Genre")),
                CsvClock.today(),
                choiceDrafts,
            ),
        )
        tracks.update(
            choiceTrack,
            choiceProjection.toDraft().copy(
                fields = choiceProjection.toDraft().fields.map { field ->
                    if (field.id == genre.id) {
                        field.copy(options = field.options.map { option ->
                            if (option.id == history.id) option.copy(label = "Nonfiction") else option
                        })
                    } else field
                },
            ),
            requireNotNull(tracks.definitionBoundary(choiceTrack)),
        )
        assertCsvConflict(
            TrackCsvImportConflictKind.FormChanged,
            runCatching { tracks.importEntries(choiceBatch.request, choiceDrafts) }.exceptionOrNull(),
        )

        val unit = UnitDefinitionEntity(
            id = "csv-serving",
            name = "serving",
            symbol = "srv",
            dimension = UnitDimension.Count.name,
            toCanonicalFactor = 1.0,
            toCanonicalOffset = 0.0,
            custom = true,
            archived = false,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        database.measurementDao().upsertUnit(unit)
        val unitTrack = tracks.create(numberTrack(unit.id))
        val unitProjection = requireNotNull(tracks.projection(unitTrack))
        val unitTitle = unitProjection.primaryField
        val amount = unitProjection.fields.single { it.name == "Amount" }
        val unitDrafts = listOf(
            TrackEntryDraft(
                CsvClock.today(),
                mapOf(
                    unitTitle.uuid to TrackValueDraft(textValue = "Unit race"),
                    amount.uuid to TrackValueDraft(enteredNumber = 2.0, enteredUnitId = unit.id),
                ),
            ),
        )
        val unitBatch = requireNotNull(
            tracks.prepareCsvImport(
                requireNotNull(tracks.csvImportForm(unitTrack)),
                "8cf65faf-3f26-42ab-b8c8-71e653909906",
                trackCsvPayloadFingerprint("Title,Amount\nUnit race,2\n"),
                TrackCsvMapping(fieldColumns = mapOf(unitTitle.uuid to "Title", amount.uuid to "Amount")),
                CsvClock.today(),
                unitDrafts,
            ),
        )
        database.measurementDao().upsertUnit(
            unit.copy(
                name = "tablet",
                symbol = "tab",
                toCanonicalFactor = 2.0,
                updatedAtMillis = 2,
            ),
        )
        assertCsvConflict(
            TrackCsvImportConflictKind.FormChanged,
            runCatching { tracks.importEntries(unitBatch.request, unitDrafts) }.exceptionOrNull(),
        )

        assertEquals(0, count("SELECT COUNT(*) FROM track_entries"))
        assertEquals(0, count("SELECT COUNT(*) FROM track_csv_import_receipts"))
    }

    @Test
    fun previewSnapshotCannotBlessAFormMutationThatOccursBeforePreparation() = runBlocking {
        val trackId = tracks.create(textTrack().copy(name = "Preview-to-prepare race"))
        val openingForm = requireNotNull(tracks.csvImportForm(trackId))
        val primary = openingForm.fields.single { it.primary }
        val draft = TrackEntryDraft(
            CsvClock.today(),
            mapOf(primary.uuid to TrackValueDraft(textValue = "Stale preview")),
        )
        val projection = requireNotNull(tracks.projection(trackId))
        tracks.update(
            trackId,
            projection.toDraft().copy(
                fields = projection.toDraft().fields.map { field ->
                    if (field.primary) field.copy(name = "Identity renamed after preview") else field
                },
            ),
            requireNotNull(tracks.definitionBoundary(trackId)),
        )

        assertCsvConflict(
            TrackCsvImportConflictKind.FormChanged,
            runCatching {
                tracks.prepareCsvImport(
                    openingForm = openingForm,
                    batchUuid = "307d048e-74cd-4fba-a015-f0983614ce3b",
                    payloadFingerprint = trackCsvPayloadFingerprint("Title\nStale preview\n"),
                    mapping = TrackCsvMapping(fieldColumns = mapOf(primary.uuid to "Title")),
                    defaultEntryDate = CsvClock.today(),
                    drafts = listOf(draft),
                )
            }.exceptionOrNull(),
        )
        assertEquals(0, count("SELECT COUNT(*) FROM track_entries"))
        assertEquals(0, count("SELECT COUNT(*) FROM track_csv_import_receipts"))
    }

    @Test
    fun emptyAndOverLimitDirectRepositoryCallsFailBeforeWriting() = runBlocking {
        val trackId = tracks.create(textTrack())
        val one = prepareTextBatch(
            trackId,
            "31ce0e28-c68c-434e-8115-b37ad42a751e",
            listOf("Seed request"),
        )

        assertCsvConflict(
            TrackCsvImportConflictKind.RequestMalformed,
            runCatching { tracks.importEntries(one.preparation.request, emptyList()) }.exceptionOrNull(),
        )
        val titleUuid = one.preparation.form.fields.single { it.primary }.uuid
        val tooMany = List(TRACK_CSV_MAX_IMPORT_ROWS + 1) { ordinal ->
            TrackEntryDraft(
                CsvClock.today(),
                mapOf(titleUuid to TrackValueDraft(textValue = "Row $ordinal")),
            )
        }
        assertCsvConflict(
            TrackCsvImportConflictKind.RequestMalformed,
            runCatching { tracks.importEntries(one.preparation.request, tooMany) }.exceptionOrNull(),
        )
        assertTrue(
            runCatching {
                tracks.prepareCsvImport(
                    requireNotNull(tracks.csvImportForm(trackId)),
                    UUID.randomUUID().toString(),
                    trackCsvPayloadFingerprint("Title\n"),
                    TrackCsvMapping(fieldColumns = mapOf(titleUuid to "Title")),
                    CsvClock.today(),
                    emptyList(),
                )
            }.isFailure,
        )
        assertEquals(0, count("SELECT COUNT(*) FROM track_entries"))
        assertEquals(0, count("SELECT COUNT(*) FROM track_csv_import_receipts"))
    }

    @Test
    fun committedBatchIndexesAllRowsAndPersistsExactReceiptEntryAndValueCounts() = runBlocking {
        val trackId = tracks.create(allTypesTrack())
        val projection = requireNotNull(tracks.projection(trackId))
        val fields = projection.fields.associateBy { it.name }
        val choice = projection.optionsFor(requireNotNull(fields["Mood"]).id).single { it.label == "Ready" }
        fun row(name: String, ordinal: Int) = TrackEntryDraft(
            CsvClock.today().minusDays(ordinal.toLong()),
            mapOf(
                requireNotNull(fields["Name"]).uuid to TrackValueDraft(textValue = name),
                requireNotNull(fields["Notes"]).uuid to TrackValueDraft(textValue = "search-$ordinal"),
                requireNotNull(fields["Amount"]).uuid to TrackValueDraft(enteredNumber = ordinal + 1.5, enteredUnitId = "unitless"),
                requireNotNull(fields["Mood"]).uuid to TrackValueDraft(choiceOptionUuid = choice.uuid),
                requireNotNull(fields["Rating"]).uuid to TrackValueDraft(scaleValue = ordinal + 1.0),
                requireNotNull(fields["When"]).uuid to TrackValueDraft(dateValue = CsvClock.today().minusDays(ordinal.toLong())),
                requireNotNull(fields["Done"]).uuid to TrackValueDraft(booleanValue = ordinal % 2 == 0),
            ),
        )
        val drafts = listOf(row("Needle alpha", 0), row("Needle beta", 1))
        val mapping = TrackCsvMapping(
            fieldColumns = fields.values.associate { it.uuid to it.name },
        )
        val preparation = requireNotNull(
            tracks.prepareCsvImport(
                requireNotNull(tracks.csvImportForm(trackId)),
                "5d6f7342-c378-45ca-b41f-32d7646c7145",
                trackCsvPayloadFingerprint("all-types payload"),
                mapping,
                CsvClock.today(),
                drafts,
            ),
        )

        val receipt = tracks.importEntries(preparation.request, drafts)
        val entryIds = preparation.request.entryUuids.map { requireNotNull(database.trackDao().getEntryByUuid(it)).id }

        assertEquals(2, receipt.rowCount)
        assertEquals(2, count("SELECT COUNT(*) FROM track_entries WHERE trackId = $trackId"))
        assertEquals(14, count("SELECT COUNT(*) FROM track_values WHERE entryId IN (${entryIds.joinToString()})"))
        assertEquals(2, count("SELECT COUNT(*) FROM track_entry_search WHERE trackId = $trackId"))
        assertEquals(setOf(entryIds[0]), tracks.searchEntryIds(trackId, "alpha"))
        assertEquals(entryIds.toSet(), tracks.searchEntryIds(trackId, "Needle").toSet())
        database.openHelper.readableDatabase.query(
            "SELECT trackId, trackUuid, requestFingerprint, entryIdentityDigest, rowCount, fingerprintVersion, identityVersion " +
                "FROM track_csv_import_receipts WHERE batchUuid = ?",
            arrayOf(receipt.batchUuid),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(trackId, cursor.getLong(0))
            assertEquals(projection.track.uuid, cursor.getString(1))
            assertEquals(preparation.request.requestFingerprint, cursor.getString(2))
            assertEquals(preparation.request.entryIdentityDigest, cursor.getString(3))
            assertEquals(2, cursor.getInt(4))
            assertEquals(preparation.request.fingerprintVersion, cursor.getInt(5))
            assertEquals(preparation.request.identityVersion, cursor.getInt(6))
            assertFalse(cursor.moveToNext())
        }
    }

    private suspend fun prepareTextBatch(trackId: Long, batchUuid: String, titles: List<String>): TextBatch {
        val projection = requireNotNull(tracks.projection(trackId))
        val primary = projection.primaryField
        val drafts = titles.map { title ->
            TrackEntryDraft(CsvClock.today(), mapOf(primary.uuid to TrackValueDraft(textValue = title)))
        }
        val payload = buildString {
            appendLine("Title")
            titles.forEach(::appendLine)
        }
        val preparation = requireNotNull(
            tracks.prepareCsvImport(
                openingForm = requireNotNull(tracks.csvImportForm(trackId)),
                batchUuid = batchUuid,
                payloadFingerprint = trackCsvPayloadFingerprint(payload),
                mapping = TrackCsvMapping(fieldColumns = mapOf(primary.uuid to "Title")),
                defaultEntryDate = CsvClock.today(),
                drafts = drafts,
            ),
        )
        return TextBatch(preparation, drafts)
    }

    private fun count(sql: String): Int = database.openHelper.readableDatabase.query(sql).use { cursor ->
        check(cursor.moveToFirst())
        cursor.getInt(0)
    }

    private fun receiptCount(batchUuid: String): Int = database.openHelper.readableDatabase.query(
        "SELECT COUNT(*) FROM track_csv_import_receipts WHERE batchUuid = ?",
        arrayOf(batchUuid),
    ).use { cursor ->
        check(cursor.moveToFirst())
        cursor.getInt(0)
    }

    private fun assertCsvConflict(
        expected: TrackCsvImportConflictKind,
        failure: Throwable?,
        case: String? = null,
    ) {
        assertTrue("${case?.let { "$it: " }.orEmpty()}Expected $expected but was $failure", failure is TrackCsvImportConflictException)
        assertEquals(expected, (failure as TrackCsvImportConflictException).kind)
    }

    private fun textTrack() = TrackDraft(
        name = "CSV text",
        fields = listOf(TrackFieldDraft("Title", TrackFieldType.ShortText, required = true, primary = true)),
    )

    private fun choiceTrack() = TrackDraft(
        name = "CSV choice",
        fields = listOf(
            TrackFieldDraft("Title", TrackFieldType.ShortText, required = true, primary = true),
            TrackFieldDraft(
                "Genre",
                TrackFieldType.SingleChoice,
                options = listOf(TrackChoiceOptionDraft("History"), TrackChoiceOptionDraft("Fiction")),
            ),
        ),
    )

    private fun numberTrack(unitId: String) = TrackDraft(
        name = "CSV number",
        fields = listOf(
            TrackFieldDraft("Title", TrackFieldType.ShortText, required = true, primary = true),
            TrackFieldDraft("Amount", TrackFieldType.Number, dimension = UnitDimension.Count, unitId = unitId),
        ),
    )

    private fun allTypesTrack() = TrackDraft(
        name = "CSV all types",
        fields = listOf(
            TrackFieldDraft("Name", TrackFieldType.ShortText, required = true, primary = true),
            TrackFieldDraft("Notes", TrackFieldType.LongText),
            TrackFieldDraft("Amount", TrackFieldType.Number, dimension = UnitDimension.Unitless, unitId = "unitless"),
            TrackFieldDraft(
                "Mood",
                TrackFieldType.SingleChoice,
                options = listOf(TrackChoiceOptionDraft("Ready"), TrackChoiceOptionDraft("Tired")),
            ),
            TrackFieldDraft("Rating", TrackFieldType.Scale, scaleMin = 1, scaleMax = 5, scaleStep = 1.0),
            TrackFieldDraft("When", TrackFieldType.Date),
            TrackFieldDraft("Done", TrackFieldType.YesNo),
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
                options = optionsFor(field.id).map { option ->
                    TrackChoiceOptionDraft(option.label, option.uuid, option.id)
                },
                uuid = field.uuid,
                id = field.id,
                scaleStep = field.scaleStep,
            )
        },
    )

    private data class TextBatch(
        val preparation: TrackCsvImportPreparation,
        val drafts: List<TrackEntryDraft>,
    )

    private object CsvClock : WhipClock {
        override fun now(): Instant = Instant.parse("2026-09-01T13:00:00Z")
        override fun today(zoneId: ZoneId): LocalDate = LocalDate.of(2026, 9, 1)
    }

    private class CsvIds : WhipIdGenerator {
        private val next = AtomicInteger()
        override fun nextId(): String = "csv-integrity-${next.incrementAndGet()}"
    }

    private class InjectedCsvWriteFailure : IllegalStateException("Injected failure after one complete CSV row")
}
