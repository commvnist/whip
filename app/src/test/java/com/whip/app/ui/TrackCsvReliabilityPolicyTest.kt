package com.whip.app.ui

import androidx.lifecycle.SavedStateHandle
import com.whip.app.data.buildTrackCsv
import com.whip.app.domain.Track
import com.whip.app.domain.TrackEntry
import com.whip.app.domain.TrackEntryProjection
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackEntryFormBoundary
import com.whip.app.domain.TrackEntryFormSnapshot
import com.whip.app.domain.TrackField
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackFieldValue
import com.whip.app.domain.TrackCsvImportPreparation
import com.whip.app.domain.TrackCsvImportPreview
import com.whip.app.domain.TrackCsvImportReceipt
import com.whip.app.domain.TrackCsvImportRequest
import com.whip.app.domain.TrackCsvMapping
import com.whip.app.domain.receiptEnvelope
import com.whip.app.domain.TrackProjection
import com.whip.app.domain.TrackCondition
import com.whip.app.domain.TrackConditionOperator
import com.whip.app.domain.UnitDimension
import java.time.LocalDate
import java.io.IOException
import java.io.Writer
import java.util.concurrent.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackCsvReliabilityPolicyTest {
    @Test
    fun quotedNewlinesAreOneCsvRecord() {
        val result = validateTrackCsvEnvelope("Name,Notes\nAlice,\"first line\nsecond line\"\n")

        assertEquals(1, result.dataRows)
        assertEquals(2, result.maximumColumns)
    }

    @Test
    fun oversizedFileHasActionableLimit() {
        val message = runCatching {
            validateTrackCsvEnvelope("x".repeat(TRACK_CSV_MAX_FILE_BYTES + 1))
        }.exceptionOrNull()?.message.orEmpty()

        assertTrue(message, message.contains("smaller than 5 MB"))
    }

    @Test
    fun rowAndColumnLimitsRecommendHowToRecover() {
        val tooManyRows = buildString {
            appendLine("Name")
            repeat(TRACK_CSV_MAX_DATA_ROWS + 1) { appendLine("row-$it") }
        }
        val rowMessage = runCatching { validateTrackCsvEnvelope(tooManyRows) }.exceptionOrNull()?.message.orEmpty()
        assertTrue(rowMessage, rowMessage.contains("Split it into smaller files"))

        val tooManyColumns = (0..TRACK_CSV_MAX_COLUMNS).joinToString(",") { "Column $it" }
        val columnMessage = runCatching { validateTrackCsvEnvelope(tooManyColumns) }.exceptionOrNull()?.message.orEmpty()
        assertTrue(columnMessage, columnMessage.contains("Remove unused columns"))
    }

    @Test
    fun quotedEmptyRecordsCannotBypassTheRowLimit() {
        val csv = buildString {
            appendLine("Value")
            repeat(TRACK_CSV_MAX_DATA_ROWS + 1) { appendLine("\"\"") }
        }

        val message = runCatching { validateTrackCsvEnvelope(csv) }.exceptionOrNull()?.message.orEmpty()

        assertTrue(message, message.contains("more than 5,000 data rows"))
    }

    @Test
    fun previewCellBudgetRejectsWideLargeImportsBeforeParsing() {
        val header = (1..TRACK_CSV_MAX_COLUMNS).joinToString(",") { "C$it" }
        val csv = buildString {
            appendLine(header)
            repeat(TRACK_CSV_MAX_PREVIEW_CELLS / TRACK_CSV_MAX_COLUMNS + 1) { appendLine("value") }
        }

        val message = runCatching { validateTrackCsvEnvelope(csv) }.exceptionOrNull()?.message.orEmpty()
        assertTrue(message, message.contains("100,000 cells"))
    }

    @Test
    fun defaultMappingRecognizesExportedNumberColumns() {
        val field = TrackField(
            id = 1,
            uuid = "distance",
            trackId = 7,
            name = "Distance",
            type = TrackFieldType.Number,
            position = 0,
            required = true,
            primary = true,
            showInList = true,
            dimension = UnitDimension.Distance,
            unitId = "kilometre",
            precision = 1,
            scaleMin = null,
            scaleMax = null,
            scaleLowLabel = "",
            scaleHighLabel = "",
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        val projection = TrackProjection(
            Track(7, "track-7", "Runs", "", "🏃", "fitness", "Fitness", emptyList(), false, false, 0, 1, 1),
            listOf(field),
            emptyList(),
            emptyList(),
        )

        val mapping = defaultTrackCsvMapping(
            projection,
            listOf("Entry Date", "Distance (Entered)", "Distance (Unit)", "Distance (Canonical)"),
        )

        assertEquals("Entry Date", mapping.entryDateColumn)
        assertEquals("Distance (Entered)", mapping.fieldColumns[field.uuid])
        assertEquals("Distance (Unit)", mapping.numberUnitColumns[field.uuid])
    }

    @Test
    fun nestedTrackConditionsRoundTripThroughRotationEncoding() {
        val condition = TrackCondition(
            fieldUuid = "field|with punctuation",
            operator = TrackConditionOperator.Between,
            textValue = "text|with,delimiters\nand unicode ✓",
            numberValue = 1.25,
            secondNumberValue = 9.75,
            choiceOptionUuids = setOf("one|1", "two.2"),
            dateValue = LocalDate.of(2026, 8, 1),
            secondDateValue = LocalDate.of(2026, 8, 29),
        )
        assertEquals(condition, decodeTrackCondition(encodeTrackCondition(condition)))
    }

    @Test
    fun exportStopsBeforeCrossingItsByteCeiling() {
        val message = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { buildTrackCsv(exportProjection(), maxBytes = 32) }
        }.message.orEmpty()

        assertTrue(message, message.contains("larger than 25 MB"))
    }

    @Test
    fun exportPropagatesCancellationAtFormattingCheckpoints() {
        var checkpoints = 0

        assertThrows(CancellationException::class.java) {
            runBlocking {
                buildTrackCsv(exportProjection(), maxBytes = 1_024) {
                    checkpoints++
                    if (checkpoints == 3) throw CancellationException("Export canceled")
                }
            }
        }
        assertEquals(3, checkpoints)
    }

    @Test
    fun compactImportSessionRestoresByRereadingItsUri() = runBlocking {
        val handle = SavedStateHandle()
        val store = TrackCsvImportSessionStore(handle)
        beginSession(store, 42, "content://documents/runs.csv", LocalDate.of(2026, 8, 29))
        store.updateMapping(
            com.whip.app.domain.TrackCsvMapping(
                entryDateColumn = "Date",
                fieldColumns = mapOf("distance" to "Distance"),
                numberUnitColumns = mapOf("distance" to "Unit"),
            ),
        )

        val restored = requireNotNull(TrackCsvImportSessionStore(handle).descriptor)
        var rereadUri: String? = null
        val csv = reloadTrackCsvText(restored) { uri ->
            rereadUri = uri
            "Date,Distance,Unit\n2026-08-29,5,km"
        }

        assertEquals(42, restored.trackId)
        assertEquals(LocalDate.of(2026, 8, 29), LocalDate.ofEpochDay(restored.todayEpochDay))
        assertEquals("Distance", restored.mapping.fieldColumns["distance"])
        assertEquals("Unit", restored.mapping.numberUnitColumns["distance"])
        assertEquals(TrackCsvImportPhase.Reading, restored.restoredUiState().phase)
        assertEquals("content://documents/runs.csv", rereadUri)
        assertTrue(csv.startsWith("Date,Distance"))
        assertFalse(restored.toString().contains("2026-08-29,5,km"))

        store.clear()
        assertNull(TrackCsvImportSessionStore(handle).descriptor)
    }

    @Test
    fun compactImportSessionSurvivesProcessRecreationOnlyWithinItsDataGeneration() {
        val handle = SavedStateHandle()
        val beforeProcessRecreation = TrackCsvImportSessionStore(handle, currentDataGeneration = 7L)
        beginSession(beforeProcessRecreation, 42, "content://documents/runs.csv", LocalDate.of(2026, 8, 29))

        assertEquals(
            42L,
            TrackCsvImportSessionStore(handle, currentDataGeneration = 7L).descriptor?.trackId,
        )

        assertNull(TrackCsvImportSessionStore(handle, currentDataGeneration = 8L).descriptor)
        assertNull(handle.get<TrackCsvImportSessionDescriptor>("track-csv-import-session"))
    }

    @Test
    fun survivingStoreStampsNewImportsWithTheLivePostRestoreGeneration() {
        val handle = SavedStateHandle()
        var currentGeneration = 3L
        val survivingStore = TrackCsvImportSessionStore(handle) { currentGeneration }
        beginSession(survivingStore, 11, "content://documents/before.csv", LocalDate.of(2026, 8, 30))

        currentGeneration = 4L
        assertNull(survivingStore.descriptor)
        beginSession(survivingStore, 11, "content://documents/after.csv", LocalDate.of(2026, 8, 31))

        val afterProcessRecreation = TrackCsvImportSessionStore(handle) { currentGeneration }
        assertEquals(4L, afterProcessRecreation.descriptor?.dataGeneration)
        assertEquals("content://documents/after.csv", afterProcessRecreation.descriptor?.uri)
    }

    @Test
    fun savedSessionOwnsBatchPayloadPreparedRequestAndPreviewRevisionWithoutPayload() {
        val handle = SavedStateHandle()
        val store = TrackCsvImportSessionStore(handle, currentDataGeneration = 9L)
        val opening = beginSession(store, 42, "content://documents/runs.csv", LocalDate.of(2026, 8, 29))
        val mapping = TrackCsvMapping(fieldColumns = mapOf("distance" to "Distance"))

        val loaded = requireNotNull(store.recordLoadedPayload(opening, "a".repeat(64), mapping))
        val preparation = csvPreparation(
            loaded.batchUuid,
            loaded.trackId,
            "a".repeat(64),
            mapping,
            LocalDate.of(2026, 8, 29),
        )
        val prepared = requireNotNull(store.recordPreparation(loaded, preparation))
        val identity = TrackCsvImportUiState(
            trackId = prepared.trackId,
            batchUuid = prepared.batchUuid,
            dataGeneration = prepared.dataGeneration,
            previewRevision = prepared.previewRevision,
            phase = TrackCsvImportPhase.Ready,
            preview = TrackCsvImportPreview(
                headers = listOf("Distance"),
                totalRows = 1,
                validDrafts = listOf(TrackEntryDraft(LocalDate.of(2026, 8, 29), emptyMap())),
                issues = emptyList(),
            ),
            preparation = preparation,
        ).commitIdentityOrNull()

        assertEquals(opening.previewRevision + 1L, prepared.previewRevision)
        assertEquals(preparation.request.requestFingerprint, prepared.preparedRequestFingerprint)
        assertEquals(1, prepared.preparedEntryCount)
        assertEquals(preparation.request.receiptEnvelope(), prepared.preparedReceiptEnvelope)
        assertTrue(prepared.ownsTrackIdentity(preparation.form))
        assertFalse(prepared.copy(trackUuid = "replacement-track").ownsTrackIdentity(preparation.form))
        assertEquals(prepared.batchUuid, identity?.batchUuid)
        assertFalse(prepared.toString().contains("Distance\n2026"))
        assertFalse(prepared.toString().contains(preparation.request.entryUuids.single()))

        val attempted = requireNotNull(store.recordCommitAttempt(requireNotNull(identity)))
        assertTrue(attempted.commitAttempted)
        assertTrue(TrackCsvImportSessionStore(handle, currentDataGeneration = 9L).descriptor?.commitAttempted == true)
    }

    @Test
    fun mappingChangeInvalidatesOnlyPreparedRequestAndRejectsStalePreparation() {
        val store = TrackCsvImportSessionStore(SavedStateHandle(), currentDataGeneration = 2L)
        val opening = beginSession(store, 7, "content://documents/a.csv", LocalDate.of(2026, 9, 1))
        val firstMapping = TrackCsvMapping(fieldColumns = mapOf("notes" to "Notes"))
        val loaded = requireNotNull(store.recordLoadedPayload(opening, "b".repeat(64), firstMapping))
        val preparation = csvPreparation(loaded.batchUuid, loaded.trackId, "b".repeat(64), firstMapping)
        assertNull(
            store.recordPreparation(
                loaded,
                preparation.copy(
                    request = preparation.request.copy(
                        mapping = TrackCsvMapping(fieldColumns = mapOf("notes" to "Different")),
                    ),
                ),
            ),
        )
        requireNotNull(store.recordPreparation(loaded, preparation))

        val changed = requireNotNull(store.updateMapping(TrackCsvMapping(fieldColumns = mapOf("notes" to "Memo"))))

        assertEquals(loaded.previewRevision + 1L, changed.previewRevision)
        assertNull(changed.preparedRequestFingerprint)
        assertNull(changed.preparedEntryCount)
        assertNull(changed.preparedReceiptEnvelope)
        assertNull(store.recordPreparation(loaded, preparation))
    }

    @Test
    fun readyAdmissionAndReceiptRequireTheExactOwnedRequest() {
        val mapping = TrackCsvMapping(fieldColumns = mapOf("notes" to "Notes"))
        val preparation = csvPreparation(
            "11111111-1111-4111-8111-111111111111",
            7,
            "c".repeat(64),
            mapping,
        )
        val state = TrackCsvImportUiState(
            trackId = 7,
            batchUuid = preparation.request.batchUuid,
            dataGeneration = 4,
            previewRevision = 3,
            phase = TrackCsvImportPhase.Ready,
            preview = TrackCsvImportPreview(
                listOf("Notes"),
                1,
                listOf(TrackEntryDraft(LocalDate.of(2026, 9, 1), emptyMap())),
                emptyList(),
            ),
            preparation = preparation,
        )
        val identity = requireNotNull(state.commitIdentityOrNull())
        val receipt = TrackCsvImportReceipt(
            batchUuid = identity.batchUuid,
            trackId = identity.trackId,
            trackUuid = "track-7",
            trackCreatedAtMillis = 1,
            requestFingerprint = identity.requestFingerprint,
            entryIdentityDigest = preparation.request.entryIdentityDigest,
            rowCount = identity.rowCount,
            fingerprintVersion = 1,
            identityVersion = 1,
            committedAtMillis = 2,
            changed = true,
            alreadyApplied = false,
        )

        assertTrue(receipt.matches(identity))
        assertFalse(receipt.copy(rowCount = 2).matches(identity))
        assertNull(state.copy(phase = TrackCsvImportPhase.Previewing).commitIdentityOrNull())
        assertNull(state.copy(preview = state.preview?.copy(issues = listOf(com.whip.app.domain.TrackCsvImportIssue(2, "bad")))).commitIdentityOrNull())
    }

    @Test
    fun previewFailureBecomesRecoverableErrorInsteadOfReadyWithoutPreview() {
        val initial = TrackCsvImportUiState(trackId = 7, phase = TrackCsvImportPhase.Previewing)

        val failed = trackCsvPreviewCompletion(initial, Result.failure(IllegalArgumentException()))

        assertEquals(TrackCsvImportPhase.Error, failed.phase)
        assertNull(failed.preview)
        assertTrue(failed.errorMessage.orEmpty().contains("Check the column mapping"))
    }

    @Test
    fun databaseImportCannotDismissItsSessionDialog() {
        assertTrue(canDismissTrackCsvImport(databaseImportRunning = false))
        assertFalse(canDismissTrackCsvImport(databaseImportRunning = true))
    }

    @Test
    fun cancellingAReplacementPickerPreservesTheReviewedImportSession() {
        assertTrue(
            shouldCancelCsvImportAfterPickerResult(
                hasSelection = false,
                replacingExistingFile = false,
            ),
        )
        assertFalse(
            shouldCancelCsvImportAfterPickerResult(
                hasSelection = false,
                replacingExistingFile = true,
            ),
        )
        assertFalse(
            shouldCancelCsvImportAfterPickerResult(
                hasSelection = true,
                replacingExistingFile = false,
            ),
        )
    }

    @Test
    fun targetLookupFailureIsNotClassifiedAsADeletedTrack() {
        assertEquals(
            TrackCsvTargetLookupStatus.Pending,
            trackCsvTargetLookupStatus(
                completed = false,
                projection = null,
                targetLookupComplete = false,
                targetLookupError = null,
            ),
        )
        assertEquals(
            TrackCsvTargetLookupStatus.Failed,
            trackCsvTargetLookupStatus(
                completed = false,
                projection = null,
                targetLookupComplete = true,
                targetLookupError = "Track storage was temporarily unavailable",
            ),
        )
        assertEquals(
            TrackCsvTargetLookupStatus.Missing,
            trackCsvTargetLookupStatus(
                completed = false,
                projection = null,
                targetLookupComplete = true,
                targetLookupError = null,
            ),
        )
    }

    @Test
    fun completedImportSuppressesAStaleTargetLookupFailure() {
        assertEquals(
            TrackCsvTargetLookupStatus.Completed,
            trackCsvTargetLookupStatus(
                completed = true,
                projection = null,
                targetLookupComplete = true,
                targetLookupError = "Stale projection error",
            ),
        )
    }

    @Test
    fun csvWriteFailurePropagatesToTheExportRecoveryBoundary() {
        val failure = IOException("Document provider stopped accepting writes")
        val writer = object : Writer() {
            override fun write(buffer: CharArray, offset: Int, count: Int) = throw failure
            override fun flush() = Unit
            override fun close() = Unit
        }

        val thrown = assertThrows(IOException::class.java) {
            runBlocking { writeTrackCsvChunks("header\nvalue", writer) }
        }

        assertEquals(failure, thrown)
    }

    private fun exportProjection(): TrackProjection {
        val notes = TrackField(
            id = 1,
            uuid = "notes",
            trackId = 7,
            name = "Notes",
            type = TrackFieldType.LongText,
            position = 0,
            required = true,
            primary = true,
            showInList = true,
            dimension = null,
            unitId = null,
            precision = 0,
            scaleMin = null,
            scaleMax = null,
            scaleLowLabel = "",
            scaleHighLabel = "",
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        val entry = TrackEntry(1, "entry-1", 7, LocalDate.of(2026, 8, 29), createdAtMillis = 1, updatedAtMillis = 1)
        val value = TrackFieldValue(
            id = 1,
            uuid = "value-1",
            entryId = entry.id,
            fieldId = notes.id,
            textValue = "A long value with \"quotes\" and unicode ✓",
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        return TrackProjection(
            Track(7, "track-7", "Notes", "", "📝", "main", "Main", emptyList(), false, false, 0, 1, 1),
            listOf(notes),
            emptyList(),
            listOf(TrackEntryProjection(entry, mapOf(notes.id to value))),
        )
    }

    private fun beginSession(
        store: TrackCsvImportSessionStore,
        trackId: Long,
        uri: String,
        date: LocalDate,
    ): TrackCsvImportSessionDescriptor = store.begin(
        trackId = trackId,
        trackName = "Runs",
        trackUuid = "track-$trackId",
        trackCreatedAtMillis = 1,
        batchUuid = "11111111-1111-4111-8111-${trackId.toString().padStart(12, '0')}",
        uri = uri,
        fileLabel = uri.substringAfterLast('/'),
        today = date,
    )

    private fun csvPreparation(
        batchUuid: String,
        trackId: Long,
        payloadFingerprint: String,
        mapping: TrackCsvMapping,
        defaultEntryDate: LocalDate = LocalDate.of(2026, 9, 1),
    ): TrackCsvImportPreparation {
        val boundary = TrackEntryFormBoundary(
            trackId = trackId,
            trackUuid = "track-$trackId",
            trackCreatedAtMillis = 1,
            writable = true,
            semanticRevisionToken = "form-$trackId",
        )
        val request = TrackCsvImportRequest(
            batchUuid = batchUuid,
            openingFormBoundary = boundary,
            payloadFingerprint = payloadFingerprint,
            mapping = mapping,
            defaultEntryDate = defaultEntryDate,
            requestFingerprint = "d".repeat(64),
            entryUuids = listOf("22222222-2222-4222-8222-222222222222"),
            entryIdentityDigest = "e".repeat(64),
            rowCount = 1,
        )
        val track = Track(
            trackId,
            "track-$trackId",
            "Runs",
            "",
            "🏃",
            "fitness",
            "Fitness",
            emptyList(),
            false,
            false,
            0,
            1,
            1,
        )
        return TrackCsvImportPreparation(
            request,
            TrackEntryFormSnapshot(boundary, track, emptyList(), emptyList(), emptyList()),
        )
    }
}
