package com.whip.app.ui

import com.whip.app.domain.TrainingMaxBasisKind
import java.io.Serializable

/** Authored values travel with their Exercise, never with a visual slot. */
internal data class FiveThreeOneExerciseSetupState(
    val exerciseId: Long,
    val cycleIncrement: String,
    val cycleIncrementAuthored: Boolean = false,
    val trainingMax: String = "",
    val useRecentMaxSuggestion: Boolean = false,
    val recentMax: String = "",
    val trainingMaxPercentage: String = "85",
    val trainingMaxBasisKind: String = TrainingMaxBasisKind.ActualOneRepMax.name,
    val appliedSourceMax: String = "",
    val appliedTrainingMaxPercentage: String = "",
    val appliedTrainingMaxBasisKind: String = "",
    val appliedDerivedTrainingMax: String = "",
    val bbbTargetId: Long = exerciseId,
    val supplement: FiveThreeOneSupplement? = null,
    val anchorSupplement: FiveThreeOneSupplement? = null,
    val boringButBigPercent: String = "50",
) : Serializable

internal fun FiveThreeOneExerciseSetupState.supplementFor(plan: FiveThreeOneProgramPlan): FiveThreeOneSupplement =
    supplement ?: when (plan) {
        FiveThreeOneProgramPlan.ForeverBbbLeaderAnchor -> FiveThreeOneSupplement.BoringButBig
        else -> FiveThreeOneSupplement.FirstSetLast
    }

internal fun FiveThreeOneExerciseSetupState.usesBoringButBig(plan: FiveThreeOneProgramPlan): Boolean =
    supplementFor(plan) == FiveThreeOneSupplement.BoringButBig ||
        (plan != FiveThreeOneProgramPlan.SingleCycle && anchorSupplement == FiveThreeOneSupplement.BoringButBig)

internal fun reconcileFiveThreeOneExerciseSetups(
    previous: List<FiveThreeOneExerciseSetupState>,
    suggestions: List<FiveThreeOneExerciseSetupState>,
): List<FiveThreeOneExerciseSetupState> {
    val byId = previous.filter { it.exerciseId > 0L }.associateBy { it.exerciseId }
    return suggestions.map { suggestion ->
        val existing = byId[suggestion.exerciseId] ?: return@map suggestion
        if (existing.cycleIncrementAuthored) existing
        else existing.copy(cycleIncrement = suggestion.cycleIncrement)
    }.withValidFiveThreeOneTargets()
}

internal fun List<FiveThreeOneExerciseSetupState>.withValidFiveThreeOneTargets(): List<FiveThreeOneExerciseSetupState> {
    val selectedIds = mapTo(mutableSetOf()) { it.exerciseId }
    return map { setup ->
        if (setup.bbbTargetId in selectedIds) setup else setup.copy(bbbTargetId = setup.exerciseId)
    }
}

internal fun customFiveThreeOneExerciseIds(
    previousIds: List<Long>,
    eligibleIds: List<Long>,
    firstCustomSelection: Boolean,
): List<Long> {
    val retained = previousIds.filter { it in eligibleIds }.distinct()
    val ordered = if (firstCustomSelection) eligibleIds.filter { it in retained } else retained
    return ordered.ifEmpty { eligibleIds.take(1) }
}
