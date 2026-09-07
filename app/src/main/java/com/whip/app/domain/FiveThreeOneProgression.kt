package com.whip.app.domain

import kotlin.math.abs

/** The role a recorded set plays in one exercise's 5/3/1 progression evidence. */
enum class FiveThreeOneEvidenceKind {
    RequiredMain,
    PrSet,
    Joker,
    TrainingMaxTest,
}

/**
 * One immutable, already-normalized set of evidence for a single exercise.
 *
 * Loads must use the same canonical unit as [FiveThreeOneProgressionInput.currentTrainingMax].
 * A PR set and a Training Max test are required work; a Joker is always optional.
 */
data class FiveThreeOneEvidenceRow(
    val kind: FiveThreeOneEvidenceKind,
    /** Stable session/boundary identity so repeated evidence means independent exposures. */
    val exposureId: String? = null,
    /** Session start time used to compare the most recent independent exposures. */
    val performedAtMillis: Long? = null,
    /** Training Max snapshotted when this work was prescribed. */
    val trainingMaxAtExposure: Double? = null,
    val completed: Boolean,
    val deleted: Boolean = false,
    val failure: Boolean = false,
    val prescribedReps: Int? = null,
    val actualReps: Int? = null,
    val prescribedLoad: Double? = null,
    val actualLoad: Double? = null,
    val rpe: Double? = null,
    val rir: Double? = null,
)

data class FiveThreeOneProgressionInput(
    val evidence: List<FiveThreeOneEvidenceRow>,
    val currentTrainingMax: Double,
    val standardIncrement: Double,
    val allowNonStandardHigher: Boolean = false,
)

enum class FiveThreeOneProgressionCategory(val label: String) {
    InsufficientEvidence("Insufficient evidence"),
    DecreaseReview("Review a decrease"),
    Hold("Hold Training Max"),
    LowerIncrease("Use a lower increase"),
    StandardIncrease("Use the standard increase"),
    CautiousHigherIncrease("Consider a cautious higher increase"),
}

data class FiveThreeOneProgressionRecommendation(
    val category: FiveThreeOneProgressionCategory,
    val suggestedDelta: Double,
    val standardDelta: Double,
    /** Stable score from 0.0 (no actionable evidence) through 1.0 (highest confidence). */
    val confidence: Double,
    /** Human-readable explanations in decision priority order. */
    val reasons: List<String>,
    val engineVersion: String,
) {
    val label: String get() = category.label
}

/** Pure, deterministic recommendations for one exercise at the end of a 5/3/1 cycle. */
object FiveThreeOneProgression {
    const val ENGINE_VERSION: String = "five-three-one-progression/2"

    fun recommend(
        evidence: List<FiveThreeOneEvidenceRow>,
        currentTrainingMax: Double,
        standardIncrement: Double,
        allowNonStandardHigher: Boolean = false,
    ): FiveThreeOneProgressionRecommendation = recommend(
        FiveThreeOneProgressionInput(
            evidence = evidence,
            currentTrainingMax = currentTrainingMax,
            standardIncrement = standardIncrement,
            allowNonStandardHigher = allowNonStandardHigher,
        ),
    )

