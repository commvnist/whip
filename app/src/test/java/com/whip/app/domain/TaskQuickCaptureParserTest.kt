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
    fun parsesMedicationAcronymOnTwoNamedWeekdays() {
        val parsed = TaskQuickCaptureParser.parse("TRT every Monday and Thursday", today)

        assertEquals("TRT", parsed.title)
        assertEquals(ScheduleKind.Recurring, parsed.scheduleKind)
        assertEquals(
            setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
            parsed.recurrence?.weekdays,
        )
        assertEquals("every Monday and Thursday", parsed.assumptions.single().sourceText)
        assertEquals("Repeat · Monday, Thursday", parsed.assumptions.single().interpretation)
    }

    @Test
    fun acceptsNaturalWeekdayListVariants() {
        val variants = listOf(
            "TRT every monday and thursday",
            "TRT every Mon & Thu",
            "TRT every Mon/Thu",
            "TRT every Mondays and Thursdays",
            "TRT every Monday, and Thursday",
            "TRT weekly on Tuesday + Friday",
        )

        variants.forEach { capture ->
            val parsed = TaskQuickCaptureParser.parse(capture, today)
            assertEquals(capture, "TRT", parsed.title)
            assertEquals(capture, ScheduleKind.Recurring, parsed.scheduleKind)
            assertTrue(capture, parsed.recurrence?.weekdays?.size == 2)
            assertEquals(capture, 1, parsed.assumptions.size)
        }
    }

    @Test
    fun parsesIntervalWeekdaysAndNamedDayGroups() {
        val alternating = TaskQuickCaptureParser.parse(
            "TRT every 2 weeks on Monday and Thursday",
            today,
        )
        assertEquals("TRT", alternating.title)
        assertEquals(2, alternating.recurrence?.interval)
        assertEquals(
            setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
            alternating.recurrence?.weekdays,
        )

        val weekdays = TaskQuickCaptureParser.parse("Walk every weekday", today)
        assertEquals("Walk", weekdays.title)
        assertEquals(DayOfWeek.entries.take(5).toSet(), weekdays.recurrence?.weekdays)

        val weekends = TaskQuickCaptureParser.parse("Call family every weekend", today)
        assertEquals("Call family", weekends.title)
        assertEquals(
            setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
            weekends.recurrence?.weekdays,
        )
    }

    @Test
    fun weekdayWordsWithoutARecurrenceCueRemainLiteral() {
        val capture = "TRT Monday and Thursday notes"
        val parsed = TaskQuickCaptureParser.parse(capture, today)

        assertEquals(capture, parsed.title)
        assertEquals(ScheduleKind.Anytime, parsed.scheduleKind)
        assertNull(parsed.recurrence)
        assertTrue(parsed.assumptions.isEmpty())
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

    @Test
    fun understandsUsefulOneTimeDateVariations() {
        val cases = mapOf(
            "Plan the visit on Friday" to LocalDate.of(2026, 8, 21),
            "Plan the visit this Tuesday" to LocalDate.of(2026, 8, 18),
            "Plan the visit in 3 weeks" to LocalDate.of(2026, 9, 8),
            "Plan the visit on Sep 5" to LocalDate.of(2026, 9, 5),
            "Plan the visit starting September 6, 2026" to LocalDate.of(2026, 9, 6),
        )

        cases.forEach { (capture, expected) ->
            val parsed = TaskQuickCaptureParser.parse(capture, today)
            assertEquals(capture, "Plan the visit", parsed.title)
            assertEquals(capture, ScheduleKind.Once, parsed.scheduleKind)
            assertEquals(capture, expected, parsed.date)
        }
    }

    @Test
    fun understandsNaturalDeadlineVariations() {
        val cases = mapOf(
            "Submit forms due tomorrow" to LocalDate.of(2026, 8, 19),
            "Submit forms by next Friday" to LocalDate.of(2026, 8, 21),
            "Submit forms deadline Sep 5" to LocalDate.of(2026, 9, 5),
            "Submit forms due in 2 months" to LocalDate.of(2026, 10, 18),
        )

        cases.forEach { (capture, expected) ->
            val parsed = TaskQuickCaptureParser.parse(capture, today)
            assertEquals(capture, "Submit forms", parsed.title)
            assertEquals(capture, expected, parsed.deadline)
            assertEquals(capture, expected, parsed.date)
        }
    }

    @Test
    fun understandsTwelveAndTwentyFourHourTimes() {
        val cases = mapOf(
            "Call supplier at 9am" to 9 * 60,
            "Call supplier 9:30 p.m." to 21 * 60 + 30,
            "Call supplier @14:05" to 14 * 60 + 5,
            "Call supplier at noon" to 12 * 60,
            "Call supplier at midnight" to 0,
        )

        cases.forEach { (capture, expected) ->
            val parsed = TaskQuickCaptureParser.parse(capture, today)
            assertEquals(capture, "Call supplier", parsed.title)
            assertEquals(capture, today, parsed.date)
            assertEquals(capture, expected, parsed.timeMinutes)
            assertEquals(capture, ScheduleKind.Once, parsed.scheduleKind)
        }
    }

    @Test
    fun understandsRecurrenceShortcutsAnchorsAndPluralWeekdays() {
        val daily = TaskQuickCaptureParser.parse("Stretch daily", today)
        assertEquals("Stretch", daily.title)
        assertEquals(RecurrenceUnit.Days, daily.recurrence?.unit)

        val alternate = TaskQuickCaptureParser.parse("Review plan every other week", today)
        assertEquals("Review plan", alternate.title)
        assertEquals(2, alternate.recurrence?.interval)

        val completionAnchored = TaskQuickCaptureParser.parse("Replace filter each month after completion", today)
        assertEquals(RecurrenceAnchor.Completion, completionAnchored.recurrence?.anchor)

        val pluralDays = TaskQuickCaptureParser.parse("Publish update Mondays and Thursdays", today)
        assertEquals("Publish update", pluralDays.title)
        assertEquals(
            setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
            pluralDays.recurrence?.weekdays,
        )
    }

    @Test
    fun understandsCalendarRecurrencesAndExplicitEnds() {
        val monthly = TaskQuickCaptureParser.parse("Reconcile account monthly on the 31st", today)
        assertEquals("Reconcile account", monthly.title)
        assertEquals(RecurrenceUnit.Months, monthly.recurrence?.unit)
        assertEquals(LocalDate.of(2026, 8, 31), monthly.recurrence?.startDate)

        val yearly = TaskQuickCaptureParser.parse("Renew membership yearly on September 5", today)
        assertEquals("Renew membership", yearly.title)
        assertEquals(RecurrenceUnit.Years, yearly.recurrence?.unit)
        assertEquals(LocalDate.of(2026, 9, 5), yearly.recurrence?.startDate)

        val until = TaskQuickCaptureParser.parse("Review roadmap every other week until Dec 31", today)
        assertEquals(RecurrenceEnd.OnDate, until.recurrence?.end)
        assertEquals(LocalDate.of(2026, 12, 31), until.recurrence?.endDate)

        val count = TaskQuickCaptureParser.parse("Run audit weekly for 10 occurrences", today)
        assertEquals(RecurrenceEnd.AfterCount, count.recurrence?.end)
        assertEquals(10, count.recurrence?.occurrenceCount)
    }

    @Test
    fun extractsPlanningMetadataWithoutLeavingSyntaxInTheTitle() {
        val parsed = TaskQuickCaptureParser.parse(
            "Send proposal tomorrow at 9am by next Friday !high for 45m light effort #work #calls remind me",
            today,
        )

        assertEquals("Send proposal", parsed.title)
        assertEquals(LocalDate.of(2026, 8, 19), parsed.date)
        assertEquals(9 * 60, parsed.timeMinutes)
        assertEquals(LocalDate.of(2026, 8, 21), parsed.deadline)
        assertEquals(TaskPriority.High, parsed.priority)
        assertEquals(45, parsed.durationMinutes)
        assertEquals(TaskEffort.Light, parsed.effort)
        assertEquals(setOf("work", "calls"), parsed.tags)
        assertTrue(parsed.reminderEnabled)
        assertEquals(listOf(0), parsed.reminderOffsetsMinutes)
        assertEquals(9, parsed.assumptions.size)
    }

    @Test
    fun supportsReminderOffsetsWhenAScheduledTimeExists() {
        val parsed = TaskQuickCaptureParser.parse(
            "Join planning call tomorrow at 2pm remind me 30m before",
            today,
        )

        assertEquals("Join planning call", parsed.title)
        assertEquals(14 * 60, parsed.timeMinutes)
        assertTrue(parsed.reminderEnabled)
        assertEquals(listOf(30), parsed.reminderOffsetsMinutes)
        assertEquals("Reminder · 30 min before", parsed.assumptions.last().interpretation)
    }

    @Test
    fun avoidsCommonNaturalLanguageFalsePositives() {
        listOf(
            "Weekly report outline",
            "Discuss today's plan",
            "Ask about priority seating",
            "Meet at length",
            "Remind me to choose a time",
        ).forEach { capture ->
            val parsed = TaskQuickCaptureParser.parse(capture, today)
            assertEquals(capture, capture, parsed.title)
            assertTrue(capture, parsed.assumptions.isEmpty())
        }
    }
}
