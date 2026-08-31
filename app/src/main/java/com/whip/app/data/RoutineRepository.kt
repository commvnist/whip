package com.whip.app.data

import androidx.room.withTransaction
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.core.SettingsRepository
import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.Exercise
import com.whip.app.domain.GymRoutine
import com.whip.app.domain.MachineLoadType
import com.whip.app.domain.MachineLevelDirection
import com.whip.app.domain.LoadInterpretation
import com.whip.app.domain.MachineStackMode
import com.whip.app.domain.canonicalResistanceKg
import com.whip.app.domain.loadInterpretationMultiplier
import com.whip.app.domain.massFromKilograms
import com.whip.app.domain.massToKilograms
import com.whip.app.domain.GraphPreset
import com.whip.app.domain.PersonalRecord
import com.whip.app.domain.PersonalRecordType
import com.whip.app.domain.RoutineDay
import com.whip.app.domain.RoutineDayDraft
import com.whip.app.domain.RoutineDraft
import com.whip.app.domain.RoutineExercise
import com.whip.app.domain.RoutineExerciseDraft
import com.whip.app.domain.RoutineEquipmentBindingState
import com.whip.app.domain.RoutineLoadPrescriptionType
import com.whip.app.domain.RoutineProgramDraft
import com.whip.app.domain.RoutineProgramKind
import com.whip.app.domain.RoutineSet
import com.whip.app.domain.RoutineTrainingMaxSource
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.domain.WorkoutSet
import com.whip.app.domain.WorkoutSetClassification
import com.whip.app.domain.WorkoutSetDraft
import com.whip.app.domain.estimatedOneRepMaxKg
import com.whip.app.domain.effectiveLoadKg
import com.whip.app.domain.paceSecondsPerKilometre
import com.whip.app.domain.speedMetresPerSecond
import com.whip.app.domain.volumeKg
import java.time.ZoneId
import kotlin.math.round
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface RoutineRepository {
    val routines: Flow<List<GymRoutine>>
    val days: Flow<List<RoutineDay>>
    val exercises: Flow<List<RoutineExercise>>
    val sets: Flow<List<RoutineSet>>
    val personalRecords: Flow<List<PersonalRecord>>
    val graphPresets: Flow<List<GraphPreset>>

    suspend fun createRoutine(draft: RoutineDraft): Long
    suspend fun updateRoutine(id: Long, draft: RoutineDraft)
    suspend fun duplicateRoutine(id: Long): Long
    suspend fun setRoutineArchived(id: Long, archived: Boolean)
    suspend fun setRoutinePinned(id: Long, pinned: Boolean)
    suspend fun reorderRoutines(ids: List<Long>)
    suspend fun setRoutineProgramPosition(routineId: Long, phaseIndex: Int, dayPosition: Int, cycle: Int)
    suspend fun resetRoutineProgramProgress(routineId: Long)
    suspend fun startRoutine(routineId: Long, dayId: Long? = null): Long
    suspend fun saveWorkoutAsRoutine(sessionId: Long, name: String): Long
    suspend fun rebuildPersonalRecords(exerciseId: Long)
    suspend fun saveGraphPreset(
        name: String,
        exerciseIds: List<Long>,
        metric: String,
        dateRange: String,
        aggregation: String,
    ): Long
    suspend fun updateGraphPreset(
        id: Long,
        name: String,
        exerciseIds: List<Long>,
        metric: String,
        dateRange: String,
        aggregation: String,
    )
    suspend fun deleteGraphPreset(id: Long)
}

