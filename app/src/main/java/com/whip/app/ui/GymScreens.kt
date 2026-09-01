package com.whip.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Remove
import com.whip.app.R
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Restore
import com.whip.app.domain.Exercise
import com.whip.app.domain.BuiltInUnits
import com.whip.app.core.zoneId
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.ExerciseCategory
import com.whip.app.domain.ExerciseTrackingType
import com.whip.app.domain.BodyweightLoadPolicy
import com.whip.app.domain.WorkoutSession
import com.whip.app.domain.WorkoutExercise
import com.whip.app.domain.WorkoutGroup
import com.whip.app.domain.WorkoutGroupType
import com.whip.app.domain.WorkoutSet
import com.whip.app.domain.WorkoutSetClassification
import com.whip.app.domain.WorkoutSetDraft
import com.whip.app.domain.WeightEquipmentSetup
import com.whip.app.domain.validateWorkoutSetDraft
import com.whip.app.domain.GymRoutine
import com.whip.app.domain.GymMachine
import com.whip.app.domain.GymMachineDraft
import com.whip.app.domain.MachineLevelDirection
import com.whip.app.domain.MachineLoadType
import com.whip.app.domain.MachineStackMode
import com.whip.app.domain.LoadInterpretation
import com.whip.app.domain.GymGraphAggregation
import com.whip.app.domain.GymGraphMetric
import com.whip.app.domain.GymGraphRange
import com.whip.app.domain.GymGraphPoint
import com.whip.app.domain.EstimatedOneRepMaxFormula
import com.whip.app.domain.RoutineDayDraft
import com.whip.app.domain.RoutineDay
import com.whip.app.domain.RoutineDraft
import com.whip.app.domain.RoutineExerciseDraft
import com.whip.app.domain.RoutineProgramDraft
import com.whip.app.domain.RoutineProgramKind
import com.whip.app.domain.RoutineProgramPhaseRole
import com.whip.app.domain.RoutineAssistanceRole
import com.whip.app.domain.RoutineAssistanceCategory
import com.whip.app.domain.RoutinePlacementKind
import com.whip.app.domain.RoutineOptionalWorkKind
import com.whip.app.domain.RoutineWorkSection
import com.whip.app.domain.TrainingMaxDecision
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
import com.whip.app.core.PersistenceRequestState
import com.whip.app.core.WhipResult
import com.whip.app.core.DEFAULT_REST_TIMER_PRESET_SECONDS
import com.whip.app.core.normalizeRestTimerPresets
import com.whip.app.core.TrackedGymRecord
import com.whip.app.core.recommendedTrackedRecordTypes
import com.whip.app.core.resolveForExercise
import com.whip.app.core.supportedTrackedRecordTypes
import com.whip.app.data.ExerciseDeletionImpact
import com.whip.app.data.RoutineDeletionImpact
import java.text.NumberFormat
import java.io.Serializable
import java.time.Duration
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

internal fun gymPersistenceResult(succeeded: Boolean, status: OperationStatus): WhipResult<Unit> =
    if (succeeded) {
        WhipResult.Success(Unit)
    } else {
        val failure = status as? OperationStatus.Failed
        WhipResult.Failure(failure?.message.orEmpty(), failure?.cause)
    }

enum class GymAddRequest {
    StartWorkout,
    AddWorkoutExercise,
    CreateExercise,
    CreateMachine,
    CreateCategory,
    CreateRoutine,
}

internal val primaryGymDestinations = listOf(
    GymDestination.Workout,
    GymDestination.History,
    GymDestination.Progress,
    GymDestination.Library,
)

internal val libraryGymDestinations = GymDestination.entries.filterNot(primaryGymDestinations::contains)

private enum class WorkoutHistoryRange { Month, ThreeMonths, Year, All }

internal enum class ExerciseLibrarySort(val label: String) {
    Manual("Custom Order"),
    Name("Name"),
    RecentlyUsed("Last Used"),
    RecentlyAdded("Date Added"),
    FavoritesFirst("Favorite Status"),
}

internal fun List<Exercise>.sortedForLibrary(
    sort: ExerciseLibrarySort,
    direction: SortDirection,
    lastUsedAtByExercise: Map<Long, Long> = emptyMap(),
): List<Exercise> {
    val ascending = direction == SortDirection.Ascending
    return when (sort) {
        ExerciseLibrarySort.Manual -> sortedWith(
            if (ascending) compareBy<Exercise>(Exercise::position).thenBy(Exercise::id)
            else compareByDescending<Exercise>(Exercise::position).thenByDescending(Exercise::id),
        )
        ExerciseLibrarySort.Name -> sortedWith(
            if (ascending) compareBy { it.name.lowercase() } else compareByDescending { it.name.lowercase() },
        )
        ExerciseLibrarySort.RecentlyUsed -> sortedWith(
            compareBy<Exercise, Long?>(nullsLast(if (ascending) naturalOrder() else reverseOrder())) {
                lastUsedAtByExercise[it.id]?.takeIf { usedAt -> usedAt > 0L }
            }.thenBy { it.name.lowercase() },
        )
        ExerciseLibrarySort.RecentlyAdded -> sortedWith(
            (if (ascending) compareBy<Exercise>(Exercise::createdAtMillis) else compareByDescending(Exercise::createdAtMillis))
                .thenBy(Exercise::id)
                .thenBy { it.name.lowercase() },
        )
        ExerciseLibrarySort.FavoritesFirst -> sortedWith(
            (if (ascending) compareBy<Exercise>(Exercise::favorite) else compareByDescending(Exercise::favorite))
                .thenBy { it.name.lowercase() },
        )
    }
}

private const val MACHINE_EQUIPMENT_FILTER = "__machine__"

private fun WorkoutHistoryRange.uiLabel(): String = when (this) {
    WorkoutHistoryRange.Month -> "1 Month"
    WorkoutHistoryRange.ThreeMonths -> "3 Months"
    WorkoutHistoryRange.Year -> "1 Year"
    WorkoutHistoryRange.All -> "All Time"
}

private fun WorkoutGroupType.uiLabel(): String = when (this) {
    WorkoutGroupType.Superset -> "Superset"
    WorkoutGroupType.Circuit -> "Circuit"
}

