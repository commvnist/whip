package com.whip.app.core

import java.time.Instant
import java.time.LocalDate
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
}
