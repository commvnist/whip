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
import com.whip.app.domain.WorkoutGroup
import com.whip.app.domain.WorkoutGroupType
import com.whip.app.domain.WorkoutSession
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.domain.WorkoutSet
import com.whip.app.domain.WorkoutSetClassification
import com.whip.app.domain.WorkoutSetDraft
import com.whip.app.domain.RoutineProgramKind
import com.whip.app.domain.RoutineTrainingMaxSource
import com.whip.app.domain.validateWorkoutSetDraft
import com.whip.app.domain.applyPolicySnapshot
import com.whip.app.domain.massToKilograms
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

interface GymRepository {
    val machines: Flow<List<GymMachine>>
    val exercises: Flow<List<Exercise>>
    val categories: Flow<List<ExerciseCategory>>
    val categoryLinks: Flow<List<ExerciseCategoryLink>>
    val sessions: Flow<List<WorkoutSession>>
    val workoutExercises: Flow<List<WorkoutExercise>>
    val sets: Flow<List<WorkoutSet>>
    val groups: Flow<List<WorkoutGroup>>

    suspend fun createMachine(draft: GymMachineDraft): Long
    suspend fun updateMachine(id: Long, draft: GymMachineDraft)
    suspend fun createMachineVersion(id: Long, draft: GymMachineDraft): Long
    suspend fun setMachineArchived(id: Long, archived: Boolean)

