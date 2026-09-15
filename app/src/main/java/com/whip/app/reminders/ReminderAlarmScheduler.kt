package com.whip.app.reminders

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
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

/**
 * Exact alarms are the clock for user-authored reminders. WorkManager remains
 * the durable execution path and a bounded fallback if Android does not deliver
 * the alarm. Both paths carry the same untrusted claim and converge on one
 * unique work name, so every notification still passes live state validation.
 */
internal class ReminderAlarmScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)
    private val workManager = WorkManager.getInstance(appContext)
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    suspend fun enqueue(payload: ReminderAlarmPayload) {
        require(payload.isStructurallyValid()) { "Invalid reminder alarm payload" }
        val nowMillis = System.currentTimeMillis()
        if (payload.claim.expectedTriggerAtMillis <= nowMillis) {
            cancelIdentity(payload.alarmIdentity)
            enqueueWork(payload, delayMillis = 0L)
            return
        }

        if (!canScheduleExactReminderAlarms(appContext)) {
            cancelIdentity(payload.alarmIdentity)
            enqueueWork(
                payload,
                reminderFallbackDelayMillis(
                    expectedTriggerAtMillis = payload.claim.expectedTriggerAtMillis,
                    nowMillis = nowMillis,
                    exactScheduled = false,
                ),
            )
            return
        }

        // Install the durable fallback first. A near-term alarm may be delivered
        // as soon as setExactAndAllowWhileIdle returns; ordering this way ensures
        // an exact promotion is always the final REPLACE, never overwritten by a
        // later delayed request.
        enqueueWork(
            payload,
            reminderFallbackDelayMillis(
                expectedTriggerAtMillis = payload.claim.expectedTriggerAtMillis,
                nowMillis = nowMillis,
                exactScheduled = true,
            ),
        )
        if (!scheduleExact(payload)) {
            enqueueWork(
                payload,
                reminderFallbackDelayMillis(
                    expectedTriggerAtMillis = payload.claim.expectedTriggerAtMillis,
                    nowMillis = System.currentTimeMillis(),
                    exactScheduled = false,
                ),
            )
        }
    }

    private suspend fun enqueueWork(payload: ReminderAlarmPayload, delayMillis: Long) {
        workManager.enqueueUniqueWork(
            payload.workName,
            ExistingWorkPolicy.REPLACE,
            payload.toWorkRequest(delayMillis),
        ).await()
    }

    suspend fun promote(payload: ReminderAlarmPayload) {
        if (!payload.isStructurallyValid()) return
        consume(payload.alarmIdentity)
        workManager.enqueueUniqueWork(
            payload.workName,
            ExistingWorkPolicy.REPLACE,
            payload.toWorkRequest(delayMillis = 0L),
        ).await()
    }

    @Synchronized
    fun cancelEntity(domain: ReminderDomain, entityId: Long) {
        val prefix = "${domain.name}:$entityId:"
        registeredIdentities().filter { it.startsWith(prefix) }.forEach(::cancelIdentityLocked)
    }

    @Synchronized
    fun cancelAll() {
        registeredIdentities().forEach(::cancelIdentityLocked)
    }

    @Synchronized
    fun cancelFromWorker(data: Data) {
        val identity = data.getString(REMINDER_ALARM_IDENTITY_KEY) ?: return
        cancelIdentityLocked(identity)
    }

    @Synchronized
    private fun cancelIdentity(identity: String) {
        cancelIdentityLocked(identity)
    }

    @Synchronized
    private fun scheduleExact(payload: ReminderAlarmPayload): Boolean {
        if (!canScheduleExactReminderAlarms(appContext)) {
            cancelIdentityLocked(payload.alarmIdentity)
            return false
        }
        val pendingIntent = payload.pendingIntent(appContext, create = true) ?: return false
        return try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                payload.claim.expectedTriggerAtMillis,
                pendingIntent,
            )
            val updated = registeredIdentities() + payload.alarmIdentity
            if (!writeRegisteredIdentities(updated)) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                false
            } else {
                true
            }
        } catch (error: SecurityException) {
            Log.w(LOG_TAG, "Exact reminder access was unavailable; using WorkManager fallback", error)
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            writeRegisteredIdentities(registeredIdentities() - payload.alarmIdentity)
            false
        } catch (error: IllegalStateException) {
            // Some Android builds impose a per-app alarm quota. Preserve the
            // reminder through the WorkManager path instead of failing the sync.
            Log.w(LOG_TAG, "Exact reminder quota was unavailable; using WorkManager fallback", error)
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            writeRegisteredIdentities(registeredIdentities() - payload.alarmIdentity)
            false
        }
    }

    @Synchronized
    private fun consume(identity: String) {
        if (!writeRegisteredIdentities(registeredIdentities() - identity)) {
            Log.w(LOG_TAG, "Could not remove a delivered exact reminder from the registry")
        }
    }

    private fun cancelIdentityLocked(identity: String) {
        ReminderAlarmPayload.pendingIntentForIdentity(appContext, identity, create = false)?.let { pending ->
            alarmManager.cancel(pending)
            pending.cancel()
        }
        val identities = registeredIdentities()
        if (identity in identities) {
            check(writeRegisteredIdentities(identities - identity)) {
                "Could not persist exact reminder cancellation"
            }
        }
    }

    private fun registeredIdentities(): Set<String> =
        preferences.getStringSet(REGISTERED_ALARMS_KEY, emptySet()).orEmpty().toSet()

    @SuppressLint("UseKtx") // The KTX wrapper discards commit's durability result.
    private fun writeRegisteredIdentities(identities: Set<String>): Boolean = preferences.edit()
        .putStringSet(REGISTERED_ALARMS_KEY, identities)
        .commit()

    private companion object {
        const val PREFERENCES_NAME = "whip_reminder_runtime"
        const val REGISTERED_ALARMS_KEY = "scheduled_exact_alarm_identities"
        const val LOG_TAG = "WhipReminderAlarm"
    }
}

