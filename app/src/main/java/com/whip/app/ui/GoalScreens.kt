package com.whip.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import com.whip.app.core.AppSettings
import com.whip.app.core.zoneId
import com.whip.app.domain.Goal
import com.whip.app.domain.Habit
import com.whip.app.domain.Area
import com.whip.app.domain.CustomIdentityEmoji
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalAggregationPeriod
import com.whip.app.domain.GoalConsistencyPeriod
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalMilestoneDraft
import com.whip.app.domain.GoalPaceType
import com.whip.app.domain.GoalProjection
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.GoalType
import com.whip.app.domain.ElapsedDisplayUnit
import com.whip.app.domain.elapsedCounter
import com.whip.app.domain.DEFAULT_GOAL_EMOJI
import com.whip.app.domain.Contribution
import com.whip.app.domain.LinkKind
import com.whip.app.domain.LinkRule
import com.whip.app.domain.LinkRuleDraft
import com.whip.app.domain.LinkSourceMetric
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.LinkValueMode
import com.whip.app.domain.MetricEntry
import com.whip.app.domain.MetricDefinition
import com.whip.app.domain.TrackAggregation
import com.whip.app.domain.TrackCondition
import com.whip.app.domain.TrackConditionMode
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.displayValue
import com.whip.app.domain.buildGoalInsights
import com.whip.app.domain.compatibleAggregations
import com.whip.app.domain.defaultAggregation
import com.whip.app.domain.defaultDirection
import com.whip.app.domain.editableNumericValue
import com.whip.app.domain.toWhipDoubleOrNull
import com.whip.app.domain.validationErrors
import com.whip.app.ui.theme.whipColors
import java.time.LocalDate
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.delay
import java.util.Locale

enum class GoalDestination { Active, Completed, Archived, Insights }
private enum class LinkHistoryChoice { NewEntriesOnly, SinceGoalStart, SinceDate, AllHistory }

