package com.whip.app.ui

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsPresentationPolicyTest {
    @Test
    fun settingsClockRequiresOneCompleteExactHhMmValue() {
        assertEquals(0, parseSettingsClock("00:00"))
        assertEquals(23 * 60 + 59, parseSettingsClock("23:59"))
        assertEquals("03:05", formatSettingsClock(3 * 60 + 5))

        listOf("", "1:02", "01:2", "24:00", "12:60", "12:30:99", " 12:30 ").forEach { value ->
            assertNull("$value must not be accepted as a complete settings clock", parseSettingsClock(value))
        }
    }

    @Test
    fun settingsTimeZoneRequiresACompleteKnownIanaIdentifier() {
        assertEquals("America/Toronto", parseSettingsTimeZone(" America/Toronto ")?.id)
        assertEquals("+02:00", parseSettingsTimeZone("+02:00")?.id)
        assertNull(parseSettingsTimeZone("America"))
        assertNull(parseSettingsTimeZone("Not/A_Zone"))
    }

    @Test
    fun freeFormSettingTagsAreStableAndReadable() {
        assertEquals(
            "settings-field-default-rest-time-seconds",
            settingsFreeFormFieldTag("Default rest time (seconds)"),
        )
    }

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