    fun recommend(input: FiveThreeOneProgressionInput): FiveThreeOneProgressionRecommendation {
        validateFiniteInput(input)

        val required = input.evidence.withIndex().filter { it.value.kind != FiveThreeOneEvidenceKind.Joker }
        val evidenceIssues = required.mapNotNull { (index, row) -> row.evidenceIssue(index) }
        if (required.isEmpty() || evidenceIssues.isNotEmpty()) {
            val details = if (required.isEmpty()) {
                listOf("No required Main, PR-set, or Training Max test evidence was provided.")
            } else {
                evidenceIssues
            }
            return recommendation(
                category = FiveThreeOneProgressionCategory.InsufficientEvidence,
                suggestedDelta = 0.0,
                standardDelta = input.standardIncrement,
                confidence = 0.20,
                reasons = listOf("There is not enough unambiguous required-work evidence to change the Training Max.") + details,
            )
        }

        val missedRequired = required.filter { (_, row) -> row.missedPrescription() }
        val failedTrainingMaxTest = missedRequired.any {
            it.value.kind == FiveThreeOneEvidenceKind.TrainingMaxTest
        }
        val failedExposureCount = missedRequired.mapNotNull { it.value.exposureId }.distinct().size
        if (failedTrainingMaxTest || failedExposureCount >= REPEATED_FAILURE_COUNT) {
            return recommendation(
                category = FiveThreeOneProgressionCategory.DecreaseReview,
                suggestedDelta = -input.standardIncrement,
                standardDelta = input.standardIncrement,
                confidence = 0.95,
                reasons = buildList {
                    add("Review a Training Max decrease before the next cycle.")
                    if (failedTrainingMaxTest) {
                        add("A Training Max test failed or missed its prescribed reps or load.")
                    }
                    if (failedExposureCount >= REPEATED_FAILURE_COUNT) {
                        add("Required work failed or missed its prescription in $failedExposureCount separate sessions.")
                    }
                },
            )
        }

        if (missedRequired.isNotEmpty()) {
            val row = missedRequired.first()
            return recommendation(
                category = FiveThreeOneProgressionCategory.Hold,
                suggestedDelta = 0.0,
                standardDelta = input.standardIncrement,
                confidence = 0.90,
                reasons = listOf(
                    "Hold the current Training Max for the next cycle.",
                    if (missedRequired.size == 1) {
                        "${row.value.kind.displayName()} evidence row ${row.index + 1} failed or missed its prescribed reps or load."
                    } else {
                        "${missedRequired.size} required sets in one session failed or missed their prescriptions; separate-session evidence is required before suggesting a decrease."
                    },
                ),
            )
        }

        val successfulRequired = required.map { it.value }
        val marginalRows = successfulRequired.count { row -> row.isMarginalSuccess() }
        if (marginalRows > 0) {
            return recommendation(
                category = FiveThreeOneProgressionCategory.LowerIncrease,
                suggestedDelta = input.standardIncrement * LOWER_INCREMENT_FACTOR,
                standardDelta = input.standardIncrement,
                confidence = 0.70,
                reasons = listOf(
                    "All required work met its prescribed reps and load.",
                    "$marginalRows required ${if (marginalRows == 1) "set was" else "sets were"} successful but showed marginal effort evidence.",
                    "Use half of the configured standard increase.",
                ),
            )
        }

        val adaptiveAssessment = assessAdaptiveEvidence(
            successfulRequired = successfulRequired,
            currentTrainingMax = input.currentTrainingMax,
            standardIncrement = input.standardIncrement,
        )
        val strongJoker = input.evidence.any { row -> row.isStrongJoker(input.currentTrainingMax) }
        val hasCorroboration = adaptiveAssessment.recentEvidence.all(AdaptivePrEvidence::hasFavorableEffort) || strongJoker
        val fullyCorroborated = adaptiveAssessment.qualifies &&
            adaptiveAssessment.supportsCorroboratedTier &&
            hasCorroboration

        if (adaptiveAssessment.qualifies && input.allowNonStandardHigher) {
            val factor = if (fullyCorroborated) CORROBORATED_HIGHER_INCREMENT_FACTOR else REP_ONLY_HIGHER_INCREMENT_FACTOR
            return recommendation(
                category = FiveThreeOneProgressionCategory.CautiousHigherIncrease,
                suggestedDelta = input.standardIncrement * factor,
                standardDelta = input.standardIncrement,
                confidence = if (fullyCorroborated) 0.90 else 0.80,
                reasons = buildList {
                    add("All required work met its prescribed reps and load.")
                    add(
                        "Two recent PR/AMRAP performances from separate sessions used at least 85% of Training Max, " +
                            "including one at 90% or more; conservative load-adjusted estimates supported the proposed next Training Max without a material drop.",
                    )
                    when {
                        hasCorroboration && !adaptiveAssessment.supportsCorroboratedTier ->
                            add(
                                "Favorable effort or Joker corroboration was present, but conservative load-adjusted estimates supported only the 1.25-times alternative.",
                            )
                        adaptiveAssessment.recentEvidence.all(AdaptivePrEvidence::hasFavorableEffort) ->
                            add(
                                "Both AMRAPs included favorable RPE or RIR corroboration; the non-standard adaptive alternative is capped at 1.5 times the standard increase.",
                            )
                        strongJoker ->
                            add(
                                "A Joker at or above Training Max convincingly beat its rep target without grinder evidence; the non-standard adaptive alternative is capped at 1.5 times the standard increase.",
                            )
                        else ->
                            add(
                                "RPE, RIR, or Joker evidence is not required for this rep-only tier; the non-standard alternative is capped at 1.25 times the standard increase.",
                            )
                    }
                },
            )
        }

        return recommendation(
            category = FiveThreeOneProgressionCategory.StandardIncrease,
            suggestedDelta = input.standardIncrement,
            standardDelta = input.standardIncrement,
            confidence = 0.85,
            reasons = buildList {
                add("All required work met its prescribed reps and load.")
                if (adaptiveAssessment.qualifies) {
                    add("Qualifying adaptive evidence was present, but above-standard alternatives are disabled.")
                } else if (input.allowNonStandardHigher) {
                    add(adaptiveAssessment.explanation)
                }
                add("Use the configured standard increase.")
            },
        )
    }

