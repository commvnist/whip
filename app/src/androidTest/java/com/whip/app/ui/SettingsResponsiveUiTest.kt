package com.whip.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.whip.app.ui.theme.WhipTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsResponsiveUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun wideSectionSidebarScrollsAtLargeTextAndKeepsSelectionAndFocusSemantics() {
        var selected by mutableStateOf(SettingsSection.Appearance)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                WhipTheme(dynamicColor = false) {
                    Box(Modifier.size(width = 240.dp, height = 220.dp)) {
                        WideSettingsSectionSidebar(
                            selectedSection = selected,
                            onSectionSelected = { selected = it },
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                }
            }
        }

        compose.onNodeWithTag("settings-section-Appearance & Home").assertIsSelected()
        compose.onNodeWithTag("settings-wide-section-list")
            .performScrollToNode(hasTestTag("settings-section-About Whip"))

        val about = compose.onNodeWithTag("settings-section-About Whip").assertIsDisplayed()
        about.performSemanticsAction(SemanticsActions.RequestFocus).assertIsFocused()
        about.performClick().assertIsSelected().assertIsFocused()
        assertTrue(about.fetchSemanticsNode().boundsInRoot.height >= with(compose.density) { 48.dp.toPx() })
    }

    @Test
    fun sharedNavigationRowsRemainReachableAndClickableInRtl() {
        var clicks = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                WhipTheme(dynamicColor = false) {
                    Column(Modifier.width(320.dp)) {
                        NavigationRow(
                            title = "Machine configurations",
                            onClick = { clicks++ },
                            modifier = Modifier.testTag("rtl-navigation-row"),
                        )
                        WhipActionRow(
                            title = "Planning and units",
                            onClick = { clicks++ },
                            modifier = Modifier.testTag("rtl-action-row"),
                        )
                    }
                }
            }
        }

        val minimumTargetPx = with(compose.density) { 48.dp.toPx() }
        listOf("rtl-navigation-row", "rtl-action-row").forEach { tag ->
            compose.onNodeWithTag(tag).assertIsDisplayed().performClick()
            assertTrue(compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.height >= minimumTargetPx)
        }
        assertEquals(2, clicks)
    }

    @Test
    fun settingsActionPairsStackAtCompactLargeTextWithoutShrinkingTargets() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                WhipTheme(dynamicColor = false) {
                    Box(Modifier.width(320.dp)) {
                        ResponsiveSettingsActions(
                            first = { modifier ->
                                WhipOutlinedButton(
                                    onClick = {},
                                    modifier = modifier.testTag("settings-action-first"),
                                ) { androidx.compose.material3.Text("Review Access") }
                            },
                            second = { modifier ->
                                WhipButton(
                                    onClick = {},
                                    modifier = modifier.testTag("settings-action-second"),
                                ) { androidx.compose.material3.Text("Sync Now") }
                            },
                        )
                    }
                }
            }
        }

        val first = compose.onNodeWithTag("settings-action-first").assertIsDisplayed().getUnclippedBoundsInRoot()
        val second = compose.onNodeWithTag("settings-action-second").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(first.bottom <= second.top)
        assertTrue(first.let { it.bottom - it.top } >= 48.dp)
        assertTrue(second.let { it.bottom - it.top } >= 48.dp)
    }
}
