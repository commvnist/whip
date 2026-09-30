package com.whip.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.whip.app.core.PersistenceRequestState
import com.whip.app.core.WhipResult
import com.whip.app.core.tryStartPersistenceRequest
import com.whip.app.domain.GymMachineDraft
import java.io.Serializable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class RoutineBuilderSetState(
    val key: Long,
    val load: String = "",
    val weightUnitId: String = "",
    val repetitionsMin: String = "",
    val repetitionsMax: String = "",
    val distance: String = "",
    val distanceUnitId: String = "kilometre",
    val durationSeconds: String = "",
    val bodyweightKg: String = "",
    val classification: String = "Working",
    val rpe: String = "",
    val rir: String = "",
    val restSeconds: String = "",
    val tempo: String = "",
    val note: String = "",
    val unilateral: Boolean = false,
    val loadPrescriptionType: String = "Absolute",
    val loadPercentage: String = "",
    val routinePhaseIndex: Int? = null,
    val workSection: String = "Unspecified",
    val optionalWorkKind: String = "None",
    val mainWorkScheme: String? = null,
    val supplementalScheme: String? = null,
) : Serializable

internal data class RoutineBuilderPlacementState(
    val key: Long,
    val exerciseId: Long,
    val exerciseNameSnapshot: String,
    val machineId: Long? = null,
    val equipmentBindingState: String = "None",
    val machineProfileUuidSnapshot: String? = null,
    val machineNameSnapshot: String = "",
    val machineLoadTypeSnapshot: String = "",
    val machineUnitIdSnapshot: String = "",
    val machineLevelLabelSnapshot: String = "",
    val machineLoadInterpretationSnapshot: String = "Total",
    val machineConfigurationGroupSnapshot: String = "",
    val machineConfigurationVersionSnapshot: Int = 1,
    val machineConfigurationSnapshot: String = "",
    val notes: String = "",
    val groupKey: String? = null,
    val copyPreviousWorkout: Boolean = true,
    val sets: List<RoutineBuilderSetState> = emptyList(),
    val trainingMaxPercent: String = "90",
    val progressionPercentages: String = "",
    val alternativeExerciseIds: List<Long> = emptyList(),
    val trainingMaxValue: String = "",
    val trainingMaxUnitId: String = "kilogram",
    val cycleIncrementValue: String = "",
    val trainingMaxSource: String = "EstimatedOneRepMaxPercent",
    val trainingMaxBasisKind: String = "Unspecified",
    val trainingMaxBasisValue: String = "",
    val trainingMaxBasisUnitId: String = "",
    val trainingMaxIncreaseEligible: Boolean = true,
    val mainWorkScheme: String = "Unspecified",
    val supplementalScheme: String = "None",
    val assistanceRole: String = "Unspecified",
    val placementKind: String = "General",
    val assistanceCategory: String = "Unspecified",
    val jokerSetsEnabled: Boolean = false,
) : Serializable

internal data class RoutineBuilderDayState(
    val key: Long,
    val name: String,
    val placements: List<RoutineBuilderPlacementState> = emptyList(),
    val progressionIndex: Int? = null,
) : Serializable

internal data class RoutineBuilderState(
    val token: String = "",
    val dataGeneration: Long = 0L,
    val name: String = "",
    val notes: String = "",
    val days: List<RoutineBuilderDayState> = emptyList(),
    val selectedDayKey: Long? = null,
    val selectedPlacementKey: Long? = null,
    val nextKey: Long = 1L,
    val independentlySavedLibraryItems: Int = 0,
    val programKind: String? = null,
    val programPhaseCount: Int = 1,
    val programPhaseLabels: List<String> = emptyList(),
    val programPhaseRoles: List<String> = emptyList(),
    val trainingMaxAdvanceAfterPhaseIndices: Set<Int> = emptySet(),
    val currentProgramPhaseIndexHint: Int? = null,
    val nextProgramDayKeyHint: Long? = null,
    val programTemplateKey: String = "None",
    val programTemplateRevision: Int = 0,
    val progressionMode: String = "Standard",
    val allowNonStandardHigherSuggestions: Boolean = false,
) : Serializable

/**
 * Owns a routine draft independently of the composable/dialog stack. Only selected
 * placements are saved; the exercise library is queried from Room and is never copied
 * into Activity state. This keeps nested exercise/equipment creation and recreation safe.
 */
