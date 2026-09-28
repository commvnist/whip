package com.whip.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.whip.app.MainActivity
import com.whip.app.R
import com.whip.app.WhipApplication
import com.whip.app.core.WhipLaunchActions
import com.whip.app.startup.MISSING_USER_DATA_GENERATION
import com.whip.app.startup.USER_DATA_GENERATION_KEY
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object FocusTimerNotifications {
    const val channelId = "focus_timer"
    const val notificationId = 40_001
    const val openGenerationKey = "focus_open_generation"
    const val openDeadlineKey = "focus_open_deadline"

    fun createChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(channelId, "Focus timer", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Running focus countdowns and focus completion alerts"
            },
        )
    }
}

data class FocusTimerStartReceipt(
    val taskId: Long,
    val taskTitle: String,
    val minutes: Int,
    val deadlineMillis: Long,
    val warnings: List<String> = emptyList(),
    val schedulingWarnings: List<String> = emptyList(),
)

data class FocusTimerCompletionReceipt(val taskId: Long, val taskTitle: String, val deadlineMillis: Long)

class FocusTimerScheduler(private val context: Context) {
    private val operationMutex = Mutex()
    private val operationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val completionEvents = MutableSharedFlow<FocusTimerCompletionReceipt>(extraBufferCapacity = 1)
    val completions = completionEvents.asSharedFlow()

    internal fun publishCompletion(receipt: FocusTimerCompletionReceipt) {
        completionEvents.tryEmit(receipt)
    }

    internal suspend fun <T> withDeliveryBoundary(block: suspend () -> T): T =
        operationMutex.withLock { block() }

    suspend fun start(
        taskId: Long,
        minutes: Int,
        expectedActiveTaskId: Long? = null,
        expectedActiveDeadlineMillis: Long? = null,
    ): FocusTimerStartReceipt = operationMutex.withLock {
        require(taskId > 0L) { "Focus timer requires a valid Task" }
        require(minutes in 1..240) { "Enter a duration from 1 to 240 minutes" }
        val app = context.applicationContext as WhipApplication
        checkNotNull(app.withUserDataAccess {
            val now = System.currentTimeMillis()
            val deadline = now + minutes * 60_000L
            val task = app.database.withTransaction {
                // Task deletion cannot commit between ownership validation and saving the pair.
                val owner = checkNotNull(app.taskRepository.getTask(taskId)) { "This Task no longer exists" }
                check(!owner.archived) { "Restore this Task before starting focus" }
                val saved = app.settingsRepository.updateAndConfirm { current ->
                    check(focusTimerReplacementAllowed(
                        current.focusTimerTaskId, current.focusTimerDeadlineMillis,
                        expectedActiveTaskId, expectedActiveDeadlineMillis, now,
                    )) { "Another focus session is active. Review it before replacing it" }
                    current.copy(focusTimerTaskId = taskId, focusTimerDeadlineMillis = deadline)
                }
                check(saved) { "Could not save the focus session. Try again" }
                owner
            }
            val warnings = mutableListOf<String>()
            val schedulingWarnings = mutableListOf<String>()
            try {
                scheduleInternal(taskId, deadline, app.currentUserDataGeneration())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w("WhipFocusTimer", "Saved focus timer could not finish background scheduling", error)
                schedulingWarnings += "Alert setup failed. Stop and restart Focus to retry."
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled() ||
                (manager.getNotificationChannel(FocusTimerNotifications.channelId)?.importance ?: NotificationManager.IMPORTANCE_NONE) == NotificationManager.IMPORTANCE_NONE
            ) warnings += "Focus is running in Whip. Enable Focus timer notifications in Android to see the countdown and completion alert"
            if (!canScheduleExactReminderAlarms(context)) {
                schedulingWarnings += "Precise alarms are off; the alert may arrive late."
            }
            FocusTimerStartReceipt(taskId, task.title, minutes, deadline, warnings + schedulingWarnings, schedulingWarnings)
        }) { "Whip data is unavailable while recovery is in progress" }
    }

