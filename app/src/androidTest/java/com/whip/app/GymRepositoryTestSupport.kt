package com.whip.app

import com.whip.app.data.RoomGymRepository
import com.whip.app.data.GymRepository
import com.whip.app.domain.WorkoutArrangementDraft
import com.whip.app.domain.WorkoutExercise
import com.whip.app.domain.WorkoutExerciseCopyBoundary
import com.whip.app.domain.WorkoutExerciseOutcome
import com.whip.app.domain.WorkoutFinishBoundary
import com.whip.app.domain.WorkoutGroupType
import com.whip.app.domain.WorkoutPlacementMutationBoundary
import com.whip.app.domain.WorkoutSet
import com.whip.app.domain.WorkoutSetCopyBoundary
import com.whip.app.domain.WorkoutSetDraft
import com.whip.app.domain.WorkoutSetMutationBoundary
import com.whip.app.domain.WorkoutSetOrderDraft
import com.whip.app.domain.WorkoutSetRemovalReason
import com.whip.app.domain.WorkoutStructureBoundary
import com.whip.app.domain.workoutStructureBoundary
import java.util.UUID
import kotlinx.coroutines.flow.first

/**
 * Concise sequential setup helpers for the pre-boundary instrumentation corpus. Production has
 * no ID-only overloads; concurrency and stale-review tests call the exact APIs directly.
 */
internal suspend fun RoomGymRepository.testStructureBoundary(sessionId: Long): WorkoutStructureBoundary {
    val session = requireNotNull(sessions.first().firstOrNull { it.id == sessionId })
    return workoutStructureBoundary(
        session,
        workoutExercises.first(),
        groups.first(),
        sets.first(),
    )
}

internal suspend fun RoomGymRepository.testPlacementBoundary(
    workoutExerciseId: Long,
): WorkoutPlacementMutationBoundary {
    val placements = workoutExercises.first()
    val placement = requireNotNull(placements.firstOrNull { it.id == workoutExerciseId })
    val sessionGroups = groups.first()
    return WorkoutPlacementMutationBoundary(
        structure = testStructureBoundary(placement.sessionId),
        workoutExerciseId = placement.id,
        workoutExerciseUuid = placement.uuid,
        workoutExerciseUpdatedAtMillis = placement.updatedAtMillis,
        expectedGroupUuid = placement.groupId?.let { id -> sessionGroups.firstOrNull { it.id == id }?.uuid },
    )
}

internal suspend fun RoomGymRepository.testSetBoundary(setId: Long): WorkoutSetMutationBoundary {
    val set = requireNotNull(sets.first().firstOrNull { it.id == setId })
    val placement = requireNotNull(workoutExercises.first().firstOrNull { it.id == set.workoutExerciseId })
    val session = requireNotNull(sessions.first().firstOrNull { it.id == placement.sessionId })
    return WorkoutSetMutationBoundary(
        sessionId = session.id,
        sessionUuid = session.uuid,
        workoutRevision = session.workoutRevision,
        workoutExerciseId = placement.id,
        workoutExerciseUuid = placement.uuid,
        setId = set.id,
        setUuid = set.uuid,
        setUpdatedAtMillis = set.updatedAtMillis,
        expectedDeletedAtMillis = set.deletedAtMillis,
        expectedRemovalReason = set.removalReason,
    )
}

internal suspend fun RoomGymRepository.updateSet(id: Long, draft: WorkoutSetDraft) =
    updateSet(testSetBoundary(id), draft)

internal suspend fun RoomGymRepository.addSet(
    workoutExerciseId: Long,
    draft: WorkoutSetDraft? = null,
) = addSet(testPlacementBoundary(workoutExerciseId), draft)

internal suspend fun RoomGymRepository.duplicateSet(id: Long) = duplicateSet(testSetBoundary(id))

internal suspend fun RoomGymRepository.deleteSet(
    id: Long,
    reason: WorkoutSetRemovalReason = WorkoutSetRemovalReason.Removed,
) = deleteSet(testSetBoundary(id), reason)

internal suspend fun RoomGymRepository.undoDeleteSet(id: Long) = undoDeleteSet(testSetBoundary(id))

internal suspend fun RoomGymRepository.setSetCompleted(
    id: Long,
    completed: Boolean,
    autoStartRest: Boolean = true,
    restOverrideSeconds: Int? = null,
) = setSetCompleted(testSetBoundary(id), completed, autoStartRest, restOverrideSeconds)

internal suspend fun RoomGymRepository.removeWorkoutExercise(id: Long) =
    removeWorkoutExercise(testPlacementBoundary(id))

internal suspend fun RoomGymRepository.removeWorkoutExerciseFromGroup(id: Long) =
    removeWorkoutExerciseFromGroup(testPlacementBoundary(id))

internal suspend fun RoomGymRepository.updateWorkoutExerciseDetails(
    id: Long,
    notes: String,
    groupId: Long?,
    machineId: Long?,
) = updateWorkoutExerciseDetails(testPlacementBoundary(id), notes, groupId, machineId)

