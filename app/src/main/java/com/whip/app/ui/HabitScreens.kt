package com.whip.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import com.whip.app.core.AppSettings
import com.whip.app.domain.Habit
import com.whip.app.domain.Area
import com.whip.app.domain.CustomIdentityEmoji
import com.whip.app.domain.HabitChecklistItemDraft
import com.whip.app.domain.HabitDayProgress
import com.whip.app.domain.HabitDayState
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.HabitEndType
import com.whip.app.domain.HabitLog
import com.whip.app.domain.HabitLogStatus
import com.whip.app.domain.HabitScheduleType
import com.whip.app.domain.HabitSkip
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.DEFAULT_HABIT_EMOJI
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
import com.whip.app.domain.TriggerAction
import com.whip.app.domain.compactNumericSequence
import com.whip.app.domain.editableNumericValue
import com.whip.app.domain.parseNumericSequence
import com.whip.app.domain.periodBounds
import com.whip.app.domain.isScheduledOn
import com.whip.app.domain.outcomeForPeriod
import com.whip.app.domain.toWhipDoubleOrNull
import com.whip.app.domain.valueInUnit
import com.whip.app.domain.dayStateOn
import com.whip.app.domain.successfulPeriodOutcomeDates
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import com.whip.app.ui.theme.whipColors

enum class HabitDestination { Today, All, Connections, Archived, Insights }

@Composable
fun HabitAreaContent(
    state: HabitUiState,
    innerPadding: PaddingValues,
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier,
    editorModifier: Modifier = modifier,
    goalState: GoalUiState = GoalUiState(),
    createRequested: Boolean = false,
    onCreateRequestConsumed: () -> Unit = {},
    openHabitIdRequest: Long? = null,
    onOpenHabitRequestConsumed: () -> Unit = {},
    editHabitIdRequest: Long? = null,
    onEditHabitRequestConsumed: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    lowPressureMode: Boolean = false,
    onOpenTask: (Long) -> Unit = {},
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
    onAreaChanged: (String?) -> Unit = {},
    destinationState: MutableState<HabitDestination>? = null,
) {
    if (state.loading || state.errorMessage != null) {
        DomainLoadContent("habits", innerPadding, state.errorMessage, viewModel::retryLoading)
        return
    }
    val localDestinationState = rememberSaveable { mutableStateOf(HabitDestination.Today) }
    val activeDestinationState = destinationState ?: localDestinationState
    var destination by activeDestinationState
    var creating by rememberSaveable { mutableStateOf(false) }
    var editingHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var actionsHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var numericLogHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pauseRequestHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var templatesOpen by rememberSaveable { mutableStateOf(false) }
    var templateDraft by rememberSaveable { mutableStateOf<HabitDraft?>(null) }
    var editorSavePending by rememberSaveable { mutableStateOf(false) }
    var linkingHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var historicalLogHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingLogHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingLogId by rememberSaveable { mutableStateOf<Long?>(null) }
    var focusedArchivedHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteCandidateHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var skipConfirmationHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
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
        DestinationTabBar(
            selected = destination,
            destinations = HabitDestination.entries,
            primaryDestinations = listOf(HabitDestination.Today, HabitDestination.All, HabitDestination.Insights),
            onSelect = { destination = it; focusedArchivedHabitId = null },
            label = { if (it == HabitDestination.Connections) "Automations" else it.name },
            testTagPrefix = "habit-destination",
            barTestTag = "habit-workspace-navigation",
        )
        when (destination) {
            HabitDestination.Today -> HabitList(
                title = "Today",
                subtitle = "Check in, log a value, or continue a timer. Completed habits move to Done for confirmation or undo.",
                progress = state.today,
                empty = areaScopeLabel?.let { "No habits are due today in $it." } ?: "No habits are due today.",
                onTemplates = { templatesOpen = true },
                onOpen = { actionsHabitId = it.habit.id },
                onEdit = { editingHabitId = it.habit.id },
                onQuick = { item -> quickHabitAction(item, viewModel) { numericLogHabitId = item.habit.id } },
                onQuickValue = viewModel::addValue,
                onSetValue = { numericLogHabitId = it.habit.id },
                onDecrement = viewModel::decrementValue,
                onUndo = { item -> latestPeriodLog(item)?.let { viewModel.undoLog(it.id, item.habit.id) } },
                canUndo = { latestPeriodLog(it) != null },
                onUndoSkip = { item -> viewModel.undoSkip(item.habit.id, item.date) },
                onChecklist = viewModel::toggleChecklist,
                onReorder = null,
                lowPressureMode = lowPressureMode,
                separateCompleted = true,
            )
            HabitDestination.All -> HabitList(
                title = "All Habits",
                subtitle = "Build, limit, avoid, or simply observe anything you define.",
                progress = state.all,
                empty = areaScopeLabel?.let { "No habits in $it." } ?: "Your habit list is empty.",
                onTemplates = { templatesOpen = true },
                onOpen = { actionsHabitId = it.habit.id },
                onEdit = { editingHabitId = it.habit.id },
                onQuick = { item -> quickHabitAction(item, viewModel) { numericLogHabitId = item.habit.id } },
                onQuickValue = viewModel::addValue,
                onSetValue = { numericLogHabitId = it.habit.id },
                onDecrement = viewModel::decrementValue,
                onUndo = { item -> latestPeriodLog(item)?.let { viewModel.undoLog(it.id, item.habit.id) } },
                canUndo = { latestPeriodLog(it) != null },
                onUndoSkip = { item -> viewModel.undoSkip(item.habit.id, item.date) },
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
            modifier = editorModifier,
            habit = editing?.habit,
            initialDraft = templateDraft.takeIf { editing == null },
            initialChecklist = editing?.checklistItems?.mapIndexed { index, (item, _) ->
                HabitChecklistItemDraft(item.name, index, item.id, item.uuid)
            }.orEmpty(),
            today = state.currentDate,
            defaultWeekStart = viewModel.defaultSettings().defaultHabitWeekStart,
            defaults = viewModel.defaultSettings(),
            customUnits = state.customUnits,
            sourceMetrics = state.sourceMetrics,
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
                editingHabitId = null
                templateDraft = null
                editorSavePending = false
            },
            onSave = { draft ->
                editorSavePending = true
                viewModel.saveHabit(editing?.habit?.id, draft) { succeeded ->
                    editorSavePending = false
                    if (succeeded) {
                        creating = false
                        editingHabitId = null
                        templateDraft = null
                    }
                }
                onAreaChanged(draft.areaId)
            },
        )
    }
    actions?.let { item ->
        HabitActionsDialog(
            item,
            modifier = modifier,
            onDismiss = { actionsHabitId = null },
            onEdit = { editingHabitId = item.habit.id; actionsHabitId = null },
            onDuplicate = { viewModel.duplicate(item.habit.id); actionsHabitId = null },
            onPin = { viewModel.setPinned(item.habit.id, !item.habit.pinned); actionsHabitId = null },
            onPause = { viewModel.setPaused(item.habit.id, !item.habit.paused); actionsHabitId = null },
            onSchedulePause = { pauseRequestHabitId = item.habit.id; actionsHabitId = null },
            onSkip = { skipConfirmationHabitId = item.habit.id; actionsHabitId = null },
            onUndoSkip = { viewModel.undoSkip(item.habit.id, item.date); actionsHabitId = null },
            logs = state.logs.filter { it.habitId == item.habit.id },
            skips = state.skips.filter { it.habitId == item.habit.id },
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
    skipConfirmationHabitId?.let { habitId ->
        val item = progressById[habitId]
        if (item != null) PaneAwareAlertDialog(
            onDismissRequest = { skipConfirmationHabitId = null },
            title = { Text("Skip Today?") },
            text = {
                Text("${item.habit.name} will be marked Skipped for today. The day stays visible in History and Insights, does not count as completed or missed, protects your streak, and stops today's reminders.")
            },
            confirmButton = {
                WhipTextButton(onClick = {
                    viewModel.skipDay(item.habit.id, item.date)
                    skipConfirmationHabitId = null
                }) { Text("Skip Today") }
            },
            dismissButton = { WhipTextButton(onClick = { skipConfirmationHabitId = null }) { Text("Cancel") } },
        )
    }
    deleteCandidate?.let { habit ->
        val logCount = state.logs.count { it.habitId == habit.id }
        val skipCount = state.skips.count { it.habitId == habit.id }
        val linkCount = goalState.linkRules.count {
            (it.sourceType == LinkSourceType.Habit && it.sourceEntityId == habit.id) || it.sourceMetricId == habit.metricId
        }
        val automationCount = state.triggerRules.count { it.targetType == TriggerTargetType.Habit && it.targetEntityId == habit.id }
        PermanentDeleteDialog(
            title = "Delete ${habit.name} Permanently?",
            impacts = listOf(
                "$logCount check-in${if (logCount == 1) "" else "s"}, $skipCount skipped day${if (skipCount == 1) "" else "s"}, checklist state, and streak history will be removed",
                "$linkCount Goal Automation${if (linkCount == 1) "" else "s"} and its generated measurements will be removed",
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
) {
    var creating by rememberSaveable { mutableStateOf(false) }
    var editingRuleId by rememberSaveable { mutableStateOf<Long?>(null) }
    var savePending by rememberSaveable { mutableStateOf(false) }
    val editingRule = editingRuleId?.let { id -> state.triggerRules.firstOrNull { it.id == id } }
    val now = Instant.now()
    val pending = state.triggerOccurrences.filter { it.dismissedAt == null && !it.availableAt.isAfter(now) }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 112.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            WhipPageHeader(
                title = "Automations",
                supportingText = "Choose what should become available after a Task, Habit, or workout result. Nothing is completed automatically unless the rule says so.",
            ) {
                WhipPageIconAction(Icons.Filled.Add, "Create Next-Action Automation", onClick = { creating = true })
            }
        }
        if (pending.isEmpty() && state.triggerRules.isEmpty()) item {
            WhipEmptyState(
                title = "No Next-Action Automations Yet",
                supportingText = "Create a rule when one result should prompt the next Task or Habit.",
            )
        }
        if (pending.isNotEmpty()) item { Text("Ready Now", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
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
                            WhipTextButton(onClick = {
                                if (rule.targetType == TriggerTargetType.Habit) onOpenHabit(rule.targetEntityId)
                                else onOpenTask(rule.targetEntityId)
                            }) { Text("Open") }
                            if (rule.targetType == TriggerTargetType.Habit) {
                                WhipTextButton(onClick = { viewModel.doTriggerNow(occurrence.id, rule) }) { Text("Do Now") }
                            }
                        }
                        WhipTextButton(onClick = { viewModel.dismissTriggerOccurrence(occurrence.id) }) { Text("Dismiss") }
                    }
                }
            }
        }
        if (state.triggerRules.isNotEmpty()) item { Text("Rules", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(state.triggerRules, key = { "trigger-${it.id}" }) { rule ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(rule.name, fontWeight = FontWeight.Bold)
                    Text(
                        buildString {
                            append("${triggerSourceLabel(rule, state)} → ${triggerTargetLabel(rule, state)} · ${rule.delayMinutes} min delay")
                            if (rule.action == TriggerAction.CheckOffHabit) append(" · automatic Check Off")
                            if (!rule.enabled) append(" · Paused")
                        },
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        WhipTextButton(onClick = { viewModel.setTriggerEnabled(rule, !rule.enabled) }) { Text(if (rule.enabled) "Pause" else "Resume") }
                        WhipTextButton(onClick = { editingRuleId = rule.id }) { Text("Edit") }
                        WhipTextButton(onClick = { viewModel.deleteTrigger(rule.id) }) { Text("Remove") }
                    }
                }
            }
        }
    }
    if (creating || editingRule != null) HabitAutomationDialog(
        state = state,
        initialRule = editingRule,
        saving = savePending,
        onDismiss = { if (!savePending) { creating = false; editingRuleId = null } },
        onSave = { draft ->
            if (draft.notificationEnabled) onRequestNotificationPermission()
            savePending = true
            val onFinished: (Boolean) -> Unit = { succeeded ->
                savePending = false
                if (succeeded) {
                    creating = false
                    editingRuleId = null
                }
            }
            if (editingRule == null) viewModel.createTrigger(draft, onFinished) else viewModel.updateTrigger(editingRule.id, draft, onFinished)
        },
    )
}

