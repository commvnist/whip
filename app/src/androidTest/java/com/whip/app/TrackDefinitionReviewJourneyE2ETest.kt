package com.whip.app

import android.content.Intent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** A real existing Track owns canonical review and exact destructive decisions through recovery. */
@RunWith(AndroidJUnit4::class)
class TrackDefinitionReviewJourneyE2ETest {
    private val compose = createEmptyComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()

    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun normalizedAuthorshipCompletesExistingTrackSave() = normalization(false)
    @Test @AndroidFontScale
    fun normalizedAuthorshipCompletesExistingTrackSaveAtLargeText() = normalization(true)
    @Test fun destructiveReviewPreservesReplacementAndRejectsChangedHistory() = removal(false)
    @Test @AndroidFontScale
    fun destructiveReviewPreservesReplacementAndRejectsChangedHistoryAtLargeText() = removal(true)
    @Test fun parentValidationRevealsEveryFailedSaveAndAllowsCorrection() = validation(false)
    @Test @AndroidFontScale
    fun parentValidationRevealsEveryFailedSaveAndAllowsCorrectionAtLargeText() = validation(true)

    private fun validation(large: Boolean) {
        val before = seed()
        val suffix = if (large) "large" else "ordinary"
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            open(before.track.id)
            editField("Terrain")
            replace("track-field-editor-list", hasTestTag("track-field-name"), " Name ")
            compose.onNodeWithText("Save Field").performClick()
            scroll("track-editor-list", hasText("Tags")).assertIsDisplayed()
            compose.onNodeWithText("Save", substring = false).performClick()
            capture("tracks.definition-review.duplicate-fields.$suffix")
            compose.onNodeWithTag("track-save-problem").assertIsDisplayed()
            compose.onNodeWithText("Field names must be unique within a Track", substring = true).assertIsDisplayed()
            if (large) assertTextScale("Field names must be unique within a Track")
            assertEquals(before, projection(before.track.id))
            // A repeated identical error must be revealed again after scrolling away.
            scroll("track-editor-list", hasText("Tags")).assertIsDisplayed()
            compose.onNodeWithText("Save", substring = false).performClick()
            compose.onNodeWithTag("track-save-problem").assertIsDisplayed()
            editField(" Name ")
            replace("track-field-editor-list", hasTestTag("track-field-name"), "Terrain")
            compose.onNodeWithText("Save Field").performClick()
            editField("Name")
            scroll("track-field-editor-list", hasText("Entry Identity")).performClick()
            compose.onNodeWithText("Save Field").performClick()
            scroll("track-editor-list", hasText("Tags")).assertIsDisplayed()
            compose.onNodeWithText("Save", substring = false).performClick()
            compose.onNodeWithTag("track-save-problem").assertIsDisplayed()
            compose.onNodeWithText("Choose at least one Entry Identity Field", substring = true).assertIsDisplayed()
            capture("tracks.definition-review.missing-identity.$suffix")
            scenario.recreate()
            compose.onNodeWithText("Choose at least one Entry Identity Field", substring = true).assertIsDisplayed()
            assertEquals(before, projection(before.track.id))
            editField("Name")
            scroll("track-field-editor-list", hasText("Entry Identity")).performClick()
            compose.onNodeWithText("Save Field").performClick()
            compose.onNodeWithText("Save", substring = false).performClick()
            awaitSaved("tracks.definition-review.validation-pending.$suffix")
            assertEquals(before.entries, projection(before.track.id).entries)
        }
    }

    private fun normalization(large: Boolean) {
        val before = seed()
        val destinationArea = runBlocking { app.areaRepository.create("Outdoors") }
        val suffix = if (large) "large" else "ordinary"
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            open(before.track.id)
            replace("track-editor-list", hasTestTag("track-editor-name"), "  Evening   walks  ")
            replace("track-editor-list", hasText("Description"), "  Route notes  ")
            replace("track-editor-list", hasText("Tags"), "park, PARK, ")
            scroll("track-editor-list", hasContentDescription("Area selection: Main")).performClick()
            compose.onNodeWithContentDescription("Area Outdoors").performClick()
            editField("Terrain")
            replace("track-field-editor-list", hasTestTag("track-field-name"), "  Surface   type  ")
            replace("track-field-editor-list", hasText("Option 2"), "  Paved   street  ")
            compose.onNodeWithText("Save Field").performClick()
            scenario.recreate()
            scroll("track-editor-list", hasTestTag("track-editor-name")).assertTextContains("  Evening   walks  ")
            compose.onNodeWithText("Save", substring = false).performClick()
            awaitSaved("tracks.definition-review.normalization-pending.$suffix")
            val after = projection(before.track.id)
            assertEquals("Evening walks", after.track.name)
            assertEquals("Route notes", after.track.description)
            assertEquals(listOf("park"), after.track.tags)
            assertEquals(destinationArea, after.track.areaId)
            assertEquals("Outdoors", after.track.area)
            assertEquals(before.entries.map { it.entry }, after.entries.map { it.entry })
            assertEquals(before.entries.map { it.values }, after.entries.map { it.values })
            assertEquals(before.fields.map { it.id to it.uuid }, after.fields.map { it.id to it.uuid })
            assertEquals(before.options.map { it.id to it.uuid }, after.options.map { it.id to it.uuid })
            assertEquals("Paved street", after.options.single { it.id == before.options.single { old -> old.label == "Street" }.id }.label)
            reopenEditor()
            scroll("track-editor-list", hasTestTag("track-editor-name")).assertTextContains("Evening walks")
            editField("Surface type")
            scroll("track-field-editor-list", hasText("Option 2")).assertIsDisplayed()
            capture("tracks.definition-review.normalized.$suffix")
            compose.onNodeWithText("Option 2").assertTextContains("Paved street")
            if (large) compose.assertDialogFontScale()
            replace("track-field-editor-list", hasText("Option 2"), "Discard this change")
            compose.onNodeWithText("Cancel", substring = false).performClick()
            compose.onNodeWithText("Discard Changes", substring = false).performClick()
            scenario.recreate()
            editField("Surface type")
            scroll("track-field-editor-list", hasText("Option 2")).assertTextContains("Paved street")
        }
    }

    private fun removal(large: Boolean) {
        val before = seed()
        val suffix = if (large) "large" else "ordinary"
        val terrain = before.fields.single { it.name == "Terrain" }
        val street = before.options.single { it.label == "Street" }
        val trail = before.options.single { it.label == "Trail" }
        val notes = before.fields.single { it.name == "Notes" }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            open(before.track.id)
            editField("Terrain")
            scroll("track-field-editor-list", hasContentDescription("Remove Option Street")).performClick()
            compose.onNodeWithText("Save Field").performClick()
            editField("Notes")
            scroll("track-field-editor-list", hasText("Delete Field")).performClick()
            compose.onNodeWithTag("track-field-removal-explanation").performScrollTo().assertIsDisplayed()
            capture("tracks.definition-review.remove-field.$suffix")
            assertCompleteText(hasTestTag("track-field-removal-explanation"))
            compose.onNodeWithText("Remove Field", substring = false).performClick()
            assertEquals(before, projection(before.track.id))
            compose.onNodeWithText("Save", substring = false).performClick()
            awaitReview()
            scroll("track-definition-removal-impact-list", hasText("Remove 2 saved values and 0 Choices")).assertIsDisplayed()
            scroll("track-definition-removal-impact-list", hasContentDescription("1 saved value: Delete Saved Values")).performClick()
            compose.onNodeWithContentDescription("1 saved value option: Move Values to Trail").performClick()
            compose.onNodeWithText("Review Updated Impact").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Apply Reviewed Changes").fetchSemanticsNodes().isNotEmpty() }
            scroll("track-definition-removal-impact-list", hasContentDescription("1 saved value: Move Values to Trail")).assertIsDisplayed()
            capture("tracks.definition-review.replace-choice.$suffix")
            assertCompleteText(hasText("Move Values to Trail"))
            if (large) compose.assertDialogFontScale()
            scenario.recreate()
            scroll("track-definition-removal-impact-list", hasContentDescription("1 saved value: Move Values to Trail")).assertIsDisplayed()
            assertEquals(before, projection(before.track.id))

            // New history changes the reviewed removal impact without changing the definition.
            addEntry(before, "Evening circuit", "Street", "New historical note", 2)
            val changed = projection(before.track.id)
            compose.onNodeWithText("Apply Reviewed Changes").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("track-persistence-save-problem").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("track-persistence-save-problem").assertIsDisplayed()
            capture("tracks.definition-review.changed-history.$suffix")
            assertEquals(changed, projection(before.track.id))
            compose.onNodeWithText("Save", substring = false).performClick()
            awaitReview()
            scroll("track-definition-removal-impact-list", hasText("Remove 3 saved values and 0 Choices")).assertIsDisplayed()
            scroll("track-definition-removal-impact-list", hasContentDescription("2 saved values: Move Values to Trail")).assertIsDisplayed()
            compose.onNodeWithText("Apply Reviewed Changes").performClick()
            awaitSaved("tracks.definition-review.removal-pending.$suffix")
            val after = projection(before.track.id)
            assertEquals(changed.entries.map { it.entry }, after.entries.map { it.entry })
            // Definition synchronization updates its own timestamps; identity/configuration are retained.
            assertEquals(changed.fields.filterNot { it.id == notes.id }.map { it.copy(updatedAtMillis = 0) }, after.fields.map { it.copy(updatedAtMillis = 0) })
            assertEquals(changed.options.filterNot { it.id == street.id }.map { it.copy(updatedAtMillis = 0) }, after.options.map { it.copy(updatedAtMillis = 0) })
            for (old in changed.entries) {
                val saved = after.entries.single { it.entry.id == old.entry.id }
                assertNull(saved.value(notes.id))
                assertEquals(trail.id, saved.value(terrain.id)?.choiceOptionId)
                val oldChoice = requireNotNull(old.value(terrain.id))
                val savedChoice = requireNotNull(saved.value(terrain.id))
                if (oldChoice.choiceOptionId == street.id) {
                    assertEquals(oldChoice.copy(choiceOptionId = trail.id, updatedAtMillis = savedChoice.updatedAtMillis), savedChoice)
                } else assertEquals(oldChoice, savedChoice)
                assertEquals(old.values.filterKeys { it != notes.id && it != terrain.id }, saved.values.filterKeys { it != notes.id && it != terrain.id })
            }
            reopenEditor()
            editField("Terrain")
            scroll("track-field-editor-list", hasText("Option 1")).assertTextContains("Trail")
            compose.onAllNodesWithContentDescription("Remove Option Street").assertCountEquals(0)
        }
    }

    private fun open(id: Long) {
        compose.onNodeWithContentDescription("Tracks tab").performClick()
        scroll("track-list", hasTestTag("track-card-$id")).performClick()
        reopenEditor()
    }
    private fun reopenEditor() {
        compose.onNodeWithContentDescription("Options").performClick()
        scroll("track-options-list", hasText("Edit Track")).performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("track-editor-name").fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }
    private fun editField(name: String) {
        scroll("track-editor-list", hasContentDescription("Edit Field $name")).performClick()
        closeSoftKeyboard()
    }
    private fun scroll(tag: String, matcher: SemanticsMatcher): SemanticsNodeInteraction {
        compose.onNodeWithTag(tag).performScrollToNode(matcher)
        return compose.onNode(matcher).performScrollTo()
    }
    private fun replace(tag: String, matcher: SemanticsMatcher, text: String) {
        scroll(tag, matcher).performClick().performTextReplacement(text)
        closeSoftKeyboard()
    }
    private fun awaitReview() = compose.waitUntil(10_000) {
        compose.onAllNodesWithTag("track-definition-removal-review").fetchSemanticsNodes().isNotEmpty()
    }
    private fun awaitSaved(failureCapture: String) {
        try {
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("track-editor-surface").fetchSemanticsNodes().isEmpty() }
        } catch (failure: Throwable) {
            capture(failureCapture)
            throw failure
        }
    }
    private fun capture(id: String) { compose.waitForIdle(); captureVisualCatalogSurface(id) }
    private fun assertCompleteText(matcher: SemanticsMatcher) {
        compose.onNode(matcher, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getResults ->
                val results = mutableListOf<TextLayoutResult>()
                assertTrue(getResults(results))
                assertTrue(results.isNotEmpty())
                results.forEach {
                    // A paragraph can retain a wider layout constraint than the final Text size.
                    // Check rendered lines; unused paragraph width is not clipped content.
                    assertFalse("Consequential text exceeds its height", it.didOverflowHeight)
                    for (line in 0 until it.lineCount) {
                        assertFalse("Consequential text is ellipsized", it.isLineEllipsized(line))
                        assertTrue("Text extends beyond its left edge", it.getLineLeft(line) >= -1f)
                        assertTrue("Text extends beyond its right edge", it.getLineRight(line) <= it.size.width + 1f)
                    }
                }
            }
    }
    private fun assertTextScale(message: String) {
        compose.onNodeWithText(message, substring = true, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getResults ->
                val results = mutableListOf<TextLayoutResult>()
                assertTrue(getResults(results))
                assertTrue(results.isNotEmpty())
                results.forEach { assertEquals(2f, it.layoutInput.density.fontScale, 0.01f) }
            }
    }
    private fun projection(id: Long) = runBlocking { requireNotNull(app.trackRepository.projection(id)) }
    private fun seed(): TrackProjection = runBlocking {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { AppSettings(setupCompleted = true, dynamicColor = false, themeMode = AppThemeMode.Light) }
        val id = app.trackRepository.create(TrackDraft("Walking review", fields = listOf(
            TrackFieldDraft("Name", TrackFieldType.ShortText, primary = true),
            TrackFieldDraft("Terrain", TrackFieldType.SingleChoice, options = listOf(TrackChoiceOptionDraft("Trail"), TrackChoiceOptionDraft("Street"))),
            TrackFieldDraft("Notes", TrackFieldType.LongText),
        )))
        val form = projection(id)
        addEntry(form, "River loop", "Trail", "Original river note", 0)
        addEntry(form, "Hill circuit", "Street", "Original hill note", 1)
        projection(id)
    }
    private fun addEntry(form: TrackProjection, name: String, choice: String, note: String, daysAgo: Long) = runBlocking {
        app.trackRepository.addEntry(form.track.id, TrackEntryDraft(
            entryDate = app.clock.today().minusDays(daysAgo),
            values = mapOf(
                form.primaryField.uuid to TrackValueDraft(textValue = name),
                form.fields.single { it.name == "Terrain" }.uuid to TrackValueDraft(choiceOptionUuid = form.options.single { it.label == choice }.uuid),
                form.fields.single { it.name == "Notes" }.uuid to TrackValueDraft(textValue = note),
            ),
        ))
    }
}
