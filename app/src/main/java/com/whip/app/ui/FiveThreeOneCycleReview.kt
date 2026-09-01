package com.whip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whip.app.domain.FiveThreeOneEvidenceKind
import com.whip.app.domain.FiveThreeOneEvidenceRow
import com.whip.app.domain.FiveThreeOneProgression
import com.whip.app.domain.FiveThreeOneProgressionCategory
import com.whip.app.domain.FiveThreeOneProgressionRecommendation
import com.whip.app.domain.RoutinePlacementKind
import com.whip.app.domain.RoutineProgramKind
import com.whip.app.domain.RoutineProgramPhaseRole
import com.whip.app.domain.RoutineProgressionMode
import com.whip.app.domain.RoutineWorkSection
import com.whip.app.domain.TrainingMaxCycleDecision
import com.whip.app.domain.TrainingMaxDecisionAction
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.domain.WorkoutSetClassification
import com.whip.app.domain.editableNumericValue
import com.whip.app.domain.massFromKilograms
import com.whip.app.domain.toWhipDoubleOrNull
import com.whip.app.domain.unitSymbol

internal data class FiveThreeOneLiftCycleReview(
    val exerciseId: Long,
    val exerciseName: String,
    val currentTrainingMax: Double,
    val unitId: String,
    val standardDelta: Double,
    val eligible: Boolean,
    val recommendation: FiveThreeOneProgressionRecommendation,
)

internal data class FiveThreeOneCycleReview(
    val routineName: String,
    val cycle: Int,
    val lifts: List<FiveThreeOneLiftCycleReview>,
)

/**
 * Builds a review only for an in-sequence, configured boundary in performance-review mode.
 * Every calculation uses immutable workout prescriptions and performed values from one cycle.
 */
