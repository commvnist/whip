package com.whip.app.ui

import com.whip.app.domain.WorkoutExercise
import com.whip.app.domain.GymGraphMetric
import com.whip.app.domain.MachineLevelDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GymProgressScopeTest {
    @Test
    fun ordinaryFreeWeightHistoryDoesNotDisableExerciseComparisons() {
        assertFalse(listOf(placement(1, null), placement(2, null)).requiresMachineScope())
    }

    @Test
    fun anyMachineProfileSnapshotEnablesEquipmentScoping() {
        assertTrue(listOf(placement(1, null), placement(2, 42)).requiresMachineScope())
    }

    @Test
    fun deletedMachineSnapshotStillEnablesEquipmentScoping() {
        assertTrue(
            listOf(
                placement(1, null),
                placement(2, null).copy(machineProfileUuidSnapshot = "deleted-profile"),
            ).requiresMachineScope(),
        )
    }

    @Test
    fun bestGraphValueFollowsResistanceDirectionAndPaceMeaning() {
        val more = MachineLevelDirection.HigherNumberMoreResistance
        val less = MachineLevelDirection.HigherNumberLessResistance
        assertEquals(8.0, bestGymGraphValue(listOf(3.0, 8.0), GymGraphMetric.MaxMachineSetting, more), 0.0)
        assertEquals(3.0, bestGymGraphValue(listOf(3.0, 8.0), GymGraphMetric.MaxMachineSetting, less), 0.0)
        assertEquals(3.0, bestGymGraphValue(listOf(3.0, 8.0), GymGraphMetric.Pace, more), 0.0)
        assertEquals(8.0, bestGymGraphValue(listOf(3.0, 8.0), GymGraphMetric.MaxWeight, less), 0.0)
    }

    private fun placement(id: Long, machineId: Long?) = WorkoutExercise(
        id = id,
        uuid = "placement-$id",
        sessionId = 1,
        exerciseId = 1,
        position = id.toInt(),
        notes = "",
        groupId = null,
        machineId = machineId,
        machineProfileUuidSnapshot = machineId?.let { "machine-profile-$it" },
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )
}
