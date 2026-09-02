package com.whip.app.data

import androidx.room.withTransaction
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.core.SettingsRepository
import com.whip.app.domain.BodyweightLoadPolicy
import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.EstimatedOneRepMaxFormula
import com.whip.app.domain.Exercise
import com.whip.app.domain.ExerciseCategory
import com.whip.app.domain.ExerciseCategoryLink
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.ExerciseTrackingType
import com.whip.app.domain.FiveThreeOneEvidenceKind
import com.whip.app.domain.FiveThreeOneEvidenceRow
import com.whip.app.domain.FiveThreeOneProgression
import com.whip.app.domain.FiveThreeOneProgressionCategory
import com.whip.app.domain.FiveThreeOneProgressionRecommendation
import com.whip.app.domain.GymMachine
import com.whip.app.domain.GymMachineDraft
import com.whip.app.domain.MachineLevelDirection
import com.whip.app.domain.MachineLoadType
import com.whip.app.domain.MachineStackMode
import com.whip.app.domain.LoadInterpretation
import com.whip.app.domain.canonicalResistanceKg
import com.whip.app.domain.loadInterpretationMultiplier
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.WorkoutExercise
import com.whip.app.domain.WorkoutExerciseCopyBoundary
import com.whip.app.domain.WorkoutGroup
import com.whip.app.domain.WorkoutGroupType
import com.whip.app.domain.WorkoutSession
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.domain.WorkoutExerciseOutcome
import com.whip.app.domain.WorkoutFinishBoundary
import com.whip.app.domain.WorkoutArrangementDraft
import com.whip.app.domain.WorkoutGroupLayoutSnapshot
import com.whip.app.domain.WorkoutLayoutSnapshot
import com.whip.app.domain.WorkoutPlacementMutationBoundary
import com.whip.app.domain.WorkoutSetMutationBoundary
import com.whip.app.domain.WorkoutSetOrderDraft
import com.whip.app.domain.WorkoutStructureBoundary
import com.whip.app.domain.WorkoutStructureMutationReceipt
import com.whip.app.domain.WorkoutSet
import com.whip.app.domain.WorkoutSetClassification
import com.whip.app.domain.WorkoutSetDraft
import com.whip.app.domain.WorkoutSetRemovalReason
import com.whip.app.domain.RoutineProgramKind
import com.whip.app.domain.RoutineEquipmentBindingState
import com.whip.app.domain.RoutineProgramPhaseRole
import com.whip.app.domain.RoutineProgressionMode
import com.whip.app.domain.RoutineMainWorkScheme
import com.whip.app.domain.RoutineOptionalWorkKind
import com.whip.app.domain.RoutineSupplementalScheme
import com.whip.app.domain.RoutineTrainingMaxSource
import com.whip.app.domain.RoutineAssistanceRole
import com.whip.app.domain.RoutineAssistanceCategory
import com.whip.app.domain.RoutinePlacementKind
import com.whip.app.domain.RoutineWorkSection
import com.whip.app.domain.TrainingMaxCycleDecision
import com.whip.app.domain.TrainingMaxDecisionAction
import com.whip.app.domain.validateWorkoutSetDraft
import com.whip.app.domain.applyPolicySnapshot
import com.whip.app.domain.massFromKilograms
import com.whip.app.domain.massToKilograms
import com.whip.app.domain.workoutLayoutSnapshot
import com.whip.app.domain.workoutStructureBoundary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.math.abs

interface GymRepository {
    val machines: Flow<List<GymMachine>>
    val exercises: Flow<List<Exercise>>
    val categories: Flow<List<ExerciseCategory>>
    val categoryLinks: Flow<List<ExerciseCategoryLink>>
    val sessions: Flow<List<WorkoutSession>>
    val workoutExercises: Flow<List<WorkoutExercise>>
    val sets: Flow<List<WorkoutSet>>
    val groups: Flow<List<WorkoutGroup>>
    /**
     * One transactionally coherent view of the workout graph. The default keeps test/fake
     * repositories source-compatible; Room overrides it so a set invalidation can never be
     * combined with the previous session revision in an authorship boundary.
     */
    val workoutSnapshot: Flow<GymWorkoutSnapshot>
        get() = combine(exercises, sessions, workoutExercises, sets, groups, ::GymWorkoutSnapshot)

    suspend fun createMachine(draft: GymMachineDraft): Long
    suspend fun updateMachine(id: Long, draft: GymMachineDraft)
    suspend fun createMachineVersion(id: Long, draft: GymMachineDraft): Long
    suspend fun setMachineArchived(id: Long, archived: Boolean)

    suspend fun createExercise(draft: ExerciseDraft): Long
    suspend fun createExerciseAndAddToWorkout(
        boundary: WorkoutStructureBoundary,
        draft: ExerciseDraft,
        requestedWorkoutExerciseUuid: String,
        requestedInitialSetUuid: String,
    ): WorkoutExerciseAdditionReceipt
    suspend fun createExerciseAndSubstitute(
        boundary: WorkoutPlacementMutationBoundary,
        draft: ExerciseDraft,
        requestedWorkoutExerciseUuid: String,
        requestedInitialSetUuid: String,
    ): Long
    suspend fun updateExercise(id: Long, draft: ExerciseDraft)
    suspend fun duplicateExercise(id: Long): Long
    suspend fun setExerciseArchived(id: Long, archived: Boolean)
    suspend fun setExerciseFavorite(id: Long, favorite: Boolean)
    suspend fun reorderExercises(idsInOrder: List<Long>)
    suspend fun createCategory(name: String, kind: String = "Category"): Long
    suspend fun updateCategory(id: Long, name: String, kind: String)
    suspend fun setCategoryArchived(id: Long, archived: Boolean)
    suspend fun reorderCategories(idsInOrder: List<Long>)

    suspend fun startWorkout(
        name: String = "",
        notes: String = "",
        startedAt: Instant? = null,
        localDate: LocalDate? = null,
        zoneId: ZoneId? = null,
        keepScreenAwake: Boolean? = null,
    ): Long
    suspend fun updateWorkout(id: Long, name: String, notes: String, keepScreenAwake: Boolean)
    /**
     * [trainingMaxDecisions] is used only at a performance-review boundary. Omitted or
     * absent lift decisions conservatively keep that lift's current Training Max. When supplied,
     * [expectedSessionUuid] and [expectedWorkoutRevision] bind the commit to the exact session
     * graph the user reviewed.
     */
    suspend fun finishWorkout(
        id: Long,
        trainingMaxDecisions: List<TrainingMaxCycleDecision>? = null,
        expectedWorkoutRevision: Long? = null,
        expectedSessionUuid: String? = null,
    ): WorkoutFinishReceipt
    suspend fun resumeWorkout(id: Long)
    suspend fun discardWorkout(boundary: WorkoutFinishBoundary)
    suspend fun restoreWorkout(id: Long)
    suspend fun duplicateWorkout(id: Long, asActive: Boolean = true): Long
    suspend fun copyWorkoutExerciseToActive(
        boundary: WorkoutExerciseCopyBoundary,
        requestedWorkoutExerciseUuid: String,
        requestedSetUuids: List<String>,
    ): Long

    suspend fun addExerciseToWorkout(sessionId: Long, exerciseId: Long, machineId: Long? = null): Long
    suspend fun addExerciseWithInitialSetToWorkout(
        boundary: WorkoutStructureBoundary,
        exerciseId: Long,
        machineId: Long? = null,
        requestedWorkoutExerciseUuid: String,
        requestedInitialSetUuid: String,
    ): WorkoutExerciseAdditionReceipt
    suspend fun updateWorkoutExerciseDetails(
        boundary: WorkoutPlacementMutationBoundary,
        notes: String,
        groupId: Long?,
        machineId: Long?,
    )
    suspend fun createMachineAndAssign(
        boundary: WorkoutPlacementMutationBoundary,
        draft: GymMachineDraft,
    ): Long
    suspend fun substituteWorkoutExercise(
        boundary: WorkoutPlacementMutationBoundary,
        exerciseId: Long,
        machineId: Long? = null,
        requestedWorkoutExerciseUuid: String,
        requestedInitialSetUuid: String,
    ): Long
    suspend fun removeWorkoutExercise(boundary: WorkoutPlacementMutationBoundary): WorkoutStructureMutationReceipt
    suspend fun removeWorkoutExerciseFromGroup(boundary: WorkoutPlacementMutationBoundary): WorkoutStructureMutationReceipt
    suspend fun applyWorkoutArrangement(
        boundary: WorkoutStructureBoundary,
        draft: WorkoutArrangementDraft,
    ): WorkoutStructureMutationReceipt
    suspend fun restoreWorkoutLayout(
        boundary: WorkoutStructureBoundary,
        snapshot: WorkoutLayoutSnapshot,
    ): WorkoutStructureMutationReceipt
    suspend fun normalizeActiveWorkoutStructure(sessionId: Long): WorkoutStructureMutationReceipt
    suspend fun createGroup(
        boundary: WorkoutStructureBoundary,
        requestedGroupUuid: String,
        name: String,
        type: WorkoutGroupType,
        workoutExerciseUuids: List<String>,
    ): WorkoutStructureMutationReceipt

    suspend fun addSet(boundary: WorkoutPlacementMutationBoundary, draft: WorkoutSetDraft? = null): Long
    suspend fun saveQuickSet(
        id: Long,
        expectedSetUuid: String,
        expectedSetUpdatedAtMillis: Long,
        expectedWorkoutRevision: Long? = null,
        draft: WorkoutSetDraft,
        addNext: Boolean,
        autoStartRest: Boolean,
        restOverrideSeconds: Int? = null,
    ): QuickSetCommitReceipt
    suspend fun updateSet(boundary: WorkoutSetMutationBoundary, draft: WorkoutSetDraft)
    suspend fun setSetCompleted(
        boundary: WorkoutSetMutationBoundary,
        completed: Boolean,
        autoStartRest: Boolean = true,
        restOverrideSeconds: Int? = null,
    )
    suspend fun duplicateSet(boundary: WorkoutSetMutationBoundary): Long
    suspend fun deleteSet(
        boundary: WorkoutSetMutationBoundary,
        reason: WorkoutSetRemovalReason = WorkoutSetRemovalReason.Removed,
    ): WorkoutStructureMutationReceipt
    suspend fun undoDeleteSet(boundary: WorkoutSetMutationBoundary): WorkoutStructureMutationReceipt

    suspend fun startRestTimer(sessionId: Long, seconds: Int)
    suspend fun adjustRestTimer(sessionId: Long, deltaSeconds: Int)
    suspend fun stopRestTimer(sessionId: Long)
    suspend fun acknowledgeRestTimerCleanup(sessionId: Long, expectedTimerRevision: Long)
    suspend fun completeRestTimerDelivery(
        sessionId: Long,
        expectedTimerRevision: Long,
        expectedDeadlineMillis: Long,
    ): Boolean
}

data class GymWorkoutSnapshot(
    val exercises: List<Exercise>,
    val sessions: List<WorkoutSession>,
    val workoutExercises: List<WorkoutExercise>,
    val sets: List<WorkoutSet>,
    val groups: List<WorkoutGroup>,
)

data class WorkoutFinishReceipt(
    val sessionId: Long,
    val sessionUuid: String,
    val exerciseIds: Set<Long>,
    val programProgressAdvanced: Boolean,
    val alreadyFinished: Boolean,
)

data class WorkoutExerciseAdditionReceipt(
    val sessionId: Long,
    val workoutExerciseId: Long,
    val workoutExerciseUuid: String,
    val initialSetId: Long,
)

data class QuickSetCommitReceipt(
    val sessionId: Long,
    val sessionUuid: String,
    val exerciseId: Long,
    val setId: Long,
    val setUuid: String,
    val appendedSetId: Long?,
    val restTimerSeconds: Int?,
)

