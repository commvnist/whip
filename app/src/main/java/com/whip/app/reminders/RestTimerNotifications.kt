package com.whip.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.whip.app.MainActivity
import com.whip.app.R
import com.whip.app.WhipApplication
import com.whip.app.core.WhipLaunchActions
import java.util.concurrent.TimeUnit

object RestTimerNotifications {
    private const val CHANNEL_PREFIX = "gym_rest_timer"

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        listOf(false, true).forEach { sound ->
            listOf(false, true).forEach { vibration ->
                val channel = NotificationChannel(
                    channelId(sound, vibration),
                    "Workout rest timer${if (!sound && !vibration) " (silent)" else ""}",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Alerts when a workout rest period ends"
                    enableVibration(vibration)
                    if (!sound) setSound(null, AudioAttributes.Builder().build())
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun channelId(sound: Boolean, vibration: Boolean) =
        "$CHANNEL_PREFIX-${if (sound) "sound" else "silent"}-${if (vibration) "vibrate" else "still"}"

    fun notificationId(sessionId: Long): Int = 30_000 + sessionId.toInt()
}

class RestTimerScheduler(private val context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun schedule(sessionId: Long, seconds: Int, nextLabel: String?) {
        val data = Data.Builder()
            .putLong(RestTimerWorker.sessionIdKey, sessionId)
            .putString(RestTimerWorker.nextLabelKey, nextLabel)
            .build()
        val work = OneTimeWorkRequestBuilder<RestTimerWorker>()
            .setInitialDelay(seconds.coerceAtLeast(1).toLong(), TimeUnit.SECONDS)
            .setInputData(data)
            .addTag(tag(sessionId))
            .addTag(ALL_WHIP_WORK_TAG)
            .build()
        workManager.enqueueUniqueWork(
            uniqueName(sessionId),
            androidx.work.ExistingWorkPolicy.REPLACE,
            work,
        )
    }

    fun cancel(sessionId: Long) {
        workManager.cancelUniqueWork(uniqueName(sessionId))
        NotificationManagerCompat.from(context).cancel(RestTimerNotifications.notificationId(sessionId))
    }

    private fun uniqueName(sessionId: Long) = "whip-rest-$sessionId"
    private fun tag(sessionId: Long) = "rest-session-$sessionId"
}

class RestTimerWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val sessionId = inputData.getLong(sessionIdKey, -1)
        if (sessionId < 0) return Result.failure()
        val app = applicationContext as WhipApplication
        val session = com.whip.app.data.WhipDatabase.get(applicationContext).gymDao().getSession(sessionId)
            ?: return Result.success()
        val deadline = session.restTimerDeadlineMillis ?: return Result.success()
        if (!restTimerShouldNotify(session.state, deadline, System.currentTimeMillis())) return Result.success()
        // A replaced/adjusted timer may race a previously scheduled worker.
        // Only the worker at or beyond the currently persisted deadline wins.
        app.gymRepository.stopRestTimer(sessionId)
        val next = inputData.getString(nextLabelKey)
        val settings = app.settingsRepository.current()
        val launchIntent = Intent(applicationContext, MainActivity::class.java)
            .setAction(WhipLaunchActions.ACTION_OPEN_GYM)
            .putExtra(WhipLaunchActions.EXTRA_ENTITY_ID, sessionId)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            sessionId.toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(
            applicationContext,
            RestTimerNotifications.channelId(settings.timerSound, settings.timerVibration),
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Rest complete")
            .setContentText(next?.takeIf(String::isNotBlank) ?: "Ready for your next set")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSilent(!settings.timerSound)
            .setVibrate(if (settings.timerVibration) longArrayOf(0, 250, 150, 250) else longArrayOf(0))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        try {
            NotificationManagerCompat.from(applicationContext)
                .notify(RestTimerNotifications.notificationId(sessionId), notification)
        } catch (_: SecurityException) {
            // The timer remains valid when notification permission is declined.
        }
        return Result.success()
    }

    companion object {
        const val sessionIdKey = "session_id"
        const val nextLabelKey = "next_label"
    }
}

internal fun restTimerShouldNotify(state: String, deadlineMillis: Long?, nowMillis: Long): Boolean =
    state == com.whip.app.domain.WorkoutSessionState.Active.name &&
        deadlineMillis != null && deadlineMillis <= nowMillis + 1_000L
