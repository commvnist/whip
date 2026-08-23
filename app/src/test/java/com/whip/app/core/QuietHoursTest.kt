package com.whip.app.core

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class QuietHoursTest {
    private val zone = ZoneId.of("America/Toronto")

    @Test fun overnightQuietHoursMoveLateAndEarlyRemindersToMorning() {
        assertEquals(
            Instant.parse("2026-08-19T11:00:00Z"),
            adjustForQuietHours(Instant.parse("2026-08-19T03:30:00Z"), zone, 22 * 60, 7 * 60),
        )
        assertEquals(
            Instant.parse("2026-08-19T11:00:00Z"),
            adjustForQuietHours(Instant.parse("2026-08-19T09:30:00Z"), zone, 22 * 60, 7 * 60),
        )
    }

    @Test fun remindersOutsideQuietHoursAndDisabledWindowsDoNotMove() {
        val instant = Instant.parse("2026-08-19T16:00:00Z")
        assertEquals(instant, adjustForQuietHours(instant, zone, 22 * 60, 7 * 60))
        assertEquals(instant, adjustForQuietHours(instant, zone, null, null))
    }
}