    private fun validateFiniteInput(input: FiveThreeOneProgressionInput) {
        require(input.currentTrainingMax.isFinite() && input.currentTrainingMax > 0.0) {
            "Current Training Max must be finite and positive"
        }
        require(input.standardIncrement.isFinite() && input.standardIncrement > 0.0) {
            "Standard increment must be finite and positive"
        }
        input.evidence.forEachIndexed { index, row ->
            require(row.prescribedReps == null || row.prescribedReps > 0) {
                "Evidence row ${index + 1} prescribed reps must be positive"
            }
            require(row.actualReps == null || row.actualReps >= 0) {
                "Evidence row ${index + 1} actual reps cannot be negative"
            }
            require(row.performedAtMillis == null || row.performedAtMillis >= 0L) {
                "Evidence row ${index + 1} performance time cannot be negative"
            }
            require(row.prescribedLoad == null || row.prescribedLoad.isFinite() && row.prescribedLoad > 0.0) {
                "Evidence row ${index + 1} prescribed load must be finite and positive"
            }
            require(row.actualLoad == null || row.actualLoad.isFinite() && row.actualLoad >= 0.0) {
                "Evidence row ${index + 1} actual load must be finite and non-negative"
            }
            require(row.rpe == null || row.rpe.isFinite() && row.rpe in 1.0..10.0) {
                "Evidence row ${index + 1} RPE must be finite and between 1 and 10"
            }
            require(row.rir == null || row.rir.isFinite() && row.rir in 0.0..10.0) {
                "Evidence row ${index + 1} RIR must be finite and between 0 and 10"
            }
        }
    }

    private fun FiveThreeOneEvidenceRow.evidenceIssue(index: Int): String? {
        val name = kind.displayName()
        return when {
            kind == FiveThreeOneEvidenceKind.TrainingMaxTest &&
                (trainingMaxAtExposure == null || !trainingMaxAtExposure.isFinite() || trainingMaxAtExposure <= 0.0) ->
                "$name evidence row ${index + 1} has no valid Training Max snapshot."
            kind == FiveThreeOneEvidenceKind.TrainingMaxTest && (prescribedReps ?: Int.MIN_VALUE) !in 3..5 ->
                "$name evidence row ${index + 1} must prescribe 3–5 reps."
            kind == FiveThreeOneEvidenceKind.TrainingMaxTest && prescribedLoad?.let { target ->
                val tm = requireNotNull(trainingMaxAtExposure)
                abs(target - tm) > maxOf(LOAD_TOLERANCE, tm * TRAINING_MAX_TEST_LOAD_TOLERANCE)
            } != false -> "$name evidence row ${index + 1} must prescribe approximately 100% of the snapshotted Training Max."
            deleted && (completed || failure || hasPerformanceData()) ->
                "$name evidence row ${index + 1} is ambiguous because it is deleted but also contains an outcome."
            deleted -> "$name evidence row ${index + 1} is deleted."
            // Failure is an explicit terminal outcome even when the source did not mark the set completed.
            failure -> null
            !completed && hasPerformanceData() ->
                "$name evidence row ${index + 1} is ambiguous because performance is recorded without completion."
            !completed -> "$name evidence row ${index + 1} is incomplete."
            prescribedReps == null || actualReps == null || prescribedLoad == null || actualLoad == null ->
                "$name evidence row ${index + 1} is missing prescribed or actual reps or load."
            rpe != null && rir != null && abs(rpe + rir - 10.0) > EFFORT_CONSISTENCY_TOLERANCE ->
                "$name evidence row ${index + 1} has contradictory RPE and RIR values."
            else -> null
        }
    }

    private fun FiveThreeOneEvidenceRow.hasPerformanceData(): Boolean =
        prescribedReps != null || actualReps != null || prescribedLoad != null || actualLoad != null || rpe != null || rir != null

    private fun FiveThreeOneEvidenceRow.missedPrescription(): Boolean =
        failure || requireNotNull(actualReps) < requireNotNull(prescribedReps) ||
            requireNotNull(actualLoad) + LOAD_TOLERANCE < requireNotNull(prescribedLoad)

