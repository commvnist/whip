package com.whip.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.Goal
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalDirection
import com.whip.app.domain.GoalMilestone
import com.whip.app.domain.GoalPaceType
import com.whip.app.domain.GoalProjection
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.GoalType
import com.whip.app.domain.ElapsedDisplayUnit
import com.whip.app.domain.Habit
import com.whip.app.domain.HabitChecklistItem
import com.whip.app.domain.HabitDayProgress
import com.whip.app.domain.HabitDayState
import com.whip.app.domain.HabitEndType
import com.whip.app.domain.HabitScheduleType
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.RecurrenceRule
import com.whip.app.domain.RecurrenceUnit
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.ScheduledSubtask
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.TargetComparison
import com.whip.app.domain.TargetPeriod
import com.whip.app.domain.TaskProgressDisplay
import com.whip.app.domain.TaskStep
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.WhipTask
import com.whip.app.ui.GoalCard
import com.whip.app.ui.HabitActivityGrid
import com.whip.app.ui.HabitAreaContent
import com.whip.app.ui.HabitDestination
import com.whip.app.ui.HabitProgressCard
import com.whip.app.ui.HabitUiState
import com.whip.app.ui.HabitViewModel
import com.whip.app.ui.DestinationTabBar
import com.whip.app.ui.LocalCompactItemLayout
import com.whip.app.ui.LocalCompactItemExpansionState
import com.whip.app.ui.TaskRow
import com.whip.app.ui.rememberCompactItemExpansionState
import com.whip.app.ui.theme.WhipTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductivityCardDesignUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun tasksHabitsAndGoalsShareIdentityActionAndEditColumns() {
        val date = LocalDate.of(2026, 8, 24)
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TaskRow(
                        item = ScheduledTask(
                            task = WhipTask(
                                id = 1,
                                title = "Read report",
                                notes = "",
                                icon = "📖",
                                scheduleKind = ScheduleKind.Once,
                                date = date,
                                recurrence = null,
                                timeMinutes = null,
                                reminderEnabled = false,
                                archived = false,
                                completedAtMillis = null,
                                createdAtMillis = 1,
                                updatedAtMillis = 1,
                            ),
                            originalDate = date,
                            scheduledDate = date,
                        ),
                        completed = false,
                        onComplete = {},
                        onOpenActions = {},
                        onEdit = {},
                    )
                    HabitProgressCard(
                        item = HabitDayProgress(
                            habit = sampleHabit(date),
                            date = date,
                            scheduled = true,
                            value = 0.0,
                            status = null,
                            successful = false,
                            checklistItems = emptyList(),
                            streak = 0,
                            completionRate = 0.0,
                            dayState = HabitDayState.Pending,
                        ),
                        onOpen = {},
                        onEdit = {},
                        onQuick = {},
                        onDecrement = {},
                        onUndo = {},
                        onUndoSkip = {},
                        onChecklist = { _, _, _, _ -> },
                    )
                    GoalCard(
                        projection = GoalProjection(
                            goal = sampleGoal(date),
                            currentValue = null,
                            progress = null,
                            deltaFromBaseline = null,
                            expectedProgress = null,
                            paceDelta = null,
                            forecastDate = null,
                            onPace = null,
                            milestones = emptyList(),
                            entries = emptyList(),
                        ),
                        onOpen = {},
                        onEdit = {},
                        onRecord = {},
                        onResetElapsed = {},
                        onToggleMilestone = { _, _ -> },
                    )
                }
            }
        }

        val identityLefts = listOf("task-icon-1", "habit-icon-2", "goal-icon-3").map(::left)
        val actionRights = listOf(
            "task-primary-action-1",
            "habit-primary-action-2",
            "goal-primary-action-3",
        ).map(::right)
        val actionLefts = listOf(
            "task-primary-action-1",
            "habit-primary-action-2",
            "goal-primary-action-3",
        ).map(::left)
        val editLefts = listOf("task-edit-action-1", "habit-edit-action-2", "goal-edit-action-3").map(::left)
        val editRights = listOf("task-edit-action-1", "habit-edit-action-2", "goal-edit-action-3").map(::right)

        identityLefts.forEach { assertEquals(identityLefts.first(), it, 0.5f) }
        actionRights.forEach { assertEquals(actionRights.first(), it, 0.5f) }
        assertTrue(identityLefts.first() < actionRights.first())
        editLefts.zip(editRights).zip(actionLefts).forEach { (edit, actionLeft) ->
            assertTrue(edit.first < edit.second)
            assertTrue(edit.second <= actionLeft)
        }
    }

    @Test
    fun pastScheduledBadgeDoesNotCollapseTheTaskTitleInANarrowPane() {
        val date = LocalDate.of(2026, 8, 24)
        val title = "Prepare the quarterly planning notes"
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                Column(Modifier.width(340.dp).padding(12.dp)) {
                    TaskRow(
                        item = ScheduledTask(
                            task = WhipTask(
                                id = 4,
                                title = title,
                                notes = "",
                                icon = "📝",
                                scheduleKind = ScheduleKind.Once,
                                date = date,
                                recurrence = null,
                                timeMinutes = null,
                                reminderEnabled = false,
                                archived = false,
                                completedAtMillis = null,
                                createdAtMillis = 1,
                                updatedAtMillis = 1,
                                areaId = "main",
                                area = "Main",
                            ),
                            originalDate = date,
                            scheduledDate = date,
                            isPastScheduledDate = true,
                        ),
                        completed = false,
                        onComplete = {},
                        onOpenActions = {},
                        onEdit = {},
                    )
                }
            }
        }

        val titleNode = compose.onNodeWithText(title, useUnmergedTree = true).assertIsDisplayed()
        val badgeNode = compose.onNodeWithText("Past Scheduled Date", useUnmergedTree = true).assertIsDisplayed()
        val titleBounds = titleNode.getUnclippedBoundsInRoot()
        val badgeBounds = badgeNode.getUnclippedBoundsInRoot()

        assertTrue(
            "The overdue badge must be below the complete title lane: title=$titleBounds badge=$badgeBounds",
            titleBounds.bottom <= badgeBounds.top,
        )
        assertTrue("The title must retain a usable width", titleBounds.right - titleBounds.left >= 100.dp)
    }

    @Test
    fun scheduledDateAndRecurrenceReflowWithoutTruncationInStandardOrCompactCards() {
        val date = LocalDate.of(2026, 8, 27)
        val compact = mutableStateOf(false)
        val item = ScheduledTask(
            task = WhipTask(
                id = 18,
                title = "Review treatment plan",
                notes = "",
                icon = "📋",
                scheduleKind = ScheduleKind.Recurring,
                date = null,
                recurrence = RecurrenceRule(
                    unit = RecurrenceUnit.Weeks,
                    weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
                    startDate = date,
                ),
                timeMinutes = null,
                reminderEnabled = false,
                archived = false,
                completedAtMillis = null,
                createdAtMillis = 1,
                updatedAtMillis = 1,
                areaId = "main",
                area = "Main",
            ),
            originalDate = date,
            scheduledDate = date,
        )
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                val expansionState = rememberCompactItemExpansionState()
                CompositionLocalProvider(
                    LocalCompactItemLayout provides compact.value,
                    LocalCompactItemExpansionState provides expansionState,
                ) {
                    Column(Modifier.width(300.dp).padding(12.dp)) {
                        TaskRow(item, false, {}, {}, {})
                    }
                }
            }
        }

        assertSchedulingMetadataFits("standard")

        compose.runOnIdle { compact.value = true }
        compose.waitForIdle()

        assertSchedulingMetadataFits("compact")
    }

    @Test
    fun compactTaskRowsReflowNotesAndSubtaskProgressWithoutLosingActions() {
        val date = LocalDate.of(2026, 8, 24)
        val compact = mutableStateOf(false)
        var completionRequested = false
        val notes = "Read the decision notes before the meeting."
        val item = ScheduledTask(
            task = WhipTask(
                id = 5,
                title = "Review quarterly plan",
                notes = notes,
                icon = "📖",
                scheduleKind = ScheduleKind.Once,
                date = date,
                recurrence = null,
                timeMinutes = null,
                reminderEnabled = false,
                archived = false,
                completedAtMillis = null,
                createdAtMillis = 1,
                updatedAtMillis = 1,
                showSubtaskProgress = true,
                progressDisplay = TaskProgressDisplay.Both,
            ),
            originalDate = date,
            scheduledDate = date,
            subtasks = listOf(
                ScheduledSubtask(
                    step = TaskStep(51, 5, "Read the forecast", 0, createdAtMillis = 1, updatedAtMillis = 1),
                    completed = true,
                    completedAtMillis = 1,
                    title = "Read the forecast",
                ),
                ScheduledSubtask(
                    step = TaskStep(52, 5, "Confirm the risks", 1, createdAtMillis = 1, updatedAtMillis = 1),
                    completed = false,
                    completedAtMillis = null,
                    title = "Confirm the risks",
                ),
            ),
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                val expansionState = rememberCompactItemExpansionState()
                CompositionLocalProvider(
                    LocalCompactItemLayout provides compact.value,
                    LocalCompactItemExpansionState provides expansionState,
                ) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        TaskRow(item, false, { completionRequested = true }, {}, {})
                    }
                }
            }
        }

        val standardHeight = contentDescriptionHeight("Open task details for Review quarterly plan")
        compose.onAllNodesWithText(notes).assertCountEquals(1)
        compose.onNodeWithText("1/2 · 50%").assertIsDisplayed()

        compose.runOnIdle { compact.value = true }
        compose.waitForIdle()

        val compactHeight = contentDescriptionHeight("Open task details for Review quarterly plan")
        assertTrue("Collapsed compact row should be materially denser: $standardHeight vs $compactHeight", compactHeight <= standardHeight - 32.dp)
        assertTrue("Compact primary action must retain a 48 dp target", height("task-primary-action-5") >= 48.dp)
        assertTrue("Compact expansion action must retain a 48 dp target", height("task-expand-5") >= 48.dp)
        compose.onAllNodesWithText(notes).assertCountEquals(0)
        compose.onAllNodesWithText("1/2 · 50%").assertCountEquals(0)

        compose.onNodeWithContentDescription("Complete task Review quarterly plan").performClick()
        compose.runOnIdle { assertTrue(completionRequested) }
        compose.onAllNodesWithText(notes).assertCountEquals(0)

        compose.onNodeWithTag("task-expand-5", useUnmergedTree = true).performClick()
        compose.onNodeWithText(notes).assertIsDisplayed()
        compose.onNodeWithText("1/2 · 50%").assertIsDisplayed()
        assertTrue("Expanded compact edit action must retain a 48 dp target", height("task-edit-action-5") >= 48.dp)
    }

    @Test
    fun compactRowsExpandIndependentlyAndWorkspaceTabChangesCollapseThem() {
        val date = LocalDate.of(2026, 8, 24)
        val firstNotes = "First expanded details"
        val secondNotes = "Second expanded details"
        val first = ScheduledTask(
            task = WhipTask(15, "First compact task", firstNotes, ScheduleKind.Once, date, null, null, false, false, null, 1, 1),
            originalDate = date,
            scheduledDate = date,
        )
        val second = ScheduledTask(
            task = WhipTask(16, "Second compact task", secondNotes, ScheduleKind.Once, date, null, null, false, false, null, 1, 1),
            originalDate = date,
            scheduledDate = date,
        )
        val selectedTab = mutableStateOf("Today")
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                val expansionState = rememberCompactItemExpansionState()
                CompositionLocalProvider(
                    LocalCompactItemLayout provides true,
                    LocalCompactItemExpansionState provides expansionState,
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        DestinationTabBar(
                            selected = selectedTab.value,
                            destinations = listOf("Today", "Upcoming"),
                            onSelect = { selectedTab.value = it },
                            label = { it },
                            testTagPrefix = "compact-test-tab",
                        )
                        if (selectedTab.value == "Today") {
                            Column(
                                Modifier.fillMaxWidth().padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                TaskRow(first, false, {}, {}, {})
                                TaskRow(second, false, {}, {}, {})
                            }
                        }
                    }
                }
            }
        }

        compose.onNodeWithTag("task-expand-15", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("task-expand-16", useUnmergedTree = true).performClick()
        compose.onNodeWithText(firstNotes).assertIsDisplayed()
        compose.onNodeWithText(secondNotes).assertIsDisplayed()
        compose.onNodeWithContentDescription("Collapse task First compact task").assertIsDisplayed()
        compose.onNodeWithContentDescription("Collapse task Second compact task").assertIsDisplayed()

        compose.onNodeWithTag("compact-test-tab-Upcoming").performClick()
        compose.onNodeWithTag("compact-test-tab-Today").performClick()

        compose.onAllNodesWithText(firstNotes).assertCountEquals(0)
        compose.onAllNodesWithText(secondNotes).assertCountEquals(0)
        compose.onNodeWithContentDescription("Expand task First compact task").assertIsDisplayed()
        compose.onNodeWithContentDescription("Expand task Second compact task").assertIsDisplayed()
    }

    @Test
    fun compactHabitAndGoalRowsKeepDetailsAndInlineActions() {
        val date = LocalDate.of(2026, 8, 24)
        val compact = mutableStateOf(false)
        val habit = sampleHabit(date).copy(
            id = 6,
            name = "Drink water",
            trackingMode = HabitTrackingMode.Count,
            comparison = TargetComparison.AtLeast,
            targetMin = 8.0,
            quickIncrement = 1.0,
        )
        val goal = sampleGoal(date).copy(id = 7, name = "Read 50 books", description = "Finish the annual reading list.")
        var quickValue: Double? = null
        var decremented = false
        var setValue = false
        var undone = false
        var goalLogged = false
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                val expansionState = rememberCompactItemExpansionState()
                CompositionLocalProvider(
                    LocalCompactItemLayout provides compact.value,
                    LocalCompactItemExpansionState provides expansionState,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        HabitProgressCard(
                            item = HabitDayProgress(
                                habit = habit,
                                date = date,
                                scheduled = true,
                                value = 2.0,
                                status = null,
                                successful = false,
                                checklistItems = emptyList(),
                                streak = 3,
                                completionRate = 0.5,
                                dayState = HabitDayState.Pending,
                            ),
                            onOpen = {},
                            onEdit = {},
                            onQuick = {},
                            onQuickValue = { quickValue = it },
                            onSetValue = { setValue = true },
                            onDecrement = { decremented = true },
                            onUndo = { undone = true },
                            canUndo = true,
                            onUndoSkip = {},
                            onChecklist = { _, _, _, _ -> },
                        )
                        GoalCard(
                            projection = GoalProjection(
                                goal = goal,
                                currentValue = 25.0,
                                progress = 0.5,
                                deltaFromBaseline = 25.0,
                                expectedProgress = null,
                                paceDelta = null,
                                forecastDate = null,
                                onPace = null,
                                milestones = emptyList(),
                                entries = emptyList(),
                            ),
                            onOpen = {},
                            onEdit = {},
                            onRecord = { goalLogged = true },
                            onResetElapsed = {},
                            onToggleMilestone = { _, _ -> },
                        )
                    }
                }
            }
        }

        val standardGoalHeight = height("goal-card-7")
        compose.onNodeWithText("50% complete").assertIsDisplayed()
        compose.onNodeWithText("Finish the annual reading list.").assertIsDisplayed()

        compose.runOnIdle { compact.value = true }
        compose.waitForIdle()

        assertTrue(height("goal-card-7") < standardGoalHeight)
        assertTrue(height("goal-primary-action-7") >= 48.dp)
        compose.onNodeWithText("2/8", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("50% complete").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Current: 25", substring = true).assertCountEquals(0)
        compose.onAllNodesWithText("Finish the annual reading list.").assertCountEquals(0)

        compose.onNodeWithText("+1").performScrollTo().performClick()
        compose.onNodeWithText("Log").performScrollTo().performClick()
        compose.onNodeWithTag("habit-expand-6", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithText("2 / 8", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("−1").performScrollTo().performClick()
        compose.onNodeWithText("Set").performScrollTo().performClick()
        compose.onNodeWithText("Undo").performScrollTo().performClick()

        compose.onNodeWithTag("goal-expand-7", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithText("2 / 8", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Collapse habit Drink water").assertIsDisplayed()
        compose.onNodeWithContentDescription("Collapse goal Read 50 books").assertIsDisplayed()
        compose.onNodeWithText("Current: 25", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Finish the annual reading list.").performScrollTo().assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(1.0, quickValue)
            assertTrue(decremented)
            assertTrue(setValue)
            assertTrue(undone)
            assertTrue(goalLogged)
        }
    }

    @Test
    fun compactChecklistHabitKeepsParentAndEachSubItemInteractive() {
        val date = LocalDate.of(2026, 8, 24)
        var parentCompletionRequested = false
        var checklistUpdate: List<Any>? = null
        val habit = sampleHabit(date).copy(
            name = "Medication",
            trackingMode = HabitTrackingMode.Checklist,
            autoCompleteFromItems = false,
        )
        val checkOffHabit = sampleHabit(date).copy(
            id = 22,
            uuid = "habit-22",
            metricId = "metric-habit-22",
            name = "Creatine",
            comparison = TargetComparison.AtLeast,
            targetMin = 1.0,
        )
        val checklistItems = (1L..3L).map { id ->
            HabitChecklistItem(
                id = id,
                uuid = "item-$id",
                habitId = habit.id,
                name = "Medication $id",
                position = id.toInt() - 1,
                archived = false,
                createdAtMillis = 1,
                updatedAtMillis = 1,
            ) to (id < 3)
        }

        compose.setContent {
            WhipTheme(dynamicColor = false) {
                val expansionState = rememberCompactItemExpansionState()
                CompositionLocalProvider(
                    LocalCompactItemLayout provides true,
                    LocalCompactItemExpansionState provides expansionState,
                ) {
                    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)) {
                        HabitProgressCard(
                            item = HabitDayProgress(
                                habit = habit,
                                date = date,
                                scheduled = true,
                                value = 1.0,
                                status = null,
                                successful = false,
                                checklistItems = checklistItems,
                                streak = 0,
                                completionRate = 0.0,
                                dayState = HabitDayState.Pending,
                            ),
                            onOpen = {},
                            onEdit = {},
                            onQuick = { parentCompletionRequested = true },
                            onDecrement = {},
                            onUndo = {},
                            onUndoSkip = {},
                            onChecklist = { habitId, itemId, localDate, checked ->
                                checklistUpdate = listOf(habitId, itemId, localDate, checked)
                            },
                        )
                        HabitProgressCard(
                            item = HabitDayProgress(
                                habit = checkOffHabit,
                                date = date,
                                scheduled = true,
                                value = 1.0,
                                status = null,
                                successful = true,
                                checklistItems = emptyList(),
                                streak = 1,
                                completionRate = 1.0,
                                dayState = HabitDayState.Completed,
                            ),
                            onOpen = {},
                            onEdit = {},
                            onQuick = {},
                            onDecrement = {},
                            onUndo = {},
                            onUndoSkip = {},
                            onChecklist = { _, _, _, _ -> },
                        )
                    }
                }
            }
        }

        compose.onNodeWithText("2/3 items", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Done · 1 day streak").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("2 / 3 items complete").assertCountEquals(0)
        checklistItems.forEach { (item, _) -> compose.onAllNodesWithText(item.name).assertCountEquals(0) }
        compose.onNodeWithContentDescription("Check off habit Medication").performClick()
        compose.runOnIdle { assertTrue(parentCompletionRequested) }
        compose.onAllNodesWithText("2 / 3 items complete").assertCountEquals(0)

        compose.onNodeWithTag("habit-expand-${habit.id}", useUnmergedTree = true).performClick()
        compose.onNodeWithText("2 / 3 items complete").assertIsDisplayed()
        compose.onAllNodesWithText("1").assertCountEquals(0)
        checklistItems.forEach { (item, _) -> compose.onNodeWithText(item.name).assertIsDisplayed() }
        val checklistHeight = height("habit-checklist-item-3")
        assertTrue("Habit checklist row must remain 48 dp; was $checklistHeight", checklistHeight >= 47.99.dp)
        val completedText = compose.onNodeWithTag("habit-checklist-text-1", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val completedCheck = compose.onNodeWithTag("habit-checklist-check-1", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue("Habit checklist completion must trail its text", completedCheck.left >= completedText.right + 8.dp)
        assertTrue("Habit checklist completion target must remain 48 dp", completedCheck.bottom - completedCheck.top >= 48.dp)
        compose.onNodeWithTag("habit-checklist-item-3", useUnmergedTree = true).performClick()
        compose.runOnIdle { assertEquals(listOf(habit.id, 3L, date, true), checklistUpdate) }

        compose.onNodeWithTag("habit-expand-${checkOffHabit.id}", useUnmergedTree = true).performScrollTo().performClick()
        compose.onAllNodesWithText("1 / 1", substring = true).assertCountEquals(0)
        compose.onAllNodesWithText("1").assertCountEquals(0)
    }

    @Test
    fun compactHabitStateMatrixKeepsTimerSkipAndSyncedInformation() {
        val date = LocalDate.of(2026, 8, 24)
        var timerRequested = false
        var undoSkipRequested = false
        val duration = sampleHabit(date).copy(
            id = 11,
            name = "Meditate",
            trackingMode = HabitTrackingMode.Duration,
            dimension = UnitDimension.Duration,
            unitId = "second",
        )
        val skipped = sampleHabit(date).copy(id = 12, name = "Evening walk")
        val synced = sampleHabit(date).copy(id = 13, name = "Daily steps", sourceMetricId = "health-steps")

        compose.setContent {
            WhipTheme(dynamicColor = false) {
                CompositionLocalProvider(LocalCompactItemLayout provides true) {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HabitProgressCard(
                            item = HabitDayProgress(duration, date, true, 90.0, null, false, emptyList(), 2, 0.4, dayState = HabitDayState.Pending),
                            onOpen = {}, onEdit = {}, onQuick = { timerRequested = true }, onDecrement = {}, onUndo = {}, onUndoSkip = {},
                            onChecklist = { _, _, _, _ -> },
                        )
                        HabitProgressCard(
                            item = HabitDayProgress(skipped, date, true, 0.0, null, null, emptyList(), 5, 0.8, dayState = HabitDayState.Skipped),
                            onOpen = {}, onEdit = {}, onQuick = {}, onDecrement = {}, onUndo = {}, onUndoSkip = { undoSkipRequested = true },
                            onChecklist = { _, _, _, _ -> },
                        )
                        HabitProgressCard(
                            item = HabitDayProgress(synced, date, true, 8_000.0, null, true, emptyList(), 7, 0.9, dayState = HabitDayState.Completed),
                            onOpen = {}, onEdit = {}, onQuick = {}, onDecrement = {}, onUndo = {}, onUndoSkip = {},
                            onChecklist = { _, _, _, _ -> },
                        )
                    }
                }
            }
        }

        compose.onNodeWithText("Start").performScrollTo().performClick()
        compose.onNodeWithText("Skipped · streak protected").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Skipped Today · Streak Protected").assertCountEquals(0)
        compose.onNodeWithText("Undo").performScrollTo().performClick()
        compose.onNodeWithText("Synced · Health Connect").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Read-only source: Health Connect", substring = true).assertCountEquals(0)
        compose.onNodeWithTag("habit-expand-13", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithText("Read-only source: Health Connect", substring = true).performScrollTo().assertIsDisplayed()
        compose.runOnIdle {
            assertTrue(timerRequested)
            assertTrue(undoSkipRequested)
        }
    }

    @Test
    fun compactGoalKeepsElapsedTimerResetAndMilestoneControls() {
        val date = LocalDate.of(2026, 8, 24)
        val nowMillis = 1_800_000_000_000L
        val twoDaysMillis = 2L * 24L * 60L * 60L * 1_000L
        var resetRequested = false
        var milestoneUpdate: Pair<Long, Boolean>? = null
        val elapsedGoal = sampleGoal(date).copy(
            id = 8,
            name = "Days since smoking",
            type = GoalType.ElapsedSince,
            elapsedStartMillis = nowMillis - twoDaysMillis,
            elapsedDisplayUnit = ElapsedDisplayUnit.Days,
        )
        val milestoneGoal = sampleGoal(date).copy(
            id = 9,
            name = "Launch the product",
            type = GoalType.WeightedMilestones,
            description = "Complete every launch gate.",
        )
        val milestone = GoalMilestone(91, "milestone-91", 9, "Publish the release", 0, 1.0, false, null, "Celebrate", 1, 1)

        compose.setContent {
            WhipTheme(dynamicColor = false) {
                val expansionState = rememberCompactItemExpansionState()
                CompositionLocalProvider(
                    LocalCompactItemLayout provides true,
                    LocalCompactItemExpansionState provides expansionState,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GoalCard(
                            projection = GoalProjection(elapsedGoal, null, null, null, null, null, null, null, emptyList(), emptyList()),
                            onOpen = {},
                            onEdit = {},
                            onRecord = {},
                            onResetElapsed = { resetRequested = true },
                            onToggleMilestone = { _, _ -> },
                            nowMillis = nowMillis,
                        )
                        GoalCard(
                            projection = GoalProjection(milestoneGoal, null, 0.0, null, null, null, null, null, listOf(milestone), emptyList()),
                            onOpen = {},
                            onEdit = {},
                            onRecord = {},
                            onResetElapsed = {},
                            onToggleMilestone = { id, completed -> milestoneUpdate = id to completed },
                        )
                    }
                }
            }
        }

        compose.onNodeWithText("2 days").assertIsDisplayed()
        compose.onNodeWithContentDescription("Expand goal Days since smoking").assertExists()
        val resetLabelHeight = compose.onNodeWithText("Reset", useUnmergedTree = true)
            .getUnclippedBoundsInRoot().let { it.bottom - it.top }
        assertTrue("Reset must remain on one line in the compact action lane: $resetLabelHeight", resetLabelHeight <= 24.dp)
        assertTrue(height("goal-primary-action-8") >= 48.dp)
        compose.onNodeWithText("Reset").performClick()
        compose.onNodeWithText("0/1 milestones").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Publish the release").assertCountEquals(0)
        compose.onAllNodesWithText("Celebrate").assertCountEquals(0)

        compose.onNodeWithTag("goal-expand-9", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithText("2 days").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Expand goal Days since smoking").assertExists()
        compose.onNodeWithContentDescription("Collapse goal Launch the product").assertExists()
        compose.onNodeWithText("Publish the release").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Celebrate").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Complete every launch gate.").performScrollTo().assertIsDisplayed()
        assertTrue(height("goal-milestone-91") >= 48.dp)
        compose.onNodeWithTag("goal-milestone-91", useUnmergedTree = true).performScrollTo().performClick()
        compose.runOnIdle {
            assertTrue(resetRequested)
            assertEquals(91L to true, milestoneUpdate)
        }
    }

    @Test
    fun compactCardsKeepLongTextReadableAtTwoHundredPercentFontScale() {
        val date = LocalDate.of(2026, 8, 24)
        val title = "Prepare the quarterly medication and care coordination review"
        val largeText = Density(compose.density.density, fontScale = 2f)
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides largeText,
                LocalCompactItemLayout provides true,
            ) {
                WhipTheme(dynamicColor = false) {
                    Column(
                        Modifier.width(340.dp).verticalScroll(rememberScrollState()).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TaskRow(
                            item = ScheduledTask(
                                task = WhipTask(10, title, "Keep this context visible.", ScheduleKind.Once, date, null, null, false, false, null, 1, 1, areaId = "health", area = "Health"),
                                originalDate = date,
                                scheduledDate = date,
                            ),
                            completed = false,
                            onComplete = {},
                            onOpenActions = {},
                            onEdit = {},
                        )
                        GoalCard(
                            projection = GoalProjection(
                                goal = sampleGoal(date).copy(
                                    id = 11,
                                    name = "Reset elapsed goal",
                                    type = GoalType.ElapsedSince,
                                    elapsedStartMillis = 1_800_000_000_000L - 86_400_000L,
                                    elapsedDisplayUnit = ElapsedDisplayUnit.Days,
                                ),
                                currentValue = null,
                                progress = null,
                                deltaFromBaseline = null,
                                expectedProgress = null,
                                paceDelta = null,
                                forecastDate = null,
                                onPace = null,
                                milestones = emptyList(),
                                entries = emptyList(),
                            ),
                            onOpen = {},
                            onEdit = {},
                            onRecord = {},
                            onResetElapsed = {},
                            onToggleMilestone = { _, _ -> },
                            nowMillis = 1_800_000_000_000L,
                        )
                    }
                }
            }
        }

        val titleBounds = compose.onNodeWithText(title, useUnmergedTree = true).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("Compact title must retain usable width at 200% text", titleBounds.right - titleBounds.left >= 100.dp)
        compose.onAllNodesWithText("Keep this context visible.").assertCountEquals(0)
        compose.onAllNodesWithText("Health").assertCountEquals(0)
        assertTrue(height("task-primary-action-10") >= 48.dp)
        assertTrue(height("task-expand-10") >= 48.dp)

        compose.onNodeWithTag("task-expand-10", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Keep this context visible.").assertIsDisplayed()
        compose.onNodeWithText("Health").assertIsDisplayed()
        assertTrue(height("task-edit-action-10") >= 48.dp)

        val largeResetLabelHeight = compose.onNodeWithText("Reset", useUnmergedTree = true)
            .performScrollTo()
            .getUnclippedBoundsInRoot().let { it.bottom - it.top }
        assertTrue("Reset must remain on one line at 200% text: $largeResetLabelHeight", largeResetLabelHeight <= 44.dp)
        assertTrue(height("goal-primary-action-11") >= 48.dp)
    }

    @Test
    fun compactExpansionSurvivesSavedStateRestoration() {
        val date = LocalDate.of(2026, 8, 24)
        val notes = "Restored expanded details"
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            WhipTheme(dynamicColor = false) {
                CompositionLocalProvider(LocalCompactItemLayout provides true) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        TaskRow(
                            item = ScheduledTask(
                                task = WhipTask(14, "Persist expansion", notes, ScheduleKind.Once, date, null, null, false, false, null, 1, 1),
                                originalDate = date,
                                scheduledDate = date,
                            ),
                            completed = false,
                            onComplete = {},
                            onOpenActions = {},
                            onEdit = {},
                        )
                    }
                }
            }
        }

        compose.onAllNodesWithText(notes).assertCountEquals(0)
        compose.onNodeWithTag("task-expand-14", useUnmergedTree = true).performClick()
        compose.onNodeWithText(notes).assertIsDisplayed()

        restoration.emulateSavedInstanceStateRestore()

        compose.onNodeWithText(notes).assertIsDisplayed()
        compose.onNodeWithContentDescription("Collapse task Persist expansion").assertIsDisplayed()
    }

    @Test
    fun habitActivityGridExposesOneSpokenDateAndStatePerDay() {
        val today = LocalDate.of(2026, 8, 30)
        val days = (27L downTo 0L).map(today::minusDays)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                HabitActivityGrid(days) { day ->
                    if (day == today) HabitDayState.Completed else HabitDayState.Pending
                }
            }
        }

        compose.onNodeWithTag("habit-activity-grid").assertIsDisplayed()
        val spokenStates = setOf(
            "completed",
            "skipped",
            "missed",
            "below target",
            "pending",
            "paused",
            "not scheduled",
        )
        days.forEach { day ->
            val descriptions = compose
                .onNodeWithTag("habit-activity-day-${day.toEpochDay()}", useUnmergedTree = true)
                .fetchSemanticsNode().config[SemanticsProperties.ContentDescription]
            val dateLabel = day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
            assertEquals("Each activity cell needs one complete spoken label", 1, descriptions.size)
            assertTrue("Activity cell omitted its date: $descriptions", descriptions.single().startsWith("$dateLabel: "))
            assertTrue(
                "Activity cell omitted a recognized state: $descriptions",
                spokenStates.any { descriptions.single().endsWith(": $it") },
            )
        }
    }

    private fun left(tag: String): Float = compose
        .onNodeWithTag(tag, useUnmergedTree = true)
        .getUnclippedBoundsInRoot()
        .left
        .value

    private fun right(tag: String): Float = compose
        .onNodeWithTag(tag, useUnmergedTree = true)
        .getUnclippedBoundsInRoot()
        .right
        .value

    private fun height(tag: String) = compose
        .onNodeWithTag(tag, useUnmergedTree = true)
        .getUnclippedBoundsInRoot()
        .let { it.bottom - it.top }

    private fun contentDescriptionHeight(description: String) = compose
        .onNodeWithContentDescription(description)
        .getUnclippedBoundsInRoot()
        .let { it.bottom - it.top }

    private fun assertSchedulingMetadataFits(mode: String) {
        val container = compose.onNodeWithTag("task-metadata-18", useUnmergedTree = true)
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        listOf("Scheduled · Aug 27, 2026", "Repeats · Mon, Thu").forEach { label ->
            val bounds = compose.onNodeWithText(label, useUnmergedTree = true)
                .assertIsDisplayed()
                .getUnclippedBoundsInRoot()
            assertTrue("$mode metadata must start inside its full-width lane: $bounds vs $container", bounds.left >= container.left)
            assertTrue("$mode metadata must end inside its full-width lane: $bounds vs $container", bounds.right <= container.right)
            assertTrue("$mode metadata must have visible height: $bounds", bounds.bottom - bounds.top > 0.dp)
        }
    }

    private fun sampleHabit(date: LocalDate) = Habit(
        id = 2,
        uuid = "habit-2",
        metricId = "metric-habit-2",
        name = "Read daily",
        notes = "",
        area = "",
        tags = emptyList(),
        icon = "📖",
        trackingMode = HabitTrackingMode.CheckOff,
        dimension = UnitDimension.Count,
        unitId = "count",
        precision = 0,
        comparison = TargetComparison.None,
        targetMin = null,
        targetMax = null,
        targetPeriod = TargetPeriod.Day,
        rollingDays = null,
        scheduleType = HabitScheduleType.Daily,
        scheduleInterval = 1,
        weekdays = emptySet(),
        flexibleTimesPerWeek = null,
        startDate = date,
        endType = HabitEndType.Never,
        endDate = null,
        endValue = null,
        quickIncrement = 1.0,
        quickActions = emptyList(),
        reminderMinutes = emptyList(),
        weekdayReminderMinutes = emptyMap(),
        weekStart = DayOfWeek.MONDAY,
        timerStartedAtMillis = null,
        pinned = false,
        position = 0,
        archived = false,
        paused = false,
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )

    private fun sampleGoal(date: LocalDate) = Goal(
        id = 3,
        uuid = "goal-3",
        metricId = "metric-goal-3",
        name = "Read 50 books",
        description = "",
        area = "",
        tags = emptyList(),
        icon = "📖",
        type = GoalType.ReachValue,
        dimension = UnitDimension.Count,
        unitId = "count",
        precision = 0,
        baseline = 0.0,
        targetMin = 50.0,
        targetMax = null,
        direction = GoalDirection.Increase,
        startDate = date,
        deadline = null,
        aggregation = GoalAggregation.Latest,
        paceType = GoalPaceType.None,
        reminderMinutes = null,
        status = GoalStatus.Active,
        pinned = false,
        position = 0,
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )
}
