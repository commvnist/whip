package com.whip.app.ui

import com.whip.app.domain.RecurrenceRule
import com.whip.app.domain.RecurrenceUnit
import com.whip.app.domain.MissedOccurrencePolicy
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.WhipTask
import com.whip.app.domain.TaskEffort
import com.whip.app.domain.TaskPriority
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskUpcomingVisibilityTest {
    @Test
    fun compactModeKeepsTheNextOccurrenceOfEachRepeatingTaskAndEveryOneShotTask() {
        val firstSeries = task(1, ScheduleKind.Recurring)
        val secondSeries = task(2, ScheduleKind.Recurring)
        val oneShot = task(3, ScheduleKind.Once)
        val items = listOf(
            occurrence(firstSeries, "2026-08-23"),
            occurrence(oneShot, "2026-08-20"),
            occurrence(firstSeries, "2026-08-19"),
            occurrence(secondSeries, "2026-08-25"),
            occurrence(secondSeries, "2026-08-22"),
        )

        val visible = items.withRecurringOccurrenceVisibility(false)

        assertEquals(
            listOf(3L to "2026-08-20", 1L to "2026-08-19", 2L to "2026-08-22"),
            visible.map { it.task.id to it.scheduledDate.toString() },
        )
    }

    @Test
    fun expandedModeKeepsEveryUpcomingOccurrence() {
        val series = task(1, ScheduleKind.Recurring)
        val items = listOf(
            occurrence(series, "2026-08-19"),
            occurrence(series, "2026-08-20"),
            occurrence(series, "2026-08-21"),
        )

        assertEquals(items, items.withRecurringOccurrenceVisibility(true))
    }

    @Test
    fun listAgendaAndCalendarConsumeTheSamePreferenceAwareUpcomingCollection() {
        val series = task(1, ScheduleKind.Recurring)
        val first = occurrence(series, "2026-08-19")
        val later = occurrence(series, "2026-08-20")
        val firstDate = requireNotNull(first.scheduledDate)
        val laterDate = requireNotNull(later.scheduledDate)
        val compactState = TaskUiState(upcoming = listOf(first, later).withRecurringOccurrenceVisibility(false))
        val expandedState = TaskUiState(upcoming = listOf(first, later).withRecurringOccurrenceVisibility(true))

        assertEquals(listOf(first), compactState.tasksFor(TaskDestination.Upcoming).forPlanningView(TaskPlanningView.List, firstDate))
        assertEquals(listOf(first), compactState.tasksFor(TaskDestination.Upcoming).forPlanningView(TaskPlanningView.Agenda, firstDate))
        assertEquals(listOf(first), compactState.tasksFor(TaskDestination.Upcoming).forPlanningView(TaskPlanningView.Calendar, firstDate))
        assertEquals(listOf(first, later), expandedState.tasksFor(TaskDestination.Upcoming).forPlanningView(TaskPlanningView.List, firstDate))
        assertEquals(listOf(first, later), expandedState.tasksFor(TaskDestination.Upcoming).forPlanningView(TaskPlanningView.Agenda, firstDate))
        assertEquals(listOf(later), expandedState.tasksFor(TaskDestination.Upcoming).forPlanningView(TaskPlanningView.Calendar, laterDate))
    }

    @Test
    fun upcomingIsCappedAtThirtyDaysBeforeAnyPlanningViewRendersIt() {
        val today = LocalDate.of(2026, 8, 18)
        val state = buildUiState(
            tasks = listOf(task(1, ScheduleKind.Recurring)),
            occurrences = emptyList(),
            steps = emptyList(),
            stepStates = emptyList(),
            stepSnapshots = emptyList(),
            today = today,
            showAllUpcomingRecurringOccurrences = true,
        )

        assertEquals(30, state.upcoming.size)
        assertEquals(today.plusDays(1), state.upcoming.first().scheduledDate)
        assertEquals(today.plusDays(30), state.upcoming.last().scheduledDate)
        assertEquals(today.plusDays(30), state.tasksFor(TaskDestination.Upcoming).maxOf { it.scheduledDate!! })
    }

    @Test
    fun missedOccurrencePolicySelectsOldestLatestOrToday() {
        val today = LocalDate.of(2026, 8, 21)
        fun dueDate(policy: MissedOccurrencePolicy): LocalDate? = buildUiState(
            tasks = listOf(task(1, ScheduleKind.Recurring).copy(missedOccurrencePolicy = policy)),
            occurrences = emptyList(),
            steps = emptyList(),
            stepStates = emptyList(),
            stepSnapshots = emptyList(),
            today = today,
            showAllUpcomingRecurringOccurrences = false,
        ).today.singleOrNull()?.scheduledDate

        assertEquals(LocalDate.of(2026, 8, 18), dueDate(MissedOccurrencePolicy.KeepOldest))
        assertEquals(today, dueDate(MissedOccurrencePolicy.KeepLatest))
        assertEquals(today, dueDate(MissedOccurrencePolicy.CurrentOnly))
    }

    @Test
    fun inboxIsSeparateFromTriagedAnytimeAndCapacityNeverOverfills() {
        val captured = task(1, ScheduleKind.Anytime).copy(inbox = true, durationMinutes = 45, priority = TaskPriority.High)
        val quick = task(2, ScheduleKind.Anytime).copy(inbox = false, durationMinutes = 30, effort = TaskEffort.Light)
        val highEffort = task(3, ScheduleKind.Anytime).copy(inbox = true, durationMinutes = 90, effort = TaskEffort.High)
        val state = buildUiState(
            tasks = listOf(captured, quick, highEffort),
            occurrences = emptyList(),
            steps = emptyList(),
            stepStates = emptyList(),
            stepSnapshots = emptyList(),
            today = LocalDate.of(2026, 8, 18),
            showAllUpcomingRecurringOccurrences = false,
        )

        assertEquals(setOf(1L, 3L), state.inbox.mapTo(mutableSetOf()) { it.task.id })
        assertEquals(listOf(2L), state.anytime.map { it.task.id })
        assertEquals(listOf(1L, 2L), selectTasksForCapacity(state.inbox + state.anytime, 75).map { it.task.id })
        assertEquals(75, selectTasksForCapacity(state.inbox + state.anytime, 75).sumOf { it.task.durationMinutes ?: 30 })
    }

    @Test
    fun scheduledDateAndDeadlineHaveIndependentStatus() {
        val today = LocalDate.of(2026, 8, 23)
        val item = task(1, ScheduleKind.Once).copy(
            date = LocalDate.of(2026, 8, 20),
            deadline = LocalDate.of(2026, 8, 30),
        )

        val projected = buildUiState(
            tasks = listOf(item),
            occurrences = emptyList(),
            steps = emptyList(),
            stepStates = emptyList(),
            stepSnapshots = emptyList(),
            today = today,
            showAllUpcomingRecurringOccurrences = false,
        ).today.single()

        assertEquals(true, projected.isPastScheduledDate)
        assertEquals(false, projected.isDeadlineOverdue)
        assertEquals(LocalDate.of(2026, 8, 30), projected.task.deadline)
    }

    @Test
    fun planRankingUsesPriorityAndDeadlineNotEffortAsImportance() {
        val start = LocalDate.of(2026, 8, 23)
        val highEffortLaterDeadline = ScheduledTask(
            task(1, ScheduleKind.Anytime).copy(
                effort = TaskEffort.High,
                deadline = start.plusDays(7),
                durationMinutes = 30,
            ),
            null,
            null,
        )
        val lightEarlierDeadline = ScheduledTask(
            task(2, ScheduleKind.Anytime).copy(
                effort = TaskEffort.Light,
                deadline = start.plusDays(1),
                durationMinutes = 30,
            ),
            null,
            null,
        )

        assertEquals(
            listOf(2L),
            selectTasksForCapacity(listOf(highEffortLaterDeadline, lightEarlierDeadline), 30).map { it.task.id },
        )
    }

    @Test
    fun currentOnlyChangesQueueProjectionWithoutCreatingHistoryFacts() {
        val today = LocalDate.of(2026, 8, 21)
        val state = buildUiState(
            tasks = listOf(task(1, ScheduleKind.Recurring).copy(missedOccurrencePolicy = MissedOccurrencePolicy.CurrentOnly)),
            occurrences = emptyList(),
            steps = emptyList(),
            stepStates = emptyList(),
            stepSnapshots = emptyList(),
            today = today,
            showAllUpcomingRecurringOccurrences = false,
        )

        assertEquals(today, state.today.single().scheduledDate)
        assertEquals(emptyList<ScheduledTask>(), state.completed)
        assertEquals(emptyList<com.whip.app.domain.TaskOccurrence>(), state.occurrences)
    }

    @Test(timeout = 5_000)
    fun tenThousandDailySeriesBuildOnlyTheNeededDefaultOccurrences() {
        val today = LocalDate.of(2026, 8, 18)
        val oldStart = LocalDate.of(2016, 8, 18)
        val tasks = List(10_000) { index ->
            task(index + 1L, ScheduleKind.Recurring).copy(
                date = oldStart,
                recurrence = RecurrenceRule(RecurrenceUnit.Days, startDate = oldStart),
            )
        }

        val state = buildUiState(
            tasks = tasks,
            occurrences = emptyList(),
            steps = emptyList(),
            stepStates = emptyList(),
            stepSnapshots = emptyList(),
            today = today,
            showAllUpcomingRecurringOccurrences = false,
        )

        assertEquals(10_000, state.today.size)
        assertEquals(10_000, state.upcoming.size)
        assertEquals(10_000, state.planning.size)
    }

    private fun occurrence(task: WhipTask, date: String): ScheduledTask {
        val localDate = LocalDate.parse(date)
        return ScheduledTask(task, localDate, localDate)
    }

    private fun task(id: Long, kind: ScheduleKind): WhipTask {
        val startDate = LocalDate.of(2026, 8, 18)
        return WhipTask(
            id = id,
            title = "Task $id",
            notes = "",
            scheduleKind = kind,
            date = startDate,
            recurrence = if (kind == ScheduleKind.Recurring) {
                RecurrenceRule(RecurrenceUnit.Days, startDate = startDate)
            } else {
                null
            },
            timeMinutes = null,
            reminderEnabled = false,
            archived = false,
            completedAtMillis = null,
            createdAtMillis = id,
            updatedAtMillis = id,
        )
    }
}
