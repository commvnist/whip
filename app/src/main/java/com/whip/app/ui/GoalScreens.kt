package com.whip.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whip.app.core.AppSettings
import com.whip.app.core.EntitySaveReceipt
import com.whip.app.core.OperationStatus
import com.whip.app.core.resolveExactLocalTime
import com.whip.app.core.resolveEditedExactInstant
import com.whip.app.core.zoneId
import com.whip.app.domain.Goal
import com.whip.app.domain.GoalHistoryPoint
import com.whip.app.domain.GoalInsightSummary
import com.whip.app.domain.Area
import com.whip.app.domain.CustomIdentityEmoji
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalAggregationPeriod
import com.whip.app.domain.GoalConsistencyPeriod
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalDirection
import com.whip.app.domain.GoalClosureSnapshot
import com.whip.app.domain.GoalElapsedResetEvent
import com.whip.app.domain.GoalMilestoneDraft
import com.whip.app.domain.GoalMilestoneBoundary
import com.whip.app.domain.GoalPaceType
import com.whip.app.domain.GoalProjection
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.GoalType
import com.whip.app.domain.withTypeSemantics
import com.whip.app.domain.ElapsedDisplay
import com.whip.app.domain.ElapsedDisplayFormat
import com.whip.app.domain.ElapsedDisplayUnit
import com.whip.app.domain.elapsedDisplay
import com.whip.app.domain.elapsedDisplayLabel
import com.whip.app.domain.elapsedDisplayValue
import com.whip.app.domain.DEFAULT_GOAL_EMOJI
import com.whip.app.domain.MeasurementEntry
import com.whip.app.domain.MeasurementEntryStatus
import com.whip.app.domain.MeasurementDefinition
import com.whip.app.domain.MeasurementSourceType
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.displayValue
import com.whip.app.domain.buildGoalInsights
import com.whip.app.domain.progressBaseline
import com.whip.app.domain.goalOutcomeKind
import com.whip.app.domain.supportsDimension
import com.whip.app.domain.compatibleAggregations
import com.whip.app.domain.defaultAggregation
import com.whip.app.domain.defaultDirection
import com.whip.app.domain.editableNumericValue
import com.whip.app.domain.plainNumericValue
import com.whip.app.domain.toWhipDoubleOrNull
import com.whip.app.domain.validationErrors
import com.whip.app.domain.measurementBoundary
import com.whip.app.domain.milestoneBoundary
import com.whip.app.domain.mutationBoundary
import com.whip.app.domain.progressBoundary
import com.whip.app.data.GoalDeletionImpact
import com.whip.app.ui.theme.whipColors
import java.time.LocalDate
import java.time.Instant
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

enum class GoalDestination { Active, Completed, Archived, Insights }

private fun GoalDestination.displayLabel(): String = when (this) {
    GoalDestination.Active -> "Goals"
    GoalDestination.Completed -> "History"
    GoalDestination.Archived -> "Archived"
    GoalDestination.Insights -> "Insights"
}

private fun GoalDestination.pageTitle(): String = when (this) {
    GoalDestination.Active -> "Active Goals"
    GoalDestination.Completed -> "Goal History"
    GoalDestination.Archived -> "Archived Goals"
    GoalDestination.Insights -> "Goal Insights"
}

private fun GoalDestination.supportingText(): String = when (this) {
    GoalDestination.Active -> "Progress toward your goals."
    GoalDestination.Completed -> "Completed and abandoned goals."
    GoalDestination.Archived -> "Saved goals, ready to restore."
    GoalDestination.Insights -> "Trends, pace, and data quality."
}

