package com.whip.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.whip.app.WhipApplication
import com.whip.app.core.OperationStatus
import com.whip.app.data.GymRepository
import com.whip.app.data.RoutineRepository
import com.whip.app.domain.Exercise
import com.whip.app.domain.ExerciseCategory
import com.whip.app.domain.ExerciseCategoryLink
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.WorkoutExercise
import com.whip.app.domain.WorkoutGroup
import com.whip.app.domain.WorkoutGroupType
import com.whip.app.domain.WorkoutSession
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.domain.WorkoutSet
import com.whip.app.domain.WorkoutSetDraft
import com.whip.app.domain.WorkoutSummary
import com.whip.app.domain.GymRoutine
import com.whip.app.domain.GymMachine
import com.whip.app.domain.GymMachineDraft
import com.whip.app.domain.GraphPreset
import com.whip.app.domain.PersonalRecord
import com.whip.app.domain.RoutineDay
import com.whip.app.domain.RoutineDraft
import com.whip.app.domain.RoutineExercise
import com.whip.app.domain.RoutineSet
import com.whip.app.domain.calculateWorkoutSummary
import com.whip.app.domain.equipmentScopeKey
import com.whip.app.data.MachineDeletionImpact
import com.whip.app.core.AppSettings
import com.whip.app.core.PlatePreset
import com.whip.app.core.RepPrescriptionScheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class WorkoutExerciseUi(
    val workoutExercise: WorkoutExercise,
    val exercise: Exercise,
    val sets: List<WorkoutSet>,
    val previousSets: List<WorkoutSet>,
    val previousSetCount: Int,
    val group: WorkoutGroup?,
    val machine: GymMachine?,
)

private const val PREVIOUS_SET_SUMMARY_LIMIT = 12

internal data class PreviousSetSummary(
    val sets: List<WorkoutSet>,
    val totalCount: Int,
)

internal fun summarizePreviousSets(
    allSets: List<WorkoutSet>,
    workoutExerciseId: Long,
    limit: Int = PREVIOUS_SET_SUMMARY_LIMIT,
): PreviousSetSummary {
    require(limit > 0)
    val visible = ArrayList<WorkoutSet>(limit)
    var total = 0
    allSets.forEach { set ->
        if (set.workoutExerciseId == workoutExerciseId && set.deletedAtMillis == null) {
            total += 1
            if (visible.size < limit) visible += set
        }
    }
    return PreviousSetSummary(visible, total)
}

data class GymUiState(
    val machines: List<GymMachine> = emptyList(),
    val archivedMachines: List<GymMachine> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val archivedExercises: List<Exercise> = emptyList(),
    val activeSession: WorkoutSession? = null,
    val activeWorkoutExercises: List<WorkoutExerciseUi> = emptyList(),
    val history: List<WorkoutSession> = emptyList(),
    val archivedWorkouts: List<WorkoutSession> = emptyList(),
    val allSessions: List<WorkoutSession> = emptyList(),
    val allWorkoutExercises: List<WorkoutExercise> = emptyList(),
    val allSets: List<WorkoutSet> = emptyList(),
    val summary: WorkoutSummary? = null,
    val restSecondsRemaining: Int? = null,
    val nowMillis: Long = 0,
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val routines: List<GymRoutine> = emptyList(),
    val archivedRoutines: List<GymRoutine> = emptyList(),
    val routineDays: List<RoutineDay> = emptyList(),
    val routineExercises: List<RoutineExercise> = emptyList(),
    val routineSets: List<RoutineSet> = emptyList(),
    val personalRecords: List<PersonalRecord> = emptyList(),
    val graphPresets: List<GraphPreset> = emptyList(),
    val categories: List<ExerciseCategory> = emptyList(),
    val archivedCategories: List<ExerciseCategory> = emptyList(),
    val categoryLinks: List<ExerciseCategoryLink> = emptyList(),
    val appSettings: AppSettings = AppSettings(),
)

internal data class QuickSetState(
    val id: Long,
    val workoutExerciseId: Long,
    val completed: Boolean,
    val deleted: Boolean,
)

