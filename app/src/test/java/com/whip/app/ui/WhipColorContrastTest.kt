package com.whip.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhipColorContrastTest {
    @Test
    fun mediumGrayChoosesTheHigherContrastAaForeground() {
        val background = Color(0xFF919191)
        val foreground = readableForeground(0xFF919191L)

        val blackContrast = contrastRatio(background, Color.Black)
        val whiteContrast = contrastRatio(background, Color.White)

        assertEquals(Color.Black, foreground)
        assertTrue("Expected black to have the stronger contrast: $blackContrast vs $whiteContrast", blackContrast > whiteContrast)
        assertTrue("Chosen foreground must meet WCAG AA: $blackContrast", blackContrast >= 4.5f)
    }

    private fun contrastRatio(first: Color, second: Color): Float {
        val lighter = maxOf(first.luminance(), second.luminance()) + 0.05f
        val darker = minOf(first.luminance(), second.luminance()) + 0.05f
        return lighter / darker
    }
}
