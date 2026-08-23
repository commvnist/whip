package com.whip.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun taskReminderInstant(
    scheduledDate: LocalDate,
    timeMinutes: Int,
    offsetMinutes: Int,
    zoneId: ZoneId,
): Instant {
    require(timeMinutes in 0..1439) { "Task time must be within one day" }
    require(offsetMinutes in 0..43_200) { "Reminder offset must be 0–43,200 minutes" }
    return scheduledDate
        .atTime(timeMinutes / 60, timeMinutes % 60)
        .atZone(zoneId)
        .toInstant()
        .minusSeconds(offsetMinutes * 60L)
}
