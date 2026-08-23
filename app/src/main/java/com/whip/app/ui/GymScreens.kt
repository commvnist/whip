package com.whip.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Remove
import com.whip.app.domain.Exercise
import com.whip.app.core.zoneId
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.ExerciseCategory
import com.whip.app.domain.ExerciseTrackingType
import com.whip.app.domain.BodyweightLoadPolicy
import com.whip.app.domain.WorkoutSession
import com.whip.app.domain.WorkoutExercise
import com.whip.app.domain.WorkoutGroupType
import com.whip.app.domain.WorkoutSet
import com.whip.app.domain.WorkoutSetClassification
import com.whip.app.domain.WorkoutSetDraft
import com.whip.app.domain.WeightEquipmentSetup
import com.whip.app.domain.validateWorkoutSetDraft
import com.whip.app.domain.GymRoutine
import com.whip.app.domain.GymMachine
import com.whip.app.domain.GymMachineDraft
import com.whip.app.domain.MachineLoadType
import com.whip.app.domain.MachineStackMode
import com.whip.app.domain.LoadInterpretation
import com.whip.app.domain.GymGraphAggregation
import com.whip.app.domain.GymGraphMetric
import com.whip.app.domain.GymGraphRange
import com.whip.app.domain.GymGraphPoint
import com.whip.app.domain.EstimatedOneRepMaxFormula
import com.whip.app.domain.RoutineDayDraft
import com.whip.app.domain.RoutineDraft
import com.whip.app.domain.RoutineExerciseDraft
import com.whip.app.domain.PersonalRecord
import com.whip.app.domain.PersonalRecordType
import com.whip.app.domain.estimatedOneRepMaxKg
import com.whip.app.domain.volumeKg
import com.whip.app.domain.applyPolicySnapshot
import com.whip.app.domain.canonicalResistanceKg
import com.whip.app.domain.buildExerciseGraph
import com.whip.app.domain.buildWeeklyGymSummary
import com.whip.app.domain.calculatePlateLoading
import com.whip.app.domain.calculateRepMaxTable
import com.whip.app.domain.graphRangeStart
import com.whip.app.domain.downsampleEvenly
import com.whip.app.domain.distanceFromMetres
import com.whip.app.domain.massFromKilograms
import com.whip.app.domain.massToKilograms
import com.whip.app.domain.unitSymbol
import com.whip.app.domain.compactNumericSequence
import com.whip.app.domain.convertWeightEquipmentSetup
import com.whip.app.domain.convertPracticalMassValue
import com.whip.app.domain.editableNumericValue
import com.whip.app.domain.parseNumericSequence
import com.whip.app.domain.standardMachineSequence
import com.whip.app.domain.standardWeightEquipment
import com.whip.app.domain.steppedNumericValue
import com.whip.app.domain.toWhipDoubleOrNull
import com.whip.app.core.PlatePreset
import com.whip.app.core.OperationStatus
import com.whip.app.core.DEFAULT_REST_TIMER_PRESET_SECONDS
import com.whip.app.core.normalizeRestTimerPresets
import java.text.NumberFormat
import java.io.Serializable
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.whip.app.data.MachineDeletionImpact
import com.whip.app.domain.RoutineEquipmentBindingState
import com.whip.app.domain.equipmentScopeKey

enum class GymDestination {
    Workout,
    History,
    Progress,
    Library,
    Routines,
    Exercises,
    Machines,
    Categories,
    Tools,
}

internal val primaryGymDestinations = listOf(
    GymDestination.Workout,
    GymDestination.History,
    GymDestination.Progress,
    GymDestination.Library,
)

internal val libraryGymDestinations = GymDestination.entries.filterNot(primaryGymDestinations::contains)

private enum class WorkoutHistoryRange { Month, ThreeMonths, Year, All }

private enum class ExerciseLibrarySort(val label: String) {
    Name("Name"),
    RecentlyUsed("Recently Used"),
    RecentlyAdded("Recently Added"),
    FavoritesFirst("Favorites First"),
}

private const val MACHINE_EQUIPMENT_FILTER = "__machine__"

private fun WorkoutHistoryRange.uiLabel(): String = when (this) {
    WorkoutHistoryRange.Month -> "1 Month"
    WorkoutHistoryRange.ThreeMonths -> "3 Months"
    WorkoutHistoryRange.Year -> "1 Year"
    WorkoutHistoryRange.All -> "All Time"
}

@Composable
fun GymAreaContent(
    state: GymUiState,
    innerPadding: PaddingValues,
    viewModel: GymViewModel,
    createExerciseRequested: Boolean = false,
    startWorkoutRequested: Boolean = false,
    onExternalRequestConsumed: () -> Unit = {},
    openSearchRequest: WhipSearchResult? = null,
    onOpenSearchRequestConsumed: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onOpenBackupSettings: () -> Unit = {},
    dialogModifier: Modifier = Modifier,
    onRoutineEditorStateChange: (Boolean) -> Unit = {},
    operationStatus: OperationStatus = OperationStatus.Idle,
    initialDestination: GymDestination = GymDestination.Workout,
    onDestinationChange: (GymDestination) -> Unit = {},
    requestedWorkoutExerciseId: Long? = null,
    onRequestedWorkoutExerciseConsumed: () -> Unit = {},
) {
    if (state.loading || state.errorMessage != null) {
        DomainLoadContent("gym data", innerPadding, state.errorMessage, viewModel::retryLoading)
        return
    }
    val context = LocalContext.current
    val machineDeletionImpact by viewModel.machineDeletionImpact.collectAsStateWithLifecycle()
    val machineDeletionInProgress by viewModel.machineDeletionInProgress.collectAsStateWithLifecycle()
    var destination by rememberSaveable(initialDestination) { mutableStateOf(initialDestination) }
    var exerciseEditorId by rememberSaveable { mutableStateOf<Long?>(null) }
    var creatingExercise by rememberSaveable { mutableStateOf(false) }
    var addCreatedExerciseToSession by rememberSaveable { mutableStateOf<Long?>(null) }
    var exerciseActionsId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showExercisePicker by rememberSaveable { mutableStateOf(false) }
    var showStartWorkout by rememberSaveable { mutableStateOf(false) }
    var showEditWorkout by rememberSaveable { mutableStateOf(false) }
    var finishConfirmation by rememberSaveable { mutableStateOf(false) }
    var finishedSummary by rememberSaveable { mutableStateOf<String?>(null) }
    var discardConfirmation by rememberSaveable { mutableStateOf(false) }
    var showGroupDialog by rememberSaveable { mutableStateOf(false) }
    var editedSetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var exerciseNotesEditorId by rememberSaveable { mutableStateOf<Long?>(null) }
    var focusedWorkoutId by rememberSaveable { mutableStateOf<Long?>(null) }
    var historyWorkoutEditorId by rememberSaveable { mutableStateOf<Long?>(null) }
    var focusedRoutineId by rememberSaveable { mutableStateOf<Long?>(null) }
    var exerciseDeleteCandidateId by rememberSaveable { mutableStateOf<Long?>(null) }
    var workoutDeleteCandidateId by rememberSaveable { mutableStateOf<Long?>(null) }
    var routineDeleteCandidateId by rememberSaveable { mutableStateOf<Long?>(null) }
    var creatingMachine by rememberSaveable { mutableStateOf(false) }
    var machineEditorId by rememberSaveable { mutableStateOf<Long?>(null) }
    var machineVersionSourceId by rememberSaveable { mutableStateOf<Long?>(null) }
    var inlineMachineWorkoutExerciseId by rememberSaveable { mutableStateOf<Long?>(null) }
    var inlineMachineExerciseId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingMachineExerciseId by rememberSaveable { mutableStateOf<Long?>(null) }
    var substituteWorkoutExerciseId by rememberSaveable { mutableStateOf<Long?>(null) }
    var createForSubstitutionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var routineEditorOpen by rememberSaveable { mutableStateOf(false) }
    var catalogSavePending by rememberSaveable { mutableStateOf(false) }
    var catalogSaveStarted by rememberSaveable { mutableStateOf(false) }
    val allExercises = state.exercises + state.archivedExercises
    val exerciseEditor = exerciseEditorId?.let { id -> allExercises.firstOrNull { it.id == id } }
    val exerciseActions = exerciseActionsId?.let { id -> allExercises.firstOrNull { it.id == id } }
    val exerciseDeleteCandidate = exerciseDeleteCandidateId?.let { id -> allExercises.firstOrNull { it.id == id } }
    val machineEditor = machineEditorId?.let { id -> (state.machines + state.archivedMachines).firstOrNull { it.id == id } }
    val machineVersionSource = machineVersionSourceId?.let { id -> (state.machines + state.archivedMachines).firstOrNull { it.id == id } }
    val pendingMachineExercise = pendingMachineExerciseId?.let { id -> state.exercises.firstOrNull { it.id == id } }
    val exerciseNotesEditor = exerciseNotesEditorId?.let { id -> state.activeWorkoutExercises.firstOrNull { it.workoutExercise.id == id } }
    val editedSet = editedSetId?.let { id ->
        val set = state.allSets.firstOrNull { it.id == id } ?: return@let null
        state.activeWorkoutExercises.firstOrNull { it.workoutExercise.id == set.workoutExerciseId }?.let { set to it }
    }
    val workoutDeleteCandidate = workoutDeleteCandidateId?.let { id -> state.allSessions.firstOrNull { it.id == id } }
    val historyWorkoutEditor = historyWorkoutEditorId?.let { id -> state.history.firstOrNull { it.id == id } }
    val routineDeleteCandidate = routineDeleteCandidateId?.let { id -> (state.routines + state.archivedRoutines).firstOrNull { it.id == id } }
    fun closeCatalogEditors() {
        creatingMachine = false
        machineEditorId = null
        machineVersionSourceId = null
        inlineMachineWorkoutExerciseId = null
        inlineMachineExerciseId = null
        creatingExercise = false
        exerciseEditorId = null
        addCreatedExerciseToSession = null
        createForSubstitutionId = null
        substituteWorkoutExerciseId = null
        catalogSavePending = false
        catalogSaveStarted = false
    }
    LaunchedEffect(operationStatus, catalogSavePending) {
        if (!catalogSavePending) return@LaunchedEffect
        when (operationStatus) {
            is OperationStatus.Running -> catalogSaveStarted = true
            is OperationStatus.Succeeded -> closeCatalogEditors()
            is OperationStatus.Failed -> {
                catalogSavePending = false
                catalogSaveStarted = false
            }
            OperationStatus.Idle -> Unit
        }
    }
    LaunchedEffect(createExerciseRequested, startWorkoutRequested) {
        if (createExerciseRequested) creatingExercise = true
        if (startWorkoutRequested) showStartWorkout = true
        if (createExerciseRequested || startWorkoutRequested) onExternalRequestConsumed()
    }
    LaunchedEffect(openSearchRequest, state.exercises, state.archivedExercises, state.allSessions, state.routines, state.archivedRoutines) {
        val request = openSearchRequest ?: return@LaunchedEffect
        when (request.domain) {
            SearchDomain.Exercise -> {
                val exercise = (state.exercises + state.archivedExercises)
                    .firstOrNull { it.id == request.id } ?: return@LaunchedEffect
                destination = GymDestination.Exercises
                exerciseActionsId = exercise.id
            }
            SearchDomain.Machine -> {
                val machine = (state.machines + state.archivedMachines)
                    .firstOrNull { it.id == request.id } ?: return@LaunchedEffect
                destination = GymDestination.Machines
                machineEditorId = machine.id
            }
            SearchDomain.Workout -> {
                if (state.allSessions.none { it.id == request.id }) return@LaunchedEffect
                if (state.activeSession?.id == request.id) {
                    focusedWorkoutId = null
                    destination = GymDestination.Workout
                } else {
                    focusedWorkoutId = request.id
                    destination = GymDestination.History
                }
            }
            SearchDomain.Routine -> {
                if ((state.routines + state.archivedRoutines).none { it.id == request.id }) return@LaunchedEffect
                focusedRoutineId = request.id
                destination = GymDestination.Routines
            }
            else -> return@LaunchedEffect
        }
        onOpenSearchRequestConsumed()
    }

    val view = LocalView.current
    val shouldKeepAwake = destination == GymDestination.Workout &&
        state.activeSession?.keepScreenAwake == true
    DisposableEffect(shouldKeepAwake) {
        view.keepScreenOn = shouldKeepAwake
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(destination) {
        onDestinationChange(destination)
    }
    BackHandler(enabled = destination in libraryGymDestinations && !routineEditorOpen) {
        destination = GymDestination.Library
        focusedRoutineId = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
        if (!routineEditorOpen) DestinationTabBar(
            selected = destination.takeUnless { it in libraryGymDestinations } ?: GymDestination.Library,
            destinations = primaryGymDestinations,
            onSelect = {
                destination = it
                focusedWorkoutId = null
                focusedRoutineId = null
            },
            label = GymDestination::name,
            testTagPrefix = "gym-destination",
        )
        if (!routineEditorOpen && destination in libraryGymDestinations) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WhipTextButton(
                    onClick = { destination = GymDestination.Library },
                    modifier = Modifier.testTag("gym-library-child-${destination.name}"),
                ) {
                    Icon(Icons.Outlined.ChevronLeft, contentDescription = null)
                    Text("Back to Library")
                }
            }
        }
        when (destination) {
            GymDestination.Library -> GymLibraryLanding(onOpen = { destination = it })
            GymDestination.Workout -> WorkoutContent(
                state = state,
                requestedWorkoutExerciseId = requestedWorkoutExerciseId,
                onRequestedWorkoutExerciseConsumed = onRequestedWorkoutExerciseConsumed,
                onStart = { showStartWorkout = true },
                onCreateExercise = { creatingExercise = true },
                onEditWorkout = { showEditWorkout = true },
                onAddExercise = { showExercisePicker = true },
                onAddSet = viewModel::addSet,
                onEditSet = { set, _ -> editedSetId = set.id },
                onEditExerciseNotes = { exerciseNotesEditorId = it.workoutExercise.id },
                onCompleteSet = { id, completed ->
                    viewModel.completeSet(id, completed)
                },
                onSaveQuickSet = { id, workoutExerciseId, draft, addNext, restOverrideSeconds ->
                    viewModel.saveQuickSet(id, workoutExerciseId, draft, addNext, restOverrideSeconds)
                },
                onDuplicateSet = viewModel::duplicateSet,
                onDeleteSet = viewModel::deleteSet,
                onUndoDeleteSet = viewModel::undoDeleteSet,
                onRemoveExercise = viewModel::removeWorkoutExercise,
                onSubstituteExercise = { workoutExerciseId ->
                    substituteWorkoutExerciseId = workoutExerciseId
                    showExercisePicker = true
                },
                onReorderExercises = viewModel::reorderWorkoutExercises,
                onReorderSets = viewModel::reorderSets,
                onFinish = { finishConfirmation = true },
                onDiscard = { discardConfirmation = true },
                onStartTimer = { sessionId, seconds ->
                    onRequestNotificationPermission()
                    viewModel.startRestTimer(sessionId, seconds)
                },
                onAdjustTimer = viewModel::adjustRestTimer,
                onStopTimer = viewModel::stopRestTimer,
                onRestTimerPresetsChange = viewModel::updateRestTimerPresets,
                onGroupExercises = { showGroupDialog = true },
            )
            GymDestination.Exercises -> ExerciseLibraryContent(
                state = state,
                onCreate = { creatingExercise = true },
                onOpen = { exerciseActionsId = it.id },
                onEdit = { exerciseEditorId = it.id },
            )
            GymDestination.Machines -> MachineLibraryContent(
                state = state,
                onCreate = { creatingMachine = true },
                onCreateExercise = { creatingExercise = true },
                onEdit = { machineEditorId = it.id },
                onArchive = viewModel::setMachineArchived,
                onNewVersion = { machineVersionSourceId = it.id },
                onDelete = { viewModel.previewMachineDeletion(it.id) },
            )
            GymDestination.Categories -> ExerciseCategoryContent(state, viewModel, dialogModifier)
            GymDestination.History -> WorkoutHistoryContent(
                history = state.history,
                state = state,
                onCopy = viewModel::duplicateWorkout,
                onResume = viewModel::resumeWorkout,
                onEditDetails = { historyWorkoutEditorId = it.id },
                onOpenActiveWorkout = { destination = GymDestination.Workout },
                onSaveAsRoutine = viewModel::saveWorkoutAsRoutine,
                onCopyExercise = viewModel::copyWorkoutExercise,
                onShare = { session -> shareWorkout(context, session, state) },
                onRestore = viewModel::restoreWorkout,
                onDelete = { workoutDeleteCandidateId = it.id },
                focusedWorkoutId = focusedWorkoutId,
                dialogModifier = dialogModifier,
            )
            GymDestination.Progress -> GymProgressContent(
                state,
                viewModel,
                onOpenExercises = { destination = GymDestination.Exercises },
                onOpenWorkout = { destination = GymDestination.Workout },
                dialogModifier = dialogModifier,
            )
            GymDestination.Routines -> RoutineContent(
                state = state,
                viewModel = viewModel,
                focusedRoutineId = focusedRoutineId,
                onDeleteRequest = { routineDeleteCandidateId = it.id },
                onOpenActiveWorkout = { destination = GymDestination.Workout },
                dialogModifier = dialogModifier,
                onEditorStateChange = { open ->
                    routineEditorOpen = open
                    onRoutineEditorStateChange(open)
                },
            )
            GymDestination.Tools -> GymToolsContent(
                state,
                onSavePreset = viewModel::savePlatePreset,
                onDeletePreset = viewModel::deletePlatePreset,
                dialogModifier = dialogModifier,
            )
        }
    }

    if (creatingMachine || machineEditor != null || machineVersionSource != null) {
        MachineEditorDialog(
            modifier = dialogModifier,
            machine = machineVersionSource ?: machineEditor,
            exercises = if (machineEditor == null) state.exercises else state.exercises + state.archivedExercises,
            definitionLocked = machineVersionSource == null && machineEditor?.let { selected -> state.allWorkoutExercises.any { it.machineId == selected.id } } == true,
            creatingVersion = machineVersionSource != null,
            initialExerciseId = inlineMachineExerciseId,
            onCreateVersion = machineEditor?.takeIf { selected -> state.allWorkoutExercises.any { it.machineId == selected.id } }?.let { selected ->
                {
                    machineEditorId = null
                    machineVersionSourceId = selected.id
                }
            },
            saving = catalogSavePending,
            onDismiss = {
                closeCatalogEditors()
            },
            onSave = { draft ->
                catalogSavePending = true
                catalogSaveStarted = false
                when {
                    machineVersionSource != null -> viewModel.createMachineVersion(machineVersionSource.id, draft)
                    inlineMachineWorkoutExerciseId != null -> viewModel.createMachineAndAssign(requireNotNull(inlineMachineWorkoutExerciseId), draft)
                    else -> viewModel.saveMachine(machineEditor?.id, draft)
                }
            },
        )
    }

    if (creatingExercise || exerciseEditor != null) {
        ExerciseEditorDialog(
            modifier = dialogModifier,
            exercise = exerciseEditor,
            categories = state.categories,
            selectedCategoryIds = state.categoryLinks.filter { it.exerciseId == exerciseEditor?.id }.mapTo(mutableSetOf()) { it.categoryId },
            defaultWeightUnit = state.appSettings.gymWeightUnitId,
            defaultRestSeconds = state.appSettings.defaultRestSeconds,
            defaultFormula = runCatching {
                EstimatedOneRepMaxFormula.valueOf(state.appSettings.oneRepMaxFormula)
            }.getOrDefault(EstimatedOneRepMaxFormula.Epley),
            platePresets = state.appSettings.platePresets,
            powerMode = state.appSettings.powerMode,
            saving = catalogSavePending,
            onDismiss = {
                closeCatalogEditors()
            },
            onSave = { draft ->
                catalogSavePending = true
                catalogSaveStarted = false
                val sessionId = addCreatedExerciseToSession
                val substitutionId = createForSubstitutionId
                if (substitutionId != null) {
                    viewModel.createExerciseAndSubstitute(substitutionId, draft)
                } else if (sessionId != null) {
                    viewModel.createExerciseAndAdd(sessionId, draft)
                } else {
                    viewModel.saveExercise(exerciseEditor?.id, draft)
                }
            },
        )
    }

    exerciseNotesEditor?.let { item ->
        WorkoutExerciseNotesDialog(
            modifier = dialogModifier,
            exerciseName = item.exercise.name,
            initialNotes = item.workoutExercise.notes,
            machines = state.machines.filter { it.exerciseId == item.exercise.id },
            selectedMachineId = item.workoutExercise.machineId,
            machineLocked = item.sets.any { it.deletedAtMillis == null },
            onDismiss = { exerciseNotesEditorId = null },
            onSave = { notes, machineId ->
                viewModel.updateWorkoutExercise(item.workoutExercise.id, notes, item.workoutExercise.groupId)
                if (machineId != item.workoutExercise.machineId) {
                    viewModel.setWorkoutExerciseMachine(item.workoutExercise.id, machineId)
                }
                exerciseNotesEditorId = null
            },
            onCreateMachine = {
                inlineMachineWorkoutExerciseId = item.workoutExercise.id
                inlineMachineExerciseId = item.exercise.id
                creatingMachine = true
            },
        )
    }

    exerciseActions?.let { exercise ->
        ExerciseActionsDialog(
            modifier = dialogModifier,
            exercise = exercise,
            onDismiss = { exerciseActionsId = null },
            onEdit = {
                exerciseEditorId = exercise.id
                exerciseActionsId = null
            },
            onFavorite = {
                viewModel.setExerciseFavorite(exercise.id, !exercise.favorite)
                exerciseActionsId = null
            },
            onDuplicate = {
                viewModel.duplicateExercise(exercise.id)
                exerciseActionsId = null
            },
            onArchive = {
                viewModel.setExerciseArchived(exercise.id, !exercise.archived)
                exerciseActionsId = null
            },
            onDelete = {
                exerciseDeleteCandidateId = exercise.id
                exerciseActionsId = null
            },
        )
    }

    exerciseDeleteCandidate?.let { exercise ->
        val placements = state.allWorkoutExercises.filter { it.exerciseId == exercise.id }
        val placementIds = placements.mapTo(mutableSetOf()) { it.id }
        val setCount = state.allSets.count { it.workoutExerciseId in placementIds }
        val routineCount = state.routineExercises.count { it.exerciseId == exercise.id }
        PermanentDeleteDialog(
            modifier = dialogModifier,
            title = "Delete ${exercise.name} Permanently?",
            impacts = listOf(
                "${placements.size} workout entr${if (placements.size == 1) "y" else "ies"} and $setCount set${if (setCount == 1) "" else "s"} will be removed from history",
                "$routineCount routine placement${if (routineCount == 1) "" else "s"} and affected graph presets will be updated",
                "Personal records and goal links sourced from this exercise will be removed or recalculated",
            ),
            onDismiss = { exerciseDeleteCandidateId = null },
            onConfirm = { viewModel.deleteExercisePermanently(exercise.id); exerciseDeleteCandidateId = null },
        )
    }

    workoutDeleteCandidate?.let { workout ->
        val placements = state.allWorkoutExercises.filter { it.sessionId == workout.id }
        val placementIds = placements.mapTo(mutableSetOf()) { it.id }
        val setCount = state.allSets.count { it.workoutExerciseId in placementIds }
        PermanentDeleteDialog(
            modifier = dialogModifier,
            title = "Delete ${workout.name.ifBlank { "Workout" }} Permanently?",
            impacts = listOf(
                "${placements.size} exercise entr${if (placements.size == 1) "y" else "ies"} and $setCount set${if (setCount == 1) "" else "s"} will be removed",
                "Personal records, goal contributions, and workout automations will be recalculated",
                "Your exercise library and routine templates will remain",
            ),
            onDismiss = { workoutDeleteCandidateId = null },
            onConfirm = { viewModel.deleteWorkoutPermanently(workout.id); workoutDeleteCandidateId = null },
        )
    }

    routineDeleteCandidate?.let { routine ->
        val days = state.routineDays.filter { it.routineId == routine.id }
        val dayIds = days.mapTo(mutableSetOf()) { it.id }
        val exerciseCount = state.routineExercises.count { it.routineDayId in dayIds }
        val historyCount = state.allSessions.count { it.sourceRoutineId == routine.id }
        PermanentDeleteDialog(
            modifier = dialogModifier,
            title = "Delete ${routine.name} Permanently?",
            impacts = listOf(
                "${days.size} template day${if (days.size == 1) "" else "s"} and $exerciseCount planned exercise entr${if (exerciseCount == 1) "y" else "ies"} will be removed",
                "$historyCount completed workout${if (historyCount == 1) "" else "s"} created from it will be preserved as ordinary history",
            ),
            onDismiss = { routineDeleteCandidateId = null },
            onConfirm = { viewModel.deleteRoutinePermanently(routine.id); routineDeleteCandidateId = null },
        )
    }

    machineDeletionImpact?.let { impact ->
        MachinePermanentDeleteDialog(
            modifier = dialogModifier,
            impact = impact,
            onDismiss = viewModel::dismissMachineDeletion,
            onConfirm = viewModel::confirmMachineDeletion,
            onReviewRoutines = {
                viewModel.dismissMachineDeletion()
                destination = GymDestination.Routines
            },
            onOpenActiveWorkout = {
                viewModel.dismissMachineDeletion()
                destination = GymDestination.Workout
            },
            onBackUpFirst = {
                viewModel.dismissMachineDeletion()
                onOpenBackupSettings()
            },
            deleting = machineDeletionInProgress,
        )
    }

    if (showExercisePicker) {
        val preferredSubstitutions = substituteWorkoutExerciseId?.let { id ->
            state.activeWorkoutExercises.firstOrNull { it.workoutExercise.id == id }
                ?.workoutExercise?.alternativeExerciseIdsSnapshot
        }.orEmpty()
        ExercisePickerDialog(
            modifier = dialogModifier,
            exercises = state.exercises,
            preferredIds = preferredSubstitutions,
            onDismiss = { showExercisePicker = false; substituteWorkoutExerciseId = null },
            onPick = { exercise ->
                val machines = state.machines.filter { it.exerciseId == exercise.id }
                if (machines.isEmpty()) {
                    val substitutionId = substituteWorkoutExerciseId
                    if (substitutionId != null) viewModel.substituteWorkoutExercise(substitutionId, exercise.id, null)
                    else state.activeSession?.let { viewModel.addExercise(it.id, exercise.id) }
                    substituteWorkoutExerciseId = null
                    showExercisePicker = false
                } else {
                    pendingMachineExerciseId = exercise.id
                    showExercisePicker = false
                }
            },
            onCreate = {
                createForSubstitutionId = substituteWorkoutExerciseId
                addCreatedExerciseToSession = state.activeSession?.id.takeIf { createForSubstitutionId == null }
                creatingExercise = true
                showExercisePicker = false
            },
        )
    }

    pendingMachineExercise?.let { exercise ->
        MachineChoiceDialog(
            modifier = dialogModifier,
            exercise = exercise,
            machines = state.machines.filter { it.exerciseId == exercise.id },
            onDismiss = { pendingMachineExerciseId = null; substituteWorkoutExerciseId = null },
            onChoose = { machineId ->
                val substitutionId = substituteWorkoutExerciseId
                if (substitutionId != null) viewModel.substituteWorkoutExercise(substitutionId, exercise.id, machineId)
                else state.activeSession?.let { viewModel.addExercise(it.id, exercise.id, machineId) }
                substituteWorkoutExerciseId = null
                pendingMachineExerciseId = null
            },
        )
    }

    if (showStartWorkout) {
        WorkoutEditorDialog(
            modifier = dialogModifier,
            session = null,
            initialDate = LocalDate.now(state.appSettings.zoneId()),
            initialKeepAwake = state.appSettings.keepScreenAwake,
            onDismiss = { showStartWorkout = false },
            onStart = { name, notes, date, keepAwake ->
                viewModel.startWorkout(name, notes, date, keepAwake)
                showStartWorkout = false
            },
        )
    }

    if (showEditWorkout) {
        state.activeSession?.let { session ->
            WorkoutEditorDialog(
                modifier = dialogModifier,
                session = session,
                initialDate = session.localDate,
                onDismiss = { showEditWorkout = false },
                onStart = { name, notes, _, keepAwake ->
                    viewModel.updateWorkout(session.id, name, notes, keepAwake)
                    showEditWorkout = false
                },
            )
        }
    }

    historyWorkoutEditor?.let { session ->
        WorkoutEditorDialog(
            modifier = dialogModifier,
            session = session,
            initialDate = session.localDate,
            onDismiss = { historyWorkoutEditorId = null },
            onStart = { name, notes, _, keepAwake ->
                viewModel.updateWorkout(session.id, name, notes, keepAwake)
                historyWorkoutEditorId = null
            },
        )
    }

    editedSet?.let { (set, item) ->
        val effectiveShowRpe = item.exercise.showRpe ?: state.appSettings.showGymRpe
        WorkoutSetEditorDialog(
            modifier = dialogModifier,
            set = set,
            exercise = item.exercise,
            workoutExercise = item.workoutExercise,
            machine = item.machine,
            preferredWeightUnitId = state.appSettings.gymWeightUnitId,
            preferredDistanceUnitId = state.appSettings.distanceUnitId,
            showRpe = effectiveShowRpe,
            showRir = (item.exercise.showRir ?: state.appSettings.showGymRir) && !effectiveShowRpe,
            showTempo = item.exercise.showTempo ?: state.appSettings.showGymTempo,
            onDismiss = { editedSetId = null },
            onSave = { draft ->
                viewModel.updateSet(set.id, draft)
                editedSetId = null
            },
        )
    }

    if (finishConfirmation) {
        val completedSets = state.activeWorkoutExercises.sumOf { item -> item.sets.count { it.completed && it.deletedAtMillis == null } }
        val incompleteSets = state.activeWorkoutExercises.sumOf { item -> item.sets.count { !it.completed && it.deletedAtMillis == null } }
        ConfirmationDialog(
            modifier = dialogModifier,
            title = "Review and Finish Workout?",
            message = "${quantityLabel(state.activeWorkoutExercises.size, "exercise")} · ${quantityLabel(completedSets, "completed set")}" +
                if (incompleteSets > 0) " · $incompleteSets planned sets remain incomplete and stay visible in History." else
                    ". Every saved value and equipment snapshot remains editable in History.",
            confirmLabel = "Finish",
            onDismiss = { finishConfirmation = false },
            onConfirm = {
                state.activeSession?.let { session ->
                    finishedSummary = buildString {
                        append(session.name.ifBlank { "Workout" })
                        append("\n${quantityLabel(state.activeWorkoutExercises.size, "exercise")} · ${quantityLabel(completedSets, "completed set")}")
                        state.summary?.let { summary ->
                            append(" · ${summary.repetitions} reps")
                            append("\n${formatNumber(massFromKilograms(summary.volumeKg, state.appSettings.gymWeightUnitId), state.appSettings.numberPrecision)} ${unitSymbol(state.appSettings.gymWeightUnitId)} volume · ${formatDuration(summary.elapsedSeconds)}")
                        }
                        if (incompleteSets > 0) append("\n$incompleteSets incomplete planned sets were retained for routine comparison.")
                    }
                    viewModel.finishWorkout(session.id)
                }
                finishConfirmation = false
            },
        )
    }
    finishedSummary?.let { summary ->
        PaneAwareAlertDialog(
            modifier = dialogModifier,
            onDismissRequest = { finishedSummary = null },
            title = { Text("Workout Saved") },
            text = { Text(summary) },
            confirmButton = {
                WhipTextButton(onClick = { finishedSummary = null; destination = GymDestination.History }) { Text("View History") }
            },
            dismissButton = { WhipTextButton(onClick = { finishedSummary = null }) { Text("Done") } },
        )
    }
    if (discardConfirmation) {
        ConfirmationDialog(
            modifier = dialogModifier,
            title = "Discard Workout?",
            message = "This hides the session from normal history. Your exercise library is not changed.",
            confirmLabel = "Discard",
            onDismiss = { discardConfirmation = false },
            onConfirm = {
                state.activeSession?.let { viewModel.discardWorkout(it.id) }
                discardConfirmation = false
            },
        )
    }
    if (showGroupDialog) {
        WorkoutGroupDialog(
            modifier = dialogModifier,
            exercises = state.activeWorkoutExercises,
            onDismiss = { showGroupDialog = false },
            onCreate = { name, type, ids ->
                state.activeSession?.let { session ->
                    viewModel.createGroup(
                        session.id,
                        name,
                        type,
                        ids,
                    )
                }
                showGroupDialog = false
            },
        )
    }
}

