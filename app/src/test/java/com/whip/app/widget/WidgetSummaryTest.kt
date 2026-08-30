package com.whip.app.widget

import com.whip.app.domain.AreaScope
import com.whip.app.domain.Habit
import com.whip.app.domain.HabitChecklistItem
import com.whip.app.domain.HabitChecklistState
import com.whip.app.domain.HabitEndType
import com.whip.app.domain.HabitLog
import com.whip.app.domain.HabitLogStatus
import com.whip.app.domain.HabitPause
import com.whip.app.domain.HabitScheduleType
import com.whip.app.domain.HabitSkip
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.MetricEntry
import com.whip.app.domain.MetricEntryStatus
import com.whip.app.domain.MetricSourceType
import com.whip.app.domain.OccurrenceState
import com.whip.app.domain.RecurrenceRule
import com.whip.app.domain.RecurrenceUnit
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.ScheduledSubtask
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.TargetComparison
import com.whip.app.domain.TargetPeriod
import com.whip.app.domain.TaskOccurrence
import com.whip.app.domain.TaskStep
import com.whip.app.domain.TaskStepSnapshot
import com.whip.app.domain.TaskStepState
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.WhipTask
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetContentTest {
    private val today = LocalDate.of(2026, 8, 28)

    @Test
    fun compactWidgetHidesActionsUntilThereIsRoomForFortyEightDpTargets() {
        assertEquals(223, availableWidgetHeight(minHeightDp = 124, maxHeightDp = 223, landscape = false))
        assertEquals(124, availableWidgetHeight(minHeightDp = 124, maxHeightDp = 223, landscape = true))
        assertEquals(3, widgetRowCapacity(0, 1f))
        assertEquals(1, widgetRowCapacity(110, 1f))
        assertEquals(2, widgetRowCapacity(223, 1f))
        assertEquals(2, widgetRowCapacity(223, 1.3f))
        assertTrue(useCompactWidgetHeader(160, 1f))
        assertTrue(useCompactWidgetHeader(240, 2f))
        assertFalse(useCompactWidgetHeader(240, 1f))
        assertFalse(useSingleLineWidgetRows(1.99f))
        assertTrue(useSingleLineWidgetRows(2f))
        assertTrue(useSingleLineWidgetRows(3.2f))
        assertEquals(255, widgetBackgroundAlpha(0))
        assertEquals(51, widgetBackgroundAlpha(80))
        assertEquals(51, widgetBackgroundAlpha(100))
    }

    @Test
    fun taskAgendaDefaultsToOverdueAndTheNextSevenDays() {
        val content = calculateTaskAgendaContent(
            tasks = listOf(
                task(1, ScheduleKind.Once, today.minusDays(2)),
                task(2, ScheduleKind.Once, today),
                task(3, ScheduleKind.Once, today.plusDays(7)),
                task(4, ScheduleKind.Once, today.plusDays(8)),
                task(5, ScheduleKind.Anytime),
            ),
            taskOccurrences = emptyList(),
            taskSteps = emptyList(),
            taskStepStates = emptyList(),
            taskStepSnapshots = emptyList(),
            today = today,
            areaScope = AreaScope.All,
            range = AgendaRange.SevenDays,
            zoneId = ZoneId.of("UTC"),
        )

        assertEquals(listOf(1L, 2L, 3L), content.items.map { it.task.id })
    }

    @Test
    fun taskAgendaShowsOnlyTheNextActionableOccurrenceOfEachRecurringTask() {
        val openSeries = task(1, ScheduleKind.Recurring).copy(
            recurrence = RecurrenceRule(RecurrenceUnit.Days, startDate = today),
        )
        val completedTodaySeries = task(2, ScheduleKind.Recurring).copy(
            recurrence = RecurrenceRule(RecurrenceUnit.Days, startDate = today),
        )
        val content = calculateTaskAgendaContent(
            tasks = listOf(openSeries, completedTodaySeries),
            taskOccurrences = listOf(
                TaskOccurrence(2, today, today, OccurrenceState.Completed, 1),
            ),
            taskSteps = emptyList(),
            taskStepStates = emptyList(),
            taskStepSnapshots = emptyList(),
            today = today,
            areaScope = AreaScope.All,
            range = AgendaRange.SevenDays,
            zoneId = ZoneId.of("UTC"),
        )

        assertEquals(2, content.items.size)
        assertEquals(today, content.items.single { it.task.id == 1L }.scheduledDate)
        assertEquals(today.plusDays(1), content.items.single { it.task.id == 2L }.scheduledDate)
    }

    @Test
    fun taskAgendaCarriesCurrentSubtaskStateForSafeCompletionDecisions() {
        val parent = task(1, ScheduleKind.Once, today)
        val step = TaskStep(11, 1, "Review", 0, createdAtMillis = 1, updatedAtMillis = 1)
        val content = calculateTaskAgendaContent(
            tasks = listOf(parent),
            taskOccurrences = emptyList(),
            taskSteps = listOf(step),
            taskStepStates = listOf(TaskStepState(11, 1, today.toEpochDay(), false, null, "Review")),
            taskStepSnapshots = emptyList<TaskStepSnapshot>(),
            today = today,
            areaScope = AreaScope.All,
            range = AgendaRange.Today,
            zoneId = ZoneId.of("UTC"),
        )

        assertEquals(1, content.items.single().subtasks.size)
        assertFalse(content.items.single().subtasks.single().completed)
    }

    @Test
    fun taskWidgetRowsOnlyExposeSubtasksWhileTheirExactOccurrenceIsExpanded() {
        val parent = task(1, ScheduleKind.Once, today)
        val item = ScheduledTask(
            task = parent,
            originalDate = today,
            scheduledDate = today,
            subtasks = listOf(
                ScheduledSubtask(
                    step = TaskStep(11, 1, "First", 0, createdAtMillis = 1, updatedAtMillis = 1),
                    completed = false,
                    completedAtMillis = null,
                    title = "First",
                ),
                ScheduledSubtask(
                    step = TaskStep(12, 1, "Second", 1, createdAtMillis = 1, updatedAtMillis = 1),
                    completed = true,
                    completedAtMillis = 1,
                    title = "Second",
                ),
            ),
        )

        assertEquals(1, taskWidgetRows(listOf(item), emptySet()).size)
        val expanded = taskWidgetRows(listOf(item), setOf(item.stableKey))
        assertEquals(3, expanded.size)
        assertTrue(expanded.first().expanded)
        assertEquals(listOf(11L, 12L), expanded.drop(1).map { it.subtask?.step?.id })
    }

    @Test
    fun habitTrackingKeepsCompletedHabitsAndFlattensChecklistItems() {
        val checkOff = habit(1).copy(trackingMode = HabitTrackingMode.CheckOff)
        val checklist = habit(2).copy(
            trackingMode = HabitTrackingMode.Checklist,
            autoCompleteFromItems = true,
        )
        val itemOne = checklistItem(21, 2, "Water", 0)
        val itemTwo = checklistItem(22, 2, "Medicine", 1)
        val content = calculateHabitTrackingContent(
            habits = listOf(checkOff, checklist),
            habitLogs = listOf(log(checkOff.id)),
            habitChecklistItems = listOf(itemOne, itemTwo),
            habitChecklistStates = listOf(
                HabitChecklistState(2, 21, today, true, 1, "Water"),
            ),
            habitPauses = emptyList(),
            habitSkips = emptyList(),
            metricEntries = emptyList(),
            customUnits = emptyList(),
            today = today,
            areaScope = AreaScope.All,
            showCompleted = true,
            expandedHabitIds = setOf(checklist.id),
        )

        assertEquals(2, content.scheduledHabits)
        assertEquals(1, content.completedHabits)
        assertEquals(4, content.rows.size)
        assertEquals(HabitWidgetAction.ToggleHabit, content.rows.first { it.habit.id == 2L && !it.isChecklistItem }.action)
        assertEquals(
            listOf(HabitWidgetAction.ToggleChecklistItem, HabitWidgetAction.ToggleChecklistItem),
            content.rows.filter(HabitWidgetRow::isChecklistItem).map(HabitWidgetRow::action),
        )
        assertEquals(1, content.rows.first { it.habit.id == 2L && !it.isChecklistItem }.completedChecklistItems)
        assertTrue(content.rows.first { it.habit.id == 2L && !it.isChecklistItem }.expanded)
    }

    @Test
    fun habitTrackingKeepsChecklistsCollapsedAndRespectsTheWidgetSelection() {
        val excluded = habit(1).copy(trackingMode = HabitTrackingMode.CheckOff)
        val selected = habit(2).copy(trackingMode = HabitTrackingMode.Checklist)
        val content = calculateHabitTrackingContent(
            habits = listOf(excluded, selected),
            habitLogs = emptyList(),
            habitChecklistItems = listOf(checklistItem(21, selected.id, "Water", 0)),
            habitChecklistStates = emptyList(),
            habitPauses = emptyList(),
            habitSkips = emptyList(),
            metricEntries = emptyList(),
            customUnits = emptyList(),
            today = today,
            areaScope = AreaScope.All,
            showCompleted = true,
            selectedHabitIds = setOf(selected.id),
        )

        assertEquals(1, content.scheduledHabits)
        assertEquals(listOf(selected.id), content.rows.map { it.habit.id })
        assertTrue(content.rows.single().expandable)
        assertFalse(content.rows.single().expanded)
    }

    @Test
    fun habitTrackingCanHideDoneRowsAndKeepsNativeTrackingActions() {
        val done = habit(1).copy(trackingMode = HabitTrackingMode.CheckOff)
        val count = habit(2).copy(trackingMode = HabitTrackingMode.Count)
        val duration = habit(3).copy(trackingMode = HabitTrackingMode.Duration)
        val synced = habit(4).copy(sourceMetricId = "health.steps")
        val content = calculateHabitTrackingContent(
            habits = listOf(done, count, duration, synced),
            habitLogs = listOf(log(done.id)),
            habitChecklistItems = emptyList(),
            habitChecklistStates = emptyList(),
            habitPauses = emptyList(),
            habitSkips = emptyList(),
            metricEntries = emptyList(),
            customUnits = emptyList(),
            today = today,
            areaScope = AreaScope.All,
            showCompleted = false,
        )

        assertFalse(content.rows.any { it.habit.id == done.id })
        assertEquals(HabitWidgetAction.Increment, content.rows.first { it.habit.id == count.id }.action)
        assertEquals(HabitWidgetAction.StartTimer, content.rows.first { it.habit.id == duration.id }.action)
        assertEquals(HabitWidgetAction.ReadOnly, content.rows.first { it.habit.id == synced.id }.action)
    }

    private fun task(
        id: Long,
        scheduleKind: ScheduleKind,
        date: LocalDate? = null,
        areaId: String? = null,
    ) = WhipTask(
        id = id,
        title = "Task $id",
        notes = "",
        scheduleKind = scheduleKind,
        date = date,
        recurrence = null,
        timeMinutes = null,
        reminderEnabled = false,
        archived = false,
        completedAtMillis = null,
        createdAtMillis = id,
        updatedAtMillis = id,
        areaId = areaId,
    )

    private fun habit(id: Long, areaId: String? = null) = Habit(
        id = id,
        uuid = "habit-$id",
        metricId = "metric-$id",
        name = "Habit $id",
        notes = "",
        areaId = areaId,
        area = "",
        tags = emptyList(),
        icon = "✓",
        trackingMode = HabitTrackingMode.Count,
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
        startDate = today.minusDays(1),
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

    private fun log(habitId: Long) = HabitLog(
        id = habitId,
        uuid = "log-$habitId",
        habitId = habitId,
        value = 1.0,
        canonicalValue = 1.0,
        enteredUnitId = "count",
        status = HabitLogStatus.Success,
        timestamp = Instant.parse("2026-08-28T12:00:00Z"),
        localDate = today,
        zoneId = "UTC",
        offsetSeconds = 0,
        note = "",
        sourceType = MetricSourceType.Habit,
        sourceId = "habit-$habitId",
        metricEntryId = null,
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )

    private fun checklistItem(id: Long, habitId: Long, name: String, position: Int) = HabitChecklistItem(
        id = id,
        uuid = "item-$id",
        habitId = habitId,
        name = name,
        position = position,
        archived = false,
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )

    private fun metricEntry(value: Double) = MetricEntry(
        id = "health-entry",
        metricId = "health.steps",
        canonicalValue = value,
        enteredValue = value,
        enteredUnitId = "count",
        status = MetricEntryStatus.Recorded,
        timestamp = Instant.parse("2026-08-28T12:00:00Z"),
        localDate = today,
        zoneId = "UTC",
        offsetSeconds = 0,
        sourceType = MetricSourceType.HealthConnect,
        sourceId = "health-steps",
        note = "",
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )
}
