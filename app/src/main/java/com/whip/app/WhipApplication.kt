package com.whip.app

import android.app.Application
import android.util.Log
import com.whip.app.core.SettingsWhipClock
import com.whip.app.core.AndroidHabitTimerClock
import com.whip.app.core.SharedPreferencesSettingsRepository
import com.whip.app.core.UuidWhipIdGenerator
import com.whip.app.data.RoomTaskRepository
import com.whip.app.data.TaskRepository
import com.whip.app.data.RoomMeasurementRepository
import com.whip.app.data.MeasurementRepository
import com.whip.app.data.RoomAreaRepository
import com.whip.app.data.RoomGymRepository
import com.whip.app.data.RoomRoutineRepository
import com.whip.app.data.RoomHabitRepository
import com.whip.app.data.HabitRepository
import com.whip.app.data.RoomGoalRepository
import com.whip.app.data.GoalRepository
import com.whip.app.data.RoomLinkRepository
import com.whip.app.data.RoomTrackRepository
import com.whip.app.data.RoomBackupRepository
import com.whip.app.data.TaskDeletionCoordinator
import com.whip.app.data.DomainDeletionCoordinator
import com.whip.app.data.AreaDeletionCoordinator
import com.whip.app.data.AreaDeletionSummary
import com.whip.app.data.PortableBackupManager
import com.whip.app.data.PortableBackupScheduler
import com.whip.app.data.PORTABLE_BACKUP_WORK_NAME
import com.whip.app.data.WhipDatabase
import com.whip.app.data.RestoreRecoveryManager
import com.whip.app.reminders.ReminderNotifications
import com.whip.app.reminders.ReminderDeliveryCoordinator
import com.whip.app.reminders.ReminderDeletionCleanupStore
import com.whip.app.reminders.ReminderDomain
import com.whip.app.reminders.CoordinatedTaskRepository
import com.whip.app.reminders.CoordinatedMeasurementRepository
import com.whip.app.reminders.CoordinatedHabitRepository
import com.whip.app.reminders.CoordinatedGoalRepository
import com.whip.app.reminders.ReminderRuntimeMaintenance
import com.whip.app.reminders.ReminderTimeInvalidationPlan
import com.whip.app.reminders.ReminderScheduler
import com.whip.app.reminders.SharedPreferencesReminderClaimVersionStore
import com.whip.app.reminders.cancelVisibleReminderNotifications
import com.whip.app.reminders.cancelVisibleTaskNotifications
import com.whip.app.reminders.RestTimerNotifications
import com.whip.app.reminders.RestTimerScheduler
import com.whip.app.reminders.restTimerScheduleDelaySeconds
import com.whip.app.reminders.HabitReminderScheduler
import com.whip.app.reminders.HabitReminderNotifications
import com.whip.app.reminders.GoalReminderScheduler
import com.whip.app.reminders.GoalReminderNotifications
import com.whip.app.reminders.AutomationPromptScheduler
import com.whip.app.reminders.FocusTimerNotifications
import com.whip.app.reminders.FocusTimerScheduler
import com.whip.app.health.HealthConnectManager
import com.whip.app.startup.StartupRecoveryGate
import com.whip.app.startup.StartupRecoveryState
import com.whip.app.startup.DataEpochGate
import com.whip.app.startup.DataEpochState
import com.whip.app.startup.LocalDataResetter
import com.whip.app.startup.FreshStartRetryAction
import com.whip.app.startup.freshStartRetryAction
import com.whip.app.startup.generationMatches
import com.whip.app.widget.WhipWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.core.app.NotificationManagerCompat
import androidx.work.Configuration
import androidx.work.WorkManager
import com.whip.app.reminders.ALL_WHIP_WORK_TAG
import com.whip.app.domain.MetricEntry
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.core.zoneId
import com.whip.app.core.calendarContextAt
import com.whip.app.core.calendarContextFlow

