package com.whip.app.ui

import com.whip.app.domain.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test

class HabitTodayProjectionTest {
    private val date = LocalDate.of(2026, 9, 30)

    @Test fun finalDailyOutcomeStaysInTodayAndHomeUntilTheNextDate() {
        for (ending in listOf(HabitEndType.AfterCompletions, HabitEndType.AfterStreak, HabitEndType.AfterTotal)) {
            val habit = habit(HabitTrackingMode.CheckOff).copy(endType = ending, endValue = 1.0)
            assertEquals(HomeHabitSummary(total = 1), state(habit, emptyList(), date).today.homeHabitSummary())
            val logs = listOf(log(habit, date))
            val completed = state(habit, logs, date)
            assertEquals(1, completed.today.size)
            assertEquals(HabitDayState.Completed, completed.today.single().dayState)
            assertEquals(HomeHabitSummary(completed = 1, total = 1), completed.today.homeHabitSummary())
            assertFalse(habit.reminderNeededOn(logs, date))
            assertTrue(state(habit, logs, date.plusDays(1)).today.isEmpty())
            assertEquals(HabitDayState.NotScheduled, state(habit, logs, date.plusDays(1)).all.single().dayState)
            assertTrue(state(habit, emptyList(), date).today.single().scheduled)
        }
    }

    @Test fun flexibleQuotaAttainmentKeepsTodayButDoesNotInventAnotherCheckInTomorrow() {
        val attainment = date.minusDays(2)
        for (schedule in listOf(HabitScheduleType.FlexibleTimesPerWeek, HabitScheduleType.FlexibleTimesPerMonth)) {
            val habit = habit(HabitTrackingMode.CheckOff).copy(scheduleType = schedule,
                flexibleTimesPerWeek = 1, startDate = attainment)
            val logs = listOf(log(habit, attainment))
            assertEquals(HomeHabitSummary(completed = 1, total = 1), state(habit, logs, attainment).today.homeHabitSummary())
            assertTrue(state(habit, logs, attainment.plusDays(1)).today.isEmpty())
        }
    }

    @Test fun openBoundedPeriodUsesOnlyEvidenceThroughTheProjectedDate() {
        val habit = habit(HabitTrackingMode.Count).copy(targetPeriod = TargetPeriod.Week,
            comparison = TargetComparison.AtMost, targetMin = null, targetMax = 2.0)
        val projected = state(habit, listOf(log(habit, date, 2.0), log(habit, date.plusDays(1), 3.0)), date)
        assertEquals(2.0, projected.today.single().value, 0.0)
        assertEquals(HabitDayState.Pending, projected.today.single().dayState)
        assertNull(projected.today.single().successful)
    }


    @Test fun discreteInsightBucketsCountEachWeekOrMonthOnlyOnce() {
        val firstWeek = date.minusDays(16)
        val weekly = habit(HabitTrackingMode.Count).copy(startDate = firstWeek, targetPeriod = TargetPeriod.Week)
        val logs = listOf(log(weekly, firstWeek.plusDays(2)))
        val twoClosedWeeks = (0L..13L).map(firstWeek::plusDays)
        assertEquals(0.5, requireNotNull(weekly.scheduledCompletionRateForDays(twoClosedWeeks, date, logs)), 0.0)
        assertEquals(1.0, requireNotNull(weekly.copy(endType = HabitEndType.AfterCompletions, endValue = 1.0)
            .scheduledCompletionRateForDays(twoClosedWeeks, date, logs)), 0.0)
        val monthly = weekly.copy(targetPeriod = TargetPeriod.Month, startDate = date.withDayOfMonth(1))
        val earned = date.withDayOfMonth(3)
        val monthLogs = listOf(log(monthly, earned))
        assertEquals(1.0, requireNotNull(monthly.scheduledCompletionRateForDays(
            (1L..7L).map { date.withDayOfMonth(it.toInt()) }, date, monthLogs)), 0.0)
        assertNull(monthly.scheduledCompletionRateForDays(
            (8L..14L).map { date.withDayOfMonth(it.toInt()) }, date, monthLogs))
        assertNull(monthly.copy(comparison = TargetComparison.AtMost, targetMin = null, targetMax = 2.0)
            .scheduledCompletionRateForDays(twoClosedWeeks, date, monthLogs))
    }

    private fun state(habit: Habit, logs: List<HabitLog>, through: LocalDate) = buildHabitUiState(
        HabitData(listOf(habit), emptyList(), logs, emptyList(), emptyList(), emptyList()), through, emptyList())

    private fun log(habit: Habit, day: LocalDate, value: Double = 1.0) = HabitLog(
        day.toEpochDay(), "log-$day", habit.id, value, value, "count", HabitLogStatus.Success,
        day.atStartOfDay(ZoneOffset.UTC).toInstant(), day, "UTC", 0, "", MeasurementSourceType.Manual,
        null, null, 1, 1)

    private fun habit(mode: HabitTrackingMode) = Habit(
        id = 1,
        uuid = "habit-1",
        measurementId = "measurement-habit-1",
        name = "Medication",
        notes = "",
        area = "Main",
        tags = emptyList(),
        icon = "💊",
        trackingMode = mode,
        dimension = UnitDimension.Count,
        unitId = "count",
        precision = 0,
        comparison = TargetComparison.AtLeast,
        targetMin = 1.0,
        targetMax = null,
        targetPeriod = TargetPeriod.Day,
        rollingDays = null,
        scheduleType = HabitScheduleType.Daily,
        scheduleInterval = 1,
        weekdays = emptySet(),
        flexibleTimesPerWeek = null,
        startDate = date,
        endType = HabitEndType.Never,
        endDate = null,
        endValue = null,
        quickIncrement = 1.0,
        quickActions = emptyList(),
        reminderMinutes = emptyList(),
        weekdayReminderMinutes = emptyMap(),
        weekStart = DayOfWeek.MONDAY,
        timerStartedAtMillis = null,
        pinned = false,
        position = 0,
        archived = false,
        paused = false,
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )

}
