package com.whip.app.ui

import com.whip.app.domain.Goal
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalAggregationPeriod
import com.whip.app.domain.GoalDirection
import com.whip.app.domain.GoalPaceType
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.GoalType
import com.whip.app.domain.MetricDefinition
import com.whip.app.domain.MetricValueKind
import com.whip.app.domain.TrackAggregation
import com.whip.app.domain.UnitDimension
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationUxPolicyTest {
    @Test
    fun internalGoalAndHabitMetricsAreNotPresentedAsHealthData() {
        val sources = listOf(
            metric("goal-progress-1", "Read 50 Books"),
            metric("habit-progress-1", "Read Daily"),
            metric("health-connect-steps", "Steps"),
        ).healthDataSources()

        assertEquals(listOf("Steps"), sources.map(MetricDefinition::name))
    }

    @Test
    fun trackAutomationOffersOneClearEntryCountChoiceAndAllGeneralizedMeasures() {
        assertEquals(1, userSelectableTrackAutomationMeasures.count { it == TrackAggregation.CountEntries })
        assertFalse(TrackAggregation.CountMatchingEntries in userSelectableTrackAutomationMeasures)
        assertTrue(
            userSelectableTrackAutomationMeasures.containsAll(
                listOf(
                    TrackAggregation.Sum,
                    TrackAggregation.Average,
                    TrackAggregation.Latest,
                    TrackAggregation.Minimum,
                    TrackAggregation.Maximum,
                    TrackAggregation.FixedAmount,
                ),
            ),
        )
        assertEquals("Each eligible Entry adds 1 to Goal progress.", TrackAggregation.CountEntries.automationExplanation())
    }

    @Test
    fun countGoalsDefaultToCountingEntriesWhileNumberGoalsKeepCompatibleMath() {
        assertEquals(
            listOf(TrackAggregation.CountEntries, TrackAggregation.Sum, TrackAggregation.FixedAmount),
            goal(GoalAggregation.Sum, UnitDimension.Count).compatibleTrackAutomationMeasures(),
        )
        assertEquals(
            listOf(TrackAggregation.Average),
            goal(GoalAggregation.Average, UnitDimension.Unitless, GoalType.MeetAverage).compatibleTrackAutomationMeasures(),
        )
        assertEquals(
            listOf(TrackAggregation.Latest, TrackAggregation.Sum, TrackAggregation.FixedAmount),
            goal(GoalAggregation.Latest, UnitDimension.Unitless).compatibleTrackAutomationMeasures(),
        )
        val typicalCountGoal = goal(GoalAggregation.Latest, UnitDimension.Count)
        assertEquals(TrackAggregation.CountEntries, typicalCountGoal.compatibleTrackAutomationMeasures().first())
        assertEquals(GoalAggregation.Sum, typicalCountGoal.requiredAggregationForTrack(TrackAggregation.CountEntries))
    }

    @Test
    fun trackBackfillMovesGoalWindowEarlierButNeverLater() {
        val original = goal(GoalAggregation.Latest, UnitDimension.Count)

        val backfilled = original.toTrackAutomationDraft(
            GoalAggregation.Sum,
            LocalDate.of(2026, 8, 20),
        )
        assertEquals(GoalAggregation.Sum, backfilled.aggregation)
        assertEquals(LocalDate.of(2026, 8, 20), backfilled.startDate)

        val futureOnly = original.toTrackAutomationDraft(
            GoalAggregation.Sum,
            LocalDate.of(2026, 8, 30),
        )
        assertEquals(original.startDate, futureOnly.startDate)

        val noBackfill = original.toTrackAutomationDraft(GoalAggregation.Sum, null)
        assertEquals(original.startDate, noBackfill.startDate)
    }

    private fun metric(id: String, name: String) = MetricDefinition(
        id = id,
        name = name,
        valueKind = MetricValueKind.Decimal,
        dimension = UnitDimension.Count,
        defaultUnitId = "count",
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )

    private fun goal(
        aggregation: GoalAggregation,
        dimension: UnitDimension,
        type: GoalType = GoalType.ReachValue,
    ) = Goal(
        id = 1,
        uuid = "goal-1",
        metricId = "goal-metric-1",
        name = "Example Goal",
        description = "",
        area = "Main",
        tags = emptyList(),
        icon = "🎯",
        type = type,
        dimension = dimension,
        unitId = if (dimension == UnitDimension.Count) "count" else "unitless",
        precision = 0,
        baseline = null,
        targetMin = 50.0,
        targetMax = null,
        direction = GoalDirection.Increase,
        startDate = LocalDate.of(2026, 8, 24),
        deadline = null,
        aggregation = aggregation,
        paceType = GoalPaceType.None,
        reminderMinutes = null,
        status = GoalStatus.Active,
        pinned = false,
        position = 0,
        createdAtMillis = 1,
        updatedAtMillis = 1,
        aggregationPeriod = GoalAggregationPeriod.All,
    )
}
