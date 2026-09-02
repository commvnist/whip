package com.whip.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedTaskCapturePolicyTest {
    @Test
    fun ordinaryShareKeepsTitleAndSubtasksWithoutWarning() {
        val capture = requireNotNull(
            SharedTaskCapturePolicy.bound("  Buy groceries\r\n Milk \r\n\r\n Eggs  "),
        )

        assertEquals("Buy groceries\nMilk\nEggs", capture.text)
        assertFalse(capture.wasShortened)
    }

    @Test
    fun titleAndSubtaskBudgetsAreExplicitAndDeterministic() {
        val input = buildString {
            append("T".repeat(SharedTaskCapturePolicy.MAX_TITLE_CODE_POINTS + 1))
            repeat(SharedTaskCapturePolicy.MAX_SUBTASKS + 1) { index ->
                append('\n').append("Step ").append(index)
            }
        }

        val capture = requireNotNull(SharedTaskCapturePolicy.bound(input))
        val lines = capture.text.lines()

        assertEquals(SharedTaskCapturePolicy.MAX_TITLE_CODE_POINTS, lines.first().length)
        assertEquals(SharedTaskCapturePolicy.MAX_SUBTASKS + 1, lines.size)
        assertEquals("Step 49", lines.last())
        assertTrue(capture.wasShortened)
    }

    @Test
    fun unicodeAndUtf8BudgetsNeverSplitAnEmoji() {
        val input = "Emoji task\n" + "😀".repeat(20_000)

        val capture = requireNotNull(SharedTaskCapturePolicy.bound(input))
        val subtask = capture.text.lines()[1]

        assertEquals(
            SharedTaskCapturePolicy.MAX_SUBTASK_CODE_POINTS,
            subtask.codePointCount(0, subtask.length),
        )
        assertTrue(subtask.startsWith("😀"))
        assertTrue(subtask.endsWith("😀"))
        assertTrue(capture.wasShortened)
    }

    @Test
    fun blankShareHasNoDraft() {
        assertNull(SharedTaskCapturePolicy.bound(" \r\n\t "))
    }

    @Test
    fun leadingWhitespaceBeyondTheInputBudgetCannotHideMeaningfulText() {
        val capture = requireNotNull(
            SharedTaskCapturePolicy.bound(
                " ".repeat(SharedTaskCapturePolicy.MAX_INPUT_CODE_POINTS + 1) + "Important",
            ),
        )

        assertEquals("Important", capture.text)
        assertFalse(capture.wasShortened)
    }
}