/** Append only after every existing set in the active workout has been completed. */
internal fun shouldAppendAfterQuickSave(
    savedSetId: Long,
    activeWorkoutExerciseIds: Set<Long>,
    sets: List<QuickSetState>,
): Boolean = sets.none { set ->
    set.id != savedSetId &&
        set.workoutExerciseId in activeWorkoutExerciseIds &&
        !set.completed &&
        !set.deleted
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GymViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as WhipApplication
    private val repository: GymRepository = app.gymRepository
    private val routineRepository: RoutineRepository = app.routineRepository
    private val clock = app.clock
    private val restTimerScheduler = app.restTimerScheduler

    private val _operationStatus = MutableStateFlow<OperationStatus>(OperationStatus.Idle)
    val operationStatus: StateFlow<OperationStatus> = _operationStatus.asStateFlow()
    private val _machineDeletionImpact = MutableStateFlow<MachineDeletionImpact?>(null)
    val machineDeletionImpact: StateFlow<MachineDeletionImpact?> = _machineDeletionImpact.asStateFlow()
    private val _machineDeletionInProgress = MutableStateFlow(false)
    val machineDeletionInProgress: StateFlow<Boolean> = _machineDeletionInProgress.asStateFlow()
    private val _pendingMachineArchiveUndo = MutableStateFlow<Long?>(null)
    val pendingMachineArchiveUndo: StateFlow<Long?> = _pendingMachineArchiveUndo.asStateFlow()
    private val reloadKey = MutableStateFlow(0)
    private val savingQuickSetIds = mutableSetOf<Long>()

    private val gymBaseData = combine(
        repository.exercises,
        repository.sessions,
        repository.workoutExercises,
        repository.sets,
        repository.groups,
    ) { exercises, sessions, workoutExercises, sets, groups ->
        GymData(exercises, sessions, workoutExercises, sets, groups)
    }

    private val gymData = combine(
        gymBaseData,
        repository.categories,
        repository.categoryLinks,
        repository.machines,
    ) { base, categories, links, machines -> base.copy(categories = categories, categoryLinks = links, machines = machines) }

    private val routineBaseData = combine(
        routineRepository.routines,
        routineRepository.days,
        routineRepository.exercises,
        routineRepository.sets,
        routineRepository.personalRecords,
        ::RoutineBaseData,
    )

    private val routineData = combine(
        routineBaseData,
        routineRepository.graphPresets,
    ) { base, presets -> RoutineData(base, presets) }

    private val calculatedUiState = reloadKey.flatMapLatest {
        val dataState = combine(gymData, routineData, app.settingsRepository.settings) { data, routines, settings ->
            buildGymUiState(data, routines, clock.now().toEpochMilli(), settings)
        }.flowOn(Dispatchers.Default)
        combine(dataState, currentTimeFlow()) { state, now -> state.withClockTick(now) }
            .catch { error ->
                emit(GymUiState(nowMillis = clock.now().toEpochMilli(), loading = false, errorMessage = error.message ?: "Could not load gym data"))
            }
    }

    val uiState = calculatedUiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GymUiState(nowMillis = clock.now().toEpochMilli()),
    )

    fun consumeOperationStatus() {
        _operationStatus.value = OperationStatus.Idle
    }
    fun retryLoading() { reloadKey.value++ }

    fun savePlatePreset(preset: PlatePreset) {
        app.settingsRepository.update { current ->
            current.copy(
                platePresets = (current.platePresets.filterNot { it.name.equals(preset.name, true) } + preset)
                    .sortedBy { it.name.lowercase() },
            )
        }
    }

    fun saveRepPrescriptionScheme(scheme: RepPrescriptionScheme) {
        require(scheme.isValid()) { "Invalid rep prescription scheme" }
        app.settingsRepository.update { current ->
            val existingIndex = current.repPrescriptionSchemes.indexOfFirst { it.id == scheme.id }
            current.copy(
                repPrescriptionSchemes = if (existingIndex >= 0) {
                    current.repPrescriptionSchemes.toMutableList().also { it[existingIndex] = scheme }
                } else {
                    current.repPrescriptionSchemes + scheme
                },
            )
        }
    }

    fun deleteRepPrescriptionScheme(id: String) {
        app.settingsRepository.update { current ->
            current.copy(repPrescriptionSchemes = current.repPrescriptionSchemes.filterNot { it.id == id })
        }
    }

    fun saveMachine(id: Long?, draft: GymMachineDraft) = runOperation(
        if (id == null) "Creating machine…" else "Saving machine…",
        if (id == null) "Machine created" else "Machine saved",
    ) {
        if (id == null) repository.createMachine(draft) else repository.updateMachine(id, draft)
    }

    fun createMachineForRoutine(draft: GymMachineDraft, onCreated: (Long?) -> Unit) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Running("Creating machine…")
            try {
                val id = repository.createMachine(draft)
                onCreated(id)
                _operationStatus.value = OperationStatus.Succeeded("Machine created and selected")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                onCreated(null)
                _operationStatus.value = OperationStatus.Failed(error.message ?: "Could not create machine", error)
            }
        }
    }

    fun createMachineVersion(sourceId: Long, draft: GymMachineDraft) = runOperation(
        "Creating configuration version…",
        "Machine configuration version created",
    ) { repository.createMachineVersion(sourceId, draft) }

    fun createMachineAndAssign(workoutExerciseId: Long, draft: GymMachineDraft) = runOperation(
        "Creating and assigning machine…",
        "Machine created and assigned",
    ) {
        val machineId = repository.createMachine(draft)
        repository.setWorkoutExerciseMachine(workoutExerciseId, machineId)
    }

    fun setMachineArchived(id: Long, archived: Boolean) {
        val routineReferences = uiState.value.routineExercises.count { it.machineId == id }
        val success = when {
            !archived -> "Machine restored"
            routineReferences > 0 -> "Machine hidden from new workouts; $routineReferences existing routine placement${if (routineReferences == 1) "" else "s"} remain available"
            else -> "Machine archived; history preserved"
        }
        runOperation(
            if (archived) "Archiving machine…" else "Restoring machine…",
            success,
        ) {
            repository.setMachineArchived(id, archived)
            _pendingMachineArchiveUndo.value = id.takeIf { archived }
        }
    }

    fun undoLastMachineArchive() {
        val id = _pendingMachineArchiveUndo.value ?: return
        _pendingMachineArchiveUndo.value = null
        runOperation("Restoring machine…", "Machine restored") { repository.setMachineArchived(id, false) }
    }

    fun clearPendingMachineArchiveUndo() { _pendingMachineArchiveUndo.value = null }

    fun previewMachineDeletion(id: Long) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Running("Checking machine usage…")
            try {
                _machineDeletionImpact.value = app.domainDeletionCoordinator.previewMachineDeletion(id)
                _operationStatus.value = OperationStatus.Idle
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                _operationStatus.value = OperationStatus.Failed(error.message ?: "Could not inspect machine", error)
            }
        }
    }

    fun dismissMachineDeletion() { _machineDeletionImpact.value = null }

    fun confirmMachineDeletion() {
        val impact = _machineDeletionImpact.value ?: return
        if (_machineDeletionInProgress.value) return
        _machineDeletionInProgress.value = true
        runOperation("Deleting machine profile…", "Machine profile permanently deleted; workout history was preserved") {
            try {
                app.domainDeletionCoordinator.deleteMachine(impact.machineId, impact.revisionToken)
                _machineDeletionImpact.value = null
            } finally {
                _machineDeletionInProgress.value = false
            }
        }
    }

    fun deletePlatePreset(name: String) {
        app.settingsRepository.update { current ->
            current.copy(platePresets = current.platePresets.filterNot { it.name == name })
        }
    }

    fun saveExercise(id: Long?, draft: ExerciseDraft) = runOperation(
        if (id == null) "Creating exercise…" else "Saving exercise…",
        if (id == null) "Exercise created" else "Exercise saved",
    ) {
        if (id == null) repository.createExercise(draft) else repository.updateExercise(id, draft)
    }

    fun createExerciseForRoutine(draft: ExerciseDraft, onCreated: (Long?) -> Unit) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Running("Creating exercise…")
            try {
                val id = repository.createExercise(draft)
                onCreated(id)
                _operationStatus.value = OperationStatus.Succeeded("Exercise created and added")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                onCreated(null)
                _operationStatus.value = OperationStatus.Failed(error.message ?: "Could not create exercise", error)
            }
        }
    }

    fun createExerciseAndAdd(sessionId: Long, draft: ExerciseDraft) = runOperation(
        "Creating exercise…",
        "Exercise added to workout",
    ) {
        val exerciseId = repository.createExercise(draft)
        repository.addExerciseToWorkout(sessionId, exerciseId)
    }

    fun createExerciseAndSubstitute(workoutExerciseId: Long, draft: ExerciseDraft) = runOperation(
        "Creating substitution…",
        "Exercise created and substituted",
    ) {
        repository.substituteWorkoutExercise(workoutExerciseId, repository.createExercise(draft), null)
    }

    fun duplicateExercise(id: Long) = runOperation("Duplicating exercise…", "Exercise duplicated") {
        repository.duplicateExercise(id)
    }

    fun setExerciseArchived(id: Long, archived: Boolean) = runOperation(
        if (archived) "Archiving exercise…" else "Restoring exercise…",
        if (archived) "Exercise archived" else "Exercise restored",
    ) { repository.setExerciseArchived(id, archived) }

    fun deleteExercisePermanently(id: Long) = runOperation("Deleting exercise…", "Exercise permanently deleted") {
        app.domainDeletionCoordinator.deleteExercise(id)
    }

    fun setExerciseFavorite(id: Long, favorite: Boolean) = runOperation(
        "Updating exercise…",
        if (favorite) "Exercise favorited" else "Favorite removed",
    ) { repository.setExerciseFavorite(id, favorite) }

    fun reorderExercises(ids: List<Long>) = runOperation("Reordering exercises…", "Exercises reordered") {
        repository.reorderExercises(ids)
    }

    fun saveCategory(id: Long?, name: String, kind: String, colorArgb: Long?) = runOperation(
        if (id == null) "Creating category…" else "Saving category…",
        "Category saved",
    ) {
        if (id == null) repository.createCategory(name, kind, colorArgb)
        else repository.updateCategory(id, name, kind, colorArgb)
    }

    fun setCategoryArchived(id: Long, archived: Boolean) = runOperation("Updating category…", "Category updated") {
        repository.setCategoryArchived(id, archived)
    }

    fun reorderCategories(ids: List<Long>) = runOperation("Reordering categories…", "Categories reordered") {
        repository.reorderCategories(ids)
    }

    fun startWorkout(
        name: String = "",
        notes: String = "",
        date: LocalDate? = null,
        keepScreenAwake: Boolean? = null,
    ) = runOperation("Starting workout…", "Workout started") {
        repository.startWorkout(
            name,
            notes,
            localDate = date,
            zoneId = clock.zoneId(),
            keepScreenAwake = keepScreenAwake,
        )
    }

    fun updateWorkout(id: Long, name: String, notes: String, keepAwake: Boolean) = runOperation(
        "Saving workout…",
        "Workout saved",
    ) { repository.updateWorkout(id, name, notes, keepAwake) }

    fun finishWorkout(id: Long) = runOperation("Finishing workout…", "Workout saved to history") {
        val exerciseIds = uiState.value.activeWorkoutExercises.map { it.exercise.id }.distinct()
        repository.finishWorkout(id)
        restTimerScheduler.cancel(id)
        exerciseIds.forEach { routineRepository.rebuildPersonalRecords(it) }
        app.linkRepository.rebuildAll()
    }

    fun resumeWorkout(id: Long) = runOperation("Resuming workout…", "Workout resumed") {
        repository.resumeWorkout(id)
        app.linkRepository.rebuildAll()
    }

    fun discardWorkout(id: Long) = runOperation("Discarding workout…", "Workout discarded") {
        repository.discardWorkout(id)
        restTimerScheduler.cancel(id)
        app.linkRepository.rebuildAll()
    }

    fun restoreWorkout(id: Long) = runOperation("Restoring workout…", "Workout restored to history") {
        repository.restoreWorkout(id)
        app.linkRepository.rebuildAll()
    }

    fun deleteWorkoutPermanently(id: Long) = runOperation("Deleting workout…", "Workout permanently deleted") {
        restTimerScheduler.cancel(id)
        app.domainDeletionCoordinator.deleteWorkout(id)
    }

    fun duplicateWorkout(id: Long) = runOperation("Copying workout…", "Workout copied into today") {
        repository.duplicateWorkout(id, asActive = true)
    }

    fun copyWorkoutExercise(id: Long) = runOperation("Copying exercise…", "Exercise sets copied into the active workout") {
        repository.copyWorkoutExerciseToActive(id)
    }

    fun addExercise(sessionId: Long, exerciseId: Long, machineId: Long? = null) = runOperation(
        "Adding exercise…",
        "Exercise added",
    ) { repository.addExerciseToWorkout(sessionId, exerciseId, machineId) }

    fun updateWorkoutExercise(id: Long, notes: String, groupId: Long?) = runOperation(
        "Saving exercise notes…",
        "Exercise updated",
    ) { repository.updateWorkoutExercise(id, notes, groupId) }

    fun setWorkoutExerciseMachine(id: Long, machineId: Long?) = runOperation(
        "Updating machine…",
        "Machine selection updated",
    ) { repository.setWorkoutExerciseMachine(id, machineId) }

    fun substituteWorkoutExercise(id: Long, exerciseId: Long, machineId: Long?) = runOperation(
        "Substituting exercise…",
        "Substitution added · completed history preserved",
    ) { repository.substituteWorkoutExercise(id, exerciseId, machineId) }

    fun removeWorkoutExercise(id: Long) = runOperation("Removing exercise…", "Exercise removed from workout") {
        repository.removeWorkoutExercise(id)
    }

    fun reorderWorkoutExercises(sessionId: Long, ids: List<Long>) = runOperation(
        "Reordering exercises…",
        "Exercises reordered",
    ) { repository.reorderWorkoutExercises(sessionId, ids) }

    fun createGroup(
        sessionId: Long,
        name: String,
        type: WorkoutGroupType,
        workoutExerciseIds: List<Long>,
    ) = runOperation("Creating group…", "Exercise group created") {
        repository.createGroup(sessionId, name, type, workoutExerciseIds)
    }

    fun addSet(workoutExerciseId: Long) = runOperation("Adding set…", "Set added") {
        repository.addSet(workoutExerciseId)
    }

    fun updateSet(id: Long, draft: WorkoutSetDraft) = runOperation("Saving set…", "Set saved") {
        repository.updateSet(id, draft)
        rebuildRecordsForSet(id)
    }

    fun saveQuickSet(
        id: Long,
        workoutExerciseId: Long,
        draft: WorkoutSetDraft,
        addNext: Boolean,
    ) {
        if (!savingQuickSetIds.add(id)) return
        runOperation(
            "Saving set…",
            if (addNext) "Set saved · Next set ready" else "Set saved",
        ) {
            try {
                val workoutExercisesBefore = repository.workoutExercises.first()
                val currentPlacement = workoutExercisesBefore.firstOrNull { it.id == workoutExerciseId }
                    ?: error("Workout exercise no longer exists")
                val activePlacementIds = workoutExercisesBefore
                    .filter { it.sessionId == currentPlacement.sessionId }
                    .mapTo(mutableSetOf(), WorkoutExercise::id)
                val setStates = repository.sets.first().map { set ->
                    QuickSetState(
                        id = set.id,
                        workoutExerciseId = set.workoutExerciseId,
                        completed = set.completed,
                        deleted = set.deletedAtMillis != null,
                    )
                }
                val appendAfterSave = addNext && shouldAppendAfterQuickSave(id, activePlacementIds, setStates)

                repository.updateSet(id, draft.copy(completed = false, planned = false))
                repository.setSetCompleted(id, true, app.settingsRepository.current().restTimerAutoStart)
                rebuildRecordsForSet(id)
                schedulePersistedRestTimer()
                if (appendAfterSave) {
                    repository.addSet(
                        workoutExerciseId,
                        draft.copy(
                            planned = false,
                            completed = false,
                            note = "",
                            rpe = null,
                            rir = null,
                        ),
                    )
                }
            } finally {
                savingQuickSetIds.remove(id)
            }
        }
    }

    fun completeSet(id: Long, completed: Boolean) = runOperation(
        "Updating set…",
        if (completed) "Set completed" else "Set reopened",
    ) {
        repository.setSetCompleted(id, completed, app.settingsRepository.current().restTimerAutoStart)
        rebuildRecordsForSet(id)
        if (completed) schedulePersistedRestTimer()
    }

    fun duplicateSet(id: Long) = runOperation("Duplicating set…", "Set duplicated") {
        repository.duplicateSet(id)
    }

    fun deleteSet(id: Long) = runOperation("Removing set…", "Set removed · Undo is available from its menu") {
        repository.deleteSet(id)
        rebuildRecordsForSet(id)
    }

    fun undoDeleteSet(id: Long) = runOperation("Restoring set…", "Set restored") {
        repository.undoDeleteSet(id)
        rebuildRecordsForSet(id)
    }

    fun reorderSets(workoutExerciseId: Long, ids: List<Long>) = runOperation(
        "Reordering sets…",
        "Sets reordered",
    ) { repository.reorderSets(workoutExerciseId, ids) }

    fun startRestTimer(sessionId: Long, seconds: Int) = runOperation("Starting timer…", "Rest timer started") {
        repository.startRestTimer(sessionId, seconds)
        restTimerScheduler.schedule(sessionId, seconds, nextExerciseLabel())
    }

    fun adjustRestTimer(sessionId: Long, delta: Int) = runOperation("Adjusting timer…", "Timer adjusted") {
        repository.adjustRestTimer(sessionId, delta)
        schedulePersistedRestTimer()
    }

    fun stopRestTimer(sessionId: Long) = runOperation("Stopping timer…", "Timer stopped") {
        repository.stopRestTimer(sessionId)
        restTimerScheduler.cancel(sessionId)
    }

    fun saveRoutine(id: Long?, draft: RoutineDraft) = runOperation(
        if (id == null) "Creating routine…" else "Saving routine…",
        if (id == null) "Routine created" else "Routine saved",
    ) {
        if (id == null) routineRepository.createRoutine(draft)
        else routineRepository.updateRoutine(id, draft)
    }

    fun saveRoutineFromBuilder(id: Long?, draft: RoutineDraft, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Running(if (id == null) "Creating routine…" else "Saving routine…")
            try {
                if (id == null) routineRepository.createRoutine(draft) else routineRepository.updateRoutine(id, draft)
                _operationStatus.value = OperationStatus.Succeeded(if (id == null) "Routine created" else "Routine saved")
                onComplete(true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _operationStatus.value = OperationStatus.Failed(error.message ?: "Could not save routine", error)
                onComplete(false)
            }
        }
    }

    fun startRoutine(routineId: Long, dayId: Long?) = runOperation("Starting routine…", "Workout started") {
        routineRepository.startRoutine(routineId, dayId)
    }

    fun duplicateRoutine(id: Long) = runOperation("Duplicating routine…", "Routine duplicated") {
        routineRepository.duplicateRoutine(id)
    }

    fun setRoutineArchived(id: Long, archived: Boolean) = runOperation(
        if (archived) "Archiving routine…" else "Restoring routine…",
        if (archived) "Routine archived" else "Routine restored",
    ) { routineRepository.setRoutineArchived(id, archived) }

    fun deleteRoutinePermanently(id: Long) = runOperation("Deleting routine…", "Routine permanently deleted") {
        app.domainDeletionCoordinator.deleteRoutine(id)
    }

    fun setRoutinePinned(id: Long, pinned: Boolean) = runOperation("Updating routine…", "Routine updated") {
        routineRepository.setRoutinePinned(id, pinned)
    }

    fun reorderRoutines(ids: List<Long>) = runOperation("Reordering routines…", "Routines reordered") {
        routineRepository.reorderRoutines(ids)
    }

    fun saveWorkoutAsRoutine(sessionId: Long, name: String) = runOperation(
        "Saving routine…",
        "Workout saved as a routine",
    ) { routineRepository.saveWorkoutAsRoutine(sessionId, name) }

    fun saveGraphPreset(name: String, exerciseIds: List<Long>, metric: String, dateRange: String, aggregation: String) =
        runOperation("Saving graph preset…", "Graph preset saved") {
            routineRepository.saveGraphPreset(name, exerciseIds, metric, dateRange, aggregation)
        }

    fun updateGraphPreset(id: Long, name: String, exerciseIds: List<Long>, metric: String, dateRange: String, aggregation: String) =
        runOperation("Updating graph preset…", "Graph preset updated") {
            routineRepository.updateGraphPreset(id, name, exerciseIds, metric, dateRange, aggregation)
        }

    fun deleteGraphPreset(id: Long) = runOperation("Deleting graph preset…", "Graph preset deleted") {
        routineRepository.deleteGraphPreset(id)
    }

    private suspend fun rebuildRecordsForSet(setId: Long) {
        val state = uiState.value
        val set = state.allSets.firstOrNull { it.id == setId } ?: return
        val workoutExercise = state.allWorkoutExercises.firstOrNull { it.id == set.workoutExerciseId }
            ?: return
        routineRepository.rebuildPersonalRecords(workoutExercise.exerciseId)
    }

    private suspend fun schedulePersistedRestTimer() {
        val session = repository.sessions.first().firstOrNull { it.state == WorkoutSessionState.Active }
            ?: return
        val remaining = session.restTimerDeadlineMillis
            ?.let { ((it - clock.now().toEpochMilli() + 999) / 1_000).toInt() }
            ?.takeIf { it > 0 }
            ?: return
        restTimerScheduler.schedule(session.id, remaining, nextExerciseLabel())
    }

    private fun nextExerciseLabel(): String? {
        val state = uiState.value
        val next = state.activeWorkoutExercises.firstOrNull { item ->
            item.sets.any { !it.completed && it.deletedAtMillis == null }
        }
        return next?.let { "Next: ${it.exercise.name}" }
    }

    private fun runOperation(running: String, success: String, block: suspend () -> Unit) {
        _operationStatus.value = OperationStatus.Running(running)
        viewModelScope.launch {
            try {
                block()
                _operationStatus.value = OperationStatus.Succeeded(success)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _operationStatus.value = OperationStatus.Failed(
                    error.message ?: "Something went wrong",
                    error,
                )
            }
        }
    }

    private fun currentTimeFlow(): Flow<Long> = flow {
        while (currentCoroutineContext().isActive) {
            emit(clock.now().toEpochMilli())
            delay(1_000)
        }
    }
}

