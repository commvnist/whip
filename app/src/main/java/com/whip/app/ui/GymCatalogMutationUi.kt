package com.whip.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.whip.app.core.PersistenceRequestState
import com.whip.app.domain.EstimatedOneRepMaxFormula
import com.whip.app.domain.Exercise
import com.whip.app.domain.GymMachine
import com.whip.app.domain.WorkoutPlacementMutationBoundary
import com.whip.app.domain.WorkoutStructureBoundary

/**
 * Gives the active Exercise/Machine Library editor sole ownership of its save
 * result without adding another state machine to the already broad Gym shell.
 */
@Composable
internal fun rememberGymCatalogMutationCoordinator(
    editorOpen: Boolean,
    state: PersistenceRequestState<GymCatalogMutationReceipt>,
    consume: (String) -> Unit,
    onExerciseCreatedForMachine: (Long) -> Unit,
    onCatalogSaved: () -> Unit,
): EntitySaveCoordinator? = if (editorOpen) {
    rememberPersistenceRequestCoordinator(
        state = state,
        consume = consume,
        key = "gym-catalog-editor",
        requestNamespace = "gym-catalog-editor",
        orphanedMessage =
            "The previous Library save was interrupted. Your changes are still here; check the Library before retrying.",
        onPersisted = { receipt ->
            when (receipt.kind) {
                GymCatalogMutationKind.ExerciseCreatedForMachine ->
                    onExerciseCreatedForMachine(receipt.targetId)
                GymCatalogMutationKind.MachineSaved,
                GymCatalogMutationKind.MachineVersionCreated,
                GymCatalogMutationKind.ExerciseSaved,
                -> onCatalogSaved()
                GymCatalogMutationKind.CategorySaved -> Unit
            }
        },
    )
} else null

@Composable
internal fun GymMachineEditorOverlay(
    visible: Boolean,
    modifier: Modifier,
    state: GymUiState,
    machineEditor: GymMachine?,
    machineVersionSource: GymMachine?,
    inlineMachineBoundary: WorkoutPlacementMutationBoundary?,
    inlineMachineExerciseId: Long?,
    createdExerciseForMachineId: Long?,
    catalogCoordinator: EntitySaveCoordinator?,
    sessionCoordinator: EntitySaveCoordinator,
    viewModel: GymViewModel,
    onCreatedExerciseConsumed: () -> Unit,
    onCreateExercise: (String) -> Unit,
    onCreateVersion: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    MachineEditorDialog(
        modifier = modifier,
        machine = machineVersionSource ?: machineEditor,
        exercises = if (machineEditor == null) state.exercises else state.exercises + state.archivedExercises,
        definitionLocked = machineVersionSource == null && machineEditor?.let { selected ->
            state.allWorkoutExercises.any { it.machineId == selected.id }
        } == true,
        creatingVersion = machineVersionSource != null,
        initialExerciseId = inlineMachineExerciseId,
        createdExerciseIdRequest = createdExerciseForMachineId,
        onCreatedExerciseRequestConsumed = onCreatedExerciseConsumed,
        onCreateExercise = onCreateExercise,
        onCreateVersion = machineEditor?.takeIf { selected ->
            state.allWorkoutExercises.any { it.machineId == selected.id }
        }?.let { selected -> { onCreateVersion(selected.id) } },
        saving = catalogCoordinator?.saving == true ||
            (inlineMachineBoundary != null && sessionCoordinator.saving),
        errorMessage = if (inlineMachineBoundary != null) {
            sessionCoordinator.errorMessage
        } else catalogCoordinator?.errorMessage,
        onDismiss = {
            if (catalogCoordinator?.saving != true) onDismiss()
        },
        onSave = { draft ->
            if (inlineMachineBoundary != null) {
                sessionCoordinator.begin()?.let { requestId ->
                    if (!viewModel.createMachineAndAssign(inlineMachineBoundary, draft, requestId)) {
                        sessionCoordinator.finishFailure(
                            "Another workout change is still saving. Wait before creating the machine.",
                        )
                    }
                }
            } else {
                catalogCoordinator?.begin()?.let { requestId ->
                    val started = if (machineVersionSource != null) {
                        viewModel.createMachineVersion(machineVersionSource.id, draft, requestId)
                    } else {
                        viewModel.saveMachine(machineEditor?.id, draft, requestId)
                    }
                    if (!started) {
                        catalogCoordinator.finishFailure(
                            "Another Library change is still saving. Wait for it, then try again.",
                        )
                    }
                }
            }
        },
    )
}

