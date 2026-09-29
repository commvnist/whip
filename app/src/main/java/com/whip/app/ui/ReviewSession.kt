package com.whip.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue

/** Review owns its inspection loop; the app shell continues to own destination navigation. */
@Stable
internal class ReviewSession(
    openState: MutableState<Boolean>,
    returnState: MutableState<Boolean>,
    private val savedContent: SaveableStateHolder,
) {
    var isOpen by openState
        private set
    var canReturn by returnState
        private set

    fun open() { isOpen = true; canReturn = false }
    fun visitSource() { isOpen = false; canReturn = true }
    fun close() { isOpen = false; canReturn = false; savedContent.removeState("review") }

    @Composable fun Content(content: @Composable () -> Unit) {
        if (isOpen) savedContent.SaveableStateProvider("review", content)
    }

    @Composable fun ReturnBar() {
        if (canReturn) ReviewReturnBar(onReturn = ::open, onDismiss = ::close)
    }
}

@Composable
internal fun rememberReviewSession(): ReviewSession {
    val open = rememberSaveable { mutableStateOf(false) }
    val canReturn = rememberSaveable { mutableStateOf(false) }
    val content = rememberSaveableStateHolder()
    return remember(open, canReturn, content) { ReviewSession(open, canReturn, content) }
}
