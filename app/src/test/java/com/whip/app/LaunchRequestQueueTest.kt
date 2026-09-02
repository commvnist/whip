package com.whip.app

import com.whip.app.core.WhipLaunchActions
import com.whip.app.widget.WhipWidgetProvider
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
