package com.whip.app.ui

import com.whip.app.domain.BodyweightLoadPolicy
import com.whip.app.domain.EstimatedOneRepMaxFormula
import com.whip.app.domain.Exercise
import com.whip.app.domain.ExerciseTrackingType
import com.whip.app.domain.GymMachine
import com.whip.app.domain.LoadInterpretation
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
