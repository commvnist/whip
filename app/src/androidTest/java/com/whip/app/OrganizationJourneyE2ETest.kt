package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.*
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

class OrganizationJourneyE2ETest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun tagChangesKeepEveryDomainAndSavedHistoryThroughRecreation() {
        val fixture = runBlocking {
            prepare()
            val areaId = app.areaRepository.areas.first().first { !it.archived }.id
            val tagId = app.measurementRepository.ensureTag("Focus")
            val taskId = app.taskRepository.create(TaskDraft(title = "Read a chapter", tags = setOf("Focus")))
            app.taskRepository.completeOccurrence(taskId, null)
            val habitId = app.habitRepository.create(HabitDraft(name = "Practice daily", tags = listOf("Focus"), startDate = app.clock.today()))
            app.habitRepository.log(habitId, 1.0, date = app.clock.today(), note = "A deliberate start")
            val goalId = app.goalRepository.create(GoalDraft(name = "Build a practice", tags = listOf("Focus"),
                type = GoalType.ReachValue, targetMin = 10.0, startDate = app.clock.today()))
            app.goalRepository.recordMeasurement(goalId, 0.5, app.clock.today())
            val trackId = app.trackRepository.create(TrackDraft(name = "Learning notes", areaId = areaId, tags = listOf("Focus"),
                fields = listOf(TrackFieldDraft("Note", TrackFieldType.ShortText, primary = true))))
            Fixture(tagId, taskId, habitId, goalId, trackId)
        }
        val history = runBlocking { app.habitRepository.logs.first() to app.measurementRepository.entries.first() }
        val completion = runBlocking { app.taskRepository.tasks.first().single { it.id == fixture.taskId }.completedAtMillis }
        assertNotNull(completion)
        val renamed = "Learning and focused practice"
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            openOrganization()
            compose.onNodeWithTag("settings-list").performScrollToNode(hasTestTag("manage-tags-action"))
            compose.onNodeWithTag("manage-tags-action").performClick()
            compose.onNodeWithText("1 Task · 1 Habit · 1 Goal · 1 Track").assertIsDisplayed()
            captureVisualCatalogSurface("organization.journey.tags")
            compose.onNodeWithTag("create-tag-action").performClick()
            compose.onNodeWithTag("create-tag-name").performTextReplacement("Practices")
            compose.onNodeWithTag("create-tag-name").assertTextContains("Practices")
            scenario.recreate()
            compose.onNodeWithTag("create-tag-name").assertTextContains("Practices")
            captureVisualCatalogSurface("organization.journey.tag-create")
            compose.onNodeWithText("Create", substring = false).performClick()
            waitForTag("Practices")
            compose.onNodeWithTag("tag-menu-${fixture.tagId}").performClick()
            compose.onNodeWithText("Rename").performClick()
            compose.onNodeWithTag("rename-tag-name").performTextReplacement(renamed)
            compose.onNodeWithTag("rename-tag-name").assertTextContains(renamed)
            scenario.recreate()
            compose.onNodeWithTag("rename-tag-name").assertTextContains(renamed)
            assertReferences(fixture, "Focus")
            captureVisualCatalogSurface("organization.journey.tag-rename")
            compose.onNodeWithText("Rename Everywhere").performClick()
            waitForTag(renamed)
            assertReferences(fixture, renamed)
            compose.onNodeWithTag("tag-menu-${fixture.tagId}").performClick()
            compose.onNodeWithText("Archive").performClick()
            compose.onNodeWithTag("archive-tag-dialog").assertIsDisplayed()
            captureVisualCatalogSurface("organization.journey.tag-archive")
            compose.onNodeWithText("Archive", substring = false).performClick()
            compose.waitUntil(10_000) { runBlocking { app.measurementRepository.tags.first().any { it.id == fixture.tagId && it.archived } } }
            assertReferences(fixture, renamed)
            compose.onNodeWithTag("tag-search").performTextReplacement(renamed)
            compose.onNodeWithText("Archived Matches · 1").assertIsDisplayed()
            scenario.recreate()
            compose.onNodeWithTag("tag-search").assertTextContains(renamed)
            compose.onNodeWithText("Archived Matches · 1").assertIsDisplayed()
            captureVisualCatalogSurface("organization.journey.tag-archived")
            compose.onNodeWithText("Restore", substring = false).performClick()
            compose.waitUntil(10_000) { runBlocking { app.measurementRepository.tags.first().any { it.id == fixture.tagId && !it.archived } } }
            compose.onNodeWithTag("tag-menu-${fixture.tagId}").performClick()
            compose.onNodeWithText("Merge", substring = false).performClick()
            compose.onNodeWithContentDescription("Merge #$renamed into #Practices").performClick()
            scenario.recreate()
            compose.onNodeWithContentDescription("Merge #$renamed into #Practices").assertIsSelected()
            captureVisualCatalogSurface("organization.journey.tag-merge")
            compose.onNodeWithText("Merge into #Practices").performClick()
            compose.waitUntil(10_000) { runBlocking { app.measurementRepository.tags.first().none { it.id == fixture.tagId } } }
            compose.onNodeWithTag("tag-search").performTextClearance()
            assertReferences(fixture, "Practices")
            compose.onNodeWithText("#Practices").assertIsDisplayed()
            compose.onNodeWithText("1 Task · 1 Habit · 1 Goal · 1 Track").assertIsDisplayed()
            captureVisualCatalogSurface("organization.journey.tag-merged")
            compose.onNodeWithTag("tag-close-action").performClick()
            compose.onNodeWithTag("manage-tags-action").performClick()
            compose.onNodeWithText("#Practices").assertIsDisplayed()
            assertEquals(history, runBlocking { app.habitRepository.logs.first() to app.measurementRepository.entries.first() })
            assertEquals(completion, runBlocking { app.taskRepository.tasks.first().single { it.id == fixture.taskId }.completedAtMillis })
        }
    }

    @Test fun areaIdentityAndRenameDraftSurviveTheRealManagerJourney() {
        val original = "Research and creative projects"
        val renamed = "Research writing and career development"
        val ids = runBlocking {
            prepare()
            val areaId = app.areaRepository.create(original)
            areaId to app.taskRepository.create(TaskDraft(title = "Finish the research outline", areaId = areaId))
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            openOrganization()
            compose.onNodeWithText("Manage Areas").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Open area details for $original").assertIsDisplayed()
            captureVisualCatalogSurface("organization.journey.areas")
            compose.onNodeWithContentDescription("Open area details for $original").performClick()
            assertAreaTitle(original)
            compose.onNodeWithTag("area-back-action").assertIsDisplayed()
            compose.onNodeWithTag("area-close-action").assertIsDisplayed()
            captureVisualCatalogSurface("organization.journey.area-detail")
            compose.onNodeWithText("Rename Area").performScrollTo().performClick()
            compose.onNodeWithTag("rename-area-name").performTextReplacement(renamed)
            compose.onNodeWithTag("rename-area-name").assertTextContains(renamed)
            scenario.recreate()
            compose.onNodeWithTag("rename-area-name").assertTextContains(renamed)
            assertEquals(original, runBlocking { app.areaRepository.areas.first().single { it.id == ids.first }.name })
            compose.onNodeWithText("Rename", substring = false).performClick()
            compose.waitUntil(10_000) { runBlocking { app.areaRepository.areas.first().any { it.id == ids.first && it.name == renamed } } }
            assertAreaTitle(renamed)
            captureVisualCatalogSurface("organization.journey.area-renamed")
            compose.onNodeWithTag("area-back-action").performClick()
            compose.onNodeWithText("Create Area").assertIsDisplayed()
            compose.onNodeWithTag("area-close-action").performClick()
            compose.onNodeWithText("Manage Areas").performClick()
            compose.onNodeWithContentDescription("Open area details for $renamed").assertIsDisplayed()
            val task = runBlocking { app.taskRepository.tasks.first().single { it.id == ids.second } }
            assertEquals(ids.first, task.areaId)
            assertEquals(renamed, task.area)
            assertEquals("Finish the research outline", task.title)
        }
    }

    private fun openOrganization() {
        compose.onNodeWithContentDescription("Open Settings").performClick()
        compose.openSettingsCategory("Organization")
    }

    private fun assertAreaTitle(name: String) {
        compose.onNode(hasText(name) and hasAnyAncestor(hasTestTag("area-destination-title")),
            useUnmergedTree = true).assertIsDisplayed()
    }

    private suspend fun prepare() {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark, dynamicColor = false) }
    }

    private fun waitForTag(name: String) {
        compose.waitUntil(10_000) { runBlocking { app.measurementRepository.tags.first().any { it.name == name && !it.archived } } }
        compose.waitForIdle()
    }

    private fun assertReferences(fixture: Fixture, name: String) = runBlocking {
        assertEquals(setOf(name), app.taskRepository.tasks.first().single { it.id == fixture.taskId }.tags)
        assertEquals(listOf(name), app.habitRepository.habits.first().single { it.id == fixture.habitId }.tags)
        assertEquals(listOf(name), app.goalRepository.goals.first().single { it.id == fixture.goalId }.tags)
        assertEquals(listOf(name), app.trackRepository.tracks.first().single { it.id == fixture.trackId }.tags)
    }

    private data class Fixture(val tagId: String, val taskId: Long, val habitId: Long, val goalId: Long, val trackId: Long)
}
