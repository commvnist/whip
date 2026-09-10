package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Inclusive recent windows must agree across the workspace and a persisted Track. */
@RunWith(AndroidJUnit4::class)
class TrackInsightWindowJourneyE2ETest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()

    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun recentWindowsExcludeFutureEntriesWithoutChangingHistory() {
        val before = runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update {
                AppSettings(setupCompleted = true, dynamicColor = false, themeMode = AppThemeMode.Light)
            }
            val id = app.trackRepository.create(TrackDraft(
                name = "Walking and recovery observations", icon = "🚶",
                fields = listOf(TrackFieldDraft("Observation", TrackFieldType.ShortText, primary = true)),
            ))
            val field = requireNotNull(app.trackRepository.projection(id)).fields.single()
            listOf(0L, 6L, 7L, 29L, 30L, 89L, 90L, -1L).forEach { daysAgo ->
                val preparation = requireNotNull(app.trackRepository.prepareEntryCreate(id))
                app.trackRepository.addEntry(preparation.request, TrackEntryDraft(
                    entryDate = app.clock.today().minusDays(daysAgo),
                    values = mapOf(field.uuid to TrackValueDraft(textValue = "Observation $daysAgo")),
                ))
            }
            requireNotNull(app.trackRepository.projection(id))
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Tracks tab").performClick()
            compose.waitUntil(15_000) {
                compose.onAllNodesWithTag("track-card-${before.track.id}").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("track-workspace-destination-Insights").performClick()
            compose.onNodeWithTag("track-workspace-insights-list").assertIsDisplayed()
            compose.waitForIdle()
            captureVisualCatalogSurface("tracks.insights.recent-windows")
            assertReading("track-workspace-insights-list", "Total Entries", "8")
            assertReading("track-workspace-insights-list", "Entries in 7 Days", "2")
            assertReading("track-workspace-insights-list", "Entries in 30 Days", "4")
            compose.onNodeWithTag("track-workspace-insights-list")
                .performScrollToNode(hasText(before.track.name))
            compose.onNodeWithText(before.track.name).performClick()
            compose.onNodeWithTag("track-insights-list").assertIsDisplayed()
            assertReading("track-insights-list", "Last 7 Days", "2")
            assertReading("track-insights-list", "Last 30 Days", "4")
            assertReading("track-insights-list", "Last 90 Days", "6")
            assertReading("track-insights-list", "Recent Weekly Rate", "0.93 Entries")
            compose.waitForIdle()
            captureVisualCatalogSurface("tracks.detail.insights.recent-windows")
            scenario.recreate()
            assertReading("track-insights-list", "Last 30 Days", "4")
            assertReading("track-insights-list", "Total", "8")
            assertEquals(before, runBlocking { app.trackRepository.projection(before.track.id) })
        }
    }

    private fun assertReading(list: String, label: String, value: String) {
        compose.onNodeWithTag(list).performScrollToNode(hasText(label))
        compose.onNodeWithText(label, useUnmergedTree = true).onParent().onChildren()
            .filter(hasText(value)).assertCountEquals(1)
    }
}
