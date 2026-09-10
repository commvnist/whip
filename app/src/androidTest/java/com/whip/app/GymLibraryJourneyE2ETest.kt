package com.whip.app

import android.content.Intent
import androidx.compose.ui.semantics.SemanticsActions
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
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class GymLibraryJourneyE2ETest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun equipmentSearchAndFilterIncludeEveryLinkedExercise() {
        runBlocking { prepare(); seed() }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            open("Exercises")
            capture("exercise-list")
            compose.onNodeWithTag("exercise-library-search").performTextReplacement(machineName)
            closeSoftKeyboard()
            capture("equipment-search")
            compose.onNodeWithContentDescription("Edit exercise $firstExercise").performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription("Edit exercise $secondExercise").performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription("Edit exercise Unlinked walk").assertDoesNotExist()
            scenario.recreate()
            compose.onNodeWithTag("exercise-library-search").assertTextContains(machineName)
            compose.onNodeWithContentDescription("Edit exercise $secondExercise").performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription("Clear Search").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Clear Search").assertDoesNotExist()
            compose.onNodeWithText("Filters and Sort").performScrollTo().performClick()
            compose.onNodeWithText("All Equipment").performScrollTo().performClick()
            compose.onNodeWithText("Any Machine").performClick()
            compose.onNodeWithText("Filters and Sort").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Edit exercise $firstExercise").performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription("Edit exercise $secondExercise").performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription("Edit exercise Unlinked walk").assertDoesNotExist()
            capture("equipment-filter")
        }
    }

    @Test fun exerciseAndCategoryEditsReorderAndArchivePreserveLinksAndHistory() {
        val fixture = runBlocking { prepare(); seed() }
        val editedExercise = "Supported single arm cable row with a controlled pause"
        val editedCategory = "Upper body pulling and scapular control"
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            open("Exercises")
            compose.waitUntil(10_000) { history().first.none { it.restTimerCleanupPending } }
            val originalHistory = history()
            compose.onNodeWithContentDescription("Edit exercise $firstExercise").performScrollTo().performClick()
            compose.onNodeWithTag("exercise-editor-name").performTextReplacement(editedExercise)
            scenario.recreate()
            compose.onNodeWithTag("exercise-editor-name").assertTextContains(editedExercise)
            capture("exercise-draft")
            compose.onNodeWithText("Save", substring = false).performClick()
            awaitClosed("exercise-editor-name")
            compose.waitUntil(10_000) { exercises().single { it.id == fixture.exercise }.name == editedExercise }
            compose.onNodeWithContentDescription("Reorder Exercises").performScrollTo().performClick()
            moveUp(secondExercise)
            compose.waitUntil(10_000) { exercises().first().id == fixture.secondExercise }
            capture("exercise-reorder")
            compose.onNodeWithTag("reorder-mode-done").performClick()
            scenario.recreate()
            compose.onNodeWithContentDescription("Edit exercise $editedExercise").performScrollTo().performClick()
            compose.onNodeWithTag("exercise-editor-name").assertTextContains(editedExercise)
            compose.onNodeWithText("Save", substring = false).performClick()
            awaitClosed("exercise-editor-name")
            open("Categories")
            capture("category-list")
            compose.onNodeWithContentDescription("Edit category $categoryName").performScrollTo().performClick()
            compose.onNodeWithTag("gym-category-name").performTextReplacement(editedCategory)
            compose.onNodeWithTag("gym-category-type").performTextReplacement("Movement family and training emphasis")
            scenario.recreate()
            compose.onNodeWithTag("gym-category-name").assertTextContains(editedCategory)
            capture("category-draft")
            compose.onNodeWithText("Save", substring = false).performClick()
            awaitClosed("gym-category-editor")
            compose.waitUntil(10_000) { categories().single { it.id == fixture.category }.name == editedCategory }
            compose.onNodeWithContentDescription("Reorder Categories").performScrollTo().performClick()
            moveUp("Conditioning")
            compose.waitUntil(10_000) { categories().first().name == "Conditioning" }
            capture("category-reorder")
            compose.onNodeWithTag("reorder-mode-done").performClick()
            compose.onNodeWithContentDescription("Archive category $editedCategory").performScrollTo().performClick()
            compose.waitUntil(10_000) { categories().single { it.id == fixture.category }.archived }
            compose.onNodeWithText("Show Archived", ignoreCase = true).performScrollTo().performClick()
            scenario.recreate()
            compose.onNodeWithContentDescription("Restore category $editedCategory").performScrollTo().assertIsDisplayed()
            capture("category-archived")
            compose.onNodeWithContentDescription("Restore category $editedCategory").performClick()
            compose.waitUntil(10_000) { !categories().single { it.id == fixture.category }.archived }
            compose.onNodeWithText("Show Archived", ignoreCase = true).performScrollTo().performClick()
            compose.onNodeWithContentDescription("Edit category $editedCategory").performScrollTo().performClick()
            compose.onNodeWithTag("gym-category-type").assertTextContains("Movement family and training emphasis")
            assertEquals(fixture.links.toSet(), runBlocking { app.gymRepository.categoryLinks.first().toSet() })
            assertEquals(originalHistory, history())
        }
    }

    @Test fun machineEditArchiveAndNewVersionKeepCompletedWorkoutSnapshots() {
        val fixture = runBlocking { prepare(); seed() }
        val editedName = "Adjustable dual cable station with independent resistance stacks"
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            open("Machines")
            compose.waitUntil(10_000) { history().first.none { it.restTimerCleanupPending } }
            val originalHistory = history()
            capture("machine-list")
            compose.onNodeWithContentDescription("Edit machine $machineName").performScrollTo().performClick()
            compose.onNodeWithTag("machine-editor-name").performTextReplacement(editedName)
            scenario.recreate()
            compose.onNodeWithTag("machine-editor-name").assertTextContains(editedName)
            capture("machine-draft")
            compose.onNodeWithText("Save", substring = false).performClick()
            awaitClosed("machine-editor-name")
            compose.waitUntil(10_000) { machines().single { it.id == fixture.machine }.name == editedName }
            machineMenu(editedName)
            compose.onNodeWithText("Archive", substring = false).performClick()
            compose.waitUntil(10_000) { machines().single { it.id == fixture.machine }.archived }
            compose.onNodeWithText("Show Archived", ignoreCase = true).performScrollTo().performClick()
            scenario.recreate()
            machineMenu(editedName)
            compose.onNodeWithText("Restore", substring = false).assertIsDisplayed()
            capture("machine-archived")
            compose.onNodeWithText("Restore", substring = false).performClick()
            compose.waitUntil(10_000) { !machines().single { it.id == fixture.machine }.archived }
            compose.onNodeWithText("Show Archived", ignoreCase = true).performScrollTo().performClick()
            machineMenu(editedName)
            compose.onNodeWithText("New Configuration Version").performClick()
            compose.onNodeWithText("New Machine Configuration Version").assertIsDisplayed()
            compose.onNodeWithTag("machine-editor-name").performTextReplacement("Cable station with revised resistance settings")
            scenario.recreate()
            capture("machine-version-draft")
            compose.onNodeWithText("Save", substring = false).performClick()
            awaitClosed("machine-editor-name")
            compose.waitUntil(10_000) { machines().size == 2 }
            val previous = machines().single { it.id == fixture.machine }
            val current = machines().single { it.id != fixture.machine }
            assertEquals(1, previous.configurationVersion)
            assertEquals(2, current.configurationVersion)
            assertEquals(previous.configurationGroupId, current.configurationGroupId)
            assertEquals(setOf(fixture.exercise, fixture.secondExercise), current.exerciseIds)
            assertEquals(originalHistory, history())
            compose.onNodeWithContentDescription("Edit machine ${current.name}").performScrollTo().assertIsDisplayed()
            capture("machine-versions")
            compose.onNodeWithContentDescription("Edit machine ${current.name}").performScrollTo().performClick()
            compose.onNodeWithTag("machine-editor-name").assertTextContains(current.name)
            assertEquals(originalHistory, history())
        }
    }

    private fun machineMenu(name: String) {
        compose.onNodeWithContentDescription("More Actions for $name").performScrollTo().performClick()
        compose.onNodeWithText("Delete Permanently").assertIsDisplayed()
    }
    private fun moveUp(name: String) {
        val node = compose.onNodeWithContentDescription("Reorder $name").performScrollTo().fetchSemanticsNode()
        val move = node.config[SemanticsActions.CustomActions].single { it.label.endsWith(" up") }
        compose.runOnIdle { assertTrue(move.action()) }
    }
    private fun open(label: String) {
        compose.onNodeWithContentDescription("Gym tab").performClick()
        compose.onNodeWithTag("gym-destination-Library").performClick()
        compose.onNodeWithTag("gym-library-$label").performClick()
        compose.waitForIdle()
    }
    private fun awaitClosed(tag: String) = compose.waitUntil(10_000) {
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()
    }
    private fun capture(state: String) {
        closeSoftKeyboard()
        compose.waitForIdle()
        captureVisualCatalogSurface("gym.library-builder.$state")
    }
    private suspend fun prepare() {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark, dynamicColor = false) }
    }
    private suspend fun seed(): Fixture {
        val repo = app.gymRepository
        val category = repo.createCategory(categoryName, "Movement family and training emphasis")
        repo.createCategory("Conditioning", "Training focus")
        val exercise = repo.createExercise(ExerciseDraft(name = firstExercise, categoryIds = setOf(category)))
        val second = repo.createExercise(ExerciseDraft(name = secondExercise, categoryIds = setOf(category)))
        repo.createExercise(ExerciseDraft(name = "Unlinked walk", trackingType = ExerciseTrackingType.DurationOnly))
        val machine = repo.createMachine(GymMachineDraft(name = machineName, exerciseIds = setOf(exercise, second),
            availableLoads = listOf(5.0, 10.0, 15.0, 20.0), seatPosition = "4", backPosition = "2",
            attachment = "Independent handles with adjustable cable height", pulleyRatio = 0.5))
        val session = repo.startWorkout("Library continuity")
        val placement = repo.addExerciseToWorkout(session, exercise, machine)
        repo.addSet(placement, WorkoutSetDraft(weight = 15.0, reps = 8, completed = true))
        repo.finishWorkout(session)
        return Fixture(exercise, second, category, machine, repo.categoryLinks.first())
    }
    private fun exercises() = runBlocking { app.gymRepository.exercises.first().sortedBy { it.position } }
    private fun categories() = runBlocking { app.gymRepository.categories.first().sortedBy { it.position } }
    private fun machines() = runBlocking { app.gymRepository.machines.first() }
    private fun history() = runBlocking {
        Triple(app.gymRepository.sessions.first(), app.gymRepository.workoutExercises.first(), app.gymRepository.sets.first())
    }
    private data class Fixture(val exercise: Long, val secondExercise: Long, val category: Long, val machine: Long,
        val links: List<ExerciseCategoryLink>)
    private val firstExercise = "Single arm cable row with a controlled pause and full reach"
    private val secondExercise = "Standing cable press with alternating arms and a stable stance"
    private val machineName = "Dual cable station with adjustable pulleys and independent handles"
    private val categoryName = "Upper body strength and controlled shoulder movement"
}