@Composable
private fun GymLibraryLanding(onOpen: (GymDestination) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 112.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            WhipPageHeader(
                title = "Library",
                supportingText = "Manage the reusable building blocks and utilities behind your workouts.",
            )
        }
        items(libraryGymDestinations, key = GymDestination::name) { destination ->
            NavigationRow(
                title = destination.name,
                supportingText = when (destination) {
                    GymDestination.Routines -> "Workout templates and training days"
                    GymDestination.Exercises -> "Exercise catalog and tracking setup"
                    GymDestination.Machines -> "Equipment profiles and resistance settings"
                    GymDestination.Categories -> "Organize exercises for faster browsing"
                    GymDestination.Tools -> "Plate calculator, rep maxes, and utilities"
                    else -> ""
                },
                onClick = { onOpen(destination) },
                modifier = Modifier.testTag("gym-library-${destination.name}"),
            )
        }
    }
}

internal fun selectNextWorkoutSet(
    items: List<WorkoutExerciseUi>,
    nextExerciseByGroup: Map<Long, Long?>,
): Pair<WorkoutExerciseUi, WorkoutSet>? {
    fun firstIncomplete(item: WorkoutExerciseUi) = item.sets.sortedBy(WorkoutSet::position)
        .firstOrNull { set -> !set.completed && set.deletedAtMillis == null }

    return items.asSequence()
        .mapNotNull { item ->
            val designatedGroupMemberId = item.group?.let { nextExerciseByGroup[it.id] }
            if (designatedGroupMemberId != null && designatedGroupMemberId != item.workoutExercise.id) return@mapNotNull null
            firstIncomplete(item)?.let { set -> item to set }
        }
        .firstOrNull()
        ?: items.asSequence().mapNotNull { item -> firstIncomplete(item)?.let { set -> item to set } }.firstOrNull()
}

@Composable
private fun WorkoutContent(
    state: GymUiState,
    requestedWorkoutExerciseId: Long? = null,
    onRequestedWorkoutExerciseConsumed: () -> Unit = {},
    onStart: () -> Unit,
    onCreateExercise: () -> Unit,
    onEditWorkout: () -> Unit,
    onAddExercise: () -> Unit,
    onAddSet: (Long) -> Unit,
    onEditSet: (WorkoutSet, WorkoutExerciseUi) -> Unit,
    onEditExerciseNotes: (WorkoutExerciseUi) -> Unit,
    onCompleteSet: (Long, Boolean) -> Unit,
    onSaveQuickSet: (Long, Long, WorkoutSetDraft, Boolean, Int?) -> Unit,
    onDuplicateSet: (Long) -> Unit,
    onDeleteSet: (Long) -> Unit,
    onUndoDeleteSet: (Long) -> Unit,
    onRemoveExercise: (Long) -> Unit,
    onSubstituteExercise: (Long) -> Unit,
    onReorderExercises: (Long, List<Long>) -> Unit,
    onReorderSets: (Long, List<Long>) -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    onStartTimer: (Long, Int) -> Unit,
    onAdjustTimer: (Long, Int) -> Unit,
    onStopTimer: (Long) -> Unit,
    onRestTimerPresetsChange: (List<Int>) -> Unit,
    onGroupExercises: () -> Unit,
) {
    val session = state.activeSession
    if (session == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WhipPageHeader(
                title = "Current Workout",
                supportingText = "Log sets with fast, repeatable controls while you train.",
            )
            WhipEmptyState(
                title = "No Workout in Progress",
                supportingText = if (state.exercises.isEmpty()) {
                    "Create exercises once, then reuse them in workouts and routines. You can also start empty and create one while logging."
                } else {
                    "Start a workout, then choose from your reusable exercise library."
                },
                primaryActionLabel = if (state.exercises.isEmpty()) "Create First Exercise" else "Start Workout",
                onPrimaryAction = if (state.exercises.isEmpty()) onCreateExercise else onStart,
                secondaryActionLabel = "Start Empty Workout".takeIf { state.exercises.isEmpty() },
                onSecondaryAction = onStart.takeIf { state.exercises.isEmpty() },
            )
        }
        return
    }

    val nextExerciseByGroup = state.activeWorkoutExercises
        .filter { it.group != null }
        .groupBy { requireNotNull(it.group).id }
        .mapValues { (_, members) ->
            val ordered = members.sortedBy { it.workoutExercise.position }
            val lastCompletedMember = ordered.mapNotNull { member ->
                member.sets.filter { it.completed && it.deletedAtMillis == null }
                    .maxOfOrNull { it.completedAtMillis ?: it.updatedAtMillis }
                    ?.let { completedAt -> member to completedAt }
            }.maxByOrNull { it.second }?.first
            if (lastCompletedMember == null) ordered.firstOrNull()?.workoutExercise?.id
            else ordered[(ordered.indexOf(lastCompletedMember) + 1) % ordered.size].workoutExercise.id
        }
    val nextSet = selectNextWorkoutSet(state.activeWorkoutExercises, nextExerciseByGroup)
    val workoutListState = rememberLazyListState()
    val workoutScrollScope = rememberCoroutineScope()
    LaunchedEffect(requestedWorkoutExerciseId, state.activeWorkoutExercises) {
        val requestedId = requestedWorkoutExerciseId ?: return@LaunchedEffect
        val exerciseIndex = state.activeWorkoutExercises.indexOfFirst { it.workoutExercise.id == requestedId }
        if (exerciseIndex >= 0) workoutListState.scrollToItem(exerciseIndex + 2)
        onRequestedWorkoutExerciseConsumed()
    }
    var lastFocusedSetId by rememberSaveable(session.id) { mutableStateOf(nextSet?.second?.id) }
    LaunchedEffect(nextSet?.second?.id) {
        val next = nextSet ?: return@LaunchedEffect
        if (lastFocusedSetId != null && lastFocusedSetId != next.second.id) {
            val exerciseIndex = state.activeWorkoutExercises.indexOfFirst {
                it.workoutExercise.id == next.first.workoutExercise.id
            }
            if (exerciseIndex >= 0) workoutListState.animateScrollToItem(exerciseIndex + 2)
        }
        lastFocusedSetId = next.second.id
    }
    var workoutRestOverrideSeconds by rememberSaveable(session.id) { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = workoutListState,
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            WhipPageHeader(
                title = session.name.ifBlank { "Workout" },
                supportingText = session.localDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
            ) {
                WhipTextButton(onClick = onEditWorkout) { Text("Edit Workout") }
            }
            state.summary?.let { summary ->
                Text(
                    "${quantityLabel(summary.exerciseCount, "exercise")} · ${quantityLabel(summary.completedSetCount, "set")} · " +
                        "${quantityLabel(summary.repetitions, "rep")} · ${formatNumber(massFromKilograms(summary.volumeKg, state.appSettings.gymWeightUnitId), state.appSettings.numberPrecision)} " +
                        "${unitSymbol(state.appSettings.gymWeightUnitId)} volume · " +
                        formatDuration(summary.elapsedSeconds),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        stickyHeader {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth().testTag("workout-execution-lane"),
            ) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    nextSet?.let { (exerciseItem, set) ->
                        Text(
                            "NEXT · ${exerciseItem.exercise.name} · Set ${set.position + 1}" +
                                set.prescriptionLabel(state.appSettings.gymWeightUnitId, state.appSettings.numberPrecision, exerciseItem.workoutExercise)?.let { " · $it" }.orEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClickLabel = "Jump to next incomplete set") {
                                    val exerciseIndex = state.activeWorkoutExercises.indexOfFirst {
                                        it.workoutExercise.id == exerciseItem.workoutExercise.id
                                    }
                                    if (exerciseIndex >= 0) workoutScrollScope.launch {
                                        workoutListState.scrollToItem(exerciseIndex + 2)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("next-set-focus"),
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    RestTimerCard(
                        session = session,
                        remaining = state.restSecondsRemaining,
                        selectedSeconds = workoutRestOverrideSeconds ?: state.appSettings.defaultRestSeconds,
                        presetSeconds = state.appSettings.restTimerPresetSeconds,
                        notificationPermissionRequested = state.appSettings.notificationPermissionRequested,
                        onSelectedSecondsChange = { workoutRestOverrideSeconds = it },
                        onPresetSecondsChange = onRestTimerPresetsChange,
                        onStart = onStartTimer,
                        onAdjust = onAdjustTimer,
                        onStop = onStopTimer,
                    )
                }
            }
        }
        if (state.activeWorkoutExercises.isEmpty()) {
            item {
                WhipEmptyState(
                    title = "Add Your First Exercise",
                    supportingText = "Choose a reusable exercise from your library or create one without leaving this workout.",
                    primaryActionLabel = "Add Exercise",
                    onPrimaryAction = onAddExercise,
                    secondaryActionLabel = "Create New Exercise",
                    onSecondaryAction = onCreateExercise,
                )
            }
        }
        items(state.activeWorkoutExercises.size, key = { state.activeWorkoutExercises[it].workoutExercise.id }) { itemIndex ->
            val item = state.activeWorkoutExercises[itemIndex]
            val effectiveShowRpe = item.exercise.showRpe ?: state.appSettings.showGymRpe
            WorkoutExerciseCard(
                item = item,
                preferredWeightUnitId = state.appSettings.gymWeightUnitId,
                preferredDistanceUnitId = state.appSettings.distanceUnitId,
                numberPrecision = state.appSettings.numberPrecision,
                compactRows = state.appSettings.gymCompactSetRows,
                showRpe = effectiveShowRpe,
                showRir = (item.exercise.showRir ?: state.appSettings.showGymRir) && !effectiveShowRpe,
                nextSetId = nextSet?.second?.id,
                nextInGroup = item.group?.let { nextExerciseByGroup[it.id] == item.workoutExercise.id } == true,
                canMoveUp = itemIndex > 0,
                canMoveDown = itemIndex < state.activeWorkoutExercises.lastIndex,
                onMoveUp = {
                    val ids = state.activeWorkoutExercises.map { it.workoutExercise.id }.toMutableList()
                    java.util.Collections.swap(ids, itemIndex, itemIndex - 1)
                    onReorderExercises(session.id, ids)
                },
                onMoveDown = {
                    val ids = state.activeWorkoutExercises.map { it.workoutExercise.id }.toMutableList()
                    java.util.Collections.swap(ids, itemIndex, itemIndex + 1)
                    onReorderExercises(session.id, ids)
                },
                onRemoveExercise = { onRemoveExercise(item.workoutExercise.id) },
                onSubstituteExercise = { onSubstituteExercise(item.workoutExercise.id) },
                onAddSet = { onAddSet(item.workoutExercise.id) },
                onEditSet = { onEditSet(it, item) },
                onEditNotes = { onEditExerciseNotes(item) },
                onCompleteSet = onCompleteSet,
                onSaveQuickSet = { setId, draft, addNext ->
                    onSaveQuickSet(
                        setId,
                        item.workoutExercise.id,
                        draft,
                        addNext,
                        workoutRestOverrideSeconds,
                    )
                },
                onDuplicateSet = onDuplicateSet,
                onDeleteSet = onDeleteSet,
                onUndoDeleteSet = onUndoDeleteSet,
                onReorderSets = { ids -> onReorderSets(item.workoutExercise.id, ids) },
            )
        }
        item {
            WhipOutlinedButton(onClick = onAddExercise, modifier = Modifier.fillMaxWidth()) {
                Text("Add Exercise")
            }
        }
        if (state.activeWorkoutExercises.size >= 2) {
            item {
                WhipOutlinedButton(onClick = onGroupExercises, modifier = Modifier.fillMaxWidth()) {
                    Text("Group Exercises as Superset / Circuit")
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WhipButton(onClick = onFinish, modifier = Modifier.weight(1f)) { Text("Finish") }
                WhipOutlinedButton(onClick = onDiscard, modifier = Modifier.weight(1f)) { Text("Discard") }
            }
        }
    }
}

@Composable
internal fun WorkoutExerciseCard(
    item: WorkoutExerciseUi,
    preferredWeightUnitId: String,
    preferredDistanceUnitId: String,
    numberPrecision: Int,
    compactRows: Boolean,
    showRpe: Boolean,
    showRir: Boolean,
    nextSetId: Long?,
    nextInGroup: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemoveExercise: () -> Unit,
    onSubstituteExercise: () -> Unit,
    onAddSet: () -> Unit,
    onEditSet: (WorkoutSet) -> Unit,
    onEditNotes: () -> Unit,
    onCompleteSet: (Long, Boolean) -> Unit,
    onSaveQuickSet: (Long, WorkoutSetDraft, Boolean) -> Unit,
    onDuplicateSet: (Long) -> Unit,
    onDeleteSet: (Long) -> Unit,
    onUndoDeleteSet: (Long) -> Unit,
    onReorderSets: (List<Long>) -> Unit,
) {
    var actionMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var setMenuId by rememberSaveable { mutableStateOf<Long?>(null) }
    var setupExpanded by rememberSaveable(item.workoutExercise.id) { mutableStateOf(false) }
    var completedSetsExpanded by rememberSaveable(item.workoutExercise.id) { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(if (compactRows) 9.dp else 14.dp), verticalArrangement = Arrangement.spacedBy(if (compactRows) 4.dp else 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.exercise.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Box {
                    IconButton(onClick = { actionMenuExpanded = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "More options for ${item.exercise.name}", modifier = Modifier.size(28.dp))
                    }
                    DropdownMenu(expanded = actionMenuExpanded, onDismissRequest = { actionMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Move Up") },
                            enabled = canMoveUp,
                            leadingIcon = { Icon(Icons.Outlined.ArrowUpward, contentDescription = null) },
                            onClick = { actionMenuExpanded = false; onMoveUp() },
                        )
                        DropdownMenuItem(
                            text = { Text("Move Down") },
                            enabled = canMoveDown,
                            leadingIcon = { Icon(Icons.Outlined.ArrowDownward, contentDescription = null) },
                            onClick = { actionMenuExpanded = false; onMoveDown() },
                        )
                        DropdownMenuItem(
                            text = { Text("Substitute Exercise") },
                            onClick = { actionMenuExpanded = false; onSubstituteExercise() },
                        )
                        DropdownMenuItem(
                            text = { Text("Remove from Workout") },
                            leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                            onClick = { actionMenuExpanded = false; onRemoveExercise() },
                        )
                    }
                }
            }
            if (item.workoutExercise.machineNameSnapshot.isNotBlank()) {
                Text(
                    "Machine: ${item.workoutExercise.machineNameSnapshot}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            item.group?.let {
                Text(
                    "${it.type.name}: ${it.name}${if (nextInGroup) " · Next in rotation" else ""}",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = if (nextInGroup) FontWeight.Bold else FontWeight.Normal,
                )
            }
            val orderedSets = item.sets.sortedBy(WorkoutSet::position)
            val completedSetCount = orderedSets.count { it.completed && it.deletedAtMillis == null }
            val hasIncompleteSet = orderedSets.any { !it.completed && it.deletedAtMillis == null }
            val focusedEntry = orderedSets.withIndex().firstOrNull { (_, set) ->
                set.id == nextSetId && set.deletedAtMillis == null
            }
            focusedEntry?.let { (index, set) ->
                val suggestedSet = orderedSets.take(index).lastOrNull { candidate ->
                    candidate.completed && candidate.deletedAtMillis == null
                } ?: item.previousSets.maxByOrNull { previous ->
                    previous.completedAtMillis ?: previous.updatedAtMillis
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().testTag("active-set-composer"),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Up Next · Set ${index + 1}${set.classification.uiLabel().takeUnless { it == "Working" }?.let { " · $it" }.orEmpty()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                set.prescriptionLabel(preferredWeightUnitId, numberPrecision, item.workoutExercise)?.let { target ->
                                    Text(
                                        "Target · $target",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Box {
                                IconButton(onClick = { setMenuId = set.id }, modifier = Modifier.size(48.dp)) {
                                    Icon(
                                        Icons.Outlined.MoreVert,
                                        contentDescription = "More options for set ${index + 1}",
                                        modifier = Modifier.size(26.dp),
                                    )
                                }
                                WorkoutSetActionsMenu(
                                    expanded = setMenuId == set.id,
                                    onDismiss = { setMenuId = null },
                                    canMoveUp = index > 0,
                                    canMoveDown = index < orderedSets.lastIndex,
                                    onDuplicate = { setMenuId = null; onDuplicateSet(set.id) },
                                    onMoveUp = {
                                        setMenuId = null
                                        val ids = orderedSets.map(WorkoutSet::id).toMutableList()
                                        java.util.Collections.swap(ids, index, index - 1)
                                        onReorderSets(ids)
                                    },
                                    onMoveDown = {
                                        setMenuId = null
                                        val ids = orderedSets.map(WorkoutSet::id).toMutableList()
                                        java.util.Collections.swap(ids, index, index + 1)
                                        onReorderSets(ids)
                                    },
                                    onRemove = { setMenuId = null; onDeleteSet(set.id) },
                                )
                            }
                        }
                        QuickSetEntry(
                            set = set,
                            exercise = item.exercise,
                            workoutExercise = item.workoutExercise,
                            machine = item.machine,
                            preferredWeightUnitId = preferredWeightUnitId,
                            preferredDistanceUnitId = preferredDistanceUnitId,
                            showRpe = showRpe,
                            showRir = showRir,
                            suggestedSet = suggestedSet,
                            onMoreDetails = { onEditSet(set) },
                            onSave = { draft, addNext -> onSaveQuickSet(set.id, draft, addNext) },
                        )
                    }
                }
            }
            if (completedSetCount > 0 && hasIncompleteSet) {
                DisclosureButton(
                    label = quantityLabel(completedSetCount, "Completed Set"),
                    expanded = completedSetsExpanded,
                    onClick = { completedSetsExpanded = !completedSetsExpanded },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            orderedSets.withIndex()
                .filter { (_, set) ->
                    set.id != focusedEntry?.value?.id &&
                        (!set.completed || completedSetsExpanded || !hasIncompleteSet)
                }
                .forEach { (index, set) ->
                if (set.deletedAtMillis != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Set ${index + 1} removed", modifier = Modifier.weight(1f))
                        WhipTextButton(onClick = { onUndoDeleteSet(set.id) }) { Text("Undo") }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClickLabel = "Edit set ${index + 1}") { onEditSet(set) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (set.completed) {
                            Checkbox(
                                checked = true,
                                onCheckedChange = { onCompleteSet(set.id, false) },
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .semantics { contentDescription = "Incomplete set ${index + 1}; enter its required values to save" },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("○", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Set ${index + 1} · ${set.shortLabel(preferredWeightUnitId, preferredDistanceUnitId, numberPrecision, item.workoutExercise, item.exercise.weightUnitId).replace("Empty set", "Ready")}",
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (!compactRows) Text(
                                buildString {
                                    append(set.classification.uiLabel())
                                    if (set.planned) append(" · planned")
                                    set.rpe?.let { append(" · RPE $it") }
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            set.prescriptionLabel(preferredWeightUnitId, numberPrecision, item.workoutExercise)?.let { target ->
                                Text("Target: $target", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                        Box {
                            IconButton(onClick = { setMenuId = set.id }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = "More options for set ${index + 1}", modifier = Modifier.size(26.dp))
                            }
                            WorkoutSetActionsMenu(
                                expanded = setMenuId == set.id,
                                onDismiss = { setMenuId = null },
                                canMoveUp = index > 0,
                                canMoveDown = index < orderedSets.lastIndex,
                                onDuplicate = { setMenuId = null; onDuplicateSet(set.id) },
                                onMoveUp = {
                                    setMenuId = null
                                    val ids = orderedSets.map(WorkoutSet::id).toMutableList()
                                    java.util.Collections.swap(ids, index, index - 1)
                                    onReorderSets(ids)
                                },
                                onMoveDown = {
                                    setMenuId = null
                                    val ids = orderedSets.map(WorkoutSet::id).toMutableList()
                                    java.util.Collections.swap(ids, index, index + 1)
                                    onReorderSets(ids)
                                },
                                onRemove = { setMenuId = null; onDeleteSet(set.id) },
                            )
                        }
                    }
                }
            }
            WhipTextButton(onClick = onAddSet) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add Set")
            }
            if (item.previousSets.isNotEmpty()) {
                val omittedSets = item.previousSetCount - item.previousSets.size
                Text(
                    "Previous workout · ${item.previousSets.joinToString(" · ") { it.shortLabel(preferredWeightUnitId, preferredDistanceUnitId, numberPrecision, item.workoutExercise, item.exercise.weightUnitId) }}" +
                        if (omittedSets > 0) " · +$omittedSets more in History" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DisclosureButton(
                label = "Exercise Details",
                expanded = setupExpanded,
                onClick = { setupExpanded = !setupExpanded },
                modifier = Modifier.fillMaxWidth(),
            )
            if (setupExpanded) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        item.workoutExercise.machineConfigurationSnapshot.takeIf(String::isNotBlank)?.let {
                            Text("Saved Machine Setup", fontWeight = FontWeight.Bold)
                            Text(it)
                        }
                        item.exercise.notes.takeIf(String::isNotBlank)?.let {
                            Text("Exercise Cues", fontWeight = FontWeight.Bold)
                            Text(it)
                        }
                        item.workoutExercise.notes.takeIf(String::isNotBlank)?.let {
                            Text("This Workout", fontWeight = FontWeight.Bold)
                            Text(it)
                        }
                        WhipTextButton(onClick = onEditNotes) {
                            Text(if (item.workoutExercise.notes.isBlank()) "Add Workout Note" else "Edit Workout Note")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutSetActionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onDuplicate: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("Duplicate Set") }, onClick = onDuplicate)
        DropdownMenuItem(
            text = { Text("Move Up") },
            enabled = canMoveUp,
            leadingIcon = { Icon(Icons.Outlined.ArrowUpward, contentDescription = null) },
            onClick = onMoveUp,
        )
        DropdownMenuItem(
            text = { Text("Move Down") },
            enabled = canMoveDown,
            leadingIcon = { Icon(Icons.Outlined.ArrowDownward, contentDescription = null) },
            onClick = onMoveDown,
        )
        DropdownMenuItem(
            text = { Text("Remove Set") },
            leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
            onClick = onRemove,
        )
    }
}

@Composable
private fun ReorderHandle(
    label: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    var accumulatedDrag by remember { mutableStateOf(0f) }
    val thresholdPx = with(LocalDensity.current) { 40.dp.toPx() }
    Icon(
        imageVector = Icons.Outlined.DragHandle,
        contentDescription = null,
        modifier = Modifier
            .size(48.dp)
            .pointerInput(canMoveUp, canMoveDown) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { accumulatedDrag = 0f },
                    onDragCancel = { accumulatedDrag = 0f },
                    onDragEnd = { accumulatedDrag = 0f },
                    onDrag = { change, amount ->
                        change.consume()
                        accumulatedDrag += amount.y
                        when {
                            accumulatedDrag <= -thresholdPx && canMoveUp -> {
                                onMoveUp()
                                accumulatedDrag = 0f
                            }
                            accumulatedDrag >= thresholdPx && canMoveDown -> {
                                onMoveDown()
                                accumulatedDrag = 0f
                            }
                        }
                    },
                )
            }
            .semantics {
                contentDescription = "Reorder $label"
                customActions = buildList {
                    if (canMoveUp) add(CustomAccessibilityAction("Move $label up") { onMoveUp(); true })
                    if (canMoveDown) add(CustomAccessibilityAction("Move $label down") { onMoveDown(); true })
                }
            },
    )
}

@Composable
internal fun QuickSetEntry(
    set: WorkoutSet,
    exercise: Exercise,
    workoutExercise: WorkoutExercise,
    machine: GymMachine?,
    preferredWeightUnitId: String,
    preferredDistanceUnitId: String,
    showRpe: Boolean,
    showRir: Boolean,
    suggestedSet: WorkoutSet? = null,
    onMoreDetails: () -> Unit = {},
    onSave: (WorkoutSetDraft, Boolean) -> Unit,
) {
    val policyExercise = workoutExercise.applyPolicySnapshot(exercise)
    val machineType = workoutExercise.machineLoadTypeSnapshot
    val weightUnitId = set.enteredWeightUnitId ?: if (machineType == MachineLoadType.Mass) {
        workoutExercise.machineUnitIdSnapshot
    } else {
        policyExercise.weightUnitId.ifBlank { preferredWeightUnitId }
    }
    val distanceUnitId = set.enteredDistanceUnitId ?: preferredDistanceUnitId
    val editorKey = "quick-set-${set.id}"
    var weight by rememberSaveable(editorKey) {
        mutableStateOf(
            set.enteredWeight?.let(::editableNumber)
                ?: set.canonicalWeightKg?.let { editableNumber(massFromKilograms(it, weightUnitId)) }
                ?: "",
        )
    }
    var machineSetting by rememberSaveable(editorKey) { mutableStateOf(set.machineLoadValue?.let(::editableNumber).orEmpty()) }
    var reps by rememberSaveable(editorKey) { mutableStateOf(set.repetitions?.toString().orEmpty()) }
    var distance by rememberSaveable(editorKey) {
        mutableStateOf(
            set.enteredDistance?.let(::editableNumber)
                ?: set.canonicalDistanceMetres?.let { editableNumber(distanceFromMetres(it, distanceUnitId)) }
                ?: "",
        )
    }
    var duration by rememberSaveable(editorKey) { mutableStateOf(set.durationSeconds?.toString().orEmpty()) }
    var bodyweight by rememberSaveable(editorKey) {
        mutableStateOf(set.bodyweightKg?.let { editableNumber(massFromKilograms(it, weightUnitId)) }.orEmpty())
    }
    var rpe by rememberSaveable(editorKey) { mutableStateOf(set.rpe?.let(::editableNumber).orEmpty()) }
    var rir by rememberSaveable(editorKey) { mutableStateOf(set.rir?.let(::editableNumber).orEmpty()) }
    var unilateral by rememberSaveable(editorKey) { mutableStateOf(set.unilateral) }
    var validationRequested by rememberSaveable(editorKey) { mutableStateOf(false) }
    val displayRpe = showRpe
    val displayRir = showRir && !showRpe
    val largeText = LocalDensity.current.fontScale >= 1.5f
    fun fieldWidth(normal: androidx.compose.ui.unit.Dp): Modifier =
        if (largeText) Modifier.fillMaxWidth() else Modifier.width(normal)
    val needsWeight = policyExercise.trackingType in setOf(
        ExerciseTrackingType.WeightReps,
        ExerciseTrackingType.BodyweightReps,
        ExerciseTrackingType.AssistedBodyweightReps,
        ExerciseTrackingType.WeightOnly,
        ExerciseTrackingType.WeightDuration,
    )
    val needsReps = policyExercise.trackingType in setOf(
        ExerciseTrackingType.WeightReps,
        ExerciseTrackingType.BodyweightReps,
        ExerciseTrackingType.AssistedBodyweightReps,
        ExerciseTrackingType.RepsOnly,
        ExerciseTrackingType.RepsDuration,
    )
    val needsDistance = policyExercise.trackingType in setOf(ExerciseTrackingType.DistanceDuration, ExerciseTrackingType.DistanceOnly)
    val needsDuration = policyExercise.trackingType in setOf(
        ExerciseTrackingType.DistanceDuration,
        ExerciseTrackingType.WeightDuration,
        ExerciseTrackingType.RepsDuration,
        ExerciseTrackingType.DurationOnly,
    )
    val draft = WorkoutSetDraft(
        weight = weight.toWhipDoubleOrNull().takeUnless { machineType == MachineLoadType.Level },
        weightUnitId = weightUnitId,
        reps = reps.toIntOrNull(),
        distance = distance.toWhipDoubleOrNull(),
        distanceUnitId = distanceUnitId,
        durationSeconds = duration.toLongOrNull(),
        bodyweightKg = bodyweight.toWhipDoubleOrNull()?.let { massToKilograms(it, weightUnitId) },
        planned = false,
        completed = true,
        classification = set.classification,
        note = set.note,
        rpe = rpe.toWhipDoubleOrNull(),
        rir = rir.toWhipDoubleOrNull(),
        tempo = set.tempo,
        restSeconds = set.restSeconds,
        machineLoadValue = when (machineType) {
            MachineLoadType.Level -> machineSetting.toWhipDoubleOrNull()
            MachineLoadType.Mass -> weight.toWhipDoubleOrNull()
            null -> null
        },
        unilateral = unilateral,
    )
    val domainError = runCatching {
        validateWorkoutSetDraft(draft, policyExercise.trackingType, machineType, workoutExercise.loadInterpretationSnapshot)
    }.exceptionOrNull()?.message
    val loadLabel = when {
        machineType == MachineLoadType.Level -> workoutExercise.machineLevelLabelSnapshot.ifBlank { "Setting" }
        workoutExercise.loadInterpretationSnapshot == LoadInterpretation.PerHand -> "Per hand (${unitSymbol(weightUnitId)})"
        workoutExercise.loadInterpretationSnapshot == LoadInterpretation.PerSide -> "Per side (${unitSymbol(weightUnitId)})"
        workoutExercise.loadInterpretationSnapshot == LoadInterpretation.AddedLoad -> "Added (${unitSymbol(weightUnitId)})"
        else -> "Weight (${unitSymbol(weightUnitId)})"
    }
    val requiresLoad = policyExercise.trackingType in setOf(
        ExerciseTrackingType.WeightReps,
        ExerciseTrackingType.WeightOnly,
        ExerciseTrackingType.WeightDuration,
    ) || (policyExercise.trackingType == ExerciseTrackingType.AssistedBodyweightReps && machineType != null)
    val loadValue = if (machineType == MachineLoadType.Level) machineSetting.toWhipDoubleOrNull() else weight.toWhipDoubleOrNull()
    val loadError = "Required".takeIf {
        requiresLoad && (loadValue == null || (loadValue <= 0.0 && workoutExercise.loadInterpretationSnapshot != LoadInterpretation.AddedLoad))
    }
    val repsError = "Enter at least 1".takeIf { needsReps && (reps.toIntOrNull() ?: 0) <= 0 }
    val distanceError = "Enter a value above 0".takeIf { needsDistance && (distance.toWhipDoubleOrNull() ?: 0.0) <= 0.0 }
    val durationError = "Enter at least 1 second".takeIf { needsDuration && (duration.toLongOrNull() ?: 0L) <= 0L }
    val bodyweightError = "Enter a value above 0".takeIf {
        bodyweight.isNotBlank() && (bodyweight.toWhipDoubleOrNull() ?: 0.0) <= 0.0
    }
    val rpeError = "RPE must be 1–10".takeIf { rpe.isNotBlank() && rpe.toWhipDoubleOrNull()?.let { it !in 1.0..10.0 } != false }
    val rirError = "RIR must be 0–10".takeIf { rir.isNotBlank() && rir.toWhipDoubleOrNull()?.let { it !in 0.0..10.0 } != false }
    val fieldErrors = listOfNotNull(loadError, repsError, distanceError, durationError, bodyweightError, rpeError, rirError)
    val validationMessages = fieldErrors.ifEmpty { listOfNotNull(domainError) }.distinct()
    fun applySuggestion(suggestion: WorkoutSet) {
        weight = suggestion.enteredWeight?.let(::editableNumber)
            ?: suggestion.canonicalWeightKg?.let { editableNumber(massFromKilograms(it, weightUnitId)) }
            ?: weight
        machineSetting = suggestion.machineLoadValue?.let(::editableNumber) ?: machineSetting
        reps = suggestion.repetitions?.toString() ?: reps
        distance = suggestion.enteredDistance?.let(::editableNumber)
            ?: suggestion.canonicalDistanceMetres?.let { editableNumber(distanceFromMetres(it, distanceUnitId)) }
            ?: distance
        duration = suggestion.durationSeconds?.toString() ?: duration
        bodyweight = suggestion.bodyweightKg?.let { editableNumber(massFromKilograms(it, weightUnitId)) } ?: bodyweight
        unilateral = suggestion.unilateral
        validationRequested = false
    }
    fun submit(addNext: Boolean) {
        validationRequested = true
        if (validationMessages.isEmpty()) onSave(draft, addNext)
    }
    Column(
        modifier = Modifier.fillMaxWidth().testTag("quick-set-${set.id}"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        suggestedSet?.takeIf { suggestion ->
            suggestion.shortLabel(
                preferredWeightUnitId,
                preferredDistanceUnitId,
                1,
                workoutExercise,
                exercise.weightUnitId,
            ) != "Empty set"
        }?.let { suggestion ->
            WhipTonalButton(
                onClick = { applySuggestion(suggestion) },
                modifier = Modifier.fillMaxWidth().testTag("quick-set-use-last-${set.id}"),
            ) {
                Text(
                    "Use Previous · ${suggestion.shortLabel(preferredWeightUnitId, preferredDistanceUnitId, 1, workoutExercise, exercise.weightUnitId)}",
                    maxLines = 1,
                )
            }
        }
        if (needsWeight) {
            SteppedNumberField(
                value = if (machineType == MachineLoadType.Level) machineSetting else weight,
                onValueChange = { value ->
                    if (machineType == MachineLoadType.Level) machineSetting = value else {
                        weight = value
                        if (machineType == MachineLoadType.Mass) machineSetting = value
                    }
                },
                label = loadLabel,
                increment = exercise.weightIncrement,
                allowedValues = machine?.availableLoads.orEmpty(),
                actionSubject = "${exercise.name} $loadLabel",
                isError = validationRequested && loadError != null,
                supportingText = loadError.takeIf { validationRequested },
                modifier = Modifier.testTag("quick-set-load-${set.id}"),
            )
        }
        if (needsReps) {
            SteppedNumberField(
                value = reps,
                onValueChange = { reps = it },
                label = "Repetitions",
                increment = exercise.repetitionIncrement.toDouble(),
                integer = true,
                actionSubject = "${exercise.name} repetitions",
                isError = validationRequested && repsError != null,
                supportingText = repsError.takeIf { validationRequested },
                modifier = Modifier.testTag("quick-set-reps-${set.id}"),
            )
        }
        if (needsDistance || needsDuration) {
            Text(
                "Set Values",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (needsDistance) OutlinedTextField(
                distance,
                { distance = it },
                label = { Text("Distance (${unitSymbol(preferredDistanceUnitId)})") },
                isError = validationRequested && distanceError != null,
                supportingText = distanceError.takeIf { validationRequested }?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = fieldWidth(170.dp),
            )
            if (needsDuration) OutlinedTextField(
                duration,
                { duration = it },
                label = { Text("Seconds") },
                isError = validationRequested && durationError != null,
                supportingText = durationError.takeIf { validationRequested }?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = fieldWidth(130.dp),
            )
            if (exercise.trackingType in setOf(ExerciseTrackingType.BodyweightReps, ExerciseTrackingType.AssistedBodyweightReps)) {
                OutlinedTextField(
                    bodyweight,
                    { bodyweight = it },
                    label = { Text("Bodyweight") },
                    isError = validationRequested && bodyweightError != null,
                    supportingText = bodyweightError.takeIf { validationRequested }?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = fieldWidth(150.dp),
                )
            }
            if (workoutExercise.loadInterpretationSnapshot in setOf(LoadInterpretation.PerHand, LoadInterpretation.PerSide) ||
                workoutExercise.machineStackModeSnapshot == MachineStackMode.DualIndependent) {
                WhipFilterChip(selected = unilateral, onClick = { unilateral = !unilateral }, label = { Text("One Side / Limb") })
            }
        }
        WhipButton(
            onClick = { submit(false) },
            modifier = Modifier.fillMaxWidth().testTag("quick-set-save-next-${set.id}"),
        ) { Text("Complete Set") }
        if (displayRpe || displayRir) {
            Text(
                "Effort (Optional) · ${if (displayRpe) "RPE" else "RIR"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SteppedNumberField(
                value = if (displayRpe) rpe else rir,
                onValueChange = { value -> if (displayRpe) rpe = value else rir = value },
                label = if (displayRpe) "RPE (1–10)" else "RIR (0–10)",
                increment = 0.5,
                actionSubject = if (displayRpe) "RPE" else "RIR",
                isError = validationRequested && if (displayRpe) rpeError != null else rirError != null,
                supportingText = (if (displayRpe) rpeError else rirError).takeIf { validationRequested },
            )
        }
        if (validationRequested) {
            listOfNotNull(rpeError, rirError).distinct().forEach { message ->
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (fieldErrors.isEmpty()) domainError?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
        machine?.availableLoads?.takeIf { it.isNotEmpty() && it.size <= 12 }?.let { values ->
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                values.forEach { value ->
                    WhipFilterChip(
                        selected = (if (machineType == MachineLoadType.Level) machineSetting else weight).toWhipDoubleOrNull() == value,
                        onClick = {
                            val text = editableNumber(value)
                            machineSetting = text
                            if (machineType == MachineLoadType.Mass) weight = text
                        },
                        label = { Text(editableNumber(value)) },
                    )
                }
            }
        }
        WhipTextButton(onClick = onMoreDetails, modifier = Modifier.align(Alignment.End)) { Text("Set Details") }
    }
}

@Composable
internal fun RestTimerCard(
    session: WorkoutSession,
    remaining: Int?,
    selectedSeconds: Int,
    presetSeconds: List<Int>,
    notificationPermissionRequested: Boolean,
    onSelectedSecondsChange: (Int) -> Unit,
    onPresetSecondsChange: (List<Int>) -> Unit,
    onStart: (Long, Int) -> Unit,
    onAdjust: (Long, Int) -> Unit,
    onStop: (Long) -> Unit,
) {
    var showDurationEditor by rememberSaveable(session.id) { mutableStateOf(false) }
    val context = LocalContext.current
    val notificationAvailable = NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Rest · ${formatDuration((remaining ?: selectedSeconds).coerceAtLeast(1).toLong())}",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (remaining == null) {
                WhipTextButton(
                    onClick = { showDurationEditor = true },
                    modifier = Modifier.semantics { contentDescription = "Adjust rest time for this workout" },
                ) {
                    Text("Adjust")
                }
                WhipTextButton(onClick = { onStart(session.id, selectedSeconds.coerceAtLeast(1)) }) {
                    Text("Start")
                }
            } else {
                WhipTextButton(onClick = { onAdjust(session.id, -15) }) { Text("−15") }
                WhipTextButton(onClick = { onAdjust(session.id, 15) }) { Text("+15") }
                WhipTextButton(onClick = { onStop(session.id) }) { Text("Stop") }
            }
        }
        if (remaining != null && notificationPermissionRequested && !notificationAvailable) {
            Text(
                "Background alert off · the in-app timer still works",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        }
    }
    if (showDurationEditor) {
        RestDurationDialog(
            initialSeconds = selectedSeconds,
            presetSeconds = presetSeconds,
            onDismiss = { showDurationEditor = false },
            onPresetSecondsChange = onPresetSecondsChange,
            onConfirm = { seconds ->
                onSelectedSecondsChange(seconds)
                showDurationEditor = false
            },
        )
    }
}

@Composable
private fun RestDurationDialog(
    initialSeconds: Int,
    presetSeconds: List<Int>,
    onDismiss: () -> Unit,
    onPresetSecondsChange: (List<Int>) -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var secondsText by rememberSaveable(initialSeconds) { mutableStateOf(initialSeconds.coerceAtLeast(15).toString()) }
    var editingPresets by rememberSaveable { mutableStateOf(false) }
    var presetDraft by remember(presetSeconds) { mutableStateOf(normalizeRestTimerPresets(presetSeconds)) }
    var newPresetText by rememberSaveable { mutableStateOf("") }
    val seconds = secondsText.toIntOrNull()
    val valid = seconds != null && seconds in 15..3_600
    val newPreset = newPresetText.toIntOrNull()
    val newPresetValid = newPreset != null && newPreset in 15..3_600 && newPreset !in presetDraft && presetDraft.size < 12
    PaneAwareAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingPresets) "Manage Rest Presets" else "Rest Time for This Workout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (editingPresets) {
                    Text(
                        "Choose the shortcuts shown in the workout rest-time editor. Keep at least one preset; up to 12 are supported.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        presetDraft.forEach { preset ->
                            WhipOutlinedButton(
                                enabled = presetDraft.size > 1,
                                onClick = { presetDraft = presetDraft - preset },
                                modifier = Modifier.semantics {
                                    contentDescription = "Remove ${formatDuration(preset.toLong())} rest preset"
                                },
                            ) {
                                Text("${formatDuration(preset.toLong())} ×")
                            }
                        }
                    }
                    SteppedNumberField(
                        value = newPresetText,
                        onValueChange = { newPresetText = it },
                        label = "New Preset (Seconds)",
                        increment = 15.0,
                        integer = true,
                        actionSubject = "new rest preset",
                        isError = newPresetText.isNotBlank() && newPreset?.let { it !in 15..3_600 } != false,
                        supportingText = "Enter 15–3,600 seconds".takeIf {
                            newPresetText.isNotBlank() && newPreset?.let { it !in 15..3_600 } != false
                        },
                        modifier = Modifier.testTag("rest-preset-seconds"),
                    )
                    WhipButton(
                        enabled = newPresetValid,
                        onClick = {
                            presetDraft = normalizeRestTimerPresets(presetDraft + requireNotNull(newPreset))
                            newPresetText = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Add Preset") }
                    if (newPresetText.isNotBlank() && newPreset != null && newPreset in presetDraft) {
                        Text("That preset already exists.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (presetDraft.size >= 12) {
                        Text("Remove a preset before adding another.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    WhipTextButton(
                        onClick = { presetDraft = DEFAULT_REST_TIMER_PRESET_SECONDS },
                        modifier = Modifier.align(Alignment.End),
                    ) { Text("Restore Defaults") }
                } else {
                    Text(
                        "This changes rest timers started during the current workout. It does not change your default in Settings.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Selected · ${seconds?.takeIf { it > 0 }?.let { formatDuration(it.toLong()) } ?: "Invalid"}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        presetDraft.forEach { preset ->
                            WhipFilterChip(
                                selected = seconds == preset,
                                onClick = { secondsText = preset.toString() },
                                label = { Text(formatDuration(preset.toLong())) },
                            )
                        }
                    }
                    WhipTextButton(onClick = { editingPresets = true }, modifier = Modifier.align(Alignment.End)) {
                        Text("Manage Presets")
                    }
                    SteppedNumberField(
                        value = secondsText,
                        onValueChange = { secondsText = it },
                        label = "Seconds",
                        increment = 15.0,
                        integer = true,
                        actionSubject = "workout rest time",
                        isError = secondsText.isNotBlank() && !valid,
                        supportingText = "Enter 15–3,600 seconds".takeIf { secondsText.isNotBlank() && !valid },
                    )
                }
            }
        },
        confirmButton = {
            if (editingPresets) {
                WhipTextButton(onClick = {
                    val normalized = normalizeRestTimerPresets(presetDraft)
                    presetDraft = normalized
                    onPresetSecondsChange(normalized)
                    editingPresets = false
                }) { Text("Save Presets") }
            } else {
                WhipTextButton(enabled = valid, onClick = { onConfirm(requireNotNull(seconds)) }) {
                    Text("Use for This Workout")
                }
            }
        },
        dismissButton = {
            WhipTextButton(onClick = {
                if (editingPresets) {
                    presetDraft = normalizeRestTimerPresets(presetSeconds)
                    editingPresets = false
                } else {
                    onDismiss()
                }
            }) { Text(if (editingPresets) "Back" else "Cancel") }
        },
    )
}

@Composable
private fun ExerciseLibraryContent(
    state: GymUiState,
    onCreate: () -> Unit,
    onOpen: (Exercise) -> Unit,
    onEdit: (Exercise) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var selectedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedTrackingType by rememberSaveable { mutableStateOf<ExerciseTrackingType?>(null) }
    var selectedEquipment by rememberSaveable { mutableStateOf<String?>(null) }
    var sort by rememberSaveable { mutableStateOf(ExerciseLibrarySort.Name) }
    val source = if (showArchived) state.archivedExercises else state.exercises
    val machineNamesByExercise = (state.machines + state.archivedMachines).groupBy(GymMachine::exerciseId)
        .mapValues { (_, machines) -> machines.joinToString(" ") { it.displayName } }
    val equipmentOptions = source.map(Exercise::equipment).filter(String::isNotBlank).distinct().sorted()
    val lastUsedAtByExercise = state.allWorkoutExercises.groupingBy(WorkoutExercise::exerciseId)
        .fold(0L) { latest, placement -> maxOf(latest, placement.createdAtMillis) }
    val visible = source.filter { exercise ->
        exerciseMatchesQuery(exercise, query, machineNamesByExercise[exercise.id].orEmpty()) &&
            (!favoritesOnly || exercise.favorite) &&
            (selectedTrackingType == null || exercise.trackingType == selectedTrackingType) &&
            (selectedEquipment == null || if (selectedEquipment == MACHINE_EQUIPMENT_FILTER) {
                machineNamesByExercise.containsKey(exercise.id)
            } else exercise.equipment.equals(selectedEquipment, ignoreCase = true)) &&
            (selectedCategoryId == null || state.categoryLinks.any { it.exerciseId == exercise.id && it.categoryId == selectedCategoryId })
    }.let { matches ->
        when (sort) {
            ExerciseLibrarySort.Name -> matches.sortedBy { it.name.lowercase() }
            ExerciseLibrarySort.RecentlyUsed -> matches.sortedWith(
                compareByDescending<Exercise> { lastUsedAtByExercise[it.id] ?: Long.MIN_VALUE }
                    .thenBy { it.name.lowercase() },
            )
            ExerciseLibrarySort.RecentlyAdded -> matches.sortedByDescending(Exercise::id)
            ExerciseLibrarySort.FavoritesFirst -> matches.sortedWith(compareByDescending<Exercise> { it.favorite }.thenBy { it.name.lowercase() })
        }
    }
    val activeFilterCount = listOf(favoritesOnly, showArchived, selectedCategoryId != null, selectedTrackingType != null, selectedEquipment != null).count { it }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 112.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            WhipPageHeader(
                title = "Exercise Library",
                supportingText = "Only your exercises appear here—Whip never seeds a movement list.",
            ) {
                WhipButton(onClick = onCreate) { Text("Create Exercise") }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search Exercises") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("exercise-library-search"),
            )
        }
        item {
            DisclosureRow(
                title = "Filters and Sort",
                supportingText = if (activeFilterCount == 0) sort.label else "$activeFilterCount active · ${sort.label}",
                expanded = filtersExpanded,
                onClick = { filtersExpanded = !filtersExpanded },
            )
        }
        if (filtersExpanded) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ToggleRow("Favorites only", favoritesOnly) { favoritesOnly = it }
                ToggleRow("Show archived", showArchived) { showArchived = it }
                GymEnumDropdown(
                    "Category",
                    listOf<Long?>(null) + state.categories.map(ExerciseCategory::id),
                    selectedCategoryId,
                    { id -> state.categories.firstOrNull { it.id == id }?.name ?: "All Categories" },
                    titleCaseValues = false,
                ) { selectedCategoryId = it }
                GymEnumDropdown(
                    "Tracking Type",
                    listOf<ExerciseTrackingType?>(null) + ExerciseTrackingType.entries,
                    selectedTrackingType,
                    { it?.label ?: "All Tracking Types" },
                    titleCaseValues = false,
                ) { selectedTrackingType = it }
                GymEnumDropdown(
                    "Equipment",
                    listOf<String?>(null, MACHINE_EQUIPMENT_FILTER) + equipmentOptions,
                    selectedEquipment,
                    { value -> when (value) {
                        null -> "All Equipment"
                        MACHINE_EQUIPMENT_FILTER -> "Any Machine"
                        else -> value
                    } },
                    titleCaseValues = false,
                ) { selectedEquipment = it }
                GymEnumDropdown("Sort", ExerciseLibrarySort.entries, sort, ExerciseLibrarySort::label) { sort = it }
            }
        }
        if (activeFilterCount > 0) item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (favoritesOnly) WhipFilterChip(true, { favoritesOnly = false }, { Text("Favorites ×") })
                if (showArchived) WhipFilterChip(true, { showArchived = false }, { Text("Archived ×") })
                selectedCategoryId?.let { id ->
                    WhipFilterChip(true, { selectedCategoryId = null }, { Text("${state.categories.firstOrNull { it.id == id }?.name ?: "Category"} ×") })
                }
                selectedTrackingType?.let { type -> WhipFilterChip(true, { selectedTrackingType = null }, { Text("${type.label} ×") }) }
                selectedEquipment?.let { equipment ->
                    WhipFilterChip(
                        true,
                        { selectedEquipment = null },
                        { Text("${if (equipment == MACHINE_EQUIPMENT_FILTER) "Any Machine" else equipment} ×") },
                    )
                }
            }
        }
        if (visible.isEmpty()) {
            item {
                WhipEmptyState(
                    title = if (source.isEmpty() && showArchived) "No Archived Exercises" else if (source.isEmpty()) "Exercise Library Is Empty" else "No Matching Exercises",
                    supportingText = if (source.isEmpty() && showArchived) "Archived exercises will appear here." else if (source.isEmpty()) "Create your first named exercise to use it in workouts and routines." else "Clear or change the search and filters.",
                    primaryActionLabel = "Create Exercise".takeUnless { showArchived || source.isNotEmpty() },
                    onPrimaryAction = onCreate.takeUnless { showArchived || source.isNotEmpty() },
                )
            }
        }
        items(visible, key = Exercise::id) { exercise ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = "Open ${exercise.name}") { onOpen(exercise) },
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            (if (exercise.favorite) "★ " else "") + exercise.name,
                            fontWeight = FontWeight.Bold,
                        )
                        val unitDetail = if (exercise.trackingType in setOf(
                                ExerciseTrackingType.WeightReps,
                                ExerciseTrackingType.BodyweightReps,
                                ExerciseTrackingType.AssistedBodyweightReps,
                                ExerciseTrackingType.WeightOnly,
                                ExerciseTrackingType.WeightDuration,
                            )
                        ) {
                            " · ${unitSymbol(exercise.weightUnitId)} · ±${editableNumber(exercise.weightIncrement)}"
                        } else {
                            ""
                        }
                        Text(exercise.trackingType.label.uiTitleCase() + unitDetail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    ItemEditButton("exercise", exercise.name, onEdit = { onEdit(exercise) })
                }
            }
        }
    }
}

