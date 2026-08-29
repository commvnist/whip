package com.whip.app.widget

import com.whip.app.domain.AreaScope
import com.whip.app.domain.Habit
import com.whip.app.domain.HabitChecklistItem
import com.whip.app.domain.HabitChecklistState
import com.whip.app.domain.HabitLog
import com.whip.app.domain.HabitPause
import com.whip.app.domain.HabitScheduleType
import com.whip.app.domain.HabitSkip
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.MetricEntry
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.ScheduledSubtask
import com.whip.app.domain.TaskOccurrence
import com.whip.app.domain.TaskStep
import com.whip.app.domain.TaskStepSnapshot
import com.whip.app.domain.TaskStepState
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.WhipTask
import com.whip.app.domain.flexibleProgress
import com.whip.app.domain.hasEnded
import com.whip.app.domain.isNeutralDate
import com.whip.app.domain.isScheduledOn
import com.whip.app.domain.matches
import com.whip.app.domain.outcomeForPeriod
import com.whip.app.domain.valueForPeriod
import com.whip.app.ui.buildUiState
import com.whip.app.ui.mirrorMetricEntriesAsHabitLogs
import com.whip.app.ui.withRecurringOccurrenceVisibility
import java.time.LocalDate
import java.time.ZoneId

internal data class TaskAgendaContent(
    val items: List<ScheduledTask>,
)

internal data class TaskWidgetRow(
    val item: ScheduledTask,
    val subtask: ScheduledSubtask? = null,
    val expanded: Boolean = false,
) {
    val isSubtask: Boolean get() = subtask != null
    val expandable: Boolean get() = !isSubtask && item.subtasks.isNotEmpty()
    val unfinishedSubtaskCount: Int get() = if (isSubtask) 0 else item.subtasks.count { !it.completed }
    val requiresSubtaskReview: Boolean get() = unfinishedSubtaskCount > 0
}

internal fun taskWidgetRows(
    items: List<ScheduledTask>,
    expandedTaskKeys: Set<String>,
): List<TaskWidgetRow> = buildList {
    items.forEach { item ->
        val expanded = item.stableKey in expandedTaskKeys
        add(TaskWidgetRow(item = item, expanded = expanded))
        if (expanded) {
            item.subtasks.forEach { subtask ->
                add(TaskWidgetRow(item = item, subtask = subtask))
            }
        }
    }
}

internal fun calculateTaskAgendaContent(
    tasks: List<WhipTask>,
    taskOccurrences: List<TaskOccurrence>,
    taskSteps: List<TaskStep>,
    taskStepStates: List<TaskStepState>,
    taskStepSnapshots: List<TaskStepSnapshot>,
    today: LocalDate,
    areaScope: AreaScope,
    range: AgendaRange,
    zoneId: ZoneId,
): TaskAgendaContent {
    val scopedTasks = tasks.filter { areaScope.matches(it.areaId) }
    val state = buildUiState(
        tasks = scopedTasks,
        occurrences = taskOccurrences,
        steps = taskSteps,
        stepStates = taskStepStates,
        stepSnapshots = taskStepSnapshots,
        today = today,
        showAllUpcomingRecurringOccurrences = false,
        zoneId = zoneId,
    )
    val through = today.plusDays(range.daysAhead)
    return TaskAgendaContent(
        items = (state.today + state.upcoming)
            .filter { item ->
                val date = item.scheduledDate ?: item.originalDate
                date == null || !date.isAfter(through)
            }
            .distinctBy(ScheduledTask::stableKey)
            .withRecurringOccurrenceVisibility(showAllRecurringOccurrences = false),
    )
}

internal enum class HabitWidgetAction {
    ToggleHabit,
    ToggleChecklistItem,
    Increment,
    StartTimer,
    StopTimer,
    Open,
    ReadOnly,
}

internal data class HabitWidgetRow(
    val habit: Habit,
    val checklistItem: HabitChecklistItem? = null,
    val completed: Boolean,
    val action: HabitWidgetAction,
    val value: Double = 0.0,
    val completedChecklistItems: Int = 0,
    val checklistItemCount: Int = 0,
    val expanded: Boolean = false,
) {
    val isChecklistItem: Boolean get() = checklistItem != null
    val expandable: Boolean get() = !isChecklistItem && checklistItemCount > 0
}

internal data class HabitTrackingContent(
    val rows: List<HabitWidgetRow>,
    val scheduledHabits: Int,
    val completedHabits: Int,
)

