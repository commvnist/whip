package com.whip.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.whip.app.domain.ScheduledTask

/** The focused entry form has an independent saved route; persistence stays with FocusTimerUi. */
internal class TaskFocusLauncher(private val requestedKey: MutableState<String?>) {
    fun open(item: ScheduledTask) { requestedKey.value = item.stableKey }
    fun close() { requestedKey.value = null }

    @Composable fun Content(state: TaskUiState, focus: FocusTimerUiBindings) {
        val index = remember(state) { TaskNavigationIndex(state) }
        val item = requestedKey.value?.let(index.byKey::get)
        LaunchedEffect(item, state.loading, focus.busy) {
            if (!state.loading && !focus.busy && (item == null || item.task.archived || item.completedAtMillis != null)) close()
        }
        item?.takeIf { !it.task.archived && it.completedAtMillis == null }?.let {
            TaskFocusDialog(
                item = it,
                onDismiss = ::close,
                onStartFocus = { minutes -> focus.start(it.task.id, minutes) },
                focusBusy = focus.busy,
                focusError = focus.startErrorFor(it.task.id),
                activeFocusTaskTitle = focus.activeTaskTitle,
                activeFocusDeadlineMillis = focus.activeDeadlineMillis,
            )
        }
    }
}

@Composable
internal fun rememberTaskFocusLauncher(): TaskFocusLauncher {
    val key = rememberSaveable { mutableStateOf<String?>(null) }
    return remember(key) { TaskFocusLauncher(key) }
}
