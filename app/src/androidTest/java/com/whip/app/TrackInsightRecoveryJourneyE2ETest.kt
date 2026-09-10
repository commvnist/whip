package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Empty views explain scope and recover without changing saved evidence. */
@RunWith(AndroidJUnit4::class)
class TrackInsightRecoveryJourneyE2ETest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun areaWithoutActiveTracksRetainsArchivedAndOtherAreaEvidence() {
        reset()
        val outside = runBlocking { app.areaRepository.create("Outdoors") }
        val other = seed("Trail observations", outside, populated = true)
        val archived = seed("Earlier observations", archived = true, populated = true)
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            openTracks()
            changeArea("Main")
            workspace("Insights")
            compose.onNodeWithText("No Active Tracks in This View").assertIsDisplayed()
            compose.onAllNodesWithText("Entry Frequency").assertCountEquals(0)
            capture("tracks.insights.empty-area", "track-workspace-insights-list")
            compose.onNodeWithText("View Archived").performClick()
            compose.onNodeWithTag("track-card-${archived.track.id}").performClick()
            detail("Track Insights")
            compose.onNodeWithText("All Entries").assertIsDisplayed()
            scenario.recreate()
            compose.onNodeWithText("All Entries").assertIsDisplayed()
            workspace("Insights")
            compose.onNodeWithText("Create Track").performClick()
            compose.onNodeWithTag("track-editor-name").performTextReplacement("New daily observations")
            closeSoftKeyboard()
            compose.onNodeWithText("Save", substring = false).performClick()
            compose.waitUntil(15_000) { runBlocking { app.trackRepository.projections.first() }.any { it.track.name == "New daily observations" } }
            val created = runBlocking { app.trackRepository.projections.first() }.single { it.track.name == "New daily observations" }
            assertEquals(archived.track.areaId, created.track.areaId)
            workspace("Insights")
            compose.onNodeWithText("No Entries to Summarize").assertIsDisplayed()
            changeArea("Outdoors")
            compose.onNodeWithText("Total Entries").assertIsDisplayed()
            assertEquals(other, projection(other.track.id))
            assertEquals(archived, projection(archived.track.id))
        }
    }

    @Test fun entrylessTrackCanReturnToRecording() {
        reset()
        val before = seed("Walking and recovery observations")
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            openTracks()
            workspace("Insights")
            compose.onNodeWithText("No Entries to Summarize").assertIsDisplayed()
            compose.onAllNodesWithText("Entry Frequency").assertCountEquals(0)
            capture("tracks.insights.no-entries", "track-workspace-insights-list")
            compose.onNodeWithText("Open Tracks").performClick()
            compose.onNodeWithTag("track-card-${before.track.id}").performClick()
            detail("Track Insights")
            compose.onNodeWithText("No Entries Yet").assertIsDisplayed()
            compose.onAllNodesWithText("All Entries").assertCountEquals(0)
            capture("tracks.detail.insights.no-entries", "track-insights-list")
            scenario.recreate()
            assertEquals(before, projection(before.track.id))
            compose.onNodeWithText("Add Entry").performClick()
            compose.onNodeWithTag("track-entry-short-text-${before.primaryField.uuid}")
                .performTextReplacement("A first recorded walk")
            closeSoftKeyboard()
            compose.onNodeWithText("Add", substring = false).performClick()
            compose.waitUntil(15_000) { projection(before.track.id).entries.size == 1 }
            val saved = projection(before.track.id)
            assertEquals(before.fields, saved.fields)
            assertEquals(before.options, saved.options)
            assertEquals("A first recorded walk", saved.primaryText(saved.entries.single()))
            detail("Track Insights")
            compose.onNodeWithText("All Entries").assertIsDisplayed()
            scenario.recreate()
            compose.onNodeWithText("All Entries").assertIsDisplayed()
            capture("tracks.detail.insights.first-entry", "track-insights-list")
            assertEquals(saved, projection(before.track.id))
        }
    }

    @Test fun unmatchedInsightFiltersCanRecoverExactHistory() {
        reset()
        val before = seed("Walking observations", populated = true)
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            openTracks()
            compose.onNodeWithTag("track-card-${before.track.id}").performClick()
            detail("Track Insights")
            compose.onNodeWithContentDescription("Filter Insights").performClick()
            compose.onNodeWithText("Add Condition").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Field: Entry Date").performScrollTo().performClick()
            compose.onNodeWithText("Name").performClick()
            compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("Unrecorded observation")
            closeSoftKeyboard()
            compose.onNodeWithText("Add", substring = false).performClick()
            compose.onNodeWithText("Apply Filters").performClick()
            compose.onNodeWithText("No Matching Entries").assertIsDisplayed()
            compose.onAllNodesWithText("Matching Entries").assertCountEquals(0)
            capture("tracks.detail.insights.no-matches", "track-insights-list")
            scenario.recreate()
            compose.onNodeWithText("No Matching Entries").assertIsDisplayed()
            compose.onNodeWithTag("track-insights-list").performScrollToNode(hasText("Clear All"))
            compose.onNodeWithText("Clear All").performClick()
            compose.onNodeWithText("All Entries").assertIsDisplayed()
            capture("tracks.detail.insights.recovered", "track-insights-list")
            assertEquals(before, projection(before.track.id))
        }
    }

    @Test fun archivedEntrylessTrackOffersReadOnlyRecovery() {
        reset()
        val before = seed("Earlier empty observations", archived = true)
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            openTracks()
            workspace("Archived")
            compose.onNodeWithTag("track-card-${before.track.id}").performClick()
            detail("Track Insights")
            compose.onNodeWithText("No Entries Yet").assertIsDisplayed()
            compose.onAllNodesWithText("Add Entry").assertCountEquals(0)
            capture("tracks.detail.insights.archived-empty", "track-insights-list")
            scenario.recreate()
            compose.onNodeWithText("View Entries").performClick()
            compose.onNodeWithText("Restore Track").assertIsDisplayed()
            assertEquals(before, projection(before.track.id))
        }
    }

    private fun reset() = runBlocking {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { AppSettings(setupCompleted = true, dynamicColor = false, themeMode = AppThemeMode.Light) }
    }
    private fun seed(name: String, area: String? = null, archived: Boolean = false, populated: Boolean = false): TrackProjection = runBlocking {
        val id = app.trackRepository.create(TrackDraft(name, areaId = area, fields = listOf(
            TrackFieldDraft("Name", TrackFieldType.ShortText, primary = true),
            TrackFieldDraft("Distance", TrackFieldType.Number, dimension = UnitDimension.Distance, unitId = "mile", precision = 3),
        )))
        val initial = requireNotNull(app.trackRepository.projection(id))
        if (populated) app.trackRepository.addEntry(id, TrackEntryDraft(entryDate = app.clock.today(), values = mapOf(
            initial.primaryField.uuid to TrackValueDraft(textValue = "A recorded walk"),
            initial.fields.single { it.name == "Distance" }.uuid to TrackValueDraft(enteredNumber = 1.25, enteredUnitId = "mile"),
        )))
        if (archived) app.trackRepository.setArchived(id, true)
        requireNotNull(app.trackRepository.projection(id))
    }
    private fun projection(id: Long) = runBlocking { requireNotNull(app.trackRepository.projection(id)) }
    private fun openTracks() {
        compose.onNodeWithContentDescription("Tracks tab").performClick()
        compose.onNodeWithTag("track-workspace-navigation").assertIsDisplayed()
    }
    private fun workspace(name: String) = compose.onNodeWithTag("track-workspace-destination-$name").performClick()
    private fun detail(name: String) {
        val tab = compose.onNodeWithTag("track-destination-$name")
        if (!tab.isDisplayed()) tab.performScrollTo()
        tab.performClick()
    }
    private fun changeArea(name: String) {
        compose.onNodeWithTag("workspace-area-action").performClick()
        compose.onNode(hasText("$name ·", substring = true) and hasAnyAncestor(isPopup())).performClick()
    }
    private fun capture(id: String, list: String) {
        compose.onNodeWithTag(list).assertIsDisplayed()
        compose.waitForIdle()
        captureVisualCatalogSurface(id)
    }
}