class RoomRoutineRepository(
    private val database: WhipDatabase,
    private val clock: WhipClock,
    private val ids: WhipIdGenerator,
    private val settingsRepository: SettingsRepository? = null,
) : RoutineRepository {
    private val dao = database.routineDao()
    private val gymDao = database.gymDao()

    override val routines = dao.observeRoutines().map { it.map(GymRoutineEntity::toDomain) }
    override val days = dao.observeDays().map { it.map(RoutineDayEntity::toDomain) }
    override val exercises = dao.observeExercises().map { it.map(RoutineExerciseEntity::toDomain) }
    override val sets = dao.observeSets().map { it.map(RoutineSetEntity::toDomain) }
    override val personalRecords = dao.observePersonalRecords().map { it.map(PersonalRecordEntity::toDomain) }
    override val graphPresets = dao.observeGraphPresets().map { it.map(GraphPresetEntity::toDomain) }

    override suspend fun createRoutine(draft: RoutineDraft): Long = database.withTransaction {
        validateRoutine(draft)
        val now = clock.now().toEpochMilli()
        val program = draft.program.normalizedForCreate()
        val routineId = dao.insertRoutine(
            GymRoutineEntity(
                uuid = ids.nextId(),
                name = draft.name.trim(),
                notes = draft.notes.trim(),
                position = dao.nextRoutinePosition(),
                archived = false,
                pinned = false,
                createdAtMillis = now,
                updatedAtMillis = now,
                programKind = program.kind.name,
                programPhaseCount = program.phaseCount,
                programPhaseLabelsCsv = program.phaseLabels.joinToString(","),
            ),
        )
        insertRoutineChildren(routineId, draft.days, now)
        routineId
    }

    override suspend fun updateRoutine(id: Long, draft: RoutineDraft) = database.withTransaction {
        require(gymDao.getActiveSession()?.sourceRoutineId != id) {
            "Finish or discard the active workout before editing this routine"
        }
        validateRoutine(draft)
        val existing = dao.getRoutine(id) ?: error("Routine no longer exists")
        val existingDays = dao.getDays(id)
        val existingExercises = existingDays.mapIndexed { dayIndex, day ->
            dao.getExercises(day.id).mapIndexed { exerciseIndex, exercise ->
                (dayIndex to exerciseIndex) to exercise
            }
        }.flatten().toMap()
        val now = clock.now().toEpochMilli()
        val requestedProgram = draft.program?.normalized()
        val programChanged = requestedProgram != null && (
            existing.programKind != requestedProgram.kind.name ||
                existing.programPhaseCount != requestedProgram.phaseCount ||
                existing.programPhaseLabelsCsv != requestedProgram.phaseLabels.joinToString(",")
            )
        dao.updateRoutine(
            existing.copy(
                name = draft.name.trim(),
                notes = draft.notes.trim(),
                updatedAtMillis = now,
                programKind = requestedProgram?.kind?.name ?: existing.programKind,
                programPhaseCount = requestedProgram?.phaseCount ?: existing.programPhaseCount,
                programPhaseLabelsCsv = requestedProgram?.phaseLabels?.joinToString(",") ?: existing.programPhaseLabelsCsv,
                currentProgramPhaseIndex = if (programChanged) 0 else existing.currentProgramPhaseIndex,
                currentProgramCycle = if (programChanged) 1 else existing.currentProgramCycle,
                nextProgramDayPosition = if (programChanged) 0 else existing.nextProgramDayPosition.coerceIn(0, draft.days.lastIndex),
            ),
        )
        dao.deleteDays(id)
        insertRoutineChildren(id, draft.days, now, existingDays, existingExercises)
    }

    override suspend fun duplicateRoutine(id: Long): Long {
        val routine = dao.getRoutine(id)?.toDomain() ?: error("Routine no longer exists")
        val source = loadDraft(id)
        return createRoutine(
            source.copy(
                name = "${routine.name} copy",
                days = source.days.map { it.copy(progressionIndex = 0) },
            ),
        )
    }

    override suspend fun setRoutineArchived(id: Long, archived: Boolean) {
        val routine = dao.getRoutine(id) ?: return
        dao.updateRoutine(routine.copy(archived = archived, updatedAtMillis = clock.now().toEpochMilli()))
    }

    override suspend fun setRoutinePinned(id: Long, pinned: Boolean) {
        val routine = dao.getRoutine(id) ?: return
        dao.updateRoutine(routine.copy(pinned = pinned, updatedAtMillis = clock.now().toEpochMilli()))
    }

    override suspend fun reorderRoutines(ids: List<Long>) = database.withTransaction {
        val requested = ids.distinct()
        val all = dao.getAllRoutines()
        require(requested.all { id -> all.any { it.id == id } }) { "Routine no longer exists" }
        val byId = all.associateBy(GymRoutineEntity::id)
        val order = requested + all.filterNot { it.id in requested }.sortedBy(GymRoutineEntity::position).map(GymRoutineEntity::id)
        val now = clock.now().toEpochMilli()
        order.forEachIndexed { index, id ->
            val routine = requireNotNull(byId[id])
            if (routine.position != index) dao.updateRoutine(routine.copy(position = index, updatedAtMillis = now))
        }
    }

    override suspend fun setRoutineProgramPosition(
        routineId: Long,
        phaseIndex: Int,
        dayPosition: Int,
        cycle: Int,
    ) = database.withTransaction {
        val routine = dao.getRoutine(routineId) ?: error("Routine no longer exists")
        require(routine.programKind != RoutineProgramKind.Static.name) { "Static routines do not have a program position" }
        val days = dao.getDays(routineId)
        require(phaseIndex in 0 until routine.programPhaseCount) { "Program phase is out of range" }
        require(dayPosition in days.indices) { "Program day is out of range" }
        require(cycle >= 1) { "Program cycle must be at least 1" }
        dao.updateRoutine(
            routine.copy(
                currentProgramPhaseIndex = phaseIndex,
                currentProgramCycle = cycle,
                nextProgramDayPosition = dayPosition,
                updatedAtMillis = clock.now().toEpochMilli(),
            ),
        )
    }

    override suspend fun resetRoutineProgramProgress(routineId: Long) = database.withTransaction {
        val routine = dao.getRoutine(routineId) ?: return@withTransaction
        val now = clock.now().toEpochMilli()
        dao.updateRoutine(
            routine.copy(
                currentProgramPhaseIndex = 0,
                currentProgramCycle = 1,
                nextProgramDayPosition = 0,
                updatedAtMillis = now,
            ),
        )
        dao.getDays(routineId).forEach { day ->
            if (day.progressionIndex != 0) dao.updateDay(day.copy(progressionIndex = 0, updatedAtMillis = now))
        }
    }

    override suspend fun startRoutine(routineId: Long, dayId: Long?): Long = database.withTransaction {
        require(gymDao.getActiveSession() == null) { "Finish or discard the active workout first" }
        val routine = dao.getRoutine(routineId) ?: error("Routine no longer exists")
        val days = dao.getDays(routineId)
        val programKind = runCatching { RoutineProgramKind.valueOf(routine.programKind) }
            .getOrDefault(RoutineProgramKind.Static)
        val programmed = programKind != RoutineProgramKind.Static
        val selectedDay = dayId?.let { selected -> days.firstOrNull { it.id == selected } }
            ?: days.getOrNull(if (programmed) routine.nextProgramDayPosition else 0)
            ?: error("Routine has no days")
        val routineExercises = dao.getExercises(selectedDay.id)
        val personalRecords = dao.getAllPersonalRecords()
        val machinesByRoutineExerciseId = routineExercises.associate { routineExercise ->
            val binding = runCatching {
                RoutineEquipmentBindingState.valueOf(routineExercise.equipmentBindingState)
            }.getOrDefault(if (routineExercise.machineId == null) RoutineEquipmentBindingState.None else RoutineEquipmentBindingState.Resolved)
            require(binding != RoutineEquipmentBindingState.NeedsEquipment) {
                "${routineExercise.machineNameSnapshot.ifBlank { "Routine equipment" }} needs a replacement before this routine can start"
            }
            val machine = routineExercise.machineId?.let { machineId ->
                requireNotNull(gymDao.getMachine(machineId)) {
                    "${routineExercise.machineNameSnapshot.ifBlank { "Routine machine" }} needs a replacement before this routine can start"
                }.also {
                    require(gymDao.machineSupportsExercise(it.id, routineExercise.exerciseId)) {
                        "Routine machine is not linked to this exercise"
                    }
                }
            }
            routineExercise.id to machine
        }
        val now = clock.now().toEpochMilli()
        val sessionId = gymDao.insertSession(
            WorkoutSessionEntity(
                uuid = ids.nextId(),
                name = if (days.size > 1) "${routine.name} · ${selectedDay.name}" else routine.name,
                notes = routine.notes,
                startedAtMillis = now,
                endedAtMillis = null,
                localEpochDay = clock.today().toEpochDay(),
                zoneId = clock.zoneId().id,
                state = WorkoutSessionState.Active.name,
                keepScreenAwake = settingsRepository?.current()?.keepScreenAwake ?: false,
                restTimerDeadlineMillis = null,
                restTimerDurationSeconds = null,
                archived = false,
                createdAtMillis = now,
                updatedAtMillis = now,
                sourceRoutineId = routineId,
                sourceRoutineDayId = selectedDay.id,
                sourceRoutineProgramKind = programKind.name,
                sourceRoutinePhaseIndex = routine.currentProgramPhaseIndex.takeIf { programmed },
                sourceRoutineCycle = routine.currentProgramCycle.takeIf { programmed },
                sourceRoutineDayPosition = selectedDay.position,
                sourceRoutineDayProgressionIndex = selectedDay.progressionIndex,
            ),
        )
        val validGroupKeys = routineExercises.mapNotNull { it.groupKey }
            .groupingBy { it }
            .eachCount()
            .filterValues { count -> count >= 2 }
            .keys
        val groupIds = mutableMapOf<String, Long>()
        routineExercises.forEach { routineExercise ->
            val sourceExercise = requireNotNull(gymDao.getExercise(routineExercise.exerciseId)) {
                "Routine exercise no longer exists"
            }
            val groupId = routineExercise.groupKey?.takeIf { it in validGroupKeys }?.let { key ->
                groupIds[key] ?: gymDao.insertWorkoutGroup(
                        WorkoutGroupEntity(
                            uuid = ids.nextId(), sessionId = sessionId, name = key,
                            type = "Superset", position = groupIds.size,
                            createdAtMillis = now, updatedAtMillis = now,
                        ),
                    ).also { groupIds[key] = it }
            }
            // Archived profiles remain valid for existing routines. Archive hides a profile
            // from new assignments; permanent deletion is what makes a binding unresolved.
            val machine = machinesByRoutineExerciseId[routineExercise.id]
            val workoutExerciseEntity = WorkoutExerciseEntity(
                    uuid = ids.nextId(), sessionId = sessionId,
                    exerciseId = routineExercise.exerciseId, position = routineExercise.position,
                    notes = routineExercise.notes, groupId = groupId,
                    createdAtMillis = now, updatedAtMillis = now,
                    machineProfileUuidSnapshot = machine?.uuid,
                    machineId = machine?.id,
                    machineNameSnapshot = machine?.let { if (it.location.isBlank()) it.name else "${it.name} · ${it.location}" }.orEmpty(),
                    machineLoadTypeSnapshot = machine?.loadType.orEmpty(),
                    machineUnitIdSnapshot = machine?.unitId.orEmpty(),
                    machineLevelLabelSnapshot = machine?.levelLabel.orEmpty(),
                    loadInterpretationSnapshot = machine?.loadInterpretation ?: sourceExercise.loadInterpretation,
                    baseLoadKgSnapshot = machine?.baseLoadKg
                        ?: sourceExercise.barWeightKg.takeIf {
                            (machine?.loadInterpretation ?: sourceExercise.loadInterpretation) == LoadInterpretation.PerSide.name
                        },
                    trackingTypeSnapshot = sourceExercise.trackingType,
                    bodyweightLoadPolicySnapshot = sourceExercise.bodyweightLoadPolicy,
                    effectiveBodyweightPercentSnapshot = sourceExercise.effectiveBodyweightPercent,
                    oneRepMaxFormulaSnapshot = sourceExercise.oneRepMaxFormula,
                    includeInVolumeSnapshot = sourceExercise.includeInVolume,
                    includeInPersonalRecordsSnapshot = sourceExercise.includeInPersonalRecords,
                    exerciseWeightUnitSnapshot = sourceExercise.weightUnitId,
                    loadMultiplierSnapshot = loadInterpretationMultiplier(
                        runCatching { LoadInterpretation.valueOf(machine?.loadInterpretation ?: sourceExercise.loadInterpretation) }
                            .getOrDefault(LoadInterpretation.Total),
                        machine?.stackMode?.let { runCatching { MachineStackMode.valueOf(it) }.getOrNull() } ?: MachineStackMode.Single,
                        machine?.pulleyRatio ?: 1.0,
                    ),
                    machineConfigurationGroupSnapshot = machine?.configurationGroupId.orEmpty(),
                    machineConfigurationVersionSnapshot = machine?.configurationVersion ?: 1,
                    machineConfigurationSnapshot = machine?.let { selected ->
                        listOfNotNull(
                            selected.seatPosition.takeIf(String::isNotBlank)?.let { "Seat $it" },
                            selected.backPosition.takeIf(String::isNotBlank)?.let { "Back $it" },
                            selected.attachment.takeIf(String::isNotBlank),
                        ).joinToString(" · ")
                    }.orEmpty(),
                    machinePulleyRatioSnapshot = machine?.pulleyRatio ?: 1.0,
                    machineStackModeSnapshot = machine?.stackMode ?: MachineStackMode.Single.name,
                    machineAddOnPlateKgSnapshot = machine?.addOnPlateKg,
                    machineMassMappingCsvSnapshot = machine?.massMappingCsv.orEmpty(),
                    alternativeExerciseIdsCsvSnapshot = routineExercise.alternativeExerciseIdsCsv,
                    trainingMaxKgSnapshot = routineExercise.trainingMaxKg,
                    trainingMaxValueSnapshot = routineExercise.trainingMaxValue,
                    trainingMaxUnitIdSnapshot = routineExercise.trainingMaxUnitId,
                    cycleIncrementValueSnapshot = routineExercise.cycleIncrementValue,
                    trainingMaxSourceSnapshot = routineExercise.trainingMaxSource,
                )
            val workoutExerciseId = gymDao.insertWorkoutExercise(workoutExerciseEntity)
            val allPlanned = dao.getSets(routineExercise.id)
            val planned = if (programmed) {
                allPlanned.filter { it.routinePhaseIndex == null || it.routinePhaseIndex == routine.currentProgramPhaseIndex }
            } else {
                allPlanned.filter { it.routinePhaseIndex == null }
            }
            if (planned.isNotEmpty()) {
                val oneRepMaxKg = personalRecords.asSequence()
                    .filter { it.exerciseId == routineExercise.exerciseId && it.current && it.type == PersonalRecordType.EstimatedOneRepMax.name }
                    .filter { it.machineProfileUuidSnapshot == machine?.uuid }
                    .maxOfOrNull(PersonalRecordEntity::value)
                val progression = routineExercise.progressionPercentagesCsv.parseDoubleCsv()
                    .takeIf(List<Double>::isNotEmpty)
                    ?.let { if (programmed) 100.0 else it[selectedDay.progressionIndex % it.size] }
                    ?: 100.0
                planned.forEach { template ->
                    gymDao.insertWorkoutSet(
                        template.toWorkoutSet(
                            ids.nextId(), workoutExerciseId, now, workoutExerciseEntity,
                            sourceExercise = sourceExercise,
                            machine = machine,
                            oneRepMaxKg = oneRepMaxKg,
                            trainingMaxPercent = routineExercise.trainingMaxPercent,
                            progressionPercent = progression,
                            explicitTrainingMaxKg = routineExercise.trainingMaxKg,
                        ),
                    )
                }
            } else if (routineExercise.copyPreviousWorkout) {
                gymDao.getLatestCompletedSet(routineExercise.exerciseId, sessionId, machine?.uuid)?.let { previous ->
                    gymDao.insertWorkoutSet(
                        previous.copy(
                            id = 0, uuid = ids.nextId(), workoutExerciseId = workoutExerciseId,
                            position = 0, planned = true, completed = false,
                            completedAtMillis = null, deletedAtMillis = null,
                            createdAtMillis = now, updatedAtMillis = now,
                            prescribedCanonicalWeightKg = previous.canonicalWeightKg,
                            prescribedEnteredWeight = previous.enteredWeight,
                            prescribedWeightUnitId = previous.enteredWeightUnitId,
                            prescribedRepetitions = previous.repetitions,
                            prescribedRpe = previous.rpe,
                            prescribedRir = previous.rir,
                            prescribedDurationSeconds = previous.durationSeconds,
                            prescribedMachineLoadValue = previous.machineLoadValue,
                        ),
                    )
                }
            }
        }
        sessionId
    }

    override suspend fun saveWorkoutAsRoutine(sessionId: Long, name: String): Long {
        val session = gymDao.getSession(sessionId) ?: error("Workout no longer exists")
        val exerciseDrafts = gymDao.getWorkoutExercises(sessionId).map { workoutExercise ->
            RoutineExerciseDraft(
                exerciseId = workoutExercise.exerciseId,
                notes = workoutExercise.notes,
                groupKey = workoutExercise.groupId?.let { "Group $it" },
                plannedSets = gymDao.getWorkoutSets(workoutExercise.id)
                    .filter { it.deletedAtMillis == null }
                    .map(WorkoutSetEntity::toDraft),
                machineId = workoutExercise.machineId,
                equipmentBindingState = if (workoutExercise.machineProfileUuidSnapshot == null) {
                    RoutineEquipmentBindingState.None
                } else if (workoutExercise.machineId == null) {
                    RoutineEquipmentBindingState.NeedsEquipment
                } else {
                    RoutineEquipmentBindingState.Resolved
                },
                machineProfileUuidSnapshot = workoutExercise.machineProfileUuidSnapshot,
                machineNameSnapshot = workoutExercise.machineNameSnapshot,
                machineLoadTypeSnapshot = workoutExercise.machineLoadTypeSnapshot.takeIf(String::isNotBlank)
                    ?.let(MachineLoadType::valueOf),
                machineUnitIdSnapshot = workoutExercise.machineUnitIdSnapshot,
                machineLevelLabelSnapshot = workoutExercise.machineLevelLabelSnapshot,
                machineLoadInterpretationSnapshot = runCatching {
                    LoadInterpretation.valueOf(workoutExercise.loadInterpretationSnapshot)
                }.getOrDefault(LoadInterpretation.Total),
                machineConfigurationGroupSnapshot = workoutExercise.machineConfigurationGroupSnapshot,
                machineConfigurationVersionSnapshot = workoutExercise.machineConfigurationVersionSnapshot,
                machineConfigurationSnapshot = workoutExercise.machineConfigurationSnapshot,
            )
        }
        return createRoutine(
            RoutineDraft(
                name = name.ifBlank { session.name.ifBlank { "Saved workout" } },
                notes = session.notes,
                days = listOf(RoutineDayDraft("Day A", exerciseDrafts)),
            ),
        )
    }

    override suspend fun rebuildPersonalRecords(exerciseId: Long) = database.withTransaction {
        val exercise = gymDao.getExercise(exerciseId)?.toDomainForRecords() ?: return@withTransaction
        val sets = dao.getCompletedSetsForExercise(exerciseId)
        dao.deleteRecords(exerciseId)
        val best = mutableMapOf<String, Double>()
        val records = mutableListOf<PersonalRecordEntity>()
        val sourceSessionBySetId = mutableMapOf<Long, Long?>()
        val sourceMachineBySetId = mutableMapOf<Long, Long?>()
        val sourceMachineScopeBySetId = mutableMapOf<Long, String?>()
        val policyExerciseBySetId = mutableMapOf<Long, Exercise>()

        fun consider(
            set: WorkoutSetEntity,
            sourceSessionId: Long?,
            machineId: Long?,
            machineProfileUuidSnapshot: String?,
            type: PersonalRecordType,
            value: Double?,
            unit: String,
            secondary: Double? = null,
            lowerIsBetter: Boolean = false,
        ) {
            if (value == null || !value.isFinite()) return
            val key = "${machineProfileUuidSnapshot ?: "none"}:${type.name}:${secondary ?: ""}"
            val previous = best[key]
            val improves = previous == null || if (lowerIsBetter) value < previous else value > previous
            if (!improves) return
            best[key] = value
            records += PersonalRecordEntity(
                uuid = "pr:${exercise.uuid}:${machineProfileUuidSnapshot ?: "none"}:${type.name}:${secondary ?: ""}:${set.uuid}",
                exerciseId = exerciseId,
                type = type.name,
                value = value,
                secondaryValue = secondary,
                unitId = unit,
                sourceSetId = set.id,
                sourceSessionId = sourceSessionId,
                achievedAtMillis = set.completedAtMillis ?: set.updatedAtMillis,
                current = false,
                imported = false,
                createdAtMillis = set.updatedAtMillis,
                updatedAtMillis = clock.now().toEpochMilli(),
                machineId = machineId,
                machineProfileUuidSnapshot = machineProfileUuidSnapshot,
            )
        }

        sets.forEach { entity ->
            val set = entity.toDomainForRecords()
            val sourceWorkoutExercise = gymDao.getWorkoutExercise(entity.workoutExerciseId)
            val sourceSessionId = sourceWorkoutExercise?.sessionId
            val machineId = sourceWorkoutExercise?.machineId
            val machineScope = sourceWorkoutExercise?.machineProfileUuidSnapshot
            sourceSessionBySetId[entity.id] = sourceSessionId
            sourceMachineBySetId[entity.id] = machineId
            sourceMachineScopeBySetId[entity.id] = machineScope
            val policyExercise = sourceWorkoutExercise?.applyPolicySnapshot(exercise) ?: exercise
            policyExerciseBySetId[entity.id] = policyExercise
            val settings = settingsRepository?.current()
            val includeWarmups = settings?.includeWarmupsInGymStats == true
            val assistedAllowed = settings?.includeAssistedInPersonalRecords == true ||
                policyExercise.trackingType != com.whip.app.domain.ExerciseTrackingType.AssistedBodyweightReps
            consider(entity, sourceSessionId, machineId, machineScope, PersonalRecordType.MaxWeight, set.effectiveLoadKg(policyExercise).takeIf { assistedAllowed }, "kilogram")
            consider(entity, sourceSessionId, machineId, machineScope, PersonalRecordType.MaxRepetitions, set.repetitions?.toDouble().takeIf { assistedAllowed }, "count")
            consider(
                entity,
                sourceSessionId,
                machineId,
                machineScope,
                PersonalRecordType.MaxRepetitionsForWeight,
                set.repetitions?.toDouble().takeIf { assistedAllowed },
                "count",
                set.effectiveLoadKg(policyExercise).takeIf { assistedAllowed },
            )
            consider(
                entity,
                sourceSessionId,
                machineId,
                machineScope,
                PersonalRecordType.BestWeightForRepCount,
                set.effectiveLoadKg(policyExercise),
                "kilogram",
                set.repetitions?.toDouble(),
            )
            consider(
                entity,
                sourceSessionId,
                machineId,
                machineScope,
                PersonalRecordType.EstimatedOneRepMax,
                set.estimatedOneRepMaxKg(
                    policyExercise,
                    settings?.oneRepMaxRepCutoff ?: 10,
                    includeWarmups,
                    settings?.adjustE1rmForEffort == true,
                ).takeIf { assistedAllowed },
                "kilogram",
            )
            consider(entity, sourceSessionId, machineId, machineScope, PersonalRecordType.SetVolume, set.volumeKg(policyExercise, includeWarmups).takeIf { assistedAllowed }, "kilogram")
            consider(entity, sourceSessionId, machineId, machineScope, PersonalRecordType.MaxDistance, set.canonicalDistanceMetres, "distance_m")
            consider(entity, sourceSessionId, machineId, machineScope, PersonalRecordType.MaxDuration, set.durationSeconds?.toDouble(), "second")
            consider(entity, sourceSessionId, machineId, machineScope, PersonalRecordType.MaxSpeed, set.speedMetresPerSecond(), "distance_m/second")
            consider(entity, sourceSessionId, machineId, machineScope, PersonalRecordType.MinPace, set.paceSecondsPerKilometre(), "second/kilometre", lowerIsBetter = true)
            if (sourceWorkoutExercise?.machineLoadTypeSnapshot == MachineLoadType.Level.name) {
                val lowerSettingIsStronger = machineId != null &&
                    gymDao.getMachine(machineId)?.levelDirection ==
                    MachineLevelDirection.HigherNumberLessResistance.name
                consider(
                    entity,
                    sourceSessionId,
                    machineId,
                    machineScope,
                    PersonalRecordType.MaxMachineSetting,
                    set.machineLoadValue,
                    sourceWorkoutExercise.machineLevelLabelSnapshot.ifBlank { "level" },
                    lowerIsBetter = lowerSettingIsStronger,
                )
            }
        }
        sets.groupBy { Triple(sourceSessionBySetId[it.id], sourceMachineBySetId[it.id], sourceMachineScopeBySetId[it.id]) }.forEach { (scope, sessionSets) ->
            val (sessionId, machineId, machineScope) = scope
            if (sessionId == null || sessionSets.isEmpty()) return@forEach
            val settings = settingsRepository?.current()
            val includeWarmups = settings?.includeWarmupsInGymStats == true
            val volume = sessionSets.sumOf { entity ->
                entity.toDomainForRecords().volumeKg(policyExerciseBySetId[entity.id] ?: exercise, includeWarmups)
            }
            val representative = sessionSets.maxBy { it.completedAtMillis ?: it.updatedAtMillis }
            consider(
                representative,
                sessionId,
                machineId,
                machineScope,
                PersonalRecordType.ExerciseWorkoutVolume,
                volume,
                "kilogram",
            )
        }
        val latestByKey = records.groupBy { "${it.machineProfileUuidSnapshot ?: "none"}:${it.type}:${it.secondaryValue ?: ""}" }
            .mapValues { (_, values) -> values.last().uuid }
        records.forEach { record ->
            dao.upsertPersonalRecord(
                record.copy(current = latestByKey["${record.machineProfileUuidSnapshot ?: "none"}:${record.type}:${record.secondaryValue ?: ""}"] == record.uuid),
            )
        }
    }

    override suspend fun saveGraphPreset(
        name: String,
        exerciseIds: List<Long>,
        metric: String,
        dateRange: String,
        aggregation: String,
    ): Long {
        require(name.isNotBlank()) { "Preset name is required" }
        val now = clock.now().toEpochMilli()
        val entity = GraphPresetEntity(
            uuid = ids.nextId(), name = name.trim(), exerciseIdsCsv = exerciseIds.joinToString(","),
            metric = metric, dateRange = dateRange, aggregation = aggregation,
            archived = false, createdAtMillis = now, updatedAtMillis = now,
        )
        return dao.insertGraphPreset(entity)
    }

    override suspend fun updateGraphPreset(
        id: Long,
        name: String,
        exerciseIds: List<Long>,
        metric: String,
        dateRange: String,
        aggregation: String,
    ) {
        require(name.isNotBlank()) { "Preset name is required" }
        val existing = dao.getGraphPresets().firstOrNull { it.id == id }
            ?: error("Graph preset no longer exists")
        dao.updateGraphPreset(
            existing.copy(
                name = name.trim(),
                exerciseIdsCsv = exerciseIds.distinct().joinToString(","),
                metric = metric,
                dateRange = dateRange,
                aggregation = aggregation,
                updatedAtMillis = clock.now().toEpochMilli(),
            ),
        )
    }

    override suspend fun deleteGraphPreset(id: Long) {
        dao.deleteGraphPreset(id)
    }

    private suspend fun insertRoutineChildren(
        routineId: Long,
        days: List<RoutineDayDraft>,
        now: Long,
        preservedDays: List<RoutineDayEntity> = emptyList(),
        preservedExercises: Map<Pair<Int, Int>, RoutineExerciseEntity> = emptyMap(),
    ) {
        days.forEachIndexed { dayIndex, day ->
            val preservedDay = preservedDays.getOrNull(dayIndex)
            val dayId = dao.insertDay(
                RoutineDayEntity(
                    uuid = ids.nextId(), routineId = routineId, name = day.name.trim(),
                    position = dayIndex, createdAtMillis = now, updatedAtMillis = now,
                    progressionIndex = day.progressionIndex ?: preservedDay?.progressionIndex ?: 0,
                ),
            )
            day.exercises.forEachIndexed { exerciseIndex, exercise ->
                val preservedExercise = preservedExercises[dayIndex to exerciseIndex]
                    ?.takeIf { it.exerciseId == exercise.exerciseId }
                requireNotNull(gymDao.getExercise(exercise.exerciseId)) { "Exercise no longer exists" }
                require(exercise.alternativeExerciseIds.none { it == exercise.exerciseId }) { "An exercise cannot replace itself" }
                exercise.alternativeExerciseIds.distinct().forEach { alternativeId ->
                    requireNotNull(gymDao.getExercise(alternativeId)) { "A planned alternative no longer exists" }
                }
                val machine = exercise.machineId?.let { machineId ->
                    requireNotNull(gymDao.getMachine(machineId)) { "Machine no longer exists" }
                        .also {
                            require(gymDao.machineSupportsExercise(it.id, exercise.exerciseId)) {
                                "Machine is unavailable for this exercise"
                            }
                            require(isCompatibleReplacement(exercise, it)) {
                                "Replacement equipment must use the same scale, unit, and load interpretation"
                            }
                        }
                }
                val bindingState = when {
                    machine != null -> RoutineEquipmentBindingState.Resolved
                    exercise.equipmentBindingState == RoutineEquipmentBindingState.NeedsEquipment -> RoutineEquipmentBindingState.NeedsEquipment
                    else -> RoutineEquipmentBindingState.None
                }
                val explicitTrainingMaxValue = exercise.trainingMaxValue ?: preservedExercise?.trainingMaxValue
                val explicitTrainingMaxUnit = when {
                    exercise.trainingMaxValue != null -> exercise.trainingMaxUnitId
                    preservedExercise?.trainingMaxValue != null -> preservedExercise.trainingMaxUnitId
                    else -> exercise.trainingMaxUnitId
                }
                val explicitTrainingMaxKg = when {
                    exercise.trainingMaxValue != null -> massToKilograms(exercise.trainingMaxValue, explicitTrainingMaxUnit)
                    preservedExercise?.trainingMaxValue != null -> preservedExercise.trainingMaxKg
                    else -> null
                }
                val cycleIncrementValue = exercise.cycleIncrementValue ?: preservedExercise?.cycleIncrementValue
                val trainingMaxSource = when {
                    exercise.trainingMaxValue != null -> exercise.trainingMaxSource.name
                    preservedExercise?.trainingMaxValue != null -> preservedExercise.trainingMaxSource
                    else -> exercise.trainingMaxSource.name
                }
                val routineExerciseId = dao.insertExercise(
                    RoutineExerciseEntity(
                        uuid = ids.nextId(), routineDayId = dayId, exerciseId = exercise.exerciseId,
                        position = exerciseIndex, notes = exercise.notes.trim(), groupKey = exercise.groupKey,
                        copyPreviousWorkout = exercise.copyPreviousWorkout,
                        createdAtMillis = now, updatedAtMillis = now,
                        machineId = machine?.id,
                        equipmentBindingState = bindingState.name,
                        machineProfileUuidSnapshot = machine?.uuid ?: exercise.machineProfileUuidSnapshot,
                        machineNameSnapshot = machine?.displayName() ?: exercise.machineNameSnapshot,
                        machineLoadTypeSnapshot = machine?.loadType ?: exercise.machineLoadTypeSnapshot?.name.orEmpty(),
                        machineUnitIdSnapshot = machine?.unitId ?: exercise.machineUnitIdSnapshot,
                        machineLevelLabelSnapshot = machine?.levelLabel ?: exercise.machineLevelLabelSnapshot,
                        machineLoadInterpretationSnapshot = machine?.loadInterpretation
                            ?: exercise.machineLoadInterpretationSnapshot.name,
                        machineConfigurationGroupSnapshot = machine?.configurationGroupId
                            ?: exercise.machineConfigurationGroupSnapshot,
                        machineConfigurationVersionSnapshot = machine?.configurationVersion
                            ?: exercise.machineConfigurationVersionSnapshot,
                        machineConfigurationSnapshot = machine?.configurationSummary()
                            ?: exercise.machineConfigurationSnapshot,
                        trainingMaxPercent = exercise.trainingMaxPercent,
                        progressionPercentagesCsv = exercise.progressionPercentages.joinToString(","),
                        alternativeExerciseIdsCsv = exercise.alternativeExerciseIds.distinct().joinToString(","),
                        trainingMaxKg = explicitTrainingMaxKg,
                        trainingMaxValue = explicitTrainingMaxValue,
                        trainingMaxUnitId = explicitTrainingMaxUnit,
                        cycleIncrementValue = cycleIncrementValue,
                        trainingMaxSource = trainingMaxSource,
                    ),
                )
                exercise.plannedSets.forEachIndexed { setIndex, set ->
                    dao.insertSet(set.toRoutineEntity(ids.nextId(), routineExerciseId, setIndex, now))
                }
            }
        }
    }

    private suspend fun loadDraft(routineId: Long): RoutineDraft {
        val routine = dao.getRoutine(routineId) ?: error("Routine no longer exists")
        val days = dao.getDays(routineId).map { day ->
            RoutineDayDraft(
                name = day.name,
                exercises = dao.getExercises(day.id).map { exercise ->
                    RoutineExerciseDraft(
                        exerciseId = exercise.exerciseId,
                        notes = exercise.notes,
                        groupKey = exercise.groupKey,
                        copyPreviousWorkout = exercise.copyPreviousWorkout,
                        plannedSets = dao.getSets(exercise.id).map { it.toDomain().draft },
                        machineId = exercise.machineId,
                        equipmentBindingState = runCatching {
                            RoutineEquipmentBindingState.valueOf(exercise.equipmentBindingState)
                        }.getOrDefault(if (exercise.machineId == null) RoutineEquipmentBindingState.None else RoutineEquipmentBindingState.Resolved),
                        machineProfileUuidSnapshot = exercise.machineProfileUuidSnapshot,
                        machineNameSnapshot = exercise.machineNameSnapshot,
                        machineLoadTypeSnapshot = exercise.machineLoadTypeSnapshot.takeIf(String::isNotBlank)?.let(MachineLoadType::valueOf),
                        machineUnitIdSnapshot = exercise.machineUnitIdSnapshot,
                        machineLevelLabelSnapshot = exercise.machineLevelLabelSnapshot,
                        machineLoadInterpretationSnapshot = runCatching {
                            LoadInterpretation.valueOf(exercise.machineLoadInterpretationSnapshot)
                        }.getOrDefault(LoadInterpretation.Total),
                        machineConfigurationGroupSnapshot = exercise.machineConfigurationGroupSnapshot,
                        machineConfigurationVersionSnapshot = exercise.machineConfigurationVersionSnapshot,
                        machineConfigurationSnapshot = exercise.machineConfigurationSnapshot,
                        trainingMaxPercent = exercise.trainingMaxPercent,
                        progressionPercentages = exercise.progressionPercentagesCsv.parseDoubleCsv(),
                        alternativeExerciseIds = exercise.alternativeExerciseIdsCsv.parseLongCsv(),
                        trainingMaxValue = exercise.trainingMaxValue,
                        trainingMaxUnitId = exercise.trainingMaxUnitId,
                        cycleIncrementValue = exercise.cycleIncrementValue,
                        trainingMaxSource = runCatching { RoutineTrainingMaxSource.valueOf(exercise.trainingMaxSource) }
                            .getOrDefault(RoutineTrainingMaxSource.EstimatedOneRepMaxPercent),
                    )
                },
                progressionIndex = day.progressionIndex,
            )
        }
        return RoutineDraft(
            routine.name,
            routine.notes,
            days,
            RoutineProgramDraft(
                kind = runCatching { RoutineProgramKind.valueOf(routine.programKind) }
                    .getOrDefault(RoutineProgramKind.Static),
                phaseCount = routine.programPhaseCount,
                phaseLabels = routine.programPhaseLabelsCsv.parseCsvStrings(),
            ),
        )
    }
}

