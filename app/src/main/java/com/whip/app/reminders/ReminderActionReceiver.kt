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
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.visibleTaskStepsForOccurrence
import com.whip.app.startup.MISSING_USER_DATA_GENERATION
import com.whip.app.startup.USER_DATA_GENERATION_KEY
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(ACTION_COMPLETE, ACTION_SNOOZE, ACTION_UNDO)) return
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId < 0L) return
        val epochDay = intent.getLongExtra(EXTRA_ORIGINAL_EPOCH_DAY, Long.MIN_VALUE)
        val pending = goAsync()
        val app = context.applicationContext as WhipApplication
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val accessed = app.withUserDataAccess {
                    app.reminderDeliveryCoordinator.withEntity(ReminderDomain.Task, taskId) {
                        app.reminderDeliveryCoordinator.withStateBoundary {
                            app.handleTaskNotificationAction(context, intent, taskId, epochDay)
                        }
                    }
                }
                if (accessed == null) cancelVisibleTaskNotifications(context, taskId)
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
        const val EXTRA_OFFSET_MINUTES = "offset_minutes"
        const val EXTRA_STABLE_ENTITY_ID = "stable_entity_id"
        const val EXTRA_DEFINITION_FINGERPRINT = "definition_fingerprint"
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
        cancelVisibleTaskNotifications(context, taskId)
        return
    }
    val action = TaskNotificationAction.fromIntentAction(intent.action)
    val original = epochDay.takeUnless { it == Long.MIN_VALUE }
        ?.let { runCatching { LocalDate.ofEpochDay(it) }.getOrNull() }
    val offsetMinutes = intent.getIntExtra(ReminderActionReceiver.EXTRA_OFFSET_MINUTES, Int.MIN_VALUE)
    val stableEntityId = intent.getStringExtra(ReminderActionReceiver.EXTRA_STABLE_ENTITY_ID).orEmpty()
    val definitionFingerprint = intent
        .getStringExtra(ReminderActionReceiver.EXTRA_DEFINITION_FINGERPRINT)
        .orEmpty()
    val actionId = listOf(
        "task",
        taskId,
        stableEntityId,
        intent.action,
        epochDay,
        offsetMinutes,
        definitionFingerprint,
        intent.getLongExtra(ReminderActionReceiver.EXTRA_ACTION_TOKEN, 0L),
    ).joinToString(":")
    val scheduler = ReminderScheduler(context, settingsRepository)
    val ledger = NotificationActionLedger(context)
    try {
        val current = if (
            action == null ||
            original == null ||
            offsetMinutes == Int.MIN_VALUE ||
            stableEntityId.isBlank() ||
            definitionFingerprint.isBlank()
        ) null else currentTaskNotificationTarget(
            taskId = taskId,
            originalDate = original,
            action = action,
            offsetMinutes = offsetMinutes,
            expectedStableEntityId = stableEntityId,
            expectedDefinitionFingerprint = definitionFingerprint,
        )
        if (current == null) {
            refreshStaleTaskNotification(context, taskId, scheduler)
            return
        }
        val validatedAction = requireNotNull(action)
        if (!ledger.begin(actionId)) return

        val applied = executeClaimedNotificationAction(
            applyAction = {
                if (validatedAction == TaskNotificationAction.Snooze) {
                    if (currentTaskNotificationTarget(
                            taskId,
                            original,
                            validatedAction,
                            offsetMinutes,
                            stableEntityId,
                            definitionFingerprint,
                        ) == null
                    ) false else scheduler.snoozeFromCoordinator(
                        taskId = taskId,
                        originalEpochDay = epochDay,
                        offsetMinutes = offsetMinutes,
                    )
                } else {
                    database.withTransaction {
                        val target = currentTaskNotificationTarget(
                            taskId,
                            original,
                            validatedAction,
                            offsetMinutes,
                            stableEntityId,
                            definitionFingerprint,
                        ) ?: return@withTransaction false
                        when (validatedAction) {
                            TaskNotificationAction.Complete -> rawTaskRepository.completeOccurrence(taskId, original)
                            TaskNotificationAction.Undo -> if (target.task.scheduleKind == ScheduleKind.Recurring) {
                                rawTaskRepository.reopenOccurrence(target.scheduledTask)
                            } else {
                                rawTaskRepository.reopen(taskId)
                            }
                            TaskNotificationAction.Snooze -> error("Snooze is handled without a database mutation")
                        }
                        true
                    }
                }
            },
            markCommitted = { ledger.complete(actionId) },
            releaseClaim = { ledger.release(actionId) },
            followUp = {
                if (validatedAction != TaskNotificationAction.Snooze) scheduler.syncTaskFromCoordinator(taskId)
                if (validatedAction == TaskNotificationAction.Complete) {
                    currentTaskNotificationTarget(
                        taskId = taskId,
                        originalDate = original,
                        action = TaskNotificationAction.Undo,
                        offsetMinutes = offsetMinutes,
                    )?.let { undoTarget ->
                        ReminderNotifications.showCompletionUndo(
                            context = context,
                            task = undoTarget.task,
                            originalEpochDay = epochDay,
                            offsetMinutes = offsetMinutes,
                            stableEntityId = undoTarget.stableEntityId,
                            definitionFingerprint = undoTarget.definitionFingerprint,
                        )
                    }
                } else if (validatedAction == TaskNotificationAction.Undo) {
                    NotificationManagerCompat.from(context)
                        .cancel(ReminderNotifications.completionUndoNotificationId(taskId))
                }
            },
        )
        if (!applied) refreshStaleTaskNotification(context, taskId, scheduler)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        // Invalid or transient pre-claim failures leave the current notification available.
    }
}

