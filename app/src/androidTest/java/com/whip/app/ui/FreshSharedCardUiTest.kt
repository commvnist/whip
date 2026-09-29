package com.whip.app.ui

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.whip.app.ui.theme.WhipTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FreshSharedCardUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun renamingAnEntityPreservesItsScrolledInspectorSection() {
        val title = mutableStateOf("Reading goal")
        compose.setContent {
            WhipTheme {
                EntityInspector(
                    entityType = "Goal", title = title.value, emoji = "📚", context = "Main", status = "Active",
                    sections = listOf(EntityInspectorSection("history", "History")),
                    selectedSectionId = "history", onSelectSection = {}, onDismiss = {}, onEdit = {},
                    stateKey = "goal-42",
                ) {
                    LazyColumn(Modifier.testTag("inspector-history")) {
                        items(40) { Text("Recorded entry $it", Modifier.padding(16.dp)) }
                    }
                }
            }
        }
        compose.onNodeWithTag("inspector-history").performScrollToIndex(25)
        compose.onNodeWithText("Recorded entry 25").assertIsDisplayed()
        compose.runOnIdle { title.value = "Read more books" }
        compose.onNodeWithText("Read more books").assertIsDisplayed()
        compose.onNodeWithText("Recorded entry 25").assertIsDisplayed()
    }

    @Test fun enlargedCardIdentityRemainsCompleteBesideItsActions() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                WhipTheme {
                    WhipItemCard(Modifier.width(320.dp)) {
                        WhipProductivityItemContent(
                            itemType = "Goal", itemName = "Build emergency savings for our family", emoji = "🎯",
                            titleModifier = Modifier.testTag("card-identity"),
                        ) {
                            disclosure(false, onToggle = {})
                            primaryAction { WhipTextButton(onClick = {}) { Text("Log") } }
                            summary { text("Ready to start") }
                        }
                    }
                }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag("card-identity", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        layouts.forEach { layout ->
            assertFalse(layout.hasVisualOverflow)
            repeat(layout.lineCount) { assertFalse(layout.isLineEllipsized(it)) }
        }
    }
}
