package com.whip.app

import android.content.Intent
import android.os.Build
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.TrackCsvMapping
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.previewTrackCsvImport
import com.whip.app.domain.trackCsvPayloadFingerprint
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackImportedTextJourneyE2ETest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()

    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun importedShortTextSurvivesEditingRecreationAndSave() = verifyImportedText(
        TrackFieldType.ShortText, "A" + "0123456789".repeat(60) + "-KEEP-SOURCE-TAIL",
    )

    @Test fun importedLongTextSurvivesEditingRecreationAndSave() = verifyImportedText(
        TrackFieldType.LongText, "A" + "Long walking note. ".repeat(340) + "-KEEP-SOURCE-TAIL",
    )

    private fun verifyImportedText(type: TrackFieldType, original: String) {
        val trackName = "Imported walking notes"
        val entryName = "River trail"
        val before = runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update {
                AppSettings(setupCompleted = true, dynamicColor = false, themeMode = AppThemeMode.Light)
            }
            val id = app.trackRepository.create(TrackDraft(
                name = trackName,
                fields = listOf(
                    TrackFieldDraft("Name", TrackFieldType.ShortText, primary = true),
                    TrackFieldDraft("Source note", type),
                ),
            ))
            val form = requireNotNull(app.trackRepository.csvImportForm(id))
            val csv = "Name,Source note\n$entryName,\"${original.replace("\"", "\"\"")}\"\n"
            val mapping = TrackCsvMapping(fieldColumns = form.fields.associate { it.uuid to it.name })
            val preview = previewTrackCsvImport(form, csv, mapping, app.clock.today())
            assertTrue(preview.issues.isEmpty())
            assertEquals(1, preview.validRows)
            val preparation = requireNotNull(app.trackRepository.prepareCsvImport(
                openingForm = form,
                batchUuid = UUID.randomUUID().toString(),
                payloadFingerprint = trackCsvPayloadFingerprint(csv),
                mapping = mapping,
                defaultEntryDate = app.clock.today(),
                drafts = preview.validDrafts,
            ))
            val receipt = app.trackRepository.importEntries(preparation.request, preview.validDrafts)
            assertTrue(receipt.changed)
            requireNotNull(app.trackRepository.projection(id))
        }
        val field = before.fields.single { it.name == "Source note" }
        val imported = before.entries.single()
        assertEquals(original, imported.value(field.id)?.textValue)
        val edited = "B" + original.drop(1)
        val tag = "track-entry-${if (type == TrackFieldType.ShortText) "short" else "long"}-text-${field.uuid}"

        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Tracks tab").performClick()
            compose.onNodeWithTag("track-list").performScrollToNode(hasTestTag("track-card-${before.track.id}"))
            compose.onNodeWithTag("track-card-${before.track.id}").performClick()
            fun openEntry() {
                compose.onNodeWithTag("track-entry-list")
                    .performScrollToNode(hasContentDescription("Edit Entry $entryName"))
                compose.onNodeWithContentDescription("Edit Entry $entryName").performClick()
                compose.onNodeWithTag("track-entry-editor-list").performScrollToNode(hasTestTag(tag))
            }
            openEntry()
            compose.onNodeWithTag(tag).assertTextContains(original)
            compose.onNodeWithTag(tag).performClick().performTextReplacement(edited)
            closeSoftKeyboard()
            scenario.recreate()
            compose.onNodeWithText("Save", substring = false).performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("track-entry-editor-surface").fetchSemanticsNodes().isEmpty()
            }
            val saved = runBlocking { requireNotNull(app.trackRepository.projection(before.track.id)) }.entries.single()
            assertEquals(imported.entry.id, saved.entry.id)
            assertEquals(imported.entry.uuid, saved.entry.uuid)
            assertEquals(imported.entry.entryDate, saved.entry.entryDate)
            assertEquals(entryName, saved.value(before.primaryField.id)?.textValue)
            openEntry()
            val actual = saved.value(field.id)?.textValue.orEmpty()
            assertEquals("A one-character edit must retain the imported suffix", edited.length, actual.length)
            assertEquals(edited, actual)
            compose.onNodeWithTag(tag).assertTextContains(edited)
            val extended = "$edited 🧭"
            compose.onNodeWithTag(tag).performClick().performTextReplacement(extended)
            closeSoftKeyboard()
            scenario.recreate()
            compose.onNodeWithTag("track-entry-editor-list").performScrollToNode(hasTestTag(tag))
            compose.onNodeWithTag(tag).assertTextContains(extended)
            compose.onNodeWithText("Save", substring = false).performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("track-entry-editor-surface").fetchSemanticsNodes().isEmpty()
            }
            val extendedEntry = runBlocking {
                requireNotNull(app.trackRepository.projection(before.track.id))
            }.entries.single()
            assertEquals(imported.entry.uuid, extendedEntry.entry.uuid)
            assertEquals(extended, extendedEntry.value(field.id)?.textValue)
            openEntry()
            compose.onNodeWithTag(tag).assertTextContains(extended)
            capture("tracks.imported-text.${if (type == TrackFieldType.ShortText) "short" else "long"}.reopened")
            compose.onNodeWithContentDescription("Close Entry Editor").performClick()
        }
    }

    private fun capture(id: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.waitForIdle(750L, 5_000L)
        val device = UiDevice.getInstance(instrumentation)
        device.waitForIdle()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) captureVisualCatalogSurface(id)
        else {
            val directory = checkNotNull(app.getExternalFilesDir("track-imported-text-journey"))
            check(directory.isDirectory || directory.mkdirs())
            check(device.takeScreenshot(File(directory, "$id.png")))
            device.dumpWindowHierarchy(File(directory, "$id.xml"))
        }
    }
}
