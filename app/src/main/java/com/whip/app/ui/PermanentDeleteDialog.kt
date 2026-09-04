package com.whip.app.ui

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun PermanentDeleteDialog(
    modifier: Modifier = Modifier,
    title: String,
    impacts: List<String>,
    message: String = "This cannot be undone. The following data will be affected:",
    confirmLabel: String = "Delete Permanently",
    busyLabel: String = "Working…",
    busy: Boolean = false,
    error: String? = null,
    confirmModifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = { if (!busy) onDismiss() },
        paneTitle = title,
        title = { Text(title) },
        text = {
            WhipDialogBody(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(message)
                impacts.filter(String::isNotBlank).forEach { Text("• $it") }
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                    )
                }
                Text(
                    "Export a backup first if you may need this history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            WhipTextButton(onClick = onConfirm, enabled = !busy, modifier = confirmModifier) {
                Text(if (busy) busyLabel else confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { WhipTextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } },
    )
}
