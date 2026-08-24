package com.whip.app.ui

import com.whip.app.core.OperationStatus
import java.util.concurrent.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class TransientFeedbackTest {
    @Test
    fun terminalMessageIsConsumedBeforeCancelableDisplayAndNonterminalStatesDoNothing() = runBlocking {
        val events = mutableListOf<String>()

        assertThrows(CancellationException::class.java) {
            runBlocking {
                OperationStatus.Succeeded("Habit created").deliverTransientMessage(
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
}
