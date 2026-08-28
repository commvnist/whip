package com.whip.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.whip.app.core.OperationStatus
import com.whip.app.domain.Area
import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.CustomIdentityEmoji
import com.whip.app.domain.TrackChoiceOption
import com.whip.app.domain.DEFAULT_TRACK_EMOJI
import com.whip.app.domain.TrackChoiceOptionDraft
import com.whip.app.domain.TrackCondition
import com.whip.app.domain.TrackConditionMode
import com.whip.app.domain.TrackConditionOperator
import com.whip.app.domain.TrackCsvImportPreview
import com.whip.app.domain.TrackCsvMapping
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackEntryPage
import com.whip.app.domain.TrackEntryProjection
import com.whip.app.domain.TrackField
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackProjection
import com.whip.app.domain.TrackValueDraft
import com.whip.app.domain.TrackAggregation
import com.whip.app.domain.GoalType
import com.whip.app.domain.Goal
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalConsistencyPeriod
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.LinkRule
import com.whip.app.domain.LinkRuleDraft
import com.whip.app.domain.LinkSourceMetric
import com.whip.app.domain.LinkValueMode
import com.whip.app.domain.TriggerAction
import com.whip.app.domain.TriggerFieldMapping
import com.whip.app.domain.TriggerOccurrence
import com.whip.app.domain.TriggerOutcome
import com.whip.app.domain.TriggerRule
import com.whip.app.domain.TriggerRuleDraft
import com.whip.app.domain.TriggerSourceProperty
import com.whip.app.domain.TriggerTargetType
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.aggregate
import com.whip.app.domain.editableNumericValue
import com.whip.app.domain.matchingEntries
import com.whip.app.domain.toWhipDoubleOrNull
import com.whip.app.domain.previewTrackCsvImport
import com.whip.app.domain.trackCsvHeaders
import com.whip.app.domain.formatTrackScaleValue
import com.whip.app.domain.normalizeTrackScaleValue
import com.whip.app.domain.snapTrackScaleValue
import com.whip.app.domain.trackScaleValues
import com.whip.app.domain.TRACK_ENTRY_DATE_CONDITION_UUID
import com.whip.app.domain.validated
import java.time.LocalDate
import java.time.YearMonth
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.io.Serializable
import java.util.Locale
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

internal enum class TrackDetailDestination(val label: String) {
    Entries("Entries"),
    Automations("Automations"),
    Options("Options"),
    Insights("Insights"),
}

/** Peer destinations for the Tracks workspace; Track detail navigation remains subordinate. */
internal enum class TrackWorkspaceDestination(val label: String) {
    Tracks("Tracks"),
    Activity("Activity"),
    Insights("Insights"),
}

internal enum class TrackSort(val label: String) {
    EntryDate("Entry Date"),
    Identity("Entry Identity"),
    Created("Created"),
}

private data class TrackEntrySortChoice(
    val label: String,
    val sort: TrackSort? = null,
    val fieldId: Long? = null,
)

private enum class TrackGoalHistory(val label: String) {
    NewEntriesOnly("New Entries Only"),
    SinceGoalStart("Include Since Goal Start"),
    SinceDate("Include Since a Chosen Date"),
    AllHistory("Include All Track History"),
}

private data class TrackConditionSubject(val trackField: TrackField?) {
    val uuid: String get() = trackField?.uuid ?: TRACK_ENTRY_DATE_CONDITION_UUID
    val name: String get() = trackField?.name ?: "Entry Date"
    val type: TrackFieldType get() = trackField?.type ?: TrackFieldType.Date
}

internal sealed interface TrackEditorIntent : Serializable {
    data class Definition(val trackId: Long?) : TrackEditorIntent
    data class Entry(
        val trackId: Long,
        val entryId: Long? = null,
        val promptOccurrenceId: Long? = null,
        val prefill: TrackEntryDraft? = null,
    ) : TrackEditorIntent
}

internal sealed interface TrackEditorRoute : Serializable {
    val sessionId: Long

    data class Definition(
        val trackId: Long?,
        override val sessionId: Long,
    ) : TrackEditorRoute

    data class Entry(
        val trackId: Long,
        val entryId: Long? = null,
        val promptOccurrenceId: Long? = null,
        val prefill: TrackEntryDraft? = null,
        override val sessionId: Long,
    ) : TrackEditorRoute
}

@Composable
internal fun TrackAreaContent(
    state: TrackUiState,
    viewModel: TrackViewModel,
    innerPadding: PaddingValues,
    areas: List<Area>,
    customUnits: List<UnitDefinition>,
    defaultAreaId: String?,
    createRequested: Boolean,
    onCreateRequestConsumed: () -> Unit,
    openTrackIdRequest: Long?,
    onOpenTrackRequestConsumed: () -> Unit,
    editTrackIdRequest: Long? = null,
    onEditTrackRequestConsumed: () -> Unit = {},
    openEntryIdRequest: Long?,
    onOpenEntryRequestConsumed: () -> Unit,
    addEntryTrackIdRequest: Long? = null,
    onAddEntryTrackRequestConsumed: () -> Unit = {},
    openPromptOccurrenceIdRequest: Long? = null,
    onOpenPromptOccurrenceRequestConsumed: () -> Unit = {},
    operationStatus: OperationStatus,
    editorOpen: Boolean = false,
    onEditorRequest: (TrackEditorIntent) -> Unit = {},
    reviewAutomationsTrackIdRequest: Long? = null,
    onReviewAutomationsRequestConsumed: () -> Unit = {},
    onCreateArea: (String, Long?, (Result<String>) -> Unit) -> Unit,
    onCreateCustomUnit: CreateCustomUnitAction,
    onSetGoal: (Long) -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    selectedTrackState: MutableState<Long?>? = null,
    workspaceDestinationState: MutableState<TrackWorkspaceDestination>? = null,
    destinationState: MutableState<TrackDetailDestination>? = null,
    dialogModifier: Modifier = Modifier,
    reorderEnabled: Boolean = true,
    onShowAllAreasForReorder: () -> Unit = {},
    onReorderModeChange: (Boolean) -> Unit = {},
    reorderDismissRequest: Int = 0,
) {
    val localSelectedTrackState = rememberSaveable { mutableStateOf<Long?>(null) }
    val activeSelectedTrackState = selectedTrackState ?: localSelectedTrackState
    var selectedTrackId by activeSelectedTrackState
    val localWorkspaceDestinationState = rememberSaveable { mutableStateOf(TrackWorkspaceDestination.Tracks) }
    val activeWorkspaceDestinationState = workspaceDestinationState ?: localWorkspaceDestinationState
    var workspaceDestination by activeWorkspaceDestinationState
    var query by rememberSaveable { mutableStateOf("") }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    val localDestinationState = rememberSaveable { mutableStateOf(TrackDetailDestination.Entries) }
    val activeDestinationState = destinationState ?: localDestinationState
    var destination by activeDestinationState
    var deleteTrackId by rememberSaveable { mutableStateOf<Long?>(null) }
    var setGoalTrackId by rememberSaveable { mutableStateOf<Long?>(null) }
    var connectGoalTrackId by rememberSaveable { mutableStateOf<Long?>(null) }
    var importTrackId by rememberSaveable { mutableStateOf<Long?>(null) }
    var importCsvText by remember { mutableStateOf<String?>(null) }
    var exportTrackId by rememberSaveable { mutableStateOf<Long?>(null) }
    var fileError by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = selectedTrackId?.let(state::track)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) {
            importTrackId = null
        } else {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Could not read the selected CSV file")
            }.onSuccess { importCsvText = it }.onFailure {
                importTrackId = null
                fileError = it.message ?: "Could not read the selected CSV file"
            }
        }
    }
    val csvExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val trackId = exportTrackId
        if (uri == null || trackId == null) {
            exportTrackId = null
        } else {
            coroutineScope.launch {
                runCatching {
                    val csv = viewModel.exportCsv(trackId)
                    context.contentResolver.openOutputStream(uri, "w")?.bufferedWriter()?.use { it.write(csv) }
                        ?: error("Could not open the selected file")
                }.onFailure { fileError = it.message ?: "Could not export this Track" }
                exportTrackId = null
            }
        }
    }

    LaunchedEffect(createRequested) {
        if (createRequested) {
            workspaceDestination = TrackWorkspaceDestination.Tracks
            onEditorRequest(TrackEditorIntent.Definition(null))
            onCreateRequestConsumed()
        }
    }
    LaunchedEffect(openTrackIdRequest) {
        openTrackIdRequest?.let {
            workspaceDestination = TrackWorkspaceDestination.Tracks
            selectedTrackId = it
            destination = TrackDetailDestination.Entries
        }
        if (openTrackIdRequest != null) onOpenTrackRequestConsumed()
    }
    LaunchedEffect(editTrackIdRequest, state.projections) {
        val trackId = editTrackIdRequest ?: return@LaunchedEffect
        if (state.track(trackId) != null) {
            workspaceDestination = TrackWorkspaceDestination.Tracks
            onEditorRequest(TrackEditorIntent.Definition(trackId))
            onEditTrackRequestConsumed()
        }
    }
    LaunchedEffect(openEntryIdRequest, state.projections) {
        val entryId = openEntryIdRequest ?: return@LaunchedEffect
        state.projections.firstOrNull { projection -> projection.entries.any { it.entry.id == entryId } }?.let {
            workspaceDestination = TrackWorkspaceDestination.Tracks
            selectedTrackId = it.track.id
            onEditorRequest(TrackEditorIntent.Entry(it.track.id, entryId))
        }
        onOpenEntryRequestConsumed()
    }
    LaunchedEffect(addEntryTrackIdRequest, state.projections) {
        val trackId = addEntryTrackIdRequest ?: return@LaunchedEffect
        if (state.track(trackId) != null) {
            workspaceDestination = TrackWorkspaceDestination.Tracks
            selectedTrackId = trackId
            onEditorRequest(TrackEditorIntent.Entry(trackId))
            onAddEntryTrackRequestConsumed()
        }
    }
    LaunchedEffect(openPromptOccurrenceIdRequest, state.triggerOccurrences, state.projections) {
        val occurrenceId = openPromptOccurrenceIdRequest ?: return@LaunchedEffect
        val occurrence = state.triggerOccurrences.firstOrNull { it.id == occurrenceId } ?: return@LaunchedEffect
        val rule = state.triggerRules.firstOrNull { it.id == occurrence.triggerRuleId } ?: return@LaunchedEffect
        val target = state.track(rule.targetEntityId) ?: return@LaunchedEffect
        workspaceDestination = TrackWorkspaceDestination.Tracks
        selectedTrackId = target.track.id
        viewModel.loadPromptDraft(occurrenceId) { loaded ->
            onEditorRequest(TrackEditorIntent.Entry(target.track.id, promptOccurrenceId = occurrenceId, prefill = loaded))
        }
        onOpenPromptOccurrenceRequestConsumed()
    }
    LaunchedEffect(reviewAutomationsTrackIdRequest, state.projections) {
        val trackId = reviewAutomationsTrackIdRequest ?: return@LaunchedEffect
        if (state.track(trackId) != null) {
            workspaceDestination = TrackWorkspaceDestination.Tracks
            selectedTrackId = trackId
            destination = TrackDetailDestination.Automations
            onReviewAutomationsRequestConsumed()
        }
    }
    BackHandler(enabled = selected != null && !editorOpen) { selectedTrackId = null }

    @Composable fun trackList(masterPane: Boolean) {
        AllTracksPage(
            state = state,
            innerPadding = PaddingValues(),
            query = query,
            onQueryChange = { query = it },
            showArchived = showArchived,
            onShowArchivedChange = { showArchived = it },
            onOpen = { selectedTrackId = it },
            onEdit = { onEditorRequest(TrackEditorIntent.Definition(it)) },
            onAddEntry = { selectedTrackId = it; onEditorRequest(TrackEditorIntent.Entry(it)) },
            onCreate = { onEditorRequest(TrackEditorIntent.Definition(null)) },
            onReorder = viewModel::reorder,
            reorderEnabled = reorderEnabled,
            onShowAllAreasForReorder = onShowAllAreasForReorder,
            onSetPinned = viewModel::setPinned,
            onSetArchived = viewModel::setArchived,
            onOpenPrompt = { occurrenceId ->
                val occurrence = state.triggerOccurrences.firstOrNull { it.id == occurrenceId }
                val rule = occurrence?.let { item -> state.triggerRules.firstOrNull { it.id == item.triggerRuleId } }
                if (rule != null) selectedTrackId = rule.targetEntityId
                viewModel.loadPromptDraft(occurrenceId) { loaded ->
                    if (rule != null) onEditorRequest(TrackEditorIntent.Entry(rule.targetEntityId, promptOccurrenceId = occurrenceId, prefill = loaded))
                }
            },
            onRemindPrompt = { viewModel.remindPrompt(it, Instant.now().plus(1, ChronoUnit.HOURS)) },
            onDismissPrompt = viewModel::dismissPrompt,
            masterPane = masterPane,
            onReorderModeChange = onReorderModeChange,
            reorderDismissRequest = reorderDismissRequest,
        )
    }
    @Composable fun trackDetail(projection: TrackProjection) {
        TrackDetailPage(
            projection = projection,
            innerPadding = PaddingValues(),
            destination = destination,
            onDestinationChange = { destination = it },
            onBack = { selectedTrackId = null },
            onEditTrack = { onEditorRequest(TrackEditorIntent.Definition(projection.track.id)) },
            onAddEntry = { onEditorRequest(TrackEditorIntent.Entry(projection.track.id)) },
            onEditEntry = { onEditorRequest(TrackEditorIntent.Entry(projection.track.id, it)) },
            onDeleteEntry = viewModel::deleteEntry,
            onSetPinned = { viewModel.setPinned(projection.track.id, it) },
            onSetArchived = { viewModel.setArchived(projection.track.id, it) },
            onDuplicate = { viewModel.duplicate(projection.track.id) },
            onDeleteTrack = { deleteTrackId = projection.track.id },
            onSetGoal = { setGoalTrackId = projection.track.id; onSetGoal(projection.track.id) },
            onConnectGoal = { connectGoalTrackId = projection.track.id },
            state = state,
            today = state.currentDate,
            viewModel = viewModel,
            customUnits = customUnits,
            onOpenPrompt = { occurrenceId ->
                viewModel.loadPromptDraft(occurrenceId) { loaded ->
                    onEditorRequest(TrackEditorIntent.Entry(projection.track.id, promptOccurrenceId = occurrenceId, prefill = loaded))
                }
            },
            onRequestNotificationPermission = onRequestNotificationPermission,
            saving = operationStatus is OperationStatus.Running,
            dialogModifier = dialogModifier,
            onImport = {
                importTrackId = projection.track.id
                csvLauncher.launch("text/*")
            },
            onExport = {
                exportTrackId = projection.track.id
                val safeName = projection.track.name.replace(Regex("[^A-Za-z0-9._ -]"), "_").trim().ifBlank { "Whip Track" }
                csvExportLauncher.launch("$safeName.csv")
            },
        )
    }
    Column(Modifier.fillMaxSize().padding(innerPadding)) {
        DestinationTabBar(
            selected = workspaceDestination,
            destinations = TrackWorkspaceDestination.entries,
            onSelect = { selectedDestination ->
                workspaceDestination = selectedDestination
                if (selectedDestination != TrackWorkspaceDestination.Tracks) selectedTrackId = null
            },
            label = TrackWorkspaceDestination::label,
            testTagPrefix = "track-workspace-destination",
            barTestTag = "track-workspace-navigation",
        )
        BoxWithConstraints(Modifier.fillMaxSize().weight(1f)) {
            when (workspaceDestination) {
                TrackWorkspaceDestination.Tracks -> if (maxWidth >= 760.dp) {
                    Row(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(0.38f).fillMaxHeight()) { trackList(masterPane = true) }
                        VerticalDivider(Modifier.fillMaxHeight())
                        Box(Modifier.weight(0.62f).fillMaxHeight()) {
                            selected?.let { trackDetail(it) } ?: Box(
                                Modifier.fillMaxSize().padding(24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                WhipEmptyState("Choose a Track", "Select a Track to review Entries, Automations, Insights, and Options.")
                            }
                        }
                    }
                } else if (selected == null) {
                    trackList(masterPane = false)
                } else {
                    trackDetail(selected)
                }
                TrackWorkspaceDestination.Activity -> TrackActivityPage(
                    state = state,
                    areas = areas,
                    customUnits = customUnits,
                    onOpenTrack = { id ->
                        workspaceDestination = TrackWorkspaceDestination.Tracks
                        selectedTrackId = id
                        destination = TrackDetailDestination.Entries
                    },
                    onEditEntry = { trackId, entryId ->
                        onEditorRequest(TrackEditorIntent.Entry(trackId, entryId))
                    },
                    onDeleteEntry = viewModel::deleteEntry,
                    dialogModifier = dialogModifier,
                )
                TrackWorkspaceDestination.Insights -> TrackWorkspaceInsightsPage(
                    state = state,
                    customUnits = customUnits,
                    onOpenTrack = { id ->
                        workspaceDestination = TrackWorkspaceDestination.Tracks
                        selectedTrackId = id
                        destination = TrackDetailDestination.Insights
                    },
                )
            }
        }
    }

    deleteTrackId?.let { id ->
        val track = state.track(id)
        if (track != null) PaneAwareAlertDialog(
            modifier = dialogModifier,
            onDismissRequest = { deleteTrackId = null },
            title = { Text("Delete ${track.track.name} Permanently?") },
            text = {
                val progress = state.linkRules.filter { it.sourceType == LinkSourceType.Track && it.sourceEntityId == id }
                val triggers = state.triggerRules.filter {
                    it.sourceType == LinkSourceType.Track && it.sourceEntityId == id ||
                        it.targetType == TriggerTargetType.Track && it.targetEntityId == id
                }
                val triggerIds = triggers.mapTo(mutableSetOf(), TriggerRule::id)
                val prompts = state.triggerOccurrences.count { it.triggerRuleId in triggerIds && it.fulfilledEntryId == null }
                val contributions = state.contributions.count { contribution -> progress.any { it.id == contribution.linkRuleId } }
                val goalNames = progress.mapNotNull { rule -> state.goals.firstOrNull { it.id == rule.targetGoalId }?.name }.distinct()
                Text(buildString {
                    append("This permanently deletes ${quantityLabel(track.entries.size, "Entry")}, ${quantityLabel(track.fields.size, "Field")}, ${quantityLabel(track.options.size, "Choice option")}, ${quantityLabel(progress.size, "Goal Automation")}, ${quantityLabel(contributions, "Goal contribution")}, ${quantityLabel(triggers.size, "Next-Action Automation")}, and ${quantityLabel(prompts, "pending prompt")}.")
                    if (goalNames.isNotEmpty()) append(" Affected Goals keep their definitions: ${goalNames.joinToString()}.")
                    append(" This cannot be undone.")
                })
            },
            confirmButton = { WhipTextButton(onClick = { viewModel.deleteTrack(id); deleteTrackId = null; selectedTrackId = null }) { Text("Delete Permanently", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { WhipTextButton(onClick = { deleteTrackId = null }) { Text("Cancel") } },
        )
    }
    setGoalTrackId?.let { id ->
        state.track(id)?.let { projection ->
            TrackGoalAutomationDialog(
                projection = projection,
                customUnits = customUnits,
                saving = operationStatus is OperationStatus.Running,
                onCreateCustomUnit = onCreateCustomUnit,
                today = state.currentDate,
                onDismiss = { setGoalTrackId = null },
                onSave = { draft ->
                    viewModel.createGoalFromTrack(id, draft) { setGoalTrackId = null }
                },
            )
        }
    }
    connectGoalTrackId?.let { id ->
        state.track(id)?.let { projection ->
            TrackExistingGoalAutomationDialog(
                projection = projection,
                goals = projection.compatibleAutomationGoals(state.goals),
                customUnits = customUnits,
                saving = operationStatus is OperationStatus.Running,
                today = state.currentDate,
                onDismiss = { connectGoalTrackId = null },
                onSave = { draft, includeHistory ->
                    viewModel.createProgressAutomation(draft, includeHistory) { connectGoalTrackId = null }
                },
            )
        }
    }
    val importProjection = importTrackId?.let(state::track)
    val csvText = importCsvText
    if (importProjection != null && csvText != null) TrackCsvImportDialog(
        projection = importProjection,
        csv = csvText,
        customUnits = customUnits,
        saving = operationStatus is OperationStatus.Running,
        today = state.currentDate,
        onDismiss = { importTrackId = null; importCsvText = null },
        onImport = { preview ->
            viewModel.importEntries(importProjection.track.id, preview.validDrafts) {
                importTrackId = null
                importCsvText = null
            }
        },
    )
    fileError?.let { message ->
        PaneAwareAlertDialog(
            modifier = dialogModifier,
            onDismissRequest = { fileError = null },
            title = { Text("CSV File Error") },
            text = { Text(message) },
            confirmButton = { WhipTextButton(onClick = { fileError = null }) { Text("OK") } },
        )
    }
}

private enum class TrackActivityDateRange(val label: String) {
    AnyDate("Any Date"),
    SevenDays("7 Days"),
    ThirtyDays("30 Days"),
    ThisYear("This Year");

    fun contains(date: LocalDate, today: LocalDate): Boolean = when (this) {
        AnyDate -> true
        SevenDays -> date in today.minusDays(6)..today
        ThirtyDays -> date in today.minusDays(29)..today
        ThisYear -> date.year == today.year && !date.isAfter(today)
    }
}

private data class TrackActivityItem(
    val projection: TrackProjection,
    val entry: TrackEntryProjection,
)

@Composable
private fun TrackActivityPage(
    state: TrackUiState,
    areas: List<Area>,
    customUnits: List<UnitDefinition>,
    onOpenTrack: (Long) -> Unit,
    onEditEntry: (Long, Long) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    dialogModifier: Modifier,
) {
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var filtersVisible by rememberSaveable { mutableStateOf(false) }
    var trackFilterId by rememberSaveable { mutableStateOf<Long?>(null) }
    var areaFilterId by rememberSaveable { mutableStateOf<String?>(null) }
    var dateRange by rememberSaveable { mutableStateOf(TrackActivityDateRange.AnyDate) }
    var viewedEntryId by rememberSaveable { mutableStateOf<Long?>(null) }
    val units = BuiltInUnits.all + customUnits
    val activeTracks = state.active
    val visibleAreaIds = activeTracks.map(TrackProjection::track).map { it.areaId }.toSet()
    val availableAreas = areas.filter { !it.archived && it.id in visibleAreaIds }
    val normalizedQuery = query.trim()
    val items = activeTracks.flatMap { projection ->
        projection.entries.map { TrackActivityItem(projection, it) }
    }.filter { item ->
        val projection = item.projection
        val searchable = buildList {
            add(projection.track.name)
            add(projection.track.area)
            add(projection.primaryText(item.entry))
            projection.fields.forEach { field -> add(projection.formattedValue(item.entry, field, units)) }
        }
        (trackFilterId == null || projection.track.id == trackFilterId) &&
            (areaFilterId == null || projection.track.areaId == areaFilterId) &&
            dateRange.contains(item.entry.entry.entryDate, state.currentDate) &&
            (normalizedQuery.isBlank() || searchable.any { it.contains(normalizedQuery, ignoreCase = true) })
    }.sortedWith(
        compareByDescending<TrackActivityItem> { it.entry.entry.entryDate }
            .thenByDescending { it.entry.entry.createdAtMillis },
    )
    val activeFilterCount = listOf(
        trackFilterId != null,
        areaFilterId != null,
        dateRange != TrackActivityDateRange.AnyDate,
    ).count { it }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 1040.dp).align(Alignment.TopCenter),
            contentPadding = WhipPageContentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                WhipPageHeader(
                    title = "Activity",
                    supportingText = "A chronological view of Entries across visible Tracks · ${quantityLabel(items.size, "Entry")}",
                ) {
                    WhipPageIconAction(
                        icon = Icons.Outlined.Search,
                        label = "Search Track Activity",
                        onClick = { searchVisible = !searchVisible; if (!searchVisible) query = "" },
                        active = searchVisible || query.isNotBlank(),
                    )
                    WhipPageIconAction(
                        icon = Icons.Outlined.FilterAlt,
                        label = "Filter Track Activity",
                        onClick = { filtersVisible = !filtersVisible },
                        badgeCount = activeFilterCount,
                        active = filtersVisible || activeFilterCount > 0,
                    )
                }
            }
            if (searchVisible) item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().testTag("track-activity-search"),
                    label = { Text("Search Activity") },
                    placeholder = { Text("Entry, Track, Area, or Field value") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = if (query.isNotEmpty()) {{
                        IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, contentDescription = "Clear Search") }
                    }} else null,
                )
            }
            if (filtersVisible) item {
                Card(Modifier.fillMaxWidth().testTag("track-activity-filters")) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Filters", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (activeFilterCount > 0) WhipTextButton(onClick = {
                                trackFilterId = null
                                areaFilterId = null
                                dateRange = TrackActivityDateRange.AnyDate
                            }) { Text("Clear") }
                        }
                        Text("Date", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            TrackActivityDateRange.entries.forEach { range ->
                                WhipFilterChip(
                                    selected = dateRange == range,
                                    onClick = { dateRange = range },
                                    label = { Text(range.label) },
                                )
                            }
                        }
                        SelectionField(
                            label = "Track",
                            values = listOf<Long?>(null) + activeTracks.map { it.track.id },
                            selected = trackFilterId,
                            valueText = { id ->
                                activeTracks.firstOrNull { it.track.id == id }?.track
                                    ?.let { "${it.icon} ${it.name}" }
                                    ?: "All Tracks"
                            },
                            onSelect = { trackFilterId = it },
                            modifier = Modifier.fillMaxWidth().testTag("track-activity-track-filter"),
                        )
                        SelectionField(
                            label = "Area",
                            values = listOf<String?>(null) + availableAreas.map { it.id },
                            selected = areaFilterId,
                            valueText = { id -> availableAreas.firstOrNull { it.id == id }?.name ?: "All Areas" },
                            onSelect = { areaFilterId = it },
                            modifier = Modifier.fillMaxWidth().testTag("track-activity-area-filter"),
                        )
                    }
                }
            }
            when {
                state.loading -> item { DomainLoadContent("Track Activity", PaddingValues()) }
                items.isEmpty() -> item {
                    WhipEmptyState(
                        title = if (activeFilterCount > 0 || query.isNotBlank()) "No Matching Activity" else "No Track Activity Yet",
                        supportingText = if (activeFilterCount > 0 || query.isNotBlank()) {
                            "Clear a filter or try a different search."
                        } else {
                            "Entries appear here after you add them to a Track."
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    )
                }
                else -> items(items, key = { "track-activity-${it.entry.entry.id}" }) { item ->
                    TrackActivityRow(
                        item = item,
                        customUnits = customUnits,
                        onOpen = { viewedEntryId = item.entry.entry.id },
                        onOpenTrack = { onOpenTrack(item.projection.track.id) },
                        onEdit = { onEditEntry(item.projection.track.id, item.entry.entry.id) },
                        onDelete = { onDeleteEntry(item.entry.entry.id) },
                    )
                }
            }
        }
    }
    viewedEntryId?.let { entryId ->
        activeTracks.firstNotNullOfOrNull { projection ->
            projection.entries.firstOrNull { it.entry.id == entryId }?.let { projection to it }
        }?.let { (projection, entry) ->
            TrackEntryDetailsDialog(
                modifier = dialogModifier,
                projection = projection,
                entry = entry,
                customUnits = customUnits,
                editable = true,
                onDismiss = { viewedEntryId = null },
                onEdit = { viewedEntryId = null; onEditEntry(projection.track.id, entryId) },
            )
        }
    }
}

