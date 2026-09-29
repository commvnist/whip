package com.whip.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import com.whip.app.domain.AreaScope

/** Review owns its inspection loop; the app shell continues to own destination navigation. */
@Stable
internal class ReviewSession(
    openState: MutableState<Boolean>,
    returnState: MutableState<Boolean>,
    areaState: MutableState<String?>,
    private val savedContent: SaveableStateHolder,
    private val currentArea: () -> AreaScope,
    private val restoreArea: (AreaScope) -> Unit,
) {
    var isOpen by openState
        private set
    var canReturn by returnState
        private set
    private var originatingArea by areaState

    fun open() {
        if (canReturn) originatingArea?.let { restoreArea(AreaScope.fromStorageKey(it)) }
        else if (!isOpen) originatingArea = currentArea().storageKey
        isOpen = true
        canReturn = false
    }
    fun visitSource() { isOpen = false; canReturn = true }
    fun close() { isOpen = false; canReturn = false; originatingArea = null; savedContent.removeState("review") }

    @Composable fun Content(content: @Composable () -> Unit) {
        if (isOpen) savedContent.SaveableStateProvider("review", content)
    }

    @Composable fun ReturnBar() {
        if (canReturn) ReviewReturnBar(onReturn = ::open, onDismiss = ::close)
    }
}

@Composable
internal fun rememberReviewSession(
    areaScope: AreaScope,
    onRestoreAreaScope: (AreaScope) -> Unit,
): ReviewSession {
    val open = rememberSaveable { mutableStateOf(false) }
    val canReturn = rememberSaveable { mutableStateOf(false) }
    val area = rememberSaveable { mutableStateOf<String?>(null) }
    val currentArea = rememberUpdatedState(areaScope)
    val restoreArea = rememberUpdatedState(onRestoreAreaScope)
    val content = rememberSaveableStateHolder()
    return remember(open, canReturn, area, content) {
        ReviewSession(open, canReturn, area, content, { currentArea.value }, { restoreArea.value(it) })
    }
}
