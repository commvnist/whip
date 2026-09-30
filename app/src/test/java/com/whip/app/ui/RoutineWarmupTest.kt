package com.whip.app.ui

import com.whip.app.domain.BodyweightLoadPolicy
import com.whip.app.domain.EstimatedOneRepMaxFormula
import com.whip.app.domain.Exercise
import com.whip.app.domain.ExerciseTrackingType
import com.whip.app.domain.GymMachine
import com.whip.app.domain.LoadInterpretation
import com.whip.app.domain.MachineLevelDirection
import com.whip.app.domain.MachineLoadType
import com.whip.app.domain.WorkoutSetClassification
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutineWarmupTest {
    @Test
    fun `warmup ramp snaps to exercise increment and preserves working sets`() {
        val placement = RoutineBuilderPlacementState(
            key = 1,
            exerciseId = 1,
            exerciseNameSnapshot = "Press",
            sets = listOf(
                RoutineBuilderSetState(10, load = "100", repetitionsMin = "5", classification = "Working"),
            ),
        )

        val result = generateWarmupSets(placement, exercise(increment = 5.0), null)

        assertEquals(listOf("40", "60", "80", "100"), result.map { it.load })
        assertEquals(listOf("WarmUp", "WarmUp", "WarmUp", "Working"), result.map { it.classification })
        assertEquals(listOf("8", "5", "3"), result.take(3).map { it.repetitionsMin })
    }

    @Test
    fun `warmup regeneration replaces old ramp and uses real machine settings`() {
        val placement = RoutineBuilderPlacementState(
            key = 1,
            exerciseId = 1,
            exerciseNameSnapshot = "Stack press",
            sets = listOf(
                RoutineBuilderSetState(8, load = "2", classification = WorkoutSetClassification.WarmUp.name),
                RoutineBuilderSetState(9, load = "9", repetitionsMin = "8", classification = WorkoutSetClassification.Working.name),
            ),
        )
        val machine = GymMachine(
            id = 3,
            uuid = "machine",
            exerciseId = 1,
            name = "Numbered stack",
            location = "Home",
            details = "",
            loadType = MachineLoadType.Level,
            unitId = "count",
            levelLabel = "level",
            availableLoads = listOf(1.0, 3.0, 5.0, 7.0, 9.0),
            loadInterpretation = LoadInterpretation.OrdinalSetting,
            baseLoadKg = null,
            archived = false,
            createdAtMillis = 0,
            updatedAtMillis = 0,
        )

        val result = generateWarmupSets(placement, exercise(increment = 1.0), machine)

        assertEquals(listOf("3", "5", "7", "9"), result.map { it.load })
        assertEquals(1, result.count { it.classification == WorkoutSetClassification.Working.name })
        assertEquals(4, result.map { it.key }.distinct().size)
    }

    @Test
    fun `reverse numbered machine ramps from higher numbers toward a lower working setting`() {
        val placement = RoutineBuilderPlacementState(
            key = 1,
            exerciseId = 1,
            exerciseNameSnapshot = "Reverse stack press",
            sets = listOf(
                RoutineBuilderSetState(9, load = "1", repetitionsMin = "8", classification = WorkoutSetClassification.Working.name),
            ),
        )
        val machine = GymMachine(
            id = 4,
            uuid = "reverse-machine",
            exerciseId = 1,
            name = "Reverse numbered stack",
            location = "Home",
            details = "",
            loadType = MachineLoadType.Level,
            unitId = "count",
            levelLabel = "level",
            availableLoads = listOf(1.0, 3.0, 5.0, 7.0, 9.0),
            loadInterpretation = LoadInterpretation.OrdinalSetting,
            baseLoadKg = null,
            archived = false,
            createdAtMillis = 0,
            updatedAtMillis = 0,
            levelDirection = MachineLevelDirection.HigherNumberLessResistance,
        )

        val result = generateWarmupSets(placement, exercise(increment = 1.0), machine)

        assertEquals(listOf("7", "5", "3", "1"), result.map { it.load })
    }

    @Test
    fun phaseWarmupsUseSelectedLoadAndPreserveEveryCommonAndSiblingPrescription() {
        val untouched = listOf(
            RoutineBuilderSetState(1, load = "10", classification = "WarmUp", note = "Common"),
            RoutineBuilderSetState(2, load = "100", routinePhaseIndex = 0),
            RoutineBuilderSetState(3, load = "15", classification = "WarmUp", routinePhaseIndex = 0),
            RoutineBuilderSetState(6, load = "120", routinePhaseIndex = 2),
        )
        val selected = listOf(
            RoutineBuilderSetState(4, load = "5", classification = "WarmUp", routinePhaseIndex = 1),
            RoutineBuilderSetState(5, load = "50", repetitionsMin = "5", note = "Keep working", routinePhaseIndex = 1),
        )
        val placement = RoutineBuilderPlacementState(1, 1, "Press", sets = untouched.take(2) + selected + untouched.drop(2))
        val result = generateWarmupSets(placement, exercise(5.0), null, phaseIndex = 1)
        assertEquals(untouched, result.filter { it.routinePhaseIndex != 1 })
        assertEquals(listOf("20", "30", "40", "50"), result.filter { it.routinePhaseIndex == 1 }.map { it.load })
        assertEquals(selected.last(), result.first { it.key == 5L })
        assertEquals(result.size, result.map { it.key }.distinct().size)
        val regenerated = generateWarmupSets(placement.copy(sets = result), exercise(5.0), null, phaseIndex = 1)
        assertEquals(untouched, regenerated.filter { it.routinePhaseIndex != 1 })
        assertEquals(3, regenerated.count { it.routinePhaseIndex == 1 && it.classification == "WarmUp" })
        assertEquals(placement.sets, generateWarmupSets(placement, exercise(5.0), null, phaseIndex = 3))
    }

    private fun exercise(increment: Double) = Exercise(
        id = 1,
        uuid = "exercise",
        name = "Press",
        trackingType = ExerciseTrackingType.WeightReps,
        notes = "",
        equipment = "",
        primaryMuscles = "",
        secondaryMuscles = "",
        weightUnitId = "kilogram",
        weightIncrement = increment,
        repetitionIncrement = 1,
        defaultRestSeconds = 120,
        defaultGraphMetric = "EstimatedOneRepMax",
        oneRepMaxFormula = EstimatedOneRepMaxFormula.Epley,
        barWeightKg = 20.0,
        availablePlatesKg = emptyList(),
        includeInVolume = true,
        includeInPersonalRecords = true,
        bodyweightLoadPolicy = BodyweightLoadPolicy.ExternalWeightOnly,
        effectiveBodyweightPercent = 100.0,
        showRpe = null,
        showRir = null,
        showTempo = null,
        favorite = false,
        position = 0,
        archived = false,
        createdAtMillis = 0,
        updatedAtMillis = 0,
    )
}
