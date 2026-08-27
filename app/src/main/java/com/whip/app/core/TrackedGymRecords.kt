package com.whip.app.core

import com.whip.app.domain.Exercise
import com.whip.app.domain.ExerciseTrackingType
import com.whip.app.domain.PersonalRecord
import com.whip.app.domain.PersonalRecordType

/** A user-selected benchmark shown on the Progress dashboard. */
data class TrackedGymRecord(
    val exerciseUuid: String,
    val type: PersonalRecordType,
    val secondaryValue: Double? = null,
    val machineProfileUuid: String? = null,
    val position: Int = 0,
)

internal fun normalizeTrackedGymRecords(records: Iterable<TrackedGymRecord>): List<TrackedGymRecord> = records
    .filter { selection ->
        selection.exerciseUuid.isNotBlank() &&
            selection.type !in specificTargetPersonalRecordTypes &&
            selection.secondaryValue == null
    }
    .sortedBy(TrackedGymRecord::position)
    .distinctBy(TrackedGymRecord::stableKey)
    .take(200)
    .mapIndexed { index, selection -> selection.copy(position = index) }

internal fun TrackedGymRecord.stableKey(): String = listOf(
    exerciseUuid,
    type.name,
    secondaryValue?.toString().orEmpty(),
    machineProfileUuid.orEmpty(),
).joinToString("|")

private val specificTargetPersonalRecordTypes: Set<PersonalRecordType> = setOf(
    PersonalRecordType.BestWeightForRepCount,
    PersonalRecordType.MaxRepetitionsForWeight,
)

fun Exercise.supportedTrackedRecordTypes(): List<PersonalRecordType> = when (trackingType) {
    ExerciseTrackingType.WeightReps -> listOf(
        PersonalRecordType.EstimatedOneRepMax,
        PersonalRecordType.MaxWeight,
        PersonalRecordType.SetVolume,
        PersonalRecordType.ExerciseWorkoutVolume,
    )
    ExerciseTrackingType.BodyweightReps,
    ExerciseTrackingType.AssistedBodyweightReps,
    ExerciseTrackingType.RepsOnly,
    -> listOf(PersonalRecordType.MaxRepetitions)
    ExerciseTrackingType.WeightOnly -> listOf(PersonalRecordType.MaxWeight)
    ExerciseTrackingType.DistanceDuration -> listOf(
        PersonalRecordType.MinPace,
        PersonalRecordType.MaxDistance,
        PersonalRecordType.MaxDuration,
        PersonalRecordType.MaxSpeed,
    )
    ExerciseTrackingType.WeightDuration -> listOf(PersonalRecordType.MaxWeight, PersonalRecordType.MaxDuration)
    ExerciseTrackingType.RepsDuration -> listOf(PersonalRecordType.MaxRepetitions, PersonalRecordType.MaxDuration)
    ExerciseTrackingType.DistanceOnly -> listOf(PersonalRecordType.MaxDistance)
    ExerciseTrackingType.DurationOnly -> listOf(PersonalRecordType.MaxDuration)
}

fun Exercise.recommendedTrackedRecordTypes(): List<PersonalRecordType> = when (trackingType) {
    ExerciseTrackingType.WeightReps -> listOf(PersonalRecordType.EstimatedOneRepMax, PersonalRecordType.MaxWeight)
    ExerciseTrackingType.BodyweightReps,
    ExerciseTrackingType.AssistedBodyweightReps,
    ExerciseTrackingType.RepsOnly,
    -> listOf(PersonalRecordType.MaxRepetitions)
    ExerciseTrackingType.WeightOnly -> listOf(PersonalRecordType.MaxWeight)
    ExerciseTrackingType.DistanceDuration -> listOf(PersonalRecordType.MinPace, PersonalRecordType.MaxDistance)
    ExerciseTrackingType.WeightDuration -> listOf(PersonalRecordType.MaxWeight, PersonalRecordType.MaxDuration)
    ExerciseTrackingType.RepsDuration -> listOf(PersonalRecordType.MaxRepetitions, PersonalRecordType.MaxDuration)
    ExerciseTrackingType.DistanceOnly -> listOf(PersonalRecordType.MaxDistance)
    ExerciseTrackingType.DurationOnly -> listOf(PersonalRecordType.MaxDuration)
}

fun TrackedGymRecord.resolveForExercise(
    exerciseId: Long,
    currentRecords: List<PersonalRecord>,
): PersonalRecord? {
    val candidates = currentRecords.filter { record ->
        record.current && record.exerciseId == exerciseId && record.type == type &&
            secondaryValue == null && record.secondaryValue == null
    }
    val exactScope = candidates.filter { it.machineProfileUuidSnapshot == machineProfileUuid }
    if (exactScope.isNotEmpty()) return exactScope.maxByOrNull(PersonalRecord::achievedAtMillis)

    // A recommendation created before the first workout has no machine scope. It may safely
    // bind once there is exactly one equipment context; Whip never merges multiple contexts.
    if (machineProfileUuid == null && candidates.none { it.machineProfileUuidSnapshot == null }) {
        val scopes = candidates.map(PersonalRecord::machineProfileUuidSnapshot).distinct()
        if (scopes.size == 1) return candidates.maxByOrNull(PersonalRecord::achievedAtMillis)
    }
    return null
}