private fun triggerSourceLabel(rule: com.whip.app.domain.TriggerRule, state: HabitUiState): String = when (rule.sourceType) {
    LinkSourceType.Habit -> (state.all + state.today).firstOrNull { it.habit.id == rule.sourceEntityId }?.habit?.name ?: "Habit"
    LinkSourceType.Task, LinkSourceType.Subtask -> state.sourceTasks.firstOrNull { it.id == rule.sourceEntityId }?.title ?: "Task"
    LinkSourceType.Workout -> "Completed workout"
    LinkSourceType.Exercise -> "Exercise result"
    LinkSourceType.Metric -> "Measurement"
    LinkSourceType.Track -> "Track Entry"
}

private fun triggerTargetLabel(rule: com.whip.app.domain.TriggerRule, state: HabitUiState): String = when (rule.targetType) {
    TriggerTargetType.Habit -> (state.all + state.today).firstOrNull { it.habit.id == rule.targetEntityId }?.habit?.name ?: "Habit"
    TriggerTargetType.Task -> state.sourceTasks.firstOrNull { it.id == rule.targetEntityId }?.title ?: "Task"
    TriggerTargetType.Track -> "Track"
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
    var action by rememberSaveable(editorKey) { mutableStateOf(initialRule?.action ?: TriggerAction.PromptHabit) }
    var notificationEnabled by rememberSaveable(editorKey) { mutableStateOf(initialRule?.notificationEnabled ?: false) }
    val sourceId = when (sourceType) { LinkSourceType.Habit -> sourceHabitId; LinkSourceType.Task -> sourceTaskId; LinkSourceType.Workout -> 0L; else -> null }
    val targetId = if (targetType == TriggerTargetType.Habit) targetHabitId else targetTaskId
    val selectedTargetHabit = habits.firstOrNull { it.id == targetHabitId }
    val actions = if (targetType == TriggerTargetType.Task) listOf(TriggerAction.PromptTask) else buildList {
        add(TriggerAction.PromptHabit)
        if (selectedTargetHabit?.trackingMode == HabitTrackingMode.CheckOff) add(TriggerAction.CheckOffHabit)
    }
    LaunchedEffect(targetType, targetHabitId) { if (action !in actions) action = actions.first() }
    val sourceOutcomes = when (sourceType) {
        LinkSourceType.Habit -> listOf(TriggerOutcome.Recorded, TriggerOutcome.Completed, TriggerOutcome.Failed, TriggerOutcome.Skipped)
        LinkSourceType.Task -> listOf(TriggerOutcome.Completed, TriggerOutcome.Skipped)
        else -> listOf(TriggerOutcome.Completed)
    }
    LaunchedEffect(sourceType) { if (outcome !in sourceOutcomes) outcome = sourceOutcomes.first() }
    PaneAwareAlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(if (initialRule == null) "Create Next-Action Automation" else "Edit Next-Action Automation") },
        text = { LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth()) }
            item { Text("When This Happens", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
            item { EnumDropdown("Event From", listOf(LinkSourceType.Habit, LinkSourceType.Task, LinkSourceType.Workout), sourceType, { if (it == LinkSourceType.Workout) "Completed Workout" else it.name }) { selected -> sourceType = selected; outcome = if (selected == LinkSourceType.Habit) TriggerOutcome.Recorded else TriggerOutcome.Completed } }
            if (sourceType == LinkSourceType.Habit && habits.isNotEmpty()) item { EnumDropdown("Habit", habits, habits.first { it.id == sourceHabitId }, { it.name }, titleCaseValues = false) { sourceHabitId = it.id } }
            if (sourceType == LinkSourceType.Task && state.sourceTasks.isNotEmpty()) item { EnumDropdown("Task", state.sourceTasks, state.sourceTasks.first { it.id == sourceTaskId }, { it.title }, titleCaseValues = false) { sourceTaskId = it.id } }
            if (sourceOutcomes.size > 1) item { EnumDropdown("Result", sourceOutcomes, outcome, { it.name }) { outcome = it } }
            if (sourceType == LinkSourceType.Habit) item { Text("Recorded means any saved Habit result. Completed means the Habit reached its target.", style = MaterialTheme.typography.bodySmall) }
            item { Text("Then", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
            item { EnumDropdown("Next Action Type", listOf(TriggerTargetType.Habit, TriggerTargetType.Task), targetType, { it.name }) { targetType = it } }
            if (targetType == TriggerTargetType.Habit && habits.isNotEmpty()) item { EnumDropdown("Habit", habits, habits.first { it.id == targetHabitId }, { it.name }, titleCaseValues = false) { targetHabitId = it.id } }
            if (targetType == TriggerTargetType.Task && state.sourceTasks.isNotEmpty()) item { EnumDropdown("Task", state.sourceTasks, state.sourceTasks.first { it.id == targetTaskId }, { it.title }, titleCaseValues = false) { targetTaskId = it.id } }
            item { EnumDropdown("Whip Should", actions, action, { selected -> when (selected) {
                TriggerAction.PromptTask -> "Prompt to open Task"
                TriggerAction.PromptHabit -> "Prompt to open Habit"
                TriggerAction.CheckOffHabit -> "Automatically Check Off Habit"
                TriggerAction.PromptTrackEntry -> "Prompt to add Track Entry"
            } }, titleCaseValues = false) { action = it } }
            item { NumberTextField(delay, { delay = it }, "Delay Minutes") }
            item { ResponsiveFieldPair(
                first = { field -> ClockPickerButton("Quiet hours start", parseClockMinutes(quietStart), { quietStart = it?.let(::formatClockMinutes).orEmpty() }, field) },
                second = { field -> ClockPickerButton("Quiet hours end", parseClockMinutes(quietEnd), { quietEnd = it?.let(::formatClockMinutes).orEmpty() }, field) },
            ) }
            if (action == TriggerAction.CheckOffHabit) item { Text("Each eligible source event automatically Checks Off the target Habit. Choose the prompt action if confirmation is preferable.") }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Notification", fontWeight = FontWeight.Medium)
                        Text("The prompt remains visible inside Whip either way. Enable this only for an Android notification.", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(notificationEnabled, { notificationEnabled = it })
                }
            }
        } },
        confirmButton = { WhipTextButton(enabled = !saving && name.isNotBlank() && sourceId != null && targetId != null, onClick = { onSave(TriggerRuleDraft(
            name = name,
            sourceType = sourceType,
            sourceEntityId = requireNotNull(sourceId),
            outcome = outcome,
            targetType = targetType,
            targetEntityId = requireNotNull(targetId),
            delayMinutes = delay.toIntOrNull() ?: 0,
            quietStartMinutes = parseClockMinutes(quietStart),
            quietEndMinutes = parseClockMinutes(quietEnd),
            action = action,
            notificationEnabled = notificationEnabled,
            enabled = initialRule?.enabled ?: true,
        )) }) { Text(if (saving) "Saving…" else if (initialRule == null) "Create" else "Save") } },
        dismissButton = { WhipTextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } },
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
    onDecrement: () -> Unit,
    onUndo: () -> Unit,
    canUndo: Boolean = false,
    onUndoSkip: () -> Unit,
    onChecklist: (Long, Long, LocalDate, Boolean) -> Unit,
    lowPressureMode: Boolean = false,
) {
    val habit = item.habit
    val compact = LocalCompactItemLayout.current
    val skipped = item.dayState == HabitDayState.Skipped
    var showAllQuickValues by rememberSaveable(habit.id) { mutableStateOf(false) }
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
    val primaryAction: (@Composable () -> Unit)? = when {
        skipped -> {{ Text("Skipped", color = MaterialTheme.whipColors.warning, fontWeight = FontWeight.SemiBold) }}
        habit.sourceMetricId != null -> {{ Text("Synced", color = MaterialTheme.whipColors.success, fontWeight = FontWeight.SemiBold) }}
        habit.trackingMode in setOf(HabitTrackingMode.CheckOff, HabitTrackingMode.Checklist) -> {{
            Checkbox(
                checked = item.successful == true,
                onCheckedChange = { onQuick() },
                modifier = Modifier.semantics {
                    contentDescription = if (item.successful == true) {
                        "Mark habit ${habit.name} incomplete"
                    } else "Check off habit ${habit.name}"
                },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.whipColors.success),
            )
        }}
        habit.trackingMode == HabitTrackingMode.Duration -> {{
            if (compact) {
                WhipTextButton(onClick = onQuick) { Text(if (habit.timerStartedAtMillis == null) "Start" else "Stop") }
            } else {
                WhipButton(onClick = onQuick) { Text(if (habit.timerStartedAtMillis == null) "Start" else "Stop") }
            }
        }}
        habit.trackingMode in setOf(HabitTrackingMode.Count, HabitTrackingMode.Decimal) -> if (compact) {{
            WhipTextButton(onClick = { onQuickValue(habit.quickIncrement) }) {
                Text("+${editableNumericValue(habit.quickIncrement)}")
            }
        }} else null
        else -> {{
            if (compact) {
                WhipTextButton(onClick = onQuick) {
                    Text(if (habit.trackingMode == HabitTrackingMode.Rating) "Rate" else "Log")
                }
            } else {
                WhipButton(onClick = onQuick) {
                    if (habit.trackingMode == HabitTrackingMode.Rating) Text("Rate")
                    else Icon(Icons.Filled.Add, contentDescription = "Log ${habit.name}", modifier = Modifier.size(24.dp))
                }
            }
        }}
    }
    ProductivityItemCard(
        modifier = Modifier
            .clickable(onClickLabel = "Open habit details for ${habit.name}", onClick = onOpen)
            .testTag("habit-card-${habit.id}")
            .semantics { contentDescription = "Open habit details for ${habit.name}" },
        containerColor = if (item.isDoneForToday()) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
    ) {
        ProductivityItemHeader(
            itemType = "habit",
            itemName = habit.name,
            emoji = habit.icon,
            areaId = habit.areaId,
            areaName = habit.area,
            onEdit = onEdit,
            identityModifier = Modifier.testTag("habit-icon-${habit.id}"),
            primaryActionModifier = Modifier.testTag("habit-primary-action-${habit.id}"),
            editModifier = Modifier.testTag("habit-edit-action-${habit.id}"),
            supportingContent = {
                Text(
                    if (lowPressureMode) {
                        "${habit.trackingMode.uiLabel()} · ${(item.completionRate * 100).toInt()}% / $rateWindow"
                    } else {
                        "${habit.trackingMode.uiLabel()} · ${item.streak} $streakUnit streak · ${(item.completionRate * 100).toInt()}% / $rateWindow"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            primaryAction = primaryAction,
        )
            if (!compact && skipped) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Skipped Today · Streak Protected",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.whipColors.warning,
                    )
                    WhipTextButton(onClick = onUndoSkip) { Text("Undo Skip") }
                }
            }
            if (!compact && !skipped && habit.sourceMetricId == null && habit.trackingMode in setOf(HabitTrackingMode.Count, HabitTrackingMode.Decimal)) {
                val quickValues = (listOf(habit.quickIncrement) + habit.quickActions)
                    .filter { it.isFinite() && it > 0.0 }
                    .distinct()
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    (if (showAllQuickValues) quickValues else quickValues.take(3)).forEach { value ->
                        WhipButton(onClick = { onQuickValue(value) }) { Text("+${editableNumericValue(value)}") }
                    }
                    if (quickValues.size > 3) {
                        DisclosureButton(
                            label = "Quick values",
                            expanded = showAllQuickValues,
                            onClick = { showAllQuickValues = !showAllQuickValues },
                        )
                    }
                    WhipOutlinedButton(enabled = item.value > 0.0, onClick = onDecrement) {
                        Text("−${editableNumericValue(minOf(habit.quickIncrement, item.value.coerceAtLeast(0.0)))}")
                    }
                    WhipOutlinedButton(onClick = onSetValue) { Text("Set") }
                    WhipTextButton(enabled = canUndo, onClick = onUndo) { Text("Undo") }
                }
            }
            if (!compact && !skipped && habit.trackingMode == HabitTrackingMode.Checklist) {
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
                val completedItems = item.checklistItems.count { it.second }
                val totalItems = item.checklistItems.size
                if (totalItems > 0) {
                    LinearProgressIndicator(
                        progress = { completedItems.toFloat() / totalItems },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "$completedItems / $totalItems items complete",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            if (!compact && !skipped && item.flexibleScheduleTarget != null && item.flexibleScheduleProgress != null) {
                val target = item.flexibleScheduleTarget
                val fraction = (item.flexibleScheduleProgress.toFloat() / target).coerceIn(0f, 1f)
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                Text(
                    "${item.flexibleScheduleProgress} / $target completions this ${if (habit.scheduleType == HabitScheduleType.FlexibleTimesPerWeek) "week" else "month"}",
                    style = MaterialTheme.typography.labelMedium,
                )
            } else if (
                !compact &&
                !skipped &&
                habit.trackingMode != HabitTrackingMode.Checklist &&
                habit.comparison != TargetComparison.None
            ) {
                val target = habit.targetMax ?: habit.targetMin ?: 1.0
                val fraction = if (target == 0.0) 0f else (item.value / target).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                Text(
                    "${formatHabitValue(item.value, habit.precision)} / ${formatHabitValue(target, habit.precision)} ${habit.unitId.unitLabel()}",
                    style = MaterialTheme.typography.labelMedium,
                )
            } else if (!compact && !skipped && item.value != 0.0) {
                Text("${formatHabitValue(item.value, habit.precision)} ${habit.unitId.unitLabel()}")
            }
            if (!compact && habit.sourceMetricId != null) {
                Text("Read-only source: Health Connect · provenance is retained per entry", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
    }
}

fun quickHabitAction(item: HabitDayProgress, vm: HabitViewModel, openNumeric: () -> Unit) {
    if (item.habit.sourceMetricId != null) return
    when (item.habit.trackingMode) {
        HabitTrackingMode.CheckOff -> vm.setCheckOff(item.habit.id, item.date, item.successful != true)
        HabitTrackingMode.Count, HabitTrackingMode.Decimal -> vm.log(item.habit.id, item.habit.quickIncrement)
        HabitTrackingMode.Duration -> if (item.habit.timerStartedAtMillis == null) vm.startTimer(item.habit.id) else vm.stopTimer(item.habit.id)
        HabitTrackingMode.Checklist -> vm.setCheckOff(item.habit.id, item.date, item.successful != true)
        HabitTrackingMode.Rating, HabitTrackingMode.LogOnly -> openNumeric()
    }
}

internal data class DailyHabitSections(
    val remaining: List<HabitDayProgress>,
    val done: List<HabitDayProgress>,
)

internal fun HabitDayProgress.isDoneForToday(): Boolean =
    dayState == HabitDayState.Completed || successful == true

internal fun List<HabitDayProgress>.dailyHabitSections(): DailyHabitSections {
    val (done, remaining) = partition(HabitDayProgress::isDoneForToday)
    return DailyHabitSections(remaining = remaining, done = done)
}

@Composable
internal fun DoneHabitsDisclosure(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DisclosureRow(
        title = "Done ($count)",
        supportingText = "Completed today · expand to review or undo.",
        expanded = expanded,
        onClick = onToggle,
        modifier = modifier.testTag("habit-done-disclosure"),
    )
}

@Composable
private fun HabitList(
    title: String,
    subtitle: String,
    progress: List<HabitDayProgress>,
    empty: String,
    onTemplates: () -> Unit,
    onOpen: (HabitDayProgress) -> Unit,
    onEdit: (HabitDayProgress) -> Unit,
    onQuick: (HabitDayProgress) -> Unit,
    onQuickValue: (HabitDayProgress, Double) -> Unit,
    onSetValue: (HabitDayProgress) -> Unit,
    onDecrement: (HabitDayProgress) -> Unit,
    onUndo: (HabitDayProgress) -> Unit,
    canUndo: (HabitDayProgress) -> Boolean,
    onUndoSkip: (HabitDayProgress) -> Unit,
    onChecklist: (Long, Long, LocalDate, Boolean) -> Unit,
    onReorder: ((List<Long>) -> Unit)?,
    lowPressureMode: Boolean,
    separateCompleted: Boolean = false,
) {
    val compact = LocalCompactItemLayout.current
    var manageOrder by rememberSaveable { mutableStateOf(false) }
    var toolsExpanded by rememberSaveable { mutableStateOf(false) }
    val sections = if (separateCompleted) progress.dailyHabitSections() else DailyHabitSections(progress, emptyList())
    val doneIds = sections.done.mapTo(linkedSetOf()) { it.habit.id }
    val dateKey = progress.firstOrNull()?.date?.toEpochDay() ?: Long.MIN_VALUE
    var doneExpanded by rememberSaveable(title, dateKey) {
        mutableStateOf(sections.remaining.isEmpty() && sections.done.isNotEmpty())
    }
    var knownDoneIds by remember(title, dateKey) { mutableStateOf(doneIds) }
    LaunchedEffect(doneIds) {
        if ((doneIds - knownDoneIds).isNotEmpty()) doneExpanded = true
        if (doneIds.isEmpty()) doneExpanded = false
        knownDoneIds = doneIds
    }
    BackHandler(enabled = manageOrder) { manageOrder = false }
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("habit-list-$title"),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 112.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 10.dp),
    ) {
        item {
            WhipPageHeader(title = title, supportingText = subtitle) {
                Box {
                    WhipPageIconAction(
                        icon = Icons.Outlined.MoreVert,
                        label = "More Habit Actions",
                        onClick = { toolsExpanded = true },
                    )
                    DropdownMenu(expanded = toolsExpanded, onDismissRequest = { toolsExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Browse Templates") },
                            onClick = { toolsExpanded = false; onTemplates() },
                        )
                        if (onReorder != null && progress.size > 1) DropdownMenuItem(
                            text = { Text("Reorder Habits") },
                            onClick = { toolsExpanded = false; manageOrder = true },
                        )
                    }
                }
            }
        }
        if (manageOrder) item {
            ModeButton(
                label = "Reorder Habits",
                active = true,
                onClick = { manageOrder = false },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (progress.isEmpty()) item {
            WhipEmptyState(
                title = "No Habits Here",
                supportingText = empty,
                primaryActionLabel = "Browse Templates",
                onPrimaryAction = onTemplates,
            )
        }
        if (separateCompleted && sections.remaining.isEmpty() && sections.done.isNotEmpty()) item {
            WhipEmptyState(
                title = "All Done for Today",
                supportingText = "Your completed habits remain below if you need to review or undo one.",
            )
        }
        items(sections.remaining, key = { it.habit.id }) { item ->
            val index = sections.remaining.indexOfFirst { it.habit.id == item.habit.id }
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
                    onUndoSkip = { onUndoSkip(item) },
                    onChecklist = onChecklist,
                    lowPressureMode = lowPressureMode,
                )
                if (manageOrder && onReorder != null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        WhipTextButton(enabled = index > 0, onClick = {
                            val ids = sections.remaining.map { it.habit.id }.toMutableList()
                            java.util.Collections.swap(ids, index, index - 1)
                            onReorder(ids)
                        }) { Icon(Icons.Outlined.ArrowUpward, contentDescription = null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(4.dp)); Text("Earlier") }
                        WhipTextButton(enabled = index in 0 until sections.remaining.lastIndex, onClick = {
                            val ids = sections.remaining.map { it.habit.id }.toMutableList()
                            java.util.Collections.swap(ids, index, index + 1)
                            onReorder(ids)
                        }) { Icon(Icons.Outlined.ArrowDownward, contentDescription = null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(4.dp)); Text("Later") }
                    }
                }
            }
        }
        if (sections.done.isNotEmpty()) {
            item {
                DoneHabitsDisclosure(
                    count = sections.done.size,
                    expanded = doneExpanded,
                    onToggle = { doneExpanded = !doneExpanded },
                )
            }
            if (doneExpanded) items(sections.done, key = { it.habit.id }) { item ->
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
                    onUndoSkip = { onUndoSkip(item) },
                    onChecklist = onChecklist,
                    lowPressureMode = lowPressureMode,
                )
            }
        }
    }
}