@Composable
private fun TrackActivityRow(
    item: TrackActivityItem,
    customUnits: List<UnitDefinition>,
    onOpen: () -> Unit,
    onOpenTrack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var moreOpen by rememberSaveable(item.entry.entry.id) { mutableStateOf(false) }
    val projection = item.projection
    val entry = item.entry
    val supporting = projection.fields.filter(TrackField::showInList).take(2).mapNotNull { field ->
        projection.formattedValue(entry, field, BuiltInUnits.all + customUnits).takeIf(String::isNotBlank)?.let { "${field.name} $it" }
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(
            onClickLabel = "Open Entry ${projection.primaryText(entry)}",
            onClick = onOpen,
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            WhipIdentityEmoji(projection.track.icon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(projection.primaryText(entry), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(projection.track.name, projection.track.area, entry.entry.entryDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
                        .filter(String::isNotBlank).joinToString(" · "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (supporting.isNotEmpty()) Text(supporting.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            ItemEditButton("Entry", projection.primaryText(entry), onEdit)
            Box {
                IconButton(onClick = { moreOpen = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = "More Actions for ${projection.primaryText(entry)}") }
                DropdownMenu(moreOpen, { moreOpen = false }) {
                    WhipMenuItem(label = "Open Track", onClick = { moreOpen = false; onOpenTrack() })
                    HorizontalDivider()
                    WhipMenuItem(
                        label = "Delete Entry",
                        icon = Icons.Outlined.DeleteOutline,
                        role = WhipMenuItemRole.Destructive,
                        onClick = { moreOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}

private data class TrackNumericSummary(
    val projection: TrackProjection,
    val field: TrackField,
    val values: List<Double>,
    val unitLabel: String,
)

@Composable
private fun TrackWorkspaceInsightsPage(
    state: TrackUiState,
    customUnits: List<UnitDefinition>,
    onOpenTrack: (Long) -> Unit,
) {
    val activeTracks = state.active
    val activeTrackIds = activeTracks.mapTo(mutableSetOf()) { it.track.id }
    val totalEntries = activeTracks.sumOf { it.entries.size }
    val lastSevenDays = (6L downTo 0L).map { offset -> state.currentDate.minusDays(offset) }
    val recentTracks = activeTracks.mapNotNull { projection ->
        projection.entries.maxWithOrNull(compareBy<TrackEntryProjection> { it.entry.entryDate }.thenBy { it.entry.createdAtMillis })
            ?.let { projection to it }
    }.sortedWith(compareByDescending<Pair<TrackProjection, TrackEntryProjection>> { it.second.entry.entryDate }.thenByDescending { it.second.entry.createdAtMillis })
    val units = BuiltInUnits.all + customUnits
    val numericSummaries = activeTracks.flatMap { projection ->
        projection.fields.filter { it.type == TrackFieldType.Number || it.type == TrackFieldType.Scale }.mapNotNull { field ->
            val values = projection.entries.mapNotNull { entry ->
                entry.value(field.id)?.let { value ->
                    if (field.type == TrackFieldType.Number) value.canonicalNumber ?: value.enteredNumber else value.scaleValue
                }
            }
            values.takeIf(List<Double>::isNotEmpty)?.let {
                val unitLabel = field.unitId?.let { id -> units.firstOrNull { it.id == id }?.symbol }.orEmpty()
                TrackNumericSummary(projection, field, values, unitLabel)
            }
        }
    }
    val progressRules = state.linkRules.filter { it.sourceType == LinkSourceType.Track && it.sourceEntityId in activeTrackIds }
    val actionRules = state.triggerRules.filter {
        it.sourceType == LinkSourceType.Track && it.sourceEntityId in activeTrackIds ||
            it.targetType == TriggerTargetType.Track && it.targetEntityId in activeTrackIds
    }
    val actionRuleIds = actionRules.mapTo(mutableSetOf(), TriggerRule::id)
    val pendingPrompts = state.triggerOccurrences.count {
        it.triggerRuleId in actionRuleIds && it.dismissedAt == null && it.fulfilledEntryId == null
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 1040.dp).align(Alignment.TopCenter).testTag("track-workspace-insights-list"),
            contentPadding = WhipPageContentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { WhipPageHeader("Insights", "Patterns and automation health across visible Tracks.") }
            if (state.loading) item { DomainLoadContent("Track Insights", PaddingValues()) }
            else {
                item {
                    InsightCard(
                        "Overview",
                        listOf(
                            "Active Tracks" to activeTracks.size.toString(),
                            "Total Entries" to totalEntries.toString(),
                            "Entries in 7 Days" to activeTracks.sumOf { track -> track.entries.count { it.entry.entryDate in state.currentDate.minusDays(6)..state.currentDate } }.toString(),
                            "Entries in 30 Days" to activeTracks.sumOf { track -> track.entries.count { it.entry.entryDate in state.currentDate.minusDays(29)..state.currentDate } }.toString(),
                        ),
                    )
                }
                item {
                    InsightCard(
                        "Entry Frequency",
                        lastSevenDays.map { date ->
                            date.format(DateTimeFormatter.ofPattern("EEE, MMM d")) to
                                activeTracks.sumOf { track -> track.entries.count { it.entry.entryDate == date } }.toString()
                        },
                    )
                }
                item {
                    InsightCard(
                        "Automation Status",
                        listOf(
                            "Goal Automations" to "${progressRules.count(LinkRule::enabled)} of ${progressRules.size} enabled",
                            "Next-Action Automations" to "${actionRules.count(TriggerRule::enabled)} of ${actionRules.size} enabled",
                            "Pending Entries" to pendingPrompts.toString(),
                        ),
                    )
                }
                if (recentTracks.isNotEmpty()) {
                    item { Text("Recently Active Tracks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                    items(recentTracks.take(8), key = { "recent-track-${it.first.track.id}" }) { (projection, entry) ->
                        Card(Modifier.fillMaxWidth().clickable(onClickLabel = "Open ${projection.track.name} Insights") { onOpenTrack(projection.track.id) }) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                WhipIdentityEmoji(projection.track.icon)
                                Column(Modifier.weight(1f)) {
                                    Text(projection.track.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${projection.primaryText(entry)} · ${entry.entry.entryDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
                if (numericSummaries.isNotEmpty()) {
                    item { Text("Numeric Summaries", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                    items(numericSummaries, key = { "numeric-${it.projection.track.id}-${it.field.id}" }) { summary ->
                        val suffix = summary.unitLabel.takeIf(String::isNotBlank)?.let { " $it" }.orEmpty()
                        InsightCard(
                            "${summary.projection.track.icon} ${summary.projection.track.name} · ${summary.field.name}",
                            listOf(
                                "Entries" to summary.values.size.toString(),
                                "Total" to "${summary.values.sum().formatCompact()}$suffix",
                                "Average" to "${summary.values.average().formatCompact()}$suffix",
                            ),
                        )
                    }
                }
                if (activeTracks.isEmpty()) item {
                    WhipEmptyState(
                        "No Track Insights Yet",
                        "Create a Track and add Entries to see cross-Track patterns here.",
                        Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackGoalAutomationDialog(
    projection: TrackProjection,
    customUnits: List<UnitDefinition>,
    saving: Boolean,
    onCreateCustomUnit: CreateCustomUnitAction,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (TrackGoalAutomationDraft) -> Unit,
) {
    var aggregation by rememberSaveable(projection.track.id) { mutableStateOf(TrackAggregation.CountEntries) }
    var sourceFieldId by rememberSaveable(projection.track.id) { mutableStateOf<Long?>(null) }
    var conditionMode by rememberSaveable(projection.track.id) { mutableStateOf(TrackConditionMode.MatchAll) }
    var conditions by remember(projection.track.id) { mutableStateOf<List<TrackCondition>>(emptyList()) }
    var addingCondition by rememberSaveable(projection.track.id) { mutableStateOf(false) }
    var goalName by rememberSaveable(projection.track.id) { mutableStateOf("") }
    var goalType by rememberSaveable(projection.track.id) { mutableStateOf(GoalType.ReachValue) }
    var target by rememberSaveable(projection.track.id) { mutableStateOf("") }
    var targetMax by rememberSaveable(projection.track.id) { mutableStateOf("") }
    var fixedAmount by rememberSaveable(projection.track.id) { mutableStateOf("1") }
    var fixedDimension by rememberSaveable(projection.track.id) { mutableStateOf(UnitDimension.Count) }
    var fixedUnitId by rememberSaveable(projection.track.id) { mutableStateOf("count") }
    var history by rememberSaveable(projection.track.id) { mutableStateOf(TrackGoalHistory.NewEntriesOnly) }
    var historyDate by rememberSaveable(projection.track.id) { mutableStateOf(today) }
    var historyDatePicker by rememberSaveable(projection.track.id) { mutableStateOf(false) }
    var deadline by rememberSaveable(projection.track.id) { mutableStateOf<LocalDate?>(null) }
    var deadlinePicker by rememberSaveable(projection.track.id) { mutableStateOf(false) }
    var consistencyPeriod by rememberSaveable(projection.track.id) { mutableStateOf(GoalConsistencyPeriod.Week) }
    var consistencyRequiredPeriods by rememberSaveable(projection.track.id) { mutableStateOf("4") }
    val numericFields = projection.fields.filter { it.type in setOf(TrackFieldType.Number, TrackFieldType.Scale) }
    val needsField = aggregation.needsTrackNumberField()
    val selectedField = numericFields.firstOrNull { it.id == sourceFieldId } ?: numericFields.firstOrNull()
    val behaviors = when (aggregation) {
        TrackAggregation.CountEntries, TrackAggregation.CountMatchingEntries -> listOf(GoalType.ReachValue, GoalType.AccumulateTotal, GoalType.Consistency)
        TrackAggregation.FixedAmount -> listOf(GoalType.ReachValue, GoalType.AccumulateTotal)
        TrackAggregation.Sum -> listOf(GoalType.AccumulateTotal)
        TrackAggregation.Average -> listOf(GoalType.MeetAverage)
        TrackAggregation.Latest -> listOf(GoalType.ReachValue, GoalType.ReduceValue, GoalType.MaintainRange, GoalType.OpenEndedTrend)
        TrackAggregation.Minimum, TrackAggregation.Maximum -> listOf(GoalType.OpenEndedTrend)
    }
    LaunchedEffect(aggregation) {
        if (goalType !in behaviors) goalType = behaviors.first()
        if (needsField && sourceFieldId == null) sourceFieldId = numericFields.firstOrNull()?.id
    }
    val retroactiveFrom = when (history) {
        TrackGoalHistory.NewEntriesOnly -> null
        TrackGoalHistory.SinceGoalStart -> today
        TrackGoalHistory.SinceDate -> historyDate
        TrackGoalHistory.AllHistory -> projection.entries.minOfOrNull { it.entry.entryDate } ?: today
    }
    val previewProjection = projection.copy(
        entries = retroactiveFrom?.let { since -> projection.entries.filter { !it.entry.entryDate.isBefore(since) } }.orEmpty(),
    )
    val preview = previewProjection.aggregate(
        aggregation = aggregation,
        fieldUuid = selectedField?.uuid,
        conditions = conditions,
        conditionMode = conditionMode,
        fixedCanonicalValue = fixedAmount.toWhipDoubleOrNull(),
    )
    val matchingPreviewCount = previewProjection.matchingEntries(conditions, conditionMode).size
    val conditionSkippedCount = previewProjection.entries.size - matchingPreviewCount
    val blankSourceCount = if (needsField) matchingPreviewCount - preview.eligibleEntryCount else 0
    val previewValueText = preview.value?.let { value ->
        when {
            aggregation == TrackAggregation.FixedAmount -> {
                val unit = (BuiltInUnits.all + customUnits).firstOrNull { it.id == fixedUnitId }
                value.formatCompact() + unit?.symbol?.takeIf(String::isNotBlank)?.let { " $it" }.orEmpty()
            }
            selectedField?.type == TrackFieldType.Number -> {
                val unit = (BuiltInUnits.all + customUnits).firstOrNull { it.id == selectedField.unitId }
                (unit?.fromCanonical(value) ?: value).formatForField(selectedField.precision) + unit?.symbol?.takeIf(String::isNotBlank)?.let { " $it" }.orEmpty()
            }
            aggregation in setOf(TrackAggregation.CountEntries, TrackAggregation.CountMatchingEntries) -> "${value.formatCompact()} Entries"
            else -> value.formatCompact()
        }
    }
    val requiresTarget = goalType != GoalType.OpenEndedTrend
    val targetValid = !requiresTarget || target.toWhipDoubleOrNull()?.let { value ->
        goalType != GoalType.Consistency || value > 0.0
    } == true
    val rangeValid = goalType != GoalType.MaintainRange || targetMax.toWhipDoubleOrNull()?.let { max ->
        target.toWhipDoubleOrNull()?.let { it <= max }
    } == true
    val consistencyValid = goalType != GoalType.Consistency || consistencyRequiredPeriods.toIntOrNull()?.let { it > 0 } == true
    val valid = goalName.isNotBlank() && (!needsField || selectedField != null) && targetValid && rangeValid && consistencyValid &&
        (aggregation != TrackAggregation.FixedAmount || fixedAmount.toWhipDoubleOrNull() != null)
    PaneAwareAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create a Goal From Entries") },
        text = {
            LazyColumn(
                modifier = Modifier.testTag("track-new-goal-content"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { Text("Choose how Entries in ${projection.track.name} should change the new Goal. Every contribution remains separately auditable.") }
                item {
                    SelectionField(
                        "How Entries Add Progress",
                        userSelectableTrackAutomationMeasures,
                        aggregation,
                        TrackAggregation::automationLabel,
                        { aggregation = it },
                        modifier = Modifier.testTag("track-goal-measure"),
                    )
                }
                item { Text(aggregation.automationExplanation(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (needsField) item {
                    SelectionField("Number or Scale Field", numericFields, selectedField, { it?.name ?: "No Compatible Fields" }, { sourceFieldId = it?.id })
                }
                if (aggregation == TrackAggregation.FixedAmount) {
                    item { TrackNumberField(fixedAmount, { fixedAmount = it }, "Amount per Matching Entry") }
                    item { SelectionField("Measurement Type", UnitDimension.entries, fixedDimension, UnitDimension::uiLabel, { selected ->
                        fixedDimension = selected
                        fixedUnitId = (BuiltInUnits.all + customUnits).firstOrNull { it.dimension == selected && !it.archived }?.id.orEmpty()
                    }) }
                    item { UnitSelectionField(BuiltInUnits.all + customUnits, fixedUnitId, fixedDimension, { fixedUnitId = it }, onCreateCustomUnit, label = "Unit") }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Include Entries Where", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (conditions.isEmpty()) Text("All Entries", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        conditions.forEachIndexed { index, condition ->
                            val fieldName = projection.conditionFieldName(condition)
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("$fieldName ${condition.operator.uiLabel()} ${condition.summaryValue(projection, BuiltInUnits.all + customUnits)}".trim(), Modifier.weight(1f))
                                WhipTextButton(onClick = { conditions = conditions.toMutableList().also { it.removeAt(index) } }) { Text("Remove") }
                            }
                        }
                        if (conditions.size > 1) SegmentedChoiceBar(conditionMode, TrackConditionMode.entries, { conditionMode = it }, { if (it == TrackConditionMode.MatchAll) "Match All" else "Match Any" }, Modifier.fillMaxWidth())
                        WhipOutlinedButton(onClick = { addingCondition = true }, modifier = Modifier.fillMaxWidth()) { Text("Add Condition") }
                    }
                }
                item { OutlinedTextField(goalName, { goalName = it }, label = { Text("Goal Name") }, modifier = Modifier.fillMaxWidth().testTag("track-goal-name")) }
                item { SelectionField("Goal Behavior", behaviors, goalType, GoalType::trackGoalLabel, { goalType = it }) }
                if (requiresTarget) item { TrackNumberField(target, { target = it }, when (goalType) {
                    GoalType.MaintainRange -> "Minimum"
                    GoalType.Consistency -> "Entries per Period"
                    else -> "Target"
                }, Modifier.testTag("track-goal-target")) }
                if (goalType == GoalType.MaintainRange) item { TrackNumberField(targetMax, { targetMax = it }, "Maximum") }
                if (goalType == GoalType.Consistency) item {
                    ResponsiveFieldPair(
                        first = { field -> Column(field) { SelectionField("Period", GoalConsistencyPeriod.entries, consistencyPeriod, { it.name }, { consistencyPeriod = it }) } },
                        second = { field -> OutlinedTextField(consistencyRequiredPeriods, { consistencyRequiredPeriods = it.filter(Char::isDigit) }, label = { Text("Number of Periods") }, singleLine = true, modifier = field) },
                    )
                }
                item { SelectionField("History", TrackGoalHistory.entries, history, TrackGoalHistory::label, { history = it }) }
                if (history == TrackGoalHistory.SinceDate) item {
                    WhipOutlinedButton(onClick = { historyDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Since ${historyDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}")
                    }
                }
                item {
                    WhipOutlinedButton(onClick = { deadlinePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(deadline?.let { "Deadline ${it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}" } ?: "Add Optional Deadline")
                    }
                    if (deadline != null) WhipTextButton(onClick = { deadline = null }) { Text("Remove Deadline") }
                }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Preview", fontWeight = FontWeight.Bold)
                            Text("${previewProjection.entries.size} scanned · ${preview.eligibleEntryCount} eligible · ${preview.skippedEntryCount} skipped")
                            if (conditionSkippedCount > 0) Text("Did not match conditions · $conditionSkippedCount", style = MaterialTheme.typography.bodySmall)
                            if (blankSourceCount > 0) Text("Blank source Field · $blankSourceCount", style = MaterialTheme.typography.bodySmall)
                            Text(previewValueText?.let { "Current result $it" } ?: "No numeric result yet")
                            retroactiveFrom?.takeIf { it.isBefore(today) }?.let { start ->
                                Text(
                                    "The new Goal will start ${start.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))} so every included contribution counts toward its current progress.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text("Creating this adds one Goal and one Goal Automation.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { WhipTextButton(enabled = valid && !saving, onClick = {
            onSave(
                TrackGoalAutomationDraft(
                    goalName = goalName,
                    goalType = goalType,
                    aggregation = aggregation,
                    sourceFieldId = selectedField?.id,
                    conditionMode = conditionMode,
                    conditions = conditions,
                    target = target.toWhipDoubleOrNull(),
                    targetMax = targetMax.toWhipDoubleOrNull(),
                    fixedAmount = fixedAmount.toWhipDoubleOrNull(),
                    fixedDimension = fixedDimension,
                    fixedUnitId = fixedUnitId,
                    retroactiveFrom = retroactiveFrom,
                    deadline = deadline,
                    consistencyPeriod = consistencyPeriod,
                    consistencyRequiredPeriods = consistencyRequiredPeriods.toIntOrNull().takeIf { goalType == GoalType.Consistency },
                ),
            )
        }, modifier = Modifier.testTag("track-goal-confirm")) { Text(if (saving) "Creating…" else "Create Goal and Automation") } },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (addingCondition) TrackConditionEditor(projection, { addingCondition = false }, today, BuiltInUnits.all + customUnits) { conditions = conditions + it; addingCondition = false }
    if (historyDatePicker) WhipDatePickerDialog(historyDate, { historyDatePicker = false }, { historyDate = it; historyDatePicker = false })
    if (deadlinePicker) WhipDatePickerDialog(deadline ?: today, { deadlinePicker = false }, { deadline = it; deadlinePicker = false })
}

@Composable
private fun TrackExistingGoalAutomationDialog(
    projection: TrackProjection,
    goals: List<Goal>,
    customUnits: List<UnitDefinition>,
    saving: Boolean,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (LinkRuleDraft, Boolean) -> Unit,
) {
    var goalId by rememberSaveable(projection.track.id) { mutableStateOf(goals.firstOrNull()?.id) }
    val selectedGoal = goals.firstOrNull { it.id == goalId } ?: goals.firstOrNull()
    var aggregation by rememberSaveable(projection.track.id) {
        mutableStateOf(selectedGoal?.compatibleTrackAutomationMeasures()?.firstOrNull() ?: TrackAggregation.CountEntries)
    }
    var sourceFieldId by rememberSaveable(projection.track.id) { mutableStateOf<Long?>(null) }
    var fixedValue by rememberSaveable(projection.track.id) { mutableStateOf("1") }
    var conditionMode by rememberSaveable(projection.track.id) { mutableStateOf(TrackConditionMode.MatchAll) }
    var conditions by remember(projection.track.id) { mutableStateOf<List<TrackCondition>>(emptyList()) }
    var addingCondition by rememberSaveable(projection.track.id) { mutableStateOf(false) }
    var history by rememberSaveable(projection.track.id) { mutableStateOf(TrackGoalHistory.NewEntriesOnly) }
    var historyDate by rememberSaveable(projection.track.id) { mutableStateOf(today) }
    var pickingHistoryDate by rememberSaveable(projection.track.id) { mutableStateOf(false) }
    val numericFields = projection.fields.filter { field ->
        field.type == TrackFieldType.Scale && selectedGoal?.dimension == UnitDimension.Unitless ||
            field.type == TrackFieldType.Number && field.dimension == selectedGoal?.dimension
    }
    val compatibleMeasures = selectedGoal?.compatibleTrackAutomationMeasures().orEmpty().filter { measure ->
        !measure.needsTrackNumberField() || numericFields.isNotEmpty()
    }
    LaunchedEffect(selectedGoal?.id, compatibleMeasures) {
        if (aggregation !in compatibleMeasures && compatibleMeasures.isNotEmpty()) aggregation = compatibleMeasures.first()
        if (sourceFieldId !in numericFields.map(TrackField::id)) sourceFieldId = numericFields.firstOrNull()?.id
    }
    val needsField = aggregation.needsTrackNumberField()
    val selectedField = numericFields.firstOrNull { it.id == sourceFieldId } ?: numericFields.firstOrNull()
    val requiredGoalAggregation = selectedGoal?.requiredAggregationForTrack(aggregation)
    val historyStart = when (history) {
        TrackGoalHistory.NewEntriesOnly -> null
        TrackGoalHistory.SinceGoalStart -> selectedGoal?.startDate ?: today
        TrackGoalHistory.SinceDate -> historyDate
        TrackGoalHistory.AllHistory -> projection.entries.minOfOrNull { it.entry.entryDate } ?: today
    }
    val previewEntries = historyStart?.let { since -> projection.entries.filter { !it.entry.entryDate.isBefore(since) } }.orEmpty()
    val fixedCanonical = fixedValue.toWhipDoubleOrNull()?.let { value ->
        val unit = (BuiltInUnits.all + customUnits).firstOrNull { it.id == selectedGoal?.unitId }
        unit?.toCanonical(value) ?: value
    }
    val preview = projection.copy(entries = previewEntries).aggregate(
        aggregation = aggregation,
        fieldUuid = selectedField?.uuid,
        conditions = conditions,
        conditionMode = conditionMode,
        fixedCanonicalValue = fixedCanonical,
    )
    val valid = selectedGoal != null && compatibleMeasures.isNotEmpty() &&
        (!needsField || selectedField != null) &&
        (aggregation != TrackAggregation.FixedAmount || fixedValue.toWhipDoubleOrNull() != null)
    PaneAwareAlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Connect Entries to a Goal") },
        text = {
            LazyColumn(
                modifier = Modifier.testTag("track-existing-goal-content"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { Text("Each eligible Entry becomes one explainable contribution. Editing or deleting an Entry recalculates the Goal automatically.") }
                if (selectedGoal == null) {
                    item { Text("No active compatible Goals are available. Create a Goal first, then return here.") }
                } else {
                    item {
                        SelectionField(
                            "Goal",
                            goals,
                            selectedGoal,
                            Goal::name,
                            { goalId = it.id },
                            modifier = Modifier.testTag("track-existing-goal"),
                        )
                    }
                    item {
                        SelectionField(
                            "How Entries Add Progress",
                            compatibleMeasures,
                            aggregation,
                            TrackAggregation::automationLabel,
                            { aggregation = it },
                            modifier = Modifier.testTag("track-existing-goal-measure"),
                        )
                    }
                    item { Text(aggregation.automationExplanation(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (requiredGoalAggregation != selectedGoal.aggregation) item {
                        Card(Modifier.fillMaxWidth()) {
                            Text(
                                "${selectedGoal.name} currently calculates its progress as ${selectedGoal.aggregation.automationCalculationLabel()}. " +
                                    "This Automation will change it to ${requireNotNull(requiredGoalAggregation).automationCalculationLabel()} so the result matches your choice.",
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                    if (needsField) item {
                        SelectionField("Number or Scale Field", numericFields, selectedField, { it?.name ?: "No Compatible Fields" }, { sourceFieldId = it?.id })
                    }
                    if (aggregation == TrackAggregation.FixedAmount) item {
                        TrackNumberField(fixedValue, { fixedValue = it }, "Amount per Entry (${selectedGoal.unitId})")
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Only Include Entries Where", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (conditions.isEmpty()) Text("All Entries are eligible.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            conditions.forEachIndexed { index, condition ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${projection.conditionFieldName(condition)} ${condition.operator.uiLabel()} ${condition.summaryValue(projection, BuiltInUnits.all + customUnits)}".trim(),
                                        Modifier.weight(1f),
                                    )
                                    WhipTextButton(onClick = { conditions = conditions.toMutableList().also { it.removeAt(index) } }) { Text("Remove") }
                                }
                            }
                            if (conditions.size > 1) SegmentedChoiceBar(
                                conditionMode,
                                TrackConditionMode.entries,
                                { conditionMode = it },
                                { if (it == TrackConditionMode.MatchAll) "Match All" else "Match Any" },
                                Modifier.fillMaxWidth(),
                            )
                            WhipOutlinedButton(onClick = { addingCondition = true }, modifier = Modifier.fillMaxWidth()) { Text("Add Condition") }
                        }
                    }
                    item { SelectionField("Use Which Entries?", TrackGoalHistory.entries, history, TrackGoalHistory::label, { history = it }) }
                    if (history == TrackGoalHistory.SinceDate) item {
                        WhipOutlinedButton(onClick = { pickingHistoryDate = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Since ${historyDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}")
                        }
                    }
                    historyStart?.takeIf { it.isBefore(selectedGoal.startDate) }?.let { alignedStart ->
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Text(
                                    "To make every included contribution count, this will move ${selectedGoal.name}'s start from " +
                                        "${selectedGoal.startDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))} to " +
                                        "${alignedStart.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}.",
                                    modifier = Modifier.padding(12.dp),
                                )
                            }
                        }
                    }
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("Cause and Effect Preview", fontWeight = FontWeight.Bold)
                                if (history == TrackGoalHistory.NewEntriesOnly) {
                                    Text(
                                        "Existing Entries stay unchanged. " +
                                            aggregation.automationExplanation().replaceFirst("Each eligible Entry", "Each new eligible Entry"),
                                    )
                                } else {
                                    Text("${previewEntries.size} scanned · ${preview.eligibleEntryCount} eligible · ${preview.skippedEntryCount} skipped")
                                    Text("Current Goal result from these Entries: ${preview.value?.formatCompact() ?: "No value yet"}")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            WhipTextButton(
                enabled = valid && !saving,
                modifier = Modifier.testTag("track-existing-goal-confirm"),
                onClick = {
                    val goal = requireNotNull(selectedGoal)
                    onSave(
                        LinkRuleDraft(
                            name = "${projection.track.name} → ${goal.name}",
                            sourceType = LinkSourceType.Track,
                            sourceEntityId = projection.track.id,
                            sourceMetric = aggregation.progressSourceMetric(),
                            targetGoalId = goal.id,
                            valueMode = if (aggregation == TrackAggregation.FixedAmount) LinkValueMode.FixedValue else LinkValueMode.SourceValue,
                            fixedValue = fixedValue.toWhipDoubleOrNull(),
                            retroactiveFrom = historyStart,
                            trackAggregation = aggregation,
                            sourceFieldId = selectedField?.id.takeIf { needsField },
                            conditionMode = conditionMode,
                            conditions = conditions,
                        ),
                        history != TrackGoalHistory.NewEntriesOnly,
                    )
                },
            ) { Text(if (saving) "Connecting…" else "Connect Goal") }
        },
        dismissButton = { WhipTextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } },
    )
    if (addingCondition) TrackConditionEditor(
        projection,
        { addingCondition = false },
        today,
        BuiltInUnits.all + customUnits,
    ) { conditions = conditions + it; addingCondition = false }
    if (pickingHistoryDate) WhipDatePickerDialog(
        historyDate,
        { pickingHistoryDate = false },
        { historyDate = it; pickingHistoryDate = false },
    )
}

private fun GoalType.trackGoalLabel(): String = when (this) {
    GoalType.ReachValue -> "Reach a Target"
    GoalType.ReduceValue -> "Reduce a Value"
    GoalType.AccumulateTotal -> "Build a Total"
    GoalType.MaintainRange -> "Stay in a Range"
    GoalType.MeetAverage -> "Meet an Average"
    GoalType.OpenEndedTrend -> "Observe a Trend"
    GoalType.Consistency -> "Stay Consistent"
    GoalType.WeightedMilestones -> "Finish Milestones"
    GoalType.ElapsedSince -> "Count Time Since"
}

@Composable
private fun TrackNumberField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(value, onValueChange, label = { Text(label) }, singleLine = true, modifier = modifier.fillMaxWidth())
}

@Composable
private fun AllTracksPage(
    state: TrackUiState,
    innerPadding: PaddingValues,
    query: String,
    onQueryChange: (String) -> Unit,
    showArchived: Boolean,
    onShowArchivedChange: (Boolean) -> Unit,
    onOpen: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onAddEntry: (Long) -> Unit,
    onCreate: () -> Unit,
    onReorder: (List<Long>) -> Unit,
    reorderEnabled: Boolean,
    onShowAllAreasForReorder: () -> Unit,
    onSetPinned: (Collection<Long>, Boolean) -> Unit,
    onSetArchived: (Collection<Long>, Boolean) -> Unit,
    onOpenPrompt: (Long) -> Unit,
    onRemindPrompt: (Long) -> Unit,
    onDismissPrompt: (Long) -> Unit,
    masterPane: Boolean,
    onReorderModeChange: (Boolean) -> Unit = {},
    reorderDismissRequest: Int = 0,
) {
    val userCompact = LocalCompactItemLayout.current
    var moreOpen by rememberSaveable { mutableStateOf(false) }
    var reordering by rememberSaveable { mutableStateOf(false) }
    var selecting by rememberSaveable { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val source = if (showArchived) state.archived else state.active
    val shown = source.filter { projection ->
        query.isBlank() || projection.track.name.contains(query, true) || projection.track.description.contains(query, true) ||
            projection.track.tags.any { it.contains(query, true) }
    }
    val pinned = if (showArchived) emptyList() else shown.filter { it.track.pinned }
    val unpinned = if (showArchived) shown else shown.filterNot { it.track.pinned }
    BackHandler(enabled = reordering) { reordering = false }
    BackHandler(enabled = selecting) { selecting = false; selectedIds = emptySet() }
    DisposableEffect(reordering) {
        onReorderModeChange(reordering)
        onDispose { if (reordering) onReorderModeChange(false) }
    }
    LaunchedEffect(reorderDismissRequest) {
        if (reorderDismissRequest > 0) reordering = false
    }
    LaunchedEffect(query, reorderEnabled, showArchived) {
        if (query.isNotBlank() || !reorderEnabled || showArchived) reordering = false
    }
    val visibleTrackIds = source.mapTo(mutableSetOf()) { it.track.id }
    val captureRules = state.triggerRules.filter { it.targetType == TriggerTargetType.Track && it.targetEntityId in visibleTrackIds }
    val captureRuleIds = captureRules.mapTo(mutableSetOf(), TriggerRule::id)
    val now = Instant.now()
    val readyPrompts = state.triggerOccurrences.filter { occurrence ->
        occurrence.triggerRuleId in captureRuleIds && occurrence.dismissedAt == null && occurrence.fulfilledEntryId == null &&
            !(occurrence.remindAt ?: occurrence.availableAt).isAfter(now)
    }
    fun moveWithin(group: List<TrackProjection>, index: Int, delta: Int) {
        val moved = moveListItem(group, index, delta)
        if (moved == group) return
        val all = if (group.firstOrNull()?.track?.pinned == true) moved + state.active.filterNot { it.track.pinned }
        else state.active.filter { it.track.pinned } + moved
        onReorder(all.map { it.track.id })
    }
    WhipReorderLazyColumn(
        Modifier.fillMaxSize().padding(innerPadding).testTag("track-list"),
        contentPadding = PaddingValues(
            start = if (masterPane) 12.dp else 20.dp,
            top = 16.dp,
            end = if (masterPane) 12.dp else 20.dp,
            bottom = WhipSpacing.screenExpanded,
        ),
        verticalArrangement = Arrangement.spacedBy(if (userCompact) 4.dp else 12.dp),
    ) {
        item {
            WhipPageHeader(
                title = if (showArchived) "Archived Tracks" else "Tracks",
                supportingText = "Structured logs that can support actions and goals.",
            ) {
                Text(
                    quantityLabel(shown.size, "Track"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!reordering) Box {
                    WhipPageIconAction(Icons.Outlined.MoreVert, "More Track Options", { moreOpen = true })
                    DropdownMenu(moreOpen, { moreOpen = false }) {
                        WhipMenuItem(
                            label = if (showArchived) "Show Active Tracks" else "Show Archived Tracks",
                            onClick = { reordering = false; onShowArchivedChange(!showArchived); moreOpen = false },
                        )
                        if (!showArchived && (state.active.size > 1 || query.isNotBlank() || !reorderEnabled)) WhipMenuItem(
                            label = when {
                                query.isNotBlank() && !reorderEnabled -> "Clear Search, Show All Areas & Reorder"
                                query.isNotBlank() -> "Clear Search & Reorder All"
                                !reorderEnabled -> "Show All Areas & Reorder"
                                else -> "Reorder Tracks"
                            },
                            onClick = {
                                onQueryChange("")
                                if (!reorderEnabled) onShowAllAreasForReorder()
                                selecting = false
                                selectedIds = emptySet()
                                reordering = true
                                moreOpen = false
                            },
                        )
                        if (shown.isNotEmpty()) WhipMenuItem(
                            label = if (selecting) "Cancel Selection" else "Select Tracks",
                            onClick = {
                                selecting = !selecting
                                reordering = false
                                if (!selecting) selectedIds = emptySet()
                                moreOpen = false
                            },
                        )
                    }
                }
            }
        }
        if (reordering) item {
            WhipReorderModeBar(
                itemLabel = "Tracks",
                onDone = { reordering = false },
                boundaryNote = "Pinned and other Tracks reorder separately.",
            )
        }
        if (selecting) item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${quantityLabel(selectedIds.size, "Track")} Selected", fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!showArchived) {
                            val allPinned = selectedIds.isNotEmpty() && source.filter { it.track.id in selectedIds }.all { it.track.pinned }
                            WhipOutlinedButton(enabled = selectedIds.isNotEmpty(), onClick = {
                                onSetPinned(selectedIds, !allPinned)
                                selectedIds = emptySet()
                                selecting = false
                            }) { Text(if (allPinned) "Unpin" else "Pin") }
                            WhipOutlinedButton(enabled = selectedIds.isNotEmpty(), onClick = {
                                onSetArchived(selectedIds, true)
                                selectedIds = emptySet()
                                selecting = false
                            }) { Text("Archive") }
                        } else {
                            WhipOutlinedButton(enabled = selectedIds.isNotEmpty(), onClick = {
                                onSetArchived(selectedIds, false)
                                selectedIds = emptySet()
                                selecting = false
                            }) { Text("Restore") }
                        }
                        WhipTextButton(onClick = { selectedIds = emptySet(); selecting = false }) { Text("Cancel") }
                    }
                }
            }
        }
        if (!reordering && !showArchived && readyPrompts.isNotEmpty()) {
            item { Text("Pending Entries", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(readyPrompts, key = { "all-track-prompt-${it.id}" }) { occurrence ->
                TrackPromptCard(
                    occurrence,
                    captureRules.firstOrNull { it.id == occurrence.triggerRuleId },
                    { onOpenPrompt(occurrence.id) },
                    { onRemindPrompt(occurrence.id) },
                    { onDismissPrompt(occurrence.id) },
                )
            }
        }
        if (state.loading) item { DomainLoadContent("Tracks", PaddingValues()) }
        else if (shown.isEmpty()) item {
            WhipEmptyState(
                title = when {
                    query.isNotBlank() -> "No Matching Tracks"
                    showArchived -> "No Archived Tracks"
                    else -> "Track What Matters"
                },
                supportingText = when {
                    query.isNotBlank() -> "Try a different name, tag, or description."
                    showArchived -> "Archived Tracks appear here and can be restored."
                    else -> "Create a reusable log for anything you want to record, compare, or connect to a Goal."
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                primaryActionLabel = "Create First Track".takeUnless { showArchived || query.isNotBlank() },
                onPrimaryAction = onCreate.takeUnless { showArchived || query.isNotBlank() },
            )
        } else {
            if (pinned.isNotEmpty() && !showArchived) {
                item { Text("Pinned Tracks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                itemsIndexed(pinned, key = { _, item -> "track-pinned-${item.track.id}" }) { index, item ->
                    TrackRow(
                        item, onOpen, onEdit, onAddEntry,
                        onMove = if (reordering) {{ delta -> moveWithin(pinned, index, delta) }} else null,
                        canMoveEarlier = index > 0,
                        canMoveLater = index < pinned.lastIndex,
                        reorderPosition = index + 1,
                        reorderTotal = pinned.size,
                        reordering = reordering,
                        selectionMode = selecting,
                        selected = item.track.id in selectedIds,
                        onSelectionToggle = { selectedIds = if (item.track.id in selectedIds) selectedIds - item.track.id else selectedIds + item.track.id },
                        onEnterSelection = { selecting = true; selectedIds = selectedIds + item.track.id; reordering = false },
                        compact = masterPane,
                    )
                }
                item { Text("Other Tracks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            }
            itemsIndexed(unpinned, key = { _, item -> "track-${item.track.id}" }) { index, item ->
                TrackRow(
                    item, onOpen, onEdit, onAddEntry,
                    onMove = if (reordering) {{ delta -> moveWithin(unpinned, index, delta) }} else null,
                    canMoveEarlier = index > 0,
                    canMoveLater = index < unpinned.lastIndex,
                    reorderPosition = index + 1,
                    reorderTotal = unpinned.size,
                    reordering = reordering,
                    selectionMode = selecting,
                    selected = item.track.id in selectedIds,
                    onSelectionToggle = { selectedIds = if (item.track.id in selectedIds) selectedIds - item.track.id else selectedIds + item.track.id },
                    onEnterSelection = { selecting = true; selectedIds = selectedIds + item.track.id; reordering = false },
                    compact = masterPane,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TrackRow(
    projection: TrackProjection,
    onOpen: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onAddEntry: (Long) -> Unit,
    onMove: ((Int) -> Unit)? = null,
    canMoveEarlier: Boolean = false,
    canMoveLater: Boolean = false,
    reorderPosition: Int? = null,
    reorderTotal: Int? = null,
    reordering: Boolean = false,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onSelectionToggle: (() -> Unit)? = null,
    onEnterSelection: (() -> Unit)? = null,
    compact: Boolean = false,
) {
    val userCompact = LocalCompactItemLayout.current
    val dense = compact || userCompact
    val reorderInteraction = rememberWhipReorderInteractionState()
    val selectable = selectionMode && onSelectionToggle != null
    val latest = projection.entries.maxWithOrNull(compareBy<TrackEntryProjection> { it.entry.entryDate }.thenBy { it.entry.createdAtMillis })
    if (dense && !selectable && !reordering) {
        CompactTrackRow(
            projection = projection,
            latest = latest,
            onOpen = onOpen,
            onEdit = onEdit,
            onAddEntry = onAddEntry,
            onEnterSelection = onEnterSelection,
        )
        return
    }
    Card(
        modifier = Modifier.fillMaxWidth()
            .whipReorderItem(
                reorderInteraction,
                layoutPosition = reorderPosition,
                layoutScope = "track-browse-${projection.track.pinned}",
            )
            .testTag("track-card-${projection.track.id}").combinedClickable(
            enabled = !reordering,
            onClickLabel = if (selectable) "${if (selected) "Deselect" else "Select"} ${projection.track.name}" else "Open ${projection.track.name}",
            onLongClickLabel = onEnterSelection?.let { "Select ${projection.track.name}" },
            onClick = { if (selectable) onSelectionToggle.invoke() else onOpen(projection.track.id) },
            onLongClick = onEnterSelection,
        )
            .semantics {
                if (!reordering) role = Role.Button
                contentDescription = buildString {
                    append("${projection.track.name}, ${projection.entries.size} Entries")
                    latest?.let {
                        append(", latest ${projection.primaryText(it)}, ${it.entry.entryDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}")
                    }
                    append(
                        when {
                            reordering -> ". Reordering"
                            selectable -> ". ${if (selected) "Selected" else "Not selected"}. Select Track"
                            else -> ". Open Track"
                        },
                    )
                }
            },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(
                horizontal = if (dense) 8.dp else 14.dp,
                vertical = if (dense) 4.dp else 14.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(if (dense) 2.dp else 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (selectable) Checkbox(selected, onCheckedChange = null)
                if (reordering && onMove != null) {
                    WhipReorderHandle(
                        label = projection.track.name,
                        canMovePrevious = canMoveEarlier,
                        canMoveNext = canMoveLater,
                        position = reorderPosition,
                        total = reorderTotal,
                        interactionState = reorderInteraction,
                        moveWholeItem = true,
                        layoutScope = "track-browse-${projection.track.pinned}",
                        reserveWhenUnavailable = true,
                        onMove = onMove,
                    )
                }
                WhipIdentityEmoji(projection.track.icon)
                Column(Modifier.weight(1f)) {
                    Text(
                        projection.track.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text("${projection.track.area} · ${quantityLabel(projection.entries.size, "Entry")}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (selectable) {
                    Text(if (selected) "Selected" else "Not Selected", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (!reordering) {
                    ItemEditButton(
                        "Track",
                        projection.track.name,
                        onEdit = { onEdit(projection.track.id) },
                        modifier = Modifier.testTag("track-edit-action-${projection.track.id}"),
                    )
                }
            }
            latest?.let {
                Text(
                    "Latest: ${projection.primaryText(it)} · ${it.entry.entryDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!dense && !selectable) {
                WhipOutlinedButton(
                    onClick = { onAddEntry(projection.track.id) },
                    enabled = !projection.track.archived && !reordering,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(if (projection.track.archived) "Restore to Add Entries" else projection.addEntryLabel())
                }
            } else if (dense && !reordering && !selectable) {
                WhipTextButton(
                    onClick = { onAddEntry(projection.track.id) },
                    enabled = !projection.track.archived,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (projection.track.archived) "Archived" else projection.addEntryLabel()) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactTrackRow(
    projection: TrackProjection,
    latest: TrackEntryProjection?,
    onOpen: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onAddEntry: (Long) -> Unit,
    onEnterSelection: (() -> Unit)?,
) {
    val disclosure = rememberCompactItemDisclosure("track:${projection.track.id}")
    val addLabel = if (projection.track.archived) "Archived" else projection.addEntryLabel()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("track-card-${projection.track.id}")
            .combinedClickable(
                onClickLabel = "Open ${projection.track.name}",
                onLongClickLabel = onEnterSelection?.let { "Select ${projection.track.name}" },
                onClick = { onOpen(projection.track.id) },
                onLongClick = onEnterSelection,
            )
            .semantics {
                role = Role.Button
                contentDescription = buildString {
                    append("${projection.track.name}, ${projection.entries.size} Entries")
                    latest?.let {
                        append(", latest ${projection.primaryText(it)}, ${it.entry.entryDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}")
                    }
                    append(". Open Track")
                }
            },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                WhipIdentityEmoji(projection.track.icon)
                Column(Modifier.weight(1f)) {
                    Text(
                        projection.track.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${projection.track.area} · ${quantityLabel(projection.entries.size, "Entry")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = { onAddEntry(projection.track.id) },
                    enabled = !projection.track.archived,
                    modifier = Modifier.size(48.dp).testTag("track-primary-action-${projection.track.id}"),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = addLabel)
                }
                IconButton(
                    onClick = disclosure.toggle,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("track-expand-${projection.track.id}")
                        .semantics {
                            contentDescription = "${if (disclosure.expanded) "Collapse" else "Expand"} Track ${projection.track.name}"
                            stateDescription = if (disclosure.expanded) "Expanded" else "Collapsed"
                        },
                ) {
                    Icon(
                        if (disclosure.expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                    )
                }
            }
            if (disclosure.expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth().testTag("track-expanded-${projection.track.id}"),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    latest?.let {
                        Text(
                            "Latest: ${projection.primaryText(it)} · ${it.entry.entryDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    WhipTextButton(
                        onClick = { onEdit(projection.track.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("track-edit-action-${projection.track.id}")
                            .semantics { contentDescription = "Edit Track ${projection.track.name}" },
                    ) { Icon(Icons.Outlined.Edit, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Edit") }
                }
            }
        }
    }
}

@Composable
private fun TrackDetailPage(
    projection: TrackProjection,
    innerPadding: PaddingValues,
    destination: TrackDetailDestination,
    onDestinationChange: (TrackDetailDestination) -> Unit,
    onBack: () -> Unit,
    onEditTrack: () -> Unit,
    onAddEntry: () -> Unit,
    onEditEntry: (Long) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    onSetPinned: (Boolean) -> Unit,
    onSetArchived: (Boolean) -> Unit,
    onDuplicate: () -> Unit,
    onDeleteTrack: () -> Unit,
    onSetGoal: () -> Unit,
    onConnectGoal: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    state: TrackUiState,
    today: LocalDate,
    viewModel: TrackViewModel,
    customUnits: List<UnitDefinition>,
    onOpenPrompt: (Long) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    saving: Boolean,
    dialogModifier: Modifier,
) {
    val captureRules = state.triggerRules.filter { it.targetType == TriggerTargetType.Track && it.targetEntityId == projection.track.id }
    val followUpRules = state.triggerRules.filter { it.sourceType == LinkSourceType.Track && it.sourceEntityId == projection.track.id }
    val progressRules = state.linkRules.filter { it.sourceType == LinkSourceType.Track && it.sourceEntityId == projection.track.id }
    val captureRuleIds = captureRules.mapTo(mutableSetOf(), TriggerRule::id)
    val now = Instant.now()
    val pending = state.triggerOccurrences.filter { occurrence ->
        occurrence.triggerRuleId in captureRuleIds && occurrence.dismissedAt == null && occurrence.fulfilledEntryId == null &&
            !(occurrence.remindAt ?: occurrence.availableAt).isAfter(now)
    }
    Column(Modifier.fillMaxSize().padding(innerPadding)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val constrainedHeader = maxWidth < 280.dp
            Row(
                Modifier.fillMaxWidth().padding(horizontal = if (constrainedHeader) 4.dp else 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(if (constrainedHeader) 2.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = "Back to Tracks" }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        projection.track.name,
                        style = if (constrainedHeader) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = if (constrainedHeader) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (constrainedHeader) quantityLabel(projection.entries.size, "Entry") else "${quantityLabel(projection.entries.size, "Entry")} · ${projection.track.area}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!constrainedHeader) ItemEditButton("Track", projection.track.name, onEditTrack)
            }
        }
        DestinationTabBar(
            selected = destination,
            destinations = TrackDetailDestination.entries,
            primaryDestinations = listOf(TrackDetailDestination.Entries, TrackDetailDestination.Automations, TrackDetailDestination.Insights),
            onSelect = onDestinationChange,
            label = TrackDetailDestination::label,
            compactLabel = TrackDetailDestination::label,
            testTagPrefix = "track-destination",
            barTestTag = "track-detail-navigation",
        )
        when (destination) {
            TrackDetailDestination.Entries -> TrackEntriesPage(
                projection,
                today,
                customUnits,
                onAddEntry,
                onEditEntry,
                onDeleteEntry,
                pending,
                captureRules,
                onOpenPrompt,
                { id -> viewModel.remindPrompt(id, Instant.now().plus(1, ChronoUnit.HOURS)) },
                viewModel::dismissPrompt,
                { query -> viewModel.searchEntryIds(projection.track.id, query) },
                { offset, limit -> viewModel.entryPage(projection.track.id, offset, limit) },
                dialogModifier,
            )
            TrackDetailDestination.Insights -> TrackInsightsPage(projection, customUnits, today, dialogModifier)
            TrackDetailDestination.Automations -> TrackAutomationsOverview(
                projection,
                state,
                captureRules,
                followUpRules,
                progressRules,
                pending,
                customUnits,
                onSetGoal,
                onConnectGoal,
                viewModel,
                onOpenPrompt,
                onRequestNotificationPermission,
                saving,
                today,
            )
            TrackDetailDestination.Options -> TrackOptionsPage(
                projection,
                onEditTrack,
                onSetPinned,
                onSetArchived,
                onDuplicate,
                onExport,
                onImport,
                onDeleteTrack,
            )
        }
    }
}

@Composable
private fun TrackEntriesPage(
    projection: TrackProjection,
    today: LocalDate,
    customUnits: List<UnitDefinition>,
    onAddEntry: () -> Unit,
    onEditEntry: (Long) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    pendingPrompts: List<TriggerOccurrence> = emptyList(),
    captureRules: List<TriggerRule> = emptyList(),
    onOpenPrompt: (Long) -> Unit = {},
    onRemindPrompt: (Long) -> Unit = {},
    onDismissPrompt: (Long) -> Unit = {},
    searchEntryIds: suspend (String) -> Set<Long> = { emptySet() },
    loadEntryPage: suspend (Int, Int) -> TrackEntryPage,
    dialogModifier: Modifier = Modifier,
) {
    var query by rememberSaveable(projection.track.id) { mutableStateOf("") }
    var sort by rememberSaveable(projection.track.id) { mutableStateOf(TrackSort.EntryDate) }
    var sortDirection by rememberSaveable(projection.track.id) { mutableStateOf(SortDirection.Descending) }
    var sortFieldId by rememberSaveable(projection.track.id) { mutableStateOf<Long?>(null) }
    var sortOpen by rememberSaveable(projection.track.id) { mutableStateOf(false) }
    var filterOpen by rememberSaveable(projection.track.id) { mutableStateOf(false) }
    var conditions by remember(projection.track.id) { mutableStateOf<List<TrackCondition>>(emptyList()) }
    var conditionMode by rememberSaveable(projection.track.id) { mutableStateOf(TrackConditionMode.MatchAll) }
    var searchMatches by remember(projection.track.id) { mutableStateOf<Set<Long>?>(null) }
    var pagedEntries by remember(projection.track.id) { mutableStateOf<List<TrackEntryProjection>>(emptyList()) }
    var totalEntryCount by remember(projection.track.id) { mutableIntStateOf(projection.entries.size) }
    var pageLoading by remember(projection.track.id) { mutableStateOf(true) }
    var pageError by remember(projection.track.id) { mutableStateOf<String?>(null) }
    var viewEntryId by rememberSaveable(projection.track.id) { mutableStateOf<Long?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val pageVersion = projection.entries.maxOfOrNull { it.entry.updatedAtMillis } ?: 0L
    suspend fun reloadPage() {
        pageLoading = true
        pageError = null
        runCatching { loadEntryPage(0, TRACK_ENTRY_PAGE_SIZE) }
            .onSuccess { page -> pagedEntries = page.entries; totalEntryCount = page.totalCount }
            .onFailure { pageError = it.message ?: "Entries could not be loaded" }
        pageLoading = false
    }
    LaunchedEffect(projection.track.id, projection.entries.size, pageVersion) { reloadPage() }
    LaunchedEffect(query, projection.entries.size) {
        if (query.isBlank()) searchMatches = null
        else {
            kotlinx.coroutines.delay(120)
            searchMatches = runCatching { searchEntryIds(query) }.getOrNull()
        }
    }
    val sortField = projection.fields.firstOrNull { it.id == sortFieldId }
    val databasePagedView = query.isBlank() && conditions.isEmpty() && sort == TrackSort.EntryDate &&
        sortField == null && sortDirection == SortDirection.Descending
    val shown = if (databasePagedView) pagedEntries else {
        projection.matchingEntries(conditions, conditionMode)
            .filter { entry -> query.isBlank() || searchMatches?.contains(entry.entry.id) == true ||
                (searchMatches == null && projection.entrySearchText(entry).contains(query, true)) }
            .let { entries -> projection.sortedEntries(entries, sort, sortField, sortDirection) }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 16.dp, 20.dp, WhipSpacing.screenExpanded),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (projection.track.archived) item {
            WhipEmptyState(
                title = "Track Archived",
                supportingText = "History remains available. Restore this Track from Options before adding or editing Entries.",
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (pendingPrompts.isNotEmpty()) {
            item { Text("Ready to Capture", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(pendingPrompts, key = { "track-prompt-${it.id}" }) { occurrence ->
                TrackPromptCard(
                    occurrence,
                    captureRules.firstOrNull { it.id == occurrence.triggerRuleId },
                    { onOpenPrompt(occurrence.id) },
                    { onRemindPrompt(occurrence.id) },
                    { onDismissPrompt(occurrence.id) },
                )
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                WhipPageIconAction(Icons.Outlined.FilterAlt, "Filter Entries", { filterOpen = true }, badgeCount = conditions.size, active = conditions.isNotEmpty())
                WhipPageIconAction(
                    Icons.AutoMirrored.Outlined.Sort,
                    "Sort Entries by ${sortField?.name ?: sort.label}, ${sortDirection.label}",
                    { sortOpen = true },
                )
            }
        }
        if (conditions.isNotEmpty()) item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                conditions.forEachIndexed { index, condition ->
                    val fieldName = projection.conditionFieldName(condition)
                    WhipFilterChip(
                        selected = true,
                        onClick = { conditions = conditions.toMutableList().also { it.removeAt(index) } },
                        label = { Text("$fieldName ${condition.operator.uiLabel()} ×") },
                    )
                }
                WhipTextButton(onClick = { conditions = emptyList() }) { Text("Clear All") }
            }
        }
        if (databasePagedView && pageLoading && pagedEntries.isEmpty()) item {
            Text("Loading Entries…", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp))
        }
        pageError?.let { error -> item {
            WhipEmptyState("Entries Unavailable", error, Modifier.fillMaxWidth().padding(vertical = 24.dp))
            WhipOutlinedButton(onClick = { coroutineScope.launch { reloadPage() } }, modifier = Modifier.fillMaxWidth()) { Text("Try Again") }
        } }
        if (!pageLoading && pageError == null && shown.isEmpty()) item {
            WhipEmptyState(
                title = if (projection.entries.isEmpty()) "No Entries Yet" else "No Matching Entries",
                supportingText = if (projection.entries.isEmpty()) "Add the first ${projection.primaryField.name.lowercase()} when the fact is ready to record." else "Clear Search or Filters to see more Entries.",
                modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
            )
        }
        items(shown, key = { "entry-${it.entry.id}" }) { entry ->
            TrackEntryRow(
                projection = projection,
                entry = entry,
                customUnits = customUnits,
                editable = !projection.track.archived,
                onOpen = { viewEntryId = entry.entry.id },
                onEdit = { onEditEntry(entry.entry.id) },
                onDelete = { onDeleteEntry(entry.entry.id) },
            )
        }
        if (databasePagedView && pagedEntries.size < totalEntryCount) item {
            WhipOutlinedButton(
                enabled = !pageLoading,
                onClick = {
                    coroutineScope.launch {
                        pageLoading = true
                        pageError = null
                        runCatching { loadEntryPage(pagedEntries.size, TRACK_ENTRY_PAGE_SIZE) }
                            .onSuccess { page ->
                                pagedEntries = (pagedEntries + page.entries).distinctBy { it.entry.id }
                                totalEntryCount = page.totalCount
                            }
                            .onFailure { pageError = it.message ?: "More Entries could not be loaded" }
                        pageLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (pageLoading) "Loading…" else "Show ${minOf(TRACK_ENTRY_PAGE_SIZE, totalEntryCount - pagedEntries.size)} More · ${totalEntryCount - pagedEntries.size} Remaining")
            }
        }
    }
    if (sortOpen) {
        val choices = TrackSort.entries.map { TrackEntrySortChoice(it.label, sort = it) } +
            projection.sortableFields().map { TrackEntrySortChoice(it.name, fieldId = it.id) }
        val selectedChoice = choices.first { choice ->
            if (sortFieldId != null) choice.fieldId == sortFieldId else choice.sort == sort
        }
        PaneAwareAlertDialog(
            modifier = dialogModifier,
            onDismissRequest = { sortOpen = false },
            paneTitle = "Sort Entries",
            title = { Text("Sort Entries") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SelectionField(
                        label = "Sort by",
                        values = choices,
                        selected = selectedChoice,
                        valueText = TrackEntrySortChoice::label,
                        onSelect = { choice ->
                            sortFieldId = choice.fieldId
                            choice.sort?.let { sort = it }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SelectionField(
                        label = "Order",
                        values = SortDirection.entries,
                        selected = sortDirection,
                        valueText = SortDirection::label,
                        onSelect = { sortDirection = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = { WhipTextButton(onClick = { sortOpen = false }) { Text("Done") } },
        )
    }
    if (filterOpen) TrackFilterDialog(
        modifier = dialogModifier,
        projection = projection,
        initial = conditions,
        initialMode = conditionMode,
        today = today,
        units = BuiltInUnits.all + customUnits,
        onDismiss = { filterOpen = false },
        onApply = { mode, updated -> conditionMode = mode; conditions = updated; filterOpen = false },
    )
    viewEntryId?.let { entryId ->
        projection.entries.firstOrNull { it.entry.id == entryId }?.let { entry ->
            TrackEntryDetailsDialog(
                modifier = dialogModifier,
                projection = projection,
                entry = entry,
                customUnits = customUnits,
                editable = !projection.track.archived,
                onDismiss = { viewEntryId = null },
                onEdit = { viewEntryId = null; onEditEntry(entryId) },
            )
        }
    }
}

private const val TRACK_ENTRY_PAGE_SIZE = 100

@Composable
private fun TrackPromptCard(
    occurrence: TriggerOccurrence,
    rule: TriggerRule?,
    onAdd: () -> Unit,
    onRemind: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(rule?.name ?: "Capture Entry", fontWeight = FontWeight.Bold)
            Text(
                occurrence.sourceSnapshot.triggerSnapshotExplanation(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                WhipButton(onClick = onAdd) { Text("Add Entry") }
                WhipTextButton(onClick = onRemind) { Text("Remind in 1 Hour") }
                WhipTextButton(onClick = onDismiss) { Text("Dismiss") }
            }
        }
    }
}

private fun String.triggerSnapshotExplanation(): String = runCatching {
    org.json.JSONObject(this).optString("explanation").ifBlank { this }
}.getOrDefault(this)

@Composable
private fun TrackEntryRow(
    projection: TrackProjection,
    entry: TrackEntryProjection,
    customUnits: List<UnitDefinition>,
    editable: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var moreOpen by rememberSaveable(entry.entry.id) { mutableStateOf(false) }
    val supporting = projection.fields.filter(TrackField::showInList).take(2).mapNotNull { field ->
        projection.formattedValue(entry, field, BuiltInUnits.all + customUnits).takeIf(String::isNotBlank)?.let { "${field.name} $it" }
    }
    Card(Modifier.fillMaxWidth().clickable(onClickLabel = "Open Entry ${projection.primaryText(entry)}", onClick = onOpen)) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(projection.primaryText(entry), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text((supporting + entry.entry.entryDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))).joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                if (entry.entry.sourceExplanation.isNotBlank()) Text(entry.entry.sourceExplanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            IconButton(enabled = editable, onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "Edit Entry ${projection.primaryText(entry)}") }
            Box {
                IconButton(onClick = { moreOpen = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = "More Actions for ${projection.primaryText(entry)}") }
                DropdownMenu(moreOpen, { moreOpen = false }) {
                    WhipMenuItem(
                        label = "Delete Entry",
                        icon = Icons.Outlined.DeleteOutline,
                        role = WhipMenuItemRole.Destructive,
                        onClick = { moreOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackEntryDetailsDialog(
    modifier: Modifier,
    projection: TrackProjection,
    entry: TrackEntryProjection,
    customUnits: List<UnitDefinition>,
    editable: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
) {
    val units = BuiltInUnits.all + customUnits
    EntityInspector(
        entityType = "Track Entry",
        title = projection.primaryText(entry),
        emoji = projection.track.icon,
        context = "${projection.track.name} · ${entry.entry.entryDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}",
        status = "Recorded",
        sections = listOf(EntityInspectorSection("overview", "Overview")),
        selectedSectionId = "overview",
        onSelectSection = {},
        onDismiss = onDismiss,
        onEdit = onEdit.takeIf { editable },
        editLabel = "Edit Entry",
        modifier = modifier,
        legacySurfaceTag = "track-entry-detail-surface",
        legacySectionTagPrefix = "track-entry-detail-section",
        content = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    EntityInspectorGroup("Recorded evidence") {
                        EntityInspectorFact(
                            "Date",
                            entry.entry.entryDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                        )
                    }
                }
                items(projection.fields, key = TrackField::id) { field ->
                    val value = projection.formattedValue(entry, field, units).ifBlank { "—" }
                    EntityInspectorFact(field.name, value)
                }
                if (entry.entry.sourceExplanation.isNotBlank()) item {
                    EntityInspectorFact("How this entry was added", entry.entry.sourceExplanation)
                }
            }
        },
    )
}

@Composable
private fun TrackInsightsPage(
    projection: TrackProjection,
    customUnits: List<UnitDefinition>,
    today: LocalDate,
    dialogModifier: Modifier = Modifier,
) {
    var filterOpen by rememberSaveable(projection.track.id) { mutableStateOf(false) }
    var conditions by remember(projection.track.id) { mutableStateOf<List<TrackCondition>>(emptyList()) }
    var conditionMode by rememberSaveable(projection.track.id) { mutableStateOf(TrackConditionMode.MatchAll) }
    val scoped = projection.copy(entries = projection.matchingEntries(conditions, conditionMode))
    val dates = scoped.entries.map { it.entry.entryDate }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 16.dp, 20.dp, WhipSpacing.screenExpanded),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            WhipPageHeader("Insights", "Clear summaries of recorded evidence—never a productivity score.") {
                WhipPageIconAction(Icons.Outlined.FilterAlt, "Filter Insights", { filterOpen = true }, badgeCount = conditions.size, active = conditions.isNotEmpty())
            }
        }
        if (conditions.isNotEmpty()) item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                conditions.forEachIndexed { index, condition ->
                    WhipFilterChip(
                        selected = true,
                        onClick = { conditions = conditions.toMutableList().also { it.removeAt(index) } },
                        label = { Text("${projection.conditionFieldName(condition)} ${condition.operator.uiLabel()} ×") },
                    )
                }
                WhipTextButton(onClick = { conditions = emptyList() }) { Text("Clear All") }
            }
        }
        item {
            InsightCard(
                if (conditions.isEmpty()) "All Entries" else "Matching Entries",
                listOf(
                    "Total" to scoped.entries.size.toString(),
                    "First" to (dates.minOrNull()?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: "—"),
                    "Latest" to (dates.maxOrNull()?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: "—"),
                    "Last 7 Days" to scoped.entries.count { it.entry.entryDate >= today.minusDays(6) }.toString(),
                    "Last 30 Days" to scoped.entries.count { it.entry.entryDate >= today.minusDays(29) }.toString(),
                    "Last 90 Days" to scoped.entries.count { it.entry.entryDate >= today.minusDays(89) }.toString(),
                    "Recent Weekly Rate" to "${(scoped.entries.count { it.entry.entryDate >= today.minusDays(29) } / 30.0 * 7.0).formatCompact()} Entries",
                ),
            )
        }
        items(scoped.fields.filterNot(TrackField::primary), key = { "insight-field-${it.id}" }) { field ->
            val values = scoped.entries.mapNotNull { it.value(field.id) }
            val lines = when (field.type) {
                TrackFieldType.Number -> {
                    val nums = values.mapNotNull { it.canonicalNumber }
                    val unit = (BuiltInUnits.all + customUnits).firstOrNull { it.id == field.unitId }
                    val dated = scoped.entries.mapNotNull { entry -> entry.value(field.id)?.canonicalNumber?.let { entry to it } }
                        .sortedWith(compareBy<Pair<TrackEntryProjection, Double>> { it.first.entry.entryDate }.thenBy { it.first.entry.createdAtMillis })
                    fun display(value: Double?): String = value?.let { canonical ->
                        val number = unit?.fromCanonical(canonical) ?: canonical
                        number.formatForField(field.precision) + unit?.symbol?.takeIf(String::isNotBlank)?.let { " $it" }.orEmpty()
                    } ?: "—"
                    val change = dated.takeIf { it.size >= 2 }?.let { rows -> rows.last().second - rows.first().second }
                    val changeDisplay = change?.let { canonicalDelta ->
                        val delta = unit?.let { canonicalDelta / it.toCanonicalFactor } ?: canonicalDelta
                        val arrow = if (delta > 0) "↑" else if (delta < 0) "↓" else "→"
                        "$arrow ${kotlin.math.abs(delta).formatForField(field.precision)}${unit?.symbol?.takeIf(String::isNotBlank)?.let { " $it" }.orEmpty()}"
                    } ?: "—"
                    listOf(
                        "Recorded" to nums.size.toString(),
                        "Sum" to display(nums.takeIf { it.isNotEmpty() }?.sum()),
                        "Average" to display(nums.takeIf { it.isNotEmpty() }?.average()),
                        "Minimum" to display(nums.minOrNull()),
                        "Maximum" to display(nums.maxOrNull()),
                        "Latest" to display(dated.lastOrNull()?.second),
                        "First-to-Latest Trend" to changeDisplay,
                    )
                }
                TrackFieldType.Scale -> {
                    val nums = values.mapNotNull { it.scaleValue }
                    val dated = scoped.entries.mapNotNull { entry -> entry.value(field.id)?.scaleValue?.let { entry to it } }
                        .sortedWith(compareBy<Pair<TrackEntryProjection, Double>> { it.first.entry.entryDate }.thenBy { it.first.entry.createdAtMillis })
                    val change = dated.takeIf { it.size >= 2 }?.let { it.last().second - it.first().second }
                    listOf(
                        "Recorded" to nums.size.toString(),
                        "Average" to nums.averageOrDash(),
                        "Minimum" to nums.minOrNull().formatOrDash(),
                        "Maximum" to nums.maxOrNull().formatOrDash(),
                        "Latest" to dated.lastOrNull()?.second.formatOrDash(),
                        "First-to-Latest Trend" to (change?.let { "${if (it > 0) "↑" else if (it < 0) "↓" else "→"} ${kotlin.math.abs(it).formatCompact()}" } ?: "—"),
                    )
                }
                TrackFieldType.SingleChoice -> scoped.optionsFor(field.id).map { option ->
                    val count = values.count { it.choiceOptionId == option.id }
                    option.label to count.withPercentage(scoped.entries.size)
                } + ("Unanswered" to (scoped.entries.size - values.size).withPercentage(scoped.entries.size))
                TrackFieldType.YesNo -> listOf(
                    "Yes" to values.count { it.booleanValue == true }.withPercentage(scoped.entries.size),
                    "No" to values.count { it.booleanValue == false }.withPercentage(scoped.entries.size),
                    "Unanswered" to (scoped.entries.size - values.size).withPercentage(scoped.entries.size),
                )
                TrackFieldType.Date -> {
                    val recordedDates = values.mapNotNull { it.dateValue }
                    val currentMonth = YearMonth.from(today)
                    listOf(
                        "Recorded" to recordedDates.size.toString(),
                        "Earliest" to (recordedDates.minOrNull()?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: "—"),
                        "Latest" to (recordedDates.maxOrNull()?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: "—"),
                    ) + (5 downTo 0).map { monthsAgo ->
                        val month = currentMonth.minusMonths(monthsAgo.toLong())
                        month.format(DateTimeFormatter.ofPattern("MMM yyyy")) to recordedDates.count { YearMonth.from(it) == month }.toString()
                    }
                }
                TrackFieldType.ShortText, TrackFieldType.LongText -> listOf("Completed" to values.count { !it.textValue.isNullOrBlank() }.toString(), "Blank" to (scoped.entries.size - values.size).toString())
            }
            InsightCard(field.name, lines)
        }
    }
    if (filterOpen) TrackFilterDialog(
        modifier = dialogModifier,
        projection = projection,
        initial = conditions,
        initialMode = conditionMode,
        today = today,
        units = BuiltInUnits.all + customUnits,
        onDismiss = { filterOpen = false },
        onApply = { mode, updated -> conditionMode = mode; conditions = updated; filterOpen = false },
    )
}

@Composable
private fun InsightCard(title: String, lines: List<Pair<String, String>>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            lines.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth()) { Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

private enum class TrackAutomationKind { CaptureIntoTrack, FollowUpFromTrack }

@Composable
private fun TrackAutomationsOverview(
    projection: TrackProjection,
    state: TrackUiState,
    captureRules: List<TriggerRule>,
    followUpRules: List<TriggerRule>,
    progressRules: List<LinkRule>,
    pendingPrompts: List<TriggerOccurrence>,
    customUnits: List<UnitDefinition>,
    onSetGoal: () -> Unit,
    onConnectGoal: () -> Unit,
    viewModel: TrackViewModel,
    onOpenPrompt: (Long) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    saving: Boolean,
    today: LocalDate,
) {
    var creatingKind by rememberSaveable(projection.track.id) { mutableStateOf<TrackAutomationKind?>(null) }
    var editingRuleId by rememberSaveable(projection.track.id) { mutableStateOf<Long?>(null) }
    var editingProgressRuleId by rememberSaveable(projection.track.id) { mutableStateOf<Long?>(null) }
    var removingTriggerRuleId by rememberSaveable(projection.track.id) { mutableStateOf<Long?>(null) }
    var removingProgressRuleId by rememberSaveable(projection.track.id) { mutableStateOf<Long?>(null) }
    val editingRule = editingRuleId?.let { id -> state.triggerRules.firstOrNull { it.id == id } }
    val connectableGoals = projection.compatibleAutomationGoals(state.goals)
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 16.dp, 20.dp, WhipSpacing.screenExpanded),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { WhipPageHeader("Automations", "Choose how Entries update Goals or prompt a next action.") }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Goal Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "The common setup is simple: each eligible Entry adds 1 toward a Goal. You can optionally use conditions or a Number/Scale Field instead.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    WhipButton(
                        onClick = onSetGoal,
                        modifier = Modifier.fillMaxWidth().testTag("track-automation-create-goal"),
                    ) {
                        Icon(Icons.Outlined.Insights, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create a Goal From Entries")
                    }
                    WhipOutlinedButton(
                        enabled = connectableGoals.isNotEmpty(),
                        onClick = onConnectGoal,
                        modifier = Modifier.fillMaxWidth().testTag("track-automation-connect-goal"),
                    ) { Text("Connect Entries to an Existing Goal") }
                    if (connectableGoals.isEmpty()) Text(
                        "Create a compatible active or paused Goal, or add a matching Number/Scale Field to this Track.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            ProgressAutomationGroup(
                rules = progressRules,
                state = state,
                projection = projection,
                onEdit = { editingProgressRuleId = it.id },
                onToggle = { viewModel.setProgressAutomationEnabled(it.id, !it.enabled) },
                onDelete = { removingProgressRuleId = it.id },
            )
        }
        if (pendingPrompts.isNotEmpty()) {
            item { Text("Ready to Capture", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(pendingPrompts, key = { "automation-prompt-${it.id}" }) { occurrence ->
                TrackPromptCard(
                    occurrence,
                    captureRules.firstOrNull { it.id == occurrence.triggerRuleId },
                    { onOpenPrompt(occurrence.id) },
                    { viewModel.remindPrompt(occurrence.id, Instant.now().plus(1, ChronoUnit.HOURS)) },
                    { viewModel.dismissPrompt(occurrence.id) },
                )
            }
        }
        item {
            AutomationGroup(
                "Prompt a New Entry",
                "After a Task, Subtask, or Habit event, show a durable prompt to review and add an Entry to this Track.",
                rules = captureRules,
                state = state,
                onAdd = { creatingKind = TrackAutomationKind.CaptureIntoTrack },
                onEdit = { editingRuleId = it.id },
                onToggle = { rule -> viewModel.updateTrigger(rule.id, rule.toDraft(enabled = !rule.enabled)) },
                onDelete = { removingTriggerRuleId = it.id },
            )
        }
        item {
            AutomationGroup(
                "Continue After an Entry",
                "After an Entry matches your conditions, prompt a Task or Habit—or explicitly Check Off a compatible Habit.",
                rules = followUpRules,
                state = state,
                onAdd = { creatingKind = TrackAutomationKind.FollowUpFromTrack },
                onEdit = { editingRuleId = it.id },
                onToggle = { rule -> viewModel.updateTrigger(rule.id, rule.toDraft(enabled = !rule.enabled)) },
                onDelete = { removingTriggerRuleId = it.id },
            )
        }
    }
    val editorKind = creatingKind ?: editingRule?.let {
        if (it.targetType == TriggerTargetType.Track) TrackAutomationKind.CaptureIntoTrack else TrackAutomationKind.FollowUpFromTrack
    }
    if (editorKind != null) TrackAutomationEditorDialog(
        kind = editorKind,
        projection = projection,
        state = state,
        customUnits = customUnits,
        initialRule = editingRule,
        today = today,
        onDismiss = { creatingKind = null; editingRuleId = null },
        onSave = { draft ->
            if (draft.notificationEnabled) onRequestNotificationPermission()
            val closeEditor = { creatingKind = null; editingRuleId = null }
            if (editingRule == null) viewModel.createTrigger(draft, closeEditor)
            else viewModel.updateTrigger(editingRule.id, draft, closeEditor)
        },
        saving = saving,
    )
    editingProgressRuleId?.let { id ->
        progressRules.firstOrNull { it.id == id }?.let { rule ->
            TrackProgressAutomationEditorDialog(
                projection = projection,
                rule = rule,
                goal = state.goals.firstOrNull { it.id == rule.targetGoalId },
                customUnits = customUnits,
                onDismiss = { editingProgressRuleId = null },
                onSave = { draft -> viewModel.updateProgressAutomation(rule.id, draft) { editingProgressRuleId = null } },
                saving = saving,
                today = today,
            )
        }
    }
    removingTriggerRuleId?.let { ruleId ->
        val rule = state.triggerRules.firstOrNull { it.id == ruleId }
        RemoveAutomationConfirmationDialog(
            automationName = rule?.name ?: "This Automation",
            onDismiss = { removingTriggerRuleId = null },
            onConfirm = {
                viewModel.deleteTrigger(ruleId)
                removingTriggerRuleId = null
            },
        )
    }
    removingProgressRuleId?.let { ruleId ->
        val rule = progressRules.firstOrNull { it.id == ruleId }
        RemoveAutomationConfirmationDialog(
            automationName = rule?.name ?: "This Goal Automation",
            onDismiss = { removingProgressRuleId = null },
            onConfirm = {
                viewModel.deleteProgressAutomation(ruleId)
                removingProgressRuleId = null
            },
        )
    }
}

@Composable
private fun ProgressAutomationGroup(
    rules: List<LinkRule>,
    state: TrackUiState,
    projection: TrackProjection,
    onEdit: (LinkRule) -> Unit,
    onToggle: (LinkRule) -> Unit,
    onDelete: (LinkRule) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Goals Using This Track", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Goal Automations keep one auditable contribution per eligible Entry.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (rules.isEmpty()) Text("No Goal Automations Yet", style = MaterialTheme.typography.labelLarge)
            rules.forEach { rule ->
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                val target = state.goals.firstOrNull { it.id == rule.targetGoalId }?.name ?: "Goal"
                Text("${rule.trackAggregation?.normalizedAutomationMeasure()?.automationLabel() ?: "Count Entries"} contributes to $target.", fontWeight = FontWeight.Medium)
                Text(if (rule.enabled) "Enabled" else "Paused", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val inHistory = projection.entries.filter { entry ->
                    rule.retroactiveFrom?.let { !entry.entry.entryDate.isBefore(it) }
                        ?: (entry.entry.createdAtMillis >= rule.createdAtMillis)
                }
                val matching = projection.copy(entries = inHistory).matchingEntries(rule.conditions, rule.conditionMode)
                val eligible = if (rule.trackAggregation in setOf(TrackAggregation.Sum, TrackAggregation.Average, TrackAggregation.Latest, TrackAggregation.Minimum, TrackAggregation.Maximum)) {
                    matching.count { entry -> rule.sourceFieldId?.let { entry.value(it)?.let { value -> value.canonicalNumber != null || value.scaleValue != null } } == true }
                } else matching.size
                val contributions = state.contributions.filter { it.linkRuleId == rule.id }
                val excluded = contributions.count { it.excluded }
                Text(
                    "$eligible eligible · ${inHistory.size - eligible} skipped · ${contributions.size} current contribution${if (contributions.size == 1) "" else "s"}${if (excluded > 0) " · $excluded excluded" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    WhipTextButton(onClick = { onToggle(rule) }) { Text(if (rule.enabled) "Pause" else "Resume") }
                    WhipTextButton(onClick = { onEdit(rule) }) { Text("Edit") }
                    WhipTextButton(onClick = { onDelete(rule) }) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackProgressAutomationEditorDialog(
    projection: TrackProjection,
    rule: LinkRule,
    goal: Goal?,
    customUnits: List<UnitDefinition>,
    onDismiss: () -> Unit,
    onSave: (LinkRuleDraft) -> Unit,
    saving: Boolean,
    today: LocalDate,
) {
    var name by rememberSaveable(rule.id) { mutableStateOf(rule.name) }
    var aggregation by rememberSaveable(rule.id) {
        mutableStateOf((rule.trackAggregation ?: TrackAggregation.CountEntries).normalizedAutomationMeasure())
    }
    var sourceFieldId by rememberSaveable(rule.id) { mutableStateOf(rule.sourceFieldId) }
    var fixedValue by rememberSaveable(rule.id) { mutableStateOf(rule.fixedValue?.let(::editableNumericValue) ?: "1") }
    var multiplier by rememberSaveable(rule.id) { mutableStateOf(editableNumericValue(rule.multiplier)) }
    var offset by rememberSaveable(rule.id) { mutableStateOf(editableNumericValue(rule.offset)) }
    var conditionMode by rememberSaveable(rule.id) { mutableStateOf(rule.conditionMode) }
    var conditions by remember(rule.id) { mutableStateOf(rule.conditions) }
    var addingCondition by rememberSaveable(rule.id) { mutableStateOf(false) }
    var includeHistory by rememberSaveable(rule.id) { mutableStateOf(rule.retroactiveFrom != null) }
    var historyDate by rememberSaveable(rule.id) { mutableStateOf(rule.retroactiveFrom ?: goal?.startDate ?: today) }
    var pickingDate by rememberSaveable(rule.id) { mutableStateOf(false) }
    var showAdvanced by rememberSaveable(rule.id) {
        mutableStateOf(rule.multiplier != 1.0 || rule.offset != 0.0)
    }
    val compatibleMeasures = goal?.compatibleTrackAutomationMeasures() ?: listOf(aggregation)
    val needsField = aggregation.needsTrackNumberField()
    val numericFields = projection.fields.filter { field ->
        field.type == TrackFieldType.Scale && goal?.dimension == UnitDimension.Unitless ||
            field.type == TrackFieldType.Number && field.dimension == goal?.dimension
    }
    val selectedField = numericFields.firstOrNull { it.id == sourceFieldId } ?: numericFields.firstOrNull()
    val previewEntries = if (includeHistory) {
        projection.entries.filter { !it.entry.entryDate.isBefore(historyDate) }
    } else {
        projection.entries.filter { it.entry.createdAtMillis >= rule.createdAtMillis }
    }
    val preview = projection.copy(entries = previewEntries)
        .aggregate(aggregation, selectedField?.uuid, conditions, conditionMode, fixedValue.toWhipDoubleOrNull())
    val valid = name.isNotBlank() && (!needsField || selectedField != null) &&
        (aggregation != TrackAggregation.FixedAmount || fixedValue.toWhipDoubleOrNull() != null) &&
        multiplier.toWhipDoubleOrNull() != null && offset.toWhipDoubleOrNull() != null
    PaneAwareAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Goal Automation") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text("Entries from ${projection.track.name} contribute to ${goal?.name ?: "this Goal"}.") }
                item { SelectionField("How Entries Add Progress", compatibleMeasures, aggregation, TrackAggregation::automationLabel, { aggregation = it }) }
                item { Text(aggregation.automationExplanation(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (needsField) item { SelectionField("Number or Scale Field", numericFields, selectedField, { it?.name ?: "No Compatible Fields" }, { sourceFieldId = it?.id }) }
                if (aggregation == TrackAggregation.FixedAmount) item { TrackNumberField(fixedValue, { fixedValue = it }, "Amount per Matching Entry") }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Include Entries Where", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (conditions.isEmpty()) Text("All Entries", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        conditions.forEachIndexed { index, condition ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("${projection.conditionFieldName(condition)} ${condition.operator.uiLabel()} ${condition.summaryValue(projection, BuiltInUnits.all + customUnits)}".trim(), Modifier.weight(1f))
                                WhipTextButton(onClick = { conditions = conditions.toMutableList().also { it.removeAt(index) } }) { Text("Remove") }
                            }
                        }
                        if (conditions.size > 1) SegmentedChoiceBar(conditionMode, TrackConditionMode.entries, { conditionMode = it }, { if (it == TrackConditionMode.MatchAll) "Match All" else "Match Any" }, Modifier.fillMaxWidth())
                        WhipOutlinedButton(onClick = { addingCondition = true }, modifier = Modifier.fillMaxWidth()) { Text("Add Condition") }
                    }
                }
                item { TrackToggleRow("Include Earlier Entries", "Choose the earliest Entry Date included in this Automation.", includeHistory, { includeHistory = it }) }
                if (includeHistory) item { WhipOutlinedButton(onClick = { pickingDate = true }, modifier = Modifier.fillMaxWidth()) { Text("Since ${historyDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}") } }
                if (includeHistory && goal != null && historyDate.isBefore(goal.startDate)) item {
                    Text(
                        "Saving will move ${goal.name}'s start to ${historyDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))} so every included contribution counts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item { Text("Preview: ${preview.eligibleEntryCount} eligible · ${preview.skippedEntryCount} skipped · ${preview.value?.let(::editableNumericValue) ?: "no result"}") }
                item {
                    DisclosureButton(
                        label = "Advanced Automation Options",
                        expanded = showAdvanced,
                        onClick = { showAdvanced = !showAdvanced },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (showAdvanced) {
                    item { OutlinedTextField(name, { name = it.replace('\n', ' ').replace('\r', ' ').take(100) }, label = { Text("Automation Name") }, supportingText = { Text("${name.length}/100") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                    item { Text("Multiplier and offset are applied after unit conversion. Leave them at 1 and 0 to preserve the source value.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    item { ResponsiveFieldPair(
                        first = { modifier -> OutlinedTextField(multiplier, { multiplier = it }, label = { Text("Multiplier") }, modifier = modifier) },
                        second = { modifier -> OutlinedTextField(offset, { offset = it }, label = { Text("Offset") }, modifier = modifier) },
                    ) }
                }
            }
        },
        confirmButton = { WhipTextButton(enabled = valid && !saving, onClick = {
            onSave(
                LinkRuleDraft(
                    name = name,
                    kind = rule.kind,
                    sourceType = LinkSourceType.Track,
                    sourceEntityId = projection.track.id,
                    sourceMetric = aggregation.progressSourceMetric(),
                    targetGoalId = rule.targetGoalId,
                    targetMilestoneId = rule.targetMilestoneId,
                    valueMode = if (aggregation == TrackAggregation.FixedAmount) LinkValueMode.FixedValue else LinkValueMode.SourceValue,
                    fixedValue = fixedValue.toWhipDoubleOrNull(),
                    multiplier = requireNotNull(multiplier.toWhipDoubleOrNull()),
                    offset = requireNotNull(offset.toWhipDoubleOrNull()),
                    retroactiveFrom = historyDate.takeIf { includeHistory },
                    enabled = rule.enabled,
                    trackAggregation = aggregation,
                    sourceFieldId = selectedField?.id.takeIf { needsField },
                    conditionMode = conditionMode,
                    conditions = conditions,
                ),
            )
        }) { Text(if (saving) "Saving…" else "Save Automation") } },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (addingCondition) TrackConditionEditor(projection, { addingCondition = false }, today, BuiltInUnits.all + customUnits) { conditions = conditions + it; addingCondition = false }
    if (pickingDate) WhipDatePickerDialog(historyDate, { pickingDate = false }, { historyDate = it; pickingDate = false })
}

@Composable
private fun AutomationGroup(
    title: String,
    explanation: String,
    rules: List<TriggerRule>,
    state: TrackUiState,
    onAdd: () -> Unit,
    onEdit: (TriggerRule) -> Unit,
    onToggle: (TriggerRule) -> Unit,
    onDelete: (TriggerRule) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            WhipTextButton(onClick = onAdd) { Text("Add") }
        }
        Text(explanation, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (rules.isEmpty()) Text("No Automations Yet", style = MaterialTheme.typography.labelLarge)
        rules.forEach { rule ->
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Text(rule.triggerSentence(state), fontWeight = FontWeight.Medium)
            val staleReason = rule.staleReason(state)
            if (staleReason != null) {
                Text(
                    "$staleReason Choose Edit to repair this Automation, or Remove to delete it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                listOfNotNull(
                    if (rule.enabled) "Enabled" else "Paused",
                    "${rule.delayMinutes} minute delay",
                    "Notifications on".takeIf { rule.notificationEnabled },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val occurrences = state.triggerOccurrences.filter { it.triggerRuleId == rule.id }
            val fulfilled = occurrences.count { it.fulfilledEntryId != null }
            val dismissed = occurrences.count { it.dismissedAt != null }
            val pending = occurrences.size - fulfilled - dismissed
            Text(
                if (rule.action == TriggerAction.CheckOffHabit) {
                    "${occurrences.size} matched source event${if (occurrences.size == 1) "" else "s"} · automatically applied idempotently"
                } else {
                    "${occurrences.size} matched source event${if (occurrences.size == 1) "" else "s"} · $pending pending · $fulfilled fulfilled · $dismissed dismissed"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                WhipTextButton(enabled = staleReason == null, onClick = { onToggle(rule) }) { Text(if (rule.enabled) "Pause" else "Resume") }
                WhipTextButton(onClick = { onEdit(rule) }) { Text("Edit") }
                WhipTextButton(onClick = { onDelete(rule) }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    } }
}

@Composable
private fun TrackAutomationEditorDialog(
    kind: TrackAutomationKind,
    projection: TrackProjection,
    state: TrackUiState,
    customUnits: List<UnitDefinition>,
    initialRule: TriggerRule?,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (TriggerRuleDraft) -> Unit,
    saving: Boolean,
) {
    val key = "track-automation-${initialRule?.id ?: kind.name}"
    var name by rememberSaveable(key) { mutableStateOf(initialRule?.name.orEmpty()) }
    var sourceType by rememberSaveable(key) { mutableStateOf(initialRule?.sourceType?.takeIf { kind == TrackAutomationKind.CaptureIntoTrack } ?: LinkSourceType.Task) }
    var sourceTaskId by rememberSaveable(key) { mutableStateOf(initialRule?.sourceEntityId?.takeIf { initialRule.sourceType in setOf(LinkSourceType.Task, LinkSourceType.Subtask) } ?: state.sourceTasks.firstOrNull()?.id) }
    var sourceStepId by rememberSaveable(key) { mutableStateOf(initialRule?.sourceItemId) }
    var sourceHabitId by rememberSaveable(key) { mutableStateOf(initialRule?.sourceEntityId?.takeIf { initialRule.sourceType == LinkSourceType.Habit } ?: state.sourceHabits.firstOrNull()?.id) }
    var outcome by rememberSaveable(key) { mutableStateOf(initialRule?.outcome ?: TriggerOutcome.Completed) }
    var targetType by rememberSaveable(key) { mutableStateOf(initialRule?.targetType?.takeIf { kind == TrackAutomationKind.FollowUpFromTrack } ?: TriggerTargetType.Task) }
    var targetTaskId by rememberSaveable(key) { mutableStateOf(initialRule?.targetEntityId?.takeIf { initialRule.targetType == TriggerTargetType.Task } ?: state.sourceTasks.firstOrNull()?.id) }
    var targetHabitId by rememberSaveable(key) { mutableStateOf(initialRule?.targetEntityId?.takeIf { initialRule.targetType == TriggerTargetType.Habit } ?: state.sourceHabits.firstOrNull()?.id) }
    var action by rememberSaveable(key) { mutableStateOf(initialRule?.action ?: TriggerAction.PromptTask) }
    var delayMinutes by rememberSaveable(key) { mutableIntStateOf(initialRule?.delayMinutes ?: 0) }
    var quietEnabled by rememberSaveable(key) { mutableStateOf(initialRule?.quietStartMinutes != null && initialRule.quietEndMinutes != null) }
    var quietStart by rememberSaveable(key) { mutableIntStateOf(initialRule?.quietStartMinutes ?: 22 * 60) }
    var quietEnd by rememberSaveable(key) { mutableIntStateOf(initialRule?.quietEndMinutes ?: 7 * 60) }
    var notificationEnabled by rememberSaveable(key) { mutableStateOf(initialRule?.notificationEnabled ?: false) }
    var conditionMode by rememberSaveable(key) { mutableStateOf(initialRule?.conditionMode ?: TrackConditionMode.MatchAll) }
    var conditions by remember(key) { mutableStateOf(initialRule?.conditions.orEmpty()) }
    var addingCondition by rememberSaveable(key) { mutableStateOf(false) }
    var mappings by remember(key) {
        mutableStateOf(initialRule?.mappings.orEmpty().associate { it.targetFieldId to it.sourceProperty })
    }
    var constants by remember(key) {
        mutableStateOf(initialRule?.mappings.orEmpty().mapNotNull { mapping -> mapping.constantValue?.let { mapping.targetFieldId to it } }.toMap())
    }
    val selectedHabit = state.sourceHabits.firstOrNull { it.id == targetHabitId }
    val actions = when (targetType) {
        TriggerTargetType.Task -> listOf(TriggerAction.PromptTask)
        TriggerTargetType.Habit -> buildList {
            add(TriggerAction.PromptHabit)
            if (selectedHabit?.trackingMode == HabitTrackingMode.CheckOff) add(TriggerAction.CheckOffHabit)
        }
        TriggerTargetType.Track -> listOf(TriggerAction.PromptTrackEntry)
    }
    LaunchedEffect(targetType, targetHabitId) { if (action !in actions) action = actions.first() }
    val sourceOutcomes = when (sourceType) {
        LinkSourceType.Task -> listOf(TriggerOutcome.Completed, TriggerOutcome.Skipped)
        LinkSourceType.Subtask -> listOf(TriggerOutcome.Completed)
        LinkSourceType.Habit -> listOf(TriggerOutcome.Recorded, TriggerOutcome.Completed, TriggerOutcome.Failed, TriggerOutcome.Skipped)
        else -> listOf(TriggerOutcome.Completed)
    }
    LaunchedEffect(sourceType) { if (outcome !in sourceOutcomes) outcome = sourceOutcomes.first() }
    val sourceId = if (kind == TrackAutomationKind.FollowUpFromTrack) projection.track.id else when (sourceType) {
        LinkSourceType.Task, LinkSourceType.Subtask -> sourceTaskId
        LinkSourceType.Habit -> sourceHabitId
        else -> null
    }
    val targetId = if (kind == TrackAutomationKind.CaptureIntoTrack) projection.track.id else when (targetType) {
        TriggerTargetType.Task -> targetTaskId
        TriggerTargetType.Habit -> targetHabitId
        TriggerTargetType.Track -> null
    }
    val effectiveSourceType = if (kind == TrackAutomationKind.FollowUpFromTrack) LinkSourceType.Track else sourceType
    val effectiveTargetType = if (kind == TrackAutomationKind.CaptureIntoTrack) TriggerTargetType.Track else targetType
    val effectiveAction = if (kind == TrackAutomationKind.CaptureIntoTrack) TriggerAction.PromptTrackEntry else action
    val valid = name.isNotBlank() && sourceId != null && targetId != null &&
        (kind != TrackAutomationKind.CaptureIntoTrack || sourceType != LinkSourceType.Subtask ||
            state.sourceTaskSteps.any { it.id == sourceStepId && it.taskId == sourceTaskId })
    PaneAwareAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialRule == null) "Create Automation" else "Edit Automation") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { OutlinedTextField(name, { name = it.replace('\n', ' ').replace('\r', ' ').take(100) }, label = { Text("Automation Name") }, supportingText = { Text("${name.length}/100") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                if (kind == TrackAutomationKind.CaptureIntoTrack) {
                    item { Text("When This Happens", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                    item {
                        SelectionField(
                            "Source",
                            listOf(LinkSourceType.Task, LinkSourceType.Subtask, LinkSourceType.Habit),
                            sourceType,
                            { type -> when (type) { LinkSourceType.Task -> "Task"; LinkSourceType.Subtask -> "Subtask"; else -> "Habit" } },
                            { selected -> sourceType = selected; sourceStepId = null; outcome = if (selected == LinkSourceType.Habit) TriggerOutcome.Recorded else TriggerOutcome.Completed },
                        )
                    }
                    if (sourceType in setOf(LinkSourceType.Task, LinkSourceType.Subtask)) item {
                        SelectionField(
                            "Task",
                            state.sourceTasks,
                            state.sourceTasks.firstOrNull { it.id == sourceTaskId },
                            { it?.title ?: "No Tasks Available" },
                            { sourceTaskId = it?.id; sourceStepId = null },
                        )
                    }
                    if (sourceType == LinkSourceType.Subtask) {
                        val steps = state.sourceTaskSteps.filter { it.taskId == sourceTaskId }
                        item {
                            SelectionField(
                                "Subtask",
                                steps,
                                steps.firstOrNull { it.id == sourceStepId },
                                { it?.title ?: "No Subtasks Available" },
                                { sourceStepId = it?.id },
                            )
                        }
                    }
                    if (sourceType == LinkSourceType.Habit) item { SelectionField("Habit", state.sourceHabits, state.sourceHabits.firstOrNull { it.id == sourceHabitId }, { it?.name ?: "No Habits Available" }, { sourceHabitId = it?.id }) }
                    item { SelectionField("Outcome", sourceOutcomes, outcome, TriggerOutcome::automationLabel, { outcome = it }) }
                    if (sourceType == LinkSourceType.Habit) item {
                        Text(
                            "Recorded means any saved Habit result. Completed means the Habit reached its target.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    item { Text("Then Prompt to Add ${projection.primaryField.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                    item { Text("Whip can prefill Fields, but it never saves a structured Entry until the user confirms it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(projection.fields, key = { "mapping-${it.id}" }) { field ->
                        val options = field.allowedSourceProperties()
                        val selected = mappings[field.id]
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SelectionField(
                                    "Prefill ${field.name}",
                                    listOf<TriggerSourceProperty?>(null) + options,
                                    selected,
                                    { it?.automationLabel() ?: "Do Not Prefill" },
                                    { chosen -> mappings = if (chosen == null) mappings - field.id else mappings + (field.id to chosen) },
                                )
                                if (selected == TriggerSourceProperty.Constant) {
                                    TrackEntryField(
                                        field = field,
                                        value = constants[field.id] ?: TrackValueDraft(enteredUnitId = field.unitId),
                                        options = projection.optionsFor(field.id),
                                        units = BuiltInUnits.all + customUnits,
                                        today = today,
                                        showError = false,
                                    ) { constants = constants + (field.id to it) }
                                }
                            }
                        }
                    }
                } else {
                    item { Text("When an Entry Matches", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (conditions.isEmpty()) Text("Any ${projection.track.name} Entry", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            conditions.forEachIndexed { index, condition ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text("${projection.conditionFieldName(condition)} ${condition.operator.uiLabel()} ${condition.summaryValue(projection, BuiltInUnits.all + customUnits)}".trim(), Modifier.weight(1f))
                                    WhipTextButton(onClick = { conditions = conditions.toMutableList().also { it.removeAt(index) } }) { Text("Remove") }
                                }
                            }
                            if (conditions.size > 1) SegmentedChoiceBar(conditionMode, TrackConditionMode.entries, { conditionMode = it }, { if (it == TrackConditionMode.MatchAll) "Match All" else "Match Any" }, Modifier.fillMaxWidth())
                            WhipOutlinedButton(onClick = { addingCondition = true }, modifier = Modifier.fillMaxWidth()) { Text("Add Condition") }
                        }
                    }
                    item { Text("Then", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                    item { SelectionField("Target", listOf(TriggerTargetType.Task, TriggerTargetType.Habit), targetType, { if (it == TriggerTargetType.Task) "Task" else "Habit" }, { targetType = it }) }
                    if (targetType == TriggerTargetType.Task) item { SelectionField("Task", state.sourceTasks, state.sourceTasks.firstOrNull { it.id == targetTaskId }, { it?.title ?: "No Tasks Available" }, { targetTaskId = it?.id }) }
                    if (targetType == TriggerTargetType.Habit) item { SelectionField("Habit", state.sourceHabits, selectedHabit, { it?.name ?: "No Habits Available" }, { targetHabitId = it?.id }) }
                    item { SelectionField("Action", actions, action, TriggerAction::automationLabel, { action = it }) }
                    if (action == TriggerAction.CheckOffHabit) item { Text("This consequence automatically Checks Off the Habit. Every other action creates a prompt.", style = MaterialTheme.typography.bodySmall) }
                }
                item { SelectionField("Delay", listOf(0, 5, 15, 30, 60, 1440), delayMinutes, ::delayLabel, { delayMinutes = it }) }
                item { TrackToggleRow("Quiet Hours", "Hold optional notifications until the quiet period ends.", quietEnabled, { quietEnabled = it }) }
                if (quietEnabled) item {
                    ResponsiveFieldPair(
                        first = { modifier -> ClockPickerButton("Quiet Hours Start", quietStart, { quietStart = it ?: quietStart }, modifier) },
                        second = { modifier -> ClockPickerButton("Quiet Hours End", quietEnd, { quietEnd = it ?: quietEnd }, modifier) },
                    )
                }
                item { TrackToggleRow("Notification", "The prompt always remains in Whip. Enable this only if an Android notification is also useful.", notificationEnabled, { notificationEnabled = it }) }
            }
        },
        confirmButton = { WhipTextButton(enabled = valid && !saving, onClick = {
            onSave(
                TriggerRuleDraft(
                    name = name,
                    sourceType = effectiveSourceType,
                    sourceEntityId = requireNotNull(sourceId),
                    sourceItemId = sourceStepId.takeIf { kind == TrackAutomationKind.CaptureIntoTrack && sourceType == LinkSourceType.Subtask },
                    outcome = outcome,
                    targetType = effectiveTargetType,
                    targetEntityId = requireNotNull(targetId),
                    delayMinutes = delayMinutes,
                    quietStartMinutes = quietStart.takeIf { quietEnabled },
                    quietEndMinutes = quietEnd.takeIf { quietEnabled },
                    action = effectiveAction,
                    notificationEnabled = notificationEnabled,
                    enabled = initialRule?.enabled ?: true,
                    conditionMode = conditionMode,
                    conditions = conditions.takeIf { kind == TrackAutomationKind.FollowUpFromTrack }.orEmpty(),
                    mappings = if (kind == TrackAutomationKind.CaptureIntoTrack) mappings.map { (fieldId, property) ->
                        TriggerFieldMapping(fieldId, property, constants[fieldId].takeIf { property == TriggerSourceProperty.Constant })
                    } else emptyList(),
                ),
            )
        }) { Text(if (saving) "Saving…" else if (initialRule == null) "Create Automation" else "Save Automation") } },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (addingCondition) TrackConditionEditor(projection, { addingCondition = false }, today, BuiltInUnits.all + customUnits) { conditions = conditions + it; addingCondition = false }
}

private fun TrackField.allowedSourceProperties(): List<TriggerSourceProperty> = when (type) {
    TrackFieldType.ShortText, TrackFieldType.LongText -> listOf(TriggerSourceProperty.Title, TriggerSourceProperty.Name, TriggerSourceProperty.Notes, TriggerSourceProperty.Unit, TriggerSourceProperty.Outcome, TriggerSourceProperty.EventDate, TriggerSourceProperty.Constant)
    TrackFieldType.Number, TrackFieldType.Scale -> listOf(TriggerSourceProperty.NumericValue, TriggerSourceProperty.Constant)
    TrackFieldType.SingleChoice -> listOf(TriggerSourceProperty.Outcome, TriggerSourceProperty.Unit, TriggerSourceProperty.Constant)
    TrackFieldType.Date -> listOf(TriggerSourceProperty.EventDate, TriggerSourceProperty.Constant)
    TrackFieldType.YesNo -> listOf(TriggerSourceProperty.Outcome, TriggerSourceProperty.Constant)
}

private fun TriggerSourceProperty.automationLabel(): String = when (this) {
    TriggerSourceProperty.Title -> "Source Title"
    TriggerSourceProperty.Notes -> "Source Notes"
    TriggerSourceProperty.Name -> "Source Name"
    TriggerSourceProperty.NumericValue -> "Source Number"
    TriggerSourceProperty.Unit -> "Source Unit"
    TriggerSourceProperty.Outcome -> "Source Outcome"
    TriggerSourceProperty.EventDate -> "Event Date"
    TriggerSourceProperty.Constant -> "Constant Value"
}

private fun TriggerOutcome.automationLabel(): String = when (this) {
    TriggerOutcome.Completed -> "Completed"
    TriggerOutcome.Recorded -> "Recorded"
    TriggerOutcome.Failed -> "Failed"
    TriggerOutcome.Skipped -> "Skipped"
}

private fun TriggerAction.automationLabel(): String = when (this) {
    TriggerAction.PromptTask -> "Prompt to Open Task"
    TriggerAction.PromptHabit -> "Prompt to Open Habit"
    TriggerAction.CheckOffHabit -> "Automatically Check Off Habit"
    TriggerAction.PromptTrackEntry -> "Prompt to Add Track Entry"
}

private fun delayLabel(minutes: Int): String = when (minutes) {
    0 -> "Immediately"
    60 -> "1 Hour"
    1440 -> "1 Day"
    else -> "$minutes Minutes"
}

private fun TriggerRule.triggerSentence(state: TrackUiState): String {
    val source = when (sourceType) {
        LinkSourceType.Task -> state.sourceTasks.firstOrNull { it.id == sourceEntityId }?.title ?: "Task"
        LinkSourceType.Subtask -> state.sourceTaskSteps.firstOrNull { it.id == sourceItemId }?.title ?: "Subtask"
        LinkSourceType.Habit -> state.sourceHabits.firstOrNull { it.id == sourceEntityId }?.name ?: "Habit"
        LinkSourceType.Track -> state.track(sourceEntityId)?.track?.name ?: "Track"
        else -> sourceType.name
    }
    val target = when (targetType) {
        TriggerTargetType.Task -> state.sourceTasks.firstOrNull { it.id == targetEntityId }?.title ?: "Task"
        TriggerTargetType.Habit -> state.sourceHabits.firstOrNull { it.id == targetEntityId }?.name ?: "Habit"
        TriggerTargetType.Track -> state.track(targetEntityId)?.track?.name ?: "Track"
    }
    return "When $source is ${outcome.automationLabel().lowercase()}, ${action.automationLabel().replaceFirstChar(Char::lowercase)} for $target."
}

private fun TriggerRule.staleReason(state: TrackUiState): String? = when {
    sourceType == LinkSourceType.Subtask && state.sourceTasks.none { it.id == sourceEntityId } ->
        "The source Task is unavailable."
    sourceType == LinkSourceType.Subtask && state.sourceTaskSteps.none { it.id == sourceItemId && it.taskId == sourceEntityId } ->
        "The selected Subtask is unavailable."
    sourceType == LinkSourceType.Task && state.sourceTasks.none { it.id == sourceEntityId } ->
        "The source Task is unavailable."
    sourceType == LinkSourceType.Habit && state.sourceHabits.none { it.id == sourceEntityId } ->
        "The source Habit is unavailable."
    targetType == TriggerTargetType.Task && state.sourceTasks.none { it.id == targetEntityId } ->
        "The target Task is unavailable."
    targetType == TriggerTargetType.Habit && state.sourceHabits.none { it.id == targetEntityId } ->
        "The target Habit is unavailable."
    else -> null
}

private fun TriggerRule.toDraft(enabled: Boolean = this.enabled) = TriggerRuleDraft(
    name = name,
    sourceType = sourceType,
    sourceEntityId = sourceEntityId,
    sourceItemId = sourceItemId,
    outcome = outcome,
    targetType = targetType,
    targetEntityId = targetEntityId,
    delayMinutes = delayMinutes,
    quietStartMinutes = quietStartMinutes,
    quietEndMinutes = quietEndMinutes,
    action = action,
    notificationEnabled = notificationEnabled,
    enabled = enabled,
    conditionMode = conditionMode,
    conditions = conditions,
    mappings = mappings,
)

@Composable
private fun TrackCsvImportDialog(
    projection: TrackProjection,
    csv: String,
    customUnits: List<UnitDefinition>,
    saving: Boolean,
    today: LocalDate,
    onDismiss: () -> Unit,
    onImport: (TrackCsvImportPreview) -> Unit,
) {
    val headersResult = remember(csv) { runCatching { trackCsvHeaders(csv) } }
    val headers = headersResult.getOrDefault(emptyList())
    val defaultMapping = remember(headers, projection.track.id) {
        TrackCsvMapping(
            entryDateColumn = headers.firstOrNull { it.equals("Entry Date", true) },
            fieldColumns = projection.fields.mapNotNull { field ->
                headers.firstOrNull {
                    it.equals(field.name, true) || field.type == TrackFieldType.Number && it.equals("${field.name} (Entered)", true)
                }?.let { field.uuid to it }
            }.toMap(),
            numberUnitColumns = projection.fields.filter { it.type == TrackFieldType.Number }.mapNotNull { field ->
                headers.firstOrNull { it.equals("${field.name} (Unit)", true) }?.let { field.uuid to it }
            }.toMap(),
        )
    }
    var mapping by remember(headers, projection.track.id) { mutableStateOf(defaultMapping) }
    val previewResult = remember(csv, projection, mapping) {
        runCatching { previewTrackCsvImport(projection, csv, mapping, today, BuiltInUnits.all + customUnits) }
    }
    val preview = previewResult.getOrNull()
    val error = headersResult.exceptionOrNull()?.message ?: previewResult.exceptionOrNull()?.message
    val canImport = preview != null && preview.validRows > 0 && preview.invalidRows == 0
    PaneAwareAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import ${projection.track.name} Entries") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text("Map CSV columns to the current Fields. Import never changes the Track structure or creates Choice options silently.") }
                if (headers.isNotEmpty()) {
                    item { SelectionField("Entry Date", listOf<String?>(null) + headers, mapping.entryDateColumn, { it ?: "Use Today's Date" }, { mapping = mapping.copy(entryDateColumn = it) }) }
                    items(projection.fields, key = { "csv-field-${it.id}" }) { field ->
                        val selected = mapping.fieldColumns[field.uuid]
                        SelectionField(
                            field.name + if (field.primary) " · Primary" else "",
                            listOf<String?>(null) + headers,
                            selected,
                            { it ?: "Do Not Import" },
                            { column -> mapping = mapping.copy(fieldColumns = if (column == null) mapping.fieldColumns - field.uuid else mapping.fieldColumns + (field.uuid to column)) },
                        )
                        if (field.type == TrackFieldType.Number) {
                            val unitColumn = mapping.numberUnitColumns[field.uuid]
                            SelectionField(
                                "${field.name} Unit",
                                listOf<String?>(null) + headers,
                                unitColumn,
                                { it ?: "Use ${field.unitId}" },
                                { column -> mapping = mapping.copy(numberUnitColumns = if (column == null) mapping.numberUnitColumns - field.uuid else mapping.numberUnitColumns + (field.uuid to column)) },
                            )
                        }
                    }
                }
                error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                preview?.let { result ->
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Validation Preview", fontWeight = FontWeight.Bold)
                                Text("${result.totalRows} rows · ${result.validRows} valid · ${result.invalidRows} invalid")
                                result.issues.take(8).forEach { issue -> Text("Row ${issue.rowNumber}: ${issue.message}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                                if (result.issues.size > 8) Text("${result.issues.size - 8} more issues", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if (result.invalidRows > 0) item { Text("Fix every invalid row before importing. No Entries are written until the entire preview is valid.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = { WhipTextButton(enabled = canImport && !saving, onClick = { onImport(requireNotNull(preview)) }) { Text(if (saving) "Importing…" else "Import ${preview?.validRows ?: 0} Entries") } },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun TrackOptionsPage(
    projection: TrackProjection,
    onEdit: () -> Unit,
    onSetPinned: (Boolean) -> Unit,
    onSetArchived: (Boolean) -> Unit,
    onDuplicate: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onDelete: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 16.dp, 20.dp, WhipSpacing.screenExpanded),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { WhipPageHeader("Track Options", "Manage this Track's identity, visibility, structure, and data.") }
        item {
            WhipActionList {
                WhipActionRow("Edit Track", onEdit, supportingText = "Change identity, Fields, Area, and tags.")
                WhipActionDivider()
                WhipActionRow(if (projection.track.pinned) "Unpin From Home" else "Pin to Home", { onSetPinned(!projection.track.pinned) }, supportingText = "Pinned Tracks provide quick Entry capture from Home.", navigates = false)
                WhipActionDivider()
                WhipActionRow("Duplicate Structure", onDuplicate, supportingText = "Copies Fields and Choice options, but not Entries or Automations.", navigates = false)
                WhipActionDivider()
                WhipActionRow("Export Track CSV", onExport, supportingText = "One column per Field, with stable Entry identity and dates.", navigates = false)
                if (!projection.track.archived) {
                    WhipActionDivider()
                    WhipActionRow("Import Entries From CSV", onImport, supportingText = "Map columns to Fields and validate every row before one atomic import.", navigates = false)
                }
                WhipActionDivider()
                WhipActionRow(if (projection.track.archived) "Restore Track" else "Archive Track", { onSetArchived(!projection.track.archived) }, supportingText = if (projection.track.archived) "Allow new Entries again." else "Pause new capture while preserving history and Goal evidence.", navigates = false)
            }
        }
        item { WhipDangerZone { WhipActionRow("Delete Track Permanently", onDelete, icon = Icons.Outlined.DeleteForever, navigates = false, danger = true) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrackEditor(
    initial: TrackProjection?,
    areas: List<Area>,
    customUnits: List<UnitDefinition>,
    automationChoiceReferenceCounts: Map<Long, Int>,
    defaultAreaId: String?,
    saving: Boolean,
    modifier: Modifier,
    sessionId: Long = 0L,
    onDismiss: () -> Unit,
    onCreateArea: (String, Long?, (Result<String>) -> Unit) -> Unit,
    onCreateCustomUnit: CreateCustomUnitAction,
    customIdentityEmojis: List<CustomIdentityEmoji> = emptyList(),
    onSaveIdentityEmoji: (CustomIdentityEmoji) -> Unit = {},
    onRemoveSavedIdentityEmoji: (String) -> Unit = {},
    onReviewAutomations: () -> Unit,
    onSave: (TrackDraft, Set<Long>, Set<Long>, Map<Long, Long>) -> Unit,
) {
    val token = "track-${initial?.track?.id ?: "new"}-$sessionId"
    val stateHolder: TrackEditorViewModel = viewModel(key = "track-editor-$token")
    val savedState by stateHolder.state.collectAsStateWithLifecycle()
    val initialDraft = remember(initial?.track?.id) {
        initial?.track?.toDraft(initial.fields, initial.options) ?: TrackDraft(
            name = "",
            areaId = defaultAreaId,
            fields = listOf(TrackFieldDraft("Name", TrackFieldType.ShortText, required = true, primary = true)),
        )
    }
    LaunchedEffect(token) { stateHolder.initialize(token, initialDraft) }
    val editorState = savedState.takeIf { it.token == token && it.draft != null }
        ?: TrackEditorState(token = token, draft = initialDraft)
    val draft = requireNotNull(editorState.draft)
    val fields = draft.fields
    var editingFieldIndex by rememberSaveable(initial?.track?.id) { mutableStateOf<Int?>(null) }
    var addingField by rememberSaveable(initial?.track?.id) { mutableStateOf(false) }
    var confirmFieldDeleteIndex by rememberSaveable(initial?.track?.id) { mutableStateOf<Int?>(null) }
    var confirmOptionDeletion by rememberSaveable(initial?.track?.id) { mutableStateOf(false) }
    var unsavedConfirm by rememberSaveable { mutableStateOf(false) }
    var validationError by rememberSaveable(initial?.track?.id) { mutableStateOf<String?>(null) }
    val dirty = draft != initialDraft
    fun dismissAndClear() { stateHolder.clear(); onDismiss() }
    fun requestDismiss() { if (dirty) unsavedConfirm = true else dismissAndClear() }
    fun currentDraft(): TrackDraft {
        val selectedArea = areas.firstOrNull { it.id == draft.areaId }
        return draft.copy(
            area = selectedArea?.name.orEmpty(),
        )
    }
    fun removedOptionsNeedingDecision(): List<Triple<TrackChoiceOption, Int, Int>> {
        val retained = fields.flatMap(TrackFieldDraft::options).mapNotNull(TrackChoiceOptionDraft::id).toSet()
        return initial?.options.orEmpty().filter { it.id !in retained }.map { option ->
            Triple(
                option,
                initial?.entries.orEmpty().count { entry -> entry.value(option.fieldId)?.choiceOptionId == option.id },
                automationChoiceReferenceCounts[option.id] ?: 0,
            )
        }.filter { (_, entryCount, automationCount) -> entryCount > 0 || automationCount > 0 }
    }
    BackHandler(enabled = true, onBack = ::requestDismiss)

    WhipFullScreenSurface(
        title = if (initial == null) "Create Track" else "Edit Track",
        modifier = modifier.testTag("track-editor-surface"),
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (initial == null) "Create Track" else "Edit Track") },
                    navigationIcon = { IconButton(onClick = ::requestDismiss) { Icon(Icons.Outlined.Close, "Close Track Editor") } },
                    actions = { WhipTextButton(enabled = !saving, onClick = {
                        if (draft.name.isBlank()) {
                            validationError = "Track name is required."
                            return@WhipTextButton
                        }
                        if (fields.isEmpty()) {
                            validationError = "Add at least one Entry Field."
                            return@WhipTextButton
                        }
                        if (initial == null && areas.count { !it.archived } > 1 && draft.areaId == null) {
                            validationError = "Choose an Area for this Track."
                            return@WhipTextButton
                        }
                        val valid = runCatching { currentDraft().validated() }
                            .onFailure { validationError = it.message ?: "Review the track fields." }
                            .getOrNull()
                        if (valid != null) {
                            validationError = null
                            val unconfirmed = removedOptionsNeedingDecision().filterNot { (option) ->
                                option.id in editorState.confirmedOptionDeletes || option.id in editorState.optionReplacementIds
                            }
                            if (unconfirmed.isNotEmpty()) confirmOptionDeletion = true
                            else onSave(valid, editorState.confirmedFieldDeletes, editorState.confirmedOptionDeletes, editorState.optionReplacementIds)
                        }
                    }) { Text(if (saving) "Saving…" else "Save") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
        ) { padding ->
            WhipReorderLazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp, 16.dp, 20.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { Text("* Required field", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                validationError?.let { message -> item { FormValidationSummary(listOf(message), visible = true, testTag = "track-save-problem") } }
                item { EditorSectionHeader("Identity", "Name this structured log and choose a simple visual identifier.") }
                item { OutlinedTextField(draft.name, { value -> stateHolder.updateDraft { it.copy(name = value.replace('\n', ' ').replace('\r', ' ').take(100)) } }, label = { Text("Track Name *") }, singleLine = true, isError = validationError != null && draft.name.isBlank(), modifier = Modifier.fillMaxWidth().testTag("track-editor-name"), supportingText = if (validationError != null && draft.name.isBlank()) {{ Text("Track name is required") }} else {{ Text("${draft.name.length}/100") }}) }
                item { OutlinedTextField(draft.description, { value -> stateHolder.updateDraft { it.copy(description = value.take(500)) } }, label = { Text("Description") }, minLines = 2, maxLines = 5, modifier = Modifier.fillMaxWidth()) }
                item {
                    WhipEmojiPicker(
                        value = draft.icon,
                        defaultEmoji = DEFAULT_TRACK_EMOJI,
                        onValueChange = { emoji -> stateHolder.updateDraft { it.copy(icon = emoji) } },
                        modifier = Modifier.fillMaxWidth(),
                        customEmojis = customIdentityEmojis,
                        onSaveEmoji = onSaveIdentityEmoji,
                        onRemoveSavedEmoji = onRemoveSavedIdentityEmoji,
                    )
                }
                item { HorizontalDivider() }
                item { EditorSectionHeader("Entry Fields", "Every Entry follows this order. One or more Entry Identity Fields create its readable name.") }
                itemsIndexed(fields, key = { index, field -> field.uuid ?: field.id?.toString() ?: "new-field-$index-${field.name}" }) { index, field ->
                    val reorderInteraction = rememberWhipReorderInteractionState()
                    Card(
                        Modifier.fillMaxWidth().whipReorderItem(
                            reorderInteraction,
                            layoutPosition = index + 1,
                            layoutScope = "track-editor-fields",
                        ),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                WhipReorderHandle(
                                    label = field.name.ifBlank { "field ${index + 1}" },
                                    canMovePrevious = index > 0,
                                    canMoveNext = index < fields.lastIndex,
                                    position = index + 1,
                                    total = fields.size,
                                    interactionState = reorderInteraction,
                                    moveWholeItem = true,
                                    layoutScope = "track-editor-fields",
                                    onMove = { delta -> stateHolder.updateDraft { current -> current.copy(fields = moveListItem(current.fields, index, delta)) } },
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(field.name.ifBlank { "Untitled Field" }, fontWeight = FontWeight.SemiBold)
                                    Text(listOfNotNull(field.configurationLabel(), "Identity".takeIf { field.primary }, "Required".takeIf { field.required && !field.primary }, "Label Shown".takeIf { field.showInList }).joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { editingFieldIndex = index }) { Icon(Icons.Outlined.Edit, "Edit Field ${field.name}") }
                            }
                            if (fields.size > 20 && index == fields.lastIndex) Text("Long entry forms take more time to fill.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item { WhipOutlinedButton(onClick = { addingField = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Add Field") } }
                item { HorizontalDivider() }
                item { EditorSectionHeader("Organization", "Every Track belongs to one Area. Tags help search without changing the Entry form.") }
                item {
                    AreaPicker(
                        areas = areas,
                        selectedAreaId = draft.areaId,
                        selectedAreaName = areas.firstOrNull { it.id == draft.areaId }?.name.orEmpty(),
                        onSelect = { id, _ -> stateHolder.updateDraft { it.copy(areaId = id) } },
                        onCreateArea = onCreateArea,
                    )
                }
                item { OutlinedTextField(draft.tags.joinToString(", "), { value -> stateHolder.updateDraft { it.copy(tags = value.split(',').map(String::trim)) } }, label = { Text("Tags") }, supportingText = { Text("Separate optional track tags with commas.") }, modifier = Modifier.fillMaxWidth()) }
            }
        }
    }

    editingFieldIndex?.let { index -> fields.getOrNull(index)?.let { field ->
        TrackFieldEditor(
            initial = field,
            allUnits = BuiltInUnits.all + customUnits,
            existingHasValues = field.id?.let { fieldId -> initial?.entries?.any { it.value(fieldId) != null } } == true,
            onDismiss = { editingFieldIndex = null },
            onCreateCustomUnit = onCreateCustomUnit,
            onSave = { updated ->
                stateHolder.updateDraft { current -> current.copy(fields = current.fields.toMutableList().also { mutable ->
                    mutable[index] = updated
                }) }
                editingFieldIndex = null
            },
            onDelete = if (fields.size > 1 && !field.primary) {{ confirmFieldDeleteIndex = index }} else null,
        )
    } }
    if (addingField) TrackFieldEditor(
        initial = TrackFieldDraft("", TrackFieldType.ShortText),
        allUnits = BuiltInUnits.all + customUnits,
        existingHasValues = false,
        onDismiss = { addingField = false },
        onCreateCustomUnit = onCreateCustomUnit,
        onSave = { field -> stateHolder.updateDraft { it.copy(fields = it.fields + field) }; addingField = false },
    )
    confirmFieldDeleteIndex?.let { index -> fields.getOrNull(index)?.let { field ->
        val valueCount = field.id?.let { id -> initial?.entries?.count { it.value(id) != null } } ?: 0
        PaneAwareAlertDialog(
            onDismissRequest = { confirmFieldDeleteIndex = null },
            title = { Text("Delete ${field.name} Field?") },
            text = { Text(if (valueCount > 0) "This permanently deletes $valueCount saved values from existing Entries. Renaming or reordering keeps them." else "This Field has no saved values and can be removed safely.") },
            confirmButton = { WhipTextButton(onClick = {
                field.id?.let { id -> stateHolder.update { it.copy(confirmedFieldDeletes = it.confirmedFieldDeletes + id) } }
                stateHolder.updateDraft { current -> current.copy(fields = current.fields.toMutableList().also { it.removeAt(index) }) }
                confirmFieldDeleteIndex = null
                editingFieldIndex = null
            }) { Text("Delete Field", color = MaterialTheme.colorScheme.error) } },
            dismissButton = {
                Row {
                    if (initial != null) WhipTextButton(onClick = { confirmFieldDeleteIndex = null; onReviewAutomations() }) { Text("Review Automations") }
                    WhipTextButton(onClick = { confirmFieldDeleteIndex = null }) { Text("Cancel") }
                }
            },
        )
    } }
    if (unsavedConfirm) PaneAwareAlertDialog(
        onDismissRequest = { unsavedConfirm = false },
        title = { Text("Discard Unsaved Changes?") },
        text = { Text("Your Track and Field edits have not been saved.") },
        confirmButton = { WhipTextButton(onClick = { unsavedConfirm = false; dismissAndClear() }) { Text("Discard", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { WhipTextButton(onClick = { unsavedConfirm = false }) { Text("Keep Editing") } },
    )
    if (confirmOptionDeletion) {
        val impacts = removedOptionsNeedingDecision()
        val unresolvedAutomation = impacts.any { (option, _, automationCount) ->
            automationCount > 0 && option.id !in editorState.optionReplacementIds
        }
        PaneAwareAlertDialog(
            onDismissRequest = { confirmOptionDeletion = false },
            title = { Text("Resolve Removed Choices") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { Text("For each removed Choice, move its Entries and Automation references to a remaining Choice, or permanently delete its saved values.") }
                    items(impacts, key = { it.first.id }) { (option, entryCount, automationCount) ->
                        val candidates = fields.firstOrNull { it.id == option.fieldId }?.options.orEmpty()
                            .filter { it.id != null && it.id != option.id }
                        SelectionField(
                            "${option.label} · ${quantityLabel(entryCount, "Entry")} · ${quantityLabel(automationCount, "Automation")}",
                            listOf<TrackChoiceOptionDraft?>(null) + candidates,
                            candidates.firstOrNull { it.id == editorState.optionReplacementIds[option.id] },
                            { it?.label?.let { label -> "Replace With $label" } ?: "Delete Saved Values" },
                            { replacement ->
                                stateHolder.update { current -> current.copy(
                                    optionReplacementIds = if (replacement?.id == null) current.optionReplacementIds - option.id
                                    else current.optionReplacementIds + (option.id to replacement.id),
                                ) }
                            },
                        )
                    }
                    if (unresolvedAutomation) item { Text("Replace every Choice used by an Automation, or choose Review Automations to edit or remove those Automations first.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { WhipTextButton(enabled = !unresolvedAutomation, onClick = {
                val replaced = editorState.optionReplacementIds.keys
                val confirmed = editorState.confirmedOptionDeletes + impacts.map { it.first.id }.filterNot { it in replaced }
                stateHolder.update { it.copy(confirmedOptionDeletes = confirmed) }
                confirmOptionDeletion = false
                onSave(currentDraft(), editorState.confirmedFieldDeletes, confirmed, editorState.optionReplacementIds)
            }) { Text("Apply Choices and Save") } },
            dismissButton = {
                Row {
                    if (initial != null) WhipTextButton(onClick = { confirmOptionDeletion = false; onReviewAutomations() }) { Text("Review Automations") }
                    WhipTextButton(onClick = { confirmOptionDeletion = false }) { Text("Keep Editing") }
                }
            },
        )
    }
}

internal fun TrackUiState.choiceAutomationReferenceCounts(): Map<Long, Int> {
    val optionIdByUuid = projections.flatMap(TrackProjection::options).associate { it.uuid to it.id }
    return buildList {
        linkRules.forEach { rule -> rule.conditions.forEach { condition -> addAll(condition.choiceOptionUuids) } }
        triggerRules.forEach { rule ->
            rule.conditions.forEach { condition -> addAll(condition.choiceOptionUuids) }
            rule.mappings.mapNotNullTo(this) { it.constantValue?.choiceOptionUuid }
        }
    }.mapNotNull(optionIdByUuid::get).groupingBy { it }.eachCount()
}

@Composable
private fun TrackFieldEditor(
    initial: TrackFieldDraft,
    allUnits: List<UnitDefinition>,
    existingHasValues: Boolean,
    onDismiss: () -> Unit,
    onCreateCustomUnit: CreateCustomUnitAction,
    onSave: (TrackFieldDraft) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by rememberSaveable(initial.uuid, initial.id) { mutableStateOf(initial.name) }
    var type by rememberSaveable(initial.uuid, initial.id) { mutableStateOf(initial.type) }
    var required by rememberSaveable(initial.uuid, initial.id) { mutableStateOf(initial.required) }
    var primary by rememberSaveable(initial.uuid, initial.id) { mutableStateOf(initial.primary) }
    var showInList by rememberSaveable(initial.uuid, initial.id) { mutableStateOf(initial.showInList) }
    var dimension by rememberSaveable(initial.uuid, initial.id) { mutableStateOf(initial.dimension ?: UnitDimension.Count) }
    var unitId by rememberSaveable(initial.uuid, initial.id) { mutableStateOf(initial.unitId ?: allUnits.first { it.dimension == UnitDimension.Count }.id) }
    var precision by rememberSaveable(initial.uuid, initial.id) { mutableIntStateOf(initial.precision) }
    var scaleMinText by rememberSaveable(initial.uuid, initial.id) { mutableStateOf((initial.scaleMin ?: 1).toString()) }
    var scaleMaxText by rememberSaveable(initial.uuid, initial.id) { mutableStateOf((initial.scaleMax ?: 5).toString()) }
    var scaleStepText by rememberSaveable(initial.uuid, initial.id) { mutableStateOf(formatTrackScaleValue(initial.scaleStep)) }
    var lowLabel by rememberSaveable(initial.uuid, initial.id) { mutableStateOf(initial.scaleLowLabel) }
    var highLabel by rememberSaveable(initial.uuid, initial.id) { mutableStateOf(initial.scaleHighLabel) }
    var choices by remember(initial.uuid, initial.id) { mutableStateOf(initial.options.ifEmpty { listOf(TrackChoiceOptionDraft("Option 1")) }) }
    val scaleMin = scaleMinText.trim().toIntOrNull()
    val scaleMax = scaleMaxText.trim().toIntOrNull()
    val scaleStep = scaleStepText.toWhipDoubleOrNull()
    val scaleValuesResult = if (type == TrackFieldType.Scale) runCatching {
        trackScaleValues(
            requireNotNull(scaleMin) { "Enter a whole-number Scale minimum" },
            requireNotNull(scaleMax) { "Enter a whole-number Scale maximum" },
            requireNotNull(scaleStep) { "Enter a valid Scale increment" },
        )
    } else null
    val scaleValues = scaleValuesResult?.getOrNull()
    val scaleError = scaleValuesResult?.exceptionOrNull()?.message
    PaneAwareAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == null && initial.uuid == null) "Add Field" else "Edit Field") },
        text = {
            WhipReorderLazyColumn(Modifier.fillMaxWidth().fillMaxHeight(0.72f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { OutlinedTextField(name, { name = it.take(80) }, label = { Text("Field Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { SelectionField("Field Type", TrackFieldType.entries, type, TrackFieldType::uiLabel, { type = it }, enabled = !existingHasValues) }
                if (existingHasValues) item { Text("A Field type can change only before it has saved values. Delete this Field and add another to change its type.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                when (type) {
                    TrackFieldType.Number -> {
                        item { SelectionField("Measurement Type", UnitDimension.entries, dimension, UnitDimension::uiLabel, { selected ->
                            dimension = selected
                            unitId = allUnits.firstOrNull { it.dimension == selected && !it.archived }?.id.orEmpty()
                        }) }
                        item { UnitSelectionField(allUnits, unitId, dimension, { unitId = it }, onCreateCustomUnit, label = "Unit", supportingText = "Stored values keep both the entered unit and a canonical value for compatible Goals.") }
                        item { SelectionField("Decimal Places", (0..6).toList(), precision, Int::toString, { precision = it }) }
                    }
                    TrackFieldType.SingleChoice -> {
                        item { Text("Choice Options", style = MaterialTheme.typography.labelLarge) }
                        itemsIndexed(choices, key = { index, option -> option.uuid ?: option.id?.toString() ?: "choice-$index" }) { index, option ->
                            val reorderInteraction = rememberWhipReorderInteractionState()
                            Row(
                                modifier = Modifier.whipReorderItem(
                                    reorderInteraction,
                                    layoutPosition = index + 1,
                                    layoutScope = "track-field-choice-options",
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                WhipReorderHandle(
                                    label = option.label.ifBlank { "choice option ${index + 1}" },
                                    canMovePrevious = index > 0,
                                    canMoveNext = index < choices.lastIndex,
                                    position = index + 1,
                                    total = choices.size,
                                    interactionState = reorderInteraction,
                                    moveWholeItem = true,
                                    layoutScope = "track-field-choice-options",
                                    onMove = { delta -> choices = moveListItem(choices, index, delta) },
                                )
                                OutlinedTextField(option.label, { label -> choices = choices.toMutableList().also { it[index] = option.copy(label = label.take(80)) } }, label = { Text("Option ${index + 1}") }, singleLine = true, modifier = Modifier.weight(1f))
                                IconButton(enabled = choices.size > 1, onClick = { choices = choices.toMutableList().also { it.removeAt(index) } }) { Icon(Icons.Outlined.Close, "Remove Option ${option.label}") }
                            }
                        }
                        item { WhipOutlinedButton(onClick = { choices = choices + TrackChoiceOptionDraft("") }, modifier = Modifier.fillMaxWidth()) { Text("Add Choice Option") } }
                    }
                    TrackFieldType.Scale -> {
                        item {
                            val currentBounds = if (scaleMin != null && scaleMax != null) scaleMin to scaleMax else 1 to 5
                            SelectionField(
                                "Scale Preset",
                                listOf(1 to 5, 1 to 10, currentBounds).distinct(),
                                currentBounds,
                                { "${it.first}–${it.second}" },
                                { selected -> scaleMinText = selected.first.toString(); scaleMaxText = selected.second.toString() },
                            )
                        }
                        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(scaleMinText, { scaleMinText = it }, label = { Text("Minimum") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(scaleMaxText, { scaleMaxText = it }, label = { Text("Maximum") }, modifier = Modifier.weight(1f), singleLine = true)
                        } }
                        item {
                            OutlinedTextField(
                                scaleStepText,
                                { scaleStepText = it },
                                label = { Text("Increment") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth().testTag("track-scale-increment"),
                                singleLine = true,
                                isError = scaleError != null,
                                supportingText = {
                                    Text(
                                        scaleError ?: buildString {
                                            append("Use 0.5 for half steps, such as a 3.5 rating.")
                                            scaleValues?.let { append(" ${it.size} selectable values.") }
                                        },
                                    )
                                },
                            )
                        }
                        item { OutlinedTextField(lowLabel, { lowLabel = it.take(40) }, label = { Text("Low Label") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                        item { OutlinedTextField(highLabel, { highLabel = it.take(40) }, label = { Text("High Label") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                    }
                    else -> Unit
                }
                item { TrackToggleRow("Required", "Entries cannot be saved without this Field.", required || primary, { required = it }, enabled = !primary) }
                item { TrackToggleRow("Entry Identity", "Combine one or more required Fields to distinguish Entries with the same name.", primary, { primary = it; if (it) required = true }) }
                item { TrackToggleRow("Show Label in Entry List", "Show this Field name and value beneath the combined Entry identity.", showInList, { showInList = it }) }
                onDelete?.let { action -> item { HorizontalDivider(); WhipTextButton(onClick = action, modifier = Modifier.fillMaxWidth()) { Text("Delete Field", color = MaterialTheme.colorScheme.error) } } }
            }
        },
        confirmButton = { WhipTextButton(enabled = name.isNotBlank() && (type != TrackFieldType.SingleChoice || choices.all { it.label.isNotBlank() }) && (type != TrackFieldType.Scale || scaleValues != null), onClick = {
            onSave(initial.copy(name = name, type = type, required = required || primary, primary = primary, showInList = showInList, dimension = dimension.takeIf { type == TrackFieldType.Number }, unitId = unitId.takeIf { type == TrackFieldType.Number }, precision = precision, scaleMin = scaleMin.takeIf { type == TrackFieldType.Scale }, scaleMax = scaleMax.takeIf { type == TrackFieldType.Scale }, scaleLowLabel = lowLabel, scaleHighLabel = highLabel, scaleStep = scaleStep?.takeIf { type == TrackFieldType.Scale } ?: 1.0, options = choices.takeIf { type == TrackFieldType.SingleChoice }.orEmpty()))
        }) { Text("Save Field") } },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrackEntryEditor(
    projection: TrackProjection,
    initial: TrackEntryProjection?,
    prefill: TrackEntryDraft? = null,
    customUnits: List<UnitDefinition>,
    today: LocalDate,
    saving: Boolean,
    modifier: Modifier,
    sessionId: Long = 0L,
    onDismiss: () -> Unit,
    onSave: (TrackEntryDraft) -> Unit,
    onDelete: (() -> Unit)? = null,
    onOpenExisting: (Long) -> Unit = {},
) {
    val token = "entry-${projection.track.id}-${initial?.entry?.id ?: "new"}-${prefill?.sourceOccurrenceId ?: "direct"}-$sessionId"
    val stateHolder: TrackEntryEditorViewModel = viewModel(key = "track-$token")
    val savedState by stateHolder.state.collectAsStateWithLifecycle()
    val initialValues = remember(initial?.entry?.id, prefill) {
        projection.fields.associate { field ->
            val value = initial?.value(field.id)
            field.uuid to (prefill?.values?.get(field.uuid) ?: TrackValueDraft(
                    textValue = value?.textValue,
                    enteredNumber = value?.enteredNumber,
                    enteredUnitId = value?.enteredUnitId ?: field.unitId,
                    dateValue = value?.dateValue,
                    booleanValue = value?.booleanValue,
                    choiceOptionUuid = projection.options.firstOrNull { it.id == value?.choiceOptionId }?.uuid,
                    scaleValue = value?.scaleValue,
                ))
        }
    }
    val initialEntryDate = prefill?.entryDate ?: initial?.entry?.entryDate ?: today
    val initialDraft = remember(token, initialValues, initialEntryDate) {
        TrackEntryDraft(
            entryDate = initialEntryDate,
            values = initialValues,
            sourceOccurrenceId = prefill?.sourceOccurrenceId,
            sourceExplanation = prefill?.sourceExplanation.orEmpty(),
        )
    }
    LaunchedEffect(token) { stateHolder.initialize(token, initialDraft) }
    val draft = savedState.takeIf { it.token == token }?.draft ?: initialDraft
    val values = draft.values
    val entryDate = draft.entryDate
    var datePickerOpen by rememberSaveable { mutableStateOf(false) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var unsavedConfirm by rememberSaveable { mutableStateOf(false) }
    var possibleMatchId by rememberSaveable { mutableStateOf<Long?>(null) }
    val dirty = draft != initialDraft
    fun dismissAndClear() { stateHolder.clear(); onDismiss() }
    fun requestDismiss() { if (dirty) unsavedConfirm = true else dismissAndClear() }
    val missing = projection.fields.filter { it.required && values[it.uuid]?.isBlankFor(it.type) != false }
    val duplicatePrimaryMatches = projection.identityKey(values)?.let { entered ->
            projection.entries.filter { candidate ->
                candidate.entry.id != initial?.entry?.id && projection.identityKey(candidate) == entered
            }.take(3)
        }.orEmpty()
    BackHandler(enabled = true, onBack = ::requestDismiss)

    WhipFullScreenSurface(
        if (initial == null) "Add Entry" else "Edit Entry",
        modifier.testTag("track-entry-editor-surface"),
    ) {
        Scaffold(
            topBar = { TopAppBar(
                title = { Text(if (initial == null) "Add Entry" else "Edit Entry") },
                navigationIcon = { IconButton(onClick = ::requestDismiss) { Icon(Icons.Outlined.Close, "Close Entry Editor") } },
                actions = { WhipTextButton(enabled = !saving, onClick = { attempted = true; if (missing.isEmpty()) onSave(draft) }) { Text(if (saving) "Saving…" else if (initial == null) "Add" else "Save") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            ) },
        ) { padding ->
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp, 16.dp, 20.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { WhipPageHeader(if (initial == null) "New ${projection.primaryField.name}" else projection.primaryText(requireNotNull(initial)), "Fields follow the reusable ${projection.track.name} structure.") }
                if (!prefill?.sourceExplanation.isNullOrBlank()) item {
                    Text("Prefilled from ${prefill.sourceExplanation}. Review the fields before adding this entry.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(projection.fields, key = TrackField::uuid) { field ->
                    val current = values[field.uuid] ?: TrackValueDraft(enteredUnitId = field.unitId)
                    TrackEntryField(
                        field = field,
                        value = current,
                        options = projection.optionsFor(field.id),
                        units = BuiltInUnits.all + customUnits,
                        today = today,
                        showError = attempted && field.required && current.isBlankFor(field.type),
                        onValue = { value -> stateHolder.updateDraft { it.copy(values = it.values + (field.uuid to value)) } },
                    )
                }
                if (duplicatePrimaryMatches.isNotEmpty()) item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Possible Existing Entry", fontWeight = FontWeight.SemiBold)
                            Text("All Entry Identity Fields match. Duplicates are allowed; review a match or keep this separate Entry.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            duplicatePrimaryMatches.forEach { match ->
                                WhipTextButton(onClick = { possibleMatchId = match.entry.id }) {
                                    Text("Review ${projection.primaryText(match)} · ${match.entry.entryDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}")
                                }
                            }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Entry Date", style = MaterialTheme.typography.labelLarge)
                        WhipOutlinedButton(onClick = { datePickerOpen = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(entryDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), modifier = Modifier.weight(1f))
                        }
                        Text("The date this fact belongs to. Backdating does not change its creation time.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (attempted && missing.isNotEmpty()) item {
                    Text("Complete ${missing.joinToString { it.name }} before saving.", color = MaterialTheme.colorScheme.error)
                }
                onDelete?.let { action -> item {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    WhipTextButton(
                        onClick = { stateHolder.clear(); action() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Delete Entry", color = MaterialTheme.colorScheme.error) }
                } }
            }
        }
    }
    if (datePickerOpen) WhipDatePickerDialog(entryDate, { datePickerOpen = false }, { selected -> stateHolder.updateDraft { it.copy(entryDate = selected) }; datePickerOpen = false })
    if (unsavedConfirm) PaneAwareAlertDialog(
        onDismissRequest = { unsavedConfirm = false },
        title = { Text("Discard Unsaved Entry?") },
        text = { Text("Your changes to this Entry have not been saved.") },
        confirmButton = { WhipTextButton(onClick = { unsavedConfirm = false; dismissAndClear() }) { Text("Discard", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { WhipTextButton(onClick = { unsavedConfirm = false }) { Text("Keep Editing") } },
    )
    possibleMatchId?.let { matchId -> projection.entries.firstOrNull { it.entry.id == matchId }?.let { match ->
        PaneAwareAlertDialog(
            onDismissRequest = { possibleMatchId = null },
            title = { Text(projection.primaryText(match)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Existing Entry · ${match.entry.entryDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}")
                    projection.fields.filterNot(TrackField::primary).mapNotNull { field ->
                        projection.formattedValue(match, field, BuiltInUnits.all + customUnits)
                            .takeIf(String::isNotBlank)?.let { field.name to it }
                    }.take(4).forEach { (name, value) -> Text("$name · $value", style = MaterialTheme.typography.bodySmall) }
                    Text("Editing the existing Entry will discard this unsaved new Entry.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { WhipTextButton(onClick = { possibleMatchId = null; stateHolder.clear(); onOpenExisting(matchId) }) { Text("Edit Existing") } },
            dismissButton = { WhipTextButton(onClick = { possibleMatchId = null }) { Text("Keep New Entry") } },
        )
    } }
}

@Composable
internal fun TrackEntryField(
    field: TrackField,
    value: TrackValueDraft,
    options: List<TrackChoiceOption>,
    units: List<UnitDefinition>,
    today: LocalDate = LocalDate.now(),
    showError: Boolean,
    onValue: (TrackValueDraft) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(field.name + if (field.required) " *" else "", style = MaterialTheme.typography.labelLarge)
        when (field.type) {
            TrackFieldType.ShortText -> OutlinedTextField(
                value.textValue.orEmpty(),
                { onValue(value.copy(textValue = it.take(300))) },
                singleLine = true,
                isError = showError,
                modifier = Modifier.fillMaxWidth().testTag("track-entry-short-text-${field.uuid}"),
            )
            TrackFieldType.LongText -> OutlinedTextField(
                value.textValue.orEmpty(),
                { onValue(value.copy(textValue = it.take(5_000))) },
                minLines = 3,
                maxLines = 8,
                isError = showError,
                modifier = Modifier.fillMaxWidth().testTag("track-entry-long-text-${field.uuid}"),
            )
            TrackFieldType.Number -> {
                val unit = units.firstOrNull { it.id == (value.enteredUnitId ?: field.unitId) }
                var numberText by rememberSaveable(field.uuid) {
                    mutableStateOf(value.enteredNumber?.let(::editableNumericValue).orEmpty())
                }
                OutlinedTextField(
                    numberText,
                    { input ->
                        numberText = input
                        onValue(
                            value.copy(
                                enteredNumber = input.toWhipDoubleOrNull(),
                                enteredUnitId = value.enteredUnitId ?: field.unitId,
                            ),
                        )
                    },
                    label = { Text(unit?.symbol?.takeIf(String::isNotBlank) ?: "Value") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = showError,
                    modifier = Modifier.fillMaxWidth().testTag("track-entry-number-${field.uuid}"),
                )
                val compatible = units.filter { it.dimension == field.dimension && !it.archived }
                if (compatible.size > 1) SelectionField("Entered Unit", compatible, compatible.firstOrNull { it.id == value.enteredUnitId } ?: compatible.first(), ::unitDefinitionDisplayLabel, { onValue(value.copy(enteredUnitId = it.id)) })
            }
            TrackFieldType.SingleChoice -> {
                val selected = options.firstOrNull { it.uuid == value.choiceOptionUuid }
                SelectionField("Choose ${field.name}", listOf<TrackChoiceOption?>(null) + options, selected, { it?.label ?: "Unanswered" }, { onValue(value.copy(choiceOptionUuid = it?.uuid)) })
            }
            TrackFieldType.Scale -> {
                val min = requireNotNull(field.scaleMin)
                val max = requireNotNull(field.scaleMax)
                val choices = trackScaleValues(min, max, field.scaleStep)
                val selectedValue = value.scaleValue?.let { current ->
                    normalizeTrackScaleValue(current, min, max, field.scaleStep)
                }
                val selectedIndex = selectedValue?.let(choices::indexOf)?.takeIf { it >= 0 }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WhipOutlinedButton(
                        onClick = { selectedIndex?.takeIf { it > 0 }?.let { onValue(value.copy(scaleValue = choices[it - 1])) } },
                        enabled = selectedIndex != null && selectedIndex > 0,
                        modifier = Modifier.testTag("track-entry-scale-decrease").semantics {
                            contentDescription = "Decrease ${field.name} by ${formatTrackScaleValue(field.scaleStep)}"
                        },
                    ) { Text("−") }
                    Text(
                        selectedValue?.let(::formatTrackScaleValue) ?: "Not Set",
                        modifier = Modifier.weight(1f).testTag("track-entry-scale-value"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    WhipOutlinedButton(
                        onClick = {
                            val nextIndex = if (selectedIndex == null) 0 else (selectedIndex + 1).coerceAtMost(choices.lastIndex)
                            onValue(value.copy(scaleValue = choices[nextIndex]))
                        },
                        enabled = selectedIndex == null || selectedIndex < choices.lastIndex,
                        modifier = Modifier.testTag("track-entry-scale-increase").semantics {
                            contentDescription = "Increase ${field.name} by ${formatTrackScaleValue(field.scaleStep)}"
                        },
                    ) { Text("+") }
                }
                Slider(
                    value = selectedValue?.toFloat() ?: choices.first().toFloat(),
                    onValueChange = { raw -> onValue(value.copy(scaleValue = snapTrackScaleValue(raw.toDouble(), min, max, field.scaleStep))) },
                    valueRange = choices.first().toFloat()..choices.last().toFloat(),
                    steps = (choices.size - 2).coerceAtLeast(0),
                    modifier = Modifier.fillMaxWidth().testTag("track-entry-scale-${field.uuid}").semantics {
                        contentDescription = "${field.name} Scale"
                        stateDescription = selectedValue?.let(::formatTrackScaleValue) ?: "Not set"
                    },
                )
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        listOf(formatTrackScaleValue(choices.first()), field.scaleLowLabel).filter(String::isNotBlank).joinToString(" · "),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        listOf(formatTrackScaleValue(choices.last()), field.scaleHighLabel).filter(String::isNotBlank).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!field.required && selectedValue != null) WhipTextButton(
                    onClick = { onValue(value.copy(scaleValue = null)) },
                    modifier = Modifier.testTag("track-entry-scale-clear"),
                ) { Text("Clear Scale") }
            }
            TrackFieldType.Date -> {
                var open by rememberSaveable(field.uuid) { mutableStateOf(false) }
                WhipOutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) { Text(value.dateValue?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: "Choose Date", modifier = Modifier.weight(1f)) }
                if (open) WhipDatePickerDialog(value.dateValue ?: today, { open = false }, { onValue(value.copy(dateValue = it)); open = false })
            }
            TrackFieldType.YesNo -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!field.required) WhipFilterChip(value.booleanValue == null, { onValue(value.copy(booleanValue = null)) }, { Text("Unanswered") })
                WhipFilterChip(value.booleanValue == true, { onValue(value.copy(booleanValue = true)) }, { Text("Yes") })
                WhipFilterChip(value.booleanValue == false, { onValue(value.copy(booleanValue = false)) }, { Text("No") })
            }
        }
        if (showError) Text("${field.name} is required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun TrackFilterDialog(
    modifier: Modifier = Modifier,
    projection: TrackProjection,
    initial: List<TrackCondition>,
    initialMode: TrackConditionMode,
    today: LocalDate = LocalDate.now(),
    units: List<UnitDefinition> = BuiltInUnits.all,
    onDismiss: () -> Unit,
    onApply: (TrackConditionMode, List<TrackCondition>) -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(initialMode) }
    var conditions by remember { mutableStateOf(initial) }
    var adding by rememberSaveable { mutableStateOf(false) }
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text("Filter Entries") },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { SegmentedChoiceBar(mode, TrackConditionMode.entries, { mode = it }, { if (it == TrackConditionMode.MatchAll) "Match All" else "Match Any" }, Modifier.fillMaxWidth()) }
                if (conditions.isEmpty()) item { Text("No filters. Every entry is included.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                itemsIndexed(conditions) { index, condition ->
                    val fieldName = projection.conditionFieldName(condition)
                    Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("$fieldName ${condition.operator.uiLabel()} ${condition.summaryValue(projection, units)}", Modifier.weight(1f))
                        IconButton(onClick = { conditions = conditions.toMutableList().also { it.removeAt(index) } }) { Icon(Icons.Outlined.Close, "Remove Filter") }
                    } }
                }
                item { WhipOutlinedButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) { Text("Add Condition") } }
            }
        },
        confirmButton = { WhipTextButton(onClick = { onApply(mode, conditions) }) { Text("Apply Filters") } },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (adding) TrackConditionEditor(projection, { adding = false }, today = today, units = units, modifier = modifier) { conditions = conditions + it; adding = false }
}

@Composable
internal fun TrackConditionEditor(
    projection: TrackProjection,
    onDismiss: () -> Unit,
    today: LocalDate = LocalDate.now(),
    units: List<UnitDefinition> = BuiltInUnits.all,
    modifier: Modifier = Modifier,
    onSave: (TrackCondition) -> Unit,
) {
    val subjects = remember(projection.track.id, projection.fields) {
        listOf(TrackConditionSubject(null)) + projection.fields.map(::TrackConditionSubject)
    }
    var subject by remember { mutableStateOf(subjects.first()) }
    val field = subject.trackField
    val fieldType = subject.type
    val numberUnit = field?.takeIf { it.type == TrackFieldType.Number }?.let { numberField ->
        units.firstOrNull { it.id == numberField.unitId }
    }
    var operator by remember(subject.uuid) { mutableStateOf(fieldType.availableOperators().first()) }
    var text by rememberSaveable(subject.uuid) { mutableStateOf("") }
    var firstNumber by rememberSaveable(subject.uuid) { mutableStateOf("") }
    var secondNumber by rememberSaveable(subject.uuid) { mutableStateOf("") }
    var selectedChoices by remember(subject.uuid) { mutableStateOf(emptySet<String>()) }
    var firstDate by rememberSaveable(subject.uuid) { mutableStateOf(today) }
    var secondDate by rememberSaveable(subject.uuid) { mutableStateOf(today) }
    var datePicker by rememberSaveable { mutableIntStateOf(0) }
    val needsNoValue = operator in setOf(TrackConditionOperator.IsBlank, TrackConditionOperator.IsNotBlank, TrackConditionOperator.IsYes, TrackConditionOperator.IsNo, TrackConditionOperator.IsUnanswered, TrackConditionOperator.IsAnswered)
    val valid = needsNoValue || when (fieldType) {
        TrackFieldType.ShortText, TrackFieldType.LongText -> text.isNotBlank()
        TrackFieldType.Number, TrackFieldType.Scale -> firstNumber.toWhipDoubleOrNull() != null && (operator != TrackConditionOperator.Between || secondNumber.toWhipDoubleOrNull() != null)
        TrackFieldType.SingleChoice -> selectedChoices.isNotEmpty()
        TrackFieldType.Date -> true
        TrackFieldType.YesNo -> true
    }
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text("Add Condition") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SelectionField("Field", subjects, subject, TrackConditionSubject::name, { subject = it; operator = it.type.availableOperators().first() })
            SelectionField("Operator", fieldType.availableOperators(), operator, TrackConditionOperator::uiLabel, { operator = it })
            if (!needsNoValue) when (fieldType) {
                TrackFieldType.ShortText, TrackFieldType.LongText -> OutlinedTextField(text, { text = it }, label = { Text("Text") }, modifier = Modifier.fillMaxWidth())
                TrackFieldType.Number, TrackFieldType.Scale -> {
                    val unitSuffix = numberUnit?.let(::unitDefinitionDisplayLabel)?.let { " ($it)" }.orEmpty()
                    OutlinedTextField(firstNumber, { firstNumber = it }, label = { Text((if (operator == TrackConditionOperator.Between) "Minimum" else "Value") + unitSuffix) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (operator == TrackConditionOperator.Between) OutlinedTextField(secondNumber, { secondNumber = it }, label = { Text("Maximum$unitSuffix") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                TrackFieldType.SingleChoice -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    projection.optionsFor(requireNotNull(field).id).forEach { option -> WhipFilterChip(option.uuid in selectedChoices, { selectedChoices = if (option.uuid in selectedChoices) selectedChoices - option.uuid else selectedChoices + option.uuid }, { Text(option.label) }) }
                }
                TrackFieldType.Date -> {
                    WhipOutlinedButton(onClick = { datePicker = 1 }, modifier = Modifier.fillMaxWidth()) { Text(firstDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))) }
                    if (operator == TrackConditionOperator.Between) WhipOutlinedButton(onClick = { datePicker = 2 }, modifier = Modifier.fillMaxWidth()) { Text(secondDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))) }
                }
                TrackFieldType.YesNo -> Unit
            }
        } },
        confirmButton = { WhipTextButton(enabled = valid, onClick = {
            val first = firstNumber.toWhipDoubleOrNull()?.let { if (fieldType == TrackFieldType.Number) numberUnit?.toCanonical(it) ?: it else it }
            val second = secondNumber.toWhipDoubleOrNull()?.let { if (fieldType == TrackFieldType.Number) numberUnit?.toCanonical(it) ?: it else it }
            onSave(TrackCondition(subject.uuid, operator, text.takeIf(String::isNotBlank), first, second, selectedChoices, firstDate.takeIf { fieldType == TrackFieldType.Date }, secondDate.takeIf { fieldType == TrackFieldType.Date && operator == TrackConditionOperator.Between }))
        }) { Text("Add") } },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (datePicker > 0) WhipDatePickerDialog(if (datePicker == 1) firstDate else secondDate, { datePicker = 0 }, { selected -> if (datePicker == 1) firstDate = selected else secondDate = selected; datePicker = 0 })
}

@Composable
private fun TrackToggleRow(
    label: String,
    supporting: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(enabled = enabled) { onChecked(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) { Text(label, fontWeight = FontWeight.Medium); Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Switch(checked, onChecked, enabled = enabled)
    }
}

private fun TrackProjection.addEntryLabel(): String = "Add ${primaryField.name}"

private fun TrackProjection.identityKey(values: Map<String, TrackValueDraft>): String? {
    if (primaryFields.isEmpty() || primaryFields.any { values[it.uuid]?.isBlankFor(it.type) != false }) return null
    return primaryFields.joinToString("\u001f") { field ->
        val value = requireNotNull(values[field.uuid])
        when (field.type) {
            TrackFieldType.ShortText, TrackFieldType.LongText -> value.textValue.orEmpty()
            TrackFieldType.Number -> "${value.enteredNumber} ${value.enteredUnitId.orEmpty()}"
            TrackFieldType.SingleChoice -> options.firstOrNull { it.uuid == value.choiceOptionUuid }?.label.orEmpty()
            TrackFieldType.Scale -> value.scaleValue?.let(::formatTrackScaleValue).orEmpty()
            TrackFieldType.Date -> value.dateValue?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)).orEmpty()
            TrackFieldType.YesNo -> value.booleanValue?.toString().orEmpty()
        }.trim().lowercase()
    }
}

internal fun TrackProjection.conditionFieldName(condition: TrackCondition): String = when (condition.fieldUuid) {
    TRACK_ENTRY_DATE_CONDITION_UUID -> "Entry Date"
    else -> fields.firstOrNull { it.uuid == condition.fieldUuid }?.name ?: "Missing Field"
}

private fun TrackProjection.entrySearchText(entry: TrackEntryProjection): String = buildList {
    add(primaryText(entry))
    add(track.name)
    fields.forEach { field -> add(formattedValue(entry, field)) }
}.joinToString(" ")

internal fun TrackProjection.sortedEntries(
    entries: List<TrackEntryProjection>,
    sort: TrackSort,
    field: TrackField?,
    direction: SortDirection,
): List<TrackEntryProjection> {
    if (field != null) return entries.sortedWith { first, second -> compareByField(first, second, field, direction) }
    val ascending = direction == SortDirection.Ascending
    return when (sort) {
        TrackSort.EntryDate -> entries.sortedWith(
            if (ascending) {
                compareBy<TrackEntryProjection> { it.entry.entryDate }.thenBy { it.entry.createdAtMillis }
            } else {
                compareByDescending<TrackEntryProjection> { it.entry.entryDate }.thenByDescending { it.entry.createdAtMillis }
            },
        )
        TrackSort.Identity -> entries.sortedWith(
            if (ascending) compareBy { primaryText(it).lowercase() }
            else compareByDescending { primaryText(it).lowercase() },
        )
        TrackSort.Created -> entries.sortedWith(
            if (ascending) compareBy { it.entry.createdAtMillis }
            else compareByDescending { it.entry.createdAtMillis },
        )
    }
}

/** Every configured Field is a valid sort key, including individual Entry
 * Identity Fields and long text. Identity remains available as a composite
 * built-in sort, while selecting an Identity Field sorts by that Field alone. */
internal fun TrackProjection.sortableFields(): List<TrackField> = fields.sortedBy(TrackField::position)

private fun TrackProjection.compareByField(
    first: TrackEntryProjection,
    second: TrackEntryProjection,
    field: TrackField,
    direction: SortDirection,
): Int {
    val firstValue = first.value(field.id)
    val secondValue = second.value(field.id)
    val comparison = when (field.type) {
        TrackFieldType.ShortText, TrackFieldType.LongText -> compareTrackFieldValues(
            firstValue?.textValue?.trim()?.takeIf(String::isNotEmpty),
            secondValue?.textValue?.trim()?.takeIf(String::isNotEmpty),
            direction,
        ) { left, right -> left.compareTo(right, ignoreCase = true) }
        TrackFieldType.Number -> compareTrackFieldValues(firstValue?.canonicalNumber, secondValue?.canonicalNumber, direction, Double::compareTo)
        TrackFieldType.SingleChoice -> compareTrackFieldValues(
            options.firstOrNull { it.id == firstValue?.choiceOptionId }?.label?.trim()?.takeIf(String::isNotEmpty),
            options.firstOrNull { it.id == secondValue?.choiceOptionId }?.label?.trim()?.takeIf(String::isNotEmpty),
            direction,
        ) { left, right -> left.compareTo(right, ignoreCase = true) }
        TrackFieldType.Scale -> compareTrackFieldValues(firstValue?.scaleValue, secondValue?.scaleValue, direction, Double::compareTo)
        TrackFieldType.Date -> compareTrackFieldValues(firstValue?.dateValue, secondValue?.dateValue, direction, LocalDate::compareTo)
        TrackFieldType.YesNo -> compareTrackFieldValues(firstValue?.booleanValue, secondValue?.booleanValue, direction, Boolean::compareTo)
    }
    return comparison.takeIf { it != 0 } ?: primaryText(first).compareTo(primaryText(second), ignoreCase = true)
}

/** Missing and blank values stay at the bottom regardless of direction. */
private inline fun <T> compareTrackFieldValues(
    first: T?,
    second: T?,
    direction: SortDirection,
    compare: (T, T) -> Int,
): Int = when {
    first == null && second == null -> 0
    first == null -> 1
    second == null -> -1
    direction == SortDirection.Ascending -> compare(first, second)
    else -> -compare(first, second)
}

private fun TrackProjection.formattedValue(
    entry: TrackEntryProjection,
    field: TrackField,
    units: List<UnitDefinition> = BuiltInUnits.all,
): String {
    val value = entry.value(field.id) ?: return ""
    return when (field.type) {
        TrackFieldType.ShortText, TrackFieldType.LongText -> value.textValue.orEmpty()
        TrackFieldType.Number -> value.enteredNumber?.let { number ->
            val symbol = (units.firstOrNull { it.id == value.enteredUnitId }?.symbol ?: value.enteredUnitId).orEmpty()
            "${number.formatForField(field.precision)}${symbol.takeIf(String::isNotBlank)?.let { " $it" }.orEmpty()}"
        }.orEmpty()
        TrackFieldType.SingleChoice -> options.firstOrNull { it.id == value.choiceOptionId }?.label.orEmpty()
        TrackFieldType.Scale -> value.scaleValue?.let(::formatTrackScaleValue).orEmpty()
        TrackFieldType.Date -> value.dateValue?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)).orEmpty()
        TrackFieldType.YesNo -> value.booleanValue?.let { if (it) "Yes" else "No" }.orEmpty()
    }
}

internal fun TrackFieldType.uiLabel(): String = when (this) {
    TrackFieldType.ShortText -> "Short Text"
    TrackFieldType.LongText -> "Long Text"
    TrackFieldType.Number -> "Number"
    TrackFieldType.SingleChoice -> "Single Choice"
    TrackFieldType.Scale -> "Scale"
    TrackFieldType.Date -> "Date"
    TrackFieldType.YesNo -> "Yes/No"
}

private fun TrackFieldDraft.configurationLabel(): String = when (type) {
    TrackFieldType.Scale -> listOfNotNull(
        scaleMin?.let { minimum -> scaleMax?.let { maximum -> "Scale $minimum–$maximum" } } ?: "Scale",
        "${formatTrackScaleValue(scaleStep)} increment",
    ).joinToString(" · ")
    TrackFieldType.SingleChoice -> "Single Choice · ${options.size} ${if (options.size == 1) "option" else "options"}"
    else -> type.uiLabel()
}

internal fun TrackFieldType.availableOperators(): List<TrackConditionOperator> = when (this) {
    TrackFieldType.ShortText -> listOf(TrackConditionOperator.Is, TrackConditionOperator.IsNot, TrackConditionOperator.Contains, TrackConditionOperator.DoesNotContain, TrackConditionOperator.IsBlank, TrackConditionOperator.IsNotBlank)
    TrackFieldType.LongText -> listOf(TrackConditionOperator.Contains, TrackConditionOperator.DoesNotContain, TrackConditionOperator.IsBlank, TrackConditionOperator.IsNotBlank)
    TrackFieldType.Number, TrackFieldType.Scale -> listOf(TrackConditionOperator.Equals, TrackConditionOperator.NotEqual, TrackConditionOperator.GreaterThan, TrackConditionOperator.AtLeast, TrackConditionOperator.LessThan, TrackConditionOperator.AtMost, TrackConditionOperator.Between, TrackConditionOperator.IsBlank, TrackConditionOperator.IsNotBlank)
    TrackFieldType.SingleChoice -> listOf(TrackConditionOperator.Is, TrackConditionOperator.IsNot, TrackConditionOperator.IsOneOf, TrackConditionOperator.IsBlank, TrackConditionOperator.IsNotBlank)
    TrackFieldType.Date -> listOf(TrackConditionOperator.On, TrackConditionOperator.Before, TrackConditionOperator.OnOrBefore, TrackConditionOperator.After, TrackConditionOperator.OnOrAfter, TrackConditionOperator.Between, TrackConditionOperator.IsBlank, TrackConditionOperator.IsNotBlank)
    TrackFieldType.YesNo -> listOf(TrackConditionOperator.IsYes, TrackConditionOperator.IsNo, TrackConditionOperator.IsUnanswered, TrackConditionOperator.IsAnswered)
}

internal fun TrackConditionOperator.uiLabel(): String = when (this) {
    TrackConditionOperator.Is -> "is"
    TrackConditionOperator.IsNot -> "is not"
    TrackConditionOperator.Contains -> "contains"
    TrackConditionOperator.DoesNotContain -> "does not contain"
    TrackConditionOperator.IsBlank -> "is blank"
    TrackConditionOperator.IsNotBlank -> "is not blank"
    TrackConditionOperator.Equals -> "equals"
    TrackConditionOperator.NotEqual -> "does not equal"
    TrackConditionOperator.GreaterThan -> "is greater than"
    TrackConditionOperator.AtLeast -> "is at least"
    TrackConditionOperator.LessThan -> "is less than"
    TrackConditionOperator.AtMost -> "is at most"
    TrackConditionOperator.Between -> "is between"
    TrackConditionOperator.IsOneOf -> "is one of"
    TrackConditionOperator.On -> "is on"
    TrackConditionOperator.Before -> "is before"
    TrackConditionOperator.OnOrBefore -> "is on or before"
    TrackConditionOperator.After -> "is after"
    TrackConditionOperator.OnOrAfter -> "is on or after"
    TrackConditionOperator.IsYes -> "is Yes"
    TrackConditionOperator.IsNo -> "is No"
    TrackConditionOperator.IsUnanswered -> "is unanswered"
    TrackConditionOperator.IsAnswered -> "is answered"
}

internal fun TrackCondition.summaryValue(
    projection: TrackProjection,
    units: List<UnitDefinition> = BuiltInUnits.all,
): String = when {
    textValue != null -> textValue
    choiceOptionUuids.isNotEmpty() -> choiceOptionUuids.mapNotNull { uuid -> projection.options.firstOrNull { it.uuid == uuid }?.label }.joinToString()
    numberValue != null -> {
        val field = projection.fields.firstOrNull { it.uuid == fieldUuid }
        val unit = field?.takeIf { it.type == TrackFieldType.Number }?.let { numberField -> units.firstOrNull { it.id == numberField.unitId } }
        fun display(value: Double): String {
            val converted = unit?.fromCanonical(value) ?: value
            val symbol = unit?.symbol?.takeIf(String::isNotBlank)?.let { " $it" }.orEmpty()
            return converted.formatForField(field?.precision ?: 2) + symbol
        }
        if (secondNumberValue != null) "${display(numberValue)} and ${display(secondNumberValue)}" else display(numberValue)
    }
    dateValue != null -> if (secondDateValue != null) {
        "${dateValue.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))} and ${secondDateValue.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}"
    } else dateValue.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    else -> ""
}

private fun List<Double>.sumOrDash(): String = takeIf { it.isNotEmpty() }?.sum().formatOrDash()
private fun List<Double>.averageOrDash(): String = takeIf { it.isNotEmpty() }?.average().formatOrDash()
private fun Double?.formatOrDash(): String = this?.formatCompact() ?: "—"
private fun Int.withPercentage(total: Int): String = "$this · ${if (total == 0) 0 else this * 100 / total}%"
private fun Double.formatCompact(): String = if (this % 1.0 == 0.0) toLong().toString() else "%.2f".format(this).trimEnd('0').trimEnd('.')
internal fun Double.formatForField(precision: Int): String = String.format(Locale.getDefault(), "%.${precision.coerceIn(0, 6)}f", this)
