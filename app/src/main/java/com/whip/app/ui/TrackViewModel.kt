package com.whip.app.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.whip.app.WhipApplication
import com.whip.app.core.HomeSection
import com.whip.app.core.CommittedEntitySaveCancellation
import com.whip.app.core.EntitySaveReceipt
import com.whip.app.core.OperationFeedbackPresentation
import com.whip.app.core.OperationStatus
import com.whip.app.core.PersistenceRequestState
import com.whip.app.core.WhipResult
import com.whip.app.core.completeCommittedEntitySave
import com.whip.app.core.revealHomeSection
import com.whip.app.core.saveFollowUpWarning
import com.whip.app.core.tryStartPersistenceRequest
import com.whip.app.data.TrackRepository
import com.whip.app.data.requireTrackCsvExportWithinLimit
import com.whip.app.domain.DeletedTrackEntry
import com.whip.app.domain.Track
import com.whip.app.domain.TrackChoiceOption
import com.whip.app.domain.TrackCsvImportPreview
import com.whip.app.domain.TrackCsvImportPreparation
import com.whip.app.domain.TrackCsvImportReceipt
import com.whip.app.domain.TrackCsvImportReceiptEnvelope
import com.whip.app.domain.TrackCsvImportReceiptVerification
import com.whip.app.domain.TrackCsvImportConflictException
import com.whip.app.domain.TrackCsvImportConflictKind
import com.whip.app.domain.TrackCsvMapping
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackDefinitionBoundary
import com.whip.app.domain.TrackDefinitionConflictException
import com.whip.app.domain.TrackDefinitionConflictKind
import com.whip.app.domain.TrackDefinitionRemovalReview
import com.whip.app.domain.TrackEntryBoundary
import com.whip.app.domain.TrackEntryConflictException
import com.whip.app.domain.TrackEntryConflictKind
import com.whip.app.domain.TrackEntryCreatePreparation
import com.whip.app.domain.TrackEntryCreateRequest
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackEntryEditSnapshot
import com.whip.app.domain.TrackEntryFormSnapshot
import com.whip.app.domain.TrackEntryMutationKind
import com.whip.app.domain.TrackEntryMutationReceipt
import com.whip.app.domain.TrackEntryPage
import com.whip.app.domain.TrackField
import com.whip.app.domain.TrackProjection
import com.whip.app.domain.TrackCondition
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.matches
import com.whip.app.domain.previewTrackCsvImport
import com.whip.app.domain.receiptEnvelope
import com.whip.app.domain.trackCsvPayloadFingerprint
import com.whip.app.domain.trackCsvHeaders
import java.io.ByteArrayOutputStream
import java.io.Serializable
import java.io.Writer
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class TrackUiState(
    val projections: List<TrackProjection> = emptyList(),
    val currentDate: LocalDate = LocalDate.now(),
    val loading: Boolean = true,
    val errorMessage: String? = null,
    /** Identifies the explicit load attempt that produced this state. */
    val lookupGeneration: Int = 0,
) {
    val active: List<TrackProjection> get() = projections.filterNot { it.track.archived }
    val archived: List<TrackProjection> get() = projections.filter { it.track.archived }
    val pinned: List<TrackProjection> get() = active.filter { it.track.pinned }
    fun track(id: Long): TrackProjection? = projections.firstOrNull { it.track.id == id }
    fun forArea(areaId: String?): TrackUiState = if (areaId == null) this else copy(
        projections = projections.filter { it.track.areaId == areaId },
    )
}

data class PendingTrackEntryUndo(
    val token: Long,
    val deletedEntry: DeletedTrackEntry,
)

internal data class TrackEntryUndoUiState(
    val token: Long? = null,
    /** Exact immutable deletion snapshot retained across a failed restore. */
    val deletedEntry: DeletedTrackEntry? = null,
    val status: OperationStatus = OperationStatus.Idle,
)

internal fun supersededEntryUndoState(
    pending: PendingTrackEntryUndo?,
): TrackEntryUndoUiState? = pending?.let {
    TrackEntryUndoUiState(
        token = it.token,
        deletedEntry = null,
        status = OperationStatus.Failed(
            "Undo is no longer available because another Entry deletion is finishing. " +
                "The previous Entry was not restored.",
        ),
    )
}

internal fun entryUndoStateAfterNewDeletionRecorded(
    current: TrackEntryUndoUiState,
): TrackEntryUndoUiState = if (
    current.status is OperationStatus.Failed && current.deletedEntry == null
) {
    TrackEntryUndoUiState()
} else {
    current
}

internal data class TrackEntryPreparationUiState(
    val sessionId: Long? = null,
    val trackId: Long? = null,
    val entryId: Long? = null,
    val loading: Boolean = false,
    val createPreparation: TrackEntryCreatePreparation? = null,
    val editSnapshot: TrackEntryEditSnapshot? = null,
    val errorMessage: String? = null,
    val conflictKind: TrackEntryConflictKind? = null,
)

internal data class TrackEntryConflictUiState(
    val requestId: String,
    val kind: TrackEntryConflictKind,
    val message: String,
)

internal data class TrackEntryDeletePreparationUiState(
    val sessionId: Long? = null,
    val entryId: Long? = null,
    val loading: Boolean = false,
    val snapshot: TrackEntryEditSnapshot? = null,
    val errorMessage: String? = null,
)

internal data class TrackDefinitionReviewUiState(
    val sessionId: Long? = null,
    val trackId: Long? = null,
    val loading: Boolean = false,
    val boundary: TrackDefinitionBoundary? = null,
    val review: TrackDefinitionRemovalReview? = null,
    val reviewedDraft: TrackDraft? = null,
    val reviewedChoiceReplacementIds: Map<Long, Long> = emptyMap(),
    val errorMessage: String? = null,
    val targetMissing: Boolean = false,
    val conflictKind: TrackDefinitionConflictKind? = null,
)

private data class TrackDefinitionBoundaryLookup(val boundary: TrackDefinitionBoundary?)
private data class TrackEntryPreparationLookup(
    val createPreparation: TrackEntryCreatePreparation? = null,
    val editSnapshot: TrackEntryEditSnapshot? = null,
)
private data class TrackCsvImportFormLookup(val form: TrackEntryFormSnapshot?)

private fun TrackDefinitionConflictKind.requiresCopyRecovery(): Boolean = this in setOf(
    TrackDefinitionConflictKind.TargetMissing,
    TrackDefinitionConflictKind.IdentityChanged,
    TrackDefinitionConflictKind.DefinitionChanged,
)

internal fun definitionConflictBelongsToEditor(
    current: TrackDefinitionReviewUiState,
    sessionId: Long,
    trackId: Long,
    expectedBoundary: TrackDefinitionBoundary,
): Boolean = current.sessionId == sessionId &&
    current.trackId == trackId &&
    current.boundary == expectedBoundary

internal const val TRACK_CSV_MAX_FILE_BYTES = 5 * 1024 * 1024
internal const val TRACK_CSV_MAX_DATA_ROWS = 5_000
internal const val TRACK_CSV_MAX_COLUMNS = 100
internal const val TRACK_CSV_MAX_PREVIEW_CELLS = 100_000
internal const val TRACK_CSV_MAX_DISPLAYED_ISSUES = 50
private const val TRACK_CSV_MAX_PHYSICAL_RECORDS = TRACK_CSV_MAX_DATA_ROWS + 2

internal data class TrackCsvEnvelope(val dataRows: Int, val maximumColumns: Int)

/** Compact process-restorable import state. The CSV payload always remains behind [uri]. */
internal data class TrackCsvImportSessionDescriptor(
    val trackId: Long,
    val trackName: String,
    val trackUuid: String,
    val trackCreatedAtMillis: Long,
    val batchUuid: String,
    val uri: String,
    val fileLabel: String,
    val todayEpochDay: Long,
    val dataGeneration: Long = 0L,
    val previewRevision: Long = 0L,
    val rawPayloadFingerprint: String? = null,
    val preparedRequestFingerprint: String? = null,
    val preparedEntryCount: Int? = null,
    val preparedReceiptEnvelope: TrackCsvImportReceiptEnvelope? = null,
    val commitAttempted: Boolean = false,
    val entryDateColumn: String? = null,
    val fieldColumns: Map<String, String> = emptyMap(),
    val numberUnitColumns: Map<String, String> = emptyMap(),
    val mappingInitialized: Boolean = false,
) : Serializable {
    val mapping: TrackCsvMapping
        get() = TrackCsvMapping(entryDateColumn, fieldColumns, numberUnitColumns)

    fun withMapping(value: TrackCsvMapping) = copy(
        previewRevision = previewRevision + 1L,
        preparedRequestFingerprint = null,
        preparedEntryCount = null,
        preparedReceiptEnvelope = null,
        entryDateColumn = value.entryDateColumn,
        fieldColumns = LinkedHashMap(value.fieldColumns),
        numberUnitColumns = LinkedHashMap(value.numberUnitColumns),
        mappingInitialized = true,
    )

    fun withLoadedPayload(
        payloadFingerprint: String,
        value: TrackCsvMapping,
    ) = copy(
        previewRevision = previewRevision + 1L,
        rawPayloadFingerprint = payloadFingerprint,
        preparedRequestFingerprint = null,
        preparedEntryCount = null,
        preparedReceiptEnvelope = null,
        entryDateColumn = value.entryDateColumn,
        fieldColumns = LinkedHashMap(value.fieldColumns),
        numberUnitColumns = LinkedHashMap(value.numberUnitColumns),
        mappingInitialized = true,
    )

    fun withPreparation(value: TrackCsvImportPreparation) = copy(
        preparedRequestFingerprint = value.request.requestFingerprint,
        preparedEntryCount = value.request.rowCount,
        preparedReceiptEnvelope = value.request.receiptEnvelope(),
    )

    fun withCommitAttempted() = copy(commitAttempted = true)
}

