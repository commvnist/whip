package com.whip.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.data.AreaDeletionCoordinator
import com.whip.app.data.DomainDeletionCoordinator
import com.whip.app.data.RoomAreaRepository
import com.whip.app.data.RoomGoalRepository
import com.whip.app.data.RoomGymRepository
import com.whip.app.data.RoomHabitRepository
import com.whip.app.data.RoomLinkRepository
import com.whip.app.data.RoomMeasurementRepository
import com.whip.app.data.RoomRoutineRepository
import com.whip.app.data.RoomTaskRepository
import com.whip.app.data.RoomTrackRepository
import com.whip.app.data.RoomBackupRepository
import com.whip.app.data.TaskDeletionCoordinator
import com.whip.app.data.CommittedTaskDeletionCancellation
import com.whip.app.data.CommittedGoalDeletionCancellation
import com.whip.app.data.CommittedAreaDeletionCancellation
import com.whip.app.data.ContributionEntity
import com.whip.app.data.GoalElapsedResetEventEntity
import com.whip.app.data.GoalClosureSnapshotEntity
import com.whip.app.data.LinkRuleConditionEntity
import com.whip.app.data.TriggerFieldMappingEntity
import com.whip.app.data.TriggerOccurrenceEntity
import com.whip.app.data.TriggerRuleConditionEntity
import com.whip.app.data.TriggerRuleEntity
import com.whip.app.data.TrainingMaxDecisionEntity
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.GymMachineDraft
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalMilestoneDraft
import com.whip.app.domain.GoalType
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.LinkRuleDraft
import com.whip.app.domain.LinkSourceMetric
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.MetricSourceType
import com.whip.app.domain.RoutineDayDraft
import com.whip.app.domain.RoutineDraft
import com.whip.app.domain.RoutineExerciseDraft
import com.whip.app.domain.RoutineEquipmentBindingState
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskStepDraft
import com.whip.app.domain.TriggerRuleDraft
import com.whip.app.domain.TriggerTargetType
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.WorkoutSetDraft
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DomainDeletionCoordinatorTest {
    private lateinit var database: WhipDatabase
    private lateinit var measurements: RoomMeasurementRepository
    private lateinit var habits: RoomHabitRepository
    private lateinit var goals: RoomGoalRepository
    private lateinit var gym: RoomGymRepository
    private lateinit var routines: RoomRoutineRepository
    private lateinit var links: RoomLinkRepository
    private lateinit var tasks: RoomTaskRepository
    private lateinit var tracks: RoomTrackRepository
    private lateinit var areas: RoomAreaRepository
    private lateinit var coordinator: DomainDeletionCoordinator
    private lateinit var areaDeletionCoordinator: AreaDeletionCoordinator

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), WhipDatabase::class.java).build()
        val ids = SequentialIds()
        measurements = RoomMeasurementRepository(database, FixedClock, ids)
        habits = RoomHabitRepository(database, measurements, FixedClock, ids)
        goals = RoomGoalRepository(database, measurements, FixedClock, ids)
        gym = RoomGymRepository(database, FixedClock, ids)
        routines = RoomRoutineRepository(database, FixedClock, ids)
        links = RoomLinkRepository(database, measurements, FixedClock, ids)
        tasks = RoomTaskRepository(database, FixedClock)
        tracks = RoomTrackRepository(database, FixedClock, ids)
        areas = RoomAreaRepository(database, FixedClock, ids)
        coordinator = DomainDeletionCoordinator(database, links, routines)
        areaDeletionCoordinator = AreaDeletionCoordinator(
            database,
            areas,
            TaskDeletionCoordinator(database, tasks, links),
            coordinator,
        )
    }

    @After fun tearDown() = database.close()

    @Test fun taskDeletionRevisionRejectsAnAutomationAddedAfterPreview() = runBlocking {
        val targetId = tasks.create(TaskDraft(title = "Reviewed deletion"))
        val sourceId = tasks.create(TaskDraft(title = "New automation source"))
        val deletion = TaskDeletionCoordinator(database, tasks, links)
        val preview = deletion.preview(targetId)
        links.createTrigger(
            TriggerRuleDraft(
                name = "Added after review",
                sourceType = LinkSourceType.Task,
                sourceEntityId = sourceId,
                targetType = TriggerTargetType.Task,
                targetEntityId = targetId,
            ),
        )

        val result = runCatching { deletion.delete(targetId, preview.revisionToken) }

        assertTrue(result.isFailure)
        assertNotNull(tasks.getTask(targetId))
        assertEquals(1, links.triggerRules.first().count { it.targetEntityId == targetId })
    }

    @Test fun trackDeletionUsesExactReviewedDefinitionAndHistoryImpact() = runBlocking {
        val trackId = tracks.create(
            TrackDraft(
                name = "Reviewed Track",
                fields = listOf(
                    TrackFieldDraft(
                        name = "Note",
                        type = TrackFieldType.ShortText,
                        primary = true,
                        required = true,
                    ),
                ),
            ),
        )
        val initial = requireNotNull(coordinator.previewTrackDeletion(trackId))
        assertEquals("Reviewed Track", initial.displayName)
        assertEquals(1, initial.fieldCount)
        assertEquals(0, initial.entryCount)

        tracks.setPinned(trackId, true)
        val staleDelete = runCatching { coordinator.deleteTrack(trackId, initial.revisionToken) }

        assertTrue(staleDelete.isFailure)
        assertNotNull(database.trackDao().getTrack(trackId))

        val beforeAutomation = requireNotNull(coordinator.previewTrackDeletion(trackId))
        val automationTargetId = tasks.create(TaskDraft(title = "Automation target"))
        links.createTrigger(
            TriggerRuleDraft(
                name = "New Track automation",
                sourceType = LinkSourceType.Track,
                sourceEntityId = trackId,
                targetType = TriggerTargetType.Task,
                targetEntityId = automationTargetId,
            ),
        )
        assertTrue(runCatching { coordinator.deleteTrack(trackId, beforeAutomation.revisionToken) }.isFailure)

        val refreshed = requireNotNull(coordinator.previewTrackDeletion(trackId))
        assertEquals(1, refreshed.automationRuleCount)
        val summary = coordinator.deleteTrack(trackId, refreshed.revisionToken)

        assertTrue(summary.trackDeleted)
        assertEquals(1, summary.fieldsDeleted)
        assertEquals(0, summary.entriesDeleted)
        assertEquals(1, summary.automationRulesDeleted)
        assertNull(database.trackDao().getTrack(trackId))
    }

    @Test fun taskDeletionRevisionRejectsAutomationConditionAndMappingAddedAfterPreview() = runBlocking {
        val taskId = tasks.create(TaskDraft(title = "Automation source under review"))
        val trackId = tracks.create(
            TrackDraft(
                name = "Deletion revision target",
                fields = listOf(
                    TrackFieldDraft(
                        name = "Task",
                        type = TrackFieldType.ShortText,
                        primary = true,
                        required = true,
                    ),
                ),
            ),
        )
        val targetFieldId = requireNotNull(tracks.projection(trackId)).fields.single().id
        val triggerId = database.linkDao().insertTriggerRule(
            TriggerRuleEntity(
                uuid = "deletion-child-trigger",
                name = "Record source Task",
                sourceType = "Task",
                sourceEntityId = taskId,
                outcome = "Completed",
                targetType = "Track",
                targetEntityId = trackId,
                delayMinutes = 0,
                quietStartMinutes = null,
                quietEndMinutes = null,
                action = "PromptTrackEntry",
                notificationEnabled = false,
                conditionMode = "MatchAll",
                enabled = true,
                createdAtMillis = 1,
                updatedAtMillis = 1,
            ),
        )
        val deletion = TaskDeletionCoordinator(database, tasks, links)
        val beforeCondition = deletion.preview(taskId)
        database.linkDao().insertTriggerCondition(
            TriggerRuleConditionEntity(
                triggerRuleId = triggerId,
                fieldId = null,
                entryDate = true,
                operator = "OnOrAfter",
                position = 0,
                textValue = null,
                numberValue = null,
                secondNumberValue = null,
                dateEpochDay = FixedClock.today().toEpochDay(),
                secondDateEpochDay = null,
            ),
        )

        assertTrue(runCatching { deletion.delete(taskId, beforeCondition.revisionToken) }.isFailure)
        assertNotNull(tasks.getTask(taskId))

        val beforeMapping = deletion.preview(taskId)
        database.linkDao().insertTriggerMapping(
            TriggerFieldMappingEntity(
                triggerRuleId = triggerId,
                targetFieldId = targetFieldId,
                sourceProperty = "Title",
                constantText = null,
                constantNumber = null,
                constantUnitId = null,
                constantDateEpochDay = null,
                constantBoolean = null,
                constantChoiceOptionId = null,
                constantScale = null,
            ),
        )

        assertTrue(runCatching { deletion.delete(taskId, beforeMapping.revisionToken) }.isFailure)
        assertNotNull(tasks.getTask(taskId))
    }

    @Test fun taskDeletionOwnsSubtaskLinksAndAutomationsByParentAndStepIdentity() = runBlocking {
        val taskId = tasks.create(
            TaskDraft(
                title = "Parent",
                steps = listOf(TaskStepDraft(title = "Child", position = 0)),
            ),
        )
        val stepId = requireNotNull(tasks.getTask(taskId)).steps.single().id
        val goalId = goals.create(accumulatingGoal("Child progress"))
        val targetId = tasks.create(TaskDraft(title = "Follow-up"))
        links.createRule(
            LinkRuleDraft(
                name = "Child to goal",
                sourceType = LinkSourceType.Subtask,
                sourceEntityId = taskId,
                sourceItemId = stepId,
                sourceMetric = LinkSourceMetric.Completion,
                targetGoalId = goalId,
            ),
        )
        links.createTrigger(
            TriggerRuleDraft(
                name = "Child follow-up",
                sourceType = LinkSourceType.Subtask,
                sourceEntityId = taskId,
                sourceItemId = stepId,
                targetType = TriggerTargetType.Task,
                targetEntityId = targetId,
            ),
        )
        val deletion = TaskDeletionCoordinator(database, tasks, links)
        val preview = deletion.preview(taskId)

        assertEquals(1, preview.linkRuleCount)
        assertEquals(1, preview.automationRuleCount)
        val summary = deletion.delete(taskId, preview.revisionToken)

        assertTrue(summary.taskDeleted)
        assertEquals(1, summary.linkRulesDeleted)
        assertEquals(1, summary.automationRulesDeleted)
        assertTrue(links.rules.first().isEmpty())
        assertTrue(links.triggerRules.first().isEmpty())
        assertNotNull(tasks.getTask(targetId))
    }

    @Test fun reviewedTaskDisappearingBeforeDeleteIsAnOwnedFailure() = runBlocking {
        val taskId = tasks.create(TaskDraft(title = "Reviewed then removed"))
        val deletion = TaskDeletionCoordinator(database, tasks, links)
        val preview = deletion.preview(taskId)
        assertTrue(tasks.deletePermanently(taskId))

        val result = runCatching { deletion.delete(taskId, preview.revisionToken) }

        assertTrue(result.isFailure)
    }

    @Test fun taskDeletionReportsPostCommitCleanupFailureWithoutClaimingTheDeleteFailed() = runBlocking {
        val taskId = tasks.create(TaskDraft(title = "Committed deletion"))
        val reconciliations = AtomicInteger()
        val deletion = TaskDeletionCoordinator(
            database,
            tasks,
            links,
            onDeletionCommitted = { error("Simulated reminder cleanup failure") },
            onDeletionInterrupted = { reconciliations.incrementAndGet() },
        )
        val preview = deletion.preview(taskId)

        val summary = deletion.delete(taskId, preview.revisionToken)

        assertTrue(summary.taskDeleted)
        assertTrue(summary.warnings.single().contains("permanent deletion was committed"))
        assertNull(tasks.getTask(taskId))
        assertEquals(1, reconciliations.get())
    }

    @Test fun taskDeletionDoesNotConvertFatalPostCommitErrorsIntoOrdinaryWarnings() = runBlocking {
        val taskId = tasks.create(TaskDraft(title = "Fatal cleanup"))
        val deletion = TaskDeletionCoordinator(
            database,
            tasks,
            links,
            onDeletionCommitted = { throw AssertionError("fatal cleanup corruption") },
        )
        val preview = deletion.preview(taskId)

        val result = runCatching { deletion.delete(taskId, preview.revisionToken) }

        assertTrue(result.exceptionOrNull() is AssertionError)
        assertNull(tasks.getTask(taskId))
    }

    @Test fun taskDeletionDoesNotSwallowFatalReconciliationFailureAfterCommittedDelete() = runBlocking {
        val taskId = tasks.create(TaskDraft(title = "Fatal reconciliation"))
        val deletion = TaskDeletionCoordinator(
            database,
            tasks,
            links,
            onDeletionCommitted = { error("ordinary cleanup failure") },
            onDeletionInterrupted = { throw AssertionError("fatal reconciliation corruption") },
        )
        val preview = deletion.preview(taskId)

        val result = runCatching { deletion.delete(taskId, preview.revisionToken) }

        assertTrue(result.exceptionOrNull() is AssertionError)
        assertNull(tasks.getTask(taskId))
    }

    @Test fun taskDeletionCancellationAfterCommitEscapesAndNeverRollsBackCommittedDelete() = runBlocking {
        val taskId = tasks.create(TaskDraft(title = "Cancelled cleanup"))
        val deletion = TaskDeletionCoordinator(
            database,
            tasks,
            links,
            onDeletionCommitted = { throw CancellationException("cancel after delete") },
        )
        val preview = deletion.preview(taskId)

        val result = runCatching { deletion.delete(taskId, preview.revisionToken) }

        assertTrue(result.exceptionOrNull() is CancellationException)
        assertNull(tasks.getTask(taskId))
    }

    @Test fun taskDeletionReconciliationCancellationCarriesCommittedSummary() = runBlocking {
        val taskId = tasks.create(TaskDraft(title = "Cancelled reconciliation"))
        val deletion = TaskDeletionCoordinator(
            database,
            tasks,
            links,
            onDeletionCommitted = { error("ordinary cleanup failure") },
            onDeletionInterrupted = { throw CancellationException("cancel reconciliation") },
        )
        val preview = deletion.preview(taskId)

        val result = runCatching { deletion.delete(taskId, preview.revisionToken) }

        val cancellation = result.exceptionOrNull() as CommittedTaskDeletionCancellation
        assertTrue(cancellation.summary.taskDeleted)
        assertNull(tasks.getTask(taskId))
    }

    @Test fun promotionUndoReportsPostCommitCleanupFailureAndKeepsItsCommittedResult() = runBlocking {
        val parentId = tasks.create(
            TaskDraft(
                title = "Parent",
                steps = listOf(TaskStepDraft(title = "Promote me", position = 0)),
            ),
        )
        val parent = requireNotNull(tasks.getTask(parentId))
        val sourceStepId = parent.steps.single().id
        val promotedId = tasks.promoteStep(
            ScheduledTask(parent, originalDate = null, scheduledDate = null),
            sourceStepId,
        )
        val archivedSource = requireNotNull(tasks.getTask(parentId)).steps.single()
        val deletion = TaskDeletionCoordinator(
            database,
            tasks,
            links,
            onDeletionCommitted = { error("Simulated reminder cleanup failure") },
        )
        val promotedPreview = deletion.preview(promotedId)

        val summary = deletion.undoPromotion(
            promotedTaskId = promotedId,
            expectedRevisionToken = promotedPreview.revisionToken,
            sourceTaskId = parentId,
            sourceStepId = sourceStepId,
            expectedSourceStepUpdatedAtMillis = archivedSource.updatedAtMillis,
        )

        assertTrue(summary.taskDeleted)
        assertTrue(summary.warnings.single().contains("promotion undo was committed"))
        assertNull(tasks.getTask(promotedId))
        assertFalse(requireNotNull(tasks.getTask(parentId)).steps.single().archived)
    }

    @Test fun pendingAutomationOccurrenceAddedAfterPreviewInvalidatesDeletion() = runBlocking {
        val sourceId = tasks.create(TaskDraft(title = "Automation source"))
        val targetId = tasks.create(TaskDraft(title = "Reviewed target"))
        val triggerId = links.createTrigger(
            TriggerRuleDraft(
                name = "Targeting reviewed Task",
                sourceType = LinkSourceType.Task,
                sourceEntityId = sourceId,
                targetType = TriggerTargetType.Task,
                targetEntityId = targetId,
            ),
        )
        val deletion = TaskDeletionCoordinator(database, tasks, links)
        val preview = deletion.preview(targetId)
        database.linkDao().upsertTriggerOccurrence(
            TriggerOccurrenceEntity(
                triggerRuleId = triggerId,
                sourceEventId = "new-after-preview",
                availableAtMillis = FixedClock.now().toEpochMilli(),
                deliveredAtMillis = null,
                dismissedAtMillis = null,
                remindAtMillis = null,
                fulfilledEntryId = null,
                sourceSnapshot = "{}",
            ),
        )

        val result = runCatching { deletion.delete(targetId, preview.revisionToken) }

        assertTrue(result.isFailure)
        assertNotNull(tasks.getTask(targetId))
    }

    @Test fun habitDeleteRemovesOwnedMetricLinksAndTargetingAutomations() = runBlocking {
        val habitId = habits.create(HabitDraft(name = "Read", startDate = FixedClock.today()))
        habits.log(habitId, 1.0)
        val habitMetricId = habits.habits.first().single().metricId
        val goalId = goals.create(accumulatingGoal("Reading goal"))
        links.createRule(
            LinkRuleDraft("Read link", sourceType = LinkSourceType.Habit, sourceEntityId = habitId,
                sourceMetric = LinkSourceMetric.NumericValue, targetGoalId = goalId, retroactiveFrom = FixedClock.today()),
            commitBackfill = true,
        )
        val taskId = tasks.create(TaskDraft(title = "Prompt source"))
        links.createTrigger(
            TriggerRuleDraft("Prompt reading", LinkSourceType.Task, taskId,
                targetType = TriggerTargetType.Habit, targetEntityId = habitId),
        )

        val summary = coordinator.deleteHabit(habitId)

        assertTrue(summary.deleted)
        assertTrue(habits.habits.first().isEmpty())
        assertTrue(habits.logs.first().isEmpty())
        assertNull(database.measurementDao().getMetric(habitMetricId))
        assertTrue(links.rules.first().isEmpty())
        assertTrue(links.triggerRules.first().isEmpty())
        assertTrue(goals.metricEntries.first().none { it.metricId == goals.goals.first().single().metricId })
    }

    @Test fun goalDeleteRemovesOwnedMetricAndPreservesIndependentHabits() = runBlocking {
        val habitId = habits.create(HabitDraft(name = "Pages", startDate = FixedClock.today()))
        habits.log(habitId, 12.0)
        val goalId = goals.create(accumulatingGoal("Book"))
        val metricId = goals.goals.first().single().metricId
        coordinator.deleteGoal(goalId)

        assertTrue(goals.goals.first().isEmpty())
        assertTrue(links.rules.first().isEmpty())
        assertTrue(links.contributions.first().isEmpty())
        assertNull(database.measurementDao().getMetric(metricId))
        assertEquals(1, habits.habits.first().size)
    }

    @Test fun goalDeletionRevisionOwnsDefinitionHistoryLinksAndContributions() = runBlocking {
        val habitId = habits.create(HabitDraft(name = "Source", startDate = FixedClock.today()))
        val goalId = goals.create(
            accumulatingGoal("Reviewed Goal").copy(
                type = GoalType.WeightedMilestones,
                milestones = listOf(GoalMilestoneDraft("Checkpoint")),
            ),
        )
        val goal = requireNotNull(goals.get(goalId))
        val milestoneId = database.goalDao().getMilestones(goalId).single().id
        val otherGoalId = goals.create(accumulatingGoal("Independent target"))

        suspend fun stalePreviewIsRejected() {
            val preview = coordinator.previewGoalDeletion(goalId)
            goals.setPinned(goalId, !requireNotNull(goals.get(goalId)).pinned)
            assertTrue(runCatching { coordinator.deleteGoal(goalId, preview.revisionToken) }.isFailure)
            assertNotNull(goals.get(goalId))
        }

        stalePreviewIsRejected()

        var preview = coordinator.previewGoalDeletion(goalId)
        val metric = requireNotNull(database.measurementDao().getMetric(goal.metricId))
        database.measurementDao().upsertMetric(metric.copy(name = "Changed metric definition"))
        assertTrue(runCatching { coordinator.deleteGoal(goalId, preview.revisionToken) }.isFailure)

        preview = coordinator.previewGoalDeletion(goalId)
        goals.toggleMilestone(milestoneId, true)
        assertTrue(runCatching { coordinator.deleteGoal(goalId, preview.revisionToken) }.isFailure)

        preview = coordinator.previewGoalDeletion(goalId)
        measurements.record(goal.metricId, 42.0, "count")
        assertTrue(runCatching { coordinator.deleteGoal(goalId, preview.revisionToken) }.isFailure)

        preview = coordinator.previewGoalDeletion(goalId)
        database.goalDao().insertClosureSnapshot(
            GoalClosureSnapshotEntity(
                uuid = "goal-closure-review",
                goalId = goalId,
                completedAtMillis = FixedClock.now().toEpochMilli(),
                value = 42.0,
                progress = 0.42,
                status = "Completed",
            ),
        )
        assertTrue(runCatching { coordinator.deleteGoal(goalId, preview.revisionToken) }.isFailure)

        preview = coordinator.previewGoalDeletion(goalId)
        database.goalDao().insertElapsedResetEvent(
            GoalElapsedResetEventEntity(
                uuid = "goal-reset-review",
                goalId = goalId,
                goalUuid = goal.uuid,
                previousStartMillis = 1,
                newStartMillis = 2,
                resetAtMillis = 3,
                elapsedDurationMillis = 1,
            ),
        )
        assertTrue(runCatching { coordinator.deleteGoal(goalId, preview.revisionToken) }.isFailure)

        preview = coordinator.previewGoalDeletion(goalId)
        val targetLinkId = links.createRule(
            LinkRuleDraft(
                name = "Source to reviewed Goal",
                sourceType = LinkSourceType.Habit,
                sourceEntityId = habitId,
                sourceMetric = LinkSourceMetric.NumericValue,
                targetGoalId = goalId,
            ),
        )
        assertTrue(runCatching { coordinator.deleteGoal(goalId, preview.revisionToken) }.isFailure)

        preview = coordinator.previewGoalDeletion(goalId)
        database.linkDao().insertRuleCondition(
            LinkRuleConditionEntity(
                linkRuleId = targetLinkId,
                fieldId = null,
                entryDate = true,
                operator = "OnOrAfter",
                position = 0,
                textValue = null,
                numberValue = null,
                secondNumberValue = null,
                dateEpochDay = FixedClock.today().toEpochDay(),
                secondDateEpochDay = null,
            ),
        )
        assertTrue(runCatching { coordinator.deleteGoal(goalId, preview.revisionToken) }.isFailure)

        preview = coordinator.previewGoalDeletion(goalId)
        database.linkDao().upsertContribution(
            ContributionEntity(
                uuid = "goal-delete-contribution",
                linkRuleId = targetLinkId,
                sourceEventId = "habit:$habitId:review",
                sourceType = LinkSourceType.Habit.name,
                sourceEntityId = habitId,
                targetGoalId = goalId,
                metricEntryId = null,
                canonicalValue = 1.0,
                localEpochDay = FixedClock.today().toEpochDay(),
                timestampMillis = FixedClock.now().toEpochMilli(),
                excluded = false,
                overrideValue = null,
                explanation = "Deletion review",
                createdAtMillis = FixedClock.now().toEpochMilli(),
                updatedAtMillis = FixedClock.now().toEpochMilli(),
            ),
        )
        assertTrue(runCatching { coordinator.deleteGoal(goalId, preview.revisionToken) }.isFailure)

        preview = coordinator.previewGoalDeletion(goalId)
        val sourceMetricLinkId = links.createRule(
            LinkRuleDraft(
                name = "Reviewed Goal metric to another Goal",
                sourceType = LinkSourceType.Metric,
                sourceMetricId = goal.metricId,
                sourceMetric = LinkSourceMetric.NumericValue,
                targetGoalId = otherGoalId,
            ),
        )
        assertTrue(runCatching { coordinator.deleteGoal(goalId, preview.revisionToken) }.isFailure)

        val finalImpact = coordinator.previewGoalDeletion(goalId)
        assertTrue(finalImpact.exists)
        assertEquals(1, finalImpact.milestoneCount)
        assertEquals(1, finalImpact.completedMilestoneCount)
        assertEquals(1, finalImpact.progressEntryCount)
        assertEquals(1, finalImpact.legacyClosureSnapshotCount)
        assertEquals(1, finalImpact.elapsedResetEventCount)
        assertEquals(2, finalImpact.linkRuleCount)
        assertEquals(1, finalImpact.contributionCount)

        val summary = coordinator.deleteGoal(goalId, finalImpact.revisionToken)

        assertTrue(summary.goalDeleted)
        assertEquals(1, summary.milestonesDeleted)
        assertEquals(1, summary.progressEntriesDeleted)
        assertEquals(1, summary.legacyClosureSnapshotsDeleted)
        assertEquals(1, summary.elapsedResetEventsDeleted)
        assertEquals(2, summary.linkRulesDeleted)
        assertEquals(1, summary.contributionsDeleted)
        assertNull(goals.get(goalId))
        assertNull(database.measurementDao().getMetric(goal.metricId))
        assertNotNull(goals.get(otherGoalId))
        assertNull(database.linkDao().getRule(targetLinkId))
        assertNull(database.linkDao().getRule(sourceMetricLinkId))
    }

    @Test fun reviewedGoalDisappearingBeforeDeleteFailsTruthfully() = runBlocking {
        val goalId = goals.create(accumulatingGoal("Reviewed then removed"))
        val preview = coordinator.previewGoalDeletion(goalId)
        assertEquals(1, database.goalDao().deleteGoal(goalId))

        val result = runCatching { coordinator.deleteGoal(goalId, preview.revisionToken) }

        assertTrue(result.isFailure)
        assertFalse(coordinator.previewGoalDeletion(goalId).exists)
    }

    @Test fun goalDeletionPreCommitFailurePreservesGoalAndReconcilesReminderState() = runBlocking {
        val goalId = goals.create(accumulatingGoal("Prepared failure"))
        val reconciliations = AtomicInteger()
        val deletion = DomainDeletionCoordinator(
            database,
            links,
            routines,
            onDeletionPrepared = { _, _ -> error("prepare failed") },
            onDeletionInterrupted = { reconciliations.incrementAndGet() },
        )
        val preview = deletion.previewGoalDeletion(goalId)

        val result = runCatching { deletion.deleteGoal(goalId, preview.revisionToken) }

        assertTrue(result.isFailure)
        assertNotNull(goals.get(goalId))
        assertEquals(1, reconciliations.get())
    }

    @Test fun goalDeletionReportsOrdinaryPostCommitFailuresAsWarnings() = runBlocking {
        val goalId = goals.create(accumulatingGoal("Committed cleanup warnings"))
        val reconciliations = AtomicInteger()
        val deletion = DomainDeletionCoordinator(
            database,
            links,
            routines,
            onDeletionCommitted = { _, _ -> error("reminder cleanup failed") },
            onDeletionInterrupted = { reconciliations.incrementAndGet() },
            rebuildLinksAfterGoalDeletion = { error("link cleanup failed") },
        )
        val preview = deletion.previewGoalDeletion(goalId)

        val summary = deletion.deleteGoal(goalId, preview.revisionToken)

        assertTrue(summary.goalDeleted)
        assertEquals(2, summary.warnings.size)
        assertTrue(summary.warnings.all { it.contains("permanent deletion was committed") })
        assertNull(goals.get(goalId))
        assertEquals(1, reconciliations.get())
    }

    @Test fun goalDeletionCancellationAfterCommitCarriesCommittedSummary() = runBlocking {
        val goalId = goals.create(accumulatingGoal("Committed cancellation"))
        val deletion = DomainDeletionCoordinator(
            database,
            links,
            routines,
            onDeletionCommitted = { _, _ -> throw CancellationException("cancel cleanup") },
        )
        val preview = deletion.previewGoalDeletion(goalId)

        val result = runCatching { deletion.deleteGoal(goalId, preview.revisionToken) }

        val cancellation = result.exceptionOrNull() as CommittedGoalDeletionCancellation
        assertTrue(cancellation.summary.goalDeleted)
        assertNull(goals.get(goalId))
    }

    @Test fun goalDeletionDoesNotConvertFatalPostCommitFailureIntoWarning() = runBlocking {
        val goalId = goals.create(accumulatingGoal("Fatal cleanup"))
        val deletion = DomainDeletionCoordinator(
            database,
            links,
            routines,
            rebuildLinksAfterGoalDeletion = { throw AssertionError("fatal link corruption") },
        )
        val preview = deletion.previewGoalDeletion(goalId)

        val result = runCatching { deletion.deleteGoal(goalId, preview.revisionToken) }

        assertTrue(result.exceptionOrNull() is AssertionError)
        assertNull(goals.get(goalId))
    }

    @Test fun areaDeleteWithItemsRemovesEveryDomainAndItsDependentHistory() = runBlocking {
        val mainId = areas.ensureDefaultArea()
        val areaId = areas.create("Client Delta")
        val taskId = tasks.create(TaskDraft(title = "Prompt source", areaId = areaId, area = "Client Delta"))
        val habitId = habits.create(
            HabitDraft(name = "Read", areaId = areaId, area = "Client Delta", startDate = FixedClock.today()),
        )
        habits.log(habitId, 1.0)
        val goalId = goals.create(accumulatingGoal("Reading goal").copy(areaId = areaId, area = "Client Delta"))
        links.createRule(
            LinkRuleDraft(
                "Read link",
                sourceType = LinkSourceType.Habit,
                sourceEntityId = habitId,
                sourceMetric = LinkSourceMetric.NumericValue,
                targetGoalId = goalId,
                retroactiveFrom = FixedClock.today(),
            ),
            commitBackfill = true,
        )
        links.createTrigger(
            TriggerRuleDraft(
                "Prompt reading",
                LinkSourceType.Task,
                taskId,
                targetType = TriggerTargetType.Habit,
                targetEntityId = habitId,
            ),
        )

        val summary = areaDeletionCoordinator.deleteAreaAndItems(areaId)

        assertEquals(listOf(taskId), summary.taskIds)
        assertEquals(listOf(habitId), summary.habitIds)
        assertEquals(listOf(goalId), summary.goalIds)
        assertEquals(3, summary.total)
        assertEquals(listOf(mainId), areas.areas.first().map { it.id })
        assertTrue(tasks.tasks.first().isEmpty())
        assertTrue(habits.habits.first().isEmpty())
        assertTrue(habits.logs.first().isEmpty())
        assertTrue(goals.goals.first().isEmpty())
        assertTrue(links.rules.first().isEmpty())
        assertTrue(links.contributions.first().isEmpty())
        assertTrue(links.triggerRules.first().isEmpty())
    }

    @Test fun areaDeletionPreparationFailureRollsBackEveryOwnedEntity() = runBlocking {
        areas.ensureDefaultArea()
        val areaId = areas.create("Rollback area")
        val taskId = tasks.create(TaskDraft(title = "Keep me", areaId = areaId, area = "Rollback area"))
        val deletion = AreaDeletionCoordinator(
            database = database,
            areaRepository = areas,
            taskDeletionCoordinator = TaskDeletionCoordinator(database, tasks, links),
            domainDeletionCoordinator = coordinator,
            onDeletionPrepared = { error("prepare failed") },
        )

        assertTrue(runCatching { deletion.deleteAreaAndItems(areaId) }.isFailure)
        assertNotNull(database.measurementDao().getArea(areaId))
        assertNotNull(database.taskDao().getTask(taskId))
    }

    @Test fun areaDeletionPostCommitFailuresReturnWarningsWithoutInvitingReplay() = runBlocking {
        areas.ensureDefaultArea()
        val areaId = areas.create("Committed area")
        val taskId = tasks.create(TaskDraft(title = "Delete me", areaId = areaId, area = "Committed area"))
        val deletion = AreaDeletionCoordinator(
            database = database,
            areaRepository = areas,
            taskDeletionCoordinator = TaskDeletionCoordinator(database, tasks, links),
            domainDeletionCoordinator = DomainDeletionCoordinator(
                database,
                links,
                routines,
                rebuildLinksAfterGoalDeletion = { error("link cleanup failed") },
            ),
            onDeletionCommitted = { error("reminder cleanup failed") },
        )

        val summary = deletion.deleteAreaAndItems(areaId)

        assertEquals(listOf(taskId), summary.taskIds)
        assertEquals(2, summary.warnings.size)
        assertNull(database.measurementDao().getArea(areaId))
        assertNull(database.taskDao().getTask(taskId))
    }

    @Test fun areaDeletionCancellationAfterCommitCarriesTheCommittedSummary() = runBlocking {
        areas.ensureDefaultArea()
        val areaId = areas.create("Cancelled cleanup area")
        val taskId = tasks.create(TaskDraft(title = "Already deleted", areaId = areaId, area = "Cancelled cleanup area"))
        val deletion = AreaDeletionCoordinator(
            database = database,
            areaRepository = areas,
            taskDeletionCoordinator = TaskDeletionCoordinator(database, tasks, links),
            domainDeletionCoordinator = coordinator,
            onDeletionCommitted = { throw CancellationException("cleanup cancelled") },
        )

        val error = runCatching { deletion.deleteAreaAndItems(areaId) }.exceptionOrNull()

        assertTrue(error is CommittedAreaDeletionCancellation)
        assertEquals(listOf(taskId), (error as CommittedAreaDeletionCancellation).summary.taskIds)
        assertNull(database.measurementDao().getArea(areaId))
        assertNull(database.taskDao().getTask(taskId))
    }

    @Test fun exerciseDeletionBlocksAnActiveWorkoutPlacement() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Active bench"))
        val sessionId = gym.startWorkout("In progress")
        gym.addExerciseToWorkout(sessionId, exerciseId)

        val impact = requireNotNull(coordinator.previewExerciseDeletion(exerciseId))
        val error = runCatching {
            coordinator.deleteExercise(exerciseId, impact.revisionToken)
        }.exceptionOrNull()

        assertEquals(1, impact.activePlacements)
        assertTrue(error is IllegalArgumentException)
        assertNotNull(database.gymDao().getExercise(exerciseId))
        assertEquals(1, database.gymDao().getWorkoutExercises(sessionId).size)
    }

    @Test fun routineDeletionBlocksItsActiveWorkout() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Active squat"))
        val routineId = routines.createRoutine(
            RoutineDraft("Active plan", days = listOf(RoutineDayDraft("A", listOf(RoutineExerciseDraft(exerciseId))))),
        )
        val sessionId = routines.startRoutine(routineId)

        val impact = requireNotNull(coordinator.previewRoutineDeletion(routineId))
        val error = runCatching {
            coordinator.deleteRoutine(routineId, impact.revisionToken)
        }.exceptionOrNull()

        assertTrue(impact.activeSession)
        assertTrue(error is IllegalArgumentException)
        assertNotNull(database.routineDao().getRoutine(routineId))
        assertEquals(routineId, database.gymDao().getSession(sessionId)?.sourceRoutineId)
    }

    @Test fun exerciseDeletionRejectsAStaleDependencyPreview() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Reviewed bench"))
        val preview = requireNotNull(coordinator.previewExerciseDeletion(exerciseId))
        routines.createRoutine(
            RoutineDraft("Added later", days = listOf(RoutineDayDraft("A", listOf(RoutineExerciseDraft(exerciseId))))),
        )

        val error = runCatching {
            coordinator.deleteExercise(exerciseId, preview.revisionToken)
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertNotNull(database.gymDao().getExercise(exerciseId))
        assertEquals(1, database.routineDao().getAllExercises().count { it.exerciseId == exerciseId })
    }

    @Test fun routineDeletionRejectsWorkoutHistoryAddedAfterPreview() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Reviewed squat"))
        val routineId = routines.createRoutine(
            RoutineDraft("Reviewed plan", days = listOf(RoutineDayDraft("A", listOf(RoutineExerciseDraft(exerciseId))))),
        )
        val preview = requireNotNull(coordinator.previewRoutineDeletion(routineId))
        val sessionId = routines.startRoutine(routineId)
        gym.finishWorkout(sessionId)

        val error = runCatching {
            coordinator.deleteRoutine(routineId, preview.revisionToken)
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertNotNull(database.routineDao().getRoutine(routineId))
        assertEquals(routineId, database.gymDao().getSession(sessionId)?.sourceRoutineId)
    }

    @Test fun exerciseDeleteReportsExactImpactAndLeavesUnrelatedHistoryUntouched() = runBlocking {
        val categoryId = gym.createCategory("Press")
        val exerciseId = gym.createExercise(ExerciseDraft("Bench", categoryIds = setOf(categoryId)))
        val unaffectedExerciseId = gym.createExercise(ExerciseDraft("Row"))
        gym.createMachine(GymMachineDraft(exerciseId, "Bench station"))
        val sessionId = gym.startWorkout("Push")
        val workoutExerciseId = gym.addExerciseToWorkout(sessionId, exerciseId)
        gym.addSet(workoutExerciseId, WorkoutSetDraft(weight = 80.0, reps = 5, completed = true))
        gym.finishWorkout(sessionId)
        val unaffectedSessionId = gym.startWorkout("Pull")
        val unaffectedPlacementId = gym.addExerciseToWorkout(unaffectedSessionId, unaffectedExerciseId)
        gym.addSet(unaffectedPlacementId, WorkoutSetDraft(weight = 60.0, reps = 8, completed = true))
        gym.finishWorkout(unaffectedSessionId)
        routines.rebuildPersonalRecords(exerciseId)
        routines.createRoutine(
            RoutineDraft(
                "Push plan",
                days = listOf(
                    RoutineDayDraft(
                        "A",
                        listOf(
                            RoutineExerciseDraft(
                                exerciseId,
                                plannedSets = listOf(
                                    WorkoutSetDraft(weight = 75.0, reps = 5),
                                    WorkoutSetDraft(weight = 80.0, reps = 3),
                                ),
                            ),
                            RoutineExerciseDraft(
                                unaffectedExerciseId,
                                alternativeExerciseIds = listOf(exerciseId),
                            ),
                        ),
                    ),
                ),
            ),
        )
        routines.saveGraphPreset("Shared graph", listOf(exerciseId, unaffectedExerciseId), "MaxWeight", "All", "Workout")
        routines.saveGraphPreset("Bench graph", listOf(exerciseId), "MaxWeight", "All", "Workout")

        val impact = requireNotNull(coordinator.previewExerciseDeletion(exerciseId))
        assertEquals(0, impact.activePlacements)
        assertEquals(1, impact.workoutPlacementCount)
        assertEquals(1, impact.workoutSetCount)
        assertEquals(1, impact.routinePlacementCount)
        assertEquals(2, impact.routineSetCount)
        assertEquals(1, impact.routineAlternativeReferenceCount)
        assertEquals(1, impact.graphPresetUpdateCount)
        assertEquals(1, impact.graphPresetDeleteCount)
        assertEquals(2, impact.machineReferenceCount)
        assertEquals(1, impact.categoryReferenceCount)
        assertEquals(routines.personalRecords.first().count { it.exerciseId == exerciseId }, impact.personalRecordCount)

        val summary = coordinator.deleteExercise(exerciseId, impact.revisionToken)

        assertTrue(summary.exerciseDeleted)
        assertEquals(impact.workoutPlacementCount, summary.workoutPlacementsDeleted)
        assertEquals(impact.workoutSetCount, summary.workoutSetsDeleted)
        assertEquals(impact.routinePlacementCount, summary.routinePlacementsDeleted)
        assertEquals(impact.routineSetCount, summary.routineSetsDeleted)
        assertEquals(impact.personalRecordCount, summary.personalRecordsDeleted)
        assertEquals(impact.machineReferenceCount, summary.machineReferencesCleared)
        assertNull(database.gymDao().getExercise(exerciseId))
        assertNotNull(database.gymDao().getExercise(unaffectedExerciseId))
        assertNotNull(database.gymDao().getWorkoutExercise(unaffectedPlacementId))
        assertEquals(1, database.gymDao().getWorkoutSets(unaffectedPlacementId).size)
        assertEquals(listOf(unaffectedExerciseId), routines.graphPresets.first().single().exerciseIds)
        val remainingRoutinePlacement = routines.exercises.first().single()
        assertEquals(unaffectedExerciseId, remainingRoutinePlacement.exerciseId)
        assertTrue(remainingRoutinePlacement.alternativeExerciseIds.isEmpty())
    }

    @Test fun exerciseDeleteDetachesOnlyThatExerciseFromSharedMachine() = runBlocking {
        val rowId = gym.createExercise(ExerciseDraft("Cable row"))
        val pressId = gym.createExercise(ExerciseDraft("Cable press"))
        gym.createMachine(
            GymMachineDraft(
                name = "Shared cable",
                exerciseIds = setOf(rowId, pressId),
            ),
        )

        coordinator.deleteExercise(rowId)

        val machine = gym.machines.first().single()
        assertEquals(setOf(pressId), machine.exerciseIds)
        assertTrue(machine.supportsExercise(pressId))
        assertFalse(machine.supportsExercise(rowId))
    }

    @Test fun routineDeletePreservesWorkoutAndWorkoutDeleteRebuildsDerivedState() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Squat"))
        val routineId = routines.createRoutine(
            RoutineDraft("Legs", days = listOf(RoutineDayDraft("A", listOf(RoutineExerciseDraft(exerciseId))))),
        )
        val sessionId = routines.startRoutine(routineId)
        val workoutExercise = gym.workoutExercises.first().single()
        gym.addSet(workoutExercise.id, WorkoutSetDraft(weight = 100.0, reps = 5, completed = true))
        gym.finishWorkout(sessionId)
        routines.rebuildPersonalRecords(exerciseId)

        val routineImpact = requireNotNull(coordinator.previewRoutineDeletion(routineId))
        assertFalse(routineImpact.activeSession)
        assertEquals(1, routineImpact.dayCount)
        assertEquals(1, routineImpact.routinePlacementCount)
        assertEquals(0, routineImpact.routineSetCount)
        assertEquals(1, routineImpact.preservedWorkoutHistoryCount)

        val routineSummary = coordinator.deleteRoutine(routineId, routineImpact.revisionToken)
        assertTrue(routineSummary.routineDeleted)
        assertEquals(routineImpact.dayCount, routineSummary.daysDeleted)
        assertEquals(routineImpact.routinePlacementCount, routineSummary.routinePlacementsDeleted)
        assertEquals(routineImpact.routineSetCount, routineSummary.routineSetsDeleted)
        assertEquals(1, routineSummary.preservedHistoryReferences)
        assertTrue(routines.routines.first().isEmpty())
        assertNull(gym.sessions.first().single().sourceRoutineId)

        coordinator.deleteWorkout(sessionId)
        assertTrue(gym.sessions.first().isEmpty())
        assertTrue(gym.workoutExercises.first().isEmpty())
        assertTrue(gym.sets.first().isEmpty())
        assertTrue(routines.personalRecords.first().isEmpty())
        assertEquals(1, gym.exercises.first().size)
    }

    @Test fun workoutDeleteReportsExactImpactAndPreservesTrainingMaxDecisionHistory() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Zercher squat"))
        val exercise = requireNotNull(database.gymDao().getExercise(exerciseId))
        val sessionId = gym.startWorkout("5/3/1 Anchor")
        val placementId = gym.addExerciseToWorkout(sessionId, exerciseId)
        gym.addSet(placementId, WorkoutSetDraft(weight = 100.0, reps = 5, completed = true))
        gym.addSet(placementId, WorkoutSetDraft(weight = 110.0, reps = 3, completed = false))
        gym.finishWorkout(sessionId)
        routines.rebuildPersonalRecords(exerciseId)
        val session = requireNotNull(database.gymDao().getSession(sessionId))
        database.routineDao().insertTrainingMaxDecision(
            TrainingMaxDecisionEntity(
                uuid = "tm-decision-${session.uuid}",
                routineUuid = "reviewed-routine",
                sessionUuid = session.uuid,
                exerciseUuid = exercise.uuid,
                exerciseName = exercise.name,
                cycle = 4,
                previousTrainingMax = 120.0,
                appliedDelta = -2.5,
                resultingTrainingMax = 117.5,
                unitId = "kilogram",
                standardDelta = 2.5,
                recommendationCategory = "Decrease",
                recommendationDelta = -2.5,
                confidence = 0.9,
                reasonsText = "Required work passed but AMRAP performance declined",
                engineVersion = "five-three-one-progression/1",
                action = "UseRecommendation",
                createdAtMillis = 1234,
            ),
        )
        val goalId = goals.create(accumulatingGoal("Preserved workout contribution"))
        val linkRuleId = links.createRule(
            LinkRuleDraft(
                name = "Historical workout link",
                sourceType = LinkSourceType.Workout,
                sourceEntityId = sessionId,
                sourceMetric = LinkSourceMetric.Completion,
                targetGoalId = goalId,
            ),
        )
        database.linkDao().upsertContribution(
            ContributionEntity(
                uuid = "workout-contribution-${session.uuid}",
                linkRuleId = linkRuleId,
                sourceEventId = "workout:${session.uuid}:Completion",
                sourceType = LinkSourceType.Workout.name,
                sourceEntityId = sessionId,
                targetGoalId = goalId,
                metricEntryId = null,
                canonicalValue = 1.0,
                localEpochDay = FixedClock.today().toEpochDay(),
                timestampMillis = FixedClock.now().toEpochMilli(),
                excluded = false,
                overrideValue = null,
                explanation = "Historical workout contribution",
                createdAtMillis = FixedClock.now().toEpochMilli(),
                updatedAtMillis = FixedClock.now().toEpochMilli(),
            ),
        )
        val targetTaskId = tasks.create(TaskDraft(title = "Preserved automation target"))
        val triggerId = links.createTrigger(
            TriggerRuleDraft(
                name = "Historical workout automation",
                sourceType = LinkSourceType.Workout,
                sourceEntityId = 0,
                targetType = TriggerTargetType.Task,
                targetEntityId = targetTaskId,
            ),
        )
        database.linkDao().upsertTriggerOccurrence(
            TriggerOccurrenceEntity(
                triggerRuleId = triggerId,
                sourceEventId = "workout:${session.uuid}:Completion",
                availableAtMillis = FixedClock.now().toEpochMilli(),
                deliveredAtMillis = FixedClock.now().toEpochMilli(),
                dismissedAtMillis = FixedClock.now().toEpochMilli(),
                remindAtMillis = null,
                fulfilledEntryId = null,
                sourceSnapshot = "{}",
            ),
        )
        val habitId = habits.create(HabitDraft(name = "Preserved generated check-in", startDate = FixedClock.today()))
        val generatedHabitLogId = habits.log(
            habitId = habitId,
            value = 1.0,
            sourceType = MetricSourceType.Workout,
            sourceId = "trigger:legacy:workout:${session.uuid}:Completion",
        )

        val impact = requireNotNull(coordinator.previewWorkoutDeletion(sessionId))

        assertEquals(session.uuid, impact.sessionUuid)
        assertEquals(1, impact.workoutPlacementCount)
        assertEquals(2, impact.workoutSetCount)
        assertEquals(1, impact.completedSetCount)
        assertTrue(impact.personalRecordCount > 0)
        assertEquals(1, impact.trainingMaxDecisionCount)
        assertEquals(1, impact.contributionCount)
        assertEquals(1, impact.generatedHabitLogCount)
        assertEquals(1, impact.triggerOccurrenceCount)
        val summary = coordinator.deleteWorkout(sessionId, impact.revisionToken)

        assertTrue(summary.workoutDeleted)
        assertEquals(impact.workoutSetCount, summary.workoutSetsDeleted)
        assertEquals(impact.completedSetCount, summary.completedSetsDeleted)
        assertEquals(1, summary.trainingMaxDecisionsPreserved)
        assertEquals(1, summary.contributionsPreserved)
        assertEquals(1, summary.generatedHabitLogsPreserved)
        assertEquals(1, summary.triggerOccurrencesPreserved)
        assertNull(database.gymDao().getSession(sessionId))
        assertTrue(routines.personalRecords.first().none { it.sourceSessionId == sessionId })
        assertEquals(
            listOf("tm-decision-${session.uuid}"),
            database.routineDao().getAllTrainingMaxDecisions().map { it.uuid },
        )
        assertTrue(links.contributions.first().any { it.uuid == "workout-contribution-${session.uuid}" })
        assertTrue(links.triggerOccurrences.first().any { it.sourceEventId == "workout:${session.uuid}:Completion" })
        assertTrue(habits.logs.first().any { it.id == generatedHabitLogId })
        assertNotNull(database.gymDao().getExercise(exerciseId))
    }

    @Test fun workoutDeleteRejectsRecordedHistoryChangedAfterPreview() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Reviewed deadlift"))
        val sessionId = gym.startWorkout("Reviewed workout")
        val placementId = gym.addExerciseToWorkout(sessionId, exerciseId)
        gym.addSet(placementId, WorkoutSetDraft(weight = 180.0, reps = 3, completed = true))
        gym.finishWorkout(sessionId)
        val preview = requireNotNull(coordinator.previewWorkoutDeletion(sessionId))
        gym.updateWorkout(sessionId, "Changed after review", "New note", keepScreenAwake = false)

        val error = runCatching {
            coordinator.deleteWorkout(sessionId, preview.revisionToken)
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals("Changed after review", database.gymDao().getSession(sessionId)?.name)
        assertEquals(1, database.gymDao().getWorkoutSets(placementId).size)
    }

    @Test fun workoutDeleteBlocksAnActiveSessionEvenWithAReviewedRevision() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Active bench"))
        val sessionId = gym.startWorkout("Still training")
        gym.addExerciseToWorkout(sessionId, exerciseId)
        val preview = requireNotNull(coordinator.previewWorkoutDeletion(sessionId))

        val error = runCatching {
            coordinator.deleteWorkout(sessionId, preview.revisionToken)
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertNotNull(database.gymDao().getSession(sessionId))
    }

    @Test fun workoutDeleteOwnsPostCommitReconciliationFailureAsAWarning() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Committed press"))
        val sessionId = gym.startWorkout("Committed workout")
        val placementId = gym.addExerciseToWorkout(sessionId, exerciseId)
        gym.addSet(placementId, WorkoutSetDraft(weight = 60.0, reps = 8, completed = true))
        gym.finishWorkout(sessionId)
        val reconciliations = AtomicInteger()
        val deletion = DomainDeletionCoordinator(
            database,
            links,
            routines,
            rebuildPersonalRecordsAfterExerciseDeletion = {
                reconciliations.incrementAndGet()
                error("Simulated PR rebuild failure")
            },
        )
        val preview = requireNotNull(deletion.previewWorkoutDeletion(sessionId))

        val summary = deletion.deleteWorkout(sessionId, preview.revisionToken)

        assertTrue(summary.workoutDeleted)
        assertEquals(1, reconciliations.get())
        assertTrue(summary.warnings.single().contains("deletion was committed"))
        assertNull(database.gymDao().getSession(sessionId))
    }

    @Test fun machineDeleteRemovesOnlyProfileAndPreservesHistoricalMeaning() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Cable press"))
        val machineId = gym.createMachine(
            GymMachineDraft(exerciseId, "Downtown cable", location = "Public gym"),
        )
        val machineUuid = gym.machines.first().single().uuid
        val sessionId = gym.startWorkout("Push")
        val placementId = gym.addExerciseToWorkout(sessionId, exerciseId, machineId)
        gym.addSet(placementId, WorkoutSetDraft(weight = 50.0, reps = 8, completed = true))
        gym.finishWorkout(sessionId)
        val routineId = routines.createRoutine(
            RoutineDraft(
                "Cable plan",
                days = listOf(
                    RoutineDayDraft(
                        "A",
                        listOf(RoutineExerciseDraft(exerciseId, machineId = machineId)),
                    ),
                ),
            ),
        )
        routines.rebuildPersonalRecords(exerciseId)
        val recordValuesBefore = routines.personalRecords.first().associate { it.uuid to it.value }

        val impact = coordinator.previewMachineDeletion(machineId)
        assertNotNull(impact)
        requireNotNull(impact)
        assertEquals(1, impact.completedSessions)
        assertEquals(1, impact.setCount)
        assertEquals(1, impact.routineReferences)
        assertEquals(0, impact.activePlacements)

        val result = coordinator.deleteMachine(machineId, impact.revisionToken)

        assertTrue(result.deleted)
        assertTrue(gym.machines.first().isEmpty())
        assertEquals(1, gym.sessions.first().size)
        assertEquals(1, gym.workoutExercises.first().size)
        assertEquals(1, gym.sets.first().size)
        val preserved = gym.workoutExercises.first().single()
        assertNull(preserved.machineId)
        assertEquals(machineUuid, preserved.machineProfileUuidSnapshot)
        assertEquals("Downtown cable · Public gym", preserved.machineNameSnapshot)
        val routinePlacement = routines.exercises.first().single()
        assertNull(routinePlacement.machineId)
        assertEquals(RoutineEquipmentBindingState.NeedsEquipment, routinePlacement.equipmentBindingState)
        assertEquals(machineUuid, routinePlacement.machineProfileUuidSnapshot)
        assertEquals("Downtown cable · Public gym", routinePlacement.machineNameSnapshot)
        assertEquals(recordValuesBefore, routines.personalRecords.first().associate { it.uuid to it.value })
        assertTrue(routines.personalRecords.first().all {
            it.machineId == null && it.machineProfileUuidSnapshot == machineUuid
        })
        assertTrue(runCatching { routines.startRoutine(routineId) }.isFailure)
        assertTrue(runCatching { gym.duplicateWorkout(sessionId) }.isFailure)
        assertTrue(runCatching { gym.copyWorkoutExerciseToActive(placementId) }.isFailure)
        assertFalse(gym.sessions.first().any { it.state.name == "Active" })
        assertFalse(coordinator.deleteMachine(machineId).deleted)

        val backup = RoomBackupRepository(database).exportBackup()
        RoomBackupRepository(database).restoreBackup(backup)
        assertTrue(gym.machines.first().isEmpty())
        assertEquals(machineUuid, gym.workoutExercises.first().single().machineProfileUuidSnapshot)
        assertNull(gym.workoutExercises.first().single().machineId)
    }

    @Test fun machineDeleteIsBlockedWhileProfileIsInActiveWorkout() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Active press"))
        val machineId = gym.createMachine(GymMachineDraft(exerciseId, "Active machine"))
        val sessionId = gym.startWorkout("In progress")
        gym.addExerciseToWorkout(sessionId, exerciseId, machineId)

        val impact = coordinator.previewMachineDeletion(machineId)
        assertNotNull(impact)
        requireNotNull(impact)
        assertEquals(1, impact.activePlacements)
        assertTrue(runCatching { coordinator.deleteMachine(machineId, impact.revisionToken) }.isFailure)
        assertEquals(1, gym.machines.first().size)
        assertEquals(machineId, gym.workoutExercises.first().single().machineId)
    }

    @Test fun machineDeleteRejectsAStaleImpactPreview() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Race-safe press"))
        val machineId = gym.createMachine(GymMachineDraft(exerciseId, "Race-safe machine"))
        val impact = requireNotNull(coordinator.previewMachineDeletion(machineId))
        gym.setMachineArchived(machineId, true)

        assertTrue(runCatching { coordinator.deleteMachine(machineId, impact.revisionToken) }.isFailure)
        assertTrue(gym.machines.first().single().archived)
    }

    private fun accumulatingGoal(name: String) = GoalDraft(
        name = name,
        type = GoalType.AccumulateTotal,
        dimension = UnitDimension.Count,
        unitId = "count",
        targetMin = 100.0,
        startDate = FixedClock.today(),
        aggregation = GoalAggregation.Sum,
    )

    private object FixedClock : WhipClock {
        override fun now(): Instant = Instant.parse("2026-08-18T16:00:00Z")
        override fun today(zoneId: ZoneId): LocalDate = LocalDate.of(2026, 8, 18)
    }

    private class SequentialIds : WhipIdGenerator {
        private val count = AtomicInteger()
        override fun nextId(): String = "delete-test-${count.incrementAndGet()}"
    }
}