@Composable
fun GoalAreaContent(
    state: GoalUiState,
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
    onCreateCustomUnit: CreateCustomUnitAction = { _, _, _, _, result ->
        result(Result.failure(IllegalStateException("Custom-unit creation is unavailable")))
    },
    customIdentityEmojis: List<CustomIdentityEmoji> = emptyList(),
    onSaveIdentityEmoji: (CustomIdentityEmoji) -> Unit = {},
    onRemoveSavedIdentityEmoji: (String) -> Unit = {},
    areaScopeLabel: String? = null,
    onShowAllAreasForReorder: () -> Unit = {},
    onAreaChanged: (String?) -> Unit = {},
    destinationState: MutableState<GoalDestination>? = null,
    showWorkspace: Boolean = true,
    onReorderModeChange: (Boolean) -> Unit = {},
    reorderDismissRequest: Int = 0,
) {
    if (state.loading || state.errorMessage != null) {
        DomainLoadContent("goals", innerPadding, state.errorMessage, viewModel::retryLoading)
        return
    }
    val localDestinationState = rememberSaveable { mutableStateOf(GoalDestination.Active) }
    val compactItemLayout = LocalCompactItemLayout.current
    val activeDestinationState = destinationState ?: localDestinationState
    var destination by activeDestinationState
    var creating by rememberSaveable { mutableStateOf(false) }
    var editingGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var recordingGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var actionsGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var linkingGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingLinkRuleId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingMeasurementGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingMeasurementId by rememberSaveable { mutableStateOf<String?>(null) }
    var overridingContributionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var resettingElapsedGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var manageOrder by rememberSaveable { mutableStateOf(false) }
    var toolsExpanded by rememberSaveable { mutableStateOf(false) }
    var deleteCandidateGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var templatesOpen by rememberSaveable { mutableStateOf(false) }
    var templateDraft by rememberSaveable { mutableStateOf<GoalDraft?>(null) }
    var editorSavePending by rememberSaveable { mutableStateOf(false) }
    var linkSavePending by rememberSaveable { mutableStateOf(false) }
    val projectionById = (state.active + state.completed + state.archived).associateBy { it.goal.id }
    val editing = editingGoalId?.let(projectionById::get)
    val recording = recordingGoalId?.let(projectionById::get)
    val actions = actionsGoalId?.let(projectionById::get)
    val linking = linkingGoalId?.let(projectionById::get)
    val editingLinkRule = editingLinkRuleId?.let { id -> state.linkRules.firstOrNull { it.id == id } }
    val editingMeasurement = editingMeasurementGoalId?.let(projectionById::get)?.let { projection ->
        editingMeasurementId?.let { id -> projection.entries.firstOrNull { it.id == id } }?.let { projection to it }
    }
    val overridingContribution = overridingContributionId?.let { id -> state.contributions.firstOrNull { it.id == id } }
    val resettingElapsed = resettingElapsedGoalId?.let(projectionById::get)
    val deleteCandidate = deleteCandidateGoalId?.let(projectionById::get)
    var elapsedNowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.active.any { it.goal.type == GoalType.ElapsedSince }) {
        while (state.active.any { it.goal.type == GoalType.ElapsedSince }) {
            elapsedNowMillis = System.currentTimeMillis()
            delay(30_000)
        }
    }
    LaunchedEffect(createRequested, recordGoalIdRequest, resetElapsedGoalIdRequest, state.active) {
        if (createRequested) creating = true
        if (recordGoalIdRequest != null && state.active.any { it.goal.id == recordGoalIdRequest }) recordingGoalId = recordGoalIdRequest
        if (resetElapsedGoalIdRequest != null && state.active.any {
                it.goal.id == resetElapsedGoalIdRequest && it.goal.type == GoalType.ElapsedSince
            }
        ) {
            destination = GoalDestination.Active
            resettingElapsedGoalId = resetElapsedGoalIdRequest
        }
        if (createRequested || recordGoalIdRequest != null || resetElapsedGoalIdRequest != null) {
            onExternalRequestConsumed()
        }
    }
    LaunchedEffect(openGoalIdRequest, state.active, state.completed, state.archived) {
        val requestedId = openGoalIdRequest ?: return@LaunchedEffect
        val projection = (state.active + state.completed + state.archived)
            .firstOrNull { it.goal.id == requestedId }
            ?: return@LaunchedEffect
        destination = when {
            projection in state.completed -> GoalDestination.Completed
            projection in state.archived -> GoalDestination.Archived
            else -> GoalDestination.Active
        }
        actionsGoalId = projection.goal.id
        onOpenGoalRequestConsumed()
    }
    LaunchedEffect(editGoalIdRequest, state.active, state.completed, state.archived) {
        val requestedId = editGoalIdRequest ?: return@LaunchedEffect
        val projection = (state.active + state.completed + state.archived)
            .firstOrNull { it.goal.id == requestedId }
            ?: return@LaunchedEffect
        destination = when {
            projection in state.completed -> GoalDestination.Completed
            projection in state.archived -> GoalDestination.Archived
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
            selected = destination,
            destinations = GoalDestination.entries,
            primaryDestinations = listOf(GoalDestination.Active, GoalDestination.Completed, GoalDestination.Insights),
            onSelect = {
                manageOrder = false
                destination = it
            },
            label = GoalDestination::name,
            compactLabel = { if (it == GoalDestination.Completed) "Done" else it.name },
            testTagPrefix = "goal-destination",
            barTestTag = "goal-workspace-navigation",
        )
        if (destination == GoalDestination.Insights) {
            GoalInsightsContent(
                projections = state.active,
                innerPadding = WhipPageContentPadding,
                onOpen = { actionsGoalId = it.goal.id },
            )
        } else WhipReorderLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = WhipPageContentPadding,
            verticalArrangement = Arrangement.spacedBy(if (compactItemLayout) 4.dp else 10.dp),
        ) {
            item {
                WhipPageHeader(
                    title = destination.name + " Goals",
                    supportingText = "Long-term measurements, consistency, ranges, totals, and project milestones.",
                ) {
                    if (!manageOrder && destination == GoalDestination.Active && (list.isNotEmpty() || areaScopeLabel != null)) Box {
                        WhipPageIconAction(
                            icon = Icons.Outlined.MoreVert,
                            label = "More Goal Actions",
                            onClick = { toolsExpanded = true },
                        )
                        DropdownMenu(expanded = toolsExpanded, onDismissRequest = { toolsExpanded = false }) {
                            WhipMenuItem(
                                modifier = Modifier.testTag("goal-browse-templates-menu-action"),
                                label = "Browse Templates",
                                onClick = { toolsExpanded = false; templatesOpen = true },
                            )
                            if (list.size > 1 || areaScopeLabel != null) WhipMenuItem(
                                label = if (areaScopeLabel == null) "Reorder Goals" else "Show All Areas & Reorder",
                                onClick = {
                                    toolsExpanded = false
                                    if (areaScopeLabel != null) onShowAllAreasForReorder()
                                    manageOrder = true
                                },
                            )
                        }
                    }
                }
            }
            if (manageOrder) item {
                WhipReorderModeBar(
                    itemLabel = "Goals",
                    onDone = { manageOrder = false },
                    boundaryNote = "Pinned and other Goals reorder separately.",
                )
            }
            if (list.isEmpty()) item {
                val firstUse = state.active.isEmpty() && state.completed.isEmpty() && state.archived.isEmpty()
                WhipEmptyState(
                    title = if (firstUse && destination == GoalDestination.Active) "Start Your First Goal" else "No Goals Here",
                    supportingText = if (destination == GoalDestination.Active) {
                        if (firstUse) {
                            "Choose a template for a fast start, or use + to define an outcome from scratch."
                        } else areaScopeLabel?.let { "No active goals in $it." } ?: "Create a goal or start from a template."
                    } else areaScopeLabel?.let { "Nothing in $it yet." } ?: "Nothing here yet.",
                    primaryActionLabel = "Browse Templates".takeIf { destination == GoalDestination.Active },
                    onPrimaryAction = { templatesOpen = true }.takeIf { destination == GoalDestination.Active },
                )
            }
            items(list, key = { it.goal.id }) { projection ->
                val index = list.indexOfFirst { it.goal.id == projection.goal.id }
                Column {
                    if (
                        manageOrder && destination == GoalDestination.Active &&
                        (index == 0 || list[index - 1].goal.pinned != projection.goal.pinned)
                    ) {
                        Text(
                            if (projection.goal.pinned) "Pinned Goals" else "Other Goals",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                    val card: @Composable () -> Unit = {
                        GoalCard(
                        projection,
                        customUnits = state.customUnits,
                        nowMillis = elapsedNowMillis,
                        onOpen = { actionsGoalId = projection.goal.id },
                        onEdit = { editingGoalId = projection.goal.id },
                        onRecord = { recordingGoalId = projection.goal.id },
                        onToggleMilestone = viewModel::toggleMilestone,
                        onResetElapsed = { resettingElapsedGoalId = projection.goal.id },
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
    if (creating || editing != null) {
        GoalEditorDialog(
            modifier = editorModifier,
            projection = editing,
            initialDraft = templateDraft.takeIf { editing == null },
            today = state.currentDate,
            customUnits = state.customUnits,
            defaults = viewModel.defaultSettings(),
            areas = areas,
            defaultAreaId = defaultAreaId,
            onCreateArea = onCreateArea,
            onCreateCustomUnit = onCreateCustomUnit,
            customIdentityEmojis = customIdentityEmojis,
            onSaveIdentityEmoji = onSaveIdentityEmoji,
            onRemoveSavedIdentityEmoji = onRemoveSavedIdentityEmoji,
            saving = editorSavePending,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onDismiss = {
                creating = false
                editingGoalId = null
                templateDraft = null
                editorSavePending = false
            },
            onSave = { draft ->
                editorSavePending = true
                viewModel.saveGoal(editing?.goal?.id, draft) { succeeded ->
                    editorSavePending = false
                    if (succeeded) {
                        creating = false
                        editingGoalId = null
                        templateDraft = null
                    }
                }
                onAreaChanged(draft.areaId)
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
    resettingElapsed?.let { projection ->
        ElapsedGoalResetDialog(
            goal = projection.goal,
            zoneId = viewModel.defaultSettings().zoneId(),
            onDismiss = { resettingElapsedGoalId = null },
            onReset = { instant ->
                viewModel.resetElapsedStart(projection.goal.id, instant)
                resettingElapsedGoalId = null
            },
        )
    }
    recording?.let { projection ->
        GoalMeasurementDialog(
            projection,
            state.currentDate,
            entry = null,
            onDismiss = { recordingGoalId = null },
            onRecord = { value, date, note -> viewModel.record(projection.goal.id, value, date, note); recordingGoalId = null },
        )
    }
    editingMeasurement?.let { (projection, entry) ->
        GoalMeasurementDialog(
            projection = projection,
            today = state.currentDate,
            entry = entry,
            onDismiss = { editingMeasurementGoalId = null; editingMeasurementId = null },
            onRecord = { value, date, note ->
                viewModel.updateMeasurement(projection.goal.id, entry.id, value, date, note)
                editingMeasurementGoalId = null; editingMeasurementId = null
            },
            onDelete = {
                viewModel.deleteMeasurement(projection.goal.id, entry.id)
                editingMeasurementGoalId = null; editingMeasurementId = null
            },
        )
    }
    actions?.let { projection ->
        GoalActionsDialog(
            projection,
            modifier = modifier,
            rules = state.linkRules.filter { it.targetGoalId == projection.goal.id },
            contributions = state.contributions.filter { it.targetGoalId == projection.goal.id },
            sourceGoals = (state.active + state.completed + state.archived).map { it.goal },
            sourceHabits = state.sourceHabits,
            sourceMetrics = state.sourceMetrics,
            onDismiss = { actionsGoalId = null },
            onAddLink = { linkingGoalId = projection.goal.id; editingLinkRuleId = null; actionsGoalId = null; viewModel.clearLinkPreview() },
            onEditLink = { rule ->
                linkingGoalId = projection.goal.id
                editingLinkRuleId = rule.id
                actionsGoalId = null
                viewModel.clearLinkPreview()
            },
            onSetLinkEnabled = viewModel::setLinkEnabled,
            onDeleteLink = viewModel::deleteLink,
            onSetContributionExcluded = viewModel::setContributionExcluded,
            onOverrideContribution = { contribution -> overridingContributionId = contribution.id; actionsGoalId = null },
            onEditMeasurement = { entry -> editingMeasurementGoalId = projection.goal.id; editingMeasurementId = entry.id; actionsGoalId = null },
            onRecordProgress = { recordingGoalId = projection.goal.id; actionsGoalId = null },
            onResetElapsed = { resettingElapsedGoalId = projection.goal.id; actionsGoalId = null },
            onEdit = { editingGoalId = projection.goal.id; actionsGoalId = null },
            onDuplicate = { viewModel.duplicate(projection.goal.id); actionsGoalId = null },
            onPin = { viewModel.setPinned(projection.goal.id, !projection.goal.pinned); actionsGoalId = null },
            onPause = { viewModel.setStatus(projection.goal.id, if (projection.goal.status == GoalStatus.Paused) GoalStatus.Active else GoalStatus.Paused); actionsGoalId = null },
            onComplete = { viewModel.setStatus(projection.goal.id, GoalStatus.Completed); actionsGoalId = null },
            onAbandon = { viewModel.setStatus(projection.goal.id, GoalStatus.Abandoned); actionsGoalId = null },
            onReopen = { viewModel.setStatus(projection.goal.id, GoalStatus.Active); actionsGoalId = null },
            onArchive = { viewModel.setStatus(projection.goal.id, if (projection.goal.status == GoalStatus.Archived) GoalStatus.Active else GoalStatus.Archived); actionsGoalId = null },
            onDelete = { deleteCandidateGoalId = projection.goal.id; actionsGoalId = null },
        )
    }
    deleteCandidate?.let { projection ->
        val goal = projection.goal
        val dependentRules = state.linkRules.count { it.targetGoalId == goal.id || it.sourceMetricId == goal.metricId }
        val linkedEntries = state.contributions.count { it.targetGoalId == goal.id }
        PermanentDeleteDialog(
            title = "Delete ${goal.name} Permanently?",
            impacts = listOf(
                "${projection.entries.size} measurement${if (projection.entries.size == 1) "" else "s"} and all milestones will be removed",
                "$dependentRules incoming or outgoing Goal Automation${if (dependentRules == 1) "" else "s"} will be removed",
                "$linkedEntries generated contribution record${if (linkedEntries == 1) "" else "s"} will be removed",
            ),
            onDismiss = { deleteCandidateGoalId = null },
            onConfirm = { viewModel.deletePermanently(goal.id); deleteCandidateGoalId = null },
        )
    }
    linking?.let { projection ->
        GoalLinkEditorDialog(
            projection = projection,
            state = state,
            initialRule = editingLinkRule,
            saving = linkSavePending,
            onDismiss = { if (!linkSavePending) { linkingGoalId = null; editingLinkRuleId = null; viewModel.clearLinkPreview() } },
            onPreview = viewModel::previewLink,
            onClearPreview = viewModel::clearLinkPreview,
            onSave = { draft, includeHistory ->
                linkSavePending = true
                val onFinished: (Boolean) -> Unit = { succeeded ->
                    linkSavePending = false
                    if (succeeded) {
                        linkingGoalId = null
                        editingLinkRuleId = null
                    }
                }
                if (editingLinkRule == null) viewModel.createLink(draft, includeHistory, onFinished)
                else viewModel.updateLink(editingLinkRule.id, draft, onFinished)
            },
        )
    }
    overridingContribution?.let { contribution ->
        ContributionOverrideDialog(
            contribution = contribution,
            onDismiss = { overridingContributionId = null },
            onSave = { value ->
                viewModel.setContributionOverride(contribution.id, value)
                overridingContributionId = null
            },
        )
    }
}

internal fun GoalProjection.collectionStatus(
    customUnits: List<UnitDefinition> = emptyList(),
    nowMillis: Long = System.currentTimeMillis(),
): String {
    val goal = this.goal
    return when {
        goal.type == GoalType.ElapsedSince && goal.elapsedStartMillis != null ->
            elapsedCounter(goal.elapsedStartMillis, nowMillis, goal.elapsedDisplayUnit).label()
        goal.type == GoalType.WeightedMilestones ->
            "${milestones.count { it.completed }}/${milestones.size} milestones"
        progress != null -> "${(progress * 100).toInt()}% complete"
        consistency != null -> with(requireNotNull(consistency)) {
            "$successfulPeriods/$requiredPeriods ${period.name.lowercase()} periods"
        }
        currentValue != null ->
            "Current ${formatGoalValue(goal.displayValue(currentValue, customUnits), goal.precision)} ${goal.unitId.goalUnitLabel()}".trim()
        else -> goal.type.displayLabel()
    }
}

@Composable
fun GoalCard(
    projection: GoalProjection,
    customUnits: List<UnitDefinition> = emptyList(),
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onRecord: () -> Unit,
    onResetElapsed: () -> Unit,
    onToggleMilestone: (Long, Boolean) -> Unit,
    nowMillis: Long = System.currentTimeMillis(),
    reorderMode: Boolean = false,
) {
    val goal = projection.goal
    val compact = LocalCompactItemLayout.current
    val disclosure = rememberCompactItemDisclosure(itemKey = "goal:${goal.id}")
    val compactStatus = projection.collectionStatus(customUnits, nowMillis)
    val primaryAction: (@Composable () -> Unit)? = when {
        reorderMode -> null
        goal.status == GoalStatus.Active && goal.type !in setOf(GoalType.WeightedMilestones, GoalType.ElapsedSince) -> {{
            ItemPrimaryTextButton("Log", onRecord)
        }}
        goal.status == GoalStatus.Active && goal.type == GoalType.ElapsedSince -> {{
            ItemPrimaryTextButton("Reset", onResetElapsed)
        }}
        else -> null
    }
    ProductivityItemCard(
        modifier = Modifier
            .then(
                if (reorderMode) Modifier
                else Modifier.clickable(onClickLabel = "Open goal details for ${goal.name}", onClick = onOpen),
            )
            .testTag("goal-card-${goal.id}")
            .then(
                if (reorderMode) Modifier
                else Modifier.semantics { contentDescription = "Open goal details for ${goal.name}" },
            ),
    ) {
        ProductivityItemHeader(
            itemType = "goal",
            itemName = goal.name,
            emoji = goal.icon,
            areaId = goal.areaId,
            areaName = goal.area,
            onEdit = onEdit.takeUnless { reorderMode },
            identityModifier = Modifier.testTag("goal-icon-${goal.id}"),
            primaryActionModifier = Modifier.testTag("goal-primary-action-${goal.id}"),
            editModifier = Modifier.testTag("goal-edit-action-${goal.id}"),
            supportingContent = {
                Text(
                    goal.type.displayLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            compactSummaryContent = {
                Text(
                    compactStatus,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            },
            compactExpanded = disclosure.expanded,
            onCompactExpansionToggle = disclosure.toggle.takeIf { compact && !reorderMode },
            compactExpansionTag = "goal-expand-${goal.id}",
            compactPrimaryActionWidth = if (goal.type == GoalType.ElapsedSince) 80.dp else 64.dp,
            primaryAction = primaryAction,
        )
        if (!reorderMode && (!compact || disclosure.expanded)) {
        projection.progress?.let { progress ->
            val progressColor = if (progress >= 1.0) MaterialTheme.whipColors.success else MaterialTheme.whipColors.action
            if (compact) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LinearProgressIndicator(
                        progress = { progress.toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.weight(1f),
                        color = progressColor,
                    )
                    Text("${(progress * 100).toInt()}% complete", style = MaterialTheme.typography.labelSmall, color = progressColor)
                }
            } else {
                LinearProgressIndicator(progress = { progress.toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = progressColor)
                Text("${(progress * 100).toInt()}% complete", color = progressColor)
            }
        }
        if (goal.type == GoalType.ElapsedSince) {
            val started = goal.elapsedStartMillis
            if (started != null) {
                Text(
                    elapsedCounter(started, nowMillis, goal.elapsedDisplayUnit).label(),
                    style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        } else projection.consistency?.let { consistency ->
            Text(
                "${consistency.successfulPeriods}/${consistency.requiredPeriods} successful ${consistency.period.name.lowercase()} periods · " +
                    "${formatGoalValue(consistency.currentPeriodValue, goal.precision)}/${formatGoalValue(consistency.targetPerPeriod, goal.precision)} this period",
            )
        } ?: projection.currentValue?.let { canonical ->
            val current = goal.displayValue(canonical, customUnits)
            Text("Current: ${formatGoalValue(current, goal.precision)} ${goal.unitId.goalUnitLabel()}")
        }
        val pace = when (projection.onPace) { true -> "On pace"; false -> "Behind pace"; null -> null }
        if (pace != null) Text(pace + (projection.forecastDate?.let { " · forecast ${it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}" } ?: ""), style = MaterialTheme.typography.labelMedium)
        projection.milestones.forEach { milestone ->
            val milestoneModifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("goal-milestone-${milestone.id}")
                .toggleable(
                    value = milestone.completed,
                    role = Role.Checkbox,
                    onValueChange = { onToggleMilestone(milestone.id, !milestone.completed) },
                )
                .semantics {
                    contentDescription = if (milestone.completed) {
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
        if (goal.description.isNotBlank()) Text(goal.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ElapsedGoalResetDialog(
    goal: Goal,
    zoneId: ZoneId,
    onDismiss: () -> Unit,
    onReset: (Instant) -> Unit,
) {
    val original = Instant.ofEpochMilli(goal.elapsedStartMillis ?: System.currentTimeMillis()).atZone(zoneId)
    var date by rememberSaveable(goal.id) { mutableStateOf(original.toLocalDate()) }
    var minutes by rememberSaveable(goal.id) { mutableIntStateOf(original.hour * 60 + original.minute) }
    var datePicker by rememberSaveable(goal.id) { mutableStateOf(false) }
    val selected = date.atTime(LocalTime.of(minutes / 60, minutes % 60)).atZone(zoneId).toInstant()
    PaneAwareAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset ${goal.name}?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Choose the new event time. This replaces the previous counter origin; it does not delete the Goal.")
                WhipOutlinedButton(onClick = { datePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
                }
                ClockPickerButton("Start Time", minutes, { if (it != null) minutes = it })
                if (selected.isAfter(Instant.now())) Text("Start time cannot be in the future.", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            WhipTextButton(enabled = !selected.isAfter(Instant.now()), onClick = { onReset(selected) }) { Text("Reset to Chosen Time") }
        },
        dismissButton = {
            Row {
                WhipTextButton(onClick = { onReset(Instant.now()) }) { Text("Reset to Now") }
                WhipTextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
    if (datePicker) WhipDatePickerDialog(date, { datePicker = false }, { date = it; datePicker = false })
}

@Composable
private fun GoalInsightsContent(
    projections: List<GoalProjection>,
    innerPadding: PaddingValues,
    onOpen: (GoalProjection) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = innerPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            WhipPageHeader(
                title = "Goal Insights",
                supportingText = "Trends, pace, forecasts, and data quality for active goals.",
            )
        }
        if (projections.isEmpty()) item {
            WhipEmptyState(
                title = "No Goal Insights Yet",
                supportingText = "Create and log a goal to see its trend here.",
            )
        }
        items(projections, key = { "goal-insight-${it.goal.id}" }) { projection ->
            val insights = remember(projection) { buildGoalInsights(projection.goal, projection.entries, projection.milestones) }
            ProductivityItemCard(
                modifier = Modifier
                    .clickable(onClickLabel = "Open ${projection.goal.name}") { onOpen(projection) }
                    .testTag("goal-insight-${projection.goal.id}"),
            ) {
                    ProductivityItemHeader(
                        itemType = "goal",
                        itemName = projection.goal.name,
                        emoji = projection.goal.icon,
                        areaId = projection.goal.areaId,
                        areaName = projection.goal.area,
                        onEdit = null,
                        identityModifier = Modifier.testTag("goal-insight-icon-${projection.goal.id}"),
                        supportingContent = {
                            Text(
                                projection.goal.type.displayLabel(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                    val chartValues = insights.points.mapNotNull { it.progress ?: it.canonicalValue }
                    if (projection.goal.type == GoalType.ElapsedSince) {
                        projection.goal.elapsedStartMillis?.let { started ->
                            Text(
                                elapsedCounter(started, System.currentTimeMillis(), projection.goal.elapsedDisplayUnit).label(),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text("Counting continuously since ${Instant.ofEpochMilli(started)}")
                        }
                    } else if (chartValues.size >= 2) {
                        GoalLineChart(
                            values = chartValues,
                            description = "${projection.goal.name} trend with ${chartValues.size} points",
                        )
                    } else Text("Log at least two observations for a trend line.")
                    if (projection.goal.type != GoalType.ElapsedSince) Text(
                        listOfNotNull(
                            projection.progress?.let { "${(it * 100).toInt()}% complete" },
                            projection.onPace?.let { if (it) "On pace" else "Behind pace" },
                            insights.ratePerDay?.let { "Rate ${formatGoalValue(it, projection.goal.precision)} per day" },
                            insights.forecastDate?.let { "Forecast $it" },
                        ).joinToString(" · ").ifBlank { "Progress becomes available after the first log." },
                    )
                    if (projection.goal.type != GoalType.ElapsedSince) Text(insights.dataQualityExplanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun GoalTemplateDialog(
    today: LocalDate,
    defaults: AppSettings,
    onDismiss: () -> Unit,
    onChoose: (GoalDraft) -> Unit,
) {
    val templates = listOf(
        "Reach a weight" to GoalDraft(
            name = "Weight target", icon = "⚖️", type = GoalType.ReduceValue,
            dimension = UnitDimension.Mass, unitId = defaults.massUnitId,
            targetMin = if (defaults.massUnitId == "pound") 150.0 else 75.0,
            aggregation = GoalAggregation.Latest,
            startDate = today,
        ),
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
            elapsedDisplayUnit = ElapsedDisplayUnit.Auto,
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

private fun goalTemplateDescription(label: String): String = when (label) {
    "Reach a weight" -> "A current measurement moving toward a mass target."
    "Build savings" -> "Add deposits toward a money total."
    "Cover a distance" -> "Accumulate walks, runs, rides, or any distance."
    "Read pages" -> "Add pages toward a reading total."
    "Stay consistent" -> "Hit a weekly success target for twelve weeks."
    "Finish a project" -> "Plan, build, and finish weighted milestones."
    "Count time since" -> "Count from a chosen date and time for recovery, sobriety, streaks, or anniversaries."
    else -> "Prefills an editable goal; nothing is saved until you confirm."
}

@Composable
private fun GoalEditorDialog(
    projection: GoalProjection?,
    modifier: Modifier = Modifier,
    initialDraft: GoalDraft? = null,
    today: LocalDate,
    customUnits: List<UnitDefinition>,
    defaults: AppSettings = AppSettings(),
    onDismiss: () -> Unit,
    onSave: (GoalDraft) -> Unit,
    onRequestNotificationPermission: () -> Unit = {},
    saving: Boolean = false,
    areas: List<Area> = emptyList(),
    defaultAreaId: String? = null,
    onCreateArea: (String, Long?, (Result<String>) -> Unit) -> Unit = { _, _, _ -> },
    onCreateCustomUnit: CreateCustomUnitAction = { _, _, _, _, result ->
        result(Result.failure(IllegalStateException("Custom-unit creation is unavailable")))
    },
    customIdentityEmojis: List<CustomIdentityEmoji> = emptyList(),
    onSaveIdentityEmoji: (CustomIdentityEmoji) -> Unit = {},
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
    var baseline by rememberSaveable(editorKey) { mutableStateOf(goal?.displayValue(goal.baseline, customUnits)?.let(::editableNumericValue) ?: initialDraft?.baseline?.let(::editableNumericValue).orEmpty()) }
    var targetMin by rememberSaveable(editorKey) { mutableStateOf(goal?.displayValue(goal.targetMin, customUnits)?.let(::editableNumericValue) ?: initialDraft?.targetMin?.let(::editableNumericValue).orEmpty()) }
    var targetMax by rememberSaveable(editorKey) { mutableStateOf(goal?.displayValue(goal.targetMax, customUnits)?.let(::editableNumericValue) ?: initialDraft?.targetMax?.let(::editableNumericValue).orEmpty()) }
    var aggregation by rememberSaveable(editorKey) {
        mutableStateOf(
            (goal?.aggregation ?: initialDraft?.aggregation)
                ?.takeIf { it in type.compatibleAggregations() }
                ?: type.defaultAggregation(),
        )
    }
    var pace by rememberSaveable(editorKey) { mutableStateOf(goal?.paceType ?: initialDraft?.paceType ?: GoalPaceType.Linear) }
    var deadline by rememberSaveable(editorKey) { mutableStateOf(goal?.deadline ?: initialDraft?.deadline) }
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
    var advanced by rememberSaveable(editorKey) {
        mutableStateOf(
            defaults.powerMode || goal?.let {
                it.description.isNotBlank() || it.tags.isNotEmpty() ||
                    it.reminderMinutes != null ||
                    it.aggregationPeriod != GoalAggregationPeriod.All || it.rollingDays != null
            } == true,
        )
    }
    var advancedMeasurement by rememberSaveable(editorKey) { mutableStateOf(false) }
    var reminder by rememberSaveable(editorKey) { mutableStateOf((goal?.reminderMinutes ?: initialDraft?.reminderMinutes)?.let { "%02d:%02d".format(it / 60, it % 60) }.orEmpty()) }
    var aggregationPeriod by rememberSaveable(editorKey) { mutableStateOf(goal?.aggregationPeriod ?: initialDraft?.aggregationPeriod ?: GoalAggregationPeriod.All) }
    var rollingDays by rememberSaveable(editorKey) { mutableStateOf((goal?.rollingDays ?: initialDraft?.rollingDays ?: 7).toString()) }
    var consistencyPeriod by rememberSaveable(editorKey) { mutableStateOf(goal?.consistencyPeriod ?: initialDraft?.consistencyPeriod ?: GoalConsistencyPeriod.Week) }
    var consistencyRequiredPeriods by rememberSaveable(editorKey) { mutableStateOf((goal?.consistencyRequiredPeriods ?: initialDraft?.consistencyRequiredPeriods ?: 12).toString()) }
    val editorZone = defaults.zoneId()
    val initialElapsedMoment = remember(editorKey) {
        Instant.ofEpochMilli(goal?.elapsedStartMillis ?: initialDraft?.elapsedStartMillis ?: System.currentTimeMillis()).atZone(editorZone)
    }
    var elapsedDate by rememberSaveable(editorKey) { mutableStateOf(initialElapsedMoment.toLocalDate()) }
    var elapsedMinutes by rememberSaveable(editorKey) { mutableIntStateOf(initialElapsedMoment.hour * 60 + initialElapsedMoment.minute) }
    var elapsedDisplayUnit by rememberSaveable(editorKey) { mutableStateOf(goal?.elapsedDisplayUnit ?: initialDraft?.elapsedDisplayUnit ?: ElapsedDisplayUnit.Auto) }
    var showElapsedDatePicker by rememberSaveable(editorKey) { mutableStateOf(false) }
    var validationRequested by rememberSaveable(editorKey) { mutableStateOf(false) }
    val elapsedStartInstant = elapsedDate.atTime(LocalTime.of(elapsedMinutes / 60, elapsedMinutes % 60)).atZone(editorZone).toInstant()
    val compatibleAggregations = type.compatibleAggregations()
    val direction = type.defaultDirection()
    val editorFingerprint = listOf(
        name, description, areaId, area, tags, icon, type, unitId, dimension, precision,
        baseline, targetMin, targetMax, aggregation, pace, deadline,
        reminder, aggregationPeriod, rollingDays, consistencyPeriod, consistencyRequiredPeriods,
        elapsedDate, elapsedMinutes, elapsedDisplayUnit,
        milestoneDrafts.map { "${it.id}:${it.uuid}:${it.name}:${it.weight}:${it.reward}" },
    ).joinToString("\u001f")
    val initialFingerprint by rememberSaveable(editorKey) { mutableStateOf(editorFingerprint) }
    var showDiscardConfirmation by rememberSaveable(editorKey) { mutableStateOf(false) }
    val requestDismiss = { if (editorFingerprint != initialFingerprint) showDiscardConfirmation = true else onDismiss() }
    val normalizedMilestones = milestoneDrafts.filter { it.name.isNotBlank() }
        .map { it.copy(name = it.name.trim(), reward = it.reward.trim()) }
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
        startDate = goal?.startDate ?: today,
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
        elapsedStartMillis = elapsedStartInstant.toEpochMilli().takeIf { type == GoalType.ElapsedSince },
        elapsedDisplayUnit = elapsedDisplayUnit,
    )
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
    }
    val draftValidationMessages = currentDraft.validationErrors(System.currentTimeMillis())
    val validationMessages = (rawFieldProblems + draftValidationMessages).distinct()
    val validationRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(validationRequested, validationMessages) {
        if (validationRequested && validationMessages.isNotEmpty()) validationRequester.bringIntoView()
    }
    BackHandler(enabled = !showDiscardConfirmation, onBack = requestDismiss)
    ProductivityEditorDialog(
        modifier = modifier,
        testTag = "goal-editor-surface",
        primary = true,
        paneTitle = if (goal == null) "Create Goal" else "Edit Goal",
        onDismissRequest = requestDismiss,
        title = { Text(if (goal == null) "Create Goal" else "Edit Goal") },
        text = {
            WhipReorderLazyColumn(
                modifier = Modifier.testTag("goal-editor-fields"),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                item {
                    Text("* Required field", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (validationRequested && validationMessages.isNotEmpty()) item {
                    FormValidationSummary(
                        messages = validationMessages,
                        visible = true,
                        modifier = Modifier.bringIntoViewRequester(validationRequester),
                        testTag = "goal-save-problem",
                    )
                }
                item { EditorSectionHeader("Basics", "Name this Goal and choose the emoji used across Whip.") }
                item {
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
                }
                item {
                    WhipEmojiPicker(
                        value = icon,
                        defaultEmoji = DEFAULT_GOAL_EMOJI,
                        onValueChange = { icon = it },
                        modifier = Modifier.fillMaxWidth(),
                        customEmojis = customIdentityEmojis,
                        onSaveEmoji = onSaveIdentityEmoji,
                        onRemoveSavedEmoji = onRemoveSavedIdentityEmoji,
                    )
                }
                item {
                    EditorSectionHeader("Target", "Choose the Goal behavior first; its required target and measurement fields stay directly below it.")
                }
                item {
                    GoalEnumDropdown("Goal Type", GoalType.entries, type, GoalType::displayLabel) { selected ->
                        type = selected
                        aggregation = selected.defaultAggregation()
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
                }
                if (type !in setOf(GoalType.WeightedMilestones, GoalType.ElapsedSince)) {
                    item {
                        if (type == GoalType.MaintainRange) {
                            ResponsiveFieldPair(
                                first = { field ->
                                    GoalNumberField(
                                        targetMin,
                                        { targetMin = it },
                                        "Range Minimum",
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
                                        "Range Maximum",
                                        field,
                                        required = true,
                                        error = if (validationRequested && targetMax.toWhipDoubleOrNull() == null) "Enter a range maximum" else null,
                                    )
                                },
                            )
                        } else GoalNumberField(
                            targetMin,
                            { targetMin = it },
                            if (type == GoalType.Consistency) "Successes per Period" else "Target",
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
                                "Starting Value (Optional)",
                                error = "Starting value must be a number".takeIf {
                                    validationRequested && baseline.isNotBlank() && baseline.toWhipDoubleOrNull() == null
                                },
                            )
                        }
                    } else {
                        item {
                            ResponsiveFieldPair(
                                first = { field -> Column(field) { GoalEnumDropdown("Period", GoalConsistencyPeriod.entries, consistencyPeriod, { it.name }) { consistencyPeriod = it } } },
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
                        Text("Choose the exact event time. Resetting later replaces this origin without creating a measurement.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item {
                        ResponsiveFieldPair(
                            first = { field ->
                                WhipOutlinedButton(onClick = { showElapsedDatePicker = true }, modifier = field.fillMaxWidth()) {
                                    Text(elapsedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
                                }
                            },
                            second = { field -> ClockPickerButton("Start Time", elapsedMinutes, { if (it != null) elapsedMinutes = it }, field) },
                        )
                    }
                    item { GoalEnumDropdown("Counter Display", ElapsedDisplayUnit.entries, elapsedDisplayUnit, ElapsedDisplayUnit::displayLabel) { elapsedDisplayUnit = it } }
                    if (elapsedStartInstant.isAfter(Instant.now())) item {
                        Text("Start time cannot be in the future.", color = MaterialTheme.colorScheme.error)
                    }
                }
                if (type !in setOf(GoalType.WeightedMilestones, GoalType.Consistency, GoalType.ElapsedSince)) {
                    item {
                        Text("Unit", fontWeight = FontWeight.Bold)
                        Text(
                            "This unit is used by starting values, targets, entries, and progress.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    item {
                        UnitSelectionField(
                            units = BuiltInUnits.all + customUnits,
                            selectedUnitId = unitId,
                            dimension = dimension,
                            onSelect = { unitId = it },
                            onCreateUnit = onCreateCustomUnit,
                            dialogModifier = modifier,
                            supportingText = "Create or choose the unit used for starting values, targets, entries, and progress.",
                            allowAnyDimension = true,
                            onDimensionSelect = { dimension = it },
                        )
                    }
                    item {
                        DisclosureButton(
                            label = "Advanced Measurement Options",
                            expanded = advancedMeasurement,
                            onClick = { advancedMeasurement = !advancedMeasurement },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (advancedMeasurement) {
                        item {
                            GoalEnumDropdown("Measurement Type", UnitDimension.entries, dimension, UnitDimension::uiLabel) { selected ->
                                dimension = selected
                                val units = BuiltInUnits.all + customUnits.filter { !it.archived || it.id == unitId }
                                val preferred = defaults.preferredUnitId(selected)
                                if (preferred != null && units.any { it.id == preferred && it.dimension == selected }) unitId = preferred
                                if (units.none { it.id == unitId && it.dimension == selected }) unitId = units.firstOrNull { it.dimension == selected }?.id ?: unitId
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
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Milestones *", fontWeight = FontWeight.Bold)
                            Text(
                                "Add at least one named milestone with a positive weight.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (validationRequested && draftValidationMessages.any { it.contains("milestone", ignoreCase = true) }) {
                                    MaterialTheme.colorScheme.error
                                } else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            WhipReorderLayout(itemSpacing = 8.dp) {
                            milestoneDrafts.forEachIndexed { index, draft ->
                                key(draft.uuid ?: "goal-milestone-${draft.id ?: index}") {
                                val reorderInteraction = rememberWhipReorderInteractionState()
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
                                            Text(
                                                "Milestone ${index + 1}",
                                                modifier = Modifier.weight(1f),
                                                style = MaterialTheme.typography.labelLarge,
                                            )
                                            IconButton(
                                                onClick = { milestoneDrafts = ArrayList(milestoneDrafts).also { it.removeAt(index) } },
                                            ) { Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove ${draft.name.ifBlank { "milestone ${index + 1}" }}") }
                                        }
                                        OutlinedTextField(
                                            value = draft.name,
                                            onValueChange = { name ->
                                                milestoneDrafts = ArrayList(milestoneDrafts).also {
                                                    it[index] = draft.copy(name = name)
                                                }
                                            },
                                            label = { Text("Milestone ${index + 1}") },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        ResponsiveFieldPair(
                                            first = { field ->
                                                GoalNumberField(
                                                    value = editableNumericValue(draft.weight),
                                                    onValueChange = { value ->
                                                        milestoneDrafts = ArrayList(milestoneDrafts).also {
                                                            it[index] = draft.copy(weight = value.toWhipDoubleOrNull() ?: 0.0)
                                                        }
                                                    },
                                                    label = "Weight",
                                                    modifier = field,
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
                item { EditorSectionHeader("Schedule", "Add a deadline, pace guidance, or reminder when this Goal needs a time boundary.") }
                if (type != GoalType.ElapsedSince) item { WhipOutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) { Text(deadline?.let { "Deadline ${it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}" } ?: "Add Deadline") } }
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
                        label = "Daily Measurement Reminder",
                        minutes = parseGoalClock(reminder),
                        onChange = { minutes ->
                            if (minutes != null && parseGoalClock(reminder) == null) onRequestNotificationPermission()
                            reminder = minutes?.let(::formatClockMinutes).orEmpty()
                        },
                    )
                }
                item { EditorSectionHeader("Organization", "Choose the Area that owns this Goal.") }
                item {
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
                }
                item {
                    DisclosureButton(
                        label = "More Details",
                        expanded = advanced,
                        onClick = { advanced = !advanced },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (advanced) {
                    item { EditorSectionHeader("Details", "Fine-tune how measurements combine, then add optional context.") }
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
                    item { OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(tags, { tags = it }, label = { Text("Tags, comma-separated") }, modifier = Modifier.fillMaxWidth()) }
                }
            }
        },
        confirmButton = {
            WhipButton(enabled = !saving, onClick = {
                validationRequested = true
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
    )
    if (showElapsedDatePicker) WhipDatePickerDialog(elapsedDate, { showElapsedDatePicker = false }, { elapsedDate = it; showElapsedDatePicker = false })
    if (showDatePicker) WhipDatePickerDialog(deadline ?: today, { showDatePicker = false }, { deadline = it; showDatePicker = false })
    if (showDiscardConfirmation) {
        UnsavedChangesDialog("goal", { showDiscardConfirmation = false }, onDismiss)
    }
}

@Composable
private fun GoalMeasurementDialog(
    projection: GoalProjection,
    today: LocalDate,
    entry: MetricEntry?,
    onDismiss: () -> Unit,
    onRecord: (Double, LocalDate, String) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val editorKey = "goal-measurement-${entry?.id ?: projection.goal.id}"
    var value by rememberSaveable(editorKey) { mutableStateOf(entry?.enteredValue?.let(::editableNumericValue).orEmpty()) }
    var note by rememberSaveable(editorKey) { mutableStateOf(entry?.note.orEmpty()) }
    var date by rememberSaveable(editorKey) { mutableStateOf(entry?.localDate ?: today) }
    var showDatePicker by rememberSaveable(editorKey) { mutableStateOf(false) }
    var confirmDelete by rememberSaveable(editorKey) { mutableStateOf(false) }
    var validationRequested by rememberSaveable(editorKey) { mutableStateOf(false) }
    val parsedValue = value.toWhipDoubleOrNull()
    PaneAwareAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry == null) "Record ${projection.goal.name}" else "Edit Measurement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(projection.goal.measurementEntryInstruction())
                val unitLabel = (entry?.enteredUnitId ?: projection.goal.unitId).goalUnitLabel()
                val fieldLabel = projection.goal.measurementEntryLabel().let { label ->
                    if (unitLabel.isBlank()) label else "$label ($unitLabel)"
                }
                GoalNumberField(
                    value,
                    { value = it },
                    fieldLabel,
                    required = true,
                    error = "Enter a value".takeIf { validationRequested && parsedValue == null },
                )
                OutlinedTextField(note, { note = it }, label = { Text("Optional note") }, modifier = Modifier.fillMaxWidth())
                WhipOutlinedButton(onClick = { showDatePicker = true }) { Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))) }
            }
        },
        confirmButton = {
            WhipTextButton(onClick = {
                validationRequested = true
                parsedValue?.let { onRecord(it, date, note) }
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) WhipTextButton(onClick = { confirmDelete = true }) { Text("Delete") }
                WhipTextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
    if (showDatePicker) WhipDatePickerDialog(date, { showDatePicker = false }, { date = it; showDatePicker = false })
    if (confirmDelete && onDelete != null) {
        PaneAwareAlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete Measurement?") },
            text = { Text("This removes this measurement from the Goal's history and recalculates its progress.") },
            confirmButton = {
                WhipTextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { WhipTextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun GoalActionsDialog(
    projection: GoalProjection,
    modifier: Modifier = Modifier,
    rules: List<LinkRule>,
    contributions: List<Contribution>,
    sourceGoals: List<Goal>,
    sourceHabits: List<Habit>,
    sourceMetrics: List<MetricDefinition>,
    onDismiss: () -> Unit,
    onAddLink: () -> Unit,
    onEditLink: (LinkRule) -> Unit,
    onSetLinkEnabled: (Long, Boolean) -> Unit,
    onDeleteLink: (Long) -> Unit,
    onSetContributionExcluded: (Long, Boolean) -> Unit,
    onOverrideContribution: (Contribution) -> Unit,
    onEditMeasurement: (MetricEntry) -> Unit,
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
) {
    var visibleMeasurements by rememberSaveable(projection.goal.id) { mutableIntStateOf(25) }
    var showAccessibleTable by rememberSaveable(projection.goal.id) { mutableStateOf(false) }
    var visibleContributions by rememberSaveable(projection.goal.id) { mutableIntStateOf(10) }
    var removingAutomationId by rememberSaveable(projection.goal.id) { mutableStateOf<Long?>(null) }
    var section by rememberSaveable(projection.goal.id) { mutableStateOf(GoalDetailSection.Overview) }
    val insights = remember(projection) { buildGoalInsights(projection.goal, projection.entries, projection.milestones) }
    val primaryAction = when (projection.goal.status) {
        GoalStatus.Active -> when (projection.goal.type) {
            GoalType.WeightedMilestones -> null
            GoalType.ElapsedSince -> EntityInspectorPrimaryAction("reset-timer", "Reset Timer", onResetElapsed)
            GoalType.OpenEndedTrend -> EntityInspectorPrimaryAction("add-measurement", "Add Measurement", onRecordProgress)
            else -> EntityInspectorPrimaryAction("log-progress", "Log Progress", onRecordProgress)
        }
        GoalStatus.Paused -> EntityInspectorPrimaryAction("resume", "Resume Goal", onPause)
        GoalStatus.Archived -> EntityInspectorPrimaryAction("restore", "Restore Goal", onArchive)
        GoalStatus.Completed, GoalStatus.Abandoned -> EntityInspectorPrimaryAction("reopen", "Reopen Goal", onReopen)
    }
    EntityInspector(
        entityType = "Goal",
        title = projection.goal.name,
        emoji = projection.goal.icon,
        context = projection.goal.area.ifBlank { projection.goal.type.displayLabel() },
        status = projection.goal.status.inspectorLabel(),
        sections = GoalDetailSection.entries.map { it.inspectorSection },
        selectedSectionId = section.id,
        onSelectSection = { id -> section = GoalDetailSection.entries.first { it.id == id } },
        onDismiss = onDismiss,
        onEdit = onEdit,
        editLabel = "Edit Goal",
        modifier = modifier,
        legacySurfaceTag = "goal-detail-surface",
        legacySectionTagPrefix = "goal-detail-section",
        primaryAction = primaryAction,
        content = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (section == GoalDetailSection.Overview) {
                item {
                    EntityInspectorGroup("Outcome") {
                        Text(
                            projection.inspectorOutcome(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        projection.goal.description.takeIf(String::isNotBlank)?.let {
                            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        projection.goal.deadline?.let {
                            EntityInspectorFact("Target date", it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
                        }
                    }
                }
                item {
                    Text(if (projection.goal.type == GoalType.ElapsedSince) "Elapsed Time" else "Progress insight", fontWeight = FontWeight.Bold)
                    val chartValues = insights.points.mapNotNull { it.progress ?: it.canonicalValue }
                    if (projection.goal.type == GoalType.ElapsedSince) {
                        projection.goal.elapsedStartMillis?.let { started ->
                            Text(elapsedCounter(started, System.currentTimeMillis(), projection.goal.elapsedDisplayUnit).label(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                            Text("Started ${Instant.ofEpochMilli(started).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))}")
                        }
                    } else if (chartValues.size >= 2) {
                        GoalLineChart(
                            values = chartValues,
                            description = "${projection.goal.name} progress chart with ${chartValues.size} points from " +
                                "${insights.points.first().date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))} to " +
                                insights.points.last().date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                        )
                    } else Text("More observations are needed for a trend line.")
                    if (projection.goal.type != GoalType.ElapsedSince) Text(
                        listOfNotNull(
                            insights.ratePerDay?.let { "Rate ${formatGoalValue(it, projection.goal.precision)} per day" },
                            insights.forecastDate?.let {
                                "Forecast ${it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))} (${insights.confidence} confidence)"
                            },
                        ).joinToString(" · ").ifBlank { "Rate and forecast unavailable" },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (projection.goal.type != GoalType.ElapsedSince && (insights.targetMin != null || insights.targetMax != null)) {
                        Text("Target overlay: ${formatGoalValue(insights.targetMin, projection.goal.precision)} to ${formatGoalValue(insights.targetMax ?: insights.targetMin, projection.goal.precision)}")
                    }
                    if (projection.goal.type != GoalType.ElapsedSince) Text(insights.dataQualityExplanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (projection.goal.type != GoalType.ElapsedSince) DisclosureButton(
                        label = "Trend data table",
                        expanded = showAccessibleTable,
                        onClick = { showAccessibleTable = !showAccessibleTable },
                    )
                }
                if (showAccessibleTable) {
                    items(insights.points.takeLast(visibleMeasurements), key = { "insight-${it.date}" }) { point ->
                        Text(
                            "${point.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}: value ${formatGoalValue(point.canonicalValue, projection.goal.precision)}, " +
                                "progress ${point.progress?.let { "${(it * 100).toInt()}%" } ?: "not applicable"}, ${point.recordedEntries} source entries",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                }
                if (section == GoalDetailSection.History) {
                if (projection.goal.type == GoalType.ElapsedSince) item { Text("Elapsed-time Goals use one editable start instant rather than measurement history.") }
                item { Text("${projection.entries.size} measurements", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                items(projection.entries.take(visibleMeasurements), key = { it.id }) { entry ->
                    EntityInspectorAction(
                        id = "measurement-${entry.id}",
                        label = entry.historyLabel(),
                        onClick = { onEditMeasurement(entry) },
                    )
                }
                if (visibleMeasurements < projection.entries.size) item {
                    WhipOutlinedButton(
                        onClick = { visibleMeasurements = (visibleMeasurements + 25).coerceAtMost(projection.entries.size) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Show 25 More · ${projection.entries.size - visibleMeasurements} Remaining") }
                }
                }
                if (section == GoalDetailSection.Connections) {
                item { Text("Goal Automations", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp)) }
                if (projection.goal.type == GoalType.ElapsedSince) item { Text("This Goal advances from time itself, so Goal Automations do not apply.") }
                else if (rules.isEmpty()) item { Text("No automatic contributions yet.") }
                items(rules, key = { "rule-${it.id}" }) { rule ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = rule.enabled, onCheckedChange = { onSetLinkEnabled(rule.id, it) })
                            Column(Modifier.weight(1f)) {
                                Text(rule.name, fontWeight = FontWeight.SemiBold)
                                Text(rule.progressSourceSummary(sourceGoals, sourceHabits, sourceMetrics), style = MaterialTheme.typography.labelSmall)
                            }
                            WhipTextButton(onClick = { onEditLink(rule) }) { Text("Edit") }
                            WhipTextButton(onClick = { removingAutomationId = rule.id }) {
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        val ruleContributions = contributions.filter { it.linkRuleId == rule.id }
                        ruleContributions.take(visibleContributions).forEach { contribution ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = !contribution.excluded, onCheckedChange = { onSetContributionExcluded(contribution.id, !it) })
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "${contribution.localDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}: ${contribution.explanation}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    contribution.overrideValue?.let {
                                        Text(
                                            "Adjusted value: ${formatGoalValue(it, projection.goal.precision)} ${projection.goal.unitId.goalUnitLabel()}".trim(),
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                                WhipTextButton(onClick = { onOverrideContribution(contribution) }) { Text("Override") }
                            }
                        }
                        if (visibleContributions < ruleContributions.size) {
                            WhipTextButton(
                                onClick = { visibleContributions = (visibleContributions + 25).coerceAtMost(ruleContributions.size) },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Show 25 More · ${ruleContributions.size - visibleContributions} Remaining") }
                        }
                    }
                }
                if (projection.goal.type != GoalType.ElapsedSince) item { WhipOutlinedButton(onClick = onAddLink, modifier = Modifier.fillMaxWidth()) { Text("Add Goal Automation") } }
                }
                if (section == GoalDetailSection.More) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        EntityInspectorGroup("Actions") {
                            EntityInspectorAction("duplicate", "Duplicate Goal", onDuplicate)
                        }
                        EntityInspectorGroup("Availability") {
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
                            if (projection.goal.status == GoalStatus.Active) {
                                EntityInspectorAction("pause", "Pause Goal", onPause)
                                EntityInspectorAction("complete", "Complete Goal", onComplete)
                                EntityInspectorAction("abandon", "Abandon Goal", onAbandon)
                            }
                            if (projection.goal.status != GoalStatus.Archived) {
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
    removingAutomationId?.let { automationId ->
        val automation = rules.firstOrNull { it.id == automationId }
        RemoveAutomationConfirmationDialog(
            automationName = automation?.name ?: "This Automation",
            onDismiss = { removingAutomationId = null },
            onConfirm = {
                onDeleteLink(automationId)
                removingAutomationId = null
            },
        )
    }
}

private enum class GoalDetailSection(val id: String, val label: String) {
    Overview("overview", "Overview"),
    History("history", "History"),
    Connections("automation", "Automations"),
    More("options", "Options"),
    ;

    val inspectorSection: EntityInspectorSection
        get() = EntityInspectorSection(id, label)
}

private fun GoalStatus.inspectorLabel(): String = when (this) {
    GoalStatus.Active -> "Active"
    GoalStatus.Paused -> "Paused"
    GoalStatus.Completed -> "Completed"
    GoalStatus.Abandoned -> "Abandoned"
    GoalStatus.Archived -> "Archived"
}

private fun GoalProjection.inspectorOutcome(): String = when {
    goal.type == GoalType.ElapsedSince && goal.elapsedStartMillis != null ->
        elapsedCounter(goal.elapsedStartMillis, System.currentTimeMillis(), goal.elapsedDisplayUnit).label()
    goal.type == GoalType.WeightedMilestones ->
        "${milestones.count { it.completed }} of ${milestones.size} milestones complete"
    progress != null -> "${(progress * 100).toInt()}% complete"
    consistency != null -> with(requireNotNull(consistency)) {
        "$successfulPeriods of $requiredPeriods ${period.name.lowercase()} periods complete"
    }
    currentValue != null -> {
        val unit = BuiltInUnits.get(goal.unitId)?.symbol.orEmpty()
        "Current ${formatGoalValue(currentValue, goal.precision)} $unit".trim()
    }
    else -> "Ready to begin"
}

private fun MetricEntry.historyLabel(): String {
    val date = localDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    val valueLabel = enteredValue?.let(::editableNumericValue) ?: when (status.name) {
        "Missing" -> "No value recorded"
        "Failed" -> "Below target"
        "Skipped" -> "Skipped"
        "Excused" -> "Excused"
        else -> "Recorded"
    }
    val unit = enteredUnitId?.let(BuiltInUnits::get)?.symbol.orEmpty()
    val source = sourceType.name
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replaceFirstChar(Char::uppercase)
    return buildString {
        append("$date · $valueLabel")
        if (unit.isNotBlank()) append(" $unit")
        append(" · $source")
        if (note.isNotBlank()) append(" · $note")
    }
}

@Composable
private fun GoalLineChart(values: List<Double>, description: String) {
    val finite = values.filter(Double::isFinite)
    if (finite.size < 2) return
    val min = finite.min()
    val max = finite.max()
    val span = (max - min).takeIf { it > 0.0 } ?: 1.0
    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .semantics { contentDescription = description },
    ) {
        val step = size.width / (finite.size - 1).coerceAtLeast(1)
        finite.zipWithNext().forEachIndexed { index, (start, end) ->
            drawLine(
                color = lineColor,
                start = androidx.compose.ui.geometry.Offset(index * step, size.height - ((start - min) / span * size.height).toFloat()),
                end = androidx.compose.ui.geometry.Offset((index + 1) * step, size.height - ((end - min) / span * size.height).toFloat()),
                strokeWidth = 4.dp.toPx(),
            )
        }
    }
}

@Composable
private fun ContributionOverrideDialog(
    contribution: Contribution,
    onDismiss: () -> Unit,
    onSave: (Double?) -> Unit,
) {
    var value by rememberSaveable(contribution.id) {
        mutableStateOf((contribution.overrideValue ?: contribution.canonicalValue)?.let(::editableNumericValue).orEmpty())
    }
    var validationRequested by rememberSaveable(contribution.id) { mutableStateOf(false) }
    val parsedValue = value.toWhipDoubleOrNull()
    PaneAwareAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Override Automated Contribution") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(contribution.explanation)
                GoalNumberField(
                    value,
                    { value = it },
                    "Canonical Value",
                    required = true,
                    error = "Enter a value".takeIf { validationRequested && parsedValue == null },
                )
                Text("This changes only this Automation result. The source event remains intact and explainable.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            WhipTextButton(onClick = {
                validationRequested = true
                parsedValue?.let(onSave)
            }) { Text("Save Override") }
        },
        dismissButton = {
            Row {
                if (contribution.overrideValue != null) WhipTextButton(onClick = { onSave(null) }) { Text("Clear Override") }
                WhipTextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun GoalLinkEditorDialog(
    projection: GoalProjection,
    state: GoalUiState,
    initialRule: LinkRule? = null,
    onDismiss: () -> Unit,
    onPreview: (LinkRuleDraft) -> Unit,
    onClearPreview: () -> Unit,
    onSave: (LinkRuleDraft, Boolean) -> Unit,
    saving: Boolean = false,
) {
    val goal = projection.goal
    val editorKey = "goal-link-${goal.id}-${initialRule?.id ?: "new"}"
    var name by rememberSaveable(editorKey) { mutableStateOf(initialRule?.name ?: "Progress for ${goal.name}") }
    var kind by rememberSaveable(editorKey) { mutableStateOf(initialRule?.kind ?: LinkKind.Contribution) }
    val initialSourceKind = initialRule?.let {
        inferGoalProgressSourceKind(it, (state.active + state.completed).map { source -> source.goal }, state.sourceHabits)
    } ?: when {
        state.sourceHabits.isNotEmpty() -> GoalProgressSourceKind.Habit
        state.sourceTasks.isNotEmpty() -> GoalProgressSourceKind.Task
        state.sourceTracks.isNotEmpty() -> GoalProgressSourceKind.Track
        state.sourceExercises.isNotEmpty() -> GoalProgressSourceKind.Exercise
        (state.active + state.completed).any { it.goal.id != goal.id } -> GoalProgressSourceKind.Goal
        state.sourceMetrics.healthDataSources().isNotEmpty() -> GoalProgressSourceKind.HealthData
        else -> GoalProgressSourceKind.Workout
    }
    var sourceKind by rememberSaveable(editorKey) {
        mutableStateOf(initialSourceKind)
    }
    val sourceType = sourceKind.sourceType
    var habitId by rememberSaveable(editorKey) {
        mutableStateOf(
            initialRule?.sourceEntityId?.takeIf { initialRule.sourceType == LinkSourceType.Habit }
                ?: state.sourceHabits.firstOrNull { it.metricId == initialRule?.sourceMetricId }?.id
                ?: state.sourceHabits.firstOrNull()?.id,
        )
    }
    var taskId by rememberSaveable(editorKey) { mutableStateOf(initialRule?.sourceEntityId?.takeIf { initialRule.sourceType in setOf(LinkSourceType.Task, LinkSourceType.Subtask) } ?: state.sourceTasks.firstOrNull()?.id) }
    var exerciseId by rememberSaveable(editorKey) { mutableStateOf(initialRule?.sourceEntityId?.takeIf { initialRule.sourceType == LinkSourceType.Exercise } ?: state.sourceExercises.firstOrNull()?.id) }
    var trackId by rememberSaveable(editorKey) { mutableStateOf(initialRule?.sourceEntityId?.takeIf { initialRule.sourceType == LinkSourceType.Track } ?: state.sourceTracks.firstOrNull()?.track?.id) }
    var sourceGoalMetricId by rememberSaveable(editorKey) { mutableStateOf(initialRule?.sourceMetricId ?: (state.active + state.completed).firstOrNull { it.goal.id != goal.id }?.goal?.metricId) }
    var sourceMetric by rememberSaveable(editorKey) {
        mutableStateOf(
            initialRule?.sourceMetric ?: when (initialSourceKind) {
                GoalProgressSourceKind.Task, GoalProgressSourceKind.Subtask -> LinkSourceMetric.Completion
                GoalProgressSourceKind.Track -> LinkSourceMetric.EntryCount
                GoalProgressSourceKind.Workout -> LinkSourceMetric.Count
                else -> LinkSourceMetric.NumericValue
            },
        )
    }
    var sourceStepId by rememberSaveable(editorKey) { mutableStateOf(initialRule?.sourceItemId) }
    var valueMode by rememberSaveable(editorKey) { mutableStateOf(initialRule?.valueMode ?: LinkValueMode.SourceValue) }
    var fixedValue by rememberSaveable(editorKey) { mutableStateOf(initialRule?.fixedValue?.let(::editableNumericValue) ?: "1") }
    var multiplier by rememberSaveable(editorKey) { mutableStateOf(initialRule?.multiplier?.let(::editableNumericValue) ?: "1") }
    var offset by rememberSaveable(editorKey) { mutableStateOf(initialRule?.offset?.let(::editableNumericValue) ?: "0") }
    var includeHistory by rememberSaveable(editorKey) { mutableStateOf(initialRule?.retroactiveFrom != null) }
    var targetMilestoneId by rememberSaveable(editorKey) { mutableStateOf(initialRule?.targetMilestoneId ?: projection.milestones.firstOrNull()?.id) }
    var trackAggregation by rememberSaveable(editorKey) { mutableStateOf(initialRule?.trackAggregation?.normalizedAutomationMeasure() ?: TrackAggregation.CountEntries) }
    var sourceFieldId by rememberSaveable(editorKey) { mutableStateOf(initialRule?.sourceFieldId) }
    var conditionMode by rememberSaveable(editorKey) { mutableStateOf(initialRule?.conditionMode ?: TrackConditionMode.MatchAll) }
    var conditions by remember(editorKey) { mutableStateOf(initialRule?.conditions.orEmpty()) }
    var addingCondition by rememberSaveable(editorKey) { mutableStateOf(false) }
    var validationRequested by rememberSaveable(editorKey) { mutableStateOf(false) }
    var showAdvanced by rememberSaveable(editorKey) {
        mutableStateOf(
            initialRule?.let { rule ->
                rule.kind != LinkKind.Contribution ||
                    rule.valueMode != LinkValueMode.SourceValue ||
                    rule.multiplier != 1.0 || rule.offset != 0.0 ||
                    rule.name != "Progress for ${goal.name}"
            } ?: false,
        )
    }
    var historyChoice by rememberSaveable(editorKey) {
        mutableStateOf(
            when (initialRule?.retroactiveFrom) {
                null -> LinkHistoryChoice.NewEntriesOnly
                goal.startDate -> LinkHistoryChoice.SinceGoalStart
                else -> LinkHistoryChoice.SinceDate
            },
        )
    }
    var historyDate by rememberSaveable(editorKey) { mutableStateOf(initialRule?.retroactiveFrom ?: goal.startDate) }
    var pickingHistoryDate by rememberSaveable(editorKey) { mutableStateOf(false) }
    val selectedTrack = state.sourceTracks.firstOrNull { it.track.id == trackId }
    val numericTrackFields = selectedTrack?.fields.orEmpty().filter { field ->
        field.type == TrackFieldType.Scale && goal.dimension == UnitDimension.Unitless ||
            field.type == TrackFieldType.Number && field.dimension == goal.dimension
    }
    val availableTrackAggregations = goal.compatibleTrackAutomationMeasures()
    val selectedTrackField = numericTrackFields.firstOrNull { it.id == sourceFieldId } ?: numericTrackFields.firstOrNull()
    val needsTrackField = trackAggregation.needsTrackNumberField()
    val requiredTrackGoalAggregation = goal.requiredAggregationForTrack(trackAggregation)
    LaunchedEffect(sourceType, goal.aggregation, goal.dimension) {
        if (sourceType == LinkSourceType.Track && trackAggregation !in availableTrackAggregations && availableTrackAggregations.isNotEmpty()) {
            trackAggregation = availableTrackAggregations.first()
            sourceMetric = trackAggregation.progressSourceMetric()
        }
    }
    LaunchedEffect(
        sourceType,
        trackId,
        sourceFieldId,
        trackAggregation,
        fixedValue,
        multiplier,
        offset,
        historyChoice,
        historyDate,
        conditionMode,
        conditions,
    ) {
        onClearPreview()
    }

    fun availableMetrics(type: LinkSourceType): List<LinkSourceMetric> = when (type) {
        LinkSourceType.Habit -> listOf(LinkSourceMetric.NumericValue, LinkSourceMetric.Success)
        LinkSourceType.Task, LinkSourceType.Subtask -> listOf(LinkSourceMetric.Completion)
        LinkSourceType.Workout -> listOf(LinkSourceMetric.Count, LinkSourceMetric.Duration, LinkSourceMetric.Volume)
        LinkSourceType.Exercise -> listOf(LinkSourceMetric.EstimatedOneRepMax, LinkSourceMetric.MaxWeight, LinkSourceMetric.Distance, LinkSourceMetric.Repetitions, LinkSourceMetric.Duration, LinkSourceMetric.Volume)
        LinkSourceType.Metric -> listOf(LinkSourceMetric.NumericValue)
        LinkSourceType.Track -> listOf(LinkSourceMetric.EntryCount, LinkSourceMetric.FieldValue)
    }
    fun draft(): LinkRuleDraft = LinkRuleDraft(
        name = name,
        kind = kind,
        sourceType = sourceType,
        sourceEntityId = when (sourceType) {
            LinkSourceType.Habit -> habitId
            LinkSourceType.Task, LinkSourceType.Subtask -> taskId
            LinkSourceType.Exercise -> exerciseId
            LinkSourceType.Track -> trackId
            LinkSourceType.Workout, LinkSourceType.Metric -> null
        },
        sourceMetricId = sourceGoalMetricId.takeIf { sourceType == LinkSourceType.Metric },
        sourceItemId = sourceStepId.takeIf { sourceType == LinkSourceType.Subtask },
        sourceMetric = sourceMetric,
        targetGoalId = goal.id,
        targetMilestoneId = targetMilestoneId.takeIf { goal.type == GoalType.WeightedMilestones },
        valueMode = if (sourceType == LinkSourceType.Track && trackAggregation == TrackAggregation.FixedAmount) LinkValueMode.FixedValue else valueMode,
        fixedValue = fixedValue.toWhipDoubleOrNull(),
        multiplier = multiplier.toWhipDoubleOrNull() ?: 1.0,
        offset = offset.toWhipDoubleOrNull() ?: 0.0,
        retroactiveFrom = when {
            sourceType != LinkSourceType.Track -> goal.startDate.takeIf { includeHistory }
            historyChoice == LinkHistoryChoice.NewEntriesOnly -> null
            historyChoice == LinkHistoryChoice.SinceGoalStart -> goal.startDate
            historyChoice == LinkHistoryChoice.SinceDate -> historyDate
            else -> selectedTrack?.entries?.minOfOrNull { it.entry.entryDate } ?: goal.startDate
        },
        enabled = initialRule?.enabled ?: true,
        trackAggregation = trackAggregation.takeIf { sourceType == LinkSourceType.Track },
        sourceFieldId = selectedTrackField?.id.takeIf { sourceType == LinkSourceType.Track && needsTrackField },
        conditionMode = conditionMode,
        conditions = conditions.takeIf { sourceType == LinkSourceType.Track }.orEmpty(),
    )
    val sourceAvailable = when (sourceType) {
        LinkSourceType.Habit -> habitId != null
        LinkSourceType.Task, LinkSourceType.Subtask -> taskId != null
        LinkSourceType.Exercise -> exerciseId != null
        LinkSourceType.Metric -> if (sourceKind == GoalProgressSourceKind.Goal) {
            (state.active + state.completed).any { it.goal.id != goal.id && it.goal.metricId == sourceGoalMetricId }
        } else {
            state.sourceMetrics.healthDataSources(sourceGoalMetricId).any { it.id == sourceGoalMetricId && it.id != goal.metricId }
        }
        LinkSourceType.Workout -> true
        LinkSourceType.Track -> selectedTrack != null && (!needsTrackField || selectedTrackField != null)
    }
    val sourceAvailability = ControlAvailability(
        enabled = sourceAvailable,
        unavailableExplanation = if (sourceAvailable) {
            null
        } else {
            when (sourceType) {
                LinkSourceType.Habit -> "Create a habit first, or choose another source."
                LinkSourceType.Task, LinkSourceType.Subtask -> "Create a task first, or choose another source."
                LinkSourceType.Exercise -> "Create an exercise first, or choose another source."
                LinkSourceType.Metric -> if (sourceKind == GoalProgressSourceKind.Goal) {
                    "Create another Goal first, or choose another source."
                } else {
                    "Connect Health Connect and import data first, or choose another source."
                }
                LinkSourceType.Workout -> null
                LinkSourceType.Track -> "Create a track first, or choose another source."
            }
        },
    )
    val automationDraft = draft()
    val automationValidationMessages = buildList {
        if (name.isBlank()) add("Automation name is required")
        if (automationDraft.valueMode == LinkValueMode.FixedValue && fixedValue.toWhipDoubleOrNull() == null) {
            add("Enter a fixed value")
        }
        if (multiplier.toWhipDoubleOrNull() == null) add("Multiplier must be a number")
        if (offset.toWhipDoubleOrNull() == null) add("Offset must be a number")
    }
    PaneAwareAlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(if (initialRule == null) "Add Goal Automation" else "Edit Goal Automation") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("Choose an action that should update ${goal.name}. Whip keeps every change explainable and recalculates it when the source changes.") }
                if (validationRequested && automationValidationMessages.isNotEmpty()) item {
                    FormValidationSummary(
                        messages = automationValidationMessages,
                        visible = true,
                        testTag = "goal-automation-save-problem",
                    )
                }
                item { GoalEnumDropdown("What Adds Progress?", GoalProgressSourceKind.entries, sourceKind, GoalProgressSourceKind::label, titleCaseValues = false) { selected ->
                    sourceKind = selected
                    sourceMetric = availableMetrics(selected.sourceType).first()
                    sourceStepId = null
                    if (selected == GoalProgressSourceKind.Track) {
                        trackId = trackId ?: state.sourceTracks.firstOrNull()?.track?.id
                        sourceFieldId = state.sourceTracks.firstOrNull { it.track.id == trackId }?.fields
                            ?.firstOrNull { it.type in setOf(TrackFieldType.Number, TrackFieldType.Scale) }?.id
                    }
                    if (selected == GoalProgressSourceKind.Goal) {
                        sourceGoalMetricId = (state.active + state.completed).firstOrNull { it.goal.id != goal.id }?.goal?.metricId
                    }
                    if (selected == GoalProgressSourceKind.HealthData) {
                        sourceGoalMetricId = state.sourceMetrics.healthDataSources().firstOrNull()?.id
                    }
                } }
                when (sourceKind) {
                    GoalProgressSourceKind.Habit -> item { GoalEnumDropdown("Habit", state.sourceHabits, state.sourceHabits.firstOrNull { it.id == habitId } ?: state.sourceHabits.firstOrNull(), { it?.name ?: "No Habits Available" }, titleCaseValues = false) { habitId = it?.id } }
                    GoalProgressSourceKind.Task, GoalProgressSourceKind.Subtask -> {
                        item { GoalEnumDropdown("Task", state.sourceTasks, state.sourceTasks.firstOrNull { it.id == taskId } ?: state.sourceTasks.firstOrNull(), { it?.title ?: "No Tasks Available" }, titleCaseValues = false) { taskId = it?.id; sourceStepId = null } }
                        if (sourceKind == GoalProgressSourceKind.Subtask) {
                            val steps = state.sourceTaskSteps.filter { it.taskId == taskId }
                            item { GoalEnumDropdown("Specific subtask", listOf<Long?>(null) + steps.map { it.id }, sourceStepId, { id -> steps.firstOrNull { it.id == id }?.title ?: "Any Subtask" }, titleCaseValues = false) { sourceStepId = it } }
                        }
                    }
                    GoalProgressSourceKind.Exercise -> item { GoalEnumDropdown("Exercise", state.sourceExercises, state.sourceExercises.firstOrNull { it.id == exerciseId } ?: state.sourceExercises.firstOrNull(), { it?.name ?: "No Exercises Available" }, titleCaseValues = false) { exerciseId = it?.id } }
                    GoalProgressSourceKind.Goal -> {
                        val sourceGoals = (state.active + state.completed).filter { it.goal.id != goal.id }
                        item {
                            GoalEnumDropdown(
                                "Source Goal",
                                sourceGoals,
                                sourceGoals.firstOrNull { it.goal.metricId == sourceGoalMetricId } ?: sourceGoals.firstOrNull(),
                                { source -> source?.goal?.name ?: "No Other Goals Available" },
                                titleCaseValues = false,
                            ) { sourceGoalMetricId = it?.goal?.metricId }
                        }
                    }
                    GoalProgressSourceKind.HealthData -> {
                        val metrics = state.sourceMetrics.healthDataSources(sourceGoalMetricId).filter { it.id != goal.metricId }
                        item {
                            GoalEnumDropdown(
                                "Health Data",
                                metrics,
                                metrics.firstOrNull { it.id == sourceGoalMetricId } ?: metrics.firstOrNull(),
                                { metric -> metric?.name ?: "No Health Connect Data Available" },
                                titleCaseValues = false,
                            ) { sourceGoalMetricId = it?.id }
                        }
                        item { Text("Health Connect updates and deletions automatically recalculate Goal progress without double counting.", style = MaterialTheme.typography.bodySmall) }
                    }
                    GoalProgressSourceKind.Workout -> Unit
                    GoalProgressSourceKind.Track -> {
                        item {
                            GoalEnumDropdown(
                                "Track",
                                state.sourceTracks,
                                selectedTrack ?: state.sourceTracks.firstOrNull(),
                                { it?.track?.name ?: "No Tracks Available" },
                                titleCaseValues = false,
                            ) { chosen ->
                                trackId = chosen?.track?.id
                                sourceFieldId = chosen?.fields?.firstOrNull { it.type in setOf(TrackFieldType.Number, TrackFieldType.Scale) }?.id
                                conditions = emptyList()
                            }
                        }
                        item {
                            GoalEnumDropdown(
                                "Measure",
                                availableTrackAggregations,
                                trackAggregation,
                                TrackAggregation::automationLabel,
                                titleCaseValues = false,
                            ) { selected ->
                                trackAggregation = selected
                                sourceMetric = selected.progressSourceMetric()
                            }
                        }
                        item { Text(trackAggregation.automationExplanation(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        if (goal.type != GoalType.WeightedMilestones && requiredTrackGoalAggregation != goal.aggregation) item {
                            Card(Modifier.fillMaxWidth()) {
                                Text(
                                    "${goal.name} currently calculates progress as ${goal.aggregation.displayLabel()}. " +
                                        "This Automation will change it to ${requiredTrackGoalAggregation.displayLabel()} so the result matches your choice.",
                                    modifier = Modifier.padding(12.dp),
                                )
                            }
                        }
                        if (needsTrackField) item {
                            GoalEnumDropdown(
                                "Number or Scale Field",
                                numericTrackFields,
                                selectedTrackField,
                                { it?.name ?: "No Compatible Fields" },
                                titleCaseValues = false,
                            ) { sourceFieldId = it?.id }
                        }
                        if (trackAggregation == TrackAggregation.FixedAmount) item {
                            GoalNumberField(fixedValue, { fixedValue = it }, "Amount per matching Entry (${goal.unitId.goalUnitLabel()})")
                        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Include Entries Where", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                if (conditions.isEmpty()) Text("All Entries are eligible.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                conditions.forEachIndexed { index, condition ->
                                    val fieldName = selectedTrack?.conditionFieldName(condition) ?: "Missing Field"
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text("$fieldName ${condition.operator.uiLabel()} ${condition.summaryValue(requireNotNull(selectedTrack), BuiltInUnits.all + state.customUnits)}".trim(), Modifier.weight(1f))
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
                                WhipOutlinedButton(
                                    enabled = selectedTrack != null,
                                    onClick = { addingCondition = true },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("Add Condition") }
                            }
                        }
                        item {
                            GoalEnumDropdown(
                                "History",
                                LinkHistoryChoice.entries,
                                historyChoice,
                                LinkHistoryChoice::uiLabel,
                                titleCaseValues = false,
                            ) { historyChoice = it }
                        }
                        if (historyChoice == LinkHistoryChoice.SinceDate) item {
                            WhipOutlinedButton(onClick = { pickingHistoryDate = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Since ${historyDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}")
                            }
                        }
                        val includedTrackStart = when (historyChoice) {
                            LinkHistoryChoice.NewEntriesOnly -> null
                            LinkHistoryChoice.SinceGoalStart -> goal.startDate
                            LinkHistoryChoice.SinceDate -> historyDate
                            LinkHistoryChoice.AllHistory -> selectedTrack?.entries?.minOfOrNull { it.entry.entryDate } ?: goal.startDate
                        }
                        includedTrackStart?.takeIf { it.isBefore(goal.startDate) }?.let { alignedStart ->
                            item {
                                Card(Modifier.fillMaxWidth()) {
                                    Text(
                                        "To make every included contribution count, saving will move this Goal's start from " +
                                            "${goal.startDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))} to " +
                                            "${alignedStart.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}.",
                                        modifier = Modifier.padding(12.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                item { AvailabilityNotice("Goal Automation", sourceAvailability) }
                if (sourceType != LinkSourceType.Track && availableMetrics(sourceType).size > 1) item {
                    GoalEnumDropdown("What Should Count?", availableMetrics(sourceType), sourceMetric, LinkSourceMetric::uiLabel) { sourceMetric = it }
                }
                if (goal.type == GoalType.WeightedMilestones) {
                    item { GoalEnumDropdown("Milestone", projection.milestones, projection.milestones.firstOrNull { it.id == targetMilestoneId } ?: projection.milestones.firstOrNull(), { it?.name ?: "No Milestone" }, titleCaseValues = false) { targetMilestoneId = it?.id } }
                }
                if (sourceType != LinkSourceType.Track) item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = includeHistory, onCheckedChange = { includeHistory = it })
                        Text("Include existing events since ${goal.startDate}", modifier = Modifier.padding(start = 8.dp))
                    }
                }
                if ((sourceType == LinkSourceType.Track && historyChoice != LinkHistoryChoice.NewEntriesOnly) || (sourceType != LinkSourceType.Track && includeHistory)) {
                    item { WhipOutlinedButton(enabled = sourceAvailability.enabled, onClick = { onPreview(draft()) }, modifier = Modifier.fillMaxWidth()) { Text("Preview Backfill") } }
                    state.backfillPreview?.let { preview ->
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Backfill Preview", fontWeight = FontWeight.Bold)
                                    Text("${preview.scannedEventCount} scanned · ${preview.eligibleEventCount} eligible · ${preview.skippedEventCount} skipped")
                                    preview.skippedReasons.forEach { (reason, count) -> Text("$reason · $count", style = MaterialTheme.typography.bodySmall) }
                                    Text("${preview.contributionCount} contributions · ${formatGoalValue(preview.totalCanonicalValue, goal.precision)} canonical total")
                                    Text("${preview.firstDate ?: "—"} to ${preview.lastDate ?: "—"}")
                                    if (preview.unitExplanation.isNotBlank()) Text(preview.unitExplanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (preview.targetImpact.isNotBlank()) Text(preview.targetImpact, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                item {
                    DisclosureButton(
                        label = "Advanced Automation Options",
                        expanded = showAdvanced,
                        onClick = { showAdvanced = !showAdvanced },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (showAdvanced) {
                    item {
                        OutlinedTextField(
                            name,
                            { name = it },
                            label = { Text("Automation Name *") },
                            isError = validationRequested && name.isBlank(),
                            supportingText = if (validationRequested && name.isBlank()) {
                                { Text("Automation name is required") }
                            } else null,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        GoalEnumDropdown(
                            "Use Automation As",
                            LinkKind.entries,
                            kind,
                            { if (it == LinkKind.Contribution) "Goal Progress" else "Supporting Context Only" },
                            titleCaseValues = false,
                        ) { kind = it }
                    }
                    if (goal.type != GoalType.WeightedMilestones && kind == LinkKind.Contribution && sourceType != LinkSourceType.Track) {
                        item {
                            GoalEnumDropdown(
                                "Contribution Value",
                                LinkValueMode.entries,
                                valueMode,
                                { if (it == LinkValueMode.SourceValue) "Use the Source Value" else "Use a Fixed Value" },
                                titleCaseValues = false,
                            ) { valueMode = it }
                        }
                        if (valueMode == LinkValueMode.FixedValue) item {
                            GoalNumberField(
                                fixedValue,
                                { fixedValue = it },
                                "Fixed Value (${goal.unitId.goalUnitLabel()})",
                                required = true,
                                error = "Enter a fixed value".takeIf {
                                    validationRequested && fixedValue.toWhipDoubleOrNull() == null
                                },
                            )
                        }
                    }
                    if (kind == LinkKind.Contribution) item {
                        ResponsiveFieldPair(
                            first = { field ->
                                GoalNumberField(
                                    multiplier,
                                    { multiplier = it },
                                    "Multiplier",
                                    field,
                                    required = true,
                                    error = "Enter a number".takeIf {
                                        validationRequested && multiplier.toWhipDoubleOrNull() == null
                                    },
                                )
                            },
                            second = { field ->
                                GoalNumberField(
                                    offset,
                                    { offset = it },
                                    "Offset",
                                    field,
                                    required = true,
                                    error = "Enter a number".takeIf {
                                        validationRequested && offset.toWhipDoubleOrNull() == null
                                    },
                                )
                            },
                        )
                    }
                }
            }
        },
        confirmButton = { WhipTextButton(
            enabled = !saving && sourceAvailability.enabled,
            onClick = {
                validationRequested = true
                if (automationValidationMessages.isEmpty()) {
                    onSave(
                        automationDraft,
                        if (sourceType == LinkSourceType.Track) historyChoice != LinkHistoryChoice.NewEntriesOnly else includeHistory,
                    )
                }
            },
        ) { Text(if (saving) "Saving…" else if (initialRule == null) "Create Automation" else "Save Automation") } },
        dismissButton = { WhipTextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } },
    )
    if (addingCondition && selectedTrack != null) TrackConditionEditor(
        selectedTrack,
        onDismiss = { addingCondition = false },
        today = state.currentDate,
        units = BuiltInUnits.all + state.customUnits,
        onSave = { conditions = conditions + it; addingCondition = false },
    )
    if (pickingHistoryDate) WhipDatePickerDialog(
        historyDate,
        onDismiss = { pickingHistoryDate = false },
        onDateSelected = { historyDate = it; pickingHistoryDate = false },
    )
}

private fun LinkHistoryChoice.uiLabel(): String = when (this) {
    LinkHistoryChoice.NewEntriesOnly -> "New Entries Only"
    LinkHistoryChoice.SinceGoalStart -> "Include Since Goal Start"
    LinkHistoryChoice.SinceDate -> "Include Since a Chosen Date"
    LinkHistoryChoice.AllHistory -> "Include All Track History"
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
) = OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label + if (required) " *" else "") },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
    GoalType.MaintainRange -> "Succeed by keeping measurements between a minimum and maximum."
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
    GoalAggregation.TimeInRange -> "Time inside the target range"
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
private fun String.goalUnitLabel() = when (this) { "unitless", "count" -> ""; "kilogram" -> "kg"; "pound" -> "lb"; "kilometre" -> "km"; "distance_m" -> "m"; "second" -> "sec"; "litre" -> "L"; "millilitre" -> "mL"; "fluid_ounce" -> "fl oz"; "currency" -> "$"; else -> this }
private fun formatGoalValue(value: Double?, precision: Int): String = value?.let { String.format(Locale.getDefault(), "%.${precision.coerceIn(0, 6)}f", it) } ?: "—"
private fun parseGoalClock(value: String): Int? {
    if (value.isBlank()) return null
    val parts = value.split(':')
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
    return if (hour in 0..23 && minute in 0..59) hour * 60 + minute else null
}