@OptIn(FlowPreview::class)
class WhipApplication : Application(), Configuration.Provider {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var startupRecoveryGate: StartupRecoveryGate
    private lateinit var dataEpochGate: DataEpochGate
    private val mutableStartupState = MutableStateFlow<StartupRecoveryState>(StartupRecoveryState.Checking)
    private var startupStateJob: Job? = null
    private var normalRuntimeJob: Job? = null
    private val epochResetMutex = Mutex()
    private val recoveryPreferences by lazy {
        getSharedPreferences(RECOVERY_RUNTIME_PREFERENCES, MODE_PRIVATE)
    }
    private val mutableUserDataGeneration by lazy {
        MutableStateFlow(recoveryPreferences.getLong(USER_DATA_GENERATION, 0L))
    }
    val settingsRepository by lazy { SharedPreferencesSettingsRepository(this) }
    val clock by lazy { SettingsWhipClock(settingsRepository) }
    private val calendarInvalidations = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val calendarContext by lazy {
        settingsRepository.calendarContextFlow(clock, calendarInvalidations).stateIn(
            applicationScope,
            SharingStarted.Eagerly,
            settingsRepository.current().calendarContextAt(clock.now()),
        )
    }
    val idGenerator = UuidWhipIdGenerator
    val database by lazy { WhipDatabase.get(this) }
    internal val rawTaskRepository by lazy { RoomTaskRepository(database, clock) }
    private val reminderDeletionCleanupStore by lazy { ReminderDeletionCleanupStore(this) }
    val taskRepository: TaskRepository by lazy {
        CoordinatedTaskRepository(rawTaskRepository, reminderDeliveryCoordinator)
    }
    internal val rawMeasurementRepository by lazy {
        RoomMeasurementRepository(database, clock, idGenerator)
    }
    val measurementRepository: MeasurementRepository by lazy {
        CoordinatedMeasurementRepository(rawMeasurementRepository, reminderDeliveryCoordinator)
    }
    val areaRepository by lazy { RoomAreaRepository(database, clock, idGenerator) }
    val gymRepository by lazy { RoomGymRepository(database, clock, idGenerator, settingsRepository) }
    val routineRepository by lazy { RoomRoutineRepository(database, clock, idGenerator, settingsRepository) }
    internal val rawHabitRepository by lazy {
        RoomHabitRepository(database, rawMeasurementRepository, clock, idGenerator, AndroidHabitTimerClock(this, clock))
    }
    val habitRepository: HabitRepository by lazy {
        CoordinatedHabitRepository(rawHabitRepository, reminderDeliveryCoordinator)
    }
    internal val rawGoalRepository by lazy {
        RoomGoalRepository(database, rawMeasurementRepository, clock, idGenerator)
    }
    val goalRepository: GoalRepository by lazy {
        CoordinatedGoalRepository(rawGoalRepository, reminderDeliveryCoordinator)
    }
    private val rawLinkRepository by lazy {
        RoomLinkRepository(database, rawMeasurementRepository, clock, idGenerator)
    }
    val linkRepository by lazy {
        RoomLinkRepository(database, measurementRepository, clock, idGenerator)
    }
    val trackRepository by lazy {
        RoomTrackRepository(database, clock, idGenerator)
    }
    val taskDeletionCoordinator by lazy {
        TaskDeletionCoordinator(
            database,
            rawTaskRepository,
            rawLinkRepository,
            reminderDeliveryCoordinator,
            onDeletionPrepared = { ids -> prepareReminderDeletion(ReminderDomain.Task, ids) },
            onDeletionCommitted = { ids -> commitReminderDeletion(ReminderDomain.Task, ids) },
            onDeletionInterrupted = ::reconcilePendingReminderDeletions,
        )
    }
    val domainDeletionCoordinator by lazy {
        DomainDeletionCoordinator(
            database,
            rawLinkRepository,
            routineRepository,
            reminderDeliveryCoordinator,
            onDeletionPrepared = ::prepareReminderDeletion,
            onDeletionCommitted = ::commitReminderDeletion,
            onDeletionInterrupted = ::reconcilePendingReminderDeletions,
        )
    }
    val areaDeletionCoordinator by lazy {
        val taskDeletionsWithinArea = TaskDeletionCoordinator(
            database,
            rawTaskRepository,
            rawLinkRepository,
        )
        val domainDeletionsWithinArea = DomainDeletionCoordinator(
            database,
            rawLinkRepository,
            routineRepository,
        )
        AreaDeletionCoordinator(
            database,
            areaRepository,
            taskDeletionsWithinArea,
            domainDeletionsWithinArea,
            reminderDeliveryCoordinator,
            onDeletionPrepared = ::prepareAreaReminderDeletion,
            onDeletionCommitted = ::commitAreaReminderDeletion,
            onDeletionInterrupted = ::reconcilePendingReminderDeletions,
        )
    }
    val backupRepository by lazy { RoomBackupRepository(database, settingsRepository, areaRepository) }
    private val restoreRecoveryManager by lazy { RestoreRecoveryManager(this, backupRepository) }
    val portableBackupManager by lazy {
        PortableBackupManager(
            context = this,
            backupRepository = backupRepository,
            zoneId = { settingsRepository.current().zoneId() },
        )
    }
    val portableBackupScheduler by lazy { PortableBackupScheduler(this) }
    internal val reminderDeliveryCoordinator by lazy { ReminderDeliveryCoordinator() }
    val reminderScheduler by lazy { ReminderScheduler(this, settingsRepository) }
    val restTimerScheduler by lazy { RestTimerScheduler(this) }
    val habitReminderScheduler by lazy { HabitReminderScheduler(this, settingsRepository) }
    val goalReminderScheduler by lazy { GoalReminderScheduler(this, settingsRepository) }
    private val reminderRuntimeMaintenance by lazy {
        ReminderRuntimeMaintenance(
            versionStore = SharedPreferencesReminderClaimVersionStore(this),
            cancelVisibleLegacyReminders = { cancelVisibleReminderNotifications(this) },
            syncTaskReminders = {
                reminderScheduler.syncAll(allowDuringRecovery = true)
            },
            syncHabitReminders = {
                habitReminderScheduler.syncAll(allowDuringRecovery = true)
            },
            syncGoalReminders = {
                goalReminderScheduler.syncAll(allowDuringRecovery = true)
            },
            refreshWidgets = { WhipWidgetProvider.updateAll(this) },
        )
    }
    val automationPromptScheduler by lazy { AutomationPromptScheduler(this) }
    val focusTimerScheduler by lazy { FocusTimerScheduler(this) }
    val healthConnectManager by lazy { HealthConnectManager(this, measurementRepository, settingsRepository) }
    val startupRecoveryState: StateFlow<StartupRecoveryState>
        get() = mutableStartupState
    val userDataGeneration: StateFlow<Long>
        get() = mutableUserDataGeneration
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setMinimumLoggingLevel(Log.INFO).build()

