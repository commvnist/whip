package com.whip.app.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun UnsavedChangesDialog(
    subject: String,
    onKeepEditing: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = onKeepEditing,
        paneTitle = "Discard Unsaved Changes",
        title = { Text("Discard Unsaved Changes?") },
        text = { Text("Your edits to this $subject have not been saved.") },
        confirmButton = { WhipTextButton(onClick = onDiscard) { Text("Discard Changes") } },
        dismissButton = { WhipTextButton(onClick = onKeepEditing) { Text("Keep Editing") } },
    )
}