internal class TrackCsvImportSessionStore(
    private val savedStateHandle: SavedStateHandle,
    private val currentDataGeneration: () -> Long = { 0L },
) {
    constructor(
        savedStateHandle: SavedStateHandle,
        currentDataGeneration: Long,
    ) : this(savedStateHandle, { currentDataGeneration })

    val descriptor: TrackCsvImportSessionDescriptor?
        get() {
            val restored = savedStateHandle.get<TrackCsvImportSessionDescriptor>(STATE_KEY) ?: return null
            if (restored.dataGeneration == currentDataGeneration()) return restored
            clear()
            return null
        }

    fun begin(
        trackId: Long,
        trackName: String,
        trackUuid: String,
        trackCreatedAtMillis: Long,
        batchUuid: String,
        uri: String,
        fileLabel: String,
        today: LocalDate,
    ): TrackCsvImportSessionDescriptor {
        val value = TrackCsvImportSessionDescriptor(
            trackId = trackId,
            trackName = trackName,
            trackUuid = trackUuid,
            trackCreatedAtMillis = trackCreatedAtMillis,
            batchUuid = batchUuid,
            uri = uri,
            fileLabel = fileLabel,
            todayEpochDay = today.toEpochDay(),
            dataGeneration = currentDataGeneration(),
        )
        savedStateHandle[STATE_KEY] = value
        return value
    }

    fun updateMapping(mapping: TrackCsvMapping): TrackCsvImportSessionDescriptor? = descriptor?.withMapping(mapping)?.also {
        savedStateHandle[STATE_KEY] = it
    }

    fun recordLoadedPayload(
        expected: TrackCsvImportSessionDescriptor,
        payloadFingerprint: String,
        mapping: TrackCsvMapping,
    ): TrackCsvImportSessionDescriptor? {
        val current = descriptor?.takeIf {
            it.batchUuid == expected.batchUuid &&
                it.previewRevision == expected.previewRevision &&
                it.dataGeneration == expected.dataGeneration
        } ?: return null
        return current.withLoadedPayload(payloadFingerprint, mapping).also {
            savedStateHandle[STATE_KEY] = it
        }
    }

    fun recordPreparation(
        expected: TrackCsvImportSessionDescriptor,
        preparation: TrackCsvImportPreparation,
    ): TrackCsvImportSessionDescriptor? {
        val current = descriptor?.takeIf {
            it.batchUuid == expected.batchUuid &&
                it.previewRevision == expected.previewRevision &&
                it.rawPayloadFingerprint == preparation.request.payloadFingerprint &&
                it.batchUuid == preparation.request.batchUuid &&
                it.trackId == preparation.request.openingFormBoundary.trackId &&
                it.trackUuid == preparation.request.openingFormBoundary.trackUuid &&
                it.trackCreatedAtMillis == preparation.request.openingFormBoundary.trackCreatedAtMillis &&
                it.mapping == preparation.request.mapping &&
                it.todayEpochDay == preparation.request.defaultEntryDate.toEpochDay() &&
                it.dataGeneration == expected.dataGeneration
        } ?: return null
        return current.withPreparation(preparation).also {
            savedStateHandle[STATE_KEY] = it
        }
    }

    fun recordCommitAttempt(expected: TrackCsvImportCommitIdentity): TrackCsvImportSessionDescriptor? {
        val current = descriptor?.takeIf {
            it.trackId == expected.trackId &&
                it.batchUuid == expected.batchUuid &&
                it.previewRevision == expected.previewRevision &&
                it.dataGeneration == expected.dataGeneration &&
                it.preparedReceiptEnvelope == expected.receiptEnvelope
        } ?: return null
        return current.withCommitAttempted().also {
            savedStateHandle[STATE_KEY] = it
        }
    }

    fun clear() {
        savedStateHandle.remove<TrackCsvImportSessionDescriptor>(STATE_KEY)
    }

    private companion object {
        const val STATE_KEY = "track-csv-import-session"
    }
}

internal fun TrackCsvImportSessionDescriptor.restoredUiState() = TrackCsvImportUiState(
    trackId = trackId,
    trackName = trackName,
    batchUuid = batchUuid,
    fileLabel = fileLabel,
    fallbackDate = LocalDate.ofEpochDay(todayEpochDay),
    dataGeneration = dataGeneration,
    previewRevision = previewRevision,
    phase = TrackCsvImportPhase.Reading,
    mapping = mapping,
    commitAttempted = commitAttempted,
)

internal fun TrackCsvImportSessionDescriptor.ownsTrackIdentity(form: TrackEntryFormSnapshot): Boolean =
    trackId == form.track.id && trackUuid == form.track.uuid &&
        trackCreatedAtMillis == form.track.createdAtMillis

internal suspend fun reloadTrackCsvText(
    descriptor: TrackCsvImportSessionDescriptor,
    readUri: suspend (String) -> String,
): String = readUri(descriptor.uri)