@Composable
private fun MachineLibraryContent(
    state: GymUiState,
    onCreate: () -> Unit,
    onCreateExercise: () -> Unit,
    onEdit: (GymMachine) -> Unit,
    onArchive: (Long, Boolean) -> Unit,
    onNewVersion: (GymMachine) -> Unit,
    onDelete: (GymMachine) -> Unit,
) {
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var actionMenuId by rememberSaveable { mutableStateOf<Long?>(null) }
    val exerciseById = (state.exercises + state.archivedExercises).associateBy(Exercise::id)
    val visible = if (showArchived) state.archivedMachines else state.machines
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("gym-machine-list"),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 112.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            WhipPageHeader(
                title = "Machines",
                supportingText = "Give each physical machine its own profile. Whip keeps its history and records separate.",
            ) {
                WhipButton(onClick = if (state.exercises.isEmpty()) onCreateExercise else onCreate) {
                    Text(if (state.exercises.isEmpty()) "Create Exercise" else "Create Machine")
                }
            }
        }
        item { ToggleRow("Show archived", showArchived) { showArchived = it } }
        if (visible.isEmpty()) item {
            WhipEmptyState(
                title = if (showArchived) "No Archived Machines" else "No Machine Profiles",
                supportingText = if (state.exercises.isEmpty()) {
                    "Create an exercise first, then add the machine profile used for it."
                } else "Free weights continue to work without a machine profile.",
                primaryActionLabel = if (state.exercises.isEmpty()) "Create Exercise" else "Create Machine",
                onPrimaryAction = if (state.exercises.isEmpty()) onCreateExercise else onCreate,
            )
        }
        items(visible, key = GymMachine::id) { machine ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(machine.displayName, fontWeight = FontWeight.Bold)
                            Text(exerciseById[machine.exerciseId]?.name ?: "Archived exercise")
                        }
                        ItemEditButton("machine", machine.displayName, onEdit = { onEdit(machine) })
                        Box {
                            IconButton(
                                onClick = { actionMenuId = machine.id },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.MoreVert,
                                    contentDescription = "More options for ${machine.displayName}",
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            DropdownMenu(
                                expanded = actionMenuId == machine.id,
                                onDismissRequest = { actionMenuId = null },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (machine.archived) "Restore" else "Archive") },
                                    onClick = { actionMenuId = null; onArchive(machine.id, !machine.archived) },
                                )
                                if (!machine.archived) DropdownMenuItem(
                                    text = { Text("New Configuration Version") },
                                    onClick = { actionMenuId = null; onNewVersion(machine) },
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Permanently", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                                    onClick = { actionMenuId = null; onDelete(machine) },
                                )
                            }
                        }
                    }
                    val scale = when (machine.loadType) {
                        MachineLoadType.Mass -> "Mass stack · ${unitSymbol(machine.unitId)}"
                        MachineLoadType.Level -> "Numbered scale · ${machine.levelLabel}"
                    }
                    Text(
                        "v${machine.configurationVersion} · $scale${machine.availableLoads.takeIf(List<Double>::isNotEmpty)?.let { " · ${machineLoadSummary(it)}" }.orEmpty()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (machine.loadType == MachineLoadType.Mass) {
                        Text(
                            "Entry meaning: ${machine.loadInterpretation.label.uiTitleCase()}" +
                                if (machine.loadInterpretation == LoadInterpretation.PerSide && machine.baseLoadKg != null) {
                                    " · base ${editableNumber(massFromKilograms(machine.baseLoadKg, machine.unitId))} ${unitSymbol(machine.unitId)}"
                                } else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val setup = listOfNotNull(
                        machine.seatPosition.takeIf(String::isNotBlank)?.let { "Seat $it" },
                        machine.backPosition.takeIf(String::isNotBlank)?.let { "Back $it" },
                        machine.attachment.takeIf(String::isNotBlank),
                        machine.pulleyRatio.takeIf { it != 1.0 }?.let { "resistance ×${editableNumber(it)}" },
                        machine.stackMode.takeIf { it != MachineStackMode.Single }?.label,
                    )
                    if (setup.isNotEmpty()) Text(setup.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
internal fun MachinePermanentDeleteDialog(
    modifier: Modifier = Modifier,
    impact: MachineDeletionImpact,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onReviewRoutines: () -> Unit,
    onOpenActiveWorkout: () -> Unit,
    onBackUpFirst: () -> Unit,
    deleting: Boolean,
) {
    val blocked = impact.activePlacements > 0
    PaneAwareAlertDialog(
        modifier = modifier.testTag("machine-delete-dialog"),
        onDismissRequest = { if (!deleting) onDismiss() },
        title = { Text("Delete “${impact.displayName}” v${impact.configurationVersion} Permanently?") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text("Removed", fontWeight = FontWeight.Bold)
                    Text("The reusable profile, load presets, and current setup metadata. It cannot be restored.")
                }
                item {
                    Text("Kept", fontWeight = FontWeight.Bold)
                    Text(
                        "${impact.completedSessions} completed workout${if (impact.completedSessions == 1) "" else "s"} and " +
                            "${impact.setCount} set${if (impact.setCount == 1) "" else "s"} remain with their saved machine and configuration snapshots. " +
                            "They will not merge with free weights.",
                    )
                }
                if (impact.routineReferences > 0) item {
                    Text("Needs Attention", fontWeight = FontWeight.Bold)
                    Text(
                        "${impact.routineReferences} routine placement${if (impact.routineReferences == 1) "" else "s"} " +
                            "will be marked Needs equipment and cannot start until replaced." +
                            impact.routineNames.takeIf(List<String>::isNotEmpty)?.joinToString(
                                prefix = "\nAffected: ",
                                limit = 3,
                            ).orEmpty(),
                    )
                    WhipTextButton(onClick = onReviewRoutines) { Text("Review Routines") }
                }
                if (blocked) item {
                    Text("Active Workout", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text("This profile is currently in use. Finish the workout or change that exercise’s equipment before deleting it.")
                    WhipTextButton(onClick = onOpenActiveWorkout) { Text("Open Active Workout") }
                }
                item {
                    Text(
                        "Other configuration versions stay. Past workout snapshots and older backups may still contain the recorded machine name, location, and setup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    WhipTextButton(onClick = onBackUpFirst) { Text("Back Up First") }
                }
            }
        },
        confirmButton = {
            WhipTextButton(
                enabled = !blocked && !deleting,
                onClick = onConfirm,
                modifier = Modifier.testTag("machine-delete-confirm"),
            ) { Text(if (deleting) "Deleting…" else "Delete profile permanently", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { WhipTextButton(enabled = !deleting, onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun MachineEditorDialog(
    modifier: Modifier = Modifier,
    machine: GymMachine?,
    exercises: List<Exercise>,
    definitionLocked: Boolean,
    creatingVersion: Boolean = false,
    initialExerciseId: Long? = null,
    onCreateVersion: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (GymMachineDraft) -> Unit,
    saving: Boolean = false,
) {
    val editorKey = "machine-${machine?.id ?: initialExerciseId ?: "new"}-${if (creatingVersion) "version" else "edit"}"
    var exerciseId by rememberSaveable(editorKey) { mutableStateOf(machine?.exerciseId ?: initialExerciseId ?: exercises.firstOrNull()?.id) }
    var name by rememberSaveable(editorKey) { mutableStateOf(machine?.name.orEmpty()) }
    var location by rememberSaveable(editorKey) { mutableStateOf(machine?.location.orEmpty()) }
    var details by rememberSaveable(editorKey) { mutableStateOf(machine?.details.orEmpty()) }
    var loadType by rememberSaveable(editorKey) { mutableStateOf(machine?.loadType ?: MachineLoadType.Mass) }
    var unitId by rememberSaveable(editorKey) {
        mutableStateOf(
            machine?.unitId?.takeIf(String::isNotBlank)
                ?: exercises.firstOrNull { it.id == exerciseId }?.weightUnitId?.ifBlank { "kilogram" }
                ?: "kilogram",
        )
    }
    var mappingUnitId by rememberSaveable(editorKey) {
        mutableStateOf(
            machine?.unitId?.takeIf(String::isNotBlank)
                ?: exercises.firstOrNull { it.id == exerciseId }?.weightUnitId?.ifBlank { "kilogram" }
                ?: "kilogram",
        )
    }
    var pendingMachineUnit by rememberSaveable(editorKey) { mutableStateOf<String?>(null) }
    var levelLabel by rememberSaveable(editorKey) { mutableStateOf(machine?.levelLabel ?: "level") }
    var loadInterpretation by rememberSaveable(editorKey) {
        mutableStateOf(machine?.loadInterpretation ?: LoadInterpretation.Total)
    }
    var baseLoad by rememberSaveable(editorKey) {
        mutableStateOf(machine?.baseLoadKg?.let { editableNumber(massFromKilograms(it, machine.unitId.ifBlank { "kilogram" })) }.orEmpty())
    }
    var seatPosition by rememberSaveable(editorKey) { mutableStateOf(machine?.seatPosition.orEmpty()) }
    var backPosition by rememberSaveable(editorKey) { mutableStateOf(machine?.backPosition.orEmpty()) }
    var attachment by rememberSaveable(editorKey) { mutableStateOf(machine?.attachment.orEmpty()) }
    var pulleyRatio by rememberSaveable(editorKey) { mutableStateOf(editableNumber(machine?.pulleyRatio ?: 1.0)) }
    var stackMode by rememberSaveable(editorKey) { mutableStateOf(machine?.stackMode ?: MachineStackMode.Single) }
    var addOnPlate by rememberSaveable(editorKey) {
        mutableStateOf(machine?.addOnPlateKg?.let { editableNumber(massFromKilograms(it, machine.unitId.takeIf(String::isNotBlank) ?: mappingUnitId)) }.orEmpty())
    }
    var stackLabels by rememberSaveable(editorKey) { mutableStateOf(machine?.stackLabels?.joinToString(", ").orEmpty()) }
    var massMapping by rememberSaveable(editorKey) {
        mutableStateOf(machine?.massMappingKg?.entries?.sortedBy { it.key }?.joinToString("\n") { "${editableNumber(it.key)}=${editableNumber(massFromKilograms(it.value, mappingUnitId))}" }.orEmpty())
    }
    var compatibleForComparison by rememberSaveable(editorKey) { mutableStateOf(machine?.compatibleForComparison ?: false) }
    var showAdvancedSetup by rememberSaveable(editorKey) { mutableStateOf(machine != null) }
    val initialSequence = remember(machine?.id) {
        machine?.availableLoads?.takeIf(List<Double>::isNotEmpty)?.let(::compactNumericSequence)
    }
    val defaultSequence = standardMachineSequence(loadType, unitId)
    var loads by rememberSaveable(editorKey) {
        mutableStateOf(initialSequence?.specification ?: defaultSequence.specification)
    }
    var loadIncrement by rememberSaveable(editorKey) {
        mutableStateOf(editableNumber(initialSequence?.increment ?: defaultSequence.increment))
    }
    val parsedLoads = parseNumericSequence(loads, loadIncrement.toWhipDoubleOrNull())
    val largeText = LocalDensity.current.fontScale >= 1.5f
    fun setupFieldWidth(normal: androidx.compose.ui.unit.Dp): Modifier =
        if (largeText) Modifier.fillMaxWidth() else Modifier.width(normal)
    fun applyStandardSequence(type: MachineLoadType = loadType, unit: String = unitId) {
        val standard = standardMachineSequence(type, unit)
        loads = standard.specification
        loadIncrement = editableNumber(standard.increment)
    }
    fun changeLoadType(selected: MachineLoadType) {
        if (selected == loadType) return
        loadType = selected
        if (selected == MachineLoadType.Level) {
            loadInterpretation = LoadInterpretation.Total
            baseLoad = ""
        }
        applyStandardSequence(type = selected)
    }
    fun changeMachineUnit(selected: String) {
        if (selected == unitId) return
        pendingMachineUnit = selected
    }
    fun convertMachineDefaults(selected: String) {
        val source = unitId
        val convertedValues = parsedLoads.values.map { convertPracticalMassValue(it, source, selected) }
        val compact = compactNumericSequence(convertedValues)
        loads = compact.specification
        loadIncrement = (compact.increment
            ?: loadIncrement.toWhipDoubleOrNull()?.let { convertPracticalMassValue(it, source, selected) })
            ?.let(::editableNumber).orEmpty()
        baseLoad = baseLoad.toWhipDoubleOrNull()
            ?.let { convertPracticalMassValue(it, source, selected) }
            ?.let(::editableNumber).orEmpty()
        addOnPlate = addOnPlate.toWhipDoubleOrNull()
            ?.let { convertPracticalMassValue(it, source, selected) }
            ?.let(::editableNumber).orEmpty()
        unitId = selected
    }
    fun resetMachineDefaults(selected: String) {
        unitId = selected
        applyStandardSequence(unit = selected)
        baseLoad = ""
        addOnPlate = ""
    }
    fun changeMappingUnit(selected: String) {
        if (selected == mappingUnitId) return
        val parsed = parseMachineMassMapping(massMapping).orEmpty()
        massMapping = parsed.entries.joinToString("\n") { (setting, value) ->
            "${editableNumber(setting)}=${editableNumber(convertPracticalMassValue(value, mappingUnitId, selected))}"
        }
        addOnPlate = addOnPlate.toWhipDoubleOrNull()
            ?.let { convertPracticalMassValue(it, mappingUnitId, selected) }
            ?.let(::editableNumber).orEmpty()
        mappingUnitId = selected
    }
    val editorFingerprint = listOf(
        exerciseId, name, location, details, loadType, unitId, mappingUnitId, levelLabel,
        loadInterpretation, baseLoad, seatPosition, backPosition, attachment, pulleyRatio,
        stackMode, addOnPlate, stackLabels, massMapping, compatibleForComparison, loads, loadIncrement,
    ).joinToString("\u001f")
    val initialFingerprint by rememberSaveable(editorKey) { mutableStateOf(editorFingerprint) }
    var showDiscardConfirmation by rememberSaveable(editorKey) { mutableStateOf(false) }
    val requestDismiss = { if (editorFingerprint != initialFingerprint) showDiscardConfirmation = true else onDismiss() }
    BackHandler(enabled = !showDiscardConfirmation, onBack = requestDismiss)
    ProductivityEditorDialog(
        modifier = modifier,
        testTag = "exercise-editor-surface",
        onDismissRequest = { if (!saving) requestDismiss() },
        title = { Text(if (creatingVersion) "New Machine Configuration Version" else if (machine == null) "Create Machine Profile" else "Edit Machine Profile") },
        text = {
            LazyColumn(
                modifier = Modifier.testTag("machine-editor-list"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    val choices = listOf<Exercise?>(null) + exercises
                    SelectionField(
                        label = "Exercise",
                        values = choices,
                        selected = exercises.firstOrNull { it.id == exerciseId },
                        valueText = { it?.name ?: "Choose Exercise" },
                        enabled = !definitionLocked,
                        onSelect = { exercise ->
                            exerciseId = exercise?.id
                            if (exercise != null && machine == null && loadType == MachineLoadType.Mass) {
                                unitId = exercise.weightUnitId.ifBlank { "kilogram" }
                                applyStandardSequence(unit = unitId)
                            }
                        }
                    )
                }
                if (definitionLocked) item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AvailabilityNotice(
                            label = "Exercise and resistance definition",
                            availability = ControlAvailability(
                                enabled = false,
                                unavailableExplanation = "Locked to preserve workout history. Names, notes, setup cues, and presets remain editable.",
                            ),
                        )
                        onCreateVersion?.let { createVersion ->
                            WhipOutlinedButton(onClick = createVersion, modifier = Modifier.fillMaxWidth()) {
                                Text("Create New Configuration Version")
                            }
                        }
                    }
                }
                item { OutlinedTextField(name, { name = it }, label = { Text("Machine name *") }, supportingText = { Text("Example: Home multi-gym or Downtown cable stack") }, modifier = Modifier.fillMaxWidth().testTag("machine-editor-name")) }
                item { OutlinedTextField(location, { location = it }, label = { Text("Location") }, supportingText = { Text("Example: Home or Downtown Gym") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(details, { details = it }, label = { Text("Model / setup notes") }, supportingText = { Text("Seat, attachment, pulley, or other setup that changes resistance") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    DisclosureButton(
                        label = "Advanced Machine Setup",
                        expanded = showAdvancedSetup,
                        onClick = { showAdvancedSetup = !showAdvancedSetup },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (showAdvancedSetup) item {
                    Text("Repeatable Setup", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(seatPosition, { seatPosition = it }, label = { Text("Seat") }, singleLine = true, modifier = setupFieldWidth(130.dp))
                        OutlinedTextField(backPosition, { backPosition = it }, label = { Text("Back") }, singleLine = true, modifier = setupFieldWidth(130.dp))
                        OutlinedTextField(attachment, { attachment = it }, label = { Text("Attachment") }, singleLine = true, modifier = setupFieldWidth(180.dp))
                    }
                    Text("Use the machine's own labels (for example seat 4, back B, rope). These values are snapshotted into each workout.", style = MaterialTheme.typography.bodySmall)
                }
                if (showAdvancedSetup) item {
                    GymEnumDropdown("Stack / arm arrangement", MachineStackMode.entries, stackMode, MachineStackMode::label) { stackMode = it }
                    NumberField(pulleyRatio, { pulleyRatio = it }, "Effective resistance multiplier")
                    Text("Use 1 for direct resistance, 0.5 when a 2:1 pulley halves the displayed resistance, or the manufacturer-tested multiplier.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(stackLabels, { stackLabels = it }, label = { Text("Stack / arm labels") }, supportingText = { Text("Comma-separated, e.g. left, right") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    Text("Resistance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("How This Machine Labels Resistance", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        MachineLoadType.entries.forEach { type ->
                            WhipFilterChip(selected = loadType == type, enabled = !definitionLocked, onClick = { changeLoadType(type) }, label = { Text(type.label.uiTitleCase()) })
                        }
                    }
                    DependentSettingsNotice(
                        message = if (loadType == MachineLoadType.Mass) {
                            "Mass units, entry meaning, and optional base resistance appear next."
                        } else {
                            "The setting label and optional mass mapping appear next."
                        },
                        testTag = "machine-load-type-consequence",
                    )
                }
                if (loadType == MachineLoadType.Mass) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WhipFilterChip(selected = unitId == "kilogram", enabled = !definitionLocked, onClick = { changeMachineUnit("kilogram") }, label = { Text("kg") })
                            WhipFilterChip(selected = unitId == "pound", enabled = !definitionLocked, onClick = { changeMachineUnit("pound") }, label = { Text("lb") })
                        }
                        Text("Use this only when the stack is labeled as a mass. Comparisons still stay inside this machine profile.", style = MaterialTheme.typography.bodySmall)
                    }
                    item {
                        if (definitionLocked) {
                            Text("Entry meaning: ${loadInterpretation.label.uiTitleCase()}", style = MaterialTheme.typography.labelMedium)
                        } else {
                            GymEnumDropdown(
                                "What one entered load means",
                                LoadInterpretation.entries,
                                loadInterpretation,
                                LoadInterpretation::label,
                            ) { loadInterpretation = it }
                        }
                        Text(
                            when (loadInterpretation) {
                                LoadInterpretation.Total -> "The stack label is the full resistance used for comparisons and volume."
                                LoadInterpretation.PerHand -> "The entered stack value is used by each hand; Whip stores twice that value as total resistance."
                                LoadInterpretation.PerSide -> "Enter the plates or stack value on one side; Whip adds both sides and the optional base resistance."
                                LoadInterpretation.AddedLoad -> "The value is external load added to a bodyweight movement; negative values can represent assistance."
                                LoadInterpretation.BodyweightPlusExternal -> "Effective load is bodyweight plus the entered external resistance."
                                LoadInterpretation.BodyweightPercentage -> "Effective load uses the exercise bodyweight percentage plus external resistance."
                                LoadInterpretation.AssistedSubtraction -> "The entered resistance is subtracted from effective bodyweight as assistance."
                                LoadInterpretation.MachineDisplayedMass -> "The machine's displayed mass is converted with its stack and pulley configuration."
                                LoadInterpretation.OrdinalSetting -> "The setting stays ordinal unless this profile maps each setting to a mass."
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (loadInterpretation == LoadInterpretation.PerSide) {
                            if (definitionLocked) {
                                Text("Base resistance: ${baseLoad.ifBlank { "0" }} ${unitSymbol(unitId)}")
                            } else {
                                NumberField(baseLoad, { baseLoad = it }, "Base resistance (${unitSymbol(unitId)})")
                            }
                        }
                    }
                } else {
                    item {
                        OutlinedTextField(levelLabel, { levelLabel = it }, enabled = !definitionLocked, label = { Text("Setting label") }, supportingText = { Text("Examples: level, pin, plate, resistance") }, modifier = Modifier.fillMaxWidth())
                        Text("Numbered settings stay ordinal and are excluded from mass analytics unless you explicitly map each setting to a mass.", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WhipFilterChip(selected = mappingUnitId == "kilogram", onClick = { changeMappingUnit("kilogram") }, label = { Text("Mapping in kg") })
                            WhipFilterChip(selected = mappingUnitId == "pound", onClick = { changeMappingUnit("pound") }, label = { Text("Mapping in lb") })
                        }
                        OutlinedTextField(
                            massMapping,
                            { massMapping = it },
                            label = { Text("Optional setting-to-${unitSymbol(mappingUnitId)} mapping") },
                            supportingText = { Text("One per line, e.g. 1=10, 2=15. Leave blank to keep the setting ordinal and exclude it from mass analytics.") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (showAdvancedSetup) item {
                    NumberField(addOnPlate, { addOnPlate = it }, "Add-on resistance (${unitSymbol(if (loadType == MachineLoadType.Mass) unitId else mappingUnitId)})")
                    ToggleRow("Allow comparison with selected compatible versions", compatibleForComparison) { compatibleForComparison = it }
                }
                if (creatingVersion) item {
                    Text("The old configuration remains immutable in history. This becomes version ${(machine?.configurationVersion ?: 0) + 1} of the same physical machine family.", style = MaterialTheme.typography.bodySmall)
                }
                item {
                    OutlinedTextField(
                        loads,
                        { loads = it },
                        label = { Text(if (loadType == MachineLoadType.Mass) "Load range or values (${unitSymbol(unitId)})" else "Setting range or values") },
                        supportingText = {
                            Text(
                                parsedLoads.error ?: if (loadType == MachineLoadType.Mass) {
                                    "Range: 50-500 with an increment, or custom values: 50,70,90"
                                } else {
                                    "Range: 1-10 with an increment, or custom values: 1,2,4,7"
                                },
                            )
                        },
                        isError = parsedLoads.error != null,
                        modifier = Modifier.fillMaxWidth().testTag("machine-load-spec"),
                    )
                }
                item {
                    NumberField(
                        loadIncrement,
                        { loadIncrement = it },
                        if (loadType == MachineLoadType.Mass) "Range increment (${unitSymbol(unitId)})" else "Range increment",
                        modifier = Modifier.testTag("machine-load-increment"),
                    )
                    Text("The increment expands range shorthand and drives −/+ during workouts. Custom lists keep their exact values.", style = MaterialTheme.typography.bodySmall)
                }
                if (parsedLoads.error == null && parsedLoads.values.isNotEmpty()) item {
                    Text(
                        "Preview · ${parsedLoads.values.size} values · ${machineLoadSummary(parsedLoads.values)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("machine-load-preview"),
                    )
                }
            }
        },
        confirmButton = {
            WhipTextButton(
                enabled = !saving && name.isNotBlank() && exerciseId != null && parsedLoads.error == null &&
                    (loadType == MachineLoadType.Mass || levelLabel.isNotBlank()) &&
                    (pulleyRatio.toWhipDoubleOrNull()?.let { it > 0.0 && it <= 10.0 } == true) &&
                    parseMachineMassMapping(massMapping) != null,
                onClick = {
                    onSave(
                        GymMachineDraft(
                            exerciseId = requireNotNull(exerciseId), name = name, location = location,
                            details = details, loadType = loadType, unitId = unitId,
                            levelLabel = levelLabel, availableLoads = parsedLoads.values,
                            loadInterpretation = loadInterpretation,
                            baseLoadKg = baseLoad.toWhipDoubleOrNull()?.let { massToKilograms(it, unitId) },
                            configurationGroupId = machine?.configurationGroupId.orEmpty(),
                            configurationVersion = machine?.configurationVersion ?: 1,
                            seatPosition = seatPosition,
                            backPosition = backPosition,
                            attachment = attachment,
                            pulleyRatio = requireNotNull(pulleyRatio.toWhipDoubleOrNull()),
                            stackMode = stackMode,
                            addOnPlateKg = addOnPlate.toWhipDoubleOrNull()?.let { massToKilograms(it, if (loadType == MachineLoadType.Mass) unitId else mappingUnitId) },
                            stackLabels = stackLabels.split(',').map(String::trim).filter(String::isNotBlank),
                            massMappingKg = requireNotNull(parseMachineMassMapping(massMapping)).mapValues { (_, value) -> massToKilograms(value, mappingUnitId) },
                            compatibleForComparison = compatibleForComparison,
                        ),
                    )
                },
            ) { Text(if (saving) "Saving…" else "Save") }
        },
        dismissButton = { WhipTextButton(onClick = requestDismiss, enabled = !saving) { Text("Cancel") } },
    )
    pendingMachineUnit?.let { selected ->
        val currentSummary = machineLoadSummary(parsedLoads.values)
        val convertedSummary = machineLoadSummary(parsedLoads.values.map { convertPracticalMassValue(it, unitId, selected) })
        DefaultsMeaningChangeDialog(
            modifier = modifier,
            title = "Change Machine Labels to ${unitSymbol(selected)}?",
            explanation = "Choose what should happen to this profile's future-entry values. Convert changes $currentSummary ${unitSymbol(unitId)} to $convertedSummary ${unitSymbol(selected)} and converts base/add-on resistance. Keep changes only the label. Reset uses Whip's standard ${unitSymbol(selected)} stack and clears base/add-on resistance. Logged workouts keep their snapshots.",
            onConvert = { convertMachineDefaults(selected); pendingMachineUnit = null },
            onKeep = { unitId = selected; pendingMachineUnit = null },
            onReset = { resetMachineDefaults(selected); pendingMachineUnit = null },
            onCancel = { pendingMachineUnit = null },
        )
    }
    if (showDiscardConfirmation) {
        UnsavedChangesDialog("machine profile", { showDiscardConfirmation = false }, onDismiss, modifier)
    }
}

@Composable
private fun MachineChoiceDialog(
    modifier: Modifier = Modifier,
    exercise: Exercise,
    machines: List<GymMachine>,
    onDismiss: () -> Unit,
    onChoose: (Long?) -> Unit,
) {
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text("Equipment for ${exercise.name}") },
        text = {
            LazyColumn(
                modifier = Modifier.testTag("machine-choice-list"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { Text("Choose the exact machine so previous sets and progress remain comparable.") }
                item { WhipOutlinedButton(onClick = { onChoose(null) }, modifier = Modifier.fillMaxWidth()) { Text("No Machine / Free Weights") } }
                items(machines, key = GymMachine::id) { machine ->
                    WhipOutlinedButton(onClick = { onChoose(machine.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text(machine.displayName)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ExerciseCategoryContent(
    state: GymUiState,
    viewModel: GymViewModel,
    dialogModifier: Modifier = Modifier,
) {
    var editingCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    val editing = editingCategoryId?.let { id -> (state.categories + state.archivedCategories).firstOrNull { it.id == id } }
    var creating by rememberSaveable { mutableStateOf(false) }
    val editorKey = "category-${editingCategoryId ?: "new"}"
    var name by rememberSaveable(editorKey) { mutableStateOf(editing?.name.orEmpty()) }
    var kind by rememberSaveable(editorKey) { mutableStateOf(editing?.kind ?: "Category") }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    val visible = if (showArchived) state.archivedCategories else state.categories
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 112.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            WhipPageHeader(
                title = "Exercise Categories",
                supportingText = "Create your own muscle, equipment, movement, or training tags.",
            ) {
                if (visible.isNotEmpty() && !showArchived) {
                    WhipButton(onClick = { creating = true }) { Text("Create Category") }
                }
            }
        }
        item { ToggleRow("Show archived", showArchived) { showArchived = it } }
        if (visible.isEmpty()) item {
            WhipEmptyState(
                title = if (showArchived) "No Archived Categories" else "No Categories Yet",
                supportingText = if (showArchived) "Archived categories will appear here." else "Categories are optional and can make a large exercise library easier to browse.",
                primaryActionLabel = "Create Category".takeUnless { showArchived },
                onPrimaryAction = { creating = true }.takeUnless { showArchived },
            )
        }
        items(visible.size, key = { visible[it].id }) { index ->
            val category = visible[index]
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(category.name, fontWeight = FontWeight.Bold); Text(category.kind, style = MaterialTheme.typography.labelSmall) }
                    if (!showArchived) {
                        WhipTextButton(enabled = index > 0, onClick = {
                            val ids = visible.map(ExerciseCategory::id).toMutableList(); java.util.Collections.swap(ids, index, index - 1); viewModel.reorderCategories(ids)
                        }) { Icon(Icons.Outlined.ArrowUpward, contentDescription = "Move category up", modifier = Modifier.size(24.dp)) }
                    }
                    ItemEditButton("category", category.name, onEdit = { editingCategoryId = category.id })
                    WhipTextButton(onClick = { viewModel.setCategoryArchived(category.id, !category.archived) }) { Text(if (category.archived) "Restore" else "Archive") }
                }
            }
        }
    }
    if (creating || editing != null) {
        PaneAwareAlertDialog(
            modifier = dialogModifier,
            onDismissRequest = { creating = false; editingCategoryId = null },
            title = { Text(if (editing == null) "Create Category" else "Edit Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") })
                    OutlinedTextField(kind, { kind = it }, label = { Text("Type, e.g. Muscle or Equipment") })
                }
            },
            confirmButton = { WhipTextButton(enabled = name.isNotBlank(), onClick = { viewModel.saveCategory(editing?.id, name, kind); creating = false; editingCategoryId = null }) { Text("Save") } },
            dismissButton = { WhipTextButton(onClick = { creating = false; editingCategoryId = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun WorkoutHistoryContent(
    history: List<WorkoutSession>,
    state: GymUiState,
    onCopy: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onEditDetails: (WorkoutSession) -> Unit,
    onOpenActiveWorkout: () -> Unit,
    onSaveAsRoutine: (Long, String) -> Unit,
    onCopyExercise: (Long) -> Unit,
    onShare: (WorkoutSession) -> Unit,
    onRestore: (Long) -> Unit,
    onDelete: (WorkoutSession) -> Unit,
    focusedWorkoutId: Long? = null,
    dialogModifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var calendarView by rememberSaveable { mutableStateOf(false) }
    var selectedExerciseId by rememberSaveable { mutableStateOf<Long?>(null) }
    var exerciseFilterQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedRoutineId by rememberSaveable { mutableStateOf<Long?>(null) }
    var historyRange by rememberSaveable { mutableStateOf(WorkoutHistoryRange.All) }
    var recordsOnly by rememberSaveable { mutableStateOf(false) }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var historyOptionsExpanded by rememberSaveable { mutableStateOf(false) }
    var actionMenuId by rememberSaveable { mutableStateOf<Long?>(null) }
    var expandedSessionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var resumeCandidateId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(focusedWorkoutId, state.archivedWorkouts) {
        if (focusedWorkoutId != null) {
            showArchived = state.archivedWorkouts.any { it.id == focusedWorkoutId }
            query = ""
            selectedExerciseId = null
            selectedCategoryId = null
            selectedRoutineId = null
            historyRange = WorkoutHistoryRange.All
            recordsOnly = false
            calendarView = false
        }
    }
    val sourceHistory = if (showArchived) state.archivedWorkouts else history
    val exerciseById = (state.exercises + state.archivedExercises).associateBy(Exercise::id)
    val through = LocalDate.now(state.appSettings.zoneId())
    var calendarMonth by rememberSaveable(through) { mutableStateOf(YearMonth.from(through)) }
    var selectedCalendarDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    val from = when (historyRange) {
        WorkoutHistoryRange.Month -> through.minusMonths(1)
        WorkoutHistoryRange.ThreeMonths -> through.minusMonths(3)
        WorkoutHistoryRange.Year -> through.minusYears(1)
        WorkoutHistoryRange.All -> null
    }
    val recordSessionIds = state.personalRecords.mapNotNullTo(mutableSetOf()) { it.sourceSessionId }
    val filteredHistory = sourceHistory.filter { session ->
        val sessionExercises = state.allWorkoutExercises.filter { it.sessionId == session.id }
        if (selectedExerciseId != null && sessionExercises.none { it.exerciseId == selectedExerciseId }) return@filter false
        if (selectedCategoryId != null && sessionExercises.none { workoutExercise -> state.categoryLinks.any { it.exerciseId == workoutExercise.exerciseId && it.categoryId == selectedCategoryId } }) return@filter false
        if (selectedRoutineId != null && session.sourceRoutineId != selectedRoutineId) return@filter false
        if (from != null && session.localDate.isBefore(from)) return@filter false
        if (recordsOnly && session.id !in recordSessionIds) return@filter false
        if (query.isBlank()) true else {
            val exerciseNames = state.allWorkoutExercises
                .filter { it.sessionId == session.id }
                .mapNotNull { exerciseById[it.exerciseId]?.name }
            session.name.contains(query, true) || exerciseNames.any { it.contains(query, true) }
        }
    }
    val focusedHistory = filteredHistory.filter { focusedWorkoutId == null || it.id == focusedWorkoutId }
    val visible = if (calendarView) {
        focusedHistory.filter { session ->
            YearMonth.from(session.localDate) == calendarMonth &&
                selectedCalendarDate?.let { session.localDate == it } != false
        }
    } else {
        focusedHistory
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 112.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            WhipPageHeader(
                title = "Workout History",
                supportingText = if (focusedWorkoutId == null) {
                    "Chronological view with optional date, exercise, routine, category, and record filters."
                } else "Showing the workout opened from search.",
            )
        }
        if (focusedWorkoutId == null) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search Workouts") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            DisclosureRow(
                title = "History options",
                supportingText = buildString {
                    append(historyRange.uiLabel())
                    append(if (calendarView) " · Calendar" else " · List")
                    if (showArchived) append(" · Archived")
                    if (recordsOnly) append(" · Records only")
                },
                expanded = historyOptionsExpanded,
                onClick = { historyOptionsExpanded = !historyOptionsExpanded },
            )
        }
        if (historyOptionsExpanded) item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ToggleRow("Calendar view", calendarView) { enabled ->
                    calendarView = enabled
                    if (enabled) {
                        calendarMonth = YearMonth.from(through)
                        selectedCalendarDate = through
                    } else {
                        selectedCalendarDate = null
                    }
                }
                ToggleRow("Show discarded or archived workouts", showArchived) { showArchived = it }
                GymEnumDropdown("Date range", WorkoutHistoryRange.entries, historyRange, WorkoutHistoryRange::uiLabel) { historyRange = it }
                if (state.exercises.isNotEmpty()) {
                OutlinedTextField(
                    exerciseFilterQuery,
                    { exerciseFilterQuery = it },
                    label = { Text("Search exercise filters") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                val matches = state.exercises.filter { it.name.contains(exerciseFilterQuery, true) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    WhipFilterChip(selectedExerciseId == null, { selectedExerciseId = null }, { Text("All Exercises") })
                    matches.take(20).forEach { option ->
                        WhipFilterChip(selectedExerciseId == option.id, { selectedExerciseId = option.id }, { Text(option.name) })
                    }
                }
                if (matches.size > 20) Text("Refine the search to choose among ${matches.size} matches.", style = MaterialTheme.typography.bodySmall)
                }
                if (state.categories.isNotEmpty()) {
                    GymEnumDropdown("Category filter", listOf<Long?>(null) + state.categories.map(ExerciseCategory::id), selectedCategoryId, { id -> state.categories.firstOrNull { it.id == id }?.name ?: "All Categories" }, titleCaseValues = false) { selectedCategoryId = it }
                }
                if (state.routines.isNotEmpty()) {
                    GymEnumDropdown("Routine filter", listOf<Long?>(null) + state.routines.map(GymRoutine::id), selectedRoutineId, { id -> state.routines.firstOrNull { it.id == id }?.name ?: "All Routines" }, titleCaseValues = false) { selectedRoutineId = it }
                }
                ToggleRow("Personal-record workouts only", recordsOnly) { recordsOnly = it }
            }
        }
        if (calendarView) {
            item {
                WorkoutMonthCalendar(
                    month = calendarMonth,
                    sessions = filteredHistory,
                    selectedDate = selectedCalendarDate,
                    onSelectDate = { selectedCalendarDate = it },
                    onPreviousMonth = {
                        calendarMonth = calendarMonth.minusMonths(1)
                        selectedCalendarDate = null
                    },
                    onNextMonth = {
                        calendarMonth = calendarMonth.plusMonths(1)
                        selectedCalendarDate = null
                    },
                    firstDayOfWeek = state.appSettings.firstDayOfWeek,
                )
            }
        }
        }
        if (visible.isEmpty()) item {
            WhipEmptyState(
                title = if (filteredHistory.isEmpty() && history.isNotEmpty()) "No Matching Workouts" else "No Workout History",
                supportingText = if (history.isEmpty()) {
                    "Finished workouts will appear here."
                } else "Change the history options or clear the filter to see more workouts.",
            )
        }
        items(visible, key = WorkoutSession::id) { session ->
            val count = state.allWorkoutExercises.count { it.sessionId == session.id }
            val expanded = expandedSessionId == session.id
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(session.name.ifBlank { "Workout" }, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        if (!showArchived) {
                            Box {
                                IconButton(onClick = { actionMenuId = session.id }, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Outlined.MoreVert, contentDescription = "More options for workout ${session.name.ifBlank { "Workout" }}", modifier = Modifier.size(28.dp))
                                }
                                DropdownMenu(expanded = actionMenuId == session.id, onDismissRequest = { actionMenuId = null }) {
                                    if (state.activeSession == null) {
                                        DropdownMenuItem(text = { Text("Copy to Today") }, onClick = { actionMenuId = null; onCopy(session.id) })
                                    } else {
                                        DropdownMenuItem(text = { Text("Open Active Workout") }, onClick = { actionMenuId = null; onOpenActiveWorkout() })
                                    }
                                    DropdownMenuItem(text = { Text("Save as Routine") }, onClick = { actionMenuId = null; onSaveAsRoutine(session.id, session.name) })
                                    DropdownMenuItem(text = { Text("Share") }, onClick = { actionMenuId = null; onShare(session) })
                                    DropdownMenuItem(
                                        text = { Text("Delete Permanently", color = MaterialTheme.colorScheme.error) },
                                        onClick = { actionMenuId = null; onDelete(session) },
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        "${session.localDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))} · " +
                            quantityLabel(count, "exercise"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (showArchived) {
                        WhipTextButton(onClick = { onRestore(session.id) }) { Text("Restore to History") }
                        WhipTextButton(onClick = { onDelete(session) }) {
                            Text("Delete Permanently", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        WhipTextButton(
                            onClick = { expandedSessionId = session.id.takeUnless { expanded } },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (expanded) "Hide Details" else "View Details") }
                    }
                    if (expanded) {
                        if (session.notes.isNotBlank()) Text(session.notes)
                        state.allWorkoutExercises.filter { it.sessionId == session.id }.forEach { workoutExercise ->
                            exerciseById[workoutExercise.exerciseId]?.let { sourceExercise ->
                                Text(sourceExercise.name, fontWeight = FontWeight.SemiBold)
                                if (workoutExercise.notes.isNotBlank()) Text(workoutExercise.notes, style = MaterialTheme.typography.bodySmall)
                                WhipTextButton(onClick = { onCopyExercise(workoutExercise.id) }) { Text("Copy ${sourceExercise.name} Sets") }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WhipOutlinedButton(onClick = { onEditDetails(session) }, modifier = Modifier.weight(1f)) { Text("Edit Details") }
                            if (state.activeSession == null) {
                                WhipOutlinedButton(onClick = { resumeCandidateId = session.id }, modifier = Modifier.weight(1f)) { Text("Resume Workout") }
                            } else {
                                WhipOutlinedButton(onClick = onOpenActiveWorkout, modifier = Modifier.weight(1f)) { Text("Open Active Workout") }
                            }
                        }
                    }
                }
            }
        }
    }
    resumeCandidateId?.let { id ->
        val session = history.firstOrNull { it.id == id }
        ConfirmationDialog(
            modifier = dialogModifier,
            title = "Resume ${session?.name?.ifBlank { "Workout" } ?: "Workout"}?",
            message = "This moves the finished workout out of History, makes it the active workout, and clears its previous end time. Use Edit Details if you only need to change its name or notes.",
            confirmLabel = "Resume Workout",
            onDismiss = { resumeCandidateId = null },
            onConfirm = { onResume(id); resumeCandidateId = null },
        )
    }
}

@Composable
private fun WorkoutExerciseNotesDialog(
    modifier: Modifier = Modifier,
    exerciseName: String,
    initialNotes: String,
    machines: List<GymMachine>,
    selectedMachineId: Long?,
    machineLocked: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Long?) -> Unit,
    onCreateMachine: () -> Unit,
) {
    var notes by rememberSaveable(initialNotes) { mutableStateOf(initialNotes) }
    var machineId by rememberSaveable(selectedMachineId) { mutableStateOf(selectedMachineId) }
    ProductivityEditorDialog(
        modifier = modifier,
        testTag = "workout-exercise-notes-editor",
        onDismissRequest = onDismiss,
        title = { Text("$exerciseName Notes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes for this workout") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                if (machines.isNotEmpty() || selectedMachineId != null) {
                    SelectionField(
                        label = "Machine",
                        values = listOf<GymMachine?>(null) + machines,
                        selected = machines.firstOrNull { it.id == machineId },
                        valueText = { it?.displayName ?: "No Machine / Free Weights" },
                        enabled = !machineLocked,
                        onSelect = { machineId = it?.id },
                    )
                    if (machineLocked) Text("Machine identity is locked after the first set. Add the exercise again to switch machines without reinterpreting history.", style = MaterialTheme.typography.bodySmall)
                }
                if (!machineLocked) {
                    WhipOutlinedButton(onClick = onCreateMachine, modifier = Modifier.fillMaxWidth()) {
                        Text("Create and Assign a Machine without Losing These Notes")
                    }
                }
            }
        },
        confirmButton = { WhipTextButton(onClick = { onSave(notes, machineId) }) { Text("Save") } },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun WorkoutMonthCalendar(
    month: YearMonth,
    sessions: List<WorkoutSession>,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate?) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    firstDayOfWeek: java.time.DayOfWeek,
) {
    val counts = sessions
        .filter { YearMonth.from(it.localDate) == month }
        .groupingBy(WorkoutSession::localDate)
        .eachCount()
    val orderedDays = (0..6).map { offset -> java.time.DayOfWeek.of((firstDayOfWeek.value - 1 + offset) % 7 + 1) }
    val firstOffset = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val cellCount = ((firstOffset + month.lengthOfMonth() + 6) / 7) * 7
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPreviousMonth, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous month", modifier = Modifier.size(30.dp))
            }
            Text(
                month.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (selectedDate != null) WhipTextButton(onClick = { onSelectDate(null) }) { Text("All") }
            IconButton(onClick = onNextMonth, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = "Next month", modifier = Modifier.size(30.dp))
            }
        }
        Row {
            orderedDays.forEach { day ->
                Text(
                    day.name.take(3).lowercase().replaceFirstChar(Char::uppercase),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        (0 until cellCount).chunked(7).forEach { week ->
            Row {
                week.forEach { cell ->
                    val dayNumber = cell - firstOffset + 1
                    if (dayNumber !in 1..month.lengthOfMonth()) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        val date = month.atDay(dayNumber)
                        val count = counts[date] ?: 0
                        WhipTextButton(
                            onClick = { onSelectDate(date.takeUnless { it == selectedDate }) },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics {
                                selected = date == selectedDate
                                stateDescription = if (date == selectedDate) "Selected" else "Not selected"
                                contentDescription = "${date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))}, ${count} workout${if (count == 1) "" else "s"}"
                            },
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    dayNumber.toString(),
                                    color = if (date == selectedDate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (date == selectedDate || count > 0) FontWeight.Bold else FontWeight.Normal,
                                )
                                if (count > 0) Text("$count", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
        Text(
            selectedDate?.let { "Showing ${it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}" }
                ?: "Showing all workouts in this month",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GymProgressContent(
    state: GymUiState,
    viewModel: GymViewModel,
    onOpenExercises: () -> Unit,
    onOpenWorkout: () -> Unit,
    dialogModifier: Modifier = Modifier,
) {
    var selectedExerciseId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(state.exercises) {
        if (state.exercises.none { it.id == selectedExerciseId }) selectedExerciseId = state.exercises.firstOrNull()?.id
    }
    if (state.exercises.isEmpty() || state.history.isEmpty()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("gym-progress-list"),
            contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                WhipPageHeader(
                    title = "Progress",
                    supportingText = "Charts are built from your completed workouts and keep their source records.",
                )
            }
            item {
                WhipEmptyState(
                    title = if (state.exercises.isEmpty()) "No Exercises to Chart" else "No Workout History Yet",
                    supportingText = if (state.exercises.isEmpty()) {
                        "Create an exercise before configuring a progress chart."
                    } else "Finish a workout to create your first progress data point.",
                    primaryActionLabel = if (state.exercises.isEmpty()) "Open Exercise Library" else "Open Workout",
                    onPrimaryAction = if (state.exercises.isEmpty()) onOpenExercises else onOpenWorkout,
                )
            }
        }
        return
    }
    val exercise = state.exercises.firstOrNull { it.id == selectedExerciseId }
    val exercisePlacements = state.allWorkoutExercises.filter { it.exerciseId == selectedExerciseId }
    val usedMachineScopes = exercisePlacements.mapNotNull(WorkoutExercise::equipmentScopeKey).distinct()
    val hasUnassignedHistory = exercisePlacements.any { it.equipmentScopeKey == null }
    var selectedMachineScope by rememberSaveable(selectedExerciseId) {
        mutableStateOf(usedMachineScopes.firstOrNull())
    }
    var includeCompatibleVersions by rememberSaveable(selectedExerciseId) { mutableStateOf(false) }
    val selectedMachine = (state.machines + state.archivedMachines).firstOrNull { it.uuid == selectedMachineScope }
    val machineLevelLabel = selectedMachine?.levelLabel
        ?: exercisePlacements.firstOrNull { it.equipmentScopeKey == selectedMachineScope }?.machineLevelLabelSnapshot
        ?: "level"
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    var exerciseMenuQuery by rememberSaveable { mutableStateOf("") }
    var metric by rememberSaveable {
        mutableStateOf(
            state.exercises.firstOrNull()?.defaultGraphMetric
                ?.let { runCatching { GymGraphMetric.valueOf(it) }.getOrNull() }
                ?: GymGraphMetric.EstimatedOneRepMax,
        )
    }
    var aggregation by rememberSaveable { mutableStateOf(GymGraphAggregation.Workout) }
    var range by rememberSaveable { mutableStateOf(GymGraphRange.ThreeMonths) }
    var selectedRepetitions by rememberSaveable { mutableStateOf("5") }
    var presetName by rememberSaveable { mutableStateOf("") }
    var selectedPresetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var comparisonIds by rememberSaveable { mutableStateOf(emptySet<Long>()) }
    var customFrom by rememberSaveable { mutableStateOf("") }
    var customTo by rememberSaveable { mutableStateOf("") }
    var sourceWorkoutId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showAllChartData by rememberSaveable { mutableStateOf(false) }
    var graphOptionsExpanded by rememberSaveable { mutableStateOf(false) }
    var graphPresetsExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedChartPointDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var selectedChartSeriesName by rememberSaveable { mutableStateOf<String?>(null) }
    val machineScoped = exercisePlacements.requiresMachineScope()
    val compatibleMachineScopes = if (includeCompatibleVersions && selectedMachine?.compatibleForComparison == true) {
        val family = selectedMachine.configurationGroupId
        (state.machines + state.archivedMachines)
            .filter { it.configurationGroupId == family && it.compatibleForComparison }
            .mapTo(mutableSetOf(), GymMachine::uuid)
            .also { scopes ->
                exercisePlacements.filter { it.machineConfigurationGroupSnapshot == family }
                    .mapNotNullTo(scopes, WorkoutExercise::equipmentScopeKey)
            }
    } else setOfNotNull(selectedMachineScope)
    LaunchedEffect(selectedMachineScope, selectedMachine?.loadType) {
        if (selectedMachine?.loadType == MachineLoadType.Level && metric !in setOf(
                GymGraphMetric.MaxMachineSetting,
                GymGraphMetric.MaxRepetitions,
                GymGraphMetric.TotalRepetitions,
                GymGraphMetric.Duration,
            )
        ) {
            metric = GymGraphMetric.MaxMachineSetting
        }
        if (machineScoped) comparisonIds = emptySet()
    }
    val through = LocalDate.now(state.appSettings.zoneId())
    val validatedRange = validateGymGraphRange(range, customFrom, customTo, through)
    val effectiveFrom = validatedRange.from
    val effectiveTo = validatedRange.to
    val selectedPlacement = exercisePlacements.firstOrNull { it.equipmentScopeKey == selectedMachineScope }
    val selectedMachineLoadType = selectedMachine?.loadType ?: selectedPlacement?.machineLoadTypeSnapshot
    val selectedMachineUnitId = selectedMachine?.unitId?.takeIf(String::isNotBlank)
        ?: selectedPlacement?.machineUnitIdSnapshot?.takeIf(String::isNotBlank)
    // A single exercise should read in the unit chosen for that exercise or machine. Comparisons
    // intentionally share the global unit so every line uses one coherent axis.
    val displayWeightUnitId = if (!machineScoped && comparisonIds.isNotEmpty()) {
        state.appSettings.gymWeightUnitId
    } else if (selectedMachineLoadType == MachineLoadType.Mass) {
        selectedMachineUnitId ?: exercise?.weightUnitId ?: state.appSettings.gymWeightUnitId
    } else {
        exercise?.weightUnitId ?: state.appSettings.gymWeightUnitId
    }
    val exercisePoints = exercise?.takeIf { validatedRange.error == null }?.let {
        buildExerciseGraph(
            exercise = it,
            sessions = state.history,
            workoutExercises = state.allWorkoutExercises,
            sets = state.allSets,
            metric = metric,
            aggregation = aggregation,
            from = effectiveFrom,
            to = effectiveTo,
            selectedRepetitions = selectedRepetitions.toIntOrNull(),
            includeWarmups = state.appSettings.includeWarmupsInGymStats,
            oneRepMaxRepCutoff = state.appSettings.oneRepMaxRepCutoff,
            adjustOneRepMaxForEffort = state.appSettings.adjustE1rmForEffort,
            firstDayOfWeek = state.appSettings.firstDayOfWeek,
            machineScopeUuid = selectedMachineScope,
            machineScopeUuids = compatibleMachineScopes,
            restrictToMachine = machineScoped,
        ).map { point -> point.copy(value = metric.displayValue(point.value, displayWeightUnitId, state.appSettings.distanceUnitId)) }
    }.orEmpty()
    val sourceWorkout = sourceWorkoutId?.let { id -> state.history.firstOrNull { it.id == id } }
    val comparisons = if (machineScoped || validatedRange.error != null) emptyMap() else comparisonIds.mapNotNull { id -> state.exercises.firstOrNull { it.id == id } }.associateWith { compared ->
        buildExerciseGraph(
            exercise = compared, sessions = state.history, workoutExercises = state.allWorkoutExercises,
            sets = state.allSets, metric = metric, aggregation = aggregation, from = effectiveFrom,
            to = effectiveTo, selectedRepetitions = selectedRepetitions.toIntOrNull(),
            includeWarmups = state.appSettings.includeWarmupsInGymStats,
            oneRepMaxRepCutoff = state.appSettings.oneRepMaxRepCutoff,
            adjustOneRepMaxForEffort = state.appSettings.adjustE1rmForEffort,
            firstDayOfWeek = state.appSettings.firstDayOfWeek,
        ).map { point -> point.copy(value = metric.displayValue(point.value, state.appSettings.gymWeightUnitId, state.appSettings.distanceUnitId)) }
    }
    val displayUnit = metric.displayUnit(
        displayWeightUnitId,
        state.appSettings.distanceUnitId,
        machineLevelLabel,
    )
    val primarySeriesName = exercise?.name.orEmpty()
    val chartSeries = buildList {
        if (exercisePoints.isNotEmpty()) add(GymChartSeries(primarySeriesName, exercisePoints))
        comparisons.forEach { (compared, points) -> if (points.isNotEmpty()) add(GymChartSeries(compared.name, points)) }
    }
    val selectedSeries = chartSeries.firstOrNull { it.name == (selectedChartSeriesName ?: primarySeriesName) }
    val selectedChartPoint = selectedChartPointDate?.let { date -> selectedSeries?.points?.lastOrNull { it.date == date } }
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("gym-progress-list"),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            WhipPageHeader(
                title = "Progress",
                supportingText = "Choose a metric, range, and aggregation. Derived estimates retain their source workouts.",
                modifier = Modifier.testTag("gym-progress-title"),
            )
            if (exercisePoints.isNotEmpty()) {
                val unit = displayUnit
                val summary = chartDescription(
                    exercise?.name.orEmpty(),
                    metric.label.uiTitleCase(),
                    exercisePoints,
                    unit,
                )
                val minimum = exercisePoints.minOf { it.value }
                val maximum = exercisePoints.maxOf { it.value }
                val change = exercisePoints.last().value - exercisePoints.first().value
                Text(
                    "${exercisePoints.size} points · range ${formatNumber(minimum, state.appSettings.numberPrecision)}–" +
                        "${formatNumber(maximum, state.appSettings.numberPrecision)} $unit · " +
                        "change ${if (change > 0) "+" else ""}${formatNumber(change, state.appSettings.numberPrecision)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("gym-chart-summary").semantics { contentDescription = summary },
                )
            }
        }
        item {
            Text("Exercise", style = MaterialTheme.typography.labelMedium)
            WhipOutlinedButton(onClick = { menuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(exercise?.name ?: "Choose Exercise", modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                OutlinedTextField(
                    exerciseMenuQuery,
                    { exerciseMenuQuery = it },
                    label = { Text("Search exercises") },
                    singleLine = true,
                    modifier = Modifier.width(300.dp).padding(8.dp),
                )
                state.exercises.filter { exerciseMatchesQuery(it, exerciseMenuQuery) }.take(50).forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.name) },
                        onClick = {
                            selectedExerciseId = option.id
                            metric = runCatching { GymGraphMetric.valueOf(option.defaultGraphMetric) }
                                .getOrDefault(GymGraphMetric.EstimatedOneRepMax)
                            menuExpanded = false
                        },
                    )
                }
            }
        }
        if (machineScoped) item {
            Text("Machine / Equipment Scope", style = MaterialTheme.typography.labelMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (hasUnassignedHistory) {
                    WhipFilterChip(selected = selectedMachineScope == null, onClick = { selectedMachineScope = null }, label = { Text("No Machine / Free Weights") })
                }
                usedMachineScopes.forEach { machineScope ->
                    val profile = (state.machines + state.archivedMachines).firstOrNull { it.uuid == machineScope }
                    val snapshot = exercisePlacements.firstOrNull { it.equipmentScopeKey == machineScope }?.machineNameSnapshot
                    WhipFilterChip(
                        selected = selectedMachineScope == machineScope,
                        onClick = { selectedMachineScope = machineScope },
                        label = { Text(profile?.displayName ?: snapshot ?: "Deleted machine") },
                    )
                }
            }
            if (selectedMachine?.compatibleForComparison == true) {
                ToggleRow("Include explicitly compatible configuration versions", includeCompatibleVersions) {
                    includeCompatibleVersions = it
                }
            }
            Text(
                if (includeCompatibleVersions && compatibleMachineScopes.size > 1) {
                    "Combining ${compatibleMachineScopes.size} versions in the same user-approved configuration family. Every source retains its version snapshot."
                } else "Whip does not merge strength, volume, previous-set, or PR data across machines or versions unless you opt in.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item {
            val availableMetrics = if (selectedMachine?.loadType == MachineLoadType.Level) {
                listOf(GymGraphMetric.MaxMachineSetting, GymGraphMetric.MaxRepetitions, GymGraphMetric.TotalRepetitions, GymGraphMetric.Duration)
            } else {
                GymGraphMetric.entries.filterNot { it == GymGraphMetric.MaxMachineSetting }
            }
            GymEnumDropdown("Metric", availableMetrics, metric.takeIf { it in availableMetrics } ?: availableMetrics.first(), { it.label }) { metric = it }
        }
        item {
            DisclosureRow(
                title = "Graph options",
                supportingText = "${range.uiLabel()} · ${aggregation.name}",
                expanded = graphOptionsExpanded,
                onClick = { graphOptionsExpanded = !graphOptionsExpanded },
            )
        }
        if (graphOptionsExpanded) item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ResponsiveFieldPair(
                    first = { field -> Column(field) {
                        GymEnumDropdown("Range", GymGraphRange.entries, range, GymGraphRange::uiLabel) { selected ->
                            range = selected
                            if (selected == GymGraphRange.Custom) {
                                if (customFrom.isBlank()) customFrom = through.minusMonths(3).toString()
                                if (customTo.isBlank()) customTo = through.toString()
                            }
                        }
                    } },
                    second = { field -> Column(field) { GymEnumDropdown("Aggregate", GymGraphAggregation.entries, aggregation, { it.name }) { aggregation = it } } },
                )
                if (range == GymGraphRange.Custom) ResponsiveFieldPair(
                    first = { field -> OutlinedTextField(customFrom, { customFrom = it }, label = { Text("From YYYY-MM-DD") }, isError = validatedRange.error != null, modifier = field) },
                    second = { field -> OutlinedTextField(customTo, { customTo = it }, label = { Text("To YYYY-MM-DD") }, isError = validatedRange.error != null, modifier = field) },
                )
                if (range == GymGraphRange.Custom && validatedRange.error != null) {
                    Text(requireNotNull(validatedRange.error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (metric in setOf(GymGraphMetric.MaxWeightForReps, GymGraphMetric.ActualRepMaxHistory)) {
                    NumberField(selectedRepetitions, { selectedRepetitions = it }, "Repetitions", integer = true)
                }
                if (state.exercises.size > 1 && !machineScoped) {
                    Text("Compare up to 3 Exercises", style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.exercises.filter { it.id != selectedExerciseId }.forEach { option ->
                            WhipFilterChip(
                                selected = option.id in comparisonIds,
                                onClick = { comparisonIds = if (option.id in comparisonIds) comparisonIds - option.id else if (comparisonIds.size < 3) comparisonIds + option.id else comparisonIds },
                                label = { Text(option.name) },
                            )
                        }
                    }
                }
            }
        }
        item {
            if (exercisePoints.isEmpty()) {
                Text("No eligible data for this metric and date range.")
            } else {
                val best = if (metric == GymGraphMetric.Pace) exercisePoints.minOf { it.value } else exercisePoints.maxOf { it.value }
                Text("${metric.label.uiTitleCase()} · ${formatNumber(best, state.appSettings.numberPrecision)} $displayUnit · best")
                SharedGymLineChart(
                    series = chartSeries.map { it.copy(points = downsampleEvenly(it.points, 200)) },
                    unit = displayUnit,
                    precision = state.appSettings.numberPrecision,
                    description = chartSeries.joinToString(" ") { series ->
                        chartDescription(series.name, metric.label, series.points, displayUnit)
                    },
                    onPointSelected = { seriesName, point ->
                        selectedChartSeriesName = seriesName
                        selectedChartPointDate = point.date
                    },
                )
                DisclosureButton(
                    label = "Data table · ${exercisePoints.size}",
                    expanded = showAllChartData,
                    onClick = { showAllChartData = !showAllChartData },
                )
                (if (showAllChartData) exercisePoints else exercisePoints.takeLast(8)).forEach { point ->
                    Text(
                        "${point.date}: ${formatNumber(point.value, state.appSettings.numberPrecision)} · tap for details",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.heightIn(min = 48.dp).clickable(onClickLabel = "Open details for ${point.date}") {
                            selectedChartSeriesName = primarySeriesName
                            selectedChartPointDate = point.date
                        },
                    )
                }
            }
        }
        item {
            val records = state.personalRecords.filter {
                it.exerciseId == selectedExerciseId && it.current &&
                    (!machineScoped || if (selectedMachineScope == null) {
                        it.machineProfileUuidSnapshot == null
                    } else {
                        it.machineProfileUuidSnapshot in compatibleMachineScopes
                    })
            }
            if (records.isNotEmpty()) {
                HorizontalDivider()
                Text("Current Records", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                records.forEach { record ->
                    Text(
                        "${record.type.name}: ${formatNumber(record.displayValue(displayWeightUnitId, state.appSettings.distanceUnitId), state.appSettings.numberPrecision)} " +
                            record.displayUnit(displayWeightUnitId, state.appSettings.distanceUnitId),
                    )
                }
            }
        }
        item {
            DisclosureRow(
                title = "Graph presets",
                supportingText = if (state.graphPresets.isEmpty()) "Save this graph setup for reuse." else "${state.graphPresets.size} saved",
                expanded = graphPresetsExpanded || selectedPresetId != null,
                onClick = { graphPresetsExpanded = !graphPresetsExpanded },
            )
        }
        if (graphPresetsExpanded || selectedPresetId != null) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(presetName, { presetName = it }, label = { Text("Preset name") }, modifier = Modifier.fillMaxWidth())
            WhipOutlinedButton(
                enabled = presetName.isNotBlank() && exercise != null,
                onClick = {
                    val exerciseIds = listOfNotNull(exercise?.id) + comparisonIds
                    selectedPresetId?.let { id ->
                        viewModel.updateGraphPreset(id, presetName, exerciseIds, metric.name, range.name, aggregation.name)
                    } ?: viewModel.saveGraphPreset(presetName, exerciseIds, metric.name, range.name, aggregation.name)
                    presetName = ""
                    selectedPresetId = null
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (selectedPresetId == null) "Save Graph Preset" else "Update Selected Preset") }
            if (selectedPresetId != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WhipTextButton(
                        onClick = { selectedPresetId = null; presetName = "" },
                        modifier = Modifier.weight(1f),
                    ) { Text("Cancel Edit") }
                    WhipTextButton(
                        onClick = {
                            viewModel.deleteGraphPreset(requireNotNull(selectedPresetId))
                            selectedPresetId = null
                            presetName = ""
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Delete Preset", color = MaterialTheme.colorScheme.error) }
                }
            }
            if (state.graphPresets.isNotEmpty()) {
                Text("Saved Presets", style = MaterialTheme.typography.labelSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.graphPresets.forEach { preset ->
                        WhipFilterChip(
                            selected = false,
                            onClick = {
                                val ids = preset.exerciseIds.filter { id -> state.exercises.any { it.id == id } }
                                selectedExerciseId = ids.firstOrNull()
                                comparisonIds = ids.drop(1).take(3).toSet()
                                metric = runCatching { GymGraphMetric.valueOf(preset.metric) }.getOrDefault(metric)
                                range = runCatching { GymGraphRange.valueOf(preset.dateRange) }.getOrDefault(range)
                                aggregation = runCatching { GymGraphAggregation.valueOf(preset.aggregation) }.getOrDefault(aggregation)
                                selectedPresetId = preset.id
                                presetName = preset.name
                            },
                            label = { Text(preset.name) },
                        )
                    }
                }
            }
            }
        }
        item {
            val weekStart = through.with(java.time.temporal.TemporalAdjusters.previousOrSame(state.appSettings.firstDayOfWeek))
            val summary = buildWeeklyGymSummary(weekStart, state.history, state.allWorkoutExercises, state.allSets, state.exercises, state.personalRecords)
            HorizontalDivider()
            Text("Week of ${summary.weekStart}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${summary.workouts} workouts · ${summary.trainingDays} days · ${formatDuration(summary.elapsedSeconds)}")
            Text(
                "${summary.completedSets} sets · ${summary.repetitions} reps · " +
                    "${formatNumber(massFromKilograms(summary.volumeKg, state.appSettings.gymWeightUnitId), state.appSettings.numberPrecision)} " +
                    "${unitSymbol(state.appSettings.gymWeightUnitId)}·rep",
            )
            Text("${summary.newPersonalRecords} personal records")
            state.categories.forEach { category ->
                val exerciseIds = state.categoryLinks.filter { it.categoryId == category.id }.mapTo(mutableSetOf()) { it.exerciseId }
                val weekSessionIds = state.history.filter { it.localDate in weekStart..weekStart.plusDays(6) }.mapTo(mutableSetOf()) { it.id }
                val weekWorkoutExerciseById = state.allWorkoutExercises
                    .filter { it.sessionId in weekSessionIds && it.exerciseId in exerciseIds }
                    .associateBy { it.id }
                val categorySets = state.allSets.filter {
                    it.workoutExerciseId in weekWorkoutExerciseById && it.completed && it.deletedAtMillis == null &&
                        it.classification.name in state.appSettings.hardSetClassifications
                }
                fun allocationFor(set: WorkoutSet): Double {
                    val exerciseId = weekWorkoutExerciseById[set.workoutExerciseId]?.exerciseId ?: return 0.0
                    val linked = state.categoryLinks.filter { it.exerciseId == exerciseId }.map { it.categoryId }.sorted()
                    return when (state.appSettings.categoryAllocationMode) {
                        "Full" -> 1.0
                        "PrimaryOnly" -> if (linked.firstOrNull() == category.id) 1.0 else 0.0
                        else -> if (category.id in linked) 1.0 / linked.size.coerceAtLeast(1) else 0.0
                    }
                }
                val categoryVolume = categorySets.sumOf { set ->
                    val workoutExercise = weekWorkoutExerciseById[set.workoutExerciseId]
                    val categoryExercise = state.exercises.firstOrNull { it.id == workoutExercise?.exerciseId }
                    if (categoryExercise == null || workoutExercise == null) 0.0 else {
                        set.volumeKg(workoutExercise.applyPolicySnapshot(categoryExercise), includeWarmups = true) * allocationFor(set)
                    }
                }
                val allocatedSetCount = categorySets.sumOf(::allocationFor)
                if (allocatedSetCount > 0.0) Text(
                    "${category.name}: ${formatNumber(allocatedSetCount, state.appSettings.numberPrecision)} allocated hard sets · " +
                        "${formatNumber(massFromKilograms(categoryVolume, state.appSettings.gymWeightUnitId), state.appSettings.numberPrecision)} " +
                        "${unitSymbol(state.appSettings.gymWeightUnitId)}·rep",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
    sourceWorkout?.let { workout ->
        PaneAwareAlertDialog(
            modifier = dialogModifier,
            onDismissRequest = { sourceWorkoutId = null },
            title = { Text(workout.name.ifBlank { "Workout" }) },
            text = { Text("${workout.localDate}\n${workout.notes.ifBlank { "Source workout for this graph point." }}") },
            confirmButton = { WhipTextButton(onClick = { sourceWorkoutId = null }) { Text("Close") } },
        )
    }
    selectedChartPoint?.let { point ->
        PaneAwareAlertDialog(
            modifier = dialogModifier,
            onDismissRequest = { selectedChartPointDate = null },
            title = { Text("${selectedSeries?.name.orEmpty()} · ${metric.label.uiTitleCase()} · ${point.date}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${formatNumber(point.value, state.appSettings.numberPrecision)} " +
                            displayUnit,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text("Built from ${point.sourceCount} source${if (point.sourceCount == 1) "" else "s"}.")
                    point.sourceSessionId?.let { sessionId ->
                        WhipOutlinedButton(
                            onClick = {
                                sourceWorkoutId = sessionId
                                selectedChartPointDate = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Open Source Workout") }
                    }
                }
            },
            confirmButton = { WhipTextButton(onClick = { selectedChartPointDate = null }) { Text("Close") } },
        )
    }
}

internal fun List<WorkoutExercise>.requiresMachineScope(): Boolean = any { it.equipmentScopeKey != null }

@Composable
private fun GymToolsContent(
    state: GymUiState,
    onSavePreset: (PlatePreset) -> Unit,
    onDeletePreset: (String) -> Unit,
    dialogModifier: Modifier = Modifier,
) {
    val weightUnit = state.appSettings.gymWeightUnitId
    val weightSymbol = unitSymbol(weightUnit)
    var weight by rememberSaveable(weightUnit) { mutableStateOf(if (weightUnit == "pound") "175" else "80") }
    var reps by rememberSaveable { mutableStateOf("8") }
    var increment by rememberSaveable(weightUnit) { mutableStateOf(if (weightUnit == "pound") "5" else "2.5") }
    var knownOneRepMax by rememberSaveable { mutableStateOf(false) }
    var formula by rememberSaveable {
        mutableStateOf(
            runCatching { EstimatedOneRepMaxFormula.valueOf(state.appSettings.oneRepMaxFormula) }
                .getOrDefault(EstimatedOneRepMaxFormula.Epley),
        )
    }
    var targetWeight by rememberSaveable(weightUnit) { mutableStateOf(if (weightUnit == "pound") "225" else "100") }
    var barWeight by rememberSaveable(weightUnit) { mutableStateOf(if (weightUnit == "pound") "45" else "20") }
    var plates by rememberSaveable(weightUnit) { mutableStateOf(if (weightUnit == "pound") "45,35,25,10,5,2.5" else "20,15,10,5,2.5,1.25") }
    var plateQuantities by rememberSaveable(weightUnit) { mutableStateOf("") }
    var collarWeight by rememberSaveable(weightUnit) { mutableStateOf("0") }
    var perSideLoading by rememberSaveable { mutableStateOf(true) }
    var plateUnit by rememberSaveable(weightUnit) { mutableStateOf(if (weightUnit == "pound") "lb" else "kg") }
    var presetName by rememberSaveable { mutableStateOf("") }
    var selectedPreset by rememberSaveable { mutableStateOf<String?>(null) }
    var activeTool by rememberSaveable { mutableStateOf("1RM") }
    var pendingPlateUnit by rememberSaveable { mutableStateOf<String?>(null) }
    fun unitId(label: String): String = if (label == "lb") "pound" else "kilogram"
    fun convertPlateInputs(selected: String) {
        val from = unitId(plateUnit)
        val to = unitId(selected)
        targetWeight = targetWeight.toWhipDoubleOrNull()?.let { editableNumber(convertPracticalMassValue(it, from, to)) } ?: targetWeight
        barWeight = barWeight.toWhipDoubleOrNull()?.let { editableNumber(convertPracticalMassValue(it, from, to)) } ?: barWeight
        collarWeight = collarWeight.toWhipDoubleOrNull()?.let { editableNumber(convertPracticalMassValue(it, from, to)) } ?: collarWeight
        plates = plates.split(',').joinToString(",") { raw ->
            raw.trim().toWhipDoubleOrNull()?.let { editableNumber(convertPracticalMassValue(it, from, to)) } ?: raw.trim()
        }
        plateQuantities = parsePlateQuantities(plateQuantities)?.entries?.joinToString(",") { (plate, quantity) ->
            "${editableNumber(convertPracticalMassValue(plate, from, to))}:$quantity"
        } ?: plateQuantities
        plateUnit = selected
    }
    fun resetPlateInputs(selected: String) {
        plateUnit = selected
        targetWeight = if (selected == "lb") "225" else "100"
        barWeight = if (selected == "lb") "45" else "20"
        plates = if (selected == "lb") "45,35,25,10,5,2.5" else "20,15,10,5,2.5,1.25"
        collarWeight = "0"
        plateQuantities = ""
    }
    val estimate = calculateRepMaxTable(weight.toWhipDoubleOrNull() ?: -1.0, if (knownOneRepMax) 1 else reps.toIntOrNull() ?: -1, formula, increment.toWhipDoubleOrNull() ?: 2.5)
    val parsedPlateQuantities = parsePlateQuantities(plateQuantities)
    val parsedPlates = parsePositiveNumberList(plates, "plate")
    val targetValue = targetWeight.toWhipDoubleOrNull()
    val barValue = barWeight.toWhipDoubleOrNull()
    val collarValue = collarWeight.toWhipDoubleOrNull()
    val plateInputError = when {
        targetValue == null || targetValue <= 0.0 -> "Enter a positive target weight"
        barValue == null || barValue < 0.0 -> "Enter a non-negative bar, sled, or base weight"
        collarValue == null || collarValue < 0.0 -> "Enter a non-negative collar or fixed add-on weight"
        parsedPlates.error != null -> parsedPlates.error
        parsedPlateQuantities == null -> "Use plate:quantity pairs such as 45:4, 25:2"
        else -> null
    }
    val loading = if (plateInputError == null) {
        calculatePlateLoading(
            requireNotNull(targetValue),
            requireNotNull(barValue),
            parsedPlates.values,
            requireNotNull(parsedPlateQuantities),
            requireNotNull(collarValue),
            perSideLoading,
        )
    } else {
        null
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 112.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            WhipPageHeader(
                title = "Workout Tools",
                supportingText = "Estimates are planning aids, not medical or safety advice.",
            )
        }
        item {
            SegmentedChoiceBar(
                selected = activeTool,
                choices = listOf("1RM", "Plate"),
                onSelect = { activeTool = it },
                label = { if (it == "1RM") "1RM Calculator" else "Plate Calculator" },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (activeTool == "1RM") {
        item { Text("1RM and Percentage Calculator", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item { ToggleRow("Weight is a known 1RM", knownOneRepMax) { knownOneRepMax = it } }
        item { ResponsiveFieldPair(
            first = { field -> NumberField(weight, { weight = it }, "Weight ($weightSymbol)", modifier = field) },
            second = { field ->
                if (knownOneRepMax) {
                    Text("Repetitions are not used when the weight is a known 1RM.", modifier = field, style = MaterialTheme.typography.bodySmall)
                } else {
                    NumberField(reps, { reps = it }, "Reps", integer = true, modifier = field)
                }
            },
        ) }
        item { ResponsiveFieldPair(
            first = { field -> Column(field) { GymEnumDropdown("Formula", EstimatedOneRepMaxFormula.entries, formula, { it.name }) { formula = it } } },
            second = { field -> NumberField(increment, { increment = it }, "Round to ($weightSymbol)", modifier = field) },
        ) }
        item {
            if (estimate == null) Text("Enter a positive weight and 1–36 repetitions.") else {
                Text("Estimated 1RM: ${formatNumber(estimate.oneRepMax, state.appSettings.numberPrecision)} $weightSymbol", fontWeight = FontWeight.Bold)
                if (!knownOneRepMax && (reps.toIntOrNull() ?: 0) > state.appSettings.oneRepMaxRepCutoff) {
                    Text("High-repetition estimates are less reliable; your workout cutoff is ${state.appSettings.oneRepMaxRepCutoff} reps.", style = MaterialTheme.typography.bodySmall)
                }
                Text(estimate.percentages.joinToString(" · ") { "${it.first}% ${formatNumber(it.second, state.appSettings.numberPrecision)} $weightSymbol" }, style = MaterialTheme.typography.bodySmall)
            }
        }
        }
        if (activeTool == "Plate") {
        item { Text("Plate Calculator", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (state.appSettings.platePresets.isNotEmpty()) {
            item {
                Text("Saved Plate Presets", fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.appSettings.platePresets.forEach { preset ->
                        WhipFilterChip(
                            selected = selectedPreset == preset.name,
                            onClick = {
                                selectedPreset = preset.name
                                plateUnit = if (preset.unitId == "pound") "lb" else "kg"
                                barWeight = editableNumber(preset.barWeight)
                                plates = preset.plates.joinToString(",", transform = ::editableNumber)
                                plateQuantities = preset.plateQuantities.entries.sortedByDescending { it.key }.joinToString(",") { "${editableNumber(it.key)}:${it.value}" }
                                collarWeight = editableNumber(preset.collarWeight)
                                perSideLoading = preset.perSideLoading
                            },
                            label = { Text(preset.name) },
                        )
                    }
                }
                selectedPreset?.let { name ->
                    WhipTextButton(onClick = { onDeletePreset(name); selectedPreset = null }) { Text("Delete Selected Preset") }
                }
            }
        }
        item {
            GymEnumDropdown("Plate unit", listOf("kg", "lb"), plateUnit, { it }, titleCaseValues = false) { selected ->
                if (selected != plateUnit) pendingPlateUnit = selected
            }
        }
        item { ResponsiveFieldPair(
            first = { field -> NumberField(targetWeight, { targetWeight = it }, "Target ($plateUnit)", modifier = field) },
            second = { field -> NumberField(barWeight, { barWeight = it }, "Bar / sled / base ($plateUnit)", modifier = field) },
        ) }
        item { NumberField(collarWeight, { collarWeight = it }, "Total collars / fixed add-on ($plateUnit)") }
        item { ToggleRow("Load matching plates on each side", perSideLoading) { perSideLoading = it } }
        item {
            OutlinedTextField(
                plates,
                { plates = it },
                label = { Text("Available plates ($plateUnit), comma-separated") },
                supportingText = { Text(parsedPlates.error ?: "Every entry must be a positive number.") },
                isError = parsedPlates.error != null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                plateQuantities,
                { plateQuantities = it },
                label = { Text("Optional total inventory quantities") },
                supportingText = { Text(if (parsedPlateQuantities == null) "Use plate:quantity pairs, e.g. 45:4, 25:2" else "Blank means unlimited. For per-side loading, Whip only uses complete pairs.") },
                isError = parsedPlateQuantities == null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(presetName, { presetName = it }, label = { Text("Preset name") }, modifier = Modifier.weight(1f), singleLine = true)
                WhipTextButton(
                    enabled = presetName.isNotBlank() && plateInputError == null,
                    onClick = {
                        onSavePreset(
                            PlatePreset(
                                name = presetName.trim(),
                                unitId = if (plateUnit == "lb") "pound" else "kilogram",
                                barWeight = requireNotNull(barValue),
                                plates = parsedPlates.values.distinct().sortedDescending(),
                                plateQuantities = requireNotNull(parsedPlateQuantities),
                                collarWeight = requireNotNull(collarValue),
                                perSideLoading = perSideLoading,
                            ),
                        )
                        selectedPreset = presetName.trim()
                        presetName = ""
                    },
                ) { Text("Save") }
            }
        }
        item {
            if (loading == null) {
                Text(requireNotNull(plateInputError), color = MaterialTheme.colorScheme.error)
            } else {
                Text("${if (loading.perSideLoading) "Per side" else "Loaded plates"}: ${loading.platesPerSide.joinToString(" + ").ifBlank { "no plates" }} $plateUnit")
                Text("${if (loading.exact) "Exact" else "Closest available"}: ${formatNumber(loading.achievedWeight, state.appSettings.numberPrecision)} $plateUnit · difference ${formatNumber(loading.remainder, state.appSettings.numberPrecision)} $plateUnit")
            }
        }
        }
    }
    pendingPlateUnit?.let { selected ->
        DefaultsMeaningChangeDialog(
            modifier = dialogModifier,
            title = "Change Plate Calculator to $selected?",
            explanation = "Convert preserves the approximate physical weights. Keep changes only the unit label. Reset replaces the target, bar, plates, collars, and inventory with standard $selected defaults.",
            onConvert = { convertPlateInputs(selected); pendingPlateUnit = null },
            onKeep = { plateUnit = selected; pendingPlateUnit = null },
            onReset = { resetPlateInputs(selected); pendingPlateUnit = null },
            onCancel = { pendingPlateUnit = null },
        )
    }
}

@Composable
private fun <T> GymEnumDropdown(
    label: String,
    values: List<T>,
    selected: T,
    text: (T) -> String,
    titleCaseValues: Boolean = true,
    onSelect: (T) -> Unit,
) {
    SelectionField(
        label = label,
        values = values,
        selected = selected,
        valueText = { value -> text(value).let { if (titleCaseValues) it.uiTitleCase() else it } },
        onSelect = onSelect,
    )
}

@Composable
private fun RoutineContent(
    state: GymUiState,
    viewModel: GymViewModel,
    focusedRoutineId: Long? = null,
    onDeleteRequest: (GymRoutine) -> Unit,
    onOpenActiveWorkout: () -> Unit,
    dialogModifier: Modifier = Modifier,
    onEditorStateChange: (Boolean) -> Unit = {},
) {
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingRoutineId by rememberSaveable { mutableStateOf<Long?>(null) }
    val editing = editingRoutineId?.let { id -> (state.routines + state.archivedRoutines).firstOrNull { it.id == id } }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var actionMenuId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(focusedRoutineId, state.archivedRoutines) {
        if (focusedRoutineId != null) {
            showArchived = state.archivedRoutines.any { it.id == focusedRoutineId }
        }
    }
    val source = if (showArchived) state.archivedRoutines else state.routines
    val visible = source.filter {
        focusedRoutineId == null || it.id == focusedRoutineId
    }
    if (showEditor || editing != null) {
        val initial = editing?.let { routine ->
            RoutineDraft(
                name = routine.name,
                notes = routine.notes,
                days = state.routineDays.filter { it.routineId == routine.id }.sortedBy { it.position }.map { day ->
                    RoutineDayDraft(
                        day.name,
                        state.routineExercises.filter { it.routineDayId == day.id }.sortedBy { it.position }.map { routineExercise ->
                            RoutineExerciseDraft(
                                exerciseId = routineExercise.exerciseId,
                                notes = routineExercise.notes,
                                groupKey = routineExercise.groupKey,
                                plannedSets = state.routineSets.filter { it.routineExerciseId == routineExercise.id }.sortedBy { it.position }.map { it.draft },
                                copyPreviousWorkout = routineExercise.copyPreviousWorkout,
                                machineId = routineExercise.machineId,
                                equipmentBindingState = routineExercise.equipmentBindingState,
                                machineProfileUuidSnapshot = routineExercise.machineProfileUuidSnapshot,
                                machineNameSnapshot = routineExercise.machineNameSnapshot,
                                machineLoadTypeSnapshot = routineExercise.machineLoadTypeSnapshot,
                                machineUnitIdSnapshot = routineExercise.machineUnitIdSnapshot,
                                machineLevelLabelSnapshot = routineExercise.machineLevelLabelSnapshot,
                                machineLoadInterpretationSnapshot = routineExercise.machineLoadInterpretationSnapshot,
                                machineConfigurationGroupSnapshot = routineExercise.machineConfigurationGroupSnapshot,
                                machineConfigurationVersionSnapshot = routineExercise.machineConfigurationVersionSnapshot,
                                machineConfigurationSnapshot = routineExercise.machineConfigurationSnapshot,
                            )
                        },
                    )
                },
            )
        }
        RoutineBuilderScreen(
            routineId = editing?.id,
            gymState = state,
            initial = initial,
            dialogModifier = dialogModifier,
            onDismiss = {
                showEditor = false
                editingRoutineId = null
                onEditorStateChange(false)
            },
            onSave = { draft, complete -> viewModel.saveRoutineFromBuilder(editing?.id, draft, complete) },
            onCreateExercise = viewModel::createExerciseForRoutine,
            onCreateMachine = viewModel::createMachineForRoutine,
            onSavePrescriptionScheme = viewModel::saveRepPrescriptionScheme,
            onDeletePrescriptionScheme = viewModel::deleteRepPrescriptionScheme,
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 112.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            WhipPageHeader(
                title = "Routines",
                supportingText = if (focusedRoutineId == null) {
                    "Reusable multi-day templates. Starting one copies it into a new workout without changing the template."
                } else "Showing the routine opened from search.",
            ) {
                if (focusedRoutineId == null) WhipButton(onClick = {
                    showEditor = true
                    onEditorStateChange(true)
                }) { Text("Create Routine") }
            }
        }
        if (focusedRoutineId == null) {
        item { ToggleRow("Show archived", showArchived) { showArchived = it } }
        }
        if (visible.isEmpty()) item {
            WhipEmptyState(
                title = if (showArchived) "No Archived Routines" else "No Routines Yet",
                supportingText = "Build a routine here or save a completed workout from History.",
                primaryActionLabel = "Create Routine".takeUnless { showArchived },
                onPrimaryAction = {
                    showEditor = true
                    onEditorStateChange(true)
                }.takeUnless { showArchived },
            )
        }
        items(visible.size, key = { visible[it].id }) { routineIndex ->
            val routine = visible[routineIndex]
            val days = state.routineDays.filter { it.routineId == routine.id }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(routine.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        ItemEditButton("routine", routine.name, onEdit = {
                            editingRoutineId = routine.id
                            onEditorStateChange(true)
                        })
                        Box {
                            IconButton(onClick = { actionMenuId = routine.id }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = "More options for routine ${routine.name}", modifier = Modifier.size(28.dp))
                            }
                            DropdownMenu(expanded = actionMenuId == routine.id, onDismissRequest = { actionMenuId = null }) {
                                if (!showArchived) {
                                    DropdownMenuItem(
                                        text = { Text("Move Up") }, enabled = routineIndex > 0,
                                        leadingIcon = { Icon(Icons.Outlined.ArrowUpward, contentDescription = null) },
                                        onClick = {
                                            actionMenuId = null
                                            val ids = visible.map(GymRoutine::id).toMutableList()
                                            java.util.Collections.swap(ids, routineIndex, routineIndex - 1)
                                            viewModel.reorderRoutines(ids)
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Move Down") }, enabled = routineIndex < visible.lastIndex,
                                        leadingIcon = { Icon(Icons.Outlined.ArrowDownward, contentDescription = null) },
                                        onClick = {
                                            actionMenuId = null
                                            val ids = visible.map(GymRoutine::id).toMutableList()
                                            java.util.Collections.swap(ids, routineIndex, routineIndex + 1)
                                            viewModel.reorderRoutines(ids)
                                        },
                                    )
                                }
                                DropdownMenuItem(text = { Text("Duplicate") }, onClick = { actionMenuId = null; viewModel.duplicateRoutine(routine.id) })
                                DropdownMenuItem(text = { Text(if (routine.pinned) "Unpin from Home" else "Pin to Home") }, onClick = { actionMenuId = null; viewModel.setRoutinePinned(routine.id, !routine.pinned) })
                                DropdownMenuItem(text = { Text(if (routine.archived) "Restore" else "Archive") }, onClick = { actionMenuId = null; viewModel.setRoutineArchived(routine.id, !routine.archived) })
                                DropdownMenuItem(
                                    text = { Text("Delete Permanently", color = MaterialTheme.colorScheme.error) },
                                    onClick = { actionMenuId = null; onDeleteRequest(routine) },
                                )
                            }
                        }
                    }
                    if (routine.notes.isNotBlank()) Text(routine.notes)
                    Text("${days.size} day${if (days.size == 1) "" else "s"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val routineHistory = state.history.filter { it.sourceRoutineId == routine.id }
                    Text(
                        if (routineHistory.isEmpty()) "No performed sessions yet" else
                            "${routineHistory.size} performed session${if (routineHistory.size == 1) "" else "s"} · last ${routineHistory.maxOf { it.localDate }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    days.forEach { day ->
                        val dayExercises = state.routineExercises.filter { it.routineDayId == day.id }
                        val count = dayExercises.size
                        val needsEquipment = dayExercises.filter {
                            it.equipmentBindingState == RoutineEquipmentBindingState.NeedsEquipment
                        }
                        val archivedEquipment = dayExercises.count { placement ->
                            placement.machineId in state.archivedMachines.mapTo(mutableSetOf(), GymMachine::id)
                        }
                        if (needsEquipment.isNotEmpty()) {
                            Text(
                                "${needsEquipment.size} placement${if (needsEquipment.size == 1) "" else "s"} need equipment before this day can start",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        } else if (archivedEquipment > 0) {
                            Text(
                                "Archived equipment · existing bindings remain usable",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        WhipTextButton(onClick = {
                            when {
                                needsEquipment.isNotEmpty() -> {
                                    editingRoutineId = routine.id
                                    onEditorStateChange(true)
                                }
                                state.activeSession != null -> onOpenActiveWorkout()
                                else -> viewModel.startRoutine(routine.id, day.id)
                            }
                        }) {
                            Text(
                                when {
                                    needsEquipment.isNotEmpty() -> "Resolve Equipment for ${day.name}"
                                    state.activeSession != null -> "Open Active Workout"
                                    else -> "Start ${day.name} · ${quantityLabel(count, "exercise")}"
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class EditableRoutineExercise(
    val exerciseId: Long,
    val plannedSetsText: String = "",
    val copyPrevious: Boolean = true,
    val groupKey: String = "",
    val machineId: Long? = null,
    val equipmentBindingState: RoutineEquipmentBindingState = if (machineId == null) RoutineEquipmentBindingState.None else RoutineEquipmentBindingState.Resolved,
    val machineProfileUuidSnapshot: String? = null,
    val machineNameSnapshot: String = "",
    val machineLoadTypeSnapshot: MachineLoadType? = null,
    val machineUnitIdSnapshot: String = "",
    val machineLevelLabelSnapshot: String = "",
    val machineLoadInterpretationSnapshot: LoadInterpretation = LoadInterpretation.Total,
    val machineConfigurationGroupSnapshot: String = "",
    val machineConfigurationVersionSnapshot: Int = 1,
    val machineConfigurationSnapshot: String = "",
) : Serializable

private data class EditableRoutineDay(
    val key: Int,
    val name: String,
    val exercises: Map<Long, EditableRoutineExercise>,
    val exerciseOrder: List<Long> = exercises.keys.toList(),
) : Serializable

@Composable
private fun RoutineEditorDialog(
    modifier: Modifier = Modifier,
    exercises: List<Exercise>,
    machines: List<GymMachine>,
    initial: RoutineDraft?,
    preferredWeightUnitId: String,
    onDismiss: () -> Unit,
    onSave: (RoutineDraft) -> Unit,
) {
    val editorKey = "routine-${initial?.name ?: "new"}"
    var name by rememberSaveable(editorKey) { mutableStateOf(initial?.name.orEmpty()) }
    var notes by rememberSaveable(editorKey) { mutableStateOf(initial?.notes.orEmpty()) }
    var machineMenuExerciseId by rememberSaveable(editorKey) { mutableStateOf<Long?>(null) }
    var exerciseQuery by rememberSaveable(editorKey) { mutableStateOf("") }
    var selectedOnly by rememberSaveable(editorKey) { mutableStateOf(false) }
    var days by rememberSaveable(editorKey) {
        mutableStateOf(
            buildList {
            val source = initial?.days.orEmpty()
            if (source.isEmpty()) add(EditableRoutineDay(0, "Day A", emptyMap()))
            else source.forEachIndexed { index, day ->
                add(
                    EditableRoutineDay(
                        index,
                        day.name,
                        day.exercises.associate { routineExercise ->
                            val exerciseWeightUnit = exercises.firstOrNull { it.id == routineExercise.exerciseId }
                                ?.weightUnitId ?: preferredWeightUnitId
                            routineExercise.exerciseId to EditableRoutineExercise(
                                exerciseId = routineExercise.exerciseId,
                                plannedSetsText = routineExercise.plannedSets.joinToString("\n") { set ->
                                    listOfNotNull(
                                        set.machineLoadValue?.let(::editableNumber)
                                            ?: set.weight?.let { editableNumber(massFromKilograms(massToKilograms(it, set.weightUnitId), exerciseWeightUnit)) },
                                        set.reps?.toString(),
                                    ).joinToString(" x ")
                                },
                                copyPrevious = routineExercise.copyPreviousWorkout,
                                groupKey = routineExercise.groupKey.orEmpty(),
                                machineId = routineExercise.machineId,
                                equipmentBindingState = routineExercise.equipmentBindingState,
                                machineProfileUuidSnapshot = routineExercise.machineProfileUuidSnapshot,
                                machineNameSnapshot = routineExercise.machineNameSnapshot,
                                machineLoadTypeSnapshot = routineExercise.machineLoadTypeSnapshot,
                                machineUnitIdSnapshot = routineExercise.machineUnitIdSnapshot,
                                machineLevelLabelSnapshot = routineExercise.machineLevelLabelSnapshot,
                                machineLoadInterpretationSnapshot = routineExercise.machineLoadInterpretationSnapshot,
                                machineConfigurationGroupSnapshot = routineExercise.machineConfigurationGroupSnapshot,
                                machineConfigurationVersionSnapshot = routineExercise.machineConfigurationVersionSnapshot,
                                machineConfigurationSnapshot = routineExercise.machineConfigurationSnapshot,
                            )
                        },
                        day.exercises.map(RoutineExerciseDraft::exerciseId),
                    ),
                )
            }
            },
        )
    }
    fun updateDay(index: Int, transform: (EditableRoutineDay) -> EditableRoutineDay) {
        days = days.toMutableList().also { updated -> updated[index] = transform(updated[index]) }
    }
    val editorFingerprint = listOf(name, notes, days).joinToString("\u001f")
    val initialFingerprint by rememberSaveable(editorKey) { mutableStateOf(editorFingerprint) }
    var showDiscardConfirmation by rememberSaveable(editorKey) { mutableStateOf(false) }
    val requestDismiss = { if (editorFingerprint != initialFingerprint) showDiscardConfirmation = true else onDismiss() }
    BackHandler(enabled = !showDiscardConfirmation, onBack = requestDismiss)
    ProductivityEditorDialog(
        modifier = modifier,
        testTag = "legacy-routine-editor",
        onDismissRequest = requestDismiss,
        title = { Text("Create Routine") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { OutlinedTextField(name, { name = it }, label = { Text("Routine name *") }, modifier = Modifier.fillMaxWidth().testTag("routine-editor-name")) }
                item { OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    OutlinedTextField(exerciseQuery, { exerciseQuery = it }, label = { Text("Search exercises") }, modifier = Modifier.fillMaxWidth())
                    ToggleRow("Show selected exercises only", selectedOnly) { selectedOnly = it }
                }
                days.forEachIndexed { dayIndex, day ->
                    item(key = "day-${day.key}") {
                        Column {
                            OutlinedTextField(
                                day.name,
                                { value -> updateDay(dayIndex) { it.copy(name = value) } },
                                label = { Text("Day / section name") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            val visibleExercises = exercises.filter { exercise ->
                                exerciseMatchesQuery(exercise, exerciseQuery) && (!selectedOnly || exercise.id in day.exercises)
                            }.sortedWith(compareBy<Exercise> { day.exerciseOrder.indexOf(it.id).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE }.thenBy { it.name.lowercase() })
                            visibleExercises.forEach { exercise ->
                                val selectedExercise = day.exercises[exercise.id]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClickLabel = if (selectedExercise != null) "Remove ${exercise.name}" else "Add ${exercise.name}") {
                                            val selected = if (selectedExercise != null) day.exercises - exercise.id else day.exercises + (exercise.id to EditableRoutineExercise(exercise.id))
                                            updateDay(dayIndex) { current -> current.copy(exercises = selected, exerciseOrder = if (selectedExercise != null) current.exerciseOrder - exercise.id else current.exerciseOrder + exercise.id) }
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = selectedExercise != null,
                                        onCheckedChange = { checked ->
                                            val selected = if (checked) day.exercises + (exercise.id to EditableRoutineExercise(exercise.id)) else day.exercises - exercise.id
                                            updateDay(dayIndex) { current -> current.copy(exercises = selected, exerciseOrder = if (checked) current.exerciseOrder + exercise.id else current.exerciseOrder - exercise.id) }
                                        },
                                    )
                                    Text(exercise.name)
                                }
                                if (selectedExercise != null) {
                                    val orderIndex = day.exerciseOrder.indexOf(exercise.id)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        WhipTextButton(enabled = orderIndex > 0, onClick = {
                                            updateDay(dayIndex) { current ->
                                                val order = current.exerciseOrder.toMutableList()
                                                java.util.Collections.swap(order, orderIndex, orderIndex - 1)
                                                current.copy(exerciseOrder = order)
                                            }
                                        }) { Text("Move Up") }
                                        WhipTextButton(enabled = orderIndex in 0 until day.exerciseOrder.lastIndex, onClick = {
                                            updateDay(dayIndex) { current ->
                                                val order = current.exerciseOrder.toMutableList()
                                                java.util.Collections.swap(order, orderIndex, orderIndex + 1)
                                                current.copy(exerciseOrder = order)
                                            }
                                        }) { Text("Move Down") }
                                    }
                                    val compatibleMachines = machines.filter { candidate ->
                                        candidate.exerciseId == exercise.id &&
                                            (!candidate.archived || candidate.id == selectedExercise.machineId) &&
                                            (selectedExercise.equipmentBindingState != RoutineEquipmentBindingState.NeedsEquipment ||
                                                selectedExercise.machineLoadTypeSnapshot == null ||
                                                (candidate.loadType == selectedExercise.machineLoadTypeSnapshot &&
                                                    (candidate.loadType != MachineLoadType.Mass || selectedExercise.machineUnitIdSnapshot.isBlank() || candidate.unitId == selectedExercise.machineUnitIdSnapshot) &&
                                                    candidate.loadInterpretation == selectedExercise.machineLoadInterpretationSnapshot))
                                    }
                                    if (selectedExercise.equipmentBindingState == RoutineEquipmentBindingState.NeedsEquipment) {
                                        Text(
                                            "Needs equipment · formerly ${selectedExercise.machineNameSnapshot.ifBlank { "machine profile" }}" +
                                                " v${selectedExercise.machineConfigurationVersionSnapshot}",
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        if (compatibleMachines.isEmpty()) Text(
                                            "Create a compatible ${selectedExercise.machineLoadTypeSnapshot?.label.orEmpty()} profile for this exercise, or remove and re-add the exercise as free weight.",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    if (compatibleMachines.isNotEmpty()) {
                                        WhipOutlinedButton(
                                            onClick = { machineMenuExerciseId = exercise.id },
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(
                                                compatibleMachines.firstOrNull { it.id == selectedExercise.machineId }?.displayName
                                                    ?: if (selectedExercise.equipmentBindingState == RoutineEquipmentBindingState.NeedsEquipment) "Choose replacement equipment"
                                                    else "No Machine / Free Weights",
                                                modifier = Modifier.weight(1f),
                                            )
                                            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
                                        }
                                        DropdownMenu(
                                            expanded = machineMenuExerciseId == exercise.id,
                                            onDismissRequest = { machineMenuExerciseId = null },
                                        ) {
                                            if (selectedExercise.equipmentBindingState != RoutineEquipmentBindingState.NeedsEquipment) {
                                                DropdownMenuItem(
                                                    text = { Text("No Machine / Free Weights") },
                                                    onClick = {
                                                        updateDay(dayIndex) {
                                                            it.copy(exercises = it.exercises + (exercise.id to selectedExercise.copy(
                                                                machineId = null,
                                                                equipmentBindingState = RoutineEquipmentBindingState.None,
                                                                machineProfileUuidSnapshot = null,
                                                                machineNameSnapshot = "",
                                                                machineLoadTypeSnapshot = null,
                                                            )))
                                                        }
                                                        machineMenuExerciseId = null
                                                    },
                                                )
                                            }
                                            compatibleMachines.forEach { machine ->
                                                DropdownMenuItem(
                                                    text = { Text(machine.displayName) },
                                                    onClick = {
                                                        updateDay(dayIndex) {
                                                            it.copy(exercises = it.exercises + (exercise.id to selectedExercise.copy(
                                                                machineId = machine.id,
                                                                equipmentBindingState = RoutineEquipmentBindingState.Resolved,
                                                            )))
                                                        }
                                                        machineMenuExerciseId = null
                                                    },
                                                )
                                            }
                                        }
                                    }
                                    val selectedMachine = machines.firstOrNull { it.id == selectedExercise.machineId }
                                    OutlinedTextField(
                                        selectedExercise.plannedSetsText,
                                        { text -> updateDay(dayIndex) { it.copy(exercises = it.exercises + (exercise.id to selectedExercise.copy(plannedSetsText = text))) } },
                                        label = {
                                            Text(
                                                if (selectedMachine?.loadType == MachineLoadType.Level) {
                                                    "Planned sets (${selectedMachine.levelLabel}), one per line: setting x reps"
                                                } else {
                                                    "Planned sets (${unitSymbol(selectedMachine?.unitId ?: exercise.weightUnitId)}), one per line: weight x reps"
                                                },
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    if (selectedMachine?.availableLoads?.isNotEmpty() == true) {
                                        Text(
                                            "Available ${if (selectedMachine.loadType == MachineLoadType.Level) selectedMachine.levelLabel else unitSymbol(selectedMachine.unitId)}: ${machineLoadSummary(selectedMachine.availableLoads)}",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    ToggleRow("Copy previous set when no plan", selectedExercise.copyPrevious) { checked ->
                                        updateDay(dayIndex) { it.copy(exercises = it.exercises + (exercise.id to selectedExercise.copy(copyPrevious = checked))) }
                                    }
                                    OutlinedTextField(
                                        selectedExercise.groupKey,
                                        { text -> updateDay(dayIndex) { it.copy(exercises = it.exercises + (exercise.id to selectedExercise.copy(groupKey = text))) } },
                                        label = { Text("Superset/circuit group name (optional)") },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }
                    item(key = "duplicate-${day.key}") {
                        WhipTextButton(onClick = {
                            val key = (days.maxOfOrNull { it.key } ?: -1) + 1
                            days = days.toMutableList().also { list ->
                                list.add(dayIndex + 1, day.copy(key = key, name = "${day.name} copy"))
                            }
                        }) { Text("Duplicate ${day.name}") }
                    }
                }
                item {
                    WhipTextButton(onClick = {
                        val key = (days.maxOfOrNull { it.key } ?: -1) + 1
                        days = days + EditableRoutineDay(key, "Day ${('A'.code + key).toChar()}", emptyMap())
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Day / Section")
                    }
                }
            }
        },
        confirmButton = {
            WhipTextButton(
                enabled = name.isNotBlank() && days.all { it.name.isNotBlank() },
                onClick = {
                    onSave(
                        RoutineDraft(
                            name = name,
                            notes = notes,
                            days = days.map { day ->
                                RoutineDayDraft(
                                    name = day.name,
                                    exercises = day.exerciseOrder.mapNotNull(day.exercises::get).map { editable ->
                                        RoutineExerciseDraft(
                                            exerciseId = editable.exerciseId,
                                            groupKey = editable.groupKey.trim().ifBlank { null },
                                            copyPreviousWorkout = editable.copyPrevious,
                                            machineId = editable.machineId,
                                            equipmentBindingState = editable.equipmentBindingState,
                                            machineProfileUuidSnapshot = editable.machineProfileUuidSnapshot,
                                            machineNameSnapshot = editable.machineNameSnapshot,
                                            machineLoadTypeSnapshot = editable.machineLoadTypeSnapshot,
                                            machineUnitIdSnapshot = editable.machineUnitIdSnapshot,
                                            machineLevelLabelSnapshot = editable.machineLevelLabelSnapshot,
                                            machineLoadInterpretationSnapshot = editable.machineLoadInterpretationSnapshot,
                                            machineConfigurationGroupSnapshot = editable.machineConfigurationGroupSnapshot,
                                            machineConfigurationVersionSnapshot = editable.machineConfigurationVersionSnapshot,
                                            machineConfigurationSnapshot = editable.machineConfigurationSnapshot,
                                            plannedSets = editable.plannedSetsText.lines().mapNotNull { line ->
                                                val parts = line.lowercase().split('x').map(String::trim)
                                                val selectedMachine = machines.firstOrNull { it.id == editable.machineId }
                                                val selectedExercise = exercises.firstOrNull { it.id == editable.exerciseId }
                                                if (parts.all(String::isBlank)) null else WorkoutSetDraft(
                                                    weight = parts.getOrNull(0)?.toWhipDoubleOrNull()
                                                        .takeUnless { selectedMachine?.loadType == MachineLoadType.Level },
                                                    weightUnitId = selectedMachine?.unitId?.takeIf(String::isNotBlank)
                                                        ?: selectedExercise?.weightUnitId
                                                        ?: preferredWeightUnitId,
                                                    reps = parts.getOrNull(1)?.toIntOrNull(),
                                                    planned = true,
                                                    machineLoadValue = parts.getOrNull(0)?.toWhipDoubleOrNull()
                                                        .takeIf { selectedMachine != null },
                                                )
                                            },
                                        )
                                    },
                                )
                            },
                        ),
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = { WhipTextButton(onClick = requestDismiss) { Text("Cancel") } },
    )
    if (showDiscardConfirmation) {
        UnsavedChangesDialog("routine", { showDiscardConfirmation = false }, onDismiss, modifier)
    }
}

private data class GymChartSeries(
    val name: String,
    val points: List<GymGraphPoint>,
)

@Composable
private fun SharedGymLineChart(
    series: List<GymChartSeries>,
    unit: String,
    precision: Int,
    description: String,
    onPointSelected: (String, GymGraphPoint) -> Unit,
) {
    val allPoints = series.flatMap(GymChartSeries::points)
    if (allPoints.isEmpty()) return
    val minimum = allPoints.minOf { it.value }
    val maximum = allPoints.maxOf { it.value }
    val valueRange = (maximum - minimum).takeIf { it > 0.0 } ?: 1.0
    val firstDate = allPoints.minOf { it.date }
    val lastDate = allPoints.maxOf { it.date }
    val totalDays = java.time.temporal.ChronoUnit.DAYS.between(firstDate, lastDate).coerceAtLeast(1)
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.error,
    )
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val density = LocalDensity.current
    val leftInsetPx = with(density) { 20.dp.toPx() }
    val rightInsetPx = with(density) { 12.dp.toPx() }
    val topInsetPx = with(density) { 12.dp.toPx() }
    val bottomInsetPx = with(density) { 16.dp.toPx() }
    val chartWidth = maxOf(360, allPoints.map { it.date }.distinct().size * 58).dp

    Column(
        Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("High ${formatNumber(maximum, precision)} $unit", style = MaterialTheme.typography.labelSmall)
            Text("Low ${formatNumber(minimum, precision)} $unit", style = MaterialTheme.typography.labelSmall)
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            Canvas(
                modifier = Modifier
                    .width(chartWidth)
                    .height(220.dp)
                    .pointerInput(series, firstDate, lastDate) {
                        detectTapGestures { tap ->
                            val usableWidth = (size.width - leftInsetPx - rightInsetPx).coerceAtLeast(1f)
                            val usableHeight = (size.height - topInsetPx - bottomInsetPx).coerceAtLeast(1f)
                            val nearest = series.flatMap { chartSeries ->
                                chartSeries.points.map { point ->
                                    val day = java.time.temporal.ChronoUnit.DAYS.between(firstDate, point.date)
                                    val x = leftInsetPx + usableWidth * (day.toFloat() / totalDays.toFloat())
                                    val y = topInsetPx + usableHeight * (1f - ((point.value - minimum) / valueRange).toFloat())
                                    Triple(chartSeries.name, point, kotlin.math.hypot(tap.x - x, tap.y - y))
                                }
                            }.minByOrNull { it.third }
                            nearest?.takeIf { it.third <= with(density) { 32.dp.toPx() } }?.let {
                                onPointSelected(it.first, it.second)
                            }
                        }
                    },
            ) {
                val usableWidth = size.width - leftInsetPx - rightInsetPx
                val usableHeight = size.height - topInsetPx - bottomInsetPx
                repeat(5) { index ->
                    val y = topInsetPx + usableHeight * index / 4f
                    drawLine(gridColor, Offset(leftInsetPx, y), Offset(size.width - rightInsetPx, y), strokeWidth = 1.dp.toPx())
                }
                series.forEachIndexed { seriesIndex, chartSeries ->
                    val plotted = chartSeries.points.sortedBy { it.date }.map { point ->
                        val day = java.time.temporal.ChronoUnit.DAYS.between(firstDate, point.date)
                        Offset(
                            x = leftInsetPx + usableWidth * (day.toFloat() / totalDays.toFloat()),
                            y = topInsetPx + usableHeight * (1f - ((point.value - minimum) / valueRange).toFloat()),
                        )
                    }
                    plotted.zipWithNext().forEach { (start, end) ->
                        drawLine(colors[seriesIndex % colors.size], start, end, strokeWidth = 3.dp.toPx())
                    }
                    plotted.forEach { drawCircle(colors[seriesIndex % colors.size], 5.dp.toPx(), it) }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(firstDate.toString(), style = MaterialTheme.typography.labelSmall)
            Text(lastDate.toString(), style = MaterialTheme.typography.labelSmall)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            series.forEachIndexed { index, item ->
                Text("● ${item.name}", color = colors[index % colors.size], style = MaterialTheme.typography.labelMedium)
            }
        }
        Text("Chart Points", style = MaterialTheme.typography.labelMedium)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            series.flatMap { chartSeries -> chartSeries.points.takeLast(12).map { chartSeries.name to it } }.forEach { (name, point) ->
                WhipFilterChip(
                    selected = false,
                    onClick = { onPointSelected(name, point) },
                    label = { Text("$name · ${point.date} · ${formatNumber(point.value, precision)} $unit") },
                )
            }
        }
    }
}

private fun chartDescription(
    exerciseName: String,
    metric: String,
    points: List<GymGraphPoint>,
    unit: String,
): String {
    if (points.isEmpty()) return "$exerciseName $metric chart with no data"
    val first = points.first()
    val latest = points.last()
    val minimum = points.minOf { it.value }
    val maximum = points.maxOf { it.value }
    val trend = when {
        latest.value > first.value -> "increased"
        latest.value < first.value -> "decreased"
        else -> "unchanged"
    }
    return "$exerciseName $metric chart. ${points.size} points from ${first.date} to ${latest.date}. " +
        "Range ${formatNumber(minimum, 2)} to ${formatNumber(maximum, 2)} $unit. " +
        "Latest ${formatNumber(latest.value, 2)} $unit; $trend from the first point."
}

@Composable
internal fun ExerciseEditorDialog(
    modifier: Modifier = Modifier,
    exercise: Exercise?,
    initialName: String = "",
    categories: List<ExerciseCategory>,
    selectedCategoryIds: Set<Long>,
    defaultWeightUnit: String,
    defaultRestSeconds: Int,
    defaultFormula: EstimatedOneRepMaxFormula,
    platePresets: List<PlatePreset>,
    powerMode: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (ExerciseDraft) -> Unit,
    saving: Boolean = false,
) {
    val editorKey = "exercise-${exercise?.id ?: "new"}-${if (exercise == null) initialName else "existing"}"
    var name by rememberSaveable(editorKey) { mutableStateOf(exercise?.name ?: initialName) }
    var trackingType by rememberSaveable(editorKey) { mutableStateOf(exercise?.trackingType ?: ExerciseTrackingType.WeightReps) }
    var notes by rememberSaveable(editorKey) { mutableStateOf(exercise?.notes.orEmpty()) }
    var equipment by rememberSaveable(editorKey) { mutableStateOf(exercise?.equipment.orEmpty()) }
    var primaryMuscles by rememberSaveable(editorKey) { mutableStateOf(exercise?.primaryMuscles.orEmpty()) }
    var secondaryMuscles by rememberSaveable(editorKey) { mutableStateOf(exercise?.secondaryMuscles.orEmpty()) }
    val initialWeightUnit = exercise?.weightUnitId ?: defaultWeightUnit
    var weightUnit by rememberSaveable(editorKey) { mutableStateOf(initialWeightUnit) }
    var restSeconds by rememberSaveable(editorKey) { mutableStateOf(exercise?.defaultRestSeconds?.toString() ?: defaultRestSeconds.toString()) }
    var weightIncrement by rememberSaveable(editorKey) {
        mutableStateOf(
            exercise?.weightIncrement
                ?.let(::editableNumber)
                ?: editableNumber(standardWeightEquipment(initialWeightUnit).increment),
        )
    }
    var repetitionIncrement by rememberSaveable(editorKey) { mutableStateOf(exercise?.repetitionIncrement?.toString() ?: "1") }
    var graphMetric by rememberSaveable(editorKey) { mutableStateOf(exercise?.defaultGraphMetric ?: GymGraphMetric.EstimatedOneRepMax.name) }
    var formula by rememberSaveable(editorKey) { mutableStateOf(exercise?.oneRepMaxFormula ?: defaultFormula) }
    var barWeight by rememberSaveable(editorKey) {
        mutableStateOf(
            exercise?.barWeightKg?.let { editableNumber(massFromKilograms(it, initialWeightUnit)) }
                ?: editableNumber(standardWeightEquipment(initialWeightUnit).barWeight),
        )
    }
    var plates by rememberSaveable(editorKey) {
        mutableStateOf(
            exercise?.availablePlatesKg?.joinToString(",") { editableNumber(massFromKilograms(it, initialWeightUnit)) }
                ?: standardWeightEquipment(initialWeightUnit).plates.joinToString(",", transform = ::editableNumber),
        )
    }
    var loadInterpretation by rememberSaveable(editorKey) {
        mutableStateOf(exercise?.loadInterpretation ?: LoadInterpretation.Total)
    }
    var bodyweightPolicy by rememberSaveable(editorKey) { mutableStateOf(exercise?.bodyweightLoadPolicy ?: BodyweightLoadPolicy.ExternalWeightOnly) }
    var effectiveBodyweight by rememberSaveable(editorKey) { mutableStateOf(exercise?.effectiveBodyweightPercent?.let(::editableNumber) ?: "100") }
    var showRpe by rememberSaveable(editorKey) { mutableStateOf(exercise?.showRpe) }
    var showRir by rememberSaveable(editorKey) { mutableStateOf(exercise?.showRir) }
    var showTempo by rememberSaveable(editorKey) { mutableStateOf(exercise?.showTempo) }
    var categoryIds by rememberSaveable(editorKey) { mutableStateOf(selectedCategoryIds) }
    var includeVolume by rememberSaveable(editorKey) { mutableStateOf(exercise?.includeInVolume ?: true) }
    var includePr by rememberSaveable(editorKey) { mutableStateOf(exercise?.includeInPersonalRecords ?: true) }
    var showAdvanced by rememberSaveable(editorKey) { mutableStateOf(powerMode) }
    var pendingWeightUnit by rememberSaveable(editorKey) { mutableStateOf<String?>(null) }
    var pendingLoadInterpretation by rememberSaveable(editorKey) { mutableStateOf<LoadInterpretation?>(null) }
    fun convertDefaultsToUnit(selected: String) {
        val current = WeightEquipmentSetup(
            increment = weightIncrement.toWhipDoubleOrNull()
                ?: standardWeightEquipment(weightUnit).increment,
            barWeight = barWeight.toWhipDoubleOrNull()
                ?: standardWeightEquipment(weightUnit).barWeight,
            plates = plates.split(',').mapNotNull { it.trim().toWhipDoubleOrNull() },
        )
        val converted = convertWeightEquipmentSetup(current, weightUnit, selected)
        weightIncrement = editableNumber(converted.increment)
        barWeight = editableNumber(converted.barWeight)
        plates = converted.plates.joinToString(",", transform = ::editableNumber)
        weightUnit = selected
    }
    fun keepDefaultsInUnit(selected: String) { weightUnit = selected }
    fun resetDefaultsForUnit(selected: String) {
        val setup = standardWeightEquipment(selected)
        weightUnit = selected
        weightIncrement = editableNumber(setup.increment)
        barWeight = editableNumber(setup.barWeight)
        plates = setup.plates.joinToString(",", transform = ::editableNumber)
    }
    val editorFingerprint = listOf(
        name, trackingType, notes, equipment, primaryMuscles, secondaryMuscles, weightUnit,
        restSeconds, weightIncrement, repetitionIncrement, graphMetric, formula, barWeight,
        plates, loadInterpretation, bodyweightPolicy, effectiveBodyweight, showRpe, showRir,
        showTempo, categoryIds.sorted(), includeVolume, includePr,
    ).joinToString("\u001f")
    val initialFingerprint by rememberSaveable(editorKey) { mutableStateOf(editorFingerprint) }
    var showDiscardConfirmation by rememberSaveable(editorKey) { mutableStateOf(false) }
    val requestDismiss = { if (editorFingerprint != initialFingerprint) showDiscardConfirmation = true else onDismiss() }
    BackHandler(enabled = !showDiscardConfirmation, onBack = requestDismiss)
    ProductivityEditorDialog(
        modifier = modifier,
        testTag = "exercise-editor-surface",
        onDismissRequest = { if (!saving) requestDismiss() },
        title = { Text(if (exercise == null) "Create Exercise" else "Edit Exercise") },
        text = {
            LazyColumn(
                modifier = Modifier.testTag("exercise-editor-list"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    OutlinedTextField(name, { name = it }, label = { Text("Name *") }, modifier = Modifier.fillMaxWidth().testTag("exercise-editor-name"))
                }
                item {
                    GymEnumDropdown("Tracking type", ExerciseTrackingType.entries, trackingType, ExerciseTrackingType::label) {
                        trackingType = it
                    }
                    DependentSettingsNotice(
                        message = when (trackingType) {
                            ExerciseTrackingType.WeightReps -> "Workout sets will ask for weight and repetitions."
                            ExerciseTrackingType.BodyweightReps -> "Workout sets will ask for repetitions and use the bodyweight rule below."
                            ExerciseTrackingType.AssistedBodyweightReps -> "Workout sets will ask for assistance and repetitions; Bodyweight Load controls how assistance is interpreted."
                            ExerciseTrackingType.DistanceDuration -> "Workout sets will ask for distance and duration."
                            ExerciseTrackingType.RepsOnly -> "Workout sets will ask for repetitions only."
                            ExerciseTrackingType.WeightOnly -> "Workout sets will ask for weight only."
                            ExerciseTrackingType.WeightDuration -> "Workout sets will ask for weight and duration."
                            ExerciseTrackingType.RepsDuration -> "Workout sets will ask for repetitions and duration."
                            ExerciseTrackingType.DistanceOnly -> "Workout sets will ask for distance only."
                            ExerciseTrackingType.DurationOnly -> "Workout sets will ask for duration only."
                        },
                        testTag = "exercise-tracking-consequence",
                    )
                }
                if (trackingType in setOf(ExerciseTrackingType.BodyweightReps, ExerciseTrackingType.AssistedBodyweightReps)) {
                    item {
                        GymEnumDropdown("Bodyweight load", BodyweightLoadPolicy.entries, bodyweightPolicy, { it.name.replace(Regex("([a-z])([A-Z])"), "$1 $2") }) { bodyweightPolicy = it }
                        if (bodyweightPolicy == BodyweightLoadPolicy.EffectiveBodyweightPercentage || trackingType == ExerciseTrackingType.AssistedBodyweightReps) {
                            NumberField(effectiveBodyweight, { effectiveBodyweight = it }, "Effective bodyweight percent")
                        }
                    }
                }
                item { OutlinedTextField(notes, { notes = it }, label = { Text("Notes / form cues") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    DisclosureButton(
                        label = "Advanced options",
                        expanded = showAdvanced,
                        onClick = { showAdvanced = !showAdvanced },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (showAdvanced) {
                    item { OutlinedTextField(equipment, { equipment = it }, label = { Text("Equipment") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(primaryMuscles, { primaryMuscles = it }, label = { Text("Primary muscles / tags") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(secondaryMuscles, { secondaryMuscles = it }, label = { Text("Secondary muscles / tags") }, modifier = Modifier.fillMaxWidth()) }
                    if (categories.isNotEmpty()) item {
                        Text("User Categories", style = MaterialTheme.typography.labelMedium)
                        categories.forEach { category ->
                            Row(Modifier.fillMaxWidth().clickable(onClickLabel = "Toggle ${category.name}") { categoryIds = if (category.id in categoryIds) categoryIds - category.id else categoryIds + category.id }, verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = category.id in categoryIds, onCheckedChange = { checked -> categoryIds = if (checked) categoryIds + category.id else categoryIds - category.id })
                                Text("${category.name} · ${category.kind}")
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WhipFilterChip(selected = weightUnit == "kilogram", onClick = { if (weightUnit != "kilogram") pendingWeightUnit = "kilogram" }, label = { Text("kg") })
                            WhipFilterChip(selected = weightUnit == "pound", onClick = { if (weightUnit != "pound") pendingWeightUnit = "pound" }, label = { Text("lb") })
                        }
                        Text(
                            "Exercise-specific unit. Switching applies common equipment defaults (45 lb bar and standard lb plates, or 20 kg and standard metric plates); saved history is not rewritten.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    item { ResponsiveFieldPair(
                        first = { field -> NumberField(weightIncrement, { weightIncrement = it }, "Weight increment (${unitSymbol(weightUnit)})", modifier = field.testTag("exercise-weight-increment")) },
                        second = { field -> NumberField(repetitionIncrement, { repetitionIncrement = it }, "Rep increment", integer = true, modifier = field) },
                    ) }
                    item {
                        GymEnumDropdown(
                            "What one weight entry means",
                            LoadInterpretation.entries,
                            loadInterpretation,
                            LoadInterpretation::label,
                        ) { selected -> if (selected != loadInterpretation) pendingLoadInterpretation = selected }
                        Text(
                            when (loadInterpretation) {
                                LoadInterpretation.Total -> "Enter the full external load, such as a loaded barbell."
                                LoadInterpretation.PerHand -> "Enter one dumbbell or handle; Whip doubles it for total volume while preserving what you typed."
                                LoadInterpretation.PerSide -> "Enter plates on one side; Whip calculates bar/base + both sides."
                                LoadInterpretation.AddedLoad -> "Enter load added to bodyweight. Negative values may represent band or counterweight assistance."
                                LoadInterpretation.BodyweightPlusExternal -> "Enter external load; Whip adds the recorded bodyweight."
                                LoadInterpretation.BodyweightPercentage -> "Enter external load; Whip adds the configured effective percentage of bodyweight."
                                LoadInterpretation.AssistedSubtraction -> "Enter assistance; Whip subtracts it from effective bodyweight."
                                LoadInterpretation.MachineDisplayedMass -> "Enter the mass printed on the machine; configuration determines effective resistance."
                                LoadInterpretation.OrdinalSetting -> "Track a numbered setting without treating it as mass unless a mapping is supplied."
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    item {
                        OutlinedTextField(
                            restSeconds,
                            { restSeconds = it },
                            label = { Text("Default rest seconds") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item { GymEnumDropdown("Default graph", GymGraphMetric.entries, GymGraphMetric.entries.firstOrNull { it.name == graphMetric } ?: GymGraphMetric.EstimatedOneRepMax, { it.label }) { graphMetric = it.name } }
                    item { GymEnumDropdown("Estimated 1RM formula", EstimatedOneRepMaxFormula.entries, formula, { it.name }) { formula = it } }
                    item { ResponsiveFieldPair(
                        first = { field -> NumberField(barWeight, { barWeight = it }, "Bar weight (${unitSymbol(weightUnit)})", modifier = field.testTag("exercise-bar-weight")) },
                        second = { field -> OutlinedTextField(plates, { plates = it }, label = { Text("Plates (${unitSymbol(weightUnit)})") }, modifier = field.testTag("exercise-plates")) },
                    ) }
                    if (platePresets.isNotEmpty()) item {
                        Text("Apply Plate Preset", style = MaterialTheme.typography.labelMedium)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            platePresets.forEach { preset ->
                                WhipFilterChip(
                                    selected = false,
                                    onClick = {
                                        weightUnit = preset.unitId
                                        barWeight = editableNumber(preset.barWeight)
                                        plates = preset.plates.joinToString(",", transform = ::editableNumber)
                                    },
                                    label = { Text(preset.name) },
                                )
                            }
                        }
                    }
                    item {
                        val effortField = when {
                            showRpe == true -> "RPE"
                            showRir == true -> "RIR"
                            showRpe == null && showRir == null -> "Use Global Setting"
                            else -> "Off"
                        }
                        GymEnumDropdown(
                            "Effort field",
                            listOf("Use Global Setting", "Off", "RPE", "RIR"),
                            effortField,
                            { it },
                        ) { selected ->
                            when (selected) {
                                "RPE" -> { showRpe = true; showRir = false }
                                "RIR" -> { showRpe = false; showRir = true }
                                "Off" -> { showRpe = false; showRir = false }
                                else -> { showRpe = null; showRir = null }
                            }
                        }
                        GymEnumDropdown("Tempo field", listOf<Boolean?>(null, true, false), showTempo, ::fieldVisibilityLabel) { showTempo = it }
                    }
                    item { ToggleRow("Include in volume", includeVolume) { includeVolume = it } }
                    item { ToggleRow("Include in personal records", includePr) { includePr = it } }
                }
            }
        },
        confirmButton = {
            WhipTextButton(
                enabled = name.isNotBlank() && !saving,
                onClick = {
                    onSave(
                        ExerciseDraft(
                            name = name,
                            trackingType = trackingType,
                            notes = notes,
                            equipment = equipment,
                            primaryMuscles = primaryMuscles,
                            secondaryMuscles = secondaryMuscles,
                            weightUnitId = weightUnit,
                            weightIncrement = weightIncrement.toWhipDoubleOrNull() ?: 2.5,
                            repetitionIncrement = repetitionIncrement.toIntOrNull() ?: 1,
                            defaultRestSeconds = restSeconds.toIntOrNull(),
                            defaultGraphMetric = graphMetric,
                            oneRepMaxFormula = formula,
                            barWeightKg = barWeight.toWhipDoubleOrNull()?.let { massToKilograms(it, weightUnit) },
                            availablePlatesKg = plates.split(',').mapNotNull { it.trim().toWhipDoubleOrNull() }.map { massToKilograms(it, weightUnit) },
                            includeInVolume = includeVolume,
                            includeInPersonalRecords = includePr,
                            bodyweightLoadPolicy = bodyweightPolicy,
                            effectiveBodyweightPercent = effectiveBodyweight.toWhipDoubleOrNull() ?: 100.0,
                            showRpe = showRpe,
                            showRir = showRir,
                            showTempo = showTempo,
                            categoryIds = categoryIds,
                            loadInterpretation = loadInterpretation,
                        ),
                    )
                },
            ) { Text(if (saving) "Saving…" else "Save") }
        },
        dismissButton = { WhipTextButton(onClick = requestDismiss, enabled = !saving) { Text("Cancel") } },
    )
    pendingWeightUnit?.let { selected ->
        val currentSetup = WeightEquipmentSetup(
            increment = weightIncrement.toWhipDoubleOrNull() ?: standardWeightEquipment(weightUnit).increment,
            barWeight = barWeight.toWhipDoubleOrNull() ?: standardWeightEquipment(weightUnit).barWeight,
            plates = plates.split(',').mapNotNull { it.trim().toWhipDoubleOrNull() },
        )
        val convertedSetup = convertWeightEquipmentSetup(currentSetup, weightUnit, selected)
        DefaultsMeaningChangeDialog(
            modifier = modifier,
            title = "Change Exercise Unit to ${unitSymbol(selected)}?",
            explanation = "This affects future set entry only. Convert changes increment ${editableNumber(currentSetup.increment)} ${unitSymbol(weightUnit)} → ${editableNumber(convertedSetup.increment)} ${unitSymbol(selected)} and bar ${editableNumber(currentSetup.barWeight)} → ${editableNumber(convertedSetup.barWeight)}; plate defaults are converted too. Keep changes only the unit label. Reset uses the standard ${unitSymbol(selected)} bar and plates. Logged sets keep exactly what you entered.",
            onConvert = { convertDefaultsToUnit(selected); pendingWeightUnit = null },
            onKeep = { keepDefaultsInUnit(selected); pendingWeightUnit = null },
            onReset = { resetDefaultsForUnit(selected); pendingWeightUnit = null },
            onCancel = { pendingWeightUnit = null },
        )
    }
    pendingLoadInterpretation?.let { selected ->
        DefaultsMeaningChangeDialog(
            modifier = modifier,
            title = "Change Entry Meaning to ${selected.label.uiTitleCase()}?",
            explanation = "This changes future sets only. Existing workout placements keep the old meaning. Convert scales the numeric equipment defaults to preserve approximately the same bilateral resistance.",
            onConvert = {
                val oldMultiplier = com.whip.app.domain.loadInterpretationMultiplier(loadInterpretation)
                val newMultiplier = com.whip.app.domain.loadInterpretationMultiplier(selected)
                val factor = oldMultiplier / newMultiplier
                weightIncrement = weightIncrement.toWhipDoubleOrNull()?.let { editableNumber(it * factor) } ?: weightIncrement
                barWeight = barWeight.toWhipDoubleOrNull()?.let { editableNumber(it * factor) } ?: barWeight
                plates = plates.split(',').joinToString(",") { raw -> raw.trim().toWhipDoubleOrNull()?.let { editableNumber(it * factor) } ?: raw.trim() }
                loadInterpretation = selected
                pendingLoadInterpretation = null
            },
            onKeep = { loadInterpretation = selected; pendingLoadInterpretation = null },
            onReset = { loadInterpretation = selected; resetDefaultsForUnit(weightUnit); pendingLoadInterpretation = null },
            onCancel = { pendingLoadInterpretation = null },
        )
    }
    if (showDiscardConfirmation) {
        UnsavedChangesDialog("exercise", { showDiscardConfirmation = false }, onDismiss, modifier)
    }
}

@Composable
private fun DefaultsMeaningChangeDialog(
    modifier: Modifier = Modifier,
    title: String,
    explanation: String,
    onConvert: () -> Unit,
    onKeep: () -> Unit,
    onReset: () -> Unit,
    onCancel: () -> Unit,
) {
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(explanation)
                WhipButton(onClick = onConvert, modifier = Modifier.fillMaxWidth()) { Text("Convert Default Values") }
                WhipOutlinedButton(onClick = onKeep, modifier = Modifier.fillMaxWidth()) { Text("Keep Entered Numbers") }
                WhipOutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text("Reset to Equipment Standard") }
            }
        },
        confirmButton = {},
        dismissButton = { WhipTextButton(onClick = onCancel) { Text("Cancel") } },
    )
}

@Composable
private fun WorkoutSetEditorDialog(
    modifier: Modifier = Modifier,
    set: WorkoutSet,
    exercise: Exercise,
    workoutExercise: WorkoutExercise,
    machine: GymMachine?,
    preferredWeightUnitId: String,
    preferredDistanceUnitId: String,
    showRpe: Boolean,
    showRir: Boolean,
    showTempo: Boolean,
    onDismiss: () -> Unit,
    onSave: (WorkoutSetDraft) -> Unit,
) {
    val policyExercise = workoutExercise.applyPolicySnapshot(exercise)
    val machineType = workoutExercise.machineLoadTypeSnapshot
    val entryWeightUnitId = set.enteredWeightUnitId ?: when (machineType) {
        MachineLoadType.Mass -> workoutExercise.machineUnitIdSnapshot
        MachineLoadType.Level -> preferredWeightUnitId
        null -> policyExercise.weightUnitId.ifBlank { preferredWeightUnitId }
    }
    val entryDistanceUnitId = set.enteredDistanceUnitId ?: preferredDistanceUnitId
    val weightSymbol = unitSymbol(entryWeightUnitId)
    val loadInterpretation = workoutExercise.loadInterpretationSnapshot
    val distanceSymbol = unitSymbol(entryDistanceUnitId)
    val editorKey = "workout-set-${set.id}"
    var weight by rememberSaveable(editorKey) {
        mutableStateOf(
            set.enteredWeight?.let(::editableNumber)
                ?: set.canonicalWeightKg?.let { editableNumber(massFromKilograms(it, entryWeightUnitId)) }
                ?: "",
        )
    }
    var machineSetting by rememberSaveable(editorKey) { mutableStateOf(set.machineLoadValue?.let(::editableNumber).orEmpty()) }
    var reps by rememberSaveable(editorKey) { mutableStateOf(set.repetitions?.toString().orEmpty()) }
    var distance by rememberSaveable(editorKey) {
        mutableStateOf(
            set.enteredDistance?.let(::editableNumber)
                ?: set.canonicalDistanceMetres?.let { editableNumber(distanceFromMetres(it, entryDistanceUnitId)) }
                ?: "",
        )
    }
    var duration by rememberSaveable(editorKey) { mutableStateOf(set.durationSeconds?.toString().orEmpty()) }
    var bodyweight by rememberSaveable(editorKey) {
        mutableStateOf(set.bodyweightKg?.let { editableNumber(massFromKilograms(it, entryWeightUnitId)) }.orEmpty())
    }
    var note by rememberSaveable(editorKey) { mutableStateOf(set.note) }
    var rpe by rememberSaveable(editorKey) { mutableStateOf(set.rpe?.let(::editableNumber).orEmpty()) }
    var rir by rememberSaveable(editorKey) { mutableStateOf(set.rir?.let(::editableNumber).orEmpty()) }
    var tempo by rememberSaveable(editorKey) { mutableStateOf(set.tempo) }
    var rest by rememberSaveable(editorKey) { mutableStateOf(set.restSeconds?.toString().orEmpty()) }
    var completed by rememberSaveable(editorKey) { mutableStateOf(set.completed) }
    var planned by rememberSaveable(editorKey) { mutableStateOf(set.planned) }
    var classification by rememberSaveable(editorKey) { mutableStateOf(set.classification) }
    var unilateral by rememberSaveable(editorKey) { mutableStateOf(set.unilateral) }
    val needsWeight = policyExercise.trackingType in setOf(
        ExerciseTrackingType.WeightReps,
        ExerciseTrackingType.BodyweightReps,
        ExerciseTrackingType.AssistedBodyweightReps,
        ExerciseTrackingType.WeightOnly,
        ExerciseTrackingType.WeightDuration,
    )
    val needsReps = policyExercise.trackingType in setOf(
        ExerciseTrackingType.WeightReps,
        ExerciseTrackingType.BodyweightReps,
        ExerciseTrackingType.AssistedBodyweightReps,
        ExerciseTrackingType.RepsOnly,
        ExerciseTrackingType.RepsDuration,
    )
    val needsDistance = policyExercise.trackingType in setOf(
        ExerciseTrackingType.DistanceDuration,
        ExerciseTrackingType.DistanceOnly,
    )
    val needsDuration = policyExercise.trackingType in setOf(
        ExerciseTrackingType.DistanceDuration,
        ExerciseTrackingType.WeightDuration,
        ExerciseTrackingType.RepsDuration,
        ExerciseTrackingType.DurationOnly,
    )
    val machineValues = machine?.availableLoads.orEmpty()
    val machineIncrement = compactNumericSequence(machineValues).increment
        ?: standardMachineSequence(machineType ?: MachineLoadType.Mass, entryWeightUnitId).increment
    val pendingDraft = WorkoutSetDraft(
        weight = weight.toWhipDoubleOrNull().takeUnless { machineType == MachineLoadType.Level },
        weightUnitId = entryWeightUnitId,
        reps = reps.toIntOrNull(),
        distance = distance.toWhipDoubleOrNull(),
        distanceUnitId = entryDistanceUnitId,
        durationSeconds = duration.toLongOrNull(),
        bodyweightKg = bodyweight.toWhipDoubleOrNull()?.let { massToKilograms(it, entryWeightUnitId) },
        planned = planned,
        completed = completed,
        classification = classification,
        note = note,
        rpe = rpe.toWhipDoubleOrNull(),
        rir = rir.toWhipDoubleOrNull(),
        tempo = tempo,
        restSeconds = rest.toIntOrNull(),
        machineLoadValue = when (machineType) {
            MachineLoadType.Level -> machineSetting.toWhipDoubleOrNull()
            MachineLoadType.Mass -> weight.toWhipDoubleOrNull()
            null -> null
        },
        unilateral = unilateral,
    )
    val validationMessage = runCatching {
        validateWorkoutSetDraft(pendingDraft, policyExercise.trackingType, machineType, loadInterpretation)
    }.exceptionOrNull()?.message
    val normalizedLoadKg = canonicalResistanceKg(
        enteredValue = weight.toWhipDoubleOrNull(),
        enteredUnitId = entryWeightUnitId,
        machineSetting = machineSetting.toWhipDoubleOrNull().takeIf { machineType == MachineLoadType.Level },
        interpretation = loadInterpretation,
        baseLoadKg = workoutExercise.baseLoadKgSnapshot,
        addOnPlateKg = workoutExercise.machineAddOnPlateKgSnapshot,
        massMappingKg = workoutExercise.machineMassMappingKgSnapshot,
        stackMode = workoutExercise.machineStackModeSnapshot,
        pulleyRatio = workoutExercise.machinePulleyRatioSnapshot,
        unilateral = unilateral,
    )
    ProductivityEditorDialog(
        modifier = modifier,
        testTag = "workout-set-editor",
        onDismissRequest = onDismiss,
        title = { Text("Edit Set · ${exercise.name}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (needsWeight && machineType != MachineLoadType.Level) item {
                    SteppedNumberField(
                        weight,
                        { value -> weight = value; if (machineType == MachineLoadType.Mass) machineSetting = value },
                        when {
                            policyExercise.trackingType == ExerciseTrackingType.AssistedBodyweightReps -> "Assistance ($weightSymbol)"
                            loadInterpretation == LoadInterpretation.PerHand -> "Load per hand ($weightSymbol)"
                            loadInterpretation == LoadInterpretation.PerSide -> "Load per side ($weightSymbol)"
                            loadInterpretation == LoadInterpretation.AddedLoad -> "Added load ($weightSymbol)"
                            machineType == MachineLoadType.Mass -> "Total stack load ($weightSymbol)"
                            else -> "Total weight ($weightSymbol)"
                        },
                        increment = if (machineType == MachineLoadType.Mass) machineIncrement else exercise.weightIncrement,
                        allowedValues = machineValues.takeIf { machineType == MachineLoadType.Mass }.orEmpty(),
                    )
                    normalizedLoadKg?.let { totalKg ->
                        if (loadInterpretation in setOf(LoadInterpretation.PerHand, LoadInterpretation.PerSide)) {
                            Text(
                                "Calculated total: ${editableNumber(massFromKilograms(totalKg, entryWeightUnitId))} $weightSymbol" +
                                    if (loadInterpretation == LoadInterpretation.PerSide && workoutExercise.baseLoadKgSnapshot != null) {
                                        " including ${editableNumber(massFromKilograms(workoutExercise.baseLoadKgSnapshot, entryWeightUnitId))} $weightSymbol base"
                                    } else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                if (needsWeight && machineType == MachineLoadType.Level) item {
                    SteppedNumberField(
                        machineSetting,
                        { machineSetting = it },
                        "Machine ${workoutExercise.machineLevelLabelSnapshot.ifBlank { "level" }}",
                        increment = machineIncrement,
                        allowedValues = machineValues,
                    )
                }
                if (workoutExercise.loadInterpretationSnapshot in setOf(LoadInterpretation.PerHand, LoadInterpretation.PerSide) ||
                    workoutExercise.machineStackModeSnapshot == MachineStackMode.DualIndependent) item {
                    ToggleRow("This set was performed on one side / limb", unilateral) { unilateral = it }
                    Text("Unilateral sets use one side of a per-hand, per-side, or independent-stack load instead of silently doubling it.", style = MaterialTheme.typography.bodySmall)
                }
                if (needsWeight && machine != null && machine.availableLoads.isNotEmpty()) item {
                    Text("Machine values · ${machineLoadSummary(machine.availableLoads)}", style = MaterialTheme.typography.labelMedium)
                    if (machine.availableLoads.size <= 12) FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        machine.availableLoads.sorted().forEach { load ->
                            WhipFilterChip(
                                selected = (if (machineType == MachineLoadType.Level) machineSetting else weight).toWhipDoubleOrNull() == load,
                                onClick = {
                                    val value = editableNumber(load)
                                    machineSetting = value
                                    if (machineType == MachineLoadType.Mass) weight = value
                                },
                                label = { Text(editableNumber(load)) },
                            )
                        }
                    } else {
                        Text("Use −/+ to move through the configured stack; direct entry remains available.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (needsReps) item {
                    SteppedNumberField(
                        reps,
                        { reps = it },
                        "Repetitions",
                        increment = exercise.repetitionIncrement.toDouble(),
                        integer = true,
                    )
                }
                if (needsDistance) item { NumberField(distance, { distance = it }, "Distance ($distanceSymbol)") }
                if (needsDuration) item { NumberField(duration, { duration = it }, "Duration (seconds)", integer = true) }
                if (exercise.trackingType in setOf(ExerciseTrackingType.BodyweightReps, ExerciseTrackingType.AssistedBodyweightReps)) {
                    item { NumberField(bodyweight, { bodyweight = it }, "Bodyweight ($weightSymbol)") }
                }
                item {
                    GymEnumDropdown(
                        "Set classification",
                        WorkoutSetClassification.entries,
                        classification,
                        WorkoutSetClassification::uiLabel,
                    ) { classification = it }
                }
                item { ToggleRow("Planned set", planned) { planned = it } }
                item { ToggleRow("Completed", completed) { completed = it } }
                if (showRpe) item { NumberField(rpe, { rpe = it }, "RPE (0–10)") }
                if (showRir) item { NumberField(rir, { rir = it }, "RIR") }
                if (showTempo) item { OutlinedTextField(tempo, { tempo = it }, label = { Text("Tempo") }, modifier = Modifier.fillMaxWidth()) }
                item { NumberField(rest, { rest = it }, "Rest seconds", integer = true) }
                item { OutlinedTextField(note, { note = it }, label = { Text("Set note") }, modifier = Modifier.fillMaxWidth()) }
                validationMessage?.let { message ->
                    item { Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            }
        },
        confirmButton = {
            WhipTextButton(enabled = validationMessage == null, onClick = { onSave(pendingDraft) }) { Text("Save") }
        },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun NumberField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, integer: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun SteppedNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    increment: Double,
    modifier: Modifier = Modifier,
    allowedValues: List<Double> = emptyList(),
    integer: Boolean = false,
    actionSubject: String = label,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    val decrementDescription = if (allowedValues.isEmpty()) "by ${editableNumber(increment)}" else "to the previous allowed value"
    val incrementDescription = if (allowedValues.isEmpty()) "by ${editableNumber(increment)}" else "to the next allowed value"
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = supportingText?.let { message -> { Text(message) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal,
        ),
        leadingIcon = {
            IconButton(
                onClick = {
                    onValueChange(editableNumber(steppedNumericValue(value, -1, increment, allowedValues)))
                },
                modifier = Modifier.size(48.dp).semantics { contentDescription = "Decrease $actionSubject $decrementDescription" },
            ) {
                Icon(Icons.Outlined.Remove, contentDescription = null)
            }
        },
        trailingIcon = {
            IconButton(
                onClick = {
                    onValueChange(editableNumber(steppedNumericValue(value, 1, increment, allowedValues)))
                },
                modifier = Modifier.size(48.dp).semantics { contentDescription = "Increase $actionSubject $incrementDescription" },
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
        },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun ExercisePickerDialog(
    modifier: Modifier = Modifier,
    exercises: List<Exercise>,
    preferredIds: List<Long> = emptyList(),
    onDismiss: () -> Unit,
    onPick: (Exercise) -> Unit,
    onCreate: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text("Add Exercise") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item { OutlinedTextField(query, { query = it }, label = { Text("Search") }, modifier = Modifier.fillMaxWidth()) }
                val visible = exercises.filter { exerciseMatchesQuery(it, query) }
                    .sortedWith(compareByDescending<Exercise> { it.id in preferredIds }.thenBy { it.name.lowercase() })
                if (preferredIds.isNotEmpty()) item {
                    Text("Routine alternatives are shown first", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                items(visible, key = Exercise::id) { exercise ->
                    WhipTextButton(onClick = { onPick(exercise) }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            exercise.name + if (exercise.id in preferredIds) " · Planned alternative" else "",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (exercises.isEmpty()) item { Text("Your library is empty.") }
            }
        },
        confirmButton = { WhipTextButton(onClick = onCreate) { Text("Create New") } },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ExerciseActionsDialog(
    modifier: Modifier = Modifier,
    exercise: Exercise,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onFavorite: () -> Unit,
    onDuplicate: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(exercise.name) },
        text = {
            Column {
                WhipTextButton(onClick = onFavorite, modifier = Modifier.fillMaxWidth()) { Text(if (exercise.favorite) "Remove Favorite" else "Favorite") }
                WhipTextButton(onClick = onDuplicate, modifier = Modifier.fillMaxWidth()) { Text("Duplicate") }
                WhipTextButton(onClick = onArchive, modifier = Modifier.fillMaxWidth()) { Text(if (exercise.archived) "Restore" else "Archive") }
                WhipTextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete Permanently", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { WhipTextButton(onClick = onDismiss) { Text("Close") } },
        dismissButton = { DetailEditButton("Edit exercise", onEdit) },
    )
}

@Composable
private fun WorkoutEditorDialog(
    modifier: Modifier = Modifier,
    session: WorkoutSession?,
    initialDate: LocalDate,
    initialKeepAwake: Boolean = false,
    onDismiss: () -> Unit,
    onStart: (String, String, LocalDate, Boolean) -> Unit,
) {
    val editorKey = "workout-${session?.id ?: "new"}"
    var name by rememberSaveable(editorKey) { mutableStateOf(session?.name.orEmpty()) }
    var notes by rememberSaveable(editorKey) { mutableStateOf(session?.notes.orEmpty()) }
    var date by rememberSaveable(editorKey) { mutableStateOf(initialDate) }
    var keepAwake by rememberSaveable(editorKey) { mutableStateOf(session?.keepScreenAwake ?: initialKeepAwake) }
    var showDatePicker by rememberSaveable(editorKey) { mutableStateOf(false) }
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(if (session == null) "Start Workout" else "Workout Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Optional name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                if (session == null) {
                    WhipOutlinedButton(onClick = { showDatePicker = true }) {
                        Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
                    }
                }
                ToggleRow("Keep screen awake on workout screen", keepAwake) { keepAwake = it }
            }
        },
        confirmButton = { WhipTextButton(onClick = { onStart(name, notes, date, keepAwake) }) { Text(if (session == null) "Start" else "Save") } },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (showDatePicker) {
        WhipDatePickerDialog(
            initialDate = date,
            onDismiss = { showDatePicker = false },
            onDateSelected = { date = it; showDatePicker = false },
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val displayLabel = label.uiTitleCase()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Toggle $displayLabel", role = Role.Switch) { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(displayLabel, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun ConfirmationDialog(
    modifier: Modifier = Modifier,
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { WhipTextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun WorkoutGroupDialog(
    modifier: Modifier = Modifier,
    exercises: List<WorkoutExerciseUi>,
    onDismiss: () -> Unit,
    onCreate: (String, WorkoutGroupType, List<Long>) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("Superset") }
    var type by rememberSaveable { mutableStateOf(WorkoutGroupType.Superset) }
    var selectedIds by rememberSaveable {
        mutableStateOf<Set<Long>>(exercises.take(2).mapTo(linkedSetOf()) { it.workoutExercise.id })
    }
    ProductivityEditorDialog(
        modifier = modifier,
        testTag = "workout-group-editor",
        onDismissRequest = onDismiss,
        title = { Text("Group Workout Exercises") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Group name") })
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    WorkoutGroupType.entries.forEach { option ->
                        WhipFilterChip(
                            selected = type == option,
                            onClick = { type = option },
                            label = { Text(option.name) },
                        )
                    }
                }
                Text("Choose two or more exercises. Other exercises remain independent.")
                exercises.forEach { item ->
                    val id = item.workoutExercise.id
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(id in selectedIds, null)
                        Text(item.exercise.name)
                    }
                }
            }
        },
        confirmButton = {
            WhipTextButton(
                enabled = name.isNotBlank() && selectedIds.size >= 2,
                onClick = { onCreate(name.trim(), type, selectedIds.toList()) },
            ) { Text("Create") }
        },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun WorkoutSet.shortLabel(
    preferredWeightUnitId: String,
    preferredDistanceUnitId: String,
    precision: Int,
    workoutExercise: WorkoutExercise? = null,
    exerciseWeightUnitId: String = preferredWeightUnitId,
): String {
    val parts = mutableListOf<String>()
    when (workoutExercise?.machineLoadTypeSnapshot) {
        MachineLoadType.Level -> machineLoadValue?.let {
            parts += "${workoutExercise.machineLevelLabelSnapshot.ifBlank { "level" }} ${formatNumber(it, precision)}"
        }
        MachineLoadType.Mass, null -> {
            val displayUnit = if (workoutExercise?.machineLoadTypeSnapshot == MachineLoadType.Mass) {
                workoutExercise.machineUnitIdSnapshot
            } else {
                exerciseWeightUnitId.ifBlank { preferredWeightUnitId }
            }
            val meaning = workoutExercise?.loadInterpretationSnapshot ?: LoadInterpretation.Total
            val raw = enteredWeight
            val total = canonicalWeightKg?.let { massFromKilograms(it, displayUnit) }
            if (raw != null && meaning != LoadInterpretation.Total) {
                val qualifier = when (meaning) {
                    LoadInterpretation.PerHand -> "per hand"
                    LoadInterpretation.PerSide -> "per side"
                    LoadInterpretation.AddedLoad -> "added"
                    LoadInterpretation.BodyweightPlusExternal -> "external"
                    LoadInterpretation.BodyweightPercentage -> "external"
                    LoadInterpretation.AssistedSubtraction -> "assistance"
                    LoadInterpretation.MachineDisplayedMass -> "displayed"
                    LoadInterpretation.OrdinalSetting -> "setting"
                    LoadInterpretation.Total -> "total"
                }
                parts += "${formatNumber(raw, precision)} ${unitSymbol(displayUnit)} $qualifier" +
                    total?.takeIf { meaning in setOf(LoadInterpretation.PerHand, LoadInterpretation.PerSide) }
                        ?.let { " (${formatNumber(it, precision)} total)" }.orEmpty()
            } else total?.let { parts += "${formatNumber(it, precision)} ${unitSymbol(displayUnit)}" }
        }
    }
    repetitions?.let { parts += "$it reps" }
    canonicalDistanceMetres?.let { parts += "${formatNumber(distanceFromMetres(it, preferredDistanceUnitId), precision)} ${unitSymbol(preferredDistanceUnitId)}" }
    durationSeconds?.let { parts += formatDuration(it) }
    return parts.joinToString(" × ").ifBlank { "Empty set" }
}

private fun WorkoutSet.prescriptionLabel(
    preferredWeightUnitId: String,
    precision: Int,
    workoutExercise: WorkoutExercise,
): String? {
    if (listOf(
            prescribedCanonicalWeightKg,
            prescribedEnteredWeight,
            prescribedRepetitions,
            prescribedRpe,
            prescribedRir,
            prescribedDurationSeconds,
            prescribedMachineLoadValue,
        ).all { it == null } && prescriptionSourceLabel.isBlank()) return null
    val parts = mutableListOf<String>()
    if (workoutExercise.machineLoadTypeSnapshot == MachineLoadType.Level) {
        prescribedMachineLoadValue?.let { parts += "${workoutExercise.machineLevelLabelSnapshot.ifBlank { "setting" }} ${formatNumber(it, precision)}" }
    } else {
        val unit = prescribedWeightUnitId ?: workoutExercise.machineUnitIdSnapshot.takeIf(String::isNotBlank) ?: preferredWeightUnitId
        prescribedEnteredWeight?.let { parts += "${formatNumber(it, precision)} ${unitSymbol(unit)} entered" }
            ?: prescribedCanonicalWeightKg?.let { parts += "${formatNumber(massFromKilograms(it, unit), precision)} ${unitSymbol(unit)}" }
    }
    prescribedRepetitions?.let { parts += "$it reps" }
    prescribedRpe?.let { parts += "RPE ${formatNumber(it, 1)}" }
    prescribedRir?.let { parts += "RIR ${formatNumber(it, 1)}" }
    prescribedDurationSeconds?.let { parts += formatDuration(it) }
    val resolved = parts.joinToString(" × ")
    return when {
        prescriptionSourceLabel.isNotBlank() && resolved.isNotBlank() -> "$prescriptionSourceLabel → $resolved"
        prescriptionSourceLabel.isNotBlank() -> prescriptionSourceLabel
        else -> resolved.takeIf(String::isNotBlank)
    }
}

private fun GymGraphMetric.displayValue(value: Double, weightUnitId: String, distanceUnitId: String): Double = when (this) {
    GymGraphMetric.EstimatedOneRepMax,
    GymGraphMetric.MaxWeight,
    GymGraphMetric.MaxWeightForReps,
    GymGraphMetric.ActualRepMaxHistory,
    GymGraphMetric.SetVolume,
    GymGraphMetric.WorkoutVolume,
    -> massFromKilograms(value, weightUnitId)
    GymGraphMetric.Distance -> distanceFromMetres(value, distanceUnitId)
    else -> value
}

private fun GymGraphMetric.displayUnit(
    weightUnitId: String,
    distanceUnitId: String,
    machineLevelLabel: String = "level",
): String = when (this) {
    GymGraphMetric.EstimatedOneRepMax,
    GymGraphMetric.MaxWeight,
    GymGraphMetric.MaxWeightForReps,
    GymGraphMetric.ActualRepMaxHistory,
    -> unitSymbol(weightUnitId)
    GymGraphMetric.SetVolume, GymGraphMetric.WorkoutVolume -> "${unitSymbol(weightUnitId)}·rep"
    GymGraphMetric.MaxRepetitions, GymGraphMetric.TotalRepetitions -> "reps"
    GymGraphMetric.Distance -> unitSymbol(distanceUnitId)
    GymGraphMetric.Duration -> "s"
    GymGraphMetric.Speed -> "m/s"
    GymGraphMetric.Pace -> "s/km"
    GymGraphMetric.MaxMachineSetting -> machineLevelLabel
}

private fun PersonalRecord.displayValue(weightUnitId: String, distanceUnitId: String): Double = when (type) {
    PersonalRecordType.MaxWeight,
    PersonalRecordType.BestWeightForRepCount,
    PersonalRecordType.EstimatedOneRepMax,
    PersonalRecordType.SetVolume,
    PersonalRecordType.ExerciseWorkoutVolume,
    -> massFromKilograms(value, weightUnitId)
    PersonalRecordType.MaxDistance -> distanceFromMetres(value, distanceUnitId)
    else -> value
}

private fun PersonalRecord.displayUnit(weightUnitId: String, distanceUnitId: String): String = when (type) {
    PersonalRecordType.MaxWeight,
    PersonalRecordType.BestWeightForRepCount,
    PersonalRecordType.EstimatedOneRepMax,
    -> unitSymbol(weightUnitId)
    PersonalRecordType.SetVolume, PersonalRecordType.ExerciseWorkoutVolume -> "${unitSymbol(weightUnitId)}·rep"
    PersonalRecordType.MaxDistance -> unitSymbol(distanceUnitId)
    PersonalRecordType.MaxRepetitions, PersonalRecordType.MaxRepetitionsForWeight -> "reps"
    PersonalRecordType.MaxDuration -> "s"
    PersonalRecordType.MaxSpeed -> "m/s"
    PersonalRecordType.MinPace -> "s/km"
    PersonalRecordType.MaxMachineSetting -> unitId
}

private fun fieldVisibilityLabel(value: Boolean?): String = when (value) {
    null -> "Use global setting"
    true -> "Always show"
    false -> "Hide"
}

private fun formatNumber(value: Double, precision: Int): String = NumberFormat.getNumberInstance(Locale.getDefault()).run {
    maximumFractionDigits = precision.coerceIn(0, 6)
    minimumFractionDigits = 0
    isGroupingUsed = false
    format(value)
}

private fun editableNumber(value: Double): String = editableNumericValue(value)

private fun parseMachineMassMapping(text: String): Map<Double, Double>? {
    if (text.isBlank()) return emptyMap()
    val result = linkedMapOf<Double, Double>()
    text.lineSequence().flatMap { it.split(',').asSequence() }.filter(String::isNotBlank).forEach { raw ->
        val parts = raw.trim().split('=', ':', limit = 2)
        val setting = parts.getOrNull(0)?.trim()?.toWhipDoubleOrNull() ?: return null
        val kg = parts.getOrNull(1)?.trim()?.toWhipDoubleOrNull() ?: return null
        if (!setting.isFinite() || setting < 0.0 || !kg.isFinite() || kg < 0.0) return null
        result[setting] = kg
    }
    return result
}

private fun parsePlateQuantities(text: String): Map<Double, Int>? {
    if (text.isBlank()) return emptyMap()
    val result = linkedMapOf<Double, Int>()
    text.split(',').filter(String::isNotBlank).forEach { raw ->
        val parts = raw.trim().split(':', '=', limit = 2)
        val plate = parts.getOrNull(0)?.trim()?.toWhipDoubleOrNull() ?: return null
        val quantity = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: return null
        if (!plate.isFinite() || plate <= 0.0 || quantity < 0) return null
        result[plate] = quantity
    }
    return result
}

internal data class ParsedPositiveNumbers(
    val values: List<Double>,
    val error: String? = null,
)

internal fun quantityLabel(count: Int, singular: String, plural: String = "${singular}s"): String =
    "$count ${if (count == 1) singular else plural}"

internal fun exerciseMatchesQuery(
    exercise: Exercise,
    query: String,
    machineNames: String = "",
): Boolean {
    val terms = query.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    if (terms.isEmpty()) return true
    val searchable = listOf(
        exercise.name,
        exercise.equipment,
        exercise.primaryMuscles,
        exercise.secondaryMuscles,
        exercise.trackingType.label,
        machineNames,
    ).joinToString(" ")
    return terms.all { searchable.contains(it, ignoreCase = true) }
}

internal fun parsePositiveNumberList(text: String, itemLabel: String): ParsedPositiveNumbers {
    if (text.isBlank()) return ParsedPositiveNumbers(emptyList(), "Enter at least one $itemLabel")
    val values = mutableListOf<Double>()
    text.split(',').forEachIndexed { index, raw ->
        val token = raw.trim()
        val value = token.toWhipDoubleOrNull()
        if (token.isBlank() || value == null || !value.isFinite() || value <= 0.0) {
            return ParsedPositiveNumbers(
                emptyList(),
                "${itemLabel.uiTitleCase()} ${index + 1} (“${token.ifBlank { "blank" }}”) must be a positive number",
            )
        }
        values += value
    }
    return ParsedPositiveNumbers(values)
}

internal data class ValidatedGymGraphRange(
    val from: LocalDate?,
    val to: LocalDate,
    val error: String? = null,
)

internal fun validateGymGraphRange(
    range: GymGraphRange,
    customFrom: String,
    customTo: String,
    today: LocalDate,
): ValidatedGymGraphRange {
    if (range != GymGraphRange.Custom) {
        return ValidatedGymGraphRange(graphRangeStart(range, today), today)
    }
    if (customFrom.isBlank() || customTo.isBlank()) {
        return ValidatedGymGraphRange(null, today, "Enter both From and To dates")
    }
    val from = runCatching { LocalDate.parse(customFrom) }.getOrNull()
        ?: return ValidatedGymGraphRange(null, today, "From must use YYYY-MM-DD")
    val to = runCatching { LocalDate.parse(customTo) }.getOrNull()
        ?: return ValidatedGymGraphRange(null, today, "To must use YYYY-MM-DD")
    if (from > to) return ValidatedGymGraphRange(null, to, "From must be on or before To")
    return ValidatedGymGraphRange(from, to)
}

private fun machineLoadSummary(values: List<Double>): String {
    val compact = compactNumericSequence(values)
    if (compact.isRange) return "${compact.specification} by ${editableNumber(requireNotNull(compact.increment))}"
    val sorted = values.distinct().sorted()
    val shown = sorted.take(10).joinToString(", ", transform = ::editableNumber)
    return if (sorted.size > 10) "$shown, … (${sorted.size} total)" else shown
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3_600
    val minutes = seconds % 3_600 / 60
    val remaining = seconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, remaining)
    else "%d:%02d".format(minutes, remaining)
}

internal fun steppedWorkoutLoad(
    current: Double?,
    direction: Int,
    availableLoads: List<Double>,
    fallbackIncrement: Double,
): Double {
    val choices = availableLoads.filter { it.isFinite() && it >= 0.0 }.distinct().sorted()
    if (choices.isNotEmpty()) {
        if (current == null) return choices.first()
        return if (direction >= 0) choices.firstOrNull { it > current + 1e-9 } ?: choices.last()
        else choices.lastOrNull { it < current - 1e-9 } ?: choices.first()
    }
    val increment = fallbackIncrement.takeIf { it.isFinite() && it > 0.0 } ?: 1.0
    return ((current ?: 0.0) + if (direction >= 0) increment else -increment).coerceAtLeast(0.0)
}

private fun shareWorkout(context: android.content.Context, session: WorkoutSession, state: GymUiState) {
    val exerciseById = state.exercises.associateBy(Exercise::id)
    val body = buildString {
        appendLine(session.name.ifBlank { "Workout" })
        appendLine(session.localDate)
        if (session.notes.isNotBlank()) appendLine(session.notes)
        state.allWorkoutExercises.filter { it.sessionId == session.id }.sortedBy { it.position }.forEach { workoutExercise ->
            appendLine()
            val sourceExercise = exerciseById[workoutExercise.exerciseId]
            appendLine(sourceExercise?.name ?: "Exercise")
            if (workoutExercise.machineNameSnapshot.isNotBlank()) appendLine("Machine: ${workoutExercise.machineNameSnapshot}")
            if (workoutExercise.notes.isNotBlank()) appendLine("> ${workoutExercise.notes}")
            state.allSets.filter { it.workoutExerciseId == workoutExercise.id && it.deletedAtMillis == null }
                .sortedBy { it.position }
                .forEachIndexed { index, set ->
                    append("${index + 1}. ${set.shortLabel(state.appSettings.gymWeightUnitId, state.appSettings.distanceUnitId, state.appSettings.numberPrecision, workoutExercise, sourceExercise?.weightUnitId ?: state.appSettings.gymWeightUnitId)}")
                    if (set.classification != WorkoutSetClassification.Working) append(" · ${set.classification.uiLabel()}")
                    set.rpe?.let { append(" · RPE $it") }
                    if (set.note.isNotBlank()) append(" · ${set.note}")
                    appendLine()
                }
        }
        appendLine()
        append("Shared from Whip")
    }
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, session.name.ifBlank { "Workout ${session.localDate}" })
                .putExtra(Intent.EXTRA_TEXT, body),
            "Share workout",
        ),
    )
}
