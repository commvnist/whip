package com.whip.app.core

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRuntimeTest {
    @Test
    fun clocksExposeStableDatesAndAWorkingSystemImplementation() {
        val zone = ZoneId.of("America/Toronto")
        val clock = object : WhipClock {
            override fun now(): Instant = Instant.parse("2026-08-27T12:00:00Z")
            override fun zoneId(): ZoneId = zone
        }

        assertEquals(LocalDate.of(2026, 8, 27), clock.today())
        val before = Instant.now()
        assertEquals(ZoneId.systemDefault(), SystemWhipClock.zoneId())
        val systemNow = SystemWhipClock.now()
        val after = Instant.now()
        assertTrue(systemNow >= before && systemNow <= after)
    }

    @Test
    fun generatedIdsAndRuntimeStatesRetainTheirPayloads() {
        assertNotNull(UUID.fromString(UuidWhipIdGenerator.nextId()))

        val cause = IllegalStateException("source")
        assertEquals("Working", OperationStatus.Running("Working").message)
        assertEquals(cause, OperationStatus.Failed("Failed", cause).cause)
        assertEquals(42, WhipResult.Success(42).value)
        assertEquals(cause, WhipResult.Failure("Failed", cause).cause)
    }

    @Test
    fun exactLocalTimeRejectsSpringGapAndReportsTheNextValidMoment() {
        val resolution = resolveExactLocalTime(
            date = LocalDate.of(2026, 3, 8),
            minutes = 2 * 60 + 30,
            zoneId = ZoneId.of("America/New_York"),
        )

        assertTrue(resolution.isGap)
        assertTrue(resolution.options.isEmpty())
        assertEquals(LocalDateTime.of(2026, 3, 8, 3, 0), resolution.firstValidDateTimeAfterGap)
    }

    @Test
    fun exactLocalTimeExposesBothFallOverlapInstants() {
        val resolution = resolveExactLocalTime(
            date = LocalDate.of(2026, 11, 1),
            minutes = 90,
            zoneId = ZoneId.of("America/New_York"),
        )

        assertTrue(resolution.isOverlap)
        assertEquals(listOf("-04:00", "-05:00"), resolution.options.map { it.offset.id })
        assertEquals(3_600L, resolution.options[1].instant.epochSecond - resolution.options[0].instant.epochSecond)
        assertEquals(null, resolution.selected(null))
        assertEquals(
            null,
            resolveEditedExactInstant(Instant.EPOCH, true, resolution, preferredOffsetSeconds = null),
        )
        assertEquals(resolution.options[1], resolution.selected(-5 * 60 * 60))
    }

    @Test
    fun unchangedWallTimePreservesItsCanonicalInstantAcrossZoneChanges() {
        val original = Instant.parse("2026-11-01T05:30:00Z")
        val reprojected = resolveExactLocalTime(
            date = LocalDate.of(2026, 11, 1),
            minutes = 90,
            zoneId = ZoneId.of("America/New_York"),
        )

        assertEquals(original, resolveEditedExactInstant(original, false, reprojected, -5 * 60 * 60))
        assertEquals(
            Instant.parse("2026-11-01T06:30:00Z"),
            resolveEditedExactInstant(original, true, reprojected, -5 * 60 * 60),
        )
    }
}