@Composable
fun GoalAreaContent(
    onBackToSource: (() -> Unit)? = null,
    state: GoalUiState,
    editorState: GoalUiState = state,
    innerPadding: PaddingValues,
    viewModel: GoalViewModel,
    modifier: Modifier = Modifier,
    editorModifier: Modifier = modifier,
    createRequested: Boolean = false,
    recordGoalIdRequest: Long? = null,
    resetElapsedGoalIdRequest: Long? = null,
    onExternalRequestConsumed: () -> Unit = {},
    openGoalIdRequest: Long? = null,
    onOpenGoalRequestConsumed: () -> Unit = {},
    editGoalIdRequest: Long? = null,
    onEditGoalRequestConsumed: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    areas: List<Area> = emptyList(),
    defaultAreaId: String? = null,
    onCreateArea: (String, Long?, (Result<String>) -> Unit) -> Unit = { _, _, _ -> },
    onCreateCustomUnit: CreateCustomUnitAction = UnavailableCreateCustomUnitAction,
    customIdentityEmojis: List<CustomIdentityEmoji> = emptyList(),
    onSaveIdentityEmoji: IdentityEmojiSaveActions? = null,
    onRemoveSavedIdentityEmoji: (String) -> Unit = {},
    areaScopeLabel: String? = null,
    onShowAllAreasForReorder: () -> Unit = {},
    onAreaChanged: (EntitySaveReceipt) -> Unit = {},
    destinationState: MutableState<GoalDestination>? = null,
    showWorkspace: Boolean = true,
    onReorderModeChange: (Boolean) -> Unit = {},
    reorderDismissRequest: Int = 0,
    mutationRequestNamespace: String = "goal-workspace",
    completeGoalIdRequest: Long? = null,
) {
    val goalCelebration = LocalGoalCelebration.current
    val localDestinationState = rememberSaveable { mutableStateOf(GoalDestination.Active) }
    val activeDestinationState = destinationState ?: localDestinationState
    var destination by activeDestinationState
    var archiveReturn by rememberSaveable { mutableStateOf(GoalDestination.Active) }
    val pages = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val inspectorPages = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    BackHandler(enabled = showWorkspace && destination == GoalDestination.Archived) { if (onBackToSource != null) onBackToSource() else destination = archiveReturn }
    if (state.loading || state.errorMessage != null) {
        if (showWorkspace) Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            DestinationTabBar(
                selected = destination.takeUnless { it == GoalDestination.Archived } ?: archiveReturn,
                destinations = listOf(GoalDestination.Active, GoalDestination.Completed, GoalDestination.Insights),
                onSelect = { destination = it },
                label = GoalDestination::displayLabel,
                compactLabel = GoalDestination::displayLabel,
                testTagPrefix = "goal-destination",
                barTestTag = "goal-workspace-navigation",
            )
            WhipWorkspaceHeader(domainLoadTitle("goals", state.errorMessage))
            DomainLoadContent("goals", PaddingValues(), state.errorMessage, viewModel::retryLoading)
        }
        return
    }
    var creating by rememberSaveable { mutableStateOf(false) }
    var editingGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var recordingGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var actionsGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingMeasurementGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingMeasurementId by rememberSaveable { mutableStateOf<String?>(null) }
    var resettingElapsedGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var completingGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var manageOrder by rememberSaveable { mutableStateOf(false) }
    var deleteCandidateGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var templatesOpen by rememberSaveable { mutableStateOf(false) }
    var templateDraft by rememberSaveable { mutableStateOf<GoalDraft?>(null) }
    val editorProjectionById = (editorState.active + editorState.completed + editorState.archived)
        .associateBy { it.goal.id }
    val liveEditing = editingGoalId?.let(editorProjectionById::get)
    var editingSnapshot by rememberSaveable(editingGoalId) { mutableStateOf(liveEditing) }
    LaunchedEffect(editingGoalId, liveEditing) {
        if (editingGoalId != null && editingSnapshot == null) editingSnapshot = liveEditing
    }
    val editing = editingSnapshot
    val liveRecording = recordingGoalId?.let(editorProjectionById::get)
    var recordingSnapshot by rememberSaveable(recordingGoalId) { mutableStateOf(liveRecording) }
    LaunchedEffect(recordingGoalId, liveRecording) {
        if (recordingGoalId != null && recordingSnapshot == null) recordingSnapshot = liveRecording
    }
    val recording = recordingSnapshot
    val liveActions = actionsGoalId?.let(editorProjectionById::get)
    var actionsSnapshot by rememberSaveable(actionsGoalId) { mutableStateOf(liveActions) }
    var refreshActionsAfterChild by rememberSaveable(actionsGoalId) { mutableStateOf(false) }
    var actionRefreshError by rememberSaveable(actionsGoalId) { mutableStateOf<String?>(null) }
    LaunchedEffect(actionsGoalId, liveActions) {
        if (actionsGoalId != null && actionsSnapshot == null) actionsSnapshot = liveActions
    }
    LaunchedEffect(actionsGoalId, refreshActionsAfterChild) {
        if (!refreshActionsAfterChild) return@LaunchedEffect
        val previous = actionsSnapshot
        if (previous == null) {
            refreshActionsAfterChild = false
            return@LaunchedEffect
        }
        actionRefreshError = null
        try {
            val saved = viewModel.currentGoalForInspector(previous.goal.id, previous.goal.uuid)
            actionsSnapshot = previous.copy(goal = saved)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            actionRefreshError = "Saved, but the details could not refresh. Close and reopen this Goal. ${error.message.orEmpty()}"
        } finally {
            refreshActionsAfterChild = false
        }
    }
    // Milestone writes leave the Goal definition unchanged. Refresh their live outcome
    // without accepting a replacement definition behind an open inspector.
    val actions = liveActions?.takeIf {
        it.goal.mutationBoundary() == actionsSnapshot?.goal?.mutationBoundary()
    } ?: actionsSnapshot
    val liveEditingMeasurementProjection = editingMeasurementGoalId?.let(editorProjectionById::get)
    val liveEditingMeasurementEntry = liveEditingMeasurementProjection?.let { projection ->
        editingMeasurementId?.let { id -> projection.entries.firstOrNull { it.id == id } }
    }
    var editingMeasurementProjectionSnapshot by rememberSaveable(editingMeasurementGoalId, editingMeasurementId) {
        mutableStateOf(liveEditingMeasurementProjection)
    }
    var editingMeasurementEntrySnapshot by rememberSaveable(editingMeasurementGoalId, editingMeasurementId) {
        mutableStateOf(liveEditingMeasurementEntry)
    }
    LaunchedEffect(
        editingMeasurementGoalId,
        editingMeasurementId,
        liveEditingMeasurementProjection,
        liveEditingMeasurementEntry,
    ) {
        if (editingMeasurementGoalId != null && editingMeasurementProjectionSnapshot == null) {
            editingMeasurementProjectionSnapshot = liveEditingMeasurementProjection
        }
        if (editingMeasurementId != null && editingMeasurementEntrySnapshot == null) {
            editingMeasurementEntrySnapshot = liveEditingMeasurementEntry
        }
    }
    val editingMeasurement = editingMeasurementProjectionSnapshot?.let { projection ->
        editingMeasurementEntrySnapshot?.let { entry -> projection to entry }
    }
    val liveResettingElapsed = resettingElapsedGoalId?.let(editorProjectionById::get)
    var resettingElapsedSnapshot by rememberSaveable(resettingElapsedGoalId) { mutableStateOf(liveResettingElapsed) }
    LaunchedEffect(resettingElapsedGoalId, liveResettingElapsed) {
        if (resettingElapsedGoalId != null && resettingElapsedSnapshot == null) {
            resettingElapsedSnapshot = liveResettingElapsed
        }
    }
    val resettingElapsed = resettingElapsedSnapshot
    val liveCompleting = completingGoalId?.let(editorProjectionById::get)
    var completingSnapshot by rememberSaveable(completingGoalId) { mutableStateOf(liveCompleting) }
    LaunchedEffect(completingGoalId, liveCompleting) {
        if (completingGoalId != null && completingSnapshot == null) completingSnapshot = liveCompleting
    }
    val completing = completingSnapshot
    val liveDeleteCandidate = deleteCandidateGoalId?.let(editorProjectionById::get)
    var deleteCandidateSnapshot by rememberSaveable(deleteCandidateGoalId) { mutableStateOf(liveDeleteCandidate) }
    LaunchedEffect(deleteCandidateGoalId, liveDeleteCandidate) {
        if (deleteCandidateGoalId != null && deleteCandidateSnapshot == null) {
            deleteCandidateSnapshot = liveDeleteCandidate
        }
    }
    val deleteCandidate = deleteCandidateSnapshot
    val editorSaveState by viewModel.editorSaveState.collectAsStateWithLifecycle()
    val editorSaveCoordinator = rememberEntitySaveCoordinator(
        state = editorSaveState,
        consume = viewModel::consumeEditorSaveResult,
        key = editingGoalId ?: if (creating) "creating-goal" else "no-goal-editor",
        onPersisted = { receipt ->
            onAreaChanged(receipt)
            if (editingGoalId == actionsGoalId && actionsGoalId != null) refreshActionsAfterChild = true
            creating = false
            editingGoalId = null
            templateDraft = null
        },
    )
    val authoredMutationSurfaceOpen = actionsGoalId != null ||
        recordingGoalId != null ||
        editingMeasurementGoalId != null ||
        resettingElapsedGoalId != null ||
        completingGoalId != null ||
        deleteCandidateGoalId != null
    val authoredMutationState by viewModel.authoredMutationState.collectAsStateWithLifecycle()
    val goalDeletionImpact by viewModel.goalDeletionImpact.collectAsStateWithLifecycle()
    val operationStatus by viewModel.operationStatus.collectAsStateWithLifecycle()
    val authoredMutationCoordinator = if (authoredMutationSurfaceOpen) {
        rememberPersistenceRequestCoordinator(
            state = authoredMutationState,
            consume = viewModel::consumeAuthoredMutationResult,
            key = mutationRequestNamespace,
            requestNamespace = mutationRequestNamespace,
            onPersisted = { receipt ->
                if (resettingElapsedGoalId == actionsGoalId && actionsGoalId != null) refreshActionsAfterChild = true
                else if (receipt.kind !in setOf(GoalMutationKind.MilestoneChanged, GoalMutationKind.ProgressRecorded,
                        GoalMutationKind.ProgressUpdated, GoalMutationKind.ProgressDeleted)) actionsGoalId = null
                recordingGoalId = null
                editingMeasurementGoalId = null
                editingMeasurementId = null
                resettingElapsedGoalId = null
                completingGoalId = null
                deleteCandidateGoalId = null
                viewModel.clearPermanentDeletionPreview()
                if (receipt.kind == GoalMutationKind.LifecycleChanged && receipt.newStatus == GoalStatus.Completed) {
                    goalCelebration?.complete(
                        viewModel.defaultSettings(),
                        editorProjectionById[receipt.goalId]?.goal?.name ?: "Goal",
                    )
                }
            },
        )
    } else null
    val elapsedNowMillis = state.nowMillis
    LaunchedEffect(createRequested, recordGoalIdRequest, resetElapsedGoalIdRequest, completeGoalIdRequest, editorState.active) {
        if (createRequested) creating = true
        recordGoalIdRequest?.let { requestedId ->
            val requested = editorState.active.firstOrNull {
                it.goal.id == requestedId && !it.goal.archived && it.goal.status == GoalStatus.Active
            }
            if (requested == null) {
                viewModel.reportUnavailable("That Goal is no longer active. Open Goals to choose an available Goal.")
            } else if (requested.goal.type == GoalType.ElapsedSince || requested.goal.type == GoalType.WeightedMilestones) {
                viewModel.reportUnavailable("That Goal does not use progress entries. Open it to use its available action.")
            } else {
                recordingGoalId = requestedId
            }
        }
        resetElapsedGoalIdRequest?.let { requestedId ->
            val requested = editorState.active.firstOrNull {
                it.goal.id == requestedId && !it.goal.archived && it.goal.status == GoalStatus.Active &&
                    it.goal.type == GoalType.ElapsedSince
            }
            if (requested == null) {
                viewModel.reportUnavailable("That elapsed-time Goal is no longer active. Open Goals to choose an available Goal.")
            } else {
                destination = GoalDestination.Active
                resettingElapsedGoalId = requestedId
            }
        }
        completeGoalIdRequest?.let { requestedId ->
            val requested = editorState.active.firstOrNull {
                it.goal.id == requestedId && !it.goal.archived && it.goal.status == GoalStatus.Active
            }
            if (requested == null) viewModel.reportUnavailable("That Goal is no longer active. Open Goals to choose an available Goal.")
            else completingGoalId = requestedId
        }
        if (createRequested || recordGoalIdRequest != null || resetElapsedGoalIdRequest != null || completeGoalIdRequest != null) {
            onExternalRequestConsumed()
        }
    }
    LaunchedEffect(openGoalIdRequest, editorState.active, editorState.completed, editorState.archived) {
        val requestedId = openGoalIdRequest ?: return@LaunchedEffect
        val projection = (editorState.active + editorState.completed + editorState.archived)
            .firstOrNull { it.goal.id == requestedId }
        if (projection == null) {
            viewModel.reportUnavailable("That Goal is no longer available.")
            onOpenGoalRequestConsumed()
            return@LaunchedEffect
        }
        destination = when {
            projection in editorState.completed -> GoalDestination.Completed
            projection in editorState.archived -> GoalDestination.Archived
            else -> GoalDestination.Active
        }
        actionsGoalId = projection.goal.id
        onOpenGoalRequestConsumed()
    }
    LaunchedEffect(editGoalIdRequest, editorState.active, editorState.completed, editorState.archived) {
        val requestedId = editGoalIdRequest ?: return@LaunchedEffect
        val projection = (editorState.active + editorState.completed + editorState.archived)
            .firstOrNull { it.goal.id == requestedId }
        if (projection == null) {
            viewModel.reportUnavailable("That Goal is no longer available to edit.")
            onEditGoalRequestConsumed()
            return@LaunchedEffect
        }
        destination = when {
            projection in editorState.completed -> GoalDestination.Completed
            projection in editorState.archived -> GoalDestination.Archived
            else -> GoalDestination.Active
        }
        editingGoalId = projection.goal.id
        onEditGoalRequestConsumed()
    }
    val list = when (destination) {
        GoalDestination.Active, GoalDestination.Insights -> state.active
        GoalDestination.Completed -> state.completed
        GoalDestination.Archived -> state.archived
    }

    BackHandler(enabled = showWorkspace && manageOrder) { manageOrder = false }
    LaunchedEffect(manageOrder) { onReorderModeChange(manageOrder) }
    LaunchedEffect(reorderDismissRequest) {
        if (reorderDismissRequest > 0) manageOrder = false
    }
    if (showWorkspace) Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        DestinationTabBar(
            selected = destination.takeUnless { it == GoalDestination.Archived } ?: archiveReturn,
            destinations = listOf(GoalDestination.Active, GoalDestination.Completed, GoalDestination.Insights),
            onSelect = {
                manageOrder = false
                destination = it
            },
            label = GoalDestination::displayLabel,
            compactLabel = GoalDestination::displayLabel,
            testTagPrefix = "goal-destination",
            barTestTag = "goal-workspace-navigation",
        )
        WhipWorkspaceHeader(
            summary = if (manageOrder) "Reordering Goals" else when (destination) {
                GoalDestination.Active -> "${quantityLabel(list.size, "goal")} · Active & paused"
                GoalDestination.Completed -> "${quantityLabel(list.size, "outcome")} · Completed & abandoned"
                GoalDestination.Insights -> "Trends across ${quantityLabel(list.size, "ongoing goal")}"
                GoalDestination.Archived -> "Archived Goals"
            },
            onBack = { if (onBackToSource != null) onBackToSource() else destination = archiveReturn }.takeIf { destination == GoalDestination.Archived },
        ) {
            if (manageOrder) WhipTextButton(onClick = { manageOrder = false }) { Text("Done") }
            else WhipWorkspaceMore("More Goal Actions") { close ->
                WhipMenuItem("Browse Templates", onClick = { close(); templatesOpen = true })
                if (destination != GoalDestination.Archived) WhipMenuItem("Archived Goals", onClick = {
                    close(); archiveReturn = destination; destination = GoalDestination.Archived
                })
                if (destination == GoalDestination.Active && (list.size > 1 || areaScopeLabel != null)) WhipMenuItem(if (areaScopeLabel == null) "Reorder Goals" else "Show All Areas & Reorder", onClick = {
                    close(); if (areaScopeLabel != null) onShowAllAreasForReorder(); manageOrder = true
                })
            }
        }
        pages.SaveableStateProvider(destination.name) {
        if (destination == GoalDestination.Insights) {
            GoalInsightsContent(
                projections = list,
                customUnits = state.customUnits,
                innerPadding = WhipPageContentPadding,
                nowMillis = state.nowMillis,
                zoneId = state.activeZoneId,
                through = state.currentDate,
                onOpen = { actionsGoalId = it.goal.id },
            )
        } else WhipReorderLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = rememberLazyListState(),
            contentPadding = WhipPageContentPadding,
            verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
        ) {


            if (list.isEmpty()) item {
                val firstUse = state.active.isEmpty() && state.completed.isEmpty() && state.archived.isEmpty()
                WhipEmptyState(
                    title = when {
                        firstUse && destination == GoalDestination.Active && areaScopeLabel == null -> "Start Your First Goal"
                        destination == GoalDestination.Completed -> "No Goal History Yet"
                        destination == GoalDestination.Archived -> "No Archived Goals"
                        else -> "No Active Goals"
                    },
                    supportingText = if (destination == GoalDestination.Active) {
                        if (areaScopeLabel != null) {
                            "No active Goals in $areaScopeLabel. Create one here or change the Area above."
                        } else if (firstUse) {
                            "Choose a template or create an outcome from scratch."
                        } else "Create a goal or start from a template."
                    } else if (destination == GoalDestination.Completed) {
                        areaScopeLabel?.let { "No completed or abandoned Goals in $it." }
                            ?: "Completed and abandoned Goals appear here with their individual status."
                    } else {
                        areaScopeLabel?.let { "No archived Goals in $it." } ?: "Archived Goals remain available here."
                    },
                    primaryActionLabel = "Browse Templates".takeIf { destination == GoalDestination.Active },
                    onPrimaryAction = { templatesOpen = true }.takeIf { destination == GoalDestination.Active },
                    secondaryActionLabel = "Create Goal".takeIf { destination == GoalDestination.Active },
                    onSecondaryAction = { creating = true }.takeIf { destination == GoalDestination.Active },
                )
            }
            items(list, key = { it.goal.id }) { projection ->
                val index = list.indexOfFirst { it.goal.id == projection.goal.id }
                Column {
                    if (
                        manageOrder && destination == GoalDestination.Active &&
                        (index == 0 || list[index - 1].goal.pinned != projection.goal.pinned)
                    ) {
                        WhipGroupHeading(
                            if (projection.goal.pinned) "Pinned Goals" else "Other Goals",
                            compact = true,
                        )
                    }
                    val card: @Composable () -> Unit = {
                        GoalCard(
                        projection,
                        customUnits = state.customUnits,
                        nowMillis = elapsedNowMillis,
                        zoneId = state.activeZoneId,
                        onOpen = { actionsGoalId = projection.goal.id },
                        onEdit = { editingGoalId = projection.goal.id },
                        onRecord = { recordingGoalId = projection.goal.id },
                        onToggleMilestone = viewModel::toggleMilestone,
                        onResetElapsed = { resettingElapsedGoalId = projection.goal.id },
                        onComplete = { completingGoalId = projection.goal.id },
                        reorderMode = manageOrder,
                        )
                    }
                    if (manageOrder && destination == GoalDestination.Active && areaScopeLabel == null) {
                        val partition = list.filter { it.goal.pinned == projection.goal.pinned }
                        val partitionIndex = partition.indexOfFirst { it.goal.id == projection.goal.id }
                        val reorderInteraction = rememberWhipReorderInteractionState()
                        Row(
                            modifier = Modifier.whipReorderItem(
                                reorderInteraction,
                                layoutPosition = partitionIndex + 1,
                                layoutScope = "goal-browse-${projection.goal.pinned}",
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            WhipReorderHandle(
                                label = projection.goal.name,
                                canMovePrevious = partitionIndex > 0,
                                canMoveNext = partitionIndex in 0 until partition.lastIndex,
                                position = partitionIndex + 1,
                                total = partition.size,
                                interactionState = reorderInteraction,
                                moveWholeItem = true,
                                layoutScope = "goal-browse-${projection.goal.pinned}",
                                reserveWhenUnavailable = true,
                                onMove = { delta ->
                                    val moved = moveListItem(partition, partitionIndex, delta)
                                    val iterator = moved.iterator()
                                    viewModel.reorder(list.map { item ->
                                        if (item.goal.pinned == projection.goal.pinned) iterator.next().goal.id else item.goal.id
                                    })
                                },
                            )
                            Box(Modifier.weight(1f)) { card() }
                        }
                    } else card()
                }
            }
        }
    }
    }
    if (creating || editing != null) {
        GoalEditorDialog(
            modifier = editorModifier,
            projection = editing,
            initialDraft = templateDraft.takeIf { editing == null },
            today = editorState.currentDate,
            activeZoneId = editorState.activeZoneId,
            nowMillis = editorState.nowMillis,
            customUnits = editorState.customUnits,
            defaults = viewModel.defaultSettings(),
            areas = areas,
            defaultAreaId = defaultAreaId,
            onCreateArea = onCreateArea,
            onCreateCustomUnit = onCreateCustomUnit,
            customIdentityEmojis = customIdentityEmojis,
            onSaveIdentityEmoji = onSaveIdentityEmoji,
            onRemoveSavedIdentityEmoji = onRemoveSavedIdentityEmoji,
            saving = editorSaveCoordinator.saving,
            persistenceError = editorSaveCoordinator.errorMessage,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onDismiss = {
                editorSaveCoordinator.clear()
                creating = false
                editingGoalId = null
                templateDraft = null
            },
            onSave = { draft ->
                val requestId = editorSaveCoordinator.begin()
                if (requestId != null) {
                    if (!viewModel.saveGoal(
                            editing?.goal?.id,
                            draft,
                            expectedBoundary = editing?.goal?.mutationBoundary(),
                            requestId = requestId,
                        )
                    ) {
                        editorSaveCoordinator.finishFailure("Another Goal save is already finishing.")
                    }
                }
            },
        )
    }
    if (templatesOpen) {
        GoalTemplateDialog(
            today = state.currentDate,
            defaults = viewModel.defaultSettings(),
            onDismiss = { templatesOpen = false },
            onChoose = { draft ->
                templateDraft = draft
                templatesOpen = false
                creating = true
            },
        )
    }
    actions?.takeIf { editingGoalId == null && resettingElapsedGoalId == null }?.let { projection ->
        val mutationCoordinator = authoredMutationCoordinator ?: return@let
        val boundary = projection.goal.mutationBoundary()
        fun beginMutation(action: (String) -> Boolean) {
            val requestId = mutationCoordinator.begin() ?: return
            if (!action(requestId)) {
                mutationCoordinator.finishFailure("Another Goal change is already finishing.")
            }
        }
        inspectorPages.SaveableStateProvider("goal-${projection.goal.id}") {
        GoalActionsDialog(
            projection,
            modifier = modifier,
            zoneId = editorState.activeZoneId,
            nowMillis = editorState.nowMillis,
            through = editorState.currentDate,
            customUnits = editorState.customUnits,
            onDismiss = {
                mutationCoordinator.clear()
                inspectorPages.removeState("goal-${projection.goal.id}")
                actionsGoalId = null
            },
            onEditMeasurement = { entry ->
                mutationCoordinator.leaveGoalActionSurface {
                    editingMeasurementGoalId = projection.goal.id
                    editingMeasurementId = entry.id
                }
            },
            onRecordProgress = {
                mutationCoordinator.leaveGoalActionSurface {
                    recordingGoalId = projection.goal.id
                }
            },
            onResetElapsed = {
                mutationCoordinator.leaveGoalActionSurface {
                    resettingElapsedGoalId = projection.goal.id
                }
            },
            onEdit = {
                mutationCoordinator.leaveGoalActionSurface {
                    editingGoalId = projection.goal.id
                }
            },
            onToggleMilestone = { milestone, completed ->
                beginMutation { viewModel.toggleMilestone(milestone, completed, it) }
            },
            onDuplicate = { beginMutation { viewModel.duplicate(boundary, it) } },
            onPin = { beginMutation { viewModel.setPinned(boundary, !projection.goal.pinned, it) } },
            onPause = {
                beginMutation {
                    viewModel.setStatus(
                        boundary,
                        if (projection.goal.status == GoalStatus.Paused) GoalStatus.Active else GoalStatus.Paused,
                        it,
                    )
                }
            },
            onComplete = { beginMutation { viewModel.setStatus(boundary, GoalStatus.Completed, it) } },
            onAbandon = { beginMutation { viewModel.setStatus(boundary, GoalStatus.Abandoned, it) } },
            onReopen = { beginMutation { viewModel.setStatus(boundary, GoalStatus.Active, it) } },
            onArchive = { beginMutation { viewModel.setArchived(boundary, !projection.goal.archived, it) } },
            onDelete = {
                mutationCoordinator.leaveGoalActionSurface {
                    viewModel.preparePermanentDeletion(projection.goal.id)
                    deleteCandidateGoalId = projection.goal.id
                    actionsGoalId = null
                }
            },
            mutationSaving = mutationCoordinator.saving || refreshActionsAfterChild,
            mutationError = mutationCoordinator.errorMessage ?: actionRefreshError,
        )
        }
    }
    resettingElapsed?.let { projection ->
        val mutationCoordinator = authoredMutationCoordinator ?: return@let
        val boundary = projection.goal.mutationBoundary()
        ElapsedGoalResetDialog(
            goal = projection.goal,
            zoneId = editorState.activeZoneId,
            nowMillis = editorState.nowMillis,
            saving = mutationCoordinator.saving,
            persistenceError = mutationCoordinator.errorMessage,
            onDismiss = {
                mutationCoordinator.clear()
                resettingElapsedGoalId = null
            },
            onReset = { instant ->
                val requestId = mutationCoordinator.begin()
                if (requestId != null && !viewModel.resetElapsedStart(boundary, instant, requestId)) {
                    mutationCoordinator.finishFailure("Another Goal change is already finishing.")
                }
            },
            onResetNow = {
                val requestId = mutationCoordinator.begin()
                if (requestId != null && !viewModel.resetElapsedStartToNow(boundary, requestId)) {
                    mutationCoordinator.finishFailure("Another Goal change is already finishing.")
                }
            },
        )
    }
    completing?.let { projection ->
        val mutationCoordinator = authoredMutationCoordinator ?: return@let
        GoalCompletionDialog(
            projection = projection,
            customUnits = editorState.customUnits,
            nowMillis = editorState.nowMillis,
            zoneId = editorState.activeZoneId,
            saving = mutationCoordinator.saving,
            persistenceError = mutationCoordinator.errorMessage,
            onDismiss = {
                mutationCoordinator.clear()
                completingGoalId = null
            },
            onComplete = {
                val requestId = mutationCoordinator.begin()
                if (requestId != null && !viewModel.setStatus(projection.goal.mutationBoundary(), GoalStatus.Completed, requestId)) {
                    mutationCoordinator.finishFailure("Another Goal change is already finishing.")
                }
            },
        )
    }
    recording?.let { projection ->
        val mutationCoordinator = authoredMutationCoordinator ?: return@let
        val boundary = projection.goal.progressBoundary()
        GoalMeasurementDialog(
            projection,
            editorState.currentDate,
            entry = null,
            customUnits = editorState.customUnits,
            saving = mutationCoordinator.saving,
            persistenceError = mutationCoordinator.errorMessage,
            onDismiss = {
                mutationCoordinator.clear()
                recordingGoalId = null
            },
            onRecord = { value, date, note ->
                val requestId = mutationCoordinator.begin()
                if (requestId != null && !viewModel.record(boundary, value, date, note, requestId)) {
                    mutationCoordinator.finishFailure("Another Goal history change is already finishing.")
                }
            },
        )
    }
    editingMeasurement?.let { (projection, entry) ->
        val mutationCoordinator = authoredMutationCoordinator ?: return@let
        val boundary = projection.goal.measurementBoundary(entry)
        GoalMeasurementDialog(
            projection = projection,
            today = editorState.currentDate,
            entry = entry,
            customUnits = editorState.customUnits,
            saving = mutationCoordinator.saving,
            persistenceError = mutationCoordinator.errorMessage,
            onDismiss = {
                mutationCoordinator.clear()
                editingMeasurementGoalId = null
                editingMeasurementId = null
            },
            onRecord = { value, date, note ->
                val requestId = mutationCoordinator.begin()
                if (requestId != null && !viewModel.updateMeasurement(boundary, value, date, note, requestId)) {
                    mutationCoordinator.finishFailure("Another Goal history change is already finishing.")
                }
            },
            onDelete = {
                val requestId = mutationCoordinator.begin()
                if (requestId != null && !viewModel.deleteMeasurement(boundary, requestId)) {
                    mutationCoordinator.finishFailure("Another Goal history change is already finishing.")
                }
            },
        )
    }
    deleteCandidateGoalId?.let { candidateId ->
        val mutationCoordinator = authoredMutationCoordinator ?: return@let
        val impact = goalDeletionImpact?.takeIf { it.goalId == candidateId }
        val previewError = (operationStatus as? OperationStatus.Failed)?.message.takeIf { impact == null }
        GoalPermanentDeleteDialog(
            goalName = impact?.name ?: deleteCandidate?.goal?.name.orEmpty(),
            impact = impact,
            preparing = impact == null && previewError == null,
            saving = mutationCoordinator.saving,
            persistenceError = mutationCoordinator.errorMessage ?: previewError,
            onDismiss = {
                mutationCoordinator.clear()
                viewModel.clearPermanentDeletionPreview()
                deleteCandidateGoalId = null
            },
            onReviewUpdatedImpact = {
                mutationCoordinator.clear()
                viewModel.preparePermanentDeletion(candidateId)
            },
            onConfirm = { reviewedImpact ->
                val requestId = mutationCoordinator.begin()
                if (requestId != null && !viewModel.deletePermanently(
                        candidateId,
                        reviewedImpact.revisionToken,
                        requestId,
                    )
                ) {
                    mutationCoordinator.finishFailure("Another Goal change is already finishing.")
                }
            },
        )
    }
}

internal inline fun EntitySaveCoordinator.leaveGoalActionSurface(transition: () -> Unit) {
    clear()
    transition()
}

