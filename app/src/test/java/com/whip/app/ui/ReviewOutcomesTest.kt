package com.whip.app.ui

import com.whip.app.core.ReviewSection
import com.whip.app.domain.*
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class ReviewOutcomesTest {
    private val today = LocalDate.of(2026, 9, 10)
    private val zone = ZoneId.of("America/Toronto")

    @Test fun taskOutcomesKeepCompletionDatesOccurrenceKeysAndAreaScopeAcrossArchive() {
        val active = task(1).copy(date = today.minusDays(2))
        val activeItem = ScheduledTask(active, active.date, active.date, Instant.parse("2026-09-10T01:30:00Z").toEpochMilli())
        val archived = task(2).copy(archived = true, scheduleKind = ScheduleKind.Recurring, date = null,
            recurrence = RecurrenceRule(RecurrenceUnit.Days, startDate = today.minusDays(5)))
        val original = today.minusDays(3)
        val record = TaskOccurrence(2, original, today.minusDays(2), OccurrenceState.Completed,
            Instant.parse("2026-09-10T12:00:00Z").toEpochMilli())
        val otherArea = activeItem.copy(task = task(3).copy(areaId = "personal"))
        val older = activeItem.copy(task = task(4), completedAtMillis = Instant.parse("2026-09-01T12:00:00Z").toEpochMilli())
        val state = TaskUiState(completed = listOf(activeItem, otherArea, older),
            archived = listOf(ScheduledTask(archived, null, null)), occurrences = listOf(record), loading = false)
            .forArea(AreaScope.One("work"))
        val outcomes = outcomes(tasks = state)
        assertEquals(listOf(2L, 1L), outcomes.map { it.sourceId })
        assertEquals(listOf(today, today.minusDays(1)), outcomes.map { it.date })
        assertEquals("2:${original.toEpochDay()}", outcomes.first().sourceKey)
        assertTrue(outcomes.first().archived)
        assertEquals(listOf(0.0, 1.0, 1.0), outcomes.dailyReviewValues(ReviewSection.Tasks,
            listOf(today.minusDays(2), today.minusDays(1), today)))
        assertEquals(activeItem, TaskNavigationIndex(state).byKey.getValue(outcomes.last().sourceKey))
    }

    @Test fun multipleGoalEntriesContributeOneNormalizedDailyGain() {
        val goal = Goal(id = 1, uuid = "g", measurementId = "m", name = "Read", description = "", area = "",
            tags = emptyList(), icon = "◎", type = GoalType.ReachValue, dimension = UnitDimension.Unitless,
            unitId = "unitless", precision = 3, baseline = 0.0, targetMin = 10.0, targetMax = null,
            direction = GoalDirection.Increase, startDate = today.minusDays(2), deadline = null,
            aggregation = GoalType.ReachValue.defaultAggregation(), paceType = GoalPaceType.None,
            reminderMinutes = null, status = GoalStatus.Active, pinned = false, position = 0,
            createdAtMillis = 1, updatedAtMillis = 1, archived = true)
        val entries = listOf(entry(1, 2.0, today.minusDays(1)), entry(2, 3.0, today), entry(3, 5.0, today))
        val rows = reviewOutcomes(TaskUiState(), HabitUiState(),
            GoalUiState(archived = listOf(projectGoal(goal, entries, emptyList(), today))), GymUiState(),
            setOf(ReviewSection.Goals), today, today, zone)
        assertEquals(1, rows.size)
        assertEquals(0.3, rows.single().score, 0.00000001)
        assertTrue(rows.single().archived)
        assertEquals(rows.single().score, rows.dailyReviewValues(ReviewSection.Goals, listOf(today)).single(), 0.0)
    }

    @Test fun rawHabitIncrementsBecomeOneOutcomeOnlyWhenThePeriodTargetIsReached() {
        val partial = habit(1)
        val complete = habit(2).copy(archived = true)
        val logs = listOf(log(1, 1, 3.0), log(2, 1, 3.0), log(3, 2, 4.0), log(4, 2, 4.0))
        val state = HabitUiState(all = listOf(HabitDayProgress(partial, today, true, 6.0, HabitLogStatus.Recorded,
            false, emptyList(), 0, 0.0)), archived = listOf(complete), logs = logs, loading = false)
        val rows = outcomes(habits = state)
        assertEquals(listOf(2L), rows.map { it.sourceId })
        assertEquals(1.0, rows.single().score, 0.0)
        assertEquals(2, outcomes(habits = state.copy(logs = logs + log(5, 1, 2.0))).size)
        assertTrue(outcomes(habits = state, sections = setOf(ReviewSection.Tasks)).isEmpty())
    }

    @Test fun positiveNormalizedProgressNeverReadsAsZero() {
        assertEquals("0.005", formatReviewNumber(0.005, Locale.US))
        assertEquals("<0.001", formatReviewNumber(0.0005, Locale.US))
        assertEquals("<0,001", formatReviewNumber(0.0005, Locale.GERMANY))
        assertEquals("0", formatReviewNumber(0.0, Locale.US))
        assertEquals("2", formatReviewNumber(2.0, Locale.US))
    }

    private fun outcomes(tasks: TaskUiState = TaskUiState(), habits: HabitUiState = HabitUiState(),
        sections: Set<ReviewSection> = ReviewSection.entries.toSet()) =
        reviewOutcomes(tasks, habits, GoalUiState(), GymUiState(), sections, today.minusDays(2), today, zone)

    private fun task(id: Long) = WhipTask(id, "Task $id", "", ScheduleKind.Once, today, null, null,
        false, false, null, 1, 1, areaId = "work")
    private fun entry(id: Long, value: Double, date: LocalDate) = MeasurementEntry("e$id", "m", value, value,
        "unitless", MeasurementEntryStatus.Recorded, date.atStartOfDay(zone).toInstant().plusSeconds(id), date,
        zone.id, 0, MeasurementSourceType.Manual, null, "", 1, 1)
    private fun log(id: Long, habit: Long, value: Double) = HabitLog(id, "log-$id", habit, value, value, "count",
        HabitLogStatus.Recorded, today.atStartOfDay(zone).toInstant().plusSeconds(id), today, zone.id, 0, "",
        MeasurementSourceType.Manual, null, null, 1, 1)
    private fun habit(id: Long) = Habit(id = id, uuid = "habit-$id", measurementId = "measure-$id", name = "Water", notes = "",
        area = "", tags = emptyList(), icon = "✓", trackingMode = HabitTrackingMode.Count,
        dimension = UnitDimension.Count, unitId = "count", precision = 0, comparison = TargetComparison.AtLeast,
        targetMin = 8.0, targetMax = null, targetPeriod = TargetPeriod.Day, rollingDays = null,
        scheduleType = HabitScheduleType.Daily, scheduleInterval = 1, weekdays = emptySet(), flexibleTimesPerWeek = null,
        startDate = today, endType = HabitEndType.Never, endDate = null, endValue = null,
        quickIncrement = 1.0, quickActions = emptyList(), reminderMinutes = emptyList(), weekdayReminderMinutes = emptyMap(),
        weekStart = DayOfWeek.MONDAY, timerStartedAtMillis = null, pinned = false, position = 0,
        archived = false, paused = false, createdAtMillis = 1, updatedAtMillis = 1)
}
