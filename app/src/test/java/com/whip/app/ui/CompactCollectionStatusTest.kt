package com.whip.app.ui

import com.whip.app.domain.ElapsedDisplayUnit
import com.whip.app.domain.Goal
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalDirection
import com.whip.app.domain.GoalMilestone
import com.whip.app.domain.GoalPaceType
import com.whip.app.domain.GoalProjection
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.GoalType
import com.whip.app.domain.Habit
import com.whip.app.domain.HabitChecklistItem
import com.whip.app.domain.HabitDayProgress
import com.whip.app.domain.HabitDayState
import com.whip.app.domain.HabitEndType
import com.whip.app.domain.HabitLogStatus
import com.whip.app.domain.HabitScheduleType
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.TargetComparison
import com.whip.app.domain.TargetPeriod
import com.whip.app.domain.UnitDimension
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CompactCollectionStatusTest {
    private val date = LocalDate.of(2026, 8, 25)

    @Test
    fun binaryAndChecklistHabitsUseHumanStatusInsteadOfRawSentinelValues() {
        val checkOff = progress(
            habit = habit(HabitTrackingMode.CheckOff),
            value = 1.0,
            successful = true,
            streak = 1,
            dayState = HabitDayState.Completed,
        )
        val checklistHabit = habit(HabitTrackingMode.Checklist)
        val checklist = progress(
            habit = checklistHabit,
            value = 1.0,
            successful = true,
            streak = 1,
            dayState = HabitDayState.Completed,
            checklistItems = (1L..3L).map { id ->
                HabitChecklistItem(id, "item-$id", checklistHabit.id, "Medication $id", id.toInt(), false, 1, 1) to true
            },
        )

        assertEquals("Done · 1 day streak", checkOff.compactCollectionStatus())
        assertEquals("3/3 items · 1 day streak", checklist.compactCollectionStatus())
    }

    @Test
    fun pausedAndOffScheduleHabitsDescribeAvailabilityInsteadOfLookingPending() {
        val paused = progress(
            habit = habit(HabitTrackingMode.CheckOff).copy(paused = true),
            value = 0.0,
            successful = false,
            streak = 4,
            dayState = HabitDayState.Paused,
        )
        val offSchedule = progress(
            habit = habit(HabitTrackingMode.Count),
            value = 0.0,
            successful = false,
            streak = 4,
            dayState = HabitDayState.NotScheduled,
        )

        assertEquals("Paused · no check-in expected", paused.compactCollectionStatus())
        assertEquals("Not scheduled today", offSchedule.compactCollectionStatus())
    }

    @Test
    fun todaySectionsTreatSkippedAndCompletedHabitsAsFinishedButKeepThemDistinct() {
        val pending = progress(habit(HabitTrackingMode.CheckOff).copy(id = 1), 0.0, false, 0, HabitDayState.Pending)
        val completed = progress(habit(HabitTrackingMode.CheckOff).copy(id = 2), 1.0, true, 1, HabitDayState.Completed)
        val skipped = progress(habit(HabitTrackingMode.CheckOff).copy(id = 3), 0.0, false, 1, HabitDayState.Skipped)

        val sections = listOf(pending, completed, skipped).dailyHabitSections()

        assertEquals(listOf(1L), sections.actionNeeded.map { it.habit.id })
        assertEquals(listOf(2L, 3L), sections.finished.map { it.habit.id })
        assertEquals(false, skipped.isDoneForToday())
        assertEquals(true, skipped.isFinishedForToday())
    }

    @Test
    fun elapsedAndMilestoneGoalsNeverFallBackToMisleadingZeroPercentProgress() {
        val nowMillis = 1_800_000_000_000L
        val elapsed = projection(
            goal = goal().copy(
                type = GoalType.ElapsedSince,
                elapsedStartMillis = nowMillis - 2L * 86_400_000L,
                elapsedDisplayUnit = ElapsedDisplayUnit.Days,
            ),
            progress = null,
        )
        val milestoneGoal = goal().copy(type = GoalType.WeightedMilestones)
        val milestone = GoalMilestone(1, "milestone-1", milestoneGoal.id, "Ship", 0, 1.0, true, null, "Celebrate", 1, 1)

        assertEquals("2 days", elapsed.collectionStatus(nowMillis = nowMillis))
        assertEquals(
            "1/1 milestones",
            projection(milestoneGoal, progress = 0.0, milestones = listOf(milestone)).collectionStatus(nowMillis = nowMillis),
        )
    }

    private fun progress(
        habit: Habit,
        value: Double,
        successful: Boolean,
        streak: Int,
        dayState: HabitDayState,
        checklistItems: List<Pair<HabitChecklistItem, Boolean>> = emptyList(),
    ) = HabitDayProgress(
        habit = habit,
        date = date,
        scheduled = true,
        value = value,
        status = HabitLogStatus.Success,
        successful = successful,
        checklistItems = checklistItems,
        streak = streak,
        completionRate = 1.0,
        dayState = dayState,
    )

    private fun habit(mode: HabitTrackingMode) = Habit(
        id = 1,
        uuid = "habit-1",
        measurementId = "measurement-habit-1",
        name = "Medication",
        notes = "",
        area = "Main",
        tags = emptyList(),
        icon = "💊",
        trackingMode = mode,
        dimension = UnitDimension.Count,
        unitId = "count",
        precision = 0,
        comparison = TargetComparison.AtLeast,
        targetMin = 1.0,
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

    private fun goal() = Goal(
        id = 2,
        uuid = "goal-2",
        measurementId = "measurement-goal-2",
        name = "Quit alcohol",
        description = "",
        area = "Main",
        tags = emptyList(),
        icon = "❤️",
        type = GoalType.ReachValue,
        dimension = UnitDimension.Count,
        unitId = "count",
        precision = 0,
        baseline = 0.0,
        targetMin = 1.0,
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

    private fun projection(
        goal: Goal,
        progress: Double?,
        milestones: List<GoalMilestone> = emptyList(),
    ) = GoalProjection(
        goal = goal,
        currentValue = null,
        progress = progress,
        deltaFromBaseline = null,
        expectedProgress = null,
        paceDelta = null,
        forecastDate = null,
        onPace = null,
        milestones = milestones,
        entries = emptyList(),
    )
}
