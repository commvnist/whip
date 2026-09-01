package com.whip.app.reminders

import com.whip.app.domain.Goal
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalDirection
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalPaceType
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.GoalType
import com.whip.app.domain.Habit
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.HabitEndType
import com.whip.app.domain.HabitScheduleType
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.MissedOccurrencePolicy
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TargetComparison
import com.whip.app.domain.TargetPeriod
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskStep
import com.whip.app.domain.TaskStepDraft
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.WhipTask
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderInvalidationTest {
    private val today = LocalDate.of(2026, 8, 24)

    @Test
    fun identityOnlyEditsNeverReschedulePlatformWork() {
        val goalDraft = GoalDraft(name = "Read", icon = "📚", type = GoalType.ReachValue, startDate = today, reminderMinutes = 540)
        assertFalse(goal(goalDraft).reminderDefinitionChanged(goalDraft.copy(icon = "📖", name = "Read books")))

        val habitDraft = HabitDraft(name = "Walk", icon = "🚶", startDate = today, reminderMinutes = listOf(540))
        assertFalse(habit(habitDraft).reminderDefinitionChanged(habitDraft.copy(icon = "🥾", name = "Daily walk", notes = "Outside")))

        val taskDraft = TaskDraft(title = "Plan", icon = "📝", scheduleKind = ScheduleKind.Once, date = today, timeMinutes = 600, reminderEnabled = true)
        assertFalse(task(taskDraft).reminderDefinitionChanged(taskDraft.copy(icon = "📋", title = "Plan week", notes = "Sunday")))
    }

    @Test
    fun schedulingAndProgressSemanticsStillReschedule() {
        val goalDraft = GoalDraft(name = "Read", type = GoalType.ReachValue, startDate = today, reminderMinutes = 540)
        assertTrue(goal(goalDraft).reminderDefinitionChanged(goalDraft.copy(reminderMinutes = 600)))
        assertTrue(goal(goalDraft).reminderDefinitionChanged(goalDraft.copy(type = GoalType.ElapsedSince)))

        val habitDraft = HabitDraft(name = "Walk", startDate = today, reminderMinutes = listOf(540))
        assertTrue(habit(habitDraft).reminderDefinitionChanged(habitDraft.copy(scheduleType = HabitScheduleType.SelectedWeekdays, weekdays = setOf(DayOfWeek.MONDAY))))
        assertTrue(habit(habitDraft).reminderDefinitionChanged(habitDraft.copy(quickIncrement = 5.0)))

        val taskDraft = TaskDraft(title = "Plan", scheduleKind = ScheduleKind.Once, date = today, timeMinutes = 600, reminderEnabled = true)
        assertTrue(task(taskDraft).reminderDefinitionChanged(taskDraft.copy(timeMinutes = 630)))

        val taskWithStep = task(taskDraft).copy(
            steps = listOf(TaskStep(7, 1, "Review", 0, createdAtMillis = 0, updatedAtMillis = 0)),
        )
        assertTrue(
            taskWithStep.reminderDefinitionChanged(
                taskDraft.copy(steps = listOf(TaskStepDraft(title = "New step", position = 0))),
            ),
        )
        assertFalse(
            taskWithStep.reminderDefinitionChanged(
                taskDraft.copy(steps = listOf(TaskStepDraft(id = 7, title = "Renamed", position = 0))),
            ),
        )
    }

    private fun goal(draft: GoalDraft) = Goal(
        id = 1, uuid = "goal", metricId = "metric", name = draft.name, description = draft.description,
        area = draft.area, tags = draft.tags, icon = draft.icon, type = draft.type, dimension = draft.dimension,
        unitId = draft.unitId, precision = draft.precision, baseline = draft.baseline, targetMin = draft.targetMin,
        targetMax = draft.targetMax, direction = GoalDirection.Increase, startDate = draft.startDate,
        deadline = draft.deadline, aggregation = GoalAggregation.Latest, paceType = GoalPaceType.None,
        reminderMinutes = draft.reminderMinutes, status = GoalStatus.Active, pinned = false, position = 0,
        createdAtMillis = 0, updatedAtMillis = 0,
    )

    private fun habit(draft: HabitDraft) = Habit(
        id = 1, uuid = "habit", metricId = "metric", name = draft.name, notes = draft.notes, area = draft.area,
        tags = draft.tags, icon = draft.icon, trackingMode = HabitTrackingMode.CheckOff, dimension = draft.dimension,
        unitId = draft.unitId, precision = draft.precision, comparison = TargetComparison.AtLeast,
        targetMin = draft.targetMin, targetMax = draft.targetMax, targetPeriod = TargetPeriod.Day,
        rollingDays = draft.rollingDays, scheduleType = draft.scheduleType, scheduleInterval = draft.scheduleInterval,
        weekdays = draft.weekdays, flexibleTimesPerWeek = draft.flexibleTimesPerWeek, startDate = draft.startDate,
        endType = HabitEndType.Never, endDate = draft.endDate, endValue = draft.endValue,
        quickIncrement = draft.quickIncrement, quickActions = draft.quickActions, reminderMinutes = draft.reminderMinutes,
        weekdayReminderMinutes = draft.weekdayReminderMinutes, weekStart = draft.weekStart, timerStartedAtMillis = null,
        pinned = false, position = 0, archived = false, paused = false, createdAtMillis = 0, updatedAtMillis = 0,
    )

    private fun task(draft: TaskDraft) = WhipTask(
        id = 1, title = draft.title, notes = draft.notes, scheduleKind = draft.scheduleKind, date = draft.date,
        recurrence = draft.recurrence, timeMinutes = draft.timeMinutes, reminderEnabled = draft.reminderEnabled,
        archived = false, completedAtMillis = null, createdAtMillis = 0, updatedAtMillis = 0,
        deadline = draft.deadline, reminderOffsetsMinutes = draft.reminderOffsetsMinutes,
        missedOccurrencePolicy = MissedOccurrencePolicy.KeepLatest, icon = draft.icon,
    )
}
