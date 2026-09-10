package com.whip.app

import android.content.Intent
import androidx.compose.ui.semantics.SemanticsProperties
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

/** Existing typed history survives corrected Field configuration and nested recreation. */
@RunWith(AndroidJUnit4::class)
class TrackFieldEditingJourneyE2ETest {
    private val compose = createEmptyComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()

    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun choiceLabelsAreValidatedBeforeLeavingTheField() = verifyEditing(false, choices = true)
    @Test @AndroidFontScale
    fun choiceLabelsAreValidatedBeforeLeavingTheFieldAtLargeText() = verifyEditing(true, choices = true)
    @Test fun scaleAndUnitChangesPreserveExistingValues() = verifyEditing(false, choices = false)
    @Test @AndroidFontScale
    fun scaleAndUnitChangesPreserveExistingValuesAtLargeText() = verifyEditing(true, choices = false)

    @Test fun incompatibleScaleHistoryCanBeCorrected() = incompatibleScale(false)
    @Test @AndroidFontScale
    fun incompatibleScaleHistoryCanBeCorrectedAtLargeText() = incompatibleScale(true)

    private fun incompatibleScale(large: Boolean) {
        val before = seed()
        val suffix = if (large) "large" else "ordinary"
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Tracks tab").performClick()
            scroll("track-list", hasTestTag("track-card-${before.track.id}")).performClick()
            compose.onNodeWithContentDescription("Options").performClick()
            scroll("track-options-list", hasText("Edit Track")).performClick()
            editField("Effort")
            replace("Maximum", "2")
            field(hasText("Maximum")).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
            field(hasText("Keep saved value 2.5 selectable.")).assertIsDisplayed()
            compose.onNodeWithText("Save Field").assertIsNotEnabled()
            capture("tracks.scale-history.range.$suffix")
            if (large) assertErrorFontScale("Keep saved value 2.5 selectable.")
            assertEquals(before, runBlocking { app.trackRepository.projection(before.track.id) })
            scenario.recreate()
            field(hasText("Maximum")).assertTextContains("2")
            compose.onNodeWithText("Save Field").assertIsNotEnabled()
            replace("Maximum", "5")
            replace("Minimum", "3")
            field(hasText("Minimum")).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
            compose.onNodeWithText("Save Field").assertIsNotEnabled()
            replace("Minimum", "1")
            replace("Increment", "1")
            field(hasText("Increment")).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
            field(hasText("Keep saved value 2.5 selectable.")).assertIsDisplayed()
            compose.onNodeWithText("Save Field").assertIsNotEnabled()
            capture("tracks.scale-history.increment.$suffix")
            scenario.recreate()
            field(hasText("Increment")).assertTextContains("1")
            replace("Increment", "0.25")
            replace("Maximum", "4")
            compose.onNodeWithText("Save Field").assertIsEnabled().performClick()
            // History may change after local validation; commit must still reject it atomically.
            runBlocking {
                app.trackRepository.addEntry(before.track.id, TrackEntryDraft(
                    entryDate = app.clock.today(),
                    values = mapOf(
                        before.primaryField.uuid to TrackValueDraft(textValue = "New hill climb"),
                        before.fields.single { it.name == "Effort" }.uuid to TrackValueDraft(scaleValue = 4.5),
                    ),
                ))
            }
            val changed = runBlocking { requireNotNull(app.trackRepository.projection(before.track.id)) }
            compose.onNodeWithText("Save", substring = false).performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("track-persistence-save-problem").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("track-persistence-save-problem").assertIsDisplayed()
            capture("tracks.scale-history.changed-history.$suffix")
            compose.onNodeWithContentDescription("Effort: existing Scale value 4.5", substring = true).assertIsDisplayed()
            assertEquals(changed, runBlocking { app.trackRepository.projection(before.track.id) })
            scenario.recreate()
            editField("Effort")
            field(hasText("Maximum")).assertTextContains("4")
            field(hasText("Keep saved value 4.5 selectable.")).assertIsDisplayed()
            compose.onNodeWithText("Save Field").assertIsNotEnabled()
            replace("Maximum", "5")
            compose.onNodeWithText("Save Field").assertIsEnabled().performClick()
            compose.onNodeWithText("Save", substring = false).performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("track-editor-surface").fetchSemanticsNodes().isEmpty() }
            val after = runBlocking { requireNotNull(app.trackRepository.projection(before.track.id)) }
            assertEquals(changed.entries, after.entries)
            assertEquals(before.fields.map { it.id to it.uuid }, after.fields.map { it.id to it.uuid })
            assertEquals(0.25, after.fields.single { it.name == "Effort" }.scaleStep, 0.0)
            compose.onNodeWithContentDescription("Options").performClick()
            scroll("track-options-list", hasText("Edit Track")).performClick()
            editField("Effort")
            field(hasText("Increment")).assertTextContains("0.25")
            capture("tracks.scale-history.corrected.$suffix")
        }
    }

    private fun verifyEditing(large: Boolean, choices: Boolean) {
        val before = seed()
        val suffix = if (large) "large" else "ordinary"
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Tracks tab").performClick()
            scroll("track-list", hasTestTag("track-card-${before.track.id}")).performClick()
            compose.onNodeWithContentDescription("Options").performClick()
            scroll("track-options-list", hasText("Edit Track")).performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("track-editor-name").fetchSemanticsNodes().isNotEmpty() }
            if (choices) {
                editField("Terrain")
                field(hasContentDescription("Field Type: Single Choice")).assertIsNotEnabled()
                replace("Option 2", "  tRAIL  ")
                capture("tracks.field-edit.choice-invalid.$suffix")
                compose.onNodeWithText("Save Field").assertIsNotEnabled()
                field(hasTestTag("track-field-choice-1")).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
                assertTrue(compose.onAllNodesWithText("This label is already used").fetchSemanticsNodes().isNotEmpty())
                if (large) compose.assertDialogFontScale()
                scenario.recreate()
                field(hasText("Option 2")).assertTextContains("  tRAIL  ")
                compose.onNodeWithText("Save Field").assertIsNotEnabled()
                replace("Option 2", "Paved street")
                compose.onNodeWithText("Save Field").assertIsEnabled().performClick()
            } else {
                editField("Effort")
                field(hasContentDescription("Field Type: Scale")).assertIsNotEnabled()
                replace("Minimum", "bad")
                field(hasText("Minimum")).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
                field(hasText("Enter a whole-number Scale minimum")).assertIsDisplayed()
                capture("tracks.field-edit.scale-minimum-invalid.$suffix")
                compose.onNodeWithText("Save Field").assertIsNotEnabled()
                if (large) assertErrorFontScale("Enter a whole-number Scale minimum")
                scenario.recreate()
                field(hasText("Minimum")).assertTextContains("bad")
                replace("Minimum", "-2")
                replace("Maximum", "-2")
                field(hasText("Maximum")).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
                field(hasText("Scale maximum must be greater than its minimum")).assertIsDisplayed()
                compose.onNodeWithText("Save Field").assertIsNotEnabled()
                replace("Maximum", "5")
                replace("Increment", "0.3")
                field(hasText("Scale increment must land exactly on the maximum")).assertIsDisplayed()
                compose.onNodeWithText("Save Field").assertIsNotEnabled()
                replace("Increment", "0.5")
                compose.onNodeWithText("Save Field").assertIsEnabled().performClick()
                editField("Distance")
                field(hasContentDescription("Field Type: Number")).assertIsNotEnabled()
                field(hasContentDescription("Measurement Type: Distance")).assertIsNotEnabled()
                field(hasContentDescription("Unit: miles (mi)")).performClick()
                compose.onNodeWithText("kilometres (km)").performScrollTo().performClick()
                field(hasContentDescription("Unit: kilometres (km)")).assertIsDisplayed()
                field(hasText("New Entries start with this unit. Changing it keeps saved values in the units originally entered.")).assertIsDisplayed()
                capture("tracks.field-edit.number-history.$suffix")
                compose.onNodeWithText("Save Field").performClick()
            }
            compose.onNodeWithText("Save", substring = false).performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("track-editor-surface").fetchSemanticsNodes().isEmpty() }
            val after = runBlocking { requireNotNull(app.trackRepository.projection(before.track.id)) }
            assertEquals(before.entries.map { it.entry }, after.entries.map { it.entry })
            assertEquals(before.entries.map { it.values }, after.entries.map { it.values })
            assertEquals(before.fields.map { it.id to it.uuid }, after.fields.map { it.id to it.uuid })
            if (choices) {
                val option = before.options.single { it.label == "Street" }
                assertEquals("Paved street", after.options.single { it.id == option.id }.label)
                assertEquals(before.options.map { it.id to it.uuid }, after.options.map { it.id to it.uuid })
            } else {
                val effort = after.fields.single { it.name == "Effort" }
                assertEquals(-2, effort.scaleMin)
                assertEquals(5, effort.scaleMax)
                assertEquals(0.5, effort.scaleStep, 0.0)
                assertEquals("kilometre", after.fields.single { it.name == "Distance" }.unitId)
            }
            // Reopen persisted configuration through the same real Track route.
            compose.onNodeWithContentDescription("Options").performClick()
            scroll("track-options-list", hasText("Edit Track")).performClick()
            editField(if (choices) "Terrain" else "Distance")
            if (choices) field(hasText("Option 2")).assertTextContains("Paved street")
            else field(hasContentDescription("Unit: kilometres (km)")).assertIsDisplayed()
        }
    }

    private fun seed(): TrackProjection = runBlocking {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { AppSettings(setupCompleted = true, dynamicColor = false, themeMode = AppThemeMode.Light) }
        val id = app.trackRepository.create(TrackDraft("Walking field review", fields = listOf(
            TrackFieldDraft("Name", TrackFieldType.ShortText, primary = true),
            TrackFieldDraft("Terrain", TrackFieldType.SingleChoice, options = listOf(TrackChoiceOptionDraft("Trail"), TrackChoiceOptionDraft("Street"))),
            TrackFieldDraft("Effort", TrackFieldType.Scale, scaleMin = 1, scaleMax = 5, scaleStep = 0.5),
            TrackFieldDraft("Distance", TrackFieldType.Number, dimension = UnitDimension.Distance, unitId = "mile", precision = 2),
        )))
        val form = requireNotNull(app.trackRepository.projection(id))
        val terrain = form.fields.single { it.name == "Terrain" }
        for ((index, name) in listOf("River loop", "Hill circuit").withIndex()) {
            app.trackRepository.addEntry(id, TrackEntryDraft(entryDate = app.clock.today().minusDays(index.toLong()), values = mapOf(
                form.primaryField.uuid to TrackValueDraft(textValue = name),
                terrain.uuid to TrackValueDraft(choiceOptionUuid = form.optionsFor(terrain.id)[index].uuid),
                form.fields.single { it.name == "Effort" }.uuid to TrackValueDraft(scaleValue = 2.5 + index),
                form.fields.single { it.name == "Distance" }.uuid to TrackValueDraft(enteredNumber = 1.25 + index, enteredUnitId = "mile"),
            )))
        }
        requireNotNull(app.trackRepository.projection(id))
    }

    private fun editField(name: String) {
        scroll("track-editor-list", hasContentDescription("Edit Field $name")).performClick()
        closeSoftKeyboard()
    }
    private fun field(matcher: SemanticsMatcher) = scroll("track-field-editor-list", matcher)
    private fun scroll(tag: String, matcher: SemanticsMatcher): SemanticsNodeInteraction {
        compose.onNodeWithTag(tag).performScrollToNode(matcher)
        // A tall lazy item can contain the target while leaving that child outside the viewport.
        return compose.onNode(matcher).performScrollTo()
    }
    private fun replace(label: String, value: String) {
        field(hasText(label)).performClick().performTextReplacement(value)
        closeSoftKeyboard()
    }
    private fun capture(id: String) {
        compose.waitForIdle()
        captureVisualCatalogSurface(id)
    }

    private fun assertErrorFontScale(message: String) {
        // Check the actual visible error, excluding lazy prefetched nodes without a text layout yet.
        compose.onNodeWithText(message, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getResults ->
                val results = mutableListOf<TextLayoutResult>()
                assertTrue(getResults(results))
                assertTrue(results.isNotEmpty())
                results.forEach { assertEquals(2f, it.layoutInput.density.fontScale, 0.01f) }
            }
    }
}
