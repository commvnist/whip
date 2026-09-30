package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.test.espresso.Espresso.closeSoftKeyboard
import com.whip.app.ui.entryDisplayTitle
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
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
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

    private fun entryNode(matcher: SemanticsMatcher): SemanticsNodeInteraction {
        compose.onNodeWithTag("track-entry-list").performScrollToNode(matcher)
        return compose.onNode(matcher)
    }

    @Test fun emptyArchivedTrackExplainsRestoreThenOffersFirstEntry() {
        val id = runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { AppSettings(setupCompleted = true, dynamicColor = false, themeMode = AppThemeMode.Light) }
            val id = app.trackRepository.create(TrackDraft(name = "Empty archived notes", fields = listOf(
                TrackFieldDraft("Note", TrackFieldType.ShortText, primary = true))))
            app.trackRepository.setArchived(id, true)
            id
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use {
            compose.onNodeWithContentDescription("Tracks tab").performClick()
            compose.openWorkspaceArchive("Tracks")
            compose.onNodeWithTag("track-list").performScrollToNode(hasTestTag("track-card-$id"))
            compose.onNodeWithTag("track-card-$id").performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("track-entry-page-loading").fetchSemanticsNodes().isEmpty() }
            entryNode(hasText("Restore it above", substring = true)).assertIsDisplayed()
            compose.onAllNodesWithContentDescription("Add entry to Empty archived notes").assertCountEquals(0)
            captureVisualCatalogSurface("ux-upgrades.tracks.archived-empty")
            compose.onNodeWithTag("track-destination-Options").performClick()
            compose.onNodeWithTag("track-options-list").performScrollToNode(hasText("Restore this Track to add Entries", substring = true))
            compose.onNodeWithText("Restore this Track to add Entries", substring = true).assertIsDisplayed()
            compose.onAllNodesWithText("Pin to Whip Home").assertCountEquals(0)
            captureVisualCatalogSurface("ux-audit-2.tracks.archived-options")
            compose.onNodeWithTag("track-destination-Entries").performClick()
            compose.onNodeWithText("Restore Track").performClick()
            compose.waitUntil(15_000) { runBlocking { app.trackRepository.projection(id)?.track?.archived == false } }
            entryNode(hasText("Add Entry")).performClick()
            compose.onNodeWithTag("track-entry-editor-surface").assertIsDisplayed()
        }
    }

    @Test fun archivedHistoryCanBeSearchedRecreatedAndRestored() = verifyHistory(false)

    @Test @AndroidFontScale
    fun archivedHistoryCanBeSearchedRecreatedAndRestoredAtLargeText() = verifyHistory(true)


    @Test fun entryCorrectionRetainsEntriesAndActivityInspectorsAndDuplicateDraft() {
        val before = runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { AppSettings(setupCompleted = true, themeMode = AppThemeMode.Light, dynamicColor = false) }
            val id = app.trackRepository.create(TrackDraft(trackName, fields = listOf(
                TrackFieldDraft("Walk", TrackFieldType.ShortText, primary = true),
                TrackFieldDraft("Notes", TrackFieldType.LongText),
            )))
            val form = requireNotNull(app.trackRepository.projection(id))
            val note = form.fields.single { it.name == "Notes" }
            for (title in listOf("River trail after the rain", "Existing trail")) {
                app.trackRepository.addEntry(id, TrackEntryDraft(app.clock.today(), values = mapOf(
                    form.primaryField.uuid to TrackValueDraft(textValue = title),
                    note.uuid to TrackValueDraft(textValue = "Saved $title"),
                )))
            }
            requireNotNull(app.trackRepository.projection(id))
        }
        val title = "River trail after the rain"
        val nameTag = "track-entry-short-text-${before.primaryField.uuid}"
        val noteTag = "track-entry-long-text-${before.fields.single { it.name == "Notes" }.uuid}"
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Tracks tab").performClick()
            compose.onNodeWithTag("track-list").performScrollToNode(hasTestTag("track-card-${before.track.id}"))
            compose.onNodeWithTag("track-card-${before.track.id}").performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("track-entry-page-loading").fetchSemanticsNodes().isEmpty() }
            entryNode(hasContentDescription("Search Entries in $trackName")).performClick()
            entryNode(hasTestTag("track-entry-search")).performTextReplacement("River trail")
            closeSoftKeyboard()
            awaitEntryText(title)
            entryNode(hasText(title)).performClick()
            compose.onNodeWithTag("entity-inspector-title").assertTextEquals(title)
            compose.onNodeWithTag("entity-inspector-edit").performClick()
            compose.onAllNodesWithTag("entity-inspector-title").assertCountEquals(0)
            compose.onNodeWithTag("track-entry-editor-list").performScrollToNode(hasTestTag(nameTag))
            compose.onNodeWithTag(nameTag).performTextReplacement("Existing trail")
            compose.onNodeWithTag("track-entry-editor-list").performScrollToNode(hasTestTag(noteTag))
            compose.onNodeWithTag(noteTag).performTextReplacement("Unsaved correction stays here")
            closeSoftKeyboard()
            compose.onNodeWithTag("track-entry-editor-list").performScrollToNode(hasText("Review Existing trail ·", substring = true))
            compose.onNodeWithText("Review Existing trail ·", substring = true).performClick()
            compose.onNodeWithText("Switching to the other Entry will discard unsaved changes to this Entry.").assertIsDisplayed()
            compose.onNodeWithText("Keep Editing This Entry").performClick()
            compose.onAllNodesWithTag("entity-inspector-title").assertCountEquals(0)
            compose.onNodeWithTag("track-entry-editor-list").performScrollToNode(hasTestTag(noteTag))
            compose.onNodeWithTag(noteTag).assertIsDisplayed().assertTextContains("Unsaved correction stays here")
            scenario.recreate()
            compose.onAllNodesWithTag("entity-inspector-title").assertCountEquals(0)
            compose.onNodeWithTag("track-entry-editor-list").performScrollToNode(hasTestTag(nameTag))
            compose.onNodeWithTag(nameTag).assertIsDisplayed().assertTextContains("Existing trail")
            compose.onNodeWithTag("track-entry-editor-list").performScrollToNode(hasTestTag(noteTag))
            compose.onNodeWithTag(noteTag).assertIsDisplayed().assertTextContains("Unsaved correction stays here")
            captureVisualCatalogSurface("overhaul.tracks.editing-duplicate-retained-draft")
            compose.onNodeWithContentDescription("Close Entry Editor").performClick()
            compose.onNodeWithText("Discard", substring = false).performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("track-entry-editor-surface").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithTag("entity-inspector-title").assertTextEquals(title)
            scenario.recreate()
            compose.onNodeWithTag("entity-inspector-title").assertTextEquals(title)
            captureVisualCatalogSurface("overhaul.tracks.entries-inspector-after-cancel")
            compose.onNodeWithContentDescription("Close Track Entry details").performClick()
            entryNode(hasTestTag("track-entry-search")).assertTextContains("River trail")
            compose.onNodeWithTag("track-destination-Entries").assertIsSelected()
            assertEquals(before.entries, runBlocking { requireNotNull(app.trackRepository.projection(before.track.id)).entries })

            compose.onNodeWithContentDescription("Back to Tracks").performClick()
            compose.onNodeWithTag("track-workspace-destination-Activity").performClick()
            compose.onNodeWithContentDescription("Filter Track Activity").performClick()
            compose.onNodeWithTag("track-activity-search").performTextReplacement("River trail")
            closeSoftKeyboard()
            compose.onNodeWithText("Apply", substring = false).performClick()
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(title))
            compose.onNodeWithText(title).performClick()
            compose.onNodeWithTag("entity-inspector-edit").performClick()
            compose.onAllNodesWithTag("entity-inspector-title").assertCountEquals(0)
            compose.onNodeWithTag("track-entry-editor-list").performScrollToNode(hasTestTag(noteTag))
            compose.onNodeWithTag(noteTag).performTextReplacement("Saved correction from Activity")
            closeSoftKeyboard()
            compose.onAllNodesWithTag("entity-inspector-title").assertCountEquals(0)
            compose.onNodeWithTag(noteTag).assertIsDisplayed().assertTextContains("Saved correction from Activity")
            scenario.recreate()
            compose.onAllNodesWithTag("entity-inspector-title").assertCountEquals(0)
            compose.onNodeWithTag("track-entry-editor-list").performScrollToNode(hasTestTag(noteTag))
            compose.onNodeWithTag(noteTag).assertIsDisplayed().assertTextContains("Saved correction from Activity")
            captureVisualCatalogSurface("overhaul.tracks.activity-editing-restored-draft")
            compose.onNodeWithText("Save", substring = false).performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("track-entry-editor-surface").fetchSemanticsNodes().isEmpty() }
            try {
                compose.waitUntil(10_000) {
                    compose.onAllNodesWithTag("entity-inspector-title").fetchSemanticsNodes().isNotEmpty()
                }
            } finally {
                captureVisualCatalogSurface("overhaul.tracks.activity-returned-after-save")
            }
            compose.onNodeWithTag("entity-inspector-title").assertTextEquals(title)
            scenario.recreate()
            try {
                compose.waitUntil(10_000) {
                    compose.onAllNodesWithTag("entity-inspector-title").fetchSemanticsNodes().isNotEmpty()
                }
            } finally {
                captureVisualCatalogSurface("overhaul.tracks.activity-restored-after-save")
            }
            compose.onNodeWithTag("entity-inspector-title").assertTextEquals(title)
            compose.onNodeWithText("Saved correction from Activity").assertIsDisplayed()
            captureVisualCatalogSurface("overhaul.tracks.activity-inspector-after-save")
            compose.onNodeWithContentDescription("Close Track Entry details").performClick()
            compose.onNodeWithTag("track-workspace-destination-Activity").assertIsSelected()
            compose.onNodeWithText("Text: River trail").assertIsDisplayed()
            val after = runBlocking { requireNotNull(app.trackRepository.projection(before.track.id)) }
            assertEquals(2, after.entries.size)
            assertEquals("Saved correction from Activity", after.entries.single { after.entryDisplayTitle(it) == title }
                .value(before.fields.single { it.name == "Notes" }.id)?.textValue)
            assertEquals(before.entries.single { before.entryDisplayTitle(it) == "Existing trail" },
                after.entries.single { after.entryDisplayTitle(it) == "Existing trail" })
        }
    }

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
            compose.openWorkspaceArchive("Tracks")
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("track-list").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("track-list").performScrollToNode(hasTestTag("track-card-$trackId"))
            compose.onNodeWithTag("track-card-$trackId").performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("track-entry-list").fetchSemanticsNodes().isNotEmpty() }
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("track-entry-page-loading").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithTag("track-entry-list").performScrollToNode(hasText("Neighbourhood walk 1"))
            capture("tracks.history.archived.$suffix")
            assertNativeTextFullyVisible("Neighbourhood walk 1")
            assertNativeTextFullyVisible(app.clock.today().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
            compose.onNodeWithTag("track-entry-list").performScrollToNode(hasText("Neighbourhood walk 12"))
            compose.onNodeWithText("Neighbourhood walk 12").assertIsDisplayed()
            capture("tracks.history.records.$suffix")
            val olderDate = app.clock.today().minusDays(11).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
            assertNativeTextFullyVisible(olderDate)

            compose.onNodeWithTag("track-entry-list").performScrollToNode(
                hasContentDescription("Search Entries in $trackName"))
            compose.onNodeWithContentDescription("Search Entries in $trackName").performClick()
            compose.onNodeWithTag("track-entry-list").performScrollToNode(hasTestTag("track-entry-search"))
            compose.waitForIdle()
            refreshAccessibilityHierarchy("Track search input")
            checkNotNull(device.findObject(By.res("track-entry-search"))).click()
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
            capture("tracks.history.search-result.$suffix")
            compose.onNodeWithTag("track-entry-search").performTextReplacement("No saved trail matches this")
            awaitEntryText("No Matching Entries")
            entryNode(hasText("No Matching Entries")).assertIsDisplayed()
            captureVisualCatalogSurface("ux-upgrades.tracks.entry-recovery.$suffix")
            entryNode(hasText("Clear Search & Filters")).performClick()
            entryNode(hasText("Neighbourhood walk 1")).assertIsDisplayed()
            entryNode(hasTestTag("track-entry-search")).performTextReplacement("River trail")
            compose.onNodeWithTag("track-entry-search").assertTextContains("River trail")
            compose.waitForIdle()
            scenario.recreate()
            compose.onNodeWithTag("track-entry-list").performScrollToNode(hasTestTag("track-entry-search"))
            compose.onNodeWithTag("track-entry-search").assertTextContains("River trail")
            awaitEntryText("River trail after the rain")
            compose.onNodeWithTag("track-entry-list").performScrollToNode(hasText("River trail after the rain"))
            compose.onNodeWithText("River trail after the rain").performClick()
            if (large) compose.assertDialogFontScale()
            compose.onAllNodesWithTag("entity-inspector-edit").assertCountEquals(0)
            capture("tracks.history.inspector.$suffix")
            compose.onNodeWithContentDescription("Close Track Entry details").performClick()
            compose.onNodeWithContentDescription("Back to Tracks").performClick()
            compose.onNodeWithTag("track-workspace-destination-Tracks").assertIsSelected()
            compose.waitUntil(10_000) {
                InstrumentationRegistry.getInstrumentation().uiAutomation.windows.none {
                    it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
                }
            }
            compose.onNodeWithTag("workspace-top-app-bar").assertIsDisplayed()
            listOf("Home", "Tasks", "Habits", "Goals", "Tracks", "Gym").forEach { destination ->
                val description = if (destination == "Home") "Go to Home" else "$destination tab"
                compose.onNodeWithContentDescription(description).assertIsDisplayed()
            }
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
            compose.onNodeWithContentDescription("Add entry to $trackName").assertIsDisplayed()
            capture("tracks.history.restored.$suffix")
            compose.onNodeWithContentDescription("Add entry to $trackName").performClick()
            compose.onNodeWithTag("track-entry-editor-surface").assertIsDisplayed()
            compose.onNodeWithContentDescription("Close Entry Editor").performClick()
            compose.onNodeWithContentDescription("Back to Tracks").assertIsDisplayed()
            clickDetailDestination("Options")
            compose.onNodeWithText("Edit Track").performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription("Back to Tracks").assertIsDisplayed()
            clickDetailDestination("Track Insights")
            compose.onNodeWithTag("track-insights-list").performScrollToNode(hasText("All Entries"))
            compose.onNodeWithText("All Entries").assertIsDisplayed()
            compose.onNodeWithContentDescription("Back to Tracks").assertIsDisplayed()
            clickDetailDestination("Entries")
            compose.onNodeWithTag("track-entry-list").assertIsDisplayed()
            val after = runBlocking { requireNotNull(app.trackRepository.projection(trackId)) }
            assertFalse(after.track.archived)
            assertEquals(before.fields, after.fields)
            assertEquals(before.options, after.options)
            assertEquals(before.entries, after.entries)
        }
    }

    private fun awaitEntryText(text: String) {
        compose.waitUntil(10_000) {
            runCatching {
                compose.onNodeWithTag("track-entry-list").performScrollToNode(hasText(text))
            }.isSuccess
        }
    }

    private fun clickDetailDestination(destination: String) {
        val node = compose.onNodeWithTag("track-destination-$destination")
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
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
