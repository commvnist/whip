package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.ElapsedDisplayFormat
import com.whip.app.domain.ElapsedDisplayUnit
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.GoalType
import com.whip.app.domain.GymMachineDraft
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.RecurrenceRule
import com.whip.app.domain.RecurrenceUnit
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
import org.junit.Assert.assertTrue
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
            captureTrackPages(seeded.activeTrackId, seeded.archivedTrackId)
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

    @Test
    fun captureGymCategoryAllocationCatalog() {
        seedRepresentativeData()
        launch().use {
            waitForHome("Plan the week")
            compose.onNodeWithContentDescription("Open Settings").performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("settings-section-Planning & Units").performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("settings-list").performScrollToNode(
                androidx.compose.ui.test.hasText("Overlapping category allocation"),
            )
            compose.onNodeWithContentDescription(
                "Overlapping category allocation: Split contribution",
            ).performClick()
            compose.onNodeWithContentDescription(
                "Overlapping category allocation option: Full contribution",
            ).assertIsDisplayed()
            compose.waitForIdle()
            captureVisualCatalogSurface("gym.category-allocation")
        }
    }

    private fun captureTaskPages() {
        openPrimary("Tasks")
        captureVisualCatalogSurface("tasks.today.populated")
        compose.onNodeWithContentDescription("Expand task Plan the week").performClick()
        compose.waitForIdle()
        captureVisualCatalogSurface("tasks.today.expanded")
        compose.onNodeWithContentDescription("Collapse task Plan the week").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Filter & Sort Tasks").performClick()
        compose.onNodeWithTag("task-filter-query").assertIsDisplayed()
        captureVisualCatalogSurface("tasks.filter")
        compose.onNodeWithTag("task-filter-query").performTextReplacement("Plan")
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Query: Plan").assertIsDisplayed()
        compose.onNodeWithText("Plan the week").assertIsDisplayed()
        captureVisualCatalogSurface("tasks.today.filtered")
        compose.onNodeWithContentDescription("Filter & Sort Tasks").performClick()
        compose.onNodeWithText("Reset").performClick()
        compose.onNodeWithText("Done").performClick()
        compose.onAllNodesWithText("Query: Plan").assertCountEquals(0)
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
        compose.onNodeWithContentDescription("Expand habit Morning medication").performClick()
        compose.waitForIdle()
        captureVisualCatalogSurface("habits.today.expanded")
        compose.onNodeWithContentDescription("Collapse habit Morning medication").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("More Habit Actions").performClick()
        compose.onNodeWithText("Browse Templates").assertIsDisplayed()
        compose.waitForIdle()
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
        compose.onNodeWithContentDescription("Expand goal Sober").performClick()
        compose.waitForIdle()
        captureVisualCatalogSurface("goals.active.expanded")
        compose.onNodeWithContentDescription("Collapse goal Sober").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("More Goal Actions").performClick()
        compose.onNodeWithTag("goal-browse-templates-menu-action").assertIsDisplayed()
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

    private fun captureTrackPages(activeTrackId: Long, archivedTrackId: Long) {
        openPrimary("Tracks")
        captureVisualCatalogSurface("tracks.all.populated")
        compose.onNodeWithContentDescription("Expand Track Reading Log").performClick()
        compose.waitForIdle()
        captureVisualCatalogSurface("tracks.all.expanded")
        compose.onNodeWithContentDescription("Collapse Track Reading Log").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("More Track Options").performClick()
        compose.onNodeWithText("Select Tracks").assertIsDisplayed()
        compose.waitForIdle()
        captureVisualCatalogSurface("tracks.reorder.menu")
        selectTag("track-workspace-destination-Activity")
        captureVisualCatalogSurface("tracks.activity.populated")
        compose.onNodeWithContentDescription("More Actions for The Dispossessed").performClick()
        compose.onNodeWithText("Open Track").assertIsDisplayed()
        compose.waitForIdle()
        captureVisualCatalogSurface("tracks.activity.menu")
        selectTag("track-workspace-destination-Archived")
        captureVisualCatalogSurface("tracks.archived.populated")
        compose.onNodeWithTag("track-card-$archivedTrackId").performClick()
        compose.waitForIdle()
        compose.onAllNodesWithContentDescription("Edit Entry Archived mood").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("More Actions for Archived mood").assertCountEquals(0)
        captureVisualCatalogSurface("tracks.archived.detail.entries")
        selectTag("track-workspace-destination-Insights")
        captureVisualCatalogSurface("tracks.insights.populated")
        selectTag("track-workspace-destination-Tracks")
        compose.onNodeWithTag("track-card-$activeTrackId").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("track-workspace-destination-Insights")
            .assertTextContains("Insights")
        compose.onNodeWithTag("track-destination-Track Insights")
            .assertTextContains("Track Insights")
        captureVisualCatalogSurface("tracks.detail.entries")
        compose.onNodeWithContentDescription("Search Entries in Reading Log").performClick()
        compose.onNodeWithTag("track-entry-search").performTextReplacement("Dispossessed")
        compose.onNodeWithText("The Dispossessed").assertIsDisplayed()
        captureVisualCatalogSurface("tracks.detail.search")
        compose.onNodeWithTag("track-entry-search").performTextReplacement("No such book")
        compose.onNodeWithText("No Matching Entries").assertIsDisplayed()
        captureVisualCatalogSurface("tracks.detail.search.empty")
        compose.onNodeWithContentDescription("Search Entries in Reading Log").performClick()
        compose.onNodeWithContentDescription("More Actions for The Dispossessed").performClick()
        compose.onNodeWithText("Delete Entry").assertIsDisplayed()
        compose.waitForIdle()
        captureVisualCatalogSurface("tracks.entry.menu")
        selectTag("track-destination-Options")
        compose.waitForIdle()
        captureVisualCatalogSurface("tracks.detail.options")
        selectTag("track-destination-Entries")
        compose.onNodeWithText("The Dispossessed").performClick()
        compose.waitForIdle()
        captureVisualCatalogSurface("tracks.entry.details")
        compose.onNodeWithContentDescription("Close Track Entry details").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Filter Entries").performClick()
        compose.waitForIdle()
        captureVisualCatalogSurface("tracks.filter")
        compose.onNodeWithText("Add Condition").performClick()
        compose.waitForIdle()
        captureVisualCatalogSurface("tracks.condition")
        compose.onAllNodesWithText("Cancel")[1].performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()
        selectTag("track-destination-Track Insights")
        captureVisualCatalogSurface("tracks.detail.insights")
    }

    private fun captureGymPages() {
        openPrimary("Gym")
        val nextSetHeight = compose.onNodeWithTag("next-set-focus").fetchSemanticsNode().boundsInRoot.height
        assertTrue(
            "Next-set jump is below Whip's 48 dp target floor: $nextSetHeight px",
            nextSetHeight >= with(compose.density) { 48.dp.toPx() },
        )
        captureVisualCatalogSurface("gym.workout.active")
        selectTag("gym-destination-History")
        captureVisualCatalogSurface("gym.history.populated")
        selectTag("gym-destination-Progress")
        captureVisualCatalogSurface("gym.progress.populated")
        selectTag("gym-destination-Library")
        captureVisualCatalogSurface("gym.library.landing")

        mapOf(
            "Routines" to "Routines",
            "Exercises" to "Exercise Library",
            "Machines" to "Machines",
            "Categories" to "Exercise Categories",
            "Tools" to "Workout Tools",
        ).forEach { (destination, pageTitle) ->
            compose.onNodeWithTag("gym-library-list").performScrollToNode(
                androidx.compose.ui.test.hasText(destination),
            )
            compose.onNodeWithTag("gym-library-$destination").performClick()
            compose.onNodeWithText(pageTitle).assertIsDisplayed()
            compose.waitForIdle()
            captureVisualCatalogSurface(
                surfaceId = "gym.${destination.lowercase()}.populated".replace("tools.populated", "tools"),
                visuallyDistinctFrom = "gym.library.landing",
            )
            if (destination == "Routines") {
                compose.onNodeWithContentDescription("More options for routine Three Day Foundation").performClick()
                compose.onNodeWithText("Duplicate").assertIsDisplayed()
                compose.waitForIdle()
                captureVisualCatalogSurface("gym.routine.menu")
            }
            if (destination == "Machines") {
                compose.onNodeWithContentDescription("More options for Home Cable Stack").performClick()
                compose.onNodeWithText("New Configuration Version").assertIsDisplayed()
                compose.waitForIdle()
                captureVisualCatalogSurface("gym.machine.menu")
            }
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
        app.taskRepository.create(
            TaskDraft(
                "Plan the week",
                scheduleKind = ScheduleKind.Recurring,
                recurrence = RecurrenceRule(
                    unit = RecurrenceUnit.Weeks,
                    weekdays = setOf(today.dayOfWeek, today.plusDays(3).dayOfWeek),
                    startDate = today,
                ),
            ),
        )
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
        val elapsedGoalId = app.goalRepository.create(
            GoalDraft(
                name = "Sober",
                icon = "🌱",
                type = GoalType.ElapsedSince,
                startDate = today.minusDays(430),
                elapsedStartMillis = app.clock.now().minusSeconds(430L * 86_400L + 5L * 3_600L + 34L * 60L).toEpochMilli(),
                elapsedDisplay = ElapsedDisplayFormat.selected(
                    ElapsedDisplayUnit.Years,
                    ElapsedDisplayUnit.Months,
                    ElapsedDisplayUnit.Weeks,
                    ElapsedDisplayUnit.Days,
                    ElapsedDisplayUnit.Hours,
                    ElapsedDisplayUnit.Minutes,
                ),
            ),
        )
        app.goalRepository.setPinned(elapsedGoalId, true)
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
        val archivedPreparation = checkNotNull(app.trackRepository.prepareEntryCreate(archivedTrackId))
        val archivedPrimaryField = archivedPreparation.form.fields.single()
        app.trackRepository.addEntry(
            archivedPreparation.request,
            TrackEntryDraft(
                entryDate = today.minusDays(1),
                values = mapOf(archivedPrimaryField.uuid to TrackValueDraft(textValue = "Archived mood")),
            ),
        )
        app.trackRepository.setArchived(archivedTrackId, true)

        val strengthCategoryId = app.gymRepository.createCategory("Strength")
        val exerciseId = app.gymRepository.createExercise(
            ExerciseDraft(
                name = "Goblet Squat",
                defaultGraphMetric = "MaxWeight",
                categoryIds = setOf(strengthCategoryId),
            ),
        )
        app.gymRepository.createMachine(
            GymMachineDraft(
                exerciseId = exerciseId,
                name = "Home Cable Stack",
                details = "Compact selectorized stack",
                availableLoads = listOf(5.0, 10.0, 15.0, 20.0),
            ),
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
        SeededCatalogData(activeTrackId, archivedTrackId)
    }

    private fun launch() = launchMainActivity(
        Intent(app, MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true),
    )

    private fun waitForHome(expectedText: String = "Build Your Day") {
        compose.waitUntil(15_000L) {
            compose.onAllNodesWithTag("home-list").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("home-list").performScrollToNode(
            androidx.compose.ui.test.hasText(expectedText),
        )
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

    private data class SeededCatalogData(val activeTrackId: Long, val archivedTrackId: Long)
}
