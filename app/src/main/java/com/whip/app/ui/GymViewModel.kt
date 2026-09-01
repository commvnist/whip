package com.whip.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.whip.app.WhipApplication
import com.whip.app.core.HomeSection
import com.whip.app.core.OperationFeedbackPresentation
import com.whip.app.core.OperationStatus
import com.whip.app.core.revealHomeSection
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
import com.whip.app.domain.PersonalRecord
import com.whip.app.domain.RoutineDay
import com.whip.app.domain.RoutineDraft
import com.whip.app.domain.RoutineExercise
import com.whip.app.domain.RoutineSet
import com.whip.app.domain.TrainingMaxDecision
import com.whip.app.domain.TrainingMaxCycleDecision
import com.whip.app.domain.calculateWorkoutSummary
import com.whip.app.domain.equipmentScopeKey
import com.whip.app.data.MachineDeletionImpact
import com.whip.app.core.AppSettings
import com.whip.app.core.PlatePreset
import com.whip.app.core.RepPrescriptionScheme
import com.whip.app.core.TrackedGymRecord
import com.whip.app.core.normalizeRestTimerPresets
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    val trainingMaxDecisions: List<TrainingMaxDecision> = emptyList(),
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
    private var pendingMachineArchiveId: Long? = null
    private var nextMachineArchiveUndoToken = 0L
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
        routineRepository.trainingMaxDecisions,
    ) { base, decisions -> base.copy(trainingMaxDecisions = decisions) }

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

    init {
        // Repair group invariants for a workout created by an older build before
        // the UI or rest-timer policy consumes its ordering.
        viewModelScope.launch {
            repository.sessions
                .map { sessions -> sessions.firstOrNull { it.state == WorkoutSessionState.Active }?.id }
                .distinctUntilChanged()
                .collectLatest { sessionId ->
                    if (sessionId != null) {
                        app.withUserDataAccess {
                            repository.normalizeWorkoutGroups(sessionId)
                            Unit
                        }
                    }
                }
        }
        viewModelScope.launch {
            app.userDataGeneration.drop(1).collect {
                pendingMachineArchiveId = null
                _pendingMachineArchiveUndo.value = null
                _machineDeletionImpact.value = null
                _machineDeletionInProgress.value = false
                _operationStatus.value = OperationStatus.Idle
            }
        }
    }

    fun consumeOperationStatus() {
        _operationStatus.value = OperationStatus.Idle
    }
    fun retryLoading() { reloadKey.value++ }

    fun savePlatePreset(preset: PlatePreset) {
        updateSettings { current ->
            current.copy(
                platePresets = (current.platePresets.filterNot { it.name.equals(preset.name, true) } + preset)
                    .sortedBy { it.name.lowercase() },
            )
        }
    }

    fun saveRepPrescriptionScheme(scheme: RepPrescriptionScheme) {
        require(scheme.isValid()) { "Invalid rep prescription scheme" }
        updateSettings { current ->
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

    fun reorderRepPrescriptionSchemes(schemes: List<RepPrescriptionScheme>) {
        updateSettings { current ->
            current.copy(repPrescriptionSchemes = schemes)
        }
    }

    fun updateTrackedGymRecords(records: List<TrackedGymRecord>) {
        updateSettings { current -> current.copy(trackedGymRecords = records) }
    }

    fun deleteRepPrescriptionScheme(id: String) {
        updateSettings { current ->
            current.copy(repPrescriptionSchemes = current.repPrescriptionSchemes.filterNot { it.id == id })
        }
    }

    fun saveMachine(id: Long?, draft: GymMachineDraft, onFinished: (Boolean) -> Unit = {}) = runOperation(
        if (id == null) "Creating machine…" else "Saving machine…",
        if (id == null) "Machine created" else "Machine saved",
        onFinished,
    ) {
        if (id == null) repository.createMachine(draft) else repository.updateMachine(id, draft)
    }

    fun createMachineForRoutine(draft: GymMachineDraft, onCreated: (Long?) -> Unit) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Running("Creating machine…")
            try {
                val id = checkNotNull(app.withUserDataAccess { repository.createMachine(draft) }) {
                    "Whip data is unavailable while recovery is in progress"
                }
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

    fun createMachineVersion(sourceId: Long, draft: GymMachineDraft, onFinished: (Boolean) -> Unit = {}) = runOperation(
        "Creating configuration version…",
        "Machine configuration version created",
        onFinished,
    ) { repository.createMachineVersion(sourceId, draft) }

    fun createMachineAndAssign(workoutExerciseId: Long, draft: GymMachineDraft, onFinished: (Boolean) -> Unit = {}) = runOperation(
        "Creating and assigning machine…",
        "Machine created and assigned",
        onFinished,
    ) {
        val machineId = repository.createMachine(draft)
        repository.setWorkoutExerciseMachine(workoutExerciseId, machineId)
    }

    fun setMachineArchived(id: Long, archived: Boolean) {
        if (!archived && pendingMachineArchiveId == id) {
            pendingMachineArchiveId = null
            _pendingMachineArchiveUndo.value = null
        }
        val undoToken = (++nextMachineArchiveUndoToken).takeIf { archived }
        val routineReferences = uiState.value.routineExercises.count { it.machineId == id }
        val success = when {
            !archived -> "Machine restored"
            routineReferences > 0 -> "Machine hidden from new workouts; $routineReferences existing routine placement${if (routineReferences == 1) "" else "s"} remain available"
            else -> "Machine archived; history preserved"
        }
        runOperation(
            if (archived) "Archiving machine…" else "Restoring machine…",
            success,
            successFeedbackPresentation = if (archived) {
                OperationFeedbackPresentation.Snackbar
            } else {
                OperationFeedbackPresentation.Inline
            },
            recoveryToken = undoToken,
        ) {
            repository.setMachineArchived(id, archived)
            pendingMachineArchiveId = id.takeIf { archived }
            _pendingMachineArchiveUndo.value = undoToken
        }
    }

    fun undoLastMachineArchive(expectedToken: Long) {
        if (_pendingMachineArchiveUndo.value != expectedToken) return
        val id = pendingMachineArchiveId ?: return
        _pendingMachineArchiveUndo.value = null
        pendingMachineArchiveId = null
        runOperation("Restoring machine…", "Machine restored") { repository.setMachineArchived(id, false) }
    }

    fun clearPendingMachineArchiveUndo(expectedToken: Long) {
        if (_pendingMachineArchiveUndo.value == expectedToken) {
            _pendingMachineArchiveUndo.value = null
            pendingMachineArchiveId = null
        }
    }

    fun previewMachineDeletion(id: Long) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Running("Checking machine usage…")
            try {
                _machineDeletionImpact.value = checkNotNull(app.withUserDataAccess {
                    checkNotNull(app.domainDeletionCoordinator.previewMachineDeletion(id)) {
                        "Machine no longer exists"
                    }
                }) { "Whip data is unavailable while recovery is in progress" }
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
        runOperation(
            "Deleting machine profile…",
            "Machine profile permanently deleted; workout history was preserved",
            successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
        ) {
            try {
                app.domainDeletionCoordinator.deleteMachine(impact.machineId, impact.revisionToken)
                _machineDeletionImpact.value = null
            } finally {
                _machineDeletionInProgress.value = false
            }
        }
    }

    fun deletePlatePreset(name: String) {
        updateSettings { current ->
            current.copy(platePresets = current.platePresets.filterNot { it.name == name })
        }
    }

    fun saveExercise(id: Long?, draft: ExerciseDraft, onFinished: (Boolean) -> Unit = {}) = runOperation(
        if (id == null) "Creating exercise…" else "Saving exercise…",
        if (id == null) "Exercise created" else "Exercise saved",
        onFinished,
    ) {
        if (id == null) repository.createExercise(draft) else repository.updateExercise(id, draft)
    }

    fun createExerciseForRoutine(draft: ExerciseDraft, onCreated: (Long?) -> Unit) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Running("Creating exercise…")
            try {
                val id = checkNotNull(app.withUserDataAccess { repository.createExercise(draft) }) {
                    "Whip data is unavailable while recovery is in progress"
                }
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

    fun createExerciseForMachine(draft: ExerciseDraft, onCreated: (Long?) -> Unit) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Running("Creating exercise…")
            try {
                val id = checkNotNull(app.withUserDataAccess { repository.createExercise(draft) }) {
                    "Whip data is unavailable while recovery is in progress"
                }
                onCreated(id)
                _operationStatus.value = OperationStatus.Succeeded("Exercise created and linked")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                onCreated(null)
                _operationStatus.value = OperationStatus.Failed(error.message ?: "Could not create exercise", error)
            }
        }
    }

    fun createExerciseAndAdd(sessionId: Long, draft: ExerciseDraft, onFinished: (Boolean) -> Unit = {}) = runOperation(
        "Creating exercise…",
        "Exercise added to workout",
        onFinished,
    ) {
        val exerciseId = repository.createExercise(draft)
        repository.addExerciseToWorkout(sessionId, exerciseId)
    }

    fun createExerciseAndSubstitute(workoutExerciseId: Long, draft: ExerciseDraft, onFinished: (Boolean) -> Unit = {}) = runOperation(
        "Creating substitution…",
        "Exercise created and substituted",
        onFinished,
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

    fun deleteExercisePermanently(id: Long) = runOperation(
        "Deleting exercise…",
        "Exercise permanently deleted",
        successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
    ) {
        val exerciseUuid = (uiState.value.exercises + uiState.value.archivedExercises)
            .firstOrNull { it.id == id }?.uuid
        app.domainDeletionCoordinator.deleteExercise(id)
        if (exerciseUuid != null) {
            app.settingsRepository.update { current ->
                current.copy(trackedGymRecords = current.trackedGymRecords.filterNot { it.exerciseUuid == exerciseUuid })
            }
        }
    }

    fun setExerciseFavorite(id: Long, favorite: Boolean) = runOperation(
        "Updating exercise…",
        if (favorite) "Exercise favorited" else "Favorite removed",
    ) { repository.setExerciseFavorite(id, favorite) }

    fun reorderExercises(ids: List<Long>) = runSilentReorder {
        repository.reorderExercises(ids)
    }

    fun saveCategory(id: Long?, name: String, kind: String) = runOperation(
        if (id == null) "Creating category…" else "Saving category…",
        "Category saved",
    ) {
        if (id == null) repository.createCategory(name, kind)
        else repository.updateCategory(id, name, kind)
    }

    fun setCategoryArchived(id: Long, archived: Boolean) = runOperation(
        "Updating category…",
        "Category updated",
    ) {
        repository.setCategoryArchived(id, archived)
    }

    fun reorderCategories(ids: List<Long>) = runSilentReorder {
        repository.reorderCategories(ids)
    }

    fun startWorkout(
        name: String = "",
        notes: String = "",
        date: LocalDate? = null,
        keepScreenAwake: Boolean? = null,
        onFinished: (Boolean) -> Unit = {},
    ) = runOperation("Starting workout…", "Workout started", onFinished) {
        repository.startWorkout(
            name,
            notes,
            localDate = date,
            zoneId = clock.zoneId(),
            keepScreenAwake = keepScreenAwake,
        )
    }

    fun updateWorkout(
        id: Long,
        name: String,
        notes: String,
        keepAwake: Boolean,
        onFinished: (Boolean) -> Unit = {},
    ) = runOperation(
        "Saving workout…",
        "Workout saved",
        onFinished,
    ) { repository.updateWorkout(id, name, notes, keepAwake) }

    fun finishWorkout(
        id: Long,
        trainingMaxDecisions: List<TrainingMaxCycleDecision>? = null,
        onFinished: (Boolean) -> Unit = {},
    ) = runOperation(
        "Finishing workout…",
        "Workout saved to history",
        onFinished = onFinished,
    ) {
        val exerciseIds = uiState.value.activeWorkoutExercises.map { it.exercise.id }.distinct()
        repository.finishWorkout(id, trainingMaxDecisions)
        restTimerScheduler.cancel(id)
        exerciseIds.forEach { routineRepository.rebuildPersonalRecords(it) }
        app.linkRepository.rebuildAll()
    }

    fun resumeWorkout(id: Long) = runOperation("Resuming workout…", "Workout resumed") {
        repository.resumeWorkout(id)
        app.linkRepository.rebuildAll()
    }

    fun discardWorkout(id: Long) = runOperation(
        "Discarding workout…",
        "Workout discarded",
        successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
    ) {
        repository.discardWorkout(id)
        restTimerScheduler.cancel(id)
        app.linkRepository.rebuildAll()
    }

    fun restoreWorkout(id: Long) = runOperation("Restoring workout…", "Workout restored to history") {
        repository.restoreWorkout(id)
        app.linkRepository.rebuildAll()
    }

    fun deleteWorkoutPermanently(id: Long) = runOperation(
        "Deleting workout…",
        "Workout permanently deleted",
        successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
    ) {
        restTimerScheduler.cancel(id)
        app.domainDeletionCoordinator.deleteWorkout(id)
    }

    fun duplicateWorkout(id: Long) = runOperation("Copying workout…", "Workout copied into today") {
        repository.duplicateWorkout(id, asActive = true)
    }

    fun copyWorkoutExercise(id: Long) = runOperation("Copying exercise…", "Exercise sets copied into the active workout") {
        repository.copyWorkoutExerciseToActive(id)
    }

    fun addExercise(
        sessionId: Long,
        exerciseId: Long,
        machineId: Long? = null,
        onFinished: (Boolean) -> Unit = {},
    ) = runOperation(
        "Adding exercise…",
        "Exercise added",
        onFinished,
    ) { repository.addExerciseToWorkout(sessionId, exerciseId, machineId) }

    fun updateWorkoutExercise(id: Long, notes: String, groupId: Long?) = runOperation(
        "Saving exercise notes…",
        "Exercise updated",
    ) { repository.updateWorkoutExercise(id, notes, groupId) }

    fun updateWorkoutExerciseDetails(id: Long, notes: String, groupId: Long?, machineId: Long?) = runOperation(
        "Saving exercise details…",
        "Exercise updated",
    ) { repository.updateWorkoutExerciseDetails(id, notes, groupId, machineId) }

    fun setWorkoutExerciseMachine(id: Long, machineId: Long?) = runOperation(
        "Updating machine…",
        "Machine selection updated",
    ) { repository.setWorkoutExerciseMachine(id, machineId) }

    fun substituteWorkoutExercise(id: Long, exerciseId: Long, machineId: Long?) = runOperation(
        "Substituting exercise…",
        "Exercise replaced",
    ) { repository.substituteWorkoutExercise(id, exerciseId, machineId) }

    fun removeWorkoutExercise(id: Long) = runOperation(
        "Removing exercise…",
        "Exercise removed from workout",
    ) {
        repository.removeWorkoutExercise(id)
    }

    fun removeWorkoutExerciseFromGroup(id: Long) = runOperation(
        "Removing exercise from group…",
        "Exercise is now independent",
    ) {
        repository.removeWorkoutExerciseFromGroup(id)
    }

    fun reorderWorkoutExercises(sessionId: Long, ids: List<Long>) = runSilentReorder {
        repository.reorderWorkoutExercises(sessionId, ids)
    }

    fun createGroup(
        sessionId: Long,
        name: String,
        type: WorkoutGroupType,
        workoutExerciseIds: List<Long>,
        onFinished: (Boolean) -> Unit = {},
    ) = runOperation("Creating group…", "Exercise group created", onFinished) {
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
        restOverrideSeconds: Int? = null,
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
                repository.setSetCompleted(
                    id,
                    true,
                    app.settingsRepository.current().restTimerAutoStart,
                    restOverrideSeconds,
                )
                rebuildRecordsForSet(id)
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
                schedulePersistedRestTimer()
            } finally {
                savingQuickSetIds.remove(id)
            }
        }
    }

    fun completeSet(id: Long, completed: Boolean) = runOperation(
        "Updating set…",
        if (completed) "Set completed" else "Set reopened",
        successFeedbackPresentation = OperationFeedbackPresentation.Inline,
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

    fun reorderSets(workoutExerciseId: Long, ids: List<Long>) = runSilentReorder {
        repository.reorderSets(workoutExerciseId, ids)
    }

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

    fun updateRestTimerPresets(seconds: List<Int>) {
        updateSettings { settings ->
            settings.copy(restTimerPresetSeconds = normalizeRestTimerPresets(seconds))
        }
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
                checkNotNull(app.withUserDataAccess {
                    if (id == null) routineRepository.createRoutine(draft) else routineRepository.updateRoutine(id, draft)
                    Unit
                }) { "Whip data is unavailable while recovery is in progress" }
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

    fun setRoutineProgramPosition(routineId: Long, phaseIndex: Int, dayPosition: Int, cycle: Int) = runOperation(
        "Updating program position…",
        "Program position updated",
    ) {
        routineRepository.setRoutineProgramPosition(routineId, phaseIndex, dayPosition, cycle)
    }

    fun resetRoutineProgramProgress(routineId: Long) = runOperation(
        "Resetting program progress…",
        "Program progress reset",
    ) {
        routineRepository.resetRoutineProgramProgress(routineId)
    }

    fun restoreRoutineTrainingMaxEligibility(routineId: Long) = runOperation(
        "Restoring Training Max eligibility…",
        "Training Max progression restored",
    ) {
        routineRepository.setRoutineTrainingMaxIncreaseEligible(routineId, true)
    }

    fun duplicateRoutine(id: Long) = runOperation("Duplicating routine…", "Routine duplicated") {
        routineRepository.duplicateRoutine(id)
    }

    fun setRoutineArchived(id: Long, archived: Boolean) = runOperation(
        if (archived) "Archiving routine…" else "Restoring routine…",
        if (archived) "Routine archived" else "Routine restored",
    ) { routineRepository.setRoutineArchived(id, archived) }

    fun deleteRoutinePermanently(id: Long) = runOperation(
        "Deleting routine…",
        "Routine permanently deleted",
        successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
    ) {
        app.domainDeletionCoordinator.deleteRoutine(id)
    }

    fun setRoutinePinned(id: Long, pinned: Boolean) = runOperation(
        "Updating Home shortcut…",
        if (pinned) "Routine start shortcut added to Whip Home" else "Routine shortcut removed from Whip Home",
        successFeedbackPresentation = OperationFeedbackPresentation.Snackbar,
    ) {
        routineRepository.setRoutinePinned(id, pinned)
        if (pinned) app.settingsRepository.revealHomeSection(HomeSection.Gym)
    }

    fun reorderRoutines(ids: List<Long>) = runSilentReorder {
        routineRepository.reorderRoutines(ids)
    }

    fun saveWorkoutAsRoutine(sessionId: Long, name: String) = runOperation(
        "Saving routine…",
        "Workout saved as a routine",
    ) { routineRepository.saveWorkoutAsRoutine(sessionId, name) }

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

    private suspend fun nextExerciseLabel(): String? {
        // Read the just-committed Room state. uiState may still be one emission
        // behind when a completion transaction immediately schedules its timer.
        val sessionId = repository.sessions.first()
            .firstOrNull { it.state == WorkoutSessionState.Active }
            ?.id
            ?: return null
        val exercises = repository.exercises.first().associateBy(Exercise::id)
        val setsByPlacement = repository.sets.first().groupBy(WorkoutSet::workoutExerciseId)
        val groups = repository.groups.first().associateBy(WorkoutGroup::id)
        val items = repository.workoutExercises.first()
            .filter { it.sessionId == sessionId }
            .mapNotNull { placement ->
                val exercise = exercises[placement.exerciseId] ?: return@mapNotNull null
                WorkoutExerciseUi(
                    workoutExercise = placement,
                    exercise = exercise,
                    sets = setsByPlacement[placement.id].orEmpty(),
                    previousSets = emptyList(),
                    previousSetCount = 0,
                    group = placement.groupId?.let(groups::get),
                    machine = null,
                )
            }
        return selectNextWorkoutSet(items)
            ?.first
            ?.let { "Next: ${it.exercise.name}" }
    }

    private val reorderMutex = Mutex()

    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        app.tryWithUserDataAccessNow {
            app.settingsRepository.update(transform)
            Unit
        }
    }

    private fun runSilentReorder(block: suspend () -> Unit) {
        viewModelScope.launch {
            reorderMutex.withLock {
                try {
                    checkNotNull(app.withUserDataAccess {
                        block()
                        Unit
                    }) { "Whip data is unavailable while recovery is in progress" }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    _operationStatus.value = OperationStatus.Failed(error.message ?: "Could not save the new order", error)
                }
            }
        }
    }

    private fun runOperation(
        running: String,
        success: String,
        onFinished: (Boolean) -> Unit = {},
        successFeedbackPresentation: OperationFeedbackPresentation = OperationFeedbackPresentation.Inline,
        recoveryToken: Long? = null,
        block: suspend () -> Unit,
    ) {
        _operationStatus.value = OperationStatus.Running(running)
        viewModelScope.launch {
            try {
                checkNotNull(app.withUserDataAccess {
                    block()
                    Unit
                }) { "Whip data is unavailable while recovery is in progress" }
                _operationStatus.value = OperationStatus.Succeeded(
                    success,
                    successFeedbackPresentation,
                    recoveryToken,
                )
                runCatching { onFinished(true) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _operationStatus.value = OperationStatus.Failed(
                    error.message ?: "Something went wrong",
                    error,
                )
                runCatching { onFinished(false) }
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
    val trainingMaxDecisions: List<TrainingMaxDecision> = emptyList(),
)

private fun buildGymUiState(data: GymData, routineData: RoutineBaseData, nowMillis: Long, appSettings: AppSettings): GymUiState {
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
    val remaining = restTimerRemainingSeconds(
        deadlineMillis = active?.restTimerDeadlineMillis,
        nowMillis = nowMillis,
        configuredDurationSeconds = active?.restTimerDurationSeconds,
    )
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
        routines = routineData.routines.filterNot(GymRoutine::archived),
        archivedRoutines = routineData.routines.filter(GymRoutine::archived),
        routineDays = routineData.days,
        routineExercises = routineData.exercises,
        routineSets = routineData.sets,
        personalRecords = routineData.personalRecords,
        trainingMaxDecisions = routineData.trainingMaxDecisions,
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
    val remaining = restTimerRemainingSeconds(
        deadlineMillis = active?.restTimerDeadlineMillis,
        nowMillis = now,
        configuredDurationSeconds = active?.restTimerDurationSeconds,
    )
    return copy(nowMillis = now, summary = updatedSummary, restSecondsRemaining = remaining)
}

/**
 * Room can publish a newly started timer between one-second clock emissions. In that brief
 * window [nowMillis] predates the stored deadline's creation and an unclamped ceiling would
 * render a selected 5:00 timer as 5:01. The persisted duration is the authoritative upper
 * bound until the next tick; normal deadline arithmetic continues to handle background time.
 */
internal fun restTimerRemainingSeconds(
    deadlineMillis: Long?,
    nowMillis: Long,
    configuredDurationSeconds: Int?,
): Int? {
    val deadline = deadlineMillis ?: return null
    val rawSeconds = ((deadline - nowMillis + 999L) / 1_000L).coerceAtLeast(0L)
        .coerceAtMost(Int.MAX_VALUE.toLong())
        .toInt()
    val boundedSeconds = configuredDurationSeconds
        ?.takeIf { it > 0 }
        ?.let { duration -> minOf(rawSeconds, duration) }
        ?: rawSeconds
    return boundedSeconds.takeIf { it > 0 }
}
