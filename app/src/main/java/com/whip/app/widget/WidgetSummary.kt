package com.whip.app.widget

import com.whip.app.domain.AreaScope
import com.whip.app.domain.Habit
import com.whip.app.domain.HabitLog
import com.whip.app.domain.HabitPause
import com.whip.app.domain.HabitSkip
import com.whip.app.domain.MetricEntry
import com.whip.app.domain.TaskOccurrence
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.WhipTask
import com.whip.app.domain.matches
import com.whip.app.domain.reminderNeededOn
import com.whip.app.ui.buildUiState
import com.whip.app.ui.mirrorMetricEntriesAsHabitLogs
import java.time.LocalDate
import java.time.ZoneId

internal data class WidgetSummary(
    val tasksDue: Int,
    val habitsDue: Int,
)

/**
 * Builds the widget's Today snapshot from the same occurrence and habit-outcome
 * policies used by the in-app Today surfaces. Task steps/checklist items are not
 * top-level work and therefore never contribute to either count.
 */
internal fun calculateWidgetSummary(
    tasks: List<WhipTask>,
    taskOccurrences: List<TaskOccurrence>,
    habits: List<Habit>,
    habitLogs: List<HabitLog>,
    habitPauses: List<HabitPause>,
    habitSkips: List<HabitSkip>,
    metricEntries: List<MetricEntry>,
    customUnits: List<UnitDefinition>,
    today: LocalDate,
    areaScope: AreaScope,
    zoneId: ZoneId,
): WidgetSummary {
    val scopedTasks = tasks.filter { areaScope.matches(it.areaId) }
    val tasksDue = buildUiState(
        tasks = scopedTasks,
        occurrences = taskOccurrences,
        steps = emptyList(),
        stepStates = emptyList(),
        stepSnapshots = emptyList(),
        today = today,
        showAllUpcomingRecurringOccurrences = false,
        zoneId = zoneId,
    ).today.size

    val projectedLogs = habitLogs + mirrorMetricEntriesAsHabitLogs(habits, metricEntries, customUnits)
    val habitsDue = habits.count { habit ->
        !habit.archived &&
            areaScope.matches(habit.areaId) &&
            habit.reminderNeededOn(
                logs = projectedLogs.filter { it.habitId == habit.id },
                date = today,
                customUnits = customUnits,
                skips = habitSkips.filter { it.habitId == habit.id },
                pauses = habitPauses.filter { it.habitId == habit.id },
            )
    }

    return WidgetSummary(tasksDue = tasksDue, habitsDue = habitsDue)
}

internal fun availableWidgetHeight(
    minHeightDp: Int,
    maxHeightDp: Int,
    landscape: Boolean,
): Int = if (landscape) {
    minHeightDp.takeIf { it > 0 } ?: maxHeightDp
} else {
    maxHeightDp.takeIf { it > 0 } ?: minHeightDp
}

internal fun useCompactWidgetHeader(availableHeightDp: Int, fontScale: Float): Boolean =
    availableHeightDp in 1 until 200 || fontScale >= 1.5f

/** At extreme scaling the secondary line would consume the minimum widget's collection viewport. */
internal fun useSingleLineWidgetRows(fontScale: Float): Boolean = fontScale >= 2f
