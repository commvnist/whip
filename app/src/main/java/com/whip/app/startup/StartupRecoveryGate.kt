package com.whip.app.startup

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Prevents the normal application runtime from starting until any pending
 * restore recovery has completed successfully.
 */
class StartupRecoveryGate(
    private val recoverPendingRestore: suspend () -> Unit,
    private val initializeNormalRuntime: suspend () -> Unit,
) {
    private val attemptMutex = Mutex()
    private val accessMonitor = Any()
    private var activeDataAccesses = 0
    private var accessesDrained = CompletableDeferred(Unit)
    private val mutableState = MutableStateFlow<StartupRecoveryState>(StartupRecoveryState.Checking)
    private var initialAttemptStarted = false

    val state: StateFlow<StartupRecoveryState> = mutableState.asStateFlow()

    suspend fun start(): Throwable? = attemptMutex.withLock {
        if (initialAttemptStarted) return@withLock null
        initialAttemptStarted = true
        attemptRecovery()
    }

    suspend fun retry(): Throwable? {
        if (mutableState.value !is StartupRecoveryState.Blocked) return null
        return attemptMutex.withLock {
            if (mutableState.value !is StartupRecoveryState.Blocked) return@withLock null
            attemptRecovery()
        }
    }

    /**
     * Serializes background access with restore/recovery. Callers that arrive
     * while startup or a restore is unresolved must defer without touching
     * repositories, settings, files, notifications, or widgets.
     */
    suspend fun <T : Any> withReadyDataAccess(
        additionalCheck: () -> Boolean = { true },
        block: suspend () -> T,
    ): T? {
        if (!beginDataAccess(additionalCheck)) return null
        return try {
            block()
        } finally {
            endDataAccess()
        }
    }

    /** A short synchronous lease for WorkManager enqueue/cancel operations. */
    fun <T : Any> tryWithReadyDataAccessNow(
        additionalCheck: () -> Boolean = { true },
        block: () -> T,
    ): T? {
        if (!beginDataAccess(additionalCheck)) return null
        return try {
            block()
        } finally {
            endDataAccess()
        }
    }

    /**
     * Runs an in-process replace restore under the same exclusive boundary as
     * startup recovery. Normal runtime is hidden and quiesced before the
     * recovery snapshot can be created.
     */
    suspend fun <T> runRestore(
        prepareForRestore: suspend () -> Unit,
        restore: suspend () -> T,
        hasPendingRecovery: () -> Boolean,
        resumeNormalRuntime: suspend (backgroundAlreadyRebuilt: Boolean) -> Unit,
        backgroundAlreadyRebuiltOnSuccess: Boolean = true,
    ): T = attemptMutex.withLock {
        withContext(NonCancellable) {
            check(mutableState.value == StartupRecoveryState.Ready) {
                "Whip data is not available while recovery is unresolved"
            }
            closeAccessAndAwaitDrain()
            try {
                prepareForRestore()
            } catch (error: Throwable) {
                return@withContext resumeAfterFailedRestore(
                    restoreError = error,
                    hasPendingRecovery = hasPendingRecovery,
                    resumeNormalRuntime = resumeNormalRuntime,
                )
            }

            val result = try {
                restore()
            } catch (error: Throwable) {
                return@withContext resumeAfterFailedRestore(
                    restoreError = error,
                    hasPendingRecovery = hasPendingRecovery,
                    resumeNormalRuntime = resumeNormalRuntime,
                )
            }

            try {
                resumeNormalRuntime(backgroundAlreadyRebuiltOnSuccess)
                mutableState.value = StartupRecoveryState.Ready
                result
            } catch (error: Throwable) {
                mutableState.value = StartupRecoveryState.Blocked(StartupBlockReason.RuntimeInitialization)
                throw error
            }
        }
    }

    /**
     * Runs a destructive whole-app operation only after every admitted data
     * lease has drained. Unlike replace-restore, maintenance has no rollback
     * marker, so a failure resumes whatever durable state the operation left.
     */
    suspend fun <T> runExclusiveMaintenance(
        prepareForMaintenance: suspend () -> Unit,
        maintenance: suspend () -> T,
        resumeNormalRuntime: suspend (backgroundAlreadyRebuilt: Boolean) -> Unit,
    ): T = runRestore(
        prepareForRestore = prepareForMaintenance,
        restore = maintenance,
        hasPendingRecovery = { false },
        resumeNormalRuntime = resumeNormalRuntime,
        backgroundAlreadyRebuiltOnSuccess = false,
    )

    /** Moves a running process behind the gate if a persistent marker is found. */
    suspend fun blockForPendingRecovery(
        hasPendingRecovery: () -> Boolean,
        prepareForRecovery: suspend () -> Unit,
    ) = attemptMutex.withLock {
        if (!hasPendingRecovery()) return@withLock
        withContext(NonCancellable) {
            closeAccessAndAwaitDrain()
            try {
                prepareForRecovery()
            } finally {
                mutableState.value = StartupRecoveryState.Blocked(StartupBlockReason.Recovery)
            }
        }
    }

    private suspend fun attemptRecovery(): Throwable? {
        mutableState.value = StartupRecoveryState.Checking
        try {
            recoverPendingRestore()
        } catch (error: Throwable) {
            mutableState.value = StartupRecoveryState.Blocked(StartupBlockReason.Recovery)
            return error
        }
        return try {
            initializeNormalRuntime()
            mutableState.value = StartupRecoveryState.Ready
            null
        } catch (error: Throwable) {
            mutableState.value = StartupRecoveryState.Blocked(StartupBlockReason.RuntimeInitialization)
            error
        }
    }

    private suspend fun resumeAfterFailedRestore(
        restoreError: Throwable,
        hasPendingRecovery: () -> Boolean,
        resumeNormalRuntime: suspend (backgroundAlreadyRebuilt: Boolean) -> Unit,
    ): Nothing {
        if (hasPendingRecovery()) {
            mutableState.value = StartupRecoveryState.Blocked(StartupBlockReason.Recovery)
            throw restoreError
        }
        try {
            // The failure may have happened before restore began or after a
            // successful rollback. A full idempotent rebuild is safest.
            resumeNormalRuntime(false)
            mutableState.value = StartupRecoveryState.Ready
        } catch (runtimeError: Throwable) {
            restoreError.addSuppressed(runtimeError)
            mutableState.value = StartupRecoveryState.Blocked(StartupBlockReason.RuntimeInitialization)
        }
        throw restoreError
    }

    private fun beginDataAccess(additionalCheck: () -> Boolean): Boolean = synchronized(accessMonitor) {
        if (mutableState.value != StartupRecoveryState.Ready || !additionalCheck()) return@synchronized false
        if (activeDataAccesses == 0) accessesDrained = CompletableDeferred()
        activeDataAccesses++
        true
    }

    private fun endDataAccess() = synchronized(accessMonitor) {
        check(activeDataAccesses > 0) { "Unbalanced Whip data-access lease" }
        activeDataAccesses--
        if (activeDataAccesses == 0) accessesDrained.complete(Unit)
    }

    private suspend fun closeAccessAndAwaitDrain() {
        val drain = synchronized(accessMonitor) {
            mutableState.value = StartupRecoveryState.Checking
            accessesDrained
        }
        drain.await()
    }
}

sealed interface StartupRecoveryState {
    data object Checking : StartupRecoveryState
    data object Ready : StartupRecoveryState
    data object FreshStartChecking : StartupRecoveryState
    data class FreshStartCheckBlocked(val detail: String?) : StartupRecoveryState
    data object FreshStartRequired : StartupRecoveryState
    data object FreshStartResetting : StartupRecoveryState
    data class FreshStartBlocked(val detail: String?) : StartupRecoveryState
    data class Blocked(val reason: StartupBlockReason) : StartupRecoveryState
}

internal enum class FreshStartRetryAction {
    ReevaluateEpoch,
    ResumeConfirmedReset,
    RetryStartupRecovery,
}

internal fun StartupRecoveryState.freshStartRetryAction(): FreshStartRetryAction = when (this) {
    is StartupRecoveryState.FreshStartCheckBlocked -> FreshStartRetryAction.ReevaluateEpoch
    is StartupRecoveryState.FreshStartBlocked -> FreshStartRetryAction.ResumeConfirmedReset
    else -> FreshStartRetryAction.RetryStartupRecovery
}

enum class StartupBlockReason {
    Recovery,
    RuntimeInitialization,
}
