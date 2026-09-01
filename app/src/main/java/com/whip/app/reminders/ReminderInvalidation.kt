package com.whip.app.reminders

import com.whip.app.domain.Goal
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.Habit
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.WhipTask

/**
 * Reminder work is derived state. Editing presentation-only metadata must not
 * cancel and recreate platform work, because doing so adds avoidable Binder,
 * database, and scheduler work to the user's Save action.
 */
internal fun Goal.reminderDefinitionChanged(draft: GoalDraft): Boolean =
    type != draft.type ||
        reminderMinutes != draft.reminderMinutes ||
        startDate != draft.startDate ||
        deadline != draft.deadline

internal fun Habit.reminderDefinitionChanged(draft: HabitDraft): Boolean =
    trackingMode != draft.trackingMode ||
        dimension != draft.dimension ||
        unitId != draft.unitId ||
        precision != draft.precision ||
        comparison != draft.comparison ||
        targetMin != draft.targetMin ||
        targetMax != draft.targetMax ||
        targetPeriod != draft.targetPeriod ||
        rollingDays != draft.rollingDays ||
        scheduleType != draft.scheduleType ||
        scheduleInterval != draft.scheduleInterval ||
        weekdays != draft.weekdays ||
        flexibleTimesPerWeek != draft.flexibleTimesPerWeek ||
        startDate != draft.startDate ||
        endType != draft.endType ||
        endDate != draft.endDate ||
        endValue != draft.endValue ||
        quickIncrement != draft.quickIncrement ||
        reminderMinutes.normalizedMinutes() != draft.reminderMinutes.normalizedMinutes() ||
        weekdayReminderMinutes.normalizedWeekdayMinutes() != draft.weekdayReminderMinutes.normalizedWeekdayMinutes() ||
        weekStart != draft.weekStart ||
        sourceMetricId != draft.sourceMetricId

internal fun WhipTask.reminderDefinitionChanged(draft: TaskDraft): Boolean =
    scheduleKind != draft.scheduleKind ||
        date != draft.date ||
        recurrence != draft.recurrence ||
        timeMinutes != draft.timeMinutes ||
        reminderEnabled != draft.reminderEnabled ||
        deadline != draft.deadline ||
        reminderOffsetsMinutes.normalizedMinutes() != draft.reminderOffsetsMinutes.normalizedMinutes() ||
        missedOccurrencePolicy != draft.missedOccurrencePolicy ||
        activeStepIdentityChanged(draft)

private fun WhipTask.activeStepIdentityChanged(draft: TaskDraft): Boolean {
    val currentIds = steps.filterNot { it.archived }.map { it.id }.toSet()
    val proposed = draft.steps.filter { it.title.isNotBlank() }
    return proposed.any { it.id == null } || proposed.mapNotNull { it.id }.toSet() != currentIds
}

private fun List<Int>.normalizedMinutes(): List<Int> = distinct().sorted()

private fun <K> Map<K, List<Int>>.normalizedWeekdayMinutes(): Map<K, List<Int>> =
    mapValues { (_, minutes) -> minutes.normalizedMinutes() }
