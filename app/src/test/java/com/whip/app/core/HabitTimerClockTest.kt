package com.whip.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class HabitTimerClockTest {
    @Test
    fun runningTimerUsesMonotonicTimeWhileReviewTimerUsesWallEstimate() {
        assertEquals(
            90.0,
            calculateHabitTimerElapsedSeconds(
                accumulatedCanonicalSeconds = 60.0,
                anchorWallMillis = 1_000L,
                anchorElapsedRealtimeMillis = 5_000L,
                needsReview = false,
                nowWallMillis = 3_601_000L,
                nowElapsedRealtimeMillis = 35_000L,
            ),
            0.0,
        )
        assertEquals(
            121.0,
            calculateHabitTimerElapsedSeconds(
                accumulatedCanonicalSeconds = 60.0,
                anchorWallMillis = 1_000L,
                anchorElapsedRealtimeMillis = 5_000L,
                needsReview = true,
                nowWallMillis = 62_000L,
                nowElapsedRealtimeMillis = 35_000L,
            ),
            0.0,
        )
        assertEquals(
            60.0,
            calculateHabitTimerElapsedSeconds(
                accumulatedCanonicalSeconds = 60.0,
                anchorWallMillis = null,
                anchorElapsedRealtimeMillis = null,
                needsReview = true,
                nowWallMillis = 62_000L,
                nowElapsedRealtimeMillis = 35_000L,
            ),
            0.0,
        )
    }
}
