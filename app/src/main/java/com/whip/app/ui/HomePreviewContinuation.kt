package com.whip.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/** A bounded Home preview always names its full destination and retained count. */
@Composable
internal fun HomePreviewContinuation(label: String, count: Int, onOpen: () -> Unit) {
    WhipTextButton(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().testTag("home-view-all-$label"),
    ) { Text("View All $label · $count") }
}
