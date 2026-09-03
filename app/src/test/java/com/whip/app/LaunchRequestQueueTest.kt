package com.whip.app

import com.whip.app.core.WhipLaunchActions
import com.whip.app.widget.WhipWidgetProvider
import com.whip.app.startup.StartupBlockReason
import com.whip.app.startup.StartupRecoveryState
import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchRequestQueueTest {
    @Test
    fun fullShareCapacityCollapsesOnlyRejectedSharesAndPreservesLaterPlatformActions() {
        val queue = LaunchRequestQueue(maxPendingSharedTaskRequests = 4)
        (1L..4L).forEach { deliveryId ->
            queue.enqueue(sharedRequest(deliveryId)) { overflowRequest(100L) }
        }

        queue.enqueue(sharedRequest(5L)) { overflowRequest(100L) }
        queue.enqueue(sharedRequest(6L)) { overflowRequest(101L) }
        queue.enqueue(
            request(
                action = WhipWidgetProvider.ACTION_OPEN_HABIT_TRACKING,
                deliveryId = 7L,
            ),
        ) { overflowRequest(102L) }

        assertEquals(
            listOf(
                WhipLaunchActions.ACTION_CAPTURE_SHARED_TASK,
                WhipLaunchActions.ACTION_CAPTURE_SHARED_TASK,
                WhipLaunchActions.ACTION_CAPTURE_SHARED_TASK,
                WhipLaunchActions.ACTION_CAPTURE_SHARED_TASK,
                WhipLaunchActions.ACTION_SHARED_TASK_QUEUE_OVERFLOW,
                WhipWidgetProvider.ACTION_OPEN_HABIT_TRACKING,
            ),
            queue.snapshot().map(LaunchRequest::action),
        )
        assertEquals("2", queue.snapshot()[4].sharedText)
        assertEquals(100L, queue.snapshot()[4].deliveryId)
        assertEquals(7L, queue.snapshot().last().deliveryId)
    }

    @Test
    fun everyFreshStartStateDropsExistingAndNewLaunchRequests() {
        listOf(
            StartupRecoveryState.FreshStartChecking,
            StartupRecoveryState.FreshStartCheckBlocked("storage unavailable"),
            StartupRecoveryState.FreshStartRequired,
            StartupRecoveryState.FreshStartResetting,
            StartupRecoveryState.FreshStartBlocked("disk full"),
        ).forEach { gatedState ->
            val queue = LaunchRequestQueue(maxPendingSharedTaskRequests = 4)
            queue.enqueue(request(WhipWidgetProvider.ACTION_OPEN_HABIT_TRACKING, 1L)) {
                overflowRequest(99L)
            }

            assertEquals(
                false,
                queue.enqueueUnlessDataEpochGated(
                    state = gatedState,
                    request = sharedRequest(2L),
                ) { overflowRequest(100L) },
            )
            assertEquals(emptyList<LaunchRequest>(), queue.snapshot())
        }
    }

    @Test
    fun currentEpochStartupAndRecoveryStatesStillQueueRequests() {
        listOf(
            StartupRecoveryState.Ready,
            StartupRecoveryState.Checking,
            StartupRecoveryState.Blocked(StartupBlockReason.Recovery),
        ).forEach { state ->
            val queue = LaunchRequestQueue(maxPendingSharedTaskRequests = 4)
            assertEquals(
                true,
                queue.enqueueUnlessDataEpochGated(
                    state = state,
                    request = sharedRequest(3L),
                ) { overflowRequest(101L) },
            )
            assertEquals(listOf(3L), queue.snapshot().map(LaunchRequest::deliveryId))
        }
    }

    private fun sharedRequest(deliveryId: Long) = request(
        action = WhipLaunchActions.ACTION_CAPTURE_SHARED_TASK,
        deliveryId = deliveryId,
        sharedText = "Shared $deliveryId",
    )

    private fun overflowRequest(deliveryId: Long) = request(
        action = WhipLaunchActions.ACTION_SHARED_TASK_QUEUE_OVERFLOW,
        deliveryId = deliveryId,
        sharedText = "1",
    )

    private fun request(
        action: String,
        deliveryId: Long,
        sharedText: String? = null,
    ) = LaunchRequest(
        action = action,
        entityId = null,
        occurrenceEpochDay = null,
        sharedText = sharedText,
        sharedTextShortened = false,
        areaScopeStorageKey = null,
        deliveryId = deliveryId,
    )
}
