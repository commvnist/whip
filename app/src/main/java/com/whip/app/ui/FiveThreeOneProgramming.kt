package com.whip.app.ui

import com.whip.app.domain.RoutineLoadPrescriptionType
import com.whip.app.domain.Exercise
import com.whip.app.domain.ExerciseTrackingType
import com.whip.app.domain.RoutineMainWorkScheme
import com.whip.app.domain.RoutineOptionalWorkKind
import com.whip.app.domain.RoutineProgramKind
import com.whip.app.domain.RoutineAssistanceRole
import com.whip.app.domain.RoutineAssistanceCategory
import com.whip.app.domain.RoutinePlacementKind
import com.whip.app.domain.RoutineProgramTemplateKey
import com.whip.app.domain.RoutineProgressionMode
import com.whip.app.domain.RoutineProgramPhaseRole
import com.whip.app.domain.RoutineSupplementalScheme
import com.whip.app.domain.RoutineWorkSection
import com.whip.app.domain.TrainingMaxBasisKind
import com.whip.app.domain.WorkoutSetClassification
import com.whip.app.domain.balancedOncePerExerciseDayOwners
import com.whip.app.domain.FIVE_THREE_ONE_ONCE_PER_EXERCISE_PROTOCOL_REVISION
import kotlin.math.abs
import kotlin.math.round

internal enum class FiveThreeOneMainScheme(val label: String) {
    Classic("Classic 5/3/1"),
    FivesPro("5s PRO"),
}

internal enum class FiveThreeOnePhase(val label: String) {
    Fives("5s Week"),
    Threes("3s Week"),
    FiveThreeOne("5/3/1 Week"),
    Deload("Deload"),
}

internal enum class FiveThreeOneSupplement(val label: String) {
    None("No Supplemental Work"),
    BoringButBig("BBB · 5 × 10"),
    FirstSetLast("FSL · 5 × 5"),
    SecondSetLast("SSL · 5 × 5"),
    BoringButStrong("Boring But Strong · 10 × 5"),
}

internal enum class FiveThreeOneProgramLayout(val label: String) {
    FourDay("4-Day 5/3/1"),
    Beginners("5/3/1 for Beginners"),
    Custom("Choose Your Exercises"),
}

internal enum class FiveThreeOneProgramPlan(val label: String, val supportingText: String) {
    SingleCycle(
        "Classic cycle",
        "Three main-work weeks followed by one editable 7th Week protocol.",
    ),
    ForeverBbbLeaderAnchor(
        "BBB Leader → FSL Anchor",
        "Two 5s PRO BBB Leader cycles, a 7th Week transition, then a PR-set/FSL Anchor.",
    ),
    ForeverFslLeaderAnchor(
        "FSL Leader → FSL Anchor",
        "Two 5s PRO FSL Leader cycles, a 7th Week transition, then a PR-set/FSL Anchor.",
    ),
}

internal enum class FiveThreeOneSeventhWeekProtocol(val label: String, val supportingText: String) {
    Deload("Deload", "70% × 5, 80% × 3–5, 90% × 1, 100% × 1"),
    TrainingMaxTest("Training Max Test", "70/80/90% × 5, then 100% × 3–5"),
    PersonalRecordTest("PR Test", "70/80/90% × 5, then 100% × 1+"),
}

internal data class FiveThreeOneJokerLadder(
    val count: Int = 0,
    val stepPercent: Double = 5.0,
) {
    init {
        require(count in 0..3) { "Joker ladder must contain from zero to three candidates" }
        require(stepPercent == 5.0 || stepPercent == 10.0) { "Joker steps must be 5% or 10% of Training Max" }
    }
}

internal data class FiveThreeOneAssistanceChoice(
    val category: RoutineAssistanceCategory,
    val exerciseId: Long,
    val exerciseName: String,
)

internal fun RoutineAssistanceCategory.fiveThreeOneUiLabel(): String = when (this) {
    RoutineAssistanceCategory.Push -> "Push"
    RoutineAssistanceCategory.Pull -> "Pull"
    RoutineAssistanceCategory.SingleLegCore -> "Single-leg / Core"
    RoutineAssistanceCategory.Other -> "Other"
    RoutineAssistanceCategory.Unspecified -> "General"
}

internal data class FiveThreeOneProgramRequest(
    val layout: FiveThreeOneProgramLayout,
    val plan: FiveThreeOneProgramPlan,
    val exercises: List<FiveThreeOneProgramExercise>,
    val mainScheme: FiveThreeOneMainScheme,
    val supplement: FiveThreeOneSupplement,
    val closingProtocol: FiveThreeOneSeventhWeekProtocol,
    val jokerLadder: FiveThreeOneJokerLadder,
    val classicFinalSetAmrap: Boolean,
    val boringButBigPercent: Double,
    val progressionMode: RoutineProgressionMode,
    /** Main exercise ID -> BBB exercise ID. Equal IDs mean same-exercise BBB. */
    val bbbExerciseByMainExerciseId: Map<Long, Long> = emptyMap(),
    val assistance: List<FiveThreeOneAssistanceChoice> = emptyList(),
)

internal enum class FiveThreeOneExerciseRole(val label: String) {
    Squat("Squat"),
    Bench("Bench Press"),
    Deadlift("Deadlift"),
    Press("Overhead Press"),
}

private val assistanceRepTrackingTypes = setOf(
    ExerciseTrackingType.WeightReps,
    ExerciseTrackingType.BodyweightReps,
    ExerciseTrackingType.AssistedBodyweightReps,
    ExerciseTrackingType.RepsOnly,
    ExerciseTrackingType.RepsDuration,
)

internal fun Exercise.isFiveThreeOneAssistanceCompatible(excludedExerciseIds: Set<Long>): Boolean =
    !archived && id !in excludedExerciseIds && trackingType in assistanceRepTrackingTypes

private val assistanceSignals = mapOf(
    RoutineAssistanceCategory.Push to listOf(
        "push", "press", "bench", "dip", "tricep", "chest", "pec", "shoulder",
    ),
    RoutineAssistanceCategory.Pull to listOf(
        "pull", "row", "chin", "lat", "curl", "bicep", "upper back", "rear delt", "face pull",
    ),
    RoutineAssistanceCategory.SingleLegCore to listOf(
        "single leg", "split squat", "lunge", "step up", "core", "ab", "plank", "leg raise",
        "back extension", "reverse hyper", "good morning",
    ),
)

/**
 * Returns deterministic, explainable suggestions from the user's existing active library.
 * A low-scoring fallback is intentionally not invented: missing categories stay visibly missing.
 */
