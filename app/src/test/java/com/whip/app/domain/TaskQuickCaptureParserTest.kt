package com.whip.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        val assumption = parsed.assumptions.single()
        assertEquals(TaskCaptureAssumptionKind.Schedule, assumption.kind)
        assertEquals("tomorrow", assumption.sourceText)
        assertEquals("tomorrow", "Buy groceries tomorrow".substring(assumption.start, assumption.endExclusive))
        assertEquals("Schedule · Tomorrow → 2026-08-19", assumption.interpretation)
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
        assertEquals(
            listOf(
                TaskCaptureAssumptionKind.Repeat,
                TaskCaptureAssumptionKind.Schedule,
                TaskCaptureAssumptionKind.Deadline,
            ),
            parsed.assumptions.map(TaskCaptureAssumption::kind),
        )
    }

    @Test
    fun parsesSelectedWeekdays() {
        val parsed = TaskQuickCaptureParser.parse("Train every Monday, Wednesday and Friday", today)
        assertEquals(
            setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            parsed.recurrence?.weekdays,
        )
        assertTrue(parsed.recognized.isNotEmpty())
        assertEquals("Train", parsed.title)
        assertEquals("every Monday, Wednesday and Friday", parsed.assumptions.single().sourceText)
        assertEquals("Repeat · Monday, Wednesday, Friday", parsed.assumptions.single().interpretation)
    }

    @Test
    fun nextWeekdayAlwaysMeansTheFollowingWeekday() {
        val parsed = TaskQuickCaptureParser.parse("Review proposal next Tuesday", today)

        assertEquals("Review proposal", parsed.title)
        assertEquals(LocalDate.of(2026, 8, 25), parsed.date)
        assertEquals("Schedule · Next Tuesday → 2026-08-25", parsed.recognized.single())
    }

    @Test
    fun deadlineOnlyCreatesAOneTimeTaskOnThatDeadline() {
        val parsed = TaskQuickCaptureParser.parse("File taxes deadline 2026-09-05", today)

        assertEquals("File taxes", parsed.title)
        assertEquals(ScheduleKind.Once, parsed.scheduleKind)
        assertEquals(LocalDate.of(2026, 9, 5), parsed.date)
        assertEquals(LocalDate.of(2026, 9, 5), parsed.deadline)
        assertEquals(TaskCaptureAssumptionKind.Deadline, parsed.assumptions.single().kind)
    }

    @Test
    fun invalidDatesAndIntervalsRemainLiteralAndUnhighlighted() {
        val capture = "Review today’s plan on 2026-02-30 every 0 days"
        val parsed = TaskQuickCaptureParser.parse(capture, today)

        assertEquals(capture, parsed.title)
        assertEquals(ScheduleKind.Anytime, parsed.scheduleKind)
        assertNull(parsed.date)
        assertNull(parsed.recurrence)
        assertTrue(parsed.assumptions.isEmpty())
    }

    @Test
    fun onlyTheFirstCompetingSchedulePhraseIsAssumed() {
        val parsed = TaskQuickCaptureParser.parse("Call tomorrow or next Friday", today)

        assertEquals(today.plusDays(1), parsed.date)
        assertEquals(listOf("tomorrow"), parsed.assumptions.map(TaskCaptureAssumption::sourceText))
        assertEquals("Call or next Friday", parsed.title)
    }

    @Test
    fun deadlineBeforeTheScheduleIsLeftLiteralInsteadOfApplyingAnInvalidAssumption() {
        val parsed = TaskQuickCaptureParser.parse(
            "Prepare launch on 2026-09-10 deadline 2026-09-01",
            today,
        )

        assertEquals(LocalDate.of(2026, 9, 10), parsed.date)
        assertNull(parsed.deadline)
        assertEquals(listOf(TaskCaptureAssumptionKind.Schedule), parsed.assumptions.map { it.kind })
        assertEquals("Prepare launch deadline 2026-09-01", parsed.title)
    }

    @Test
    fun surroundingWhitespaceAndPunctuationDoNotCorruptHighlightOffsets() {
        val capture = "  Plan launch — on 2026-09-01, every 2 weeks  "
        val parsed = TaskQuickCaptureParser.parse(capture, today)

        assertEquals("Plan launch", parsed.title)
        assertEquals(
            listOf("on 2026-09-01", "every 2 weeks"),
            parsed.assumptions.map { capture.substring(it.start, it.endExclusive) },
        )
    }
}