internal fun GymUiState.activeFiveThreeOneCycleReview(): FiveThreeOneCycleReview? {
    val session = activeSession ?: return null
    if (session.programProgressAdvanced || !session.sourceRoutineProgramKind.isFiveThreeOneProgramKind()) return null
    val routineId = session.sourceRoutineId ?: return null
    val routine = (routines + archivedRoutines).firstOrNull { it.id == routineId } ?: return null
    if (routine.programKind != session.sourceRoutineProgramKind ||
        !routine.programKind.isFiveThreeOneProgramKind()
    ) return null
    if (routine.progressionMode != RoutineProgressionMode.PerformanceInformed) return null
    val phase = session.sourceRoutinePhaseIndex ?: return null
    val cycle = session.sourceRoutineCycle ?: return null
    val dayPosition = session.sourceRoutineDayPosition ?: return null
    val days = routineDays.filter { it.routineId == routineId }.sortedBy { it.position }
    if (dayPosition != days.lastIndex || phase !in routine.trainingMaxAdvanceAfterPhaseIndices) return null

    val dayIds = days.mapTo(mutableSetOf()) { it.id }
    val placements = routineExercises.filter { placement ->
        placement.routineDayId in dayIds &&
            (placement.placementKind == RoutinePlacementKind.MainLift || routineSets.any { set ->
                set.routineExerciseId == placement.id && set.draft.workSection == RoutineWorkSection.Main
            })
    }.groupBy { it.exerciseId }
    if (placements.isEmpty()) return null
    val sourceSessions = allSessions.filter { candidate ->
        candidate.sourceRoutineId == routineId && candidate.sourceRoutineCycle == cycle &&
            candidate.sourceRoutineProgramKind == routine.programKind &&
            !candidate.archived &&
            (candidate.id == session.id && candidate.state == WorkoutSessionState.Active ||
                candidate.state == WorkoutSessionState.Finished && candidate.programProgressAdvanced)
    }.associateBy { it.id }
    val workoutPlacementsByExercise = allWorkoutExercises.filter { it.sessionId in sourceSessions }
        .groupBy { it.exerciseId }
    val exerciseById = (exercises + archivedExercises).associateBy { it.id }

    val lifts = placements.mapNotNull { (exerciseId, repeatedPlacements) ->
        val representative = repeatedPlacements.first()
        val currentTm = representative.trainingMaxValue ?: return@mapNotNull null
        val standard = representative.cycleIncrementValue ?: return@mapNotNull null
        if (!currentTm.isFinite() || currentTm <= 0.0 || !standard.isFinite() || standard <= 0.0) {
            return@mapNotNull null
        }
        val evidence = buildList {
            workoutPlacementsByExercise[exerciseId].orEmpty().forEach { workoutExercise ->
                val evidenceSession = sourceSessions[workoutExercise.sessionId] ?: return@forEach
                if (evidenceSession.sourceRoutinePhaseRole == RoutineProgramPhaseRole.Deload) return@forEach
                val snapshotTm = workoutExercise.trainingMaxValueSnapshot
                if (snapshotTm == null || workoutExercise.trainingMaxUnitIdSnapshot != representative.trainingMaxUnitId ||
                    kotlin.math.abs(snapshotTm - currentTm) > 1e-9
                ) return@forEach
                allSets.filter { it.workoutExerciseId == workoutExercise.id }.forEach { set ->
                    val kind = when {
                        set.workSectionSnapshot == RoutineWorkSection.Optional &&
                            set.optionalWorkKindSnapshot.name == "Joker" -> FiveThreeOneEvidenceKind.Joker
                        set.workSectionSnapshot != RoutineWorkSection.Main -> return@forEach
                        evidenceSession.sourceRoutinePhaseRole == RoutineProgramPhaseRole.TrainingMaxTest &&
                            set.prescribedClassificationSnapshot == WorkoutSetClassification.TrainingMaxTest ->
                            FiveThreeOneEvidenceKind.TrainingMaxTest
                        set.prescribedClassificationSnapshot == WorkoutSetClassification.Amrap ->
                            FiveThreeOneEvidenceKind.PrSet
                        else -> FiveThreeOneEvidenceKind.RequiredMain
                    }
                    add(
                        FiveThreeOneEvidenceRow(
                            kind = kind,
                            exposureId = evidenceSession.uuid,
                            trainingMaxAtExposure = snapshotTm,
                            completed = set.completed,
                            deleted = set.deletedAtMillis != null,
                            failure = set.classification == WorkoutSetClassification.Failure,
                            prescribedReps = set.prescribedRepetitions,
                            actualReps = set.repetitions,
                            prescribedLoad = set.prescribedCanonicalWeightKg?.let { kg ->
                                massFromKilograms(kg, representative.trainingMaxUnitId)
                            },
                            actualLoad = set.canonicalWeightKg?.let { kg ->
                                massFromKilograms(kg, representative.trainingMaxUnitId)
                            },
                            rpe = set.rpe,
                            rir = set.rir,
                        ),
                    )
                }
            }
            if (session.requiredMainWorkInvalidated &&
                (session.invalidatedMainExerciseIds.isEmpty() || exerciseId in session.invalidatedMainExerciseIds)
            ) {
                add(
                    FiveThreeOneEvidenceRow(
                        kind = FiveThreeOneEvidenceKind.RequiredMain,
                        exposureId = session.uuid,
                        trainingMaxAtExposure = currentTm,
                        completed = true,
                        failure = true,
                        prescribedReps = 1,
                        actualReps = 0,
                        prescribedLoad = currentTm,
                        actualLoad = 0.0,
                    ),
                )
            }
        }
        val recommendation = FiveThreeOneProgression.recommend(
            evidence = evidence,
            currentTrainingMax = currentTm,
            standardIncrement = standard,
            allowNonStandardHigher = routine.allowNonStandardHigherSuggestions,
        )
        val activeLiftWork = activeWorkoutExercises.filter { it.exercise.id == exerciseId }
        val currentRequiredMainPassed = if (activeLiftWork.isEmpty()) {
            // Other lifts were performed on earlier days in this cycle. Their persisted per-lift
            // eligibility and immutable evidence remain authoritative at the final-day review.
            true
        } else {
            activeLiftWork.flatMap { it.sets }
                .filter { it.workSectionSnapshot == RoutineWorkSection.Main }
                .let { sets -> sets.isNotEmpty() && sets.all { set ->
                    set.deletedAtMillis == null && set.completed &&
                        set.classification != WorkoutSetClassification.Failure &&
                        (set.prescribedRepetitions == null ||
                            (set.repetitions ?: Int.MIN_VALUE) >= set.prescribedRepetitions) &&
                        (set.prescribedCanonicalWeightKg == null ||
                            (set.canonicalWeightKg ?: Double.NEGATIVE_INFINITY) + 1e-9 >= set.prescribedCanonicalWeightKg)
                } }
        }
        FiveThreeOneLiftCycleReview(
            exerciseId = exerciseId,
            exerciseName = exerciseById[exerciseId]?.name ?: "Exercise $exerciseId",
            currentTrainingMax = currentTm,
            unitId = representative.trainingMaxUnitId,
            standardDelta = standard,
            eligible = repeatedPlacements.all { it.trainingMaxIncreaseEligible } &&
                currentRequiredMainPassed &&
                (!session.requiredMainWorkInvalidated ||
                    (session.invalidatedMainExerciseIds.isNotEmpty() &&
                        exerciseId !in session.invalidatedMainExerciseIds)),
            recommendation = recommendation,
        )
    }.sortedBy { lift -> lift.exerciseName.lowercase() }
    return FiveThreeOneCycleReview(routine.name, cycle, lifts).takeIf { lifts.isNotEmpty() }
}

private enum class CycleReviewChoice { Suggestion, Standard, Hold, Ignore, Custom }