internal fun suggestFiveThreeOneAssistance(
    exercises: List<Exercise>,
    excludedExerciseIds: Set<Long>,
): Map<RoutineAssistanceCategory, List<Exercise>> = assistanceSignals.mapValues { (_, signals) ->
    exercises.asSequence()
        .filter { exercise -> exercise.isFiveThreeOneAssistanceCompatible(excludedExerciseIds) }
        // Never silently demote an unselected canonical main exercise into assistance. Lifters may
        // still choose it explicitly from the full compatible-library picker.
        .filterNot { exercise -> FiveThreeOneExerciseRole.entries.any { it.matchesExerciseName(exercise.name) } }
        .map { exercise ->
            val searchable = listOf(
                exercise.name,
                exercise.primaryMuscles,
                exercise.secondaryMuscles,
                exercise.equipment,
            ).joinToString(" ").lowercase()
            val signalScore = signals.count(searchable::contains)
            val score = signalScore * 100 + if (exercise.favorite) 10 else 0
            Triple(exercise, signalScore, score)
        }
        .filter { (_, signalScore) -> signalScore > 0 }
        .sortedWith(
            compareByDescending<Triple<Exercise, Int, Int>> { it.third }
                .thenBy { it.first.position }
                .thenBy { it.first.name.lowercase() }
                .thenBy { it.first.id },
        )
        .map(Triple<Exercise, Int, Int>::first)
        .toList()
}

internal fun RoutineProgramKind.isFiveThreeOneProgramKind(): Boolean = this == RoutineProgramKind.FiveThreeOne

internal fun String?.isFiveThreeOneProgramKindName(): Boolean =
    runCatching { RoutineProgramKind.valueOf(this.orEmpty()) }.getOrNull()?.isFiveThreeOneProgramKind() == true

internal fun FiveThreeOneExerciseRole.matchesExerciseName(name: String): Boolean {
    val normalized = name.lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
    return when (this) {
        FiveThreeOneExerciseRole.Squat -> normalized in setOf(
            "squat", "back squat", "barbell squat", "barbell back squat", "low bar squat", "high bar squat",
        )
        FiveThreeOneExerciseRole.Bench -> normalized in setOf(
            "bench", "bench press", "barbell bench", "barbell bench press", "flat bench press",
        )
        FiveThreeOneExerciseRole.Deadlift -> normalized in setOf(
            "deadlift", "dead lift", "barbell deadlift", "conventional deadlift", "sumo deadlift",
        )
        FiveThreeOneExerciseRole.Press -> normalized in setOf(
            "press", "overhead press", "barbell overhead press", "military press", "standing press", "shoulder press",
        )
    }
}

internal fun defaultFiveThreeOneCycleIncrease(
    unitId: String,
    exerciseName: String,
    role: FiveThreeOneExerciseRole? = null,
): Double {
    val lowerExercise = role in setOf(FiveThreeOneExerciseRole.Squat, FiveThreeOneExerciseRole.Deadlift) ||
        (role == null && listOf("squat", "deadlift", "dead lift").any(exerciseName.lowercase()::contains))
    return if (unitId == "pound") {
        if (lowerExercise) 10.0 else 5.0
    } else {
        if (lowerExercise) 5.0 else 2.5
    }
}

internal data class FiveThreeOneProgramExercise(
    /** Standard-role identity is required by the two Wendler presets and absent for custom exercises. */
    val role: FiveThreeOneExerciseRole?,
    val exerciseId: Long,
    val exerciseName: String,
    val trainingMax: Double,
    val unitId: String,
    val loadIncrement: Double,
    val cycleIncrement: Double,
    val trainingMaxPercent: Double = 85.0,
    val trainingMaxBasisKind: TrainingMaxBasisKind = TrainingMaxBasisKind.ExplicitTrainingMax,
    val trainingMaxBasisValue: Double? = null,
    val trainingMaxBasisUnitId: String = "",
)

internal enum class FiveThreeOneSetSection(val label: String) {
    Main("Main Work"),
    Supplemental("Supplemental"),
    Optional("Optional Work"),
}

internal data class FiveThreeOneSetPlan(
    val section: FiveThreeOneSetSection,
    val percentageOfTrainingMax: Double,
    val repetitions: Int,
    val repetitionsMax: Int? = null,
    val amrap: Boolean = false,
    val phase: FiveThreeOnePhase? = null,
    val optionalWorkKind: RoutineOptionalWorkKind = RoutineOptionalWorkKind.None,
    val optionalOrdinal: Int? = null,
    val optionalCount: Int? = null,
    val classificationOverride: WorkoutSetClassification? = null,
) {
    val classification: WorkoutSetClassification
        get() = when {
            classificationOverride != null -> classificationOverride
            amrap -> WorkoutSetClassification.Amrap
            section == FiveThreeOneSetSection.Supplemental -> WorkoutSetClassification.BackOff
            section == FiveThreeOneSetSection.Optional -> WorkoutSetClassification.Working
            else -> WorkoutSetClassification.Working
        }

    val workSection: RoutineWorkSection
        get() = when (section) {
            FiveThreeOneSetSection.Main -> RoutineWorkSection.Main
            FiveThreeOneSetSection.Supplemental -> RoutineWorkSection.Supplemental
            FiveThreeOneSetSection.Optional -> RoutineWorkSection.Optional
        }

    val repetitionLabel: String
        get() = when {
            amrap -> "$repetitions+ · AMRAP"
            repetitionsMax != null -> "$repetitions–$repetitionsMax"
            else -> repetitions.toString()
        }
}

internal data class FiveThreeOnePreviewSet(
    val plan: FiveThreeOneSetPlan,
    val roundedLoad: Double,
)

internal data class FiveThreeOneAuthoringConfig(
    val trainingMax: Double,
    val mainScheme: FiveThreeOneMainScheme,
    val phase: FiveThreeOnePhase,
    val supplement: FiveThreeOneSupplement,
    val classicFinalSetAmrap: Boolean = true,
    val boringButBigPercent: Double = 50.0,
    val jokerSetsEnabled: Boolean = false,
    val jokerSetCount: Int = if (jokerSetsEnabled) 1 else 0,
    val jokerStepPercent: Double = 5.0,
)

internal data class FiveThreeOneBuilderResult(
    val sets: List<RoutineBuilderSetState>,
    val trainingMax: Double,
    val trainingMaxUnitId: String,
    val cycleIncrementValue: Double,
    val programKind: RoutineProgramKind,
    val mainWorkScheme: RoutineMainWorkScheme,
    val supplementalScheme: RoutineSupplementalScheme,
    val jokerSetsEnabled: Boolean,
)

