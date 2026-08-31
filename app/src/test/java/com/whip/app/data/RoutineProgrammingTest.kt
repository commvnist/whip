package com.whip.app.data

import com.whip.app.domain.RoutineLoadPrescriptionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RoutineProgrammingTest {
    @Test
    fun percentageLoadsResolveFromOneRepAndTrainingMaxThenSnapToEquipment() {
        val oneRep = resolveRoutinePrescribedLoad(
            type = RoutineLoadPrescriptionType.PercentOneRepMax,
            enteredWeight = null,
            enteredUnitId = "kilogram",
            percentage = 75.0,
            oneRepMaxKg = 100.0,
            trainingMaxPercent = 90.0,
            progressionPercent = 100.0,
            loadMultiplier = 1.0,
            baseLoadKg = null,
            addOnPlateKg = null,
            availableLoads = emptyList(),
            increment = 2.5,
        )
        assertEquals(75.0, requireNotNull(oneRep).displayValue, 0.0)
        assertEquals("75.0% e1RM", oneRep.label)

        val training = resolveRoutinePrescribedLoad(
            type = RoutineLoadPrescriptionType.PercentTrainingMax,
            enteredWeight = null,
            enteredUnitId = "kilogram",
            percentage = 80.0,
            oneRepMaxKg = 200.0,
            trainingMaxPercent = 90.0,
            progressionPercent = 100.0,
            loadMultiplier = 1.0,
            baseLoadKg = null,
            addOnPlateKg = null,
            availableLoads = listOf(130.0, 140.0, 150.0),
            increment = 2.5,
        )
        assertEquals(140.0, requireNotNull(training).displayValue, 0.0)
        assertEquals("80.0% of 90.0% training max", training.label)
    }

    @Test
    fun percentageTargetsInvertPerSideAndBaseLoadMeaningBeforeRounding() {
        val resolved = resolveRoutinePrescribedLoad(
            type = RoutineLoadPrescriptionType.PercentOneRepMax,
            enteredWeight = null,
            enteredUnitId = "kilogram",
            percentage = 80.0,
            oneRepMaxKg = 100.0,
            trainingMaxPercent = 90.0,
            progressionPercent = 100.0,
            loadMultiplier = 2.0,
            baseLoadKg = 20.0,
            addOnPlateKg = null,
            availableLoads = emptyList(),
            increment = 2.5,
        )
        assertEquals(30.0, requireNotNull(resolved).displayValue, 0.0)
    }

    @Test
    fun explicitTrainingMaxIsStableWhenEstimatedOneRepMaxChanges() {
        fun resolve(oneRepMaxKg: Double) = resolveRoutinePrescribedLoad(
            type = RoutineLoadPrescriptionType.PercentTrainingMax,
            enteredWeight = null,
            enteredUnitId = "kilogram",
            percentage = 85.0,
            oneRepMaxKg = oneRepMaxKg,
            trainingMaxPercent = 90.0,
            progressionPercent = 100.0,
            loadMultiplier = 1.0,
            baseLoadKg = null,
            addOnPlateKg = null,
            availableLoads = emptyList(),
            increment = 2.5,
            explicitTrainingMaxKg = 200.0,
        )

        val before = requireNotNull(resolve(oneRepMaxKg = 225.0))
        val after = requireNotNull(resolve(oneRepMaxKg = 300.0))
        assertEquals(170.0, before.displayValue, 0.0)
        assertEquals(before.displayValue, after.displayValue, 0.0)
        assertEquals("85.0% of explicit training max", before.label)
    }

    @Test
    fun waveMultiplierAppliesToAbsoluteLoadsAndMissingMaxFailsClearly() {
        val deload = resolveRoutinePrescribedLoad(
            type = RoutineLoadPrescriptionType.Absolute,
            enteredWeight = 100.0,
            enteredUnitId = "kilogram",
            percentage = null,
            oneRepMaxKg = null,
            trainingMaxPercent = 90.0,
            progressionPercent = 90.0,
            loadMultiplier = 1.0,
            baseLoadKg = null,
            addOnPlateKg = null,
            availableLoads = emptyList(),
            increment = 2.5,
        )
        assertEquals(90.0, requireNotNull(deload).displayValue, 0.0)
        assertThrows(IllegalArgumentException::class.java) {
            resolveRoutinePrescribedLoad(
                RoutineLoadPrescriptionType.PercentOneRepMax,
                null,
                "kilogram",
                70.0,
                null,
                90.0,
                100.0,
                1.0,
                null,
                null,
                emptyList(),
                2.5,
            )
        }
    }
}
