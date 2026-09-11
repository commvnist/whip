package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class LegacyImportJourneyE2ETest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app = ApplicationProvider.getApplicationContext<WhipApplication>()
    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun restoredHabitContinuesManuallyAndSettingsFocusOnLocalData() {
        val id = runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark, dynamicColor = false) }
            app.measurementRepository.ensureMeasurement("health-connect-steps", "Health steps", MeasurementValueKind.Integer, UnitDimension.Count, "count", 0)
            app.measurementRepository.record("health-connect-steps", 6500.0, "count", localDate = app.clock.today().minusDays(1), timestamp = app.clock.now().minusSeconds(86400), sourceType = MeasurementSourceType.HealthConnect, sourceId = "health:steps:old", note = "Saved walking activity")
            val habitId = app.habitRepository.create(HabitDraft(name = "Daily movement", startDate = app.clock.today().minusDays(2), trackingMode = HabitTrackingMode.Count, targetMin = 10000.0))
            app.database.habitDao().updateHabit(app.database.habitDao().getHabit(habitId)!!.copy(sourceMeasurementId = "health-connect-steps"))
            val saved = app.backupRepository.exportBackup()
            app.restoreBackup(saved)
            habitId
        }
        val originalEntries = runBlocking { app.measurementRepository.entries.first() }
        val originalHistory = runBlocking { app.habitRepository.logs.first() }
        assertEquals(1, originalHistory.size)
        assertNull(runBlocking { app.habitRepository.habits.first().single().sourceMeasurementId })
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Open Settings").performClick()
            compose.openSettingsCategory("Data & Privacy")
            compose.onNodeWithText("Backup & Export").assertIsDisplayed()
            compose.onAllNodesWithText("Health Connect", substring = true).assertCountEquals(0)
            captureVisualCatalogSurface("settings.data-privacy.local")
            scenario.recreate()
            compose.onNodeWithText("Backup & Export").assertIsDisplayed()
            compose.onNodeWithContentDescription("Close Settings").performClick()
            compose.onNodeWithContentDescription("Habits tab").performClick()
            compose.onNodeWithTag("habit-card-$id").assertIsDisplayed()
            compose.onNodeWithText("+1").assertIsEnabled().performClick()
            compose.waitUntil(10000) { runBlocking { app.habitRepository.logs.first().size == 2 } }
            compose.waitUntil(10000) { compose.onAllNodesWithText("1/10000").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("1/10000").assertIsDisplayed()
            captureVisualCatalogSurface("habits.legacy-import.manual")
            compose.onNodeWithContentDescription("Open habit details for Daily movement").performSemanticsAction(SemanticsActions.OnClick)
            compose.onNodeWithTag("habit-detail-section-History").performClick()
            compose.onNodeWithText("Imported activity", substring = true).assertIsDisplayed()
            captureVisualCatalogSurface("habits.legacy-import.history")
            scenario.recreate()
            compose.onNodeWithText("Imported activity", substring = true).assertIsDisplayed()
        }
        val after = runBlocking { app.habitRepository.logs.first() }
        assertTrue(after.containsAll(originalHistory))
        assertEquals(1.0, after.single { it.sourceType == MeasurementSourceType.Manual }.value!!, 0.0)
        assertEquals(originalEntries, runBlocking { app.measurementRepository.entries.first().filter { it.sourceType == MeasurementSourceType.HealthConnect } })
    }
}