internal fun fiveThreeOneSetPlans(config: FiveThreeOneAuthoringConfig): List<FiveThreeOneSetPlan> {
    require(config.trainingMax.isFinite() && config.trainingMax > 0.0) { "Training max must be above zero" }
    require(config.boringButBigPercent.isFinite() && config.boringButBigPercent in 1.0..100.0) {
        "BBB percentage must be from 1 to 100%"
    }
    require(config.jokerSetCount in 0..3) { "Joker ladder must contain from zero to three candidates" }
    require(config.jokerStepPercent == 5.0 || config.jokerStepPercent == 10.0) {
        "Joker steps must be 5% or 10% of Training Max"
    }
    val mainPercentages = when (config.phase) {
        FiveThreeOnePhase.Fives -> listOf(65.0, 75.0, 85.0)
        FiveThreeOnePhase.Threes -> listOf(70.0, 80.0, 90.0)
        FiveThreeOnePhase.FiveThreeOne -> listOf(75.0, 85.0, 95.0)
        FiveThreeOnePhase.Deload -> listOf(40.0, 50.0, 60.0)
    }
    val classicRepetitions = when (config.phase) {
        FiveThreeOnePhase.Fives -> listOf(5, 5, 5)
        FiveThreeOnePhase.Threes -> listOf(3, 3, 3)
        FiveThreeOnePhase.FiveThreeOne -> listOf(5, 3, 1)
        FiveThreeOnePhase.Deload -> listOf(5, 5, 5)
    }
    val repetitions = if (config.mainScheme == FiveThreeOneMainScheme.FivesPro) {
        listOf(5, 5, 5)
    } else {
        classicRepetitions
    }
    val allowAmrap = config.mainScheme == FiveThreeOneMainScheme.Classic &&
        config.phase != FiveThreeOnePhase.Deload &&
        config.classicFinalSetAmrap
    val main = mainPercentages.mapIndexed { index, percentage ->
        FiveThreeOneSetPlan(
            section = FiveThreeOneSetSection.Main,
            percentageOfTrainingMax = percentage,
            repetitions = repetitions[index],
            amrap = allowAmrap && index == mainPercentages.lastIndex,
            phase = config.phase,
        )
    }
    val jokerCount = config.jokerSetCount.takeIf { config.jokerSetsEnabled }.orZero()
    val joker = if (jokerCount > 0 && config.phase != FiveThreeOnePhase.Deload) {
        List(jokerCount) { index ->
            FiveThreeOneSetPlan(
                section = FiveThreeOneSetSection.Optional,
                percentageOfTrainingMax = mainPercentages.last() + config.jokerStepPercent * (index + 1),
                repetitions = classicRepetitions.last(),
                phase = config.phase,
                optionalWorkKind = RoutineOptionalWorkKind.Joker,
                optionalOrdinal = index + 1,
                optionalCount = jokerCount,
            )
        }
    } else {
        emptyList()
    }
    // Wendler's public BBB guidance explicitly keeps 5x10 (or reduces it to 3x10) during
    // deload. The cited public material does not establish the same default for FSL, SSL,
    // or BBS, so generated programs omit those supplements from Deload. Advanced phase
    // editing remains the explicit route for a lifter who follows a different prescription.
    if (config.phase == FiveThreeOnePhase.Deload && config.supplement != FiveThreeOneSupplement.BoringButBig) {
        return main + joker
    }
    val supplementalPercentage = when (config.supplement) {
        FiveThreeOneSupplement.None -> return main + joker
        FiveThreeOneSupplement.BoringButBig -> config.boringButBigPercent
        FiveThreeOneSupplement.FirstSetLast -> mainPercentages.first()
        FiveThreeOneSupplement.SecondSetLast -> mainPercentages[1]
        FiveThreeOneSupplement.BoringButStrong -> mainPercentages.first()
    }
    val supplementalRepetitions = when (config.supplement) {
        FiveThreeOneSupplement.BoringButBig -> 10
        FiveThreeOneSupplement.FirstSetLast,
        FiveThreeOneSupplement.SecondSetLast,
        FiveThreeOneSupplement.BoringButStrong,
        -> 5
        FiveThreeOneSupplement.None -> error("Handled above")
    }
    val supplementalSetCount = if (config.supplement == FiveThreeOneSupplement.BoringButStrong) 10 else 5
    return main + joker + List(supplementalSetCount) {
        FiveThreeOneSetPlan(
            section = FiveThreeOneSetSection.Supplemental,
            percentageOfTrainingMax = supplementalPercentage,
            repetitions = supplementalRepetitions,
            // BBB is unchanged across the cycle, while FSL follows each phase's first set.
            phase = config.phase.takeIf {
                config.supplement in setOf(
                    FiveThreeOneSupplement.FirstSetLast,
                    FiveThreeOneSupplement.SecondSetLast,
                    FiveThreeOneSupplement.BoringButStrong,
                )
            },
        )
    }
}

private fun Int?.orZero(): Int = this ?: 0

internal fun fiveThreeOneSeventhWeekSetPlans(
    protocol: FiveThreeOneSeventhWeekProtocol,
): List<FiveThreeOneSetPlan> = when (protocol) {
    FiveThreeOneSeventhWeekProtocol.Deload -> listOf(
        FiveThreeOneSetPlan(FiveThreeOneSetSection.Main, 70.0, 5),
        FiveThreeOneSetPlan(FiveThreeOneSetSection.Main, 80.0, 3, repetitionsMax = 5),
        FiveThreeOneSetPlan(FiveThreeOneSetSection.Main, 90.0, 1),
        FiveThreeOneSetPlan(FiveThreeOneSetSection.Main, 100.0, 1),
    )
    FiveThreeOneSeventhWeekProtocol.TrainingMaxTest -> listOf(
        FiveThreeOneSetPlan(FiveThreeOneSetSection.Main, 70.0, 5),
        FiveThreeOneSetPlan(FiveThreeOneSetSection.Main, 80.0, 5),
        FiveThreeOneSetPlan(FiveThreeOneSetSection.Main, 90.0, 5),
        FiveThreeOneSetPlan(
            FiveThreeOneSetSection.Main,
            100.0,
            3,
            repetitionsMax = 5,
            classificationOverride = WorkoutSetClassification.TrainingMaxTest,
        ),
    )
    FiveThreeOneSeventhWeekProtocol.PersonalRecordTest -> listOf(
        FiveThreeOneSetPlan(FiveThreeOneSetSection.Main, 70.0, 5),
        FiveThreeOneSetPlan(FiveThreeOneSetSection.Main, 80.0, 5),
        FiveThreeOneSetPlan(FiveThreeOneSetSection.Main, 90.0, 5),
        FiveThreeOneSetPlan(FiveThreeOneSetSection.Main, 100.0, 1, amrap = true),
    )
}

internal fun FiveThreeOneSeventhWeekProtocol.phaseRole(): RoutineProgramPhaseRole = when (this) {
    FiveThreeOneSeventhWeekProtocol.Deload -> RoutineProgramPhaseRole.Deload
    FiveThreeOneSeventhWeekProtocol.TrainingMaxTest -> RoutineProgramPhaseRole.TrainingMaxTest
    FiveThreeOneSeventhWeekProtocol.PersonalRecordTest -> RoutineProgramPhaseRole.PersonalRecordTest
}

internal fun roundedFiveThreeOneLoad(
    rawLoad: Double,
    increment: Double,
    availableLoads: List<Double> = emptyList(),
): Double {
    require(rawLoad.isFinite() && rawLoad >= 0.0)
    val choices = availableLoads.filter { it.isFinite() && it >= 0.0 }.distinct()
    if (choices.isNotEmpty()) return choices.minWith(compareBy<Double> { abs(it - rawLoad) }.thenBy { it })
    val step = increment.takeIf { it.isFinite() && it > 0.0 } ?: 1.0
    return (round(rawLoad / step) * step).coerceAtLeast(0.0)
}

