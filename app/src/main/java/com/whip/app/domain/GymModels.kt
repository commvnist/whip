package com.whip.app.domain

import java.time.Instant
import java.time.LocalDate
import java.security.MessageDigest
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

enum class EstimatedOneRepMaxFormula(val label: String) {
    Epley("Epley"),
    Brzycki("Brzycki"),
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

enum class MachineLevelDirection(val label: String) {
    HigherNumberMoreResistance("Higher number = more resistance"),
    HigherNumberLessResistance("Higher number = less resistance"),
}

data class GymMachineDraft(
    val exerciseId: Long? = null,
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
    val exerciseIds: Set<Long> = exerciseId?.let(::setOf).orEmpty(),
    val levelDirection: MachineLevelDirection = MachineLevelDirection.HigherNumberMoreResistance,
)

data class GymMachine(
    val id: Long,
    val uuid: String,
    val exerciseId: Long?,
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
    val exerciseIds: Set<Long> = exerciseId?.let(::setOf).orEmpty(),
    val levelDirection: MachineLevelDirection = MachineLevelDirection.HigherNumberMoreResistance,
) {
    val displayName: String
        get() = if (location.isBlank()) name else "$name · $location"

    fun supportsExercise(exerciseId: Long): Boolean = exerciseId in exerciseIds
}

enum class WorkoutSessionState(val label: String) {
    Active("In progress"),
    Finished("Finished"),
    Discarded("Discarded"),
}

/** What happened to an exercise placement after it was snapshotted into a workout. */
enum class WorkoutExerciseOutcome {
    Active,
    Removed,
    Substituted,
}

/** Why a snapshotted set stopped being executable while its historical fact was retained. */
enum class WorkoutSetRemovalReason {
    Removed,
    Skipped,
    ExerciseRemoved,
    ExerciseSubstituted,
}

enum class WorkoutSetClassification {
    WarmUp,
    Working,
    BackOff,
    Drop,
    Amrap,
    /** Explicit 100%-of-TM, 3–5-rep test set inside a Training Max Test phase. */
    TrainingMaxTest,
    Failure,
}

enum class WorkoutGroupType(val label: String) {
    Superset("Superset"),
    Circuit("Circuit"),
}

enum class RoutineLoadPrescriptionType(val label: String) {
    Absolute("Exact load"),
    PercentOneRepMax("% of estimated 1RM"),
    PercentTrainingMax("% of training max"),
}

/**
 * Stable routine-program identities. [Static] preserves the pre-program behavior; the
 * remaining values opt a routine into persisted phase/day/cycle progression.
 */
enum class RoutineProgramKind {
    Static,
    Custom,
    FiveThreeOne,
}

enum class RoutineTrainingMaxSource {
    EstimatedOneRepMaxPercent,
    Explicit,
}

/** The user-visible value from which an explicit, stable Training Max was derived. */
enum class TrainingMaxBasisKind {
    Unspecified,
    ActualOneRepMax,
    EstimatedOneRepMax,
    ManualSourceMax,
    ExplicitTrainingMax,
}

enum class RoutineProgressionMode {
    Standard,
    PerformanceInformed,
}

enum class TrainingMaxDecisionAction {
    UseSuggestion,
    UseStandard,
    Hold,
    /** Decline Whip's advisory recommendation and intentionally leave this cycle's TM unchanged. */
    IgnoreRecommendation,
    Custom,
}

/** Explicit user choice submitted while finishing a configured program boundary. */
data class TrainingMaxCycleDecision(
    val exerciseId: Long,
    /** Optimistic-lock snapshot from the review; repository rejects a stale Training Max. */
    val expectedCurrentTrainingMax: Double? = null,
    val requestedDelta: Double,
    val standardDelta: Double,
    val recommendationCategory: String,
    val recommendationDelta: Double,
    val confidence: Double,
    val reasons: List<String>,
    val engineVersion: String,
    val action: TrainingMaxDecisionAction,
)

/** Immutable, user-auditable record of a Training Max boundary decision. */
data class TrainingMaxDecision(
    val uuid: String,
    val routineUuid: String,
    val sessionUuid: String,
    val exerciseUuid: String,
    val exerciseName: String,
    val cycle: Int,
    val previousTrainingMax: Double,
    val appliedDelta: Double,
    val resultingTrainingMax: Double,
    val unitId: String,
    val standardDelta: Double,
    val recommendationCategory: String,
    val recommendationDelta: Double,
    val confidence: Double,
    val reasons: List<String>,
    val engineVersion: String,
    val action: TrainingMaxDecisionAction,
    val createdAtMillis: Long,
)

enum class RoutineMainWorkScheme {
    Unspecified,
    ClassicPrSet,
    ClassicMinimumReps,
    FivesPro,
}

enum class RoutineSupplementalScheme {
    None,
    FirstSetLast,
    SecondSetLast,
    BoringButBig,
    BoringButStrong,
    Custom,
}

/** The structural job of an exercise placement inside one routine day. */
enum class RoutinePlacementKind {
    General,
    MainLift,
    /** Programmed Supplemental work performed with a lift other than the day's Main lift. */
    Supplemental,
    Assistance,
}

/**
 * Assigns one execution day to each logical lift while covering as many scheduled days as
 * possible. Unique lifts are fixed first; repeated lifts then fill uncovered days in schedule
 * order. The result is deterministic and keeps once-per-lift protocol weeks from producing an
 * avoidable empty day.
 */
fun balancedOncePerLiftDayOwners(exerciseIdsByDay: List<List<Long>>): Map<Long, Int> {
    val occurrences = linkedMapOf<Long, MutableList<Int>>()
    exerciseIdsByDay.forEachIndexed { dayIndex, exerciseIds ->
        exerciseIds.distinct().forEach { exerciseId ->
            occurrences.getOrPut(exerciseId) { mutableListOf() } += dayIndex
        }
    }
    val owners = linkedMapOf<Long, Int>()
    val occupiedDays = mutableSetOf<Int>()
    occurrences.forEach { (exerciseId, days) ->
        if (days.size == 1) {
            owners[exerciseId] = days.single()
            occupiedDays += days.single()
        }
    }
    exerciseIdsByDay.indices.forEach { dayIndex ->
        if (dayIndex !in occupiedDays) {
            exerciseIdsByDay[dayIndex].firstOrNull { it !in owners }?.let { exerciseId ->
                owners[exerciseId] = dayIndex
                occupiedDays += dayIndex
            }
        }
    }
    occurrences.forEach { (exerciseId, days) ->
        owners.putIfAbsent(exerciseId, days.first())
    }
    return owners
}

/**
 * A routine-local assistance classification. This deliberately does not live on [Exercise]:
 * the same movement can be Pull work in one program and unclassified general work in another.
 */
enum class RoutineAssistanceCategory {
    Unspecified,
    Push,
    Pull,
    SingleLegCore,
    Other,
}

/** Stable provenance for a generated program; notes remain editable user content. */
enum class RoutineProgramTemplateKey {
    None,
    FiveThreeOneFourDay,
    FiveThreeOneBeginners,
    FiveThreeOneCustom,
    FiveThreeOneForeverBbbLeaderAnchor,
    FiveThreeOneForeverFslLeaderAnchor,
}

/** Template semantics from this revision execute 7th Week protocols once per logical lift. */
const val FIVE_THREE_ONE_ONCE_PER_LIFT_PROTOCOL_REVISION = 2

/** Builder choice used while assigning a structural placement or assistance category. */
enum class RoutineAssistanceRole {
    Unspecified,
    MainLift,
    Push,
    Pull,
    SingleLegCore,
    Other,
}

enum class RoutineWorkSection {
    Unspecified,
    Main,
    Supplemental,
    Assistance,
    Optional,
}

enum class RoutineOptionalWorkKind {
    None,
    Joker,
}

enum class RoutineProgramPhaseRole {
    Standard,
    Leader,
    Anchor,
    Deload,
    TrainingMaxTest,
    PersonalRecordTest,
    /** Revision-2 7th Week phases whose repeated lift placements execute once per logical lift. */
    OncePerLiftDeload,
    OncePerLiftTrainingMaxTest,
    OncePerLiftPersonalRecordTest,
    ;

