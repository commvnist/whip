package com.whip.app.ui

import java.util.concurrent.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitMutationCommitTest {
    private val committed = HabitMutationReceipt(
        kind = HabitMutationKind.LogCreated,
        habitId = 7L,
        logId = 11L,
    )

    @Test
    fun ordinaryFollowUpFailureReturnsACommittedReceiptWithAWarning() {
        var commits = 0
        val result = runBlocking {
            completeCommittedHabitMutation(
                commit = {
                    commits++
                    committed
                },
                followUp = { error("reminder scheduler unavailable") },
            )
        }

        assertEquals(1, commits)
        assertEquals(committed.logId, result.logId)
        assertTrue(result.warnings.single().contains("history change itself was saved"))
    }

    @Test
    fun cancellationAfterCommitCarriesTheReceiptAndFatalErrorsStillEscape() {
        val cancelled = assertThrows(CommittedHabitMutationCancellation::class.java) {
            runBlocking {
                completeCommittedHabitMutation(
                    commit = { committed },
                    followUp = { throw CancellationException("stop") },
                )
            }
        }
        assertEquals(committed, cancelled.receipt)

        assertThrows(AssertionError::class.java) {
            runBlocking {
                completeCommittedHabitMutation(
                    commit = { committed },
                    followUp = { throw AssertionError("fatal") },
                )
            }
        }
    }
}
