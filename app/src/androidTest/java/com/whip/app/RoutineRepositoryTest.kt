package com.whip.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.data.RoomGymRepository
import com.whip.app.data.RoomRoutineRepository
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.GymMachineDraft
import com.whip.app.domain.MachineLoadType
import com.whip.app.domain.PersonalRecordType
import com.whip.app.domain.RoutineDayDraft
import com.whip.app.domain.RoutineDraft
import com.whip.app.domain.RoutineExerciseDraft
import com.whip.app.domain.WorkoutSetDraft
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoutineRepositoryTest {
    private lateinit var database: WhipDatabase
    private lateinit var gym: RoomGymRepository
    private lateinit var routines: RoomRoutineRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WhipDatabase::class.java,
        ).build()
        val ids = SequentialIds()
        gym = RoomGymRepository(database, FixedClock, ids)
        routines = RoomRoutineRepository(database, FixedClock, ids)
    }

    @After fun tearDown() = database.close()

    @Test
    fun startingRoutineDoesNotMutateTemplate() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Bench"))
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Push",
                days = listOf(
                    RoutineDayDraft(
                        "Day A",
                        listOf(
                            RoutineExerciseDraft(
                                exerciseId,
                                plannedSets = listOf(WorkoutSetDraft(weight = 80.0, reps = 8)),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val templateBefore = routines.sets.first()

        routines.startRoutine(routineId)
        val liveSet = gym.sets.first().single()
        gym.updateSet(liveSet.id, WorkoutSetDraft(weight = 85.0, reps = 8))

        assertEquals(80.0, routines.sets.first().single().draft.weight!!, 0.0)
        assertEquals(templateBefore.single().id, routines.sets.first().single().id)
    }

    @Test
    fun recordRebuildTracksAuditableImprovementsAndHistoricalEdits() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Bench"))
        val sessionId = gym.startWorkout()
        val workoutExerciseId = gym.addExerciseToWorkout(sessionId, exerciseId)
        val first = gym.addSet(workoutExerciseId, WorkoutSetDraft(weight = 80.0, reps = 5, completed = true))
        gym.addSet(workoutExerciseId, WorkoutSetDraft(weight = 90.0, reps = 5, completed = true))

        routines.rebuildPersonalRecords(exerciseId)
        val records = routines.personalRecords.first()
        assertTrue(records.any { it.type == PersonalRecordType.MaxWeight && it.value == 90.0 && it.current })
        assertTrue(records.count { it.type == PersonalRecordType.MaxWeight } >= 2)
        assertTrue(records.any {
            it.type == PersonalRecordType.MaxRepetitionsForWeight && it.value == 5.0 && it.secondaryValue == 90.0
        })
        assertTrue(records.any { it.type == PersonalRecordType.ExerciseWorkoutVolume && it.value == 850.0 && it.current })

        gym.updateSet(first, WorkoutSetDraft(weight = 100.0, reps = 5, completed = true))
        routines.rebuildPersonalRecords(exerciseId)
        assertTrue(routines.personalRecords.first().any {
            it.type == PersonalRecordType.MaxWeight && it.value == 100.0 && it.current
        })
        assertTrue(routines.personalRecords.first().any {
            it.type == PersonalRecordType.ExerciseWorkoutVolume && it.value == 950.0 && it.current
        })
    }

    @Test
    fun routinePreservesMachineIdentityAndNumberedSetting() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Machine press"))
        val machineId = gym.createMachine(
            GymMachineDraft(
                exerciseId = exerciseId,
                name = "Home press",
                loadType = MachineLoadType.Level,
                levelLabel = "pin",
                availableLoads = (1..10).map(Int::toDouble),
            ),
        )
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Home push",
                days = listOf(
                    RoutineDayDraft(
                        "A",
                        listOf(
                            RoutineExerciseDraft(
                                exerciseId = exerciseId,
                                machineId = machineId,
                                plannedSets = listOf(
                                    WorkoutSetDraft(machineLoadValue = 7.0, reps = 8),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        routines.startRoutine(routineId)

        assertEquals(machineId, gym.workoutExercises.first().single().machineId)
        assertEquals(MachineLoadType.Level, gym.workoutExercises.first().single().machineLoadTypeSnapshot)
        assertEquals(7.0, gym.sets.first().single().machineLoadValue!!, 0.0)
        assertEquals(null, gym.sets.first().single().canonicalWeightKg)
    }

    @Test
    fun duplicatePlacementsAndRepRangesSurviveTemplateAndWorkoutCreation() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Range bench"))
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Power and backoff",
                days = listOf(
                    RoutineDayDraft(
                        "A",
                        listOf(
                            RoutineExerciseDraft(
                                exerciseId = exerciseId,
                                plannedSets = listOf(WorkoutSetDraft(weight = 100.0, reps = 3, repsMax = 5)),
                            ),
                            RoutineExerciseDraft(
                                exerciseId = exerciseId,
                                plannedSets = listOf(WorkoutSetDraft(weight = 80.0, reps = 8, repsMax = 10)),
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(listOf(3 to 5, 8 to 10), routines.sets.first().map { it.draft.reps to it.draft.repsMax })

        routines.startRoutine(routineId)
        assertEquals(2, gym.workoutExercises.first().size)
        assertEquals(listOf(5, 10), gym.sets.first().map { it.prescribedRepetitionsMax })
    }

    @Test
    fun routineWaveAndReusableAlternativesSnapshotIntoEachStartedWorkout() = runBlocking {
        val primary = gym.createExercise(ExerciseDraft("Primary press", weightIncrement = 2.5))
        val alternative = gym.createExercise(ExerciseDraft("Alternative press"))
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Wave",
                days = listOf(
                    RoutineDayDraft(
                        "A",
                        listOf(
                            RoutineExerciseDraft(
                                exerciseId = primary,
                                plannedSets = listOf(WorkoutSetDraft(weight = 100.0, reps = 5)),
                                progressionPercentages = listOf(100.0, 90.0),
                                alternativeExerciseIds = listOf(alternative),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val firstSession = routines.startRoutine(routineId)
        assertEquals(100.0, gym.sets.first().single().enteredWeight!!, 0.0)
        assertEquals("Exact load", gym.sets.first().single().prescriptionSourceLabel)
        assertEquals(listOf(alternative), gym.workoutExercises.first().single().alternativeExerciseIdsSnapshot)
        gym.finishWorkout(firstSession)

        routines.startRoutine(routineId)
        val latestPlacement = gym.workoutExercises.first().maxBy { it.id }
        val latestSet = gym.sets.first().single { it.workoutExerciseId == latestPlacement.id }
        assertEquals(90.0, latestSet.enteredWeight!!, 0.0)
        assertEquals("Exact load · cycle 90.0%", latestSet.prescriptionSourceLabel)
        assertEquals(listOf(alternative), latestPlacement.alternativeExerciseIdsSnapshot)
    }

    @Test
    fun archivedMachineRemainsUsableByExistingRoutine() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Archived machine press"))
        val machineId = gym.createMachine(GymMachineDraft(exerciseId, "Known machine"))
        val routineId = routines.createRoutine(
            RoutineDraft(
                "Existing plan",
                days = listOf(RoutineDayDraft("A", listOf(RoutineExerciseDraft(exerciseId, machineId = machineId)))),
            ),
        )
        gym.setMachineArchived(machineId, true)

        routines.startRoutine(routineId)

        assertEquals(machineId, gym.workoutExercises.first().single().machineId)
        assertTrue(gym.machines.first().single().archived)
    }

    @Test
    fun graphPresetCanBeRenamedUpdatedAndDeleted() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Preset bench"))
        val id = routines.saveGraphPreset(
            "Bench trend",
            listOf(exerciseId),
            "MaxWeight",
            "ThreeMonths",
            "Workout",
        )

        routines.updateGraphPreset(id, "Bench year", listOf(exerciseId), "EstimatedOneRepMax", "Year", "Week")
        val updated = routines.graphPresets.first().single()
        assertEquals("Bench year", updated.name)
        assertEquals("EstimatedOneRepMax", updated.metric)
        assertEquals("Year", updated.dateRange)
        assertEquals("Week", updated.aggregation)

        routines.deleteGraphPreset(id)
        assertTrue(routines.graphPresets.first().isEmpty())
    }

    private object FixedClock : WhipClock {
        override fun now(): Instant = Instant.parse("2026-08-17T16:00:00Z")
        override fun today(zoneId: ZoneId): LocalDate = LocalDate.of(2026, 8, 17)
    }

    private class SequentialIds : WhipIdGenerator {
        private val count = AtomicInteger()
        override fun nextId(): String = "routine-test-${count.incrementAndGet()}"
    }
}