    override fun onCreate() {
        super.onCreate()
        // Channel registration is safe platform setup and must precede any
        // recovery rebuild that can enqueue an immediate notification worker.
        createNotificationChannels()
        dataEpochGate = DataEpochGate(this)
        val epochState = runCatching(dataEpochGate::evaluate).getOrElse { error ->
            Log.e(LOG_TAG, "Data epoch could not be established", error)
            mutableStartupState.value = StartupRecoveryState.FreshStartCheckBlocked(error.message)
            showUpdateRequiredWidgetsBestEffort()
            return
        }
        when (epochState) {
            is DataEpochState.Current -> runBlocking(Dispatchers.IO) { startCanonicalRuntime() }
            DataEpochState.ResetRequired -> {
                mutableStartupState.value = StartupRecoveryState.FreshStartRequired
                showUpdateRequiredWidgetsBestEffort()
            }
            is DataEpochState.ResetInProgress -> {
                mutableStartupState.value = StartupRecoveryState.FreshStartResetting
                showUpdateRequiredWidgetsBestEffort()
                applicationScope.launch(Dispatchers.IO) { performEpochReset() }
            }
        }
    }

    private suspend fun startCanonicalRuntime() {
        var recoveryRebuiltBackground = false
        startupRecoveryGate = StartupRecoveryGate(
            recoverPendingRestore = {
                recoveryRebuiltBackground =
                    restoreRecoveryManager.recoverIfNeeded(::rebuildBackgroundState) ||
                    recoveryRebuiltBackground
            },
            initializeNormalRuntime = {
                initializeNormalRuntime(backgroundAlreadyRebuilt = recoveryRebuiltBackground)
                recoveryRebuiltBackground = false
            },
        )
        startupStateJob?.cancel()
        startupStateJob = applicationScope.launch {
            startupRecoveryGate.state.collect { mutableStartupState.value = it }
        }
        startupRecoveryGate.start()?.let(::logStartupRecoveryFailure)
        mutableStartupState.value = startupRecoveryGate.state.value
    }

