package com.whip.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/** Scope and archive navigation use the same visible controls as a person. */
internal fun ComposeTestRule.selectTaskCollectionScope(label: String) {
    onNodeWithTag("task-destination-Tasks").performClick()
    val scope = hasText("All Tasks ▾") or hasText("Unscheduled ▾") or hasText("Upcoming ▾") or hasText("Custom scope ▾")
    onNodeWithTag("task-workspace-list").performScrollToNode(scope)
    onNode(scope).performClick()
    onNode(hasText(label, substring = false) and hasAnyAncestor(isPopup())).performClick()
    onNodeWithTag("task-destination-Tasks").assertIsSelected()
    onNodeWithText("$label ▾").assertIsDisplayed()
}

internal fun ComposeTestRule.openWorkspaceArchive(workspace: String) {
    val more = when (workspace) {
        "Habits" -> hasContentDescription("More Habit Actions")
        "Goals" -> hasContentDescription("More Goal Actions")
        "Tracks" -> hasContentDescription("More Track Actions") or hasContentDescription("More Track Options")
        "Tasks" -> hasContentDescription("More task list actions")
        else -> error("Unknown workspace: $workspace")
    }
    onNode(more).performClick()
    onNodeWithText("Archived $workspace").performClick()
}

internal fun ComposeTestRule.selectTaskPlanningLayout(label: String) {
    val chooser = hasText("List ▾") or hasText("Agenda ▾") or hasText("Calendar ▾")
    onNodeWithTag("task-workspace-list").performScrollToNode(chooser)
    onNode(chooser).performClick()
    onNode(hasText(label, substring = false) and hasAnyAncestor(isPopup())).performClick()
    onNodeWithText("$label ▾").assertIsDisplayed()
}

internal fun ComposeTestRule.openSavedTaskView(name: String) {
    onNodeWithContentDescription("More task list actions").performClick()
    onNodeWithText("Saved Views").performClick()
    onNodeWithText(name).performScrollTo().performClick()
    waitUntil(10_000) { onAllNodesWithText("Saved Views").fetchSemanticsNodes().isEmpty() }
}

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
