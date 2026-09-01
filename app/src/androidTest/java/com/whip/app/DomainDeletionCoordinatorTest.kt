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
import com.whip.app.data.TriggerFieldMappingEntity
import com.whip.app.data.TriggerOccurrenceEntity
import com.whip.app.data.TriggerRuleConditionEntity
import com.whip.app.data.TriggerRuleEntity
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.GymMachineDraft
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalType
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.LinkRuleDraft
import com.whip.app.domain.LinkSourceMetric
import com.whip.app.domain.LinkSourceType
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

    @Test fun exerciseDeleteCleansHistoryTemplatesRecordsAndGraphPresets() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Bench"))
        val sessionId = gym.startWorkout("Push")
        val workoutExerciseId = gym.addExerciseToWorkout(sessionId, exerciseId)
        gym.addSet(workoutExerciseId, WorkoutSetDraft(weight = 80.0, reps = 5, completed = true))
        gym.finishWorkout(sessionId)
        routines.rebuildPersonalRecords(exerciseId)
        routines.createRoutine(RoutineDraft("Push plan", days = listOf(RoutineDayDraft("A", listOf(RoutineExerciseDraft(exerciseId))))))
        routines.saveGraphPreset("Bench graph", listOf(exerciseId), "MaxWeight", "All", "Workout")

        coordinator.deleteExercise(exerciseId)

        assertTrue(gym.exercises.first().isEmpty())
        assertTrue(gym.workoutExercises.first().isEmpty())
        assertTrue(gym.sets.first().isEmpty())
        assertEquals(1, gym.sessions.first().size)
        assertTrue(routines.exercises.first().isEmpty())
        assertTrue(routines.personalRecords.first().isEmpty())
        assertTrue(routines.graphPresets.first().isEmpty())
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

        val routineSummary = coordinator.deleteRoutine(routineId)
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
