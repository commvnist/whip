package com.whip.app.reminders

import android.content.Context

/** Prevents a retried PendingIntent from applying a destructive/additive action twice. */
class NotificationActionLedger(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    @Synchronized
    @android.annotation.SuppressLint("UseKtx")
    fun begin(actionId: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (actionId.isBlank()) return false
        val existing = preferences.getLong(actionId, 0L)
        if (existing > 0L) return false
        if (existing < 0L && -existing >= nowMillis - IN_FLIGHT_TIMEOUT_MILLIS) return false
        val cutoff = nowMillis - RETENTION_MILLIS
        val editor = preferences.edit().putLong(actionId, -nowMillis.coerceAtLeast(1L))
        preferences.all.forEach { (key, value) ->
            if ((value as? Long)?.let { kotlin.math.abs(it) < cutoff } == true) editor.remove(key)
        }
        return editor.commit()
    }

    @Synchronized
    @android.annotation.SuppressLint("UseKtx")
    fun complete(actionId: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (preferences.getLong(actionId, 0L) >= 0L) return false
        return preferences.edit().putLong(actionId, nowMillis.coerceAtLeast(1L)).commit()
    }

    @Synchronized
    @android.annotation.SuppressLint("UseKtx")
    fun release(actionId: String): Boolean {
        if (preferences.getLong(actionId, 0L) >= 0L) return false
        return preferences.edit().remove(actionId).commit()
    }

    /** Compatibility helper for actions that have no fallible mutation. */
    fun claim(actionId: String, nowMillis: Long = System.currentTimeMillis()): Boolean =
        begin(actionId, nowMillis) && complete(actionId, nowMillis)

    private companion object {
        const val PREFERENCES = "notification_action_receipts"
        const val RETENTION_MILLIS = 31L * 24L * 60L * 60L * 1_000L
        const val IN_FLIGHT_TIMEOUT_MILLIS = 5L * 60L * 1_000L
    }
}
