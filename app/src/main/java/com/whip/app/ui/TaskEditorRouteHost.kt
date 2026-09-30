package com.whip.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.whip.app.domain.AreaScope
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskEditBoundary
import java.time.LocalDate

// The app shell keeps the same editor state across routes; registration lives with its host.
internal class TaskEditorRouteState(
    val taskEditorOpen: MutableState<Boolean>,
    val taskEditorTaskId: MutableState<Long?>,
    val taskEditorSnapshot: MutableState<com.whip.app.domain.WhipTask?>,
    val taskEditorBoundary: MutableState<TaskEditBoundary?>,
    val taskEditorFromEpochDay: MutableState<Long?>,
    val taskEditorSaveAndNew: MutableState<Boolean>,
    val taskEditorCapture: MutableState<String>,
    val taskEditorCaptureShortened: MutableState<Boolean>,
    val taskEditorInitialScheduleEpochDay: MutableState<Long?>,
    val taskEditorInitialPlacement: MutableState<TaskPlacement?>,
    val taskEditorInitialAreaResolved: MutableState<Boolean>,
    val taskEditorInitialAreaId: MutableState<String?>,
    val taskEditorSessionId: MutableState<Long>,
)

@Composable
internal fun rememberTaskEditorRouteState(): TaskEditorRouteState {
    val taskEditorOpen = rememberSaveable { mutableStateOf(false) }
    val taskEditorTaskId = rememberSaveable { mutableStateOf<Long?>(null) }
    val taskEditorSnapshot = remember { mutableStateOf<com.whip.app.domain.WhipTask?>(null) }
    val taskEditorBoundary = rememberSaveable { mutableStateOf<TaskEditBoundary?>(null) }
    val taskEditorFromEpochDay = rememberSaveable { mutableStateOf<Long?>(null) }
    val taskEditorSaveAndNew = rememberSaveable { mutableStateOf(false) }
    val taskEditorCapture = rememberSaveable { mutableStateOf("") }
    val taskEditorCaptureShortened = rememberSaveable { mutableStateOf(false) }
    val taskEditorInitialScheduleEpochDay = rememberSaveable { mutableStateOf<Long?>(null) }
    val taskEditorInitialPlacement = rememberSaveable { mutableStateOf<TaskPlacement?>(null) }
    val taskEditorInitialAreaResolved = rememberSaveable { mutableStateOf(false) }
    val taskEditorInitialAreaId = rememberSaveable { mutableStateOf<String?>(null) }
    val taskEditorSessionId = rememberSaveable { mutableLongStateOf(0L) }
    return remember(
        taskEditorOpen,
        taskEditorTaskId,
        taskEditorSnapshot,
        taskEditorBoundary,
        taskEditorFromEpochDay,
        taskEditorSaveAndNew,
        taskEditorCapture,
        taskEditorCaptureShortened,
        taskEditorInitialScheduleEpochDay,
        taskEditorInitialPlacement,
        taskEditorInitialAreaResolved,
        taskEditorInitialAreaId,
        taskEditorSessionId,
    ) {
        TaskEditorRouteState(
            taskEditorOpen,
            taskEditorTaskId,
            taskEditorSnapshot,
            taskEditorBoundary,
            taskEditorFromEpochDay,
            taskEditorSaveAndNew,
            taskEditorCapture,
            taskEditorCaptureShortened,
            taskEditorInitialScheduleEpochDay,
            taskEditorInitialPlacement,
            taskEditorInitialAreaResolved,
            taskEditorInitialAreaId,
            taskEditorSessionId,
        )
    }
}

