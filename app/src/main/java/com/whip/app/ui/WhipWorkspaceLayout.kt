package com.whip.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The screen declares its reading task; item builders retain their own content geometry. */
internal enum class WhipWorkspaceComposition(val maxWidth: Dp) {
    Reading(720.dp),
    Overview(1000.dp),
    Browser(1000.dp),
}

/** Keeps workspace chrome and content on the same centered measure within the available pane. */
@Composable
internal fun WhipWorkspaceLayout(
    composition: WhipWorkspaceComposition,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier.fillMaxSize().testTag("workspace-layout-viewport"),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            Modifier.widthIn(max = composition.maxWidth).fillMaxSize().testTag("workspace-content-column"),
            content = content,
        )
    }
}
