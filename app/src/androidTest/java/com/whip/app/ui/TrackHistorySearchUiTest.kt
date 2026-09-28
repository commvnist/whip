package com.whip.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import com.whip.app.AndroidFontScale
import com.whip.app.AndroidFontScaleRule
import com.whip.app.captureVisualCatalogSurface
import com.whip.app.domain.*
import com.whip.app.ui.theme.WhipTheme
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import androidx.test.espresso.Espresso.closeSoftKeyboard
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.Assert.assertEquals

class TrackHistorySearchUiTest {
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)

    @Test fun changedQueriesCannotShowStaleMatchesAndFailuresRemainRetryable() = verifySearch("ordinary")
    @Test @AndroidFontScale fun changedQueriesCannotShowStaleMatchesAndFailuresRemainRetryableAtLargeText() = verifySearch("large")

    @Test fun numericConditionReopeningPreservesSubMicroCanonicalBound() {
        val field = TrackField(1, "distance", 1, "Distance", TrackFieldType.Number, 0, true, true, true,
            UnitDimension.Distance, "kilometre", 6, null, null, "", "", 1, 1)
        val track = Track(id = 1, uuid = "track", name = "Tiny readings", description = "", icon = "📓", areaId = "main", area = "Main",
            tags = emptyList(), pinned = false, archived = false, position = 0, createdAtMillis = 1, updatedAtMillis = 1)
        val initial = TrackCondition(field.uuid, TrackConditionOperator.GreaterThan, numberValue = 0.000012345678)
        var saved: TrackCondition? = null
        compose.setContent { WhipTheme(dynamicColor = false) {
            TrackConditionEditor(TrackProjection(track, listOf(field), emptyList(), emptyList()), onDismiss = {},
                today = LocalDate.of(2026, 9, 28), initial = initial, onSave = { saved = it })
        } }
        val displayed = compose.onNode(hasSetTextAction()).performScrollTo()
            .fetchSemanticsNode().config[SemanticsProperties.EditableText].text.toWhipDoubleOrNull()
        val unit = requireNotNull(BuiltInUnits.get(requireNotNull(field.unitId)))
        assertEquals(initial.numberValue!!, unit.toCanonical(requireNotNull(displayed)), Math.ulp(initial.numberValue!!))
        compose.onNodeWithText("Save Condition").performClick()
        compose.runOnIdle { assertEquals(initial.numberValue!!, requireNotNull(saved).numberValue!!, Math.ulp(initial.numberValue!!)) }
    }

    private fun verifySearch(suffix: String) {
        val today = LocalDate.of(2026, 9, 28)
        val field = TrackField(1, "name", 1, "Name", TrackFieldType.ShortText, 0, true, true, true, null, null, 0, null, null, "", "", 1, 1)
        val entries = listOf("Alpha entry", "Beta entry").mapIndexed { index, name ->
            val id = index + 1L
            TrackEntryProjection(TrackEntry(id, "entry-$id", 1, today, id, id), mapOf(1L to
                TrackFieldValue(id, "value-$id", id, 1, textValue = name, createdAtMillis = 1, updatedAtMillis = 1)))
        }
        val projection = TrackProjection(Track(id = 1, uuid = "track", name = "Search evidence", description = "", icon = "📓",
            areaId = "main", area = "Main", tags = emptyList(), pinned = false, archived = true, position = 0,
            createdAtMillis = 1, updatedAtMillis = 1), listOf(field), emptyList(), entries)
        val oldResult = CompletableDeferred<Set<Long>>()
        val newResult = CompletableDeferred<Set<Long>>()
        val started = AtomicInteger(0)
        val retryCalls = AtomicInteger(0)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TrackEntriesPage(projection, today, emptyList(), {}, {}, {},
                    searchEntryIds = { query -> when (query) {
                        "Alpha" -> { started.incrementAndGet(); withContext(NonCancellable) { oldResult.await() } }
                        "Beta" -> { started.incrementAndGet(); newResult.await() }
                        else -> if (retryCalls.incrementAndGet() == 1) error("Search storage is temporarily unavailable") else setOf(1L)
                    } },
                    loadEntryPage = { _, _ -> TrackEntryPage(entries, 0, entries.size) }, onRestore = {})
            }
        }
        compose.onNodeWithContentDescription("Search Entries in Search evidence").performClick()
        compose.onNodeWithTag("track-entry-search").performTextReplacement("Alpha")
        compose.waitUntil(5_000) { started.get() == 1 }
        compose.onNodeWithTag("track-entry-search").performTextReplacement("Beta")
        closeSoftKeyboard()
        compose.waitUntil(5_000) { started.get() == 2 }
        compose.runOnIdle { oldResult.complete(setOf(1L)) }
        compose.onAllNodesWithText("Alpha entry").assertCountEquals(0)
        compose.onAllNodesWithText("No Matching Entries").assertCountEquals(0)
        compose.onNodeWithTag("track-entry-list").performScrollToNode(hasTestTag("track-entry-search-loading"))
        captureVisualCatalogSurface("ux-audit-2.tracks.search-pending.$suffix")
        compose.runOnIdle { newResult.complete(setOf(2L)) }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("track-entry-search-loading").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("track-entry-list").performScrollToNode(hasText("Beta entry"))
        compose.onNodeWithText("Beta entry").assertIsDisplayed()
        compose.onNodeWithTag("track-entry-list").performScrollToNode(hasTestTag("track-entry-search"))
        compose.onNodeWithTag("track-entry-search").performTextReplacement("retry")
        closeSoftKeyboard()
        compose.waitUntil(5_000) { retryCalls.get() == 1 }
        compose.onNodeWithTag("track-entry-list").performScrollToNode(hasTestTag("track-entry-search-error"))
        compose.onNodeWithText("Search Unavailable").assertIsDisplayed()
        compose.onAllNodesWithText("No Matching Entries").assertCountEquals(0)
        captureVisualCatalogSurface("ux-audit-2.tracks.search-error.$suffix")
        compose.onNodeWithTag("track-entry-list").performScrollToNode(hasText("Retry Search"))
        compose.onNodeWithText("Retry Search").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("track-entry-search-error").fetchSemanticsNodes().isEmpty() && compose.onAllNodesWithTag("track-entry-search-loading").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("track-entry-list").performScrollToNode(hasText("Alpha entry"))
        compose.onNodeWithText("Alpha entry").assertIsDisplayed()
        compose.onNodeWithTag("track-entry-list").performScrollToNode(hasTestTag("track-entry-search"))
        compose.onNodeWithTag("track-entry-search").assertTextContains("retry")
    }
}
