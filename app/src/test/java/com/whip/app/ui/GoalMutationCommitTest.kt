package com.whip.app.ui

import java.util.concurrent.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalMutationCommitTest {
    private val committed = GoalMutationReceipt(
        kind = GoalMutationKind.ProgressRecorded,
        goalId = 7L,
        measurementEntryId = "entry-11",
    )

    @Test
    fun ordinaryFollowUpFailureReturnsCommittedSuccessWithWarning() {
        var commits = 0
        val result = runBlocking {
            completeCommittedGoalMutation(
                commit = {
                    commits++
                    committed
                },
                followUp = { error("reminder scheduler unavailable") },
            )
        }

        assertEquals(1, commits)
        assertEquals(committed.measurementEntryId, result.measurementEntryId)
        assertTrue(result.warnings.single().contains("Goal change itself was saved"))
    }

    @Test
    fun cancellationAfterCommitCarriesReceiptAndFatalErrorsEscape() {
        val cancelled = assertThrows(CommittedGoalMutationCancellation::class.java) {
            runBlocking {
                completeCommittedGoalMutation(
                    commit = { committed },
                    followUp = { throw CancellationException("stop") },
                )
            }
        }
        assertEquals(committed, cancelled.receipt)

        assertThrows(AssertionError::class.java) {
            runBlocking {
                completeCommittedGoalMutation(
                    commit = { committed },
                    followUp = { throw AssertionError("fatal") },
                )
            }
        }
    }
}
