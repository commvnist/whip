package com.whip.app.domain

import java.time.Instant
import java.time.LocalDate
import kotlin.math.max

enum class ExerciseTrackingType(val label: String) {
    WeightReps("Weight + repetitions"),
    BodyweightReps("Bodyweight + repetitions"),
    AssistedBodyweightReps("Assisted bodyweight + repetitions"),
    RepsOnly("Repetitions only"),
    WeightOnly("Weight only"),
    DistanceDuration("Distance + duration"),
    WeightDuration("Weight + duration"),
    RepsDuration("Repetitions + duration"),
    DistanceOnly("Distance only"),
    DurationOnly("Duration only"),
}

enum class EstimatedOneRepMaxFormula {
    Epley,
    Brzycki,
}

enum class BodyweightLoadPolicy {
    ExternalWeightOnly,
    BodyweightPlusExternal,
    EffectiveBodyweightPercentage,
}

enum class LoadInterpretation(val label: String) {
    Total("Total load"),
    PerHand("Per hand"),
    PerSide("Per side"),
    AddedLoad("Added external load"),
    BodyweightPlusExternal("Bodyweight plus external load"),
    BodyweightPercentage("Effective bodyweight percentage plus load"),
    AssistedSubtraction("Bodyweight minus assistance"),
    MachineDisplayedMass("Machine displayed mass"),
    OrdinalSetting("Ordinal setting (no mass unless mapped)"),
}

enum class MachineLoadType(val label: String) {
    Mass("Weight stack / mass"),
    Level("Numbered stack / level"),
}

enum class MachineStackMode(val label: String) {
    Single("Single stack / resistance source"),
    DualCombined("Two stacks, entered once and combined"),
    DualIndependent("Independent stacks / arms"),
}

data class GymMachineDraft(
    val exerciseId: Long,
    val name: String,
    val location: String = "",
    val details: String = "",
    val loadType: MachineLoadType = MachineLoadType.Mass,
    val unitId: String = "kilogram",
    val levelLabel: String = "level",
    val availableLoads: List<Double> = emptyList(),
    val loadInterpretation: LoadInterpretation = LoadInterpretation.Total,
    val baseLoadKg: Double? = null,
    val configurationGroupId: String = "",
    val configurationVersion: Int = 1,
    val seatPosition: String = "",
    val backPosition: String = "",
    val attachment: String = "",
    val pulleyRatio: Double = 1.0,
    val stackMode: MachineStackMode = MachineStackMode.Single,
    val addOnPlateKg: Double? = null,
    val stackLabels: List<String> = emptyList(),
    /** Displayed setting -> actual canonical kilograms. */
    val massMappingKg: Map<Double, Double> = emptyMap(),
    val compatibleForComparison: Boolean = false,
)

data class GymMachine(
    val id: Long,
    val uuid: String,
    val exerciseId: Long,
    val name: String,
    val location: String,
    val details: String,
    val loadType: MachineLoadType,
    val unitId: String,
    val levelLabel: String,
    val availableLoads: List<Double>,
    val loadInterpretation: LoadInterpretation,
    val baseLoadKg: Double?,
    val archived: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val configurationGroupId: String = uuid,
    val configurationVersion: Int = 1,
    val seatPosition: String = "",
    val backPosition: String = "",
    val attachment: String = "",
    val pulleyRatio: Double = 1.0,
    val stackMode: MachineStackMode = MachineStackMode.Single,
    val addOnPlateKg: Double? = null,
    val stackLabels: List<String> = emptyList(),
    val massMappingKg: Map<Double, Double> = emptyMap(),
    val compatibleForComparison: Boolean = false,
) {
    val displayName: String
        get() = if (location.isBlank()) name else "$name · $location"
}

enum class WorkoutSessionState {
    Active,
    Finished,
    Discarded,
}

enum class WorkoutSetClassification {
    WarmUp,
    Working,
    BackOff,
    Drop,
    Amrap,
    Failure,
}

enum class WorkoutGroupType {
    Superset,
    Circuit,
}

enum class RoutineLoadPrescriptionType(val label: String) {
    Absolute("Exact load"),
    PercentOneRepMax("% of estimated 1RM"),
    PercentTrainingMax("% of training max"),
}

