package com.whip.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.whip.app.WhipApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Android can invalidate persisted reminder timing without a Whip settings
 * edit. Reboot also clears exact alarms, while a permission grant or package
 * replacement requires their authoritative rebuild. The application recovery
 * gate is established before this receiver runs; blocked recovery fails closed.
 */
class ReminderTimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in REMINDER_TIME_INVALIDATION_ACTIONS) return
        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                routeReminderTimeInvalidation(context, action)
            } catch (error: Throwable) {
                Log.e(LOG_TAG, "Could not reconcile reminders after $action", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val LOG_TAG = "WhipReminderTime"
        val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}

internal val REMINDER_TIME_INVALIDATION_ACTIONS = setOf(
    ACTION_DEVICE_DATE_CHANGED,
    ACTION_DEVICE_TIME_CHANGED,
    ACTION_DEVICE_TIME_ZONE_CHANGED,
    ACTION_DEVICE_BOOT_COMPLETED,
    ACTION_PACKAGE_REPLACED,
    ACTION_EXACT_ALARM_ACCESS_CHANGED,
)

/** Shared by the manifest receiver and Android integrity tests. */
internal suspend fun routeReminderTimeInvalidation(
    context: Context,
    action: String,
): ReminderTimeInvalidationPlan? {
    if (action !in REMINDER_TIME_INVALIDATION_ACTIONS) return null
    val app = context.applicationContext as? WhipApplication ?: return null
    return app.reconcileReminderTimeInvalidation(action)
}
