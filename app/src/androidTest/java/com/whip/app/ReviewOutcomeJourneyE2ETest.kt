package com.whip.app

import android.content.Intent
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
        }
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
                compose.onNodeWithText("Showing the selected workout.").assertIsDisplayed()
                compose.onNodeWithTag("history-set-performed-$setId").performScrollTo().assertTextContains("40 kg × 5 reps")
            } else {
                compose.onNodeWithTag("entity-inspector-title").assertTextEquals(title)
                compose.onNodeWithTag("entity-inspector-status").assert(hasAnyDescendant(hasText("Archived")))
                if (section == ReviewSection.Tasks) compose.onNodeWithText("No scheduled date.").assertIsDisplayed()
                if (section == ReviewSection.Habits) compose.onNodeWithTag("entity-inspector-content-history").assertIsDisplayed()
            }
            captureVisualCatalogSurface("shared.review.original-${section.name.lowercase()}")
        }
    }
}