    suspend fun stopIfCurrent(taskId: Long, deadlineMillis: Long, generation: Long? = null): Boolean =
        operationMutex.withLock {
            val app = context.applicationContext as WhipApplication
            app.withUserDataAccess {
                if (generation != null && !app.isCurrentUserDataGeneration(generation)) return@withUserDataAccess false
                var matched = false
                val committed = app.settingsRepository.updateAndConfirm { current ->
                    if (current.focusTimerTaskId == taskId && current.focusTimerDeadlineMillis == deadlineMillis) {
                        matched = true
                        current.copy(focusTimerTaskId = null, focusTimerDeadlineMillis = null)
                    } else current
                }
                check(committed) { "Could not save the stopped focus session. Try again" }
                if (!matched) return@withUserDataAccess false
                try {
                    app.timerAlarmScheduler.cancel("focus", uniqueName)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    // Clearing the identity has already made remaining work harmless.
                    Log.w("WhipFocusTimer", "Stopped focus session could not finish work cleanup", error)
                } finally {
                    NotificationManagerCompat.from(context).cancel(FocusTimerNotifications.notificationId)
                }
                true
            } ?: false
        }

    suspend fun schedule(taskId: Long, deadlineMillis: Long, allowDuringRecovery: Boolean = false) {
        operationMutex.withLock {
            val app = context.applicationContext as WhipApplication
            if (allowDuringRecovery) {
                scheduleInternal(taskId, deadlineMillis, app.currentUserDataGeneration())
            } else {
                app.withUserDataAccess {
                    scheduleInternal(taskId, deadlineMillis, app.currentUserDataGeneration())
                }
            }
        }
    }

    private suspend fun scheduleInternal(taskId: Long, deadlineMillis: Long, generation: Long) {
        val app = context.applicationContext as WhipApplication
        val current = app.settingsRepository.current()
        if (!app.isCurrentUserDataGeneration(generation) ||
            current.focusTimerTaskId != taskId || current.focusTimerDeadlineMillis != deadlineMillis
        ) return
        try {
            app.timerAlarmScheduler.enqueue(
                TimerAlarmPayload(
                    kind = TimerAlarmKind.Focus,
                    entityId = taskId,
                    deadlineMillis = deadlineMillis,
                    userDataGeneration = generation,
                ),
                isCurrent = {
                    val settings = app.settingsRepository.current()
                    app.isCurrentUserDataGeneration(generation) &&
                        settings.focusTimerTaskId == taskId && settings.focusTimerDeadlineMillis == deadlineMillis
                },
            )
        } finally {
            // Presentation remains useful even when durable background preparation fails.
            publishCurrentNotification(taskId, deadlineMillis, generation)
        }
    }

    suspend fun refreshNotification() = operationMutex.withLock {
        val app = context.applicationContext as WhipApplication
        app.withUserDataAccess {
            val current = app.settingsRepository.current()
            val taskId = current.focusTimerTaskId ?: return@withUserDataAccess
            val deadline = current.focusTimerDeadlineMillis ?: return@withUserDataAccess
            publishCurrentNotification(taskId, deadline, app.currentUserDataGeneration())
        }
        Unit
    }

