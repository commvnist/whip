package com.whip.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    private val operationMutex = Mutex()

    internal suspend fun <T> withDeliveryBoundary(block: suspend () -> T): T =
        operationMutex.withLock { block() }

    internal suspend fun promoteIfCurrent(payload: TimerAlarmPayload) = operationMutex.withLock {
        val app = context.applicationContext as WhipApplication
        app.withUserDataAccess {
            val session = com.whip.app.data.WhipDatabase.get(context).gymDao().getSession(payload.entityId)
            if (payload.kind == TimerAlarmKind.Rest &&
                app.isCurrentUserDataGeneration(payload.userDataGeneration) &&
                session?.state == com.whip.app.domain.WorkoutSessionState.Active.name &&
                session.restTimerRevision == payload.timerRevision &&
                session.restTimerDeadlineMillis == payload.deadlineMillis
            ) app.timerAlarmScheduler.promote(payload)
        }
    }

    suspend fun schedule(
        sessionId: Long,
        nextLabel: String?,
        timerRevision: Long,
        expectedDeadlineMillis: Long,
        allowDuringRecovery: Boolean = false,
    ) = operationMutex.withLock {
        val app = context.applicationContext as WhipApplication
        if (allowDuringRecovery) {
            scheduleInternal(
                sessionId,
                nextLabel,
                timerRevision,
                expectedDeadlineMillis,
                app.currentUserDataGeneration(),
            )
        } else {
            checkNotNull(app.withUserDataAccess {
                scheduleInternal(
                    sessionId,
                    nextLabel,
                    timerRevision,
                    expectedDeadlineMillis,
                    app.currentUserDataGeneration(),
                )
                Unit
            }) { "Whip data is unavailable while recovery is in progress" }
        }
    }

    private suspend fun scheduleInternal(
        sessionId: Long,
        nextLabel: String?,
        timerRevision: Long,
        expectedDeadlineMillis: Long,
        generation: Long,
    ) {
        val session = com.whip.app.data.WhipDatabase.get(context).gymDao().getSession(sessionId)
        if (session?.state != com.whip.app.domain.WorkoutSessionState.Active.name ||
            session.restTimerRevision != timerRevision ||
            session.restTimerDeadlineMillis != expectedDeadlineMillis
        ) return
        // A just-finished prior timer may still have a visible notification. Starting or
        // restoring the durable timer replaces that terminal UI as one scheduler action.
        NotificationManagerCompat.from(context).cancel(RestTimerNotifications.notificationId(sessionId))
        (context.applicationContext as WhipApplication).timerAlarmScheduler.enqueue(
            TimerAlarmPayload(
                kind = TimerAlarmKind.Rest,
                entityId = sessionId,
                deadlineMillis = expectedDeadlineMillis,
                userDataGeneration = generation,
                timerRevision = timerRevision,
                nextLabel = nextLabel,
            ),
        )
    }

    suspend fun cancel(sessionId: Long, allowDuringRecovery: Boolean = false) {
        operationMutex.withLock {
            val app = context.applicationContext as WhipApplication
            if (allowDuringRecovery) {
                cancelInternal(sessionId)
            } else {
                checkNotNull(app.withUserDataAccess {
                    cancelInternal(sessionId)
                    Unit
                }) { "Whip data is unavailable while recovery is in progress" }
            }
        }
    }

    private suspend fun cancelInternal(sessionId: Long) {
        (context.applicationContext as WhipApplication).timerAlarmScheduler.cancel(
            "rest:$sessionId",
            uniqueName(sessionId),
        )
        NotificationManagerCompat.from(context).cancel(RestTimerNotifications.notificationId(sessionId))
    }

    internal companion object {
        fun uniqueName(sessionId: Long) = "whip-rest-$sessionId"
        fun tag(sessionId: Long) = "rest-session-$sessionId"
    }
}

class RestTimerWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val app = applicationContext as WhipApplication
        return try {
            app.restTimerScheduler.withDeliveryBoundary {
                app.withUserDataAccess {
                    if (!app.isCurrentUserDataGeneration(
                            inputData.getLong(USER_DATA_GENERATION_KEY, MISSING_USER_DATA_GENERATION),
                        )
                    ) return@withUserDataAccess Result.success()
                    val sessionId = inputData.getLong(sessionIdKey, -1)
                    if (sessionId < 0) return@withUserDataAccess Result.failure()
                    val expectedRevision = inputData.getLong(timerRevisionKey, Long.MIN_VALUE)
                    val expectedDeadline = inputData.getLong(expectedDeadlineMillisKey, Long.MIN_VALUE)
                    val session = com.whip.app.data.WhipDatabase.get(applicationContext).gymDao().getSession(sessionId)
                        ?: return@withUserDataAccess Result.success()
                    if (
                        session.restTimerRevision != expectedRevision ||
                        session.restTimerDeadlineMillis != expectedDeadline ||
                        !restTimerShouldNotify(session.state, expectedDeadline, System.currentTimeMillis())
                    ) return@withUserDataAccess Result.success()
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
                    val notificationId = RestTimerNotifications.notificationId(sessionId)
                    val delivered = deliverRestTimerAtLeastOnce(
                        post = {
                            try {
                                NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
                            } catch (_: SecurityException) {
                                // A declined permission is terminal, not a transient delivery failure.
                            }
                        },
                        complete = {
                            app.gymRepository.completeRestTimerDelivery(
                                sessionId,
                                expectedRevision,
                                expectedDeadline,
                            )
                        },
                        cancelStale = { NotificationManagerCompat.from(applicationContext).cancel(notificationId) },
                    )
                    if (delivered) app.timerAlarmScheduler.cancelAlarm("rest:$sessionId")
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
        const val sessionIdKey = "session_id"
        const val nextLabelKey = "next_label"
        const val timerRevisionKey = "timer_revision"
        const val expectedDeadlineMillisKey = "expected_deadline_millis"
    }
}

internal suspend fun deliverRestTimerAtLeastOnce(
    post: () -> Unit,
    complete: suspend () -> Boolean,
    cancelStale: () -> Unit,
): Boolean {
    // Posting the same notification ID is idempotent. Persist only afterward so process death
    // between those steps replays delivery instead of turning a committed deadline into silence.
    post()
    return complete().also { completed -> if (!completed) cancelStale() }
}

internal fun restTimerShouldNotify(state: String, deadlineMillis: Long?, nowMillis: Long): Boolean =
    state == com.whip.app.domain.WorkoutSessionState.Active.name &&
        deadlineMillis != null && deadlineMillis <= nowMillis + 1_000L