internal fun previewFiveThreeOneSets(
    config: FiveThreeOneAuthoringConfig,
    increment: Double,
    availableLoads: List<Double> = emptyList(),
): List<FiveThreeOnePreviewSet> = fiveThreeOneSetPlans(config).map { plan ->
    FiveThreeOnePreviewSet(
        plan = plan,
        roundedLoad = roundedFiveThreeOneLoad(
            rawLoad = config.trainingMax * plan.percentageOfTrainingMax / 100.0,
            increment = increment,
            availableLoads = availableLoads,
        ),
    )
}

/** Builds the entire four-phase cycle while keeping the chosen phase as a preview concern only. */
internal fun previewFiveThreeOneCycle(
    config: FiveThreeOneAuthoringConfig,
    increment: Double,
    availableLoads: List<Double> = emptyList(),
): List<FiveThreeOnePreviewSet> {
    val previewsByPhase = FiveThreeOnePhase.entries.map { phase ->
        previewFiveThreeOneSets(config.copy(phase = phase), increment, availableLoads)
    }
    val phaseSpecificSets = previewsByPhase.flatten().filter { it.plan.phase != null }
    val everyPhaseSets = previewsByPhase.first().filter { it.plan.phase == null }
    return phaseSpecificSets + everyPhaseSets
}

internal fun fiveThreeOneProgramKind(config: FiveThreeOneAuthoringConfig): RoutineProgramKind = when (config.supplement) {
    FiveThreeOneSupplement.None,
    FiveThreeOneSupplement.BoringButBig,
    FiveThreeOneSupplement.FirstSetLast,
    FiveThreeOneSupplement.SecondSetLast,
    FiveThreeOneSupplement.BoringButStrong,
    -> RoutineProgramKind.FiveThreeOne
}

internal fun fiveThreeOneMainWorkScheme(config: FiveThreeOneAuthoringConfig): RoutineMainWorkScheme = when {
    config.mainScheme == FiveThreeOneMainScheme.FivesPro -> RoutineMainWorkScheme.FivesPro
    config.classicFinalSetAmrap -> RoutineMainWorkScheme.ClassicPrSet
    else -> RoutineMainWorkScheme.ClassicMinimumReps
}

internal fun fiveThreeOneSupplementalScheme(config: FiveThreeOneAuthoringConfig): RoutineSupplementalScheme = when (config.supplement) {
    FiveThreeOneSupplement.None -> RoutineSupplementalScheme.None
    FiveThreeOneSupplement.FirstSetLast -> RoutineSupplementalScheme.FirstSetLast
    FiveThreeOneSupplement.SecondSetLast -> RoutineSupplementalScheme.SecondSetLast
    FiveThreeOneSupplement.BoringButBig -> RoutineSupplementalScheme.BoringButBig
    FiveThreeOneSupplement.BoringButStrong -> RoutineSupplementalScheme.BoringButStrong
}

internal fun suggestedFiveThreeOneTrainingMax(
    estimatedOneRepMax: Double,
    increment: Double,
    percentage: Double = 85.0,
    availableLoads: List<Double> = emptyList(),
): Double {
    require(estimatedOneRepMax.isFinite() && estimatedOneRepMax > 0.0)
    require(percentage.isFinite() && percentage in 1.0..100.0)
    return roundedFiveThreeOneLoad(
        rawLoad = estimatedOneRepMax * percentage / 100.0,
        increment = increment,
        availableLoads = availableLoads,
    )
}

/**
 * Saves percentages against the routine's explicit training max. The workout resolver performs
 * equipment-aware rounding when a session starts, so loads remain stable when an estimated 1RM
 * changes while still increasing after the routine advances to a new cycle.
 */
internal fun fiveThreeOneBuilderSets(
    existingSets: List<RoutineBuilderSetState>,
    previews: List<FiveThreeOnePreviewSet>,
    mainWorkScheme: RoutineMainWorkScheme? = null,
    supplementalScheme: RoutineSupplementalScheme? = null,
    routinePhaseIndexOverride: Int? = null,
): List<RoutineBuilderSetState> {
    var nextKey = (existingSets.maxOfOrNull(RoutineBuilderSetState::key) ?: 0L) + 1L
    return previews.mapIndexed { index, preview ->
        val existing = existingSets.getOrNull(index)
        RoutineBuilderSetState(
            key = existing?.key ?: nextKey++,
            load = "",
            repetitionsMin = preview.plan.repetitions.toString(),
            repetitionsMax = preview.plan.repetitionsMax?.toString().orEmpty(),
            classification = preview.plan.classification.name,
            restSeconds = existing?.restSeconds.orEmpty(),
            note = buildString {
                append(preview.plan.section.label)
                preview.plan.phase?.let { append(" · ${it.label}") }
                append(" · ")
                append(com.whip.app.domain.editableNumericValue(preview.plan.percentageOfTrainingMax))
                append("% TM")
                if (preview.plan.amrap) append(" · PR set, minimum ${preview.plan.repetitions} reps; stop before technical failure")
                preview.plan.optionalOrdinal?.let { ordinal ->
                    append(" · Joker $ordinal of ${preview.plan.optionalCount ?: ordinal}")
                }
            },
            loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax.name,
            loadPercentage = com.whip.app.domain.editableNumericValue(preview.plan.percentageOfTrainingMax),
            routinePhaseIndex = routinePhaseIndexOverride ?: preview.plan.phase?.ordinal,
            workSection = preview.plan.workSection.name,
            optionalWorkKind = preview.plan.optionalWorkKind.name,
            mainWorkScheme = if (preview.plan.workSection == RoutineWorkSection.Main) {
                if (preview.plan.phase == FiveThreeOnePhase.Deload && mainWorkScheme == RoutineMainWorkScheme.ClassicPrSet) {
                    RoutineMainWorkScheme.ClassicMinimumReps.name
                } else {
                    mainWorkScheme?.name
                }
            } else null,
            supplementalScheme = supplementalScheme?.name
                .takeIf { preview.plan.workSection == RoutineWorkSection.Supplemental },
        )
    }
}

