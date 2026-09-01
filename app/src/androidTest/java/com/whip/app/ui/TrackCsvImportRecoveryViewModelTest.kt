package com.whip.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whip.app.WhipApplication
import com.whip.app.domain.TrackCsvImportPreparation
import com.whip.app.domain.TrackCsvMapping
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackProjection
import com.whip.app.domain.TrackValueDraft
import com.whip.app.domain.receiptEnvelope
import com.whip.app.domain.trackCsvPayloadFingerprint
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Real ViewModel/process-restoration coverage for unknown-outcome CSV commits. */
@RunWith(AndroidJUnit4::class)
class TrackCsvImportRecoveryViewModelTest {
    private val app: WhipApplication
        get() = ApplicationProvider.getApplicationContext()

    private lateinit var viewModelStore: ViewModelStore

    @Before
    fun clearApplicationData() = runBlocking {
        app.backupRepository.deleteAllData()
        app.areaRepository.ensureDefaultArea()
        Unit
    }

    @After
    fun clearViewModelAndApplicationData() {
        if (::viewModelStore.isInitialized) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { viewModelStore.clear() }
        }
        runBlocking { app.backupRepository.deleteAllData() }
    }

    @Test
    fun restoredCommittedSessionVerifiesReceiptBeforeTouchingAnUnavailableUri() = runBlocking {
        val fixture = committedFixture()
        val viewModel = createViewModel(restoredHandle(fixture.descriptor))

        val recovered = awaitTerminal(viewModel)

        assertEquals(TrackCsvImportPhase.Complete, recovered.phase)
        assertEquals(fixture.preparation.request.receiptEnvelope(), recovered.completionReceipt?.receiptEnvelope())
        assertTrue(requireNotNull(recovered.completionReceipt).alreadyApplied)
        assertFalse(recovered.requiresNewFile)
        assertEquals(null, recovered.errorMessage)
    }

    @Test
    fun restoredSessionWithSameBatchButDifferentFullEnvelopeFailsClosedBeforeReadingUri() = runBlocking {
        val fixture = committedFixture()
        val expectedEnvelope = requireNotNull(fixture.descriptor.preparedReceiptEnvelope)
        val forged = fixture.descriptor.copy(
            preparedReceiptEnvelope = expectedEnvelope.copy(
                entryIdentityDigest = "0".repeat(64),
            ),
        )
        val viewModel = createViewModel(restoredHandle(forged))

        val recovered = awaitTerminal(viewModel)

        assertEquals(TrackCsvImportPhase.Error, recovered.phase)
        assertTrue(recovered.requiresNewFile)
        assertTrue(requireNotNull(recovered.errorMessage).contains("different completed import"))
        assertEquals(null, recovered.completionReceipt)
    }

    @Test
    fun transientTargetLookupFailureKeepsSessionAndRetryWaitsForFreshSuccessfulLoad() = runBlocking {
        val trackId = app.trackRepository.create(
            TrackDraft(
                name = "Retryable projection ${UUID.randomUUID()}",
                fields = listOf(
                    TrackFieldDraft(
                        name = "Title",
                        type = TrackFieldType.ShortText,
                        required = true,
                        primary = true,
                    ),
                ),
            ),
        )
        val projection = requireNotNull(app.trackRepository.projection(trackId))
        val csv = File.createTempFile("track-import-retry-", ".csv", app.cacheDir).apply {
            writeText("Title\nRecovered after retry\n", StandardCharsets.UTF_8)
        }
        try {
            val handle = SavedStateHandle()
            val session = TrackCsvImportSessionStore(handle, app.currentUserDataGeneration()).begin(
                trackId = trackId,
                trackName = projection.track.name,
                trackUuid = projection.track.uuid,
                trackCreatedAtMillis = projection.track.createdAtMillis,
                batchUuid = UUID.randomUUID().toString(),
                uri = csv.toURI().toString(),
                fileLabel = csv.name,
                today = LocalDate.of(2026, 9, 1),
            )
            val subscriptions = AtomicInteger()
            val transientProjectionSource = flow {
                if (subscriptions.incrementAndGet() == 1) {
                    throw IOException("Track storage was temporarily unavailable")
                }
                emit(listOf(projection))
            }
            val viewModel = createViewModel(handle, transientProjectionSource)

            val failedLookup = withTimeout(15_000) {
                viewModel.uiState.first { !it.loading && it.errorMessage != null }
            }
            assertEquals(0, failedLookup.lookupGeneration)
            assertEquals("Track storage was temporarily unavailable", failedLookup.errorMessage)
            assertEquals(TrackCsvImportPhase.Reading, viewModel.csvImportState.value.phase)
            assertFalse(viewModel.csvImportState.value.requiresNewFile)
            assertNotNull(TrackCsvImportSessionStore(handle, app.currentUserDataGeneration()).descriptor)

            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                viewModel.retryCsvImport()
            }

            val ready = withTimeout(15_000) {
                viewModel.csvImportState.first { it.phase == TrackCsvImportPhase.Ready }
            }
            assertTrue(subscriptions.get() >= 2)
            assertTrue(viewModel.uiState.value.lookupGeneration >= 1)
            assertEquals(null, viewModel.uiState.value.errorMessage)
            assertEquals(1, ready.preview?.validRows)
            assertEquals("Title", ready.mapping.fieldColumns[ready.openingForm?.fields?.single()?.uuid])
            assertNotNull(TrackCsvImportSessionStore(handle, app.currentUserDataGeneration()).descriptor)
        } finally {
            csv.delete()
        }
    }

    @Test
    fun deviceRuntimeRejectsMalformedUtf8AndNulButAllowsMaximumRowsWithOneTrailingBlank() {
        val malformed = runCatching {
            decodeTrackCsvUtf8(byteArrayOf(0xC3.toByte(), 0x28))
        }.exceptionOrNull()
        val nul = runCatching {
            decodeTrackCsvUtf8("Title\nA\u0000B\n".toByteArray(StandardCharsets.UTF_8))
        }.exceptionOrNull()
        val maximumWithTrailingBlank = buildString {
            appendLine("Title")
            repeat(5_000) { ordinal -> appendLine("Row $ordinal") }
            appendLine()
        }

        assertTrue("Android UTF-8 decoding must fail closed: $malformed", malformed is IllegalArgumentException)
        assertTrue("Android NUL decoding must fail closed: $nul", nul is IllegalArgumentException)
        assertEquals(5_000, validateTrackCsvEnvelope(maximumWithTrailingBlank).dataRows)
    }

    private suspend fun committedFixture(): CommittedFixture {
        val trackId = app.trackRepository.create(
            TrackDraft(
                name = "Process-restored CSV ${UUID.randomUUID()}",
                fields = listOf(
                    TrackFieldDraft(
                        name = "Title",
                        type = TrackFieldType.ShortText,
                        required = true,
                        primary = true,
                    ),
                ),
            ),
        )
        val form = requireNotNull(app.trackRepository.csvImportForm(trackId))
        val primary = form.fields.single { it.primary }
        val today = LocalDate.of(2026, 9, 1)
        val mapping = TrackCsvMapping(fieldColumns = mapOf(primary.uuid to "Title"))
        val draft = TrackEntryDraft(
            entryDate = today,
            values = mapOf(primary.uuid to TrackValueDraft(textValue = "Committed before process death")),
        )
        val payloadFingerprint = trackCsvPayloadFingerprint("Title\nCommitted before process death\n")
        val preparation = app.trackRepository.prepareCsvImport(
            openingForm = form,
            batchUuid = UUID.randomUUID().toString(),
            payloadFingerprint = payloadFingerprint,
            mapping = mapping,
            defaultEntryDate = today,
            drafts = listOf(draft),
        )

        val handle = SavedStateHandle()
        val sessionStore = TrackCsvImportSessionStore(handle, app.currentUserDataGeneration())
        val opened = sessionStore.begin(
            trackId = trackId,
            trackName = form.track.name,
            trackUuid = form.track.uuid,
            trackCreatedAtMillis = form.track.createdAtMillis,
            batchUuid = preparation.request.batchUuid,
            uri = "content://com.whip.test/deleted-provider-document.csv",
            fileLabel = "provider-document-no-longer-readable.csv",
            today = today,
        )
        val loaded = requireNotNull(sessionStore.recordLoadedPayload(opened, payloadFingerprint, mapping))
        val prepared = requireNotNull(sessionStore.recordPreparation(loaded, preparation))
        app.trackRepository.importEntries(preparation.request, listOf(draft))
        val commitIdentity = TrackCsvImportCommitIdentity(
            trackId = trackId,
            batchUuid = preparation.request.batchUuid,
            previewRevision = prepared.previewRevision,
            dataGeneration = prepared.dataGeneration,
            requestFingerprint = preparation.request.requestFingerprint,
            rowCount = preparation.request.rowCount,
            receiptEnvelope = preparation.request.receiptEnvelope(),
        )
        val attempted = requireNotNull(sessionStore.recordCommitAttempt(commitIdentity))
        return CommittedFixture(preparation, attempted)
    }

    private fun restoredHandle(descriptor: TrackCsvImportSessionDescriptor) = SavedStateHandle(
        mapOf("track-csv-import-session" to descriptor),
    )

    private fun createViewModel(
        handle: SavedStateHandle,
        projectionSource: Flow<List<TrackProjection>>? = null,
    ): TrackViewModel {
        lateinit var value: TrackViewModel
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            viewModelStore = ViewModelStore()
            value = if (projectionSource == null) {
                TrackViewModel(app, handle)
            } else {
                TrackViewModel(app, handle, projectionSource, testOverride = true)
            }
        }
        return value
    }

    private suspend fun awaitTerminal(viewModel: TrackViewModel): TrackCsvImportUiState = withTimeout(15_000) {
        viewModel.csvImportState.first { state ->
            state.phase == TrackCsvImportPhase.Complete || state.phase == TrackCsvImportPhase.Error
        }
    }

    private data class CommittedFixture(
        val preparation: TrackCsvImportPreparation,
        val descriptor: TrackCsvImportSessionDescriptor,
    )
}
