package com.whip.app.ui

import com.whip.app.domain.RoutineLoadPrescriptionType
import com.whip.app.domain.RoutineProgramKind
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
}

internal enum class FiveThreeOneSetSection(val label: String) {
    Main("Main Work"),
    Supplemental("Supplemental"),
}

internal data class FiveThreeOneSetPlan(
    val section: FiveThreeOneSetSection,
    val percentageOfTrainingMax: Double,
    val repetitions: Int,
    val amrap: Boolean = false,
    val phase: FiveThreeOnePhase? = null,
) {
    val classification: WorkoutSetClassification
        get() = when {
            amrap -> WorkoutSetClassification.Amrap
            section == FiveThreeOneSetSection.Supplemental -> WorkoutSetClassification.BackOff
            else -> WorkoutSetClassification.Working
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
)

internal data class FiveThreeOneBuilderResult(
    val sets: List<RoutineBuilderSetState>,
    val trainingMax: Double,
    val trainingMaxUnitId: String,
    val cycleIncrementValue: Double,
    val programKind: RoutineProgramKind,
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
    val supplementalPercentage = when (config.supplement) {
        FiveThreeOneSupplement.None -> return main
        FiveThreeOneSupplement.BoringButBig -> config.boringButBigPercent
        FiveThreeOneSupplement.FirstSetLast -> mainPercentages.first()
    }
    val supplementalRepetitions = when (config.supplement) {
        FiveThreeOneSupplement.BoringButBig -> 10
        FiveThreeOneSupplement.FirstSetLast -> 5
        FiveThreeOneSupplement.None -> error("Handled above")
    }
    return main + List(5) {
        FiveThreeOneSetPlan(
            section = FiveThreeOneSetSection.Supplemental,
            percentageOfTrainingMax = supplementalPercentage,
            repetitions = supplementalRepetitions,
            // BBB is unchanged across the cycle, while FSL follows each phase's first set.
            phase = config.phase.takeIf { config.supplement == FiveThreeOneSupplement.FirstSetLast },
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
    FiveThreeOneSupplement.BoringButBig -> RoutineProgramKind.BoringButBig
    FiveThreeOneSupplement.FirstSetLast -> RoutineProgramKind.FirstSetLast
    FiveThreeOneSupplement.None -> when (config.mainScheme) {
        FiveThreeOneMainScheme.Classic -> RoutineProgramKind.FiveThreeOneClassic
        FiveThreeOneMainScheme.FivesPro -> RoutineProgramKind.FiveSPro
    }
}

internal fun suggestedFiveThreeOneTrainingMax(
    estimatedOneRepMax: Double,
    increment: Double,
    percentage: Double = 90.0,
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
                if (preview.plan.amrap) append(" · AMRAP, minimum ${preview.plan.repetitions} reps")
            },
            loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax.name,
            loadPercentage = com.whip.app.domain.editableNumericValue(preview.plan.percentageOfTrainingMax),
            routinePhaseIndex = preview.plan.phase?.ordinal,
        )
    }
}
