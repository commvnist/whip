package com.whip.app.health

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.whip.app.ui.theme.WhipTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HealthPermissionsRationaleUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun compactLargeTextDarkLayoutKeepsCloseStableAndEveryExplanationScrollable() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                WhipTheme(darkTheme = true, dynamicColor = false) {
                    Box(Modifier.size(width = 320.dp, height = 480.dp)) {
                        HealthPermissionsRationaleContent(onClose = {})
                    }
                }
            }
        }

        compose.onNodeWithTag("health-rationale-surface").assertIsDisplayed()
        compose.onNodeWithTag("health-rationale-close").assertIsDisplayed()
        val closeBeforeScroll = compose.onNodeWithTag("health-rationale-close")
            .fetchSemanticsNode().boundsInRoot

        compose.onNodeWithTag("health-rationale-list")
            .performScrollToNode(hasTestTag("health-rationale-sync-retention"))
        compose.onNodeWithTag("health-rationale-sync-retention").assertIsDisplayed()
        compose.onNodeWithTag("health-rationale-close").assertIsDisplayed()

        val closeAfterScroll = compose.onNodeWithTag("health-rationale-close")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(closeBeforeScroll.top == closeAfterScroll.top)
        assertTrue(closeAfterScroll.bottom - closeAfterScroll.top >= with(compose.density) { 48.dp.toPx() })
    }
}
