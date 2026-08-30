package com.whip.app

import android.app.Application
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
import com.whip.app.widget.WhipWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.work.WorkManager
import com.whip.app.reminders.ALL_WHIP_WORK_TAG
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.core.zoneId
import com.whip.app.core.currentDateFlow

@OptIn(FlowPreview::class)
class WhipApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
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
    val restoreRecoveryManager by lazy { RestoreRecoveryManager(this, backupRepository) }
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

    override fun onCreate() {
        super.onCreate()
        ReminderNotifications.createChannel(this)
        RestTimerNotifications.createChannel(this)
        HabitReminderNotifications.createChannel(this)
        GoalReminderNotifications.createChannel(this)
        FocusTimerNotifications.createChannel(this)
        // Recovery is rare and must complete before UI flows observe a mixed
        // database/settings state left by an interrupted restore.
        runBlocking(Dispatchers.IO) {
            runCatching { restoreRecoveryManager.recoverIfNeeded(::rebuildBackgroundState) }
            areaRepository.ensureDefaultArea()
        }
        portableBackupScheduler.sync(portableBackupManager.state.value)
        applicationScope.launch { runCatching { portableBackupManager.recoverInterruptedWrites() } }
        applicationScope.launch { runCatching { automationPromptScheduler.syncAll() } }
        if (settingsRepository.current().healthConnectEnabled) {
            applicationScope.launch {
                val settings = settingsRepository.current()
                runCatching { healthConnectManager.sync(settings.healthDataTypes, settings.healthSyncDays) }
            }
        }
        settingsRepository.current().let { settings ->
            val deadline = settings.focusTimerDeadlineMillis
            val taskId = settings.focusTimerTaskId
            if (deadline != null && taskId != null && deadline > System.currentTimeMillis()) {
                focusTimerScheduler.schedule(taskId, deadline)
            }
        }
        applicationScope.launch {
            merge(
                taskRepository.tasks.map { Unit }, taskRepository.occurrences.map { Unit },
                taskRepository.steps.map { Unit }, taskRepository.stepStates.map { Unit },
                taskRepository.stepSnapshots.map { Unit },
                habitRepository.habits.map { Unit }, habitRepository.logs.map { Unit },
                habitRepository.checklistItems.map { Unit }, habitRepository.checklistStates.map { Unit },
                habitRepository.pauses.map { Unit }, habitRepository.skips.map { Unit },
                measurementRepository.entries.map { Unit }, measurementRepository.customUnits.map { Unit },
                areaRepository.areas.map { Unit }, settingsRepository.currentDateFlow(clock).map { Unit },
            ).debounce(250).collectLatest { WhipWidgetProvider.updateAll(this@WhipApplication) }
        }
    }

    suspend fun rebuildBackgroundState() {
        areaRepository.ensureDefaultArea()
        WorkManager.getInstance(this).cancelAllWorkByTag(ALL_WHIP_WORK_TAG).result.get()
        automationPromptScheduler.syncAll()
        reminderScheduler.syncAll()
        habitReminderScheduler.syncAll()
        goalReminderScheduler.syncAll()
        settingsRepository.current().let { settings ->
            val deadline = settings.focusTimerDeadlineMillis
            val taskId = settings.focusTimerTaskId
            if (deadline != null && taskId != null && deadline > System.currentTimeMillis()) focusTimerScheduler.schedule(taskId, deadline)
        }
        val session = gymRepository.sessions.first().firstOrNull { it.state == WorkoutSessionState.Active }
        val seconds = session?.restTimerDeadlineMillis?.minus(System.currentTimeMillis())?.div(1_000L)?.toInt()
        if (session != null && seconds != null && seconds > 0) restTimerScheduler.schedule(session.id, seconds, null)
    }
}
