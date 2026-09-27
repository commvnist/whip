package com.whip.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class GoalCelebrationTest {
    @Test fun cardFadesInAndOutSymmetricallyWithinFourSecondTimeline() {
        val fractions = listOf(0f, 0.1f, 0.2f, 0.5f, 0.8f, 0.9f, 1f)
        val expected = listOf(0f, 0.5f, 1f, 1f, 1f, 0.5f, 0f)
        fractions.zip(expected).forEach { (time, opacity) ->
            assertEquals("Progress $time", opacity, celebrationCardVisibility(time), 0.000001f)
        }
    }
}
