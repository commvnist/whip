package com.whip.app.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitRulesTest {
    private val monday = LocalDate.of(2026, 8, 17)

    @Test
    fun allTargetComparisonsHandleThresholds() {
        assertTrue(habit(TargetComparison.AtLeast, 8.0).targetSatisfied(8.0)!!)
        assertFalse(habit(TargetComparison.AtLeast, 8.0).targetSatisfied(7.0)!!)
        assertTrue(habit(TargetComparison.AtMost, 2.0).targetSatisfied(1.0)!!)
        assertTrue(habit(TargetComparison.Exactly, 3.0).targetSatisfied(3.0)!!)
        assertTrue(habit(TargetComparison.WithinRange, 3.0, 5.0).targetSatisfied(4.0)!!)
        assertNull(habit(TargetComparison.None, null).targetSatisfied(4.0))
    }

    @Test
    fun missingCheckInNeverInventsSuccessOrFailure() {
        val limit = habit(TargetComparison.AtMost, 0.0)

        assertNull(limit.outcomeForPeriod(emptyList(), monday))
    }

    @Test
    fun dailyEveryNDaysAndWeekdaysScheduleCorrectly() {
        assertTrue(habit().isScheduledOn(monday.plusDays(1)))
        assertTrue(habit(schedule = HabitScheduleType.EveryNDays, interval = 3).isScheduledOn(monday.plusDays(3)))
        assertFalse(habit(schedule = HabitScheduleType.EveryNDays, interval = 3).isScheduledOn(monday.plusDays(2)))
        assertTrue(habit(schedule = HabitScheduleType.SelectedWeekdays, weekdays = setOf(DayOfWeek.WEDNESDAY)).isScheduledOn(monday.plusDays(2)))
    }

    @Test
    fun staleEndDateDoesNotSuppressNeverEndingHabitAndOnDateIsInclusive() {
        val stale = habit().copy(endType = HabitEndType.Never, endDate = monday)
        assertTrue(stale.isScheduledOn(monday.plusDays(10)))

        val dated = habit().copy(endType = HabitEndType.OnDate, endDate = monday.plusDays(2))
        assertTrue(dated.isScheduledOn(monday.plusDays(2)))
        assertFalse(dated.isScheduledOn(monday.plusDays(3)))
    }

    @Test
    fun thresholdEndingsStayEndedAndSuppressReminders() {
        val successes = listOf(
            log(1, monday, HabitLogStatus.Success),
            log(2, monday.plusDays(1), HabitLogStatus.Success),
        )
        val byCompletions = habit().copy(endType = HabitEndType.AfterCompletions, endValue = 2.0)
        assertTrue(byCompletions.hasEnded(successes, monday.plusDays(1)))
        assertFalse(byCompletions.reminderNeededOn(successes, monday.plusDays(2)))

        val byStreak = habit().copy(endType = HabitEndType.AfterStreak, endValue = 2.0)
        assertTrue(byStreak.hasEnded(successes, monday.plusDays(1)))
        assertTrue(byStreak.hasEnded(successes, monday.plusDays(5)))

        val totalLogs = listOf(
            log(3, monday, HabitLogStatus.Recorded).copy(value = 2.0, canonicalValue = 2.0),
            log(4, monday.plusDays(1), HabitLogStatus.Recorded).copy(value = 3.0, canonicalValue = 3.0),
        )
        val byTotal = habit().copy(endType = HabitEndType.AfterTotal, endValue = 5.0)
        assertTrue(byTotal.hasEnded(totalLogs, monday.plusDays(1)))
    }

    @Test
    fun flexibleWeeklyStopsBeingDueAfterTargetSuccesses() {
        val habit = habit(schedule = HabitScheduleType.FlexibleTimesPerWeek, flexible = 3)
        assertTrue(habit.isScheduledOn(monday, weekSuccesses = 2))
        assertFalse(habit.isScheduledOn(monday, weekSuccesses = 3))
    }

    @Test
    fun flexibleWeeklyProgressCountsEachSuccessfulAutomationEvent() {
        val habit = habit(schedule = HabitScheduleType.FlexibleTimesPerWeek, flexible = 4)
        val logs = listOf(
            log(1, monday, HabitLogStatus.Success),
            log(2, monday.plusDays(1), HabitLogStatus.Success),
            log(3, monday.plusDays(1), HabitLogStatus.Success),
            log(4, monday.plusDays(3), HabitLogStatus.Recorded),
            log(5, monday.plusDays(4), HabitLogStatus.Failed),
            log(6, monday.minusDays(1), HabitLogStatus.Success),
        )

        val progress = requireNotNull(habit.flexibleProgress(logs, monday.plusDays(4)))

        assertEquals(4, progress.completed)
        assertEquals(4, progress.target)
        assertFalse(habit.isScheduledOn(monday.plusDays(4), weekSuccesses = progress.completed))
    }

    @Test
    fun partialNumericLogsAreOneOutcomeOnlyAfterTheTargetIsReached() {
        val hydration = habit(min = 8.0)
        val partial = (1L..6L).map { id -> log(id, monday, HabitLogStatus.Recorded) }

        assertFalse(requireNotNull(hydration.outcomeForPeriod(partial, monday)))
        assertEquals(6.0, hydration.valueForPeriod(partial, monday), 0.0)

        val complete = partial + log(7, monday, HabitLogStatus.Recorded).copy(value = 2.0, canonicalValue = 2.0)
        assertTrue(requireNotNull(hydration.outcomeForPeriod(complete, monday)))
        assertEquals(8.0, hydration.valueForPeriod(complete, monday), 0.0)
        assertFalse(hydration.reminderNeededOn(complete, monday))
        assertTrue(hydration.reminderNeededOn(partial, monday))
    }

    @Test
    fun flexibleWeeklyStreakUsesCompletedPeriodsAndDoesNotPunishAnOpenWeek() {
        val flexible = habit(schedule = HabitScheduleType.FlexibleTimesPerWeek, flexible = 3)
            .copy(startDate = monday.minusDays(14))
        val previousWeek = listOf(
            log(1, monday.minusDays(7), HabitLogStatus.Success),
            log(2, monday.minusDays(6), HabitLogStatus.Success),
            log(3, monday.minusDays(5), HabitLogStatus.Success),
        )
        val unfinishedCurrentWeek = previousWeek + listOf(
            log(4, monday, HabitLogStatus.Success),
            log(5, monday.plusDays(1), HabitLogStatus.Success),
        )

        assertEquals(1, flexible.flexiblePeriodStreak(unfinishedCurrentWeek, monday.plusDays(2)))

        val finishedCurrentWeek = unfinishedCurrentWeek + log(6, monday.plusDays(2), HabitLogStatus.Success)
        assertEquals(2, flexible.flexiblePeriodStreak(finishedCurrentWeek, monday.plusDays(2)))
        assertEquals(2.0 / 3.0, flexible.completionRateOverRecentPeriods(finishedCurrentWeek, monday.plusDays(2)), 0.0001)
    }

    @Test
    fun flexiblePeriodsHonorEveryWeekStartAndArbitraryCompletionDays() {
        DayOfWeek.entries.forEach { firstDay ->
            val start = monday.with(java.time.temporal.TemporalAdjusters.previousOrSame(firstDay))
            val flexible = habit(schedule = HabitScheduleType.FlexibleTimesPerWeek, flexible = 3)
                .copy(startDate = start.minusWeeks(1), weekStart = firstDay)
            val logs = listOf(
                log(1, start.plusDays(1), HabitLogStatus.Success),
                log(2, start.plusDays(3), HabitLogStatus.Success),
                log(3, start.plusDays(6), HabitLogStatus.Success),
            )
            assertEquals("week starts on $firstDay", 3, flexible.flexibleProgress(logs, start.plusDays(4))?.completed)
            assertEquals("week starts on $firstDay", 1, flexible.flexiblePeriodStreak(logs, start.plusDays(6)))
        }
    }

    @Test
    fun flexibleMonthlyUsesCalendarBoundaryAndOneOutcomeOnTargetReachingDay() {
        val monthly = habit(schedule = HabitScheduleType.FlexibleTimesPerMonth, flexible = 3)
            .copy(startDate = LocalDate.of(2026, 7, 1))
        val logs = listOf(
            log(1, LocalDate.of(2026, 7, 2), HabitLogStatus.Success),
            log(2, LocalDate.of(2026, 7, 19), HabitLogStatus.Success),
            log(3, LocalDate.of(2026, 7, 31), HabitLogStatus.Success),
            log(4, LocalDate.of(2026, 8, 1), HabitLogStatus.Success),
        )

        assertEquals(3, monthly.flexibleProgress(logs, LocalDate.of(2026, 7, 15))?.completed)
        assertEquals(1, monthly.flexibleProgress(logs, LocalDate.of(2026, 8, 15))?.completed)
        assertEquals(
            setOf(LocalDate.of(2026, 7, 31)),
            monthly.successfulPeriodOutcomeDates(logs, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)),
        )
    }

    @Test
    fun aPauseIsNeutralAndOnlyReducesAnImpossibleFlexibleTarget() {
        val flexible = habit(schedule = HabitScheduleType.FlexibleTimesPerWeek, flexible = 3)
        val pause = HabitPause(1, flexible.id, monday, monday.plusDays(5), "Travel")
        val logs = listOf(
            log(3, monday.plusDays(6), HabitLogStatus.Success),
        )

        val progress = requireNotNull(flexible.flexibleProgress(logs, monday.plusDays(6), listOf(pause)))
        assertEquals(1, progress.target)
        assertEquals(1, progress.completed)
        assertEquals(1.0, flexible.completionRateOverRecentPeriods(logs, monday.plusDays(6), pauses = listOf(pause)), 0.0)
    }

    @Test
    fun skipIsVisibleNeutralAndSuppressesReminderWithoutBecomingALog() {
        val daily = habit()
        val skip = HabitSkip("skip-1", daily.id, monday, 1, 1, 1)

        assertTrue(daily.isNeutralDate(monday, skips = listOf(skip)))
        assertEquals(HabitDayState.Skipped, daily.dayStateOn(monday, monday, emptyList(), skips = listOf(skip)))
        assertFalse(daily.reminderNeededOn(emptyList(), monday, skips = listOf(skip)))
        assertTrue(emptyList<HabitLog>().isEmpty())
    }

    @Test
    fun pastUnloggedDayIsMissedButPendingTodayCarriesThePriorStreak() {
        val daily = habit().copy(startDate = monday.minusDays(2))
        val yesterday = monday.minusDays(1)
        val logs = listOf(log(1, yesterday, HabitLogStatus.Success))

        assertEquals(HabitDayState.Missed, daily.dayStateOn(monday.minusDays(2), monday, logs))
        assertEquals(HabitDayState.Pending, daily.dayStateOn(monday, monday, logs))
        assertEquals(1, habitStreak(daily, monday, mapOf(monday to null, yesterday to true)))
        assertEquals(0.5, daily.completionRateOverRecentPeriods(logs, monday, lookbackDays = 3), 0.0)
    }

    @Test
    fun localDateOutcomesRemainStableAcrossDstOffsetChanges() {
        val flexible = habit(schedule = HabitScheduleType.FlexibleTimesPerWeek, flexible = 3)
        val dstLogs = listOf(
            log(1, monday, HabitLogStatus.Success).copy(offsetSeconds = -18_000),
            log(2, monday.plusDays(1), HabitLogStatus.Success).copy(offsetSeconds = -14_400),
            log(3, monday.plusDays(2), HabitLogStatus.Success).copy(offsetSeconds = -14_400),
        )
        assertEquals(3, flexible.flexibleProgress(dstLogs, monday.plusDays(2))?.completed)
        assertEquals(setOf(monday.plusDays(2)), flexible.successfulPeriodOutcomeDates(dstLogs, monday, monday.plusDays(6)))
    }

    @Test
    fun skippedDatesDoNotInventAStreak() {
        val habit = habit()
        assertEquals(2, habitStreak(habit, monday.plusDays(2), mapOf(
            monday.plusDays(2) to true,
            monday.plusDays(1) to true,
            monday to null,
        )))
    }

    @Test
    fun dailyWeeklyMonthlyAndRollingWindowsHaveStableBounds() {
        assertEquals(monday..monday, habit().periodBounds(monday))
        assertEquals(
            monday..monday.plusDays(6),
            habit().copy(targetPeriod = TargetPeriod.Week).periodBounds(monday.plusDays(3)),
        )
        assertEquals(
            LocalDate.of(2026, 8, 1)..LocalDate.of(2026, 8, 31),
            habit().copy(targetPeriod = TargetPeriod.Month).periodBounds(monday),
        )
        assertEquals(
            monday.minusDays(6)..monday,
            habit().copy(targetPeriod = TargetPeriod.RollingDays, rollingDays = 7).periodBounds(monday),
        )
    }

    private fun habit(
        comparison: TargetComparison = TargetComparison.AtLeast,
        min: Double? = 1.0,
        max: Double? = null,
        schedule: HabitScheduleType = HabitScheduleType.Daily,
        interval: Int = 1,
        weekdays: Set<DayOfWeek> = emptySet(),
        flexible: Int? = null,
    ) = Habit(
        id = 1, uuid = "habit", metricId = "metric", name = "Habit", notes = "", area = "", tags = emptyList(), icon = "✓",
        trackingMode = HabitTrackingMode.Count,
        dimension = UnitDimension.Count, unitId = "count", precision = 0,
        comparison = comparison, targetMin = min, targetMax = max, targetPeriod = TargetPeriod.Day,
        rollingDays = null, scheduleType = schedule, scheduleInterval = interval, weekdays = weekdays,
        flexibleTimesPerWeek = flexible, startDate = monday, endType = HabitEndType.Never,
        endDate = null, endValue = null,
        quickIncrement = 1.0, quickActions = emptyList(), reminderMinutes = emptyList(),
        weekdayReminderMinutes = emptyMap(),
        weekStart = DayOfWeek.MONDAY, timerStartedAtMillis = null, pinned = false,
        position = 0, archived = false, paused = false,
        createdAtMillis = 1, updatedAtMillis = 1,
    )

    private fun log(id: Long, date: LocalDate, status: HabitLogStatus) = HabitLog(
        id = id,
        uuid = "log-$id",
        habitId = 1,
        value = 1.0,
        canonicalValue = 1.0,
        enteredUnitId = "count",
        status = status,
        timestamp = Instant.parse("2026-08-17T12:00:00Z").plusSeconds(id),
        localDate = date,
        zoneId = "UTC",
        offsetSeconds = 0,
        note = "",
        sourceType = MetricSourceType.Workout,
        sourceId = "workout-$id",
        metricEntryId = "entry-$id",
        createdAtMillis = id,
        updatedAtMillis = id,
    )
}
