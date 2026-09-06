package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskDraft
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VisualCatalogSharedShellTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app: WhipApplication
        get() = ApplicationProvider.getApplicationContext()
    private var planTaskId = 0L

    @Before
    fun seed() = runBlocking {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update {
            AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark, dynamicColor = false)
        }
        val today = app.clock.today()
        planTaskId = app.taskRepository.create(
            TaskDraft("Plan the week", scheduleKind = ScheduleKind.Once, date = today),
        )
        val completed = app.taskRepository.create(
            TaskDraft("Send design notes", scheduleKind = ScheduleKind.Once, date = today),
        )
        app.taskRepository.completeOccurrence(completed, today)
    }

    @After
    fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test
    fun captureSharedShellCatalog() {
        launchMainActivity(
            Intent(app, MainActivity::class.java)
                .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true),
        ).use {
            compose.waitUntil(15_000L) {
                compose.onAllNodesWithText("Plan the week").fetchSemanticsNodes().isNotEmpty()
            }

            compose.onNodeWithTag("workspace-add-action").performClick()
            compose.onNodeWithText("New Task").assertExists()
            captureVisualCatalogSurface("shared.global-add.menu")
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
            compose.waitForIdle()

            compose.onNodeWithTag("workspace-search-action").performClick()
            compose.onNodeWithTag("unified-search-query").performTextReplacement("Plan")
            compose.waitUntil(10_000L) {
                compose.onAllNodesWithTag("unified-search-result-Task-$planTaskId")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("unified-search-result-Task-$planTaskId").assertExists()
            compose.waitForIdle()
            captureVisualCatalogSurface("shared.search.results")

            compose.onNodeWithTag("unified-search-query").performTextReplacement("No such Whip result")
            compose.waitUntil(10_000L) {
                compose.onAllNodesWithText("No matching items").fetchSemanticsNodes().isNotEmpty()
            }
            compose.waitForIdle()
            captureVisualCatalogSurface("shared.search.empty")
            compose.onNodeWithContentDescription("Close Search").performClick()

            compose.waitUntil(10_000L) {
                compose.onAllNodesWithText("Review & Trends").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Review & Trends").performClick()
            compose.waitForIdle()
            captureVisualCatalogSurface("shared.review.summary")
        }
    }
}