internal fun GoalProjection.collectionStatus(
    customUnits: List<UnitDefinition> = emptyList(),
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String {
    val goal = this.goal
    return when {
        terminalSnapshot != null && goal.type == GoalType.ElapsedSince ->
            elapsedDisplayLabel(nowMillis, zoneId) ?: terminalSnapshot.status.inspectorLabel()
        terminalSnapshot != null && goal.type == GoalType.WeightedMilestones ->
            terminalSnapshot.milestoneOutcomeLabel()
                ?: terminalSnapshot.progress?.let { "${formatGoalProgressPercent(it)} complete" }
                ?: terminalSnapshot.status.inspectorLabel()
        goal.type == GoalType.ElapsedSince && goal.elapsedStartMillis != null ->
            elapsedDisplayLabel(nowMillis, zoneId) ?: goal.type.displayLabel()
        goal.type == GoalType.WeightedMilestones ->
            "${milestones.count { it.completed }}/${milestones.size} milestones"
        typedOutcomeReading(customUnits) != null -> requireNotNull(typedOutcomeReading(customUnits))
        progress != null -> "${formatGoalProgressPercent(progress)} complete"
        consistency != null -> with(requireNotNull(consistency)) {
            "$successfulPeriods/$requiredPeriods ${period.periodLabel} periods"
        }
        currentValue != null ->
            "Current ${formatGoalValue(goal.displayValue(currentValue, customUnits), goal.precision)} ${goal.unitId.goalUnitLabel(customUnits)}".trim()
        else -> goal.type.displayLabel()
    }
}

internal fun GoalProjection.offersCompletion(): Boolean =
    !goal.archived && goal.status == GoalStatus.Active && terminalSnapshot == null &&
        goal.type in setOf(GoalType.ReachValue, GoalType.ReduceValue, GoalType.AccumulateTotal,
            GoalType.MeetAverage, GoalType.Consistency, GoalType.WeightedMilestones) &&
        (progress?.let { it.isFinite() && it >= 1.0 } == true ||
            (progress == null && goal.progressBaseline == null && goal.type in setOf(GoalType.ReachValue, GoalType.ReduceValue, GoalType.AccumulateTotal, GoalType.MeetAverage) &&
                currentValue?.let { value -> goal.targetMin?.let { target ->
                    value.isFinite() && if (goal.direction == GoalDirection.Decrease) value <= target else value >= target
                } } == true))

@Composable
fun GoalCard(
    projection: GoalProjection,
    customUnits: List<UnitDefinition> = emptyList(),
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onRecord: () -> Unit,
    onResetElapsed: () -> Unit,
    onToggleMilestone: (GoalMilestoneBoundary, Boolean) -> Unit,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
    reorderMode: Boolean = false,
    onComplete: (() -> Unit)? = null,
) {
    val goal = projection.goal
    val executable = !reorderMode && !goal.archived && goal.status == GoalStatus.Active
    val disclosure = rememberItemDisclosure(itemKey = "goal:${goal.id}")
    val numericSummary = projection.compactNumericReading(customUnits)
    val compactStatus = listOfNotNull(
        projection.goal.status.takeUnless { it == GoalStatus.Active || it == GoalStatus.Archived }?.inspectorLabel(),
        numericSummary ?: projection.collectionStatus(customUnits, nowMillis, zoneId),
        "Target reached".takeIf { projection.offersCompletion() },
    ).joinToString(" · ")
    val elapsedStatus = projection.elapsedDisplayValue(nowMillis, zoneId)
    val primaryLabel = when {
        goal.type == GoalType.ElapsedSince -> "Reset"
        goal.aggregation == GoalAggregation.CompletionCount -> "+1"
        else -> "Log"
    }
    val textMeasurer = rememberTextMeasurer()
    val primaryTextStyle = LocalTextStyle.current.merge(MaterialTheme.typography.labelLarge)
    val primaryWidth = with(LocalDensity.current) {
        val labelWidth = textMeasurer.measure(primaryLabel, style = primaryTextStyle, maxLines = 1).size.width
        // Material merges the inherited style; padding rounds each edge separately in pixels.
        (labelWidth + 2 * 4.dp.roundToPx()).toDp().coerceIn(64.dp, 112.dp)
    }
    val primaryAction: (@Composable () -> Unit)? = when {
        !executable || goal.type == GoalType.WeightedMilestones -> null
        goal.type == GoalType.ElapsedSince -> {{
            ItemPrimaryTextButton(primaryLabel, onResetElapsed,
                Modifier.testTag("goal-card-reset-${goal.id}").semantics { contentDescription = "Reset timer for ${goal.name}" })
        }}
        else -> {{
            ItemPrimaryTextButton(primaryLabel, onRecord,
                Modifier.semantics { contentDescription = if (goal.aggregation == GoalAggregation.CompletionCount) "Record a completion for ${goal.name}" else "Log progress for ${goal.name}" })
        }}
    }
    WhipItemCard(
        modifier = Modifier
            .then(
                if (reorderMode) Modifier
                else Modifier.clickable(onClickLabel = "Open goal details for ${goal.name}", onClick = onOpen),
            )
            .testTag("goal-card-${goal.id}")
            .then(
                if (reorderMode) Modifier
                else Modifier.semantics {
                    contentDescription = "Open goal details for ${goal.name}"
                    stateDescription = compactStatus
                },
            ),
    ) {
        WhipProductivityItemContent(
            itemType = "goal",
            itemName = goal.name,
            emoji = goal.icon,
            identityModifier = Modifier.testTag("goal-icon-${goal.id}"),
            titleModifier = Modifier.testTag("goal-card-title-${goal.id}"),
            primaryActionModifier = Modifier.testTag("goal-primary-action-${goal.id}"),
            editModifier = Modifier.testTag("goal-edit-action-${goal.id}"),
        ) {
            area(goal.areaId, goal.area)
            edit(onEdit.takeUnless { reorderMode })
            details {
                text(
                    text = listOfNotNull(goal.type.displayLabel(), goal.status.takeUnless { it == GoalStatus.Active || it == GoalStatus.Archived }?.inspectorLabel()).joinToString(" · "),
                )
            }
            summary {
                text(
                    text = compactStatus,
                    modifier = Modifier
                        .testTag("goal-card-status-${goal.id}")
                        .then(
                            elapsedStatus?.let {
                                Modifier.semantics(mergeDescendants = true) {
                                    contentDescription = compactStatus
                                }
                            } ?: Modifier,
                        ),
                )
            }
            if (projection.progress != null && goal.type != GoalType.MaintainRange) summaryContent {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { projection.progress.toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.weight(1f).testTag("goal-card-progress-${goal.id}"),
                        color = if (projection.progress >= 1.0) MaterialTheme.whipColors.success else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                    Text(formatGoalProgressPercent(projection.progress), style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.testTag("goal-card-progress-label-${goal.id}"))
                }
            }
            disclosure(expanded = disclosure.expanded, tag = "goal-expand-${goal.id}", onToggle = disclosure.toggle.takeUnless { reorderMode })
            primaryAction(width = primaryWidth, content = primaryAction)

            expandedContent {
                projection.progress?.let { progress ->
                    val progressColor = if (progress >= 1.0) MaterialTheme.whipColors.success else MaterialTheme.whipColors.action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LinearProgressIndicator(
                            progress = { progress.toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.weight(1f),
                            color = progressColor,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        )
                        Text(
                            if (goal.type == GoalType.MaintainRange) {
                                if (projection.terminalSnapshot != null) "${formatGoalProgressPercent(progress)} recorded progress"
                                else if (goal.aggregation == GoalAggregation.TimeInRange) "${formatGoalProgressPercent(progress)} in range"
                                else if (progress >= 1.0) "In range" else "Outside range"
                            } else "${formatGoalProgressPercent(progress)} complete",
                            style = MaterialTheme.typography.labelSmall, color = progressColor,
                        )
                    }
                }
                if (goal.type == GoalType.ElapsedSince) {
                    elapsedStatus?.let { display ->
                        ElapsedGoalMetric(
                            display = display,
                            modifier = Modifier.testTag("goal-card-expanded-status-${goal.id}"),
                        )
                    }
                    when {
                        elapsedStatus != null && goal.elapsedStartMillis != null -> Text(
                            if (projection.terminalSnapshot == null) {
                                "Counting since ${elapsedGoalStartLabel(goal.elapsedStartMillis, zoneId)}"
                            } else {
                                "Recorded when this Goal was ${projection.terminalSnapshot.status.label.lowercase()}."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        projection.terminalSnapshot != null -> Text(
                            "Exact elapsed duration was not stored for this older closure.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else projection.typedOutcomeReading(customUnits)?.let {
                    Text(it)
                    projection.consistencyPeriodReading()?.let { period -> Text(period, style = MaterialTheme.typography.bodySmall) }
                } ?: projection.numericReading(customUnits)?.let { Text(it) } ?: projection.currentValue?.let { canonical ->
                    val current = goal.displayValue(canonical, customUnits)
                    Text("Current: ${formatGoalValue(current, goal.precision)} ${goal.unitId.goalUnitLabel(customUnits)}")
                }
                if (goal.type in setOf(GoalType.ReachValue, GoalType.ReduceValue, GoalType.AccumulateTotal, GoalType.MeetAverage) &&
                    projection.terminalSnapshot == null && goal.progressBaseline == null) {
                    Text("Edit the starting value to show a progress percentage. Recorded values and the target remain available.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val pace = when (projection.onPace) { true -> "On pace"; false -> "Behind pace"; null -> null }
                if (pace != null) Text(pace + (projection.forecastDate?.let { " · forecast ${it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}" } ?: ""), style = MaterialTheme.typography.labelMedium)
                if (projection.terminalSnapshot != null && goal.type == GoalType.WeightedMilestones) {
                    Text(
                        "Closed outcome is frozen. Milestones below show the current saved definition.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!executable) GoalMilestoneChecklist(projection, onToggleMilestone, enabled = false)
                if (goal.description.isNotBlank()) Text(goal.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (executable && goal.type == GoalType.WeightedMilestones) {
            GoalMilestoneChecklist(projection, onToggleMilestone)
        }
        if (executable && onComplete != null) WhipTextButton(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .testTag("goal-card-complete-${goal.id}")
                .semantics { contentDescription = "Complete goal ${goal.name}" },
        ) { Text("Complete Goal") }
    }
}

@Composable
internal fun GoalCompletionDialog(
    projection: GoalProjection,
    customUnits: List<UnitDefinition>,
    nowMillis: Long,
    zoneId: ZoneId,
    saving: Boolean,
    persistenceError: String?,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
) {
    PaneAwareAlertDialog(
        testTag = "goal-completion-dialog",
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Complete ${projection.goal.name}?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PersistenceFailureNotice(persistenceError, testTag = "goal-completion-save-problem")
                Text(projection.inspectorOutcome(nowMillis, customUnits, zoneId))
                if (projection.progress?.let { it < 1.0 } == true) Text(
                    "This Goal has not reached its measured target. You can still complete it with the outcome shown above.",
                )
                Text("Completing saves this outcome in History. Progress entries remain available for correction.")
            }
        },
        confirmButton = {
            WhipTextButton(onClick = onComplete, enabled = !saving, modifier = Modifier.testTag("goal-completion-confirm")) {
                Text(if (saving) "Completing…" else "Complete Goal")
            }
        },
        dismissButton = { WhipTextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } },
        inputBlocked = saving,
        inputBlockedLabel = "Completing Goal",
    )
}

@Composable
private fun GoalMilestoneChecklist(
    projection: GoalProjection,
    onToggleMilestone: (GoalMilestoneBoundary, Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val goal = projection.goal
    projection.milestones.forEach { milestone ->
        val milestoneEditable = enabled && !goal.archived && goal.status == GoalStatus.Active
        val milestoneModifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("goal-milestone-${milestone.id}")
            .toggleable(
                value = milestone.completed,
                enabled = milestoneEditable,
                role = Role.Checkbox,
                onValueChange = {
                    onToggleMilestone(goal.milestoneBoundary(milestone), !milestone.completed)
                },
            )
            .semantics {
                contentDescription = if (!milestoneEditable) {
                    "${milestone.name} is ${if (milestone.completed) "complete" else "not complete"}. Make the Goal active to change milestones"
                } else if (milestone.completed) {
                    "Mark milestone ${milestone.name} incomplete"
                } else "Complete milestone ${milestone.name}"
            }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stacked = maxWidth < 360.dp || LocalDensity.current.fontScale >= 1.5f
            if (stacked) {
                Column(milestoneModifier) {
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            milestone.name,
                            modifier = Modifier.weight(1f),
                            color = completionTextColor(milestone.completed),
                            textDecoration = completionTextDecoration(milestone.completed),
                        )
                        Spacer(Modifier.width(8.dp))
                        WhipCompletionCheckbox(
                            checked = milestone.completed,
                            onCheckedChange = null,
                            modifier = Modifier.clearAndSetSemantics { },
                        )
                    }
                    if (milestone.reward.isNotBlank()) {
                        Text(
                            milestone.reward,
                            modifier = Modifier.padding(end = 56.dp),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            } else {
                Row(milestoneModifier, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        milestone.name,
                        modifier = Modifier.weight(1f),
                        color = completionTextColor(milestone.completed),
                        textDecoration = completionTextDecoration(milestone.completed),
                    )
                    if (milestone.reward.isNotBlank()) {
                        Text(milestone.reward, style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.width(8.dp))
                    WhipCompletionCheckbox(
                        checked = milestone.completed,
                        onCheckedChange = null,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }
            }
        }
    }
}

private fun GoalProjection.numericReading(customUnits: List<UnitDefinition>): String? {
    if (terminalSnapshot != null || goal.type !in setOf(GoalType.ReachValue, GoalType.ReduceValue, GoalType.AccumulateTotal, GoalType.MeetAverage)) return null
    val current = currentValue ?: return null
    val target = goal.targetMin ?: return null
    return "Current ${formatGoalCanonicalValue(current, goal.unitId, goal.precision, customUnits)} → " +
        "target ${formatGoalCanonicalValue(target, goal.unitId, goal.precision, customUnits)}"
}

internal fun GoalProjection.compactNumericReading(customUnits: List<UnitDefinition> = emptyList()): String? {
    if (terminalSnapshot != null || goal.type !in setOf(GoalType.ReachValue, GoalType.ReduceValue, GoalType.AccumulateTotal, GoalType.MeetAverage)) return null
    val target = goal.targetMin ?: return null
    val format = java.text.NumberFormat.getNumberInstance().apply { maximumFractionDigits = goal.precision.coerceIn(0, 6) }
    fun reading(value: Double) = format.format(goal.displayValue(value, customUnits))
    val unit = goal.unitId.goalUnitLabel(customUnits).takeIf(String::isNotBlank)?.let { " $it" }.orEmpty()
    return if (currentValue == null) "No current value · target ${reading(target)}$unit"
        else "${reading(currentValue)} → ${reading(target)}$unit"
}

@Composable
internal fun ElapsedGoalMetric(
    display: ElapsedDisplay,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
) {
    val valueStyle = if (prominent) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodySmall
    val unitStyle = if (prominent) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = display.label() },
        horizontalArrangement = Arrangement.spacedBy(if (prominent) 12.dp else 8.dp),
        verticalArrangement = Arrangement.spacedBy(if (prominent) 4.dp else 2.dp),
    ) {
        display.parts.forEach { part ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    part.value.toString(),
                    style = valueStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    part.unitLabel(),
                    style = unitStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun ElapsedGoalResetDialog(
    goal: Goal,
    zoneId: ZoneId,
    nowMillis: Long,
    onDismiss: () -> Unit,
    onReset: (Instant) -> Unit,
    onResetNow: () -> Unit,
    saving: Boolean = false,
    persistenceError: String? = null,
) {
    val draftZoneId by rememberSaveable(goal.id) { mutableStateOf(zoneId.id) }
    val draftZone = remember(draftZoneId) { ZoneId.of(draftZoneId) }
    val original = Instant.ofEpochMilli(goal.elapsedStartMillis ?: nowMillis).atZone(draftZone)
    var date by rememberSaveable(goal.id) { mutableStateOf(original.toLocalDate()) }
    var minutes by rememberSaveable(goal.id) { mutableIntStateOf(original.hour * 60 + original.minute) }
    var wallTimeEdited by rememberSaveable(goal.id) { mutableStateOf(false) }
    var preferredOffsetSeconds by rememberSaveable(goal.id) { mutableStateOf<Int?>(original.offset.totalSeconds) }
    var datePicker by rememberSaveable(goal.id) { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable(goal.id) { mutableStateOf(false) }
    val resolution = resolveExactLocalTime(date, minutes, draftZone)
    val selectedInstant = resolveEditedExactInstant(
        initialInstant = original.toInstant(),
        wallTimeEdited = wallTimeEdited,
        resolution = resolution,
        preferredOffsetSeconds = preferredOffsetSeconds,
    )
    val now = Instant.ofEpochMilli(nowMillis)
    val inFuture = selectedInstant?.isAfter(now) == true
    fun requestDismiss() {
        if (saving) return
        if (wallTimeEdited) confirmDiscard = true else onDismiss()
    }
    PaneAwareAlertDialog(
        testTag = "elapsed-reset-dialog",
        onDismissRequest = ::requestDismiss,
        title = { Text("Reset ${goal.name}?") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PersistenceFailureNotice(persistenceError, testTag = "elapsed-reset-save-problem")
                Text("Choose the new event time. This replaces the previous counter origin; it does not delete the Goal.")
                Text("Whip time · ${draftZone.id}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                WhipOutlinedButton(enabled = !saving, onClick = { datePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
                }
                ClockPickerButton("Start Time", minutes, { if (it != null) {
                    minutes = it
                    wallTimeEdited = true
                    preferredOffsetSeconds = null
                } })
                WhipOutlinedButton(
                    enabled = !saving,
                    onClick = onResetNow,
                    modifier = Modifier.fillMaxWidth().testTag("elapsed-reset-now"),
                ) { Text("Reset to Now") }
                if (resolution.isGap) {
                    val next = resolution.firstValidDateTimeAfterGap?.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))
                    Text(
                        "That local time does not exist because the clock moves forward.${next?.let { " The next valid time is $it." }.orEmpty()}",
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (wallTimeEdited && resolution.isOverlap) {
                    Text(
                        "That time occurs twice because the clock moves back. Choose which occurrence you mean.",
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    resolution.options.forEach { option ->
                        val selected = preferredOffsetSeconds == option.offset.totalSeconds
                        WhipOutlinedButton(
                            onClick = { preferredOffsetSeconds = option.offset.totalSeconds },
                            modifier = Modifier.fillMaxWidth().testTag("elapsed-reset-overlap-${option.offset.id}"),
                        ) {
                            val index = resolution.options.indexOf(option)
                            Text("${if (index == 0) "First" else "Second"} occurrence · ${option.offset.id}${if (selected) " · Selected" else ""}")
                        }
                    }
                }
                if (inFuture) Text(
                    "Start time cannot be in the future.",
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        confirmButton = {
                WhipTextButton(
                enabled = !saving && selectedInstant != null && !inFuture,
                onClick = { selectedInstant?.let(onReset) },
                modifier = Modifier.testTag("elapsed-reset-confirm"),
            ) { Text(if (saving) "Resetting…" else "Reset to Chosen Time") }
        },
        dismissButton = {
            WhipTextButton(
                enabled = !saving,
                onClick = ::requestDismiss,
                modifier = Modifier.testTag("elapsed-reset-cancel"),
            ) { Text("Cancel") }
        },
        inputBlocked = saving,
        inputBlockedLabel = "Resetting Goal Timer",
    )
    if (datePicker && !saving) WhipDatePickerDialog(date, { datePicker = false }, {
        date = it
        wallTimeEdited = true
        preferredOffsetSeconds = null
        datePicker = false
    })
    if (confirmDiscard && !saving) UnsavedChangesDialog(
        subject = "timer reset",
        onKeepEditing = { confirmDiscard = false },
        onDiscard = {
            confirmDiscard = false
            onDismiss()
        },
    )
}

internal fun elapsedGoalStartLabel(startedMillis: Long, zoneId: ZoneId): String =
    "${Instant.ofEpochMilli(startedMillis).atZone(zoneId).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))} · ${zoneId.id}"

@Composable
private fun GoalInsightsContent(
    projections: List<GoalProjection>,
    customUnits: List<UnitDefinition>,
    innerPadding: PaddingValues,
    nowMillis: Long,
    zoneId: ZoneId,
    through: LocalDate,
    onOpen: (GoalProjection) -> Unit,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().testTag("goal-insights-list"),
        contentPadding = innerPadding,
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
    ) {

        if (projections.isEmpty()) item {
            WhipEmptyState(
                title = "No Goal Insights Yet",
                supportingText = "Insights summarize active Goals in this Area. Open Goals to create a Goal or change the Area above.",
            )
        }
        items(projections, key = { "goal-insight-${it.goal.id}" }) { projection ->
            val insights = remember(projection, through) { buildGoalInsights(projection.goal, projection.entries, projection.milestones, through) }
            WhipItemCard(
                modifier = Modifier
                    .clickable(onClickLabel = "Open ${projection.goal.name}") { onOpen(projection) }
                    .testTag("goal-insight-${projection.goal.id}"),
            ) {
                    WhipProductivityItemContent(
                        itemType = "goal",
                        itemName = projection.goal.name,
                        emoji = projection.goal.icon,
                        identityModifier = Modifier.testTag("goal-insight-icon-${projection.goal.id}"),
                    ) {
                        area(projection.goal.areaId, projection.goal.area)
                        details {
                            text(
                                text = projection.goal.type.displayLabel(),
                            )
                        }
                    }
                    val chartValues = insights.points.mapNotNull { it.progress ?: it.canonicalValue }
                    if (projection.goal.type == GoalType.ElapsedSince) {
                        projection.goal.elapsedStartMillis?.let { started ->
                            projection.elapsedDisplayValue(nowMillis, zoneId)?.let { display ->
                                ElapsedGoalMetric(
                                    display = display,
                                    prominent = true,
                                    modifier = Modifier.testTag("goal-insight-elapsed-${projection.goal.id}"),
                                )
                            } ?: return@let
                            Text(
                                "Counting continuously since ${elapsedGoalStartLabel(started, zoneId)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else if (projection.goal.type == GoalType.WeightedMilestones) {
                        Text(projection.inspectorOutcome(nowMillis, customUnits, zoneId), style = MaterialTheme.typography.titleMedium)
                        GoalMilestoneProgress(projection)
                    } else {
                        Text(
                            projection.inspectorOutcome(nowMillis, customUnits, zoneId),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        projection.numericReading(customUnits)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        projection.consistencyPeriodReading()?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        projection.onPace?.let { EntityInspectorFact("Pace", if (it) "On pace" else "Behind pace") }
                        if (chartValues.size >= 2) {
                            GoalLineChart(
                                points = insights.points,
                                goal = projection.goal,
                                customUnits = customUnits,
                                description = "${projection.goal.name} trend with ${chartValues.size} points",
                            )
                        } else Text("Log on at least two days for a trend line.")
                        insights.ratePerDay?.let {
                            EntityInspectorFact("Daily Rate", "${formatGoalCanonicalValue(it, projection.goal.trendUnitId, projection.goal.precision, customUnits, difference = true)} per day")
                        }
                        insights.forecastDate?.let {
                            EntityInspectorFact("Forecast", it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
                        }
                        insights.forecastExplanation?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        GoalInsightEvidence(insights, projection)
                    }
            }
        }
    }
}

@Composable
private fun GoalInsightEvidence(insights: GoalInsightSummary, projection: GoalProjection) {
    var expanded by rememberSaveable(projection.goal.id) { mutableStateOf(false) }
    val excluded = insights.excludedEntries + insights.outsideWindowEntries
    Text(
        "${quantityLabel(insights.points.size, "observed day")} · ${insights.confidence.replaceFirstChar(Char::uppercase)} evidence" +
            if (excluded > 0) " · ${quantityLabel(excluded, "excluded update")}" else "",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    DisclosureButton("About This Data", expanded, { expanded = !expanded })
    if (projection.goal.aggregationPeriod != GoalAggregationPeriod.All) Text(
        "The trend shows observed days. Current progress uses today's calculation window.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (expanded) Text(insights.dataQualityExplanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun GoalMilestoneProgress(projection: GoalProjection, interactive: Boolean = false) {
    val progress = if (projection.terminalSnapshot != null) projection.terminalSnapshot.progress else projection.progress
    progress?.let { EntityInspectorFact("Weighted Progress", "${formatGoalProgressPercent(it)} complete") }
    Text(
        if (projection.terminalSnapshot != null) {
            "Recorded when this Goal was ${projection.terminalSnapshot.status.label.lowercase()}. Later milestone changes do not change this outcome."
        } else if (!projection.goal.archived && projection.goal.status == GoalStatus.Active) {
            if (interactive) "Check off milestones here. Each contributes its saved weight to progress."
            else "Open this Goal to check off milestones. Each contributes its saved weight to progress."
        } else {
            "Progress reflects each milestone's weight. Make the Goal active to change milestones."
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun GoalTemplateDialog(
    today: LocalDate,
    defaults: AppSettings,
    onDismiss: () -> Unit,
    onChoose: (GoalDraft) -> Unit,
) {
    val templates = listOf(
        "Reach a weight" to reachWeightTemplateDraft(defaults, today),
        "Build savings" to GoalDraft(
            name = "Savings", icon = "💰", type = GoalType.AccumulateTotal,
            dimension = UnitDimension.Money, unitId = "currency", targetMin = 1000.0,
            aggregation = GoalAggregation.Sum,
            startDate = today,
        ),
        "Cover a distance" to GoalDraft(
            name = "Distance", icon = "🏃", type = GoalType.AccumulateTotal,
            dimension = UnitDimension.Distance, unitId = defaults.distanceUnitId,
            targetMin = 100.0, aggregation = GoalAggregation.Sum, startDate = today,
        ),
        "Read pages" to GoalDraft(
            name = "Reading", icon = "📚", type = GoalType.AccumulateTotal,
            dimension = UnitDimension.Count, unitId = "count", targetMin = 1000.0,
            aggregation = GoalAggregation.Sum,
            startDate = today,
        ),
        "Stay consistent" to GoalDraft(
            name = "Weekly consistency", icon = "✅", type = GoalType.Consistency,
            dimension = UnitDimension.Count, unitId = "count", precision = 0,
            targetMin = 3.0, aggregation = GoalAggregation.CompletionCount,
            consistencyPeriod = GoalConsistencyPeriod.Week,
            consistencyRequiredPeriods = 12, startDate = today,
        ),
        "Finish a project" to GoalDraft(
            name = "Project", icon = "🛠️", type = GoalType.WeightedMilestones,
            milestones = listOf(
                GoalMilestoneDraft("Plan", 1.0),
                GoalMilestoneDraft("Build", 2.0),
                GoalMilestoneDraft("Finish", 1.0),
            ),
            aggregation = GoalAggregation.CompletionCount,
            startDate = today,
        ),
        "Count time since" to GoalDraft(
            name = "Time since", icon = "⏱️", type = GoalType.ElapsedSince,
            startDate = today,
            elapsedStartMillis = today.atStartOfDay(defaults.zoneId()).toInstant().toEpochMilli(),
            elapsedDisplay = ElapsedDisplayFormat.Automatic,
            paceType = GoalPaceType.None,
        ),
    )
    PaneAwareAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start from a Goal Template") },
        text = {
            LazyColumn(
                modifier = Modifier.testTag("goal-template-list"),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(templates, key = { it.first }) { (label, draft) ->
                    WhipTextButton(onClick = { onChoose(draft) }, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                WhipIdentityEmoji(draft.icon)
                                Spacer(Modifier.width(8.dp))
                                Text(label.uiTitleCase(), fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                goalTemplateDescription(label),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

internal fun reachWeightTemplateDraft(defaults: AppSettings, today: LocalDate): GoalDraft {
    val unit = BuiltInUnits.get(defaults.massUnitId)
        ?.takeIf { it.dimension == UnitDimension.Mass && !it.archived }
        ?: BuiltInUnits.all.first { it.dimension == UnitDimension.Mass && !it.archived }
    return GoalDraft(
        name = "Weight target",
        icon = "⚖️",
        type = GoalType.ReduceValue,
        dimension = UnitDimension.Mass,
        unitId = unit.id,
        targetMin = if (unit.id == "pound") 150.0 else unit.fromCanonical(75.0),
        aggregation = GoalAggregation.Latest,
        startDate = today,
    )
}

private fun goalTemplateDescription(label: String): String = when (label) {
    "Reach a weight" -> "A current value moving toward a mass target."
    "Build savings" -> "Add deposits toward a money total."
    "Cover a distance" -> "Accumulate walks, runs, rides, or any distance."
    "Read pages" -> "Add pages toward a reading total."
    "Stay consistent" -> "Hit a weekly success target for twelve weeks."
    "Finish a project" -> "Plan, build, and finish weighted milestones."
    "Count time since" -> "Count from a chosen date and time for recovery, sobriety, streaks, or anniversaries."
    else -> "Prefills an editable goal; nothing is saved until you confirm."
}

@Composable
internal fun GoalEditorDialog(
    projection: GoalProjection?,
    modifier: Modifier = Modifier,
    initialDraft: GoalDraft? = null,
    today: LocalDate,
    activeZoneId: ZoneId,
    nowMillis: Long,
    customUnits: List<UnitDefinition>,
    defaults: AppSettings = AppSettings(),
    onDismiss: () -> Unit,
    onSave: (GoalDraft) -> Unit,
    onRequestNotificationPermission: () -> Unit = {},
    saving: Boolean = false,
    persistenceError: String? = null,
    areas: List<Area> = emptyList(),
    defaultAreaId: String? = null,
    onCreateArea: (String, Long?, (Result<String>) -> Unit) -> Unit = { _, _, _ -> },
    onCreateCustomUnit: CreateCustomUnitAction = UnavailableCreateCustomUnitAction,
    customIdentityEmojis: List<CustomIdentityEmoji> = emptyList(),
    onSaveIdentityEmoji: IdentityEmojiSaveActions? = null,
    onRemoveSavedIdentityEmoji: (String) -> Unit = {},
) {
    val goal = projection?.goal
    val editorKey = "goal-${goal?.id ?: "new"}-${initialDraft?.type?.name ?: "blank"}"
    var name by rememberSaveable(editorKey) { mutableStateOf(goal?.name ?: initialDraft?.name.orEmpty()) }
    var description by rememberSaveable(editorKey) { mutableStateOf(goal?.description ?: initialDraft?.description.orEmpty()) }
    var areaId by rememberSaveable(editorKey) {
        mutableStateOf(goal?.areaId ?: initialDraft?.areaId ?: defaultAreaId)
    }
    var area by rememberSaveable(editorKey) {
        mutableStateOf(
            goal?.area ?: initialDraft?.area?.takeIf(String::isNotBlank)
            ?: areas.firstOrNull { it.id == (goal?.areaId ?: initialDraft?.areaId ?: defaultAreaId) }?.name.orEmpty(),
        )
    }
    var tags by rememberSaveable(editorKey) { mutableStateOf(goal?.tags?.joinToString(",") ?: initialDraft?.tags?.joinToString(",").orEmpty()) }
    var icon by rememberSaveable(editorKey) {
        mutableStateOf(goal?.icon ?: initialDraft?.icon ?: DEFAULT_GOAL_EMOJI)
    }
    var type by rememberSaveable(editorKey) { mutableStateOf(goal?.type ?: initialDraft?.type ?: GoalType.ReachValue) }
    var unitId by rememberSaveable(editorKey) { mutableStateOf(goal?.unitId ?: initialDraft?.unitId ?: "unitless") }
    var dimension by rememberSaveable(editorKey) { mutableStateOf(goal?.dimension ?: initialDraft?.dimension ?: UnitDimension.Unitless) }
    var precision by rememberSaveable(editorKey) { mutableStateOf((goal?.precision ?: initialDraft?.precision ?: defaults.numberPrecision).toString()) }
    var baseline by rememberSaveable(editorKey) { mutableStateOf(goal?.displayValue(goal.baseline, customUnits)?.let(::plainNumericValue) ?: initialDraft?.baseline?.let(::plainNumericValue).orEmpty()) }
    var targetMin by rememberSaveable(editorKey) { mutableStateOf(goal?.displayValue(goal.targetMin, customUnits)?.let(::plainNumericValue) ?: initialDraft?.targetMin?.let(::plainNumericValue).orEmpty()) }
    var targetMax by rememberSaveable(editorKey) { mutableStateOf(goal?.displayValue(goal.targetMax, customUnits)?.let(::plainNumericValue) ?: initialDraft?.targetMax?.let(::plainNumericValue).orEmpty()) }
    var aggregation by rememberSaveable(editorKey) {
        mutableStateOf(
            (goal?.aggregation ?: initialDraft?.aggregation)
                ?.takeIf { it in type.compatibleAggregations() }
                ?: type.defaultAggregation(),
        )
    }
    var pace by rememberSaveable(editorKey) { mutableStateOf(goal?.paceType ?: initialDraft?.paceType ?: GoalPaceType.Linear) }
    var deadline by rememberSaveable(editorKey) { mutableStateOf(goal?.deadline ?: initialDraft?.deadline) }
    var startDate by rememberSaveable(editorKey) { mutableStateOf(goal?.startDate ?: initialDraft?.startDate ?: today) }
    var showStartDatePicker by rememberSaveable(editorKey) { mutableStateOf(false) }
    var unitChangeError by rememberSaveable(editorKey) { mutableStateOf<String?>(null) }
    var showDatePicker by rememberSaveable(editorKey) { mutableStateOf(false) }
    var milestoneDrafts by rememberSaveable(editorKey) {
        mutableStateOf<List<GoalMilestoneDraft>>(
            projection?.milestones?.map {
                    GoalMilestoneDraft(
                        name = it.name,
                        weight = it.weight,
                        reward = it.reward,
                        id = it.id,
                        uuid = it.uuid,
                    )
                } ?: initialDraft?.milestones.orEmpty().map { draft ->
                    if (draft.uuid != null || draft.id != null) draft
                    else draft.copy(uuid = java.util.UUID.randomUUID().toString())
                },
        )
    }
    var milestoneWeights by rememberSaveable(editorKey) { mutableStateOf<Map<String, String>>(emptyMap()) }
    fun milestoneKey(draft: GoalMilestoneDraft) = draft.uuid ?: "saved-${draft.id}"
    var advanced by rememberSaveable(editorKey) {
        mutableStateOf(
            defaults.powerMode || goal?.let {
                it.description.isNotBlank() || it.tags.isNotEmpty()
            } == true,
        )
    }
    var advancedMeasurement by rememberSaveable(editorKey) {
        mutableStateOf(
            defaults.powerMode || (goal?.aggregation ?: initialDraft?.aggregation)?.let { it != type.defaultAggregation() } == true ||
                (goal?.aggregationPeriod ?: initialDraft?.aggregationPeriod ?: GoalAggregationPeriod.All) != GoalAggregationPeriod.All ||
                (goal?.precision ?: initialDraft?.precision ?: defaults.numberPrecision) != defaults.numberPrecision,
        )
    }
    var reminder by rememberSaveable(editorKey) { mutableStateOf((goal?.reminderMinutes ?: initialDraft?.reminderMinutes)?.let { "%02d:%02d".format(it / 60, it % 60) }.orEmpty()) }
    var aggregationPeriod by rememberSaveable(editorKey) { mutableStateOf(goal?.aggregationPeriod ?: initialDraft?.aggregationPeriod ?: GoalAggregationPeriod.All) }
    var rollingDays by rememberSaveable(editorKey) { mutableStateOf((goal?.rollingDays ?: initialDraft?.rollingDays ?: 7).toString()) }
    var consistencyPeriod by rememberSaveable(editorKey) { mutableStateOf(goal?.consistencyPeriod ?: initialDraft?.consistencyPeriod ?: GoalConsistencyPeriod.Week) }
    var consistencyRequiredPeriods by rememberSaveable(editorKey) { mutableStateOf((goal?.consistencyRequiredPeriods ?: initialDraft?.consistencyRequiredPeriods ?: 12).toString()) }
    val editorZoneId by rememberSaveable(editorKey) { mutableStateOf(activeZoneId.id) }
    val editorZone = remember(editorZoneId) { ZoneId.of(editorZoneId) }
    val initialElapsedInstantMillis by rememberSaveable(editorKey) {
        mutableStateOf(goal?.elapsedStartMillis ?: initialDraft?.elapsedStartMillis ?: nowMillis)
    }
    val initialElapsedMoment = remember(editorKey, editorZoneId, initialElapsedInstantMillis) {
        Instant.ofEpochMilli(initialElapsedInstantMillis).atZone(editorZone)
    }
    var elapsedDate by rememberSaveable(editorKey) { mutableStateOf(initialElapsedMoment.toLocalDate()) }
    var elapsedMinutes by rememberSaveable(editorKey) { mutableIntStateOf(initialElapsedMoment.hour * 60 + initialElapsedMoment.minute) }
    var elapsedMomentEdited by rememberSaveable(editorKey) { mutableStateOf(false) }
    var elapsedOffsetSeconds by rememberSaveable(editorKey) { mutableStateOf<Int?>(initialElapsedMoment.offset.totalSeconds) }
    var elapsedDisplayFormat by rememberSaveable(editorKey) {
        mutableStateOf(goal?.elapsedDisplay ?: initialDraft?.elapsedDisplay ?: ElapsedDisplayFormat.Automatic)
    }
    var showElapsedDatePicker by rememberSaveable(editorKey) { mutableStateOf(false) }
    var validationAttempt by rememberSaveable(editorKey) { mutableIntStateOf(0) }
    val validationRequested = validationAttempt > 0
    val elapsedResolution = resolveExactLocalTime(elapsedDate, elapsedMinutes, editorZone)
    val elapsedStartInstant = resolveEditedExactInstant(
        initialInstant = Instant.ofEpochMilli(initialElapsedInstantMillis),
        wallTimeEdited = elapsedMomentEdited,
        resolution = elapsedResolution,
        preferredOffsetSeconds = elapsedOffsetSeconds,
    )
    val closedOutcomeKind = goal?.takeIf { projection?.closureSnapshots?.isNotEmpty() == true }
        ?.let { goalOutcomeKind(it.type, it.aggregation) }
    val compatibleAggregations = type.compatibleAggregations().filter {
        closedOutcomeKind == null || goalOutcomeKind(type, it) == closedOutcomeKind
    }
    val availableTypes = GoalType.entries.filter { candidate ->
        (goal == null || candidate.supportsDimension(goal.dimension)) &&
            (closedOutcomeKind == null || candidate.compatibleAggregations().any { goalOutcomeKind(candidate, it) == closedOutcomeKind })
    }
    val direction = type.defaultDirection()
    val editorFingerprint = listOf(
        name, description, areaId, area, tags, icon, type, unitId, dimension, precision,
        baseline, targetMin, targetMax, aggregation, pace, startDate, deadline,
        reminder, aggregationPeriod, rollingDays, consistencyPeriod, consistencyRequiredPeriods,
        elapsedDate, elapsedMinutes, elapsedMomentEdited, elapsedOffsetSeconds, elapsedDisplayFormat,
        milestoneDrafts.map { "${it.id}:${it.uuid}:${it.name}:${milestoneWeights[milestoneKey(it)] ?: it.weight}:${it.reward}" },
    ).joinToString("\u001f")
    val initialFingerprint by rememberSaveable(editorKey) { mutableStateOf(editorFingerprint) }
    var showDiscardConfirmation by rememberSaveable(editorKey) { mutableStateOf(false) }
    val requestDismiss = { if (editorFingerprint != initialFingerprint) showDiscardConfirmation = true else onDismiss() }
    val normalizedMilestones = milestoneDrafts.filter { it.name.isNotBlank() }
        .map { it.copy(name = it.name.trim(), reward = it.reward.trim(),
            weight = milestoneWeights[milestoneKey(it)]?.toWhipDoubleOrNull() ?:
                if (milestoneKey(it) in milestoneWeights) Double.NaN else it.weight) }
    val currentDraft = GoalDraft(
        name = name,
        description = description,
        areaId = areaId,
        area = area,
        tags = tags.split(',').map(String::trim).filter(String::isNotBlank),
        icon = icon.ifBlank { DEFAULT_GOAL_EMOJI },
        type = type,
        dimension = dimension,
        unitId = unitId,
        precision = precision.toIntOrNull() ?: -1,
        baseline = baseline.toWhipDoubleOrNull(),
        targetMin = targetMin.toWhipDoubleOrNull(),
        targetMax = targetMax.toWhipDoubleOrNull(),
        direction = direction,
        startDate = startDate,
        deadline = deadline,
        aggregation = aggregation,
        paceType = pace.takeIf { deadline != null && type !in setOf(GoalType.OpenEndedTrend, GoalType.ElapsedSince) }
            ?: GoalPaceType.None,
        reminderMinutes = parseGoalClock(reminder),
        milestones = normalizedMilestones,
        aggregationPeriod = aggregationPeriod,
        rollingDays = rollingDays.toIntOrNull(),
        consistencyPeriod = consistencyPeriod,
        consistencyRequiredPeriods = consistencyRequiredPeriods.toIntOrNull(),
        elapsedStartMillis = elapsedStartInstant?.toEpochMilli().takeIf { type == GoalType.ElapsedSince },
        elapsedDisplay = elapsedDisplayFormat,
    ).withTypeSemantics()
    val rawFieldProblems = buildList {
        if (goal == null && areas.count { !it.archived } > 1 && areaId == null) add("Choose an Area for this Goal")
        if (
            type !in setOf(GoalType.Consistency, GoalType.WeightedMilestones, GoalType.ElapsedSince) &&
            baseline.isNotBlank() && baseline.toWhipDoubleOrNull() == null
        ) add("Starting value must be a number")
        if (
            type !in setOf(GoalType.WeightedMilestones, GoalType.ElapsedSince) &&
            targetMin.isNotBlank() && targetMin.toWhipDoubleOrNull() == null
        ) add(
            if (type == GoalType.MaintainRange) "Range minimum must be a number" else "Target must be a number",
        )
        if (type == GoalType.MaintainRange && targetMax.isNotBlank() && targetMax.toWhipDoubleOrNull() == null) {
            add("Range maximum must be a number")
        }
        if (type == GoalType.ElapsedSince && elapsedStartInstant == null) {
            add("Choose a start time that exists in ${editorZone.id}")
        }
    }
    val draftValidationMessages = currentDraft.validationErrors(nowMillis)
    val validationMessages = (rawFieldProblems + draftValidationMessages).distinct()
    val validationRequester = remember { BringIntoViewRequester() }
    val editorListState = rememberLazyListState()
    LaunchedEffect(validationAttempt, validationMessages) {
        if (validationRequested && validationMessages.isNotEmpty()) validationRequester.bringIntoView()
    }
    LaunchedEffect(persistenceError) {
        if (!persistenceError.isNullOrBlank()) editorListState.scrollToItem(0)
    }
    BackHandler(enabled = !showDiscardConfirmation && !saving, onBack = requestDismiss)
    ProductivityEditorDialog(
        modifier = modifier,
        testTag = "goal-editor-surface",
        primary = true,
        paneTitle = if (goal == null) "Create Goal" else "Edit Goal",
        onDismissRequest = { if (!saving) requestDismiss() },
        title = { Text(if (goal == null) "Create Goal" else "Edit Goal") },
        text = {
            WhipReorderLazyColumn(
                modifier = Modifier.testTag("goal-editor-fields"),
                state = editorListState,
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                item {
                    Text("* Required field", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (persistenceError != null) item {
                    PersistenceFailureNotice(
                        message = persistenceError,
                        testTag = "goal-persistence-save-problem",
                    )
                }
                if (validationRequested && validationMessages.isNotEmpty()) item {
                    FormValidationSummary(
                        messages = validationMessages,
                        visible = true,
                        modifier = Modifier.bringIntoViewRequester(validationRequester),
                        testTag = "goal-save-problem",
                    )
                }
                item {
                    ProductivityIdentitySection(
                        title = "Basics",
                        identityFields = {
                    OutlinedTextField(
                        name,
                        { name = it.replace('\n', ' ').replace('\r', ' ').take(100) },
                        label = { Text("Name *") },
                        isError = validationRequested && name.isBlank(),
                        supportingText = if (validationRequested && name.isBlank()) {
                            { Text("Goal name is required") }
                        } else {{ Text("${name.length}/100") }},
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("goal-editor-name"),
                    )
                        },
                        emojiPicker = {
                    WhipEmojiPicker(
                        value = icon,
                        defaultEmoji = DEFAULT_GOAL_EMOJI,
                        onValueChange = { icon = it },
                        modifier = Modifier.fillMaxWidth(),
                        customEmojis = customIdentityEmojis,
                        onSaveEmoji = onSaveIdentityEmoji,
                        onRemoveSavedEmoji = onRemoveSavedIdentityEmoji,
                    )
                        },
                    )
                }
                item {
                    EditorSectionHeader("Target")
                }
                item {
                    GoalEnumDropdown("Goal Type", availableTypes, type, GoalType::displayLabel) { selected ->
                        type = selected
                        aggregation = selected.defaultAggregation().takeIf {
                            closedOutcomeKind == null || goalOutcomeKind(selected, it) == closedOutcomeKind
                        } ?: selected.compatibleAggregations().firstOrNull {
                            closedOutcomeKind == null || goalOutcomeKind(selected, it) == closedOutcomeKind
                        } ?: selected.defaultAggregation()
                        when (selected) {
                        GoalType.ReduceValue, GoalType.ReachValue, GoalType.MaintainRange, GoalType.AccumulateTotal -> Unit
                        GoalType.MeetAverage -> aggregationPeriod = GoalAggregationPeriod.Week
                        GoalType.Consistency -> {
                            unitId = "count"
                            dimension = UnitDimension.Count
                            precision = "0"
                            if (targetMin.isBlank()) targetMin = "3"
                        }
                        GoalType.WeightedMilestones -> { unitId = "unitless"; dimension = UnitDimension.Unitless }
                        GoalType.OpenEndedTrend -> pace = GoalPaceType.None
                        GoalType.ElapsedSince -> {
                            unitId = "unitless"
                            dimension = UnitDimension.Unitless
                            precision = "0"
                            pace = GoalPaceType.None
                            deadline = null
                        }
                        }
                    }
                    Text(type.explanation(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (goal != null) Text(
                        "Measurement type: ${goal.dimension.uiLabel()}. Create a new Goal to use a different measurement type." +
                            if (closedOutcomeKind != null) " Closed outcomes also keep their original kind of value." else "",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (type !in setOf(GoalType.WeightedMilestones, GoalType.Consistency, GoalType.ElapsedSince)) {
                    item {
                        UnitSelectionField(
                            units = BuiltInUnits.all + customUnits,
                            selectedUnitId = unitId,
                            dimension = dimension,
                            onSelect = { selectedId ->
                                val allUnits = BuiltInUnits.all + customUnits
                                val oldUnit = allUnits.firstOrNull { it.id == unitId }
                                val newUnit = allUnits.firstOrNull { it.id == selectedId }
                                val rawValues = listOf(baseline, targetMin, targetMax)
                                if (oldUnit != null && newUnit != null && oldUnit.dimension == newUnit.dimension) {
                                    val converted = convertNumericDraftValues(rawValues, oldUnit, newUnit)
                                    if (converted == null) {
                                        unitChangeError = "Finish or clear the numeric values before changing the unit. Values must be finite in the selected unit. Your current unit and draft are unchanged."
                                    } else {
                                        baseline = converted[0]
                                        targetMin = converted[1]
                                        targetMax = converted[2]
                                        unitId = selectedId
                                        unitChangeError = null
                                    }
                                } else {
                                    unitId = selectedId
                                    unitChangeError = null
                                }
                            },
                            onCreateUnit = onCreateCustomUnit,
                            dialogModifier = modifier,
                            allowAnyDimension = goal == null,
                            onDimensionSelect = { dimension = it },
                        )
                        unitChangeError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("goal-unit-change-error")) }
                    }
                }
                if (type !in setOf(GoalType.WeightedMilestones, GoalType.ElapsedSince)) {
                    item {
                        if (type == GoalType.MaintainRange) {
                            ResponsiveFieldPair(
                                first = { field ->
                                    GoalNumberField(
                                        targetMin,
                                        { targetMin = it },
                                        goalValueLabel("Range Minimum", unitId, customUnits),
                                        field,
                                        required = true,
                                        error = if (validationRequested) when {
                                            targetMin.toWhipDoubleOrNull() == null -> "Enter a range minimum"
                                            targetMax.toWhipDoubleOrNull() != null && requireNotNull(targetMin.toWhipDoubleOrNull()) > requireNotNull(targetMax.toWhipDoubleOrNull()) -> "Must not exceed the maximum"
                                            else -> null
                                        } else null,
                                    )
                                },
                                second = { field ->
                                    GoalNumberField(
                                        targetMax,
                                        { targetMax = it },
                                        goalValueLabel("Range Maximum", unitId, customUnits),
                                        field,
                                        required = true,
                                        error = if (validationRequested && targetMax.toWhipDoubleOrNull() == null) "Enter a range maximum" else null,
                                    )
                                },
                            )
                        } else GoalNumberField(
                            targetMin,
                            { targetMin = it },
                            if (type == GoalType.Consistency) "Successes per Period" else goalValueLabel("Target", unitId, customUnits),
                            Modifier.testTag("goal-editor-target"),
                            required = type != GoalType.OpenEndedTrend,
                            error = if (!validationRequested) null else when {
                                type == GoalType.OpenEndedTrend -> null
                                targetMin.toWhipDoubleOrNull() == null -> "Enter a target"
                                type == GoalType.Consistency && requireNotNull(targetMin.toWhipDoubleOrNull()) <= 0.0 -> "Enter a value greater than zero"
                                else -> null
                            },
                        )
                    }
                    if (type != GoalType.Consistency) {
                        item {
                            GoalNumberField(
                                baseline,
                                { baseline = it },
                                goalValueLabel(if (type == GoalType.ReduceValue) "Starting Value" else "Starting Value (Optional)", unitId, customUnits),
                                required = type == GoalType.ReduceValue,
                                error = if (!validationRequested) null else when {
                                    type == GoalType.ReduceValue && baseline.isBlank() -> "Enter a starting value for a reduction Goal"
                                    baseline.isNotBlank() && baseline.toWhipDoubleOrNull() == null -> "Starting value must be a number"
                                    else -> draftValidationMessages.firstOrNull { it.startsWith("Starting value cannot") }
                                },
                            )
                        }
                    } else {
                        item {
                            ResponsiveFieldPair(
                                first = { field -> Column(field) { GoalEnumDropdown("Period", GoalConsistencyPeriod.entries, consistencyPeriod, { it.periodLabel.replaceFirstChar(Char::uppercase) }) { consistencyPeriod = it } } },
                                second = { field ->
                                    GoalNumberField(
                                        consistencyRequiredPeriods,
                                        { consistencyRequiredPeriods = it },
                                        "Number of Periods",
                                        field,
                                        required = true,
                                        error = "Enter a positive whole number".takeIf {
                                            validationRequested && (consistencyRequiredPeriods.toIntOrNull() ?: 0) <= 0
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
                if (type == GoalType.ElapsedSince) {
                    item {
                        Text("Counter Start", fontWeight = FontWeight.Bold)
                        Text("Choose the exact event time. Resetting later replaces this start time without adding a progress update.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Whip time · ${editorZone.id}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item {
                        ResponsiveFieldPair(
                            first = { field ->
                                WhipOutlinedButton(onClick = { showElapsedDatePicker = true }, modifier = field.fillMaxWidth()) {
                                    Text(elapsedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
                                }
                            },
                            second = { field -> ClockPickerButton("Start Time", elapsedMinutes, {
                                if (it != null) {
                                    elapsedMinutes = it
                                    elapsedMomentEdited = true
                                    elapsedOffsetSeconds = null
                                }
                            }, field) },
                        )
                    }
                    if (elapsedResolution.isGap) item {
                        val next = elapsedResolution.firstValidDateTimeAfterGap
                            ?.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))
                        Text(
                            "That local time does not exist because the clock moves forward.${next?.let { " The next valid time is $it." }.orEmpty()}",
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (elapsedMomentEdited && elapsedResolution.isOverlap) item {
                        Text(
                            "That time occurs twice because the clock moves back. Choose which occurrence you mean.",
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            style = MaterialTheme.typography.bodySmall,
                        )
                        elapsedResolution.options.forEach { option ->
                            val selected = elapsedOffsetSeconds == option.offset.totalSeconds
                            WhipOutlinedButton(
                                onClick = { elapsedOffsetSeconds = option.offset.totalSeconds },
                                modifier = Modifier.fillMaxWidth().testTag("elapsed-editor-overlap-${option.offset.id}"),
                            ) {
                                val index = elapsedResolution.options.indexOf(option)
                                Text("${if (index == 0) "First" else "Second"} occurrence · ${option.offset.id}${if (selected) " · Selected" else ""}")
                            }
                        }
                    }
                    item {
                        WhipGroupedInformationCard(Modifier.testTag("elapsed-display-config")) {
                            Text("Always-visible counter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Choose Automatic or any combination. Your counter stays prominent on Goal cards, Home, Insights, and details.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                WhipFilterChip(
                                    selected = elapsedDisplayFormat.automatic,
                                    onClick = { elapsedDisplayFormat = ElapsedDisplayFormat.Automatic },
                                    label = { Text("Automatic") },
                                    modifier = Modifier.testTag("elapsed-display-auto"),
                                )
                                ElapsedDisplayFormat.DISPLAY_ORDER.forEach { unit ->
                                    WhipFilterChip(
                                        selected = unit in elapsedDisplayFormat.units,
                                        onClick = {
                                            val selected = if (elapsedDisplayFormat.automatic) {
                                                listOf(unit)
                                            } else if (unit in elapsedDisplayFormat.units) {
                                                elapsedDisplayFormat.units - unit
                                            } else {
                                                elapsedDisplayFormat.units + unit
                                            }
                                            elapsedDisplayFormat = if (selected.isEmpty()) {
                                                ElapsedDisplayFormat.Automatic
                                            } else {
                                                ElapsedDisplayFormat.selected(selected)
                                            }
                                        },
                                        label = { Text(unit.displayLabel()) },
                                        modifier = Modifier.testTag("elapsed-display-${unit.name.lowercase()}"),
                                    )
                                }
                            }
                            elapsedStartInstant?.takeUnless { it.isAfter(Instant.ofEpochMilli(nowMillis)) }?.let { started ->
                                Text("Preview", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                ElapsedGoalMetric(
                                    display = elapsedDisplay(started.toEpochMilli(), nowMillis, elapsedDisplayFormat, editorZone),
                                    modifier = Modifier.testTag("elapsed-display-preview"),
                                    prominent = true,
                                )
                            }
                        }
                    }
                    if (elapsedStartInstant?.isAfter(Instant.ofEpochMilli(nowMillis)) == true) item {
                        Text(
                            "Start time cannot be in the future.",
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                if (type !in setOf(GoalType.WeightedMilestones, GoalType.Consistency, GoalType.ElapsedSince)) {
                    item { EditorSectionHeader("Measurement") }
                    item {
                        DisclosureButton(
                            label = "Progress Calculation",
                            expanded = advancedMeasurement,
                            onClick = { advancedMeasurement = !advancedMeasurement },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "${aggregation.displayLabel()} · ${aggregationPeriod.displayLabel()} · $precision decimal ${if (precision.toIntOrNull() == 1) "place" else "places"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (advancedMeasurement) {
                        if (compatibleAggregations.size > 1) item {
                            GoalEnumDropdown("How entries combine", compatibleAggregations, aggregation, GoalAggregation::displayLabel) { aggregation = it }
                        }
                        if (type !in setOf(GoalType.Consistency, GoalType.WeightedMilestones, GoalType.ElapsedSince)) {
                            item { GoalEnumDropdown("Time window", GoalAggregationPeriod.entries, aggregationPeriod, GoalAggregationPeriod::displayLabel) { aggregationPeriod = it } }
                            if (aggregationPeriod == GoalAggregationPeriod.RollingDays) item {
                                GoalNumberField(
                                    rollingDays,
                                    { rollingDays = it },
                                    "Rolling Days",
                                    required = true,
                                    error = "Enter a positive whole number".takeIf {
                                        validationRequested && (rollingDays.toIntOrNull() ?: 0) <= 0
                                    },
                                )
                            }
                        }
                        item {
                            GoalNumberField(
                                precision,
                                { precision = it },
                                "Decimal Places (0–6)",
                                required = true,
                                error = "Enter a whole number from 0 to 6".takeIf {
                                    validationRequested && (precision.toIntOrNull() ?: -1) !in 0..6
                                },
                            )
                        }
                    }
                }
                if (type == GoalType.WeightedMilestones) {
                    item(key = "goal-editor-milestones") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Milestones *", fontWeight = FontWeight.Bold)
                            Text(
                                "Weights set each milestone’s share of progress: weight 2 counts twice as much as weight 1.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (validationRequested && draftValidationMessages.any { it.contains("milestone", ignoreCase = true) }) {
                                    MaterialTheme.colorScheme.error
                                } else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            WhipReorderLayout(itemSpacing = 8.dp) {
                            milestoneDrafts.forEachIndexed { index, draft ->
                                key(draft.uuid ?: "goal-milestone-${draft.id ?: index}") {
                                val reorderInteraction = rememberWhipReorderInteractionState()
                                val rawWeight = milestoneWeights[milestoneKey(draft)] ?: plainNumericValue(draft.weight)
                                var detailsExpanded by rememberSaveable(milestoneKey(draft)) { mutableStateOf(false) }
                                LaunchedEffect(validationAttempt, rawWeight) {
                                    if (validationRequested && (rawWeight.toWhipDoubleOrNull()?.let { it <= 0.0 } != false)) detailsExpanded = true
                                }
                                Card(
                                    Modifier.fillMaxWidth().whipReorderItem(
                                        reorderInteraction,
                                        layoutPosition = index + 1,
                                        layoutScope = "goal-editor-milestones",
                                    ),
                                ) {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            WhipReorderHandle(
                                                label = draft.name.ifBlank { "milestone ${index + 1}" },
                                                canMovePrevious = index > 0,
                                                canMoveNext = index < milestoneDrafts.lastIndex,
                                                position = index + 1,
                                                total = milestoneDrafts.size,
                                                interactionState = reorderInteraction,
                                                moveWholeItem = true,
                                                layoutScope = "goal-editor-milestones",
                                                onMove = { delta -> milestoneDrafts = ArrayList(moveListItem(milestoneDrafts, index, delta)) },
                                            )
                                            OutlinedTextField(
                                                value = draft.name,
                                                onValueChange = { name -> milestoneDrafts = ArrayList(milestoneDrafts).also { it[index] = draft.copy(name = name) } },
                                                label = { Text("Milestone ${index + 1}") },
                                                modifier = Modifier.weight(1f).testTag("goal-milestone-name-$index"),
                                            )
                                            IconButton(
                                                onClick = { milestoneDrafts = ArrayList(milestoneDrafts).also { it.removeAt(index) } },
                                            ) { Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove ${draft.name.ifBlank { "milestone ${index + 1}" }}") }
                                        }
                                        DisclosureButton(
                                            label = "Weight $rawWeight" + draft.reward.takeIf(String::isNotBlank)?.let { " · Reward: $it" }.orEmpty() + " · Weight & Reward",
                                            expanded = detailsExpanded,
                                            onClick = { detailsExpanded = !detailsExpanded },
                                            modifier = Modifier.fillMaxWidth().testTag("goal-milestone-details-$index"),
                                        )
                                        if (detailsExpanded) ResponsiveFieldPair(
                                            first = { field ->
                                                GoalNumberField(
                                                    value = milestoneWeights[milestoneKey(draft)] ?: plainNumericValue(draft.weight),
                                                    onValueChange = { value -> milestoneWeights = milestoneWeights + (milestoneKey(draft) to value) },
                                                    error = "Enter a non-negative weight".takeIf {
                                                        validationRequested && (milestoneWeights[milestoneKey(draft)]?.toWhipDoubleOrNull()
                                                            ?: if (milestoneKey(draft) in milestoneWeights) Double.NaN else draft.weight).let { !it.isFinite() || it < 0.0 }
                                                    },
                                                    label = "Weight",
                                                    modifier = field.testTag("goal-milestone-weight-$index"),
                                                )
                                            },
                                            second = { field ->
                                                OutlinedTextField(
                                                    value = draft.reward,
                                                    onValueChange = { reward ->
                                                        milestoneDrafts = ArrayList(milestoneDrafts).also {
                                                            it[index] = draft.copy(reward = reward)
                                                        }
                                                    },
                                                    label = { Text("Reward (optional)") },
                                                    modifier = field,
                                                )
                                            },
                                        )
                                    }
                                }
                                }
                            }
                            }
                            WhipOutlinedButton(
                                onClick = {
                                    milestoneDrafts = ArrayList(milestoneDrafts).also {
                                        it += GoalMilestoneDraft("", uuid = java.util.UUID.randomUUID().toString())
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Add Milestone")
                            }
                        }
                    }
                }
                item { EditorSectionHeader("Schedule") }
                if (type != GoalType.ElapsedSince) item {
                    WhipOutlinedButton(onClick = { showStartDatePicker = true }, modifier = Modifier.fillMaxWidth().testTag("goal-start-date")) {
                        Text("Start Date ${startDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}")
                    }
                    Text("Progress uses entries from this date. Changing it recalculates current progress; saved history and closed outcomes stay intact.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (validationRequested && deadline?.isBefore(startDate) == true) Text("Start date must be on or before the deadline.", color = MaterialTheme.colorScheme.error)
                }
                if (type != GoalType.ElapsedSince) item { WhipOutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) { Text(deadline?.let { "Deadline ${it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}" } ?: "Add Deadline") } }
                if (deadline != null && type != GoalType.ElapsedSince) item {
                    WhipTextButton(onClick = { deadline = null }, modifier = Modifier.testTag("goal-remove-deadline")) { Text("Remove Deadline") }
                }
                if (deadline != null && type != GoalType.OpenEndedTrend) item {
                    GoalEnumDropdown("Pace guidance", GoalPaceType.entries, pace, GoalPaceType::displayLabel) { pace = it }
                    Text(
                        "This compares completed progress with the share of time elapsed before the deadline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    ClockPickerButton(
                        label = "Daily Progress Reminder",
                        minutes = parseGoalClock(reminder),
                        onChange = { minutes ->
                            if (minutes != null && parseGoalClock(reminder) == null) onRequestNotificationPermission()
                            reminder = minutes?.let(::formatClockMinutes).orEmpty()
                        },
                    )
                }
                item {
                    ProductivityOrganizationSection(
                        areaPicker = {
                    AreaPicker(
                        areas = areas,
                        selectedAreaId = areaId,
                        selectedAreaName = area,
                        onSelect = { id, value -> areaId = id; area = value },
                        onCreateArea = onCreateArea,
                        modifier = Modifier.fillMaxWidth(),
                        dialogModifier = modifier,
                        inheritedFromScope = projection == null && initialDraft?.areaId == null && defaultAreaId != null,
                    )
                        },
                        extras = {
                    DisclosureButton(
                        label = "Additional Details",
                        expanded = advanced,
                        onClick = { advanced = !advanced },
                        modifier = Modifier.fillMaxWidth(),
                    )
                        },
                    )
                }
                if (advanced) {
                    item { EditorSectionHeader("Details") }
                    item { OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(tags, { tags = it }, label = { Text("Tags, comma-separated") }, modifier = Modifier.fillMaxWidth()) }
                }
            }
        },
        confirmButton = {
            WhipButton(enabled = !saving, onClick = {
                validationAttempt++
                if (type !in setOf(GoalType.WeightedMilestones, GoalType.Consistency, GoalType.ElapsedSince) &&
                    ((precision.toIntOrNull() ?: -1) !in 0..6 ||
                        aggregationPeriod == GoalAggregationPeriod.RollingDays && (rollingDays.toIntOrNull() ?: 0) <= 0)
                ) advancedMeasurement = true
                if (validationMessages.isEmpty()) onSave(currentDraft)
            }) { Text(if (saving) "Saving…" else "Save") }
        },
        dismissButton = {
            IconButton(
                onClick = requestDismiss,
                enabled = !saving,
                modifier = Modifier.semantics { contentDescription = "Cancel Goal editing" },
            ) { Icon(Icons.Outlined.Close, contentDescription = null) }
        },
        inputBlocked = saving,
        inputBlockedLabel = "Saving Goal",
    )
    if (showElapsedDatePicker) WhipDatePickerDialog(elapsedDate, { showElapsedDatePicker = false }, {
        elapsedDate = it
        elapsedMomentEdited = true
        elapsedOffsetSeconds = null
        showElapsedDatePicker = false
    })
    if (showDatePicker) WhipDatePickerDialog(deadline ?: today, { showDatePicker = false }, { deadline = it; showDatePicker = false })
    if (showStartDatePicker) WhipDatePickerDialog(startDate, { showStartDatePicker = false }, { startDate = it; showStartDatePicker = false })
    if (showDiscardConfirmation) {
        UnsavedChangesDialog("goal", { showDiscardConfirmation = false }, onDismiss)
    }
}

@Composable
internal fun GoalMeasurementDialog(
    projection: GoalProjection,
    today: LocalDate,
    entry: MeasurementEntry?,
    customUnits: List<UnitDefinition> = emptyList(),
    onDismiss: () -> Unit,
    onRecord: (Double, LocalDate, String) -> Unit,
    onDelete: (() -> Unit)? = null,
    saving: Boolean = false,
    persistenceError: String? = null,
) {
    val editorKey = "goal-measurement-${entry?.id ?: projection.goal.id}"
    val completionEntry = projection.goal.aggregation == GoalAggregation.CompletionCount
    val initialValue = entry?.enteredValue?.let(::plainNumericValue) ?: if (completionEntry) "1" else ""
    val initialNote = entry?.note.orEmpty()
    val initialDate = entry?.localDate ?: today
    var value by rememberSaveable(editorKey) { mutableStateOf(initialValue) }
    var note by rememberSaveable(editorKey) { mutableStateOf(initialNote) }
    var date by rememberSaveable(editorKey) { mutableStateOf(initialDate) }
    var showDatePicker by rememberSaveable(editorKey) { mutableStateOf(false) }
    var confirmDelete by rememberSaveable(editorKey) { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable(editorKey) { mutableStateOf(false) }
    var confirmOutsideWindow by rememberSaveable(editorKey) { mutableStateOf(false) }
    var validationRequested by rememberSaveable(editorKey) { mutableStateOf(false) }
    val parsedValue = value.toWhipDoubleOrNull()
    val dateInFuture = date.isAfter(today)
    val dateOutsideGoalWindow = date.isBefore(projection.goal.startDate) ||
        projection.goal.deadline?.let(date::isAfter) == true
    val dirty = value != initialValue || note != initialNote || date != initialDate
    val focusManager = LocalFocusManager.current
    fun requestDismiss() {
        if (saving) return
        if (dirty) confirmDiscard = true else onDismiss()
    }
    PaneAwareAlertDialog(
        testTag = "goal-measurement-dialog",
        onDismissRequest = ::requestDismiss,
        title = { Text(if (entry == null) projection.goal.recordActionLabel() else if (completionEntry) "Edit Completion" else "Edit Progress Update") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PersistenceFailureNotice(persistenceError, testTag = "goal-measurement-save-problem")
                Text("${projection.goal.icon} ${projection.goal.name}", style = MaterialTheme.typography.titleMedium)
                Text(if (completionEntry) "Each recorded completion counts once. This does not mark the Goal complete." else projection.goal.measurementEntryInstruction())
                val unitLabel = (entry?.enteredUnitId ?: projection.goal.unitId).goalUnitLabel(customUnits)
                val fieldLabel = projection.goal.measurementEntryLabel().let { label ->
                    if (unitLabel.isBlank()) label else "$label ($unitLabel)"
                }
                if (completionEntry) {
                    if (entry == null) Text("One completion", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.testTag("goal-new-completion"))
                    else {
                        WhipToggleRow(
                            title = "Counts as a Completion",
                            supportingText = "Changing this outcome records 1 or 0. Editing only the note or date keeps the original value.",
                            checked = (parsedValue ?: 0.0) > 0.0,
                            enabled = !saving,
                            onCheckedChange = { completed -> value = plainNumericValue(completionEntryValue(entry.enteredValue, completed)) },
                            modifier = Modifier.testTag("goal-completion-outcome"),
                        )
                        Text("Original recorded value: ${entry.historyTitle(customUnits)}", style = MaterialTheme.typography.bodySmall)
                    }
                } else GoalNumberField(
                    value,
                    { value = it },
                    fieldLabel,
                    required = true,
                    error = "Enter a value".takeIf { validationRequested && parsedValue == null },
                    enabled = !saving,
                    imeAction = ImeAction.Next,
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                    modifier = Modifier.testTag("goal-measurement-value"),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    enabled = !saving,
                    label = { Text("Note (optional)") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth().testTag("goal-measurement-note"),
                )
                WhipOutlinedButton(
                    enabled = !saving,
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth().testTag("goal-measurement-date"),
                ) {
                    Text("Date · ${date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}")
                }
                if (dateInFuture) Text(
                    "Progress date cannot be in the future.",
                    modifier = Modifier
                        .testTag("goal-measurement-date-problem")
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                else if (dateOutsideGoalWindow) Text(
                    "This date is outside the Goal's tracking window. It will remain in History but will not affect current progress.",
                    modifier = Modifier
                        .testTag("goal-measurement-window-warning")
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            WhipTextButton(
                enabled = !saving && !dateInFuture,
                onClick = {
                    validationRequested = true
                    value.toWhipDoubleOrNull()?.let { parsed ->
                        if (dateOutsideGoalWindow) confirmOutsideWindow = true
                        else onRecord(parsed, date, note)
                    }
                },
                modifier = Modifier.testTag("goal-measurement-save"),
            ) { Text(if (saving) "Saving…" else if (entry == null) projection.goal.recordActionLabel() else "Save Changes") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) WhipTextButton(
                    enabled = !saving,
                    onClick = { confirmDelete = true },
                    modifier = Modifier.testTag("goal-measurement-delete"),
                ) { Text("Delete") }
                WhipTextButton(
                    enabled = !saving,
                    onClick = ::requestDismiss,
                    modifier = Modifier.testTag("goal-measurement-cancel"),
                ) { Text("Cancel") }
            }
        },
        inputBlocked = saving,
        inputBlockedLabel = if (entry == null) "Saving Goal Progress" else "Updating Goal Progress",
    )
    if (showDatePicker && !saving) WhipDatePickerDialog(date, { showDatePicker = false }, { date = it; showDatePicker = false })
    if (confirmDelete && onDelete != null && !saving) {
        PaneAwareAlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete Progress Update?") },
            text = { Text("This removes the update from the Goal's history and recalculates its progress.") },
            confirmButton = {
                WhipDestructiveTextButton(onClick = { confirmDelete = false; onDelete() },
                    modifier = Modifier.testTag("goal-measurement-confirm-delete")) {
                    Text("Delete")
                }
            },
            dismissButton = { WhipTextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
    if (confirmDiscard && !saving) {
        UnsavedChangesDialog(
            subject = "progress update",
            onKeepEditing = { confirmDiscard = false },
            onDiscard = {
                confirmDiscard = false
                onDismiss()
            },
        )
    }
    if (confirmOutsideWindow && parsedValue != null && !saving) {
        PaneAwareAlertDialog(
            testTag = "goal-measurement-window-confirmation",
            onDismissRequest = { confirmOutsideWindow = false },
            paneTitle = "Confirm Progress Date",
            title = { Text("Save Outside Goal Window?") },
            text = {
                Text(
                    "The update will stay in ${projection.goal.name}'s History, but the Goal's current progress ignores dates " +
                        "before ${projection.goal.startDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}" +
                        projection.goal.deadline?.let {
                            " or after ${it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}"
                        }.orEmpty() + ".",
                )
            },
            confirmButton = {
                WhipTextButton(
                    onClick = {
                        confirmOutsideWindow = false
                        onRecord(parsedValue, date, note)
                    },
                    modifier = Modifier.testTag("goal-measurement-window-confirm"),
                ) { Text("Save to History") }
            },
            dismissButton = {
                WhipTextButton(onClick = { confirmOutsideWindow = false }) { Text("Change Date") }
            },
        )
    }
}

@Composable
internal fun GoalPermanentDeleteDialog(
    goalName: String,
    impact: GoalDeletionImpact?,
    preparing: Boolean,
    saving: Boolean,
    persistenceError: String?,
    onDismiss: () -> Unit,
    onReviewUpdatedImpact: () -> Unit,
    onConfirm: (GoalDeletionImpact) -> Unit,
) {
    val titleName = goalName.ifBlank { "Goal" }
    PaneAwareAlertDialog(
        testTag = "goal-permanent-delete-dialog",
        onDismissRequest = { if (!saving) onDismiss() },
        paneTitle = "Permanent Goal Deletion",
        title = { Text("Delete $titleName Permanently?") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PersistenceFailureNotice(persistenceError, testTag = "goal-delete-problem")
                if (preparing) {
                    Row(
                        modifier = Modifier.fillMaxWidth().semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = "Reviewing permanent deletion impact"
                        },
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                        Text("Reviewing everything this deletion will remove…")
                    }
                }
                impact?.let { reviewed ->
                    Text("This cannot be undone. The reviewed deletion includes:")
                    Text(
                        "• The ${if (reviewed.archived) "archived " else ""}${reviewed.status.lowercase()} Goal " +
                            "and its measurement definition",
                    )
                    Text(
                        "• ${reviewed.progressEntryCount} progress update" +
                            if (reviewed.progressEntryCount == 1) "" else "s",
                    )
                    Text(
                        "• ${reviewed.milestoneCount} milestone" +
                            if (reviewed.milestoneCount == 1) " (${reviewed.completedMilestoneCount} completed)"
                            else "s (${reviewed.completedMilestoneCount} completed)",
                    )
                    if (reviewed.closureSnapshotCount > 0) Text(
                        "• ${reviewed.closureSnapshotCount} historical completion or abandonment snapshot" +
                            if (reviewed.closureSnapshotCount == 1) "" else "s",
                    )
                    if (reviewed.elapsedResetEventCount > 0) Text(
                        "• ${reviewed.elapsedResetEventCount} timer reset history event" +
                            if (reviewed.elapsedResetEventCount == 1) "" else "s",
                    )
                    Text(
                        "Export a backup first if you may need this history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!persistenceError.isNullOrBlank()) {
                    WhipOutlinedButton(
                        enabled = !saving,
                        onClick = onReviewUpdatedImpact,
                        modifier = Modifier.fillMaxWidth().testTag("goal-delete-review-impact"),
                    ) {
                        Text(if (impact == null) "Retry Impact Review" else "Review Updated Impact")
                    }
                }
            }
        },
        confirmButton = {
            WhipDestructiveTextButton(
                enabled = impact != null && persistenceError.isNullOrBlank() && !saving,
                onClick = { impact?.let(onConfirm) },
                modifier = Modifier.testTag("goal-delete-confirm"),
            ) {
                Text(if (saving) "Deleting…" else "Delete Permanently")
            }
        },
        dismissButton = {
            WhipTextButton(
                enabled = !saving,
                onClick = onDismiss,
                modifier = Modifier.testTag("goal-delete-cancel"),
            ) { Text("Cancel") }
        },
        inputBlocked = saving,
        inputBlockedLabel = "Permanently Deleting Goal",
    )
}

@Composable
internal fun GoalActionsDialog(
    projection: GoalProjection,
    modifier: Modifier = Modifier,
    zoneId: ZoneId,
    nowMillis: Long,
    customUnits: List<UnitDefinition> = emptyList(),
    onDismiss: () -> Unit,
    onEditMeasurement: (MeasurementEntry) -> Unit,
    onRecordProgress: () -> Unit,
    onResetElapsed: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onPin: () -> Unit,
    onPause: () -> Unit,
    onComplete: () -> Unit,
    onAbandon: () -> Unit,
    onReopen: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    mutationSaving: Boolean = false,
    mutationError: String? = null,
    onToggleMilestone: (GoalMilestoneBoundary, Boolean) -> Unit = { _, _ -> },
    through: LocalDate = LocalWhipToday.current,
) {
    var historyQuery by rememberSaveable(projection.goal.id) { mutableStateOf("") }
    val historyLocale = LocalConfiguration.current.locales[0]
    val historyIndex = remember(projection.entries, customUnits, historyLocale) {
        projection.entries.map { it to it.goalHistorySearchText(customUnits, historyLocale) }
    }
    val normalizedHistoryQuery = historyQuery.trim().lowercase(Locale.ROOT)
    val matchingMeasurements = remember(historyIndex, normalizedHistoryQuery) {
        historyIndex.filter { normalizedHistoryQuery in it.second }.map { it.first }
    }
    var visibleMeasurements by rememberSaveable(projection.goal.id, normalizedHistoryQuery) { mutableIntStateOf(25) }
    var visibleTrendPoints by rememberSaveable(projection.goal.id) { mutableIntStateOf(25) }
    var showAccessibleTable by rememberSaveable(projection.goal.id) { mutableStateOf(false) }
    var section by rememberSaveable(projection.goal.id) { mutableStateOf(GoalDetailSection.Overview) }
    val insights = remember(projection, through) { buildGoalInsights(projection.goal, projection.entries, projection.milestones, through) }
    val primaryAction = if (projection.goal.archived) {
        EntityInspectorPrimaryAction("restore", "Restore Goal", onArchive)
    } else {
        when (projection.goal.status) {
            GoalStatus.Active -> when (projection.goal.type) {
                GoalType.WeightedMilestones -> null
                GoalType.ElapsedSince -> null
                GoalType.OpenEndedTrend -> EntityInspectorPrimaryAction("add-update", if (projection.goal.aggregation == GoalAggregation.CompletionCount) "Record Completion" else "Log an Update", onRecordProgress)
                else -> EntityInspectorPrimaryAction("log-progress", projection.goal.recordActionLabel(), onRecordProgress)
            }
            GoalStatus.Paused -> EntityInspectorPrimaryAction("resume", "Resume Goal", onPause)
            GoalStatus.Archived -> EntityInspectorPrimaryAction("restore", "Restore Goal", onArchive)
            GoalStatus.Completed, GoalStatus.Abandoned -> EntityInspectorPrimaryAction("reopen", "Reopen Goal", onReopen)
        }
    }
    EntityInspector(
        entityType = "Goal",
        stateKey = "goal:${projection.goal.id}",
        title = projection.goal.name,
        emoji = projection.goal.icon,
        context = projection.goal.area.ifBlank { projection.goal.type.displayLabel() },
        status = if (projection.goal.archived) "Archived" else projection.goal.status.inspectorLabel(),
        statusTone = if (projection.goal.archived) WhipStatusTone.Neutral else projection.goal.status.inspectorStatusTone(),
        sections = GoalDetailSection.entries.map { it.inspectorSection },
        selectedSectionId = section.id,
        onSelectSection = { id -> section = GoalDetailSection.entries.first { it.id == id } },
        onDismiss = onDismiss,
        onEdit = onEdit,
        editLabel = "Edit Goal",
        modifier = modifier,
        connectedSurfaceTag = "goal-detail-surface",
        connectedSectionTagPrefix = "goal-detail-section",
        primaryAction = primaryAction,
        inputBlocked = mutationSaving,
        inputBlockedLabel = "Updating Goal",
        content = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!mutationError.isNullOrBlank()) item {
                    PersistenceFailureNotice(mutationError, testTag = "goal-action-save-problem")
                }
                if (section == GoalDetailSection.Overview) {
                item {
                    EntityInspectorInformationGroup(
                        title = if (projection.goal.type == GoalType.ElapsedSince) "Elapsed time" else "Outcome",
                        modifier = Modifier.testTag("goal-inspector-outcome-card"),
                    ) {
                        if (projection.goal.type == GoalType.ElapsedSince) {
                            val terminal = projection.terminalSnapshot
                            projection.elapsedDisplayValue(nowMillis, zoneId)?.let { display ->
                                ElapsedGoalMetric(
                                    display = display,
                                    prominent = true,
                                    modifier = Modifier.testTag("goal-inspector-outcome"),
                                )
                            } ?: Text(
                                "Exact elapsed duration was not stored for this older closure.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            projection.goal.elapsedStartMillis?.let { started ->
                                Text(
                                    if (terminal == null) {
                                        "Counting continuously since ${elapsedGoalStartLabel(started, zoneId)}"
                                    } else {
                                        "Recorded when this Goal was ${terminal.status.label.lowercase()}."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (!projection.goal.archived && projection.goal.status == GoalStatus.Active) {
                                EntityInspectorAction("reset-timer", "Reset Timer", onResetElapsed,
                                    supportingText = "Change the counter's start time; the previous start remains in History.")
                            }
                        } else {
                            Text(
                                projection.inspectorOutcome(nowMillis, customUnits, zoneId),
                                modifier = Modifier.testTag("goal-inspector-outcome"),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                            projection.consistencyPeriodReading()?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            projection.numericReading(customUnits)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        }
                        projection.goal.description.takeIf(String::isNotBlank)?.let {
                            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        projection.goal.deadline?.let {
                            EntityInspectorFact("Target date", it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
                        }
                    }
                }
                if (projection.offersCompletion()) item {
                    WhipStatusCard(
                        kind = WhipStatusKind.Success,
                        title = "Target Reached",
                        message = "You can keep recording progress or mark this Goal complete. Completing saves this outcome in History.",
                        actionLabel = "Complete Goal",
                        onAction = onComplete,
                        modifier = Modifier.testTag("goal-completion-opportunity"),
                    )
                }
                if (projection.goal.type == GoalType.WeightedMilestones) item {
                    EntityInspectorInformationGroup(
                        title = "Milestone Progress",
                        modifier = Modifier.testTag("goal-inspector-progress-card"),
                    ) {
                        GoalMilestoneProgress(projection, interactive = true)
                        GoalMilestoneChecklist(projection, onToggleMilestone, enabled = !mutationSaving)
                    }
                }
                if (projection.goal.type !in setOf(GoalType.ElapsedSince, GoalType.WeightedMilestones)) item {
                    EntityInspectorInformationGroup(
                        title = if (projection.terminalSnapshot != null) "Trend under current settings" else "Progress Insight",
                        modifier = Modifier.testTag("goal-inspector-progress-card"),
                    ) {
                        if (projection.terminalSnapshot != null) Text(
                            "The closed outcome above is frozen. This trend uses current settings; original target and calculation settings were not stored. See Lifecycle History for closure facts.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        val chartValues = insights.points.mapNotNull { it.progress ?: it.canonicalValue }
                        if (chartValues.size >= 2) {
                            GoalLineChart(
                                points = insights.points,
                                goal = projection.goal,
                                customUnits = customUnits,
                                description = "${projection.goal.name} progress chart with ${chartValues.size} points from " +
                                    "${insights.points.first().date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))} to " +
                                    insights.points.last().date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                            )
                        } else {
                            Text("Log on at least two days for a trend line.")
                        }
                        insights.ratePerDay?.let {
                            EntityInspectorFact("Daily Rate", "${formatGoalCanonicalValue(it, projection.goal.trendUnitId, projection.goal.precision, customUnits, difference = true)} per day")
                        }
                        insights.forecastDate?.let {
                            EntityInspectorFact("Forecast", it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
                        }
                        insights.forecastExplanation?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        if (insights.targetMin != null || insights.targetMax != null) {
                            val targetMinimum = insights.targetMin
                            val targetMaximum = insights.targetMax ?: targetMinimum
                            val targetRange = if (targetMinimum == targetMaximum) {
                                formatGoalCanonicalValue(targetMinimum, projection.goal.unitId, projection.goal.precision, customUnits)
                            } else {
                                "${formatGoalCanonicalValue(targetMinimum, projection.goal.unitId, projection.goal.precision, customUnits)} to " +
                                    formatGoalCanonicalValue(targetMaximum, projection.goal.unitId, projection.goal.precision, customUnits)
                            }
                            EntityInspectorFact("Target", targetRange)
                        }
                        GoalInsightEvidence(insights, projection)
                        if (insights.points.isNotEmpty()) DisclosureButton(
                            label = "Trend data table",
                            expanded = showAccessibleTable,
                            onClick = { showAccessibleTable = !showAccessibleTable },
                        )
                    }
                }
                if (showAccessibleTable) {
                    item {
                        Text("Showing ${minOf(visibleTrendPoints, insights.points.size)} of ${insights.points.size} observed days", style = MaterialTheme.typography.labelMedium)
                        if (visibleTrendPoints < insights.points.size) WhipOutlinedButton(
                            onClick = { visibleTrendPoints = (visibleTrendPoints + 25).coerceAtMost(insights.points.size) },
                            modifier = Modifier.fillMaxWidth().testTag("goal-trend-show-earlier"),
                        ) { Text("Show Earlier Days") }
                    }
                    items(insights.points.takeLast(visibleTrendPoints), key = { "insight-${it.date}" }) { point ->
                        Text(
                            "${point.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}: value ${formatGoalCanonicalValue(point.canonicalValue, projection.goal.trendUnitId, projection.goal.precision, customUnits)}, " +
                                "progress ${point.progress?.let { formatGoalProgressPercent(it) } ?: "not applicable"}, ${point.recordedEntries} update${if (point.recordedEntries == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                }
                if (section == GoalDetailSection.History) {
                    if (projection.closureSnapshots.isNotEmpty()) {
                        item {
                            WhipSectionHeading("Lifecycle History", compact = true)
                            Text("Closed values and progress are frozen. Original target and calculation settings were not stored; current settings do not reconstruct them.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "Completion and abandonment outcomes are permanent history. Reopening does not erase them.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        items(
                            projection.closureSnapshots.sortedByDescending { it.completedAtMillis },
                            key = { "goal-closure-${it.id}" },
                        ) { snapshot ->
                            Text(
                                snapshot.accessibleHistoryDescription(projection.goal, zoneId, customUnits),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("goal-closure-history-${snapshot.id}"),
                            )
                        }
                    }
                    if (projection.elapsedResetEvents.isNotEmpty()) {
                        item {
                            WhipSectionHeading("Timer Reset History", modifier = Modifier.padding(top = 8.dp), compact = true)
                            Text(
                                "Each reset preserves the previous and new counter origin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        items(
                            projection.elapsedResetEvents.sortedByDescending { it.resetAtMillis },
                            key = { "goal-reset-event-${it.id}" },
                        ) { event ->
                            Text(
                                event.accessibleHistoryDescription(zoneId),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("goal-reset-history-${event.id}"),
                            )
                        }
                    }
                    if (projection.goal.type == GoalType.ElapsedSince) {
                        if (projection.elapsedResetEvents.isEmpty()) item {
                            Text(
                                "No timer resets yet. The current start time remains editable from Reset Timer.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else if (projection.goal.type == GoalType.WeightedMilestones) {
                        item { Text("Milestone progress reflects the current checklist. Completed and abandoned outcomes are retained in Lifecycle History.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        item { WhipSectionHeading("Progress History", modifier = Modifier.padding(top = 8.dp), compact = true) }
                        if (projection.entries.isNotEmpty()) item { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            WhipSearchField(
                                label = "Search Progress History", query = historyQuery,
                                onQueryChange = { historyQuery = it },
                                hint = "Value, unit, date, or note",
                                modifier = Modifier.fillMaxWidth().testTag("goal-history-search"),
                            )
                            Text("${matchingMeasurements.size} of ${quantityLabel(projection.entries.size, "update")}",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.testTag("goal-history-result-count"))
                        } }
                        if (matchingMeasurements.isEmpty() && historyQuery.isNotBlank()) item {
                            WhipEmptyState("No Matching Updates", "Try a value, unit, date, or words from a note.",
                                primaryActionLabel = "Clear Search", onPrimaryAction = { historyQuery = "" })
                        }
                        if (projection.entries.isEmpty()) item {
                            Text("No progress updates yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        items(matchingMeasurements.take(visibleMeasurements), key = { it.id }) { entry ->
                            EntityInspectorAction(
                                id = "progress-update-${entry.id}",
                                label = if (projection.goal.aggregation == GoalAggregation.CompletionCount) {
                                    if ((entry.enteredValue ?: 0.0) > 0.0) "Completion" else "Not Counted as a Completion"
                                } else entry.historyTitle(customUnits),
                                supportingText = entry.historySupportingText() + if (projection.goal.aggregation == GoalAggregation.CompletionCount) " · Recorded value ${entry.historyTitle(customUnits)}" else "",
                                enabled = entry.isUserEditableGoalUpdate(),
                                onClick = { if (entry.isUserEditableGoalUpdate()) onEditMeasurement(entry) },
                            )
                        }
                        if (visibleMeasurements < matchingMeasurements.size) item {
                            WhipOutlinedButton(
                                onClick = { visibleMeasurements = (visibleMeasurements + 25).coerceAtMost(matchingMeasurements.size) },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Show More History · ${matchingMeasurements.size - visibleMeasurements} Remaining") }
                        }
                    }
                }
                if (section == GoalDetailSection.More) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        EntityInspectorGroup("Actions") {
                            EntityInspectorAction("duplicate", "Duplicate Goal", onDuplicate)
                        }
                        EntityInspectorGroup("Availability") {
                            if (
                                (!projection.goal.archived &&
                                    projection.goal.status in setOf(GoalStatus.Active, GoalStatus.Paused)) ||
                                projection.goal.pinned
                            ) {
                                EntityInspectorAction(
                                    "pin",
                                    if (projection.goal.pinned) "Unpin from Whip Home" else "Pin to Whip Home",
                                    onPin,
                                    supportingText = if (projection.goal.pinned) {
                                        "The Goal remains available in Goals."
                                    } else {
                                        "Keeps this active Goal in Whip Home's visible Goals summary."
                                    },
                                )
                            }
                            if (!projection.goal.archived && projection.goal.status == GoalStatus.Active) {
                                EntityInspectorAction("pause", "Pause Goal", onPause)
                                EntityInspectorAction("complete", "Complete Goal", onComplete)
                                EntityInspectorAction("abandon", "Abandon Goal", onAbandon)
                            }
                            if (!projection.goal.archived) {
                                EntityInspectorAction("archive", "Archive Goal", onArchive)
                            }
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
            }
        },
    )
}

private enum class GoalDetailSection(val id: String, val label: String) {
    Overview("overview", "Overview"),
    History("history", "History"),
    More("options", "Options"),
    ;

    val inspectorSection: EntityInspectorSection
        get() = EntityInspectorSection(id = id, label = label)
}

internal fun GoalStatus.inspectorLabel(): String = when (this) {
    GoalStatus.Active -> "Active"
    GoalStatus.Paused -> "Paused"
    GoalStatus.Completed -> "Completed"
    GoalStatus.Abandoned -> "Abandoned"
    GoalStatus.Archived -> "Archived"
}

internal fun GoalStatus.inspectorStatusTone(): WhipStatusTone = when (this) {
    GoalStatus.Active -> WhipStatusTone.Info
    GoalStatus.Paused -> WhipStatusTone.Warning
    GoalStatus.Completed -> WhipStatusTone.Success
    GoalStatus.Abandoned -> WhipStatusTone.Destructive
    GoalStatus.Archived -> WhipStatusTone.Neutral
}

private fun GoalProjection.inspectorOutcome(
    nowMillis: Long,
    customUnits: List<UnitDefinition>,
    zoneId: ZoneId,
): String = when {
    terminalSnapshot != null && goal.type == GoalType.ElapsedSince ->
        elapsedDisplayLabel(nowMillis, zoneId) ?: terminalSnapshot.status.inspectorLabel()
    terminalSnapshot != null && goal.type == GoalType.WeightedMilestones ->
        terminalSnapshot.milestoneOutcomeLabel()
            ?.replace("/", " of ")
            ?.replace(" milestones", " milestones complete")
            ?: terminalSnapshot.progress?.let { "${formatGoalProgressPercent(it)} complete" }
            ?: terminalSnapshot.status.inspectorLabel()
    goal.type == GoalType.ElapsedSince && goal.elapsedStartMillis != null ->
        elapsedDisplayLabel(nowMillis, zoneId) ?: "Ready to begin"
    goal.type == GoalType.WeightedMilestones ->
        "${milestones.count { it.completed }} of ${milestones.size} milestones complete"
    typedOutcomeReading(customUnits) != null -> requireNotNull(typedOutcomeReading(customUnits))
    progress != null -> "${formatGoalProgressPercent(progress)} complete"
    consistency != null -> with(requireNotNull(consistency)) {
        "$successfulPeriods of $requiredPeriods ${period.periodLabel} periods complete"
    }
    currentValue != null -> {
        val displayed = goal.displayValue(currentValue, customUnits)
        val unit = goal.unitId.goalUnitLabel(customUnits)
        "Current ${formatGoalValue(displayed, goal.precision)} $unit".trim()
    }
    else -> "Ready to begin"
}

internal fun MeasurementEntry.goalHistorySearchText(
    customUnits: List<UnitDefinition> = emptyList(), locale: Locale = Locale.getDefault(),
): String = "$localDate ${historyTitle(customUnits)} ${historySupportingText(locale)}".lowercase(Locale.ROOT)

internal fun MeasurementEntry.historyTitle(customUnits: List<UnitDefinition> = emptyList()): String {
    val valueLabel = enteredValue?.let(::editableNumericValue) ?: status.activityLabel()
    val unit = enteredUnitId?.goalUnitLabel(customUnits).orEmpty()
    return buildString {
        append(valueLabel)
        if (unit.isNotBlank()) append(" $unit")
    }
}

internal fun MeasurementEntry.historySupportingText(locale: Locale = Locale.getDefault()): String = buildList {
    add(localDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)))
    note.takeIf(String::isNotBlank)?.let(::add)
    if (sourceType !in setOf(MeasurementSourceType.Manual, MeasurementSourceType.Goal)) {
        sourceType.activityAttribution()?.let(::add)
    }
}.joinToString(" · ")

internal fun GoalClosureSnapshot.accessibleHistoryDescription(
    goal: Goal,
    zoneId: ZoneId,
    customUnits: List<UnitDefinition> = emptyList(),
): String = buildList {
    val outcome = when (status) {
        GoalStatus.Completed -> "Completed"
        GoalStatus.Abandoned -> "Abandoned"
        else -> status.inspectorLabel()
    }
    add(
        "$outcome on ${Instant.ofEpochMilli(completedAtMillis).atZone(zoneId).format(
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM),
        )}",
    )
    progress?.let { add("${formatGoalProgressPercent(it)} progress") }
    elapsedDurationMillis?.let { duration ->
        add("elapsed ${formatGoalDuration(duration)}")
    }
    milestoneOutcomeLabel()?.let { add(it) }
    value?.let { canonical ->
        add(
            "recorded value ${formatGoalCanonicalValue(canonical, goal.trendUnitId, goal.precision, customUnits)}" +
                if (goal.type == GoalType.Consistency) " successful periods" else
                    if (goal.aggregation == GoalAggregation.TimeInRange) " of observations in range" else "",
        )
    }
}.joinToString(" · ").trim()

private fun GoalClosureSnapshot.milestoneOutcomeLabel(): String? {
    val completed = completedMilestoneCount ?: return null
    val total = totalMilestoneCount ?: return null
    return "$completed/$total milestones"
}

internal fun GoalElapsedResetEvent.accessibleHistoryDescription(zoneId: ZoneId): String {
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
    val resetAt = Instant.ofEpochMilli(resetAtMillis).atZone(zoneId).format(formatter)
    val previous = Instant.ofEpochMilli(previousStartMillis).atZone(zoneId).format(formatter)
    val next = Instant.ofEpochMilli(newStartMillis).atZone(zoneId).format(formatter)
    return "Reset on $resetAt. Counter origin changed from $previous to $next. " +
        "Elapsed time before reset: ${formatGoalDuration(elapsedDurationMillis)}."
}

private fun formatGoalDuration(durationMillis: Long): String {
    val duration = Duration.ofMillis(durationMillis.coerceAtLeast(0L))
    val days = duration.toDays()
    val hours = duration.minusDays(days).toHours()
    val minutes = duration.minusDays(days).minusHours(hours).toMinutes()
    return buildList {
        if (days > 0) add("$days day${if (days == 1L) "" else "s"}")
        if (hours > 0 || days > 0) add("$hours hour${if (hours == 1L) "" else "s"}")
        add("$minutes minute${if (minutes == 1L) "" else "s"}")
    }.joinToString(" ")
}

internal fun MeasurementEntry.isUserEditableGoalUpdate(): Boolean =
    sourceType in setOf(MeasurementSourceType.Manual, MeasurementSourceType.Goal)

@Composable
private fun GoalLineChart(
    points: List<GoalHistoryPoint>,
    goal: Goal,
    customUnits: List<UnitDefinition>,
    description: String,
) {
    val useProgress = points.all { it.progress != null }
    val readings = points.mapNotNull { point ->
        (if (useProgress) point.progress else point.canonicalValue)?.takeIf(Double::isFinite)?.let { point.date to it }
    }
    if (readings.size < 2) return
    val min = readings.minOf { it.second }
    val max = readings.maxOf { it.second }
    val span = (max - min).takeIf { it > 0.0 } ?: 1.0
    val firstDay = readings.first().first.toEpochDay()
    val days = (readings.last().first.toEpochDay() - firstDay).coerceAtLeast(1)
    fun format(value: Double) = if (useProgress) formatGoalProgressPercent(value)
        else formatGoalCanonicalValue(value, goal.trendUnitId, goal.precision, customUnits)
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val firstLabel = "${readings.first().first.format(dateFormat)} · ${format(readings.first().second)}"
    val lastLabel = "${readings.last().first.format(dateFormat)} · ${format(readings.last().second)}"
    val lineColor = MaterialTheme.colorScheme.primary
    Text(if (useProgress) "Progress Over Time" else "Recorded Value Over Time", style = MaterialTheme.typography.labelMedium)
    Text("Range ${format(min)} to ${format(max)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Canvas(
        modifier = Modifier.fillMaxWidth().height(140.dp).semantics {
            contentDescription = "$description. From $firstLabel to $lastLabel. Range ${format(min)} to ${format(max)}. Spaced by date."
        },
    ) {
        val inset = 4.dp.toPx()
        fun position(reading: Pair<LocalDate, Double>) = androidx.compose.ui.geometry.Offset(
            inset + ((reading.first.toEpochDay() - firstDay).toDouble() / days * (size.width - inset * 2)).toFloat(),
            if (max == min) size.height / 2 else inset + ((max - reading.second) / span * (size.height - inset * 2)).toFloat(),
        )
        readings.zipWithNext().forEach { (start, end) -> drawLine(lineColor, position(start), position(end), 3.dp.toPx()) }
        readings.forEach { drawCircle(lineColor, 3.dp.toPx(), position(it)) }
    }
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro)) {
        Text(firstLabel, style = MaterialTheme.typography.bodySmall)
        Text(lastLabel, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun <T> GoalEnumDropdown(
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
private fun GoalNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    error: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) = OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    enabled = enabled,
    label = { Text(label + if (required) " *" else "") },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
    keyboardActions = keyboardActions,
    isError = error != null,
    supportingText = error?.let { message -> { Text(message) } },
    modifier = modifier.fillMaxWidth(),
)

private fun GoalType.label() = name.replace(Regex("([a-z])([A-Z])"), "$1 $2")
private fun GoalType.displayLabel(): String = when (this) {
    GoalType.ReachValue -> "Reach a Target"
    GoalType.ReduceValue -> "Reduce a Value"
    GoalType.AccumulateTotal -> "Build a Total"
    GoalType.MaintainRange -> "Stay in a Range"
    GoalType.MeetAverage -> "Meet an Average"
    GoalType.Consistency -> "Stay Consistent"
    GoalType.WeightedMilestones -> "Finish Milestones"
    GoalType.OpenEndedTrend -> "Observe a Trend"
    GoalType.ElapsedSince -> "Count Time Since"
}

private fun GoalType.explanation(): String = when (this) {
    GoalType.ReachValue -> "Move from a starting value toward a measurable target."
    GoalType.ReduceValue -> "Track a value that should move downward, such as debt or weight."
    GoalType.AccumulateTotal -> "Add contributions over time, such as savings, pages, or distance."
    GoalType.MaintainRange -> "Succeed by keeping recorded values between a minimum and maximum."
    GoalType.MeetAverage -> "Judge progress by the average value in each chosen period."
    GoalType.Consistency -> "Reach a recurring number of successes for several periods."
    GoalType.WeightedMilestones -> "Complete named project stages, optionally with different importance."
    GoalType.OpenEndedTrend -> "Observe direction and change without requiring a finish line."
    GoalType.ElapsedSince -> "Count continuously from an exact event, such as sobriety, recovery, or an anniversary."
}

private fun ElapsedDisplayUnit.displayLabel(): String = when (this) {
    ElapsedDisplayUnit.Auto -> "Automatic"
    else -> name
}

private fun GoalAggregation.displayLabel(): String = when (this) {
    GoalAggregation.Latest -> "Latest entry"
    GoalAggregation.Sum -> "Add entries together"
    GoalAggregation.Average -> "Average entries"
    GoalAggregation.Minimum -> "Lowest entry"
    GoalAggregation.Maximum -> "Highest entry"
    GoalAggregation.CompletionCount -> "Count completions"
    GoalAggregation.TimeInRange -> "Share of observations in range"
}

private fun GoalAggregationPeriod.displayLabel(): String = when (this) {
    GoalAggregationPeriod.All -> "All Time"
    GoalAggregationPeriod.Day -> "Each day"
    GoalAggregationPeriod.Week -> "Each week"
    GoalAggregationPeriod.Month -> "Each month"
    GoalAggregationPeriod.RollingDays -> "Rolling number of days"
}

private fun GoalPaceType.displayLabel(): String = when (this) {
    GoalPaceType.Linear -> "Compare progress with time elapsed"
    GoalPaceType.None -> "Do not compare pace"
}

internal fun completionEntryValue(original: Double?, completed: Boolean): Double =
    original?.takeIf { it.isFinite() && (it > 0.0) == completed } ?: if (completed) 1.0 else 0.0

internal fun Goal.recordActionLabel(): String =
    if (aggregation == GoalAggregation.CompletionCount) "Record Completion" else "Log Progress"

private fun Goal.measurementEntryInstruction(): String = when (aggregation) {
    GoalAggregation.Sum -> "Enter the amount to add. Whip adds each entry to the goal total."
    GoalAggregation.CompletionCount -> "Enter a positive value to record one completion. Zero does not count."
    GoalAggregation.Latest -> "Enter the current observed value. Whip uses the latest entry."
    GoalAggregation.Average -> "Enter an observed value. Whip averages entries in the selected time window."
    GoalAggregation.Minimum -> "Enter an observed value. Whip uses the lowest entry in the selected time window."
    GoalAggregation.Maximum -> "Enter an observed value. Whip uses the highest entry in the selected time window."
    GoalAggregation.TimeInRange -> "Enter an observed value. Whip measures how many entries fall inside the target range."
}

private fun Goal.measurementEntryLabel(): String = when (aggregation) {
    GoalAggregation.Sum -> "Amount to Add"
    GoalAggregation.CompletionCount -> "Completion Value"
    else -> "Observed Value"
}
private fun String.goalUnitLabel(customUnits: List<UnitDefinition> = emptyList()): String =
    customUnits.firstOrNull { it.id == this }?.let { it.symbol.ifBlank { it.name } }
        ?: when (this) {
            "unitless", "count" -> ""
            "kilogram" -> "kg"
            "pound" -> "lb"
            "kilometre" -> "km"
            "distance_m" -> "m"
            "second" -> "sec"
            "litre" -> "L"
            "millilitre" -> "mL"
            "fluid_ounce" -> "fl oz"
            "currency" -> "$"
            else -> this
        }
private fun formatGoalValue(value: Double?, precision: Int): String = value?.let { String.format(Locale.getDefault(), "%.${precision.coerceIn(0, 6)}f", it) } ?: "—"
private fun parseGoalClock(value: String): Int? {
    if (value.isBlank()) return null
    val parts = value.split(':')
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
    return if (hour in 0..23 && minute in 0..59) hour * 60 + minute else null
}

private fun goalValueLabel(label: String, unitId: String, customUnits: List<UnitDefinition>): String =
    unitId.goalUnitLabel(customUnits).takeIf(String::isNotBlank)?.let { "$label ($it)" } ?: label
