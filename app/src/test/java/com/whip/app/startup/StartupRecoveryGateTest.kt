package com.whip.app.startup

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.yield
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupRecoveryGateTest {
    @Test
    fun failedRecoveryBlocksEveryNormalStartupAction() = runBlocking {
        var repositoryInitializations = 0
        var defaultAreaCreations = 0
        var backgroundSchedules = 0
        val failure = IllegalStateException("corrupt recovery snapshot")
        val gate = StartupRecoveryGate(
            recoverPendingRestore = { throw failure },
            initializeNormalRuntime = {
                repositoryInitializations++
                defaultAreaCreations++
                backgroundSchedules++
            },
        )

        val result = gate.start()

        assertSame(failure, result)
        assertEquals(
            StartupRecoveryState.Blocked(StartupBlockReason.Recovery),
            gate.state.value,
        )
        assertEquals(0, repositoryInitializations)
        assertEquals(0, defaultAreaCreations)
        assertEquals(0, backgroundSchedules)
    }

    @Test
    fun retryStartsNormalRuntimeOnlyAfterRecoverySucceeds() = runBlocking {
        var recoveryShouldFail = true
        var recoveryAttempts = 0
        var normalRuntimeStarts = 0
        val gate = StartupRecoveryGate(
            recoverPendingRestore = {
                recoveryAttempts++
                if (recoveryShouldFail) error("injected rollback failure")
            },
            initializeNormalRuntime = { normalRuntimeStarts++ },
        )

        assertTrue(gate.start() is IllegalStateException)
        assertEquals(
            StartupRecoveryState.Blocked(StartupBlockReason.Recovery),
            gate.state.value,
        )
        assertEquals(0, normalRuntimeStarts)

        recoveryShouldFail = false
        assertNull(gate.retry())

        assertSame(StartupRecoveryState.Ready, gate.state.value)
        assertEquals(2, recoveryAttempts)
        assertEquals(1, normalRuntimeStarts)
        assertNull(gate.retry())
        assertEquals(1, normalRuntimeStarts)
    }

    @Test
    fun failedNormalRuntimeInitializationAlsoKeepsUiBlockedAndCanRetry() = runBlocking {
        var runtimeAttempts = 0
        val gate = StartupRecoveryGate(
            recoverPendingRestore = { },
            initializeNormalRuntime = {
                runtimeAttempts++
                if (runtimeAttempts == 1) error("injected scheduler initialization failure")
            },
        )

        assertTrue(gate.start() is IllegalStateException)
        assertEquals(
            StartupRecoveryState.Blocked(StartupBlockReason.RuntimeInitialization),
            gate.state.value,
        )

        assertNull(gate.retry())
        assertSame(StartupRecoveryState.Ready, gate.state.value)
        assertEquals(2, runtimeAttempts)
    }

    @Test
    fun failedLiveRestoreWithMarkerTransitionsReadyToBlockedAndRetryRecovers() = runBlocking {
        var pendingRecovery = false
        var recoveryAttempts = 0
        var runtimeStarts = 0
        var runtimeStops = 0
        val restoreFailure = IllegalStateException("target and rollback both failed")
        val gate = StartupRecoveryGate(
            recoverPendingRestore = {
                recoveryAttempts++
                pendingRecovery = false
            },
            initializeNormalRuntime = { runtimeStarts++ },
        )
        assertNull(gate.start())

        val result = runCatching {
            gate.runRestore(
                prepareForRestore = { runtimeStops++ },
                restore = {
                    pendingRecovery = true
                    throw restoreFailure
                },
                hasPendingRecovery = { pendingRecovery },
                resumeNormalRuntime = { runtimeStarts++ },
            )
        }

        assertEquals(restoreFailure::class, result.exceptionOrNull()?.let { it::class })
        assertEquals(restoreFailure.message, result.exceptionOrNull()?.message)
        assertEquals(StartupRecoveryState.Blocked(StartupBlockReason.Recovery), gate.state.value)
        assertEquals(1, runtimeStops)
        assertEquals(1, runtimeStarts)
        assertNull(gate.withReadyDataAccess { "must not run" })

        assertNull(gate.retry())
        assertSame(StartupRecoveryState.Ready, gate.state.value)
        assertEquals(2, recoveryAttempts)
        assertEquals(2, runtimeStarts)
    }

    @Test
    fun failedLiveRestoreWithoutMarkerRebuildsRuntimeAndReturnsReady() = runBlocking {
        var runtimeStops = 0
        val resumes = mutableListOf<Boolean>()
        val restoreFailure = IllegalArgumentException("target backup was rejected")
        val gate = StartupRecoveryGate(
            recoverPendingRestore = { },
            initializeNormalRuntime = { },
        )
        assertNull(gate.start())

        val result = runCatching {
            gate.runRestore(
                prepareForRestore = { runtimeStops++ },
                restore = { throw restoreFailure },
                hasPendingRecovery = { false },
                resumeNormalRuntime = { resumes += it },
            )
        }

        assertEquals(restoreFailure::class, result.exceptionOrNull()?.let { it::class })
        assertEquals(restoreFailure.message, result.exceptionOrNull()?.message)
        assertSame(StartupRecoveryState.Ready, gate.state.value)
        assertEquals(1, runtimeStops)
        assertEquals(listOf(false), resumes)
        assertEquals("available", gate.withReadyDataAccess { "available" })
    }

    @Test
    fun liveRestoreStopsNewAccessBeforeWaitingForInflightAccessToDrain() = runBlocking {
        val accessStarted = CompletableDeferred<Unit>()
        val releaseAccess = CompletableDeferred<Unit>()
        var restoreRan = false
        val gate = StartupRecoveryGate(
            recoverPendingRestore = { },
            initializeNormalRuntime = { },
        )
        assertNull(gate.start())
        val activeAccess = async {
            gate.withReadyDataAccess {
                accessStarted.complete(Unit)
                releaseAccess.await()
                "finished"
            }
        }
        accessStarted.await()

        val restore = async {
            gate.runRestore(
                prepareForRestore = { },
                restore = {
                    restoreRan = true
                    "restored"
                },
                hasPendingRecovery = { false },
                resumeNormalRuntime = { },
            )
        }
        while (gate.state.value != StartupRecoveryState.Checking) yield()

        assertNull(gate.withReadyDataAccess { "late access" })
        assertEquals(false, restoreRan)
        releaseAccess.complete(Unit)

        assertEquals("finished", activeAccess.await())
        assertEquals("restored", restore.await())
        assertSame(StartupRecoveryState.Ready, gate.state.value)
    }

    @Test
    fun exclusiveMaintenanceDrainsAdmittedMutationsRejectsLateAccessAndRebuildsBeforeReady() = runBlocking {
        val accessStarted = CompletableDeferred<Unit>()
        val releaseAccess = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()
        val gate = StartupRecoveryGate(recoverPendingRestore = { }, initializeNormalRuntime = { })
        assertNull(gate.start())
        val admittedMutation = async {
            gate.withReadyDataAccess {
                accessStarted.complete(Unit)
                releaseAccess.await()
                events += "mutation"
            }
        }
        accessStarted.await()

        val maintenance = async {
            gate.runExclusiveMaintenance(
                prepareForMaintenance = { events += "quiesce" },
                maintenance = { events += "reset" },
                resumeNormalRuntime = { rebuilt ->
                    assertEquals(false, rebuilt)
                    events += "rebuild"
                },
            )
        }
        while (gate.state.value != StartupRecoveryState.Checking) yield()
        assertNull(gate.withReadyDataAccess { "late mutation" })
        assertEquals(emptyList<String>(), events)

        releaseAccess.complete(Unit)
        admittedMutation.await()
        maintenance.await()

        assertEquals(listOf("mutation", "quiesce", "reset", "rebuild"), events)
        assertSame(StartupRecoveryState.Ready, gate.state.value)
    }

    @Test
    fun cancellingRestoreWhileAccessDrainsStillReachesAStableTerminalState() = runBlocking {
        val accessStarted = CompletableDeferred<Unit>()
        val releaseAccess = CompletableDeferred<Unit>()
        var restoreRan = false
        val gate = StartupRecoveryGate(recoverPendingRestore = { }, initializeNormalRuntime = { })
        assertNull(gate.start())
        val activeAccess = async {
            gate.withReadyDataAccess {
                accessStarted.complete(Unit)
                releaseAccess.await()
                Unit
            }
        }
        accessStarted.await()
        val restore = async {
            gate.runRestore(
                prepareForRestore = { },
                restore = { restoreRan = true },
                hasPendingRecovery = { false },
                resumeNormalRuntime = { },
            )
        }
        while (gate.state.value != StartupRecoveryState.Checking) yield()

        restore.cancel()
        assertSame(StartupRecoveryState.Checking, gate.state.value)
        releaseAccess.complete(Unit)
        activeAccess.await()
        runCatching { restore.await() }

        assertTrue(restoreRan)
        assertSame(StartupRecoveryState.Ready, gate.state.value)
        assertEquals(Unit, gate.withReadyDataAccess { Unit })
    }

    @Test
    fun synchronousSchedulerLeaseIsDrainedAndLateSchedulerLeaseIsRejected() = runBlocking {
        val accessStarted = CountDownLatch(1)
        val releaseAccess = CountDownLatch(1)
        var restoreRan = false
        val gate = StartupRecoveryGate(recoverPendingRestore = { }, initializeNormalRuntime = { })
        assertNull(gate.start())
        val activeAccess = async(Dispatchers.Default) {
            gate.tryWithReadyDataAccessNow {
                accessStarted.countDown()
                check(releaseAccess.await(5, TimeUnit.SECONDS))
                "scheduled"
            }
        }
        assertTrue(accessStarted.await(5, TimeUnit.SECONDS))
        val restore = async {
            gate.runRestore(
                prepareForRestore = { },
                restore = { restoreRan = true },
                hasPendingRecovery = { false },
                resumeNormalRuntime = { },
            )
        }
        while (gate.state.value != StartupRecoveryState.Checking) yield()

        assertNull(gate.tryWithReadyDataAccessNow { "late scheduler" })
        assertEquals(false, restoreRan)
        releaseAccess.countDown()

        assertEquals("scheduled", activeAccess.await())
        restore.await()
        assertTrue(restoreRan)
        assertSame(StartupRecoveryState.Ready, gate.state.value)
    }
}
