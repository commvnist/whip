package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.core.HomeSection
import com.whip.app.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Visible execution controls reach the real repositories through the ordinary app shell. */
class DirectDailyExecutionJourneyTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()

    @Before fun prepare() = runBlocking {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update {
            AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark, dynamicColor = false,
                goalCelebrationEnabled = false, notificationPermissionRequested = true,
                showHabitsInTaskPlanning = true)
        }
    }
    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    private fun await(condition: () -> Boolean) = compose.waitUntil(10_000, condition)
    private fun assertNoInspector() {
        compose.onAllNodesWithTag("task-actions-surface").assertCountEquals(0)
        compose.onAllNodesWithTag("habit-detail-surface").assertCountEquals(0)
        compose.onAllNodesWithTag("goal-detail-surface").assertCountEquals(0)
    }

    @Test fun collectionExecutesExactRecurringOccurrenceAndReopensIt() {
        val today = app.clock.today()
        val id = runBlocking {
            app.taskRepository.create(TaskDraft("Daily practice", scheduleKind = ScheduleKind.Recurring,
                date = today, recurrence = RecurrenceRule(unit = RecurrenceUnit.Days, startDate = today),
                steps = listOf(TaskStepDraft(title = "Prepare practice", position = 0)), autoCompleteFromSteps = false))
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Tasks tab").performClick()
            compose.selectTaskCollectionScope("All Tasks")
            compose.onNodeWithContentDescription("Complete Subtask Prepare practice").performScrollTo().performClick()
            await { runBlocking { app.taskRepository.stepStates.first().any { it.completed } } }
            compose.onNodeWithTag("task-execution-context-$id", useUnmergedTree = true).assertTextContains("For $today")
            captureVisualCatalogSurface("direct.tasks.collection-recurring")
            compose.onNodeWithContentDescription("Complete task Daily practice").performScrollTo().performClick()
            await { runBlocking { app.taskRepository.getOccurrences(id).any { it.originalDate == today && it.state == OccurrenceState.Completed } } }
            assertNoInspector()
            compose.onAllNodesWithContentDescription("Complete task Daily practice").assertCountEquals(0)
            compose.onNodeWithTag("task-destination-History").performClick()
            compose.onNodeWithContentDescription("Reopen task Daily practice").performScrollTo().performClick()
            await { runBlocking { app.taskRepository.getOccurrences(id).none { it.state == OccurrenceState.Completed } } }
            assertNoInspector()
            scenario.recreate()
            compose.selectTaskCollectionScope("All Tasks")
            compose.onNodeWithContentDescription("Complete task Daily practice").performScrollTo().assertIsDisplayed()
        }
    }

    @Test fun homeChecksChecklistLogsGoalAndConfirmsClosureWithoutAnInspector() {
        val ids = runBlocking {
            app.settingsRepository.update { it.copy(homeSections = listOf(HomeSection.Habits, HomeSection.Goals),
                hiddenHomeSections = HomeSection.entries.filterNot { section -> section in setOf(HomeSection.Habits, HomeSection.Goals) }.toSet()) }
            val habit = app.habitRepository.create(HabitDraft("Reset", trackingMode = HabitTrackingMode.Checklist,
                startDate = app.clock.today(), checklistItems = listOf(HabitChecklistItemDraft("Prepare tomorrow", 0))))
            val goal = app.goalRepository.create(GoalDraft("Practice total", type = GoalType.ReachValue,
                dimension = UnitDimension.Count, unitId = "count", baseline = 0.0, targetMin = 10.0, startDate = app.clock.today()))
            habit to goal
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use {
            await { compose.onAllNodesWithTag("home-list").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("home-list").performScrollToNode(hasContentDescription("Complete checklist item Prepare tomorrow"))
            compose.onNodeWithContentDescription("Complete checklist item Prepare tomorrow").performClick()
            await { runBlocking { app.habitRepository.logs.first().any { it.habitId == ids.first } } }
            assertNoInspector()
            compose.onNodeWithTag("home-list").performScrollToNode(hasContentDescription("Log progress for Practice total"))
            compose.onNodeWithContentDescription("Log progress for Practice total").performClick()
            compose.onNodeWithTag("goal-measurement-value").performTextReplacement("4")
            closeSoftKeyboard()
            compose.onNodeWithTag("goal-measurement-save").performClick()
            await { runBlocking { app.goalRepository.measurementEntries.first().any { it.canonicalValue == 4.0 } } }
            await { compose.onAllNodesWithTag("goal-measurement-value").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithTag("home-list").performScrollToNode(hasTestTag("goal-card-complete-${ids.second}"))
            compose.onNodeWithTag("goal-card-complete-${ids.second}").performClick()
            assertNoInspector()
            captureVisualCatalogSurface("direct.home.goal-completion")
            compose.onNodeWithTag("goal-completion-confirm").performClick()
            await { runBlocking { app.goalRepository.get(ids.second)?.status == GoalStatus.Completed } }
            assertNoInspector()
            runBlocking { assertEquals(.4, app.database.goalDao().getClosureSnapshots(ids.second).single().progress!!, 0.0) }
        }
    }

    @Test fun calendarHabitAndMultiDayRoutineRunWithoutExpandingDetails() {
        val ids = runBlocking {
            val habit = app.habitRepository.create(HabitDraft("Calendar practice", startDate = app.clock.today()))
            app.taskRepository.create(TaskDraft("Calendar recurrence", scheduleKind = ScheduleKind.Recurring,
                date = app.clock.today(), recurrence = RecurrenceRule(unit = RecurrenceUnit.Days, startDate = app.clock.today())))
            val exercise = app.gymRepository.createExercise(ExerciseDraft("Goblet squat"))
            val routine = app.routineRepository.createRoutine(RoutineDraft("Two day plan", days = listOf("Upper", "Lower").map { name ->
                RoutineDayDraft(name, listOf(RoutineExerciseDraft(exerciseId = exercise,
                    plannedSets = listOf(WorkoutSetDraft(reps = 5)))))
            }))
            val lower = app.routineRepository.days.first().single { it.routineId == routine && it.name == "Lower" }
            Triple(habit, routine, lower.id)
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use {
            compose.onNodeWithContentDescription("Tasks tab").performClick()
            compose.selectTaskCollectionScope("All Tasks")
            compose.selectTaskPlanningLayout("Calendar")
            compose.onNodeWithTag("task-workspace-list").performScrollToNode(hasTestTag("task-calendar"))
            val today = app.clock.today()
            if (java.time.YearMonth.from(today) != java.time.YearMonth.from(today.plusDays(1))) {
                compose.onNodeWithContentDescription("Previous Month").performScrollTo().performClick()
            }
            compose.onNodeWithTag("task-calendar-day-${today.toEpochDay()}", useUnmergedTree = true)
                .performScrollTo().performClick()
            compose.onNodeWithText("Selected: ${app.clock.today().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}")
                .performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("task-workspace-list")
                .performScrollToNode(hasContentDescription("Complete task Calendar recurrence"))
            compose.onNodeWithContentDescription("Complete task Calendar recurrence").assertIsDisplayed()
            compose.onNodeWithTag("task-workspace-list")
                .performScrollToNode(hasContentDescription("Check off habit Calendar practice"))
            compose.onNodeWithContentDescription("Check off habit Calendar practice").performClick()
            await { runBlocking { app.habitRepository.logs.first().any { it.habitId == ids.first && it.localDate == app.clock.today() } } }
            assertNoInspector()
            captureVisualCatalogSurface("direct.tasks.calendar-habit")
            compose.onNodeWithTag("task-workspace-list").performScrollToNode(hasText("Tomorrow"))
            compose.onNodeWithText("Tomorrow").performClick()
            compose.onNodeWithText("Selected: ${app.clock.today().plusDays(1).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}")
                .performScrollTo().assertIsDisplayed()
            compose.onAllNodesWithContentDescription("Check off habit Calendar practice").assertCountEquals(0)
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithTag("gym-destination-Library").performClick()
            compose.onNodeWithTag("gym-library-Routines").performClick()
            compose.onNodeWithTag("routine-details-${ids.second}").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("routine-start-day-${ids.third}").performScrollTo().assertIsDisplayed()
            captureVisualCatalogSurface("direct.gym.static-day-choices")
            compose.onNodeWithTag("routine-start-day-${ids.third}").performClick()
            await { runBlocking { app.gymRepository.sessions.first().any { it.state == WorkoutSessionState.Active } } }
            runBlocking { assertEquals(ids.third, app.gymRepository.sessions.first().single().sourceRoutineDayId) }
        }
    }
}
