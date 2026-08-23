package com.whip.app.ui

import com.whip.app.domain.WorkoutExercise
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
