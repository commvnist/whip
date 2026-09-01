package com.whip.app.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.whip.app.core.OperationFeedbackPresentation
import com.whip.app.core.OperationStatus

internal data class TaskSnackbarVisuals(
    override val message: String,
    override val actionLabel: String?,
    override val withDismissAction: Boolean,
    override val duration: SnackbarDuration,
    val undoToken: Long?,
    val quickAdd: Boolean,
) : SnackbarVisuals

@Composable
internal fun WhipOperationFeedbackHost(
    taskStatus: OperationStatus,
    taskUndoMessage: String?,
    taskUndoToken: Long?,
    quickAddedTaskId: Long?,
    onTaskStatusConsumed: () -> Unit,
    onEditQuickAddedTask: (Long) -> Unit,
    onTaskUndo: (Long) -> Unit,
    onTaskUndoDismissed: (Long) -> Unit,
    gymStatus: OperationStatus,
    machineArchiveUndoToken: Long?,
    onGymStatusConsumed: () -> Unit,
    onMachineArchiveUndo: (Long) -> Unit,
    onMachineArchiveUndoDismissed: (Long) -> Unit,
    habitStatus: OperationStatus,
    onHabitStatusConsumed: () -> Unit,
    goalStatus: OperationStatus,
    onGoalStatusConsumed: () -> Unit,
    trackStatus: OperationStatus,
    onTrackStatusConsumed: () -> Unit,
    trackEntryUndoState: TrackEntryUndoUiState,
    trackEntryUndoToken: Long?,
    onTrackEntryUndo: (Long) -> Unit,
    onTrackEntryUndoDismissed: (Long) -> Unit,
    onTrackEntryUndoStatusConsumed: (Long) -> Unit,
    snackbarHostState: SnackbarHostState,
    presentFeedback: TrackTransientFeedbackPresenter,
    invalidateFeedback: (source: String, preserveRecoveries: Boolean) -> Unit,
    invalidateRecovery: (String) -> Unit,
) {
    TaskOperationFeedbackEffect(
        status = taskStatus,
        undoMessage = taskUndoMessage,
        undoToken = taskUndoToken,
        quickAddedTaskId = quickAddedTaskId,
        snackbarHostState = snackbarHostState,
        onStatusConsumed = onTaskStatusConsumed,
        onEditQuickAddedTask = onEditQuickAddedTask,
        onUndo = onTaskUndo,
        onUndoDismissed = onTaskUndoDismissed,
        presentFeedback = presentFeedback,
        invalidateFeedback = invalidateFeedback,
    )
    GymOperationFeedbackEffect(
        status = gymStatus,
        pendingArchiveUndoToken = machineArchiveUndoToken,
        snackbarHostState = snackbarHostState,
        onStatusConsumed = onGymStatusConsumed,
        onArchiveUndo = onMachineArchiveUndo,
        onArchiveUndoDismissed = onMachineArchiveUndoDismissed,
        presentFeedback = presentFeedback,
        invalidateFeedback = invalidateFeedback,
        invalidateRecovery = invalidateRecovery,
    )
    SimpleOperationFeedbackEffect(
        source = "habits",
        status = habitStatus,
        snackbarHostState = snackbarHostState,
        onStatusConsumed = onHabitStatusConsumed,
        presentFeedback = presentFeedback,
        invalidateFeedback = invalidateFeedback,
    )
    SimpleOperationFeedbackEffect(
        source = "goals",
        status = goalStatus,
        snackbarHostState = snackbarHostState,
        onStatusConsumed = onGoalStatusConsumed,
        presentFeedback = presentFeedback,
        invalidateFeedback = invalidateFeedback,
    )
    TrackOperationFeedbackEffect(
        status = trackStatus,
        pendingEntryUndoToken = trackEntryUndoToken,
        snackbarHostState = snackbarHostState,
        onStatusConsumed = onTrackStatusConsumed,
        onEntryUndo = onTrackEntryUndo,
        onEntryUndoDismissed = onTrackEntryUndoDismissed,
        presentFeedback = presentFeedback,
        invalidateFeedback = invalidateFeedback,
    )
    TrackEntryUndoFeedbackEffects(
        state = trackEntryUndoState,
        pendingUndoToken = trackEntryUndoToken,
        snackbarHostState = snackbarHostState,
        onUndo = onTrackEntryUndo,
        onUndoDismissed = onTrackEntryUndoDismissed,
        onStatusConsumed = onTrackEntryUndoStatusConsumed,
        presentFeedback = presentFeedback,
        invalidateRecovery = invalidateRecovery,
    )
}

