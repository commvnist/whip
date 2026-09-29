package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskDraft
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class DeepSharedJourneyTest {
    private val compose = createEmptyComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)
    private val app get() = ApplicationProvider.getApplicationContext<WhipApplication>()

    @Before fun prepare() = runBlocking {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { AppSettings(setupCompleted = true, activeAreaScope = "all",
            themeMode = AppThemeMode.Dark, dynamicColor = false) }
    }
    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun denseHomeBalancesDomainsAndCompletingRefillsPreview() = homePreview()
    @Test @AndroidFontScale fun denseHomeContinuationRemainsReachableAtLargeText() = homePreview()

    private fun homePreview() {
        val taskIds = runBlocking {
            val ids = (1..12).map { index ->
                app.taskRepository.create(TaskDraft("Priority work $index", scheduleKind = ScheduleKind.Once,
                    date = app.clock.today(), inbox = false)).also { app.taskRepository.setPinned(it, true) }
            }
            repeat(6) { app.habitRepository.create(HabitDraft("Daily practice ${it + 1}", startDate = app.clock.today())) }
            ids
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use {
            await("task-card-${taskIds.first()}")
            compose.onNodeWithTag("home-list").performScrollToNode(hasTestTag("home-view-all-Tasks"))
            compose.onNodeWithTag("home-view-all-Tasks").assertTextEquals("View All Tasks · 12")
            compose.onNodeWithTag("task-card-${taskIds[3]}").assertDoesNotExist()
            compose.onNodeWithTag("home-list").performScrollToNode(hasTestTag("home-view-all-Habits"))
            compose.onNodeWithTag("home-view-all-Habits").assertTextEquals("View All Habits · 6")
            captureVisualCatalogSurface("deep.home.balanced.${app.resources.configuration.fontScale}")
            compose.onNodeWithTag("home-list").performScrollToNode(hasContentDescription("Complete task Priority work 1"))
            compose.onNodeWithContentDescription("Complete task Priority work 1").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("task-card-${taskIds.first()}").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithTag("home-list").performScrollToNode(hasTestTag("task-card-${taskIds[3]}"))
            compose.onNodeWithTag("task-card-${taskIds[3]}").assertIsDisplayed()
            compose.onNodeWithTag("home-list").performScrollToNode(hasTestTag("home-view-all-Tasks"))
            compose.onNodeWithTag("home-view-all-Tasks").assertTextEquals("View All Tasks · 11").performClick()
            await("task-workspace-list")
            compose.onNodeWithTag("task-workspace-list").performScrollToNode(hasTestTag("task-card-${taskIds.last()}"))
            compose.onNodeWithTag("task-card-${taskIds.last()}").assertIsDisplayed()
        }
    }

    @Test fun workspaceSearchAndSavedFilterChildRetainConsistentChrome() {
        val ids = runBlocking {
            (1..8).map { app.taskRepository.create(TaskDraft("Repair item $it", scheduleKind = ScheduleKind.Once,
                date = app.clock.today(), inbox = false)) }
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            await("home-list")
            compose.onNodeWithTag("primary-navigation-Tasks").performClick()
            await("task-workspace-list")
            val headerBounds = compose.onNodeWithTag("workspace-top-app-bar").fetchSemanticsNode().boundsInRoot
            val searchBounds = compose.onNodeWithTag("workspace-search-action").fetchSemanticsNode().boundsInRoot
            val contextBounds = compose.onNodeWithTag("workspace-context-row").fetchSemanticsNode().boundsInRoot
            for (destination in listOf("Habits", "Goals", "Tracks", "Gym", "Tasks")) {
                compose.onNodeWithTag("primary-navigation-$destination").performClick()
                compose.onAllNodesWithTag("workspace-search-action").assertCountEquals(1)
                org.junit.Assert.assertEquals(headerBounds, compose.onNodeWithTag("workspace-top-app-bar").fetchSemanticsNode().boundsInRoot)
                org.junit.Assert.assertEquals(searchBounds, compose.onNodeWithTag("workspace-search-action").fetchSemanticsNode().boundsInRoot)
                org.junit.Assert.assertEquals("Context row shifted in $destination", contextBounds, compose.onNodeWithTag("workspace-context-row").fetchSemanticsNode().boundsInRoot)
            }
            compose.onNodeWithTag("workspace-settings-action").performClick()
            compose.onAllNodesWithContentDescription("Search Settings").assertCountEquals(1)
            org.junit.Assert.assertEquals(searchBounds, compose.onNodeWithTag("workspace-search-action").fetchSemanticsNode().boundsInRoot)
            compose.onNodeWithTag("workspace-search-action").performClick()
            await("settings-search-query")
            compose.onNodeWithText("Close", substring = false).performClick()
            compose.onNodeWithTag("workspace-settings-action").performClick()
            compose.onNodeWithTag("task-list-query").assertDoesNotExist()
            compose.onNodeWithContentDescription("Find Tasks").assertDoesNotExist()
            compose.onNodeWithTag("workspace-search-action").performClick()
            await("unified-search-query")
            compose.onNodeWithTag("unified-search-query").performTextReplacement("item 8")
            closeSoftKeyboard()
            await("unified-search-result-Task-${ids.last()}")
            compose.onNodeWithTag("unified-search-result-Task-${ids.last()}").performScrollTo().performClick()
            compose.onNodeWithTag("entity-inspector-close").performClick()
            scenario.recreate()
            compose.onNodeWithTag("task-list-query").assertDoesNotExist()
            compose.onNodeWithContentDescription("Filter & Sort Tasks").performClick()
            compose.onNodeWithTag("task-filter-query").performScrollTo().performTextReplacement("item 8")
            closeSoftKeyboard()
            compose.onNodeWithText("Any Date").assertDoesNotExist()
            compose.onNodeWithText("No Scheduled Date").assertDoesNotExist()
            compose.onNodeWithText("Cancel").performClick()
            compose.onNodeWithText("Query: item 8").assertDoesNotExist()
            compose.onNodeWithContentDescription("Filter & Sort Tasks").performClick()
            compose.onNodeWithTag("task-filter-query").performScrollTo().performTextReplacement("item 8")
            closeSoftKeyboard()
            compose.onNodeWithText("Apply").performClick()
            compose.onNodeWithText("Query: item 8").assertIsDisplayed()
            compose.onNodeWithContentDescription("More task list actions").performClick()
            compose.onNodeWithText("Save This View").performClick()
            compose.onNodeWithText("View Name").performTextReplacement("Repair lookup")
            compose.onNodeWithText("Cancel").performClick()
            compose.onNodeWithContentDescription("More task list actions").performClick()
            compose.onNodeWithText("Save This View").performClick()
            compose.onNodeWithText("View Name").assertTextContains("Repair lookup")
            compose.onNodeWithText("Save").performClick()
            compose.onNodeWithTag("task-destination-Tasks").performClick()
            compose.onNodeWithText("Query: item 8").assertDoesNotExist()
            compose.onNodeWithTag("task-destination-Today").performClick()
            compose.onNodeWithText("Query: item 8").assertIsDisplayed()
            org.junit.Assert.assertEquals(contextBounds, compose.onNodeWithTag("workspace-context-row").fetchSemanticsNode().boundsInRoot)
            captureVisualCatalogSurface("search-consistency.tasks.active-filter")
            compose.onNodeWithContentDescription("Filter & Sort Tasks", substring = true).performClick()
            compose.onNodeWithTag("task-filter-query").performScrollTo().performTextClearance()
            closeSoftKeyboard()
            compose.onNodeWithText("Apply").performClick()
            compose.onNodeWithTag("task-card-${ids.first()}").performScrollTo().assertIsDisplayed()
        }
    }

    @Test fun taskInspectorRetainsExecutionContextThroughDefinitionEdit() {
        val id = runBlocking {
            app.taskRepository.create(TaskDraft("Draft the introduction", scheduleKind = ScheduleKind.Once,
                date = app.clock.today(), inbox = false,
                steps = listOf(com.whip.app.domain.TaskStepDraft(title = "Write the opening paragraph", position = 0))))
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            await("task-card-$id")
            compose.onNodeWithTag("task-card-$id").performClick()
            compose.onNodeWithText("Write the opening paragraph").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("focus-preset-30").performScrollTo().assertIsDisplayed()
            captureVisualCatalogSurface("deep.tasks.execution-overview")
            compose.onNodeWithContentDescription("More actions for subtask Write the opening paragraph").performScrollTo().performClick()
            compose.onNodeWithText("Convert to Task").performClick()
            compose.onNodeWithText("Convert Subtask to a Task?").assertIsDisplayed()
            compose.onNodeWithText("Cancel").performClick()
            compose.onNodeWithContentDescription("Edit Task").performClick()
            compose.onNodeWithTag("task-editor-title").performTextReplacement("Draft a clear introduction")
            closeSoftKeyboard()
            scenario.recreate()
            compose.onNodeWithTag("task-editor-title").assertTextContains("Draft a clear introduction")
            compose.onNodeWithText("Save", substring = false).performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("task-editor-surface").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithTag("entity-inspector-title").assertTextEquals("Draft a clear introduction")
            compose.onNodeWithTag("focus-preset-30").performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription("Edit Task").performClick()
            compose.onNodeWithContentDescription("Cancel Task editing").performClick()
            compose.onNodeWithTag("entity-inspector-title").assertTextEquals("Draft a clear introduction")
        }
    }

    private fun await(tag: String) = compose.waitUntil(10_000) {
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
}
