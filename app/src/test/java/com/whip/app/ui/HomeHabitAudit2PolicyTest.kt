package com.whip.app.ui

import com.whip.app.core.ReviewSection
import com.whip.app.core.SavedTaskFilter
import com.whip.app.domain.*
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class HomeHabitAudit2PolicyTest {
    private val today = LocalDate.of(2026, 9, 28)
    private val zone = ZoneId.of("America/Toronto")

    @Test fun dateFiltersSeparateScheduleCompletionAndDeadlineWithSevenInclusiveDates() {
        val undated = ScheduledTask(task(1).copy(deadline = today), null, null)
        fun matches(item: ScheduledTask, mode: String, destination: String = "Inbox") =
            item.matchesTaskDateFilter(SavedTaskFilter("Test", dateMode = mode, destination = destination), today, zone)
        assertFalse(matches(undated, "Today"))
        assertTrue(matches(undated, "NoDate"))
        (-1L..7L).forEach { offset ->
            val date = today.plusDays(offset)
            val item = undated.copy(scheduledDate = date)
            assertEquals("Next7 offset $offset", offset in 0L..6L, matches(item, "Next7Days"))
        }
        val completed = undated.copy(scheduledDate = today.minusDays(20),
            completedAtMillis = Instant.parse("2026-09-29T02:00:00Z").toEpochMilli())
        assertTrue(matches(completed, "Today", "Completed"))
        assertTrue(matches(completed, "Last7Days", "Completed"))
        assertFalse(matches(completed.copy(completedAtMillis = today.minusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()), "Last7Days", "Completed"))
    }

    @Test fun upcomingResumeCountsOnlyTheDestinationSource() {
        val one = ScheduledTask(task(1), null, today)
        val two = ScheduledTask(task(2), null, today.plusDays(1))
        assertEquals(1, homeUpcomingTaskCount(TaskUiState(today = listOf(one), planning = listOf(one, two), upcoming = listOf(two, two))))
    }

    @Test fun targetlessRecordedFactsStayNeutralBesideScoredHabitsAndTimers() {
        val scored = progress(habit()).copy(successful = true, dayState = HabitDayState.Completed)
        val note = progress(habit().copy(id = 2, comparison = TargetComparison.None, trackingMode = HabitTrackingMode.LogOnly))
            .copy(status = HabitLogStatus.Recorded)
        val zeroRating = note.copy(habit = note.habit.copy(id = 3, trackingMode = HabitTrackingMode.Rating), value = 0.0)
        val timer = note.copy(habit = note.habit.copy(id = 4, timerSessionId = "running"))
        val pending = note.copy(habit = note.habit.copy(id = 5), status = null)
        val summary = listOf(scored, note, zeroRating, timer, pending).homeHabitSummary()
        assertEquals(HomeHabitSummary(completed = 1, total = 1, recordedCheckIns = 3, targetlessCheckIns = 4, timersToReview = 1), summary)
        assertEquals(2, summary.attentionCount)
    }

    @Test fun customUnitLabelsPreserveHistoricalUnitsAndCompatibleEditCapabilities() {
        val old = UnitDefinition("old-scoop", "Old scoop", "old scoop", UnitDimension.Count, 2.0, custom = true, archived = true)
        val current = UnitDefinition("new-scoop", "New scoop", "scoop", UnitDimension.Count, 4.0, custom = true)
        val habit = habit().copy(unitId = current.id)
        val units = listOf(old, current)
        assertTrue(progress(habit).inspectorPrimaryActionLabel(units).contains("scoop"))
        assertTrue(habit.periodTotalAmountLabel(units).contains("scoop"))
        assertEquals("Logged 5 old scoop", log(1, today, 5.0).copy(enteredUnitId = old.id).activityTitle(habit, units))
        assertTrue(habit.canChangeTrackingModeTo(HabitTrackingMode.Checklist))
        assertFalse(habit.canChangeTrackingModeTo(HabitTrackingMode.Duration))
        assertFalse(habit.copy(dimension = UnitDimension.Volume).canChangeTrackingModeTo(HabitTrackingMode.CheckOff))
        assertTrue(habit.copy(dimension = UnitDimension.Volume).canChangeTrackingModeTo(HabitTrackingMode.Decimal))
    }

    @Test fun currentPausePreservesHistoricalOutcomesReviewAndEarnedEndConditions() {
        val habit = habit().copy(startDate = today.minusDays(4), paused = true)
        val logs = listOf(log(1, today.minusDays(4)), log(2, today.minusDays(3)))
        val pauses = listOf(HabitPause(1, habit.id, today.minusDays(2), today.minusDays(2), "Known interval"))
        val skips = listOf(HabitSkip("skip", habit.id, today.minusDays(1), 1, 1, 1))
        assertFalse(habit.isScheduledOn(today))
        assertEquals(HabitDayState.Paused, habit.dayStateOn(today, today, logs, pauses, skips))
        assertEquals(HabitDayState.Completed, habit.dayStateOn(today.minusDays(4), today, logs, pauses, skips))
        assertEquals(HabitDayState.Paused, habit.dayStateOn(today.minusDays(2), today, logs, pauses, skips))
        assertEquals(HabitDayState.Skipped, habit.dayStateOn(today.minusDays(1), today, logs, pauses, skips))
        assertEquals(1.0, habit.completionRateOverRecentPeriods(logs, today, pauses = pauses, skips = skips), 0.0)
        assertTrue(habit.copy(endType = HabitEndType.AfterCompletions, endValue = 2.0).hasEnded(logs, today, pauses, skips = skips))
        assertTrue(habit.copy(endType = HabitEndType.AfterStreak, endValue = 2.0).hasEnded(logs, today, pauses, skips = skips))
        val state = HabitUiState(all = listOf(progress(habit)), logs = logs, pauses = pauses, skips = skips)
        assertEquals(2, reviewOutcomes(TaskUiState(), state, GoalUiState(), GymUiState(), setOf(ReviewSection.Habits), today.minusDays(4), today, zone).size)
        assertEquals(HabitDayState.Missed, habit.dayStateOn(today.minusDays(1), today, logs))
    }

    @Test fun aClosedWeeksFinalMissingDayRemainsInTheChartDenominator() {
        val start = today.minusWeeks(1)
        val habit = habit().copy(startDate = start)
        val days = (0L..6L).map(start::plusDays)
        val logs = days.dropLast(1).mapIndexed { i, day -> log(i.toLong(), day) }
        assertEquals(6.0 / 7.0, requireNotNull(habit.scheduledCompletionRateForDays(days, today, logs)), 0.00001)
        assertEquals(6.0 / 7.0, requireNotNull(habit.copy(paused = true).scheduledCompletionRateForDays(days, today, logs)), 0.00001)
    }

    @Test fun capacityUsesAllAreasAndExcludesClosedRecurringOccurrences() {
        val selected = ScheduledTask(task(1).copy(durationMinutes = 30), null, null)
        val otherArea = task(2).copy(scheduleKind = ScheduleKind.Once, date = today, areaId = "other", durationMinutes = 100)
        assertTrue(runCatching { validateDayPlanCapacity(listOf(selected.task, otherArea), emptyList(), listOf(selected), today, zone, 120) }.isFailure)
        val recurring = task(3).copy(scheduleKind = ScheduleKind.Recurring, durationMinutes = 120,
            recurrence = RecurrenceRule(RecurrenceUnit.Days, 1, startDate = today))
        val closed = TaskOccurrence(recurring.id, today, today, OccurrenceState.Completed, today.atStartOfDay(zone).toInstant().toEpochMilli())
        validateDayPlanCapacity(listOf(selected.task, recurring), listOf(closed), listOf(selected), today, zone, 30)
    }

    private fun task(id: Long) = WhipTask(id, "Task $id", "", ScheduleKind.Anytime, null, null, null, false, false, null, 1, 1)
    private fun progress(habit: Habit) = HabitDayProgress(habit, today, true, 0.0, null, null, emptyList(), 0, 0.0)
    private fun log(id: Long, date: LocalDate, value: Double = 1.0) = HabitLog(id, "log$id", 1, value, value, "count", HabitLogStatus.Recorded,
        date.atStartOfDay(zone).toInstant(), date, zone.id, 0, "", MeasurementSourceType.Manual, null, "entry$id", 1, 1)
    private fun habit() = Habit(id = 1, uuid = "habit", measurementId = "measure", name = "Habit", notes = "", area = "", tags = emptyList(), icon = "✓",
        trackingMode = HabitTrackingMode.Count, dimension = UnitDimension.Count, unitId = "count", precision = 0,
        comparison = TargetComparison.AtLeast, targetMin = 1.0, targetMax = null, targetPeriod = TargetPeriod.Day, rollingDays = null,
        scheduleType = HabitScheduleType.Daily, scheduleInterval = 1, weekdays = emptySet(), flexibleTimesPerWeek = null, startDate = today,
        endType = HabitEndType.Never, endDate = null, endValue = null, quickIncrement = 1.0, quickActions = emptyList(), reminderMinutes = emptyList(),
        weekdayReminderMinutes = emptyMap(), weekStart = DayOfWeek.MONDAY, timerStartedAtMillis = null, pinned = false, position = 0, archived = false, paused = false,
        createdAtMillis = 1, updatedAtMillis = 1)
}
