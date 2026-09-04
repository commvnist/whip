package com.whip.app.data

import androidx.room.withTransaction
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.core.SettingsRepository
import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.Exercise
import com.whip.app.domain.ExerciseTrackingType
import com.whip.app.domain.GymRoutine
import com.whip.app.domain.MachineLoadType
import com.whip.app.domain.MachineLevelDirection
import com.whip.app.domain.LoadInterpretation
import com.whip.app.domain.MachineStackMode
import com.whip.app.domain.configuredMachineLevelDefault
import com.whip.app.domain.resolveMachineLevelDefault
import com.whip.app.domain.canonicalResistanceKg
import com.whip.app.domain.loadInterpretationMultiplier
import com.whip.app.domain.supportsRoutinePercentagePrescription
import com.whip.app.domain.massFromKilograms
import com.whip.app.domain.massToKilograms
import com.whip.app.domain.GraphPreset
import com.whip.app.domain.GymGraphAggregation
import com.whip.app.domain.GymGraphMetric
import com.whip.app.domain.GymGraphRange
import com.whip.app.domain.PersonalRecord
import com.whip.app.domain.PersonalRecordType
import com.whip.app.domain.RoutineDay
import com.whip.app.domain.RoutineDayDraft
import com.whip.app.domain.RoutineDraft
import com.whip.app.domain.RoutineExercise
import com.whip.app.domain.RoutineExerciseDraft
import com.whip.app.domain.RoutineEquipmentBindingState
import com.whip.app.domain.RoutineLoadPrescriptionType
import com.whip.app.domain.RoutineMainWorkScheme
import com.whip.app.domain.RoutineOptionalWorkKind
import com.whip.app.domain.RoutineProgramDraft
import com.whip.app.domain.RoutineProgramKind
import com.whip.app.domain.RoutineProgramPhaseRole
import com.whip.app.domain.RoutineSet
import com.whip.app.domain.RoutineSupplementalScheme
import com.whip.app.domain.RoutineTrainingMaxSource
import com.whip.app.domain.RoutineAssistanceCategory
import com.whip.app.domain.RoutinePlacementKind
import com.whip.app.domain.RoutineProgramTemplateKey
import com.whip.app.domain.RoutineProgressionMode
import com.whip.app.domain.TrainingMaxBasisKind
import com.whip.app.domain.TrainingMaxDecision
import com.whip.app.domain.TrainingMaxDecisionAction
import com.whip.app.domain.RoutineWorkSection
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
import com.whip.app.domain.balancedOncePerExerciseDayOwners
import com.whip.app.domain.FIVE_THREE_ONE_ONCE_PER_EXERCISE_PROTOCOL_REVISION
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
    val trainingMaxDecisions: Flow<List<TrainingMaxDecision>>

    suspend fun createRoutine(draft: RoutineDraft): Long
    suspend fun updateRoutine(id: Long, draft: RoutineDraft)
    suspend fun duplicateRoutine(id: Long): Long
    suspend fun setRoutineArchived(id: Long, archived: Boolean)
    suspend fun setRoutinePinned(id: Long, pinned: Boolean)
    suspend fun reorderRoutines(ids: List<Long>)
    suspend fun setRoutineProgramPosition(routineId: Long, phaseIndex: Int, dayPosition: Int, cycle: Int)
    suspend fun setRoutineTrainingMaxIncreaseEligible(routineId: Long, eligible: Boolean)
    suspend fun resetRoutineProgramProgress(routineId: Long)
    suspend fun startRoutine(routineId: Long, dayId: Long? = null): Long
    suspend fun saveWorkoutAsRoutine(sessionId: Long, name: String): Long
    suspend fun rebuildPersonalRecords(exerciseId: Long)
    suspend fun saveGraphPreset(
        name: String,
        exerciseIds: List<Long>,
        measurement: String,
        dateRange: String,
        aggregation: String,
    ): Long
    suspend fun updateGraphPreset(
        id: Long,
        name: String,
        exerciseIds: List<Long>,
        measurement: String,
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
    override val trainingMaxDecisions = dao.observeTrainingMaxDecisions()
        .map { it.map(TrainingMaxDecisionEntity::toDomain) }

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
                programPhaseRolesCsv = program.phaseRoles.joinToString(",", transform = RoutineProgramPhaseRole::name),
                trainingMaxAdvanceAfterPhaseIndicesCsv = program.trainingMaxAdvanceAfterPhaseIndices.sorted().joinToString(","),
                programTemplateKey = program.templateKey.name,
                programTemplateRevision = program.templateRevision,
                progressionMode = program.progressionMode.name,
                allowNonStandardHigherSuggestions = program.allowNonStandardHigherSuggestions,
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
        val existingMainPlacements = mutableListOf<RoutineExerciseEntity>()
        existingDays.forEach { day ->
            dao.getExercises(day.id).forEach { placement ->
                if (dao.getSets(placement.id).any { it.workSection == RoutineWorkSection.Main.name }) {
                    existingMainPlacements += placement
                }
            }
        }
        val now = clock.now().toEpochMilli()
        val requestedProgram = draft.program?.normalized()
        val existingProgramEnabled = existing.programKind != RoutineProgramKind.Static.name && existing.programPhaseCount > 0
        val requestedProgramEnabled = requestedProgram != null &&
            requestedProgram.kind != RoutineProgramKind.Static && requestedProgram.phaseCount > 0
        // Metadata edits are not a restart command. Only turning a static routine into a
        // structured program initializes position; phase-count changes clamp the current phase.
        val initializesProgram = !existingProgramEnabled && requestedProgramEnabled
        val requestedPhaseCount = requestedProgram?.phaseCount ?: existing.programPhaseCount
        val existingCurrentPhaseLabel = existing.programPhaseLabelsCsv.parseCsvStrings()
            .getOrNull(existing.currentProgramPhaseIndex)
        val reorderedCurrentPhaseIndex = requestedProgram?.currentPhaseIndexHint
            ?.takeIf { it in 0 until requestedPhaseCount }
            ?: existingCurrentPhaseLabel?.let { label ->
                requestedProgram?.phaseLabels?.indexOf(label)?.takeIf { index ->
                    index >= 0 && requestedProgram.phaseLabels.count { it == label } == 1
                }
            }
        dao.updateRoutine(
            existing.copy(
                name = draft.name.trim(),
                notes = draft.notes.trim(),
                updatedAtMillis = now,
                programKind = requestedProgram?.kind?.name ?: existing.programKind,
                programPhaseCount = requestedProgram?.phaseCount ?: existing.programPhaseCount,
                programPhaseLabelsCsv = requestedProgram?.phaseLabels?.joinToString(",") ?: existing.programPhaseLabelsCsv,
                programPhaseRolesCsv = requestedProgram?.phaseRoles?.joinToString(",", transform = RoutineProgramPhaseRole::name)
                    ?: existing.programPhaseRolesCsv,
                trainingMaxAdvanceAfterPhaseIndicesCsv = requestedProgram?.trainingMaxAdvanceAfterPhaseIndices
                    ?.sorted()?.joinToString(",") ?: existing.trainingMaxAdvanceAfterPhaseIndicesCsv,
                programTemplateKey = requestedProgram?.templateKey?.name ?: existing.programTemplateKey,
                programTemplateRevision = requestedProgram?.templateRevision ?: existing.programTemplateRevision,
                progressionMode = requestedProgram?.progressionMode?.name ?: existing.progressionMode,
                allowNonStandardHigherSuggestions = requestedProgram?.allowNonStandardHigherSuggestions
                    ?: existing.allowNonStandardHigherSuggestions,
                currentProgramPhaseIndex = if (initializesProgram) {
                    0
                } else {
                    (reorderedCurrentPhaseIndex ?: existing.currentProgramPhaseIndex)
                        .coerceIn(0, (requestedPhaseCount - 1).coerceAtLeast(0))
                },
                currentProgramCycle = if (initializesProgram) 1 else existing.currentProgramCycle.coerceAtLeast(1),
                nextProgramDayPosition = if (initializesProgram) {
                    0
                } else {
                    (draft.nextProgramDayPositionHint ?: existing.nextProgramDayPosition)
                        .coerceIn(0, draft.days.lastIndex)
                },
                trainingMaxIncreaseEligible = if (initializesProgram) true else existing.trainingMaxIncreaseEligible,
            ),
        )
        // Day rows are replaced below. Historical sessions retain the routine UUID/ID and their
        // authored day position, but must not retain a numeric pointer to a deleted day row.
        gymDao.clearSourceRoutineDayReferences(id)
        dao.deleteDays(id)
        insertRoutineChildren(id, draft.days, now, existingDays)
        val previousByExercise = existingMainPlacements.groupBy(RoutineExerciseEntity::exerciseId)
            .mapValues { (_, placements) -> placements.first() }
        val updatedByExercise = draft.days.flatMap(RoutineDayDraft::exercises)
            .filter { it.resolvedPlacementKind() == RoutinePlacementKind.MainExercise }
            .groupBy(RoutineExerciseDraft::exerciseId)
            .mapValues { (_, placements) -> placements.first() }
        updatedByExercise.forEach { (exerciseId, updated) ->
            val previous = previousByExercise[exerciseId] ?: return@forEach
            val previousValue = previous.trainingMaxValue ?: return@forEach
            val updatedValue = updated.trainingMaxValue ?: return@forEach
            val previousKg = massToKilograms(previousValue, previous.trainingMaxUnitId)
            val updatedKg = massToKilograms(updatedValue, updated.trainingMaxUnitId)
            if (kotlin.math.abs(previousKg - updatedKg) <= 1e-9) return@forEach
            val exercise = gymDao.getExercise(exerciseId) ?: return@forEach
            val previousInUpdatedUnit = massFromKilograms(previousKg, updated.trainingMaxUnitId)
            val delta = updatedValue - previousInUpdatedUnit
            dao.insertTrainingMaxDecision(
                TrainingMaxDecisionEntity(
                    uuid = ids.nextId(),
                    routineUuid = existing.uuid,
                    sessionUuid = "routine-edit:${ids.nextId()}",
                    exerciseUuid = exercise.uuid,
                    exerciseName = exercise.name,
                    cycle = existing.currentProgramCycle,
                    previousTrainingMax = previousInUpdatedUnit,
                    appliedDelta = delta,
                    resultingTrainingMax = updatedValue,
                    unitId = updated.trainingMaxUnitId,
                    standardDelta = updated.cycleIncrementValue ?: previous.cycleIncrementValue ?: 0.0,
                    recommendationCategory = "ManualAdjustment",
                    recommendationDelta = delta,
                    confidence = 1.0,
                    reasonsText = "Training Max changed in the program editor; completed workout prescriptions remain unchanged.",
                    engineVersion = "manual-training-max/1",
                    action = TrainingMaxDecisionAction.Custom.name,
                    createdAtMillis = now,
                ),
            )
        }
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
        val routine = dao.getRoutine(id) ?: error("Routine no longer exists")
        dao.updateRoutine(routine.copy(archived = archived, updatedAtMillis = clock.now().toEpochMilli()))
    }

    override suspend fun setRoutinePinned(id: Long, pinned: Boolean) {
        val routine = dao.getRoutine(id) ?: error("Routine no longer exists")
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
        require(gymDao.getActiveSession()?.sourceRoutineId != routineId) {
            "Finish or discard the active workout before changing program position"
        }
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

    override suspend fun setRoutineTrainingMaxIncreaseEligible(routineId: Long, eligible: Boolean) =
        database.withTransaction {
            val routine = dao.getRoutine(routineId) ?: error("Routine no longer exists")
            require(gymDao.getActiveSession()?.sourceRoutineId != routineId) {
                "Finish or discard the active workout before changing Training Max eligibility"
            }
            require(routine.programKind != RoutineProgramKind.Static.name) {
                "Static routines do not have Training Max eligibility"
            }
            dao.updateRoutine(
                routine.copy(
                    trainingMaxIncreaseEligible = eligible,
                    updatedAtMillis = clock.now().toEpochMilli(),
                ),
            )
            val now = clock.now().toEpochMilli()
            dao.getDays(routineId).forEach { day ->
                dao.getExercises(day.id).forEach { exercise ->
                    if (dao.getSets(exercise.id).any { it.workSection == RoutineWorkSection.Main.name }) {
                        dao.updateExercise(
                            exercise.copy(trainingMaxIncreaseEligible = eligible, updatedAtMillis = now),
                        )
                    }
                }
            }
        }

    override suspend fun resetRoutineProgramProgress(routineId: Long) = database.withTransaction {
        val routine = dao.getRoutine(routineId) ?: error("Routine no longer exists")
        require(gymDao.getActiveSession()?.sourceRoutineId != routineId) {
            "Finish or discard the active workout before resetting program progress"
        }
        val now = clock.now().toEpochMilli()
        dao.updateRoutine(
            routine.copy(
                currentProgramPhaseIndex = 0,
                currentProgramCycle = 1,
                nextProgramDayPosition = 0,
                trainingMaxIncreaseEligible = true,
                updatedAtMillis = now,
            ),
        )
        dao.getDays(routineId).forEach { day ->
            dao.getExercises(day.id).forEach { exercise ->
                if (!exercise.trainingMaxIncreaseEligible &&
                    dao.getSets(exercise.id).any { it.workSection == RoutineWorkSection.Main.name }
                ) {
                    dao.updateExercise(
                        exercise.copy(trainingMaxIncreaseEligible = true, updatedAtMillis = now),
                    )
                }
            }
        }
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
        val authoredProgramPhaseRole = routine.programPhaseRolesCsv.parseCsvStrings()
            .getOrNull(routine.currentProgramPhaseIndex)
            ?.let { runCatching { RoutineProgramPhaseRole.valueOf(it) }.getOrNull() }
            ?: RoutineProgramPhaseRole.Standard
        val activeProgramPhaseRole = authoredProgramPhaseRole.semanticRole()
        val templateKey = runCatching { RoutineProgramTemplateKey.valueOf(routine.programTemplateKey) }
            .getOrDefault(RoutineProgramTemplateKey.None)
        val oncePerExerciseProtocolPhase =
            templateKey != RoutineProgramTemplateKey.None &&
                routine.programTemplateRevision >= FIVE_THREE_ONE_ONCE_PER_EXERCISE_PROTOCOL_REVISION &&
                authoredProgramPhaseRole.usesOncePerExerciseProtocol()
        val protocolOwnerDayIdByExerciseId = if (programmed && oncePerExerciseProtocolPhase) {
            val mainExercisesByDay = days.map { day ->
                dao.getExercises(day.id).filter { exercise ->
                    exercise.placementKind == RoutinePlacementKind.MainExercise.name
                }
            }
            val owners = balancedOncePerExerciseDayOwners(
                mainExercisesByDay.map { exercises -> exercises.map { it.exerciseId } },
            ).mapValuesTo(mutableMapOf()) { (_, dayIndex) -> days[dayIndex].id }
            if (activeProgramPhaseRole == RoutineProgramPhaseRole.TrainingMaxTest) {
                mainExercisesByDay.forEachIndexed { dayIndex, exercises ->
                    exercises.forEach { exercise ->
                        if (dao.getSets(exercise.id).any { set ->
                                set.routinePhaseIndex == routine.currentProgramPhaseIndex &&
                                    set.classification == WorkoutSetClassification.TrainingMaxTest.name
                            }
                        ) owners[exercise.exerciseId] = days[dayIndex].id
                    }
                }
            }
            owners
        } else {
            emptyMap()
        }
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
                sourceRoutineDayProgressionIndex = selectedDay.progressionIndex.takeIf {
                    routineExercises.any { exercise -> exercise.progressionPercentagesCsv.parseDoubleCsv().isNotEmpty() }
                },
                sourceRoutinePhaseLabel = routine.programPhaseLabelsCsv.parseCsvStrings()
                    .getOrNull(routine.currentProgramPhaseIndex).orEmpty(),
            sourceRoutinePhaseRole = activeProgramPhaseRole.name,
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
            val allPlanned = dao.getSets(routineExercise.id)
            val planned = if (programmed) {
                allPlanned.filter { it.routinePhaseIndex == null || it.routinePhaseIndex == routine.currentProgramPhaseIndex }
            } else {
                allPlanned.filter { it.routinePhaseIndex == null }
            }
            val mainPlacement = routineExercise.placementKind == RoutinePlacementKind.MainExercise.name
            if (
                oncePerExerciseProtocolPhase && mainPlacement &&
                protocolOwnerDayIdByExerciseId[routineExercise.exerciseId] != selectedDay.id
            ) return@forEach
            // Phase-scoped Main or Supplemental placements can intentionally have no work in
            // this phase (for example an alternate BBB exercise outside a Leader).
            // Do not create an empty exercise card in the active workout.
            if (programmed && planned.isEmpty() && routineExercise.placementKind in setOf(
                    RoutinePlacementKind.MainExercise.name,
                    RoutinePlacementKind.Supplemental.name,
                )
            ) return@forEach
            val activePolicy = if (programmed) {
                resolveActiveProgramPolicy(
                    baseMainWorkScheme = routineExercise.mainWorkScheme.toMainWorkScheme(),
                    baseSupplementalScheme = routineExercise.supplementalScheme.toSupplementalScheme(),
                    activeSets = planned.map { set -> set.toDomain().draft },
                )
            } else {
                ResolvedActiveProgramPolicy(
                    mainWorkScheme = routineExercise.mainWorkScheme.toMainWorkScheme(),
                    supplementalScheme = routineExercise.supplementalScheme.toSupplementalScheme(),
                    jokerSetsEnabled = routineExercise.jokerSetsEnabled,
                )
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
                    machineLevelDirectionSnapshot = machine?.levelDirection
                        ?: MachineLevelDirection.HigherNumberMoreResistance.name,
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
                    mainWorkSchemeSnapshot = activePolicy.mainWorkScheme.name,
                    supplementalSchemeSnapshot = activePolicy.supplementalScheme.name,
                    placementKindSnapshot = routineExercise.placementKind,
                    assistanceCategorySnapshot = routineExercise.assistanceCategory,
                    jokerSetsEnabledSnapshot = activePolicy.jokerSetsEnabled,
                )
            val workoutExerciseId = gymDao.insertWorkoutExercise(workoutExerciseEntity)
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
                            position = 0, classification = WorkoutSetClassification.Working.name,
                            planned = true, completed = false,
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
                            prescriptionSourceLabel = "Previous workout",
                            workSectionSnapshot = RoutineWorkSection.Unspecified.name,
                            optionalWorkKindSnapshot = RoutineOptionalWorkKind.None.name,
                            prescribedClassificationSnapshot = WorkoutSetClassification.Working.name,
                            requiredForProgressionSnapshot = true,
                            removalReason = null,
                        ),
                    )
                }
            }
        }
        sessionId
    }

    override suspend fun saveWorkoutAsRoutine(sessionId: Long, name: String): Long {
        val session = gymDao.getSession(sessionId) ?: error("Workout no longer exists")
        val exerciseDrafts = gymDao.getWorkoutExercises(sessionId).mapNotNull { sourceWorkoutExercise ->
            val projection = sourceWorkoutExercise.toWorkoutReuseProjection(
                gymDao.getWorkoutSets(sourceWorkoutExercise.id),
            ) ?: return@mapNotNull null
            val workoutExercise = projection.workoutExercise
            RoutineExerciseDraft(
                exerciseId = workoutExercise.exerciseId,
                notes = workoutExercise.notes,
                groupKey = workoutExercise.groupId?.let { "Group $it" },
                plannedSets = projection.sets.map(WorkoutSetEntity::toWorkoutReuseDraft),
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
                mainWorkScheme = RoutineMainWorkScheme.Unspecified,
                supplementalScheme = RoutineSupplementalScheme.None,
                placementKind = RoutinePlacementKind.General,
                assistanceCategory = RoutineAssistanceCategory.Unspecified,
                jokerSetsEnabled = false,
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
        val volumeEligibleSetIds = mutableSetOf<Long>()
        val settings = settingsRepository?.current()
        val includeWarmups = settings?.includeWarmupsInGymStats == true

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
            val sourceWorkoutExercise = gymDao.getWorkoutExercise(entity.workoutExerciseId) ?: return@forEach
            val sourceSession = gymDao.getSession(sourceWorkoutExercise.sessionId) ?: return@forEach
            if (sourceSession.archived || sourceSession.state == WorkoutSessionState.Discarded.name) return@forEach
            val sourceSessionId = sourceWorkoutExercise.sessionId
            val machineId = sourceWorkoutExercise.machineId
            val machineScope = sourceWorkoutExercise.machineProfileUuidSnapshot
            val policyExercise = sourceWorkoutExercise.applyPolicySnapshot(exercise)
            val assistedAllowed = settings?.includeAssistedInPersonalRecords == true ||
                policyExercise.trackingType != com.whip.app.domain.ExerciseTrackingType.AssistedBodyweightReps
            if (!policyExercise.includeInPersonalRecords ||
                (!includeWarmups && set.classification == WorkoutSetClassification.WarmUp) ||
                !assistedAllowed
            ) return@forEach
            sourceSessionBySetId[entity.id] = sourceSessionId
            sourceMachineBySetId[entity.id] = machineId
            sourceMachineScopeBySetId[entity.id] = machineScope
            policyExerciseBySetId[entity.id] = policyExercise
            consider(entity, sourceSessionId, machineId, machineScope, PersonalRecordType.MaxWeight, set.effectiveLoadKg(policyExercise), "kilogram")
            consider(entity, sourceSessionId, machineId, machineScope, PersonalRecordType.MaxRepetitions, set.repetitions?.toDouble(), "count")
            consider(
                entity,
                sourceSessionId,
                machineId,
                machineScope,
                PersonalRecordType.MaxRepetitionsForWeight,
                set.repetitions?.toDouble(),
                "count",
                set.effectiveLoadKg(policyExercise),
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
                ),
                "kilogram",
            )
            val setVolume = set.volumeKg(policyExercise, includeWarmups).takeIf { it > 0.0 }
            consider(entity, sourceSessionId, machineId, machineScope, PersonalRecordType.SetVolume, setVolume, "kilogram")
            if (setVolume != null) volumeEligibleSetIds += entity.id
            consider(entity, sourceSessionId, machineId, machineScope, PersonalRecordType.MaxDistance, set.canonicalDistanceMetres, "distance_m")
            consider(entity, sourceSessionId, machineId, machineScope, PersonalRecordType.MaxDuration, set.durationSeconds?.toDouble(), "second")
            consider(entity, sourceSessionId, machineId, machineScope, PersonalRecordType.MaxSpeed, set.speedMetresPerSecond(), "distance_m/second")
            consider(entity, sourceSessionId, machineId, machineScope, PersonalRecordType.MinPace, set.paceSecondsPerKilometre(), "second/kilometre", lowerIsBetter = true)
            if (sourceWorkoutExercise.machineLoadTypeSnapshot == MachineLoadType.Level.name) {
                val lowerSettingIsStronger = sourceWorkoutExercise.machineLevelDirectionSnapshot ==
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
        sets.filter { it.id in volumeEligibleSetIds }
            .groupBy { Triple(sourceSessionBySetId[it.id], sourceMachineBySetId[it.id], sourceMachineScopeBySetId[it.id]) }
            .forEach { (scope, sessionSets) ->
                val (sessionId, machineId, machineScope) = scope
                if (sessionId == null || sessionSets.isEmpty()) return@forEach
                val volume = sessionSets.sumOf { entity ->
                    entity.toDomainForRecords().volumeKg(
                        policyExerciseBySetId[entity.id] ?: exercise,
                        includeWarmups,
                    )
                }
                val representative = sessionSets.maxBy { it.completedAtMillis ?: it.updatedAtMillis }
                consider(
                    representative,
                    sessionId,
                    machineId,
                    machineScope,
                    PersonalRecordType.ExerciseWorkoutVolume,
                    volume.takeIf { it > 0.0 },
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
        measurement: String,
        dateRange: String,
        aggregation: String,
    ): Long = database.withTransaction {
        val normalizedExerciseIds = validateGraphPreset(name, exerciseIds, measurement, dateRange, aggregation)
        val now = clock.now().toEpochMilli()
        val entity = GraphPresetEntity(
            uuid = ids.nextId(), name = name.trim(), exerciseIdsCsv = normalizedExerciseIds.joinToString(","),
            measurement = measurement, dateRange = dateRange, aggregation = aggregation,
            archived = false, createdAtMillis = now, updatedAtMillis = now,
        )
        dao.insertGraphPreset(entity)
    }

    override suspend fun updateGraphPreset(
        id: Long,
        name: String,
        exerciseIds: List<Long>,
        measurement: String,
        dateRange: String,
        aggregation: String,
    ) = database.withTransaction {
        val normalizedExerciseIds = validateGraphPreset(name, exerciseIds, measurement, dateRange, aggregation)
        val existing = dao.getGraphPresets().firstOrNull { it.id == id }
            ?: error("Graph preset no longer exists")
        dao.updateGraphPreset(
            existing.copy(
                name = name.trim(),
                exerciseIdsCsv = normalizedExerciseIds.joinToString(","),
                measurement = measurement,
                dateRange = dateRange,
                aggregation = aggregation,
                updatedAtMillis = clock.now().toEpochMilli(),
            ),
        )
    }

    private suspend fun validateGraphPreset(
        name: String,
        exerciseIds: List<Long>,
        measurement: String,
        dateRange: String,
        aggregation: String,
    ): List<Long> {
        require(name.isNotBlank()) { "Preset name is required" }
        val normalizedExerciseIds = exerciseIds.distinct()
        require(normalizedExerciseIds.isNotEmpty()) { "Select at least one exercise" }
        normalizedExerciseIds.forEach { exerciseId ->
            requireNotNull(gymDao.getExercise(exerciseId)) { "Exercise no longer exists" }
        }
        require(runCatching { GymGraphMetric.valueOf(measurement) }.isSuccess) { "Graph measurement is not supported" }
        require(runCatching { GymGraphRange.valueOf(dateRange) }.isSuccess) { "Graph date range is not supported" }
        require(runCatching { GymGraphAggregation.valueOf(aggregation) }.isSuccess) { "Graph aggregation is not supported" }
        return normalizedExerciseIds
    }

    override suspend fun deleteGraphPreset(id: Long) {
        dao.deleteGraphPreset(id)
    }

    private suspend fun insertRoutineChildren(
        routineId: Long,
        days: List<RoutineDayDraft>,
        now: Long,
        preservedDays: List<RoutineDayEntity> = emptyList(),
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
                val sourceExercise = requireNotNull(gymDao.getExercise(exercise.exerciseId)) { "Exercise no longer exists" }
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
                val percentageInterpretation = machine?.loadInterpretation
                    ?.let { value -> runCatching { LoadInterpretation.valueOf(value) }.getOrNull() }
                    ?: runCatching { LoadInterpretation.valueOf(sourceExercise.loadInterpretation) }
                        .getOrDefault(LoadInterpretation.Total)
                exercise.plannedSets.filter { set ->
                    set.loadPrescriptionType != RoutineLoadPrescriptionType.Absolute
                }.forEach { set ->
                    require(machine?.loadType != MachineLoadType.Level.name &&
                        percentageInterpretation.supportsRoutinePercentagePrescription()
                    ) { "Percentage prescriptions require a mass-based exercise or machine" }
                    if (set.loadPrescriptionType == RoutineLoadPrescriptionType.PercentOneRepMax) {
                        require(sourceExercise.trackingType == ExerciseTrackingType.WeightReps.name) {
                            "Estimated 1RM prescriptions require a Weight + Reps exercise"
                        }
                    } else {
                        require(sourceExercise.trackingType in setOf(
                            ExerciseTrackingType.WeightReps.name,
                            ExerciseTrackingType.WeightOnly.name,
                            ExerciseTrackingType.WeightDuration.name,
                        )) { "Training Max prescriptions require a mass-tracked exercise" }
                    }
                }
                val bindingState = when {
                    machine != null -> RoutineEquipmentBindingState.Resolved
                    exercise.equipmentBindingState == RoutineEquipmentBindingState.NeedsEquipment -> RoutineEquipmentBindingState.NeedsEquipment
                    else -> RoutineEquipmentBindingState.None
                }
                // RoutineExerciseDraft is a complete edit contract. Null clears an explicit TM;
                // a non-null value must state that it is the applied explicit Training Max.
                val explicitTrainingMaxValue = exercise.trainingMaxValue
                require(
                    explicitTrainingMaxValue == null || exercise.trainingMaxSource == RoutineTrainingMaxSource.Explicit,
                ) { "An applied Training Max must use the Explicit source" }
                val explicitTrainingMaxUnit = exercise.trainingMaxUnitId
                val explicitTrainingMaxKg = explicitTrainingMaxValue?.let { value ->
                    massToKilograms(value, explicitTrainingMaxUnit)
                }
                val cycleIncrementValue = exercise.cycleIncrementValue.takeIf { explicitTrainingMaxValue != null }
                val trainingMaxSource = exercise.trainingMaxSource.name
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
                        trainingMaxBasisKind = exercise.trainingMaxBasisKind.name,
                        trainingMaxBasisValue = exercise.trainingMaxBasisValue,
                        trainingMaxBasisUnitId = exercise.trainingMaxBasisUnitId,
                        trainingMaxIncreaseEligible = exercise.trainingMaxIncreaseEligible,
                        mainWorkScheme = exercise.mainWorkScheme.name,
                        supplementalScheme = exercise.supplementalScheme.name,
                        placementKind = exercise.resolvedPlacementKind().name,
                        assistanceCategory = exercise.resolvedAssistanceCategory().name,
                        jokerSetsEnabled = exercise.jokerSetsEnabled,
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
                        trainingMaxBasisKind = runCatching { TrainingMaxBasisKind.valueOf(exercise.trainingMaxBasisKind) }
                            .getOrDefault(TrainingMaxBasisKind.Unspecified),
                        trainingMaxBasisValue = exercise.trainingMaxBasisValue,
                        trainingMaxBasisUnitId = exercise.trainingMaxBasisUnitId,
                        trainingMaxIncreaseEligible = exercise.trainingMaxIncreaseEligible,
                        mainWorkScheme = runCatching { RoutineMainWorkScheme.valueOf(exercise.mainWorkScheme) }
                            .getOrDefault(RoutineMainWorkScheme.Unspecified),
                        supplementalScheme = runCatching { RoutineSupplementalScheme.valueOf(exercise.supplementalScheme) }
                            .getOrDefault(RoutineSupplementalScheme.None),
                        placementKind = runCatching { RoutinePlacementKind.valueOf(exercise.placementKind) }
                            .getOrDefault(RoutinePlacementKind.General),
                        assistanceCategory = runCatching { RoutineAssistanceCategory.valueOf(exercise.assistanceCategory) }
                            .getOrDefault(RoutineAssistanceCategory.Unspecified),
                        jokerSetsEnabled = exercise.jokerSetsEnabled,
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
                phaseRoles = routine.programPhaseRolesCsv.parseCsvStrings().map {
                    runCatching { RoutineProgramPhaseRole.valueOf(it) }.getOrDefault(RoutineProgramPhaseRole.Standard)
                },
                trainingMaxAdvanceAfterPhaseIndices = routine.trainingMaxAdvanceAfterPhaseIndicesCsv
                    .parseIntCsv().toSet(),
                templateKey = runCatching { RoutineProgramTemplateKey.valueOf(routine.programTemplateKey) }
                    .getOrDefault(RoutineProgramTemplateKey.None),
                templateRevision = routine.programTemplateRevision,
                progressionMode = runCatching { RoutineProgressionMode.valueOf(routine.progressionMode) }
                    .getOrDefault(RoutineProgressionMode.Standard),
                allowNonStandardHigherSuggestions = routine.allowNonStandardHigherSuggestions,
            ),
        )
    }
}

private fun RoutineExerciseDraft.resolvedPlacementKind(): RoutinePlacementKind = when {
    placementKind != RoutinePlacementKind.General || assistanceCategory != RoutineAssistanceCategory.Unspecified -> placementKind
    plannedSets.isNotEmpty() && plannedSets.all { it.workSection == RoutineWorkSection.Assistance } -> RoutinePlacementKind.Assistance
    plannedSets.isNotEmpty() && plannedSets.all { it.workSection == RoutineWorkSection.Supplemental } -> RoutinePlacementKind.Supplemental
    plannedSets.any { it.workSection == RoutineWorkSection.Main } -> RoutinePlacementKind.MainExercise
    else -> RoutinePlacementKind.General
}

private fun RoutineExerciseDraft.resolvedAssistanceCategory(): RoutineAssistanceCategory = when {
    assistanceCategory != RoutineAssistanceCategory.Unspecified -> assistanceCategory
    resolvedPlacementKind() == RoutinePlacementKind.Assistance -> RoutineAssistanceCategory.Other
    else -> RoutineAssistanceCategory.Unspecified
}

private fun validateRoutine(draft: RoutineDraft) {
    require(draft.name.isNotBlank()) { "Routine name is required" }
    require(draft.days.isNotEmpty()) { "A routine needs at least one day" }
    require(draft.days.all { it.name.isNotBlank() }) { "Every routine day needs a name" }
    val program = draft.program.normalizedForCreate()
    require(program.phaseCount in 1..52) { "A routine program needs from 1 to 52 phases" }
    require(program.templateRevision >= 0) { "Program template revision cannot be negative" }
    require(
        (program.templateKey == RoutineProgramTemplateKey.None && program.templateRevision == 0) ||
            (program.templateKey != RoutineProgramTemplateKey.None && program.templateRevision > 0),
    ) { "Program template provenance requires a positive revision" }
    require(program.phaseLabels.size <= program.phaseCount) { "Program phase labels cannot exceed the phase count" }
    require(program.phaseRoles.size <= program.phaseCount) { "Program phase roles cannot exceed the phase count" }
    require(program.trainingMaxAdvanceAfterPhaseIndices.all { it in 0 until program.phaseCount }) {
        "Training-max advance boundaries must reference a program phase"
    }
    require(program.phaseLabels.none { ',' in it }) { "Program phase labels cannot contain commas" }
    val fiveThreeOneProgram = program.kind == RoutineProgramKind.FiveThreeOne
    require(program.progressionMode != RoutineProgressionMode.PerformanceInformed || fiveThreeOneProgram) {
        "Performance-informed Training Max review is only available for 5/3/1 programs"
    }
    require(!program.allowNonStandardHigherSuggestions ||
        fiveThreeOneProgram && program.progressionMode == RoutineProgressionMode.PerformanceInformed
    ) { "Higher Training Max alternatives require performance-informed 5/3/1 review" }
    val phasesThatDisallowJokers = (0 until program.phaseCount).filterTo(mutableSetOf()) { phase ->
        program.phaseRoles.getOrNull(phase)?.semanticRole() in setOf(
            RoutineProgramPhaseRole.Deload,
            RoutineProgramPhaseRole.TrainingMaxTest,
            RoutineProgramPhaseRole.PersonalRecordTest,
        )
    }
    draft.days.flatMap(RoutineDayDraft::exercises).forEach { exercise ->
        val placementKind = exercise.resolvedPlacementKind()
        val assistanceCategory = exercise.resolvedAssistanceCategory()
        require(
            (placementKind == RoutinePlacementKind.Assistance) ==
                (assistanceCategory != RoutineAssistanceCategory.Unspecified),
        ) { "Assistance placements require an explicit Push, Pull, Single-leg/Core, or Other category" }
        when (placementKind) {
            RoutinePlacementKind.MainExercise -> require(exercise.plannedSets.none {
                it.workSection in setOf(RoutineWorkSection.Assistance, RoutineWorkSection.Unspecified)
            }) { "Main-exercise placements may only contain Main, Supplemental, or Optional work" }
            RoutinePlacementKind.Supplemental -> require(exercise.plannedSets.all {
                it.workSection == RoutineWorkSection.Supplemental
            }) { "Supplemental placements may only contain Supplemental work" }
            RoutinePlacementKind.Assistance -> require(exercise.plannedSets.all {
                it.workSection == RoutineWorkSection.Assistance
            }) { "Assistance placements may only contain Assistance work" }
            RoutinePlacementKind.General -> require(exercise.plannedSets.none {
                it.workSection in setOf(RoutineWorkSection.Main, RoutineWorkSection.Supplemental, RoutineWorkSection.Assistance)
            }) { "General placements cannot contain hidden program-work sections" }
        }
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
        if (fiveThreeOneProgram && placementKind == RoutinePlacementKind.MainExercise) {
            require(exercise.trainingMaxValue != null) { "5/3/1 main exercises require an explicit training max" }
            require(exercise.cycleIncrementValue?.let { it.isFinite() && it > 0.0 } == true) {
                "5/3/1 main exercises require a cycle increase above zero"
            }
        }
        require(exercise.progressionPercentages.all { it.isFinite() && it in 1.0..200.0 }) {
            "Cycle multipliers must be from 1 to 200%"
        }
        exercise.plannedSets.forEach { set ->
            require(set.mainWorkScheme == null || set.workSection == RoutineWorkSection.Main) {
                "Main-work scheme tags may only be attached to Main sets"
            }
            require(set.supplementalScheme == null || set.workSection == RoutineWorkSection.Supplemental) {
                "Supplemental scheme tags may only be attached to Supplemental sets"
            }
            require(
                set.optionalWorkKind != RoutineOptionalWorkKind.Joker ||
                    set.workSection == RoutineWorkSection.Optional,
            ) { "Joker sets must be optional work" }
            set.routinePhaseIndex?.let { phase ->
                require(program.kind != RoutineProgramKind.Static) { "Phase-specific sets require a programmed routine" }
                require(phase in 0 until program.phaseCount) { "Routine set phase is out of range" }
            }
            if (set.classification == WorkoutSetClassification.TrainingMaxTest) {
                val phase = requireNotNull(set.routinePhaseIndex) {
                    "Training Max test sets must belong to one explicit program phase"
                }
                require(
                    fiveThreeOneProgram &&
                        program.phaseRoles.getOrNull(phase)?.semanticRole() == RoutineProgramPhaseRole.TrainingMaxTest,
                ) {
                    "Training Max test sets are only allowed in a 5/3/1 Training Max Test phase"
                }
                require(set.workSection == RoutineWorkSection.Main &&
                    set.loadPrescriptionType == RoutineLoadPrescriptionType.PercentTrainingMax &&
                    set.loadPercentage?.let { kotlin.math.abs(it - 100.0) <= 1e-9 } == true &&
                    (set.reps ?: Int.MIN_VALUE) in 3..5
                ) { "A Training Max test set must prescribe 100% of TM for 3–5 reps" }
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
        if (fiveThreeOneProgram) {
            val testSetsByPhase = exercise.plannedSets
                .filter { it.classification == WorkoutSetClassification.TrainingMaxTest }
                .groupBy(WorkoutSetDraft::routinePhaseIndex)
            require(testSetsByPhase.values.all { it.size == 1 }) {
                "A main-exercise placement may have only one explicit Training Max test set per phase"
            }
            require(exercise.plannedSets.none { set ->
                set.optionalWorkKind == RoutineOptionalWorkKind.Joker &&
                    phasesThatDisallowJokers.any { phase -> set.routinePhaseIndex == null || set.routinePhaseIndex == phase }
            }) { "Joker sets are not allowed in deload or test phases" }
            (0 until program.phaseCount).forEach { phaseIndex ->
                val activeSets = exercise.plannedSets.filter { set ->
                    set.routinePhaseIndex == null || set.routinePhaseIndex == phaseIndex
                }
                validateFiveThreeOnePhasePolicy(exercise, phaseIndex, activeSets)
            }
        }
    }
    if (fiveThreeOneProgram) {
        val mainPlacementsByExercise = draft.days.flatMap(RoutineDayDraft::exercises)
            .filter { it.resolvedPlacementKind() == RoutinePlacementKind.MainExercise }
            .groupBy(RoutineExerciseDraft::exerciseId)
        program.phaseRoles.forEachIndexed { phaseIndex, role ->
            if (role.semanticRole() == RoutineProgramPhaseRole.TrainingMaxTest) {
                mainPlacementsByExercise.forEach { (_, placements) ->
                    require(placements.sumOf { placement ->
                        placement.plannedSets.count { set ->
                            set.routinePhaseIndex == phaseIndex &&
                                set.classification == WorkoutSetClassification.TrainingMaxTest
                        }
                    } == 1) {
                        "Each main exercise requires exactly one explicit Training Max test set in phase ${phaseIndex + 1}"
                    }
                }
            }
        }
        mainPlacementsByExercise.filterValues { it.size > 1 }
            .forEach { (_, placements) ->
                val expected = placements.first()
                require(placements.drop(1).all { candidate ->
                    candidate.trainingMaxValue == expected.trainingMaxValue &&
                        candidate.trainingMaxUnitId == expected.trainingMaxUnitId &&
                        candidate.trainingMaxSource == expected.trainingMaxSource &&
                        candidate.trainingMaxPercent == expected.trainingMaxPercent &&
                        candidate.trainingMaxBasisKind == expected.trainingMaxBasisKind &&
                        candidate.trainingMaxBasisValue == expected.trainingMaxBasisValue &&
                        candidate.trainingMaxBasisUnitId == expected.trainingMaxBasisUnitId &&
                        candidate.trainingMaxIncreaseEligible == expected.trainingMaxIncreaseEligible &&
                        candidate.cycleIncrementValue == expected.cycleIncrementValue &&
                        candidate.mainWorkScheme == expected.mainWorkScheme &&
                        candidate.supplementalScheme == expected.supplementalScheme &&
                        candidate.jokerSetsEnabled == expected.jokerSetsEnabled &&
                        candidate.plannedSets.filterNot { it.classification == WorkoutSetClassification.TrainingMaxTest } ==
                            expected.plannedSets.filterNot { it.classification == WorkoutSetClassification.TrainingMaxTest }
                }) {
                    "Repeated 5/3/1 main-exercise placements must share the same training max, progression, and non-test prescriptions"
                }
            }
    }
}

private data class ResolvedActiveProgramPolicy(
    val mainWorkScheme: RoutineMainWorkScheme,
    val supplementalScheme: RoutineSupplementalScheme,
    val jokerSetsEnabled: Boolean,
)

private fun resolveActiveProgramPolicy(
    baseMainWorkScheme: RoutineMainWorkScheme,
    baseSupplementalScheme: RoutineSupplementalScheme,
    activeSets: List<WorkoutSetDraft>,
): ResolvedActiveProgramPolicy {
    val mainSets = activeSets.filter { it.workSection == RoutineWorkSection.Main }
    val supplementalSets = activeSets.filter { it.workSection == RoutineWorkSection.Supplemental }
    val explicitMainSchemes = mainSets.mapNotNull(WorkoutSetDraft::mainWorkScheme).distinct()
    require(explicitMainSchemes.size <= 1) { "Active Main sets disagree about their main-work scheme" }
    val inferredMain = when {
        mainSets.isEmpty() -> RoutineMainWorkScheme.Unspecified
        mainSets.any { it.classification == WorkoutSetClassification.Amrap } -> RoutineMainWorkScheme.ClassicPrSet
        else -> RoutineMainWorkScheme.ClassicMinimumReps
    }
    val resolvedMain = explicitMainSchemes.singleOrNull() ?: when (baseMainWorkScheme) {
        RoutineMainWorkScheme.ClassicPrSet -> baseMainWorkScheme.takeIf {
            mainSets.any { set -> set.classification == WorkoutSetClassification.Amrap }
        } ?: inferredMain
        RoutineMainWorkScheme.FivesPro -> baseMainWorkScheme.takeIf {
            mainSets.isNotEmpty() && mainSets.all { set ->
                set.reps == 5 && set.classification != WorkoutSetClassification.Amrap
            }
        } ?: inferredMain
        RoutineMainWorkScheme.ClassicMinimumReps -> baseMainWorkScheme.takeIf {
            mainSets.none { set -> set.classification == WorkoutSetClassification.Amrap }
        } ?: inferredMain
        RoutineMainWorkScheme.Unspecified -> inferredMain
    }

    val explicitSupplementalSchemes = supplementalSets.mapNotNull(WorkoutSetDraft::supplementalScheme).distinct()
    require(explicitSupplementalSchemes.size <= 1) {
        "Active Supplemental sets disagree about their supplemental scheme"
    }
    val mainPercentages = mainSets.map { it.loadPercentage }
    val supplementalPercentages = supplementalSets.map { it.loadPercentage }
    val fiveByFive = supplementalSets.size == 5 && supplementalSets.all { it.reps == 5 }
    val inferredSupplemental = when {
        supplementalSets.isEmpty() -> RoutineSupplementalScheme.None
        supplementalSets.size == 5 && supplementalSets.all { it.reps == 10 } ->
            RoutineSupplementalScheme.BoringButBig
        supplementalSets.size == 10 && supplementalSets.all { it.reps == 5 } ->
            RoutineSupplementalScheme.BoringButStrong
        fiveByFive && mainPercentages.getOrNull(0) != null &&
            supplementalPercentages.all { it == mainPercentages[0] } -> RoutineSupplementalScheme.FirstSetLast
        fiveByFive && mainPercentages.getOrNull(1) != null &&
            supplementalPercentages.all { it == mainPercentages[1] } -> RoutineSupplementalScheme.SecondSetLast
        else -> RoutineSupplementalScheme.Custom
    }
    val resolvedSupplemental = explicitSupplementalSchemes.singleOrNull() ?: when {
        inferredSupplemental != RoutineSupplementalScheme.Custom -> inferredSupplemental
        baseSupplementalScheme in setOf(
            RoutineSupplementalScheme.FirstSetLast,
            RoutineSupplementalScheme.SecondSetLast,
        ) && fiveByFive -> baseSupplementalScheme
        else -> RoutineSupplementalScheme.Custom
    }

    return ResolvedActiveProgramPolicy(
        mainWorkScheme = resolvedMain,
        supplementalScheme = resolvedSupplemental,
        jokerSetsEnabled = activeSets.any { set ->
            set.workSection == RoutineWorkSection.Optional &&
                set.optionalWorkKind == RoutineOptionalWorkKind.Joker
        },
    )
}

private fun validateFiveThreeOnePhasePolicy(
    exercise: RoutineExerciseDraft,
    phaseIndex: Int,
    activeSets: List<WorkoutSetDraft>,
) {
    val policy = resolveActiveProgramPolicy(
        baseMainWorkScheme = exercise.mainWorkScheme,
        baseSupplementalScheme = exercise.supplementalScheme,
        activeSets = activeSets,
    )
    val phaseLabel = "phase ${phaseIndex + 1}"
    val mainSets = activeSets.filter { it.workSection == RoutineWorkSection.Main }
    val supplementalSets = activeSets.filter { it.workSection == RoutineWorkSection.Supplemental }
    when (policy.mainWorkScheme) {
        RoutineMainWorkScheme.ClassicPrSet -> require(
            mainSets.any { it.classification == WorkoutSetClassification.Amrap },
        ) { "Classic PR-set policy requires an active Main AMRAP set in $phaseLabel" }
        RoutineMainWorkScheme.FivesPro -> require(
            mainSets.isNotEmpty() && mainSets.all {
                it.reps == 5 && it.classification != WorkoutSetClassification.Amrap
            },
        ) { "5s PRO requires every active Main set to prescribe 5 reps without AMRAP in $phaseLabel" }
        RoutineMainWorkScheme.ClassicMinimumReps -> require(
            mainSets.none { it.classification == WorkoutSetClassification.Amrap },
        ) { "Minimum-reps Main work cannot contain an AMRAP set in $phaseLabel" }
        RoutineMainWorkScheme.Unspecified -> Unit
    }
    when (policy.supplementalScheme) {
        RoutineSupplementalScheme.None -> require(supplementalSets.isEmpty()) {
            "No-supplemental policy cannot contain Supplemental sets in $phaseLabel"
        }
        RoutineSupplementalScheme.BoringButBig -> require(
            supplementalSets.size == 5 && supplementalSets.all { it.reps == 10 },
        ) { "BBB requires exactly 5 Supplemental sets of 10 reps in $phaseLabel" }
        RoutineSupplementalScheme.BoringButStrong -> require(
            supplementalSets.size == 10 && supplementalSets.all { it.reps == 5 },
        ) { "Boring But Strong requires exactly 10 Supplemental sets of 5 reps in $phaseLabel" }
        RoutineSupplementalScheme.FirstSetLast,
        RoutineSupplementalScheme.SecondSetLast,
        -> {
            require(supplementalSets.size == 5 && supplementalSets.all { it.reps == 5 }) {
                "FSL and SSL require exactly 5 Supplemental sets of 5 reps in $phaseLabel"
            }
            val mainIndex = if (policy.supplementalScheme == RoutineSupplementalScheme.FirstSetLast) 0 else 1
            mainSets.getOrNull(mainIndex)?.loadPercentage?.let { expectedPercentage ->
                require(supplementalSets.all { it.loadPercentage == expectedPercentage }) {
                    "${policy.supplementalScheme.name} Supplemental load must match active Main set ${mainIndex + 1} in $phaseLabel"
                }
            }
        }
        RoutineSupplementalScheme.Custom -> Unit
    }
    val jokers = activeSets.filter {
        it.workSection == RoutineWorkSection.Optional && it.optionalWorkKind == RoutineOptionalWorkKind.Joker
    }
    require(jokers.size <= 3) { "A 5/3/1 phase may contain at most three Joker candidates" }
    if (jokers.isNotEmpty()) {
        val topMain = requireNotNull(mainSets.lastOrNull()?.loadPercentage) {
            "Joker candidates require programmed Main work in $phaseLabel"
        }
        val percentages = jokers.map { joker ->
            requireNotNull(joker.loadPercentage) { "Joker candidates require a Training Max percentage" }
        }
        val steps = (listOf(topMain) + percentages).zipWithNext { previous, next -> next - previous }
        require(steps.all { step -> kotlin.math.abs(step - steps.first()) <= 1e-9 } &&
            steps.first().let { step -> kotlin.math.abs(step - 5.0) <= 1e-9 || kotlin.math.abs(step - 10.0) <= 1e-9 }
        ) { "Joker candidates must form ordered 5% or 10% Training Max steps in $phaseLabel" }
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
    trainingMaxIncreaseEligible = trainingMaxIncreaseEligible,
    programPhaseRoles = programPhaseRolesCsv.parseCsvStrings().map {
        runCatching { RoutineProgramPhaseRole.valueOf(it) }.getOrDefault(RoutineProgramPhaseRole.Standard)
    },
    trainingMaxAdvanceAfterPhaseIndices = trainingMaxAdvanceAfterPhaseIndicesCsv.parseIntCsv().toSet(),
    programTemplateKey = runCatching { RoutineProgramTemplateKey.valueOf(programTemplateKey) }
        .getOrDefault(RoutineProgramTemplateKey.None),
    programTemplateRevision = programTemplateRevision,
    progressionMode = runCatching { RoutineProgressionMode.valueOf(progressionMode) }
        .getOrDefault(RoutineProgressionMode.Standard),
    allowNonStandardHigherSuggestions = allowNonStandardHigherSuggestions,
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
    trainingMaxBasisKind = runCatching { TrainingMaxBasisKind.valueOf(trainingMaxBasisKind) }
        .getOrDefault(TrainingMaxBasisKind.Unspecified),
    trainingMaxBasisValue = trainingMaxBasisValue,
    trainingMaxBasisUnitId = trainingMaxBasisUnitId,
    trainingMaxIncreaseEligible = trainingMaxIncreaseEligible,
    mainWorkScheme = runCatching { RoutineMainWorkScheme.valueOf(mainWorkScheme) }
        .getOrDefault(RoutineMainWorkScheme.Unspecified),
    supplementalScheme = runCatching { RoutineSupplementalScheme.valueOf(supplementalScheme) }
        .getOrDefault(RoutineSupplementalScheme.None),
    placementKind = runCatching { RoutinePlacementKind.valueOf(placementKind) }
        .getOrDefault(RoutinePlacementKind.General),
    assistanceCategory = runCatching { RoutineAssistanceCategory.valueOf(assistanceCategory) }
        .getOrDefault(RoutineAssistanceCategory.Unspecified),
    jokerSetsEnabled = jokerSetsEnabled,
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
        workSection = runCatching { RoutineWorkSection.valueOf(workSection) }
            .getOrDefault(RoutineWorkSection.Unspecified),
        optionalWorkKind = runCatching { RoutineOptionalWorkKind.valueOf(optionalWorkKind) }
            .getOrDefault(RoutineOptionalWorkKind.None),
        mainWorkScheme = mainWorkScheme.takeIf(String::isNotBlank)?.let { value ->
            runCatching { RoutineMainWorkScheme.valueOf(value) }.getOrNull()
        },
        supplementalScheme = supplementalScheme.takeIf(String::isNotBlank)?.let { value ->
            runCatching { RoutineSupplementalScheme.valueOf(value) }.getOrNull()
        },
    ),
    createdAtMillis, updatedAtMillis,
)

private fun TrainingMaxDecisionEntity.toDomain() = TrainingMaxDecision(
    uuid = uuid,
    routineUuid = routineUuid,
    sessionUuid = sessionUuid,
    exerciseUuid = exerciseUuid,
    exerciseName = exerciseName,
    cycle = cycle,
    previousTrainingMax = previousTrainingMax,
    appliedDelta = appliedDelta,
    resultingTrainingMax = resultingTrainingMax,
    unitId = unitId,
    standardDelta = standardDelta,
    recommendationCategory = recommendationCategory,
    recommendationDelta = recommendationDelta,
    confidence = confidence,
    reasons = reasonsText.lineSequence().filter(String::isNotBlank).toList(),
    engineVersion = engineVersion,
    action = runCatching { TrainingMaxDecisionAction.valueOf(action) }
        .getOrDefault(TrainingMaxDecisionAction.Custom),
    createdAtMillis = createdAtMillis,
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
private fun GraphPresetEntity.toDomain() = GraphPreset(id, uuid, name, exerciseIdsCsv.split(',').mapNotNull(String::toLongOrNull), measurement, dateRange, aggregation, archived, createdAtMillis, updatedAtMillis)

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
    workSection = workSection.name,
    optionalWorkKind = optionalWorkKind.name,
    mainWorkScheme = mainWorkScheme?.name.orEmpty(),
    supplementalScheme = supplementalScheme?.name.orEmpty(),
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
    val loadInterpretation = runCatching { LoadInterpretation.valueOf(workoutExercise.loadInterpretationSnapshot) }
        .getOrDefault(LoadInterpretation.Total)
    require(
        prescription == RoutineLoadPrescriptionType.Absolute ||
            machineType != MachineLoadType.Level && loadInterpretation.supportsRoutinePercentagePrescription(),
    ) {
        "Percentage prescriptions require a mass-based exercise or machine"
    }
    if (prescription == RoutineLoadPrescriptionType.PercentOneRepMax) {
        require(sourceExercise.trackingType == ExerciseTrackingType.WeightReps.name) {
            "Estimated 1RM prescriptions require a Weight + Reps exercise"
        }
    } else if (prescription == RoutineLoadPrescriptionType.PercentTrainingMax) {
        require(sourceExercise.trackingType in setOf(
            ExerciseTrackingType.WeightReps.name,
            ExerciseTrackingType.WeightOnly.name,
            ExerciseTrackingType.WeightDuration.name,
        )) { "Training Max prescriptions require a mass-tracked exercise" }
    }
    val setLoadMultiplier = loadInterpretationMultiplier(
        loadInterpretation,
        runCatching { MachineStackMode.valueOf(workoutExercise.machineStackModeSnapshot) }
            .getOrDefault(MachineStackMode.Single),
        workoutExercise.machinePulleyRatioSnapshot,
        unilateral,
    )
    val resolvedLoad = resolveRoutinePrescribedLoad(
        type = prescription,
        enteredWeight = enteredWeight,
        enteredUnitId = effectiveWeightUnitId,
        percentage = loadPercentage,
        oneRepMaxKg = oneRepMaxKg,
        trainingMaxPercent = trainingMaxPercent,
        progressionPercent = progressionPercent,
        loadMultiplier = setLoadMultiplier,
        baseLoadKg = workoutExercise.baseLoadKgSnapshot,
        addOnPlateKg = workoutExercise.machineAddOnPlateKgSnapshot,
        availableLoads = machine?.availableLoadsCsv?.parseDoubleCsv().orEmpty(),
        increment = sourceExercise.weightIncrement.takeIf { effectiveWeightUnitId == sourceExercise.weightUnitId }
            ?: if (effectiveWeightUnitId == "pound") 5.0 else 2.5,
        explicitTrainingMaxKg = explicitTrainingMaxKg,
    )
    val effectiveWeight = resolvedLoad?.displayValue ?: enteredWeight.takeUnless { machineType == MachineLoadType.Level }
    val explicitMachineSetting = machineLoadValue.takeIf { machineType == MachineLoadType.Level }
    val configuredMachineSetting = machine
        ?.takeIf { candidate ->
            machineType == MachineLoadType.Level &&
                candidate.uuid == workoutExercise.machineProfileUuidSnapshot &&
                candidate.loadType == MachineLoadType.Level.name
        }
        ?.let { candidate ->
            configuredMachineLevelDefault(
                candidate.availableLoadsCsv.parseDoubleCsv(),
                runCatching { MachineLevelDirection.valueOf(candidate.levelDirection) }
                    .getOrDefault(MachineLevelDirection.HigherNumberMoreResistance),
            )
        }
    // Routine templates have no current-placement or completed-history context. A blank level
    // field therefore receives only the same live-profile endpoint used by ad-hoc workout adds.
    val actualMachineSetting = if (machineType == MachineLoadType.Level) {
        resolveMachineLevelDefault(
            explicitValue = explicitMachineSetting,
            latestSamePlacementValue = null,
            latestCompletedProfileValue = null,
            configuredEndpointValue = configuredMachineSetting,
        )
    } else null
    val weightUnit = BuiltInUnits.get(effectiveWeightUnitId)
    val distanceUnit = enteredDistanceUnitId?.let(BuiltInUnits::get)
    return WorkoutSetEntity(
        uuid = uuid, workoutExerciseId = workoutExerciseId, position = position,
        classification = classification, planned = true, completed = false,
        canonicalWeightKg = canonicalResistanceKg(
            enteredValue = effectiveWeight,
            enteredUnitId = effectiveWeightUnitId,
            machineSetting = actualMachineSetting,
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
        machineLoadValue = actualMachineSetting ?: effectiveWeight.takeIf { workoutExercise.machineId != null },
        unilateral = unilateral,
        prescribedCanonicalWeightKg = if (
            machineType == MachineLoadType.Level && explicitMachineSetting == null
        ) null else canonicalResistanceKg(
            enteredValue = effectiveWeight,
            enteredUnitId = effectiveWeightUnitId,
            machineSetting = explicitMachineSetting,
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
        prescribedMachineLoadValue = explicitMachineSetting ?: effectiveWeight.takeIf { workoutExercise.machineId != null },
        prescriptionSourceLabel = resolvedLoad?.label.orEmpty(),
        workSectionSnapshot = workSection,
        optionalWorkKindSnapshot = optionalWorkKind,
        prescribedClassificationSnapshot = classification,
        requiredForProgressionSnapshot = workSection != RoutineWorkSection.Optional.name,
        removalReason = null,
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
    val snapped = if (type == RoutineLoadPrescriptionType.Absolute && progressionPercent == 100.0) {
        rawDisplay
    } else if (choices.isNotEmpty()) {
        // Match 5/3/1 preview behavior exactly and break an exact tie conservatively downward,
        // independent of the order in which machine settings were entered.
        choices.minWith(compareBy<Double> { kotlin.math.abs(it - rawDisplay) }.thenBy { it })
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
private fun String.parseIntCsv(): List<Int> = split(',').mapNotNull(String::toIntOrNull)
private fun String.parseCsvStrings(): List<String> = split(',').map(String::trim).filter(String::isNotBlank)
private fun String.toMainWorkScheme(): RoutineMainWorkScheme =
    runCatching { RoutineMainWorkScheme.valueOf(this) }.getOrDefault(RoutineMainWorkScheme.Unspecified)
private fun String.toSupplementalScheme(): RoutineSupplementalScheme =
    runCatching { RoutineSupplementalScheme.valueOf(this) }.getOrDefault(RoutineSupplementalScheme.None)

private fun RoutineProgramDraft?.normalizedForCreate(): RoutineProgramDraft =
    this?.normalized() ?: RoutineProgramDraft(RoutineProgramKind.Static, 1)

private fun RoutineProgramDraft.normalized(): RoutineProgramDraft = when (kind) {
    RoutineProgramKind.Static -> copy(
        phaseCount = 1,
        phaseLabels = emptyList(),
        phaseRoles = emptyList(),
        trainingMaxAdvanceAfterPhaseIndices = emptySet(),
        templateKey = RoutineProgramTemplateKey.None,
        templateRevision = 0,
    )
    else -> copy(
        phaseCount = phaseCount.coerceAtLeast(1),
        phaseLabels = phaseLabels.map(String::trim).filter(String::isNotBlank).take(phaseCount.coerceAtLeast(1)),
        phaseRoles = phaseRoles.take(phaseCount.coerceAtLeast(1)),
        trainingMaxAdvanceAfterPhaseIndices = trainingMaxAdvanceAfterPhaseIndices
            .filterTo(sortedSetOf()) { it in 0 until phaseCount.coerceAtLeast(1) },
    )
}

/** Saving performed history as a static routine must not re-mint old program or failure state. */
private fun WorkoutSetEntity.toWorkoutReuseDraft() = WorkoutSetDraft(
    weight = enteredWeight, weightUnitId = enteredWeightUnitId ?: "kilogram", reps = repetitions,
    repsMax = null,
    distance = enteredDistance, distanceUnitId = enteredDistanceUnitId ?: "kilometre",
    durationSeconds = durationSeconds, bodyweightKg = bodyweightKg,
    planned = true, completed = false,
    classification = WorkoutSetClassification.Working, note = note,
    rpe = rpe, rir = rir, tempo = tempo, restSeconds = restSeconds,
    machineLoadValue = machineLoadValue,
    unilateral = unilateral,
    workSection = RoutineWorkSection.Unspecified,
    optionalWorkKind = RoutineOptionalWorkKind.None,
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
    prescribedRepetitionsMax = prescribedRepetitionsMax,
    prescriptionSourceLabel = prescriptionSourceLabel,
    workSectionSnapshot = runCatching { RoutineWorkSection.valueOf(workSectionSnapshot) }
        .getOrDefault(RoutineWorkSection.Unspecified),
    optionalWorkKindSnapshot = runCatching { RoutineOptionalWorkKind.valueOf(optionalWorkKindSnapshot) }
        .getOrDefault(RoutineOptionalWorkKind.None),
    prescribedClassificationSnapshot = runCatching {
        WorkoutSetClassification.valueOf(prescribedClassificationSnapshot)
    }.getOrDefault(WorkoutSetClassification.Working),
)
