package com.whip.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whip.app.WhipApplication
import com.whip.app.domain.EstimatedOneRepMaxFormula
import com.whip.app.domain.Exercise
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.ExerciseTrackingType
import com.whip.app.domain.GymMachine
import com.whip.app.domain.GymMachineDraft
import com.whip.app.domain.LoadInterpretation
import com.whip.app.domain.MachineLevelDirection
import com.whip.app.domain.MachineLoadType
import com.whip.app.domain.PersonalRecordType
import com.whip.app.domain.RoutineDayDraft
import com.whip.app.domain.RoutineDraft
import com.whip.app.domain.RoutineEquipmentBindingState
import com.whip.app.domain.RoutineLoadPrescriptionType
import com.whip.app.domain.RoutineMainWorkScheme
import com.whip.app.domain.RoutineOptionalWorkKind
import com.whip.app.domain.RoutineProgramPhaseRole
import com.whip.app.domain.RoutineProgramDraft
import com.whip.app.domain.RoutineProgramKind
import com.whip.app.domain.RoutineExerciseDraft
import com.whip.app.domain.RoutineAssistanceRole
import com.whip.app.domain.RoutineAssistanceCategory
import com.whip.app.domain.RoutinePlacementKind
import com.whip.app.domain.RoutineProgramTemplateKey
import com.whip.app.domain.RoutineProgressionMode
import com.whip.app.domain.RoutineSupplementalScheme
import com.whip.app.domain.RoutineTrainingMaxSource
import com.whip.app.domain.TrainingMaxBasisKind
import com.whip.app.domain.RoutineWorkSection
import com.whip.app.domain.WorkoutSession
import com.whip.app.domain.WorkoutSetClassification
import com.whip.app.domain.WorkoutSetDraft
import com.whip.app.domain.balancedOncePerLiftDayOwners
import com.whip.app.domain.FIVE_THREE_ONE_ONCE_PER_LIFT_PROTOCOL_REVISION
import com.whip.app.domain.convertPracticalMassValue
import com.whip.app.domain.editableNumericValue
import com.whip.app.domain.massFromKilograms
import com.whip.app.domain.supportsRoutinePercentagePrescription
import com.whip.app.domain.unitSymbol
import com.whip.app.domain.toWhipDoubleOrNull
import com.whip.app.core.RepPrescriptionScheme
import java.io.Serializable
import java.util.UUID
import kotlin.math.absoluteValue

private enum class RoutineBuilderPage { Outline, ProgramStructure, ExercisePicker, WorkoutPicker }

private data class DeletedPlacementUndo(
    val dayKey: Long,
    val index: Int,
    val placement: RoutineBuilderPlacementState,
    val formerGroupMemberKeys: List<Long>,
) : Serializable

/** Draft-only inputs. Persisted Training Max provenance changes only when the user applies them. */
private data class PendingTrainingMaxDerivation(
    val exerciseId: Long,
    val basisKind: String,
    val basisValue: String,
    val basisUnitId: String,
    val percentage: String,
) : Serializable {
    fun matchesApplied(placement: RoutineBuilderPlacementState): Boolean =
        basisKind == placement.trainingMaxBasisKind &&
            basisUnitId == placement.trainingMaxBasisUnitId &&
            basisValue.numericallyEquals(placement.trainingMaxBasisValue) &&
            percentage.numericallyEquals(placement.trainingMaxPercent)
}

private fun String.numericallyEquals(other: String): Boolean {
    val first = toWhipDoubleOrNull()
    val second = other.toWhipDoubleOrNull()
    return if (first != null && second != null) (first - second).absoluteValue < 1e-9 else this == other
}

