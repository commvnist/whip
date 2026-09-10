package com.whip.app

import android.content.Intent
import android.view.accessibility.AccessibilityWindowInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.*
import com.whip.app.ui.toDraft
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** Real paged history and temporary view settings must survive changing forms and recreation. */
@RunWith(AndroidJUnit4::class)
class TrackHistoryControlsJourneyE2ETest {
    private val compose = createEmptyComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()

    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun pagedHistoryCanCombineAndRecoverFilters() = filters(false)
    @Test @AndroidFontScale
    fun pagedHistoryCanCombineAndRecoverFiltersAtLargeText() = filters(true)
    @Test fun removingSelectedSortFieldKeepsHistoryUsable() = removedSort(false)
    @Test @AndroidFontScale
    fun removingSelectedSortFieldKeepsHistoryUsableAtLargeText() = removedSort(true)

    private fun filters(large: Boolean) {
        val before = seed(125)
        val suffix = if (large) "large" else "ordinary"
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            open(before.track.id)
            history(hasText("Show 25 More · 25 Remaining")).performClick()
            history(hasText("Walk 125")).assertIsDisplayed()
            history(hasContentDescription("Filter Entries", substring = true)).performClick()
            compose.onNodeWithText("Add Condition").performScrollTo().performClick()
            choose("Field", "Entry Date", "Terrain")
            compose.onNodeWithText("Woodland route 18").performScrollTo().performClick()
            capture("tracks.history-controls.choice.$suffix")
            if (large) compose.assertDialogFontScale()
            scenario.recreate()
            compose.onNodeWithText("Woodland route 18").performScrollTo().assertIsSelected()
            compose.onNodeWithText("Add", substring = false).performClick()
            scenario.recreate()
            compose.onNodeWithText("Add Condition").performScrollTo().performClick()
            // A completed condition must not return as the draft of the next condition.
            choose("Field", "Entry Date", "Distance")
            choose("Operator", "equals", "is between")
            compose.onNode(hasSetTextAction() and hasText("Minimum", substring = true))
                .performScrollTo().performClick().performTextReplacement("10.5")
            compose.onNode(hasSetTextAction() and hasText("Maximum", substring = true))
                .performScrollTo().performClick().performTextReplacement("11")
            compose.waitUntil(10_000) {
                InstrumentationRegistry.getInstrumentation().uiAutomation.windows.any {
                    it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
                }
            }
            capture("tracks.history-controls.range-keyboard.$suffix")
            closeSoftKeyboard()
            compose.onNodeWithText("Add", substring = false).performClick()
            compose.onNodeWithText("Apply Filters").performClick()
            history(hasText("Walk 108")).assertIsDisplayed()
            compose.onAllNodesWithContentDescription("Edit Entry", substring = true).assertCountEquals(1)
            history(hasText("Match All")).assertIsDisplayed()
            history(hasText("Woodland route 18", substring = true)).assertIsDisplayed()
            capture("tracks.history-controls.match-all.$suffix")
            scenario.recreate()
            history(hasText("Walk 108")).performClick()
            compose.onNodeWithContentDescription("Close Track Entry details").performClick()
            history(hasContentDescription("Filter Entries", substring = true)).performClick()
            val compactMode = compose.onNodeWithContentDescription("Choose view. Selected Match All")
            if (compose.onAllNodesWithContentDescription("Choose view. Selected Match All").fetchSemanticsNodes().isNotEmpty()) {
                compactMode.performScrollTo().performClick()
            }
            compose.onNodeWithText("Match Any").performScrollTo().performClick()
            compose.onNodeWithText("Apply Filters").performClick()
            history(hasText("Walk 18")).assertIsDisplayed()
            history(hasText("Match Any")).assertIsDisplayed()
            capture("tracks.history-controls.match-any.$suffix")
            history(hasText("Clear All")).performClick()
            history(hasText("Walk 1")).assertIsDisplayed()
            val insights = compose.onNodeWithTag("track-destination-Track Insights")
            if (!insights.isDisplayed()) insights.performScrollTo()
            insights.performClick()
            compose.onNodeWithTag("track-insights-list").performScrollToNode(hasContentDescription("Filter Insights"))
            compose.onNodeWithContentDescription("Filter Insights").performClick()
            compose.onNodeWithText("Add Condition").performScrollTo().performClick()
            choose("Field", "Entry Date", "Name")
            compose.onNode(hasSetTextAction()).performScrollTo().performClick().performTextReplacement("Walk 108")
            closeSoftKeyboard()
            compose.onNodeWithText("Add", substring = false).performClick()
            compose.onNodeWithText("Apply Filters").performClick()
            compose.onNodeWithTag("track-insights-list").performScrollToNode(hasText("Name is Walk 108"))
            compose.onNodeWithText("Name is Walk 108").assertIsDisplayed()
            capture("tracks.history-controls.insights.$suffix")
            val entries = compose.onNodeWithTag("track-destination-Entries")
            if (!entries.isDisplayed()) entries.performScrollTo()
            entries.performClick()
            history(hasText("Walk 1")).assertIsDisplayed()
            assertEquals(before, projection(before.track.id))
        }
    }

    private fun removedSort(large: Boolean) {
        val before = seed(3)
        val suffix = if (large) "large" else "ordinary"
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            open(before.track.id)
            history(hasContentDescription("Sort Entries by Entry Date, Descending")).performClick()
            choose("Sort by", "Entry Date", "Notes")
            // A valid external definition update while this view remains alive.
            runBlocking {
                val draft = before.track.toDraft(before.fields, before.options).let { it.copy(fields = it.fields.filterNot { f -> f.name == "Notes" }) }
                val boundary = requireNotNull(app.trackRepository.definitionBoundary(before.track.id))
                val review = app.trackRepository.reviewDefinitionUpdate(before.track.id, draft, boundary)
                app.trackRepository.update(before.track.id, draft, boundary, review)
            }
            compose.waitForIdle()
            compose.onNodeWithContentDescription("Sort by: Entry Date").performScrollTo().assertIsDisplayed()
            capture("tracks.history-controls.sort-recovered.$suffix")
            if (large) compose.assertDialogFontScale()
            scenario.recreate()
            compose.onNodeWithContentDescription("Sort by: Entry Date").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Done", substring = false).performClick()
            history(hasContentDescription("Sort Entries by Entry Date, Descending")).performClick()
            choose("Sort by", "Entry Date", "Distance")
            choose("Order", "Descending", "Ascending")
            compose.onNodeWithText("Done", substring = false).performClick()
            history(hasText("Walk 1")).assertIsDisplayed()
            assertEquals(before.entries, projection(before.track.id).entries)
        }
    }

    private fun choose(label: String, current: String, next: String) {
        val control = compose.onNodeWithContentDescription("$label: $current")
        if (!control.isDisplayed()) control.performScrollTo()
        control.performClick()
        compose.onNodeWithContentDescription("$label option: $next").performScrollTo().performClick()
    }
    private fun open(id: Long) {
        compose.onNodeWithContentDescription("Tracks tab").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("track-list").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("track-list").performScrollToNode(hasTestTag("track-card-$id"))
        compose.onNodeWithTag("track-card-$id").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("track-entry-list").fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("track-entry-page-loading").fetchSemanticsNodes().isEmpty() }
    }
    private fun history(matcher: SemanticsMatcher): SemanticsNodeInteraction {
        compose.onNodeWithTag("track-entry-list").performScrollToNode(matcher)
        return compose.onNode(matcher).performScrollTo()
    }
    private fun capture(id: String) { compose.waitForIdle(); captureVisualCatalogSurface(id) }
    private fun projection(id: Long) = runBlocking { requireNotNull(app.trackRepository.projection(id)) }
    private fun seed(count: Int): TrackProjection = runBlocking {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { AppSettings(setupCompleted = true, dynamicColor = false, themeMode = AppThemeMode.Light) }
        val id = app.trackRepository.create(TrackDraft("Walking history", fields = listOf(
            TrackFieldDraft("Name", TrackFieldType.ShortText, primary = true),
            TrackFieldDraft("Terrain", TrackFieldType.SingleChoice, options = (1..18).map { TrackChoiceOptionDraft("Woodland route $it") }),
            TrackFieldDraft("Distance", TrackFieldType.Number, dimension = UnitDimension.Distance, unitId = "distance_m", precision = 1),
            TrackFieldDraft("Notes", TrackFieldType.LongText),
        )))
        val form = projection(id)
        repeat(count) { index ->
            app.trackRepository.addEntry(id, TrackEntryDraft(
                entryDate = app.clock.today().minusDays(index.toLong()),
                values = mapOf(
                    form.primaryField.uuid to TrackValueDraft(textValue = "Walk ${index + 1}"),
                    form.fields.single { it.name == "Terrain" }.uuid to TrackValueDraft(choiceOptionUuid = form.options[index % 18].uuid),
                    form.fields.single { it.name == "Distance" }.uuid to TrackValueDraft(enteredNumber = index / 10.0, enteredUnitId = "distance_m"),
                ),
            ))
        }
        projection(id)
    }
}
