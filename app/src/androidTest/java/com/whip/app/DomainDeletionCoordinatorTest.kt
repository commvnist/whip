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
import com.whip.app.data.RoomBackupRepository
import com.whip.app.data.TaskDeletionCoordinator
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
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TriggerRuleDraft
import com.whip.app.domain.TriggerTargetType
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.WorkoutSetDraft
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.first
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

    @Test fun goalDeleteRemovesIncomingLinksContributionsAndOwnedMetric() = runBlocking {
        val habitId = habits.create(HabitDraft(name = "Pages", startDate = FixedClock.today()))
        habits.log(habitId, 12.0)
        val goalId = goals.create(accumulatingGoal("Book"))
        val metricId = goals.goals.first().single().metricId
        links.createRule(
            LinkRuleDraft("Pages to book", sourceType = LinkSourceType.Habit, sourceEntityId = habitId,
                sourceMetric = LinkSourceMetric.NumericValue, targetGoalId = goalId, retroactiveFrom = FixedClock.today()),
            commitBackfill = true,
        )
        assertEquals(1, links.contributions.first().size)

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
