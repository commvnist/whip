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
 * Android can invalidate WorkManager's wall-clock-derived delays without a
 * Whip settings edit. The application recovery gate is already established
 * before this receiver runs; blocked recovery therefore fails closed.
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
