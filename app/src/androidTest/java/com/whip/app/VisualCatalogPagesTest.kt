package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.GoalType
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.RoutineDayDraft
import com.whip.app.domain.RoutineDraft
import com.whip.app.domain.RoutineExerciseDraft
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackValueDraft
import com.whip.app.domain.WorkoutSetDraft
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VisualCatalogPagesTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app: WhipApplication
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun resetToDeterministicDarkTheme() = runBlocking {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update {
            AppSettings(
                setupCompleted = true,
                themeMode = AppThemeMode.Dark,
                dynamicColor = false,
            )
        }
    }

    @After
    fun clearProductData() = runBlocking {
        app.backupRepository.deleteAllData()
    }

    @Test
    fun captureSharedPageCatalog() {
        launch().use {
            waitForHome()
            captureVisualCatalogSurface("shared.home.empty")
        }
        seedRepresentativeData()
        launch().use {
            waitForHome("Plan the week")
            captureVisualCatalogSurface("shared.home.populated")
        }
    }

    @Test
    fun captureTaskPageCatalog() {
        launch().use {
            waitForHome()
            openPrimary("Tasks")
            captureVisualCatalogSurface("tasks.today.empty")
        }
        seedRepresentativeData()
        launch().use {
            waitForHome("Plan the week")
            captureTaskPages()
        }
    }

    @Test
    fun captureHabitPageCatalog() {
        launch().use {
            waitForHome()
            openPrimary("Habits")
            captureVisualCatalogSurface("habits.today.empty")
        }
        seedRepresentativeData()
        launch().use {
            waitForHome("Plan the week")
            captureHabitPages()
        }
    }

    @Test
    fun captureGoalPageCatalog() {
        launch().use {
            waitForHome()
            openPrimary("Goals")
            captureVisualCatalogSurface("goals.active.empty")
        }
        seedRepresentativeData()
        launch().use {
            waitForHome("Plan the week")
            captureGoalPages()
        }
    }

    @Test
    fun captureTrackPageCatalog() {
        launch().use {
            waitForHome()
            openPrimary("Tracks")
            captureVisualCatalogSurface("tracks.all.empty")
        }
        val seeded = seedRepresentativeData()
        launch().use {
            waitForHome("Plan the week")
            captureTrackPages(seeded.activeTrackId)
        }
    }

    @Test
    fun captureGymPageCatalog() {
        launch().use {
            waitForHome()
            openPrimary("Gym")
            captureVisualCatalogSurface("gym.workout.empty")
        }
        seedRepresentativeData()
        launch().use {
            waitForHome("Plan the week")
            captureGymPages()
        }
    }

    @Test
    fun captureSettingsPageCatalog() {
        seedRepresentativeData()
        launch().use {
            waitForHome("Plan the week")
            captureSettingsPages()
        }
    }

    private fun captureTaskPages() {
        openPrimary("Tasks")
        captureVisualCatalogSurface("tasks.today.populated")
        selectTag("task-destination-Inbox")
        captureVisualCatalogSurface("tasks.inbox.populated")
        selectTag("task-destination-Upcoming")
        captureVisualCatalogSurface("tasks.upcoming.list")
        compose.onNodeWithText("Agenda").performClick()
        compose.waitForIdle()
        captureVisualCatalogSurface("tasks.upcoming.agenda")
        compose.onNodeWithText("Calendar").performClick()
        compose.waitForIdle()
        captureVisualCatalogSurface("tasks.upcoming.calendar")
        selectTag("task-destination-History")
        captureVisualCatalogSurface("tasks.history.completed")
        compose.onNodeWithText("Archived").performClick()
        compose.waitForIdle()
        captureVisualCatalogSurface("tasks.history.archived")
    }

    private fun captureHabitPages() {
        openPrimary("Habits")
        captureVisualCatalogSurface("habits.today.populated")
        compose.onNodeWithContentDescription("More Habit Actions").performClick()
        captureVisualCatalogSurface("habits.row.menu")
        compose.onNodeWithText("Browse Templates").performClick()
        compose.waitForIdle()
        captureVisualCatalogSurface("habits.template")
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()
        selectTag("habit-destination-All")
        captureVisualCatalogSurface("habits.all.populated")
        selectTag("habit-destination-Archived")
        captureVisualCatalogSurface("habits.archived.populated")
        selectTag("habit-destination-Insights")
        captureVisualCatalogSurface("habits.insights.populated")
    }

    private fun captureGoalPages() {
        openPrimary("Goals")
        captureVisualCatalogSurface("goals.active.populated")
        compose.onNodeWithContentDescription("More Goal Actions").performClick()
        captureVisualCatalogSurface("goals.row.menu")
        compose.onNodeWithTag("goal-browse-templates-menu-action").performClick()
        compose.waitForIdle()
        captureVisualCatalogSurface("goals.template")
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()
        selectTag("goal-destination-History")
        captureVisualCatalogSurface("goals.history.populated")
        selectTag("goal-destination-Archived")
        captureVisualCatalogSurface("goals.archived.populated")
        selectTag("goal-destination-Insights")
        captureVisualCatalogSurface("goals.insights.populated")
    }

    private fun captureTrackPages(activeTrackId: Long) {
        openPrimary("Tracks")
        captureVisualCatalogSurface("tracks.all.populated")
        selectTag("track-workspace-destination-Activity")
        captureVisualCatalogSurface("tracks.activity.populated")
        selectTag("track-workspace-destination-Archived")
        captureVisualCatalogSurface("tracks.archived.populated")
        selectTag("track-workspace-destination-Insights")
        captureVisualCatalogSurface("tracks.insights.populated")
        selectTag("track-workspace-destination-Tracks")
        compose.onNodeWithTag("track-card-$activeTrackId").performClick()
        compose.waitForIdle()
        captureVisualCatalogSurface("tracks.detail.entries")
        selectTag("track-destination-Options")
        captureVisualCatalogSurface("tracks.detail.options")
        selectTag("track-destination-Insights")
        captureVisualCatalogSurface("tracks.detail.insights")
    }

    private fun captureGymPages() {
        openPrimary("Gym")
        captureVisualCatalogSurface("gym.workout.active")
        selectTag("gym-destination-History")
        captureVisualCatalogSurface("gym.history.populated")
        selectTag("gym-destination-Progress")
        captureVisualCatalogSurface("gym.progress.populated")
        selectTag("gym-destination-Library")
        captureVisualCatalogSurface("gym.library.landing")

        listOf("Routines", "Exercises", "Machines", "Categories", "Tools").forEach { destination ->
            compose.onNodeWithTag("gym-library-list").performScrollToNode(
                androidx.compose.ui.test.hasText(destination),
            )
            compose.onNodeWithTag("gym-library-$destination").performClick()
            compose.waitForIdle()
            captureVisualCatalogSurface("gym.${destination.lowercase()}.populated".replace("tools.populated", "tools"))
            compose.onNodeWithTag("gym-library-child-$destination").performClick()
            compose.waitForIdle()
        }
    }

    private fun captureSettingsPages() {
        compose.onNodeWithContentDescription("Open Settings").performClick()
        compose.waitForIdle()
        captureVisualCatalogSurface("settings.index")
        val sections = listOf(
            "Appearance & Home" to "settings.appearance",
            "Planning & Units" to "settings.planning",
            "Organization" to "settings.organization",
            "Reminders" to "settings.reminders",
            "Data & Privacy" to "settings.data-privacy",
            "About Whip" to "settings.about-diagnostics",
        )
        sections.forEachIndexed { index, (label, surfaceId) ->
            compose.onNodeWithTag("settings-section-$label").performClick()
            compose.waitForIdle()
            captureVisualCatalogSurface(surfaceId)
            if (index < sections.lastIndex) {
                compose.onNodeWithContentDescription("Back to Settings").performClick()
                compose.waitForIdle()
            }
        }
    }

    private fun seedRepresentativeData(): SeededCatalogData = runBlocking {
        val today = app.clock.today()
        app.taskRepository.create(TaskDraft("Plan the week", scheduleKind = ScheduleKind.Once, date = today))
        app.taskRepository.create(TaskDraft("Sort the inbox"))
        app.taskRepository.create(
            TaskDraft("Prepare design review", scheduleKind = ScheduleKind.Once, date = today.plusDays(5)),
        )
        val completedTaskId = app.taskRepository.create(
            TaskDraft("Send the project update", scheduleKind = ScheduleKind.Once, date = today),
        )
        app.taskRepository.completeOccurrence(completedTaskId, today)
        val archivedTaskId = app.taskRepository.create(TaskDraft("Someday idea"))
        app.taskRepository.archive(archivedTaskId)

        app.habitRepository.create(HabitDraft(name = "Morning medication", icon = "💊", startDate = today))
        app.habitRepository.create(HabitDraft(name = "Evening stretch", icon = "🧘", startDate = today))
        val archivedHabitId = app.habitRepository.create(
            HabitDraft(name = "Previous routine", icon = "🌱", startDate = today.minusDays(30)),
        )
        app.habitRepository.setArchived(archivedHabitId, true)

        val activeGoalId = app.goalRepository.create(
            GoalDraft(
                name = "Read 24 books",
                icon = "📚",
                type = GoalType.ReachValue,
                targetMin = 24.0,
                startDate = today.minusDays(30),
            ),
        )
        app.goalRepository.recordMeasurement(activeGoalId, 8.0, today, note = "Current progress")
        app.goalRepository.create(
            GoalDraft(
                name = "Build emergency savings",
                icon = "🛟",
                type = GoalType.ReachValue,
                targetMin = 10_000.0,
                startDate = today.minusDays(14),
            ),
        )
        val completedGoalId = app.goalRepository.create(
            GoalDraft(
                name = "Launch personal site",
                icon = "🚀",
                type = GoalType.ReachValue,
                targetMin = 1.0,
                startDate = today.minusDays(60),
            ),
        )
        app.goalRepository.setStatus(completedGoalId, GoalStatus.Completed)
        val archivedGoalId = app.goalRepository.create(
            GoalDraft(
                name = "Old savings plan",
                icon = "🏦",
                type = GoalType.ReachValue,
                targetMin = 1_000.0,
                startDate = today.minusDays(90),
            ),
        )
        app.goalRepository.setStatus(archivedGoalId, GoalStatus.Archived)

        val activeTrackId = app.trackRepository.create(
            TrackDraft(
                name = "Reading Log",
                icon = "📖",
                fields = listOf(
                    TrackFieldDraft("Book", TrackFieldType.ShortText, required = true, primary = true),
                ),
            ),
        )
        val preparation = checkNotNull(app.trackRepository.prepareEntryCreate(activeTrackId))
        val primaryField = preparation.form.fields.single()
        app.trackRepository.addEntry(
            preparation.request,
            TrackEntryDraft(
                entryDate = today,
                values = mapOf(primaryField.uuid to TrackValueDraft(textValue = "The Dispossessed")),
            ),
        )
        val archivedTrackId = app.trackRepository.create(
            TrackDraft(
                name = "Archived Mood Log",
                fields = listOf(
                    TrackFieldDraft("Mood", TrackFieldType.ShortText, required = true, primary = true),
                ),
            ),
        )
        app.trackRepository.setArchived(archivedTrackId, true)

        val exerciseId = app.gymRepository.createExercise(
            ExerciseDraft(name = "Goblet Squat", defaultGraphMetric = "MaxWeight"),
        )
        val historyId = app.gymRepository.startWorkout("Saturday Strength")
        val historyExerciseId = app.gymRepository.addExerciseToWorkout(historyId, exerciseId)
        app.gymRepository.addSet(
            historyExerciseId,
            WorkoutSetDraft(weight = 24.0, reps = 10, completed = true),
        )
        app.gymRepository.finishWorkout(historyId)
        val activeWorkoutId = app.gymRepository.startWorkout("Full Body")
        val activeExerciseId = app.gymRepository.addExerciseToWorkout(activeWorkoutId, exerciseId)
        app.gymRepository.addSet(activeExerciseId, WorkoutSetDraft(weight = 26.0, reps = 8))
        app.routineRepository.createRoutine(
            RoutineDraft(
                name = "Three Day Foundation",
                days = listOf(
                    RoutineDayDraft("Day A", listOf(RoutineExerciseDraft(exerciseId))),
                ),
            ),
        )
        SeededCatalogData(activeTrackId)
    }

    private fun launch() = launchMainActivity(
        Intent(app, MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true),
    )

    private fun waitForHome(expectedText: String = "Build Your Day") {
        compose.waitUntil(15_000L) {
            compose.onAllNodesWithText(expectedText).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(expectedText).assertIsDisplayed()
    }

    private fun openPrimary(label: String) {
        compose.onNodeWithContentDescription("$label tab").performClick()
        compose.waitForIdle()
    }

    private fun selectTag(tag: String) {
        compose.onNodeWithTag(tag).performClick()
        compose.waitForIdle()
    }

    private data class SeededCatalogData(val activeTrackId: Long)
}