@Composable
private fun ExerciseSelectionField(
    label: String,
    exercises: List<Exercise>,
    selectedExerciseId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    allLabel: String? = null,
) {
    var expanded by rememberSaveable(label) { mutableStateOf(false) }
    var query by rememberSaveable(label) { mutableStateOf("") }
    val selectedName = exercises.firstOrNull { it.id == selectedExerciseId }?.name
    val matches = exercises.filter { exerciseMatchesQuery(it, query) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box(Modifier.fillMaxWidth()) {
            WhipOutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    selectedName ?: allLabel ?: "Choose Exercise",
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false; query = "" },
                modifier = Modifier.widthIn(max = 320.dp).testTag("gym-exercise-filter-menu"),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search Exercises") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                )
                if (allLabel != null) {
                    val isSelected = selectedExerciseId == null
                    DropdownMenuItem(
                        text = { Text(allLabel) },
                        trailingIcon = if (isSelected) {{
                            Text(
                                "Selected",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.clearAndSetSemantics {},
                            )
                        }} else null,
                        onClick = { onSelect(null); expanded = false; query = "" },
                        modifier = Modifier.semantics { selected = isSelected },
                    )
                }
                matches.take(50).forEach { exercise ->
                    val isSelected = exercise.id == selectedExerciseId
                    DropdownMenuItem(
                        text = { Text(exercise.name) },
                        trailingIcon = if (isSelected) {{
                            Text(
                                "Selected",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.clearAndSetSemantics {},
                            )
                        }} else null,
                        onClick = { onSelect(exercise.id); expanded = false; query = "" },
                        modifier = Modifier.semantics { selected = isSelected },
                    )
                }
                if (matches.isEmpty()) {
                    Text(
                        "No matching exercises",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (matches.size > 50) {
                    Text(
                        "${matches.size - 50} more matches · refine your search",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun ExerciseComparisonField(
    exercises: List<Exercise>,
    excludedExerciseId: Long?,
    selectedExerciseIds: Set<Long>,
    onSelectionChange: (Set<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val choices = exercises.filter { it.id != excludedExerciseId }
    val matches = choices.filter { exerciseMatchesQuery(it, query) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Compare Exercises", style = MaterialTheme.typography.labelMedium)
        Box(Modifier.fillMaxWidth()) {
            WhipOutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (selectedExerciseIds.isEmpty()) "Add up to 3 Comparisons" else "Comparisons · ${selectedExerciseIds.size} of 3",
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false; query = "" },
                modifier = Modifier.widthIn(max = 320.dp).testTag("gym-exercise-comparison-menu"),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search Exercises") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                )
                matches.take(50).forEach { exercise ->
                    val selected = exercise.id in selectedExerciseIds
                    DropdownMenuItem(
                        text = { Text(exercise.name) },
                        trailingIcon = if (selected) {{
                            Text(
                                "Selected",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.clearAndSetSemantics {},
                            )
                        }} else null,
                        enabled = selected || selectedExerciseIds.size < 3,
                        modifier = Modifier.semantics { this.selected = selected },
                        onClick = {
                            onSelectionChange(
                                if (selected) selectedExerciseIds - exercise.id else selectedExerciseIds + exercise.id,
                            )
                        },
                    )
                }
                if (matches.isEmpty()) {
                    Text(
                        "No matching exercises",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (selectedExerciseIds.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                selectedExerciseIds.mapNotNull { id -> exercises.firstOrNull { it.id == id } }.forEach { selected ->
                    WhipFilterChip(
                        selected = true,
                        onClick = { onSelectionChange(selectedExerciseIds - selected.id) },
                        label = { Text("${selected.name} ×") },
                    )
                }
            }
        }
    }
}

@Composable
fun GymAreaContent(
    state: GymUiState,
    innerPadding: PaddingValues,
    viewModel: GymViewModel,
    modifier: Modifier = Modifier,
    addRequest: GymAddRequest? = null,
    onExternalRequestConsumed: () -> Unit = {},
    openSearchRequest: WhipSearchResult? = null,
    onOpenSearchRequestConsumed: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onOpenBackupSettings: () -> Unit = {},
    onRoutineEditorStateChange: (Boolean) -> Unit = {},
    operationStatus: OperationStatus = OperationStatus.Idle,
    initialDestination: GymDestination = GymDestination.Workout,
    onDestinationChange: (GymDestination) -> Unit = {},
    requestedWorkoutExerciseId: Long? = null,
    onRequestedWorkoutExerciseConsumed: () -> Unit = {},
    onReorderModeChange: (Boolean) -> Unit = {},
    reorderDismissRequest: Int = 0,
) {
    val dialogModifier = modifier
    var destination by rememberSaveable(initialDestination) { mutableStateOf(initialDestination) }
    if (state.loading || state.errorMessage != null) {
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            DestinationTabBar(
                selected = destination.takeUnless { it in libraryGymDestinations } ?: GymDestination.Library,
                destinations = primaryGymDestinations,
                onSelect = { destination = it },
                label = GymDestination::name,
                testTagPrefix = "gym-destination",
                barTestTag = "gym-workspace-navigation",
            )
            DomainLoadContent("gym data", PaddingValues(), state.errorMessage, viewModel::retryLoading)
        }
        return
    }
    val context = LocalContext.current
    val machineDeletionImpact by viewModel.machineDeletionImpact.collectAsStateWithLifecycle()
    val machineDeletionInProgress by viewModel.machineDeletionInProgress.collectAsStateWithLifecycle()
    val exerciseDeletionImpact by viewModel.exerciseDeletionImpact.collectAsStateWithLifecycle()
    val exerciseDeletionPreviewError by viewModel.exerciseDeletionPreviewError.collectAsStateWithLifecycle()
    val exerciseDeletionTargetMissing by viewModel.exerciseDeletionTargetMissing.collectAsStateWithLifecycle()
    val routineDeletionImpact by viewModel.routineDeletionImpact.collectAsStateWithLifecycle()
    val routineDeletionPreviewError by viewModel.routineDeletionPreviewError.collectAsStateWithLifecycle()
    val routineDeletionTargetMissing by viewModel.routineDeletionTargetMissing.collectAsStateWithLifecycle()
    val gymDeletionState by viewModel.gymDeletionState.collectAsStateWithLifecycle()
    val orphanedGymDeletionRequestId by viewModel.orphanedGymDeletionRequestId.collectAsStateWithLifecycle()
    var exerciseEditorId by rememberSaveable { mutableStateOf<Long?>(null) }
    var creatingExercise by rememberSaveable { mutableStateOf(false) }
    var addCreatedExerciseToSession by rememberSaveable { mutableStateOf<Long?>(null) }
    var exerciseActionsId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showExercisePicker by rememberSaveable { mutableStateOf(false) }
    var showStartWorkout by rememberSaveable { mutableStateOf(false) }
    var showEditWorkout by rememberSaveable { mutableStateOf(false) }
    var finishConfirmation by rememberSaveable { mutableStateOf(false) }
    var trainingMaxCycleReviewOpen by rememberSaveable { mutableStateOf(false) }
    var discardConfirmation by rememberSaveable { mutableStateOf(false) }
    var showGroupDialog by rememberSaveable { mutableStateOf(false) }
    var editedSetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var exerciseNotesEditorId by rememberSaveable { mutableStateOf<Long?>(null) }
    var focusedWorkoutId by rememberSaveable { mutableStateOf<Long?>(null) }
    var historyWorkoutEditorId by rememberSaveable { mutableStateOf<Long?>(null) }
    var focusedRoutineId by rememberSaveable { mutableStateOf<Long?>(null) }
    var exerciseDeleteCandidateId by rememberSaveable { mutableStateOf<Long?>(null) }
    var exerciseDeleteCandidateUuid by rememberSaveable { mutableStateOf<String?>(null) }
    var exerciseDeleteCandidateGeneration by rememberSaveable { mutableStateOf<Long?>(null) }
    var workoutDeleteCandidateId by rememberSaveable { mutableStateOf<Long?>(null) }
    var routineDeleteCandidateId by rememberSaveable { mutableStateOf<Long?>(null) }
    var routineDeleteCandidateGeneration by rememberSaveable { mutableStateOf<Long?>(null) }
    var creatingMachine by rememberSaveable { mutableStateOf(false) }
    var creatingExerciseForMachine by rememberSaveable { mutableStateOf(false) }
    var createdExerciseForMachineId by rememberSaveable { mutableStateOf<Long?>(null) }
    var machineEditorId by rememberSaveable { mutableStateOf<Long?>(null) }
    var machineVersionSourceId by rememberSaveable { mutableStateOf<Long?>(null) }
    var inlineMachineWorkoutExerciseId by rememberSaveable { mutableStateOf<Long?>(null) }
    var inlineMachineExerciseId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingMachineExerciseId by rememberSaveable { mutableStateOf<Long?>(null) }
    var substituteWorkoutExerciseId by rememberSaveable { mutableStateOf<Long?>(null) }
    var createForSubstitutionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var routineEditorOpen by rememberSaveable { mutableStateOf(false) }
    var catalogSavePending by rememberSaveable { mutableStateOf(false) }
    var trackedRecordsManagerOpen by rememberSaveable { mutableStateOf(false) }
    var trackedRecordsInitialExerciseId by rememberSaveable { mutableStateOf<Long?>(null) }
    var browseReordering by rememberSaveable { mutableStateOf(false) }
    val reportBrowseReordering: (Boolean) -> Unit = { active ->
        browseReordering = active
        onReorderModeChange(active)
    }
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
    val deletionTargetKey = when {
        exerciseDeleteCandidateId != null -> "exercise-${exerciseDeleteCandidateId}"
        routineDeleteCandidateId != null -> "routine-${routineDeleteCandidateId}"
        else -> null
    }
    val gymDeletionCoordinator = deletionTargetKey?.let { targetKey ->
        rememberPersistenceRequestCoordinator(
            state = gymDeletionState,
            consume = viewModel::consumeGymDeletionResult,
            key = targetKey,
            requestNamespace = "gym-delete-$targetKey",
            onPersisted = { receipt ->
                when (receipt.kind) {
                    GymDeletionKind.Exercise -> {
                        exerciseDeleteCandidateId = null
                        exerciseDeleteCandidateUuid = null
                        exerciseDeleteCandidateGeneration = null
                        viewModel.dismissExerciseDeletion()
                    }
                    GymDeletionKind.Routine -> {
                        routineDeleteCandidateId = null
                        routineDeleteCandidateGeneration = null
                        viewModel.dismissRoutineDeletion()
                    }
                }
            },
        )
    }
    LaunchedEffect(exerciseDeleteCandidateId) {
        val id = exerciseDeleteCandidateId ?: return@LaunchedEffect
        if (exerciseDeletionImpact?.exerciseId != id && exerciseDeletionPreviewError == null) {
            viewModel.previewExerciseDeletion(id)
        }
    }
    LaunchedEffect(routineDeleteCandidateId) {
        val id = routineDeleteCandidateId ?: return@LaunchedEffect
        if (routineDeletionImpact?.routineId != id && routineDeletionPreviewError == null) {
            viewModel.previewRoutineDeletion(id)
        }
    }
    LaunchedEffect(
        gymDeletionCoordinator?.requestId,
        gymDeletionState,
        orphanedGymDeletionRequestId,
        deletionTargetKey,
    ) {
        val coordinator = gymDeletionCoordinator ?: return@LaunchedEffect
        val requestId = coordinator.requestId ?: return@LaunchedEffect
        if (
            gymDeletionState is PersistenceRequestState.Idle &&
            orphanedGymDeletionRequestId != requestId
        ) {
            // A normal submit enters the ViewModel's Running state synchronously
            // before this effect can recompose. Remaining Idle ownership came
            // from SavedState after process replacement, so claim it before the
            // generic coordinator's orphan timeout can misclassify the outcome.
            viewModel.adoptOrphanedGymDeletionRequest(requestId)
        }
    }
    LaunchedEffect(
        orphanedGymDeletionRequestId,
        exerciseDeletionTargetMissing,
        routineDeletionTargetMissing,
        exerciseDeletionImpact,
        routineDeletionImpact,
        exerciseDeletionPreviewError,
        routineDeletionPreviewError,
    ) {
        val coordinator = gymDeletionCoordinator ?: return@LaunchedEffect
        val requestId = coordinator.requestId ?: return@LaunchedEffect
        if (orphanedGymDeletionRequestId != requestId) return@LaunchedEffect
        when {
            exerciseDeleteCandidateId != null && exerciseDeletionTargetMissing -> {
                val expectedGeneration = exerciseDeleteCandidateGeneration ?: return@LaunchedEffect
                viewModel.finishOrphanedExerciseDeletionAsAchieved(
                    requestId = requestId,
                    exerciseId = requireNotNull(exerciseDeleteCandidateId),
                    exerciseUuid = exerciseDeleteCandidateUuid,
                    expectedDataGeneration = expectedGeneration,
                )
            }
            routineDeleteCandidateId != null && routineDeletionTargetMissing -> {
                val expectedGeneration = routineDeleteCandidateGeneration ?: return@LaunchedEffect
                viewModel.finishOrphanedRoutineDeletionAsAchieved(
                    requestId = requestId,
                    routineId = requireNotNull(routineDeleteCandidateId),
                    expectedDataGeneration = expectedGeneration,
                )
            }
            exerciseDeleteCandidateId != null &&
                exerciseDeletionImpact?.exerciseId == exerciseDeleteCandidateId -> {
                viewModel.finishOrphanedGymDeletionAsInterrupted(requestId)
            }
            routineDeleteCandidateId != null &&
                routineDeletionImpact?.routineId == routineDeleteCandidateId -> {
                viewModel.finishOrphanedGymDeletionAsInterrupted(requestId)
            }
            exerciseDeleteCandidateId != null && exerciseDeletionPreviewError != null -> {
                viewModel.finishOrphanedGymDeletionAsUnverified(requestId)
            }
            routineDeleteCandidateId != null && routineDeletionPreviewError != null -> {
                viewModel.finishOrphanedGymDeletionAsUnverified(requestId)
            }
        }
    }
    LaunchedEffect(viewModel.currentDataGeneration()) {
        val currentGeneration = viewModel.currentDataGeneration()
        if (exerciseDeleteCandidateGeneration?.let { it != currentGeneration } == true) {
            gymDeletionCoordinator?.clear()
            viewModel.abandonOrphanedGymDeletionVerification()
            viewModel.dismissExerciseDeletion()
            exerciseDeleteCandidateId = null
            exerciseDeleteCandidateUuid = null
            exerciseDeleteCandidateGeneration = null
        }
        if (routineDeleteCandidateGeneration?.let { it != currentGeneration } == true) {
            gymDeletionCoordinator?.clear()
            viewModel.abandonOrphanedGymDeletionVerification()
            viewModel.dismissRoutineDeletion()
            routineDeleteCandidateId = null
            routineDeleteCandidateGeneration = null
        }
    }
    fun closeCatalogEditors() {
        creatingMachine = false
        creatingExerciseForMachine = false
        createdExerciseForMachineId = null
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
    }
    LaunchedEffect(addRequest) {
        when (addRequest) {
            GymAddRequest.StartWorkout -> showStartWorkout = true
            GymAddRequest.AddWorkoutExercise -> showExercisePicker = true
            GymAddRequest.CreateExercise -> creatingExercise = true
            GymAddRequest.CreateMachine -> creatingMachine = true
            GymAddRequest.CreateCategory,
            GymAddRequest.CreateRoutine,
            null -> return@LaunchedEffect
        }
        onExternalRequestConsumed()
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
        if (!routineEditorOpen && !browseReordering) DestinationTabBar(
            selected = destination.takeUnless { it in libraryGymDestinations } ?: GymDestination.Library,
            destinations = primaryGymDestinations,
            onSelect = {
                destination = it
                focusedWorkoutId = null
                focusedRoutineId = null
            },
            label = GymDestination::name,
            testTagPrefix = "gym-destination",
            barTestTag = "gym-workspace-navigation",
        )
        if (!routineEditorOpen && !browseReordering && destination in libraryGymDestinations) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WhipBackAction(
                    label = "Back to Gym Library",
                    onClick = { destination = GymDestination.Library },
                    modifier = Modifier.testTag("gym-library-child-${destination.name}"),
                )
                Text("Library", style = MaterialTheme.typography.labelLarge)
            }
        }
        when (destination) {
            GymDestination.Library -> GymLibraryLanding(onOpen = { destination = it })
            GymDestination.Workout -> WorkoutContent(
                state = state,
                requestedWorkoutExerciseId = requestedWorkoutExerciseId,
                onRequestedWorkoutExerciseConsumed = onRequestedWorkoutExerciseConsumed,
                onStart = { showStartWorkout = true },
                onOpenRoutines = { destination = GymDestination.Routines },
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
                onRemoveFromGroup = viewModel::removeWorkoutExerciseFromGroup,
                onSubstituteExercise = { workoutExerciseId ->
                    substituteWorkoutExerciseId = workoutExerciseId
                    showExercisePicker = true
                },
                onReorderExercises = viewModel::reorderWorkoutExercises,
                onReorderSets = viewModel::reorderSets,
                onFinish = {
                    val hasIncompleteSets = state.activeWorkoutExercises.any { item ->
                        item.sets.any { it.isIncompleteRequiredWork() }
                    }
                    if (hasIncompleteSets) {
                        finishConfirmation = true
                    } else if (state.activeFiveThreeOneCycleReview() != null) {
                        trainingMaxCycleReviewOpen = true
                    } else {
                        state.activeSession?.let { viewModel.finishWorkout(it.id) }
                    }
                },
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
                onReorder = viewModel::reorderExercises,
                onReorderModeChange = reportBrowseReordering,
                reorderDismissRequest = reorderDismissRequest,
            )
            GymDestination.Machines -> MachineLibraryContent(
                state = state,
                onCreate = { creatingMachine = true },
                onEdit = { machineEditorId = it.id },
                onArchive = viewModel::setMachineArchived,
                onNewVersion = { machineVersionSourceId = it.id },
                onDelete = { viewModel.previewMachineDeletion(it.id) },
            )
            GymDestination.Categories -> ExerciseCategoryContent(
                state = state,
                viewModel = viewModel,
                modifier = dialogModifier,
                createRequested = addRequest == GymAddRequest.CreateCategory,
                onCreateRequestConsumed = onExternalRequestConsumed,
                onReorderModeChange = reportBrowseReordering,
                reorderDismissRequest = reorderDismissRequest,
            )
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
                modifier = dialogModifier,
            )
            GymDestination.Progress -> GymProgressContent(
                state,
                onOpenExercises = { destination = GymDestination.Exercises },
                onOpenWorkout = { destination = GymDestination.Workout },
                onOpenWorkoutHistory = { workoutId ->
                    focusedWorkoutId = workoutId
                    destination = GymDestination.History
                },
                onManageTrackedRecords = {
                    trackedRecordsInitialExerciseId = null
                    trackedRecordsManagerOpen = true
                },
                modifier = dialogModifier,
            )
            GymDestination.Routines -> RoutineContent(
                state = state,
                viewModel = viewModel,
                focusedRoutineId = focusedRoutineId,
                onDeleteRequest = {
                    routineDeleteCandidateId = it.id
                    routineDeleteCandidateGeneration = viewModel.currentDataGeneration()
                },
                onOpenActiveWorkout = { destination = GymDestination.Workout },
                modifier = dialogModifier,
                onEditorStateChange = { open ->
                    routineEditorOpen = open
                    onRoutineEditorStateChange(open)
                },
                createRequested = addRequest == GymAddRequest.CreateRoutine,
                onCreateRequestConsumed = onExternalRequestConsumed,
                onReorderModeChange = reportBrowseReordering,
                reorderDismissRequest = reorderDismissRequest,
            )
            GymDestination.Tools -> GymToolsContent(
                state,
                onSavePreset = viewModel::savePlatePreset,
                onDeletePreset = viewModel::deletePlatePreset,
                modifier = dialogModifier,
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
            createdExerciseIdRequest = createdExerciseForMachineId,
            onCreatedExerciseRequestConsumed = { createdExerciseForMachineId = null },
            onCreateExercise = {
                creatingExerciseForMachine = true
                creatingExercise = true
            },
            onCreateVersion = machineEditor?.takeIf { selected -> state.allWorkoutExercises.any { it.machineId == selected.id } }?.let { selected ->
                {
                    machineEditorId = null
                    machineVersionSourceId = selected.id
                }
            },
            saving = catalogSavePending,
            onDismiss = { closeCatalogEditors() },
            onSave = { draft ->
                catalogSavePending = true
                val onFinished: (Boolean) -> Unit = { succeeded ->
                    catalogSavePending = false
                    if (succeeded) closeCatalogEditors()
                }
                when {
                    machineVersionSource != null -> viewModel.createMachineVersion(machineVersionSource.id, draft, onFinished)
                    inlineMachineWorkoutExerciseId != null -> viewModel.createMachineAndAssign(requireNotNull(inlineMachineWorkoutExerciseId), draft, onFinished)
                    else -> viewModel.saveMachine(machineEditor?.id, draft, onFinished)
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
                if (creatingExerciseForMachine) {
                    creatingExercise = false
                    creatingExerciseForMachine = false
                    catalogSavePending = false
                } else {
                    closeCatalogEditors()
                }
            },
            onSave = { draft ->
                catalogSavePending = true
                if (creatingExerciseForMachine) {
                    viewModel.createExerciseForMachine(draft) { createdId ->
                        catalogSavePending = false
                        if (createdId != null) {
                            createdExerciseForMachineId = createdId
                            creatingExercise = false
                            creatingExerciseForMachine = false
                        }
                    }
                } else {
                val onFinished: (Boolean) -> Unit = { succeeded ->
                    catalogSavePending = false
                    if (succeeded) {
                        if (addCreatedExerciseToSession != null) destination = GymDestination.Workout
                        closeCatalogEditors()
                    }
                }
                val sessionId = addCreatedExerciseToSession
                val substitutionId = createForSubstitutionId
                if (substitutionId != null) {
                    viewModel.createExerciseAndSubstitute(substitutionId, draft, onFinished)
                } else if (sessionId != null) {
                    viewModel.createExerciseAndAdd(sessionId, draft, onFinished)
                } else {
                    viewModel.saveExercise(exerciseEditor?.id, draft, onFinished)
                }
                }
            },
        )
    }

    exerciseNotesEditor?.let { item ->
        WorkoutExerciseNotesDialog(
            modifier = dialogModifier,
            exerciseName = item.exercise.name,
            initialNotes = item.workoutExercise.notes,
            machines = state.machines.filter { it.supportsExercise(item.exercise.id) },
            selectedMachineId = item.workoutExercise.machineId,
            machineLocked = item.sets.any { it.completed },
            onDismiss = { exerciseNotesEditorId = null },
            onSave = { notes, machineId ->
                viewModel.updateWorkoutExerciseDetails(
                    item.workoutExercise.id,
                    notes,
                    item.workoutExercise.groupId,
                    machineId,
                )
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
            trackedInProgress = state.appSettings.trackedGymRecords.any {
                it.exerciseUuid == exercise.uuid && it.type in exercise.supportedTrackedRecordTypes()
            },
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
            onConfigureTrackedRecords = {
                trackedRecordsInitialExerciseId = exercise.id
                trackedRecordsManagerOpen = true
                exerciseActionsId = null
            },
            onArchive = {
                viewModel.setExerciseArchived(exercise.id, !exercise.archived)
                exerciseActionsId = null
            },
            onDelete = {
                exerciseDeleteCandidateId = exercise.id
                exerciseDeleteCandidateUuid = exercise.uuid
                exerciseDeleteCandidateGeneration = viewModel.currentDataGeneration()
                exerciseActionsId = null
            },
        )
    }

    if (trackedRecordsManagerOpen) {
        TrackedRecordsManagerDialog(
            modifier = dialogModifier,
            state = state,
            initialExerciseId = trackedRecordsInitialExerciseId,
            onDismiss = {
                trackedRecordsManagerOpen = false
                trackedRecordsInitialExerciseId = null
            },
            onSave = { records ->
                viewModel.updateTrackedGymRecords(records)
                trackedRecordsManagerOpen = false
                trackedRecordsInitialExerciseId = null
            },
        )
    }

    exerciseDeleteCandidateId?.let { exerciseId ->
        val coordinator = gymDeletionCoordinator ?: return@let
        val exactImpact = exerciseDeletionImpact?.takeIf { it.exerciseId == exerciseId }
        val previewError = exerciseDeletionPreviewError.takeIf { exactImpact == null }
        ExercisePermanentDeleteDialog(
            modifier = dialogModifier,
            exerciseName = exactImpact?.displayName ?: exerciseDeleteCandidate?.name.orEmpty(),
            impact = exactImpact,
            targetMissing = exerciseDeletionTargetMissing,
            outcomeVerificationPending = orphanedGymDeletionRequestId != null,
            preparing = exactImpact == null && previewError == null,
            deleting = coordinator.saving,
            errorMessage = coordinator.errorMessage ?: previewError,
            onDismiss = {
                coordinator.clear()
                viewModel.abandonOrphanedGymDeletionVerification()
                viewModel.dismissExerciseDeletion()
                exerciseDeleteCandidateId = null
                exerciseDeleteCandidateUuid = null
                exerciseDeleteCandidateGeneration = null
            },
            onReviewUpdatedImpact = {
                coordinator.clear()
                val previousRequestId = orphanedGymDeletionRequestId
                val verificationStarted = if (previousRequestId != null) {
                    val verificationRequestId = coordinator.begin()
                    verificationRequestId != null &&
                        viewModel.restartOrphanedGymDeletionVerification(
                            previousRequestId,
                            verificationRequestId,
                        )
                } else {
                    true
                }
                if (verificationStarted) {
                    viewModel.previewExerciseDeletion(exerciseId)
                } else {
                        coordinator.finishFailure("Deletion verification is already running.")
                }
            },
            onOpenActiveWorkout = {
                coordinator.clear()
                viewModel.abandonOrphanedGymDeletionVerification()
                viewModel.dismissExerciseDeletion()
                exerciseDeleteCandidateId = null
                exerciseDeleteCandidateUuid = null
                exerciseDeleteCandidateGeneration = null
                destination = GymDestination.Workout
            },
            onConfirm = { reviewedImpact ->
                val requestId = coordinator.begin() ?: return@ExercisePermanentDeleteDialog
                if (!viewModel.deleteExercisePermanently(
                        exerciseId,
                        reviewedImpact.revisionToken,
                        requestId,
                    )
                ) {
                    coordinator.finishFailure("Another Gym deletion is already finishing.")
                }
            },
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
                "Personal records affected by this workout will be recalculated",
                "Your exercise library and routine templates will remain",
            ),
            onDismiss = { workoutDeleteCandidateId = null },
            onConfirm = { viewModel.deleteWorkoutPermanently(workout.id); workoutDeleteCandidateId = null },
        )
    }

    routineDeleteCandidateId?.let { routineId ->
        val coordinator = gymDeletionCoordinator ?: return@let
        val exactImpact = routineDeletionImpact?.takeIf { it.routineId == routineId }
        val previewError = routineDeletionPreviewError.takeIf { exactImpact == null }
        RoutinePermanentDeleteDialog(
            modifier = dialogModifier,
            routineName = exactImpact?.displayName ?: routineDeleteCandidate?.name.orEmpty(),
            impact = exactImpact,
            targetMissing = routineDeletionTargetMissing,
            outcomeVerificationPending = orphanedGymDeletionRequestId != null,
            preparing = exactImpact == null && previewError == null,
            deleting = coordinator.saving,
            errorMessage = coordinator.errorMessage ?: previewError,
            onDismiss = {
                coordinator.clear()
                viewModel.abandonOrphanedGymDeletionVerification()
                viewModel.dismissRoutineDeletion()
                routineDeleteCandidateId = null
                routineDeleteCandidateGeneration = null
            },
            onReviewUpdatedImpact = {
                coordinator.clear()
                val previousRequestId = orphanedGymDeletionRequestId
                val verificationStarted = if (previousRequestId != null) {
                    val verificationRequestId = coordinator.begin()
                    verificationRequestId != null &&
                        viewModel.restartOrphanedGymDeletionVerification(
                            previousRequestId,
                            verificationRequestId,
                        )
                } else {
                    true
                }
                if (verificationStarted) {
                    viewModel.previewRoutineDeletion(routineId)
                } else {
                        coordinator.finishFailure("Deletion verification is already running.")
                }
            },
            onOpenActiveWorkout = {
                coordinator.clear()
                viewModel.abandonOrphanedGymDeletionVerification()
                viewModel.dismissRoutineDeletion()
                routineDeleteCandidateId = null
                routineDeleteCandidateGeneration = null
                destination = GymDestination.Workout
            },
            onConfirm = { reviewedImpact ->
                val requestId = coordinator.begin() ?: return@RoutinePermanentDeleteDialog
                if (!viewModel.deleteRoutinePermanently(
                        routineId,
                        reviewedImpact.revisionToken,
                        requestId,
                    )
                ) {
                    coordinator.finishFailure("Another Gym deletion is already finishing.")
                }
            },
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
        val substitutingExercise = substituteWorkoutExerciseId != null
        val preferredSubstitutions = substituteWorkoutExerciseId?.let { id ->
            state.activeWorkoutExercises.firstOrNull { it.workoutExercise.id == id }
                ?.workoutExercise?.alternativeExerciseIdsSnapshot
        }.orEmpty()
        ExercisePickerDialog(
            modifier = dialogModifier,
            exercises = state.exercises,
            preferredIds = preferredSubstitutions,
            title = if (substitutingExercise) "Substitute Exercise" else "Add Exercise to This Workout",
            supportingText = if (substitutingExercise) {
                "Choose a replacement for this workout. Completed history is never rewritten."
            } else if (state.activeSession?.sourceRoutineId != null) {
                "This adds the exercise only to the active workout. The routine stays unchanged; logged sets will appear in History."
            } else {
                "This adds the exercise only to the active workout. Logged sets will appear in History."
            },
            onDismiss = { showExercisePicker = false; substituteWorkoutExerciseId = null },
            onPick = { exercise ->
                val machines = state.machines.filter { it.supportsExercise(exercise.id) }
                if (machines.isEmpty()) {
                    val substitutionId = substituteWorkoutExerciseId
                    if (substitutionId != null) viewModel.substituteWorkoutExercise(substitutionId, exercise.id, null)
                    else state.activeSession?.let { session ->
                        viewModel.addExercise(session.id, exercise.id) { succeeded ->
                            if (succeeded) destination = GymDestination.Workout
                        }
                    }
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
            machines = state.machines.filter { it.supportsExercise(exercise.id) },
            onDismiss = { pendingMachineExerciseId = null; substituteWorkoutExerciseId = null },
            onChoose = { machineId ->
                val substitutionId = substituteWorkoutExerciseId
                if (substitutionId != null) viewModel.substituteWorkoutExercise(substitutionId, exercise.id, machineId)
                else state.activeSession?.let { session ->
                    viewModel.addExercise(session.id, exercise.id, machineId) { succeeded ->
                        if (succeeded) destination = GymDestination.Workout
                    }
                }
                substituteWorkoutExerciseId = null
                pendingMachineExerciseId = null
            },
        )
    }

    if (showStartWorkout) {
        WorkoutEditorDialog(
            modifier = dialogModifier,
            session = null,
            initialDate = LocalWhipToday.current,
            initialKeepAwake = state.appSettings.keepScreenAwake,
            onDismiss = { showStartWorkout = false },
            onStart = { name, notes, date, keepAwake, onFinished ->
                viewModel.startWorkout(name, notes, date, keepAwake) { succeeded ->
                    if (succeeded) destination = GymDestination.Workout
                    onFinished(gymPersistenceResult(succeeded, viewModel.operationStatus.value))
                }
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
                onStart = { name, notes, _, keepAwake, onFinished ->
                    viewModel.updateWorkout(session.id, name, notes, keepAwake) { succeeded ->
                        onFinished(gymPersistenceResult(succeeded, viewModel.operationStatus.value))
                    }
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
            onStart = { name, notes, _, keepAwake, onFinished ->
                viewModel.updateWorkout(session.id, name, notes, keepAwake) { succeeded ->
                    onFinished(gymPersistenceResult(succeeded, viewModel.operationStatus.value))
                }
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
        val incompleteSets = state.activeWorkoutExercises.sumOf { item -> item.sets.count(WorkoutSet::isIncompleteRequiredWork) }
        val finishingSession = state.activeSession
        val programmedSession = finishingSession != null &&
            finishingSession.sourceRoutineProgramKind != RoutineProgramKind.Static
        val heldMainLiftNames = state.activeWorkoutExercises.mapNotNull { item ->
            val required = item.sets.filter { it.workSectionSnapshot == RoutineWorkSection.Main }
            val missed = required.isNotEmpty() && required.any { set ->
                !set.completed || set.deletedAtMillis != null ||
                    set.classification == WorkoutSetClassification.Failure ||
                    (set.prescribedRepetitions != null &&
                        (set.repetitions ?: Int.MIN_VALUE) < set.prescribedRepetitions) ||
                    (set.prescribedCanonicalWeightKg != null &&
                        (set.canonicalWeightKg ?: Double.NEGATIVE_INFINITY) + 1e-9 < set.prescribedCanonicalWeightKg)
            }
            item.exercise.name.takeIf { missed }
        }.distinct()
        val progressionMessage = if (programmedSession) {
            if (finishingSession.requiredMainWorkInvalidated) {
                val names = finishingSession.invalidatedMainExerciseIds.mapNotNull { exerciseId ->
                    (state.exercises + state.archivedExercises).firstOrNull { it.id == exerciseId }?.name
                }
                " Program position will advance, but Training Max progression is held" +
                    (names.takeIf { it.isNotEmpty() }?.joinToString(prefix = " only for ") ?: " for affected Main work") +
                    " because a prescribed Main lift was removed or substituted."
            } else if (heldMainLiftNames.isNotEmpty()) {
                " Program position will advance; Training Max progression is held only for ${heldMainLiftNames.joinToString()} because required Main work was incomplete, failed, under reps, or under load."
            } else {
                " Program position will advance; Training Max increases occur only at configured boundaries."
            }
        } else ""
        ConfirmationDialog(
            modifier = dialogModifier,
            title = "Review and Finish Workout?",
            message = "${quantityLabel(state.activeWorkoutExercises.size, "exercise")} · ${quantityLabel(completedSets, "completed set")}" +
                (if (incompleteSets > 0) {
                    " · $incompleteSets planned sets remain incomplete and stay visible in History."
                } else {
                    ". History preserves every saved value and equipment snapshot. Resume the workout to change individual set values."
                }) + progressionMessage,
            confirmLabel = "Finish",
            onDismiss = { finishConfirmation = false },
            onConfirm = {
                finishConfirmation = false
                if (state.activeFiveThreeOneCycleReview() != null) {
                    trainingMaxCycleReviewOpen = true
                } else {
                    state.activeSession?.let { session ->
                        viewModel.finishWorkout(session.id)
                    }
                }
            },
        )
    }
    if (trainingMaxCycleReviewOpen) {
        val review = state.activeFiveThreeOneCycleReview()
        val session = state.activeSession
        if (review != null && session != null) {
            FiveThreeOneCycleReviewDialog(
                review = review,
                modifier = dialogModifier,
                onDismiss = { trainingMaxCycleReviewOpen = false },
                onApply = { decisions ->
                    viewModel.finishWorkout(session.id, decisions) { succeeded ->
                        if (succeeded) trainingMaxCycleReviewOpen = false
                    }
                },
            )
        }
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
            onCreate = { name, type, ids, onFinished ->
                val session = state.activeSession
                if (session == null) {
                    onFinished(WhipResult.Failure(""))
                } else {
                    viewModel.createGroup(
                        session.id,
                        name,
                        type,
                        ids,
                    ) { succeeded ->
                        onFinished(gymPersistenceResult(succeeded, viewModel.operationStatus.value))
                    }
                }
            },
        )
    }
}

@Composable
private fun GymLibraryLanding(onOpen: (GymDestination) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("gym-library-list"),
        contentPadding = WhipPageContentPadding,
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.compact),
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
    nextExerciseByGroup: Map<Long, Long?> = nextExerciseRotation(items),
    acceptedOptionalSetIds: Set<Long> = emptySet(),
): Pair<WorkoutExerciseUi, WorkoutSet>? {
    fun priority(set: WorkoutSet): Int? = when (set.workSectionSnapshot) {
        RoutineWorkSection.Main -> 0
        RoutineWorkSection.Optional -> 0.takeIf { set.id in acceptedOptionalSetIds }
        RoutineWorkSection.Supplemental -> 1
        RoutineWorkSection.Assistance,
        RoutineWorkSection.Unspecified,
        -> 2
    }
    fun firstIncomplete(item: WorkoutExerciseUi, targetPriority: Int) = item.sets.sortedBy(WorkoutSet::position)
        .firstOrNull { set -> !set.completed && set.deletedAtMillis == null && priority(set) == targetPriority }

    val executionOrder = buildWorkoutExerciseBlocks(items).flatMap(WorkoutExerciseBlock::exercises)
    fun respectsRotation(item: WorkoutExerciseUi): Boolean {
        val designatedGroupMemberId = item.group?.let { nextExerciseByGroup[it.id] }
        return designatedGroupMemberId == null || designatedGroupMemberId == item.workoutExercise.id
    }
    fun nextForItem(item: WorkoutExerciseUi, priorities: IntRange): Pair<WorkoutExerciseUi, WorkoutSet>? =
        priorities.firstNotNullOfOrNull { targetPriority ->
            firstIncomplete(item, targetPriority)?.let { set -> item to set }
        }

    // Finish each programmed lift's Main → accepted Optional → Supplemental work before moving
    // to the next bar/equipment station. Global section priority caused Squat Main → Bench Main →
    // Squat FSL, which is expensive and surprising during a real workout.
    val programmed = executionOrder.filter { item -> item.sets.any { priority(it) in 0..1 } }
    programmed.firstNotNullOfOrNull { item ->
        nextForItem(item, 0..1).takeIf { respectsRotation(item) }
    }?.let { return it }
    programmed.firstNotNullOfOrNull { item -> nextForItem(item, 0..1) }?.let { return it }

    // Assistance retains group rotation (supersets/circuits) after programmed work is complete.
    executionOrder.firstNotNullOfOrNull { item ->
        firstIncomplete(item, 2)?.let { set -> item to set }.takeIf { respectsRotation(item) }
    }?.let { return it }
    return executionOrder.firstNotNullOfOrNull { item ->
        firstIncomplete(item, 2)?.let { set -> item to set }
    }
}

internal fun WorkoutSet.isIncompleteRequiredWork(): Boolean =
    !completed && deletedAtMillis == null && workSectionSnapshot != RoutineWorkSection.Optional

internal fun selectPendingOptionalWorkoutSet(
    items: List<WorkoutExerciseUi>,
    acceptedOptionalSetIds: Set<Long> = emptySet(),
): Pair<WorkoutExerciseUi, WorkoutSet>? = buildWorkoutExerciseBlocks(items)
    .flatMap(WorkoutExerciseBlock::exercises)
    .asSequence()
    .mapNotNull { item ->
        val mainWorkComplete = item.sets.none { set ->
            set.workSectionSnapshot == RoutineWorkSection.Main && !set.completed && set.deletedAtMillis == null
        }
        if (!mainWorkComplete) return@mapNotNull null
        item.sets.sortedBy(WorkoutSet::position).firstOrNull { set ->
            set.workSectionSnapshot == RoutineWorkSection.Optional &&
                !set.completed && set.deletedAtMillis == null && set.id !in acceptedOptionalSetIds
        }?.let { set -> item to set }
    }
    .firstOrNull()

internal fun nextExerciseRotation(items: List<WorkoutExerciseUi>): Map<Long, Long?> = items
    .filter { it.group != null }
    .groupBy { requireNotNull(it.group).id }
    .filterValues { members -> members.size >= 2 }
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

internal data class WorkoutExerciseBlock(
    val group: WorkoutGroup?,
    val exercises: List<WorkoutExerciseUi>,
) {
    val key: String = group?.let { "group-${it.id}" }
        ?: "exercise-${exercises.single().workoutExercise.id}"
}

internal fun buildWorkoutExerciseBlocks(items: List<WorkoutExerciseUi>): List<WorkoutExerciseBlock> {
    val ordered = items.sortedBy { it.workoutExercise.position }
    val groupCounts = ordered.mapNotNull { it.group?.id }.groupingBy { it }.eachCount()
    val emittedGroupIds = mutableSetOf<Long>()
    return buildList {
        ordered.forEach { item ->
            val group = item.group?.takeIf { (groupCounts[it.id] ?: 0) >= 2 }
            if (group == null) {
                add(WorkoutExerciseBlock(group = null, exercises = listOf(item)))
            } else if (emittedGroupIds.add(group.id)) {
                add(
                    WorkoutExerciseBlock(
                        group = group,
                        exercises = ordered.filter { it.group?.id == group.id },
                    ),
                )
            }
        }
    }
}

internal fun WorkoutGroup.customDisplayName(): String? = name.trim()
    .takeIf(String::isNotBlank)
    ?.takeUnless { candidate -> WorkoutGroupType.entries.any { candidate.equals(it.name, ignoreCase = true) } }

internal fun reorderWorkoutBlock(
    blocks: List<WorkoutExerciseBlock>,
    blockIndex: Int,
    delta: Int,
): List<Long> {
    if (blockIndex !in blocks.indices) return blocks.flattenWorkoutExerciseIds()
    return moveListItem(blocks, blockIndex, delta).flattenWorkoutExerciseIds()
}

internal fun reorderWorkoutGroupMember(
    blocks: List<WorkoutExerciseBlock>,
    blockIndex: Int,
    memberIndex: Int,
    delta: Int,
): List<Long> {
    val block = blocks.getOrNull(blockIndex) ?: return blocks.flattenWorkoutExerciseIds()
    if (memberIndex !in block.exercises.indices) {
        return blocks.flattenWorkoutExerciseIds()
    }
    val reorderedMembers = moveListItem(block.exercises, memberIndex, delta)
    return blocks.mapIndexed { index, candidate ->
        if (index == blockIndex) candidate.copy(exercises = reorderedMembers) else candidate
    }.flattenWorkoutExerciseIds()
}

private fun List<WorkoutExerciseBlock>.flattenWorkoutExerciseIds(): List<Long> =
    flatMap { block -> block.exercises.map { it.workoutExercise.id } }

@Composable
private fun WorkoutContent(
    state: GymUiState,
    requestedWorkoutExerciseId: Long? = null,
    onRequestedWorkoutExerciseConsumed: () -> Unit = {},
    onStart: () -> Unit,
    onOpenRoutines: () -> Unit,
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
    onRemoveFromGroup: (Long) -> Unit,
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = WhipPageContentPadding,
            verticalArrangement = Arrangement.spacedBy(WhipSpacing.compact),
        ) {
            item {
                WhipPageHeader(
                    title = "Current Workout",
                    supportingText = "Log sets with fast, repeatable controls while you train.",
                )
            }
            item {
                WhipEmptyState(
                    title = "No Workout in Progress",
                    supportingText = if (state.exercises.isEmpty()) {
                        "Create exercises once, then reuse them in workouts and routines. You can also start empty and create one while logging."
                    } else if (state.routines.isNotEmpty()) {
                        "Start from a routine for planned sets, or start an empty workout and build it as you train."
                    } else {
                        "Start a workout, then choose from your reusable exercise library."
                    },
                    primaryActionLabel = when {
                        state.exercises.isEmpty() -> "Create First Exercise"
                        state.routines.isNotEmpty() -> "Start from Routine"
                        else -> "Start Workout"
                    },
                    onPrimaryAction = when {
                        state.exercises.isEmpty() -> onCreateExercise
                        state.routines.isNotEmpty() -> onOpenRoutines
                        else -> onStart
                    },
                    secondaryActionLabel = when {
                        state.exercises.isEmpty() -> "Start Empty Workout"
                        state.routines.isNotEmpty() -> "Start Empty Workout"
                        else -> null
                    },
                    onSecondaryAction = onStart.takeIf { state.exercises.isEmpty() || state.routines.isNotEmpty() },
                )
            }
        }
        return
    }

    var acceptedOptionalSetIds by rememberSaveable(session.id) { mutableStateOf<List<Long>>(emptyList()) }
    var skippedOptionalSetId by rememberSaveable(session.id) { mutableStateOf<Long?>(null) }
    val acceptedOptionalIds = acceptedOptionalSetIds.toSet()
    val pendingOptionalSet = selectPendingOptionalWorkoutSet(state.activeWorkoutExercises, acceptedOptionalIds)
    val nextExerciseByGroup = nextExerciseRotation(state.activeWorkoutExercises)
    val nextSet = if (pendingOptionalSet == null) {
        selectNextWorkoutSet(state.activeWorkoutExercises, nextExerciseByGroup, acceptedOptionalIds)
    } else {
        null
    }
    val workoutBlocks = remember(state.activeWorkoutExercises) {
        buildWorkoutExerciseBlocks(state.activeWorkoutExercises)
    }
    val workoutListState = rememberLazyListState()
    val workoutScrollScope = rememberCoroutineScope()
    LaunchedEffect(requestedWorkoutExerciseId, workoutBlocks) {
        val requestedId = requestedWorkoutExerciseId ?: return@LaunchedEffect
        val blockIndex = workoutBlocks.indexOfFirst { block ->
            block.exercises.any { it.workoutExercise.id == requestedId }
        }
        if (blockIndex >= 0) workoutListState.scrollToItem(blockIndex + 2)
        onRequestedWorkoutExerciseConsumed()
    }
    var lastFocusedSetId by rememberSaveable(session.id) { mutableStateOf(nextSet?.second?.id) }
    LaunchedEffect(nextSet?.second?.id) {
        val next = nextSet ?: return@LaunchedEffect
        if (lastFocusedSetId != null && lastFocusedSetId != next.second.id) {
            val blockIndex = workoutBlocks.indexOfFirst { block ->
                block.exercises.any { it.workoutExercise.id == next.first.workoutExercise.id }
            }
            if (blockIndex >= 0) workoutListState.animateScrollToItem(blockIndex + 2)
        }
        lastFocusedSetId = next.second.id
    }
    var workoutRestOverrideSeconds by rememberSaveable(session.id) { mutableStateOf<Int?>(null) }

    WhipReorderLazyColumn(
        modifier = Modifier.fillMaxSize().testTag("active-workout-list"),
        state = workoutListState,
        contentPadding = WhipPageContentPadding,
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.compact),
    ) {
        item {
            WhipPageHeader(
                title = session.name.ifBlank { "Workout" },
                supportingText = session.localDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
            ) {
                WhipTextButton(onClick = onEditWorkout) { Text("Edit Workout") }
            }
            if (session.sourceRoutineProgramKind != RoutineProgramKind.Static) {
                val programLabel = when (session.sourceRoutineProgramKind) {
                    RoutineProgramKind.Static -> "Routine"
                    RoutineProgramKind.Custom -> "Program"
                    RoutineProgramKind.FiveThreeOne -> "5/3/1"
                    RoutineProgramKind.FiveThreeOneClassic -> "Classic 5/3/1"
                    RoutineProgramKind.FiveSPro -> "5s PRO"
                    RoutineProgramKind.BoringButBig -> "Boring But Big"
                    RoutineProgramKind.FirstSetLast -> "First Set Last"
                }
                val phaseLabel = session.sourceRoutinePhaseLabel.takeIf(String::isNotBlank)
                    ?: session.sourceRoutinePhaseIndex?.let { "Phase ${it + 1}" }
                val phaseRole = when (session.sourceRoutinePhaseRole) {
                    RoutineProgramPhaseRole.Standard -> null
                    RoutineProgramPhaseRole.Leader -> "Leader"
                    RoutineProgramPhaseRole.Anchor -> "Anchor"
                    RoutineProgramPhaseRole.Deload -> "Deload"
                    RoutineProgramPhaseRole.TrainingMaxTest -> "Training Max Test"
                    RoutineProgramPhaseRole.PersonalRecordTest -> "PR Test"
                }
                Text(
                    listOfNotNull(
                        programLabel,
                        session.sourceRoutineCycle?.let { "Cycle $it" },
                        phaseLabel,
                        phaseRole,
                        session.sourceRoutineDayPosition?.let { "Day ${it + 1}" },
                    ).distinct().joinToString(" · "),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.testTag("active-workout-program-context"),
                )
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
                    skippedOptionalSetId?.let { skippedId ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Optional Joker skipped", modifier = Modifier.weight(1f))
                            WhipTextButton(
                                onClick = {
                                    onUndoDeleteSet(skippedId)
                                    skippedOptionalSetId = null
                                },
                                modifier = Modifier.testTag("undo-skip-optional-set"),
                            ) { Text("Undo") }
                        }
                    }
                    pendingOptionalSet?.let { (exerciseItem, set) ->
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                "OPTIONAL · ${exerciseItem.exercise.name} · Joker candidate",
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                set.prescriptionLabel(
                                    state.appSettings.gymWeightUnitId,
                                    state.appSettings.numberPrecision,
                                    exerciseItem.workoutExercise,
                                )?.let { "Target · $it · Perform only if readiness and bar speed justify it." }
                                    ?: "Perform only if readiness and bar speed justify it.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WhipButton(
                                    onClick = {
                                        skippedOptionalSetId = null
                                        acceptedOptionalSetIds = acceptedOptionalSetIds + set.id
                                    },
                                    modifier = Modifier.weight(1f).testTag("perform-optional-set"),
                                ) { Text("Perform Joker") }
                                WhipOutlinedButton(
                                    onClick = {
                                        skippedOptionalSetId = set.id
                                        onDeleteSet(set.id)
                                    },
                                    modifier = Modifier.weight(1f).testTag("skip-optional-set"),
                                ) { Text("Skip") }
                            }
                        }
                    }
                    nextSet?.let { (exerciseItem, set) ->
                        Text(
                            "NEXT · ${exerciseItem.exercise.name} · Set ${set.position + 1}" +
                                set.prescriptionLabel(state.appSettings.gymWeightUnitId, state.appSettings.numberPrecision, exerciseItem.workoutExercise)?.let { " · $it" }.orEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClickLabel = "Jump to next incomplete set") {
                                    val blockIndex = workoutBlocks.indexOfFirst { block ->
                                        block.exercises.any { it.workoutExercise.id == exerciseItem.workoutExercise.id }
                                    }
                                    if (blockIndex >= 0) workoutScrollScope.launch {
                                        workoutListState.scrollToItem(blockIndex + 2)
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
                    supportingText = "Choose a reusable exercise from your library or create one without leaving this workout. This changes only this workout; logged work remains in History.",
                    primaryActionLabel = "Add Exercise to This Workout",
                    onPrimaryAction = onAddExercise,
                    secondaryActionLabel = "Create New Exercise",
                    onSecondaryAction = onCreateExercise,
                )
            }
        } else {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    WhipOutlinedButton(
                        onClick = onAddExercise,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add-exercise-to-active-workout"),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Exercise to This Workout")
                    }
                    Text(
                        if (session.sourceRoutineId != null) {
                            "Workout only · Your routine and future scheduled workouts stay unchanged. Logged sets remain in this workout's History."
                        } else {
                            "Workout only · Logged sets remain with this workout in History."
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("active-workout-exercise-scope"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(workoutBlocks, key = WorkoutExerciseBlock::key) { block ->
            val blockIndex = workoutBlocks.indexOf(block)
            val renderExercise: @Composable (WorkoutExerciseUi, Int) -> Unit = { item, memberIndex ->
                val effectiveShowRpe = item.exercise.showRpe ?: state.appSettings.showGymRpe
                val isGrouped = block.group != null
                val canMoveUp = if (isGrouped) memberIndex > 0 else blockIndex > 0
                val canMoveDown = if (isGrouped) memberIndex < block.exercises.lastIndex else blockIndex < workoutBlocks.lastIndex
                val reorder: (Int) -> Unit = { delta ->
                    val ids = if (isGrouped) {
                        reorderWorkoutGroupMember(workoutBlocks, blockIndex, memberIndex, delta)
                    } else {
                        reorderWorkoutBlock(workoutBlocks, blockIndex, delta)
                    }
                    onReorderExercises(session.id, ids)
                }
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
                    canMoveUp = canMoveUp,
                    canMoveDown = canMoveDown,
                    reorderPosition = if (isGrouped) memberIndex + 1 else blockIndex + 1,
                    reorderTotal = if (isGrouped) block.exercises.size else workoutBlocks.size,
                    onMoveUp = { reorder(-1) },
                    onMoveDown = { reorder(1) },
                    onMoveBy = reorder,
                    onRemoveExercise = { onRemoveExercise(item.workoutExercise.id) },
                    onRemoveFromGroup = { onRemoveFromGroup(item.workoutExercise.id) },
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

            val group = block.group
            if (group == null) {
                renderExercise(block.exercises.single(), 0)
            } else {
                WorkoutExerciseGroupSurface(
                    group = group,
                    exerciseCount = block.exercises.size,
                    canMoveUp = blockIndex > 0,
                    canMoveDown = blockIndex < workoutBlocks.lastIndex,
                    position = blockIndex + 1,
                    total = workoutBlocks.size,
                    onMoveUp = {
                        onReorderExercises(session.id, reorderWorkoutBlock(workoutBlocks, blockIndex, -1))
                    },
                    onMoveDown = {
                        onReorderExercises(session.id, reorderWorkoutBlock(workoutBlocks, blockIndex, 1))
                    },
                    onMoveBy = { delta ->
                        onReorderExercises(session.id, reorderWorkoutBlock(workoutBlocks, blockIndex, delta))
                    },
                ) {
                    block.exercises.forEachIndexed { memberIndex, item ->
                        renderExercise(item, memberIndex)
                    }
                }
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
internal fun WorkoutExerciseGroupSurface(
    group: WorkoutGroup,
    exerciseCount: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    position: Int? = null,
    total: Int? = null,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveBy: (Int) -> Unit = { delta -> if (delta < 0) onMoveUp() else onMoveDown() },
    content: @Composable () -> Unit,
) {
    val reorderInteraction = rememberWhipReorderInteractionState()
    val groupLabel = group.type.uiLabel()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .whipReorderItem(
                reorderInteraction,
                layoutPosition = position,
                layoutScope = "workout-blocks",
            )
            .testTag("workout-group-${group.id}")
            .semantics {
                contentDescription = "$groupLabel group, ${quantityLabel(exerciseCount, "exercise")}"
            },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WhipReorderHandle(
                    label = "$groupLabel group",
                    canMovePrevious = canMoveUp,
                    canMoveNext = canMoveDown,
                    position = position,
                    total = total,
                    interactionState = reorderInteraction,
                    moveWholeItem = true,
                    layoutScope = "workout-blocks",
                    onMove = onMoveBy,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(groupLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        listOfNotNull(
                            group.customDisplayName(),
                            quantityLabel(exerciseCount, "exercise"),
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            WhipReorderLayout(itemSpacing = 8.dp) { content() }
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
    reorderPosition: Int? = null,
    reorderTotal: Int? = null,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveBy: (Int) -> Unit = { delta -> if (delta < 0) onMoveUp() else onMoveDown() },
    onRemoveExercise: () -> Unit,
    onRemoveFromGroup: () -> Unit = {},
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
    var removeConfirmation by rememberSaveable(item.workoutExercise.id) { mutableStateOf(false) }
    var substituteConfirmation by rememberSaveable(item.workoutExercise.id) { mutableStateOf(false) }
    var setMenuId by rememberSaveable { mutableStateOf<Long?>(null) }
    var setupExpanded by rememberSaveable(item.workoutExercise.id) { mutableStateOf(false) }
    var completedSetsExpanded by rememberSaveable(item.workoutExercise.id) { mutableStateOf(false) }
    val reorderInteraction = rememberWhipReorderInteractionState()
    val exerciseReorderScope = item.group?.let { "workout-group-${it.id}" } ?: "workout-blocks"
    Card(
        modifier = Modifier.fillMaxWidth().whipReorderItem(
            reorderInteraction,
            layoutPosition = reorderPosition,
            layoutScope = exerciseReorderScope,
        ),
    ) {
        Column(modifier = Modifier.padding(if (compactRows) 9.dp else 14.dp), verticalArrangement = Arrangement.spacedBy(if (compactRows) 4.dp else 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WhipReorderHandle(
                    label = item.exercise.name,
                    canMovePrevious = canMoveUp,
                    canMoveNext = canMoveDown,
                    position = reorderPosition,
                    total = reorderTotal,
                    interactionState = reorderInteraction,
                    moveWholeItem = true,
                    layoutScope = exerciseReorderScope,
                    onMove = onMoveBy,
                )
                Text(item.exercise.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Box {
                    IconButton(onClick = { actionMenuExpanded = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "More options for ${item.exercise.name}", modifier = Modifier.size(28.dp))
                    }
                    DropdownMenu(expanded = actionMenuExpanded, onDismissRequest = { actionMenuExpanded = false }) {
                        item.group?.let { group ->
                            WhipMenuItem(
                                label = "Remove from ${group.type.uiLabel()}",
                                onClick = {
                                    actionMenuExpanded = false
                                    onRemoveFromGroup()
                                },
                            )
                        }
                        WhipMenuItem(
                            label = "Substitute Exercise",
                            onClick = {
                                actionMenuExpanded = false
                                if (item.sets.any { it.deletedAtMillis == null }) substituteConfirmation = true
                                else onSubstituteExercise()
                            },
                        )
                        HorizontalDivider()
                        WhipMenuItem(
                            label = "Remove from Workout",
                            icon = Icons.Outlined.DeleteOutline,
                            role = WhipMenuItemRole.Destructive,
                            onClick = { actionMenuExpanded = false; removeConfirmation = true },
                        )
                    }
                }
            }
            item.workoutExercise.assistanceLabel()?.let { roleLabel ->
                Text(
                    roleLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            if (item.workoutExercise.machineNameSnapshot.isNotBlank()) {
                Text(
                    "Machine: ${item.workoutExercise.machineNameSnapshot}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (nextInGroup) {
                Text(
                    "Next in rotation",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                )
            }
            val orderedSets = item.sets.sortedBy(WorkoutSet::position)
            val completedSetCount = orderedSets.count { it.completed && it.deletedAtMillis == null }
            val hasIncompleteSet = orderedSets.any { !it.completed && it.deletedAtMillis == null }
            val visibleReorderSets = orderedSets.filter { set ->
                set.deletedAtMillis == null && (!set.completed || completedSetsExpanded || !hasIncompleteSet)
            }
            fun reorderVisibleSet(set: WorkoutSet, delta: Int) {
                val visibleIndex = visibleReorderSets.indexOfFirst { it.id == set.id }
                if (visibleIndex < 0) return
                val moved = moveListItem(visibleReorderSets, visibleIndex, delta)
                val iterator = moved.iterator()
                val visibleIds = visibleReorderSets.mapTo(mutableSetOf(), WorkoutSet::id)
                onReorderSets(orderedSets.map { candidate ->
                    if (candidate.id in visibleIds) iterator.next().id else candidate.id
                })
            }
            if (completedSetCount > 0 && hasIncompleteSet) {
                DisclosureButton(
                    label = quantityLabel(completedSetCount, "Completed Set"),
                    expanded = completedSetsExpanded,
                    onClick = { completedSetsExpanded = !completedSetsExpanded },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            WhipReorderLayout(itemSpacing = if (compactRows) 4.dp else 8.dp) {
            orderedSets.withIndex()
                .filter { (_, set) ->
                    !set.completed || completedSetsExpanded || !hasIncompleteSet
                }
                .forEach { (index, set) ->
                key(set.id) {
                if (set.deletedAtMillis != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Set ${index + 1} removed", modifier = Modifier.weight(1f))
                        WhipTextButton(onClick = { onUndoDeleteSet(set.id) }) { Text("Undo") }
                    }
                } else if (set.id == nextSetId) {
                    val setReorderInteraction = rememberWhipReorderInteractionState()
                    val suggestedSet = orderedSets.take(index).lastOrNull { candidate ->
                        candidate.completed && candidate.deletedAtMillis == null
                    } ?: item.previousSets.maxByOrNull { previous ->
                        previous.completedAtMillis ?: previous.updatedAtMillis
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth()
                            .whipReorderItem(
                                setReorderInteraction,
                                layoutPosition = visibleReorderSets.indexOfFirst { it.id == set.id } + 1,
                                layoutScope = "workout-sets-${item.workoutExercise.id}",
                            )
                            .testTag("active-set-composer"),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val visibleIndex = visibleReorderSets.indexOfFirst { it.id == set.id }
                                WhipReorderHandle(
                                    label = "set ${index + 1}",
                                    canMovePrevious = visibleIndex > 0,
                                    canMoveNext = visibleIndex in 0 until visibleReorderSets.lastIndex,
                                    position = visibleIndex + 1,
                                    total = visibleReorderSets.size,
                                    interactionState = setReorderInteraction,
                                    moveWholeItem = true,
                                    layoutScope = "workout-sets-${item.workoutExercise.id}",
                                    onMove = { delta -> reorderVisibleSet(set, delta) },
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    val executionLabel = when {
                                        set.optionalWorkKindSnapshot == RoutineOptionalWorkKind.Joker -> "Optional · Joker"
                                        set.workSectionSnapshot == RoutineWorkSection.Main -> "Main Work"
                                        set.workSectionSnapshot == RoutineWorkSection.Supplemental -> "Supplemental Work"
                                        set.workSectionSnapshot == RoutineWorkSection.Assistance -> "Assistance Work"
                                        else -> "Up Next"
                                    }
                                    Text(
                                        "$executionLabel · Set ${index + 1}${set.classification.uiLabel().takeUnless { it == "Working" }?.let { " · $it" }.orEmpty()}",
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
                                            contentDescription = "Manage set ${index + 1}",
                                            modifier = Modifier.size(26.dp),
                                        )
                                    }
                                    WorkoutSetActionsMenu(
                                        expanded = setMenuId == set.id,
                                        onDismiss = { setMenuId = null },
                                        onDuplicate = { setMenuId = null; onDuplicateSet(set.id) },
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
                } else {
                    val setReorderInteraction = rememberWhipReorderInteractionState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .whipReorderItem(
                                setReorderInteraction,
                                layoutPosition = visibleReorderSets.indexOfFirst { it.id == set.id } + 1,
                                layoutScope = "workout-sets-${item.workoutExercise.id}",
                            )
                            .clickable(onClickLabel = "Edit set ${index + 1}") { onEditSet(set) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val visibleIndex = visibleReorderSets.indexOfFirst { it.id == set.id }
                        WhipReorderHandle(
                            label = "set ${index + 1}",
                            canMovePrevious = visibleIndex > 0,
                            canMoveNext = visibleIndex in 0 until visibleReorderSets.lastIndex,
                            position = visibleIndex + 1,
                            total = visibleReorderSets.size,
                            interactionState = setReorderInteraction,
                            moveWholeItem = true,
                            layoutScope = "workout-sets-${item.workoutExercise.id}",
                            onMove = { delta -> reorderVisibleSet(set, delta) },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                buildString {
                                    if (set.optionalWorkKindSnapshot == RoutineOptionalWorkKind.Joker) append("Optional Joker · ")
                                    else when (set.workSectionSnapshot) {
                                        RoutineWorkSection.Main -> append("Main · ")
                                        RoutineWorkSection.Supplemental -> append("Supplemental · ")
                                        RoutineWorkSection.Assistance -> append("Assistance · ")
                                        else -> Unit
                                    }
                                    append("Set ${index + 1} · ")
                                    append(set.shortLabel(preferredWeightUnitId, preferredDistanceUnitId, numberPrecision, item.workoutExercise, item.exercise.weightUnitId).replace("Empty set", "Ready"))
                                },
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
                                Icon(Icons.Outlined.MoreVert, contentDescription = "Manage set ${index + 1}", modifier = Modifier.size(26.dp))
                            }
                            WorkoutSetActionsMenu(
                                expanded = setMenuId == set.id,
                                onDismiss = { setMenuId = null },
                                onDuplicate = { setMenuId = null; onDuplicateSet(set.id) },
                                onRemove = { setMenuId = null; onDeleteSet(set.id) },
                            )
                        }
                        if (set.completed) {
                            WhipCompletionCheckbox(
                                checked = true,
                                onCheckedChange = { onCompleteSet(set.id, false) },
                                modifier = Modifier.semantics { contentDescription = "Mark set ${index + 1} incomplete" },
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .semantics { contentDescription = "Incomplete set ${index + 1}; enter its required values to save" },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Outlined.RadioButtonUnchecked, contentDescription = null)
                            }
                        }
                    }
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
    if (removeConfirmation) {
        val savedSetCount = item.sets.count { it.deletedAtMillis == null }
        ConfirmationDialog(
            title = "Remove ${item.exercise.name}?",
            message = if (savedSetCount == 0) {
                "This removes the exercise from the active workout."
            } else {
                "This removes ${quantityLabel(savedSetCount, "saved set")} from the active workout."
            },
            confirmLabel = "Remove Exercise",
            onDismiss = { removeConfirmation = false },
            onConfirm = { removeConfirmation = false; onRemoveExercise() },
        )
    }
    if (substituteConfirmation) {
        val savedSetCount = item.sets.count { it.deletedAtMillis == null }
        ConfirmationDialog(
            title = "Replace ${item.exercise.name}?",
            message = "Choosing a replacement removes ${quantityLabel(savedSetCount, "saved set")} from this workout. This cannot be undone.",
            confirmLabel = "Choose Replacement",
            onDismiss = { substituteConfirmation = false },
            onConfirm = {
                substituteConfirmation = false
                onSubstituteExercise()
            },
        )
    }
}

@Composable
private fun WorkoutSetActionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onDuplicate: () -> Unit,
    onRemove: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        WhipMenuItem(label = "Duplicate Set", onClick = onDuplicate)
        HorizontalDivider()
        WhipMenuItem(
            label = "Remove Set",
            icon = Icons.Outlined.DeleteOutline,
            role = WhipMenuItemRole.Destructive,
            onClick = onRemove,
        )
    }
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
    onMoreDetails: () -> Unit,
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
        workSection = set.workSectionSnapshot,
        optionalWorkKind = set.optionalWorkKindSnapshot,
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
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val stackActions = LocalDensity.current.fontScale >= 1.5f || maxWidth < 330.dp
                val timerActions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
                    if (remaining == null) {
                        WhipTextButton(
                            onClick = { showDurationEditor = true },
                            modifier = Modifier.semantics { contentDescription = "Adjust rest time for this workout" },
                        ) {
                            Text("Adjust")
                        }
                        WhipTextButton(
                            onClick = { onStart(session.id, selectedSeconds.coerceAtLeast(1)) },
                            modifier = Modifier.semantics { contentDescription = "Start rest timer" },
                        ) {
                            Text("Start")
                        }
                    } else {
                        WhipTextButton(
                            onClick = { onAdjust(session.id, -15) },
                            modifier = Modifier.semantics { contentDescription = "Subtract 15 seconds from rest timer" },
                        ) { Text("−15") }
                        WhipTextButton(
                            onClick = { onAdjust(session.id, 15) },
                            modifier = Modifier.semantics { contentDescription = "Add 15 seconds to rest timer" },
                        ) { Text("+15") }
                        WhipTextButton(
                            onClick = { onStop(session.id) },
                            modifier = Modifier.semantics { contentDescription = "Stop rest timer" },
                        ) { Text("Stop") }
                    }
                }
                if (stackActions) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            "Rest · ${formatDuration((remaining ?: selectedSeconds).coerceAtLeast(1).toLong())}",
                            fontWeight = FontWeight.Bold,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            content = timerActions,
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Rest · ${formatDuration((remaining ?: selectedSeconds).coerceAtLeast(1).toLong())}",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        timerActions()
                    }
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
    var presetDraft by rememberSaveable(presetSeconds) { mutableStateOf(normalizeRestTimerPresets(presetSeconds)) }
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
                        "This changes rest timers started during the current workout. Your default remains in Settings → Planning & Units → Gym Defaults.",
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
    onReorder: (List<Long>) -> Unit,
    onReorderModeChange: (Boolean) -> Unit = {},
    reorderDismissRequest: Int = 0,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var selectedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedTrackingType by rememberSaveable { mutableStateOf<ExerciseTrackingType?>(null) }
    var selectedEquipment by rememberSaveable { mutableStateOf<String?>(null) }
    var sort by rememberSaveable { mutableStateOf(ExerciseLibrarySort.Name) }
    var sortDirection by rememberSaveable { mutableStateOf(SortDirection.Ascending) }
    var reordering by rememberSaveable { mutableStateOf(false) }
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
    }.sortedForLibrary(sort, sortDirection, lastUsedAtByExercise)
    val activeFilterCount = listOf(favoritesOnly, showArchived, selectedCategoryId != null, selectedTrackingType != null, selectedEquipment != null).count { it }
    val reorderHasConstraints = query.isNotBlank() || activeFilterCount > 0
    val manualReorderEnabled = reordering && sort == ExerciseLibrarySort.Manual && !reorderHasConstraints
    BackHandler(enabled = reordering) { reordering = false }
    DisposableEffect(reordering) {
        onReorderModeChange(reordering)
        onDispose { if (reordering) onReorderModeChange(false) }
    }
    LaunchedEffect(reorderDismissRequest) {
        if (reorderDismissRequest > 0) reordering = false
    }
    LaunchedEffect(sort, reorderHasConstraints) {
        if (sort != ExerciseLibrarySort.Manual || reorderHasConstraints) reordering = false
    }
    WhipReorderLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = WhipPageContentPadding,
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.compact),
    ) {
        item {
            WhipPageHeader(
                title = "Exercise Library",
                supportingText = "Only your exercises appear here—Whip never seeds a movement list.",
            ) {
                if (!reordering && state.exercises.size > 1) {
                    WhipPageIconAction(
                        icon = Icons.Outlined.DragHandle,
                        label = if (reorderHasConstraints) "Clear filters and reorder all Exercises" else "Reorder Exercises",
                        onClick = {
                            query = ""
                            favoritesOnly = false
                            showArchived = false
                            selectedCategoryId = null
                            selectedTrackingType = null
                            selectedEquipment = null
                            sort = ExerciseLibrarySort.Manual
                            sortDirection = SortDirection.Ascending
                            reordering = true
                        },
                    )
                }
            }
        }
        if (reordering) item {
            WhipReorderModeBar(itemLabel = "Exercises", onDone = { reordering = false })
        }
        if (!reordering) item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search Exercises") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("exercise-library-search"),
            )
        }
        if (!reordering) item {
            DisclosureRow(
                title = "Filters and Sort",
                supportingText = listOfNotNull(
                    "$activeFilterCount active".takeIf { activeFilterCount > 0 },
                    sort.label,
                    sortDirection.label,
                ).joinToString(" · "),
                expanded = filtersExpanded,
                onClick = { filtersExpanded = !filtersExpanded },
            )
        }
        if (!reordering && filtersExpanded) item {
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
                GymEnumDropdown("Sort By", ExerciseLibrarySort.entries, sort, ExerciseLibrarySort::label) { selected ->
                    sort = selected
                    if (selected == ExerciseLibrarySort.Manual) {
                        sortDirection = SortDirection.Ascending
                        if (!reorderHasConstraints) {
                            filtersExpanded = false
                            reordering = true
                        }
                    }
                }
                if (sort != ExerciseLibrarySort.Manual) {
                    GymEnumDropdown("Order", SortDirection.entries, sortDirection, SortDirection::label) { sortDirection = it }
                }
            }
        }
        if (!reordering && sort == ExerciseLibrarySort.Manual && !manualReorderEnabled) item {
            Text(
                if (reorderHasConstraints) {
                    "Clear search and filters to reorder the complete Exercise Library without moving hidden items."
                } else {
                    "Select Custom Order to reorder exercises."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!reordering && activeFilterCount > 0) item {
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
        items(visible.size, key = { visible[it].id }) { index ->
            val exercise = visible[index]
            val reorderInteraction = rememberWhipReorderInteractionState()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .whipReorderItem(
                        reorderInteraction,
                        layoutPosition = index + 1,
                        layoutScope = "exercise-browse",
                    )
                    .then(
                        if (manualReorderEnabled) Modifier
                        else Modifier.clickable(onClickLabel = "Open ${exercise.name}") { onOpen(exercise) },
                    ),
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (manualReorderEnabled) {
                        WhipReorderHandle(
                            label = exercise.name,
                            canMovePrevious = index > 0,
                            canMoveNext = index < visible.lastIndex,
                            position = index + 1,
                            total = visible.size,
                            interactionState = reorderInteraction,
                            moveWholeItem = true,
                            layoutScope = "exercise-browse",
                            reserveWhenUnavailable = true,
                            onMove = { delta -> onReorder(moveListItem(visible, index, delta).map(Exercise::id)) },
                        )
                    }
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
                        val trackedDetail = if (state.appSettings.trackedGymRecords.any {
                                it.exerciseUuid == exercise.uuid && it.type in exercise.supportedTrackedRecordTypes()
                            }
                        ) {
                            " · Tracked"
                        } else ""
                        Text(
                            exercise.trackingType.label.uiTitleCase() + unitDetail + trackedDetail,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!manualReorderEnabled) ItemEditButton("exercise", exercise.name, onEdit = { onEdit(exercise) })
                }
            }
        }
    }
}

@Composable
private fun MachineLibraryContent(
    state: GymUiState,
    onCreate: () -> Unit,
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
        contentPadding = WhipPageContentPadding,
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.compact),
    ) {
        item {
            WhipPageHeader(
                title = "Machines",
                supportingText = "Give each physical machine its own profile, then link every exercise you perform on it. Each profile keeps its history and progress tied to the equipment you used.",
            )
        }
        item { ToggleRow("Show archived", showArchived) { showArchived = it } }
        if (visible.isEmpty()) item {
            WhipEmptyState(
                title = if (showArchived) "No Archived Machines" else "No Machine Profiles",
                supportingText = "Create the machine now. Exercises are optional and can be created or linked inside the machine editor.",
                primaryActionLabel = "Create Machine",
                onPrimaryAction = onCreate,
            )
        }
        items(visible, key = GymMachine::id) { machine ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(machine.displayName, fontWeight = FontWeight.Bold)
                            val linkedNames = machine.exerciseIds.mapNotNull { exerciseById[it]?.name }.sorted()
                            Text(
                                when {
                                    linkedNames.isEmpty() -> "No exercises linked"
                                    linkedNames.size <= 3 -> linkedNames.joinToString(" · ")
                                    else -> linkedNames.take(3).joinToString(" · ") + " · +${linkedNames.size - 3} more"
                                },
                            )
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
                                WhipMenuItem(
                                    label = if (machine.archived) "Restore" else "Archive",
                                    onClick = { actionMenuId = null; onArchive(machine.id, !machine.archived) },
                                )
                                if (!machine.archived) WhipMenuItem(
                                    label = "New Configuration Version",
                                    onClick = { actionMenuId = null; onNewVersion(machine) },
                                )
                                HorizontalDivider()
                                WhipMenuItem(
                                    label = "Delete Permanently",
                                    icon = Icons.Outlined.DeleteOutline,
                                    role = WhipMenuItemRole.Destructive,
                                    onClick = { actionMenuId = null; onDelete(machine) },
                                )
                            }
                        }
                    }
                    val scale = when (machine.loadType) {
                        MachineLoadType.Mass -> "Mass stack · ${unitSymbol(machine.unitId)}"
                        MachineLoadType.Level -> "Numbered scale · ${machine.levelLabel} · ${machine.levelDirection.label.lowercase()}"
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
internal fun ExercisePermanentDeleteDialog(
    modifier: Modifier = Modifier,
    exerciseName: String,
    impact: ExerciseDeletionImpact?,
    targetMissing: Boolean,
    preparing: Boolean,
    deleting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onReviewUpdatedImpact: () -> Unit,
    onOpenActiveWorkout: () -> Unit,
    onConfirm: (ExerciseDeletionImpact) -> Unit,
    outcomeVerificationPending: Boolean = false,
) {
    val blocked = impact?.activePlacements?.let { it > 0 } == true
    val outcomeUnverified = outcomeVerificationPending ||
        errorMessage?.contains("could not be verified", ignoreCase = true) == true
    PaneAwareAlertDialog(
        modifier = modifier.testTag("exercise-delete-dialog"),
        onDismissRequest = { if (!deleting) onDismiss() },
        title = { Text("Delete “${exerciseName.ifBlank { "Exercise" }}” Permanently?") },
        text = {
            LazyColumn(
                modifier = Modifier.testTag("exercise-delete-impact-list"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (preparing && !targetMissing) item {
                    WhipNoticeCard(
                        title = "Reviewing impact",
                        message = "Checking the exact workout, routine, record, and automation impact…",
                        tone = WhipNoticeTone.Informative,
                        showProgress = true,
                        semanticStateLabel = "Reviewing exercise deletion impact",
                    )
                }
                errorMessage?.let { message -> item {
                    WhipNoticeCard(
                        title = when {
                            outcomeUnverified -> "Outcome not verified"
                            targetMissing -> "Exercise unavailable"
                            else -> "Deletion not completed"
                        },
                        message = message,
                        tone = if (targetMissing && !outcomeUnverified) WhipNoticeTone.Neutral else WhipNoticeTone.Error,
                        actionLabel = when {
                            outcomeUnverified -> "Retry Verification"
                            targetMissing -> "Close"
                            else -> "Review Updated Impact"
                        },
                        onAction = if (targetMissing && !outcomeUnverified) onDismiss else onReviewUpdatedImpact,
                        semanticStateLabel = when {
                            outcomeUnverified -> "Exercise deletion outcome not verified"
                            targetMissing -> "Exercise no longer available"
                            else -> null
                        },
                        modifier = Modifier.testTag("exercise-delete-error"),
                    )
                } }
                impact?.let { exact ->
                    if (errorMessage == null) item {
                        Text(
                            "Deletion impact ready — review it before confirming.",
                            modifier = Modifier.semantics {
                                liveRegion = LiveRegionMode.Polite
                                stateDescription = "Exercise deletion impact ready"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (blocked) item {
                        WhipNoticeCard(
                            title = "Active Workout",
                            message = "This exercise is used by ${exact.activePlacements} active workout " +
                                "placement${if (exact.activePlacements == 1) "" else "s"}. Finish the workout, " +
                                "remove or substitute it there, or archive the exercise instead.",
                            tone = WhipNoticeTone.Warning,
                            actionLabel = "Open Active Workout",
                            onAction = onOpenActiveWorkout,
                            semanticStateLabel = "Deletion blocked by active workout",
                        )
                    }
                    item {
                        Text("Removed", fontWeight = FontWeight.Bold)
                        Text(
                            "${exact.workoutPlacementCount} workout placement${if (exact.workoutPlacementCount == 1) "" else "s"} " +
                                "and ${exact.workoutSetCount} recorded set${if (exact.workoutSetCount == 1) "" else "s"}; " +
                                "${exact.routinePlacementCount} routine placement${if (exact.routinePlacementCount == 1) "" else "s"} " +
                                "and ${exact.routineSetCount} prescribed set${if (exact.routineSetCount == 1) "" else "s"}.",
                        )
                    }
                    item {
                        Text("Definitions and records removed", fontWeight = FontWeight.Bold)
                        Text(
                            "${exact.personalRecordCount} personal record${if (exact.personalRecordCount == 1) "" else "s"}, " +
                                "${exact.graphPresetUpdateCount + exact.graphPresetDeleteCount} graph preset${if (exact.graphPresetUpdateCount + exact.graphPresetDeleteCount == 1) "" else "s"}, " +
                                "${exact.linkRuleCount} Goal link rule${if (exact.linkRuleCount == 1) "" else "s"}, and " +
                                "${exact.automationRuleCount} trigger rule${if (exact.automationRuleCount == 1) "" else "s"}.",
                        )
                    }
                    item {
                        Text("References changed", fontWeight = FontWeight.Bold)
                        Text(
                            "${exact.routineAlternativeReferenceCount} routine alternative reference${if (exact.routineAlternativeReferenceCount == 1) "" else "s"}, " +
                                "${exact.machineReferenceCount} machine assignment or compatibility link${if (exact.machineReferenceCount == 1) "" else "s"}, " +
                                "and ${exact.categoryReferenceCount} category membership${if (exact.categoryReferenceCount == 1) "" else "s"} are removed. " +
                                "Machine profiles themselves remain.",
                        )
                    }
                    if (exact.linkedTrackEntryCount > 0) item {
                        Text("Track history kept", fontWeight = FontWeight.Bold)
                        Text(
                            "${exact.linkedTrackEntryCount} automation-created Track entr${if (exact.linkedTrackEntryCount == 1) "y remains" else "ies remain"} " +
                                "as recorded history, but no longer point to the deleted trigger occurrence.",
                        )
                    }
                    if (exact.trainingMaxDecisionCount > 0) item {
                        Text("Preserved", fontWeight = FontWeight.Bold)
                        Text(
                            "${exact.trainingMaxDecisionCount} historical Training Max decision${if (exact.trainingMaxDecisionCount == 1) "" else "s"} " +
                                "remain as immutable audit history with the recorded exercise name and UUID.",
                        )
                    }
                    item {
                        Text(
                            "Tracked-record shortcuts for this exercise are removed. " +
                                "Export a backup first if you may need this history.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            WhipTextButton(
                enabled = impact != null && !blocked && !deleting && errorMessage == null,
                onClick = { impact?.let(onConfirm) },
                modifier = Modifier.testTag("exercise-delete-confirm"),
            ) {
                Text(if (deleting) "Deleting…" else "Delete permanently", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            WhipTextButton(enabled = !deleting, onClick = onDismiss) { Text("Cancel") }
        },
        inputBlocked = deleting,
        inputBlockedLabel = "Permanently Deleting Exercise",
        paneTitle = "Exercise deletion review",
    )
}

@Composable
internal fun RoutinePermanentDeleteDialog(
    modifier: Modifier = Modifier,
    routineName: String,
    impact: RoutineDeletionImpact?,
    targetMissing: Boolean,
    preparing: Boolean,
    deleting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onReviewUpdatedImpact: () -> Unit,
    onOpenActiveWorkout: () -> Unit,
    onConfirm: (RoutineDeletionImpact) -> Unit,
    outcomeVerificationPending: Boolean = false,
) {
    val blocked = impact?.activeSession == true
    val outcomeUnverified = outcomeVerificationPending ||
        errorMessage?.contains("could not be verified", ignoreCase = true) == true
    PaneAwareAlertDialog(
        modifier = modifier.testTag("routine-delete-dialog"),
        onDismissRequest = { if (!deleting) onDismiss() },
        title = { Text("Delete “${routineName.ifBlank { "Routine" }}” Permanently?") },
        text = {
            LazyColumn(
                modifier = Modifier.testTag("routine-delete-impact-list"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (preparing && !targetMissing) item {
                    WhipNoticeCard(
                        title = "Reviewing impact",
                        message = "Checking the exact program and workout-history impact…",
                        tone = WhipNoticeTone.Informative,
                        showProgress = true,
                        semanticStateLabel = "Reviewing routine deletion impact",
                    )
                }
                errorMessage?.let { message -> item {
                    WhipNoticeCard(
                        title = when {
                            outcomeUnverified -> "Outcome not verified"
                            targetMissing -> "Routine unavailable"
                            else -> "Deletion not completed"
                        },
                        message = message,
                        tone = if (targetMissing && !outcomeUnverified) WhipNoticeTone.Neutral else WhipNoticeTone.Error,
                        actionLabel = when {
                            outcomeUnverified -> "Retry Verification"
                            targetMissing -> "Close"
                            else -> "Review Updated Impact"
                        },
                        onAction = if (targetMissing && !outcomeUnverified) onDismiss else onReviewUpdatedImpact,
                        semanticStateLabel = when {
                            outcomeUnverified -> "Routine deletion outcome not verified"
                            targetMissing -> "Routine no longer available"
                            else -> null
                        },
                        modifier = Modifier.testTag("routine-delete-error"),
                    )
                } }
                impact?.let { exact ->
                    if (errorMessage == null) item {
                        Text(
                            "Deletion impact ready — review it before confirming.",
                            modifier = Modifier.semantics {
                                liveRegion = LiveRegionMode.Polite
                                stateDescription = "Routine deletion impact ready"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (blocked) item {
                        WhipNoticeCard(
                            title = "Active Workout",
                            message = "A workout created from this routine is active. Finish or discard it before deleting the routine so its progression and Training Max decisions stay linked.",
                            tone = WhipNoticeTone.Warning,
                            actionLabel = "Open Active Workout",
                            onAction = onOpenActiveWorkout,
                            semanticStateLabel = "Deletion blocked by active workout",
                        )
                    }
                    item {
                        Text("Removed", fontWeight = FontWeight.Bold)
                        Text(
                            "${exact.dayCount} template day${if (exact.dayCount == 1) "" else "s"}, " +
                                "${exact.routinePlacementCount} planned exercise placement${if (exact.routinePlacementCount == 1) "" else "s"}, " +
                                "and ${exact.routineSetCount} prescribed set${if (exact.routineSetCount == 1) "" else "s"}.",
                        )
                    }
                    item {
                        Text("Kept", fontWeight = FontWeight.Bold)
                        Text(
                            "${exact.preservedWorkoutHistoryCount} completed or discarded workout${if (exact.preservedWorkoutHistoryCount == 1) "" else "s"} " +
                                "remain as ordinary History with their recorded prescriptions and performed sets. " +
                                "${exact.trainingMaxDecisionCount} Training Max decision${if (exact.trainingMaxDecisionCount == 1) "" else "s"} " +
                                "remain as immutable audit history.",
                        )
                    }
                    item {
                        Text(
                            "Program position and future Training Max progression for this routine are permanently removed. Export a backup first if you may need the template.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            WhipTextButton(
                enabled = impact != null && !blocked && !deleting && errorMessage == null,
                onClick = { impact?.let(onConfirm) },
                modifier = Modifier.testTag("routine-delete-confirm"),
            ) {
                Text(if (deleting) "Deleting…" else "Delete permanently", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            WhipTextButton(enabled = !deleting, onClick = onDismiss) { Text("Cancel") }
        },
        inputBlocked = deleting,
        inputBlockedLabel = "Permanently Deleting Routine",
        paneTitle = "Routine deletion review",
    )
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
    createdExerciseIdRequest: Long? = null,
    onCreatedExerciseRequestConsumed: () -> Unit = {},
    onCreateExercise: () -> Unit = {},
    onCreateVersion: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (GymMachineDraft) -> Unit,
    saving: Boolean = false,
) {
    val editorKey = "machine-${machine?.id ?: initialExerciseId ?: "new"}-${if (creatingVersion) "version" else "edit"}"
    var exerciseIds by rememberSaveable(editorKey) {
        mutableStateOf((machine?.exerciseIds.orEmpty() + listOfNotNull(initialExerciseId)).distinct())
    }
    var exercisePickerOpen by rememberSaveable(editorKey) { mutableStateOf(false) }
    var name by rememberSaveable(editorKey) { mutableStateOf(machine?.name.orEmpty()) }
    var location by rememberSaveable(editorKey) { mutableStateOf(machine?.location.orEmpty()) }
    var details by rememberSaveable(editorKey) { mutableStateOf(machine?.details.orEmpty()) }
    var loadType by rememberSaveable(editorKey) { mutableStateOf(machine?.loadType ?: MachineLoadType.Mass) }
    var unitId by rememberSaveable(editorKey) {
        mutableStateOf(
            machine?.unitId?.takeIf(String::isNotBlank)
                ?: exercises.firstOrNull { it.id == exerciseIds.firstOrNull() }?.weightUnitId?.ifBlank { "kilogram" }
                ?: "kilogram",
        )
    }
    var mappingUnitId by rememberSaveable(editorKey) {
        mutableStateOf(
            machine?.unitId?.takeIf(String::isNotBlank)
                ?: exercises.firstOrNull { it.id == exerciseIds.firstOrNull() }?.weightUnitId?.ifBlank { "kilogram" }
                ?: "kilogram",
        )
    }
    var pendingMachineUnit by rememberSaveable(editorKey) { mutableStateOf<String?>(null) }
    var levelLabel by rememberSaveable(editorKey) { mutableStateOf(machine?.levelLabel ?: "level") }
    var levelDirection by rememberSaveable(editorKey) {
        mutableStateOf(machine?.levelDirection ?: MachineLevelDirection.HigherNumberMoreResistance)
    }
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
    LaunchedEffect(createdExerciseIdRequest, exercises) {
        val createdId = createdExerciseIdRequest ?: return@LaunchedEffect
        if (exercises.any { it.id == createdId }) {
            exerciseIds = (exerciseIds + createdId).distinct()
            if (machine == null && loadType == MachineLoadType.Mass) {
                exercises.first { it.id == createdId }.weightUnitId.takeIf(String::isNotBlank)?.let { selectedUnit ->
                    unitId = selectedUnit
                    applyStandardSequence(unit = selectedUnit)
                }
            }
            onCreatedExerciseRequestConsumed()
        }
    }
    val editorFingerprint = listOf(
        exerciseIds.sorted(), name, location, details, loadType, unitId, mappingUnitId, levelLabel, levelDirection,
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
        primary = true,
        paneTitle = if (machine == null) "Create Machine Profile" else "Edit Machine Profile",
        onDismissRequest = { if (!saving) requestDismiss() },
        title = { Text(if (creatingVersion) "New Machine Configuration Version" else if (machine == null) "Create Machine Profile" else "Edit Machine Profile") },
        text = {
            LazyColumn(
                modifier = Modifier.testTag("machine-editor-list"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { OutlinedTextField(name, { name = it.replace('\n', ' ').replace('\r', ' ').take(100) }, label = { Text("Machine name *") }, supportingText = { Text("${name.length}/100 · Example: Home multi-gym") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("machine-editor-name")) }
                item { OutlinedTextField(location, { location = it }, label = { Text("Location") }, supportingText = { Text("Example: Home or Downtown Gym") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(details, { details = it }, label = { Text("Model / setup notes") }, supportingText = { Text("Seat, attachment, pulley, or other setup that changes resistance") }, modifier = Modifier.fillMaxWidth()) }
                if (definitionLocked) item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AvailabilityNotice(
                            label = "Resistance definition",
                            availability = ControlAvailability(
                                enabled = false,
                                unavailableExplanation = "Locked to preserve workout history. Names, notes, linked exercises, setup cues, and presets remain editable.",
                            ),
                        )
                        onCreateVersion?.let { createVersion ->
                            WhipOutlinedButton(onClick = createVersion, modifier = Modifier.fillMaxWidth()) {
                                Text("Create New Configuration Version")
                            }
                        }
                    }
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
                        Text("What a higher number means", style = MaterialTheme.typography.labelMedium)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            MachineLevelDirection.entries.forEach { direction ->
                                WhipFilterChip(
                                    selected = levelDirection == direction,
                                    enabled = !definitionLocked,
                                    onClick = { levelDirection = direction },
                                    label = { Text(direction.label) },
                                )
                            }
                        }
                        Text(
                            "Whip uses this for stronger-setting comparisons and generated warm-up ramps.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
                item {
                    Text("Exercises (Optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Link every movement that uses this machine, or save the profile now and add exercises later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val linkedNames = exerciseIds.mapNotNull { id -> exercises.firstOrNull { it.id == id }?.name }
                    Text(
                        linkedNames.takeIf(List<String>::isNotEmpty)?.joinToString(" · ") ?: "No exercises linked",
                        modifier = Modifier.testTag("machine-linked-exercises-summary"),
                    )
                    WhipOutlinedButton(
                        onClick = { exercisePickerOpen = true },
                        modifier = Modifier.fillMaxWidth().testTag("machine-choose-exercises"),
                    ) {
                        Text(if (exerciseIds.isEmpty()) "Choose or Create Exercises" else "Manage Linked Exercises (${exerciseIds.size})")
                    }
                }
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
                    NumberField(addOnPlate, { addOnPlate = it }, "Add-on resistance (${unitSymbol(if (loadType == MachineLoadType.Mass) unitId else mappingUnitId)})")
                    ToggleRow("Allow comparison with selected compatible versions", compatibleForComparison) { compatibleForComparison = it }
                }
            }
        },
        confirmButton = {
            WhipTextButton(
                enabled = !saving && name.isNotBlank() && parsedLoads.error == null &&
                    (loadType == MachineLoadType.Mass || levelLabel.isNotBlank()) &&
                    (pulleyRatio.toWhipDoubleOrNull()?.let { it > 0.0 && it <= 10.0 } == true) &&
                    parseMachineMassMapping(massMapping) != null,
                onClick = {
                    onSave(
                        GymMachineDraft(
                            exerciseId = exerciseIds.firstOrNull(), name = name, location = location,
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
                            exerciseIds = exerciseIds.toSet(),
                            levelDirection = levelDirection,
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
    if (exercisePickerOpen) {
        var query by rememberSaveable(editorKey) { mutableStateOf("") }
        val visibleExercises = exercises.filter { it.name.contains(query, ignoreCase = true) }
        PaneAwareAlertDialog(
            modifier = modifier.testTag("machine-exercise-picker"),
            onDismissRequest = { exercisePickerOpen = false },
            title = { Text("Linked Exercises") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search exercises") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("machine-exercise-search"),
                    )
                    WhipOutlinedButton(
                        onClick = {
                            exercisePickerOpen = false
                            onCreateExercise()
                        },
                        modifier = Modifier.fillMaxWidth().testTag("machine-create-exercise"),
                    ) { Text("Create New Exercise") }
                    LazyColumn(
                        modifier = Modifier.heightIn(min = 96.dp, max = 320.dp).testTag("machine-exercise-picker-list"),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(visibleExercises, key = Exercise::id) { exercise ->
                            val selected = exercise.id in exerciseIds
                            WhipMultiChoiceRow(
                                label = exercise.name,
                                supportingText = exercise.equipment.takeIf(String::isNotBlank),
                                checked = selected,
                                onCheckedChange = {
                                    exerciseIds = if (selected) exerciseIds - exercise.id else (exerciseIds + exercise.id).distinct()
                                },
                            )
                        }
                        if (visibleExercises.isEmpty()) item {
                            Text("No matching exercises. Create one without leaving this machine profile.")
                        }
                    }
                }
            },
            confirmButton = {
                WhipTextButton(onClick = { exercisePickerOpen = false }) { Text("Done") }
            },
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
    modifier: Modifier = Modifier,
    createRequested: Boolean = false,
    onCreateRequestConsumed: () -> Unit = {},
    onReorderModeChange: (Boolean) -> Unit = {},
    reorderDismissRequest: Int = 0,
) {
    val dialogModifier = modifier
    var editingCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    val editing = editingCategoryId?.let { id -> (state.categories + state.archivedCategories).firstOrNull { it.id == id } }
    var creating by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(createRequested) {
        if (createRequested) {
            creating = true
            onCreateRequestConsumed()
        }
    }
    val editorKey = "category-${editingCategoryId ?: "new"}"
    var name by rememberSaveable(editorKey) { mutableStateOf(editing?.name.orEmpty()) }
    var kind by rememberSaveable(editorKey) { mutableStateOf(editing?.kind ?: "Category") }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var reordering by rememberSaveable { mutableStateOf(false) }
    val visible = if (showArchived) state.archivedCategories else state.categories
    BackHandler(enabled = reordering) { reordering = false }
    DisposableEffect(reordering) {
        onReorderModeChange(reordering)
        onDispose { if (reordering) onReorderModeChange(false) }
    }
    LaunchedEffect(reorderDismissRequest) {
        if (reorderDismissRequest > 0) reordering = false
    }
    LaunchedEffect(showArchived) { if (showArchived) reordering = false }
    WhipReorderLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = WhipPageContentPadding,
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.compact),
    ) {
        item {
            WhipPageHeader(
                title = "Exercise Categories",
                supportingText = "Library labels for browsing and analytics. Assign them in Edit Exercise; they never assign 5/3/1 Push/Pull assistance roles.",
            ) {
                if (!reordering && state.categories.size > 1) {
                    WhipPageIconAction(
                        icon = Icons.Outlined.DragHandle,
                        label = if (showArchived) "Show active and reorder all Categories" else "Reorder Categories",
                        onClick = {
                            showArchived = false
                            reordering = true
                        },
                    )
                }
            }
        }
        if (!reordering) item { ToggleRow("Show archived", showArchived) { showArchived = it } }
        if (reordering) item {
            WhipReorderModeBar(
                itemLabel = "Categories",
                onDone = { reordering = false },
                boundaryNote = "Archived Categories stay outside this order.",
            )
        }
        if (visible.isEmpty()) item {
            WhipEmptyState(
                title = if (showArchived) "No Archived Categories" else "No Categories Yet",
                supportingText = if (showArchived) {
                    "Archived categories will appear here."
                } else {
                    "Categories are optional. Their order also decides the winner when Settings uses First linked category only."
                },
                primaryActionLabel = "Create Category".takeUnless { showArchived },
                onPrimaryAction = { creating = true }.takeUnless { showArchived },
            )
        }
        items(visible.size, key = { visible[it].id }) { index ->
            val category = visible[index]
            val reorderInteraction = rememberWhipReorderInteractionState()
            Card(
                Modifier.fillMaxWidth().whipReorderItem(
                    reorderInteraction,
                    layoutPosition = index + 1,
                    layoutScope = "category-browse",
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (reordering && !showArchived) {
                        WhipReorderHandle(
                            label = category.name,
                            canMovePrevious = index > 0,
                            canMoveNext = index < visible.lastIndex,
                            position = index + 1,
                            total = visible.size,
                            interactionState = reorderInteraction,
                            moveWholeItem = true,
                            layoutScope = "category-browse",
                            reserveWhenUnavailable = true,
                            onMove = { delta ->
                                viewModel.reorderCategories(moveListItem(visible, index, delta).map(ExerciseCategory::id))
                            },
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(category.kind, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!reordering) ItemEditButton("category", category.name, onEdit = { editingCategoryId = category.id })
                    if (!reordering) {
                        IconButton(
                            onClick = { viewModel.setCategoryArchived(category.id, !category.archived) },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = if (category.archived) Icons.Outlined.Restore else Icons.Outlined.Archive,
                                contentDescription = if (category.archived) {
                                    "Restore category ${category.name}"
                                } else {
                                    "Archive category ${category.name}"
                                },
                            )
                        }
                    }
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
                    OutlinedTextField(name, { name = it.replace('\n', ' ').replace('\r', ' ').take(80) }, label = { Text("Name") }, singleLine = true)
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
    modifier: Modifier = Modifier,
    focusedWorkoutId: Long? = null,
) {
    val dialogModifier = modifier
    var query by rememberSaveable { mutableStateOf("") }
    var calendarView by rememberSaveable { mutableStateOf(false) }
    var selectedExerciseId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedRoutineId by rememberSaveable { mutableStateOf<Long?>(null) }
    var historyRange by rememberSaveable { mutableStateOf(WorkoutHistoryRange.All) }
    var recordsOnly by rememberSaveable { mutableStateOf(false) }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var historyOptionsExpanded by rememberSaveable { mutableStateOf(false) }
    var trainingMaxHistoryExpanded by rememberSaveable { mutableStateOf(false) }
    var actionMenuId by rememberSaveable { mutableStateOf<Long?>(null) }
    var expandedSessionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var resumeCandidateId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(focusedWorkoutId) {
        if (focusedWorkoutId != null) expandedSessionId = focusedWorkoutId
    }
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
    // An exact route (search, record source, deep link) owns its own visibility.
    // Do not wait for the browse-mode "show archived" toggle to catch up before
    // rendering a discarded workout the user explicitly selected.
    val focusedSession = focusedWorkoutId?.let { id -> state.allSessions.firstOrNull { it.id == id } }
    val effectiveShowArchived = focusedSession?.archived ?: showArchived
    val sourceHistory = focusedSession?.let(::listOf)
        ?: if (showArchived) state.archivedWorkouts else history
    val exerciseById = (state.exercises + state.archivedExercises).associateBy(Exercise::id)
    val workoutExercisesBySession = remember(state.allWorkoutExercises) {
        state.allWorkoutExercises.groupBy(WorkoutExercise::sessionId)
    }
    val setsByWorkoutExercise = remember(state.allSets) {
        state.allSets.groupBy(WorkoutSet::workoutExerciseId)
    }
    val categoryIdsByExercise = remember(state.categoryLinks) {
        state.categoryLinks.groupBy({ it.exerciseId }, { it.categoryId })
    }
    val through = LocalWhipToday.current
    // A day rollover advances live ranges without ejecting someone browsing an older month.
    var calendarMonth by rememberSaveable { mutableStateOf(YearMonth.from(through)) }
    var selectedCalendarDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    val from = when (historyRange) {
        WorkoutHistoryRange.Month -> through.minusMonths(1)
        WorkoutHistoryRange.ThreeMonths -> through.minusMonths(3)
        WorkoutHistoryRange.Year -> through.minusYears(1)
        WorkoutHistoryRange.All -> null
    }
    val recordSessionIds = state.personalRecords.mapNotNullTo(mutableSetOf()) { it.sourceSessionId }
    val filteredHistory = sourceHistory.filter { session ->
        val sessionExercises = workoutExercisesBySession[session.id].orEmpty()
        if (selectedExerciseId != null && sessionExercises.none { it.exerciseId == selectedExerciseId }) return@filter false
        if (selectedCategoryId != null && sessionExercises.none { workoutExercise ->
                selectedCategoryId in categoryIdsByExercise[workoutExercise.exerciseId].orEmpty()
            }
        ) return@filter false
        if (selectedRoutineId != null && session.sourceRoutineId != selectedRoutineId) return@filter false
        if (from != null && session.localDate.isBefore(from)) return@filter false
        if (recordsOnly && session.id !in recordSessionIds) return@filter false
        if (query.isBlank()) true else {
            val exerciseNames = sessionExercises.mapNotNull { exerciseById[it.exerciseId]?.name }
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
    val sessionUuids = state.allSessions.mapTo(mutableSetOf(), WorkoutSession::uuid)
    val selectedExerciseUuid = selectedExerciseId?.let { exerciseById[it]?.uuid }
    val routineUuidById = (state.routines + state.archivedRoutines).associate { it.id to it.uuid }
    val selectedRoutineUuid = selectedRoutineId?.let(routineUuidById::get)
    val standaloneTrainingMaxDecisions = state.trainingMaxDecisions.filter { decision ->
        decision.sessionUuid !in sessionUuids &&
            (selectedExerciseUuid == null || decision.exerciseUuid == selectedExerciseUuid) &&
            (selectedRoutineUuid == null || decision.routineUuid == selectedRoutineUuid) &&
            (query.isBlank() || decision.exerciseName.contains(query, ignoreCase = true)) &&
            (from == null || !java.time.Instant.ofEpochMilli(decision.createdAtMillis)
                .atZone(state.appSettings.zoneId()).toLocalDate().isBefore(from))
    }.sortedByDescending { it.createdAtMillis }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = WhipPageContentPadding,
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.compact),
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
                    if (effectiveShowArchived) append(" · Archived")
                    if (recordsOnly) append(" · Records only")
                    state.exercises.firstOrNull { it.id == selectedExerciseId }?.let { append(" · ${it.name}") }
                    state.categories.firstOrNull { it.id == selectedCategoryId }?.let { append(" · ${it.name}") }
                    state.routines.firstOrNull { it.id == selectedRoutineId }?.let { append(" · ${it.name}") }
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
                    ExerciseSelectionField(
                        label = "Exercise Filter",
                        exercises = state.exercises,
                        selectedExerciseId = selectedExerciseId,
                        onSelect = { selectedExerciseId = it },
                        modifier = Modifier.fillMaxWidth().testTag("history-exercise-filter"),
                        allLabel = "All Exercises",
                    )
                }
                if (state.categories.isNotEmpty()) {
                    GymEnumDropdown("Category filter", listOf<Long?>(null) + state.categories.map(ExerciseCategory::id), selectedCategoryId, { id -> state.categories.firstOrNull { it.id == id }?.name ?: "All Categories" }, titleCaseValues = false) { selectedCategoryId = it }
                }
                if (state.routines.isNotEmpty()) {
                    GymEnumDropdown("Routine filter", listOf<Long?>(null) + state.routines.map(GymRoutine::id), selectedRoutineId, { id -> state.routines.firstOrNull { it.id == id }?.name ?: "All Routines" }, titleCaseValues = false) { selectedRoutineId = it }
                }
                ToggleRow("Personal-record workouts only", recordsOnly) { recordsOnly = it }
                if (query.isNotBlank() || selectedExerciseId != null || selectedCategoryId != null || selectedRoutineId != null ||
                    historyRange != WorkoutHistoryRange.All || recordsOnly
                ) {
                    WhipTextButton(
                        onClick = {
                            query = ""
                            selectedExerciseId = null
                            selectedCategoryId = null
                            selectedRoutineId = null
                            historyRange = WorkoutHistoryRange.All
                            recordsOnly = false
                            selectedCalendarDate = null
                        },
                    ) { Text("Clear History Filters") }
                }
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
        if (!calendarView && focusedWorkoutId == null && standaloneTrainingMaxDecisions.isNotEmpty()) {
            item {
                OutlinedCard(Modifier.fillMaxWidth().testTag("history-standalone-training-max-decisions")) {
                    Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DisclosureRow(
                            title = "Program Training Max changes",
                            supportingText = "${standaloneTrainingMaxDecisions.size} manual program edit${if (standaloneTrainingMaxDecisions.size == 1) "" else "s"}",
                            expanded = trainingMaxHistoryExpanded,
                            onClick = { trainingMaxHistoryExpanded = !trainingMaxHistoryExpanded },
                        )
                        if (trainingMaxHistoryExpanded) {
                            standaloneTrainingMaxDecisions.take(20).forEach { decision ->
                                val date = java.time.Instant.ofEpochMilli(decision.createdAtMillis)
                                    .atZone(state.appSettings.zoneId()).toLocalDate()
                                Text(
                                    "$date · ${decision.exerciseName} · ${editableNumericValue(decision.previousTrainingMax)} → " +
                                        "${editableNumericValue(decision.resultingTrainingMax)} ${unitSymbol(decision.unitId)}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    decision.reasons.joinToString(" "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (visible.isEmpty() && standaloneTrainingMaxDecisions.isEmpty()) item {
            WhipEmptyState(
                title = when {
                    filteredHistory.isEmpty() && sourceHistory.isNotEmpty() -> "No Matching Workouts"
                    effectiveShowArchived -> "No Archived Workouts"
                    else -> "No Workout History"
                },
                supportingText = when {
                    sourceHistory.isNotEmpty() -> "Change the history options or clear the filter to see more workouts."
                    effectiveShowArchived -> "Workouts you archive will appear here."
                    else -> "Finished workouts will appear here."
                },
            )
        }
        items(visible, key = WorkoutSession::id) { session ->
            val sessionExercises = workoutExercisesBySession[session.id].orEmpty()
            val sessionSets = sessionExercises.flatMap { setsByWorkoutExercise[it.id].orEmpty() }
            val expanded = expandedSessionId == session.id
            WorkoutHistoryCard(
                session = session,
                workoutExercises = sessionExercises,
                sets = sessionSets,
                exerciseById = exerciseById,
                trainingMaxDecisions = state.trainingMaxDecisions.filter { it.sessionUuid == session.uuid },
                preferredWeightUnitId = state.appSettings.gymWeightUnitId,
                preferredDistanceUnitId = state.appSettings.distanceUnitId,
                numberPrecision = state.appSettings.numberPrecision,
                expanded = expanded,
                archivedView = session.archived,
                hasActiveWorkout = state.activeSession != null,
                menuExpanded = actionMenuId == session.id,
                onToggleExpanded = { expandedSessionId = session.id.takeUnless { expanded } },
                onMenuExpandedChange = { menuOpen -> actionMenuId = session.id.takeIf { menuOpen } },
                onRepeatWorkout = { onCopy(session.id) },
                onOpenActiveWorkout = onOpenActiveWorkout,
                onEditDetails = { onEditDetails(session) },
                onResume = { resumeCandidateId = session.id },
                onSaveAsRoutine = { onSaveAsRoutine(session.id, session.name) },
                onShare = { onShare(session) },
                onRestore = { onRestore(session.id) },
                onDelete = { onDelete(session) },
                onReuseExercise = onCopyExercise,
            )
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
internal fun WorkoutHistoryCard(
    session: WorkoutSession,
    workoutExercises: List<WorkoutExercise>,
    sets: List<WorkoutSet>,
    exerciseById: Map<Long, Exercise>,
    trainingMaxDecisions: List<TrainingMaxDecision> = emptyList(),
    preferredWeightUnitId: String = "kilogram",
    preferredDistanceUnitId: String = "kilometre",
    numberPrecision: Int = 1,
    expanded: Boolean,
    archivedView: Boolean,
    hasActiveWorkout: Boolean,
    menuExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onMenuExpandedChange: (Boolean) -> Unit,
    onRepeatWorkout: () -> Unit,
    onOpenActiveWorkout: () -> Unit,
    onEditDetails: () -> Unit,
    onResume: () -> Unit,
    onSaveAsRoutine: () -> Unit,
    onShare: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onReuseExercise: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = session.name.ifBlank { "Workout" }
    val workoutExerciseIds = workoutExercises.mapTo(mutableSetOf(), WorkoutExercise::id)
    val activeSets = sets.filter { set ->
        set.deletedAtMillis == null && set.workoutExerciseId in workoutExerciseIds
    }
    val completedSets = activeSets.count(WorkoutSet::completed)
    val durationMinutes = session.endedAt?.let { endedAt ->
        Duration.between(session.startedAt, endedAt).toMinutes().coerceAtLeast(0)
    }
    val summary = buildList {
        add(session.localDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
        add(quantityLabel(workoutExercises.size, "exercise"))
        if (activeSets.isNotEmpty()) add(quantityLabel(completedSets.takeIf { it > 0 } ?: activeSets.size, "set"))
        durationMinutes?.takeIf { it > 0 }?.let { add(formatWorkoutHistoryDuration(it)) }
    }.joinToString(" · ")
    val exerciseNames = workoutExercises.mapNotNull { exerciseById[it.exerciseId]?.name }

    Card(modifier = modifier.fillMaxWidth().testTag("history-workout-card-${session.id}")) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    IconButton(
                        onClick = { onMenuExpandedChange(true) },
                        modifier = Modifier.size(48.dp).testTag("history-workout-menu-${session.id}"),
                    ) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "More options for workout $title")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { onMenuExpandedChange(false) }) {
                        if (archivedView) {
                            WhipMenuItem(
                                label = "Restore to History",
                                onClick = { onMenuExpandedChange(false); onRestore() },
                            )
                        } else {
                            WhipMenuItem(
                                label = "Edit Details",
                                onClick = { onMenuExpandedChange(false); onEditDetails() },
                            )
                            WhipMenuItem(
                                label = "Save as Routine",
                                onClick = { onMenuExpandedChange(false); onSaveAsRoutine() },
                            )
                            WhipMenuItem(
                                label = "Share",
                                onClick = { onMenuExpandedChange(false); onShare() },
                            )
                            if (!hasActiveWorkout) {
                                WhipMenuItem(
                                    label = "Resume Original Workout",
                                    onClick = { onMenuExpandedChange(false); onResume() },
                                )
                            }
                        }
                        HorizontalDivider()
                        WhipMenuItem(
                            label = "Delete Permanently",
                            role = WhipMenuItemRole.Destructive,
                            onClick = { onMenuExpandedChange(false); onDelete() },
                        )
                    }
                }
                IconButton(
                    onClick = onToggleExpanded,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("history-workout-toggle-${session.id}")
                        .semantics {
                            contentDescription = "${if (expanded) "Hide" else "Show"} details for workout $title"
                            stateDescription = if (expanded) "Expanded" else "Collapsed"
                        },
                ) {
                    Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
                }
            }

            if (!expanded && exerciseNames.isNotEmpty()) {
                Text(
                    exerciseNames.joinToString(" · "),
                    modifier = Modifier.testTag("history-workout-preview-${session.id}"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (session.notes.isNotBlank()) {
                    Text(session.notes, style = MaterialTheme.typography.bodyMedium)
                }
                workoutProgramSnapshotLabel(session)?.let { snapshot ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history-program-snapshot-${session.id}")
                            .semantics(mergeDescendants = true) {},
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            snapshot,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                if (trainingMaxDecisions.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().testTag("history-training-max-decisions-${session.id}"),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("Training Max decisions", fontWeight = FontWeight.SemiBold)
                            trainingMaxDecisions.forEach { decision ->
                                Text(
                                    "${decision.exerciseName} · ${editableNumericValue(decision.previousTrainingMax)} → " +
                                        "${editableNumericValue(decision.resultingTrainingMax)} ${unitSymbol(decision.unitId)} " +
                                        "(${decision.action.name.replace(Regex("([a-z])([A-Z])"), "$1 $2")})",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    "${decision.recommendationCategory.replace(Regex("([a-z])([A-Z])"), "$1 $2")} · " +
                                        "Evidence strength: ${fiveThreeOneEvidenceStrength(decision.confidence)} · " +
                                        decision.reasons.joinToString("; "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth().testTag("history-workout-exercises-${session.id}"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    workoutExercises.forEach { workoutExercise ->
                        val sourceExercise = exerciseById[workoutExercise.exerciseId] ?: return@forEach
                        val exerciseSets = activeSets.filter { it.workoutExerciseId == workoutExercise.id }
                        Surface(
                            modifier = Modifier.fillMaxWidth().testTag("history-exercise-row-${workoutExercise.id}"),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            sourceExercise.name,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        val detail = buildList {
                                            if (exerciseSets.isNotEmpty()) add(quantityLabel(exerciseSets.size, "set"))
                                            workoutExercise.machineNameSnapshot.takeIf(String::isNotBlank)?.let { add("Equipment: $it") }
                                        }.joinToString(" · ")
                                        if (detail.isNotBlank()) {
                                            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        workoutExercise.machineConfigurationSnapshot.takeIf(String::isNotBlank)?.let { setup ->
                                            Text(
                                                "Setup: $setup",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        if (workoutExercise.notes.isNotBlank()) {
                                            Text(
                                                workoutExercise.notes,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                    if (!archivedView) {
                                        WhipTextButton(
                                            onClick = { onReuseExercise(workoutExercise.id) },
                                            modifier = Modifier.heightIn(min = 48.dp).testTag("history-exercise-reuse-${workoutExercise.id}"),
                                        ) { Text("Use Again", maxLines = 1) }
                                    }
                                }

                                exerciseSets.sortedBy(WorkoutSet::position).forEachIndexed { setIndex, set ->
                                    HistoricalWorkoutSetRow(
                                        set = set,
                                        setNumber = setIndex + 1,
                                        sourceExercise = sourceExercise,
                                        workoutExercise = workoutExercise,
                                        preferredWeightUnitId = preferredWeightUnitId,
                                        preferredDistanceUnitId = preferredDistanceUnitId,
                                        numberPrecision = numberPrecision,
                                    )
                                }
                            }
                        }
                    }
                }

                if (!archivedView) {
                    WhipButton(
                        onClick = if (hasActiveWorkout) onOpenActiveWorkout else onRepeatWorkout,
                        modifier = Modifier.fillMaxWidth().testTag("history-primary-action-${session.id}"),
                    ) {
                        Text(if (hasActiveWorkout) "Open Active Workout" else "Repeat Workout", maxLines = 1)
                    }
                } else {
                    WhipOutlinedButton(
                        onClick = onRestore,
                        modifier = Modifier.fillMaxWidth().testTag("history-primary-action-${session.id}"),
                    ) { Text("Restore to History", maxLines = 1) }
                }

            }
        }
    }
}

internal fun workoutProgramSnapshotLabel(session: WorkoutSession): String? {
    if (session.sourceRoutineProgramKind == RoutineProgramKind.Static) {
        return session.sourceRoutineDayProgressionIndex?.let { index ->
            "Load cycle snapshot · step ${index + 1}"
        }
    }
    val program = when (session.sourceRoutineProgramKind) {
        RoutineProgramKind.Static -> return null
        RoutineProgramKind.Custom -> "Program"
        RoutineProgramKind.FiveThreeOne -> "5/3/1"
        RoutineProgramKind.FiveThreeOneClassic -> "Classic 5/3/1"
        RoutineProgramKind.FiveSPro -> "5s PRO"
        RoutineProgramKind.BoringButBig -> "Boring But Big"
        RoutineProgramKind.FirstSetLast -> "First Set Last"
    }
    return buildList {
        add("Program snapshot · $program")
        session.sourceRoutineCycle?.let { add("Cycle $it") }
        session.sourceRoutinePhaseIndex?.let { phaseIndex ->
            val label = session.sourceRoutinePhaseLabel.takeIf(String::isNotBlank) ?: "Phase ${phaseIndex + 1}"
            val role = when (session.sourceRoutinePhaseRole) {
                RoutineProgramPhaseRole.Standard -> null
                RoutineProgramPhaseRole.Leader -> "Leader"
                RoutineProgramPhaseRole.Anchor -> "Anchor"
                RoutineProgramPhaseRole.Deload -> "Deload"
                RoutineProgramPhaseRole.TrainingMaxTest -> "Training Max Test"
                RoutineProgramPhaseRole.PersonalRecordTest -> "PR Test"
            }
            add(listOfNotNull(label, role).distinct().joinToString(" · "))
        }
        session.sourceRoutineDayPosition?.let { add("Day ${it + 1}") }
        session.sourceRoutineDayProgressionIndex?.let { add("Day progression ${it + 1}") }
        add(if (session.programProgressAdvanced) "Advanced program progress" else "Did not advance program progress")
    }.joinToString(" · ")
}

@Composable
private fun HistoricalWorkoutSetRow(
    set: WorkoutSet,
    setNumber: Int,
    sourceExercise: Exercise,
    workoutExercise: WorkoutExercise,
    preferredWeightUnitId: String,
    preferredDistanceUnitId: String,
    numberPrecision: Int,
) {
    val performed = set.shortLabel(
        preferredWeightUnitId = preferredWeightUnitId,
        preferredDistanceUnitId = preferredDistanceUnitId,
        precision = numberPrecision,
        workoutExercise = workoutExercise,
        exerciseWeightUnitId = sourceExercise.weightUnitId,
    )
    val prescription = set.prescriptionLabel(preferredWeightUnitId, numberPrecision, workoutExercise)
    val details = buildList {
        set.rpe?.let { add("RPE ${formatNumber(it, 1)}") }
        set.rir?.let { add("RIR ${formatNumber(it, 1)}") }
        set.restSeconds?.let { add("${formatDuration(it.toLong())} rest") }
        set.tempo.takeIf(String::isNotBlank)?.let { add("Tempo $it") }
        if (set.unilateral) add("One side / limb")
    }.joinToString(" · ")
    val sectionLabel = when {
        set.optionalWorkKindSnapshot == RoutineOptionalWorkKind.Joker -> "Optional Joker"
        set.workSectionSnapshot == RoutineWorkSection.Main -> "Main"
        set.workSectionSnapshot == RoutineWorkSection.Supplemental -> "Supplemental"
        set.workSectionSnapshot == RoutineWorkSection.Assistance ->
            workoutExercise.assistanceLabel() ?: "Assistance"
        set.workSectionSnapshot == RoutineWorkSection.Optional -> "Optional"
        else -> null
    }
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("history-set-row-${set.id}"),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                buildList {
                    add("Set $setNumber")
                    sectionLabel?.let(::add)
                    add(set.classification.uiLabel())
                    add(if (set.completed) "Completed" else "Not completed")
                }.joinToString(" · "),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                if (set.completed) "Performed · $performed" else "Performed · No completed values",
                modifier = Modifier.testTag("history-set-performed-${set.id}"),
                style = MaterialTheme.typography.bodyMedium,
            )
            prescription?.let { target ->
                Text(
                    "Target · $target",
                    modifier = Modifier.testTag("history-set-target-${set.id}"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            if (details.isNotBlank()) {
                Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            set.note.takeIf(String::isNotBlank)?.let { note ->
                Text("Note · $note", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun WorkoutExercise.assistanceLabel(): String? {
    if (placementKindSnapshot != RoutinePlacementKind.Assistance &&
        assistanceRoleSnapshot !in setOf(
            RoutineAssistanceRole.Push,
            RoutineAssistanceRole.Pull,
            RoutineAssistanceRole.SingleLegCore,
            RoutineAssistanceRole.Other,
        )
    ) return null
    val category = if (assistanceCategorySnapshot != RoutineAssistanceCategory.Unspecified) {
        assistanceCategorySnapshot
    } else {
        when (assistanceRoleSnapshot) {
            RoutineAssistanceRole.Push -> RoutineAssistanceCategory.Push
            RoutineAssistanceRole.Pull -> RoutineAssistanceCategory.Pull
            RoutineAssistanceRole.SingleLegCore -> RoutineAssistanceCategory.SingleLegCore
            RoutineAssistanceRole.Other -> RoutineAssistanceCategory.Other
            else -> RoutineAssistanceCategory.Unspecified
        }
    }
    return when (category) {
        RoutineAssistanceCategory.Push -> "Assistance · Push"
        RoutineAssistanceCategory.Pull -> "Assistance · Pull"
        RoutineAssistanceCategory.SingleLegCore -> "Assistance · Single-leg / Core"
        RoutineAssistanceCategory.Other -> "Assistance · Other"
        RoutineAssistanceCategory.Unspecified -> "Assistance"
    }
}

private fun formatWorkoutHistoryDuration(totalMinutes: Long): String = when {
    totalMinutes < 60 -> "$totalMinutes min"
    totalMinutes % 60L == 0L -> "${totalMinutes / 60} hr"
    else -> "${totalMinutes / 60} hr ${totalMinutes % 60} min"
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
        paneTitle = "$exerciseName Notes",
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
private fun TrackedRecordsSection(
    state: GymUiState,
    onManage: () -> Unit,
    onOpenWorkoutHistory: (Long) -> Unit,
) {
    val activeExercisesByUuid = state.exercises.associateBy(Exercise::uuid)
    val configured = state.appSettings.trackedGymRecords.filter { selection ->
        activeExercisesByUuid[selection.exerciseUuid]?.let { exercise ->
            selection.type in exercise.supportedTrackedRecordTypes()
        } == true
    }
    val groups = configured.groupBy(TrackedGymRecord::exerciseUuid)
    Column(
        modifier = Modifier.fillMaxWidth().testTag("gym-tracked-records"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text("Tracked Records", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (configured.isEmpty()) "The benchmarks you choose to keep in view."
                    else "${groups.size} exercise${if (groups.size == 1) "" else "s"} · ${configured.size} record${if (configured.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            WhipTextButton(
                onClick = onManage,
                modifier = Modifier.testTag("gym-manage-tracked-records"),
            ) { Text("Manage") }
        }
        if (configured.isEmpty()) {
            WhipEmptyState(
                title = "No Tracked Records Yet",
                supportingText = "Choose the exercises and benchmarks that matter to you. Whip will update them from eligible completed sets.",
                primaryActionLabel = "Choose Records",
                onPrimaryAction = onManage,
            )
        } else {
            groups.forEach { (exerciseUuid, selections) ->
                val exercise = activeExercisesByUuid.getValue(exerciseUuid)
                val weightUnitId = exercise.weightUnitId.ifBlank { state.appSettings.gymWeightUnitId }
                Surface(
                    modifier = Modifier.fillMaxWidth().semantics {
                        contentDescription = "${exercise.name}, ${selections.size} tracked record${if (selections.size == 1) "" else "s"}"
                    },
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(exercise.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        selections.sortedBy(TrackedGymRecord::position).forEachIndexed { index, selection ->
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            val record = selection.resolveForExercise(exercise.id, state.personalRecords)
                            val sourceFormula = record?.sourceSetId?.let { sourceSetId ->
                                val workoutExerciseId = state.allSets.firstOrNull { it.id == sourceSetId }?.workoutExerciseId
                                state.allWorkoutExercises.firstOrNull { it.id == workoutExerciseId }?.oneRepMaxFormulaSnapshot
                            }
                            val sourceSessionId = record?.sourceSessionId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .then(
                                        if (sourceSessionId == null) Modifier.semantics { stateDescription = "No record yet" }
                                        else Modifier.clickable(
                                            onClickLabel = "View source workout in History",
                                            onClick = { onOpenWorkoutHistory(sourceSessionId) },
                                        ),
                                    )
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        selection.trackedRecordLabel(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    trackedRecordContext(selection, record, sourceFormula, state)?.let { context ->
                                        Text(context, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Text(
                                    record?.displayText(weightUnitId, state.appSettings.distanceUnitId, state.appSettings.numberPrecision)
                                        ?: "No record yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (record == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                )
                                if (sourceSessionId != null) Icon(Icons.AutoMirrored.Outlined.NavigateNext, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun TrackedGymRecord.trackedRecordLabel(): String = type.uiLabel()

private fun trackedRecordContext(
    selection: TrackedGymRecord,
    record: PersonalRecord?,
    sourceFormula: EstimatedOneRepMaxFormula?,
    state: GymUiState,
): String? {
    if (record == null) {
        val exercise = (state.exercises + state.archivedExercises).firstOrNull { it.uuid == selection.exerciseUuid }
        return if (exercise?.includeInPersonalRecords == false) {
            "Record updates are off in Exercise settings"
        } else "No qualifying workout yet"
    }
    val details = buildList {
        if (selection.type == PersonalRecordType.EstimatedOneRepMax) {
            add("${sourceFormula?.name ?: state.appSettings.oneRepMaxFormula} formula")
        }
        record.machineProfileUuidSnapshot?.let { scope ->
            val machineName = (state.machines + state.archivedMachines)
                .firstOrNull { it.uuid == scope }?.displayName
            add(machineName ?: "Saved equipment")
        }
        add(
            java.time.Instant.ofEpochMilli(record.achievedAtMillis)
                .atZone(state.appSettings.zoneId()).toLocalDate()
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
        )
    }
    return details.joinToString(" · ")
}

private fun recommendedTrackedRecords(exercise: Exercise, state: GymUiState): List<TrackedGymRecord> {
    val currentRecords = state.personalRecords
        .asSequence()
        .filter { it.exerciseId == exercise.id && it.current }
        .toList()
    val latestRecord = currentRecords.maxByOrNull(PersonalRecord::achievedAtMillis)
    val levelMachine = state.machines.firstOrNull { it.supportsExercise(exercise.id) && it.loadType == MachineLoadType.Level }
    val levelOnlyRecords = currentRecords.any { it.type == PersonalRecordType.MaxMachineSetting } &&
        currentRecords.none { it.type in setOf(PersonalRecordType.MaxWeight, PersonalRecordType.EstimatedOneRepMax) }
    val recommendedTypes = if (
        levelOnlyRecords || (latestRecord == null && levelMachine != null)
    ) {
        listOf(PersonalRecordType.MaxMachineSetting)
    } else exercise.recommendedTrackedRecordTypes()
    val latestScope = latestRecord?.machineProfileUuidSnapshot ?: levelMachine?.uuid
    return recommendedTypes.mapIndexed { index, type ->
        TrackedGymRecord(
            exerciseUuid = exercise.uuid,
            type = type,
            machineProfileUuid = latestScope,
            position = state.appSettings.trackedGymRecords.size + index,
        )
    }
}

private fun availableTrackedRecords(exercise: Exercise, state: GymUiState): List<TrackedGymRecord> {
    val existing = state.appSettings.trackedGymRecords.filter {
        it.exerciseUuid == exercise.uuid && it.type in exercise.supportedTrackedRecordTypes()
    }
    val current = state.personalRecords.filter { it.exerciseId == exercise.id && it.current }
    val choices = buildList {
        exercise.supportedTrackedRecordTypes().forEach { type ->
            val scopes = current.filter { it.type == type && it.secondaryValue == null }
                .map(PersonalRecord::machineProfileUuidSnapshot).distinct()
            if (scopes.isEmpty()) add(TrackedGymRecord(exercise.uuid, type))
            else scopes.forEach { scope -> add(TrackedGymRecord(exercise.uuid, type, machineProfileUuid = scope)) }
        }
        if (
            current.any { it.type == PersonalRecordType.MaxMachineSetting } ||
            state.machines.any { it.supportsExercise(exercise.id) && it.loadType == MachineLoadType.Level }
        ) {
            (current.filter { it.type == PersonalRecordType.MaxMachineSetting }
                .map(PersonalRecord::machineProfileUuidSnapshot) +
                state.machines.filter { it.supportsExercise(exercise.id) && it.loadType == MachineLoadType.Level }.map(GymMachine::uuid))
                .distinct()
                .forEach { scope -> add(TrackedGymRecord(exercise.uuid, PersonalRecordType.MaxMachineSetting, machineProfileUuid = scope)) }
        }
        addAll(existing)
    }
    return choices.distinctBy { listOf(it.type, it.secondaryValue, it.machineProfileUuid) }
        .sortedWith(compareBy<TrackedGymRecord> { exercise.supportedTrackedRecordTypes().indexOf(it.type).let { value -> if (value < 0) Int.MAX_VALUE else value } }
            .thenBy { it.secondaryValue ?: Double.NEGATIVE_INFINITY })
}

@Composable
private fun TrackedRecordsManagerDialog(
    modifier: Modifier,
    state: GymUiState,
    initialExerciseId: Long?,
    onDismiss: () -> Unit,
    onSave: (List<TrackedGymRecord>) -> Unit,
) {
    val allExercises = state.exercises + state.archivedExercises
    val initialExercise = allExercises.firstOrNull { it.id == initialExerciseId }
    val original = state.appSettings.trackedGymRecords.filter { selection ->
        allExercises.firstOrNull { it.uuid == selection.exerciseUuid }
            ?.let { exercise -> selection.type in exercise.supportedTrackedRecordTypes() } != false
    }
    var draft by remember(initialExerciseId, original) {
        mutableStateOf(
            if (initialExercise != null && original.none { it.exerciseUuid == initialExercise.uuid }) {
                original + recommendedTrackedRecords(initialExercise, state)
            } else original,
        )
    }
    var editingExerciseUuid by rememberSaveable(initialExerciseId) {
        mutableStateOf(initialExercise?.uuid ?: draft.firstOrNull()?.exerciseUuid)
    }
    val trackedExerciseUuids = draft.mapTo(linkedSetOf(), TrackedGymRecord::exerciseUuid)
    val editingExercise = allExercises.firstOrNull { it.uuid == editingExerciseUuid }
    val groupedDraft = draft.groupBy(TrackedGymRecord::exerciseUuid)
    val orderedTrackedExercises = allExercises.filter { it.uuid in trackedExerciseUuids }.sortedBy { exercise ->
        draft.filter { it.exerciseUuid == exercise.uuid }.minOfOrNull(TrackedGymRecord::position) ?: Int.MAX_VALUE
    }

    fun replaceExerciseRecords(exerciseUuid: String, records: List<TrackedGymRecord>) {
        val insertionIndex = draft.indexOfFirst { it.exerciseUuid == exerciseUuid }.let { if (it < 0) draft.size else it }
        val remaining = draft.filterNot { it.exerciseUuid == exerciseUuid }.toMutableList()
        remaining.addAll(insertionIndex.coerceAtMost(remaining.size), records)
        draft = remaining.mapIndexed { index, selection -> selection.copy(position = index) }
    }

    ProductivityEditorDialog(
        modifier = modifier,
        testTag = "tracked-records-manager",
        primary = true,
        paneTitle = "Manage Tracked Records",
        onDismissRequest = onDismiss,
        title = { Text("Manage Tracked Records") },
        text = {
            WhipReorderLazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text(
                        if (draft.isEmpty()) "Choose exercises and records to keep at a glance."
                        else "${groupedDraft.size} exercise${if (groupedDraft.size == 1) "" else "s"} · ${draft.size} record${if (draft.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    ExerciseSelectionField(
                        label = "Add Exercise",
                        exercises = state.exercises.filterNot { it.uuid in trackedExerciseUuids },
                        selectedExerciseId = null,
                        onSelect = { id ->
                            val selected = state.exercises.firstOrNull { it.id == id } ?: return@ExerciseSelectionField
                            draft = draft + recommendedTrackedRecords(selected, state)
                            editingExerciseUuid = selected.uuid
                        },
                        modifier = Modifier.fillMaxWidth().testTag("tracked-records-add-exercise"),
                    )
                }
                if (draft.isEmpty()) item {
                    WhipEmptyState(
                        title = "No Tracked Exercises",
                        supportingText = "Add an exercise to choose its progress records.",
                    )
                }
                items(orderedTrackedExercises.size, key = { orderedTrackedExercises[it].uuid }) { exerciseIndex ->
                    val exercise = orderedTrackedExercises[exerciseIndex]
                    val selectedRecords = draft.filter { it.exerciseUuid == exercise.uuid }
                    val expanded = editingExerciseUuid == exercise.uuid
                    val reorderInteraction = rememberWhipReorderInteractionState()
                    Surface(
                        modifier = Modifier.fillMaxWidth().whipReorderItem(
                            reorderInteraction,
                            layoutPosition = exerciseIndex + 1,
                            layoutScope = "tracked-record-exercises",
                        ),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                WhipReorderHandle(
                                    label = "${exercise.name} tracked records",
                                    canMovePrevious = exerciseIndex > 0,
                                    canMoveNext = exerciseIndex < orderedTrackedExercises.lastIndex,
                                    position = exerciseIndex + 1,
                                    total = orderedTrackedExercises.size,
                                    interactionState = reorderInteraction,
                                    moveWholeItem = true,
                                    layoutScope = "tracked-record-exercises",
                                    onMove = { delta ->
                                        val moved = moveListItem(orderedTrackedExercises, exerciseIndex, delta)
                                        draft = moved.flatMap { movedExercise ->
                                            draft.filter { it.exerciseUuid == movedExercise.uuid }
                                        }.mapIndexed { index, selection -> selection.copy(position = index) }
                                    },
                                )
                                Box(Modifier.weight(1f)) {
                                    DisclosureRow(
                                        title = exercise.name,
                                        supportingText = buildString {
                                            append("${selectedRecords.size} record${if (selectedRecords.size == 1) "" else "s"}")
                                            if (exercise.archived) append(" · Archived")
                                        },
                                        expanded = expanded,
                                        onClick = { editingExerciseUuid = if (expanded) null else exercise.uuid },
                                    )
                                }
                            }
                            if (expanded) {
                                if (!exercise.includeInPersonalRecords) {
                                    Text(
                                        "Personal record updates are off for this exercise. Tracked values will remain empty until you enable them in Exercise settings.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(top = 8.dp),
                                    )
                                }
                                Text(
                                    "Records to show",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                val availableChoices = availableTrackedRecords(exercise, state)
                                val orderedChoices = selectedRecords + availableChoices.filterNot { choice ->
                                    selectedRecords.any { it.sameTrackedChoice(choice) }
                                }
                                orderedChoices.forEach { choice ->
                                    androidx.compose.runtime.key(choice) {
                                    val recordReorderInteraction = rememberWhipReorderInteractionState()
                                    val isSelected = selectedRecords.any { it.sameTrackedChoice(choice) }
                                    val selectedIndex = selectedRecords.indexOfFirst { it.sameTrackedChoice(choice) }
                                    val scopeName = choice.machineProfileUuid?.let { scope ->
                                        (state.machines + state.archivedMachines).firstOrNull { it.uuid == scope }?.displayName
                                            ?: "Saved equipment"
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .whipReorderItem(
                                                recordReorderInteraction,
                                                layoutPosition = (selectedIndex + 1).takeIf { isSelected },
                                                layoutScope = "tracked-records-${exercise.uuid}".takeIf { isSelected },
                                            )
                                            .heightIn(min = 48.dp)
                                            .toggleable(
                                                value = isSelected,
                                                role = Role.Checkbox,
                                                onValueChange = { nextSelected ->
                                                if (!nextSelected) {
                                                    replaceExerciseRecords(
                                                        exercise.uuid,
                                                        selectedRecords.filterNot { it.sameTrackedChoice(choice) },
                                                    )
                                                } else {
                                                    replaceExerciseRecords(exercise.uuid, selectedRecords + choice)
                                                }
                                                },
                                            )
                                            .semantics {
                                                contentDescription = "${if (isSelected) "Stop tracking" else "Track"} ${choice.trackedRecordLabel()}"
                                                stateDescription = if (isSelected) "Tracked" else "Not tracked"
                                            },
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (isSelected) {
                                            WhipReorderHandle(
                                                label = "${choice.trackedRecordLabel()} for ${exercise.name}",
                                                canMovePrevious = selectedIndex > 0,
                                                canMoveNext = selectedIndex in 0 until selectedRecords.lastIndex,
                                                position = selectedIndex + 1,
                                                total = selectedRecords.size,
                                                interactionState = recordReorderInteraction,
                                                moveWholeItem = true,
                                                layoutScope = "tracked-records-${exercise.uuid}",
                                                onMove = { delta ->
                                                    replaceExerciseRecords(
                                                        exercise.uuid,
                                                        moveListItem(selectedRecords, selectedIndex, delta),
                                                    )
                                                },
                                            )
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text(choice.trackedRecordLabel())
                                            val description = when {
                                                choice.type == PersonalRecordType.EstimatedOneRepMax ->
                                                    "${exercise.oneRepMaxFormula.name} · eligible sets up to ${state.appSettings.oneRepMaxRepCutoff} reps"
                                                scopeName != null -> scopeName
                                                else -> null
                                            }
                                            description?.let {
                                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = null,
                                            modifier = Modifier.clearAndSetSemantics {},
                                        )
                                    }
                                    }
                                }
                                WhipTextButton(
                                    onClick = {
                                        draft = draft.filterNot { it.exerciseUuid == exercise.uuid }
                                        editingExerciseUuid = draft.firstOrNull { it.exerciseUuid != exercise.uuid }?.exerciseUuid
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("Remove ${exercise.name} from Tracked Records") }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            WhipTextButton(
                onClick = {
                    onSave(draft.mapIndexed { index, selection -> selection.copy(position = index) })
                },
                modifier = Modifier.testTag("tracked-records-save"),
            ) { Text("Save") }
        },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun TrackedGymRecord.sameTrackedChoice(other: TrackedGymRecord): Boolean =
    exerciseUuid == other.exerciseUuid && type == other.type && secondaryValue == other.secondaryValue &&
        machineProfileUuid == other.machineProfileUuid

@Composable
internal fun GymProgressContent(
    state: GymUiState,
    onOpenExercises: () -> Unit,
    onOpenWorkout: () -> Unit,
    onOpenWorkoutHistory: (Long) -> Unit,
    onManageTrackedRecords: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedExerciseId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(state.exercises) {
        if (state.exercises.none { it.id == selectedExerciseId }) selectedExerciseId = state.exercises.firstOrNull()?.id
    }
    if (state.exercises.isEmpty()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("gym-progress-list"),
            contentPadding = WhipPageContentPadding,
            verticalArrangement = Arrangement.spacedBy(WhipSpacing.compact),
        ) {
            item {
                WhipPageHeader(
                    title = "Progress",
                    supportingText = "Charts are built from your completed workouts and keep their source records.",
                )
            }
            item {
                WhipEmptyState(
                    title = "No Exercises to Track",
                    supportingText = "Create an exercise before choosing records or exploring a progress trend.",
                    primaryActionLabel = "Open Exercise Library",
                    onPrimaryAction = onOpenExercises,
                )
            }
        }
        return
    }
    if (state.history.isEmpty()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("gym-progress-list"),
            contentPadding = WhipPageContentPadding,
            verticalArrangement = Arrangement.spacedBy(WhipSpacing.compact),
        ) {
            item {
                WhipPageHeader(
                    title = "Progress",
                    supportingText = "Progress is calculated from completed workout sets; exercise defaults never appear as results.",
                    modifier = Modifier.testTag("gym-progress-title"),
                )
            }
            item {
                TrackedRecordsSection(
                    state = state,
                    onManage = onManageTrackedRecords,
                    onOpenWorkoutHistory = onOpenWorkoutHistory,
                )
            }
            item {
                WhipEmptyState(
                    title = "No Progress Data Yet",
                    supportingText = "Complete a workout to create trustworthy records, trends, and weekly summaries. Your existing exercise setup is ready to use.",
                    primaryActionLabel = "Start a Workout",
                    onPrimaryAction = onOpenWorkout,
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
    var comparisonIds by rememberSaveable { mutableStateOf(emptySet<Long>()) }
    var customFrom by rememberSaveable { mutableStateOf("") }
    var customTo by rememberSaveable { mutableStateOf("") }
    var showAllChartData by rememberSaveable { mutableStateOf(false) }
    var graphOptionsExpanded by rememberSaveable { mutableStateOf(false) }
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
    val through = LocalWhipToday.current
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
            machineLevelDirection = selectedMachine?.levelDirection
                ?: MachineLevelDirection.HigherNumberMoreResistance,
        ).map { point -> point.copy(value = metric.displayValue(point.value, displayWeightUnitId, state.appSettings.distanceUnitId)) }
    }.orEmpty()
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
        contentPadding = WhipPageContentPadding,
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.compact),
    ) {
        item {
            WhipPageHeader(
                title = "Progress",
                supportingText = "Keep your chosen benchmarks at a glance, then explore the complete workout history in a trend.",
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
                    "${exercisePoints.size} point${if (exercisePoints.size == 1) "" else "s"} · range ${formatNumber(minimum, state.appSettings.numberPrecision)}–" +
                        "${formatNumber(maximum, state.appSettings.numberPrecision)} $unit · " +
                        "change ${if (change > 0) "+" else ""}${formatNumber(change, state.appSettings.numberPrecision)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("gym-chart-summary").semantics { contentDescription = summary },
                )
            }
        }
        item {
            TrackedRecordsSection(
                state = state,
                onManage = onManageTrackedRecords,
                onOpenWorkoutHistory = onOpenWorkoutHistory,
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                HorizontalDivider()
                Text("Explore a Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (state.history.isEmpty()) "Finish a workout to create your first progress data point."
                    else "Choose an exercise and metric. Every point keeps its source workout.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.history.isEmpty()) {
                    WhipTextButton(onClick = onOpenWorkout) { Text("Open Workout") }
                }
            }
        }
        item {
            ExerciseSelectionField(
                label = "Exercise",
                exercises = state.exercises,
                selectedExerciseId = selectedExerciseId,
                onSelect = { id ->
                    val selected = state.exercises.firstOrNull { it.id == id } ?: return@ExerciseSelectionField
                    selectedExerciseId = selected.id
                    metric = runCatching { GymGraphMetric.valueOf(selected.defaultGraphMetric) }
                        .getOrDefault(GymGraphMetric.EstimatedOneRepMax)
                },
                modifier = Modifier.fillMaxWidth().testTag("gym-progress-exercise-selector"),
            )
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
            if (metric == GymGraphMetric.EstimatedOneRepMax) {
                val formulas = exercisePlacements.map(WorkoutExercise::oneRepMaxFormulaSnapshot).distinct()
                    .ifEmpty { listOfNotNull(exercise?.oneRepMaxFormula) }
                Text(
                    buildString {
                        append("Formula: ")
                        append(formulas.joinToString(" and ") { it.name })
                        append(" · eligible sets up to ${state.appSettings.oneRepMaxRepCutoff} reps")
                        if (state.appSettings.adjustE1rmForEffort) append(" · adjusted using recorded RIR or RPE")
                        if (formulas.size > 1) append(". Each point uses the formula saved with its source workout.")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("gym-e1rm-formula"),
                )
            }
        }
        item {
            DisclosureRow(
                title = "Graph Options",
                supportingText = "${range.uiLabel()} · ${aggregation.uiLabel()}",
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
                    second = { field -> Column(field) { GymEnumDropdown("Group points", GymGraphAggregation.entries, aggregation, GymGraphAggregation::uiLabel) { aggregation = it } } },
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
                    ExerciseComparisonField(
                        exercises = state.exercises,
                        excludedExerciseId = selectedExerciseId,
                        selectedExerciseIds = comparisonIds,
                        onSelectionChange = { comparisonIds = it },
                        modifier = Modifier.fillMaxWidth().testTag("gym-progress-comparison-selector"),
                    )
                }
            }
            if (selectedMachine?.loadType == MachineLoadType.Level) {
                Text(
                    selectedMachine.levelDirection.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                Text("Data Points", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (exercisePoints.size > 8) {
                    WhipTextButton(onClick = { showAllChartData = !showAllChartData }) {
                        Text(if (showAllChartData) "Show Latest 8" else "Show All ${exercisePoints.size}")
                    }
                }
                (if (showAllChartData) exercisePoints else exercisePoints.takeLast(8)).forEach { point ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable(onClickLabel = "Open details for ${point.date}") {
                            selectedChartSeriesName = primarySeriesName
                            selectedChartPointDate = point.date
                        },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(point.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${formatNumber(point.value, state.appSettings.numberPrecision)} $displayUnit",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
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
            val exerciseByUuid = state.exercises.associateBy(Exercise::uuid)
            val trackedImprovements = state.appSettings.trackedGymRecords.count { selection ->
                val trackedExercise = exerciseByUuid[selection.exerciseUuid] ?: return@count false
                if (selection.type !in trackedExercise.supportedTrackedRecordTypes()) return@count false
                val record = selection.resolveForExercise(trackedExercise.id, state.personalRecords) ?: return@count false
                val achievedDate = java.time.Instant.ofEpochMilli(record.achievedAtMillis)
                    .atZone(state.appSettings.zoneId()).toLocalDate()
                achievedDate in summary.weekStart..summary.weekStart.plusDays(6)
            }
            Text("$trackedImprovements tracked record improvement${if (trackedImprovements == 1) "" else "s"}")
            val categoryPositionById = state.categories.associate { it.id to it.position }
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
                    val linked = state.categoryLinks.filter { it.exerciseId == exerciseId }
                        .map { it.categoryId }
                        .sortedBy { categoryPositionById[it] ?: Int.MAX_VALUE }
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
    selectedChartPoint?.let { point ->
        PaneAwareAlertDialog(
            modifier = modifier,
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
                                selectedChartPointDate = null
                                onOpenWorkoutHistory(sessionId)
                            },
                            modifier = Modifier.fillMaxWidth().testTag("gym-chart-point-open-workout"),
                        ) { Text("View Workout in History") }
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
    modifier: Modifier = Modifier,
) {
    val dialogModifier = modifier
    val weightUnit = state.appSettings.gymWeightUnitId
    val weightSymbol = unitSymbol(weightUnit)
    var weight by rememberSaveable(weightUnit) { mutableStateOf("") }
    var reps by rememberSaveable { mutableStateOf("") }
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
    val lastEligibleSet = state.allSets.asSequence()
        .filter { it.completed && it.deletedAtMillis == null && it.canonicalWeightKg != null && it.repetitions != null }
        .maxByOrNull { it.completedAtMillis ?: it.updatedAtMillis }
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = WhipPageContentPadding,
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.compact),
    ) {
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
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                lastEligibleSet?.let { set ->
                    val lastWeight = massFromKilograms(requireNotNull(set.canonicalWeightKg), weightUnit)
                    WhipOutlinedButton(onClick = {
                        weight = editableNumber(lastWeight)
                        reps = requireNotNull(set.repetitions).toString()
                        knownOneRepMax = false
                    }) { Text("Use Last Set · ${formatNumber(lastWeight, state.appSettings.numberPrecision)} $weightSymbol × ${set.repetitions}") }
                }
                WhipTextButton(onClick = {
                    weight = if (weightUnit == "pound") "175" else "80"
                    reps = "8"
                    knownOneRepMax = false
                }) { Text("Use Example") }
            }
            Text("Example values are never inserted until you choose Use Example.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
            if (estimate == null) Text("Enter a positive weight and 1–36 repetitions to calculate an estimate.") else {
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
    modifier: Modifier = Modifier,
    focusedRoutineId: Long? = null,
    onDeleteRequest: (GymRoutine) -> Unit,
    onOpenActiveWorkout: () -> Unit,
    onEditorStateChange: (Boolean) -> Unit = {},
    createRequested: Boolean = false,
    onCreateRequestConsumed: () -> Unit = {},
    onReorderModeChange: (Boolean) -> Unit = {},
    reorderDismissRequest: Int = 0,
) {
    val dialogModifier = modifier
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingRoutineId by rememberSaveable { mutableStateOf<Long?>(null) }
    val editing = editingRoutineId?.let { id -> (state.routines + state.archivedRoutines).firstOrNull { it.id == id } }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var actionMenuId by rememberSaveable { mutableStateOf<Long?>(null) }
    var positionRoutineId by rememberSaveable { mutableStateOf<Long?>(null) }
    var resetRoutineId by rememberSaveable { mutableStateOf<Long?>(null) }
    var restoreTrainingMaxEligibilityRoutineId by rememberSaveable { mutableStateOf<Long?>(null) }
    var reordering by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(createRequested) {
        if (createRequested) {
            showEditor = true
            onEditorStateChange(true)
            onCreateRequestConsumed()
        }
    }
    LaunchedEffect(focusedRoutineId, state.archivedRoutines) {
        if (focusedRoutineId != null) {
            showArchived = state.archivedRoutines.any { it.id == focusedRoutineId }
        }
    }
    val source = if (showArchived) state.archivedRoutines else state.routines
    val visible = source.filter {
        focusedRoutineId == null || it.id == focusedRoutineId
    }
    BackHandler(enabled = reordering) { reordering = false }
    DisposableEffect(reordering) {
        onReorderModeChange(reordering)
        onDispose { if (reordering) onReorderModeChange(false) }
    }
    LaunchedEffect(reorderDismissRequest) {
        if (reorderDismissRequest > 0) reordering = false
    }
    LaunchedEffect(showArchived, focusedRoutineId) {
        if (showArchived || focusedRoutineId != null) reordering = false
    }
    if (showEditor || editing != null) {
        val initial = editing?.let { routine -> routineDraftForEditing(state, routine) }
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
            onReorderPrescriptionSchemes = viewModel::reorderRepPrescriptionSchemes,
            onDeletePrescriptionScheme = viewModel::deleteRepPrescriptionScheme,
        )
        return
    }
    WhipReorderLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = WhipPageContentPadding,
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.compact),
    ) {
        item {
            WhipPageHeader(
                title = "Routines",
                supportingText = if (focusedRoutineId == null) {
                    "Reusable multi-day templates. Starting one copies it into a new workout without changing the template."
                } else "Showing the routine opened from search.",
            ) {
                if (!reordering && focusedRoutineId == null && state.routines.size > 1) {
                    WhipPageIconAction(
                        icon = Icons.Outlined.DragHandle,
                        label = if (showArchived) "Show active and reorder all Routines" else "Reorder Routines",
                        onClick = {
                            showArchived = false
                            reordering = true
                        },
                    )
                }
            }
        }
        if (!reordering && focusedRoutineId == null) {
        item { ToggleRow("Show archived", showArchived) { showArchived = it } }
        }
        if (reordering) item {
            WhipReorderModeBar(
                itemLabel = "Routines",
                onDone = { reordering = false },
                boundaryNote = "Archived Routines stay outside this order.",
            )
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
            val days = state.routineDays.filter { it.routineId == routine.id }.sortedBy { it.position }
            val dayIds = days.mapTo(mutableSetOf(), RoutineDay::id)
            val mainPlacements = state.routineExercises.filter { placement ->
                placement.routineDayId in dayIds &&
                    (placement.placementKind == RoutinePlacementKind.MainLift ||
                        state.routineSets.any { set ->
                            set.routineExerciseId == placement.id && set.draft.workSection == RoutineWorkSection.Main
                        })
            }.distinctBy { it.exerciseId }
            val heldMainLiftNames = mainPlacements.filterNot { it.trainingMaxIncreaseEligible }
                .map { placement ->
                    (state.exercises + state.archivedExercises)
                        .firstOrNull { it.id == placement.exerciseId }?.name ?: "Exercise ${placement.exerciseId}"
                }
            val programmed = routine.programKind != RoutineProgramKind.Static
            val hasStaticLoadCycle = !programmed && days.any { day ->
                state.routineExercises.any { placement ->
                    placement.routineDayId == day.id && placement.progressionPercentages.isNotEmpty()
                }
            }
            val editingBlockedByActiveWorkout = state.activeSession?.sourceRoutineId == routine.id
            val nextProgramDay = days.firstOrNull { it.position == routine.nextProgramDayPosition }
                ?: days.getOrNull(routine.nextProgramDayPosition)
                ?: days.firstOrNull()
            var phaseRoadmapExpanded by rememberSaveable(routine.id, "phase-roadmap-expanded") { mutableStateOf(false) }
            var roadmapPhaseIndex by rememberSaveable(routine.id, "phase-roadmap-index") {
                mutableStateOf(routine.currentProgramPhaseIndex)
            }
            LaunchedEffect(routine.programPhaseCount) {
                roadmapPhaseIndex = roadmapPhaseIndex.coerceIn(0, (routine.programPhaseCount - 1).coerceAtLeast(0))
            }
            val reorderInteraction = rememberWhipReorderInteractionState()
            Card(
                modifier = Modifier.fillMaxWidth().whipReorderItem(
                    reorderInteraction,
                    layoutPosition = routineIndex + 1,
                    layoutScope = "routine-browse",
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (reordering && !showArchived) {
                            WhipReorderHandle(
                                label = routine.name,
                                canMovePrevious = routineIndex > 0,
                                canMoveNext = routineIndex < visible.lastIndex,
                                position = routineIndex + 1,
                                total = visible.size,
                                interactionState = reorderInteraction,
                                moveWholeItem = true,
                                layoutScope = "routine-browse",
                                reserveWhenUnavailable = true,
                                onMove = { delta ->
                                    viewModel.reorderRoutines(moveListItem(visible, routineIndex, delta).map(GymRoutine::id))
                                },
                            )
                        }
                        Text(routine.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (!reordering && !editingBlockedByActiveWorkout) ItemEditButton("routine", routine.name, onEdit = {
                            editingRoutineId = routine.id
                            onEditorStateChange(true)
                        })
                        if (!reordering) Box {
                            IconButton(onClick = { actionMenuId = routine.id }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = "More options for routine ${routine.name}", modifier = Modifier.size(28.dp))
                            }
                            DropdownMenu(expanded = actionMenuId == routine.id, onDismissRequest = { actionMenuId = null }) {
                                WhipMenuItem(label = "Duplicate", onClick = { actionMenuId = null; viewModel.duplicateRoutine(routine.id) })
                                WhipMenuItem(label = if (routine.pinned) "Unpin from Whip Home" else "Pin to Whip Home", onClick = { actionMenuId = null; viewModel.setRoutinePinned(routine.id, !routine.pinned) })
                                WhipMenuItem(label = if (routine.archived) "Restore" else "Archive", onClick = { actionMenuId = null; viewModel.setRoutineArchived(routine.id, !routine.archived) })
                                if (programmed && !editingBlockedByActiveWorkout) {
                                    WhipMenuItem(
                                        label = "Set Program Position",
                                        onClick = { actionMenuId = null; positionRoutineId = routine.id },
                                    )
                                    WhipMenuItem(
                                        label = "Reset Program Progress",
                                        onClick = { actionMenuId = null; resetRoutineId = routine.id },
                                    )
                                    if (!routine.trainingMaxIncreaseEligible) {
                                        WhipMenuItem(
                                            label = "Restore Training Max Eligibility",
                                            onClick = {
                                                actionMenuId = null
                                                restoreTrainingMaxEligibilityRoutineId = routine.id
                                            },
                                        )
                                    }
                                } else if (hasStaticLoadCycle && !editingBlockedByActiveWorkout) {
                                    WhipMenuItem(
                                        label = "Reset Load Cycle",
                                        onClick = { actionMenuId = null; resetRoutineId = routine.id },
                                    )
                                }
                                HorizontalDivider()
                                WhipMenuItem(
                                    label = "Delete Permanently",
                                    role = WhipMenuItemRole.Destructive,
                                    onClick = { actionMenuId = null; onDeleteRequest(routine) },
                                )
                            }
                        }
                    }
                    if (routine.notes.isNotBlank()) Text(routine.notes)
                    if (editingBlockedByActiveWorkout) {
                        Text(
                            "Finish or discard the active workout before editing this routine or changing its program position.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text("${days.size} day${if (days.size == 1) "" else "s"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (programmed) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().testTag("routine-program-status-${routine.id}"),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    routineProgramStatusLabel(routine, nextProgramDay?.name),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "Finishing the next workout advances this program. Starting another day out of order uses the current phase but does not advance program progress.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Text(
                                    if (heldMainLiftNames.isEmpty()) {
                                        "Training Max progression · All lifts eligible"
                                    } else {
                                        "Training Max held · ${heldMainLiftNames.joinToString()}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (heldMainLiftNames.isEmpty()) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.testTag("routine-training-max-eligibility-${routine.id}"),
                                )
                                WhipTextButton(
                                    onClick = {
                                        phaseRoadmapExpanded = !phaseRoadmapExpanded
                                        if (phaseRoadmapExpanded) roadmapPhaseIndex = routine.currentProgramPhaseIndex
                                    },
                                    modifier = Modifier.testTag("routine-program-roadmap-toggle-${routine.id}"),
                                ) {
                                    Text(if (phaseRoadmapExpanded) "Hide Phase Roadmap" else "Browse All Phases")
                                }
                                if (phaseRoadmapExpanded) {
                                    val phaseLabel = routine.programPhaseLabels.getOrNull(roadmapPhaseIndex)
                                        ?.takeIf(String::isNotBlank) ?: "Phase ${roadmapPhaseIndex + 1}"
                                    val phaseRole = when (routine.programPhaseRoles.getOrNull(roadmapPhaseIndex)) {
                                        RoutineProgramPhaseRole.Leader -> "Leader"
                                        RoutineProgramPhaseRole.Anchor -> "Anchor"
                                        RoutineProgramPhaseRole.Deload -> "Deload"
                                        RoutineProgramPhaseRole.TrainingMaxTest -> "Training Max Test"
                                        RoutineProgramPhaseRole.PersonalRecordTest -> "PR Test"
                                        else -> "Standard"
                                    }
                                    Text(
                                        "Preview ${roadmapPhaseIndex + 1} of ${routine.programPhaseCount} · $phaseLabel · $phaseRole" +
                                            if (roadmapPhaseIndex == routine.currentProgramPhaseIndex) " · Current" else "",
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.testTag("routine-program-roadmap-phase-${routine.id}"),
                                    )
                                    Text(
                                        if (roadmapPhaseIndex in routine.trainingMaxAdvanceAfterPhaseIndices) {
                                            "Training Max advances after this phase when required Main work is completed."
                                        } else {
                                            "No Training Max advance after this phase."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        WhipOutlinedButton(
                                            enabled = roadmapPhaseIndex > 0,
                                            onClick = { roadmapPhaseIndex-- },
                                            modifier = Modifier.weight(1f).testTag("routine-program-roadmap-previous-${routine.id}"),
                                        ) { Text("Previous") }
                                        WhipOutlinedButton(
                                            enabled = roadmapPhaseIndex < routine.programPhaseCount - 1,
                                            onClick = { roadmapPhaseIndex++ },
                                            modifier = Modifier.weight(1f).testTag("routine-program-roadmap-next-${routine.id}"),
                                        ) { Text("Next") }
                                    }
                                }
                            }
                        }
                    }
                    val routineHistory = state.history.filter { it.sourceRoutineId == routine.id }
                    Text(
                        if (routineHistory.isEmpty()) "No performed sessions yet" else
                            "${routineHistory.size} performed session${if (routineHistory.size == 1) "" else "s"} · last ${routineHistory.maxOf { it.localDate }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!reordering && programmed && nextProgramDay != null) {
                        val nextDayExercises = state.routineExercises.filter { it.routineDayId == nextProgramDay.id }
                        val nextNeedsEquipment = nextDayExercises.any {
                            it.equipmentBindingState == RoutineEquipmentBindingState.NeedsEquipment
                        }
                        WhipButton(
                            onClick = {
                                when {
                                    state.activeSession != null -> onOpenActiveWorkout()
                                    nextNeedsEquipment -> {
                                        editingRoutineId = routine.id
                                        onEditorStateChange(true)
                                    }
                                    else -> viewModel.startRoutine(routine.id, null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("routine-start-next-${routine.id}"),
                        ) {
                            Text(
                                when {
                                    state.activeSession != null -> "Open Active Workout"
                                    nextNeedsEquipment -> "Resolve Equipment for Next · ${nextProgramDay.name}"
                                    else -> "Start Next · ${nextProgramDay.name}"
                                },
                            )
                        }
                    }
                    if (!reordering) days.forEach { day ->
                        val dayExercises = state.routineExercises.filter { it.routineDayId == day.id }
                        val count = dayExercises.size
                        val needsEquipment = dayExercises.filter {
                            it.equipmentBindingState == RoutineEquipmentBindingState.NeedsEquipment
                        }
                        val archivedEquipment = dayExercises.count { placement ->
                            placement.machineId in state.archivedMachines.mapTo(mutableSetOf(), GymMachine::id)
                        }
                        if (!programmed) {
                            val nextCycleMultipliers = dayExercises.mapNotNull { placement ->
                                placement.progressionPercentages.takeIf(List<Double>::isNotEmpty)?.let { values ->
                                    values[day.progressionIndex % values.size]
                                }
                            }.distinct()
                            if (nextCycleMultipliers.isNotEmpty()) {
                                Text(
                                    "Next load cycle · step ${day.progressionIndex + 1} · " +
                                        nextCycleMultipliers.joinToString(" / ") { value -> "${editableNumber(value)}%" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.testTag("routine-static-cycle-${day.id}"),
                                )
                            }
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
                        if (!programmed || day.id != nextProgramDay?.id) WhipTextButton(onClick = {
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
                                    programmed -> "Start Out of Order · ${day.name} · ${quantityLabel(count, "exercise")}"
                                    else -> "Start ${day.name} · ${quantityLabel(count, "exercise")}"
                                },
                            )
                        }
                    }
                }
            }
        }
    }
    positionRoutineId?.let { routineId ->
        val routine = (state.routines + state.archivedRoutines).firstOrNull { it.id == routineId }
        val days = state.routineDays.filter { it.routineId == routineId }.sortedBy { it.position }
        if (routine == null || days.isEmpty()) {
            positionRoutineId = null
        } else {
            RoutineProgramPositionDialog(
                routine = routine,
                days = days,
                onDismiss = { positionRoutineId = null },
                onSave = { phaseIndex, dayPosition, cycle ->
                    positionRoutineId = null
                    viewModel.setRoutineProgramPosition(routine.id, phaseIndex, dayPosition, cycle)
                },
            )
        }
    }
    resetRoutineId?.let { routineId ->
        val routine = (state.routines + state.archivedRoutines).firstOrNull { it.id == routineId }
        if (routine == null) {
            resetRoutineId = null
        } else {
            PaneAwareAlertDialog(
                onDismissRequest = { resetRoutineId = null },
                title = {
                    Text(
                        if (routine.programKind == RoutineProgramKind.Static) "Reset ${routine.name} Load Cycle?"
                        else "Reset ${routine.name} Program Progress?",
                    )
                },
                text = {
                    Text(
                        if (routine.programKind == RoutineProgramKind.Static) {
                            "This returns every day to its first load multiplier. Routine prescriptions and workout History are not deleted."
                        } else {
                            "This returns the program to cycle 1, its first phase, and its first day. Routine prescriptions and workout History are not deleted."
                        },
                    )
                },
                confirmButton = {
                    WhipTextButton(onClick = {
                        resetRoutineId = null
                        viewModel.resetRoutineProgramProgress(routine.id)
                    }) { Text("Reset Progress") }
                },
                dismissButton = { WhipTextButton(onClick = { resetRoutineId = null }) { Text("Cancel") } },
            )
        }
    }
    restoreTrainingMaxEligibilityRoutineId?.let { routineId ->
        val routine = (state.routines + state.archivedRoutines).firstOrNull { it.id == routineId }
        if (routine == null) {
            restoreTrainingMaxEligibilityRoutineId = null
        } else {
            PaneAwareAlertDialog(
                onDismissRequest = { restoreTrainingMaxEligibilityRoutineId = null },
                title = { Text("Restore Training Max Progression?") },
                text = {
                    Text(
                        "This makes future required Main work eligible to advance the Training Max at the next configured boundary. It does not change completed workouts, the current cycle, or any Training Max now.",
                    )
                },
                confirmButton = {
                    WhipTextButton(onClick = {
                        restoreTrainingMaxEligibilityRoutineId = null
                        viewModel.restoreRoutineTrainingMaxEligibility(routine.id)
                    }) { Text("Restore Eligibility") }
                },
                dismissButton = {
                    WhipTextButton(onClick = { restoreTrainingMaxEligibilityRoutineId = null }) { Text("Cancel") }
                },
            )
        }
    }
}

@Composable
private fun RoutineProgramPositionDialog(
    routine: GymRoutine,
    days: List<RoutineDay>,
    onDismiss: () -> Unit,
    onSave: (phaseIndex: Int, dayPosition: Int, cycle: Int) -> Unit,
) {
    var phaseIndex by rememberSaveable(routine.id) { mutableStateOf(routine.currentProgramPhaseIndex) }
    var dayPosition by rememberSaveable(routine.id) { mutableStateOf(routine.nextProgramDayPosition) }
    var cycleText by rememberSaveable(routine.id) { mutableStateOf(routine.currentProgramCycle.toString()) }
    val cycle = cycleText.toIntOrNull()
    PaneAwareAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set ${routine.name} Program Position") },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Use this to correct or resume a cycle. It changes future starts only; workout History stays unchanged.")
                Text("Phase", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(routine.programPhaseCount) { index ->
                        val label = routine.programPhaseLabels.getOrNull(index)?.takeIf(String::isNotBlank)
                            ?: "Phase ${index + 1}"
                        WhipFilterChip(
                            selected = phaseIndex == index,
                            onClick = { phaseIndex = index },
                            label = { Text(label) },
                        )
                    }
                }
                Text("Next Day", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    days.forEach { day ->
                        WhipFilterChip(
                            selected = dayPosition == day.position,
                            onClick = { dayPosition = day.position },
                            label = { Text(day.name) },
                        )
                    }
                }
                OutlinedTextField(
                    value = cycleText,
                    onValueChange = { cycleText = it.filter(Char::isDigit).take(6) },
                    label = { Text("Cycle") },
                    supportingText = { Text("Cycle must be 1 or higher.") },
                    isError = cycleText.isNotEmpty() && (cycle == null || cycle < 1),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            WhipButton(
                enabled = cycle != null && cycle >= 1 && phaseIndex in 0 until routine.programPhaseCount &&
                    days.any { it.position == dayPosition },
                onClick = { onSave(phaseIndex, dayPosition, requireNotNull(cycle)) },
            ) { Text("Set Position") }
        },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

internal fun routineProgramStatusLabel(routine: GymRoutine, nextDayName: String?): String {
    val program = when (routine.programKind) {
        RoutineProgramKind.Static -> "Static Routine"
        RoutineProgramKind.Custom -> "Program"
        RoutineProgramKind.FiveThreeOne -> "5/3/1"
        RoutineProgramKind.FiveThreeOneClassic -> "Classic 5/3/1"
        RoutineProgramKind.FiveSPro -> "5s PRO"
        RoutineProgramKind.BoringButBig -> "Boring But Big"
        RoutineProgramKind.FirstSetLast -> "First Set Last"
    }
    val phase = routine.programPhaseLabels.getOrNull(routine.currentProgramPhaseIndex)
        ?.takeIf(String::isNotBlank)
        ?: "Phase ${routine.currentProgramPhaseIndex + 1}"
    return buildList {
        add(program)
        add("Cycle ${routine.currentProgramCycle}")
        add(phase)
        nextDayName?.takeIf(String::isNotBlank)?.let { add("Next · $it") }
    }.joinToString(" · ")
}

internal fun routineDraftForEditing(state: GymUiState, routine: GymRoutine): RoutineDraft = RoutineDraft(
    name = routine.name,
    notes = routine.notes,
    days = state.routineDays.filter { it.routineId == routine.id }.sortedBy { it.position }.map { day ->
        RoutineDayDraft(
            name = day.name,
            exercises = state.routineExercises.filter { it.routineDayId == day.id }.sortedBy { it.position }.map { routineExercise ->
                RoutineExerciseDraft(
                    exerciseId = routineExercise.exerciseId,
                    notes = routineExercise.notes,
                    groupKey = routineExercise.groupKey,
                    plannedSets = state.routineSets.filter { it.routineExerciseId == routineExercise.id }
                        .sortedBy { it.position }
                        .map { it.draft },
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
                    trainingMaxPercent = routineExercise.trainingMaxPercent,
                    progressionPercentages = routineExercise.progressionPercentages,
                    alternativeExerciseIds = routineExercise.alternativeExerciseIds,
                    trainingMaxValue = routineExercise.trainingMaxValue,
                    trainingMaxUnitId = routineExercise.trainingMaxUnitId,
                    cycleIncrementValue = routineExercise.cycleIncrementValue,
                    trainingMaxSource = routineExercise.trainingMaxSource,
                    mainWorkScheme = routineExercise.mainWorkScheme,
                    supplementalScheme = routineExercise.supplementalScheme,
                    assistanceRole = routineExercise.assistanceRole,
                    placementKind = routineExercise.placementKind,
                    assistanceCategory = routineExercise.assistanceCategory,
                    jokerSetsEnabled = routineExercise.jokerSetsEnabled,
                )
            },
            progressionIndex = day.progressionIndex,
        )
    },
    program = RoutineProgramDraft(
        kind = routine.programKind,
        phaseCount = routine.programPhaseCount,
        phaseLabels = routine.programPhaseLabels,
        phaseRoles = routine.programPhaseRoles,
        trainingMaxAdvanceAfterPhaseIndices = routine.trainingMaxAdvanceAfterPhaseIndices,
        currentPhaseIndexHint = routine.currentProgramPhaseIndex,
        templateKey = routine.programTemplateKey,
        templateRevision = routine.programTemplateRevision,
    ),
    nextProgramDayPositionHint = routine.nextProgramDayPosition,
)

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
            Text(firstDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), style = MaterialTheme.typography.labelSmall)
            Text(lastDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), style = MaterialTheme.typography.labelSmall)
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
                    modifier = Modifier.testTag("gym-chart-point"),
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
    return "$exerciseName $metric chart. ${points.size} point${if (points.size == 1) "" else "s"} from ${first.date} to ${latest.date}. " +
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
    var validationRequested by rememberSaveable(editorKey) { mutableStateOf(false) }
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
    val plateEntries = plates.split(',').map(String::trim).filter(String::isNotBlank)
    val parsedPlateValues = plateEntries.mapNotNull(String::toWhipDoubleOrNull)
    val nameError = "Enter an exercise name".takeIf { name.isBlank() }
    val weightIncrementError = "Weight increment must be above 0".takeIf {
        weightIncrement.toWhipDoubleOrNull()?.let { !it.isFinite() || it <= 0.0 } != false
    }
    val repetitionIncrementError = "Rep increment must be at least 1".takeIf {
        repetitionIncrement.toIntOrNull()?.let { it <= 0 } != false
    }
    val restSecondsError = "Default rest must be 1–86,400 seconds, or blank".takeIf {
        restSeconds.isNotBlank() && restSeconds.toIntOrNull()?.let { it !in 1..86_400 } != false
    }
    val effectiveBodyweightError = "Effective bodyweight must be from 0–200%".takeIf {
        effectiveBodyweight.toWhipDoubleOrNull()?.let { !it.isFinite() || it !in 0.0..200.0 } != false
    }
    val barWeightError = "Bar or base weight must be 0 or more".takeIf {
        barWeight.isNotBlank() && barWeight.toWhipDoubleOrNull()?.let { !it.isFinite() || it < 0.0 } != false
    }
    val platesError = "Plates must be comma-separated positive numbers".takeIf {
        parsedPlateValues.size != plateEntries.size || parsedPlateValues.any { !it.isFinite() || it <= 0.0 }
    }
    val validationErrors = listOfNotNull(
        nameError,
        weightIncrementError,
        repetitionIncrementError,
        restSecondsError,
        effectiveBodyweightError,
        barWeightError,
        platesError,
    )
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
        primary = true,
        paneTitle = if (exercise == null) "Create Exercise" else "Edit Exercise",
        onDismissRequest = { if (!saving) requestDismiss() },
        title = { Text(if (exercise == null) "Create Exercise" else "Edit Exercise") },
        text = {
            LazyColumn(
                modifier = Modifier.testTag("exercise-editor-list"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    OutlinedTextField(
                        name,
                        { name = it.replace('\n', ' ').replace('\r', ' ').take(100) },
                        label = { Text("Name *") },
                        supportingText = { Text(nameError.takeIf { validationRequested } ?: "${name.length}/100") },
                        isError = validationRequested && nameError != null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("exercise-editor-name"),
                    )
                }
                if (validationRequested && validationErrors.isNotEmpty()) item {
                    FormValidationSummary(
                        messages = validationErrors,
                        visible = true,
                        testTag = "exercise-save-problem",
                    )
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
                            NumberField(
                                effectiveBodyweight,
                                { effectiveBodyweight = it },
                                "Effective bodyweight percent",
                                isError = validationRequested && effectiveBodyweightError != null,
                                supportingText = effectiveBodyweightError.takeIf { validationRequested },
                            )
                        }
                    }
                }
                item { OutlinedTextField(notes, { notes = it }, label = { Text("Notes / form cues") }, modifier = Modifier.fillMaxWidth()) }
                if (categories.isNotEmpty()) item {
                    OutlinedCard(Modifier.fillMaxWidth().testTag("exercise-library-categories")) {
                        Column(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("Exercise Library categories", style = MaterialTheme.typography.labelLarge)
                            Text(
                                "These are browsing and analytics labels stored on this exercise. They do not assign Push, Pull, or Single-leg/Core roles inside a routine; those are chosen per routine day.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            categories.forEach { category ->
                                WhipMultiChoiceRow(
                                    label = "${category.name} · ${category.kind}",
                                    checked = category.id in categoryIds,
                                    onCheckedChange = { checked ->
                                        categoryIds = if (checked) categoryIds + category.id else categoryIds - category.id
                                    },
                                )
                            }
                        }
                    }
                }
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
                        first = { field -> NumberField(
                            weightIncrement,
                            { weightIncrement = it },
                            "Weight increment (${unitSymbol(weightUnit)})",
                            modifier = field.testTag("exercise-weight-increment"),
                            isError = validationRequested && weightIncrementError != null,
                            supportingText = weightIncrementError.takeIf { validationRequested },
                        ) },
                        second = { field -> NumberField(
                            repetitionIncrement,
                            { repetitionIncrement = it },
                            "Rep increment",
                            integer = true,
                            modifier = field,
                            isError = validationRequested && repetitionIncrementError != null,
                            supportingText = repetitionIncrementError.takeIf { validationRequested },
                        ) },
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
                            isError = validationRequested && restSecondsError != null,
                            supportingText = restSecondsError.takeIf { validationRequested }?.let { message -> { Text(message) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item { GymEnumDropdown("Default graph", GymGraphMetric.entries, GymGraphMetric.entries.firstOrNull { it.name == graphMetric } ?: GymGraphMetric.EstimatedOneRepMax, { it.label }) { graphMetric = it.name } }
                    item { GymEnumDropdown("Estimated 1RM formula", EstimatedOneRepMaxFormula.entries, formula, { it.name }) { formula = it } }
                    item { ResponsiveFieldPair(
                        first = { field -> NumberField(
                            barWeight,
                            { barWeight = it },
                            "Bar weight (${unitSymbol(weightUnit)})",
                            modifier = field.testTag("exercise-bar-weight"),
                            isError = validationRequested && barWeightError != null,
                            supportingText = barWeightError.takeIf { validationRequested },
                        ) },
                        second = { field -> OutlinedTextField(
                            plates,
                            { plates = it },
                            label = { Text("Plates (${unitSymbol(weightUnit)})") },
                            isError = validationRequested && platesError != null,
                            supportingText = platesError.takeIf { validationRequested }?.let { message -> { Text(message) } },
                            modifier = field.testTag("exercise-plates"),
                        ) },
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
                enabled = !saving,
                onClick = {
                    validationRequested = true
                    if (validationErrors.isNotEmpty()) {
                        showAdvanced = true
                        return@WhipTextButton
                    }
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
                            availablePlatesKg = parsedPlateValues.map { massToKilograms(it, weightUnit) },
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
        workSection = set.workSectionSnapshot,
        optionalWorkKind = set.optionalWorkKindSnapshot,
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
        primary = true,
        paneTitle = "Edit Set",
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
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    integer: Boolean = false,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = isError,
        supportingText = supportingText?.let { message -> { Text(message) } },
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
    title: String = "Add Exercise",
    supportingText: String? = null,
    onDismiss: () -> Unit,
    onPick: (Exercise) -> Unit,
    onCreate: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(
                modifier = Modifier.testTag("workout-exercise-picker-list"),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                supportingText?.let { message ->
                    item {
                        Text(
                            message,
                            modifier = Modifier.testTag("workout-exercise-picker-scope"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
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
    trackedInProgress: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onFavorite: () -> Unit,
    onDuplicate: () -> Unit,
    onConfigureTrackedRecords: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    var section by rememberSaveable(exercise.id) {
        mutableStateOf(if (exercise.archived) ExerciseDetailSection.More else ExerciseDetailSection.Overview)
    }
    EntityInspector(
        entityType = "Exercise",
        title = exercise.name,
        emoji = "🏋️",
        context = exercise.trackingType.label,
        status = when {
            exercise.archived -> "Archived"
            exercise.favorite && trackedInProgress -> "Favorite · Tracked"
            exercise.favorite -> "Favorite"
            trackedInProgress -> "Tracked"
            else -> "Available"
        },
        statusTone = if (exercise.archived) WhipStatusTone.Neutral else WhipStatusTone.Info,
        sections = ExerciseDetailSection.entries.map { it.inspectorSection },
        selectedSectionId = section.id,
        onSelectSection = { id -> section = ExerciseDetailSection.entries.first { it.id == id } },
        onDismiss = onDismiss,
        onEdit = onEdit,
        editLabel = "Edit Exercise",
        modifier = modifier,
        legacySurfaceTag = "exercise-detail-surface",
        legacySectionTagPrefix = "exercise-detail-section",
        primaryAction = EntityInspectorPrimaryAction(
            id = "restore",
            label = "Restore Exercise",
            onClick = onArchive,
        ).takeIf { exercise.archived },
        content = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (section) {
                    ExerciseDetailSection.Overview -> {
                        EntityInspectorGroup("Training setup") {
                            EntityInspectorFact("Tracks", exercise.trackingType.label)
                            exercise.equipment.takeIf(String::isNotBlank)?.let { EntityInspectorFact("Equipment", it) }
                            exercise.primaryMuscles.takeIf(String::isNotBlank)?.let { EntityInspectorFact("Primary muscles", it) }
                            exercise.secondaryMuscles.takeIf(String::isNotBlank)?.let { EntityInspectorFact("Also trains", it) }
                            exercise.notes.takeIf(String::isNotBlank)?.let { EntityInspectorFact("Notes", it) }
                        }
                        EntityInspectorGroup("Defaults") {
                            if (exercise.trackingType.name.contains("Weight")) {
                                val unit = BuiltInUnits.get(exercise.weightUnitId)?.symbol.orEmpty()
                                EntityInspectorFact(
                                    "Weight increment",
                                    "${exercise.weightIncrement} $unit".trim(),
                                )
                            }
                            if (exercise.trackingType.name.contains("Reps")) {
                                EntityInspectorFact("Repetition increment", exercise.repetitionIncrement.toString())
                            }
                            exercise.defaultRestSeconds?.let { seconds ->
                                EntityInspectorFact(
                                    "Rest timer",
                                    if (seconds % 60 == 0) "${seconds / 60} min" else "${seconds / 60} min ${seconds % 60} sec",
                                )
                            }
                            EntityInspectorFact("Load meaning", exercise.loadInterpretation.label)
                        }
                    }
                    ExerciseDetailSection.More -> {
                        EntityInspectorGroup("Actions") {
                            EntityInspectorAction("favorite", if (exercise.favorite) "Remove from Favorites" else "Add to Favorites", onFavorite)
                            EntityInspectorAction(
                                "tracked-records",
                                if (trackedInProgress) "Edit Progress Records" else "Add to Tracked Records",
                                onConfigureTrackedRecords,
                            )
                            EntityInspectorAction("duplicate", "Duplicate", onDuplicate)
                        }
                        if (!exercise.archived) EntityInspectorGroup("Availability") {
                            EntityInspectorAction("archive", "Archive Exercise", onArchive)
                        }
                        EntityInspectorDangerZone {
                            EntityInspectorAction(
                                id = "delete",
                                label = "Delete Permanently",
                                onClick = onDelete,
                                modifier = Modifier.testTag("entity-inspector-delete"),
                                danger = true,
                            )
                        }
                    }
                }
            }
        },
    )
}

private enum class ExerciseDetailSection(val id: String, val label: String) {
    Overview("overview", "Overview"),
    More("options", "Options"),
    ;

    val inspectorSection: EntityInspectorSection
        get() = EntityInspectorSection(
            id = id,
            label = label,
        )
}

@Composable
internal fun WorkoutEditorDialog(
    modifier: Modifier = Modifier,
    session: WorkoutSession?,
    initialDate: LocalDate,
    initialKeepAwake: Boolean = false,
    onDismiss: () -> Unit,
    onStart: (String, String, LocalDate, Boolean, (WhipResult<Unit>) -> Unit) -> Unit,
) {
    val editorKey = "workout-${session?.id ?: "new"}"
    var name by rememberSaveable(editorKey) { mutableStateOf(session?.name.orEmpty()) }
    var notes by rememberSaveable(editorKey) { mutableStateOf(session?.notes.orEmpty()) }
    var date by rememberSaveable(editorKey) { mutableStateOf(initialDate) }
    var keepAwake by rememberSaveable(editorKey) { mutableStateOf(session?.keepScreenAwake ?: initialKeepAwake) }
    var showDatePicker by rememberSaveable(editorKey) { mutableStateOf(false) }
    var showDiscardConfirmation by rememberSaveable(editorKey) { mutableStateOf(false) }
    var saving by rememberSaveable(editorKey) { mutableStateOf(false) }
    var saveError by rememberSaveable(editorKey) { mutableStateOf<String?>(null) }
    val fallbackSaveError = stringResource(R.string.gym_workout_save_failed)
    val editorFingerprint = listOf(name, notes, date, keepAwake).joinToString("\u001f")
    val initialFingerprint by rememberSaveable(editorKey) { mutableStateOf(editorFingerprint) }
    val requestDismiss = {
        if (!saving) {
            if (editorFingerprint != initialFingerprint) showDiscardConfirmation = true else onDismiss()
        }
    }
    BackHandler(enabled = !showDiscardConfirmation && !saving, onBack = requestDismiss)
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = requestDismiss,
        title = { Text(if (session == null) "Start Workout" else "Workout Details") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    name,
                    { name = it },
                    label = { Text("Optional name") },
                    modifier = Modifier.fillMaxWidth().testTag("workout-editor-name"),
                )
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                if (session == null) {
                    WhipOutlinedButton(onClick = { showDatePicker = true }) {
                        Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
                    }
                }
                ToggleRow("Keep screen awake on workout screen", keepAwake) { keepAwake = it }
                saveError?.let { message ->
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("workout-editor-save-error"),
                    )
                }
            }
        },
        confirmButton = {
            WhipTextButton(
                enabled = !saving,
                onClick = {
                    if (!saving) {
                        saving = true
                        saveError = null
                        onStart(name, notes, date, keepAwake) { result ->
                            saving = false
                            when (result) {
                                is WhipResult.Success -> onDismiss()
                                is WhipResult.Failure -> saveError = result.message.ifBlank { fallbackSaveError }
                            }
                        }
                    }
                },
                modifier = Modifier.testTag("workout-editor-confirm"),
            ) {
                Text(if (saving) stringResource(R.string.gym_workout_saving) else if (session == null) "Start" else "Save")
            }
        },
        dismissButton = { WhipTextButton(onClick = requestDismiss, enabled = !saving) { Text("Cancel") } },
    )
    if (showDatePicker) {
        WhipDatePickerDialog(
            initialDate = date,
            onDismiss = { showDatePicker = false },
            onDateSelected = { date = it; showDatePicker = false },
        )
    }
    if (showDiscardConfirmation) {
        UnsavedChangesDialog(
            subject = stringResource(R.string.gym_workout_discard_subject),
            onKeepEditing = { showDiscardConfirmation = false },
            onDiscard = {
                showDiscardConfirmation = false
                onDismiss()
            },
            modifier = modifier,
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    WhipSettingsRow(
        title = label.uiTitleCase(),
        checked = checked,
        onCheckedChange = onCheckedChange,
    )
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
internal fun WorkoutGroupDialog(
    modifier: Modifier = Modifier,
    exercises: List<WorkoutExerciseUi>,
    onDismiss: () -> Unit,
    onCreate: (String, WorkoutGroupType, List<Long>, (WhipResult<Unit>) -> Unit) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(WorkoutGroupType.Superset) }
    var selectedIds by rememberSaveable {
        mutableStateOf<Set<Long>>(exercises.take(2).mapTo(linkedSetOf()) { it.workoutExercise.id })
    }
    var showDiscardConfirmation by rememberSaveable { mutableStateOf(false) }
    var saving by rememberSaveable { mutableStateOf(false) }
    var saveError by rememberSaveable { mutableStateOf<String?>(null) }
    val fallbackSaveError = stringResource(R.string.gym_workout_group_save_failed)
    val editorFingerprint = listOf(name, type, selectedIds.sorted()).joinToString("\u001f")
    val initialFingerprint by rememberSaveable { mutableStateOf(editorFingerprint) }
    val requestDismiss = {
        if (!saving) {
            if (editorFingerprint != initialFingerprint) showDiscardConfirmation = true else onDismiss()
        }
    }
    BackHandler(enabled = !showDiscardConfirmation && !saving, onBack = requestDismiss)
    ProductivityEditorDialog(
        modifier = modifier,
        testTag = "workout-group-editor",
        paneTitle = "Group Workout Exercises",
        onDismissRequest = requestDismiss,
        title = { Text("Group Workout Exercises") },
        text = {
            WhipChoiceList(Modifier.testTag("workout-group-choice-list")) {
                item {
                    OutlinedTextField(
                        name,
                        { name = it.replace('\n', ' ').replace('\r', ' ').take(80) },
                        label = { Text("Group name (optional)") },
                        supportingText = { Text("Use a name only when it helps distinguish this group.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("workout-group-name"),
                    )
                }
                saveError?.let { message ->
                    item {
                        Text(
                            message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("workout-group-save-error"),
                        )
                    }
                }
                item { Text(stringResource(R.string.gym_workout_group_type), style = MaterialTheme.typography.labelLarge) }
                items(WorkoutGroupType.entries, key = WorkoutGroupType::name) { option ->
                    WhipSingleChoiceRow(
                        label = option.name,
                        selected = type == option,
                        onSelect = { type = option },
                        accessibilityLabel = stringResource(R.string.gym_workout_group_type_accessibility, option.name),
                    )
                }
                item { Text("Choose two or more exercises. Other exercises remain independent.") }
                items(exercises, key = { it.workoutExercise.id }) { item ->
                    val id = item.workoutExercise.id
                    WhipMultiChoiceRow(
                        label = item.exercise.name,
                        checked = id in selectedIds,
                        onCheckedChange = {
                            selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                        },
                        accessibilityLabel = stringResource(R.string.gym_workout_group_include_accessibility, item.exercise.name),
                    )
                }
            }
        },
        confirmButton = {
            WhipTextButton(
                enabled = selectedIds.size >= 2 && !saving,
                onClick = {
                    if (!saving) {
                        saving = true
                        saveError = null
                        onCreate(name.trim().ifBlank { type.name }, type, selectedIds.toList()) { result ->
                            saving = false
                            when (result) {
                                is WhipResult.Success -> onDismiss()
                                is WhipResult.Failure -> saveError = result.message.ifBlank { fallbackSaveError }
                            }
                        }
                    }
                },
                modifier = Modifier.testTag("workout-group-confirm"),
            ) { Text(if (saving) stringResource(R.string.gym_workout_saving) else "Create") }
        },
        dismissButton = { WhipTextButton(onClick = requestDismiss, enabled = !saving) { Text("Cancel") } },
    )
    if (showDiscardConfirmation) {
        UnsavedChangesDialog(
            subject = stringResource(R.string.gym_workout_group_discard_subject),
            onKeepEditing = { showDiscardConfirmation = false },
            onDiscard = {
                showDiscardConfirmation = false
                onDismiss()
            },
            modifier = modifier,
        )
    }
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
            prescribedRepetitionsMax,
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
    prescribedRepetitions?.let { minimum ->
        parts += when {
            classification == WorkoutSetClassification.Amrap -> "$minimum+ reps"
            prescribedRepetitionsMax != null && prescribedRepetitionsMax != minimum -> "$minimum–$prescribedRepetitionsMax reps"
            else -> "$minimum reps"
        }
    } ?: prescribedRepetitionsMax?.let { maximum -> parts += "Up to $maximum reps" }
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

private fun PersonalRecord.displayText(weightUnitId: String, distanceUnitId: String, precision: Int): String = when (type) {
    PersonalRecordType.MaxDuration -> formatDuration(value.toLong())
    PersonalRecordType.MinPace -> "${formatDuration(value.toLong())}/km"
    else -> {
        val unit = displayUnit(weightUnitId, distanceUnitId)
        "${formatNumber(displayValue(weightUnitId, distanceUnitId), precision)}${unit.takeIf(String::isNotBlank)?.let { " $it" }.orEmpty()}"
    }
}

private fun PersonalRecord.contextLabel(
    weightUnitId: String,
    precision: Int,
    sourceFormula: EstimatedOneRepMaxFormula?,
): String? = when (type) {
    PersonalRecordType.MaxRepetitionsForWeight -> secondaryValue?.let { weightKg ->
        "At ${formatNumber(massFromKilograms(weightKg, weightUnitId), precision)} ${unitSymbol(weightUnitId)}"
    }
    PersonalRecordType.BestWeightForRepCount -> secondaryValue?.let { reps ->
        "For ${formatNumber(reps, 0)} reps"
    }
    PersonalRecordType.EstimatedOneRepMax -> sourceFormula?.let { "${it.name} formula" }
    else -> null
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

internal fun quantityLabel(count: Int, singular: String, plural: String = defaultPlural(singular)): String =
    "$count ${if (count == 1) singular else plural}"

internal fun defaultPlural(singular: String): String {
    val lower = singular.lowercase()
    return when {
        lower.endsWith("y") && lower.length > 1 && lower[lower.lastIndex - 1] !in "aeiou" -> singular.dropLast(1) + "ies"
        lower.endsWith("s") || lower.endsWith("x") || lower.endsWith("z") || lower.endsWith("ch") || lower.endsWith("sh") -> singular + "es"
        else -> singular + "s"
    }
}

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
