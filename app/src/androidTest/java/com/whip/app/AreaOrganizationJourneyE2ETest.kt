package com.whip.app

import android.content.Intent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
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

class AreaOrganizationJourneyE2ETest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun createColorOrderAndArchivedIdentitySurviveRecreation() {
        runBlocking { prepare() }
        val name = "Creative practice"
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            openAreas()
            openArea("Main")
            compose.onNodeWithText("Delete Area Permanently").performScrollTo().performClick()
            compose.onNodeWithText("Create Another Area First").assertIsDisplayed()
            capture("last-required")
            compose.onNodeWithText("Create Area", substring = false).performClick()
            compose.onNodeWithText("Area name").performTextReplacement(name)
            compose.onNodeWithTag("color-picker-field").performClick()
            compose.onNodeWithTag("color-preset-blue").performClick()
            compose.onNodeWithTag("color-picker-apply").performClick()
            scenario.recreate()
            compose.onNodeWithText("Area name").assertTextContains(name)
            capture("create-draft")
            compose.onNodeWithText("Create", substring = false).performClick()
            val created = awaitArea(name)
            assertNotNull(created.colorArgb)
            compose.onNodeWithTag("area-back-action").performClick()
            compose.onNodeWithContentDescription("Reorder Areas").performClick()
            val move = compose.onNodeWithContentDescription("Reorder $name Area").fetchSemanticsNode()
                .config[SemanticsActions.CustomActions].single { it.label.endsWith(" up") }
            compose.runOnIdle { assertTrue(move.action()) }
            compose.waitUntil(10_000) { areas().filterNot { it.archived }.first().id == created.id }
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Saving Area Change").fetchSemanticsNodes().isEmpty() &&
                    compose.onNodeWithContentDescription("Reorder $name Area").fetchSemanticsNode()
                        .config[SemanticsProperties.StateDescription].contains("Position 1 of 2")
            }
            capture("reordered")
            compose.onNodeWithTag("reorder-mode-done").performClick()
            scenario.recreate()
            assertEquals(created.id, areas().filterNot { it.archived }.first().id)
            openArea(name)
            compose.onNodeWithText("Choose Color").performScrollTo().performClick()
            compose.onNodeWithTag("color-preset-violet").performClick()
            scenario.recreate()
            compose.onNodeWithTag("color-preset-violet").assertIsSelected()
            capture("color-draft")
            compose.onNodeWithTag("color-picker-apply").performClick()
            compose.waitUntil(10_000) { areas().single { it.id == created.id }.colorArgb != created.colorArgb }
            val savedColor = areas().single { it.id == created.id }.colorArgb
            compose.onNodeWithText("Archive Area").performScrollTo().performClick()
            compose.onNodeWithText("Archive $name?").assertIsDisplayed()
            capture("archive-review")
            compose.onNodeWithText("Archive", substring = false).performClick()
            compose.waitUntil(10_000) { areas().single { it.id == created.id }.archived }
            compose.onNodeWithText("Undo", substring = false).performClick()
            compose.waitUntil(10_000) { !areas().single { it.id == created.id }.archived }
            compose.onNodeWithText("Archive Area").performScrollTo().performClick()
            compose.onNodeWithText("Archive", substring = false).performClick()
            compose.waitUntil(10_000) { areas().single { it.id == created.id }.archived }
            scenario.recreate()
            compose.onNodeWithText("Restore Area").performScrollTo().assertIsDisplayed()
            capture("archived-detail")
            compose.onNodeWithTag("area-back-action").performClick()
            compose.onNodeWithText("Archived · 1").performScrollTo().performClick()
            compose.onNodeWithText(name).performScrollTo().assertIsDisplayed()
            capture("archived-list")
            compose.onNodeWithText("Create Area", substring = false).performClick()
            compose.onNodeWithTag("color-picker-field").performClick()
            compose.onNodeWithTag("color-preset-blue").performClick()
            compose.onNodeWithTag("color-picker-apply").performClick()
            compose.onNodeWithText("Area name").performTextReplacement(name)
            compose.onNodeWithText("its saved color will be kept", substring = true).assertIsDisplayed()
            compose.onNodeWithContentDescription("Saved Color: Violet. Choose color.").assertIsNotEnabled()
            compose.onNodeWithText("Area name").performTextReplacement("A different project")
            compose.onNodeWithContentDescription("Color: Blue. Choose color.").assertIsEnabled()
            compose.onNodeWithText("Area name").performTextReplacement(name)
            scenario.recreate()
            compose.onNodeWithContentDescription("Saved Color: Violet. Choose color.").assertIsNotEnabled()
            capture("restore-existing")
            compose.onNodeWithText("Restore Existing Area").performClick()
            compose.waitUntil(10_000) { !areas().single { it.id == created.id }.archived }
            assertEquals(savedColor, areas().single { it.id == created.id }.colorArgb)
            assertEquals(2, areas().size)
            compose.onNodeWithTag("area-close-action").performClick()
            compose.onNodeWithText("Manage Areas").performClick()
            openArea(name)
            compose.onNodeWithText("Archive Area").performScrollTo().assertIsDisplayed()
            capture("restored-detail")
        }
    }

    @Test fun moveAndMergeKeepSavedHistoryAndSearchRecoverable() {
        val fixture = runBlocking {
            prepare()
            val f = seed("Client Delta")
            val targetId = app.areaRepository.create("Personal projects")
            repeat(6) { app.areaRepository.create("Reference ${it + 1}") }
            app.settingsRepository.update { it.copy(activeAreaScope = AreaScope.One(f.areaId).storageKey,
                chosenOpeningAreaScope = AreaScope.One(targetId).storageKey) }
            f.copy(targetId = targetId)
        }
        val saved = history(fixture)
        val mainId = areas().single { it.name == "Main" }.id
        assertEquals(9, areas().size)
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            openAreas()
            compose.onNodeWithText("Find Area").performTextReplacement("Client Delta")
            openArea("Client Delta")
            compose.onNodeWithText("Move 4 Items").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Move to Personal projects").performScrollTo().performClick()
            scenario.recreate()
            compose.onNodeWithContentDescription("Move to Personal projects").assertIsSelected()
            capture("move-review")
            compose.onNode(hasText("Move 4 Items") and hasAnyAncestor(isDialog())).performClick()
            awaitAssignment(fixture, fixture.targetId)
            assertEquals(saved, history(fixture))
            assertTrue(areas().any { it.id == fixture.areaId })
            compose.onNodeWithText("No Items to Move").assertIsNotEnabled()
            capture("moved-source")
            compose.onNodeWithTag("area-back-action").performClick()
            compose.onNodeWithText("Find Area").performTextReplacement("Personal projects")
            openArea("Personal projects")
            compose.onNodeWithText("Merge into Another Area").performScrollTo().performClick()
            compose.onNodeWithText("Merge Personal projects").assertIsDisplayed()
            compose.waitForIdle()
            compose.onNodeWithContentDescription("Area selection: Choose Area").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithContentDescription("Area Main").fetchSemanticsNodes().size == 1
            }
            compose.onNodeWithContentDescription("Area Main").performClick()
            scenario.recreate()
            compose.onNodeWithContentDescription("Area selection: Main").assertIsDisplayed()
            capture("merge-review")
            compose.onNodeWithText("Merge into Main", substring = false).performClick()
            compose.waitUntil(10_000) { areas().none { it.id == fixture.targetId } }
            awaitAssignment(fixture, mainId)
            assertEquals(saved, history(fixture))
            assertEquals(AreaScope.One(mainId).storageKey, runBlocking { app.settingsRepository.current().activeAreaScope })
            assertEquals(AreaScope.One(mainId).storageKey, runBlocking { app.settingsRepository.current().chosenOpeningAreaScope })
            compose.onNodeWithTag("area-back-action").performClick()
            compose.waitForIdle()
            capture("search-after-merge")
            compose.onNodeWithText("Find Area").assertIsDisplayed().assertTextContains("Personal projects")
            scenario.recreate()
            compose.onNodeWithText("Find Area").assertTextContains("Personal projects")
            compose.onNodeWithContentDescription("Clear Search").performClick()
            compose.onNodeWithContentDescription("Open area details for Main").performScrollTo().assertIsDisplayed()
            capture("search-recovered")
            compose.onNodeWithTag("area-close-action").performClick()
            compose.onNodeWithText("Manage Areas").performClick()
            openArea("Main")
            compose.onNodeWithText("Move 4 Items").assertIsDisplayed()
            assertEquals(saved, history(fixture))
        }
    }

    @Test fun reviewedCleanupPreservesOrDeletesOnlyTheAssignedHistory() {
        val fixture = runBlocking {
            prepare()
            val f = seed("Past projects")
            val destination = app.areaRepository.create("Retained work")
            app.taskRepository.create(TaskDraft(title = "Keep my unrelated task"))
            app.settingsRepository.update { it.copy(activeAreaScope = AreaScope.One(f.areaId).storageKey,
                chosenOpeningAreaScope = AreaScope.One(f.areaId).storageKey) }
            f.copy(targetId = destination)
        }
        val saved = history(fixture)
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            openAreas()
            openArea("Past projects")
            openDelete()
            compose.onNodeWithText("Move Items and Delete Area").assertIsNotEnabled()
            capture("delete-choice")
            compose.onNodeWithText("Cancel", substring = false).performClick()
            assertEquals(saved, history(fixture))
            assertTrue(areas().any { it.id == fixture.areaId })
            openDelete()
            compose.onNodeWithContentDescription("Move items to Retained work").performScrollTo().performClick()
            scenario.recreate()
            compose.onNodeWithContentDescription("Move items to Retained work").assertIsSelected()
            capture("delete-keep-review")
            compose.onNodeWithText("Move Items and Delete Area").performClick()
            compose.waitUntil(10_000) { areas().none { it.id == fixture.areaId } }
            awaitAssignment(fixture, fixture.targetId)
            assertEquals(saved, history(fixture))
            assertEquals("all", runBlocking { app.settingsRepository.current().activeAreaScope })
            assertEquals("all", runBlocking { app.settingsRepository.current().chosenOpeningAreaScope })
            openArea("Retained work")
            compose.onNodeWithText("Move 4 Items").assertIsDisplayed()
            capture("delete-kept-history")
            openDelete()
            scenario.recreate()
            compose.onNodeWithText("Delete Area and 4 Items").assertIsDisplayed()
            capture("delete-all-review")
            compose.onNodeWithText("Delete Area and 4 Items").performClick()
            compose.waitUntil(10_000) { areas().none { it.id == fixture.targetId } }
            runBlocking {
                assertTrue(app.taskRepository.tasks.first().none { it.id == fixture.taskId })
                assertTrue(app.habitRepository.habits.first().none { it.id == fixture.habitId })
                assertTrue(app.goalRepository.goals.first().none { it.id == fixture.goalId })
                assertTrue(app.trackRepository.tracks.first().none { it.id == fixture.trackId })
                assertTrue(app.habitRepository.logs.first().isEmpty())
                assertTrue(app.measurementRepository.entries.first().isEmpty())
                assertTrue(app.trackRepository.entries.first().isEmpty())
                assertTrue(app.trackRepository.values.first().isEmpty())
                assertEquals("Keep my unrelated task", app.taskRepository.tasks.first().single().title)
            }
            scenario.recreate()
            compose.onNodeWithContentDescription("Open area details for Main").assertIsDisplayed()
            capture("cleanup-complete")
        }
    }

    private fun openAreas() {
        compose.onNodeWithContentDescription("Open Settings").performClick()
        compose.openSettingsCategory("Organization")
        compose.onNodeWithText("Manage Areas").performScrollTo().performClick()
    }
    private fun openArea(name: String) {
        compose.onNodeWithContentDescription("Open area details for $name").performScrollTo().performClick()
    }
    private fun openDelete() = compose.onNodeWithText("Delete Area Permanently").performScrollTo().performClick()
    private fun capture(name: String) {
        compose.waitForIdle()
        captureVisualCatalogSurface("organization.lifecycle.$name")
    }
    private fun areas() = runBlocking { app.areaRepository.areas.first() }
    private fun awaitArea(name: String): Area {
        compose.waitUntil(10_000) { areas().any { it.name == name && !it.archived } }
        compose.waitForIdle()
        return areas().single { it.name == name }
    }
    private suspend fun prepare() {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark, dynamicColor = false) }
    }
    private suspend fun seed(name: String): Fixture {
        val areaId = app.areaRepository.create(name)
        val taskId = app.taskRepository.create(TaskDraft(title = "Finish the project notes", areaId = areaId))
        app.taskRepository.completeOccurrence(taskId, null)
        val habitId = app.habitRepository.create(HabitDraft(name = "Read daily", areaId = areaId, startDate = app.clock.today()))
        app.habitRepository.log(habitId, 1.0, date = app.clock.today(), note = "Preserved practice")
        val goalId = app.goalRepository.create(GoalDraft(name = "Build momentum", areaId = areaId,
            type = GoalType.ReachValue, targetMin = 10.0, startDate = app.clock.today()))
        app.goalRepository.recordMeasurement(goalId, 0.5, app.clock.today())
        val trackId = app.trackRepository.create(TrackDraft(name = "Project notes", areaId = areaId,
            fields = listOf(TrackFieldDraft("Note", TrackFieldType.ShortText, primary = true))))
        val preparation = requireNotNull(app.trackRepository.prepareEntryCreate(trackId))
        val field = requireNotNull(app.trackRepository.projection(trackId)).primaryField
        app.trackRepository.addEntry(preparation.request,
            TrackEntryDraft(app.clock.today(), mapOf(field.uuid to TrackValueDraft(textValue = "Keep this exact evidence"))))
        return Fixture(areaId, taskId, habitId, goalId, trackId)
    }
    private fun history(f: Fixture): List<Any?> = runBlocking {
        listOf(app.taskRepository.tasks.first().single { it.id == f.taskId }.completedAtMillis,
            app.habitRepository.logs.first(), app.measurementRepository.entries.first(),
            app.trackRepository.entries.first(), app.trackRepository.values.first())
    }
    private fun awaitAssignment(f: Fixture, areaId: String) {
        compose.waitUntil(10_000) { runBlocking {
            app.taskRepository.tasks.first().single { it.id == f.taskId }.areaId == areaId &&
                app.habitRepository.habits.first().single { it.id == f.habitId }.areaId == areaId &&
                app.goalRepository.goals.first().single { it.id == f.goalId }.areaId == areaId &&
                app.trackRepository.tracks.first().single { it.id == f.trackId }.areaId == areaId
        } }
        compose.waitForIdle()
    }
    private data class Fixture(val areaId: String, val taskId: Long, val habitId: Long,
        val goalId: Long, val trackId: Long, val targetId: String = "")
}
