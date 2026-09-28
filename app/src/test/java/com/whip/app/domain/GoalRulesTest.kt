package com.whip.app.domain

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
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

    @Test fun numericGoalsKeepOverTargetProgressWithoutInflatingReviewOutcomes() {
        val reach = goal(target = 10.0)
        assertEquals(1.2, calculateGoalProgress(reach, 12.0)!!, 0.0)
        assertEquals(0.0, calculateGoalProgress(reach, -1.0)!!, 0.0)
        assertEquals(1.5, calculateGoalProgress(goal(baseline = 100.0, target = 80.0, type = GoalType.ReduceValue), 70.0)!!, 0.0)
        assertEquals(1.2, calculateGoalProgress(goal(target = 10.0, type = GoalType.AccumulateTotal), 12.0)!!, 0.0)
        assertEquals(1.2, calculateGoalProgress(goal(target = 10.0, type = GoalType.MeetAverage), 12.0)!!, 0.0)
        assertNull(calculateGoalProgress(goal(target = Double.MIN_VALUE), Double.MAX_VALUE))

        val entries = listOf(entry(9.0, today), entry(12.0, today.plusDays(1)).copy(id = "later"))
        assertEquals(1.2, projectGoal(reach, entries, emptyList(), today.plusDays(1)).progress!!, 0.0)
        assertEquals(1.2, buildGoalInsights(reach, entries).points.last().progress!!, 0.0)
        assertEquals(0.1, goalOutcomeScoreOnDate(reach, entries, emptyList(), today.plusDays(1)), 0.0000001)
        val beyondEntries = entries + entry(13.0, today.plusDays(2)).copy(id = "later-still")
        assertEquals(0.0, goalOutcomeScoreOnDate(reach, beyondEntries, emptyList(), today.plusDays(2)), 0.0)
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

    @Test fun elapsedDisplaySupportsAnyCanonicalUnitCombinationWithCalendarMonths() {
        val zone = ZoneId.of("America/Toronto")
        val started = ZonedDateTime.of(2024, 1, 31, 10, 0, 0, 0, zone).toInstant().toEpochMilli()
        val now = ZonedDateTime.of(2025, 3, 21, 15, 6, 0, 0, zone).toInstant().toEpochMilli()
        val format = ElapsedDisplayFormat.selected(
            ElapsedDisplayUnit.Minutes,
            ElapsedDisplayUnit.Years,
            ElapsedDisplayUnit.Days,
            ElapsedDisplayUnit.Months,
            ElapsedDisplayUnit.Hours,
            ElapsedDisplayUnit.Weeks,
        )

        assertEquals(
            listOf(
                ElapsedDisplayUnit.Years,
                ElapsedDisplayUnit.Months,
                ElapsedDisplayUnit.Weeks,
                ElapsedDisplayUnit.Days,
                ElapsedDisplayUnit.Hours,
                ElapsedDisplayUnit.Minutes,
            ),
            format.units,
        )
        assertEquals(
            "1 year · 1 month · 3 weeks · 0 days · 5 hours · 6 minutes",
            elapsedDisplay(started, now, format, zone).label(),
        )
    }

    @Test fun elapsedDisplayStorageMigratesLegacyChoicesAndRejectsInvalidCombinations() {
        assertEquals(ElapsedDisplayFormat.Automatic, ElapsedDisplayFormat.fromStorageValue("Auto"))
        assertEquals(
            ElapsedDisplayFormat.selected(ElapsedDisplayUnit.Days),
            ElapsedDisplayFormat.fromStorageValue("Days"),
        )
        val selected = ElapsedDisplayFormat.selected(
            ElapsedDisplayUnit.Months,
            ElapsedDisplayUnit.Days,
            ElapsedDisplayUnit.Minutes,
        )
        assertEquals("Selected:Months|Days|Minutes", selected.storageValue())
        assertEquals(selected, ElapsedDisplayFormat.fromStorageValue(selected.storageValue()))
        assertTrue(runCatching { ElapsedDisplayFormat.fromStorageValue("Selected:Days|Years") }.isFailure)
        assertTrue(runCatching { ElapsedDisplayFormat.fromStorageValue("Selected:") }.isFailure)
    }

    @Test fun elapsedGoalHasNoSyntheticProgressOrOutcome() {
        val elapsed = goal(type = GoalType.ElapsedSince).copy(
            elapsedStartMillis = 1_000,
            elapsedDisplay = ElapsedDisplayFormat.selected(ElapsedDisplayUnit.Days),
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
            listOf(entry(0.0, today).copy(id = "start"), entry(25.0, today.plusDays(25))),
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
        assertEquals(20.0, buildGoalInsights(rolling, entries, through = today).points.last().canonicalValue!!, 0.0)
        assertNull(projectGoal(rolling, entries, emptyList(), today.plusDays(8)).currentValue)
        assertEquals(today, buildGoalInsights(rolling, entries, through = today.plusDays(8)).points.last().date)
    }

    @Test fun insightPointsRespectEveryWindowAndRetainOutsideHistorySeparately() {
        val start = today.minusDays(40)
        val entries = listOf(-1L to 999.0, 0L to 3.0, 2L to 10.0, 15L to 20.0, 32L to 7.0, 40L to 11.0, 41L to 888.0)
            .mapIndexed { index, (day, value) -> entry(value, start.plusDays(day)).copy(id = "window-$index") }
        GoalAggregationPeriod.entries.forEach { period ->
            listOf(GoalAggregation.Latest, GoalAggregation.Sum, GoalAggregation.Average, GoalAggregation.Minimum, GoalAggregation.Maximum, GoalAggregation.TimeInRange).forEach { aggregation ->
                val authored = goal(type = GoalType.OpenEndedTrend).copy(startDate = start, deadline = today,
                    aggregation = aggregation, aggregationPeriod = period, rollingDays = 7, targetMin = 5.0, targetMax = 15.0)
                val summary = buildGoalInsights(authored, entries, through = today)
                assertEquals(5, summary.points.size)
                assertEquals(2, summary.outsideWindowEntries)
                summary.points.forEach { point ->
                    assertEquals("$period / $aggregation on ${point.date}", aggregateGoalValue(authored, entries, point.date)!!, point.canonicalValue!!, 0.000000001)
                }
                assertEquals(7, entries.size)
            }
        }
    }

    @Test fun rollingDecimalAggregatesRetainSmallValuesAfterLargeObservationExpires() {
        val entries = listOf(1e16, 1.0, 2.0).mapIndexed { index, value ->
            entry(value, today.plusDays(index.toLong())).copy(id = "precision-$index")
        }
        listOf(GoalAggregation.Sum to 3.0, GoalAggregation.Average to 1.5).forEach { (aggregation, expected) ->
            val authored = goal(type = GoalType.OpenEndedTrend).copy(
                aggregation = aggregation, aggregationPeriod = GoalAggregationPeriod.RollingDays, rollingDays = 2,
            )
            val points = buildGoalInsights(authored, entries).points
            points.forEach { point ->
                assertEquals(aggregateGoalValue(authored, entries, point.date)!!, point.canonicalValue!!, 0.0)
            }
            assertEquals(expected, points.last().canonicalValue!!, 0.0)
            assertEquals(expected, projectGoal(authored, entries, emptyList(), today.plusDays(2)).currentValue!!, 0.0)
        }
    }

    @Test fun decimalMeansAvoidOverflowAndMatchCurrentAndConsistencyCalculations() {
        val cases = listOf(
            Triple(listOf(0.1, 0.2), 0.3, 0.15),
            Triple(listOf(1e16, 1.0, -1e16), 1.0, 1.0 / 3.0),
            Triple(listOf(Double.MAX_VALUE, Double.MAX_VALUE), Double.POSITIVE_INFINITY, Double.MAX_VALUE),
        )
        cases.forEach { (values, expectedSum, expectedMean) ->
            val entries = values.mapIndexed { index, value -> entry(value, today).copy(id = "decimal-$index") } +
                listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).mapIndexed { index, value ->
                    entry(value, today).copy(id = "invalid-$index")
                }
            listOf(GoalAggregation.Sum to expectedSum, GoalAggregation.Average to expectedMean).forEach { (aggregation, expected) ->
                val authored = goal(type = GoalType.OpenEndedTrend).copy(aggregation = aggregation)
                assertEquals(expected, aggregateGoalValue(authored, entries, today)!!, 0.0)
                val insights = buildGoalInsights(authored, entries, through = today)
                assertEquals(expected, insights.points.single().canonicalValue!!, 0.0)
                assertEquals(3, insights.excludedEntries)
                val consistency = authored.copy(type = GoalType.Consistency, targetMin = expectedMean,
                    consistencyPeriod = GoalConsistencyPeriod.Day, consistencyRequiredPeriods = 1)
                val progress = calculateConsistencyProgress(consistency, entries, today)
                assertEquals(expected, progress.currentPeriodValue, 0.0)
                assertEquals(1, progress.successfulPeriods)
                assertEquals(1.0, buildGoalInsights(consistency, entries, through = today).points.single().canonicalValue!!, 0.0)
            }
        }
    }

    @Test fun latestInsightUsesTheSameFirstTieAsCurrentProgress() {
        val instant = today.atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
        val entries = listOf(entry(4.0, today).copy(id = "z", timestamp = instant), entry(9.0, today).copy(id = "a", timestamp = instant))
        listOf(entries, entries.reversed()).forEach { ordered ->
            val authored = goal()
            assertEquals(aggregateGoalValue(authored, ordered, today)!!,
                buildGoalInsights(authored, ordered, through = today).points.single().canonicalValue!!, 0.0)
        }
    }

    @Test fun forecastsNeedObservedMovementAndDoNotExtrapolateResettingWindows() {
        val entries = listOf(entry(10.0, today).copy(id = "first"), entry(20.0, today.plusDays(10)).copy(id = "last"))
        val authored = goal(target = 100.0)
        assertNull(projectGoal(authored, entries.take(1), emptyList(), today).forecastDate)
        assertEquals(buildGoalInsights(authored, entries).forecastDate, projectGoal(authored, entries, emptyList(), today.plusDays(10)).forecastDate)
        listOf(
            authored to listOf(130.0, 120.0),
            goal(type = GoalType.ReduceValue, baseline = 100.0, target = 80.0) to listOf(70.0, 75.0),
            authored to listOf(60.0, 50.0),
            goal(type = GoalType.ReduceValue, baseline = 100.0, target = 80.0) to listOf(90.0, 95.0),
        ).forEach { (definition, readings) ->
            val observations = readings.mapIndexed { index, value -> entry(value, today.plusDays(index.toLong())).copy(id = "direction-$index") }
            assertNull(buildGoalInsights(definition, observations).forecastDate)
            assertNull(projectGoal(definition, observations, emptyList(), today.plusDays(1)).forecastDate)
        }
        GoalAggregationPeriod.entries.filterNot { it == GoalAggregationPeriod.All }.forEach { period ->
            val windowed = authored.copy(aggregationPeriod = period, rollingDays = 7)
            assertNull(projectGoal(windowed, entries, emptyList(), today.plusDays(10)).forecastDate)
            val summary = buildGoalInsights(windowed, entries)
            assertNull(summary.forecastDate)
            assertTrue(summary.forecastExplanation!!.contains("resets or rolls"))
        }
    }

    @Test fun inferredConsistencyPeriodsAndExpiredPeriodLabelsRemainTruthful() {
        val authored = goal(type = GoalType.Consistency, target = 1.0).copy(
            consistencyPeriod = GoalConsistencyPeriod.Day, consistencyRequiredPeriods = null, deadline = today.plusDays(2))
        val entries = listOf(entry(1.0, today).copy(id = "one"), entry(1.0, today.plusDays(2)).copy(id = "two"))
        assertEquals(2.0 / 3.0, buildGoalInsights(authored, entries).points.last().progress!!, 0.0)
        val expired = authored.copy(consistencyRequiredPeriods = 1, deadline = null)
        val progress = calculateConsistencyProgress(expired, entries, today.plusDays(5))
        assertTrue(progress.trackingWindowEnded)
        assertEquals(today, progress.trackedPeriodStart)
        assertEquals(1, progress.successfulPeriods)
        val deadlineEnded = authored.copy(consistencyPeriod = GoalConsistencyPeriod.Week, consistencyRequiredPeriods = 3,
            startDate = LocalDate.of(2026, 9, 14), deadline = LocalDate.of(2026, 9, 21))
        val afterDeadline = calculateConsistencyProgress(deadlineEnded, emptyList(), LocalDate.of(2026, 9, 28))
        assertEquals(LocalDate.of(2026, 9, 21), afterDeadline.trackedPeriodStart)
        assertTrue(afterDeadline.trackingWindowEnded)
    }

    @Test fun rangeReviewUsesDailyObservationsWithoutComparingPercentagesToMeasurements() {
        val authored = goal(type = GoalType.MaintainRange).copy(targetMin = 18.0, targetMax = 22.0)
        val entries = listOf(entry(20.0, today).copy(id = "inside"), entry(30.0, today).copy(id = "outside", timestamp = today.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().plusSeconds(1)))
        assertEquals(0.0, goalOutcomeScoreOnDate(authored, entries, emptyList(), today), 0.0)
        assertEquals(1.0, goalOutcomeScoreOnDate(authored, entries.take(1), emptyList(), today), 0.0)
        val ratio = authored.copy(aggregation = GoalAggregation.TimeInRange)
        assertEquals(0.5, goalOutcomeScoreOnDate(ratio, entries, emptyList(), today), 0.0)
        assertEquals(0.0, goalOutcomeScoreOnDate(ratio, emptyList(), emptyList(), today), 0.0)
        assertEquals(0.0, goalOutcomeScoreOnDate(ratio.copy(startDate = today.plusDays(1)), entries, emptyList(), today), 0.0)
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
        } + entry(0.0, today).copy(id = "missing", status = MeasurementEntryStatus.Missing, canonicalValue = null)
        val accumulate = goal(type = GoalType.AccumulateTotal, target = 20_000.0).copy(aggregation = GoalAggregation.Sum)
        val insight = buildGoalInsights(accumulate, large)

        assertEquals(1_000, insight.points.size)
        assertEquals(10_000.0, insight.points.last().canonicalValue ?: -1.0, 0.0)
        assertTrue(insight.dataQualityExplanation.contains("1 missing"))
        assertEquals("higher", insight.confidence)
    }

    @Test fun emptyInsightUsesSingularFallbackSourceTypeCopy() {
        val insight = buildGoalInsights(goal(), emptyList())

        assertTrue(insight.dataQualityExplanation.contains("0 observed days from 1 source type;"))
        assertTrue(!insight.dataQualityExplanation.contains("1 source types"))
    }

    private fun goal(
        baseline: Double? = 0.0,
        target: Double? = 100.0,
        type: GoalType = GoalType.ReachValue,
        direction: GoalDirection = type.defaultDirection(),
    ) = Goal(
        id = 1, uuid = "g", measurementId = "m", name = "Goal", description = "", area = "",
        tags = emptyList(), icon = "◎", type = type,
        dimension = UnitDimension.Unitless, unitId = "unitless", precision = 1,
        baseline = baseline, targetMin = target, targetMax = null, direction = direction,
        startDate = today, deadline = null, aggregation = type.defaultAggregation(),
        paceType = GoalPaceType.None,
        reminderMinutes = null, status = GoalStatus.Active, pinned = false, position = 0,
        createdAtMillis = 1, updatedAtMillis = 1,
    )

    private fun milestone(weight: Double, completed: Boolean) = GoalMilestone(1, "ms-$weight", 1, "M", 0, weight, completed, null, "", 1, 1)
    private fun entry(value: Double, date: LocalDate) = MeasurementEntry("e", "m", value, value, "unitless", MeasurementEntryStatus.Recorded, date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant(), date, "UTC", 0, MeasurementSourceType.Manual, null, "", 1, 1)
}