private fun validateRoutine(draft: RoutineDraft) {
    require(draft.name.isNotBlank()) { "Routine name is required" }
    require(draft.days.isNotEmpty()) { "A routine needs at least one day" }
    require(draft.days.all { it.name.isNotBlank() }) { "Every routine day needs a name" }
    val program = draft.program.normalizedForCreate()
    require(program.phaseCount in 1..52) { "A routine program needs from 1 to 52 phases" }
    require(program.phaseLabels.size <= program.phaseCount) { "Program phase labels cannot exceed the phase count" }
    require(program.phaseLabels.none { ',' in it }) { "Program phase labels cannot contain commas" }
    draft.days.flatMap(RoutineDayDraft::exercises).forEach { exercise ->
        require(exercise.trainingMaxPercent in 1.0..100.0) { "Training max must be from 1 to 100%" }
        exercise.trainingMaxValue?.let { value ->
            require(value.isFinite() && value > 0.0) { "Training max must be a positive number" }
            require(BuiltInUnits.get(exercise.trainingMaxUnitId)?.dimension == UnitDimension.Mass) {
                "Training max requires a mass unit"
            }
        }
        exercise.cycleIncrementValue?.let { increment ->
            require(increment.isFinite() && increment >= 0.0) { "Cycle increment cannot be negative" }
            require(exercise.trainingMaxValue != null) { "Cycle increment requires an explicit training max" }
        }
        require(exercise.progressionPercentages.all { it.isFinite() && it in 1.0..200.0 }) {
            "Cycle multipliers must be from 1 to 200%"
        }
        exercise.plannedSets.forEach { set ->
            set.routinePhaseIndex?.let { phase ->
                require(program.kind != RoutineProgramKind.Static) { "Phase-specific sets require a programmed routine" }
                require(phase in 0 until program.phaseCount) { "Routine set phase is out of range" }
            }
            if (set.loadPrescriptionType != RoutineLoadPrescriptionType.Absolute) {
                val percent = set.loadPercentage
                require(percent != null && percent.isFinite() && percent in 1.0..200.0) {
                    "Percentage prescriptions must be from 1 to 200%"
                }
                if (program.kind != RoutineProgramKind.Static && set.loadPrescriptionType == RoutineLoadPrescriptionType.PercentTrainingMax) {
                    require(exercise.trainingMaxValue != null) {
                        "Programmed training-max sets require an explicit training max"
                    }
                }
            }
        }
    }
}

