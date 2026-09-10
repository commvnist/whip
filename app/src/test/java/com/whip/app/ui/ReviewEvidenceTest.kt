package com.whip.app.ui

import com.whip.app.domain.*
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ReviewEvidenceTest {
    private val today = LocalDate.of(2026, 9, 10)

    @Test fun archiveKeepsCompletedOnceAndRecurringEvidenceButDoesNotCountSkippedOrOpenWork() {
        val once = task(1, completed = 1234)
        val recurring = task(2, recurring = true)
        val records = listOf(
            TaskOccurrence(2, today.minusDays(1), today, OccurrenceState.Completed, 5678),
            TaskOccurrence(2, today, today, OccurrenceState.Completed, 9012),
            TaskOccurrence(2, today.minusDays(2), today.minusDays(2), OccurrenceState.Skipped, 4567),
            TaskOccurrence(2, today.plusDays(1), today.plusDays(1), OccurrenceState.Open, null),
        )
        val state = TaskUiState(archived = listOf(item(once), item(recurring), item(task(3))), occurrences = records)
        val completed = reviewCompletedTasks(state)
        assertEquals(listOf("1:${today.toEpochDay()}", "2:${today.minusDays(1).toEpochDay()}", "2:${today.toEpochDay()}"), completed.map { it.stableKey })
        assertEquals(listOf(1234L, 5678L, 9012L), completed.map { it.completedAtMillis })
        assertEquals(listOf(today, today, today), completed.map { it.scheduledDate })
    }

    @Test fun archivingChangesCollectionMembershipWithoutChangingReviewHistory() {
        val once = item(task(1, completed = 1234).copy(archived = false))
        val recurring = task(2, recurring = true).copy(archived = false)
        val occurrence = TaskOccurrence(2, today, today, OccurrenceState.Completed, 5678)
        val completedRecurring = ScheduledTask(recurring, today, today, 5678, OccurrenceState.Completed)
        val before = TaskUiState(completed = listOf(once, completedRecurring), occurrences = listOf(occurrence))
        val after = TaskUiState(archived = listOf(once.copy(task = once.task.copy(archived = true)), item(recurring.copy(archived = true))), occurrences = listOf(occurrence))
        fun facts(state: TaskUiState) = reviewCompletedTasks(state).map { Triple(it.stableKey, it.scheduledDate, it.completedAtMillis) }
        assertEquals(facts(before), facts(after))
    }

    @Test fun archivedReviewEvidenceStillRespectsTheProductivityArea() {
        val work = task(1, completed = 1234)
        val personal = task(2, completed = 5678).copy(areaId = "personal")
        val state = TaskUiState(taskEntities = listOf(work, personal), archived = listOf(item(work), item(personal)))
        assertEquals(listOf(1L), reviewCompletedTasks(state.forArea(AreaScope.One("work"))).map { it.task.id })
    }

    @Test fun habitAreaScopeKeepsArchivedHistoryAndFiltersEveryRelatedProjection() {
        val active = habit(1, "work", false)
        val archived = habit(2, "work", true)
        val other = habit(3, "personal", true)
        val state = HabitUiState(
            all = listOf(progress(active)), archived = listOf(archived, other),
            archivedProgress = listOf(progress(archived), progress(other)),
            logs = listOf(active, archived, other).map { log(it.id) },
            pauses = listOf(archived, other).map { HabitPause(it.id, it.id, today, today, "") },
            skips = listOf(archived, other).map { HabitSkip("skip-${it.id}", it.id, today, 1, 1, 1) },
        ).forArea(AreaScope.One("work"))
        assertEquals(listOf(1L), state.all.map { it.habit.id })
        assertEquals(listOf(2L), state.archived.map { it.id })
        assertEquals(listOf(2L), state.archivedProgress.map { it.habit.id })
        assertEquals(listOf(1L, 2L), state.logs.map { it.habitId })
        assertEquals(listOf(2L), state.pauses.map { it.habitId })
        assertEquals(listOf(2L), state.skips.map { it.habitId })
    }

    private fun task(id: Long, completed: Long? = null, recurring: Boolean = false) = WhipTask(
        id, "Task $id", "", if (recurring) ScheduleKind.Recurring else ScheduleKind.Once,
        if (recurring) null else today, if (recurring) RecurrenceRule(RecurrenceUnit.Days, startDate = today.minusDays(2)) else null,
        null, false, true, completed, 1, 1, areaId = "work",
    )
    private fun item(task: WhipTask) = ScheduledTask(task, task.date, task.date, task.completedAtMillis)
    private fun progress(habit: Habit) = HabitDayProgress(habit, today, true, 1.0, HabitLogStatus.Success, true, emptyList(), 1, 1.0)
    private fun log(id: Long) = HabitLog(id, "log-$id", id, 1.0, 1.0, "count", HabitLogStatus.Success,
        Instant.EPOCH, today, "UTC", 0, "", MeasurementSourceType.Manual, null, null, 1, 1)
    private fun habit(id: Long, area: String, archived: Boolean) = Habit(
        id = id, uuid = "habit-$id", measurementId = "measurement-$id", name = "Habit", notes = "", area = area,
        areaId = area, tags = emptyList(), icon = "✓", trackingMode = HabitTrackingMode.CheckOff,
        dimension = UnitDimension.Count, unitId = "count", precision = 0, comparison = TargetComparison.AtLeast,
        targetMin = 1.0, targetMax = null, targetPeriod = TargetPeriod.Day, rollingDays = null,
        scheduleType = HabitScheduleType.Daily, scheduleInterval = 1, weekdays = emptySet(), flexibleTimesPerWeek = null,
        startDate = today, endType = HabitEndType.Never, endDate = null, endValue = null,
        quickIncrement = 1.0, quickActions = emptyList(), reminderMinutes = emptyList(), weekdayReminderMinutes = emptyMap(),
        weekStart = DayOfWeek.MONDAY, timerStartedAtMillis = null, pinned = false, position = 0,
        archived = archived, paused = false, createdAtMillis = 1, updatedAtMillis = 1,
    )
}
