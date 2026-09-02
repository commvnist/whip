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

/**
 * Owns acknowledgment of a platform-entry queue rejection. The corresponding
 * Activity FIFO item remains unconsumed until [admit] observes that the user
 * acknowledged the same delivery ID, including after state restoration.
 */
@Stable
internal class LaunchQueueOverflowState {
    var pendingDeliveryId by mutableStateOf<Long?>(null)
        private set
    var rejectedShareCount by mutableStateOf(0)
        private set
    var dialogVisible by mutableStateOf(false)
        private set
    var acknowledgedDeliveryId by mutableStateOf<Long?>(null)
        private set
    var acknowledgedShareCount by mutableStateOf(0)
        private set
    var revision by mutableStateOf(0L)
        private set

    fun admit(deliveryId: Long, rejectedShareCount: Int): Boolean {
        val normalizedCount = rejectedShareCount.coerceAtLeast(1)
        if (
            acknowledgedDeliveryId == deliveryId &&
            acknowledgedShareCount == normalizedCount
        ) {
            return true
        }
        if (
            pendingDeliveryId != deliveryId ||
            this.rejectedShareCount != normalizedCount ||
            !dialogVisible
        ) {
            pendingDeliveryId = deliveryId
            this.rejectedShareCount = normalizedCount
            dialogVisible = true
            acknowledgedDeliveryId = null
            acknowledgedShareCount = 0
            revision++
        }
        return false
    }

    fun acknowledge() {
        val deliveryId = pendingDeliveryId ?: return
        acknowledgedDeliveryId = deliveryId
        acknowledgedShareCount = rejectedShareCount
        dialogVisible = false
        revision++
    }

    companion object {
        val Saver = listSaver<LaunchQueueOverflowState, Any>(
            save = { state ->
                listOf(
                    state.pendingDeliveryId ?: 0L,
                    state.rejectedShareCount,
                    state.dialogVisible,
                    state.acknowledgedDeliveryId ?: 0L,
                    state.acknowledgedShareCount,
                    state.revision,
                )
            },
            restore = { values ->
                LaunchQueueOverflowState().also { state ->
                    state.pendingDeliveryId = (values[0] as Long).takeIf { it > 0L }
                    state.rejectedShareCount = values[1] as Int
                    state.dialogVisible = values[2] as Boolean
                    state.acknowledgedDeliveryId = (values[3] as Long).takeIf { it > 0L }
                    state.acknowledgedShareCount = values[4] as Int
                    state.revision = values[5] as Long
                }
            },
        )
    }
}

@Composable
internal fun rememberLaunchQueueOverflowState(): LaunchQueueOverflowState =
    rememberSaveable(saver = LaunchQueueOverflowState.Saver) {
        LaunchQueueOverflowState()
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LaunchQueueOverflowDialog(
    state: LaunchQueueOverflowState,
    modifier: Modifier,
) {
    if (!state.dialogVisible) return
    val count = state.rejectedShareCount.coerceAtLeast(1)
    val subject = if (count == 1) "Task share wasn't" else "Task shares weren't"
    val pronoun = if (count == 1) "it" else "them"
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = {},
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        title = { Text("Share Queue Full") },
        text = {
            Text(
                "$count additional $subject added because four Task shares were already waiting. " +
                    "Return to the source app and share $pronoun again after reviewing the waiting drafts. " +
                    "Other Whip shortcuts and notification links remain queued.",
            )
        },
        confirmButton = {
            WhipTextButton(onClick = state::acknowledge) {
                Text("Got It")
            }
        },
    )
}