internal class RoutineBuilderViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        savedStateHandle.get<RoutineBuilderState>(STATE_KEY) ?: RoutineBuilderState(),
    )
    val state = mutableState.asStateFlow()
    private val mutableLibrarySave = MutableStateFlow<PersistenceRequestState<RoutineLibrarySaveReceipt>>(PersistenceRequestState.Idle)
    val librarySave = mutableLibrarySave.asStateFlow()
    private val mutableRoutineSave = MutableStateFlow<PersistenceRequestState<RoutineBuilderState>>(PersistenceRequestState.Idle)
    val routineSave = mutableRoutineSave.asStateFlow()

    fun saveRoutine(
        requestId: String,
        submitted: RoutineBuilderState,
        save: ((Boolean) -> Unit) -> Unit,
    ): Boolean {
        if (!mutableRoutineSave.tryStartPersistenceRequest(requestId)) return false
        val token = state.value.token
        fun finish(saved: Boolean) {
            if (state.value.token != token ||
                (mutableRoutineSave.value as? PersistenceRequestState.Running)?.requestId != requestId) return
            mutableRoutineSave.value = PersistenceRequestState.Finished(requestId,
                if (saved) WhipResult.Success(submitted)
                else WhipResult.Failure("The Routine save could not be confirmed. Your draft is still here; check the Routine Library before retrying."))
        }
        try { save(::finish) }
        catch (cancelled: kotlinx.coroutines.CancellationException) {
            if ((mutableRoutineSave.value as? PersistenceRequestState.Running)?.requestId == requestId) mutableRoutineSave.value = PersistenceRequestState.Idle
            throw cancelled
        }
        catch (_: Exception) { finish(false) }
        return true
    }

    fun consumeRoutineSave(requestId: String) {
        if ((mutableRoutineSave.value as? PersistenceRequestState.Finished)?.requestId == requestId) {
            mutableRoutineSave.value = PersistenceRequestState.Idle
        }
    }

    /** The retained owner receives late callbacks; a recreated dialog only consumes its own request. */
    fun saveLibraryItem(
        requestId: String,
        receipt: (Long) -> RoutineLibrarySaveReceipt,
        save: ((Long?) -> Unit) -> Unit,
    ): Boolean {
        if (!mutableLibrarySave.tryStartPersistenceRequest(requestId)) return false
        val token = state.value.token
        fun finish(id: Long?) {
            if (state.value.token != token ||
                (mutableLibrarySave.value as? PersistenceRequestState.Running)?.requestId != requestId) return
            mutableLibrarySave.value = PersistenceRequestState.Finished(requestId,
                if (id != null) WhipResult.Success(receipt(id))
                else WhipResult.Failure("The Library item could not be saved. Your changes are still here; try again."))
        }
        try { save(::finish) }
        catch (cancelled: kotlinx.coroutines.CancellationException) {
            if ((mutableLibrarySave.value as? PersistenceRequestState.Running)?.requestId == requestId) mutableLibrarySave.value = PersistenceRequestState.Idle
            throw cancelled
        }
        catch (_: Exception) { finish(null) }
        return true
    }

    fun consumeLibrarySave(requestId: String) {
        if ((mutableLibrarySave.value as? PersistenceRequestState.Finished)?.requestId == requestId) {
            mutableLibrarySave.value = PersistenceRequestState.Idle
        }
    }

    fun initialize(token: String, initial: RoutineBuilderState, dataGeneration: Long = 0L) {
        if (
            mutableState.value.token == token &&
            mutableState.value.dataGeneration == dataGeneration
        ) return
        mutableLibrarySave.value = PersistenceRequestState.Idle
        mutableRoutineSave.value = PersistenceRequestState.Idle
        set(initial.copy(token = token, dataGeneration = dataGeneration))
    }

    fun update(transform: (RoutineBuilderState) -> RoutineBuilderState) {
        if (mutableRoutineSave.value is PersistenceRequestState.Running) return
        set(transform(mutableState.value))
    }

    fun nextKey(): Long {
        val key = mutableState.value.nextKey
        set(mutableState.value.copy(nextKey = key + 1L))
        return key
    }

    fun noteIndependentLibrarySave() {
        update { it.copy(independentlySavedLibraryItems = it.independentlySavedLibraryItems + 1) }
    }

    fun clear() {
        mutableLibrarySave.value = PersistenceRequestState.Idle
        mutableRoutineSave.value = PersistenceRequestState.Idle
        savedStateHandle.remove<RoutineBuilderState>(STATE_KEY)
        mutableState.value = RoutineBuilderState()
    }

    private fun set(value: RoutineBuilderState) {
        mutableState.value = value
        savedStateHandle[STATE_KEY] = value
    }

    private companion object {
        const val STATE_KEY = "routine-builder-state"
    }
}

internal data class RoutineLibrarySaveReceipt(
    val id: Long,
    val exerciseName: String = "",
    val dayKey: Long? = null,
    val forMachineProfile: Boolean = false,
    val placementKey: Long? = null,
    val machineDraft: GymMachineDraft? = null,
)