@Composable
internal fun FiveThreeOneCycleReviewDialog(
    review: FiveThreeOneCycleReview,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onApply: (List<TrainingMaxCycleDecision>) -> Unit,
) {
    var choices by rememberSaveable(review.cycle, review.lifts.map { it.exerciseId }) {
        mutableStateOf(review.lifts.associate { lift ->
            lift.exerciseId to if (lift.eligible) CycleReviewChoice.Standard.name else CycleReviewChoice.Hold.name
        })
    }
    var customValues by rememberSaveable(review.cycle, review.lifts.map { it.exerciseId }) {
        mutableStateOf(review.lifts.associate { it.exerciseId to "0" })
    }
    val decisions = remember(review, choices, customValues) {
        review.lifts.mapNotNull { lift ->
            val choice = runCatching { CycleReviewChoice.valueOf(choices[lift.exerciseId].orEmpty()) }
                .getOrDefault(CycleReviewChoice.Hold)
            val delta = when (choice) {
                CycleReviewChoice.Suggestion -> lift.recommendation.suggestedDelta
                CycleReviewChoice.Standard -> lift.standardDelta
                CycleReviewChoice.Hold -> 0.0
                CycleReviewChoice.Ignore -> 0.0
                CycleReviewChoice.Custom -> customValues[lift.exerciseId]?.toWhipDoubleOrNull() ?: return@mapNotNull null
            }
            val maximumDelta = if (lift.recommendation.category == FiveThreeOneProgressionCategory.CautiousHigherIncrease) {
                lift.standardDelta * 2.0
            } else {
                lift.standardDelta
            }
            if (!delta.isFinite() || delta < -lift.standardDelta || delta > maximumDelta ||
                (!lift.eligible && delta > 0.0) || lift.currentTrainingMax + delta <= 0.0
            ) {
                return@mapNotNull null
            }
            TrainingMaxCycleDecision(
                exerciseId = lift.exerciseId,
                expectedCurrentTrainingMax = lift.currentTrainingMax,
                requestedDelta = delta,
                standardDelta = lift.standardDelta,
                recommendationCategory = lift.recommendation.category.name,
                recommendationDelta = lift.recommendation.suggestedDelta,
                confidence = lift.recommendation.confidence,
                reasons = lift.recommendation.reasons,
                engineVersion = lift.recommendation.engineVersion,
                action = when (choice) {
                    CycleReviewChoice.Suggestion -> TrainingMaxDecisionAction.UseSuggestion
                    CycleReviewChoice.Standard -> TrainingMaxDecisionAction.UseStandard
                    CycleReviewChoice.Hold -> TrainingMaxDecisionAction.Hold
                    CycleReviewChoice.Ignore -> TrainingMaxDecisionAction.IgnoreRecommendation
                    CycleReviewChoice.Custom -> TrainingMaxDecisionAction.Custom
                },
            )
        }
    }
    PaneAwareAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review Cycle ${review.cycle} Training Maxes") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Nothing changes until you apply these per-lift decisions. 5/3/1 standard uses the saved increase; Whip suggestions are advisory.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                review.lifts.forEach { lift ->
                    val recommendation = lift.recommendation
                    val maximumDelta = if (recommendation.category == FiveThreeOneProgressionCategory.CautiousHigherIncrease) {
                        lift.standardDelta * 2.0
                    } else {
                        lift.standardDelta
                    }
                    val suggestionAllowed = recommendation.category != FiveThreeOneProgressionCategory.InsufficientEvidence &&
                        recommendation.suggestedDelta in -lift.standardDelta..maximumDelta &&
                        (lift.eligible || recommendation.suggestedDelta <= 0.0) &&
                        lift.currentTrainingMax + recommendation.suggestedDelta > 0.0
                    Surface(
                        modifier = Modifier.fillMaxWidth().testTag("training-max-review-${lift.exerciseId}"),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Text(lift.exerciseName, fontWeight = FontWeight.Bold)
                            Text(
                                "Current TM ${editableNumericValue(lift.currentTrainingMax)} ${unitSymbol(lift.unitId)} · " +
                                    "5/3/1 standard +${editableNumericValue(lift.standardDelta)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                when {
                                    lift.eligible -> recommendation.label
                                    recommendation.category == FiveThreeOneProgressionCategory.DecreaseReview -> recommendation.label
                                    else -> "Hold — required Main work was not completed"
                                },
                                fontWeight = FontWeight.SemiBold,
                                color = if (lift.eligible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                            if (recommendation.category == FiveThreeOneProgressionCategory.CautiousHigherIncrease) {
                                Text(
                                    "Whip suggestion · non-standard 5/3/1 option",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            Text(
                                "Evidence strength: ${fiveThreeOneEvidenceStrength(recommendation.confidence)} · " +
                                    recommendation.reasons.joinToString(" "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                WhipFilterChip(
                                    selected = choices[lift.exerciseId] == CycleReviewChoice.Standard.name,
                                    onClick = {
                                        choices = choices + (lift.exerciseId to CycleReviewChoice.Standard.name)
                                    },
                                    enabled = lift.eligible,
                                    label = { Text("Standard +${editableNumericValue(lift.standardDelta)}") },
                                    modifier = Modifier.testTag("training-max-choice-standard-${lift.exerciseId}"),
                                )
                                WhipFilterChip(
                                    selected = choices[lift.exerciseId] == CycleReviewChoice.Suggestion.name,
                                    onClick = {
                                        choices = choices + (lift.exerciseId to CycleReviewChoice.Suggestion.name)
                                    },
                                    enabled = suggestionAllowed,
                                    label = { Text("Suggestion ${deltaLabel(recommendation.suggestedDelta)}") },
                                    modifier = Modifier.testTag("training-max-choice-suggestion-${lift.exerciseId}"),
                                )
                                WhipFilterChip(
                                    selected = choices[lift.exerciseId] == CycleReviewChoice.Hold.name,
                                    onClick = { choices = choices + (lift.exerciseId to CycleReviewChoice.Hold.name) },
                                    label = { Text("Hold") },
                                    modifier = Modifier.testTag("training-max-choice-hold-${lift.exerciseId}"),
                                )
                                WhipFilterChip(
                                    selected = choices[lift.exerciseId] == CycleReviewChoice.Ignore.name,
                                    onClick = { choices = choices + (lift.exerciseId to CycleReviewChoice.Ignore.name) },
                                    label = { Text("Ignore recommendation") },
                                    modifier = Modifier.testTag("training-max-choice-ignore-${lift.exerciseId}"),
                                )
                                WhipFilterChip(
                                    selected = choices[lift.exerciseId] == CycleReviewChoice.Custom.name,
                                    onClick = { choices = choices + (lift.exerciseId to CycleReviewChoice.Custom.name) },
                                    label = { Text("Custom") },
                                    modifier = Modifier.testTag("training-max-choice-custom-${lift.exerciseId}"),
                                )
                            }
                            if (choices[lift.exerciseId] == CycleReviewChoice.Custom.name) {
                                OutlinedTextField(
                                    value = customValues[lift.exerciseId].orEmpty(),
                                    onValueChange = { value ->
                                        customValues = customValues + (lift.exerciseId to value.cycleDeltaInput())
                                    },
                                    label = { Text("Custom cycle change (${unitSymbol(lift.unitId)})") },
                                    supportingText = {
                                        Text(
                                            if (lift.eligible) {
                                                "-${editableNumericValue(lift.standardDelta)} to ${editableNumericValue(maximumDelta)}; zero keeps the current TM"
                                            } else {
                                                "-${editableNumericValue(lift.standardDelta)} to 0; positive changes require completed Main work"
                                            },
                                        )
                                    },
                                    isError = customValues[lift.exerciseId]?.toWhipDoubleOrNull()
                                        ?.let { value ->
                                            value !in -lift.standardDelta..maximumDelta ||
                                                (!lift.eligible && value > 0.0) || lift.currentTrainingMax + value <= 0.0
                                        } != false,
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("training-max-custom-delta-${lift.exerciseId}"),
                                )
                            }
                            if (choices[lift.exerciseId] == CycleReviewChoice.Ignore.name) {
                                Text(
                                    "Decline Whip's advisory recommendation for this cycle and keep the current Training Max. This is recorded separately from a programming Hold.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            val selectedDelta = decisions.firstOrNull { it.exerciseId == lift.exerciseId }?.requestedDelta
                            if (selectedDelta != null) {
                                Text(
                                    "Next TM ${editableNumericValue(lift.currentTrainingMax + selectedDelta)} ${unitSymbol(lift.unitId)}",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            WhipButton(
                enabled = decisions.size == review.lifts.size,
                onClick = { onApply(decisions) },
                modifier = Modifier.testTag("apply-training-max-decisions"),
            ) { Text("Apply Decisions & Finish") }
        },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Keep Training") } },
        modifier = modifier,
    )
}

private fun deltaLabel(delta: Double): String = when {
    delta > 0.0 -> "+${editableNumericValue(delta)}"
    delta == 0.0 -> "Hold"
    else -> editableNumericValue(delta)
}

/** The engine score ranks deterministic rule evidence; it is not a calibrated probability. */
internal fun fiveThreeOneEvidenceStrength(score: Double): String = when {
    score >= 0.85 -> "strong"
    score >= 0.60 -> "moderate"
    else -> "limited"
}

private fun String.cycleDeltaInput(): String = filterIndexed { index, character ->
    character.isDigit() || character == '.' || character == ',' || (character == '-' && index == 0)
}.take(12)