private data class GymData(
    val exercises: List<Exercise>,
    val sessions: List<WorkoutSession>,
    val workoutExercises: List<WorkoutExercise>,
    val sets: List<WorkoutSet>,
    val groups: List<WorkoutGroup>,
    val categories: List<ExerciseCategory> = emptyList(),
    val categoryLinks: List<ExerciseCategoryLink> = emptyList(),
    val machines: List<GymMachine> = emptyList(),
)

private data class RoutineBaseData(
    val routines: List<GymRoutine>,
    val days: List<RoutineDay>,
    val exercises: List<RoutineExercise>,
    val sets: List<RoutineSet>,
    val personalRecords: List<PersonalRecord>,
)

private data class RoutineData(
    val base: RoutineBaseData,
    val graphPresets: List<GraphPreset>,
)

private fun buildGymUiState(data: GymData, routineData: RoutineData, nowMillis: Long, appSettings: AppSettings): GymUiState {
    val exercisesById = data.exercises.associateBy(Exercise::id)
    val active = data.sessions.firstOrNull { it.state == WorkoutSessionState.Active }
    val activeWorkoutExercises = active?.let { session ->
        data.workoutExercises
            .filter { it.sessionId == session.id }
            .sortedBy(WorkoutExercise::position)
            .mapNotNull { workoutExercise ->
                val exercise = exercisesById[workoutExercise.exerciseId] ?: return@mapNotNull null
                val previousWorkoutExercise = data.workoutExercises
                    .asSequence()
                    .filter {
                        it.exerciseId == exercise.id && it.equipmentScopeKey == workoutExercise.equipmentScopeKey &&
                            it.sessionId != session.id
                    }
                    .mapNotNull { candidate ->
                        val candidateSession = data.sessions.firstOrNull { it.id == candidate.sessionId }
                        candidate.takeIf { candidateSession?.state == WorkoutSessionState.Finished }
                            ?.let { it to requireNotNull(candidateSession).startedAt }
                    }
                    .maxByOrNull { it.second }
                    ?.first
                val previousSummary = previousWorkoutExercise?.let { previous ->
                    summarizePreviousSets(data.sets, previous.id)
                } ?: PreviousSetSummary(emptyList(), 0)
                WorkoutExerciseUi(
                    workoutExercise = workoutExercise,
                    exercise = exercise,
                    sets = data.sets.filter { it.workoutExerciseId == workoutExercise.id },
                    previousSets = previousSummary.sets,
                    previousSetCount = previousSummary.totalCount,
                    group = workoutExercise.groupId?.let { groupId ->
                        data.groups.firstOrNull { it.id == groupId }
                    },
                    machine = workoutExercise.machineId?.let { machineId ->
                        data.machines.firstOrNull { it.id == machineId }
                    },
                )
            }
    }.orEmpty()
    val summary = active?.let { session ->
        calculateWorkoutSummary(
            session = session,
            workoutExercises = activeWorkoutExercises.map(WorkoutExerciseUi::workoutExercise),
            sets = activeWorkoutExercises.flatMap(WorkoutExerciseUi::sets),
            exercisesById = exercisesById,
            nowMillis = nowMillis,
            includeWarmups = appSettings.includeWarmupsInGymStats,
        )
    }
    val remaining = active?.restTimerDeadlineMillis?.let { deadline ->
        ((deadline - nowMillis + 999L) / 1_000L).coerceAtLeast(0).toInt()
    }?.takeIf { it > 0 }
    return GymUiState(
        machines = data.machines.filterNot(GymMachine::archived),
        archivedMachines = data.machines.filter(GymMachine::archived),
        exercises = data.exercises.filterNot(Exercise::archived),
        archivedExercises = data.exercises.filter(Exercise::archived),
        activeSession = active,
        activeWorkoutExercises = activeWorkoutExercises,
        history = data.sessions.filter {
            it.state == WorkoutSessionState.Finished && !it.archived
        }.sortedByDescending(WorkoutSession::startedAt),
        archivedWorkouts = data.sessions.filter(WorkoutSession::archived)
            .sortedByDescending(WorkoutSession::startedAt),
        allSessions = data.sessions,
        allWorkoutExercises = data.workoutExercises,
        allSets = data.sets,
        summary = summary,
        restSecondsRemaining = remaining,
        nowMillis = nowMillis,
        loading = false,
        routines = routineData.base.routines.filterNot(GymRoutine::archived),
        archivedRoutines = routineData.base.routines.filter(GymRoutine::archived),
        routineDays = routineData.base.days,
        routineExercises = routineData.base.exercises,
        routineSets = routineData.base.sets,
        personalRecords = routineData.base.personalRecords,
        graphPresets = routineData.graphPresets.filterNot(GraphPreset::archived),
        categories = data.categories.filterNot(ExerciseCategory::archived),
        archivedCategories = data.categories.filter(ExerciseCategory::archived),
        categoryLinks = data.categoryLinks,
        appSettings = appSettings,
    )
}

private fun GymUiState.withClockTick(now: Long): GymUiState {
    val active = activeSession
    val updatedSummary = active?.let { session ->
        calculateWorkoutSummary(
            session = session,
            workoutExercises = activeWorkoutExercises.map(WorkoutExerciseUi::workoutExercise),
            sets = activeWorkoutExercises.flatMap(WorkoutExerciseUi::sets),
            exercisesById = (exercises + archivedExercises).associateBy(Exercise::id),
            nowMillis = now,
            includeWarmups = appSettings.includeWarmupsInGymStats,
        )
    }
    val remaining = active?.restTimerDeadlineMillis?.let { deadline ->
        ((deadline - now + 999L) / 1_000L).coerceAtLeast(0).toInt()
    }?.takeIf { it > 0 }
    return copy(nowMillis = now, summary = updatedSummary, restSecondsRemaining = remaining)
}
