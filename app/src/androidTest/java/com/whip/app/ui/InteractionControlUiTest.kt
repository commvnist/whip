package com.whip.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Text
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.UnitDimension
import com.whip.app.ui.theme.WhipTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InteractionControlUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun customIconSelectionImmediatelyReplacesTheStalePresetState() {
        var icon by mutableStateOf("✓")
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipIconPicker(value = icon, onValueChange = { icon = it })
            }
        }

        compose.onNodeWithContentDescription("Icon: Check").performClick()
        compose.onNodeWithTag("icon-picker-custom-option").performClick()

        assertEquals("", icon)
        compose.onNodeWithContentDescription("Icon: Custom Icon").assertIsDisplayed()
        compose.onNodeWithTag("icon-picker-custom-input").assertIsDisplayed()

        compose.onNodeWithTag("icon-picker-custom-input").performTextReplacement("★")
        assertEquals("★", icon)
        compose.onNodeWithContentDescription("Icon: Custom Icon").performClick()
        compose.onNodeWithTag("icon-picker-custom-option").performClick()
        assertEquals("★", icon)
        compose.onNodeWithContentDescription("Icon: Custom Icon").performClick()
        compose.onNodeWithText("○  Circle").performClick()

        assertEquals("○", icon)
        compose.onNodeWithContentDescription("Icon: Circle").assertIsDisplayed()
        compose.onAllNodesWithTag("icon-picker-custom-input").assertCountEquals(0)
    }

    @Test
    fun sharedColorPickerSupportsPresetsAndExactCustomColors() {
        var color by mutableStateOf<Long?>(null)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipColorField(value = color, onValueChange = { color = it })
            }
        }

        compose.onNodeWithContentDescription("Color: Default. Choose color.").performClick()
        compose.onNodeWithTag("color-picker-dialog").assertIsDisplayed()
        compose.onNodeWithTag("color-preset-default").assertIsSelected()
        compose.onNodeWithTag("color-preset-blue").performClick().assertIsSelected()
        compose.onNodeWithTag("custom-color-toggle").performClick()
        compose.onNodeWithTag("custom-color-hex").performTextReplacement("#123456")
        compose.onNodeWithText("Apply").performClick()

        assertEquals(0xFF123456L, color)
        compose.onNodeWithContentDescription("Color: Custom · #123456. Choose color.").assertIsDisplayed()
    }

    @Test
    fun fullScreenDestinationOwnsTheWindowWhileContentRespectsSafeInsets() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Box(Modifier.size(width = 320.dp, height = 240.dp)) {
                    WhipFullScreenSurface(
                        title = "Areas",
                        contentInsets = WindowInsets(top = 24.dp),
                    ) {
                        Box(Modifier.fillMaxSize())
                    }
                }
            }
        }

        val surfaceBounds = compose.onNodeWithTag("full-screen-destination-surface")
            .fetchSemanticsNode().boundsInRoot
        val contentBounds = compose.onNodeWithTag("full-screen-destination-content")
            .fetchSemanticsNode().boundsInRoot
        val insetPx = with(compose.density) { 24.dp.toPx() }

        assertEquals(0f, surfaceBounds.top, 0.5f)
        assertEquals(0f, surfaceBounds.left, 0.5f)
        assertEquals(with(compose.density) { 320.dp.toPx() }, surfaceBounds.width, 0.5f)
        assertEquals(with(compose.density) { 240.dp.toPx() }, surfaceBounds.height, 0.5f)
        assertEquals(surfaceBounds.top + insetPx, contentBounds.top, 0.5f)
        assertEquals(surfaceBounds.bottom, contentBounds.bottom, 0.5f)
    }

    @Test
    fun sharedControlsExposeConsistentStateRolesAndTouchTargets() {
        var destination by mutableStateOf("Today")
        var expanded by mutableStateOf(false)
        var choice by mutableStateOf("System")
        var mode by mutableStateOf(false)

        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Column {
                    DestinationTabBar(
                        selected = destination,
                        destinations = listOf("Today", "Anytime", "Archived"),
                        onSelect = { destination = it },
                        label = { it },
                        testTagPrefix = "control-tab",
                    )
                    DisclosureButton(
                        label = "Advanced options",
                        expanded = expanded,
                        onClick = { expanded = !expanded },
                        modifier = Modifier.testTag("control-disclosure"),
                    )
                    SelectionField(
                        label = "Theme",
                        values = listOf("System", "Light", "Dark"),
                        selected = choice,
                        valueText = { it },
                        onSelect = { choice = it },
                    )
                    NavigationRow("Machines", onClick = {}, modifier = Modifier.testTag("control-navigation"))
                    ModeButton("Select tasks", mode, { mode = !mode }, Modifier.testTag("control-mode"))
                }
            }
        }

        compose.onNodeWithTag("control-tab-Today").assertIsSelected()
        compose.onNodeWithTag("control-tab-Archived").performClick().assertIsSelected()
        compose.onNodeWithTag("control-disclosure").performClick()
        assertEquals("Expanded", compose.onNodeWithTag("control-disclosure").fetchSemanticsNode().config[SemanticsProperties.StateDescription])

        compose.onNodeWithContentDescription("Theme: System").performClick()
        compose.onNodeWithText("✓  System").assertIsDisplayed()
        compose.onNodeWithText("Dark").performClick()
        compose.onNodeWithContentDescription("Theme: Dark").assertIsDisplayed()

        val navigation = compose.onNodeWithTag("control-navigation").fetchSemanticsNode()
        assertEquals(Role.Button, navigation.config[SemanticsProperties.Role])
        compose.onNodeWithTag("control-mode").performClick()
        compose.onNodeWithText("Done").assertIsDisplayed()
        assertEquals("Active", compose.onNodeWithTag("control-mode").fetchSemanticsNode().config[SemanticsProperties.StateDescription])

        val minimumPx = with(compose.density) { 48.dp.toPx() }
        listOf("control-tab-Archived", "control-disclosure", "control-mode").forEach { tag ->
            assertTrue("$tag is below 48 dp", compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.height >= minimumPx)
        }
    }

    @Test
    fun destinationTabsRemainReachableAtLargeTextInRtl() {
        val largeText = Density(compose.density.density, fontScale = 2f)
        var destination by mutableStateOf("Today")
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides largeText,
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                WhipTheme(dynamicColor = false) {
                    Box(Modifier.width(320.dp)) {
                        DestinationTabBar(
                            selected = destination,
                            destinations = listOf("Today", "All", "Insights", "Connections", "Archived"),
                            onSelect = { destination = it },
                            label = { it },
                            testTagPrefix = "rtl-tab",
                        )
                    }
                }
            }
        }

        compose.onNodeWithTag("rtl-tab-Archived").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithTag("rtl-tab-Archived").assertIsSelected()
        compose.onNodeWithTag("rtl-tab-Today").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithTag("rtl-tab-Today").assertIsSelected()
    }

    @Test
    fun destinationTabsKeepTheirPositionWhenTheLastVisibleTabIsSelected() {
        var destination by mutableStateOf("Workout")
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Box(Modifier.width(380.dp)) {
                    DestinationTabBar(
                        selected = destination,
                        destinations = listOf("Workout", "History", "Progress", "Library"),
                        onSelect = { destination = it },
                        label = { it },
                        testTagPrefix = "stable-tab",
                    )
                }
            }
        }

        val before = compose.onNodeWithTag("stable-tab-Workout").fetchSemanticsNode().boundsInRoot.left
        compose.onNodeWithTag("stable-tab-Library").performClick().assertIsSelected()
        val after = compose.onNodeWithTag("stable-tab-Workout").fetchSemanticsNode().boundsInRoot.left
        val expectedInset = with(compose.density) { 20.dp.toPx() }

        assertEquals(before, after, 0.5f)
        assertEquals(expectedInset, after, 0.5f)
    }

    @Test
    fun destinationTabsRevealASelectedTabWithoutLosingTheOuterGutter() {
        var destination by mutableStateOf("General")
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Box(Modifier.width(320.dp)) {
                    DestinationTabBar(
                        selected = destination,
                        destinations = listOf("General", "Organization", "Reminders", "Data & Backup"),
                        onSelect = { destination = it },
                        label = { it },
                        testTagPrefix = "reveal-tab",
                    )
                }
            }
        }

        compose.onNodeWithTag("reveal-tab-Data & Backup")
            .performSemanticsAction(SemanticsActions.OnClick)
            .assertIsSelected()

        val expectedRightEdge = with(compose.density) { (320.dp - 20.dp).toPx() }
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onNodeWithTag("reveal-tab-Data & Backup")
                .fetchSemanticsNode().boundsInRoot.right <= expectedRightEdge + 0.5f
        }
        val selectedBounds = compose.onNodeWithTag("reveal-tab-Data & Backup")
            .fetchSemanticsNode().boundsInRoot
        assertEquals(expectedRightEdge, selectedBounds.right, 0.5f)
    }

    @Test
    fun peerToolbarActionsCenterEveryContentGroup() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Row(Modifier.width(360.dp)) {
                    ToolbarActionButton(
                        label = "Search",
                        onClick = {},
                        icon = Icons.Outlined.Search,
                        modifier = Modifier.weight(1f).testTag("toolbar-search"),
                    )
                    ToolbarActionButton(
                        label = "Filters",
                        onClick = {},
                        modifier = Modifier.weight(1f).testTag("toolbar-filters"),
                    )
                    ToolbarActionButton(
                        label = "Select",
                        onClick = {},
                        modifier = Modifier.weight(1f).testTag("toolbar-select"),
                    )
                }
            }
        }

        listOf("Search" to "toolbar-search", "Filters" to "toolbar-filters", "Select" to "toolbar-select")
            .forEach { (label, controlTag) ->
                val controlCenter = compose.onNodeWithTag(controlTag).fetchSemanticsNode().boundsInRoot.center.x
                val contentCenter = compose.onNodeWithTag("toolbar-action-content-$label", useUnmergedTree = true)
                    .fetchSemanticsNode().boundsInRoot.center.x
                assertEquals("$label content is not centered", controlCenter, contentCenter, 0.5f)
            }
    }

    @Test
    fun pageSearchLivesInTheHeaderInsteadOfASecondControlRow() {
        var searchOpened by mutableStateOf(false)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Box(Modifier.width(360.dp)) {
                    Column {
                        WhipPageHeader("Today", "What needs your attention now") {
                            WhipPageIconAction(
                                icon = Icons.Outlined.Search,
                                label = "Search Tasks & Steps",
                                onClick = { searchOpened = true },
                            )
                        }
                        if (searchOpened) Text("Search controller opened")
                        WhipViewAndFilterRow(
                            selectedView = "List",
                            views = listOf("List"),
                            viewLabel = { it },
                            onSelectView = {},
                            filterCount = 0,
                            onOpenFilters = {},
                        )
                    }
                }
            }
        }

        compose.onNodeWithText("Today").assertIsDisplayed()
        compose.onNodeWithContentDescription("Search Tasks & Steps").performClick()
        compose.onNodeWithText("Search controller opened").assertIsDisplayed()
        compose.onAllNodesWithTag("contextual-search-actions").assertCountEquals(0)
        compose.onAllNodesWithTag("contextual-search-field").assertCountEquals(0)
    }

    @Test
    fun unitChooserCreatesAndSelectsACustomUnitInContext() {
        var selectedUnitId by mutableStateOf("count")
        var units by mutableStateOf(BuiltInUnits.all)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Box(Modifier.width(360.dp)) {
                    UnitSelectionField(
                        units = units,
                        selectedUnitId = selectedUnitId,
                        dimension = UnitDimension.Count,
                        onSelect = { selectedUnitId = it },
                        onCreateUnit = { name, symbol, dimension, factor, onResult ->
                            val id = "custom-$name"
                            units = units + UnitDefinition(
                                id = id,
                                name = name,
                                symbol = symbol,
                                dimension = dimension,
                                toCanonicalFactor = factor,
                                custom = true,
                            )
                            onResult(Result.success(id))
                        },
                    )
                }
            }
        }

        compose.onNodeWithContentDescription("Unit: count").performClick()
        compose.onNodeWithText("Create Custom Unit…").performClick()
        compose.onNodeWithText("Create Custom Unit").assertIsDisplayed()
        compose.onNodeWithText("Dimension: Count").assertIsDisplayed()
        compose.onNodeWithTag("custom-unit-name").performTextReplacement("session")
        compose.onNodeWithTag("custom-unit-symbol").performTextReplacement("sess")
        compose.onNodeWithTag("custom-unit-factor").performTextReplacement("1")
        compose.onNodeWithTag("custom-unit-confirm").performClick()

        assertEquals("custom-session", selectedUnitId)
        compose.onNodeWithContentDescription("Unit: session (sess)").assertIsDisplayed()
    }
}
