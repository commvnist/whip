package com.whip.app.reminders

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import com.whip.app.WhipApplication
import com.whip.app.core.AppSettings
import com.whip.app.core.SettingsRepository
import com.whip.app.core.adjustForQuietHours
import com.whip.app.core.zoneId
import com.whip.app.data.TaskEntity
import com.whip.app.data.TaskOccurrenceEntity
import com.whip.app.data.WhipDatabase
import com.whip.app.data.toDomain
import com.whip.app.domain.OccurrenceState
import com.whip.app.domain.RecurrenceAnchor
import com.whip.app.domain.RecurrenceEngine
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskOccurrence
import com.whip.app.domain.WhipTask
import com.whip.app.domain.taskReminderInstant
import com.whip.app.startup.USER_DATA_GENERATION_KEY
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

const val ALL_WHIP_WORK_TAG = "whip-background-work"

class ReminderScheduler(context: Context, private val settingsRepository: SettingsRepository? = null) {
    private val appContext = context.applicationContext
    private val app = appContext as WhipApplication
    private val workManager = WorkManager.getInstance(appContext)
    private val dao = WhipDatabase.get(appContext).taskDao()

    suspend fun syncAll(allowDuringRecovery: Boolean = false) {
        withDataAccess(allowDuringRecovery) {
            // Calling WorkManager cancellation once per ordinary task can create
            // thousands of Binder proxies on large databases. Only reminder-capable
            // Tasks need startup reconciliation; normal mutations call syncTask.
            dao.getReminderTaskIds().forEach { taskId ->
                app.reminderDeliveryCoordinator.withEntity(ReminderDomain.Task, taskId) {
                    syncTaskLocked(taskId)
                }
            }
        }
    }

    suspend fun syncTask(taskId: Long, allowDuringRecovery: Boolean = false) {
        withDataAccess(allowDuringRecovery) {
            app.reminderDeliveryCoordinator.withEntity(ReminderDomain.Task, taskId) {
                syncTaskLocked(taskId)
            }
        }
    }

    suspend fun scheduleNext(
        taskId: Long,
        afterMillis: Long,
        allowDuringRecovery: Boolean = false,
    ) {
        withDataAccess(allowDuringRecovery) {
            app.reminderDeliveryCoordinator.withEntity(ReminderDomain.Task, taskId) {
                scheduleNextLocked(taskId, afterMillis)
            }
        }
    }

    suspend fun snooze(
        taskId: Long,
        originalEpochDay: Long?,
        offsetMinutes: Int,
        minutes: Int = 10,
        allowDuringRecovery: Boolean = false,
    ) {
        withDataAccess(allowDuringRecovery) {
            app.reminderDeliveryCoordinator.withEntity(ReminderDomain.Task, taskId) {
                snoozeLocked(taskId, originalEpochDay, offsetMinutes, minutes)
            }
        }
    }

    internal suspend fun syncTaskFromCoordinator(taskId: Long) {
        syncTaskLocked(taskId)
    }

    internal suspend fun scheduleNextFromCoordinator(taskId: Long, afterMillis: Long) {
        scheduleNextLocked(taskId, afterMillis)
    }

    /** A running worker must never cancel its own tag and await that cancellation.
     * Old claims are safe to drain because every worker revalidates fail-closed. */
    internal suspend fun reconcileAfterWorkerFromCoordinator(taskId: Long, afterMillis: Long) {
        cancelVisibleTaskReminder(appContext, taskId)
        scheduleNextLocked(taskId, afterMillis)
    }

    internal suspend fun snoozeFromCoordinator(
        taskId: Long,
        originalEpochDay: Long?,
        offsetMinutes: Int,
        minutes: Int = 10,
    ): Boolean = snoozeLocked(taskId, originalEpochDay, offsetMinutes, minutes)

    private suspend fun withDataAccess(
        allowDuringRecovery: Boolean,
        block: suspend () -> Unit,
    ) {
        if (allowDuringRecovery) block() else app.withUserDataAccess { block() }
    }

    private suspend fun syncTaskLocked(taskId: Long) {
        cancelVisibleTaskNotifications(appContext, taskId)
        workManager.cancelAllWorkByTag(tag(taskId)).await()
        scheduleNextLocked(taskId = taskId, afterMillis = System.currentTimeMillis())
    }