@Composable
internal fun GymExerciseEditorOverlay(
    visible: Boolean,
    modifier: Modifier,
    state: GymUiState,
    exerciseEditor: Exercise?,
    initialName: String,
    creatingExerciseForMachine: Boolean,
    createExerciseAddBoundary: WorkoutStructureBoundary?,
    createForSubstitutionBoundary: WorkoutPlacementMutationBoundary?,
    requestedWorkoutExerciseUuid: String?,
    requestedInitialSetUuid: String?,
    catalogCoordinator: EntitySaveCoordinator?,
    sessionCoordinator: EntitySaveCoordinator,
    viewModel: GymViewModel,
    onDismissMachineExercise: () -> Unit,
    onDismissCatalog: () -> Unit,
) {
    if (!visible) return
    ExerciseEditorDialog(
        modifier = modifier,
        exercise = exerciseEditor,
        initialName = initialName,
        categories = state.categories,
        selectedCategoryIds = state.categoryLinks.filter { it.exerciseId == exerciseEditor?.id }
            .mapTo(mutableSetOf()) { it.categoryId },
        defaultWeightUnit = state.appSettings.gymWeightUnitId,
        defaultRestSeconds = state.appSettings.defaultRestSeconds,
        defaultFormula = runCatching {
            EstimatedOneRepMaxFormula.valueOf(state.appSettings.oneRepMaxFormula)
        }.getOrDefault(EstimatedOneRepMaxFormula.Epley),
        platePresets = state.appSettings.platePresets,
        powerMode = state.appSettings.powerMode,
        saving = catalogCoordinator?.saving == true || sessionCoordinator.saving,
        errorMessage = sessionCoordinator.errorMessage.takeIf {
            createExerciseAddBoundary != null || createForSubstitutionBoundary != null
        } ?: catalogCoordinator?.errorMessage,
        onDismiss = {
            if (creatingExerciseForMachine) {
                if (catalogCoordinator?.saving != true) {
                    catalogCoordinator?.clear()
                    onDismissMachineExercise()
                }
            } else {
                onDismissCatalog()
            }
        },
        onSave = { draft ->
            if (creatingExerciseForMachine) {
                catalogCoordinator?.begin()?.let { requestId ->
                    if (!viewModel.createExerciseForMachine(draft, requestId)) {
                        catalogCoordinator.finishFailure(
                            "Another Library change is still saving. Wait for it, then try again.",
                        )
                    }
                }
            } else {
                val placementUuid = requestedWorkoutExerciseUuid
                val setUuid = requestedInitialSetUuid
                if (createForSubstitutionBoundary != null && placementUuid != null && setUuid != null) {
                    sessionCoordinator.begin()?.let { requestId ->
                        if (!viewModel.createExerciseAndSubstitute(
                                createForSubstitutionBoundary,
                                draft,
                                placementUuid,
                                setUuid,
                                requestId,
                            )
                        ) {
                            sessionCoordinator.finishFailure(
                                "Another workout change is still saving. Wait for it before trying again.",
                            )
                        }
                    }
                } else if (createExerciseAddBoundary != null && placementUuid != null && setUuid != null) {
                    sessionCoordinator.begin()?.let { requestId ->
                        if (!viewModel.createExerciseAndAdd(
                                createExerciseAddBoundary,
                                draft,
                                placementUuid,
                                setUuid,
                                requestId,
                            )
                        ) {
                            sessionCoordinator.finishFailure(
                                "Another workout change is still saving. Wait for it before trying again.",
                            )
                        }
                    }
                } else {
                    catalogCoordinator?.begin()?.let { requestId ->
                        if (!viewModel.saveExercise(exerciseEditor?.id, draft, requestId)) {
                            catalogCoordinator.finishFailure(
                                "Another Library change is still saving. Wait for it, then try again.",
                            )
                        }
                    }
                }
            }
        },
    )
}
