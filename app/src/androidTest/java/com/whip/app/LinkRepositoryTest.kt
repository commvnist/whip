package com.whip.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.data.RoomGoalRepository
import com.whip.app.data.RoomGymRepository
import com.whip.app.data.RoomHabitRepository
import com.whip.app.data.RoomLinkRepository
import com.whip.app.data.RoomMeasurementRepository
import com.whip.app.data.RoomTaskRepository
import com.whip.app.data.TaskDeletionCoordinator
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalMilestoneDraft
import com.whip.app.domain.GoalType
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.HabitScheduleType
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.LinkRuleDraft
import com.whip.app.domain.LinkSourceMetric
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.MetricSourceType
import com.whip.app.domain.MetricValueKind
import com.whip.app.domain.TriggerRuleDraft
import com.whip.app.domain.TriggerOutcome
import com.whip.app.domain.TriggerTargetType
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskStepDraft
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.WorkoutSetDraft
import com.whip.app.domain.flexibleProgress
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LinkRepositoryTest {
    private lateinit var database: WhipDatabase
    private lateinit var measurements: RoomMeasurementRepository
    private lateinit var habits: RoomHabitRepository
    private lateinit var goals: RoomGoalRepository
    private lateinit var gym: RoomGymRepository
    private lateinit var links: RoomLinkRepository
    private lateinit var tasks: RoomTaskRepository

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), WhipDatabase::class.java).build()
        val ids = SequentialIds()
        measurements = RoomMeasurementRepository(database, FixedClock, ids)
        habits = RoomHabitRepository(database, measurements, FixedClock, ids)
        goals = RoomGoalRepository(database, measurements, FixedClock, ids)
        gym = RoomGymRepository(database, FixedClock, ids)
        links = RoomLinkRepository(database, measurements, FixedClock, ids)
        tasks = RoomTaskRepository(database, FixedClock)
    }

    @After fun tearDown() = database.close()

    @Test fun habitContributionsBackfillOnceAndDisappearAfterUndo() = runBlocking {
        val habitId = habits.create(HabitDraft(name = "Read", trackingMode = HabitTrackingMode.Count, startDate = FixedClock.today()))
        val logId = habits.log(habitId, 20.0)
        val goalId = goals.create(GoalDraft(name = "Read pages", type = GoalType.AccumulateTotal, dimension = UnitDimension.Count,
            unitId = "count", targetMin = 1000.0, startDate = FixedClock.today(), aggregation = GoalAggregation.Sum))
        val draft = LinkRuleDraft("Reading", sourceType = LinkSourceType.Habit, sourceEntityId = habitId,
            sourceMetric = LinkSourceMetric.NumericValue, targetGoalId = goalId, retroactiveFrom = FixedClock.today())
        val preview = links.previewBackfill(draft)
        assertEquals(1, preview.contributionCount)
        val ruleId = links.createRule(draft, commitBackfill = true)
        links.rebuildRule(ruleId)
        assertEquals(1, links.contributions.first().size)
        assertEquals(20.0, goals.metricEntries.first().single { it.metricId == goals.goals.first().single().metricId }.canonicalValue!!, 0.0)

        habits.undoLog(logId)
        links.rebuildRule(ruleId)
        assertTrue(links.contributions.first().isEmpty())
        assertTrue(goals.metricEntries.first().none { it.metricId == goals.goals.first().single().metricId })
    }

    @Test fun healthBackedHabitRebuildsGoalAfterProviderEditAndDeletion() = runBlocking {
        val sourceMetricId = "health.steps"
        measurements.ensureMetric(
            id = sourceMetricId,
            name = "Steps",
            valueKind = MetricValueKind.Integer,
            dimension = UnitDimension.Count,
            defaultUnitId = "count",
            precision = 0,
        )
        measurements.record(
            metricId = sourceMetricId,
            value = 1_000.0,
            unitId = "count",
            sourceType = MetricSourceType.HealthConnect,
            sourceId = "health:steps:a",
            existingEntryId = "entry-health:steps:a",
        )
        val habitId = habits.create(
            HabitDraft(
                name = "Daily steps",
                trackingMode = HabitTrackingMode.Count,
                targetMin = 8_000.0,
                startDate = FixedClock.today(),
                sourceMetricId = sourceMetricId,
            ),
        )
        val goalId = goals.create(
            GoalDraft(
                name = "Walk more",
                type = GoalType.AccumulateTotal,
                dimension = UnitDimension.Count,
                unitId = "count",
                targetMin = 100_000.0,
                startDate = FixedClock.today(),
                aggregation = GoalAggregation.Sum,
            ),
        )
        val ruleId = links.createRule(
            LinkRuleDraft(
                name = "Steps feed walking goal",
                sourceType = LinkSourceType.Habit,
                sourceEntityId = habitId,
                sourceMetric = LinkSourceMetric.NumericValue,
                targetGoalId = goalId,
                retroactiveFrom = FixedClock.today(),
            ),
            commitBackfill = true,
        )
        assertEquals(1_000.0, links.contributions.first().single().canonicalValue ?: -1.0, 0.0)

        measurements.record(
            metricId = sourceMetricId,
            value = 1_250.0,
            unitId = "count",
            sourceType = MetricSourceType.HealthConnect,
            sourceId = "health:steps:a",
            existingEntryId = "entry-health:steps:a",
        )
        links.rebuildRule(ruleId)
        assertEquals(1_250.0, links.contributions.first().single().canonicalValue ?: -1.0, 0.0)

        measurements.deleteSourceEntriesExcept(MetricSourceType.HealthConnect, "health:steps:", emptySet())
        links.rebuildRule(ruleId)
        assertTrue(links.contributions.first().isEmpty())
        assertTrue(goals.metricEntries.first().none { it.metricId == goals.goals.first().single().metricId })
    }

    @Test fun everyCompletedWorkoutAdvancesFlexibleWeeklyHabitAndRebuildIsIdempotent() = runBlocking {
        val habitId = habits.create(
            HabitDraft(
                name = "Gym 4x week",
                scheduleType = HabitScheduleType.FlexibleTimesPerWeek,
                flexibleTimesPerWeek = 4,
                startDate = FixedClock.today(),
            ),
        )
        links.createTrigger(
            TriggerRuleDraft(
                "Workout counts as training",
                LinkSourceType.Workout,
                0,
                targetType = TriggerTargetType.Habit,
                targetEntityId = habitId,
                // Old rules could persist this as false; workout-to-habit rules must still log.
                autoCompleteTargetHabit = false,
            ),
        )
        val exerciseId = gym.createExercise(ExerciseDraft("Press"))
        val sessions = (0L..3L).map { dayOffset ->
            val sessionId = gym.startWorkout(localDate = FixedClock.today().plusDays(dayOffset))
            val workoutExerciseId = gym.addExerciseToWorkout(sessionId, exerciseId)
            gym.addSet(workoutExerciseId, WorkoutSetDraft(weight = 50.0, reps = 5, completed = true))
            gym.finishWorkout(sessionId)
            links.rebuildAll()
            assertEquals((dayOffset + 1).toInt(), habits.logs.first().size)
            sessionId
        }

        links.rebuildAll()
        val logs = habits.logs.first()
        assertEquals(4, logs.size)
        assertEquals(4, logs.mapNotNull { it.sourceId }.distinct().size)
        assertTrue(logs.all { it.value == 1.0 })
        assertEquals(4, requireNotNull(habits.habits.first().single().flexibleProgress(logs, FixedClock.today().plusDays(3))).completed)

        gym.resumeWorkout(sessions.last())
        links.rebuildAll()
        assertEquals(3, habits.logs.first().size)

        gym.finishWorkout(sessions.last())
        links.rebuildAll()
        assertEquals(4, habits.logs.first().size)
    }

    @Test fun workoutAutomationRejectsOutcomesThatCannotOccur() = runBlocking {
        val habitId = habits.create(HabitDraft(name = "Training", startDate = FixedClock.today()))
        var rejected = false
        try {
            links.createTrigger(
                TriggerRuleDraft(
                    name = "Impossible failed workout",
                    sourceType = LinkSourceType.Workout,
                    sourceEntityId = 0,
                    outcome = TriggerOutcome.Failed,
                    targetType = TriggerTargetType.Habit,
                    targetEntityId = habitId,
                ),
            )
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
        assertTrue(links.triggerRules.first().isEmpty())
    }

    @Test fun linkedContributionCanBeOverriddenAndExcludedWithoutDeletingItsSource() = runBlocking {
        val habitId = habits.create(HabitDraft(name = "Read", trackingMode = HabitTrackingMode.Count, startDate = FixedClock.today()))
        habits.log(habitId, 20.0)
        val goalId = goals.create(GoalDraft(name = "Read pages", type = GoalType.AccumulateTotal, dimension = UnitDimension.Count,
            unitId = "count", targetMin = 1000.0, startDate = FixedClock.today(), aggregation = GoalAggregation.Sum))
        links.createRule(LinkRuleDraft("Reading", sourceType = LinkSourceType.Habit, sourceEntityId = habitId,
            sourceMetric = LinkSourceMetric.NumericValue, targetGoalId = goalId, retroactiveFrom = FixedClock.today()), commitBackfill = true)
        val contribution = links.contributions.first().single()
        val goalMetricId = goals.goals.first().single { it.id == goalId }.metricId

        links.setContributionOverride(contribution.id, 5.0)
        assertEquals(5.0, links.contributions.first().single().overrideValue ?: -1.0, 0.0)
        assertEquals(5.0, goals.metricEntries.first().single { it.metricId == goalMetricId }.canonicalValue ?: -1.0, 0.0)

        links.setContributionExcluded(contribution.id, true)
        assertTrue(links.contributions.first().single().excluded)
        assertTrue(goals.metricEntries.first().none { it.metricId == goalMetricId })
        assertEquals(1, habits.logs.first().size)
    }

    @Test fun metricDependencyCyclesAreRejected() = runBlocking {
        val firstId = goals.create(GoalDraft(name = "First", type = GoalType.AccumulateTotal, targetMin = 1.0, startDate = FixedClock.today(), aggregation = GoalAggregation.Sum))
        val secondId = goals.create(GoalDraft(name = "Second", type = GoalType.AccumulateTotal, targetMin = 1.0, startDate = FixedClock.today(), aggregation = GoalAggregation.Sum))
        val first = goals.goals.first().first { it.id == firstId }
        val second = goals.goals.first().first { it.id == secondId }
        links.createRule(LinkRuleDraft("A to B", sourceType = LinkSourceType.Metric, sourceMetricId = first.metricId,
            sourceMetric = LinkSourceMetric.NumericValue, targetGoalId = secondId))
        var rejected = false
        try {
            links.createRule(LinkRuleDraft("B to A", sourceType = LinkSourceType.Metric, sourceMetricId = second.metricId,
                sourceMetric = LinkSourceMetric.NumericValue, targetGoalId = firstId))
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test fun customUnitsRemainCanonicalAcrossHabitToGoalLinks() = runBlocking {
        val glass = measurements.createCustomUnit("glass", "gl", UnitDimension.Volume, 250.0)
        val habitId = habits.create(
            HabitDraft(
                name = "Water",
                trackingMode = HabitTrackingMode.Decimal,
                dimension = UnitDimension.Volume,
                unitId = glass,
                startDate = FixedClock.today(),
            ),
        )
        habits.log(habitId, 2.0)
        val goalId = goals.create(
            GoalDraft(
                name = "Hydrate",
                type = GoalType.AccumulateTotal,
                dimension = UnitDimension.Volume,
                unitId = glass,
                targetMin = 8.0,
                startDate = FixedClock.today(),
                aggregation = GoalAggregation.Sum,
            ),
        )

        links.createRule(
            LinkRuleDraft(
                "Water contribution",
                sourceType = LinkSourceType.Habit,
                sourceEntityId = habitId,
                sourceMetric = LinkSourceMetric.NumericValue,
                targetGoalId = goalId,
                retroactiveFrom = FixedClock.today(),
            ),
            commitBackfill = true,
        )

        val linked = goals.metricEntries.first().single { it.metricId == goals.goals.first().single().metricId }
        assertEquals(2.0, linked.enteredValue ?: -1.0, 0.0)
        assertEquals(500.0, linked.canonicalValue ?: -1.0, 0.0)
    }

    @Test fun permanentTaskDeleteRemovesLinksAutomationsAndTheirDerivedData() = runBlocking {
        val taskId = tasks.create(
            TaskDraft(
                title = "Submit report",
                steps = listOf(TaskStepDraft(title = "Proofread", position = 0)),
            ),
        )
        val stepId = tasks.steps.first().single().id
        val task = requireNotNull(tasks.getTask(taskId))
        tasks.complete(
            com.whip.app.domain.ScheduledTask(
                task = task,
                originalDate = null,
                scheduledDate = null,
            ),
        )
        val goalId = goals.create(
            GoalDraft(
                name = "Projects shipped",
                type = GoalType.AccumulateTotal,
                dimension = UnitDimension.Count,
                unitId = "count",
                targetMin = 10.0,
                startDate = FixedClock.today(),
                aggregation = GoalAggregation.Sum,
            ),
        )
        links.createRule(
            LinkRuleDraft(
                name = "Completed projects",
                sourceType = LinkSourceType.Task,
                sourceEntityId = taskId,
                sourceMetric = LinkSourceMetric.Completion,
                targetGoalId = goalId,
                retroactiveFrom = FixedClock.today(),
            ),
            commitBackfill = true,
        )
        links.createRule(
            LinkRuleDraft(
                name = "Proofreading contribution",
                sourceType = LinkSourceType.Subtask,
                sourceEntityId = stepId,
                sourceMetric = LinkSourceMetric.Completion,
                targetGoalId = goalId,
            ),
        )
        val habitId = habits.create(HabitDraft(name = "Celebrate", startDate = FixedClock.today()))
        links.createTrigger(
            TriggerRuleDraft(
                name = "Celebrate completed project",
                sourceType = LinkSourceType.Task,
                sourceEntityId = taskId,
                targetType = TriggerTargetType.Habit,
                targetEntityId = habitId,
                autoCompleteTargetHabit = true,
            ),
        )
        links.createTrigger(
            TriggerRuleDraft(
                name = "Celebrate proofreading",
                sourceType = LinkSourceType.Subtask,
                sourceEntityId = stepId,
                targetType = TriggerTargetType.Habit,
                targetEntityId = habitId,
            ),
        )
        val reminderHabitId = habits.create(HabitDraft(name = "Plan tomorrow", startDate = FixedClock.today()))
        links.createTrigger(
            TriggerRuleDraft(
                name = "Habit reminds task",
                sourceType = LinkSourceType.Habit,
                sourceEntityId = reminderHabitId,
                targetType = TriggerTargetType.Task,
                targetEntityId = taskId,
            ),
        )
        links.rebuildAll()

        assertEquals(1, links.contributions.first().size)
        assertEquals(3, links.triggerRules.first().size)
        assertEquals(1, habits.logs.first().size)
        assertTrue(goals.metricEntries.first().any { it.metricId == goals.goals.first().single().metricId })

        val coordinator = TaskDeletionCoordinator(database, tasks, links)
        val impact = coordinator.preview(taskId)
        assertEquals("Submit report", impact.title)
        assertEquals(1, impact.recordedOccurrenceCount)
        assertEquals(1, impact.completedOccurrenceCount)
        assertEquals(1, impact.stepCount)
        assertEquals(2, impact.linkRuleCount)
        assertEquals(3, impact.automationRuleCount)

        val result = coordinator.delete(taskId, impact.revisionToken)

        assertTrue(result.taskDeleted)
        assertEquals(2, result.linkRulesDeleted)
        assertEquals(3, result.automationRulesDeleted)
        assertTrue(links.rules.first().isEmpty())
        assertTrue(links.contributions.first().isEmpty())
        assertTrue(links.triggerRules.first().isEmpty())
        assertTrue(links.triggerOccurrences.first().isEmpty())
        assertTrue(habits.logs.first().isEmpty())
        assertTrue(goals.metricEntries.first().none { it.metricId == goals.goals.first().single().metricId })
        assertEquals(1, goals.goals.first().size)
        assertEquals(2, habits.habits.first().size)
    }

    private object FixedClock : WhipClock {
        override fun now(): Instant = Instant.parse("2026-08-17T16:00:00Z")
        override fun today(zoneId: ZoneId): LocalDate = LocalDate.of(2026, 8, 17)
    }
    private class SequentialIds : WhipIdGenerator {
        private val count = AtomicInteger()
        override fun nextId() = "link-test-${count.incrementAndGet()}"
    }
}
