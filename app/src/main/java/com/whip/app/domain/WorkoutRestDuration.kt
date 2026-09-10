package com.whip.app.domain

enum class WorkoutRestSource { WorkoutOverride, SetPrescription, ExerciseDefault, AppDefault }

data class WorkoutRestDuration(val seconds: Int, val source: WorkoutRestSource)

/** A prescribed zero means no rest; it must not fall through to a default. */
fun resolveWorkoutRestDuration(
    workoutOverrideSeconds: Int?,
    setRestSeconds: Int?,
    exerciseDefaultSeconds: Int?,
    appDefaultSeconds: Int,
): WorkoutRestDuration = when {
    workoutOverrideSeconds != null -> WorkoutRestDuration(workoutOverrideSeconds, WorkoutRestSource.WorkoutOverride)
    setRestSeconds != null -> WorkoutRestDuration(setRestSeconds, WorkoutRestSource.SetPrescription)
    exerciseDefaultSeconds != null -> WorkoutRestDuration(exerciseDefaultSeconds, WorkoutRestSource.ExerciseDefault)
    else -> WorkoutRestDuration(appDefaultSeconds, WorkoutRestSource.AppDefault)
}