    private suspend fun publishCurrentNotification(taskId: Long, deadlineMillis: Long, generation: Long) {
        val app = context.applicationContext as WhipApplication
        val task = app.taskRepository.getTask(taskId) ?: return
        val settings = app.settingsRepository.current()
        if (!app.isCurrentUserDataGeneration(generation) || settings.focusTimerTaskId != taskId ||
            settings.focusTimerDeadlineMillis != deadlineMillis
        ) return
        val remaining = deadlineMillis - System.currentTimeMillis()
        if (remaining <= 0L) return
        // System UI owns the ticking clock; expiry removes it while durable completion work catches up.
        val openIntent = focusOpenIntent(context, taskId, generation, deadlineMillis)
        val stopIntent = Intent(context, FocusTimerStopReceiver::class.java)
            .setAction(FocusTimerStopReceiver.action)
            .setData(Uri.parse("whip://focus-stop/$generation/$taskId/$deadlineMillis"))
            .putExtra(USER_DATA_GENERATION_KEY, generation)
            .putExtra(FocusTimerWorker.taskIdKey, taskId)
            .putExtra(FocusTimerWorker.deadlineKey, deadlineMillis)
        val stop = PendingIntent.getBroadcast(context, FocusTimerNotifications.notificationId, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, FocusTimerNotifications.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Focusing on ${task.title}")
            .setContentText("Focus session running")
            .setWhen(deadlineMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setTimeoutAfter(remaining)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openIntent)
            .addAction(0, "Open", openIntent)
            .addAction(0, "Stop", stop)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(FocusTimerNotifications.notificationId, notification)
        } catch (_: SecurityException) {
            // The durable timer remains active when Android denies drawer presentation.
        }
    }

    internal suspend fun promoteIfCurrent(payload: TimerAlarmPayload) = operationMutex.withLock {
        val app = context.applicationContext as WhipApplication
        app.withUserDataAccess {
            val settings = app.settingsRepository.current()
            if (payload.kind == TimerAlarmKind.Focus &&
                app.isCurrentUserDataGeneration(payload.userDataGeneration) &&
                settings.focusTimerTaskId == payload.entityId &&
                settings.focusTimerDeadlineMillis == payload.deadlineMillis
            ) app.timerAlarmScheduler.promote(payload)
        }
    }

    fun cancel() {
        operationScope.launch(start = CoroutineStart.UNDISPATCHED) {
            operationMutex.withLock {
                val app = context.applicationContext as WhipApplication
                if (app.tryWithUserDataAccessNow { app.settingsRepository.current().focusTimerTaskId == null } == true) {
                    app.timerAlarmScheduler.cancel("focus", uniqueName)
                    NotificationManagerCompat.from(context).cancel(FocusTimerNotifications.notificationId)
                }
            }
        }
    }

    companion object { const val uniqueName = "whip-focus-timer" }
}

class FocusTimerStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != action) return
        val taskId = intent.getLongExtra(FocusTimerWorker.taskIdKey, -1L)
        val deadline = intent.getLongExtra(FocusTimerWorker.deadlineKey, -1L)
        val generation = intent.getLongExtra(USER_DATA_GENERATION_KEY, MISSING_USER_DATA_GENERATION)
        if (taskId <= 0L || deadline <= 0L || generation == MISSING_USER_DATA_GENERATION) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                (context.applicationContext as WhipApplication).focusTimerScheduler.stopIfCurrent(taskId, deadline, generation)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w("WhipFocusTimer", "Could not stop focus session", error)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val action = "com.whip.app.action.STOP_FOCUS"
        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}

