package com.whip.app.ui

import androidx.compose.material3.SnackbarHostState
import com.whip.app.core.OperationFeedbackPresentation
import com.whip.app.core.OperationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private data class TransientFeedbackRequest(
    val source: String,
    val priority: Int,
    val recoverable: Boolean,
    val show: suspend () -> Unit,
)

/** Owns Snackbar arbitration without inflating the root Compose method. */
internal class TransientFeedbackCoordinator(
    private val snackbarHostState: SnackbarHostState,
    private val scope: CoroutineScope,
) {
    private var job: Job? = null
    private var active: TransientFeedbackRequest? = null
    private var generation = 0L
    private val pending = ArrayDeque<TransientFeedbackRequest>()

    private fun start(request: TransientFeedbackRequest) {
        val requestGeneration = ++generation
        active = request
        job = scope.launch {
            try {
                request.show()
            } finally {
                if (requestGeneration == generation) {
                    job = null
                    active = null
                    pending.removeFirstOrNull()?.let(::start)
                }
            }
        }
    }

    fun present(
        source: String,
        priority: Int,
        recoverable: Boolean = false,
        show: suspend () -> Unit,
    ) {
        val request = TransientFeedbackRequest(source, priority, recoverable, show)
        val current = active
        if (job?.isActive != true || current == null) {
            start(request)
            return
        }
        when {
            recoverable && current.recoverable && source == current.source -> {
                replaceActive(request)
            }
            current.recoverable -> {
                if (priority >= 2) {
                    pending.removeAll { it.source == source && it.recoverable == recoverable }
                    pending.addLast(request)
                }
            }
            recoverable -> replaceActive(request)
            current.priority >= 3 -> if (priority >= 3) pending.addLast(request)
            priority >= 3 -> replaceActive(request)
            else -> replaceActive(request)
        }
    }

    fun invalidate(source: String, preserveRecoveries: Boolean = false) {
        pending.removeAll { it.source == source && (!preserveRecoveries || !it.recoverable) }
        val current = active
        if (current?.source != source || (preserveRecoveries && current.recoverable)) return
        cancelActive()
    }

    fun invalidateRecovery(source: String) {
        pending.removeAll { it.source == source && it.recoverable }
        val current = active
        if (current?.source != source || !current.recoverable) return
        cancelActive()
    }

    fun onDestinationChanged() {
        pending.removeAll { !transientFeedbackSurvivesDestinationChange(it.recoverable) }
        if (active?.let { transientFeedbackSurvivesDestinationChange(it.recoverable) } == true) return
        cancelActive()
    }

    private fun replaceActive(request: TransientFeedbackRequest) {
        generation++
        job?.cancel()
        snackbarHostState.currentSnackbarData?.dismiss()
        job = null
        active = null
        start(request)
    }

    private fun cancelActive() {
        generation++
        job?.cancel()
        job = null
        active = null
        snackbarHostState.currentSnackbarData?.dismiss()
        pending.removeFirstOrNull()?.let(::start)
    }
}

/**
 * Claims a terminal operation exactly once before entering the suspending UI
 * that displays it. Navigation may cancel [show], but can no longer replay the
 * same message and restart its timeout on the next page.
 */
internal suspend fun <T> OperationStatus.deliverTransientMessage(
    consume: () -> Unit,
    show: suspend (String) -> T,
): T? {
    val message = when (this) {
        OperationStatus.Idle, is OperationStatus.Running -> return null
        is OperationStatus.Succeeded -> {
            if (feedbackPresentation == OperationFeedbackPresentation.Inline) {
                consume()
                return null
            }
            message
        }
        is OperationStatus.Failed -> message
    }
    consume()
    return show(message)
}
