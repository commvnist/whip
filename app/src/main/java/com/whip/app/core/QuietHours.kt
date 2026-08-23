package com.whip.app.core

import java.time.Instant
import java.time.ZoneId

/** Moves an instant to the end of quiet hours when it falls inside that window. */
fun adjustForQuietHours(
    instant: Instant,
    zoneId: ZoneId,
    quietStartMinutes: Int?,
    quietEndMinutes: Int?,
): Instant {
    val start = quietStartMinutes ?: return instant
    val end = quietEndMinutes ?: return instant
    if (start == end) return instant
    val local = instant.atZone(zoneId)
    val minute = local.hour * 60 + local.minute
    val inQuietHours = if (start < end) minute in start until end else minute >= start || minute < end
    if (!inQuietHours) return instant
    val endDate = if (start > end && minute >= start) local.toLocalDate().plusDays(1) else local.toLocalDate()
    return endDate.atTime(end / 60, end % 60).atZone(zoneId).toInstant()
}
