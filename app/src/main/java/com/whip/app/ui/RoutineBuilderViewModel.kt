package com.whip.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
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

    fun initialize(token: String, initial: RoutineBuilderState) {
        if (mutableState.value.token == token) return
        set(initial.copy(token = token))
    }

    fun update(transform: (RoutineBuilderState) -> RoutineBuilderState) {
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
