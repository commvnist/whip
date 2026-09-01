package com.whip.app.reminders

import android.content.Context

/**
 * Durable bridge for a deletion that spans Room and NotificationManager.
 * Entries are written before the database transaction and removed only after
 * committed deletion cleanup (or a confirmed rollback) completes.
 */
internal class ReminderDeletionCleanupStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun prepare(domain: ReminderDomain, entityIds: Set<Long>) {
        synchronized(updateLock) {
            update { current -> current + entityIds.validKeys(domain) }
        }
    }

    fun clear(domain: ReminderDomain, entityIds: Set<Long>) {
        synchronized(updateLock) {
            update { current -> current - entityIds.validKeys(domain) }
        }
    }

    fun pending(): Set<PendingReminderDeletion> = synchronized(updateLock) {
        preferences
            .getStringSet(PENDING_KEYS, emptySet())
            .orEmpty()
            .mapNotNullTo(linkedSetOf(), ::parsePendingReminderDeletion)
    }

    private fun update(transform: (Set<String>) -> Set<String>) {
        val current = preferences.getStringSet(PENDING_KEYS, emptySet()).orEmpty().toSet()
        check(preferences.edit().putStringSet(PENDING_KEYS, transform(current)).commit()) {
            "Could not persist reminder deletion cleanup"
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "reminder-deletion-cleanup"
        const val PENDING_KEYS = "pending"
        val updateLock = Any()
    }
}

internal data class PendingReminderDeletion(
    val domain: ReminderDomain,
    val entityId: Long,
)

private fun Set<Long>.validKeys(domain: ReminderDomain): Set<String> =
    filterTo(linkedSetOf()) { it > 0L }.mapTo(linkedSetOf()) { "${domain.name}:$it" }

private fun parsePendingReminderDeletion(value: String): PendingReminderDeletion? {
    val separator = value.indexOf(':')
    if (separator <= 0 || separator == value.lastIndex) return null
    val domain = runCatching { ReminderDomain.valueOf(value.substring(0, separator)) }.getOrNull()
        ?: return null
    val entityId = value.substring(separator + 1).toLongOrNull()?.takeIf { it > 0L } ?: return null
    return PendingReminderDeletion(domain, entityId)
}
