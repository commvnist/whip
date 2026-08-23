package com.whip.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.whip.app.WhipApplication
import java.time.LocalDate
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
        val actionId = "task:$taskId:${intent.action}:$epochDay:${intent.getLongExtra(EXTRA_ACTION_TOKEN, 0L)}"
        val ledger = NotificationActionLedger(context)
        if (!ledger.begin(actionId)) {
            pending.finish()
            return
        }
        if (intent.action == ACTION_SNOOZE) {
            runCatching {
                app.reminderScheduler.snooze(taskId, epochDay.takeUnless { it == Long.MIN_VALUE })
                NotificationManagerCompat.from(context).cancel(taskId.hashCode())
            }.onSuccess { ledger.complete(actionId) }.onFailure { ledger.release(actionId) }
            pending.finish()
            return
        }
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val original = epochDay.takeUnless { it == Long.MIN_VALUE }?.let(LocalDate::ofEpochDay)
                if (intent.action == ACTION_COMPLETE) {
                    app.taskRepository.completeOccurrence(taskId, original)
                } else {
                    val task = app.taskRepository.getTask(taskId) ?: error("Task no longer exists")
                    if (task.scheduleKind == com.whip.app.domain.ScheduleKind.Recurring && original != null) {
                        val occurrence = app.taskRepository.getOccurrences(taskId).firstOrNull { it.originalDate == original }
                        if (occurrence != null) {
                            app.taskRepository.reopenOccurrence(
                                com.whip.app.domain.ScheduledTask(task, original, occurrence.scheduledDate),
                            )
                        }
                    } else {
                        app.taskRepository.reopen(taskId)
                    }
                }
                app.reminderScheduler.syncTask(taskId)
                NotificationManagerCompat.from(context).cancel(taskId.hashCode())
                if (intent.action == ACTION_COMPLETE) {
                    app.taskRepository.getTask(taskId)?.let { ReminderNotifications.showCompletionUndo(context, it, epochDay.takeUnless { day -> day == Long.MIN_VALUE }) }
                } else {
                    NotificationManagerCompat.from(context).cancel(ReminderNotifications.completionUndoNotificationId(taskId))
                }
                ledger.complete(actionId)
            } catch (_: Throwable) {
                ledger.release(actionId)
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
