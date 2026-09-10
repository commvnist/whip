package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.core.AreaOpeningMode
import com.whip.app.domain.AreaScope
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingItemBuilderJourneyE2ETest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun choicesSaveImmediatelyWhileTypedDraftsWaitForConfirmedSave() {
        val areaName = "Research, writing and career development"
        runBlocking {
            app.backupRepository.deleteAllData()
            val areaId = app.areaRepository.create(areaName)
            app.settingsRepository.update {
                AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark, dynamicColor = false,
                    areaOpeningMode = AreaOpeningMode.Chosen, chosenOpeningAreaScope = AreaScope.One(areaId).storageKey)
            }
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Open Settings").performClick()
            compose.openSettingsCategory("Appearance & Home")
            compose.onNodeWithText("Show advanced controls by default").performClick()
            compose.waitUntil(10_000) { app.settingsRepository.current().powerMode }
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Opening area"))
            compose.onNodeWithContentDescription("Opening area: $areaName").assertIsDisplayed()
            captureVisualCatalogSurface("settings.item-builder.choice-value")
            compose.onNodeWithContentDescription("Opening area: $areaName").performClick()
            compose.onNodeWithContentDescription("Opening area option: All Areas").performClick()
            compose.waitUntil(10_000) { app.settingsRepository.current().chosenOpeningAreaScope == AreaScope.All.storageKey }
            compose.openSettingsCategory("Planning & Units")
            val field = "settings-field-default-decimal-precision"
            compose.onNodeWithTag("settings-list").performScrollToNode(hasTestTag(field))
            val original = app.settingsRepository.current().numberPrecision
            compose.onNodeWithTag(field).performClick()
            compose.onNodeWithTag("$field-input").performTextReplacement("3")
            // Apply the entered text to Compose's saveable state before the platform recreates it.
            compose.onNodeWithTag("$field-input").assertTextContains("3")
            assertEquals(original, app.settingsRepository.current().numberPrecision)
            scenario.recreate()
            compose.onNodeWithTag("$field-input").assertTextContains("3")
            assertEquals(original, app.settingsRepository.current().numberPrecision)
            compose.onNodeWithTag("$field-save").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("$field-editor").fetchSemanticsNodes().isEmpty() }
            assertEquals(3, app.settingsRepository.current().numberPrecision)
            assertTrue(app.settingsRepository.current().powerMode)
            assertEquals(AreaScope.All.storageKey, app.settingsRepository.current().chosenOpeningAreaScope)
            compose.onNodeWithTag(field).assertTextContains("3")
            captureVisualCatalogSurface("settings.item-builder.saved-value")
            compose.onNodeWithTag(field).performClick()
            compose.onNodeWithTag("$field-input").assertTextContains("3")
            compose.onNodeWithTag("$field-cancel").performClick()
        }
    }
}
