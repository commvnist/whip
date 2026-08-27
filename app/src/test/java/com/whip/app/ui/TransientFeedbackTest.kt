package com.whip.app.ui

import com.whip.app.core.OperationStatus
import com.whip.app.core.OperationFeedbackPresentation
import java.util.concurrent.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TransientFeedbackTest {
    @Test
    fun onlyRecoverableFeedbackSurvivesTopLevelDestinationChanges() {
        assertTrue(transientFeedbackSurvivesDestinationChange(recoverable = true))
        assertFalse(transientFeedbackSurvivesDestinationChange(recoverable = false))
    }

    @Test
    fun terminalMessageIsConsumedBeforeCancelableDisplayAndNonterminalStatesDoNothing() = runBlocking {
        val events = mutableListOf<String>()

        assertThrows(CancellationException::class.java) {
            runBlocking {
                OperationStatus.Succeeded(
                    "Habit created",
                    OperationFeedbackPresentation.Snackbar,
                ).deliverTransientMessage(
                    consume = { events += "consumed" },
                    show = { message ->
                        events += "show:$message"
                        throw CancellationException("Page changed")
                    },
                )
            }
        }
        assertEquals(listOf("consumed", "show:Habit created"), events)

        var idleConsumed = false
        val idleResult = OperationStatus.Idle.deliverTransientMessage(
            consume = { idleConsumed = true },
            show = { "unexpected" },
        )
        assertNull(idleResult)
        assertFalse(idleConsumed)
    }

    @Test
    fun inlineSuccessIsConsumedWithoutShowingSnackbarWhileFailuresStillShow() = runBlocking {
        var inlineConsumed = false
        var inlineShown = false
        val inlineResult = OperationStatus.Succeeded(
            "Habit completed",
            OperationFeedbackPresentation.Inline,
        ).deliverTransientMessage(
            consume = { inlineConsumed = true },
            show = {
                inlineShown = true
                "unexpected"
            },
        )

        assertNull(inlineResult)
        assertEquals(true, inlineConsumed)
        assertFalse(inlineShown)

        val shownFailure = OperationStatus.Failed("Could not save").deliverTransientMessage(
            consume = {},
            show = { it },
        )
        assertEquals("Could not save", shownFailure)
    }

    @Test
    fun routineSuccessDefaultsToVisibleStateInsteadOfSnackbar() = runBlocking {
        var shown = false
        val result = OperationStatus.Succeeded("Set saved").deliverTransientMessage(
            consume = {},
            show = {
                shown = true
                it
            },
        )

        assertNull(result)
        assertFalse(shown)
    }
}
