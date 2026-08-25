package com.whip.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whip.app.domain.EstimatedOneRepMaxFormula
import com.whip.app.domain.Exercise
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.ExerciseTrackingType
import com.whip.app.domain.GymMachine
import com.whip.app.domain.GymMachineDraft
import com.whip.app.domain.LoadInterpretation
import com.whip.app.domain.MachineLoadType
import com.whip.app.domain.RoutineDayDraft
import com.whip.app.domain.RoutineDraft
import com.whip.app.domain.RoutineEquipmentBindingState
import com.whip.app.domain.RoutineLoadPrescriptionType
import com.whip.app.domain.RoutineExerciseDraft
import com.whip.app.domain.WorkoutSession
import com.whip.app.domain.WorkoutSetClassification
import com.whip.app.domain.WorkoutSetDraft
import com.whip.app.domain.editableNumericValue
import com.whip.app.domain.unitSymbol
import com.whip.app.domain.toWhipDoubleOrNull
import com.whip.app.core.RepPrescriptionScheme
import java.util.UUID
import kotlin.math.absoluteValue

private enum class RoutineBuilderPage { Outline, ExercisePicker, WorkoutPicker }

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
    onDeletePrescriptionScheme: (String) -> Unit = {},
) {
    val token = "routine-${routineId ?: "new"}"
    val stateHolder: RoutineBuilderViewModel = viewModel(key = "routine-builder-${routineId ?: "new"}")
    val builder by stateHolder.state.collectAsStateWithLifecycle()
    val initialState = rememberSaveable(token) {
        buildInitialRoutineState(token, initial, gymState.exercises, gymState.machines + gymState.archivedMachines)
    }
    LaunchedEffect(token) { stateHolder.initialize(token, initialState) }

    var page by rememberSaveable(token) { mutableStateOf(RoutineBuilderPage.Outline) }
    var pickerSelection by rememberSaveable(token) { mutableStateOf<List<Long>>(emptyList()) }
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
    var deletedPlacementUndo by rememberSaveable(token) { mutableStateOf<Triple<Long, Int, RoutineBuilderPlacementState>?>(null) }

    val selectedDay = builder.days.firstOrNull { it.key == builder.selectedDayKey }
        ?: builder.days.firstOrNull()
    val selectedPlacement = builder.days.asSequence().flatMap { it.placements.asSequence() }
        .firstOrNull { it.key == builder.selectedPlacementKey }
    val initialComparable = initialState.copy(token = builder.token)
    val isDirty = builder.copy(
        selectedDayKey = initialComparable.selectedDayKey,
        selectedPlacementKey = initialComparable.selectedPlacementKey,
        nextKey = initialComparable.nextKey,
        independentlySavedLibraryItems = initialComparable.independentlySavedLibraryItems,
    ) != initialComparable
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
        stateHolder.update { current ->
            current.copy(days = current.days.map { day ->
                day.copy(placements = day.placements.map { placement ->
                    if (placement.key == placementKey) transform(placement) else placement
                })
            })
        }
    }

    fun addExercises(dayKey: Long, exerciseIds: List<Long>) {
        if (exerciseIds.isEmpty()) return
        stateHolder.update { current ->
            var next = current.nextKey
            val additions = exerciseIds.mapNotNull { exerciseId ->
                val exercise = gymState.exercises.firstOrNull { it.id == exerciseId } ?: return@mapNotNull null
                RoutineBuilderPlacementState(
                    key = next++,
                    exerciseId = exercise.id,
                    exerciseNameSnapshot = exercise.name,
                    sets = listOf(RoutineBuilderSetState(key = next++)),
                )
            }
            current.copy(
                days = current.days.map { day -> if (day.key == dayKey) day.copy(placements = day.placements + additions) else day },
                selectedPlacementKey = additions.lastOrNull()?.key ?: current.selectedPlacementKey,
                nextKey = next,
            )
        }
    }

    fun addCreatedExercise(dayKey: Long, exerciseId: Long, exerciseName: String) {
        stateHolder.update { current ->
            val placementKey = current.nextKey
            val setKey = placementKey + 1L
            val placement = RoutineBuilderPlacementState(
                key = placementKey,
                exerciseId = exerciseId,
                exerciseNameSnapshot = exerciseName,
                sets = listOf(RoutineBuilderSetState(setKey)),
            )
            current.copy(
                days = current.days.map { day -> if (day.key == dayKey) day.copy(placements = day.placements + placement) else day },
                selectedPlacementKey = placementKey,
                nextKey = setKey + 1L,
                independentlySavedLibraryItems = current.independentlySavedLibraryItems + 1,
            )
        }
    }

    val validationErrors = routineBuilderValidationErrors(builder, gymState)
    val canSave = !librarySaveInFlight && !routineSaveInFlight && builder.name.isNotBlank() && builder.days.isNotEmpty() && builder.days.all { it.name.isNotBlank() } && validationErrors.isEmpty()

    Surface(modifier.fillMaxSize().testTag("routine-builder"), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            RoutineBuilderHeader(
                editing = routineId != null,
                page = page,
                canSave = canSave,
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
                            stateHolder.clear()
                            onDismiss()
                        }
                    }
                },
            )

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
            deletedPlacementUndo?.let { (dayKey, index, placement) ->
                UndoRow("Removed ${placement.exerciseNameSnapshot}") {
                    updateDay(dayKey) { day ->
                        day.copy(placements = day.placements.toMutableList().also { it.add(index.coerceIn(0, it.size), placement) })
                    }
                    deletedPlacementUndo = null
                }
            }

            when (page) {
                RoutineBuilderPage.ExercisePicker -> ExercisePickerPage(
                    modifier = Modifier.weight(1f),
                    gymState = gymState,
                    selectedIds = pickerSelection,
                    onSelectionChange = { pickerSelection = it },
                    onCreateExercise = { seed -> exerciseNameSeed = seed; showCreateExercise = true },
                    onAdd = {
                        selectedDay?.let { day -> addExercises(day.key, pickerSelection) }
                        pickerSelection = emptyList()
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
                RoutineBuilderPage.Outline -> {
                    if (selectedDay == null) return@Surface
                    BoxWithConstraints(Modifier.weight(1f)) {
                        val showMasterDetail = maxWidth >= 720.dp && selectedPlacement != null
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
                                    onMovePlacement = { key, delta -> updateDay(selectedDay.key) { it.movePlacement(key, delta) } },
                                    onDuplicatePlacement = { placement -> duplicatePlacement(stateHolder, selectedDay.key, placement) },
                                    onRemovePlacement = { placement ->
                                        val index = selectedDay.placements.indexOfFirst { it.key == placement.key }
                                        deletedPlacementUndo = Triple(selectedDay.key, index, placement)
                                        updateDay(selectedDay.key) { it.copy(placements = it.placements.filterNot { item -> item.key == placement.key }) }
                                        stateHolder.update { it.copy(selectedPlacementKey = null) }
                                    },
                                    onAddExercises = { pickerSelection = emptyList(); page = RoutineBuilderPage.ExercisePicker },
                                    onAddFromWorkout = { page = RoutineBuilderPage.WorkoutPicker },
                                    onDeleteDay = { day ->
                                        val index = builder.days.indexOfFirst { it.key == day.key }
                                        deletedDayUndo = index to day
                                        stateHolder.update { current ->
                                            val remaining = current.days.filterNot { it.key == day.key }
                                            current.copy(days = remaining, selectedDayKey = remaining.getOrNull(index.coerceAtMost(remaining.lastIndex))?.key, selectedPlacementKey = null)
                                        }
                                    },
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
                                            updatePlacement(selectedPlacement.key) { it.withoutMachine() }
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
                                        onUpdateDay = { transform -> updateDay(selectedDay.key, transform) },
                                        onChooseEquipment = { equipmentPickerPlacementKey = selectedPlacement.key },
                                        onMoveToDay = { target, copy -> moveOrCopyPlacement(stateHolder, selectedDay.key, target, selectedPlacement, copy) },
                                        dialogModifier = dialogModifier,
                                        onSavePrescriptionScheme = onSavePrescriptionScheme,
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
                                    onNoMachine = { updatePlacement(selectedPlacement.key) { it.withoutMachine() }; equipmentPickerPlacementKey = null },
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
                                    onUpdateDay = { transform -> updateDay(selectedDay.key, transform) },
                                    onChooseEquipment = { equipmentPickerPlacementKey = selectedPlacement.key },
                                    onMoveToDay = { target, copy -> moveOrCopyPlacement(stateHolder, selectedDay.key, target, selectedPlacement, copy) },
                                    dialogModifier = dialogModifier,
                                    onSavePrescriptionScheme = onSavePrescriptionScheme,
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
                                onMovePlacement = { key, delta -> updateDay(selectedDay.key) { it.movePlacement(key, delta) } },
                                onDuplicatePlacement = { duplicatePlacement(stateHolder, selectedDay.key, it) },
                                onRemovePlacement = { placement ->
                                    val index = selectedDay.placements.indexOfFirst { it.key == placement.key }
                                    deletedPlacementUndo = Triple(selectedDay.key, index, placement)
                                    updateDay(selectedDay.key) { it.copy(placements = it.placements.filterNot { item -> item.key == placement.key }) }
                                },
                                onAddExercises = { pickerSelection = emptyList(); page = RoutineBuilderPage.ExercisePicker },
                                onAddFromWorkout = { page = RoutineBuilderPage.WorkoutPicker },
                                onDeleteDay = { day ->
                                    val index = builder.days.indexOfFirst { it.key == day.key }
                                    deletedDayUndo = index to day
                                    stateHolder.update { current ->
                                        val remaining = current.days.filterNot { it.key == day.key }
                                        current.copy(days = remaining, selectedDayKey = remaining.getOrNull(index.coerceAtMost(remaining.lastIndex))?.key)
                                    }
                                },
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
                        addCreatedExercise(targetDay, id, draft.name.trim())
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
        val exercise = placement?.let { selected -> gymState.exercises.firstOrNull { it.id == selected.exerciseId } }
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
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back in routine builder")
        }
        Text(
            when (page) {
                RoutineBuilderPage.Outline -> if (editing) "Edit routine" else "New routine"
                RoutineBuilderPage.ExercisePicker -> "Add exercises"
                RoutineBuilderPage.WorkoutPicker -> "Add from workout"
            }.uiTitleCase(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (page == RoutineBuilderPage.Outline) {
            WhipTextButton(onClick = onCancel) { Text("Cancel") }
            WhipButton(enabled = canSave, onClick = onSave, modifier = Modifier.testTag("routine-builder-save")) { Text("Save") }
        }
    }
    HorizontalDivider()
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
private fun RoutineOutlinePane(
    modifier: Modifier,
    builder: RoutineBuilderState,
    selectedDay: RoutineBuilderDayState,
    selectedPlacementKey: Long?,
    gymState: GymUiState,
    validationErrors: Map<Long, String>,
    onBuilderChange: ((RoutineBuilderState) -> RoutineBuilderState) -> Unit,
    onSelectPlacement: (Long) -> Unit,
    onMovePlacement: (Long, Int) -> Unit,
    onDuplicatePlacement: (RoutineBuilderPlacementState) -> Unit,
    onRemovePlacement: (RoutineBuilderPlacementState) -> Unit,
    onAddExercises: () -> Unit,
    onAddFromWorkout: () -> Unit,
    onDeleteDay: (RoutineBuilderDayState) -> Unit,
) {
    Column(modifier) {
        OutlinedTextField(
            value = builder.name,
            onValueChange = { value -> onBuilderChange { it.copy(name = value) } },
            label = { Text("Routine name *") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).testTag("routine-editor-name"),
            singleLine = true,
        )
        OutlinedTextField(
            value = builder.notes,
            onValueChange = { value -> onBuilderChange { it.copy(notes = value) } },
            label = { Text("Routine notes") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            maxLines = 2,
        )
        if (builder.days.all { it.placements.isEmpty() }) {
            Text("Start with a Split", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                listOf(
                    "Full body" to listOf("Full body"),
                    "Upper / lower" to listOf("Upper", "Lower"),
                    "Push / pull / legs" to listOf("Push", "Pull", "Legs"),
                ).forEach { (label, names) ->
                    WhipFilterChip(
                        selected = builder.days.map { it.name } == names,
                        onClick = { onBuilderChange { current -> current.withDayTemplate(names) } },
                        label = { Text(label) },
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            builder.days.forEach { day ->
                WhipFilterChip(
                    selected = day.key == selectedDay.key,
                    onClick = { onBuilderChange { it.copy(selectedDayKey = day.key, selectedPlacementKey = null) } },
                    label = { Text("${day.name} · ${day.placements.size}") },
                    modifier = Modifier.testTag("routine-day-${day.key}"),
                )
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
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
            if (builder.days.size > 1) {
                IconButton(
                    enabled = builder.days.indexOfFirst { it.key == selectedDay.key } > 0,
                    onClick = { onBuilderChange { it.moveDay(selectedDay.key, -1) } },
                    modifier = Modifier.semantics { contentDescription = "Move ${selectedDay.name} earlier" },
                ) { Text("←") }
                IconButton(
                    enabled = builder.days.indexOfFirst { it.key == selectedDay.key } < builder.days.lastIndex,
                    onClick = { onBuilderChange { it.moveDay(selectedDay.key, 1) } },
                    modifier = Modifier.semantics { contentDescription = "Move ${selectedDay.name} later" },
                ) { Text("→") }
            }
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
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("routine-selected-exercises"),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 100.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
            items(selectedDay.placements, key = RoutineBuilderPlacementState::key) { placement ->
                RoutinePlacementCard(
                    placement = placement,
                    selected = placement.key == selectedPlacementKey,
                    gymState = gymState,
                    error = validationErrors[placement.key],
                    onOpen = { onSelectPlacement(placement.key) },
                    onMove = { onMovePlacement(placement.key, it) },
                    onDuplicate = { onDuplicatePlacement(placement) },
                    onRemove = { onRemovePlacement(placement) },
                )
            }
            item {
                WhipButton(onClick = onAddExercises, modifier = Modifier.fillMaxWidth().testTag("routine-add-exercises")) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add Exercises")
                }
            }
            item { WhipOutlinedButton(onClick = onAddFromWorkout, modifier = Modifier.fillMaxWidth()) { Text("Add from a Previous Workout") } }
        }
    }
}

@Composable
private fun RoutinePlacementCard(
    placement: RoutineBuilderPlacementState,
    selected: Boolean,
    gymState: GymUiState,
    error: String?,
    onOpen: () -> Unit,
    onMove: (Int) -> Unit,
    onDuplicate: () -> Unit,
    onRemove: () -> Unit,
) {
    var menu by rememberSaveable(placement.key) { mutableStateOf(false) }
    var dragDistance by rememberSaveable(placement.key) { mutableFloatStateOf(0f) }
    val exercise = gymState.exercises.firstOrNull { it.id == placement.exerciseId }
    val machine = (gymState.machines + gymState.archivedMachines).firstOrNull { it.id == placement.machineId }
    val border = when {
        error != null -> BorderStroke(2.dp, MaterialTheme.colorScheme.error)
        placement.groupKey != null -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        selected -> BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
        else -> null
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClickLabel = "Edit ${exercise?.name ?: placement.exerciseNameSnapshot}", onClick = onOpen),
        border = border,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.DragHandle,
                contentDescription = "Drag to reorder ${exercise?.name ?: placement.exerciseNameSnapshot}",
                modifier = Modifier.size(48.dp).padding(8.dp).pointerInput(placement.key) {
                    detectDragGesturesAfterLongPress(
                        onDragEnd = { dragDistance = 0f },
                        onDragCancel = { dragDistance = 0f },
                    ) { change, amount ->
                        change.consume()
                        dragDistance += amount.y
                        if (dragDistance.absoluteValue > 48.dp.toPx()) {
                            onMove(if (dragDistance > 0f) 1 else -1)
                            dragDistance = 0f
                        }
                    }
                },
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(exercise?.name ?: placement.exerciseNameSnapshot, fontWeight = FontWeight.Bold)
                Text(
                    listOfNotNull(
                        machine?.displayName ?: placement.machineNameSnapshot.takeIf(String::isNotBlank) ?: "No Machine / Free Weights",
                        placement.sets.takeIf(List<*>::isNotEmpty)?.let { routineSetSummary(placement.sets) },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                placement.groupKey?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
            ItemEditButton(
                itemType = "routine exercise",
                itemName = exercise?.name ?: placement.exerciseNameSnapshot,
                onEdit = onOpen,
            )
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.semantics { contentDescription = "More options for ${exercise?.name ?: placement.exerciseNameSnapshot}" }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Move Up") }, onClick = { menu = false; onMove(-1) })
                    DropdownMenuItem(text = { Text("Move Down") }, onClick = { menu = false; onMove(1) })
                    DropdownMenuItem(text = { Text("Duplicate") }, onClick = { menu = false; onDuplicate() }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) })
                    DropdownMenuItem(text = { Text("Remove") }, onClick = { menu = false; onRemove() }, leadingIcon = { Icon(Icons.Outlined.Delete, null) })
                }
            }
        }
    }
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
    onUpdateDay: ((RoutineBuilderDayState) -> RoutineBuilderDayState) -> Unit,
    onChooseEquipment: () -> Unit,
    onMoveToDay: (Long, Boolean) -> Unit,
    dialogModifier: Modifier,
    onSavePrescriptionScheme: (RepPrescriptionScheme) -> Unit,
    onDeletePrescriptionScheme: (String) -> Unit,
) {
    val exercise = gymState.exercises.firstOrNull { it.id == placement.exerciseId }
    val machine = (gymState.machines + gymState.archivedMachines).firstOrNull { it.id == placement.machineId }
    var dayMenu by rememberSaveable(placement.key) { mutableStateOf(false) }
    var copyDayMenu by rememberSaveable(placement.key) { mutableStateOf(false) }
    var showSchemeEditor by rememberSaveable(placement.key) { mutableStateOf(false) }
    var editingSchemeId by rememberSaveable(placement.key) { mutableStateOf<String?>(null) }
    var pendingDeleteSchemeId by rememberSaveable(placement.key) { mutableStateOf<String?>(null) }
    var alternativeQuery by rememberSaveable(placement.key) { mutableStateOf("") }
    LazyColumn(
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
        if (exercise?.supportsRepPrescription() != false) item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Rep Prescription Schemes", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
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
                    gymState.appSettings.repPrescriptionSchemes.forEach { scheme ->
                        RepPrescriptionSchemeRow(
                            scheme = scheme,
                            onApply = { onUpdate { current -> current.copy(sets = applyRepPrescriptionScheme(current.sets, scheme)) } },
                            onEdit = { editingSchemeId = scheme.id; showSchemeEditor = true },
                            onDelete = { pendingDeleteSchemeId = scheme.id },
                        )
                    }
                }
                Text(
                    "Schemes speed up entry. Editing or deleting one never changes routines already using it.",
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
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Show Advanced Prescription Fields", modifier = Modifier.weight(1f))
                Switch(checked = showAdvanced, onCheckedChange = onShowAdvanced)
            }
            if (showAdvanced) {
                DependentSettingsNotice(
                    message = "RPE, RIR, rest, tempo, notes, and unilateral controls are shown inside every set below.",
                    testTag = "routine-advanced-consequence",
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Copy Previous Values When No Plan", modifier = Modifier.weight(1f))
                Switch(checked = placement.copyPreviousWorkout, onCheckedChange = { checked -> onUpdate { it.copy(copyPreviousWorkout = checked) } })
            }
            if (placement.copyPreviousWorkout) {
                Text(
                    "Unplanned fields start with values from the previous workout.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(placement.sets, key = RoutineBuilderSetState::key) { set ->
            RoutineSetEditorCard(
                set = set,
                exercise = exercise,
                machine = machine,
                showAdvanced = showAdvanced,
                onUpdate = { transform -> onUpdate { current -> current.copy(sets = current.sets.map { if (it.key == set.key) transform(it) else it }) } },
                onDuplicate = { onUpdate { current -> current.copy(sets = current.sets + set.copy(key = nextLocalSetKey(current.sets))) } },
                onDelete = { onUpdate { current -> current.copy(sets = current.sets.filterNot { it.key == set.key }) } },
            )
        }
        item {
            WhipOutlinedButton(
                onClick = { onUpdate { current -> current.copy(sets = current.sets + RoutineBuilderSetState(nextLocalSetKey(current.sets))) } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Add Set") }
        }
        if (showAdvanced && exercise?.trackingType in setOf(ExerciseTrackingType.WeightReps, ExerciseTrackingType.WeightOnly, ExerciseTrackingType.WeightDuration)) {
            item {
                Text("Training Cycle", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    placement.trainingMaxPercent,
                    { value -> onUpdate { it.copy(trainingMaxPercent = value.numericInput()) } },
                    label = { Text("Training max (% of estimated 1RM)") },
                    supportingText = { Text("Used only by sets prescribed as % of training max.") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    placement.progressionPercentages,
                    { value -> onUpdate { it.copy(progressionPercentages = value.filter { char -> char.isDigit() || char in ".,- " }) } },
                    label = { Text("Cycle load multipliers (%)") },
                    supportingText = { Text("Comma-separated weeks or sessions, for example 100, 102.5, 105, 90 for a three-step wave and deload. Blank keeps 100%.") },
                    modifier = Modifier.fillMaxWidth(),
                )
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
            Text("Superset or Circuit", fontWeight = FontWeight.SemiBold)
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
    onUpdate: ((RoutineBuilderSetState) -> RoutineBuilderSetState) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var classificationMenu by rememberSaveable(set.key) { mutableStateOf(false) }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    WhipTextButton(onClick = { classificationMenu = true }) {
                        Text(set.classification.workoutSetClassificationLabel())
                    }
                    DropdownMenu(expanded = classificationMenu, onDismissRequest = { classificationMenu = false }) {
                        WorkoutSetClassification.entries.forEach { value ->
                            DropdownMenuItem(text = { Text(value.uiLabel()) }, onClick = { classificationMenu = false; onUpdate { it.copy(classification = value.name) } })
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDuplicate, modifier = Modifier.semantics { contentDescription = "Duplicate set" }) { Icon(Icons.Outlined.ContentCopy, null) }
                IconButton(onClick = onDelete, modifier = Modifier.semantics { contentDescription = "Delete set" }) { Icon(Icons.Outlined.Delete, null) }
            }
            val needsLoad = exercise?.trackingType in setOf(
                ExerciseTrackingType.WeightReps,
                ExerciseTrackingType.WeightOnly,
                ExerciseTrackingType.WeightDuration,
                ExerciseTrackingType.AssistedBodyweightReps,
            )
            if (needsLoad) {
                val prescriptionType = runCatching { RoutineLoadPrescriptionType.valueOf(set.loadPrescriptionType) }
                    .getOrDefault(RoutineLoadPrescriptionType.Absolute)
                if (machine?.loadType != MachineLoadType.Level) {
                    Text("Load Prescription", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        RoutineLoadPrescriptionType.entries.forEach { type ->
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
                        label = { Text(if (machine?.loadType == MachineLoadType.Level) "${machine.levelLabel.humanizeEnum()} target" else "Load (${unitSymbol(machine?.unitId ?: exercise?.weightUnitId ?: "kilogram")})") },
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
                }
            }
            if (exercise?.trackingType !in setOf(ExerciseTrackingType.WeightOnly, ExerciseTrackingType.DistanceOnly, ExerciseTrackingType.DurationOnly)) {
                ResponsiveFieldPair(
                    first = { field -> OutlinedTextField(set.repetitionsMin, { value -> onUpdate { it.copy(repetitionsMin = value.filter(Char::isDigit).take(4)) } }, label = { Text("Reps min") }, modifier = field.testTag("routine-reps-min-${set.key}"), singleLine = true) },
                    second = { field -> OutlinedTextField(set.repetitionsMax, { value -> onUpdate { it.copy(repetitionsMax = value.filter(Char::isDigit).take(4)) } }, label = { Text("Reps max") }, modifier = field.testTag("routine-reps-max-${set.key}"), singleLine = true) },
                )
            }
            if (exercise?.trackingType in setOf(ExerciseTrackingType.DistanceOnly, ExerciseTrackingType.DistanceDuration)) {
                OutlinedTextField(set.distance, { value -> onUpdate { it.copy(distance = value.numericInput()) } }, label = { Text("Distance") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
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
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Unilateral Set", modifier = Modifier.weight(1f))
                    Switch(set.unilateral, { checked -> onUpdate { it.copy(unilateral = checked) } })
                }
            }
        }
    }
}

@Composable
private fun ExercisePickerPage(
    modifier: Modifier,
    gymState: GymUiState,
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
                gymState.machines.filter { it.exerciseId == exercise.id }.joinToString(" ") { it.displayName },
            )
        }
        .filter { !favouritesOnly || it.favorite }
        .filter { !recentOnly || it.id in recentIds }
        .filter { selected -> categoryId == null || gymState.categoryLinks.any { it.exerciseId == selected.id && it.categoryId == categoryId } }
        .filter { selected -> equipment == null || selected.equipment.equals(equipment, true) }
        .filter { selected -> muscle == null || (selected.primaryMuscles + "," + selected.secondaryMuscles).contains(requireNotNull(muscle), true) }
        .sortedWith(compareByDescending<Exercise> { it.id in recentIds }.thenByDescending(Exercise::favorite).thenBy { it.name.lowercase() })
        .toList()
    Column(modifier) {
        OutlinedTextField(query, { query = it }, label = { Text("Search exercises") }, modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("routine-exercise-search"), singleLine = true)
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
                Row(
                    Modifier.fillMaxWidth().clickable {
                        onSelectionChange(if (checked) selectedIds - exercise.id else selectedIds + exercise.id)
                    }.padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked, { value -> onSelectionChange(if (value) selectedIds + exercise.id else selectedIds - exercise.id) })
                    Column(Modifier.weight(1f)) {
                        Text(exercise.name, fontWeight = FontWeight.SemiBold)
                        Text(listOfNotNull(exercise.equipment.takeIf(String::isNotBlank), exercise.primaryMuscles.takeIf(String::isNotBlank)).joinToString(" · ").ifBlank { exercise.trackingType.label.uiTitleCase() }, style = MaterialTheme.typography.bodySmall)
                    }
                    if (exercise.favorite) Icon(
                        Icons.Outlined.Star,
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Surface(tonalElevation = 4.dp) {
            WhipButton(enabled = selectedIds.isNotEmpty(), onClick = onAdd, modifier = Modifier.fillMaxWidth().padding(12.dp).testTag("routine-add-selected")) {
                Text("Add ${selectedIds.size} Exercise${if (selectedIds.size == 1) "" else "s"}")
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
    val machines = (gymState.machines + gymState.archivedMachines).filter {
        it.exerciseId == placement.exerciseId && it.displayName.contains(query, true)
    }.sortedWith(compareBy<GymMachine> { it.archived }.thenBy { it.location.lowercase() }.thenBy { it.name.lowercase() })
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Choose Equipment", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Equipment is scoped to this exercise so its history and progression remain comparable.")
        }
        item { OutlinedTextField(query, { query = it }, label = { Text("Search machine or location") }, modifier = Modifier.fillMaxWidth()) }
        item { WhipOutlinedButton(onClick = onNoMachine, modifier = Modifier.fillMaxWidth()) { Text("No Machine / Free Weights") } }
        items(machines, key = GymMachine::id) { machine ->
            OutlinedCard(Modifier.fillMaxWidth().clickable(onClickLabel = "Use ${machine.displayName}") { onChoose(machine) }) {
                Column(Modifier.padding(14.dp)) {
                    Text(machine.displayName, fontWeight = FontWeight.Bold)
                    Text("${machine.loadType.label.uiTitleCase()} · ${machine.availableLoads.size} saved values${if (machine.archived) " · Archived" else ""}")
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
                OutlinedTextField(name, { name = it }, label = { Text("Machine name *") }, modifier = Modifier.fillMaxWidth().testTag("routine-quick-machine-name"))
                OutlinedTextField(location, { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MachineLoadType.entries.forEach { type -> WhipFilterChip(loadType == type, { loadType = type; minimum = if (type == MachineLoadType.Level) "1" else "5"; maximum = if (type == MachineLoadType.Level) "10" else "100"; increment = if (type == MachineLoadType.Level) "1" else "5" }, { Text(type.label.uiTitleCase()) }) }
                }
                if (loadType == MachineLoadType.Mass) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("kilogram", "pound").forEach { unit -> WhipFilterChip(unitId == unit, { unitId = unit }, { Text(unitSymbol(unit)) }) }
                } else OutlinedTextField(levelLabel, { levelLabel = it }, label = { Text("Setting label") }, modifier = Modifier.fillMaxWidth())
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
                    sets = placement.plannedSets.map { set ->
                        RoutineBuilderSetState(
                            key = next++,
                            load = (set.machineLoadValue ?: set.weight)?.let(::editableNumericValue).orEmpty(),
                            repetitionsMin = set.reps?.toString().orEmpty(),
                            repetitionsMax = set.repsMax?.toString().orEmpty(),
                            distance = set.distance?.let(::editableNumericValue).orEmpty(),
                            durationSeconds = set.durationSeconds?.toString().orEmpty(),
                            classification = set.classification.name,
                            rpe = set.rpe?.let(::editableNumericValue).orEmpty(),
                            rir = set.rir?.let(::editableNumericValue).orEmpty(),
                            restSeconds = set.restSeconds?.toString().orEmpty(),
                            tempo = set.tempo,
                            note = set.note,
                            unilateral = set.unilateral,
                            loadPrescriptionType = set.loadPrescriptionType.name,
                            loadPercentage = set.loadPercentage?.let(::editableNumericValue).orEmpty(),
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
    )
}

private fun RoutineBuilderState.toRoutineDraft(gymState: GymUiState): RoutineDraft {
    val exercises = gymState.exercises.associateBy(Exercise::id)
    val machines = (gymState.machines + gymState.archivedMachines).associateBy(GymMachine::id)
    return RoutineDraft(
        name = name.trim(),
        notes = notes.trim(),
        days = days.map { day ->
            RoutineDayDraft(
                name = day.name.trim(),
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
                                    ?: exercise?.weightUnitId ?: gymState.appSettings.gymWeightUnitId,
                                reps = set.repetitionsMin.toIntOrNull(),
                                repsMax = set.repetitionsMax.toIntOrNull(),
                                distance = set.distance.toWhipDoubleOrNull(),
                                durationSeconds = set.durationSeconds.toLongOrNull(),
                                planned = true,
                                classification = runCatching { WorkoutSetClassification.valueOf(set.classification) }.getOrDefault(WorkoutSetClassification.Working),
                                note = set.note.trim(),
                                rpe = set.rpe.toWhipDoubleOrNull(),
                                rir = set.rir.toWhipDoubleOrNull(),
                                tempo = set.tempo.trim(),
                                restSeconds = set.restSeconds.toIntOrNull(),
                                machineLoadValue = load.takeIf { placement.machineId != null },
                                unilateral = set.unilateral,
                                loadPrescriptionType = runCatching { RoutineLoadPrescriptionType.valueOf(set.loadPrescriptionType) }
                                    .getOrDefault(RoutineLoadPrescriptionType.Absolute),
                                loadPercentage = set.loadPercentage.toWhipDoubleOrNull(),
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
                    )
                },
            )
        },
    )
}

private fun routineBuilderValidationErrors(state: RoutineBuilderState, gymState: GymUiState): Map<Long, String> {
    val errors = mutableMapOf<Long, String>()
    val exercises = gymState.exercises.associateBy(Exercise::id)
    val machines = (gymState.machines + gymState.archivedMachines).associateBy(GymMachine::id)
    state.days.flatMap(RoutineBuilderDayState::placements).forEach { placement ->
        val exercise = exercises[placement.exerciseId]
        val machine = placement.machineId?.let(machines::get)
        val error = when {
            exercise == null -> "Exercise no longer exists"
            placement.equipmentBindingState == RoutineEquipmentBindingState.NeedsEquipment.name -> "Choose replacement equipment"
            placement.machineId != null && machine == null -> "Machine no longer exists"
            placement.trainingMaxPercent.toWhipDoubleOrNull()?.let { it !in 1.0..100.0 } != false -> "Training max must be from 1 to 100%"
            placement.progressionPercentages.split(',').map(String::trim).filter(String::isNotBlank)
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
)

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
)

private fun RoutineBuilderPlacementState.withoutMachine() = copy(
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
)

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
    holder.update { state ->
        var next = state.nextKey
        val target = if (copy) source.copy(key = next++, sets = source.sets.map { it.copy(key = next++) }) else source
        state.copy(
            days = state.days.map { day -> when (day.key) {
                fromDayKey -> if (copy) day else day.copy(placements = day.placements.filterNot { it.key == source.key })
                targetDayKey -> day.copy(placements = day.placements + target)
                else -> day
            } },
            selectedDayKey = targetDayKey,
            selectedPlacementKey = target.key,
            nextKey = next,
        )
    }
}

private fun importWorkoutIntoDay(holder: RoutineBuilderViewModel, dayKey: Long, session: WorkoutSession, gymState: GymUiState) {
    holder.update { state ->
        var next = state.nextKey
        val exercises = gymState.exercises.associateBy(Exercise::id)
        val machines = (gymState.machines + gymState.archivedMachines).associateBy(GymMachine::id)
        val additions = gymState.allWorkoutExercises.filter { it.sessionId == session.id }.sortedBy { it.position }.map { workoutExercise ->
            val exercise = exercises[workoutExercise.exerciseId]
            val machine = workoutExercise.machineId?.let(machines::get)
            RoutineBuilderPlacementState(
                key = next++,
                exerciseId = workoutExercise.exerciseId,
                exerciseNameSnapshot = exercise?.name ?: "Exercise ${workoutExercise.exerciseId}",
                machineId = machine?.id,
                equipmentBindingState = if (machine == null) RoutineEquipmentBindingState.None.name else RoutineEquipmentBindingState.Resolved.name,
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
                        repetitionsMin = set.repetitions?.toString().orEmpty(),
                        repetitionsMax = set.prescribedRepetitionsMax?.toString().orEmpty(),
                        distance = set.enteredDistance?.let(::editableNumericValue).orEmpty(),
                        durationSeconds = set.durationSeconds?.toString().orEmpty(),
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
    return copy(days = newDays, selectedDayKey = newDays.firstOrNull()?.key, selectedPlacementKey = null, nextKey = next)
}

internal fun RoutineBuilderState.moveDay(key: Long, delta: Int): RoutineBuilderState {
    val index = days.indexOfFirst { it.key == key }
    val target = (index + delta).coerceIn(0, days.lastIndex)
    if (index < 0 || index == target) return this
    return copy(days = days.toMutableList().also { java.util.Collections.swap(it, index, target) })
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
    val index = placements.indexOfFirst { it.key == key }
    val target = (index + delta).coerceIn(0, placements.lastIndex)
    if (index < 0 || index == target) return this
    return copy(placements = placements.toMutableList().also { java.util.Collections.swap(it, index, target) })
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
    val generated = listOf(0.4 to 8, 0.6 to 5, 0.8 to 3).mapNotNull { (fraction, reps) ->
        val load = snapped(workingLoad * fraction)
        if (load >= workingLoad) return@mapNotNull null
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
    val grouped = copy(placements = placements.map { item ->
        if (item.key == firstKey || item.key == secondKey) item.copy(groupKey = group) else item
    })
    return oldGroups.fold(grouped) { day, oldGroup -> day.clearSingletonGroup(oldGroup) }
}

internal fun RoutineBuilderDayState.removePlacementFromGroup(key: Long): RoutineBuilderDayState {
    val group = placements.firstOrNull { it.key == key }?.groupKey ?: return this
    return copy(placements = placements.map { if (it.key == key) it.copy(groupKey = null) else it })
        .clearSingletonGroup(group)
}

private fun RoutineBuilderDayState.clearSingletonGroup(group: String): RoutineBuilderDayState {
    if (placements.count { it.groupKey == group } != 1) return this
    return copy(placements = placements.map { if (it.groupKey == group) it.copy(groupKey = null) else it })
}

private fun routineSetSummary(sets: List<RoutineBuilderSetState>): String {
    if (sets.isEmpty()) return "No prescribed sets"
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