internal fun calculateHabitTrackingContent(
    habits: List<Habit>,
    habitLogs: List<HabitLog>,
    habitChecklistItems: List<HabitChecklistItem>,
    habitChecklistStates: List<HabitChecklistState>,
    habitPauses: List<HabitPause>,
    habitSkips: List<HabitSkip>,
    metricEntries: List<MetricEntry>,
    customUnits: List<UnitDefinition>,
    today: LocalDate,
    areaScope: AreaScope,
    showCompleted: Boolean,
    selectedHabitIds: Set<Long>? = null,
    expandedHabitIds: Set<Long> = emptySet(),
): HabitTrackingContent {
    val projectedLogs = habitLogs + mirrorMetricEntriesAsHabitLogs(habits, metricEntries, customUnits)
    val scheduled = habits.mapNotNull { habit ->
        if (
            habit.archived ||
            habit.paused ||
            !areaScope.matches(habit.areaId) ||
            selectedHabitIds?.contains(habit.id) == false
        ) return@mapNotNull null
        val logs = projectedLogs.filter { it.habitId == habit.id }
        val pauses = habitPauses.filter { it.habitId == habit.id }
        val skips = habitSkips.filter { it.habitId == habit.id }
        if (habit.hasEnded(logs, today, pauses, customUnits, skips)) return@mapNotNull null
        if (habit.isNeutralDate(today, pauses, skips)) return@mapNotNull null
        val flexible = habit.flexibleProgress(logs, today, pauses, skips)
        val isScheduled = when (habit.scheduleType) {
            HabitScheduleType.FlexibleTimesPerWeek,
            HabitScheduleType.FlexibleTimesPerMonth,
            -> flexible?.target?.let { it > 0 } == true
            else -> habit.isScheduledOn(today)
        }
        if (!isScheduled) return@mapNotNull null
        val completed = habit.outcomeForPeriod(logs, today, customUnits) == true
        ScheduledHabit(
            habit = habit,
            completed = completed,
            value = habit.valueForPeriod(logs, today, customUnits),
        )
    }
    val eligible = scheduled.filter { showCompleted || !it.completed }
    val visible = eligible.filterNot(ScheduledHabit::completed) + eligible.filter(ScheduledHabit::completed)
    val rows = buildList {
        visible.forEach { scheduledHabit ->
            val habit = scheduledHabit.habit
            val items = habitChecklistItems
                .filter { it.habitId == habit.id && !it.archived }
                .sortedBy(HabitChecklistItem::position)
            val states = habitChecklistStates
                .filter { it.habitId == habit.id && it.localDate == today }
                .associateBy(HabitChecklistState::itemId)
            val completedItems = items.count { states[it.id]?.completed == true }
            val parentAction = when {
                habit.sourceMetricId != null -> HabitWidgetAction.ReadOnly
                habit.trackingMode == HabitTrackingMode.CheckOff -> HabitWidgetAction.ToggleHabit
                habit.trackingMode == HabitTrackingMode.Checklist -> HabitWidgetAction.ToggleHabit
                habit.trackingMode in setOf(HabitTrackingMode.Count, HabitTrackingMode.Decimal) -> HabitWidgetAction.Increment
                habit.trackingMode == HabitTrackingMode.Duration && habit.timerStartedAtMillis == null -> HabitWidgetAction.StartTimer
                habit.trackingMode == HabitTrackingMode.Duration -> HabitWidgetAction.StopTimer
                else -> HabitWidgetAction.Open
            }
            add(
                HabitWidgetRow(
                    habit = habit,
                    completed = scheduledHabit.completed,
                    action = parentAction,
                    value = scheduledHabit.value,
                    completedChecklistItems = completedItems,
                    checklistItemCount = items.size,
                    expanded = habit.id in expandedHabitIds,
                ),
            )
            if (habit.trackingMode == HabitTrackingMode.Checklist && habit.id in expandedHabitIds) {
                items.forEach { item ->
                    add(
                        HabitWidgetRow(
                            habit = habit,
                            checklistItem = item,
                            completed = states[item.id]?.completed == true,
                            action = HabitWidgetAction.ToggleChecklistItem,
                        ),
                    )
                }
            }
        }
    }
    return HabitTrackingContent(
        rows = rows,
        scheduledHabits = scheduled.size,
        completedHabits = scheduled.count(ScheduledHabit::completed),
    )
}

private data class ScheduledHabit(
    val habit: Habit,
    val completed: Boolean,
    val value: Double,
)