    fun retryStartupRecovery() {
        val state = mutableStartupState.value
        when (state.freshStartRetryAction()) {
            FreshStartRetryAction.ReevaluateEpoch -> {
                if (!mutableStartupState.compareAndSet(state, StartupRecoveryState.FreshStartChecking)) return
                applicationScope.launch(Dispatchers.IO) { retryDataEpochCheck() }
            }
            FreshStartRetryAction.ResumeConfirmedReset -> {
                if (!mutableStartupState.compareAndSet(state, StartupRecoveryState.FreshStartResetting)) return
                applicationScope.launch(Dispatchers.IO) { performEpochReset() }
            }
            FreshStartRetryAction.RetryStartupRecovery -> {
                if (state !is StartupRecoveryState.Blocked) return
                applicationScope.launch(Dispatchers.IO) {
                    startupRecoveryGate.retry()?.let(::logStartupRecoveryFailure)
                }
            }
        }
    }

    private suspend fun retryDataEpochCheck() {
        if (mutableStartupState.value != StartupRecoveryState.FreshStartChecking) return
        val epochState = runCatching(dataEpochGate::evaluate).getOrElse { error ->
            Log.e(LOG_TAG, "Data epoch still could not be established", error)
            mutableStartupState.value = StartupRecoveryState.FreshStartCheckBlocked(error.message)
            showUpdateRequiredWidgetsBestEffort()
            return
        }
        when (epochState) {
            is DataEpochState.Current -> startCanonicalRuntime()
            DataEpochState.ResetRequired -> {
                mutableStartupState.value = StartupRecoveryState.FreshStartRequired
                showUpdateRequiredWidgetsBestEffort()
            }
            is DataEpochState.ResetInProgress -> {
                mutableStartupState.value = StartupRecoveryState.FreshStartResetting
                performEpochReset()
            }
        }
    }

    fun beginFreshStartReset() {
        if (mutableStartupState.value != StartupRecoveryState.FreshStartRequired) return
        mutableStartupState.value = StartupRecoveryState.FreshStartResetting
        applicationScope.launch(Dispatchers.IO) { performEpochReset() }
    }

    private suspend fun performEpochReset() = epochResetMutex.withLock {
        try {
            // Always rewrite and fsync the marker, including crash-resume and Retry paths. No
            // deletion is allowed to rely only on a marker observed during an earlier process.
            dataEpochGate.markResetInProgress()
            val generation = LocalDataResetter(this).resetAndVerify()
            mutableUserDataGeneration.value = generation
            dataEpochGate.markCurrent()
            startCanonicalRuntime()
        } catch (error: Throwable) {
            Log.e(LOG_TAG, "Fresh-start reset did not complete", error)
            mutableStartupState.value = StartupRecoveryState.FreshStartBlocked(error.message)
            showUpdateRequiredWidgetsBestEffort()
        }
    }

    private fun showUpdateRequiredWidgetsBestEffort() {
        runCatching { WhipWidgetProvider.showUpdateRequired(this) }
            .onFailure { error -> Log.e(LOG_TAG, "Could not lock widgets for the data epoch gate", error) }
    }

    /**
     * The only supported replace-restore entry point. It closes the same gate
     * used at process startup before a recovery snapshot or repository can be
     * touched, and it leaves the process blocked if rollback remains pending.
     */
    suspend fun restoreBackup(targetJson: String) = startupRecoveryGate.runRestore(
        prepareForRestore = ::quiesceNormalRuntime,
        restore = {
            restoreRecoveryManager.restore(
                targetJson = targetJson,
                rebuildBackgroundState = ::rebuildBackgroundState,
                onRecoveryPrepared = { advanceUserDataGeneration() },
            )
        },
        hasPendingRecovery = restoreRecoveryManager::hasPendingRecovery,
        resumeNormalRuntime = ::resumeAfterRestoreMaintenance,
    )

    /**
     * Whole-app reset is exclusive with every repository lease, worker, and
     * editor. Old ownership is invalidated before any table can be cleared.
     */
    suspend fun resetAllData() = startupRecoveryGate.runExclusiveMaintenance(
        prepareForMaintenance = ::quiesceNormalRuntime,
        maintenance = {
            portableBackupManager.clearFolder()
            healthConnectManager.withMutationBoundary {
                reminderDeliveryCoordinator.withStateBoundary {
                    NotificationManagerCompat.from(this).cancelAll()
                    advanceUserDataGeneration()
                    backupRepository.deleteAllData()
                    NotificationManagerCompat.from(this).cancelAll()
                }
            }
        },
        resumeNormalRuntime = ::resumeAfterRestoreMaintenance,
    )

