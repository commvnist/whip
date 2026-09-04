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
import com.whip.app.data.RoomMeasurementRepository
import com.whip.app.data.RoomRoutineRepository
import com.whip.app.data.RoomTaskRepository
import com.whip.app.data.RoomTrackRepository
import com.whip.app.data.RoomBackupRepository
import com.whip.app.data.TaskDeletionCoordinator
import com.whip.app.data.CommittedTaskDeletionCancellation
import com.whip.app.data.CommittedGoalDeletionCancellation
import com.whip.app.data.CommittedAreaDeletionCancellation
import com.whip.app.data.GoalElapsedResetEventEntity
import com.whip.app.data.GoalClosureSnapshotEntity
import com.whip.app.data.TrainingMaxDecisionEntity
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.GymMachineDraft
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalMilestoneDraft
import com.whip.app.domain.GoalType
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.MachineLevelDirection
import com.whip.app.domain.MachineLoadType
import com.whip.app.domain.MeasurementSourceType
import com.whip.app.domain.RoutineDayDraft
import com.whip.app.domain.RoutineDraft
import com.whip.app.domain.RoutineExerciseDraft
import com.whip.app.domain.RoutineEquipmentBindingState
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskStepDraft
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
        tasks = RoomTaskRepository(database, FixedClock)
        tracks = RoomTrackRepository(database, FixedClock, ids)
        areas = RoomAreaRepository(database, FixedClock, ids)
        coordinator = DomainDeletionCoordinator(database, routines)
        areaDeletionCoordinator = AreaDeletionCoordinator(
            database,
            areas,
            TaskDeletionCoordinator(database, tasks),
            coordinator,
        )
    }

    @After fun tearDown() = database.close()

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

        val refreshed = requireNotNull(coordinator.previewTrackDeletion(trackId))
        val summary = coordinator.deleteTrack(trackId, refreshed.revisionToken)

        assertTrue(summary.trackDeleted)
        assertEquals(1, summary.fieldsDeleted)
        assertEquals(0, summary.entriesDeleted)
        assertNull(database.trackDao().getTrack(trackId))
    }

      @Test fun reviewedTaskDisappearingBeforeDeleteIsAnOwnedFailure() = runBlocking {
        val taskId = tasks.create(TaskDraft(title = "Reviewed then removed"))
        val deletion = TaskDeletionCoordinator(database, tasks)
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
            onDeletionCommitted = { error("Simulated reminder cleanup failure") },
            onDeletionInterrupted = { reconciliations.incrementAndGet() },
        )
        val preview = deletion.preview(taskId)

        val summary = deletion.delete(taskId, preview.revisionToken)

        assertTrue(summary.taskDeleted)
        assertTrue(summary.warnings.single().contains("permanent deletion was committed"))
        assertNull(tasks.getTask(taskId))
        assertEquals(0, reconciliations.get())
    }

    @Test fun taskDeletionDoesNotConvertFatalPostCommitErrorsIntoOrdinaryWarnings() = runBlocking {
        val taskId = tasks.create(TaskDraft(title = "Fatal cleanup"))
        val deletion = TaskDeletionCoordinator(
            database,
            tasks,
            onDeletionCommitted = { throw AssertionError("fatal cleanup corruption") },
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
            onDeletionCommitted = { throw CancellationException("cancel after delete") },
        )
        val preview = deletion.preview(taskId)

        val result = runCatching { deletion.delete(taskId, preview.revisionToken) }

        assertTrue(result.exceptionOrNull() is CancellationException)
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

      @Test fun goalDeleteRemovesOwnedMeasurementAndPreservesIndependentHabits() = runBlocking {
        val habitId = habits.create(HabitDraft(name = "Pages", startDate = FixedClock.today()))
        habits.log(habitId, 12.0)
        val goalId = goals.create(accumulatingGoal("Book"))
        val measurementId = goals.goals.first().single().measurementId
        coordinator.deleteGoal(goalId)

        assertTrue(goals.goals.first().isEmpty())
        assertNull(database.measurementDao().getMeasurement(measurementId))
        assertEquals(1, habits.habits.first().size)
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
            routines,
            onDeletionCommitted = { _, _ -> error("reminder cleanup failed") },
            onDeletionInterrupted = { reconciliations.incrementAndGet() },
        )
        val preview = deletion.previewGoalDeletion(goalId)

        val summary = deletion.deleteGoal(goalId, preview.revisionToken)

        assertTrue(summary.goalDeleted)
        assertEquals(1, summary.warnings.size)
        assertTrue(summary.warnings.all { it.contains("permanent deletion was committed") })
        assertNull(goals.get(goalId))
        assertEquals(1, reconciliations.get())
    }

    @Test fun goalDeletionCancellationAfterCommitCarriesCommittedSummary() = runBlocking {
        val goalId = goals.create(accumulatingGoal("Committed cancellation"))
        val deletion = DomainDeletionCoordinator(
            database,
            routines,
            onDeletionCommitted = { _, _ -> throw CancellationException("cancel cleanup") },
        )
        val preview = deletion.previewGoalDeletion(goalId)

        val result = runCatching { deletion.deleteGoal(goalId, preview.revisionToken) }

        val cancellation = result.exceptionOrNull() as CommittedGoalDeletionCancellation
        assertTrue(cancellation.summary.goalDeleted)
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
    }

    @Test fun areaDeletionPreparationFailureRollsBackEveryOwnedEntity() = runBlocking {
        areas.ensureDefaultArea()
        val areaId = areas.create("Rollback area")
        val taskId = tasks.create(TaskDraft(title = "Keep me", areaId = areaId, area = "Rollback area"))
        val deletion = AreaDeletionCoordinator(
            database = database,
            areaRepository = areas,
            taskDeletionCoordinator = TaskDeletionCoordinator(database, tasks),
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
            taskDeletionCoordinator = TaskDeletionCoordinator(database, tasks),
            domainDeletionCoordinator = DomainDeletionCoordinator(
                database,
                routines,
                ),
            onDeletionCommitted = { error("reminder cleanup failed") },
        )

        val summary = deletion.deleteAreaAndItems(areaId)

        assertEquals(listOf(taskId), summary.taskIds)
        assertEquals(1, summary.warnings.size)
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
            taskDeletionCoordinator = TaskDeletionCoordinator(database, tasks),
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
        val impact = requireNotNull(coordinator.previewWorkoutDeletion(sessionId))

        assertEquals(session.uuid, impact.sessionUuid)
        assertEquals(1, impact.workoutPlacementCount)
        assertEquals(2, impact.workoutSetCount)
        assertEquals(1, impact.completedSetCount)
        assertTrue(impact.personalRecordCount > 0)
        assertEquals(1, impact.trainingMaxDecisionCount)
        val summary = coordinator.deleteWorkout(sessionId, impact.revisionToken)

        assertTrue(summary.workoutDeleted)
        assertEquals(impact.workoutSetCount, summary.workoutSetsDeleted)
        assertEquals(impact.completedSetCount, summary.completedSetsDeleted)
        assertEquals(1, summary.trainingMaxDecisionsPreserved)
        assertNull(database.gymDao().getSession(sessionId))
        assertTrue(routines.personalRecords.first().none { it.sourceSessionId == sessionId })
        assertEquals(
            listOf("tm-decision-${session.uuid}"),
            database.routineDao().getAllTrainingMaxDecisions().map { it.uuid },
        )
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

    @Test fun deletedNumberedMachineKeepsItsHistoricalStrengthDirection() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Assistance machine"))
        val machineId = gym.createMachine(
            GymMachineDraft(
                exerciseId = exerciseId,
                name = "Counterbalanced machine",
                loadType = MachineLoadType.Level,
                levelLabel = "assistance",
                levelDirection = MachineLevelDirection.HigherNumberLessResistance,
            ),
        )
        val sessionId = gym.startWorkout("Machine work")
        val placementId = gym.addExerciseToWorkout(sessionId, exerciseId, machineId)
        gym.addSet(placementId, WorkoutSetDraft(machineLoadValue = 8.0, reps = 8, completed = true))
        gym.addSet(placementId, WorkoutSetDraft(machineLoadValue = 5.0, reps = 8, completed = true))
        gym.finishWorkout(sessionId)
        routines.rebuildPersonalRecords(exerciseId)
        assertEquals(
            5.0,
            routines.personalRecords.first().single {
                it.type == com.whip.app.domain.PersonalRecordType.MaxMachineSetting && it.current
            }.value,
            0.0,
        )

        coordinator.deleteMachine(machineId)
        val historicalPlacement = gym.workoutExercises.first().single()
        assertNull(historicalPlacement.machineId)
        assertEquals(
            MachineLevelDirection.HigherNumberLessResistance,
            historicalPlacement.machineLevelDirectionSnapshot,
        )

        routines.rebuildPersonalRecords(exerciseId)
        assertEquals(
            5.0,
            routines.personalRecords.first().single {
                it.type == com.whip.app.domain.PersonalRecordType.MaxMachineSetting && it.current
            }.value,
            0.0,
        )
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