internal data class ReminderAlarmPayload(
    val domain: ReminderDomain,
    val entityId: Long,
    val workName: String,
    val claim: ReminderDeliveryClaim,
    val userDataGeneration: Long,
    val offsetMinutes: Int? = null,
) {
    val alarmIdentity: String
        get() = "${domain.name}:$entityId:$userDataGeneration:$workName"

    fun isStructurallyValid(): Boolean =
        entityId >= 0L &&
            workName.isNotBlank() &&
            workName.startsWith(reminderEntityTag(domain, entityId)) &&
            claim.isStructurallyValid() &&
            userDataGeneration != MISSING_USER_DATA_GENERATION &&
            ((domain == ReminderDomain.Task && offsetMinutes != null) ||
                (domain != ReminderDomain.Task && offsetMinutes == null))

    fun toWorkRequest(delayMillis: Long): OneTimeWorkRequest {
        val input = Data.Builder()
            .putReminderDeliveryClaim(claim)
            .putLong(USER_DATA_GENERATION_KEY, userDataGeneration)
            .putString(REMINDER_ALARM_IDENTITY_KEY, alarmIdentity)
            .apply {
                when (domain) {
                    ReminderDomain.Task -> {
                        putLong(ReminderWorker.TASK_ID, entityId)
                        putLong(ReminderWorker.ORIGINAL_EPOCH_DAY, claim.logicalEpochDay)
                        putInt(ReminderWorker.OFFSET_MINUTES, requireNotNull(offsetMinutes))
                    }
                    ReminderDomain.Habit -> {
                        putLong(HabitReminderWorker.HABIT_ID, entityId)
                        putLong(HabitReminderWorker.LOGICAL_EPOCH_DAY, claim.logicalEpochDay)
                    }
                    ReminderDomain.Goal -> putLong(GoalReminderWorker.GOAL_ID, entityId)
                }
            }
            .build()
        val tag = reminderEntityTag(domain, entityId)
        return when (domain) {
            ReminderDomain.Task -> OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(input)
                .addTag(tag)
                .addTag(ALL_WHIP_WORK_TAG)
                .build()
            ReminderDomain.Habit -> OneTimeWorkRequestBuilder<HabitReminderWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(input)
                .addTag(tag)
                .addTag(ALL_WHIP_WORK_TAG)
                .build()
            ReminderDomain.Goal -> OneTimeWorkRequestBuilder<GoalReminderWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(input)
                .addTag(tag)
                .addTag(ALL_WHIP_WORK_TAG)
                .build()
        }
    }

    fun pendingIntent(context: Context, create: Boolean): PendingIntent? {
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
            .setAction(REMINDER_EXACT_ALARM_ACTION)
            .setData(identityUri(alarmIdentity))
            .putExtra(EXTRA_DOMAIN, domain.name)
            .putExtra(EXTRA_ENTITY_ID, entityId)
            .putExtra(EXTRA_WORK_NAME, workName)
            .putExtra(EXTRA_USER_DATA_GENERATION, userDataGeneration)
            .putExtra(EXTRA_OFFSET_MINUTES, offsetMinutes ?: Int.MIN_VALUE)
            .putExtra(EXTRA_CLAIM_VERSION, claim.version)
            .putExtra(EXTRA_CLAIM_KIND, claim.kind.name)
            .putExtra(EXTRA_STABLE_ENTITY_ID, claim.stableEntityId)
            .putExtra(EXTRA_LOGICAL_EPOCH_DAY, claim.logicalEpochDay)
            .putExtra(EXTRA_EXPECTED_TRIGGER, claim.expectedTriggerAtMillis)
            .putExtra(EXTRA_DEFINITION_FINGERPRINT, claim.definitionFingerprint)
        return PendingIntent.getBroadcast(
            context,
            alarmIdentity.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or
                if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE,
        )
    }

    companion object {
        fun fromIntent(intent: Intent): ReminderAlarmPayload? {
            if (intent.action != REMINDER_EXACT_ALARM_ACTION) return null
            val domain = intent.getStringExtra(EXTRA_DOMAIN)
                ?.let { runCatching { ReminderDomain.valueOf(it) }.getOrNull() }
                ?: return null
            val offset = intent.getIntExtra(EXTRA_OFFSET_MINUTES, Int.MIN_VALUE)
                .takeUnless { it == Int.MIN_VALUE }
            val claim = ReminderDeliveryClaim(
                version = intent.getIntExtra(EXTRA_CLAIM_VERSION, -1),
                kind = intent.getStringExtra(EXTRA_CLAIM_KIND)
                    ?.let { runCatching { ReminderDeliveryKind.valueOf(it) }.getOrNull() }
                    ?: return null,
                stableEntityId = intent.getStringExtra(EXTRA_STABLE_ENTITY_ID).orEmpty(),
                logicalEpochDay = intent.getLongExtra(EXTRA_LOGICAL_EPOCH_DAY, Long.MIN_VALUE),
                expectedTriggerAtMillis = intent.getLongExtra(EXTRA_EXPECTED_TRIGGER, -1L),
                definitionFingerprint = intent.getStringExtra(EXTRA_DEFINITION_FINGERPRINT).orEmpty(),
            )
            val payload = ReminderAlarmPayload(
                domain = domain,
                entityId = intent.getLongExtra(EXTRA_ENTITY_ID, -1L),
                workName = intent.getStringExtra(EXTRA_WORK_NAME).orEmpty(),
                claim = claim,
                userDataGeneration = intent.getLongExtra(
                    EXTRA_USER_DATA_GENERATION,
                    MISSING_USER_DATA_GENERATION,
                ),
                offsetMinutes = offset,
            )
            val identity = intent.data?.lastPathSegment?.let(Uri::decode)
            return payload.takeIf { it.isStructurallyValid() && identity == it.alarmIdentity }
        }

        fun pendingIntentForIdentity(
            context: Context,
            identity: String,
            create: Boolean,
        ): PendingIntent? = PendingIntent.getBroadcast(
            context,
            identity.hashCode(),
            Intent(context, ReminderAlarmReceiver::class.java)
                .setAction(REMINDER_EXACT_ALARM_ACTION)
                .setData(identityUri(identity)),
            PendingIntent.FLAG_IMMUTABLE or
                if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE,
        )

        private fun identityUri(identity: String): Uri =
            Uri.parse("whip://reminder-alarm/${Uri.encode(identity)}")
    }
}

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val payload = ReminderAlarmPayload.fromIntent(intent) ?: return
        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                val app = context.applicationContext as? WhipApplication ?: return@launch
                app.reminderAlarmScheduler.promote(payload)
            } catch (error: Throwable) {
                Log.e(LOG_TAG, "Could not promote exact reminder work", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val LOG_TAG = "WhipReminderAlarm"
        val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}

internal fun canScheduleExactReminderAlarms(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

internal fun reminderFallbackDelayMillis(
    expectedTriggerAtMillis: Long,
    nowMillis: Long,
    exactScheduled: Boolean,
): Long {
    val fallbackAt = if (exactScheduled) {
        expectedTriggerAtMillis.saturatedPlus(REMINDER_WORK_FALLBACK_GRACE_MILLIS)
    } else {
        expectedTriggerAtMillis
    }
    return (fallbackAt - nowMillis).coerceAtLeast(0L)
}

internal fun reminderEntityTag(domain: ReminderDomain, entityId: Long): String = when (domain) {
    ReminderDomain.Task -> "whip-reminder-$entityId"
    ReminderDomain.Habit -> "whip-habit-reminder-$entityId"
    ReminderDomain.Goal -> "whip-goal-reminder-$entityId"
}

private fun Long.saturatedPlus(other: Long): Long =
    if (this > Long.MAX_VALUE - other) Long.MAX_VALUE else this + other

private const val REMINDER_EXACT_ALARM_ACTION = "com.whip.app.action.EXACT_REMINDER"
private const val REMINDER_ALARM_IDENTITY_KEY = "reminder_alarm_identity"
internal const val REMINDER_WORK_FALLBACK_GRACE_MILLIS = 2 * 60_000L

private const val EXTRA_DOMAIN = "alarm_domain"
private const val EXTRA_ENTITY_ID = "alarm_entity_id"
private const val EXTRA_WORK_NAME = "alarm_work_name"
private const val EXTRA_USER_DATA_GENERATION = "alarm_user_data_generation"
private const val EXTRA_OFFSET_MINUTES = "alarm_offset_minutes"
private const val EXTRA_CLAIM_VERSION = "alarm_claim_version"
private const val EXTRA_CLAIM_KIND = "alarm_claim_kind"
private const val EXTRA_STABLE_ENTITY_ID = "alarm_stable_entity_id"
private const val EXTRA_LOGICAL_EPOCH_DAY = "alarm_logical_epoch_day"
private const val EXTRA_EXPECTED_TRIGGER = "alarm_expected_trigger"
private const val EXTRA_DEFINITION_FINGERPRINT = "alarm_definition_fingerprint"
