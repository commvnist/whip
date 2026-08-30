package com.whip.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DestructiveSubmissionTest {
    @Test
    fun twoImmediateAttemptsInvokeTheDestructiveActionExactlyOnce() {
        var submitted = false
        var destructiveInvocations = 0

        val firstAccepted = submitDestructiveActionOnce(
            alreadySubmitted = submitted,
            busy = false,
            markSubmitted = { submitted = true },
            action = { destructiveInvocations += 1 },
        )
        val secondAccepted = submitDestructiveActionOnce(
            alreadySubmitted = submitted,
            busy = false,
            markSubmitted = { submitted = true },
            action = { destructiveInvocations += 1 },
        )

        assertTrue(firstAccepted)
        assertFalse(secondAccepted)
        assertEquals(1, destructiveInvocations)
    }

    @Test
    fun busyStateRejectsSubmissionWithoutMutatingTheLatch() {
        var submitted = false
        var destructiveInvocations = 0

        val accepted = submitDestructiveActionOnce(
            alreadySubmitted = submitted,
            busy = true,
            markSubmitted = { submitted = true },
            action = { destructiveInvocations += 1 },
        )

        assertFalse(accepted)
        assertFalse(submitted)
        assertEquals(0, destructiveInvocations)
    }
}