/** Builds an intentional whole program from either a standard layout or the lifter's chosen exercises. */
internal fun buildFiveThreeOneProgramState(
    current: RoutineBuilderState,
    layout: FiveThreeOneProgramLayout,
    exercises: List<FiveThreeOneProgramExercise>,
    mainScheme: FiveThreeOneMainScheme,
    supplement: FiveThreeOneSupplement,
    jokerSetsEnabled: Boolean,
    classicFinalSetAmrap: Boolean = true,
    boringButBigPercent: Double = 50.0,
    progressionMode: RoutineProgressionMode = RoutineProgressionMode.Standard,
): RoutineBuilderState {
    when (layout) {
        FiveThreeOneProgramLayout.FourDay,
        FiveThreeOneProgramLayout.Beginners,
        -> {
            require(exercises.size == FiveThreeOneExerciseRole.entries.size)
            require(exercises.mapNotNull(FiveThreeOneProgramExercise::role).toSet() == FiveThreeOneExerciseRole.entries.toSet())
        }
        FiveThreeOneProgramLayout.Custom -> {
            require(exercises.isNotEmpty())
            require(exercises.all { it.role == null })
        }
    }
    require(exercises.map(FiveThreeOneProgramExercise::exerciseId).distinct().size == exercises.size)
    require(exercises.all {
        it.exerciseId > 0L && it.trainingMax.isFinite() && it.trainingMax > 0.0 &&
            it.loadIncrement.isFinite() && it.loadIncrement > 0.0 &&
            it.cycleIncrement.isFinite() && it.cycleIncrement > 0.0
    })
    require(boringButBigPercent.isFinite() && boringButBigPercent in 1.0..100.0)
    val exerciseByRole = exercises.mapNotNull { exercise -> exercise.role?.let { role -> role to exercise } }.toMap()
    val schedule = when (layout) {
        FiveThreeOneProgramLayout.FourDay -> listOf(
            "Squat" to listOf(requireNotNull(exerciseByRole[FiveThreeOneExerciseRole.Squat])),
            "Bench" to listOf(requireNotNull(exerciseByRole[FiveThreeOneExerciseRole.Bench])),
            "Deadlift" to listOf(requireNotNull(exerciseByRole[FiveThreeOneExerciseRole.Deadlift])),
            "Press" to listOf(requireNotNull(exerciseByRole[FiveThreeOneExerciseRole.Press])),
        )
        FiveThreeOneProgramLayout.Beginners -> listOf(
            "Monday · Squat + Bench" to listOf(
                requireNotNull(exerciseByRole[FiveThreeOneExerciseRole.Squat]),
                requireNotNull(exerciseByRole[FiveThreeOneExerciseRole.Bench]),
            ),
            "Wednesday · Deadlift + Press" to listOf(
                requireNotNull(exerciseByRole[FiveThreeOneExerciseRole.Deadlift]),
                requireNotNull(exerciseByRole[FiveThreeOneExerciseRole.Press]),
            ),
            "Friday · Bench + Squat" to listOf(
                requireNotNull(exerciseByRole[FiveThreeOneExerciseRole.Bench]),
                requireNotNull(exerciseByRole[FiveThreeOneExerciseRole.Squat]),
            ),
        )
        FiveThreeOneProgramLayout.Custom -> exercises.map { exercise -> exercise.exerciseName to listOf(exercise) }
    }
    var nextKey = current.nextKey.coerceAtLeast(1L)
    fun placement(exercise: FiveThreeOneProgramExercise): RoutineBuilderPlacementState {
        val config = FiveThreeOneAuthoringConfig(
            trainingMax = exercise.trainingMax,
            mainScheme = mainScheme,
            phase = FiveThreeOnePhase.Fives,
            supplement = if (layout == FiveThreeOneProgramLayout.Beginners) FiveThreeOneSupplement.FirstSetLast else supplement,
            classicFinalSetAmrap = classicFinalSetAmrap,
            boringButBigPercent = boringButBigPercent,
            jokerSetsEnabled = jokerSetsEnabled,
        )
        val placementKey = nextKey++
        val generated = fiveThreeOneBuilderSets(
            emptyList(),
            previewFiveThreeOneCycle(config, exercise.loadIncrement),
            mainWorkScheme = fiveThreeOneMainWorkScheme(config),
            supplementalScheme = fiveThreeOneSupplementalScheme(config),
        )
            .map { it.copy(key = nextKey++) }
        return RoutineBuilderPlacementState(
            key = placementKey,
            exerciseId = exercise.exerciseId,
            exerciseNameSnapshot = exercise.exerciseName,
            copyPreviousWorkout = false,
            sets = generated,
            trainingMaxValue = com.whip.app.domain.editableNumericValue(exercise.trainingMax),
            trainingMaxUnitId = exercise.unitId,
            cycleIncrementValue = com.whip.app.domain.editableNumericValue(exercise.cycleIncrement),
            trainingMaxSource = "Explicit",
            trainingMaxPercent = com.whip.app.domain.editableNumericValue(exercise.trainingMaxPercent),
            trainingMaxBasisKind = exercise.trainingMaxBasisKind.name,
            trainingMaxBasisValue = exercise.trainingMaxBasisValue
                ?.let { value -> com.whip.app.domain.editableNumericValue(value) }.orEmpty(),
            trainingMaxBasisUnitId = exercise.trainingMaxBasisUnitId,
            mainWorkScheme = fiveThreeOneMainWorkScheme(config).name,
            supplementalScheme = fiveThreeOneSupplementalScheme(config).name,
            assistanceRole = RoutineAssistanceRole.MainExercise.name,
            placementKind = RoutinePlacementKind.MainExercise.name,
            assistanceCategory = RoutineAssistanceCategory.Unspecified.name,
            jokerSetsEnabled = jokerSetsEnabled,
        )
    }
    val days = schedule.map { (name, scheduledExercises) ->
        RoutineBuilderDayState(key = nextKey++, name = name, placements = scheduledExercises.map(::placement))
    }
    val defaultName = when (layout) {
        FiveThreeOneProgramLayout.FourDay -> "4-Day 5/3/1"
        FiveThreeOneProgramLayout.Beginners -> "5/3/1 for Beginners"
        FiveThreeOneProgramLayout.Custom -> "Custom 5/3/1"
    }
    val assistanceGuidance = when (layout) {
        FiveThreeOneProgramLayout.Beginners ->
            "Add assistance after the main and FSL work: choose one Push, one Pull, and one Single-leg/Core movement each day."
        FiveThreeOneProgramLayout.FourDay,
        FiveThreeOneProgramLayout.Custom,
        -> "Main work and supplemental work are generated separately. Add assistance as Push, Pull, or Single-leg/Core where appropriate."
    }
    return current.copy(
        name = current.name.ifBlank { defaultName },
        notes = current.notes.ifBlank { assistanceGuidance },
        days = days,
        selectedDayKey = days.first().key,
        selectedPlacementKey = null,
        nextKey = nextKey,
        programKind = RoutineProgramKind.FiveThreeOne.name,
        programTemplateKey = when (layout) {
            FiveThreeOneProgramLayout.FourDay -> RoutineProgramTemplateKey.FiveThreeOneFourDay
            FiveThreeOneProgramLayout.Beginners -> RoutineProgramTemplateKey.FiveThreeOneBeginners
            FiveThreeOneProgramLayout.Custom -> RoutineProgramTemplateKey.FiveThreeOneCustom
        }.name,
        programTemplateRevision = 1,
        progressionMode = progressionMode.name,
        allowNonStandardHigherSuggestions = false,
        programPhaseCount = FiveThreeOnePhase.entries.size,
        programPhaseLabels = FiveThreeOnePhase.entries.map(FiveThreeOnePhase::label),
        programPhaseRoles = listOf(
            RoutineProgramPhaseRole.Standard.name,
            RoutineProgramPhaseRole.Standard.name,
            RoutineProgramPhaseRole.Standard.name,
            RoutineProgramPhaseRole.Deload.name,
        ),
        trainingMaxAdvanceAfterPhaseIndices = setOf(FiveThreeOnePhase.Deload.ordinal),
        currentProgramPhaseIndexHint = 0,
        nextProgramDayKeyHint = days.firstOrNull()?.key,
    )
}

