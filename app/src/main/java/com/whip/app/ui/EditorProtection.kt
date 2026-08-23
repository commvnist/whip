package com.whip.app.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun UnsavedChangesDialog(
    subject: String,
    onKeepEditing: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onKeepEditing,
        title = { Text("Discard unsaved changes?") },
        text = { Text("Your edits to this $subject have not been saved.") },
        confirmButton = { TextButton(onClick = onDiscard) { Text("Discard changes") } },
        dismissButton = { TextButton(onClick = onKeepEditing) { Text("Keep editing") } },
    )
}