    private fun FiveThreeOneEvidenceRow.isMarginalSuccess(): Boolean =
        rpe?.let { it >= MARGINAL_RPE } == true || rir?.let { it <= MARGINAL_RIR } == true

    private fun assessAdaptiveEvidence(
        successfulRequired: List<FiveThreeOneEvidenceRow>,
        currentTrainingMax: Double,
        standardIncrement: Double,
    ): AdaptiveEvidenceAssessment {
        val byExposure = successfulRequired.mapIndexedNotNull { index, row -> row.adaptivePrEvidence(index) }
            .groupBy(AdaptivePrEvidence::exposureId)
            .map { (_, rows) -> rows.maxWith(compareBy<AdaptivePrEvidence> { it.conservativeEstimatedOneRepMax }.thenBy { it.inputIndex }) }
            .sortedWith(compareBy<AdaptivePrEvidence> { it.performedAtMillis ?: Long.MIN_VALUE }.thenBy { it.inputIndex })
        if (byExposure.size < STRONG_PR_SET_COUNT) {
            return AdaptiveEvidenceAssessment(
                explanation = "Adaptive alternatives require two strong PR/AMRAP performances from separate sessions.",
            )
        }

        val recent = byExposure.takeLast(STRONG_PR_SET_COUNT)
        if (recent.none { it.trainingMaxFraction + LOAD_TOLERANCE >= HIGH_INTENSITY_TM_FRACTION }) {
            return AdaptiveEvidenceAssessment(
                recentEvidence = recent,
                explanation = "At least one of the two recent strong AMRAPs must use 90% or more of its snapshotted Training Max.",
            )
        }
        val proposedNextTrainingMax = currentTrainingMax + standardIncrement * REP_ONLY_HIGHER_INCREMENT_FACTOR
        if (recent.any { evidence ->
                evidence.conservativeEstimatedOneRepMax * TRAINING_MAX_OF_E1RM_FACTOR + LOAD_TOLERANCE < proposedNextTrainingMax
            }
        ) {
            return AdaptiveEvidenceAssessment(
                recentEvidence = recent,
                explanation = "The two recent AMRAPs did not both support the proposed next Training Max after conservative load adjustment.",
            )
        }
        val earlier = recent.first().conservativeEstimatedOneRepMax
        val later = recent.last().conservativeEstimatedOneRepMax
        if (later + LOAD_TOLERANCE < earlier * MIN_LATER_ESTIMATE_FRACTION) {
            return AdaptiveEvidenceAssessment(
                recentEvidence = recent,
                explanation = "The later load-adjusted AMRAP estimate dropped materially, so the standard increase remains the safer choice.",
            )
        }
        val corroboratedNextTrainingMax = currentTrainingMax + standardIncrement * CORROBORATED_HIGHER_INCREMENT_FACTOR
        val supportsCorroboratedTier = recent.all { evidence ->
            evidence.conservativeEstimatedOneRepMax * TRAINING_MAX_OF_E1RM_FACTOR + LOAD_TOLERANCE >= corroboratedNextTrainingMax
        }
        return AdaptiveEvidenceAssessment(
            qualifies = true,
            supportsCorroboratedTier = supportsCorroboratedTier,
            recentEvidence = recent,
            explanation = "Repeated load-adjusted AMRAP evidence supports a cautious adaptive alternative.",
        )
    }

    private fun FiveThreeOneEvidenceRow.adaptivePrEvidence(inputIndex: Int): AdaptivePrEvidence? {
        if (kind != FiveThreeOneEvidenceKind.PrSet) return null
        val id = exposureId?.takeIf(String::isNotBlank) ?: return null
        val snapshotTrainingMax = trainingMaxAtExposure?.takeIf { it.isFinite() && it > 0.0 } ?: return null
        val targetReps = prescribedReps ?: return null
        val performedReps = actualReps ?: return null
        val performedLoad = actualLoad ?: return null
        if (performedReps - targetReps < STRONG_REP_SURPLUS) return null
        val fraction = performedLoad / snapshotTrainingMax
        if (fraction + LOAD_TOLERANCE < MIN_ADAPTIVE_TM_FRACTION ||
            fraction - LOAD_TOLERANCE > MAX_ADAPTIVE_TM_FRACTION
        ) return null
        val cappedReps = performedReps.coerceAtMost(MAX_ESTIMATED_REPS)
        if (cappedReps <= 0) return null
        val epley = performedLoad * (1.0 + cappedReps / 30.0)
        val brzycki = performedLoad * 36.0 / (37.0 - cappedReps)
        return AdaptivePrEvidence(
            exposureId = id,
            performedAtMillis = performedAtMillis,
            inputIndex = inputIndex,
            trainingMaxFraction = fraction,
            conservativeEstimatedOneRepMax = minOf(epley, brzycki),
            hasFavorableEffort = hasFavorableEffort(),
        )
    }

