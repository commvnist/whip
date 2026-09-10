package com.whip.app.ui

import com.whip.app.core.ReviewSection
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.domain.goalOutcomeScoreOnDate
import com.whip.app.domain.successfulPeriodOutcomeDates
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** A contributing outcome, not a raw entry or a second source of domain scoring rules. */
data class ReviewOutcome(
    val section: ReviewSection,
    val sourceId: Long,
    val sourceKey: String,
    val title: String,
    val date: LocalDate,
    val score: Double,
    val icon: String? = null,
    val archived: Boolean = false,
    val scheduledDate: LocalDate? = null,
    val originalDate: LocalDate? = null,
) {
    val stableKey: String get() = "${section.name}:$sourceKey:$date"
}

internal fun reviewOutcomes(
    tasks: TaskUiState,
    habits: HabitUiState,
    goals: GoalUiState,
    gym: GymUiState,
    sections: Set<ReviewSection>,
    start: LocalDate,
    through: LocalDate,
    zone: ZoneId,
): List<ReviewOutcome> = buildList {
    if (ReviewSection.Tasks in sections) {
        reviewCompletedTasks(tasks).forEach { item ->
            val date = item.completedAtMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
                ?: item.scheduledDate
            if (date != null && date in start..through) add(ReviewOutcome(
                ReviewSection.Tasks, item.task.id, item.stableKey, item.task.title, date, 1.0,
                item.task.icon, item.task.archived, item.scheduledDate, item.originalDate,
            ))
        }
    }
    if (ReviewSection.Habits in sections) {
        (habits.all.map { it.habit } + habits.archived).distinctBy { it.id }.forEach { habit ->
            habit.successfulPeriodOutcomeDates(habits.logs, start, through, habits.pauses, habits.customUnits, habits.skips)
                .forEach { date -> add(ReviewOutcome(
                    ReviewSection.Habits, habit.id, habit.id.toString(), habit.name, date, 1.0,
                    habit.icon, habit.archived,
                )) }
        }
    }
    if (ReviewSection.Goals in sections) {
        val dates = generateSequence(start) { it.plusDays(1) }.takeWhile { it <= through }.toList()
        (goals.active + goals.completed + goals.archived).forEach { projection ->
            dates.forEach { date ->
                val score = goalOutcomeScoreOnDate(projection.goal, projection.entries, projection.milestones, date)
                if (score > 0.0) add(ReviewOutcome(
                    ReviewSection.Goals, projection.goal.id, projection.goal.id.toString(), projection.goal.name,
                    date, score, projection.goal.icon, projection.goal.archived,
                ))
            }
        }
    }
    if (ReviewSection.Gym in sections) {
        gym.history.filter { it.state == WorkoutSessionState.Finished && it.localDate in start..through }.forEach { workout ->
            add(ReviewOutcome(ReviewSection.Gym, workout.id, workout.id.toString(), workout.name,
                workout.localDate, 1.0, archived = workout.archived))
        }
    }
}.sortedWith(compareByDescending<ReviewOutcome> { it.date }.thenBy { it.title }.thenBy { it.stableKey })

internal fun List<ReviewOutcome>.dailyReviewValues(section: ReviewSection, dates: List<LocalDate>): List<Double> {
    val scores = filter { it.section == section }.groupBy { it.date }.mapValues { (_, rows) -> rows.sumOf { it.score } }
    return dates.map { scores[it] ?: 0.0 }
}
