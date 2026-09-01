package com.whip.app

import android.app.Application
import android.util.Log
import com.whip.app.core.SettingsWhipClock
import com.whip.app.core.SharedPreferencesSettingsRepository
import com.whip.app.core.UuidWhipIdGenerator
import com.whip.app.data.RoomTaskRepository
import com.whip.app.data.RoomMeasurementRepository
import com.whip.app.data.RoomAreaRepository
import com.whip.app.data.RoomGymRepository
import com.whip.app.data.RoomRoutineRepository
import com.whip.app.data.RoomHabitRepository
import com.whip.app.data.RoomGoalRepository
import com.whip.app.data.RoomLinkRepository
import com.whip.app.data.RoomTrackRepository
import com.whip.app.data.RoomBackupRepository
import com.whip.app.data.TaskDeletionCoordinator
import com.whip.app.data.DomainDeletionCoordinator
import com.whip.app.data.AreaDeletionCoordinator
import com.whip.app.data.PortableBackupManager
import com.whip.app.data.PortableBackupScheduler
import com.whip.app.data.PORTABLE_BACKUP_WORK_NAME
import com.whip.app.data.WhipDatabase
import com.whip.app.data.RestoreRecoveryManager
import com.whip.app.reminders.ReminderNotifications
import com.whip.app.reminders.ReminderScheduler
import com.whip.app.reminders.RestTimerNotifications
import com.whip.app.reminders.RestTimerScheduler
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.core.app.NotificationManagerCompat
import androidx.work.Configuration
import androidx.work.WorkManager
import com.whip.app.reminders.ALL_WHIP_WORK_TAG
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.core.zoneId
import com.whip.app.core.currentDateFlow

@OptIn(FlowPreview::class)
class WhipApplication : Application(), Configuration.Provider {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var startupRecoveryGate: StartupRecoveryGate
    private var normalRuntimeJob: Job? = null
    private val recoveryPreferences by lazy {
        getSharedPreferences(RECOVERY_RUNTIME_PREFERENCES, MODE_PRIVATE)
    }
    private val mutableUserDataGeneration by lazy {
        MutableStateFlow(recoveryPreferences.getLong(USER_DATA_GENERATION, 0L))
    }
    val settingsRepository by lazy { SharedPreferencesSettingsRepository(this) }
    val clock by lazy { SettingsWhipClock(settingsRepository) }
    val idGenerator = UuidWhipIdGenerator
    val database by lazy { WhipDatabase.get(this) }
    val taskRepository by lazy { RoomTaskRepository(database, clock) }
    val measurementRepository by lazy {
        RoomMeasurementRepository(database, clock, idGenerator)
    }
    val areaRepository by lazy { RoomAreaRepository(database, clock, idGenerator) }
    val gymRepository by lazy { RoomGymRepository(database, clock, idGenerator, settingsRepository) }
    val routineRepository by lazy { RoomRoutineRepository(database, clock, idGenerator, settingsRepository) }
    val habitRepository by lazy {
        RoomHabitRepository(database, measurementRepository, clock, idGenerator)
    }
    val goalRepository by lazy {
        RoomGoalRepository(database, measurementRepository, clock, idGenerator)
    }
    val linkRepository by lazy {
        RoomLinkRepository(database, measurementRepository, clock, idGenerator)
    }
    val trackRepository by lazy {
        RoomTrackRepository(database, clock, idGenerator)
    }
    val taskDeletionCoordinator by lazy {
        TaskDeletionCoordinator(database, taskRepository, linkRepository)
    }
    val domainDeletionCoordinator by lazy {
        DomainDeletionCoordinator(database, linkRepository, routineRepository)
    }
    val areaDeletionCoordinator by lazy {
        AreaDeletionCoordinator(database, areaRepository, taskDeletionCoordinator, domainDeletionCoordinator)
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
    val reminderScheduler by lazy { ReminderScheduler(this, settingsRepository) }
    val restTimerScheduler by lazy { RestTimerScheduler(this) }
    val habitReminderScheduler by lazy { HabitReminderScheduler(this, settingsRepository) }
    val goalReminderScheduler by lazy { GoalReminderScheduler(this, settingsRepository) }
    val automationPromptScheduler by lazy { AutomationPromptScheduler(this) }
    val focusTimerScheduler by lazy { FocusTimerScheduler(this) }
    val healthConnectManager by lazy { HealthConnectManager(this, measurementRepository, settingsRepository) }
    val startupRecoveryState: StateFlow<StartupRecoveryState>
        get() = startupRecoveryGate.state
    val userDataGeneration: StateFlow<Long>
        get() = mutableUserDataGeneration
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setMinimumLoggingLevel(Log.INFO).build()

    override fun onCreate() {
        super.onCreate()
        // Channel registration is safe platform setup and must precede any
        // recovery rebuild that can enqueue an immediate notification worker.
        createNotificationChannels()
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
        runBlocking(Dispatchers.IO) {
            startupRecoveryGate.start()?.let(::logStartupRecoveryFailure)
        }
    }

    fun retryStartupRecovery() {
        applicationScope.launch(Dispatchers.IO) {
            startupRecoveryGate.retry()?.let(::logStartupRecoveryFailure)
        }
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

    fun currentUserDataGeneration(): Long = mutableUserDataGeneration.value

    fun isCurrentUserDataGeneration(
        presented: Long,
    ): Boolean = generationMatches(currentUserDataGeneration(), presented)

    suspend fun <T : Any> withUserDataAccess(block: suspend () -> T): T? =
        startupRecoveryGate.withReadyDataAccess(
            additionalCheck = { !restoreRecoveryManager.hasPendingRecovery() },
            block = block,
        )

    fun <T : Any> tryWithUserDataAccessNow(block: () -> T): T? =
        startupRecoveryGate.tryWithReadyDataAccessNow(
            additionalCheck = { !restoreRecoveryManager.hasPendingRecovery() },
            block = block,
        )

    internal suspend fun blockForPendingRecovery() {
        startupRecoveryGate.blockForPendingRecovery(
            hasPendingRecovery = restoreRecoveryManager::hasPendingRecovery,
            prepareForRecovery = ::quiesceNormalRuntime,
        )
    }

    private suspend fun initializeNormalRuntime(backgroundAlreadyRebuilt: Boolean) {
        if (normalRuntimeJob?.isActive == true) return
        val settings = settingsRepository.current()
        if (!backgroundAlreadyRebuilt) {
            areaRepository.ensureDefaultArea()
        }
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
        if (settings.healthConnectEnabled) {
            runtimeScope.launch {
                runCatching { healthConnectManager.sync(settings.healthDataTypes, settings.healthSyncDays) }
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
                areaRepository.areas.map { Unit }, settingsRepository.currentDateFlow(clock).map { Unit },
            ).debounce(250).collectLatest {
                withUserDataAccess { WhipWidgetProvider.updateAll(this@WhipApplication) }
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
        val session = gymRepository.sessions.first().firstOrNull { it.state == WorkoutSessionState.Active }
        val seconds = session?.restTimerDeadlineMillis?.minus(System.currentTimeMillis())?.div(1_000L)?.toInt()
        if (session != null && seconds != null && seconds > 0) {
            restTimerScheduler.schedule(session.id, seconds, null, allowDuringRecovery = true)
        }
        portableBackupScheduler.sync(portableBackupManager.state.value, allowDuringRecovery = true)
    }

    private companion object {
        const val LOG_TAG = "WhipStartupRecovery"
        const val RECOVERY_RUNTIME_PREFERENCES = "whip_recovery_runtime"
        const val USER_DATA_GENERATION = "user_data_generation"
    }
}