    fun currentUserDataGeneration(): Long = mutableUserDataGeneration.value

    fun isCurrentUserDataGeneration(
        presented: Long,
    ): Boolean = generationMatches(currentUserDataGeneration(), presented)

    suspend fun <T : Any> withUserDataAccess(block: suspend () -> T): T? =
        if (!::startupRecoveryGate.isInitialized || mutableStartupState.value != StartupRecoveryState.Ready) null else startupRecoveryGate.withReadyDataAccess(
            additionalCheck = { !restoreRecoveryManager.hasPendingRecovery() },
            block = block,
        )

    fun <T : Any> tryWithUserDataAccessNow(block: () -> T): T? =
        if (!::startupRecoveryGate.isInitialized || mutableStartupState.value != StartupRecoveryState.Ready) null else startupRecoveryGate.tryWithReadyDataAccessNow(
            additionalCheck = { !restoreRecoveryManager.hasPendingRecovery() },
            block = block,
        )

    internal suspend fun blockForPendingRecovery() {
        startupRecoveryGate.blockForPendingRecovery(
            hasPendingRecovery = restoreRecoveryManager::hasPendingRecovery,
            prepareForRecovery = ::quiesceNormalRuntime,
        )
    }

    internal suspend fun reconcileReminderTimeInvalidation(action: String): ReminderTimeInvalidationPlan? {
        calendarInvalidations.tryEmit(Unit)
        return withUserDataAccess {
            reminderRuntimeMaintenance.handleSystemTimeInvalidation(
                action = action,
                followsDeviceTimeZone = settingsRepository.current().timeZoneId == null,
            )
        }
    }

    private suspend fun initializeNormalRuntime(backgroundAlreadyRebuilt: Boolean) {
        if (normalRuntimeJob?.isActive == true) return
        var settings = settingsRepository.current()
        if (!backgroundAlreadyRebuilt) {
            areaRepository.ensureDefaultArea()
        }
        habitRepository.reconcileTimerClockState()
        // Existing persisted reminder work cannot be trusted across a delivery
        // claim schema change. This is awaited while the startup recovery gate
        // is still closed, before receivers or normal runtime jobs can schedule.
        reconcilePendingReminderDeletions()
        if (settings.healthConnectDeletionPending) {
            healthConnectManager.deleteImportedData()
            linkRepository.rebuildAll()
            check(healthConnectManager.completeImportedDataDeletion()) {
                "Could not complete the pending Health Connect deletion"
            }
            settings = settingsRepository.current()
        }
        reminderRuntimeMaintenance.upgradeDeliveryClaimsIfRequired()
        portableBackupScheduler.sync(portableBackupManager.state.value, allowDuringRecovery = true)
        if (!backgroundAlreadyRebuilt) {
            val deadline = settings.focusTimerDeadlineMillis
            val taskId = settings.focusTimerTaskId
            if (deadline != null && taskId != null && deadline > System.currentTimeMillis()) {
                focusTimerScheduler.schedule(taskId, deadline, allowDuringRecovery = true)
            }
        }
        val runtimeJob = SupervisorJob(applicationScope.coroutineContext[Job])
        val runtimeScope = CoroutineScope(runtimeJob + Dispatchers.Default)
        normalRuntimeJob = runtimeJob
        runtimeScope.launch { runCatching { portableBackupManager.recoverInterruptedWrites() } }
        if (!backgroundAlreadyRebuilt) {
            runtimeScope.launch { runCatching { automationPromptScheduler.syncAll() } }
        }
        if (settings.healthConnectEnabled && !settings.healthConnectDeletionPending) {
            runtimeScope.launch {
                runCatching {
                    healthConnectManager.sync(settings.healthDataTypes, settings.healthSyncDays)
                    runCatching { withUserDataAccess { linkRepository.rebuildAll() } }
                }
            }
        }
        runtimeScope.launch {
            merge(
                taskRepository.tasks.map { Unit }, taskRepository.occurrences.map { Unit },
                taskRepository.steps.map { Unit }, taskRepository.stepStates.map { Unit },
                taskRepository.stepSnapshots.map { Unit },
                habitRepository.habits.map { Unit }, habitRepository.logs.map { Unit },
                habitRepository.checklistItems.map { Unit }, habitRepository.checklistStates.map { Unit },
                habitRepository.pauses.map { Unit }, habitRepository.skips.map { Unit },
                measurementRepository.entries.map { Unit }, measurementRepository.customUnits.map { Unit },
                areaRepository.areas.map { Unit }, calendarContext.map { Unit },
            ).debounce(250).collectLatest {
                withUserDataAccess { WhipWidgetProvider.updateAll(this@WhipApplication) }
            }
        }
        // A source-backed Habit can cross its target without a Habit mutation:
        // Health Connect, imports, Tracks, Goals, and manual measurements all
        // write metric entries directly. Diff those shared flows here so every
        // writer gets the same bounded reminder reconciliation.
        runtimeScope.launch {
            var previousByMetric = emptyMap<String, List<MetricEntry>>()
            measurementRepository.entries.debounce(250).collect { entries ->
                val currentByMetric = entries.groupBy(MetricEntry::metricId)
                val changedMetricIds = (previousByMetric.keys + currentByMetric.keys)
                    .filterTo(sortedSetOf()) { metricId ->
                        previousByMetric[metricId] != currentByMetric[metricId]
                    }
                try {
                    changedMetricIds.forEach { metricId ->
                        habitReminderScheduler.syncSourceMetric(metricId)
                    }
                    previousByMetric = currentByMetric
                } catch (error: Throwable) {
                    Log.e(LOG_TAG, "Could not reconcile metric-backed Habit reminders", error)
                }
            }
        }
        // Unit edits can change the canonical value of both a Habit target and
        // its historical logs. Include removals by comparing the union of IDs.
        runtimeScope.launch {
            var previousUnits = emptyMap<String, UnitDefinition>()
            measurementRepository.customUnits.debounce(250).collect { units ->
                val currentUnits = units.associateBy(UnitDefinition::id)
                val changedUnitIds = (previousUnits.keys + currentUnits.keys)
                    .filterTo(sortedSetOf()) { unitId ->
                        previousUnits[unitId] != currentUnits[unitId]
                    }
                try {
                    changedUnitIds.forEach { unitId ->
                        habitReminderScheduler.syncUnit(unitId)
                    }
                    previousUnits = currentUnits
                } catch (error: Throwable) {
                    Log.e(LOG_TAG, "Could not reconcile unit-backed Habit reminders", error)
                }
            }
        }
    }

