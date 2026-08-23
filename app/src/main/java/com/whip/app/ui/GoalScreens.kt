package com.whip.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.DeleteOutline
import com.whip.app.core.AppSettings
import com.whip.app.core.OperationStatus
import com.whip.app.domain.Goal
import com.whip.app.domain.Area
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalAggregationPeriod
import com.whip.app.domain.GoalConsistencyPeriod
import com.whip.app.domain.GoalDirection
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalEntryMode
import com.whip.app.domain.GoalMilestoneDraft
import com.whip.app.domain.GoalPaceType
import com.whip.app.domain.GoalProjection
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.GoalType
import com.whip.app.domain.Contribution
import com.whip.app.domain.LinkKind
import com.whip.app.domain.LinkRule
import com.whip.app.domain.LinkRuleDraft
import com.whip.app.domain.LinkSourceMetric
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.LinkValueMode
import com.whip.app.domain.MetricEntry
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.displayValue
import com.whip.app.domain.buildGoalInsights
import com.whip.app.domain.editableNumericValue
import com.whip.app.domain.toWhipDoubleOrNull
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private enum class GoalDestination { Active, Insights, Completed, Archived }

@Composable
fun GoalAreaContent(
    state: GoalUiState,
    innerPadding: PaddingValues,
    viewModel: GoalViewModel,
    createRequested: Boolean = false,
    recordGoalIdRequest: Long? = null,
    onExternalRequestConsumed: () -> Unit = {},
    openGoalIdRequest: Long? = null,
    onOpenGoalRequestConsumed: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    operationStatus: OperationStatus = OperationStatus.Idle,
    dialogModifier: Modifier = Modifier,
    areas: List<Area> = emptyList(),
    defaultAreaId: String? = null,
    onCreateArea: (String, Long?, (Result<String>) -> Unit) -> Unit = { _, _, _ -> },
    areaScopeLabel: String? = null,
    onAreaChanged: (String?) -> Unit = {},
) {
    if (state.loading || state.errorMessage != null) {
        DomainLoadContent("goals", innerPadding, state.errorMessage, viewModel::retryLoading)
        return
    }
    var destination by rememberSaveable { mutableStateOf(GoalDestination.Active) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var editingGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var recordingGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var actionsGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var linkingGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingLinkRuleId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingMeasurementGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingMeasurementId by rememberSaveable { mutableStateOf<String?>(null) }
    var overridingContributionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var manageOrder by rememberSaveable { mutableStateOf(false) }
    var deleteCandidateGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var templatesOpen by rememberSaveable { mutableStateOf(false) }
    var templateDraft by rememberSaveable { mutableStateOf<GoalDraft?>(null) }
    var editorSavePending by rememberSaveable { mutableStateOf(false) }
    var editorSaveStarted by rememberSaveable { mutableStateOf(false) }
    var creationBaselineIds by rememberSaveable { mutableStateOf(emptyList<Long>()) }
    var awaitingCreatedGoal by rememberSaveable { mutableStateOf(false) }
    var postSaveGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var linkSavePending by rememberSaveable { mutableStateOf(false) }
    var linkSaveStarted by rememberSaveable { mutableStateOf(false) }
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
    val deleteCandidate = deleteCandidateGoalId?.let(projectionById::get)
    LaunchedEffect(createRequested, recordGoalIdRequest) {
        if (createRequested) creating = true
        if (recordGoalIdRequest != null && state.active.any { it.goal.id == recordGoalIdRequest }) recordingGoalId = recordGoalIdRequest
        if (createRequested || recordGoalIdRequest != null) onExternalRequestConsumed()
    }
    LaunchedEffect(operationStatus, editorSavePending) {
        if (!editorSavePending) return@LaunchedEffect
        when (operationStatus) {
            is OperationStatus.Running -> editorSaveStarted = true
            is OperationStatus.Succeeded -> {
                if (awaitingCreatedGoal) {
                    state.active.filterNot { it.goal.id in creationBaselineIds }
                        .maxByOrNull { it.goal.createdAtMillis }
                        ?.let { postSaveGoalId = it.goal.id; awaitingCreatedGoal = false }
                }
                creating = false
                editingGoalId = null
                templateDraft = null
                editorSavePending = false
                editorSaveStarted = false
            }
            is OperationStatus.Failed -> {
                editorSavePending = false
                editorSaveStarted = false
            }
            OperationStatus.Idle -> Unit
        }
    }
    LaunchedEffect(state.active, awaitingCreatedGoal) {
        if (awaitingCreatedGoal && !editorSavePending) {
            state.active.filterNot { it.goal.id in creationBaselineIds }
                .maxByOrNull { it.goal.createdAtMillis }
                ?.let { postSaveGoalId = it.goal.id; awaitingCreatedGoal = false }
        }
    }
    LaunchedEffect(operationStatus, linkSavePending) {
        if (!linkSavePending) return@LaunchedEffect
        when (operationStatus) {
            is OperationStatus.Running -> linkSaveStarted = true
            is OperationStatus.Succeeded -> {
                linkingGoalId = null; editingLinkRuleId = null
                linkSavePending = false; linkSaveStarted = false
            }
            is OperationStatus.Failed -> { linkSavePending = false; linkSaveStarted = false }
            OperationStatus.Idle -> Unit
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
    val list = when (destination) {
        GoalDestination.Active, GoalDestination.Insights -> state.active
        GoalDestination.Completed -> state.completed
        GoalDestination.Archived -> state.archived
    }
    val visible = list.filter { projection ->
        query.isBlank() || listOf(
            projection.goal.name,
            projection.goal.description,
            projection.goal.area,
            projection.goal.tags.joinToString(" "),
        ).any { it.contains(query, ignoreCase = true) }
    }
    val destinationScrollState = rememberScrollState()
    val destinationBringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(destination) { destinationBringIntoView.bringIntoView() }
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(destinationScrollState)
                .padding(start = 20.dp, end = 52.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GoalDestination.entries.forEach { tab ->
                FilterChip(
                    selected = destination == tab,
                    modifier = if (destination == tab) Modifier.bringIntoViewRequester(destinationBringIntoView) else Modifier,
                    onClick = { destination = tab },
                    label = { Text(tab.name) },
                )
            }
        }
        if (destination == GoalDestination.Insights) {
            GoalInsightsContent(
                projections = state.active,
                innerPadding = PaddingValues(20.dp, 12.dp, 20.dp, 96.dp),
                onOpen = { actionsGoalId = it.goal.id },
            )
        } else LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(destination.name + " goals", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Long-term measurements, consistency, ranges, totals, and project milestones.")
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { searchOpen = !searchOpen }) { Text(if (searchOpen) "Hide search" else "Search") }
                    if (destination == GoalDestination.Active) OutlinedButton(onClick = { templatesOpen = true }) { Text("Templates") }
                    if (destination == GoalDestination.Active && list.size > 1) {
                        OutlinedButton(onClick = { manageOrder = !manageOrder }) { Text(if (manageOrder) "Finish ordering" else "Manage order") }
                    }
                }
            }
            if (searchOpen || query.isNotBlank()) item { OutlinedTextField(query, { query = it }, label = { Text("Search goals, areas, or tags") }, modifier = Modifier.fillMaxWidth()) }
            if (visible.isEmpty()) item { Text(if (list.isEmpty() && destination == GoalDestination.Active) areaScopeLabel?.let { "No active goals in $it. Create one in this area with +." } ?: "No active goals. Create one around any measurable value or milestone project." else if (list.isEmpty()) areaScopeLabel?.let { "Nothing in $it yet." } ?: "Nothing here yet." else "No goals match this search.") }
            items(visible, key = { it.goal.id }) { projection ->
                val index = list.indexOfFirst { it.goal.id == projection.goal.id }
                Column {
                    GoalCard(
                        projection,
                        customUnits = state.customUnits,
                        onOpen = { actionsGoalId = projection.goal.id },
                        onRecord = { recordingGoalId = projection.goal.id },
                        onToggleMilestone = viewModel::toggleMilestone,
                    )
                    if (manageOrder && destination == GoalDestination.Active && query.isBlank()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(enabled = index > 0, onClick = {
                                val ids = list.map { it.goal.id }.toMutableList()
                                java.util.Collections.swap(ids, index, index - 1)
                                viewModel.reorder(ids)
                            }) { Icon(Icons.Outlined.ArrowUpward, contentDescription = null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(4.dp)); Text("Earlier") }
                            TextButton(enabled = index in 0 until list.lastIndex, onClick = {
                                val ids = list.map { it.goal.id }.toMutableList()
                                java.util.Collections.swap(ids, index, index + 1)
                                viewModel.reorder(ids)
                            }) { Icon(Icons.Outlined.ArrowDownward, contentDescription = null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(4.dp)); Text("Later") }
                        }
                    }
                }
            }
            if (destination == GoalDestination.Active && list.isEmpty()) item {
                Text("Use the + button to create a goal, or start with a template.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (creating || editing != null) {
        GoalEditorDialog(
            dialogModifier = dialogModifier,
            projection = editing,
            initialDraft = templateDraft.takeIf { editing == null },
            today = state.currentDate,
            customUnits = state.customUnits,
            defaults = viewModel.defaultSettings(),
            areas = areas,
            defaultAreaId = defaultAreaId,
            onCreateArea = onCreateArea,
            saving = editorSavePending,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onDismiss = {
                creating = false
                editingGoalId = null
                templateDraft = null
                editorSavePending = false
                editorSaveStarted = false
            },
            onSave = { draft ->
                if (editing == null) {
                    creationBaselineIds = state.active.map { it.goal.id }
                    awaitingCreatedGoal = true
                }
                editorSavePending = true
                editorSaveStarted = false
                viewModel.saveGoal(editing?.goal?.id, draft)
                onAreaChanged(draft.areaId)
            },
        )
    }
    postSaveGoalId?.let { goalId ->
        val created = projectionById[goalId]
        if (created != null) PaneAwareAlertDialog(
            modifier = dialogModifier,
            onDismissRequest = { postSaveGoalId = null },
            title = { Text("Goal created") },
            text = {
                Text(
                    if (created.goal.baseline != null) {
                        "${created.goal.name} is ready with its starting value. You can connect future progress from another part of Whip, or continue."
                    } else {
                        "${created.goal.name} is ready. Add a starting value now, connect progress from another part of Whip, or continue."
                    },
                )
            },
            confirmButton = {
                if (created.goal.baseline == null) {
                    TextButton(onClick = { recordingGoalId = goalId; postSaveGoalId = null }) { Text("Log first value") }
                } else {
                    TextButton(onClick = { linkingGoalId = goalId; editingLinkRuleId = null; postSaveGoalId = null; viewModel.clearLinkPreview() }) { Text("Link progress") }
                }
            },
            dismissButton = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (created.goal.baseline == null) TextButton(onClick = { linkingGoalId = goalId; editingLinkRuleId = null; postSaveGoalId = null; viewModel.clearLinkPreview() }) { Text("Link progress") }
                    TextButton(onClick = { postSaveGoalId = null }) { Text("Done") }
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
            dialogModifier = dialogModifier,
            rules = state.linkRules.filter { it.targetGoalId == projection.goal.id },
            contributions = state.contributions.filter { it.targetGoalId == projection.goal.id },
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
            onEdit = { editingGoalId = projection.goal.id; actionsGoalId = null },
            onDuplicate = { viewModel.duplicate(projection.goal.id); actionsGoalId = null },
            onPin = { viewModel.setPinned(projection.goal.id, !projection.goal.pinned); actionsGoalId = null },
            onPause = { viewModel.setStatus(projection.goal.id, if (projection.goal.status == GoalStatus.Paused) GoalStatus.Active else GoalStatus.Paused); actionsGoalId = null },
            onComplete = { viewModel.setStatus(projection.goal.id, GoalStatus.Completed); actionsGoalId = null },
            onAbandon = { viewModel.setStatus(projection.goal.id, GoalStatus.Abandoned); actionsGoalId = null },
            onArchive = { viewModel.setStatus(projection.goal.id, if (projection.goal.status == GoalStatus.Archived) GoalStatus.Active else GoalStatus.Archived); actionsGoalId = null },
            onDelete = { deleteCandidateGoalId = projection.goal.id; actionsGoalId = null },
        )
    }
    deleteCandidate?.let { projection ->
        val goal = projection.goal
        val dependentRules = state.linkRules.count { it.targetGoalId == goal.id || it.sourceMetricId == goal.metricId }
        val linkedEntries = state.contributions.count { it.targetGoalId == goal.id }
        PermanentDeleteDialog(
            title = "Delete ${goal.name} permanently?",
            impacts = listOf(
                "${projection.entries.size} measurement${if (projection.entries.size == 1) "" else "s"}, milestones, and completion snapshots will be removed",
                "$dependentRules incoming or outgoing link${if (dependentRules == 1) "" else "s"} will be removed",
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
            onSave = { draft, includeHistory ->
                linkSavePending = true
                linkSaveStarted = false
                if (editingLinkRule == null) viewModel.createLink(draft, includeHistory)
                else viewModel.updateLink(editingLinkRule.id, draft)
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

@Composable
fun GoalCard(
    projection: GoalProjection,
    customUnits: List<UnitDefinition> = emptyList(),
    onOpen: () -> Unit,
    onRecord: () -> Unit,
    onToggleMilestone: (Long, Boolean) -> Unit,
) {
    val goal = projection.goal
    Card(modifier = Modifier.fillMaxWidth().clickable(onClickLabel = "Open ${goal.name}", onClick = onOpen)) {
        Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(goal.icon, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(goal.name, fontWeight = FontWeight.Bold)
                    if (goal.areaId != null) AreaBadge(goal.areaId, goal.area)
                    Text(goal.type.displayLabel(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (goal.status == GoalStatus.Active && goal.type != GoalType.WeightedMilestones) {
                    TextButton(onClick = onRecord) { Text("Log") }
                }
            }
            projection.progress?.let { progress ->
                LinearProgressIndicator(progress = { progress.toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Text("${(progress * 100).toInt()}% complete", color = MaterialTheme.colorScheme.primary)
            }
            projection.consistency?.let { consistency ->
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
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = milestone.completed, onCheckedChange = { onToggleMilestone(milestone.id, it) })
                    Text(milestone.name, modifier = Modifier.weight(1f))
                    if (milestone.reward.isNotBlank()) Text(milestone.reward, style = MaterialTheme.typography.labelSmall)
                }
            }
            if (goal.description.isNotBlank()) Text(goal.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
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
            Text("Goal insights", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Trends, pace, forecasts, and data quality for active goals.")
        }
        if (projections.isEmpty()) item { Text("Create and log a goal to see its trend here.") }
        items(projections, key = { "goal-insight-${it.goal.id}" }) { projection ->
            val insights = remember(projection) { buildGoalInsights(projection.goal, projection.entries, projection.milestones) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = "Open ${projection.goal.name}") { onOpen(projection) }
                    .testTag("goal-insight-${projection.goal.id}"),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${projection.goal.icon} ${projection.goal.name}", fontWeight = FontWeight.Bold)
                    val chartValues = insights.points.mapNotNull { it.progress ?: it.canonicalValue }
                    if (chartValues.size >= 2) {
                        GoalLineChart(
                            values = chartValues,
                            description = "${projection.goal.name} trend with ${chartValues.size} points",
                        )
                    } else Text("Log at least two observations for a trend line.")
                    Text(
                        listOfNotNull(
                            projection.progress?.let { "${(it * 100).toInt()}% complete" },
                            projection.onPace?.let { if (it) "On pace" else "Behind pace" },
                            insights.ratePerDay?.let { "Rate ${formatGoalValue(it, projection.goal.precision)} per day" },
                            insights.forecastDate?.let { "Forecast $it" },
                        ).joinToString(" · ").ifBlank { "Progress becomes available after the first log." },
                    )
                    Text(insights.dataQualityExplanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
            name = "Weight target", icon = "◈", type = GoalType.ReachValue,
            dimension = UnitDimension.Mass, unitId = defaults.massUnitId,
            targetMin = if (defaults.massUnitId == "pound") 150.0 else 75.0,
            direction = GoalDirection.Decrease, aggregation = GoalAggregation.Latest,
            startDate = today,
        ),
        "Build savings" to GoalDraft(
            name = "Savings", icon = "\$", type = GoalType.AccumulateTotal,
            dimension = UnitDimension.Money, unitId = "currency", targetMin = 1000.0,
            aggregation = GoalAggregation.Sum, entryMode = GoalEntryMode.AmountToAdd,
            startDate = today,
        ),
        "Cover a distance" to GoalDraft(
            name = "Distance", icon = "↗", type = GoalType.AccumulateTotal,
            dimension = UnitDimension.Distance, unitId = defaults.distanceUnitId,
            targetMin = 100.0, aggregation = GoalAggregation.Sum,
            entryMode = GoalEntryMode.AmountToAdd, startDate = today,
        ),
        "Read pages" to GoalDraft(
            name = "Reading", icon = "▤", type = GoalType.AccumulateTotal,
            dimension = UnitDimension.Count, unitId = "count", targetMin = 1000.0,
            aggregation = GoalAggregation.Sum, entryMode = GoalEntryMode.AmountToAdd,
            startDate = today,
        ),
        "Stay consistent" to GoalDraft(
            name = "Weekly consistency", icon = "✓", type = GoalType.Consistency,
            dimension = UnitDimension.Count, unitId = "count", precision = 0,
            targetMin = 3.0, aggregation = GoalAggregation.CompletionCount,
            consistencyPeriod = GoalConsistencyPeriod.Week,
            consistencyRequiredPeriods = 12, startDate = today,
        ),
        "Finish a project" to GoalDraft(
            name = "Project", icon = "◆", type = GoalType.WeightedMilestones,
            milestones = listOf(
                GoalMilestoneDraft("Plan", 1.0),
                GoalMilestoneDraft("Build", 2.0),
                GoalMilestoneDraft("Finish", 1.0),
            ),
            aggregation = GoalAggregation.CompletionCount,
            paceType = GoalPaceType.Milestone,
            startDate = today,
        ),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start from a goal template") },
        text = {
            LazyColumn(
                modifier = Modifier.testTag("goal-template-list"),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(templates, key = { it.first }) { (label, draft) ->
                    TextButton(onClick = { onChoose(draft) }, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("${draft.icon}  $label", fontWeight = FontWeight.SemiBold)
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun goalTemplateDescription(label: String): String = when (label) {
    "Reach a weight" -> "A current measurement moving toward a mass target."
    "Build savings" -> "Add deposits toward a money total."
    "Cover a distance" -> "Accumulate walks, runs, rides, or any distance."
    "Read pages" -> "Add pages toward a reading total."
    "Stay consistent" -> "Hit a weekly success target for twelve weeks."
    "Finish a project" -> "Plan, build, and finish weighted milestones."
    else -> "Prefills an editable goal; nothing is saved until you confirm."
}

@Composable
private fun GoalEditorDialog(
    projection: GoalProjection?,
    initialDraft: GoalDraft? = null,
    today: LocalDate,
    customUnits: List<UnitDefinition>,
    defaults: AppSettings = AppSettings(),
    onDismiss: () -> Unit,
    onSave: (GoalDraft) -> Unit,
    onRequestNotificationPermission: () -> Unit = {},
    saving: Boolean = false,
    dialogModifier: Modifier = Modifier,
    areas: List<Area> = emptyList(),
    defaultAreaId: String? = null,
    onCreateArea: (String, Long?, (Result<String>) -> Unit) -> Unit = { _, _, _ -> },
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
    var icon by rememberSaveable(editorKey) { mutableStateOf(goal?.icon ?: initialDraft?.icon ?: "◎") }
    var colorHex by rememberSaveable(editorKey) { mutableStateOf(colorArgbToHex(goal?.colorArgb)) }
    var type by rememberSaveable(editorKey) { mutableStateOf(goal?.type ?: initialDraft?.type ?: GoalType.ReachValue) }
    var unitId by rememberSaveable(editorKey) { mutableStateOf(goal?.unitId ?: initialDraft?.unitId ?: "unitless") }
    var dimension by rememberSaveable(editorKey) { mutableStateOf(goal?.dimension ?: initialDraft?.dimension ?: UnitDimension.Unitless) }
    var precision by rememberSaveable(editorKey) { mutableStateOf((goal?.precision ?: initialDraft?.precision ?: defaults.numberPrecision).toString()) }
    var baseline by rememberSaveable(editorKey) { mutableStateOf(goal?.displayValue(goal.baseline, customUnits)?.let(::editableNumericValue) ?: initialDraft?.baseline?.let(::editableNumericValue).orEmpty()) }
    var targetMin by rememberSaveable(editorKey) { mutableStateOf(goal?.displayValue(goal.targetMin, customUnits)?.let(::editableNumericValue) ?: initialDraft?.targetMin?.let(::editableNumericValue).orEmpty()) }
    var targetMax by rememberSaveable(editorKey) { mutableStateOf(goal?.displayValue(goal.targetMax, customUnits)?.let(::editableNumericValue) ?: initialDraft?.targetMax?.let(::editableNumericValue).orEmpty()) }
    var direction by rememberSaveable(editorKey) { mutableStateOf(goal?.direction ?: initialDraft?.direction ?: GoalDirection.Increase) }
    var aggregation by rememberSaveable(editorKey) { mutableStateOf(goal?.aggregation ?: initialDraft?.aggregation ?: GoalAggregation.Latest) }
    var entryMode by rememberSaveable(editorKey) { mutableStateOf(goal?.entryMode ?: initialDraft?.entryMode ?: GoalEntryMode.CurrentTotal) }
    var pace by rememberSaveable(editorKey) { mutableStateOf(goal?.paceType ?: initialDraft?.paceType ?: GoalPaceType.Linear) }
    var deadline by rememberSaveable(editorKey) { mutableStateOf(goal?.deadline ?: initialDraft?.deadline) }
    var showDatePicker by rememberSaveable(editorKey) { mutableStateOf(false) }
    var milestoneDrafts by rememberSaveable(editorKey) {
        mutableStateOf(
            ArrayList(
                projection?.milestones?.map {
                    GoalMilestoneDraft(
                        name = it.name,
                        weight = it.weight,
                        targetValue = it.targetValue,
                        reward = it.reward,
                        id = it.id,
                        uuid = it.uuid,
                    )
                } ?: initialDraft?.milestones.orEmpty(),
            ),
        )
    }
    var advanced by rememberSaveable(editorKey) {
        mutableStateOf(
            defaults.powerMode || goal?.let {
                it.description.isNotBlank() || it.tags.isNotEmpty() ||
                    it.colorArgb != null || it.reminderMinutes != null ||
                    it.aggregationPeriod != GoalAggregationPeriod.All || it.rollingDays != null
            } == true,
        )
    }
    var reminder by rememberSaveable(editorKey) { mutableStateOf((goal?.reminderMinutes ?: initialDraft?.reminderMinutes)?.let { "%02d:%02d".format(it / 60, it % 60) }.orEmpty()) }
    var aggregationPeriod by rememberSaveable(editorKey) { mutableStateOf(goal?.aggregationPeriod ?: initialDraft?.aggregationPeriod ?: GoalAggregationPeriod.All) }
    var rollingDays by rememberSaveable(editorKey) { mutableStateOf((goal?.rollingDays ?: initialDraft?.rollingDays ?: 7).toString()) }
    var consistencyPeriod by rememberSaveable(editorKey) { mutableStateOf(goal?.consistencyPeriod ?: initialDraft?.consistencyPeriod ?: GoalConsistencyPeriod.Week) }
    var consistencyRequiredPeriods by rememberSaveable(editorKey) { mutableStateOf((goal?.consistencyRequiredPeriods ?: initialDraft?.consistencyRequiredPeriods ?: 12).toString()) }
    val editorFingerprint = listOf(
        name, description, areaId, area, tags, icon, colorHex, type, unitId, dimension, precision,
        baseline, targetMin, targetMax, direction, aggregation, entryMode, pace, deadline,
        reminder, aggregationPeriod, rollingDays, consistencyPeriod, consistencyRequiredPeriods,
        milestoneDrafts.map { "${it.id}:${it.uuid}:${it.name}:${it.weight}:${it.targetValue}:${it.reward}" },
    ).joinToString("\u001f")
    val initialFingerprint by rememberSaveable(editorKey) { mutableStateOf(editorFingerprint) }
    var showDiscardConfirmation by rememberSaveable(editorKey) { mutableStateOf(false) }
    val requestDismiss = { if (editorFingerprint != initialFingerprint) showDiscardConfirmation = true else onDismiss() }
    BackHandler(enabled = !showDiscardConfirmation, onBack = requestDismiss)
    ProductivityEditorDialog(
        modifier = dialogModifier,
        testTag = "goal-editor-surface",
        onDismissRequest = requestDismiss,
        title = { Text(if (goal == null) "Create goal" else "Edit goal") },
        text = {
            LazyColumn(
                modifier = Modifier.testTag("goal-editor-fields"),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                item { ResponsiveFieldPair(
                    first = { field -> OutlinedTextField(icon, { icon = it.take(2) }, label = { Text("Icon") }, modifier = field) },
                    second = { field -> OutlinedTextField(name, { name = it }, label = { Text("Name *") }, modifier = field.testTag("goal-editor-name")) },
                ) }
                if (advanced) item { OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    GoalEnumDropdown("Goal type", GoalType.entries, type, GoalType::displayLabel) { selected ->
                        type = selected
                        when (selected) {
                        GoalType.ReduceValue -> { direction = GoalDirection.Decrease; aggregation = GoalAggregation.Latest }
                        GoalType.AccumulateTotal -> { direction = GoalDirection.Increase; aggregation = GoalAggregation.Sum; entryMode = GoalEntryMode.AmountToAdd }
                        GoalType.MaintainRange -> aggregation = GoalAggregation.Latest
                        GoalType.MeetAverage -> { aggregation = GoalAggregation.Average; aggregationPeriod = GoalAggregationPeriod.Week }
                        GoalType.Consistency -> {
                            aggregation = GoalAggregation.CompletionCount
                            unitId = "count"
                            dimension = UnitDimension.Count
                            precision = "0"
                            if (targetMin.isBlank()) targetMin = "3"
                        }
                        GoalType.WeightedMilestones -> { aggregation = GoalAggregation.CompletionCount; unitId = "unitless"; dimension = UnitDimension.Unitless }
                        GoalType.OpenEndedTrend -> { pace = GoalPaceType.None; aggregation = GoalAggregation.Latest }
                        GoalType.ReachValue -> { direction = GoalDirection.Increase; aggregation = GoalAggregation.Latest }
                        }
                    }
                    Text(type.explanation(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (type != GoalType.WeightedMilestones) {
                    if (type != GoalType.Consistency) item { GoalNumberField(baseline, { baseline = it }, "Starting value (optional)") }
                    item {
                        if (type == GoalType.MaintainRange) {
                            ResponsiveFieldPair(
                                first = { field -> GoalNumberField(targetMin, { targetMin = it }, "Range minimum", field) },
                                second = { field -> GoalNumberField(targetMax, { targetMax = it }, "Range maximum", field) },
                            )
                        } else GoalNumberField(
                            targetMin,
                            { targetMin = it },
                            if (type == GoalType.Consistency) "Successes per period" else "Target",
                            Modifier.testTag("goal-editor-target"),
                        )
                    }
                    if (type == GoalType.Consistency) {
                        item {
                            ResponsiveFieldPair(
                                first = { field -> Column(field) { GoalEnumDropdown("Period", GoalConsistencyPeriod.entries, consistencyPeriod, { it.name }) { consistencyPeriod = it } } },
                                second = { field -> GoalNumberField(consistencyRequiredPeriods, { consistencyRequiredPeriods = it }, "Number of periods", field) },
                            )
                        }
                    }
                } else {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Milestones", fontWeight = FontWeight.Bold)
                            milestoneDrafts.forEachIndexed { index, draft ->
                                Card(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            IconButton(
                                                enabled = index > 0,
                                                onClick = {
                                                    milestoneDrafts = ArrayList(milestoneDrafts).also {
                                                        val moved = it.removeAt(index)
                                                        it.add(index - 1, moved)
                                                    }
                                                },
                                            ) { Icon(Icons.Outlined.ArrowUpward, contentDescription = "Move ${draft.name.ifBlank { "milestone ${index + 1}" }} up") }
                                            IconButton(
                                                enabled = index < milestoneDrafts.lastIndex,
                                                onClick = {
                                                    milestoneDrafts = ArrayList(milestoneDrafts).also {
                                                        val moved = it.removeAt(index)
                                                        it.add(index + 1, moved)
                                                    }
                                                },
                                            ) { Icon(Icons.Outlined.ArrowDownward, contentDescription = "Move ${draft.name.ifBlank { "milestone ${index + 1}" }} down") }
                                            IconButton(
                                                onClick = { milestoneDrafts = ArrayList(milestoneDrafts).also { it.removeAt(index) } },
                                            ) { Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove ${draft.name.ifBlank { "milestone ${index + 1}" }}") }
                                        }
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    milestoneDrafts = ArrayList(milestoneDrafts).also {
                                        it += GoalMilestoneDraft("", uuid = java.util.UUID.randomUUID().toString())
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Add milestone")
                            }
                        }
                    }
                }
                item { OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) { Text(deadline?.let { "Deadline ${it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}" } ?: "Add deadline") } }
                item {
                    AreaPicker(
                        areas = areas,
                        selectedAreaId = areaId,
                        selectedAreaName = area,
                        onSelect = { id, value -> areaId = id; area = value },
                        onCreateArea = onCreateArea,
                        modifier = Modifier.fillMaxWidth(),
                        dialogModifier = dialogModifier,
                        inheritedFromScope = projection == null && initialDraft?.areaId == null && defaultAreaId != null,
                    )
                }
                item {
                    TextButton(onClick = { advanced = !advanced }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (advanced) "Hide advanced options" else "Show advanced options", modifier = Modifier.fillMaxWidth())
                    }
                }
                if (advanced) {
                    item { OutlinedTextField(tags, { tags = it }, label = { Text("Tags, comma-separated") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(colorHex, { colorHex = it }, label = { Text("Color, #RRGGBB or #AARRGGBB") }, modifier = Modifier.fillMaxWidth()) }
                    item {
                        GoalEnumDropdown("Unit dimension", UnitDimension.entries, dimension, { it.name }) { selected ->
                            dimension = selected
                            val units = BuiltInUnits.all + customUnits.filter { !it.archived || it.id == unitId }
                            val preferred = defaults.preferredUnitId(selected)
                            if (preferred != null && units.any { it.id == preferred && it.dimension == selected }) {
                                unitId = preferred
                            }
                            if (units.none { it.id == unitId && it.dimension == selected }) {
                                unitId = units.firstOrNull { it.dimension == selected }?.id ?: unitId
                            }
                        }
                    }
                    val compatibleUnits = (BuiltInUnits.all + customUnits).filter {
                        it.dimension == dimension && (!it.archived || it.id == unitId)
                    }
                    if (compatibleUnits.isNotEmpty()) {
                        item {
                            GoalEnumDropdown(
                                "Saved unit",
                                compatibleUnits,
                                compatibleUnits.firstOrNull { it.id == unitId } ?: compatibleUnits.first(),
                                ::goalUnitDefinitionLabel,
                            ) { unitId = it.id }
                        }
                    }
                    item { Text("Create additional units under Settings > Custom units.") }
                    item { GoalNumberField(precision, { precision = it }, "Decimal places (0–6)") }
                    item { GoalEnumDropdown("How entries combine", GoalAggregation.entries, aggregation, GoalAggregation::displayLabel) { aggregation = it } }
                    if (type != GoalType.Consistency) {
                        item { GoalEnumDropdown("Time window", GoalAggregationPeriod.entries, aggregationPeriod, GoalAggregationPeriod::displayLabel) { aggregationPeriod = it } }
                        if (aggregationPeriod == GoalAggregationPeriod.RollingDays) item { GoalNumberField(rollingDays, { rollingDays = it }, "Rolling days") }
                    }
                    item { GoalEnumDropdown("Entry meaning", GoalEntryMode.entries, entryMode, { if (it == GoalEntryMode.CurrentTotal) "Each entry is the current total" else "Each entry is an amount to add" }) { entryMode = it } }
                    item { GoalEnumDropdown("Pace model", GoalPaceType.entries, pace, GoalPaceType::displayLabel) { pace = it } }
                    item { GoalEnumDropdown("Desired direction", GoalDirection.entries, direction, GoalDirection::displayLabel) { direction = it } }
                    item {
                        ClockPickerButton(
                            label = "Daily measurement reminder",
                            minutes = parseGoalClock(reminder),
                            onChange = { minutes ->
                                if (minutes != null && parseGoalClock(reminder) == null) onRequestNotificationPermission()
                                reminder = minutes?.let(::formatClockMinutes).orEmpty()
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank() && !saving, onClick = {
                val milestones = milestoneDrafts.filter { it.name.isNotBlank() }
                    .map { it.copy(name = it.name.trim(), reward = it.reward.trim()) }
                val draft = GoalDraft(
                        name = name,
                        description = description,
                        areaId = areaId,
                        area = area,
                        tags = tags.split(',').map(String::trim).filter(String::isNotBlank),
                        icon = icon.ifBlank { "◎" }, colorArgb = parseColorArgb(colorHex), type = type, dimension = dimension,
                        unitId = unitId, precision = precision.toIntOrNull()?.coerceIn(0, 6) ?: 1,
                        baseline = baseline.toWhipDoubleOrNull(), targetMin = targetMin.toWhipDoubleOrNull(),
                        targetMax = targetMax.toWhipDoubleOrNull(), direction = direction,
                        startDate = goal?.startDate ?: today, deadline = deadline,
                        aggregation = aggregation, entryMode = entryMode, paceType = pace,
                        reminderMinutes = parseGoalClock(reminder),
                        milestones = milestones,
                        aggregationPeriod = aggregationPeriod,
                        rollingDays = rollingDays.toIntOrNull(),
                        consistencyPeriod = consistencyPeriod,
                        consistencyRequiredPeriods = consistencyRequiredPeriods.toIntOrNull(),
                    )
                onSave(draft)
            }) { Text(if (saving) "Saving…" else "Save") }
        },
        dismissButton = { TextButton(onClick = requestDismiss, enabled = !saving) { Text("Cancel") } },
    )
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry == null) "Record ${projection.goal.name}" else "Edit measurement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (projection.goal.entryMode == GoalEntryMode.CurrentTotal) "Enter the current total/value." else "Enter an amount to add to the goal total.")
                GoalNumberField(
                    value,
                    { value = it },
                    "Value (${(entry?.enteredUnitId ?: projection.goal.unitId).goalUnitLabel()})",
                )
                OutlinedTextField(note, { note = it }, label = { Text("Optional note") }, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = { showDatePicker = true }) { Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))) }
            }
        },
        confirmButton = { TextButton(enabled = value.toWhipDoubleOrNull() != null, onClick = { onRecord(requireNotNull(value.toWhipDoubleOrNull()), date, note) }) { Text("Save") } },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
    if (showDatePicker) WhipDatePickerDialog(date, { showDatePicker = false }, { date = it; showDatePicker = false })
}

@Composable
private fun GoalActionsDialog(
    projection: GoalProjection,
    dialogModifier: Modifier = Modifier,
    rules: List<LinkRule>,
    contributions: List<Contribution>,
    onDismiss: () -> Unit,
    onAddLink: () -> Unit,
    onEditLink: (LinkRule) -> Unit,
    onSetLinkEnabled: (Long, Boolean) -> Unit,
    onDeleteLink: (Long) -> Unit,
    onSetContributionExcluded: (Long, Boolean) -> Unit,
    onOverrideContribution: (Contribution) -> Unit,
    onEditMeasurement: (MetricEntry) -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onPin: () -> Unit,
    onPause: () -> Unit,
    onComplete: () -> Unit,
    onAbandon: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    var visibleMeasurements by rememberSaveable(projection.goal.id) { mutableIntStateOf(25) }
    var showAccessibleTable by rememberSaveable(projection.goal.id) { mutableStateOf(false) }
    var visibleContributions by rememberSaveable(projection.goal.id) { mutableIntStateOf(10) }
    var section by rememberSaveable(projection.goal.id) { mutableStateOf(GoalDetailSection.Overview) }
    val insights = remember(projection) { buildGoalInsights(projection.goal, projection.entries, projection.milestones) }
    PaneAwareAlertDialog(
        modifier = dialogModifier.testTag("goal-detail-surface"),
        onDismissRequest = onDismiss,
        title = { Text(projection.goal.name) },
        text = {
            LazyColumn {
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        GoalDetailSection.entries.forEach { value ->
                            FilterChip(selected = section == value, onClick = { section = value }, label = { Text(value.label) })
                        }
                    }
                }
                if (section == GoalDetailSection.Overview) {
                item {
                    Text("Progress insight", fontWeight = FontWeight.Bold)
                    val chartValues = insights.points.mapNotNull { it.progress ?: it.canonicalValue }
                    if (chartValues.size >= 2) {
                        GoalLineChart(
                            values = chartValues,
                            description = "${projection.goal.name} progress chart with ${chartValues.size} points from ${insights.points.first().date} to ${insights.points.last().date}",
                        )
                    } else Text("More observations are needed for a trend line.")
                    Text(
                        listOfNotNull(
                            insights.ratePerDay?.let { "Rate ${formatGoalValue(it, projection.goal.precision)} per day" },
                            insights.forecastDate?.let { "Forecast $it (${insights.confidence} confidence)" },
                        ).joinToString(" · ").ifBlank { "Rate and forecast unavailable" },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (insights.targetMin != null || insights.targetMax != null) {
                        Text("Target overlay: ${formatGoalValue(insights.targetMin, projection.goal.precision)} to ${formatGoalValue(insights.targetMax ?: insights.targetMin, projection.goal.precision)}")
                    }
                    Text(insights.dataQualityExplanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { showAccessibleTable = !showAccessibleTable }) {
                        Text(if (showAccessibleTable) "Hide trend data table" else "Show accessible trend data table")
                    }
                }
                if (showAccessibleTable) {
                    items(insights.points.takeLast(visibleMeasurements), key = { "insight-${it.date}" }) { point ->
                        Text(
                            "${point.date}: value ${formatGoalValue(point.canonicalValue, projection.goal.precision)}, " +
                                "progress ${point.progress?.let { "${(it * 100).toInt()}%" } ?: "not applicable"}, ${point.recordedEntries} source entries",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                }
                if (section == GoalDetailSection.History) {
                item { Text("${projection.entries.size} measurements", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                items(projection.entries.take(visibleMeasurements), key = { it.id }) { entry ->
                    TextButton(onClick = { onEditMeasurement(entry) }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "${entry.localDate}: ${entry.enteredValue?.let(::editableNumericValue) ?: entry.status.name} ${entry.enteredUnitId.orEmpty()}" +
                                " · ${entry.sourceType.name}${entry.sourceId?.let { " ($it)" }.orEmpty()}" +
                                if (entry.note.isBlank()) "" else " · ${entry.note}",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (visibleMeasurements < projection.entries.size) item {
                    OutlinedButton(
                        onClick = { visibleMeasurements = (visibleMeasurements + 25).coerceAtMost(projection.entries.size) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Show 25 more · ${projection.entries.size - visibleMeasurements} remaining") }
                }
                }
                if (section == GoalDetailSection.Connections) {
                item { Text("Links", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp)) }
                if (rules.isEmpty()) item { Text("No automatic contributions yet.") }
                items(rules, key = { "rule-${it.id}" }) { rule ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = rule.enabled, onCheckedChange = { onSetLinkEnabled(rule.id, it) })
                            Column(Modifier.weight(1f)) {
                                Text(rule.name, fontWeight = FontWeight.SemiBold)
                                Text("${rule.sourceType.name} · ${rule.sourceMetric.name}", style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(onClick = { onEditLink(rule) }) { Text("Edit") }
                            TextButton(onClick = { onDeleteLink(rule.id) }) { Text("Remove") }
                        }
                        val ruleContributions = contributions.filter { it.linkRuleId == rule.id }
                        ruleContributions.take(visibleContributions).forEach { contribution ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = !contribution.excluded, onCheckedChange = { onSetContributionExcluded(contribution.id, !it) })
                                Column(Modifier.weight(1f)) {
                                    Text("${contribution.localDate}: ${contribution.explanation}", style = MaterialTheme.typography.bodySmall)
                                    contribution.overrideValue?.let { Text("Override: $it canonical", style = MaterialTheme.typography.labelSmall) }
                                }
                                TextButton(onClick = { onOverrideContribution(contribution) }) { Text("Override") }
                            }
                        }
                        if (visibleContributions < ruleContributions.size) {
                            TextButton(
                                onClick = { visibleContributions = (visibleContributions + 25).coerceAtMost(ruleContributions.size) },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Show 25 more · ${ruleContributions.size - visibleContributions} remaining") }
                        }
                    }
                }
                item { OutlinedButton(onClick = onAddLink, modifier = Modifier.fillMaxWidth()) { Text("Add link") } }
                }
                if (section == GoalDetailSection.More) {
                item {
                    listOf("Edit" to onEdit, "Duplicate" to onDuplicate, (if (projection.goal.pinned) "Unpin" else "Pin") to onPin, (if (projection.goal.status == GoalStatus.Paused) "Resume" else "Pause") to onPause, "Complete" to onComplete, "Abandon" to onAbandon, (if (projection.goal.status == GoalStatus.Archived) "Restore" else "Archive") to onArchive).forEach { (label, action) ->
                        TextButton(onClick = action, modifier = Modifier.fillMaxWidth()) { Text(label, modifier = Modifier.fillMaxWidth()) }
                    }
                    TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                        Text("Delete permanently", modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.error)
                    }
                }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

private enum class GoalDetailSection(val label: String) {
    Overview("Overview"),
    History("History"),
    Connections("Connections"),
    More("More"),
}

@Composable
private fun GoalLineChart(values: List<Double>, description: String) {
    val finite = values.filter(Double::isFinite)
    if (finite.size < 2) return
    val min = finite.min()
    val max = finite.max()
    val span = (max - min).takeIf { it > 0.0 } ?: 1.0
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .semantics { contentDescription = description },
    ) {
        val step = size.width / (finite.size - 1).coerceAtLeast(1)
        finite.zipWithNext().forEachIndexed { index, (start, end) ->
            drawLine(
                color = androidx.compose.ui.graphics.Color(0xFFFFC400),
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Override linked contribution") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(contribution.explanation)
                GoalNumberField(value, { value = it }, "Canonical value")
                Text("This changes only this link result. The source event remains intact and explainable.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(enabled = value.toWhipDoubleOrNull() != null, onClick = { onSave(value.toWhipDoubleOrNull()) }) { Text("Save override") } },
        dismissButton = {
            Row {
                if (contribution.overrideValue != null) TextButton(onClick = { onSave(null) }) { Text("Clear override") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
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
    onSave: (LinkRuleDraft, Boolean) -> Unit,
    saving: Boolean = false,
) {
    val goal = projection.goal
    val editorKey = "goal-link-${goal.id}-${initialRule?.id ?: "new"}"
    var name by rememberSaveable(editorKey) { mutableStateOf(initialRule?.name ?: "Link to ${goal.name}") }
    var kind by rememberSaveable(editorKey) { mutableStateOf(initialRule?.kind ?: LinkKind.Contribution) }
    var sourceType by rememberSaveable(editorKey) { mutableStateOf(initialRule?.sourceType ?: LinkSourceType.Habit) }
    var habitId by rememberSaveable(editorKey) { mutableStateOf(initialRule?.sourceEntityId?.takeIf { initialRule.sourceType == LinkSourceType.Habit } ?: state.sourceHabits.firstOrNull()?.id) }
    var taskId by rememberSaveable(editorKey) { mutableStateOf(initialRule?.sourceEntityId?.takeIf { initialRule.sourceType in setOf(LinkSourceType.Task, LinkSourceType.Subtask) } ?: state.sourceTasks.firstOrNull()?.id) }
    var exerciseId by rememberSaveable(editorKey) { mutableStateOf(initialRule?.sourceEntityId?.takeIf { initialRule.sourceType == LinkSourceType.Exercise } ?: state.sourceExercises.firstOrNull()?.id) }
    var sourceGoalMetricId by rememberSaveable(editorKey) { mutableStateOf(initialRule?.sourceMetricId ?: (state.active + state.completed).firstOrNull { it.goal.id != goal.id }?.goal?.metricId) }
    var sourceMetric by rememberSaveable(editorKey) { mutableStateOf(initialRule?.sourceMetric ?: LinkSourceMetric.NumericValue) }
    var sourceStepId by rememberSaveable(editorKey) { mutableStateOf(initialRule?.sourceItemId) }
    var valueMode by rememberSaveable(editorKey) { mutableStateOf(initialRule?.valueMode ?: LinkValueMode.SourceValue) }
    var fixedValue by rememberSaveable(editorKey) { mutableStateOf(initialRule?.fixedValue?.let(::editableNumericValue) ?: "1") }
    var multiplier by rememberSaveable(editorKey) { mutableStateOf(initialRule?.multiplier?.let(::editableNumericValue) ?: "1") }
    var offset by rememberSaveable(editorKey) { mutableStateOf(initialRule?.offset?.let(::editableNumericValue) ?: "0") }
    var includeHistory by rememberSaveable(editorKey) { mutableStateOf(initialRule?.retroactiveFrom != null) }
    var targetMilestoneId by rememberSaveable(editorKey) { mutableStateOf(initialRule?.targetMilestoneId ?: projection.milestones.firstOrNull()?.id) }

    fun availableMetrics(type: LinkSourceType): List<LinkSourceMetric> = when (type) {
        LinkSourceType.Habit -> listOf(LinkSourceMetric.NumericValue, LinkSourceMetric.Success)
        LinkSourceType.Task, LinkSourceType.Subtask -> listOf(LinkSourceMetric.Completion)
        LinkSourceType.Workout -> listOf(LinkSourceMetric.Count, LinkSourceMetric.Duration, LinkSourceMetric.Volume)
        LinkSourceType.Exercise -> listOf(LinkSourceMetric.EstimatedOneRepMax, LinkSourceMetric.MaxWeight, LinkSourceMetric.Distance, LinkSourceMetric.Repetitions, LinkSourceMetric.Duration, LinkSourceMetric.Volume)
        LinkSourceType.Metric -> listOf(LinkSourceMetric.NumericValue)
    }
    fun draft(): LinkRuleDraft = LinkRuleDraft(
        name = name,
        kind = kind,
        sourceType = sourceType,
        sourceEntityId = when (sourceType) {
            LinkSourceType.Habit -> habitId
            LinkSourceType.Task, LinkSourceType.Subtask -> taskId
            LinkSourceType.Exercise -> exerciseId
            LinkSourceType.Workout, LinkSourceType.Metric -> null
        },
        sourceMetricId = sourceGoalMetricId.takeIf { sourceType == LinkSourceType.Metric },
        sourceItemId = sourceStepId.takeIf { sourceType == LinkSourceType.Subtask },
        sourceMetric = sourceMetric,
        targetGoalId = goal.id,
        targetMilestoneId = targetMilestoneId.takeIf { goal.type == GoalType.WeightedMilestones },
        valueMode = valueMode,
        fixedValue = fixedValue.toWhipDoubleOrNull(),
        multiplier = multiplier.toWhipDoubleOrNull() ?: 1.0,
        offset = offset.toWhipDoubleOrNull() ?: 0.0,
        retroactiveFrom = goal.startDate.takeIf { includeHistory },
        enabled = initialRule?.enabled ?: true,
    )
    val sourceAvailable = when (sourceType) {
        LinkSourceType.Habit -> habitId != null
        LinkSourceType.Task, LinkSourceType.Subtask -> taskId != null
        LinkSourceType.Exercise -> exerciseId != null
        LinkSourceType.Metric -> sourceGoalMetricId != null
        LinkSourceType.Workout -> true
    }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(if (initialRule == null) "Link progress to ${goal.name}" else "Edit link to ${goal.name}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("Each matching source event is kept separately, so it can be explained, recalculated, or excluded later.") }
                item { OutlinedTextField(name, { name = it }, label = { Text("Link name") }, modifier = Modifier.fillMaxWidth()) }
                item { GoalEnumDropdown("Link kind", LinkKind.entries, kind, { it.name }) { kind = it } }
                item { GoalEnumDropdown("Source", LinkSourceType.entries, sourceType, { it.name }) { selected ->
                    sourceType = selected
                    sourceMetric = availableMetrics(selected).first()
                    sourceStepId = null
                } }
                when (sourceType) {
                    LinkSourceType.Habit -> item { GoalEnumDropdown("Habit", state.sourceHabits, state.sourceHabits.firstOrNull { it.id == habitId } ?: state.sourceHabits.firstOrNull(), { it?.name ?: "No habits available" }) { habitId = it?.id } }
                    LinkSourceType.Task, LinkSourceType.Subtask -> {
                        item { GoalEnumDropdown("Task", state.sourceTasks, state.sourceTasks.firstOrNull { it.id == taskId } ?: state.sourceTasks.firstOrNull(), { it?.title ?: "No tasks available" }) { taskId = it?.id; sourceStepId = null } }
                        if (sourceType == LinkSourceType.Subtask) {
                            val steps = state.sourceTaskSteps.filter { it.taskId == taskId }
                            item { GoalEnumDropdown("Specific subtask", listOf<Long?>(null) + steps.map { it.id }, sourceStepId, { id -> steps.firstOrNull { it.id == id }?.title ?: "Any subtask" }) { sourceStepId = it } }
                        }
                    }
                    LinkSourceType.Exercise -> item { GoalEnumDropdown("Exercise", state.sourceExercises, state.sourceExercises.firstOrNull { it.id == exerciseId } ?: state.sourceExercises.firstOrNull(), { it?.name ?: "No exercises available" }) { exerciseId = it?.id } }
                    LinkSourceType.Metric -> {
                        val metrics = state.sourceMetrics.filter { it.id != goal.metricId }
                        item {
                            GoalEnumDropdown(
                                "Source measurement",
                                metrics,
                                metrics.firstOrNull { it.id == sourceGoalMetricId } ?: metrics.firstOrNull(),
                                { metric -> metric?.let { if (it.id.startsWith("health-connect-")) "Health Connect · ${it.name}" else it.name } ?: "No measurements available" },
                            ) { sourceGoalMetricId = it?.id }
                        }
                        item { Text("Health Connect records retain provider provenance. Backfill is previewed before it contributes and updates/deletions rebuild idempotently.", style = MaterialTheme.typography.bodySmall) }
                    }
                    LinkSourceType.Workout -> Unit
                }
                item { GoalEnumDropdown("Source value", availableMetrics(sourceType), sourceMetric, { it.name.replace(Regex("([a-z])([A-Z])"), "$1 $2") }) { sourceMetric = it } }
                if (goal.type == GoalType.WeightedMilestones) {
                    item { GoalEnumDropdown("Milestone", projection.milestones, projection.milestones.firstOrNull { it.id == targetMilestoneId } ?: projection.milestones.firstOrNull(), { it?.name ?: "No milestone" }) { targetMilestoneId = it?.id } }
                } else if (kind == LinkKind.Contribution) {
                    item { GoalEnumDropdown("Contribution value", LinkValueMode.entries, valueMode, { if (it == LinkValueMode.SourceValue) "Use source value" else "Use a fixed value" }) { valueMode = it } }
                    if (valueMode == LinkValueMode.FixedValue) item { GoalNumberField(fixedValue, { fixedValue = it }, "Fixed value (${goal.unitId.goalUnitLabel()})") }
                    item { ResponsiveFieldPair(
                        first = { field -> GoalNumberField(multiplier, { multiplier = it }, "Multiplier", field) },
                        second = { field -> GoalNumberField(offset, { offset = it }, "Canonical offset", field) },
                    ) }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = includeHistory, onCheckedChange = { includeHistory = it })
                        Text("Include existing events since ${goal.startDate}", modifier = Modifier.padding(start = 8.dp))
                    }
                }
                if (includeHistory) {
                    item { OutlinedButton(enabled = sourceAvailable, onClick = { onPreview(draft()) }, modifier = Modifier.fillMaxWidth()) { Text("Preview backfill") } }
                    state.backfillPreview?.let { preview ->
                        item { Text("${preview.contributionCount} contributions · ${formatGoalValue(preview.totalCanonicalValue, goal.precision)} canonical total · ${preview.firstDate ?: "—"} to ${preview.lastDate ?: "—"}") }
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = !saving && sourceAvailable && name.isNotBlank(), onClick = { onSave(draft(), includeHistory) }) { Text(if (saving) "Saving…" else if (initialRule == null) "Create link" else "Save link") } },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun <T> GoalEnumDropdown(label: String, values: List<T>, selected: T, text: (T) -> String, onSelect: (T) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column { Text(label, style = MaterialTheme.typography.labelMedium); OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(text(selected)) }; DropdownMenu(expanded, { expanded = false }) { values.forEach { value -> DropdownMenuItem({ Text(text(value)) }, { onSelect(value); expanded = false }) } } }
}

@Composable
private fun GoalNumberField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) = OutlinedTextField(value, onValueChange, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = modifier.fillMaxWidth())

private fun GoalType.label() = name.replace(Regex("([a-z])([A-Z])"), "$1 $2")
private fun GoalType.displayLabel(): String = when (this) {
    GoalType.ReachValue -> "Reach a target"
    GoalType.ReduceValue -> "Reduce a value"
    GoalType.AccumulateTotal -> "Build a total"
    GoalType.MaintainRange -> "Stay in a range"
    GoalType.MeetAverage -> "Meet an average"
    GoalType.Consistency -> "Stay consistent"
    GoalType.WeightedMilestones -> "Finish milestones"
    GoalType.OpenEndedTrend -> "Track a trend"
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
    GoalAggregationPeriod.All -> "All time"
    GoalAggregationPeriod.Day -> "Each day"
    GoalAggregationPeriod.Week -> "Each week"
    GoalAggregationPeriod.Month -> "Each month"
    GoalAggregationPeriod.RollingDays -> "Rolling number of days"
}

private fun GoalPaceType.displayLabel(): String = when (this) {
    GoalPaceType.Linear -> "Steady progress to the deadline"
    GoalPaceType.Milestone -> "Progress follows milestones"
    GoalPaceType.None -> "Do not judge pace"
}

private fun GoalDirection.displayLabel(): String = when (this) {
    GoalDirection.Increase -> "Increase"
    GoalDirection.Decrease -> "Decrease"
    GoalDirection.Neutral -> "No preferred direction"
}
private fun goalUnitDefinitionLabel(unit: UnitDefinition): String =
    "${unit.name}${unit.symbol.takeIf(String::isNotBlank)?.let { " ($it)" }.orEmpty()}"
private fun String.goalUnitLabel() = when (this) { "unitless", "count" -> ""; "kilogram" -> "kg"; "kilogram_rep" -> "kg·rep"; "pound" -> "lb"; "kilometre" -> "km"; "distance_m" -> "m"; "second" -> "sec"; "litre" -> "L"; "millilitre" -> "mL"; "fluid_ounce" -> "fl oz"; "currency" -> "$"; else -> this }
private fun formatGoalValue(value: Double?, precision: Int): String = value?.let { String.format(Locale.getDefault(), "%.${precision.coerceIn(0, 4)}f", it) } ?: "—"
private fun parseGoalClock(value: String): Int? {
    if (value.isBlank()) return null
    val parts = value.split(':')
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
    return if (hour in 0..23 && minute in 0..59) hour * 60 + minute else null
}
