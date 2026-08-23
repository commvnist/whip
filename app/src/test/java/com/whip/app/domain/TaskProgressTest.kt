package com.whip.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskProgressTest {
    @Test
    fun scheduledTask_reportsEqualWeightSubtaskProgress() {
        val task = task()
        val scheduled = ScheduledTask(
            task = task,
            originalDate = LocalDate.of(2026, 8, 17),
            scheduledDate = LocalDate.of(2026, 8, 17),
            subtasks = (1L..5L).map { id ->
                ScheduledSubtask(
                    step = step(id, task.id),
                    completed = id <= 3,
                    completedAtMillis = null,
                    title = "Step $id",
                )
            },
        )

        assertEquals(3, scheduled.completedSubtasks)
        assertEquals(5, scheduled.totalSubtasks)
        assertEquals(0.6f, scheduled.subtaskProgress, 0f)
    }

    @Test
    fun scheduledTask_withNoSubtasksHasZeroProgress() {
        assertEquals(0f, ScheduledTask(task(), null, null).subtaskProgress, 0f)
    }

    @Test
    fun resetPolicyShowsEveryActiveStepForTheNextOccurrence() {
        val steps = listOf(step(1, 1), step(2, 1))
        val snapshots = listOf(snapshot(stepId = 1, completed = true))

        assertEquals(
            listOf(1L, 2L),
            visibleTaskStepsForOccurrence(
                steps,
                snapshots,
                occurrenceKey = 11,
                policy = RepeatStepPolicy.Reset,
            ).map(TaskStep::id),
        )
    }

    @Test
    fun carryPolicyOnlyShowsPreviouslyUnfinishedSteps() {
        val steps = listOf(step(1, 1), step(2, 1))
        val snapshots = listOf(
            snapshot(stepId = 1, completed = true),
            snapshot(stepId = 2, completed = false),
        )

        assertEquals(
            listOf(2L),
            visibleTaskStepsForOccurrence(
                steps,
                snapshots,
                occurrenceKey = 11,
                policy = RepeatStepPolicy.CarryUnfinished,
            ).map(TaskStep::id),
        )
    }

    private fun task() = WhipTask(
        id = 1,
        title = "Task",
        notes = "",
        scheduleKind = ScheduleKind.Anytime,
        date = null,
        recurrence = null,
        timeMinutes = null,
        reminderEnabled = false,
        archived = false,
        completedAtMillis = null,
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )

    private fun step(id: Long, taskId: Long) = TaskStep(
        id = id,
        taskId = taskId,
        title = "Step $id",
        position = id.toInt(),
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )

    private fun snapshot(stepId: Long, completed: Boolean) = TaskStepSnapshot(
        stepId = stepId,
        taskId = 1,
        occurrenceKey = 10,
        title = "Step $stepId",
        position = stepId.toInt(),
        notes = "",
        completed = completed,
        completedAtMillis = 1L.takeIf { completed },
    )
}
