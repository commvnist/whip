package com.whip.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** One page measure keeps headings and actions stable when changing sections in a pane. */
@Composable
internal fun WhipWorkspaceLayout(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier.fillMaxSize().testTag("workspace-layout-viewport"),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            Modifier.widthIn(max = 1000.dp).fillMaxSize().testTag("workspace-content-column"),
            content = content,
        )
    }
}
