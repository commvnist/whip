package com.whip.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.ui.theme.WhipTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WhipSummaryBuilderTest {
    @get:Rule val compose = createComposeRule()

    @Test fun metricsReflowWithoutLosingExactReadingsOrTheirLabels() {
        val width = mutableStateOf(360.dp)
        val label = "Average distance after the complete recovery walk"
        compose.setContent {
            WhipTheme(darkTheme = false, dynamicColor = false) {
                Box(Modifier.width(width.value).verticalScroll(rememberScrollState())) {
                    WhipSummaryCard("Recorded distance") {
                        metric(label, "12345.678 mi")
                        metric("Latest", "0.001 mi")
                        fact("First-to-Latest Trend", "↑ 0.000125 mi")
                    }
                }
            }
        }
        val first = compose.onNodeWithText(label, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val latest = compose.onNodeWithText("Latest", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(first.top, latest.top)
        assertTrue(latest.left > first.left)
        compose.onNode(hasText(label) and hasText("12345.678 mi")).assertHasNoClickAction()
        compose.runOnIdle { width.value = 240.dp }
        val value = compose.onNodeWithText("12345.678 mi", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val stacked = compose.onNodeWithText("Latest", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(stacked.top > value.bottom)
        for (text in listOf(label, "12345.678 mi", "First-to-Latest Trend", "↑ 0.000125 mi")) {
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(text, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue("Complete reading: $text; ${layouts.map { "${it.size}, lines=${it.lineCount}, width=${it.didOverflowWidth}, height=${it.didOverflowHeight}" }}",
                layouts.isNotEmpty() && layouts.none { it.hasVisualOverflow })
        }
    }

    @Test fun shortSeriesNamesZeroAndNonzeroEvidenceWithoutClickActions() {
        compose.setContent {
            WhipTheme(darkTheme = false, dynamicColor = false) {
                Box(Modifier.width(320.dp)) {
                    WhipSummarySeries("Entry Frequency", "Sep 4 – Sep 10", listOf(
                        WhipSummaryPoint("Fri", "Friday, September 4", "0", 0.0),
                        WhipSummaryPoint("Thu", "Thursday, September 10", "12", 12.0),
                    ))
                }
            }
        }
        compose.onNodeWithContentDescription("Friday, September 4: 0").assertIsDisplayed().assertHasNoClickAction()
        compose.onNodeWithContentDescription("Thursday, September 10: 12").assertIsDisplayed().assertHasNoClickAction()
    }
}
