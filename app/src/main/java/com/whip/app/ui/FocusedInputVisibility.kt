package com.whip.app.ui

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/** Keep the field and its floating label visible as the scrolling viewport resizes. */
@Composable
internal fun rememberFocusedInputVisibility(
    viewportSize: IntSize,
    hasFloatingLabel: Boolean = true,
    onFocusChange: (Boolean) -> Unit = {},
): Modifier {
    val requester = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(focused, viewportSize) {
        if (focused) requester.bringIntoView()
    }
    return Modifier.bringIntoViewRequester(requester).onFocusChanged {
        focused = it.isFocused
        onFocusChange(it.isFocused)
    // At enlarged text a floating label can extend above the field's own bounds.
    // Include that space in the visibility request, outside the field semantics.
    }.padding(top = if (hasFloatingLabel) 8.dp else 0.dp)
}