/** Strictly decodes document bytes so binary or damaged text never becomes silently altered Entry data. */
internal fun decodeTrackCsvUtf8(bytes: ByteArray): String {
    val decoded = try {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (error: CharacterCodingException) {
        throw IllegalArgumentException(
            "Choose a CSV saved as valid UTF-8 text. This file contains damaged or unsupported text encoding.",
            error,
        )
    }
    require('\u0000' !in decoded) {
        "Choose a text CSV. This file contains binary NUL characters."
    }
    return decoded.removePrefix("\uFEFF")
}

/** Fast allocation-free guard run before the full RFC-4180 parser. */
internal fun validateTrackCsvEnvelope(text: String): TrackCsvEnvelope {
    require(text.toByteArray(Charsets.UTF_8).size <= TRACK_CSV_MAX_FILE_BYTES) {
        "Choose a CSV smaller than 5 MB."
    }
    var quoted = false
    var columns = 1
    var maximumColumns = 0
    var records = 0
    var trailingBareBlankRecords = 0
    var recordStarted = false
    var recordCanBeIgnoredAsBareBlank = true
    var index = 0
    fun finishRecord() {
        records++
        maximumColumns = maxOf(maximumColumns, columns)
        trailingBareBlankRecords = if (recordCanBeIgnoredAsBareBlank && columns == 1) {
            trailingBareBlankRecords + 1
        } else {
            0
        }
        require(records <= TRACK_CSV_MAX_PHYSICAL_RECORDS) {
            "This CSV has too many physical rows, including extra blank lines. Remove blank lines or split the file and try again."
        }
        require(records - trailingBareBlankRecords <= TRACK_CSV_MAX_DATA_ROWS + 1) {
            "This CSV has more than 5,000 data rows. Split it into smaller files and import them separately."
        }
        columns = 1
        recordStarted = false
        recordCanBeIgnoredAsBareBlank = true
    }
    while (index < text.length) {
        val char = text[index]
        when {
            quoted && char == '"' && index + 1 < text.length && text[index + 1] == '"' -> {
                recordStarted = true
                recordCanBeIgnoredAsBareBlank = false
                index++
            }
            char == '"' -> {
                recordStarted = true
                recordCanBeIgnoredAsBareBlank = false
                quoted = !quoted
            }
            !quoted && char == ',' -> {
                recordStarted = true
                recordCanBeIgnoredAsBareBlank = false
                columns++
                require(columns <= TRACK_CSV_MAX_COLUMNS) {
                    "This CSV has more than 100 columns. Remove unused columns and try again."
                }
            }
            !quoted && (char == '\n' || char == '\r') -> {
                if (char == '\r' && index + 1 < text.length && text[index + 1] == '\n') index++
                finishRecord()
            }
            else -> {
                recordStarted = true
                if (!char.isWhitespace()) recordCanBeIgnoredAsBareBlank = false
            }
        }
        index++
    }
    require(!quoted) { "The CSV contains an unclosed quoted value. Fix the quoted cell and try again." }
    if (recordStarted || columns > 1) finishRecord()
    val envelope = TrackCsvEnvelope(
        dataRows = (records - trailingBareBlankRecords - 1).coerceAtLeast(0),
        maximumColumns = maximumColumns,
    )
    require(envelope.dataRows.toLong() * envelope.maximumColumns <= TRACK_CSV_MAX_PREVIEW_CELLS) {
        "This CSV preview would inspect more than 100,000 cells. Remove unused columns or split the file into smaller imports."
    }
    return envelope
}

internal fun defaultTrackCsvMapping(projection: TrackProjection, headers: List<String>) =
    defaultTrackCsvMapping(projection.fields, headers)

private fun defaultTrackCsvMapping(fields: List<TrackField>, headers: List<String>) = TrackCsvMapping(
    entryDateColumn = headers.firstOrNull { it.equals("Entry Date", true) },
    fieldColumns = fields.mapNotNull { field ->
        headers.firstOrNull {
            it.equals(field.name, true) ||
                field.type == TrackFieldType.Number && it.equals("${field.name} (Entered)", true)
        }?.let { field.uuid to it }
    }.toMap(),
    numberUnitColumns = fields.filter { it.type == TrackFieldType.Number }.mapNotNull { field ->
        headers.firstOrNull { it.equals("${field.name} (Unit)", true) }?.let { field.uuid to it }
    }.toMap(),
)

internal enum class TrackCsvImportPhase { Idle, Reading, Previewing, Ready, Complete, Error }

internal data class TrackCsvImportUiState(
    val trackId: Long? = null,
    val trackName: String = "",
    val batchUuid: String? = null,
    val fileLabel: String = "",
    val fallbackDate: LocalDate? = null,
    val dataGeneration: Long? = null,
    val previewRevision: Long = 0L,
    val phase: TrackCsvImportPhase = TrackCsvImportPhase.Idle,
    val headers: List<String> = emptyList(),
    val mapping: TrackCsvMapping = TrackCsvMapping(),
    val openingForm: TrackEntryFormSnapshot? = null,
    val preview: TrackCsvImportPreview? = null,
    val preparation: TrackCsvImportPreparation? = null,
    val completionReceipt: TrackCsvImportReceipt? = null,
    val recoveryNotice: String? = null,
    val requiresNewFile: Boolean = false,
    val commitAttempted: Boolean = false,
    val errorMessage: String? = null,
)

internal data class TrackCsvImportCommitIdentity(
    val trackId: Long,
    val batchUuid: String,
    val previewRevision: Long,
    val dataGeneration: Long,
    val requestFingerprint: String,
    val rowCount: Int,
    val receiptEnvelope: TrackCsvImportReceiptEnvelope,
)

internal fun TrackCsvImportUiState.commitIdentityOrNull(): TrackCsvImportCommitIdentity? {
    val prepared = preparation?.request ?: return null
    if (
        phase != TrackCsvImportPhase.Ready || preview?.invalidRows != 0 ||
        preview.validRows <= 0 || preview.validRows != prepared.rowCount
    ) return null
    return TrackCsvImportCommitIdentity(
        trackId = trackId ?: return null,
        batchUuid = batchUuid ?: return null,
        previewRevision = previewRevision,
        dataGeneration = dataGeneration ?: return null,
        requestFingerprint = prepared.requestFingerprint,
        rowCount = prepared.rowCount,
        receiptEnvelope = prepared.receiptEnvelope(),
    )
}

internal fun TrackCsvImportReceipt.matches(expected: TrackCsvImportCommitIdentity): Boolean =
    matches(expected.receiptEnvelope)

private fun TrackCsvImportSessionDescriptor?.matchesSession(
    expected: TrackCsvImportSessionDescriptor,
): Boolean = this != null &&
    trackId == expected.trackId &&
    batchUuid == expected.batchUuid &&
    previewRevision == expected.previewRevision &&
    dataGeneration == expected.dataGeneration

private fun TrackCsvImportSessionDescriptor.uiState(
    phase: TrackCsvImportPhase,
    headers: List<String> = emptyList(),
    openingForm: TrackEntryFormSnapshot? = null,
    preview: TrackCsvImportPreview? = null,
    preparation: TrackCsvImportPreparation? = null,
    completionReceipt: TrackCsvImportReceipt? = null,
    recoveryNotice: String? = null,
    requiresNewFile: Boolean = false,
    errorMessage: String? = null,
) = TrackCsvImportUiState(
    trackId = trackId,
    trackName = trackName,
    batchUuid = batchUuid,
    fileLabel = fileLabel,
    fallbackDate = LocalDate.ofEpochDay(todayEpochDay),
    dataGeneration = dataGeneration,
    previewRevision = previewRevision,
    phase = phase,
    headers = headers,
    mapping = mapping,
    openingForm = openingForm,
    preview = preview,
    preparation = preparation,
    completionReceipt = completionReceipt,
    recoveryNotice = recoveryNotice,
    requiresNewFile = requiresNewFile,
    commitAttempted = commitAttempted,
    errorMessage = errorMessage,
)

internal fun trackCsvPreviewCompletion(
    current: TrackCsvImportUiState,
    result: Result<TrackCsvImportPreview>,
): TrackCsvImportUiState = current.copy(
    phase = if (result.isSuccess) TrackCsvImportPhase.Ready else TrackCsvImportPhase.Error,
    preview = result.getOrNull(),
    errorMessage = result.exceptionOrNull()?.let { error ->
        error.message ?: "Could not build the CSV preview. Check the column mapping or choose another file."
    },
)

internal fun canDismissTrackCsvImport(databaseImportRunning: Boolean): Boolean = !databaseImportRunning

internal suspend fun writeTrackCsvChunks(csv: String, writer: Writer) {
    var offset = 0
    while (offset < csv.length) {
        currentCoroutineContext().ensureActive()
        val count = minOf(16 * 1024, csv.length - offset)
        writer.write(csv, offset, count)
        offset += count
    }
}

internal enum class TrackCsvExportPhase { Idle, Writing, Complete, Error }

internal data class TrackCsvExportUiState(
    val trackId: Long? = null,
    val phase: TrackCsvExportPhase = TrackCsvExportPhase.Idle,
    val errorMessage: String? = null,
)

class TrackViewModel private constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    projectionSourceOverride: kotlinx.coroutines.flow.Flow<List<TrackProjection>>?,
) : AndroidViewModel(application) {
    constructor(
        application: Application,
        savedStateHandle: SavedStateHandle,
    ) : this(application, savedStateHandle, null)

    /** Test seam for exercising projection-load failure and retry with the real ViewModel coordinator. */
    internal constructor(
        application: Application,
        savedStateHandle: SavedStateHandle,
        projectionSource: kotlinx.coroutines.flow.Flow<List<TrackProjection>>,
        @Suppress("UNUSED_PARAMETER") testOverride: Boolean,
    ) : this(application, savedStateHandle, projectionSource)

    private val app = application as WhipApplication
    private val repository: TrackRepository = app.trackRepository
    private val projectionSource = projectionSourceOverride ?: repository.projections
    private val csvImportSessionStore = TrackCsvImportSessionStore(
        savedStateHandle,
        currentDataGeneration = app::currentUserDataGeneration,
    )
    private val restoredCsvImportSession = csvImportSessionStore.descriptor
    private val _operationStatus = MutableStateFlow<OperationStatus>(OperationStatus.Idle)
    val operationStatus: StateFlow<OperationStatus> = _operationStatus.asStateFlow()
    private val _definitionSaveState = MutableStateFlow<PersistenceRequestState<EntitySaveReceipt>>(
        PersistenceRequestState.Idle,
    )
    val definitionSaveState: StateFlow<PersistenceRequestState<EntitySaveReceipt>> =
        _definitionSaveState.asStateFlow()
    private val _entryMutationState = MutableStateFlow<PersistenceRequestState<TrackEntryMutationReceipt>>(
        PersistenceRequestState.Idle,
    )
    val entryMutationState: StateFlow<PersistenceRequestState<TrackEntryMutationReceipt>> =
        _entryMutationState.asStateFlow()
    private val _csvImportRequestState = MutableStateFlow<PersistenceRequestState<TrackCsvImportReceipt>>(
        PersistenceRequestState.Idle,
    )
    internal val csvImportRequestState: StateFlow<PersistenceRequestState<TrackCsvImportReceipt>> =
        _csvImportRequestState.asStateFlow()
    private val _entryPreparationState = MutableStateFlow(TrackEntryPreparationUiState())
    internal val entryPreparationState: StateFlow<TrackEntryPreparationUiState> =
        _entryPreparationState.asStateFlow()
    private val _entryConflictState = MutableStateFlow<TrackEntryConflictUiState?>(null)
    internal val entryConflictState: StateFlow<TrackEntryConflictUiState?> = _entryConflictState.asStateFlow()
    private val _entryDeletePreparationState = MutableStateFlow(TrackEntryDeletePreparationUiState())
    internal val entryDeletePreparationState: StateFlow<TrackEntryDeletePreparationUiState> =
        _entryDeletePreparationState.asStateFlow()
    private val _definitionReviewState = MutableStateFlow(TrackDefinitionReviewUiState())
    internal val definitionReviewState: StateFlow<TrackDefinitionReviewUiState> =
        _definitionReviewState.asStateFlow()
    private var definitionReviewGeneration = 0L
    private var entryPreparationGeneration = 0L
    private var entryDeletePreparationGeneration = 0L
    private val operationMutex = Mutex()
    private var recoveryAcknowledgement: CompletableDeferred<Unit>? = null
    private val _lastDeletedEntry = MutableStateFlow<PendingTrackEntryUndo?>(null)
    val lastDeletedEntry: StateFlow<PendingTrackEntryUndo?> = _lastDeletedEntry.asStateFlow()
    private val _entryUndoState = MutableStateFlow(TrackEntryUndoUiState())
    internal val entryUndoState: StateFlow<TrackEntryUndoUiState> = _entryUndoState.asStateFlow()
    private var nextEntryUndoToken = 0L
    private val reloadKey = MutableStateFlow(0)
    private val _csvImportState = MutableStateFlow(
        restoredCsvImportSession?.restoredUiState() ?: TrackCsvImportUiState(),
    )
    internal val csvImportState: StateFlow<TrackCsvImportUiState> = _csvImportState.asStateFlow()
    private val _csvExportState = MutableStateFlow(TrackCsvExportUiState())
    internal val csvExportState: StateFlow<TrackCsvExportUiState> = _csvExportState.asStateFlow()
    private var csvImportJob: Job? = null
    private var csvImportRestoreJob: Job? = null
    private var csvExportJob: Job? = null
    private var importCsvText: String? = null
    private var importCsvForm: TrackEntryFormSnapshot? = null
    private var importUri: Uri? = restoredCsvImportSession?.uri?.let(Uri::parse)
    private var importToday: LocalDate? = restoredCsvImportSession?.todayEpochDay?.let(LocalDate::ofEpochDay)
    private var exportUri: Uri? = null

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TrackUiState> = reloadKey.flatMapLatest { lookupGeneration ->
        combine(projectionSource, app.calendarContext) { projections, calendar ->
            TrackUiState(
                projections = projections,
                currentDate = calendar.logicalDate,
                loading = false,
                lookupGeneration = lookupGeneration,
            )
        }.catch { error ->
            emit(
                TrackUiState(
                    currentDate = app.calendarContext.value.logicalDate,
                    loading = false,
                    errorMessage = error.message ?: "Could not load Tracks",
                    lookupGeneration = lookupGeneration,
                ),
            )
        }.onStart {
            emit(
                TrackUiState(
                    currentDate = app.calendarContext.value.logicalDate,
                    loading = true,
                    lookupGeneration = lookupGeneration,
                ),
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TrackUiState(currentDate = app.calendarContext.value.logicalDate),
    )

    init {
        restoredCsvImportSession?.let(::restoreCsvImportSession)
        viewModelScope.launch {
            app.userDataGeneration.drop(1).collect {
                _lastDeletedEntry.value = null
                _entryUndoState.value = TrackEntryUndoUiState()
                recoveryAcknowledgement?.complete(Unit)
                recoveryAcknowledgement = null
                _operationStatus.value = OperationStatus.Idle
                _definitionSaveState.value = PersistenceRequestState.Idle
                _entryMutationState.value = PersistenceRequestState.Idle
                _csvImportRequestState.value = PersistenceRequestState.Idle
                entryPreparationGeneration++
                _entryPreparationState.value = TrackEntryPreparationUiState()
                _entryConflictState.value = null
                entryDeletePreparationGeneration++
                _entryDeletePreparationState.value = TrackEntryDeletePreparationUiState()
                definitionReviewGeneration++
                _definitionReviewState.value = TrackDefinitionReviewUiState()
                clearCsvImportSession()
                cancelCsvExport()
            }
        }
    }

    fun consumeOperationStatus() {
        _operationStatus.value = OperationStatus.Idle
        recoveryAcknowledgement?.complete(Unit)
        recoveryAcknowledgement = null
    }

    fun retryLoading() { requestTrackReload() }

    private fun requestTrackReload(): Int = (reloadKey.value + 1).also { nextGeneration ->
        reloadKey.value = nextGeneration
    }

    fun consumeDefinitionSaveResult(requestId: String) {
        if ((_definitionSaveState.value as? PersistenceRequestState.Finished)?.requestId == requestId) {
            _definitionSaveState.value = PersistenceRequestState.Idle
        }
    }

    fun consumeEntryMutationResult(requestId: String) {
        if ((_entryMutationState.value as? PersistenceRequestState.Finished)?.requestId == requestId) {
            _entryMutationState.value = PersistenceRequestState.Idle
        }
    }

    fun consumeCsvImportResult(requestId: String) {
        if ((_csvImportRequestState.value as? PersistenceRequestState.Finished)?.requestId == requestId) {
            _csvImportRequestState.value = PersistenceRequestState.Idle
        }
    }

    fun prepareEntryEditor(sessionId: Long, trackId: Long, entryId: Long?) {
        val current = _entryPreparationState.value
        if (
            current.sessionId == sessionId &&
            current.trackId == trackId &&
            current.entryId == entryId &&
            (current.loading || current.createPreparation != null || current.editSnapshot != null || current.conflictKind != null)
        ) return
        val generation = ++entryPreparationGeneration
        _entryPreparationState.value = TrackEntryPreparationUiState(
            sessionId = sessionId,
            trackId = trackId,
            entryId = entryId,
            loading = true,
        )
        viewModelScope.launch {
            val result = runCatching {
                checkNotNull(app.withUserDataAccess {
                    if (entryId == null) {
                        TrackEntryPreparationLookup(createPreparation = repository.prepareEntryCreate(trackId))
                    } else {
                        TrackEntryPreparationLookup(editSnapshot = repository.prepareEntryEdit(entryId))
                    }
                }) { "Whip data is unavailable while recovery is in progress" }
            }
            if (entryPreparationGeneration != generation) return@launch
            result.fold(
                onSuccess = { lookup ->
                    val createPreparation = lookup.createPreparation?.takeIf {
                        it.request.openingFormBoundary.trackId == trackId && it.form.track.id == trackId
                    }
                    val editSnapshot = lookup.editSnapshot?.takeIf {
                        it.boundary.formBoundary.trackId == trackId && it.boundary.entryId == entryId
                    }
                    val missing = if (entryId == null) createPreparation == null else editSnapshot == null
                    _entryPreparationState.value = TrackEntryPreparationUiState(
                        sessionId = sessionId,
                        trackId = trackId,
                        entryId = entryId,
                        createPreparation = createPreparation,
                        editSnapshot = editSnapshot,
                        errorMessage = if (missing) {
                            if (entryId == null) "This Track is no longer available. No Entry was added."
                            else "This Entry is no longer available. It was not reinterpreted as a new Entry."
                        } else null,
                        conflictKind = if (missing) {
                            if (entryId == null) TrackEntryConflictKind.ParentMissing
                            else TrackEntryConflictKind.TargetMissing
                        } else null,
                    )
                },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    val conflict = (error as? TrackEntryConflictException)?.kind
                    _entryPreparationState.value = TrackEntryPreparationUiState(
                        sessionId = sessionId,
                        trackId = trackId,
                        entryId = entryId,
                        errorMessage = error.message ?: "Could not verify this Entry.",
                        conflictKind = conflict,
                    )
                },
            )
        }
    }

    fun clearEntryEditorState(sessionId: Long? = null) {
        if (sessionId != null && _entryPreparationState.value.sessionId != sessionId) return
        entryPreparationGeneration++
        _entryPreparationState.value = TrackEntryPreparationUiState()
        _entryConflictState.value = null
    }

    fun prepareEntryDelete(sessionId: Long, entryId: Long) {
        val current = _entryDeletePreparationState.value
        if (
            current.sessionId == sessionId && current.entryId == entryId &&
            (current.loading || current.snapshot != null)
        ) return
        val generation = ++entryDeletePreparationGeneration
        _entryDeletePreparationState.value = TrackEntryDeletePreparationUiState(
            sessionId = sessionId,
            entryId = entryId,
            loading = true,
        )
        viewModelScope.launch {
            val result = runCatching {
                checkNotNull(app.withUserDataAccess {
                    TrackEntryPreparationLookup(editSnapshot = repository.prepareEntryEdit(entryId))
                }) {
                    "Whip data is unavailable while recovery is in progress"
                }.editSnapshot
            }
            if (entryDeletePreparationGeneration != generation) return@launch
            result.fold(
                onSuccess = { snapshot ->
                    _entryDeletePreparationState.value = TrackEntryDeletePreparationUiState(
                        sessionId = sessionId,
                        entryId = entryId,
                        snapshot = snapshot,
                        errorMessage = if (snapshot == null) "This Entry is no longer available." else null,
                    )
                },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    _entryDeletePreparationState.value = TrackEntryDeletePreparationUiState(
                        sessionId = sessionId,
                        entryId = entryId,
                        errorMessage = error.message ?: "The Entry could not be prepared for deletion.",
                    )
                },
            )
        }
    }

    fun clearEntryDeletePreparation(sessionId: Long? = null) {
        if (sessionId != null && _entryDeletePreparationState.value.sessionId != sessionId) return
        entryDeletePreparationGeneration++
        _entryDeletePreparationState.value = TrackEntryDeletePreparationUiState()
        _entryConflictState.value = null
    }

    fun prepareDefinitionEditor(
        sessionId: Long,
        trackId: Long,
        openingDraft: TrackDraft,
    ) {
        val current = _definitionReviewState.value
        if (
            current.sessionId == sessionId &&
            current.trackId == trackId &&
            (current.loading || current.boundary != null || current.targetMissing)
        ) return
        val generation = ++definitionReviewGeneration
        _definitionReviewState.value = TrackDefinitionReviewUiState(
            sessionId = sessionId,
            trackId = trackId,
            loading = true,
        )
        viewModelScope.launch {
            val result = runCatching {
                checkNotNull(app.withUserDataAccess {
                    TrackDefinitionBoundaryLookup(
                        repository.definitionBoundary(trackId, openingDraft),
                    )
                }) { "Whip data is unavailable while recovery is in progress" }.boundary
            }
            if (definitionReviewGeneration != generation) return@launch
            result.fold(
                onSuccess = { boundary ->
                    _definitionReviewState.value = TrackDefinitionReviewUiState(
                        sessionId = sessionId,
                        trackId = trackId,
                        boundary = boundary,
                        targetMissing = boundary == null,
                        conflictKind = TrackDefinitionConflictKind.TargetMissing.takeIf { boundary == null },
                        errorMessage = if (boundary == null) {
                            "This Track is no longer available. Your draft is still here."
                        } else null,
                    )
                },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    val conflict = (error as? TrackDefinitionConflictException)?.kind
                    _definitionReviewState.value = TrackDefinitionReviewUiState(
                        sessionId = sessionId,
                        trackId = trackId,
                        errorMessage = error.message ?: "Could not verify the Track definition.",
                        targetMissing = conflict == TrackDefinitionConflictKind.TargetMissing,
                        conflictKind = conflict,
                    )
                },
            )
        }
    }

    fun reviewDefinitionUpdate(
        sessionId: Long,
        trackId: Long,
        draft: TrackDraft,
        expectedBoundary: TrackDefinitionBoundary,
        choiceReplacementIds: Map<Long, Long>,
    ) {
        val generation = ++definitionReviewGeneration
        _definitionReviewState.value = TrackDefinitionReviewUiState(
            sessionId = sessionId,
            trackId = trackId,
            loading = true,
            boundary = expectedBoundary,
            reviewedDraft = draft,
            reviewedChoiceReplacementIds = choiceReplacementIds,
        )
        viewModelScope.launch {
            val result = runCatching {
                checkNotNull(app.withUserDataAccess {
                    repository.reviewDefinitionUpdate(
                        trackId,
                        draft,
                        expectedBoundary,
                        choiceReplacementIds,
                    )
                }) { "Whip data is unavailable while recovery is in progress" }
            }
            if (definitionReviewGeneration != generation) return@launch
            result.fold(
                onSuccess = { review ->
                    _definitionReviewState.value = TrackDefinitionReviewUiState(
                        sessionId = sessionId,
                        trackId = trackId,
                        boundary = expectedBoundary,
                        review = review,
                        reviewedDraft = draft,
                        reviewedChoiceReplacementIds = choiceReplacementIds,
                    )
                },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    val conflict = (error as? TrackDefinitionConflictException)?.kind
                    _definitionReviewState.value = TrackDefinitionReviewUiState(
                        sessionId = sessionId,
                        trackId = trackId,
                        boundary = expectedBoundary,
                        reviewedDraft = draft,
                        reviewedChoiceReplacementIds = choiceReplacementIds,
                        errorMessage = error.message ?: "Could not review the Track definition.",
                        targetMissing = conflict == TrackDefinitionConflictKind.TargetMissing,
                        conflictKind = conflict,
                    )
                },
            )
        }
    }

    fun clearDefinitionEditorState(sessionId: Long? = null) {
        if (sessionId != null && _definitionReviewState.value.sessionId != sessionId) return
        definitionReviewGeneration++
        _definitionReviewState.value = TrackDefinitionReviewUiState()
    }

    fun saveTrack(
        id: Long?,
        draft: TrackDraft,
        sessionId: Long,
        expectedBoundary: TrackDefinitionBoundary? = null,
        reviewedRemoval: TrackDefinitionRemovalReview? = null,
        requestId: String,
    ): Boolean {
        if (!_definitionSaveState.tryStartPersistenceRequest(requestId)) return false
        runDefinitionSaveOperation(
            runningMessage = if (id == null) "Creating Track…" else "Saving Track…",
            successMessage = if (id == null) "Track created" else "Track saved",
            requestId = requestId,
        ) {
            completeCommittedEntitySave(
                commit = {
                    val savedId = if (id == null) repository.create(draft) else try {
                        repository.update(
                            id,
                            draft,
                            requireNotNull(expectedBoundary) {
                                "The Track definition has not finished loading. Try again."
                            },
                            reviewedRemoval,
                        )
                        id
                    } catch (conflict: TrackDefinitionConflictException) {
                        if (conflict.kind.requiresCopyRecovery()) {
                            val current = _definitionReviewState.value
                            if (definitionConflictBelongsToEditor(
                                    current,
                                    sessionId,
                                    id,
                                    requireNotNull(expectedBoundary),
                                )
                            ) {
                                _definitionReviewState.value = current.copy(
                                    loading = false,
                                    review = null,
                                    errorMessage = conflict.message,
                                    targetMissing = conflict.kind == TrackDefinitionConflictKind.TargetMissing,
                                    conflictKind = conflict.kind,
                                )
                            }
                        }
                        throw conflict
                    }
                    EntitySaveReceipt(savedId, draft.areaId, areaVerified = false)
                },
                followUp = { committed ->
                    val savedId = requireNotNull(committed.entityId)
                    var resolvedAreaId = committed.areaId
                    var areaVerified = false
                    val warnings = listOfNotNull(
                        saveFollowUpWarning("Saved Area could not be verified; showing All Areas.") {
                            val saved = requireNotNull(repository.projection(savedId)) {
                                "Saved Track could not be reread"
                            }
                            resolvedAreaId = saved.track.areaId
                            areaVerified = true
                        },
                        saveFollowUpWarning("Some tag suggestions did not refresh; the Track itself was saved.") {
                            draft.tags.forEach { app.measurementRepository.ensureTag(it) }
                        },
                    )
                    committed.copy(
                        areaId = resolvedAreaId,
                        warnings = warnings,
                        areaVerified = areaVerified,
                    )
                },
            )
        }
        return true
    }

    fun duplicate(id: Long) = runOperation("Duplicating Track…", "Track structure duplicated") { repository.duplicate(id) }
    fun setPinned(id: Long, pinned: Boolean) = runOperation(
        "Updating Home Quick Log…",
        if (pinned) "Track added to Home Quick Log" else "Track removed from Home Quick Log",
        successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
    ) {
        repository.setPinned(id, pinned)
        if (pinned) app.settingsRepository.revealHomeSection(HomeSection.Tracks)
    }
    fun setPinned(ids: Collection<Long>, pinned: Boolean) = runOperation(
        "Updating Home Quick Log…",
        "${ids.size} Tracks ${if (pinned) "added to" else "removed from"} Home Quick Log",
        successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
    ) {
        app.database.withTransaction { ids.distinct().forEach { repository.setPinned(it, pinned) } }
        if (pinned) app.settingsRepository.revealHomeSection(HomeSection.Tracks)
    }
    fun setArchived(id: Long, archived: Boolean) = runOperation(
        if (archived) "Archiving Track…" else "Restoring Track…",
        if (archived) "Track archived" else "Track restored",
    ) {
        repository.setArchived(id, archived)
    }
    fun setArchived(ids: Collection<Long>, archived: Boolean) = runOperation(
        if (archived) "Archiving Tracks…" else "Restoring Tracks…",
        "${ids.size} Tracks ${if (archived) "archived" else "restored"}",
    ) {
        app.database.withTransaction { ids.distinct().forEach { repository.setArchived(it, archived) } }
    }
    fun reorder(ids: List<Long>) = runSilentReorder { repository.reorder(ids) }
    fun deleteTrack(id: Long) = runOperation(
        "Deleting Track…",
        "Track permanently deleted",
        successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
    ) {
        app.domainDeletionCoordinator.deleteTrack(id)
    }

    fun saveEntry(
        trackId: Long,
        entryId: Long?,
        createRequest: TrackEntryCreateRequest?,
        expectedBoundary: TrackEntryBoundary?,
        draft: TrackEntryDraft,
        requestId: String,
    ): Boolean {
        if (!_entryMutationState.tryStartPersistenceRequest(requestId)) return false
        _entryConflictState.value = null
        viewModelScope.launch {
            val result = try {
                val receipt = checkNotNull(app.withUserDataAccess {
                    operationMutex.withLock {
                        if (entryId == null) {
                            val request = requireNotNull(createRequest) {
                                "The Entry form has not finished loading. Try again."
                            }
                            require(request.openingFormBoundary.trackId == trackId) {
                                "The Entry form belongs to a different Track."
                            }
                            repository.addEntry(request, draft)
                        } else {
                            val boundary = requireNotNull(expectedBoundary) {
                                "The Entry has not finished loading. Try again."
                            }
                            require(boundary.formBoundary.trackId == trackId && boundary.entryId == entryId) {
                                "The Entry editor no longer matches its saved target."
                            }
                            repository.updateEntry(boundary, draft)
                        }
                    }
                }) { "Whip data is unavailable while recovery is in progress" }
                WhipResult.Success(receipt)
            } catch (cancelled: CancellationException) {
                if (currentCoroutineContext().isActive) {
                    WhipResult.Failure(
                        "The Entry change was interrupted. Your draft is still here; verify history before retrying.",
                        cancelled,
                    )
                } else {
                    if ((_entryMutationState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                        _entryMutationState.value = PersistenceRequestState.Idle
                    }
                    throw cancelled
                }
            } catch (error: Exception) {
                (error as? TrackEntryConflictException)?.let { conflict ->
                    _entryConflictState.value = TrackEntryConflictUiState(
                        requestId = requestId,
                        kind = conflict.kind,
                        message = conflict.message ?: "The Entry changed before this request could finish.",
                    )
                }
                WhipResult.Failure(error.message ?: "The Entry could not be saved.", error)
            }
            if ((_entryMutationState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                _entryMutationState.value = PersistenceRequestState.Finished(requestId, result)
            }
        }
        return true
    }

    internal fun importEntries(expected: TrackCsvImportCommitIdentity, requestId: String): Boolean {
        val current = _csvImportState.value
        val preparation = current.preparation ?: return false
        val drafts = current.preview?.validDrafts ?: return false
        if (current.commitIdentityOrNull() != expected) return false
        val session = csvImportSessionStore.descriptor ?: return false
        if (
            session.trackId != expected.trackId ||
            session.batchUuid != expected.batchUuid ||
            session.previewRevision != expected.previewRevision ||
            session.dataGeneration != expected.dataGeneration ||
            session.preparedReceiptEnvelope != expected.receiptEnvelope ||
            preparation.request.batchUuid != expected.batchUuid ||
            preparation.request.receiptEnvelope() != expected.receiptEnvelope
        ) return false
        if (!_csvImportRequestState.tryStartPersistenceRequest(requestId)) return false
        val attempted = csvImportSessionStore.recordCommitAttempt(expected)
        if (attempted == null) {
            _csvImportRequestState.value = PersistenceRequestState.Finished(
                requestId,
                WhipResult.Failure("This import preview changed before it could start. Review it and try again."),
            )
            return true
        }
        _csvImportState.value = current.copy(commitAttempted = true)
        viewModelScope.launch {
            val result = try {
                val receipt = checkNotNull(app.withUserDataAccess {
                    repository.importEntries(preparation.request, drafts)
                }) { "Whip data is unavailable while recovery is in progress" }
                WhipResult.Success(receipt)
            } catch (cancelled: CancellationException) {
                if (currentCoroutineContext().isActive) {
                    WhipResult.Failure(
                        "The import was interrupted. Whip will verify whether it finished before any retry.",
                        cancelled,
                    )
                } else {
                    if ((_csvImportRequestState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                        _csvImportRequestState.value = PersistenceRequestState.Idle
                    }
                    throw cancelled
                }
            } catch (error: Exception) {
                if (error is TrackCsvImportConflictException) {
                    val latest = _csvImportState.value
                    if (
                        latest.batchUuid == expected.batchUuid &&
                        latest.previewRevision == expected.previewRevision &&
                        latest.dataGeneration == expected.dataGeneration
                    ) {
                        _csvImportState.value = latest.copy(
                            phase = TrackCsvImportPhase.Error,
                            requiresNewFile = true,
                            errorMessage =
                                (error.message ?: "This import no longer matches the current Track.") + when (error.kind) {
                                    TrackCsvImportConflictKind.TargetMissing,
                                    TrackCsvImportConflictKind.IdentityChanged,
                                    -> " Cancel this import and choose an available Track."
                                    else -> " Choose Replace File to build a new preview."
                                },
                        )
                    }
                }
                WhipResult.Failure(error.message ?: "The Entries could not be imported.", error)
            }
            if ((_csvImportRequestState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                _csvImportRequestState.value = PersistenceRequestState.Finished(requestId, result)
            }
        }
        return true
    }

    internal fun completeCsvImport(
        expected: TrackCsvImportCommitIdentity,
        receipt: TrackCsvImportReceipt,
    ): Boolean {
        val current = _csvImportState.value
        if (current.commitIdentityOrNull() != expected || !receipt.matches(expected)) return false
        _csvImportState.value = current.copy(
            phase = TrackCsvImportPhase.Complete,
            completionReceipt = receipt,
            recoveryNotice = null,
            errorMessage = null,
        )
        return true
    }

    fun deleteEntry(expectedBoundary: TrackEntryBoundary, requestId: String): Boolean {
        if (_entryUndoState.value.status !is OperationStatus.Idle) return false
        if (!_entryMutationState.tryStartPersistenceRequest(requestId)) return false
        supersededEntryUndoState(_lastDeletedEntry.value)?.let { superseded ->
            _lastDeletedEntry.value = null
            _entryUndoState.value = superseded
        }
        _entryConflictState.value = null
        viewModelScope.launch {
            val result = try {
                val receipt = checkNotNull(app.withUserDataAccess {
                    operationMutex.withLock {
                        repository.deleteEntry(expectedBoundary).also(::recordEntryDeletionRecovery)
                    }
                }) { "Whip data is unavailable while recovery is in progress" }
                WhipResult.Success(receipt)
            } catch (cancelled: CancellationException) {
                if (currentCoroutineContext().isActive) {
                    WhipResult.Failure(
                        "The Entry deletion was interrupted. The Entry may still exist; verify history before retrying.",
                        cancelled,
                    )
                } else {
                    if ((_entryMutationState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                        _entryMutationState.value = PersistenceRequestState.Idle
                    }
                    throw cancelled
                }
            } catch (error: Exception) {
                (error as? TrackEntryConflictException)?.let { conflict ->
                    _entryConflictState.value = TrackEntryConflictUiState(
                        requestId = requestId,
                        kind = conflict.kind,
                        message = conflict.message ?: "The Entry changed before deletion could finish.",
                    )
                }
                WhipResult.Failure(error.message ?: "The Entry could not be deleted.", error)
            }
            if ((_entryMutationState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                _entryMutationState.value = PersistenceRequestState.Finished(requestId, result)
            }
        }
        return true
    }

    fun undoEntryDeletion(expectedToken: Long) {
        val currentUndo = _entryUndoState.value
        val deletedSnapshot = when {
            currentUndo.token == expectedToken && currentUndo.status is OperationStatus.Failed ->
                currentUndo.deletedEntry
            currentUndo.status is OperationStatus.Idle ->
                _lastDeletedEntry.value?.takeIf { it.token == expectedToken }?.deletedEntry
            else -> null
        } ?: return
        // Admission is synchronous on the main thread. Keep the exact Undo
        // snapshot and surface Retry instead of silently dropping an Undo tap
        // that races an already-admitted Entry mutation.
        if (_entryMutationState.value is PersistenceRequestState.Running) {
            _entryUndoState.value = TrackEntryUndoUiState(
                token = expectedToken,
                deletedEntry = deletedSnapshot,
                status = OperationStatus.Failed(
                    "Another Entry change is finishing. Retry Undo.",
                ),
            )
            return
        }
        _entryUndoState.value = TrackEntryUndoUiState(
            token = expectedToken,
            deletedEntry = deletedSnapshot,
            status = OperationStatus.Running("Restoring Entry…"),
        )
        viewModelScope.launch {
            try {
                checkNotNull(app.withUserDataAccess {
                    operationMutex.withLock { repository.restoreEntry(deletedSnapshot) }
                }) { "Whip data is unavailable while recovery is in progress" }
                if (_lastDeletedEntry.value?.token == expectedToken) _lastDeletedEntry.value = null
                if (_entryUndoState.value.token == expectedToken) {
                    _entryUndoState.value = TrackEntryUndoUiState(
                        token = expectedToken,
                        deletedEntry = deletedSnapshot,
                        status = OperationStatus.Succeeded(
                            "Entry restored",
                            OperationFeedbackPresentation.Snackbar,
                            expectedToken,
                        ),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (_entryUndoState.value.token == expectedToken) {
                    _entryUndoState.value = TrackEntryUndoUiState(
                        token = expectedToken,
                        deletedEntry = deletedSnapshot,
                        status = OperationStatus.Failed(
                            error.message ?: "The Entry could not be restored.",
                            error,
                        ),
                    )
                }
            }
        }
    }

    fun clearEntryUndo(expectedToken: Long) {
        if (_lastDeletedEntry.value?.token == expectedToken) _lastDeletedEntry.value = null
    }

    fun consumeEntryUndoStatus(expectedToken: Long) {
        if (_entryUndoState.value.token == expectedToken) {
            _entryUndoState.value = TrackEntryUndoUiState()
        }
    }

    fun entryDeletionUndoToken(receipt: TrackEntryMutationReceipt): Long? = _lastDeletedEntry.value
        ?.takeIf { pending ->
            receipt.kind == TrackEntryMutationKind.Delete &&
                pending.deletedEntry.entry.uuid == receipt.entryUuid
        }
        ?.token

    fun prepareCsvImport(trackId: Long, uri: Uri, today: LocalDate): Boolean {
        if (_csvImportRequestState.value is PersistenceRequestState.Running) return false
        val projection = uiState.value.track(trackId) ?: return false
        csvImportRestoreJob?.cancel()
        csvImportRestoreJob = null
        runCatching {
            app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val session = csvImportSessionStore.begin(
            trackId = trackId,
            trackName = projection.track.name,
            trackUuid = projection.track.uuid,
            trackCreatedAtMillis = projection.track.createdAtMillis,
            batchUuid = UUID.randomUUID().toString(),
            uri = uri.toString(),
            fileLabel = resolveCsvFileLabel(uri),
            today = today,
        )
        _csvImportRequestState.value = PersistenceRequestState.Idle
        importUri = uri
        importToday = today
        startCsvImport(session)
        return true
    }

    fun retryCsvImport() {
        if (_csvImportRequestState.value is PersistenceRequestState.Running) return
        val session = csvImportSessionStore.descriptor ?: return
        val current = _csvImportState.value
        val frozenForm = current.openingForm ?: current.preparation?.form
        _csvImportState.value = session.uiState(
            phase = TrackCsvImportPhase.Reading,
            openingForm = frozenForm,
        )
        val lookupGeneration = requestTrackReload()
        restoreCsvImportSession(
            descriptor = session,
            minimumLookupGeneration = lookupGeneration,
            frozenForm = frozenForm,
        )
    }

    fun updateCsvImportMapping(mapping: TrackCsvMapping) {
        val current = _csvImportState.value
        val trackId = current.trackId ?: return
        val text = importCsvText ?: return
        val openingForm = importCsvForm?.takeIf { it.track.id == trackId } ?: return
        if (current.commitAttempted || _csvImportRequestState.value !is PersistenceRequestState.Idle) return
        val session = csvImportSessionStore.updateMapping(mapping) ?: return
        csvImportJob?.cancel()
        _csvImportState.value = session.uiState(
            phase = TrackCsvImportPhase.Previewing,
            openingForm = openingForm,
        )
        csvImportJob = viewModelScope.launch {
            try {
                val preview = buildCsvPreview(openingForm, text, mapping, requireNotNull(importToday))
                if (!csvImportSessionStore.descriptor.matchesSession(session)) return@launch
                val preparation = prepareCsvRequest(session, openingForm, preview, mapping)
                val finalSession = if (preparation == null) session else {
                    csvImportSessionStore.recordPreparation(session, preparation) ?: return@launch
                }
                _csvImportState.value = finalSession.uiState(
                    phase = TrackCsvImportPhase.Ready,
                    headers = preview.headers,
                    openingForm = openingForm,
                    preview = preview,
                    preparation = preparation,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (!csvImportSessionStore.descriptor.matchesSession(session)) return@launch
                _csvImportState.value = session.uiState(
                    phase = TrackCsvImportPhase.Error,
                    openingForm = openingForm,
                    errorMessage = error.message
                        ?: "Could not build the CSV preview. Check the column mapping or choose another file.",
                )
            }
        }
    }

    fun cancelCsvImport(): Boolean {
        if (_csvImportRequestState.value is PersistenceRequestState.Running) return false
        clearCsvImportSession()
        return true
    }

    private fun clearCsvImportSession() {
        csvImportRestoreJob?.cancel()
        csvImportRestoreJob = null
        csvImportJob?.cancel()
        csvImportJob = null
        importCsvText = null
        importCsvForm = null
        importUri = null
        importToday = null
        csvImportSessionStore.clear()
        _csvImportRequestState.value = PersistenceRequestState.Idle
        _csvImportState.value = TrackCsvImportUiState()
    }

    fun exportCsvToUri(trackId: Long, uri: Uri) {
        exportUri = uri
        startCsvExport(trackId)
    }

    fun retryCsvExport() {
        _csvExportState.value.trackId?.let(::startCsvExport)
    }

    fun cancelCsvExport() {
        csvExportJob?.cancel()
        csvExportJob = null
        exportUri = null
        _csvExportState.value = TrackCsvExportUiState()
    }

    fun clearCompletedCsvExport() {
        if (_csvExportState.value.phase == TrackCsvExportPhase.Complete) cancelCsvExport()
    }

    suspend fun searchEntryIds(trackId: Long, query: String): Set<Long> =
        checkNotNull(app.withUserDataAccess { repository.searchEntryIds(trackId, query) }) {
            "Whip data is unavailable while recovery is in progress"
        }
    suspend fun entryPage(trackId: Long, offset: Int, limit: Int = 100): TrackEntryPage =
        checkNotNull(app.withUserDataAccess { repository.entryPage(trackId, offset, limit) }) {
            "Whip data is unavailable while recovery is in progress"
        }

    private suspend fun buildCsvPreview(
        openingForm: TrackEntryFormSnapshot,
        text: String,
        mapping: TrackCsvMapping,
        today: LocalDate,
    ): TrackCsvImportPreview = withContext(Dispatchers.Default) {
        previewTrackCsvImport(openingForm, text, mapping, today)
    }

    private suspend fun prepareCsvRequest(
        session: TrackCsvImportSessionDescriptor,
        openingForm: TrackEntryFormSnapshot,
        preview: TrackCsvImportPreview,
        mapping: TrackCsvMapping,
    ): TrackCsvImportPreparation? {
        if (preview.validRows <= 0 || preview.invalidRows != 0) return null
        require(preview.validRows <= TRACK_CSV_MAX_DATA_ROWS) {
            "Import at most 5,000 Entries at a time. Split this CSV into smaller files."
        }
        return checkNotNull(app.withUserDataAccess {
            repository.prepareCsvImport(
                openingForm = openingForm,
                batchUuid = session.batchUuid,
                payloadFingerprint = requireNotNull(session.rawPayloadFingerprint),
                mapping = mapping,
                defaultEntryDate = LocalDate.ofEpochDay(session.todayEpochDay),
                drafts = preview.validDrafts,
            )
        }) { "Whip data is temporarily unavailable." }
    }

    private fun resolveCsvFileLabel(uri: Uri): String = runCatching {
        app.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index)?.trim() else null
        }
    }.getOrNull()?.takeIf(String::isNotBlank) ?: uri.toString()

    private fun restoreCsvImportSession(
        descriptor: TrackCsvImportSessionDescriptor,
        minimumLookupGeneration: Int = reloadKey.value,
        frozenForm: TrackEntryFormSnapshot? = _csvImportState.value.openingForm,
    ) {
        csvImportRestoreJob?.cancel()
        csvImportRestoreJob = viewModelScope.launch {
            if (!csvImportSessionStore.descriptor.matchesSession(descriptor)) return@launch
            descriptor.preparedReceiptEnvelope?.let { expectedReceipt ->
                val verificationResult = runCatching {
                    checkNotNull(app.withUserDataAccess {
                        repository.verifyCsvImportReceipt(expectedReceipt)
                    }) { "Whip data is unavailable while recovery is in progress" }
                }
                if (!csvImportSessionStore.descriptor.matchesSession(descriptor)) return@launch
                verificationResult.exceptionOrNull()?.let { error ->
                    _csvImportState.value = descriptor.uiState(
                        phase = TrackCsvImportPhase.Error,
                        errorMessage = error.message
                            ?: "Whip could not verify the previous import. Try again before choosing a different file.",
                    )
                    return@launch
                }
                when (val verification = verificationResult.getOrThrow()) {
                    is TrackCsvImportReceiptVerification.Exact -> {
                        _csvImportState.value = descriptor.uiState(
                            phase = TrackCsvImportPhase.Complete,
                            completionReceipt = verification.receipt,
                        )
                        return@launch
                    }
                    is TrackCsvImportReceiptVerification.Collision -> {
                        _csvImportState.value = descriptor.uiState(
                            phase = TrackCsvImportPhase.Error,
                            requiresNewFile = true,
                            errorMessage =
                                "Whip found a different completed import for this saved session. " +
                                    "Cancel and select the file again to start a new review.",
                        )
                        return@launch
                    }
                    is TrackCsvImportReceiptVerification.Missing -> Unit
                }
            }
            val loadedState = uiState.first {
                !it.loading && it.lookupGeneration >= minimumLookupGeneration
            }
            if (!csvImportSessionStore.descriptor.matchesSession(descriptor)) return@launch
            // A projection load failure is not evidence that the saved target
            // was deleted. Keep the durable session and let the dialog offer a
            // retry that owns a newer lookup generation.
            if (loadedState.errorMessage != null) return@launch
            if (loadedState.track(descriptor.trackId) == null) {
                _csvImportState.value = descriptor.uiState(
                    phase = TrackCsvImportPhase.Error,
                    requiresNewFile = true,
                    errorMessage =
                        "${descriptor.trackName} is no longer available. No Entries were imported. " +
                            "Cancel this import or choose another file after selecting an available Track.",
                )
                return@launch
            }
            importUri = Uri.parse(descriptor.uri)
            importToday = LocalDate.ofEpochDay(descriptor.todayEpochDay)
            startCsvImport(descriptor, recovering = true, frozenForm = frozenForm)
        }
    }

    private fun startCsvImport(
        openingSession: TrackCsvImportSessionDescriptor,
        recovering: Boolean = false,
        frozenForm: TrackEntryFormSnapshot? = null,
    ) {
        val uri = importUri ?: return
        val today = importToday ?: return
        if (!csvImportSessionStore.descriptor.matchesSession(openingSession)) return
        csvImportJob?.cancel()
        _csvImportState.value = openingSession.uiState(
            phase = TrackCsvImportPhase.Reading,
            openingForm = frozenForm,
        )
        csvImportJob = viewModelScope.launch {
            var ownedSession = openingSession
            var ownedForm: TrackEntryFormSnapshot? = frozenForm
            try {
                val openingForm = frozenForm ?: checkNotNull(app.withUserDataAccess {
                    TrackCsvImportFormLookup(repository.csvImportForm(openingSession.trackId))
                }) { "Whip data is unavailable while recovery is in progress" }.form
                    ?: error("${openingSession.trackName} is no longer available. No Entries were imported.")
                if (!csvImportSessionStore.descriptor.matchesSession(openingSession)) return@launch
                if (!openingSession.ownsTrackIdentity(openingForm)) {
                    _csvImportState.value = openingSession.uiState(
                        phase = TrackCsvImportPhase.Error,
                        requiresNewFile = true,
                        errorMessage =
                            "The target Track changed after this file was selected. No Entries were imported. " +
                                "Cancel this import or choose Replace File to begin a new review for the current Track.",
                    )
                    return@launch
                }
                importCsvForm = openingForm
                ownedForm = openingForm
                _csvImportState.value = openingSession.uiState(
                    phase = TrackCsvImportPhase.Reading,
                    openingForm = openingForm,
                )
                val text = withContext(Dispatchers.IO) {
                    reloadTrackCsvText(openingSession) { savedUri ->
                        readCsvText(Uri.parse(savedUri))
                    }
                }
                val payloadFingerprint = withContext(Dispatchers.Default) { trackCsvPayloadFingerprint(text) }
                if (
                    openingSession.rawPayloadFingerprint != null &&
                    openingSession.rawPayloadFingerprint != payloadFingerprint
                ) {
                    _csvImportState.value = openingSession.uiState(
                        phase = TrackCsvImportPhase.Error,
                        openingForm = openingForm,
                        requiresNewFile = true,
                        errorMessage =
                            "The selected file changed after this preview was saved. " +
                                "Choose Replace File to review the current contents as a new import.",
                    )
                    return@launch
                }
                val headers = withContext(Dispatchers.Default) {
                    validateTrackCsvEnvelope(text)
                    trackCsvHeaders(text).map(String::trim).also { headers ->
                    require(headers.isNotEmpty()) { "The CSV file has no header row. Choose a file whose first row contains column names." }
                    require(headers.size <= TRACK_CSV_MAX_COLUMNS) { "This CSV has more than 100 columns. Remove unused columns and try again." }
                    }
                }
                val mapping = openingSession.takeIf { it.mappingInitialized }?.mapping
                    ?: defaultTrackCsvMapping(openingForm.fields, headers)
                val session = if (openingSession.rawPayloadFingerprint == null) {
                    csvImportSessionStore.recordLoadedPayload(openingSession, payloadFingerprint, mapping)
                        ?: return@launch
                } else {
                    openingSession
                }
                ownedSession = session
                importCsvText = text
                _csvImportState.value = session.uiState(
                    phase = TrackCsvImportPhase.Previewing,
                    headers = headers,
                    openingForm = openingForm,
                )
                val preview = buildCsvPreview(openingForm, text, mapping, today)
                val preparation = prepareCsvRequest(session, openingForm, preview, mapping)
                if (openingSession.preparedReceiptEnvelope != null && (
                        preparation == null ||
                            preparation.request.receiptEnvelope() != openingSession.preparedReceiptEnvelope
                    )
                ) {
                    _csvImportState.value = openingSession.uiState(
                        phase = TrackCsvImportPhase.Error,
                        openingForm = openingForm,
                        requiresNewFile = true,
                        errorMessage =
                            "This Track or import setup changed after the preview was saved. " +
                                "Choose Replace File to review it as a new import.",
                    )
                    return@launch
                }
                val finalSession = if (preparation == null || session.preparedReceiptEnvelope != null) {
                    session
                } else {
                    csvImportSessionStore.recordPreparation(session, preparation) ?: return@launch
                }
                ownedSession = finalSession
                if (!csvImportSessionStore.descriptor.matchesSession(finalSession)) return@launch
                _csvImportState.value = finalSession.uiState(
                    phase = TrackCsvImportPhase.Ready,
                    headers = headers,
                    openingForm = openingForm,
                    preview = preview,
                    preparation = preparation,
                    recoveryNotice = if (recovering && openingSession.commitAttempted) {
                        "No completed import was found. Review this preview, then import again when you are ready."
                    } else null,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (!csvImportSessionStore.descriptor.matchesSession(ownedSession)) return@launch
                _csvImportState.value = ownedSession.uiState(
                    phase = TrackCsvImportPhase.Error,
                    openingForm = ownedForm,
                    errorMessage = error.message ?: "Could not read this CSV. Choose another file and try again.",
                )
            }
        }
    }

    private fun startCsvExport(trackId: Long) {
        val uri = exportUri ?: return
        csvExportJob?.cancel()
        _csvExportState.value = TrackCsvExportUiState(trackId, TrackCsvExportPhase.Writing)
        csvExportJob = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val csv = checkNotNull(app.withUserDataAccess { repository.exportCsv(trackId) }) {
                        "Whip data is unavailable while recovery is in progress"
                    }
                    writeCsvText(uri, csv)
                }
                _csvExportState.value = TrackCsvExportUiState(trackId, TrackCsvExportPhase.Complete)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _csvExportState.value = TrackCsvExportUiState(
                    trackId,
                    TrackCsvExportPhase.Error,
                    error.message ?: "Could not export this Track. Try again or choose another destination.",
                )
            }
        }
    }

    private suspend fun readCsvText(uri: Uri): String {
        val stream = app.contentResolver.openInputStream(uri)
            ?: error("Could not open the selected CSV. Choose another file and try again.")
        return stream.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            var total = 0
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                require(total <= TRACK_CSV_MAX_FILE_BYTES) { "Choose a CSV smaller than 5 MB." }
                output.write(buffer, 0, count)
            }
            decodeTrackCsvUtf8(output.toByteArray())
        }
    }

    private suspend fun writeCsvText(uri: Uri, csv: String) {
        requireTrackCsvExportWithinLimit(csv)
        val output = app.contentResolver.openOutputStream(uri, "w")
            ?: error("Could not open the selected file. Choose another destination and try again.")
        output.bufferedWriter(Charsets.UTF_8).use { writer ->
            writeTrackCsvChunks(csv, writer)
        }
    }

    private fun runSilentReorder(block: suspend () -> Unit) {
        viewModelScope.launch {
            operationMutex.withLock {
                try {
                    checkNotNull(app.withUserDataAccess {
                        block()
                        Unit
                    }) { "Whip data is unavailable while recovery is in progress" }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    _operationStatus.value = OperationStatus.Failed(error.message ?: "Could not save the new order", error)
                }
            }
        }
    }

    private fun recordEntryDeletionRecovery(receipt: TrackEntryMutationReceipt) {
        if (receipt.kind != TrackEntryMutationKind.Delete || !receipt.changed) return
        val deleted = requireNotNull(receipt.deletedEntry) {
            "A deleted Entry receipt must include an exact recovery snapshot."
        }
        val token = ++nextEntryUndoToken
        _entryUndoState.value = entryUndoStateAfterNewDeletionRecorded(_entryUndoState.value)
        _lastDeletedEntry.value = PendingTrackEntryUndo(token, deleted)
    }

    private fun runDefinitionSaveOperation(
        runningMessage: String,
        successMessage: String,
        requestId: String,
        block: suspend () -> EntitySaveReceipt,
    ) {
        _operationStatus.value = OperationStatus.Running(runningMessage)
        viewModelScope.launch {
            fun successResult(receipt: EntitySaveReceipt): WhipResult.Success<EntitySaveReceipt> {
                val message = if (receipt.warnings.isEmpty()) successMessage else {
                    "$successMessage · ${receipt.warnings.joinToString(" ")}"
                }
                _operationStatus.value = OperationStatus.Succeeded(
                    message,
                    OperationFeedbackPresentation.Snackbar,
                )
                return WhipResult.Success(receipt)
            }
            val result = try {
                val receipt = checkNotNull(app.withUserDataAccess {
                    operationMutex.withLock { block() }
                }) { "Whip data is unavailable while recovery is in progress" }
                successResult(receipt)
            } catch (cancelled: CommittedEntitySaveCancellation) {
                if (currentCoroutineContext().isActive) {
                    successResult(
                        cancelled.receipt.copy(
                            warnings = cancelled.receipt.warnings +
                                "Some post-save updates were interrupted; the Track itself was saved.",
                        ),
                    )
                } else {
                    if ((_definitionSaveState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                        _definitionSaveState.value = PersistenceRequestState.Idle
                    }
                    _operationStatus.value = OperationStatus.Idle
                    throw cancelled
                }
            } catch (cancelled: CancellationException) {
                if (currentCoroutineContext().isActive) {
                    _operationStatus.value = OperationStatus.Idle
                    WhipResult.Failure(
                        "The Track save was interrupted. Your changes are still here.",
                        cancelled,
                    )
                } else {
                    if ((_definitionSaveState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                        _definitionSaveState.value = PersistenceRequestState.Idle
                    }
                    _operationStatus.value = OperationStatus.Idle
                    throw cancelled
                }
            } catch (error: Exception) {
                _operationStatus.value = OperationStatus.Idle
                WhipResult.Failure(error.message ?: "The Track could not be saved.", error)
            }
            if ((_definitionSaveState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                _definitionSaveState.value = PersistenceRequestState.Finished(requestId, result)
            }
        }
    }

    private fun runOperation(
        running: String,
        success: String,
        successFeedbackPresentation: OperationFeedbackPresentation = OperationFeedbackPresentation.Inline,
        recoveryToken: Long? = null,
        block: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            operationMutex.withLock {
                _operationStatus.value = OperationStatus.Running(running)
                try {
                    checkNotNull(app.withUserDataAccess {
                        block()
                        Unit
                    }) { "Whip data is unavailable while recovery is in progress" }
                    val acknowledgement = recoveryToken?.let { CompletableDeferred<Unit>() }
                    recoveryAcknowledgement = acknowledgement
                    _operationStatus.value = OperationStatus.Succeeded(
                        success,
                        successFeedbackPresentation,
                        recoveryToken,
                    )
                    acknowledgement?.await()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    _operationStatus.value = OperationStatus.Failed(error.message ?: "Something went wrong", error)
                }
            }
        }
    }
}

internal fun Track.toDraft(fields: List<TrackField>, options: List<TrackChoiceOption>) = TrackDraft(
    name = name,
    description = description,
    icon = icon,
    areaId = areaId,
    area = area,
    tags = tags,
    fields = fields.sortedBy(TrackField::position).map { field ->
        com.whip.app.domain.TrackFieldDraft(
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
            options = options.filter { it.fieldId == field.id }.sortedBy(TrackChoiceOption::position).map {
                com.whip.app.domain.TrackChoiceOptionDraft(it.label, it.uuid, it.id)
            },
            uuid = field.uuid,
            id = field.id,
        )
    },
)
