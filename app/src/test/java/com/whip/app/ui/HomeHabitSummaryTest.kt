package com.whip.app.ui

import com.whip.app.domain.Habit
import com.whip.app.domain.HabitDayProgress
import com.whip.app.domain.HabitDayState
import com.whip.app.domain.HabitEndType
import com.whip.app.domain.HabitScheduleType
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.TargetComparison
import com.whip.app.domain.TargetPeriod
import com.whip.app.domain.UnitDimension
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeHabitSummaryTest {
    @Test
    fun skippedAndUnavailableOutcomesStayOutsideScoredProgress() {
        val summary = listOf(
            progress(HabitDayState.Completed),
            progress(HabitDayState.BelowTarget),
            progress(HabitDayState.Skipped),
            progress(HabitDayState.Paused),
            progress(HabitDayState.NotScheduled),
        ).homeHabitSummary()

        assertEquals(HomeHabitSummary(completed = 1, total = 2, skipped = 1), summary)
        assertEquals(1, summary.attentionCount)
    }

    @Test
    fun timerRecoveryRemainsActionableWithoutCreatingAnExpectedCheckIn() {
        val summary = listOf(
            progress(HabitDayState.Completed, timer = true),
            progress(HabitDayState.Pending, timer = true),
            progress(HabitDayState.Paused, timer = true),
            progress(HabitDayState.NotScheduled, timer = true),
        ).homeHabitSummary()

        assertEquals(HomeHabitSummary(completed = 1, total = 2, timersToReview = 3), summary)
        assertEquals(4, summary.attentionCount)
    }

    @Test
    fun anAllSkippedDayHasContextButNoScoredOrUnfinishedHabits() {
        val summary = listOf(progress(HabitDayState.Skipped), progress(HabitDayState.Skipped)).homeHabitSummary()
        assertEquals(HomeHabitSummary(skipped = 2), summary)
        assertEquals(0, summary.attentionCount)
        assertTrue(summary.hasContent)
    }

    @Test
    fun explicitNeutralStateWinsOverASuccessfulValueProjection() {
        val summary = listOf(progress(HabitDayState.Skipped).copy(successful = true)).homeHabitSummary()
        assertEquals(HomeHabitSummary(skipped = 1), summary)
        assertEquals(HomeHabitSummary(), emptyList<HabitDayProgress>().homeHabitSummary())
    }

    private fun progress(state: HabitDayState, timer: Boolean = false): HabitDayProgress {
        val date = LocalDate.of(2026, 9, 8)
        val habit = Habit(
            id = 1, uuid = "habit", measurementId = "measurement", name = "Habit", notes = "", area = "",
            tags = emptyList(), icon = "✓", trackingMode = HabitTrackingMode.CheckOff,
            dimension = UnitDimension.Count, unitId = "count", precision = 0,
            comparison = TargetComparison.AtLeast, targetMin = 1.0, targetMax = null, targetPeriod = TargetPeriod.Day,
            rollingDays = null, scheduleType = HabitScheduleType.Daily, scheduleInterval = 1, weekdays = emptySet(),
            flexibleTimesPerWeek = null, startDate = date, endType = HabitEndType.Never, endDate = null, endValue = null,
            quickIncrement = 1.0, quickActions = emptyList(), reminderMinutes = emptyList(), weekdayReminderMinutes = emptyMap(),
            weekStart = DayOfWeek.MONDAY, timerStartedAtMillis = if (timer) 1 else null, pinned = false,
            position = 0, archived = false, paused = state == HabitDayState.Paused,
            createdAtMillis = 1, updatedAtMillis = 1, timerSessionId = if (timer) "timer" else null,
        )
        return HabitDayProgress(
            habit = habit, date = date,
            scheduled = state !in setOf(HabitDayState.Paused, HabitDayState.NotScheduled),
            value = 0.0, status = null, successful = state == HabitDayState.Completed,
            checklistItems = emptyList(), streak = 0, completionRate = 0.0, dayState = state,
        )
    }
}
