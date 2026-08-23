package com.whip.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.whip.app.MainActivity
import com.whip.app.R
import com.whip.app.WhipApplication
import com.whip.app.core.WhipLaunchActions
import java.util.concurrent.TimeUnit

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
    private val workManager = WorkManager.getInstance(context)

    fun schedule(taskId: Long, deadlineMillis: Long) {
        val work = OneTimeWorkRequestBuilder<FocusTimerWorker>()
            .setInitialDelay((deadlineMillis - System.currentTimeMillis()).coerceAtLeast(1L), TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putLong(FocusTimerWorker.taskIdKey, taskId).putLong(FocusTimerWorker.deadlineKey, deadlineMillis).build())
            .addTag(ALL_WHIP_WORK_TAG)
            .build()
        workManager.enqueueUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, work)
    }

    fun cancel() {
        workManager.cancelUniqueWork(uniqueName)
        NotificationManagerCompat.from(context).cancel(FocusTimerNotifications.notificationId)
    }

    companion object { const val uniqueName = "whip-focus-timer" }
}

class FocusTimerWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(taskIdKey, -1L)
        val expectedDeadline = inputData.getLong(deadlineKey, -1L)
        if (taskId < 0L || expectedDeadline < 0L) return Result.failure()
        val app = applicationContext as WhipApplication
        val settings = app.settingsRepository.current()
        if (!focusTimerShouldNotify(settings.focusTimerTaskId, settings.focusTimerDeadlineMillis, taskId, expectedDeadline, System.currentTimeMillis())) {
            return Result.success()
        }
        val task = app.taskRepository.getTask(taskId)
        app.settingsRepository.update { it.copy(focusTimerTaskId = null, focusTimerDeadlineMillis = null) }
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
        try {
            NotificationManagerCompat.from(applicationContext).notify(FocusTimerNotifications.notificationId, notification)
        } catch (_: SecurityException) {
            // The session still finishes when notification permission is unavailable.
        }
        return Result.success()
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
): Boolean = currentTaskId == expectedTaskId && currentDeadlineMillis == expectedDeadlineMillis && expectedDeadlineMillis <= nowMillis + 1_000L
