package com.whip.app.reminders

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal const val ACTION_DEVICE_DATE_CHANGED = "android.intent.action.DATE_CHANGED"
internal const val ACTION_DEVICE_TIME_CHANGED = "android.intent.action.TIME_SET"
internal const val ACTION_DEVICE_TIME_ZONE_CHANGED = "android.intent.action.TIMEZONE_CHANGED"

/**
 * Private runtime metadata only. This version is intentionally not part of the
 * user domain or portable backup: it describes whether persisted WorkManager
 * claims have been rebuilt for the code currently reading them.
 */
internal interface ReminderClaimVersionStore {
    fun read(): Int
    fun write(version: Int): Boolean
}

internal class SharedPreferencesReminderClaimVersionStore(context: Context) :
    ReminderClaimVersionStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun read(): Int = preferences.getInt(CLAIM_VERSION_KEY, VERSION_MISSING)

    @SuppressLint("UseKtx") // The KTX wrapper discards commit's durability result.
    override fun write(version: Int): Boolean = preferences.edit()
        .putInt(CLAIM_VERSION_KEY, version)
        .commit()

    private companion object {
        const val PREFERENCES_NAME = "whip_reminder_runtime"
        const val CLAIM_VERSION_KEY = "delivery_claim_schema_version"
        const val VERSION_MISSING = -1
    }
}

internal data class ReminderTimeInvalidationPlan(
    val resyncReminders: Boolean,
    val refreshWidgets: Boolean,
)

/**
 * Device-zone changes cannot affect a user-selected fixed Whip zone. A manual
 * wall-clock change affects every future WorkManager delay, while the date
 * broadcast is relevant to follow-device reminder dates and all widget dates.
 */
internal fun reminderTimeInvalidationPlan(
    action: String?,
    followsDeviceTimeZone: Boolean,
): ReminderTimeInvalidationPlan = when (action) {
    ACTION_DEVICE_TIME_CHANGED -> ReminderTimeInvalidationPlan(
        resyncReminders = true,
        refreshWidgets = true,
    )
    ACTION_DEVICE_TIME_ZONE_CHANGED -> ReminderTimeInvalidationPlan(
        resyncReminders = followsDeviceTimeZone,
        refreshWidgets = followsDeviceTimeZone,
    )
    ACTION_DEVICE_DATE_CHANGED -> ReminderTimeInvalidationPlan(
        resyncReminders = followsDeviceTimeZone,
        refreshWidgets = true,
    )
    else -> ReminderTimeInvalidationPlan(
        resyncReminders = false,
        refreshWidgets = false,
    )
}

/**
 * One serialized reminder-maintenance boundary for process startup and Android
 * time invalidations. Domain schedulers retain their per-entity locks, so a
 * concurrent normal mutation either finishes before this rebuild or is
 * reconciled by its own authoritative sync afterward.
 */
internal class ReminderRuntimeMaintenance(
    private val versionStore: ReminderClaimVersionStore,
    private val currentClaimVersion: Int = REMINDER_DELIVERY_CLAIM_VERSION,
    private val cancelVisibleConnectedReminders: () -> Unit,
    private val syncTaskReminders: suspend () -> Unit,
    private val syncHabitReminders: suspend () -> Unit,
    private val syncGoalReminders: suspend () -> Unit,
    private val refreshWidgets: () -> Unit,
) {
    private val maintenanceMutex = Mutex()

    /**
     * Returns true only when an upgrade was completed and durably recorded.
     * Missing and older markers retry from the first idempotent sync after any
     * failure; a future marker is left untouched on an app downgrade.
     */
    suspend fun upgradeDeliveryClaimsIfRequired(): Boolean = maintenanceMutex.withLock {
        if (versionStore.read() >= currentClaimVersion) return@withLock false

        cancelVisibleConnectedReminders()
        syncAllReminderDomains()
        check(versionStore.write(currentClaimVersion)) {
            "Could not persist the reminder delivery claim schema version"
        }
        true
    }

    suspend fun handleSystemTimeInvalidation(
        action: String?,
        followsDeviceTimeZone: Boolean,
    ): ReminderTimeInvalidationPlan = maintenanceMutex.withLock {
        val plan = reminderTimeInvalidationPlan(action, followsDeviceTimeZone)
        if (plan.resyncReminders) syncAllReminderDomains()
        if (plan.refreshWidgets) refreshWidgets()
        plan
    }

    private suspend fun syncAllReminderDomains() {
        // Keep this order stable: Tasks establish the common reminder channel
        // and have the broadest recurrence surface, followed by Habits and Goals.
        syncTaskReminders()
        syncHabitReminders()
        syncGoalReminders()
    }
}

/** Cancels only notifications posted on Whip's three reminder channels. */
internal fun cancelVisibleReminderNotifications(context: Context) {
    val reminderChannels = setOf(
        ReminderNotifications.CHANNEL_ID,
        HabitReminderNotifications.CHANNEL_ID,
        GoalReminderNotifications.CHANNEL_ID,
    )
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.activeNotifications
        .filter { notification -> notification.notification.channelId in reminderChannels }
        .forEach { notification -> manager.cancel(notification.tag, notification.id) }
}