    fun semanticRole(): RoutineProgramPhaseRole = when (this) {
        OncePerLiftDeload -> Deload
        OncePerLiftTrainingMaxTest -> TrainingMaxTest
        OncePerLiftPersonalRecordTest -> PersonalRecordTest
        else -> this
    }

    fun usesOncePerLiftProtocol(): Boolean = when (this) {
        OncePerLiftDeload,
        OncePerLiftTrainingMaxTest,
        OncePerLiftPersonalRecordTest,
        -> true
        else -> false
    }

    fun asOncePerLiftProtocol(): RoutineProgramPhaseRole = when (semanticRole()) {
        Deload -> OncePerLiftDeload
        TrainingMaxTest -> OncePerLiftTrainingMaxTest
        PersonalRecordTest -> OncePerLiftPersonalRecordTest
        else -> this
    }
}

data class RoutineProgramDraft(
    val kind: RoutineProgramKind,
    val phaseCount: Int,
    val phaseLabels: List<String> = emptyList(),
    val phaseRoles: List<RoutineProgramPhaseRole> = emptyList(),
    val trainingMaxAdvanceAfterPhaseIndices: Set<Int> = emptySet(),
    /** Edit-only hint used to preserve the same semantic phase through reorder/rename operations. */
    val currentPhaseIndexHint: Int? = null,
    val templateKey: RoutineProgramTemplateKey = RoutineProgramTemplateKey.None,
    val templateRevision: Int = 0,
    val progressionMode: RoutineProgressionMode = RoutineProgressionMode.Standard,
    val allowNonStandardHigherSuggestions: Boolean = false,
)

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
    val sourceRoutineDayId: Long? = null,
    val sourceRoutineProgramKind: RoutineProgramKind = RoutineProgramKind.Static,
    val sourceRoutinePhaseIndex: Int? = null,
    val sourceRoutineCycle: Int? = null,
    val sourceRoutineDayPosition: Int? = null,
    val sourceRoutineDayProgressionIndex: Int? = null,
    val programProgressAdvanced: Boolean = false,
    val requiredMainWorkInvalidated: Boolean = false,
    val invalidatedMainExerciseIds: Set<Long> = emptySet(),
    val sourceRoutinePhaseLabel: String = "",
    val sourceRoutinePhaseRole: RoutineProgramPhaseRole = RoutineProgramPhaseRole.Standard,
    /** Monotonic revision of performed/prescribed workout content; timer and metadata do not change it. */
    val workoutRevision: Long = 0,
    val restTimerRevision: Long = 0,
    val restTimerCleanupPending: Boolean = false,
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
    val machineLevelDirectionSnapshot: MachineLevelDirection = MachineLevelDirection.HigherNumberMoreResistance,
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
    val trainingMaxKgSnapshot: Double? = null,
    val trainingMaxValueSnapshot: Double? = null,
    val trainingMaxUnitIdSnapshot: String = "",
    val cycleIncrementValueSnapshot: Double? = null,
    val trainingMaxSourceSnapshot: RoutineTrainingMaxSource = RoutineTrainingMaxSource.EstimatedOneRepMaxPercent,
    val mainWorkSchemeSnapshot: RoutineMainWorkScheme = RoutineMainWorkScheme.Unspecified,
    val supplementalSchemeSnapshot: RoutineSupplementalScheme = RoutineSupplementalScheme.None,
    val placementKindSnapshot: RoutinePlacementKind = RoutinePlacementKind.General,
    val assistanceCategorySnapshot: RoutineAssistanceCategory = RoutineAssistanceCategory.Unspecified,
    val jokerSetsEnabledSnapshot: Boolean = false,
    val outcome: WorkoutExerciseOutcome = WorkoutExerciseOutcome.Active,
    val outcomeAtMillis: Long? = null,
    val replacementWorkoutExerciseUuid: String? = null,
)

