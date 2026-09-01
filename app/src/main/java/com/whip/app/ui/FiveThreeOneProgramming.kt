package com.whip.app.ui

import com.whip.app.domain.RoutineLoadPrescriptionType
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
    Custom("Choose Your Lifts"),
}

internal enum class FiveThreeOneLiftRole(val label: String) {
    Squat("Squat"),
    Bench("Bench Press"),
    Deadlift("Deadlift"),
    Press("Overhead Press"),
}

internal fun RoutineProgramKind.isFiveThreeOneProgramKind(): Boolean = this in setOf(
    RoutineProgramKind.FiveThreeOne,
    RoutineProgramKind.FiveThreeOneClassic,
    RoutineProgramKind.FiveSPro,
    RoutineProgramKind.BoringButBig,
    RoutineProgramKind.FirstSetLast,
)

internal fun String?.isFiveThreeOneProgramKindName(): Boolean =
    runCatching { RoutineProgramKind.valueOf(this.orEmpty()) }.getOrNull()?.isFiveThreeOneProgramKind() == true

internal fun FiveThreeOneLiftRole.matchesExerciseName(name: String): Boolean {
    val normalized = name.lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
    return when (this) {
        FiveThreeOneLiftRole.Squat -> normalized in setOf(
            "squat", "back squat", "barbell squat", "barbell back squat", "low bar squat", "high bar squat",
        )
        FiveThreeOneLiftRole.Bench -> normalized in setOf(
            "bench", "bench press", "barbell bench", "barbell bench press", "flat bench press",
        )
        FiveThreeOneLiftRole.Deadlift -> normalized in setOf(
            "deadlift", "dead lift", "barbell deadlift", "conventional deadlift", "sumo deadlift",
        )
        FiveThreeOneLiftRole.Press -> normalized in setOf(
            "press", "overhead press", "barbell overhead press", "military press", "standing press", "shoulder press",
        )
    }
}

internal fun defaultFiveThreeOneCycleIncrease(
    unitId: String,
    exerciseName: String,
    role: FiveThreeOneLiftRole? = null,
): Double {
    val lowerLift = role in setOf(FiveThreeOneLiftRole.Squat, FiveThreeOneLiftRole.Deadlift) ||
        (role == null && listOf("squat", "deadlift", "dead lift").any(exerciseName.lowercase()::contains))
    return if (unitId == "pound") {
        if (lowerLift) 10.0 else 5.0
    } else {
        if (lowerLift) 5.0 else 2.5
    }
}

