package com.whip.app.core

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

data class HabitTimerClockReading(
    val wallMillis: Long,
    val elapsedRealtimeMillis: Long?,
    val bootId: String?,
)

interface HabitTimerClock {
    fun read(): HabitTimerClockReading
}

fun calculateHabitTimerElapsedSeconds(
    accumulatedCanonicalSeconds: Double,
    anchorWallMillis: Long?,
    anchorElapsedRealtimeMillis: Long?,
    needsReview: Boolean,
    nowWallMillis: Long,
    nowElapsedRealtimeMillis: Long,
): Double {
    val accumulated = accumulatedCanonicalSeconds.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
    val deltaSeconds = if (
        !needsReview && anchorElapsedRealtimeMillis != null &&
        nowElapsedRealtimeMillis >= anchorElapsedRealtimeMillis
    ) {
        (nowElapsedRealtimeMillis - anchorElapsedRealtimeMillis) / 1_000.0
    } else {
        val wall = anchorWallMillis ?: return accumulated
        ((nowWallMillis.toDouble() - wall.toDouble()) / 1_000.0).coerceAtLeast(0.0)
    }
    return (accumulated + deltaSeconds).takeIf(Double::isFinite)?.coerceAtLeast(0.0) ?: accumulated
}

/**
 * Timer elapsed time must not change when the user, network, or time zone changes the wall clock.
 * BOOT_COUNT makes persisted monotonic readings explicitly invalid after a reboot.
 */
class AndroidHabitTimerClock(
    context: Context,
    private val wallClock: WhipClock,
) : HabitTimerClock {
    private val resolver = context.applicationContext.contentResolver

    override fun read(): HabitTimerClockReading = HabitTimerClockReading(
        wallMillis = wallClock.now().toEpochMilli(),
        elapsedRealtimeMillis = SystemClock.elapsedRealtime(),
        bootId = runCatching {
            Settings.Global.getInt(resolver, Settings.Global.BOOT_COUNT).toString()
        }.getOrNull(),
    )
}

/** Conservative default for tests and non-Android composition: elapsed time requires review. */
class WallOnlyHabitTimerClock(private val wallClock: WhipClock) : HabitTimerClock {
    override fun read() = HabitTimerClockReading(
        wallMillis = wallClock.now().toEpochMilli(),
        elapsedRealtimeMillis = null,
        bootId = null,
    )
}