data class ExerciseDraft(
    val name: String,
    val trackingType: ExerciseTrackingType = ExerciseTrackingType.WeightReps,
    val notes: String = "",
    val equipment: String = "",
    val primaryMuscles: String = "",
    val secondaryMuscles: String = "",
    val weightUnitId: String = "kilogram",
    val weightIncrement: Double = 2.5,
    val repetitionIncrement: Int = 1,
    val defaultRestSeconds: Int? = 120,
    val defaultGraphMetric: String = "EstimatedOneRepMax",
    val oneRepMaxFormula: EstimatedOneRepMaxFormula = EstimatedOneRepMaxFormula.Epley,
    val barWeightKg: Double? = 20.0,
    val availablePlatesKg: List<Double> = listOf(20.0, 15.0, 10.0, 5.0, 2.5, 1.25),
    val includeInVolume: Boolean = true,
    val includeInPersonalRecords: Boolean = true,
    val bodyweightLoadPolicy: BodyweightLoadPolicy = BodyweightLoadPolicy.ExternalWeightOnly,
    val effectiveBodyweightPercent: Double = 100.0,
    val showRpe: Boolean? = null,
    val showRir: Boolean? = null,
    val showTempo: Boolean? = null,
    val categoryIds: Set<Long> = emptySet(),
    val loadInterpretation: LoadInterpretation = LoadInterpretation.Total,
)

data class Exercise(
    val id: Long,
    val uuid: String,
    val name: String,
    val trackingType: ExerciseTrackingType,
    val notes: String,
    val equipment: String,
    val primaryMuscles: String,
    val secondaryMuscles: String,
    val weightUnitId: String,
    val weightIncrement: Double,
    val repetitionIncrement: Int,
    val defaultRestSeconds: Int?,
    val defaultGraphMetric: String,
    val oneRepMaxFormula: EstimatedOneRepMaxFormula,
    val barWeightKg: Double?,
    val availablePlatesKg: List<Double>,
    val includeInVolume: Boolean,
    val includeInPersonalRecords: Boolean,
    val bodyweightLoadPolicy: BodyweightLoadPolicy,
    val effectiveBodyweightPercent: Double,
    val showRpe: Boolean?,
    val showRir: Boolean?,
    val showTempo: Boolean?,
    val favorite: Boolean,
    val position: Int,
    val archived: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val loadInterpretation: LoadInterpretation = LoadInterpretation.Total,
)

