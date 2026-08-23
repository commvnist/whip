package com.whip.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.whip.app.WhipApplication
import com.whip.app.core.AppSettings
import com.whip.app.core.HomeSection
import com.whip.app.core.HealthDataType
import com.whip.app.core.SavedTaskFilter
import com.whip.app.core.SavedReviewFilter
import com.whip.app.core.PlatePreset
import com.whip.app.core.withoutAreaReferences
import com.whip.app.data.BackupPreview
import com.whip.app.data.EncryptedBackupCodec
import com.whip.app.data.PortableBackupOutcome
import com.whip.app.data.PortableBackupState
import com.whip.app.health.HealthConnectStatus
import com.whip.app.reminders.ALL_WHIP_WORK_TAG
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.Area
import com.whip.app.domain.AreaScope
import com.whip.app.domain.WhipTag
import com.whip.app.widget.WhipWidgetProvider
import java.time.DayOfWeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ExportKind { Backup, EncryptedBackup, TasksCsv, HabitsCsv, GoalsCsv, GymCsv }

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val backupPreview: BackupPreview? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val healthConnect: HealthConnectStatus = HealthConnectStatus(),
    val customUnits: List<UnitDefinition> = emptyList(),
    val areas: List<Area> = emptyList(),
    val areaUsage: Map<String, AreaUsageCounts> = emptyMap(),
    val unassignedAreaUsage: AreaUsageCounts = AreaUsageCounts(),
    val taxonomyLoaded: Boolean = false,
    val tags: List<WhipTag> = emptyList(),
    val portableBackup: PortableBackupState = PortableBackupState(),
    val encryptedRestorePending: Boolean = false,
)

data class AreaUsageCounts(val tasks: Int = 0, val habits: Int = 0, val goals: Int = 0) {
    val total: Int get() = tasks + habits + goals
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as WhipApplication
    private val repository = app.settingsRepository
    private val backups = app.backupRepository
    private val healthConnect = app.healthConnectManager
    private val runtime = MutableStateFlow(SettingsRuntime())
    private val healthRuntime = MutableStateFlow(HealthConnectStatus(availability = healthConnect.availability()))
    private var pendingRestoreJson: String? = null
    private var pendingEncryptedRestoreJson: String? = null

    private val taxonomyCore = combine(
        app.measurementRepository.customUnits,
        app.areaRepository.areas,
        app.measurementRepository.tags,
    ) { units, areas, tags -> TaxonomyState(units, areas, tags) }

    private val areaUsage = combine(
        app.taskRepository.tasks,
        app.habitRepository.habits,
        app.goalRepository.goals,
    ) { tasks, habits, goals ->
        val assigned = (tasks.mapNotNull { it.areaId } + habits.mapNotNull { it.areaId } + goals.mapNotNull { it.areaId })
            .distinct()
            .associateWith { id ->
                AreaUsageCounts(
                    tasks = tasks.count { it.areaId == id },
                    habits = habits.count { it.areaId == id },
                    goals = goals.count { it.areaId == id },
                )
            }
        AreaUsageState(
            assigned = assigned,
            unassigned = AreaUsageCounts(
                tasks = tasks.count { it.areaId == null },
                habits = habits.count { it.areaId == null },
                goals = goals.count { it.areaId == null },
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
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val tasks = app.taskRepository.tasks.first()
                val habits = app.habitRepository.habits.first()
                val goals = app.goalRepository.goals.first()
                (tasks.map { it.area } + habits.map { it.area } + goals.map { it.area })
                    .filter(String::isNotBlank).distinctBy(String::lowercase)
                    .forEach { app.measurementRepository.ensureArea(it) }
                (tasks.flatMap { it.tags } + habits.flatMap { it.tags } + goals.flatMap { it.tags })
                    .filter(String::isNotBlank).distinctBy(String::lowercase)
                    .forEach { app.measurementRepository.ensureTag(it) }
            }
        }
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        val before = repository.current()
        repository.update(transform)
        val after = repository.current()
        if (
            before.quietStartMinutes != after.quietStartMinutes ||
            before.quietEndMinutes != after.quietEndMinutes ||
            before.timeZoneId != after.timeZoneId
        ) {
            viewModelScope.launch {
                runCatching { app.reminderScheduler.syncAll() }
                runCatching { app.habitReminderScheduler.syncAll() }
                runCatching { app.goalReminderScheduler.syncAll() }
            }
        }
        if (before.locationRemindersEnabled != after.locationRemindersEnabled) {
            viewModelScope.launch { runCatching { app.locationReminderScheduler.syncAll() } }
        }
    }

    fun markNotificationPermissionRequested() = update {
        it.copy(notificationPermissionRequested = true)
    }

    fun setAreaScope(scope: AreaScope) = update { it.copy(activeAreaScope = scope.storageKey) }

    fun moveHomeSection(section: HomeSection, direction: Int) = update { settings ->
        val list = settings.homeSections.toMutableList()
        val from = list.indexOf(section)
        val to = (from + direction).coerceIn(0, list.lastIndex)
        if (from != to) java.util.Collections.swap(list, from, to)
        settings.copy(homeSections = list)
    }

