package com.whip.app.reminders

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderDeliveryCoordinatorTest {
    @Test
    fun sameEntityMutationAndDeliveryCannotOverlap() {
        runBlocking {
            val coordinator = ReminderDeliveryCoordinator(stripeCount = 64)
            val firstEntered = CompletableDeferred<Unit>()
            val releaseFirst = CompletableDeferred<Unit>()
            val secondEntered = CompletableDeferred<Unit>()
            val first = async {
                coordinator.withEntity(ReminderDomain.Task, 42L) {
                    firstEntered.complete(Unit)
                    releaseFirst.await()
                }
            }
            firstEntered.await()
            val second = async {
                coordinator.withEntity(ReminderDomain.Task, 42L) {
                    secondEntered.complete(Unit)
                }
            }

            assertNull(withTimeoutOrNull(50) { secondEntered.await() })
            releaseFirst.complete(Unit)
            secondEntered.await()
            first.await()
            second.await()
        }
    }

    @Test
    fun unrelatedEntitiesRemainIndependent() {
        runBlocking {
            val coordinator = ReminderDeliveryCoordinator(stripeCount = 64)
            val releaseTask = CompletableDeferred<Unit>()
            val taskEntered = CompletableDeferred<Unit>()
            val task = async {
                coordinator.withEntity(ReminderDomain.Task, 7L) {
                    taskEntered.complete(Unit)
                    releaseTask.await()
                }
            }
            taskEntered.await()

            val goalResult = coordinator.withEntity(ReminderDomain.Goal, 99L) { "goal-ready" }

            assertEquals("goal-ready", goalResult)
            releaseTask.complete(Unit)
            task.await()
        }
    }

    @Test
    fun stateBoundaryBlocksNestedAndChildCoroutinesUntilTheOwnerExits() {
        runBlocking {
            val coordinator = ReminderDeliveryCoordinator()
            val nestedEntered = CompletableDeferred<Unit>()
            val childEntered = CompletableDeferred<Unit>()
            lateinit var nested: kotlinx.coroutines.Deferred<Unit>
            lateinit var child: kotlinx.coroutines.Deferred<Unit>
            coordinator.withStateBoundary {
                nested = async {
                    coordinator.withStateBoundary { nestedEntered.complete(Unit) }
                }
                child = async {
                    coordinator.withStateBoundary { childEntered.complete(Unit) }
                }
                assertNull(withTimeoutOrNull(50) { nestedEntered.await() })
                assertNull(withTimeoutOrNull(50) { childEntered.await() })
            }
            nested.await()
            child.await()
            assertEquals(Unit, nestedEntered.await())
            assertEquals(Unit, childEntered.await())
        }
    }
}
