package com.whip.app.ui

import com.whip.app.core.OperationStatus

/**
 * Claims a terminal operation exactly once before entering the suspending UI
 * that displays it. Navigation may cancel [show], but can no longer replay the
 * same message and restart its timeout on the next page.
 */
internal suspend fun <T> OperationStatus.deliverTransientMessage(
    consume: () -> Unit,
    show: suspend (String) -> T,
): T? {
    val message = when (this) {
        OperationStatus.Idle, is OperationStatus.Running -> return null
        is OperationStatus.Succeeded -> message
        is OperationStatus.Failed -> message
    }
    consume()
    return show(message)
}