    private suspend fun resumeAfterRestoreMaintenance(backgroundAlreadyRebuilt: Boolean) {
        if (!backgroundAlreadyRebuilt) rebuildBackgroundState()
        initializeNormalRuntime(backgroundAlreadyRebuilt = true)
    }

    private suspend fun quiesceNormalRuntime() {
        normalRuntimeJob?.cancelAndJoin()
        normalRuntimeJob = null
        val workManager = WorkManager.getInstance(this)
        workManager.cancelAllWorkByTag(ALL_WHIP_WORK_TAG).result.get()
        // Also catches a periodic request created by a pre-gate release, when
        // portable work did not yet carry the global tag.
        workManager.cancelUniqueWork(PORTABLE_BACKUP_WORK_NAME).result.get()
        NotificationManagerCompat.from(this).cancelAll()
    }

    private fun createNotificationChannels() {
        ReminderNotifications.createChannel(this)
        RestTimerNotifications.createChannel(this)
        HabitReminderNotifications.createChannel(this)
        GoalReminderNotifications.createChannel(this)
        FocusTimerNotifications.createChannel(this)
    }

    private fun logStartupRecoveryFailure(error: Throwable) {
        Log.e(LOG_TAG, "Startup recovery did not complete; normal runtime remains blocked", error)
    }

    @Synchronized
    private fun advanceUserDataGeneration(): Long {
        val current = currentUserDataGeneration()
        val next = if (current == Long.MAX_VALUE) 1L else current + 1L
        check(recoveryPreferences.edit().putLong(USER_DATA_GENERATION, next).commit()) {
            "Could not persist the restore data generation"
        }
        mutableUserDataGeneration.value = next
        return next
    }

