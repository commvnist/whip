package com.whip.app.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
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
import com.whip.app.domain.TrackCsvMapping
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackDefinitionBoundary
import com.whip.app.domain.TrackDefinitionConflictException
import com.whip.app.domain.TrackDefinitionConflictKind
import com.whip.app.domain.TrackDefinitionRemovalReview
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackEntryPage
import com.whip.app.domain.TrackField
import com.whip.app.domain.TrackProjection
import com.whip.app.domain.TrackCondition
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.previewTrackCsvImport
import com.whip.app.domain.trackCsvHeaders
import java.io.ByteArrayOutputStream
import java.io.Serializable
import java.io.Writer
import java.time.LocalDate
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

internal data class TrackCsvEnvelope(val dataRows: Int, val maximumColumns: Int)

/** Compact process-restorable import state. The CSV payload always remains behind [uri]. */
internal data class TrackCsvImportSessionDescriptor(
    val trackId: Long,
    val uri: String,
    val todayEpochDay: Long,
    val dataGeneration: Long = 0L,
    val entryDateColumn: String? = null,
    val fieldColumns: Map<String, String> = emptyMap(),
    val numberUnitColumns: Map<String, String> = emptyMap(),
    val mappingInitialized: Boolean = false,
) : Serializable {
    val mapping: TrackCsvMapping
        get() = TrackCsvMapping(entryDateColumn, fieldColumns, numberUnitColumns)

    fun withMapping(value: TrackCsvMapping) = copy(
        entryDateColumn = value.entryDateColumn,
        fieldColumns = LinkedHashMap(value.fieldColumns),
        numberUnitColumns = LinkedHashMap(value.numberUnitColumns),
        mappingInitialized = true,
    )
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

    fun begin(trackId: Long, uri: String, today: LocalDate): TrackCsvImportSessionDescriptor {
        val value = TrackCsvImportSessionDescriptor(
            trackId = trackId,
            uri = uri,
            todayEpochDay = today.toEpochDay(),
            dataGeneration = currentDataGeneration(),
        )
        savedStateHandle[STATE_KEY] = value
        return value
    }

    fun updateMapping(mapping: TrackCsvMapping): TrackCsvImportSessionDescriptor? = descriptor?.withMapping(mapping)?.also {
        savedStateHandle[STATE_KEY] = it
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
    phase = TrackCsvImportPhase.Reading,
    mapping = mapping,
)

internal suspend fun reloadTrackCsvText(
    descriptor: TrackCsvImportSessionDescriptor,
    readUri: suspend (String) -> String,
): String = readUri(descriptor.uri)

/** Fast allocation-free guard run before the full RFC-4180 parser. */
internal fun validateTrackCsvEnvelope(text: String): TrackCsvEnvelope {
    require(text.toByteArray(Charsets.UTF_8).size <= TRACK_CSV_MAX_FILE_BYTES) {
        "Choose a CSV smaller than 5 MB."
    }
    var quoted = false
    var columns = 1
    var maximumColumns = 0
    var records = 0
    var recordStarted = false
    var index = 0
    fun finishRecord() {
        records++
        maximumColumns = maxOf(maximumColumns, columns)
        require(records <= TRACK_CSV_MAX_DATA_ROWS + 1) {
            "This CSV has more than 5,000 data rows. Split it into smaller files and import them separately."
        }
        columns = 1
        recordStarted = false
    }
    while (index < text.length) {
        val char = text[index]
        when {
            quoted && char == '"' && index + 1 < text.length && text[index + 1] == '"' -> {
                recordStarted = true
                index++
            }
            char == '"' -> {
                recordStarted = true
                quoted = !quoted
            }
            !quoted && char == ',' -> {
                recordStarted = true
                columns++
                require(columns <= TRACK_CSV_MAX_COLUMNS) {
                    "This CSV has more than 100 columns. Remove unused columns and try again."
                }
            }
            !quoted && (char == '\n' || char == '\r') -> {
                if (char == '\r' && index + 1 < text.length && text[index + 1] == '\n') index++
                finishRecord()
            }
            else -> recordStarted = true
        }
        index++
    }
    require(!quoted) { "The CSV contains an unclosed quoted value. Fix the quoted cell and try again." }
    if (recordStarted || columns > 1) finishRecord()
    val envelope = TrackCsvEnvelope(dataRows = (records - 1).coerceAtLeast(0), maximumColumns = maximumColumns)
    require(envelope.dataRows.toLong() * envelope.maximumColumns <= TRACK_CSV_MAX_PREVIEW_CELLS) {
        "This CSV preview would inspect more than 100,000 cells. Remove unused columns or split the file into smaller imports."
    }
    return envelope
}

internal fun defaultTrackCsvMapping(projection: TrackProjection, headers: List<String>) = TrackCsvMapping(
    entryDateColumn = headers.firstOrNull { it.equals("Entry Date", true) },
    fieldColumns = projection.fields.mapNotNull { field ->
        headers.firstOrNull {
            it.equals(field.name, true) ||
                field.type == TrackFieldType.Number && it.equals("${field.name} (Entered)", true)
        }?.let { field.uuid to it }
    }.toMap(),
    numberUnitColumns = projection.fields.filter { it.type == TrackFieldType.Number }.mapNotNull { field ->
        headers.firstOrNull { it.equals("${field.name} (Unit)", true) }?.let { field.uuid to it }
    }.toMap(),
)

internal enum class TrackCsvImportPhase { Idle, Reading, Previewing, Ready, Error }

internal data class TrackCsvImportUiState(
    val trackId: Long? = null,
    val phase: TrackCsvImportPhase = TrackCsvImportPhase.Idle,
    val headers: List<String> = emptyList(),
    val mapping: TrackCsvMapping = TrackCsvMapping(),
    val preview: TrackCsvImportPreview? = null,
    val errorMessage: String? = null,
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

class TrackViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val app = application as WhipApplication
    private val repository: TrackRepository = app.trackRepository
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
    private val _definitionReviewState = MutableStateFlow(TrackDefinitionReviewUiState())
    internal val definitionReviewState: StateFlow<TrackDefinitionReviewUiState> =
        _definitionReviewState.asStateFlow()
    private var definitionReviewGeneration = 0L
    private val operationMutex = Mutex()
    private var recoveryAcknowledgement: CompletableDeferred<Unit>? = null
    private val _lastDeletedEntry = MutableStateFlow<PendingTrackEntryUndo?>(null)
    val lastDeletedEntry: StateFlow<PendingTrackEntryUndo?> = _lastDeletedEntry.asStateFlow()
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
    private var importUri: Uri? = restoredCsvImportSession?.uri?.let(Uri::parse)
    private var importToday: LocalDate? = restoredCsvImportSession?.todayEpochDay?.let(LocalDate::ofEpochDay)
    private var importUnits: List<UnitDefinition> = emptyList()
    private var exportUri: Uri? = null

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TrackUiState> = reloadKey.flatMapLatest {
        combine(repository.projections, app.calendarContext) { projections, calendar ->
            TrackUiState(
                projections = projections,
                currentDate = calendar.logicalDate,
                loading = false,
            )
        }.catch { error ->
            emit(
                TrackUiState(
                    currentDate = app.calendarContext.value.logicalDate,
                    loading = false,
                    errorMessage = error.message ?: "Could not load Tracks",
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
                recoveryAcknowledgement?.complete(Unit)
                recoveryAcknowledgement = null
                _operationStatus.value = OperationStatus.Idle
                _definitionSaveState.value = PersistenceRequestState.Idle
                definitionReviewGeneration++
                _definitionReviewState.value = TrackDefinitionReviewUiState()
                cancelCsvImport()
                cancelCsvExport()
            }
        }
    }

    fun consumeOperationStatus() {
        _operationStatus.value = OperationStatus.Idle
        recoveryAcknowledgement?.complete(Unit)
        recoveryAcknowledgement = null
    }

    fun retryLoading() { reloadKey.value++ }

    fun consumeDefinitionSaveResult(requestId: String) {
        if ((_definitionSaveState.value as? PersistenceRequestState.Finished)?.requestId == requestId) {
            _definitionSaveState.value = PersistenceRequestState.Idle
        }
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

    fun saveEntry(trackId: Long, entryId: Long?, draft: TrackEntryDraft, onSaved: (Long) -> Unit = {}) = runOperation(
        if (entryId == null) "Adding Entry…" else "Saving Entry…",
        if (entryId == null) "Entry added" else "Entry saved",
    ) {
        val savedId = if (entryId == null) repository.addEntry(trackId, draft) else {
            repository.updateEntry(entryId, draft)
            entryId
        }
        onSaved(savedId)
    }

    fun importEntries(trackId: Long, drafts: List<TrackEntryDraft>, onSaved: (Int) -> Unit = {}) = runOperation(
        "Importing Entries…",
        "${drafts.size} Entries imported",
        successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
    ) {
        require(drafts.size <= TRACK_CSV_MAX_DATA_ROWS) {
            "Import at most 5,000 Entries at a time. Split this CSV into smaller files."
        }
        val importedCount = repository.importEntries(trackId, drafts).size
        onSaved(importedCount)
    }

    fun deleteEntry(entryId: Long): Unit {
        val undoToken = ++nextEntryUndoToken
        runOperation(
            "Deleting Entry…",
            "Entry deleted",
            successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
        recoveryToken = undoToken,
        ) {
            val deletedEntry = repository.deleteEntry(entryId) ?: error("Entry no longer exists")
            _lastDeletedEntry.value = PendingTrackEntryUndo(undoToken, deletedEntry)
        }
    }

    fun undoEntryDeletion(expectedToken: Long) {
        if (_lastDeletedEntry.value?.token != expectedToken) return
        runOperation("Restoring Entry…", "Entry restored") {
            _lastDeletedEntry.value
                ?.takeIf { it.token == expectedToken }
                ?.let { repository.restoreEntry(it.deletedEntry) }
            if (_lastDeletedEntry.value?.token == expectedToken) _lastDeletedEntry.value = null
        }
    }

    fun clearEntryUndo(expectedToken: Long) {
        if (_lastDeletedEntry.value?.token == expectedToken) _lastDeletedEntry.value = null
    }

    fun prepareCsvImport(trackId: Long, uri: Uri, today: LocalDate, customUnits: List<UnitDefinition>) {
        csvImportRestoreJob?.cancel()
        csvImportRestoreJob = null
        runCatching {
            app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        csvImportSessionStore.begin(trackId, uri.toString(), today)
        importUri = uri
        importToday = today
        importUnits = BuiltInUnits.all + customUnits
        startCsvImport(trackId)
    }

    fun retryCsvImport() {
        val trackId = _csvImportState.value.trackId ?: return
        if (importUri != null && importToday != null) startCsvImport(trackId)
    }

    fun updateCsvImportMapping(mapping: TrackCsvMapping) {
        val current = _csvImportState.value
        val trackId = current.trackId ?: return
        val text = importCsvText ?: return
        val projection = uiState.value.track(trackId) ?: return
        csvImportSessionStore.updateMapping(mapping)
        csvImportJob?.cancel()
        _csvImportState.value = current.copy(
            phase = TrackCsvImportPhase.Previewing,
            mapping = mapping,
            preview = null,
            errorMessage = null,
        )
        csvImportJob = viewModelScope.launch {
            val result = try {
                Result.success(withContext(Dispatchers.Default) {
                    previewTrackCsvImport(projection, text, mapping, requireNotNull(importToday), importUnits)
                })
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                Result.failure(error)
            }
            if (_csvImportState.value.trackId != trackId || _csvImportState.value.mapping != mapping) return@launch
            _csvImportState.value = trackCsvPreviewCompletion(_csvImportState.value, result)
        }
    }

    fun cancelCsvImport() {
        csvImportRestoreJob?.cancel()
        csvImportRestoreJob = null
        csvImportJob?.cancel()
        csvImportJob = null
        importCsvText = null
        importUri = null
        importToday = null
        importUnits = emptyList()
        csvImportSessionStore.clear()
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

    private fun restoreCsvImportSession(descriptor: TrackCsvImportSessionDescriptor) {
        csvImportRestoreJob?.cancel()
        csvImportRestoreJob = viewModelScope.launch {
            val loadedState = uiState.first { !it.loading }
            if (csvImportSessionStore.descriptor != descriptor) return@launch
            if (loadedState.track(descriptor.trackId) == null) {
                _csvImportState.value = TrackCsvImportUiState(
                    trackId = descriptor.trackId,
                    phase = TrackCsvImportPhase.Error,
                    mapping = descriptor.mapping,
                    errorMessage = "This Track is no longer available. Cancel this import and choose another Track.",
                )
                return@launch
            }
            importUnits = BuiltInUnits.all + withContext(Dispatchers.IO) {
                app.measurementRepository.customUnits.first()
            }
            if (csvImportSessionStore.descriptor != descriptor) return@launch
            startCsvImport(descriptor.trackId)
        }
    }

    private fun startCsvImport(trackId: Long) {
        val uri = importUri ?: return
        val today = importToday ?: return
        val session = csvImportSessionStore.descriptor?.takeIf { it.trackId == trackId }
        csvImportJob?.cancel()
        _csvImportState.value = TrackCsvImportUiState(
            trackId = trackId,
            phase = TrackCsvImportPhase.Reading,
            mapping = session?.mapping ?: TrackCsvMapping(),
        )
        csvImportJob = viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    if (session == null) readCsvText(uri) else reloadTrackCsvText(session) { savedUri ->
                        readCsvText(Uri.parse(savedUri))
                    }
                }
                val projection = uiState.value.track(trackId) ?: error("This Track is no longer available")
                val prepared = withContext(Dispatchers.Default) {
                    validateTrackCsvEnvelope(text)
                    val headers = trackCsvHeaders(text).map(String::trim)
                    require(headers.isNotEmpty()) { "The CSV file has no header row. Choose a file whose first row contains column names." }
                    require(headers.size <= TRACK_CSV_MAX_COLUMNS) { "This CSV has more than 100 columns. Remove unused columns and try again." }
                    val mapping = session?.takeIf { it.mappingInitialized }?.mapping
                        ?: defaultTrackCsvMapping(projection, headers)
                    val previewResult = try {
                        Result.success(previewTrackCsvImport(projection, text, mapping, today, importUnits))
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Throwable) {
                        Result.failure(error)
                    }
                    Triple(headers, mapping, previewResult)
                }
                importCsvText = text
                csvImportSessionStore.updateMapping(prepared.second)
                _csvImportState.value = trackCsvPreviewCompletion(TrackCsvImportUiState(
                    trackId = trackId,
                    phase = TrackCsvImportPhase.Previewing,
                    headers = prepared.first,
                    mapping = prepared.second,
                ), prepared.third)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _csvImportState.value = TrackCsvImportUiState(
                    trackId = trackId,
                    phase = TrackCsvImportPhase.Error,
                    mapping = session?.mapping ?: TrackCsvMapping(),
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
            output.toString(Charsets.UTF_8.name()).removePrefix("\uFEFF")
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
