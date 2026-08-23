package com.whip.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskQuickCaptureParserTest {
    private val today = LocalDate.of(2026, 8, 18)

    @Test
    fun parsesRelativeDateWithoutSendingOrGuessingTheTitle() {
        val parsed = TaskQuickCaptureParser.parse("Buy groceries tomorrow", today)
        assertEquals("Buy groceries", parsed.title)
        assertEquals(ScheduleKind.Once, parsed.scheduleKind)
        assertEquals(today.plusDays(1), parsed.date)
    }

    @Test
    fun parsesMonthlyCadenceAndSeparateDeadline() {
        val parsed = TaskQuickCaptureParser.parse(
            "Close books every 2 months on 2026-09-01 deadline 2026-09-05",
            today,
        )
        assertEquals("Close books", parsed.title)
        assertEquals(RecurrenceUnit.Months, parsed.recurrence?.unit)
        assertEquals(2, parsed.recurrence?.interval)
        assertEquals(LocalDate.of(2026, 9, 1), parsed.date)
        assertEquals(LocalDate.of(2026, 9, 5), parsed.deadline)
    }

    @Test
    fun parsesSelectedWeekdays() {
        val parsed = TaskQuickCaptureParser.parse("Train every Monday, Wednesday and Friday", today)
        assertEquals(
            setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            parsed.recurrence?.weekdays,
        )
        assertTrue(parsed.recognized.isNotEmpty())
    }
}
