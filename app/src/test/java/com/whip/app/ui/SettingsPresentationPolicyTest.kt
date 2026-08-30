package com.whip.app.ui

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsPresentationPolicyTest {
    @Test
    fun timestampsUseTheConfiguredZoneAndLocalizedPresentation() {
        val instant = Instant.parse("2026-08-30T01:05:00Z")
        val utc = formatSettingsTimestamp(instant, ZoneId.of("UTC"), Locale.US)
        val toronto = formatSettingsTimestamp(instant, ZoneId.of("America/Toronto"), Locale.US)

        assertNotEquals(utc, toronto)
        assertTrue(utc.contains("2026"))
        assertTrue(toronto.contains("2026"))
        assertFalse(utc.contains('T'))
        assertFalse(toronto.contains('T'))
    }
}