    suspend fun createExercise(draft: ExerciseDraft): Long
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
        zoneId: ZoneId = ZoneId.systemDefault(),
        keepScreenAwake: Boolean? = null,
    ): Long
    suspend fun updateWorkout(id: Long, name: String, notes: String, keepScreenAwake: Boolean)
    suspend fun finishWorkout(id: Long)
    suspend fun resumeWorkout(id: Long)
    suspend fun discardWorkout(id: Long)
    suspend fun restoreWorkout(id: Long)
    suspend fun duplicateWorkout(id: Long, asActive: Boolean = true): Long
    suspend fun copyWorkoutExerciseToActive(workoutExerciseId: Long): Long

    suspend fun addExerciseToWorkout(sessionId: Long, exerciseId: Long, machineId: Long? = null): Long
    suspend fun updateWorkoutExercise(id: Long, notes: String, groupId: Long? = null)
    suspend fun setWorkoutExerciseMachine(id: Long, machineId: Long?)
    suspend fun substituteWorkoutExercise(id: Long, exerciseId: Long, machineId: Long? = null): Long
    suspend fun removeWorkoutExercise(id: Long)
    suspend fun removeWorkoutExerciseFromGroup(id: Long)
    suspend fun reorderWorkoutExercises(sessionId: Long, idsInOrder: List<Long>)
    suspend fun normalizeWorkoutGroups(sessionId: Long)
    suspend fun createGroup(
        sessionId: Long,
        name: String,
        type: WorkoutGroupType,
        workoutExerciseIds: List<Long>,
    ): Long

    suspend fun addSet(workoutExerciseId: Long, draft: WorkoutSetDraft? = null): Long
    suspend fun updateSet(id: Long, draft: WorkoutSetDraft)
    suspend fun setSetCompleted(
        id: Long,
        completed: Boolean,
        autoStartRest: Boolean = true,
        restOverrideSeconds: Int? = null,
    )
    suspend fun duplicateSet(id: Long): Long
    suspend fun deleteSet(id: Long)
    suspend fun undoDeleteSet(id: Long)
    suspend fun reorderSets(workoutExerciseId: Long, idsInOrder: List<Long>)

    suspend fun startRestTimer(sessionId: Long, seconds: Int)
    suspend fun adjustRestTimer(sessionId: Long, deltaSeconds: Int)
    suspend fun stopRestTimer(sessionId: Long)
}

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

    override suspend fun createMachine(draft: GymMachineDraft): Long = database.withTransaction {
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
        syncMachineExercises(id, exerciseIds)
    }

    private suspend fun syncMachineExercises(machineId: Long, exerciseIds: Set<Long>) {
        dao.clearMachineExercises(machineId)
        exerciseIds.forEach { exerciseId ->
            dao.upsertMachineExerciseJoin(GymMachineExerciseJoinEntity(machineId, exerciseId))
        }
    }

    override suspend fun setMachineArchived(id: Long, archived: Boolean) {
        val existing = dao.getMachine(id) ?: return
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

    override suspend fun updateExercise(id: Long, draft: ExerciseDraft) {
        validateExercise(draft)
        val existing = dao.getExercise(id) ?: error("Exercise no longer exists")
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
        val existing = dao.getExercise(id) ?: return
        dao.updateExercise(existing.copy(archived = archived, updatedAtMillis = nowMillis()))
    }

    override suspend fun setExerciseFavorite(id: Long, favorite: Boolean) {
        val existing = dao.getExercise(id) ?: return
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
        val current = dao.getCategory(id) ?: return
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
        zoneId: ZoneId,
        keepScreenAwake: Boolean?,
    ): Long = database.withTransaction {
        require(dao.getActiveSession() == null) { "Finish or discard the active workout first" }
        val start = startedAt ?: clock.now()
        val now = clock.now().toEpochMilli()
        dao.insertSession(
            WorkoutSessionEntity(
                uuid = ids.nextId(),
                name = name.trim(),
                notes = notes.trim(),
                startedAtMillis = start.toEpochMilli(),
                endedAtMillis = null,
                localEpochDay = (localDate ?: start.atZone(zoneId).toLocalDate()).toEpochDay(),
                zoneId = zoneId.id,
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
        val session = dao.getSession(id) ?: return
        dao.updateSession(
            session.copy(
                name = name.trim(),
                notes = notes.trim(),
                keepScreenAwake = keepScreenAwake,
                updatedAtMillis = nowMillis(),
            ),
        )
    }

    override suspend fun finishWorkout(id: Long) = database.withTransaction {
        val session = dao.getSession(id) ?: return@withTransaction
        val now = nowMillis()
        val advanced = if (session.state == WorkoutSessionState.Active.name && !session.programProgressAdvanced) {
            advanceRoutineProgressForSession(session, now)
        } else {
            session.programProgressAdvanced
        }
        dao.updateSession(
            session.copy(
                state = WorkoutSessionState.Finished.name,
                endedAtMillis = session.endedAtMillis ?: now,
                restTimerDeadlineMillis = null,
                updatedAtMillis = now,
                programProgressAdvanced = advanced,
            ),
        )
    }

    private suspend fun advanceRoutineProgressForSession(session: WorkoutSessionEntity, now: Long): Boolean {
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
        val next = when {
            !finalDay -> routine.copy(nextProgramDayPosition = dayPosition + 1, updatedAtMillis = now)
            !finalPhase -> routine.copy(
                currentProgramPhaseIndex = phase + 1,
                nextProgramDayPosition = 0,
                updatedAtMillis = now,
            )
            else -> routine.copy(
                currentProgramPhaseIndex = 0,
                currentProgramCycle = cycle + 1,
                nextProgramDayPosition = 0,
                updatedAtMillis = now,
            )
        }
        routineDao.updateRoutine(next)
        if (finalDay && finalPhase) {
            days.forEach { day ->
                routineDao.getExercises(day.id).forEach { exercise ->
                    val current = exercise.trainingMaxValue ?: return@forEach
                    val increment = exercise.cycleIncrementValue ?: return@forEach
                    if (increment <= 0.0) return@forEach
                    val updatedValue = current + increment
                    routineDao.updateExercise(
                        exercise.copy(
                            trainingMaxValue = updatedValue,
                            trainingMaxKg = massToKilograms(updatedValue, exercise.trainingMaxUnitId),
                            updatedAtMillis = now,
                        ),
                    )
                }
            }
        }
        return true
    }

    override suspend fun resumeWorkout(id: Long) = database.withTransaction {
        val session = dao.getSession(id) ?: return@withTransaction
        val active = dao.getActiveSession()
        require(active == null || active.id == id) { "Another workout is active" }
        dao.updateSession(
            session.copy(
                state = WorkoutSessionState.Active.name,
                archived = false,
                endedAtMillis = null,
                updatedAtMillis = nowMillis(),
            ),
        )
    }

    override suspend fun discardWorkout(id: Long) {
        val session = dao.getSession(id) ?: return
        dao.updateSession(
            session.copy(
                state = WorkoutSessionState.Discarded.name,
                endedAtMillis = session.endedAtMillis ?: nowMillis(),
                restTimerDeadlineMillis = null,
                archived = true,
                updatedAtMillis = nowMillis(),
            ),
        )
    }

    override suspend fun restoreWorkout(id: Long) {
        val session = dao.getSession(id) ?: return
        dao.updateSession(
            session.copy(
                state = WorkoutSessionState.Finished.name,
                archived = false,
                updatedAtMillis = nowMillis(),
            ),
        )
    }

    override suspend fun duplicateWorkout(id: Long, asActive: Boolean): Long =
        database.withTransaction {
            if (asActive) require(dao.getActiveSession() == null) { "Another workout is active" }
            val source = dao.getSession(id) ?: error("Workout no longer exists")
            val sourceWorkoutExercises = dao.getWorkoutExercises(id)
            sourceWorkoutExercises.forEach(WorkoutExerciseEntity::requireResolvedEquipmentForNewWorkout)
            val now = nowMillis()
            val newSessionId = dao.insertSession(
                source.copy(
                    id = 0,
                    uuid = ids.nextId(),
                    name = source.name.ifBlank { "Copied workout" },
                    startedAtMillis = now,
                    endedAtMillis = null,
                    localEpochDay = clock.today().toEpochDay(),
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
                ),
            )
            sourceWorkoutExercises.forEach { sourceWorkoutExercise ->
                val newWorkoutExerciseId = dao.insertWorkoutExercise(
                    sourceWorkoutExercise.copy(
                        id = 0,
                        uuid = ids.nextId(),
                        sessionId = newSessionId,
                        groupId = null,
                        createdAtMillis = now,
                        updatedAtMillis = now,
                    ),
                )
                dao.getWorkoutSets(sourceWorkoutExercise.id)
                    .filter { it.deletedAtMillis == null }
                    .forEach { sourceSet ->
                        dao.insertWorkoutSet(
                            sourceSet.copy(
                                id = 0,
                                uuid = ids.nextId(),
                                workoutExerciseId = newWorkoutExerciseId,
                                planned = true,
                                completed = false,
                                completedAtMillis = null,
                                deletedAtMillis = null,
                                createdAtMillis = now,
                                updatedAtMillis = now,
                            ),
                        )
                    }
            }
            newSessionId
        }

    override suspend fun copyWorkoutExerciseToActive(workoutExerciseId: Long): Long =
        database.withTransaction {
            val source = dao.getWorkoutExercise(workoutExerciseId) ?: error("Workout exercise no longer exists")
            source.requireResolvedEquipmentForNewWorkout()
            val targetSessionId = dao.getActiveSession()?.id ?: startWorkout(name = "Copied workout")
            val now = nowMillis()
            val targetId = dao.insertWorkoutExercise(
                source.copy(
                    id = 0, uuid = ids.nextId(), sessionId = targetSessionId,
                    position = dao.nextWorkoutExercisePosition(targetSessionId), groupId = null,
                    createdAtMillis = now, updatedAtMillis = now,
                ),
            )
            dao.getWorkoutSets(source.id).filter { it.deletedAtMillis == null }.forEachIndexed { index, set ->
                dao.insertWorkoutSet(
                    set.copy(
                        id = 0, uuid = ids.nextId(), workoutExerciseId = targetId, position = index,
                        planned = true, completed = false, completedAtMillis = null,
                        deletedAtMillis = null, createdAtMillis = now, updatedAtMillis = now,
                    ),
                )
            }
            targetId
        }

    override suspend fun addExerciseToWorkout(sessionId: Long, exerciseId: Long, machineId: Long?): Long =
        database.withTransaction {
            requireNotNull(dao.getSession(sessionId)) { "Workout no longer exists" }
            val exercise = requireNotNull(dao.getExercise(exerciseId)) { "Exercise no longer exists" }
            val now = nowMillis()
            val machine = machineId?.let { selected ->
                requireNotNull(dao.getMachine(selected)) { "Machine no longer exists" }
                    .also {
                        require(dao.machineSupportsExercise(it.id, exerciseId) && !it.archived) {
                            "Machine is not available for this exercise"
                        }
                    }
            }
            dao.insertWorkoutExercise(
                WorkoutExerciseEntity(
                    uuid = ids.nextId(),
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
                        machine?.stackMode?.let { runCatching { MachineStackMode.valueOf(it) }.getOrNull() } ?: MachineStackMode.Single,
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
        }

    override suspend fun updateWorkoutExercise(id: Long, notes: String, groupId: Long?) {
        val current = dao.getWorkoutExercise(id) ?: return
        dao.updateWorkoutExercise(
            current.copy(notes = notes.trim(), groupId = groupId, updatedAtMillis = nowMillis()),
        )
    }

    override suspend fun setWorkoutExerciseMachine(id: Long, machineId: Long?) = database.withTransaction {
        val current = dao.getWorkoutExercise(id) ?: return@withTransaction
        if (current.machineId == machineId) return@withTransaction
        require(dao.getWorkoutSets(id).none { it.deletedAtMillis == null }) {
            "Machine cannot be changed after sets are added. Add the exercise again to preserve history."
        }
        val machine = machineId?.let { selected ->
            requireNotNull(dao.getMachine(selected)) { "Machine no longer exists" }
                .also {
                    require(dao.machineSupportsExercise(it.id, current.exerciseId) && !it.archived) {
                        "Machine is not available for this exercise"
                    }
                }
        }
        val exercise = requireNotNull(dao.getExercise(current.exerciseId)) { "Exercise no longer exists" }
        dao.updateWorkoutExercise(
            current.copy(
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
                exerciseWeightUnitSnapshot = exercise.weightUnitId,
                loadMultiplierSnapshot = loadInterpretationMultiplier(
                    runCatching { LoadInterpretation.valueOf(machine?.loadInterpretation ?: exercise.loadInterpretation) }
                        .getOrDefault(LoadInterpretation.Total),
                    machine?.stackMode?.let { runCatching { MachineStackMode.valueOf(it) }.getOrNull() } ?: MachineStackMode.Single,
                    machine?.pulleyRatio ?: 1.0,
                ),
                machineConfigurationGroupSnapshot = machine?.configurationGroupId.orEmpty(),
                machineConfigurationVersionSnapshot = machine?.configurationVersion ?: 1,
                machineConfigurationSnapshot = machine?.configurationSummary().orEmpty(),
                machinePulleyRatioSnapshot = machine?.pulleyRatio ?: 1.0,
                machineStackModeSnapshot = machine?.stackMode ?: MachineStackMode.Single.name,
                machineAddOnPlateKgSnapshot = machine?.addOnPlateKg,
                machineMassMappingCsvSnapshot = machine?.massMappingCsv.orEmpty(),
                updatedAtMillis = nowMillis(),
            ),
        )
    }

    override suspend fun substituteWorkoutExercise(id: Long, exerciseId: Long, machineId: Long?): Long =
        database.withTransaction {
            val current = requireNotNull(dao.getWorkoutExercise(id)) { "Workout exercise no longer exists" }
            val session = requireNotNull(dao.getSession(current.sessionId)) { "Workout no longer exists" }
            require(session.state == WorkoutSessionState.Active.name) { "Only an active workout can be changed" }
            require(exerciseId != current.exerciseId || machineId != current.machineId) { "Choose a different exercise or machine" }
            val now = nowMillis()
            val newId = addExerciseToWorkout(current.sessionId, exerciseId, machineId)
            val oldName = dao.getExercise(current.exerciseId)?.name.orEmpty()
            dao.getWorkoutExercise(newId)?.let { replacement ->
                dao.updateWorkoutExercise(
                    replacement.copy(
                        notes = replacement.notes.ifBlank { "Substitution for $oldName" },
                        groupId = current.groupId,
                        updatedAtMillis = now,
                    ),
                )
            }
            // "Substitute" is replacement, not addition. Remove the source
            // placement (and its sets via the foreign-key cascade), then put
            // the replacement in the exact same workout position.
            dao.deleteWorkoutExercise(id)
            val orderedIds = dao.getWorkoutExercises(current.sessionId)
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
            newId
        }

    override suspend fun removeWorkoutExercise(id: Long) = database.withTransaction {
        val current = dao.getWorkoutExercise(id) ?: return@withTransaction
        dao.deleteWorkoutExercise(id)
        normalizeWorkoutGroupsInSession(current.sessionId, nowMillis())
        Unit
    }

    override suspend fun removeWorkoutExerciseFromGroup(id: Long) = database.withTransaction {
        val current = dao.getWorkoutExercise(id) ?: return@withTransaction
        val groupId = current.groupId ?: return@withTransaction
        val now = nowMillis()
        dao.updateWorkoutExercise(current.copy(groupId = null, updatedAtMillis = now))

        normalizeWorkoutGroupsInSession(current.sessionId, now)
    }

    override suspend fun reorderWorkoutExercises(sessionId: Long, idsInOrder: List<Long>) =
        database.withTransaction {
            idsInOrder.forEachIndexed { index, id ->
                dao.getWorkoutExercise(id)
                    ?.takeIf { it.sessionId == sessionId }
                    ?.let { dao.updateWorkoutExercise(it.copy(position = index, updatedAtMillis = nowMillis())) }
            }
        }

    override suspend fun normalizeWorkoutGroups(sessionId: Long) = database.withTransaction {
        normalizeWorkoutGroupsInSession(sessionId, nowMillis())
    }

    override suspend fun createGroup(
        sessionId: Long,
        name: String,
        type: WorkoutGroupType,
        workoutExerciseIds: List<Long>,
    ): Long = database.withTransaction {
        require(workoutExerciseIds.size >= 2) { "A group needs at least two exercises" }
        require(workoutExerciseIds.distinct().size == workoutExerciseIds.size) {
            "Choose each exercise only once"
        }
        val now = nowMillis()
        val ordered = dao.getWorkoutExercises(sessionId).sortedWith(compareBy({ it.position }, { it.id }))
        val requestedIds = workoutExerciseIds.toSet()
        val requestedMembers = ordered.filter { it.id in requestedIds }
        require(requestedMembers.size == requestedIds.size) { "Exercise is not in this workout" }
        val requestedName = name.trim()
        val normalizedName = requestedName
            .takeIf { candidate ->
                candidate.isNotBlank() && WorkoutGroupType.entries.none { candidate.equals(it.name, ignoreCase = true) }
            }
            ?: type.name
        val groupId = dao.insertWorkoutGroup(
            WorkoutGroupEntity(
                uuid = ids.nextId(),
                sessionId = sessionId,
                name = normalizedName,
                type = type.name,
                position = 0,
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
        )
        ordered.forEach { item ->
            if (item.id in requestedIds && item.groupId != groupId) {
                dao.updateWorkoutExercise(
                    item.copy(groupId = groupId, updatedAtMillis = now),
                )
            }
        }
        normalizeWorkoutGroupsInSession(sessionId, now)
        groupId
    }

    private suspend fun normalizeWorkoutGroupsInSession(sessionId: Long, now: Long) {
        var ordered = dao.getWorkoutExercises(sessionId).sortedWith(compareBy({ it.position }, { it.id }))
        val groups = dao.getWorkoutGroups(sessionId)
        val knownGroupIds = groups.mapTo(mutableSetOf(), WorkoutGroupEntity::id)

        // Clear dangling or singleton membership and delete empty/singleton group records.
        ordered.filter { it.groupId != null && it.groupId !in knownGroupIds }.forEach { member ->
            dao.updateWorkoutExercise(member.copy(groupId = null, updatedAtMillis = now))
        }
        groups.forEach { group ->
            val members = ordered.filter { it.groupId == group.id }
            if (members.size < 2) {
                members.forEach { member ->
                    dao.updateWorkoutExercise(member.copy(groupId = null, updatedAtMillis = now))
                }
                dao.deleteWorkoutGroup(group.id)
            }
        }

        // Flatten every surviving group at its first authored position. This repairs
        // legacy sessions and prevents regrouping one member from splitting its old group.
        ordered = dao.getWorkoutExercises(sessionId).sortedWith(compareBy({ it.position }, { it.id }))
        val emittedGroups = mutableSetOf<Long>()
        val normalized = buildList {
            ordered.forEach { item ->
                val groupId = item.groupId
                if (groupId == null) add(item)
                else if (emittedGroups.add(groupId)) addAll(ordered.filter { it.groupId == groupId })
            }
        }
        normalized.forEachIndexed { index, item ->
            if (item.position != index) {
                dao.updateWorkoutExercise(item.copy(position = index, updatedAtMillis = now))
            }
        }
    }

    override suspend fun addSet(workoutExerciseId: Long, draft: WorkoutSetDraft?): Long =
        database.withTransaction {
            val workoutExercise = dao.getWorkoutExercise(workoutExerciseId)
                ?: error("Workout exercise no longer exists")
            val previous = dao.getWorkoutSets(workoutExerciseId)
                .lastOrNull { it.deletedAtMillis == null }
                ?: dao.getLatestCompletedSet(
                    workoutExercise.exerciseId,
                    workoutExercise.sessionId,
                    workoutExercise.machineProfileUuidSnapshot,
                )
            val effectiveDraft = draft ?: previous?.toDraft()?.copy(
                planned = false,
                completed = false,
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
            val now = nowMillis()
            dao.insertWorkoutSet(
                effectiveDraft.toEntity(
                    uuid = ids.nextId(),
                    workoutExerciseId = workoutExerciseId,
                    position = dao.nextSetPosition(workoutExerciseId),
                    createdAtMillis = now,
                    completedAtMillis = now.takeIf { effectiveDraft.completed },
                    workoutExercise = workoutExercise,
                ),
            )
        }

    override suspend fun updateSet(id: Long, draft: WorkoutSetDraft) {
        val existing = dao.getWorkoutSet(id) ?: return
        val workoutExercise = dao.getWorkoutExercise(existing.workoutExerciseId)
            ?: error("Workout exercise no longer exists")
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
        dao.updateWorkoutSet(
            draft.toEntity(
                id = existing.id,
                uuid = existing.uuid,
                workoutExerciseId = existing.workoutExerciseId,
                position = existing.position,
                deletedAtMillis = existing.deletedAtMillis,
                createdAtMillis = existing.createdAtMillis,
                updatedAtMillis = nowMillis(),
                completedAtMillis = when {
                    draft.completed && existing.completedAtMillis == null -> nowMillis()
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
            ),
        )
    }

    override suspend fun setSetCompleted(
        id: Long,
        completed: Boolean,
        autoStartRest: Boolean,
        restOverrideSeconds: Int?,
    ) =
        database.withTransaction {
            val set = dao.getWorkoutSet(id) ?: return@withTransaction
            if (completed) {
                val workoutExercise = dao.getWorkoutExercise(set.workoutExerciseId)
                    ?: error("Workout exercise no longer exists")
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
                val workoutExercise = dao.getWorkoutExercise(set.workoutExerciseId)
                val exercise = workoutExercise?.let { dao.getExercise(it.exerciseId) }
                val seconds = restOverrideSeconds ?: set.restSeconds ?: exercise?.defaultRestSeconds
                    ?: settingsRepository?.current()?.defaultRestSeconds ?: 120
                if (workoutExercise != null && seconds > 0) {
                    startRestTimer(workoutExercise.sessionId, seconds)
                }
            }
        }

    override suspend fun duplicateSet(id: Long): Long = database.withTransaction {
        val source = dao.getWorkoutSet(id) ?: error("Set no longer exists")
        val now = nowMillis()
        dao.insertWorkoutSet(
            source.copy(
                id = 0,
                uuid = ids.nextId(),
                position = dao.nextSetPosition(source.workoutExerciseId),
                completed = false,
                completedAtMillis = null,
                deletedAtMillis = null,
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
        )
    }

    override suspend fun deleteSet(id: Long) {
        val set = dao.getWorkoutSet(id) ?: return
        dao.updateWorkoutSet(set.copy(deletedAtMillis = nowMillis(), updatedAtMillis = nowMillis()))
    }

    override suspend fun undoDeleteSet(id: Long) {
        val set = dao.getWorkoutSet(id) ?: return
        dao.updateWorkoutSet(set.copy(deletedAtMillis = null, updatedAtMillis = nowMillis()))
    }

    override suspend fun reorderSets(workoutExerciseId: Long, idsInOrder: List<Long>) =
        database.withTransaction {
            idsInOrder.forEachIndexed { index, id ->
                dao.getWorkoutSet(id)
                    ?.takeIf { it.workoutExerciseId == workoutExerciseId }
                    ?.let { dao.updateWorkoutSet(it.copy(position = index, updatedAtMillis = nowMillis())) }
            }
        }

    override suspend fun startRestTimer(sessionId: Long, seconds: Int) {
        require(seconds > 0) { "Timer duration must be positive" }
        val session = dao.getSession(sessionId) ?: return
        val now = nowMillis()
        dao.updateSession(
            session.copy(
                restTimerDurationSeconds = seconds,
                restTimerDeadlineMillis = now + seconds * 1_000L,
                updatedAtMillis = now,
            ),
        )
    }

    override suspend fun adjustRestTimer(sessionId: Long, deltaSeconds: Int) {
        val session = dao.getSession(sessionId) ?: return
        val now = nowMillis()
        val deadline = session.restTimerDeadlineMillis ?: now
        val adjusted = (deadline + deltaSeconds * 1_000L).coerceAtLeast(now)
        dao.updateSession(
            session.copy(
                restTimerDeadlineMillis = adjusted.takeIf { it > now },
                restTimerDurationSeconds = ((adjusted - now) / 1_000L).toInt().takeIf { it > 0 },
                updatedAtMillis = now,
            ),
        )
    }

    override suspend fun stopRestTimer(sessionId: Long) {
        val session = dao.getSession(sessionId) ?: return
        dao.updateSession(
            session.copy(
                restTimerDeadlineMillis = null,
                restTimerDurationSeconds = null,
                updatedAtMillis = nowMillis(),
            ),
        )
    }

    private fun nowMillis() = clock.now().toEpochMilli()
}

private fun validateExercise(draft: ExerciseDraft) {
    require(draft.name.isNotBlank()) { "Exercise name is required" }
    require(draft.weightIncrement.isFinite() && draft.weightIncrement > 0.0) { "Weight increment must be positive" }
    require(draft.repetitionIncrement > 0) { "Repetition increment must be positive" }
    require(draft.effectiveBodyweightPercent in 0.0..200.0) {
        "Effective bodyweight percentage must be between 0 and 200"
    }
    require(draft.barWeightKg == null || draft.barWeightKg.isFinite() && draft.barWeightKg >= 0.0) {
        "Bar or base load must be a non-negative number"
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
