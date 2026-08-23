package com.whip.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ColorValuesTest {
    @Test
    fun sharedPresetsHaveUniqueNamesAndOpaqueColors() {
        assertEquals(WhipColorPresets.size, WhipColorPresets.map { it.name }.distinct().size)
        assertEquals(WhipColorPresets.size, WhipColorPresets.map { it.argb }.distinct().size)
        assertTrue(WhipColorPresets.all { it.name.isNotBlank() })
        assertTrue(WhipColorPresets.all { it.argb and 0xFF000000L == 0xFF000000L })
    }

    @Test
    fun displayNamesUsePresetNameOrExactCustomRgb() {
        assertEquals("Default", colorDisplayName(null))
        assertEquals("Blue", colorDisplayName(0xFF315CB5L))
        assertEquals("Custom · #123456", colorDisplayName(0xFF123456L))
    }

    @Test
    fun hsvConversionRoundTripsRgbColors() {
        val colors = WhipColorPresets.map { it.argb } + listOf(0xFF000000L, 0xFFFFFFFFL, 0xFF123456L)
        colors.forEach { original ->
            val hsv = colorArgbToHsv(original)
            val converted = hsvToColorArgb(hsv.hue, hsv.saturation, hsv.brightness)
            assertChannelsNear(original, converted)
        }
    }

    @Test
    fun hueWrapsAndSliderInputsAreClamped() {
        assertEquals(hsvToColorArgb(0f, 1f, 1f), hsvToColorArgb(360f, 1f, 1f))
        assertEquals(0xFFFF0000L, hsvToColorArgb(0f, 2f, 2f))
        assertEquals(0xFF000000L, hsvToColorArgb(120f, 1f, -1f))
    }

    private fun assertChannelsNear(expected: Long, actual: Long) {
        listOf(16, 8, 0).forEach { shift ->
            val expectedChannel = (expected shr shift) and 0xFF
            val actualChannel = (actual shr shift) and 0xFF
            assertTrue(
                "Channel $shift differs: expected $expectedChannel, actual $actualChannel",
                abs(expectedChannel - actualChannel) <= 1,
            )
        }
    }
}