private fun isCompatibleReplacement(draft: RoutineExerciseDraft, machine: GymMachineEntity): Boolean {
    val previousType = draft.machineLoadTypeSnapshot ?: return true
    if (machine.loadType != previousType.name) return false
    if (previousType == MachineLoadType.Mass && draft.machineUnitIdSnapshot.isNotBlank() &&
        machine.unitId != draft.machineUnitIdSnapshot
    ) return false
    return machine.loadInterpretation == draft.machineLoadInterpretationSnapshot.name
}

private fun GymRoutineEntity.toDomain() = GymRoutine(
    id = id,
    uuid = uuid,
    name = name,
    notes = notes,
    position = position,
    archived = archived,
    pinned = pinned,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    programKind = runCatching { RoutineProgramKind.valueOf(programKind) }.getOrDefault(RoutineProgramKind.Static),
    programPhaseCount = programPhaseCount,
    programPhaseLabels = programPhaseLabelsCsv.parseCsvStrings(),
    currentProgramPhaseIndex = currentProgramPhaseIndex,
    currentProgramCycle = currentProgramCycle,
    nextProgramDayPosition = nextProgramDayPosition,
)
private fun RoutineDayEntity.toDomain() = RoutineDay(
    id, uuid, routineId, name, position, createdAtMillis, updatedAtMillis, progressionIndex,
)
private fun RoutineExerciseEntity.toDomain() = RoutineExercise(
    id = id, uuid = uuid, routineDayId = routineDayId, exerciseId = exerciseId,
    position = position, notes = notes, groupKey = groupKey,
    copyPreviousWorkout = copyPreviousWorkout, createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis, machineId = machineId,
    equipmentBindingState = runCatching { RoutineEquipmentBindingState.valueOf(equipmentBindingState) }
        .getOrDefault(if (machineId == null) RoutineEquipmentBindingState.None else RoutineEquipmentBindingState.Resolved),
    machineProfileUuidSnapshot = machineProfileUuidSnapshot,
    machineNameSnapshot = machineNameSnapshot,
    machineLoadTypeSnapshot = machineLoadTypeSnapshot.takeIf(String::isNotBlank)?.let(MachineLoadType::valueOf),
    machineUnitIdSnapshot = machineUnitIdSnapshot,
    machineLevelLabelSnapshot = machineLevelLabelSnapshot,
    machineLoadInterpretationSnapshot = runCatching { LoadInterpretation.valueOf(machineLoadInterpretationSnapshot) }
        .getOrDefault(LoadInterpretation.Total),
    machineConfigurationGroupSnapshot = machineConfigurationGroupSnapshot,
    machineConfigurationVersionSnapshot = machineConfigurationVersionSnapshot,
    machineConfigurationSnapshot = machineConfigurationSnapshot,
    trainingMaxPercent = trainingMaxPercent,
    progressionPercentages = progressionPercentagesCsv.parseDoubleCsv(),
    alternativeExerciseIds = alternativeExerciseIdsCsv.parseLongCsv(),
    trainingMaxKg = trainingMaxKg,
    trainingMaxValue = trainingMaxValue,
    trainingMaxUnitId = trainingMaxUnitId,
    cycleIncrementValue = cycleIncrementValue,
    trainingMaxSource = runCatching { RoutineTrainingMaxSource.valueOf(trainingMaxSource) }
        .getOrDefault(RoutineTrainingMaxSource.EstimatedOneRepMaxPercent),
)
private fun RoutineSetEntity.toDomain() = RoutineSet(
    id, uuid, routineExerciseId, position,
    WorkoutSetDraft(
        weight = enteredWeight, weightUnitId = enteredWeightUnitId ?: "kilogram",
        reps = repetitions, repsMax = repetitionsMax, distance = enteredDistance,
        distanceUnitId = enteredDistanceUnitId ?: "kilometre", durationSeconds = durationSeconds,
        bodyweightKg = bodyweightKg, planned = true, completed = false,
        classification = WorkoutSetClassification.valueOf(classification), note = note,
        rpe = rpe, rir = rir, tempo = tempo, restSeconds = restSeconds,
        machineLoadValue = machineLoadValue,
        unilateral = unilateral,
        loadPrescriptionType = runCatching { RoutineLoadPrescriptionType.valueOf(loadPrescriptionType) }
            .getOrDefault(RoutineLoadPrescriptionType.Absolute),
        loadPercentage = loadPercentage,
        routinePhaseIndex = routinePhaseIndex,
    ),
    createdAtMillis, updatedAtMillis,
)
private fun PersonalRecordEntity.toDomain() = PersonalRecord(
    uuid = uuid, exerciseId = exerciseId, type = PersonalRecordType.valueOf(type),
    value = value, secondaryValue = secondaryValue, unitId = unitId,
    sourceSetId = sourceSetId, sourceSessionId = sourceSessionId,
    achievedAtMillis = achievedAtMillis, current = current, imported = imported,
    createdAtMillis = createdAtMillis, updatedAtMillis = updatedAtMillis,
    machineId = machineId,
    machineProfileUuidSnapshot = machineProfileUuidSnapshot,
)
private fun GraphPresetEntity.toDomain() = GraphPreset(id, uuid, name, exerciseIdsCsv.split(',').mapNotNull(String::toLongOrNull), metric, dateRange, aggregation, archived, createdAtMillis, updatedAtMillis)