    private suspend fun scheduleNextLocked(taskId: Long, afterMillis: Long) {
        val stored = dao.getTask(taskId) ?: return
        val task = runCatching(stored::toDomain).getOrNull() ?: return
        if (task.archived || !task.reminderEnabled || task.timeMinutes !in 0..1_439) return
        val offsets = parseTaskReminderOffsetsOrNull(stored.reminderOffsetsMinutesCsv) ?: return
        val occurrences = dao.getOccurrences(taskId).toTaskReminderOccurrencesOrNull() ?: return
        val settings = currentSettings()
        offsets.forEach { offsetMinutes ->
            val reminder = runCatching {
                nextTaskReminder(
                    task = task,
                    occurrences = occurrences,
                    afterMillis = afterMillis,
                    offsetMinutes = offsetMinutes,
                    zone = settings.zoneId(),
                    quietStartMinutes = settings.quietStartMinutes,
                    quietEndMinutes = settings.quietEndMinutes,
                )
            }.getOrNull() ?: return@forEach
            val claim = ReminderDeliveryClaim(
                kind = ReminderDeliveryKind.Scheduled,
                stableEntityId = stored.uuid,
                logicalEpochDay = reminder.originalDate.toEpochDay(),
                expectedTriggerAtMillis = reminder.triggerAtMillis,
                definitionFingerprint = taskReminderFingerprint(
                    task = task,
                    stableEntityId = stored.uuid,
                    originalDate = reminder.originalDate,
                    scheduledDate = reminder.scheduledDate,
                    occurrenceState = OccurrenceState.Open,
                    offsetMinutes = offsetMinutes,
                    normalizedOffsets = offsets,
                    settings = settings,
                ),
            )
            val delayMillis = (reminder.triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(0)
            val workName = "${tag(taskId)}-${reminder.originalDate.toEpochDay()}-$offsetMinutes"
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(
                    Data.Builder()
                        .putLong(ReminderWorker.TASK_ID, taskId)
                        .putLong(ReminderWorker.ORIGINAL_EPOCH_DAY, reminder.originalDate.toEpochDay())
                        .putInt(ReminderWorker.OFFSET_MINUTES, offsetMinutes)
                        .putReminderDeliveryClaim(claim)
                        .putLong(USER_DATA_GENERATION_KEY, app.currentUserDataGeneration())
                        .build(),
                )
                .addTag(tag(taskId))
                .addTag(ALL_WHIP_WORK_TAG)
                .build()
            workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, request).await()
        }
    }

    private suspend fun snoozeLocked(
        taskId: Long,
        originalEpochDay: Long?,
        offsetMinutes: Int,
        minutes: Int,
    ): Boolean {
        val originalDate = originalEpochDay?.let { runCatching { LocalDate.ofEpochDay(it) }.getOrNull() }
            ?: return false
        val occurrences = dao.getOccurrences(taskId).toTaskReminderOccurrencesOrNull() ?: return false
        val snapshot = resolveCurrentTaskReminder(
            stored = dao.getTask(taskId) ?: return false,
            occurrences = occurrences,
            originalDate = originalDate,
            offsetMinutes = offsetMinutes,
            settings = currentSettings(),
            requireOpen = true,
        ) ?: return false
        val trigger = System.currentTimeMillis() + minutes.coerceIn(1, 1_440) * 60_000L
        val claim = ReminderDeliveryClaim(
            kind = ReminderDeliveryKind.Snoozed,
            stableEntityId = snapshot.stableEntityId,
            logicalEpochDay = originalDate.toEpochDay(),
            expectedTriggerAtMillis = trigger,
            definitionFingerprint = snapshot.definitionFingerprint,
        )
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay((trigger - System.currentTimeMillis()).coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putLong(ReminderWorker.TASK_ID, taskId)
                    .putLong(ReminderWorker.ORIGINAL_EPOCH_DAY, originalDate.toEpochDay())
                    .putInt(ReminderWorker.OFFSET_MINUTES, offsetMinutes)
                    .putReminderDeliveryClaim(claim)
                    .putLong(USER_DATA_GENERATION_KEY, app.currentUserDataGeneration())
                    .build(),
            )
            .addTag(tag(taskId))
            .addTag(ALL_WHIP_WORK_TAG)
            .build()
        workManager.enqueueUniqueWork("${tag(taskId)}-snooze", ExistingWorkPolicy.REPLACE, request).await()
        cancelVisibleTaskReminder(appContext, taskId)
        return true
    }

    private fun currentSettings(): AppSettings = settingsRepository?.current() ?: app.settingsRepository.current()

    private fun tag(taskId: Long): String = "whip-reminder-$taskId"
}

internal data class CurrentTaskReminder(
    val task: WhipTask,
    val stableEntityId: String,
    val originalDate: LocalDate,
    val scheduledDate: LocalDate,
    val occurrenceState: OccurrenceState,
    val offsetMinutes: Int,
    val expectedScheduledTriggerAtMillis: Long,
    val definitionFingerprint: String,
)