    fun completeSetup(
        selectedAreas: Set<HomeSection>,
        powerMode: Boolean,
        usePounds: Boolean,
        lowPressureMode: Boolean,
        backupPrivacyChoice: String,
    ) = update { current ->
        current.copy(
            setupCompleted = true,
            powerMode = powerMode,
            lowPressureMode = lowPressureMode,
            backupPrivacyChoice = backupPrivacyChoice,
            backupPrivacyChoiceHandled = backupPrivacyChoice == "Later",
            hiddenHomeSections = HomeSection.entries.toSet() - selectedAreas,
            massUnitId = if (usePounds) "pound" else "kilogram",
            gymWeightUnitId = if (usePounds) "pound" else "kilogram",
        )
    }

    fun markBackupPrivacyChoiceHandled() = update {
        it.copy(backupPrivacyChoiceHandled = true)
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
            runCatching { withContext(Dispatchers.IO) { app.areaRepository.create(name, colorArgb) } }
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
            runCatching { withContext(Dispatchers.IO) { app.areaRepository.rename(id, name) } }
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
        if (repository.current().activeAreaScope == AreaScope.One(sourceId).storageKey) {
            repository.update { it.copy(activeAreaScope = AreaScope.One(targetId).storageKey) }
        }
    }
    fun deleteAreaAndKeepItems(id: String) = runIo("Area deleted; assigned items moved to No area") {
        val area = requireNotNull(uiState.value.areas.firstOrNull { it.id == id }) { "Area no longer exists" }
        app.areaRepository.deletePermanently(id)
        clearAreaReferences(id, area.name)
    }
    fun deleteAreaAndItems(id: String) = runIo("Area and assigned items permanently deleted") {
        val area = requireNotNull(uiState.value.areas.firstOrNull { it.id == id }) { "Area no longer exists" }
        val summary = app.areaDeletionCoordinator.deleteAreaAndItems(id)
        summary.taskIds.forEach { taskId ->
            app.reminderScheduler.syncTask(taskId)
            app.locationReminderScheduler.syncTask(taskId)
        }
        summary.habitIds.forEach { app.habitReminderScheduler.syncHabit(it) }
        summary.goalIds.forEach { app.goalReminderScheduler.syncGoal(it) }
        clearAreaReferences(id, area.name, summary.taskIds.toSet())
    }
    fun setAreaColor(id: String, colorArgb: Long?) = runIo("Area color updated") { app.areaRepository.setColor(id, colorArgb) }
    fun renameTag(id: String, name: String) = runIo("Tag updated") { app.measurementRepository.renameTag(id, name) }
    fun setAreaArchived(id: String, archived: Boolean) = runIo(if (archived) "Area archived" else "Area restored") {
        app.areaRepository.setArchived(id, archived)
        if (archived && repository.current().activeAreaScope == AreaScope.One(id).storageKey) {
            repository.update { it.copy(activeAreaScope = AreaScope.All.storageKey) }
        }
    }
    fun setTagArchived(id: String, archived: Boolean) = runIo(if (archived) "Tag archived" else "Tag restored") {
        app.measurementRepository.setTagArchived(id, archived)
    }
    fun moveArea(id: String, direction: Int) = runIo("Area order updated") { app.areaRepository.move(id, direction) }

    private fun clearAreaReferences(id: String, name: String, deletedTaskIds: Set<Long> = emptySet()) {
        repository.update { settings ->
            settings.withoutAreaReferences(id, name).let { cleaned ->
                if (cleaned.focusTimerTaskId in deletedTaskIds) {
                    cleaned.copy(focusTimerDeadlineMillis = null, focusTimerTaskId = null)
                } else {
                    cleaned
                }
            }
        }
        if (app.settingsRepository.current().focusTimerTaskId == null && deletedTaskIds.isNotEmpty()) {
            app.focusTimerScheduler.cancel()
        }
        WhipWidgetProvider.clearAreaScope(app, id)
    }

    fun saveReviewFilter(filter: SavedReviewFilter) = update { current ->
        current.copy(
            savedReviewFilters = (current.savedReviewFilters.filterNot { it.name.equals(filter.name, true) } + filter)
                .sortedBy { it.name.lowercase() },
            selectedReviewFilterName = filter.name,
            reviewSections = filter.sections,
        )
    }

    fun selectReviewFilter(name: String?) = update { current ->
        val selected = current.savedReviewFilters.firstOrNull { it.name == name }
        current.copy(
            selectedReviewFilterName = selected?.name,
            reviewSections = selected?.sections ?: HomeSection.entries.toSet(),
        )
    }

    fun deleteReviewFilter(name: String) = update { current ->
        current.copy(
            savedReviewFilters = current.savedReviewFilters.filterNot { it.name == name },
            selectedReviewFilterName = current.selectedReviewFilterName.takeUnless { it == name },
        )
    }

    fun setReviewSections(sections: Set<HomeSection>) = update { current ->
        current.copy(reviewSections = sections, selectedReviewFilterName = null)
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
        }
        app.contentResolver.openOutputStream(uri, "w")?.bufferedWriter()?.use { it.write(content) }
            ?: error("Could not open the selected file")
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

