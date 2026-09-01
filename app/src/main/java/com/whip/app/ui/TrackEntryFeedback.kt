package com.whip.app.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.whip.app.core.OperationFeedbackPresentation
import com.whip.app.core.OperationStatus
import com.whip.app.domain.TrackEntryMutationKind
import com.whip.app.domain.TrackEntryMutationReceipt

internal typealias TrackTransientFeedbackPresenter = (
    source: String,
    priority: Int,
    recoverable: Boolean,
    show: suspend () -> Unit,
) -> Unit

internal fun presentTrackEntryMutationFeedback(
    receipt: TrackEntryMutationReceipt,
    undoToken: Long?,
    snackbarHostState: SnackbarHostState,
    onUndo: (Long) -> Unit,
    onUndoDismissed: (Long) -> Unit,
    presentFeedback: TrackTransientFeedbackPresenter,
) {
    val message = buildString {
        append(
            when (receipt.kind) {
                TrackEntryMutationKind.Create -> if (receipt.alreadyApplied) "Entry was already added" else "Entry added"
                TrackEntryMutationKind.Update -> if (receipt.alreadyApplied) "Entry was already saved" else "Entry saved"
                TrackEntryMutationKind.Delete -> if (receipt.alreadyApplied) "Entry was already deleted" else "Entry deleted"
                TrackEntryMutationKind.Restore -> "Entry restored"
            },
        )
        if (receipt.warnings.isNotEmpty()) append(" · ${receipt.warnings.joinToString(" ")}")
    }
    presentFeedback(
        if (undoToken != null) "track-entry-delete" else "track-entry-${receipt.entryUuid}",
        if (undoToken != null) 2 else 1,
        undoToken != null,
    ) {
        var undoStarted = false
        try {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Undo".takeIf { undoToken != null },
                withDismissAction = undoToken != null,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed && undoToken != null) {
                undoStarted = true
                onUndo(undoToken)
            }
        } finally {
            if (!undoStarted) undoToken?.let(onUndoDismissed)
        }
    }
}

@Composable
internal fun TrackOperationFeedbackEffect(
    status: OperationStatus,
    pendingEntryUndoToken: Long?,
    snackbarHostState: SnackbarHostState,
    onStatusConsumed: () -> Unit,
    onEntryUndo: (Long) -> Unit,
    onEntryUndoDismissed: (Long) -> Unit,
    presentFeedback: TrackTransientFeedbackPresenter,
    invalidateFeedback: (source: String, preserveRecoveries: Boolean) -> Unit,
) {
    LaunchedEffect(status) {
        if (
            status is OperationStatus.Running ||
            (status is OperationStatus.Succeeded &&
                status.feedbackPresentation == OperationFeedbackPresentation.Inline)
        ) {
            invalidateFeedback("tracks", true)
        }
        status.deliverTransientMessage(onStatusConsumed) { message ->
            val undoToken = (status as? OperationStatus.Succeeded)
                ?.recoveryToken
                ?.takeIf { it == pendingEntryUndoToken }
            val undoAvailable = undoToken != null
            presentFeedback(
                "tracks",
                when {
                    status is OperationStatus.Failed -> 3
                    undoAvailable -> 2
                    else -> 1
                },
                undoAvailable,
            ) {
                try {
                    val result = snackbarHostState.showSnackbar(
                        message,
                        actionLabel = "Undo".takeIf { undoAvailable },
                        withDismissAction = undoAvailable || status is OperationStatus.Failed,
                        duration = if (status is OperationStatus.Failed) {
                            SnackbarDuration.Indefinite
                        } else {
                            SnackbarDuration.Long
                        },
                    )
                    if (result == SnackbarResult.ActionPerformed && undoToken != null) {
                        onEntryUndo(undoToken)
                    }
                } finally {
                    undoToken?.let(onEntryUndoDismissed)
                }
            }
        }
    }
}

@Composable
internal fun TrackEntryUndoFeedbackEffects(
    state: TrackEntryUndoUiState,
    pendingUndoToken: Long?,
    snackbarHostState: SnackbarHostState,
    onUndo: (Long) -> Unit,
    onUndoDismissed: (Long) -> Unit,
    onStatusConsumed: (Long) -> Unit,
    presentFeedback: TrackTransientFeedbackPresenter,
    invalidateRecovery: (String) -> Unit,
) {
    LaunchedEffect(pendingUndoToken) {
        if (pendingUndoToken == null) invalidateRecovery("track-entry-delete")
    }
    LaunchedEffect(state, pendingUndoToken) {
        val statusToken = state.token
        when (val status = state.status) {
            OperationStatus.Idle, is OperationStatus.Running -> Unit
            is OperationStatus.Succeeded -> {
                presentFeedback("track-entry-undo-success", 1, false) {
                    snackbarHostState.showSnackbar(status.message, duration = SnackbarDuration.Long)
                }
                statusToken?.let(onStatusConsumed)
            }
            is OperationStatus.Failed -> {
                val retryToken = trackEntryUndoRetryToken(state)
                presentFeedback("track-entry-delete", 3, retryToken != null) {
                    var retryStarted = false
                    try {
                        val result = snackbarHostState.showSnackbar(
                            message = if (retryToken != null) {
                                "Undo didn't finish. ${status.message}"
                            } else {
                                status.message
                            },
                            actionLabel = "Retry".takeIf { retryToken != null },
                            withDismissAction = true,
                            duration = SnackbarDuration.Indefinite,
                        )
                        if (result == SnackbarResult.ActionPerformed && retryToken != null) {
                            retryStarted = true
                            onUndo(retryToken)
                        }
                    } finally {
                        if (!retryStarted) {
                            retryToken?.let(onUndoDismissed)
                            statusToken?.let(onStatusConsumed)
                        }
                    }
                }
            }
        }
    }
}
