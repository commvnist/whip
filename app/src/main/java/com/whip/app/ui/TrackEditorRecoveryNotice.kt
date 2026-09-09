package com.whip.app.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

@Composable
internal fun TrackEditorRecoveryNotice(session: TrackEditorSessionViewModel) {
    val problem = session.recoveryProblem ?: return
    PaneAwareAlertDialog(
        onDismissRequest = session::dismissRecoveryProblem,
        title = { Text("Track Draft Recovery") },
        text = {
            Text(
                problem,
                modifier = Modifier.testTag("track-draft-recovery-problem")
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        },
        confirmButton = {
            if (session.canRetryRecovery) {
                WhipTextButton(onClick = session::retryRecovery) { Text("Retry") }
            }
        },
        dismissButton = {
            WhipTextButton(onClick = session::dismissRecoveryProblem) {
                Text(if (session.routeState.value == null) "Close" else "Keep Editing")
            }
        },
    )
}