    fun confirmRestore() = runIo("Backup restored") {
        app.restoreRecoveryManager.restore(
            pendingRestoreJson ?: error("Choose a backup first"),
            app::rebuildBackgroundState,
        )
        pendingRestoreJson = null
        runtime.value = runtime.value.copy(preview = null)
    }

    fun confirmMerge() {
        viewModelScope.launch {
            runtime.value = runtime.value.copy(busy = true, message = null)
            runCatching {
                withContext(Dispatchers.IO) {
                    backups.mergeBackup(pendingRestoreJson ?: error("Choose a backup first")).also {
                        app.rebuildBackgroundState()
                    }
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
        check(app.portableBackupManager.backupNow() is PortableBackupOutcome.Saved)
    }

    fun setPortableBackupAutomatic(enabled: Boolean) {
        runCatching {
            app.portableBackupManager.setAutomaticEnabled(enabled)
            app.portableBackupScheduler.sync(app.portableBackupManager.state.value)
        }.onFailure { error ->
            runtime.value = runtime.value.copy(message = error.message ?: "Could not update automatic backups")
        }
    }

    fun setPortableBackupRetention(count: Int) {
        app.portableBackupManager.setRetentionCount(count)
    }

    fun clearPortableBackupFolder() = runIo("Backup folder forgotten; existing files were not deleted") {
        app.portableBackupManager.clearFolder()
        app.portableBackupScheduler.sync(app.portableBackupManager.state.value)
    }

    fun deleteAllData() = runIo("All Whip data deleted") {
        app.locationReminderScheduler.clearAll()
        backups.deleteAllData()
        WorkManager.getInstance(app).cancelAllWorkByTag(ALL_WHIP_WORK_TAG).result.get()
    }
    fun createCustomUnit(name: String, symbol: String, dimension: UnitDimension, factor: Double) =
        runIo("Custom unit created") {
            app.measurementRepository.createCustomUnit(name, symbol, dimension, factor)
        }
    fun renameCustomUnit(id: String, name: String, symbol: String) = runIo("Custom unit renamed") {
        app.measurementRepository.renameCustomUnit(id, name, symbol)
    }
    fun setCustomUnitArchived(id: String, archived: Boolean) = runIo(
        if (archived) "Custom unit archived" else "Custom unit restored",
    ) { app.measurementRepository.setCustomUnitArchived(id, archived) }
    fun createCustomUnitVersion(id: String, name: String, symbol: String, factor: Double) =
        runIo("New custom-unit version created; the old conversion remains with history") {
            app.measurementRepository.createCustomUnitVersion(id, name, symbol, factor)
        }
    fun consumeMessage() { runtime.value = runtime.value.copy(message = null) }

    fun requiredHealthPermissions(): Set<String> =
        healthConnect.requiredPermissions(repository.current().healthDataTypes)

    fun requiredHealthPermissionsFor(type: HealthDataType): String =
        healthConnect.requiredPermissions(setOf(type)).single()

    fun setHealthConnectEnabled(enabled: Boolean) = update { it.copy(healthConnectEnabled = enabled) }

    fun setHealthDataType(type: HealthDataType, enabled: Boolean) = update { current ->
        current.copy(
            healthDataTypes = if (enabled) current.healthDataTypes + type else current.healthDataTypes - type,
        )
    }

    fun refreshHealthConnect() {
        viewModelScope.launch {
            healthRuntime.value = runCatching { healthConnect.status(healthRuntime.value) }
                .getOrElse { healthRuntime.value.copy(message = it.message ?: "Could not check Health Connect") }
        }
    }

    fun onHealthPermissionsResult(@Suppress("UNUSED_PARAMETER") granted: Set<String>) {
        refreshHealthConnect()
        if (repository.current().healthConnectEnabled) syncHealthConnect()
    }

    fun syncHealthConnect() {
        viewModelScope.launch {
            runtime.value = runtime.value.copy(busy = true, message = null)
            val settings = repository.current()
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    healthConnect.sync(settings.healthDataTypes, settings.healthSyncDays).also {
                        app.linkRepository.rebuildAll()
                    }
                }
            }
            result.onSuccess { status ->
                healthRuntime.value = status
                runtime.value = runtime.value.copy(busy = false, message = status.message)
            }.onFailure { error ->
                healthRuntime.value = runCatching { healthConnect.status(healthRuntime.value) }
                    .getOrElse { healthRuntime.value }
                runtime.value = runtime.value.copy(
                    busy = false,
                    message = error.message ?: "Health Connect sync failed",
                )
            }
        }
    }

    private fun runIo(success: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            runtime.value = runtime.value.copy(busy = true, message = null)
            try {
                withContext(Dispatchers.IO) { block() }
                runtime.value = runtime.value.copy(busy = false, message = success)
            } catch (error: Throwable) {
                runtime.value = runtime.value.copy(busy = false, message = error.message ?: "Operation failed")
            }
        }
    }
}

private data class TaxonomyState(
    val units: List<UnitDefinition>,
    val areas: List<Area>,
    val tags: List<WhipTag>,
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
