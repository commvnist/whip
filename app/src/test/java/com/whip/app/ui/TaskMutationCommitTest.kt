package com.whip.app.ui

import java.time.LocalDate
import java.util.concurrent.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskMutationCommitTest {
    private val committed = TaskMutationReceipt(
        kind = TaskMutationKind.Rescheduled,
        taskIds = setOf(7L),
        occurrenceKeys = setOf("7:task"),
        effectiveDate = LocalDate.of(2026, 8, 31),
    )

    @Test
    fun ordinaryFollowUpFailureReturnsCommittedSuccessWithAWarning() {
        var commits = 0
        val result = runBlocking {
            completeCommittedTaskMutation(
                commit = {
                    commits++
                    committed
                },
                followUp = { error("reminder scheduler unavailable") },
            )
        }

        assertEquals(1, commits)
        assertEquals(committed.taskIds, result.taskIds)
        assertTrue(result.warnings.single().contains("Task change itself was saved"))
    }

    @Test
    fun cancellationAfterCommitCarriesTheExactReceiptAndFatalErrorsEscape() {
        val cancelled = assertThrows(CommittedTaskMutationCancellation::class.java) {
            runBlocking {
                completeCommittedTaskMutation(
                    commit = { committed },
                    followUp = { throw CancellationException("stop") },
                )
            }
        }
        assertEquals(committed, cancelled.receipt)

        assertThrows(AssertionError::class.java) {
            runBlocking {
                completeCommittedTaskMutation(
                    commit = { committed },
                    followUp = { throw AssertionError("fatal") },
                )
            }
        }
    }
}
