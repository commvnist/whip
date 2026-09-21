package com.whip.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object FocusTimerNotifications {
    const val channelId = "focus_timer"
    const val notificationId = 40_001

    fun createChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(channelId, "Focus timer", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Alerts when a user-started focus session finishes"
            },
        )
    }
}

class FocusTimerScheduler(private val context: Context) {
    private val operationMutex = Mutex()
    private val operationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    internal suspend fun <T> withDeliveryBoundary(block: suspend () -> T): T =
        operationMutex.withLock { block() }

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
        if (current.focusTimerTaskId != taskId || current.focusTimerDeadlineMillis != deadlineMillis) return
        app.timerAlarmScheduler.enqueue(
            TimerAlarmPayload(
                kind = TimerAlarmKind.Focus,
                entityId = taskId,
                deadlineMillis = deadlineMillis,
                userDataGeneration = generation,
            ),
            isCurrent = {
                val settings = app.settingsRepository.current()
                settings.focusTimerTaskId == taskId && settings.focusTimerDeadlineMillis == deadlineMillis
            },
        )
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
                if (app.tryWithUserDataAccessNow { true } == true) {
                    app.timerAlarmScheduler.cancel("focus", uniqueName)
                    NotificationManagerCompat.from(context).cancel(FocusTimerNotifications.notificationId)
                }
            }
        }
    }

    companion object { const val uniqueName = "whip-focus-timer" }
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
                    val launchIntent = Intent(applicationContext, MainActivity::class.java)
                        .setAction(WhipLaunchActions.ACTION_OPEN_TASK)
                        .putExtra(WhipLaunchActions.EXTRA_ENTITY_ID, taskId)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    val pendingIntent = PendingIntent.getActivity(
                        applicationContext, FocusTimerNotifications.notificationId, launchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                    val notification = NotificationCompat.Builder(applicationContext, FocusTimerNotifications.channelId)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("Focus session complete")
                        .setContentText(task?.title ?: "Take a moment to review what you finished")
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