private data class FiveThreeOneGeneratedPhase(
    val label: String,
    val role: RoutineProgramPhaseRole,
    val mainPhase: FiveThreeOnePhase? = null,
    val mainScheme: FiveThreeOneMainScheme = FiveThreeOneMainScheme.Classic,
    val supplement: FiveThreeOneSupplement = FiveThreeOneSupplement.None,
    val protocol: FiveThreeOneSeventhWeekProtocol? = null,
    val allowJokers: Boolean = false,
)

private fun FiveThreeOneProgramRequest.generatedPhases(): List<FiveThreeOneGeneratedPhase> = when (plan) {
    FiveThreeOneProgramPlan.SingleCycle -> listOf(
        FiveThreeOneGeneratedPhase("5s Week", RoutineProgramPhaseRole.Standard, FiveThreeOnePhase.Fives, mainScheme, supplement, allowJokers = true),
        FiveThreeOneGeneratedPhase("3s Week", RoutineProgramPhaseRole.Standard, FiveThreeOnePhase.Threes, mainScheme, supplement, allowJokers = true),
        FiveThreeOneGeneratedPhase("5/3/1 Week", RoutineProgramPhaseRole.Standard, FiveThreeOnePhase.FiveThreeOne, mainScheme, supplement, allowJokers = true),
        FiveThreeOneGeneratedPhase(
            "7th Week · ${closingProtocol.label}",
            closingProtocol.phaseRole().asOncePerExerciseProtocol(),
            protocol = closingProtocol,
        ),
    )
    FiveThreeOneProgramPlan.ForeverBbbLeaderAnchor,
    FiveThreeOneProgramPlan.ForeverFslLeaderAnchor,
    -> {
        require(layout != FiveThreeOneProgramLayout.Beginners) {
            "Leader/Anchor presets require one programmed main exercise per day"
        }
        val leaderSupplement = if (plan == FiveThreeOneProgramPlan.ForeverBbbLeaderAnchor) {
            FiveThreeOneSupplement.BoringButBig
        } else {
            FiveThreeOneSupplement.FirstSetLast
        }
        buildList {
            repeat(2) { cycle ->
                add(FiveThreeOneGeneratedPhase("Leader ${cycle + 1} · 5s Week", RoutineProgramPhaseRole.Leader, FiveThreeOnePhase.Fives, FiveThreeOneMainScheme.FivesPro, leaderSupplement))
                add(FiveThreeOneGeneratedPhase("Leader ${cycle + 1} · 3s Week", RoutineProgramPhaseRole.Leader, FiveThreeOnePhase.Threes, FiveThreeOneMainScheme.FivesPro, leaderSupplement))
                add(FiveThreeOneGeneratedPhase("Leader ${cycle + 1} · 5/3/1 Week", RoutineProgramPhaseRole.Leader, FiveThreeOnePhase.FiveThreeOne, FiveThreeOneMainScheme.FivesPro, leaderSupplement))
            }
            add(
                FiveThreeOneGeneratedPhase(
                    "7th Week · Deload",
                    RoutineProgramPhaseRole.Deload.asOncePerExerciseProtocol(),
                    protocol = FiveThreeOneSeventhWeekProtocol.Deload,
                ),
            )
            add(FiveThreeOneGeneratedPhase("Anchor · 5s Week", RoutineProgramPhaseRole.Anchor, FiveThreeOnePhase.Fives, FiveThreeOneMainScheme.Classic, FiveThreeOneSupplement.FirstSetLast, allowJokers = true))
            add(FiveThreeOneGeneratedPhase("Anchor · 3s Week", RoutineProgramPhaseRole.Anchor, FiveThreeOnePhase.Threes, FiveThreeOneMainScheme.Classic, FiveThreeOneSupplement.FirstSetLast, allowJokers = true))
            add(FiveThreeOneGeneratedPhase("Anchor · 5/3/1 Week", RoutineProgramPhaseRole.Anchor, FiveThreeOnePhase.FiveThreeOne, FiveThreeOneMainScheme.Classic, FiveThreeOneSupplement.FirstSetLast, allowJokers = true))
            add(
                FiveThreeOneGeneratedPhase(
                    "7th Week · ${closingProtocol.label}",
                    closingProtocol.phaseRole().asOncePerExerciseProtocol(),
                    protocol = closingProtocol,
                ),
            )
        }
    }
}

private fun FiveThreeOneProgramRequest.schedule(): List<Pair<String, List<FiveThreeOneProgramExercise>>> {
    val exerciseByRole = exercises.mapNotNull { exercise -> exercise.role?.let { role -> role to exercise } }.toMap()
    return when (layout) {
        FiveThreeOneProgramLayout.FourDay -> listOf(
            "Squat" to listOf(requireNotNull(exerciseByRole[FiveThreeOneExerciseRole.Squat])),
            "Bench" to listOf(requireNotNull(exerciseByRole[FiveThreeOneExerciseRole.Bench])),
            "Deadlift" to listOf(requireNotNull(exerciseByRole[FiveThreeOneExerciseRole.Deadlift])),
            "Press" to listOf(requireNotNull(exerciseByRole[FiveThreeOneExerciseRole.Press])),
        )
        FiveThreeOneProgramLayout.Beginners -> listOf(
            "Monday · Squat + Bench" to listOf(requireNotNull(exerciseByRole[FiveThreeOneExerciseRole.Squat]), requireNotNull(exerciseByRole[FiveThreeOneExerciseRole.Bench])),
            "Wednesday · Deadlift + Press" to listOf(requireNotNull(exerciseByRole[FiveThreeOneExerciseRole.Deadlift]), requireNotNull(exerciseByRole[FiveThreeOneExerciseRole.Press])),
            "Friday · Bench + Squat" to listOf(requireNotNull(exerciseByRole[FiveThreeOneExerciseRole.Bench]), requireNotNull(exerciseByRole[FiveThreeOneExerciseRole.Squat])),
        )
        FiveThreeOneProgramLayout.Custom -> exercises.map { exercise -> exercise.exerciseName to listOf(exercise) }
    }
}

