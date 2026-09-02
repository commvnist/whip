package com.whip.app.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Stable
internal class PendingTaskEditorLaunchState(
    initialText: String? = null,
    initialShortened: Boolean = false,
    initialScheduleEpochDay: Long? = null,
    initialResolvedAreaId: String? = null,
) {
    var text by mutableStateOf(initialText)
        private set
    var shortened by mutableStateOf(initialShortened)
        private set
    var scheduleEpochDay by mutableStateOf(initialScheduleEpochDay)
        private set
    var resolvedAreaId by mutableStateOf(initialResolvedAreaId)
        private set
    var dialogVisible by mutableStateOf(initialText != null)
        private set
    var revision by mutableStateOf(0L)
        private set

    fun queue(
        text: String,
        shortened: Boolean,
        scheduleEpochDay: Long? = null,
        resolvedAreaId: String?,
    ): Boolean {
        if (this.text != null) return false
        this.text = text
        this.shortened = shortened
        this.scheduleEpochDay = scheduleEpochDay
        this.resolvedAreaId = resolvedAreaId
        dialogVisible = true
        revision++
        return true
    }

    fun defer() {
        if (text == null) return
        dialogVisible = false
        revision++
    }

    fun clear() {
        text = null
        shortened = false
        scheduleEpochDay = null
        resolvedAreaId = null
        dialogVisible = false
        revision++
    }

    companion object {
        val Saver = listSaver<PendingTaskEditorLaunchState, Any>(
            save = { state ->
                listOf(
                    state.text != null,
                    state.text.orEmpty(),
                    state.shortened,
                    state.scheduleEpochDay != null,
                    state.scheduleEpochDay ?: 0L,
                    state.resolvedAreaId != null,
                    state.resolvedAreaId.orEmpty(),
                    state.dialogVisible,
                    state.revision,
                )
            },
            restore = { values ->
                PendingTaskEditorLaunchState(
                    initialText = (values[1] as String).takeIf { values[0] as Boolean },
                    initialShortened = values[2] as Boolean,
                    initialScheduleEpochDay = (values[4] as Long).takeIf { values[3] as Boolean },
                    initialResolvedAreaId = (values[6] as String).takeIf { values[5] as Boolean },
                ).also { state ->
                    state.dialogVisible = values[7] as Boolean
                    state.revision = values[8] as Long
                }
            },
        )
    }
}

@Composable
internal fun rememberPendingTaskEditorLaunchState(): PendingTaskEditorLaunchState =
    rememberSaveable(saver = PendingTaskEditorLaunchState.Saver) {
        PendingTaskEditorLaunchState()
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PendingTaskEditorLaunchDialog(
    visible: Boolean,
    saving: Boolean,
    modifier: Modifier,
    onReplace: () -> Unit,
    onKeepEditing: () -> Unit,
) {
    if (!visible) return
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = { if (!saving) onKeepEditing() },
        title = { Text("Review New Task Request?") },
        text = {
            Text(
                "Another Task draft is already open. Replace it now, or keep editing; the new Task request will wait and open after you save or close this draft.",
            )
        },
        confirmButton = {
            WhipTextButton(enabled = !saving, onClick = onReplace) {
                Text("Replace Open Draft")
            }
        },
        dismissButton = {
            WhipTextButton(enabled = !saving, onClick = onKeepEditing) {
                Text("Keep Editing")
            }
        },
    )
}
