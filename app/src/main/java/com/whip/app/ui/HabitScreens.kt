package com.whip.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
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
import com.whip.app.domain.AvoidMissingPolicy
import com.whip.app.domain.Habit
import com.whip.app.domain.Area
import com.whip.app.domain.HabitChecklistItemDraft
import com.whip.app.domain.HabitDayProgress
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.HabitEndType
import com.whip.app.domain.HabitIntent
import com.whip.app.domain.HabitLog
import com.whip.app.domain.HabitLogStatus
import com.whip.app.domain.HabitScheduleType
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.TargetComparison
import com.whip.app.domain.TargetPeriod
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.MetricDefinition
import com.whip.app.domain.MetricValueKind
import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.LinkSourceMetric
import com.whip.app.domain.GoalProjection
import com.whip.app.domain.TriggerOutcome
import com.whip.app.domain.TriggerRuleDraft
import com.whip.app.domain.TriggerTargetType
import com.whip.app.domain.compactNumericSequence
import com.whip.app.domain.editableNumericValue
import com.whip.app.domain.parseNumericSequence
import com.whip.app.domain.periodBounds
import com.whip.app.domain.isScheduledOn
import com.whip.app.domain.outcomeForPeriod
import com.whip.app.domain.toWhipDoubleOrNull
import com.whip.app.domain.valueInUnit
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private enum class HabitDestination { Today, All, Insights, Connections, Archived }

@Composable
fun HabitAreaContent(
    state: HabitUiState,
    innerPadding: PaddingValues,
    viewModel: HabitViewModel,
    goalState: GoalUiState = GoalUiState(),
    createRequested: Boolean = false,
    onCreateRequestConsumed: () -> Unit = {},
    openHabitIdRequest: Long? = null,
    onOpenHabitRequestConsumed: () -> Unit = {},
    editHabitIdRequest: Long? = null,
    onEditHabitRequestConsumed: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    lowPressureMode: Boolean = false,
    operationStatus: OperationStatus = OperationStatus.Idle,
    onOpenTask: (Long) -> Unit = {},
    dialogModifier: Modifier = Modifier,
    areas: List<Area> = emptyList(),
    defaultAreaId: String? = null,
    onCreateArea: (String, Long?, (Result<String>) -> Unit) -> Unit = { _, _, _ -> },
    areaScopeLabel: String? = null,
    onAreaChanged: (String?) -> Unit = {},
) {
    if (state.loading || state.errorMessage != null) {
        DomainLoadContent("habits", innerPadding, state.errorMessage, viewModel::retryLoading)
        return
    }
    var destination by rememberSaveable { mutableStateOf(HabitDestination.Today) }
    var moreDestinationsOpen by rememberSaveable { mutableStateOf(false) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var editingHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var actionsHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var numericLogHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pauseRequestHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var templatesOpen by rememberSaveable { mutableStateOf(false) }
    var templateDraft by rememberSaveable { mutableStateOf<HabitDraft?>(null) }
    var editorSavePending by rememberSaveable { mutableStateOf(false) }
    var editorSaveStarted by rememberSaveable { mutableStateOf(false) }
    var linkingHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var historicalLogHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingLogHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingLogId by rememberSaveable { mutableStateOf<Long?>(null) }
    var focusedArchivedHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteCandidateHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    val progressById = (state.all + state.today + state.archivedProgress).associateBy { it.habit.id }
    val editing = editingHabitId?.let(progressById::get)
    val actions = actionsHabitId?.let(progressById::get)
    val numericLog = numericLogHabitId?.let(progressById::get)
    val pauseRequest = pauseRequestHabitId?.let(progressById::get)
    val linkingHabit = linkingHabitId?.let(progressById::get)
    val historicalLogHabit = historicalLogHabitId?.let(progressById::get)
    val editingLog = editingLogHabitId?.let(progressById::get)?.let { item ->
        editingLogId?.let { id -> state.logs.firstOrNull { it.id == id } }?.let { item to it }
    }
    val deleteCandidate = deleteCandidateHabitId?.let { id ->
        progressById[id]?.habit ?: state.archived.firstOrNull { it.id == id }
    }
    fun latestPeriodLog(item: HabitDayProgress): HabitLog? = state.logs
        .asSequence()
        .filter { it.habitId == item.habit.id && it.localDate in item.habit.periodBounds(item.date) }
        .filter { it.status in setOf(HabitLogStatus.Recorded, HabitLogStatus.Success, HabitLogStatus.Failed) }
        .maxByOrNull(HabitLog::timestamp)
    LaunchedEffect(createRequested) {
        if (createRequested) {
            creating = true
            onCreateRequestConsumed()
        }
    }
    LaunchedEffect(operationStatus, editorSavePending) {
        if (!editorSavePending) return@LaunchedEffect
        when (operationStatus) {
            is OperationStatus.Running -> editorSaveStarted = true
            is OperationStatus.Succeeded -> {
                creating = false
                editingHabitId = null
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
    LaunchedEffect(openHabitIdRequest, state.all, state.archived) {
        val requestedId = openHabitIdRequest ?: return@LaunchedEffect
        val active = state.all.firstOrNull { it.habit.id == requestedId }
        when {
            active != null -> {
                destination = HabitDestination.All
                actionsHabitId = active.habit.id
                focusedArchivedHabitId = null
                onOpenHabitRequestConsumed()
            }
            state.archived.any { it.id == requestedId } -> {
                destination = HabitDestination.Archived
                actionsHabitId = requestedId
                focusedArchivedHabitId = requestedId
                onOpenHabitRequestConsumed()
            }
        }
    }
    LaunchedEffect(editHabitIdRequest, state.all, state.archivedProgress) {
        val requestedId = editHabitIdRequest ?: return@LaunchedEffect
        val requested = progressById[requestedId]
        if (requested != null) {
            destination = if (requested.habit.archived) HabitDestination.Archived else HabitDestination.All
            editingHabitId = requestedId
            focusedArchivedHabitId = requestedId.takeIf { requested.habit.archived }
            onEditHabitRequestConsumed()
        }
    }
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        val primaryDestinations = listOf(HabitDestination.Today, HabitDestination.All, HabitDestination.Insights)
        ProgressiveDestinationBar(
            selected = destination,
            primary = primaryDestinations,
            secondary = listOf(HabitDestination.Connections, HabitDestination.Archived),
            expanded = moreDestinationsOpen,
            onExpandedChange = { moreDestinationsOpen = it },
            onSelect = { destination = it; focusedArchivedHabitId = null },
            label = HabitDestination::name,
            modifier = Modifier.fillMaxWidth()
                .padding(start = 20.dp, end = 52.dp, top = 8.dp, bottom = 8.dp),
            testTagPrefix = "habit-destination",
        )
        when (destination) {
            HabitDestination.Today -> HabitList(
                title = "Today",
                subtitle = "Check in, log a value, or continue a timer for habits due today.",
                progress = state.today,
                empty = areaScopeLabel?.let { "No habits are due today in $it." } ?: "No habits are due today.",
                onCreate = { creating = true },
                onTemplates = { templatesOpen = true },
                onOpen = { actionsHabitId = it.habit.id },
                onEdit = { editingHabitId = it.habit.id },
                onQuick = { item -> quickHabitAction(item, viewModel) { numericLogHabitId = item.habit.id } },
                onQuickValue = viewModel::addValue,
                onSetValue = { numericLogHabitId = it.habit.id },
                onDecrement = viewModel::decrementValue,
                onUndo = { item -> latestPeriodLog(item)?.let { viewModel.undoLog(it.id, item.habit.id) } },
                canUndo = { latestPeriodLog(it) != null },
                onChecklist = viewModel::toggleChecklist,
                onReorder = null,
                lowPressureMode = lowPressureMode,
            )
            HabitDestination.All -> HabitList(
                title = "All habits",
                subtitle = "Build, limit, avoid, or simply observe anything you define.",
                progress = state.all,
                empty = areaScopeLabel?.let { "No habits in $it. Create one in this area with +." } ?: "Your habit list is empty. Create one from scratch or use a setup template later.",
                onCreate = { creating = true },
                onTemplates = { templatesOpen = true },
                onOpen = { actionsHabitId = it.habit.id },
                onEdit = { editingHabitId = it.habit.id },
                onQuick = { item -> quickHabitAction(item, viewModel) { numericLogHabitId = item.habit.id } },
                onQuickValue = viewModel::addValue,
                onSetValue = { numericLogHabitId = it.habit.id },
                onDecrement = viewModel::decrementValue,
                onUndo = { item -> latestPeriodLog(item)?.let { viewModel.undoLog(it.id, item.habit.id) } },
                canUndo = { latestPeriodLog(it) != null },
                onChecklist = viewModel::toggleChecklist,
                onReorder = viewModel::reorder,
                lowPressureMode = lowPressureMode,
            )
            HabitDestination.Insights -> HabitInsights(state, lowPressureMode)
            HabitDestination.Connections -> HabitAutomationContent(
                state,
                viewModel,
                onOpenHabit = { actionsHabitId = it },
                onOpenTask = onOpenTask,
                onRequestNotificationPermission = onRequestNotificationPermission,
                operationStatus = operationStatus,
            )
            HabitDestination.Archived -> ArchivedHabitList(
                habits = state.archived,
                focusedHabitId = focusedArchivedHabitId,
                onOpen = { actionsHabitId = it.id },
                onEdit = { editingHabitId = it.id },
            )
        }
    }
    if (creating || editing != null) {
        HabitEditorDialog(
            dialogModifier = dialogModifier,
            habit = editing?.habit,
            initialDraft = templateDraft.takeIf { editing == null },
            initialChecklist = editing?.checklistItems?.mapIndexed { index, (item, _) ->
                HabitChecklistItemDraft(item.name, index, item.id, item.uuid)
            }.orEmpty(),
            today = state.currentDate,
            defaultWeekStart = viewModel.defaultSettings().defaultHabitWeekStart,
            defaultAvoidPolicy = viewModel.defaultSettings().defaultAvoidMissingPolicy,
            defaults = viewModel.defaultSettings(),
            customUnits = state.customUnits,
            sourceMetrics = state.sourceMetrics,
            areas = areas,
            defaultAreaId = defaultAreaId,
            onCreateArea = onCreateArea,
            saving = editorSavePending,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onDismiss = {
                creating = false
                editingHabitId = null
                templateDraft = null
                editorSavePending = false
                editorSaveStarted = false
            },
            onSave = { draft ->
                editorSavePending = true
                editorSaveStarted = false
                viewModel.saveHabit(editing?.habit?.id, draft)
                onAreaChanged(draft.areaId)
            },
        )
    }
    actions?.let { item ->
        HabitActionsDialog(
            item,
            dialogModifier = dialogModifier,
            onDismiss = { actionsHabitId = null },
            onEdit = { editingHabitId = item.habit.id; actionsHabitId = null },
            onDuplicate = { viewModel.duplicate(item.habit.id); actionsHabitId = null },
            onPin = { viewModel.setPinned(item.habit.id, !item.habit.pinned); actionsHabitId = null },
            onPause = { viewModel.setPaused(item.habit.id, !item.habit.paused); actionsHabitId = null },
            onSchedulePause = { pauseRequestHabitId = item.habit.id; actionsHabitId = null },
            onSkip = { viewModel.log(item.habit.id, null, HabitLogStatus.Skipped); actionsHabitId = null },
            onExcuse = { viewModel.log(item.habit.id, null, HabitLogStatus.Excused); actionsHabitId = null },
            onMissing = { viewModel.log(item.habit.id, null, HabitLogStatus.Missing); actionsHabitId = null },
            logs = state.logs.filter { it.habitId == item.habit.id },
            onAddHistoricalLog = { historicalLogHabitId = item.habit.id; actionsHabitId = null },
            onEditLog = { log -> editingLogHabitId = item.habit.id; editingLogId = log.id; actionsHabitId = null },
            linkedGoals = goalState.linkRules.filter { it.sourceType == LinkSourceType.Habit && it.sourceEntityId == item.habit.id }
                .mapNotNull { rule -> (goalState.active + goalState.completed + goalState.archived).firstOrNull { it.goal.id == rule.targetGoalId }?.goal?.name },
            onLinkGoal = { linkingHabitId = item.habit.id; actionsHabitId = null },
            onArchive = { viewModel.setArchived(item.habit.id, !item.habit.archived); actionsHabitId = null },
            onDelete = { deleteCandidateHabitId = item.habit.id; actionsHabitId = null },
            lowPressureMode = lowPressureMode,
        )
    }
    deleteCandidate?.let { habit ->
        val logCount = state.logs.count { it.habitId == habit.id }
        val linkCount = goalState.linkRules.count {
            (it.sourceType == LinkSourceType.Habit && it.sourceEntityId == habit.id) || it.sourceMetricId == habit.metricId
        }
        val automationCount = state.triggerRules.count { it.targetType == TriggerTargetType.Habit && it.targetEntityId == habit.id }
        PermanentDeleteDialog(
            title = "Delete ${habit.name} permanently?",
            impacts = listOf(
                "$logCount check-in${if (logCount == 1) "" else "s"}, checklist state, and streak history will be removed",
                "$linkCount goal link${if (linkCount == 1) "" else "s"} and their generated measurements will be removed",
                "$automationCount automation${if (automationCount == 1) "" else "s"} targeting this habit will be removed",
            ),
            onDismiss = { deleteCandidateHabitId = null },
            onConfirm = { viewModel.deletePermanently(habit.id); deleteCandidateHabitId = null },
        )
    }
    numericLog?.let { item ->
        HabitValueDialog(
            item = item,
            onDismiss = { numericLogHabitId = null },
            onLog = { value, note -> viewModel.setPeriodValue(item, value, note); numericLogHabitId = null },
        )
    }
    historicalLogHabit?.let { item ->
        HabitHistoryLogDialog(
            item = item,
            log = null,
            initialDate = state.currentDate,
            onDismiss = { historicalLogHabitId = null },
            onSave = { value, status, date, note ->
                viewModel.log(item.habit.id, value, status, date, note)
                historicalLogHabitId = null
            },
        )
    }
    editingLog?.let { (item, log) ->
        HabitHistoryLogDialog(
            item = item,
            log = log,
            initialDate = log.localDate,
            onDismiss = { editingLogHabitId = null; editingLogId = null },
            onSave = { value, status, date, note ->
                viewModel.updateLog(log.id, value, status, date, note)
                editingLogHabitId = null; editingLogId = null
            },
            onDelete = {
                viewModel.undoLog(log.id, item.habit.id)
                editingLogHabitId = null; editingLogId = null
            },
        )
    }
    pauseRequest?.let { item ->
        HabitPauseDialog(state.currentDate, { pauseRequestHabitId = null }) { start, end, note ->
            viewModel.addPause(item.habit.id, start, end, note)
            pauseRequestHabitId = null
        }
    }
    if (templatesOpen) {
        HabitTemplateDialog(
            today = state.currentDate,
            onDismiss = { templatesOpen = false },
            onChoose = { draft ->
                templateDraft = draft
                creating = true
                templatesOpen = false
            },
        )
    }
    linkingHabit?.let { item ->
        HabitGoalLinkDialog(
            habit = item.habit,
            goals = goalState.active,
            onDismiss = { linkingHabitId = null },
            onSave = { goal, metric, includeHistory ->
                viewModel.linkHabitToGoal(item.habit.id, goal.goal.id, metric, goal.goal.startDate.takeIf { includeHistory })
                linkingHabitId = null
            },
        )
    }
}

@Composable
private fun HabitAutomationContent(
    state: HabitUiState,
    viewModel: HabitViewModel,
    onOpenHabit: (Long) -> Unit,
    onOpenTask: (Long) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    operationStatus: OperationStatus,
) {
    var creating by rememberSaveable { mutableStateOf(false) }
    var editingRuleId by rememberSaveable { mutableStateOf<Long?>(null) }
    var savePending by rememberSaveable { mutableStateOf(false) }
    var saveStarted by rememberSaveable { mutableStateOf(false) }
    val editingRule = editingRuleId?.let { id -> state.triggerRules.firstOrNull { it.id == id } }
    val now = Instant.now()
    val pending = state.triggerOccurrences.filter { it.dismissedAt == null && !it.availableAt.isAfter(now) }
    LaunchedEffect(operationStatus, savePending) {
        if (!savePending) return@LaunchedEffect
        when (operationStatus) {
            is OperationStatus.Running -> saveStarted = true
            is OperationStatus.Succeeded -> {
                creating = false; editingRuleId = null; savePending = false; saveStarted = false
            }
            is OperationStatus.Failed -> { savePending = false; saveStarted = false }
            OperationStatus.Idle -> Unit
        }
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Connections", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Connect outcomes across Tasks, Habits, Goals, and completed workouts without merging their histories.") }
        if (pending.isNotEmpty()) item { Text("Ready now", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(pending, key = { "occurrence-${it.id}" }) { occurrence ->
            val rule = state.triggerRules.firstOrNull { it.id == occurrence.triggerRuleId }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(rule?.name ?: "Next action", fontWeight = FontWeight.Bold)
                    Text(
                        "${rule?.let { triggerSourceLabel(it, state) } ?: "Completed source"} is ready · ${rule?.let { triggerTargetLabel(it, state) } ?: "Next action"}",
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        if (rule != null) {
                            TextButton(onClick = {
                                if (rule.targetType == TriggerTargetType.Habit) onOpenHabit(rule.targetEntityId)
                                else onOpenTask(rule.targetEntityId)
                            }) { Text("Open") }
                            if (rule.targetType == TriggerTargetType.Habit) {
                                TextButton(onClick = { viewModel.doTriggerNow(occurrence.id, rule) }) { Text("Do now") }
                            }
                        }
                        TextButton(onClick = { viewModel.dismissTriggerOccurrence(occurrence.id) }) { Text("Dismiss") }
                    }
                }
            }
        }
        item { Text("Rules", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (state.triggerRules.isEmpty()) item { Text("No automatic connections yet.") }
        items(state.triggerRules, key = { "trigger-${it.id}" }) { rule ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(rule.name, fontWeight = FontWeight.Bold)
                    Text(
                        buildString {
                            append("${triggerSourceLabel(rule, state)} → ${triggerTargetLabel(rule, state)} · ${rule.delayMinutes} min delay")
                            if (rule.autoCompleteTargetHabit || (rule.sourceType == LinkSourceType.Workout && rule.targetType == TriggerTargetType.Habit)) append(" · auto check-in")
                            if (!rule.enabled) append(" · Paused")
                        },
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { viewModel.setTriggerEnabled(rule, !rule.enabled) }) { Text(if (rule.enabled) "Pause" else "Resume") }
                        TextButton(onClick = { editingRuleId = rule.id }) { Text("Edit") }
                        TextButton(onClick = { viewModel.deleteTrigger(rule.id) }) { Text("Remove") }
                    }
                }
            }
        }
        item { Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) { Text("Create connection") } }
    }
    if (creating || editingRule != null) HabitAutomationDialog(
        state = state,
        initialRule = editingRule,
        saving = savePending,
        onDismiss = { if (!savePending) { creating = false; editingRuleId = null } },
        onSave = { draft ->
            onRequestNotificationPermission()
            savePending = true
            saveStarted = false
            if (editingRule == null) viewModel.createTrigger(draft) else viewModel.updateTrigger(editingRule.id, draft)
        },
    )
}