/** Stable equipment partition captured when the workout placement is created. */
val WorkoutExercise.equipmentScopeKey: String?
    get() = machineProfileUuidSnapshot

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
    /** Null applies in every program phase; otherwise this set is active only in that zero-based phase. */
    val routinePhaseIndex: Int? = null,
    val workSection: RoutineWorkSection = RoutineWorkSection.Unspecified,
    val optionalWorkKind: RoutineOptionalWorkKind = RoutineOptionalWorkKind.None,
    /** Set-authoritative program policy. Only Main sets may carry this value. */
    val mainWorkScheme: RoutineMainWorkScheme? = null,
    /** Set-authoritative program policy. Only Supplemental sets may carry this value. */
    val supplementalScheme: RoutineSupplementalScheme? = null,
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
    val workSectionSnapshot: RoutineWorkSection = RoutineWorkSection.Unspecified,
    val optionalWorkKindSnapshot: RoutineOptionalWorkKind = RoutineOptionalWorkKind.None,
    /** Immutable authored set type, even when [classification] is changed to Failure. */
    val prescribedClassificationSnapshot: WorkoutSetClassification = classification,
    /** True only when routine instantiation made this set part of progression eligibility. */
    val requiredForProgressionSnapshot: Boolean = false,
    /** Null while executable; otherwise explains why the retained snapshot left the workout lane. */
    val removalReason: WorkoutSetRemovalReason? = null,
)

