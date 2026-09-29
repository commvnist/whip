package com.whip.app.ui

import com.whip.app.domain.Exercise
import com.whip.app.domain.WorkoutExercise
import com.whip.app.domain.WorkoutSession
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.domain.WorkoutSet

/** Prefer actual completed work, including retained exercise identities, over library order. */
internal fun mostRecentlyPerformedExerciseId(
    exercises: List<Exercise>, sessions: List<WorkoutSession>,
    placements: List<WorkoutExercise>, sets: List<WorkoutSet>,
): Long? {
    return mostRecentlyPerformedPlacement(exercises.mapTo(mutableSetOf(), Exercise::id), sessions, placements, sets)
        ?.exerciseId ?: exercises.firstOrNull()?.id
}

internal fun mostRecentlyPerformedPlacement(
    exerciseIds: Set<Long>, sessions: List<WorkoutSession>,
    placements: List<WorkoutExercise>, sets: List<WorkoutSet>,
): WorkoutExercise? {
    val finished = sessions.filter { it.state == WorkoutSessionState.Finished && !it.archived }.associateBy(WorkoutSession::id)
    val performed = sets.asSequence().filter { it.completed && it.deletedAtMillis == null }
        .mapTo(mutableSetOf(), WorkoutSet::workoutExerciseId)
    return placements.asSequence().filter { it.exerciseId in exerciseIds && it.id in performed && it.sessionId in finished }
        .maxWithOrNull(compareBy<WorkoutExercise> { finished.getValue(it.sessionId).startedAt }.thenBy { -it.position })
}
