package com.whip.app

import android.content.Intent
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import com.whip.app.core.ReviewPeriod
import com.whip.app.ui.*
import com.whip.app.ui.theme.WhipTheme
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.core.ReviewSection
import com.whip.app.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test

class ReviewOutcomeJourneyE2ETest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun archivedTaskOutcomesOpenTheirOriginalTask() = sourceJourney(ReviewSection.Tasks, "Archived design task")
    @Test fun archivedHabitOutcomesOpenTheirOriginalHabit() = sourceJourney(ReviewSection.Habits, "Archived reflection")
    @Test fun archivedGoalProgressOpensItsOriginalGoal() = sourceJourney(ReviewSection.Goals, "Archived reading goal")
    @Test fun finishedWorkoutOutcomesOpenTheirOriginalSession() = sourceJourney(ReviewSection.Gym, "Finished review workout")

    @Test fun mixedTaskHistoryKeepsAreaScopeAndDistinctOccurrences() {
        val today = app.clock.today()
        val (active, recurring) = runBlocking {
            app.backupRepository.deleteAllData()
            val work = app.areaRepository.create("Work")
            val personal = app.areaRepository.create("Personal")
            app.settingsRepository.update { AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark,
                dynamicColor = false, activeAreaScope = AreaScope.One(work).storageKey) }
            val active = app.taskRepository.create(TaskDraft("Finished current work", areaId = work))
            app.taskRepository.completeOccurrence(active, null)
            val recurring = app.taskRepository.create(TaskDraft("Daily design notes", areaId = work,
                scheduleKind = ScheduleKind.Recurring,
                recurrence = RecurrenceRule(RecurrenceUnit.Days, startDate = today.minusDays(1))))
            app.taskRepository.completeOccurrence(recurring, today.minusDays(1))
            app.taskRepository.completeOccurrence(recurring, today)
            app.taskRepository.archive(recurring)
            val excluded = app.taskRepository.create(TaskDraft("Personal outcome", areaId = personal))
            app.taskRepository.completeOccurrence(excluded, null)
            active to recurring
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            val review = (hasText("Review & Trends") or hasText("Review Progress")) and hasClickAction()
            compose.waitUntil(15_000) { compose.onAllNodes(review).fetchSemanticsNodes().isNotEmpty() }
            compose.onAllNodes(review)[0].performScrollTo().performClick()
            compose.onNodeWithTag("review-total-Tasks", useUnmergedTree = true).assertTextEquals("3")
            compose.onNodeWithTag("review-signal-Tasks").performScrollTo().performClick()
            val dateFormat = java.time.format.DateTimeFormatter.ofPattern("MMM d, uuuu", java.util.Locale.getDefault())
            fun assertRows() {
                listOf(today.minusDays(1), today).forEach { original ->
                    compose.onNodeWithTag("review-outcome-Tasks:$recurring:${original.toEpochDay()}:$today")
                        .performScrollTo().assertTextContains("Scheduled: ${original.format(dateFormat)}", substring = true)
                }
                compose.onNodeWithTag("review-outcome-Tasks:$active:task:$today").performScrollTo().assertIsDisplayed()
                compose.onAllNodesWithText("Personal outcome").assertCountEquals(0)
            }
            assertRows()
            scenario.recreate()
            assertRows()
            captureVisualCatalogSurface("shared.review.mixed-task-outcomes")
            compose.onNodeWithContentDescription("Back to Review & Trends").performClick()
            compose.onNodeWithTag("review-total-Tasks", useUnmergedTree = true).assertTextEquals("3")
            compose.onNodeWithTag("review-signal-Tasks").performScrollTo().performClick()
            compose.onNodeWithTag("review-outcome-Tasks:$active:task:$today").performScrollTo().performClick()
            compose.onNodeWithTag("entity-inspector-title").assertTextEquals("Finished current work")
            compose.onNodeWithTag("entity-inspector-status").assert(hasAnyDescendant(hasText("Completed")))
            compose.onNodeWithText("No scheduled date.").assertIsDisplayed()
            captureVisualCatalogSurface("shared.review.original-current-task")
            compose.onNodeWithTag("entity-inspector-close").performClick()
            compose.onNodeWithTag("return-to-review").performClick()
            assertRows()
            scenario.recreate()
            assertRows()
            compose.onNodeWithTag("review-outcome-Tasks:$recurring:${today.toEpochDay()}:$today")
                .performScrollTo().performClick()
            compose.onNodeWithTag("entity-inspector-title").assertTextEquals("Daily design notes")
            compose.onNodeWithTag("entity-inspector-close").performClick()
            androidx.test.espresso.Espresso.pressBack()
            compose.onNodeWithTag("review-outcome-list").assertIsDisplayed()
            captureVisualCatalogSurface("deep.review.returned-context")
        }
    }


    @Test fun archivedFinishedWorkoutKeepsItsOneDayReviewCountAndVisiblePoint() {
        val date = app.clock.today().withDayOfMonth(1)
        val id = runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark, dynamicColor = false) }
            val exercise = app.gymRepository.createExercise(ExerciseDraft("Retained review exercise"))
            val workout = app.gymRepository.startWorkout("Retained finished workout", localDate = date)
            val placement = app.gymRepository.addExerciseToWorkout(workout, exercise)
            app.gymRepository.addSet(placement, WorkoutSetDraft(weight = 40.0, reps = 5, completed = true))
            app.gymRepository.finishWorkout(workout)
            workout
        }
        val source = mutableStateOf(runBlocking { app.gymRepository.sessions.first().single { it.id == id } })
        var pointColor = Color.Transparent
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            // Fixed-date production Review host, not a device-clock change or shell archive gesture.
            scenario.onActivity { activity ->
                activity.setContent {
                    WhipTheme(darkTheme = true, dynamicColor = false) {
                        pointColor = MaterialTheme.colorScheme.primary
                        val session = source.value
                        ReviewDialog(
                            taskState = TaskUiState(currentDate = date, loading = false),
                            habitState = HabitUiState(loading = false), goalState = GoalUiState(loading = false),
                            gymState = GymUiState(loading = false, allSessions = listOf(session),
                                history = listOf(session).filterNot { it.archived },
                                archivedWorkouts = listOf(session).filter { it.archived }),
                            period = ReviewPeriod.Monthly, sections = setOf(ReviewSection.Gym),
                            onPeriodChange = {}, onDismiss = {},
                        )
                    }
                }
            }
            for ((stage, archived) in listOf("before" to false, "archived" to true, "restored" to false)) {
                val stored = runBlocking {
                    val dao = app.database.gymDao()
                    val current = requireNotNull(dao.getSession(id))
                    dao.updateSession(current.copy(archived = archived))
                    app.gymRepository.sessions.first().single { it.id == id }
                }
                compose.runOnIdle { source.value = stored }
                compose.onNodeWithTag("review-total-Gym", useUnmergedTree = true).assertTextEquals("1")
                val chart = compose.onNodeWithContentDescription("Workouts daily values: 1", useUnmergedTree = true)
                chart.performScrollTo().assertIsDisplayed()
                val pixels = chart.captureToImage().toPixelMap()
                var colored = 0
                for (x in 0 until pixels.width) for (y in 0 until pixels.height) {
                    val color = pixels[x, y]
                    if (kotlin.math.abs(color.red - pointColor.red) < 0.03f &&
                        kotlin.math.abs(color.green - pointColor.green) < 0.03f &&
                        kotlin.math.abs(color.blue - pointColor.blue) < 0.03f) colored++
                }
                org.junit.Assert.assertTrue("The sole observation must paint a visible point: $colored pixels", colored > 12)
                compose.onNodeWithTag("review-signal-Gym").performClick()
                val row = compose.onNodeWithTag("review-outcome-Gym:$id:$date")
                row.performScrollTo().assertTextContains("Retained finished workout", substring = true)
                if (archived) row.assertTextContains("Archived", substring = true)
                captureVisualCatalogSurface("overhaul.review.finished-workout-$stage")
                compose.onNodeWithContentDescription("Back to Review & Trends").performClick()
            }
        }
        val restored = runBlocking { app.gymRepository.sessions.first().single { it.id == id } }
        org.junit.Assert.assertFalse(restored.archived)
        assertEquals(WorkoutSessionState.Finished, restored.state)
    }

    private fun sourceJourney(section: ReviewSection, title: String) {
        val setId = runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark, dynamicColor = false) }
            val today = app.clock.today()
            val task = app.taskRepository.create(TaskDraft("Archived design task"))
            app.taskRepository.completeOccurrence(task, null)
            app.taskRepository.archive(task)
            val habit = app.habitRepository.create(HabitDraft("Archived reflection", startDate = today))
            app.habitRepository.setCheckOff(habit, today, true)
            app.habitRepository.setArchived(habit, true)
            val goal = app.goalRepository.create(GoalDraft("Archived reading goal", type = GoalType.ReachValue,
                startDate = today, targetMin = 10.0, precision = 3))
            app.goalRepository.recordMeasurement(goal, 0.05, today)
            app.goalRepository.setArchived(requireNotNull(app.goalRepository.get(goal)).mutationBoundary(), true)
            val exercise = app.gymRepository.createExercise(ExerciseDraft("Review bench press"))
            val workout = app.gymRepository.startWorkout("Finished review workout")
            val placement = app.gymRepository.addExerciseToWorkout(workout, exercise)
            val set = app.gymRepository.addSet(placement, WorkoutSetDraft(weight = 40.0, reps = 5, completed = true))
            app.gymRepository.finishWorkout(workout)
            set
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            val review = (hasText("Review & Trends") or hasText("Review Progress")) and hasClickAction()
            compose.waitUntil(15_000) { compose.onAllNodes(review).fetchSemanticsNodes().isNotEmpty() }
            compose.onAllNodes(review)[0].performScrollTo().performClick()
            if (section == ReviewSection.Goals) {
                compose.onNodeWithTag("review-total-Goals", useUnmergedTree = true).assertTextEquals("0.005")
            }
            compose.onNodeWithTag("review-signal-${section.name}").performScrollTo().performClick()
            compose.waitForIdle()
            captureVisualCatalogSurface("shared.review.outcomes-${section.name.lowercase()}")
            compose.onNodeWithTag("review-outcome-list").assertIsDisplayed()
            compose.onNodeWithText(title).performScrollTo().assertIsDisplayed()
            if (section == ReviewSection.Goals) compose.onAllNodesWithText("Progress score: 0.005", useUnmergedTree = true)
                .assertCountEquals(2)
            scenario.recreate()
            compose.onNodeWithText(title).performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription("Back to Review & Trends").performClick()
            compose.onNodeWithTag("review-signal-${section.name}").performScrollTo().performClick()
            compose.onNodeWithText(title).performScrollTo().performClick()
            compose.onAllNodesWithTag("review-outcome-list").assertCountEquals(0)
            compose.waitUntil(10_000) { compose.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty() }
            if (section == ReviewSection.Gym) {
                compose.onNodeWithTag("gym-destination-History").assertIsSelected()
                compose.onNodeWithTag("history-set-performed-$setId").performScrollTo().assertTextContains("40 kg × 5 reps")
            } else {
                compose.onNodeWithTag("entity-inspector-title").assertTextEquals(title)
                compose.onNodeWithTag("entity-inspector-status").assert(hasAnyDescendant(hasText("Archived")))
                if (section == ReviewSection.Tasks) compose.onNodeWithText("No scheduled date.").assertIsDisplayed()
                if (section == ReviewSection.Habits) compose.onNodeWithTag("entity-inspector-content-history").assertIsDisplayed()
            }
            captureVisualCatalogSurface("shared.review.original-${section.name.lowercase()}")
            if (section != ReviewSection.Gym) compose.onNodeWithTag("entity-inspector-close").performClick()
            compose.onNodeWithTag("return-to-review").performClick()
            compose.onNodeWithTag("review-outcome-list").assertIsDisplayed()
            val returnedOutcome = hasText(title) and hasAnyAncestor(hasTestTag("review-outcome-list"))
            compose.onNode(returnedOutcome).performScrollTo().assertIsDisplayed()
            scenario.recreate()
            compose.onNode(returnedOutcome).performScrollTo().assertIsDisplayed()
            captureVisualCatalogSurface("deep.review.returned-${section.name.lowercase()}")
            if (section != ReviewSection.Gym) {
                compose.onNode(returnedOutcome).performClick()
                compose.onNodeWithTag("entity-inspector-title").assertTextEquals(title)
                compose.onNodeWithTag("entity-inspector-close").performClick()
                androidx.test.espresso.Espresso.pressBack()
                compose.onNodeWithTag("review-outcome-list").assertIsDisplayed()
                compose.onNode(returnedOutcome).performScrollTo().assertIsDisplayed()
            }
        }
    }
}
