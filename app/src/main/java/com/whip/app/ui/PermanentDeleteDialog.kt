package com.whip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PermanentDeleteDialog(
    modifier: Modifier = Modifier,
    title: String,
    impacts: List<String>,
    message: String = "This cannot be undone. The following data will be affected:",
    confirmLabel: String = "Delete Permanently",
    busy: Boolean = false,
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(message)
                impacts.filter(String::isNotBlank).forEach { Text("• $it") }
                Text(
                    "Export a backup first if you may need this history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            WhipTextButton(onClick = onConfirm, enabled = !busy, modifier = confirmModifier) {
                Text(if (busy) "Working…" else confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { WhipTextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } },
    )
}
