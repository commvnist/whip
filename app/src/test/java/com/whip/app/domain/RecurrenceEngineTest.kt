package com.whip.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class RecurrenceEngineTest {
    private val monday = LocalDate.of(2026, 8, 17)

    @Test
    fun everyDay_includesEveryDate() {
        val rule = RecurrenceRule(RecurrenceUnit.Days, startDate = monday)

        assertEquals(
            listOf(monday, monday.plusDays(1), monday.plusDays(2)),
            RecurrenceEngine.occurrencesBetween(rule, monday, monday.plusDays(2)),
        )
    }

    @Test
    fun everyThreeDays_staysAnchoredToStart() {
        val rule = RecurrenceRule(
            unit = RecurrenceUnit.Days,
            interval = 3,
            startDate = monday,
        )

        assertEquals(
            listOf(monday, monday.plusDays(3), monday.plusDays(6)),
            RecurrenceEngine.occurrencesBetween(rule, monday, monday.plusDays(8)),
        )
    }

    @Test
    fun selectedWeekdays_usesOnlyChosenDays() {
        val rule = RecurrenceRule(
            unit = RecurrenceUnit.Weeks,
            weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            startDate = monday,
        )

        assertEquals(
            listOf(monday, monday.plusDays(2), monday.plusDays(4), monday.plusDays(7)),
            RecurrenceEngine.occurrencesBetween(rule, monday, monday.plusDays(7)),
        )
    }

    @Test
    fun everyTwoWeeks_skipsAlternatingWeeks() {
        val rule = RecurrenceRule(
            unit = RecurrenceUnit.Weeks,
            interval = 2,
            weekdays = setOf(DayOfWeek.TUESDAY),
            startDate = monday,
        )

        assertEquals(
            listOf(monday.plusDays(1), monday.plusDays(15)),
            RecurrenceEngine.occurrencesBetween(rule, monday, monday.plusDays(21)),
        )
    }

    @Test
    fun afterCount_limitsWholeSeriesEvenForLaterQuery() {
        val rule = RecurrenceRule(
            unit = RecurrenceUnit.Days,
            startDate = monday,
            end = RecurrenceEnd.AfterCount,
            occurrenceCount = 3,
        )

        assertEquals(
            listOf(monday.plusDays(1), monday.plusDays(2)),
            RecurrenceEngine.occurrencesBetween(rule, monday.plusDays(1), monday.plusDays(10)),
        )
    }

    @Test
    fun onDate_includesEndDate() {
        val rule = RecurrenceRule(
            unit = RecurrenceUnit.Days,
            startDate = monday,
            end = RecurrenceEnd.OnDate,
            endDate = monday.plusDays(2),
        )

        assertEquals(
            listOf(monday, monday.plusDays(1), monday.plusDays(2)),
            RecurrenceEngine.occurrencesBetween(rule, monday, monday.plusDays(20)),
        )
    }

    @Test
    fun monthlyClampsToLastDayAndYearlyHandlesLeapDay() {
        val monthly = RecurrenceRule(
            unit = RecurrenceUnit.Months,
            startDate = LocalDate.of(2026, 1, 31),
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 3, 31),
            ),
            RecurrenceEngine.occurrencesBetween(monthly, monthly.startDate, LocalDate.of(2026, 3, 31)),
        )

        val yearly = RecurrenceRule(
            unit = RecurrenceUnit.Years,
            startDate = LocalDate.of(2024, 2, 29),
        )
        assertEquals(
            listOf(LocalDate.of(2024, 2, 29), LocalDate.of(2025, 2, 28), LocalDate.of(2026, 2, 28)),
            RecurrenceEngine.occurrencesBetween(yearly, yearly.startDate, LocalDate.of(2026, 3, 1)),
        )
    }

    @Test
    fun completionRelativeCadenceHonorsEndCount() {
        val rule = RecurrenceRule(
            unit = RecurrenceUnit.Months,
            interval = 2,
            startDate = monday,
            end = RecurrenceEnd.AfterCount,
            occurrenceCount = 2,
            anchor = RecurrenceAnchor.Completion,
        )
        assertEquals(monday, RecurrenceEngine.nextCompletionRelative(rule, null, 0))
        assertEquals(LocalDate.of(2026, 12, 20), RecurrenceEngine.nextCompletionRelative(rule, LocalDate.of(2026, 10, 20), 1))
        assertEquals(null, RecurrenceEngine.nextCompletionRelative(rule, LocalDate.of(2026, 12, 20), 2))
    }

    @Test
    fun distantQueriesStayAnchoredWithoutScanningFromSeriesStart() {
        val daily = RecurrenceRule(RecurrenceUnit.Days, interval = 3, startDate = LocalDate.of(2000, 1, 1))
        val query = LocalDate.of(2026, 8, 17)
        val expected = generateSequence(daily.startDate) { it.plusDays(3) }.first { !it.isBefore(query) }
        assertEquals(listOf(expected), RecurrenceEngine.occurrencesBetween(daily, query, expected))
        assertEquals(expected, RecurrenceEngine.nextOccurrence(daily, query))
        assertEquals(expected.minusDays(3), RecurrenceEngine.previousOccurrence(daily, expected.minusDays(1)))

        val weekdays = RecurrenceRule(
            RecurrenceUnit.Weeks,
            interval = 2,
            weekdays = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
            startDate = LocalDate.of(2000, 1, 3),
        )
        val august = RecurrenceEngine.occurrencesBetween(weekdays, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31))
        assertEquals(listOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), august.map { it.dayOfWeek })
        assertEquals(august.last(), RecurrenceEngine.previousOccurrence(weekdays, LocalDate.of(2026, 8, 31)))
    }

    @Test
    fun laterAfterCountQueriesRespectGlobalOccurrenceIndexForEveryUnit() {
        val weekly = RecurrenceRule(
            RecurrenceUnit.Weeks,
            weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
            startDate = monday,
            end = RecurrenceEnd.AfterCount,
            occurrenceCount = 3,
        )
        assertEquals(listOf(monday.plusDays(7)), RecurrenceEngine.occurrencesBetween(weekly, monday.plusDays(3), monday.plusDays(20)))

        val monthly = RecurrenceRule(
            RecurrenceUnit.Months,
            startDate = LocalDate.of(2026, 1, 31),
            end = RecurrenceEnd.AfterCount,
            occurrenceCount = 3,
        )
        assertEquals(listOf(LocalDate.of(2026, 3, 31)), RecurrenceEngine.occurrencesBetween(monthly, LocalDate.of(2026, 3, 1), LocalDate.of(2027, 1, 1)))
    }
}
