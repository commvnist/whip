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
import com.whip.app.reminders.AutomationPromptNotifications
import com.whip.app.reminders.AutomationPromptScheduler
import com.whip.app.reminders.FocusTimerNotifications
import com.whip.app.reminders.FocusTimerScheduler
import com.whip.app.health.HealthConnectManager
import com.whip.app.widget.WhipWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.work.WorkManager
import com.whip.app.reminders.ALL_WHIP_WORK_TAG
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.core.zoneId
import com.whip.app.domain.LinkSourceType

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
        AutomationPromptNotifications.createChannel(this)
        FocusTimerNotifications.createChannel(this)
        // Recovery is rare and must complete before UI flows observe a mixed
        // database/settings state left by an interrupted restore.
        runBlocking(Dispatchers.IO) {
            runCatching { restoreRecoveryManager.recoverIfNeeded(::rebuildBackgroundState) }
            areaRepository.ensureDefaultArea()
        }
        portableBackupScheduler.sync(portableBackupManager.state.value)
        applicationScope.launch { runCatching { portableBackupManager.recoverInterruptedWrites() } }
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
                taskRepository.occurrences.map { Unit },
                taskRepository.stepStates.map { Unit },
            ).debounce(150).collectLatest {
                runCatching { linkRepository.rebuildSources(setOf(LinkSourceType.Task, LinkSourceType.Subtask)) }
                runCatching { automationPromptScheduler.syncAll() }
            }
        }
        applicationScope.launch {
            habitRepository.logs.map { Unit }.debounce(150).collectLatest {
                runCatching { linkRepository.rebuildSources(setOf(LinkSourceType.Habit)) }
                runCatching { automationPromptScheduler.syncAll() }
            }
        }
        applicationScope.launch {
            merge(
                gymRepository.sessions.map { Unit }, gymRepository.workoutExercises.map { Unit },
                gymRepository.sets.map { Unit }, gymRepository.exercises.map { Unit },
            ).debounce(150).collectLatest {
                runCatching { linkRepository.rebuildSources(setOf(LinkSourceType.Workout, LinkSourceType.Exercise)) }
                runCatching { automationPromptScheduler.syncAll() }
            }
        }
        applicationScope.launch {
            measurementRepository.entries.map { entries ->
                entries.filterNot { it.note.startsWith("Linked:") || it.note.startsWith("Automatically logged by") }
                    .map { it.id to it.updatedAtMillis }
            }.distinctUntilChanged().debounce(150).collectLatest {
                runCatching { linkRepository.rebuildSources(setOf(LinkSourceType.Metric)) }
            }
        }
        applicationScope.launch {
            merge(
                trackRepository.tracks.map { Unit },
                trackRepository.fields.map { Unit },
                trackRepository.options.map { Unit },
                trackRepository.entries.map { Unit },
                trackRepository.values.map { Unit },
            ).debounce(150).collectLatest {
                runCatching { linkRepository.rebuildSources(setOf(LinkSourceType.Track)) }
                runCatching { automationPromptScheduler.syncAll() }
            }
        }
        applicationScope.launch {
            merge(
                linkRepository.triggerRules.map { Unit },
                linkRepository.triggerOccurrences.map { Unit },
            ).debounce(150).collectLatest {
                runCatching { automationPromptScheduler.syncAll() }
            }
        }
        applicationScope.launch {
            merge(
                taskRepository.tasks.map { Unit }, taskRepository.occurrences.map { Unit },
                habitRepository.logs.map { Unit }, gymRepository.sessions.map { Unit },
                gymRepository.sets.map { Unit }, linkRepository.rules.map { Unit },
                linkRepository.triggerRules.map { Unit },
                trackRepository.tracks.map { Unit }, trackRepository.entries.map { Unit },
            ).debounce(250).collectLatest { WhipWidgetProvider.updateAll(this@WhipApplication) }
        }
    }

    suspend fun rebuildBackgroundState() {
        areaRepository.ensureDefaultArea()
        WorkManager.getInstance(this).cancelAllWorkByTag(ALL_WHIP_WORK_TAG).result.get()
        linkRepository.rebuildAll()
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
