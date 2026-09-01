package com.whip.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.data.RoomGymRepository
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.ExerciseTrackingType
import com.whip.app.domain.GymMachineDraft
import com.whip.app.domain.MachineLevelDirection
import com.whip.app.domain.MachineLoadType
import com.whip.app.domain.LoadInterpretation
import com.whip.app.domain.RoutineMainWorkScheme
import com.whip.app.domain.RoutineOptionalWorkKind
import com.whip.app.domain.RoutineWorkSection
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.domain.WorkoutExerciseOutcome
import com.whip.app.domain.WorkoutSetClassification
import com.whip.app.domain.WorkoutSetRemovalReason
import com.whip.app.domain.WorkoutGroupType
import com.whip.app.domain.WorkoutSetDraft
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GymRepositoryTest {
    private lateinit var database: WhipDatabase
    private lateinit var repository: RoomGymRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WhipDatabase::class.java,
        )
            .addCallback(WhipDatabase.integrityGuardCallback)
            .build()
        repository = RoomGymRepository(database, FixedClock, SequentialIds())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun emptyLibraryThenThreeByEightAtEightyKgPersists() = runBlocking {
        assertTrue(repository.exercises.first().isEmpty())

        val exerciseId = repository.createExercise(
            ExerciseDraft(name = "Flat Barbell Bench Press"),
        )
        val sessionId = repository.startWorkout(name = "Push day")
        val workoutExerciseId = repository.addExerciseToWorkout(sessionId, exerciseId)
        repeat(3) {
            repository.addSet(
                workoutExerciseId,
                WorkoutSetDraft(weight = 80.0, reps = 8, completed = true),
            )
        }

        assertEquals("Flat Barbell Bench Press", repository.exercises.first().single().name)
        assertEquals(WorkoutSessionState.Active, repository.sessions.first().single().state)
        assertEquals(3, repository.sets.first().count { it.completed })
        assertTrue(repository.sets.first().all { it.canonicalWeightKg == 80.0 })

        repository.finishWorkout(sessionId)
        assertEquals(WorkoutSessionState.Finished, repository.sessions.first().single().state)
    }

    @Test
    fun workoutSnapshotPairsAnInitialSetWithItsCommittedSessionRevision() = runBlocking {
        val exerciseId = repository.createExercise(ExerciseDraft(name = "Goblet squat"))
        val sessionId = repository.startWorkout(name = "Coherent workout")

        val addition = repository.addExerciseWithInitialSetToWorkout(sessionId, exerciseId)
        val snapshot = repository.workoutSnapshot.first { current ->
            current.sets.any { it.id == addition.initialSetId }
        }

        assertEquals(addition.workoutExerciseId, snapshot.sets.single().workoutExerciseId)
        assertEquals(2L, snapshot.sessions.single { it.id == sessionId }.workoutRevision)
        assertEquals(addition.workoutExerciseId, snapshot.workoutExercises.single().id)
    }

    @Test
    fun exerciseDefaultsRejectInvalidRestAndPlateValuesBeforePersistence() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.createExercise(
                    ExerciseDraft(name = "Bench press", defaultRestSeconds = -30),
                )
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.createExercise(
                    ExerciseDraft(name = "Bench press", availablePlatesKg = listOf(20.0, 0.0)),
                )
            }
        }
        assertTrue(runBlocking { repository.exercises.first().isEmpty() })
    }

    @Test
    fun machineCanBeCreatedUnattachedThenLinkedToMultipleExercises() = runBlocking {
        val rowId = repository.createExercise(ExerciseDraft(name = "Cable row"))
        val pressId = repository.createExercise(ExerciseDraft(name = "Cable press"))
        val unrelatedId = repository.createExercise(ExerciseDraft(name = "Leg extension"))
        val machineId = repository.createMachine(
            GymMachineDraft(
                name = "Dual cable",
                loadType = MachineLoadType.Level,
                levelLabel = "position",
                availableLoads = listOf(1.0, 2.0, 3.0),
                loadInterpretation = LoadInterpretation.OrdinalSetting,
                levelDirection = MachineLevelDirection.HigherNumberLessResistance,
            ),
        )

        assertTrue(repository.machines.first().single().exerciseIds.isEmpty())

        repository.updateMachine(
            machineId,
            GymMachineDraft(
                name = "Dual cable",
                exerciseIds = setOf(rowId, pressId),
                loadType = MachineLoadType.Level,
                levelLabel = "position",
                availableLoads = listOf(1.0, 2.0, 3.0),
                loadInterpretation = LoadInterpretation.OrdinalSetting,
                levelDirection = MachineLevelDirection.HigherNumberLessResistance,
            ),
        )
        val linked = repository.machines.first().single()
        assertEquals(setOf(rowId, pressId), linked.exerciseIds)
        assertEquals(MachineLevelDirection.HigherNumberLessResistance, linked.levelDirection)

        val sessionId = repository.startWorkout("Cable day")
        repository.addExerciseToWorkout(sessionId, rowId, machineId)
        repository.addExerciseToWorkout(sessionId, pressId, machineId)
        assertTrue(runCatching { repository.addExerciseToWorkout(sessionId, unrelatedId, machineId) }.isFailure)
        assertEquals(2, repository.workoutExercises.first().size)
    }

    @Test
    fun poundsNormalizeAndSetDeleteCanBeUndone() = runBlocking {
        val exerciseId = repository.createExercise(ExerciseDraft(name = "Press", weightUnitId = "pound"))
        val sessionId = repository.startWorkout()
        val workoutExerciseId = repository.addExerciseToWorkout(sessionId, exerciseId)
        val setId = repository.addSet(
            workoutExerciseId,
            WorkoutSetDraft(weight = 100.0, weightUnitId = "pound", reps = 5),
        )

        assertEquals(45.359237, repository.sets.first().single().canonicalWeightKg!!, 0.000001)
        repository.deleteSet(setId)
        assertNotNull(repository.sets.first().single().deletedAtMillis)
        repository.undoDeleteSet(setId)
        assertEquals(null, repository.sets.first().single().deletedAtMillis)
    }

    @Test
    fun poundEquipmentAndMachineStackValuesPersistWithoutDisplayRounding() = runBlocking {
        val exerciseId = repository.createExercise(
            ExerciseDraft(
                name = "Garage bench",
                weightUnitId = "pound",
                weightIncrement = 5.0,
                barWeightKg = 45.0 * 0.45359237,
                availablePlatesKg = listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5).map { it * 0.45359237 },
            ),
        )
        repository.createMachine(
            GymMachineDraft(
                exerciseId = exerciseId,
                name = "Garage stack",
                loadType = MachineLoadType.Level,
                levelLabel = "pin",
                availableLoads = (1..10).map(Int::toDouble),
            ),
        )

        val exercise = repository.exercises.first().single()
        val machine = repository.machines.first().single()
        assertEquals("pound", exercise.weightUnitId)
        assertEquals(5.0, exercise.weightIncrement, 0.0)
        assertEquals(45.0 * 0.45359237, exercise.barWeightKg!!, 0.0000001)
        assertEquals((1..10).map(Int::toDouble), machine.availableLoads)
    }

    @Test
    fun activeSessionAndTimerDeadlineAreStoredData() = runBlocking {
        val sessionId = repository.startWorkout()
        repository.startRestTimer(sessionId, 90)

        val session = repository.sessions.first().single()
        assertEquals(90, session.restTimerDurationSeconds)
        assertEquals(FixedClock.now().toEpochMilli() + 90_000L, session.restTimerDeadlineMillis)
        assertTrue(session.restTimerCleanupPending)

        repository.acknowledgeRestTimerCleanup(sessionId, session.restTimerRevision)
        assertFalse(repository.sessions.first().single().restTimerCleanupPending)

        repository.stopRestTimer(sessionId)
        val stopped = repository.sessions.first().single()
        assertEquals(null, stopped.restTimerDeadlineMillis)
        assertTrue(stopped.restTimerCleanupPending)

        repository.acknowledgeRestTimerCleanup(sessionId, session.restTimerRevision)
        assertTrue(repository.sessions.first().single().restTimerCleanupPending)
        repository.acknowledgeRestTimerCleanup(sessionId, stopped.restTimerRevision)
        assertFalse(repository.sessions.first().single().restTimerCleanupPending)
    }

    @Test
    fun terminalSessionKeepsTimerCleanupPendingUntilSchedulerAcknowledgesIt() = runBlocking {
        val sessionId = repository.startWorkout()
        repository.startRestTimer(sessionId, 30)

        repository.finishWorkout(sessionId)

        val finished = repository.sessions.first().single()
        assertEquals(WorkoutSessionState.Finished, finished.state)
        assertEquals(null, finished.restTimerDeadlineMillis)
        assertEquals(null, finished.restTimerDurationSeconds)
        assertTrue(finished.restTimerCleanupPending)

        repository.acknowledgeRestTimerCleanup(sessionId, finished.restTimerRevision)
        assertFalse(repository.sessions.first().single().restTimerCleanupPending)
    }

    @Test
    fun restTimerDeliveryCompletesOnlyTheExactScheduledRevisionAndDeadline() = runBlocking {
        val sessionId = repository.startWorkout()
        repository.startRestTimer(sessionId, 30)
        val scheduled = repository.sessions.first().single()
        val deadline = requireNotNull(scheduled.restTimerDeadlineMillis)

        assertFalse(
            repository.completeRestTimerDelivery(
                sessionId,
                scheduled.restTimerRevision + 1,
                deadline,
            ),
        )
        assertEquals(deadline, repository.sessions.first().single().restTimerDeadlineMillis)
        assertTrue(
            repository.completeRestTimerDelivery(
                sessionId,
                scheduled.restTimerRevision,
                deadline,
            ),
        )
        val delivered = repository.sessions.first().single()
        assertEquals(null, delivered.restTimerDeadlineMillis)
        assertTrue(delivered.restTimerCleanupPending)
        assertEquals(scheduled.restTimerRevision + 1, delivered.restTimerRevision)
        assertFalse(
            repository.completeRestTimerDelivery(
                sessionId,
                scheduled.restTimerRevision,
                deadline,
            ),
        )
    }

    @Test
    fun restTimerAdjustmentsPreserveTheDisplayedSecondCount() = runBlocking {
        val sessionId = repository.startWorkout()
        repository.startRestTimer(sessionId, 300)

        repository.adjustRestTimer(sessionId, 15)
        assertEquals(315, repository.sessions.first().single().restTimerDurationSeconds)

        repository.adjustRestTimer(sessionId, -15)
        assertEquals(300, repository.sessions.first().single().restTimerDurationSeconds)
    }

    @Test
    fun workoutRestOverrideControlsAutomaticRestWithoutChangingExerciseDefault() = runBlocking {
        val exerciseId = repository.createExercise(
            ExerciseDraft(name = "Bench press", defaultRestSeconds = 180),
        )
        val sessionId = repository.startWorkout()
        val placementId = repository.addExerciseToWorkout(sessionId, exerciseId)
        val setId = repository.addSet(
            placementId,
            WorkoutSetDraft(weight = 80.0, reps = 5),
        )

        repository.setSetCompleted(
            id = setId,
            completed = true,
            autoStartRest = true,
            restOverrideSeconds = 75,
        )

        val session = repository.sessions.first().single()
        assertEquals(75, session.restTimerDurationSeconds)
        assertEquals(FixedClock.now().toEpochMilli() + 75_000L, session.restTimerDeadlineMillis)
        assertEquals(180, repository.exercises.first().single().defaultRestSeconds)
    }

    @Test
    fun quickSaveCommitsSetAppendTimerAndRevisionAsOneReviewedMutation() = runBlocking {
        val exerciseId = repository.createExercise(ExerciseDraft(name = "Bench", defaultRestSeconds = 180))
        val sessionId = repository.startWorkout("Atomic quick save")
        val placementId = repository.addExerciseToWorkout(sessionId, exerciseId)
        val setId = repository.addSet(placementId, WorkoutSetDraft(weight = 100.0, reps = 5))
        val before = repository.sessions.first().single { it.id == sessionId }
        val source = repository.sets.first().single { it.id == setId }

        assertTrue(
            runCatching {
                repository.saveQuickSet(
                    id = setId,
                    expectedSetUuid = source.uuid,
                    expectedSetUpdatedAtMillis = source.updatedAtMillis + 1,
                    expectedWorkoutRevision = before.workoutRevision,
                    draft = WorkoutSetDraft(weight = 105.0, reps = 5),
                    addNext = true,
                    autoStartRest = true,
                    restOverrideSeconds = 300,
                )
            }.isFailure,
        )
        assertFalse(repository.sets.first().single { it.id == setId }.completed)

        val receipt = repository.saveQuickSet(
            id = setId,
            expectedSetUuid = source.uuid,
            expectedSetUpdatedAtMillis = source.updatedAtMillis,
            expectedWorkoutRevision = before.workoutRevision,
            draft = WorkoutSetDraft(weight = 105.0, reps = 5),
            addNext = true,
            autoStartRest = true,
            restOverrideSeconds = 300,
        )

        val storedSets = repository.sets.first().sortedBy { it.position }
        assertEquals(2, storedSets.size)
        assertTrue(storedSets.first().completed)
        assertEquals(receipt.appendedSetId, storedSets.last().id)
        assertFalse(storedSets.last().completed)
        assertEquals(RoutineWorkSection.Unspecified, storedSets.last().workSectionSnapshot)
        assertEquals(RoutineOptionalWorkKind.None, storedSets.last().optionalWorkKindSnapshot)
        assertFalse(storedSets.last().requiredForProgressionSnapshot)
        val after = repository.sessions.first().single { it.id == sessionId }
        assertEquals(before.workoutRevision + 1, after.workoutRevision)
        assertEquals(300, after.restTimerDurationSeconds)
        assertEquals(FixedClock.now().toEpochMilli() + 300_000L, after.restTimerDeadlineMillis)
    }

    @Test
    fun concurrentQuickSavesAdmitOnlyOneCommitForTheReviewedBoundary() = runBlocking {
        val exerciseId = repository.createExercise(ExerciseDraft(name = "Press"))
        val sessionId = repository.startWorkout("Concurrent quick save")
        val placementId = repository.addExerciseToWorkout(sessionId, exerciseId)
        val setId = repository.addSet(placementId, WorkoutSetDraft(weight = 50.0, reps = 5))
        val reviewedSession = repository.sessions.first().single { it.id == sessionId }
        val reviewedSet = repository.sets.first().single { it.id == setId }

        val outcomes = listOf(52.5, 55.0).map { weight ->
            async(Dispatchers.Default) {
                runCatching {
                    repository.saveQuickSet(
                        id = setId,
                        expectedSetUuid = reviewedSet.uuid,
                        expectedSetUpdatedAtMillis = reviewedSet.updatedAtMillis,
                        expectedWorkoutRevision = reviewedSession.workoutRevision,
                        draft = WorkoutSetDraft(weight = weight, reps = 5),
                        addNext = false,
                        autoStartRest = false,
                    )
                }
            }
        }.awaitAll()

        assertEquals(1, outcomes.count { it.isSuccess })
        assertEquals(1, outcomes.count { it.isFailure })
        assertTrue(repository.sets.first().single { it.id == setId }.completed)
        assertEquals(
            reviewedSession.workoutRevision + 1,
            repository.sessions.first().single { it.id == sessionId }.workoutRevision,
        )
    }

    @Test
    fun finishRejectsAWorkoutGraphThatChangedAfterReview() = runBlocking {
        val exerciseId = repository.createExercise(ExerciseDraft(name = "Deadlift"))
        val sessionId = repository.startWorkout("Revision boundary")
        val placementId = repository.addExerciseToWorkout(sessionId, exerciseId)
        val reviewedSession = repository.sessions.first().single { it.id == sessionId }
        val reviewedRevision = reviewedSession.workoutRevision
        val wrongIdentity = runCatching {
            repository.finishWorkout(
                sessionId,
                expectedWorkoutRevision = reviewedRevision,
                expectedSessionUuid = "different-workout",
            )
        }.exceptionOrNull()
        assertTrue(wrongIdentity is IllegalArgumentException)
        assertTrue(wrongIdentity?.message.orEmpty().contains("identity changed"))
        assertEquals(WorkoutSessionState.Active, repository.sessions.first().single { it.id == sessionId }.state)

        repository.addSet(placementId, WorkoutSetDraft(weight = 180.0, reps = 5, completed = true))

        assertTrue(
            runCatching {
                repository.finishWorkout(
                    sessionId,
                    expectedWorkoutRevision = reviewedRevision,
                    expectedSessionUuid = reviewedSession.uuid,
                )
            }.isFailure,
        )
        assertEquals(WorkoutSessionState.Active, repository.sessions.first().single { it.id == sessionId }.state)

        val latestRevision = repository.sessions.first().single { it.id == sessionId }.workoutRevision
        repository.finishWorkout(
            sessionId,
            expectedWorkoutRevision = latestRevision,
            expectedSessionUuid = reviewedSession.uuid,
        )
        assertEquals(WorkoutSessionState.Finished, repository.sessions.first().single { it.id == sessionId }.state)
    }

    @Test
    fun substitutingBeforeAnyCompletedSetRetiresPlacementAndPreservesItsDraftOutcome() = runBlocking {
        val press = repository.createExercise(ExerciseDraft(name = "Press"))
        val incline = repository.createExercise(ExerciseDraft(name = "Incline press"))
        val session = repository.startWorkout()
        val original = repository.addExerciseToWorkout(session, press)
        repository.addSet(original, WorkoutSetDraft(weight = 60.0, reps = 8, planned = true, completed = false))

        val replacement = repository.substituteWorkoutExercise(original, incline)

        val placements = repository.workoutExercises.first().associateBy { it.id }
        assertEquals(WorkoutExerciseOutcome.Substituted, placements.getValue(original).outcome)
        assertEquals(placements.getValue(replacement).uuid, placements.getValue(original).replacementWorkoutExerciseUuid)
        assertEquals(WorkoutExerciseOutcome.Active, placements.getValue(replacement).outcome)
        assertEquals(incline, placements.getValue(replacement).exerciseId)
        assertEquals("Substitution for Press", placements.getValue(replacement).notes)
        val retiredSet = repository.sets.first().single { it.workoutExerciseId == original }
        assertNotNull(retiredSet.deletedAtMillis)
        assertEquals(WorkoutSetRemovalReason.ExerciseSubstituted, retiredSet.removalReason)
        assertEquals(1, repository.sets.first().count { it.workoutExerciseId == replacement })
    }

    @Test
    fun substitutingAfterCompletedSetStillReplacesPlacementAndSnapshotsNewMachine() = runBlocking {
        val press = repository.createExercise(ExerciseDraft(name = "Press"))
        val cable = repository.createExercise(ExerciseDraft(name = "Cable press"))
        val machine = repository.createMachine(
            GymMachineDraft(
                exerciseId = cable,
                name = "Cable tower",
                location = "Public gym",
                loadType = MachineLoadType.Level,
                levelLabel = "pin",
                availableLoads = (1..10).map(Int::toDouble),
                loadInterpretation = LoadInterpretation.OrdinalSetting,
            ),
        )
        val session = repository.startWorkout()
        val original = repository.addExerciseToWorkout(session, press)
        val completed = repository.addSet(original, WorkoutSetDraft(weight = 60.0, reps = 8, completed = true))
        val incomplete = repository.addSet(original, WorkoutSetDraft(weight = 65.0, reps = 8, planned = true, completed = false))

        val replacement = repository.substituteWorkoutExercise(original, cable, machine)

        val placements = repository.workoutExercises.first().associateBy { it.id }
        assertEquals(WorkoutExerciseOutcome.Substituted, placements.getValue(original).outcome)
        assertEquals(WorkoutExerciseOutcome.Active, placements.getValue(replacement).outcome)
        assertEquals("Cable tower · Public gym", placements.getValue(replacement).machineNameSnapshot)
        assertEquals("pin", placements.getValue(replacement).machineLevelLabelSnapshot)
        val storedSets = repository.sets.first().associateBy { it.id }
        assertTrue(storedSets.getValue(completed).completed)
        assertEquals(null, storedSets.getValue(completed).deletedAtMillis)
        assertNotNull(storedSets.getValue(incomplete).deletedAtMillis)
        assertEquals(WorkoutSetRemovalReason.ExerciseSubstituted, storedSets.getValue(incomplete).removalReason)
        assertEquals(1, storedSets.values.count { it.workoutExerciseId == replacement })
    }

    @Test
    fun removingAGroupMemberKeepsAValidPairThenDissolvesTheLastSingleton() = runBlocking {
        val firstExercise = repository.createExercise(ExerciseDraft(name = "Bench press"))
        val secondExercise = repository.createExercise(ExerciseDraft(name = "Row"))
        val thirdExercise = repository.createExercise(ExerciseDraft(name = "Shoulder press"))
        val session = repository.startWorkout("Upper body")
        val firstPlacement = repository.addExerciseToWorkout(session, firstExercise)
        val secondPlacement = repository.addExerciseToWorkout(session, secondExercise)
        val thirdPlacement = repository.addExerciseToWorkout(session, thirdExercise)
        val groupId = repository.createGroup(
            session,
            "Superset",
            WorkoutGroupType.Circuit,
            listOf(firstPlacement, secondPlacement, thirdPlacement),
        )

        assertEquals("Circuit", repository.groups.first().single().name)

        repository.removeWorkoutExerciseFromGroup(firstPlacement)

        val validPair = repository.workoutExercises.first().associateBy { it.id }
        assertEquals(null, validPair.getValue(firstPlacement).groupId)
        assertEquals(groupId, validPair.getValue(secondPlacement).groupId)
        assertEquals(groupId, validPair.getValue(thirdPlacement).groupId)
        assertEquals(groupId, repository.groups.first().single().id)

        repository.removeWorkoutExerciseFromGroup(secondPlacement)

        assertTrue(repository.workoutExercises.first().all { it.groupId == null })
        assertTrue(repository.groups.first().isEmpty())
    }

    @Test
    fun groupingNonAdjacentExercisesPersistsOneContiguousBlock() = runBlocking {
        val exerciseIds = listOf("Bench", "Row", "Press", "Curl").map { name ->
            repository.createExercise(ExerciseDraft(name = name))
        }
        val session = repository.startWorkout("Block order")
        val placements = exerciseIds.map { exerciseId -> repository.addExerciseToWorkout(session, exerciseId) }

        val groupId = repository.createGroup(
            session,
            "Superset",
            WorkoutGroupType.Superset,
            listOf(placements[0], placements[2]),
        )

        val stored = repository.workoutExercises.first().sortedBy { it.position }
        assertEquals(listOf(placements[0], placements[2], placements[1], placements[3]), stored.map { it.id })
        assertEquals(listOf(0, 1, 2, 3), stored.map { it.position })
        assertEquals(setOf(placements[0], placements[2]), stored.filter { it.groupId == groupId }.map { it.id }.toSet())
    }

    @Test
    fun regroupingDissolvesTheOldSingletonAndKeepsTheNewGroupContiguous() = runBlocking {
        val exerciseIds = listOf("Bench", "Row", "Press", "Curl").map { name ->
            repository.createExercise(ExerciseDraft(name = name))
        }
        val session = repository.startWorkout("Regroup")
        val placements = exerciseIds.map { exerciseId -> repository.addExerciseToWorkout(session, exerciseId) }
        val oldGroupId = repository.createGroup(
            session,
            "First pair",
            WorkoutGroupType.Superset,
            listOf(placements[0], placements[1]),
        )

        val newGroupId = repository.createGroup(
            session,
            "Circuit",
            WorkoutGroupType.Circuit,
            listOf(placements[1], placements[3]),
        )

        val stored = repository.workoutExercises.first().sortedBy { it.position }
        assertEquals(listOf(placements[0], placements[1], placements[3], placements[2]), stored.map { it.id })
        assertEquals(null, stored.single { it.id == placements[0] }.groupId)
        assertEquals(setOf(placements[1], placements[3]), stored.filter { it.groupId == newGroupId }.map { it.id }.toSet())
        assertFalse(repository.groups.first().any { it.id == oldGroupId })
    }

    @Test
    fun regroupingOneMemberKeepsTheSurvivingOldGroupContiguous() = runBlocking {
        val exerciseIds = listOf("A", "B", "C", "D").map { name ->
            repository.createExercise(ExerciseDraft(name = name))
        }
        val session = repository.startWorkout("Surviving group")
        val placements = exerciseIds.map { exerciseId -> repository.addExerciseToWorkout(session, exerciseId) }
        val oldGroupId = repository.createGroup(
            session,
            "Old circuit",
            WorkoutGroupType.Circuit,
            listOf(placements[0], placements[1], placements[2]),
        )

        val newGroupId = repository.createGroup(
            session,
            "New pair",
            WorkoutGroupType.Superset,
            listOf(placements[1], placements[3]),
        )

        val stored = repository.workoutExercises.first().sortedBy { it.position }
        assertEquals(listOf(placements[0], placements[2], placements[1], placements[3]), stored.map { it.id })
        assertEquals(setOf(placements[0], placements[2]), stored.filter { it.groupId == oldGroupId }.map { it.id }.toSet())
        assertEquals(setOf(placements[1], placements[3]), stored.filter { it.groupId == newGroupId }.map { it.id }.toSet())
    }

    @Test
    fun normalizationRepairsLegacySplitAndSingletonWorkoutGroups() = runBlocking {
        val exerciseIds = listOf("A", "B", "C").map { name ->
            repository.createExercise(ExerciseDraft(name = name))
        }
        val session = repository.startWorkout("Legacy repair")
        val placements = exerciseIds.map { exerciseId -> repository.addExerciseToWorkout(session, exerciseId) }
        val groupId = repository.createGroup(
            session,
            "Pair",
            WorkoutGroupType.Superset,
            listOf(placements[0], placements[2]),
        )
        repository.reorderWorkoutExercises(session, listOf(placements[0], placements[1], placements[2]))

        repository.normalizeWorkoutGroups(session)

        assertEquals(
            listOf(placements[0], placements[2], placements[1]),
            repository.workoutExercises.first().sortedBy { it.position }.map { it.id },
        )

        val second = database.gymDao().getWorkoutExercise(placements[2])!!
        database.gymDao().updateWorkoutExercise(second.copy(groupId = null))
        repository.normalizeWorkoutGroups(session)

        assertEquals(null, repository.workoutExercises.first().single { it.id == placements[0] }.groupId)
        assertFalse(repository.groups.first().any { it.id == groupId })
    }

    @Test
    fun deletingAWorkoutExerciseDissolvesItsRemainingSingletonGroup() = runBlocking {
        val firstExercise = repository.createExercise(ExerciseDraft(name = "Bench"))
        val secondExercise = repository.createExercise(ExerciseDraft(name = "Row"))
        val session = repository.startWorkout("Delete grouped exercise")
        val firstPlacement = repository.addExerciseToWorkout(session, firstExercise)
        val secondPlacement = repository.addExerciseToWorkout(session, secondExercise)
        repository.createGroup(
            session,
            "Superset",
            WorkoutGroupType.Superset,
            listOf(firstPlacement, secondPlacement),
        )

        repository.removeWorkoutExercise(firstPlacement)

        val placements = repository.workoutExercises.first().associateBy { it.id }
        assertEquals(WorkoutExerciseOutcome.Removed, placements.getValue(firstPlacement).outcome)
        assertEquals(null, placements.getValue(secondPlacement).groupId)
        assertTrue(repository.groups.first().isEmpty())
    }

    @Test
    fun archivingExerciseRetainsWorkoutHistory() = runBlocking {
        val exerciseId = repository.createExercise(ExerciseDraft(name = "User movement"))
        val sessionId = repository.startWorkout()
        repository.addExerciseToWorkout(sessionId, exerciseId)
        repository.finishWorkout(sessionId)
        repository.setExerciseArchived(exerciseId, true)

        assertTrue(repository.exercises.first().single().archived)
        assertFalse(repository.workoutExercises.first().isEmpty())
    }

    @Test
    fun discardedWorkoutCanBeRestoredWithItsHistory() = runBlocking {
        val exerciseId = repository.createExercise(ExerciseDraft(name = "Restorable movement"))
        val sessionId = repository.startWorkout()
        val workoutExerciseId = repository.addExerciseToWorkout(sessionId, exerciseId)
        repository.addSet(workoutExerciseId, WorkoutSetDraft(weight = 50.0, reps = 5, completed = true))

        repository.discardWorkout(sessionId)
        val discarded = repository.sessions.first().single()
        assertTrue(discarded.archived)
        assertEquals(WorkoutSessionState.Discarded, discarded.state)

        repository.restoreWorkout(sessionId)
        val restored = repository.sessions.first().single()
        assertFalse(restored.archived)
        assertEquals(WorkoutSessionState.Finished, restored.state)
        assertEquals(1, repository.sets.first().size)
    }

    @Test
    fun uniqueMachinesKeepOrdinalAndMassLoadsSemanticallySeparate() = runBlocking {
        val exerciseId = repository.createExercise(ExerciseDraft(name = "Chest press"))
        val homeMachineId = repository.createMachine(
            GymMachineDraft(
                exerciseId = exerciseId,
                name = "Home multi-gym",
                location = "Home",
                loadType = MachineLoadType.Level,
                levelLabel = "pin",
                availableLoads = (1..10).map(Int::toDouble),
            ),
        )
        val publicMachineId = repository.createMachine(
            GymMachineDraft(
                exerciseId = exerciseId,
                name = "Public chest press",
                location = "Downtown",
                loadType = MachineLoadType.Mass,
                unitId = "pound",
                availableLoads = listOf(50.0, 60.0, 70.0),
            ),
        )

        val homeSession = repository.startWorkout("Home")
        val homePlacement = repository.addExerciseToWorkout(homeSession, exerciseId, homeMachineId)
        repository.addSet(
            homePlacement,
            WorkoutSetDraft(machineLoadValue = 7.0, reps = 8, completed = true),
        )
        val homeSet = repository.sets.first().single()
        assertEquals(7.0, homeSet.machineLoadValue!!, 0.0)
        assertEquals(null, homeSet.canonicalWeightKg)
        assertEquals("Home multi-gym · Home", repository.workoutExercises.first().single().machineNameSnapshot)
        repository.finishWorkout(homeSession)

        val publicSession = repository.startWorkout("Public")
        val publicPlacement = repository.addExerciseToWorkout(publicSession, exerciseId, publicMachineId)
        repository.addSet(
            publicPlacement,
            WorkoutSetDraft(weight = 50.0, weightUnitId = "pound", machineLoadValue = 50.0, reps = 8),
        )
        val publicSet = repository.sets.first().first { it.workoutExerciseId == publicPlacement }
        assertEquals(22.6796185, publicSet.canonicalWeightKg!!, 0.0000001)
        assertEquals(50.0, publicSet.machineLoadValue!!, 0.0)

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.setWorkoutExerciseMachine(publicPlacement, homeMachineId) }
        }
        Unit
    }

    @Test
    fun perHandAndPerSideLoadsNormalizeOnceAndKeepPlacementMeaning() = runBlocking {
        val dumbbellDraft = ExerciseDraft(
            name = "Dumbbell press",
            weightUnitId = "pound",
            loadInterpretation = LoadInterpretation.PerHand,
        )
        val exerciseId = repository.createExercise(dumbbellDraft)
        val sessionId = repository.startWorkout("Meaning snapshots")
        val placementId = repository.addExerciseToWorkout(sessionId, exerciseId)

        repository.addSet(placementId, WorkoutSetDraft(weight = 45.0, weightUnitId = "pound", reps = 8))
        repository.updateExercise(exerciseId, dumbbellDraft.copy(loadInterpretation = LoadInterpretation.Total))
        repository.addSet(placementId, WorkoutSetDraft(weight = 50.0, weightUnitId = "pound", reps = 8))

        val freeWeightSets = repository.sets.first().filter { it.workoutExerciseId == placementId }
        assertEquals(45.0, freeWeightSets[0].enteredWeight!!, 0.0)
        assertEquals(90.0 * 0.45359237, freeWeightSets[0].canonicalWeightKg!!, 0.0000001)
        assertEquals(100.0 * 0.45359237, freeWeightSets[1].canonicalWeightKg!!, 0.0000001)
        assertEquals(LoadInterpretation.PerHand, repository.workoutExercises.first().single().loadInterpretationSnapshot)

        val machineId = repository.createMachine(
            GymMachineDraft(
                exerciseId = exerciseId,
                name = "Plate-loaded press",
                loadType = MachineLoadType.Mass,
                unitId = "pound",
                availableLoads = listOf(25.0, 35.0, 45.0),
                loadInterpretation = LoadInterpretation.PerSide,
                baseLoadKg = 10.0 * 0.45359237,
            ),
        )
        repository.finishWorkout(sessionId)
        val machineSessionId = repository.startWorkout("Machine meaning")
        val machinePlacementId = repository.addExerciseToWorkout(machineSessionId, exerciseId, machineId)
        repository.addSet(machinePlacementId, WorkoutSetDraft(weight = 25.0, weightUnitId = "pound", reps = 8))
        val machineSet = repository.sets.first().first { it.workoutExerciseId == machinePlacementId }
        assertEquals(60.0 * 0.45359237, machineSet.canonicalWeightKg!!, 0.0000001)
        assertEquals(LoadInterpretation.PerSide, repository.workoutExercises.first().first { it.id == machinePlacementId }.loadInterpretationSnapshot)
    }

    @Test
    fun historicalSetEditingUsesPlacementTrackingAndEnteredUnitSnapshots() = runBlocking {
        val originalExercise = ExerciseDraft(
            name = "Snapshot press",
            trackingType = ExerciseTrackingType.WeightReps,
            weightUnitId = "kilogram",
        )
        val exerciseId = repository.createExercise(originalExercise)
        val sessionId = repository.startWorkout("Snapshot workout")
        val placementId = repository.addExerciseToWorkout(sessionId, exerciseId)
        val setId = repository.addSet(
            placementId,
            WorkoutSetDraft(
                weight = 100.0,
                weightUnitId = "kilogram",
                reps = 5,
                completed = true,
            ),
        )
        repository.finishWorkout(sessionId)

        repository.updateExercise(
            exerciseId,
            originalExercise.copy(
                trackingType = ExerciseTrackingType.DurationOnly,
                weightUnitId = "pound",
            ),
        )
        repository.updateSet(
            setId,
            WorkoutSetDraft(
                weight = 100.0,
                weightUnitId = "kilogram",
                reps = 5,
                completed = true,
                note = "unchanged",
            ),
        )

        val saved = repository.sets.first().single { it.id == setId }
        assertEquals("kilogram", saved.enteredWeightUnitId)
        assertEquals(100.0, saved.enteredWeight ?: -1.0, 0.0)
        assertEquals(100.0, saved.canonicalWeightKg ?: -1.0, 0.0)
        assertEquals(ExerciseTrackingType.WeightReps, repository.workoutExercises.first().single().trackingTypeSnapshot)
    }

    @Test
    fun resumedWorkoutClearsOldEndAndCanFinishAgain() = runBlocking {
        val sessionId = repository.startWorkout("Resume lifecycle")
        repository.finishWorkout(sessionId)
        assertNotNull(repository.sessions.first().single().endedAt)

        repository.resumeWorkout(sessionId)
        val resumed = repository.sessions.first().single()
        assertEquals(WorkoutSessionState.Active, resumed.state)
        assertEquals(null, resumed.endedAt)

        repository.finishWorkout(sessionId)
        val finishedAgain = repository.sessions.first().single()
        assertEquals(WorkoutSessionState.Finished, finishedAgain.state)
        assertNotNull(finishedAgain.endedAt)
    }

    @Test
    fun databaseRejectsASecondActiveWorkoutEvenWhenRepositoryChecksAreBypassed() = runBlocking {
        val finishedId = repository.startWorkout("Finished")
        repository.finishWorkout(finishedId)
        val activeId = repository.startWorkout("Active")

        assertThrows(android.database.sqlite.SQLiteException::class.java) {
            database.openHelper.writableDatabase.execSQL(
                "UPDATE workout_sessions SET state = 'Active' WHERE id = ?",
                arrayOf(finishedId),
            )
        }
        assertEquals(activeId, repository.sessions.first().single { it.state == WorkoutSessionState.Active }.id)
    }

    @Test
    fun addedLoadCanRepresentNegativeAssistanceWithoutLosingRawEntry() = runBlocking {
        val exerciseId = repository.createExercise(
            ExerciseDraft(
                name = "Band-assisted pull-up",
                trackingType = com.whip.app.domain.ExerciseTrackingType.BodyweightReps,
                loadInterpretation = LoadInterpretation.AddedLoad,
            ),
        )
        val sessionId = repository.startWorkout()
        val placementId = repository.addExerciseToWorkout(sessionId, exerciseId)
        repository.addSet(
            placementId,
            WorkoutSetDraft(weight = -20.0, weightUnitId = "kilogram", bodyweightKg = 80.0, reps = 5, completed = true),
        )

        val set = repository.sets.first().single()
        assertEquals(-20.0, set.enteredWeight!!, 0.0)
        assertEquals(-20.0, set.canonicalWeightKg!!, 0.0)
    }

    @Test
    fun defaultAndDuplicatedWorkoutsSnapshotTheClockDateAndZoneWithoutRewritingHistory() = runBlocking {
        val defaultId = repository.startWorkout(name = "Default")
        val defaultSession = repository.sessions.first().single { it.id == defaultId }
        assertEquals(FixedClock.today(), defaultSession.localDate)
        assertEquals(FixedClock.zoneId().id, defaultSession.zoneId)
        repository.discardWorkout(defaultId)

        val historicalDate = LocalDate.of(2025, 1, 2)
        val historicalZone = ZoneId.of("UTC")
        val sourceId = repository.startWorkout(
            name = "Historical",
            localDate = historicalDate,
            zoneId = historicalZone,
        )
        repository.finishWorkout(sourceId)

        val duplicateId = repository.duplicateWorkout(sourceId)
        val sessions = repository.sessions.first()
        val source = sessions.single { it.id == sourceId }
        val duplicate = sessions.single { it.id == duplicateId }

        assertEquals(historicalDate, source.localDate)
        assertEquals("UTC", source.zoneId)
        assertEquals(FixedClock.today(), duplicate.localDate)
        assertEquals(FixedClock.zoneId().id, duplicate.zoneId)
    }

    @Test
    fun duplicateWorkoutExcludesEmptyRetiredPlacementsAndReusesRetainedPerformedWork() = runBlocking {
        val pressId = repository.createExercise(ExerciseDraft(name = "Press"))
        val inclineId = repository.createExercise(ExerciseDraft(name = "Incline press"))
        val rowId = repository.createExercise(ExerciseDraft(name = "Row"))
        val sourceSessionId = repository.startWorkout(name = "Substitutions")

        val unperformedPress = repository.addExerciseToWorkout(sourceSessionId, pressId)
        repository.addSet(unperformedPress, WorkoutSetDraft(weight = 50.0, reps = 5, planned = true))
        val activeIncline = repository.substituteWorkoutExercise(unperformedPress, inclineId)
        val inclineSet = repository.sets.first().single { it.workoutExerciseId == activeIncline }
        repository.updateSet(inclineSet.id, WorkoutSetDraft(weight = 45.0, reps = 8, completed = true))

        val performedRow = repository.addExerciseToWorkout(sourceSessionId, rowId)
        repository.addSet(
            performedRow,
            WorkoutSetDraft(
                weight = 70.0,
                reps = 10,
                completed = true,
                classification = WorkoutSetClassification.Failure,
                workSection = RoutineWorkSection.Main,
                mainWorkScheme = RoutineMainWorkScheme.ClassicPrSet,
            ),
        )
        repository.removeWorkoutExercise(performedRow)
        repository.finishWorkout(sourceSessionId)

        val duplicateSessionId = repository.duplicateWorkout(sourceSessionId)
        val duplicatePlacements = repository.workoutExercises.first()
            .filter { it.sessionId == duplicateSessionId }
            .sortedBy { it.position }

        assertEquals(listOf(inclineId, rowId), duplicatePlacements.map { it.exerciseId })
        assertTrue(duplicatePlacements.all { it.outcome == WorkoutExerciseOutcome.Active })
        assertEquals(listOf(0, 1), duplicatePlacements.map { it.position })
        assertFalse(duplicatePlacements.any { it.exerciseId == pressId })

        val duplicatedSets = repository.sets.first()
            .filter { set -> duplicatePlacements.any { it.id == set.workoutExerciseId } }
        assertEquals(2, duplicatedSets.size)
        assertTrue(duplicatedSets.none { it.completed })
        assertTrue(duplicatedSets.all { it.deletedAtMillis == null })
        assertTrue(duplicatedSets.all { !it.requiredForProgressionSnapshot })
        val duplicatedRowSet = duplicatedSets.single { set ->
            duplicatePlacements.single { it.id == set.workoutExerciseId }.exerciseId == rowId
        }
        assertEquals(10, duplicatedRowSet.repetitions)
        assertEquals(WorkoutSetClassification.Working, duplicatedRowSet.classification)
        assertEquals(RoutineWorkSection.Unspecified, duplicatedRowSet.workSectionSnapshot)
        assertEquals(RoutineOptionalWorkKind.None, duplicatedRowSet.optionalWorkKindSnapshot)
    }

    @Test
    fun explicitHistoricalStartDerivesItsPhysicalDateInTheChosenZone() = runBlocking {
        val startedAt = Instant.parse("2020-01-02T02:30:00Z")

        val sessionId = repository.startWorkout(
            name = "Backdated",
            startedAt = startedAt,
            zoneId = ZoneId.of("America/Toronto"),
        )
        val session = repository.sessions.first().single { it.id == sessionId }

        assertEquals(startedAt, session.startedAt)
        assertEquals(LocalDate.of(2020, 1, 1), session.localDate)
        assertEquals("America/Toronto", session.zoneId)
    }

    @Test
    fun copyingAnExerciseWithoutAnActiveWorkoutUsesTheClockDateAndZone() = runBlocking {
        val exerciseId = repository.createExercise(ExerciseDraft(name = "Zercher squat"))
        val sourceSessionId = repository.startWorkout(
            name = "Old session",
            localDate = LocalDate.of(2025, 4, 3),
            zoneId = ZoneId.of("UTC"),
        )
        val sourcePlacementId = repository.addExerciseToWorkout(sourceSessionId, exerciseId)
        repository.addSet(sourcePlacementId, WorkoutSetDraft(weight = 100.0, reps = 5, completed = true))
        repository.finishWorkout(sourceSessionId)

        repository.copyWorkoutExerciseToActive(sourcePlacementId)
        val copiedSession = repository.sessions.first().single { it.state == WorkoutSessionState.Active }

        assertEquals(FixedClock.today(), copiedSession.localDate)
        assertEquals(FixedClock.zoneId().id, copiedSession.zoneId)
        val preservedSource = repository.sessions.first().single { it.id == sourceSessionId }
        assertEquals(WorkoutSessionState.Finished, preservedSource.state)
        assertEquals(LocalDate.of(2025, 4, 3), preservedSource.localDate)
        assertEquals("UTC", preservedSource.zoneId)
    }

    private object FixedClock : WhipClock {
        override fun now(): Instant = Instant.parse("2026-08-17T16:00:00Z")
        override fun zoneId(): ZoneId = ZoneId.of("America/Toronto")
        override fun today(zoneId: ZoneId): LocalDate = LocalDate.of(2026, 8, 17)
    }

    private class SequentialIds : WhipIdGenerator {
        private val count = AtomicInteger()
        override fun nextId(): String = "test-${count.incrementAndGet()}"
    }
}
