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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
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

internal data class FiveThreeOneExerciseCycleReview(
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
    val exercises: List<FiveThreeOneExerciseCycleReview>,
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
            (placement.placementKind == RoutinePlacementKind.MainExercise || routineSets.any { set ->
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

    val exercises = placements.mapNotNull { (exerciseId, repeatedPlacements) ->
        val representative = repeatedPlacements.first()
        val currentTm = representative.trainingMaxValue ?: return@mapNotNull null
        val standard = representative.cycleIncrementValue ?: return@mapNotNull null
        if (!currentTm.isFinite() || currentTm <= 0.0 || !standard.isFinite() || standard <= 0.0) {
            return@mapNotNull null
        }
        val evidence = buildList {
            workoutPlacementsByExercise[exerciseId].orEmpty().forEach { workoutExercise ->
                val evidenceSession = sourceSessions[workoutExercise.sessionId] ?: return@forEach
                if (evidenceSession.sourceRoutinePhaseRole.semanticRole() == RoutineProgramPhaseRole.Deload) return@forEach
                val snapshotTm = workoutExercise.trainingMaxValueSnapshot
                if (snapshotTm == null || workoutExercise.trainingMaxUnitIdSnapshot != representative.trainingMaxUnitId ||
                    kotlin.math.abs(snapshotTm - currentTm) > 1e-9
                ) return@forEach
                allSets.filter { it.workoutExerciseId == workoutExercise.id }.forEach { set ->
                    val kind = when {
                        set.workSectionSnapshot == RoutineWorkSection.Optional &&
                            set.optionalWorkKindSnapshot.name == "Joker" -> FiveThreeOneEvidenceKind.Joker
                        set.workSectionSnapshot != RoutineWorkSection.Main -> return@forEach
                        evidenceSession.sourceRoutinePhaseRole.semanticRole() == RoutineProgramPhaseRole.TrainingMaxTest &&
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
        val activeExerciseWork = activeWorkoutPerformanceExercises.filter { it.exercise.id == exerciseId }
        val currentRequiredMainPassed = if (activeExerciseWork.isEmpty()) {
            // Other exercises were performed on earlier days in this cycle. Their persisted per-exercise
            // eligibility and immutable evidence remain authoritative at the final-day review.
            true
        } else {
            activeExerciseWork.flatMap { it.sets }
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
        FiveThreeOneExerciseCycleReview(
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
    }.sortedBy { exercise -> exercise.exerciseName.lowercase() }
    return FiveThreeOneCycleReview(routine.name, cycle, exercises).takeIf { exercises.isNotEmpty() }
}

private enum class CycleReviewChoice { Suggestion, Standard, Hold, Ignore, Custom }

@Composable
internal fun FiveThreeOneCycleReviewDialog(
    review: FiveThreeOneCycleReview,
    modifier: Modifier = Modifier,
    saving: Boolean = false,
    errorMessage: String? = null,
    reviewRevision: Long? = null,
    onDismiss: () -> Unit,
    onApply: (List<TrainingMaxCycleDecision>) -> Unit,
) {
    val reviewIdentity = review.exercises.map { exercise ->
        listOf(
            exercise.exerciseId,
            exercise.currentTrainingMax,
            exercise.eligible,
            exercise.recommendation.category,
            exercise.recommendation.suggestedDelta,
            exercise.recommendation.confidence,
        )
    }
    var choices by rememberSaveable(review.cycle, reviewRevision, reviewIdentity) {
        mutableStateOf(review.exercises.associate { exercise ->
            exercise.exerciseId to if (exercise.eligible) CycleReviewChoice.Standard.name else CycleReviewChoice.Hold.name
        })
    }
    var customValues by rememberSaveable(review.cycle, reviewRevision, reviewIdentity) {
        mutableStateOf(review.exercises.associate { it.exerciseId to "0" })
    }
    val decisions = remember(review, choices, customValues) {
        review.exercises.mapNotNull { exercise ->
            val choice = runCatching { CycleReviewChoice.valueOf(choices[exercise.exerciseId].orEmpty()) }
                .getOrDefault(CycleReviewChoice.Hold)
            val delta = when (choice) {
                CycleReviewChoice.Suggestion -> exercise.recommendation.suggestedDelta
                CycleReviewChoice.Standard -> exercise.standardDelta
                CycleReviewChoice.Hold -> 0.0
                CycleReviewChoice.Ignore -> 0.0
                CycleReviewChoice.Custom -> customValues[exercise.exerciseId]?.toWhipDoubleOrNull() ?: return@mapNotNull null
            }
            val maximumDelta = if (exercise.recommendation.category == FiveThreeOneProgressionCategory.CautiousHigherIncrease) {
                exercise.standardDelta * 2.0
            } else {
                exercise.standardDelta
            }
            if (!delta.isFinite() || delta < -exercise.standardDelta || delta > maximumDelta ||
                (!exercise.eligible && delta > 0.0) || exercise.currentTrainingMax + delta <= 0.0
            ) {
                return@mapNotNull null
            }
            TrainingMaxCycleDecision(
                exerciseId = exercise.exerciseId,
                expectedCurrentTrainingMax = exercise.currentTrainingMax,
                requestedDelta = delta,
                standardDelta = exercise.standardDelta,
                recommendationCategory = exercise.recommendation.category.name,
                recommendationDelta = exercise.recommendation.suggestedDelta,
                confidence = exercise.recommendation.confidence,
                reasons = exercise.recommendation.reasons,
                engineVersion = exercise.recommendation.engineVersion,
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
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Review Cycle ${review.cycle} Training Maxes") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Nothing changes until you apply these per-exercise decisions. 5/3/1 standard uses the saved increase; Whip suggestions are advisory.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                errorMessage?.let { message ->
                    Text(
                        message,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                review.exercises.forEach { exercise ->
                    val recommendation = exercise.recommendation
                    val maximumDelta = if (recommendation.category == FiveThreeOneProgressionCategory.CautiousHigherIncrease) {
                        exercise.standardDelta * 2.0
                    } else {
                        exercise.standardDelta
                    }
                    val suggestionAllowed = recommendation.category != FiveThreeOneProgressionCategory.InsufficientEvidence &&
                        recommendation.suggestedDelta in -exercise.standardDelta..maximumDelta &&
                        (exercise.eligible || recommendation.suggestedDelta <= 0.0) &&
                        exercise.currentTrainingMax + recommendation.suggestedDelta > 0.0
                    Surface(
                        modifier = Modifier.fillMaxWidth().testTag("training-max-review-${exercise.exerciseId}"),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Text(exercise.exerciseName, fontWeight = FontWeight.Bold)
                            Text(
                                "Current TM ${editableNumericValue(exercise.currentTrainingMax)} ${unitSymbol(exercise.unitId)} · " +
                                    "5/3/1 standard +${editableNumericValue(exercise.standardDelta)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                when {
                                    exercise.eligible -> recommendation.label
                                    recommendation.category == FiveThreeOneProgressionCategory.DecreaseReview -> recommendation.label
                                    else -> "Hold — required Main work was not completed"
                                },
                                fontWeight = FontWeight.SemiBold,
                                color = if (exercise.eligible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
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
                                    selected = choices[exercise.exerciseId] == CycleReviewChoice.Standard.name,
                                    onClick = {
                                        choices = choices + (exercise.exerciseId to CycleReviewChoice.Standard.name)
                                    },
                                    enabled = exercise.eligible,
                                    label = { Text("Standard +${editableNumericValue(exercise.standardDelta)}") },
                                    modifier = Modifier.testTag("training-max-choice-standard-${exercise.exerciseId}"),
                                )
                                WhipFilterChip(
                                    selected = choices[exercise.exerciseId] == CycleReviewChoice.Suggestion.name,
                                    onClick = {
                                        choices = choices + (exercise.exerciseId to CycleReviewChoice.Suggestion.name)
                                    },
                                    enabled = suggestionAllowed,
                                    label = { Text("Suggestion ${deltaLabel(recommendation.suggestedDelta)}") },
                                    modifier = Modifier.testTag("training-max-choice-suggestion-${exercise.exerciseId}"),
                                )
                                WhipFilterChip(
                                    selected = choices[exercise.exerciseId] == CycleReviewChoice.Hold.name,
                                    onClick = { choices = choices + (exercise.exerciseId to CycleReviewChoice.Hold.name) },
                                    label = { Text("Hold") },
                                    modifier = Modifier.testTag("training-max-choice-hold-${exercise.exerciseId}"),
                                )
                                WhipFilterChip(
                                    selected = choices[exercise.exerciseId] == CycleReviewChoice.Ignore.name,
                                    onClick = { choices = choices + (exercise.exerciseId to CycleReviewChoice.Ignore.name) },
                                    label = { Text("Ignore recommendation") },
                                    modifier = Modifier.testTag("training-max-choice-ignore-${exercise.exerciseId}"),
                                )
                                WhipFilterChip(
                                    selected = choices[exercise.exerciseId] == CycleReviewChoice.Custom.name,
                                    onClick = { choices = choices + (exercise.exerciseId to CycleReviewChoice.Custom.name) },
                                    label = { Text("Custom") },
                                    modifier = Modifier.testTag("training-max-choice-custom-${exercise.exerciseId}"),
                                )
                            }
                            if (choices[exercise.exerciseId] == CycleReviewChoice.Custom.name) {
                                OutlinedTextField(
                                    value = customValues[exercise.exerciseId].orEmpty(),
                                    onValueChange = { value ->
                                        customValues = customValues + (exercise.exerciseId to value.cycleDeltaInput())
                                    },
                                    label = { Text("Custom cycle change (${unitSymbol(exercise.unitId)})") },
                                    supportingText = {
                                        Text(
                                            if (exercise.eligible) {
                                                "-${editableNumericValue(exercise.standardDelta)} to ${editableNumericValue(maximumDelta)}; zero keeps the current TM"
                                            } else {
                                                "-${editableNumericValue(exercise.standardDelta)} to 0; positive changes require completed Main work"
                                            },
                                        )
                                    },
                                    isError = customValues[exercise.exerciseId]?.toWhipDoubleOrNull()
                                        ?.let { value ->
                                            value !in -exercise.standardDelta..maximumDelta ||
                                                (!exercise.eligible && value > 0.0) || exercise.currentTrainingMax + value <= 0.0
                                        } != false,
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("training-max-custom-delta-${exercise.exerciseId}"),
                                )
                            }
                            if (choices[exercise.exerciseId] == CycleReviewChoice.Ignore.name) {
                                Text(
                                    "Decline Whip's advisory recommendation for this cycle and keep the current Training Max. This is recorded separately from a programming Hold.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            val selectedDelta = decisions.firstOrNull { it.exerciseId == exercise.exerciseId }?.requestedDelta
                            if (selectedDelta != null) {
                                Text(
                                    "Next TM ${editableNumericValue(exercise.currentTrainingMax + selectedDelta)} ${unitSymbol(exercise.unitId)}",
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
                enabled = decisions.size == review.exercises.size && !saving,
                onClick = { onApply(decisions) },
                modifier = Modifier.testTag("apply-training-max-decisions"),
            ) { Text(if (saving) "Finishing…" else "Apply Decisions & Finish") }
        },
        dismissButton = { WhipTextButton(onClick = onDismiss, enabled = !saving) { Text("Keep Training") } },
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
