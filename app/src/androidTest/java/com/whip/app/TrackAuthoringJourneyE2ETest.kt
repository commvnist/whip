package com.whip.app

import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityWindowInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackProjection
import com.whip.app.domain.UnitDimension
import java.io.File
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** Real nested definition authoring and all seven Entry value types, through persistence and reopening. */
@RunWith(AndroidJUnit4::class)
class TrackAuthoringJourneyE2ETest {
    private val compose = createEmptyComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val trackName = "Weekend walks and trail notes"
    private val entryName = "River trail after the rain"
    private val trackDescription = "Record routes, conditions and useful notes for the next walk."
    private val trackTags = listOf("outdoors", "weekend")

    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun mixedTrackCanBeAuthoredRecoveredAndUsed() = verifyAuthoring(false)

    @Test @AndroidFontScale
    fun mixedTrackCanBeAuthoredRecoveredAndUsedAtLargeText() = verifyAuthoring(true)

    private fun verifyAuthoring(large: Boolean) {
        runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update {
                AppSettings(setupCompleted = true, dynamicColor = false, themeMode = AppThemeMode.Light,
                    distanceUnitId = "mile", numberPrecision = 3)
            }
        }
        val scale = if (large) "large" else "ordinary"
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Tracks tab").performClick()
            scroll("track-list", hasText("Create First Track")).performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("track-editor-name").fetchSemanticsNodes().isNotEmpty() }
            capture("tracks.authoring.track-start.$scale")
            assertReadableEditorColumn("track-editor-list")
            compose.onNodeWithTag("track-editor-name").performClick().performTextReplacement(trackName)
            waitForNativeKeyboard()
            capture("tracks.authoring.track-name.$scale")
            assertNativeInputVisible("track-editor-name", trackName, "Track Name *")
            closeSoftKeyboard()

            addField("Distance", "Number")
            field(hasContentDescription("Measurement Type: Count")).performClick()
            compose.onNodeWithContentDescription("Measurement Type option: Distance").performClick()
            field(hasContentDescription("Unit: miles (mi)")).assertIsDisplayed()
            field(hasContentDescription("Decimal Places: 3")).assertIsDisplayed()
            field(hasText("Required")).performClick()
            field(hasTestTag("track-field-name")).performClick().performTextReplacement("Distance")
            scenario.recreate()
            compose.onNodeWithTag("track-field-name").assertTextContains("Distance")
            field(hasTestTag("track-field-name")).performClick()
            capture("tracks.authoring.field-number.$scale")
            assertNativeInputVisible("track-field-name", "Distance")
            if (large) compose.assertDialogFontScale()
            closeSoftKeyboard()
            field(hasContentDescription("Unit: miles (mi)")).assertIsDisplayed()
            field(hasContentDescription("Decimal Places: 3")).assertIsDisplayed()
            compose.onNodeWithText("Save Field").performClick()

            addField("Terrain", "Single Choice")
            field(hasText("Option 1")).performClick().performTextReplacement("Trail")
            closeSoftKeyboard()
            field(hasText("Add Choice Option")).performClick()
            field(hasText("Option 2")).performClick().performTextReplacement("Street")
            closeSoftKeyboard()
            field(hasText("Add Choice Option")).performClick()
            field(hasText("Option 3")).performClick().performTextReplacement("Temporary")
            closeSoftKeyboard()
            field(hasContentDescription("Remove Option Temporary")).performClick()
            val moveTrail = field(hasContentDescription("Reorder Trail")).fetchSemanticsNode()
                .config[SemanticsActions.CustomActions].single { it.label == "Move Trail down" }
            compose.runOnIdle { assertTrue(moveTrail.action()) }
            field(hasText("Option 1")).assertTextContains("Street")
            capture("tracks.authoring.field-choice.$scale")
            compose.onNodeWithText("Save Field").performClick()

            addField("Effort", "Scale")
            field(hasTestTag("track-scale-increment")).performClick().performTextReplacement("0.5")
            closeSoftKeyboard()
            field(hasText("Low Label")).performClick().performTextReplacement("Easy")
            closeSoftKeyboard()
            field(hasText("High Label")).performClick().performTextReplacement("Hard")
            closeSoftKeyboard()
            field(hasTestTag("track-scale-increment")).assertTextContains("0.5")
            capture("tracks.authoring.field-scale.$scale")
            compose.onNodeWithText("Save Field").performClick()

            for ((name, type) in listOf("Notes" to "Long Text", "Visit date" to "Date", "Rained" to "Yes/No")) {
                addField(name, type)
                compose.onNodeWithText("Save Field").performClick()
            }
            scroll("track-editor-list", hasContentDescription("Edit Field Distance")).performClick()
            field(hasTestTag("track-field-name")).performClick().performTextReplacement("Unsaved distance")
            closeSoftKeyboard()
            compose.onNodeWithText("Cancel").performClick()
            compose.onNodeWithText("Discard Changes").performClick()
            scroll("track-editor-list", hasContentDescription("Edit Field Distance")).assertIsDisplayed()
            capture("tracks.authoring.definition.$scale")
            scroll("track-editor-list", hasText("Description")).performClick().performTextReplacement(trackDescription)
            closeSoftKeyboard()
            scroll("track-editor-list", hasText("Tags")).performClick().performTextReplacement(trackTags.joinToString(", "))
            closeSoftKeyboard()
            compose.onNodeWithText("Save").performClick()

            val definition = awaitProjection { it.fields.size == 7 }
            assertEquals(trackDescription, definition.track.description)
            assertEquals(trackTags, definition.track.tags)
            assertEquals(listOf("Name", "Distance", "Terrain", "Effort", "Notes", "Visit date", "Rained"), definition.fields.map { it.name })
            assertEquals(TrackFieldType.entries.toSet(), definition.fields.map { it.type }.toSet())
            val distance = definition.fields.single { it.name == "Distance" }
            assertTrue(distance.required)
            assertEquals(UnitDimension.Distance, distance.dimension)
            assertEquals("mile", distance.unitId)
            assertEquals(3, distance.precision)
            val terrain = definition.fields.single { it.name == "Terrain" }
            assertEquals(listOf("Street", "Trail"), definition.optionsFor(terrain.id).map { it.label })
            assertEquals(0.5, definition.fields.single { it.name == "Effort" }.scaleStep, 0.0)
            scenario.recreate()
            compose.onNodeWithContentDescription("Add entry to $trackName").performClick()
            capture("tracks.authoring.entry-start.$scale")
            assertReadableEditorColumn("track-entry-editor-list")
            assertWholeNativeField("track-entry-short-text-${definition.primaryField.uuid}", "Name *")
            assertWholeNativeField("track-entry-number-${distance.uuid}", "Distance * (mi)")
            compose.onNodeWithText("Add", substring = false).performClick()
            scroll("track-entry-editor-list", hasTestTag("track-entry-save-problem")).assertIsDisplayed()
            capture("tracks.authoring.entry-validation.$scale")

            fun entryField(name: String, prefix: String) = scroll("track-entry-editor-list",
                hasTestTag("track-entry-$prefix-${definition.fields.single { it.name == name }.uuid}"))
            entryField("Name", "short-text").performClick().performTextReplacement(entryName)
            waitForNativeKeyboard()
            capture("tracks.authoring.entry-name.$scale")
            assertNativeInputVisible("track-entry-short-text-${definition.primaryField.uuid}", entryName, "Name *")
            closeSoftKeyboard()
            entryField("Distance", "number").performClick().performTextReplacement("1.25")
            closeSoftKeyboard()
            scroll("track-entry-editor-list", hasContentDescription("Choose Terrain: Unanswered")).performClick()
            compose.onNodeWithContentDescription("Choose Terrain option: Trail").performClick()
            scroll("track-entry-editor-list", hasContentDescription("Increase Effort by 0.5"))
            repeat(6) { compose.onNodeWithContentDescription("Increase Effort by 0.5").performClick() }
            compose.onNodeWithTag("track-entry-scale-value").assertTextContains("3.5")
            entryField("Notes", "long-text").performClick().performTextReplacement("Wet leaves on the return path.\nBring waterproof shoes next time.")
            closeSoftKeyboard()
            scroll("track-entry-editor-list", hasContentDescription("Visit date, choose date")).performClick()
            compose.onNodeWithTag("date-picker-selected-date")
                .assertTextEquals(app.clock.today().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)))
            if (large) {
                compose.onNodeWithTag("date-picker-wheel-selector").performScrollTo().assertIsDisplayed()
                compose.assertDialogFontScale()
            }
            capture("tracks.authoring.entry-date.$scale")
            if (large) {
                val wheel = device.findObject(By.desc("Year picker"))?.visibleBounds
                assertTrue("Complete native Year wheel: $wheel", wheel != null &&
                    wheel.height() >= 144f * app.resources.displayMetrics.density - 1f)
            }
            compose.onNodeWithText("Set", substring = false).performClick()
            scroll("track-entry-editor-list", hasContentDescription("Rained, yes")).performClick()
            capture("tracks.authoring.entry-types.$scale")
            scenario.recreate()
            entryField("Distance", "number").assertTextContains("1.25")
            compose.onNodeWithText("Add", substring = false).performClick()

            val saved = awaitProjection { it.entries.size == 1 }
            val firstEntry = saved.entries.single()
            fun value(name: String) = firstEntry.value(saved.fields.single { it.name == name }.id)!!
            assertEquals(entryName, value("Name").textValue)
            assertEquals(1.25, value("Distance").enteredNumber!!, 0.0)
            assertEquals(2011.68, value("Distance").canonicalNumber!!, 0.000001)
            assertEquals("mile", value("Distance").enteredUnitId)
            assertEquals(saved.optionsFor(terrain.id).single { it.label == "Trail" }.id, value("Terrain").choiceOptionId)
            assertEquals(3.5, value("Effort").scaleValue!!, 0.0)
            assertEquals("Wet leaves on the return path.\nBring waterproof shoes next time.", value("Notes").textValue)
            assertEquals(app.clock.today(), value("Visit date").dateValue)
            assertEquals(true, value("Rained").booleanValue)
            scenario.recreate()
            scroll("track-entry-list", hasContentDescription("Edit Entry $entryName")).performClick()
            entryField("Distance", "number").performClick().performTextReplacement("1..5")
            closeSoftKeyboard()
            compose.onNodeWithText("Save", substring = false).performClick()
            entryField("Distance", "number").assertTextContains("1..5")
            scenario.recreate()
            entryField("Distance", "number").assertTextContains("1..5")
            capture("tracks.authoring.entry-number.$scale")
            entryField("Distance", "number").performClick().performTextReplacement("2.25")
            closeSoftKeyboard()
            compose.onNodeWithText("Save", substring = false).performClick()
            val edited = awaitProjection { it.entries.singleOrNull()?.value(distance.id)?.enteredNumber == 2.25 }
            assertEquals(definition.fields, edited.fields)
            assertEquals(definition.options, edited.options)
            assertEquals(firstEntry.entry.id, edited.entries.single().entry.id)
            val editedEntry = edited.entries.single()
            val unchangedValues = firstEntry.values.filterKeys { it != distance.id }
            // Saving updates every value's revision time; identities and authored data must survive.
            assertEquals(unchangedValues, editedEntry.values.filterKeys { it != distance.id }.mapValues { (fieldId, value) ->
                value.copy(updatedAtMillis = unchangedValues.getValue(fieldId).updatedAtMillis)
            })
            assertEquals(firstEntry.entry.uuid, editedEntry.entry.uuid)
            assertEquals(firstEntry.entry.entryDate, editedEntry.entry.entryDate)
            assertEquals(3621.024, edited.entries.single().value(distance.id)!!.canonicalNumber!!, 0.000001)
            scenario.recreate()
            scroll("track-entry-list", hasContentDescription("Edit Entry $entryName")).performClick()
            entryField("Distance", "number").assertTextContains("2.25")
            capture("tracks.authoring.entry-reopened.$scale")
            compose.onNodeWithContentDescription("Close Entry Editor").performClick()
            compose.onNodeWithTag("track-destination-Options").performClick()
            compose.onNodeWithText("Edit Track").performScrollTo().performClick()
            scroll("track-editor-list", hasText("Description")).assertTextContains(trackDescription)
            capture("tracks.authoring.details-reopened.$scale")
            scroll("track-editor-list", hasText("Tags")).assertTextContains(trackTags.joinToString(", "))
            compose.onNodeWithContentDescription("Close Track Editor").performClick()
        }
    }

    private fun addField(name: String, type: String) {
        scroll("track-editor-list", hasText("Add Field")).performClick()
        field(hasTestTag("track-field-name")).performClick().performTextReplacement(name)
        closeSoftKeyboard()
        field(hasContentDescription("Field Type: Short Text")).performClick()
        compose.onNodeWithContentDescription("Field Type option: $type").performClick()
    }

    private fun field(matcher: SemanticsMatcher) = scroll("track-field-editor-list", matcher)

    private fun scroll(listTag: String, matcher: SemanticsMatcher): SemanticsNodeInteraction {
        compose.onNodeWithTag(listTag).performScrollToNode(matcher)
        return compose.onNode(matcher)
    }

    private fun awaitProjection(predicate: (TrackProjection) -> Boolean): TrackProjection = runBlocking {
        withTimeout(10_000) {
            val observed = app.trackRepository.projections.first { rows -> rows.any { it.track.name == trackName && predicate(it) } }
                .single { it.track.name == trackName }
            // Check the committed repository contents as well as observing the live projection.
            checkNotNull(app.trackRepository.projection(observed.track.id))
        }
    }

    private fun waitForNativeKeyboard() {
        compose.waitUntil(10_000) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
        }
    }

    private fun assertReadableEditorColumn(listTag: String) {
        val body = compose.onNodeWithTag(listTag).getUnclippedBoundsInRoot()
        val header = compose.onNodeWithTag("editor-header").getUnclippedBoundsInRoot()
        val available = compose.onNodeWithTag("full-screen-destination-content").getUnclippedBoundsInRoot()
        assertTrue("Serial authoring should have a readable width: $body", (body.right - body.left).value <= 800f)
        assertEquals("Header and fields share their leading edge", body.left.value, header.left.value, 1f)
        assertEquals("Header and fields share their trailing edge", body.right.value, header.right.value, 1f)
        assertEquals("Form is centered inside the available content", (available.left.value + available.right.value) / 2f,
            (body.left.value + body.right.value) / 2f, 1f)
    }

    private fun assertWholeNativeField(tag: String, labelText: String) {
        compose.onNodeWithTag(tag).assertIsDisplayed()
        refreshAccessibilityHierarchy("Track primary field visibility")
        val label = device.findObject(By.text(labelText))
        val native = label?.parent?.takeIf { it.className == "android.widget.EditText" }?.visibleBounds
        val layout = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()
        assertTrue("Whole $labelText input: $native versus $layout", native != null &&
            native.height() >= (layout.bottom - layout.top).value * app.resources.displayMetrics.density - 1f)
        val labelLayout = compose.onNodeWithText(labelText, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("Whole $labelText label", label != null &&
            label.visibleBounds.height() >= (labelLayout.bottom - labelLayout.top).value * app.resources.displayMetrics.density - 1f)
    }

    private fun assertNativeInputVisible(tag: String, enteredText: String, labelText: String = "Field Name *") {
        waitForNativeKeyboard()
        refreshAccessibilityHierarchy("Track authoring input")
        val native = device.findObject(By.clazz("android.widget.EditText").text(enteredText))?.visibleBounds
        val layout = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()
        val ime = android.graphics.Rect()
        InstrumentationRegistry.getInstrumentation().uiAutomation.windows.single { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }.getBoundsInScreen(ime)
        assertTrue("Whole Field input above keyboard: $native versus $layout and $ime",
            native != null && native.bottom <= ime.top && native.height() >= (layout.bottom - layout.top).value * app.resources.displayMetrics.density - 1f)
        val labelLayout = compose.onNodeWithText(labelText, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val label = device.findObject(By.text(labelText))?.visibleBounds
        assertTrue("Whole $labelText label: $label versus $labelLayout",
            label != null && label.height() >= (labelLayout.bottom - labelLayout.top).value * app.resources.displayMetrics.density - 1f)
    }

    private fun capture(id: String) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().uiAutomation.waitForIdle(750L, 5_000L)
        device.waitForIdle()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) captureVisualCatalogSurface(id)
        else {
            val directory = checkNotNull(app.getExternalFilesDir("track-authoring-journey"))
            check(directory.isDirectory || directory.mkdirs())
            check(device.takeScreenshot(File(directory, "$id.png")))
            device.dumpWindowHierarchy(File(directory, "$id.xml"))
        }
    }
}
