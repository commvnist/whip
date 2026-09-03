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
import com.whip.app.domain.GymGraphMetric
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
import com.whip.app.domain.WorkoutArrangementDraft
import com.whip.app.domain.WorkoutSetOrderDraft
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
        assertEquals(1L, snapshot.sessions.single { it.id == sessionId }.workoutRevision)
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
    fun repositoryCanonicalizesFieldsHiddenByExerciseAndMachineTypes() = runBlocking {
        val exerciseId = repository.createExercise(
            ExerciseDraft(
                name = "Plank",
                trackingType = ExerciseTrackingType.DurationOnly,
                weightIncrement = Double.NaN,
                repetitionIncrement = -1,
                defaultGraphMetric = GymGraphMetric.EstimatedOneRepMax.name,
                barWeightKg = -1.0,
                availablePlatesKg = listOf(-1.0),
                includeInVolume = true,
            ),
        )
        val exercise = requireNotNull(repository.exercises.first().single { it.id == exerciseId })
        assertEquals(GymGraphMetric.Duration.name, exercise.defaultGraphMetric)
        assertEquals(null, exercise.barWeightKg)
        assertTrue(exercise.availablePlatesKg.isEmpty())
        assertFalse(exercise.includeInVolume)

        val machineId = repository.createMachine(
            GymMachineDraft(
                name = "Mass stack",
                loadType = MachineLoadType.Mass,
                loadInterpretation = LoadInterpretation.OrdinalSetting,
                massMappingKg = mapOf(1.0 to 10.0),
            ),
        )
        val machine = requireNotNull(repository.machines.first().single { it.id == machineId })
        assertEquals(LoadInterpretation.MachineDisplayedMass, machine.loadInterpretation)
        assertTrue(machine.massMappingKg.isEmpty())
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
    fun noOpStructureRepairDoesNotRewriteRowsOrAdvanceWorkoutRevision() = runBlocking {
        val exerciseId = repository.createExercise(ExerciseDraft(name = "No-op press"))
        val sessionId = repository.startWorkout("No-op repair")
        val placementId = repository.addExerciseToWorkout(sessionId, exerciseId)
        repository.addSet(placementId)
        val beforeSession = repository.sessions.first().single { it.id == sessionId }
        val beforePlacement = repository.workoutExercises.first().single { it.id == placementId }
        val beforeSet = repository.sets.first().single { it.workoutExerciseId == placementId }

        val receipt = repository.normalizeActiveWorkoutStructure(sessionId)

        val afterSession = repository.sessions.first().single { it.id == sessionId }
        val afterPlacement = repository.workoutExercises.first().single { it.id == placementId }
        val afterSet = repository.sets.first().single { it.workoutExerciseId == placementId }
        assertFalse(receipt.changed)
        assertEquals(beforeSession.workoutRevision, afterSession.workoutRevision)
        assertEquals(beforePlacement.updatedAtMillis, afterPlacement.updatedAtMillis)
        assertEquals(beforeSet.updatedAtMillis, afterSet.updatedAtMillis)
    }

    @Test
    fun arrangementRetainsTombstoneSlotAndUndoRestoresWithoutDuplicatePositions() = runBlocking {
        val exerciseId = repository.createExercise(ExerciseDraft(name = "Tombstone row"))
        val sessionId = repository.startWorkout("Tombstone order")
        val placementId = repository.addExerciseToWorkout(sessionId, exerciseId)
        val first = repository.addSet(placementId)
        val removed = repository.addSet(placementId)
        val third = repository.addSet(placementId)
        repository.deleteSet(removed)
        val placement = repository.workoutExercises.first().single { it.id == placementId }
        val setById = repository.sets.first().associateBy { it.id }
        val before = repository.testStructureBoundary(sessionId)

        repository.applyWorkoutArrangement(
            before,
            WorkoutArrangementDraft(
                activeWorkoutExerciseUuidsInOrder = listOf(placement.uuid),
                setOrders = listOf(
                    WorkoutSetOrderDraft(
                        placement.uuid,
                        listOf(
                            requireNotNull(setById[third]).uuid,
                            requireNotNull(setById[removed]).uuid,
                            requireNotNull(setById[first]).uuid,
                        ),
                    ),
                ),
            ),
        )
        repository.undoDeleteSet(removed)

        val stored = repository.sets.first().filter { it.workoutExerciseId == placementId }.sortedBy { it.position }
        assertEquals(listOf(third, removed, first), stored.map { it.id })
        assertEquals(listOf(0, 1, 2), stored.map { it.position })
        assertEquals(stored.size, stored.map { it.position }.distinct().size)
        assertTrue(stored.all { it.deletedAtMillis == null })
    }

    @Test
    fun staleArrangementCannotOverwriteAGroupCreatedAfterReview() = runBlocking {
        val exerciseIds = listOf("A", "B", "C").map { repository.createExercise(ExerciseDraft(name = it)) }
        val sessionId = repository.startWorkout("Stale arrangement")
        val placements = exerciseIds.map { repository.addExerciseToWorkout(sessionId, it) }
        placements.forEach { repository.addSet(it) }
        val staleBoundary = repository.testStructureBoundary(sessionId)
        val placementRows = repository.workoutExercises.first().associateBy { it.id }
        val allSets = repository.sets.first()
        val staleDraft = WorkoutArrangementDraft(
            activeWorkoutExerciseUuidsInOrder = placements.reversed().map { requireNotNull(placementRows[it]).uuid },
            setOrders = placements.map { id ->
                val placement = requireNotNull(placementRows[id])
                WorkoutSetOrderDraft(
                    placement.uuid,
                    allSets.filter { it.workoutExerciseId == id }.map { it.uuid },
                )
            },
        )
        repository.createGroup(sessionId, "Pair", WorkoutGroupType.Superset, placements.take(2))

        val failure = runCatching { repository.applyWorkoutArrangement(staleBoundary, staleDraft) }

        assertTrue(failure.isFailure)
        assertEquals(
            placements.take(2).toSet(),
            repository.workoutExercises.first().filter { it.groupId != null }.map { it.id }.toSet(),
        )
    }

    @Test
    fun staleUngroupCannotDetachExerciseFromAReplacementGroup() = runBlocking {
        val exerciseIds = listOf("A", "B", "C").map { repository.createExercise(ExerciseDraft(name = it)) }
        val sessionId = repository.startWorkout("Exact ungroup")
        val placements = exerciseIds.map { repository.addExerciseToWorkout(sessionId, it) }
        repository.createGroup(sessionId, "First", WorkoutGroupType.Superset, placements.take(2))
        val staleRemoval = repository.testPlacementBoundary(placements[0])
        val replacementGroupId = repository.createGroup(
            sessionId,
            "Second",
            WorkoutGroupType.Circuit,
            listOf(placements[0], placements[2]),
        )

        val failure = runCatching { repository.removeWorkoutExerciseFromGroup(staleRemoval) }

        assertTrue(failure.isFailure)
        assertEquals(
            replacementGroupId,
            repository.workoutExercises.first().single { it.id == placements[0] }.groupId,
        )
    }

    @Test
    fun optionalSkipAndUndoRequireTheExactReviewedTombstone() = runBlocking {
        val exerciseId = repository.createExercise(ExerciseDraft(name = "Joker row"))
        val sessionId = repository.startWorkout("Exact optional undo")
        val placementId = repository.addExerciseToWorkout(sessionId, exerciseId)
        val setId = repository.addSet(
            placementId,
            WorkoutSetDraft(
                workSection = RoutineWorkSection.Optional,
                optionalWorkKind = RoutineOptionalWorkKind.Joker,
            ),
        )
        repository.deleteSet(setId, WorkoutSetRemovalReason.Skipped)
        val staleUndo = repository.testSetBoundary(setId)
        repository.undoDeleteSet(setId)
        repository.deleteSet(setId, WorkoutSetRemovalReason.Removed)

        val failure = runCatching { repository.undoDeleteSet(staleUndo) }

        assertTrue(failure.isFailure)
        assertEquals(WorkoutSetRemovalReason.Removed, repository.sets.first().single { it.id == setId }.removalReason)
    }

    @Test
    fun exactAddRequestIsIdempotentAndAStaleDifferentRequestWritesNothing() = runBlocking {
        val pressId = repository.createExercise(ExerciseDraft(name = "Exact press"))
        val rowId = repository.createExercise(ExerciseDraft(name = "Exact row"))
        val sessionId = repository.startWorkout("Exact add")
        val reviewed = repository.testStructureBoundary(sessionId)

        val first = repository.addExerciseWithInitialSetToWorkout(
            boundary = reviewed,
            exerciseId = pressId,
            requestedWorkoutExerciseUuid = "requested-placement",
            requestedInitialSetUuid = "requested-set",
        )
        val replay = repository.addExerciseWithInitialSetToWorkout(
            boundary = reviewed,
            exerciseId = pressId,
            requestedWorkoutExerciseUuid = "requested-placement",
            requestedInitialSetUuid = "requested-set",
        )
        val revisionAfterCommit = repository.sessions.first().single { it.id == sessionId }.workoutRevision

        val staleFailure = runCatching {
            repository.addExerciseWithInitialSetToWorkout(
                boundary = reviewed,
                exerciseId = rowId,
                requestedWorkoutExerciseUuid = "different-placement",
                requestedInitialSetUuid = "different-set",
            )
        }

        assertEquals(first, replay)
        assertEquals(1, repository.workoutExercises.first().count { it.sessionId == sessionId })
        assertEquals(1, repository.sets.first().count { it.workoutExerciseId == first.workoutExerciseId })
        assertEquals(1L, revisionAfterCommit)
        assertTrue(staleFailure.isFailure)
        assertFalse(repository.workoutExercises.first().any { it.uuid == "different-placement" })
        assertEquals(revisionAfterCommit, repository.sessions.first().single { it.id == sessionId }.workoutRevision)
    }

    @Test
    fun exactSubstitutionRequestIsIdempotentWithoutRewritingRetainedSourceHistory() = runBlocking {
        val pressId = repository.createExercise(ExerciseDraft(name = "Original press"))
        val inclineId = repository.createExercise(ExerciseDraft(name = "Replacement press"))
        val sessionId = repository.startWorkout("Exact substitution")
        val originalId = repository.addExerciseToWorkout(sessionId, pressId)
        val performedSetId = repository.addSet(
            originalId,
            WorkoutSetDraft(weight = 70.0, reps = 5, completed = true),
        )
        val reviewed = repository.testPlacementBoundary(originalId)

        val replacementId = repository.substituteWorkoutExercise(
            boundary = reviewed,
            exerciseId = inclineId,
            requestedWorkoutExerciseUuid = "replacement-placement",
            requestedInitialSetUuid = "replacement-set",
        )
        val replayId = repository.substituteWorkoutExercise(
            boundary = reviewed,
            exerciseId = inclineId,
            requestedWorkoutExerciseUuid = "replacement-placement",
            requestedInitialSetUuid = "replacement-set",
        )

        assertEquals(replacementId, replayId)
        assertEquals(WorkoutExerciseOutcome.Substituted, repository.workoutExercises.first().single { it.id == originalId }.outcome)
        assertTrue(repository.sets.first().single { it.id == performedSetId }.completed)
        assertEquals(70.0, repository.sets.first().single { it.id == performedSetId }.enteredWeight ?: -1.0, 0.0)
        assertEquals(1, repository.sets.first().count { it.uuid == "replacement-set" })
    }

    @Test
    fun createAndAuthorRequestsReplayWithoutDuplicatingCatalogExercises() = runBlocking {
        val sessionId = repository.startWorkout("Create and author")
        val addBoundary = repository.testStructureBoundary(sessionId)
        val zercherDraft = ExerciseDraft(name = "Zercher squat", notes = "Elbows high")

        val addition = repository.createExerciseAndAddToWorkout(
            addBoundary,
            zercherDraft,
            requestedWorkoutExerciseUuid = "created-add-placement",
            requestedInitialSetUuid = "created-add-set",
        )
        val additionReplay = repository.createExerciseAndAddToWorkout(
            addBoundary,
            zercherDraft,
            requestedWorkoutExerciseUuid = "created-add-placement",
            requestedInitialSetUuid = "created-add-set",
        )
        val changedDraftFailure = runCatching {
            repository.createExerciseAndAddToWorkout(
                addBoundary,
                zercherDraft.copy(notes = "Changed after review"),
                requestedWorkoutExerciseUuid = "created-add-placement",
                requestedInitialSetUuid = "created-add-set",
            )
        }

        val pressId = repository.createExercise(ExerciseDraft(name = "Temporary press"))
        val originalPlacementId = repository.addExerciseToWorkout(sessionId, pressId)
        val substitutionBoundary = repository.testPlacementBoundary(originalPlacementId)
        val customDraft = ExerciseDraft(name = "Custom incline press")
        val replacementId = repository.createExerciseAndSubstitute(
            substitutionBoundary,
            customDraft,
            requestedWorkoutExerciseUuid = "created-sub-placement",
            requestedInitialSetUuid = "created-sub-set",
        )
        val replacementReplay = repository.createExerciseAndSubstitute(
            substitutionBoundary,
            customDraft,
            requestedWorkoutExerciseUuid = "created-sub-placement",
            requestedInitialSetUuid = "created-sub-set",
        )

        assertEquals(addition, additionReplay)
        assertEquals(replacementId, replacementReplay)
        assertTrue(changedDraftFailure.isFailure)
        assertEquals(1, repository.exercises.first().count { it.name == "Zercher squat" })
        assertEquals(1, repository.exercises.first().count { it.name == "Custom incline press" })
        assertEquals(addition.workoutExerciseId, repository.workoutExercises.first().single { it.uuid == "created-add-placement" }.id)
    }

    @Test
    fun machineAssignmentRollsBackCreationAndCannotRetargetRemovedSetHistory() = runBlocking {
        val pressId = repository.createExercise(ExerciseDraft(name = "Machine press"))
        val rowId = repository.createExercise(ExerciseDraft(name = "Machine row"))
        val originalMachineId = repository.createMachine(
            GymMachineDraft(exerciseId = pressId, name = "Original stack", loadType = MachineLoadType.Mass),
        )
        val sessionId = repository.startWorkout("Atomic machine")
        val placementId = repository.addExerciseToWorkout(sessionId, pressId, originalMachineId)
        val removedSetId = repository.addSet(placementId, WorkoutSetDraft(weight = 40.0, reps = 8))
        repository.deleteSet(removedSetId)
        val reviewed = repository.testPlacementBoundary(placementId)
        val machineCountBefore = repository.machines.first().size
        val setBefore = repository.sets.first().single { it.id == removedSetId }
        val placementBefore = repository.workoutExercises.first().single { it.id == placementId }

        val failure = runCatching {
            repository.createMachineAndAssign(
                reviewed,
                GymMachineDraft(exerciseId = rowId, name = "Wrong stack", loadType = MachineLoadType.Mass),
            )
        }

        val setAfter = repository.sets.first().single { it.id == removedSetId }
        val placementAfter = repository.workoutExercises.first().single { it.id == placementId }
        assertTrue(failure.isFailure)
        assertEquals(machineCountBefore, repository.machines.first().size)
        assertEquals(setBefore, setAfter)
        assertEquals(placementBefore, placementAfter)
    }

    @Test
    fun groupRequestReplayIsIdempotentButChangedPayloadWithSameIdentityFailsClosed() = runBlocking {
        val exerciseIds = listOf("Replay A", "Replay B").map { repository.createExercise(ExerciseDraft(name = it)) }
        val sessionId = repository.startWorkout("Group replay")
        val placementIds = exerciseIds.map { repository.addExerciseToWorkout(sessionId, it) }
        val placements = repository.workoutExercises.first().filter { it.id in placementIds }.sortedBy { it.position }
        val reviewed = repository.testStructureBoundary(sessionId)

        val committed = repository.createGroup(
            reviewed,
            requestedGroupUuid = "group-request",
            name = "Pair",
            type = WorkoutGroupType.Superset,
            workoutExerciseUuids = placements.map { it.uuid },
        )
        val replay = repository.createGroup(
            reviewed,
            requestedGroupUuid = "group-request",
            name = "Pair",
            type = WorkoutGroupType.Superset,
            workoutExerciseUuids = placements.map { it.uuid },
        )
        val conflict = runCatching {
            repository.createGroup(
                reviewed,
                requestedGroupUuid = "group-request",
                name = "Changed name",
                type = WorkoutGroupType.Circuit,
                workoutExerciseUuids = placements.map { it.uuid },
            )
        }

        assertTrue(committed.changed)
        assertFalse(replay.changed)
        assertTrue(conflict.isFailure)
        assertEquals(1, repository.groups.first().count { it.uuid == "group-request" })
    }

    @Test
    fun layoutUndoPreservesNewSetValuesButRejectsLaterStructuralAuthorshipAtomically() = runBlocking {
        val exerciseIds = listOf("Undo A", "Undo B").map { repository.createExercise(ExerciseDraft(name = it)) }
        val sessionId = repository.startWorkout("Exact layout undo")
        val placementIds = exerciseIds.map { repository.addExerciseToWorkout(sessionId, it) }
        val setIds = placementIds.map { repository.addSet(it, WorkoutSetDraft(weight = 50.0, reps = 5)) }
        val placements = repository.workoutExercises.first().filter { it.id in placementIds }.associateBy { it.id }
        val arrangement = repository.applyWorkoutArrangement(
            repository.testStructureBoundary(sessionId),
            WorkoutArrangementDraft(
                activeWorkoutExerciseUuidsInOrder = placementIds.reversed().map { requireNotNull(placements[it]).uuid },
                setOrders = placementIds.map { id ->
                    WorkoutSetOrderDraft(
                        requireNotNull(placements[id]).uuid,
                        repository.sets.first().filter { it.workoutExerciseId == id }.map { it.uuid },
                    )
                },
            ),
        )
        val snapshot = requireNotNull(arrangement.previousLayout)
        repository.updateSet(
            repository.testSetBoundary(setIds.first()),
            WorkoutSetDraft(weight = 62.5, reps = 7),
        )
        repository.restoreWorkoutLayout(arrangement.afterBoundary, snapshot)

        assertEquals(62.5, repository.sets.first().single { it.id == setIds.first() }.enteredWeight ?: -1.0, 0.0)
        assertEquals(placementIds, repository.workoutExercises.first().filter { it.id in placementIds }.sortedBy { it.position }.map { it.id })

        val secondArrangement = repository.applyWorkoutArrangement(
            repository.testStructureBoundary(sessionId),
            WorkoutArrangementDraft(
                activeWorkoutExerciseUuidsInOrder = placementIds.reversed().map { requireNotNull(placements[it]).uuid },
                setOrders = placementIds.map { id ->
                    WorkoutSetOrderDraft(
                        requireNotNull(placements[id]).uuid,
                        repository.sets.first().filter { it.workoutExerciseId == id }.map { it.uuid },
                    )
                },
            ),
        )
        val staleSnapshot = requireNotNull(secondArrangement.previousLayout)
        repository.addSet(repository.testPlacementBoundary(placementIds.first()))
        val beforeFailure = repository.workoutSnapshot.first()
        val failure = runCatching { repository.restoreWorkoutLayout(secondArrangement.afterBoundary, staleSnapshot) }
        val afterFailure = repository.workoutSnapshot.first()

        assertTrue(failure.isFailure)
        assertEquals(beforeFailure, afterFailure)
    }

    @Test
    fun historyCopyUsesExactSourceAndTargetAuthorshipAndReplaysOnce() = runBlocking {
        val exerciseId = repository.createExercise(ExerciseDraft(name = "History copy press"))
        val sourceSessionId = repository.startWorkout("Source history")
        val sourcePlacementId = repository.addExerciseToWorkout(sourceSessionId, exerciseId)
        repository.addSet(sourcePlacementId, WorkoutSetDraft(weight = 80.0, reps = 5, completed = true))
        repository.finishWorkout(sourceSessionId)
        val sourceSession = repository.sessions.first().single { it.id == sourceSessionId }
        val sourcePlacement = repository.workoutExercises.first().single { it.id == sourcePlacementId }
        val sourceSets = repository.sets.first().filter { it.workoutExerciseId == sourcePlacementId }
        val boundary = com.whip.app.domain.WorkoutExerciseCopyBoundary(
            sourceSessionId = sourceSession.id,
            sourceSessionUuid = sourceSession.uuid,
            sourceWorkoutExerciseId = sourcePlacement.id,
            sourceWorkoutExerciseUuid = sourcePlacement.uuid,
            sourceWorkoutExerciseUpdatedAtMillis = sourcePlacement.updatedAtMillis,
            sourceSets = sourceSets.map {
                com.whip.app.domain.WorkoutSetCopyBoundary(it.id, it.uuid, it.updatedAtMillis)
            },
            target = null,
        )

        val copiedId = repository.copyWorkoutExerciseToActive(
            boundary,
            requestedWorkoutExerciseUuid = "history-copy-placement",
            requestedSetUuids = listOf("history-copy-set"),
        )
        val replayId = repository.copyWorkoutExerciseToActive(
            boundary,
            requestedWorkoutExerciseUuid = "history-copy-placement",
            requestedSetUuids = listOf("history-copy-set"),
        )
        val conflictingReplay = runCatching {
            repository.copyWorkoutExerciseToActive(
                boundary,
                requestedWorkoutExerciseUuid = "history-copy-placement",
                requestedSetUuids = listOf("different-copy-set"),
            )
        }

        val activeSession = repository.sessions.first().single { it.state == WorkoutSessionState.Active }
        assertEquals(copiedId, replayId)
        assertTrue(conflictingReplay.isFailure)
        assertEquals(1, repository.workoutExercises.first().count { it.uuid == "history-copy-placement" })
        assertEquals(1, repository.sets.first().count { it.uuid == "history-copy-set" })
        assertEquals(1L, activeSession.workoutRevision)
    }

    @Test
    fun historyCopyRejectsAnActiveTargetChangedAfterReviewWithoutWrites() = runBlocking {
        val sourceExerciseId = repository.createExercise(ExerciseDraft(name = "Reviewed history row"))
        val sourceSessionId = repository.startWorkout("Reviewed source")
        val sourcePlacementId = repository.addExerciseToWorkout(sourceSessionId, sourceExerciseId)
        repository.addSet(sourcePlacementId, WorkoutSetDraft(weight = 90.0, reps = 3, completed = true))
        repository.finishWorkout(sourceSessionId)
        val sourceSession = repository.sessions.first().single { it.id == sourceSessionId }
        val sourcePlacement = repository.workoutExercises.first().single { it.id == sourcePlacementId }
        val sourceSets = repository.sets.first().filter { it.workoutExerciseId == sourcePlacementId }

        val activeSessionId = repository.startWorkout("Changed target")
        val reviewedTarget = repository.testStructureBoundary(activeSessionId)
        val otherExerciseId = repository.createExercise(ExerciseDraft(name = "Concurrent target row"))
        repository.addExerciseToWorkout(activeSessionId, otherExerciseId)
        val beforeFailure = repository.workoutSnapshot.first()
        val failure = runCatching {
            repository.copyWorkoutExerciseToActive(
                com.whip.app.domain.WorkoutExerciseCopyBoundary(
                    sourceSessionId = sourceSession.id,
                    sourceSessionUuid = sourceSession.uuid,
                    sourceWorkoutExerciseId = sourcePlacement.id,
                    sourceWorkoutExerciseUuid = sourcePlacement.uuid,
                    sourceWorkoutExerciseUpdatedAtMillis = sourcePlacement.updatedAtMillis,
                    sourceSets = sourceSets.map {
                        com.whip.app.domain.WorkoutSetCopyBoundary(it.id, it.uuid, it.updatedAtMillis)
                    },
                    target = reviewedTarget,
                ),
                requestedWorkoutExerciseUuid = "stale-target-copy-placement",
                requestedSetUuids = sourceSets.mapIndexed { index, _ -> "stale-target-copy-set-$index" },
            )
        }

        assertTrue(failure.isFailure)
        assertEquals(beforeFailure, repository.workoutSnapshot.first())
        assertFalse(
            repository.workoutExercises.first().any { it.uuid == "stale-target-copy-placement" },
        )
        assertFalse(repository.sets.first().any { it.uuid.startsWith("stale-target-copy-set-") })
    }

    @Test
    fun discardRejectsAWorkoutChangedAfterConfirmationReview() = runBlocking {
        val exerciseId = repository.createExercise(ExerciseDraft(name = "Discard review press"))
        val sessionId = repository.startWorkout("Discard review")
        val session = repository.sessions.first().single { it.id == sessionId }
        val reviewed = com.whip.app.domain.WorkoutFinishBoundary(session.id, session.uuid, session.workoutRevision)
        val placementId = repository.addExerciseToWorkout(sessionId, exerciseId)
        repository.addSet(placementId)
        val revisionBeforeFailure = repository.sessions.first().single { it.id == sessionId }.workoutRevision

        val failure = runCatching { repository.discardWorkout(reviewed) }

        assertTrue(failure.isFailure)
        assertEquals(WorkoutSessionState.Active, repository.sessions.first().single { it.id == sessionId }.state)
        assertEquals(revisionBeforeFailure, repository.sessions.first().single { it.id == sessionId }.workoutRevision)
        repository.discardWorkout(sessionId)
        assertEquals(WorkoutSessionState.Discarded, repository.sessions.first().single { it.id == sessionId }.state)
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
        listOf(placements[0], placements[1], placements[2]).forEachIndexed { index, id ->
            val row = requireNotNull(database.gymDao().getWorkoutExercise(id))
            database.gymDao().updateWorkoutExercise(row.copy(position = index))
        }

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
    fun duplicatedWorkoutStartsWithCleanTimerAndProgressionState() = runBlocking {
        val sourceId = repository.startWorkout(name = "Previously invalidated")
        val source = requireNotNull(database.gymDao().getSession(sourceId))
        database.gymDao().updateSession(
            source.copy(
                state = WorkoutSessionState.Finished.name,
                endedAtMillis = FixedClock.now().toEpochMilli(),
                restTimerDeadlineMillis = FixedClock.now().plusSeconds(90).toEpochMilli(),
                restTimerDurationSeconds = 90,
                restTimerRevision = 7,
                restTimerCleanupPending = true,
                requiredMainWorkInvalidated = true,
                invalidatedMainExerciseIdsCsv = "42,73",
                workoutRevision = 9,
            ),
        )

        val duplicateId = repository.duplicateWorkout(sourceId)
        val duplicate = repository.sessions.first().single { it.id == duplicateId }

        assertEquals(WorkoutSessionState.Active, duplicate.state)
        assertEquals(null, duplicate.restTimerDeadlineMillis)
        assertEquals(null, duplicate.restTimerDurationSeconds)
        assertEquals(0, duplicate.restTimerRevision)
        assertFalse(duplicate.restTimerCleanupPending)
        assertFalse(duplicate.requiredMainWorkInvalidated)
        assertTrue(duplicate.invalidatedMainExerciseIds.isEmpty())
        assertEquals(0, duplicate.workoutRevision)
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
