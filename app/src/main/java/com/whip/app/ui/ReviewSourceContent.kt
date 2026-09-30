package com.whip.app.ui

import com.whip.app.domain.Goal
import com.whip.app.domain.GoalMilestone
import com.whip.app.domain.Habit
import com.whip.app.domain.HabitLog
import com.whip.app.domain.HabitPause
import com.whip.app.domain.HabitSkip
import com.whip.app.domain.MeasurementEntry
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.TaskOccurrence
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.WorkoutSession

/** Exactly the persisted evidence used by reviewOutcomes; clocks and live progress are not evidence. */
internal data class ReviewSourceContent(
    val completedTasks: List<ScheduledTask>,
    val archivedTasks: List<ScheduledTask>,
    val occurrences: List<TaskOccurrence>,
    val habits: List<Habit>,
    val logs: List<HabitLog>,
    val pauses: List<HabitPause>,
    val skips: List<HabitSkip>,
    val units: List<UnitDefinition>,
    val goals: List<Triple<Goal, List<MeasurementEntry>, List<GoalMilestone>>>,
    val workouts: List<WorkoutSession>,
)

internal fun reviewSourceContent(tasks: TaskUiState, habits: HabitUiState, goals: GoalUiState, gym: GymUiState) =
    ReviewSourceContent(
        tasks.completed, tasks.archived, tasks.occurrences,
        habits.all.map { it.habit } + habits.archived, habits.logs, habits.pauses, habits.skips, habits.customUnits,
        (goals.active + goals.completed + goals.archived).map { Triple(it.goal, it.entries, it.milestones) },
        reviewFinishedWorkouts(gym),
    )
