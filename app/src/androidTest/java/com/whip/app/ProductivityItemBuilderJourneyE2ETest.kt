package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.UnitDimension
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductivityItemBuilderJourneyE2ETest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    private val name = "Breathe and settle before the day"

    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun expandedTimerHasOneStatusAndKeepsItsActionsAfterRecreation() {
        val id = runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { AppSettings(setupCompleted = true, themeMode = AppThemeMode.Light, dynamicColor = false) }
            app.habitRepository.create(HabitDraft(
                name = name, icon = "🧘", startDate = app.clock.today(),
                trackingMode = HabitTrackingMode.Duration, dimension = UnitDimension.Duration,
                unitId = "minute", targetMin = 10.0,
            ))
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Habits tab").performClick()
            compose.onNodeWithTag("habit-card-$id").performScrollTo()
            compose.onNodeWithContentDescription("Start timer for $name").performClick()
            compose.waitUntil(10_000) { runBlocking { app.habitRepository.get(id)?.timerStartedAtMillis != null } }
            compose.onNodeWithTag("habit-expand-$id").performClick()
            compose.waitForIdle()
            compose.assertWorkspaceMeasure(720.dp)
            captureVisualCatalogSurface("habits.productivity-builder.timer-expanded")
            assertOneTimerStatus(id)
            val timer = runBlocking { requireNotNull(app.habitRepository.get(id)) }
            scenario.recreate()
            assertOneTimerStatus(id)
            assertEquals(timer.timerSessionId, runBlocking { app.habitRepository.get(id)?.timerSessionId })
            compose.onNodeWithContentDescription("Edit habit $name").performScrollTo().performClick()
            compose.onNodeWithTag("habit-editor-surface").assertIsDisplayed()
            compose.onNodeWithContentDescription("Cancel Habit editing").performClick()
            compose.onNode(hasContentDescription("Stop and log $name;", substring = true)).performScrollTo().performClick()
            compose.waitUntil(10_000) { runBlocking { app.habitRepository.get(id)?.timerStartedAtMillis == null } }
            val stopped = runBlocking { requireNotNull(app.habitRepository.get(id)) }
            assertNull(stopped.timerSessionId)
            assertEquals(timer.name, stopped.name)
            assertEquals(timer.uuid, stopped.uuid)
            assertEquals(timer.measurementId, stopped.measurementId)
            assertEquals(timer.unitId, stopped.unitId)
            compose.onNodeWithContentDescription("Start timer for $name").assertIsDisplayed()
            captureVisualCatalogSurface("habits.productivity-builder.timer-stopped")
        }
    }

    private fun assertOneTimerStatus(id: Long) {
        compose.onAllNodes(
            hasText("Timer running", substring = true) and hasAnyAncestor(hasTestTag("habit-card-$id")),
            useUnmergedTree = true,
        ).assertCountEquals(1)
    }
}