/** Builds the expanded editable program without changing or recomputing any saved routine. */
internal fun buildFiveThreeOneProgramState(
    current: RoutineBuilderState,
    request: FiveThreeOneProgramRequest,
): RoutineBuilderState {
    val exercises = request.exercises
    when (request.layout) {
        FiveThreeOneProgramLayout.FourDay,
        FiveThreeOneProgramLayout.Beginners,
        -> {
            require(exercises.size == FiveThreeOneExerciseRole.entries.size)
            require(exercises.mapNotNull(FiveThreeOneProgramExercise::role).toSet() == FiveThreeOneExerciseRole.entries.toSet())
        }
        FiveThreeOneProgramLayout.Custom -> require(exercises.isNotEmpty() && exercises.all { it.role == null })
    }
    require(exercises.map(FiveThreeOneProgramExercise::exerciseId).distinct().size == exercises.size)
    require(exercises.all { exercise ->
        exercise.exerciseId > 0L && exercise.trainingMax.isFinite() && exercise.trainingMax > 0.0 &&
            exercise.loadIncrement.isFinite() && exercise.loadIncrement > 0.0 &&
            exercise.cycleIncrement.isFinite() && exercise.cycleIncrement > 0.0
    })
    require(request.boringButBigPercent.isFinite() && request.boringButBigPercent in 1.0..100.0)
    require(request.assistance.map(FiveThreeOneAssistanceChoice::category).distinct().size == request.assistance.size)
    require(request.assistance.all { it.category in setOf(
        RoutineAssistanceCategory.Push,
        RoutineAssistanceCategory.Pull,
        RoutineAssistanceCategory.SingleLegCore,
    ) && it.exerciseId !in exercises.map(FiveThreeOneProgramExercise::exerciseId) })
    val exerciseById = exercises.associateBy(FiveThreeOneProgramExercise::exerciseId)
    require(request.bbbExerciseByMainExerciseId.all { (mainId, supplementalId) ->
        mainId in exerciseById && supplementalId in exerciseById
    })

    val phases = request.generatedPhases()
    val scheduledDays = request.schedule()
    val protocolOwnerDayByExerciseId = balancedOncePerExerciseDayOwners(
        scheduledDays.map { (_, scheduledExercises) -> scheduledExercises.map(FiveThreeOneProgramExercise::exerciseId) },
    )
    var nextKey = current.nextKey.coerceAtLeast(1L)

    fun rekey(sets: List<RoutineBuilderSetState>): List<RoutineBuilderSetState> =
        sets.map { set -> set.copy(key = nextKey++) }

    fun plansFor(
        exercise: FiveThreeOneProgramExercise,
        dayIndex: Int,
        phaseIndex: Int,
        definition: FiveThreeOneGeneratedPhase,
    ): List<RoutineBuilderSetState> {
        val protocol = definition.protocol
        if (protocol != null) {
            val plans = fiveThreeOneSeventhWeekSetPlans(protocol).filterNot { plan ->
                protocol == FiveThreeOneSeventhWeekProtocol.TrainingMaxTest &&
                    protocolOwnerDayByExerciseId[exercise.exerciseId] != dayIndex &&
                    plan.classification == WorkoutSetClassification.TrainingMaxTest
            }
            val previews = plans.map { plan ->
                FiveThreeOnePreviewSet(
                    plan,
                    roundedFiveThreeOneLoad(exercise.trainingMax * plan.percentageOfTrainingMax / 100.0, exercise.loadIncrement),
                )
            }
            val protocolMainScheme = if (protocol == FiveThreeOneSeventhWeekProtocol.PersonalRecordTest) {
                RoutineMainWorkScheme.ClassicPrSet
            } else {
                RoutineMainWorkScheme.ClassicMinimumReps
            }
            return rekey(fiveThreeOneBuilderSets(emptyList(), previews, protocolMainScheme, RoutineSupplementalScheme.None, phaseIndex))
        }
        val ladder = request.jokerLadder.takeIf { definition.allowJokers } ?: FiveThreeOneJokerLadder()
        val config = FiveThreeOneAuthoringConfig(
            trainingMax = exercise.trainingMax,
            mainScheme = definition.mainScheme,
            phase = requireNotNull(definition.mainPhase),
            supplement = definition.supplement,
            classicFinalSetAmrap = definition.mainScheme == FiveThreeOneMainScheme.Classic && request.classicFinalSetAmrap,
            boringButBigPercent = request.boringButBigPercent,
            jokerSetsEnabled = ladder.count > 0,
            jokerSetCount = ladder.count,
            jokerStepPercent = ladder.stepPercent,
        )
        return rekey(
            fiveThreeOneBuilderSets(
                emptyList(),
                previewFiveThreeOneSets(config, exercise.loadIncrement),
                fiveThreeOneMainWorkScheme(config),
                fiveThreeOneSupplementalScheme(config),
                phaseIndex,
            ),
        )
    }

    fun mainPlacement(exercise: FiveThreeOneProgramExercise, dayIndex: Int): RoutineBuilderPlacementState {
        val alternateBbbTarget = request.bbbExerciseByMainExerciseId[exercise.exerciseId]
        val generated = phases.flatMapIndexed { phaseIndex, definition ->
            plansFor(exercise, dayIndex, phaseIndex, definition).filterNot { set ->
                alternateBbbTarget != null && alternateBbbTarget != exercise.exerciseId &&
                    definition.supplement == FiveThreeOneSupplement.BoringButBig &&
                    set.workSection == RoutineWorkSection.Supplemental.name
            }
        }
        val mainSchemes = generated.mapNotNull(RoutineBuilderSetState::mainWorkScheme).distinct()
        val supplementalSchemes = phases.map { phase -> fiveThreeOneSupplementalScheme(
            FiveThreeOneAuthoringConfig(
                trainingMax = exercise.trainingMax,
                mainScheme = phase.mainScheme,
                phase = phase.mainPhase ?: FiveThreeOnePhase.Deload,
                supplement = phase.supplement,
            ),
        ).name }.distinct()
        return RoutineBuilderPlacementState(
            key = nextKey++,
            exerciseId = exercise.exerciseId,
            exerciseNameSnapshot = exercise.exerciseName,
            copyPreviousWorkout = false,
            sets = generated,
            trainingMaxValue = com.whip.app.domain.editableNumericValue(exercise.trainingMax),
            trainingMaxUnitId = exercise.unitId,
            cycleIncrementValue = com.whip.app.domain.editableNumericValue(exercise.cycleIncrement),
            trainingMaxSource = "Explicit",
            trainingMaxPercent = com.whip.app.domain.editableNumericValue(exercise.trainingMaxPercent),
            trainingMaxBasisKind = exercise.trainingMaxBasisKind.name,
            trainingMaxBasisValue = exercise.trainingMaxBasisValue
                ?.let { value -> com.whip.app.domain.editableNumericValue(value) }.orEmpty(),
            trainingMaxBasisUnitId = exercise.trainingMaxBasisUnitId,
            mainWorkScheme = mainSchemes.singleOrNull() ?: RoutineMainWorkScheme.Unspecified.name,
            supplementalScheme = supplementalSchemes.singleOrNull() ?: RoutineSupplementalScheme.Custom.name,
            assistanceRole = RoutineAssistanceRole.MainExercise.name,
            placementKind = RoutinePlacementKind.MainExercise.name,
            assistanceCategory = RoutineAssistanceCategory.Unspecified.name,
            jokerSetsEnabled = generated.any { it.optionalWorkKind == RoutineOptionalWorkKind.Joker.name },
        )
    }

    fun alternateBbbPlacement(mainExercise: FiveThreeOneProgramExercise): RoutineBuilderPlacementState? {
        val targetId = request.bbbExerciseByMainExerciseId[mainExercise.exerciseId] ?: return null
        if (targetId == mainExercise.exerciseId) return null
        val target = requireNotNull(exerciseById[targetId])
        val generated = phases.flatMapIndexed { phaseIndex, definition ->
            if (definition.supplement != FiveThreeOneSupplement.BoringButBig) return@flatMapIndexed emptyList()
            val config = FiveThreeOneAuthoringConfig(
                trainingMax = target.trainingMax,
                mainScheme = definition.mainScheme,
                phase = requireNotNull(definition.mainPhase),
                supplement = FiveThreeOneSupplement.BoringButBig,
                classicFinalSetAmrap = false,
                boringButBigPercent = request.boringButBigPercent,
            )
            rekey(
                fiveThreeOneBuilderSets(
                    emptyList(),
                    previewFiveThreeOneSets(config, target.loadIncrement).filter {
                        it.plan.section == FiveThreeOneSetSection.Supplemental
                    },
                    supplementalScheme = RoutineSupplementalScheme.BoringButBig,
                    routinePhaseIndexOverride = phaseIndex,
                ),
            )
        }
        if (generated.isEmpty()) return null
        return RoutineBuilderPlacementState(
            key = nextKey++,
            exerciseId = target.exerciseId,
            exerciseNameSnapshot = target.exerciseName,
            notes = "Alternate BBB after ${mainExercise.exerciseName}; uses ${target.exerciseName}'s Training Max.",
            copyPreviousWorkout = false,
            sets = generated,
            trainingMaxValue = com.whip.app.domain.editableNumericValue(target.trainingMax),
            trainingMaxUnitId = target.unitId,
            cycleIncrementValue = com.whip.app.domain.editableNumericValue(target.cycleIncrement),
            trainingMaxSource = "Explicit",
            trainingMaxPercent = com.whip.app.domain.editableNumericValue(target.trainingMaxPercent),
            trainingMaxBasisKind = target.trainingMaxBasisKind.name,
            trainingMaxBasisValue = target.trainingMaxBasisValue
                ?.let { value -> com.whip.app.domain.editableNumericValue(value) }.orEmpty(),
            trainingMaxBasisUnitId = target.trainingMaxBasisUnitId,
            supplementalScheme = RoutineSupplementalScheme.BoringButBig.name,
            placementKind = RoutinePlacementKind.Supplemental.name,
        )
    }

    fun assistancePlacement(choice: FiveThreeOneAssistanceChoice): RoutineBuilderPlacementState {
        val setCount = if (request.layout == FiveThreeOneProgramLayout.Beginners) 5 else 3
        val total = setCount * 10
        val role = when (choice.category) {
            RoutineAssistanceCategory.Push -> RoutineAssistanceRole.Push
            RoutineAssistanceCategory.Pull -> RoutineAssistanceRole.Pull
            RoutineAssistanceCategory.SingleLegCore -> RoutineAssistanceRole.SingleLegCore
            else -> RoutineAssistanceRole.Other
        }
        return RoutineBuilderPlacementState(
            key = nextKey++,
            exerciseId = choice.exerciseId,
            exerciseNameSnapshot = choice.exerciseName,
            notes = "Suggested ${choice.category.fiveThreeOneUiLabel()} assistance · $total total reps. Review or replace for this program.",
            copyPreviousWorkout = false,
            sets = List(setCount) {
                RoutineBuilderSetState(
                    key = nextKey++,
                    repetitionsMin = "10",
                    classification = WorkoutSetClassification.Working.name,
                    note = "Assistance · ${choice.category.fiveThreeOneUiLabel()} · $total total reps",
                    workSection = RoutineWorkSection.Assistance.name,
                )
            },
            assistanceRole = role.name,
            placementKind = RoutinePlacementKind.Assistance.name,
            assistanceCategory = choice.category.name,
        )
    }

    val days = scheduledDays.mapIndexed { dayIndex, (name, scheduledExercises) ->
        val programmed = scheduledExercises.flatMap { exercise ->
            listOfNotNull(mainPlacement(exercise, dayIndex), alternateBbbPlacement(exercise))
        }
        RoutineBuilderDayState(
            key = nextKey++,
            name = name,
            placements = programmed + request.assistance.map(::assistancePlacement),
        )
    }
    val defaultName = when (request.plan) {
        FiveThreeOneProgramPlan.SingleCycle -> when (request.layout) {
            FiveThreeOneProgramLayout.FourDay -> "4-Day 5/3/1"
            FiveThreeOneProgramLayout.Beginners -> "5/3/1 for Beginners"
            FiveThreeOneProgramLayout.Custom -> "Custom 5/3/1"
        }
        FiveThreeOneProgramPlan.ForeverBbbLeaderAnchor -> "5/3/1 BBB Leader → FSL Anchor"
        FiveThreeOneProgramPlan.ForeverFslLeaderAnchor -> "5/3/1 FSL Leader → FSL Anchor"
    }
    val templateKey = when (request.plan) {
        FiveThreeOneProgramPlan.ForeverBbbLeaderAnchor -> RoutineProgramTemplateKey.FiveThreeOneForeverBbbLeaderAnchor
        FiveThreeOneProgramPlan.ForeverFslLeaderAnchor -> RoutineProgramTemplateKey.FiveThreeOneForeverFslLeaderAnchor
        FiveThreeOneProgramPlan.SingleCycle -> when (request.layout) {
            FiveThreeOneProgramLayout.FourDay -> RoutineProgramTemplateKey.FiveThreeOneFourDay
            FiveThreeOneProgramLayout.Beginners -> RoutineProgramTemplateKey.FiveThreeOneBeginners
            FiveThreeOneProgramLayout.Custom -> RoutineProgramTemplateKey.FiveThreeOneCustom
        }
    }
    val boundaries = when (request.plan) {
        FiveThreeOneProgramPlan.SingleCycle -> setOf(phases.lastIndex)
        FiveThreeOneProgramPlan.ForeverBbbLeaderAnchor,
        FiveThreeOneProgramPlan.ForeverFslLeaderAnchor,
        -> setOf(2, 6, 10)
    }
    return current.copy(
        name = current.name.ifBlank { defaultName },
        notes = current.notes.ifBlank {
            "Book-guided editable 5/3/1 structure. Review percentages against the edition and template you follow. Main, Supplemental, Assistance, and Optional work remain separate."
        },
        days = days,
        selectedDayKey = days.first().key,
        selectedPlacementKey = null,
        nextKey = nextKey,
        programKind = RoutineProgramKind.FiveThreeOne.name,
        programTemplateKey = templateKey.name,
        programTemplateRevision = FIVE_THREE_ONE_ONCE_PER_EXERCISE_PROTOCOL_REVISION,
        progressionMode = request.progressionMode.name,
        allowNonStandardHigherSuggestions = false,
        programPhaseCount = phases.size,
        programPhaseLabels = phases.map(FiveThreeOneGeneratedPhase::label),
        programPhaseRoles = phases.map { it.role.name },
        trainingMaxAdvanceAfterPhaseIndices = boundaries,
        currentProgramPhaseIndexHint = 0,
        nextProgramDayKeyHint = days.first().key,
    )
}
