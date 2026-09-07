package com.whip.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FiveThreeOneProgressionTest {
    @Test
    fun recommendationTableCoversEveryCategoryAndBoundary() {
        data class Case(
            val name: String,
            val evidence: List<FiveThreeOneEvidenceRow>,
            val allowHigher: Boolean = false,
            val category: FiveThreeOneProgressionCategory,
            val delta: Double,
        )

        val passingMain = passing()
        val cases = listOf(
            Case(
                name = "no required evidence",
                evidence = emptyList(),
                category = FiveThreeOneProgressionCategory.InsufficientEvidence,
                delta = 0.0,
            ),
            Case(
                name = "deleted required evidence",
                evidence = listOf(passingMain.copy(completed = false, deleted = true, prescribedReps = null, actualReps = null, prescribedLoad = null, actualLoad = null)),
                category = FiveThreeOneProgressionCategory.InsufficientEvidence,
                delta = 0.0,
            ),
            Case(
                name = "missing actual evidence",
                evidence = listOf(passingMain.copy(actualReps = null)),
                category = FiveThreeOneProgressionCategory.InsufficientEvidence,
                delta = 0.0,
            ),
            Case(
                name = "deleted and completed is ambiguous",
                evidence = listOf(passingMain.copy(deleted = true)),
                category = FiveThreeOneProgressionCategory.InsufficientEvidence,
                delta = 0.0,
            ),
            Case(
                name = "one explicit required failure",
                evidence = listOf(passingMain.copy(failure = true)),
                category = FiveThreeOneProgressionCategory.Hold,
                delta = 0.0,
            ),
            Case(
                name = "failure is a terminal outcome without a completion flag",
                evidence = listOf(passingMain.copy(completed = false, failure = true)),
                category = FiveThreeOneProgressionCategory.Hold,
                delta = 0.0,
            ),
            Case(
                name = "one under-load required set",
                evidence = listOf(passingMain.copy(actualLoad = 254.0)),
                category = FiveThreeOneProgressionCategory.Hold,
                delta = 0.0,
            ),
            Case(
                name = "one under-rep required set",
                evidence = listOf(passingMain.copy(actualReps = 4)),
                category = FiveThreeOneProgressionCategory.Hold,
                delta = 0.0,
            ),
            Case(
                name = "repeated misses",
                evidence = listOf(
                    passingMain.copy(actualReps = 4),
                    passingMain.copy(exposureId = "session-2", actualLoad = 250.0),
                ),
                category = FiveThreeOneProgressionCategory.DecreaseReview,
                delta = -10.0,
            ),
            Case(
                name = "failed Training Max test",
                evidence = listOf(passing(FiveThreeOneEvidenceKind.TrainingMaxTest, load = 300.0).copy(actualReps = 2)),
                category = FiveThreeOneProgressionCategory.DecreaseReview,
                delta = -10.0,
            ),
            Case(
                name = "all required work passes",
                evidence = listOf(passingMain),
                category = FiveThreeOneProgressionCategory.StandardIncrease,
                delta = 10.0,
            ),
            Case(
                name = "successful but marginal RPE",
                evidence = listOf(passingMain.copy(rpe = 9.5)),
                category = FiveThreeOneProgressionCategory.LowerIncrease,
                delta = 5.0,
            ),
            Case(
                name = "successful but marginal RIR",
                evidence = listOf(passingMain.copy(rir = 0.5)),
                category = FiveThreeOneProgressionCategory.LowerIncrease,
                delta = 5.0,
            ),
            Case(
                name = "higher evidence remains standard without opt-in",
                evidence = higherEvidence(),
                category = FiveThreeOneProgressionCategory.StandardIncrease,
                delta = 10.0,
            ),
            Case(
                name = "higher evidence is capped at one and a half increments",
                evidence = higherEvidence(),
                allowHigher = true,
                category = FiveThreeOneProgressionCategory.CautiousHigherIncrease,
                delta = 15.0,
            ),
            Case(
                name = "failed Joker is neutral",
                evidence = listOf(passingMain, joker().copy(failure = true)),
                category = FiveThreeOneProgressionCategory.StandardIncrease,
                delta = 10.0,
            ),
            Case(
                name = "skipped Joker is neutral",
                evidence = listOf(passingMain, joker().copy(completed = false, prescribedReps = null, actualReps = null, prescribedLoad = null, actualLoad = null)),
                category = FiveThreeOneProgressionCategory.StandardIncrease,
                delta = 10.0,
            ),
        )

        cases.forEach { case ->
            val result = FiveThreeOneProgression.recommend(
                evidence = case.evidence,
                currentTrainingMax = 300.0,
                standardIncrement = 10.0,
                allowNonStandardHigher = case.allowHigher,
            )

            assertEquals(case.name, case.category, result.category)
            assertEquals(case.name, case.delta, result.suggestedDelta, 0.0)
            assertEquals(case.name, 10.0, result.standardDelta, 0.0)
            assertTrue(case.name, result.confidence in 0.0..1.0)
            assertTrue(case.name, result.reasons.isNotEmpty())
            assertEquals(case.name, FiveThreeOneProgression.ENGINE_VERSION, result.engineVersion)
            assertEquals(case.name, result.category.label, result.label)
        }
    }

    @Test
    fun adaptivePrSetsNeedRepSurplusMeaningfulLoadAndSeparateSessions() {
        val weakCases = listOf(
            higherEvidence().map { if (it.kind == FiveThreeOneEvidenceKind.PrSet) it.copy(actualReps = it.prescribedReps!! + 1) else it },
            higherEvidence().map { if (it.kind == FiveThreeOneEvidenceKind.PrSet) it.copy(prescribedLoad = 200.0, actualLoad = 200.0) else it },
            higherEvidence().map { if (it.kind == FiveThreeOneEvidenceKind.PrSet) it.copy(exposureId = "one-session") else it },
            higherEvidence().filterNot { it.exposureId == "session-pr-2" },
            higherEvidence().map { row ->
                if (row.kind == FiveThreeOneEvidenceKind.PrSet) {
                    row.copy(prescribedLoad = 255.0, actualLoad = 255.0, actualReps = 12)
                } else {
                    row
                }
            },
        )

        weakCases.forEach { evidence ->
            val result = recommend(evidence, allowHigher = true)
            assertEquals(FiveThreeOneProgressionCategory.StandardIncrease, result.category)
            assertEquals(10.0, result.suggestedDelta, 0.0)
        }
    }

    @Test
    fun repeatedLoadAdjustedAmrapsUnlockBoundedRepOnlyTierWithoutEffort() {
        val result = recommend(repOnlyEvidence(), allowHigher = true)

        assertEquals(FiveThreeOneProgressionCategory.CautiousHigherIncrease, result.category)
        assertEquals(12.5, result.suggestedDelta, 0.0)
        assertEquals(0.80, result.confidence, 0.0)
        assertTrue(result.reasons.any { it.contains("RPE, RIR, or Joker evidence is not required") })
        assertTrue(result.reasons.last().contains("1.25 times"))
    }

    @Test
    fun largerAdaptiveTierNeedsFavorableEffortOnBothAmrapsOrOneStrongJoker() {
        val oneEffort = repOnlyEvidence().map { row ->
            if (row.exposureId == "session-pr-1") row.copy(rpe = 8.0) else row
        }
        val bothEffort = repOnlyEvidence().map { row ->
            if (row.kind == FiveThreeOneEvidenceKind.PrSet) row.copy(rpe = 8.0) else row
        }
        val mixedDualEffort = repOnlyEvidence().map { row ->
            if (row.kind == FiveThreeOneEvidenceKind.PrSet) row.copy(rpe = 8.5, rir = 0.6) else row
        }
        val strongJoker = repOnlyEvidence() + joker()
        val merelyCompletedJoker = repOnlyEvidence() + joker().copy(actualReps = 3)
        val uncorroboratedSingleExtraRepJoker = repOnlyEvidence() + joker().copy(actualReps = 4, rpe = null, rir = null)
        val grinderJoker = repOnlyEvidence() + joker().copy(rpe = 9.5, rir = 0.5)

        assertEquals(12.5, recommend(oneEffort, true).suggestedDelta, 0.0)
        assertEquals(15.0, recommend(bothEffort, true).suggestedDelta, 0.0)
        assertEquals(12.5, recommend(mixedDualEffort, true).suggestedDelta, 0.0)
        assertEquals(15.0, recommend(strongJoker, true).suggestedDelta, 0.0)
        assertEquals(12.5, recommend(merelyCompletedJoker, true).suggestedDelta, 0.0)
        assertEquals(12.5, recommend(uncorroboratedSingleExtraRepJoker, true).suggestedDelta, 0.0)
        assertEquals(12.5, recommend(grinderJoker, true).suggestedDelta, 0.0)
    }

    @Test
    fun corroborationCannotExceedTheTierSupportedByConservativeEstimates() {
        val evidence = higherEvidence().map { row ->
            if (row.exposureId == "session-pr-1") row.copy(actualReps = 11) else row
        }

        val result = recommend(evidence, allowHigher = true)

        assertEquals(FiveThreeOneProgressionCategory.CautiousHigherIncrease, result.category)
        assertEquals(12.5, result.suggestedDelta, 0.0)
        assertEquals(0.80, result.confidence, 0.0)
        assertTrue(result.reasons.last().contains("supported only the 1.25-times alternative"))
    }

    @Test
    fun recentAmrapsMustBothSupportNextTrainingMaxWithoutMaterialRegression() {
        val insufficientCapacity = repOnlyEvidence().map { row ->
            if (row.exposureId == "session-pr-2") row.copy(actualReps = 9) else row
        }
        val regressed = repOnlyEvidence().map { row ->
            when (row.exposureId) {
                "session-pr-1" -> row.copy(prescribedLoad = 285.0, actualLoad = 285.0, actualReps = 12)
                "session-pr-2" -> row.copy(prescribedLoad = 270.0, actualLoad = 270.0, actualReps = 10)
                else -> row
            }
        }

        val insufficientResult = recommend(insufficientCapacity, allowHigher = true)
        val regressedResult = recommend(regressed, allowHigher = true)

        assertEquals(FiveThreeOneProgressionCategory.StandardIncrease, insufficientResult.category)
        assertTrue(insufficientResult.reasons.any { it.contains("did not both support") })
        assertEquals(FiveThreeOneProgressionCategory.StandardIncrease, regressedResult.category)
        assertTrue(regressedResult.reasons.any { it.contains("dropped materially") })
    }

    @Test
    fun reasonsAreStableAndOrderedByDecisionPriority() {
        val higher = recommend(higherEvidence(), allowHigher = true)
        assertEquals(
            listOf(
                "All required work met its prescribed reps and load.",
                "Two recent PR/AMRAP performances from separate sessions used at least 85% of Training Max, including one at 90% or more; conservative load-adjusted estimates supported the proposed next Training Max without a material drop.",
                "Both AMRAPs included favorable RPE or RIR corroboration; the non-standard adaptive alternative is capped at 1.5 times the standard increase.",
            ),
            higher.reasons,
        )

        val failedTest = recommend(
            listOf(
                passing(FiveThreeOneEvidenceKind.TrainingMaxTest, load = 300.0).copy(failure = true),
                passing().copy(actualReps = 4),
            ),
        )
        assertEquals(
            listOf(
                "Review a Training Max decrease before the next cycle.",
                "A Training Max test failed or missed its prescribed reps or load.",
            ),
            failedTest.reasons,
        )
    }

    @Test
    fun finiteAndRangeValidationRejectsInvalidInputs() {
        val invalidCalls = listOf<() -> Unit>(
            { FiveThreeOneProgression.recommend(listOf(passing()), Double.NaN, 10.0) },
            { FiveThreeOneProgression.recommend(listOf(passing()), Double.POSITIVE_INFINITY, 10.0) },
            { FiveThreeOneProgression.recommend(listOf(passing()), 300.0, Double.NaN) },
            { FiveThreeOneProgression.recommend(listOf(passing()), 300.0, Double.POSITIVE_INFINITY) },
            { FiveThreeOneProgression.recommend(listOf(passing()), 300.0, 0.0) },
            { recommend(listOf(passing().copy(actualLoad = Double.NaN))) },
            { recommend(listOf(passing().copy(prescribedLoad = Double.POSITIVE_INFINITY))) },
            { recommend(listOf(passing().copy(rpe = 10.1))) },
            { recommend(listOf(passing().copy(rir = -0.1))) },
            { recommend(listOf(passing().copy(actualReps = -1))) },
            { recommend(listOf(passing().copy(performedAtMillis = -1))) },
        )

        invalidCalls.forEachIndexed { index, call ->
            val failure = runCatching(call).exceptionOrNull()
            assertTrue("invalid call $index", failure is IllegalArgumentException)
        }
    }

    @Test
    fun contradictoryEffortAndIncompletePerformanceEvidenceIsInsufficient() {
        val contradictoryEffort = recommend(listOf(passing().copy(rpe = 7.0, rir = 0.0)))
        val performanceWithoutCompletion = recommend(listOf(passing().copy(completed = false)))

        assertEquals(FiveThreeOneProgressionCategory.InsufficientEvidence, contradictoryEffort.category)
        assertEquals(FiveThreeOneProgressionCategory.InsufficientEvidence, performanceWithoutCompletion.category)
        assertTrue(contradictoryEffort.reasons[1].contains("contradictory RPE and RIR"))
        assertTrue(performanceWithoutCompletion.reasons[1].contains("performance is recorded without completion"))
    }

    @Test
    fun repeatedFailureEvidenceRequiresSeparateSessions() {
        val sameSessionMisses = recommend(
            listOf(
                passing().copy(actualReps = 4),
                passing().copy(actualLoad = 250.0),
            ),
        )
        assertEquals(FiveThreeOneProgressionCategory.Hold, sameSessionMisses.category)
    }

    @Test
    fun trainingMaxTestRequiresExplicitValidSnapshotLoadAndRepProtocol() {
        val valid = passing(FiveThreeOneEvidenceKind.TrainingMaxTest, load = 300.0)
        val missingSnapshot = valid.copy(trainingMaxAtExposure = null)
        val wrongLoad = valid.copy(prescribedLoad = 275.0, actualLoad = 275.0)
        val wrongReps = valid.copy(prescribedReps = 1, actualReps = 1)

        assertEquals(FiveThreeOneProgressionCategory.StandardIncrease, recommend(listOf(valid)).category)
        assertEquals(FiveThreeOneProgressionCategory.InsufficientEvidence, recommend(listOf(missingSnapshot)).category)
        assertEquals(FiveThreeOneProgressionCategory.InsufficientEvidence, recommend(listOf(wrongLoad)).category)
        assertEquals(FiveThreeOneProgressionCategory.InsufficientEvidence, recommend(listOf(wrongReps)).category)
        assertEquals(
            FiveThreeOneProgressionCategory.DecreaseReview,
            recommend(listOf(valid.copy(actualReps = 2))).category,
        )
    }

    private fun recommend(
        evidence: List<FiveThreeOneEvidenceRow>,
        allowHigher: Boolean = false,
    ): FiveThreeOneProgressionRecommendation = FiveThreeOneProgression.recommend(
        FiveThreeOneProgressionInput(
            evidence = evidence,
            currentTrainingMax = 300.0,
            standardIncrement = 10.0,
            allowNonStandardHigher = allowHigher,
        ),
    )

    private fun passing(
        kind: FiveThreeOneEvidenceKind = FiveThreeOneEvidenceKind.RequiredMain,
        load: Double = 255.0,
    ) = FiveThreeOneEvidenceRow(
        kind = kind,
        exposureId = "session-main",
        performedAtMillis = 1_000,
        trainingMaxAtExposure = 300.0,
        completed = true,
        prescribedReps = 5,
        actualReps = 5,
        prescribedLoad = load,
        actualLoad = load,
    )

    private fun joker() = FiveThreeOneEvidenceRow(
        kind = FiveThreeOneEvidenceKind.Joker,
        exposureId = "session-joker",
        performedAtMillis = 3_000,
        trainingMaxAtExposure = 300.0,
        completed = true,
        prescribedReps = 3,
        actualReps = 5,
        prescribedLoad = 305.0,
        actualLoad = 305.0,
        rpe = 9.0,
        rir = 1.0,
    )

    private fun higherEvidence(): List<FiveThreeOneEvidenceRow> = listOf(
        passing(),
        FiveThreeOneEvidenceRow(
            kind = FiveThreeOneEvidenceKind.PrSet,
            exposureId = "session-pr-1",
            performedAtMillis = 1_000,
            trainingMaxAtExposure = 300.0,
            completed = true,
            prescribedReps = 5,
            actualReps = 12,
            prescribedLoad = 255.0,
            actualLoad = 255.0,
            rpe = 8.0,
            rir = 2.0,
        ),
        FiveThreeOneEvidenceRow(
            kind = FiveThreeOneEvidenceKind.PrSet,
            exposureId = "session-pr-2",
            performedAtMillis = 2_000,
            trainingMaxAtExposure = 300.0,
            completed = true,
            prescribedReps = 3,
            actualReps = 10,
            prescribedLoad = 270.0,
            actualLoad = 270.0,
            rpe = 8.5,
            rir = 1.5,
        ),
        joker(),
    )

    private fun repOnlyEvidence(): List<FiveThreeOneEvidenceRow> = higherEvidence()
        .filterNot { it.kind == FiveThreeOneEvidenceKind.Joker }
        .map { row -> if (row.kind == FiveThreeOneEvidenceKind.PrSet) row.copy(rpe = null, rir = null) else row }
}
