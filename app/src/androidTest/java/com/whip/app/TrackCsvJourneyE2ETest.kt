package com.whip.app

import android.content.Intent
import android.util.Base64
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.*
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** Native document selection, interpreted values, mapping changes, recovery and persisted import. */
@RunWith(AndroidJUnit4::class)
class TrackCsvJourneyE2ETest {
    private val compose = createEmptyComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val device get() = UiDevice.getInstance(instrumentation)
    private val trackName = "Walking CSV archive"
    private val fileName = "WhipCsvJourney.csv"
    private val exportFileName = "WhipCsvExport-${java.util.UUID.randomUUID().toString().take(8)}.csv"

    @After fun clean() {
        runBlocking { app.backupRepository.deleteAllData() }
        device.executeShellCommand("rm -f /sdcard/Download/$fileName")
        device.executeShellCommand("rm -f /sdcard/Download/$exportFileName")
    }

    @Test fun csvValuesCanBeReviewedRemappedRecoveredAndImported() = verifyJourney(false)

    @Test @AndroidFontScale
    fun csvValuesCanBeReviewedRemappedRecoveredAndImportedAtLargeText() = verifyJourney(true)

    private fun verifyJourney(large: Boolean) {
        val trackId = runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { AppSettings(setupCompleted = true, dynamicColor = false, themeMode = AppThemeMode.Light) }
            app.trackRepository.create(TrackDraft(name = trackName, fields = listOf(
                TrackFieldDraft("Name", TrackFieldType.ShortText, required = true, primary = true),
                TrackFieldDraft("Distance", TrackFieldType.Number, dimension = UnitDimension.Distance, unitId = "mile", precision = 2),
                TrackFieldDraft("Terrain", TrackFieldType.SingleChoice, options = listOf(TrackChoiceOptionDraft("Trail"), TrackChoiceOptionDraft("Street"))),
                TrackFieldDraft("Effort", TrackFieldType.Scale, scaleMin = 0, scaleMax = 5, scaleStep = 0.5),
                TrackFieldDraft("Notes", TrackFieldType.LongText),
                TrackFieldDraft("Visit date", TrackFieldType.Date),
                TrackFieldDraft("Rained", TrackFieldType.YesNo),
            )))
        }
        val csv = "Entry Date,Name,Distance,Distance Unit,Terrain,Effort,Notes,Visit date,Rained\n" +
            "2026-09-01,River loop,1.25,mile,Trail,2.5,\"Wind, then rain\",2026-08-31,No\n" +
            "2026-09-02,Hill circuit,2,kilometre,Street,4,\"Clear skies\nLookout\",2026-09-01,Yes\n"
        val encoded = Base64.encodeToString(csv.toByteArray(), Base64.NO_WRAP)
        // Shell writes only this named synthetic fixture; the app reads it through the real picker grant.
        device.executeShellCommand("mkdir -p /sdcard/Download")
        // UiAutomation tokenizes arguments without shell quote parsing. Keep the script one token.
        device.executeShellCommand("sh -c echo\${IFS}$encoded|base64\${IFS}-d>/sdcard/Download/$fileName")
        assertEquals(csv, device.executeShellCommand("cat /sdcard/Download/$fileName"))
        val suffix = if (large) "large" else "ordinary"
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Tracks tab").performClick()
            compose.onNodeWithTag("track-list").performScrollToNode(hasTestTag("track-card-$trackId"))
            compose.onNodeWithTag("track-card-$trackId").performClick()
            compose.onNodeWithContentDescription("Options").performClick()
            compose.onNodeWithTag("track-options-list")
                .performScrollToNode(hasText("Import Entries from CSV"))
            compose.onNodeWithText("Import Entries from CSV").performClick()
            selectDownload()
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("track-csv-import-confirm").fetchSemanticsNodes().isNotEmpty() }
            compose.waitUntil(15_000) { compose.onAllNodesWithText("Import 2 Entries").fetchSemanticsNodes().isNotEmpty() }
            capture("tracks.csv-review.ready.$suffix")
            if (large) compose.assertDialogFontScale()
            csvScroll(hasText("River loop")).assertIsDisplayed()
            csvScroll(hasText("1.25 mi")).assertIsDisplayed()
            csvScroll(hasText("Trail")).assertIsDisplayed()
            csvScroll(hasText("2.5")).assertIsDisplayed()
            csvScroll(hasText("Wind, then rain")).assertIsDisplayed()
            csvScroll(hasText("No")).assertIsDisplayed()
            assertEquals(0, runBlocking { app.trackRepository.projection(trackId)!!.entries.size })

            csvScroll(hasText("Review Field Mapping (7 mapped)")).performClick()
            csvScroll(hasContentDescription("Name · Primary: Name")).performClick()
            compose.onNodeWithContentDescription("Name · Primary option: Notes").performScrollTo().performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithText("Import 2 Entries").fetchSemanticsNodes().isNotEmpty() }
            csvScroll(hasTestTag("track-csv-preview-value-${fieldUuid(trackId, "Name")}"))
                .assertTextContains("Wind, then rain")
            capture("tracks.csv-review.remapped.$suffix")
            csvScroll(hasContentDescription("Name · Primary: Notes")).performClick()
            compose.onNodeWithContentDescription("Name · Primary option: Name").performScrollTo().performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithText("Import 2 Entries").fetchSemanticsNodes().isNotEmpty() }
            csvScroll(hasText("Next Entry")).performClick()
            csvScroll(hasText("Hill circuit")).assertIsDisplayed()
            // A nonstandard unit-column header needs an explicit mapping; the preview reveals the default.
            csvScroll(hasText("2.00 mi")).assertIsDisplayed()
            csvScroll(hasContentDescription("Distance Unit: Use miles (mi)")).performClick()
            compose.onNodeWithContentDescription("Distance Unit option: Distance Unit").performScrollTo().performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithText("Import 2 Entries").fetchSemanticsNodes().isNotEmpty() }
            csvScroll(hasText("Next Entry")).performClick()
            csvScroll(hasText("Hill circuit")).assertIsDisplayed()
            csvScroll(hasText("2.00 km")).assertIsDisplayed()
            csvScroll(hasText("Yes")).assertIsDisplayed()
            capture("tracks.csv-review.second.$suffix")
            scenario.recreate()
            compose.waitUntil(15_000) { compose.onAllNodesWithText("Import 2 Entries").fetchSemanticsNodes().isNotEmpty() }
            csvScroll(hasText("Hill circuit")).assertIsDisplayed()
            compose.onNodeWithTag("track-csv-import-confirm").performClick()
            try {
                compose.waitUntil(15_000) { compose.onAllNodesWithTag("track-csv-import-complete").fetchSemanticsNodes().isNotEmpty() }
            } catch (failure: Throwable) {
                capture("tracks.csv-review.completion-missing.$suffix")
                throw failure
            }
            compose.onNodeWithTag("track-csv-import-complete").assertIsDisplayed()
            compose.waitUntil(15_000) {
                compose.onAllNodesWithTag("persistence-saving-overlay").fetchSemanticsNodes().isEmpty()
            }
            compose.onNodeWithText("Done").assertIsEnabled()
            capture("tracks.csv-review.complete.$suffix",
                visuallyDistinctFrom = "tracks.csv-review.second.$suffix")
            scenario.recreate()
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("track-csv-import-complete").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Done").performClick()
            val projection = runBlocking { requireNotNull(app.trackRepository.projection(trackId)) }
            assertEquals(2, projection.entries.size)
            val river = projection.entries.single { projection.primaryText(it) == "River loop" }
            val hill = projection.entries.single { projection.primaryText(it) == "Hill circuit" }
            assertEquals(LocalDate.of(2026, 9, 1), river.entry.entryDate)
            assertEquals(1.25, river.value(projection.fields.single { it.name == "Distance" }.id)!!.enteredNumber!!, 0.0)
            assertEquals("kilometre", hill.value(projection.fields.single { it.name == "Distance" }.id)!!.enteredUnitId)
            assertEquals(false, river.value(projection.fields.single { it.name == "Rained" }.id)!!.booleanValue)
            assertEquals("Clear skies\nLookout", hill.value(projection.fields.single { it.name == "Notes" }.id)!!.textValue)
            val exported = runBlocking { app.trackRepository.exportCsv(trackId) }
            assertTrue(exported.contains("River loop"))
            assertTrue(exported.contains("\"Clear skies\nLookout\""))
            compose.onNodeWithTag("track-options-list")
                .performScrollToNode(hasText("Export Track CSV"))
            compose.onNodeWithText("Export Track CSV").performClick()
            assertTrue(device.wait(Until.hasObject(By.desc("Show roots")), 10_000))
            device.findObject(By.desc("Show roots")).click()
            assertTrue(device.wait(Until.hasObject(By.text("Downloads")), 10_000))
            device.findObjects(By.text("Downloads")).last().click()
            val filenameInput = device.wait(Until.findObject(By.clazz("android.widget.EditText")), 10_000)
            assertNotNull(filenameInput)
            filenameInput.text = exportFileName
            val save = device.wait(Until.findObject(By.text(java.util.regex.Pattern.compile("(?i)save"))), 10_000)
            assertNotNull(save)
            save.click()
            compose.waitUntil(15_000) { device.executeShellCommand("cat /sdcard/Download/$exportFileName") == exported }
            val form = runBlocking { requireNotNull(app.trackRepository.csvImportForm(trackId)) }
            val roundTrip = previewTrackCsvImport(form, exported,
                com.whip.app.ui.defaultTrackCsvMapping(projection, trackCsvHeaders(exported)), app.clock.today())
            assertEquals(0, roundTrip.invalidRows)
            assertEquals(runBlocking { projection.entries.map { app.trackRepository.prepareEntryEdit(it.entry.id)!!.draft } }, roundTrip.validDrafts)
        }
    }

    private fun fieldUuid(trackId: Long, name: String) = runBlocking {
        app.trackRepository.projection(trackId)!!.fields.single { it.name == name }.uuid
    }

    private fun csvScroll(matcher: SemanticsMatcher): SemanticsNodeInteraction {
        compose.onNodeWithTag("track-csv-import-content").performScrollToNode(matcher)
        return compose.onNode(matcher)
    }

    private fun selectDownload() {
        assertTrue(device.wait(Until.hasObject(By.desc("Show roots")), 10_000))
        device.findObject(By.desc("Show roots")).click()
        assertTrue(device.wait(Until.hasObject(By.text("Downloads")), 10_000))
        device.findObjects(By.text("Downloads")).last().click()
        assertTrue("Synthetic CSV must appear in the native Downloads picker", device.wait(Until.hasObject(By.text(fileName)), 10_000))
        device.findObject(By.text(fileName)).click()
    }

    private fun capture(id: String, visuallyDistinctFrom: String? = null) {
        compose.waitForIdle()
        captureVisualCatalogSurface(id, visuallyDistinctFrom)
    }
}