@Composable
private fun HabitInsights(state: HabitUiState, lowPressureMode: Boolean) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 112.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            WhipPageHeader(
                title = "Habit Insights",
                supportingText = "Consistency, streaks, and logged activity for each habit.",
            )
        }
        if (state.all.isEmpty()) item {
            WhipEmptyState(
                title = "No Habit Insights Yet",
                supportingText = "Create and check in to a habit to build insight over time.",
            )
        }
        items(state.all, key = { it.habit.id }) { item ->
            ProductivityItemCard {
                    ProductivityItemHeader(
                        itemType = "habit",
                        itemName = item.habit.name,
                        emoji = item.habit.icon,
                        areaId = item.habit.areaId,
                        areaName = item.habit.area,
                        onEdit = null,
                        identityModifier = Modifier.testTag("habit-insight-icon-${item.habit.id}"),
                        supportingContent = {
                            Text(
                                item.habit.trackingMode.uiLabel(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                    val habitLogs = state.logs.filter { it.habitId == item.habit.id }
                    val habitSkips = state.skips.filter { it.habitId == item.habit.id }
                    val habitPauses = state.pauses.filter { it.habitId == item.habit.id }
                    if (habitLogs.isEmpty() && habitSkips.isEmpty()) {
                        Text(
                            "No activity yet. Check off or log this habit to build consistency and recent-activity insights.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        if (!lowPressureMode) Text("Current streak: ${item.streak}")
                        Text("30-day completion: ${(item.completionRate * 100).toInt()}%")
                        val successfulDates = item.habit.successfulPeriodOutcomeDates(
                            habitLogs,
                            item.habit.startDate,
                            state.currentDate,
                            habitPauses,
                            state.customUnits,
                            habitSkips,
                        ).size
                        val values = habitLogs.asSequence()
                            .filter { it.status in setOf(HabitLogStatus.Recorded, HabitLogStatus.Success) }
                            .mapNotNull { it.valueInUnit(item.habit.unitId, state.customUnits) }
                            .toList()
                        Text(when (item.habit.trackingMode) {
                            HabitTrackingMode.CheckOff, HabitTrackingMode.Checklist -> "Completed $successfulDates Time${if (successfulDates == 1) "" else "s"}"
                            HabitTrackingMode.Rating -> "Average Rating: ${formatHabitValue(values.average().takeUnless(Double::isNaN) ?: 0.0, item.habit.precision)}"
                            HabitTrackingMode.LogOnly -> "Entries Logged: ${habitLogs.count { it.status != HabitLogStatus.Failed }}"
                            else -> "All-Time Logged: ${formatHabitValue(values.sum(), item.habit.precision)} ${item.habit.unitId.unitLabel()}"
                        })
                        val scheduledStates = (29L downTo 0L).map { state.currentDate.minusDays(it) }
                            .filter(item.habit::isScheduledOn)
                            .map { day -> item.habit.dayStateOn(day, state.currentDate, habitLogs, habitPauses, habitSkips, state.customUnits) }
                        val completedDays = scheduledStates.count { it == HabitDayState.Completed }
                        val skippedDays = scheduledStates.count { it == HabitDayState.Skipped }
                        val missedDays = scheduledStates.count { it in setOf(HabitDayState.Missed, HabitDayState.BelowTarget) }
                        Text("Last 30 Days: $completedDays Completed · $skippedDays Skipped · $missedDays Missed/Below Target")
                        val weeklyRates = (7L downTo 0L).map { weeksAgo ->
                            val start = state.currentDate.minusWeeks(weeksAgo)
                                .with(TemporalAdjusters.previousOrSame(item.habit.weekStart))
                            val end = minOf(start.plusDays(6), state.currentDate)
                            val scheduled = generateSequence(start) { it.plusDays(1) }
                                .takeWhile { !it.isAfter(end) }
                                .filter(item.habit::isScheduledOn)
                                .toList()
                            val outcomes = scheduled.mapNotNull { day ->
                                when (item.habit.dayStateOn(day, end, habitLogs, habitPauses, habitSkips, state.customUnits)) {
                                    HabitDayState.Completed -> true
                                    HabitDayState.Missed, HabitDayState.BelowTarget -> false
                                    else -> null
                                }
                            }
                            if (outcomes.isEmpty()) null else outcomes.count { it }.toDouble() / outcomes.size
                        }
                        Text("Eight-Week Consistency", style = MaterialTheme.typography.labelMedium)
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
                        val days = (27L downTo 0L).map { state.currentDate.minusDays(it) }
                        Text("Recent Activity", style = MaterialTheme.typography.labelMedium)
                        val calendarDescription = days.joinToString("; ") { day ->
                            "$day: " + when (item.habit.dayStateOn(day, state.currentDate, habitLogs, habitPauses, habitSkips, state.customUnits)) {
                                HabitDayState.Completed -> "completed"
                                HabitDayState.Skipped -> "skipped"
                                HabitDayState.Missed -> "missed"
                                HabitDayState.BelowTarget -> "below target"
                                HabitDayState.Pending -> "pending"
                                HabitDayState.Paused -> "paused"
                                HabitDayState.NotScheduled -> "not scheduled"
                            }
                        }
                        Text(
                            days.chunked(7).joinToString(" ") { week -> week.joinToString("") { day ->
                                when (item.habit.dayStateOn(day, state.currentDate, habitLogs, habitPauses, habitSkips, state.customUnits)) {
                                    HabitDayState.Completed -> "✓"
                                    HabitDayState.Skipped -> "○"
                                    HabitDayState.Missed, HabitDayState.BelowTarget -> "×"
                                    else -> "·"
                                }
                            } },
                            modifier = Modifier.semantics { contentDescription = calendarDescription },
                        )
                        Text("✓ completed · ○ skipped · × missed/below target · · pending, paused, or not scheduled", style = MaterialTheme.typography.labelSmall)
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 112.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            WhipPageHeader(
                title = "Archived Habits",
                supportingText = "Habits kept for history but removed from active check-ins.",
            )
        }
        if (visible.isEmpty()) item {
            WhipEmptyState(
                title = "No Archived Habits",
                supportingText = "Archived habits will appear here and can be restored or edited.",
            )
        }
        items(visible, key = Habit::id) { habit ->
            ProductivityItemCard(
                modifier = Modifier
                    .clickable(onClickLabel = "Open habit details for ${habit.name}") { onOpen(habit) }
                    .semantics { contentDescription = "Open habit details for ${habit.name}" },
            ) {
                ProductivityItemHeader(
                    itemType = "habit",
                    itemName = habit.name,
                    emoji = habit.icon,
                    areaId = habit.areaId,
                    areaName = habit.area,
                    onEdit = { onEdit(habit) },
                    identityModifier = Modifier.testTag("habit-icon-${habit.id}"),
                    editModifier = Modifier.testTag("habit-edit-action-${habit.id}"),
                    supportingContent = {
                        Text(
                            "Archived",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
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
            name = "Hydration", icon = "💧", trackingMode = HabitTrackingMode.Count,
            dimension = UnitDimension.Count, unitId = "glass", targetMin = 8.0,
            quickIncrement = 1.0, quickActions = listOf(1.0, 2.0), startDate = today,
        ),
        "Medication" to HabitDraft(
            name = "Medication",
            icon = "💊",
            trackingMode = HabitTrackingMode.Checklist,
            checklistItems = listOf(
                HabitChecklistItemDraft("Medication 1", 0),
                HabitChecklistItemDraft("Medication 2", 1),
                HabitChecklistItemDraft("Medication 3", 2),
            ),
            startDate = today,
        ),
        "Reading" to HabitDraft(
            name = "Reading", icon = "📚", trackingMode = HabitTrackingMode.Count,
            dimension = UnitDimension.Count, unitId = "page", targetMin = 20.0,
            quickIncrement = 5.0, quickActions = listOf(5.0, 10.0, 20.0), startDate = today,
        ),
        "Meditation" to HabitDraft(
            name = "Meditation", icon = "🧘", trackingMode = HabitTrackingMode.Duration,
            dimension = UnitDimension.Duration, unitId = "minute", targetMin = 10.0,
            quickIncrement = 5.0, startDate = today,
        ),
        "No-spend day" to HabitDraft(
            name = "No-spend day", icon = "💰", trackingMode = HabitTrackingMode.CheckOff,
            targetPeriod = TargetPeriod.Occurrence, startDate = today,
        ),
        "Exercise 3× weekly" to HabitDraft(
            name = "Exercise", icon = "💪", trackingMode = HabitTrackingMode.CheckOff,
            scheduleType = HabitScheduleType.FlexibleTimesPerWeek, flexibleTimesPerWeek = 3,
            targetPeriod = TargetPeriod.Week, targetMin = 3.0, startDate = today,
        ),
        "Daily rating" to HabitDraft(
            name = "Daily rating", icon = "⭐", trackingMode = HabitTrackingMode.Rating,
            comparison = TargetComparison.None,
            dimension = UnitDimension.Unitless, unitId = "unitless", targetMin = null,
            precision = 0, startDate = today,
        ),
    )
    PaneAwareAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Habit Templates") },
        text = {
            LazyColumn {
                item { Text("Templates create ordinary habits that you can fully edit afterward.") }
                items(templates, key = { it.first }) { (label, draft) ->
                    WhipTextButton(onClick = { onChoose(draft) }, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(label, fontWeight = FontWeight.SemiBold)
                            Text(habitTemplateDescription(label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun habitTemplateDescription(label: String): String = when (label) {
    "Hydration" -> "Count toward 8 glasses per day with +1 and +2 buttons."
    "Medication" -> "Three editable items; checking the final one completes the Habit."
    "Reading" -> "Add pages toward a daily target of 20."
    "Meditation" -> "Track minutes toward a daily duration target."
    "No-spend day" -> "Check it off after a day without spending."
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
    quickIncrement = quickIncrement,
    quickActions = quickActions,
    reminderMinutes = reminderMinutes,
    weekdayReminderMinutes = weekdayReminderMinutes,
    weekStart = weekStart,
    checklistItems = checklist,
    autoCompleteFromItems = autoCompleteFromItems,
    sourceMetricId = sourceMetricId,
)

@Composable
private fun HabitEditorDialog(
    habit: Habit?,
    modifier: Modifier = Modifier,
    initialDraft: HabitDraft? = null,
    initialChecklist: List<HabitChecklistItemDraft>,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (HabitDraft) -> Unit,
    defaultWeekStart: DayOfWeek = DayOfWeek.MONDAY,
    defaults: AppSettings = AppSettings(),
    customUnits: List<UnitDefinition> = emptyList(),
    sourceMetrics: List<MetricDefinition> = emptyList(),
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
    val baseInitial = initialDraft ?: habit?.toEditorDraft(initialChecklist) ?: HabitDraft(
        name = "",
        startDate = today,
        weekStart = defaultWeekStart,
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
    var endType by rememberSaveable(editorKey) { mutableStateOf(initial.endType) }
    var endDate by rememberSaveable(editorKey) { mutableStateOf(initial.endDate) }
    var endValue by rememberSaveable(editorKey) { mutableStateOf(initial.endValue?.let(::editableNumericValue).orEmpty()) }
    var weekStart by rememberSaveable(editorKey) { mutableStateOf(initial.weekStart) }
    var showEndDatePicker by rememberSaveable(editorKey) { mutableStateOf(false) }
    var unitId by rememberSaveable(editorKey) { mutableStateOf(initial.unitId) }
    var dimension by rememberSaveable(editorKey) { mutableStateOf(initial.dimension) }
    var precision by rememberSaveable(editorKey) { mutableStateOf(initial.precision.toString()) }
    var sourceMetricId by rememberSaveable(editorKey) { mutableStateOf(initial.sourceMetricId) }
    var checklistDrafts by rememberSaveable(editorKey) {
        mutableStateOf<List<HabitChecklistItemDraft>>(initial.checklistItems.toList())
    }
    var autoCompleteFromItems by rememberSaveable(editorKey) {
        mutableStateOf(initial.autoCompleteFromItems)
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
                initial.quickActions.isNotEmpty() ||
                initial.reminderMinutes.isNotEmpty() || initial.weekdayReminderMinutes.isNotEmpty() ||
                initial.endType != HabitEndType.Never,
        )
    }
    val editorFingerprint = listOf(
        name, notes, areaId, area, tags, icon, mode, comparison, targetMin, targetMax,
        targetPeriod, schedule, interval, weekdays.sortedBy { it.value }, flexible, rollingDays,
        quickIncrement, quickActions, reminders, weekdayReminders, endType,
        endDate, endValue, weekStart, unitId, dimension, precision, sourceMetricId,
        checklistDrafts.map { "${it.id}:${it.uuid}:${it.position}:${it.name}" }, autoCompleteFromItems,
    ).joinToString("\u001f")
    val initialFingerprint by rememberSaveable(editorKey) { mutableStateOf(editorFingerprint) }
    var showDiscardConfirmation by rememberSaveable(editorKey) { mutableStateOf(false) }
    val requestDismiss = { if (editorFingerprint != initialFingerprint) showDiscardConfirmation = true else onDismiss() }
    BackHandler(enabled = !showDiscardConfirmation, onBack = requestDismiss)
    ProductivityEditorDialog(
        modifier = modifier,
        testTag = "habit-editor-surface",
        primary = true,
        onDismissRequest = requestDismiss,
        title = { Text(if (habit == null) "Create Habit" else "Edit Habit") },
        text = {
            LazyColumn(
                modifier = Modifier.testTag("habit-editor-fields"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { EditorSectionHeader("Basics", "Name this Habit and choose the emoji used across Whip.") }
                item { OutlinedTextField(name, { name = it }, label = { Text("Name *") }, modifier = Modifier.fillMaxWidth().testTag("habit-editor-name")) }
                item {
                    WhipEmojiPicker(
                        value = icon,
                        defaultEmoji = DEFAULT_HABIT_EMOJI,
                        onValueChange = { icon = it },
                        modifier = Modifier.fillMaxWidth(),
                        customEmojis = customIdentityEmojis,
                        onSaveEmoji = onSaveIdentityEmoji,
                        onRemoveSavedEmoji = onRemoveSavedIdentityEmoji,
                    )
                }
                if (sourceMetrics.isNotEmpty()) item {
                    EnumDropdown(
                        "Data source",
                        listOf<MetricDefinition?>(null) + sourceMetrics,
                        sourceMetrics.firstOrNull { it.id == sourceMetricId },
                        { metric -> metric?.let { "Health Connect · ${it.name}" } ?: "Manual Check-Ins" },
                        titleCaseValues = false,
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
                        "Health Connect determines tracking mode and units. Imported records are read-only and reconcile by source ID without double counting.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                item { EditorSectionHeader("Tracking", "Choose the daily action first; its targets and measurement fields stay directly below it.") }
                item {
                    Text("How do you want to track it?", fontWeight = FontWeight.Bold)
                    Text("Choose the action you want available each day. Whip fills in sensible defaults.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        HabitTrackingMode.entries.forEach { selected ->
                            WhipFilterChip(
                                selected = mode == selected,
                                enabled = sourceMetricId == null,
                                onClick = {
                                    mode = selected
                                    when (selected) {
                                        HabitTrackingMode.Duration -> { unitId = "second"; dimension = UnitDimension.Duration; precision = "0"; comparison = TargetComparison.AtLeast }
                                        HabitTrackingMode.Rating -> { unitId = "unitless"; dimension = UnitDimension.Unitless; precision = defaults.numberPrecision.toString(); comparison = TargetComparison.None }
                                        HabitTrackingMode.Decimal -> { unitId = "unitless"; dimension = UnitDimension.Unitless; precision = defaults.numberPrecision.toString(); comparison = TargetComparison.AtLeast }
                                        HabitTrackingMode.LogOnly -> { unitId = "unitless"; dimension = UnitDimension.Unitless; precision = defaults.numberPrecision.toString(); comparison = TargetComparison.None }
                                        HabitTrackingMode.CheckOff, HabitTrackingMode.Checklist -> { unitId = "count"; dimension = UnitDimension.Count; precision = "0"; comparison = TargetComparison.AtLeast; targetMin = "1"; targetPeriod = TargetPeriod.Occurrence }
                                        HabitTrackingMode.Count -> { unitId = "count"; dimension = UnitDimension.Count; precision = "0"; comparison = TargetComparison.AtLeast }
                                    }
                                },
                                label = { Text(selected.uiLabel()) },
                            )
                        }
                    }
                    AvailabilityNotice(
                        label = "Tracking mode",
                        availability = ControlAvailability(
                            enabled = sourceMetricId == null,
                            unavailableExplanation = "Health Connect determines this value. Set Data Source to Manual Check-Ins to change it.",
                        ),
                    )
                }
                if (mode == HabitTrackingMode.Checklist) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Checklist Items", fontWeight = FontWeight.Bold)
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
                            WhipOutlinedButton(
                                onClick = {
                                    checklistDrafts = ArrayList(checklistDrafts).also {
                                        it += HabitChecklistItemDraft("", it.size, uuid = java.util.UUID.randomUUID().toString())
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Add Checklist Item")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Complete Habit With Final Item", fontWeight = FontWeight.Medium)
                                    Text(
                                        "Automatically complete the parent Habit when every checklist item is checked.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = autoCompleteFromItems,
                                    onCheckedChange = { autoCompleteFromItems = it },
                                    modifier = Modifier.testTag("habit-auto-complete-from-items"),
                                )
                            }
                            if (checklistDrafts.none { it.name.isNotBlank() }) {
                                Text(
                                    "Add at least one checklist item.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
                if (sourceMetricId == null && mode in setOf(HabitTrackingMode.Count, HabitTrackingMode.Decimal)) {
                    item {
                        NumberTextField(quickIncrement, { quickIncrement = it }, "Quick increment")
                        Text(
                            "This is the amount added by the habit's one-tap logging action.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!quickIncrementValid) item {
                        Text("Quick increment must be a positive number.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (mode in setOf(HabitTrackingMode.CheckOff, HabitTrackingMode.Checklist)) {
                    item {
                        DependentSettingsNotice(
                            message = if (mode == HabitTrackingMode.CheckOff) {
                                "One check completes each scheduled occurrence. No numeric value is required."
                            } else {
                                if (autoCompleteFromItems) {
                                    "The final checklist item completes the scheduled occurrence. You can also complete the parent Habit directly."
                                } else {
                                    "Checklist items stay independent. Complete the parent Habit directly when the occurrence is done."
                                }
                            },
                            testTag = "habit-checkoff-consequence",
                        )
                    }
                } else {
                    if (sourceMetricId == null && mode != HabitTrackingMode.Rating) {
                        item {
                            Text("Measurement Unit", fontWeight = FontWeight.Bold)
                            Text(
                                "This unit is used by targets, check-ins, and history.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        item {
                            EnumDropdown("Measurement type", UnitDimension.entries, dimension, UnitDimension::uiLabel) { selected ->
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
                        item {
                            UnitSelectionField(
                                units = BuiltInUnits.all + customUnits,
                                selectedUnitId = unitId,
                                dimension = dimension,
                                onSelect = { unitId = it },
                                onCreateUnit = onCreateCustomUnit,
                                dialogModifier = modifier,
                            )
                        }
                        item { NumberTextField(precision, { precision = it }, "Decimal places (0–6)") }
                    }
                    item { EnumDropdown("Target rule", TargetComparison.entries, comparison, TargetComparison::displayLabel) { comparison = it } }
                    if (comparison != TargetComparison.None) {
                        item {
                            if (comparison == TargetComparison.WithinRange) {
                                ResponsiveFieldPair(
                                    first = { field -> NumberTextField(targetMin, { targetMin = it }, "Minimum", field) },
                                    second = { field -> NumberTextField(targetMax, { targetMax = it }, "Maximum", field) },
                                )
                            } else NumberTextField(
                                targetMin,
                                { targetMin = it },
                                when (targetPeriod) {
                                    TargetPeriod.Week -> "Target per week"
                                    TargetPeriod.Month -> "Target per month"
                                    TargetPeriod.Occurrence -> "Target each time"
                                    else -> "Target per day"
                                },
                            )
                        }
                        item {
                            EnumChips("Target period", TargetPeriod.entries, targetPeriod, TargetPeriod::displayLabel) { targetPeriod = it }
                            Text(targetPeriod.explanation(schedule), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (targetPeriod == TargetPeriod.RollingDays) item { NumberTextField(rollingDays, { rollingDays = it }, "Rolling-day window") }
                    }
                }
                item { EditorSectionHeader("Schedule", "Choose when this Habit is expected, then configure only the settings that schedule enables.") }
                item {
                    EnumDropdown("Schedule", HabitScheduleType.entries, schedule, { it.scheduleLabel() }) { schedule = it }
                    DependentSettingsNotice(
                        message = when (schedule) {
                            HabitScheduleType.EveryNDays -> "The interval that controls this schedule appears next."
                            HabitScheduleType.SelectedWeekdays -> "Only the weekdays you select next create scheduled check-ins."
                            HabitScheduleType.FlexibleTimesPerWeek -> "Set the allowed check-ins in Times per Week next."
                            HabitScheduleType.FlexibleTimesPerMonth -> "Set the allowed check-ins in Times per Month next."
                            else -> "This cadence controls when the habit is expected and when reminders can apply."
                        },
                        testTag = "habit-schedule-consequence",
                    )
                }
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
                                WhipFilterChip(selected = day in weekdays, onClick = { weekdays = if (day in weekdays) weekdays - day else weekdays + day }, label = { Text(day.name.take(2)) })
                            }
                        }
                    }
                }
                item {
                    val parsed = reminders.split(',').mapNotNull { parseClockMinutes(it.trim()) }.distinct().sorted()
                    ReminderTimesEditor("Default Reminders", parsed) { updated ->
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
                item { EnumDropdown("End Condition", HabitEndType.entries, endType, { it.scheduleLabel() }) { endType = it } }
                if (endType == HabitEndType.OnDate) item { WhipOutlinedButton(onClick = { showEndDatePicker = true }, modifier = Modifier.fillMaxWidth()) { Text(endDate?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: "Choose End Date") } }
                if (endType in setOf(HabitEndType.AfterStreak, HabitEndType.AfterCompletions, HabitEndType.AfterTotal)) item { NumberTextField(endValue, { endValue = it }, when (endType) { HabitEndType.AfterStreak -> "End After Streak"; HabitEndType.AfterCompletions -> "End After Completions"; else -> "End After Total" }) }
                item { EnumDropdown("First Day of Week", DayOfWeek.entries, weekStart, { it.name.lowercase().replaceFirstChar(Char::uppercase) }) { weekStart = it } }
                item { EditorSectionHeader("Organization", "Choose the Area that owns this Habit.") }
                item {
                    AreaPicker(
                        areas = areas,
                        selectedAreaId = areaId,
                        selectedAreaName = area,
                        onSelect = { id, value -> areaId = id; area = value },
                        onCreateArea = onCreateArea,
                        modifier = Modifier.fillMaxWidth(),
                        dialogModifier = modifier,
                        inheritedFromScope = habit == null && initialDraft?.areaId == null && defaultAreaId != null,
                    )
                }
                item {
                    DisclosureButton(
                        label = "More Details",
                        expanded = showAdvanced,
                        onClick = { showAdvanced = !showAdvanced },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (showAdvanced) {
                    item { EditorSectionHeader("Details", "Add reusable tags, quick actions, and notes only when they help.") }
                    item { OutlinedTextField(tags, { tags = it }, label = { Text("Tags, comma-separated") }, modifier = Modifier.fillMaxWidth()) }
                    if (sourceMetricId == null) item {
                        NumericQuickActionBuilder(
                            values = quickActionResult.values,
                            increment = quickIncrement,
                            rawSpecification = quickActions,
                            onSpecificationChange = { quickActions = it },
                        )
                        quickActionResult.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    }
                    item { OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth()) }
                }
            }
        },
        confirmButton = {
            WhipButton(
                enabled = name.isNotBlank() &&
                    quickActionResult.error == null &&
                    quickIncrementValid &&
                    (mode != HabitTrackingMode.Checklist || checklistDrafts.any { it.name.isNotBlank() }) &&
                    !saving,
                onClick = {
                    val draft = HabitDraft(
                            name = name,
                            notes = notes,
                            areaId = areaId,
                            area = area,
                            tags = tags.split(',').map(String::trim).filter(String::isNotBlank),
                            icon = icon.ifBlank { DEFAULT_HABIT_EMOJI },
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
                            quickIncrement = quickIncrement.toWhipDoubleOrNull() ?: 1.0,
                            quickActions = quickActionResult.values,
                            reminderMinutes = reminders.split(',').mapNotNull { parseClockMinutes(it.trim()) },
                            weekdayReminderMinutes = parseWeekdayReminderMap(weekdayReminders),
                            weekStart = weekStart,
                            checklistItems = checklistDrafts.filter { it.name.isNotBlank() }
                                .mapIndexed { index, item -> item.copy(name = item.name.trim(), position = index) },
                            autoCompleteFromItems = autoCompleteFromItems,
                            sourceMetricId = sourceMetricId,
                        )
                    onSave(draft)
                },
            ) { Text(if (saving) "Saving…" else "Save") }
        },
        dismissButton = {
            IconButton(
                onClick = requestDismiss,
                enabled = !saving,
                modifier = Modifier.semantics { contentDescription = "Cancel Habit editing" },
            ) { Icon(Icons.Outlined.Close, contentDescription = null) }
        },
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
    PaneAwareAlertDialog(
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
                            WhipTextButton(onClick = { value = label }) { Text(label) }
                        }
                    }
                }
            }
        },
        confirmButton = { WhipTextButton(enabled = value.toWhipDoubleOrNull()?.isFinite() == true, onClick = { onLog(requireNotNull(value.toWhipDoubleOrNull()), note) }) { Text("Set") } },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
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
    PaneAwareAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (log == null) "Add Past ${item.habit.name} Entry" else "Edit ${item.habit.name} Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EnumDropdown("State", HabitLogStatus.entries, status, { it.name }) { status = it }
                NumberTextField(
                    value,
                    { value = it },
                    "Value (${(log?.enteredUnitId ?: item.habit.unitId).unitLabel()})",
                )
                WhipOutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
                }
                OutlinedTextField(note, { note = it }, label = { Text("Optional note") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            WhipTextButton(
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
                if (onDelete != null) WhipTextButton(onClick = onDelete) { Text("Delete") }
                WhipTextButton(onClick = onDismiss) { Text("Cancel") }
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
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onPin: () -> Unit,
    onPause: () -> Unit,
    onSchedulePause: () -> Unit,
    onSkip: () -> Unit,
    onUndoSkip: () -> Unit,
    logs: List<HabitLog>,
    skips: List<HabitSkip>,
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
        modifier = modifier.testTag("habit-detail-surface"),
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
                            else if (item.dayState == HabitDayState.Skipped) "Skipped Today · Streak Protected"
                            else if (lowPressureMode) "${formatHabitValue(item.value, item.habit.precision)} logged"
                            else "${formatHabitValue(item.value, item.habit.precision)} logged · streak ${item.streak}",
                        )
                        if (!item.habit.archived) {
                            listOf(
                                (if (item.habit.paused) "Resume" else "Pause Indefinitely") to onPause,
                                "Schedule Pause Dates" to onSchedulePause,
                            ).forEach { (label, action) -> WhipTextButton(onClick = action, modifier = Modifier.fillMaxWidth()) { Text(label) } }
                            when {
                                item.dayState == HabitDayState.Skipped -> WhipTextButton(
                                    onClick = onUndoSkip,
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("Undo Skip") }
                                item.dayState == HabitDayState.Pending &&
                                    item.habit.sourceMetricId == null &&
                                    item.habit.scheduleType !in setOf(
                                        HabitScheduleType.FlexibleTimesPerWeek,
                                        HabitScheduleType.FlexibleTimesPerMonth,
                                    ) -> WhipTextButton(
                                    onClick = onSkip,
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("Skip Today") }
                            }
                        }
                    }
                    HabitDetailSection.History -> {
                        WhipTextButton(onClick = onAddHistoricalLog, modifier = Modifier.fillMaxWidth()) { Text("Add Backdated Entry") }
                        val events = (logs.map(HabitHistoryEvent::Log) + skips.map(HabitHistoryEvent::Skip))
                            .sortedByDescending(HabitHistoryEvent::timeMillis)
                        if (events.isEmpty()) Text("No entries yet.") else {
                            Text("Recent Entries", style = MaterialTheme.typography.labelMedium)
                            events.take(visibleLogs).forEach { event ->
                                when (event) {
                                    is HabitHistoryEvent.Log -> WhipTextButton(onClick = { onEditLog(event.value) }, modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            "${event.value.localDate}: ${event.value.value?.let { formatHabitValue(it, item.habit.precision) } ?: event.value.status.name}${if (event.value.note.isBlank()) "" else " · ${event.value.note}"}",
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                    is HabitHistoryEvent.Skip -> Text(
                                        "${event.value.localDate}: Skipped",
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (visibleLogs < events.size) WhipTextButton(
                                onClick = { visibleLogs = (visibleLogs + 25).coerceAtMost(events.size) },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Show 25 More · ${events.size - visibleLogs} Remaining") }
                        }
                    }
                    HabitDetailSection.Connections -> {
                        if (linkedGoals.isEmpty()) Text("This habit does not feed any goals yet.")
                        else Text("Adds Progress To: ${linkedGoals.distinct().joinToString()}", style = MaterialTheme.typography.bodyMedium)
                        if (!item.habit.archived) WhipTextButton(onClick = onLinkGoal, modifier = Modifier.fillMaxWidth()) { Text("Add Progress to a Goal") }
                    }
                    HabitDetailSection.More -> {
                        if (!item.habit.archived) {
                            listOf(
                                "Duplicate" to onDuplicate,
                                (if (item.habit.pinned) "Unpin" else "Pin") to onPin,
                            ).forEach { (label, action) -> WhipTextButton(onClick = action, modifier = Modifier.fillMaxWidth()) { Text(label) } }
                        }
                        WhipTextButton(onClick = onArchive, modifier = Modifier.fillMaxWidth()) {
                            Text(if (item.habit.archived) "Restore" else "Archive")
                        }
                        WhipTextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                            Text("Delete Permanently", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = { WhipTextButton(onClick = onDismiss) { Text("Close") } },
        dismissButton = { DetailEditButton("Edit habit", onEdit) },
    )
}

private sealed interface HabitHistoryEvent {
    val timeMillis: Long

    data class Log(val value: HabitLog) : HabitHistoryEvent {
        override val timeMillis: Long = value.timestamp.toEpochMilli()
    }

    data class Skip(val value: HabitSkip) : HabitHistoryEvent {
        override val timeMillis: Long = value.skippedAtMillis
    }
}

private enum class HabitDetailSection(val label: String) {
    Today("Today"),
    History("History"),
    Connections("Automation"),
    More("Options"),
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
    PaneAwareAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect ${habit.name} to a Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (goals.isEmpty()) Text("Create an active goal first.") else {
                    EnumDropdown("Goal", goals, selected ?: goals.first(), { it.goal.name }, titleCaseValues = false) { selectedGoalId = it.goal.id }
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
        confirmButton = { WhipTextButton(enabled = selected != null, onClick = { onSave(requireNotNull(selected), metric, includeHistory) }) { Text("Create Goal Automation") } },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun HabitPauseDialog(today: LocalDate, onDismiss: () -> Unit, onSave: (LocalDate, LocalDate?, String) -> Unit) {
    var start by rememberSaveable { mutableStateOf(today) }
    var end by rememberSaveable { mutableStateOf<LocalDate?>(today) }
    var note by rememberSaveable { mutableStateOf("") }
    var pickingStart by rememberSaveable { mutableStateOf(false) }
    var pickingEnd by rememberSaveable { mutableStateOf(false) }
    PaneAwareAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Habit Pause") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WhipOutlinedButton(onClick = { pickingStart = true }) { Text("Start ${start.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}") }
            WhipOutlinedButton(onClick = { pickingEnd = true }) { Text(end?.let { "End ${it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}" } ?: "No End Date") }
            OutlinedTextField(note, { note = it }, label = { Text("Optional note") })
        } },
        confirmButton = { WhipTextButton(enabled = end == null || !requireNotNull(end).isBefore(start), onClick = { onSave(start, end, note) }) { Text("Save Pause") } },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
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
private fun <T> EnumDropdown(
    label: String,
    values: List<T>,
    selected: T,
    text: (T) -> String,
    titleCaseValues: Boolean = true,
    enabled: Boolean = true,
    onSelect: (T) -> Unit,
) {
    SelectionField(
        label = label,
        values = values,
        selected = selected,
        valueText = { value -> text(value).let { if (titleCaseValues) it.uiTitleCase() else it } },
        enabled = enabled,
        onSelect = onSelect,
    )
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
            values.forEach { value -> WhipFilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(text(value).uiTitleCase()) }) }
        }
    }
}

@Composable
private fun NumberTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedTextField(value, onValueChange, label = { Text(label) }, enabled = enabled, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = modifier.fillMaxWidth())
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
    HabitScheduleType.EveryNDays -> "Custom Day Interval"
    HabitScheduleType.SelectedWeekdays -> "Specific Weekdays"
    HabitScheduleType.FlexibleTimesPerWeek -> "Flexible Weekly Target"
    HabitScheduleType.FlexibleTimesPerMonth -> "Flexible Monthly Target"
}
private fun HabitEndType.scheduleLabel(): String = when (this) {
    HabitEndType.Never -> "Never"
    HabitEndType.OnDate -> "On date"
    HabitEndType.AfterStreak -> "After streak"
    HabitEndType.AfterCompletions -> "After completions"
    HabitEndType.AfterTotal -> "After total"
}
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
internal fun formatHabitValue(value: Double, precision: Int): String = String.format(Locale.getDefault(), "%.${precision.coerceIn(0, 6)}f", value)
