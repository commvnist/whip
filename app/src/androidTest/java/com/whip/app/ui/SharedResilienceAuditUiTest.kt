package com.whip.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.AndroidFontScale
import com.whip.app.AndroidFontScaleRule
import com.whip.app.assertDialogFontScale
import com.whip.app.captureVisualCatalogSurface
import com.whip.app.ui.theme.WhipTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedResilienceAuditUiTest {
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)

    @Test
    @AndroidFontScale
    fun listOwnedLoadFailureKeepsRetryAndFollowingContentReachable() {
        var retries = 0
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                LazyColumn(Modifier.width(320.dp).height(300.dp).testTag("load-parent-list")) {
                    item {
                        DomainLoadContent("Track Activity", PaddingValues(),
                            "The saved entries could not be loaded. ".repeat(12), { retries++ })
                    }
                    item { Text("Following activity content") }
                }
            }
        }
        compose.onNodeWithTag("load-parent-list").performScrollToNode(hasText("Try Again"))
        compose.onNodeWithText("Try Again").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, retries) }
        compose.onNodeWithTag("load-parent-list").performScrollToNode(hasText("Following activity content"))
        compose.onNodeWithText("Following activity content").assertIsDisplayed()
        captureVisualCatalogSurface("audit2.shared.list-load-failure")
    }

    @Test
    @AndroidFontScale
    fun longLoadFailureKeepsRetryReachableAtLargeText() {
        var retries = 0
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Box(Modifier.width(320.dp).height(300.dp)) {
                    DomainLoadContent(
                        domain = "Tracks",
                        innerPadding = PaddingValues(),
                        errorMessage = "The saved entries could not be loaded. ".repeat(12),
                        onRetry = { retries++ },
                    )
                }
            }
        }
        captureVisualCatalogSurface("audit2.shared.load-failure-initial")
        compose.onNodeWithText("Try Again").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, retries) }
        captureVisualCatalogSurface("audit2.shared.load-failure-retry")
    }

    @Test
    @AndroidFontScale
    fun shortInspectorKeepsContentSectionsAndActionsReachable() {
        var actionCount = 0
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                var selected by remember { mutableStateOf("overview") }
                EntityInspector(
                    entityType = "Goal",
                    title = "Build a consistent strength training routine for the autumn season",
                    emoji = "🎯",
                    context = "Personal fitness and wellbeing · Weekly consistency",
                    status = "Active · Ahead of pace",
                    sections = listOf(
                        EntityInspectorSection("overview", "Overview"),
                        EntityInspectorSection("history", "History"),
                        EntityInspectorSection("options", "Options"),
                    ),
                    selectedSectionId = selected,
                    onSelectSection = { selected = it },
                    onDismiss = {},
                    onEdit = {},
                    primaryAction = EntityInspectorPrimaryAction("record", "Record Progress", { actionCount++ }),
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        repeat(12) { Text("$selected evidence row $it", Modifier.testTag("audit-evidence-$it")) }
                    }
                }
            }
        }
        compose.assertDialogFontScale()
        captureVisualCatalogSurface("audit2.shared.short-inspector-initial")
        val content = compose.onNodeWithTag("entity-inspector-content-overview").fetchSemanticsNode().boundsInRoot
        assertTrue("Inspector must leave usable space for evidence", content.height / compose.density.density >= 96f)
        listOf("entity-inspector-title", "entity-inspector-context").forEach { tag ->
            compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                val layouts = mutableListOf<TextLayoutResult>()
                assertTrue(action(layouts))
                assertTrue("Scrollable identity must retain full $tag", layouts.all { !it.hasVisualOverflow })
            }
        }
        compose.onNodeWithTag("entity-inspector-status").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("entity-inspector-edit").assertIsDisplayed()
        compose.onNodeWithTag("entity-inspector-close").assertIsDisplayed()
        compose.onNodeWithTag("audit-evidence-11").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("entity-inspector-section-history").assertIsDisplayed().performClick()
        compose.onNodeWithTag("audit-evidence-0").assertIsDisplayed()
        compose.onNodeWithTag("entity-inspector-primary-record").assertIsDisplayed().performClick()
        compose.onNodeWithTag("entity-inspector-close").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, actionCount) }
        captureVisualCatalogSurface("audit2.shared.short-inspector-history")
    }
}
