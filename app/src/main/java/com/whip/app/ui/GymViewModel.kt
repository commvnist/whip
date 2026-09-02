package com.whip.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.whip.app.WhipApplication
import com.whip.app.core.HomeSection
import com.whip.app.core.OperationFeedbackPresentation
import com.whip.app.core.OperationStatus
import com.whip.app.core.PersistenceRequestState
import com.whip.app.core.WhipResult
import com.whip.app.core.completeCommittedPersistence
import com.whip.app.core.revealHomeSection
import com.whip.app.core.tryStartPersistenceRequest
import com.whip.app.data.CommittedExerciseDeletionCancellation
import com.whip.app.data.CommittedRoutineDeletionCancellation
import com.whip.app.data.CommittedWorkoutDeletionCancellation
import com.whip.app.data.ExerciseDeletionImpact
import com.whip.app.data.RoutineDeletionImpact
import com.whip.app.data.WorkoutDeletionImpact
import com.whip.app.data.GymRepository
import com.whip.app.data.GymWorkoutSnapshot
import com.whip.app.data.QuickSetCommitReceipt
import com.whip.app.data.RoutineRepository
import com.whip.app.data.WorkoutFinishReceipt
import com.whip.app.domain.Exercise
import com.whip.app.domain.ExerciseCategory
import com.whip.app.domain.ExerciseCategoryLink
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.WorkoutExercise
import com.whip.app.domain.WorkoutExerciseCopyBoundary
import com.whip.app.domain.WorkoutExerciseOutcome
import com.whip.app.domain.WorkoutFinishBoundary
import com.whip.app.domain.WorkoutGroup
import com.whip.app.domain.WorkoutGroupType
import com.whip.app.domain.WorkoutSession
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.domain.WorkoutSet
import com.whip.app.domain.WorkoutSetCopyBoundary
import com.whip.app.domain.WorkoutSetDraft
import com.whip.app.domain.WorkoutSetRemovalReason
import com.whip.app.domain.WorkoutArrangementDraft
import com.whip.app.domain.WorkoutLayoutSnapshot
import com.whip.app.domain.WorkoutPlacementMutationBoundary
import com.whip.app.domain.WorkoutSetMutationBoundary
import com.whip.app.domain.WorkoutStructureBoundary
import com.whip.app.domain.WorkoutStructureMutationReceipt
import com.whip.app.domain.workoutStructureBoundary
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
import com.whip.app.reminders.restTimerScheduleDelaySeconds
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
private const val GYM_DELETION_RECOVERY_REQUEST_ID_KEY = "gym_deletion_recovery_request_id"
internal const val GYM_HISTORY_COPY_AUTHORSHIP_KEY = "gym_history_copy_authorship_v1"

internal data class HistoryCopyAuthorship(
    val boundary: WorkoutExerciseCopyBoundary,
    val requestedWorkoutExerciseUuid: String,
    val requestedSetUuids: List<String>,
    val dataGeneration: Long,
)

internal fun encodeHistoryCopyAuthorship(authorship: HistoryCopyAuthorship): ArrayList<String> =
    ArrayList<String>().apply {
        val boundary = authorship.boundary
        add("1")
        add(boundary.sourceSessionId.toString())
        add(boundary.sourceSessionUuid)
        add(boundary.sourceWorkoutExerciseId.toString())
        add(boundary.sourceWorkoutExerciseUuid)
        add(boundary.sourceWorkoutExerciseUpdatedAtMillis.toString())
        add(if (boundary.target != null) "1" else "0")
        add(boundary.target?.sessionId?.toString().orEmpty())
        add(boundary.target?.sessionUuid.orEmpty())
        add(boundary.target?.fingerprint.orEmpty())
        add(authorship.requestedWorkoutExerciseUuid)
        add(authorship.dataGeneration.toString())
        add(boundary.sourceSets.size.toString())
        boundary.sourceSets.forEach { set ->
            add(set.setId.toString())
            add(set.setUuid)
            add(set.setUpdatedAtMillis.toString())
        }
        add(authorship.requestedSetUuids.size.toString())
        addAll(authorship.requestedSetUuids)
    }

internal fun decodeHistoryCopyAuthorship(encoded: List<String>?): HistoryCopyAuthorship? = runCatching {
    encoded ?: return null
    require(encoded.firstOrNull() == "1")
    val sourceSetCount = encoded[12].toInt()
    require(sourceSetCount >= 0)
    val sourceSetsOffset = 13
    val requestedSetCountOffset = sourceSetsOffset + sourceSetCount * 3
    val requestedSetCount = encoded[requestedSetCountOffset].toInt()
    require(requestedSetCount >= 0)
    require(encoded.size == requestedSetCountOffset + 1 + requestedSetCount)
    HistoryCopyAuthorship(
        boundary = WorkoutExerciseCopyBoundary(
            sourceSessionId = encoded[1].toLong(),
            sourceSessionUuid = encoded[2],
            sourceWorkoutExerciseId = encoded[3].toLong(),
            sourceWorkoutExerciseUuid = encoded[4],
            sourceWorkoutExerciseUpdatedAtMillis = encoded[5].toLong(),
            target = if (encoded[6] == "1") {
                WorkoutStructureBoundary(encoded[7].toLong(), encoded[8], encoded[9])
            } else {
                require(encoded[6] == "0")
                null
            },
            sourceSets = List(sourceSetCount) { index ->
                val offset = sourceSetsOffset + index * 3
                WorkoutSetCopyBoundary(
                    setId = encoded[offset].toLong(),
                    setUuid = encoded[offset + 1],
                    setUpdatedAtMillis = encoded[offset + 2].toLong(),
                )
            },
        ),
        requestedWorkoutExerciseUuid = encoded[10],
        dataGeneration = encoded[11].toLong(),
        requestedSetUuids = List(requestedSetCount) { index -> encoded[requestedSetCountOffset + 1 + index] },
    ).also { authorship ->
        require(authorship.requestedWorkoutExerciseUuid.isNotBlank())
        require(authorship.requestedSetUuids.size == authorship.boundary.sourceSets.size)
    }
}.getOrNull()

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

internal fun personalRecordReconciliationExerciseIds(
    sets: List<WorkoutSet>,
    placements: List<WorkoutExercise>,
    records: List<PersonalRecord>,
): Set<Long> {
    val completedPlacementIds = sets.asSequence()
        .filter { it.completed && it.deletedAtMillis == null }
        .mapTo(mutableSetOf(), WorkoutSet::workoutExerciseId)
    return buildSet {
        placements.asSequence()
            .filter { it.id in completedPlacementIds }
            .mapTo(this, WorkoutExercise::exerciseId)
        records.mapTo(this, PersonalRecord::exerciseId)
    }
}

