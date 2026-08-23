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
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This cannot be undone. The following data will be affected:")
                impacts.filter(String::isNotBlank).forEach { Text("• $it") }
                Text(
                    "Export a backup first if you may need this history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            WhipTextButton(onClick = onConfirm) {
                Text("Delete Permanently", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
