package com.whip.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/** A separate Android window must follow the same resolved theme as its content. */
@Composable
internal fun WhipDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    val darkTheme = LocalWhipDarkTheme.current ?: isSystemInDarkTheme()
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        val window = (LocalView.current.parent as DialogWindowProvider).window
        SideEffect { applyWhipWindowAppearance(window, darkTheme) }
        content()
    }
}
