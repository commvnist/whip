package com.whip.app

import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityWindowInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.By
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackValueDraft
import java.io.File
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackHistoryJourneyE2ETest {
    private val compose = createEmptyComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val trackName = "Weekend walks and trail notes"

    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun archivedHistoryCanBeSearchedRecreatedAndRestored() = verifyHistory(false)

    @Test @AndroidFontScale
    fun archivedHistoryCanBeSearchedRecreatedAndRestoredAtLargeText() = verifyHistory(true)

    private fun verifyHistory(large: Boolean) {
        val trackId = runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update {
                AppSettings(setupCompleted = true, dynamicColor = false, themeMode = AppThemeMode.Light)
            }
            val id = app.trackRepository.create(TrackDraft(
                name = trackName, icon = "🥾",
                fields = listOf(TrackFieldDraft("Walk", TrackFieldType.ShortText, primary = true)),
            ))
            val field = requireNotNull(app.trackRepository.projection(id)).primaryField
            repeat(30) { index ->
                val preparation = requireNotNull(app.trackRepository.prepareEntryCreate(id))
                app.trackRepository.addEntry(preparation.request, TrackEntryDraft(
                    entryDate = app.clock.today().minusDays(index.toLong()),
                    values = mapOf(field.uuid to TrackValueDraft(textValue =
                        if (index == 17) "River trail after the rain" else "Neighbourhood walk ${index + 1}")),
                ))
            }
            app.trackRepository.setArchived(id, true)
            id
        }
        val before = runBlocking { requireNotNull(app.trackRepository.projection(trackId)) }
        val suffix = if (large) "large" else "ordinary"
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Tracks tab").performClick()
            compose.onNodeWithTag("track-workspace-destination-Archived").performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("track-list").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("track-list").performScrollToNode(hasTestTag("track-card-$trackId"))
            compose.onNodeWithTag("track-card-$trackId").performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("track-entry-list").fetchSemanticsNodes().isNotEmpty() }
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("track-entry-page-loading").fetchSemanticsNodes().isEmpty() }
            capture("tracks.history.archived.$suffix")
            compose.onNodeWithTag("track-entry-list").performScrollToNode(hasText("Neighbourhood walk 12"))
            compose.onNodeWithText("Neighbourhood walk 12").assertIsDisplayed()
            capture("tracks.history.records.$suffix")
            val olderDate = app.clock.today().minusDays(11).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
            assertNativeTextFullyVisible(olderDate)

            compose.onNodeWithTag("track-entry-list").performScrollToNode(
                hasContentDescription("Search Entries in $trackName"))
            compose.onNodeWithContentDescription("Search Entries in $trackName").performClick()
            compose.onNodeWithTag("track-entry-search").performScrollTo().performClick()
            compose.waitUntil(10_000) {
                InstrumentationRegistry.getInstrumentation().uiAutomation.windows.any {
                    it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
                }
            }
            InstrumentationRegistry.getInstrumentation().uiAutomation.waitForIdle(750L, 5_000L)
            compose.waitForIdle()
            compose.onNodeWithTag("track-entry-search").performTextReplacement("River trail")
            capture("tracks.history.search.$suffix")
            assertNativeQueryAboveKeyboard()
            compose.onNodeWithTag("track-entry-list").performScrollToNode(hasText("River trail after the rain"))
            compose.onNodeWithText("River trail after the rain").assertIsDisplayed()
            assertNativeTextFullyVisible("River trail after the rain")
            compose.onAllNodesWithContentDescription("Edit Entry River trail after the rain").assertCountEquals(0)

            device.pressBack()
            compose.waitUntil(10_000) {
                InstrumentationRegistry.getInstrumentation().uiAutomation.windows.none {
                    it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
                }
            }
            compose.onNodeWithTag("track-entry-list").performScrollToNode(hasTestTag("track-entry-search"))
            compose.onNodeWithTag("track-entry-search").assertTextContains("River trail")
            compose.onNodeWithTag("workspace-top-app-bar").assertIsDisplayed()
            capture("tracks.history.search-result.$suffix")
            scenario.recreate()
            compose.onNodeWithTag("track-entry-list").performScrollToNode(hasTestTag("track-entry-search"))
            compose.onNodeWithTag("track-entry-search").assertTextContains("River trail")
            compose.onNodeWithTag("track-entry-list").performScrollToNode(hasText("River trail after the rain"))
            compose.onNodeWithText("River trail after the rain").performClick()
            if (large) compose.assertDialogFontScale()
            compose.onAllNodesWithTag("entity-inspector-edit").assertCountEquals(0)
            capture("tracks.history.inspector.$suffix")
            compose.onNodeWithContentDescription("Close Track Entry details").performClick()
            compose.onNodeWithContentDescription("Back to Tracks").performClick()
            compose.onNodeWithTag("track-workspace-destination-Archived").assertIsSelected()
            compose.onNodeWithTag("track-list").performScrollToNode(hasTestTag("track-card-$trackId"))
            compose.onNodeWithTag("track-card-$trackId").performClick()

            compose.onNodeWithTag("track-entry-list").performScrollToNode(hasText("Restore Track"))
            compose.onNodeWithText("Restore Track").performScrollTo().performClick()
            compose.waitUntil(15_000) {
                runBlocking { app.trackRepository.projection(trackId)?.track?.archived == false }
            }
            scenario.recreate()
            compose.onNodeWithTag("track-entry-list").performScrollToNode(hasText("Neighbourhood walk 1"))
            compose.onNodeWithContentDescription("Edit Entry Neighbourhood walk 1").assertIsDisplayed()
            val after = runBlocking { requireNotNull(app.trackRepository.projection(trackId)) }
            assertFalse(after.track.archived)
            assertEquals(before.fields, after.fields)
            assertEquals(before.options, after.options)
            assertEquals(before.entries, after.entries)
        }
    }

    private fun capture(id: String) {
        compose.waitForIdle()
        // Native dialog transitions continue after the Compose test clock settles.
        // The legacy private-file capture needs the same idle boundary as Task journeys.
        InstrumentationRegistry.getInstrumentation().uiAutomation.waitForIdle(750L, 5_000L)
        device.waitForIdle()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            captureVisualCatalogSurface(id)
        } else {
            val directory = checkNotNull(app.getExternalFilesDir("track-history-journey"))
            check(directory.isDirectory || directory.mkdirs())
            check(device.takeScreenshot(File(directory, "$id.png")))
            device.dumpWindowHierarchy(File(directory, "$id.xml"))
        }
    }

    private fun assertNativeTextFullyVisible(text: String) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().uiAutomation.waitForIdle(750L, 5_000L)
        device.waitForIdle()
        refreshAccessibilityHierarchy("Track history text")
        val layout = compose.onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val native = device.findObjects(By.text(text)).singleOrNull()?.visibleBounds
        val density = app.resources.displayMetrics.density
        assertTrue("Complete history text $text: $native versus $layout",
            native != null && native.height() >= (layout.bottom - layout.top).value * density - 1f)
    }

    private fun assertNativeQueryAboveKeyboard() {
        val ime = Rect()
        checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.windows.firstOrNull {
            it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
        }).getBoundsInScreen(ime)
        val native = device.findObject(By.res("track-entry-search"))?.visibleBounds
        val layout = compose.onNodeWithTag("track-entry-search").getUnclippedBoundsInRoot()
        val density = app.resources.displayMetrics.density
        assertTrue("Complete native Track query above keyboard: $native versus $ime; layout $layout",
            native != null && native.bottom <= ime.top && native.height() >= (layout.bottom - layout.top).value * density - 1f)
        assertNativeTextFullyVisible("Search Entries")
        val title = device.findObject(By.res("track-detail-title"))?.visibleBounds
        val titleLayout = compose.onNodeWithTag("track-detail-title").getUnclippedBoundsInRoot()
        val status = checkNotNull(device.findObject(By.res("com.android.systemui:id/status_bar"))).visibleBounds
        assertTrue("Track identity must remain below status icons: $title versus $status and $titleLayout",
            title != null && title.top >= status.bottom &&
                title.height() >= (titleLayout.bottom - titleLayout.top).value * density - 1f)
        val back = device.findObject(By.desc("Back to Tracks"))?.visibleBounds
        assertTrue("Back must remain reachable above the keyboard: $back versus $ime",
            back != null && back.top >= status.bottom && back.bottom <= ime.top && back.height() >= 48 * density - 1)
    }
}
