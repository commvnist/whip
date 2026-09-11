package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Normal-scale record reading and exact actions through both native Track entry points. */
@RunWith(AndroidJUnit4::class)
class RecordItemBuilderJourneyE2ETest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    private val title = "Riverside loop before work"

    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun recordsKeepTheirReadingOrderAndExactActions() {
        val before = runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { AppSettings(setupCompleted = true, dynamicColor = false, themeMode = AppThemeMode.Light) }
            val id = app.trackRepository.create(TrackDraft("Weekend walks and trail notes", icon = "🥾", fields = listOf(
                TrackFieldDraft("Walk", TrackFieldType.ShortText, primary = true),
                TrackFieldDraft("Terrain", TrackFieldType.ShortText, showInList = true),
                TrackFieldDraft("Weather", TrackFieldType.ShortText, showInList = true),
            )))
            val form = requireNotNull(app.trackRepository.projection(id))
            listOf(title, "Oak woodland circuit", "Neighbourhood evening walk").forEachIndexed { index, name ->
                app.trackRepository.addEntry(id, TrackEntryDraft(entryDate = app.clock.today().minusDays(index.toLong()), values = mapOf(
                    form.primaryField.uuid to TrackValueDraft(textValue = name),
                    form.fields.single { it.name == "Terrain" }.uuid to TrackValueDraft(textValue = "Gravel paths and forest trails"),
                    form.fields.single { it.name == "Weather" }.uuid to TrackValueDraft(textValue = "Cool morning with light rain"),
                )))
            }
            requireNotNull(app.trackRepository.projection(id))
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Tracks tab").performClick()
            compose.onNodeWithTag("track-workspace-destination-Insights").performClick()
            val fields = compose.onNodeWithText("Fields").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue("Insights retains the Track's three Fields", compose.onAllNodesWithText("3").fetchSemanticsNodes().any {
                kotlin.math.abs(it.boundsInRoot.center.y - fields.center.y) <= 1f
            })
            compose.assertWorkspaceMeasure(1000.dp)
            capture("tracks.record-builder.insights")
            compose.onNodeWithTag("track-workspace-destination-Tracks").performClick()
            compose.onNodeWithTag("track-list").performScrollToNode(hasTestTag("track-card-${before.track.id}"))
            compose.onNodeWithTag("track-card-${before.track.id}").performClick()
            entry(title)
            compose.assertWorkspaceMeasure(1000.dp)
            // A flat wide Tracks workspace owns its list/detail panes itself.
            compose.onAllNodesWithTag("expanded-support-pane").assertCountEquals(0)
            capture("tracks.record-builder.entries")
            compose.onNodeWithText(title).performClick()
            compose.onNodeWithTag("track-entry-detail-surface").assertIsDisplayed()
            compose.onNodeWithContentDescription("Close Track Entry details").performClick()
            compose.onNodeWithContentDescription("Edit Entry $title").performClick()
            compose.onNodeWithTag("track-entry-short-text-${before.primaryField.uuid}").assertTextContains(title)
            compose.onNodeWithContentDescription("Close Entry Editor").performClick()
            compose.onNodeWithContentDescription("More Actions for $title").performClick()
            compose.onNodeWithText("Delete Entry").performClick()
            compose.onNodeWithTag("track-entry-row-delete-confirmation").assertIsDisplayed()
            compose.onNodeWithText("Keep Entry").performClick()
            compose.onNodeWithContentDescription("Back to Tracks").performClick()
            compose.onNodeWithTag("track-workspace-destination-Activity").performClick()
            compose.onNodeWithText(title).assertIsDisplayed()
            compose.assertWorkspaceMeasure(1000.dp)
            capture("tracks.record-builder.activity")
            compose.onNodeWithContentDescription("Edit Entry $title").performClick()
            compose.onNodeWithTag("track-entry-short-text-${before.primaryField.uuid}").assertTextContains(title)
            compose.onNodeWithContentDescription("Close Entry Editor").performClick()
            compose.onNodeWithContentDescription("More Actions for $title").performClick()
            capture("tracks.record-builder.actions")
            compose.onNodeWithText("Open Track").performClick()
            entry(title)
            scenario.recreate()
            entry(title)
            runBlocking { app.trackRepository.setArchived(before.track.id, true) }
            compose.onNodeWithContentDescription("Back to Tracks").performClick()
            compose.onNodeWithTag("track-workspace-destination-Archived").performClick()
            compose.onNodeWithTag("track-card-${before.track.id}").performClick()
            entry(title)
            compose.onAllNodesWithContentDescription("Edit Entry $title").assertCountEquals(0)
            compose.onAllNodesWithContentDescription("More Actions for $title").assertCountEquals(0)
            capture("tracks.record-builder.archived")
            compose.onNodeWithText(title).performClick()
            compose.onNodeWithTag("track-entry-detail-surface").assertIsDisplayed()
            val after = runBlocking { requireNotNull(app.trackRepository.projection(before.track.id)) }
            assertEquals(before.entries, after.entries)
            assertEquals(before.fields, after.fields)
            assertEquals(before.options, after.options)
        }
    }

    private fun entry(name: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("track-entry-page-loading").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("track-entry-list").performScrollToNode(hasText(name))
        compose.onNodeWithText(name).assertIsDisplayed()
    }
    private fun capture(id: String) { compose.waitForIdle(); captureVisualCatalogSurface(id) }
}
