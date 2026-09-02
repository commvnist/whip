package com.whip.app.ui

import android.app.Application
import android.net.Uri
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.whip.app.WhipApplication
import com.whip.app.core.AppSettings
import com.whip.app.core.HomeSection
import com.whip.app.core.ReviewSection
import com.whip.app.core.HealthDataType
import com.whip.app.core.SavedTaskFilter
import com.whip.app.core.PlatePreset
import com.whip.app.core.PersistenceRequestState
import com.whip.app.core.WhipResult
import com.whip.app.core.tryStartPersistenceRequest
import com.whip.app.core.withoutAreaReferences
import com.whip.app.data.BackupPreview
import com.whip.app.data.CommittedAreaDeletionCancellation
import com.whip.app.data.EncryptedBackupCodec
import com.whip.app.data.PortableBackupOutcome
import com.whip.app.data.PortableBackupState
import com.whip.app.health.HealthConnectStatus
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.Area
import com.whip.app.domain.AreaScope
import com.whip.app.domain.WhipTag
import com.whip.app.domain.CustomIdentityEmoji
import com.whip.app.domain.CustomUnitBoundary
import com.whip.app.domain.MetricSourceType
import com.whip.app.domain.normalizeCustomIdentityEmojis
import com.whip.app.widget.WhipWidgetProvider
import java.time.DayOfWeek
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.CancellationException
import java.util.UUID

enum class ExportKind { Backup, EncryptedBackup, TasksCsv, HabitsCsv, GoalsCsv, GymCsv, TracksCsv }

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val backupPreview: BackupPreview? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val healthConnect: HealthConnectStatus = HealthConnectStatus(),
    val customUnits: List<UnitDefinition> = emptyList(),
    val healthImportedEntryCount: Int = 0,
    val areas: List<Area> = emptyList(),
    val areaUsage: Map<String, AreaUsageCounts> = emptyMap(),
    val unassignedAreaUsage: AreaUsageCounts = AreaUsageCounts(),
    val taxonomyLoaded: Boolean = false,
    val tags: List<WhipTag> = emptyList(),
    val portableBackup: PortableBackupState = PortableBackupState(),
    val encryptedRestorePending: Boolean = false,
)

data class AreaUsageCounts(
    val tasks: Int = 0,
    val habits: Int = 0,
    val goals: Int = 0,
    val tracks: Int = 0,
    val trackEntries: Int = 0,
) {
    val total: Int get() = tasks + habits + goals + tracks
}

internal fun reminderDeliverySemanticsChanged(before: AppSettings, after: AppSettings): Boolean =
    before.quietStartMinutes != after.quietStartMinutes ||
        before.quietEndMinutes != after.quietEndMinutes ||
        before.timeZoneId != after.timeZoneId

data class SettingsMutationReceipt(
    val warnings: List<String> = emptyList(),
)

data class CustomUnitMutationReceipt(val unitId: String)

enum class HealthMutationKind { Policy, Sync, DeleteLocalCopies }

