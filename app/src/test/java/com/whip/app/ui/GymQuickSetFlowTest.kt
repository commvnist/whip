package com.whip.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GymQuickSetFlowTest {
    @Test
    fun `load stepper follows exact machine choices and clamps at both ends`() {
        val stack = listOf(1.0, 2.0, 4.0, 7.0, 10.0)
        assertEquals(1.0, steppedWorkoutLoad(null, 1, stack, 2.5), 0.0)
        assertEquals(7.0, steppedWorkoutLoad(4.0, 1, stack, 2.5), 0.0)
        assertEquals(2.0, steppedWorkoutLoad(4.0, -1, stack, 2.5), 0.0)
        assertEquals(10.0, steppedWorkoutLoad(10.0, 1, stack, 2.5), 0.0)
        assertEquals(1.0, steppedWorkoutLoad(1.0, -1, stack, 2.5), 0.0)
    }

    @Test
    fun `load stepper uses exercise increment without producing negative load`() {
        assertEquals(50.0, steppedWorkoutLoad(45.0, 1, emptyList(), 5.0), 0.0)
        assertEquals(40.0, steppedWorkoutLoad(45.0, -1, emptyList(), 5.0), 0.0)
        assertEquals(0.0, steppedWorkoutLoad(2.5, -1, emptyList(), 5.0), 0.0)
    }

    @Test
    fun `save and next reuses another incomplete planned set`() {
        val sets = listOf(
            QuickSetState(id = 1, workoutExerciseId = 10, completed = false, deleted = false),
            QuickSetState(id = 2, workoutExerciseId = 10, completed = false, deleted = false),
            QuickSetState(id = 3, workoutExerciseId = 11, completed = true, deleted = false),
        )

        assertFalse(shouldAppendAfterQuickSave(1, setOf(10, 11), sets))
    }

    @Test
    fun `save and next follows incomplete set in another exercise rotation`() {
        val sets = listOf(
            QuickSetState(id = 1, workoutExerciseId = 10, completed = false, deleted = false),
            QuickSetState(id = 2, workoutExerciseId = 11, completed = false, deleted = false),
        )

        assertFalse(shouldAppendAfterQuickSave(1, setOf(10, 11), sets))
    }

    @Test
    fun `save and next appends only when no incomplete set remains`() {
        val sets = listOf(
            QuickSetState(id = 1, workoutExerciseId = 10, completed = false, deleted = false),
            QuickSetState(id = 2, workoutExerciseId = 11, completed = true, deleted = false),
            QuickSetState(id = 3, workoutExerciseId = 11, completed = false, deleted = true),
            QuickSetState(id = 4, workoutExerciseId = 99, completed = false, deleted = false),
        )

        assertTrue(shouldAppendAfterQuickSave(1, setOf(10, 11), sets))
    }
}
