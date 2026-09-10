package com.whip.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule

/** Select the same Settings category through the phone index or the app's wide support pane. */
internal fun ComposeTestRule.openSettingsCategory(label: String) {
    waitForIdle()
    if (onAllNodesWithTag("settings-support-list").fetchSemanticsNodes().isNotEmpty()) {
        onNodeWithTag("settings-support-list")
            .performScrollToNode(hasTestTag("settings-support-section-$label"))
        onNodeWithTag("settings-support-section-$label").performClick()
    } else {
        if (onAllNodesWithContentDescription("Back to Settings").fetchSemanticsNodes().isNotEmpty()) {
            onNodeWithContentDescription("Back to Settings").performClick()
        }
        onNodeWithTag("settings-section-$label").performScrollTo().performClick()
    }
    waitForIdle()
}
