package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.core.HomeSection
import com.whip.app.core.SharedPreferencesSettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirstRunJourneyTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()

    @Before
    fun seed() = runBlocking {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { AppSettings(themeMode = AppThemeMode.Dark, dynamicColor = false) }
    }

    @After
    fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test
    fun customizationRetainsItsDraftThroughRecreationAndStartsWithTheChosenHome() {
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            waitForText("Welcome to Whip")
            captureVisualCatalogSurface("shared.first-run.welcome-real")
            compose.onNodeWithText("Customize").performClick()
            waitForText("Customize Whip")
            compose.waitForIdle()
            captureVisualCatalogSurface("shared.first-run.customize", visuallyDistinctFrom = "shared.first-run.welcome-real")
            setupChoice("Tasks").performClick()
            setupChoice("Habits").performClick()
            setupChoice("Tracks").performClick()
            setupChoice("Gym").performClick()
            compose.onNodeWithContentDescription("Show advanced controls by default").performClick()
            compose.onNodeWithText("lb").performScrollTo().performClick()
            compose.onNodeWithText("Optional Preferences").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Use low-pressure presentation").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Ask for reminder notifications").performScrollTo()
            compose.waitForIdle()
            captureVisualCatalogSurface("shared.first-run.optional", visuallyDistinctFrom = "shared.first-run.customize")

            scenario.recreate()
            waitForText("Customize Whip")
            setupChoice("Tracks").performScrollTo().assertIsSelected()
            setupChoice("Gym").assertIsSelected()
            compose.onNodeWithText("lb").performScrollTo().assertIsSelected()
            compose.onNodeWithText("Save and Start").performClick()
            compose.waitUntil(10_000) { app.settingsRepository.current().setupCompleted }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Customize Whip").fetchSemanticsNodes().isEmpty() }
            captureVisualCatalogSurface("shared.first-run.configured-home")

            scenario.recreate()
            compose.waitForIdle()
            val stored = SharedPreferencesSettingsRepository(app).current()
            assertTrue(stored.setupCompleted)
            assertEquals(HomeSection.entries.toSet() - setOf(HomeSection.Tracks, HomeSection.Gym), stored.hiddenHomeSections)
            assertTrue(stored.powerMode)
            assertTrue(stored.lowPressureMode)
            assertEquals("pound", stored.massUnitId)
            assertEquals("pound", stored.gymWeightUnitId)
            assertFalse(stored.notificationPermissionRequested)
        }
    }

    @Test
    fun recommendedSetupPersistsTheTwoCoreHomeSectionsWithoutAskingForPermissions() {
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            waitForText("Welcome to Whip")
            compose.onNodeWithText("Use Recommended").performClick()
            compose.waitUntil(10_000) { app.settingsRepository.current().setupCompleted }
            scenario.recreate()
            compose.waitForIdle()
            val stored = SharedPreferencesSettingsRepository(app).current()
            assertTrue(stored.setupCompleted)
            assertEquals(HomeSection.entries.toSet() - setOf(HomeSection.Tasks, HomeSection.Habits), stored.hiddenHomeSections)
            assertFalse(stored.powerMode)
            assertFalse(stored.lowPressureMode)
            assertEquals("kilogram", stored.massUnitId)
            assertEquals("kilogram", stored.gymWeightUnitId)
            assertFalse(stored.notificationPermissionRequested)
        }
    }

    private fun waitForText(text: String) = compose.waitUntil(10_000) {
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    private fun setupChoice(text: String) = compose.onNode(hasText(text) and hasAnyAncestor(isDialog()))
}