data class ExerciseCategory(
    val id: Long,
    val uuid: String,
    val name: String,
    val kind: String,
    val colorArgb: Long?,
    val position: Int,
    val archived: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

data class ExerciseCategoryLink(val exerciseId: Long, val categoryId: Long)

data class WorkoutSession(
    val id: Long,
    val uuid: String,
    val name: String,
    val notes: String,
    val startedAt: Instant,
    val endedAt: Instant?,
    val localDate: LocalDate,
    val zoneId: String,
    val state: WorkoutSessionState,
    val keepScreenAwake: Boolean,
    val restTimerDeadlineMillis: Long?,
    val restTimerDurationSeconds: Int?,
    val archived: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val sourceRoutineId: Long? = null,
)

data class WorkoutGroup(
    val id: Long,
    val uuid: String,
    val sessionId: Long,
    val name: String,
    val type: WorkoutGroupType,
    val position: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

data class WorkoutExercise(
    val id: Long,
    val uuid: String,
    val sessionId: Long,
    val exerciseId: Long,
    val position: Int,
    val notes: String,
    val groupId: Long?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val machineProfileUuidSnapshot: String? = null,
    val machineId: Long? = null,
    val machineNameSnapshot: String = "",
    val machineLoadTypeSnapshot: MachineLoadType? = null,
    val machineUnitIdSnapshot: String = "",
    val machineLevelLabelSnapshot: String = "",
    val loadInterpretationSnapshot: LoadInterpretation = LoadInterpretation.Total,
    val baseLoadKgSnapshot: Double? = null,
    val trackingTypeSnapshot: ExerciseTrackingType = ExerciseTrackingType.WeightReps,
    val bodyweightLoadPolicySnapshot: BodyweightLoadPolicy = BodyweightLoadPolicy.ExternalWeightOnly,
    val effectiveBodyweightPercentSnapshot: Double = 100.0,
    val oneRepMaxFormulaSnapshot: EstimatedOneRepMaxFormula = EstimatedOneRepMaxFormula.Epley,
    val includeInVolumeSnapshot: Boolean = true,
    val includeInPersonalRecordsSnapshot: Boolean = true,
    val exerciseWeightUnitSnapshot: String = "kilogram",
    val loadMultiplierSnapshot: Double = 1.0,
    val machineConfigurationGroupSnapshot: String = "",
    val machineConfigurationVersionSnapshot: Int = 1,
    val machineConfigurationSnapshot: String = "",
    val machinePulleyRatioSnapshot: Double = 1.0,
    val machineStackModeSnapshot: MachineStackMode = MachineStackMode.Single,
    val machineAddOnPlateKgSnapshot: Double? = null,
    val machineMassMappingKgSnapshot: Map<Double, Double> = emptyMap(),
    val alternativeExerciseIdsSnapshot: List<Long> = emptyList(),
)

/** Stable equipment partition. Numeric IDs are retained only as live-profile links. */
val WorkoutExercise.equipmentScopeKey: String?
    get() = machineProfileUuidSnapshot ?: machineId?.let { "legacy-machine-id:$it" }

fun WorkoutExercise.applyPolicySnapshot(exercise: Exercise): Exercise = exercise.copy(
    trackingType = trackingTypeSnapshot,
    weightUnitId = exerciseWeightUnitSnapshot,
    bodyweightLoadPolicy = bodyweightLoadPolicySnapshot,
    effectiveBodyweightPercent = effectiveBodyweightPercentSnapshot,
    oneRepMaxFormula = oneRepMaxFormulaSnapshot,
    includeInVolume = includeInVolumeSnapshot,
    includeInPersonalRecords = includeInPersonalRecordsSnapshot,
    loadInterpretation = loadInterpretationSnapshot,
)

data class WorkoutSetDraft(
    val weight: Double? = null,
    val weightUnitId: String = "kilogram",
    val reps: Int? = null,
    /** Optional inclusive upper target for a prescribed repetition range. */
    val repsMax: Int? = null,
    val distance: Double? = null,
    val distanceUnitId: String = "kilometre",
    val durationSeconds: Long? = null,
    val bodyweightKg: Double? = null,
    val planned: Boolean = false,
    val completed: Boolean = false,
    val classification: WorkoutSetClassification = WorkoutSetClassification.Working,
    val note: String = "",
    val rpe: Double? = null,
    val rir: Double? = null,
    val tempo: String = "",
    val restSeconds: Int? = null,
    val machineLoadValue: Double? = null,
    val unilateral: Boolean = false,
    val loadPrescriptionType: RoutineLoadPrescriptionType = RoutineLoadPrescriptionType.Absolute,
    val loadPercentage: Double? = null,
)

fun validateWorkoutSetDraft(
    draft: WorkoutSetDraft,
    trackingType: ExerciseTrackingType,
    machineLoadType: MachineLoadType? = null,
    loadInterpretation: LoadInterpretation = LoadInterpretation.Total,
) {
    val decimalValues = listOfNotNull(
        draft.weight,
        draft.distance,
        draft.bodyweightKg,
        draft.rpe,
        draft.rir,
        draft.machineLoadValue,
    )
    require(decimalValues.all(Double::isFinite)) { "Set values must be finite numbers" }
    require(draft.weight == null || draft.weight >= 0.0 || loadInterpretation == LoadInterpretation.AddedLoad) {
        "Weight cannot be negative unless it represents added external load"
    }
    require(draft.distance == null || draft.distance >= 0.0) { "Distance cannot be negative" }
    require(draft.bodyweightKg == null || draft.bodyweightKg > 0.0) { "Bodyweight must be positive" }
    require(draft.reps == null || draft.reps >= 0) { "Repetitions cannot be negative" }
    require(draft.repsMax == null || draft.repsMax >= 0) { "Maximum repetitions cannot be negative" }
    require(draft.reps == null || draft.repsMax == null || draft.repsMax >= draft.reps) {
        "Maximum repetitions must be at least the minimum"
    }
    require(draft.durationSeconds == null || draft.durationSeconds >= 0) { "Duration cannot be negative" }
    require(draft.rpe == null || draft.rpe in 1.0..10.0) { "RPE must be between 1 and 10" }
    require(draft.rir == null || draft.rir in 0.0..10.0) { "RIR must be between 0 and 10" }
    require(draft.restSeconds == null || draft.restSeconds in 0..86_400) { "Rest must be between 0 and 86,400 seconds" }
    require(draft.machineLoadValue == null || draft.machineLoadValue >= 0.0) { "Machine setting cannot be negative" }
    if (!draft.completed) return

    val needsWeight = trackingType in setOf(
        ExerciseTrackingType.WeightReps,
        ExerciseTrackingType.WeightOnly,
        ExerciseTrackingType.WeightDuration,
    )
    if (needsWeight) {
        when (machineLoadType) {
            MachineLoadType.Level -> require(draft.machineLoadValue != null) { "Enter the machine setting before completing the set" }
            else -> require(
                draft.weight != null && (draft.weight > 0.0 || loadInterpretation == LoadInterpretation.AddedLoad),
            ) { "Enter the load before completing the set" }
        }
    }
    if (trackingType == ExerciseTrackingType.AssistedBodyweightReps && machineLoadType != null) {
        require(draft.machineLoadValue != null || draft.weight != null) { "Enter the assistance before completing the set" }
    }
    if (trackingType in setOf(
            ExerciseTrackingType.WeightReps,
            ExerciseTrackingType.BodyweightReps,
            ExerciseTrackingType.AssistedBodyweightReps,
            ExerciseTrackingType.RepsOnly,
            ExerciseTrackingType.RepsDuration,
        )
    ) {
        require((draft.reps ?: 0) > 0) { "Enter at least one repetition before completing the set" }
    }
    if (trackingType in setOf(ExerciseTrackingType.DistanceDuration, ExerciseTrackingType.DistanceOnly)) {
        require((draft.distance ?: 0.0) > 0.0) { "Enter a positive distance before completing the set" }
    }
    if (trackingType in setOf(
            ExerciseTrackingType.DistanceDuration,
            ExerciseTrackingType.WeightDuration,
            ExerciseTrackingType.RepsDuration,
            ExerciseTrackingType.DurationOnly,
        )
    ) {
        require((draft.durationSeconds ?: 0L) > 0L) { "Enter a positive duration before completing the set" }
    }
}

data class WorkoutSet(
    val id: Long,
    val uuid: String,
    val workoutExerciseId: Long,
    val position: Int,
    val classification: WorkoutSetClassification,
    val planned: Boolean,
    val completed: Boolean,
    val canonicalWeightKg: Double?,
    val enteredWeight: Double?,
    val enteredWeightUnitId: String?,
    val repetitions: Int?,
    val canonicalDistanceMetres: Double?,
    val enteredDistance: Double?,
    val enteredDistanceUnitId: String?,
    val durationSeconds: Long?,
    val bodyweightKg: Double?,
    val note: String,
    val rpe: Double?,
    val rir: Double?,
    val tempo: String,
    val restSeconds: Int?,
    val completedAtMillis: Long?,
    val deletedAtMillis: Long?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val machineLoadValue: Double? = null,
    val unilateral: Boolean = false,
    val prescribedCanonicalWeightKg: Double? = null,
    val prescribedEnteredWeight: Double? = null,
    val prescribedWeightUnitId: String? = null,
    val prescribedRepetitions: Int? = null,
    val prescribedRpe: Double? = null,
    val prescribedRir: Double? = null,
    val prescribedDurationSeconds: Long? = null,
    val prescribedMachineLoadValue: Double? = null,
    val prescribedRepetitionsMax: Int? = null,
    val prescriptionSourceLabel: String = "",
)

data class WorkoutSummary(
    val exerciseCount: Int,
    val completedSetCount: Int,
    val repetitions: Int,
    val volumeKg: Double,
    val distanceMetres: Double,
    val durationSeconds: Long,
    val elapsedSeconds: Long,
    val highestEstimatedOneRepMaxKg: Double?,
)

data class RoutineDraft(
    val name: String,
    val notes: String = "",
    val days: List<RoutineDayDraft>,
)

data class RoutineDayDraft(
    val name: String,
    val exercises: List<RoutineExerciseDraft>,
)

data class RoutineExerciseDraft(
    val exerciseId: Long,
    val notes: String = "",
    val groupKey: String? = null,
    val plannedSets: List<WorkoutSetDraft> = emptyList(),
    val copyPreviousWorkout: Boolean = false,
    val machineId: Long? = null,
    val equipmentBindingState: RoutineEquipmentBindingState = if (machineId == null) {
        RoutineEquipmentBindingState.None
    } else {
        RoutineEquipmentBindingState.Resolved
    },
    val machineProfileUuidSnapshot: String? = null,
    val machineNameSnapshot: String = "",
    val machineLoadTypeSnapshot: MachineLoadType? = null,
    val machineUnitIdSnapshot: String = "",
    val machineLevelLabelSnapshot: String = "",
    val machineLoadInterpretationSnapshot: LoadInterpretation = LoadInterpretation.Total,
    val machineConfigurationGroupSnapshot: String = "",
    val machineConfigurationVersionSnapshot: Int = 1,
    val machineConfigurationSnapshot: String = "",
    val trainingMaxPercent: Double = 90.0,
    val progressionPercentages: List<Double> = emptyList(),
    val alternativeExerciseIds: List<Long> = emptyList(),
)

enum class RoutineEquipmentBindingState {
    None,
    Resolved,
    NeedsEquipment,
}

data class GymRoutine(
    val id: Long,
    val uuid: String,
    val name: String,
    val notes: String,
    val position: Int,
    val archived: Boolean,
    val pinned: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

data class RoutineDay(
    val id: Long,
    val uuid: String,
    val routineId: Long,
    val name: String,
    val position: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

data class RoutineExercise(
    val id: Long,
    val uuid: String,
    val routineDayId: Long,
    val exerciseId: Long,
    val position: Int,
    val notes: String,
    val groupKey: String?,
    val copyPreviousWorkout: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val machineId: Long? = null,
    val equipmentBindingState: RoutineEquipmentBindingState = if (machineId == null) {
        RoutineEquipmentBindingState.None
    } else {
        RoutineEquipmentBindingState.Resolved
    },
    val machineProfileUuidSnapshot: String? = null,
    val machineNameSnapshot: String = "",
    val machineLoadTypeSnapshot: MachineLoadType? = null,
    val machineUnitIdSnapshot: String = "",
    val machineLevelLabelSnapshot: String = "",
    val machineLoadInterpretationSnapshot: LoadInterpretation = LoadInterpretation.Total,
    val machineConfigurationGroupSnapshot: String = "",
    val machineConfigurationVersionSnapshot: Int = 1,
    val machineConfigurationSnapshot: String = "",
    val trainingMaxPercent: Double = 90.0,
    val progressionPercentages: List<Double> = emptyList(),
    val alternativeExerciseIds: List<Long> = emptyList(),
)

data class RoutineSet(
    val id: Long,
    val uuid: String,
    val routineExerciseId: Long,
    val position: Int,
    val draft: WorkoutSetDraft,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

enum class PersonalRecordType {
    MaxWeight,
    MaxRepetitions,
    MaxRepetitionsForWeight,
    BestWeightForRepCount,
    EstimatedOneRepMax,
    SetVolume,
    ExerciseWorkoutVolume,
    MaxDistance,
    MaxDuration,
    MaxSpeed,
    MinPace,
    MaxMachineSetting,
}

data class PersonalRecord(
    val uuid: String,
    val exerciseId: Long,
    val type: PersonalRecordType,
    val value: Double,
    val secondaryValue: Double?,
    val unitId: String,
    val sourceSetId: Long?,
    val sourceSessionId: Long?,
    val achievedAtMillis: Long,
    val current: Boolean,
    val imported: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val machineId: Long? = null,
    val machineProfileUuidSnapshot: String? = null,
)

data class GraphPreset(
    val id: Long,
    val uuid: String,
    val name: String,
    val exerciseIds: List<Long>,
    val metric: String,
    val dateRange: String,
    val aggregation: String,
    val archived: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

fun estimatedOneRepMax(
    weightKg: Double,
    repetitions: Int,
    formula: EstimatedOneRepMaxFormula,
    repCutoff: Int = 10,
): Double? {
    if (weightKg < 0.0 || repetitions !in 1..repCutoff) return null
    return when (formula) {
        EstimatedOneRepMaxFormula.Epley -> weightKg * (1.0 + repetitions / 30.0)
        EstimatedOneRepMaxFormula.Brzycki -> {
            if (repetitions >= 37) null else weightKg * 36.0 / (37.0 - repetitions)
        }
    }
}

fun loadInterpretationMultiplier(
    interpretation: LoadInterpretation,
    stackMode: MachineStackMode = MachineStackMode.Single,
    pulleyRatio: Double = 1.0,
    unilateral: Boolean = false,
): Double {
    val sideMultiplier = when (interpretation) {
        LoadInterpretation.PerHand, LoadInterpretation.PerSide -> if (unilateral) 1.0 else 2.0
        LoadInterpretation.Total,
        LoadInterpretation.AddedLoad,
        LoadInterpretation.BodyweightPlusExternal,
        LoadInterpretation.BodyweightPercentage,
        LoadInterpretation.AssistedSubtraction,
        LoadInterpretation.MachineDisplayedMass,
        LoadInterpretation.OrdinalSetting,
        -> when (stackMode) {
            MachineStackMode.Single -> 1.0
            MachineStackMode.DualCombined -> 2.0
            MachineStackMode.DualIndependent -> if (unilateral) 1.0 else 2.0
        }
    }
    return sideMultiplier * pulleyRatio.takeIf { it.isFinite() && it > 0.0 }.orEmptyOne()
}

private fun Double?.orEmptyOne(): Double = this ?: 1.0

fun canonicalResistanceKg(
    enteredValue: Double?,
    enteredUnitId: String?,
    machineSetting: Double? = null,
    interpretation: LoadInterpretation = LoadInterpretation.Total,
    baseLoadKg: Double? = null,
    addOnPlateKg: Double? = null,
    massMappingKg: Map<Double, Double> = emptyMap(),
    stackMode: MachineStackMode = MachineStackMode.Single,
    pulleyRatio: Double = 1.0,
    unilateral: Boolean = false,
): Double? {
    if (interpretation == LoadInterpretation.OrdinalSetting &&
        (machineSetting == null || !massMappingKg.containsKey(machineSetting))) return null
    val rawKg = machineSetting?.let(massMappingKg::get)
        ?: enteredValue?.let { value ->
            val unit = enteredUnitId?.let(BuiltInUnits::get) ?: return@let null
            if (unit.dimension != UnitDimension.Mass) null else unit.toCanonical(value)
        }
        ?: return null
    val multiplier = loadInterpretationMultiplier(interpretation, stackMode, pulleyRatio, unilateral)
    return rawKg * multiplier + (baseLoadKg ?: 0.0) + (addOnPlateKg ?: 0.0)
}

fun WorkoutSet.effectiveLoadKg(exercise: Exercise): Double? {
    val enteredLoad = canonicalWeightKg ?: 0.0
    when (exercise.loadInterpretation) {
        LoadInterpretation.OrdinalSetting -> return canonicalWeightKg
        LoadInterpretation.BodyweightPlusExternal -> return bodyweightKg?.plus(enteredLoad)
        LoadInterpretation.BodyweightPercentage -> return bodyweightKg
            ?.times(exercise.effectiveBodyweightPercent / 100.0)
            ?.plus(enteredLoad)
        LoadInterpretation.AssistedSubtraction -> return bodyweightKg?.let { bodyweight ->
            max(0.0, bodyweight * exercise.effectiveBodyweightPercent / 100.0 - enteredLoad)
        }
        else -> Unit
    }
    return when (exercise.trackingType) {
        ExerciseTrackingType.WeightReps,
        ExerciseTrackingType.WeightOnly,
        ExerciseTrackingType.WeightDuration,
        -> canonicalWeightKg
        ExerciseTrackingType.BodyweightReps -> when (exercise.bodyweightLoadPolicy) {
            BodyweightLoadPolicy.ExternalWeightOnly -> canonicalWeightKg
            BodyweightLoadPolicy.BodyweightPlusExternal -> {
                bodyweightKg?.plus(enteredLoad)
            }
            BodyweightLoadPolicy.EffectiveBodyweightPercentage -> {
                bodyweightKg?.times(exercise.effectiveBodyweightPercent / 100.0)?.plus(enteredLoad)
            }
        }
        ExerciseTrackingType.AssistedBodyweightReps -> bodyweightKg?.let { bodyweight ->
            max(0.0, bodyweight * exercise.effectiveBodyweightPercent / 100.0 - enteredLoad)
        }
        else -> null
    }
}

fun WorkoutSet.volumeKg(exercise: Exercise, includeWarmups: Boolean = false): Double {
    if (!completed || deletedAtMillis != null || !exercise.includeInVolume) return 0.0
    if (!includeWarmups && classification == WorkoutSetClassification.WarmUp) return 0.0
    return (effectiveLoadKg(exercise) ?: return 0.0) * (repetitions ?: 1)
}

fun WorkoutSet.estimatedOneRepMaxKg(
    exercise: Exercise,
    repCutoff: Int = 10,
    includeWarmups: Boolean = false,
    adjustForEffort: Boolean = false,
): Double? {
    if (!completed || deletedAtMillis != null || !exercise.includeInPersonalRecords) return null
    if (!includeWarmups && classification == WorkoutSetClassification.WarmUp) return null
    val repetitions = repetitions ?: return null
    val effortRepetitions = if (adjustForEffort) {
        repetitions + (rir ?: rpe?.let { (10.0 - it).coerceAtLeast(0.0) } ?: 0.0)
    } else repetitions.toDouble()
    if (effortRepetitions !in 1.0..repCutoff.toDouble()) return null
    val load = effectiveLoadKg(exercise) ?: return null
    return when (exercise.oneRepMaxFormula) {
        EstimatedOneRepMaxFormula.Epley -> load * (1.0 + effortRepetitions / 30.0)
        EstimatedOneRepMaxFormula.Brzycki -> if (effortRepetitions >= 37.0) null else load * 36.0 / (37.0 - effortRepetitions)
    }
}

fun WorkoutSet.speedMetresPerSecond(): Double? {
    val seconds = durationSeconds ?: return null
    if (seconds <= 0) return null
    return canonicalDistanceMetres?.div(seconds)
}

fun WorkoutSet.paceSecondsPerKilometre(): Double? {
    val metres = canonicalDistanceMetres ?: return null
    if (metres <= 0) return null
    return durationSeconds?.times(1_000.0)?.div(metres)
}

fun calculateWorkoutSummary(
    session: WorkoutSession,
    workoutExercises: List<WorkoutExercise>,
    sets: List<WorkoutSet>,
    exercisesById: Map<Long, Exercise>,
    nowMillis: Long,
    includeWarmups: Boolean = false,
): WorkoutSummary {
    val workoutExerciseById = workoutExercises.associateBy(WorkoutExercise::id)
    val activeSets = sets.filter { it.deletedAtMillis == null && it.completed }
    val volume = activeSets.sumOf { set ->
        val workoutExercise = workoutExerciseById[set.workoutExerciseId]
        val exercise = workoutExercise?.let { placement -> exercisesById[placement.exerciseId]?.let(placement::applyPolicySnapshot) }
        if (exercise == null) 0.0 else set.volumeKg(exercise, includeWarmups)
    }
    val bestOneRepMax = activeSets.mapNotNull { set ->
        val workoutExercise = workoutExerciseById[set.workoutExerciseId]
        val exercise = workoutExercise?.let { placement -> exercisesById[placement.exerciseId]?.let(placement::applyPolicySnapshot) }
        exercise?.let { set.estimatedOneRepMaxKg(it, includeWarmups = includeWarmups) }
    }.maxOrNull()
    val endedOrNow = session.endedAt?.toEpochMilli() ?: nowMillis
    return WorkoutSummary(
        exerciseCount = workoutExercises.map(WorkoutExercise::exerciseId).distinct().size,
        completedSetCount = activeSets.size,
        repetitions = activeSets.sumOf { it.repetitions ?: 0 },
        volumeKg = volume,
        distanceMetres = activeSets.sumOf { it.canonicalDistanceMetres ?: 0.0 },
        durationSeconds = activeSets.sumOf { it.durationSeconds ?: 0L },
        elapsedSeconds = max(0L, (endedOrNow - session.startedAt.toEpochMilli()) / 1_000L),
        highestEstimatedOneRepMaxKg = bestOneRepMax,
    )
}
