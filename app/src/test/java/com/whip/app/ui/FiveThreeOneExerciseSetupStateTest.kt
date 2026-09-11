package com.whip.app.ui

import com.whip.app.domain.TrainingMaxBasisKind
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import org.junit.Assert.assertEquals
import org.junit.Test

class FiveThreeOneExerciseSetupStateTest {
    private val bench = FiveThreeOneExerciseSetupState(
        exerciseId = 1, cycleIncrement = "4", cycleIncrementAuthored = true,
        trainingMax = "100", useRecentMaxSuggestion = true, recentMax = "125",
        trainingMaxPercentage = "80", trainingMaxBasisKind = TrainingMaxBasisKind.EstimatedOneRepMax.name,
        appliedSourceMax = "125", appliedTrainingMaxPercentage = "80",
        appliedTrainingMaxBasisKind = TrainingMaxBasisKind.EstimatedOneRepMax.name,
        appliedDerivedTrainingMax = "100", bbbTargetId = 2,
        supplement = FiveThreeOneSupplement.BoringButBig, boringButBigPercent = "60",
        anchorSupplement = FiveThreeOneSupplement.None,
    )
    private val squat = FiveThreeOneExerciseSetupState(
        exerciseId = 2, cycleIncrement = "7", cycleIncrementAuthored = true,
        trainingMax = "200", useRecentMaxSuggestion = true, recentMax = "250",
        trainingMaxPercentage = "80", trainingMaxBasisKind = TrainingMaxBasisKind.ActualOneRepMax.name,
        appliedSourceMax = "250", appliedTrainingMaxPercentage = "80",
        appliedTrainingMaxBasisKind = TrainingMaxBasisKind.ActualOneRepMax.name,
        appliedDerivedTrainingMax = "200", bbbTargetId = 1,
        supplement = FiveThreeOneSupplement.FirstSetLast, boringButBigPercent = "40",
        anchorSupplement = FiveThreeOneSupplement.BoringButStrong,
    )

    @Test fun libraryRefreshKeepsUserOrderAndCompleteExerciseConfiguration() {
        val ids = customFiveThreeOneExerciseIds(listOf(2, 1), listOf(1, 2, 3), firstCustomSelection = false)
        val actual = reconcileFiveThreeOneExerciseSetups(
            listOf(squat, bench), ids.map { FiveThreeOneExerciseSetupState(it, "2.5") },
        )
        assertEquals(listOf(squat, bench), actual)
    }

    @Test fun firstCustomScheduleOrdersByLibraryButTrainingValuesFollowIdentity() {
        val ids = customFiveThreeOneExerciseIds(listOf(2, 1, 0, 0), listOf(1, 2, 3), firstCustomSelection = true)
        val actual = reconcileFiveThreeOneExerciseSetups(
            listOf(squat, bench), ids.map { FiveThreeOneExerciseSetupState(it, "5") },
        )
        assertEquals(listOf(bench, squat), actual)
    }

    @Test fun removedExerciseCannotDonateValuesAndInvalidSupplementalTargetFallsBackToSelf() {
        val fresh = FiveThreeOneExerciseSetupState(3, "2.5")
        val actual = reconcileFiveThreeOneExerciseSetups(
            listOf(squat, bench), listOf(FiveThreeOneExerciseSetupState(1, "5"), fresh),
        )
        assertEquals(listOf(bench.copy(bbbTargetId = 1), fresh), actual)
        assertEquals(listOf(1L), customFiveThreeOneExerciseIds(listOf(2, 1), listOf(1, 3), false))
    }

    @Test fun scheduleSuggestionsRefreshOnlyUntouchedIncreasesEvenWhenAuthoredValueEqualsOldDefault() {
        val untouched = bench.copy(cycleIncrement = "5", cycleIncrementAuthored = false)
        val authored = squat.copy(cycleIncrement = "5")
        val actual = reconcileFiveThreeOneExerciseSetups(
            listOf(untouched, authored),
            listOf(FiveThreeOneExerciseSetupState(1, "2.5"), FiveThreeOneExerciseSetupState(2, "10")),
        )
        assertEquals(listOf(untouched.copy(cycleIncrement = "2.5"), authored), actual)
    }

    @Test fun completeConfigurationsSurviveSavedStateSerialization() {
        val original = arrayListOf(squat, bench)
        val bytes = ByteArrayOutputStream().also { output ->
            ObjectOutputStream(output).use { it.writeObject(original) }
        }.toByteArray()
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }
        assertEquals(original, restored)
    }

    @Test fun presetDefaultsDoNotOverwriteExplicitSupplementalChoices() {
        val fresh = FiveThreeOneExerciseSetupState(3, "5")
        assertEquals(FiveThreeOneSupplement.FirstSetLast, fresh.supplementFor(FiveThreeOneProgramPlan.SingleCycle))
        assertEquals(FiveThreeOneSupplement.BoringButBig, fresh.supplementFor(FiveThreeOneProgramPlan.ForeverBbbLeaderAnchor))
        FiveThreeOneProgramPlan.entries.forEach { plan ->
            assertEquals(FiveThreeOneSupplement.FirstSetLast, squat.supplementFor(plan))
            assertEquals(FiveThreeOneSupplement.None, fresh.copy(supplement = FiveThreeOneSupplement.None).supplementFor(plan))
        }
    }
}
