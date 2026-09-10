package com.whip.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/** Native bounds prove that chrome and working content share the available reading column. */
internal fun ComposeTestRule.assertWorkspaceMeasure(maxWidth: Dp) {
    val viewport = onNodeWithTag("workspace-layout-viewport").fetchSemanticsNode().boundsInRoot
    val column = onNodeWithTag("workspace-content-column").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
    val header = onNodeWithTag("workspace-top-app-bar").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
    val expectedWidth = minOf(viewport.width, with(density) { maxWidth.toPx() })
    assertEquals("Workspace must use its available width up to the reading measure", expectedWidth, column.width, 1f)
    assertEquals("Workspace must center in the available pane", viewport.center.x, column.center.x, 1f)
    assertTrue("Header controls must align with the working column", header.left >= column.left - 1f && header.right <= column.right + 1f)
}