data class HealthMutationReceipt(
    val kind: HealthMutationKind,
    val affectedEntries: Int = 0,
    val warnings: List<String> = emptyList(),
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as WhipApplication
    private val repository = app.settingsRepository
    private val backups = app.backupRepository
    private val healthConnect = app.healthConnectManager
    private val runtime = MutableStateFlow(SettingsRuntime())
    private val healthRuntime = MutableStateFlow(
        repository.current().let { settings ->
            HealthConnectStatus(
                availability = healthConnect.availability(),
                lastSync = settings.healthLastSyncMillis?.let(Instant::ofEpochMilli),
                importedEntries = settings.healthLastSyncCount,
            )
        },
    )
    private val _typedSettingMutationState =
        MutableStateFlow<PersistenceRequestState<SettingsMutationReceipt>>(PersistenceRequestState.Idle)
    internal val typedSettingMutationState = _typedSettingMutationState.asStateFlow()
    private val _customUnitMutationState =
        MutableStateFlow<PersistenceRequestState<CustomUnitMutationReceipt>>(PersistenceRequestState.Idle)
    internal val customUnitMutationState = _customUnitMutationState.asStateFlow()
    private val _healthMutationState =
        MutableStateFlow<PersistenceRequestState<HealthMutationReceipt>>(PersistenceRequestState.Idle)
    internal val healthMutationState = _healthMutationState.asStateFlow()
    private var pendingRestoreJson: String? = null
    private var pendingEncryptedRestoreJson: String? = null

    private val taxonomyCore = combine(
        app.measurementRepository.customUnits,
        app.areaRepository.areas,
        app.measurementRepository.tags,
        app.measurementRepository.entries,
    ) { units, areas, tags, entries ->
        TaxonomyState(
            units = units,
            areas = areas,
            tags = tags,
            healthImportedEntryCount = entries.count { it.sourceType == MetricSourceType.HealthConnect },
        )
    }

    private val areaUsage = combine(
        app.taskRepository.tasks,
        app.habitRepository.habits,
        app.goalRepository.goals,
        app.trackRepository.tracks,
        app.trackRepository.entries,
    ) { tasks, habits, goals, tracks, trackEntries ->
        val trackAreaIds = tracks.associate { it.id to it.areaId }
        val assigned = (tasks.mapNotNull { it.areaId } + habits.mapNotNull { it.areaId } + goals.mapNotNull { it.areaId } + tracks.map { it.areaId })
            .distinct()
            .associateWith { id ->
                AreaUsageCounts(
                    tasks = tasks.count { it.areaId == id },
                    habits = habits.count { it.areaId == id },
                    goals = goals.count { it.areaId == id },
                    tracks = tracks.count { it.areaId == id },
                    trackEntries = trackEntries.count { trackAreaIds[it.trackId] == id },
                )
            }
        AreaUsageState(
            assigned = assigned,
            unassigned = AreaUsageCounts(
                tasks = tasks.count { it.areaId == null },
                habits = habits.count { it.areaId == null },
                goals = goals.count { it.areaId == null },
                tracks = 0,
            ),
        )
    }

    private val taxonomy = combine(taxonomyCore, areaUsage) { taxonomy, usage ->
        taxonomy.copy(usage = usage.assigned, unassignedUsage = usage.unassigned)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.settings,
        runtime,
        healthRuntime,
        taxonomy,
        app.portableBackupManager.state,
    ) { settings, state, health, taxonomy, portableBackup ->
        SettingsUiState(
            settings = settings,
            backupPreview = state.preview,
            busy = state.busy,
            message = state.message,
            healthConnect = health,
            customUnits = taxonomy.units,
            healthImportedEntryCount = taxonomy.healthImportedEntryCount,
            areas = taxonomy.areas,
            areaUsage = taxonomy.usage,
            unassignedAreaUsage = taxonomy.unassignedUsage,
            taxonomyLoaded = true,
            tags = taxonomy.tags,
            portableBackup = portableBackup,
            encryptedRestorePending = state.encryptedRestorePending,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsUiState(repository.current(), healthConnect = healthRuntime.value),
    )

    init {
        refreshHealthConnect()
        viewModelScope.launch {
            app.userDataGeneration.drop(1).collect {
                _typedSettingMutationState.value = PersistenceRequestState.Idle
                _customUnitMutationState.value = PersistenceRequestState.Idle
                _healthMutationState.value = PersistenceRequestState.Idle
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                app.withUserDataAccess {
                    val tasks = app.taskRepository.tasks.first()
                    val habits = app.habitRepository.habits.first()
                    val goals = app.goalRepository.goals.first()
                    val tracks = app.trackRepository.tracks.first()
                    (tasks.map { it.area } + habits.map { it.area } + goals.map { it.area } + tracks.map { it.area })
                        .filter(String::isNotBlank).distinctBy(String::lowercase)
                        .forEach { app.measurementRepository.ensureArea(it) }
                    (tasks.flatMap { it.tags } + habits.flatMap { it.tags } + goals.flatMap { it.tags } + tracks.flatMap { it.tags })
                        .filter(String::isNotBlank).distinctBy(String::lowercase)
                        .forEach { app.measurementRepository.ensureTag(it) }
                    Unit
                }
            }
        }
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        val preview = app.tryWithUserDataAccessNow {
            val before = repository.current()
            before to transform(before)
        } ?: return

        if (!reminderDeliverySemanticsChanged(preview.first, preview.second)) {
            app.tryWithUserDataAccessNow { repository.update(transform) }
            return
        }

        viewModelScope.launch {
            reminderSettingsUpdateMutex.withLock {
                val change = app.withUserDataAccess {
                    app.reminderDeliveryCoordinator.withStateBoundary {
                        val before = repository.current()
                        repository.update(transform)
                        before to repository.current()
                    }
                } ?: return@withLock
                if (!reminderDeliverySemanticsChanged(change.first, change.second)) return@withLock
                runCatching { app.reminderScheduler.syncAll() }
                runCatching { app.habitReminderScheduler.syncAll() }
                runCatching { app.goalReminderScheduler.syncAll() }
            }
        }
    }

    fun consumeTypedSettingMutation(requestId: String) {
        if ((_typedSettingMutationState.value as? PersistenceRequestState.Finished)?.requestId == requestId) {
            _typedSettingMutationState.value = PersistenceRequestState.Idle
        }
    }

    fun consumeCustomUnitMutation(requestId: String) {
        if ((_customUnitMutationState.value as? PersistenceRequestState.Finished)?.requestId == requestId) {
            _customUnitMutationState.value = PersistenceRequestState.Idle
        }
    }

    fun consumeHealthMutation(requestId: String) {
        if ((_healthMutationState.value as? PersistenceRequestState.Finished)?.requestId == requestId) {
            _healthMutationState.value = PersistenceRequestState.Idle
        }
    }

    /**
     * Admits one authored typed-setting save, commits it durably off the main
     * thread, and publishes a request-scoped terminal result. Observation of a
     * matching value is intentionally not treated as proof that this request
     * succeeded.
     */
    fun updateTypedSetting(
        requestId: String,
        transform: (AppSettings) -> AppSettings,
    ): Boolean {
        if (!_typedSettingMutationState.tryStartPersistenceRequest(requestId)) return false
        viewModelScope.launch(Dispatchers.IO) {
            val result: WhipResult<SettingsMutationReceipt> = try {
                val receipt = reminderSettingsUpdateMutex.withLock {
                    val change = app.withUserDataAccess {
                        app.healthConnectManager.withMutationBoundary {
                            app.reminderDeliveryCoordinator.withStateBoundary {
                                val before = repository.current()
                                check(repository.updateAndConfirm(transform)) {
                                    "Local storage did not confirm the settings change."
                                }
                                before to repository.current()
                            }
                        }
                    } ?: error("Whip data is temporarily unavailable; try again.")
                    val warnings = mutableListOf<String>()
                    if (reminderDeliverySemanticsChanged(change.first, change.second)) {
                        suspend fun sync(label: String, block: suspend () -> Unit) {
                            try {
                                block()
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                warnings += "$label could not be refreshed. The setting was saved; use Refresh Notification Status to retry."
                            }
                        }
                        sync("Task reminders") { app.reminderScheduler.syncAll() }
                        sync("Habit reminders") { app.habitReminderScheduler.syncAll() }
                        sync("Goal reminders") { app.goalReminderScheduler.syncAll() }
                    }
                    SettingsMutationReceipt(warnings)
                }
                WhipResult.Success(receipt)
            } catch (cancelled: CancellationException) {
                if ((_typedSettingMutationState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                    _typedSettingMutationState.value = PersistenceRequestState.Idle
                }
                throw cancelled
            } catch (error: Exception) {
                WhipResult.Failure(
                    error.message ?: "Whip could not save this setting. Your draft is still here.",
                    error,
                )
            }
            if ((_typedSettingMutationState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                _typedSettingMutationState.value = PersistenceRequestState.Finished(requestId, result)
            }
        }
        return true
    }

    fun markNotificationPermissionRequested() = update {
        it.copy(notificationPermissionRequested = true)
    }

    fun upsertCustomIdentityEmoji(originalEmoji: String? = null, choice: CustomIdentityEmoji) = update { current ->
        val replaceEmoji = originalEmoji ?: choice.emoji
        val existingIndex = current.customIdentityEmojis.indexOfFirst { it.emoji == replaceEmoji }
        val updatedChoices = if (existingIndex >= 0) {
            current.customIdentityEmojis.mapIndexed { index, existing ->
                if (index == existingIndex) choice else existing
            }
        } else {
            current.customIdentityEmojis + choice
        }
        current.copy(
            customIdentityEmojis = normalizeCustomIdentityEmojis(updatedChoices),
        )
    }

    fun removeCustomIdentityEmoji(emoji: String) = update { current ->
        current.copy(customIdentityEmojis = current.customIdentityEmojis.filterNot { it.emoji == emoji })
    }

    fun moveCustomIdentityEmoji(emoji: String, delta: Int) = update { current ->
        val index = current.customIdentityEmojis.indexOfFirst { it.emoji == emoji }
        if (index < 0) current else current.copy(
            customIdentityEmojis = moveListItem(current.customIdentityEmojis, index, delta),
        )
    }

    fun setAreaScope(scope: AreaScope) = update { it.copy(activeAreaScope = scope.storageKey) }

    fun moveHomeSection(section: HomeSection, direction: Int) = update { settings ->
        val from = settings.homeSections.indexOf(section)
        if (from < 0) settings else settings.copy(homeSections = moveListItem(settings.homeSections, from, direction))
    }

    fun completeSetup(
        selectedSections: Set<HomeSection>,
        powerMode: Boolean,
        usePounds: Boolean,
        lowPressureMode: Boolean,
    ) = update { current ->
        current.copy(
            setupCompleted = true,
            powerMode = powerMode,
            lowPressureMode = lowPressureMode,
            hiddenHomeSections = HomeSection.entries.toSet() - selectedSections,
            massUnitId = if (usePounds) "pound" else "kilogram",
            gymWeightUnitId = if (usePounds) "pound" else "kilogram",
        )
    }

    fun saveTaskFilter(filter: SavedTaskFilter) = update { current ->
        current.copy(
            savedTaskFilters = (current.savedTaskFilters.filterNot { it.name.equals(filter.name, true) } + filter)
                .sortedBy { it.name.lowercase() },
        )
    }

    fun deleteTaskFilter(name: String) = update { current ->
        current.copy(
            savedTaskFilters = current.savedTaskFilters.filterNot { it.name == name },
            homeTaskFilterName = current.homeTaskFilterName.takeUnless { it == name },
        )
    }

    fun selectHomeTaskFilter(name: String?) = update { it.copy(homeTaskFilterName = name) }

    fun startFocusTimer(taskId: Long, minutes: Int = 25) {
        val deadline = System.currentTimeMillis() + minutes.coerceIn(1, 240) * 60_000L
        update { it.copy(focusTimerDeadlineMillis = deadline, focusTimerTaskId = taskId) }
        app.focusTimerScheduler.schedule(taskId, deadline)
    }

    fun stopFocusTimer() {
        update { it.copy(focusTimerDeadlineMillis = null, focusTimerTaskId = null) }
        app.focusTimerScheduler.cancel()
    }

    fun createArea(
        name: String,
        colorArgb: Long? = null,
        onResult: (Result<String>) -> Unit = {},
    ) {
        viewModelScope.launch {
            runtime.value = runtime.value.copy(busy = true, message = null)
            val existing = uiState.value.areas.firstOrNull { it.name.equals(name.trim(), true) }
            runCatching {
                withContext(Dispatchers.IO) {
                    checkNotNull(app.withUserDataAccess { app.areaRepository.create(name, colorArgb) }) {
                        "Whip data is unavailable while recovery is in progress"
                    }
                }
            }
                .onSuccess { id ->
                    runtime.value = runtime.value.copy(
                        busy = false,
                        message = if (existing == null) "Area created" else "${existing.name} already exists; selected it instead",
                    )
                    onResult(Result.success(id))
                }
                .onFailure { error ->
                    runtime.value = runtime.value.copy(busy = false, message = error.message ?: "Could not create Area")
                    onResult(Result.failure(error))
                }
        }
    }

    fun renameArea(id: String, name: String, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            runtime.value = runtime.value.copy(busy = true, message = null)
            runCatching {
                withContext(Dispatchers.IO) {
                    checkNotNull(app.withUserDataAccess {
                        app.areaRepository.rename(id, name)
                        Unit
                    }) { "Whip data is unavailable while recovery is in progress" }
                }
            }
                .onSuccess {
                    runtime.value = runtime.value.copy(busy = false, message = "Area renamed")
                    onResult(Result.success(Unit))
                }
                .onFailure { error ->
                    runtime.value = runtime.value.copy(busy = false, message = error.message ?: "Could not rename Area")
                    onResult(Result.failure(error))
                }
        }
    }
    fun mergeAreas(sourceId: String, targetId: String) = runIo("Areas merged") {
        app.areaRepository.merge(sourceId, targetId)
        val sourceScope = AreaScope.One(sourceId).storageKey
        val targetScope = AreaScope.One(targetId).storageKey
        repository.update { current ->
            current.copy(
                activeAreaScope = if (current.activeAreaScope == sourceScope) targetScope else current.activeAreaScope,
                chosenOpeningAreaScope = if (current.chosenOpeningAreaScope == sourceScope) targetScope else current.chosenOpeningAreaScope,
            )
        }
    }
    fun moveAllAreaItems(sourceId: String, targetId: String) = runIo("Area items moved") {
        app.areaRepository.moveAssignments(sourceId, targetId)
        val currentSourceScope = AreaScope.One(sourceId)
        if (repository.current().activeAreaScope == currentSourceScope.storageKey) {
            repository.update { it.copy(activeAreaScope = AreaScope.One(targetId).storageKey) }
        }
    }
    fun deleteAreaAndKeepItems(id: String, replacementAreaId: String) {
        var committedWarnings: List<String> = emptyList()
        runIo(
            success = "Area deleted; assigned items moved",
            successDetail = { committedWarnings.joinToString(" ").takeIf(String::isNotBlank) },
        ) {
            requireNotNull(uiState.value.areas.firstOrNull { it.id == id }) { "Area no longer exists" }
            app.areaRepository.deletePermanently(id, replacementAreaId)
            committedWarnings = reconcileAreaReferencesAfterCommittedDeletion(id)
        }
    }
    fun deleteAreaAndItems(id: String) {
        var committedWarnings: List<String> = emptyList()
        runIo(
            success = "Area and assigned items permanently deleted",
            successDetail = { committedWarnings.joinToString(" ").takeIf(String::isNotBlank) },
        ) {
            requireNotNull(uiState.value.areas.firstOrNull { it.id == id }) { "Area no longer exists" }
            val summary = try {
                app.areaDeletionCoordinator.deleteAreaAndItems(id)
            } catch (cancelled: CommittedAreaDeletionCancellation) {
                if (!currentCoroutineContext().isActive) throw cancelled
                cancelled.summary
            }
            committedWarnings = summary.warnings + reconcileAreaReferencesAfterCommittedDeletion(
                id,
                summary.taskIds.toSet(),
            )
        }
    }
    fun setAreaColor(id: String, colorArgb: Long?) = runIo("Area color updated") { app.areaRepository.setColor(id, colorArgb) }
    fun renameTag(id: String, name: String) = runIo("Tag updated") { app.measurementRepository.renameTag(id, name) }
    fun setAreaArchived(id: String, archived: Boolean) = runIo(
        success = if (archived) "Area archived" else "Area restored",
        showSuccess = false,
    ) {
        app.areaRepository.setArchived(id, archived)
        if (archived) {
            repository.update { it.withoutAreaReferences(id) }
        }
    }
    fun setTagArchived(id: String, archived: Boolean) = runIo(if (archived) "Tag archived" else "Tag restored") {
        app.measurementRepository.setTagArchived(id, archived)
    }
    fun moveArea(id: String, direction: Int) {
        viewModelScope.launch {
            areaReorderMutex.withLock {
                try {
                    withContext(Dispatchers.IO) {
                        checkNotNull(app.withUserDataAccess {
                            app.areaRepository.move(id, direction)
                            Unit
                        }) { "Whip data is unavailable while recovery is in progress" }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    runtime.value = runtime.value.copy(message = error.message ?: "Could not save the new order")
                }
            }
        }
    }

    private fun reconcileAreaReferencesAfterCommittedDeletion(
        id: String,
        deletedTaskIds: Set<Long> = emptySet(),
    ): List<String> = reconcileCommittedAreaDeletion(
        "Settings cleanup" to {
            repository.update { settings ->
                settings.withoutAreaReferences(id).let { cleaned ->
                    if (cleaned.focusTimerTaskId in deletedTaskIds) {
                        cleaned.copy(focusTimerDeadlineMillis = null, focusTimerTaskId = null)
                    } else {
                        cleaned
                    }
                }
            }
        },
        "Focus timer cleanup" to {
            if (app.settingsRepository.current().focusTimerTaskId == null && deletedTaskIds.isNotEmpty()) {
                app.focusTimerScheduler.cancel()
            }
        },
        "Widget cleanup" to { WhipWidgetProvider.clearAreaScope(app, id) },
    )

    fun setReviewSections(sections: Set<ReviewSection>) = update { current ->
        current.copy(reviewSections = sections)
    }

    fun savePlatePreset(preset: PlatePreset) = update { current ->
        current.copy(
            platePresets = (current.platePresets.filterNot { it.name.equals(preset.name, true) } + preset)
                .sortedBy { it.name.lowercase() },
        )
    }

    fun deletePlatePreset(name: String) = update { current ->
        current.copy(platePresets = current.platePresets.filterNot { it.name == name })
    }

    fun export(uri: Uri, kind: ExportKind, passphrase: String? = null) = runIo(
        if (kind in setOf(ExportKind.Backup, ExportKind.EncryptedBackup)) "Backup saved" else "CSV saved",
    ) {
        app.withUserDataAccess {
            val content = when (kind) {
                ExportKind.Backup -> backups.exportBackup()
                ExportKind.EncryptedBackup -> EncryptedBackupCodec.encrypt(
                    backups.exportBackup(),
                    requireNotNull(passphrase) { "Enter an encryption passphrase" }.toCharArray(),
                )
                ExportKind.TasksCsv -> backups.exportTasksCsv()
                ExportKind.HabitsCsv -> backups.exportHabitsCsv()
                ExportKind.GoalsCsv -> backups.exportGoalsCsv()
                ExportKind.GymCsv -> backups.exportGymCsv()
                ExportKind.TracksCsv -> backups.exportTracksCsv()
            }
            app.contentResolver.openOutputStream(uri, "w")?.bufferedWriter()?.use { it.write(content) }
                ?: error("Could not open the selected file")
        } ?: error("Whip data is unavailable while recovery is in progress")
    }

    fun previewRestore(uri: Uri) = runIo("Backup validated") {
        val json = app.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Could not read the selected file")
        if (EncryptedBackupCodec.isEncrypted(json)) {
            pendingEncryptedRestoreJson = json
            pendingRestoreJson = null
            runtime.value = runtime.value.copy(preview = null, encryptedRestorePending = true)
            return@runIo
        }
        prepareRestore(json)
    }

    fun unlockEncryptedRestore(passphrase: String) = runIo("Encrypted backup validated") {
        val encrypted = pendingEncryptedRestoreJson ?: error("Choose an encrypted backup first")
        val json = EncryptedBackupCodec.decrypt(encrypted, passphrase.toCharArray())
        prepareRestore(json)
        pendingEncryptedRestoreJson = null
        runtime.value = runtime.value.copy(encryptedRestorePending = false)
    }

    fun cancelEncryptedRestore() {
        pendingEncryptedRestoreJson = null
        runtime.value = runtime.value.copy(encryptedRestorePending = false, message = null)
    }

    private suspend fun prepareRestore(json: String) {
        val preview = backups.previewBackup(json)
        require(preview.checksumValid) { "Backup checksum does not match" }
        pendingRestoreJson = json
        runtime.value = runtime.value.copy(preview = preview)
    }

    fun confirmRestore() = runIo("Backup restored", requiresDataAccess = false) {
        app.restoreBackup(pendingRestoreJson ?: error("Choose a backup first"))
        pendingRestoreJson = null
        runtime.value = runtime.value.copy(preview = null)
    }

    fun confirmMerge() {
        viewModelScope.launch {
            runtime.value = runtime.value.copy(busy = true, message = null)
            runCatching {
                withContext(Dispatchers.IO) {
                    app.withUserDataAccess {
                        val summary = app.reminderDeliveryCoordinator.withStateBoundary {
                            backups.mergeBackup(
                                pendingRestoreJson ?: error("Choose a backup first"),
                            ).also { NotificationManagerCompat.from(app).cancelAll() }
                        }
                        app.rebuildBackgroundState()
                        summary
                    } ?: error("Whip data is unavailable while recovery is in progress")
                }
            }.onSuccess { summary ->
                pendingRestoreJson = null
                runtime.value = runtime.value.copy(
                    busy = false,
                    preview = null,
                    message = "Imported ${summary.importedRecords} records · skipped ${summary.skippedExistingRecords} already present · kept current settings",
                )
            }.onFailure { error ->
                runtime.value = runtime.value.copy(busy = false, message = error.message ?: "Merge failed")
            }
        }
    }

    fun cancelRestore() {
        pendingRestoreJson = null
        runtime.value = runtime.value.copy(preview = null, message = null)
    }

    fun configurePortableBackupFolder(uri: Uri) = runIo("Backup folder ready") {
        app.portableBackupManager.configureFolder(uri)
        app.portableBackupScheduler.sync(app.portableBackupManager.state.value)
    }

    fun createPortableBackup() = runIo("Portable backup saved and verified") {
        app.withUserDataAccess {
            check(app.portableBackupManager.backupNow() is PortableBackupOutcome.Saved)
        } ?: error("Whip data is unavailable while recovery is in progress")
    }

    fun setPortableBackupAutomatic(enabled: Boolean) {
        runCatching {
            app.portableBackupManager.setAutomaticEnabled(enabled)
            app.portableBackupScheduler.sync(app.portableBackupManager.state.value)
        }.onFailure { error ->
            runtime.value = runtime.value.copy(message = error.message ?: "Could not update automatic backups")
        }
    }

    fun setPortableBackupRetention(requestId: String, count: Int): Boolean {
        if (!_typedSettingMutationState.tryStartPersistenceRequest(requestId)) return false
        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                check(app.portableBackupManager.setRetentionCountAndConfirm(count)) {
                    "Local storage did not confirm the backup-retention change."
                }
                WhipResult.Success(SettingsMutationReceipt())
            } catch (cancelled: CancellationException) {
                if ((_typedSettingMutationState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                    _typedSettingMutationState.value = PersistenceRequestState.Idle
                }
                throw cancelled
            } catch (error: Exception) {
                WhipResult.Failure(
                    error.message ?: "Whip could not save backup retention. Your draft is still here.",
                    error,
                )
            }
            if ((_typedSettingMutationState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                _typedSettingMutationState.value = PersistenceRequestState.Finished(requestId, result)
            }
        }
        return true
    }

    fun clearPortableBackupFolder() = runIo("Backup folder forgotten; existing files were not deleted") {
        app.portableBackupManager.clearFolder()
        app.portableBackupScheduler.sync(app.portableBackupManager.state.value)
    }

    fun deleteAllData(
        onSuccess: () -> Unit = {},
        onFailure: (Throwable) -> Unit = {},
    ) = runIo(
        success = "Whip reset; local data deleted",
        onSuccess = onSuccess,
        onFailure = onFailure,
        requiresDataAccess = false,
    ) {
        app.resetAllData()
    }
    fun createCustomUnit(
        requestedId: String,
        name: String,
        symbol: String,
        dimension: UnitDimension,
        factor: Double,
        onResult: (Result<String>) -> Unit = {},
    ) {
        viewModelScope.launch {
            runtime.value = runtime.value.copy(busy = true, message = null)
            runCatching {
                withContext(Dispatchers.IO) {
                    checkNotNull(app.withUserDataAccess {
                        app.measurementRepository.createCustomUnitExact(requestedId, name, symbol, dimension, factor)
                    }) { "Whip data is unavailable while recovery is in progress" }
                }
            }.onSuccess { id ->
                runtime.value = runtime.value.copy(busy = false, message = "Custom unit created")
                onResult(Result.success(id))
            }.onFailure { error ->
                runtime.value = runtime.value.copy(busy = false, message = error.message ?: "Could not create custom unit")
                onResult(Result.failure(error))
            }
        }
    }

    fun createCustomUnitMutation(
        requestId: String,
        requestedUnitId: String,
        name: String,
        symbol: String,
        dimension: UnitDimension,
        factor: Double,
    ): Boolean = runCustomUnitMutation(requestId) {
        app.measurementRepository.createCustomUnitExact(requestedUnitId, name, symbol, dimension, factor)
    }

    fun renameCustomUnitMutation(
        requestId: String,
        boundary: CustomUnitBoundary,
        name: String,
        symbol: String,
    ): Boolean = runCustomUnitMutation(requestId) {
        app.measurementRepository.renameCustomUnitExact(boundary, name, symbol)
        boundary.id
    }

    fun setCustomUnitArchivedMutation(
        requestId: String,
        boundary: CustomUnitBoundary,
        archived: Boolean,
    ): Boolean = runCustomUnitMutation(requestId) {
        app.measurementRepository.setCustomUnitArchivedExact(boundary, archived)
        boundary.id
    }

    fun createCustomUnitVersionMutation(
        requestId: String,
        boundary: CustomUnitBoundary,
        requestedUnitId: String,
        name: String,
        symbol: String,
        factor: Double,
    ): Boolean = runCustomUnitMutation(requestId) {
        app.measurementRepository.createCustomUnitVersionExact(
            boundary = boundary,
            requestedId = requestedUnitId,
            name = name,
            symbol = symbol,
            toCanonicalFactor = factor,
        )
    }

    private fun runCustomUnitMutation(
        requestId: String,
        block: suspend () -> String,
    ): Boolean {
        if (!_customUnitMutationState.tryStartPersistenceRequest(requestId)) return false
        viewModelScope.launch(Dispatchers.IO) {
            val result: WhipResult<CustomUnitMutationReceipt> = try {
                val unitId = checkNotNull(app.withUserDataAccess { block() }) {
                    "Whip data is temporarily unavailable; try again."
                }
                WhipResult.Success(CustomUnitMutationReceipt(unitId))
            } catch (cancelled: CancellationException) {
                if ((_customUnitMutationState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                    _customUnitMutationState.value = PersistenceRequestState.Idle
                }
                throw cancelled
            } catch (error: Exception) {
                WhipResult.Failure(
                    error.message ?: "Whip could not save this custom unit. Your draft is still here.",
                    error,
                )
            }
            if ((_customUnitMutationState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                _customUnitMutationState.value = PersistenceRequestState.Finished(requestId, result)
            }
        }
        return true
    }
    fun consumeMessage() { runtime.value = runtime.value.copy(message = null) }

    fun requiredHealthPermissions(): Set<String> =
        healthConnect.requiredPermissions(repository.current().healthDataTypes)

    fun requiredHealthPermissionsFor(type: HealthDataType): String =
        healthConnect.requiredPermissions(setOf(type)).single()

    fun setHealthConnectEnabled(requestId: String, enabled: Boolean): Boolean =
        runHealthMutation(requestId, HealthMutationKind.Policy) {
            check(healthConnect.updatePolicyAndConfirm { current ->
                check(!enabled || current.healthDataTypes.isNotEmpty()) {
                    "Choose at least one health category before turning on sync."
                }
                check(!enabled || !current.healthConnectDeletionPending) {
                    "Finish the pending local Health Connect deletion before turning sync on again."
                }
                current.copy(
                    healthConnectEnabled = enabled,
                )
            }) { "Local storage did not confirm the Health Connect setting." }
            HealthMutationReceipt(HealthMutationKind.Policy)
        }

    fun setHealthDataType(requestId: String, type: HealthDataType, enabled: Boolean): Boolean =
        runHealthMutation(requestId, HealthMutationKind.Policy) {
            check(healthConnect.updatePolicyAndConfirm { current ->
                val selected = if (enabled) current.healthDataTypes + type else current.healthDataTypes - type
                current.copy(
                    healthDataTypes = selected,
                    healthConnectEnabled = current.healthConnectEnabled && selected.isNotEmpty(),
                )
            }) { "Local storage did not confirm the Health Connect category." }
            HealthMutationReceipt(HealthMutationKind.Policy)
        }

    fun setHealthSyncDays(requestId: String, days: Int): Boolean {
        if (!_typedSettingMutationState.tryStartPersistenceRequest(requestId)) return false
        viewModelScope.launch(Dispatchers.IO) {
            val result: WhipResult<SettingsMutationReceipt> = try {
                check(days in 1..365) { "Health read window must be between 1 and 365 days." }
                check(healthConnect.updatePolicyAndConfirm { it.copy(healthSyncDays = days) }) {
                    "Local storage did not confirm the Health read window."
                }
                WhipResult.Success(SettingsMutationReceipt())
            } catch (cancelled: CancellationException) {
                if ((_typedSettingMutationState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                    _typedSettingMutationState.value = PersistenceRequestState.Idle
                }
                throw cancelled
            } catch (error: Exception) {
                WhipResult.Failure(error.message ?: "Whip could not save the Health read window.", error)
            }
            if ((_typedSettingMutationState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                _typedSettingMutationState.value = PersistenceRequestState.Finished(requestId, result)
            }
        }
        return true
    }

    fun refreshHealthConnect() {
        viewModelScope.launch {
            runCatching { healthConnect.status() }
                .onSuccess { checked ->
                    healthRuntime.update { current ->
                        current.copy(
                            availability = checked.availability,
                            grantedPermissions = checked.grantedPermissions,
                            message = checked.message,
                        )
                    }
                }
                .onFailure { error ->
                    healthRuntime.update { current ->
                        current.copy(message = error.message ?: "Could not check Health Connect")
                    }
                }
        }
    }

    fun onHealthPermissionsResult(@Suppress("UNUSED_PARAMETER") granted: Set<String>) {
        refreshHealthConnect()
    }

    fun syncHealthConnect(requestId: String): Boolean =
        runHealthMutation(requestId, HealthMutationKind.Sync) {
            val settings = repository.current()
            val status = checkNotNull(app.withUserDataAccess {
                healthConnect.sync(settings.healthDataTypes, settings.healthSyncDays)
            }) { "Whip data is temporarily unavailable; try again." }
            val warnings = mutableListOf<String>()
            try {
                app.withUserDataAccess { app.linkRepository.rebuildAll() }
                    ?: error("Whip data is temporarily unavailable")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                warnings += "Health records were synchronized, but linked Habit and trend summaries could not be refreshed. Restart Whip to retry."
            }
            if (!status.receiptPersisted) {
                warnings += "Health records were synchronized, but Whip could not preserve the last-sync receipt."
            }
            healthRuntime.value = status
            HealthMutationReceipt(
                kind = HealthMutationKind.Sync,
                affectedEntries = status.importedEntries,
                warnings = warnings,
            )
        }

    fun deleteHealthConnectCopies(requestId: String): Boolean =
        runHealthMutation(requestId, HealthMutationKind.DeleteLocalCopies) {
            val deletion = checkNotNull(app.withUserDataAccess { healthConnect.deleteImportedData() }) {
                "Whip data is temporarily unavailable; deletion will retry when Whip starts."
            }
            val warnings = mutableListOf<String>()
            val linksRebuilt = try {
                app.withUserDataAccess { app.linkRepository.rebuildAll() }
                    ?: error("Whip data is temporarily unavailable")
                true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                warnings += "Local Health Connect copies were deleted, but linked Habit and trend summaries could not be refreshed. Whip kept a recovery marker and will retry when it starts."
                false
            }
            if (linksRebuilt && !healthConnect.completeImportedDataDeletion()) {
                warnings += "Deletion finished, but Whip could not clear its recovery marker. It will safely verify deletion next time it starts."
            }
            val clearedStatus = healthRuntime.value.copy(lastSync = null, importedEntries = 0)
            healthRuntime.value = try {
                healthConnect.status(clearedStatus)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                warnings += "Local Health Connect copies were deleted, but Android access status could not be refreshed."
                clearedStatus
            }
            HealthMutationReceipt(
                kind = HealthMutationKind.DeleteLocalCopies,
                affectedEntries = deletion.deletedEntries,
                warnings = warnings,
            )
        }

    private fun runHealthMutation(
        requestId: String,
        kind: HealthMutationKind,
        block: suspend () -> HealthMutationReceipt,
    ): Boolean {
        if (!_healthMutationState.tryStartPersistenceRequest(requestId)) return false
        viewModelScope.launch(Dispatchers.IO) {
            val result: WhipResult<HealthMutationReceipt> = try {
                val receipt = block()
                WhipResult.Success(receipt)
            } catch (cancelled: CancellationException) {
                if ((_healthMutationState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                    _healthMutationState.value = PersistenceRequestState.Idle
                }
                throw cancelled
            } catch (error: Exception) {
                healthRuntime.value = runCatching { healthConnect.status(healthRuntime.value) }
                    .getOrElse { healthRuntime.value }
                WhipResult.Failure(
                    error.message ?: when (kind) {
                        HealthMutationKind.Policy -> "Whip could not save the Health Connect setting."
                        HealthMutationKind.Sync -> "Health Connect sync failed."
                        HealthMutationKind.DeleteLocalCopies -> "Whip could not delete its Health Connect copies. Deletion will retry when Whip starts."
                    },
                    error,
                )
            }
            if ((_healthMutationState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                _healthMutationState.value = PersistenceRequestState.Finished(requestId, result)
            }
        }
        return true
    }

    private val areaReorderMutex = Mutex()
    private val reminderSettingsUpdateMutex = Mutex()

    private fun runIo(
        success: String,
        successDetail: () -> String? = { null },
        onSuccess: () -> Unit = {},
        onFailure: (Throwable) -> Unit = {},
        showSuccess: Boolean = true,
        requiresDataAccess: Boolean = true,
        block: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            runtime.value = runtime.value.copy(busy = true, message = null)
            try {
                withContext(Dispatchers.IO) {
                    if (requiresDataAccess) {
                        checkNotNull(app.withUserDataAccess {
                            block()
                            Unit
                        }) { "Whip data is unavailable while recovery is in progress" }
                    } else {
                        block()
                    }
                }
                val successMessage = listOfNotNull(success, successDetail()).joinToString(" · ")
                runtime.value = runtime.value.copy(busy = false, message = successMessage.takeIf { showSuccess })
                onSuccess()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                runtime.value = runtime.value.copy(busy = false, message = error.message ?: "Operation failed")
                onFailure(error)
            }
        }
    }
}

internal fun reconcileCommittedAreaDeletion(
    vararg steps: Pair<String, () -> Unit>,
): List<String> = buildList {
    steps.forEach { (label, step) ->
        try {
            step()
        } catch (fatal: Error) {
            throw fatal
        } catch (_: Exception) {
            add("$label did not finish; the Area deletion was committed and cleanup will be reconciled later.")
        }
    }
}

private data class TaxonomyState(
    val units: List<UnitDefinition>,
    val areas: List<Area>,
    val tags: List<WhipTag>,
    val healthImportedEntryCount: Int = 0,
    val usage: Map<String, AreaUsageCounts> = emptyMap(),
    val unassignedUsage: AreaUsageCounts = AreaUsageCounts(),
)

private data class AreaUsageState(
    val assigned: Map<String, AreaUsageCounts>,
    val unassigned: AreaUsageCounts,
)

private data class SettingsRuntime(
    val preview: BackupPreview? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val encryptedRestorePending: Boolean = false,
)