internal fun resolveCurrentTaskReminder(
    stored: TaskEntity,
    occurrences: List<TaskOccurrence>,
    originalDate: LocalDate,
    offsetMinutes: Int,
    settings: AppSettings,
    requireOpen: Boolean,
): CurrentTaskReminder? {
    if (stored.uuid.isBlank()) return null
    val task = runCatching(stored::toDomain).getOrNull() ?: return null
    if (task.archived || task.scheduleKind == ScheduleKind.Anytime || task.timeMinutes !in 0..1_439) return null
    val offsets = parseTaskReminderOffsetsOrNull(stored.reminderOffsetsMinutesCsv) ?: return null
    if (offsetMinutes !in offsets) return null

    val occurrence = occurrences.firstOrNull { it.originalDate == originalDate }
    val current = when (task.scheduleKind) {
        ScheduleKind.Anytime -> false
        ScheduleKind.Once -> originalDate == task.date
        ScheduleKind.Recurring -> task.hasCurrentOccurrence(originalDate, occurrences, settings.zoneId())
    }
    if (!current) return null
    val state = when (task.scheduleKind) {
        ScheduleKind.Recurring -> occurrence?.state ?: OccurrenceState.Open
        else -> if (task.completedAtMillis == null) OccurrenceState.Open else OccurrenceState.Completed
    }
    if (requireOpen && state != OccurrenceState.Open) return null
    val scheduledDate = occurrence?.scheduledDate ?: originalDate
    val expectedTrigger = runCatching {
        adjustForQuietHours(
            taskReminderInstant(
                scheduledDate,
                requireNotNull(task.timeMinutes),
                offsetMinutes,
                settings.zoneId(),
            ),
            settings.zoneId(),
            settings.quietStartMinutes,
            settings.quietEndMinutes,
        ).toEpochMilli()
    }.getOrNull() ?: return null
    return CurrentTaskReminder(
        task = task,
        stableEntityId = stored.uuid,
        originalDate = originalDate,
        scheduledDate = scheduledDate,
        occurrenceState = state,
        offsetMinutes = offsetMinutes,
        expectedScheduledTriggerAtMillis = expectedTrigger,
        definitionFingerprint = taskReminderFingerprint(
            task = task,
            stableEntityId = stored.uuid,
            originalDate = originalDate,
            scheduledDate = scheduledDate,
            occurrenceState = state,
            offsetMinutes = offsetMinutes,
            normalizedOffsets = offsets,
            settings = settings,
        ),
    )
}

internal fun parseTaskReminderOffsetsOrNull(csv: String): List<Int>? {
    if (csv.isBlank()) return listOf(0)
    val parsed = csv.split(',').map { token ->
        val normalized = token.trim()
        if (normalized.isEmpty() || normalized.any { !it.isDigit() }) return null
        normalized.toIntOrNull()?.takeIf { it in 0..43_200 } ?: return null
    }
    return parsed.distinct().sortedDescending().ifEmpty { listOf(0) }
}

internal fun List<TaskOccurrenceEntity>.toTaskReminderOccurrencesOrNull(): List<TaskOccurrence>? {
    val converted = mapNotNull { row -> runCatching(row::toDomain).getOrNull() }
    return converted.takeIf { it.size == size }
}

internal fun taskReminderFingerprint(
    task: WhipTask,
    stableEntityId: String,
    originalDate: LocalDate,
    scheduledDate: LocalDate,
    occurrenceState: OccurrenceState,
    offsetMinutes: Int,
    normalizedOffsets: List<Int>,
    settings: AppSettings,
): String {
    val recurrence = task.recurrence
    return reminderSemanticFingerprint(
        "task-reminder-v1",
        stableEntityId,
        task.scheduleKind.name,
        task.date?.toEpochDay()?.toString().orEmpty(),
        recurrence?.unit?.name.orEmpty(),
        recurrence?.interval?.toString().orEmpty(),
        recurrence?.weekdays?.map { it.value }?.sorted()?.joinToString(",").orEmpty(),
        recurrence?.startDate?.toEpochDay()?.toString().orEmpty(),
        recurrence?.end?.name.orEmpty(),
        recurrence?.endDate?.toEpochDay()?.toString().orEmpty(),
        recurrence?.occurrenceCount?.toString().orEmpty(),
        recurrence?.anchor?.name.orEmpty(),
        task.timeMinutes?.toString().orEmpty(),
        task.reminderEnabled.toString(),
        normalizedOffsets.distinct().sortedDescending().joinToString(","),
        task.deadline?.toEpochDay()?.toString().orEmpty(),
        task.missedOccurrencePolicy.name,
        originalDate.toEpochDay().toString(),
        scheduledDate.toEpochDay().toString(),
        occurrenceState.name,
        offsetMinutes.toString(),
        settings.zoneId().id,
        settings.quietStartMinutes?.toString().orEmpty(),
        settings.quietEndMinutes?.toString().orEmpty(),
    )
}

