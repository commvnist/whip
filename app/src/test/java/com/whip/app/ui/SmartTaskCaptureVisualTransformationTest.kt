package com.whip.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import com.whip.app.domain.TaskQuickCaptureParser
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SmartTaskCaptureVisualTransformationTest {
    @Test
    fun everyParserAssumptionReceivesTheVisibleHighlightStyleAtItsSourceRange() {
        val input = "Plan launch every 2 weeks on 2026-09-01 deadline 2026-10-01"
        val parsed = TaskQuickCaptureParser.parse(input, LocalDate.of(2026, 8, 25))
        val highlight = Color(0xFFDDD0FF)
        val foreground = Color(0xFF241047)

        val transformed = SmartTaskCaptureVisualTransformation(
            assumptions = parsed.assumptions,
            highlightColor = highlight,
            highlightedTextColor = foreground,
        ).filter(AnnotatedString(input)).text

        assertEquals(input, transformed.text)
        assertEquals(parsed.assumptions.size, transformed.spanStyles.size)
        parsed.assumptions.forEach { assumption ->
            val style = transformed.spanStyles.single {
                it.start == assumption.start && it.end == assumption.endExclusive
            }.item
            assertEquals(highlight, style.background)
            assertEquals(foreground, style.color)
        }
    }
}
