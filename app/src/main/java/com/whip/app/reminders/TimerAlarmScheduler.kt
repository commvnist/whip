package com.whip.app.reminders

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import com.whip.app.WhipApplication
import com.whip.app.startup.MISSING_USER_DATA_GENERATION
import com.whip.app.startup.USER_DATA_GENERATION_KEY
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** A precise wakeup for user-started timers, with durable WorkManager delivery as fallback. */
internal class TimerAlarmScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)
    private val workManager = WorkManager.getInstance(appContext)
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    suspend fun enqueue(payload: TimerAlarmPayload, isCurrent: () -> Boolean = { true }) {
        require(payload.isStructurallyValid()) { "Invalid timer alarm payload" }
        val now = System.currentTimeMillis()
        val exact = payload.deadlineMillis > now && canScheduleExactReminderAlarms(appContext)
        // Persist the fallback before registering an alarm that may fire immediately.
        workManager.enqueueUniqueWork(
            payload.workName,
            ExistingWorkPolicy.REPLACE,
            payload.toWorkRequest(reminderFallbackDelayMillis(payload.deadlineMillis, now, exact)),
        ).await()
        if (!exact || !scheduleExact(payload, isCurrent)) {
            cancelAlarm(payload.identity)
            if (exact) {
                workManager.enqueueUniqueWork(
                    payload.workName,
                    ExistingWorkPolicy.REPLACE,
                    payload.toWorkRequest(
                        reminderFallbackDelayMillis(payload.deadlineMillis, System.currentTimeMillis(), false),
                    ),
                ).await()
            }
        }
    }

    suspend fun promote(payload: TimerAlarmPayload) {
        if (!payload.isStructurallyValid()) return
        cancelAlarm(payload.identity)
        workManager.enqueueUniqueWork(
            payload.workName,
            ExistingWorkPolicy.REPLACE,
            payload.toWorkRequest(0L),
        ).await()
    }

    suspend fun cancel(identity: String, workName: String) {
        cancelAlarm(identity)
        workManager.cancelUniqueWork(workName).await()
    }

    @Synchronized
    fun cancelAll() {
        registeredIdentities().forEach(::cancelAlarmLocked)
    }

    @Synchronized
    fun cancelAlarm(identity: String) {
        cancelAlarmLocked(identity)
    }

    @Synchronized
    private fun scheduleExact(payload: TimerAlarmPayload, isCurrent: () -> Boolean): Boolean {
        if (!isCurrent() || !canScheduleExactReminderAlarms(appContext)) return false
        val pending = payload.pendingIntent(appContext, create = true) ?: return false
        if (!writeRegisteredIdentities(registeredIdentities() + payload.identity)) return false
        return try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, payload.deadlineMillis, pending)
            true
        } catch (error: SecurityException) {
            Log.w(LOG_TAG, "Exact timer access was unavailable; using the durable fallback", error)
            cancelAlarmLocked(payload.identity)
            false
        } catch (error: IllegalStateException) {
            Log.w(LOG_TAG, "Exact timer quota was unavailable; using the durable fallback", error)
            cancelAlarmLocked(payload.identity)
            false
        }
    }

    private fun cancelAlarmLocked(identity: String) {
        TimerAlarmPayload.pendingIntentForIdentity(appContext, identity, create = false)?.let { pending ->
            alarmManager.cancel(pending)
            pending.cancel()
        }
        val registered = registeredIdentities()
        if (identity in registered) {
            check(writeRegisteredIdentities(registered - identity)) {
                "Could not persist exact timer cancellation"
            }
        }
    }

    private fun registeredIdentities(): Set<String> =
        preferences.getStringSet(REGISTERED_ALARMS_KEY, emptySet()).orEmpty().toSet()

    @SuppressLint("UseKtx") // A durable cancellation requires the commit result.
    private fun writeRegisteredIdentities(identities: Set<String>): Boolean = preferences.edit()
        .putStringSet(REGISTERED_ALARMS_KEY, identities)
        .commit()

    private companion object {
        const val PREFERENCES_NAME = "whip_timer_runtime"
        const val REGISTERED_ALARMS_KEY = "scheduled_exact_timer_identities"
        const val LOG_TAG = "WhipTimerAlarm"
    }
}

internal enum class TimerAlarmKind { Focus, Rest }