class RoomGymRepository(
    private val database: WhipDatabase,
    private val clock: WhipClock,
    private val ids: WhipIdGenerator,
    private val settingsRepository: SettingsRepository? = null,
) : GymRepository {
    private val dao = database.gymDao()
    private val routineDao = database.routineDao()

    override val machines = combine(dao.observeMachines(), dao.observeMachineExerciseJoins()) { machines, joins ->
        val exerciseIdsByMachine = joins.groupBy(GymMachineExerciseJoinEntity::machineId)
            .mapValues { (_, values) -> values.mapTo(linkedSetOf(), GymMachineExerciseJoinEntity::exerciseId) }
        machines.map { machine -> machine.toDomain(exerciseIdsByMachine[machine.id].orEmpty()) }
    }
    override val exercises = dao.observeExercises().map { list -> list.map { it.toDomain() } }
    override val categories = dao.observeCategories().map { list -> list.map { it.toDomain() } }
    override val categoryLinks = dao.observeCategoryJoins().map { list -> list.map { ExerciseCategoryLink(it.exerciseId, it.categoryId) } }
    override val sessions = dao.observeSessions().map { list -> list.map { it.toDomain() } }
    override val workoutExercises = dao.observeWorkoutExercises().map { list -> list.map { it.toDomain() } }
    override val sets = dao.observeWorkoutSets().map { list -> list.map { it.toDomain() } }
    override val groups = dao.observeWorkoutGroups().map { list -> list.map { it.toDomain() } }
    override val workoutSnapshot = database.invalidationTracker.createFlow(
        "exercises",
        "workout_sessions",
        "workout_exercises",
        "workout_sets",
        "workout_groups",
    ).map {
        database.withTransaction {
            GymWorkoutSnapshot(
                exercises = dao.getAllExercises()
                    .sortedWith(
                        compareByDescending<ExerciseEntity> { it.favorite }
                            .thenBy(ExerciseEntity::position)
                            .thenBy(ExerciseEntity::name),
                    )
                    .map(ExerciseEntity::toDomain),
                sessions = dao.getAllSessions()
                    .sortedByDescending(WorkoutSessionEntity::startedAtMillis)
                    .map(WorkoutSessionEntity::toDomain),
                workoutExercises = dao.getAllWorkoutExercises()
                    .sortedWith(
                        compareBy<WorkoutExerciseEntity>(WorkoutExerciseEntity::sessionId)
                            .thenBy(WorkoutExerciseEntity::position)
                            .thenBy(WorkoutExerciseEntity::id),
                    )
                    .map(WorkoutExerciseEntity::toDomain),
                sets = dao.getAllWorkoutSets()
                    .sortedWith(
                        compareBy<WorkoutSetEntity>(WorkoutSetEntity::workoutExerciseId)
                            .thenBy(WorkoutSetEntity::position)
                            .thenBy(WorkoutSetEntity::id),
                    )
                    .map(WorkoutSetEntity::toDomain),
                groups = dao.getAllWorkoutGroups()
                    .sortedWith(
                        compareBy<WorkoutGroupEntity>(WorkoutGroupEntity::sessionId)
                            .thenBy(WorkoutGroupEntity::position)
                            .thenBy(WorkoutGroupEntity::id),
                    )
                    .map(WorkoutGroupEntity::toDomain),
            )
        }
    }

    override suspend fun createMachine(draft: GymMachineDraft): Long = database.withTransaction {
        createMachineInTransaction(draft)
    }

    private suspend fun createMachineInTransaction(draft: GymMachineDraft): Long {
        validateMachine(draft)
        val exerciseIds = draft.normalizedExerciseIds()
        exerciseIds.forEach { exerciseId ->
            requireNotNull(dao.getExercise(exerciseId)) { "Exercise no longer exists" }
        }
        val now = nowMillis()
        val uuid = ids.nextId()
        val machineId = dao.insertMachine(
            draft.copy(
                configurationGroupId = draft.configurationGroupId.ifBlank { uuid },
                configurationVersion = draft.configurationVersion.coerceAtLeast(1),
            ).toEntity(uuid = uuid, createdAtMillis = now, primaryExerciseId = exerciseIds.firstOrNull()),
        )
        syncMachineExercises(machineId, exerciseIds)
        return machineId
    }

    override suspend fun createMachineAndAssign(
        boundary: WorkoutPlacementMutationBoundary,
        draft: GymMachineDraft,
    ): Long = database.withTransaction {
        val (current, session) = requireExactActivePlacement(boundary)
        val machineId = createMachineInTransaction(draft)
        val now = nowMillis()
        check(updateWorkoutExerciseDetailsInSession(current, session, current.notes, current.groupId, machineId, now)) {
            "The new machine was not assigned"
        }
        bumpWorkoutRevision(session.id, now)
        machineId
    }

    override suspend fun createMachineVersion(id: Long, draft: GymMachineDraft): Long {
        val source = dao.getMachine(id) ?: error("Machine no longer exists")
        val siblings = dao.getAllMachines().filter { it.configurationGroupId == source.configurationGroupId }
        return createMachine(
            draft.copy(
                configurationGroupId = source.configurationGroupId.ifBlank { source.uuid },
                configurationVersion = (siblings.maxOfOrNull { it.configurationVersion } ?: source.configurationVersion) + 1,
            ),
        )
    }

    override suspend fun updateMachine(id: Long, draft: GymMachineDraft) = database.withTransaction {
        validateMachine(draft)
        val exerciseIds = draft.normalizedExerciseIds()
        exerciseIds.forEach { exerciseId ->
            requireNotNull(dao.getExercise(exerciseId)) { "Exercise no longer exists" }
        }
        val existing = dao.getMachine(id) ?: error("Machine no longer exists")
        val removedExerciseIds = dao.getMachineExerciseJoins(id).mapTo(mutableSetOf()) { it.exerciseId } - exerciseIds
        if (dao.getAllWorkoutExercises().any { it.machineId == id }) {
            require(
                draft.loadType.name == existing.loadType &&
                    (draft.loadType != MachineLoadType.Mass || draft.unitId == existing.unitId) &&
                    draft.loadInterpretation.name == existing.loadInterpretation &&
                    draft.baseLoadKg == existing.baseLoadKg &&
                    draft.pulleyRatio == existing.pulleyRatio &&
                    draft.stackMode.name == existing.stackMode &&
                    draft.addOnPlateKg == existing.addOnPlateKg &&
                    draft.massMappingKg.toStableMappingCsv() == existing.massMappingCsv &&
                    (draft.loadType != MachineLoadType.Level ||
                        draft.levelLabel.trim() == existing.levelLabel && draft.levelDirection.name == existing.levelDirection),
            ) {
                "The resistance scale is locked after this machine has workout history. Create a new machine profile for a changed stack or setup."
            }
        }
        dao.updateMachine(
            draft.toEntity(
                id = existing.id,
                uuid = existing.uuid,
                archived = existing.archived,
                createdAtMillis = existing.createdAtMillis,
                updatedAtMillis = nowMillis(),
                primaryExerciseId = exerciseIds.firstOrNull(),
            ),
        )
        if (removedExerciseIds.isNotEmpty()) {
            val now = nowMillis()
            routineDao.getAllExercises()
                .filter { placement -> placement.machineId == id && placement.exerciseId in removedExerciseIds }
                .forEach { placement ->
                    routineDao.updateExercise(
                        placement.copy(
                            machineId = null,
                            equipmentBindingState = RoutineEquipmentBindingState.NeedsEquipment.name,
                            updatedAtMillis = now,
                        ),
                    )
                }
        }
        syncMachineExercises(id, exerciseIds)
    }

    private suspend fun syncMachineExercises(machineId: Long, exerciseIds: Set<Long>) {
        dao.clearMachineExercises(machineId)
        exerciseIds.forEach { exerciseId ->
            dao.upsertMachineExerciseJoin(GymMachineExerciseJoinEntity(machineId, exerciseId))
        }
    }

    override suspend fun setMachineArchived(id: Long, archived: Boolean) {
        val existing = dao.getMachine(id) ?: error("Machine no longer exists")
        dao.updateMachine(existing.copy(archived = archived, updatedAtMillis = nowMillis()))
    }

    override suspend fun createExercise(draft: ExerciseDraft): Long = database.withTransaction {
        validateExercise(draft)
        val now = clock.now().toEpochMilli()
        val id = dao.insertExercise(
            draft.toEntity(
                uuid = ids.nextId(),
                position = dao.nextExercisePosition(),
                createdAtMillis = now,
            ),
        )
        syncCategories(id, draft.categoryIds)
        id
    }

    override suspend fun createExerciseAndAddToWorkout(
        boundary: WorkoutStructureBoundary,
        draft: ExerciseDraft,
        requestedWorkoutExerciseUuid: String,
        requestedInitialSetUuid: String,
    ): WorkoutExerciseAdditionReceipt = database.withTransaction {
        val exerciseId = requestedExerciseIdForReplay(requestedWorkoutExerciseUuid, draft) ?: run {
            requireExactStructure(boundary)
            createExercise(draft)
        }
        addExerciseWithInitialSetToWorkout(
            boundary,
            exerciseId,
            requestedWorkoutExerciseUuid = requestedWorkoutExerciseUuid,
            requestedInitialSetUuid = requestedInitialSetUuid,
        )
    }

    override suspend fun createExerciseAndSubstitute(
        boundary: WorkoutPlacementMutationBoundary,
        draft: ExerciseDraft,
        requestedWorkoutExerciseUuid: String,
        requestedInitialSetUuid: String,
    ): Long = database.withTransaction {
        val exerciseId = requestedExerciseIdForReplay(requestedWorkoutExerciseUuid, draft) ?: run {
            requireExactActivePlacement(boundary)
            createExercise(draft)
        }
        substituteWorkoutExercise(
            boundary,
            exerciseId,
            requestedWorkoutExerciseUuid = requestedWorkoutExerciseUuid,
            requestedInitialSetUuid = requestedInitialSetUuid,
        )
    }

    private suspend fun requestedExerciseIdForReplay(
        requestedWorkoutExerciseUuid: String,
        draft: ExerciseDraft,
    ): Long? {
        val requestedPlacement = dao.getWorkoutExerciseByUuid(requestedWorkoutExerciseUuid) ?: return null
        validateExercise(draft)
        val existing = requireNotNull(dao.getExercise(requestedPlacement.exerciseId)) {
            "The exercise created by this request no longer exists"
        }
        val expected = draft.toEntity(
            id = existing.id,
            uuid = existing.uuid,
            position = existing.position,
            favorite = existing.favorite,
            archived = existing.archived,
            createdAtMillis = existing.createdAtMillis,
            updatedAtMillis = existing.updatedAtMillis,
        )
        val categoryIds = dao.getCategoryJoins(existing.id).mapTo(mutableSetOf()) { it.categoryId }
        require(existing == expected && categoryIds == draft.categoryIds) {
            "This exercise-creation request conflicts with a newer catalog change"
        }
        return existing.id
    }

    override suspend fun updateExercise(id: Long, draft: ExerciseDraft) = database.withTransaction {
        validateExercise(draft)
        val existing = dao.getExercise(id) ?: error("Exercise no longer exists")
        val programmingSemanticsChanged = existing.trackingType != draft.trackingType.name ||
            existing.loadInterpretation != draft.loadInterpretation.name ||
            existing.bodyweightLoadPolicy != draft.bodyweightLoadPolicy.name ||
            existing.effectiveBodyweightPercent != draft.effectiveBodyweightPercent
        require(!programmingSemanticsChanged || routineDao.getAllExercises().none { it.exerciseId == id }) {
            "This exercise is used by a routine. Remove it from those routines before changing its tracking or load semantics."
        }
        dao.updateExercise(
            draft.toEntity(
                id = existing.id,
                uuid = existing.uuid,
                position = existing.position,
                favorite = existing.favorite,
                archived = existing.archived,
                createdAtMillis = existing.createdAtMillis,
                updatedAtMillis = clock.now().toEpochMilli(),
            ),
        )
        syncCategories(id, draft.categoryIds)
    }

    override suspend fun duplicateExercise(id: Long): Long = database.withTransaction {
        val source = dao.getExercise(id)?.toDomain() ?: error("Exercise no longer exists")
        createExercise(source.toDraft().copy(name = "${source.name} copy", categoryIds = dao.getCategoryJoins(id).mapTo(mutableSetOf()) { it.categoryId }))
    }

    override suspend fun setExerciseArchived(id: Long, archived: Boolean) {
        val existing = dao.getExercise(id) ?: error("Exercise no longer exists")
        dao.updateExercise(existing.copy(archived = archived, updatedAtMillis = nowMillis()))
    }

    override suspend fun setExerciseFavorite(id: Long, favorite: Boolean) {
        val existing = dao.getExercise(id) ?: error("Exercise no longer exists")
        dao.updateExercise(existing.copy(favorite = favorite, updatedAtMillis = nowMillis()))
    }

    override suspend fun reorderExercises(idsInOrder: List<Long>) = database.withTransaction {
        val requested = idsInOrder.distinct()
        val all = dao.getAllExercises()
        require(requested.all { id -> all.any { it.id == id } }) { "Exercise no longer exists" }
        val byId = all.associateBy(ExerciseEntity::id)
        val order = requested + all.filterNot { it.id in requested }.sortedBy(ExerciseEntity::position).map(ExerciseEntity::id)
        val now = nowMillis()
        order.forEachIndexed { index, id ->
            val exercise = requireNotNull(byId[id])
            if (exercise.position != index) dao.updateExercise(exercise.copy(position = index, updatedAtMillis = now))
        }
    }

    override suspend fun createCategory(name: String, kind: String): Long {
        require(name.isNotBlank()) { "Category name is required" }
        val now = nowMillis()
        return dao.insertCategory(ExerciseCategoryEntity(uuid = ids.nextId(), name = name.trim(), kind = kind.trim().ifBlank { "Category" }, position = dao.nextCategoryPosition(), archived = false, createdAtMillis = now, updatedAtMillis = now))
    }

    override suspend fun updateCategory(id: Long, name: String, kind: String) {
        require(name.isNotBlank()) { "Category name is required" }
        val current = dao.getCategory(id) ?: error("Category no longer exists")
        dao.updateCategory(current.copy(name = name.trim(), kind = kind.trim().ifBlank { "Category" }, updatedAtMillis = nowMillis()))
    }

    override suspend fun setCategoryArchived(id: Long, archived: Boolean) {
        val current = dao.getCategory(id) ?: error("Category no longer exists")
        dao.updateCategory(current.copy(archived = archived, updatedAtMillis = nowMillis()))
    }

    override suspend fun reorderCategories(idsInOrder: List<Long>) = database.withTransaction {
        val requested = idsInOrder.distinct()
        val all = dao.getAllCategories()
        require(requested.all { id -> all.any { it.id == id } }) { "Category no longer exists" }
        val byId = all.associateBy(ExerciseCategoryEntity::id)
        val order = requested + all.filterNot { it.id in requested }.sortedBy(ExerciseCategoryEntity::position).map(ExerciseCategoryEntity::id)
        val now = nowMillis()
        order.forEachIndexed { index, id ->
            val category = requireNotNull(byId[id])
            if (category.position != index) dao.updateCategory(category.copy(position = index, updatedAtMillis = now))
        }
    }

    private suspend fun syncCategories(exerciseId: Long, categoryIds: Set<Long>) {
        dao.clearExerciseCategories(exerciseId)
        categoryIds.forEach { categoryId ->
            requireNotNull(dao.getCategory(categoryId)) { "Category no longer exists" }
            dao.upsertCategoryJoin(ExerciseCategoryJoinEntity(exerciseId, categoryId))
        }
    }

    override suspend fun startWorkout(
        name: String,
        notes: String,
        startedAt: Instant?,
        localDate: LocalDate?,
        zoneId: ZoneId?,
        keepScreenAwake: Boolean?,
    ): Long = database.withTransaction {
        require(dao.getActiveSession() == null) { "Finish or discard the active workout first" }
        val start = startedAt ?: clock.now()
        val effectiveZone = zoneId ?: clock.zoneId()
        val now = clock.now().toEpochMilli()
        dao.insertSession(
            WorkoutSessionEntity(
                uuid = ids.nextId(),
                name = name.trim(),
                notes = notes.trim(),
                startedAtMillis = start.toEpochMilli(),
                endedAtMillis = null,
                localEpochDay = (
                    localDate
                        ?: startedAt?.atZone(effectiveZone)?.toLocalDate()
                        ?: clock.today(effectiveZone)
                ).toEpochDay(),
                zoneId = effectiveZone.id,
                state = WorkoutSessionState.Active.name,
                keepScreenAwake = keepScreenAwake ?: settingsRepository?.current()?.keepScreenAwake ?: false,
                restTimerDeadlineMillis = null,
                restTimerDurationSeconds = null,
                archived = false,
                createdAtMillis = now,
                updatedAtMillis = now,
                sourceRoutineId = null,
            ),
        )
    }

    override suspend fun updateWorkout(
        id: Long,
        name: String,
        notes: String,
        keepScreenAwake: Boolean,
    ) {
        check(dao.updateSessionMetadata(id, name.trim(), notes.trim(), keepScreenAwake, nowMillis()) == 1) {
            "Workout no longer exists"
        }
    }

    override suspend fun finishWorkout(
        id: Long,
        trainingMaxDecisions: List<TrainingMaxCycleDecision>?,
        expectedWorkoutRevision: Long?,
        expectedSessionUuid: String?,
    ) = database.withTransaction {
        val session = requireNotNull(dao.getSession(id)) { "Workout no longer exists" }
        require(expectedSessionUuid == null || session.uuid == expectedSessionUuid) {
            "The workout identity changed after this finish review. Review the current workout before finishing."
        }
        if (session.state == WorkoutSessionState.Finished.name) {
            return@withTransaction WorkoutFinishReceipt(
                sessionId = session.id,
                sessionUuid = session.uuid,
                exerciseIds = dao.getWorkoutExercises(session.id).mapTo(mutableSetOf(), WorkoutExerciseEntity::exerciseId),
                programProgressAdvanced = session.programProgressAdvanced,
                alreadyFinished = true,
            )
        }
        require(session.state == WorkoutSessionState.Active.name) { "Only an active workout can be finished" }
        require(expectedWorkoutRevision == null || session.workoutRevision == expectedWorkoutRevision) {
            "The workout changed after this finish review. Review the latest sets before finishing."
        }
        val now = nowMillis()
        val advanced = if (session.state == WorkoutSessionState.Active.name && !session.programProgressAdvanced) {
            advanceRoutineProgressForSession(session, now, trainingMaxDecisions)
        } else {
            session.programProgressAdvanced
        }
        dao.updateSession(
            session.copy(
                state = WorkoutSessionState.Finished.name,
                endedAtMillis = session.endedAtMillis ?: now,
                restTimerDeadlineMillis = null,
                restTimerDurationSeconds = null,
                restTimerRevision = session.restTimerRevision + 1,
                restTimerCleanupPending = true,
                updatedAtMillis = now,
                programProgressAdvanced = advanced,
            ),
        )
        WorkoutFinishReceipt(
            sessionId = session.id,
            sessionUuid = session.uuid,
            exerciseIds = dao.getWorkoutExercises(session.id).mapTo(mutableSetOf(), WorkoutExerciseEntity::exerciseId),
            programProgressAdvanced = advanced,
            alreadyFinished = false,
        )
    }

    private suspend fun advanceRoutineProgressForSession(
        session: WorkoutSessionEntity,
        now: Long,
        trainingMaxDecisions: List<TrainingMaxCycleDecision>?,
    ): Boolean {
        val routineId = session.sourceRoutineId ?: return false
        val routine = routineDao.getRoutine(routineId) ?: return false
        val kind = runCatching { RoutineProgramKind.valueOf(session.sourceRoutineProgramKind) }
            .getOrDefault(RoutineProgramKind.Static)
        val days = routineDao.getDays(routineId)
        if (kind == RoutineProgramKind.Static) {
            val day = session.sourceRoutineDayId?.let { sourceId -> days.firstOrNull { it.id == sourceId } }
                ?: session.sourceRoutineDayPosition?.let(days::getOrNull)
                ?: return false
            val expectedIndex = session.sourceRoutineDayProgressionIndex ?: return false
            if (day.progressionIndex != expectedIndex) return false
            val prescribedSets = dao.getWorkoutExercises(session.id).flatMap { workoutExercise ->
                dao.getWorkoutSets(workoutExercise.id).filter(WorkoutSetEntity::requiredForProgressionSnapshot)
            }
            val completedPrescribedWork = prescribedSets.isNotEmpty() && prescribedSets.all { set ->
                set.deletedAtMillis == null && set.completed &&
                    set.classification != WorkoutSetClassification.Failure.name
            }
            if (!completedPrescribedWork) return false
            routineDao.updateDay(day.copy(progressionIndex = expectedIndex + 1, updatedAtMillis = now))
            return true
        }

        val phase = session.sourceRoutinePhaseIndex ?: return false
        val cycle = session.sourceRoutineCycle ?: return false
        val dayPosition = session.sourceRoutineDayPosition ?: return false
        if (routine.programKind != kind.name ||
            routine.currentProgramPhaseIndex != phase ||
            routine.currentProgramCycle != cycle ||
            routine.nextProgramDayPosition != dayPosition
        ) return false

        val finalDay = dayPosition == days.lastIndex
        val finalPhase = phase == routine.programPhaseCount - 1
        val invalidatedExerciseIds = session.invalidatedMainExerciseIdsCsv
            .split(',').mapNotNull(String::toLongOrNull).toSet()
        val legacyUnknownInvalidation = session.requiredMainWorkInvalidated && invalidatedExerciseIds.isEmpty()
        val mainOutcomeByExerciseId = dao.getWorkoutExercises(session.id).mapNotNull { workoutExercise ->
            val mainSets = dao.getWorkoutSets(workoutExercise.id).filter {
                it.workSectionSnapshot == RoutineWorkSection.Main.name
            }
            if (mainSets.isEmpty()) return@mapNotNull null
            val completed = !legacyUnknownInvalidation && workoutExercise.exerciseId !in invalidatedExerciseIds &&
                mainSets.all { set ->
                    val repetitionsMet = set.prescribedRepetitions == null ||
                        (set.repetitions ?: Int.MIN_VALUE) >= set.prescribedRepetitions
                    val loadMet = set.prescribedCanonicalWeightKg == null ||
                        (set.canonicalWeightKg ?: Double.NEGATIVE_INFINITY) + 1e-9 >= set.prescribedCanonicalWeightKg
                    set.deletedAtMillis == null && set.completed &&
                        set.classification != WorkoutSetClassification.Failure.name && repetitionsMet && loadMet
                }
            workoutExercise.exerciseId to completed
        }.groupBy(Pair<Long, Boolean>::first)
            .mapValues { (_, outcomes) -> outcomes.all(Pair<Long, Boolean>::second) }
            .toMutableMap()
            .also { outcomes -> invalidatedExerciseIds.forEach { outcomes[it] = false } }

        val mainPlacements = days.flatMap { day ->
            routineDao.getExercises(day.id).filter { exercise ->
                routineDao.getSets(exercise.id).any { it.workSection == RoutineWorkSection.Main.name }
            }
        }
        val mainPlacementsByExerciseId = mainPlacements.groupBy(RoutineExerciseEntity::exerciseId)
        val eligibilityByExerciseId = mainPlacementsByExerciseId.mapValues { (exerciseId, placements) ->
            placements.all(RoutineExerciseEntity::trainingMaxIncreaseEligible) &&
                (mainOutcomeByExerciseId[exerciseId] ?: true)
        }
        mainPlacements.forEach { placement ->
            val eligible = eligibilityByExerciseId.getValue(placement.exerciseId)
            if (placement.trainingMaxIncreaseEligible != eligible) {
                routineDao.updateExercise(
                    placement.copy(trainingMaxIncreaseEligible = eligible, updatedAtMillis = now),
                )
            }
        }
        val increaseEligible = eligibilityByExerciseId.values.all { it }
        val configuredAdvanceBoundaries = routine.trainingMaxAdvanceAfterPhaseIndicesCsv
            .split(',').mapNotNull(String::toIntOrNull).toSet()
        val trainingMaxAdvanceBoundary = finalDay && phase in configuredAdvanceBoundaries
        val eligibilityForNextPosition = if (trainingMaxAdvanceBoundary) true else increaseEligible
        val next = when {
            !finalDay -> routine.copy(
                nextProgramDayPosition = dayPosition + 1,
                trainingMaxIncreaseEligible = increaseEligible,
                updatedAtMillis = now,
            )
            !finalPhase -> routine.copy(
                currentProgramPhaseIndex = phase + 1,
                nextProgramDayPosition = 0,
                trainingMaxIncreaseEligible = eligibilityForNextPosition,
                updatedAtMillis = now,
            )
            else -> routine.copy(
                currentProgramPhaseIndex = 0,
                currentProgramCycle = cycle + 1,
                nextProgramDayPosition = 0,
                trainingMaxIncreaseEligible = eligibilityForNextPosition,
                updatedAtMillis = now,
            )
        }
        routineDao.updateRoutine(next)
        if (trainingMaxAdvanceBoundary) {
            val mode = runCatching { RoutineProgressionMode.valueOf(routine.progressionMode) }
                .getOrDefault(RoutineProgressionMode.Standard)
            require(trainingMaxDecisions.orEmpty().map(TrainingMaxCycleDecision::exerciseId).distinct().size ==
                trainingMaxDecisions.orEmpty().size) { "Only one Training Max decision is allowed per lift" }
            val decisionsByExerciseId = trainingMaxDecisions.orEmpty().associateBy(TrainingMaxCycleDecision::exerciseId)
            decisionsByExerciseId.keys.let { keys ->
                require(keys.all { it in mainPlacementsByExerciseId }) {
                    "Training Max decisions must reference a programmed Main lift"
                }
            }
            mainPlacementsByExerciseId.forEach { (exerciseId, placements) ->
                val representative = placements.first()
                val current = representative.trainingMaxValue ?: return@forEach
                val standard = representative.cycleIncrementValue ?: return@forEach
                val eligible = eligibilityByExerciseId[exerciseId] == true
                val decision = decisionsByExerciseId[exerciseId]
                val recommendation = if (mode == RoutineProgressionMode.PerformanceInformed && decision != null) {
                    recommendationForBoundary(
                        routine = routine,
                        boundarySession = session,
                        exerciseId = exerciseId,
                        representative = representative,
                        currentTrainingMax = current,
                        standardIncrement = standard,
                    )
                } else {
                    null
                }
                val requestedDelta = when (mode) {
                    RoutineProgressionMode.Standard -> if (eligible) standard else 0.0
                    RoutineProgressionMode.PerformanceInformed -> {
                        if (decision == null) {
                            0.0
                        } else {
                            require(decision.expectedCurrentTrainingMax?.let { abs(it - current) <= 1e-9 } == true) {
                                "Training Max review is stale; the current Training Max changed"
                            }
                            require(abs(decision.standardDelta - standard) <= 1e-9) {
                                "Training Max review is stale; the configured increase changed"
                            }
                            val recomputed = requireNotNull(recommendation)
                            when (decision.action) {
                                TrainingMaxDecisionAction.UseStandard -> require(
                                    eligible && abs(decision.requestedDelta - standard) <= 1e-9,
                                ) { "Use Standard must apply the configured increase to an eligible lift" }
                                TrainingMaxDecisionAction.Hold -> require(abs(decision.requestedDelta) <= 1e-9) {
                                    "Hold must keep the current Training Max"
                                }
                                TrainingMaxDecisionAction.IgnoreRecommendation -> require(
                                    abs(decision.requestedDelta) <= 1e-9,
                                ) { "Ignoring a recommendation must keep the current Training Max" }
                                TrainingMaxDecisionAction.UseSuggestion -> {
                                    require(recomputed.category != FiveThreeOneProgressionCategory.InsufficientEvidence) {
                                        "Insufficient evidence cannot be applied as a recommendation"
                                    }
                                    require(abs(decision.requestedDelta - recomputed.suggestedDelta) <= 1e-9) {
                                        "Training Max suggestion is stale or does not match persisted evidence"
                                    }
                                }
                                TrainingMaxDecisionAction.Custom -> Unit
                            }
                            decision.requestedDelta
                        }
                    }
                }
                require(requestedDelta.isFinite() && requestedDelta >= -standard && requestedDelta <= standard * 2.0) {
                    "Training Max change must be from one standard decrease through twice the configured increase"
                }
                require(eligible || requestedDelta <= 0.0) {
                    "A positive Training Max change requires completed Main work for that lift"
                }
                require(current + requestedDelta > 0.0) {
                    "Training Max change must leave a positive Training Max"
                }
                if (requestedDelta > standard + 1e-9) {
                    require(routine.allowNonStandardHigherSuggestions &&
                        recommendation?.category == FiveThreeOneProgressionCategory.CautiousHigherIncrease
                    ) {
                        "An above-standard Training Max change requires enabled, corroborated higher evidence"
                    }
                }
                placements.forEach { placement ->
                    val updatedValue = current + requestedDelta
                    routineDao.updateExercise(
                        placement.copy(
                            trainingMaxValue = updatedValue,
                            trainingMaxKg = massToKilograms(updatedValue, placement.trainingMaxUnitId),
                            trainingMaxIncreaseEligible = true,
                            updatedAtMillis = now,
                        ),
                    )
                }
                val audit = when {
                    mode == RoutineProgressionMode.PerformanceInformed && decision != null -> {
                        val recomputed = requireNotNull(recommendation)
                        TrainingMaxCycleDecision(
                            exerciseId = exerciseId,
                            expectedCurrentTrainingMax = current,
                            requestedDelta = requestedDelta,
                            standardDelta = standard,
                            recommendationCategory = recomputed.category.name,
                            recommendationDelta = recomputed.suggestedDelta,
                            confidence = recomputed.confidence,
                            reasons = recomputed.reasons + if (
                                decision.action == TrainingMaxDecisionAction.IgnoreRecommendation
                            ) {
                                listOf("The user declined Whip's advisory recommendation for this cycle.")
                            } else {
                                emptyList()
                            },
                            engineVersion = recomputed.engineVersion,
                            action = decision.action,
                        )
                    }
                    mode == RoutineProgressionMode.Standard -> TrainingMaxCycleDecision(
                        exerciseId = exerciseId,
                        expectedCurrentTrainingMax = current,
                        requestedDelta = requestedDelta,
                        standardDelta = standard,
                        recommendationCategory = if (eligible) {
                            FiveThreeOneProgressionCategory.StandardIncrease.name
                        } else {
                            FiveThreeOneProgressionCategory.Hold.name
                        },
                        recommendationDelta = requestedDelta,
                        confidence = 1.0,
                        reasons = listOf(
                            if (eligible) {
                                "Applied the configured standard 5/3/1 cycle increase after completed Main work."
                            } else {
                                "Held this lift because its required Main work was not completed."
                            },
                        ),
                        engineVersion = "five-three-one-standard/1",
                        action = if (eligible) TrainingMaxDecisionAction.UseStandard else TrainingMaxDecisionAction.Hold,
                    )
                    else -> null
                }
                if (audit != null) {
                    val exercise = requireNotNull(dao.getExercise(exerciseId)) {
                        "Training Max exercise no longer exists"
                    }
                    routineDao.insertTrainingMaxDecision(
                        TrainingMaxDecisionEntity(
                            uuid = ids.nextId(),
                            routineUuid = routine.uuid,
                            sessionUuid = session.uuid,
                            exerciseUuid = exercise.uuid,
                            exerciseName = exercise.name,
                            cycle = cycle,
                            previousTrainingMax = current,
                            appliedDelta = requestedDelta,
                            resultingTrainingMax = current + requestedDelta,
                            unitId = representative.trainingMaxUnitId,
                            standardDelta = standard,
                            recommendationCategory = audit.recommendationCategory,
                            recommendationDelta = audit.recommendationDelta,
                            confidence = audit.confidence,
                            reasonsText = audit.reasons.joinToString("\n"),
                            engineVersion = audit.engineVersion,
                            action = audit.action.name,
                            createdAtMillis = now,
                        ),
                    )
                }
            }
        }
        return true
    }

    /** Recomputes recommendations from persisted, immutable workout snapshots inside the finish transaction. */
    private suspend fun recommendationForBoundary(
        routine: GymRoutineEntity,
        boundarySession: WorkoutSessionEntity,
        exerciseId: Long,
        representative: RoutineExerciseEntity,
        currentTrainingMax: Double,
        standardIncrement: Double,
    ): FiveThreeOneProgressionRecommendation {
        val sourceSessions = dao.getAllSessions().filter { candidate ->
            candidate.sourceRoutineId == routine.id &&
                candidate.sourceRoutineCycle == boundarySession.sourceRoutineCycle &&
                candidate.sourceRoutineProgramKind == routine.programKind &&
                !candidate.archived &&
                (candidate.id == boundarySession.id && candidate.state == WorkoutSessionState.Active.name ||
                    candidate.state == WorkoutSessionState.Finished.name && candidate.programProgressAdvanced)
        }
        val sessionsById = sourceSessions.associateBy(WorkoutSessionEntity::id)
        val placements = dao.getAllWorkoutExercises().filter { placement ->
            placement.sessionId in sessionsById && placement.exerciseId == exerciseId &&
                placement.trainingMaxUnitIdSnapshot == representative.trainingMaxUnitId &&
                placement.trainingMaxValueSnapshot?.let { abs(it - currentTrainingMax) <= 1e-9 } == true
        }
        val setsByPlacement = dao.getAllWorkoutSets().groupBy(WorkoutSetEntity::workoutExerciseId)
        val evidence = buildList {
            placements.forEach { placement ->
                val evidenceSession = sessionsById.getValue(placement.sessionId)
                val phaseRole = runCatching { RoutineProgramPhaseRole.valueOf(evidenceSession.sourceRoutinePhaseRole) }
                    .getOrDefault(RoutineProgramPhaseRole.Standard)
                if (phaseRole == RoutineProgramPhaseRole.Deload) return@forEach
                setsByPlacement[placement.id].orEmpty().forEach { set ->
                    val workSection = runCatching { RoutineWorkSection.valueOf(set.workSectionSnapshot) }
                        .getOrDefault(RoutineWorkSection.Unspecified)
                    val optionalKind = runCatching { RoutineOptionalWorkKind.valueOf(set.optionalWorkKindSnapshot) }
                        .getOrDefault(RoutineOptionalWorkKind.None)
                    val classification = runCatching { WorkoutSetClassification.valueOf(set.classification) }
                        .getOrDefault(WorkoutSetClassification.Working)
                    val prescribedClassification = runCatching {
                        WorkoutSetClassification.valueOf(set.prescribedClassificationSnapshot)
                    }.getOrDefault(classification)
                    val kind = when {
                        workSection == RoutineWorkSection.Optional && optionalKind == RoutineOptionalWorkKind.Joker ->
                            FiveThreeOneEvidenceKind.Joker
                        workSection != RoutineWorkSection.Main -> return@forEach
                        phaseRole == RoutineProgramPhaseRole.TrainingMaxTest &&
                            prescribedClassification == WorkoutSetClassification.TrainingMaxTest ->
                            FiveThreeOneEvidenceKind.TrainingMaxTest
                        prescribedClassification == WorkoutSetClassification.Amrap -> FiveThreeOneEvidenceKind.PrSet
                        else -> FiveThreeOneEvidenceKind.RequiredMain
                    }
                    add(
                        FiveThreeOneEvidenceRow(
                            kind = kind,
                            exposureId = evidenceSession.uuid,
                            trainingMaxAtExposure = placement.trainingMaxValueSnapshot,
                            completed = set.completed,
                            deleted = set.deletedAtMillis != null,
                            failure = classification == WorkoutSetClassification.Failure,
                            prescribedReps = set.prescribedRepetitions,
                            actualReps = set.repetitions,
                            prescribedLoad = set.prescribedCanonicalWeightKg?.let { kg ->
                                massFromKilograms(kg, representative.trainingMaxUnitId)
                            },
                            actualLoad = set.canonicalWeightKg?.let { kg ->
                                massFromKilograms(kg, representative.trainingMaxUnitId)
                            },
                            rpe = set.rpe,
                            rir = set.rir,
                        ),
                    )
                }
            }
            sourceSessions.forEach { evidenceSession ->
                val invalidatedIds = evidenceSession.invalidatedMainExerciseIdsCsv
                    .split(',').mapNotNull(String::toLongOrNull).toSet()
                if (evidenceSession.requiredMainWorkInvalidated &&
                    (invalidatedIds.isEmpty() || exerciseId in invalidatedIds)
                ) {
                    add(
                        FiveThreeOneEvidenceRow(
                            kind = FiveThreeOneEvidenceKind.RequiredMain,
                            exposureId = evidenceSession.uuid,
                            trainingMaxAtExposure = currentTrainingMax,
                            completed = true,
                            failure = true,
                            prescribedReps = 1,
                            actualReps = 0,
                            prescribedLoad = currentTrainingMax,
                            actualLoad = 0.0,
                        ),
                    )
                }
            }
        }
        return FiveThreeOneProgression.recommend(
            evidence = evidence,
            currentTrainingMax = currentTrainingMax,
            standardIncrement = standardIncrement,
            allowNonStandardHigher = routine.allowNonStandardHigherSuggestions,
        )
    }

    override suspend fun resumeWorkout(id: Long) = database.withTransaction {
        val session = requireNotNull(dao.getSession(id)) { "Workout no longer exists" }
        val active = dao.getActiveSession()
        require(active == null || active.id == id) { "Another workout is active" }
        if (session.state == WorkoutSessionState.Active.name) return@withTransaction
        check(dao.resumeSession(id, nowMillis()) == 1) { "Workout state changed before it could be resumed" }
    }

    override suspend fun discardWorkout(boundary: WorkoutFinishBoundary) = database.withTransaction {
        val session = requireNotNull(dao.getSession(boundary.sessionId)) { "Workout no longer exists" }
        require(session.uuid == boundary.sessionUuid && session.workoutRevision == boundary.workoutRevision) {
            "The workout changed after the discard review opened"
        }
        if (session.state == WorkoutSessionState.Discarded.name) return@withTransaction
        require(session.state == WorkoutSessionState.Active.name) { "Only an active workout can be discarded" }
        check(dao.discardActiveSession(boundary.sessionId, nowMillis()) == 1) {
            "Workout changed before it could be discarded"
        }
    }

    override suspend fun restoreWorkout(id: Long) = database.withTransaction {
        val session = requireNotNull(dao.getSession(id)) { "Workout no longer exists" }
        if (session.state == WorkoutSessionState.Finished.name && !session.archived) return@withTransaction
        require(session.state == WorkoutSessionState.Discarded.name) { "Only a discarded workout can be restored" }
        check(dao.restoreDiscardedSession(id, nowMillis()) == 1) {
            "Workout changed before it could be restored"
        }
    }

    override suspend fun duplicateWorkout(id: Long, asActive: Boolean): Long =
        database.withTransaction {
            if (asActive) require(dao.getActiveSession() == null) { "Another workout is active" }
            val source = dao.getSession(id) ?: error("Workout no longer exists")
            val sourceWorkoutExercises = dao.getWorkoutExercises(id).mapNotNull { workoutExercise ->
                workoutExercise.toWorkoutReuseProjection(dao.getWorkoutSets(workoutExercise.id))
            }
            sourceWorkoutExercises.forEach { projection ->
                projection.workoutExercise.requireResolvedEquipmentForNewWorkout()
            }
            val now = nowMillis()
            val newSessionId = dao.insertSession(
                source.copy(
                    id = 0,
                    uuid = ids.nextId(),
                    name = source.name.ifBlank { "Copied workout" },
                    startedAtMillis = now,
                    endedAtMillis = null,
                    localEpochDay = clock.today().toEpochDay(),
                    zoneId = clock.zoneId().id,
                    state = if (asActive) WorkoutSessionState.Active.name else WorkoutSessionState.Finished.name,
                    restTimerDeadlineMillis = null,
                    archived = false,
                    createdAtMillis = now,
                    updatedAtMillis = now,
                    sourceRoutineId = null,
                    sourceRoutineDayId = null,
                    sourceRoutineProgramKind = RoutineProgramKind.Static.name,
                    sourceRoutinePhaseIndex = null,
                    sourceRoutineCycle = null,
                    sourceRoutineDayPosition = null,
                    sourceRoutineDayProgressionIndex = null,
                    programProgressAdvanced = false,
                    requiredMainWorkInvalidated = false,
                    workoutRevision = 0,
                    sourceRoutinePhaseLabel = "",
                    sourceRoutinePhaseRole = RoutineProgramPhaseRole.Standard.name,
                ),
            )
            sourceWorkoutExercises.forEachIndexed { position, projection ->
                val sourceWorkoutExercise = projection.workoutExercise
                val newWorkoutExerciseId = dao.insertWorkoutExercise(
                    sourceWorkoutExercise.copy(
                        id = 0,
                        uuid = ids.nextId(),
                        sessionId = newSessionId,
                        position = position,
                        groupId = null,
                        outcome = WorkoutExerciseOutcome.Active.name,
                        outcomeAtMillis = null,
                        replacementWorkoutExerciseUuid = null,
                        createdAtMillis = now,
                        updatedAtMillis = now,
                    ),
                )
                projection.sets.forEachIndexed { setPosition, sourceSet ->
                    dao.insertWorkoutSet(
                        sourceSet.asWorkoutOnlyCopy(
                            id = 0,
                            uuid = ids.nextId(),
                            workoutExerciseId = newWorkoutExerciseId,
                            position = setPosition,
                            createdAtMillis = now,
                            updatedAtMillis = now,
                        ),
                    )
                }
            }
            newSessionId
        }

    override suspend fun copyWorkoutExerciseToActive(
        boundary: WorkoutExerciseCopyBoundary,
        requestedWorkoutExerciseUuid: String,
        requestedSetUuids: List<String>,
    ): Long =
        database.withTransaction {
            require(requestedWorkoutExerciseUuid.isNotBlank() && requestedSetUuids.none(String::isBlank)) {
                "Copy request identity is missing"
            }
            dao.getWorkoutExerciseByUuid(requestedWorkoutExerciseUuid)?.let { existing ->
                val existingSets = dao.getWorkoutSets(existing.id).sortedWith(compareBy({ it.position }, { it.id }))
                val existingSession = requireNotNull(dao.getSession(existing.sessionId)) { "Copied workout no longer exists" }
                require(
                    existing.exerciseId == dao.getWorkoutExercise(boundary.sourceWorkoutExerciseId)?.exerciseId &&
                        existing.outcome == WorkoutExerciseOutcome.Active.name &&
                        existingSession.state == WorkoutSessionState.Active.name &&
                        (boundary.target == null || existing.sessionId == boundary.target.sessionId) &&
                        existingSets.map { it.uuid } == requestedSetUuids,
                ) { "This copy request conflicts with a newer workout change" }
                return@withTransaction existing.id
            }
            require(requestedSetUuids.distinct().size == requestedSetUuids.size) {
                "Each copied Set needs a unique request identity"
            }
            val sourceSession = requireNotNull(dao.getSession(boundary.sourceSessionId)) { "Source workout no longer exists" }
            require(sourceSession.uuid == boundary.sourceSessionUuid) { "Source workout changed after review" }
            val source = requireNotNull(dao.getWorkoutExercise(boundary.sourceWorkoutExerciseId)) {
                "Workout exercise no longer exists"
            }
            require(
                source.uuid == boundary.sourceWorkoutExerciseUuid &&
                    source.updatedAtMillis == boundary.sourceWorkoutExerciseUpdatedAtMillis &&
                    source.sessionId == sourceSession.id,
            ) { "Source exercise changed after review" }
            val sourceSets = dao.getWorkoutSets(source.id)
                .filter { it.deletedAtMillis == null }
                .sortedWith(compareBy({ it.position }, { it.id }))
            require(
                sourceSets.map { Triple(it.id, it.uuid, it.updatedAtMillis) } ==
                    boundary.sourceSets.map { Triple(it.setId, it.setUuid, it.setUpdatedAtMillis) },
            ) { "Source Sets changed after review" }
            require(requestedSetUuids.size == sourceSets.size) { "Copy Set request count changed" }
            source.requireResolvedEquipmentForNewWorkout()
            val targetSessionId = boundary.target?.let { target ->
                requireExactStructure(target).id
            } ?: run {
                require(dao.getActiveSession() == null) { "An active workout appeared after this copy was reviewed" }
                startWorkout(name = "Copied workout")
            }
            val now = nowMillis()
            val targetId = dao.insertWorkoutExercise(
                source.copy(
                    id = 0, uuid = requestedWorkoutExerciseUuid, sessionId = targetSessionId,
                    position = dao.nextWorkoutExercisePosition(targetSessionId), groupId = null,
                    outcome = WorkoutExerciseOutcome.Active.name,
                    outcomeAtMillis = null,
                    replacementWorkoutExerciseUuid = null,
                    createdAtMillis = now, updatedAtMillis = now,
                ),
            )
            sourceSets.forEachIndexed { index, set ->
                dao.insertWorkoutSet(
                    set.asWorkoutOnlyCopy(
                        id = 0, uuid = requestedSetUuids[index], workoutExerciseId = targetId,
                        position = index, createdAtMillis = now, updatedAtMillis = now,
                    ),
                )
            }
            bumpWorkoutRevision(targetSessionId, now)
            targetId
        }

    override suspend fun addExerciseToWorkout(sessionId: Long, exerciseId: Long, machineId: Long?): Long =
        database.withTransaction {
            val now = nowMillis()
            val (workoutExerciseId, session) = insertWorkoutExerciseToActiveSession(
                sessionId = sessionId,
                exerciseId = exerciseId,
                machineId = machineId,
                now = now,
                workoutExerciseUuid = ids.nextId(),
            )
            bumpWorkoutRevision(sessionId, now)
            workoutExerciseId
        }

    override suspend fun addExerciseWithInitialSetToWorkout(
        boundary: WorkoutStructureBoundary,
        exerciseId: Long,
        machineId: Long?,
        requestedWorkoutExerciseUuid: String,
        requestedInitialSetUuid: String,
    ): WorkoutExerciseAdditionReceipt = database.withTransaction {
        require(requestedWorkoutExerciseUuid.isNotBlank() && requestedInitialSetUuid.isNotBlank()) {
            "Workout exercise request identity is missing"
        }
        dao.getWorkoutExerciseByUuid(requestedWorkoutExerciseUuid)?.let { existing ->
            val existingSession = requireNotNull(dao.getSession(existing.sessionId)) { "Workout no longer exists" }
            val existingSet = requireNotNull(dao.getWorkoutSetByUuid(requestedInitialSetUuid)) {
                "The previous add request is incomplete"
            }
            require(
                existing.sessionId == boundary.sessionId &&
                    existingSession.uuid == boundary.sessionUuid &&
                    existing.exerciseId == exerciseId &&
                    existing.machineId == machineId &&
                    existing.outcome == WorkoutExerciseOutcome.Active.name &&
                    existingSet.workoutExerciseId == existing.id,
            ) { "This add request conflicts with a newer workout change" }
            return@withTransaction WorkoutExerciseAdditionReceipt(
                sessionId = existing.sessionId,
                workoutExerciseId = existing.id,
                workoutExerciseUuid = existing.uuid,
                initialSetId = existingSet.id,
            )
        }
        val reviewedSession = requireExactStructure(boundary)
        val now = nowMillis()
        val (workoutExerciseId, session) = insertWorkoutExerciseToActiveSession(
            sessionId = reviewedSession.id,
            exerciseId = exerciseId,
            machineId = machineId,
            now = now,
            workoutExerciseUuid = requestedWorkoutExerciseUuid,
        )
        val placement = requireNotNull(dao.getWorkoutExercise(workoutExerciseId))
        val initialSetId = insertSetForActivePlacement(
            placement,
            session,
            draft = null,
            now = now,
            requestedSetUuid = requestedInitialSetUuid,
        )
        bumpWorkoutRevision(session.id, now)
        WorkoutExerciseAdditionReceipt(
            sessionId = session.id,
            workoutExerciseId = workoutExerciseId,
            workoutExerciseUuid = placement.uuid,
            initialSetId = initialSetId,
        )
    }

    private suspend fun insertWorkoutExerciseToActiveSession(
        sessionId: Long,
        exerciseId: Long,
        machineId: Long?,
        now: Long,
        workoutExerciseUuid: String,
    ): Pair<Long, WorkoutSessionEntity> {
        val session = requireNotNull(dao.getSession(sessionId)) { "Workout no longer exists" }
        require(session.state == WorkoutSessionState.Active.name) { "Exercises can only be added to an active workout" }
        val exercise = requireNotNull(dao.getExercise(exerciseId)) { "Exercise no longer exists" }
        require(!exercise.archived) { "Restore this exercise before adding it to a workout" }
        val machine = machineId?.let { selected ->
            requireNotNull(dao.getMachine(selected)) { "Machine no longer exists" }
                .also {
                    require(dao.machineSupportsExercise(it.id, exerciseId) && !it.archived) {
                        "Machine is not available for this exercise"
                    }
                }
        }
        val workoutExerciseId = dao.insertWorkoutExercise(
            WorkoutExerciseEntity(
                uuid = workoutExerciseUuid,
                sessionId = sessionId,
                exerciseId = exerciseId,
                position = dao.nextWorkoutExercisePosition(sessionId),
                notes = "",
                groupId = null,
                createdAtMillis = now,
                updatedAtMillis = now,
                machineProfileUuidSnapshot = machine?.uuid,
                machineId = machine?.id,
                machineNameSnapshot = machine?.displayName().orEmpty(),
                machineLoadTypeSnapshot = machine?.loadType.orEmpty(),
                machineUnitIdSnapshot = machine?.unitId.orEmpty(),
                machineLevelLabelSnapshot = machine?.levelLabel.orEmpty(),
                loadInterpretationSnapshot = machine?.loadInterpretation ?: exercise.loadInterpretation,
                baseLoadKgSnapshot = machine?.baseLoadKg
                    ?: exercise.barWeightKg.takeIf {
                        (machine?.loadInterpretation ?: exercise.loadInterpretation) == LoadInterpretation.PerSide.name
                    },
                trackingTypeSnapshot = exercise.trackingType,
                bodyweightLoadPolicySnapshot = exercise.bodyweightLoadPolicy,
                effectiveBodyweightPercentSnapshot = exercise.effectiveBodyweightPercent,
                oneRepMaxFormulaSnapshot = exercise.oneRepMaxFormula,
                includeInVolumeSnapshot = exercise.includeInVolume,
                includeInPersonalRecordsSnapshot = exercise.includeInPersonalRecords,
                exerciseWeightUnitSnapshot = exercise.weightUnitId,
                loadMultiplierSnapshot = loadInterpretationMultiplier(
                    runCatching { LoadInterpretation.valueOf(machine?.loadInterpretation ?: exercise.loadInterpretation) }
                        .getOrDefault(LoadInterpretation.Total),
                    machine?.stackMode?.let { runCatching { MachineStackMode.valueOf(it) }.getOrNull() }
                        ?: MachineStackMode.Single,
                    machine?.pulleyRatio ?: 1.0,
                ),
                machineConfigurationGroupSnapshot = machine?.configurationGroupId.orEmpty(),
                machineConfigurationVersionSnapshot = machine?.configurationVersion ?: 1,
                machineConfigurationSnapshot = machine?.configurationSummary().orEmpty(),
                machinePulleyRatioSnapshot = machine?.pulleyRatio ?: 1.0,
                machineStackModeSnapshot = machine?.stackMode ?: MachineStackMode.Single.name,
                machineAddOnPlateKgSnapshot = machine?.addOnPlateKg,
                machineMassMappingCsvSnapshot = machine?.massMappingCsv.orEmpty(),
            ),
        )
        return workoutExerciseId to session
    }

    override suspend fun updateWorkoutExerciseDetails(
        boundary: WorkoutPlacementMutationBoundary,
        notes: String,
        groupId: Long?,
        machineId: Long?,
    ) = database.withTransaction {
        val (current, session) = requireExactActivePlacement(boundary)
        val now = nowMillis()
        if (updateWorkoutExerciseDetailsInSession(current, session, notes, groupId, machineId, now)) {
            bumpWorkoutRevision(session.id, now)
        }
    }

    private suspend fun updateWorkoutExerciseDetailsInSession(
        current: WorkoutExerciseEntity,
        session: WorkoutSessionEntity,
        notes: String,
        groupId: Long?,
        machineId: Long?,
        now: Long,
    ): Boolean {
        if (groupId != null) {
            require(dao.getWorkoutGroups(session.id).any { it.id == groupId }) {
                "Exercise group no longer exists in this workout"
            }
        }
        val trimmedNotes = notes.trim()
        val machineChanged = current.machineId != machineId
        if (current.notes == trimmedNotes && current.groupId == groupId && !machineChanged) return false
        val sets = dao.getWorkoutSets(current.id)
        if (machineChanged) {
            require(sets.none { it.completed || it.deletedAtMillis != null }) {
                "Machine cannot be changed after a set is completed or removed. Add the exercise again to preserve history."
            }
        }
        val machine = if (machineChanged) machineId?.let { selected ->
            requireNotNull(dao.getMachine(selected)) { "Machine no longer exists" }
                .also {
                    require(dao.machineSupportsExercise(it.id, current.exerciseId) && !it.archived) {
                        "Machine is not available for this exercise"
                    }
                }
        } else null
        val exercise = requireNotNull(dao.getExercise(current.exerciseId)) { "Exercise no longer exists" }
        val baseUpdated = current.copy(
            notes = trimmedNotes,
            groupId = groupId,
            updatedAtMillis = now,
        )
        val updated = if (!machineChanged) baseUpdated else baseUpdated.copy(
            machineProfileUuidSnapshot = machine?.uuid,
            machineId = machine?.id,
            machineNameSnapshot = machine?.displayName().orEmpty(),
            machineLoadTypeSnapshot = machine?.loadType.orEmpty(),
            machineUnitIdSnapshot = machine?.unitId.orEmpty(),
            machineLevelLabelSnapshot = machine?.levelLabel.orEmpty(),
            loadInterpretationSnapshot = machine?.loadInterpretation ?: exercise.loadInterpretation,
            baseLoadKgSnapshot = machine?.baseLoadKg ?: exercise.barWeightKg.takeIf {
                (machine?.loadInterpretation ?: exercise.loadInterpretation) == LoadInterpretation.PerSide.name
            },
            exerciseWeightUnitSnapshot = exercise.weightUnitId,
            loadMultiplierSnapshot = loadInterpretationMultiplier(
                runCatching { LoadInterpretation.valueOf(machine?.loadInterpretation ?: exercise.loadInterpretation) }
                    .getOrDefault(LoadInterpretation.Total),
                machine?.stackMode?.let { runCatching { MachineStackMode.valueOf(it) }.getOrNull() }
                    ?: MachineStackMode.Single,
                machine?.pulleyRatio ?: 1.0,
            ),
            machineConfigurationGroupSnapshot = machine?.configurationGroupId.orEmpty(),
            machineConfigurationVersionSnapshot = machine?.configurationVersion ?: 1,
            machineConfigurationSnapshot = machine?.configurationSummary().orEmpty(),
            machinePulleyRatioSnapshot = machine?.pulleyRatio ?: 1.0,
            machineStackModeSnapshot = machine?.stackMode ?: MachineStackMode.Single.name,
            machineAddOnPlateKgSnapshot = machine?.addOnPlateKg,
            machineMassMappingCsvSnapshot = machine?.massMappingCsv.orEmpty(),
        )
        if (machineChanged) {
            // Planned rows remain mutable configuration until the first completed Set.
            sets.filter { it.deletedAtMillis == null }.forEach { set ->
                dao.updateWorkoutSet(set.retargetForMachineChange(updated, machine, exercise, now))
            }
        }
        dao.updateWorkoutExercise(updated)
        return true
    }

    override suspend fun substituteWorkoutExercise(
        boundary: WorkoutPlacementMutationBoundary,
        exerciseId: Long,
        machineId: Long?,
        requestedWorkoutExerciseUuid: String,
        requestedInitialSetUuid: String,
    ): Long =
        database.withTransaction {
            require(requestedWorkoutExerciseUuid.isNotBlank() && requestedInitialSetUuid.isNotBlank()) {
                "Substitution request identity is missing"
            }
            dao.getWorkoutExerciseByUuid(requestedWorkoutExerciseUuid)?.let { existing ->
                val original = requireNotNull(dao.getWorkoutExercise(boundary.workoutExerciseId)) {
                    "Original exercise no longer exists"
                }
                val initialSet = requireNotNull(dao.getWorkoutSetByUuid(requestedInitialSetUuid)) {
                    "The previous substitution request is incomplete"
                }
                require(
                    original.uuid == boundary.workoutExerciseUuid &&
                        original.sessionId == boundary.structure.sessionId &&
                        original.outcome == WorkoutExerciseOutcome.Substituted.name &&
                        original.replacementWorkoutExerciseUuid == existing.uuid &&
                        existing.sessionId == original.sessionId &&
                        existing.exerciseId == exerciseId &&
                        existing.machineId == machineId &&
                        existing.outcome == WorkoutExerciseOutcome.Active.name &&
                        initialSet.workoutExerciseId == existing.id,
                ) { "This substitution request conflicts with a newer workout change" }
                return@withTransaction existing.id
            }
            val (current, session) = requireExactActivePlacement(boundary)
            require(exerciseId != current.exerciseId || machineId != current.machineId) { "Choose a different exercise or machine" }
            val now = nowMillis()
            invalidateRequiredMainWorkIfNeeded(current, session, now)
            val (newId, _) = insertWorkoutExerciseToActiveSession(
                current.sessionId,
                exerciseId,
                machineId,
                now,
                workoutExerciseUuid = requestedWorkoutExerciseUuid,
            )
            val oldName = dao.getExercise(current.exerciseId)?.name.orEmpty()
            val replacement = requireNotNull(dao.getWorkoutExercise(newId))
            dao.updateWorkoutExercise(
                replacement.copy(
                    notes = replacement.notes.ifBlank { "Substitution for $oldName" },
                    groupId = current.groupId,
                    position = current.position,
                    updatedAtMillis = now,
                ),
            )
            dao.getWorkoutSets(current.id).filter { !it.completed && it.deletedAtMillis == null }.forEach { set ->
                dao.updateWorkoutSet(
                    set.copy(
                        deletedAtMillis = now,
                        removalReason = WorkoutSetRemovalReason.ExerciseSubstituted.name,
                        updatedAtMillis = now,
                    ),
                )
            }
            dao.updateWorkoutExercise(
                current.copy(
                    groupId = null,
                    outcome = WorkoutExerciseOutcome.Substituted.name,
                    outcomeAtMillis = now,
                    replacementWorkoutExerciseUuid = replacement.uuid,
                    updatedAtMillis = now,
                ),
            )
            // A replacement is immediately loggable. This is workout-only work; it never
            // inherits the retired lift's prescribed Main/Supplemental identity.
            insertSetForActivePlacement(
                requireNotNull(dao.getWorkoutExercise(newId)),
                session,
                draft = null,
                now = now,
                requestedSetUuid = requestedInitialSetUuid,
            )
            val orderedIds = dao.getWorkoutExercises(current.sessionId)
                .filter { it.outcome == WorkoutExerciseOutcome.Active.name }
                .sortedBy { it.position }
                .map { it.id }
                .filterNot { it == newId }
                .toMutableList()
                .also { ids -> ids.add(current.position.coerceIn(0, ids.size), newId) }
            orderedIds.forEachIndexed { index, placementId ->
                dao.getWorkoutExercise(placementId)?.let { placement ->
                    dao.updateWorkoutExercise(placement.copy(position = index, updatedAtMillis = now))
                }
            }
            normalizeActiveWorkoutStructureInSession(current.sessionId, now)
            bumpWorkoutRevision(session.id, now)
            newId
        }

    override suspend fun removeWorkoutExercise(
        boundary: WorkoutPlacementMutationBoundary,
    ): WorkoutStructureMutationReceipt = database.withTransaction {
        val (current, session) = requireExactActivePlacement(boundary)
        val previousLayout = currentWorkoutLayout(session)
        val now = nowMillis()
        invalidateRequiredMainWorkIfNeeded(current, session, now)
        dao.getWorkoutSets(current.id).filter { !it.completed && it.deletedAtMillis == null }.forEach { set ->
            dao.updateWorkoutSet(
                set.copy(
                    deletedAtMillis = now,
                    removalReason = WorkoutSetRemovalReason.ExerciseRemoved.name,
                    updatedAtMillis = now,
                ),
            )
        }
        dao.updateWorkoutExercise(
            current.copy(
                groupId = null,
                outcome = WorkoutExerciseOutcome.Removed.name,
                outcomeAtMillis = now,
                replacementWorkoutExerciseUuid = null,
                updatedAtMillis = now,
            ),
        )
        normalizeActiveWorkoutStructureInSession(current.sessionId, now)
        bumpWorkoutRevision(session.id, now)
        structureReceipt(session.id, boundary.structure.fingerprint, true, previousLayout, current.uuid)
    }

    private suspend fun invalidateRequiredMainWorkIfNeeded(
        workoutExercise: WorkoutExerciseEntity,
        session: WorkoutSessionEntity,
        now: Long,
    ) {
        val removedRequiredMainWork = dao.getWorkoutSets(workoutExercise.id).any { set ->
            if (set.workSectionSnapshot != RoutineWorkSection.Main.name) return@any false
            val repetitionsMet = set.prescribedRepetitions == null ||
                (set.repetitions ?: Int.MIN_VALUE) >= set.prescribedRepetitions
            val loadMet = set.prescribedCanonicalWeightKg == null ||
                (set.canonicalWeightKg ?: Double.NEGATIVE_INFINITY) + 1e-9 >= set.prescribedCanonicalWeightKg
            set.deletedAtMillis != null || !set.completed ||
                set.classification == WorkoutSetClassification.Failure.name || !repetitionsMet || !loadMet
        }
        if (removedRequiredMainWork) {
            val invalidatedIds = session.invalidatedMainExerciseIdsCsv
                .split(',').mapNotNull(String::toLongOrNull).toMutableSet()
                .also { it += workoutExercise.exerciseId }
            check(
                dao.invalidateActiveSessionMainWork(
                    id = session.id,
                    invalidatedExerciseIdsCsv = invalidatedIds.sorted().joinToString(","),
                    updatedAtMillis = now,
                ) == 1,
            ) { "Workout changed before the main-work decision could be recorded" }
        }
    }

    override suspend fun removeWorkoutExerciseFromGroup(
        boundary: WorkoutPlacementMutationBoundary,
    ): WorkoutStructureMutationReceipt = database.withTransaction {
        val session = requireExactStructure(boundary.structure)
        val current = requireNotNull(dao.getWorkoutExercise(boundary.workoutExerciseId)) {
            "Exercise is no longer in this workout"
        }
        require(
            current.uuid == boundary.workoutExerciseUuid &&
                current.sessionId == session.id &&
                current.outcome == WorkoutExerciseOutcome.Active.name,
        ) { "This workout exercise changed; review the latest workout and try again" }
        val currentGroup = current.groupId?.let { groupId ->
            dao.getWorkoutGroups(session.id).firstOrNull { it.id == groupId }
        }
        require(current.updatedAtMillis == boundary.workoutExerciseUpdatedAtMillis) {
            "This workout exercise changed; review the latest workout and try again"
        }
        require(currentGroup?.uuid == boundary.expectedGroupUuid && currentGroup != null) {
            "This exercise is no longer in the group you reviewed"
        }
        val previousLayout = currentWorkoutLayout(session)
        val now = nowMillis()
        dao.updateWorkoutExercise(current.copy(groupId = null, updatedAtMillis = now))
        normalizeActiveWorkoutStructureInSession(current.sessionId, now)
        bumpWorkoutRevision(session.id, now)
        structureReceipt(session.id, boundary.structure.fingerprint, true, previousLayout, current.uuid)
    }

    override suspend fun applyWorkoutArrangement(
        boundary: WorkoutStructureBoundary,
        draft: WorkoutArrangementDraft,
    ): WorkoutStructureMutationReceipt = database.withTransaction {
        val session = requireExactStructure(boundary)
        val previousLayout = currentWorkoutLayout(session)
        val allPlacements = dao.getWorkoutExercises(session.id)
            .sortedWith(compareBy(WorkoutExerciseEntity::position, WorkoutExerciseEntity::id))
        val activePlacements = allPlacements.filter { it.outcome == WorkoutExerciseOutcome.Active.name }
        require(
            draft.activeWorkoutExerciseUuidsInOrder.distinct().size == draft.activeWorkoutExerciseUuidsInOrder.size &&
                draft.activeWorkoutExerciseUuidsInOrder.toSet() == activePlacements.map { it.uuid }.toSet(),
        ) { "Exercise order is stale; review the workout and try again" }
        val requestedActive = draft.activeWorkoutExerciseUuidsInOrder.map { uuid ->
            requireNotNull(activePlacements.firstOrNull { it.uuid == uuid }) {
                "Exercise order is stale; review the workout and try again"
            }
        }
        val groupIds = activePlacements.mapNotNull(WorkoutExerciseEntity::groupId).distinct()
        groupIds.forEach { groupId ->
            val indexes = requestedActive.mapIndexedNotNull { index, placement -> index.takeIf { placement.groupId == groupId } }
            require(indexes.isEmpty() || indexes.last() - indexes.first() + 1 == indexes.size) {
                "Grouped exercises must stay together"
            }
        }

        val setOrderByPlacementUuid = draft.setOrders.associateBy(WorkoutSetOrderDraft::workoutExerciseUuid)
        require(setOrderByPlacementUuid.size == draft.setOrders.size) { "Each exercise can have only one Set order" }
        require(setOrderByPlacementUuid.keys == activePlacements.map { it.uuid }.toSet()) {
            "Set order is stale; review the workout and try again"
        }
        val now = nowMillis()
        var changed = false
        val activeIterator = requestedActive.iterator()
        allPlacements.forEachIndexed { index, placement ->
            val target = if (placement.outcome == WorkoutExerciseOutcome.Active.name) activeIterator.next() else placement
            if (target.position != index) {
                dao.updateWorkoutExercise(target.copy(position = index, updatedAtMillis = now))
                changed = true
            }
        }
        activePlacements.forEach { placement ->
            val currentSets = dao.getWorkoutSets(placement.id)
                .sortedWith(compareBy(WorkoutSetEntity::position, WorkoutSetEntity::id))
            val requestedUuids = requireNotNull(setOrderByPlacementUuid[placement.uuid]).setUuidsInOrder
            require(requestedUuids.distinct().size == requestedUuids.size && requestedUuids.toSet() == currentSets.map { it.uuid }.toSet()) {
                "Set order is stale; review the workout and try again"
            }
            currentSets.forEachIndexed { index, set ->
                if (set.deletedAtMillis != null) {
                    require(requestedUuids[index] == set.uuid) {
                        "Removed Set history must stay in its original slot"
                    }
                }
            }
            requestedUuids.forEachIndexed { index, uuid ->
                val set = requireNotNull(currentSets.firstOrNull { it.uuid == uuid })
                if (set.position != index) {
                    dao.updateWorkoutSet(set.copy(position = index, updatedAtMillis = now))
                    changed = true
                }
            }
        }
        if (normalizeActiveWorkoutStructureInSession(session.id, now)) changed = true
        if (changed) bumpWorkoutRevision(session.id, now)
        structureReceipt(session.id, boundary.fingerprint, changed, previousLayout.takeIf { changed })
    }

    override suspend fun restoreWorkoutLayout(
        boundary: WorkoutStructureBoundary,
        snapshot: WorkoutLayoutSnapshot,
    ): WorkoutStructureMutationReceipt = database.withTransaction {
        val session = requireExactStructure(boundary)
        val currentLayout = currentWorkoutLayout(session)
        if (currentLayout == snapshot) {
            return@withTransaction structureReceipt(session.id, boundary.fingerprint, false)
        }
        val placements = dao.getWorkoutExercises(session.id)
        require(snapshot.allWorkoutExerciseUuidsInOrder.toSet() == placements.map { it.uuid }.toSet()) {
            "Undo is no longer safe because exercises were added or removed"
        }
        require(snapshot.allWorkoutExerciseUuidsInOrder.distinct().size == placements.size) {
            "Undo layout is malformed"
        }
        val now = nowMillis()
        placements.filter { it.groupId != null }.forEach { placement ->
            dao.updateWorkoutExercise(placement.copy(groupId = null, updatedAtMillis = now))
        }
        val currentGroups = dao.getWorkoutGroups(session.id)
        val targetGroupUuids = snapshot.groups.mapTo(mutableSetOf(), WorkoutGroupLayoutSnapshot::uuid)
        currentGroups.filter { it.uuid !in targetGroupUuids }.forEach { dao.deleteWorkoutGroup(it.id) }
        val groupIdByUuid = mutableMapOf<String, Long>()
        snapshot.groups.forEach { target ->
            val existing = currentGroups.firstOrNull { it.uuid == target.uuid }
            val entity = WorkoutGroupEntity(
                id = existing?.id ?: 0,
                uuid = target.uuid,
                sessionId = session.id,
                name = target.name,
                type = target.type.name,
                position = target.position,
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now,
            )
            val id = if (existing == null) dao.insertWorkoutGroup(entity) else {
                dao.updateWorkoutGroup(entity)
                existing.id
            }
            groupIdByUuid[target.uuid] = id
        }
        val placementByUuid = placements.associateBy(WorkoutExerciseEntity::uuid)
        snapshot.allWorkoutExerciseUuidsInOrder.forEachIndexed { index, uuid ->
            val placement = requireNotNull(placementByUuid[uuid])
            val targetGroupUuid = snapshot.groupUuidByWorkoutExerciseUuid[uuid]
            val targetGroupId = targetGroupUuid?.let { requireNotNull(groupIdByUuid[it]) { "Undo group is missing" } }
            dao.updateWorkoutExercise(
                placement.copy(position = index, groupId = targetGroupId, updatedAtMillis = now),
            )
        }
        val setOrderByPlacement = snapshot.setOrders.associateBy(WorkoutSetOrderDraft::workoutExerciseUuid)
        placements.forEach { placement ->
            val currentSets = dao.getWorkoutSets(placement.id)
            val targetOrder = requireNotNull(setOrderByPlacement[placement.uuid]) {
                "Undo is no longer safe because Set structure changed"
            }.setUuidsInOrder
            require(targetOrder.toSet() == currentSets.map { it.uuid }.toSet() && targetOrder.distinct().size == currentSets.size) {
                "Undo is no longer safe because Sets were added or removed"
            }
            targetOrder.forEachIndexed { index, uuid ->
                val set = requireNotNull(currentSets.firstOrNull { it.uuid == uuid })
                if (set.position != index) dao.updateWorkoutSet(set.copy(position = index, updatedAtMillis = now))
            }
        }
        normalizeActiveWorkoutStructureInSession(session.id, now)
        bumpWorkoutRevision(session.id, now)
        structureReceipt(session.id, boundary.fingerprint, true, currentLayout)
    }

    override suspend fun normalizeActiveWorkoutStructure(sessionId: Long): WorkoutStructureMutationReceipt = database.withTransaction {
        val session = requireNotNull(dao.getSession(sessionId)) { "Workout no longer exists" }
        require(session.state == WorkoutSessionState.Active.name) { "This workout is no longer active" }
        val before = currentWorkoutBoundary(session)
        val previousLayout = currentWorkoutLayout(session)
        val now = nowMillis()
        val changed = normalizeActiveWorkoutStructureInSession(sessionId, now)
        if (changed) bumpWorkoutRevision(sessionId, now)
        structureReceipt(sessionId, before.fingerprint, changed, previousLayout.takeIf { changed })
    }

    override suspend fun createGroup(
        boundary: WorkoutStructureBoundary,
        requestedGroupUuid: String,
        name: String,
        type: WorkoutGroupType,
        workoutExerciseUuids: List<String>,
    ): WorkoutStructureMutationReceipt = database.withTransaction {
        require(requestedGroupUuid.isNotBlank()) { "Group identity is missing" }
        val requestedName = name.trim()
        val normalizedName = requestedName
            .takeIf { candidate ->
                candidate.isNotBlank() && WorkoutGroupType.entries.none { candidate.equals(it.name, ignoreCase = true) }
            }
            ?: type.name
        val boundarySession = requireNotNull(dao.getSession(boundary.sessionId)) { "Workout no longer exists" }
        require(boundarySession.uuid == boundary.sessionUuid && boundarySession.state == WorkoutSessionState.Active.name) {
            "This workout is no longer the active workout"
        }
        dao.getWorkoutGroups(boundary.sessionId).firstOrNull { it.uuid == requestedGroupUuid }?.let { existing ->
            val memberUuids = dao.getWorkoutExercises(boundary.sessionId)
                .filter { it.groupId == existing.id }.map { it.uuid }.toSet()
            require(existing.type == type.name && existing.name == normalizedName && memberUuids == workoutExerciseUuids.toSet()) {
                "This group request conflicts with a newer workout change"
            }
            return@withTransaction structureReceipt(boundary.sessionId, boundary.fingerprint, false, targetUuid = existing.uuid)
        }
        val session = requireExactStructure(boundary)
        require(workoutExerciseUuids.size >= 2) { "A group needs at least two exercises" }
        require(workoutExerciseUuids.distinct().size == workoutExerciseUuids.size) {
            "Choose each exercise only once"
        }
        val previousLayout = currentWorkoutLayout(session)
        val now = nowMillis()
        val ordered = dao.getWorkoutExercises(session.id)
            .filter { it.outcome == WorkoutExerciseOutcome.Active.name }
            .sortedWith(compareBy({ it.position }, { it.id }))
        val requestedUuids = workoutExerciseUuids.toSet()
        val requestedMembers = ordered.filter { it.uuid in requestedUuids }
        require(requestedMembers.size == requestedUuids.size) { "Exercise is not in this workout" }
        val groupId = dao.insertWorkoutGroup(
            WorkoutGroupEntity(
                uuid = requestedGroupUuid,
                sessionId = session.id,
                name = normalizedName,
                type = type.name,
                position = dao.getWorkoutGroups(session.id).size,
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
        )
        ordered.forEach { item ->
            if (item.uuid in requestedUuids && item.groupId != groupId) {
                dao.updateWorkoutExercise(
                    item.copy(groupId = groupId, updatedAtMillis = now),
                )
            }
        }
        normalizeActiveWorkoutStructureInSession(session.id, now)
        bumpWorkoutRevision(session.id, now)
        structureReceipt(session.id, boundary.fingerprint, true, previousLayout, requestedGroupUuid)
    }

    private suspend fun normalizeActiveWorkoutStructureInSession(sessionId: Long, now: Long): Boolean {
        var changed = false
        val allPlacements = dao.getWorkoutExercises(sessionId)
            .sortedWith(compareBy(WorkoutExerciseEntity::position, WorkoutExerciseEntity::id))
        var ordered = allPlacements
            .filter { it.outcome == WorkoutExerciseOutcome.Active.name }
            .sortedWith(compareBy({ it.position }, { it.id }))
        val groups = dao.getWorkoutGroups(sessionId)
        val knownGroupIds = groups.mapTo(mutableSetOf(), WorkoutGroupEntity::id)

        // Clear dangling or singleton membership and delete empty/singleton group records.
        ordered.filter { it.groupId != null && it.groupId !in knownGroupIds }.forEach { member ->
            dao.updateWorkoutExercise(member.copy(groupId = null, updatedAtMillis = now))
            changed = true
        }
        groups.forEach { group ->
            val members = ordered.filter { it.groupId == group.id }
            if (members.size < 2) {
                members.forEach { member ->
                    dao.updateWorkoutExercise(member.copy(groupId = null, updatedAtMillis = now))
                    changed = true
                }
                dao.deleteWorkoutGroup(group.id)
                changed = true
            }
        }

        // Flatten every surviving group at its first authored position. This repairs
        // legacy sessions and prevents regrouping one member from splitting its old group.
        ordered = dao.getWorkoutExercises(sessionId)
            .filter { it.outcome == WorkoutExerciseOutcome.Active.name }
            .sortedWith(compareBy({ it.position }, { it.id }))
        val emittedGroups = mutableSetOf<Long>()
        val normalized = buildList {
            ordered.forEach { item ->
                val groupId = item.groupId
                if (groupId == null) add(item)
                else if (emittedGroups.add(groupId)) addAll(ordered.filter { it.groupId == groupId })
            }
        }
        val activeIterator = normalized.iterator()
        allPlacements.forEachIndexed { index, slot ->
            val item = if (slot.outcome == WorkoutExerciseOutcome.Active.name) activeIterator.next() else slot
            if (item.position != index) {
                dao.updateWorkoutExercise(item.copy(position = index, updatedAtMillis = now))
                changed = true
            }
        }
        dao.getWorkoutExercises(sessionId).forEach { placement ->
            dao.getWorkoutSets(placement.id)
                .sortedWith(compareBy(WorkoutSetEntity::position, WorkoutSetEntity::id))
                .forEachIndexed { index, set ->
                    if (set.position != index) {
                        dao.updateWorkoutSet(set.copy(position = index, updatedAtMillis = now))
                        changed = true
                    }
                }
        }
        val normalizedPlacements = dao.getWorkoutExercises(sessionId)
        val firstMemberPositionByGroup = normalizedPlacements
            .filter { it.groupId != null }
            .groupBy { requireNotNull(it.groupId) }
            .mapValues { (_, members) -> members.minOf(WorkoutExerciseEntity::position) }
        dao.getWorkoutGroups(sessionId)
            .sortedWith(compareBy<WorkoutGroupEntity> { group ->
                firstMemberPositionByGroup[group.id] ?: Int.MAX_VALUE
            }.thenBy(WorkoutGroupEntity::id))
            .forEachIndexed { index, group ->
                if (group.position != index) {
                    dao.updateWorkoutGroup(group.copy(position = index, updatedAtMillis = now))
                    changed = true
                }
            }
        return changed
    }

    override suspend fun addSet(boundary: WorkoutPlacementMutationBoundary, draft: WorkoutSetDraft?): Long =
        database.withTransaction {
            val (workoutExercise, session) = requireExactActivePlacement(boundary)
            val now = nowMillis()
            val newId = insertSetForActivePlacement(workoutExercise, session, draft, now)
            bumpWorkoutRevision(session.id, now)
            newId
        }

    private suspend fun insertSetForActivePlacement(
        workoutExercise: WorkoutExerciseEntity,
        session: WorkoutSessionEntity,
        draft: WorkoutSetDraft?,
        now: Long,
        requestedSetUuid: String? = null,
    ): Long {
        val previous = dao.getWorkoutSets(workoutExercise.id)
            .lastOrNull { it.deletedAtMillis == null }
            ?: dao.getLatestCompletedSet(
                workoutExercise.exerciseId,
                workoutExercise.sessionId,
                workoutExercise.machineProfileUuidSnapshot,
            )
        val effectiveDraft = draft ?: previous?.toDraft()?.copy(
            planned = false,
            completed = false,
            classification = WorkoutSetClassification.Working,
            workSection = if (session.sourceRoutineProgramKind != RoutineProgramKind.Static.name) {
                RoutineWorkSection.Optional
            } else {
                RoutineWorkSection.Unspecified
            },
            optionalWorkKind = RoutineOptionalWorkKind.None,
            mainWorkScheme = null,
            supplementalScheme = null,
        ) ?: WorkoutSetDraft()
        val exercise = dao.getExercise(workoutExercise.exerciseId)?.toDomain()
            ?: error("Exercise no longer exists")
        val policyExercise = workoutExercise.toDomain().applyPolicySnapshot(exercise)
        validateWorkoutSetDraft(
            effectiveDraft,
            policyExercise.trackingType,
            workoutExercise.machineLoadTypeSnapshot.takeIf(String::isNotBlank)?.let(MachineLoadType::valueOf),
            runCatching { LoadInterpretation.valueOf(workoutExercise.loadInterpretationSnapshot) }
                .getOrDefault(LoadInterpretation.Total),
        )
        return dao.insertWorkoutSet(
            effectiveDraft.toEntity(
                uuid = requestedSetUuid ?: ids.nextId(),
                workoutExerciseId = workoutExercise.id,
                position = dao.nextSetPosition(workoutExercise.id),
                createdAtMillis = now,
                completedAtMillis = now.takeIf { effectiveDraft.completed },
                workoutExercise = workoutExercise,
            ).copy(requiredForProgressionSnapshot = false, removalReason = null),
        )
    }

    override suspend fun saveQuickSet(
        id: Long,
        expectedSetUuid: String,
        expectedSetUpdatedAtMillis: Long,
        expectedWorkoutRevision: Long?,
        draft: WorkoutSetDraft,
        addNext: Boolean,
        autoStartRest: Boolean,
        restOverrideSeconds: Int?,
    ): QuickSetCommitReceipt = database.withTransaction {
        val existing = requireNotNull(dao.getWorkoutSet(id)) { "Set no longer exists" }
        require(existing.uuid == expectedSetUuid && existing.updatedAtMillis == expectedSetUpdatedAtMillis) {
            "This set changed before it could be saved; review the latest values and try again"
        }
        require(existing.deletedAtMillis == null) { "This set has already been removed" }
        val (workoutExercise, session) = requireActivePlacement(existing.workoutExerciseId)
        require(expectedWorkoutRevision == null || session.workoutRevision == expectedWorkoutRevision) {
            "The workout changed before this quick save. Review the latest sets and try again."
        }
        val exercise = requireNotNull(dao.getExercise(workoutExercise.exerciseId)) { "Exercise no longer exists" }
        val policyExercise = workoutExercise.toDomain().applyPolicySnapshot(exercise.toDomain())
        val completedDraft = draft.copy(completed = true, planned = false)
        validateWorkoutSetDraft(
            completedDraft,
            policyExercise.trackingType,
            workoutExercise.machineLoadTypeSnapshot.takeIf(String::isNotBlank)?.let(MachineLoadType::valueOf),
            runCatching { LoadInterpretation.valueOf(workoutExercise.loadInterpretationSnapshot) }
                .getOrDefault(LoadInterpretation.Total),
        )
        val now = nowMillis()
        val completed = completedDraft.toEntity(
            id = existing.id,
            uuid = existing.uuid,
            workoutExerciseId = existing.workoutExerciseId,
            position = existing.position,
            deletedAtMillis = null,
            completedAtMillis = existing.completedAtMillis ?: now,
            createdAtMillis = existing.createdAtMillis,
            updatedAtMillis = now,
            workoutExercise = workoutExercise,
        ).copy(
            prescribedCanonicalWeightKg = existing.prescribedCanonicalWeightKg,
            prescribedEnteredWeight = existing.prescribedEnteredWeight,
            prescribedWeightUnitId = existing.prescribedWeightUnitId,
            prescribedRepetitions = existing.prescribedRepetitions,
            prescribedRepetitionsMax = existing.prescribedRepetitionsMax,
            prescribedRpe = existing.prescribedRpe,
            prescribedRir = existing.prescribedRir,
            prescribedDurationSeconds = existing.prescribedDurationSeconds,
            prescribedMachineLoadValue = existing.prescribedMachineLoadValue,
            prescriptionSourceLabel = existing.prescriptionSourceLabel,
            workSectionSnapshot = existing.workSectionSnapshot,
            optionalWorkKindSnapshot = existing.optionalWorkKindSnapshot,
            prescribedClassificationSnapshot = existing.prescribedClassificationSnapshot,
            requiredForProgressionSnapshot = existing.requiredForProgressionSnapshot,
            removalReason = null,
        )
        dao.updateWorkoutSet(completed)

        val activePlacementIds = dao.getWorkoutExercises(session.id)
            .filter { it.outcome == WorkoutExerciseOutcome.Active.name }
            .mapTo(mutableSetOf(), WorkoutExerciseEntity::id)
        val hasAnotherIncompleteSet = activePlacementIds.any { placementId ->
            dao.getWorkoutSets(placementId).any { set ->
                set.id != id && !set.completed && set.deletedAtMillis == null
            }
        }
        val appendedSetId = if (addNext && !hasAnotherIncompleteSet) {
            dao.insertWorkoutSet(
                completed.asWorkoutOnlyCopy(
                    id = 0,
                    uuid = ids.nextId(),
                    workoutExerciseId = workoutExercise.id,
                    position = dao.nextSetPosition(workoutExercise.id),
                    createdAtMillis = now,
                    updatedAtMillis = now,
                    programmed = session.sourceRoutineProgramKind != RoutineProgramKind.Static.name,
                    planned = false,
                ).copy(note = "", rpe = null, rir = null),
            )
        } else {
            null
        }

        val restSeconds = if (autoStartRest) {
            restOverrideSeconds ?: completed.restSeconds ?: exercise.defaultRestSeconds
                ?: settingsRepository?.current()?.defaultRestSeconds ?: 120
        } else {
            null
        }?.takeIf { it > 0 }
        if (restSeconds != null) {
            check(
                dao.updateActiveSessionTimer(
                    session.id,
                    now + restSeconds * 1_000L,
                    restSeconds,
                    now,
                ) == 1,
            ) { "Workout changed before the timer could start" }
        }
        bumpWorkoutRevision(session.id, now)
        QuickSetCommitReceipt(
            sessionId = session.id,
            sessionUuid = session.uuid,
            exerciseId = workoutExercise.exerciseId,
            setId = completed.id,
            setUuid = completed.uuid,
            appendedSetId = appendedSetId,
            restTimerSeconds = restSeconds,
        )
    }

    override suspend fun updateSet(
        boundary: WorkoutSetMutationBoundary,
        draft: WorkoutSetDraft,
    ) = database.withTransaction {
        val (existing, workoutExercise, session) = requireExactSetMutation(boundary, requireActive = false)
        require(existing.deletedAtMillis == null) { "This Set has already been removed" }
        val exercise = dao.getExercise(workoutExercise.exerciseId)?.toDomain()
            ?: error("Exercise no longer exists")
        val policyExercise = workoutExercise.toDomain().applyPolicySnapshot(exercise)
        validateWorkoutSetDraft(
            draft,
            policyExercise.trackingType,
            workoutExercise.machineLoadTypeSnapshot.takeIf(String::isNotBlank)?.let(MachineLoadType::valueOf),
            runCatching { LoadInterpretation.valueOf(workoutExercise.loadInterpretationSnapshot) }
                .getOrDefault(LoadInterpretation.Total),
        )
        val now = nowMillis()
        val updated = draft.toEntity(
                id = existing.id,
                uuid = existing.uuid,
                workoutExerciseId = existing.workoutExerciseId,
                position = existing.position,
                deletedAtMillis = existing.deletedAtMillis,
                createdAtMillis = existing.createdAtMillis,
                updatedAtMillis = now,
                completedAtMillis = when {
                    draft.completed && existing.completedAtMillis == null -> now
                    draft.completed -> existing.completedAtMillis
                    else -> null
                },
                workoutExercise = workoutExercise,
            ).copy(
                prescribedCanonicalWeightKg = existing.prescribedCanonicalWeightKg,
                prescribedEnteredWeight = existing.prescribedEnteredWeight,
                prescribedWeightUnitId = existing.prescribedWeightUnitId,
                prescribedRepetitions = existing.prescribedRepetitions,
                prescribedRepetitionsMax = existing.prescribedRepetitionsMax,
                prescribedRpe = existing.prescribedRpe,
                prescribedRir = existing.prescribedRir,
                prescribedDurationSeconds = existing.prescribedDurationSeconds,
                prescribedMachineLoadValue = existing.prescribedMachineLoadValue,
                prescriptionSourceLabel = existing.prescriptionSourceLabel,
                workSectionSnapshot = existing.workSectionSnapshot,
                optionalWorkKindSnapshot = existing.optionalWorkKindSnapshot,
                prescribedClassificationSnapshot = existing.prescribedClassificationSnapshot,
                requiredForProgressionSnapshot = existing.requiredForProgressionSnapshot,
                removalReason = existing.removalReason,
            )
        if (updated.copy(updatedAtMillis = existing.updatedAtMillis) == existing) return@withTransaction
        dao.updateWorkoutSet(updated)
        if (session.state == WorkoutSessionState.Active.name) bumpWorkoutRevision(session.id, now)
    }

    override suspend fun setSetCompleted(
        boundary: WorkoutSetMutationBoundary,
        completed: Boolean,
        autoStartRest: Boolean,
        restOverrideSeconds: Int?,
    ) =
        database.withTransaction {
            val (set, workoutExercise, session) = requireExactSetMutation(boundary)
            require(set.deletedAtMillis == null) { "This set has already been removed" }
            if (set.completed == completed) return@withTransaction
            if (completed) {
                val exercise = dao.getExercise(workoutExercise.exerciseId)?.toDomain()
                    ?: error("Exercise no longer exists")
                val policyExercise = workoutExercise.toDomain().applyPolicySnapshot(exercise)
                validateWorkoutSetDraft(
                    set.toDraft().copy(completed = true),
                    policyExercise.trackingType,
                    workoutExercise.machineLoadTypeSnapshot.takeIf(String::isNotBlank)?.let(MachineLoadType::valueOf),
                    runCatching { LoadInterpretation.valueOf(workoutExercise.loadInterpretationSnapshot) }
                        .getOrDefault(LoadInterpretation.Total),
                )
            }
            val now = nowMillis()
            dao.updateWorkoutSet(
                set.copy(
                    completed = completed,
                    planned = false,
                    completedAtMillis = if (completed) set.completedAtMillis ?: now else null,
                    updatedAtMillis = now,
                ),
            )
            if (completed && autoStartRest) {
                val exercise = dao.getExercise(workoutExercise.exerciseId)
                val seconds = restOverrideSeconds ?: set.restSeconds ?: exercise?.defaultRestSeconds
                    ?: settingsRepository?.current()?.defaultRestSeconds ?: 120
                if (seconds > 0) {
                    check(
                        dao.updateActiveSessionTimer(
                            session.id,
                            now + seconds * 1_000L,
                            seconds,
                            now,
                        ) == 1,
                    ) { "Workout changed before the timer could start" }
                }
            }
            bumpWorkoutRevision(session.id, now)
        }

    override suspend fun duplicateSet(boundary: WorkoutSetMutationBoundary): Long = database.withTransaction {
        val (source, workoutExercise, session) = requireExactSetMutation(boundary)
        require(source.deletedAtMillis == null) { "Removed Sets cannot be duplicated" }
        val now = nowMillis()
        val newId = dao.insertWorkoutSet(
            source.asWorkoutOnlyCopy(
                id = 0,
                uuid = ids.nextId(),
                workoutExerciseId = source.workoutExerciseId,
                position = dao.nextSetPosition(source.workoutExerciseId),
                createdAtMillis = now,
                updatedAtMillis = now,
                programmed = session.sourceRoutineProgramKind != RoutineProgramKind.Static.name,
                planned = false,
            ),
        )
        bumpWorkoutRevision(workoutExercise.sessionId, now)
        newId
    }

    override suspend fun deleteSet(
        boundary: WorkoutSetMutationBoundary,
        reason: WorkoutSetRemovalReason,
    ): WorkoutStructureMutationReceipt = database.withTransaction {
        val (set, _, session) = requireExactSetMutation(boundary)
        require(set.deletedAtMillis == null) { "This Set has already been removed; review its current status" }
        if (reason == WorkoutSetRemovalReason.Skipped) {
            require(!set.completed) { "A completed Set cannot be skipped" }
            require(
                set.workSectionSnapshot == RoutineWorkSection.Optional.name ||
                    set.optionalWorkKindSnapshot == RoutineOptionalWorkKind.Joker.name,
            ) { "Only optional work can be skipped" }
        }
        val before = currentWorkoutBoundary(session)
        val now = nowMillis()
        dao.updateWorkoutSet(set.copy(deletedAtMillis = now, removalReason = reason.name, updatedAtMillis = now))
        bumpWorkoutRevision(session.id, now)
        structureReceipt(session.id, before.fingerprint, true, targetUuid = set.uuid)
    }

    override suspend fun undoDeleteSet(
        boundary: WorkoutSetMutationBoundary,
    ): WorkoutStructureMutationReceipt = database.withTransaction {
        val (set, _, session) = requireExactSetMutation(boundary)
        require(set.deletedAtMillis != null && set.removalReason != null) {
            "This Set is no longer in the removed state you reviewed"
        }
        val before = currentWorkoutBoundary(session)
        val now = nowMillis()
        dao.updateWorkoutSet(set.copy(deletedAtMillis = null, removalReason = null, updatedAtMillis = now))
        bumpWorkoutRevision(session.id, now)
        structureReceipt(session.id, before.fingerprint, true, targetUuid = set.uuid)
    }

    override suspend fun startRestTimer(sessionId: Long, seconds: Int) = database.withTransaction {
        require(seconds > 0) { "Timer duration must be positive" }
        val session = requireNotNull(dao.getSession(sessionId)) { "Workout no longer exists" }
        require(session.state == WorkoutSessionState.Active.name) { "Rest timers require an active workout" }
        val now = nowMillis()
        check(dao.updateActiveSessionTimer(sessionId, now + seconds * 1_000L, seconds, now) == 1) {
            "Workout changed before the timer could start"
        }
    }

    override suspend fun adjustRestTimer(sessionId: Long, deltaSeconds: Int) = database.withTransaction {
        val session = requireNotNull(dao.getSession(sessionId)) { "Workout no longer exists" }
        require(session.state == WorkoutSessionState.Active.name) { "Rest timers require an active workout" }
        val now = nowMillis()
        val deadline = session.restTimerDeadlineMillis ?: now
        val adjusted = (deadline + deltaSeconds * 1_000L).coerceAtLeast(now)
        // Preserve the same ceiling semantics used by the countdown UI. Room may deliver an
        // adjustment between one-second UI ticks, so flooring the fractional remainder makes a
        // visible +15 seconds look like +14 (and -15 like -16).
        val adjustedDurationSeconds = ((adjusted - now + 999L) / 1_000L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
        check(
            dao.updateActiveSessionTimer(
                sessionId,
                adjusted.takeIf { it > now },
                adjustedDurationSeconds.takeIf { it > 0 },
                now,
            ) == 1,
        ) { "Workout changed before the timer could be adjusted" }
    }

    override suspend fun stopRestTimer(sessionId: Long) = database.withTransaction {
        val session = requireNotNull(dao.getSession(sessionId)) { "Workout no longer exists" }
        if (session.state != WorkoutSessionState.Active.name) {
            require(session.restTimerDeadlineMillis == null) { "Rest timers require an active workout" }
            return@withTransaction
        }
        check(dao.updateActiveSessionTimer(sessionId, null, null, nowMillis()) == 1) {
            "Workout changed before the timer could stop"
        }
    }

    override suspend fun acknowledgeRestTimerCleanup(sessionId: Long, expectedTimerRevision: Long) {
        dao.acknowledgeRestTimerCleanup(sessionId, expectedTimerRevision)
    }

    override suspend fun completeRestTimerDelivery(
        sessionId: Long,
        expectedTimerRevision: Long,
        expectedDeadlineMillis: Long,
    ): Boolean = dao.completeActiveRestTimerDelivery(
        id = sessionId,
        expectedTimerRevision = expectedTimerRevision,
        expectedDeadlineMillis = expectedDeadlineMillis,
        updatedAtMillis = nowMillis(),
    ) == 1

    private suspend fun currentWorkoutBoundary(session: WorkoutSessionEntity): WorkoutStructureBoundary {
        val placements = dao.getWorkoutExercises(session.id)
        val placementIds = placements.mapTo(mutableSetOf(), WorkoutExerciseEntity::id)
        return workoutStructureBoundary(
            session = session.toDomain(),
            workoutExercises = placements.map(WorkoutExerciseEntity::toDomain),
            groups = dao.getWorkoutGroups(session.id).map(WorkoutGroupEntity::toDomain),
            sets = placements.flatMap { placement -> dao.getWorkoutSets(placement.id) }
                .filter { it.workoutExerciseId in placementIds }
                .map(WorkoutSetEntity::toDomain),
        )
    }

    private suspend fun currentWorkoutLayout(session: WorkoutSessionEntity): WorkoutLayoutSnapshot {
        val placements = dao.getWorkoutExercises(session.id)
        return workoutLayoutSnapshot(
            session = session.toDomain(),
            workoutExercises = placements.map(WorkoutExerciseEntity::toDomain),
            groups = dao.getWorkoutGroups(session.id).map(WorkoutGroupEntity::toDomain),
            sets = placements.flatMap { placement -> dao.getWorkoutSets(placement.id) }
                .map(WorkoutSetEntity::toDomain),
        )
    }

    private suspend fun requireExactStructure(boundary: WorkoutStructureBoundary): WorkoutSessionEntity {
        val session = requireNotNull(dao.getSession(boundary.sessionId)) { "Workout no longer exists" }
        require(session.state == WorkoutSessionState.Active.name && session.uuid == boundary.sessionUuid) {
            "This workout is no longer the active workout"
        }
        require(currentWorkoutBoundary(session).fingerprint == boundary.fingerprint) {
            "The workout layout changed. Review the latest exercises and Sets before trying again."
        }
        return session
    }

    private suspend fun requireExactActivePlacement(
        boundary: WorkoutPlacementMutationBoundary,
    ): Pair<WorkoutExerciseEntity, WorkoutSessionEntity> {
        val session = requireExactStructure(boundary.structure)
        val placement = requireNotNull(dao.getWorkoutExercise(boundary.workoutExerciseId)) {
            "Workout exercise no longer exists"
        }
        val groupUuid = placement.groupId?.let { groupId ->
            dao.getWorkoutGroups(session.id).firstOrNull { it.id == groupId }?.uuid
        }
        require(
            placement.sessionId == session.id &&
                placement.uuid == boundary.workoutExerciseUuid &&
                placement.updatedAtMillis == boundary.workoutExerciseUpdatedAtMillis &&
                placement.outcome == WorkoutExerciseOutcome.Active.name &&
                groupUuid == boundary.expectedGroupUuid,
        ) { "This workout exercise changed. Review it before trying again." }
        return placement to session
    }

    private suspend fun requireExactSetMutation(
        boundary: WorkoutSetMutationBoundary,
        requireActive: Boolean = true,
    ): Triple<WorkoutSetEntity, WorkoutExerciseEntity, WorkoutSessionEntity> {
        val session = requireNotNull(dao.getSession(boundary.sessionId)) { "Workout no longer exists" }
        require(
            (!requireActive || session.state == WorkoutSessionState.Active.name) &&
                session.uuid == boundary.sessionUuid &&
                session.workoutRevision == boundary.workoutRevision,
        ) { "The workout changed. Review the latest Set before trying again." }
        val placement = requireNotNull(dao.getWorkoutExercise(boundary.workoutExerciseId)) {
            "Workout exercise no longer exists"
        }
        require(
            placement.sessionId == session.id &&
                placement.uuid == boundary.workoutExerciseUuid &&
                (!requireActive || placement.outcome == WorkoutExerciseOutcome.Active.name),
        ) { "This workout exercise changed. Review it before trying again." }
        val set = requireNotNull(dao.getWorkoutSet(boundary.setId)) { "Set no longer exists" }
        val removalReason = set.removalReason?.let(WorkoutSetRemovalReason::valueOf)
        require(
            set.workoutExerciseId == placement.id &&
                set.uuid == boundary.setUuid &&
                set.updatedAtMillis == boundary.setUpdatedAtMillis &&
                set.deletedAtMillis == boundary.expectedDeletedAtMillis &&
                removalReason == boundary.expectedRemovalReason,
        ) { "This Set changed. Review its latest values and status before trying again." }
        return Triple(set, placement, session)
    }

    private suspend fun structureReceipt(
        sessionId: Long,
        beforeFingerprint: String,
        changed: Boolean,
        previousLayout: WorkoutLayoutSnapshot? = null,
        targetUuid: String? = null,
    ): WorkoutStructureMutationReceipt {
        val session = requireNotNull(dao.getSession(sessionId)) { "Workout no longer exists" }
        return WorkoutStructureMutationReceipt(
            sessionId = session.id,
            sessionUuid = session.uuid,
            changed = changed,
            beforeFingerprint = beforeFingerprint,
            afterBoundary = currentWorkoutBoundary(session),
            previousLayout = previousLayout,
            targetUuid = targetUuid,
        )
    }

    private suspend fun requireActivePlacement(workoutExerciseId: Long): Pair<WorkoutExerciseEntity, WorkoutSessionEntity> {
        val placement = requireNotNull(dao.getWorkoutExercise(workoutExerciseId)) {
            "Workout exercise no longer exists"
        }
        require(placement.outcome == WorkoutExerciseOutcome.Active.name) {
            "Workout exercise is no longer active"
        }
        val session = requireNotNull(dao.getSession(placement.sessionId)) { "Workout no longer exists" }
        require(session.state == WorkoutSessionState.Active.name) {
            "This workout is no longer active"
        }
        return placement to session
    }

    private suspend fun bumpWorkoutRevision(sessionId: Long, now: Long = nowMillis()) {
        check(dao.bumpActiveWorkoutRevision(sessionId, now) == 1) {
            "The workout changed before this action could finish"
        }
    }

    private fun nowMillis() = clock.now().toEpochMilli()
}

private fun validateExercise(draft: ExerciseDraft) {
    require(draft.name.isNotBlank()) { "Exercise name is required" }
    require(draft.weightIncrement.isFinite() && draft.weightIncrement > 0.0) { "Weight increment must be positive" }
    require(draft.repetitionIncrement > 0) { "Repetition increment must be positive" }
    require(draft.defaultRestSeconds == null || draft.defaultRestSeconds in 1..86_400) {
        "Default rest must be between 1 second and 24 hours"
    }
    require(draft.effectiveBodyweightPercent in 0.0..200.0) {
        "Effective bodyweight percentage must be between 0 and 200"
    }
    require(draft.barWeightKg == null || draft.barWeightKg.isFinite() && draft.barWeightKg >= 0.0) {
        "Bar or base load must be a non-negative number"
    }
    require(draft.availablePlatesKg.all { it.isFinite() && it > 0.0 }) {
        "Available plates must contain positive numbers"
    }
}

private fun validateMachine(draft: GymMachineDraft) {
    require(draft.name.isNotBlank()) { "Machine name is required" }
    require(draft.availableLoads.all { it.isFinite() && it >= 0.0 }) { "Machine loads cannot be negative" }
    require(draft.baseLoadKg == null || draft.baseLoadKg.isFinite() && draft.baseLoadKg >= 0.0) {
        "Machine base resistance must be a non-negative number"
    }
    require(draft.pulleyRatio.isFinite() && draft.pulleyRatio > 0.0 && draft.pulleyRatio <= 10.0) {
        "Resistance multiplier must be greater than 0 and at most 10"
    }
    require(draft.addOnPlateKg == null || draft.addOnPlateKg.isFinite() && draft.addOnPlateKg >= 0.0) {
        "Add-on resistance must be non-negative"
    }
    require(draft.massMappingKg.all { (setting, kg) -> setting.isFinite() && setting >= 0.0 && kg.isFinite() && kg >= 0.0 }) {
        "Every setting-to-mass mapping must contain non-negative finite numbers"
    }
    if (draft.loadType == MachineLoadType.Mass) {
        require(BuiltInUnits.get(draft.unitId)?.dimension == UnitDimension.Mass) { "Machine mass unit must be kg or lb" }
    } else {
        require(draft.levelLabel.isNotBlank()) { "Numbered settings need a label" }
    }
}

private fun GymMachineDraft.toEntity(
    id: Long = 0,
    uuid: String,
    archived: Boolean = false,
    createdAtMillis: Long,
    updatedAtMillis: Long = createdAtMillis,
    primaryExerciseId: Long? = normalizedExerciseIds().firstOrNull(),
) = GymMachineEntity(
    id = id,
    uuid = uuid,
    exerciseId = primaryExerciseId,
    name = name.trim(),
    location = location.trim(),
    details = details.trim(),
    loadType = loadType.name,
    unitId = if (loadType == MachineLoadType.Mass) unitId else "",
    levelLabel = levelLabel.trim().ifBlank { "level" },
    availableLoadsCsv = availableLoads.distinct().sorted().joinToString(","),
    archived = archived,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    loadInterpretation = loadInterpretation.name,
    baseLoadKg = baseLoadKg,
    configurationGroupId = configurationGroupId,
    configurationVersion = configurationVersion.coerceAtLeast(1),
    seatPosition = seatPosition.trim(),
    backPosition = backPosition.trim(),
    attachment = attachment.trim(),
    pulleyRatio = pulleyRatio,
    stackMode = stackMode.name,
    addOnPlateKg = addOnPlateKg,
    stackLabelsCsv = stackLabels.map(String::trim).filter(String::isNotBlank).joinToString("\u001f"),
    massMappingCsv = massMappingKg.toStableMappingCsv(),
    compatibleForComparison = compatibleForComparison,
    levelDirection = levelDirection.name,
)

private fun GymMachineEntity.toDomain(exerciseIds: Set<Long>) = GymMachine(
    id = id,
    uuid = uuid,
    exerciseId = exerciseId,
    name = name,
    location = location,
    details = details,
    loadType = MachineLoadType.valueOf(loadType),
    unitId = unitId,
    levelLabel = levelLabel,
    availableLoads = availableLoadsCsv.split(',').mapNotNull(String::toDoubleOrNull),
    loadInterpretation = runCatching { LoadInterpretation.valueOf(loadInterpretation) }.getOrDefault(LoadInterpretation.Total),
    baseLoadKg = baseLoadKg,
    archived = archived,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    configurationGroupId = configurationGroupId.ifBlank { uuid },
    configurationVersion = configurationVersion,
    seatPosition = seatPosition,
    backPosition = backPosition,
    attachment = attachment,
    pulleyRatio = pulleyRatio,
    stackMode = runCatching { MachineStackMode.valueOf(stackMode) }.getOrDefault(MachineStackMode.Single),
    addOnPlateKg = addOnPlateKg,
    stackLabels = stackLabelsCsv.split('\u001f').filter(String::isNotBlank),
    massMappingKg = massMappingCsv.parseStableMappingCsv(),
    compatibleForComparison = compatibleForComparison,
    exerciseIds = exerciseIds,
    levelDirection = runCatching { MachineLevelDirection.valueOf(levelDirection) }
        .getOrDefault(MachineLevelDirection.HigherNumberMoreResistance),
)

private fun GymMachineDraft.normalizedExerciseIds(): Set<Long> =
    (exerciseIds + listOfNotNull(exerciseId)).filterTo(linkedSetOf()) { it > 0L }

internal fun GymMachineEntity.displayName() = if (location.isBlank()) name else "$name · $location"

internal fun GymMachineEntity.configurationSummary() = buildList {
    if (seatPosition.isNotBlank()) add("Seat $seatPosition")
    if (backPosition.isNotBlank()) add("Back $backPosition")
    if (attachment.isNotBlank()) add(attachment)
    if (pulleyRatio != 1.0) add("resistance ×$pulleyRatio")
    if (stackMode != MachineStackMode.Single.name) add(stackMode)
    if (stackLabelsCsv.isNotBlank()) add(stackLabelsCsv.split('\u001f').joinToString(" / "))
}.joinToString(" · ")

private fun WorkoutExerciseEntity.requireResolvedEquipmentForNewWorkout() {
    require(machineProfileUuidSnapshot == null || machineId != null) {
        "${machineNameSnapshot.ifBlank { "Deleted equipment" }} must be replaced before copying this workout into a new session"
    }
}

private fun ExerciseDraft.toEntity(
    id: Long = 0,
    uuid: String,
    position: Int,
    favorite: Boolean = false,
    archived: Boolean = false,
    createdAtMillis: Long,
    updatedAtMillis: Long = createdAtMillis,
) = ExerciseEntity(
    id = id,
    uuid = uuid,
    name = name.trim(),
    trackingType = trackingType.name,
    notes = notes.trim(),
    equipment = equipment.trim(),
    primaryMuscles = primaryMuscles.trim(),
    secondaryMuscles = secondaryMuscles.trim(),
    weightUnitId = weightUnitId,
    weightIncrement = weightIncrement,
    repetitionIncrement = repetitionIncrement,
    defaultRestSeconds = defaultRestSeconds,
    defaultGraphMetric = defaultGraphMetric,
    oneRepMaxFormula = oneRepMaxFormula.name,
    barWeightKg = barWeightKg,
    availablePlatesKgCsv = availablePlatesKg.joinToString(","),
    includeInVolume = includeInVolume,
    includeInPersonalRecords = includeInPersonalRecords,
    bodyweightLoadPolicy = bodyweightLoadPolicy.name,
    effectiveBodyweightPercent = effectiveBodyweightPercent,
    showRpe = showRpe,
    showRir = showRir,
    showTempo = showTempo,
    favorite = favorite,
    position = position,
    archived = archived,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    loadInterpretation = loadInterpretation.name,
)

private fun ExerciseEntity.toDomain() = Exercise(
    id = id,
    uuid = uuid,
    name = name,
    trackingType = ExerciseTrackingType.valueOf(trackingType),
    notes = notes,
    equipment = equipment,
    primaryMuscles = primaryMuscles,
    secondaryMuscles = secondaryMuscles,
    weightUnitId = weightUnitId,
    weightIncrement = weightIncrement,
    repetitionIncrement = repetitionIncrement,
    defaultRestSeconds = defaultRestSeconds,
    defaultGraphMetric = defaultGraphMetric,
    oneRepMaxFormula = EstimatedOneRepMaxFormula.valueOf(oneRepMaxFormula),
    barWeightKg = barWeightKg,
    availablePlatesKg = availablePlatesKgCsv.split(',').mapNotNull(String::toDoubleOrNull),
    includeInVolume = includeInVolume,
    includeInPersonalRecords = includeInPersonalRecords,
    bodyweightLoadPolicy = BodyweightLoadPolicy.valueOf(bodyweightLoadPolicy),
    effectiveBodyweightPercent = effectiveBodyweightPercent,
    showRpe = showRpe,
    showRir = showRir,
    showTempo = showTempo,
    favorite = favorite,
    position = position,
    archived = archived,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    loadInterpretation = runCatching { LoadInterpretation.valueOf(loadInterpretation) }.getOrDefault(LoadInterpretation.Total),
)

private fun Exercise.toDraft() = ExerciseDraft(
    name, trackingType, notes, equipment, primaryMuscles, secondaryMuscles,
    weightUnitId, weightIncrement, repetitionIncrement, defaultRestSeconds,
    defaultGraphMetric, oneRepMaxFormula, barWeightKg, availablePlatesKg,
    includeInVolume, includeInPersonalRecords, bodyweightLoadPolicy,
    effectiveBodyweightPercent, showRpe, showRir, showTempo,
    categoryIds = emptySet(), loadInterpretation = loadInterpretation,
)

private data class RetargetedWorkoutLoad(
    val canonicalKg: Double?,
    val enteredValue: Double?,
    val enteredUnitId: String?,
    val machineValue: Double?,
)

private fun WorkoutSetEntity.retargetForMachineChange(
    workoutExercise: WorkoutExerciseEntity,
    machine: GymMachineEntity?,
    exercise: ExerciseEntity,
    now: Long,
): WorkoutSetEntity {
    val actual = retargetWorkoutLoad(
        canonicalKg = canonicalWeightKg,
        ordinalValue = machineLoadValue,
        workoutExercise = workoutExercise,
        machine = machine,
        exercise = exercise,
        unilateral = unilateral,
    )
    val prescribed = retargetWorkoutLoad(
        canonicalKg = prescribedCanonicalWeightKg,
        ordinalValue = prescribedMachineLoadValue,
        workoutExercise = workoutExercise,
        machine = machine,
        exercise = exercise,
        unilateral = unilateral,
    )
    return copy(
        canonicalWeightKg = actual.canonicalKg,
        enteredWeight = actual.enteredValue,
        enteredWeightUnitId = actual.enteredUnitId,
        machineLoadValue = actual.machineValue,
        prescribedCanonicalWeightKg = prescribed.canonicalKg,
        prescribedEnteredWeight = prescribed.enteredValue,
        prescribedWeightUnitId = prescribed.enteredUnitId,
        prescribedMachineLoadValue = prescribed.machineValue,
        updatedAtMillis = now,
    )
}

private fun retargetWorkoutLoad(
    canonicalKg: Double?,
    ordinalValue: Double?,
    workoutExercise: WorkoutExerciseEntity,
    machine: GymMachineEntity?,
    exercise: ExerciseEntity,
    unilateral: Boolean,
): RetargetedWorkoutLoad {
    val machineType = machine?.loadType?.let(MachineLoadType::valueOf)
    if (canonicalKg == null && ordinalValue == null) {
        return RetargetedWorkoutLoad(null, null, null, null)
    }
    if (machineType == MachineLoadType.Level) {
        val available = machine.availableLoadsCsv.split(',').mapNotNull(String::toDoubleOrNull)
        val mapping = machine.massMappingCsv.parseStableMappingCsv()
        val setting = if (canonicalKg != null) {
            require(mapping.isNotEmpty()) {
                "This level-based machine has no resistance mapping, so the existing mass prescription cannot be translated safely"
            }
            val interpretation = runCatching { LoadInterpretation.valueOf(machine.loadInterpretation) }
                .getOrDefault(LoadInterpretation.OrdinalSetting)
            val stackMode = runCatching { MachineStackMode.valueOf(machine.stackMode) }
                .getOrDefault(MachineStackMode.Single)
            val candidates = mapping.keys.filter { available.isEmpty() || it in available }.ifEmpty { mapping.keys }
            require(candidates.isNotEmpty()) { "This machine has no usable resistance settings" }
            candidates.minBy { candidate ->
                val candidateKg = canonicalResistanceKg(
                    enteredValue = null,
                    enteredUnitId = null,
                    machineSetting = candidate,
                    interpretation = interpretation,
                    baseLoadKg = machine.baseLoadKg,
                    addOnPlateKg = machine.addOnPlateKg,
                    massMappingKg = mapping,
                    stackMode = stackMode,
                    pulleyRatio = machine.pulleyRatio,
                    unilateral = unilateral,
                ) ?: Double.POSITIVE_INFINITY
                kotlin.math.abs(candidateKg - canonicalKg)
            }
        } else {
            requireNotNull(ordinalValue) { "The existing machine setting is unavailable" }.let { target ->
                available.minByOrNull { kotlin.math.abs(it - target) } ?: target
            }
        }
        val mappedCanonical = canonicalResistanceKg(
            enteredValue = null,
            enteredUnitId = null,
            machineSetting = setting,
            interpretation = runCatching { LoadInterpretation.valueOf(machine.loadInterpretation) }
                .getOrDefault(LoadInterpretation.OrdinalSetting),
            baseLoadKg = machine.baseLoadKg,
            addOnPlateKg = machine.addOnPlateKg,
            massMappingKg = mapping,
            stackMode = runCatching { MachineStackMode.valueOf(machine.stackMode) }
                .getOrDefault(MachineStackMode.Single),
            pulleyRatio = machine.pulleyRatio,
            unilateral = unilateral,
        )
        return RetargetedWorkoutLoad(mappedCanonical, null, null, setting)
    }

    require(canonicalKg != null) {
        "The existing level setting has no resistance mapping, so it cannot be translated to mass-based equipment safely"
    }
    val interpretation = runCatching {
        LoadInterpretation.valueOf(machine?.loadInterpretation ?: exercise.loadInterpretation)
    }.getOrDefault(LoadInterpretation.Total)
    val stackMode = machine?.stackMode?.let { runCatching { MachineStackMode.valueOf(it) }.getOrNull() }
        ?: MachineStackMode.Single
    val multiplier = loadInterpretationMultiplier(
        interpretation = interpretation,
        stackMode = stackMode,
        pulleyRatio = machine?.pulleyRatio ?: 1.0,
        unilateral = unilateral,
    )
    val rawKg = ((canonicalKg - (machine?.baseLoadKg ?: exercise.barWeightKg.takeIf {
        interpretation == LoadInterpretation.PerSide
    } ?: 0.0) - (machine?.addOnPlateKg ?: 0.0)) / multiplier).coerceAtLeast(0.0)
    val unitId = machine?.unitId?.takeIf(String::isNotBlank)
        ?: workoutExercise.exerciseWeightUnitSnapshot.takeIf(String::isNotBlank)
        ?: exercise.weightUnitId
    val rawDisplay = massFromKilograms(rawKg, unitId)
    val choices = machine?.availableLoadsCsv?.split(',')?.mapNotNull(String::toDoubleOrNull).orEmpty()
    val display = if (choices.isNotEmpty()) {
        choices.minBy { kotlin.math.abs(it - rawDisplay) }
    } else {
        val increment = exercise.weightIncrement.takeIf { it.isFinite() && it > 0.0 }
            ?: if (unitId == "pound") 5.0 else 2.5
        kotlin.math.round(rawDisplay / increment) * increment
    }.coerceAtLeast(0.0)
    val translatedCanonical = canonicalResistanceKg(
        enteredValue = display,
        enteredUnitId = unitId,
        interpretation = interpretation,
        baseLoadKg = machine?.baseLoadKg ?: exercise.barWeightKg.takeIf {
            interpretation == LoadInterpretation.PerSide
        },
        addOnPlateKg = machine?.addOnPlateKg,
        stackMode = stackMode,
        pulleyRatio = machine?.pulleyRatio ?: 1.0,
        unilateral = unilateral,
    )
    return RetargetedWorkoutLoad(
        canonicalKg = translatedCanonical,
        enteredValue = display,
        enteredUnitId = unitId,
        machineValue = display.takeIf { machine != null },
    )
}

private fun ExerciseCategoryEntity.toDomain() = ExerciseCategory(
    id, uuid, name, kind, position, archived, createdAtMillis, updatedAtMillis,
)

private fun WorkoutSessionEntity.toDomain() = WorkoutSession(
    id = id,
    uuid = uuid,
    name = name,
    notes = notes,
    startedAt = Instant.ofEpochMilli(startedAtMillis),
    endedAt = endedAtMillis?.let(Instant::ofEpochMilli),
    localDate = LocalDate.ofEpochDay(localEpochDay),
    zoneId = zoneId,
    state = WorkoutSessionState.valueOf(state),
    keepScreenAwake = keepScreenAwake,
    restTimerDeadlineMillis = restTimerDeadlineMillis,
    restTimerDurationSeconds = restTimerDurationSeconds,
    archived = archived,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    sourceRoutineId = sourceRoutineId,
    sourceRoutineDayId = sourceRoutineDayId,
    sourceRoutineProgramKind = runCatching { RoutineProgramKind.valueOf(sourceRoutineProgramKind) }
        .getOrDefault(RoutineProgramKind.Static),
    sourceRoutinePhaseIndex = sourceRoutinePhaseIndex,
    sourceRoutineCycle = sourceRoutineCycle,
    sourceRoutineDayPosition = sourceRoutineDayPosition,
    sourceRoutineDayProgressionIndex = sourceRoutineDayProgressionIndex,
    programProgressAdvanced = programProgressAdvanced,
    requiredMainWorkInvalidated = requiredMainWorkInvalidated,
    invalidatedMainExerciseIds = invalidatedMainExerciseIdsCsv
        .split(',').mapNotNull(String::toLongOrNull).toSet(),
    sourceRoutinePhaseLabel = sourceRoutinePhaseLabel,
    sourceRoutinePhaseRole = runCatching { RoutineProgramPhaseRole.valueOf(sourceRoutinePhaseRole) }
        .getOrDefault(RoutineProgramPhaseRole.Standard),
    workoutRevision = workoutRevision,
    restTimerRevision = restTimerRevision,
    restTimerCleanupPending = restTimerCleanupPending,
)

private fun WorkoutExerciseEntity.toDomain() = WorkoutExercise(
    id = id,
    uuid = uuid,
    sessionId = sessionId,
    exerciseId = exerciseId,
    position = position,
    notes = notes,
    groupId = groupId,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    machineProfileUuidSnapshot = machineProfileUuidSnapshot,
    machineId = machineId,
    machineNameSnapshot = machineNameSnapshot,
    machineLoadTypeSnapshot = machineLoadTypeSnapshot.takeIf(String::isNotBlank)?.let(MachineLoadType::valueOf),
    machineUnitIdSnapshot = machineUnitIdSnapshot,
    machineLevelLabelSnapshot = machineLevelLabelSnapshot,
    loadInterpretationSnapshot = runCatching { LoadInterpretation.valueOf(loadInterpretationSnapshot) }.getOrDefault(LoadInterpretation.Total),
    baseLoadKgSnapshot = baseLoadKgSnapshot,
    trackingTypeSnapshot = runCatching { ExerciseTrackingType.valueOf(trackingTypeSnapshot) }.getOrDefault(ExerciseTrackingType.WeightReps),
    bodyweightLoadPolicySnapshot = runCatching { BodyweightLoadPolicy.valueOf(bodyweightLoadPolicySnapshot) }.getOrDefault(BodyweightLoadPolicy.ExternalWeightOnly),
    effectiveBodyweightPercentSnapshot = effectiveBodyweightPercentSnapshot,
    oneRepMaxFormulaSnapshot = runCatching { EstimatedOneRepMaxFormula.valueOf(oneRepMaxFormulaSnapshot) }.getOrDefault(EstimatedOneRepMaxFormula.Epley),
    includeInVolumeSnapshot = includeInVolumeSnapshot,
    includeInPersonalRecordsSnapshot = includeInPersonalRecordsSnapshot,
    exerciseWeightUnitSnapshot = exerciseWeightUnitSnapshot,
    loadMultiplierSnapshot = loadMultiplierSnapshot,
    machineConfigurationGroupSnapshot = machineConfigurationGroupSnapshot,
    machineConfigurationVersionSnapshot = machineConfigurationVersionSnapshot,
    machineConfigurationSnapshot = machineConfigurationSnapshot,
    machinePulleyRatioSnapshot = machinePulleyRatioSnapshot,
    machineStackModeSnapshot = runCatching { MachineStackMode.valueOf(machineStackModeSnapshot) }.getOrDefault(MachineStackMode.Single),
    machineAddOnPlateKgSnapshot = machineAddOnPlateKgSnapshot,
    machineMassMappingKgSnapshot = machineMassMappingCsvSnapshot.parseStableMappingCsv(),
    alternativeExerciseIdsSnapshot = alternativeExerciseIdsCsvSnapshot.split(',').mapNotNull(String::toLongOrNull),
    trainingMaxKgSnapshot = trainingMaxKgSnapshot,
    trainingMaxValueSnapshot = trainingMaxValueSnapshot,
    trainingMaxUnitIdSnapshot = trainingMaxUnitIdSnapshot,
    cycleIncrementValueSnapshot = cycleIncrementValueSnapshot,
    trainingMaxSourceSnapshot = runCatching { RoutineTrainingMaxSource.valueOf(trainingMaxSourceSnapshot) }
        .getOrDefault(RoutineTrainingMaxSource.EstimatedOneRepMaxPercent),
    mainWorkSchemeSnapshot = runCatching { RoutineMainWorkScheme.valueOf(mainWorkSchemeSnapshot) }
        .getOrDefault(RoutineMainWorkScheme.Unspecified),
    supplementalSchemeSnapshot = runCatching { RoutineSupplementalScheme.valueOf(supplementalSchemeSnapshot) }
        .getOrDefault(RoutineSupplementalScheme.None),
    assistanceRoleSnapshot = runCatching { RoutineAssistanceRole.valueOf(assistanceRoleSnapshot) }
        .getOrDefault(RoutineAssistanceRole.Unspecified),
    placementKindSnapshot = runCatching { RoutinePlacementKind.valueOf(placementKindSnapshot) }
        .getOrDefault(RoutinePlacementKind.General),
    assistanceCategorySnapshot = runCatching { RoutineAssistanceCategory.valueOf(assistanceCategorySnapshot) }
        .getOrDefault(RoutineAssistanceCategory.Unspecified),
    jokerSetsEnabledSnapshot = jokerSetsEnabledSnapshot,
    outcome = runCatching { WorkoutExerciseOutcome.valueOf(outcome) }
        .getOrDefault(WorkoutExerciseOutcome.Active),
    outcomeAtMillis = outcomeAtMillis,
    replacementWorkoutExerciseUuid = replacementWorkoutExerciseUuid,
)

private fun WorkoutGroupEntity.toDomain() = WorkoutGroup(
    id, uuid, sessionId, name, WorkoutGroupType.valueOf(type), position,
    createdAtMillis, updatedAtMillis,
)

private fun WorkoutSetEntity.toDomain() = WorkoutSet(
    id = id,
    uuid = uuid,
    workoutExerciseId = workoutExerciseId,
    position = position,
    classification = WorkoutSetClassification.valueOf(classification),
    planned = planned,
    completed = completed,
    canonicalWeightKg = canonicalWeightKg,
    enteredWeight = enteredWeight,
    enteredWeightUnitId = enteredWeightUnitId,
    repetitions = repetitions,
    canonicalDistanceMetres = canonicalDistanceMetres,
    enteredDistance = enteredDistance,
    enteredDistanceUnitId = enteredDistanceUnitId,
    durationSeconds = durationSeconds,
    bodyweightKg = bodyweightKg,
    note = note,
    rpe = rpe,
    rir = rir,
    tempo = tempo,
    restSeconds = restSeconds,
    completedAtMillis = completedAtMillis,
    deletedAtMillis = deletedAtMillis,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    machineLoadValue = machineLoadValue,
    unilateral = unilateral,
    prescribedCanonicalWeightKg = prescribedCanonicalWeightKg,
    prescribedEnteredWeight = prescribedEnteredWeight,
    prescribedWeightUnitId = prescribedWeightUnitId,
    prescribedRepetitions = prescribedRepetitions,
    prescribedRepetitionsMax = prescribedRepetitionsMax,
    prescribedRpe = prescribedRpe,
    prescribedRir = prescribedRir,
    prescribedDurationSeconds = prescribedDurationSeconds,
    prescribedMachineLoadValue = prescribedMachineLoadValue,
    prescriptionSourceLabel = prescriptionSourceLabel,
    workSectionSnapshot = runCatching { RoutineWorkSection.valueOf(workSectionSnapshot) }
        .getOrDefault(RoutineWorkSection.Unspecified),
    optionalWorkKindSnapshot = runCatching { RoutineOptionalWorkKind.valueOf(optionalWorkKindSnapshot) }
        .getOrDefault(RoutineOptionalWorkKind.None),
    prescribedClassificationSnapshot = runCatching {
        WorkoutSetClassification.valueOf(prescribedClassificationSnapshot)
    }.getOrDefault(WorkoutSetClassification.Working),
    requiredForProgressionSnapshot = requiredForProgressionSnapshot,
    removalReason = removalReason?.let { value ->
        runCatching { WorkoutSetRemovalReason.valueOf(value) }.getOrNull()
    },
)

private fun WorkoutSetEntity.toDraft() = WorkoutSetDraft(
    weight = enteredWeight,
    weightUnitId = enteredWeightUnitId ?: "kilogram",
    reps = repetitions,
    repsMax = prescribedRepetitionsMax,
    distance = enteredDistance,
    distanceUnitId = enteredDistanceUnitId ?: "kilometre",
    durationSeconds = durationSeconds,
    bodyweightKg = bodyweightKg,
    planned = planned,
    completed = completed,
    classification = WorkoutSetClassification.valueOf(classification),
    note = note,
    rpe = rpe,
    rir = rir,
    tempo = tempo,
    restSeconds = restSeconds,
    machineLoadValue = machineLoadValue,
    unilateral = unilateral,
    workSection = runCatching { RoutineWorkSection.valueOf(workSectionSnapshot) }
        .getOrDefault(RoutineWorkSection.Unspecified),
    optionalWorkKind = runCatching { RoutineOptionalWorkKind.valueOf(optionalWorkKindSnapshot) }
        .getOrDefault(RoutineOptionalWorkKind.None),
)

/**
 * A retired placement is history, not part of the current execution plan. Reuse it only when
 * it contains retained work the lifter actually performed; otherwise a pre-set substitution or
 * removal would resurrect an empty exercise in duplicated workouts and saved routines.
 */
internal data class WorkoutReuseProjection(
    val workoutExercise: WorkoutExerciseEntity,
    val sets: List<WorkoutSetEntity>,
)

internal fun WorkoutExerciseEntity.toWorkoutReuseProjection(
    sourceSets: List<WorkoutSetEntity>,
): WorkoutReuseProjection? {
    val retainedSets = sourceSets
        .asSequence()
        .filter { it.deletedAtMillis == null }
        .filter { outcome == WorkoutExerciseOutcome.Active.name || it.completed }
        .sortedWith(compareBy(WorkoutSetEntity::position, WorkoutSetEntity::id))
        .toList()
    return WorkoutReuseProjection(this, retainedSets)
        .takeIf { outcome == WorkoutExerciseOutcome.Active.name || retainedSets.isNotEmpty() }
}

/** Generic reuse is workout-only work; it must never mint new prescribed 5/3/1 requirements. */
private fun WorkoutSetEntity.asWorkoutOnlyCopy(
    id: Long,
    uuid: String,
    workoutExerciseId: Long,
    position: Int,
    createdAtMillis: Long,
    updatedAtMillis: Long,
    programmed: Boolean = false,
    planned: Boolean = true,
): WorkoutSetEntity = copy(
    id = id,
    uuid = uuid,
    workoutExerciseId = workoutExerciseId,
    position = position,
    classification = WorkoutSetClassification.Working.name,
    planned = planned,
    completed = false,
    completedAtMillis = null,
    deletedAtMillis = null,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    prescribedCanonicalWeightKg = null,
    prescribedEnteredWeight = null,
    prescribedWeightUnitId = null,
    prescribedRepetitions = null,
    prescribedRepetitionsMax = null,
    prescribedRpe = null,
    prescribedRir = null,
    prescribedDurationSeconds = null,
    prescribedMachineLoadValue = null,
    prescriptionSourceLabel = "",
    workSectionSnapshot = if (programmed) RoutineWorkSection.Optional.name else RoutineWorkSection.Unspecified.name,
    optionalWorkKindSnapshot = RoutineOptionalWorkKind.None.name,
    prescribedClassificationSnapshot = WorkoutSetClassification.Working.name,
    requiredForProgressionSnapshot = false,
    removalReason = null,
)

private fun WorkoutSetDraft.toEntity(
    id: Long = 0,
    uuid: String,
    workoutExerciseId: Long,
    position: Int,
    deletedAtMillis: Long? = null,
    completedAtMillis: Long? = null,
    createdAtMillis: Long,
    updatedAtMillis: Long = createdAtMillis,
    workoutExercise: WorkoutExerciseEntity,
): WorkoutSetEntity {
    val machineType = workoutExercise.machineLoadTypeSnapshot.takeIf(String::isNotBlank)?.let(MachineLoadType::valueOf)
    val effectiveWeight = weight.takeUnless { machineType == MachineLoadType.Level }
    val effectiveWeightUnitId = if (machineType == MachineLoadType.Mass) workoutExercise.machineUnitIdSnapshot else weightUnitId
    val weightUnit = BuiltInUnits.get(effectiveWeightUnitId)
    if (effectiveWeight != null) require(weightUnit?.dimension == UnitDimension.Mass) { "Invalid weight unit" }
    val distanceUnit = BuiltInUnits.get(distanceUnitId)
    if (distance != null) require(distanceUnit?.dimension == UnitDimension.Distance) { "Invalid distance unit" }
    require(effectiveWeight == null || effectiveWeight.isFinite()) { "Weight must be finite" }
    require(distance == null || distance.isFinite()) { "Distance must be finite" }
    require(bodyweightKg == null || bodyweightKg.isFinite()) { "Bodyweight must be finite" }
    return WorkoutSetEntity(
        id = id,
        uuid = uuid,
        workoutExerciseId = workoutExerciseId,
        position = position,
        classification = classification.name,
        planned = planned,
        completed = completed,
        canonicalWeightKg = canonicalResistanceKg(
            enteredValue = effectiveWeight,
            enteredUnitId = effectiveWeightUnitId,
            machineSetting = machineLoadValue.takeIf { machineType == MachineLoadType.Level },
            interpretation = runCatching { LoadInterpretation.valueOf(workoutExercise.loadInterpretationSnapshot) }
                .getOrDefault(LoadInterpretation.Total),
            baseLoadKg = workoutExercise.baseLoadKgSnapshot,
            addOnPlateKg = workoutExercise.machineAddOnPlateKgSnapshot,
            massMappingKg = workoutExercise.machineMassMappingCsvSnapshot.parseStableMappingCsv(),
            stackMode = runCatching { MachineStackMode.valueOf(workoutExercise.machineStackModeSnapshot) }
                .getOrDefault(MachineStackMode.Single),
            pulleyRatio = workoutExercise.machinePulleyRatioSnapshot,
            unilateral = unilateral,
        ),
        enteredWeight = effectiveWeight,
        enteredWeightUnitId = effectiveWeightUnitId.takeIf { effectiveWeight != null },
        repetitions = reps,
        prescribedRepetitionsMax = repsMax,
        canonicalDistanceMetres = distance?.let { requireNotNull(distanceUnit).toCanonical(it) },
        enteredDistance = distance,
        enteredDistanceUnitId = distanceUnitId.takeIf { distance != null },
        durationSeconds = durationSeconds,
        bodyweightKg = bodyweightKg,
        note = note.trim(),
        rpe = rpe,
        rir = rir,
        tempo = tempo.trim(),
        restSeconds = restSeconds,
        completedAtMillis = completedAtMillis,
        deletedAtMillis = deletedAtMillis,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
        machineLoadValue = machineLoadValue ?: effectiveWeight.takeIf { workoutExercise.machineId != null },
        unilateral = unilateral,
        workSectionSnapshot = workSection.name,
        optionalWorkKindSnapshot = optionalWorkKind.name,
        prescribedClassificationSnapshot = classification.name,
    )
}

internal fun Map<Double, Double>.toStableMappingCsv(): String = entries
    .sortedBy(Map.Entry<Double, Double>::key)
    .joinToString(",") { (setting, kg) -> "$setting:$kg" }

internal fun String.parseStableMappingCsv(): Map<Double, Double> = split(',').mapNotNull { item ->
    val parts = item.split(':', limit = 2)
    val setting = parts.getOrNull(0)?.toDoubleOrNull() ?: return@mapNotNull null
    val kg = parts.getOrNull(1)?.toDoubleOrNull() ?: return@mapNotNull null
    setting to kg
}.toMap()
