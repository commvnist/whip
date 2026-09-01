package com.whip.app.ui

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertTrue
import org.junit.Test

class ElapsedGoalPresentationTest {
    @Test
    fun elapsedStartLabelsUseTheExplicitWhipZone() {
        val label = elapsedGoalStartLabel(
            Instant.parse("2026-09-01T02:00:00Z").toEpochMilli(),
            ZoneId.of("America/Toronto"),
        )

        assertTrue(label.endsWith("America/Toronto"))
        assertTrue(label.contains("Aug") || label.contains("8/31") || label.contains("31/08"))
    }
}
