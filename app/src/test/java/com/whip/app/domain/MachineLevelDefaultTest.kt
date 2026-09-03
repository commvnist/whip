package com.whip.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MachineLevelDefaultTest {
    @Test
    fun endpointIgnoresOrderAndInvalidValuesAndHonorsDirection() {
        val values = listOf(8.0, Double.NaN, -1.0, 3.0, Double.POSITIVE_INFINITY, 5.0)

        assertEquals(
            3.0,
            configuredMachineLevelDefault(values, MachineLevelDirection.HigherNumberMoreResistance),
        )
        assertEquals(
            8.0,
            configuredMachineLevelDefault(values, MachineLevelDirection.HigherNumberLessResistance),
        )
        assertEquals(
            null,
            configuredMachineLevelDefault(listOf(Double.NaN, -2.0), MachineLevelDirection.HigherNumberMoreResistance),
        )
    }

    @Test
    fun resolverUsesFieldLevelPrecedence() {
        assertEquals(9.0, resolveMachineLevelDefault(9.0, 8.0, 7.0, 1.0))
        assertEquals(8.0, resolveMachineLevelDefault(null, 8.0, 7.0, 1.0))
        assertEquals(7.0, resolveMachineLevelDefault(null, null, 7.0, 1.0))
        assertEquals(1.0, resolveMachineLevelDefault(null, null, null, 1.0))
        assertEquals(null, resolveMachineLevelDefault(null, null, null, null))
    }
}
