package com.whip.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalRulesTest {
    private val today = LocalDate.of(2026, 8, 17)

    @Test fun increasingAndDecreasingProgressUseBaseline() {
        assertEquals(.5, calculateGoalProgress(goal(baseline = 0.0, target = 100.0), 50.0)!!, 0.0)
        assertEquals(.5, calculateGoalProgress(goal(baseline = 100.0, target = 80.0, type = GoalType.ReduceValue), 90.0)!!, 0.0)
    }

    @Test fun baselineEqualTargetIsDefined() {
        val goal = goal(baseline = 75.0, target = 75.0)
        assertEquals(1.0, calculateGoalProgress(goal, 75.0)!!, 0.0)
        assertEquals(0.0, calculateGoalProgress(goal, 74.0)!!, 0.0)
    }

    @Test fun weightedMilestonesAndOpenTrendBehaveDifferently() {
        val milestoneGoal = goal(type = GoalType.WeightedMilestones)
        val milestones = listOf(milestone(1.0, true), milestone(3.0, false))
        assertEquals(.25, calculateGoalProgress(milestoneGoal, null, milestones)!!, 0.0)
        assertNull(calculateGoalProgress(goal(type = GoalType.OpenEndedTrend), 10.0))
    }

    @Test fun elapsedCounterSupportsEveryRequestedViewAndAutomaticScale() {
        val day = 86_400_000L
        assertEquals("90 minutes", elapsedCounter(0, 90 * 60_000L, ElapsedDisplayUnit.Minutes).label())
        assertEquals("36 hours", elapsedCounter(0, 36 * 3_600_000L, ElapsedDisplayUnit.Hours).label())
        assertEquals("10 days", elapsedCounter(0, 10 * day, ElapsedDisplayUnit.Days).label())
        assertEquals("8 weeks", elapsedCounter(0, 56 * day, ElapsedDisplayUnit.Weeks).label())
        assertEquals("2 years", elapsedCounter(0, 730 * day, ElapsedDisplayUnit.Years).label())
        assertEquals(ElapsedDisplayUnit.Years, elapsedCounter(0, 400 * day, ElapsedDisplayUnit.Auto).unit)
        assertEquals(0, elapsedCounter(100, 0, ElapsedDisplayUnit.Minutes).value)
    }

    @Test fun elapsedGoalHasNoSyntheticProgressOrOutcome() {
        val elapsed = goal(type = GoalType.ElapsedSince).copy(
            elapsedStartMillis = 1_000,
            elapsedDisplayUnit = ElapsedDisplayUnit.Days,
        )
        val projection = projectGoal(elapsed, emptyList(), emptyList(), today)
        assertNull(projection.currentValue)
        assertNull(projection.progress)
        assertEquals(0.0, goalOutcomeScoreOnDate(elapsed, emptyList(), emptyList(), today), 0.0)
    }

    @Test fun goalDraftValidationExplainsRequiredAndInvalidFieldsBeforePersistence() {
        val missingTarget = GoalDraft(
            name = "Named goal",
            type = GoalType.ReachValue,
            startDate = today,
        ).withTypeSemantics()
        assertEquals(listOf("Enter a target"), missingTarget.validationErrors(nowMillis = 1_000L))

        val invalidRange = missingTarget.copy(
            type = GoalType.MaintainRange,
            targetMin = 10.0,
            targetMax = 5.0,
        ).withTypeSemantics()
        assertTrue(invalidRange.validationErrors(1_000L).contains("Range minimum cannot exceed range maximum"))

        val invalidPrecision = missingTarget.copy(targetMin = 10.0, precision = -1)
        assertEquals(listOf("Decimal places must be between 0 and 6"), invalidPrecision.validationErrors(1_000L))
    }

    @Test fun paceAndForecastAreDeterministic() {
        val projection = projectGoal(
            goal(baseline = 0.0, target = 100.0).copy(
                deadline = today.plusDays(100),
                paceType = GoalPaceType.Linear,
            ),
            listOf(entry(25.0, today.plusDays(25))),
            emptyList(),
            today.plusDays(25),
        )
        assertTrue(projection.onPace == true)
        assertEquals(today.plusDays(100), projection.forecastDate)
    }

    @Test fun goalTypeNormalizesCalculationDirectionAndDeadlinePace() {
        val accumulated = GoalDraft(
            name = "Distance",
            type = GoalType.AccumulateTotal,
            startDate = today,
            aggregation = GoalAggregation.Latest,
            direction = GoalDirection.Decrease,
            paceType = GoalPaceType.Linear,
        ).withTypeSemantics()

        assertEquals(GoalAggregation.Sum, accumulated.aggregation)
        assertEquals(GoalDirection.Increase, accumulated.direction)
        assertEquals(GoalPaceType.None, accumulated.paceType)

        val range = accumulated.copy(
            type = GoalType.MaintainRange,
            aggregation = GoalAggregation.TimeInRange,
            deadline = today.plusMonths(1),
            paceType = GoalPaceType.Linear,
        ).withTypeSemantics()
        assertEquals(GoalAggregation.TimeInRange, range.aggregation)
        assertEquals(GoalDirection.Neutral, range.direction)
        assertEquals(GoalPaceType.Linear, range.paceType)
    }

    @Test fun goalTypeRemovesAHiddenAggregationWindow() {
        val staleWindow = GoalDraft(
            name = "Project",
            type = GoalType.WeightedMilestones,
            startDate = today,
            aggregationPeriod = GoalAggregationPeriod.RollingDays,
            rollingDays = 0,
            milestones = listOf(GoalMilestoneDraft("Ship", weight = 1.0)),
        )

        assertTrue(staleWindow.validationErrors(1_000L).none { it.contains("rolling window") })
        val semantic = staleWindow.withTypeSemantics()
        assertEquals(GoalAggregationPeriod.All, semantic.aggregationPeriod)
        assertNull(semantic.rollingDays)
        assertTrue(semantic.validationErrors(1_000L).isEmpty())
    }

    @Test fun rollingAverageOnlyUsesConfiguredWindow() {
        val rolling = goal(type = GoalType.MeetAverage).copy(
            aggregation = GoalAggregation.Average,
            aggregationPeriod = GoalAggregationPeriod.RollingDays,
            rollingDays = 7,
            startDate = today.minusDays(30),
        )
        val entries = listOf(
            entry(100.0, today.minusDays(8)).copy(id = "old"),
            entry(10.0, today.minusDays(2)).copy(id = "recent-1"),
            entry(30.0, today).copy(id = "recent-2"),
        )

        assertEquals(20.0, aggregateGoalValue(rolling, entries)!!, 0.0)
    }

    @Test fun consistencyCountsSuccessfulPeriodsRatherThanRawEvents() {
        val consistencyGoal = goal(type = GoalType.Consistency, target = 3.0).copy(
            aggregation = GoalAggregation.CompletionCount,
            consistencyPeriod = GoalConsistencyPeriod.Week,
            consistencyRequiredPeriods = 12,
            deadline = today.plusWeeks(12).minusDays(1),
        )
        val entries = listOf(
            entry(1.0, today).copy(id = "w1-1"),
            entry(1.0, today.plusDays(1)).copy(id = "w1-2"),
            entry(1.0, today.plusDays(2)).copy(id = "w1-3"),
            entry(1.0, today.plusWeeks(1)).copy(id = "w2-1"),
            entry(1.0, today.plusWeeks(1).plusDays(1)).copy(id = "w2-2"),
        )
        val projection = projectGoal(consistencyGoal, entries, emptyList(), today.plusWeeks(1).plusDays(2))

        assertEquals(1, projection.consistency?.successfulPeriods)
        assertEquals(2.0, projection.consistency?.currentPeriodValue ?: -1.0, 0.0)
        assertEquals(1.0 / 12.0, projection.progress ?: -1.0, 0.0)
    }

    @Test fun timeInRangeProgressUsesObservedPercentage() {
        val range = goal(type = GoalType.MaintainRange).copy(
            targetMin = 70.0,
            targetMax = 75.0,
            aggregation = GoalAggregation.TimeInRange,
        )
        val entries = listOf(
            entry(72.0, today).copy(id = "inside"),
            entry(80.0, today.plusDays(1)).copy(id = "outside"),
        )
        val projection = projectGoal(range, entries, emptyList(), today.plusDays(1))

        assertEquals(50.0, projection.currentValue ?: -1.0, 0.0)
        assertEquals(.5, projection.progress ?: -1.0, 0.0)
    }

    @Test fun insightTimelineCoversReachReduceRangeAverageConsistencyAndOpenTrend() {
        val entries = listOf(
            entry(10.0, today).copy(id = "one"),
            entry(20.0, today.plusDays(10)).copy(id = "two"),
        )
        val reach = buildGoalInsights(goal(target = 100.0), entries)
        assertEquals(2, reach.points.size)
        assertEquals(1.0, reach.ratePerDay ?: -1.0, 0.0)
        assertEquals(today.plusDays(90), reach.forecastDate)

        val reduce = buildGoalInsights(
            goal(baseline = 100.0, target = 80.0, type = GoalType.ReduceValue),
            listOf(entry(95.0, today).copy(id = "r1"), entry(90.0, today.plusDays(5)).copy(id = "r2")),
        )
        assertEquals(today.plusDays(15), reduce.forecastDate)

        val range = goal(type = GoalType.MaintainRange).copy(targetMin = 10.0, targetMax = 15.0, aggregation = GoalAggregation.TimeInRange)
        assertEquals(.5, buildGoalInsights(range, entries).points.last().progress ?: -1.0, 0.0)

        val average = goal(type = GoalType.MeetAverage, target = 15.0).copy(aggregation = GoalAggregation.Average)
        assertEquals(15.0, buildGoalInsights(average, entries).points.last().canonicalValue ?: -1.0, 0.0)

        val consistency = goal(type = GoalType.Consistency, target = 1.0).copy(
            aggregation = GoalAggregation.CompletionCount,
            consistencyPeriod = GoalConsistencyPeriod.Week,
            consistencyRequiredPeriods = 2,
        )
        assertTrue(buildGoalInsights(consistency, entries).points.isNotEmpty())

        val open = buildGoalInsights(goal(type = GoalType.OpenEndedTrend), entries)
        assertNull(open.points.last().progress)
    }

    @Test fun insightQualityExplainsExcludedDataAndLargeHistoryStaysComplete() {
        val large = (0 until 10_000).map { index ->
            entry(1.0, today.plusDays((index / 10).toLong())).copy(id = "large-$index")
        } + entry(0.0, today).copy(id = "missing", status = MetricEntryStatus.Missing, canonicalValue = null)
        val accumulate = goal(type = GoalType.AccumulateTotal, target = 20_000.0).copy(aggregation = GoalAggregation.Sum)
        val insight = buildGoalInsights(accumulate, large)

        assertEquals(1_000, insight.points.size)
        assertEquals(10_000.0, insight.points.last().canonicalValue ?: -1.0, 0.0)
        assertTrue(insight.dataQualityExplanation.contains("1 missing"))
        assertEquals("higher", insight.confidence)
    }

    private fun goal(
        baseline: Double? = 0.0,
        target: Double? = 100.0,
        type: GoalType = GoalType.ReachValue,
        direction: GoalDirection = type.defaultDirection(),
    ) = Goal(
        id = 1, uuid = "g", metricId = "m", name = "Goal", description = "", area = "",
        tags = emptyList(), icon = "◎", type = type,
        dimension = UnitDimension.Unitless, unitId = "unitless", precision = 1,
        baseline = baseline, targetMin = target, targetMax = null, direction = direction,
        startDate = today, deadline = null, aggregation = type.defaultAggregation(),
        paceType = GoalPaceType.None,
        reminderMinutes = null, status = GoalStatus.Active, pinned = false, position = 0,
        createdAtMillis = 1, updatedAtMillis = 1,
    )

    private fun milestone(weight: Double, completed: Boolean) = GoalMilestone(1, "ms-$weight", 1, "M", 0, weight, completed, null, "", 1, 1)
    private fun entry(value: Double, date: LocalDate) = MetricEntry("e", "m", value, value, "unitless", MetricEntryStatus.Recorded, date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant(), date, "UTC", 0, MetricSourceType.Manual, null, "", 1, 1)
}
