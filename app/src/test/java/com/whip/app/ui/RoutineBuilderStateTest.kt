package com.whip.app.ui

import androidx.lifecycle.SavedStateHandle
import com.whip.app.core.RepPrescriptionScheme
import com.whip.app.domain.RoutineLoadPrescriptionType
import com.whip.app.domain.WorkoutSetClassification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineBuilderStateTest {
    @Test
    fun draftSurvivesViewModelRecreationAndClearsOnlyWhenRequested() {
        val handle = SavedStateHandle()
        val original = RoutineBuilderViewModel(handle)
        original.initialize(
            "new-routine",
            RoutineBuilderState(
                token = "new-routine",
                name = "Upper",
                days = listOf(RoutineBuilderDayState(1, "A")),
                selectedDayKey = 1,
                nextKey = 2,
            ),
        )
        original.update { state ->
            state.copy(
                notes = "Keep this draft",
                days = state.days.map { day ->
                    day.copy(placements = listOf(RoutineBuilderPlacementState(2, 99, "Bench")))
                },
                nextKey = 3,
            )
        }

        val recreated = RoutineBuilderViewModel(handle)
        assertEquals("Keep this draft", recreated.state.value.notes)
        assertEquals("Bench", recreated.state.value.days.single().placements.single().exerciseNameSnapshot)

        recreated.clear()
        assertNull(handle.get<RoutineBuilderState>("routine-builder-state"))
    }

    @Test
    fun groupingAndUngroupingNeverLeaveSingletonGroups() {
        val day = RoutineBuilderDayState(
            key = 1,
            name = "A",
            placements = listOf(
                RoutineBuilderPlacementState(10, 1, "Bench"),
                RoutineBuilderPlacementState(11, 2, "Row"),
                RoutineBuilderPlacementState(12, 3, "Curl"),
                RoutineBuilderPlacementState(13, 4, "Extension"),
            ),
        )

        val grouped = day.groupPlacements(10, 11)
        val group = grouped.placements.first().groupKey
        assertEquals(group, grouped.placements[1].groupKey)

        val withSecondGroup = grouped.groupPlacements(12, 13)
        val regrouped = withSecondGroup.groupPlacements(10, 12)
        assertNull(regrouped.placements.first { it.key == 11L }.groupKey)
        assertEquals(regrouped.placements.first { it.key == 10L }.groupKey, regrouped.placements.first { it.key == 12L }.groupKey)

        val removed = regrouped.removePlacementFromGroup(13).removePlacementFromGroup(10)
        assertNull(removed.placements.first { it.key == 10L }.groupKey)
        assertNull(removed.placements.first { it.key == 12L }.groupKey)
    }

    @Test
    fun dayTemplatesDuplicateAndReorderWithUniquePlacementKeys() {
        val placement = RoutineBuilderPlacementState(
            key = 2,
            exerciseId = 10,
            exerciseNameSnapshot = "Bench",
            sets = listOf(RoutineBuilderSetState(3, repetitionsMin = "8", repetitionsMax = "10")),
        )
        val initial = RoutineBuilderState(
            token = "routine",
            days = listOf(RoutineBuilderDayState(1, "Upper", listOf(placement))),
            selectedDayKey = 1,
            nextKey = 4,
        )

        val duplicated = initial.duplicateDay(1)
        assertEquals(listOf("Upper", "Upper copy"), duplicated.days.map { it.name })
        assertEquals(2, duplicated.days.sumOf { it.placements.size })
        assertEquals(2, duplicated.days.flatMap { it.placements }.map { it.key }.distinct().size)
        assertEquals(2, duplicated.days.flatMap { it.placements }.flatMap { it.sets }.map { it.key }.distinct().size)

        val reordered = duplicated.moveDay(duplicated.days.last().key, -1)
        assertEquals("Upper copy", reordered.days.first().name)
        val templated = initial.copy(days = listOf(RoutineBuilderDayState(1, "Empty")), nextKey = 2)
            .withDayTemplate(listOf("Push", "Pull", "Legs"))
        assertEquals(listOf("Push", "Pull", "Legs"), templated.days.map { it.name })
    }

    @Test
    fun placementReorderSupportsDuplicateExerciseIds() {
        val day = RoutineBuilderDayState(
            1,
            "A",
            listOf(
                RoutineBuilderPlacementState(2, 99, "Bench heavy"),
                RoutineBuilderPlacementState(3, 99, "Bench backoff"),
            ),
        )

        assertEquals(listOf(3L, 2L), day.movePlacement(3, -1).placements.map { it.key })
    }

    @Test
    fun groupedPlacementsBecomeContiguousAndOnlyMoveWithinTheirBlock() {
        val day = RoutineBuilderDayState(
            1,
            "A",
            listOf(
                RoutineBuilderPlacementState(10, 1, "Bench"),
                RoutineBuilderPlacementState(11, 2, "Row"),
                RoutineBuilderPlacementState(12, 3, "Curl"),
                RoutineBuilderPlacementState(13, 4, "Press"),
            ),
        )

        val grouped = day.groupPlacements(10, 12)
        assertEquals(listOf(10L, 12L, 11L, 13L), grouped.placements.map { it.key })
        val moved = grouped.movePlacement(12, -1)
        assertEquals(listOf(12L, 10L, 11L, 13L), moved.placements.map { it.key })
        assertEquals(1, moved.placements.take(2).mapNotNull { it.groupKey }.distinct().size)
    }

    @Test
    fun removingAndRestoringAPlacementPreservesTheGroupInvariant() {
        val grouped = RoutineBuilderDayState(
            1,
            "A",
            listOf(
                RoutineBuilderPlacementState(10, 1, "Bench", groupKey = "Superset A"),
                RoutineBuilderPlacementState(11, 2, "Row", groupKey = "Superset A"),
                RoutineBuilderPlacementState(12, 3, "Curl"),
            ),
        )

        val removed = grouped.removePlacement(10)
        assertNull(removed.placements.single { it.key == 11L }.groupKey)

        val restored = removed.restorePlacement(0, grouped.placements.first(), listOf(10L, 11L))
        assertEquals("Superset A", restored.placements.single { it.key == 10L }.groupKey)
        assertEquals("Superset A", restored.placements.single { it.key == 11L }.groupKey)
    }

    @Test
    fun movingOrCopyingOnePlacementBetweenDaysNeverCarriesAGroupDesignation() {
        val groupedDay = RoutineBuilderDayState(
            1,
            "A",
            listOf(
                RoutineBuilderPlacementState(10, 1, "Bench", groupKey = "Superset A"),
                RoutineBuilderPlacementState(11, 2, "Row", groupKey = "Superset A"),
            ),
        )
        val emptyDay = RoutineBuilderDayState(2, "B")
        val initial = RoutineBuilderState(
            token = "routine",
            days = listOf(groupedDay, emptyDay),
            selectedDayKey = 1,
            nextKey = 20,
        )

        val moved = initial.moveOrCopyPlacement(1, 2, groupedDay.placements.first(), copy = false)
        assertNull(moved.days.single { it.key == 1L }.placements.single().groupKey)
        assertNull(moved.days.single { it.key == 2L }.placements.single().groupKey)

        val copied = initial.moveOrCopyPlacement(1, 2, groupedDay.placements.first(), copy = true)
        assertEquals(2, copied.days.single { it.key == 1L }.placements.count { it.groupKey == "Superset A" })
        assertNull(copied.days.single { it.key == 2L }.placements.single().groupKey)
    }

    @Test
    fun repSchemeAppliesItsMeaningWithoutOverwritingIndependentSetInputs() {
        val existing = listOf(
            RoutineBuilderSetState(key = 7, load = "135", restSeconds = "90", note = "Keep me"),
            RoutineBuilderSetState(key = 8, load = "140", restSeconds = "100"),
        )
        val scheme = RepPrescriptionScheme(
            id = "hypertrophy",
            name = "Hypertrophy",
            setCount = 3,
            repetitionsMin = 8,
            repetitionsMax = 12,
            classification = WorkoutSetClassification.BackOff,
            restSeconds = 120,
        )

        val applied = applyRepPrescriptionScheme(existing, scheme)

        assertEquals(3, applied.size)
        assertEquals(listOf(7L, 8L, 9L), applied.map { it.key })
        assertEquals(listOf("8", "8", "8"), applied.map { it.repetitionsMin })
        assertEquals(listOf("12", "12", "12"), applied.map { it.repetitionsMax })
        assertEquals(listOf("120", "120", "120"), applied.map { it.restSeconds })
        assertEquals(listOf("BackOff", "BackOff", "BackOff"), applied.map { it.classification })
        assertEquals("135", applied.first().load)
        assertEquals("Keep me", applied.first().note)

        val repsOnly = scheme.copy(id = "reps-only", restSeconds = null, setCount = 1, repetitionsMax = 8)
        assertEquals("90", applyRepPrescriptionScheme(existing, repsOnly).single().restSeconds)
    }

    @Test
    fun classicFiveThreeOneBuildsAllFourPhasesWithExplicitAmrapTargets() {
        val config = FiveThreeOneAuthoringConfig(
            trainingMax = 200.0,
            mainScheme = FiveThreeOneMainScheme.Classic,
            phase = FiveThreeOnePhase.Threes,
            supplement = FiveThreeOneSupplement.None,
        )

        val cycle = previewFiveThreeOneCycle(config, increment = 5.0)
        val saved = fiveThreeOneBuilderSets(emptyList(), cycle)

        assertEquals(12, saved.size)
        assertEquals(listOf(0, 1, 2, 3), saved.mapNotNull { it.routinePhaseIndex }.distinct())
        assertEquals(listOf(3, 3, 3, 3), (0..3).map { phase -> saved.count { it.routinePhaseIndex == phase } })
        assertEquals(3, saved.count { it.classification == WorkoutSetClassification.Amrap.name })
        assertTrue(saved.all { it.load.isEmpty() })
        assertTrue(saved.all { it.loadPrescriptionType == RoutineLoadPrescriptionType.PercentTrainingMax.name })
        assertEquals("65", saved.first { it.routinePhaseIndex == FiveThreeOnePhase.Fives.ordinal }.loadPercentage)
        assertTrue(saved.filter { it.classification == WorkoutSetClassification.Amrap.name }.all { "minimum" in it.note })
        assertFalse(saved.filter { it.routinePhaseIndex == FiveThreeOnePhase.Deload.ordinal }
            .any { it.classification == WorkoutSetClassification.Amrap.name })
    }

    @Test
    fun fivesProAndBbbCreateFiveRepMainWorkAndUniversalFiveByTenWork() {
        val config = FiveThreeOneAuthoringConfig(
            trainingMax = 100.0,
            mainScheme = FiveThreeOneMainScheme.FivesPro,
            phase = FiveThreeOnePhase.Fives,
            supplement = FiveThreeOneSupplement.BoringButBig,
            boringButBigPercent = 50.0,
        )

        val cycle = previewFiveThreeOneCycle(config, increment = 2.5)
        val main = cycle.filter { it.plan.section == FiveThreeOneSetSection.Main }
        val supplemental = cycle.filter { it.plan.section == FiveThreeOneSetSection.Supplemental }

        assertEquals(12, main.size)
        assertTrue(main.all { it.plan.repetitions == 5 && !it.plan.amrap })
        assertEquals(5, supplemental.size)
        assertTrue(supplemental.all {
            it.plan.phase == null && it.plan.repetitions == 10 && it.plan.percentageOfTrainingMax == 50.0
        })
    }

    @Test
    fun firstSetLastTracksEachPhasesFirstPercentage() {
        val config = FiveThreeOneAuthoringConfig(
            trainingMax = 100.0,
            mainScheme = FiveThreeOneMainScheme.Classic,
            phase = FiveThreeOnePhase.Fives,
            supplement = FiveThreeOneSupplement.FirstSetLast,
        )

        val supplemental = previewFiveThreeOneCycle(config, increment = 2.5)
            .filter { it.plan.section == FiveThreeOneSetSection.Supplemental }

        assertEquals(20, supplemental.size)
        assertEquals(
            listOf(65.0, 70.0, 75.0, 40.0),
            FiveThreeOnePhase.entries.map { phase ->
                supplemental.first { it.plan.phase == phase }.plan.percentageOfTrainingMax
            },
        )
        assertTrue(supplemental.all { it.plan.repetitions == 5 })
    }

    @Test
    fun kgAndPoundLoadsRoundToPracticalIncrementsOrMachineChoices() {
        assertEquals(85.0, roundedFiveThreeOneLoad(85.1, increment = 2.5), 0.0001)
        assertEquals(190.0, roundedFiveThreeOneLoad(191.25, increment = 5.0), 0.0001)
        assertEquals(87.5, roundedFiveThreeOneLoad(86.0, increment = 2.5, availableLoads = listOf(80.0, 87.5, 95.0)), 0.0001)
        assertEquals(180.0, suggestedFiveThreeOneTrainingMax(200.0, increment = 5.0), 0.0001)
    }

    @Test
    fun changingMassUnitsConvertsTrainingMaxIncrementAndAbsoluteSets() {
        val placement = RoutineBuilderPlacementState(
            key = 1,
            exerciseId = 2,
            exerciseNameSnapshot = "Squat",
            trainingMaxValue = "220",
            trainingMaxUnitId = "pound",
            cycleIncrementValue = "5",
            sets = listOf(
                RoutineBuilderSetState(key = 3, load = "110"),
                RoutineBuilderSetState(
                    key = 4,
                    loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax.name,
                    loadPercentage = "85",
                ),
            ),
        )

        val converted = placement.withProgramMassUnit("kilogram")

        assertEquals("kilogram", converted.trainingMaxUnitId)
        assertEquals("99.75", converted.trainingMaxValue)
        assertEquals("2.5", converted.cycleIncrementValue)
        assertEquals("50", converted.sets.first().load)
        assertEquals("85", converted.sets.last().loadPercentage)
    }
}
