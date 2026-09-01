package com.whip.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Text
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.CustomIdentityEmoji
import com.whip.app.domain.TrackField
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackValueDraft
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.WhipTask
import com.whip.app.ui.theme.WhipTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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
    fun commandMenuItemsDoNotPretendToBeSelectableChoices() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Column {
                    WhipMenuItem(
                        label = "Duplicate",
                        onClick = {},
                        modifier = Modifier.testTag("plain-menu-command"),
                    )
                    WhipMenuItem(
                        label = "Newest first",
                        onClick = {},
                        selected = false,
                        modifier = Modifier.testTag("unselected-menu-choice"),
                    )
                    WhipMenuItem(
                        label = "Oldest first",
                        onClick = {},
                        selected = true,
                        modifier = Modifier.testTag("selected-menu-choice"),
                    )
                }
            }
        }

        val plain = compose.onNodeWithTag("plain-menu-command").fetchSemanticsNode().config
        val unselected = compose.onNodeWithTag("unselected-menu-choice").fetchSemanticsNode().config
        val selected = compose.onNodeWithTag("selected-menu-choice").fetchSemanticsNode().config
        assertTrue(!plain.contains(SemanticsProperties.Selected))
        assertEquals(false, unselected[SemanticsProperties.Selected])
        assertEquals(true, selected[SemanticsProperties.Selected])
    }

    @Test
    fun reorderHandleHasOneConsistentTouchAndAccessibilityContract() {
        var movement = 0
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipReorderHandle(
                    label = "Morning routine",
                    canMovePrevious = true,
                    canMoveNext = true,
                    onMove = { movement += it },
                )
            }
        }

        val node = compose.onNodeWithContentDescription("Reorder Morning routine").assertIsDisplayed()
        val semantics = node.fetchSemanticsNode().config
        assertEquals("Drag up or down", semantics[SemanticsProperties.StateDescription])
        assertTrue(SemanticsActions.OnClick !in semantics)
        val actions = semantics[SemanticsActions.CustomActions]
        assertEquals(listOf("Move Morning routine up", "Move Morning routine down"), actions.map { it.label })
        compose.runOnIdle { actions.last().action() }
        assertEquals(1, movement)
        val minimumPx = with(compose.density) { 48.dp.toPx() }
        assertTrue(node.fetchSemanticsNode().boundsInRoot.height >= minimumPx)
        assertTrue(node.fetchSemanticsNode().boundsInRoot.width >= minimumPx)
    }

    @Test
    fun repeatedAccessibilityMovesKeepAnnouncementWithTheLogicalItem() {
        var order by mutableStateOf(listOf("Alpha", "Beta", "Gamma"))
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipReorderLayout(itemSpacing = 8.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        order.forEach { label ->
                            key(label) {
                                val index = order.indexOf(label)
                                val interaction = rememberWhipReorderInteractionState()
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .whipReorderItem(
                                            interaction,
                                            layoutPosition = index + 1,
                                            layoutScope = "accessibility-identity",
                                        ),
                                ) {
                                    WhipReorderHandle(
                                        label = label,
                                        canMovePrevious = index > 0,
                                        canMoveNext = index < order.lastIndex,
                                        position = index + 1,
                                        total = order.size,
                                        interactionState = interaction,
                                        moveWholeItem = true,
                                        layoutScope = "accessibility-identity",
                                        onMove = { delta -> order = moveListItem(order, index, delta) },
                                    )
                                    Text(label)
                                }
                            }
                        }
                    }
                }
            }
        }

        fun moveAlphaDown() {
            val action = compose.onNodeWithContentDescription("Reorder Alpha")
                .fetchSemanticsNode().config[SemanticsActions.CustomActions]
                .last { it.label == "Move Alpha down" }
            compose.runOnIdle { assertTrue(action.action()) }
        }

        moveAlphaDown()
        compose.waitForIdle()
        assertEquals(listOf("Beta", "Alpha", "Gamma"), order)
        assertTrue(
            compose.onNodeWithContentDescription("Reorder Alpha")
                .fetchSemanticsNode().config[SemanticsProperties.StateDescription]
                .startsWith("Position 2 of 3. Moved to position 2"),
        )

        moveAlphaDown()
        compose.waitForIdle()
        assertEquals(listOf("Beta", "Gamma", "Alpha"), order)
        assertTrue(
            compose.onNodeWithContentDescription("Reorder Alpha")
                .fetchSemanticsNode().config[SemanticsProperties.StateDescription]
                .startsWith("Position 3 of 3. Moved to position 3"),
        )
    }

    @Test
    fun reorderHandleOwnsTouchDragAndUsesTheLatestMoveCallback() {
        var parentClicks by mutableStateOf(0)
        var callbackVersion by mutableStateOf(1)
        val moves = mutableListOf<Pair<Int, Int>>()
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Box(Modifier.clickable { parentClicks++ }) {
                    WhipReorderHandle(
                        label = "Nested item",
                        canMovePrevious = true,
                        canMoveNext = true,
                        onMove = { delta -> moves += callbackVersion to delta },
                    )
                }
            }
        }

        val handle = compose.onNodeWithContentDescription("Reorder Nested item")
        handle.performTouchInput { down(center); up() }
        compose.runOnIdle { assertEquals(0, parentClicks) }

        compose.runOnIdle { callbackVersion = 2 }
        val dragDistance = with(compose.density) { 72.dp.toPx() }
        handle.performTouchInput {
            down(center)
            moveBy(Offset(0f, dragDistance), 300L)
            up()
        }
        compose.runOnIdle {
            assertEquals(0, parentClicks)
            assertEquals(listOf(2 to 1), moves)
        }
    }

    @Test
    fun longReorderListScrollsAndKeepsMovingAtTheViewportEdge() {
        var order by mutableStateOf((0 until 12).toList())
        var observedListState: LazyListState? = null
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                val listState = rememberLazyListState()
                observedListState = listState
                WhipReorderLazyColumn(
                    modifier = Modifier.width(280.dp).height(240.dp).testTag("edge-reorder-list"),
                    state = listState,
                ) {
                    items(order, key = { it }) { id ->
                        val index = order.indexOf(id)
                        val interaction = rememberWhipReorderInteractionState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .whipReorderItem(
                                    interaction,
                                    layoutPosition = index + 1,
                                    layoutScope = "edge-test",
                                ),
                        ) {
                            WhipReorderHandle(
                                label = "Item $id",
                                canMovePrevious = index > 0,
                                canMoveNext = index < order.lastIndex,
                                position = index + 1,
                                total = order.size,
                                interactionState = interaction,
                                moveWholeItem = true,
                                layoutScope = "edge-test",
                                onMove = { delta -> order = moveListItem(order, index, delta) },
                            )
                            Text("Item $id")
                        }
                    }
                }
            }
        }

        val edgeDrag = with(compose.density) { 92.dp.toPx() }
        compose.onNodeWithContentDescription("Reorder Item 2").performTouchInput {
            down(center)
            moveBy(Offset(0f, edgeDrag), 900L)
            compose.mainClock.advanceTimeBy(650L)
            up()
        }
        compose.runOnIdle {
            val firstVisible = requireNotNull(observedListState).firstVisibleItemIndex
            assertTrue("order=$order firstVisible=$firstVisible", order.indexOf(2) >= 4)
            assertTrue("order=$order firstVisible=$firstVisible", firstVisible > 0)
        }
    }

    @Test
    fun longHorizontalReorderRowScrollsAndKeepsMovingAtTheViewportEdge() {
        var order by mutableStateOf((0 until 12).toList())
        var observedScrollState: ScrollState? = null
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                val scrollState = androidx.compose.foundation.rememberScrollState()
                observedScrollState = scrollState
                WhipReorderHorizontalRow(
                    modifier = Modifier.width(240.dp).height(64.dp),
                    state = scrollState,
                ) {
                    order.forEach { id ->
                        val index = order.indexOf(id)
                        val interaction = rememberWhipReorderInteractionState()
                        Row(
                            modifier = Modifier
                                .width(64.dp)
                                .height(64.dp)
                                .whipReorderItem(
                                    interaction,
                                    axis = WhipReorderAxis.Horizontal,
                                    layoutPosition = index + 1,
                                    layoutScope = "horizontal-edge-test",
                                ),
                        ) {
                            WhipReorderHandle(
                                label = "Day $id",
                                canMovePrevious = index > 0,
                                canMoveNext = index < order.lastIndex,
                                position = index + 1,
                                total = order.size,
                                interactionState = interaction,
                                moveWholeItem = true,
                                axis = WhipReorderAxis.Horizontal,
                                layoutScope = "horizontal-edge-test",
                                onMove = { delta -> order = moveListItem(order, index, delta) },
                            )
                        }
                    }
                }
            }
        }

        val edgeDrag = with(compose.density) { 96.dp.toPx() }
        compose.onNodeWithContentDescription("Reorder Day 2").performTouchInput {
            down(center)
            moveBy(Offset(edgeDrag, 0f), 900L)
            compose.mainClock.advanceTimeBy(650L)
            up()
        }
        compose.runOnIdle {
            val scroll = requireNotNull(observedScrollState).value
            assertTrue("order=$order scroll=$scroll", order.indexOf(2) >= 4)
            assertTrue("order=$order scroll=$scroll", scroll > 0)
        }
    }

    @Test
    fun measuredInsertionPreviewMovesSiblingsAndCancelRestoresTheirGeometry() {
        val heights = listOf(64.dp, 112.dp, 80.dp)
        var committedMoves = 0
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipReorderLayout(itemSpacing = 12.dp) {
                    Column(
                        Modifier.width(280.dp).height(330.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                    repeat(3) { index ->
                        val interaction = rememberWhipReorderInteractionState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(heights[index])
                                .testTag("preview-row-$index")
                                .whipReorderItem(
                                    interaction,
                                    layoutPosition = index + 1,
                                    layoutScope = "variable-height-preview",
                                ),
                        ) {
                            WhipReorderHandle(
                                label = "Preview item $index",
                                canMovePrevious = index > 0,
                                canMoveNext = index < 2,
                                position = index + 1,
                                total = 3,
                                interactionState = interaction,
                                moveWholeItem = true,
                                layoutScope = "variable-height-preview",
                                onMove = { committedMoves += it },
                            )
                            Text("Preview item $index")
                        }
                    }
                }
                }
            }
        }

        val beforeFirst = compose.onNodeWithTag("preview-row-0").fetchSemanticsNode().boundsInRoot
        val beforeSecond = compose.onNodeWithTag("preview-row-1").fetchSemanticsNode().boundsInRoot
        val beforeThird = compose.onNodeWithTag("preview-row-2").fetchSemanticsNode().boundsInRoot
        // Leave enough room for Android's touch slop while remaining short of
        // the second variable-height insertion boundary (184 dp).
        val crossFirstTarget = with(compose.density) { 140.dp.toPx() }
        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription("Reorder Preview item 0").performTouchInput {
            down(center)
            moveBy(Offset(0f, crossFirstTarget), 300L)
        }
        compose.mainClock.advanceTimeBy(500L)

        val previewSecond = compose.onNodeWithTag("preview-row-1").fetchSemanticsNode().boundsInRoot
        val previewThird = compose.onNodeWithTag("preview-row-2").fetchSemanticsNode().boundsInRoot
        val previewSecondOffset = compose.onNodeWithTag("preview-row-1")
            .fetchSemanticsNode().config[WhipReorderPreviewOffsetKey]
        val previewThirdOffset = compose.onNodeWithTag("preview-row-2")
            .fetchSemanticsNode().config[WhipReorderPreviewOffsetKey]
        val previewState = compose.onNodeWithContentDescription("Reorder Preview item 0")
            .fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        assertTrue("Unexpected live reorder state: $previewState", previewState.startsWith("Position 2 of 3"))
        val expectedDisplacement = with(compose.density) { -(64.dp + 12.dp).toPx() }
        assertEquals(expectedDisplacement, previewSecondOffset, 1.5f)
        assertEquals(0f, previewThirdOffset, 1.5f)
        assertTrue(
            "Displaced siblings overlap: second=$previewSecond third=$previewThird offset=$previewSecondOffset",
            previewSecond.bottom + previewSecondOffset <= previewThird.top + previewThirdOffset,
        )

        compose.onNodeWithContentDescription("Reorder Preview item 0").performTouchInput { cancel() }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        val restoredSecond = compose.onNodeWithTag("preview-row-1").fetchSemanticsNode().boundsInRoot
        val restoredThird = compose.onNodeWithTag("preview-row-2").fetchSemanticsNode().boundsInRoot
        val restoredSecondOffset = compose.onNodeWithTag("preview-row-1")
            .fetchSemanticsNode().config[WhipReorderPreviewOffsetKey]
        val restoredThirdOffset = compose.onNodeWithTag("preview-row-2")
            .fetchSemanticsNode().config[WhipReorderPreviewOffsetKey]
        assertEquals(beforeSecond.top, restoredSecond.top, 1.5f)
        assertEquals(beforeThird.top, restoredThird.top, 1.5f)
        assertEquals(0f, restoredSecondOffset, 1.5f)
        assertEquals(0f, restoredThirdOffset, 1.5f)
        assertEquals(0, committedMoves)

        val farBeyondStart = with(compose.density) { -400.dp.toPx() }
        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription("Reorder Preview item 1").performTouchInput {
            down(center)
            moveBy(Offset(0f, farBeyondStart), 300L)
        }
        compose.mainClock.advanceTimeBy(500L)
        val clampedMiddle = compose.onNodeWithTag("preview-row-1").fetchSemanticsNode().boundsInRoot
        val clampedMiddleOffset = compose.onNodeWithTag("preview-row-1")
            .fetchSemanticsNode().config[WhipReorderPreviewOffsetKey]
        assertTrue(
            "Dragged item escaped above its collection: item=$clampedMiddle collectionTop=${beforeFirst.top}",
            clampedMiddle.top + clampedMiddleOffset >= beforeFirst.top - 1.5f,
        )
        assertTrue(
            compose.onNodeWithContentDescription("Reorder Preview item 1")
                .fetchSemanticsNode().config[SemanticsProperties.StateDescription]
                .startsWith("Position 1 of 3"),
        )
        compose.onNodeWithContentDescription("Reorder Preview item 1").performTouchInput { cancel() }
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun emojiPickerSearchesCommonChoicesAndPersistsReusableCustomEmoji() {
        var icon by mutableStateOf("✅")
        var saved by mutableStateOf(emptyList<CustomIdentityEmoji>())
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipEmojiPicker(
                    value = icon,
                    defaultEmoji = "✅",
                    onValueChange = { icon = it },
                    customEmojis = saved,
                    onSaveEmoji = { choice -> saved = (saved.filterNot { it.emoji == choice.emoji } + choice) },
                    onRemoveSavedEmoji = { emoji -> saved = saved.filterNot { it.emoji == emoji } },
                )
            }
        }

        compose.onNodeWithTag("emoji-picker-trigger").performClick()
        compose.onNodeWithTag("emoji-picker-search").performTextReplacement("dentist")
        compose.onNodeWithTag("emoji-preset-Dentist").assertIsDisplayed().performClick()
        assertEquals("🦷", icon)

        compose.onNodeWithTag("emoji-picker-trigger").performClick()
        compose.onNodeWithTag("emoji-picker-custom-option").performClick()
        compose.onNodeWithTag("emoji-picker-custom-input").assertIsDisplayed()

        compose.onNodeWithTag("emoji-picker-custom-input").performTextReplacement("🦊")
        compose.onNodeWithTag("emoji-picker-custom-input").assertTextContains("🦊")
        compose.onNodeWithTag("emoji-picker-custom-name").performTextReplacement("Forest Work")
        compose.onNodeWithTag("emoji-picker-custom-apply").performClick()
        assertEquals(
            "Custom emoji code points were ${icon.codePoints().toArray().joinToString()}",
            "🦊",
            icon,
        )
        assertEquals(listOf(CustomIdentityEmoji("🦊", "Forest Work")), saved)

        compose.onNodeWithTag("emoji-picker-trigger").performClick()
        compose.onNodeWithTag("emoji-picker-search").performTextReplacement("forest")
        compose.onNodeWithTag("emoji-saved-0").assertIsDisplayed().performClick()
        assertEquals("🦊", icon)

        compose.onNodeWithTag("emoji-picker-trigger").performClick()
        compose.onNodeWithTag("emoji-picker-search").performTextReplacement("🦊")
        compose.onNodeWithTag("emoji-picker-manage-saved").performClick()
        compose.onNodeWithTag("emoji-saved-0").performClick()
        assertTrue(saved.isEmpty())
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
                        destinations = listOf("Today", "Upcoming", "Archived"),
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
    fun explicitDirectDestinationsRemainStableAtLargeTextInRtl() {
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

        listOf("Today", "All", "Insights", "Connections", "Archived").forEach { label ->
            compose.onNodeWithTag("rtl-tab-$label").fetchSemanticsNode()
        }
        compose.onAllNodesWithContentDescription("More destinations").assertCountEquals(0)
        compose.onNodeWithTag("rtl-tab-Archived").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithTag("rtl-tab-Archived").assertIsDisplayed().assertIsSelected()
        compose.onNodeWithTag("rtl-tab-Today").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithTag("rtl-tab-Today").assertIsDisplayed().assertIsSelected()
    }

    @Test
    fun largeTextKeepsTwoDestinationsVisibleWhenTheRowHasRoom() {
        val largeText = Density(compose.density.density, fontScale = 1.5f)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides largeText) {
                WhipTheme(dynamicColor = false) {
                    Box(Modifier.width(320.dp)) {
                        DestinationTabBar(
                            selected = "Overview",
                            destinations = listOf("Overview", "Options"),
                            onSelect = {},
                            label = { it },
                            testTagPrefix = "roomy-tab",
                        )
                    }
                }
            }
        }

        listOf("Overview", "Options").forEach { label ->
            compose.onNodeWithTag("roomy-tab-$label").assertIsDisplayed()
        }
        compose.onAllNodesWithContentDescription("More destinations").assertCountEquals(0)
    }

    @Test
    fun destinationsShareEqualWidthWhenTheirFullRowFits() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Box(Modifier.width(360.dp)) {
                    DestinationTabBar(
                        selected = "Today",
                        destinations = listOf("Today", "All", "Insights"),
                        onSelect = {},
                        label = { it },
                        testTagPrefix = "equal-tab",
                    )
                }
            }
        }

        val widths = listOf("Today", "All", "Insights").map { label ->
            compose.onNodeWithTag("equal-tab-$label").fetchSemanticsNode().boundsInRoot.width
        }
        widths.drop(1).forEach { width -> assertEquals(widths.first(), width, 1.5f) }
        compose.onAllNodesWithContentDescription("More destinations").assertCountEquals(0)
    }

    @Test
    fun narrowLargeTextUsesFullLabelsAndRevealsTheSelectedDestination() {
        val largeText = Density(compose.density.density, fontScale = 2f)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides largeText) {
                WhipTheme(dynamicColor = false) {
                    Box(Modifier.width(240.dp)) {
                        DestinationTabBar(
                            selected = "Configuration",
                            destinations = listOf("Entries", "Insights", "Configuration"),
                            onSelect = {},
                            label = { it },
                            compactLabel = { if (it == "Configuration") "Config" else it },
                            testTagPrefix = "overflow-tab",
                        )
                    }
                }
            }
        }

        compose.onNodeWithText("Configuration").assertIsDisplayed()
        compose.onNodeWithTag("overflow-tab-Configuration").assertIsSelected()
        compose.onAllNodesWithText("Config").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("More destinations").assertCountEquals(0)
        val selected = compose.onNodeWithTag("overflow-tab-Configuration").fetchSemanticsNode().boundsInRoot
        val expectedRightEdge = with(compose.density) { (240.dp - 12.dp).toPx() }
        assertTrue("Selected destination was not brought into view: $selected", selected.right <= expectedRightEdge + 0.5f)
    }

    @Test
    fun compactProductivityHeaderKeepsDisclosureBeforeTheTrailingPrimaryAction() {
        compose.setContent {
            CompositionLocalProvider(LocalCompactItemLayout provides true) {
                WhipTheme(dynamicColor = false) {
                    Box(Modifier.width(320.dp)) {
                        ProductivityItemHeader(
                            itemType = "task",
                            itemName = "Review release notes",
                            emoji = "📋",
                            areaId = null,
                            areaName = "Main",
                            onEdit = null,
                            compactExpanded = false,
                            onCompactExpansionToggle = {},
                            compactExpansionTag = "header-disclosure",
                            primaryAction = {
                                Box(Modifier.size(48.dp).testTag("header-primary-action"))
                            },
                        )
                    }
                }
            }
        }

        val disclosure = compose.onNodeWithTag("header-disclosure", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val action = compose.onNodeWithTag("header-primary-action", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        assertTrue("Disclosure must precede the primary action: $disclosure vs $action", disclosure.right <= action.left)
        assertTrue("Primary action must remain the logical trailing control: $action", action.right <= with(compose.density) { 320.dp.toPx() } + 0.5f)
    }

    @Test
    fun datePickerTodayUsesTheConfiguredWhipDate() {
        val configuredToday = LocalDate.of(2035, 1, 2)
        var selectedDate: LocalDate? = null
        compose.setContent {
            CompositionLocalProvider(LocalWhipToday provides configuredToday) {
                WhipTheme(dynamicColor = false) {
                    WhipDatePickerDialog(
                        initialDate = LocalDate.of(2020, 6, 15),
                        onDismiss = {},
                        onDateSelected = { selectedDate = it },
                        preferWheelSelector = false,
                    )
                }
            }
        }

        compose.onNodeWithTag("date-picker-month-year").performClick()
        compose.onNodeWithTag("date-picker-today").performClick()
        compose.onNodeWithTag("date-picker-selected-date").assertTextEquals(
            configuredToday.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)),
        )
        compose.onNodeWithText("Set").performClick()
        compose.runOnIdle { assertEquals(configuredToday, selectedDate) }
    }

    @Test
    fun largeTextKeepsTwoSegmentChoicesVisibleWhenTheyFit() {
        val largeText = Density(compose.density.density, fontScale = 1.5f)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides largeText) {
                WhipTheme(dynamicColor = false) {
                    Box(Modifier.width(320.dp)) {
                        SegmentedChoiceBar(
                            selected = "Overview",
                            choices = listOf("Overview", "Options"),
                            onSelect = {},
                            label = { it },
                            modifier = Modifier.fillMaxSize(),
                            testTagPrefix = "roomy-choice",
                        )
                    }
                }
            }
        }

        compose.onNodeWithTag("roomy-choice-Overview").assertIsDisplayed().assertIsSelected()
        compose.onNodeWithTag("roomy-choice-Options").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Choose view. Selected Overview").assertCountEquals(0)
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
        val expectedInset = with(compose.density) { 12.dp.toPx() }

        assertEquals(before, after, 0.5f)
        assertEquals(expectedInset, after, 0.5f)
    }

    @Test
    fun allDestinationsRemainDirectWhenPriorityHintIsShorter() {
        var destination by mutableStateOf("Entries")
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Box(Modifier.width(480.dp)) {
                    DestinationTabBar(
                        selected = destination,
                        destinations = listOf("Entries", "Rules", "Options", "Insights"),
                        onSelect = { destination = it },
                        label = { it },
                        testTagPrefix = "capacity-tab",
                    )
                }
            }
        }

        listOf("Entries", "Rules", "Options", "Insights").forEach { label ->
            compose.onNodeWithTag("capacity-tab-$label").assertIsDisplayed()
        }
        compose.onAllNodesWithContentDescription("More destinations").assertCountEquals(0)
        compose.onNodeWithTag("capacity-tab-Options").performClick().assertIsSelected()
    }

    @Test
    fun destinationTabsUseShortVisibleLabelsWithoutLosingFullAccessibleNames() {
        var destination by mutableStateOf("Configuration")
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Box(Modifier.width(320.dp)) {
                    DestinationTabBar(
                        selected = destination,
                        destinations = listOf("Entries", "Insights", "Configuration"),
                        onSelect = { destination = it },
                        label = { it },
                        compactLabel = { if (it == "Configuration") "Config" else it },
                        testTagPrefix = "short-tab",
                    )
                }
            }
        }

        compose.onNodeWithText("Config").assertIsDisplayed()
        compose.onNodeWithContentDescription("Configuration").assertIsDisplayed()
        compose.onNodeWithTag("short-tab-Configuration").assertIsSelected()
    }

    @Test
    fun searchFilterSummariesRemainReadableAndTouchSizedWhenSeveralAreActive() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                UnifiedSearchDialog(
                    taskState = TaskUiState(loading = false),
                    habitState = HabitUiState(loading = false),
                    goalState = GoalUiState(loading = false),
                    gymState = GymUiState(loading = false),
                    trackState = TrackUiState(loading = false),
                    initialScope = WhipSearchEntryContext.Tasks.defaultSearchScope(),
                    onDismiss = {},
                    onSelect = {},
                )
            }
        }

        compose.onNodeWithTag("search-filter-disclosure").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithText("Habit").performClick()
        compose.onNodeWithTag("search-terms-Match Any")
            .performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithTag("search-terms-Match Any").assertIsSelected()
        compose.onNodeWithTag("search-filter-disclosure").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitUntil {
            compose.onAllNodesWithTag("search-active-match-any").fetchSemanticsNodes().size == 1
        }
        val minimumHeight = with(compose.density) { 48.dp.toPx() }
        val minimumWidth = with(compose.density) { 92.dp.toPx() }
        listOf("Task", "Habit").forEach { domain ->
            val bounds = compose.onNodeWithTag("search-active-domain-$domain").fetchSemanticsNode().boundsInRoot
            assertTrue("$domain is too short", bounds.height >= minimumHeight)
            assertTrue("$domain is too narrow", bounds.width >= minimumWidth)
        }
        assertTrue(compose.onNodeWithTag("search-active-match-any").fetchSemanticsNode().boundsInRoot.width >= minimumWidth)
    }

    @Test
    fun searchReindexesCompletedTaskDatesWhenTheActiveWhipZoneChangesWithoutClearingTheQuery() {
        var activeZone by mutableStateOf(ZoneId.of("UTC"))
        val completedAt = Instant.parse("2026-09-01T02:00:00Z").toEpochMilli()
        val task = WhipTask(
            id = 42,
            title = "Boundary task",
            notes = "",
            scheduleKind = ScheduleKind.Once,
            date = LocalDate.of(2026, 8, 31),
            recurrence = null,
            timeMinutes = null,
            reminderEnabled = false,
            archived = false,
            completedAtMillis = completedAt,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        val item = ScheduledTask(
            task = task,
            originalDate = task.date,
            scheduledDate = task.date,
            completedAtMillis = completedAt,
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                CompositionLocalProvider(LocalWhipZone provides activeZone) {
                    UnifiedSearchDialog(
                        taskState = TaskUiState(completed = listOf(item), loading = false),
                        habitState = HabitUiState(loading = false),
                        goalState = GoalUiState(loading = false),
                        gymState = GymUiState(loading = false),
                        trackState = TrackUiState(loading = false),
                        initialScope = WhipSearchEntryContext.Tasks.defaultSearchScope(),
                        onDismiss = {},
                        onSelect = {},
                    )
                }
            }
        }

        compose.onNodeWithTag("unified-search-query").performTextReplacement("Boundary after:2026-08-31")
        compose.waitUntil {
            compose.onAllNodesWithTag("unified-search-result-Task-42").fetchSemanticsNodes().size == 1
        }

        compose.runOnIdle { activeZone = ZoneId.of("America/Toronto") }
        compose.waitUntil {
            compose.onAllNodesWithTag("unified-search-result-Task-42").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithTag("unified-search-query").assertTextContains("Boundary after:2026-08-31")
    }

    @Test
    fun fourRemainingTaskDestinationsFitTheCompactCoverWidthWithoutClipping() {
        var destination by mutableStateOf("Today")
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Box(Modifier.width(360.dp)) {
                    DestinationTabBar(
                        selected = destination,
                        destinations = listOf("Today", "Inbox", "Upcoming", "History"),
                        onSelect = { destination = it },
                        label = { it },
                        testTagPrefix = "compact-task-tab",
                    )
                }
            }
        }

        val leftGutter = with(compose.density) { 12.dp.toPx() }
        val rightEdge = with(compose.density) { (360.dp - 12.dp).toPx() }
        val today = compose.onNodeWithTag("compact-task-tab-Today").fetchSemanticsNode().boundsInRoot
        val history = compose.onNodeWithTag("compact-task-tab-History").fetchSemanticsNode().boundsInRoot
        assertEquals(leftGutter, today.left, 0.5f)
        assertEquals(rightEdge, history.right, 0.5f)
        compose.onNodeWithText("History").assertIsDisplayed()
    }

    @Test
    fun destinationTabsKeepEveryPeerDirectAndStableWhenSelecting() {
        var destination by mutableStateOf("General")
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Box(Modifier.width(320.dp)) {
                        DestinationTabBar(
                            selected = destination,
                            destinations = listOf("General", "Organization", "Reminders", "Appearance", "Data & Backup"),
                        onSelect = { destination = it },
                        label = { it },
                        testTagPrefix = "reveal-tab",
                    )
                }
            }
        }

        listOf("General", "Organization", "Reminders", "Appearance", "Data & Backup").forEach { label ->
            compose.onNodeWithTag("reveal-tab-$label").fetchSemanticsNode()
        }
        compose.onNodeWithTag("reveal-tab-Data & Backup").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithTag("reveal-tab-Data & Backup").assertIsDisplayed().assertIsSelected()
        compose.onAllNodesWithContentDescription("More destinations").assertCountEquals(0)

        val expectedRightEdge = with(compose.density) { (320.dp - 12.dp).toPx() }
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onNodeWithTag("reveal-tab-Data & Backup")
                .fetchSemanticsNode().boundsInRoot.right <= expectedRightEdge + 0.5f
        }
        val pagesBounds = compose.onNodeWithTag("reveal-tab-Data & Backup")
            .fetchSemanticsNode().boundsInRoot
        assertEquals(expectedRightEdge, pagesBounds.right, 0.5f)
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
                                label = "Search Tasks & Subtasks",
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
        compose.onNodeWithContentDescription("Search Tasks & Subtasks").performClick()
        compose.onNodeWithText("Search controller opened").assertIsDisplayed()
        compose.onAllNodesWithTag("contextual-search-actions").assertCountEquals(0)
        compose.onAllNodesWithTag("contextual-search-field").assertCountEquals(0)
    }

    @Test
    fun pageHeaderActionsDoNotShiftTitleOrSupportingText() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Column(Modifier.width(520.dp)) {
                    WhipPageHeader(
                        title = "Tracks",
                        modifier = Modifier.testTag("plain-page-header"),
                        supportingText = "Structured logs for facts you want to record and compare.",
                    )
                    WhipPageHeader(
                        title = "Activity",
                        modifier = Modifier.testTag("action-page-header"),
                        supportingText = "A chronological view of Entries across visible Tracks.",
                    ) {
                        WhipPageIconAction(
                            icon = Icons.Outlined.Search,
                            label = "Search Track Activity",
                            onClick = {},
                        )
                        WhipPageIconAction(
                            icon = Icons.Outlined.FilterAlt,
                            label = "Filter Track Activity",
                            onClick = {},
                        )
                    }
                }
            }
        }

        val plainHeader = compose.onNodeWithTag("plain-page-header").fetchSemanticsNode().boundsInRoot
        val actionHeader = compose.onNodeWithTag("action-page-header").fetchSemanticsNode().boundsInRoot
        val plainTitle = compose.onNodeWithText("Tracks").fetchSemanticsNode().boundsInRoot
        val actionTitle = compose.onNodeWithText("Activity").fetchSemanticsNode().boundsInRoot
        val plainSupporting = compose.onNodeWithText("Structured logs for facts you want to record and compare.")
            .fetchSemanticsNode().boundsInRoot
        val actionSupporting = compose.onNodeWithText("A chronological view of Entries across visible Tracks.")
            .fetchSemanticsNode().boundsInRoot

        assertEquals(plainTitle.top - plainHeader.top, actionTitle.top - actionHeader.top, 0.5f)
        assertEquals(plainSupporting.top - plainHeader.top, actionSupporting.top - actionHeader.top, 0.5f)
        assertEquals(plainHeader.height, actionHeader.height, 0.5f)
        val searchAction = compose.onNodeWithContentDescription("Search Track Activity").fetchSemanticsNode().boundsInRoot
        assertTrue(searchAction.bottom <= actionSupporting.top)
    }

    @Test
    fun pageHeaderKeepsFollowingContentVisuallySeparated() {
        lateinit var density: Density
        compose.setContent {
            density = LocalDensity.current
            WhipTheme(dynamicColor = false) {
                Column(Modifier.width(360.dp)) {
                    WhipPageHeader(
                        title = "Today",
                        supportingText = "Check in, log a value, or continue a timer.",
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .testTag("first-page-content"),
                    )
                }
            }
        }

        val supportingText = compose
            .onNodeWithText("Check in, log a value, or continue a timer.")
            .fetchSemanticsNode()
            .boundsInRoot
        val firstContent = compose.onNodeWithTag("first-page-content").fetchSemanticsNode().boundsInRoot
        val minimumGap = with(density) { WhipSpacing.sibling.toPx() }

        assertTrue(
            "Page content should begin at least one sibling space below its supporting text",
            firstContent.top - supportingText.bottom >= minimumGap - 0.5f,
        )
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

    @Test
    fun trackScaleEntryUsesTheConfiguredFractionalIncrementAndCanBeCleared() {
        val field = TrackField(
            id = 1,
            uuid = "rating",
            trackId = 1,
            name = "Rating",
            type = TrackFieldType.Scale,
            position = 0,
            required = false,
            primary = false,
            showInList = true,
            dimension = null,
            unitId = null,
            precision = 1,
            scaleMin = 1,
            scaleMax = 5,
            scaleLowLabel = "Poor",
            scaleHighLabel = "Excellent",
            createdAtMillis = 1,
            updatedAtMillis = 1,
            scaleStep = 0.5,
        )
        var value by mutableStateOf(TrackValueDraft())
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Box(Modifier.width(360.dp)) {
                    TrackEntryField(
                        field = field,
                        value = value,
                        options = emptyList(),
                        units = BuiltInUnits.all,
                        showError = false,
                        onValue = { value = it },
                    )
                }
            }
        }

        repeat(6) { compose.onNodeWithTag("track-entry-scale-increase").performClick() }
        compose.onNodeWithTag("track-entry-scale-value").assertTextEquals("3.5")
        compose.runOnIdle { assertEquals(3.5, value.scaleValue ?: 0.0, 0.0) }

        compose.onNodeWithTag("track-entry-scale-decrease").performClick()
        compose.onNodeWithTag("track-entry-scale-value").assertTextEquals("3")
        compose.onNodeWithTag("track-entry-scale-clear").performClick()
        compose.onNodeWithTag("track-entry-scale-value").assertTextEquals("Not Set")
        compose.runOnIdle { assertEquals(null, value.scaleValue) }
    }

    @Test
    fun trackNumberEntrySupportsSignedDecimalsWithoutDestroyingPartialInput() {
        val field = TrackField(
            id = 1,
            uuid = "temperature",
            trackId = 1,
            name = "Temperature",
            type = TrackFieldType.Number,
            position = 0,
            required = false,
            primary = false,
            showInList = true,
            dimension = UnitDimension.Temperature,
            unitId = "celsius",
            precision = 6,
            scaleMin = null,
            scaleMax = null,
            scaleLowLabel = "",
            scaleHighLabel = "",
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        var value by mutableStateOf(TrackValueDraft(enteredUnitId = "celsius"))
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TrackEntryField(
                    field = field,
                    value = value,
                    options = emptyList(),
                    units = BuiltInUnits.all,
                    showError = false,
                    onValue = { value = it },
                )
            }
        }

        compose.onNodeWithTag("track-entry-number-temperature").performTextReplacement("-")
        compose.onNodeWithTag("track-entry-number-temperature").assertTextEquals("°C", "-")
        compose.runOnIdle { assertEquals(null, value.enteredNumber) }

        compose.onNodeWithTag("track-entry-number-temperature").performTextReplacement("-12.345678")
        compose.runOnIdle { assertEquals(-12.345678, value.enteredNumber ?: 0.0, 0.0) }
    }
}
