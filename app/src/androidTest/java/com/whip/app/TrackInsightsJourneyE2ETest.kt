package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackValueDraft
import com.whip.app.domain.UnitDimension
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackInsightsJourneyE2ETest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()

    @After
    fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test
    fun mixedDistanceUnitsUseTheFieldUnitAndPrecisionAcrossInsights() {
        val trackId = runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update {
                AppSettings(setupCompleted = true, dynamicColor = false, themeMode = AppThemeMode.Light)
            }
            val id = app.trackRepository.create(TrackDraft(
                name = "Walking log",
                icon = "🚶",
                fields = listOf(
                    TrackFieldDraft("Walk", TrackFieldType.ShortText, primary = true),
                    TrackFieldDraft("Distance", TrackFieldType.Number, dimension = UnitDimension.Distance,
                        unitId = "mile", precision = 3),
                ),
            ))
            val fields = requireNotNull(app.trackRepository.projection(id)).fields.associateBy { it.name }
            listOf("Morning walk" to "mile", "Evening walk" to "kilometre").forEach { (name, unit) ->
                val preparation = requireNotNull(app.trackRepository.prepareEntryCreate(id))
                app.trackRepository.addEntry(preparation.request, TrackEntryDraft(
                    entryDate = app.clock.today(),
                    values = mapOf(
                        fields.getValue("Walk").uuid to TrackValueDraft(textValue = name),
                        fields.getValue("Distance").uuid to TrackValueDraft(enteredNumber = 1.0, enteredUnitId = unit),
                    ),
                ))
            }
            id
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Tracks tab").performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("track-card-$trackId").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("track-workspace-destination-Insights").performClick()
            compose.onNodeWithTag("track-workspace-insights-list")
                .performScrollToNode(hasText("🚶 Walking log · Distance"))
            capture("tracks.insights.mixed-units")
            assertNumberFullyVisible("1.621 mi")
            assertNumberFullyVisible("0.811 mi")

            compose.onNodeWithTag("track-workspace-destination-Tracks").performClick()
            compose.onNodeWithTag("track-card-$trackId").performClick()
            compose.onNodeWithTag("track-destination-Track Insights").performClick()
            compose.onNodeWithTag("track-insights-list").performScrollToIndex(2)
            compose.onNodeWithTag("track-insights-list").performTouchInput { swipeUp() }
            capture("tracks.detail.insights.mixed-units")
            assertNumberFullyVisible("1.621 mi")
            assertNumberFullyVisible("0.811 mi")

            scenario.recreate()
            compose.onNodeWithTag("track-insights-list").performScrollToIndex(2)
            compose.onNodeWithTag("track-insights-list").performTouchInput { swipeUp() }
            assertNumberFullyVisible("1.621 mi")
            assertNumberFullyVisible("0.811 mi")
        }
    }

    @Test
    fun temperatureAndFineScaleInsightsPreserveReadingsWithoutTotals() {
        val trackId = runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update {
                AppSettings(setupCompleted = true, dynamicColor = false, themeMode = AppThemeMode.Light)
            }
            val id = app.trackRepository.create(TrackDraft(
                name = "Room log", icon = "🌡️",
                fields = listOf(
                    TrackFieldDraft("Reading", TrackFieldType.ShortText, primary = true),
                    TrackFieldDraft("Temperature", TrackFieldType.Number, dimension = UnitDimension.Temperature,
                        unitId = "fahrenheit", precision = 1),
                    TrackFieldDraft("Comfort", TrackFieldType.Scale, scaleMin = 0, scaleMax = 1, scaleStep = 0.125),
                ),
            ))
            val fields = requireNotNull(app.trackRepository.projection(id)).fields.associateBy { it.name }
            listOf(32.0 to "fahrenheit", 20.0 to "celsius").forEachIndexed { index, (value, unit) ->
                val preparation = requireNotNull(app.trackRepository.prepareEntryCreate(id))
                app.trackRepository.addEntry(preparation.request, TrackEntryDraft(
                    entryDate = app.clock.today().minusDays(1L - index),
                    values = mapOf(
                        fields.getValue("Reading").uuid to TrackValueDraft(textValue = "Reading ${index + 1}"),
                        fields.getValue("Temperature").uuid to TrackValueDraft(enteredNumber = value, enteredUnitId = unit),
                        fields.getValue("Comfort").uuid to TrackValueDraft(scaleValue = 0.125 * (index + 1)),
                    ),
                ))
            }
            id
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Tracks tab").performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("track-card-$trackId").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("track-workspace-destination-Insights").performClick()
            compose.onNodeWithTag("track-workspace-insights-list")
                .performScrollToNode(hasText("Room log · Temperature", substring = true))
            compose.onNodeWithText("50.0 °F").assertIsDisplayed()
            compose.onAllNodesWithText("Total").assertCountEquals(0)
            compose.onNodeWithTag("track-workspace-insights-list")
                .performScrollToNode(hasText("Room log · Comfort", substring = true))
            capture("tracks.insights.temperature-and-scale")
            assertNumberFullyVisible("50.0 °F")
            assertNumberFullyVisible("0.188")
            compose.onAllNodesWithText("Total").assertCountEquals(0)

            compose.onNodeWithTag("track-workspace-destination-Tracks").performClick()
            compose.onNodeWithTag("track-card-$trackId").performClick()
            compose.onNodeWithTag("track-destination-Track Insights").performClick()
            // Header and All Entries precede the two numeric cards in this fixture.
            compose.onNodeWithTag("track-insights-list").performScrollToIndex(2)
            compose.onNodeWithText("50.0 °F").assertIsDisplayed()
            compose.onNodeWithText("32.0 °F").assertIsDisplayed()
            compose.onNodeWithText("↑ 36.0 °F").assertIsDisplayed()
            compose.onAllNodesWithText("Sum").assertCountEquals(0)
            compose.onNodeWithTag("track-insights-list").performScrollToIndex(3)
            compose.onNodeWithTag("track-insights-list").performTouchInput { swipeUp() }
            capture("tracks.detail.insights.temperature-and-scale")
            listOf("50.0 °F", "32.0 °F", "↑ 36.0 °F", "0.188", "0.125", "↑ 0.125")
                .forEach(::assertNumberFullyVisible)

            scenario.recreate()
            compose.onNodeWithTag("track-insights-list").performScrollToIndex(2)
            compose.onNodeWithTag("track-insights-list").performTouchInput { swipeUp() }
            assertNumberFullyVisible("50.0 °F")
            compose.onAllNodesWithText("Sum").assertCountEquals(0)
        }
    }

    private fun capture(id: String) {
        // Settle the Compose test clock after a gesture before waiting on Android's queue.
        compose.waitForIdle()
        captureVisualCatalogSurface(id)
    }

    private fun assertNumberFullyVisible(value: String) {
        compose.onNodeWithText(value).assertIsDisplayed()
        val layout = compose.onNodeWithText(value).getUnclippedBoundsInRoot()
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val nativeBounds = device.findObject(By.text(value))?.visibleBounds
        val density = app.resources.displayMetrics.density
        assertTrue("Complete numeric reading $value: $nativeBounds versus $layout",
            nativeBounds != null &&
                nativeBounds.height() >= (layout.bottom - layout.top).value * density - 1f &&
                nativeBounds.width() >= (layout.right - layout.left).value * density - 1f)
    }
}