internal fun nextTaskReminder(
    task: WhipTask,
    occurrences: List<TaskOccurrence>,
    afterMillis: Long,
    offsetMinutes: Int,
    zone: ZoneId,
    quietStartMinutes: Int? = null,
    quietEndMinutes: Int? = null,
): TaskReminderTime? {
    val timeMinutes = task.timeMinutes ?: return null
    if (timeMinutes !in 0..1_439 || offsetMinutes !in 0..43_200) return null
    val afterInstant = Instant.ofEpochMilli(afterMillis)
    val afterDate = afterInstant.atZone(zone).toLocalDate()
    val records = occurrences.associateBy(TaskOccurrence::originalDate)

    val generatedCandidates = when (task.scheduleKind) {
        ScheduleKind.Anytime -> emptyList()
        ScheduleKind.Once -> if (task.completedAtMillis == null) listOfNotNull(task.date) else emptyList()
        ScheduleKind.Recurring -> {
            val rule = task.recurrence ?: return null
            if (rule.anchor == RecurrenceAnchor.Completion) {
                val closed = occurrences.filter { it.state in setOf(OccurrenceState.Completed, OccurrenceState.Skipped) }
                val latest = closed.maxByOrNull { it.completedAtMillis ?: it.scheduledDate.toEpochDay() * 86_400_000L }
                val closedOn = latest?.completedAtMillis?.let {
                    Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
                } ?: latest?.scheduledDate
                listOfNotNull(RecurrenceEngine.nextCompletionRelative(rule, closedOn, closed.size))
            } else {
                RecurrenceEngine.occurrencesBetween(rule, afterDate.minusDays(2), afterDate.plusYears(10))
                    .filter { original ->
                        records[original]?.state !in setOf(OccurrenceState.Completed, OccurrenceState.Skipped)
                    }
            }
        }
    }
    // Explicit rows are authoritative overrides. Include a still-open moved
    // occurrence even when its original recurrence date is outside this window.
    val movedCandidates = if (task.scheduleKind == ScheduleKind.Recurring) {
        occurrences.asSequence()
            .filter { it.state == OccurrenceState.Open }
            .filter { it.scheduledDate != it.originalDate }
            .map(TaskOccurrence::originalDate)
            .toList()
    } else emptyList()
    return (generatedCandidates + movedCandidates).distinct().asSequence()
        .map { original ->
            val scheduled = records[original]?.scheduledDate ?: original
            val raw = taskReminderInstant(scheduled, timeMinutes, offsetMinutes, zone)
            TaskReminderTime(
                originalDate = original,
                scheduledDate = scheduled,
                triggerAtMillis = adjustForQuietHours(raw, zone, quietStartMinutes, quietEndMinutes).toEpochMilli(),
            )
        }
        .filter { it.triggerAtMillis >= afterMillis }
        .minByOrNull(TaskReminderTime::triggerAtMillis)
}

internal fun WhipTask.hasCurrentOccurrence(
    originalDate: LocalDate,
    occurrences: List<TaskOccurrence>,
    zoneId: ZoneId,
): Boolean {
    if (occurrences.any { it.originalDate == originalDate }) return true
    val rule = recurrence ?: return false
    if (rule.anchor == RecurrenceAnchor.Schedule) {
        return originalDate in RecurrenceEngine.occurrencesBetween(rule, originalDate, originalDate)
    }
    val closed = occurrences.filter { it.state in setOf(OccurrenceState.Completed, OccurrenceState.Skipped) }
    val latest = closed.maxByOrNull {
        it.completedAtMillis ?: it.scheduledDate.toEpochDay() * 86_400_000L
    }
    val closedOn = latest?.completedAtMillis?.let {
        Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate()
    } ?: latest?.scheduledDate
    return RecurrenceEngine.nextCompletionRelative(rule, closedOn, closed.size) == originalDate
}

internal fun cancelVisibleTaskReminder(context: Context, taskId: Long) {
    NotificationManagerCompat.from(context).cancel(taskId.hashCode())
}

internal fun cancelVisibleTaskNotifications(context: Context, taskId: Long) {
    NotificationManagerCompat.from(context).apply {
        cancel(taskId.hashCode())
        cancel(ReminderNotifications.completionUndoNotificationId(taskId))
    }
}

internal data class TaskReminderTime(
    val originalDate: LocalDate,
    val scheduledDate: LocalDate,
    val triggerAtMillis: Long,
)