internal data class FiveThreeOneProgramLift(
    /** Standard-role identity is required by the two Wendler presets and absent for custom lifts. */
    val role: FiveThreeOneLiftRole?,
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
    val amrap: Boolean = false,
    val phase: FiveThreeOnePhase? = null,
    val optionalWorkKind: RoutineOptionalWorkKind = RoutineOptionalWorkKind.None,
) {
    val classification: WorkoutSetClassification
        get() = when {
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
        get() = if (amrap) "$repetitions+ · AMRAP" else repetitions.toString()
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
    val joker = if (config.jokerSetsEnabled && config.phase != FiveThreeOnePhase.Deload) {
        listOf(
            FiveThreeOneSetPlan(
                section = FiveThreeOneSetSection.Optional,
                percentageOfTrainingMax = mainPercentages.last() + 5.0,
                repetitions = classicRepetitions.last(),
                phase = config.phase,
                optionalWorkKind = RoutineOptionalWorkKind.Joker,
            ),
        )
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
): List<RoutineBuilderSetState> {
    var nextKey = (existingSets.maxOfOrNull(RoutineBuilderSetState::key) ?: 0L) + 1L
    return previews.mapIndexed { index, preview ->
        val existing = existingSets.getOrNull(index)
        RoutineBuilderSetState(
            key = existing?.key ?: nextKey++,
            load = "",
            repetitionsMin = preview.plan.repetitions.toString(),
            repetitionsMax = "",
            classification = preview.plan.classification.name,
            restSeconds = existing?.restSeconds.orEmpty(),
            note = buildString {
                append(preview.plan.section.label)
                preview.plan.phase?.let { append(" · ${it.label}") }
                append(" · ")
                append(com.whip.app.domain.editableNumericValue(preview.plan.percentageOfTrainingMax))
                append("% TM")
                if (preview.plan.amrap) append(" · PR set, minimum ${preview.plan.repetitions} reps; stop before technical failure")
            },
            loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax.name,
            loadPercentage = com.whip.app.domain.editableNumericValue(preview.plan.percentageOfTrainingMax),
            routinePhaseIndex = preview.plan.phase?.ordinal,
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

/** Builds an intentional whole program from either a standard layout or the lifter's chosen lifts. */
internal fun buildFiveThreeOneProgramState(
    current: RoutineBuilderState,
    layout: FiveThreeOneProgramLayout,
    lifts: List<FiveThreeOneProgramLift>,
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
            require(lifts.size == FiveThreeOneLiftRole.entries.size)
            require(lifts.mapNotNull(FiveThreeOneProgramLift::role).toSet() == FiveThreeOneLiftRole.entries.toSet())
        }
        FiveThreeOneProgramLayout.Custom -> {
            require(lifts.isNotEmpty())
            require(lifts.all { it.role == null })
        }
    }
    require(lifts.map(FiveThreeOneProgramLift::exerciseId).distinct().size == lifts.size)
    require(lifts.all {
        it.exerciseId > 0L && it.trainingMax.isFinite() && it.trainingMax > 0.0 &&
            it.loadIncrement.isFinite() && it.loadIncrement > 0.0 &&
            it.cycleIncrement.isFinite() && it.cycleIncrement > 0.0
    })
    require(boringButBigPercent.isFinite() && boringButBigPercent in 1.0..100.0)
    val liftByRole = lifts.mapNotNull { lift -> lift.role?.let { role -> role to lift } }.toMap()
    val schedule = when (layout) {
        FiveThreeOneProgramLayout.FourDay -> listOf(
            "Squat" to listOf(requireNotNull(liftByRole[FiveThreeOneLiftRole.Squat])),
            "Bench" to listOf(requireNotNull(liftByRole[FiveThreeOneLiftRole.Bench])),
            "Deadlift" to listOf(requireNotNull(liftByRole[FiveThreeOneLiftRole.Deadlift])),
            "Press" to listOf(requireNotNull(liftByRole[FiveThreeOneLiftRole.Press])),
        )
        FiveThreeOneProgramLayout.Beginners -> listOf(
            "Monday · Squat + Bench" to listOf(
                requireNotNull(liftByRole[FiveThreeOneLiftRole.Squat]),
                requireNotNull(liftByRole[FiveThreeOneLiftRole.Bench]),
            ),
            "Wednesday · Deadlift + Press" to listOf(
                requireNotNull(liftByRole[FiveThreeOneLiftRole.Deadlift]),
                requireNotNull(liftByRole[FiveThreeOneLiftRole.Press]),
            ),
            "Friday · Bench + Squat" to listOf(
                requireNotNull(liftByRole[FiveThreeOneLiftRole.Bench]),
                requireNotNull(liftByRole[FiveThreeOneLiftRole.Squat]),
            ),
        )
        FiveThreeOneProgramLayout.Custom -> lifts.map { lift -> lift.exerciseName to listOf(lift) }
    }
    var nextKey = current.nextKey.coerceAtLeast(1L)
    fun placement(lift: FiveThreeOneProgramLift): RoutineBuilderPlacementState {
        val config = FiveThreeOneAuthoringConfig(
            trainingMax = lift.trainingMax,
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
            previewFiveThreeOneCycle(config, lift.loadIncrement),
            mainWorkScheme = fiveThreeOneMainWorkScheme(config),
            supplementalScheme = fiveThreeOneSupplementalScheme(config),
        )
            .map { it.copy(key = nextKey++) }
        return RoutineBuilderPlacementState(
            key = placementKey,
            exerciseId = lift.exerciseId,
            exerciseNameSnapshot = lift.exerciseName,
            copyPreviousWorkout = false,
            sets = generated,
            trainingMaxValue = com.whip.app.domain.editableNumericValue(lift.trainingMax),
            trainingMaxUnitId = lift.unitId,
            cycleIncrementValue = com.whip.app.domain.editableNumericValue(lift.cycleIncrement),
            trainingMaxSource = "Explicit",
            trainingMaxPercent = com.whip.app.domain.editableNumericValue(lift.trainingMaxPercent),
            trainingMaxBasisKind = lift.trainingMaxBasisKind.name,
            trainingMaxBasisValue = lift.trainingMaxBasisValue
                ?.let { value -> com.whip.app.domain.editableNumericValue(value) }.orEmpty(),
            trainingMaxBasisUnitId = lift.trainingMaxBasisUnitId,
            mainWorkScheme = fiveThreeOneMainWorkScheme(config).name,
            supplementalScheme = fiveThreeOneSupplementalScheme(config).name,
            assistanceRole = RoutineAssistanceRole.MainLift.name,
            placementKind = RoutinePlacementKind.MainLift.name,
            assistanceCategory = RoutineAssistanceCategory.Unspecified.name,
            jokerSetsEnabled = jokerSetsEnabled,
        )
    }
    val days = schedule.map { (name, scheduledLifts) ->
        RoutineBuilderDayState(key = nextKey++, name = name, placements = scheduledLifts.map(::placement))
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