@Composable
internal fun TaskOperationFeedbackEffect(
    status: OperationStatus,
    undoMessage: String?,
    undoToken: Long?,
    quickAddedTaskId: Long?,
    snackbarHostState: SnackbarHostState,
    onStatusConsumed: () -> Unit,
    onEditQuickAddedTask: (Long) -> Unit,
    onUndo: (Long) -> Unit,
    onUndoDismissed: (Long) -> Unit,
    presentFeedback: TrackTransientFeedbackPresenter,
    invalidateFeedback: (source: String, preserveRecoveries: Boolean) -> Unit,
) {
    LaunchedEffect(status) {
        if (
            status is OperationStatus.Running ||
            (status is OperationStatus.Succeeded &&
                status.feedbackPresentation == OperationFeedbackPresentation.Inline)
        ) {
            invalidateFeedback("tasks", false)
        }
        status.deliverTransientMessage(onStatusConsumed) { message ->
            val succeeded = status is OperationStatus.Succeeded
            presentFeedback(
                "tasks",
                when {
                    status is OperationStatus.Failed -> 3
                    undoMessage != null -> 2
                    else -> 1
                },
                undoToken != null,
            ) {
                try {
                    val result = snackbarHostState.showSnackbar(
                        TaskSnackbarVisuals(
                            message = message,
                            actionLabel = when {
                                !succeeded || undoMessage == null -> null
                                quickAddedTaskId != null -> "Edit"
                                else -> "Undo"
                            },
                            withDismissAction = undoToken != null || !succeeded,
                            duration = if (succeeded) SnackbarDuration.Long else SnackbarDuration.Indefinite,
                            undoToken = undoToken,
                            quickAdd = quickAddedTaskId != null,
                        ),
                    )
                    if (undoToken == null) return@presentFeedback
                    when {
                        quickAddedTaskId != null && result == SnackbarResult.ActionPerformed -> {
                            onEditQuickAddedTask(quickAddedTaskId)
                            onUndoDismissed(undoToken)
                        }
                        quickAddedTaskId == null && result == SnackbarResult.ActionPerformed -> onUndo(undoToken)
                        else -> Unit
                    }
                } finally {
                    undoToken?.let(onUndoDismissed)
                }
            }
        }
    }
}

@Composable
internal fun SimpleOperationFeedbackEffect(
    source: String,
    status: OperationStatus,
    snackbarHostState: SnackbarHostState,
    onStatusConsumed: () -> Unit,
    presentFeedback: TrackTransientFeedbackPresenter,
    invalidateFeedback: (source: String, preserveRecoveries: Boolean) -> Unit,
) {
    LaunchedEffect(source, status) {
        if (
            status is OperationStatus.Running ||
            (status is OperationStatus.Succeeded &&
                status.feedbackPresentation == OperationFeedbackPresentation.Inline)
        ) {
            invalidateFeedback(source, false)
        }
        status.deliverTransientMessage(onStatusConsumed) { message ->
            presentFeedback(source, if (status is OperationStatus.Failed) 3 else 1, false) {
                snackbarHostState.showSnackbar(
                    message = message,
                    withDismissAction = status is OperationStatus.Failed,
                    duration = if (status is OperationStatus.Failed) {
                        SnackbarDuration.Indefinite
                    } else {
                        SnackbarDuration.Long
                    },
                )
            }
        }
    }
}

@Composable
internal fun GymOperationFeedbackEffect(
    status: OperationStatus,
    pendingArchiveUndoToken: Long?,
    snackbarHostState: SnackbarHostState,
    onStatusConsumed: () -> Unit,
    onArchiveUndo: (Long) -> Unit,
    onArchiveUndoDismissed: (Long) -> Unit,
    presentFeedback: TrackTransientFeedbackPresenter,
    invalidateFeedback: (source: String, preserveRecoveries: Boolean) -> Unit,
    invalidateRecovery: (String) -> Unit,
) {
    LaunchedEffect(status) {
        if (
            status is OperationStatus.Running ||
            (status is OperationStatus.Succeeded &&
                status.feedbackPresentation == OperationFeedbackPresentation.Inline)
        ) {
            invalidateFeedback("gym", true)
        }
        status.deliverTransientMessage(onStatusConsumed) { message ->
            val archiveUndoToken = (status as? OperationStatus.Succeeded)
                ?.recoveryToken
                ?.takeIf { it == pendingArchiveUndoToken }
            val undoAvailable = archiveUndoToken != null
            presentFeedback(
                "gym",
                when {
                    status is OperationStatus.Failed -> 3
                    undoAvailable -> 2
                    else -> 1
                },
                undoAvailable,
            ) {
                try {
                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = "Undo".takeIf { undoAvailable },
                        withDismissAction = undoAvailable || status is OperationStatus.Failed,
                        duration = if (status is OperationStatus.Failed) {
                            SnackbarDuration.Indefinite
                        } else {
                            SnackbarDuration.Long
                        },
                    )
                    if (result == SnackbarResult.ActionPerformed && archiveUndoToken != null) {
                        onArchiveUndo(archiveUndoToken)
                    }
                } finally {
                    archiveUndoToken?.let(onArchiveUndoDismissed)
                }
            }
        }
    }
    LaunchedEffect(pendingArchiveUndoToken) {
        if (pendingArchiveUndoToken == null) invalidateRecovery("gym")
    }
}
