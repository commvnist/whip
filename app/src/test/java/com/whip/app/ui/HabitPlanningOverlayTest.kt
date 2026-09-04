package com.whip.app.ui

import com.whip.app.domain.Habit
import com.whip.app.domain.HabitDayProgress
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

class HabitPlanningOverlayTest {
    private val monday = LocalDate.of(2026, 8, 17)

    @Test
    fun futureProjectionHonorsWeekdaysAndKeepsHabitIdentity() {
        val habit = habit().copy(scheduleType = HabitScheduleType.SelectedWeekdays, weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
        val state = HabitUiState(all = listOf(progress(habit)), today = listOf(progress(habit)), currentDate = monday, loading = false)

        assertEquals(habit.id, state.plannedOn(monday.plusDays(2)).single().habit.id)
        assertTrue(state.plannedOn(monday.plusDays(1)).isEmpty())
    }

    @Test
    fun projectionExcludesPausedAndEndedHabits() {
        val ended = habit().copy(endType = HabitEndType.OnDate, endDate = monday)
        val paused = habit().copy(id = 2, uuid = "paused")
        val state = HabitUiState(
            all = listOf(progress(ended), progress(paused)),
            pauses = listOf(com.whip.app.domain.HabitPause(1, paused.id, monday, monday.plusWeeks(1), "holiday")),
            currentDate = monday,
            loading = false,
        )

        assertTrue(state.plannedOn(monday.plusDays(1)).isEmpty())
    }

    private fun progress(habit: Habit) = HabitDayProgress(
        habit, monday, true, 0.0, null, false, emptyList(), 0, 0.0,
    )

    private fun habit() = Habit(
        id = 1, uuid = "habit", measurementId = "measurement", name = "Habit", notes = "", area = "", tags = emptyList(), icon = "✓",
        trackingMode = HabitTrackingMode.Count,
        dimension = UnitDimension.Count, unitId = "count", precision = 0,
        comparison = TargetComparison.AtLeast, targetMin = 1.0, targetMax = null, targetPeriod = TargetPeriod.Day,
        rollingDays = null, scheduleType = HabitScheduleType.Daily, scheduleInterval = 1, weekdays = emptySet(),
        flexibleTimesPerWeek = null, startDate = monday, endType = HabitEndType.Never,
        endDate = null, endValue = null,
        quickIncrement = 1.0, quickActions = emptyList(), reminderMinutes = emptyList(), weekdayReminderMinutes = emptyMap(),
        weekStart = DayOfWeek.MONDAY, timerStartedAtMillis = null, pinned = false,
        position = 0, archived = false, paused = false,
        createdAtMillis = 1, updatedAtMillis = 1,
    )
}
