package com.whip.app.ui

import com.whip.app.AndroidFontScale
import com.whip.app.AndroidFontScaleRule
import com.whip.app.assertDialogFontScale
import com.whip.app.captureVisualCatalogSurface

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.Area
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.ScheduleKind
import com.whip.app.ui.theme.WhipTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedConsistencyUiTest {
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)

    @Test
    fun statusAndEmptyStateExposeSeverityAnnouncementAndHierarchy() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Column {
                    WhipStatusCard(
                        kind = WhipStatusKind.Loading,
                        title = "Loading Entries",
                        message = "Preparing saved Entries.",
                        modifier = Modifier.testTag("loading-status"),
                    )
                    WhipStatusCard(
                        kind = WhipStatusKind.Error,
                        title = "Entries Unavailable",
                        message = "Storage is offline",
                        modifier = Modifier.testTag("error-status"),
                    )
                    WhipEmptyState("No Entries Yet", "Add the first Entry when it is ready.")
                }
            }
        }

        val loading = compose.onNodeWithTag("loading-status").fetchSemanticsNode().config
        assertEquals(LiveRegionMode.Polite, loading[SemanticsProperties.LiveRegion])
        assertEquals("Loading", loading[SemanticsProperties.StateDescription])
        val error = compose.onNodeWithTag("error-status").fetchSemanticsNode().config
        assertEquals(LiveRegionMode.Polite, error[SemanticsProperties.LiveRegion])
        assertEquals("Error", error[SemanticsProperties.StateDescription])
        assertEquals("Storage is offline", error[SemanticsProperties.Error])
        assertTrue(SemanticsProperties.Heading in compose.onNodeWithText("No Entries Yet").fetchSemanticsNode().config)
    }

    @Test
    fun settingsRowAndClickableSectionHeadingHaveOneCompleteSemanticsNode() {
        var changes = 0
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Column {
                    WhipSettingsRow("Quiet Hours", checked = true, onCheckedChange = { changes += 1 })
                    SectionHeading("Tasks", 3, onClick = {})
                }
            }
        }

        val switches = compose.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On),
        ).fetchSemanticsNodes()
        assertEquals(1, switches.size)
        assertEquals(Role.Switch, switches.single().config[SemanticsProperties.Role])
        compose.onNodeWithText("Quiet Hours").performClick()
        assertEquals(1, changes)
        val heading = compose.onNodeWithText("Tasks").fetchSemanticsNode().config
        assertTrue(SemanticsProperties.Heading in heading)
        assertEquals(Role.Button, heading[SemanticsProperties.Role])
    }

    @Test
    fun areaMenuPlacesSelectionOnTheRowAndKeepsTheCheckDecorative() {
        val work = Area("work", "Work", null, 0, false, 1, 1)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                AreaSelectionDropdown(listOf(work), "work", onSelect = { _, _ -> })
            }
        }

        compose.onNodeWithContentDescription("Area selection: Work").performClick()
        compose.onNodeWithContentDescription("Area Work").assertIsDisplayed().assertIsSelected()
        compose.onAllNodesWithContentDescription("Selected").assertCountEquals(0)
    }

    @Test
    @AndroidFontScale(3.2f)
    fun taskTemplatesKeepTheFinalRecipeReachableAtExtremeTextAndShortHeight() {
        var chosen: TaskDraft? = null
        val largeText = Density(compose.density.density, fontScale = 3.2f)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides largeText) {
                WhipTheme(dynamicColor = false) {
                    TaskRecipeDialog(
                        today = LocalDate.of(2026, 8, 29),
                        modifier = Modifier.width(300.dp).height(460.dp),
                        onDismiss = {},
                        onChoose = { chosen = it },
                    )
                }
            }
        }

        compose.assertDialogFontScale(3.2f)
        captureVisualCatalogSurface("tasks.templates.extreme")

        val initialChoice = compose.onNodeWithContentDescription("Inbox Task", substring = true)
            .assertIsDisplayed().getUnclippedBoundsInRoot()
        val viewport = compose.onNodeWithTag("task-template-list").getUnclippedBoundsInRoot()
        assertTrue("The first template must be fully visible before scrolling", initialChoice.bottom <= viewport.bottom)
        val cancelBefore = compose.onNodeWithText("Cancel").getUnclippedBoundsInRoot()
        compose.onNodeWithTag("task-template-list").performScrollToNode(
            hasContentDescription("Task with Subtasks", substring = true),
        )
        captureVisualCatalogSurface("tasks.templates.extreme.last")
        compose.onNodeWithContentDescription(
            "Task with Subtasks",
            substring = true,
        ).assertIsDisplayed().performClick()
        compose.onNodeWithText("Cancel").assertIsDisplayed()
        assertEquals(cancelBefore, compose.onNodeWithText("Cancel").getUnclippedBoundsInRoot())
        compose.runOnIdle { assertEquals(listOf("Plan", "Do the work", "Review"), chosen?.steps?.map { it.title }) }
    }

    @Test
    fun templateChoiceReturnsToAnEditableScheduledTask() {
        val today = LocalDate.of(2026, 9, 9)
        var saved: TaskDraft? = null
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TaskEditorDialog(
                    request = TaskEditorRequest(sessionId = 2_211L),
                    today = today,
                    onDismiss = {},
                    onSave = { _, draft, _ -> saved = draft },
                    onRequestNotificationPermission = {},
                )
            }
        }
        compose.onNodeWithText("Use a Template").performScrollTo().performClick()
        compose.onNodeWithText("Task Templates").assertIsDisplayed()
        captureVisualCatalogSurface("tasks.templates.normal")
        compose.onNodeWithContentDescription("Task on a Date", substring = true).performClick()
        compose.runOnIdle { assertEquals(null, saved) }
        compose.onNodeWithText("Dated Task").assertIsDisplayed()
        compose.onNodeWithText("Save").performClick()
        compose.runOnIdle {
            assertEquals("Dated Task", saved?.title)
            assertEquals(today, saved?.date)
            assertEquals(ScheduleKind.Once, saved?.scheduleKind)
            assertEquals(30, saved?.durationMinutes)
            assertEquals(false, saved?.inbox)
        }
    }
}
