package com.whip.app.ui

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class GoalProgressPresentationTest {
    @Test fun aBeginningAndNearlyReachedTargetNeverReadAsTheirEndpoints() {
        val cases = listOf(
            0.0 to "0%", -0.0 to "0%", Double.MIN_VALUE to "<0.1%",
            0.00005 to "<0.1%", 0.001 to "0.1%", 0.005 to "0.5%",
            0.999 to "99.9%", 0.99901 to ">99.9%", Math.nextDown(1.0) to ">99.9%", 1.0 to "100%",
        )
        cases.forEach { (progress, expected) -> assertEquals("Ratio $progress", expected, formatGoalProgressPercent(progress, Locale.US)) }
    }

    @Test fun ordinaryProgressUsesOneReadableDecimalWithoutTrailingZeros() {
        assertEquals("12.3%", formatGoalProgressPercent(0.123456, Locale.US))
        assertEquals("33.3%", formatGoalProgressPercent(1.0 / 3.0, Locale.US))
        assertEquals("50%", formatGoalProgressPercent(0.5, Locale.US))
        assertEquals("66.7%", formatGoalProgressPercent(2.0 / 3.0, Locale.US))
    }

    @Test fun localeControlsDecimalAndPercentSpacingForEveryState() {
        assertEquals("0,5\u00a0%", formatGoalProgressPercent(0.005, Locale.GERMANY))
        assertEquals("<0,1\u00a0%", formatGoalProgressPercent(0.00005, Locale.GERMANY))
        assertEquals(">99,9\u00a0%", formatGoalProgressPercent(0.99999, Locale.GERMANY))
        assertEquals("100\u00a0%", formatGoalProgressPercent(1.0, Locale.GERMANY))
    }
}