internal data class TimerAlarmPayload(
    val kind: TimerAlarmKind,
    val entityId: Long,
    val deadlineMillis: Long,
    val userDataGeneration: Long,
    val timerRevision: Long = Long.MIN_VALUE,
    val nextLabel: String? = null,
) {
    val identity: String get() = when (kind) {
        TimerAlarmKind.Focus -> "focus"
        TimerAlarmKind.Rest -> "rest:$entityId"
    }
    val workName: String get() = when (kind) {
        TimerAlarmKind.Focus -> FocusTimerScheduler.uniqueName
        TimerAlarmKind.Rest -> RestTimerScheduler.uniqueName(entityId)
    }

    fun isStructurallyValid(): Boolean = entityId > 0L && deadlineMillis > 0L &&
        userDataGeneration != MISSING_USER_DATA_GENERATION &&
        (kind != TimerAlarmKind.Rest || timerRevision >= 0L)

    fun toWorkRequest(delayMillis: Long): OneTimeWorkRequest {
        val data = Data.Builder()
            .putLong(USER_DATA_GENERATION_KEY, userDataGeneration)
        return when (kind) {
            TimerAlarmKind.Focus -> OneTimeWorkRequestBuilder<FocusTimerWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(
                    data.putLong(FocusTimerWorker.taskIdKey, entityId)
                        .putLong(FocusTimerWorker.deadlineKey, deadlineMillis).build(),
                )
                .addTag(ALL_WHIP_WORK_TAG)
                .build()
            TimerAlarmKind.Rest -> OneTimeWorkRequestBuilder<RestTimerWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(
                    data.putLong(RestTimerWorker.sessionIdKey, entityId)
                        .putLong(RestTimerWorker.timerRevisionKey, timerRevision)
                        .putLong(RestTimerWorker.expectedDeadlineMillisKey, deadlineMillis)
                        .putString(RestTimerWorker.nextLabelKey, nextLabel).build(),
                )
                .addTag(RestTimerScheduler.tag(entityId))
                .addTag(ALL_WHIP_WORK_TAG)
                .build()
        }
    }

    fun pendingIntent(context: Context, create: Boolean): PendingIntent? = PendingIntent.getBroadcast(
        context,
        identity.hashCode(),
        Intent(context, TimerAlarmReceiver::class.java)
            .setAction(ACTION_EXACT_TIMER)
            .setData(identityUri(identity))
            .putExtra(EXTRA_KIND, kind.name)
            .putExtra(EXTRA_ENTITY_ID, entityId)
            .putExtra(EXTRA_DEADLINE, deadlineMillis)
            .putExtra(EXTRA_GENERATION, userDataGeneration)
            .putExtra(EXTRA_REVISION, timerRevision)
            .putExtra(EXTRA_NEXT_LABEL, nextLabel),
        PendingIntent.FLAG_IMMUTABLE or
            if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE,
    )

    companion object {
        fun fromIntent(intent: Intent): TimerAlarmPayload? {
            if (intent.action != ACTION_EXACT_TIMER) return null
            val kind = intent.getStringExtra(EXTRA_KIND)
                ?.let { runCatching { TimerAlarmKind.valueOf(it) }.getOrNull() } ?: return null
            val payload = TimerAlarmPayload(
                kind = kind,
                entityId = intent.getLongExtra(EXTRA_ENTITY_ID, -1L),
                deadlineMillis = intent.getLongExtra(EXTRA_DEADLINE, -1L),
                userDataGeneration = intent.getLongExtra(EXTRA_GENERATION, MISSING_USER_DATA_GENERATION),
                timerRevision = intent.getLongExtra(EXTRA_REVISION, Long.MIN_VALUE),
                nextLabel = intent.getStringExtra(EXTRA_NEXT_LABEL),
            )
            return payload.takeIf {
                it.isStructurallyValid() && intent.data?.lastPathSegment?.let(Uri::decode) == it.identity
            }
        }

        fun pendingIntentForIdentity(context: Context, identity: String, create: Boolean): PendingIntent? =
            PendingIntent.getBroadcast(
                context,
                identity.hashCode(),
                Intent(context, TimerAlarmReceiver::class.java)
                    .setAction(ACTION_EXACT_TIMER)
                    .setData(identityUri(identity)),
                PendingIntent.FLAG_IMMUTABLE or
                    if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE,
            )

        private fun identityUri(identity: String): Uri = Uri.parse("whip://timer-alarm/${Uri.encode(identity)}")
    }
}

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val payload = TimerAlarmPayload.fromIntent(intent) ?: return
        val pending = goAsync()
        receiverScope.launch {
            try {
                val app = context.applicationContext as? WhipApplication ?: return@launch
                when (payload.kind) {
                    TimerAlarmKind.Focus -> app.focusTimerScheduler.promoteIfCurrent(payload)
                    TimerAlarmKind.Rest -> app.restTimerScheduler.promoteIfCurrent(payload)
                }
            } catch (error: Throwable) {
                Log.e(LOG_TAG, "Could not promote exact timer work", error)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val LOG_TAG = "WhipTimerAlarm"
        val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}

private const val ACTION_EXACT_TIMER = "com.whip.app.action.EXACT_TIMER"
private const val EXTRA_KIND = "timer_alarm_kind"
private const val EXTRA_ENTITY_ID = "timer_alarm_entity_id"
private const val EXTRA_DEADLINE = "timer_alarm_deadline"
private const val EXTRA_GENERATION = "timer_alarm_generation"
private const val EXTRA_REVISION = "timer_alarm_revision"
private const val EXTRA_NEXT_LABEL = "timer_alarm_next_label"
