package com.whip.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.Goal
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalDirection
import com.whip.app.domain.GoalPaceType
import com.whip.app.domain.GoalProjection
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.GoalType
import com.whip.app.domain.Habit
import com.whip.app.domain.HabitDayProgress
import com.whip.app.domain.HabitDayState
import com.whip.app.domain.HabitEndType
import com.whip.app.domain.HabitScheduleType
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.TargetComparison
import com.whip.app.domain.TargetPeriod
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.WhipTask
import com.whip.app.ui.GoalCard
import com.whip.app.ui.HabitProgressCard
import com.whip.app.ui.TaskRow
import com.whip.app.ui.theme.WhipTheme
import java.time.DayOfWeek
import java.time.LocalDate
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
        val actionLefts = listOf(
            "task-primary-action-1",
            "habit-primary-action-2",
            "goal-primary-action-3",
        ).map(::left)
        val editLefts = listOf("task-edit-action-1", "habit-edit-action-2", "goal-edit-action-3").map(::left)

        identityLefts.forEach { assertEquals(identityLefts.first(), it, 0.5f) }
        actionLefts.forEach { assertEquals(actionLefts.first(), it, 0.5f) }
        editLefts.forEach { assertEquals(editLefts.first(), it, 0.5f) }
        assertTrue(identityLefts.first() < actionLefts.first())
        assertTrue(actionLefts.first() < editLefts.first())
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

    private fun left(tag: String): Float = compose
        .onNodeWithTag(tag, useUnmergedTree = true)
        .getUnclippedBoundsInRoot()
        .left
        .value

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
