package com.whip.app.ui

import androidx.compose.material3.SnackbarHostState
import com.whip.app.core.OperationStatus
import com.whip.app.core.OperationFeedbackPresentation
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
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

    @Test
    fun sameSourceRecoveryAtomicallyReplacesTheOlderRecovery() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val coordinator = TransientFeedbackCoordinator(SnackbarHostState(), scope)
            val firstStarted = CompletableDeferred<Unit>()
            val firstCancelled = CompletableDeferred<Unit>()
            val replacementStarted = CompletableDeferred<Unit>()
            coordinator.present("entry-delete", 2, recoverable = true) {
                firstStarted.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    firstCancelled.complete(Unit)
                }
            }
            assertTrue(firstStarted.isCompleted)

            coordinator.present("entry-delete", 2, recoverable = true) {
                replacementStarted.complete(Unit)
                awaitCancellation()
            }

            assertTrue(firstCancelled.isCompleted)
            assertTrue(replacementStarted.isCompleted)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun differentRecoveryWaitsBehindTheActiveRecoveryAndStartsAfterInvalidation() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val coordinator = TransientFeedbackCoordinator(SnackbarHostState(), scope)
            val firstStarted = CompletableDeferred<Unit>()
            val secondStarted = CompletableDeferred<Unit>()
            coordinator.present("first", 2, recoverable = true) {
                firstStarted.complete(Unit)
                awaitCancellation()
            }
            coordinator.present("second", 2, recoverable = true) {
                secondStarted.complete(Unit)
                awaitCancellation()
            }

            assertTrue(firstStarted.isCompleted)
            assertFalse(secondStarted.isCompleted)
            coordinator.invalidateRecovery("first")
            assertTrue(secondStarted.isCompleted)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun sourceInvalidationRemovesItsQueuedFeedbackWithoutDisturbingAnotherRecovery() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val coordinator = TransientFeedbackCoordinator(SnackbarHostState(), scope)
            val activeStarted = CompletableDeferred<Unit>()
            val queuedStarted = CompletableDeferred<Unit>()
            coordinator.present("active", 2, recoverable = true) {
                activeStarted.complete(Unit)
                awaitCancellation()
            }
            coordinator.present("queued", 3, recoverable = false) {
                queuedStarted.complete(Unit)
                awaitCancellation()
            }

            coordinator.invalidate("queued")
            assertTrue(activeStarted.isCompleted)
            assertFalse(queuedStarted.isCompleted)
            coordinator.invalidateRecovery("active")
            assertFalse(queuedStarted.isCompleted)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun destinationChangePreservesRecoveryAndDropsQueuedTransientFeedback() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val coordinator = TransientFeedbackCoordinator(SnackbarHostState(), scope)
            val recoveryCancelled = CompletableDeferred<Unit>()
            val queuedStarted = CompletableDeferred<Unit>()
            coordinator.present("recovery", 2, recoverable = true) {
                try {
                    awaitCancellation()
                } finally {
                    recoveryCancelled.complete(Unit)
                }
            }
            coordinator.present("transient", 2, recoverable = false) {
                queuedStarted.complete(Unit)
                awaitCancellation()
            }

            coordinator.onDestinationChanged()
            assertFalse(recoveryCancelled.isCompleted)
            assertFalse(queuedStarted.isCompleted)
            coordinator.invalidateRecovery("recovery")
            assertFalse(queuedStarted.isCompleted)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun highPriorityFeedbackBlocksLowerPriorityButRecoveryCanReplaceIt() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val coordinator = TransientFeedbackCoordinator(SnackbarHostState(), scope)
            val highCancelled = CompletableDeferred<Unit>()
            val lowStarted = CompletableDeferred<Unit>()
            val recoveryStarted = CompletableDeferred<Unit>()
            coordinator.present("error", 3, recoverable = false) {
                try {
                    awaitCancellation()
                } finally {
                    highCancelled.complete(Unit)
                }
            }
            coordinator.present("low", 1, recoverable = false) {
                lowStarted.complete(Unit)
                awaitCancellation()
            }
            assertFalse(lowStarted.isCompleted)

            coordinator.present("undo", 2, recoverable = true) {
                recoveryStarted.complete(Unit)
                awaitCancellation()
            }
            assertTrue(highCancelled.isCompleted)
            assertTrue(recoveryStarted.isCompleted)
            assertFalse(lowStarted.isCompleted)
        } finally {
            scope.cancel()
        }
    }
}
