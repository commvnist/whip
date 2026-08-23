package com.whip.app.domain

import com.whip.app.ui.summarizePreviousSets
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class LargeHistoryRegressionTest {
    private val start = LocalDate.of(2016, 1, 1)

    @Test(timeout = 5_000) fun tenYearsOfHabitGoalAndWorkoutHistoryRemainDeterministic() {
        val days = 3_653
        val dates = (0 until days).map { start.plusDays(it.toLong()) }

        val habit = dailyHabit()
        assertEquals(days, habitStreak(habit, dates.last(), dates.associateWith { true }))

        val goal = latestGoal()
        val measurements = dates.mapIndexed { index, date ->
            MetricEntry(
                id = "entry-$index", metricId = goal.metricId,
                canonicalValue = index.toDouble(), enteredValue = index.toDouble(),
                enteredUnitId = "unitless", status = MetricEntryStatus.Recorded,
                timestamp = date.atStartOfDay(ZoneOffset.UTC).toInstant(), localDate = date,
                zoneId = "UTC", offsetSeconds = 0, sourceType = MetricSourceType.Manual,
                sourceId = null, note = "", createdAtMillis = index.toLong(), updatedAtMillis = index.toLong(),
            )
        }
        assertEquals((days - 1).toDouble(), aggregateGoalValue(goal, measurements)!!, 0.0)

        val exercise = strengthExercise()
        val sessions = dates.mapIndexed { index, date ->
            WorkoutSession(
                id = index + 1L, uuid = "session-$index", name = "", notes = "",
                startedAt = date.atStartOfDay(ZoneOffset.UTC).toInstant(), endedAt = date.atTime(1, 0).toInstant(ZoneOffset.UTC),
                localDate = date, zoneId = "UTC", state = WorkoutSessionState.Finished,
                keepScreenAwake = false, restTimerDeadlineMillis = null, restTimerDurationSeconds = null,
                archived = false, createdAtMillis = index.toLong(), updatedAtMillis = index.toLong(),
            )
        }
        val workoutExercises = sessions.map { session -> WorkoutExercise(session.id, "we-${session.id}", session.id, exercise.id, 0, "", null, 1, 1) }
        val sets = workoutExercises.mapIndexed { index, workoutExercise ->
            WorkoutSet(
                id = index + 1L, uuid = "set-$index", workoutExerciseId = workoutExercise.id,
                position = 0, classification = WorkoutSetClassification.Working, planned = false,
                completed = true, canonicalWeightKg = 60.0 + index % 40, enteredWeight = 60.0 + index % 40,
                enteredWeightUnitId = "kilogram", repetitions = 5, canonicalDistanceMetres = null,
                enteredDistance = null, enteredDistanceUnitId = null, durationSeconds = null, bodyweightKg = null,
                note = "", rpe = null, rir = null, tempo = "", restSeconds = null,
                completedAtMillis = index.toLong(), deletedAtMillis = null, createdAtMillis = index.toLong(), updatedAtMillis = index.toLong(),
            )
        }
        val graph = buildExerciseGraph(exercise, sessions, workoutExercises, sets, GymGraphMetric.EstimatedOneRepMax)
        assertEquals(days, graph.size)
        assertEquals(200, downsampleEvenly(graph, 200).size)
        assertEquals(graph.first(), downsampleEvenly(graph, 200).first())
        assertEquals(graph.last(), downsampleEvenly(graph, 200).last())
    }

    @Test(timeout = 10_000) fun hundredThousandGoalAndSetPointsStayBoundedAndComplete() {
        val exercise = strengthExercise()
        val session = WorkoutSession(
            id = 1, uuid = "large-session", name = "Large", notes = "",
            startedAt = start.atStartOfDay(ZoneOffset.UTC).toInstant(), endedAt = start.atTime(1, 0).toInstant(ZoneOffset.UTC),
            localDate = start, zoneId = "UTC", state = WorkoutSessionState.Finished,
            keepScreenAwake = false, restTimerDeadlineMillis = null, restTimerDurationSeconds = null,
            archived = false, createdAtMillis = 1, updatedAtMillis = 1,
        )
        val placement = WorkoutExercise(1, "large-placement", session.id, exercise.id, 0, "", null, 1, 1)
        val sets = List(100_000) { index ->
            WorkoutSet(
                id = index + 1L, uuid = "large-set-$index", workoutExerciseId = placement.id,
                position = index, classification = WorkoutSetClassification.Working, planned = false,
                completed = true, canonicalWeightKg = 50.0 + index % 100, enteredWeight = 50.0 + index % 100,
                enteredWeightUnitId = "kilogram", repetitions = 5, canonicalDistanceMetres = null,
                enteredDistance = null, enteredDistanceUnitId = null, durationSeconds = null, bodyweightKg = null,
                note = "", rpe = null, rir = null, tempo = "", restSeconds = null,
                completedAtMillis = index.toLong(), deletedAtMillis = null, createdAtMillis = index.toLong(), updatedAtMillis = index.toLong(),
            )
        }
        val graph = buildExerciseGraph(exercise, listOf(session), listOf(placement), sets, GymGraphMetric.WorkoutVolume)
        assertEquals(1, graph.size)
        assertEquals(sets.sumOf { it.volumeKg(exercise) }, graph.single().value, 0.0)
        val previousSummary = summarizePreviousSets(sets, placement.id)
        assertEquals(100_000, previousSummary.totalCount)
        assertEquals(12, previousSummary.sets.size)
        assertEquals(sets.take(12), previousSummary.sets)

        val goal = latestGoal().copy(aggregation = GoalAggregation.Sum, type = GoalType.AccumulateTotal, targetMin = 200_000.0)
        val entries = List(100_000) { index ->
            val date = start.plusDays((index / 10).toLong())
            MetricEntry(
                id = "large-entry-$index", metricId = goal.metricId, canonicalValue = 1.0, enteredValue = 1.0,
                enteredUnitId = "unitless", status = MetricEntryStatus.Recorded,
                timestamp = date.atStartOfDay(ZoneOffset.UTC).toInstant().plusSeconds((index % 10).toLong()),
                localDate = date, zoneId = "UTC", offsetSeconds = 0, sourceType = MetricSourceType.Manual,
                sourceId = null, note = "", createdAtMillis = index.toLong(), updatedAtMillis = index.toLong(),
            )
        }
        val insights = buildGoalInsights(goal, entries)
        assertEquals(10_000, insights.points.size)
        assertEquals(100_000.0, insights.points.last().canonicalValue ?: -1.0, 0.0)
        assertEquals(200, downsampleEvenly(insights.points, 200).size)
    }

    private fun dailyHabit() = Habit(
        id = 1, uuid = "habit", metricId = "habit-metric", name = "Daily", notes = "", area = "", tags = emptyList(),
        icon = "✓", trackingMode = HabitTrackingMode.CheckOff,
        dimension = UnitDimension.Count, unitId = "count", precision = 0, comparison = TargetComparison.AtLeast,
        targetMin = 1.0, targetMax = null, targetPeriod = TargetPeriod.Day, rollingDays = null,
        scheduleType = HabitScheduleType.Daily, scheduleInterval = 1, weekdays = DayOfWeek.entries.toSet(),
        flexibleTimesPerWeek = null, startDate = start, endType = HabitEndType.Never, endDate = null, endValue = null,
        quickIncrement = 1.0, quickActions = emptyList(),
        reminderMinutes = emptyList(), weekdayReminderMinutes = emptyMap(), weekStart = DayOfWeek.MONDAY,
        timerStartedAtMillis = null, pinned = false, position = 0,
        archived = false, paused = false, createdAtMillis = 1, updatedAtMillis = 1,
    )

    private fun latestGoal() = Goal(
        id = 1, uuid = "goal", metricId = "goal-metric", name = "Trend", description = "", area = "", tags = emptyList(),
        icon = "◎", type = GoalType.OpenEndedTrend, dimension = UnitDimension.Unitless,
        unitId = "unitless", precision = 1, baseline = null, targetMin = null, targetMax = null,
        direction = GoalDirection.Neutral, startDate = start, deadline = null, aggregation = GoalAggregation.Latest,
        paceType = GoalPaceType.None, reminderMinutes = null,
        status = GoalStatus.Active, pinned = false, position = 0, createdAtMillis = 1, updatedAtMillis = 1,
    )

    private fun strengthExercise() = Exercise(
        id = 1, uuid = "exercise", name = "Press", trackingType = ExerciseTrackingType.WeightReps,
        notes = "", equipment = "", primaryMuscles = "", secondaryMuscles = "", weightUnitId = "kilogram",
        weightIncrement = 2.5, repetitionIncrement = 1, defaultRestSeconds = 120, defaultGraphMetric = "Estimated1RM",
        oneRepMaxFormula = EstimatedOneRepMaxFormula.Epley, barWeightKg = 20.0, availablePlatesKg = emptyList(),
        includeInVolume = true, includeInPersonalRecords = true, bodyweightLoadPolicy = BodyweightLoadPolicy.ExternalWeightOnly,
        effectiveBodyweightPercent = 100.0, showRpe = null, showRir = null, showTempo = null,
        favorite = false, position = 0, archived = false, createdAtMillis = 1, updatedAtMillis = 1,
    )
}
