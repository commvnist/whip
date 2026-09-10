package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Unit labels may evolve; existing definitions and recorded conversion meaning stay exact. */
class CustomUnitJourneyE2ETest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    private val longName = "Afternoon walking circuit through the riverside and community garden"
    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun createRenameVersionAndArchivePreserveRecordedMeaning() {
        runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update {
                AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark, dynamicColor = false)
            }
            app.measurementRepository.createCustomUnitExact("circuit", longName, "lap", UnitDimension.Distance, 1250.0)
            app.measurementRepository.createCustomUnitExact("glass", "Kitchen glass", "gl", UnitDimension.Volume, 250.0)
            app.measurementRepository.setCustomUnitArchived("glass", true)
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Open Settings").performClick()
            compose.openSettingsCategory("Planning & Units")
            reveal(hasText("Custom Units"))
            captureVisualCatalogSurface("settings.units.collection")
            reveal(hasText("Create Custom Unit"))
            compose.onNodeWithText("Create Custom Unit").performClick()
            compose.onNodeWithTag("custom-unit-name").performTextReplacement("Bundle")
            compose.onNodeWithTag("custom-unit-symbol").performTextReplacement("bdl")
            compose.onNodeWithTag("custom-unit-factor").performTextReplacement("6")
            closeSoftKeyboard()
            compose.onNodeWithTag("custom-unit-factor").assertTextContains("6")
            captureVisualCatalogSurface("settings.units.create")
            scenario.recreate()
            compose.onNodeWithTag("custom-unit-name").assertTextContains("Bundle")
            compose.onNodeWithTag("custom-unit-factor").assertTextContains("6")
            confirm()
            val original = units().single { it.name == "Bundle" }
            assertEquals(6.0, original.toCanonicalFactor, 0.0)
            assertEquals(UnitDimension.Count, original.dimension)
            runBlocking {
                val measurement = app.measurementRepository.createMeasurement("Packing", MeasurementValueKind.Integer, UnitDimension.Count, original.id)
                app.measurementRepository.record(measurement, 2.0, original.id, note = "Two original bundles")
            }
            val definitions = runBlocking { app.measurementRepository.measurements.first() }
            val entries = runBlocking { app.measurementRepository.entries.first() }
            menu(original)
            compose.onNodeWithText("Rename", substring = false).assertIsDisplayed()
            compose.onNodeWithText("Create New Version").assertIsDisplayed()
            captureVisualCatalogSurface("settings.units.actions")
            compose.onNodeWithText("Rename", substring = false).performClick()
            compose.onNodeWithTag("custom-unit-name").performTextReplacement("Travel bundle")
            compose.onNodeWithTag("custom-unit-name").assertTextContains("Travel bundle")
            scenario.recreate()
            compose.onNodeWithTag("custom-unit-name").assertTextContains("Travel bundle")
            closeSoftKeyboard()
            confirm()
            val renamed = units().single { it.id == original.id }
            assertEquals(original.copy(name = "Travel bundle", updatedAtMillis = renamed.updatedAtMillis), renamed)
            menu(renamed)
            compose.onNodeWithText("Create New Version").performClick()
            compose.onNodeWithTag("custom-unit-name").performTextReplacement("Travel bundle, larger")
            compose.onNodeWithTag("custom-unit-factor").performTextReplacement("12")
            closeSoftKeyboard()
            captureVisualCatalogSurface("settings.units.version")
            confirm()
            val version = units().single { it.name == "Travel bundle, larger" }
            assertTrue(version.id != original.id)
            assertEquals(12.0, version.toCanonicalFactor, 0.0)
            assertTrue(units().single { it.id == original.id }.archived)
            menu(version)
            compose.onNodeWithText("Archive", substring = false).performClick()
            compose.waitUntil(10_000) { units().single { it.id == version.id }.archived }
            menu(units().single { it.id == version.id })
            compose.onAllNodesWithText("Create New Version").assertCountEquals(0)
            captureVisualCatalogSurface("settings.units.archived-actions")
            compose.onNodeWithText("Restore", substring = false).performClick()
            compose.waitUntil(10_000) { !units().single { it.id == version.id }.archived }
            scenario.recreate()
            reveal(hasText("Create Custom Unit"))
            captureVisualCatalogSurface("settings.units.restored")
            assertEquals(definitions, runBlocking { app.measurementRepository.measurements.first() })
            assertEquals(entries, runBlocking { app.measurementRepository.entries.first() })
            val retained = units().single { it.id == original.id }
            val restored = units().single { it.id == version.id }
            assertEquals(renamed.copy(archived = true, updatedAtMillis = retained.updatedAtMillis), retained)
            assertEquals(version.copy(updatedAtMillis = restored.updatedAtMillis), restored)
        }
    }

    private fun units() = runBlocking { app.measurementRepository.customUnits.first() }
    private fun confirm() {
        compose.onNodeWithTag("custom-unit-confirm").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("custom-unit-dialog").fetchSemanticsNodes().isEmpty() }
    }
    private fun reveal(matcher: SemanticsMatcher) {
        compose.onNodeWithTag("settings-list").performScrollToNode(matcher)
        compose.onNode(matcher).performScrollTo().assertIsDisplayed()
    }
    private fun menu(unit: UnitDefinition) {
        // Both the original row and the shared record renderer expose an identity-qualified action.
        val matcher = hasContentDescription("Options for ${unit.name}") or
            hasContentDescription("More Actions for ${unit.name}${unit.symbol.takeIf(String::isNotBlank)?.let { " ($it)" }.orEmpty()}")
        reveal(matcher)
        compose.onNode(matcher).performClick()
    }
}