/**
 * Identity of the exact active-workout structure a user reviewed before editing it.
 *
 * This is deliberately narrower than [WorkoutSession.workoutRevision]: entering load/reps or
 * completing a set does not invalidate an open layout editor, while adding, removing, grouping,
 * or reordering work does. The repository recomputes the fingerprint in the same transaction as
 * the requested mutation, so stale UI can never silently overwrite a newer layout.
 */
data class WorkoutStructureBoundary(
    val sessionId: Long,
    val sessionUuid: String,
    val fingerprint: String,
)

data class WorkoutFinishBoundary(
    val sessionId: Long,
    val sessionUuid: String,
    val workoutRevision: Long,
)

data class WorkoutSetOrderDraft(
    val workoutExerciseUuid: String,
    /** Includes retained tombstones. Invisible historical slots therefore cannot be collapsed. */
    val setUuidsInOrder: List<String>,
)

data class WorkoutArrangementDraft(
    /** Active placements only. Retired historical placements keep their authored slots. */
    val activeWorkoutExerciseUuidsInOrder: List<String>,
    val setOrders: List<WorkoutSetOrderDraft>,
)

data class WorkoutPlacementMutationBoundary(
    val structure: WorkoutStructureBoundary,
    val workoutExerciseId: Long,
    val workoutExerciseUuid: String,
    val workoutExerciseUpdatedAtMillis: Long,
    val expectedGroupUuid: String?,
)

data class WorkoutSetCopyBoundary(
    val setId: Long,
    val setUuid: String,
    val setUpdatedAtMillis: Long,
)

data class WorkoutExerciseCopyBoundary(
    val sourceSessionId: Long,
    val sourceSessionUuid: String,
    val sourceWorkoutExerciseId: Long,
    val sourceWorkoutExerciseUuid: String,
    val sourceWorkoutExerciseUpdatedAtMillis: Long,
    val sourceSets: List<WorkoutSetCopyBoundary>,
    val target: WorkoutStructureBoundary?,
)

data class WorkoutSetMutationBoundary(
    val sessionId: Long,
    val sessionUuid: String,
    val workoutRevision: Long,
    val workoutExerciseId: Long,
    val workoutExerciseUuid: String,
    val setId: Long,
    val setUuid: String,
    val setUpdatedAtMillis: Long,
    val expectedDeletedAtMillis: Long?,
    val expectedRemovalReason: WorkoutSetRemovalReason?,
)

data class WorkoutGroupLayoutSnapshot(
    val uuid: String,
    val name: String,
    val type: WorkoutGroupType,
    val position: Int,
)

/** Exact, same-session layout used for the single-level Arrange/Grouping Undo action. */
data class WorkoutLayoutSnapshot(
    val allWorkoutExerciseUuidsInOrder: List<String>,
    val groups: List<WorkoutGroupLayoutSnapshot>,
    val groupUuidByWorkoutExerciseUuid: Map<String, String?>,
    val setOrders: List<WorkoutSetOrderDraft>,
)

data class WorkoutStructureMutationReceipt(
    val sessionId: Long,
    val sessionUuid: String,
    val changed: Boolean,
    val beforeFingerprint: String,
    val afterBoundary: WorkoutStructureBoundary,
    val previousLayout: WorkoutLayoutSnapshot? = null,
    val targetUuid: String? = null,
)

/**
 * Canonical structural identity shared by UI review state and transactional persistence checks.
 * UUIDs, rather than local row ids, make the identity stable across import/restore boundaries.
 */
