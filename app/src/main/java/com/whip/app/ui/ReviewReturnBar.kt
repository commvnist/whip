package com.whip.app.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
internal fun ReviewReturnBar(onReturn: () -> Unit, onDismiss: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = WhipSpacing.compact).testTag("review-return-bar"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WhipTextButton(onClick = onReturn, modifier = Modifier.weight(1f).testTag("return-to-review")) {
            Text("Return to Review & Trends")
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Outlined.Close, contentDescription = "Dismiss return to Review")
        }
    }
}