private fun focusOpenIntent(context: Context, taskId: Long, generation: Long, deadlineMillis: Long? = null): PendingIntent {
    val launch = Intent(context, MainActivity::class.java)
        .setAction(WhipLaunchActions.ACTION_OPEN_TASK)
        .setData(Uri.parse("whip://focus-open/$generation/$taskId/${deadlineMillis ?: "complete"}"))
        .putExtra(WhipLaunchActions.EXTRA_ENTITY_ID, taskId)
        .putExtra(FocusTimerNotifications.openGenerationKey, generation)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    if (deadlineMillis != null) launch.putExtra(FocusTimerNotifications.openDeadlineKey, deadlineMillis)
    return PendingIntent.getActivity(context, FocusTimerNotifications.notificationId, launch,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

class FocusTimerWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val app = applicationContext as WhipApplication
        return try {
            app.focusTimerScheduler.withDeliveryBoundary {
                app.withUserDataAccess {
                    if (!app.isCurrentUserDataGeneration(
                            inputData.getLong(USER_DATA_GENERATION_KEY, MISSING_USER_DATA_GENERATION),
                        )
                    ) return@withUserDataAccess Result.success()
                    val taskId = inputData.getLong(taskIdKey, -1L)
                    val expectedDeadline = inputData.getLong(deadlineKey, -1L)
                    if (taskId < 0L || expectedDeadline < 0L) return@withUserDataAccess Result.failure()
                    val settings = app.settingsRepository.current()
                    if (!focusTimerShouldNotify(
                            settings.focusTimerTaskId,
                            settings.focusTimerDeadlineMillis,
                            taskId,
                            expectedDeadline,
                            System.currentTimeMillis(),
                        )
                    ) return@withUserDataAccess Result.success()

                    val task = app.taskRepository.getTask(taskId)
                    val pendingIntent = focusOpenIntent(applicationContext, taskId, app.currentUserDataGeneration())
                    val notification = NotificationCompat.Builder(applicationContext, FocusTimerNotifications.channelId)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("Focus session complete")
                        .setContentText(task?.title ?: "Take a moment to review what you finished")
                        .setSilent(false)
                        .setOnlyAlertOnce(false)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .build()
                    val notifications = NotificationManagerCompat.from(applicationContext)
                    try {
                        notifications.notify(FocusTimerNotifications.notificationId, notification)
                    } catch (_: SecurityException) {
                        // A declined permission is terminal for the alert, not a storage failure.
                    }
                    var completedCurrentTimer = false
                    val committed = app.settingsRepository.updateAndConfirm { current ->
                        if (current.focusTimerTaskId == taskId &&
                            current.focusTimerDeadlineMillis == expectedDeadline
                        ) {
                            completedCurrentTimer = true
                            current.copy(focusTimerTaskId = null, focusTimerDeadlineMillis = null)
                        } else current
                    }
                    if (!committed) return@withUserDataAccess Result.retry()
                    if (!completedCurrentTimer) {
                        notifications.cancel(FocusTimerNotifications.notificationId)
                        return@withUserDataAccess Result.success()
                    }
                    app.focusTimerScheduler.publishCompletion(
                        FocusTimerCompletionReceipt(taskId, task?.title ?: "Focus session", expectedDeadline),
                    )
                    app.timerAlarmScheduler.cancelAlarm("focus")
                    Result.success()
                } ?: Result.retry()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val taskIdKey = "task_id"
        const val deadlineKey = "deadline_millis"
    }
}

internal fun focusTimerShouldNotify(
    currentTaskId: Long?,
    currentDeadlineMillis: Long?,
    expectedTaskId: Long,
    expectedDeadlineMillis: Long,
    nowMillis: Long,
): Boolean = currentTaskId == expectedTaskId &&
    currentDeadlineMillis == expectedDeadlineMillis &&
    expectedDeadlineMillis <= nowMillis + 1_000L &&
    !focusTimerDeadlineIsTooOld(expectedDeadlineMillis, nowMillis)

internal fun focusTimerDeadlineIsTooOld(deadlineMillis: Long, nowMillis: Long): Boolean =
    deadlineMillis < nowMillis - FOCUS_TIMER_MAX_LATE_MILLIS

private const val FOCUS_TIMER_MAX_LATE_MILLIS = 24 * 60 * 60 * 1_000L

internal fun focusTimerReplacementAllowed(
    currentTaskId: Long?,
    currentDeadlineMillis: Long?,
    expectedTaskId: Long?,
    expectedDeadlineMillis: Long?,
    nowMillis: Long,
): Boolean = currentTaskId == null || currentDeadlineMillis == null || currentDeadlineMillis <= nowMillis ||
    (currentTaskId == expectedTaskId && currentDeadlineMillis == expectedDeadlineMillis)