    suspend fun rebuildBackgroundState() {
        areaRepository.ensureDefaultArea()
        val workManager = WorkManager.getInstance(this)
        workManager.cancelAllWorkByTag(ALL_WHIP_WORK_TAG).result.get()
        workManager.cancelUniqueWork(PORTABLE_BACKUP_WORK_NAME).result.get()
        NotificationManagerCompat.from(this).cancelAll()
        automationPromptScheduler.syncAll()
        reminderScheduler.syncAll(allowDuringRecovery = true)
        habitReminderScheduler.syncAll(allowDuringRecovery = true)
        goalReminderScheduler.syncAll(allowDuringRecovery = true)
        settingsRepository.current().let { settings ->
            val deadline = settings.focusTimerDeadlineMillis
            val taskId = settings.focusTimerTaskId
            if (deadline != null && taskId != null && deadline > System.currentTimeMillis()) {
                focusTimerScheduler.schedule(taskId, deadline, allowDuringRecovery = true)
            }
        }
        val sessions = gymRepository.sessions.first()
        val session = sessions.firstOrNull { it.state == WorkoutSessionState.Active }
        val seconds = restTimerScheduleDelaySeconds(
            session?.restTimerDeadlineMillis,
            System.currentTimeMillis(),
        )
        if (session?.restTimerDeadlineMillis != null) {
            restTimerScheduler.schedule(
                sessionId = session.id,
                seconds = seconds ?: 1,
                nextLabel = null,
                timerRevision = session.restTimerRevision,
                expectedDeadlineMillis = session.restTimerDeadlineMillis,
                allowDuringRecovery = true,
            )
        }
        sessions.asSequence()
            .filter { it.restTimerCleanupPending }
            .forEach { gymRepository.acknowledgeRestTimerCleanup(it.id, it.restTimerRevision) }
        portableBackupScheduler.sync(portableBackupManager.state.value, allowDuringRecovery = true)
    }

    private fun prepareReminderDeletion(domain: ReminderDomain, entityIds: Set<Long>) {
        reminderDeletionCleanupStore.prepare(domain, entityIds)
    }

    private fun commitReminderDeletion(domain: ReminderDomain, entityIds: Set<Long>) {
        val notifications = NotificationManagerCompat.from(this)
        entityIds.forEach { id ->
            when (domain) {
                ReminderDomain.Task -> cancelVisibleTaskNotifications(this, id)
                ReminderDomain.Habit -> notifications.cancel(HabitReminderNotifications.notificationId(id))
                ReminderDomain.Goal -> notifications.cancel(GoalReminderNotifications.notificationId(id))
            }
        }
        reminderDeletionCleanupStore.clear(domain, entityIds)
    }

    private fun prepareAreaReminderDeletion(summary: AreaDeletionSummary) {
        prepareReminderDeletion(ReminderDomain.Task, summary.taskIds.toSet())
        prepareReminderDeletion(ReminderDomain.Habit, summary.habitIds.toSet())
        prepareReminderDeletion(ReminderDomain.Goal, summary.goalIds.toSet())
    }

    private fun commitAreaReminderDeletion(summary: AreaDeletionSummary) {
        commitReminderDeletion(ReminderDomain.Task, summary.taskIds.toSet())
        commitReminderDeletion(ReminderDomain.Habit, summary.habitIds.toSet())
        commitReminderDeletion(ReminderDomain.Goal, summary.goalIds.toSet())
    }

    internal suspend fun reconcilePendingReminderDeletions() {
        reminderDeletionCleanupStore.pending().forEach { pending ->
            val entityStillExists = when (pending.domain) {
                ReminderDomain.Task -> database.taskDao().getTask(pending.entityId) != null
                ReminderDomain.Habit -> database.habitDao().getHabit(pending.entityId) != null
                ReminderDomain.Goal -> database.goalDao().getGoal(pending.entityId) != null
            }
            if (!entityStillExists) {
                when (pending.domain) {
                    ReminderDomain.Task -> reminderScheduler.syncTask(
                        pending.entityId,
                        allowDuringRecovery = true,
                    )
                    ReminderDomain.Habit -> habitReminderScheduler.syncHabit(
                        pending.entityId,
                        allowDuringRecovery = true,
                    )
                    ReminderDomain.Goal -> goalReminderScheduler.syncGoal(
                        pending.entityId,
                        allowDuringRecovery = true,
                    )
                }
            }
            reminderDeletionCleanupStore.clear(pending.domain, setOf(pending.entityId))
        }
    }

    private companion object {
        const val LOG_TAG = "WhipStartupRecovery"
        const val RECOVERY_RUNTIME_PREFERENCES = "whip_recovery_runtime"
        const val USER_DATA_GENERATION = "user_data_generation"
    }
}
