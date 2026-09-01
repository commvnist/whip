package com.whip.app.reminders

import androidx.work.Data
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReferenceArray
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal const val REMINDER_DELIVERY_CLAIM_VERSION = 1

internal enum class ReminderDeliveryKind { Scheduled, Snoozed }

internal enum class ReminderDomain { Task, Habit, Goal }

/**
 * A queued reminder is an untrusted claim about one exact current delivery.
 * Stable identity, semantic fingerprint, trigger, kind, and schema version are
 * all required; legacy or malformed work fails closed and is reconciled.
 */
internal data class ReminderDeliveryClaim(
    val version: Int = REMINDER_DELIVERY_CLAIM_VERSION,
    val kind: ReminderDeliveryKind,
    val stableEntityId: String,
    val logicalEpochDay: Long,
    val expectedTriggerAtMillis: Long,
    val definitionFingerprint: String,
) {
    fun isStructurallyValid(): Boolean =
        version == REMINDER_DELIVERY_CLAIM_VERSION &&
            stableEntityId.isNotBlank() &&
            logicalEpochDay != Long.MIN_VALUE &&
            runCatching { LocalDate.ofEpochDay(logicalEpochDay) }.isSuccess &&
            expectedTriggerAtMillis >= 0L &&
            definitionFingerprint.isNotBlank()
}

internal fun Data.Builder.putReminderDeliveryClaim(claim: ReminderDeliveryClaim): Data.Builder =
    putInt(REMINDER_CLAIM_VERSION_KEY, claim.version)
        .putString(REMINDER_CLAIM_KIND_KEY, claim.kind.name)
        .putString(REMINDER_CLAIM_STABLE_ID_KEY, claim.stableEntityId)
        .putLong(REMINDER_CLAIM_LOGICAL_DAY_KEY, claim.logicalEpochDay)
        .putLong(REMINDER_CLAIM_TRIGGER_KEY, claim.expectedTriggerAtMillis)
        .putString(REMINDER_CLAIM_FINGERPRINT_KEY, claim.definitionFingerprint)

internal fun Data.reminderDeliveryClaimOrNull(): ReminderDeliveryClaim? {
    val claim = ReminderDeliveryClaim(
        version = getInt(REMINDER_CLAIM_VERSION_KEY, -1),
        kind = getString(REMINDER_CLAIM_KIND_KEY)
            ?.let { runCatching { ReminderDeliveryKind.valueOf(it) }.getOrNull() }
            ?: return null,
        stableEntityId = getString(REMINDER_CLAIM_STABLE_ID_KEY).orEmpty(),
        logicalEpochDay = getLong(REMINDER_CLAIM_LOGICAL_DAY_KEY, Long.MIN_VALUE),
        expectedTriggerAtMillis = getLong(REMINDER_CLAIM_TRIGGER_KEY, -1L),
        definitionFingerprint = getString(REMINDER_CLAIM_FINGERPRINT_KEY).orEmpty(),
    )
    return claim.takeIf(ReminderDeliveryClaim::isStructurallyValid)
}

internal fun reminderClaimIsForToday(
    claim: ReminderDeliveryClaim,
    today: LocalDate,
    zoneId: ZoneId,
): Boolean = Instant.ofEpochMilli(claim.expectedTriggerAtMillis)
    .atZone(zoneId)
    .toLocalDate() == today

internal fun reminderSemanticFingerprint(vararg parts: Any?): String {
    val canonical = parts.joinToString(separator = "\u001f") { part ->
        part?.toString()?.let { "${it.length}:$it" } ?: "-"
    }
    return MessageDigest.getInstance("SHA-256")
        .digest(canonical.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
}

/**
 * Striped locks serialize each entity's cancel/enqueue and resolve/post work
 * without turning independent reminders into one global queue.
 */
internal class ReminderDeliveryCoordinator(stripeCount: Int = 64) {
    private val stripes = AtomicReferenceArray<Mutex>(stripeCount.coerceAtLeast(1)).apply {
        for (index in 0 until length()) set(index, Mutex())
    }
    private val stateBoundary = Mutex()

    suspend fun <T> withEntity(
        domain: ReminderDomain,
        entityId: Long,
        block: suspend () -> T,
    ): T {
        val mixed = 31 * domain.ordinal + entityId.hashCode()
        val index = (mixed and Int.MAX_VALUE) % stripes.length()
        return stripes.get(index).withLock { block() }
    }

    /**
     * Linearizes one complete production mutation or one resolve/post decision.
     * This boundary is intentionally non-reentrant: composition roots must pass
     * raw delegates to an outer owner instead of relying on coroutine context
     * identity, which Room changes while entering a transaction.
     * Code that needs both locks acquires the entity stripe first and calls a
     * `*FromCoordinator` scheduler method; state owners must never acquire a
     * stripe, which keeps the lock order acyclic.
     */
    suspend fun <T> withStateBoundary(block: suspend () -> T): T =
        stateBoundary.withLock { block() }
}

private const val REMINDER_CLAIM_VERSION_KEY = "reminder_claim_version"
private const val REMINDER_CLAIM_KIND_KEY = "reminder_claim_kind"
private const val REMINDER_CLAIM_STABLE_ID_KEY = "reminder_claim_stable_id"
private const val REMINDER_CLAIM_LOGICAL_DAY_KEY = "reminder_claim_logical_day"
private const val REMINDER_CLAIM_TRIGGER_KEY = "reminder_claim_trigger"
private const val REMINDER_CLAIM_FINGERPRINT_KEY = "reminder_claim_fingerprint"
