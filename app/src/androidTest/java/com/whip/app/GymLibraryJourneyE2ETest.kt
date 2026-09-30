package com.whip.app

import android.content.Intent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.core.PlatePreset
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
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

    @Test fun emptyWorkoutCreateExerciseAddsOneReusablePlacementAndSetAcrossRecreation() {
        runBlocking { prepare() }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithText("Start Empty Workout").performScrollTo().performClick()
            compose.onNodeWithTag("workout-editor-name").performTextReplacement("Empty create acceptance")
            closeSoftKeyboard()
            compose.onNodeWithTag("workout-editor-confirm").performClick()
            compose.waitUntil(10_000) { runBlocking { app.gymRepository.sessions.first() }.size == 1 }
            val session = runBlocking { app.gymRepository.sessions.first().single() }
            assertEquals(WorkoutSessionState.Active, session.state)
            assertTrue(exercises().isEmpty())
            assertTrue(history().second.isEmpty())
            assertTrue(history().third.isEmpty())
            compose.onNodeWithTag("active-workout-list").performScrollToNode(hasText("Create New Exercise"))
            compose.onNodeWithTag("active-workout-empty-state").assertIsDisplayed()
            captureVisualCatalogSurface("overhaul.gym.empty-workout-before-create")
            compose.onNodeWithText("Create New Exercise").performClick()
            compose.onNodeWithTag("exercise-editor-name").performTextInput("Empty workout authored row")
            closeSoftKeyboard()
            compose.onNodeWithText("Save", substring = false).performClick()
            compose.waitUntil(10_000) {
                runBlocking {
                    app.gymRepository.exercises.first().size == 1 &&
                        app.gymRepository.workoutExercises.first().size == 1 &&
                        app.gymRepository.sets.first().size == 1
                }
            }
            val exercise = exercises().single()
            val placement = history().second.single()
            val set = history().third.single()
            assertEquals("Empty workout authored row", exercise.name)
            assertEquals(session.id, placement.sessionId)
            assertEquals(exercise.id, placement.exerciseId)
            assertEquals(placement.id, set.workoutExerciseId)
            assertFalse(set.completed)
            assertEquals(session.id, history().first.single().id)
            compose.onNodeWithTag("quick-set-${set.id}").performScrollTo().assertIsDisplayed()
            captureVisualCatalogSurface("overhaul.gym.empty-workout-after-atomic-create")
            scenario.recreate()
            compose.onNodeWithTag("quick-set-${set.id}").performScrollTo().assertIsDisplayed()
            assertEquals(listOf(exercise.id to exercise.uuid), exercises().map { it.id to it.uuid })
            assertEquals(listOf(placement.id to placement.uuid), history().second.map { it.id to it.uuid })
            assertEquals(listOf(set.id to set.uuid), history().third.map { it.id to it.uuid })
            assertEquals(listOf(session.id to session.uuid), history().first.map { it.id to it.uuid })
            open("Exercises")
            compose.onNodeWithText(exercise.name, substring = false).performScrollTo().assertIsDisplayed()
            captureVisualCatalogSurface("overhaul.gym.empty-workout-reusable-library")
            compose.onNodeWithTag("gym-destination-Workout").performClick()
            compose.onNodeWithTag("quick-set-${set.id}").performScrollTo().assertIsDisplayed()
            assertEquals(session.id, history().first.single().id)
            assertEquals(1, exercises().size)
            assertEquals(1, history().second.size)
            assertEquals(1, history().third.size)
            captureVisualCatalogSurface("overhaul.gym.empty-workout-returned-original-session")
        }
    }

    @Test fun workoutLoggingPrecedesAdministrationAndOptionsRetainActionsAndTotals() {
        val setId = runBlocking {
            prepare()
            val exercise = app.gymRepository.createExercise(ExerciseDraft("Goblet Squat"))
            val session = app.gymRepository.startWorkout("Full Body")
            val placement = app.gymRepository.addExerciseToWorkout(session, exercise)
            app.gymRepository.addSet(placement, WorkoutSetDraft(weight = 24.0, reps = 10))
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use {
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithTag("quick-set-load-$setId").assertIsDisplayed()
            compose.onNodeWithTag("quick-set-reps-$setId").assertIsDisplayed()
            compose.onNodeWithTag("add-exercise-to-active-workout").assertDoesNotExist()
            compose.onNodeWithContentDescription("Workout options").performClick()
            compose.onNodeWithText("Edit Workout", substring = false).assertIsDisplayed()
            compose.onNodeWithText("Add Exercise", substring = false).assertIsDisplayed()
            compose.onNodeWithText("Arrange Workout", substring = false).assertIsDisplayed()
            compose.onNodeWithText("Show Workout Totals").performClick()
            compose.onNodeWithTag("active-workout-totals").assertIsDisplayed()
            compose.onNodeWithContentDescription("Workout options").performClick()
            compose.onNodeWithText("Hide Workout Totals").performClick()
            compose.onNodeWithTag("active-workout-totals").assertDoesNotExist()
            compose.onNodeWithText("Hide Workout Totals").assertDoesNotExist()
            captureVisualCatalogSurface("fresh.gym.logging-hierarchy")
            compose.onNodeWithTag("quick-set-save-next-$setId").performScrollTo().performClick()
            compose.waitUntil(5_000) { runBlocking { app.gymRepository.sets.first() }.single { it.id == setId }.completed }
        }
    }

    @Test fun historicalSetCorrectionPreservesBothSessionsAndRejectsAStaleEdit() {
        val (finishedId, setId, activeId) = runBlocking {
            prepare()
            val exercise = app.gymRepository.createExercise(ExerciseDraft("History press"))
            val finished = app.gymRepository.startWorkout("Earlier workout")
            val placement = app.gymRepository.addExerciseToWorkout(finished, exercise)
            val set = app.gymRepository.addSet(placement, WorkoutSetDraft(weight = 40.0, reps = 5, completed = true))
            app.gymRepository.finishWorkout(finished)
            Triple(finished, set, app.gymRepository.startWorkout("Current workout"))
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithTag("gym-destination-History").performClick()
            compose.waitUntil(5_000) {
                runBlocking { app.gymRepository.sessions.first() }.single { it.id == finishedId }.restTimerCleanupPending.not()
            }
            val before = runBlocking { app.gymRepository.sessions.first().associateBy { it.id } }
            compose.onNodeWithTag("history-workout-toggle-$finishedId").performScrollTo().performClick()
            compose.onNodeWithTag("history-set-edit-$setId").performScrollTo().performClick()
            compose.onNodeWithTag("workout-set-editor-reps").performScrollTo().performTextReplacement("7")
            closeSoftKeyboard()
            scenario.recreate()
            compose.onNodeWithTag("workout-set-editor-reps").performScrollTo().assertTextContains("7")
            compose.onNodeWithText("Save", substring = false).performClick()
            compose.waitUntil(5_000) { runBlocking { app.gymRepository.sets.first() }.single { it.id == setId }.repetitions == 7 }
            compose.onNodeWithTag("history-set-edit-$setId").performScrollTo().assertIsDisplayed()
            val after = runBlocking { app.gymRepository.sessions.first().associateBy { it.id } }
            assertEquals(before.getValue(finishedId), after.getValue(finishedId))
            assertEquals(before.getValue(activeId), after.getValue(activeId))
            captureVisualCatalogSurface("fresh.gym.history-correction")
            compose.onNodeWithTag("history-set-edit-$setId").performClick()
            compose.onNodeWithTag("workout-set-editor-reps").performScrollTo().performTextReplacement("9")
            runBlocking { app.gymRepository.updateSet(setId, WorkoutSetDraft(weight = 40.0, reps = 8, completed = true)) }
            closeSoftKeyboard()
            compose.onNodeWithText("Save", substring = false).performClick()
            compose.onNode(hasScrollToIndexAction() and hasAnyAncestor(hasTestTag("workout-set-editor")))
                .performScrollToNode(hasText("This Set changed.", substring = true))
            compose.waitUntil(5_000) { compose.onAllNodesWithText("This Set changed.", substring = true).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("workout-set-editor-reps").performScrollTo().assertTextContains("9")
            assertEquals(8, runBlocking { app.gymRepository.sets.first() }.single { it.id == setId }.repetitions)
        }
    }

    @Test fun workoutOverviewPreservesChosenDraftAndOwnedDetailsCorrection() {
        val (sessionId, placements) = runBlocking { prepare(); seedDeepSession() }
        val target = runBlocking { app.gymRepository.sets.first() }.first { it.workoutExerciseId == placements[4] }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Gym tab").performClick()
            fun choose(index: Int) {
                closeSoftKeyboard()
                compose.onNodeWithTag("workout-overview-open").performClick()
                compose.onNodeWithTag("workout-overview-list").performScrollToNode(hasTestTag("workout-overview-exercise-${placements[index]}"))
                compose.onNodeWithTag("workout-overview-exercise-${placements[index]}").performClick()
            }
            compose.onNodeWithTag("workout-overview-open").performClick()
            compose.onNodeWithTag("workout-overview").assertIsDisplayed()
            captureVisualCatalogSurface("deep.gym.session-overview")
            compose.onNodeWithText("Follow Workout Order").performClick()
            choose(4)
            compose.onNodeWithTag("quick-set-load-${target.id}").performScrollTo().performTextReplacement("73")
            choose(1)
            choose(4)
            compose.onNodeWithTag("quick-set-load-${target.id}").performScrollTo().assertTextContains("73")
            scenario.recreate()
            compose.onNodeWithTag("quick-set-load-${target.id}").performScrollTo().assertTextContains("73")
            compose.onNodeWithText("Set Details").performScrollTo().performClick()
            compose.onNodeWithTag("workout-set-editor-load").assertTextContains("73")
            compose.onNodeWithContentDescription("Cancel set editing").performClick()
            compose.onNodeWithTag("quick-set-load-${target.id}").performScrollTo().assertTextContains("73")
            compose.onNodeWithText("Set Details").performScrollTo().performClick()
            compose.onNodeWithTag("workout-set-editor-load").performTextReplacement("75")
            compose.onNodeWithTag("workout-set-editor-reps").performScrollTo().performTextReplacement("7")
            closeSoftKeyboard()
            compose.onNodeWithText("Save").performClick()
            compose.waitUntil(5_000) { runBlocking { app.gymRepository.sets.first() }.single { it.id == target.id }.enteredWeight == 75.0 }
            compose.onNodeWithTag("quick-set-load-${target.id}").performScrollTo().assertTextContains("75")
            compose.onNodeWithTag("quick-set-save-next-${target.id}").performScrollTo().performClick()
            compose.waitUntil(5_000) { runBlocking { app.gymRepository.sets.first() }.single { it.id == target.id }.completed }
            val stored = runBlocking { app.gymRepository.sets.first() }.single { it.id == target.id }
            assertEquals(75.0, stored.enteredWeight!!, 0.0)
            assertEquals(7, stored.repetitions)
            val queued = runBlocking { app.gymRepository.sets.first() }.first { it.workoutExerciseId == placements[1] }
            compose.onNodeWithTag("quick-set-${queued.id}").performScrollTo().assertIsDisplayed()
            assertNotNull(runBlocking { app.gymRepository.sessions.first() }.single { it.id == sessionId }.restTimerDeadlineMillis)
            captureVisualCatalogSurface("deep.gym.session-returned-to-order")
        }
    }

    @Test fun repeatedSetsAndCompletedCorrectionKeepWorkoutContext() {
        val (sessionId, placements) = runBlocking { prepare(); seedDeepSession() }
        val initialSets = runBlocking { app.gymRepository.sets.first() }
        val queued = initialSets.filter { it.workoutExerciseId == placements[1] }.sortedBy { it.position }
        val completed = initialSets.first { it.workoutExerciseId == placements[0] }
        launchMainActivity(Intent(app, MainActivity::class.java)).use {
            compose.onNodeWithContentDescription("Gym tab").performClick()
            queued.take(2).forEach { set ->
                compose.onNodeWithTag("quick-set-save-next-${set.id}").performScrollTo().performClick()
                compose.waitUntil(5_000) { runBlocking { app.gymRepository.sets.first() }.single { it.id == set.id }.completed }
            }
            closeSoftKeyboard()
            val beforeSwitch = runBlocking { app.gymRepository.sessions.first() }.single { it.id == sessionId }.restTimerDeadlineMillis
            compose.onNodeWithTag("workout-overview-open").performClick()
            compose.onNodeWithTag("workout-overview-exercise-${placements[0]}").performClick()
            assertEquals(beforeSwitch, runBlocking { app.gymRepository.sessions.first() }.single { it.id == sessionId }.restTimerDeadlineMillis)
            compose.onNodeWithTag("active-workout-list").performScrollToNode(hasText("4 Completed Sets"))
            compose.onAllNodesWithTag("workout-set-card-${completed.id}").assertCountEquals(0)
            compose.onNodeWithText("4 Completed Sets").performClick()
            compose.onNodeWithTag("workout-set-card-${completed.id}").performScrollTo().performClick()
            compose.onNodeWithTag("workout-set-editor-reps").performScrollTo().performTextReplacement("9")
            closeSoftKeyboard()
            compose.onNodeWithText("Save").performClick()
            compose.waitUntil(5_000) { runBlocking { app.gymRepository.sets.first() }.single { it.id == completed.id }.repetitions == 9 }
            compose.onNodeWithTag("workout-set-card-${completed.id}").performScrollTo().assertIsDisplayed()
            assertEquals(WorkoutSessionState.Active, runBlocking { app.gymRepository.sessions.first() }.single { it.id == sessionId }.state)
            captureVisualCatalogSurface("deep.gym.completed-correction-context")
        }
    }

    @Test fun overviewFinishReviewsRemainingWorkAndOpensExactHistory() {
        val (sessionId, placements) = runBlocking { prepare(); seedDeepSession() }
        runBlocking { app.gymRepository.startRestTimer(sessionId, 120) }
        launchMainActivity(Intent(app, MainActivity::class.java)).use {
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithTag("workout-overview-open").performClick()
            compose.onNodeWithTag("workout-overview-finish").performClick()
            compose.onNodeWithTag("finish-workout-confirmation").assertIsDisplayed()
            captureVisualCatalogSurface("deep.gym.finish-review")
            compose.onNodeWithTag("finish-workout-confirm").performClick()
            compose.waitUntil(5_000) { runBlocking { app.gymRepository.sessions.first() }.single { it.id == sessionId }.state == WorkoutSessionState.Finished }
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("history-workout-card-$sessionId").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("history-workout-card-$sessionId").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("history-workout-exercises-$sessionId").assertExists()
            val finished = runBlocking { app.gymRepository.sessions.first() }.single { it.id == sessionId }
            assertNull(finished.restTimerDeadlineMillis)
            assertEquals(24, runBlocking { app.gymRepository.sets.first() }.count { it.workoutExerciseId in placements })
            captureVisualCatalogSurface("deep.gym.finished-exact-history")
        }
    }

    private suspend fun seedDeepSession(): Pair<Long, List<Long>> {
        val repo = app.gymRepository
        val session = repo.startWorkout("Six station training")
        val placements = (1..6).map { index ->
            val exercise = repo.createExercise(ExerciseDraft("Station $index", defaultRestSeconds = 120))
            val placement = repo.addExerciseToWorkout(session, exercise)
            repeat(4) { repo.addSet(placement, WorkoutSetDraft(weight = 50.0, reps = 5, completed = index == 1)) }
            placement
        }
        repo.createGroup(session, "Accessory pair", WorkoutGroupType.Superset, placements.takeLast(2))
        return session to placements
    }

    @Test fun routineSearchRestoresScopeAndLaunchesTheExplicitDay() {
        val routineId = runBlocking {
            prepare()
            val exercise = app.gymRepository.createExercise(ExerciseDraft("Goblet Squat"))
            val target = app.routineRepository.createRoutine(RoutineDraft("Weekend strength", notes = "Steady build", days = listOf(
                RoutineDayDraft("Saturday", listOf(RoutineExerciseDraft(exercise, plannedSets = listOf(WorkoutSetDraft(weight = 20.0, reps = 8))))),
                RoutineDayDraft("Sunday", listOf(RoutineExerciseDraft(exercise, plannedSets = listOf(WorkoutSetDraft(weight = 25.0, reps = 5))))))))
            app.routineRepository.createRoutine(RoutineDraft("Other plan", days = listOf(RoutineDayDraft("Recovery", listOf(RoutineExerciseDraft(exercise, plannedSets = listOf(WorkoutSetDraft(reps = 5))))))))
            val archived = app.routineRepository.createRoutine(RoutineDraft("Archived strength", days = listOf(
                RoutineDayDraft("Saturday", listOf(RoutineExerciseDraft(exercise, plannedSets = listOf(WorkoutSetDraft(reps = 5))))))))
            app.routineRepository.setRoutineArchived(archived, true)
            target
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithTag("workout-start-list").performScrollToNode(hasText("Browse All Routines"))
            compose.onNodeWithText("Browse All Routines").performClick()
            compose.onNodeWithTag("routine-library-search").assertDoesNotExist()
            compose.onNodeWithTag("workspace-search-action").performClick()
            compose.onNodeWithTag("unified-search-query").performTextReplacement("status:active Saturday squat")
            closeSoftKeyboard()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("unified-search-result-Routine-$routineId").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("unified-search-result-Routine-$routineId").assertIsDisplayed()
            captureVisualCatalogSurface("search-consistency.gym.routines")
            scenario.recreate()
            compose.onNodeWithTag("unified-search-query").assertTextContains("status:active Saturday squat")
            compose.onNodeWithTag("unified-search-query").performTextReplacement("status:archived Saturday squat")
            closeSoftKeyboard()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Archived strength").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Archived strength").assertIsDisplayed()
            compose.onNodeWithTag("unified-search-close-action").performClick()
            compose.onNodeWithTag("routine-library-list").performScrollToNode(hasText("Show archived", ignoreCase = true))
            compose.onNodeWithText("Show archived", ignoreCase = true).performClick()
            compose.onNodeWithTag("routine-library-count").assertTextContains("1 routine · Archived")
            compose.onNodeWithTag("routine-library-list").performScrollToNode(hasText("Archived strength"))
            compose.onNodeWithText("Archived strength").assertIsDisplayed()
            compose.onNodeWithTag("routine-library-list").performScrollToNode(hasContentDescription("Clear filters and reorder all Routines"))
            compose.onNodeWithContentDescription("Clear filters and reorder all Routines").performClick()
            compose.onNodeWithTag("reorder-mode-done").performClick()
            compose.onNodeWithTag("routine-library-count").assertTextContains("2 routines · Active")
            compose.onNodeWithTag("routine-library-search").assertDoesNotExist()
            compose.onNodeWithTag("gym-destination-Workout").performClick()
            compose.onNodeWithTag("workout-start-list").performScrollToNode(hasTestTag("workout-quick-start-$routineId"))
            compose.onNodeWithTag("workout-quick-start-$routineId").performClick()
            compose.onNodeWithTag("routine-library-search").assertDoesNotExist()
            compose.onNodeWithTag("routine-library-list").performScrollToNode(hasText("Start Sunday · 1 exercise"))
            compose.onNodeWithText("Start Sunday · 1 exercise").performClick()
            compose.waitUntil(10_000) { runBlocking { app.gymRepository.sessions.first() }.any { it.state == WorkoutSessionState.Active } }
            val session = runBlocking { app.gymRepository.sessions.first() }.single { it.state == WorkoutSessionState.Active }
            val sunday = runBlocking { app.routineRepository.days.first() }.single { it.routineId == routineId && it.name == "Sunday" }
            assertEquals(sunday.id, session.sourceRoutineDayId)
            compose.onNodeWithTag("gym-destination-Workout").performClick()
            compose.onNodeWithTag("active-workout-list").assertIsDisplayed()
            captureVisualCatalogSurface("experience.gym.routine-started")
        }
    }

    @Test fun emptyHistoryCalendarReturnsToCurrentCollection() {
        runBlocking { prepare(); seed() }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithTag("gym-destination-History").performClick()
            fun history(matcher: SemanticsMatcher): SemanticsNodeInteraction {
                compose.onNode(hasScrollToIndexAction()).performScrollToNode(matcher)
                return compose.onNode(matcher)
            }
            history(hasText("History options", ignoreCase = true)).performClick()
            history(hasText("Calendar view", ignoreCase = true)).performClick()
            history(hasText("History options", ignoreCase = true)).performClick()
            history(hasContentDescription("Previous Month")).performClick()
            history(hasText("No Workouts This Month")).assertIsDisplayed()
            scenario.recreate()
            history(hasText("No Workouts This Month")).assertIsDisplayed()
            captureVisualCatalogSurface("ux-upgrades.gym.history-calendar-empty")
            history(hasText("Show All History")).performClick()
            history(hasText("Library continuity")).assertIsDisplayed()
            history(hasText("History options", ignoreCase = true)).performClick()
            history(hasText("Calendar view", ignoreCase = true)).assertIsOff()
            history(hasText("Show discarded or archived workouts", ignoreCase = true)).assertIsOff()
        }
    }

    @Test fun emptySearchCanRecoverWithoutReopeningFilters() {
        runBlocking { prepare(); seed() }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            open("Exercises")
            compose.onNodeWithTag("exercise-library-search").assertDoesNotExist()
            compose.onNodeWithTag("workspace-search-action").performClick()
            compose.onNodeWithTag("unified-search-query").performTextReplacement("No matching exercise anywhere")
            closeSoftKeyboard()
            scenario.recreate()
            compose.onNodeWithTag("unified-search-query").assertTextContains("No matching exercise anywhere")
            compose.waitUntil(10_000) { compose.onAllNodesWithText(app.getString(R.string.search_no_matches)).fetchSemanticsNodes().isNotEmpty() }
            captureVisualCatalogSurface("search-consistency.gym.empty-search")
            compose.onNodeWithTag("unified-search-close-action").performClick()
            compose.onNodeWithContentDescription("Edit exercise $firstExercise").performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription("Edit exercise Unlinked walk").performScrollTo().assertIsDisplayed()
        }
    }

    @Test fun equipmentSearchAndFilterIncludeEveryLinkedExercise() {
        val fixture = runBlocking { prepare(); seed() }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            open("Exercises")
            capture("exercise-list")
            compose.onNodeWithTag("workspace-search-action").performClick()
            compose.onNodeWithTag("unified-search-query").performTextReplacement(machineName)
            closeSoftKeyboard()
            capture("equipment-search")
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("unified-search-result-Exercise-${fixture.secondExercise}").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("unified-search-result-Exercise-${fixture.exercise}").assertIsDisplayed()
            compose.onNodeWithTag("unified-search-result-Exercise-${fixture.secondExercise}").assertIsDisplayed()
            compose.onNode(hasText("Unlinked walk") and hasAnyAncestor(hasTestTag("unified-search-results-list")))
                .assertDoesNotExist()
            scenario.recreate()
            compose.onNodeWithTag("unified-search-query").assertTextContains(machineName)
            compose.onNodeWithTag("unified-search-close-action").performClick()
            compose.onNodeWithTag("exercise-library-search").assertDoesNotExist()
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


    @Test fun populatedInsightsReturnsFromExactSourceWithRangeExerciseAndPointRetained() {
        val source = runBlocking {
            prepare()
            val first = app.gymRepository.createExercise(ExerciseDraft("Recent default exercise",
                defaultGraphMetric = GymGraphMetric.MaxWeight.name))
            val chosen = app.gymRepository.createExercise(ExerciseDraft("Chosen older trend",
                defaultGraphMetric = GymGraphMetric.MaxWeight.name))
            val date = LocalDate.now().minusDays(150)
            val target = app.gymRepository.startWorkout("Exact older trend source",
                startedAt = date.atStartOfDay(ZoneOffset.UTC).toInstant(), localDate = date)
            val targetPlacement = app.gymRepository.addExerciseToWorkout(target, chosen)
            val targetSet = app.gymRepository.addSet(targetPlacement, WorkoutSetDraft(weight = 67.0, reps = 6, completed = true))
            app.gymRepository.finishWorkout(target)
            val recent = app.gymRepository.startWorkout("Recent distractor", startedAt = Instant.now())
            val recentPlacement = app.gymRepository.addExerciseToWorkout(recent, first)
            app.gymRepository.addSet(recentPlacement, WorkoutSetDraft(weight = 31.0, reps = 5, completed = true))
            app.gymRepository.finishWorkout(recent)
            target to targetSet
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Gym tab").performClick()
            // Capture the full graph after normal timer cleanup, before any analysis navigation.
            compose.waitUntil(10_000) { history().first.none { it.restTimerCleanupPending } }
            val historical = history()
            compose.onNodeWithTag("gym-destination-Insights").performClick()
            val list = compose.onNodeWithTag("gym-progress-list")
            list.performScrollToNode(hasTestTag("gym-progress-exercise-selector"))
            compose.onNode(hasClickAction() and hasAnyAncestor(hasTestTag("gym-progress-exercise-selector"))).performClick()
            compose.onNode(hasText("Chosen older trend") and hasAnyAncestor(hasTestTag("gym-exercise-filter-menu"))).performClick()
            list.performScrollToNode(hasText("Graph Options"))
            compose.onNodeWithText("Graph Options").performClick()
            list.performScrollToNode(hasText("3 Months"))
            compose.onNodeWithText("3 Months").performClick()
            compose.onNodeWithText("All Time").performClick()
            list.performScrollToNode(hasText("Data Points"))
            compose.onNodeWithText("Data Points").performClick()
            list.performScrollToNode(hasText("67 kg"))
            compose.onNodeWithText("67 kg").performClick()
            compose.onNodeWithTag("gym-chart-point-open-workout").assertIsDisplayed()
            captureVisualCatalogSurface("overhaul.gym.insights-return.selected-source")
            compose.onNodeWithTag("gym-chart-point-open-workout").performClick()
            compose.onNodeWithTag("gym-history-back-insights").assertIsDisplayed()
            compose.onNodeWithTag("history-workout-toggle-${source.first}").performScrollTo().assert(hasContentDescription("Exact older trend source", substring = true))
            compose.onNodeWithTag("history-set-performed-${source.second}").performScrollTo().assertTextContains("67 kg × 6 reps")
            captureVisualCatalogSurface("overhaul.gym.insights-return.exact-history")
            scenario.recreate()
            androidx.test.espresso.Espresso.pressBack()
            // Returning restores the exact open point inspector, not just the destination tab.
            compose.onNodeWithTag("gym-chart-point-open-workout").assertIsDisplayed()
            compose.onNodeWithText("Close").performClick()
            list.performScrollToNode(hasTestTag("gym-progress-exercise-selector"))
            compose.onNode(hasText("Chosen older trend") and hasAnyAncestor(hasTestTag("gym-progress-exercise-selector")),
                useUnmergedTree = true).assertIsDisplayed()
            list.performScrollToNode(hasText("All Time"))
            compose.onNodeWithText("All Time").assertIsDisplayed()
            list.performScrollToNode(hasText("67 kg"))
            compose.onNodeWithText("67 kg").assertIsDisplayed()
            captureVisualCatalogSurface("overhaul.gym.insights-return.retained-analysis")
            assertEquals(historical, history())
        }
    }

    @Test fun crossUnitSavedPlatePresetConvertsAuthoredTargetAndRestoresItsHardware() {
        val poundPreset = PlatePreset("Saved pound rack", "pound", 45.0, listOf(45.0, 25.0, 5.0),
            plateQuantities = mapOf(45.0 to 4, 25.0 to 2, 5.0 to 2), collarWeight = 5.0)
        val kilogramPreset = PlatePreset("Saved kilogram rack", "kilogram", 20.0, listOf(20.0, 10.0, 2.5),
            plateQuantities = mapOf(20.0 to 4, 10.0 to 2, 2.5 to 2))
        runBlocking {
            prepare()
            app.settingsRepository.update { it.copy(gymWeightUnitId = "kilogram",
                platePresets = listOf(poundPreset, kilogramPreset)) }
        }
        fun field(label: String): SemanticsNodeInteraction {
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(label))
            return compose.onNodeWithText(label)
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithTag("gym-destination-Library").performClick()
            compose.onNodeWithTag("gym-library-Tools").performScrollTo().performClick()
            compose.onNodeWithText("Plate Calculator").performClick()
            field("Target (kg)").performTextReplacement("100")
            closeSoftKeyboard()
            field("Saved pound rack").performClick()
            field("Target (lb)").assertTextContains("220.5")
            field("Bar / sled / base (lb)").assertTextContains("45")
            field("Total collars / fixed add-on (lb)").assertTextContains("5")
            field("Optional total inventory quantities").assertTextContains("45:4,25:2,5:2")
            captureVisualCatalogSurface("overhaul.gym.plate-preset.converted-pound")
            scenario.recreate()
            field("Target (lb)").assertTextContains("220.5")
            field("Saved pound rack").assertIsSelected()
            field("Saved kilogram rack").performClick()
            field("Target (kg)").assertTextContains("100")
            field("Bar / sled / base (kg)").assertTextContains("20")
            field("Available plates (kg), comma-separated").assertTextContains("20,10,2.5")
            field("Optional total inventory quantities").assertTextContains("20:4,10:2,2.5:2")
            captureVisualCatalogSurface("overhaul.gym.plate-preset.converted-kilogram")
            assertEquals(listOf(poundPreset, kilogramPreset), runBlocking { app.settingsRepository.current() }.platePresets)
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