    private fun FiveThreeOneEvidenceRow.hasFavorableEffort(): Boolean = when {
        rpe != null && rir != null -> rpe <= STRONG_RPE && rir >= STRONG_RIR
        rpe != null -> rpe <= STRONG_RPE
        rir != null -> rir >= STRONG_RIR
        else -> false
    }

    private fun FiveThreeOneEvidenceRow.isStrongJoker(currentTrainingMax: Double): Boolean {
        if (kind != FiveThreeOneEvidenceKind.Joker || deleted || !completed || failure) return false
        val targetReps = prescribedReps ?: return false
        val performedReps = actualReps ?: return false
        val targetLoad = prescribedLoad ?: return false
        val performedLoad = actualLoad ?: return false
        val contradictoryEffort = rpe != null && rir != null && abs(rpe + rir - 10.0) > EFFORT_CONSISTENCY_TOLERANCE
        val repSurplus = performedReps - targetReps
        val convincinglyExceededTarget = repSurplus >= STRONG_REP_SURPLUS ||
            (repSurplus >= 1 && hasFavorableEffort())
        return !contradictoryEffort && !isMarginalSuccess() && convincinglyExceededTarget &&
            performedLoad + LOAD_TOLERANCE >= targetLoad &&
            performedLoad + LOAD_TOLERANCE >= currentTrainingMax
    }

    private fun FiveThreeOneEvidenceKind.displayName(): String = when (this) {
        FiveThreeOneEvidenceKind.RequiredMain -> "Required Main"
        FiveThreeOneEvidenceKind.PrSet -> "PR-set"
        FiveThreeOneEvidenceKind.Joker -> "Joker"
        FiveThreeOneEvidenceKind.TrainingMaxTest -> "Training Max test"
    }

    private fun recommendation(
        category: FiveThreeOneProgressionCategory,
        suggestedDelta: Double,
        standardDelta: Double,
        confidence: Double,
        reasons: List<String>,
    ): FiveThreeOneProgressionRecommendation {
        require(suggestedDelta.isFinite() && standardDelta.isFinite()) { "Recommendation deltas must be finite" }
        return FiveThreeOneProgressionRecommendation(
            category = category,
            suggestedDelta = suggestedDelta,
            standardDelta = standardDelta,
            confidence = confidence,
            reasons = reasons,
            engineVersion = ENGINE_VERSION,
        )
    }

    private const val REPEATED_FAILURE_COUNT = 2
    private const val STRONG_PR_SET_COUNT = 2
    private const val STRONG_REP_SURPLUS = 2
    private const val MARGINAL_RPE = 9.5
    private const val MARGINAL_RIR = 0.5
    private const val STRONG_RPE = 8.5
    private const val STRONG_RIR = 1.5
    private const val MIN_ADAPTIVE_TM_FRACTION = 0.85
    private const val HIGH_INTENSITY_TM_FRACTION = 0.90
    private const val MAX_ADAPTIVE_TM_FRACTION = 1.10
    private const val MAX_ESTIMATED_REPS = 12
    private const val TRAINING_MAX_OF_E1RM_FACTOR = 0.90
    private const val MIN_LATER_ESTIMATE_FRACTION = 0.95
    private const val TRAINING_MAX_TEST_LOAD_TOLERANCE = 0.025
    private const val EFFORT_CONSISTENCY_TOLERANCE = 1.0
    private const val LOAD_TOLERANCE = 1e-9
    private const val LOWER_INCREMENT_FACTOR = 0.5
    private const val REP_ONLY_HIGHER_INCREMENT_FACTOR = 1.25
    private const val CORROBORATED_HIGHER_INCREMENT_FACTOR = 1.5
}

private data class AdaptiveEvidenceAssessment(
    val qualifies: Boolean = false,
    val supportsCorroboratedTier: Boolean = false,
    val recentEvidence: List<AdaptivePrEvidence> = emptyList(),
    val explanation: String,
)

private data class AdaptivePrEvidence(
    val exposureId: String,
    val performedAtMillis: Long?,
    val inputIndex: Int,
    val trainingMaxFraction: Double,
    val conservativeEstimatedOneRepMax: Double,
    val hasFavorableEffort: Boolean,
)
