package com.whip.app.reminders

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.whip.app.data.WhipDatabase
import com.whip.app.core.SettingsRepository
import com.whip.app.core.adjustForQuietHours
import com.whip.app.core.zoneId
import com.whip.app.data.toDomain
import com.whip.app.domain.OccurrenceState
import com.whip.app.domain.RecurrenceEngine
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskOccurrence
import com.whip.app.domain.WhipTask
import com.whip.app.domain.RecurrenceAnchor
import com.whip.app.domain.taskReminderInstant
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

const val ALL_WHIP_WORK_TAG = "whip-background-work"

class ReminderScheduler(context: Context, private val settingsRepository: SettingsRepository? = null) {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)
    private val dao = WhipDatabase.get(appContext).taskDao()

    suspend fun syncAll() {
        // Calling WorkManager cancellation once per ordinary task can create
        // thousands of Binder proxies on large databases. Only tasks capable
        // of producing a reminder need daily/startup rescheduling; normal edit,
        // archive, and delete paths already call syncTask to clear stale work.
        dao.getReminderTaskIds().forEach { taskId -> syncTask(taskId) }
    }

    suspend fun syncTask(taskId: Long) {
        workManager.cancelAllWorkByTag(tag(taskId))
        scheduleNext(taskId = taskId, afterMillis = System.currentTimeMillis())
    }

    suspend fun scheduleNext(taskId: Long, afterMillis: Long) {
        val task = dao.getTask(taskId)?.toDomain() ?: return
        if (task.archived || !task.reminderEnabled || task.timeMinutes == null) return

        val occurrences = dao.getOccurrences(taskId).map { it.toDomain() }
        task.reminderOffsetsMinutes.ifEmpty { listOf(0) }.distinct().forEach { offsetMinutes ->
            val reminder = nextReminder(task, occurrences, afterMillis, offsetMinutes) ?: return@forEach
            val delayMillis = (reminder.triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(0)
            val workName = "${tag(taskId)}-${reminder.originalDate.toEpochDay()}-$offsetMinutes"
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(
                    Data.Builder()
                        .putLong(ReminderWorker.TASK_ID, taskId)
                        .putLong(ReminderWorker.ORIGINAL_EPOCH_DAY, reminder.originalDate.toEpochDay())
                        .putInt(ReminderWorker.OFFSET_MINUTES, offsetMinutes)
                        .build(),
                )
                .addTag(tag(taskId))
                .addTag(ALL_WHIP_WORK_TAG)
                .build()
            workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, request)
        }
    }

    fun snooze(taskId: Long, originalEpochDay: Long?, minutes: Int = 10) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(minutes.coerceIn(1, 1_440).toLong(), TimeUnit.MINUTES)
            .setInputData(
                Data.Builder()
                    .putLong(ReminderWorker.TASK_ID, taskId)
                    .putLong(ReminderWorker.ORIGINAL_EPOCH_DAY, originalEpochDay ?: Long.MIN_VALUE)
                    .putInt(ReminderWorker.OFFSET_MINUTES, 0)
                    .build(),
            )
            .addTag(tag(taskId))
            .addTag(ALL_WHIP_WORK_TAG)
            .build()
        workManager.enqueueUniqueWork("${tag(taskId)}-snooze", ExistingWorkPolicy.REPLACE, request)
    }

    private fun nextReminder(
        task: WhipTask,
        occurrences: List<TaskOccurrence>,
        afterMillis: Long,
        offsetMinutes: Int,
    ): TaskReminderTime? {
        val settings = settingsRepository?.current()
        val zone = settings?.zoneId() ?: ZoneId.systemDefault()
        return nextTaskReminder(
            task = task,
            occurrences = occurrences,
            afterMillis = afterMillis,
            offsetMinutes = offsetMinutes,
            zone = zone,
            quietStartMinutes = settings?.quietStartMinutes,
            quietEndMinutes = settings?.quietEndMinutes,
        )
    }

    private fun tag(taskId: Long): String = "whip-reminder-$taskId"
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
        val afterInstant = Instant.ofEpochMilli(afterMillis)
        val afterDate = afterInstant.atZone(zone).toLocalDate()
        val records = occurrences.associateBy(TaskOccurrence::originalDate)

        val generatedCandidates = when (task.scheduleKind) {
            ScheduleKind.Anytime -> emptyList()
            ScheduleKind.Once -> {
                if (task.completedAtMillis == null) listOfNotNull(task.date) else emptyList()
            }
            ScheduleKind.Recurring -> {
                val rule = requireNotNull(task.recurrence)
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
                            records[original]?.state !in setOf(
                                OccurrenceState.Completed,
                                OccurrenceState.Skipped,
                            )
                        }
                }
            }
        }
        // Explicit occurrence rows are authoritative overrides. Include every
        // still-open moved occurrence even when its original recurrence date
        // is far outside the generation window around `afterDate`.
        val movedCandidates = if (task.scheduleKind == ScheduleKind.Recurring) {
            occurrences.asSequence()
                .filter { it.state !in setOf(OccurrenceState.Completed, OccurrenceState.Skipped) }
                .filter { it.scheduledDate != it.originalDate }
                .map(TaskOccurrence::originalDate)
                .toList()
        } else emptyList()
        val candidates = (generatedCandidates + movedCandidates).distinct()

        return candidates.asSequence()
            .map { original ->
                val scheduled = records[original]?.scheduledDate ?: original
                val raw = taskReminderInstant(scheduled, timeMinutes, offsetMinutes, zone)
                TaskReminderTime(
                    originalDate = original,
                    triggerAtMillis = adjustForQuietHours(
                        raw,
                        zone,
                        quietStartMinutes,
                        quietEndMinutes,
                    ).toEpochMilli(),
                )
            }
            .filter { it.triggerAtMillis >= afterMillis }
            .minByOrNull(TaskReminderTime::triggerAtMillis)
}

internal data class TaskReminderTime(
    val originalDate: LocalDate,
    val triggerAtMillis: Long,
)
