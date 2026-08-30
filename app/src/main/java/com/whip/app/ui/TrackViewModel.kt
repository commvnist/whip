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
import com.whip.app.core.OperationFeedbackPresentation
import com.whip.app.core.OperationStatus
import com.whip.app.core.revealHomeSection
import com.whip.app.data.TrackRepository
import com.whip.app.data.requireTrackCsvExportWithinLimit
import com.whip.app.domain.DeletedTrackEntry
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalConsistencyPeriod
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalType
import com.whip.app.domain.Habit
import com.whip.app.domain.LinkRuleDraft
import com.whip.app.domain.LinkRule
import com.whip.app.domain.LinkSourceMetric
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.LinkValueMode
import com.whip.app.domain.Track
import com.whip.app.domain.TrackChoiceOption
import com.whip.app.domain.TrackCsvImportPreview
import com.whip.app.domain.TrackCsvMapping
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackEntryPage
import com.whip.app.domain.TrackField
import com.whip.app.domain.TrackProjection
import com.whip.app.domain.TrackAggregation
import com.whip.app.domain.TrackCondition
import com.whip.app.domain.TrackConditionMode
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.TaskStep
import com.whip.app.domain.TriggerOccurrence
import com.whip.app.domain.TriggerRule
import com.whip.app.domain.TriggerRuleDraft
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.WhipTask
import com.whip.app.domain.Goal
import com.whip.app.domain.compatibleAggregations
import com.whip.app.domain.Contribution
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class TrackUiState(
    val projections: List<TrackProjection> = emptyList(),
    val linkRules: List<LinkRule> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val triggerRules: List<TriggerRule> = emptyList(),
    val triggerOccurrences: List<TriggerOccurrence> = emptyList(),
    val contributions: List<Contribution> = emptyList(),
    val sourceTasks: List<WhipTask> = emptyList(),
    val sourceTaskSteps: List<TaskStep> = emptyList(),
    val sourceHabits: List<Habit> = emptyList(),
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

internal class TrackCsvImportSessionStore(private val savedStateHandle: SavedStateHandle) {
    val descriptor: TrackCsvImportSessionDescriptor?
        get() = savedStateHandle[STATE_KEY]

    fun begin(trackId: Long, uri: String, today: LocalDate): TrackCsvImportSessionDescriptor {
        val value = TrackCsvImportSessionDescriptor(trackId, uri, today.toEpochDay())
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

private data class TrackAutomationState(
    val linkRules: List<LinkRule>,
    val triggerRules: List<TriggerRule>,
    val occurrences: List<TriggerOccurrence>,
    val contributions: List<Contribution>,
)

private data class TrackSourceState(
    val tasks: List<WhipTask>,
    val taskSteps: List<TaskStep>,
    val habits: List<Habit>,
    val goals: List<Goal>,
)

data class TrackGoalAutomationDraft(
    val goalName: String,
    val goalType: GoalType,
    val aggregation: TrackAggregation,
    val sourceFieldId: Long? = null,
    val conditionMode: TrackConditionMode = TrackConditionMode.MatchAll,
    val conditions: List<TrackCondition> = emptyList(),
    val target: Double? = null,
    val targetMax: Double? = null,
    val fixedAmount: Double? = null,
    val fixedDimension: UnitDimension = UnitDimension.Count,
    val fixedUnitId: String = "count",
    val retroactiveFrom: LocalDate? = null,
    val deadline: LocalDate? = null,
    val consistencyPeriod: GoalConsistencyPeriod = GoalConsistencyPeriod.Week,
    val consistencyRequiredPeriods: Int? = null,
)

class TrackViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val app = application as WhipApplication
    private val repository: TrackRepository = app.trackRepository
    private val clock = app.clock
    private val csvImportSessionStore = TrackCsvImportSessionStore(savedStateHandle)
    private val restoredCsvImportSession = csvImportSessionStore.descriptor
    private val _operationStatus = MutableStateFlow<OperationStatus>(OperationStatus.Idle)
    val operationStatus: StateFlow<OperationStatus> = _operationStatus.asStateFlow()
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

    private val automationState = combine(
        app.linkRepository.rules,
        app.linkRepository.triggerRules,
        app.linkRepository.triggerOccurrences,
        app.linkRepository.contributions,
    ) { linkRules, triggerRules, occurrences, contributions ->
        TrackAutomationState(linkRules, triggerRules, occurrences, contributions)
    }
    private val sourceState = combine(
        app.taskRepository.tasks,
        app.taskRepository.steps,
        app.habitRepository.habits,
        app.goalRepository.goals,
    ) { tasks, taskSteps, habits, goals -> TrackSourceState(tasks, taskSteps, habits, goals) }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TrackUiState> = reloadKey.flatMapLatest {
        combine(
            repository.projections,
            automationState,
            sourceState,
        ) { projections, automations, sources ->
            val (linkRules, triggerRules, occurrences, contributions) = automations
            TrackUiState(
                projections = projections,
                linkRules = linkRules,
                goals = sources.goals,
                triggerRules = triggerRules,
                triggerOccurrences = occurrences,
                contributions = contributions,
                sourceTasks = sources.tasks.filterNot(WhipTask::archived),
                sourceTaskSteps = sources.taskSteps.filterNot(TaskStep::archived),
                sourceHabits = sources.habits.filterNot(Habit::archived),
                currentDate = clock.today(),
                loading = false,
            )
        }.catch { error ->
            emit(TrackUiState(currentDate = clock.today(), loading = false, errorMessage = error.message ?: "Could not load Tracks"))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackUiState())

    init {
        restoredCsvImportSession?.let(::restoreCsvImportSession)
    }

    fun consumeOperationStatus() {
        _operationStatus.value = OperationStatus.Idle
        recoveryAcknowledgement?.complete(Unit)
        recoveryAcknowledgement = null
    }

    fun retryLoading() { reloadKey.value++ }

    fun saveTrack(
        id: Long?,
        draft: TrackDraft,
        confirmedFieldValueDeletionIds: Set<Long> = emptySet(),
        confirmedOptionValueDeletionIds: Set<Long> = emptySet(),
        optionReplacementIds: Map<Long, Long> = emptyMap(),
        onSaved: (Long) -> Unit = {},
    ) = runOperation(if (id == null) "Creating Track…" else "Saving Track…", if (id == null) "Track created" else "Track saved") {
        val existingTags = id?.let { uiState.value.track(it)?.track?.tags }
        var automationInputsChanged = false
        val savedId = if (id == null) repository.create(draft) else {
            automationInputsChanged = repository.update(
                id,
                draft,
                confirmedFieldValueDeletionIds,
                confirmedOptionValueDeletionIds,
                optionReplacementIds,
            ).automationInputsChanged
            id
        }
        draft.tags.filter { tag -> existingTags?.none { it.equals(tag, ignoreCase = true) } != false }
            .forEach { app.measurementRepository.ensureTag(it) }
        if (automationInputsChanged) {
            app.linkRepository.rebuildAll()
            app.automationPromptScheduler.syncAll()
        }
        onSaved(savedId)
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
        app.linkRepository.rebuildAll()
        app.automationPromptScheduler.syncAll()
    }
    fun setArchived(ids: Collection<Long>, archived: Boolean) = runOperation(
        if (archived) "Archiving Tracks…" else "Restoring Tracks…",
        "${ids.size} Tracks ${if (archived) "archived" else "restored"}",
    ) {
        app.database.withTransaction { ids.distinct().forEach { repository.setArchived(it, archived) } }
        app.linkRepository.rebuildAll()
        app.automationPromptScheduler.syncAll()
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
        reconcileTrackAutomations()
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
        reconcileTrackAutomations()
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
            reconcileTrackAutomations()
        }
    }

    fun undoEntryDeletion(expectedToken: Long) {
        if (_lastDeletedEntry.value?.token != expectedToken) return
        runOperation("Restoring Entry…", "Entry restored") {
            _lastDeletedEntry.value
                ?.takeIf { it.token == expectedToken }
                ?.let { repository.restoreEntry(it.deletedEntry) }
            reconcileTrackAutomations()
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

    suspend fun searchEntryIds(trackId: Long, query: String): Set<Long> = repository.searchEntryIds(trackId, query)
    suspend fun entryPage(trackId: Long, offset: Int, limit: Int = 100): TrackEntryPage =
        repository.entryPage(trackId, offset, limit)

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
                    val csv = repository.exportCsv(trackId)
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

    fun createTrigger(draft: TriggerRuleDraft, onSaved: () -> Unit = {}) = runOperation("Creating Next-Action Automation…", "Next-Action Automation created") {
        app.linkRepository.createTrigger(draft)
        app.automationPromptScheduler.syncAll()
        onSaved()
    }

    fun updateTrigger(id: Long, draft: TriggerRuleDraft, onSaved: () -> Unit = {}) = runOperation("Saving Next-Action Automation…", "Next-Action Automation saved") {
        app.linkRepository.updateTrigger(id, draft)
        app.automationPromptScheduler.syncAll()
        onSaved()
    }

    fun deleteTrigger(id: Long) = runOperation("Removing Next-Action Automation…", "Next-Action Automation removed") {
        app.linkRepository.deleteTrigger(id)
        app.automationPromptScheduler.syncAll()
    }

    fun updateProgressAutomation(id: Long, draft: LinkRuleDraft, onSaved: () -> Unit = {}) = runOperation("Saving Goal Automation…", "Goal Automation saved") {
        alignGoalCalculation(draft)
        app.linkRepository.updateRule(id, draft)
        onSaved()
    }

    fun createProgressAutomation(
        draft: LinkRuleDraft,
        includeHistory: Boolean,
        onSaved: () -> Unit = {},
    ) = runOperation("Connecting Track to Goal…", "Track connected to Goal") {
        alignGoalCalculation(draft)
        app.linkRepository.createRule(draft, commitBackfill = includeHistory)
        onSaved()
    }

    fun setProgressAutomationEnabled(id: Long, enabled: Boolean) = runOperation("Updating Goal Automation…", "Goal Automation updated") {
        app.linkRepository.setRuleEnabled(id, enabled)
    }

    fun deleteProgressAutomation(id: Long) = runOperation("Removing Goal Automation…", "Goal Automation removed") {
        app.linkRepository.deleteRule(id)
    }

    fun dismissPrompt(id: Long) = runOperation("Dismissing prompt…", "Prompt dismissed") {
        app.linkRepository.dismissTriggerOccurrence(id)
        app.automationPromptScheduler.cancel(id)
    }

    fun remindPrompt(id: Long, at: java.time.Instant) = runOperation("Setting reminder…", "Reminder set") {
        app.linkRepository.remindTriggerOccurrence(id, at)
        app.automationPromptScheduler.syncAll()
    }

    fun loadPromptDraft(id: Long, onLoaded: (TrackEntryDraft) -> Unit) {
        viewModelScope.launch {
            try {
                onLoaded(app.linkRepository.trackPromptDraft(id))
            } catch (error: Throwable) {
                _operationStatus.value = OperationStatus.Failed(error.message ?: "Could not open this prompt", error)
            }
        }
    }

    fun fulfillPrompt(id: Long, draft: TrackEntryDraft, onSaved: (Long) -> Unit = {}) = runOperation(
        "Adding prompted Entry…",
        "Entry added",
    ) {
        val entryId = app.linkRepository.fulfillTrackPrompt(id, draft)
        reconcileTrackAutomations()
        app.automationPromptScheduler.cancel(id)
        onSaved(entryId)
    }

    fun createGoalFromTrack(trackId: Long, draft: TrackGoalAutomationDraft, onSaved: (Long) -> Unit = {}) = runOperation(
        "Creating Goal From Track…",
        "Goal and Automation created",
    ) {
        val projection = repository.projection(trackId) ?: error("Track no longer exists")
        val needsField = draft.aggregation in setOf(
            TrackAggregation.Sum,
            TrackAggregation.Average,
            TrackAggregation.Latest,
            TrackAggregation.Minimum,
            TrackAggregation.Maximum,
        )
        val field = draft.sourceFieldId?.let { id -> projection.fields.firstOrNull { it.id == id } }
        if (needsField) require(field?.type in setOf(TrackFieldType.Number, TrackFieldType.Scale)) {
            "Choose a Number or Scale Field"
        }
        val goalAggregation = when {
            draft.goalType == GoalType.Consistency -> GoalAggregation.CompletionCount
            draft.aggregation in setOf(TrackAggregation.Sum, TrackAggregation.CountEntries, TrackAggregation.CountMatchingEntries, TrackAggregation.FixedAmount) -> GoalAggregation.Sum
            draft.aggregation == TrackAggregation.Average -> GoalAggregation.Average
            draft.aggregation == TrackAggregation.Latest -> GoalAggregation.Latest
            draft.aggregation == TrackAggregation.Minimum -> GoalAggregation.Minimum
            draft.aggregation == TrackAggregation.Maximum -> GoalAggregation.Maximum
            else -> error("Choose a compatible Track measure")
        }
        require(draft.goalType != GoalType.Consistency || draft.aggregation in setOf(TrackAggregation.CountEntries, TrackAggregation.CountMatchingEntries)) {
            "Consistency Goals require Count Entries or Count Matching Entries"
        }
        require(goalAggregation in draft.goalType.compatibleAggregations()) {
            "${draft.aggregation.name} does not match ${draft.goalType.name}"
        }
        val dimension = when {
            draft.aggregation in setOf(TrackAggregation.CountEntries, TrackAggregation.CountMatchingEntries) -> UnitDimension.Count
            draft.aggregation == TrackAggregation.FixedAmount -> draft.fixedDimension
            field?.type == TrackFieldType.Scale -> UnitDimension.Unitless
            else -> field?.dimension ?: UnitDimension.Unitless
        }
        val unitId = when {
            draft.aggregation == TrackAggregation.FixedAmount -> draft.fixedUnitId
            dimension == UnitDimension.Count -> "count"
            field?.type == TrackFieldType.Scale -> "unitless"
            else -> field?.unitId ?: "unitless"
        }
        val goalDraft = GoalDraft(
            name = draft.goalName,
            description = "Progress from ${projection.track.name} Entries.",
            areaId = projection.track.areaId,
            area = projection.track.area,
            tags = projection.track.tags,
            icon = projection.track.icon,
            type = draft.goalType,
            dimension = dimension,
            unitId = unitId,
            precision = if (dimension == UnitDimension.Count) 0 else field?.precision ?: 1,
            targetMin = draft.target,
            targetMax = draft.targetMax,
            startDate = draft.retroactiveFrom ?: clock.today(),
            deadline = draft.deadline,
            aggregation = goalAggregation,
            consistencyPeriod = draft.consistencyPeriod,
            consistencyRequiredPeriods = draft.consistencyRequiredPeriods,
        )
        var goalId = 0L
        app.database.withTransaction {
            goalId = app.goalRepository.create(goalDraft)
            app.linkRepository.createRule(
                LinkRuleDraft(
                    name = "${projection.track.name} → ${draft.goalName}",
                    sourceType = LinkSourceType.Track,
                    sourceEntityId = trackId,
                    sourceMetric = if (needsField) LinkSourceMetric.FieldValue else LinkSourceMetric.EntryCount,
                    targetGoalId = goalId,
                    valueMode = if (draft.aggregation == TrackAggregation.FixedAmount) LinkValueMode.FixedValue else LinkValueMode.SourceValue,
                    fixedValue = draft.fixedAmount,
                    retroactiveFrom = draft.retroactiveFrom,
                    trackAggregation = draft.aggregation,
                    sourceFieldId = field?.id.takeIf { needsField },
                    conditionMode = draft.conditionMode,
                    conditions = draft.conditions,
                ),
                commitBackfill = draft.retroactiveFrom != null,
            )
        }
        app.goalReminderScheduler.syncGoal(goalId)
        onSaved(goalId)
    }

    private suspend fun reconcileTrackAutomations() {
        app.linkRepository.rebuildSources(setOf(LinkSourceType.Track))
        app.automationPromptScheduler.syncAll()
    }

    private suspend fun alignGoalCalculation(draft: LinkRuleDraft) {
        if (draft.sourceType != LinkSourceType.Track || draft.targetMilestoneId != null) return
        val measure = draft.trackAggregation ?: return
        val goal = app.goalRepository.goals.first().firstOrNull { it.id == draft.targetGoalId }
            ?: error("Goal no longer exists")
        val required = goal.requiredAggregationForTrack(measure)
        val aligned = goal.toTrackAutomationDraft(required, draft.retroactiveFrom)
        if (goal.aggregation != aligned.aggregation || goal.startDate != aligned.startDate) {
            app.goalRepository.update(goal.id, aligned)
        }
    }

    private fun runSilentReorder(block: suspend () -> Unit) {
        viewModelScope.launch {
            operationMutex.withLock {
                try {
                    block()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    _operationStatus.value = OperationStatus.Failed(error.message ?: "Could not save the new order", error)
                }
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
                    block()
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
