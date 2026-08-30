package com.whip.app.ui

import com.whip.app.domain.GoalStatus
import com.whip.app.domain.Habit
import com.whip.app.domain.HabitDayProgress
import com.whip.app.domain.HabitDayState
import com.whip.app.domain.HabitEndType
import com.whip.app.domain.HabitScheduleType
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.TargetComparison
import com.whip.app.domain.TargetPeriod
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.WhipTask
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class InspectorStatusToneTest {
    @Test
    fun taskStatusToneUsesDomainSeverity() {
        assertEquals(WhipStatusTone.Neutral, scheduledTask(archived = true).inspectorStatusTone(completed = false))
        assertEquals(WhipStatusTone.Success, scheduledTask().inspectorStatusTone(completed = true))
        assertEquals(
            WhipStatusTone.Destructive,
            scheduledTask(deadlineOverdue = true, pastScheduledDate = true).inspectorStatusTone(completed = false),
        )
        assertEquals(
            WhipStatusTone.Warning,
            scheduledTask(pastScheduledDate = true).inspectorStatusTone(completed = false),
        )
        assertEquals(WhipStatusTone.Neutral, scheduledTask(ScheduleKind.Anytime).inspectorStatusTone(completed = false))
        assertEquals(WhipStatusTone.Info, scheduledTask(ScheduleKind.Once).inspectorStatusTone(completed = false))
        assertEquals(WhipStatusTone.Info, scheduledTask(ScheduleKind.Recurring).inspectorStatusTone(completed = false))
    }

    @Test
    fun habitStatusToneUsesDayStateRatherThanDisplayCopy() {
        assertEquals(WhipStatusTone.Neutral, habitProgress(HabitDayState.Pending, archived = true).inspectorStatusTone())
        assertEquals(WhipStatusTone.Warning, habitProgress(HabitDayState.Pending, paused = true).inspectorStatusTone())
        assertEquals(WhipStatusTone.Warning, habitProgress(HabitDayState.Paused).inspectorStatusTone())
        assertEquals(WhipStatusTone.Warning, habitProgress(HabitDayState.Skipped).inspectorStatusTone())
        assertEquals(WhipStatusTone.Success, habitProgress(HabitDayState.Completed).inspectorStatusTone())
        assertEquals(WhipStatusTone.Info, habitProgress(HabitDayState.BelowTarget).inspectorStatusTone())
        assertEquals(WhipStatusTone.Destructive, habitProgress(HabitDayState.Missed).inspectorStatusTone())
        assertEquals(WhipStatusTone.Neutral, habitProgress(HabitDayState.NotScheduled).inspectorStatusTone())
        assertEquals(WhipStatusTone.Info, habitProgress(HabitDayState.Pending).inspectorStatusTone())

        val missed = habitProgress(HabitDayState.Missed)
        assertNotEquals(missed.inspectorStatus(lowPressureMode = false), missed.inspectorStatus(lowPressureMode = true))
        assertEquals(WhipStatusTone.Destructive, missed.inspectorStatusTone())
    }

    @Test
    fun goalStatusToneMapsEveryPersistedStatusExplicitly() {
        val expected = mapOf(
            GoalStatus.Active to WhipStatusTone.Info,
            GoalStatus.Paused to WhipStatusTone.Warning,
            GoalStatus.Completed to WhipStatusTone.Success,
            GoalStatus.Abandoned to WhipStatusTone.Destructive,
            GoalStatus.Archived to WhipStatusTone.Neutral,
        )

        assertEquals(expected, GoalStatus.entries.associateWith(GoalStatus::inspectorStatusTone))
    }

    private fun scheduledTask(
        scheduleKind: ScheduleKind = ScheduleKind.Once,
        archived: Boolean = false,
        deadlineOverdue: Boolean = false,
        pastScheduledDate: Boolean = false,
    ) = ScheduledTask(
        task = WhipTask(
            id = 1,
            title = "Status mapping",
            notes = "",
            scheduleKind = scheduleKind,
            date = LocalDate.of(2026, 8, 29),
            recurrence = null,
            timeMinutes = null,
            reminderEnabled = false,
            archived = archived,
            completedAtMillis = null,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        ),
        originalDate = null,
        scheduledDate = null,
        isPastScheduledDate = pastScheduledDate,
        isDeadlineOverdue = deadlineOverdue,
    )

    private fun habitProgress(
        dayState: HabitDayState,
        archived: Boolean = false,
        paused: Boolean = false,
    ) = HabitDayProgress(
        habit = Habit(
            id = 1,
            uuid = "habit-status",
            metricId = "habit-status-metric",
            name = "Status mapping",
            notes = "",
            area = "",
            tags = emptyList(),
            icon = "✓",
            trackingMode = HabitTrackingMode.CheckOff,
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
            weekdays = DayOfWeek.entries.toSet(),
            flexibleTimesPerWeek = null,
            startDate = LocalDate.of(2026, 8, 29),
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
            archived = archived,
            paused = paused,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        ),
        date = LocalDate.of(2026, 8, 29),
        scheduled = dayState != HabitDayState.NotScheduled,
        value = 0.0,
        status = null,
        successful = null,
        checklistItems = emptyList(),
        streak = 0,
        completionRate = 0.0,
        dayState = dayState,
    )
}