@Composable
internal fun RoutineBuilderScreen(
    modifier: Modifier = Modifier,
    routineId: Long?,
    gymState: GymUiState,
    initial: RoutineDraft?,
    dialogModifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onSave: (RoutineDraft, (Boolean) -> Unit) -> Unit,
    onCreateExercise: (ExerciseDraft, (Long?) -> Unit) -> Unit,
    onCreateMachine: (GymMachineDraft, (Long?) -> Unit) -> Unit,
    onSavePrescriptionScheme: (RepPrescriptionScheme) -> Unit = {},
    onReorderPrescriptionSchemes: (List<RepPrescriptionScheme>) -> Unit = {},
    onDeletePrescriptionScheme: (String) -> Unit = {},
) {
    val app = LocalContext.current.applicationContext as WhipApplication
    val dataGeneration by app.userDataGeneration.collectAsStateWithLifecycle()
    val token = "routine-${routineId ?: "new"}-g$dataGeneration"
    val stateHolder: RoutineBuilderViewModel = viewModel(
        key = "routine-builder-${routineId ?: "new"}-g$dataGeneration",
    )
    val storedBuilder by stateHolder.state.collectAsStateWithLifecycle()
    var savedBaseline by rememberSaveable(token) {
        mutableStateOf(
            buildInitialRoutineState(
                token,
                initial,
                gymState.exercises + gymState.archivedExercises,
                gymState.machines + gymState.archivedMachines,
            ).copy(dataGeneration = dataGeneration),
        )
    }
    val builder = storedBuilder.takeIf {
        it.token == token && it.dataGeneration == dataGeneration
    } ?: savedBaseline
    LaunchedEffect(token, dataGeneration) {
        stateHolder.initialize(token, savedBaseline, dataGeneration)
    }

    var page by rememberSaveable(token) { mutableStateOf(RoutineBuilderPage.Outline) }
    var pickerSelection by rememberSaveable(token) { mutableStateOf<List<Long>>(emptyList()) }
    var pendingAssistanceRole by rememberSaveable(token) { mutableStateOf<RoutineAssistanceRole?>(null) }
    var showCreateExercise by rememberSaveable(token) { mutableStateOf(false) }
    var exerciseNameSeed by rememberSaveable(token) { mutableStateOf("") }
    var librarySaveInFlight by rememberSaveable(token) { mutableStateOf(false) }
    var routineSaveInFlight by rememberSaveable(token) { mutableStateOf(false) }
    var machineEditorPlacementKey by rememberSaveable(token) { mutableStateOf<Long?>(null) }
    var quickMachinePlacementKey by rememberSaveable(token) { mutableStateOf<Long?>(null) }
    var equipmentPickerPlacementKey by rememberSaveable(token) { mutableStateOf<Long?>(null) }
    var showDiscardConfirmation by rememberSaveable(token) { mutableStateOf(false) }
    var showAdvancedSetFields by rememberSaveable(token) { mutableStateOf(false) }
    var deletedDayUndo by rememberSaveable(token) { mutableStateOf<Pair<Int, RoutineBuilderDayState>?>(null) }
    var deletedPlacementUndo by rememberSaveable(token) { mutableStateOf<DeletedPlacementUndo?>(null) }
    var showFiveThreeOneProgramSetup by rememberSaveable(token) { mutableStateOf(false) }
    var standardLiftCreationInFlight by rememberSaveable(token) { mutableStateOf(false) }
    var standardLiftCreationError by rememberSaveable(token) { mutableStateOf<String?>(null) }
    var savedInPlaceMessage by rememberSaveable(token) { mutableStateOf<String?>(null) }
    var pendingTrainingMaxDerivations by rememberSaveable(token) {
        mutableStateOf<List<PendingTrainingMaxDerivation>>(emptyList())
    }

    val selectedDay = builder.days.firstOrNull { it.key == builder.selectedDayKey }
        ?: builder.days.firstOrNull()
    val selectedPlacement = builder.days.asSequence().flatMap { it.placements.asSequence() }
        .firstOrNull { it.key == builder.selectedPlacementKey }
    val initialComparable = savedBaseline.copy(token = builder.token)
    val isDirty = builder.copy(
        selectedDayKey = initialComparable.selectedDayKey,
        selectedPlacementKey = initialComparable.selectedPlacementKey,
        nextKey = initialComparable.nextKey,
        independentlySavedLibraryItems = initialComparable.independentlySavedLibraryItems,
    ) != initialComparable || pendingTrainingMaxDerivations.isNotEmpty()
    LaunchedEffect(isDirty) {
        if (isDirty) savedInPlaceMessage = null
    }
    val requestDismiss = {
        if (isDirty) showDiscardConfirmation = true
        else {
            stateHolder.clear()
            onDismiss()
        }
    }
    BackHandler(enabled = !showDiscardConfirmation && !showCreateExercise && machineEditorPlacementKey == null && quickMachinePlacementKey == null) {
        when {
            equipmentPickerPlacementKey != null -> equipmentPickerPlacementKey = null
            page != RoutineBuilderPage.Outline -> page = RoutineBuilderPage.Outline
            selectedPlacement != null -> stateHolder.update { it.copy(selectedPlacementKey = null) }
            else -> requestDismiss()
        }
    }

    fun updateDay(dayKey: Long, transform: (RoutineBuilderDayState) -> RoutineBuilderDayState) {
        stateHolder.update { current ->
            current.copy(days = current.days.map { day -> if (day.key == dayKey) transform(day) else day })
        }
    }

    fun updatePlacement(placementKey: Long, transform: (RoutineBuilderPlacementState) -> RoutineBuilderPlacementState) {
        stateHolder.update { current -> current.updateProgramPlacement(placementKey, transform) }
    }

    fun freeWeightUnitId(placement: RoutineBuilderPlacementState): String = (gymState.exercises + gymState.archivedExercises)
        .firstOrNull { it.id == placement.exerciseId }
        ?.weightUnitId
        ?.takeIf(String::isNotBlank)
        ?: gymState.appSettings.gymWeightUnitId

    fun createMissingStandardLifts() {
        val standardNames = mapOf(
            FiveThreeOneLiftRole.Squat to "Squat",
            FiveThreeOneLiftRole.Bench to "Bench Press",
            FiveThreeOneLiftRole.Deadlift to "Deadlift",
            FiveThreeOneLiftRole.Press to "Overhead Press",
        )
        val missing = FiveThreeOneLiftRole.entries.filter { role ->
            gymState.exercises.none { exercise ->
                exercise.trackingType == ExerciseTrackingType.WeightReps && role.matchesExerciseName(exercise.name)
            }
        }
        if (missing.isEmpty() || standardLiftCreationInFlight) return
        standardLiftCreationInFlight = true
        standardLiftCreationError = null
        fun createAt(index: Int) {
            if (index >= missing.size) {
                standardLiftCreationInFlight = false
                return
            }
            val role = missing[index]
            val unitId = gymState.appSettings.gymWeightUnitId
            onCreateExercise(
                ExerciseDraft(
                    name = requireNotNull(standardNames[role]),
                    trackingType = ExerciseTrackingType.WeightReps,
                    equipment = "Barbell",
                    weightUnitId = unitId,
                    weightIncrement = if (unitId == "pound") 5.0 else 2.5,
                    primaryMuscles = when (role) {
                        FiveThreeOneLiftRole.Squat -> "Quadriceps, glutes"
                        FiveThreeOneLiftRole.Bench -> "Chest, triceps"
                        FiveThreeOneLiftRole.Deadlift -> "Posterior chain"
                        FiveThreeOneLiftRole.Press -> "Shoulders, triceps"
                    },
                ),
            ) { createdId ->
                if (createdId == null) {
                    standardLiftCreationInFlight = false
                    standardLiftCreationError = "Could not create ${standardNames[role]}. No other exercises were removed or changed."
                } else {
                    stateHolder.noteIndependentLibrarySave()
                    createAt(index + 1)
                }
            }
        }
        createAt(0)
    }

    fun applyFiveThreeOneProgram(placementKey: Long, result: FiveThreeOneBuilderResult) {
        stateHolder.update { current ->
            // Existing structured programs are edited from Program Structure. Re-running a
            // canonical four-phase generator here would silently erase Leader/Anchor/test phases.
            if (current.programKind.isFiveThreeOneProgramKindName()) return@update current
            // A per-placement conversion cannot create the required Main prescription on other
            // days. Multi-day drafts must use the whole-program setup below instead.
            if (current.days.size != 1) return@update current
            current.copy(
                programKind = result.programKind.name,
                programTemplateKey = RoutineProgramTemplateKey.FiveThreeOneCustom.name,
                programTemplateRevision = 1,
                programPhaseCount = FiveThreeOnePhase.entries.size,
                programPhaseLabels = FiveThreeOnePhase.entries.map(FiveThreeOnePhase::label),
                programPhaseRoles = listOf(
                    RoutineProgramPhaseRole.Standard.name,
                    RoutineProgramPhaseRole.Standard.name,
                    RoutineProgramPhaseRole.Standard.name,
                    RoutineProgramPhaseRole.Deload.name,
                ),
                trainingMaxAdvanceAfterPhaseIndices = setOf(FiveThreeOnePhase.Deload.ordinal),
                currentProgramPhaseIndexHint = 0,
                nextProgramDayKeyHint = current.days.firstOrNull()?.key,
            ).updateProgramPlacement(placementKey) { placement ->
                placement.copy(
                    sets = result.sets,
                    copyPreviousWorkout = false,
                    trainingMaxValue = editableNumericValue(result.trainingMax),
                    trainingMaxUnitId = result.trainingMaxUnitId,
                    cycleIncrementValue = editableNumericValue(result.cycleIncrementValue),
                    trainingMaxSource = RoutineTrainingMaxSource.Explicit.name,
                    mainWorkScheme = result.mainWorkScheme.name,
                    supplementalScheme = result.supplementalScheme.name,
                    assistanceRole = RoutineAssistanceRole.MainLift.name,
                    placementKind = RoutinePlacementKind.MainLift.name,
                    assistanceCategory = RoutineAssistanceCategory.Unspecified.name,
                    jokerSetsEnabled = result.jokerSetsEnabled,
                )
            }
        }
    }

    fun addExercises(dayKey: Long, exerciseIds: List<Long>, assistanceRole: RoutineAssistanceRole? = null) {
        if (exerciseIds.isEmpty()) return
        stateHolder.update { current ->
            var next = current.nextKey
            val additions = exerciseIds.mapNotNull { exerciseId ->
                val exercise = gymState.exercises.firstOrNull { it.id == exerciseId } ?: return@mapNotNull null
                RoutineBuilderPlacementState(
                    key = next++,
                    exerciseId = exercise.id,
                    exerciseNameSnapshot = exercise.name,
                    assistanceRole = assistanceRole?.name ?: RoutineAssistanceRole.Unspecified.name,
                    placementKind = if (assistanceRole == null) {
                        RoutinePlacementKind.General.name
                    } else {
                        RoutinePlacementKind.Assistance.name
                    },
                    assistanceCategory = assistanceRole?.toBuilderAssistanceCategory()?.name
                        ?: RoutineAssistanceCategory.Unspecified.name,
                    sets = listOf(
                        RoutineBuilderSetState(
                            key = next++,
                            workSection = if (assistanceRole == null) {
                                RoutineWorkSection.Unspecified.name
                            } else {
                                RoutineWorkSection.Assistance.name
                            },
                        ),
                    ),
                )
            }
            current.copy(
                days = current.days.map { day -> if (day.key == dayKey) day.copy(placements = day.placements + additions) else day },
                selectedPlacementKey = if (assistanceRole == null) {
                    additions.lastOrNull()?.key ?: current.selectedPlacementKey
                } else {
                    null
                },
                nextKey = next,
            )
        }
    }

    fun addCreatedExercise(
        dayKey: Long,
        exerciseId: Long,
        exerciseName: String,
        assistanceRole: RoutineAssistanceRole? = null,
    ) {
        stateHolder.update { current ->
            val placementKey = current.nextKey
            val setKey = placementKey + 1L
            val placement = RoutineBuilderPlacementState(
                key = placementKey,
                exerciseId = exerciseId,
                exerciseNameSnapshot = exerciseName,
                assistanceRole = assistanceRole?.name ?: RoutineAssistanceRole.Unspecified.name,
                placementKind = if (assistanceRole == null) {
                    RoutinePlacementKind.General.name
                } else {
                    RoutinePlacementKind.Assistance.name
                },
                assistanceCategory = assistanceRole?.toBuilderAssistanceCategory()?.name
                    ?: RoutineAssistanceCategory.Unspecified.name,
                sets = listOf(
                    RoutineBuilderSetState(
                        setKey,
                        workSection = if (assistanceRole == null) {
                            RoutineWorkSection.Unspecified.name
                        } else {
                            RoutineWorkSection.Assistance.name
                        },
                    ),
                ),
            )
            current.copy(
                days = current.days.map { day -> if (day.key == dayKey) day.copy(placements = day.placements + placement) else day },
                selectedPlacementKey = placementKey.takeIf { assistanceRole == null },
                nextKey = setKey + 1L,
                independentlySavedLibraryItems = current.independentlySavedLibraryItems + 1,
            )
        }
    }

    val validationErrors = routineBuilderValidationErrors(builder, gymState)
    val canSave = !librarySaveInFlight && !routineSaveInFlight &&
        pendingTrainingMaxDerivations.isEmpty() && builder.name.isNotBlank() &&
        builder.days.isNotEmpty() && builder.days.all { it.name.isNotBlank() } && validationErrors.isEmpty()

    Surface(modifier.fillMaxSize().testTag("routine-builder"), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            RoutineBuilderHeader(
                editing = routineId != null,
                page = page,
                canSave = canSave && (routineId == null || isDirty),
                onBack = {
                    when {
                        equipmentPickerPlacementKey != null -> equipmentPickerPlacementKey = null
                        page != RoutineBuilderPage.Outline -> page = RoutineBuilderPage.Outline
                        selectedPlacement != null -> stateHolder.update { it.copy(selectedPlacementKey = null) }
                        else -> requestDismiss()
                    }
                },
                onCancel = requestDismiss,
                onSave = {
                    routineSaveInFlight = true
                    onSave(builder.toRoutineDraft(gymState)) { saved ->
                        routineSaveInFlight = false
                        if (saved) {
                            if (routineId == null) {
                                stateHolder.clear()
                                onDismiss()
                            } else {
                                savedBaseline = builder
                                savedInPlaceMessage = "Routine saved. Continue editing ${selectedDay?.name ?: "this routine"}."
                            }
                        }
                    }
                },
            )

            savedInPlaceMessage?.let { message ->
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth().testTag("routine-saved-in-place"),
                ) {
                    Text(
                        message,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (builder.independentlySavedLibraryItems > 0) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${builder.independentlySavedLibraryItems} new library item${if (builder.independentlySavedLibraryItems == 1) " was" else "s were"} saved independently and will remain if this routine is canceled.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            deletedDayUndo?.let { (index, day) ->
                UndoRow("Deleted ${day.name}") {
                    stateHolder.update { current ->
                        val restored = current.days.toMutableList().also { it.add(index.coerceIn(0, it.size), day) }
                        current.copy(days = restored, selectedDayKey = day.key)
                    }
                    deletedDayUndo = null
                }
            }
            deletedPlacementUndo?.let { undo ->
                UndoRow("Removed ${undo.placement.exerciseNameSnapshot}") {
                    updateDay(undo.dayKey) { day ->
                        day.restorePlacement(undo.index, undo.placement, undo.formerGroupMemberKeys)
                    }
                    deletedPlacementUndo = null
                }
            }

            when (page) {
                RoutineBuilderPage.ExercisePicker -> ExercisePickerPage(
                    modifier = Modifier.weight(1f),
                    gymState = gymState,
                    dayName = selectedDay?.name.orEmpty(),
                    assistanceRole = pendingAssistanceRole,
                    selectedIds = pickerSelection,
                    onSelectionChange = { pickerSelection = it },
                    onCreateExercise = { seed -> exerciseNameSeed = seed; showCreateExercise = true },
                    onAdd = {
                        selectedDay?.let { day -> addExercises(day.key, pickerSelection, pendingAssistanceRole) }
                        pickerSelection = emptyList()
                        pendingAssistanceRole = null
                        page = RoutineBuilderPage.Outline
                    },
                )
                RoutineBuilderPage.WorkoutPicker -> WorkoutPickerPage(
                    modifier = Modifier.weight(1f),
                    gymState = gymState,
                    onChoose = { session ->
                        selectedDay?.let { day -> importWorkoutIntoDay(stateHolder, day.key, session, gymState) }
                        page = RoutineBuilderPage.Outline
                    },
                )
                RoutineBuilderPage.ProgramStructure -> RoutineProgramStructurePage(
                    modifier = Modifier.weight(1f),
                    builder = builder,
                    gymState = gymState,
                    currentProgramPhaseIndex = routineId?.let { id ->
                        gymState.routines.firstOrNull { it.id == id }?.currentProgramPhaseIndex
                    },
                    pendingTrainingMaxDerivations = pendingTrainingMaxDerivations,
                    onPendingTrainingMaxDerivationChange = { exerciseId, pending ->
                        pendingTrainingMaxDerivations = pendingTrainingMaxDerivations
                            .filterNot { it.exerciseId == exerciseId }
                            .let { current -> if (pending == null) current else current + pending }
                    },
                    onBuilderChange = stateHolder::update,
                )
                RoutineBuilderPage.Outline -> {
                    if (selectedDay == null) return@Surface
                    BoxWithConstraints(Modifier.weight(1f)) {
                        val showMasterDetail = maxWidth >= 720.dp &&
                            LocalDensity.current.fontScale <= 1.2f &&
                            selectedPlacement != null
                        if (showMasterDetail) {
                            Row(Modifier.fillMaxSize()) {
                                RoutineOutlinePane(
                                    modifier = Modifier.weight(0.44f),
                                    builder = builder,
                                    selectedDay = selectedDay,
                                    selectedPlacementKey = selectedPlacement.key,
                                    gymState = gymState,
                                    validationErrors = validationErrors,
                                    onBuilderChange = stateHolder::update,
                                    onSelectPlacement = { stateHolder.update { current -> current.copy(selectedPlacementKey = it) } },
                                    onDuplicatePlacement = { placement -> duplicatePlacement(stateHolder, selectedDay.key, placement) },
                                    onRemovePlacement = { placement ->
                                        val index = selectedDay.placements.indexOfFirst { it.key == placement.key }
                                        deletedPlacementUndo = DeletedPlacementUndo(
                                            selectedDay.key,
                                            index,
                                            placement,
                                            selectedDay.groupMemberKeys(placement),
                                        )
                                        updateDay(selectedDay.key) { it.removePlacement(placement.key) }
                                        stateHolder.update { it.copy(selectedPlacementKey = null) }
                                    },
                                    onAddExercises = {
                                        pickerSelection = emptyList()
                                        pendingAssistanceRole = null
                                        page = RoutineBuilderPage.ExercisePicker
                                    },
                                    onAddAssistance = { role ->
                                        pickerSelection = emptyList()
                                        pendingAssistanceRole = role
                                        page = RoutineBuilderPage.ExercisePicker
                                    },
                                    onAddFromWorkout = { page = RoutineBuilderPage.WorkoutPicker },
                                    onDeleteDay = { day ->
                                        val index = builder.days.indexOfFirst { it.key == day.key }
                                        deletedDayUndo = index to day
                                        stateHolder.update { current ->
                                            val remaining = current.days.filterNot { it.key == day.key }
                                            current.copy(days = remaining, selectedDayKey = remaining.getOrNull(index.coerceAtMost(remaining.lastIndex))?.key, selectedPlacementKey = null)
                                        }
                                    },
                                    onCreateFiveThreeOneProgram = { showFiveThreeOneProgramSetup = true },
                                    onEditProgramStructure = { page = RoutineBuilderPage.ProgramStructure },
                                )
                                VerticalDivider(Modifier.fillMaxHeight().width(1.dp))
                                if (equipmentPickerPlacementKey == selectedPlacement.key) {
                                    EquipmentPickerPane(
                                        modifier = Modifier.weight(0.56f),
                                        placement = requireNotNull(selectedPlacement),
                                        gymState = gymState,
                                        onChoose = { machine ->
                                            updatePlacement(selectedPlacement.key) { it.withMachine(machine) }
                                            equipmentPickerPlacementKey = null
                                        },
                                        onNoMachine = {
                                            updatePlacement(selectedPlacement.key) { it.withoutMachine(freeWeightUnitId(it)) }
                                            equipmentPickerPlacementKey = null
                                        },
                                        onQuickCreate = { quickMachinePlacementKey = selectedPlacement.key },
                                        onAdvancedCreate = { machineEditorPlacementKey = selectedPlacement.key },
                                    )
                                } else {
                                    RoutinePlacementEditor(
                                        modifier = Modifier.weight(0.56f),
                                        placement = requireNotNull(selectedPlacement),
                                        selectedDay = selectedDay,
                                        allDays = builder.days,
                                        gymState = gymState,
                                        showAdvanced = showAdvancedSetFields,
                                        onShowAdvanced = { showAdvancedSetFields = it },
                                        onUpdate = { transform -> updatePlacement(selectedPlacement.key, transform) },
                                        programKind = builder.programKind,
                                        programPhaseCount = builder.programPhaseCount,
                                        programPhaseLabels = builder.programPhaseLabels,
                                        onApplyFiveThreeOne = { result -> applyFiveThreeOneProgram(selectedPlacement.key, result) },
                                        onCreateFiveThreeOneProgram = { showFiveThreeOneProgramSetup = true },
                                        onUpdateDay = { transform -> updateDay(selectedDay.key, transform) },
                                        onChooseEquipment = { equipmentPickerPlacementKey = selectedPlacement.key },
                                        onMoveToDay = { target, copy -> moveOrCopyPlacement(stateHolder, selectedDay.key, target, selectedPlacement, copy) },
                                        dialogModifier = dialogModifier,
                                        onSavePrescriptionScheme = onSavePrescriptionScheme,
                                        onReorderPrescriptionSchemes = onReorderPrescriptionSchemes,
                                        onDeletePrescriptionScheme = onDeletePrescriptionScheme,
                                    )
                                }
                            }
                        } else if (selectedPlacement != null) {
                            if (equipmentPickerPlacementKey == selectedPlacement.key) {
                                EquipmentPickerPane(
                                    modifier = Modifier.fillMaxSize(),
                                    placement = selectedPlacement,
                                    gymState = gymState,
                                    onChoose = { machine -> updatePlacement(selectedPlacement.key) { it.withMachine(machine) }; equipmentPickerPlacementKey = null },
                                    onNoMachine = {
                                        updatePlacement(selectedPlacement.key) { it.withoutMachine(freeWeightUnitId(it)) }
                                        equipmentPickerPlacementKey = null
                                    },
                                    onQuickCreate = { quickMachinePlacementKey = selectedPlacement.key },
                                    onAdvancedCreate = { machineEditorPlacementKey = selectedPlacement.key },
                                )
                            } else {
                                RoutinePlacementEditor(
                                    modifier = Modifier.fillMaxSize(),
                                    placement = selectedPlacement,
                                    selectedDay = selectedDay,
                                    allDays = builder.days,
                                    gymState = gymState,
                                    showAdvanced = showAdvancedSetFields,
                                    onShowAdvanced = { showAdvancedSetFields = it },
                                    onUpdate = { transform -> updatePlacement(selectedPlacement.key, transform) },
                                    programKind = builder.programKind,
                                    programPhaseCount = builder.programPhaseCount,
                                    programPhaseLabels = builder.programPhaseLabels,
                                    onApplyFiveThreeOne = { result -> applyFiveThreeOneProgram(selectedPlacement.key, result) },
                                    onCreateFiveThreeOneProgram = { showFiveThreeOneProgramSetup = true },
                                    onUpdateDay = { transform -> updateDay(selectedDay.key, transform) },
                                    onChooseEquipment = { equipmentPickerPlacementKey = selectedPlacement.key },
                                    onMoveToDay = { target, copy -> moveOrCopyPlacement(stateHolder, selectedDay.key, target, selectedPlacement, copy) },
                                    dialogModifier = dialogModifier,
                                    onSavePrescriptionScheme = onSavePrescriptionScheme,
                                    onReorderPrescriptionSchemes = onReorderPrescriptionSchemes,
                                    onDeletePrescriptionScheme = onDeletePrescriptionScheme,
                                )
                            }
                        } else {
                            RoutineOutlinePane(
                                modifier = Modifier.fillMaxSize(),
                                builder = builder,
                                selectedDay = selectedDay,
                                selectedPlacementKey = null,
                                gymState = gymState,
                                validationErrors = validationErrors,
                                onBuilderChange = stateHolder::update,
                                onSelectPlacement = { stateHolder.update { current -> current.copy(selectedPlacementKey = it) } },
                                onDuplicatePlacement = { duplicatePlacement(stateHolder, selectedDay.key, it) },
                                onRemovePlacement = { placement ->
                                    val index = selectedDay.placements.indexOfFirst { it.key == placement.key }
                                    deletedPlacementUndo = DeletedPlacementUndo(
                                        selectedDay.key,
                                        index,
                                        placement,
                                        selectedDay.groupMemberKeys(placement),
                                    )
                                    updateDay(selectedDay.key) { it.removePlacement(placement.key) }
                                },
                                onAddExercises = {
                                    pickerSelection = emptyList()
                                    pendingAssistanceRole = null
                                    page = RoutineBuilderPage.ExercisePicker
                                },
                                onAddAssistance = { role ->
                                    pickerSelection = emptyList()
                                    pendingAssistanceRole = role
                                    page = RoutineBuilderPage.ExercisePicker
                                },
                                onAddFromWorkout = { page = RoutineBuilderPage.WorkoutPicker },
                                onDeleteDay = { day ->
                                    val index = builder.days.indexOfFirst { it.key == day.key }
                                    deletedDayUndo = index to day
                                    stateHolder.update { current ->
                                        val remaining = current.days.filterNot { it.key == day.key }
                                        current.copy(days = remaining, selectedDayKey = remaining.getOrNull(index.coerceAtMost(remaining.lastIndex))?.key)
                                    }
                                },
                                onCreateFiveThreeOneProgram = { showFiveThreeOneProgramSetup = true },
                                onEditProgramStructure = { page = RoutineBuilderPage.ProgramStructure },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateExercise) {
        ExerciseEditorDialog(
            modifier = dialogModifier,
            exercise = null,
            initialName = exerciseNameSeed,
            categories = gymState.categories,
            selectedCategoryIds = emptySet(),
            defaultWeightUnit = gymState.appSettings.gymWeightUnitId,
            defaultRestSeconds = gymState.appSettings.defaultRestSeconds,
            defaultFormula = runCatching { EstimatedOneRepMaxFormula.valueOf(gymState.appSettings.oneRepMaxFormula) }
                .getOrDefault(EstimatedOneRepMaxFormula.Epley),
            platePresets = gymState.appSettings.platePresets,
            powerMode = gymState.appSettings.powerMode,
            onDismiss = { showCreateExercise = false },
            onSave = { draft ->
                val targetDay = selectedDay?.key
                showCreateExercise = false
                librarySaveInFlight = true
                onCreateExercise(draft) { id ->
                    librarySaveInFlight = false
                    if (id != null && targetDay != null) {
                        addCreatedExercise(targetDay, id, draft.name.trim(), pendingAssistanceRole)
                        pendingAssistanceRole = null
                        page = RoutineBuilderPage.Outline
                    }
                }
            },
        )
    }

    machineEditorPlacementKey?.let { placementKey ->
        val placement = builder.days.flatMap { it.placements }.firstOrNull { it.key == placementKey }
        if (placement != null) {
            MachineEditorDialog(
                modifier = dialogModifier,
                machine = null,
                exercises = gymState.exercises,
                definitionLocked = false,
                initialExerciseId = placement.exerciseId,
                onDismiss = { machineEditorPlacementKey = null },
                onSave = { draft ->
                    machineEditorPlacementKey = null
                    librarySaveInFlight = true
                    onCreateMachine(draft) { id ->
                        librarySaveInFlight = false
                        if (id != null) {
                            updatePlacement(placementKey) { it.withMachine(id, draft) }
                            stateHolder.noteIndependentLibrarySave()
                            equipmentPickerPlacementKey = null
                        }
                    }
                },
            )
        }
    }

    quickMachinePlacementKey?.let { placementKey ->
        val placement = builder.days.flatMap { it.placements }.firstOrNull { it.key == placementKey }
        val exercise = placement?.let { selected ->
            (gymState.exercises + gymState.archivedExercises).firstOrNull { it.id == selected.exerciseId }
        }
        if (placement != null && exercise != null) {
            QuickMachineDialog(
                modifier = dialogModifier,
                exercise = exercise,
                onDismiss = { quickMachinePlacementKey = null },
                onSave = { draft ->
                    quickMachinePlacementKey = null
                    librarySaveInFlight = true
                    onCreateMachine(draft) { id ->
                        librarySaveInFlight = false
                        if (id != null) {
                            updatePlacement(placementKey) { it.withMachine(id, draft) }
                            stateHolder.noteIndependentLibrarySave()
                            equipmentPickerPlacementKey = null
                        }
                    }
                },
            )
        }
    }

    if (showDiscardConfirmation) {
        UnsavedChangesDialog(
            subject = "routine",
            onKeepEditing = { showDiscardConfirmation = false },
            onDiscard = {
                showDiscardConfirmation = false
                stateHolder.clear()
                onDismiss()
            },
            modifier = dialogModifier,
        )
    }
    if (showFiveThreeOneProgramSetup) {
        FiveThreeOneProgramSetupDialog(
            exercises = gymState.exercises,
            personalRecords = gymState.personalRecords,
            replacingExistingRoutine = builder.days.any { it.placements.isNotEmpty() },
            onDismiss = { showFiveThreeOneProgramSetup = false },
            standardLiftCreationInFlight = standardLiftCreationInFlight,
            standardLiftCreationError = standardLiftCreationError,
            onCreateMissingStandardLifts = ::createMissingStandardLifts,
            onApply = { request ->
                stateHolder.update { current ->
                    buildFiveThreeOneProgramState(current, request)
                }
                showFiveThreeOneProgramSetup = false
            },
        )
    }
}

@Composable
private fun RoutineBuilderHeader(
    editing: Boolean,
    page: RoutineBuilderPage,
    canSave: Boolean,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    val title = when (page) {
        RoutineBuilderPage.Outline -> if (editing) "Edit routine" else "New routine"
        RoutineBuilderPage.ProgramStructure -> "Program structure"
        RoutineBuilderPage.ExercisePicker -> "Add exercises"
        RoutineBuilderPage.WorkoutPicker -> "Add from workout"
    }.uiTitleCase()
    WhipEditorHeader(
        navigationAction = {
            if (page == RoutineBuilderPage.Outline) {
                WhipTrailingCloseAction(
                    label = if (editing) "Close routine editor" else "Cancel routine creation",
                    onClick = onCancel,
                )
            } else {
                WhipBackAction(label = "Back to routine outline", onClick = onBack)
            }
        },
        title = {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        hasActions = page == RoutineBuilderPage.Outline,
        actions = {
            if (page == RoutineBuilderPage.Outline) {
                WhipButton(
                    enabled = canSave,
                    onClick = onSave,
                    modifier = Modifier.testTag("routine-builder-save"),
                ) { Text("Save") }
            }
        },
    )
}

@Composable
private fun UndoRow(message: String, onUndo: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.inverseSurface, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, color = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.weight(1f))
            WhipTextButton(onClick = onUndo) { Text("Undo") }
        }
    }
}

@Composable
private fun FiveThreeOneProgramSetupDialog(
    exercises: List<Exercise>,
    personalRecords: List<com.whip.app.domain.PersonalRecord>,
    replacingExistingRoutine: Boolean,
    onDismiss: () -> Unit,
    standardLiftCreationInFlight: Boolean,
    standardLiftCreationError: String?,
    onCreateMissingStandardLifts: () -> Unit,
    onApply: (FiveThreeOneProgramRequest) -> Unit,
) {
    val eligible = exercises.filter { it.trackingType == ExerciseTrackingType.WeightReps }
    val roles = FiveThreeOneLiftRole.entries
    fun suggested(role: FiveThreeOneLiftRole, unused: Set<Long>): Exercise? {
        return eligible.firstOrNull { it.id !in unused && role.matchesExerciseName(it.name) }
            ?: eligible.firstOrNull { it.id !in unused }
    }
    val initialIds = rememberSaveable(eligible.map(Exercise::id)) {
        val selected = mutableListOf<Long>()
        roles.forEach { role -> suggested(role, selected.toSet())?.id?.let(selected::add) }
        selected + List((roles.size - selected.size).coerceAtLeast(0)) { 0L }
    }
    var exerciseIds by rememberSaveable { mutableStateOf(initialIds) }
    var manuallySelectedRoleIndices by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var trainingMaxes by rememberSaveable { mutableStateOf(List(roles.size) { "" }) }
    var useRecentMaxSuggestion by rememberSaveable { mutableStateOf(List(roles.size) { false }) }
    var recentMaxes by rememberSaveable { mutableStateOf(List(roles.size) { "" }) }
    var trainingMaxPercentages by rememberSaveable { mutableStateOf(List(roles.size) { "85" }) }
    var trainingMaxBasisKinds by rememberSaveable {
        mutableStateOf(List(roles.size) { TrainingMaxBasisKind.ActualOneRepMax.name })
    }
    var appliedSourceMaxes by rememberSaveable { mutableStateOf(List(roles.size) { "" }) }
    var appliedTrainingMaxPercentages by rememberSaveable { mutableStateOf(List(roles.size) { "" }) }
    var appliedTrainingMaxBasisKinds by rememberSaveable { mutableStateOf(List(roles.size) { "" }) }
    var appliedDerivedTrainingMaxes by rememberSaveable { mutableStateOf(List(roles.size) { "" }) }
    var increments by rememberSaveable {
        mutableStateOf(roles.indices.map { index ->
            val exercise = eligible.firstOrNull { it.id == initialIds.getOrNull(index) }
            editableNumericValue(
                defaultFiveThreeOneCycleIncrease(
                    unitId = exercise?.weightUnitId ?: "kilogram",
                    exerciseName = exercise?.name.orEmpty(),
                    role = roles[index],
                ),
            )
        })
    }
    var layoutName by rememberSaveable { mutableStateOf(FiveThreeOneProgramLayout.FourDay.name) }
    var planName by rememberSaveable { mutableStateOf(FiveThreeOneProgramPlan.SingleCycle.name) }
    var closingProtocolName by rememberSaveable { mutableStateOf(FiveThreeOneSeventhWeekProtocol.Deload.name) }
    var mainSchemeName by rememberSaveable { mutableStateOf(FiveThreeOneMainScheme.Classic.name) }
    var supplementName by rememberSaveable { mutableStateOf(FiveThreeOneSupplement.FirstSetLast.name) }
    var classicFinalSetAmrap by rememberSaveable { mutableStateOf(true) }
    var boringButBigPercentText by rememberSaveable { mutableStateOf("50") }
    var previewPhaseName by rememberSaveable { mutableStateOf(FiveThreeOnePhase.Fives.name) }
    var jokerCount by rememberSaveable { mutableStateOf(0) }
    var jokerStepPercent by rememberSaveable { mutableStateOf(5) }
    var bbbTargetIds by rememberSaveable { mutableStateOf(initialIds) }
    var automaticAssistanceEnabled by rememberSaveable { mutableStateOf(true) }
    var assistanceExerciseIds by rememberSaveable { mutableStateOf(List(3) { 0L }) }
    var manuallyChangedAssistanceIndices by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var progressionModeName by rememberSaveable { mutableStateOf(RoutineProgressionMode.Standard.name) }
    val layout = FiveThreeOneProgramLayout.valueOf(layoutName)
    val plan = FiveThreeOneProgramPlan.valueOf(planName)
    val closingProtocol = FiveThreeOneSeventhWeekProtocol.valueOf(closingProtocolName)
    LaunchedEffect(plan) {
        if (plan != FiveThreeOneProgramPlan.SingleCycle && layoutName == FiveThreeOneProgramLayout.Beginners.name) {
            layoutName = FiveThreeOneProgramLayout.FourDay.name
        }
        if (plan != FiveThreeOneProgramPlan.SingleCycle && closingProtocolName == FiveThreeOneSeventhWeekProtocol.Deload.name) {
            closingProtocolName = FiveThreeOneSeventhWeekProtocol.TrainingMaxTest.name
        }
    }
    LaunchedEffect(eligible.map(Exercise::id), layout) {
        val previousIds = exerciseIds
        val eligibleIds = eligible.map(Exercise::id).toSet()
        val reconciledIds = if (layout == FiveThreeOneProgramLayout.Custom) {
            val previousSet = previousIds.filter { it in eligibleIds }.toSet()
            eligible.filter { it.id in previousSet }.map(Exercise::id).ifEmpty {
                eligible.firstOrNull()?.let { listOf(it.id) }.orEmpty()
            }
        } else {
            fillEmptyFiveThreeOneLiftSelections(
                currentIds = previousIds,
                candidates = eligible.map { it.id to it.name },
                manuallySelectedRoleIndices = manuallySelectedRoleIndices.toSet(),
            )
        }
        val count = reconciledIds.size
        trainingMaxes = List(count) { index -> trainingMaxes.getOrNull(index).orEmpty() }
        useRecentMaxSuggestion = List(count) { index -> useRecentMaxSuggestion.getOrNull(index) ?: false }
        recentMaxes = List(count) { index -> recentMaxes.getOrNull(index).orEmpty() }
        trainingMaxPercentages = List(count) { index -> trainingMaxPercentages.getOrNull(index) ?: "85" }
        trainingMaxBasisKinds = List(count) { index ->
            trainingMaxBasisKinds.getOrNull(index) ?: TrainingMaxBasisKind.ActualOneRepMax.name
        }
        appliedSourceMaxes = List(count) { index -> appliedSourceMaxes.getOrNull(index).orEmpty() }
        appliedTrainingMaxPercentages = List(count) { index -> appliedTrainingMaxPercentages.getOrNull(index).orEmpty() }
        appliedTrainingMaxBasisKinds = List(count) { index -> appliedTrainingMaxBasisKinds.getOrNull(index).orEmpty() }
        appliedDerivedTrainingMaxes = List(count) { index -> appliedDerivedTrainingMaxes.getOrNull(index).orEmpty() }
        increments = List(count) { index ->
            val existing = increments.getOrNull(index).orEmpty()
            if (reconciledIds.getOrNull(index).orZero() <= 0L || reconciledIds.getOrNull(index) == previousIds.getOrNull(index)) {
                existing
            } else {
                val exercise = eligible.firstOrNull { it.id == reconciledIds[index] }
                editableNumericValue(
                    defaultFiveThreeOneCycleIncrease(
                        unitId = exercise?.weightUnitId ?: "kilogram",
                        exerciseName = exercise?.name.orEmpty(),
                        role = roles.getOrNull(index).takeIf { layout != FiveThreeOneProgramLayout.Custom },
                    ),
                )
            }
        }
        bbbTargetIds = List(count) { index ->
            bbbTargetIds.getOrNull(index)?.takeIf { it in reconciledIds } ?: reconciledIds.getOrElse(index) { 0L }
        }
        exerciseIds = reconciledIds
    }
    LaunchedEffect(exerciseIds, personalRecords) {
        exerciseIds.forEachIndexed { index, exerciseId ->
            if (recentMaxes.getOrNull(index).orEmpty().isNotBlank()) return@forEachIndexed
            val exercise = eligible.firstOrNull { it.id == exerciseId } ?: return@forEachIndexed
            val actual = personalRecords.firstOrNull { record ->
                record.exerciseId == exerciseId && record.current &&
                    record.type == PersonalRecordType.BestWeightForRepCount && record.secondaryValue == 1.0 &&
                    record.machineProfileUuidSnapshot == null
            }
            val estimated = personalRecords.firstOrNull { record ->
                record.exerciseId == exerciseId && record.current &&
                    record.type == PersonalRecordType.EstimatedOneRepMax &&
                    record.machineProfileUuidSnapshot == null
            }
            val source = actual ?: estimated ?: return@forEachIndexed
            recentMaxes = recentMaxes.toMutableList().also {
                it[index] = editableNumericValue(massFromKilograms(source.value, exercise.weightUnitId))
            }
            trainingMaxBasisKinds = trainingMaxBasisKinds.toMutableList().also {
                it[index] = if (actual != null) {
                    TrainingMaxBasisKind.ActualOneRepMax.name
                } else {
                    TrainingMaxBasisKind.EstimatedOneRepMax.name
                }
            }
        }
    }
    val mainScheme = when (plan) {
        FiveThreeOneProgramPlan.SingleCycle -> FiveThreeOneMainScheme.valueOf(mainSchemeName)
        FiveThreeOneProgramPlan.ForeverBbbLeaderAnchor,
        FiveThreeOneProgramPlan.ForeverFslLeaderAnchor,
        -> FiveThreeOneMainScheme.FivesPro
    }
    val supplement = when (plan) {
        FiveThreeOneProgramPlan.ForeverBbbLeaderAnchor -> FiveThreeOneSupplement.BoringButBig
        FiveThreeOneProgramPlan.ForeverFslLeaderAnchor -> FiveThreeOneSupplement.FirstSetLast
        FiveThreeOneProgramPlan.SingleCycle -> if (layout == FiveThreeOneProgramLayout.Beginners) {
            FiveThreeOneSupplement.FirstSetLast
        } else {
            FiveThreeOneSupplement.valueOf(supplementName)
        }
    }
    val boringButBigPercent = boringButBigPercentText.toWhipDoubleOrNull()
    val previewPhase = FiveThreeOnePhase.valueOf(previewPhaseName)
    val selectedExercises = exerciseIds.map { id -> eligible.firstOrNull { it.id == id } }
    val activeRoles = exerciseIds.indices.map { index ->
        roles.getOrNull(index).takeIf { layout != FiveThreeOneProgramLayout.Custom }
    }
    val lifts = exerciseIds.indices.mapNotNull { index ->
        val exercise = selectedExercises.getOrNull(index) ?: return@mapNotNull null
        val tm = trainingMaxes.getOrNull(index)?.toWhipDoubleOrNull() ?: return@mapNotNull null
        val increase = increments.getOrNull(index)?.toWhipDoubleOrNull() ?: return@mapNotNull null
        FiveThreeOneProgramLift(
            activeRoles[index],
            exercise.id,
            exercise.name,
            tm,
            exercise.weightUnitId,
            exercise.weightIncrement.takeIf { it > 0.0 } ?: if (exercise.weightUnitId == "pound") 5.0 else 2.5,
            increase,
            trainingMaxPercent = if (useRecentMaxSuggestion.getOrNull(index) == true) {
                appliedTrainingMaxPercentages.getOrNull(index)?.toWhipDoubleOrNull() ?: 85.0
            } else {
                trainingMaxPercentages.getOrNull(index)?.toWhipDoubleOrNull() ?: 85.0
            },
            trainingMaxBasisKind = if (useRecentMaxSuggestion.getOrNull(index) == true) {
                runCatching { TrainingMaxBasisKind.valueOf(appliedTrainingMaxBasisKinds[index]) }
                    .getOrDefault(TrainingMaxBasisKind.ManualSourceMax)
            } else {
                TrainingMaxBasisKind.ExplicitTrainingMax
            },
            trainingMaxBasisValue = appliedSourceMaxes.getOrNull(index)?.toWhipDoubleOrNull()
                .takeIf { useRecentMaxSuggestion.getOrNull(index) == true },
            trainingMaxBasisUnitId = exercise.weightUnitId.takeIf {
                useRecentMaxSuggestion.getOrNull(index) == true
            }.orEmpty(),
        )
    }
    val assistanceCategories = listOf(
        RoutineAssistanceCategory.Push,
        RoutineAssistanceCategory.Pull,
        RoutineAssistanceCategory.SingleLegCore,
    )
    val assistanceSuggestions = suggestFiveThreeOneAssistance(
        exercises = exercises,
        excludedExerciseIds = exerciseIds.toSet(),
    )
    val compatibleAssistanceExercises = exercises.filter {
        it.isFiveThreeOneAssistanceCompatible(exerciseIds.toSet())
    }
    LaunchedEffect(
        automaticAssistanceEnabled,
        exerciseIds,
        assistanceSuggestions.mapValues { (_, candidates) -> candidates.map(Exercise::id) },
    ) {
        if (!automaticAssistanceEnabled) return@LaunchedEffect
        val used = mutableSetOf<Long>()
        assistanceExerciseIds = assistanceCategories.mapIndexed { index, category ->
            val candidates = assistanceSuggestions[category].orEmpty().filter { it.id !in used }
            val selected = assistanceExerciseIds.getOrNull(index) ?: 0L
            val resolved = when {
                index in manuallyChangedAssistanceIndices && selected == 0L -> 0L
                selected in candidates.map(Exercise::id) -> selected
                else -> candidates.firstOrNull()?.id ?: 0L
            }
            if (resolved > 0L) used += resolved
            resolved
        }
    }
    val assistanceChoices = if (automaticAssistanceEnabled) {
        assistanceCategories.mapIndexedNotNull { index, category ->
            exercises.firstOrNull { it.id == assistanceExerciseIds.getOrNull(index) }?.let { exercise ->
                FiveThreeOneAssistanceChoice(category, exercise.id, exercise.name)
            }
        }
    } else {
        emptyList()
    }
    val requiredLiftCount = if (layout == FiveThreeOneProgramLayout.Custom) exerciseIds.size else roles.size
    val bbbLiftByMainExerciseId = if (
        supplement == FiveThreeOneSupplement.BoringButBig && layout != FiveThreeOneProgramLayout.Beginners
    ) {
        exerciseIds.mapIndexedNotNull { index, mainId ->
            bbbTargetIds.getOrNull(index)?.takeIf { it in exerciseIds }?.let { targetId -> mainId to targetId }
        }.toMap()
    } else {
        emptyMap()
    }
    val bbbMappingsValid = supplement != FiveThreeOneSupplement.BoringButBig ||
        layout == FiveThreeOneProgramLayout.Beginners || bbbLiftByMainExerciseId.size == exerciseIds.size
    val standardSelectionsConfirmed = layout == FiveThreeOneProgramLayout.Custom ||
        exerciseIds.indices.all { index ->
            val selected = selectedExercises.getOrNull(index)
            val role = activeRoles.getOrNull(index)
            selected != null && role != null &&
                (role.matchesExerciseName(selected.name) || index in manuallySelectedRoleIndices)
        }
    val everyDerivedTrainingMaxIsApplied = exerciseIds.indices.all { index ->
        useRecentMaxSuggestion.getOrNull(index) != true ||
            (
                appliedTrainingMaxBasisKinds.getOrNull(index) == trainingMaxBasisKinds.getOrNull(index) &&
                    appliedSourceMaxes.getOrNull(index).orEmpty().numericallyEquals(recentMaxes.getOrNull(index).orEmpty()) &&
                    appliedTrainingMaxPercentages.getOrNull(index).orEmpty()
                        .numericallyEquals(trainingMaxPercentages.getOrNull(index).orEmpty()) &&
                    appliedDerivedTrainingMaxes.getOrNull(index).orEmpty()
                        .numericallyEquals(trainingMaxes.getOrNull(index).orEmpty()) &&
                    appliedSourceMaxes.getOrNull(index).orEmpty().isNotBlank()
                )
    }
    val valid = requiredLiftCount > 0 && lifts.size == requiredLiftCount &&
        lifts.map(FiveThreeOneProgramLift::exerciseId).distinct().size == requiredLiftCount &&
        standardSelectionsConfirmed && everyDerivedTrainingMaxIsApplied &&
        bbbMappingsValid &&
        lifts.all { it.trainingMax > 0.0 && it.cycleIncrement > 0.0 } &&
        (supplement != FiveThreeOneSupplement.BoringButBig ||
            boringButBigPercent?.let { it.isFinite() && it in 1.0..100.0 } == true)
    val buildBlocker = when {
        !everyDerivedTrainingMaxIsApplied -> "Apply every calculated Training Max after changing its source max or percentage."
        requiredLiftCount <= 0 || lifts.size != requiredLiftCount -> "Enter a Training Max and cycle increase above zero for every selected lift."
        lifts.map(FiveThreeOneProgramLift::exerciseId).distinct().size != requiredLiftCount -> "Choose each main lift only once."
        !standardSelectionsConfirmed -> "Confirm or replace every prefilled standard lift."
        !bbbMappingsValid -> "Choose the BBB lift used after every Main lift."
        lifts.any { it.trainingMax <= 0.0 || it.cycleIncrement <= 0.0 } -> "Enter a Training Max and cycle increase above zero for every selected lift."
        supplement == FiveThreeOneSupplement.BoringButBig &&
            boringButBigPercent?.let { it.isFinite() && it in 1.0..100.0 } != true ->
            "Enter a Boring But Big percentage from 1 to 100%."
        else -> null
    }
    fun <T> List<T>.moved(fromIndex: Int, toIndex: Int): List<T> {
        if (fromIndex !in indices || toIndex !in indices || fromIndex == toIndex) return this
        return toMutableList().also { values ->
            val moved = values.removeAt(fromIndex)
            values.add(toIndex, moved)
        }
    }
    fun moveCustomLift(fromIndex: Int, toIndex: Int) {
        exerciseIds = exerciseIds.moved(fromIndex, toIndex)
        trainingMaxes = trainingMaxes.moved(fromIndex, toIndex)
        useRecentMaxSuggestion = useRecentMaxSuggestion.moved(fromIndex, toIndex)
        recentMaxes = recentMaxes.moved(fromIndex, toIndex)
        trainingMaxPercentages = trainingMaxPercentages.moved(fromIndex, toIndex)
        trainingMaxBasisKinds = trainingMaxBasisKinds.moved(fromIndex, toIndex)
        appliedSourceMaxes = appliedSourceMaxes.moved(fromIndex, toIndex)
        appliedTrainingMaxPercentages = appliedTrainingMaxPercentages.moved(fromIndex, toIndex)
        appliedTrainingMaxBasisKinds = appliedTrainingMaxBasisKinds.moved(fromIndex, toIndex)
        appliedDerivedTrainingMaxes = appliedDerivedTrainingMaxes.moved(fromIndex, toIndex)
        increments = increments.moved(fromIndex, toIndex)
        bbbTargetIds = bbbTargetIds.moved(fromIndex, toIndex)
    }

    PaneAwareAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Up 5/3/1") },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Choose a program structure, then review its lifts, Training Maxes, optional work, assistance, and exact phase timeline before building.")
                if (replacingExistingRoutine) {
                    Text(
                        "Building replaces this draft's current days with the complete program shown below. Nothing is persisted until you use the routine Save action.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("five-three-one-program-replacement-warning"),
                    )
                }
                Text("1 · Program preset", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                FiveThreeOneProgramPlan.entries.forEach { choice ->
                    OutlinedCard(
                        onClick = { planName = choice.name },
                        modifier = Modifier.fillMaxWidth().testTag("five-three-one-plan-${choice.name}"),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (plan == choice) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(choice.label, fontWeight = FontWeight.SemiBold)
                            Text(choice.supportingText, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (plan != FiveThreeOneProgramPlan.SingleCycle) {
                    Text(
                        "Book-guided editable structure. Whip shows every generated percentage; verify it against the edition and exact template you follow.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("five-three-one-book-guided-note"),
                    )
                }
                Text("2 · Schedule and lifts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FiveThreeOneProgramLayout.entries.filter { choice ->
                        plan == FiveThreeOneProgramPlan.SingleCycle || choice != FiveThreeOneProgramLayout.Beginners
                    }.forEach { choice ->
                        WhipFilterChip(layout == choice, { layoutName = choice.name }, { Text(choice.label) })
                    }
                }
                Text("Training Max progression", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WhipFilterChip(
                        selected = progressionModeName == RoutineProgressionMode.Standard.name,
                        onClick = { progressionModeName = RoutineProgressionMode.Standard.name },
                        label = { Text("5/3/1 standard · recommended") },
                    )
                    WhipFilterChip(
                        selected = progressionModeName == RoutineProgressionMode.PerformanceInformed.name,
                        onClick = { progressionModeName = RoutineProgressionMode.PerformanceInformed.name },
                        label = { Text("Review each cycle") },
                    )
                }
                Text(
                    if (progressionModeName == RoutineProgressionMode.Standard.name) {
                        "Automatically apply each lift's saved standard increase after completed Main work. Every boundary is recorded in Training Max history."
                    } else {
                        "At each boundary, Whip shows per-lift evidence and waits for Standard, suggestion, custom, decrease, or Hold. Log RPE or RIR for effort-sensitive suggestions."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("five-three-one-progression-explanation"),
                )
                if (layout == FiveThreeOneProgramLayout.Beginners) {
                    Text(
                        "Mon Squat + Bench · Wed Deadlift + Press · Fri Bench + Squat. FSL 5 × 5 is included. Choose one Push, Pull, and Single-leg/Core movement each day; target 50–100 total reps in each category.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (layout == FiveThreeOneProgramLayout.Custom) {
                    Text(
                        "Choose one or more of your Weight + Reps exercises. Each selected lift becomes its own training day in this order; the lift does not need to be one of the four standard 5/3/1 lifts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (layout == FiveThreeOneProgramLayout.Custom && eligible.isEmpty()) {
                    Text("Create at least one Weight + Reps exercise in the Gym library first.", color = MaterialTheme.colorScheme.error)
                } else if (layout != FiveThreeOneProgramLayout.Custom && eligible.size < roles.size) {
                    Text("Create at least four weight-and-reps exercises before setting up this program.", color = MaterialTheme.colorScheme.error)
                }
                val missingNamedRoles = roles.filter { role -> eligible.none { role.matchesExerciseName(it.name) } }
                if (layout != FiveThreeOneProgramLayout.Custom && missingNamedRoles.isNotEmpty()) {
                    WhipOutlinedButton(
                        enabled = !standardLiftCreationInFlight,
                        onClick = onCreateMissingStandardLifts,
                        modifier = Modifier.fillMaxWidth().testTag("five-three-one-create-standard-lifts"),
                    ) {
                        Text(if (standardLiftCreationInFlight) "Creating standard lifts…" else "Create missing standard Squat, Bench Press, Deadlift, and Overhead Press exercises")
                    }
                    standardLiftCreationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
                if (eligible.isNotEmpty()) {
                    exerciseIds.indices.forEach { index ->
                        val role = activeRoles[index]
                        val selected = selectedExercises[index] ?: eligible.first()
                        val fieldKey = role?.name ?: "Custom-$index"
                        val heading = role?.label ?: "Lift ${index + 1}"
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                heading,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f).semantics { heading() },
                            )
                            if (layout == FiveThreeOneProgramLayout.Custom && exerciseIds.size > 1) {
                                IconButton(
                                    onClick = {
                                        exerciseIds = exerciseIds.filterIndexed { itemIndex, _ -> itemIndex != index }
                                        trainingMaxes = trainingMaxes.filterIndexed { itemIndex, _ -> itemIndex != index }
                                        useRecentMaxSuggestion = useRecentMaxSuggestion.filterIndexed { itemIndex, _ -> itemIndex != index }
                                        recentMaxes = recentMaxes.filterIndexed { itemIndex, _ -> itemIndex != index }
                                        trainingMaxPercentages = trainingMaxPercentages.filterIndexed { itemIndex, _ -> itemIndex != index }
                                        trainingMaxBasisKinds = trainingMaxBasisKinds.filterIndexed { itemIndex, _ -> itemIndex != index }
                                        appliedSourceMaxes = appliedSourceMaxes.filterIndexed { itemIndex, _ -> itemIndex != index }
                                        appliedTrainingMaxPercentages = appliedTrainingMaxPercentages.filterIndexed { itemIndex, _ -> itemIndex != index }
                                        appliedTrainingMaxBasisKinds = appliedTrainingMaxBasisKinds.filterIndexed { itemIndex, _ -> itemIndex != index }
                                        appliedDerivedTrainingMaxes = appliedDerivedTrainingMaxes.filterIndexed { itemIndex, _ -> itemIndex != index }
                                        increments = increments.filterIndexed { itemIndex, _ -> itemIndex != index }
                                        val remainingExerciseIds = exerciseIds
                                        bbbTargetIds = bbbTargetIds.filterIndexed { itemIndex, _ -> itemIndex != index }
                                            .mapIndexed { remainingIndex, targetId ->
                                                targetId.takeIf { it in remainingExerciseIds }
                                                    ?: remainingExerciseIds.getOrElse(remainingIndex) { remainingExerciseIds.first() }
                                            }
                                        manuallySelectedRoleIndices = emptyList()
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("five-three-one-remove-$fieldKey")
                                        .semantics { contentDescription = "Remove ${selected.name} from 5/3/1 program" },
                                ) { Icon(Icons.Outlined.Delete, contentDescription = null) }
                            }
                        }
                        if (layout == FiveThreeOneProgramLayout.Custom && exerciseIds.size > 1) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WhipTextButton(
                                    enabled = index > 0,
                                    onClick = { moveCustomLift(index, index - 1) },
                                    modifier = Modifier
                                        .testTag("five-three-one-move-earlier-$fieldKey")
                                        .semantics { contentDescription = "Move ${selected.name} earlier" },
                                ) { Text("Move earlier") }
                                WhipTextButton(
                                    enabled = index < exerciseIds.lastIndex,
                                    onClick = { moveCustomLift(index, index + 1) },
                                    modifier = Modifier
                                        .testTag("five-three-one-move-later-$fieldKey")
                                        .semantics { contentDescription = "Move ${selected.name} later" },
                                ) { Text("Move later") }
                            }
                        }
                        val selectedElsewhere = exerciseIds.filterIndexed { itemIndex, _ -> itemIndex != index }.toSet()
                        val availableForSlot = eligible.filter { exercise ->
                            exercise.id == selected.id || exercise.id !in selectedElsewhere
                        }
                        SelectionField(
                            label = if (role == null) "Lift ${index + 1} exercise" else "${role.label} exercise",
                            values = availableForSlot,
                            selected = selected,
                            valueText = Exercise::name,
                            onSelect = { exercise ->
                                val exerciseChanged = exercise.id != selected.id
                                exerciseIds = exerciseIds.toMutableList().also { it[index] = exercise.id }
                                if (layout != FiveThreeOneProgramLayout.Custom) {
                                    manuallySelectedRoleIndices = (manuallySelectedRoleIndices + index).distinct()
                                }
                                if (exerciseChanged) {
                                    // A Training Max and its source belong to the selected lift. Never carry
                                    // another lift's value or unit into a replacement selection.
                                    trainingMaxes = trainingMaxes.toMutableList().also { it[index] = "" }
                                    useRecentMaxSuggestion = useRecentMaxSuggestion.toMutableList().also { it[index] = false }
                                    recentMaxes = recentMaxes.toMutableList().also { it[index] = "" }
                                    trainingMaxPercentages = trainingMaxPercentages.toMutableList().also { it[index] = "85" }
                                    trainingMaxBasisKinds = trainingMaxBasisKinds.toMutableList().also {
                                        it[index] = TrainingMaxBasisKind.ActualOneRepMax.name
                                    }
                                    appliedSourceMaxes = appliedSourceMaxes.toMutableList().also { it[index] = "" }
                                    appliedTrainingMaxPercentages = appliedTrainingMaxPercentages.toMutableList().also { it[index] = "" }
                                    appliedTrainingMaxBasisKinds = appliedTrainingMaxBasisKinds.toMutableList().also { it[index] = "" }
                                    appliedDerivedTrainingMaxes = appliedDerivedTrainingMaxes.toMutableList().also { it[index] = "" }
                                }
                                increments = increments.toMutableList().also {
                                    it[index] = editableNumericValue(
                                        defaultFiveThreeOneCycleIncrease(
                                            unitId = exercise.weightUnitId,
                                            exerciseName = exercise.name,
                                            role = role,
                                        ),
                                    )
                                }
                            },
                            modifier = Modifier.testTag("five-three-one-exercise-$fieldKey"),
                        )
                        if (role != null) {
                            Text(
                                when {
                                    role.matchesExerciseName(selected.name) ->
                                        "Suggested from the exercise name. Confirm that ${selected.name} is the ${role.label} lift you intend to program."
                                    index in manuallySelectedRoleIndices ->
                                        "Confirmed by you for the ${role.label} slot; the exercise name did not assign this role."
                                    else ->
                                        "Needs confirmation: no confident ${role.label} name match was found, so the first unused Weight + Reps exercise was prefilled. Choose this field to confirm or replace it."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (!role.matchesExerciseName(selected.name) && index !in manuallySelectedRoleIndices) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            WhipFilterChip(
                                selected = !useRecentMaxSuggestion[index],
                                onClick = { useRecentMaxSuggestion = useRecentMaxSuggestion.toMutableList().also { it[index] = false } },
                                label = { Text("Enter Training Max") },
                                modifier = Modifier
                                    .testTag("five-three-one-enter-tm-${role?.name ?: index}")
                                    .semantics {
                                        contentDescription = "${role?.label ?: selected.name}: Enter Training Max"
                                    },
                            )
                            WhipFilterChip(
                                selected = useRecentMaxSuggestion[index],
                                onClick = { useRecentMaxSuggestion = useRecentMaxSuggestion.toMutableList().also { it[index] = true } },
                                label = { Text("Calculate from max / e1RM") },
                                modifier = Modifier
                                    .testTag("five-three-one-calculate-tm-${role?.name ?: index}")
                                    .semantics {
                                        contentDescription = "${role?.label ?: selected.name}: Calculate from max or estimated 1RM"
                                    },
                            )
                        }
                        if (useRecentMaxSuggestion[index]) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(
                                    TrainingMaxBasisKind.ActualOneRepMax to "Actual 1RM",
                                    TrainingMaxBasisKind.EstimatedOneRepMax to "Estimated 1RM",
                                    TrainingMaxBasisKind.ManualSourceMax to "Other source max",
                                ).forEach { (basis, label) ->
                                    WhipFilterChip(
                                        selected = trainingMaxBasisKinds[index] == basis.name,
                                        onClick = {
                                            trainingMaxBasisKinds = trainingMaxBasisKinds.toMutableList().also {
                                                it[index] = basis.name
                                            }
                                            val matchingRecord = when (basis) {
                                                TrainingMaxBasisKind.ActualOneRepMax -> personalRecords.firstOrNull { record ->
                                                    record.exerciseId == selected.id && record.current &&
                                                        record.type == PersonalRecordType.BestWeightForRepCount &&
                                                        record.secondaryValue == 1.0 && record.machineProfileUuidSnapshot == null
                                                }
                                                TrainingMaxBasisKind.EstimatedOneRepMax -> personalRecords.firstOrNull { record ->
                                                    record.exerciseId == selected.id && record.current &&
                                                        record.type == PersonalRecordType.EstimatedOneRepMax &&
                                                        record.machineProfileUuidSnapshot == null
                                                }
                                                else -> null
                                            }
                                            recentMaxes = recentMaxes.toMutableList().also {
                                                it[index] = matchingRecord?.let { record ->
                                                    editableNumericValue(massFromKilograms(record.value, selected.weightUnitId))
                                                }.orEmpty()
                                            }
                                        },
                                        label = { Text(label) },
                                    )
                                }
                            }
                            val entryState = FiveThreeOneTrainingMaxEntryState(
                                explicitTrainingMax = trainingMaxes[index],
                                recentMaxOrEstimatedOneRepMax = recentMaxes[index],
                                trainingMaxPercentage = trainingMaxPercentages[index],
                            )
                            val loadIncrement = selected.weightIncrement.takeIf { it > 0.0 }
                                ?: if (selected.weightUnitId == "pound") 5.0 else 2.5
                            val suggestion = entryState.suggestionOrNull(loadIncrement)
                            ResponsiveFieldPair(
                                first = { field ->
                                    OutlinedTextField(
                                        recentMaxes[index],
                                        { value -> recentMaxes = recentMaxes.toMutableList().also { it[index] = value.numericInput() } },
                                        label = {
                                            Text(
                                                when (trainingMaxBasisKinds[index]) {
                                                    TrainingMaxBasisKind.ActualOneRepMax.name -> "Actual 1RM (${unitSymbol(selected.weightUnitId)})"
                                                    TrainingMaxBasisKind.EstimatedOneRepMax.name -> "Estimated 1RM (${unitSymbol(selected.weightUnitId)})"
                                                    else -> "Source max (${unitSymbol(selected.weightUnitId)})"
                                                },
                                            )
                                        },
                                        modifier = field.testTag("five-three-one-recent-max-$fieldKey"),
                                        singleLine = true,
                                    )
                                },
                                second = { field ->
                                    OutlinedTextField(
                                        trainingMaxPercentages[index],
                                        { value -> trainingMaxPercentages = trainingMaxPercentages.toMutableList().also { it[index] = value.numericInput() } },
                                        label = { Text("TM percentage") },
                                        isError = trainingMaxPercentages[index].toWhipDoubleOrNull()?.let { it !in 1.0..100.0 } != false,
                                        modifier = field.testTag("five-three-one-tm-percent-$fieldKey"),
                                        singleLine = true,
                                    )
                                },
                            )
                            Text(
                                if (trainingMaxPercentages[index].toWhipDoubleOrNull()?.let { it !in 80.0..90.0 } == true) {
                                    "Outside the common 80–90% starting range. This is allowed for readiness or individual programming; review it carefully. Applying copies a stable explicit TM."
                                } else {
                                    "The source max is used once. Applying copies a rounded, stable Training Max; later source changes never mutate it."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            WhipOutlinedButton(
                                enabled = suggestion != null,
                                onClick = {
                                    val applied = entryState.applySuggestion(loadIncrement)
                                    trainingMaxes = trainingMaxes.toMutableList().also { it[index] = applied.explicitTrainingMax }
                                    appliedSourceMaxes = appliedSourceMaxes.toMutableList().also { it[index] = recentMaxes[index] }
                                    appliedTrainingMaxPercentages = appliedTrainingMaxPercentages.toMutableList().also {
                                        it[index] = trainingMaxPercentages[index]
                                    }
                                    appliedTrainingMaxBasisKinds = appliedTrainingMaxBasisKinds.toMutableList().also {
                                        it[index] = trainingMaxBasisKinds[index]
                                    }
                                    appliedDerivedTrainingMaxes = appliedDerivedTrainingMaxes.toMutableList().also {
                                        it[index] = applied.explicitTrainingMax
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("five-three-one-use-tm-suggestion-$fieldKey"),
                            ) {
                                Text(
                                    suggestion?.let { "Use ${editableNumericValue(it)} ${unitSymbol(selected.weightUnitId)} as Training Max" }
                                        ?: "Enter a source max and 1–100%",
                                )
                            }
                            trainingMaxes[index].takeIf(String::isNotBlank)?.let { explicit ->
                                Text("Current explicit TM · $explicit ${unitSymbol(selected.weightUnitId)}", fontWeight = FontWeight.SemiBold)
                            }
                            val derivedInputsAreApplied =
                                appliedTrainingMaxBasisKinds.getOrNull(index) == trainingMaxBasisKinds.getOrNull(index) &&
                                    appliedSourceMaxes.getOrNull(index).orEmpty().numericallyEquals(recentMaxes[index]) &&
                                    appliedTrainingMaxPercentages.getOrNull(index).orEmpty()
                                        .numericallyEquals(trainingMaxPercentages[index]) &&
                                    appliedDerivedTrainingMaxes.getOrNull(index).orEmpty()
                                        .numericallyEquals(trainingMaxes[index]) &&
                                    appliedSourceMaxes.getOrNull(index).orEmpty().isNotBlank()
                            if (!derivedInputsAreApplied) {
                                Text(
                                    "Source max or percentage has unapplied changes. Use the calculated Training Max before building the program.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.testTag("five-three-one-tm-unapplied-$fieldKey"),
                                )
                            }
                            OutlinedTextField(
                                increments[index],
                                { value -> increments = increments.toMutableList().also { it[index] = value.numericInput() } },
                                label = { Text("Cycle increase (editable suggestion)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                        } else {
                            ResponsiveFieldPair(
                                first = { field ->
                                    OutlinedTextField(
                                        trainingMaxes[index],
                                        { value -> trainingMaxes = trainingMaxes.toMutableList().also { it[index] = value.numericInput() } },
                                        label = { Text("Training Max (${unitSymbol(selected.weightUnitId)})") },
                                        modifier = field.testTag("five-three-one-training-max-$fieldKey"),
                                        singleLine = true,
                                    )
                                },
                                second = { field ->
                                    OutlinedTextField(
                                        increments[index],
                                        { value -> increments = increments.toMutableList().also { it[index] = value.numericInput() } },
                                        label = { Text("Cycle increase (editable suggestion)") },
                                        modifier = field,
                                        singleLine = true,
                                    )
                                },
                            )
                        }
                        Text(
                            if (role != null) {
                                "The starting increase is suggested from the ${role.label} role and unit. You control the saved value."
                            } else {
                                "The starting increase is suggested from this custom lift's name and unit. Review it; you control the saved value."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (layout == FiveThreeOneProgramLayout.Custom && exerciseIds.size < eligible.size) {
                        WhipOutlinedButton(
                            onClick = {
                                val nextExercise = eligible.first { candidate -> candidate.id !in exerciseIds }
                                exerciseIds = exerciseIds + nextExercise.id
                                trainingMaxes = trainingMaxes + ""
                                useRecentMaxSuggestion = useRecentMaxSuggestion + false
                                recentMaxes = recentMaxes + ""
                                trainingMaxPercentages = trainingMaxPercentages + "85"
                                trainingMaxBasisKinds = trainingMaxBasisKinds + TrainingMaxBasisKind.ActualOneRepMax.name
                                appliedSourceMaxes = appliedSourceMaxes + ""
                                appliedTrainingMaxPercentages = appliedTrainingMaxPercentages + ""
                                appliedTrainingMaxBasisKinds = appliedTrainingMaxBasisKinds + ""
                                appliedDerivedTrainingMaxes = appliedDerivedTrainingMaxes + ""
                                increments = increments + editableNumericValue(
                                    defaultFiveThreeOneCycleIncrease(
                                        unitId = nextExercise.weightUnitId,
                                        exerciseName = nextExercise.name,
                                    ),
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("five-three-one-add-custom-lift"),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add another lift")
                        }
                    }
                    Text("3 · Programming", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (plan == FiveThreeOneProgramPlan.SingleCycle) {
                        Text("Main Work", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FiveThreeOneMainScheme.entries.forEach { choice ->
                                WhipFilterChip(mainScheme == choice, { mainSchemeName = choice.name }, { Text(choice.label) })
                            }
                        }
                    } else {
                        Text(
                            "Leaders use 5s PRO without PR sets. The Anchor uses Classic Main work with PR sets and FSL.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.testTag("five-three-one-leader-anchor-policy"),
                        )
                    }
                    if (plan == FiveThreeOneProgramPlan.SingleCycle && mainScheme == FiveThreeOneMainScheme.Classic) {
                        RoutineLabeledSwitchRow(
                            label = "Final main set is a PR set",
                            checked = classicFinalSetAmrap,
                            onCheckedChange = { classicFinalSetAmrap = it },
                            supportingText = if (classicFinalSetAmrap) {
                                "The listed reps are minimums; the final set is a PR set. Stop before technical failure."
                            } else {
                                "Classic percentages and prescribed minimum reps, with no PR set."
                            },
                            testTag = "five-three-one-program-pr-set",
                        )
                    } else if (plan == FiveThreeOneProgramPlan.SingleCycle) {
                        Text(
                            "5s PRO prescribes five reps for every Main set and no PR set.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (layout != FiveThreeOneProgramLayout.Beginners && plan == FiveThreeOneProgramPlan.SingleCycle) {
                        Text("Supplemental Work", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FiveThreeOneSupplement.entries.forEach { choice ->
                                WhipFilterChip(supplement == choice, { supplementName = choice.name }, { Text(choice.label) })
                            }
                        }
                    }
                    if (supplement == FiveThreeOneSupplement.BoringButBig) {
                        OutlinedTextField(
                            value = boringButBigPercentText,
                            onValueChange = { boringButBigPercentText = it.numericInput().take(6) },
                            label = { Text("BBB percentage of Training Max") },
                            supportingText = { Text("Creates five Supplemental sets of 10 at this percentage.") },
                            isError = boringButBigPercent?.let { it !in 1.0..100.0 } != false,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("five-three-one-program-bbb-percent"),
                        )
                        if (layout != FiveThreeOneProgramLayout.Beginners && lifts.isNotEmpty()) {
                            Text("BBB lift mapping", style = MaterialTheme.typography.labelLarge)
                            Text(
                                "Use the Main lift again or choose another selected program lift. The alternate uses its own Training Max.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            lifts.forEachIndexed { index, mainLift ->
                                val selectedTarget = lifts.firstOrNull { it.exerciseId == bbbTargetIds.getOrNull(index) }
                                    ?: mainLift
                                SelectionField(
                                    label = "BBB after ${mainLift.exerciseName}",
                                    values = lifts,
                                    selected = selectedTarget,
                                    valueText = { target ->
                                        if (target.exerciseId == mainLift.exerciseId) "${target.exerciseName} · same lift"
                                        else target.exerciseName
                                    },
                                    onSelect = { target ->
                                        bbbTargetIds = List(exerciseIds.size) { slot ->
                                            if (slot == index) target.exerciseId
                                            else bbbTargetIds.getOrNull(slot)?.takeIf { it in exerciseIds } ?: exerciseIds[slot]
                                        }
                                    },
                                    modifier = Modifier.testTag("five-three-one-bbb-lift-${mainLift.exerciseId}"),
                                )
                            }
                        }
                    }
                    Text("7th Week protocol", style = MaterialTheme.typography.labelLarge)
                    Text(
                        if (plan == FiveThreeOneProgramPlan.SingleCycle) {
                            "This closes the cycle."
                        } else {
                            "The Leader transition uses Deload. Choose the protocol that closes the full block."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FiveThreeOneSeventhWeekProtocol.entries.forEach { protocol ->
                        OutlinedCard(
                            onClick = { closingProtocolName = protocol.name },
                            modifier = Modifier.fillMaxWidth().testTag("five-three-one-protocol-${protocol.name}"),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (closingProtocol == protocol) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                            ),
                        ) {
                            Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(protocol.label, fontWeight = FontWeight.SemiBold)
                                Text(protocol.supportingText, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Text("4 · Optional work", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Joker ladder", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        (0..3).forEach { count ->
                            WhipFilterChip(
                                selected = jokerCount == count,
                                onClick = { jokerCount = count },
                                label = { Text(if (count == 0) "Off" else "$count ${if (count == 1) "set" else "sets"}") },
                                modifier = Modifier.testTag("five-three-one-joker-count-$count"),
                            )
                        }
                    }
                    if (jokerCount > 0) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(5, 10).forEach { step ->
                                WhipFilterChip(
                                    selected = jokerStepPercent == step,
                                    onClick = { jokerStepPercent = step },
                                    label = { Text("+$step% TM steps") },
                                    modifier = Modifier.testTag("five-three-one-joker-step-$step"),
                                )
                            }
                        }
                        Text(
                            "Each candidate is optional. Whip offers the next only after successful prerequisite work; a skip, failed target, RPE 9+, or RIR 1 or lower ends the ladder. Supplemental work remains afterward.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text("5 · Assistance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    RoutineLabeledSwitchRow(
                        label = "Build a balanced assistance draft",
                        checked = automaticAssistanceEnabled,
                        onCheckedChange = { automaticAssistanceEnabled = it },
                        supportingText = "Uses compatible exercises already in your Library. Nothing is created silently, and every choice remains editable.",
                        testTag = "five-three-one-assistance-enabled",
                    )
                    if (automaticAssistanceEnabled) {
                        val perCategoryTarget = if (layout == FiveThreeOneProgramLayout.Beginners) "5 × 10 · 50 reps" else "3 × 10 · 30 reps"
                        assistanceCategories.forEachIndexed { index, category ->
                            val categoryLabel = when (category) {
                                RoutineAssistanceCategory.Push -> "Push"
                                RoutineAssistanceCategory.Pull -> "Pull"
                                RoutineAssistanceCategory.SingleLegCore -> "Single-leg / Core"
                                else -> "Assistance"
                            }
                            val selectedId = assistanceExerciseIds.getOrNull(index) ?: 0L
                            val rankedCandidates = assistanceSuggestions[category].orEmpty() +
                                compatibleAssistanceExercises.filterNot { candidate ->
                                    assistanceSuggestions[category].orEmpty().any { it.id == candidate.id }
                                }
                            val candidates = rankedCandidates.filter { candidate ->
                                candidate.id == selectedId || candidate.id !in assistanceExerciseIds.filterIndexed { otherIndex, _ -> otherIndex != index }
                            }
                            val selected = candidates.firstOrNull { it.id == selectedId }
                            Text("$categoryLabel · $perCategoryTarget", style = MaterialTheme.typography.labelLarge)
                            if (selected != null) {
                                SelectionField(
                                    label = "$categoryLabel exercise",
                                    values = candidates,
                                    selected = selected,
                                    valueText = Exercise::name,
                                    onSelect = { exercise ->
                                        assistanceExerciseIds = assistanceExerciseIds.toMutableList().also { it[index] = exercise.id }
                                        manuallyChangedAssistanceIndices = (manuallyChangedAssistanceIndices + index).distinct()
                                    },
                                    modifier = Modifier.testTag("five-three-one-assistance-${category.name}"),
                                )
                                WhipTextButton(
                                    onClick = {
                                        assistanceExerciseIds = assistanceExerciseIds.toMutableList().also { it[index] = 0L }
                                        manuallyChangedAssistanceIndices = (manuallyChangedAssistanceIndices + index).distinct()
                                    },
                                    modifier = Modifier.testTag("five-three-one-assistance-omit-${category.name}"),
                                ) { Text("Omit $categoryLabel") }
                            } else {
                                Text(
                                    if (candidates.isEmpty()) "No compatible active rep-based Library exercise is available. Add this category later in the routine editor."
                                    else "$categoryLabel is omitted. Choose any compatible Library exercise or leave it out.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                candidates.firstOrNull()?.let { candidate ->
                                    WhipOutlinedButton(
                                        onClick = {
                                            assistanceExerciseIds = assistanceExerciseIds.toMutableList().also { it[index] = candidate.id }
                                            manuallyChangedAssistanceIndices = manuallyChangedAssistanceIndices.filterNot { it == index }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) { Text("Use ${candidate.name}") }
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                    Text("6 · Review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (plan == FiveThreeOneProgramPlan.SingleCycle) {
                            "4 weeks · 5s → 3s → 5/3/1 → ${closingProtocol.label}"
                        } else {
                            "11 weeks · Leader 1 (3) → Leader 2 (3) → Deload → Anchor (3) → ${closingProtocol.label}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("five-three-one-program-timeline"),
                    )
                    if (assistanceChoices.isNotEmpty()) {
                        Text(
                            "Assistance · " + assistanceChoices.joinToString(" · ") { choice ->
                                "${choice.category.fiveThreeOneUiLabel()}: ${choice.exerciseName}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text("Rounded working-load review", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "Inspect any week before building. Loads round to each exercise's configured increment.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FiveThreeOnePhase.entries.forEach { choice ->
                            WhipFilterChip(
                                selected = previewPhase == choice,
                                onClick = { previewPhaseName = choice.name },
                                label = { Text(choice.label) },
                                modifier = Modifier.testTag("five-three-one-program-preview-${choice.name}"),
                            )
                        }
                    }
                    lifts.forEach { lift ->
                        val previewConfig = FiveThreeOneAuthoringConfig(
                            trainingMax = lift.trainingMax,
                            mainScheme = mainScheme,
                            phase = previewPhase,
                            supplement = if (layout == FiveThreeOneProgramLayout.Beginners) {
                                FiveThreeOneSupplement.FirstSetLast
                            } else supplement,
                            classicFinalSetAmrap = classicFinalSetAmrap,
                            boringButBigPercent = boringButBigPercent ?: 50.0,
                            jokerSetsEnabled = jokerCount > 0 && plan == FiveThreeOneProgramPlan.SingleCycle,
                            jokerSetCount = if (plan == FiveThreeOneProgramPlan.SingleCycle) jokerCount else 0,
                            jokerStepPercent = jokerStepPercent.toDouble(),
                        )
                        val mainPreview = runCatching {
                            previewFiveThreeOneSets(previewConfig, lift.loadIncrement)
                                .filter { it.plan.section == FiveThreeOneSetSection.Main }
                        }.getOrDefault(emptyList())
                        if (mainPreview.isNotEmpty()) {
                            Text(
                                "${lift.exerciseName} · " + mainPreview.joinToString(" · ") { set ->
                                    "${editableNumericValue(set.roundedLoad)} ${unitSymbol(lift.unitId)} × ${set.plan.repetitionLabel}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.testTag("five-three-one-program-preview-lift-${lift.exerciseId}"),
                            )
                        }
                    }
                    buildBlocker?.let { message ->
                        Text(
                            message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("five-three-one-program-build-blocker"),
                        )
                    }
                }
            }
        },
        confirmButton = {
            WhipButton(
                enabled = valid,
                onClick = {
                    onApply(
                        FiveThreeOneProgramRequest(
                            layout = layout,
                            plan = plan,
                            lifts = lifts,
                            mainScheme = mainScheme,
                            supplement = supplement,
                            closingProtocol = closingProtocol,
                            jokerLadder = FiveThreeOneJokerLadder(jokerCount, jokerStepPercent.toDouble()),
                            classicFinalSetAmrap = classicFinalSetAmrap,
                            boringButBigPercent = boringButBigPercent ?: 50.0,
                            progressionMode = RoutineProgressionMode.valueOf(progressionModeName),
                            bbbLiftByMainExerciseId = bbbLiftByMainExerciseId,
                            assistance = assistanceChoices,
                        ),
                    )
                },
                modifier = Modifier.testTag("five-three-one-program-create"),
            ) { Text(if (replacingExistingRoutine) "Replace Draft with Program" else "Build Program") }
        },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RoutineProgramStructurePage(
    modifier: Modifier,
    builder: RoutineBuilderState,
    gymState: GymUiState,
    currentProgramPhaseIndex: Int?,
    pendingTrainingMaxDerivations: List<PendingTrainingMaxDerivation>,
    onPendingTrainingMaxDerivationChange: (Long, PendingTrainingMaxDerivation?) -> Unit,
    onBuilderChange: ((RoutineBuilderState) -> RoutineBuilderState) -> Unit,
) {
    val savedCurrentPhase = (builder.currentProgramPhaseIndexHint ?: currentProgramPhaseIndex)
        ?.coerceIn(0, (builder.programPhaseCount - 1).coerceAtLeast(0))
    var selectedPhase by rememberSaveable(builder.token) { mutableStateOf(savedCurrentPhase ?: 0) }
    var prescriptionExerciseId by rememberSaveable(builder.token) { mutableStateOf<Long?>(null) }
    var pendingRemovePhase by rememberSaveable(builder.token) { mutableStateOf<Int?>(null) }
    LaunchedEffect(builder.programPhaseCount) {
        selectedPhase = selectedPhase.coerceIn(0, (builder.programPhaseCount - 1).coerceAtLeast(0))
    }
    val labels = builder.normalizedProgramPhaseLabels()
    val roles = builder.normalizedProgramPhaseRoles()
    val selectedRole = roles.getOrElse(selectedPhase) { RoutineProgramPhaseRole.Standard }
    val mainLifts = builder.days.asSequence()
        .flatMap { it.placements.asSequence() }
        .filter { it.placementKind == RoutinePlacementKind.MainLift.name }
        .distinctBy { it.exerciseId }
        .toList()
    LaunchedEffect(mainLifts.map { it.exerciseId }) {
        if (prescriptionExerciseId != null && mainLifts.none { it.exerciseId == prescriptionExerciseId }) {
            prescriptionExerciseId = null
        }
    }
    val policiesByExercise = mainLifts.mapNotNull { lift ->
        builder.fiveThreeOnePhasePolicy(selectedPhase, lift.exerciseId)?.let { lift.exerciseId to it }
    }
    val exactUniformPolicy = policiesByExercise.map(Pair<Long, FiveThreeOnePhasePolicyState>::second)
        .distinct().singleOrNull()
    val uniformPolicy = exactUniformPolicy ?: policiesByExercise.map { (_, candidate) ->
        candidate.copy(
            alternateSupplementalExerciseId = null,
            alternateSupplementalExerciseName = null,
        )
    }.distinct().singleOrNull()
    val policy = prescriptionExerciseId?.let { id -> policiesByExercise.firstOrNull { it.first == id }?.second }
        ?: uniformPolicy
    val mixedProgramPolicy = prescriptionExerciseId == null && policiesByExercise.isNotEmpty() && uniformPolicy == null

    LazyColumn(
        modifier = modifier.fillMaxWidth().testTag("routine-program-structure-page"),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Review the whole program", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Move backward or forward through phases here. Changes update this draft immediately; use the routine Save action when you are finished. Your current cycle and day position are preserved when labels, roles, or boundaries change.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (builder.programKind in setOf(
                RoutineProgramKind.FiveThreeOne.name,
                RoutineProgramKind.FiveThreeOneClassic.name,
                RoutineProgramKind.FiveSPro.name,
                RoutineProgramKind.BoringButBig.name,
                RoutineProgramKind.FirstSetLast.name,
            )
        ) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cycle progression", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WhipFilterChip(
                        selected = builder.progressionMode == RoutineProgressionMode.Standard.name,
                        onClick = {
                            onBuilderChange {
                                it.copy(
                                    progressionMode = RoutineProgressionMode.Standard.name,
                                    allowNonStandardHigherSuggestions = false,
                                )
                            }
                        },
                        label = { Text("5/3/1 standard") },
                    )
                    WhipFilterChip(
                        selected = builder.progressionMode == RoutineProgressionMode.PerformanceInformed.name,
                        onClick = {
                            onBuilderChange { it.copy(progressionMode = RoutineProgressionMode.PerformanceInformed.name) }
                        },
                        label = { Text("Performance review") },
                    )
                }
                Text(
                    if (builder.progressionMode == RoutineProgressionMode.PerformanceInformed.name) {
                        "At a cycle boundary, review each lift independently. Nothing changes until you choose the suggestion, the saved standard increase, a custom value, or Hold. Log RPE or RIR on PR and Joker sets when you want effort-sensitive lower or higher suggestions."
                    } else {
                        "Use the saved per-lift 5/3/1 increase at each boundary. Required Main work can still hold only the affected lift."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (builder.progressionMode == RoutineProgressionMode.PerformanceInformed.name) {
                    RoutineLabeledSwitchRow(
                        label = "Show cautiously higher alternatives",
                        checked = builder.allowNonStandardHigherSuggestions,
                        onCheckedChange = { checked ->
                            onBuilderChange { it.copy(allowNonStandardHigherSuggestions = checked) }
                        },
                        supportingText = "Whip suggestion · non-standard 5/3/1 option. It requires repeated strong evidence and is never selected automatically.",
                        testTag = "five-three-one-allow-higher-suggestions",
                    )
                }
            }
        }
        item {
            Text("Main lifts & Training Maxes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Training Max is separate from your actual or estimated 1RM. Editing a repeated lift updates every day where that lift appears.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        mainLifts.forEach { lift ->
            item(key = "program-lift-${lift.exerciseId}") {
                val exercise = (gymState.exercises + gymState.archivedExercises).firstOrNull { it.id == lift.exerciseId }
                val unitId = lift.trainingMaxUnitId.ifBlank { exercise?.weightUnitId ?: gymState.appSettings.gymWeightUnitId }
                val trainingMaxIsInvalid = lift.trainingMaxValue.toWhipDoubleOrNull()
                    ?.let { !it.isFinite() || it <= 0.0 } != false
                val cycleIncreaseIsInvalid = lift.cycleIncrementValue.toWhipDoubleOrNull()
                    ?.let { !it.isFinite() || it <= 0.0 } != false
                val appliedBasisKind = runCatching { TrainingMaxBasisKind.valueOf(lift.trainingMaxBasisKind) }
                    .getOrDefault(TrainingMaxBasisKind.ExplicitTrainingMax)
                val pendingDerivation = pendingTrainingMaxDerivations.firstOrNull {
                    it.exerciseId == lift.exerciseId
                }
                val editedBasis = pendingDerivation ?: PendingTrainingMaxDerivation(
                    exerciseId = lift.exerciseId,
                    basisKind = lift.trainingMaxBasisKind,
                    basisValue = lift.trainingMaxBasisValue,
                    basisUnitId = lift.trainingMaxBasisUnitId,
                    percentage = lift.trainingMaxPercent.ifBlank { "85" },
                )
                val basisKind = runCatching { TrainingMaxBasisKind.valueOf(editedBasis.basisKind) }
                    .getOrDefault(appliedBasisKind)
                val loadIncrement = exercise?.weightIncrement?.takeIf { it > 0.0 }
                    ?: if (unitId == "pound") 5.0 else 2.5
                val derivation = FiveThreeOneTrainingMaxEntryState(
                    explicitTrainingMax = lift.trainingMaxValue,
                    recentMaxOrEstimatedOneRepMax = editedBasis.basisValue,
                    trainingMaxPercentage = editedBasis.percentage,
                )
                val derivedSuggestion = derivation.suggestionOrNull(loadIncrement)
                var basisExpanded by rememberSaveable(lift.key) { mutableStateOf(false) }
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(exercise?.name ?: lift.exerciseNameSnapshot, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        ResponsiveFieldPair(
                            first = { field ->
                                OutlinedTextField(
                                    value = lift.trainingMaxValue,
                                    onValueChange = { value ->
                                        onPendingTrainingMaxDerivationChange(lift.exerciseId, null)
                                        onBuilderChange { current ->
                                            current.updateProgramPlacement(lift.key) {
                                                it.copy(
                                                    trainingMaxValue = value.numericInput(),
                                                    trainingMaxUnitId = unitId,
                                                    trainingMaxSource = RoutineTrainingMaxSource.Explicit.name,
                                                    trainingMaxBasisKind = TrainingMaxBasisKind.ExplicitTrainingMax.name,
                                                    trainingMaxBasisValue = value.numericInput(),
                                                    trainingMaxBasisUnitId = unitId,
                                                )
                                            }
                                        }
                                    },
                                    label = { Text("Training Max (${unitSymbol(unitId)})") },
                                    supportingText = { Text(if (trainingMaxIsInvalid) "Enter a Training Max above zero" else "Explicit TM") },
                                    isError = trainingMaxIsInvalid,
                                    singleLine = true,
                                    modifier = field.testTag("routine-program-training-max-${lift.exerciseId}"),
                                )
                            },
                            second = { field ->
                                OutlinedTextField(
                                    value = lift.cycleIncrementValue,
                                    onValueChange = { value ->
                                        onBuilderChange { current ->
                                            current.updateProgramPlacement(lift.key) {
                                                it.copy(cycleIncrementValue = value.numericInput())
                                            }
                                        }
                                    },
                                    label = { Text("Cycle increase (${unitSymbol(unitId)})") },
                                    supportingText = {
                                        Text(if (cycleIncreaseIsInvalid) "Enter a cycle increase above zero" else "Applied at selected TM boundaries")
                                    },
                                    isError = cycleIncreaseIsInvalid,
                                    singleLine = true,
                                    modifier = field.testTag("routine-program-cycle-increase-${lift.exerciseId}"),
                                )
                            },
                        )
                        DisclosureRow(
                            title = "Training Max basis",
                            supportingText = when (basisKind) {
                                TrainingMaxBasisKind.ActualOneRepMax -> "Actual 1RM · ${editedBasis.percentage}%"
                                TrainingMaxBasisKind.EstimatedOneRepMax -> "Estimated 1RM · ${editedBasis.percentage}%"
                                TrainingMaxBasisKind.ManualSourceMax -> "Other source max · ${editedBasis.percentage}%"
                                TrainingMaxBasisKind.ExplicitTrainingMax,
                                TrainingMaxBasisKind.Unspecified,
                                -> "Entered directly"
                            },
                            expanded = basisExpanded,
                            onClick = { basisExpanded = !basisExpanded },
                            modifier = Modifier.testTag("routine-program-tm-basis-${lift.exerciseId}"),
                        )
                        if (basisExpanded) {
                            Text(
                                "Re-deriving changes only this program's future Training Max. Completed workouts keep their original prescriptions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                listOf(
                                    TrainingMaxBasisKind.ActualOneRepMax to "Actual 1RM",
                                    TrainingMaxBasisKind.EstimatedOneRepMax to "Estimated 1RM",
                                    TrainingMaxBasisKind.ManualSourceMax to "Other source max",
                                    TrainingMaxBasisKind.ExplicitTrainingMax to "Direct TM",
                                ).forEach { (basis, label) ->
                                    WhipFilterChip(
                                        selected = basisKind == basis,
                                        onClick = {
                                            val matchingRecord = when (basis) {
                                                TrainingMaxBasisKind.ActualOneRepMax -> gymState.personalRecords.firstOrNull { record ->
                                                    record.exerciseId == lift.exerciseId && record.current &&
                                                        record.type == PersonalRecordType.BestWeightForRepCount &&
                                                        record.secondaryValue == 1.0 && record.machineProfileUuidSnapshot == null
                                                }
                                                TrainingMaxBasisKind.EstimatedOneRepMax -> gymState.personalRecords.firstOrNull { record ->
                                                    record.exerciseId == lift.exerciseId && record.current &&
                                                        record.type == PersonalRecordType.EstimatedOneRepMax &&
                                                        record.machineProfileUuidSnapshot == null
                                                }
                                                else -> null
                                            }
                                            val sourceValue = when (basis) {
                                                TrainingMaxBasisKind.ActualOneRepMax,
                                                TrainingMaxBasisKind.EstimatedOneRepMax,
                                                -> matchingRecord?.let { record ->
                                                    editableNumericValue(massFromKilograms(record.value, unitId))
                                                }.orEmpty()
                                                TrainingMaxBasisKind.ManualSourceMax -> ""
                                                TrainingMaxBasisKind.ExplicitTrainingMax -> lift.trainingMaxValue
                                                TrainingMaxBasisKind.Unspecified -> ""
                                            }
                                            if (basis == TrainingMaxBasisKind.ExplicitTrainingMax) {
                                                onPendingTrainingMaxDerivationChange(lift.exerciseId, null)
                                                onBuilderChange { current ->
                                                    current.updateProgramPlacement(lift.key) {
                                                        it.copy(
                                                            trainingMaxBasisKind = basis.name,
                                                            trainingMaxBasisValue = it.trainingMaxValue,
                                                            trainingMaxBasisUnitId = unitId,
                                                            trainingMaxSource = RoutineTrainingMaxSource.Explicit.name,
                                                        )
                                                    }
                                                }
                                            } else {
                                                val candidate = editedBasis.copy(
                                                    basisKind = basis.name,
                                                    basisValue = sourceValue,
                                                    basisUnitId = unitId,
                                                )
                                                onPendingTrainingMaxDerivationChange(
                                                    lift.exerciseId,
                                                    candidate.takeUnless { it.matchesApplied(lift) },
                                                )
                                            }
                                        },
                                        label = { Text(label) },
                                        modifier = Modifier.testTag("routine-program-tm-basis-${lift.exerciseId}-${basis.name}"),
                                    )
                                }
                            }
                            if (basisKind !in setOf(
                                    TrainingMaxBasisKind.ExplicitTrainingMax,
                                    TrainingMaxBasisKind.Unspecified,
                                )
                            ) {
                                ResponsiveFieldPair(
                                    first = { field ->
                                        OutlinedTextField(
                                            value = editedBasis.basisValue,
                                            onValueChange = { value ->
                                                val candidate = editedBasis.copy(
                                                    basisValue = value.numericInput(),
                                                    basisUnitId = unitId,
                                                )
                                                onPendingTrainingMaxDerivationChange(
                                                    lift.exerciseId,
                                                    candidate.takeUnless { it.matchesApplied(lift) },
                                                )
                                            },
                                            label = {
                                                Text(
                                                    when (basisKind) {
                                                        TrainingMaxBasisKind.ActualOneRepMax -> "Actual 1RM (${unitSymbol(unitId)})"
                                                        TrainingMaxBasisKind.EstimatedOneRepMax -> "Estimated 1RM (${unitSymbol(unitId)})"
                                                        else -> "Source max (${unitSymbol(unitId)})"
                                                    },
                                                )
                                            },
                                            singleLine = true,
                                            modifier = field.testTag("routine-program-tm-source-${lift.exerciseId}"),
                                        )
                                    },
                                    second = { field ->
                                        OutlinedTextField(
                                            value = editedBasis.percentage,
                                            onValueChange = { value ->
                                                val candidate = editedBasis.copy(percentage = value.numericInput())
                                                onPendingTrainingMaxDerivationChange(
                                                    lift.exerciseId,
                                                    candidate.takeUnless { it.matchesApplied(lift) },
                                                )
                                            },
                                            label = { Text("TM percentage") },
                                            supportingText = { Text("1–100%; 80–90% is the common starting range") },
                                            isError = editedBasis.percentage.toWhipDoubleOrNull()?.let { it !in 1.0..100.0 } != false,
                                            singleLine = true,
                                            modifier = field.testTag("routine-program-tm-percent-${lift.exerciseId}"),
                                        )
                                    },
                                )
                                if (editedBasis.percentage.toWhipDoubleOrNull()?.let { it !in 80.0..90.0 } == true) {
                                    Text(
                                        "Outside the common 80–90% range. This can be intentional for readiness; review before applying.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                                WhipOutlinedButton(
                                    enabled = derivedSuggestion != null,
                                    onClick = {
                                        val suggestion = derivedSuggestion ?: return@WhipOutlinedButton
                                        onBuilderChange { current ->
                                            current.updateProgramPlacement(lift.key) {
                                                it.copy(
                                                    trainingMaxValue = editableNumericValue(suggestion),
                                                    trainingMaxUnitId = unitId,
                                                    trainingMaxSource = RoutineTrainingMaxSource.Explicit.name,
                                                    trainingMaxBasisKind = editedBasis.basisKind,
                                                    trainingMaxBasisValue = editedBasis.basisValue,
                                                    trainingMaxBasisUnitId = editedBasis.basisUnitId,
                                                    trainingMaxPercent = editedBasis.percentage,
                                                )
                                            }
                                        }
                                        onPendingTrainingMaxDerivationChange(lift.exerciseId, null)
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("routine-program-apply-tm-${lift.exerciseId}"),
                                ) {
                                    Text(
                                        derivedSuggestion?.let { value ->
                                            "Apply ${editableNumericValue(value)} ${unitSymbol(unitId)} Training Max"
                                        } ?: "Enter a source max and 1–100%",
                                    )
                                }
                                if (pendingDerivation != null) {
                                    Text(
                                        "Training Max basis has unapplied changes. Apply the calculated Training Max before saving the routine.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.testTag("routine-program-tm-unapplied-${lift.exerciseId}"),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            HorizontalDivider()
            Text("Program phases", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Select any phase to inspect or change it. This is a preview/edit cursor—it does not move the program's current training position.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                labels.forEachIndexed { index, label ->
                    WhipFilterChip(
                        selected = selectedPhase == index,
                        onClick = { selectedPhase = index },
                        label = {
                            Text("${index + 1} · $label${if (index == savedCurrentPhase) " · Current" else ""}")
                        },
                        modifier = Modifier.testTag("routine-program-phase-select-$index"),
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WhipOutlinedButton(
                    enabled = selectedPhase > 0,
                    onClick = { selectedPhase-- },
                    modifier = Modifier.weight(1f).testTag("routine-program-previous-phase"),
                ) { Text("Previous Phase") }
                Spacer(Modifier.width(8.dp))
                WhipOutlinedButton(
                    enabled = selectedPhase < builder.programPhaseCount - 1,
                    onClick = { selectedPhase++ },
                    modifier = Modifier.weight(1f).testTag("routine-program-next-phase"),
                ) { Text("Next Phase") }
            }
        }
        item {
            Text(
                "Phase ${selectedPhase + 1} of ${builder.programPhaseCount} · ${labels.getOrElse(selectedPhase) { "" }}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("routine-program-selected-phase"),
            )
            OutlinedTextField(
                value = labels.getOrElse(selectedPhase) { "" },
                onValueChange = { value ->
                    onBuilderChange { current -> current.updateProgramPhaseMetadata(selectedPhase, label = value) }
                },
                label = { Text("Phase label") },
                modifier = Modifier.fillMaxWidth().testTag("routine-program-phase-label-$selectedPhase"),
                singleLine = true,
            )
        }
        item {
            SelectionField(
                label = "Phase role",
                values = RoutineProgramPhaseRole.entries.filterNot(RoutineProgramPhaseRole::usesOncePerLiftProtocol),
                selected = selectedRole.semanticRole(),
                valueText = RoutineProgramPhaseRole::uiLabel,
                onSelect = { role ->
                    onBuilderChange { current ->
                        val storedRole = if (role == selectedRole.semanticRole()) selectedRole else role
                        val metadata = current.updateProgramPhaseMetadata(selectedPhase, role = storedRole)
                        if (role.disallowsJokers()) {
                            metadata.removeFiveThreeOneJokers(selectedPhase)
                        } else metadata
                    }
                },
                modifier = Modifier.testTag("routine-program-phase-role-$selectedPhase"),
            )
            Text(
                if (selectedRole.semanticRole() == RoutineProgramPhaseRole.TrainingMaxTest) {
                    "Mark exactly one Main set as Training Max test and prescribe 100% TM for 3–5 reps. Other build-up sets remain ordinary Main work; record RPE or RIR to describe rep quality."
                } else {
                    "Leader and Anchor describe block membership. Use a 7th Week preset below when you want Whip to replace this phase with a complete reviewable protocol."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Text("Apply 7th Week preset", style = MaterialTheme.typography.labelLarge)
            Text(
                "One tap replaces only this phase's Main, Supplemental, and Joker prescription. Other phases and completed workouts are unchanged.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FiveThreeOneSeventhWeekProtocol.entries.forEach { protocol ->
                    OutlinedCard(
                        onClick = {
                            onBuilderChange { current ->
                                current.applyFiveThreeOneSeventhWeekProtocol(selectedPhase, protocol)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                            .testTag("routine-program-apply-seventh-week-${protocol.name}"),
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(protocol.label, fontWeight = FontWeight.SemiBold)
                            Text(protocol.supportingText, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        if (mainLifts.isNotEmpty()) {
            item {
                Text("Prescription for this phase", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Choose one lift to preserve intentional differences, or apply a uniform policy to all mapped main lifts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    WhipFilterChip(
                        selected = prescriptionExerciseId == null,
                        onClick = { prescriptionExerciseId = null },
                        label = { Text("All main lifts") },
                        modifier = Modifier.testTag("routine-program-policy-scope-all"),
                    )
                    mainLifts.forEach { lift ->
                        val liftName = (gymState.exercises + gymState.archivedExercises).firstOrNull { it.id == lift.exerciseId }?.name
                            ?: lift.exerciseNameSnapshot
                        WhipFilterChip(
                            selected = prescriptionExerciseId == lift.exerciseId,
                            onClick = { prescriptionExerciseId = lift.exerciseId },
                            label = { Text(liftName) },
                            modifier = Modifier.testTag("routine-program-policy-scope-${lift.exerciseId}"),
                        )
                    }
                }
                if (mixedProgramPolicy) {
                    Text(
                        "This phase has different prescriptions between lifts. Choose a lift to edit it without overwriting the others.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("routine-program-policy-mixed"),
                    )
                } else if (policy != null) {
                    val scopeLabel = prescriptionExerciseId?.let { id ->
                        (gymState.exercises + gymState.archivedExercises).firstOrNull { it.id == id }?.name
                    } ?: "All main lifts"
                    Text(
                        "$scopeLabel · ${policy.mainWorkScheme.uiLabel()} · ${policy.supplementalScheme.uiLabel()}" +
                            policy.alternateSupplementalExerciseName?.let { " · alternate lift: $it" }.orEmpty() +
                            if (policy.jokerEnabled) " · ${policy.jokerCount} optional ${if (policy.jokerCount == 1) "Joker" else "Jokers"}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("routine-program-policy-summary"),
                    )
                }
            }
        }
        if (policy != null) {
            item {
                val summaries = builder.fiveThreeOnePhasePrescriptionSummary(
                    selectedPhase,
                    prescriptionExerciseId ?: mainLifts.firstOrNull()?.exerciseId,
                )
                if (summaries.isNotEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().testTag("routine-program-phase-prescription-summary-$selectedPhase"),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        summaries.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
            item {
                SelectionField(
                    label = "Main work",
                    values = listOf(
                        RoutineMainWorkScheme.ClassicPrSet,
                        RoutineMainWorkScheme.ClassicMinimumReps,
                        RoutineMainWorkScheme.FivesPro,
                    ),
                    selected = policy.mainWorkScheme.takeUnless { it == RoutineMainWorkScheme.Unspecified }
                        ?: RoutineMainWorkScheme.ClassicMinimumReps,
                    valueText = RoutineMainWorkScheme::uiLabel,
                    onSelect = { scheme ->
                        onBuilderChange { current ->
                            val currentPolicy = current.fiveThreeOnePhasePolicy(selectedPhase, prescriptionExerciseId)
                                ?: return@onBuilderChange current
                            current.applyFiveThreeOnePhasePolicy(
                                selectedPhase,
                                scheme,
                                currentPolicy.supplementalScheme,
                                currentPolicy.jokerEnabled && !selectedRole.disallowsJokers(),
                                jokerCount = currentPolicy.jokerCount,
                                jokerStepPercent = currentPolicy.jokerStepPercent,
                                exerciseId = prescriptionExerciseId,
                            )
                        }
                    },
                    modifier = Modifier.testTag("routine-program-phase-main-$selectedPhase"),
                )
            }
            item {
                SelectionField(
                    label = "Supplemental work",
                    values = RoutineSupplementalScheme.entries,
                    selected = policy.supplementalScheme,
                    valueText = RoutineSupplementalScheme::uiLabel,
                    onSelect = { scheme ->
                        onBuilderChange { current ->
                            val currentPolicy = current.fiveThreeOnePhasePolicy(selectedPhase, prescriptionExerciseId)
                                ?: return@onBuilderChange current
                            current.applyFiveThreeOnePhasePolicy(
                                selectedPhase,
                                currentPolicy.mainWorkScheme,
                                scheme,
                                currentPolicy.jokerEnabled && !selectedRole.disallowsJokers(),
                                jokerCount = currentPolicy.jokerCount,
                                jokerStepPercent = currentPolicy.jokerStepPercent,
                                exerciseId = prescriptionExerciseId,
                            )
                        }
                    },
                    modifier = Modifier.testTag("routine-program-phase-supplemental-$selectedPhase"),
                )
                policy.alternateSupplementalExerciseName?.let { alternateName ->
                    Text(
                        "Alternate-lift BBB uses $alternateName and its own Training Max. Selecting another Supplemental scheme replaces this phase's alternate BBB; Main-work and Joker edits preserve it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("routine-program-alternate-bbb-summary"),
                    )
                }
            }
            item {
                Text("Optional Joker ladder", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (0..3).forEach { count ->
                        WhipFilterChip(
                            selected = policy.jokerCount == count && !selectedRole.disallowsJokers(),
                            enabled = !selectedRole.disallowsJokers(),
                            onClick = {
                                onBuilderChange { current ->
                                    current.setFiveThreeOneJokerLadder(
                                        selectedPhase,
                                        count,
                                        policy.jokerStepPercent,
                                        prescriptionExerciseId,
                                    )
                                }
                            },
                            label = { Text(if (count == 0) "Off" else count.toString()) },
                            modifier = Modifier.testTag("routine-program-phase-joker-count-$selectedPhase-$count"),
                        )
                    }
                }
                if (policy.jokerCount > 0 && !selectedRole.disallowsJokers()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(5.0, 10.0).forEach { step ->
                            WhipFilterChip(
                                selected = policy.jokerStepPercent == step,
                                onClick = {
                                    onBuilderChange { current ->
                                        current.setFiveThreeOneJokerLadder(
                                            selectedPhase,
                                            policy.jokerCount,
                                            step,
                                            prescriptionExerciseId,
                                        )
                                    }
                                },
                                label = { Text("+${step.toInt()}% TM") },
                            )
                        }
                    }
                }
                Text(
                    if (selectedRole.disallowsJokers()) {
                        "Joker candidates are removed from deload and test phases."
                    } else {
                        "Each candidate remains Optional and additive. Supplemental work stays after the ladder."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            RoutineLabeledSwitchRow(
                label = "Advance Training Max after this phase",
                checked = selectedPhase in builder.trainingMaxAdvanceAfterPhaseIndices,
                onCheckedChange = { checked ->
                    onBuilderChange { current ->
                        current.updateProgramPhaseMetadata(selectedPhase, advancesTrainingMax = checked)
                    }
                },
                supportingText = "Use only at an intentional cycle/block boundary. Incomplete required Main work holds the increase; History remains unchanged.",
                testTag = "routine-program-phase-tm-boundary-$selectedPhase",
            )
        }
        item {
            Text(
                "Structure changes preserve the current cycle/day position where possible. After removing or reordering phases, review Set Program Position from the routine menu.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WhipOutlinedButton(
                    enabled = builder.programPhaseCount < 52,
                    onClick = {
                        val newPhase = builder.programPhaseCount
                        onBuilderChange { it.addProgramPhase(selectedPhase) }
                        selectedPhase = newPhase
                    },
                    modifier = Modifier.weight(1f).testTag("routine-program-add-phase"),
                ) { Text("Copy as New Phase") }
                WhipTextButton(
                    enabled = builder.programPhaseCount > 1,
                    onClick = { pendingRemovePhase = selectedPhase },
                    modifier = Modifier.weight(1f).testTag("routine-program-remove-phase"),
                ) { Text("Remove Phase") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WhipTextButton(
                    enabled = selectedPhase > 0,
                    onClick = {
                        onBuilderChange { it.moveProgramPhase(selectedPhase, selectedPhase - 1) }
                        selectedPhase--
                    },
                    modifier = Modifier.weight(1f).testTag("routine-program-move-phase-earlier"),
                ) { Text("Move Earlier") }
                WhipTextButton(
                    enabled = selectedPhase < builder.programPhaseCount - 1,
                    onClick = {
                        onBuilderChange { it.moveProgramPhase(selectedPhase, selectedPhase + 1) }
                        selectedPhase++
                    },
                    modifier = Modifier.weight(1f).testTag("routine-program-move-phase-later"),
                ) { Text("Move Later") }
            }
        }
    }
    pendingRemovePhase?.let { phaseIndex ->
        val phaseLabel = labels.getOrNull(phaseIndex) ?: "Phase ${phaseIndex + 1}"
        val affectedSetCount = builder.days.sumOf { day ->
            day.placements.sumOf { placement -> placement.sets.count { it.routinePhaseIndex == phaseIndex } }
        }
        PaneAwareAlertDialog(
            onDismissRequest = { pendingRemovePhase = null },
            title = { Text("Remove $phaseLabel?") },
            text = {
                Text(
                    "$affectedSetCount phase-specific set${if (affectedSetCount == 1) "" else "s"}, its role, and its Training Max boundary will be removed from this draft. Workout History is unchanged.",
                )
            },
            confirmButton = {
                WhipTextButton(
                    onClick = {
                        onBuilderChange { it.removeProgramPhase(phaseIndex) }
                        selectedPhase = phaseIndex.coerceAtMost(builder.programPhaseCount - 2)
                        pendingRemovePhase = null
                    },
                    modifier = Modifier.testTag("routine-program-confirm-remove-phase"),
                ) { Text("Remove Phase") }
            },
            dismissButton = { WhipTextButton(onClick = { pendingRemovePhase = null }) { Text("Keep Phase") } },
        )
    }
}

private fun RoutineProgramPhaseRole.uiLabel(): String = when (semanticRole()) {
    RoutineProgramPhaseRole.Standard -> "Standard"
    RoutineProgramPhaseRole.Leader -> "Leader"
    RoutineProgramPhaseRole.Anchor -> "Anchor"
    RoutineProgramPhaseRole.Deload -> "Deload"
    RoutineProgramPhaseRole.TrainingMaxTest -> "Training Max Test"
    RoutineProgramPhaseRole.PersonalRecordTest -> "PR Test"
    RoutineProgramPhaseRole.OncePerLiftDeload,
    RoutineProgramPhaseRole.OncePerLiftTrainingMaxTest,
    RoutineProgramPhaseRole.OncePerLiftPersonalRecordTest,
    -> error("semanticRole() must return a public phase role")
}

private fun RoutineProgramPhaseRole.disallowsJokers(): Boolean = semanticRole() in setOf(
    RoutineProgramPhaseRole.Deload,
    RoutineProgramPhaseRole.TrainingMaxTest,
    RoutineProgramPhaseRole.PersonalRecordTest,
)

private fun RoutineMainWorkScheme.uiLabel(): String = when (this) {
    RoutineMainWorkScheme.ClassicPrSet -> "Classic · PR set"
    RoutineMainWorkScheme.ClassicMinimumReps -> "Classic · prescribed reps"
    RoutineMainWorkScheme.FivesPro -> "5s PRO"
    RoutineMainWorkScheme.Unspecified -> "Custom"
}

private fun RoutineSupplementalScheme.uiLabel(): String = when (this) {
    RoutineSupplementalScheme.None -> "None"
    RoutineSupplementalScheme.FirstSetLast -> "FSL · 5 × 5"
    RoutineSupplementalScheme.SecondSetLast -> "SSL · 5 × 5"
    RoutineSupplementalScheme.BoringButBig -> "BBB · 5 × 10"
    RoutineSupplementalScheme.BoringButStrong -> "Boring But Strong · 10 × 5"
    RoutineSupplementalScheme.Custom -> "Custom · keep current sets"
}

@Composable
private fun RoutineOutlinePane(
    modifier: Modifier,
    builder: RoutineBuilderState,
    selectedDay: RoutineBuilderDayState,
    selectedPlacementKey: Long?,
    gymState: GymUiState,
    validationErrors: Map<Long, String>,
    onBuilderChange: ((RoutineBuilderState) -> RoutineBuilderState) -> Unit,
    onSelectPlacement: (Long) -> Unit,
    onDuplicatePlacement: (RoutineBuilderPlacementState) -> Unit,
    onRemovePlacement: (RoutineBuilderPlacementState) -> Unit,
    onAddExercises: () -> Unit,
    onAddAssistance: (RoutineAssistanceRole) -> Unit,
    onAddFromWorkout: () -> Unit,
    onDeleteDay: (RoutineBuilderDayState) -> Unit,
    onCreateFiveThreeOneProgram: () -> Unit,
    onEditProgramStructure: () -> Unit,
) {
    val isFiveThreeOneProgram = builder.programKind.isFiveThreeOneProgramKindName()
    // The entire outline is one scroll surface. Routine metadata and the 5/3/1
    // summary used to sit above a separately scrolling exercise list, leaving
    // that lower viewport only tall enough for roughly one exercise on a Fold.
    // Scrolling the whole outline lets the exercise section use the full pane.
    WhipReorderLazyColumn(
        modifier = modifier.fillMaxSize().testTag("routine-selected-exercises"),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column {
        OutlinedTextField(
            value = builder.name,
            onValueChange = { value -> onBuilderChange { it.copy(name = value.replace('\n', ' ').replace('\r', ' ').take(100)) } },
            label = { Text("Routine name *") },
            supportingText = { Text("${builder.name.length}/100") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag("routine-editor-name"),
            singleLine = true,
        )
        OutlinedTextField(
            value = builder.notes,
            onValueChange = { value -> onBuilderChange { it.copy(notes = value) } },
            label = { Text("Routine notes") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            maxLines = 2,
        )
        if (isFiveThreeOneProgram && builder.programPhaseCount > 0) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    .testTag("routine-program-structure"),
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("5/3/1 Program", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        when (builder.programTemplateKey) {
                            RoutineProgramTemplateKey.FiveThreeOneFourDay.name -> "Based on 4-Day 5/3/1 · template v${builder.programTemplateRevision}"
                            RoutineProgramTemplateKey.FiveThreeOneBeginners.name -> "Based on 5/3/1 for Beginners · template v${builder.programTemplateRevision}"
                            RoutineProgramTemplateKey.FiveThreeOneCustom.name -> "Based on Custom 5/3/1 · template v${builder.programTemplateRevision}"
                            RoutineProgramTemplateKey.LegacyFiveThreeOne.name -> "Existing 5/3/1 program · original template is unknown"
                            else -> "Structured 5/3/1 program"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "${builder.programPhaseCount} phases · ${builder.normalizedProgramPhaseLabels().joinToString(" → ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Review earlier and later phases, then change one phase without digging through every lift.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    WhipOutlinedButton(
                        onClick = onEditProgramStructure,
                        modifier = Modifier.fillMaxWidth().testTag("routine-open-program-structure"),
                    ) { Text("Review & Edit Program Phases") }
                }
            }
        }
        if (builder.days.all { it.placements.isEmpty() }) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    .testTag("routine-five-three-one-program-entry"),
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Start a strength program", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Choose a standard layout or build 5/3/1 around your own Weight + Reps lifts. Gym keeps Main, Supplemental, Assistance, and Optional work distinct across the full cycle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    WhipButton(onClick = onCreateFiveThreeOneProgram, modifier = Modifier.fillMaxWidth()) {
                        Text("Set Up 5/3/1")
                    }
                }
            }
            Text("Start with a Split", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 4.dp))
            Text(
                "These shortcuts only create and name routine days. They do not choose exercises or assign Push/Pull assistance roles.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                listOf(
                    listOf("Full Body"),
                    listOf("Upper", "Lower"),
                    listOf("Push", "Pull", "Legs"),
                ).forEach { names ->
                    WhipFilterChip(
                        selected = builder.days.map { it.name } == names,
                        onClick = { onBuilderChange { current -> current.withDayTemplate(names) } },
                        label = { Text(names.joinToString(" / ")) },
                    )
                }
            }
        }
            }
        }
        item {
        WhipReorderHorizontalRow(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            builder.days.forEachIndexed { dayIndex, day ->
                val reorderInteraction = rememberWhipReorderInteractionState()
                Row(
                    modifier = Modifier.whipReorderItem(
                        reorderInteraction,
                        WhipReorderAxis.Horizontal,
                        layoutPosition = dayIndex + 1,
                        layoutScope = "routine-days",
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (builder.days.size > 1) {
                        WhipReorderHandle(
                            label = day.name,
                            canMovePrevious = dayIndex > 0,
                            canMoveNext = dayIndex < builder.days.lastIndex,
                            position = dayIndex + 1,
                            total = builder.days.size,
                            onMove = { delta -> onBuilderChange { it.moveDay(day.key, delta) } },
                            axis = WhipReorderAxis.Horizontal,
                            interactionState = reorderInteraction,
                            moveWholeItem = true,
                            layoutScope = "routine-days",
                        )
                    }
                    WhipFilterChip(
                        selected = day.key == selectedDay.key,
                        onClick = { onBuilderChange { it.copy(selectedDayKey = day.key, selectedPlacementKey = null) } },
                        label = { Text("${day.name} · ${day.placements.size}") },
                        modifier = Modifier.testTag("routine-day-${day.key}"),
                    )
                }
            }
            WhipFilterChip(
                selected = false,
                onClick = {
                    onBuilderChange { current ->
                        val key = current.nextKey
                        current.copy(
                            days = current.days + RoutineBuilderDayState(key, "Day ${current.days.size + 1}"),
                            selectedDayKey = key,
                            selectedPlacementKey = null,
                            nextKey = key + 1,
                        )
                    }
                },
                label = { Text("+ Day") },
            )
        }
        }
        item {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedTextField(
                value = selectedDay.name,
                onValueChange = { value -> onBuilderChange { current -> current.copy(days = current.days.map { if (it.key == selectedDay.key) it.copy(name = value) else it }) } },
                label = { Text("Day name") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            IconButton(
                onClick = { onBuilderChange { it.duplicateDay(selectedDay.key) } },
                modifier = Modifier.semantics { contentDescription = "Duplicate ${selectedDay.name}" },
            ) { Icon(Icons.Outlined.ContentCopy, contentDescription = null) }
            if (builder.days.size > 1) {
                IconButton(
                    onClick = { onDeleteDay(selectedDay) },
                    modifier = Modifier.semantics { contentDescription = "Delete ${selectedDay.name}" },
                ) { Icon(Icons.Outlined.Delete, contentDescription = null) }
            }
        }
        }
            if (selectedDay.placements.isEmpty()) {
                item {
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("No exercises in ${selectedDay.name}", fontWeight = FontWeight.Bold)
                            Text("Add only the exercises used in this day. The complete library stays in the picker.")
                        }
                    }
                }
            }
            val blocks = selectedDay.placements.routinePlacementBlocks()
            items(blocks.size, key = { blocks[it].stableKey }) { blockIndex ->
                val block = blocks[blockIndex]
                if (block.groupKey != null) {
                    val reorderInteraction = rememberWhipReorderInteractionState()
                    Surface(
                        modifier = Modifier.fillMaxWidth().whipReorderItem(
                            reorderInteraction,
                            layoutPosition = blockIndex + 1,
                            layoutScope = "routine-day-${selectedDay.key}-blocks",
                        ),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                    ) {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                WhipReorderHandle(
                                    label = block.groupKey,
                                    canMovePrevious = blockIndex > 0,
                                    canMoveNext = blockIndex < blocks.lastIndex,
                                    position = blockIndex + 1,
                                    total = blocks.size,
                                    interactionState = reorderInteraction,
                                    moveWholeItem = true,
                                    layoutScope = "routine-day-${selectedDay.key}-blocks",
                                    onMove = { delta ->
                                        onBuilderChange { current ->
                                            current.copy(days = current.days.map { day ->
                                                if (day.key == selectedDay.key) day.movePlacementBlock(blockIndex, delta) else day
                                            })
                                        }
                                    },
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(block.groupKey, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(
                                        "${block.placements.size} exercises · moves as one block",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            WhipReorderLayout(itemSpacing = 4.dp) {
                            block.placements.forEachIndexed { memberIndex, placement ->
                                RoutinePlacementCard(
                                    placement = placement,
                                    selected = placement.key == selectedPlacementKey,
                                    gymState = gymState,
                                    error = validationErrors[placement.key],
                                    canMovePrevious = memberIndex > 0,
                                    canMoveNext = memberIndex < block.placements.lastIndex,
                                    position = memberIndex + 1,
                                    total = block.placements.size,
                                    grouped = true,
                                    layoutScope = "routine-day-${selectedDay.key}-group-${block.groupKey}",
                                    onOpen = { onSelectPlacement(placement.key) },
                                    onMove = { delta ->
                                        onBuilderChange { current ->
                                            current.copy(days = current.days.map { day ->
                                                if (day.key == selectedDay.key) day.movePlacementWithinGroup(placement.key, delta) else day
                                            })
                                        }
                                    },
                                    onDuplicate = { onDuplicatePlacement(placement) },
                                    onRemove = { onRemovePlacement(placement) },
                                )
                            }
                            }
                        }
                    }
                } else {
                    val placement = block.placements.single()
                    RoutinePlacementCard(
                        placement = placement,
                        selected = placement.key == selectedPlacementKey,
                        gymState = gymState,
                        error = validationErrors[placement.key],
                        canMovePrevious = blockIndex > 0,
                        canMoveNext = blockIndex < blocks.lastIndex,
                        position = blockIndex + 1,
                        total = blocks.size,
                        layoutScope = "routine-day-${selectedDay.key}-blocks",
                        onOpen = { onSelectPlacement(placement.key) },
                        onMove = { delta ->
                            onBuilderChange { current ->
                                current.copy(days = current.days.map { day ->
                                    if (day.key == selectedDay.key) day.movePlacementBlock(blockIndex, delta) else day
                                })
                            }
                        },
                        onDuplicate = { onDuplicatePlacement(placement) },
                        onRemove = { onRemovePlacement(placement) },
                    )
                }
            }
            item {
                if (isFiveThreeOneProgram) {
                    OutlinedCard(
                        Modifier.fillMaxWidth().testTag("routine-assistance-plan"),
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                "Assistance for ${selectedDay.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                if (builder.programTemplateKey == RoutineProgramTemplateKey.FiveThreeOneBeginners.name) {
                                    "5/3/1 for Beginners suggests 50–100 total reps in each category. This is guidance, not a save blocker."
                                } else {
                                    "These optional routine roles keep assistance distinct from Main and Supplemental work."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "You assign each role for this day. Exercise Library categories and muscle tags never assign it automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            listOf(
                                RoutineAssistanceRole.Push,
                                RoutineAssistanceRole.Pull,
                                RoutineAssistanceRole.SingleLegCore,
                            ).forEach { role ->
                                val category = role.toBuilderAssistanceCategory().name
                                val matching = selectedDay.placements.filter {
                                    it.placementKind == RoutinePlacementKind.Assistance.name &&
                                        it.assistanceCategory == category
                                }
                                val plannedReps = matching.sumOf(RoutineBuilderPlacementState::plannedAssistanceReps)
                                WhipOutlinedButton(
                                    onClick = { onAddAssistance(role) },
                                    modifier = Modifier.fillMaxWidth()
                                        .testTag("routine-add-assistance-${role.name}"),
                                ) {
                                    Column(
                                        Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Text("Add ${role.assistanceUiLabel()} assistance", fontWeight = FontWeight.SemiBold)
                                        Text(
                                            buildList {
                                                add(role.assistanceUiDescription())
                                                if (matching.isNotEmpty()) {
                                                    add("${matching.size} selected")
                                                    add(if (plannedReps > 0) "$plannedReps planned reps" else "rep target needs review")
                                                }
                                            }.joinToString(" · "),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    WhipOutlinedButton(
                        onClick = onAddExercises,
                        modifier = Modifier.fillMaxWidth().testTag("routine-add-exercises"),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add an unclassified exercise")
                    }
                    Text(
                        "Use this for general or optional work that should not be labeled Push, Pull, or Single-leg/Core.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                } else {
                    WhipButton(onClick = onAddExercises, modifier = Modifier.fillMaxWidth().testTag("routine-add-exercises")) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add Exercises")
                    }
                }
            }
            item {
                WhipOutlinedButton(onClick = onAddFromWorkout, modifier = Modifier.fillMaxWidth()) {
                    Text("Add from a Previous Workout")
                }
                Text(
                    "Copies the performed exercises and set details into this day as editable prescriptions; it does not alter the original workout.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }

private data class RoutinePlacementBlock(
    val groupKey: String?,
    val placements: List<RoutineBuilderPlacementState>,
) {
    val stableKey: String = groupKey?.let { "group-$it" } ?: "placement-${placements.single().key}"
}

private fun List<RoutineBuilderPlacementState>.routinePlacementBlocks(): List<RoutinePlacementBlock> {
    val emittedGroups = mutableSetOf<String>()
    return mapNotNull { placement ->
        val group = placement.groupKey ?: return@mapNotNull RoutinePlacementBlock(null, listOf(placement))
        if (!emittedGroups.add(group)) return@mapNotNull null
        RoutinePlacementBlock(group, filter { it.groupKey == group })
    }
}

@Composable
private fun RoutinePlacementCard(
    placement: RoutineBuilderPlacementState,
    selected: Boolean,
    gymState: GymUiState,
    error: String?,
    canMovePrevious: Boolean,
    canMoveNext: Boolean,
    position: Int,
    total: Int,
    grouped: Boolean = false,
    layoutScope: String,
    onOpen: () -> Unit,
    onMove: (Int) -> Unit,
    onDuplicate: () -> Unit,
    onRemove: () -> Unit,
) {
    var menu by rememberSaveable(placement.key) { mutableStateOf(false) }
    val exercise = (gymState.exercises + gymState.archivedExercises).firstOrNull { it.id == placement.exerciseId }
    val machine = (gymState.machines + gymState.archivedMachines).firstOrNull { it.id == placement.machineId }
    val border = when {
        error != null -> BorderStroke(2.dp, MaterialTheme.colorScheme.error)
        selected -> BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
        else -> null
    }
    val reorderInteraction = rememberWhipReorderInteractionState()
    Card(
        modifier = Modifier.fillMaxWidth()
            .whipReorderItem(
                reorderInteraction,
                layoutPosition = position,
                layoutScope = layoutScope,
            )
            .clickable(onClickLabel = "Edit ${exercise?.name ?: placement.exerciseNameSnapshot}", onClick = onOpen),
        border = border,
        colors = CardDefaults.cardColors(
            containerColor = if (grouped) MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
            else MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            WhipReorderHandle(
                label = exercise?.name ?: placement.exerciseNameSnapshot,
                canMovePrevious = canMovePrevious,
                canMoveNext = canMoveNext,
                position = position,
                total = total,
                interactionState = reorderInteraction,
                moveWholeItem = true,
                layoutScope = layoutScope,
                onMove = onMove,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                placement.routineRoleSummary()?.let { roleSummary ->
                    Text(
                        roleSummary,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(exercise?.name ?: placement.exerciseNameSnapshot, fontWeight = FontWeight.Bold)
                Text(
                    listOfNotNull(
                        machine?.displayName ?: placement.machineNameSnapshot.takeIf(String::isNotBlank) ?: "No Machine / Free Weights",
                        placement.sets.takeIf(List<*>::isNotEmpty)?.let { routineSetSummary(placement.sets) },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
            ItemEditButton(
                itemType = "routine exercise",
                itemName = exercise?.name ?: placement.exerciseNameSnapshot,
                onEdit = onOpen,
            )
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.semantics { contentDescription = "Manage ${exercise?.name ?: placement.exerciseNameSnapshot}" }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    WhipMenuItem(label = "Duplicate", icon = Icons.Outlined.ContentCopy, onClick = { menu = false; onDuplicate() })
                    HorizontalDivider()
                    WhipMenuItem(
                        label = "Remove",
                        icon = Icons.Outlined.Delete,
                        role = WhipMenuItemRole.Destructive,
                        onClick = { menu = false; onRemove() },
                    )
                }
            }
        }
    }
}

private fun RoutineBuilderPlacementState.routineRoleSummary(): String? = when (placementKind) {
    RoutinePlacementKind.MainLift.name -> "Program main lift"
    RoutinePlacementKind.Supplemental.name -> "Program supplemental work"
    RoutinePlacementKind.Assistance.name -> {
        val label = when (assistanceCategory) {
            RoutineAssistanceCategory.Push.name -> "Push"
            RoutineAssistanceCategory.Pull.name -> "Pull"
            RoutineAssistanceCategory.SingleLegCore.name -> "Single-leg / Core"
            RoutineAssistanceCategory.Other.name -> "Other"
            else -> "Unclassified"
        }
        val reps = plannedAssistanceReps()
        "Assistance · $label" + if (reps > 0) " · $reps planned reps" else " · rep target needs review"
    }
    else -> null
}

@Composable
private fun RoutinePlacementEditor(
    modifier: Modifier,
    placement: RoutineBuilderPlacementState,
    selectedDay: RoutineBuilderDayState,
    allDays: List<RoutineBuilderDayState>,
    gymState: GymUiState,
    showAdvanced: Boolean,
    onShowAdvanced: (Boolean) -> Unit,
    onUpdate: ((RoutineBuilderPlacementState) -> RoutineBuilderPlacementState) -> Unit,
    programKind: String?,
    programPhaseCount: Int,
    programPhaseLabels: List<String>,
    onApplyFiveThreeOne: (FiveThreeOneBuilderResult) -> Unit,
    onCreateFiveThreeOneProgram: () -> Unit,
    onUpdateDay: ((RoutineBuilderDayState) -> RoutineBuilderDayState) -> Unit,
    onChooseEquipment: () -> Unit,
    onMoveToDay: (Long, Boolean) -> Unit,
    dialogModifier: Modifier,
    onSavePrescriptionScheme: (RepPrescriptionScheme) -> Unit,
    onReorderPrescriptionSchemes: (List<RepPrescriptionScheme>) -> Unit,
    onDeletePrescriptionScheme: (String) -> Unit,
) {
    val exercise = (gymState.exercises + gymState.archivedExercises).firstOrNull { it.id == placement.exerciseId }
    val machine = (gymState.machines + gymState.archivedMachines).firstOrNull { it.id == placement.machineId }
    var dayMenu by rememberSaveable(placement.key) { mutableStateOf(false) }
    var copyDayMenu by rememberSaveable(placement.key) { mutableStateOf(false) }
    var showSchemeEditor by rememberSaveable(placement.key) { mutableStateOf(false) }
    var editingSchemeId by rememberSaveable(placement.key) { mutableStateOf<String?>(null) }
    var pendingDeleteSchemeId by rememberSaveable(placement.key) { mutableStateOf<String?>(null) }
    var alternativeQuery by rememberSaveable(placement.key) { mutableStateOf("") }
    var showFiveThreeOneBuilder by rememberSaveable(placement.key) {
        mutableStateOf(false)
    }
    var visibleProgramPhase by rememberSaveable(placement.key) { mutableStateOf(0) }
    LaunchedEffect(programPhaseCount) {
        visibleProgramPhase = visibleProgramPhase.coerceIn(0, (programPhaseCount - 1).coerceAtLeast(0))
    }
    val supportsFiveThreeOne = exercise?.trackingType == ExerciseTrackingType.WeightReps &&
        (machine == null || machine.loadType == MachineLoadType.Mass)
    val programUnitId = when {
        machine?.loadType == MachineLoadType.Mass -> machine.unitId
        else -> exercise?.weightUnitId ?: gymState.appSettings.gymWeightUnitId
    }.ifBlank { gymState.appSettings.gymWeightUnitId }
    val programIncrement = exercise?.weightIncrement?.takeIf {
        it > 0.0 && programUnitId == exercise.weightUnitId
    } ?: if (programUnitId == "pound") 5.0 else 2.5
    val programAvailableLoads = machine?.availableLoads.orEmpty().takeIf {
        machine?.loadType == MachineLoadType.Mass
    }.orEmpty()
    val currentEstimatedOneRepMax = currentEstimatedOneRepMaxKg(gymState, placement.exerciseId, machine)
        ?.let { massFromKilograms(it, programUnitId) }
    val suggestedTrainingMax = currentEstimatedOneRepMax?.let { estimate ->
        runCatching {
            suggestedFiveThreeOneTrainingMax(estimate, programIncrement, availableLoads = programAvailableLoads)
        }.getOrNull()
    }
    val isStructuredFiveThreeOne = programKind.isFiveThreeOneProgramKindName()
    val legacySingleLiftConversionCompatible = allDays.size == 1
    val placementKind = runCatching { RoutinePlacementKind.valueOf(placement.placementKind) }
        .getOrDefault(
            if (placement.assistanceRole == RoutineAssistanceRole.MainLift.name) {
                RoutinePlacementKind.MainLift
            } else if (placement.assistanceRole != RoutineAssistanceRole.Unspecified.name) {
                RoutinePlacementKind.Assistance
            } else {
                RoutinePlacementKind.General
            },
        )
    val hasTrainingMaxPrescription = placement.sets.any {
        it.loadPrescriptionType == RoutineLoadPrescriptionType.PercentTrainingMax.name
    }
    val supportsTrainingMax = exercise?.trackingType in setOf(
        ExerciseTrackingType.WeightReps,
        ExerciseTrackingType.WeightOnly,
        ExerciseTrackingType.WeightDuration,
    ) && machine?.loadType != MachineLoadType.Level &&
        (machine?.loadInterpretation ?: exercise?.loadInterpretation ?: LoadInterpretation.Total)
            .supportsRoutinePercentagePrescription()
    val trainingMaxSource = runCatching { RoutineTrainingMaxSource.valueOf(placement.trainingMaxSource) }
        .getOrDefault(RoutineTrainingMaxSource.EstimatedOneRepMaxPercent)
    val trainingMaxPercentage = placement.trainingMaxPercent.toWhipDoubleOrNull()
    val derivedTrainingMax = if (currentEstimatedOneRepMax != null && trainingMaxPercentage != null) {
        runCatching {
            suggestedFiveThreeOneTrainingMax(
                currentEstimatedOneRepMax,
                programIncrement,
                trainingMaxPercentage,
                programAvailableLoads,
            )
        }.getOrNull()
    } else null
    var trainingMaxExpanded by rememberSaveable(placement.key) {
        mutableStateOf(hasTrainingMaxPrescription || placement.trainingMaxValue.isNotBlank())
    }
    LaunchedEffect(hasTrainingMaxPrescription) {
        if (hasTrainingMaxPrescription) trainingMaxExpanded = true
    }
    WhipReorderLazyColumn(
        modifier = modifier.testTag("routine-placement-editor"),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(exercise?.name ?: placement.exerciseNameSnapshot, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(exercise?.trackingType?.label.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            WhipOutlinedButton(onClick = onChooseEquipment, modifier = Modifier.fillMaxWidth().testTag("routine-equipment-picker")) {
                Text("Equipment · ${machine?.displayName ?: placement.machineNameSnapshot.takeIf(String::isNotBlank) ?: "No Machine / Free Weights"}")
            }
            if (placement.equipmentBindingState == RoutineEquipmentBindingState.NeedsEquipment.name) {
                Text("This placement needs compatible equipment before the routine can start.", color = MaterialTheme.colorScheme.error)
            }
        }
        item {
            OutlinedTextField(
                placement.notes,
                { value -> onUpdate { it.copy(notes = value) } },
                label = { Text("Exercise notes") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
            )
        }
        if (placementKind == RoutinePlacementKind.MainLift) item {
            OutlinedCard(Modifier.fillMaxWidth().testTag("routine-main-lift-provenance")) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Program main lift", fontWeight = FontWeight.Bold)
                    Text(
                        "This role came from the 5/3/1 program setup. Change main-lift structure in Program Structure; routine assistance controls cannot promote or demote a main lift.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else if (placementKind == RoutinePlacementKind.Supplemental) item {
            OutlinedCard(Modifier.fillMaxWidth().testTag("routine-supplemental-provenance")) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Program supplemental lift", fontWeight = FontWeight.Bold)
                    Text(
                        "This lift is programmed separately from the day's Main lift and uses its own Training Max. Change the mapping by rebuilding the preset or edit its executable sets below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else if (isStructuredFiveThreeOne || placementKind == RoutinePlacementKind.Assistance) item {
            val selectedCategory = runCatching {
                RoutineAssistanceCategory.valueOf(placement.assistanceCategory)
            }.getOrDefault(RoutineAssistanceCategory.Unspecified)
            Text("Role in this routine", style = MaterialTheme.typography.labelLarge)
            Text(
                "You assign this for ${selectedDay.name}. It does not come from—or change—the exercise's Library categories, muscles, or equipment.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    RoutineAssistanceCategory.Unspecified to "General",
                    RoutineAssistanceCategory.Push to "Push",
                    RoutineAssistanceCategory.Pull to "Pull",
                    RoutineAssistanceCategory.SingleLegCore to "Single-leg / Core",
                    RoutineAssistanceCategory.Other to "Other assistance",
                ).forEach { (category, label) ->
                    WhipFilterChip(
                        selected = if (category == RoutineAssistanceCategory.Unspecified) {
                            placementKind == RoutinePlacementKind.General
                        } else {
                            placementKind == RoutinePlacementKind.Assistance && selectedCategory == category
                        },
                        onClick = {
                            onUpdate { current ->
                                current.withAssistanceCategory(category.takeUnless {
                                    it == RoutineAssistanceCategory.Unspecified
                                })
                            }
                        },
                        label = { Text(label) },
                        modifier = Modifier.testTag("routine-assistance-category-${category.name}"),
                    )
                }
            }
        }
        if (
            supportsFiveThreeOne && isStructuredFiveThreeOne &&
            placementKind == RoutinePlacementKind.MainLift
        ) item {
            OutlinedCard(Modifier.fillMaxWidth().testTag("routine-five-three-one-program-controlled")) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Program-controlled main work", fontWeight = FontWeight.Bold)
                    Text(
                        "Use Program Structure to edit Training Maxes, cycle increases, phases, PR sets, 5s PRO, supplemental work, and optional Jokers. Toggling a Joker preserves your Main and Supplemental set details; choosing a different Main or Supplemental scheme intentionally regenerates that section.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (supportsFiveThreeOne && !isStructuredFiveThreeOne && legacySingleLiftConversionCompatible) item {
            WhipOutlinedButton(
                onClick = { showFiveThreeOneBuilder = !showFiveThreeOneBuilder },
                modifier = Modifier.fillMaxWidth().testTag("routine-five-three-one-toggle"),
            ) {
                Text(if (showFiveThreeOneBuilder) "Hide 5/3/1 Cycle Generator" else "Generate a 5/3/1 Cycle for This Lift")
            }
            if (showFiveThreeOneBuilder) {
                Text(
                    "This converts the current routine into a canonical four-phase 5/3/1 cycle. Use Set Up 5/3/1 from an empty routine to choose several standard or custom lifts at once.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FiveThreeOneBuilder(
                    placementKey = placement.key,
                    exerciseName = exercise.name,
                    currentSets = placement.sets,
                    unitId = programUnitId,
                    increment = programIncrement,
                    availableLoads = programAvailableLoads,
                    suggestedTrainingMax = suggestedTrainingMax,
                    initialTrainingMax = placement.trainingMaxValue.toWhipDoubleOrNull(),
                    initialCycleIncrement = placement.cycleIncrementValue.toWhipDoubleOrNull(),
                    initialProgramKind = programKind?.let { runCatching { RoutineProgramKind.valueOf(it) }.getOrNull() },
                    initialMainWorkScheme = runCatching { RoutineMainWorkScheme.valueOf(placement.mainWorkScheme) }
                        .getOrDefault(RoutineMainWorkScheme.Unspecified),
                    initialSupplementalScheme = runCatching { RoutineSupplementalScheme.valueOf(placement.supplementalScheme) }
                        .getOrDefault(RoutineSupplementalScheme.None),
                    initialJokerSetsEnabled = placement.jokerSetsEnabled,
                    onApply = onApplyFiveThreeOne,
                )
            }
        }
        if (supportsFiveThreeOne && !isStructuredFiveThreeOne && !legacySingleLiftConversionCompatible) item {
            OutlinedCard(Modifier.fillMaxWidth().testTag("routine-five-three-one-whole-program-required")) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Build the whole 5/3/1 program", fontWeight = FontWeight.Bold)
                    Text(
                        "This routine has several days. Converting only this lift would leave other days without required Main work and block Training Max progression. Review a complete standard or custom-lift program instead.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    WhipOutlinedButton(
                        onClick = onCreateFiveThreeOneProgram,
                        modifier = Modifier.fillMaxWidth().testTag("routine-five-three-one-replace-with-program"),
                    ) { Text("Set Up Complete 5/3/1 Program") }
                }
            }
        }
        if (exercise?.supportsRepPrescription() != false) item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Saved Schemes · App-wide", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { editingSchemeId = null; showSchemeEditor = true },
                    modifier = Modifier.testTag("routine-add-rep-scheme").semantics { contentDescription = "Add rep prescription scheme" },
                ) { Icon(Icons.Filled.Add, contentDescription = null) }
            }
            if (gymState.appSettings.repPrescriptionSchemes.isEmpty()) {
                Text(
                    "No saved schemes yet. Tap + to add one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("routine-rep-schemes-empty"),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    gymState.appSettings.repPrescriptionSchemes.forEachIndexed { index, scheme ->
                        val reorderInteraction = rememberWhipReorderInteractionState()
                        Row(
                            modifier = Modifier.whipReorderItem(
                                reorderInteraction,
                                layoutPosition = index + 1,
                                layoutScope = "routine-prescription-schemes",
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            WhipReorderHandle(
                                label = "${scheme.name.ifBlank { "rep prescription" }} scheme",
                                canMovePrevious = index > 0,
                                canMoveNext = index < gymState.appSettings.repPrescriptionSchemes.lastIndex,
                                position = index + 1,
                                total = gymState.appSettings.repPrescriptionSchemes.size,
                                interactionState = reorderInteraction,
                                moveWholeItem = true,
                                layoutScope = "routine-prescription-schemes",
                                onMove = { delta ->
                                    onReorderPrescriptionSchemes(
                                        moveListItem(gymState.appSettings.repPrescriptionSchemes, index, delta),
                                    )
                                },
                            )
                            Box(Modifier.weight(1f)) {
                                RepPrescriptionSchemeRow(
                                    scheme = scheme,
                                    onApply = { onUpdate { current -> current.copy(sets = applyRepPrescriptionScheme(current.sets, scheme)) } },
                                    onEdit = { editingSchemeId = scheme.id; showSchemeEditor = true },
                                    onDelete = { pendingDeleteSchemeId = scheme.id },
                                )
                            }
                        }
                    }
                }
                Text(
                    "Apply a saved prescription here. Its name and order are shared across every Routine; editing or deleting it never changes Routines already using it.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (exercise?.trackingType in setOf(ExerciseTrackingType.WeightReps, ExerciseTrackingType.WeightOnly, ExerciseTrackingType.WeightDuration)) {
            item {
                val workingLoad = placement.sets.firstOrNull { it.classification != WorkoutSetClassification.WarmUp.name }
                    ?.load?.toWhipDoubleOrNull()
                WhipOutlinedButton(
                    enabled = workingLoad != null && workingLoad > 0.0,
                    onClick = {
                        onUpdate { current ->
                            current.copy(sets = generateWarmupSets(current, requireNotNull(exercise), machine))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("routine-generate-warmups"),
                ) { Text("Generate Equipment-Aware Warm-Ups") }
                Text(
                    if (workingLoad == null) "Enter the first working load, then Whip can add 40%, 60%, and 80% ramp sets."
                    else "Loads snap to this exercise's increment or the selected machine's available settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            RoutineLabeledSwitchRow(
                label = "Show Advanced Prescription Fields",
                checked = showAdvanced,
                onCheckedChange = onShowAdvanced,
                testTag = "routine-show-advanced",
            )
            if (showAdvanced) {
                DependentSettingsNotice(
                    message = "RPE, RIR, rest, tempo, notes, and unilateral controls are shown inside every set below.",
                    testTag = "routine-advanced-consequence",
                )
            }
            RoutineLabeledSwitchRow(
                label = "Copy Previous Values When No Plan",
                checked = placement.copyPreviousWorkout,
                onCheckedChange = { checked -> onUpdate { it.copy(copyPreviousWorkout = checked) } },
                testTag = "routine-copy-previous",
            )
            if (placement.copyPreviousWorkout) {
                Text(
                    "Unplanned fields start with values from the previous workout.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (supportsTrainingMax && !isStructuredFiveThreeOne) {
            item {
                val summary = when (trainingMaxSource) {
                    RoutineTrainingMaxSource.Explicit -> placement.trainingMaxValue.toWhipDoubleOrNull()?.let { value ->
                        "Explicit · ${editableNumericValue(value)} ${unitSymbol(programUnitId)}"
                    } ?: "Optional · not set"
                    RoutineTrainingMaxSource.EstimatedOneRepMaxPercent -> derivedTrainingMax?.let { value ->
                        "Derived · ${editableNumericValue(trainingMaxPercentage ?: 90.0)}% of current e1RM · ${editableNumericValue(value)} ${unitSymbol(programUnitId)}"
                    } ?: "Optional · needs an estimated 1RM or an explicit value"
                }
                DisclosureRow(
                    title = "Training Max & Percentage Loads",
                    supportingText = summary,
                    expanded = trainingMaxExpanded,
                    onClick = { trainingMaxExpanded = !trainingMaxExpanded },
                    modifier = Modifier.testTag("routine-training-max-disclosure"),
                )
            }
            if (trainingMaxExpanded) item {
                OutlinedCard(Modifier.fillMaxWidth().testTag("routine-training-max-section")) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Training Max", fontWeight = FontWeight.SemiBold)
                        Text(
                            "This belongs to this exercise in this routine. Sets prescribed as % of Training Max use it; it is separate from your actual or estimated 1RM.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            WhipFilterChip(
                                selected = trainingMaxSource == RoutineTrainingMaxSource.Explicit,
                                onClick = {
                                    onUpdate {
                                        it.copy(
                                            trainingMaxSource = RoutineTrainingMaxSource.Explicit.name,
                                            trainingMaxUnitId = programUnitId,
                                            trainingMaxBasisKind = TrainingMaxBasisKind.ExplicitTrainingMax.name,
                                            trainingMaxBasisValue = it.trainingMaxValue,
                                            trainingMaxBasisUnitId = programUnitId,
                                        )
                                    }
                                },
                                label = { Text("Enter Training Max") },
                                modifier = Modifier.testTag("routine-training-max-source-explicit"),
                            )
                            WhipFilterChip(
                                selected = trainingMaxSource == RoutineTrainingMaxSource.EstimatedOneRepMaxPercent,
                                onClick = {
                                    onUpdate {
                                        it.copy(
                                            trainingMaxSource = RoutineTrainingMaxSource.EstimatedOneRepMaxPercent.name,
                                            trainingMaxValue = "",
                                            trainingMaxUnitId = programUnitId,
                                            trainingMaxBasisKind = TrainingMaxBasisKind.EstimatedOneRepMax.name,
                                            trainingMaxBasisValue = currentEstimatedOneRepMax?.let(::editableNumericValue).orEmpty(),
                                            trainingMaxBasisUnitId = programUnitId.takeIf { currentEstimatedOneRepMax != null }.orEmpty(),
                                            cycleIncrementValue = "",
                                        )
                                    }
                                },
                                label = { Text("Derive from estimated 1RM") },
                                modifier = Modifier.testTag("routine-training-max-source-derived"),
                            )
                        }
                        if (trainingMaxSource == RoutineTrainingMaxSource.Explicit) {
                            OutlinedTextField(
                                placement.trainingMaxValue,
                                { value ->
                                    onUpdate {
                                        it.copy(
                                            trainingMaxValue = value.numericInput(),
                                            trainingMaxUnitId = programUnitId,
                                            trainingMaxSource = RoutineTrainingMaxSource.Explicit.name,
                                            trainingMaxBasisKind = TrainingMaxBasisKind.ExplicitTrainingMax.name,
                                            trainingMaxBasisValue = value.numericInput(),
                                            trainingMaxBasisUnitId = programUnitId,
                                        )
                                    }
                                },
                                label = { Text("Training Max (${unitSymbol(programUnitId)})") },
                                supportingText = { Text("Enter the exact Training Max this routine should use.") },
                                modifier = Modifier.fillMaxWidth().testTag("routine-training-max-value"),
                                singleLine = true,
                            )
                            if (derivedTrainingMax != null) {
                                WhipOutlinedButton(
                                    onClick = {
                                        onUpdate {
                                            it.copy(
                                                trainingMaxValue = editableNumericValue(derivedTrainingMax),
                                                trainingMaxUnitId = programUnitId,
                                                trainingMaxSource = RoutineTrainingMaxSource.Explicit.name,
                                                trainingMaxBasisKind = TrainingMaxBasisKind.EstimatedOneRepMax.name,
                                                trainingMaxBasisValue = currentEstimatedOneRepMax?.let(::editableNumericValue).orEmpty(),
                                                trainingMaxBasisUnitId = programUnitId,
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("routine-training-max-use-suggestion"),
                                ) {
                                    Text("Use ${editableNumericValue(derivedTrainingMax)} ${unitSymbol(programUnitId)} from current e1RM")
                                }
                            }
                        } else {
                            OutlinedTextField(
                                placement.trainingMaxPercent,
                                { value -> onUpdate { it.copy(trainingMaxPercent = value.numericInput()) } },
                                label = { Text("Training Max (% of estimated 1RM)") },
                                supportingText = { Text("Recalculates from the current equipment-specific e1RM when the routine starts.") },
                                modifier = Modifier.fillMaxWidth().testTag("routine-training-max-percent"),
                                singleLine = true,
                            )
                            if (derivedTrainingMax == null) {
                                Text(
                                    "No current estimated 1RM exists for this exercise and equipment. Record one first or enter an explicit Training Max.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.testTag("routine-training-max-missing-estimate"),
                                )
                            } else {
                                Text(
                                    "Current derived Training Max · ${editableNumericValue(derivedTrainingMax)} ${unitSymbol(programUnitId)} from ${editableNumericValue(requireNotNull(currentEstimatedOneRepMax))} ${unitSymbol(programUnitId)} e1RM",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("routine-training-max-derived-value"),
                                )
                            }
                        }
                    }
                }
            }
        }
        val hasProgramPhases = placement.sets.any { it.routinePhaseIndex != null }
        if (hasProgramPhases) item {
            Text("Edit Program Phase", fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                List(programPhaseCount) { index ->
                    programPhaseLabels.getOrNull(index)?.takeIf(String::isNotBlank) ?: "Phase ${index + 1}"
                }.forEachIndexed { index, phaseLabel ->
                    WhipFilterChip(
                        selected = visibleProgramPhase == index,
                        onClick = { visibleProgramPhase = index },
                        label = { Text(phaseLabel) },
                        modifier = Modifier.testTag("routine-program-phase-$index"),
                    )
                }
            }
            Text(
                "Showing this phase plus supplemental sets that apply in every phase.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val visibleSets = placement.sets.filter { set ->
            !hasProgramPhases || set.routinePhaseIndex == null || set.routinePhaseIndex == visibleProgramPhase
        }
        items(visibleSets.size, key = { visibleSets[it].key }) { setIndex ->
            val set = visibleSets[setIndex]
            RoutineSetEditorCard(
                set = set,
                exercise = exercise,
                machine = machine,
                showAdvanced = showAdvanced,
                canMovePrevious = setIndex > 0,
                canMoveNext = setIndex < visibleSets.lastIndex,
                position = setIndex + 1,
                total = visibleSets.size,
                layoutScope = "routine-placement-${placement.key}-sets",
                onMove = { delta ->
                    val target = visibleSets.getOrNull(setIndex + delta)
                    if (target != null) onUpdate { current -> current.copy(sets = swapRoutineBuilderSets(current.sets, set.key, target.key)) }
                },
                onUpdate = { transform -> onUpdate { current -> current.copy(sets = current.sets.map { if (it.key == set.key) transform(it) else it }) } },
                onDuplicate = { onUpdate { current -> current.copy(sets = current.sets + set.copy(key = nextLocalSetKey(current.sets))) } },
                onDelete = { onUpdate { current -> current.copy(sets = current.sets.filterNot { it.key == set.key }) } },
            )
        }
        item {
            WhipOutlinedButton(
                onClick = {
                    onUpdate { current ->
                        current.copy(
                            sets = current.sets + RoutineBuilderSetState(
                                key = nextLocalSetKey(current.sets),
                                routinePhaseIndex = visibleProgramPhase.takeIf { hasProgramPhases },
                                workSection = when (placementKind) {
                                    RoutinePlacementKind.Assistance -> RoutineWorkSection.Assistance.name
                                    RoutinePlacementKind.MainLift -> RoutineWorkSection.Optional.name
                                    RoutinePlacementKind.Supplemental -> RoutineWorkSection.Supplemental.name
                                    RoutinePlacementKind.General -> RoutineWorkSection.Unspecified.name
                                },
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Add Set") }
        }
        if (showAdvanced) {
            item {
                val programmed = programKind?.let { kind ->
                    runCatching { RoutineProgramKind.valueOf(kind) }.getOrDefault(RoutineProgramKind.Static)
                }?.let { it != RoutineProgramKind.Static } == true
                Text("Training Cycle", fontWeight = FontWeight.SemiBold)
                if (programmed) {
                    Text(
                        "Cycle progression is controlled by the program phases above; per-exercise multipliers do not apply to programmed routines.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("routine-cycle-program-controlled"),
                    )
                } else {
                    OutlinedTextField(
                        placement.progressionPercentages,
                        { value -> onUpdate { it.copy(progressionPercentages = value.filter { char -> char.isDigit() || char in ".,- " }) } },
                        label = { Text("Cycle load multipliers (%)") },
                        supportingText = { Text("Comma-separated workouts, for example 100, 102.5, 105, 90. The next workout advances to the next value.") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        if (showAdvanced) item {
            Text("Planned Alternatives", fontWeight = FontWeight.SemiBold)
            Text(
                "These stay attached to the routine and are offered first when substituting during a workout.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                alternativeQuery,
                { alternativeQuery = it },
                label = { Text("Find alternative exercises") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                gymState.exercises.asSequence()
                    .filter { it.id != placement.exerciseId }
                    .filter { alternativeQuery.isBlank() || it.name.contains(alternativeQuery.trim(), true) }
                    .sortedWith(compareByDescending<Exercise> { it.id in placement.alternativeExerciseIds }.thenBy { it.name.lowercase() })
                    .take(24)
                    .forEach { candidate ->
                        WhipFilterChip(
                            selected = candidate.id in placement.alternativeExerciseIds,
                            onClick = {
                                onUpdate { current ->
                                    current.copy(
                                        alternativeExerciseIds = if (candidate.id in current.alternativeExerciseIds) {
                                            current.alternativeExerciseIds - candidate.id
                                        } else current.alternativeExerciseIds + candidate.id,
                                    )
                                }
                            },
                            label = { Text(candidate.name) },
                        )
                    }
            }
        }
        item {
            Text("Superset", fontWeight = FontWeight.SemiBold)
            placement.groupKey?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                selectedDay.placements.filterNot { it.key == placement.key }.forEach { other ->
                    val otherExercise = gymState.exercises.firstOrNull { it.id == other.exerciseId }
                    WhipFilterChip(
                        selected = placement.groupKey != null && placement.groupKey == other.groupKey,
                        onClick = {
                            onUpdateDay { day ->
                                if (placement.groupKey != null && placement.groupKey == other.groupKey) {
                                    day.removePlacementFromGroup(placement.key)
                                } else {
                                    day.groupPlacements(placement.key, other.key)
                                }
                            }
                        },
                        label = { Text(otherExercise?.name ?: other.exerciseNameSnapshot) },
                    )
                }
                if (placement.groupKey != null) WhipFilterChip(selected = false, onClick = {
                    onUpdateDay { day -> day.removePlacementFromGroup(placement.key) }
                }, label = { Text("Remove from group") })
            }
        }
        if (allDays.size > 1) item {
            Text("Move or Copy", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    WhipOutlinedButton(onClick = { dayMenu = true }) { Text("Move to Day") }
                    DropdownMenu(expanded = dayMenu, onDismissRequest = { dayMenu = false }) {
                        allDays.filterNot { it.key == selectedDay.key }.forEach { day ->
                            DropdownMenuItem(text = { Text(day.name) }, onClick = { dayMenu = false; onMoveToDay(day.key, false) })
                        }
                    }
                }
                Box {
                    WhipOutlinedButton(onClick = { copyDayMenu = true }) { Text("Copy to Day") }
                    DropdownMenu(expanded = copyDayMenu, onDismissRequest = { copyDayMenu = false }) {
                        allDays.filterNot { it.key == selectedDay.key }.forEach { day ->
                            DropdownMenuItem(text = { Text(day.name) }, onClick = { copyDayMenu = false; onMoveToDay(day.key, true) })
                        }
                    }
                }
            }
        }
    }

    if (showSchemeEditor) {
        RepPrescriptionSchemeDialog(
            modifier = dialogModifier,
            scheme = gymState.appSettings.repPrescriptionSchemes.firstOrNull { it.id == editingSchemeId },
            onDismiss = { showSchemeEditor = false; editingSchemeId = null },
            onSave = { scheme ->
                onSavePrescriptionScheme(scheme)
                showSchemeEditor = false
                editingSchemeId = null
            },
        )
    }
    pendingDeleteSchemeId?.let { schemeId ->
        val scheme = gymState.appSettings.repPrescriptionSchemes.firstOrNull { it.id == schemeId }
        if (scheme != null) {
            PaneAwareAlertDialog(
                modifier = dialogModifier,
                onDismissRequest = { pendingDeleteSchemeId = null },
                title = { Text("Delete ${scheme.displayLabel}?") },
                text = { Text("This removes the saved shortcut. Existing routine prescriptions stay unchanged.") },
                confirmButton = {
                    WhipTextButton(onClick = { onDeletePrescriptionScheme(scheme.id); pendingDeleteSchemeId = null }) {
                        Text("Delete Scheme", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = { WhipTextButton(onClick = { pendingDeleteSchemeId = null }) { Text("Cancel") } },
            )
        }
    }
}

@Composable
private fun RepPrescriptionSchemeRow(
    scheme: RepPrescriptionScheme,
    onApply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    OutlinedCard(Modifier.fillMaxWidth().testTag("routine-rep-scheme-${scheme.id}")) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WhipTextButton(
                onClick = onApply,
                modifier = Modifier.weight(1f).semantics { contentDescription = "Apply ${scheme.displayLabel}" },
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Text(scheme.displayLabel, fontWeight = FontWeight.SemiBold)
                    Text(
                        buildString {
                            append(scheme.classification.uiLabel())
                            scheme.restSeconds?.let { append(" · ${it}s rest") }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(
                onClick = onEdit,
                modifier = Modifier.semantics { contentDescription = "Edit ${scheme.displayLabel}" },
            ) { Icon(Icons.Outlined.Edit, contentDescription = null) }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.semantics { contentDescription = "Delete ${scheme.displayLabel}" },
            ) { Icon(Icons.Outlined.Delete, contentDescription = null) }
        }
    }
}

@Composable
private fun RepPrescriptionSchemeDialog(
    modifier: Modifier,
    scheme: RepPrescriptionScheme?,
    onDismiss: () -> Unit,
    onSave: (RepPrescriptionScheme) -> Unit,
) {
    val editorKey = scheme?.id ?: "new-rep-scheme"
    val schemeId = rememberSaveable(editorKey) { scheme?.id ?: UUID.randomUUID().toString() }
    var name by rememberSaveable(editorKey) { mutableStateOf(scheme?.name.orEmpty()) }
    var setCount by rememberSaveable(editorKey) { mutableStateOf(scheme?.setCount?.toString().orEmpty()) }
    var repetitionsMin by rememberSaveable(editorKey) { mutableStateOf(scheme?.repetitionsMin?.toString().orEmpty()) }
    var repetitionsMax by rememberSaveable(editorKey) { mutableStateOf(scheme?.repetitionsMax?.toString().orEmpty()) }
    var classificationName by rememberSaveable(editorKey) {
        mutableStateOf((scheme?.classification ?: WorkoutSetClassification.Working).name)
    }
    var restSeconds by rememberSaveable(editorKey) { mutableStateOf(scheme?.restSeconds?.toString().orEmpty()) }
    var classificationMenu by rememberSaveable(editorKey) { mutableStateOf(false) }
    val classification = runCatching { WorkoutSetClassification.valueOf(classificationName) }
        .getOrDefault(WorkoutSetClassification.Working)
    val candidate = RepPrescriptionScheme(
        id = schemeId,
        name = name.trim(),
        setCount = setCount.toIntOrNull() ?: 0,
        repetitionsMin = repetitionsMin.toIntOrNull() ?: 0,
        repetitionsMax = repetitionsMax.toIntOrNull() ?: 0,
        classification = classification,
        restSeconds = restSeconds.takeIf(String::isNotBlank)?.toIntOrNull(),
    )
    val canSave = candidate.isValid() && (restSeconds.isBlank() || restSeconds.toIntOrNull() != null)

    PaneAwareAlertDialog(
        modifier = modifier.testTag("rep-scheme-editor"),
        onDismissRequest = onDismiss,
        title = { Text(if (scheme == null) "Add Rep Prescription Scheme" else "Edit Rep Prescription Scheme") },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Save a reusable set-and-rep shortcut. The name is optional.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    name,
                    { name = it.take(60) },
                    label = { Text("Name (optional)") },
                    placeholder = { Text("Heavy, Hypertrophy…") },
                    modifier = Modifier.fillMaxWidth().testTag("rep-scheme-name"),
                    singleLine = true,
                )
                OutlinedTextField(
                    setCount,
                    { setCount = it.filter(Char::isDigit).take(3) },
                    label = { Text("Number of sets") },
                    supportingText = { Text("1–100") },
                    modifier = Modifier.fillMaxWidth().testTag("rep-scheme-set-count"),
                    singleLine = true,
                    isError = setCount.isNotEmpty() && setCount.toIntOrNull() !in 1..100,
                )
                OutlinedTextField(
                    repetitionsMin,
                    { repetitionsMin = it.filter(Char::isDigit).take(4) },
                    label = { Text("Minimum reps") },
                    modifier = Modifier.fillMaxWidth().testTag("rep-scheme-reps-min"),
                    singleLine = true,
                )
                OutlinedTextField(
                    repetitionsMax,
                    { repetitionsMax = it.filter(Char::isDigit).take(4) },
                    label = { Text("Maximum reps") },
                    supportingText = { Text("Use the same number for an exact rep target.") },
                    modifier = Modifier.fillMaxWidth().testTag("rep-scheme-reps-max"),
                    singleLine = true,
                    isError = repetitionsMax.isNotEmpty() && (
                        repetitionsMax.toIntOrNull() !in 1..1000 ||
                            (repetitionsMin.toIntOrNull()?.let { min -> repetitionsMax.toIntOrNull()?.let { it < min } } == true)
                    ),
                )
                Box(Modifier.fillMaxWidth()) {
                    WhipOutlinedButton(
                        onClick = { classificationMenu = true },
                        modifier = Modifier.fillMaxWidth().testTag("rep-scheme-classification"),
                    ) { Text("Set Type · ${classification.uiLabel()}") }
                    DropdownMenu(expanded = classificationMenu, onDismissRequest = { classificationMenu = false }) {
                        WorkoutSetClassification.entries.forEach { value ->
                            DropdownMenuItem(
                                text = { Text(value.uiLabel()) },
                                onClick = { classificationName = value.name; classificationMenu = false },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    restSeconds,
                    { restSeconds = it.filter(Char::isDigit).take(5) },
                    label = { Text("Rest seconds (optional)") },
                    supportingText = { Text("Leave blank to keep each set's existing rest target.") },
                    modifier = Modifier.fillMaxWidth().testTag("rep-scheme-rest"),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            WhipButton(
                onClick = { onSave(candidate) },
                enabled = canSave,
                modifier = Modifier.testTag("rep-scheme-save"),
            ) { Text(if (scheme == null) "Add Scheme" else "Save Changes") }
        },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RoutineSetEditorCard(
    set: RoutineBuilderSetState,
    exercise: Exercise?,
    machine: GymMachine?,
    showAdvanced: Boolean,
    canMovePrevious: Boolean,
    canMoveNext: Boolean,
    position: Int,
    total: Int,
    layoutScope: String,
    onMove: (Int) -> Unit,
    onUpdate: ((RoutineBuilderSetState) -> RoutineBuilderSetState) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var classificationMenu by rememberSaveable(set.key) { mutableStateOf(false) }
    val reorderInteraction = rememberWhipReorderInteractionState()
    @Composable
    fun ClassificationSelector(modifier: Modifier = Modifier, fillWidth: Boolean = false) {
        Box(modifier) {
            WhipTextButton(
                onClick = { classificationMenu = true },
                modifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier,
            ) {
                Text(set.classification.workoutSetClassificationLabel())
            }
            DropdownMenu(expanded = classificationMenu, onDismissRequest = { classificationMenu = false }) {
                WorkoutSetClassification.entries.forEach { value ->
                    DropdownMenuItem(
                        text = { Text(value.uiLabel()) },
                        onClick = {
                            classificationMenu = false
                            onUpdate { it.copy(classification = value.name) }
                        },
                    )
                }
            }
        }
    }
    OutlinedCard(
        Modifier.fillMaxWidth().whipReorderItem(
            reorderInteraction,
            layoutPosition = position,
            layoutScope = layoutScope,
        ),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val stackHeader = LocalDensity.current.fontScale >= 1.5f || maxWidth < 330.dp
                if (stackHeader) {
                    Column(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            WhipReorderHandle(
                                label = "planned set",
                                canMovePrevious = canMovePrevious,
                                canMoveNext = canMoveNext,
                                position = position,
                                total = total,
                                interactionState = reorderInteraction,
                                moveWholeItem = true,
                                layoutScope = layoutScope,
                                onMove = onMove,
                            )
                            Text(
                                "Set $position",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            IconButton(onClick = onDuplicate, modifier = Modifier.semantics { contentDescription = "Duplicate set $position" }) { Icon(Icons.Outlined.ContentCopy, null) }
                            IconButton(onClick = onDelete, modifier = Modifier.semantics { contentDescription = "Delete set $position" }) { Icon(Icons.Outlined.Delete, null) }
                        }
                        ClassificationSelector(Modifier.fillMaxWidth(), fillWidth = true)
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        WhipReorderHandle(
                            label = "planned set",
                            canMovePrevious = canMovePrevious,
                            canMoveNext = canMoveNext,
                            position = position,
                            total = total,
                            interactionState = reorderInteraction,
                            moveWholeItem = true,
                            layoutScope = layoutScope,
                            onMove = onMove,
                        )
                        ClassificationSelector()
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onDuplicate, modifier = Modifier.semantics { contentDescription = "Duplicate set $position" }) { Icon(Icons.Outlined.ContentCopy, null) }
                        IconButton(onClick = onDelete, modifier = Modifier.semantics { contentDescription = "Delete set $position" }) { Icon(Icons.Outlined.Delete, null) }
                    }
                }
            }
            val needsLoad = exercise?.trackingType in setOf(
                ExerciseTrackingType.WeightReps,
                ExerciseTrackingType.WeightOnly,
                ExerciseTrackingType.WeightDuration,
                ExerciseTrackingType.BodyweightReps,
                ExerciseTrackingType.AssistedBodyweightReps,
            )
            if (needsLoad) {
                val prescriptionType = runCatching { RoutineLoadPrescriptionType.valueOf(set.loadPrescriptionType) }
                    .getOrDefault(RoutineLoadPrescriptionType.Absolute)
                val interpretation = machine?.loadInterpretation ?: exercise?.loadInterpretation ?: LoadInterpretation.Total
                val percentageInterpretationSupported = interpretation.supportsRoutinePercentagePrescription()
                val estimatedOneRepMaxSupported = percentageInterpretationSupported &&
                    exercise?.trackingType == ExerciseTrackingType.WeightReps
                val trainingMaxSupported = percentageInterpretationSupported && exercise?.trackingType in setOf(
                    ExerciseTrackingType.WeightReps,
                    ExerciseTrackingType.WeightOnly,
                    ExerciseTrackingType.WeightDuration,
                )
                if (machine?.loadType != MachineLoadType.Level) {
                    Text("Load Prescription", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        RoutineLoadPrescriptionType.entries.filter { type ->
                            when (type) {
                                RoutineLoadPrescriptionType.Absolute -> true
                                RoutineLoadPrescriptionType.PercentOneRepMax -> estimatedOneRepMaxSupported
                                RoutineLoadPrescriptionType.PercentTrainingMax -> trainingMaxSupported
                            }
                        }.forEach { type ->
                            WhipFilterChip(
                                selected = prescriptionType == type,
                                onClick = { onUpdate { it.copy(loadPrescriptionType = type.name) } },
                                label = { Text(type.label.uiTitleCase()) },
                            )
                        }
                    }
                }
                if (prescriptionType == RoutineLoadPrescriptionType.Absolute || machine?.loadType == MachineLoadType.Level) {
                    OutlinedTextField(
                        set.load,
                        { value -> onUpdate { it.copy(load = value.numericInput(), loadPrescriptionType = RoutineLoadPrescriptionType.Absolute.name) } },
                        label = {
                            Text(
                                when {
                                    machine?.loadType == MachineLoadType.Level -> "${machine.levelLabel.humanizeEnum()} target"
                                    exercise?.trackingType in setOf(ExerciseTrackingType.BodyweightReps, ExerciseTrackingType.AssistedBodyweightReps) ->
                                        "External load (${unitSymbol(machine?.unitId ?: set.weightUnitId.ifBlank { exercise?.weightUnitId ?: "kilogram" })})"
                                    else -> "Load (${unitSymbol(machine?.unitId ?: set.weightUnitId.ifBlank { exercise?.weightUnitId ?: "kilogram" })})"
                                },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                } else {
                    OutlinedTextField(
                        set.loadPercentage,
                        { value -> onUpdate { it.copy(loadPercentage = value.numericInput()) } },
                        label = { Text(if (prescriptionType == RoutineLoadPrescriptionType.PercentTrainingMax) "% of training max" else "% of estimated 1RM") },
                        supportingText = { Text("Resolved and rounded to available equipment when the routine starts.") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    if (!percentageInterpretationSupported ||
                        prescriptionType == RoutineLoadPrescriptionType.PercentOneRepMax && !estimatedOneRepMaxSupported ||
                        prescriptionType == RoutineLoadPrescriptionType.PercentTrainingMax && !trainingMaxSupported
                    ) {
                        Text(
                            "This percentage prescription is incompatible with the exercise's load model. Choose Exact Load or change the exercise/equipment configuration.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            if (exercise?.trackingType !in setOf(ExerciseTrackingType.WeightOnly, ExerciseTrackingType.DistanceOnly, ExerciseTrackingType.DurationOnly)) {
                ResponsiveFieldPair(
                    first = { field -> OutlinedTextField(set.repetitionsMin, { value -> onUpdate { it.copy(repetitionsMin = value.filter(Char::isDigit).take(4)) } }, label = { Text("Reps min") }, modifier = field.testTag("routine-reps-min-${set.key}"), singleLine = true) },
                    second = { field -> OutlinedTextField(set.repetitionsMax, { value -> onUpdate { it.copy(repetitionsMax = value.filter(Char::isDigit).take(4)) } }, label = { Text("Reps max") }, modifier = field.testTag("routine-reps-max-${set.key}"), singleLine = true) },
                )
            }
            if (exercise?.trackingType in setOf(ExerciseTrackingType.DistanceOnly, ExerciseTrackingType.DistanceDuration)) {
                OutlinedTextField(set.distance, { value -> onUpdate { it.copy(distance = value.numericInput()) } }, label = { Text("Distance (${unitSymbol(set.distanceUnitId)})") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            if (exercise?.trackingType in setOf(ExerciseTrackingType.DurationOnly, ExerciseTrackingType.DistanceDuration, ExerciseTrackingType.WeightDuration, ExerciseTrackingType.RepsDuration)) {
                OutlinedTextField(set.durationSeconds, { value -> onUpdate { it.copy(durationSeconds = value.filter(Char::isDigit).take(6)) } }, label = { Text("Duration seconds") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            if (showAdvanced) {
                ResponsiveFieldPair(
                    first = { field -> OutlinedTextField(set.rpe, { value -> onUpdate { it.copy(rpe = value.numericInput()) } }, label = { Text("RPE") }, modifier = field, singleLine = true) },
                    second = { field -> OutlinedTextField(set.rir, { value -> onUpdate { it.copy(rir = value.numericInput()) } }, label = { Text("RIR") }, modifier = field, singleLine = true) },
                )
                ResponsiveFieldPair(
                    first = { field -> OutlinedTextField(set.restSeconds, { value -> onUpdate { it.copy(restSeconds = value.filter(Char::isDigit).take(5)) } }, label = { Text("Rest seconds") }, modifier = field, singleLine = true) },
                    second = { field -> OutlinedTextField(set.tempo, { value -> onUpdate { it.copy(tempo = value) } }, label = { Text("Tempo") }, modifier = field, singleLine = true) },
                )
                OutlinedTextField(set.note, { value -> onUpdate { it.copy(note = value) } }, label = { Text("Set note") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
                RoutineLabeledSwitchRow(
                    label = "Unilateral Set",
                    checked = set.unilateral,
                    onCheckedChange = { checked -> onUpdate { it.copy(unilateral = checked) } },
                )
            }
        }
    }
}

@Composable
private fun ExercisePickerPage(
    modifier: Modifier,
    gymState: GymUiState,
    dayName: String,
    assistanceRole: RoutineAssistanceRole?,
    selectedIds: List<Long>,
    onSelectionChange: (List<Long>) -> Unit,
    onCreateExercise: (String) -> Unit,
    onAdd: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var equipment by rememberSaveable { mutableStateOf<String?>(null) }
    var muscle by rememberSaveable { mutableStateOf<String?>(null) }
    var favouritesOnly by rememberSaveable { mutableStateOf(false) }
    var recentOnly by rememberSaveable { mutableStateOf(false) }
    val recentIds = gymState.allWorkoutExercises.sortedByDescending { it.createdAtMillis }.map( com.whip.app.domain.WorkoutExercise::exerciseId ).distinct().take(30).toSet()
    val equipmentOptions = gymState.exercises.map(Exercise::equipment).filter(String::isNotBlank).distinct().sorted()
    val muscleOptions = gymState.exercises.flatMap { exercise ->
        (exercise.primaryMuscles + "," + exercise.secondaryMuscles)
            .split(',', ';').map(String::trim).filter(String::isNotBlank)
    }.distinct().sorted()
    val visible = gymState.exercises.asSequence()
        .filter { exercise ->
            exerciseMatchesQuery(
                exercise,
                query,
                gymState.machines.filter { it.supportsExercise(exercise.id) }.joinToString(" ") { it.displayName },
            )
        }
        .filter { !favouritesOnly || it.favorite }
        .filter { !recentOnly || it.id in recentIds }
        .filter { selected -> categoryId == null || gymState.categoryLinks.any { it.exerciseId == selected.id && it.categoryId == categoryId } }
        .filter { selected -> equipment == null || selected.equipment.equals(equipment, true) }
        .filter { selected -> muscle == null || (selected.primaryMuscles + "," + selected.secondaryMuscles).contains(requireNotNull(muscle), true) }
        .sortedWith(compareByDescending<Exercise> { it.id in recentIds }.thenByDescending(Exercise::favorite).thenBy { it.name.lowercase() })
        .toList()
    val assistanceLabel = assistanceRole?.assistanceUiLabel()
    Column(modifier) {
        if (assistanceLabel != null) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth().testTag("routine-assistance-picker-context"),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "Choose $assistanceLabel assistance",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text("For $dayName · every exercise selected here will be assigned as $assistanceLabel in this routine.")
                    Text(
                        "You make this assignment. Whip does not infer it from muscles, equipment, or Exercise Library categories.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    "Choose exercises",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    "Adding to $dayName without a program role. You can classify an exercise as assistance from its routine editor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OutlinedTextField(query, { query = it }, label = { Text("Search exercises") }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag("routine-exercise-search"), singleLine = true)
        Text(
            "Filters come from your library: Favorites, workout history, your categories, equipment, and muscle fields. They filter this list; they do not assign the routine role above.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WhipFilterChip(favouritesOnly, { favouritesOnly = !favouritesOnly }, { Text("Favourites") })
            WhipFilterChip(recentOnly, { recentOnly = !recentOnly }, { Text("Recent") })
            gymState.categories.forEach { category -> WhipFilterChip(categoryId == category.id, { categoryId = if (categoryId == category.id) null else category.id }, { Text(category.name) }) }
            equipmentOptions.forEach { option -> WhipFilterChip(equipment == option, { equipment = option.takeUnless { it == equipment } }, { Text(option) }) }
            muscleOptions.forEach { option -> WhipFilterChip(muscle == option, { muscle = option.takeUnless { it == muscle } }, { Text(option) }) }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(quantityLabel(visible.size, "exercise"), modifier = Modifier.weight(1f))
            WhipTextButton(onClick = { onCreateExercise(query.trim()) }) { Text(if (query.isBlank()) "Create Exercise" else "Create “${query.trim()}”") }
        }
        LazyColumn(Modifier.weight(1f).testTag("routine-exercise-picker-list"), contentPadding = PaddingValues(12.dp, 0.dp, 12.dp, 96.dp)) {
            if (visible.isEmpty()) item {
                OutlinedCard(Modifier.fillMaxWidth().padding(4.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("No Matching Exercise", fontWeight = FontWeight.Bold)
                        Text("Create it without leaving this routine. It will be added to your library and selected here.")
                        WhipButton(onClick = { onCreateExercise(query.trim()) }) { Text("Create Exercise") }
                    }
                }
            }
            items(visible, key = Exercise::id) { exercise ->
                val checked = exercise.id in selectedIds
                WhipMultiChoiceRow(
                    label = exercise.name,
                    supportingText = listOfNotNull(
                        "★ Favorite".takeIf { exercise.favorite },
                        exercise.equipment.takeIf(String::isNotBlank),
                        exercise.primaryMuscles.takeIf(String::isNotBlank),
                    ).joinToString(" · ").ifBlank { exercise.trackingType.label.uiTitleCase() },
                    checked = checked,
                    onCheckedChange = {
                        onSelectionChange(if (checked) selectedIds - exercise.id else selectedIds + exercise.id)
                    },
                    accessibilityLabel = buildString {
                        append(exercise.name)
                        if (exercise.favorite) append(", favorite")
                        assistanceLabel?.let { append(", will be added as $it assistance to $dayName") }
                    },
                )
            }
        }
        Surface(tonalElevation = 4.dp) {
            WhipButton(enabled = selectedIds.isNotEmpty(), onClick = onAdd, modifier = Modifier.fillMaxWidth().padding(12.dp).testTag("routine-add-selected")) {
                Text(
                    if (assistanceLabel == null) {
                        "Add ${selectedIds.size} Exercise${if (selectedIds.size == 1) "" else "s"} to $dayName"
                    } else {
                        "Add ${selectedIds.size} as $assistanceLabel to $dayName"
                    },
                )
            }
        }
    }
}

@Composable
private fun WorkoutPickerPage(modifier: Modifier, gymState: GymUiState, onChoose: (WorkoutSession) -> Unit) {
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Reuse a Performed Workout", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Exercises, equipment, set types, loads, repetitions, effort, rest, and notes are copied as editable prescriptions.")
        }
        if (gymState.history.isEmpty()) item { Text("No completed workouts are available yet.") }
        items(gymState.history.take(50), key = WorkoutSession::id) { session ->
            val placements = gymState.allWorkoutExercises.count { it.sessionId == session.id }
            OutlinedCard(Modifier.fillMaxWidth().clickable(onClickLabel = "Add ${session.name} to routine") { onChoose(session) }) {
                Column(Modifier.padding(14.dp)) {
                    Text(session.name.ifBlank { "Workout" }, fontWeight = FontWeight.Bold)
                    Text("${session.localDate} · $placements exercise${if (placements == 1) "" else "s"}")
                }
            }
        }
    }
}

@Composable
private fun EquipmentPickerPane(
    modifier: Modifier,
    placement: RoutineBuilderPlacementState,
    gymState: GymUiState,
    onChoose: (GymMachine) -> Unit,
    onNoMachine: () -> Unit,
    onQuickCreate: () -> Unit,
    onAdvancedCreate: () -> Unit,
) {
    var query by rememberSaveable(placement.key) { mutableStateOf("") }
    val machines = gymState.machines.filter {
        it.supportsExercise(placement.exerciseId) && it.displayName.contains(query, true)
    }.sortedWith(compareBy<GymMachine> { it.location.lowercase() }.thenBy { it.name.lowercase() })
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Choose Equipment", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("This list shows machines linked to the exercise; one machine can support many movements.")
        }
        item { OutlinedTextField(query, { query = it }, label = { Text("Search machine or location") }, modifier = Modifier.fillMaxWidth()) }
        item { WhipOutlinedButton(onClick = onNoMachine, modifier = Modifier.fillMaxWidth()) { Text("No Machine / Free Weights") } }
        items(machines, key = GymMachine::id) { machine ->
            OutlinedCard(Modifier.fillMaxWidth().clickable(onClickLabel = "Use ${machine.displayName}") { onChoose(machine) }) {
                Column(Modifier.padding(14.dp)) {
                    Text(machine.displayName, fontWeight = FontWeight.Bold)
                    Text("${machine.loadType.label.uiTitleCase()} · ${machine.availableLoads.size} saved values")
                }
            }
        }
        item {
            WhipButton(onClick = onQuickCreate, modifier = Modifier.fillMaxWidth()) { Text("Quick-Create Machine for This Exercise") }
            WhipOutlinedButton(onClick = onAdvancedCreate, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) { Text("Create Advanced Machine Profile") }
            Text("A new machine is saved to the library immediately and automatically selected in this routine.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun QuickMachineDialog(modifier: Modifier = Modifier, exercise: Exercise, onDismiss: () -> Unit, onSave: (GymMachineDraft) -> Unit) {
    var name by rememberSaveable(exercise.id) { mutableStateOf("") }
    var location by rememberSaveable(exercise.id) { mutableStateOf("") }
    var loadType by rememberSaveable(exercise.id) { mutableStateOf(MachineLoadType.Mass) }
    var unitId by rememberSaveable(exercise.id) { mutableStateOf(exercise.weightUnitId) }
    var minimum by rememberSaveable(exercise.id) { mutableStateOf(if (loadType == MachineLoadType.Level) "1" else "5") }
    var maximum by rememberSaveable(exercise.id) { mutableStateOf(if (loadType == MachineLoadType.Level) "10" else "100") }
    var increment by rememberSaveable(exercise.id) { mutableStateOf(if (loadType == MachineLoadType.Level) "1" else "5") }
    var levelLabel by rememberSaveable(exercise.id) { mutableStateOf("level") }
    var levelDirection by rememberSaveable(exercise.id) {
        mutableStateOf(MachineLevelDirection.HigherNumberMoreResistance)
    }
    val min = minimum.toWhipDoubleOrNull()
    val max = maximum.toWhipDoubleOrNull()
    val step = increment.toWhipDoubleOrNull()
    val validRange = min != null && max != null && step != null && min >= 0.0 && max >= min && step > 0.0 && ((max - min) / step) <= 500
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text("Quick-Create Machine") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("For ${exercise.name}. Add advanced setup details later from the machine library.")
                OutlinedTextField(name, { name = it.replace('\n', ' ').replace('\r', ' ').take(100) }, label = { Text("Machine name *") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("routine-quick-machine-name"))
                OutlinedTextField(location, { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MachineLoadType.entries.forEach { type -> WhipFilterChip(loadType == type, { loadType = type; minimum = if (type == MachineLoadType.Level) "1" else "5"; maximum = if (type == MachineLoadType.Level) "10" else "100"; increment = if (type == MachineLoadType.Level) "1" else "5" }, { Text(type.label.uiTitleCase()) }) }
                }
                if (loadType == MachineLoadType.Mass) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("kilogram", "pound").forEach { unit -> WhipFilterChip(unitId == unit, { unitId = unit }, { Text(unitSymbol(unit)) }) }
                } else {
                    OutlinedTextField(levelLabel, { levelLabel = it }, label = { Text("Setting label") }, modifier = Modifier.fillMaxWidth())
                    MachineLevelDirection.entries.forEach { direction ->
                        WhipFilterChip(
                            selected = levelDirection == direction,
                            onClick = { levelDirection = direction },
                            label = { Text(direction.label) },
                        )
                    }
                }
                ResponsiveFieldPair(
                    first = { field -> OutlinedTextField(minimum, { minimum = it.numericInput() }, label = { Text("Minimum") }, modifier = field) },
                    second = { field -> OutlinedTextField(maximum, { maximum = it.numericInput() }, label = { Text("Maximum") }, modifier = field) },
                )
                OutlinedTextField(increment, { increment = it.numericInput() }, label = { Text("Increment") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            WhipTextButton(enabled = name.isNotBlank() && validRange, onClick = {
                onSave(
                    GymMachineDraft(
                        exerciseId = exercise.id,
                        name = name.trim(),
                        location = location.trim(),
                        loadType = loadType,
                        unitId = unitId,
                        levelLabel = levelLabel.trim().ifBlank { "level" },
                        availableLoads = numericRange(requireNotNull(min), requireNotNull(max), requireNotNull(step)),
                        loadInterpretation = if (loadType == MachineLoadType.Level) LoadInterpretation.OrdinalSetting else LoadInterpretation.MachineDisplayedMass,
                        levelDirection = levelDirection,
                    ),
                )
            }, modifier = Modifier.testTag("routine-quick-machine-create")) { Text("Create and Select") }
        },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun buildInitialRoutineState(
    token: String,
    initial: RoutineDraft?,
    exercises: List<Exercise>,
    machines: List<GymMachine>,
): RoutineBuilderState {
    var next = 1L
    val exerciseMap = exercises.associateBy(Exercise::id)
    val machineMap = machines.associateBy(GymMachine::id)
    val days = initial?.days.orEmpty().map { day ->
        RoutineBuilderDayState(
            key = next++,
            name = day.name,
            progressionIndex = day.progressionIndex,
            placements = day.exercises.map { placement ->
                val exercise = exerciseMap[placement.exerciseId]
                val machine = placement.machineId?.let(machineMap::get)
                RoutineBuilderPlacementState(
                    key = next++,
                    exerciseId = placement.exerciseId,
                    exerciseNameSnapshot = exercise?.name ?: "Exercise ${placement.exerciseId}",
                    machineId = placement.machineId,
                    equipmentBindingState = placement.equipmentBindingState.name,
                    machineProfileUuidSnapshot = placement.machineProfileUuidSnapshot,
                    machineNameSnapshot = machine?.displayName ?: placement.machineNameSnapshot,
                    machineLoadTypeSnapshot = machine?.loadType?.name ?: placement.machineLoadTypeSnapshot?.name.orEmpty(),
                    machineUnitIdSnapshot = machine?.unitId ?: placement.machineUnitIdSnapshot,
                    machineLevelLabelSnapshot = machine?.levelLabel ?: placement.machineLevelLabelSnapshot,
                    machineLoadInterpretationSnapshot = machine?.loadInterpretation?.name ?: placement.machineLoadInterpretationSnapshot.name,
                    machineConfigurationGroupSnapshot = machine?.configurationGroupId ?: placement.machineConfigurationGroupSnapshot,
                    machineConfigurationVersionSnapshot = machine?.configurationVersion ?: placement.machineConfigurationVersionSnapshot,
                    machineConfigurationSnapshot = machine?.let { listOf(it.seatPosition, it.backPosition, it.attachment).filter(String::isNotBlank).joinToString(" · ") } ?: placement.machineConfigurationSnapshot,
                    notes = placement.notes,
                    groupKey = placement.groupKey,
                    copyPreviousWorkout = placement.copyPreviousWorkout,
                    trainingMaxPercent = editableNumericValue(placement.trainingMaxPercent),
                    progressionPercentages = placement.progressionPercentages.joinToString(", ", transform = ::editableNumericValue),
                    alternativeExerciseIds = placement.alternativeExerciseIds,
                    trainingMaxValue = placement.trainingMaxValue?.let(::editableNumericValue).orEmpty(),
                    trainingMaxUnitId = placement.trainingMaxUnitId,
                    cycleIncrementValue = placement.cycleIncrementValue?.let(::editableNumericValue).orEmpty(),
                    trainingMaxSource = placement.trainingMaxSource.name,
                    trainingMaxBasisKind = placement.trainingMaxBasisKind.name,
                    trainingMaxBasisValue = placement.trainingMaxBasisValue?.let(::editableNumericValue).orEmpty(),
                    trainingMaxBasisUnitId = placement.trainingMaxBasisUnitId,
                    trainingMaxIncreaseEligible = placement.trainingMaxIncreaseEligible,
                    mainWorkScheme = placement.mainWorkScheme.name,
                    supplementalScheme = placement.supplementalScheme.name,
                    assistanceRole = placement.assistanceRole.name,
                    placementKind = when {
                        placement.placementKind != RoutinePlacementKind.General -> placement.placementKind
                        placement.assistanceRole == RoutineAssistanceRole.MainLift -> RoutinePlacementKind.MainLift
                        placement.assistanceRole != RoutineAssistanceRole.Unspecified -> RoutinePlacementKind.Assistance
                        else -> RoutinePlacementKind.General
                    }.name,
                    assistanceCategory = when {
                        placement.assistanceCategory != RoutineAssistanceCategory.Unspecified -> placement.assistanceCategory
                        else -> placement.assistanceRole.toBuilderAssistanceCategory()
                    }.name,
                    jokerSetsEnabled = placement.jokerSetsEnabled,
                    sets = placement.plannedSets.map { set ->
                        RoutineBuilderSetState(
                            key = next++,
                            load = (set.machineLoadValue ?: set.weight)?.let(::editableNumericValue).orEmpty(),
                            weightUnitId = set.weightUnitId,
                            repetitionsMin = set.reps?.toString().orEmpty(),
                            repetitionsMax = set.repsMax?.toString().orEmpty(),
                            distance = set.distance?.let(::editableNumericValue).orEmpty(),
                            distanceUnitId = set.distanceUnitId,
                            durationSeconds = set.durationSeconds?.toString().orEmpty(),
                            bodyweightKg = set.bodyweightKg?.let(::editableNumericValue).orEmpty(),
                            classification = set.classification.name,
                            rpe = set.rpe?.let(::editableNumericValue).orEmpty(),
                            rir = set.rir?.let(::editableNumericValue).orEmpty(),
                            restSeconds = set.restSeconds?.toString().orEmpty(),
                            tempo = set.tempo,
                            note = set.note,
                            unilateral = set.unilateral,
                            loadPrescriptionType = set.loadPrescriptionType.name,
                            loadPercentage = set.loadPercentage?.let(::editableNumericValue).orEmpty(),
                            routinePhaseIndex = set.routinePhaseIndex,
                            workSection = set.workSection.name,
                            optionalWorkKind = set.optionalWorkKind.name,
                            mainWorkScheme = set.mainWorkScheme?.name,
                            supplementalScheme = set.supplementalScheme?.name,
                        )
                    },
                )
            },
        )
    }.ifEmpty { listOf(RoutineBuilderDayState(next++, "Day A")) }
    return RoutineBuilderState(
        token = token,
        name = initial?.name.orEmpty(),
        notes = initial?.notes.orEmpty(),
        days = days,
        selectedDayKey = days.first().key,
        nextKey = next,
        programKind = initial?.program?.kind?.name,
        programPhaseCount = initial?.program?.phaseCount ?: 1,
        programPhaseLabels = initial?.program?.phaseLabels.orEmpty(),
        programPhaseRoles = initial?.program?.phaseRoles.orEmpty().map(RoutineProgramPhaseRole::name),
        trainingMaxAdvanceAfterPhaseIndices = initial?.program?.trainingMaxAdvanceAfterPhaseIndices.orEmpty(),
        currentProgramPhaseIndexHint = initial?.program?.currentPhaseIndexHint,
        nextProgramDayKeyHint = initial?.nextProgramDayPositionHint?.let { days.getOrNull(it)?.key },
        programTemplateKey = initial?.program?.templateKey?.name ?: RoutineProgramTemplateKey.None.name,
        programTemplateRevision = initial?.program?.templateRevision ?: 0,
        progressionMode = initial?.program?.progressionMode?.name ?: RoutineProgressionMode.Standard.name,
        allowNonStandardHigherSuggestions = initial?.program?.allowNonStandardHigherSuggestions ?: false,
    )
}

private fun RoutineBuilderState.toRoutineDraft(gymState: GymUiState): RoutineDraft {
    val exercises = (gymState.exercises + gymState.archivedExercises).associateBy(Exercise::id)
    val machines = (gymState.machines + gymState.archivedMachines).associateBy(GymMachine::id)
    return RoutineDraft(
        name = name.trim(),
        notes = notes.trim(),
        nextProgramDayPositionHint = nextProgramDayKeyHint?.let { key ->
            days.indexOfFirst { it.key == key }.takeIf { it >= 0 } ?: 0
        },
        days = days.map { day ->
            RoutineDayDraft(
                name = day.name.trim(),
                progressionIndex = day.progressionIndex,
                exercises = day.placements.map { placement ->
                    val exercise = exercises[placement.exerciseId]
                    val machine = placement.machineId?.let(machines::get)
                    val machineLoadType = machine?.loadType
                        ?: placement.machineLoadTypeSnapshot.takeIf(String::isNotBlank)?.let(MachineLoadType::valueOf)
                    val machineUnitId = machine?.unitId ?: placement.machineUnitIdSnapshot
                    RoutineExerciseDraft(
                        exerciseId = placement.exerciseId,
                        notes = placement.notes.trim(),
                        groupKey = placement.groupKey,
                        plannedSets = placement.sets.map { set ->
                            val load = set.load.toWhipDoubleOrNull()
                            WorkoutSetDraft(
                                weight = load.takeUnless { machineLoadType == MachineLoadType.Level },
                                weightUnitId = machineUnitId.takeIf(String::isNotBlank)
                                    ?: set.weightUnitId.takeIf(String::isNotBlank)
                                    ?: exercise?.weightUnitId ?: gymState.appSettings.gymWeightUnitId,
                                reps = set.repetitionsMin.toIntOrNull(),
                                repsMax = set.repetitionsMax.toIntOrNull(),
                                distance = set.distance.toWhipDoubleOrNull(),
                                distanceUnitId = set.distanceUnitId,
                                durationSeconds = set.durationSeconds.toLongOrNull(),
                                bodyweightKg = set.bodyweightKg.toWhipDoubleOrNull(),
                                planned = true,
                                classification = runCatching { WorkoutSetClassification.valueOf(set.classification) }.getOrDefault(WorkoutSetClassification.Working),
                                note = set.note.trim(),
                                rpe = set.rpe.toWhipDoubleOrNull(),
                                rir = set.rir.toWhipDoubleOrNull(),
                                tempo = set.tempo.trim(),
                                restSeconds = set.restSeconds.toIntOrNull(),
                                machineLoadValue = load.takeIf {
                                    placement.machineId != null || machineLoadType == MachineLoadType.Level
                                },
                                unilateral = set.unilateral,
                                loadPrescriptionType = runCatching { RoutineLoadPrescriptionType.valueOf(set.loadPrescriptionType) }
                                    .getOrDefault(RoutineLoadPrescriptionType.Absolute),
                                loadPercentage = set.loadPercentage.toWhipDoubleOrNull(),
                                routinePhaseIndex = set.routinePhaseIndex,
                                workSection = runCatching { RoutineWorkSection.valueOf(set.workSection) }
                                    .getOrDefault(RoutineWorkSection.Unspecified),
                                optionalWorkKind = runCatching { RoutineOptionalWorkKind.valueOf(set.optionalWorkKind) }
                                    .getOrDefault(RoutineOptionalWorkKind.None),
                                mainWorkScheme = set.mainWorkScheme?.let { value ->
                                    runCatching { RoutineMainWorkScheme.valueOf(value) }.getOrNull()
                                },
                                supplementalScheme = set.supplementalScheme?.let { value ->
                                    runCatching { RoutineSupplementalScheme.valueOf(value) }.getOrNull()
                                },
                            )
                        },
                        copyPreviousWorkout = placement.copyPreviousWorkout,
                        machineId = placement.machineId,
                        equipmentBindingState = runCatching { RoutineEquipmentBindingState.valueOf(placement.equipmentBindingState) }.getOrDefault(RoutineEquipmentBindingState.None),
                        machineProfileUuidSnapshot = machine?.uuid ?: placement.machineProfileUuidSnapshot,
                        machineNameSnapshot = machine?.displayName ?: placement.machineNameSnapshot,
                        machineLoadTypeSnapshot = machine?.loadType ?: placement.machineLoadTypeSnapshot.takeIf(String::isNotBlank)?.let(MachineLoadType::valueOf),
                        machineUnitIdSnapshot = machine?.unitId ?: placement.machineUnitIdSnapshot,
                        machineLevelLabelSnapshot = machine?.levelLabel ?: placement.machineLevelLabelSnapshot,
                        machineLoadInterpretationSnapshot = machine?.loadInterpretation ?: runCatching { LoadInterpretation.valueOf(placement.machineLoadInterpretationSnapshot) }.getOrDefault(LoadInterpretation.Total),
                        machineConfigurationGroupSnapshot = machine?.configurationGroupId ?: placement.machineConfigurationGroupSnapshot,
                        machineConfigurationVersionSnapshot = machine?.configurationVersion ?: placement.machineConfigurationVersionSnapshot,
                        machineConfigurationSnapshot = placement.machineConfigurationSnapshot,
                        trainingMaxPercent = placement.trainingMaxPercent.toWhipDoubleOrNull() ?: 90.0,
                        progressionPercentages = placement.progressionPercentages.split(',').mapNotNull { it.trim().toWhipDoubleOrNull() },
                        alternativeExerciseIds = placement.alternativeExerciseIds.distinct().filterNot { it == placement.exerciseId },
                        trainingMaxValue = placement.trainingMaxValue.toWhipDoubleOrNull()
                            .takeIf { placement.trainingMaxSource == RoutineTrainingMaxSource.Explicit.name },
                        trainingMaxUnitId = placement.trainingMaxUnitId,
                        cycleIncrementValue = placement.cycleIncrementValue.toWhipDoubleOrNull()
                            .takeIf { placement.trainingMaxSource == RoutineTrainingMaxSource.Explicit.name },
                        trainingMaxSource = runCatching { RoutineTrainingMaxSource.valueOf(placement.trainingMaxSource) }
                            .getOrDefault(RoutineTrainingMaxSource.EstimatedOneRepMaxPercent),
                        trainingMaxBasisKind = runCatching { TrainingMaxBasisKind.valueOf(placement.trainingMaxBasisKind) }
                            .getOrDefault(TrainingMaxBasisKind.Unspecified),
                        trainingMaxBasisValue = placement.trainingMaxBasisValue.toWhipDoubleOrNull(),
                        trainingMaxBasisUnitId = placement.trainingMaxBasisUnitId,
                        trainingMaxIncreaseEligible = placement.trainingMaxIncreaseEligible,
                        mainWorkScheme = runCatching { RoutineMainWorkScheme.valueOf(placement.mainWorkScheme) }
                            .getOrDefault(RoutineMainWorkScheme.Unspecified),
                        supplementalScheme = runCatching { RoutineSupplementalScheme.valueOf(placement.supplementalScheme) }
                            .getOrDefault(RoutineSupplementalScheme.None),
                        assistanceRole = runCatching { RoutineAssistanceRole.valueOf(placement.assistanceRole) }
                            .getOrDefault(RoutineAssistanceRole.Unspecified),
                        placementKind = runCatching { RoutinePlacementKind.valueOf(placement.placementKind) }
                            .getOrDefault(RoutinePlacementKind.General),
                        assistanceCategory = runCatching { RoutineAssistanceCategory.valueOf(placement.assistanceCategory) }
                            .getOrDefault(RoutineAssistanceCategory.Unspecified),
                        jokerSetsEnabled = placement.jokerSetsEnabled,
                    )
                },
            )
        },
        program = programKind?.let { kind ->
            RoutineProgramDraft(
                kind = runCatching { RoutineProgramKind.valueOf(kind) }.getOrDefault(RoutineProgramKind.Static),
                phaseCount = programPhaseCount,
                phaseLabels = programPhaseLabels,
                phaseRoles = programPhaseRoles.mapNotNull { role -> runCatching { RoutineProgramPhaseRole.valueOf(role) }.getOrNull() },
                trainingMaxAdvanceAfterPhaseIndices = trainingMaxAdvanceAfterPhaseIndices,
                currentPhaseIndexHint = currentProgramPhaseIndexHint,
                templateKey = runCatching { RoutineProgramTemplateKey.valueOf(programTemplateKey) }
                    .getOrDefault(RoutineProgramTemplateKey.None),
                templateRevision = programTemplateRevision,
                progressionMode = runCatching { RoutineProgressionMode.valueOf(progressionMode) }
                    .getOrDefault(RoutineProgressionMode.Standard),
                allowNonStandardHigherSuggestions = allowNonStandardHigherSuggestions,
            )
        },
    )
}

private fun routineBuilderValidationErrors(state: RoutineBuilderState, gymState: GymUiState): Map<Long, String> {
    val errors = mutableMapOf<Long, String>()
    val exercises = (gymState.exercises + gymState.archivedExercises).associateBy(Exercise::id)
    val machines = (gymState.machines + gymState.archivedMachines).associateBy(GymMachine::id)
    state.days.flatMap(RoutineBuilderDayState::placements).forEach { placement ->
        val exercise = exercises[placement.exerciseId]
        val machine = placement.machineId?.let(machines::get)
        val hasTrainingMaxPrescription = placement.sets.any {
            it.loadPrescriptionType == RoutineLoadPrescriptionType.PercentTrainingMax.name
        }
        val hasEstimatedOneRepMaxPrescription = placement.sets.any {
            it.loadPrescriptionType == RoutineLoadPrescriptionType.PercentOneRepMax.name
        }
        val trainingMaxSource = runCatching { RoutineTrainingMaxSource.valueOf(placement.trainingMaxSource) }
            .getOrDefault(RoutineTrainingMaxSource.EstimatedOneRepMaxPercent)
        val hasCurrentEstimatedOneRepMax = currentEstimatedOneRepMaxKg(gymState, placement.exerciseId, machine) != null
        val loadInterpretation = machine?.loadInterpretation ?: exercise?.loadInterpretation ?: LoadInterpretation.Total
        val hasPercentagePrescription = hasTrainingMaxPrescription || hasEstimatedOneRepMaxPrescription
        val isProgrammed = state.programKind?.let { kind ->
            runCatching { RoutineProgramKind.valueOf(kind) }.getOrDefault(RoutineProgramKind.Static)
        }?.let { it != RoutineProgramKind.Static } == true
        val isProgramMainLift = state.programKind.isFiveThreeOneProgramKindName() &&
            placement.placementKind == RoutinePlacementKind.MainLift.name
        val error = when {
            exercise == null -> "Exercise no longer exists"
            placement.equipmentBindingState == RoutineEquipmentBindingState.NeedsEquipment.name -> "Choose replacement equipment"
            placement.machineId != null && machine == null -> "Machine no longer exists"
            hasPercentagePrescription && (machine?.loadType == MachineLoadType.Level ||
                !loadInterpretation.supportsRoutinePercentagePrescription()) ->
                "Percentage prescriptions require a compatible mass-based load model"
            hasEstimatedOneRepMaxPrescription && exercise.trackingType != ExerciseTrackingType.WeightReps ->
                "Estimated 1RM prescriptions require a Weight + Reps exercise"
            hasTrainingMaxPrescription && exercise.trackingType !in setOf(
                ExerciseTrackingType.WeightReps,
                ExerciseTrackingType.WeightOnly,
                ExerciseTrackingType.WeightDuration,
            ) -> "Training Max prescriptions require a mass-tracked exercise"
            machine?.loadType == MachineLoadType.Level && placement.sets.any {
                it.load.toWhipDoubleOrNull()?.let { value -> !value.isFinite() || value < 0.0 } != false
            } -> "Enter a valid setting for every planned set"
            trainingMaxSource == RoutineTrainingMaxSource.Explicit && placement.trainingMaxValue.isNotBlank() &&
                placement.trainingMaxValue.toWhipDoubleOrNull()
                    ?.let { !it.isFinite() || it <= 0.0 } != false -> "Training Max must be above zero"
            placement.trainingMaxPercent.toWhipDoubleOrNull()?.let { it !in 1.0..100.0 } != false ->
                "Training Max percentage must be from 1 to 100%"
            hasTrainingMaxPrescription && trainingMaxSource == RoutineTrainingMaxSource.Explicit &&
                placement.trainingMaxValue.toWhipDoubleOrNull()
                ?.let { !it.isFinite() || it <= 0.0 } != false -> "Training Max must be above zero"
            hasTrainingMaxPrescription && trainingMaxSource == RoutineTrainingMaxSource.EstimatedOneRepMaxPercent &&
                !hasCurrentEstimatedOneRepMax -> "Record an estimated 1RM or enter an explicit Training Max"
            hasEstimatedOneRepMaxPrescription && !hasCurrentEstimatedOneRepMax ->
                "Record an estimated 1RM for this exercise and equipment"
            isProgramMainLift && placement.cycleIncrementValue.toWhipDoubleOrNull()
                ?.let { !it.isFinite() || it <= 0.0 } != false -> "Cycle increase must be above zero"
            !isProgrammed && placement.progressionPercentages.split(',').map(String::trim).filter(String::isNotBlank)
                .any { it.toWhipDoubleOrNull()?.let { value -> value !in 1.0..200.0 } != false } -> "Every cycle multiplier must be from 1 to 200%"
            else -> placement.sets.firstNotNullOfOrNull { set ->
                val values = listOf(set.load, set.distance, set.rpe, set.rir).filter(String::isNotBlank)
                when {
                    values.any { it.toWhipDoubleOrNull() == null } -> "A set contains an invalid number"
                    set.repetitionsMin.isNotBlank() && set.repetitionsMin.toIntOrNull() == null -> "A set contains invalid repetitions"
                    set.repetitionsMax.isNotBlank() && set.repetitionsMax.toIntOrNull() == null -> "A set contains an invalid repetition maximum"
                    set.repetitionsMin.toIntOrNull() != null && set.repetitionsMax.toIntOrNull() != null && requireNotNull(set.repetitionsMax.toIntOrNull()) < requireNotNull(set.repetitionsMin.toIntOrNull()) -> "Maximum repetitions must be at least the minimum"
                    set.rpe.toWhipDoubleOrNull()?.let { it !in 1.0..10.0 } == true -> "RPE must be from 1 to 10"
                    set.rir.toWhipDoubleOrNull()?.let { it !in 0.0..10.0 } == true -> "RIR must be from 0 to 10"
                    set.restSeconds.toIntOrNull()?.let { it !in 0..86_400 } == true -> "Rest must be from 0 to 86,400 seconds"
                    runCatching { RoutineLoadPrescriptionType.valueOf(set.loadPrescriptionType) }.getOrNull() != RoutineLoadPrescriptionType.Absolute &&
                        set.loadPercentage.toWhipDoubleOrNull()?.let { it !in 1.0..200.0 } != false -> "Percentage prescriptions must be from 1 to 200"
                    else -> null
                }
            }
        }
        if (error != null) errors[placement.key] = error
    }
    return errors
}

private fun currentEstimatedOneRepMaxKg(
    gymState: GymUiState,
    exerciseId: Long,
    machine: GymMachine?,
): Double? = gymState.personalRecords.asSequence()
    .filter { record ->
        record.current && record.exerciseId == exerciseId &&
            record.type == PersonalRecordType.EstimatedOneRepMax
    }
    .filter { record ->
        if (machine == null) record.machineProfileUuidSnapshot == null
        else record.machineProfileUuidSnapshot == machine.uuid
    }
    .maxByOrNull { record -> record.value }
    ?.value

private fun RoutineBuilderPlacementState.withMachine(machine: GymMachine) = copy(
    machineId = machine.id,
    equipmentBindingState = RoutineEquipmentBindingState.Resolved.name,
    machineProfileUuidSnapshot = machine.uuid,
    machineNameSnapshot = machine.displayName,
    machineLoadTypeSnapshot = machine.loadType.name,
    machineUnitIdSnapshot = machine.unitId,
    machineLevelLabelSnapshot = machine.levelLabel,
    machineLoadInterpretationSnapshot = machine.loadInterpretation.name,
    machineConfigurationGroupSnapshot = machine.configurationGroupId,
    machineConfigurationVersionSnapshot = machine.configurationVersion,
    machineConfigurationSnapshot = listOf(machine.seatPosition, machine.backPosition, machine.attachment).filter(String::isNotBlank).joinToString(" · "),
).let { updated ->
    if (machine.loadType == MachineLoadType.Mass) updated.withProgramMassUnit(machine.unitId)
    else updated.withLevelMachinePrescriptions()
}

private fun RoutineBuilderPlacementState.withMachine(id: Long, draft: GymMachineDraft) = copy(
    machineId = id,
    equipmentBindingState = RoutineEquipmentBindingState.Resolved.name,
    machineProfileUuidSnapshot = null,
    machineNameSnapshot = if (draft.location.isBlank()) draft.name else "${draft.name} · ${draft.location}",
    machineLoadTypeSnapshot = draft.loadType.name,
    machineUnitIdSnapshot = draft.unitId,
    machineLevelLabelSnapshot = draft.levelLabel,
    machineLoadInterpretationSnapshot = draft.loadInterpretation.name,
    machineConfigurationGroupSnapshot = draft.configurationGroupId,
    machineConfigurationVersionSnapshot = draft.configurationVersion,
    machineConfigurationSnapshot = listOf(draft.seatPosition, draft.backPosition, draft.attachment).filter(String::isNotBlank).joinToString(" · "),
).let { updated ->
    if (draft.loadType == MachineLoadType.Mass) updated.withProgramMassUnit(draft.unitId)
    else updated.withLevelMachinePrescriptions()
}

private fun RoutineBuilderPlacementState.withLevelMachinePrescriptions() = copy(
    trainingMaxValue = "",
    cycleIncrementValue = "",
    trainingMaxSource = RoutineTrainingMaxSource.EstimatedOneRepMaxPercent.name,
    trainingMaxBasisKind = TrainingMaxBasisKind.Unspecified.name,
    trainingMaxBasisValue = "",
    trainingMaxBasisUnitId = "",
    sets = sets.map { set ->
        if (set.loadPrescriptionType == RoutineLoadPrescriptionType.Absolute.name) set
        else set.copy(
            load = "",
            loadPrescriptionType = RoutineLoadPrescriptionType.Absolute.name,
            loadPercentage = "",
        )
    },
)

private fun RoutineBuilderPlacementState.withoutMachine(freeWeightUnitId: String) = copy(
    machineId = null,
    equipmentBindingState = RoutineEquipmentBindingState.None.name,
    machineProfileUuidSnapshot = null,
    machineNameSnapshot = "",
    machineLoadTypeSnapshot = "",
    machineUnitIdSnapshot = "",
    machineLevelLabelSnapshot = "",
    machineLoadInterpretationSnapshot = LoadInterpretation.Total.name,
    machineConfigurationGroupSnapshot = "",
    machineConfigurationVersionSnapshot = 1,
    machineConfigurationSnapshot = "",
).withProgramMassUnit(freeWeightUnitId)

/** Keeps entered prescriptions and explicit cycle values physically equivalent across unit changes. */
internal fun RoutineBuilderPlacementState.withProgramMassUnit(targetUnitId: String): RoutineBuilderPlacementState {
    if (targetUnitId.isBlank()) return this
    fun converted(value: String, sourceUnitId: String): String = value.toWhipDoubleOrNull()
        ?.let { convertPracticalMassValue(it, sourceUnitId, targetUnitId) }
        ?.let(::editableNumericValue)
        .orEmpty()
    return copy(
        trainingMaxValue = converted(trainingMaxValue, trainingMaxUnitId),
        trainingMaxUnitId = targetUnitId,
        cycleIncrementValue = converted(cycleIncrementValue, trainingMaxUnitId),
        trainingMaxBasisValue = converted(
            trainingMaxBasisValue,
            trainingMaxBasisUnitId.ifBlank { trainingMaxUnitId },
        ),
        trainingMaxBasisUnitId = targetUnitId.takeIf { trainingMaxBasisValue.isNotBlank() }.orEmpty(),
        sets = sets.map { set ->
            if (set.loadPrescriptionType == RoutineLoadPrescriptionType.Absolute.name && set.load.isNotBlank()) {
                set.copy(
                    load = converted(set.load, set.weightUnitId.ifBlank { trainingMaxUnitId }),
                    weightUnitId = targetUnitId,
                )
            } else {
                set
            }
        },
    )
}

private fun duplicatePlacement(holder: RoutineBuilderViewModel, dayKey: Long, source: RoutineBuilderPlacementState) {
    holder.update { state ->
        var next = state.nextKey
        val duplicate = source.copy(key = next++, sets = source.sets.map { it.copy(key = next++) })
        state.copy(
            days = state.days.map { day ->
                if (day.key != dayKey) day else {
                    val index = day.placements.indexOfFirst { it.key == source.key }
                    day.copy(placements = day.placements.toMutableList().also { it.add((index + 1).coerceIn(0, it.size), duplicate) })
                }
            },
            selectedPlacementKey = duplicate.key,
            nextKey = next,
        )
    }
}

private fun moveOrCopyPlacement(holder: RoutineBuilderViewModel, fromDayKey: Long, targetDayKey: Long, source: RoutineBuilderPlacementState, copy: Boolean) {
    holder.update { state -> state.moveOrCopyPlacement(fromDayKey, targetDayKey, source, copy) }
}

internal fun RoutineBuilderState.moveOrCopyPlacement(
    fromDayKey: Long,
    targetDayKey: Long,
    source: RoutineBuilderPlacementState,
    copy: Boolean,
): RoutineBuilderState {
    var next = nextKey
    val target = if (copy) {
        source.copy(key = next++, groupKey = null, sets = source.sets.map { it.copy(key = next++) })
    } else {
        source.copy(groupKey = null)
    }
    return copy(
        days = days.map { day -> when {
            day.key == fromDayKey && day.key == targetDayKey && copy ->
                day.copy(placements = day.placements + target).normalizePlacementGroupsAndOrder()
            day.key == fromDayKey && day.key == targetDayKey -> day
            day.key == fromDayKey -> if (copy) day else day.removePlacement(source.key)
            day.key == targetDayKey -> day.copy(placements = day.placements + target).normalizePlacementGroupsAndOrder()
            else -> day
        } },
        selectedDayKey = targetDayKey,
        selectedPlacementKey = target.key,
        nextKey = next,
    )
}

private fun importWorkoutIntoDay(holder: RoutineBuilderViewModel, dayKey: Long, session: WorkoutSession, gymState: GymUiState) {
    holder.update { state ->
        var next = state.nextKey
        val exercises = (gymState.exercises + gymState.archivedExercises).associateBy(Exercise::id)
        val machines = (gymState.machines + gymState.archivedMachines).associateBy(GymMachine::id)
        val additions = gymState.allWorkoutExercises.filter { it.sessionId == session.id }.sortedBy { it.position }.map { workoutExercise ->
            val exercise = exercises[workoutExercise.exerciseId]
            val machine = workoutExercise.machineId?.let(machines::get)
            RoutineBuilderPlacementState(
                key = next++,
                exerciseId = workoutExercise.exerciseId,
                exerciseNameSnapshot = exercise?.name ?: "Exercise ${workoutExercise.exerciseId}",
                machineId = machine?.id,
                equipmentBindingState = when {
                    machine != null -> RoutineEquipmentBindingState.Resolved.name
                    workoutExercise.machineProfileUuidSnapshot != null -> RoutineEquipmentBindingState.NeedsEquipment.name
                    else -> RoutineEquipmentBindingState.None.name
                },
                machineProfileUuidSnapshot = workoutExercise.machineProfileUuidSnapshot,
                machineNameSnapshot = workoutExercise.machineNameSnapshot,
                machineLoadTypeSnapshot = workoutExercise.machineLoadTypeSnapshot?.name.orEmpty(),
                machineUnitIdSnapshot = workoutExercise.machineUnitIdSnapshot,
                machineLevelLabelSnapshot = workoutExercise.machineLevelLabelSnapshot,
                machineLoadInterpretationSnapshot = workoutExercise.loadInterpretationSnapshot.name,
                machineConfigurationGroupSnapshot = workoutExercise.machineConfigurationGroupSnapshot,
                machineConfigurationVersionSnapshot = workoutExercise.machineConfigurationVersionSnapshot,
                machineConfigurationSnapshot = workoutExercise.machineConfigurationSnapshot,
                notes = workoutExercise.notes,
                sets = gymState.allSets.filter { it.workoutExerciseId == workoutExercise.id && it.deletedAtMillis == null }.sortedBy { it.position }.map { set ->
                    RoutineBuilderSetState(
                        key = next++,
                        load = (set.machineLoadValue ?: set.enteredWeight)?.let(::editableNumericValue).orEmpty(),
                        weightUnitId = set.enteredWeightUnitId.orEmpty(),
                        repetitionsMin = set.repetitions?.toString().orEmpty(),
                        repetitionsMax = set.repetitions?.toString().orEmpty(),
                        distance = set.enteredDistance?.let(::editableNumericValue).orEmpty(),
                        distanceUnitId = set.enteredDistanceUnitId ?: gymState.appSettings.distanceUnitId,
                        durationSeconds = set.durationSeconds?.toString().orEmpty(),
                        bodyweightKg = set.bodyweightKg?.let(::editableNumericValue).orEmpty(),
                        classification = set.classification.name,
                        rpe = set.rpe?.let(::editableNumericValue).orEmpty(),
                        rir = set.rir?.let(::editableNumericValue).orEmpty(),
                        restSeconds = set.restSeconds?.toString().orEmpty(),
                        tempo = set.tempo,
                        note = set.note,
                        unilateral = set.unilateral,
                    )
                },
            )
        }
        state.copy(
            days = state.days.map { if (it.key == dayKey) it.copy(placements = it.placements + additions) else it },
            selectedPlacementKey = additions.lastOrNull()?.key,
            nextKey = next,
        )
    }
}

internal fun RoutineBuilderState.withDayTemplate(names: List<String>): RoutineBuilderState {
    var next = nextKey
    val newDays = names.map { RoutineBuilderDayState(next++, it) }
    return copy(
        days = newDays,
        selectedDayKey = newDays.firstOrNull()?.key,
        selectedPlacementKey = null,
        nextKey = next,
        nextProgramDayKeyHint = newDays.firstOrNull()?.key,
    )
}

internal fun RoutineBuilderState.moveDay(key: Long, delta: Int): RoutineBuilderState {
    val index = days.indexOfFirst { it.key == key }
    if (index < 0) return this
    val moved = moveListItem(days, index, delta)
    return if (moved == days) this else copy(days = moved)
}

internal fun RoutineBuilderState.duplicateDay(key: Long): RoutineBuilderState {
    val index = days.indexOfFirst { it.key == key }
    if (index < 0) return this
    var next = nextKey
    val source = days[index]
    val duplicate = source.copy(
        key = next++,
        name = "${source.name} copy",
        placements = source.placements.map { placement -> placement.copy(key = next++, sets = placement.sets.map { it.copy(key = next++) }) },
    )
    return copy(days = days.toMutableList().also { it.add(index + 1, duplicate) }, selectedDayKey = duplicate.key, selectedPlacementKey = null, nextKey = next)
}

internal fun RoutineBuilderDayState.movePlacement(key: Long, delta: Int): RoutineBuilderDayState {
    if (placements.firstOrNull { it.key == key }?.groupKey != null) {
        return movePlacementWithinGroup(key, delta)
    }
    val index = placements.indexOfFirst { it.key == key }
    if (index < 0) return this
    val moved = moveListItem(placements, index, delta)
    return if (moved == placements) this else copy(placements = moved)
}

private fun RoutineBuilderDayState.movePlacementBlock(blockIndex: Int, delta: Int): RoutineBuilderDayState {
    val blocks = placements.routinePlacementBlocks()
    val moved = moveListItem(blocks, blockIndex, delta)
    if (moved == blocks) return this
    return copy(placements = moved.flatMap(RoutinePlacementBlock::placements))
}

private fun RoutineBuilderDayState.movePlacementWithinGroup(key: Long, delta: Int): RoutineBuilderDayState {
    val group = placements.firstOrNull { it.key == key }?.groupKey ?: return movePlacement(key, delta)
    val members = placements.filter { it.groupKey == group }
    val memberIndex = members.indexOfFirst { it.key == key }
    if (memberIndex < 0) return this
    val moved = moveListItem(members, memberIndex, delta)
    if (moved == members) return this
    val iterator = moved.iterator()
    return copy(placements = placements.map { placement ->
        if (placement.groupKey == group) iterator.next() else placement
    })
}

internal fun applyRepPrescriptionScheme(
    existing: List<RoutineBuilderSetState>,
    scheme: RepPrescriptionScheme,
): List<RoutineBuilderSetState> {
    require(scheme.isValid())
    var key = nextLocalSetKey(existing)
    return List(scheme.setCount) { index ->
        val current = existing.getOrNull(index) ?: RoutineBuilderSetState(key++)
        current.copy(
            repetitionsMin = scheme.repetitionsMin.toString(),
            repetitionsMax = scheme.repetitionsMax.takeIf { it != scheme.repetitionsMin }?.toString().orEmpty(),
            classification = scheme.classification.name,
            restSeconds = scheme.restSeconds?.toString() ?: current.restSeconds,
        )
    }
}

private fun Exercise.supportsRepPrescription(): Boolean = trackingType !in setOf(
    ExerciseTrackingType.WeightOnly,
    ExerciseTrackingType.DistanceOnly,
    ExerciseTrackingType.DurationOnly,
    ExerciseTrackingType.DistanceDuration,
    ExerciseTrackingType.WeightDuration,
)

private fun nextLocalSetKey(sets: List<RoutineBuilderSetState>): Long = (sets.maxOfOrNull { it.key } ?: 0L) + 1L

private fun swapRoutineBuilderSets(
    sets: List<RoutineBuilderSetState>,
    firstKey: Long,
    secondKey: Long,
): List<RoutineBuilderSetState> {
    val first = sets.indexOfFirst { it.key == firstKey }
    val second = sets.indexOfFirst { it.key == secondKey }
    if (first < 0 || second < 0 || first == second) return sets
    return sets.toMutableList().also { reordered ->
        val value = reordered[first]
        reordered[first] = reordered[second]
        reordered[second] = value
    }
}

private data class FiveThreeOnePlacementProjection(
    val trainingMaxValue: String,
    val trainingMaxUnitId: String,
    val cycleIncrementValue: String,
    val trainingMaxSource: String,
    val trainingMaxPercent: String,
    val trainingMaxBasisKind: String,
    val trainingMaxBasisValue: String,
    val trainingMaxBasisUnitId: String,
    val trainingMaxIncreaseEligible: Boolean,
    val mainWorkScheme: String,
    val supplementalScheme: String,
    val jokerSetsEnabled: Boolean,
    val sets: List<RoutineBuilderSetState>,
)

private fun RoutineAssistanceRole.toBuilderAssistanceCategory(): RoutineAssistanceCategory = when (this) {
    RoutineAssistanceRole.Push -> RoutineAssistanceCategory.Push
    RoutineAssistanceRole.Pull -> RoutineAssistanceCategory.Pull
    RoutineAssistanceRole.SingleLegCore -> RoutineAssistanceCategory.SingleLegCore
    RoutineAssistanceRole.Other -> RoutineAssistanceCategory.Other
    RoutineAssistanceRole.Unspecified,
    RoutineAssistanceRole.MainLift,
    -> RoutineAssistanceCategory.Unspecified
}

private fun RoutineAssistanceRole.assistanceUiLabel(): String = when (this) {
    RoutineAssistanceRole.Push -> "Push"
    RoutineAssistanceRole.Pull -> "Pull"
    RoutineAssistanceRole.SingleLegCore -> "Single-leg / Core"
    RoutineAssistanceRole.Other -> "Other"
    RoutineAssistanceRole.Unspecified -> "Unclassified"
    RoutineAssistanceRole.MainLift -> "Main lift"
}

private fun RoutineAssistanceRole.assistanceUiDescription(): String = when (this) {
    RoutineAssistanceRole.Push -> "Pressing and triceps work"
    RoutineAssistanceRole.Pull -> "Rows, chins, pulldowns, curls, and upper-back work"
    RoutineAssistanceRole.SingleLegCore -> "Single-leg lower-body, abdominal, or lower-back work"
    RoutineAssistanceRole.Other -> "Assistance outside the three common categories"
    RoutineAssistanceRole.Unspecified -> "No routine assistance category"
    RoutineAssistanceRole.MainLift -> "Program-managed Main and Supplemental work"
}

private fun RoutineBuilderPlacementState.plannedAssistanceReps(): Int = sets.sumOf { set ->
    (set.repetitionsMin.toIntOrNull() ?: 0).coerceAtLeast(0)
}

internal fun RoutineBuilderPlacementState.withAssistanceCategory(
    category: RoutineAssistanceCategory?,
): RoutineBuilderPlacementState {
    val normalizedCategory = category ?: RoutineAssistanceCategory.Unspecified
    val isAssistance = category != null
    val legacyRole = when (normalizedCategory) {
        RoutineAssistanceCategory.Push -> RoutineAssistanceRole.Push
        RoutineAssistanceCategory.Pull -> RoutineAssistanceRole.Pull
        RoutineAssistanceCategory.SingleLegCore -> RoutineAssistanceRole.SingleLegCore
        RoutineAssistanceCategory.Other -> RoutineAssistanceRole.Other
        RoutineAssistanceCategory.Unspecified -> RoutineAssistanceRole.Unspecified
    }
    return copy(
        placementKind = if (isAssistance) RoutinePlacementKind.Assistance.name else RoutinePlacementKind.General.name,
        assistanceCategory = normalizedCategory.name,
        assistanceRole = legacyRole.name,
        sets = sets.filterNot { isAssistance && it.optionalWorkKind == RoutineOptionalWorkKind.Joker.name }.map { set ->
            set.copy(
                workSection = if (isAssistance) RoutineWorkSection.Assistance.name else RoutineWorkSection.Unspecified.name,
                optionalWorkKind = RoutineOptionalWorkKind.None.name,
                mainWorkScheme = null,
                supplementalScheme = null,
            )
        },
        mainWorkScheme = RoutineMainWorkScheme.Unspecified.name,
        supplementalScheme = RoutineSupplementalScheme.None.name,
        jokerSetsEnabled = false,
    )
}

internal fun RoutineBuilderPlacementState.withAssistanceRole(role: RoutineAssistanceRole): RoutineBuilderPlacementState =
    when (role) {
        RoutineAssistanceRole.MainLift -> copy(
            placementKind = RoutinePlacementKind.MainLift.name,
            assistanceCategory = RoutineAssistanceCategory.Unspecified.name,
            assistanceRole = RoutineAssistanceRole.MainLift.name,
        )
        RoutineAssistanceRole.Unspecified -> withAssistanceCategory(null)
        RoutineAssistanceRole.Push -> withAssistanceCategory(RoutineAssistanceCategory.Push)
        RoutineAssistanceRole.Pull -> withAssistanceCategory(RoutineAssistanceCategory.Pull)
        RoutineAssistanceRole.SingleLegCore -> withAssistanceCategory(RoutineAssistanceCategory.SingleLegCore)
        RoutineAssistanceRole.Other -> withAssistanceCategory(RoutineAssistanceCategory.Other)
    }

/**
 * Reconciles asynchronously created standard lifts. Automatic fallbacks may move to a better
 * role/name match as the library changes; indices the lifter selected manually are preserved.
 */
internal fun fillEmptyFiveThreeOneLiftSelections(
    currentIds: List<Long>,
    candidates: List<Pair<Long, String>>,
    manuallySelectedRoleIndices: Set<Int> = emptySet(),
): List<Long> {
    val roles = FiveThreeOneLiftRole.entries
    val candidateIds = candidates.mapTo(mutableSetOf(), Pair<Long, String>::first)
    val result = MutableList(roles.size) { index ->
        currentIds.getOrNull(index).orZero().takeIf { index in manuallySelectedRoleIndices && it in candidateIds } ?: 0L
    }
    val used = result.filterTo(mutableSetOf()) { it > 0L }
    roles.forEachIndexed { index, role ->
        if (result[index] > 0L) return@forEachIndexed
        val selected = candidates.firstOrNull { (id, name) -> id !in used && role.matchesExerciseName(name) }
            ?: candidates.firstOrNull { (id, _) -> id !in used }
        if (selected != null) {
            result[index] = selected.first
            used += selected.first
        }
    }
    return result
}

internal data class FiveThreeOneTrainingMaxEntryState(
    val explicitTrainingMax: String = "",
    val recentMaxOrEstimatedOneRepMax: String = "",
    val trainingMaxPercentage: String = "85",
) {
    fun suggestionOrNull(loadIncrement: Double, availableLoads: List<Double> = emptyList()): Double? {
        val recentMax = recentMaxOrEstimatedOneRepMax.toWhipDoubleOrNull()
            ?.takeIf { it.isFinite() && it > 0.0 } ?: return null
        val percentage = trainingMaxPercentage.toWhipDoubleOrNull()
            ?.takeIf { it.isFinite() && it in 1.0..100.0 } ?: return null
        return runCatching {
            suggestedFiveThreeOneTrainingMax(recentMax, loadIncrement, percentage, availableLoads)
        }.getOrNull()
    }

    /** Applies a suggestion once; later source-max changes cannot mutate the explicit TM. */
    fun applySuggestion(loadIncrement: Double, availableLoads: List<Double> = emptyList()): FiveThreeOneTrainingMaxEntryState {
        val suggestion = suggestionOrNull(loadIncrement, availableLoads) ?: return this
        return copy(explicitTrainingMax = editableNumericValue(suggestion))
    }
}

private fun RoutineBuilderState.normalizedProgramPhaseLabels(): List<String> =
    List(programPhaseCount) { index -> programPhaseLabels.getOrNull(index)?.takeIf(String::isNotBlank) ?: "Phase ${index + 1}" }

private fun RoutineBuilderState.normalizedProgramPhaseRoles(): List<RoutineProgramPhaseRole> =
    List(programPhaseCount) { index ->
        programPhaseRoles.getOrNull(index)?.let { value ->
            runCatching { RoutineProgramPhaseRole.valueOf(value) }.getOrNull()
        } ?: RoutineProgramPhaseRole.Standard
    }

internal data class FiveThreeOnePhasePolicyState(
    val mainWorkScheme: RoutineMainWorkScheme,
    val supplementalScheme: RoutineSupplementalScheme,
    val jokerEnabled: Boolean,
    val jokerCount: Int = if (jokerEnabled) 1 else 0,
    val jokerStepPercent: Double = 5.0,
    val alternateSupplementalExerciseId: Long? = null,
    val alternateSupplementalExerciseName: String? = null,
)

internal fun RoutineBuilderState.fiveThreeOnePhasePrescriptionSummary(
    phaseIndex: Int,
    exerciseId: Long?,
): List<String> {
    if (phaseIndex !in 0 until programPhaseCount || exerciseId == null) return emptyList()
    val day = days.firstOrNull { candidate ->
        candidate.placements.any {
            it.exerciseId == exerciseId && it.placementKind == RoutinePlacementKind.MainLift.name
        }
    } ?: return emptyList()
    val placement = day.placements.first {
        it.exerciseId == exerciseId && it.placementKind == RoutinePlacementKind.MainLift.name
    }
    val active = placement.sets.filter { it.routinePhaseIndex == null || it.routinePhaseIndex == phaseIndex }
    val alternate = day.placements.firstOrNull { candidate ->
        candidate.placementKind == RoutinePlacementKind.Supplemental.name && candidate.sets.any { set ->
            (set.routinePhaseIndex == null || set.routinePhaseIndex == phaseIndex) &&
                set.workSection == RoutineWorkSection.Supplemental.name
        }
    }
    val alternateActive = alternate?.sets.orEmpty().filter {
        (it.routinePhaseIndex == null || it.routinePhaseIndex == phaseIndex) &&
            it.workSection == RoutineWorkSection.Supplemental.name
    }
    fun RoutineBuilderSetState.shortPrescription(): String {
        val percent = loadPercentage.toWhipDoubleOrNull()?.let { "${editableNumericValue(it)}% TM" }
            ?: load.takeIf(String::isNotBlank)
            ?: "Unspecified load"
        val reps = repetitionsMin.takeIf(String::isNotBlank)?.let { value ->
            if (classification == WorkoutSetClassification.Amrap.name) "$value+" else value
        } ?: "?"
        return "$percent × $reps"
    }
    val summaries = listOf(
        RoutineWorkSection.Main to "Main",
        RoutineWorkSection.Supplemental to "Supplemental",
        RoutineWorkSection.Optional to "Optional",
    ).mapNotNull { (section, label) ->
        active.filter { it.workSection == section.name }.takeIf(List<RoutineBuilderSetState>::isNotEmpty)
            ?.joinToString(prefix = "$label · ", separator = " · ") { it.shortPrescription() }
    }.toMutableList()
    if (alternate != null && alternateActive.isNotEmpty()) {
        summaries.add(
            1.coerceAtMost(summaries.size),
            alternateActive.joinToString(
                prefix = "Supplemental · ${alternate.exerciseNameSnapshot} (alternate lift) · ",
                separator = " · ",
            ) { it.shortPrescription() },
        )
    }
    return summaries
}

/** Reads policy from the executable sets for one phase; placement fields are legacy fallback only. */
internal fun RoutineBuilderState.fiveThreeOnePhasePolicy(
    phaseIndex: Int,
    exerciseId: Long? = null,
): FiveThreeOnePhasePolicyState? {
    if (phaseIndex !in 0 until programPhaseCount) return null
    val day = days.firstOrNull { candidate ->
        candidate.placements.any {
            it.placementKind == RoutinePlacementKind.MainLift.name &&
                (exerciseId == null || it.exerciseId == exerciseId)
        }
    } ?: return null
    val placement = day.placements.first {
        it.placementKind == RoutinePlacementKind.MainLift.name &&
            (exerciseId == null || it.exerciseId == exerciseId)
    }
    val active = placement.sets.filter { it.routinePhaseIndex == null || it.routinePhaseIndex == phaseIndex }
    val main = active.filter { it.workSection == RoutineWorkSection.Main.name }
    val mainPlacementSupplemental = active.filter { it.workSection == RoutineWorkSection.Supplemental.name }
    val alternatePlacement = day.placements.firstOrNull { candidate ->
        candidate.placementKind == RoutinePlacementKind.Supplemental.name && candidate.sets.any { set ->
            (set.routinePhaseIndex == null || set.routinePhaseIndex == phaseIndex) &&
                set.workSection == RoutineWorkSection.Supplemental.name
        }
    }
    val alternateSupplemental = alternatePlacement?.sets.orEmpty().filter {
        (it.routinePhaseIndex == null || it.routinePhaseIndex == phaseIndex) &&
            it.workSection == RoutineWorkSection.Supplemental.name
    }
    val supplemental = mainPlacementSupplemental.ifEmpty { alternateSupplemental }
    val explicitMain = main.mapNotNull { set ->
        set.mainWorkScheme?.let { runCatching { RoutineMainWorkScheme.valueOf(it) }.getOrNull() }
    }.distinct()
    val mainScheme = explicitMain.singleOrNull() ?: when {
        main.any { it.classification == WorkoutSetClassification.Amrap.name } -> RoutineMainWorkScheme.ClassicPrSet
        main.isNotEmpty() && main.all { it.repetitionsMin == "5" } &&
            placement.mainWorkScheme == RoutineMainWorkScheme.FivesPro.name -> RoutineMainWorkScheme.FivesPro
        main.isNotEmpty() -> RoutineMainWorkScheme.ClassicMinimumReps
        else -> RoutineMainWorkScheme.Unspecified
    }
    val explicitSupplemental = supplemental.mapNotNull { set ->
        set.supplementalScheme?.let { runCatching { RoutineSupplementalScheme.valueOf(it) }.getOrNull() }
    }.distinct()
    val supplementalScheme = explicitSupplemental.singleOrNull() ?: when {
        supplemental.isEmpty() -> RoutineSupplementalScheme.None
        supplemental.size == 5 && supplemental.all { it.repetitionsMin == "10" } ->
            RoutineSupplementalScheme.BoringButBig
        supplemental.size == 10 && supplemental.all { it.repetitionsMin == "5" } ->
            RoutineSupplementalScheme.BoringButStrong
        supplemental.size == 5 && supplemental.all { it.repetitionsMin == "5" } -> {
            val mainPercentages = main.map(RoutineBuilderSetState::loadPercentage)
            when {
                supplemental.all { it.loadPercentage == mainPercentages.getOrNull(0) } ->
                    RoutineSupplementalScheme.FirstSetLast
                supplemental.all { it.loadPercentage == mainPercentages.getOrNull(1) } ->
                    RoutineSupplementalScheme.SecondSetLast
                else -> RoutineSupplementalScheme.Custom
            }
        }
        else -> RoutineSupplementalScheme.Custom
    }
    val jokers = active.filter {
        it.workSection == RoutineWorkSection.Optional.name &&
            it.optionalWorkKind == RoutineOptionalWorkKind.Joker.name
    }.sortedBy { it.loadPercentage.toWhipDoubleOrNull() ?: Double.MAX_VALUE }
    val topMainPercentage = main.lastOrNull()?.loadPercentage?.toWhipDoubleOrNull()
    val jokerStep = jokers.firstOrNull()?.loadPercentage?.toWhipDoubleOrNull()?.let { first ->
        (first - (topMainPercentage ?: first - 5.0)).takeIf { it == 10.0 } ?: 5.0
    } ?: 5.0
    return FiveThreeOnePhasePolicyState(
        mainWorkScheme = mainScheme,
        supplementalScheme = supplementalScheme,
        jokerEnabled = jokers.isNotEmpty(),
        jokerCount = jokers.size.coerceAtMost(3),
        jokerStepPercent = jokerStep,
        alternateSupplementalExerciseId = alternatePlacement?.exerciseId?.takeIf { alternateSupplemental.isNotEmpty() },
        alternateSupplementalExerciseName = alternatePlacement?.exerciseNameSnapshot?.takeIf {
            alternateSupplemental.isNotEmpty()
        },
    )
}

/**
 * Rewrites only one phase's executable Main/Supplemental/Joker work across mapped main lifts.
 * Phase-null supplemental work is first expanded across every phase so changing one phase can
 * never leave both a global and a phase-specific prescription active.
 */
internal fun RoutineBuilderState.applyFiveThreeOnePhasePolicy(
    phaseIndex: Int,
    mainWorkScheme: RoutineMainWorkScheme,
    supplementalScheme: RoutineSupplementalScheme,
    jokerEnabled: Boolean,
    jokerCount: Int = if (jokerEnabled) 1 else 0,
    jokerStepPercent: Double = 5.0,
    exerciseId: Long? = null,
): RoutineBuilderState {
    if (!programKind.isFiveThreeOneProgramKindName() || phaseIndex !in 0 until programPhaseCount) return this
    require(jokerCount in 0..3)
    require(jokerStepPercent == 5.0 || jokerStepPercent == 10.0)
    var next = nextKey
    fun nextSetKey(): Long = next++

    fun materializeGlobalSupplemental(sets: List<RoutineBuilderSetState>): List<RoutineBuilderSetState> {
        val global = sets.filter {
            it.workSection == RoutineWorkSection.Supplemental.name && it.routinePhaseIndex == null
        }
        if (global.isEmpty()) return sets
        val retained = sets.filterNot(global::contains)
        return retained + (0 until programPhaseCount).flatMap { phase ->
            global.map { set -> set.copy(key = nextSetKey(), routinePhaseIndex = phase) }
        }
    }

    fun classicRepetitions(main: List<RoutineBuilderSetState>): List<Int> {
        val percentages = main.mapNotNull { it.loadPercentage.toWhipDoubleOrNull() }
        fun matches(expected: List<Double>) = percentages.size == expected.size &&
            percentages.zip(expected).all { (actual, target) -> (actual - target).absoluteValue < 0.001 }
        return when {
            matches(listOf(70.0, 80.0, 90.0)) -> listOf(3, 3, 3)
            matches(listOf(75.0, 85.0, 95.0)) -> listOf(5, 3, 1)
            else -> main.map { it.repetitionsMin.toIntOrNull() ?: 5 }
        }
    }

    val updatedDays = days.map { day ->
        val targetedMainInDay = day.placements.any { placement ->
            placement.placementKind == RoutinePlacementKind.MainLift.name &&
                (exerciseId == null || placement.exerciseId == exerciseId)
        }
        val activeAlternatePlacement = day.placements.firstOrNull { placement ->
            placement.placementKind == RoutinePlacementKind.Supplemental.name && placement.sets.any { set ->
                (set.routinePhaseIndex == null || set.routinePhaseIndex == phaseIndex) &&
                    set.workSection == RoutineWorkSection.Supplemental.name
            }
        }
        val activeAlternateScheme = activeAlternatePlacement?.sets?.firstNotNullOfOrNull { set ->
            if ((set.routinePhaseIndex == null || set.routinePhaseIndex == phaseIndex) &&
                set.workSection == RoutineWorkSection.Supplemental.name
            ) {
                set.supplementalScheme?.let { runCatching { RoutineSupplementalScheme.valueOf(it) }.getOrNull() }
            } else null
        } ?: activeAlternatePlacement?.supplementalScheme?.let { value ->
            runCatching { RoutineSupplementalScheme.valueOf(value) }.getOrNull()
        }
        day.copy(placements = day.placements.map { placement ->
            if (targetedMainInDay && placement.placementKind == RoutinePlacementKind.Supplemental.name) {
                if (
                    placement.key == activeAlternatePlacement?.key &&
                    supplementalScheme == activeAlternateScheme
                ) return@map placement
                val retained = materializeGlobalSupplemental(placement.sets).filterNot { set ->
                    set.routinePhaseIndex == phaseIndex && set.workSection == RoutineWorkSection.Supplemental.name
                }
                return@map placement.copy(
                    sets = retained,
                    supplementalScheme = RoutineSupplementalScheme.Custom.name,
                )
            }
            if (
                placement.placementKind != RoutinePlacementKind.MainLift.name ||
                (exerciseId != null && placement.exerciseId != exerciseId)
            ) return@map placement
            val materialized = materializeGlobalSupplemental(placement.sets)
            val selectedMain = materialized.filter {
                it.routinePhaseIndex == phaseIndex && it.workSection == RoutineWorkSection.Main.name
            }
            val selectedSupplemental = materialized.filter {
                it.routinePhaseIndex == phaseIndex && it.workSection == RoutineWorkSection.Supplemental.name
            }
            val existingMainWorkScheme = selectedMain.firstNotNullOfOrNull { set ->
                set.mainWorkScheme?.let { runCatching { RoutineMainWorkScheme.valueOf(it) }.getOrNull() }
            } ?: runCatching { RoutineMainWorkScheme.valueOf(placement.mainWorkScheme) }
                .getOrDefault(RoutineMainWorkScheme.Unspecified)
            val existingSupplementalScheme = selectedSupplemental.firstNotNullOfOrNull { set ->
                set.supplementalScheme?.let { runCatching { RoutineSupplementalScheme.valueOf(it) }.getOrNull() }
            } ?: activeAlternateScheme?.takeIf { selectedSupplemental.isEmpty() }
                ?: if (selectedSupplemental.isEmpty()) {
                    RoutineSupplementalScheme.None
                } else {
                    runCatching { RoutineSupplementalScheme.valueOf(placement.supplementalScheme) }
                        .getOrDefault(RoutineSupplementalScheme.Custom)
                }
            val mainSchemeChanged = existingMainWorkScheme != mainWorkScheme
            val supplementalSchemeChanged = existingSupplementalScheme != supplementalScheme
            val retained = materialized.filterNot { set ->
                set.routinePhaseIndex == phaseIndex &&
                    (set.workSection == RoutineWorkSection.Main.name ||
                        set.workSection == RoutineWorkSection.Supplemental.name ||
                        (set.workSection == RoutineWorkSection.Optional.name &&
                            set.optionalWorkKind == RoutineOptionalWorkKind.Joker.name))
            }
            val classicReps = classicRepetitions(selectedMain)
            val rewrittenMain = if (!mainSchemeChanged) selectedMain else selectedMain.mapIndexed { index, set ->
                when (mainWorkScheme) {
                    RoutineMainWorkScheme.Unspecified -> set.copy(mainWorkScheme = null)
                    RoutineMainWorkScheme.FivesPro -> set.copy(
                        repetitionsMin = "5",
                        repetitionsMax = "",
                        classification = WorkoutSetClassification.Working.name,
                        mainWorkScheme = mainWorkScheme.name,
                    )
                    RoutineMainWorkScheme.ClassicMinimumReps,
                    RoutineMainWorkScheme.ClassicPrSet,
                    -> set.copy(
                        repetitionsMin = classicReps.getOrElse(index) { 5 }.toString(),
                        repetitionsMax = "",
                        classification = if (
                            mainWorkScheme == RoutineMainWorkScheme.ClassicPrSet && index == selectedMain.lastIndex
                        ) WorkoutSetClassification.Amrap.name else WorkoutSetClassification.Working.name,
                        mainWorkScheme = mainWorkScheme.name,
                    )
                }
            }
            val mainPercentages = rewrittenMain.mapNotNull { it.loadPercentage.toWhipDoubleOrNull() }
            val supplementalPercent = when (supplementalScheme) {
                RoutineSupplementalScheme.FirstSetLast,
                RoutineSupplementalScheme.BoringButStrong,
                -> mainPercentages.getOrNull(0)
                RoutineSupplementalScheme.SecondSetLast -> mainPercentages.getOrNull(1)
                RoutineSupplementalScheme.BoringButBig -> selectedSupplemental
                    .takeIf { existing -> existing.firstOrNull()?.supplementalScheme == supplementalScheme.name }
                    ?.firstOrNull()?.loadPercentage?.toWhipDoubleOrNull() ?: 50.0
                RoutineSupplementalScheme.None,
                RoutineSupplementalScheme.Custom,
                -> null
            }
            val rewrittenSupplemental = if (!supplementalSchemeChanged) {
                selectedSupplemental
            } else when (supplementalScheme) {
                RoutineSupplementalScheme.None -> emptyList()
                RoutineSupplementalScheme.Custom -> selectedSupplemental.map {
                    it.copy(supplementalScheme = RoutineSupplementalScheme.Custom.name)
                }
                else -> {
                    val count = if (supplementalScheme == RoutineSupplementalScheme.BoringButStrong) 10 else 5
                    val reps = if (supplementalScheme == RoutineSupplementalScheme.BoringButBig) 10 else 5
                    val percentage = supplementalPercent
                    if (percentage == null) emptyList() else List(count) {
                        RoutineBuilderSetState(
                            key = nextSetKey(),
                            repetitionsMin = reps.toString(),
                            classification = WorkoutSetClassification.BackOff.name,
                            note = "Supplemental · ${supplementalScheme.name} · ${editableNumericValue(percentage)}% TM",
                            loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax.name,
                            loadPercentage = editableNumericValue(percentage),
                            routinePhaseIndex = phaseIndex,
                            workSection = RoutineWorkSection.Supplemental.name,
                            supplementalScheme = supplementalScheme.name,
                        )
                    }
                }
            }
            val rewrittenJoker = if (jokerEnabled && jokerCount > 0 && rewrittenMain.isNotEmpty()) {
                val top = rewrittenMain.last()
                List(jokerCount) { index ->
                    val percentage = (top.loadPercentage.toWhipDoubleOrNull() ?: 95.0) + jokerStepPercent * (index + 1)
                    RoutineBuilderSetState(
                        key = nextSetKey(),
                        repetitionsMin = top.repetitionsMin.ifBlank { "1" },
                        classification = WorkoutSetClassification.Working.name,
                        note = "Optional Joker ${index + 1} of $jokerCount · ${editableNumericValue(percentage)}% TM",
                        loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax.name,
                        loadPercentage = editableNumericValue(percentage),
                        routinePhaseIndex = phaseIndex,
                        workSection = RoutineWorkSection.Optional.name,
                        optionalWorkKind = RoutineOptionalWorkKind.Joker.name,
                    )
                }
            } else emptyList()
            val combined = retained + rewrittenMain + rewrittenJoker + rewrittenSupplemental
            val phaseMainSchemes = (0 until programPhaseCount).mapNotNull { phase ->
                combined.firstOrNull {
                    it.routinePhaseIndex == phase && it.workSection == RoutineWorkSection.Main.name
                }?.mainWorkScheme
            }.distinct()
            val phaseSupplementalSchemes = (0 until programPhaseCount).map { phase ->
                combined.firstOrNull {
                    it.routinePhaseIndex == phase && it.workSection == RoutineWorkSection.Supplemental.name
                }?.supplementalScheme ?: RoutineSupplementalScheme.None.name
            }.distinct()
            placement.copy(
                sets = combined,
                mainWorkScheme = phaseMainSchemes.singleOrNull() ?: RoutineMainWorkScheme.Unspecified.name,
                supplementalScheme = phaseSupplementalSchemes.singleOrNull() ?: RoutineSupplementalScheme.Custom.name,
                jokerSetsEnabled = combined.any {
                    it.workSection == RoutineWorkSection.Optional.name &&
                        it.optionalWorkKind == RoutineOptionalWorkKind.Joker.name
                },
            )
        })
    }
    return copy(days = updatedDays, nextKey = next)
}

/**
 * Toggles only the optional Joker prescription for one phase. Main and Supplemental sets are
 * deliberately left as the exact same objects in the exact same order: a Joker is additive work,
 * never a replacement for the last BBB/FSL/SSL/BBS/custom Supplemental set.
 */
internal fun RoutineBuilderState.setFiveThreeOneJokerEnabled(
    phaseIndex: Int,
    enabled: Boolean,
    exerciseId: Long? = null,
): RoutineBuilderState = setFiveThreeOneJokerLadder(
    phaseIndex = phaseIndex,
    count = if (enabled) 1 else 0,
    stepPercent = 5.0,
    exerciseId = exerciseId,
)

/** Replaces only a phase's ordered optional Joker rows; Main and Supplemental objects are retained. */
internal fun RoutineBuilderState.setFiveThreeOneJokerLadder(
    phaseIndex: Int,
    count: Int,
    stepPercent: Double,
    exerciseId: Long? = null,
): RoutineBuilderState {
    if (!programKind.isFiveThreeOneProgramKindName() || phaseIndex !in 0 until programPhaseCount) return this
    require(count in 0..3) { "Joker ladder must contain from zero to three candidates" }
    require(stepPercent == 5.0 || stepPercent == 10.0) { "Joker steps must be 5% or 10% of Training Max" }
    val highestExistingKey = days.asSequence()
        .flatMap { it.placements.asSequence() }
        .flatMap { it.sets.asSequence() }
        .maxOfOrNull(RoutineBuilderSetState::key)
        ?: 0L
    var next = maxOf(nextKey, highestExistingKey + 1L)
    val updatedDays = days.map { day ->
        day.copy(placements = day.placements.map { placement ->
            if (
                placement.placementKind != RoutinePlacementKind.MainLift.name ||
                (exerciseId != null && placement.exerciseId != exerciseId)
            ) return@map placement
            val isPhaseJoker: (RoutineBuilderSetState) -> Boolean = { set ->
                set.routinePhaseIndex == phaseIndex &&
                    set.workSection == RoutineWorkSection.Optional.name &&
                    set.optionalWorkKind == RoutineOptionalWorkKind.Joker.name
            }
            val retained = placement.sets.filterNot(isPhaseJoker)
            val topMainIndex = retained.indexOfLast { set ->
                set.routinePhaseIndex == phaseIndex && set.workSection == RoutineWorkSection.Main.name
            }
            val topMain = retained.getOrNull(topMainIndex)
            val jokers = if (count == 0 || topMain == null) emptyList() else List(count) { index ->
                val percentage = (topMain.loadPercentage.toWhipDoubleOrNull() ?: 95.0) + stepPercent * (index + 1)
                RoutineBuilderSetState(
                    key = next++,
                    repetitionsMin = topMain.repetitionsMin.ifBlank { "1" },
                    classification = WorkoutSetClassification.Working.name,
                    note = "Optional Joker ${index + 1} of $count · ${editableNumericValue(percentage)}% TM",
                    loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax.name,
                    loadPercentage = editableNumericValue(percentage),
                    routinePhaseIndex = phaseIndex,
                    workSection = RoutineWorkSection.Optional.name,
                    optionalWorkKind = RoutineOptionalWorkKind.Joker.name,
                )
            }
            val updatedSets = if (jokers.isEmpty()) retained else retained.toMutableList().also { sets ->
                sets.addAll(topMainIndex + 1, jokers)
            }
            placement.copy(
                sets = updatedSets,
                jokerSetsEnabled = updatedSets.any { set ->
                    set.workSection == RoutineWorkSection.Optional.name &&
                        set.optionalWorkKind == RoutineOptionalWorkKind.Joker.name
                },
            )
        })
    }
    return copy(days = updatedDays, nextKey = next)
}

/** Removes optional Joker prescriptions without normalizing any lift's Main or Supplemental policy. */
internal fun RoutineBuilderState.removeFiveThreeOneJokers(
    phaseIndex: Int,
    exerciseId: Long? = null,
): RoutineBuilderState = setFiveThreeOneJokerEnabled(
    phaseIndex = phaseIndex,
    enabled = false,
    exerciseId = exerciseId,
)

/** Applies one complete 7th Week prescription to a phase without touching other phases/history. */
internal fun RoutineBuilderState.applyFiveThreeOneSeventhWeekProtocol(
    phaseIndex: Int,
    protocol: FiveThreeOneSeventhWeekProtocol,
): RoutineBuilderState {
    if (!programKind.isFiveThreeOneProgramKindName() || phaseIndex !in 0 until programPhaseCount) return this
    val highestKey = days.asSequence().flatMap { it.placements.asSequence() }
        .flatMap { it.sets.asSequence() }.maxOfOrNull(RoutineBuilderSetState::key) ?: 0L
    var next = maxOf(nextKey, highestKey + 1L)
    val protocolOwnerDayByExerciseId = balancedOncePerLiftDayOwners(
        days.map { day ->
            day.placements.filter { it.placementKind == RoutinePlacementKind.MainLift.name }
                .map(RoutineBuilderPlacementState::exerciseId)
        },
    )

    fun materializeGlobalSupplemental(sets: List<RoutineBuilderSetState>): List<RoutineBuilderSetState> {
        val global = sets.filter {
            it.workSection == RoutineWorkSection.Supplemental.name && it.routinePhaseIndex == null
        }
        if (global.isEmpty()) return sets
        return sets.filterNot(global::contains) + (0 until programPhaseCount).flatMap { phase ->
            global.map { set -> set.copy(key = next++, routinePhaseIndex = phase) }
        }
    }

    val protocolMainScheme = if (protocol == FiveThreeOneSeventhWeekProtocol.PersonalRecordTest) {
        RoutineMainWorkScheme.ClassicPrSet
    } else {
        RoutineMainWorkScheme.ClassicMinimumReps
    }
    val updated = days.mapIndexed { dayIndex, day ->
        day.copy(placements = day.placements.map { placement ->
            val kind = runCatching { RoutinePlacementKind.valueOf(placement.placementKind) }
                .getOrDefault(RoutinePlacementKind.General)
            when (kind) {
                RoutinePlacementKind.MainLift -> {
                    val materialized = materializeGlobalSupplemental(placement.sets)
                    val retained = materialized.filterNot { set ->
                        set.routinePhaseIndex == phaseIndex && (
                            set.workSection == RoutineWorkSection.Main.name ||
                                set.workSection == RoutineWorkSection.Supplemental.name ||
                                set.optionalWorkKind == RoutineOptionalWorkKind.Joker.name
                            )
                    }
                    val plans = fiveThreeOneSeventhWeekSetPlans(protocol).filterNot { plan ->
                        protocol == FiveThreeOneSeventhWeekProtocol.TrainingMaxTest &&
                            protocolOwnerDayByExerciseId[placement.exerciseId] != dayIndex &&
                            plan.classification == WorkoutSetClassification.TrainingMaxTest
                    }
                    val generated = fiveThreeOneBuilderSets(
                        existingSets = emptyList(),
                        previews = plans.map { FiveThreeOnePreviewSet(it, 0.0) },
                        mainWorkScheme = protocolMainScheme,
                        supplementalScheme = RoutineSupplementalScheme.None,
                        routinePhaseIndexOverride = phaseIndex,
                    ).map { set -> set.copy(key = next++) }
                    val combined = retained + generated
                    placement.copy(
                        sets = combined,
                        mainWorkScheme = RoutineMainWorkScheme.Unspecified.name,
                        supplementalScheme = RoutineSupplementalScheme.Custom.name,
                        jokerSetsEnabled = combined.any {
                            it.optionalWorkKind == RoutineOptionalWorkKind.Joker.name
                        },
                    )
                }
                RoutinePlacementKind.Supplemental -> {
                    val retained = materializeGlobalSupplemental(placement.sets).filterNot { set ->
                        set.routinePhaseIndex == phaseIndex && set.workSection == RoutineWorkSection.Supplemental.name
                    }
                    placement.copy(
                        sets = retained,
                        supplementalScheme = RoutineSupplementalScheme.Custom.name,
                    )
                }
                RoutinePlacementKind.General,
                RoutinePlacementKind.Assistance,
                -> placement
            }
        })
    }
    return copy(
        days = updated,
        nextKey = next,
        programTemplateKey = programTemplateKey.takeUnless { it == RoutineProgramTemplateKey.None.name }
            ?: RoutineProgramTemplateKey.FiveThreeOneCustom.name,
        programTemplateRevision = maxOf(
            programTemplateRevision,
            FIVE_THREE_ONE_ONCE_PER_LIFT_PROTOCOL_REVISION,
        ),
    )
        .updateProgramPhaseMetadata(
            phaseIndex = phaseIndex,
            label = "7th Week · ${protocol.label}",
            role = protocol.phaseRole().asOncePerLiftProtocol(),
            advancesTrainingMax = true,
        )
}

internal fun RoutineBuilderState.updateProgramPhaseMetadata(
    phaseIndex: Int,
    label: String = normalizedProgramPhaseLabels().getOrElse(phaseIndex) { "" },
    role: RoutineProgramPhaseRole = normalizedProgramPhaseRoles().getOrElse(phaseIndex) { RoutineProgramPhaseRole.Standard },
    advancesTrainingMax: Boolean = phaseIndex in trainingMaxAdvanceAfterPhaseIndices,
): RoutineBuilderState {
    if (phaseIndex !in 0 until programPhaseCount) return this
    val labels = normalizedProgramPhaseLabels().toMutableList().also { it[phaseIndex] = label.take(80) }
    val roles = normalizedProgramPhaseRoles().toMutableList().also { it[phaseIndex] = role }
    val boundaries = trainingMaxAdvanceAfterPhaseIndices.toMutableSet().also {
        if (advancesTrainingMax) it += phaseIndex else it -= phaseIndex
    }
    return copy(
        programPhaseLabels = labels,
        programPhaseRoles = roles.map(RoutineProgramPhaseRole::name),
        trainingMaxAdvanceAfterPhaseIndices = boundaries,
    )
}

/** Appends a custom phase by copying every prescription that references [sourcePhaseIndex]. */
internal fun RoutineBuilderState.addProgramPhase(sourcePhaseIndex: Int = programPhaseCount - 1): RoutineBuilderState {
    if (programPhaseCount >= 52 || sourcePhaseIndex !in 0 until programPhaseCount) return this
    var next = nextKey
    val newPhaseIndex = programPhaseCount
    val copiedDays = days.map { day ->
        day.copy(placements = day.placements.map { placement ->
            val copiedSets = placement.sets.filter { it.routinePhaseIndex == sourcePhaseIndex }
                .map { set -> set.copy(key = next++, routinePhaseIndex = newPhaseIndex) }
            val insertionIndex = placement.sets.indexOfFirst { it.routinePhaseIndex == null }
                .takeIf { it >= 0 } ?: placement.sets.size
            placement.copy(
                sets = placement.sets.toMutableList().also { sets -> sets.addAll(insertionIndex, copiedSets) },
            )
        })
    }
    val labels = normalizedProgramPhaseLabels()
    val roles = normalizedProgramPhaseRoles()
    return copy(
        days = copiedDays,
        nextKey = next,
        programPhaseCount = programPhaseCount + 1,
        programPhaseLabels = labels + "${labels[sourcePhaseIndex]} Copy",
        programPhaseRoles = (roles + roles[sourcePhaseIndex]).map(RoutineProgramPhaseRole::name),
        // Copying prescriptions does not imply a new TM-advance boundary.
        trainingMaxAdvanceAfterPhaseIndices = trainingMaxAdvanceAfterPhaseIndices,
    )
}

/** Removes one custom phase and reindexes every later prescription and TM boundary. */
internal fun RoutineBuilderState.removeProgramPhase(phaseIndex: Int): RoutineBuilderState {
    if (programPhaseCount <= 1 || phaseIndex !in 0 until programPhaseCount) return this
    val updatedDays = days.map { day ->
        day.copy(placements = day.placements.map { placement ->
            placement.copy(sets = placement.sets.mapNotNull { set ->
                when {
                    set.routinePhaseIndex == phaseIndex -> null
                    set.routinePhaseIndex != null && set.routinePhaseIndex > phaseIndex ->
                        set.copy(routinePhaseIndex = set.routinePhaseIndex - 1)
                    else -> set
                }
            })
        })
    }
    val boundaries = trainingMaxAdvanceAfterPhaseIndices.mapNotNullTo(mutableSetOf()) { boundary ->
        when {
            boundary == phaseIndex -> null
            boundary > phaseIndex -> boundary - 1
            else -> boundary
        }
    }
    return copy(
        days = updatedDays,
        programPhaseCount = programPhaseCount - 1,
        programPhaseLabels = normalizedProgramPhaseLabels().filterIndexed { index, _ -> index != phaseIndex },
        programPhaseRoles = normalizedProgramPhaseRoles().filterIndexed { index, _ -> index != phaseIndex }
            .map(RoutineProgramPhaseRole::name),
        trainingMaxAdvanceAfterPhaseIndices = boundaries,
        currentProgramPhaseIndexHint = currentProgramPhaseIndexHint?.let { current ->
            when {
                current == phaseIndex -> phaseIndex.coerceAtMost(programPhaseCount - 2)
                current > phaseIndex -> current - 1
                else -> current
            }
        },
    )
}

/** Reorders phase metadata, prescriptions, and TM boundaries as one atomic draft operation. */
internal fun RoutineBuilderState.moveProgramPhase(fromIndex: Int, toIndex: Int): RoutineBuilderState {
    if (fromIndex !in 0 until programPhaseCount || toIndex !in 0 until programPhaseCount || fromIndex == toIndex) {
        return this
    }
    val originalOrder = (0 until programPhaseCount).toMutableList()
    val moved = originalOrder.removeAt(fromIndex)
    originalOrder.add(toIndex, moved)
    val newIndexByOldIndex = originalOrder.withIndex().associate { (newIndex, oldIndex) -> oldIndex to newIndex }
    val labels = normalizedProgramPhaseLabels()
    val roles = normalizedProgramPhaseRoles()
    return copy(
        days = days.map { day ->
            day.copy(placements = day.placements.map { placement ->
                placement.copy(sets = placement.sets.map { set ->
                    set.routinePhaseIndex?.let { oldIndex ->
                        set.copy(routinePhaseIndex = newIndexByOldIndex[oldIndex] ?: oldIndex)
                    } ?: set
                })
            })
        },
        programPhaseLabels = originalOrder.map(labels::get),
        programPhaseRoles = originalOrder.map { roles[it].name },
        trainingMaxAdvanceAfterPhaseIndices = trainingMaxAdvanceAfterPhaseIndices.mapNotNullTo(mutableSetOf()) {
            newIndexByOldIndex[it]
        },
        currentProgramPhaseIndexHint = currentProgramPhaseIndexHint?.let { newIndexByOldIndex[it] },
    )
}

private fun Long?.orZero(): Long = this ?: 0L

private fun RoutineBuilderPlacementState.fiveThreeOneProjection() = FiveThreeOnePlacementProjection(
    trainingMaxValue,
    trainingMaxUnitId,
    cycleIncrementValue,
    trainingMaxSource,
    trainingMaxPercent,
    trainingMaxBasisKind,
    trainingMaxBasisValue,
    trainingMaxBasisUnitId,
    trainingMaxIncreaseEligible,
    mainWorkScheme,
    supplementalScheme,
    jokerSetsEnabled,
    sets,
)

/** Keeps repeated main-lift placements in one program from silently drifting apart. */
internal fun RoutineBuilderState.updateProgramPlacement(
    placementKey: Long,
    transform: (RoutineBuilderPlacementState) -> RoutineBuilderPlacementState,
): RoutineBuilderState {
    val before = days.asSequence().flatMap { it.placements.asSequence() }.firstOrNull { it.key == placementKey } ?: return this
    val after = transform(before)
    val shouldSync = programKind.isFiveThreeOneProgramKindName() &&
        after.placementKind == RoutinePlacementKind.MainLift.name &&
        before.fiveThreeOneProjection() != after.fiveThreeOneProjection()
    val trainingMaxChanged = before.trainingMaxValue != after.trainingMaxValue ||
        before.trainingMaxUnitId != after.trainingMaxUnitId ||
        before.cycleIncrementValue != after.cycleIncrementValue ||
        before.trainingMaxSource != after.trainingMaxSource ||
        before.trainingMaxPercent != after.trainingMaxPercent ||
        before.trainingMaxBasisKind != after.trainingMaxBasisKind ||
        before.trainingMaxBasisValue != after.trainingMaxBasisValue ||
        before.trainingMaxBasisUnitId != after.trainingMaxBasisUnitId
    val repeatedPlacements = days.asSequence()
        .flatMap { it.placements.asSequence() }
        .filter {
            it.exerciseId == after.exerciseId &&
                it.placementKind == RoutinePlacementKind.MainLift.name
        }
        .toList()
    fun RoutineBuilderSetState.isTrainingMaxTest() =
        classification == WorkoutSetClassification.TrainingMaxTest.name
    val trainingMaxTestOwnerByPhase = mutableMapOf<Int?, Long>()
    repeatedPlacements.forEach { placement ->
        placement.sets.filter { it.isTrainingMaxTest() }.forEach { set ->
            trainingMaxTestOwnerByPhase.putIfAbsent(set.routinePhaseIndex, placement.key)
        }
    }
    val testPhasesBefore = before.sets.filter { it.isTrainingMaxTest() }.mapTo(mutableSetOf()) { it.routinePhaseIndex }
    val testPhasesAfter = after.sets.filter { it.isTrainingMaxTest() }.mapTo(mutableSetOf()) { it.routinePhaseIndex }
    (testPhasesBefore + testPhasesAfter).forEach { phase ->
        if (phase in testPhasesAfter) trainingMaxTestOwnerByPhase[phase] = placementKey
        else trainingMaxTestOwnerByPhase.remove(phase)
    }
    val sharedNonTestSets = after.sets.filterNot { it.isTrainingMaxTest() }
    var next = nextKey
    val updatedDays = days.map { day ->
        day.copy(placements = day.placements.map { placement ->
            when {
                placement.key == placementKey -> after
                shouldSync && placement.exerciseId == after.exerciseId &&
                    placement.placementKind == RoutinePlacementKind.MainLift.name -> {
                    val ownedTrainingMaxTests = placement.sets.filter { set ->
                        set.isTrainingMaxTest() && trainingMaxTestOwnerByPhase[set.routinePhaseIndex] == placement.key
                    }
                    val synchronizedSets = (sharedNonTestSets + ownedTrainingMaxTests)
                        .sortedBy { it.routinePhaseIndex ?: Int.MAX_VALUE }
                    val rekeyed = synchronizedSets.map { set -> set.copy(key = next++) }
                    placement.copy(
                        sets = rekeyed,
                        copyPreviousWorkout = after.copyPreviousWorkout,
                        trainingMaxValue = after.trainingMaxValue,
                        trainingMaxUnitId = after.trainingMaxUnitId,
                        cycleIncrementValue = after.cycleIncrementValue,
                        trainingMaxSource = after.trainingMaxSource,
                        trainingMaxPercent = after.trainingMaxPercent,
                        trainingMaxBasisKind = after.trainingMaxBasisKind,
                        trainingMaxBasisValue = after.trainingMaxBasisValue,
                        trainingMaxBasisUnitId = after.trainingMaxBasisUnitId,
                        trainingMaxIncreaseEligible = after.trainingMaxIncreaseEligible,
                        mainWorkScheme = after.mainWorkScheme,
                        supplementalScheme = after.supplementalScheme,
                        jokerSetsEnabled = after.jokerSetsEnabled,
                    )
                }
                trainingMaxChanged && placement.exerciseId == after.exerciseId &&
                    placement.placementKind == RoutinePlacementKind.Supplemental.name -> placement.copy(
                    trainingMaxValue = after.trainingMaxValue,
                    trainingMaxUnitId = after.trainingMaxUnitId,
                    cycleIncrementValue = after.cycleIncrementValue,
                    trainingMaxSource = after.trainingMaxSource,
                    trainingMaxPercent = after.trainingMaxPercent,
                    trainingMaxBasisKind = after.trainingMaxBasisKind,
                    trainingMaxBasisValue = after.trainingMaxBasisValue,
                    trainingMaxBasisUnitId = after.trainingMaxBasisUnitId,
                    trainingMaxIncreaseEligible = after.trainingMaxIncreaseEligible,
                )
                else -> placement
            }
        })
    }
    return copy(days = updatedDays, nextKey = next)
}

private fun nextGroupName(day: RoutineBuilderDayState): String {
    val used = day.placements.mapNotNull { it.groupKey }.toSet()
    var letter = 'A'
    while ("Superset $letter" in used) letter++
    return "Superset $letter"
}

internal fun generateWarmupSets(
    placement: RoutineBuilderPlacementState,
    exercise: Exercise,
    machine: GymMachine?,
): List<RoutineBuilderSetState> {
    val working = placement.sets.firstOrNull { it.classification != WorkoutSetClassification.WarmUp.name }
        ?: return placement.sets
    val workingLoad = working.load.toWhipDoubleOrNull()?.takeIf { it > 0.0 } ?: return placement.sets
    val usedKeys = placement.sets.mapTo(mutableSetOf(), RoutineBuilderSetState::key)
    var nextKey = nextLocalSetKey(placement.sets)
    fun snapped(target: Double): Double {
        val choices = machine?.availableLoads.orEmpty().filter { it >= 0.0 && it <= workingLoad }
        if (choices.isNotEmpty()) return choices.minBy { (it - target).absoluteValue }
        val increment = exercise.weightIncrement.takeIf { it > 0.0 } ?: 1.0
        return ((target / increment).toInt() * increment).coerceAtLeast(increment)
    }
    val ramp = listOf(0.4 to 8, 0.6 to 5, 0.8 to 3)
    val ordinalWarmups = machine?.takeIf { it.loadType == MachineLoadType.Level }?.let { selectedMachine ->
        val resistanceOrder = selectedMachine.availableLoads.distinct().sorted().let { values ->
            when (selectedMachine.levelDirection) {
                MachineLevelDirection.HigherNumberMoreResistance -> values
                MachineLevelDirection.HigherNumberLessResistance -> values.reversed()
            }
        }
        val workingIndex = resistanceOrder.indexOfFirst { (it - workingLoad).absoluteValue < 1e-9 }
        if (workingIndex <= 0) emptyList() else ramp.map { (fraction, _) ->
            val index = (((workingIndex + 1) * fraction).toInt() - 1).coerceIn(0, workingIndex - 1)
            resistanceOrder[index]
        }
    }
    val generated = ramp.mapIndexedNotNull { index, (fraction, reps) ->
        val load = if (ordinalWarmups != null) {
            ordinalWarmups.getOrNull(index) ?: return@mapIndexedNotNull null
        } else {
            snapped(workingLoad * fraction)
        }
        if (machine?.loadType != MachineLoadType.Level && load >= workingLoad) return@mapIndexedNotNull null
        while (nextKey in usedKeys) nextKey++
        RoutineBuilderSetState(
            key = nextKey++,
            load = editableNumericValue(load),
            repetitionsMin = reps.toString(),
            repetitionsMax = reps.toString(),
            classification = WorkoutSetClassification.WarmUp.name,
            restSeconds = "60",
            note = "Generated ${(fraction * 100).toInt()}% ramp set",
        )
    }.distinctBy { it.load }
    return generated + placement.sets.filterNot { it.classification == WorkoutSetClassification.WarmUp.name }
}

internal fun RoutineBuilderDayState.groupPlacements(firstKey: Long, secondKey: Long): RoutineBuilderDayState {
    val first = placements.firstOrNull { it.key == firstKey } ?: return this
    val second = placements.firstOrNull { it.key == secondKey } ?: return this
    val group = second.groupKey ?: first.groupKey ?: nextGroupName(this)
    val oldGroups = setOfNotNull(first.groupKey, second.groupKey) - group
    val assigned = placements.map { item ->
        if (item.key == firstKey || item.key == secondKey) item.copy(groupKey = group) else item
    }
    val groupMembers = assigned.filter { it.groupKey == group }
    val insertionIndex = assigned.indexOfFirst { it.groupKey == group }
    val remaining = assigned.filterNot { it.groupKey == group }.toMutableList()
    remaining.addAll(insertionIndex.coerceAtMost(remaining.size), groupMembers)
    val grouped = copy(placements = remaining)
    return oldGroups.fold(grouped) { day, oldGroup -> day.clearSingletonGroup(oldGroup) }
        .normalizePlacementGroupsAndOrder()
}

internal fun RoutineBuilderDayState.removePlacementFromGroup(key: Long): RoutineBuilderDayState {
    val group = placements.firstOrNull { it.key == key }?.groupKey ?: return this
    return copy(placements = placements.map { if (it.key == key) it.copy(groupKey = null) else it })
        .clearSingletonGroup(group)
        .normalizePlacementGroupsAndOrder()
}

internal fun RoutineBuilderDayState.removePlacement(key: Long): RoutineBuilderDayState {
    val removed = placements.firstOrNull { it.key == key } ?: return this
    val remaining = copy(placements = placements.filterNot { it.key == key })
    return removed.groupKey?.let(remaining::clearSingletonGroup)
        ?.normalizePlacementGroupsAndOrder()
        ?: remaining.normalizePlacementGroupsAndOrder()
}

internal fun RoutineBuilderDayState.restorePlacement(
    index: Int,
    placement: RoutineBuilderPlacementState,
    formerGroupMemberKeys: List<Long>,
): RoutineBuilderDayState {
    val originalGroup = placement.groupKey
    var restored = placements.toMutableList().also { items ->
        items.add(index.coerceIn(0, items.size), placement.copy(groupKey = null))
    }
    if (originalGroup != null) {
        val candidates = (formerGroupMemberKeys + placement.key).toSet()
        val eligible = restored.filter { item ->
            item.key in candidates && (item.key == placement.key || item.groupKey == null || item.groupKey == originalGroup)
        }.mapTo(mutableSetOf(), RoutineBuilderPlacementState::key)
        if (eligible.size >= 2) {
            restored = restored.mapTo(mutableListOf()) { item ->
                if (item.key in eligible) item.copy(groupKey = originalGroup) else item
            }
        }
    }
    return copy(placements = restored).normalizePlacementGroupsAndOrder()
}

private fun RoutineBuilderDayState.groupMemberKeys(placement: RoutineBuilderPlacementState): List<Long> =
    placement.groupKey?.let { group -> placements.filter { it.groupKey == group }.map { it.key } }.orEmpty()

private fun RoutineBuilderDayState.normalizePlacementGroupsAndOrder(): RoutineBuilderDayState {
    val groupCounts = placements.mapNotNull(RoutineBuilderPlacementState::groupKey).groupingBy { it }.eachCount()
    val valid = placements.map { placement ->
        if (placement.groupKey?.let { (groupCounts[it] ?: 0) < 2 } == true) placement.copy(groupKey = null)
        else placement
    }
    return copy(placements = valid.routinePlacementBlocks().flatMap(RoutinePlacementBlock::placements))
}

private fun RoutineBuilderDayState.clearSingletonGroup(group: String): RoutineBuilderDayState {
    if (placements.count { it.groupKey == group } != 1) return this
    return copy(placements = placements.map { if (it.groupKey == group) it.copy(groupKey = null) else it })
}

internal fun routineSetSummary(sets: List<RoutineBuilderSetState>): String {
    if (sets.isEmpty()) return "No prescribed sets"
    val phaseCount = sets.mapNotNull(RoutineBuilderSetState::routinePhaseIndex).maxOrNull()?.plus(1)
    if (phaseCount != null) {
        val activeCounts = (0 until phaseCount).map { phase ->
            sets.count { it.routinePhaseIndex == null || it.routinePhaseIndex == phase }
        }
        val activeCountLabel = if (activeCounts.distinct().size == 1) {
            "${activeCounts.first()} active sets/phase"
        } else {
            "${activeCounts.min()}–${activeCounts.max()} active sets/phase"
        }
        val structure = buildList {
            if (sets.any { it.workSection == RoutineWorkSection.Main.name }) add("Main")
            val supplemental = sets.asSequence()
                .filter { it.workSection == RoutineWorkSection.Supplemental.name }
                .mapNotNull { set -> set.supplementalScheme }
                .mapNotNull { value -> runCatching { RoutineSupplementalScheme.valueOf(value) }.getOrNull() }
                .distinct()
                .toList()
            when {
                supplemental.size == 1 -> add(
                    when (supplemental.single()) {
                        RoutineSupplementalScheme.FirstSetLast -> "FSL"
                        RoutineSupplementalScheme.SecondSetLast -> "SSL"
                        RoutineSupplementalScheme.BoringButBig -> "BBB"
                        RoutineSupplementalScheme.BoringButStrong -> "BBS"
                        RoutineSupplementalScheme.Custom -> "Custom supplemental"
                        RoutineSupplementalScheme.None -> "Supplemental"
                    },
                )
                supplemental.size > 1 -> add("Varied supplemental")
                sets.any { it.workSection == RoutineWorkSection.Supplemental.name } -> add("Supplemental")
            }
            if (sets.any { it.workSection == RoutineWorkSection.Assistance.name }) add("Assistance")
            if (sets.any { it.optionalWorkKind == RoutineOptionalWorkKind.Joker.name }) add("optional Joker")
        }
        return buildList {
            add("$phaseCount phases")
            add(activeCountLabel)
            structure.takeIf(List<String>::isNotEmpty)?.joinToString(" + ")?.let(::add)
        }.joinToString(" · ")
    }
    val first = sets.first()
    val reps = when {
        first.repetitionsMin.isBlank() -> null
        first.repetitionsMax.isBlank() || first.repetitionsMax == first.repetitionsMin -> first.repetitionsMin
        else -> "${first.repetitionsMin}–${first.repetitionsMax}"
    }
    return "${sets.size} set${if (sets.size == 1) "" else "s"}" + reps?.let { " × $it reps" }.orEmpty()
}

private fun numericRange(min: Double, max: Double, step: Double): List<Double> = buildList {
    var value = min
    var guard = 0
    while (value <= max + step / 1000.0 && guard++ <= 500) {
        add((value * 1000.0).toLong() / 1000.0)
        value += step
    }
}

private fun String.numericInput(): String = filter { it.isDigit() || it == '.' || it == ',' || it == '-' }.take(16)
private fun String.humanizeEnum(): String = replace(Regex("([a-z])([A-Z])"), "$1 $2").replaceFirstChar(Char::uppercase)
