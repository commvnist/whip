package com.whip.app.reminders

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderActionCommitTest {
    @Test
    fun ordinaryFailureBeforeCommitReleasesClaimForSafeRetry() = runBlocking {
        val committed = AtomicInteger()
        val released = AtomicInteger()

        val result = executeClaimedNotificationAction(
            applyAction = { error("database unavailable") },
            markCommitted = { committed.incrementAndGet(); true },
            releaseClaim = { released.incrementAndGet() },
            followUp = {},
        )

        assertFalse(result)
        assertEquals(0, committed.get())
        assertEquals(1, released.get())
    }

    @Test
    fun ordinaryFollowUpFailureKeepsCommittedReceiptAndDoesNotPermitReplay() = runBlocking {
        val committed = AtomicInteger()
        val released = AtomicInteger()

        val result = executeClaimedNotificationAction(
            applyAction = { true },
            markCommitted = { committed.incrementAndGet(); true },
            releaseClaim = { released.incrementAndGet() },
            followUp = { error("scheduler unavailable") },
        )

        assertTrue(result)
        assertEquals(1, committed.get())
        assertEquals(0, released.get())
    }

    @Test
    fun ledgerWriteFailureAfterAuthoritativeActionDoesNotReleaseForReplay() = runBlocking {
        val released = AtomicInteger()

        val result = executeClaimedNotificationAction(
            applyAction = { true },
            markCommitted = { error("receipt storage unavailable") },
            releaseClaim = { released.incrementAndGet() },
            followUp = {},
        )

        assertTrue(result)
        assertEquals(0, released.get())
    }

    @Test
    fun cancellationReleasesOnlyBeforeAuthoritativeCommitAndAlwaysEscapes() = runBlocking {
        val releasedBefore = AtomicInteger()
        val before = runCatching {
            executeClaimedNotificationAction(
                applyAction = { throw CancellationException("cancel before") },
                markCommitted = { true },
                releaseClaim = { releasedBefore.incrementAndGet() },
                followUp = {},
            )
        }
        assertTrue(before.exceptionOrNull() is CancellationException)
        assertEquals(1, releasedBefore.get())

        val releasedAfter = AtomicInteger()
        val after = runCatching {
            executeClaimedNotificationAction(
                applyAction = { true },
                markCommitted = { true },
                releaseClaim = { releasedAfter.incrementAndGet() },
                followUp = { throw CancellationException("cancel after") },
            )
        }
        assertTrue(after.exceptionOrNull() is CancellationException)
        assertEquals(0, releasedAfter.get())
    }

    @Test
    fun fatalErrorsEscapeWithoutReleasingACommittedAction() = runBlocking {
        val released = AtomicInteger()

        val result = runCatching {
            executeClaimedNotificationAction(
                applyAction = { true },
                markCommitted = { true },
                releaseClaim = { released.incrementAndGet() },
                followUp = { throw AssertionError("fatal") },
            )
        }

        assertTrue(result.exceptionOrNull() is AssertionError)
        assertEquals(0, released.get())
    }
}
