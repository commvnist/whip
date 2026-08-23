package com.whip.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TaskReminderRulesTest {
    @Test
    fun offsetsCanCrossIntoPreviousDaysWithoutChangingTheWorkDate() {
        assertEquals(
            Instant.parse("2026-08-17T17:00:00Z"),
            taskReminderInstant(
                LocalDate.of(2026, 8, 18),
                9 * 60,
                20 * 60,
                ZoneId.of("America/Toronto"),
            ),
        )
    }

    @Test
    fun invalidOffsetsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            taskReminderInstant(LocalDate.of(2026, 8, 18), 540, -1, ZoneId.of("UTC"))
        }
    }
}