private fun triggerSourceLabel(rule: com.whip.app.domain.TriggerRule, state: HabitUiState): String = when (rule.sourceType) {
    LinkSourceType.Habit -> (state.all + state.today).firstOrNull { it.habit.id == rule.sourceEntityId }?.habit?.name ?: "Habit"
    LinkSourceType.Task, LinkSourceType.Subtask -> state.sourceTasks.firstOrNull { it.id == rule.sourceEntityId }?.title ?: "Task"
    LinkSourceType.Workout -> "Completed workout"
    LinkSourceType.Exercise -> "Exercise result"
    LinkSourceType.Metric -> "Measurement"
}

private fun triggerTargetLabel(rule: com.whip.app.domain.TriggerRule, state: HabitUiState): String = when (rule.targetType) {
    TriggerTargetType.Habit -> (state.all + state.today).firstOrNull { it.habit.id == rule.targetEntityId }?.habit?.name ?: "Habit"
    TriggerTargetType.Task -> state.sourceTasks.firstOrNull { it.id == rule.targetEntityId }?.title ?: "Task"
}

@Composable
private fun HabitAutomationDialog(
    state: HabitUiState,
    initialRule: com.whip.app.domain.TriggerRule? = null,
    onDismiss: () -> Unit,
    onSave: (TriggerRuleDraft) -> Unit,
    saving: Boolean = false,
) {
    val habits = state.all.map(HabitDayProgress::habit)
    val editorKey = "automation-${initialRule?.id ?: "new"}"
    var name by rememberSaveable(editorKey) { mutableStateOf(initialRule?.name.orEmpty()) }
    var sourceType by rememberSaveable(editorKey) { mutableStateOf(initialRule?.sourceType ?: LinkSourceType.Habit) }
    var sourceHabitId by rememberSaveable(editorKey) { mutableStateOf(initialRule?.sourceEntityId?.takeIf { initialRule.sourceType == LinkSourceType.Habit } ?: habits.firstOrNull()?.id) }
    var sourceTaskId by rememberSaveable(editorKey) { mutableStateOf(initialRule?.sourceEntityId?.takeIf { initialRule.sourceType == LinkSourceType.Task } ?: state.sourceTasks.firstOrNull()?.id) }
    var targetType by rememberSaveable(editorKey) { mutableStateOf(initialRule?.targetType ?: TriggerTargetType.Habit) }
    var targetHabitId by rememberSaveable(editorKey) { mutableStateOf(initialRule?.targetEntityId?.takeIf { initialRule.targetType == TriggerTargetType.Habit } ?: habits.firstOrNull()?.id) }
    var targetTaskId by rememberSaveable(editorKey) { mutableStateOf(initialRule?.targetEntityId?.takeIf { initialRule.targetType == TriggerTargetType.Task } ?: state.sourceTasks.firstOrNull()?.id) }
    var outcome by rememberSaveable(editorKey) { mutableStateOf(initialRule?.outcome ?: TriggerOutcome.Completed) }
    var delay by rememberSaveable(editorKey) { mutableStateOf((initialRule?.delayMinutes ?: 0).toString()) }
    var quietStart by rememberSaveable(editorKey) { mutableStateOf(initialRule?.quietStartMinutes?.let { "%02d:%02d".format(it / 60, it % 60) } ?: "22:00") }
    var quietEnd by rememberSaveable(editorKey) { mutableStateOf(initialRule?.quietEndMinutes?.let { "%02d:%02d".format(it / 60, it % 60) } ?: "07:00") }
    val sourceId = when (sourceType) { LinkSourceType.Habit -> sourceHabitId; LinkSourceType.Task -> sourceTaskId; LinkSourceType.Workout -> 0L; else -> null }
    val targetId = if (targetType == TriggerTargetType.Habit) targetHabitId else targetTaskId
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(if (initialRule == null) "Create connection" else "Edit connection") },
        text = { LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth()) }
            item { EnumDropdown("Source type", listOf(LinkSourceType.Habit, LinkSourceType.Task, LinkSourceType.Workout), sourceType, { it.name }) { selected -> sourceType = selected; if (selected == LinkSourceType.Workout) outcome = TriggerOutcome.Completed } }
            if (sourceType == LinkSourceType.Habit && habits.isNotEmpty()) item { EnumDropdown("Source habit", habits, habits.first { it.id == sourceHabitId }, { it.name }) { sourceHabitId = it.id } }
            if (sourceType == LinkSourceType.Task && state.sourceTasks.isNotEmpty()) item { EnumDropdown("Source task", state.sourceTasks, state.sourceTasks.first { it.id == sourceTaskId }, { it.title }) { sourceTaskId = it.id } }
            item { EnumDropdown("Outcome", if (sourceType == LinkSourceType.Workout) listOf(TriggerOutcome.Completed) else TriggerOutcome.entries, outcome, { it.name }) { outcome = it } }
            item { EnumDropdown("Target type", TriggerTargetType.entries, targetType, { it.name }) { targetType = it } }
            if (targetType == TriggerTargetType.Habit && habits.isNotEmpty()) item { EnumDropdown("Target habit", habits, habits.first { it.id == targetHabitId }, { it.name }) { targetHabitId = it.id } }
            if (targetType == TriggerTargetType.Task && state.sourceTasks.isNotEmpty()) item { EnumDropdown("Target task", state.sourceTasks, state.sourceTasks.first { it.id == targetTaskId }, { it.title }) { targetTaskId = it.id } }
            item { NumberTextField(delay, { delay = it }, "Delay minutes") }
            item { ResponsiveFieldPair(
                first = { field -> ClockPickerButton("Quiet hours start", parseClockMinutes(quietStart), { quietStart = it?.let(::formatClockMinutes).orEmpty() }, field) },
                second = { field -> ClockPickerButton("Quiet hours end", parseClockMinutes(quietEnd), { quietEnd = it?.let(::formatClockMinutes).orEmpty() }, field) },
            ) }
            if (sourceType == LinkSourceType.Workout && targetType == TriggerTargetType.Habit) {
                item { Text("Each completed workout automatically adds one completion to the target habit.") }
            }
        } },
        confirmButton = { TextButton(enabled = !saving && name.isNotBlank() && sourceId != null && targetId != null, onClick = { onSave(TriggerRuleDraft(name, sourceType, requireNotNull(sourceId), outcome, targetType, requireNotNull(targetId), delay.toIntOrNull() ?: 0, parseClockMinutes(quietStart), parseClockMinutes(quietEnd), sourceType == LinkSourceType.Workout && targetType == TriggerTargetType.Habit, initialRule?.enabled ?: true)) }) { Text(if (saving) "Saving…" else if (initialRule == null) "Create" else "Save") } },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun HabitProgressCard(
    item: HabitDayProgress,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onQuick: () -> Unit,
    onQuickValue: (Double) -> Unit = { onQuick() },
    onSetValue: () -> Unit = onQuick,
    onDecrement: () -> Unit = {},
    onUndo: () -> Unit = {},
    canUndo: Boolean = false,
    onChecklist: (Long, Long, LocalDate, Boolean) -> Unit,
    lowPressureMode: Boolean = false,
) {
    val habit = item.habit
    var showAllQuickValues by rememberSaveable(habit.id) { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Open habit details for ${habit.name}", onClick = onOpen)
            .testTag("habit-card-${habit.id}")
            .semantics { contentDescription = "Open habit details for ${habit.name}" },
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(habit.icon, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(habit.name, fontWeight = FontWeight.Bold)
                    if (habit.areaId != null) AreaBadge(habit.areaId, habit.area)
                    val streakUnit = when (habit.scheduleType) {
                        HabitScheduleType.FlexibleTimesPerWeek -> "week"
                        HabitScheduleType.FlexibleTimesPerMonth -> "month"
                        else -> "day"
                    }
                    val rateWindow = if (habit.scheduleType in setOf(
                            HabitScheduleType.FlexibleTimesPerWeek,
                            HabitScheduleType.FlexibleTimesPerMonth,
                        )
                    ) "recent periods" else "30d"
                    Text(
                        if (lowPressureMode) {
                            "${habit.trackingMode.label()} · ${(item.completionRate * 100).toInt()}% / $rateWindow"
                        } else {
                            "${habit.trackingMode.label()} · ${item.streak} $streakUnit streak · ${(item.completionRate * 100).toInt()}% / $rateWindow"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (habit.sourceMetricId != null) {
                    Text("Synced", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                } else when (habit.trackingMode) {
                    HabitTrackingMode.CheckOff -> Checkbox(checked = item.successful == true, onCheckedChange = { onQuick() })
                    HabitTrackingMode.Duration -> Button(onClick = onQuick) { Text(if (habit.timerStartedAtMillis == null) "Start" else "Stop") }
                    HabitTrackingMode.Count, HabitTrackingMode.Decimal, HabitTrackingMode.LimitAvoid ->
                        OutlinedButton(onClick = onSetValue) { Text("Set") }
                    else -> Button(onClick = onQuick) {
                        if (habit.trackingMode == HabitTrackingMode.Rating) Text("Rate")
                        else Icon(Icons.Filled.Add, contentDescription = "Log ${habit.name}", modifier = Modifier.size(24.dp))
                    }
                }
                ItemEditButton("habit", habit.name, onEdit)
            }
            if (habit.sourceMetricId == null && habit.trackingMode in setOf(HabitTrackingMode.Count, HabitTrackingMode.Decimal, HabitTrackingMode.LimitAvoid)) {
                val quickValues = (listOf(habit.quickIncrement) + habit.quickActions)
                    .filter { it.isFinite() && it > 0.0 }
                    .distinct()
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    (if (showAllQuickValues) quickValues else quickValues.take(3)).forEach { value ->
                        Button(onClick = { onQuickValue(value) }) { Text("+${editableNumericValue(value)}") }
                    }
                    if (quickValues.size > 3) {
                        TextButton(onClick = { showAllQuickValues = !showAllQuickValues }) {
                            Text(if (showAllQuickValues) "Fewer" else "More values")
                        }
                    }
                    OutlinedButton(enabled = item.value > 0.0, onClick = onDecrement) {
                        Text("−${editableNumericValue(minOf(habit.quickIncrement, item.value.coerceAtLeast(0.0)))}")
                    }
                    OutlinedButton(onClick = onSetValue) { Text("Set") }
                    TextButton(enabled = canUndo, onClick = onUndo) { Text("Undo") }
                }
            }
            if (habit.trackingMode == HabitTrackingMode.Checklist) {
                item.checklistItems.forEach { (checklistItem, completed) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(onClickLabel = "Toggle ${checklistItem.name}") {
                            onChecklist(habit.id, checklistItem.id, item.date, !completed)
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = completed, onCheckedChange = { checked -> onChecklist(habit.id, checklistItem.id, item.date, checked) })
                        Text(checklistItem.name)
                    }
                }
            }
            if (item.flexibleScheduleTarget != null && item.flexibleScheduleProgress != null) {
                val target = item.flexibleScheduleTarget
                val fraction = (item.flexibleScheduleProgress.toFloat() / target).coerceIn(0f, 1f)
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                Text(
                    "${item.flexibleScheduleProgress} / $target completions this ${if (habit.scheduleType == HabitScheduleType.FlexibleTimesPerWeek) "week" else "month"}",
                    style = MaterialTheme.typography.labelMedium,
                )
            } else if (habit.comparison != TargetComparison.None) {
                val target = habit.targetMax ?: habit.targetMin ?: 1.0
                val fraction = if (target == 0.0) 0f else (item.value / target).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                Text(
                    "${formatHabitValue(item.value, habit.precision)} / ${formatHabitValue(target, habit.precision)} ${habit.unitId.unitLabel()}",
                    style = MaterialTheme.typography.labelMedium,
                )
            } else if (item.value != 0.0) {
                Text("${formatHabitValue(item.value, habit.precision)} ${habit.unitId.unitLabel()}")
            }
            if (habit.sourceMetricId != null) {
                Text("Read-only source: Health Connect · provenance is retained per entry", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

fun quickHabitAction(item: HabitDayProgress, vm: HabitViewModel, openNumeric: () -> Unit) {
    if (item.habit.sourceMetricId != null) return
    when (item.habit.trackingMode) {
        HabitTrackingMode.CheckOff -> vm.setCheckOff(item.habit.id, item.date, item.successful != true)
        HabitTrackingMode.Count, HabitTrackingMode.Decimal, HabitTrackingMode.LimitAvoid -> vm.log(item.habit.id, item.habit.quickIncrement)
        HabitTrackingMode.Duration -> if (item.habit.timerStartedAtMillis == null) vm.startTimer(item.habit.id) else vm.stopTimer(item.habit.id)
        HabitTrackingMode.Checklist -> Unit
        HabitTrackingMode.Rating, HabitTrackingMode.LogOnly -> openNumeric()
    }
}

@Composable
private fun HabitList(
    title: String,
    subtitle: String,
    progress: List<HabitDayProgress>,
    empty: String,
    onCreate: () -> Unit,
    onTemplates: () -> Unit,
    onOpen: (HabitDayProgress) -> Unit,
    onEdit: (HabitDayProgress) -> Unit,
    onQuick: (HabitDayProgress) -> Unit,
    onQuickValue: (HabitDayProgress, Double) -> Unit,
    onSetValue: (HabitDayProgress) -> Unit,
    onDecrement: (HabitDayProgress) -> Unit,
    onUndo: (HabitDayProgress) -> Unit,
    canUndo: (HabitDayProgress) -> Boolean,
    onChecklist: (Long, Long, LocalDate, Boolean) -> Unit,
    onReorder: ((List<Long>) -> Unit)?,
    lowPressureMode: Boolean,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var manageOrder by rememberSaveable { mutableStateOf(false) }
    val visible = progress.filter { item ->
        query.isBlank() || listOf(
            item.habit.name,
            item.habit.notes,
            item.habit.area,
            item.habit.tags.joinToString(" "),
        ).any { it.contains(query, ignoreCase = true) }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text(subtitle) }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { searchOpen = !searchOpen }) { Text(if (searchOpen) "Hide search" else "Search") }
                OutlinedButton(onClick = onTemplates) { Text("Templates") }
                if (onReorder != null && progress.size > 1) {
                    OutlinedButton(onClick = { manageOrder = !manageOrder }) { Text(if (manageOrder) "Finish ordering" else "Manage order") }
                }
            }
        }
        if (searchOpen || query.isNotBlank()) item { OutlinedTextField(query, { query = it }, label = { Text("Search habits, areas, or tags") }, modifier = Modifier.fillMaxWidth()) }
        if (visible.isEmpty()) item {
            Column(Modifier.padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (progress.isEmpty()) empty else "No habits match this search.")
                if (progress.isEmpty()) Text("Use the + button to create one, or start with a template.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(visible, key = { it.habit.id }) { item ->
            val index = progress.indexOfFirst { it.habit.id == item.habit.id }
            Column {
                HabitProgressCard(
                    item = item,
                    onOpen = { onOpen(item) },
                    onEdit = { onEdit(item) },
                    onQuick = { onQuick(item) },
                    onQuickValue = { value -> onQuickValue(item, value) },
                    onSetValue = { onSetValue(item) },
                    onDecrement = { onDecrement(item) },
                    onUndo = { onUndo(item) },
                    canUndo = canUndo(item),
                    onChecklist = onChecklist,
                    lowPressureMode = lowPressureMode,
                )
                if (manageOrder && onReorder != null && query.isBlank()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(enabled = index > 0, onClick = {
                            val ids = progress.map { it.habit.id }.toMutableList()
                            java.util.Collections.swap(ids, index, index - 1)
                            onReorder(ids)
                        }) { Icon(Icons.Outlined.ArrowUpward, contentDescription = null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(4.dp)); Text("Earlier") }
                        TextButton(enabled = index in 0 until progress.lastIndex, onClick = {
                            val ids = progress.map { it.habit.id }.toMutableList()
                            java.util.Collections.swap(ids, index, index + 1)
                            onReorder(ids)
                        }) { Icon(Icons.Outlined.ArrowDownward, contentDescription = null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(4.dp)); Text("Later") }
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitInsights(state: HabitUiState, lowPressureMode: Boolean) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Text("Habit insights", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        items(state.all, key = { it.habit.id }) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(item.habit.name, fontWeight = FontWeight.Bold)
                    if (!lowPressureMode) Text("Current streak: ${item.streak}")
                    Text("30-day completion: ${(item.completionRate * 100).toInt()}%")
                    val total = state.logs.asSequence()
                        .filter { it.habitId == item.habit.id }
                        .mapNotNull { it.valueInUnit(item.habit.unitId, state.customUnits) }
                        .sum()
                    Text(
                        "All-time logged total: ${formatHabitValue(total, item.habit.precision)} ${item.habit.unitId.unitLabel()}",
                    )
                    val habitLogs = state.logs.filter { it.habitId == item.habit.id }
                    val weeklyRates = (7L downTo 0L).map { weeksAgo ->
                        val start = state.currentDate.minusWeeks(weeksAgo)
                            .with(TemporalAdjusters.previousOrSame(item.habit.weekStart))
                        val end = minOf(start.plusDays(6), state.currentDate)
                        val scheduled = generateSequence(start) { it.plusDays(1) }
                            .takeWhile { !it.isAfter(end) }
                            .filter(item.habit::isScheduledOn)
                            .toList()
                        val outcomes = scheduled.mapNotNull { day -> item.habit.outcomeForPeriod(habitLogs, day, state.customUnits) }
                        if (outcomes.isEmpty()) null else outcomes.count { it }.toDouble() / outcomes.size
                    }
                    Text("Eight-week consistency", style = MaterialTheme.typography.labelMedium)
                    HabitRateChart(item.habit.name, weeklyRates)
                    val recent = weeklyRates.lastOrNull()
                    val previous = weeklyRates.dropLast(1).lastOrNull()
                    Text(
                        when {
                            recent == null -> "No completed target periods this week yet."
                            previous == null -> "This week: ${(recent * 100).toInt()}%"
                            else -> "This week: ${(recent * 100).toInt()}% · ${if (recent >= previous) "up" else "down"} ${kotlin.math.abs((recent - previous) * 100).toInt()} points"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    val recorded = habitLogs.groupBy { it.localDate }
                    val days = (27L downTo 0L).map { state.currentDate.minusDays(it) }
                    Text("Recent activity", style = MaterialTheme.typography.labelMedium)
                    val calendarDescription = days.joinToString("; ") { day ->
                        val logs = recorded[day].orEmpty()
                        "$day: " + when {
                            logs.any { it.status == HabitLogStatus.Excused || it.status == HabitLogStatus.Skipped } -> "skipped or excused"
                            logs.any { (it.value ?: 0.0) > 0.0 || it.status == HabitLogStatus.Success } -> "positive or successful"
                            logs.isNotEmpty() -> "zero logged"
                            else -> "no entry"
                        }
                    }
                    Text(
                        days.chunked(7).joinToString(" ") { week -> week.joinToString("") { day ->
                            val logs = recorded[day].orEmpty()
                            when {
                                logs.any { it.status == HabitLogStatus.Excused || it.status == HabitLogStatus.Skipped } -> "○"
                                logs.any { (it.value ?: 0.0) > 0.0 || it.status == HabitLogStatus.Success } -> "■"
                                logs.isNotEmpty() -> "□"
                                else -> "·"
                            }
                        } },
                        modifier = Modifier.semantics { contentDescription = calendarDescription },
                    )
                    Text("■ success · □ recorded below target · ○ skipped/excused · · no entry", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun HabitRateChart(name: String, rates: List<Double?>) {
    val values = rates.map { it?.coerceIn(0.0, 1.0) }
    val description = values.mapIndexed { index, value ->
        "week ${index + 1}: ${value?.let { "${(it * 100).toInt()} percent" } ?: "no data"}"
    }.joinToString("; ")
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .semantics { contentDescription = "$name eight-week consistency chart; $description" },
    ) {
        if (values.size < 2) return@Canvas
        val step = size.width / (values.size - 1)
        values.zipWithNext().forEachIndexed { index, (start, end) ->
            if (start != null && end != null) {
                drawLine(
                    color = androidx.compose.ui.graphics.Color(0xFFFFC400),
                    start = androidx.compose.ui.geometry.Offset(index * step, size.height - start.toFloat() * size.height),
                    end = androidx.compose.ui.geometry.Offset((index + 1) * step, size.height - end.toFloat() * size.height),
                    strokeWidth = 4.dp.toPx(),
                )
            }
        }
    }
}

@Composable
private fun ArchivedHabitList(
    habits: List<Habit>,
    focusedHabitId: Long? = null,
    onOpen: (Habit) -> Unit,
    onEdit: (Habit) -> Unit,
) {
    val visible = habits.filter { focusedHabitId == null || it.id == focusedHabitId }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 96.dp)) {
        item { Text("Archived habits", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        if (visible.isEmpty()) item { Text("No archived habits.") }
        items(visible, key = Habit::id) { habit ->
            Card(
                modifier = Modifier.fillMaxWidth()
                    .clickable(onClickLabel = "Open habit details for ${habit.name}") { onOpen(habit) }
                    .semantics { contentDescription = "Open habit details for ${habit.name}" },
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(habit.name, fontWeight = FontWeight.Bold)
                        if (habit.areaId != null) AreaBadge(habit.areaId, habit.area)
                        Text("Archived", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    ItemEditButton("habit", habit.name, onEdit = { onEdit(habit) })
                }
            }
        }
    }
}

@Composable
private fun HabitTemplateDialog(
    today: LocalDate,
    onDismiss: () -> Unit,
    onChoose: (HabitDraft) -> Unit,
) {
    val templates = listOf(
        "Hydration" to HabitDraft(
            name = "Hydration", icon = "◉", trackingMode = HabitTrackingMode.Count,
            dimension = UnitDimension.Count, unitId = "glass", targetMin = 8.0,
            quickIncrement = 1.0, quickActions = listOf(1.0, 2.0), startDate = today,
        ),
        "Medication" to HabitDraft(name = "Medication", icon = "✚", trackingMode = HabitTrackingMode.CheckOff, startDate = today),
        "Reading" to HabitDraft(
            name = "Reading", icon = "▤", trackingMode = HabitTrackingMode.Count,
            dimension = UnitDimension.Count, unitId = "page", targetMin = 20.0,
            quickIncrement = 5.0, quickActions = listOf(5.0, 10.0, 20.0), startDate = today,
        ),
        "Meditation" to HabitDraft(
            name = "Meditation", icon = "○", trackingMode = HabitTrackingMode.Duration,
            dimension = UnitDimension.Duration, unitId = "minute", targetMin = 10.0,
            quickIncrement = 5.0, startDate = today,
        ),
        "No-spend day" to HabitDraft(
            name = "No-spend day", icon = "\$", intent = HabitIntent.Avoid,
            trackingMode = HabitTrackingMode.LimitAvoid, comparison = TargetComparison.AtMost,
            targetMin = 0.0, avoidMissingPolicy = AvoidMissingPolicy.Success, startDate = today,
        ),
        "Exercise 3× weekly" to HabitDraft(
            name = "Exercise", icon = "◆", trackingMode = HabitTrackingMode.CheckOff,
            scheduleType = HabitScheduleType.FlexibleTimesPerWeek, flexibleTimesPerWeek = 3,
            targetPeriod = TargetPeriod.Week, targetMin = 3.0, startDate = today,
        ),
        "Daily rating" to HabitDraft(
            name = "Daily rating", icon = "★", intent = HabitIntent.Observe,
            trackingMode = HabitTrackingMode.Rating, comparison = TargetComparison.None,
            dimension = UnitDimension.Unitless, unitId = "unitless", targetMin = null,
            precision = 0, startDate = today,
        ),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Habit templates") },
        text = {
            LazyColumn {
                item { Text("Templates create ordinary habits that you can fully edit afterward.") }
                items(templates, key = { it.first }) { (label, draft) ->
                    TextButton(onClick = { onChoose(draft) }, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(label, fontWeight = FontWeight.SemiBold)
                            Text(habitTemplateDescription(label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun habitTemplateDescription(label: String): String = when (label) {
    "Hydration" -> "Count toward 8 glasses per day with +1 and +2 buttons."
    "Medication" -> "A simple daily done/not-done check-in."
    "Reading" -> "Add pages toward a daily target of 20."
    "Meditation" -> "Track minutes toward a daily duration target."
    "No-spend day" -> "Succeed when spending stays at zero."
    "Exercise 3× weekly" -> "Complete any three sessions during the week."
    "Daily rating" -> "Record a value without success, failure, or streak pressure."
    else -> "Prefills an editable habit; nothing is saved until you confirm."
}

private fun Habit.toEditorDraft(checklist: List<HabitChecklistItemDraft>) = HabitDraft(
    name = name,
    notes = notes,
    areaId = areaId,
    area = area,
    tags = tags,
    icon = icon,
    colorArgb = colorArgb,
    intent = intent,
    trackingMode = trackingMode,
    dimension = dimension,
    unitId = unitId,
    precision = precision,
    comparison = comparison,
    targetMin = targetMin,
    targetMax = targetMax,
    targetPeriod = targetPeriod,
    rollingDays = rollingDays,
    scheduleType = scheduleType,
    scheduleInterval = scheduleInterval,
    weekdays = weekdays,
    flexibleTimesPerWeek = flexibleTimesPerWeek,
    startDate = startDate,
    endType = endType,
    endDate = endDate,
    endValue = endValue,
    timeWindowStartMinutes = timeWindowStartMinutes,
    timeWindowEndMinutes = timeWindowEndMinutes,
    quickIncrement = quickIncrement,
    quickActions = quickActions,
    reminderMinutes = reminderMinutes,
    weekdayReminderMinutes = weekdayReminderMinutes,
    weekStart = weekStart,
    avoidMissingPolicy = avoidMissingPolicy,
    checklistItems = checklist,
    sourceMetricId = sourceMetricId,
)

@Composable
private fun HabitEditorDialog(
    habit: Habit?,
    initialDraft: HabitDraft? = null,
    initialChecklist: List<HabitChecklistItemDraft>,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (HabitDraft) -> Unit,
    defaultWeekStart: DayOfWeek = DayOfWeek.MONDAY,
    defaultAvoidPolicy: AvoidMissingPolicy = AvoidMissingPolicy.Unknown,
    defaults: AppSettings = AppSettings(),
    customUnits: List<UnitDefinition> = emptyList(),
    sourceMetrics: List<MetricDefinition> = emptyList(),
    onRequestNotificationPermission: () -> Unit = {},
    saving: Boolean = false,
    dialogModifier: Modifier = Modifier,
    areas: List<Area> = emptyList(),
    defaultAreaId: String? = null,
    onCreateArea: (String, Long?, (Result<String>) -> Unit) -> Unit = { _, _, _ -> },
) {
    val baseInitial = initialDraft ?: habit?.toEditorDraft(initialChecklist) ?: HabitDraft(
        name = "",
        startDate = today,
        weekStart = defaultWeekStart,
        avoidMissingPolicy = defaultAvoidPolicy,
        precision = 0,
        targetPeriod = TargetPeriod.Occurrence,
        areaId = defaultAreaId,
        area = areas.firstOrNull { it.id == defaultAreaId }?.name.orEmpty(),
    )
    val initial = if (habit == null && baseInitial.areaId == null && defaultAreaId != null) {
        baseInitial.copy(
            areaId = defaultAreaId,
            area = areas.firstOrNull { it.id == defaultAreaId }?.name.orEmpty(),
        )
    } else baseInitial
    val editorKey = "habit-${habit?.id ?: initialDraft?.hashCode() ?: "new"}"
    var name by rememberSaveable(editorKey) { mutableStateOf(initial.name) }
    var notes by rememberSaveable(editorKey) { mutableStateOf(initial.notes) }
    var areaId by rememberSaveable(editorKey) { mutableStateOf(initial.areaId) }
    var area by rememberSaveable(editorKey) { mutableStateOf(initial.area) }
    var tags by rememberSaveable(editorKey) { mutableStateOf(initial.tags.joinToString(",")) }
    var icon by rememberSaveable(editorKey) { mutableStateOf(initial.icon) }
    var colorHex by rememberSaveable(editorKey) { mutableStateOf(colorArgbToHex(initial.colorArgb)) }
    var intent by rememberSaveable(editorKey) { mutableStateOf(initial.intent) }
    var mode by rememberSaveable(editorKey) { mutableStateOf(initial.trackingMode) }
    var comparison by rememberSaveable(editorKey) { mutableStateOf(initial.comparison) }
    var targetMin by rememberSaveable(editorKey) { mutableStateOf(initial.targetMin?.let(::editableNumericValue) ?: "1") }
    var targetMax by rememberSaveable(editorKey) { mutableStateOf(initial.targetMax?.let(::editableNumericValue).orEmpty()) }
    var targetPeriod by rememberSaveable(editorKey) { mutableStateOf(initial.targetPeriod) }
    var schedule by rememberSaveable(editorKey) { mutableStateOf(initial.scheduleType) }
    var interval by rememberSaveable(editorKey) { mutableStateOf(initial.scheduleInterval.toString()) }
    var weekdays by rememberSaveable(editorKey) { mutableStateOf(initial.weekdays) }
    var flexible by rememberSaveable(editorKey) { mutableStateOf(initial.flexibleTimesPerWeek?.toString() ?: "3") }
    var rollingDays by rememberSaveable(editorKey) { mutableStateOf(initial.rollingDays?.toString() ?: "7") }
    var quickIncrement by rememberSaveable(editorKey) { mutableStateOf(initial.quickIncrement.let(::editableNumericValue)) }
    var quickActions by rememberSaveable(editorKey) {
        mutableStateOf(
            initial.quickActions.let { values ->
                val compact = compactNumericSequence(values)
                if (compact.increment == initial.quickIncrement) compact.specification
                else values.joinToString(",", transform = ::editableNumericValue)
            },
        )
    }
    var reminders by rememberSaveable(editorKey) { mutableStateOf(initial.reminderMinutes.joinToString(",") { minutes -> "%02d:%02d".format(minutes / 60, minutes % 60) }) }
    var weekdayReminders by rememberSaveable(editorKey) {
        mutableStateOf(
            initial.weekdayReminderMinutes.entries.sortedBy { it.key.value }.joinToString(";") { (day, times) ->
                "${day.name.take(3)}=${times.joinToString(",") { minutes -> "%02d:%02d".format(minutes / 60, minutes % 60) }}"
            },
        )
    }
    var windowStart by rememberSaveable(editorKey) { mutableStateOf(initial.timeWindowStartMinutes?.let { "%02d:%02d".format(it / 60, it % 60) }.orEmpty()) }
    var windowEnd by rememberSaveable(editorKey) { mutableStateOf(initial.timeWindowEndMinutes?.let { "%02d:%02d".format(it / 60, it % 60) }.orEmpty()) }
    var endType by rememberSaveable(editorKey) { mutableStateOf(initial.endType) }
    var endDate by rememberSaveable(editorKey) { mutableStateOf(initial.endDate) }
    var endValue by rememberSaveable(editorKey) { mutableStateOf(initial.endValue?.let(::editableNumericValue).orEmpty()) }
    var weekStart by rememberSaveable(editorKey) { mutableStateOf(initial.weekStart) }
    var showEndDatePicker by rememberSaveable(editorKey) { mutableStateOf(false) }
    var unitId by rememberSaveable(editorKey) { mutableStateOf(initial.unitId) }
    var dimension by rememberSaveable(editorKey) { mutableStateOf(initial.dimension) }
    var precision by rememberSaveable(editorKey) { mutableStateOf(initial.precision.toString()) }
    var avoidPolicy by rememberSaveable(editorKey) { mutableStateOf(initial.avoidMissingPolicy) }
    var sourceMetricId by rememberSaveable(editorKey) { mutableStateOf(initial.sourceMetricId) }
    var checklistDrafts by rememberSaveable(editorKey) {
        mutableStateOf(ArrayList(initial.checklistItems))
    }
    val quickActionResult = parseNumericSequence(
        specification = quickActions,
        rangeIncrement = quickIncrement.toWhipDoubleOrNull(),
        maximumValues = 24,
    )
    val quickIncrementValid = quickIncrement.toWhipDoubleOrNull()?.let { it.isFinite() && it > 0.0 } == true
    var showAdvanced by rememberSaveable(editorKey) {
        mutableStateOf(
            defaults.powerMode || initial.notes.isNotBlank() || initial.tags.isNotEmpty() ||
                initial.colorArgb != null || initial.quickActions.isNotEmpty() ||
                initial.reminderMinutes.isNotEmpty() || initial.weekdayReminderMinutes.isNotEmpty() ||
                initial.timeWindowStartMinutes != null || initial.timeWindowEndMinutes != null ||
                initial.endType != HabitEndType.Never,
        )
    }
    val editorFingerprint = listOf(
        name, notes, areaId, area, tags, icon, colorHex, intent, mode, comparison, targetMin, targetMax,
        targetPeriod, schedule, interval, weekdays.sortedBy { it.value }, flexible, rollingDays,
        quickIncrement, quickActions, reminders, weekdayReminders, windowStart, windowEnd, endType,
        endDate, endValue, weekStart, unitId, dimension, precision, avoidPolicy, sourceMetricId,
        checklistDrafts.map { "${it.id}:${it.uuid}:${it.position}:${it.name}" },
    ).joinToString("\u001f")
    val initialFingerprint by rememberSaveable(editorKey) { mutableStateOf(editorFingerprint) }
    var showDiscardConfirmation by rememberSaveable(editorKey) { mutableStateOf(false) }
    val requestDismiss = { if (editorFingerprint != initialFingerprint) showDiscardConfirmation = true else onDismiss() }
    BackHandler(enabled = !showDiscardConfirmation, onBack = requestDismiss)
    ProductivityEditorDialog(
        modifier = dialogModifier,
        testTag = "habit-editor-surface",
        onDismissRequest = requestDismiss,
        title = { Text(if (habit == null) "Create habit" else "Edit habit") },
        text = {
            LazyColumn(
                modifier = Modifier.testTag("habit-editor-fields"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    ResponsiveFieldPair(
                        first = { field -> OutlinedTextField(icon, { icon = it.take(2) }, label = { Text("Icon") }, modifier = field) },
                        second = { field -> OutlinedTextField(name, { name = it }, label = { Text("Name *") }, modifier = field.testTag("habit-editor-name")) },
                    )
                }
                if (showAdvanced) item { OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth()) }
                if (showAdvanced) item {
                    EnumChips("Intent", HabitIntent.entries, intent, HabitIntent::displayLabel) { intent = it }
                    Text(intent.explanation(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    Text("How do you want to track it?", fontWeight = FontWeight.Bold)
                    Text("Choose the action you want available each day. Whip fills in sensible defaults.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        HabitTrackingMode.entries.forEach { selected ->
                            FilterChip(
                                selected = mode == selected,
                                onClick = {
                                    mode = selected
                                    when (selected) {
                                        HabitTrackingMode.Duration -> { unitId = "second"; dimension = UnitDimension.Duration; precision = "0"; intent = HabitIntent.Build; comparison = TargetComparison.AtLeast }
                                        HabitTrackingMode.Rating -> { unitId = "unitless"; dimension = UnitDimension.Unitless; precision = defaults.numberPrecision.toString(); intent = HabitIntent.Observe; comparison = TargetComparison.None }
                                        HabitTrackingMode.Decimal -> { unitId = "unitless"; dimension = UnitDimension.Unitless; precision = defaults.numberPrecision.toString(); intent = HabitIntent.Build; comparison = TargetComparison.AtLeast }
                                        HabitTrackingMode.LogOnly -> { unitId = "unitless"; dimension = UnitDimension.Unitless; precision = defaults.numberPrecision.toString(); intent = HabitIntent.Observe; comparison = TargetComparison.None }
                                        HabitTrackingMode.LimitAvoid -> { unitId = "count"; dimension = UnitDimension.Count; precision = "0"; intent = HabitIntent.Avoid; comparison = TargetComparison.AtMost }
                                        HabitTrackingMode.CheckOff, HabitTrackingMode.Checklist -> { unitId = "count"; dimension = UnitDimension.Count; precision = "0"; intent = HabitIntent.Build; comparison = TargetComparison.AtLeast; targetMin = "1"; targetPeriod = TargetPeriod.Occurrence }
                                        HabitTrackingMode.Count -> { unitId = "count"; dimension = UnitDimension.Count; precision = "0"; intent = HabitIntent.Build; comparison = TargetComparison.AtLeast }
                                    }
                                },
                                label = { Text(selected.setupLabel()) },
                            )
                        }
                    }
                }
                if (mode == HabitTrackingMode.Checklist) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Checklist items", fontWeight = FontWeight.Bold)
                            checklistDrafts.forEachIndexed { index, draft ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = draft.name,
                                        onValueChange = { name ->
                                            checklistDrafts = ArrayList(checklistDrafts).also {
                                                it[index] = draft.copy(name = name)
                                            }
                                        },
                                        label = { Text("Item ${index + 1}") },
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(
                                        enabled = index > 0,
                                        onClick = {
                                            checklistDrafts = ArrayList(checklistDrafts).also {
                                                val moved = it.removeAt(index)
                                                it.add(index - 1, moved)
                                            }
                                        },
                                    ) { Icon(Icons.Outlined.ArrowUpward, contentDescription = "Move ${draft.name.ifBlank { "item ${index + 1}" }} up") }
                                    IconButton(
                                        enabled = index < checklistDrafts.lastIndex,
                                        onClick = {
                                            checklistDrafts = ArrayList(checklistDrafts).also {
                                                val moved = it.removeAt(index)
                                                it.add(index + 1, moved)
                                            }
                                        },
                                    ) { Icon(Icons.Outlined.ArrowDownward, contentDescription = "Move ${draft.name.ifBlank { "item ${index + 1}" }} down") }
                                    IconButton(
                                        onClick = {
                                            checklistDrafts = ArrayList(checklistDrafts).also { it.removeAt(index) }
                                        },
                                    ) { Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove ${draft.name.ifBlank { "item ${index + 1}" }}") }
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    checklistDrafts = ArrayList(checklistDrafts).also {
                                        it += HabitChecklistItemDraft("", it.size, uuid = java.util.UUID.randomUUID().toString())
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Add checklist item")
                            }
                        }
                    }
                }
                if (showAdvanced) item { EnumDropdown("Target rule", TargetComparison.entries, comparison, TargetComparison::displayLabel) { comparison = it } }
                if (comparison != TargetComparison.None) {
                    if (mode !in setOf(HabitTrackingMode.CheckOff, HabitTrackingMode.Checklist)) item {
                        if (comparison == TargetComparison.WithinRange) {
                            ResponsiveFieldPair(
                                first = { field -> NumberTextField(targetMin, { targetMin = it }, "Minimum", field) },
                                second = { field -> NumberTextField(targetMax, { targetMax = it }, "Maximum", field) },
                            )
                        } else NumberTextField(
                            targetMin,
                            { targetMin = it },
                            if (showAdvanced) "Target" else when (targetPeriod) {
                                TargetPeriod.Week -> "Target per week"
                                TargetPeriod.Month -> "Target per month"
                                TargetPeriod.Occurrence -> "Target each time"
                                else -> "Target per day"
                            },
                        )
                    }
                    if (showAdvanced) item {
                        EnumChips("Target period", TargetPeriod.entries, targetPeriod, TargetPeriod::displayLabel) { targetPeriod = it }
                        Text(targetPeriod.explanation(schedule), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (targetPeriod == TargetPeriod.RollingDays) item { NumberTextField(rollingDays, { rollingDays = it }, "Rolling-day window") }
                }
                item { EnumDropdown("Schedule", HabitScheduleType.entries, schedule, { it.scheduleLabel() }) { schedule = it } }
                if (schedule == HabitScheduleType.EveryNDays) item { NumberTextField(interval, { interval = it }, "Every how many days?") }
                if (schedule in setOf(HabitScheduleType.FlexibleTimesPerWeek, HabitScheduleType.FlexibleTimesPerMonth)) {
                    item { NumberTextField(flexible, { flexible = it }, if (schedule == HabitScheduleType.FlexibleTimesPerWeek) "Times per week" else "Times per month") }
                }
                if (schedule == HabitScheduleType.SelectedWeekdays) {
                    item {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            orderedHabitWeekdays(weekStart).forEach { day ->
                                FilterChip(selected = day in weekdays, onClick = { weekdays = if (day in weekdays) weekdays - day else weekdays + day }, label = { Text(day.name.take(2)) })
                            }
                        }
                    }
                }
                if (showAdvanced && mode in setOf(HabitTrackingMode.Count, HabitTrackingMode.Decimal, HabitTrackingMode.LimitAvoid)) {
                    item { NumberTextField(quickIncrement, { quickIncrement = it }, "Quick increment") }
                    if (!quickIncrementValid) item {
                        Text("Quick increment must be a positive number.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (intent == HabitIntent.Avoid) item { EnumDropdown("Missing-data policy", AvoidMissingPolicy.entries, avoidPolicy, { it.name }) { avoidPolicy = it } }
                item {
                    AreaPicker(
                        areas = areas,
                        selectedAreaId = areaId,
                        selectedAreaName = area,
                        onSelect = { id, value -> areaId = id; area = value },
                        onCreateArea = onCreateArea,
                        modifier = Modifier.fillMaxWidth(),
                        dialogModifier = dialogModifier,
                        inheritedFromScope = habit == null && initialDraft?.areaId == null && defaultAreaId != null,
                    )
                }
                item {
                    TextButton(onClick = { showAdvanced = !showAdvanced }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (showAdvanced) "Hide advanced options" else "Show advanced options", modifier = Modifier.fillMaxWidth())
                    }
                }
                if (showAdvanced) {
                    if (sourceMetrics.isNotEmpty()) item {
                        EnumDropdown(
                            "Data source",
                            listOf<MetricDefinition?>(null) + sourceMetrics,
                            sourceMetrics.firstOrNull { it.id == sourceMetricId },
                            { metric -> metric?.let { "Health Connect · ${it.name}" } ?: "Manual check-ins" },
                        ) { selected ->
                            sourceMetricId = selected?.id
                            if (selected != null) {
                                dimension = selected.dimension
                                unitId = selected.defaultUnitId
                                precision = selected.precision.toString()
                                mode = when (selected.valueKind) {
                                    MetricValueKind.Integer -> HabitTrackingMode.Count
                                    MetricValueKind.Duration -> HabitTrackingMode.Duration
                                    else -> HabitTrackingMode.Decimal
                                }
                            }
                        }
                        Text(
                            "Imported records are read-only here. Updates and deletions from Health Connect reconcile by source ID without double counting.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    item { Text("Choose a built-in unit or one created under Settings > Custom units.") }
                    item { OutlinedTextField(tags, { tags = it }, label = { Text("Tags, comma-separated") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(colorHex, { colorHex = it }, label = { Text("Color, #RRGGBB or #AARRGGBB") }, modifier = Modifier.fillMaxWidth()) }
                    item {
                        EnumDropdown("Unit dimension", UnitDimension.entries, dimension, { it.name }) { selected ->
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
                            EnumDropdown(
                                "Saved unit",
                                compatibleUnits,
                                compatibleUnits.firstOrNull { it.id == unitId } ?: compatibleUnits.first(),
                                ::unitDefinitionLabel,
                            ) { unitId = it.id }
                        }
                    }
                    item { NumberTextField(precision, { precision = it }, "Decimal places (0–6)") }
                    item {
                        NumericQuickActionBuilder(
                            values = quickActionResult.values,
                            increment = quickIncrement,
                            rawSpecification = quickActions,
                            onSpecificationChange = { quickActions = it },
                        )
                        quickActionResult.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    }
                    item {
                        val parsed = reminders.split(',').mapNotNull { parseClockMinutes(it.trim()) }.distinct().sorted()
                        ReminderTimesEditor("Default reminders", parsed) { updated ->
                            if (updated.isNotEmpty() && parsed.isEmpty()) onRequestNotificationPermission()
                            reminders = updated.joinToString(",", transform = ::formatClockMinutes)
                        }
                    }
                    item {
                        WeekdayReminderEditor(
                            values = parseWeekdayReminderMap(weekdayReminders),
                            firstDayOfWeek = weekStart,
                        ) { updated ->
                            if (updated.values.any { it.isNotEmpty() } && parseWeekdayReminderMap(weekdayReminders).values.all { it.isEmpty() }) {
                                onRequestNotificationPermission()
                            }
                            weekdayReminders = formatWeekdayReminderMap(updated)
                        }
                    }
                    item {
                        ResponsiveFieldPair(
                            first = { field -> ClockPickerButton("Earliest check-in", parseClockMinutes(windowStart), { windowStart = it?.let(::formatClockMinutes).orEmpty() }, field) },
                            second = { field -> ClockPickerButton("Latest check-in", parseClockMinutes(windowEnd), { windowEnd = it?.let(::formatClockMinutes).orEmpty() }, field) },
                        )
                    }
                    item { EnumDropdown("End condition", HabitEndType.entries, endType, { it.scheduleLabel() }) { endType = it } }
                    if (endType == HabitEndType.OnDate) item { OutlinedButton(onClick = { showEndDatePicker = true }, modifier = Modifier.fillMaxWidth()) { Text(endDate?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: "Choose end date") } }
                    if (endType in setOf(HabitEndType.AfterStreak, HabitEndType.AfterCompletions, HabitEndType.AfterTotal)) item { NumberTextField(endValue, { endValue = it }, when (endType) { HabitEndType.AfterStreak -> "End after streak"; HabitEndType.AfterCompletions -> "End after completions"; else -> "End after total" }) }
                    item { EnumDropdown("First day of week", DayOfWeek.entries, weekStart, { it.name.lowercase().replaceFirstChar(Char::uppercase) }) { weekStart = it } }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && quickActionResult.error == null && quickIncrementValid && !saving,
                onClick = {
                    val draft = HabitDraft(
                            name = name,
                            notes = notes,
                            areaId = areaId,
                            area = area,
                            tags = tags.split(',').map(String::trim).filter(String::isNotBlank),
                            icon = icon.ifBlank { "✓" },
                            colorArgb = parseColorArgb(colorHex),
                            intent = intent,
                            trackingMode = mode,
                            dimension = dimension,
                            unitId = unitId,
                            precision = precision.toIntOrNull()?.coerceIn(0, 6) ?: 0,
                            comparison = comparison,
                            targetMin = targetMin.toWhipDoubleOrNull(),
                            targetMax = targetMax.toWhipDoubleOrNull(),
                            targetPeriod = targetPeriod,
                            rollingDays = rollingDays.toIntOrNull(),
                            scheduleType = schedule,
                            scheduleInterval = interval.toIntOrNull() ?: 1,
                            weekdays = weekdays,
                            flexibleTimesPerWeek = flexible.toIntOrNull(),
                            startDate = initial.startDate,
                            endType = endType,
                            endDate = endDate,
                            endValue = endValue.toWhipDoubleOrNull(),
                            timeWindowStartMinutes = parseClockMinutes(windowStart),
                            timeWindowEndMinutes = parseClockMinutes(windowEnd),
                            quickIncrement = quickIncrement.toWhipDoubleOrNull() ?: 1.0,
                            quickActions = quickActionResult.values,
                            reminderMinutes = reminders.split(',').mapNotNull { parseClockMinutes(it.trim()) },
                            weekdayReminderMinutes = parseWeekdayReminderMap(weekdayReminders),
                            weekStart = weekStart,
                            avoidMissingPolicy = avoidPolicy,
                            checklistItems = checklistDrafts.filter { it.name.isNotBlank() }
                                .mapIndexed { index, item -> item.copy(name = item.name.trim(), position = index) },
                            sourceMetricId = sourceMetricId,
                        )
                    onSave(draft)
                },
            ) { Text(if (saving) "Saving…" else "Save") }
        },
        dismissButton = { TextButton(onClick = requestDismiss, enabled = !saving) { Text("Cancel") } },
    )
    if (showEndDatePicker) WhipDatePickerDialog(endDate ?: today, { showEndDatePicker = false }, { endDate = it; showEndDatePicker = false })
    if (showDiscardConfirmation) {
        UnsavedChangesDialog(
            subject = "habit",
            onKeepEditing = { showDiscardConfirmation = false },
            onDiscard = onDismiss,
        )
    }
}

@Composable
internal fun HabitValueDialog(item: HabitDayProgress, onDismiss: () -> Unit, onLog: (Double, String) -> Unit) {
    var value by rememberSaveable(item.habit.id, item.date) { mutableStateOf(editableNumericValue(item.value)) }
    var note by rememberSaveable(item.habit.id, item.date) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set ${item.habit.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberTextField(value, { value = it }, if (item.habit.trackingMode == HabitTrackingMode.Rating) "Rating" else "Value")
                OutlinedTextField(note, { note = it }, label = { Text("Optional note") })
                if (item.habit.quickActions.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        item.habit.quickActions.forEach { quick ->
                            val label = editableNumericValue(quick)
                            TextButton(onClick = { value = label }) { Text(label) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = value.toWhipDoubleOrNull()?.isFinite() == true, onClick = { onLog(requireNotNull(value.toWhipDoubleOrNull()), note) }) { Text("Set") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun HabitHistoryLogDialog(
    item: HabitDayProgress,
    log: HabitLog?,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Double?, HabitLogStatus, LocalDate, String) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val editorKey = "habit-log-${log?.id ?: "${item.habit.id}-${initialDate.toEpochDay()}"}"
    var value by rememberSaveable(editorKey) { mutableStateOf(log?.value?.let(::editableNumericValue).orEmpty()) }
    var status by rememberSaveable(editorKey) { mutableStateOf(log?.status ?: HabitLogStatus.Recorded) }
    var date by rememberSaveable(editorKey) { mutableStateOf(initialDate) }
    var note by rememberSaveable(editorKey) { mutableStateOf(log?.note.orEmpty()) }
    var showDatePicker by rememberSaveable(editorKey) { mutableStateOf(false) }
    val requiresValue = status in setOf(HabitLogStatus.Recorded, HabitLogStatus.Success) &&
        item.habit.trackingMode !in setOf(HabitTrackingMode.CheckOff, HabitTrackingMode.Checklist)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (log == null) "Add past ${item.habit.name} entry" else "Edit ${item.habit.name} entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EnumDropdown("State", HabitLogStatus.entries, status, { it.name }) { selected ->
                    status = selected
                    if (selected in setOf(HabitLogStatus.Skipped, HabitLogStatus.Excused, HabitLogStatus.Missing)) value = ""
                }
                if (status !in setOf(HabitLogStatus.Skipped, HabitLogStatus.Excused, HabitLogStatus.Missing)) {
                    NumberTextField(
                        value,
                        { value = it },
                        "Value (${(log?.enteredUnitId ?: item.habit.unitId).unitLabel()})",
                    )
                }
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
                }
                OutlinedTextField(note, { note = it }, label = { Text("Optional note") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = !requiresValue || value.toWhipDoubleOrNull() != null,
                onClick = {
                    val effective = value.toWhipDoubleOrNull()
                        ?: 1.0.takeIf { item.habit.trackingMode in setOf(HabitTrackingMode.CheckOff, HabitTrackingMode.Checklist) && status in setOf(HabitLogStatus.Recorded, HabitLogStatus.Success) }
                    onSave(effective, status, date, note)
                },
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
    if (showDatePicker) {
        WhipDatePickerDialog(date, { showDatePicker = false }, { selected -> date = selected; showDatePicker = false })
    }
}

@Composable
private fun HabitActionsDialog(
    item: HabitDayProgress,
    dialogModifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onPin: () -> Unit,
    onPause: () -> Unit,
    onSchedulePause: () -> Unit,
    onSkip: () -> Unit,
    onExcuse: () -> Unit,
    onMissing: () -> Unit,
    logs: List<HabitLog>,
    onAddHistoricalLog: () -> Unit,
    onEditLog: (HabitLog) -> Unit,
    linkedGoals: List<String>,
    onLinkGoal: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    lowPressureMode: Boolean = false,
) {
    var visibleLogs by rememberSaveable(item.habit.id) { mutableIntStateOf(8) }
    var section by rememberSaveable(item.habit.id) {
        mutableStateOf(if (item.habit.archived) HabitDetailSection.More else HabitDetailSection.Today)
    }
    PaneAwareAlertDialog(
        modifier = dialogModifier.testTag("habit-detail-surface"),
        onDismissRequest = onDismiss,
        title = { Text(item.habit.name) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                DetailSectionBar(
                    labels = HabitDetailSection.entries.map(HabitDetailSection::label),
                    selected = section.label,
                    onSelect = { label -> section = HabitDetailSection.entries.first { it.label == label } },
                    testTagPrefix = "habit-detail-section",
                )
                when (section) {
                    HabitDetailSection.Today -> {
                        Text(
                            if (item.habit.archived) "This habit is archived. Restore it from More to resume check-ins."
                            else if (lowPressureMode) "${formatHabitValue(item.value, item.habit.precision)} logged"
                            else "${formatHabitValue(item.value, item.habit.precision)} logged · streak ${item.streak}",
                        )
                        if (!item.habit.archived) listOf(
                            (if (item.habit.paused) "Resume" else "Pause indefinitely") to onPause,
                            "Schedule pause dates" to onSchedulePause,
                            "Skip today" to onSkip,
                            "Excuse today" to onExcuse,
                            "Mark today missing" to onMissing,
                        ).forEach { (label, action) -> TextButton(onClick = action, modifier = Modifier.fillMaxWidth()) { Text(label, modifier = Modifier.fillMaxWidth()) } }
                    }
                    HabitDetailSection.History -> {
                        TextButton(onClick = onAddHistoricalLog, modifier = Modifier.fillMaxWidth()) { Text("Add backdated entry", modifier = Modifier.fillMaxWidth()) }
                        if (logs.isEmpty()) Text("No entries yet.") else {
                            Text("Recent entries", style = MaterialTheme.typography.labelMedium)
                            val orderedLogs = logs.sortedByDescending(HabitLog::timestamp)
                            orderedLogs.take(visibleLogs).forEach { log ->
                                TextButton(onClick = { onEditLog(log) }, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        "${log.localDate}: ${log.value?.let { formatHabitValue(it, item.habit.precision) } ?: log.status.name}${if (log.note.isBlank()) "" else " · ${log.note}"}",
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                            if (visibleLogs < orderedLogs.size) TextButton(
                                onClick = { visibleLogs = (visibleLogs + 25).coerceAtMost(orderedLogs.size) },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Show 25 more · ${orderedLogs.size - visibleLogs} remaining") }
                        }
                    }
                    HabitDetailSection.Connections -> {
                        if (linkedGoals.isEmpty()) Text("This habit does not feed any goals yet.")
                        else Text("Feeds: ${linkedGoals.distinct().joinToString()}", style = MaterialTheme.typography.bodyMedium)
                        if (!item.habit.archived) TextButton(onClick = onLinkGoal, modifier = Modifier.fillMaxWidth()) { Text("Link to a goal", modifier = Modifier.fillMaxWidth()) }
                    }
                    HabitDetailSection.More -> {
                        if (!item.habit.archived) {
                            listOf(
                                "Duplicate" to onDuplicate,
                                (if (item.habit.pinned) "Unpin" else "Pin") to onPin,
                            ).forEach { (label, action) -> TextButton(onClick = action, modifier = Modifier.fillMaxWidth()) { Text(label, modifier = Modifier.fillMaxWidth()) } }
                        }
                        TextButton(onClick = onArchive, modifier = Modifier.fillMaxWidth()) {
                            Text(if (item.habit.archived) "Restore" else "Archive", modifier = Modifier.fillMaxWidth())
                        }
                        TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                            Text("Delete permanently", modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        dismissButton = { DetailEditButton("Edit habit", onEdit) },
    )
}

private enum class HabitDetailSection(val label: String) {
    Today("Today"),
    History("History"),
    Connections("Connections"),
    More("More"),
}

@Composable
private fun HabitGoalLinkDialog(
    habit: Habit,
    goals: List<GoalProjection>,
    onDismiss: () -> Unit,
    onSave: (GoalProjection, LinkSourceMetric, Boolean) -> Unit,
) {
    var selectedGoalId by rememberSaveable(habit.id) { mutableStateOf(goals.firstOrNull()?.goal?.id) }
    val selected = selectedGoalId?.let { id -> goals.firstOrNull { it.goal.id == id } }
    var metric by rememberSaveable(habit.id) { mutableStateOf(if (habit.trackingMode == HabitTrackingMode.CheckOff) LinkSourceMetric.Success else LinkSourceMetric.NumericValue) }
    var includeHistory by rememberSaveable(habit.id) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link ${habit.name} to a goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (goals.isEmpty()) Text("Create an active goal first.") else {
                    EnumDropdown("Goal", goals, selected ?: goals.first(), { it.goal.name }) { selectedGoalId = it.goal.id }
                    EnumDropdown("Contribution", listOf(LinkSourceMetric.NumericValue, LinkSourceMetric.Success), metric, {
                        if (it == LinkSourceMetric.Success) "Add one when successful" else "Use logged numeric value"
                    }) { metric = it }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(includeHistory, { includeHistory = it })
                        Text("Backfill existing habit logs", modifier = Modifier.padding(start = 8.dp))
                    }
                    Text("Whip validates unit compatibility and keeps each source event explainable.", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(enabled = selected != null, onClick = { onSave(requireNotNull(selected), metric, includeHistory) }) { Text("Create link") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun HabitPauseDialog(today: LocalDate, onDismiss: () -> Unit, onSave: (LocalDate, LocalDate?, String) -> Unit) {
    var start by rememberSaveable { mutableStateOf(today) }
    var end by rememberSaveable { mutableStateOf<LocalDate?>(today) }
    var note by rememberSaveable { mutableStateOf("") }
    var pickingStart by rememberSaveable { mutableStateOf(false) }
    var pickingEnd by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule habit pause") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pickingStart = true }) { Text("Start ${start.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}") }
            OutlinedButton(onClick = { pickingEnd = true }) { Text(end?.let { "End ${it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}" } ?: "No end date") }
            OutlinedTextField(note, { note = it }, label = { Text("Optional note") })
        } },
        confirmButton = { TextButton(enabled = end == null || !requireNotNull(end).isBefore(start), onClick = { onSave(start, end, note) }) { Text("Save pause") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (pickingStart) WhipDatePickerDialog(start, { pickingStart = false }, { start = it; pickingStart = false })
    if (pickingEnd) WhipDatePickerDialog(end ?: start, { pickingEnd = false }, { end = it; pickingEnd = false })
}

private fun TargetComparison.displayLabel(): String = when (this) {
    TargetComparison.AtLeast -> "At least"
    TargetComparison.AtMost -> "At most"
    TargetComparison.Exactly -> "Exactly"
    TargetComparison.WithinRange -> "Within a range"
    TargetComparison.None -> "Track only · no target"
}

private fun TargetPeriod.displayLabel(): String = when (this) {
    TargetPeriod.Occurrence -> "Each scheduled occurrence"
    TargetPeriod.Day -> "Per day"
    TargetPeriod.Week -> "Per week"
    TargetPeriod.Month -> "Per month"
    TargetPeriod.RollingDays -> "Rolling-day window"
}

@Composable
private fun <T> EnumDropdown(label: String, values: List<T>, selected: T, text: (T) -> String, onSelect: (T) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(text(selected)) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            values.forEach { value -> DropdownMenuItem(text = { Text(text(value)) }, onClick = { onSelect(value); expanded = false }) }
        }
    }
}

@Composable
private fun <T> EnumChips(label: String, values: List<T>, selected: T, text: (T) -> String, onSelect: (T) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            values.forEach { value -> FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(text(value)) }) }
        }
    }
}

@Composable
private fun NumberTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(value, onValueChange, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = modifier.fillMaxWidth())
}

private fun HabitTrackingMode.label() = name.replace(Regex("([a-z])([A-Z])"), "$1 $2")
private fun HabitTrackingMode.setupLabel(): String = when (this) {
    HabitTrackingMode.CheckOff -> "Done"
    HabitTrackingMode.Count -> "Count"
    HabitTrackingMode.Decimal -> "Measurement"
    HabitTrackingMode.Duration -> "Timer"
    HabitTrackingMode.Checklist -> "Checklist"
    HabitTrackingMode.Rating -> "Rating"
    HabitTrackingMode.LimitAvoid -> "Limit"
    HabitTrackingMode.LogOnly -> "Log only"
}

private fun HabitIntent.displayLabel(): String = when (this) {
    HabitIntent.Build -> "Build"
    HabitIntent.Limit -> "Limit"
    HabitIntent.Avoid -> "Avoid"
    HabitIntent.Observe -> "Observe"
}

private fun HabitIntent.explanation(): String = when (this) {
    HabitIntent.Build -> "Practice or increase this behavior."
    HabitIntent.Limit -> "Stay at or below a chosen amount."
    HabitIntent.Avoid -> "Treat the behavior as something to avoid."
    HabitIntent.Observe -> "Record it without judging success or failure."
}

private fun TargetPeriod.explanation(schedule: HabitScheduleType): String = when (this) {
    TargetPeriod.Occurrence -> "The target resets each time this habit is scheduled."
    TargetPeriod.Day -> if (schedule == HabitScheduleType.Daily) "The target resets each day." else "Multiple check-ins on the same day count together."
    TargetPeriod.Week -> "All entries in the week count toward one target."
    TargetPeriod.Month -> "All entries in the month count toward one target."
    TargetPeriod.RollingDays -> "The target follows a moving number of recent days."
}
private fun HabitScheduleType.scheduleLabel(): String = when (this) {
    HabitScheduleType.Daily -> "Daily"
    HabitScheduleType.EveryNDays -> "Every X days"
    HabitScheduleType.SelectedWeekdays -> "Weekdays"
    HabitScheduleType.FlexibleTimesPerWeek -> "Flexible times per week"
    HabitScheduleType.FlexibleTimesPerMonth -> "Flexible times per month"
}
private fun HabitEndType.scheduleLabel() = name.replace(Regex("([a-z])([A-Z])"), "$1 $2")
private fun parseClockMinutes(value: String): Int? {
    val parts = value.split(':')
    val hours = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val minutes = parts.getOrNull(1)?.toIntOrNull() ?: return null
    if (hours !in 0..23 || minutes !in 0..59) return null
    return hours * 60 + minutes
}

internal fun parseWeekdayReminderMap(value: String): Map<DayOfWeek, List<Int>> = value.split(';').mapNotNull { segment ->
    val pieces = segment.split('=', limit = 2)
    val key = pieces.getOrNull(0)?.trim()?.uppercase().orEmpty()
    if (key.isBlank()) return@mapNotNull null
    val day = DayOfWeek.entries.firstOrNull { key == it.name || key == it.name.take(3) }
        ?: return@mapNotNull null
    val times = pieces.getOrNull(1).orEmpty().split(',')
        .mapNotNull { parseClockMinutes(it.trim()) }
        .distinct()
        .sorted()
    if (times.isEmpty()) null else day to times
}.toMap()

internal fun formatWeekdayReminderMap(value: Map<DayOfWeek, List<Int>>): String = value.entries
    .filter { (_, times) -> times.isNotEmpty() }
    .sortedBy { it.key.value }
    .joinToString(";") { (day, times) ->
        "${day.name.take(3)}=${times.distinct().sorted().joinToString(",", transform = ::formatClockMinutes)}"
    }
private fun orderedHabitWeekdays(first: DayOfWeek): List<DayOfWeek> =
    (0..6).map { offset -> DayOfWeek.of((first.value - 1 + offset) % 7 + 1) }
private fun String.unitLabel() = when (this) { "count" -> ""; "second" -> "sec"; "unitless" -> ""; "kilogram" -> "kg"; "pound" -> "lb"; "litre" -> "L"; "millilitre" -> "mL"; "fluid_ounce" -> "fl oz"; else -> this }
private fun unitDefinitionLabel(unit: UnitDefinition): String =
    "${unit.name}${unit.symbol.takeIf(String::isNotBlank)?.let { " ($it)" }.orEmpty()}"
internal fun formatHabitValue(value: Double, precision: Int): String = String.format(Locale.getDefault(), "%.${precision.coerceIn(0, 4)}f", value)