fun workoutStructureBoundary(
    session: WorkoutSession,
    workoutExercises: List<WorkoutExercise>,
    groups: List<WorkoutGroup>,
    sets: List<WorkoutSet>,
): WorkoutStructureBoundary {
    val placements = workoutExercises.filter { it.sessionId == session.id }
    val placementIds = placements.mapTo(mutableSetOf(), WorkoutExercise::id)
    val placementUuidById = placements.associate { it.id to it.uuid }
    val groupUuidById = groups.filter { it.sessionId == session.id }.associate { it.id to it.uuid }
    val canonical = buildString {
        appendCanonicalPart(session.uuid)
        groups.asSequence().filter { it.sessionId == session.id }.sortedBy(WorkoutGroup::uuid).forEach { group ->
            appendCanonicalPart("group")
            appendCanonicalPart(group.uuid)
            appendCanonicalPart(group.name)
            appendCanonicalPart(group.type.name)
            appendCanonicalPart(group.position.toString())
        }
        placements.sortedBy(WorkoutExercise::uuid).forEach { placement ->
            appendCanonicalPart("placement")
            appendCanonicalPart(placement.uuid)
            appendCanonicalPart(placement.position.toString())
            appendCanonicalPart(placement.outcome.name)
            appendCanonicalPart(placement.groupId?.let(groupUuidById::get).orEmpty())
            appendCanonicalPart(placement.replacementWorkoutExerciseUuid.orEmpty())
        }
        sets.asSequence().filter { it.workoutExerciseId in placementIds }.sortedBy(WorkoutSet::uuid).forEach { set ->
            appendCanonicalPart("set")
            appendCanonicalPart(set.uuid)
            appendCanonicalPart(requireNotNull(placementUuidById[set.workoutExerciseId]))
            appendCanonicalPart(set.position.toString())
            appendCanonicalPart((set.deletedAtMillis != null).toString())
            appendCanonicalPart(set.removalReason?.name.orEmpty())
        }
    }
    val fingerprint = MessageDigest.getInstance("SHA-256")
        .digest(canonical.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
    return WorkoutStructureBoundary(session.id, session.uuid, fingerprint)
}

fun workoutLayoutSnapshot(
    session: WorkoutSession,
    workoutExercises: List<WorkoutExercise>,
    groups: List<WorkoutGroup>,
    sets: List<WorkoutSet>,
): WorkoutLayoutSnapshot {
    val placements = workoutExercises.filter { it.sessionId == session.id }
        .sortedWith(compareBy(WorkoutExercise::position, WorkoutExercise::uuid))
    val placementIds = placements.mapTo(mutableSetOf(), WorkoutExercise::id)
    val sessionGroups = groups.filter { it.sessionId == session.id }
    val groupUuidById = sessionGroups.associate { it.id to it.uuid }
    return WorkoutLayoutSnapshot(
        allWorkoutExerciseUuidsInOrder = placements.map(WorkoutExercise::uuid),
        groups = sessionGroups.sortedWith(compareBy(WorkoutGroup::position, WorkoutGroup::uuid)).map { group ->
            WorkoutGroupLayoutSnapshot(group.uuid, group.name, group.type, group.position)
        },
        groupUuidByWorkoutExerciseUuid = placements.associate { placement ->
            placement.uuid to placement.groupId?.let(groupUuidById::get)
        },
        setOrders = placements.map { placement ->
            WorkoutSetOrderDraft(
                placement.uuid,
                sets.asSequence().filter { it.workoutExerciseId == placement.id }
                    .sortedWith(compareBy(WorkoutSet::position, WorkoutSet::uuid))
                    .map(WorkoutSet::uuid).toList(),
            )
        },
    )
}

private fun StringBuilder.appendCanonicalPart(value: String) {
    append(value.length).append(':').append(value).append('|')
}

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
    /** Null selects a static routine on create and preserves the stored program on update. */
    val program: RoutineProgramDraft? = null,
    /** Edit-only hint used to preserve the same next day when the routine days are reordered. */
    val nextProgramDayPositionHint: Int? = null,
)

