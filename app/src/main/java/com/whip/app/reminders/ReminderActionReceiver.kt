package com.whip.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.room.withTransaction
import com.whip.app.WhipApplication
import com.whip.app.data.toDomain
import com.whip.app.domain.ANYTIME_TASK_OCCURRENCE_KEY
import com.whip.app.domain.OccurrenceState
import com.whip.app.domain.RecurrenceAnchor
import com.whip.app.domain.RecurrenceEngine
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.TaskOccurrence
import com.whip.app.domain.TaskStep
import com.whip.app.startup.MISSING_USER_DATA_GENERATION
import com.whip.app.startup.USER_DATA_GENERATION_KEY
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(ACTION_COMPLETE, ACTION_SNOOZE, ACTION_UNDO)) return
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId < 0) return
        val epochDay = intent.getLongExtra(EXTRA_ORIGINAL_EPOCH_DAY, Long.MIN_VALUE)
        val pending = goAsync()
        val app = context.applicationContext as WhipApplication
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val accessed = app.withUserDataAccess {
                    app.handleTaskNotificationAction(context, intent, taskId, epochDay)
                }
                if (accessed == null) cancelTaskNotification(context, taskId)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE = "commvne.com.whip.app.action.COMPLETE_TASK"
        const val ACTION_SNOOZE = "commvne.com.whip.app.action.SNOOZE_TASK"
        const val ACTION_UNDO = "commvne.com.whip.app.action.UNDO_COMPLETE_TASK"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_ORIGINAL_EPOCH_DAY = "original_epoch_day"
        const val EXTRA_ACTION_TOKEN = "action_token"
    }
}

private suspend fun WhipApplication.handleTaskNotificationAction(
    context: Context,
    intent: Intent,
    taskId: Long,
    epochDay: Long,
) {
    if (!isCurrentUserDataGeneration(intent.getLongExtra(USER_DATA_GENERATION_KEY, MISSING_USER_DATA_GENERATION))) {
        cancelTaskNotification(context, taskId)
        return
    }
    val action = TaskNotificationAction.fromIntentAction(intent.action)
    val original = epochDay.takeUnless { it == Long.MIN_VALUE }?.let(LocalDate::ofEpochDay)
    val actionId = "task:$taskId:${intent.action}:$epochDay:${intent.getLongExtra(ReminderActionReceiver.EXTRA_ACTION_TOKEN, 0L)}"
    val ledger = NotificationActionLedger(context)
    try {
        if (action == null || currentTaskNotificationTarget(taskId, original, action) == null) {
            refreshStaleTaskNotification(context, taskId)
            return
        }
        if (!ledger.begin(actionId)) return

        val applied = if (action == TaskNotificationAction.Snooze) {
            if (currentTaskNotificationTarget(taskId, original, action) == null) false
            else {
                reminderScheduler.snooze(
                    taskId,
                    original?.toEpochDay(),
                    allowDuringRecovery = true,
                )
                true
            }
        } else {
            database.withTransaction {
                val target = currentTaskNotificationTarget(taskId, original, action)
                    ?: return@withTransaction false
                when (action) {
                    TaskNotificationAction.Complete -> taskRepository.completeOccurrence(taskId, original)
                    TaskNotificationAction.Undo -> if (target.task.scheduleKind == ScheduleKind.Recurring) {
                        taskRepository.reopenOccurrence(target)
                    } else {
                        taskRepository.reopen(taskId)
                    }
                    TaskNotificationAction.Snooze -> error("Snooze is handled without a database mutation")
                }
                true
            }
        }
        if (!applied) {
            ledger.release(actionId)
            refreshStaleTaskNotification(context, taskId)
            return
        }

        if (action != TaskNotificationAction.Snooze) {
            reminderScheduler.syncTask(taskId, allowDuringRecovery = true)
        }
        NotificationManagerCompat.from(context).cancel(taskId.hashCode())
        if (action == TaskNotificationAction.Complete) {
            taskRepository.getTask(taskId)?.let {
                ReminderNotifications.showCompletionUndo(context, it, original?.toEpochDay())
            }
        } else if (action == TaskNotificationAction.Undo) {
            NotificationManagerCompat.from(context)
                .cancel(ReminderNotifications.completionUndoNotificationId(taskId))
        }
        ledger.complete(actionId)
    } catch (_: Throwable) {
        ledger.release(actionId)
    }
}