internal suspend fun RoomGymRepository.setWorkoutExerciseMachine(id: Long, machineId: Long?) {
    val placement = requireNotNull(workoutExercises.first().firstOrNull { it.id == id })
    updateWorkoutExerciseDetails(testPlacementBoundary(id), placement.notes, placement.groupId, machineId)
}

internal suspend fun RoomGymRepository.addExerciseWithInitialSetToWorkout(
    sessionId: Long,
    exerciseId: Long,
    machineId: Long? = null,
) = addExerciseWithInitialSetToWorkout(
    boundary = testStructureBoundary(sessionId),
    exerciseId = exerciseId,
    machineId = machineId,
    requestedWorkoutExerciseUuid = UUID.randomUUID().toString(),
    requestedInitialSetUuid = UUID.randomUUID().toString(),
)

internal suspend fun RoomGymRepository.substituteWorkoutExercise(
    workoutExerciseId: Long,
    exerciseId: Long,
    machineId: Long? = null,
) = substituteWorkoutExercise(
    boundary = testPlacementBoundary(workoutExerciseId),
    exerciseId = exerciseId,
    machineId = machineId,
    requestedWorkoutExerciseUuid = UUID.randomUUID().toString(),
    requestedInitialSetUuid = UUID.randomUUID().toString(),
)

internal suspend fun RoomGymRepository.createGroup(
    sessionId: Long,
    name: String,
    type: WorkoutGroupType,
    workoutExerciseIds: List<Long>,
): Long {
    val placements = workoutExercises.first()
    val requestedUuid = UUID.randomUUID().toString()
    createGroup(
        boundary = testStructureBoundary(sessionId),
        requestedGroupUuid = requestedUuid,
        name = name,
        type = type,
        workoutExerciseUuids = workoutExerciseIds.map { id ->
            requireNotNull(placements.firstOrNull { it.id == id }).uuid
        },
    )
    return requireNotNull(groups.first().firstOrNull { it.uuid == requestedUuid }).id
}

internal suspend fun RoomGymRepository.reorderWorkoutExercises(sessionId: Long, idsInOrder: List<Long>) {
    val placements = workoutExercises.first().filter { it.sessionId == sessionId }
    val active = placements.filter { it.outcome == WorkoutExerciseOutcome.Active }
    val placementById = active.associateBy(WorkoutExercise::id)
    val allSets = sets.first()
    applyWorkoutArrangement(
        testStructureBoundary(sessionId),
        WorkoutArrangementDraft(
            activeWorkoutExerciseUuidsInOrder = idsInOrder.map { id -> requireNotNull(placementById[id]).uuid },
            setOrders = active.map { placement ->
                WorkoutSetOrderDraft(
                    placement.uuid,
                    allSets.filter { it.workoutExerciseId == placement.id }
                        .sortedWith(compareBy(WorkoutSet::position, WorkoutSet::id))
                        .map(WorkoutSet::uuid),
                )
            },
        ),
    )
}

internal suspend fun RoomGymRepository.normalizeWorkoutGroups(sessionId: Long) {
    normalizeActiveWorkoutStructure(sessionId)
}

internal suspend fun GymRepository.discardWorkout(sessionId: Long) {
    val session = requireNotNull(sessions.first().firstOrNull { it.id == sessionId })
    discardWorkout(WorkoutFinishBoundary(session.id, session.uuid, session.workoutRevision))
}

internal suspend fun GymRepository.copyWorkoutExerciseToActive(workoutExerciseId: Long): Long {
    val allPlacements = workoutExercises.first()
    val allSessions = sessions.first()
    val allSets = sets.first()
    val allGroups = groups.first()
    val source = requireNotNull(allPlacements.firstOrNull { it.id == workoutExerciseId })
    val sourceSession = requireNotNull(allSessions.firstOrNull { it.id == source.sessionId })
    val sourceSets = allSets.filter { it.workoutExerciseId == source.id && it.deletedAtMillis == null }
        .sortedWith(compareBy(WorkoutSet::position, WorkoutSet::id))
    val active = allSessions.firstOrNull { it.state == com.whip.app.domain.WorkoutSessionState.Active }
    val boundary = WorkoutExerciseCopyBoundary(
        sourceSessionId = sourceSession.id,
        sourceSessionUuid = sourceSession.uuid,
        sourceWorkoutExerciseId = source.id,
        sourceWorkoutExerciseUuid = source.uuid,
        sourceWorkoutExerciseUpdatedAtMillis = source.updatedAtMillis,
        sourceSets = sourceSets.map { WorkoutSetCopyBoundary(it.id, it.uuid, it.updatedAtMillis) },
        target = active?.let { workoutStructureBoundary(it, allPlacements, allGroups, allSets) },
    )
    return copyWorkoutExerciseToActive(
        boundary,
        requestedWorkoutExerciseUuid = UUID.randomUUID().toString(),
        requestedSetUuids = sourceSets.map { UUID.randomUUID().toString() },
    )
}