data class RoutineDayDraft(
    val name: String,
    val exercises: List<RoutineExerciseDraft>,
    /** Null lets repository updates preserve the stored per-day progression cursor. */
    val progressionIndex: Int? = null,
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
    /** Explicit, auditable training max. Null means no Training Max has been applied. */
    val trainingMaxValue: Double? = null,
    val trainingMaxUnitId: String = "kilogram",
    val cycleIncrementValue: Double? = null,
    val trainingMaxSource: RoutineTrainingMaxSource = if (trainingMaxValue == null) {
        RoutineTrainingMaxSource.EstimatedOneRepMaxPercent
    } else {
        RoutineTrainingMaxSource.Explicit
    },
    val trainingMaxBasisKind: TrainingMaxBasisKind = TrainingMaxBasisKind.Unspecified,
    val trainingMaxBasisValue: Double? = null,
    val trainingMaxBasisUnitId: String = "",
    /** Eligibility is per exercise identity; repeated program placements are synchronized. */
    val trainingMaxIncreaseEligible: Boolean = true,
    val mainWorkScheme: RoutineMainWorkScheme = RoutineMainWorkScheme.Unspecified,
    val supplementalScheme: RoutineSupplementalScheme = RoutineSupplementalScheme.None,
    val placementKind: RoutinePlacementKind = RoutinePlacementKind.General,
    val assistanceCategory: RoutineAssistanceCategory = RoutineAssistanceCategory.Unspecified,
    val jokerSetsEnabled: Boolean = false,
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
    val programKind: RoutineProgramKind = RoutineProgramKind.Static,
    val programPhaseCount: Int = 1,
    val programPhaseLabels: List<String> = emptyList(),
    val currentProgramPhaseIndex: Int = 0,
    val currentProgramCycle: Int = 1,
    val nextProgramDayPosition: Int = 0,
    val trainingMaxIncreaseEligible: Boolean = true,
    val programPhaseRoles: List<RoutineProgramPhaseRole> = emptyList(),
    val trainingMaxAdvanceAfterPhaseIndices: Set<Int> = emptySet(),
    val programTemplateKey: RoutineProgramTemplateKey = RoutineProgramTemplateKey.None,
    val programTemplateRevision: Int = 0,
    val progressionMode: RoutineProgressionMode = RoutineProgressionMode.Standard,
    val allowNonStandardHigherSuggestions: Boolean = false,
)

data class RoutineDay(
    val id: Long,
    val uuid: String,
    val routineId: Long,
    val name: String,
    val position: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val progressionIndex: Int = 0,
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
    val trainingMaxKg: Double? = null,
    val trainingMaxValue: Double? = null,
    val trainingMaxUnitId: String = "kilogram",
    val cycleIncrementValue: Double? = null,
    val trainingMaxSource: RoutineTrainingMaxSource = RoutineTrainingMaxSource.EstimatedOneRepMaxPercent,
    val trainingMaxBasisKind: TrainingMaxBasisKind = TrainingMaxBasisKind.Unspecified,
    val trainingMaxBasisValue: Double? = null,
    val trainingMaxBasisUnitId: String = "",
    val trainingMaxIncreaseEligible: Boolean = true,
    val mainWorkScheme: RoutineMainWorkScheme = RoutineMainWorkScheme.Unspecified,
    val supplementalScheme: RoutineSupplementalScheme = RoutineSupplementalScheme.None,
    val placementKind: RoutinePlacementKind = RoutinePlacementKind.General,
    val assistanceCategory: RoutineAssistanceCategory = RoutineAssistanceCategory.Unspecified,
    val jokerSetsEnabled: Boolean = false,
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
    if (!weightKg.isFinite() || weightKg < 0.0 || repetitions !in 1..repCutoff) return null
    // A performed single is an observed one-repetition maximum, not an estimate that should be
    // inflated by a multi-repetition formula.
    if (repetitions == 1) return weightKg
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

/** Percentage targets cannot yet invert load modes whose effective resistance depends on bodyweight. */
fun LoadInterpretation.supportsRoutinePercentagePrescription(): Boolean = this !in setOf(
    LoadInterpretation.BodyweightPlusExternal,
    LoadInterpretation.BodyweightPercentage,
    LoadInterpretation.AssistedSubtraction,
    LoadInterpretation.OrdinalSetting,
)

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
    if (classification == WorkoutSetClassification.Failure ||
        (!includeWarmups && classification == WorkoutSetClassification.WarmUp)
    ) return null
    val repetitions = repetitions ?: return null
    val effortRepetitions = if (adjustForEffort) {
        repetitions + (rir ?: rpe?.let { (10.0 - it).coerceAtLeast(0.0) } ?: 0.0)
    } else repetitions.toDouble()
    if (effortRepetitions !in 1.0..repCutoff.toDouble()) return null
    val load = effectiveLoadKg(exercise) ?: return null
    if (!load.isFinite() || load < 0.0) return null
    if (effortRepetitions == 1.0) return load
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