private fun cancelTaskNotification(context: Context, taskId: Long) {
    NotificationManagerCompat.from(context).apply {
        cancel(taskId.hashCode())
        cancel(ReminderNotifications.completionUndoNotificationId(taskId))
    }
}

internal enum class TaskNotificationAction {
    Complete,
    Snooze,
    Undo;

    companion object {
        fun fromIntentAction(action: String?): TaskNotificationAction? = when (action) {
            ReminderActionReceiver.ACTION_COMPLETE -> Complete
            ReminderActionReceiver.ACTION_SNOOZE -> Snooze
            ReminderActionReceiver.ACTION_UNDO -> Undo
            else -> null
        }
    }
}

/** Resolves an action against the current task definition and exact occurrence.
 * Notification payloads are hints, never authority: edits, archival, completion,
 * recurrence changes, and newly-added subtasks can all make an old action stale. */
internal suspend fun WhipApplication.currentTaskNotificationTarget(
    taskId: Long,
    originalDate: LocalDate?,
    action: TaskNotificationAction,
): ScheduledTask? {
    val dao = database.taskDao()
    val stored = dao.getTask(taskId) ?: return null
    val steps = dao.getSteps(taskId).map { it.toDomain() }
    val task = stored.toDomain().copy(steps = steps)
    if (task.archived) return null
    if (
        action == TaskNotificationAction.Snooze &&
        (!task.reminderEnabled || task.timeMinutes == null)
    ) return null

    val occurrences = dao.getOccurrences(taskId).map { it.toDomain() }
    val occurrence = originalDate?.let { date -> occurrences.firstOrNull { it.originalDate == date } }
    val targetIsCurrent = when (task.scheduleKind) {
        ScheduleKind.Anytime -> originalDate == null
        ScheduleKind.Once -> originalDate != null && originalDate == task.date
        ScheduleKind.Recurring -> originalDate != null && task.hasCurrentOccurrence(
            originalDate,
            occurrences,
            clock.zoneId(),
        )
    }
    if (!targetIsCurrent) return null

    val isClosed = when (task.scheduleKind) {
        ScheduleKind.Recurring -> occurrence?.state in setOf(OccurrenceState.Completed, OccurrenceState.Skipped)
        else -> task.completedAtMillis != null
    }
    if (action == TaskNotificationAction.Undo) {
        val isCompleted = when (task.scheduleKind) {
            ScheduleKind.Recurring -> occurrence?.state == OccurrenceState.Completed
            else -> task.completedAtMillis != null
        }
        if (!isCompleted) return null
    } else if (isClosed) {
        return null
    }

    val occurrenceKey = originalDate?.toEpochDay()
        ?: task.date?.toEpochDay()
        ?: ANYTIME_TASK_OCCURRENCE_KEY
    val states = dao.getStepStates(taskId, occurrenceKey).associateBy { it.stepId }
    if (
        action == TaskNotificationAction.Complete &&
        steps.filterNot(TaskStep::archived).any { step -> states[step.id]?.completed != true }
    ) return null

    return ScheduledTask(
        task = task,
        originalDate = originalDate,
        scheduledDate = occurrence?.scheduledDate ?: originalDate ?: task.date,
    )
}

private fun com.whip.app.domain.WhipTask.hasCurrentOccurrence(
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

private suspend fun WhipApplication.refreshStaleTaskNotification(context: Context, taskId: Long) {
    cancelTaskNotification(context, taskId)
    reminderScheduler.syncTask(taskId, allowDuringRecovery = true)
}