internal suspend fun executeClaimedNotificationAction(
    applyAction: suspend () -> Boolean,
    markCommitted: () -> Boolean,
    releaseClaim: () -> Unit,
    followUp: suspend () -> Unit,
): Boolean {
    var authoritativeApplied = false
    return try {
        if (!applyAction()) {
            releaseClaim()
            false
        } else {
            authoritativeApplied = true
            markCommitted()
            try {
                followUp()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // The authoritative action is committed. A later sync will reconcile presentation.
            }
            true
        }
    } catch (cancelled: CancellationException) {
        if (!authoritativeApplied) releaseClaim()
        throw cancelled
    } catch (failure: Exception) {
        if (!authoritativeApplied) releaseClaim()
        authoritativeApplied
    } catch (fatal: Error) {
        if (!authoritativeApplied) releaseClaim()
        throw fatal
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

internal data class CurrentTaskNotificationTarget(
    val scheduledTask: ScheduledTask,
    val stableEntityId: String,
    val definitionFingerprint: String,
) {
    val task get() = scheduledTask.task
}

/** Resolves an action against the current Task definition and exact occurrence.
 * Notification payloads are hints, never authority: edits, archival, completion,
 * recurrence changes, moved occurrences, and newly unfinished subtasks can make
 * an old action stale. */
internal suspend fun WhipApplication.currentTaskNotificationTarget(
    taskId: Long,
    originalDate: LocalDate?,
    action: TaskNotificationAction,
    offsetMinutes: Int = 0,
    expectedStableEntityId: String? = null,
    expectedDefinitionFingerprint: String? = null,
): CurrentTaskNotificationTarget? {
    val original = originalDate ?: return null
    val dao = database.taskDao()
    val stored = dao.getTask(taskId) ?: return null
    val occurrences = dao.getOccurrences(taskId).toTaskReminderOccurrencesOrNull() ?: return null
    val snapshot = resolveCurrentTaskReminder(
        stored = stored,
        occurrences = occurrences,
        originalDate = original,
        offsetMinutes = offsetMinutes,
        settings = settingsRepository.current(),
        requireOpen = action != TaskNotificationAction.Undo,
    ) ?: return null
    if (expectedStableEntityId != null && expectedStableEntityId != snapshot.stableEntityId) return null
    if (
        expectedDefinitionFingerprint != null &&
        expectedDefinitionFingerprint != snapshot.definitionFingerprint
    ) return null
    if (
        action in setOf(TaskNotificationAction.Complete, TaskNotificationAction.Snooze) &&
        !snapshot.task.reminderEnabled
    ) return null
    if (action == TaskNotificationAction.Undo && snapshot.occurrenceState != OccurrenceState.Completed) return null

    val steps = dao.getSteps(taskId).map { it.toDomain() }
    val occurrenceKey = original.toEpochDay()
        .takeUnless { snapshot.task.scheduleKind == ScheduleKind.Anytime }
        ?: snapshot.task.date?.toEpochDay()
        ?: ANYTIME_TASK_OCCURRENCE_KEY
    val states = dao.getStepStates(taskId, occurrenceKey).associateBy { it.stepId }
    val visibleSteps = visibleTaskStepsForOccurrence(
        steps = steps,
        snapshots = dao.getStepSnapshotsForTask(taskId).map { it.toDomain() },
        occurrenceKey = occurrenceKey,
        policy = snapshot.task.repeatStepPolicy,
    )
    if (
        action == TaskNotificationAction.Complete &&
        visibleSteps.any { step -> states[step.id]?.completed != true }
    ) return null

    return CurrentTaskNotificationTarget(
        scheduledTask = ScheduledTask(
            task = snapshot.task.copy(steps = steps),
            originalDate = original,
            scheduledDate = snapshot.scheduledDate,
            completedAtMillis = occurrences.firstOrNull { it.originalDate == original }?.completedAtMillis
                ?: snapshot.task.completedAtMillis,
            occurrenceState = snapshot.occurrenceState,
        ),
        stableEntityId = snapshot.stableEntityId,
        definitionFingerprint = snapshot.definitionFingerprint,
    )
}

private suspend fun WhipApplication.refreshStaleTaskNotification(
    context: Context,
    taskId: Long,
    scheduler: ReminderScheduler,
) {
    cancelVisibleTaskNotifications(context, taskId)
    scheduler.syncTaskFromCoordinator(taskId)
}
