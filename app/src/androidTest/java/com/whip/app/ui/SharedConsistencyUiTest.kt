package com.whip.app.ui

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
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.Area
import com.whip.app.ui.theme.WhipTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedConsistencyUiTest {
    @get:Rule
    val compose = createComposeRule()

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
    fun taskTemplatesKeepTheFinalRecipeReachableAtExtremeTextAndShortHeight() {
        val largeText = Density(compose.density.density, fontScale = 3.2f)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides largeText) {
                WhipTheme(dynamicColor = false) {
                    TaskRecipeDialog(
                        today = LocalDate.of(2026, 8, 29),
                        modifier = Modifier.width(300.dp).height(460.dp),
                        onDismiss = {},
                        onChoose = {},
                    )
                }
            }
        }

        compose.onNodeWithTag("task-template-list").performScrollToNode(
            hasContentDescription("Break Complex Work into Subtasks", substring = true),
        )
        compose.onNodeWithContentDescription(
            "Break Complex Work into Subtasks",
            substring = true,
        ).assertIsDisplayed()
        compose.onNodeWithText("Cancel").assertIsDisplayed()
    }
}