data class GymUiState(
    val machines: List<GymMachine> = emptyList(),
    val archivedMachines: List<GymMachine> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val archivedExercises: List<Exercise> = emptyList(),
    val activeSession: WorkoutSession? = null,
    val activeWorkoutExercises: List<WorkoutExerciseUi> = emptyList(),
    val activeWorkoutPerformanceExercises: List<WorkoutExerciseUi> = emptyList(),
    val history: List<WorkoutSession> = emptyList(),
    val archivedWorkouts: List<WorkoutSession> = emptyList(),
    val allSessions: List<WorkoutSession> = emptyList(),
    val allWorkoutExercises: List<WorkoutExercise> = emptyList(),
    val allSets: List<WorkoutSet> = emptyList(),
    val allWorkoutGroups: List<WorkoutGroup> = emptyList(),
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

internal data class QuickSetAuthorshipBoundary(
    val setId: Long,
    val setUuid: String,
    val setUpdatedAtMillis: Long,
    val workoutExerciseId: Long,
    val workoutRevision: Long,
)

internal enum class GymSessionMutationKind {
    ExerciseAdded,
    ExerciseSubstituted,
    WorkoutExerciseCopied,
    SetUpdated,
    ExerciseDetailsUpdated,
    MachineCreatedAndAssigned,
    WorkoutExerciseRemoved,
    WorkoutGroupMemberRemoved,
    WorkoutGroupCreated,
    WorkoutArranged,
    WorkoutLayoutRestored,
    WorkoutSetRemoved,
    WorkoutSetRestored,
    WorkoutSetCompletionUpdated,
    WorkoutSetAdded,
    WorkoutSetDuplicated,
    WorkoutFinished,
    WorkoutDiscarded,
}

internal data class GymSessionMutationReceipt(
    val kind: GymSessionMutationKind,
    val targetId: Long? = null,
    val warnings: List<String> = emptyList(),
    val structureReceipt: WorkoutStructureMutationReceipt? = null,
    val setRemovalReason: WorkoutSetRemovalReason? = null,
)

data class WorkoutLayoutUndo(
    val boundary: WorkoutStructureBoundary,
    val snapshot: WorkoutLayoutSnapshot,
    val label: String,
)

internal fun GymUiState.captureWorkoutStructureBoundary(): WorkoutStructureBoundary? {
    val session = activeSession ?: return null
    return workoutStructureBoundary(session, allWorkoutExercises, allWorkoutGroups, allSets)
}

internal fun GymUiState.captureWorkoutExerciseCopyBoundary(
    workoutExerciseId: Long,
): WorkoutExerciseCopyBoundary? {
    val source = allWorkoutExercises.firstOrNull { it.id == workoutExerciseId } ?: return null
    val sourceSession = allSessions.firstOrNull { it.id == source.sessionId } ?: return null
    val sourceSets = allSets
        .filter { it.workoutExerciseId == source.id && it.deletedAtMillis == null }
        .sortedWith(compareBy(WorkoutSet::position, WorkoutSet::id))
    return WorkoutExerciseCopyBoundary(
        sourceSessionId = sourceSession.id,
        sourceSessionUuid = sourceSession.uuid,
        sourceWorkoutExerciseId = source.id,
        sourceWorkoutExerciseUuid = source.uuid,
        sourceWorkoutExerciseUpdatedAtMillis = source.updatedAtMillis,
        sourceSets = sourceSets.map { set ->
            WorkoutSetCopyBoundary(set.id, set.uuid, set.updatedAtMillis)
        },
        target = captureWorkoutStructureBoundary(),
    )
}

internal fun GymUiState.capturePlacementMutationBoundary(
    workoutExerciseId: Long,
): WorkoutPlacementMutationBoundary? {
    val structure = captureWorkoutStructureBoundary() ?: return null
    val placement = allWorkoutExercises.firstOrNull {
        it.id == workoutExerciseId && it.sessionId == structure.sessionId
    } ?: return null
    val groupUuid = placement.groupId?.let { groupId ->
        allWorkoutGroups.firstOrNull { it.id == groupId }?.uuid
    }
    return WorkoutPlacementMutationBoundary(
        structure = structure,
        workoutExerciseId = placement.id,
        workoutExerciseUuid = placement.uuid,
        workoutExerciseUpdatedAtMillis = placement.updatedAtMillis,
        expectedGroupUuid = groupUuid,
    )
}

internal fun GymUiState.captureSetMutationBoundary(setId: Long): WorkoutSetMutationBoundary? {
    val session = activeSession ?: return null
    val set = allSets.firstOrNull { it.id == setId } ?: return null
    val placement = allWorkoutExercises.firstOrNull {
        it.id == set.workoutExerciseId && it.sessionId == session.id
    } ?: return null
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

internal fun GymUiState.captureWorkoutArrangementDraft(
    activeWorkoutExerciseIdsInOrder: List<Long> = activeWorkoutExercises
        .sortedBy { it.workoutExercise.position }.map { it.workoutExercise.id },
    setIdsInOrderByWorkoutExerciseId: Map<Long, List<Long>> = emptyMap(),
): WorkoutArrangementDraft {
    val placementById = activeWorkoutExercises.associateBy { it.workoutExercise.id }
    return WorkoutArrangementDraft(
        activeWorkoutExerciseUuidsInOrder = activeWorkoutExerciseIdsInOrder.map { id ->
            requireNotNull(placementById[id]) { "Exercise order changed; review the workout" }
                .workoutExercise.uuid
        },
        setOrders = activeWorkoutExercises.map { item ->
            val orderedSets = setIdsInOrderByWorkoutExerciseId[item.workoutExercise.id]?.map { id ->
                requireNotNull(item.sets.firstOrNull { it.id == id }) { "Set order changed; review the workout" }
            } ?: item.sets.sortedWith(compareBy(WorkoutSet::position, WorkoutSet::id))
            com.whip.app.domain.WorkoutSetOrderDraft(
                workoutExerciseUuid = item.workoutExercise.uuid,
                setUuidsInOrder = orderedSets.map(WorkoutSet::uuid),
            )
        },
    )
}

internal enum class GymDeletionKind {
    Exercise,
    Routine,
    Workout,
}

internal data class GymDeletionReceipt(
    val kind: GymDeletionKind,
    val targetId: Long,
    val warnings: List<String> = emptyList(),
)

private data class GymDeletionPreviewLookup<T>(val impact: T?)

internal class CommittedGymDeletionCancellation(
    val receipt: GymDeletionReceipt,
    cause: CancellationException,
) : CancellationException(cause.message) {
    init { initCause(cause) }
}

internal class CommittedGymMutationCancellation(cause: CancellationException) :
    CancellationException(cause.message) {
    init { initCause(cause) }
}

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

internal fun selectWorkoutPerformancePlacements(
    placements: List<WorkoutExercise>,
    sets: List<WorkoutSet>,
    sessionId: Long,
): List<WorkoutExercise> {
    val completedPlacementIds = sets.asSequence()
        .filter { it.completed && it.deletedAtMillis == null }
        .mapTo(mutableSetOf(), WorkoutSet::workoutExerciseId)
    return placements.filter { placement ->
        placement.sessionId == sessionId &&
            (placement.outcome == WorkoutExerciseOutcome.Active || placement.id in completedPlacementIds)
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GymViewModel @JvmOverloads constructor(
    application: Application,
    savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : AndroidViewModel(application) {
    private val app = application as WhipApplication
    private val repository: GymRepository = app.gymRepository
    private val routineRepository: RoutineRepository = app.routineRepository
    private val clock = app.clock
    private val restTimerScheduler = app.restTimerScheduler

    private val _operationStatus = MutableStateFlow<OperationStatus>(OperationStatus.Idle)
    val operationStatus: StateFlow<OperationStatus> = _operationStatus.asStateFlow()
    private val _sessionMutationState = MutableStateFlow<PersistenceRequestState<GymSessionMutationReceipt>>(
        PersistenceRequestState.Idle,
    )
    internal val sessionMutationState: StateFlow<PersistenceRequestState<GymSessionMutationReceipt>> =
        _sessionMutationState.asStateFlow()
    private val historyCopyAuthorshipEncoded = savedStateHandle.getMutableStateFlow<ArrayList<String>?>(
        GYM_HISTORY_COPY_AUTHORSHIP_KEY,
        null,
    )
    internal val historyCopyAuthorship: StateFlow<HistoryCopyAuthorship?> = historyCopyAuthorshipEncoded
        .map(::decodeHistoryCopyAuthorship)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            decodeHistoryCopyAuthorship(historyCopyAuthorshipEncoded.value),
        )
    private val _machineDeletionImpact = MutableStateFlow<MachineDeletionImpact?>(null)
    val machineDeletionImpact: StateFlow<MachineDeletionImpact?> = _machineDeletionImpact.asStateFlow()
    private val _machineDeletionInProgress = MutableStateFlow(false)
    val machineDeletionInProgress: StateFlow<Boolean> = _machineDeletionInProgress.asStateFlow()
    private val _exerciseDeletionImpact = MutableStateFlow<ExerciseDeletionImpact?>(null)
    val exerciseDeletionImpact: StateFlow<ExerciseDeletionImpact?> = _exerciseDeletionImpact.asStateFlow()
    private val _exerciseDeletionPreviewError = MutableStateFlow<String?>(null)
    val exerciseDeletionPreviewError: StateFlow<String?> = _exerciseDeletionPreviewError.asStateFlow()
    private val _exerciseDeletionTargetMissing = MutableStateFlow(false)
    val exerciseDeletionTargetMissing: StateFlow<Boolean> = _exerciseDeletionTargetMissing.asStateFlow()
    private val _routineDeletionImpact = MutableStateFlow<RoutineDeletionImpact?>(null)
    val routineDeletionImpact: StateFlow<RoutineDeletionImpact?> = _routineDeletionImpact.asStateFlow()
    private val _routineDeletionPreviewError = MutableStateFlow<String?>(null)
    val routineDeletionPreviewError: StateFlow<String?> = _routineDeletionPreviewError.asStateFlow()
    private val _routineDeletionTargetMissing = MutableStateFlow(false)
    val routineDeletionTargetMissing: StateFlow<Boolean> = _routineDeletionTargetMissing.asStateFlow()
    private val _workoutDeletionImpact = MutableStateFlow<WorkoutDeletionImpact?>(null)
    val workoutDeletionImpact: StateFlow<WorkoutDeletionImpact?> = _workoutDeletionImpact.asStateFlow()
    private val _workoutDeletionPreviewError = MutableStateFlow<String?>(null)
    val workoutDeletionPreviewError: StateFlow<String?> = _workoutDeletionPreviewError.asStateFlow()
    private val _workoutDeletionTargetMissing = MutableStateFlow(false)
    val workoutDeletionTargetMissing: StateFlow<Boolean> = _workoutDeletionTargetMissing.asStateFlow()
    private val _gymDeletionState = MutableStateFlow<PersistenceRequestState<GymDeletionReceipt>>(
        PersistenceRequestState.Idle,
    )
    internal val gymDeletionState: StateFlow<PersistenceRequestState<GymDeletionReceipt>> =
        _gymDeletionState.asStateFlow()
    private val _orphanedGymDeletionRequestId = savedStateHandle.getMutableStateFlow<String?>(
        GYM_DELETION_RECOVERY_REQUEST_ID_KEY,
        null,
    )
    val orphanedGymDeletionRequestId: StateFlow<String?> = _orphanedGymDeletionRequestId.asStateFlow()
    private val orphanRecoveryInProgressRequestId = MutableStateFlow<String?>(null)
    private val gymDeletionMutex = Mutex()
    private val sessionMutationMutex = Mutex()
    private val _pendingWorkoutLayoutUndo = MutableStateFlow<WorkoutLayoutUndo?>(null)
    val pendingWorkoutLayoutUndo: StateFlow<WorkoutLayoutUndo?> = _pendingWorkoutLayoutUndo.asStateFlow()
    private var exerciseDeletionPreviewGeneration = 0L
    private var routineDeletionPreviewGeneration = 0L
    private var workoutDeletionPreviewGeneration = 0L
    private val _pendingMachineArchiveUndo = MutableStateFlow<Long?>(null)
    val pendingMachineArchiveUndo: StateFlow<Long?> = _pendingMachineArchiveUndo.asStateFlow()
    private var pendingMachineArchiveId: Long? = null
    private var nextMachineArchiveUndoToken = 0L
    private val reloadKey = MutableStateFlow(0)
    private val savingQuickSetIds = mutableSetOf<Long>()

    private val gymBaseData = repository.workoutSnapshot.map { snapshot ->
        snapshot.toGymData()
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
        // Personal records and timer notifications are derived from durable workout rows.
        // Reconcile them whenever Gym is opened so a process death or post-commit scheduler
        // failure cannot leave the persisted workout in a permanently half-applied state.
        viewModelScope.launch {
            try {
                app.withUserDataAccess {
                    try {
                        personalRecordReconciliationExerciseIds(
                            sets = repository.sets.first(),
                            placements = repository.workoutExercises.first(),
                            records = routineRepository.personalRecords.first(),
                        ).forEach { exerciseId ->
                            routineRepository.rebuildPersonalRecords(exerciseId)
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // A later Gym open retries from completed sets plus the still-present PR rows.
                    }
                    try {
                        repository.sessions.first()
                            .filter(WorkoutSession::restTimerCleanupPending)
                            .forEach { session ->
                                reconcilePersistedRestTimer(session.id)
                            }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // The durable pending bit keeps this exact cleanup retryable next time.
                    }
                    Unit
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // The durable workout remains authoritative; the next Gym open retries this pass.
            }
        }
        // Repair group invariants for a workout created by an older build before
        // the UI or rest-timer policy consumes its ordering.
        viewModelScope.launch {
            repository.sessions
                .map { sessions -> sessions.firstOrNull { it.state == WorkoutSessionState.Active }?.id }
                .distinctUntilChanged()
                .collectLatest { sessionId ->
                    if (sessionId != null) {
                        app.withUserDataAccess {
                            repository.normalizeActiveWorkoutStructure(sessionId)
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
                _exerciseDeletionImpact.value = null
                _exerciseDeletionPreviewError.value = null
                _exerciseDeletionTargetMissing.value = false
                _routineDeletionImpact.value = null
                _routineDeletionPreviewError.value = null
                _routineDeletionTargetMissing.value = false
                _workoutDeletionImpact.value = null
                _workoutDeletionPreviewError.value = null
                _workoutDeletionTargetMissing.value = false
                _gymDeletionState.value = PersistenceRequestState.Idle
                _sessionMutationState.value = PersistenceRequestState.Idle
                historyCopyAuthorshipEncoded.value = null
                _pendingWorkoutLayoutUndo.value = null
                _orphanedGymDeletionRequestId.value = null
                orphanRecoveryInProgressRequestId.value = null
                exerciseDeletionPreviewGeneration++
                routineDeletionPreviewGeneration++
                workoutDeletionPreviewGeneration++
                _operationStatus.value = OperationStatus.Idle
            }
        }
    }

    fun consumeOperationStatus() {
        _operationStatus.value = OperationStatus.Idle
    }
    fun consumeSessionMutationResult(requestId: String) {
        if ((_sessionMutationState.value as? PersistenceRequestState.Finished)?.requestId == requestId) {
            _sessionMutationState.value = PersistenceRequestState.Idle
        }
    }
    fun consumeGymDeletionResult(requestId: String) {
        if ((_gymDeletionState.value as? PersistenceRequestState.Finished)?.requestId == requestId) {
            _gymDeletionState.value = PersistenceRequestState.Idle
        }
    }
    fun currentDataGeneration(): Long = app.currentUserDataGeneration()

    internal fun setHistoryCopyAuthorship(authorship: HistoryCopyAuthorship?) {
        historyCopyAuthorshipEncoded.value = authorship?.let(::encodeHistoryCopyAuthorship)
    }

    fun adoptOrphanedGymDeletionRequest(requestId: String): Boolean {
        val adopted = _gymDeletionState.tryStartPersistenceRequest(requestId)
        if (adopted) _orphanedGymDeletionRequestId.value = requestId
        return adopted
    }

    fun restartOrphanedGymDeletionVerification(
        previousRequestId: String,
        requestId: String,
    ): Boolean {
        if (_orphanedGymDeletionRequestId.value != previousRequestId) return false
        while (true) {
            val current = _gymDeletionState.value
            val ownsPreviousOutcome = when (current) {
                PersistenceRequestState.Idle -> true
                is PersistenceRequestState.Finished -> current.requestId == previousRequestId
                is PersistenceRequestState.Running -> false
            }
            if (!ownsPreviousOutcome) return false
            if (_gymDeletionState.compareAndSet(current, PersistenceRequestState.Running(requestId))) {
                _orphanedGymDeletionRequestId.value = requestId
                return true
            }
        }
    }

    fun abandonOrphanedGymDeletionVerification() {
        _orphanedGymDeletionRequestId.value = null
        orphanRecoveryInProgressRequestId.value = null
    }

    private fun tryStartOrphanedGymDeletionRecovery(requestId: String): Boolean {
        if (_orphanedGymDeletionRequestId.value != requestId) return false
        if ((_gymDeletionState.value as? PersistenceRequestState.Running)?.requestId != requestId) return false
        return orphanRecoveryInProgressRequestId.compareAndSet(null, requestId)
    }

    private fun finishOrphanedGymDeletionRecovery(requestId: String) {
        orphanRecoveryInProgressRequestId.compareAndSet(requestId, null)
    }

    fun finishOrphanedGymDeletionAsInterrupted(requestId: String) {
        if (!tryStartOrphanedGymDeletionRecovery(requestId)) return
        if ((_gymDeletionState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
            _orphanedGymDeletionRequestId.value = null
            _gymDeletionState.value = PersistenceRequestState.Finished(
                requestId,
                WhipResult.Failure(
                    "The previous deletion was interrupted before it committed. The exact impact has been refreshed; review it before retrying.",
                ),
            )
        }
        finishOrphanedGymDeletionRecovery(requestId)
    }

    fun finishOrphanedGymDeletionAsUnverified(requestId: String) {
        if (!tryStartOrphanedGymDeletionRecovery(requestId)) return
        if ((_gymDeletionState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
            _gymDeletionState.value = PersistenceRequestState.Finished(
                requestId,
                WhipResult.Failure(
                    "The previous deletion outcome could not be verified. Retry the read-only verification or close and inspect Gym History or the Library; do not submit another deletion until the target is confirmed present.",
                ),
            )
        }
        finishOrphanedGymDeletionRecovery(requestId)
    }

    fun finishOrphanedExerciseDeletionAsAchieved(
        requestId: String,
        exerciseId: Long,
        exerciseUuid: String?,
        expectedDataGeneration: Long,
    ) {
        if (!tryStartOrphanedGymDeletionRecovery(requestId)) return
        viewModelScope.launch {
            try {
                if (!app.isCurrentUserDataGeneration(expectedDataGeneration)) {
                    if ((_gymDeletionState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                        _gymDeletionState.value = PersistenceRequestState.Idle
                    }
                    return@launch
                }
                val warnings = mutableListOf<String>()
                try {
                    checkNotNull(app.withUserDataAccess {
                        require(app.isCurrentUserDataGeneration(expectedDataGeneration)) {
                            "User data changed during deletion recovery"
                        }
                        try {
                            routineRepository.rebuildPersonalRecords(exerciseId)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            warnings += "Personal-record reconciliation could not be verified after process recovery."
                        }
                        try {
                            app.linkRepository.rebuildAll()
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            warnings += "Link reconciliation could not be verified after process recovery."
                        }
                        if (exerciseUuid != null) {
                            try {
                                app.settingsRepository.update { current ->
                                    current.copy(
                                        trackedGymRecords = current.trackedGymRecords.filterNot {
                                            it.exerciseUuid == exerciseUuid
                                        },
                                    )
                                }
                            } catch (_: Exception) {
                                warnings += "Tracked-record preferences could not be verified after process recovery."
                            }
                        }
                        Unit
                    }) { "Whip data is unavailable while recovery is in progress" }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    warnings += "Some post-delete reconciliation could not be verified after process recovery."
                }
                if (!app.isCurrentUserDataGeneration(expectedDataGeneration)) {
                    if ((_gymDeletionState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                        _gymDeletionState.value = PersistenceRequestState.Idle
                    }
                    return@launch
                }
                if ((_gymDeletionState.value as? PersistenceRequestState.Running)?.requestId == requestId &&
                    _orphanedGymDeletionRequestId.value == requestId
                ) {
                    _operationStatus.value = OperationStatus.Succeeded(
                        listOf("Exercise is already absent; deletion end state confirmed")
                            .plus(warnings).joinToString(" · "),
                        OperationFeedbackPresentation.Snackbar,
                    )
                    _orphanedGymDeletionRequestId.value = null
                    _gymDeletionState.value = PersistenceRequestState.Finished(
                        requestId,
                        WhipResult.Success(
                            GymDeletionReceipt(
                                kind = GymDeletionKind.Exercise,
                                targetId = exerciseId,
                                warnings = warnings,
                            ),
                        ),
                    )
                }
            } finally {
                finishOrphanedGymDeletionRecovery(requestId)
            }
        }
    }

    fun finishOrphanedRoutineDeletionAsAchieved(
        requestId: String,
        routineId: Long,
        expectedDataGeneration: Long,
    ) {
        if (!tryStartOrphanedGymDeletionRecovery(requestId)) return
        viewModelScope.launch {
            try {
                if (!app.isCurrentUserDataGeneration(expectedDataGeneration)) {
                    if ((_gymDeletionState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                        _gymDeletionState.value = PersistenceRequestState.Idle
                    }
                    return@launch
                }
                val warnings = mutableListOf<String>()
                try {
                    checkNotNull(app.withUserDataAccess {
                        require(app.isCurrentUserDataGeneration(expectedDataGeneration)) {
                            "User data changed during deletion recovery"
                        }
                        try {
                            app.linkRepository.rebuildAll()
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            warnings += "Link reconciliation could not be verified after process recovery."
                        }
                        Unit
                    }) { "Whip data is unavailable while recovery is in progress" }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    warnings += "Post-delete reconciliation could not be verified after process recovery."
                }
                if (!app.isCurrentUserDataGeneration(expectedDataGeneration)) {
                    if ((_gymDeletionState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                        _gymDeletionState.value = PersistenceRequestState.Idle
                    }
                    return@launch
                }
                if ((_gymDeletionState.value as? PersistenceRequestState.Running)?.requestId == requestId &&
                    _orphanedGymDeletionRequestId.value == requestId
                ) {
                    _operationStatus.value = OperationStatus.Succeeded(
                        listOf("Routine is already absent; deletion end state confirmed")
                            .plus(warnings).joinToString(" · "),
                        OperationFeedbackPresentation.Snackbar,
                    )
                    _orphanedGymDeletionRequestId.value = null
                    _gymDeletionState.value = PersistenceRequestState.Finished(
                        requestId,
                        WhipResult.Success(
                            GymDeletionReceipt(GymDeletionKind.Routine, routineId, warnings),
                        ),
                    )
                }
            } finally {
                finishOrphanedGymDeletionRecovery(requestId)
            }
        }
    }

    fun finishOrphanedWorkoutDeletionAsAchieved(
        requestId: String,
        sessionId: Long,
        expectedDataGeneration: Long,
    ) {
        if (!tryStartOrphanedGymDeletionRecovery(requestId)) return
        viewModelScope.launch {
            try {
                if (!app.isCurrentUserDataGeneration(expectedDataGeneration)) {
                    if ((_gymDeletionState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                        _gymDeletionState.value = PersistenceRequestState.Idle
                    }
                    return@launch
                }
                val warnings = mutableListOf<String>()
                try {
                    checkNotNull(app.withUserDataAccess {
                        require(app.isCurrentUserDataGeneration(expectedDataGeneration)) {
                            "User data changed during deletion recovery"
                        }
                        try {
                            personalRecordReconciliationExerciseIds(
                                sets = repository.sets.first(),
                                placements = repository.workoutExercises.first(),
                                records = routineRepository.personalRecords.first(),
                            ).forEach { exerciseId ->
                                routineRepository.rebuildPersonalRecords(exerciseId)
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            warnings += "Personal-record reconciliation could not be verified after process recovery."
                        }
                        try {
                            app.linkRepository.rebuildAll()
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            warnings += "Link reconciliation could not be verified after process recovery."
                        }
                        Unit
                    }) { "Whip data is unavailable while recovery is in progress" }
                    try {
                        restTimerScheduler.cancel(sessionId)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        warnings += "Rest-timer cleanup could not be verified after process recovery."
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    warnings += "Some post-delete reconciliation could not be verified after process recovery."
                }
                if (!app.isCurrentUserDataGeneration(expectedDataGeneration)) {
                    if ((_gymDeletionState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                        _gymDeletionState.value = PersistenceRequestState.Idle
                    }
                    return@launch
                }
                if ((_gymDeletionState.value as? PersistenceRequestState.Running)?.requestId == requestId &&
                    _orphanedGymDeletionRequestId.value == requestId
                ) {
                    _operationStatus.value = OperationStatus.Succeeded(
                        listOf("Workout is already absent; deletion end state confirmed")
                            .plus(warnings).joinToString(" · "),
                        OperationFeedbackPresentation.Snackbar,
                    )
                    _orphanedGymDeletionRequestId.value = null
                    _gymDeletionState.value = PersistenceRequestState.Finished(
                        requestId,
                        WhipResult.Success(
                            GymDeletionReceipt(GymDeletionKind.Workout, sessionId, warnings),
                        ),
                    )
                }
            } finally {
                finishOrphanedGymDeletionRecovery(requestId)
            }
        }
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

    fun createMachineAndAssign(
        boundary: WorkoutPlacementMutationBoundary,
        draft: GymMachineDraft,
        requestId: String,
    ): Boolean = runSessionMutation(
        running = "Creating and assigning machine…",
        success = "Machine created and assigned",
        requestId = requestId,
        committedReceipt = GymSessionMutationReceipt(
            GymSessionMutationKind.MachineCreatedAndAssigned,
            boundary.workoutExerciseId,
        ),
    ) {
        repository.createMachineAndAssign(boundary, draft)
        GymSessionMutationReceipt(
            GymSessionMutationKind.MachineCreatedAndAssigned,
            boundary.workoutExerciseId,
        )
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

    fun createExerciseAndAdd(
        boundary: WorkoutStructureBoundary,
        draft: ExerciseDraft,
        requestedWorkoutExerciseUuid: String,
        requestedInitialSetUuid: String,
        requestId: String,
    ): Boolean = runSessionMutation(
        running = "Creating exercise…",
        success = "Exercise created · First set ready",
        requestId = requestId,
        committedReceipt = GymSessionMutationReceipt(GymSessionMutationKind.ExerciseAdded),
    ) {
        val receipt = repository.createExerciseAndAddToWorkout(
            boundary,
            draft,
            requestedWorkoutExerciseUuid,
            requestedInitialSetUuid,
        )
        _pendingWorkoutLayoutUndo.value = null
        GymSessionMutationReceipt(GymSessionMutationKind.ExerciseAdded, receipt.workoutExerciseId)
    }

    fun createExerciseAndSubstitute(
        boundary: WorkoutPlacementMutationBoundary,
        draft: ExerciseDraft,
        requestedWorkoutExerciseUuid: String,
        requestedInitialSetUuid: String,
        requestId: String,
    ): Boolean = runSessionMutation(
        running = "Creating substitution…",
        success = "Exercise created and substituted",
        requestId = requestId,
        committedReceipt = GymSessionMutationReceipt(GymSessionMutationKind.ExerciseSubstituted),
    ) {
        val replacementId = repository.createExerciseAndSubstitute(
            boundary,
            draft,
            requestedWorkoutExerciseUuid,
            requestedInitialSetUuid,
        )
        _pendingWorkoutLayoutUndo.value = null
        GymSessionMutationReceipt(GymSessionMutationKind.ExerciseSubstituted, replacementId)
    }

    fun duplicateExercise(id: Long) = runOperation("Duplicating exercise…", "Exercise duplicated") {
        repository.duplicateExercise(id)
    }

    fun setExerciseArchived(id: Long, archived: Boolean) = runOperation(
        if (archived) "Archiving exercise…" else "Restoring exercise…",
        if (archived) "Exercise archived" else "Exercise restored",
    ) { repository.setExerciseArchived(id, archived) }

    fun previewExerciseDeletion(id: Long) {
        val generation = ++exerciseDeletionPreviewGeneration
        _exerciseDeletionImpact.value = null
        _exerciseDeletionPreviewError.value = null
        _exerciseDeletionTargetMissing.value = false
        viewModelScope.launch {
            try {
                val lookup = checkNotNull(app.withUserDataAccess {
                    GymDeletionPreviewLookup(app.domainDeletionCoordinator.previewExerciseDeletion(id))
                }) { "Whip data is unavailable while recovery is in progress" }
                if (exerciseDeletionPreviewGeneration == generation) {
                    _exerciseDeletionImpact.value = lookup.impact
                    if (lookup.impact == null) {
                        _exerciseDeletionTargetMissing.value = true
                        _exerciseDeletionPreviewError.value =
                            "Exercise no longer exists. It may already have been deleted; close this review and verify the Library."
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (exerciseDeletionPreviewGeneration == generation) {
                    _exerciseDeletionPreviewError.value =
                        error.message ?: "Could not review exercise deletion impact"
                }
            }
        }
    }

    fun dismissExerciseDeletion() {
        exerciseDeletionPreviewGeneration++
        _exerciseDeletionImpact.value = null
        _exerciseDeletionPreviewError.value = null
        _exerciseDeletionTargetMissing.value = false
    }

    fun deleteExercisePermanently(
        id: Long,
        expectedRevisionToken: String,
        requestId: String,
    ): Boolean = runGymDeletion(
        running = "Deleting exercise…",
        success = "Exercise permanently deleted",
        requestId = requestId,
        savedDescription = "exercise deletion",
    ) {
        val exerciseUuid = (uiState.value.exercises + uiState.value.archivedExercises)
            .firstOrNull { it.id == id }?.uuid
        completeCommittedPersistence<GymDeletionReceipt>(
            commit = {
                val summary = try {
                    app.domainDeletionCoordinator.deleteExercise(id, expectedRevisionToken)
                } catch (cancelled: CommittedExerciseDeletionCancellation) {
                    throw CommittedGymDeletionCancellation(
                        GymDeletionReceipt(
                            GymDeletionKind.Exercise,
                            id,
                            cancelled.summary.warnings,
                        ),
                        cancelled,
                    )
                }
                require(summary.exerciseDeleted) {
                    _exerciseDeletionImpact.value = null
                    _exerciseDeletionTargetMissing.value = true
                    _exerciseDeletionPreviewError.value =
                        "Exercise no longer exists. It may already have been deleted; close this review and verify the Library."
                    "Exercise no longer exists. It may already have been deleted; close this review and verify the Library."
                }
                GymDeletionReceipt(GymDeletionKind.Exercise, id, summary.warnings)
            },
            followUp = { committed ->
                if (exerciseUuid != null) {
                    app.settingsRepository.update { current ->
                        current.copy(
                            trackedGymRecords = current.trackedGymRecords.filterNot {
                                it.exerciseUuid == exerciseUuid
                            },
                        )
                    }
                }
                committed
            },
            onCancellation = { committed, cancelled ->
                CommittedGymDeletionCancellation(committed, cancelled)
            },
            onOrdinaryFailure = { committed ->
                committed.copy(
                    warnings = committed.warnings +
                        "Tracked-record preferences did not finish updating; the exercise itself was deleted.",
                )
            },
        )
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
            localDate = date ?: clock.today(),
            zoneId = clock.zoneId(),
            keepScreenAwake = keepScreenAwake,
        )
        _pendingWorkoutLayoutUndo.value = null
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

    internal fun finishWorkout(
        boundary: WorkoutFinishBoundary,
        trainingMaxDecisions: List<TrainingMaxCycleDecision>? = null,
        requestId: String,
    ): Boolean = runSessionMutation(
        running = "Finishing workout…",
        success = "Workout saved to history",
        requestId = requestId,
        committedReceipt = GymSessionMutationReceipt(GymSessionMutationKind.WorkoutFinished, boundary.sessionId),
    ) {
        var followUpWarning = false
        completeCommittedPersistence<WorkoutFinishReceipt>(
            commit = {
                repository.finishWorkout(
                    id = boundary.sessionId,
                    trainingMaxDecisions = trainingMaxDecisions,
                    expectedSessionUuid = boundary.sessionUuid,
                    expectedWorkoutRevision = boundary.workoutRevision,
                )
            },
            followUp = { receipt ->
                reconcilePersistedRestTimer(receipt.sessionId)
                receipt.exerciseIds.forEach { routineRepository.rebuildPersonalRecords(it) }
                app.linkRepository.rebuildAll()
                receipt
            },
            onCancellation = { _, cancelled -> CommittedGymMutationCancellation(cancelled) },
            onOrdinaryFailure = { committed -> committed.also { followUpWarning = true } },
        )
        _pendingWorkoutLayoutUndo.value = null
        GymSessionMutationReceipt(
            GymSessionMutationKind.WorkoutFinished,
            boundary.sessionId,
            warnings = if (followUpWarning) {
                listOf("Workout saved; background records will be reconciled when Gym opens again.")
            } else emptyList(),
        )
    }

    fun resumeWorkout(id: Long) = runOperation("Resuming workout…", "Workout resumed") {
        completeCommittedPersistence<Unit>(
            commit = { repository.resumeWorkout(id) },
            followUp = {
                reconcilePersistedRestTimer(id)
                app.linkRepository.rebuildAll()
            },
            onCancellation = { _, cancelled -> CommittedGymMutationCancellation(cancelled) },
            onOrdinaryFailure = { },
        )
        _pendingWorkoutLayoutUndo.value = null
    }

    fun discardWorkout(boundary: WorkoutFinishBoundary, requestId: String): Boolean = runSessionMutation(
        running = "Discarding workout…",
        success = "Workout discarded",
        requestId = requestId,
        committedReceipt = GymSessionMutationReceipt(GymSessionMutationKind.WorkoutDiscarded, boundary.sessionId),
    ) {
        var followUpWarning = false
        completeCommittedPersistence<Unit>(
            commit = { repository.discardWorkout(boundary) },
            followUp = {
                reconcilePersistedRestTimer(boundary.sessionId)
                app.linkRepository.rebuildAll()
            },
            onCancellation = { _, cancelled -> CommittedGymMutationCancellation(cancelled) },
            onOrdinaryFailure = { followUpWarning = true },
        )
        _pendingWorkoutLayoutUndo.value = null
        GymSessionMutationReceipt(
            GymSessionMutationKind.WorkoutDiscarded,
            boundary.sessionId,
            warnings = if (followUpWarning) {
                listOf("Workout discarded; background cleanup will be reconciled when Gym opens again.")
            } else emptyList(),
        )
    }

    fun restoreWorkout(id: Long) = runOperation("Restoring workout…", "Workout restored to history") {
        completeCommittedPersistence<Unit>(
            commit = { repository.restoreWorkout(id) },
            followUp = {
                reconcilePersistedRestTimer(id)
                app.linkRepository.rebuildAll()
            },
            onCancellation = { _, cancelled -> CommittedGymMutationCancellation(cancelled) },
            onOrdinaryFailure = { },
        )
    }

    fun previewWorkoutDeletion(id: Long, expectedUuid: String? = null) {
        val generation = ++workoutDeletionPreviewGeneration
        _workoutDeletionImpact.value = null
        _workoutDeletionPreviewError.value = null
        _workoutDeletionTargetMissing.value = false
        viewModelScope.launch {
            try {
                val lookup = checkNotNull(app.withUserDataAccess {
                    GymDeletionPreviewLookup(app.domainDeletionCoordinator.previewWorkoutDeletion(id))
                }) { "Whip data is unavailable while recovery is in progress" }
                if (workoutDeletionPreviewGeneration == generation) {
                    val impact = lookup.impact
                    if (impact != null && expectedUuid != null && impact.sessionUuid != expectedUuid) {
                        _workoutDeletionPreviewError.value =
                            "Workout identity changed while the deletion review was opening. Close it and select the History item again."
                    } else {
                        _workoutDeletionImpact.value = impact
                        if (impact == null) {
                            _workoutDeletionTargetMissing.value = true
                            _workoutDeletionPreviewError.value =
                                "Workout no longer exists. It may already have been deleted; close this review and verify History."
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (workoutDeletionPreviewGeneration == generation) {
                    _workoutDeletionPreviewError.value =
                        error.message ?: "Could not review workout deletion impact"
                }
            }
        }
    }

    fun dismissWorkoutDeletion() {
        workoutDeletionPreviewGeneration++
        _workoutDeletionImpact.value = null
        _workoutDeletionPreviewError.value = null
        _workoutDeletionTargetMissing.value = false
    }

    fun deleteWorkoutPermanently(
        id: Long,
        expectedRevisionToken: String,
        requestId: String,
    ): Boolean = runGymDeletion(
        running = "Deleting workout…",
        success = "Workout permanently deleted",
        requestId = requestId,
        savedDescription = "workout deletion",
    ) {
        completeCommittedPersistence<GymDeletionReceipt>(
            commit = {
                val summary = try {
                    app.domainDeletionCoordinator.deleteWorkout(id, expectedRevisionToken)
                } catch (cancelled: CommittedWorkoutDeletionCancellation) {
                    throw CommittedGymDeletionCancellation(
                        GymDeletionReceipt(GymDeletionKind.Workout, id, cancelled.summary.warnings),
                        cancelled,
                    )
                }
                require(summary.workoutDeleted) {
                    _workoutDeletionImpact.value = null
                    _workoutDeletionTargetMissing.value = true
                    _workoutDeletionPreviewError.value =
                        "Workout no longer exists. It may already have been deleted; close this review and verify History."
                    "Workout no longer exists. It may already have been deleted; close this review and verify History."
                }
                GymDeletionReceipt(GymDeletionKind.Workout, id, summary.warnings)
            },
            followUp = { committed ->
                restTimerScheduler.cancel(id)
                committed
            },
            onCancellation = { committed, cancelled ->
                CommittedGymDeletionCancellation(committed, cancelled)
            },
            onOrdinaryFailure = { committed ->
                committed.copy(
                    warnings = committed.warnings +
                        "Rest-timer cleanup did not finish; the workout itself was deleted.",
                )
            },
        )
    }

    fun duplicateWorkout(id: Long) = runOperation("Copying workout…", "Workout copied into today") {
        repository.duplicateWorkout(id, asActive = true)
    }

    fun copyWorkoutExercise(
        boundary: WorkoutExerciseCopyBoundary,
        requestedWorkoutExerciseUuid: String,
        requestedSetUuids: List<String>,
        requestId: String,
    ): Boolean = runSessionMutation(
        running = "Copying exercise…",
        success = "Exercise sets copied into the active workout",
        requestId = requestId,
        committedReceipt = GymSessionMutationReceipt(GymSessionMutationKind.WorkoutExerciseCopied),
    ) {
        val copiedId = repository.copyWorkoutExerciseToActive(
            boundary,
            requestedWorkoutExerciseUuid,
            requestedSetUuids,
        )
        _pendingWorkoutLayoutUndo.value = null
        GymSessionMutationReceipt(GymSessionMutationKind.WorkoutExerciseCopied, copiedId)
    }

    fun addExercise(
        boundary: WorkoutStructureBoundary,
        exerciseId: Long,
        machineId: Long? = null,
        requestedWorkoutExerciseUuid: String,
        requestedInitialSetUuid: String,
        requestId: String,
    ): Boolean = runSessionMutation(
        running = "Adding exercise…",
        success = "Exercise added · First set ready",
        requestId = requestId,
        committedReceipt = GymSessionMutationReceipt(GymSessionMutationKind.ExerciseAdded),
    ) {
        val receipt = repository.addExerciseWithInitialSetToWorkout(
            boundary,
            exerciseId,
            machineId,
            requestedWorkoutExerciseUuid,
            requestedInitialSetUuid,
        )
        _pendingWorkoutLayoutUndo.value = null
        GymSessionMutationReceipt(GymSessionMutationKind.ExerciseAdded, receipt.workoutExerciseId)
    }

    fun updateWorkoutExerciseDetails(
        boundary: WorkoutPlacementMutationBoundary,
        notes: String,
        groupId: Long?,
        machineId: Long?,
        requestId: String,
    ): Boolean = runSessionMutation(
        running = "Saving exercise details…",
        success = "Exercise updated",
        requestId = requestId,
        committedReceipt = GymSessionMutationReceipt(
            GymSessionMutationKind.ExerciseDetailsUpdated,
            boundary.workoutExerciseId,
        ),
    ) {
        repository.updateWorkoutExerciseDetails(boundary, notes, groupId, machineId)
        GymSessionMutationReceipt(
            GymSessionMutationKind.ExerciseDetailsUpdated,
            boundary.workoutExerciseId,
        )
    }

    fun substituteWorkoutExercise(
        boundary: WorkoutPlacementMutationBoundary,
        exerciseId: Long,
        machineId: Long?,
        requestedWorkoutExerciseUuid: String,
        requestedInitialSetUuid: String,
        requestId: String,
    ): Boolean = runSessionMutation(
        running = "Substituting exercise…",
        success = "Exercise replaced · Completed history preserved",
        requestId = requestId,
        committedReceipt = GymSessionMutationReceipt(GymSessionMutationKind.ExerciseSubstituted),
    ) {
        val replacementId = repository.substituteWorkoutExercise(
            boundary,
            exerciseId,
            machineId,
            requestedWorkoutExerciseUuid,
            requestedInitialSetUuid,
        )
        _pendingWorkoutLayoutUndo.value = null
        GymSessionMutationReceipt(GymSessionMutationKind.ExerciseSubstituted, replacementId)
    }

    fun removeWorkoutExercise(
        boundary: WorkoutPlacementMutationBoundary,
        requestId: String,
    ): Boolean = runSessionMutation(
        running = "Removing exercise…",
        success = "Exercise removed from this workout",
        requestId = requestId,
        committedReceipt = GymSessionMutationReceipt(GymSessionMutationKind.WorkoutExerciseRemoved),
    ) {
        val receipt = repository.removeWorkoutExercise(boundary)
        _pendingWorkoutLayoutUndo.value = null
        GymSessionMutationReceipt(
            kind = GymSessionMutationKind.WorkoutExerciseRemoved,
            targetId = boundary.workoutExerciseId,
            structureReceipt = receipt,
        )
    }

    fun removeWorkoutExerciseFromGroup(
        boundary: WorkoutPlacementMutationBoundary,
        requestId: String,
    ): Boolean = runSessionMutation(
        running = "Removing exercise from group…",
        success = "Exercise is now independent · Undo available",
        requestId = requestId,
        committedReceipt = GymSessionMutationReceipt(GymSessionMutationKind.WorkoutGroupMemberRemoved),
    ) {
        val receipt = repository.removeWorkoutExerciseFromGroup(boundary)
        receipt.previousLayout?.let { snapshot ->
            _pendingWorkoutLayoutUndo.value = WorkoutLayoutUndo(
                boundary = receipt.afterBoundary,
                snapshot = snapshot,
                label = "Undo removal from group",
            )
        }
        GymSessionMutationReceipt(
            kind = GymSessionMutationKind.WorkoutGroupMemberRemoved,
            targetId = boundary.workoutExerciseId,
            structureReceipt = receipt,
        )
    }

    fun reorderWorkoutExercises(sessionId: Long, ids: List<Long>) = runSilentReorder {
        val state = uiState.value
        val boundary = requireNotNull(state.captureWorkoutStructureBoundary()) { "Workout changed; review it and try again" }
        require(boundary.sessionId == sessionId) { "Workout changed; review it and try again" }
        repository.applyWorkoutArrangement(boundary, state.captureWorkoutArrangementDraft(ids))
    }

    fun applyWorkoutArrangement(
        boundary: WorkoutStructureBoundary,
        draft: WorkoutArrangementDraft,
        requestId: String,
    ): Boolean = runSessionMutation(
        running = "Saving workout arrangement…",
        success = "Workout arrangement saved · Undo available",
        requestId = requestId,
        committedReceipt = GymSessionMutationReceipt(GymSessionMutationKind.WorkoutArranged),
    ) {
        val receipt = repository.applyWorkoutArrangement(boundary, draft)
        receipt.previousLayout?.let { snapshot ->
            _pendingWorkoutLayoutUndo.value = WorkoutLayoutUndo(
                boundary = receipt.afterBoundary,
                snapshot = snapshot,
                label = "Undo arrangement",
            )
        }
        GymSessionMutationReceipt(
            kind = GymSessionMutationKind.WorkoutArranged,
            structureReceipt = receipt,
        )
    }

    fun undoWorkoutLayout(undo: WorkoutLayoutUndo, requestId: String): Boolean = runSessionMutation(
        running = "Restoring previous workout layout…",
        success = "Previous workout layout restored",
        requestId = requestId,
        committedReceipt = GymSessionMutationReceipt(GymSessionMutationKind.WorkoutLayoutRestored),
    ) {
        val receipt = repository.restoreWorkoutLayout(undo.boundary, undo.snapshot)
        _pendingWorkoutLayoutUndo.value = null
        GymSessionMutationReceipt(
            kind = GymSessionMutationKind.WorkoutLayoutRestored,
            structureReceipt = receipt,
        )
    }

    fun clearWorkoutLayoutUndo() {
        _pendingWorkoutLayoutUndo.value = null
    }

    fun createGroup(
        boundary: WorkoutStructureBoundary,
        requestedGroupUuid: String,
        name: String,
        type: WorkoutGroupType,
        workoutExerciseUuids: List<String>,
        requestId: String,
    ): Boolean = runSessionMutation(
        running = "Creating exercise group…",
        success = "Exercise group created · Undo available",
        requestId = requestId,
        committedReceipt = GymSessionMutationReceipt(GymSessionMutationKind.WorkoutGroupCreated),
    ) {
        val receipt = repository.createGroup(
            boundary,
            requestedGroupUuid,
            name,
            type,
            workoutExerciseUuids,
        )
        receipt.previousLayout?.let { snapshot ->
            _pendingWorkoutLayoutUndo.value = WorkoutLayoutUndo(
                boundary = receipt.afterBoundary,
                snapshot = snapshot,
                label = "Undo group creation",
            )
        }
        GymSessionMutationReceipt(
            kind = GymSessionMutationKind.WorkoutGroupCreated,
            structureReceipt = receipt,
        )
    }

    fun addSet(boundary: WorkoutPlacementMutationBoundary, requestId: String): Boolean = runSessionMutation(
        running = "Adding Set…",
        success = "Set added",
        requestId = requestId,
        committedReceipt = GymSessionMutationReceipt(GymSessionMutationKind.WorkoutSetAdded),
    ) {
        val setId = repository.addSet(boundary)
        _pendingWorkoutLayoutUndo.value = null
        GymSessionMutationReceipt(GymSessionMutationKind.WorkoutSetAdded, setId)
    }

    fun updateSet(
        boundary: WorkoutSetMutationBoundary,
        draft: WorkoutSetDraft,
        requestId: String,
    ): Boolean = runSessionMutation(
        running = "Saving set…",
        success = "Set saved",
        requestId = requestId,
        committedReceipt = GymSessionMutationReceipt(GymSessionMutationKind.SetUpdated, boundary.setId),
    ) {
        var followUpWarning = false
        completeCommittedPersistence<Unit>(
            commit = { repository.updateSet(boundary, draft) },
            followUp = { routineRepository.rebuildPersonalRecords(exerciseIdForSet(boundary.setId)) },
            onCancellation = { _, cancelled -> CommittedGymMutationCancellation(cancelled) },
            onOrdinaryFailure = { followUpWarning = true },
        )
        GymSessionMutationReceipt(
            GymSessionMutationKind.SetUpdated,
            boundary.setId,
            warnings = if (followUpWarning) {
                listOf("Set saved; personal records will be reconciled when Gym opens again.")
            } else emptyList(),
        )
    }

    internal fun saveQuickSet(
        boundary: QuickSetAuthorshipBoundary,
        draft: WorkoutSetDraft,
        addNext: Boolean,
        restOverrideSeconds: Int? = null,
    ) {
        if (!savingQuickSetIds.add(boundary.setId)) return
        var postCommitWarning: String? = null
        runOperation(
            "Saving set…",
            if (addNext) "Set saved · Next set ready" else "Set saved",
            successOverride = { postCommitWarning },
            successFeedbackPresentationOverride = {
                OperationFeedbackPresentation.Snackbar.takeIf { postCommitWarning != null }
            },
        ) {
            try {
                var followUpWarning = false
                completeCommittedPersistence<QuickSetCommitReceipt>(
                    commit = {
                        repository.saveQuickSet(
                            id = boundary.setId,
                            expectedSetUuid = boundary.setUuid,
                            expectedSetUpdatedAtMillis = boundary.setUpdatedAtMillis,
                            expectedWorkoutRevision = boundary.workoutRevision,
                            draft = draft,
                            addNext = addNext,
                            autoStartRest = app.settingsRepository.current().restTimerAutoStart,
                            restOverrideSeconds = restOverrideSeconds,
                        )
                    },
                    followUp = { receipt ->
                        routineRepository.rebuildPersonalRecords(receipt.exerciseId)
                        if (receipt.restTimerSeconds != null) schedulePersistedRestTimer()
                        receipt
                    },
                    onCancellation = { _, cancelled -> CommittedGymMutationCancellation(cancelled) },
                    onOrdinaryFailure = { committed -> committed.also { followUpWarning = true } },
                ).also {
                    if (it.appendedSetId != null) _pendingWorkoutLayoutUndo.value = null
                    if (followUpWarning) {
                        postCommitWarning =
                            "Set saved · Background records or timer notification will be reconciled when Gym opens again"
                    }
                }
            } finally {
                savingQuickSetIds.remove(boundary.setId)
            }
        }
    }

    fun completeSet(
        boundary: WorkoutSetMutationBoundary,
        completed: Boolean,
        requestId: String,
    ): Boolean = runSessionMutation(
        running = "Updating Set…",
        success = if (completed) "Set completed" else "Set reopened",
        requestId = requestId,
        committedReceipt = GymSessionMutationReceipt(
            GymSessionMutationKind.WorkoutSetCompletionUpdated,
            boundary.setId,
        ),
    ) {
        var followUpWarning = false
        completeCommittedPersistence<Unit>(
            commit = {
                repository.setSetCompleted(
                    boundary,
                    completed,
                    app.settingsRepository.current().restTimerAutoStart,
                )
            },
            followUp = {
                rebuildRecordsForSet(boundary.setId)
                if (completed) schedulePersistedRestTimer()
            },
            onCancellation = { _, cancelled -> CommittedGymMutationCancellation(cancelled) },
            onOrdinaryFailure = { followUpWarning = true },
        )
        GymSessionMutationReceipt(
            kind = GymSessionMutationKind.WorkoutSetCompletionUpdated,
            targetId = boundary.setId,
            warnings = if (followUpWarning) {
                listOf("Set saved; personal records or timer notification will reconcile when Gym opens again.")
            } else emptyList(),
        )
    }

    fun duplicateSet(boundary: WorkoutSetMutationBoundary, requestId: String): Boolean = runSessionMutation(
        running = "Duplicating Set…",
        success = "Set duplicated",
        requestId = requestId,
        committedReceipt = GymSessionMutationReceipt(GymSessionMutationKind.WorkoutSetDuplicated),
    ) {
        val setId = repository.duplicateSet(boundary)
        _pendingWorkoutLayoutUndo.value = null
        GymSessionMutationReceipt(GymSessionMutationKind.WorkoutSetDuplicated, setId)
    }

    fun deleteSet(
        boundary: WorkoutSetMutationBoundary,
        reason: WorkoutSetRemovalReason,
        requestId: String,
    ): Boolean = runSessionMutation(
        running = if (reason == WorkoutSetRemovalReason.Skipped) "Skipping optional Set…" else "Removing Set…",
        success = if (reason == WorkoutSetRemovalReason.Skipped) {
            "Optional Set skipped · Undo available"
        } else {
            "Set marked not performed · Undo available"
        },
        requestId = requestId,
        committedReceipt = GymSessionMutationReceipt(
            GymSessionMutationKind.WorkoutSetRemoved,
            boundary.setId,
            setRemovalReason = reason,
        ),
    ) {
        var followUpWarning = false
        val receipt = completeCommittedPersistence<WorkoutStructureMutationReceipt>(
            commit = { repository.deleteSet(boundary, reason) },
            followUp = { committed -> rebuildRecordsForSet(boundary.setId); committed },
            onCancellation = { _, cancelled -> CommittedGymMutationCancellation(cancelled) },
            onOrdinaryFailure = { committed -> committed.also { followUpWarning = true } },
        )
        _pendingWorkoutLayoutUndo.value = null
        GymSessionMutationReceipt(
            kind = GymSessionMutationKind.WorkoutSetRemoved,
            targetId = boundary.setId,
            warnings = if (followUpWarning) {
                listOf("Set saved; personal records will be reconciled when Gym opens again.")
            } else emptyList(),
            structureReceipt = receipt,
            setRemovalReason = reason,
        )
    }

    fun undoDeleteSet(
        boundary: WorkoutSetMutationBoundary,
        requestId: String,
    ): Boolean = runSessionMutation(
        running = "Restoring Set…",
        success = "Set restored",
        requestId = requestId,
        committedReceipt = GymSessionMutationReceipt(GymSessionMutationKind.WorkoutSetRestored, boundary.setId),
    ) {
        var followUpWarning = false
        val receipt = completeCommittedPersistence<WorkoutStructureMutationReceipt>(
            commit = { repository.undoDeleteSet(boundary) },
            followUp = { committed -> rebuildRecordsForSet(boundary.setId); committed },
            onCancellation = { _, cancelled -> CommittedGymMutationCancellation(cancelled) },
            onOrdinaryFailure = { committed -> committed.also { followUpWarning = true } },
        )
        GymSessionMutationReceipt(
            kind = GymSessionMutationKind.WorkoutSetRestored,
            targetId = boundary.setId,
            warnings = if (followUpWarning) {
                listOf("Set restored; personal records will be reconciled when Gym opens again.")
            } else emptyList(),
            structureReceipt = receipt,
        )
    }

    fun reorderSets(workoutExerciseId: Long, ids: List<Long>) = runSilentReorder {
        val state = uiState.value
        val boundary = requireNotNull(state.captureWorkoutStructureBoundary()) { "Workout changed; review it and try again" }
        repository.applyWorkoutArrangement(
            boundary,
            state.captureWorkoutArrangementDraft(
                setIdsInOrderByWorkoutExerciseId = mapOf(workoutExerciseId to ids),
            ),
        )
    }

    fun startRestTimer(sessionId: Long, seconds: Int) {
        var warning: String? = null
        runOperation(
            "Starting timer…",
            "Rest timer started",
            successOverride = { warning },
            successFeedbackPresentationOverride = {
                OperationFeedbackPresentation.Snackbar.takeIf { warning != null }
            },
        ) {
        completeCommittedPersistence<Unit>(
            commit = { repository.startRestTimer(sessionId, seconds) },
            followUp = { schedulePersistedRestTimer() },
            onCancellation = { _, cancelled -> CommittedGymMutationCancellation(cancelled) },
            onOrdinaryFailure = {
                warning = "Timer saved · Its notification will be reconciled when Gym opens again"
            },
        )
        }
    }

    fun adjustRestTimer(sessionId: Long, delta: Int) {
        var warning: String? = null
        runOperation(
            "Adjusting timer…",
            "Timer adjusted",
            successOverride = { warning },
            successFeedbackPresentationOverride = {
                OperationFeedbackPresentation.Snackbar.takeIf { warning != null }
            },
        ) {
        completeCommittedPersistence<Unit>(
            commit = { repository.adjustRestTimer(sessionId, delta) },
            followUp = { schedulePersistedRestTimer() },
            onCancellation = { _, cancelled -> CommittedGymMutationCancellation(cancelled) },
            onOrdinaryFailure = {
                warning = "Timer adjusted · Its notification will be reconciled when Gym opens again"
            },
        )
        }
    }

    fun stopRestTimer(sessionId: Long) {
        var warning: String? = null
        runOperation(
            "Stopping timer…",
            "Timer stopped",
            successOverride = { warning },
            successFeedbackPresentationOverride = {
                OperationFeedbackPresentation.Snackbar.takeIf { warning != null }
            },
        ) {
        completeCommittedPersistence<Unit>(
            commit = { repository.stopRestTimer(sessionId) },
            followUp = { reconcilePersistedRestTimer(sessionId) },
            onCancellation = { _, cancelled -> CommittedGymMutationCancellation(cancelled) },
            onOrdinaryFailure = {
                warning = "Timer stopped · Notification cleanup will be reconciled when Gym opens again"
            },
        )
        }
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

    fun previewRoutineDeletion(id: Long) {
        val generation = ++routineDeletionPreviewGeneration
        _routineDeletionImpact.value = null
        _routineDeletionPreviewError.value = null
        _routineDeletionTargetMissing.value = false
        viewModelScope.launch {
            try {
                val lookup = checkNotNull(app.withUserDataAccess {
                    GymDeletionPreviewLookup(app.domainDeletionCoordinator.previewRoutineDeletion(id))
                }) { "Whip data is unavailable while recovery is in progress" }
                if (routineDeletionPreviewGeneration == generation) {
                    _routineDeletionImpact.value = lookup.impact
                    if (lookup.impact == null) {
                        _routineDeletionTargetMissing.value = true
                        _routineDeletionPreviewError.value =
                            "Routine no longer exists. It may already have been deleted; close this review and verify the Library."
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (routineDeletionPreviewGeneration == generation) {
                    _routineDeletionPreviewError.value =
                        error.message ?: "Could not review routine deletion impact"
                }
            }
        }
    }

    fun dismissRoutineDeletion() {
        routineDeletionPreviewGeneration++
        _routineDeletionImpact.value = null
        _routineDeletionPreviewError.value = null
        _routineDeletionTargetMissing.value = false
    }

    fun deleteRoutinePermanently(
        id: Long,
        expectedRevisionToken: String,
        requestId: String,
    ): Boolean = runGymDeletion(
        running = "Deleting routine…",
        success = "Routine permanently deleted",
        requestId = requestId,
        savedDescription = "routine deletion",
    ) {
        try {
            val summary = app.domainDeletionCoordinator.deleteRoutine(id, expectedRevisionToken)
            require(summary.routineDeleted) {
                _routineDeletionImpact.value = null
                _routineDeletionTargetMissing.value = true
                _routineDeletionPreviewError.value =
                    "Routine no longer exists. It may already have been deleted; close this review and verify the Library."
                "Routine no longer exists. It may already have been deleted; close this review and verify the Library."
            }
            GymDeletionReceipt(GymDeletionKind.Routine, id, summary.warnings)
        } catch (cancelled: CommittedRoutineDeletionCancellation) {
            throw CommittedGymDeletionCancellation(
                GymDeletionReceipt(
                    GymDeletionKind.Routine,
                    id,
                    cancelled.summary.warnings,
                ),
                cancelled,
            )
        }
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
        routineRepository.rebuildPersonalRecords(exerciseIdForSet(setId))
    }

    private suspend fun exerciseIdForSet(setId: Long): Long {
        val set = repository.sets.first().firstOrNull { it.id == setId } ?: error("Set no longer exists")
        return repository.workoutExercises.first().firstOrNull { it.id == set.workoutExerciseId }?.exerciseId
            ?: error("Workout exercise no longer exists")
    }

    private suspend fun schedulePersistedRestTimer() {
        val session = repository.sessions.first().firstOrNull { it.state == WorkoutSessionState.Active }
            ?: return
        reconcilePersistedRestTimer(session.id, nextExerciseLabel())
    }

    private suspend fun reconcilePersistedRestTimer(sessionId: Long, nextLabel: String? = null) {
        val session = repository.sessions.first().firstOrNull { it.id == sessionId } ?: return
        if (!session.restTimerCleanupPending) return
        val remaining = restTimerScheduleDelaySeconds(
            session.restTimerDeadlineMillis,
            clock.now().toEpochMilli(),
        )
        if (session.state == WorkoutSessionState.Active && session.restTimerDeadlineMillis != null) {
            restTimerScheduler.schedule(
                sessionId = session.id,
                seconds = remaining ?: 1,
                nextLabel = nextLabel,
                timerRevision = session.restTimerRevision,
                expectedDeadlineMillis = session.restTimerDeadlineMillis,
            )
        } else {
            restTimerScheduler.cancel(session.id)
        }
        repository.acknowledgeRestTimerCleanup(session.id, session.restTimerRevision)
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

    private fun runGymDeletion(
        running: String,
        success: String,
        requestId: String,
        savedDescription: String,
        block: suspend () -> GymDeletionReceipt,
    ): Boolean {
        if (!_gymDeletionState.tryStartPersistenceRequest(requestId)) return false
        _operationStatus.value = OperationStatus.Running(running)
        viewModelScope.launch {
            fun successResult(receipt: GymDeletionReceipt): WhipResult.Success<GymDeletionReceipt> {
                val message = if (receipt.warnings.isEmpty()) success else {
                    "$success · ${receipt.warnings.joinToString(" ")}"
                }
                _operationStatus.value = OperationStatus.Succeeded(
                    message,
                    OperationFeedbackPresentation.Snackbar,
                )
                return WhipResult.Success(receipt)
            }

            val result = try {
                val receipt = checkNotNull(app.withUserDataAccess {
                    gymDeletionMutex.withLock { block() }
                }) { "Whip data is unavailable while recovery is in progress" }
                successResult(receipt)
            } catch (cancelled: CommittedGymDeletionCancellation) {
                if (currentCoroutineContext().isActive) {
                    successResult(
                        cancelled.receipt.copy(
                            warnings = cancelled.receipt.warnings +
                                "Some post-delete updates were interrupted; the $savedDescription was committed.",
                        ),
                    )
                } else {
                    if ((_gymDeletionState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                        _gymDeletionState.value = PersistenceRequestState.Idle
                    }
                    _operationStatus.value = OperationStatus.Idle
                    throw cancelled
                }
            } catch (cancelled: CancellationException) {
                if (currentCoroutineContext().isActive) {
                    _operationStatus.value = OperationStatus.Idle
                    WhipResult.Failure(
                        "The $savedDescription was interrupted. Review the impact before retrying.",
                        cancelled,
                    )
                } else {
                    if ((_gymDeletionState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                        _gymDeletionState.value = PersistenceRequestState.Idle
                    }
                    _operationStatus.value = OperationStatus.Idle
                    throw cancelled
                }
            } catch (error: Exception) {
                _operationStatus.value = OperationStatus.Idle
                WhipResult.Failure(
                    error.message ?: "The $savedDescription could not be completed.",
                    error,
                )
            }
            if ((_gymDeletionState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                _gymDeletionState.value = PersistenceRequestState.Finished(requestId, result)
            }
        }
        return true
    }

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

    private fun runSessionMutation(
        running: String,
        success: String,
        requestId: String,
        committedReceipt: GymSessionMutationReceipt,
        block: suspend () -> GymSessionMutationReceipt,
    ): Boolean {
        if (!_sessionMutationState.tryStartPersistenceRequest(requestId)) return false
        _operationStatus.value = OperationStatus.Running(running)
        viewModelScope.launch {
            fun successResult(receipt: GymSessionMutationReceipt): WhipResult.Success<GymSessionMutationReceipt> {
                val message = if (receipt.warnings.isEmpty()) success else {
                    "$success · ${receipt.warnings.joinToString(" ")}"
                }
                _operationStatus.value = OperationStatus.Succeeded(
                    message,
                    if (receipt.warnings.isEmpty()) {
                        OperationFeedbackPresentation.Inline
                    } else {
                        OperationFeedbackPresentation.Snackbar
                    },
                )
                return WhipResult.Success(receipt)
            }

            val result = try {
                val receipt = checkNotNull(app.withUserDataAccess {
                    sessionMutationMutex.withLock { block() }
                }) { "Whip data is unavailable while recovery is in progress" }
                successResult(receipt)
            } catch (committed: CommittedGymMutationCancellation) {
                if (currentCoroutineContext().isActive) {
                    successResult(
                        committedReceipt.copy(
                            warnings = committedReceipt.warnings +
                                "The change was saved; background reconciliation was interrupted and will retry when Gym opens again.",
                        ),
                    )
                } else {
                    if ((_sessionMutationState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                        _sessionMutationState.value = PersistenceRequestState.Idle
                    }
                    _operationStatus.value = OperationStatus.Idle
                    throw committed
                }
            } catch (cancelled: CancellationException) {
                if (currentCoroutineContext().isActive) {
                    val message = "The change was interrupted. Your draft is still here; verify the workout before retrying."
                    _operationStatus.value = OperationStatus.Failed(message, cancelled)
                    WhipResult.Failure(
                        message,
                        cancelled,
                    )
                } else {
                    if ((_sessionMutationState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                        _sessionMutationState.value = PersistenceRequestState.Idle
                    }
                    _operationStatus.value = OperationStatus.Idle
                    throw cancelled
                }
            } catch (error: Exception) {
                val message = error.message ?: "The workout change could not be saved."
                _operationStatus.value = OperationStatus.Failed(message, error)
                WhipResult.Failure(message, error)
            }
            if ((_sessionMutationState.value as? PersistenceRequestState.Running)?.requestId == requestId) {
                _sessionMutationState.value = PersistenceRequestState.Finished(requestId, result)
            }
        }
        return true
    }

    private fun runOperation(
        running: String,
        success: String,
        onFinished: (Boolean) -> Unit = {},
        successFeedbackPresentation: OperationFeedbackPresentation = OperationFeedbackPresentation.Inline,
        recoveryToken: Long? = null,
        successOverride: () -> String? = { null },
        successFeedbackPresentationOverride: () -> OperationFeedbackPresentation? = { null },
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
                    successOverride() ?: success,
                    successFeedbackPresentationOverride() ?: successFeedbackPresentation,
                    recoveryToken,
                )
                runCatching { onFinished(true) }
            } catch (committed: CommittedGymMutationCancellation) {
                _operationStatus.value = OperationStatus.Succeeded(
                    "$success · Saved; a follow-up update was interrupted",
                    successFeedbackPresentation,
                    recoveryToken,
                )
                runCatching { onFinished(true) }
                if (!currentCoroutineContext().isActive) throw committed
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

private fun GymWorkoutSnapshot.toGymData() = GymData(
    exercises = exercises,
    sessions = sessions,
    workoutExercises = workoutExercises,
    sets = sets,
    groups = groups,
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
    val activeSessionWorkoutExercises = active?.let { session ->
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
    val activeWorkoutExercises = activeSessionWorkoutExercises.filter {
        it.workoutExercise.outcome == WorkoutExerciseOutcome.Active
    }
    val performancePlacementIds = active?.let { session ->
        selectWorkoutPerformancePlacements(data.workoutExercises, data.sets, session.id)
            .mapTo(mutableSetOf(), WorkoutExercise::id)
    }.orEmpty()
    val activeWorkoutPerformanceExercises = activeSessionWorkoutExercises.filter { item ->
        item.workoutExercise.id in performancePlacementIds
    }
    val summary = active?.let { session ->
        calculateWorkoutSummary(
            session = session,
            workoutExercises = activeWorkoutPerformanceExercises.map(WorkoutExerciseUi::workoutExercise),
            sets = activeWorkoutPerformanceExercises.flatMap(WorkoutExerciseUi::sets),
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
        activeWorkoutPerformanceExercises = activeWorkoutPerformanceExercises,
        history = data.sessions.filter {
            it.state == WorkoutSessionState.Finished && !it.archived
        }.sortedByDescending(WorkoutSession::startedAt),
        archivedWorkouts = data.sessions.filter(WorkoutSession::archived)
            .sortedByDescending(WorkoutSession::startedAt),
        allSessions = data.sessions,
        allWorkoutExercises = data.workoutExercises,
        allSets = data.sets,
        allWorkoutGroups = data.groups,
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
            workoutExercises = activeWorkoutPerformanceExercises.map(WorkoutExerciseUi::workoutExercise),
            sets = activeWorkoutPerformanceExercises.flatMap(WorkoutExerciseUi::sets),
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