private fun WorkoutSetDraft.toRoutineEntity(uuid: String, routineExerciseId: Long, position: Int, now: Long) = RoutineSetEntity(
    uuid = uuid, routineExerciseId = routineExerciseId, position = position,
    classification = classification.name, enteredWeight = weight,
    enteredWeightUnitId = weightUnitId.takeIf { weight != null }, repetitions = reps,
    enteredDistance = distance, enteredDistanceUnitId = distanceUnitId.takeIf { distance != null },
    durationSeconds = durationSeconds, bodyweightKg = bodyweightKg, note = note.trim(),
    rpe = rpe, rir = rir, tempo = tempo.trim(), restSeconds = restSeconds,
    createdAtMillis = now, updatedAtMillis = now,
    machineLoadValue = machineLoadValue,
    unilateral = unilateral,
    repetitionsMax = repsMax,
    loadPrescriptionType = loadPrescriptionType.name,
    loadPercentage = loadPercentage,
    routinePhaseIndex = routinePhaseIndex,
)

private fun RoutineSetEntity.toWorkoutSet(
    uuid: String,
    workoutExerciseId: Long,
    now: Long,
    workoutExercise: WorkoutExerciseEntity,
    sourceExercise: ExerciseEntity,
    machine: GymMachineEntity?,
    oneRepMaxKg: Double?,
    trainingMaxPercent: Double,
    progressionPercent: Double,
    explicitTrainingMaxKg: Double?,
): WorkoutSetEntity {
    val machineType = workoutExercise.machineLoadTypeSnapshot.takeIf(String::isNotBlank)?.let(MachineLoadType::valueOf)
    val effectiveWeightUnitId = (
        if (machineType == MachineLoadType.Mass) workoutExercise.machineUnitIdSnapshot else enteredWeightUnitId
        ) ?: sourceExercise.weightUnitId
    val prescription = runCatching { RoutineLoadPrescriptionType.valueOf(loadPrescriptionType) }
        .getOrDefault(RoutineLoadPrescriptionType.Absolute)
    require(machineType != MachineLoadType.Level || prescription == RoutineLoadPrescriptionType.Absolute) {
        "Percentage prescriptions require a mass-based exercise or machine"
    }
    val resolvedLoad = resolveRoutinePrescribedLoad(
        type = prescription,
        enteredWeight = enteredWeight,
        enteredUnitId = effectiveWeightUnitId,
        percentage = loadPercentage,
        oneRepMaxKg = oneRepMaxKg,
        trainingMaxPercent = trainingMaxPercent,
        progressionPercent = progressionPercent,
        loadMultiplier = workoutExercise.loadMultiplierSnapshot,
        baseLoadKg = workoutExercise.baseLoadKgSnapshot,
        addOnPlateKg = workoutExercise.machineAddOnPlateKgSnapshot,
        availableLoads = machine?.availableLoadsCsv?.parseDoubleCsv().orEmpty(),
        increment = sourceExercise.weightIncrement.takeIf { effectiveWeightUnitId == sourceExercise.weightUnitId }
            ?: if (effectiveWeightUnitId == "pound") 5.0 else 2.5,
        explicitTrainingMaxKg = explicitTrainingMaxKg,
    )
    val effectiveWeight = resolvedLoad?.displayValue ?: enteredWeight.takeUnless { machineType == MachineLoadType.Level }
    val weightUnit = BuiltInUnits.get(effectiveWeightUnitId)
    val distanceUnit = enteredDistanceUnitId?.let(BuiltInUnits::get)
    return WorkoutSetEntity(
        uuid = uuid, workoutExerciseId = workoutExerciseId, position = position,
        classification = classification, planned = true, completed = false,
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
        enteredWeight = effectiveWeight, enteredWeightUnitId = effectiveWeightUnitId.takeIf { effectiveWeight != null },
        repetitions = repetitions,
        canonicalDistanceMetres = enteredDistance?.let { requireNotNull(distanceUnit).toCanonical(it) },
        enteredDistance = enteredDistance, enteredDistanceUnitId = enteredDistanceUnitId,
        durationSeconds = durationSeconds, bodyweightKg = bodyweightKg, note = note,
        rpe = rpe, rir = rir, tempo = tempo, restSeconds = restSeconds,
        completedAtMillis = null, deletedAtMillis = null,
        createdAtMillis = now, updatedAtMillis = now,
        machineLoadValue = if (machineType == MachineLoadType.Level) machineLoadValue else effectiveWeight.takeIf { workoutExercise.machineId != null },
        unilateral = unilateral,
        prescribedCanonicalWeightKg = canonicalResistanceKg(
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
        prescribedEnteredWeight = effectiveWeight,
        prescribedWeightUnitId = effectiveWeightUnitId.takeIf { effectiveWeight != null },
        prescribedRepetitions = repetitions,
        prescribedRepetitionsMax = repetitionsMax,
        prescribedRpe = rpe,
        prescribedRir = rir,
        prescribedDurationSeconds = durationSeconds,
        prescribedMachineLoadValue = if (machineType == MachineLoadType.Level) machineLoadValue else effectiveWeight.takeIf { workoutExercise.machineId != null },
        prescriptionSourceLabel = resolvedLoad?.label.orEmpty(),
    )
}

internal data class ResolvedRoutineLoad(val displayValue: Double, val label: String)

internal fun resolveRoutinePrescribedLoad(
    type: RoutineLoadPrescriptionType,
    enteredWeight: Double?,
    enteredUnitId: String,
    percentage: Double?,
    oneRepMaxKg: Double?,
    trainingMaxPercent: Double,
    progressionPercent: Double,
    loadMultiplier: Double,
    baseLoadKg: Double?,
    addOnPlateKg: Double?,
    availableLoads: List<Double>,
    increment: Double,
    explicitTrainingMaxKg: Double? = null,
): ResolvedRoutineLoad? {
    require(trainingMaxPercent in 1.0..100.0) { "Training max must be from 1 to 100%" }
    require(progressionPercent in 1.0..200.0) { "Cycle multiplier must be from 1 to 200%" }
    val progressionFactor = progressionPercent / 100.0
    val rawDisplay = when (type) {
        RoutineLoadPrescriptionType.Absolute -> enteredWeight?.times(progressionFactor) ?: return null
        RoutineLoadPrescriptionType.PercentOneRepMax,
        RoutineLoadPrescriptionType.PercentTrainingMax,
        -> {
            val selectedPercent = requireNotNull(percentage) { "Enter a load percentage" }
            require(selectedPercent in 1.0..200.0) { "Load percentage must be from 1 to 200%" }
            val base = when (type) {
                RoutineLoadPrescriptionType.PercentOneRepMax -> requireNotNull(oneRepMaxKg) {
                    "Record an estimated 1RM before starting this percentage-based routine"
                }
                RoutineLoadPrescriptionType.PercentTrainingMax -> explicitTrainingMaxKg
                    ?: requireNotNull(oneRepMaxKg) {
                        "Set an explicit training max or record an estimated 1RM before starting this routine"
                    } * trainingMaxPercent / 100.0
                RoutineLoadPrescriptionType.Absolute -> error("Absolute prescriptions are resolved separately")
            }
            val targetCanonical = base * selectedPercent / 100.0 * progressionFactor
            val rawKg = ((targetCanonical - (baseLoadKg ?: 0.0) - (addOnPlateKg ?: 0.0)) /
                loadMultiplier.takeIf { it.isFinite() && it > 0.0 }.orEmptyOne()).coerceAtLeast(0.0)
            massFromKilograms(rawKg, enteredUnitId)
        }
    }
    val choices = availableLoads.filter { it.isFinite() && it >= 0.0 }.distinct()
    val snapped = if (type == RoutineLoadPrescriptionType.Absolute && progressionPercent == 100.0 && choices.isEmpty()) {
        rawDisplay
    } else if (choices.isNotEmpty()) {
        choices.minBy { kotlin.math.abs(it - rawDisplay) }
    } else {
        val step = increment.takeIf { it.isFinite() && it > 0.0 } ?: 1.0
        round(rawDisplay / step) * step
    }.coerceAtLeast(0.0)
    val label = buildString {
        when (type) {
            RoutineLoadPrescriptionType.Absolute -> append("Exact load")
            RoutineLoadPrescriptionType.PercentOneRepMax -> append("${percentage?.let { "$it%" }} e1RM")
            RoutineLoadPrescriptionType.PercentTrainingMax -> if (explicitTrainingMaxKg != null) {
                append("${percentage?.let { "$it%" }} of explicit training max")
            } else {
                append("${percentage?.let { "$it%" }} of $trainingMaxPercent% training max")
            }
        }
        if (progressionPercent != 100.0) append(" · cycle $progressionPercent%")
    }
    return ResolvedRoutineLoad(snapped, label)
}

private fun Double?.orEmptyOne(): Double = this ?: 1.0

private fun String.parseDoubleCsv(): List<Double> = split(',').mapNotNull(String::toDoubleOrNull)
private fun String.parseLongCsv(): List<Long> = split(',').mapNotNull(String::toLongOrNull)
private fun String.parseCsvStrings(): List<String> = split(',').map(String::trim).filter(String::isNotBlank)

private fun RoutineProgramDraft?.normalizedForCreate(): RoutineProgramDraft =
    this?.normalized() ?: RoutineProgramDraft(RoutineProgramKind.Static, 1)

private fun RoutineProgramDraft.normalized(): RoutineProgramDraft = when (kind) {
    RoutineProgramKind.Static -> copy(phaseCount = 1, phaseLabels = emptyList())
    else -> copy(
        phaseCount = phaseCount.coerceAtLeast(1),
        phaseLabels = phaseLabels.map(String::trim).filter(String::isNotBlank).take(phaseCount.coerceAtLeast(1)),
    )
}

private fun WorkoutSetEntity.toDraft() = WorkoutSetDraft(
    weight = enteredWeight, weightUnitId = enteredWeightUnitId ?: "kilogram", reps = repetitions,
    repsMax = prescribedRepetitionsMax,
    distance = enteredDistance, distanceUnitId = enteredDistanceUnitId ?: "kilometre",
    durationSeconds = durationSeconds, bodyweightKg = bodyweightKg,
    planned = true, completed = false,
    classification = WorkoutSetClassification.valueOf(classification), note = note,
    rpe = rpe, rir = rir, tempo = tempo, restSeconds = restSeconds,
    machineLoadValue = machineLoadValue,
    unilateral = unilateral,
)

private fun ExerciseEntity.toDomainForRecords() = Exercise(
    id, uuid, name, com.whip.app.domain.ExerciseTrackingType.valueOf(trackingType), notes,
    equipment, primaryMuscles, secondaryMuscles, weightUnitId, weightIncrement,
    repetitionIncrement, defaultRestSeconds, defaultGraphMetric,
    com.whip.app.domain.EstimatedOneRepMaxFormula.valueOf(oneRepMaxFormula), barWeightKg,
    availablePlatesKgCsv.split(',').mapNotNull(String::toDoubleOrNull), includeInVolume,
    includeInPersonalRecords, com.whip.app.domain.BodyweightLoadPolicy.valueOf(bodyweightLoadPolicy),
    effectiveBodyweightPercent, showRpe, showRir, showTempo, favorite, position, archived,
    createdAtMillis, updatedAtMillis,
    runCatching { LoadInterpretation.valueOf(loadInterpretation) }.getOrDefault(LoadInterpretation.Total),
)

private fun WorkoutExerciseEntity.applyPolicySnapshot(exercise: Exercise): Exercise = exercise.copy(
    trackingType = runCatching { com.whip.app.domain.ExerciseTrackingType.valueOf(trackingTypeSnapshot) }
        .getOrDefault(com.whip.app.domain.ExerciseTrackingType.WeightReps),
    bodyweightLoadPolicy = runCatching { com.whip.app.domain.BodyweightLoadPolicy.valueOf(bodyweightLoadPolicySnapshot) }
        .getOrDefault(com.whip.app.domain.BodyweightLoadPolicy.ExternalWeightOnly),
    effectiveBodyweightPercent = effectiveBodyweightPercentSnapshot,
    oneRepMaxFormula = runCatching { com.whip.app.domain.EstimatedOneRepMaxFormula.valueOf(oneRepMaxFormulaSnapshot) }
        .getOrDefault(com.whip.app.domain.EstimatedOneRepMaxFormula.Epley),
    includeInVolume = includeInVolumeSnapshot,
    includeInPersonalRecords = includeInPersonalRecordsSnapshot,
    loadInterpretation = runCatching { LoadInterpretation.valueOf(loadInterpretationSnapshot) }
        .getOrDefault(LoadInterpretation.Total),
)

private fun WorkoutSetEntity.toDomainForRecords() = WorkoutSet(
    id, uuid, workoutExerciseId, position, WorkoutSetClassification.valueOf(classification),
    planned, completed, canonicalWeightKg, enteredWeight, enteredWeightUnitId, repetitions,
    canonicalDistanceMetres, enteredDistance, enteredDistanceUnitId, durationSeconds,
    bodyweightKg, note, rpe, rir, tempo, restSeconds, completedAtMillis, deletedAtMillis,
    createdAtMillis, updatedAtMillis, machineLoadValue,
    unilateral, prescribedCanonicalWeightKg, prescribedEnteredWeight,
    prescribedWeightUnitId, prescribedRepetitions, prescribedRpe, prescribedRir,
    prescribedDurationSeconds, prescribedMachineLoadValue,
)