@Composable
internal fun TaskEditorRouteHost(
    request: TaskEditorRequest?,
    taskState: TaskUiState,
    settingsState: SettingsUiState,
    settingsViewModel: SettingsViewModel?,
    areaScope: AreaScope,
    saveCoordinator: EntitySaveCoordinator,
    pendingTaskEditorLaunchState: PendingTaskEditorLaunchState,
    launchQueueOverflowState: LaunchQueueOverflowState,
    paneOffsetX: Dp,
    paneMaxWidth: Dp,
    pendingDialogModifier: Modifier,
    onDismiss: () -> Unit,
    onSaveAndNewIntentChange: (Boolean) -> Unit,
    onSaveTaskRequest: (Long?, TaskEditBoundary?, TaskDraft, LocalDate?, String) -> Boolean,
    onRequestNotificationPermission: () -> Unit,
    onReplacePendingTaskRequest: (String, Boolean, LocalDate?, String?) -> Unit,
) {
    request?.let { editorRequest ->
        TaskEditorDialog(
            request = editorRequest,
            onDismiss = {
                val pendingCapture = pendingTaskEditorLaunchState.text
                val pendingShortened = pendingTaskEditorLaunchState.shortened
                val pendingScheduleDate = pendingTaskEditorLaunchState.scheduleEpochDay
                    ?.let(LocalDate::ofEpochDay)
                val pendingAreaId = pendingTaskEditorLaunchState.resolvedAreaId
                if (pendingCapture != null) pendingTaskEditorLaunchState.clear()
                onDismiss()
                if (pendingCapture != null) {
                    onReplacePendingTaskRequest(
                        pendingCapture,
                        pendingShortened,
                        pendingScheduleDate,
                        pendingAreaId,
                    )
                }
            },
            onSave = { taskId, draft, fromOccurrence ->
                val requestId = saveCoordinator.begin() ?: return@TaskEditorDialog
                onSaveAndNewIntentChange(false)
                if (!onSaveTaskRequest(taskId, editorRequest.expectedBoundary, draft, fromOccurrence, requestId)) {
                    saveCoordinator.finishFailure("Another Task save is already finishing.")
                }
            },
            onSaveAndNew = { taskId, draft, fromOccurrence ->
                val requestId = saveCoordinator.begin() ?: return@TaskEditorDialog
                onSaveAndNewIntentChange(true)
                if (!onSaveTaskRequest(taskId, editorRequest.expectedBoundary, draft, fromOccurrence, requestId)) {
                    onSaveAndNewIntentChange(false)
                    saveCoordinator.finishFailure("Another Task save is already finishing.")
                }
            },
            onRequestNotificationPermission = onRequestNotificationPermission,
            defaultRepeatStepPolicy = settingsState.settings.defaultTaskStepPolicy,
            firstDayOfWeek = settingsState.settings.firstDayOfWeek,
            today = taskState.currentDate,
            naturalLanguageCapture = settingsState.settings.naturalLanguageTaskCapture,
            powerMode = settingsState.settings.powerMode,
            areas = settingsState.areas,
            defaultAreaId = if (editorRequest.initialAreaResolved) {
                editorRequest.initialAreaId
            } else {
                areaScope.creationDefaultAreaId(settingsState.areas)
            },
            inheritedAreaFromScope = areaScope is AreaScope.One,
            onCreateArea = { name, color, result -> settingsViewModel?.createArea(name, color, result) },
            knownTags = settingsState.tags.filterNot { it.archived }.map { it.name },
            customIdentityEmojis = settingsState.settings.customIdentityEmojis,
            onSaveIdentityEmoji = identityEmojiSaveActions(settingsViewModel),
            onRemoveSavedIdentityEmoji = { settingsViewModel?.removeCustomIdentityEmoji(it) },
            paneOffsetX = paneOffsetX,
            paneMaxWidth = paneMaxWidth,
            saving = saveCoordinator.saving,
            persistenceError = saveCoordinator.errorMessage,
            pendingTaskRequestWaiting =
                pendingTaskEditorLaunchState.text != null && !pendingTaskEditorLaunchState.dialogVisible,
        )
    }
    PendingTaskEditorLaunchDialog(
        visible = pendingTaskEditorLaunchState.text != null && pendingTaskEditorLaunchState.dialogVisible,
        saving = saveCoordinator.saving,
        modifier = pendingDialogModifier,
        onReplace = {
            val capture = pendingTaskEditorLaunchState.text ?: return@PendingTaskEditorLaunchDialog
            val shortened = pendingTaskEditorLaunchState.shortened
            val scheduleDate = pendingTaskEditorLaunchState.scheduleEpochDay?.let(LocalDate::ofEpochDay)
            val areaId = pendingTaskEditorLaunchState.resolvedAreaId
            pendingTaskEditorLaunchState.clear()
            onReplacePendingTaskRequest(capture, shortened, scheduleDate, areaId)
        },
        onKeepEditing = pendingTaskEditorLaunchState::defer,
    )
    LaunchQueueOverflowDialog(
        state = launchQueueOverflowState,
        modifier = pendingDialogModifier,
    )
}
